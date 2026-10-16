package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The data of a {@code payment.*} or {@code verification.*} event.
 */
public final class PaymentEventData {

    private String transactionId;
    private String status;
    private String previousStatus;
    private String kind;
    private long amount;
    private long grossAmount;
    private Long surchargeAmount;
    private String currency;
    private String paymentMethod;
    private String walletType;
    private String originalTransactionId;
    private String idempotencyKey;
    private String orderReference;
    private String orderId;
    private String pspReference;
    private String description;
    private String paymentMethodBrand;
    private String paymentMethodLast4;
    private StoredPaymentMethod storedPaymentMethod;
    private boolean verifyOnly;
    private String declineClass;
    @JsonIgnore
    private JsonNode raw;

    private PaymentEventData() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getKind() {
        return kind;
    }

    public long getAmount() {
        return amount;
    }

    public long getGrossAmount() {
        return grossAmount;
    }

    public Long getSurchargeAmount() {
        return surchargeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getWalletType() {
        return walletType;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPspReference() {
        return pspReference;
    }

    public String getDescription() {
        return description;
    }

    public String getPaymentMethodBrand() {
        return paymentMethodBrand;
    }

    public String getPaymentMethodLast4() {
        return paymentMethodLast4;
    }

    public StoredPaymentMethod getStoredPaymentMethod() {
        return storedPaymentMethod;
    }

    public boolean isVerifyOnly() {
        return verifyOnly;
    }

    /** One of {@link DeclineClasses}, or null. */
    public String getDeclineClass() {
        return declineClass;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }
}
