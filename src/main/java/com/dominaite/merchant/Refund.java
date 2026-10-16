package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * A refund of a payment.
 */
public final class Refund {

    private String refundId;
    private String transactionId;
    private String status;
    private Long amount;
    private String currency;
    private String failureCode;
    private String failureMessage;
    private Instant completedAt;
    @JsonIgnore
    private JsonNode raw;

    private Refund() {
    }

    public String getRefundId() {
        return refundId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    /** One of {@link RefundStatuses}. */
    public String getStatus() {
        return status;
    }

    public Long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    /** One of {@link ErrorCodes#REFUND_FAILURE}, or null. */
    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }

    /** True when the status is {@code succeeded}. */
    public boolean isSucceeded() {
        return RefundStatuses.SUCCEEDED.equals(status);
    }

    /** True when the status does not change any more by itself. */
    public boolean isTerminal() {
        return RefundStatuses.SUCCEEDED.equals(status) || RefundStatuses.FAILED.equals(status);
    }
}
