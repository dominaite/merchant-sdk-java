package com.dominaite.merchant;

import java.util.List;

/**
 * The webhook event types. An event type the SDK has never seen still parses: it arrives in
 * {@link WebhookEvent#getType()} with its data in {@link WebhookEvent#getData()}.
 */
public final class WebhookEventTypes {

    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REQUIRES_CAPTURE = "payment.requires_capture";
    public static final String PAYMENT_CANCELLED = "payment.cancelled";
    public static final String PAYMENT_ABANDONED = "payment.abandoned";
    public static final String PAYMENT_REFUNDED = "payment.refunded";
    public static final String PAYMENT_DISPUTED = "payment.disputed";
    public static final String AGREEMENT_ACTIVATED = "agreement.activated";
    public static final String AGREEMENT_PAST_DUE = "agreement.past_due";
    public static final String AGREEMENT_CANCELLED = "agreement.cancelled";
    public static final String CHARGE_SUCCEEDED = "charge.succeeded";
    public static final String CHARGE_RETRYING = "charge.retrying";
    public static final String CHARGE_FAILED = "charge.failed";
    public static final String VERIFICATION_SUCCEEDED = "verification.succeeded";
    public static final String VERIFICATION_FAILED = "verification.failed";

    /** The payment events of the public contract's {@code webhookEventCatalog}, in its order. */
    public static final List<String> PAYMENT = ErrorCodes.list(
        PAYMENT_SUCCEEDED, PAYMENT_FAILED, PAYMENT_REQUIRES_CAPTURE, PAYMENT_CANCELLED,
        PAYMENT_ABANDONED, PAYMENT_REFUNDED, PAYMENT_DISPUTED);

    /** The subscription events of the public contract's {@code recurring.webhookEvents}, in its order. */
    public static final List<String> RECURRING = ErrorCodes.list(
        AGREEMENT_ACTIVATED, AGREEMENT_PAST_DUE, AGREEMENT_CANCELLED, CHARGE_SUCCEEDED, CHARGE_FAILED,
        CHARGE_RETRYING);

    private WebhookEventTypes() {
    }
}
