package com.dominaite.merchant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The Dominaite merchant API client. Build one per API key with {@link #builder()} and share it:
 * it is thread-safe and holds no per-request state.
 *
 * <p>Every method is synchronous and signs its request with HMAC-SHA256. The SDK reads no
 * environment variables and sends no telemetry.
 */
public final class DominaiteClient {

    /** The sandbox merchant API. Use it with your sandbox keys. */
    public static final String SANDBOX_BASE_URL = "https://api.staging.dominaite.com/payments";

    /** The production merchant API. Use it with your live keys. */
    public static final String PRODUCTION_BASE_URL = "https://api.dominaite.com/payments";

    /** The default per-request timeout. Cold starts outside production can take 10+ seconds. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(45);

    /** The SDK version, also sent in the User-Agent. */
    public static final String VERSION = "0.5.2";

    public static final String SESSIONS_PATH = "/merchant-api/checkout/sessions";
    public static final String PING_PATH = "/merchant-api/ping";
    public static final String INTEGRATION_CONTRACT_PATH = "/merchant-api/integration/contract";
    public static final String PAYMENT_METHODS_PATH = "/merchant-api/payment-methods";
    public static final String PAYMENTS_PATH = "/merchant-api/payments";
    public static final String PLANS_PATH = "/merchant-api/plans";
    public static final String AGREEMENTS_PATH = "/merchant-api/agreements";
    public static final String WEBHOOKS_PATH = "/merchant-api/webhooks";
    public static final String TRANSACTIONS_PATH = "/merchant-api/transactions";

    private static final String KEY_ID_PREFIX = "dmk_";
    private static final String SECRET_PREFIX = "dms_";
    private static final Pattern ID_SEGMENT = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");
    private static final Pattern UUID_TEXT = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private final String keyId;
    private final String secret;
    private final String baseUrl;
    private final Duration timeout;
    private final String userAgent;
    private final HttpClient http;

    private DominaiteClient(Builder b) {
        if (b.keyId == null || !b.keyId.startsWith(KEY_ID_PREFIX)) {
            throw new ValidationException("keyId must start with dmk_");
        }
        if (b.secret == null || !b.secret.startsWith(SECRET_PREFIX)) {
            throw new ValidationException("secret must start with dms_");
        }
        this.keyId = b.keyId;
        this.secret = b.secret;

        String base = b.baseUrl == null ? "" : b.baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // There is no default base URL. A fallback to production would turn an unset environment
        // variable into sandbox keys sent to production.
        if (base.isEmpty()) {
            throw new ValidationException("baseUrl is required. Use " + SANDBOX_BASE_URL
                + " for the sandbox or " + PRODUCTION_BASE_URL + " for production.");
        }
        requireEncryptedTransport(base);
        this.baseUrl = base;

        if (b.timeout == null || b.timeout.isZero() || b.timeout.isNegative()) {
            throw new ValidationException("timeout must be positive");
        }
        this.timeout = b.timeout;

        this.userAgent = b.userAgentSuffix == null || b.userAgentSuffix.trim().isEmpty()
            ? "dominaite-java/" + VERSION
            : "dominaite-java/" + VERSION + " " + b.userAgentSuffix.trim();

        // The API never redirects. Following a 3xx would replay a signed request at whatever
        // host the redirect names, so the client the SDK builds never follows one.
        this.http = b.httpClient != null
            ? b.httpClient
            : HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(this.timeout)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The API this client calls, without a trailing slash. */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** The User-Agent this client sends. */
    public String getUserAgent() {
        return userAgent;
    }

    /** A description with the secret redacted. */
    @Override
    public String toString() {
        return "DominaiteClient{keyId=" + keyId + ", baseUrl=" + baseUrl + ", secret=[REDACTED]}";
    }

    // ---------------------------------------------------------------- ping and contract

    /**
     * Checks the credentials and the clock. Every key can call it.
     *
     * @return the merchant id and the clock skew
     */
    public PingResponse ping() {
        Reply reply = send("GET", PING_PATH, "", "", "");
        reply.failOnError();
        return reply.read(reply.payload(), PingResponse.class, "ping");
    }

    /**
     * Reads the public contract of the API: version, status and event values, consent wording
     * versions. Every key can call it.
     */
    public IntegrationContract getIntegrationContract() {
        Reply reply = send("GET", INTEGRATION_CONTRACT_PATH, "", "", "");
        return reply.read(reply.readObject("integration contract"), IntegrationContract.class, "integration contract");
    }

    // ---------------------------------------------------------------- checkout sessions

    /**
     * Opens a checkout session. Needs the {@code payments} scope.
     *
     * @param request the session; the idempotency key is required
     * @return the session to hand to the widget
     * @throws RefusalException when the API answers {@code success: false}
     * @throws StorefrontException when the storefront configuration refuses the session
     */
    public CheckoutSession createCheckoutSession(CheckoutSessionRequest request) {
        Objects.requireNonNull(request, "request");
        String key = IdempotencyKeys.validate(request.getIdempotencyKey());
        validateMoney(request.getAmount(), request.getCurrency(), request.getOrderReference(),
            Boolean.TRUE.equals(request.getVerifyOnly()));
        String body = Json.body(request, request.getExtra());

        Reply reply = withKey(key, () -> send("POST", SESSIONS_PATH, "", body, key));
        return withKey(key, () -> {
            // The storefront codes arrive as a real HTTP 409 or 400. They are configuration,
            // not input, so they get their own type.
            if (reply.status >= 400) {
                StorefrontException storefront = reply.status < 500 ? reply.storefrontRefusal() : null;
                throw reply.tag(storefront != null ? storefront : reply.rejection());
            }

            // Create is the nested shape: an inner success next to checkout.
            JsonNode payload = reply.payload();
            JsonNode checkout = payload.get("checkout");
            boolean succeeded = payload.path("success").isBoolean() && payload.path("success").asBoolean();
            if (succeeded && checkout != null && checkout.isObject()) {
                return reply.read(checkout, CheckoutSession.class, "checkout object");
            }

            // A storefront mismatch on an idempotent replay comes back as a 200 refusal.
            StorefrontException replayStorefront = reply.storefrontRefusal();
            if (replayStorefront != null) {
                throw reply.tag(replayStorefront);
            }

            // A replay refusal names the transaction the key collided with.
            String code = Json.text(payload, "errorCode");
            String message = Json.text(payload, "errorMessage");
            throw reply.tag(new RefusalException(
                reply.status,
                code != null ? code : "UNKNOWN",
                message != null ? message : "The checkout session was refused.",
                Json.text(payload, "transactionId"),
                reply.envelope));
        });
    }

    /**
     * Opens a checkout session and retries the retryable failures with the same idempotency
     * key: transport failures, HTTP 5xx and {@code PAYMENT_PROCESSING_UNAVAILABLE}. The wait
     * starts at {@code baseDelay} and doubles each attempt.
     *
     * @param request the session; the idempotency key is required
     * @param attempts total attempts including the first; at least 1
     * @param baseDelay the wait before the first retry
     * @return the session
     */
    public CheckoutSession createCheckoutSessionWithRetry(
            CheckoutSessionRequest request, int attempts, Duration baseDelay) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(baseDelay, "baseDelay");
        if (attempts < 1) {
            throw new ValidationException("attempts must be at least 1");
        }
        // Checked once, up front, so a missing key fails before the first attempt.
        IdempotencyKeys.validate(request.getIdempotencyKey());

        DominaiteException last = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                return createCheckoutSession(request);
            } catch (DominaiteException error) {
                if (!error.isRetryable()) {
                    throw error;
                }
                last = error;
                if (attempt + 1 < attempts) {
                    sleep(baseDelay.multipliedBy(1L << Math.min(attempt, 16)));
                }
            }
        }
        throw last;
    }

    /** {@link #createCheckoutSessionWithRetry(CheckoutSessionRequest, int, Duration)} with 3 attempts from 500 ms. */
    public CheckoutSession createCheckoutSessionWithRetry(CheckoutSessionRequest request) {
        return createCheckoutSessionWithRetry(request, 3, Duration.ofMillis(500));
    }

    /**
     * Reads the status of a payment.
     *
     * @param transactionId the payment UUID from the session or a charge
     * @return the status
     */
    public CheckoutStatus getStatus(String transactionId) {
        String path = SESSIONS_PATH + "/" + normalizeTransactionId(transactionId);
        Reply reply = send("GET", path, "", "", "");
        reply.failOnError();
        return reply.read(reply.payload(), CheckoutStatus.class, "status");
    }

    // ---------------------------------------------------------------- stored payment methods

    /**
     * Charges a stored payment method. A provider decline is a result, not an exception: the
     * charge comes back with status {@code failed}.
     *
     * @param paymentMethodId the stored payment method id from the status read
     * @param request the charge; the idempotency key is required
     * @return the charge, also when declined
     * @throws ChargeException when the charge is refused with a code
     */
    public PaymentMethodCharge chargePaymentMethod(String paymentMethodId, ChargeRequest request) {
        Objects.requireNonNull(request, "request");
        String id = normalizePaymentMethodId(paymentMethodId);
        String key = IdempotencyKeys.validate(request.getIdempotencyKey());
        validateMoney(request.getAmount(), request.getCurrency(), request.getOrderReference(), false);
        String body = Json.body(request, request.getExtra());
        String path = PAYMENT_METHODS_PATH + "/" + id + "/charges";

        Reply reply = withKey(key, () -> send("POST", path, "", body, key));
        return withKey(key, () -> {
            // 201 (200 on a replay): the charge was placed, whatever its status. 402: the
            // provider declined; the charge is in the body with status failed.
            PaymentMethodCharge charge = reply.charge();
            if (charge != null && (reply.success() || reply.status == 402)) {
                return charge;
            }
            if (reply.status >= 400) {
                String code = reply.errorCode();
                if (code != null && !isGenericFailureStatus(reply.status)) {
                    String message = reply.errorMessage();
                    throw reply.tag(new ChargeException(
                        reply.status, code, message != null ? message : "The charge was refused.", charge, reply.envelope));
                }
                throw reply.tag(reply.rejection());
            }
            throw reply.tag(new ApiException(
                reply.status, null, "The Dominaite API answered the charge without a charge body.", reply.envelope));
        });
    }

    /**
     * Revokes a stored payment method. It cannot be charged after this.
     *
     * @param paymentMethodId the stored payment method id
     * @throws RevokeException when the revoke is refused with a code
     */
    public void revokePaymentMethod(String paymentMethodId) {
        String path = PAYMENT_METHODS_PATH + "/" + normalizePaymentMethodId(paymentMethodId);
        // DELETE signs an empty idempotency key and an empty body, exactly like GET.
        Reply reply = send("DELETE", path, "", "", "");
        if (reply.status < 400) {
            return;
        }
        String code = reply.errorCode();
        if (code != null && !isGenericFailureStatus(reply.status)) {
            String message = reply.errorMessage();
            throw reply.tag(new RevokeException(
                reply.status, code, message != null ? message : "The revoke was refused.", reply.envelope));
        }
        throw reply.tag(reply.rejection());
    }

    // ---------------------------------------------------------------- refunds

    /**
     * Refunds a payment, in full or in part.
     *
     * @param transactionId the payment UUID
     * @param request the refund; the idempotency key is required
     * @return the refund; poll it with {@link #getRefund} until it is terminal
     * @throws RefundException when the refund is refused with a code
     */
    public Refund createRefund(String transactionId, RefundRequest request) {
        Objects.requireNonNull(request, "request");
        String id = normalizeTransactionId(transactionId);
        String key = IdempotencyKeys.validate(request.getIdempotencyKey());
        if (request.getAmount() != null && request.getAmount() <= 0) {
            throw new ValidationException("amount must be a positive integer in MINOR units (e.g. 2500 for 25.00 EUR), "
                + "or null to refund everything still refundable");
        }
        String body = Json.body(request, request.getExtra());
        String path = PAYMENTS_PATH + "/" + id + "/refunds";

        Reply reply = withKey(key, () -> send("POST", path, "", body, key));
        return withKey(key, reply::readRefund);
    }

    /** Reads one refund of a payment. */
    public Refund getRefund(String transactionId, String refundId) {
        String path = PAYMENTS_PATH + "/" + normalizeTransactionId(transactionId) + "/refunds/"
            + normalizeHandle(refundId, "refundId must be the Refund.getRefundId() from createRefund");
        return send("GET", path, "", "", "").readRefund();
    }

    /** Lists the refunds of a payment, with the refunded and the refundable amount. */
    public RefundList listRefunds(String transactionId) {
        String path = PAYMENTS_PATH + "/" + normalizeTransactionId(transactionId) + "/refunds";
        return send("GET", path, "", "", "").readRefundList();
    }

    // ---------------------------------------------------------------- plans

    /** Creates a subscription plan. The idempotency key is required. */
    public Plan createPlan(CreatePlanRequest request) {
        Objects.requireNonNull(request, "request");
        String key = IdempotencyKeys.validate(request.getIdempotencyKey());
        String body = Json.body(request, request.getExtra());
        Reply reply = withKey(key, () -> send("POST", PLANS_PATH, "", body, key));
        return withKey(key, () -> reply.read(reply.readObject("plan"), Plan.class, "plan"));
    }

    /** Lists your plans. */
    public List<Plan> listPlans() {
        Reply reply = send("GET", PLANS_PATH, "", "", "");
        return reply.readList(reply.readArray("plan list"), Plan.class, "plan");
    }

    /** Reads one plan. */
    public Plan getPlan(String planId) {
        Reply reply = send("GET", planPath(planId), "", "", "");
        return reply.read(reply.readObject("plan"), Plan.class, "plan");
    }

    /**
     * Changes a plan's name, trial days or cancel URL. The gateway takes no idempotency key on
     * PATCH, so the SDK signs an empty one and sends no header.
     */
    public Plan updatePlan(String planId, UpdatePlanRequest request) {
        Objects.requireNonNull(request, "request");
        String path = planPath(planId);
        String body = Json.body(request, request.getExtra());
        if ("{}".equals(body)) {
            throw new ValidationException("Set at least one of name, trialDays or cancelUrl");
        }
        Reply reply = send("PATCH", path, "", body, "");
        return reply.read(reply.readObject("plan"), Plan.class, "plan");
    }

    /** Archives a plan. Agreements on it keep running. */
    public void deletePlan(String planId) {
        send("DELETE", planPath(planId), "", "", "").readNothing();
    }

    // ---------------------------------------------------------------- agreements

    /** Creates a subscription agreement for one payer. The idempotency key is required. */
    public Agreement createAgreement(CreateAgreementRequest request) {
        Objects.requireNonNull(request, "request");
        String key = IdempotencyKeys.validate(request.getIdempotencyKey());
        String body = Json.body(request, request.getExtra());
        Reply reply = withKey(key, () -> send("POST", AGREEMENTS_PATH, "", body, key));
        return withKey(key, () -> reply.read(reply.readObject("agreement"), Agreement.class, "agreement"));
    }

    /** Reads one agreement. */
    public Agreement getAgreement(String agreementId) {
        Reply reply = send("GET", agreementPath(agreementId), "", "", "");
        return reply.read(reply.readObject("agreement"), Agreement.class, "agreement");
    }

    /** Cancels an agreement. No charge runs after this. */
    public void cancelAgreement(String agreementId) {
        send("DELETE", agreementPath(agreementId), "", "", "").readNothing();
    }

    // ---------------------------------------------------------------- webhook endpoints

    /**
     * Registers a webhook endpoint. Needs the {@code webhooks:manage} scope. The answer is the
     * only place the signing secret appears: store it.
     */
    public WebhookEndpoint createWebhookEndpoint(CreateWebhookEndpointRequest request) {
        Objects.requireNonNull(request, "request");
        String key = IdempotencyKeys.validate(request.getIdempotencyKey());
        String body = Json.body(request, request.getExtra());
        Reply reply = withKey(key, () -> send("POST", WEBHOOKS_PATH, "", body, key));
        return withKey(key, () -> reply.read(reply.readObject("webhook endpoint"), WebhookEndpoint.class, "webhook endpoint"));
    }

    /** Lists your webhook endpoints. Needs the {@code webhooks:manage} scope. */
    public List<WebhookEndpoint> listWebhookEndpoints() {
        Reply reply = send("GET", WEBHOOKS_PATH, "", "", "");
        return reply.readList(reply.readArray("webhook endpoint list"), WebhookEndpoint.class, "webhook endpoint");
    }

    /** Deletes a webhook endpoint. Needs the {@code webhooks:manage} scope. */
    public void deleteWebhookEndpoint(String webhookId) {
        String id = normalizeUuid(webhookId,
            "webhookId must be the WebhookEndpoint.getId() from createWebhookEndpoint or listWebhookEndpoints");
        send("DELETE", WEBHOOKS_PATH + "/" + id, "", "", "").readNothing();
    }

    // ---------------------------------------------------------------- transactions

    /**
     * Lists your online payments in a time window, oldest first, one page at a time. Needs the
     * {@code transactions:read} scope. The query is in the URL only: the signed path is
     * {@code /merchant-api/transactions} without it.
     *
     * @param request the window, the filters and the cursor
     * @return one page; {@link TransactionPage#getNextCursor()} is null on the last page
     */
    public TransactionPage listTransactions(ListTransactionsRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.getFrom() == null || request.getTo() == null) {
            throw new ValidationException("from and to are required: ISO 8601 UTC, e.g. 2026-10-01T00:00:00Z");
        }
        Reply reply = send("GET", TRANSACTIONS_PATH, transactionQuery(request), "", "");
        JsonNode data = reply.readObject("transaction page");
        TransactionPage page = reply.read(data, TransactionPage.class, "transaction page");
        JsonNode items = data.get("items");
        if (items != null && items.isArray()) {
            List<TransactionItem> rows = page.getItems();
            for (int i = 0; i < rows.size(); i++) {
                rows.get(i).setRaw(items.get(i));
            }
        }
        return page;
    }

    static String transactionQuery(ListTransactionsRequest request) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("from", Json.formatInstant(request.getFrom()));
        parameters.put("to", Json.formatInstant(request.getTo()));
        if (request.getSettledFrom() != null) {
            parameters.put("settledFrom", request.getSettledFrom().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (request.getSettledTo() != null) {
            parameters.put("settledTo", request.getSettledTo().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (request.getStatus() != null) {
            parameters.put("status", request.getStatus());
        }
        if (request.getLimit() != null) {
            parameters.put("limit", request.getLimit().toString());
        }
        if (request.getCursor() != null) {
            parameters.put("cursor", request.getCursor());
        }
        for (Map.Entry<String, String> field : request.getExtra().entrySet()) {
            parameters.putIfAbsent(field.getKey(), field.getValue());
        }
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(escape(parameter.getKey())).append('=').append(escape(parameter.getValue()));
        }
        return query.toString();
    }

    /** RFC 3986 percent-encoding: every byte but the unreserved characters. A space is %20. */
    static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                out.append((char) c);
            } else {
                out.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
                    .append(Character.toUpperCase(Character.forDigit(c & 0x0f, 16)));
            }
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- transport

    /**
     * Refuses a base URL that would put the signed request on the wire in clear text. Plain
     * {@code http://} is allowed only for localhost, 127.0.0.1 and ::1, for local mock servers.
     */
    private static void requireEncryptedTransport(String baseUrl) {
        URI uri;
        try {
            uri = new URI(baseUrl);
        } catch (URISyntaxException e) {
            throw new ValidationException("baseUrl is not a valid URL: " + baseUrl);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme.equals("https")) {
            return;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (scheme.equals("http") && (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1"))) {
            return;
        }
        throw new ValidationException("baseUrl must use https:// (http:// is allowed only for localhost, "
            + "127.0.0.1 and ::1). Anywhere else it would send your key id, signature and request body "
            + "in clear text.");
    }

    private Reply send(String method, String path, String query, String body, String idempotencyKey) {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        // The signed path is the canonical path only: never the base URL's own prefix, never
        // the query string.
        String signature = RequestSigner.sign(new SignatureInput(secret, timestamp, method, path, idempotencyKey, body));

        URI uri;
        try {
            uri = URI.create(query.isEmpty() ? baseUrl + path : baseUrl + path + "?" + query);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("baseUrl is not a valid URL: " + baseUrl);
        }
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
            .timeout(timeout)
            // Some edges block requests without a real User-Agent, so always send one.
            .header("User-Agent", userAgent)
            .header("X-Api-Key-Id", keyId)
            .header("X-Timestamp", timestamp)
            .header("X-Signature", signature);
        // No Idempotency-Key header when the signature has an empty one.
        if (!idempotencyKey.isEmpty()) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        // The exact bytes that were hashed above. GET and DELETE send no body.
        if (!body.isEmpty()) {
            request.header("Content-Type", "application/json");
            request.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<String> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new TransportException(
                "The Dominaite API did not answer in time; retry with the same idempotency key.", null, null, e);
        } catch (IOException e) {
            throw new TransportException("Could not reach the Dominaite API: " + e.getMessage(), null, null, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("The call to the Dominaite API was interrupted.", null, null, e);
        }

        int status = response.statusCode();
        // The API never redirects. A 3xx is a hard error, never retried.
        if (status >= 300 && status < 400) {
            throw new ApiException(status, null,
                "The Dominaite API returned an unexpected redirect response; the Dominaite API never redirects.", null);
        }
        // A revoke answers 204 with no body, and no body is not a parse failure.
        if (status == 204) {
            return new Reply(status, Json.MAPPER.createObjectNode(), null);
        }

        String raw = response.body() == null ? "" : response.body();
        // A 429 means the same thing whoever sent it, the API or an edge in front of it.
        if (status == 429) {
            Duration retryAfter = readRetryAfter(response.headers().firstValue("Retry-After").orElse(null));
            JsonNode envelope = tryParseObject(raw);
            Reply reply = new Reply(status, envelope != null ? envelope : Json.MAPPER.createObjectNode(), retryAfter);
            throw reply.tag(reply.rejection());
        }

        JsonNode envelope = tryParseObject(raw);
        if (envelope == null) {
            if (status >= 500) {
                throw new TransportException("The Dominaite API is unavailable (HTTP " + status
                    + "); retry with the same idempotency key.", status, null, null);
            }
            throw new ApiException(status, null, "The Dominaite API returned a non-JSON response.", null);
        }

        Reply reply = new Reply(status, envelope, null);
        // Credentials and permissions are refused the same way on every route.
        if (status == 401 || status == 403) {
            throw reply.tag(reply.rejection());
        }
        return reply;
    }

    private static JsonNode tryParseObject(String raw) {
        try {
            JsonNode node = Json.MAPPER.readTree(raw);
            return node != null && node.isObject() ? node : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** Retry-After is delta seconds or an HTTP date. A date in the past is zero. */
    static Duration readRetryAfter(String header) {
        if (header == null || header.trim().isEmpty()) {
            return null;
        }
        String value = header.trim();
        try {
            long seconds = Long.parseLong(value);
            return seconds < 0 ? null : Duration.ofSeconds(seconds);
        } catch (NumberFormatException notSeconds) {
            try {
                Duration wait = Duration.between(Instant.now(),
                    ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                return wait.isNegative() ? Duration.ZERO : wait;
            } catch (DateTimeParseException notADate) {
                return null;
            }
        }
    }

    // ---------------------------------------------------------------- validation

    private static void validateMoney(long amount, String currency, String orderReference, boolean verifyOnly) {
        // The gateway refuses a verify-only session with any amount but 0: the platform sets
        // the verification amount.
        if (verifyOnly && amount != 0) {
            throw new ValidationException("Leave amount at 0 with verifyOnly: the platform sets the verification amount");
        }
        if (amount <= 0 && !verifyOnly) {
            throw new ValidationException("amount must be a positive integer in MINOR units (e.g. 2500 for 25.00 EUR)");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new ValidationException("Missing required parameter: currency");
        }
        if (orderReference == null || orderReference.trim().isEmpty()) {
            throw new ValidationException("Missing required parameter: orderReference");
        }
        if (orderReference.length() > 100) {
            throw new ValidationException("orderReference must be at most 100 characters");
        }
    }

    private static String normalizeTransactionId(String transactionId) {
        return normalizeUuid(transactionId,
            "transactionId must be the payment UUID returned by createCheckoutSession or a charge");
    }

    private static String normalizeUuid(String value, String message) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!UUID_TEXT.matcher(normalized).matches()) {
            throw new ValidationException(message);
        }
        return normalized;
    }

    private static String normalizePaymentMethodId(String paymentMethodId) {
        return normalizeHandle(paymentMethodId,
            "paymentMethodId must be the StoredPaymentMethod id from getStatus");
    }

    private static String normalizeHandle(String id, String what) {
        String trimmed = id == null ? "" : id.trim();
        if (!ID_SEGMENT.matcher(trimmed).matches()) {
            throw new ValidationException(what + " (letters, digits, _ or -, at most 100 characters)");
        }
        return trimmed;
    }

    private static String planPath(String planId) {
        return PLANS_PATH + "/" + normalizeHandle(planId, "planId must be the Plan.getId() from createPlan");
    }

    private static String agreementPath(String agreementId) {
        return AGREEMENTS_PATH + "/"
            + normalizeHandle(agreementId, "agreementId must be the Agreement.getId() from createAgreement");
    }

    private static boolean isGenericFailureStatus(int status) {
        return status == 400 || status == 404;
    }

    /** Runs a step and stamps the idempotency key on any SDK error it throws. */
    private static <T> T withKey(String key, Step<T> step) {
        try {
            return step.run();
        } catch (DominaiteException error) {
            if (error.getIdempotencyKey() == null) {
                error.setIdempotencyKey(key);
            }
            throw error;
        }
    }

    private static void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("The retry wait was interrupted.", null, null, e);
        }
    }

    @FunctionalInterface
    private interface Step<T> {
        T run();
    }

    // ---------------------------------------------------------------- reply

    /** One parsed response: the status, the envelope, and the reads every route shares. */
    private static final class Reply {

        final int status;
        final JsonNode envelope;
        final Duration retryAfter;

        Reply(int status, JsonNode envelope, Duration retryAfter) {
            this.status = status;
            this.envelope = envelope;
            this.retryAfter = retryAfter;
        }

        boolean success() {
            JsonNode success = envelope.get("success");
            return success != null && success.isBoolean() && success.asBoolean();
        }

        JsonNode data() {
            JsonNode data = envelope.get("data");
            return data != null && data.isObject() ? data : null;
        }

        /** The data object, or the envelope itself for the flat reads (ping, status). */
        JsonNode payload() {
            JsonNode data = data();
            return data != null ? data : envelope;
        }

        String errorCode() {
            String code = Json.text(envelope.get("error"), "code");
            return code != null ? code : Json.text(payload(), "errorCode");
        }

        String errorMessage() {
            String message = Json.text(envelope.get("error"), "message");
            return message != null ? message : Json.text(payload(), "errorMessage");
        }

        String requestId() {
            return Json.text(envelope.get("metadata"), "requestId");
        }

        <E extends DominaiteException> E tag(E error) {
            error.setRequestId(requestId());
            return error;
        }

        void failOnError() {
            if (status >= 400) {
                throw tag(rejection());
            }
        }

        JsonNode readObject(String what) {
            failOnError();
            JsonNode data = data();
            if (data == null) {
                throw tag(new ApiException(status, null, "The Dominaite API answered without a " + what + " body.", envelope));
            }
            return data;
        }

        JsonNode readArray(String what) {
            failOnError();
            JsonNode data = envelope.get("data");
            if (data == null || !data.isArray()) {
                throw tag(new ApiException(status, null, "The Dominaite API answered without a " + what + " body.", envelope));
            }
            return data;
        }

        void readNothing() {
            failOnError();
        }

        <T> T read(JsonNode node, Class<T> type, String what) {
            T value;
            try {
                value = Json.MAPPER.treeToValue(node, type);
            } catch (JsonProcessingException | IllegalArgumentException e) {
                throw tag(new ApiException(status, null, "The Dominaite API returned an unexpected " + what + " response.", envelope));
            }
            if (value == null) {
                throw tag(new ApiException(status, null, "The Dominaite API returned an unexpected " + what + " response.", envelope));
            }
            setRaw(value, node);
            return value;
        }

        <T> List<T> readList(JsonNode array, Class<T> type, String what) {
            List<T> items = new ArrayList<>();
            for (JsonNode item : array) {
                items.add(read(item, type, what));
            }
            return Collections.unmodifiableList(items);
        }

        PaymentMethodCharge charge() {
            JsonNode data = data();
            if (data == null || Json.text(data, "chargeId") == null) {
                return null;
            }
            return read(data, PaymentMethodCharge.class, "charge");
        }

        Refund readRefund() {
            if (status >= 400) {
                String code = errorCode();
                if (status < 500 && code != null) {
                    String message = errorMessage();
                    throw tag(new RefundException(status, code, message != null ? message : "The refund was refused.", envelope));
                }
                throw tag(rejection());
            }
            JsonNode data = data();
            if (data == null || Json.text(data, "refundId") == null) {
                throw tag(new ApiException(status, null, "The Dominaite API answered the refund without a refund body.", envelope));
            }
            return read(data, Refund.class, "refund");
        }

        RefundList readRefundList() {
            if (status >= 400) {
                String code = errorCode();
                if (status < 500 && code != null) {
                    String message = errorMessage();
                    throw tag(new RefundException(status, code, message != null ? message : "The refund list was refused.", envelope));
                }
                throw tag(rejection());
            }
            JsonNode data = data();
            JsonNode refunds = data == null ? null : data.get("refunds");
            if (refunds == null || !refunds.isArray()) {
                throw tag(new ApiException(status, null,
                    "The Dominaite API answered the refund list without a refund list body.", envelope));
            }
            RefundList list = read(data, RefundList.class, "refund list");
            for (int i = 0; i < list.getRefunds().size(); i++) {
                list.getRefunds().get(i).setRaw(refunds.get(i));
            }
            return list;
        }

        StorefrontException storefrontRefusal() {
            String code = errorCode();
            if (code == null || !ErrorCodes.STOREFRONT.contains(code)) {
                return null;
            }
            String message = errorMessage();
            return new StorefrontException(status, code,
                message != null ? message : "The storefront refused the checkout session.",
                Json.text(payload(), "transactionId"), envelope);
        }

        DominaiteException rejection() {
            String code = errorCode();
            String message = errorMessage();
            DominaiteException error;
            if (status >= 500) {
                error = new TransportException("The Dominaite API is unavailable (HTTP " + status
                    + "); retry with the same idempotency key.", status, code, null);
            } else if (status == 401) {
                error = new AuthenticationException(
                    code != null ? code : "UNAUTHORIZED",
                    message != null ? message : "Authentication failed - check your key id, secret, and server clock.",
                    status);
            } else if (status == 403) {
                error = forbidden(code, message);
            } else if (status == 429) {
                error = new RateLimitedException(code,
                    message != null ? message : "Too many requests (HTTP 429). Wait, then retry with the same idempotency key.",
                    retryAfter);
            } else {
                error = new ApiException(status, code, message != null ? message : "Request rejected", envelope);
            }
            return tag(error);
        }

        private static PermissionException forbidden(String code, String message) {
            if (ErrorCodes.INSUFFICIENT_SCOPE.equals(code)) {
                String text = message != null ? message : "This API key does not have the scope this route requires";
                while (text.endsWith(".")) {
                    text = text.substring(0, text.length() - 1);
                }
                return new InsufficientScopeException(text + ". Use an API key that has this scope.");
            }
            if (ErrorCodes.VERIFY_ONLY_NOT_ENABLED.equals(code)) {
                return new PermissionException(code, "Verify-only sessions are not enabled for this account. "
                    + "We switch it on for your account. Contact Dominaite support to ask for it.");
            }
            return new PermissionException(code != null ? code : "FORBIDDEN",
                message != null ? message : "This API key is not allowed to make this call (HTTP 403).");
        }

        private static void setRaw(Object value, JsonNode node) {
            if (value instanceof PingResponse) {
                ((PingResponse) value).setRaw(node);
            } else if (value instanceof IntegrationContract) {
                ((IntegrationContract) value).setRaw(node);
            } else if (value instanceof CheckoutSession) {
                ((CheckoutSession) value).setRaw(node);
            } else if (value instanceof CheckoutStatus) {
                ((CheckoutStatus) value).setRaw(node);
            } else if (value instanceof PaymentMethodCharge) {
                ((PaymentMethodCharge) value).setRaw(node);
            } else if (value instanceof Refund) {
                ((Refund) value).setRaw(node);
            } else if (value instanceof RefundList) {
                ((RefundList) value).setRaw(node);
            } else if (value instanceof Plan) {
                ((Plan) value).setRaw(node);
            } else if (value instanceof Agreement) {
                ((Agreement) value).setRaw(node);
            } else if (value instanceof WebhookEndpoint) {
                ((WebhookEndpoint) value).setRaw(node);
            } else if (value instanceof TransactionPage) {
                ((TransactionPage) value).setRaw(node);
            }
        }
    }

    // ---------------------------------------------------------------- builder

    /** Builds a {@link DominaiteClient}. keyId, secret and baseUrl are required. */
    public static final class Builder {

        private String keyId;
        private String secret;
        private String baseUrl;
        private Duration timeout = DEFAULT_TIMEOUT;
        private String userAgentSuffix;
        private HttpClient httpClient;

        private Builder() {
        }

        /** Your API key id ({@code dmk_...}). */
        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        /** Your API secret ({@code dms_...}). Server-side only. */
        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        /**
         * The API to call: {@link #SANDBOX_BASE_URL}, {@link #PRODUCTION_BASE_URL} or your own
         * value. Required: {@link Builder#build()} throws {@link ValidationException} when it is
         * null, empty or blank. It must be {@code https://}; {@code http://} is accepted only for
         * localhost, 127.0.0.1 and ::1. Trailing slashes are trimmed. The base URL's own prefix
         * is never part of the signed path.
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** The per-request timeout. Defaults to 45 seconds. */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /** Appended to the User-Agent. It helps Dominaite support find your calls in the logs. */
        public Builder userAgentSuffix(String userAgentSuffix) {
            this.userAgentSuffix = userAgentSuffix;
            return this;
        }

        /**
         * Your own HttpClient, for a proxy or an executor of your own. Build it with
         * {@code followRedirects(HttpClient.Redirect.NEVER)}: a client that follows a 3xx
         * replays your signed request at whatever host the redirect names.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public DominaiteClient build() {
            return new DominaiteClient(this);
        }
    }
}
