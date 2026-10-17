package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Every result and every error carries the request id: the {@code Dominaite-Request-Id} header
 * first, then {@code metadata.requestId} in the body. Production sends only the body field.
 */
class RequestIdTest {

    static final String TX = ClientWireTest.TX;
    static final String PM = ErrorMappingTest.PM;
    static final Map<String, String> HEADER = Map.of("Dominaite-Request-Id", "req-header");

    /** A success envelope with the body request id, and the header when asked. */
    static void answer(MockServer server, int status, String data, boolean header) {
        server.respond(status, "{\"success\":true,\"data\":" + data + ",\"metadata\":{\"requestId\":\"req-body\"}}",
            header ? HEADER : Map.of());
    }

    @Test
    void theHeaderWinsOverTheBody() throws Exception {
        try (MockServer server = new MockServer()) {
            answer(server, 200, ping(), true);
            server.respond(409, "{\"success\":false,\"error\":{\"code\":\"PAYMENT_METHOD_NOT_ACTIVE\",\"message\":\"x\"},"
                + "\"metadata\":{\"requestId\":\"req-body\"}}", HEADER);
            DominaiteClient client = server.client();

            assertEquals("req-header", client.ping().getRequestId());
            assertEquals("req-header", assertThrows(ChargeException.class,
                () -> client.chargePaymentMethod(PM, ErrorMappingTest.charge())).getRequestId());
        }
    }

    @Test
    void theBodyIsTheFallbackWhenTheHeaderIsAbsentOrBlank() throws Exception {
        try (MockServer server = new MockServer()) {
            answer(server, 200, ping(), false);
            server.respond(200, "{\"success\":true,\"data\":" + ping() + ",\"metadata\":{\"requestId\":\"req-body\"}}",
                Map.of("Dominaite-Request-Id", " "));
            server.respondError(409, "PAYMENT_METHOD_NOT_ACTIVE", "x");
            DominaiteClient client = server.client();

            assertEquals("req-body", client.ping().getRequestId());
            assertEquals("req-body", client.ping().getRequestId());
            assertEquals("req-123", assertThrows(ChargeException.class,
                () -> client.chargePaymentMethod(PM, ErrorMappingTest.charge())).getRequestId());
        }
    }

    @Test
    void noRequestIdAnywhereIsNull() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(200, "{\"success\":true,\"data\":" + ping() + "}");

            assertNull(server.client().ping().getRequestId());
        }
    }

    @Test
    void everyResultTypeCarriesTheRequestId() throws Exception {
        String plan = "{\"id\":\"pln_1\",\"name\":\"Gold\",\"amount\":999,\"currency\":\"EUR\",\"intervalUnit\":\"month\","
            + "\"intervalCount\":1,\"trialDays\":0,\"status\":\"active\"}";
        String agreement = "{\"id\":\"agr_1\",\"planId\":\"pln_1\",\"status\":\"pending\",\"amount\":999,\"currency\":\"EUR\"}";
        String endpoint = "{\"id\":\"6f1c2d3e-4a5b-4c6d-8e7f-001122334455\",\"url\":\"https://shop.example/hooks\","
            + "\"events\":[\"payment.succeeded\"],\"status\":\"active\"}";
        String refund = "{\"refundId\":\"rf_1\",\"transactionId\":\"" + TX + "\",\"status\":\"pending\",\"currency\":\"EUR\"}";
        List<Call> calls = Arrays.asList(
            new Call(ping(), DominaiteClient::ping),
            new Call("{\"version\":\"2026-09-16\"}", DominaiteClient::getIntegrationContract),
            new Call(ClientWireTest.SESSION, c -> c.createCheckoutSession(ClientWireTest.session().build())),
            new Call("{\"transactionId\":\"" + TX + "\",\"status\":\"pending\",\"amount\":1,\"currency\":\"EUR\"}",
                c -> c.getStatus(TX)),
            new Call("{\"chargeId\":\"ch_1\",\"status\":\"succeeded\",\"transactionId\":\"" + TX + "\"}",
                c -> c.chargePaymentMethod(PM, ErrorMappingTest.charge())),
            new Call(refund, c -> c.createRefund(TX, RefundRequest.builder().idempotencyKey("refund-1").build())),
            new Call(refund, c -> c.getRefund(TX, "rf_1")),
            new Call("{\"transactionId\":\"" + TX + "\",\"currency\":\"EUR\",\"refundedAmount\":0,\"refundableAmount\":1,"
                + "\"refunds\":[" + refund + "]}", c -> {
                    RefundList list = c.listRefunds(TX);
                    assertEquals(list.getRequestId(), list.getRefunds().get(0).getRequestId());
                    return list;
                }),
            new Call(plan, c -> c.createPlan(CreatePlanRequest.builder().name("Gold").amount(999).currency("EUR")
                .intervalUnit(IntervalUnits.MONTH).idempotencyKey("plan-1").build())),
            new Call("[" + plan + "]", c -> c.listPlans().get(0)),
            new Call(plan, c -> c.getPlan("pln_1")),
            new Call(plan, c -> c.updatePlan("pln_1", UpdatePlanRequest.builder().name("Gold 2").build())),
            new Call(agreement, c -> c.createAgreement(CreateAgreementRequest.builder().planId("pln_1")
                .customerReference("cust-1").idempotencyKey("agr-1").build())),
            new Call(agreement, c -> c.getAgreement("agr_1")),
            new Call(endpoint, c -> c.createWebhookEndpoint(CreateWebhookEndpointRequest.builder()
                .url("https://shop.example/hooks").events(Arrays.asList(WebhookEventTypes.PAYMENT_SUCCEEDED))
                .idempotencyKey("hook-1").build())),
            new Call("[" + endpoint + "]", c -> c.listWebhookEndpoints().get(0)),
            new Call("{\"items\":[],\"nextCursor\":null}", c -> c.listTransactions(ListTransactionsRequest.builder()
                .from(Instant.parse("2026-10-01T00:00:00Z")).to(Instant.parse("2026-10-02T00:00:00Z")).build())));

        for (boolean header : new boolean[] {true, false}) {
            for (Call call : calls) {
                try (MockServer server = new MockServer()) {
                    answer(server, 200, call.data, header);

                    ApiResult result = call.run.apply(server.client());

                    assertEquals(header ? "req-header" : "req-body", result.getRequestId(), call.data);
                }
            }
        }
    }

    @Test
    void errorsWithoutAJsonBodyTakeTheHeader() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(502, "Bad gateway", HEADER).respond(429, "", HEADER).respond(404, "<html>", HEADER);
            DominaiteClient client = server.client();

            assertEquals("req-header", assertThrows(TransportException.class, client::ping).getRequestId());
            assertEquals("req-header", assertThrows(RateLimitedException.class, client::ping).getRequestId());
            assertEquals("req-header", assertThrows(ApiException.class, client::ping).getRequestId());
        }
    }

    @Test
    void everyContractExampleWithARequestIdCarriesItThrough() throws Exception {
        JsonNode endpoints = ContractTest.fixture("merchant-api-contract.json").get("endpoints");
        JsonNode charge = endpoints.get("chargePaymentMethod");
        for (String name : new String[] {"successExample", "declinedExample"}) {
            JsonNode example = charge.get(name);
            try (MockServer server = new MockServer()) {
                server.respond(name.equals("declinedExample") ? 402 : 201, example.toString());
                assertEquals(example.get("metadata").get("requestId").asText(),
                    server.client().chargePaymentMethod(PM, ErrorMappingTest.charge()).getRequestId(), name);
            }
        }
        for (String name : new String[] {"partialExample", "fullExample"}) {
            JsonNode example = endpoints.get("createRefund").get(name);
            try (MockServer server = new MockServer()) {
                server.respond(202, example.toString());
                assertEquals(example.get("metadata").get("requestId").asText(), server.client()
                    .createRefund(TX, RefundRequest.builder().idempotencyKey("refund-1").build()).getRequestId(), name);
            }
        }
        for (String name : new String[] {"succeededExample", "failedExample"}) {
            JsonNode example = endpoints.get("getRefund").get(name);
            try (MockServer server = new MockServer()) {
                server.respond(200, example.toString());
                assertEquals(example.get("metadata").get("requestId").asText(),
                    server.client().getRefund(TX, "rf_1").getRequestId(), name);
            }
        }
        for (String route : new String[] {"createRefund", "getRefund", "revokePaymentMethod"}) {
            for (JsonNode example : endpoints.get(route).get("errorExamples")) {
                try (MockServer server = new MockServer()) {
                    server.respond(example.get("httpStatus").asInt(), example.get("body").toString());
                    DominaiteClient client = server.client();
                    DominaiteException error = assertThrows(DominaiteException.class, () -> {
                        if (route.equals("createRefund")) {
                            client.createRefund(TX, RefundRequest.builder().idempotencyKey("refund-1").build());
                        } else if (route.equals("getRefund")) {
                            client.getRefund(TX, "rf_1");
                        } else {
                            client.revokePaymentMethod(PM);
                        }
                    });
                    assertEquals(example.get("body").get("metadata").get("requestId").asText(), error.getRequestId(), route);
                    assertEquals(example.get("code").asText(), error.getCode(), route);
                }
            }
        }
    }

    private static String ping() {
        return "{\"pong\":true,\"merchantId\":\"m_1\",\"clockSkewSeconds\":0}";
    }

    private static final class Call {
        final String data;
        final Function<DominaiteClient, ApiResult> run;

        Call(String data, Function<DominaiteClient, ApiResult> run) {
            this.data = data;
            this.run = run;
        }
    }
}
