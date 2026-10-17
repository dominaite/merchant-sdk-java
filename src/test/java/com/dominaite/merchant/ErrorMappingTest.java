package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** How each answer maps onto the exception hierarchy, and what stays a result. */
class ErrorMappingTest {

    static final String TX = ClientWireTest.TX;
    static final String PM = "pm_0123456789abcdef0123456789abcdef";

    static ChargeRequest charge() {
        return ChargeRequest.builder().amount(2500).currency("EUR").orderReference("order-1043")
            .idempotencyKey("charge-1043").build();
    }

    @Test
    void a401IsAnAuthenticationErrorWithTheClockHint() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(401, "{\"success\":false,\"error\":{\"code\":\"INVALID_SIGNATURE\"}}");

            AuthenticationException error = assertThrows(AuthenticationException.class, () -> server.client().ping());

            assertEquals(ErrorCodes.INVALID_SIGNATURE, error.getCode());
            assertEquals(Integer.valueOf(401), error.getHttpStatus());
            assertTrue(error.getMessage().contains("server clock"));
            assertFalse(error.isRetryable());
        }
    }

    @Test
    void a403InsufficientScopeHasItsOwnTypeAndNoClockHint() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(403, "INSUFFICIENT_SCOPE", "This route requires the transactions:read scope.");

            InsufficientScopeException error = assertThrows(InsufficientScopeException.class,
                () -> server.client().listTransactions(ListTransactionsRequest.builder()
                    .from(TransactionTest.FROM).to(TransactionTest.TO).build()));

            assertEquals(ErrorCodes.INSUFFICIENT_SCOPE, error.getCode());
            assertEquals(Integer.valueOf(403), error.getHttpStatus());
            assertEquals("This route requires the transactions:read scope. Use an API key that has this scope.",
                error.getMessage());
            assertFalse(error.getMessage().contains("clock"));
            assertEquals("req-123", error.getRequestId());
        }
    }

    @Test
    void a403VerifyOnlyNotEnabledSaysWeSwitchItOn() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(403, "VERIFY_ONLY_NOT_ENABLED", "Verify-only is off.");

            PermissionException error = assertThrows(PermissionException.class, () -> server.client()
                .createCheckoutSession(ClientWireTest.session().amount(0).verifyOnly(true).build()));

            assertFalse(error instanceof InsufficientScopeException);
            assertEquals(ErrorCodes.VERIFY_ONLY_NOT_ENABLED, error.getCode());
            assertTrue(error.getMessage().contains("We switch it on for your account."));
            assertEquals("checkout-order-1042-8440-EUR", error.getIdempotencyKey());
        }
    }

    @Test
    void a429CarriesRetryAfterAndIsNotRetriedBySdk() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(429, "{\"success\":false,\"error\":{\"code\":\"CHARGE_VELOCITY_EXCEEDED\","
                + "\"message\":\"Limit full.\"},\"metadata\":{\"requestId\":\"req-429\"}}", Map.of("Retry-After", "17"));

            RateLimitedException error = assertThrows(RateLimitedException.class,
                () -> server.client().chargePaymentMethod(PM, charge()));

            assertEquals(ErrorCodes.CHARGE_VELOCITY_EXCEEDED, error.getCode());
            assertEquals(Duration.ofSeconds(17), error.getRetryAfter());
            assertEquals("req-429", error.getRequestId());
            assertEquals("charge-1043", error.getIdempotencyKey());
            assertFalse(error.isRetryable());
            assertEquals(1, server.requests().size());
        }
    }

    @Test
    void a429FromAnEdgeWithoutJsonIsStillARateLimit() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(429, "<html>slow down</html>");

            RateLimitedException error = assertThrows(RateLimitedException.class, () -> server.client().ping());

            assertNull(error.getCode());
            assertNull(error.getRetryAfter());
        }
    }

    @Test
    void retryAfterAcceptsAnHttpDate() {
        assertEquals(Duration.ZERO, DominaiteClient.readRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT"));
        assertNull(DominaiteClient.readRetryAfter("soon"));
    }

    @Test
    void a402DeclineIsAResultNotAnException() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(402, "{\"success\":false,\"data\":{\"chargeId\":\"ch_1\",\"status\":\"failed\","
                + "\"declineClass\":null,\"declineCode\":\"05\",\"transactionId\":\"" + TX + "\",\"sequence\":1},"
                + "\"error\":{\"code\":\"CHARGE_DECLINED\",\"message\":\"Declined\"}}");

            PaymentMethodCharge declined = server.client().chargePaymentMethod(PM, charge());

            assertEquals(ChargeStatuses.FAILED, declined.getStatus());
            assertNull(declined.getDeclineClass());
            assertEquals("05", declined.getDeclineCode());
            assertFalse(declined.isPaid());
            assertTrue(declined.isTerminal());
        }
    }

    @Test
    void aCodedChargeRefusalIsAChargeException() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(409, "PAYMENT_METHOD_NOT_ACTIVE", "The payment method was revoked.");

            ChargeException error = assertThrows(ChargeException.class,
                () -> server.client().chargePaymentMethod(PM, charge()));

            assertEquals(ErrorCodes.PAYMENT_METHOD_NOT_ACTIVE, error.getCode());
            assertEquals(Integer.valueOf(409), error.getHttpStatus());
            assertEquals("charge-1043", error.getIdempotencyKey());
            assertTrue(error instanceof ApiException);
        }
    }

    @Test
    void aCodedCharge404IsAChargeException() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(404, "PAYMENT_METHOD_NOT_FOUND", "No stored payment method with this id.")
                .respondError(404, "AGREEMENT_NOT_FOUND", "No agreement with this id.");
            DominaiteClient client = server.client();

            ChargeException method = assertThrows(ChargeException.class, () -> client.chargePaymentMethod(PM, charge()));
            assertEquals(ErrorCodes.PAYMENT_METHOD_NOT_FOUND, method.getCode());
            assertEquals(Integer.valueOf(404), method.getHttpStatus());
            assertEquals("req-123", method.getRequestId());

            ChargeException agreement = assertThrows(ChargeException.class, () -> client.chargePaymentMethod(PM, charge()));
            assertEquals(ErrorCodes.AGREEMENT_NOT_FOUND, agreement.getCode());
        }
    }

    @Test
    void aCoded5xxThatTheRouteDoesNotOwnIsATransportErrorThatKeepsItsCode() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(500, "INTERNAL_ERROR", "An unexpected error occurred")
                .respondError(500, "INTERNAL_ERROR", "An unexpected error occurred");
            DominaiteClient client = server.client();

            TransportException charge = assertThrows(TransportException.class, () -> client.chargePaymentMethod(PM, charge()));
            assertEquals(ErrorCodes.INTERNAL_ERROR, charge.getCode());
            assertEquals(Integer.valueOf(500), charge.getHttpStatus());
            assertTrue(charge.isRetryable());

            TransportException revoke = assertThrows(TransportException.class, () -> client.revokePaymentMethod("pm_1"));
            assertEquals(ErrorCodes.INTERNAL_ERROR, revoke.getCode());
        }
    }

    @Test
    void everyChargeErrorExampleOfTheContractIsAChargeException() throws Exception {
        JsonNode endpoint = ContractTest.fixture("merchant-api-contract.json").get("endpoints").get("chargePaymentMethod");
        List<JsonNode> examples = new ArrayList<>();
        endpoint.get("errorExamples").forEach(examples::add);
        examples.add(endpoint.get("notFoundExample"));
        for (JsonNode example : examples) {
            String code = example.get("code").asText();
            try (MockServer server = new MockServer()) {
                server.respond(example.get("httpStatus").asInt(), example.get("body").toString());

                ChargeException error = assertThrows(ChargeException.class,
                    () -> server.client().chargePaymentMethod(PM, charge()), code);

                assertEquals(code, error.getCode());
                assertEquals(example.get("body").get("metadata").get("requestId").asText(), error.getRequestId(), code);
            }
        }
    }

    @Test
    void a400ValidationErrorIsAnApiExceptionWithTheEnvelope() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(400, "VALIDATION_ERROR", "currency is invalid");

            ApiException error = assertThrows(ApiException.class,
                () -> server.client().chargePaymentMethod(PM, charge()));

            assertFalse(error instanceof ChargeException);
            assertEquals("VALIDATION_ERROR", error.getCode());
            assertEquals("currency is invalid", error.getRawResult().get("error").get("message").asText());
        }
    }

    @Test
    void a5xxIsARetryableTransportError() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(503, "MERCHANT_API_UNAVAILABLE", "down");

            TransportException error = assertThrows(TransportException.class, () -> server.client().ping());

            assertEquals(Integer.valueOf(503), error.getHttpStatus());
            assertEquals(ErrorCodes.MERCHANT_API_UNAVAILABLE, error.getCode());
            assertTrue(error.isRetryable());
        }
    }

    @Test
    void a5xxWithoutJsonIsATransportErrorToo() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(502, "Bad gateway");

            assertThrows(TransportException.class, () -> server.client().ping());
        }
    }

    @Test
    void aRedirectIsAHardErrorAndIsNotFollowed() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(302, "", Map.of("Location", "https://evil.example/"));

            ApiException error = assertThrows(ApiException.class, () -> server.client().ping());

            assertEquals(Integer.valueOf(302), error.getHttpStatus());
            assertTrue(error.getMessage().contains("never redirects"));
            assertEquals(1, server.requests().size());
        }
    }

    @Test
    void anUnreachableHostIsARetryableTransportError() {
        DominaiteClient client = DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
            .baseUrl("http://127.0.0.1:1").timeout(Duration.ofSeconds(2)).build();

        TransportException error = assertThrows(TransportException.class, client::ping);

        assertTrue(error.isRetryable());
        assertNull(error.getHttpStatus());
    }

    @Test
    void aSessionRefusalNamesTheCollidingTransaction() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"success\":false,\"errorCode\":\"ALREADY_PROCESSED\","
                + "\"errorMessage\":\"Paid already.\",\"transactionId\":\"" + TX + "\"}");

            RefusalException error = assertThrows(RefusalException.class,
                () -> server.client().createCheckoutSession(ClientWireTest.session().build()));

            assertEquals(ErrorCodes.ALREADY_PROCESSED, error.getCode());
            assertEquals(TX, error.getTransactionId());
            assertEquals("checkout-order-1042-8440-EUR", error.getIdempotencyKey());
            assertEquals("req-ok", error.getRequestId());
            assertFalse(error.isRetryable());
        }
    }

    @Test
    void paymentProcessingUnavailableIsRetryable() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"success\":false,\"errorCode\":\"PAYMENT_PROCESSING_UNAVAILABLE\"}")
                .respondData(201, ClientWireTest.SESSION);

            CheckoutSession session = server.client()
                .createCheckoutSessionWithRetry(ClientWireTest.session().build(), 3, Duration.ofMillis(1));

            assertEquals(TX, session.getTransactionId());
            assertEquals(2, server.requests().size());
            assertEquals(server.requests().get(0).header("Idempotency-Key"), server.requests().get(1).header("Idempotency-Key"));
        }
    }

    @Test
    void theRetryStopsAtTheFirstNonRetryableError() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(409, "STOREFRONT_NOT_WHITELISTED", "Not whitelisted.");

            StorefrontException error = assertThrows(StorefrontException.class, () -> server.client()
                .createCheckoutSessionWithRetry(ClientWireTest.session().build(), 3, Duration.ofMillis(1)));

            assertEquals(ErrorCodes.STOREFRONT_NOT_WHITELISTED, error.getCode());
            assertEquals(1, server.requests().size());
        }
    }

    @Test
    void aStorefrontMismatchOnAReplayIsAStorefrontErrorToo() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"success\":false,\"errorCode\":\"STOREFRONT_MISMATCH\",\"transactionId\":\"" + TX + "\"}");

            StorefrontException error = assertThrows(StorefrontException.class,
                () -> server.client().createCheckoutSession(ClientWireTest.session().build()));

            assertEquals(TX, error.getTransactionId());
        }
    }

    @Test
    void refundRefusalsAreRefundExceptions() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(409, "REFUND_AMOUNT_EXCEEDED", "Too much.").respondError(404, "REFUND_NOT_FOUND", "Not yet.");
            DominaiteClient client = server.client();

            RefundException exceeded = assertThrows(RefundException.class, () -> client.createRefund(TX,
                RefundRequest.builder().amount(99999L).idempotencyKey("refund-1").build()));
            assertEquals(ErrorCodes.REFUND_AMOUNT_EXCEEDED, exceeded.getCode());
            assertFalse(exceeded.isRetryable());
            assertEquals("refund-1", exceeded.getIdempotencyKey());

            RefundException notFound = assertThrows(RefundException.class, () -> client.getRefund(TX, "rf_1"));
            assertTrue(notFound.isRetryable());
        }
    }

    @Test
    void aCodedRevokeRefusalIsARevokeException() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(502, "UPSTREAM_CONTRACT_ERROR", "Upstream.").respondError(404, "NOT_FOUND", "x");

            RevokeException error = assertThrows(RevokeException.class, () -> server.client().revokePaymentMethod("pm_1"));
            assertEquals(Integer.valueOf(502), error.getHttpStatus());
            // A 400 or 404 is a generic refusal, not a revoke code.
            ApiException generic = assertThrows(ApiException.class, () -> server.client().revokePaymentMethod("pm_1"));
            assertFalse(generic instanceof RevokeException);
        }
    }

    @Test
    void anUnexpectedBodyShapeIsAnApiExceptionNotACrash() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"transactionId\":\"" + TX + "\",\"amount\":\"not a number\"}");

            ApiException error = assertThrows(ApiException.class, () -> server.client().getStatus(TX));

            assertTrue(error.getMessage().contains("unexpected status response"));
        }
    }

    @Test
    void theSdkRefusesBadInputBeforeSending() throws Exception {
        try (MockServer server = new MockServer()) {
            DominaiteClient client = server.client();

            assertThrows(ValidationException.class,
                () -> client.createCheckoutSession(ClientWireTest.session().idempotencyKey(null).build()));
            assertThrows(ValidationException.class,
                () -> client.createCheckoutSession(ClientWireTest.session().amount(0).build()));
            assertThrows(ValidationException.class,
                () -> client.createCheckoutSession(ClientWireTest.session().amount(100).verifyOnly(true).build()));
            assertThrows(ValidationException.class,
                () -> client.createCheckoutSession(ClientWireTest.session().orderReference(" ").build()));
            assertThrows(ValidationException.class, () -> client.getStatus("not-a-uuid"));
            assertThrows(ValidationException.class, () -> client.chargePaymentMethod("pm/1", charge()));
            assertThrows(ValidationException.class, () -> client.deleteWebhookEndpoint("hook_1"));
            assertThrows(ValidationException.class, () -> client.createRefund(TX,
                RefundRequest.builder().amount(0L).idempotencyKey("r").build()));
            assertTrue(server.requests().isEmpty());
        }
    }
}
