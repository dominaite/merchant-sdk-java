package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Every status, retired reason, decline class and event type is a plain string. A value the SDK
 * has never seen arrives as it is and never throws.
 */
class UnknownValuesTest {

    static final String TX = ClientWireTest.TX;

    @Test
    void anUnknownPaymentStatusDeclineClassAndRetiredReasonPassThrough() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"transactionId\":\"" + TX + "\",\"status\":\"teleported\",\"amount\":1,"
                + "\"currency\":\"EUR\",\"declineClass\":\"soft_cosmic_rays\",\"paymentMethod\":\"carrier_pigeon\","
                + "\"walletType\":\"moon_pay\",\"storedPaymentMethod\":{\"id\":\"pm_1\",\"status\":\"hibernating\","
                + "\"retiredReason\":\"lost_in_space\"},\"agreement\":{\"id\":\"agr_1\",\"status\":\"paused_forever\"}}");

            CheckoutStatus status = server.client().getStatus(TX);

            assertEquals("teleported", status.getStatus());
            assertFalse(status.isPaid());
            assertFalse(status.isTerminal());
            assertEquals("soft_cosmic_rays", status.getDeclineClass());
            assertEquals("carrier_pigeon", status.getPaymentMethod());
            assertEquals("moon_pay", status.getWalletType());
            assertEquals("hibernating", status.getStoredPaymentMethod().getStatus());
            assertFalse(status.getStoredPaymentMethod().isChargeable());
            assertEquals("lost_in_space", status.getStoredPaymentMethod().getRetiredReason());
            assertEquals("paused_forever", status.getAgreement().getStatus());
        }
    }

    @Test
    void unknownChargeRefundPlanAndTransactionValuesPassThrough() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(201, "{\"chargeId\":\"ch_1\",\"status\":\"quantum\",\"declineClass\":\"hard_but_soft\"}")
                .respondData(200, "{\"refundId\":\"rf_1\",\"status\":\"in_orbit\",\"failureCode\":\"NEW_CODE\"}")
                .respondData(200, "{\"id\":\"pln_1\",\"status\":\"frozen\",\"intervalUnit\":\"fortnight\"}")
                .respondData(200, "{\"items\":[{\"transactionId\":\"t\",\"type\":\"crypto\",\"status\":\"settling\"}]}");
            DominaiteClient client = server.client();

            PaymentMethodCharge charge = client.chargePaymentMethod("pm_1", ChargeRequest.builder().amount(1)
                .currency("EUR").orderReference("o").idempotencyKey("k").build());
            assertEquals("quantum", charge.getStatus());
            assertFalse(charge.isTerminal());

            Refund refund = client.getRefund(TX, "rf_1");
            assertEquals("in_orbit", refund.getStatus());
            assertEquals("NEW_CODE", refund.getFailureCode());

            Plan plan = client.getPlan("pln_1");
            assertEquals("fortnight", plan.getIntervalUnit());
            assertFalse(plan.isActive());

            TransactionItem item = client.listTransactions(ListTransactionsRequest.builder()
                .from(TransactionTest.FROM).to(TransactionTest.TO).build()).getItems().get(0);
            assertEquals("crypto", item.getType());
            assertEquals("settling", item.getStatus());
        }
    }

    @Test
    void anUnknownEventTypeParsesWithItsDataUntyped() {
        String body = "{\"id\":\"e9\",\"type\":\"payout.created\",\"apiVersion\":\"2027-01-01\","
            + "\"data\":{\"payoutId\":\"po_1\"},\"newTopLevel\":true}";
        String ts = WebhookTest.TS;
        String header = "t=" + ts + ",v1=" + WebhookTest.sign(ts, body);

        WebhookEvent event = Webhooks.verifyAndParse(
            body.getBytes(StandardCharsets.UTF_8), header, WebhookTest.SECRET, 300, WebhookTest.TIMESTAMP);

        assertEquals("payout.created", event.getType());
        assertNull(event.getPayment());
        assertNull(event.getAgreement());
        assertNull(event.getCharge());
        assertEquals("po_1", event.getData().get("payoutId").asText());
        assertEquals(true, event.getRaw().get("newTopLevel").asBoolean());
    }

    @Test
    void anUnknownPaymentEventStatusParses() {
        String body = "{\"id\":\"e1\",\"type\":\"payment.teleported\",\"data\":{\"transactionId\":\"t\","
            + "\"status\":\"teleported\",\"declineClass\":\"brand_new\",\"storedPaymentMethod\":{\"id\":\"pm_1\","
            + "\"status\":\"new_status\",\"retiredReason\":\"new_reason\"}}}";
        String ts = WebhookTest.TS;

        WebhookEvent event = Webhooks.verifyAndParse(
            body, "t=" + ts + ",v1=" + WebhookTest.sign(ts, body), WebhookTest.SECRET, 300, WebhookTest.TIMESTAMP);

        assertEquals("teleported", event.getPayment().getStatus());
        assertEquals("brand_new", event.getPayment().getDeclineClass());
        assertEquals("new_reason", event.getPayment().getStoredPaymentMethod().getRetiredReason());
    }
}
