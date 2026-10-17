package com.dominaite.merchant.samples;

import com.dominaite.merchant.ChargeException;
import com.dominaite.merchant.ChargeRequest;
import com.dominaite.merchant.ChargeStatuses;
import com.dominaite.merchant.CheckoutSession;
import com.dominaite.merchant.CheckoutSessionRequest;
import com.dominaite.merchant.CheckoutStatus;
import com.dominaite.merchant.CreateWebhookEndpointRequest;
import com.dominaite.merchant.Customer;
import com.dominaite.merchant.DominaiteClient;
import com.dominaite.merchant.ListTransactionsRequest;
import com.dominaite.merchant.PaymentMethodCharge;
import com.dominaite.merchant.PingResponse;
import com.dominaite.merchant.Refund;
import com.dominaite.merchant.RefundList;
import com.dominaite.merchant.RefundRequest;
import com.dominaite.merchant.TransactionItem;
import com.dominaite.merchant.TransactionPage;
import com.dominaite.merchant.WebhookEndpoint;
import com.dominaite.merchant.WebhookEvent;
import com.dominaite.merchant.WebhookVerificationException;
import com.dominaite.merchant.Webhooks;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The Java samples of the Dominaite merchant integration guide, one method each, compiled against
 * the public API. Each method body is the sample as the guide shows it, without its import lines.
 * Nothing here runs.
 */
@SuppressWarnings("unused")
final class GuideSamples {

    private GuideSamples() {
    }

    // Guide sample 1.
    static void clients() {
        // The SDK reads no environment variables. baseUrl is required: the sandbox here.
        DominaiteClient payments = DominaiteClient.builder()
            .keyId(System.getenv("DOMINAITE_KEY_ID"))              // payments key
            .secret(System.getenv("DOMINAITE_SECRET"))
            .baseUrl("https://api.staging.dominaite.com/payments")
            .build();

        DominaiteClient reporting = DominaiteClient.builder()
            .keyId(System.getenv("DOMINAITE_REPORTING_KEY_ID"))    // reporting key
            .secret(System.getenv("DOMINAITE_REPORTING_SECRET"))
            .baseUrl("https://api.staging.dominaite.com/payments")
            .build();

        PingResponse ping = payments.ping();   // ping.getClockSkewSeconds() must be near 0
    }

    // Guide sample 2.
    static void createSession(DominaiteClient payments) {
        CheckoutSession session = payments.createCheckoutSession(CheckoutSessionRequest.builder()
            .amount(5000)                       // 5,000 Ft
            .currency("HUF")
            .orderReference("member-1042-first")
            .saveCard(true)
            .customer(Customer.builder().email("anna.kovacs@example.com").build())
            .idempotencyKey("member-1042-first")
            .build());
        // session.getTransactionId(), getCashierKey(), getCashierToken(), getExpiresAt()
    }

    // Guide sample 3.
    static void registerWebhook(DominaiteClient reporting) {
        WebhookEndpoint endpoint = reporting.createWebhookEndpoint(CreateWebhookEndpointRequest.builder()
            .url("https://your-server.example/webhooks/dominaite")
            .events(List.of(
                "payment.succeeded", "payment.failed", "payment.abandoned", "payment.refunded", "payment.disputed",
                "charge.succeeded", "charge.failed",
                "verification.succeeded", "verification.failed"))
            .idempotencyKey("webhook-endpoint-1")
            .build());
        // endpoint.getId(), endpoint.getSecret() ("whsec_..." in this first answer only)
    }

    // Guide sample 4.
    // The SDK has no HTTP server. Call this from your framework's handler with the raw
    // body bytes and the X-Webhook-Signature header. It returns the status to send.
    static int handleWebhook(byte[] rawBody, String signatureHeader) {
        WebhookEvent event;
        try {
            event = Webhooks.verifyAndParse(rawBody, signatureHeader, System.getenv("DOMINAITE_WEBHOOK_SECRET"));
        } catch (WebhookVerificationException e) {
            return 400;
        }
        // event.getId(), event.getType(), event.getCreatedAt(), event.getData()
        // 1. Skip the event if you already processed event.getId().
        // 2. Save the event. 3. Answer 200. 4. Do the work after the answer.
        return 200;
    }

    // Guide sample 5.
    static void readStatus(DominaiteClient payments, CheckoutSession session) {
        CheckoutStatus status = payments.getStatus(session.getTransactionId());
        if (status.isPaid() && status.getStoredPaymentMethod() != null) {   // isPaid(): status is "succeeded"
            String savedCardId = status.getStoredPaymentMethod().getId();    // "pm_..."
        }
    }

    // Guide sample 6.
    static void refunds(DominaiteClient payments, String transactionId) {
        // Partial refund of 2,000 Ft
        Refund refund = payments.createRefund(transactionId, RefundRequest.builder()
            .amount(2000L)
            .reason("Member request")
            .idempotencyKey("refund-member-1042-2026-11-a")
            .build());

        // Full refund of what is left: leave amount out
        Refund full = payments.createRefund(transactionId, RefundRequest.builder()
            .idempotencyKey("refund-member-1042-2026-11-b")
            .build());

        RefundList list = payments.listRefunds(transactionId);
        // list.getRefundedAmount(), list.getRefundableAmount(), list.getRefunds()
    }

    // Guide sample 7.
    static void chargeCard(DominaiteClient payments) {
        try {
            PaymentMethodCharge charge = payments.chargePaymentMethod("pm_...", ChargeRequest.builder()
                .amount(5000)
                .currency("HUF")
                .orderReference("member-1042-2026-11")
                .idempotencyKey("member-1042-2026-11-try1")
                .build());

            if (ChargeStatuses.SUCCEEDED.equals(charge.getStatus())) { /* paid */ }
            if (ChargeStatuses.PENDING.equals(charge.getStatus()))   { /* wait for the webhook */ }
            if (ChargeStatuses.FAILED.equals(charge.getStatus()))    { /* the bank declined: read charge.getDeclineClass() */ }
        } catch (ChargeException e) {
            // nothing was decided: read e.getCode()
        }
    }

    // Guide sample 8.
    static void cardCheck(DominaiteClient payments) {
        CheckoutSession session = payments.createCheckoutSession(CheckoutSessionRequest.builder()
            .verifyOnly(true)
            .amount(0)
            .currency("HUF")
            .orderReference("member-1042-card")
            .idempotencyKey("member-1042-card")
            .build());
    }

    // Guide sample 9.
    static void settlementDay(DominaiteClient reporting) {
        // All payments that the acquirer settled on 13 October 2026. SDK 0.5.1 or later.
        List<TransactionItem> rows = new ArrayList<>();
        ListTransactionsRequest request = ListTransactionsRequest.builder()
            .from(Instant.parse("2026-09-13T00:00:00Z"))   // created window, at most 31 days
            .to(Instant.parse("2026-10-14T00:00:00Z"))
            .settledFrom(LocalDate.parse("2026-10-13"))    // settlement window: one day
            .settledTo(LocalDate.parse("2026-10-14"))
            .limit(100)
            .build();
        while (true) {
            TransactionPage page = reporting.listTransactions(request);
            rows.addAll(page.getItems());   // getFee(), getNetAmount(), getSettledAt(), getPayoutId(): null until settled
            if (page.getNextCursor() == null) {
                break;
            }
            request = request.withCursor(page.getNextCursor());
        }
    }
}
