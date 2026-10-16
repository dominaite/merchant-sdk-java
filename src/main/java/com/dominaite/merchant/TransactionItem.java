package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * One transaction of {@link DominaiteClient#listTransactions}. The settlement fields are null until
 * the acquirer settles the payment, usually the next business day.
 */
public final class TransactionItem {

    private String transactionId;
    private String orderReference;
    private String type;
    private String status;
    private long amount;
    private String currency;
    private Instant createdAt;
    private Instant completedAt;
    private String storedPaymentMethodId;
    private Long refundedAmount;
    private Long fee;
    private Long netAmount;
    private Instant settledAt;
    private String payoutId;
    @JsonIgnore
    private JsonNode raw;

    private TransactionItem() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOrderReference() {
        return orderReference;
    }

    /** One of {@link TransactionTypes}. A verification is a card check, not a sale. */
    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    /** The amount in minor units. */
    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getStoredPaymentMethodId() {
        return storedPaymentMethodId;
    }

    public Long getRefundedAmount() {
        return refundedAmount;
    }

    /** The fee of this payment in minor units, or null until settled. It can change after it appears. */
    public Long getFee() {
        return fee;
    }

    /** The amount minus the fee of this payment only, or null until settled. Payout deductions are not in it. */
    public Long getNetAmount() {
        return netAmount;
    }

    /** When the acquirer settled the payment, or null. */
    public Instant getSettledAt() {
        return settledAt;
    }

    /** The payout this payment is in, or null. */
    public String getPayoutId() {
        return payoutId;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }
}
