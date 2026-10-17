package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** What the client puts on the wire, against a real loopback HTTP server. */
class ClientWireTest {

    static final String TX = "0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0";

    static final String SESSION = "{\"success\":true,\"checkout\":{\"transactionId\":\"" + TX + "\","
        + "\"orderId\":\"dom_9a8b7c6d5e4f\",\"cashierKey\":\"ck_live_2f3a4d5e6f708192\","
        + "\"cashierToken\":\"ctok_5e4f3a2b1c0d9e8f\",\"amount\":8440,\"currency\":\"EUR\","
        + "\"expiresAt\":\"2026-08-21T11:15:30.000Z\",\"integration\":\"widget\",\"futureField\":1}}";

    static CheckoutSessionRequest.Builder session() {
        return CheckoutSessionRequest.builder()
            .amount(8440)
            .currency("EUR")
            .orderReference("order-1042")
            .idempotencyKey("checkout-order-1042-8440-EUR");
    }

    @Test
    void createSendsASignedRequestAndReturnsTheSession() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(201, SESSION);

            CheckoutSession created = server.client().createCheckoutSession(session().build());

            MockServer.Recorded request = server.last();
            assertEquals("POST", request.method);
            assertEquals("/payments/merchant-api/checkout/sessions", request.path);
            assertEquals(MockServer.KEY_ID, request.header("X-Api-Key-Id"));
            assertEquals("checkout-order-1042-8440-EUR", request.header("Idempotency-Key"));
            assertEquals("application/json", request.header("Content-Type"));
            assertEquals("dominaite-java/" + DominaiteClient.VERSION, request.header("User-Agent"));
            assertEquals("{\"amount\":8440,\"currency\":\"EUR\",\"orderReference\":\"order-1042\"}", request.body);
            assertTrue(Math.abs(Long.parseLong(request.header("X-Timestamp")) - Instant.now().getEpochSecond()) < 5);
            // The base URL's /payments prefix is not part of the signed path.
            assertEquals(request.expectedSignature(), request.header("X-Signature"));

            assertEquals(TX, created.getTransactionId());
            assertEquals("ck_live_2f3a4d5e6f708192", created.getCashierKey());
            assertEquals(8440, created.getAmount());
            assertEquals(Instant.parse("2026-08-21T11:15:30Z"), created.getExpiresAt());
            assertEquals(1, created.getRaw().get("futureField").asInt());
            assertFalse(created.toString().contains("ctok_5e4f3a2b1c0d9e8f"));
        }
    }

    @Test
    void everyModelledSessionFieldAndTheExtraFieldsReachTheBody() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(201, SESSION);

            server.client().createCheckoutSession(session()
                .customer(Customer.builder().firstName("Anna").email("anna@example.com").build())
                .country("HU")
                .language("hu")
                .theme("dark")
                .description("Order 1042")
                .locationId(java.util.UUID.fromString("11111111-2222-4333-8444-555555555555"))
                .saveCard(true)
                .integration(CheckoutIntegrations.FIELDS)
                .extra("futureField", Map.of("a", 1))
                .extra("currency", "USD")
                .build());

            assertEquals("{\"amount\":8440,\"currency\":\"EUR\",\"orderReference\":\"order-1042\","
                + "\"customer\":{\"firstName\":\"Anna\",\"email\":\"anna@example.com\"},\"country\":\"HU\","
                + "\"language\":\"hu\",\"theme\":\"dark\",\"description\":\"Order 1042\","
                + "\"locationId\":\"11111111-2222-4333-8444-555555555555\",\"saveCard\":true,"
                + "\"integration\":\"fields\",\"futureField\":{\"a\":1}}", server.last().body);
            assertEquals(server.last().expectedSignature(), server.last().header("X-Signature"));
        }
    }

    @Test
    void anAgreementSessionSendsTheConsent() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(201, SESSION);

            server.client().createCheckoutSession(session()
                .agreementId("agr_123")
                .consent(AgreementConsent.builder()
                    .textVersion("2026-09-01")
                    .acceptedAt(Instant.parse("2026-10-01T10:00:00Z"))
                    .ip("203.0.113.7")
                    .build())
                .build());

            assertTrue(server.last().body.endsWith("\"agreementId\":\"agr_123\",\"consent\":{\"textVersion\":"
                + "\"2026-09-01\",\"acceptedAt\":\"2026-10-01T10:00:00Z\",\"ip\":\"203.0.113.7\"}}"));
        }
    }

    @Test
    void aVerifyOnlySessionSendsAmountZero() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(201, SESSION);

            server.client().createCheckoutSession(session().amount(0).verifyOnly(true).build());

            assertEquals("{\"amount\":0,\"currency\":\"EUR\",\"orderReference\":\"order-1042\",\"verifyOnly\":true}",
                server.last().body);
        }
    }

    @Test
    void aGetSignsAnEmptyKeyAndAnEmptyBodyAndSendsNoKeyHeader() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"transactionId\":\"" + TX + "\",\"status\":\"succeeded\",\"amount\":8440,"
                + "\"currency\":\"EUR\",\"isRecurring\":false,\"sequence\":4}");

            CheckoutStatus status = server.client().getStatus(" " + TX.toUpperCase() + " ");

            MockServer.Recorded request = server.last();
            assertEquals("GET", request.method);
            assertEquals("/payments/merchant-api/checkout/sessions/" + TX, request.path);
            assertNull(request.header("Idempotency-Key"));
            assertNull(request.header("Content-Type"));
            assertEquals("", request.body);
            assertEquals(request.expectedSignature(), request.header("X-Signature"));
            assertTrue(status.isPaid());
            assertTrue(status.isTerminal());
            assertEquals(4, status.getSequence());
        }
    }

    @Test
    void theStatusReadModelsTheRecurringAndWalletFields() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"transactionId\":\"" + TX + "\",\"status\":\"failed\",\"amount\":100,"
                + "\"currency\":\"EUR\",\"verifyOnly\":true,\"declineClass\":\"soft_funds\",\"isRecurring\":true,"
                + "\"agreement\":{\"id\":\"agr_1\",\"status\":\"past_due\",\"nextChargeAt\":\"2026-11-01T00:00:00+02:00\"},"
                + "\"sequence\":9,\"expiresAt\":\"2026-10-01T00:00:00Z\",\"pspReference\":\"psp-1\","
                + "\"paymentMethod\":\"wallet\",\"walletType\":\"apple_pay\",\"storedPaymentMethod\":{\"id\":\"pm_1\","
                + "\"brand\":\"visa\",\"last4\":\"4242\",\"expiryMonth\":12,\"expiryYear\":2030,\"status\":\"retired\","
                + "\"retiredReason\":\"chargeback\"}}");

            CheckoutStatus status = server.client().getStatus(TX);

            assertEquals(Boolean.TRUE, status.getVerifyOnly());
            assertEquals(DeclineClasses.SOFT_FUNDS, status.getDeclineClass());
            assertTrue(status.isRecurring());
            assertEquals("agr_1", status.getAgreement().getId());
            assertEquals(Instant.parse("2026-10-31T22:00:00Z"), status.getAgreement().getNextChargeAt());
            assertEquals("psp-1", status.getPspReference());
            assertEquals(WalletTypes.APPLE_PAY, status.getWalletType());
            assertEquals(StoredPaymentMethodRetiredReasons.CHARGEBACK, status.getStoredPaymentMethod().getRetiredReason());
            assertFalse(status.getStoredPaymentMethod().isChargeable());
            assertEquals(Integer.valueOf(12), status.getStoredPaymentMethod().getExpiryMonth());
        }
    }

    @Test
    void theChargeSignsItsPathAndModelsTheAgreementFields() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(201, "{\"chargeId\":\"ch_1\",\"status\":\"succeeded\",\"transactionId\":\"" + TX + "\","
                + "\"agreementId\":\"agr_1\",\"sequence\":2}");

            PaymentMethodCharge charge = server.client().chargePaymentMethod("pm_0123456789abcdef0123456789abcdef",
                ChargeRequest.builder().amount(2500).currency("EUR").orderReference("order-1043")
                    .description("Renewal").agreementId("agr_1").idempotencyKey("charge-1043")
                    .extra("futureField", true).build());

            MockServer.Recorded request = server.last();
            assertEquals("/payments/merchant-api/payment-methods/pm_0123456789abcdef0123456789abcdef/charges", request.path);
            assertEquals("{\"amount\":2500,\"currency\":\"EUR\",\"orderReference\":\"order-1043\","
                + "\"description\":\"Renewal\",\"agreementId\":\"agr_1\",\"futureField\":true}", request.body);
            assertEquals(request.expectedSignature(), request.header("X-Signature"));
            assertTrue(charge.isPaid());
            assertEquals("agr_1", charge.getAgreementId());
            assertEquals(2, charge.getSequence());
        }
    }

    @Test
    void aRevokeIsADeleteWithoutKeyOrBodyAndA204IsSuccess() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(204, "");

            server.client().revokePaymentMethod("pm_1");

            MockServer.Recorded request = server.last();
            assertEquals("DELETE", request.method);
            assertNull(request.header("Idempotency-Key"));
            assertEquals("", request.body);
            assertEquals(request.expectedSignature(), request.header("X-Signature"));
        }
    }

    @Test
    void refundsCreateReadAndList() throws Exception {
        try (MockServer server = new MockServer()) {
            String refund = "{\"refundId\":\"rf_1\",\"transactionId\":\"" + TX + "\",\"status\":\"pending\","
                + "\"amount\":500,\"currency\":\"EUR\"}";
            server.respondData(201, refund).respondData(200, refund)
                .respondData(200, "{\"transactionId\":\"" + TX + "\",\"currency\":\"EUR\",\"refundedAmount\":500,"
                    + "\"refundableAmount\":7940,\"refunds\":[" + refund + "]}");
            DominaiteClient client = server.client();

            Refund created = client.createRefund(TX, RefundRequest.builder().amount(500L).reason("damaged")
                .idempotencyKey("refund-1").build());
            assertEquals("{\"amount\":500,\"reason\":\"damaged\"}", server.last().body);
            assertEquals("/payments/merchant-api/payments/" + TX + "/refunds", server.last().path);
            assertEquals("refund-1", server.last().header("Idempotency-Key"));
            assertEquals("rf_1", created.getRefundId());
            assertFalse(created.isTerminal());

            client.getRefund(TX, "rf_1");
            assertEquals("/payments/merchant-api/payments/" + TX + "/refunds/rf_1", server.last().path);

            RefundList list = client.listRefunds(TX);
            assertEquals(7940, list.getRefundableAmount());
            assertEquals(1, list.getRefunds().size());
            assertEquals("rf_1", list.getRefunds().get(0).getRaw().get("refundId").asText());
        }
    }

    @Test
    void aFullRefundSendsAnEmptyObject() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(201, "{\"refundId\":\"rf_1\",\"transactionId\":\"" + TX + "\",\"status\":\"pending\"}");

            server.client().createRefund(TX, RefundRequest.builder().idempotencyKey("refund-all").build());

            assertEquals("{}", server.last().body);
        }
    }

    @Test
    void plansCoverTheFiveMethodsAndPatchSendsNoKey() throws Exception {
        String plan = "{\"id\":\"pln_1\",\"name\":\"Gold\",\"amount\":999,\"currency\":\"EUR\",\"intervalUnit\":\"month\","
            + "\"intervalCount\":1,\"trialDays\":0,\"status\":\"active\",\"createdAt\":\"2026-10-01T00:00:00Z\"}";
        try (MockServer server = new MockServer()) {
            server.respondData(201, plan).respond(200, "{\"success\":true,\"data\":[" + plan + "]}")
                .respondData(200, plan).respondData(200, plan).respond(204, "");
            DominaiteClient client = server.client();

            Plan created = client.createPlan(CreatePlanRequest.builder().name("Gold").amount(999).currency("EUR")
                .intervalUnit(IntervalUnits.MONTH).idempotencyKey("plan-gold").build());
            assertEquals("{\"name\":\"Gold\",\"amount\":999,\"currency\":\"EUR\",\"intervalUnit\":\"month\","
                + "\"intervalCount\":1}", server.last().body);
            assertTrue(created.isActive());

            List<Plan> plans = client.listPlans();
            assertEquals(1, plans.size());
            assertEquals("pln_1", plans.get(0).getRaw().get("id").asText());

            client.getPlan("pln_1");
            assertEquals("/payments/merchant-api/plans/pln_1", server.last().path);

            client.updatePlan("pln_1", UpdatePlanRequest.builder().name("Gold 2").build());
            MockServer.Recorded patch = server.last();
            assertEquals("PATCH", patch.method);
            assertEquals("{\"name\":\"Gold 2\"}", patch.body);
            assertNull(patch.header("Idempotency-Key"));
            assertEquals(patch.expectedSignature(), patch.header("X-Signature"));

            client.deletePlan("pln_1");
            assertEquals("DELETE", server.last().method);
        }
    }

    @Test
    void anEmptyPlanUpdateIsRefusedBeforeSending() throws Exception {
        try (MockServer server = new MockServer()) {
            assertThrows(ValidationException.class,
                () -> server.client().updatePlan("pln_1", UpdatePlanRequest.builder().build()));
            assertTrue(server.requests().isEmpty());
        }
    }

    @Test
    void agreementsCreateReadAndCancel() throws Exception {
        String agreement = "{\"id\":\"agr_1\",\"planId\":\"pln_1\",\"customerReference\":\"cust-7\",\"status\":\"pending\","
            + "\"amount\":999,\"currency\":\"EUR\",\"intervalUnit\":\"month\",\"intervalCount\":1,\"trialDays\":0,"
            + "\"consent\":{\"textVersion\":\"2026-09-01\",\"ip\":\"203.0.113.7\",\"receivedFromIp\":\"198.51.100.1\"},"
            + "\"version\":1,\"sequence\":1}";
        try (MockServer server = new MockServer()) {
            server.respondData(201, agreement).respondData(200, agreement).respond(204, "");
            DominaiteClient client = server.client();

            Agreement created = client.createAgreement(CreateAgreementRequest.builder().planId("pln_1")
                .customerReference("cust-7").maxAmountPerPeriod(2000L).idempotencyKey("agr-cust-7").build());
            assertEquals("{\"planId\":\"pln_1\",\"customerReference\":\"cust-7\",\"maxAmountPerPeriod\":2000}",
                server.last().body);
            assertEquals("198.51.100.1", created.getConsent().getReceivedFromIp());

            client.getAgreement("agr_1");
            assertEquals("/payments/merchant-api/agreements/agr_1", server.last().path);

            client.cancelAgreement("agr_1");
            assertEquals("DELETE", server.last().method);
        }
    }

    @Test
    void webhookEndpointsCreateListAndDelete() throws Exception {
        String id = "6f1c2d3e-4a5b-4c6d-8e7f-001122334455";
        String endpoint = "{\"id\":\"" + id + "\",\"url\":\"https://shop.example/hooks\","
            + "\"events\":[\"payment.succeeded\"],\"status\":\"active\",\"secret\":\"whsec_abc\"}";
        try (MockServer server = new MockServer()) {
            server.respondData(201, endpoint).respond(200, "{\"success\":true,\"data\":[" + endpoint + "]}")
                .respond(204, "");
            DominaiteClient client = server.client();

            WebhookEndpoint created = client.createWebhookEndpoint(CreateWebhookEndpointRequest.builder()
                .url("https://shop.example/hooks").events(Arrays.asList(WebhookEventTypes.PAYMENT_SUCCEEDED))
                .idempotencyKey("hook-1").build());
            assertEquals("{\"url\":\"https://shop.example/hooks\",\"events\":[\"payment.succeeded\"]}", server.last().body);
            assertEquals("whsec_abc", created.getSecret());
            assertFalse(created.toString().contains("whsec_abc"));

            assertEquals(1, client.listWebhookEndpoints().size());

            client.deleteWebhookEndpoint(id.toUpperCase());
            assertEquals("/payments/merchant-api/webhooks/" + id, server.last().path);
        }
    }

    @Test
    void pingAndTheIntegrationContractAreReadable() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"pong\":true,\"merchantId\":\"m_1\",\"serverTime\":\"2026-10-16T10:00:00Z\","
                + "\"serverUnixTime\":1792144800,\"clockSkewSeconds\":-2}")
                .respondData(200, "{\"version\":\"2026-09-16\",\"apiBase\":\"https://api.dominaite.com/payments\","
                    + "\"statuses\":[\"pending\"],\"webhookEventCatalog\":[\"payment.succeeded\"],"
                    + "\"recurring\":{\"consentTextVersions\":[\"2026-09-01\"],\"intervalUnits\":[\"month\"]}}");
            DominaiteClient client = server.client();

            PingResponse ping = client.ping();
            assertTrue(ping.isPong());
            assertEquals(-2, ping.getClockSkewSeconds());

            IntegrationContract contract = client.getIntegrationContract();
            assertEquals("2026-09-16", contract.getVersion());
            assertEquals(Arrays.asList("2026-09-01"), contract.getRecurring().getConsentTextVersions());
            assertNotNull(contract.getRaw());
        }
    }

    @Test
    void aMissingOrBlankBaseUrlIsRefusedWithBothUrlsInTheMessage() {
        String expected = "baseUrl is required. Use https://api.staging.dominaite.com/payments for the sandbox"
            + " or https://api.dominaite.com/payments for production.";

        ValidationException unset = assertThrows(ValidationException.class,
            () -> DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET).build());
        assertEquals(expected, unset.getMessage());

        for (String blank : Arrays.asList(null, "", "  ", "\t\n", "/", " // ")) {
            ValidationException error = assertThrows(ValidationException.class,
                () -> DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
                    .baseUrl(blank).build(), "baseUrl " + blank);
            assertEquals(expected, error.getMessage());
        }
    }

    @Test
    void theNamedBaseUrlsAreTheSandboxAndProduction() {
        assertEquals("https://api.staging.dominaite.com/payments", DominaiteClient.SANDBOX_BASE_URL);
        assertEquals("https://api.dominaite.com/payments", DominaiteClient.PRODUCTION_BASE_URL);

        for (String base : Arrays.asList(DominaiteClient.SANDBOX_BASE_URL, DominaiteClient.PRODUCTION_BASE_URL)) {
            DominaiteClient client = DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
                .baseUrl(base).build();
            assertEquals(base, client.getBaseUrl());
        }
    }

    @Test
    void anExplicitBaseUrlIsUsedForTheRequest() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"merchantId\":\"m\",\"clockSkewSeconds\":0}");
            DominaiteClient client = DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
                .baseUrl(server.baseUrl() + "/").build();
            client.ping();
            assertEquals("/payments/merchant-api/ping", server.last().path);
        }
    }

    @Test
    void theBaseUrlLosesWhitespaceAndTrailingSlashes() {
        DominaiteClient custom = DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
            .baseUrl("  https://dev.example/api//  ").userAgentSuffix("shop/1.2").timeout(Duration.ofSeconds(5)).build();
        assertEquals("https://dev.example/api", custom.getBaseUrl());
        assertEquals("dominaite-java/" + DominaiteClient.VERSION + " shop/1.2", custom.getUserAgent());
        assertFalse(custom.toString().contains(MockServer.SECRET));
    }

    @Test
    void plainHttpIsRefusedOutsideLoopback() {
        for (String base : Arrays.asList("http://api.dominaite.com/payments", "HTTP://shop.example", "ftp://x.example",
                "api.dominaite.com/payments")) {
            ValidationException error = assertThrows(ValidationException.class,
                () -> DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
                    .baseUrl(base).build(), base);
            assertTrue(error.getMessage().startsWith("baseUrl must use https://"), error.getMessage());
        }
        for (String base : Arrays.asList("http://localhost:8080", "http://127.0.0.1:1", "http://[::1]:9000/x",
                "HTTPS://API.DOMINAITE.COM/payments")) {
            DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET).baseUrl(base).build();
        }
    }

    @Test
    void wrongCredentialPrefixesAreRefused() {
        assertThrows(ValidationException.class,
            () -> DominaiteClient.builder().keyId("key").secret(MockServer.SECRET)
                .baseUrl(DominaiteClient.SANDBOX_BASE_URL).build());
        assertThrows(ValidationException.class,
            () -> DominaiteClient.builder().keyId(MockServer.KEY_ID).secret("secret")
                .baseUrl(DominaiteClient.SANDBOX_BASE_URL).build());
    }
}
