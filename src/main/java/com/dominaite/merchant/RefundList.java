package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * The refunds of one payment.
 */
public final class RefundList extends ApiResult {

    private String transactionId;
    private String currency;
    private long refundedAmount;
    private long refundableAmount;
    private List<Refund> refunds;
    @JsonIgnore
    private JsonNode raw;

    private RefundList() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCurrency() {
        return currency;
    }

    public long getRefundedAmount() {
        return refundedAmount;
    }

    public long getRefundableAmount() {
        return refundableAmount;
    }

    public List<Refund> getRefunds() {
        return refunds;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }
}
