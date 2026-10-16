package com.dominaite.merchant;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A stored payment method charge was refused with a code from {@link ErrorCodes#CHARGE}. A
 * provider decline is not this exception: it comes back as a {@link PaymentMethodCharge} with
 * status {@code failed}.
 */
public final class ChargeException extends ApiException {

    private static final long serialVersionUID = 1L;

    private final transient PaymentMethodCharge charge;
    private final String transactionId;

    public ChargeException(int httpStatus, String code, String message, PaymentMethodCharge charge, JsonNode rawResult) {
        super(httpStatus, code, message, rawResult);
        this.charge = charge;
        this.transactionId = charge == null || charge.getTransactionId() == null || charge.getTransactionId().isEmpty()
            ? null
            : charge.getTransactionId();
    }

    /** The charge the response carried, or null. */
    public PaymentMethodCharge getCharge() {
        return charge;
    }

    /** The charge's transaction id, or null. */
    public String getTransactionId() {
        return transactionId;
    }
}
