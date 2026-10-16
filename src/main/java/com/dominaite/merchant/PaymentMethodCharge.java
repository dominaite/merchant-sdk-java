package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The result of {@link DominaiteClient#chargePaymentMethod}. A decline is a result too: the status
 * is {@code failed}, and {@link #getDeclineClass()} says what kind, or is null.
 */
public final class PaymentMethodCharge {

    private String chargeId;
    private String status;
    private String declineClass;
    private String declineCode;
    private String transactionId;
    private String agreementId;
    private long sequence;
    @JsonIgnore
    private JsonNode raw;

    private PaymentMethodCharge() {
    }

    public String getChargeId() {
        return chargeId;
    }

    /** One of {@link ChargeStatuses}. */
    public String getStatus() {
        return status;
    }

    /** One of {@link DeclineClasses}, or null. */
    public String getDeclineClass() {
        return declineClass;
    }

    public String getDeclineCode() {
        return declineCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public long getSequence() {
        return sequence;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }

    /** True when the status is {@code succeeded}. */
    public boolean isPaid() {
        return ChargeStatuses.SUCCEEDED.equals(status);
    }

    /** True when the status does not change any more by itself. */
    public boolean isTerminal() {
        return ChargeStatuses.SUCCEEDED.equals(status)
            || ChargeStatuses.FAILED.equals(status)
            || ChargeStatuses.CANCELLED.equals(status);
    }
}
