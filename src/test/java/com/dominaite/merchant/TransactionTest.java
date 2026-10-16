package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** listTransactions: the query, the signed path without it, and the settlement fields. */
class TransactionTest {

    static final Instant FROM = Instant.parse("2026-10-01T00:00:00Z");
    static final Instant TO = Instant.parse("2026-11-01T00:00:00Z");

    static final String SETTLED = "{\"transactionId\":\"0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0\","
        + "\"orderReference\":\"order-1042\",\"type\":\"payment\",\"status\":\"succeeded\",\"amount\":10000,"
        + "\"currency\":\"EUR\",\"createdAt\":\"2026-10-12T09:00:00Z\",\"refundedAmount\":0,\"fee\":180,"
        + "\"netAmount\":9820,\"settledAt\":\"2026-10-13T00:00:00Z\",\"payoutId\":\"po_77\"}";

    static final String UNSETTLED = "{\"transactionId\":\"1f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0\","
        + "\"type\":\"verification\",\"status\":\"succeeded\",\"amount\":0,\"currency\":\"EUR\","
        + "\"createdAt\":\"2026-10-12T09:00:00Z\"}";

    @Test
    void theQueryIsInTheUrlAndNotInTheSignedPath() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"items\":[],\"nextCursor\":null}");

            server.client().listTransactions(ListTransactionsRequest.builder()
                .from(FROM).to(TO)
                .settledFrom(LocalDate.of(2026, 10, 13)).settledTo(LocalDate.of(2026, 10, 14))
                .status("succeeded").limit(50).cursor("abc+/=")
                .build());

            MockServer.Recorded request = server.last();
            assertEquals("GET", request.method);
            assertEquals("/payments/merchant-api/transactions", request.path);
            assertEquals("from=2026-10-01T00%3A00%3A00Z&to=2026-11-01T00%3A00%3A00Z&settledFrom=2026-10-13"
                + "&settledTo=2026-10-14&status=succeeded&limit=50&cursor=abc%2B%2F%3D", request.query);
            assertNull(request.header("Idempotency-Key"));
            // The server recomputes the signature over the bare path, the way the gateway does.
            assertEquals(request.expectedSignature(), request.header("X-Signature"));
            String withQuery = RequestSigner.sign(new SignatureInput(MockServer.SECRET, request.header("X-Timestamp"),
                "GET", "/merchant-api/transactions?" + request.query, "", ""));
            assertTrue(!withQuery.equals(request.header("X-Signature")));
        }
    }

    @Test
    void onlyTheSetParametersAreSentAndExtrasNeverOverrideThem() {
        String query = DominaiteClient.transactionQuery(ListTransactionsRequest.builder()
            .from(Instant.parse("2026-10-01T00:00:00.5Z")).to(TO)
            .extra("from", "ignored").extra("futureFilter", "a b")
            .build());

        assertEquals("from=2026-10-01T00%3A00%3A00.500Z&to=2026-11-01T00%3A00%3A00Z&futureFilter=a%20b", query);
    }

    @Test
    void theEscapeIsRfc3986() {
        assertEquals("a%20b~c-d_e.f%C3%BC", DominaiteClient.escape("a b~c-d_e.fü"));
    }

    @Test
    void fromAndToAreRequired() throws Exception {
        try (MockServer server = new MockServer()) {
            assertThrows(ValidationException.class,
                () -> server.client().listTransactions(ListTransactionsRequest.builder().from(FROM).build()));
            assertTrue(server.requests().isEmpty());
        }
    }

    @Test
    void settlementFieldsArePresentOnASettledPaymentAndNullOtherwise() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"items\":[" + SETTLED + "," + UNSETTLED + "],\"nextCursor\":\"c2\"}");

            TransactionPage page = server.client().listTransactions(
                ListTransactionsRequest.builder().from(FROM).to(TO).build());

            assertEquals("c2", page.getNextCursor());
            TransactionItem settled = page.getItems().get(0);
            assertEquals(Long.valueOf(180), settled.getFee());
            assertEquals(Long.valueOf(9820), settled.getNetAmount());
            assertEquals(settled.getAmount() - settled.getFee(), settled.getNetAmount().longValue());
            assertEquals(Instant.parse("2026-10-13T00:00:00Z"), settled.getSettledAt());
            assertEquals("po_77", settled.getPayoutId());
            assertEquals(Long.valueOf(0), settled.getRefundedAmount());
            assertEquals("po_77", settled.getRaw().get("payoutId").asText());

            TransactionItem unsettled = page.getItems().get(1);
            assertEquals(TransactionTypes.VERIFICATION, unsettled.getType());
            assertNull(unsettled.getFee());
            assertNull(unsettled.getNetAmount());
            assertNull(unsettled.getSettledAt());
            assertNull(unsettled.getPayoutId());
            assertNull(unsettled.getRefundedAmount());
            assertNull(unsettled.getOrderReference());
        }
    }

    @Test
    void withCursorPagesThroughTheWindow() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"items\":[" + SETTLED + "],\"nextCursor\":\"c2\"}")
                .respondData(200, "{\"items\":[" + UNSETTLED + "]}");
            DominaiteClient client = server.client();

            ListTransactionsRequest request = ListTransactionsRequest.builder().from(FROM).to(TO).build();
            int rows = 0;
            int pages = 0;
            while (true) {
                TransactionPage page = client.listTransactions(request);
                rows += page.getItems().size();
                pages++;
                if (page.getNextCursor() == null) {
                    break;
                }
                request = request.withCursor(page.getNextCursor());
            }

            assertEquals(2, rows);
            assertEquals(2, pages);
            assertTrue(server.last().query.endsWith("&cursor=c2"));
        }
    }
}
