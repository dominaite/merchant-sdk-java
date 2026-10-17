package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * The data of a {@code charge.*} event: one scheduled charge attempt.
 */
public final class ChargeEventData {

    private String chargeId;
    private String transactionId;
    private String storedPaymentMethodId;
    private String agreementId;
    private String customerReference;
    private String outcome;
    private Integer periodNumber;
    private Integer attemptNumber;
    private long amount;
    private String currency;
    private ChargeEventPaymentMethod paymentMethod;
    private String orderReference;
    private String description;
    private String declineClass;
    private String declineCode;
    private Instant nextAttemptAt;
    private Instant nextChargeAt;
    private Long sequence;
    @JsonIgnore
    private JsonNode raw;

    private ChargeEventData() {
    }

    public String getChargeId() {
        return chargeId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getStoredPaymentMethodId() {
        return storedPaymentMethodId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public String getOutcome() {
        return outcome;
    }

    public Integer getPeriodNumber() {
        return periodNumber;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    /** The charged card, or null when the event has none. */
    public ChargeEventPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public String getDescription() {
        return description;
    }

    public String getDeclineClass() {
        return declineClass;
    }

    public String getDeclineCode() {
        return declineCode;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getNextChargeAt() {
        return nextChargeAt;
    }

    public Long getSequence() {
        return sequence;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }

    /**
     * Order the events of one charge period by {@link #getSequence()} within this key:
     * {@code "{agreementId}/{periodNumber}"}, or the charge id without an agreement.
     */
    public String getOrderingKey() {
        if (agreementId != null && !agreementId.isEmpty()) {
            return agreementId + "/" + (periodNumber == null ? "" : periodNumber.toString());
        }
        return chargeId;
    }
}
