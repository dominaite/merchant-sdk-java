package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The client-level retry policy: what is retried, how often, how long it waits, and with which key. */
class RetryTest {

    static final String TX = ClientWireTest.TX;
    static final String PM = ErrorMappingTest.PM;
    static final String CHARGE = "{\"chargeId\":\"ch_1\",\"status\":\"succeeded\",\"transactionId\":\"" + TX + "\"}";
    static final String REFUND = "{\"refundId\":\"rf_1\",\"transactionId\":\"" + TX + "\",\"status\":\"pending\"}";

    private final List<Duration> waits = Collections.synchronizedList(new ArrayList<>());

    /** A client that times out after 300 ms and records its waits instead of sleeping. */
    private DominaiteClient client(MockServer server) {
        return server.builder().timeout(Duration.ofMillis(300)).sleeper(waits::add).build();
    }

    /** Every attempt sent the same method, path, key and body, and each one is signed correctly. */
    private static void assertSameRequest(List<MockServer.Recorded> requests, String key) {
        MockServer.Recorded first = requests.get(0);
        for (MockServer.Recorded request : requests) {
            assertEquals(first.method, request.method);
            assertEquals(first.path, request.path);
            assertEquals(key, request.header("Idempotency-Key"));
            assertEquals(first.body, request.body);
            assertEquals(request.expectedSignature(), request.header("X-Signature"));
        }
    }

    @Test
    void aTimedOutChargeIsRetriedWithTheSameKeyAndBody() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondLate(2000).respondData(201, CHARGE);

            PaymentMethodCharge charge = client(server).chargePaymentMethod(PM, ErrorMappingTest.charge());

            assertEquals("ch_1", charge.getChargeId());
            assertEquals(2, server.requests().size());
            assertSameRequest(server.requests(), "charge-1043");
            assertEquals(1, waits.size());
        }
    }

    @Test
    void aTimedOutRefundIsRetriedWithTheSameKeyAndBody() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondLate(2000).respondData(202, REFUND);

            Refund refund = client(server).createRefund(TX, RefundRequest.builder().amount(500L)
                .idempotencyKey("refund-1").build());

            assertEquals("rf_1", refund.getRefundId());
            assertEquals(2, server.requests().size());
            assertSameRequest(server.requests(), "refund-1");
        }
    }

    @Test
    void every5xxWithoutARouteCodeAndProcessingUnavailableAreRetried() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(503, "MERCHANT_API_UNAVAILABLE", "down").respond(502, "<html>bad gateway</html>")
                .respondData(200, "{\"pong\":true,\"merchantId\":\"m_1\",\"clockSkewSeconds\":0}");
            assertTrue(client(server).ping().isPong());
            assertEquals(3, server.requests().size());
        }
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"success\":false,\"errorCode\":\"PAYMENT_PROCESSING_UNAVAILABLE\"}")
                .respondData(201, ClientWireTest.SESSION);
            assertEquals(TX, client(server).createCheckoutSession(ClientWireTest.session().build()).getTransactionId());
            assertSameRequest(server.requests(), "checkout-order-1042-8440-EUR");
        }
        try (MockServer server = new MockServer()) {
            server.respondError(503, "PAYMENT_PROCESSING_UNAVAILABLE", "off").respondData(201, CHARGE);
            assertEquals("ch_1", client(server).chargePaymentMethod(PM, ErrorMappingTest.charge()).getChargeId());
            assertEquals(2, server.requests().size());
        }
    }

    @Test
    void theRetriesStopAfterMaxRetriesAndTheLastErrorIsThrown() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(503, "A", "x").respondError(503, "B", "x").respondError(503, "C", "x")
                .respondData(200, CHARGE);

            TransportException error = assertThrows(TransportException.class,
                () -> client(server).chargePaymentMethod(PM, ErrorMappingTest.charge()));

            assertEquals("C", error.getCode());
            assertEquals("charge-1043", error.getIdempotencyKey());
            assertEquals(DominaiteClient.DEFAULT_MAX_RETRIES + 1, server.requests().size());
            assertEquals(DominaiteClient.DEFAULT_MAX_RETRIES, waits.size());
        }
    }

    @Test
    void maxRetriesZeroSendsOnce() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(503, "MERCHANT_API_UNAVAILABLE", "down").respondData(201, CHARGE);

            assertThrows(TransportException.class, () -> server.builder().maxRetries(0).sleeper(waits::add).build()
                .chargePaymentMethod(PM, ErrorMappingTest.charge()));

            assertEquals(1, server.requests().size());
            assertTrue(waits.isEmpty());
        }
    }

    @Test
    void a429IsNeverRetriedAndKeepsRetryAfter() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(429, "{\"success\":false,\"error\":{\"code\":\"CHARGE_VELOCITY_EXCEEDED\",\"message\":\"slow\"}}",
                Map.of("Retry-After", "7")).respondData(201, CHARGE);

            RateLimitedException error = assertThrows(RateLimitedException.class,
                () -> client(server).chargePaymentMethod(PM, ErrorMappingTest.charge()));

            assertEquals(Duration.ofSeconds(7), error.getRetryAfter());
            assertEquals(1, server.requests().size());
        }
    }

    @Test
    void refusalsAndRouteOwned5xxCodesAreNotRetried() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(502, "CHARGE_OUTCOME_UNKNOWN", "no verdict").respondData(201, CHARGE);
            assertThrows(ChargeException.class, () -> client(server).chargePaymentMethod(PM, ErrorMappingTest.charge()));
            assertEquals(1, server.requests().size());
        }
        try (MockServer server = new MockServer()) {
            server.respondError(404, "REFUND_NOT_FOUND", "not yet").respondData(200, REFUND);
            assertThrows(RefundException.class, () -> client(server).getRefund(TX, "rf_1"));
            assertEquals(1, server.requests().size());
        }
        try (MockServer server = new MockServer()) {
            server.respondError(401, "INVALID_SIGNATURE", "bad").respondData(200, "{}");
            assertThrows(AuthenticationException.class, () -> client(server).ping());
            assertEquals(1, server.requests().size());
        }
    }

    @Test
    void webhookCreatePatchAndDeleteAreNeverRetried() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(503, "MERCHANT_API_UNAVAILABLE", "down").respondError(503, "MERCHANT_API_UNAVAILABLE", "down")
                .respondError(503, "MERCHANT_API_UNAVAILABLE", "down").respondError(503, "MERCHANT_API_UNAVAILABLE", "down");
            DominaiteClient client = client(server);

            assertThrows(TransportException.class, () -> client.createWebhookEndpoint(CreateWebhookEndpointRequest.builder()
                .url("https://shop.example/hooks").events(Arrays.asList(WebhookEventTypes.PAYMENT_SUCCEEDED))
                .idempotencyKey("hook-1").build()));
            assertThrows(TransportException.class, () -> client.updatePlan("pln_1", UpdatePlanRequest.builder().name("x").build()));
            assertThrows(TransportException.class, () -> client.deletePlan("pln_1"));
            assertThrows(RevokeException.class, () -> client.revokePaymentMethod(PM));

            assertEquals(4, server.requests().size());
            assertTrue(waits.isEmpty());
        }
    }

    @Test
    void theWaitsStayInsideTheFullJitterBounds() throws Exception {
        Iterator<Double> draws = Arrays.asList(0.999999, 0.0, 0.5, 0.999999, 0.999999, 0.999999).iterator();
        try (MockServer server = new MockServer()) {
            DominaiteClient client = server.builder().maxRetries(6).sleeper(waits::add).random(draws::next).build();

            assertThrows(TransportException.class, client::ping);

            assertEquals(7, server.requests().size());
            // Retry n waits random x min(500 ms x 2^n, 8 s).
            assertEquals(Arrays.asList(Duration.ofMillis(499), Duration.ZERO, Duration.ofMillis(1000),
                Duration.ofMillis(3999), Duration.ofMillis(7999), Duration.ofMillis(7999)), waits);
        }
    }

    @Test
    void theBackoffNeverLeavesItsBounds() {
        for (int retry = 0; retry < 40; retry++) {
            long ceiling = Math.min(8000L, 500L * (1L << Math.min(retry, 20)));
            assertEquals(Duration.ZERO, DominaiteClient.backoff(retry, DominaiteClient.RETRY_BASE_DELAY, 0.0));
            Duration top = DominaiteClient.backoff(retry, DominaiteClient.RETRY_BASE_DELAY, Math.nextDown(1.0));
            assertTrue(top.toMillis() < ceiling && top.toMillis() >= ceiling - 1, retry + ": " + top);
        }
    }

    @Test
    void theDeprecatedHelperUsesItsOwnCountInsteadOfAddingToTheClients() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(503, "A", "x").respondError(503, "B", "x").respondError(503, "C", "x");

            @SuppressWarnings("deprecation")
            Runnable call = () -> client(server).createCheckoutSessionWithRetry(
                ClientWireTest.session().build(), 2, Duration.ofMillis(10));
            TransportException error = assertThrows(TransportException.class, call::run);

            assertEquals("B", error.getCode());
            assertEquals(2, server.requests().size());
        }
    }

    @Test
    void anInterruptedWaitKeepsTheFailureTheKeyAndTheRequestIdAndTheInterruptFlag() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondError(503, "PAYMENT_PROCESSING_UNAVAILABLE", "off").respondData(201, CHARGE);
            InterruptedException interrupt = new InterruptedException("stop");
            DominaiteClient client = server.builder().sleeper(delay -> {
                throw interrupt;
            }).build();

            TransportException error;
            try {
                error = assertThrows(TransportException.class,
                    () -> client.chargePaymentMethod(PM, ErrorMappingTest.charge()));
            } finally {
                // Read and clear the flag, so it cannot leak into the next test.
                assertTrue(Thread.interrupted());
            }

            assertEquals(1, server.requests().size());
            assertEquals("charge-1043", error.getIdempotencyKey());
            assertEquals("req-123", error.getRequestId());
            assertEquals(Integer.valueOf(503), error.getHttpStatus());
            assertEquals("PAYMENT_PROCESSING_UNAVAILABLE", error.getCode());
            assertTrue(error.getCause() instanceof ChargeException);
            assertEquals("req-123", ((DominaiteException) error.getCause()).getRequestId());
            assertSame(interrupt, error.getSuppressed()[0]);
        }
    }

    @Test
    void aNegativeMaxRetriesIsRefused() {
        assertThrows(ValidationException.class, () -> DominaiteClient.builder().keyId(MockServer.KEY_ID)
            .secret(MockServer.SECRET).baseUrl(DominaiteClient.SANDBOX_BASE_URL).maxRetries(-1).build());
        assertEquals(2, DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
            .baseUrl(DominaiteClient.SANDBOX_BASE_URL).build().getMaxRetries());
    }
}
