package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * The status of a payment, read with {@link DominaiteClient#getStatus}. Status values are
 * plain strings: see {@link TransactionStatuses}.
 */
public final class CheckoutStatus {

    private String transactionId;
    private String orderId;
    private String orderReference;
    private String pspReference;
    private String status;
    private long amount;
    private String currency;
    private Long refundedAmount;
    private String paymentMethod;
    private String walletType;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
    private StoredPaymentMethod storedPaymentMethod;
    private String declineClass;
    private boolean isRecurring;
    private Boolean verifyOnly;
    private CheckoutStatusAgreement agreement;
    private long sequence;
    @JsonIgnore
    private JsonNode raw;

    private CheckoutStatus() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderReference() {
        return orderReference;
    }

    /** The acquirer's reference, or null. */
    public String getPspReference() {
        return pspReference;
    }

    public String getStatus() {
        return status;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getRefundedAmount() {
        return refundedAmount;
    }

    /** One of {@link PaymentMethodCategories}, or null. */
    public String getPaymentMethod() {
        return paymentMethod;
    }

    /** One of {@link WalletTypes}, or null. */
    public String getWalletType() {
        return walletType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    /** The saved card, or null until a saveCard session is paid. */
    public StoredPaymentMethod getStoredPaymentMethod() {
        return storedPaymentMethod;
    }

    /** One of {@link DeclineClasses}, or null. A decline can have no class. */
    public String getDeclineClass() {
        return declineClass;
    }

    /** True for a charge of a stored payment method. */
    public boolean isRecurring() {
        return isRecurring;
    }

    public Boolean getVerifyOnly() {
        return verifyOnly;
    }

    public CheckoutStatusAgreement getAgreement() {
        return agreement;
    }

    /** Grows with every change. Ignore a read with a lower sequence than one you have. */
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
        return TransactionStatuses.SUCCEEDED.equals(status);
    }

    /** True when the status does not change any more by itself. */
    public boolean isTerminal() {
        return TransactionStatuses.SUCCEEDED.equals(status)
            || TransactionStatuses.FAILED.equals(status)
            || TransactionStatuses.REFUNDED.equals(status)
            || TransactionStatuses.PARTIALLY_REFUNDED.equals(status)
            || TransactionStatuses.CANCELLED.equals(status)
            || TransactionStatuses.ABANDONED.equals(status);
    }
}
