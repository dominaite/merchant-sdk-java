package com.dominaite.merchant.samples;

import com.dominaite.merchant.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * The Java samples of README.md, compiled against the public API. This class is outside the SDK
 * package, so a sample that needs package access fails the build. Nothing here runs.
 */
@SuppressWarnings("unused")
final class ReadmeSamples {

    private ReadmeSamples() {
    }

    static void quickStart() {
        DominaiteClient client = DominaiteClient.builder()
            .keyId(System.getenv("DOMINAITE_KEY_ID"))
            .secret(System.getenv("DOMINAITE_SECRET"))
            // Required. An unset variable is null, and build() throws on it.
            .baseUrl(System.getenv("DOMINAITE_BASE_URL"))
            .build();

        PingResponse ping = client.ping();
        System.out.println("merchant " + ping.getMerchantId() + ", clock skew " + ping.getClockSkewSeconds() + "s");

        CheckoutSession session = client.createCheckoutSession(CheckoutSessionRequest.builder()
            .amount(2500) // 2500 = 25.00 EUR, always MINOR units
            .currency("EUR")
            .orderReference("order-1042")
            .customer(Customer.builder().firstName("Ana").email("ana@example.com").build())
            .idempotencyKey(IdempotencyKeys.forOrder("checkout", "order-1042", 2500, "EUR"))
            .build());
    }

    static void signing() {
        String signature = RequestSigner.sign(new SignatureInput(
            "dms_...",
            "1755302400",                                   // unix SECONDS
            "POST",
            "/merchant-api/checkout/sessions",              // the path only, no host
            "00000000-0000-4000-8000-000000000001",         // "" for GET
            "{\"amount\":2500,\"currency\":\"EUR\",\"orderReference\":\"order-1042\"}")); // "" for GET
    }

    static void minorUnits() {
        long amount = MinorUnits.from(new BigDecimal("0.30"), "EUR");  // 30
        long yen = MinorUnits.from(new BigDecimal("2500"), "JPY");     // 2500
        int decimals = MinorUnits.exponent("KWD");                     // 3
    }

    static void retries(DominaiteClient client, CheckoutSessionRequest request, String orderId, long amountMinor) {
        String key = IdempotencyKeys.forOrder("checkout", orderId, amountMinor, "EUR");

        CheckoutSession retried = client.createCheckoutSessionWithRetry(request, 3, Duration.ofMillis(500));

        try {
            CheckoutSession session = client.createCheckoutSessionWithRetry(request);
        } catch (RefusalException error) {
            if (error.getTransactionId() != null) {
                CheckoutStatus status = client.getStatus(error.getTransactionId());
            }
        }
    }

    static void cardFields(DominaiteClient client) {
        CheckoutSession session = client.createCheckoutSession(CheckoutSessionRequest.builder()
            .amount(2500)
            .currency("EUR")
            .orderReference("order-1042")
            .integration(CheckoutIntegrations.FIELDS) // null = CheckoutIntegrations.WIDGET
            .idempotencyKey(IdempotencyKeys.forOrder("checkout", "order-1042", 2500, "EUR"))
            .build());
        String secret = session.getClientSecret();
        String integration = session.getIntegration();
    }

    static void storedPaymentMethods(DominaiteClient client, CheckoutSession session, String customerId, String methodId) {
        CheckoutStatus status = client.getStatus(session.getTransactionId());
        StoredPaymentMethod method = status.getStoredPaymentMethod();
        if (method != null && method.isChargeable()) {
            storeForCustomer(customerId, method.getId()); // pm_...
        }

        try {
            PaymentMethodCharge charge = client.chargePaymentMethod(methodId, ChargeRequest.builder()
                .amount(2500)
                .currency("EUR")
                .orderReference("order-1043")
                .description("Monthly plan")
                .idempotencyKey(IdempotencyKeys.forOrder("charge", "order-1043", 2500, "EUR"))
                .build());

            if (charge.isPaid()) {
                markPaid(charge.getTransactionId());
            } else if (ChargeStatuses.PENDING.equals(charge.getStatus())) {
                pollLater(charge.getTransactionId()); // not terminal
            } else if (DeclineClasses.HARD.equals(charge.getDeclineClass())) {
                stopCharging(methodId); // never retry
            } else if (DeclineClasses.SOFT_SCA_REQUIRED.equals(charge.getDeclineClass())) {
                bringThePayerBack(); // needs a session
            }
        } catch (ChargeException error) {
            if (ErrorCodes.CHARGE_OUTCOME_UNKNOWN.equals(error.getCode())) {
                pollLater(error.getTransactionId());
            } else if (error.isRetryable() || ErrorCodes.DUPLICATE_REQUEST.equals(error.getCode())) {
                retryLater(error.getIdempotencyKey()); // nothing was charged: the SAME key
            } else {
                throw error;
            }
        }

        client.revokePaymentMethod(methodId);
    }

    static void verifyOnly(DominaiteClient client) {
        CheckoutSession session = client.createCheckoutSession(CheckoutSessionRequest.builder()
            .currency("EUR")
            .orderReference("card-check-42")
            .verifyOnly(true)
            .idempotencyKey("verify-card-check-42-EUR") // forOrder needs an amount above 0
            .build());
        Boolean verifyOnly = client.getStatus(session.getTransactionId()).getVerifyOnly();
    }

    static void subscriptions(DominaiteClient client, Instant acceptedAt, String payerIp) {
        Plan plan = client.createPlan(CreatePlanRequest.builder()
            .name("Pro monthly")
            .amount(2500)
            .currency("EUR")
            .intervalUnit(IntervalUnits.MONTH)
            .idempotencyKey("plan-pro-monthly-2500-EUR")
            .build());

        Agreement agreement = client.createAgreement(CreateAgreementRequest.builder()
            .planId(plan.getId())
            .customerReference("cust-42")
            .idempotencyKey("subscription-cust-42-pro")
            .build());

        IntegrationContract contract = client.getIntegrationContract();
        CheckoutSession session = client.createCheckoutSession(CheckoutSessionRequest.builder()
            .amount(2500)
            .currency("EUR")
            .orderReference("sub-42-1")
            .agreementId(agreement.getId())
            .consent(AgreementConsent.builder()
                .textVersion(contract.getRecurring().getConsentTextVersions().get(0)) // the wording your page shows
                .acceptedAt(acceptedAt)
                .ip(payerIp)
                .build())
            .idempotencyKey(IdempotencyKeys.forOrder("checkout", "sub-42-1", 2500, "EUR"))
            .build());

        List<Plan> plans = client.listPlans();
        client.updatePlan(plan.getId(), UpdatePlanRequest.builder().name("Pro").build());
        client.deletePlan(plan.getId());
        client.getAgreement(agreement.getId());
        client.cancelAgreement(agreement.getId());
    }

    static void webhooks(byte[] body, String signatureHeader, String secret) {
        try {
            WebhookEvent event = Webhooks.verifyAndParse(body, signatureHeader, secret);
        } catch (WebhookVerificationException error) {
            if (error.getReason() == WebhookFailureReason.TIMESTAMP_OUT_OF_TOLERANCE) {
                return;
            }
        }

        WebhookEvent event = Webhooks.verifyAndParse(body, signatureHeader, secret);
        if (WebhookEventTypes.PAYMENT_SUCCEEDED.equals(event.getType()) && event.getPayment() != null) {
            fulfil(event.getPayment().getOrderReference(), event.getPayment().getTransactionId());
        }
        if (event.getCharge() != null) {
            String key = event.getCharge().getOrderingKey();
            Long sequence = event.getCharge().getSequence();
        }
    }

    static void webhookEndpoints(DominaiteClient client) {
        WebhookEndpoint endpoint = client.createWebhookEndpoint(CreateWebhookEndpointRequest.builder()
            .url("https://shop.example.com/webhooks/dominaite")
            .events(List.of(WebhookEventTypes.PAYMENT_SUCCEEDED, WebhookEventTypes.PAYMENT_REFUNDED))
            .idempotencyKey("webhook-orders-endpoint")
            .build());
        storeWebhookSecret(endpoint.getSecret()); // whsec_...
        List<WebhookEndpoint> endpoints = client.listWebhookEndpoints();
        client.deleteWebhookEndpoint(endpoint.getId());
    }

    static void statusPolling(DominaiteClient client, CheckoutSession session) {
        CheckoutStatus status = client.getStatus(session.getTransactionId());
        if (status.isPaid()) {
            return;
        }
        List<String> all = TransactionStatuses.ALL;
        String wallet = status.getWalletType();
        String method = status.getPaymentMethod();
    }

    static void refunds(DominaiteClient client, String transactionId) {
        Refund refund = client.createRefund(transactionId, RefundRequest.builder()
            .amount(MinorUnits.from(new BigDecimal("1500"), "HUF")) // 1500: HUF has 0 decimals on the gateway
            .reason("Returned item")
            .idempotencyKey(IdempotencyKeys.forOrder("refund", "rma-77", 1500, "HUF"))
            .build());

        Refund current = client.getRefund(transactionId, refund.getRefundId());
        if (current.isTerminal() && !current.isSucceeded()) {
            String code = current.getFailureCode();
        }

        RefundList refunds = client.listRefunds(transactionId);
        long refundable = refunds.getRefundableAmount();
    }

    static void transactions(DominaiteClient client) {
        ListTransactionsRequest request = ListTransactionsRequest.builder()
            .from(Instant.parse("2026-10-01T00:00:00Z"))
            .to(Instant.parse("2026-11-01T00:00:00Z"))
            .build();
        while (true) {
            TransactionPage page = client.listTransactions(request);
            for (TransactionItem item : page.getItems()) {
                book(item);
            }
            if (page.getNextCursor() == null) {
                break; // the last page
            }
            request = request.withCursor(page.getNextCursor());
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant now = Instant.now();
        TransactionPage page = client.listTransactions(ListTransactionsRequest.builder()
            .from(now.minus(Duration.ofDays(31)))
            .to(now)
            .settledFrom(today.minusDays(1))
            .settledTo(today)
            .build());
        TransactionItem item = page.getItems().get(0);
        Long fee = item.getFee();
        Long net = item.getNetAmount();
        Instant settledAt = item.getSettledAt();
        String payoutId = item.getPayoutId();
    }

    static void errors(DominaiteClient client) {
        try {
            client.ping();
        } catch (InsufficientScopeException error) {
            String scope = error.getMessage();
        } catch (RateLimitedException error) {
            Duration wait = error.getRetryAfter();
        } catch (ApiException error) {
            Object envelope = error.getRawResult();
        } catch (DominaiteException error) {
            String requestId = error.getRequestId();
            Integer status = error.getHttpStatus();
        }
    }

    private static void storeForCustomer(String customerId, String methodId) {
    }

    private static void markPaid(String transactionId) {
    }

    private static void pollLater(String transactionId) {
    }

    private static void stopCharging(String methodId) {
    }

    private static void bringThePayerBack() {
    }

    /** Stands in for the application's own logger, such as SLF4J. */
    interface AppLog {
        void warn(String message);

        void debug(String message);
    }

    static void loggingAndWarnings(AppLog log) {
        DominaiteClient client = DominaiteClient.builder()
            .keyId(System.getenv("DOMINAITE_KEY_ID"))
            .secret(System.getenv("DOMINAITE_SECRET"))
            .baseUrl(System.getenv("DOMINAITE_BASE_URL"))
            .onWarning(message -> log.warn(message))
            .onDebug(line -> log.debug(line))   // leave out in production
            .build();
    }

    private static void retryLater(String idempotencyKey) {
    }

    private static void fulfil(String orderReference, String transactionId) {
    }

    private static void storeWebhookSecret(String secret) {
    }

    private static void book(TransactionItem item) {
    }
}
