package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * A checkout session: hand {@link #getCashierKey()} and {@link #getCashierToken()} to the widget
 * on your page. Store {@link #getTransactionId()}.
 */
public final class CheckoutSession extends ApiResult {

    private String transactionId;
    private String orderId;
    private String cashierKey;
    private String cashierToken;
    private long amount;
    private String currency;
    private Instant expiresAt;
    private String integration;
    private String clientSecret;
    private Boolean verifyOnly;
    private String agreementId;
    @JsonIgnore
    private JsonNode raw;

    private CheckoutSession() {
    }

    /** Dominaite's payment id. Store it and poll the status with it. */
    public String getTransactionId() {
        return transactionId;
    }

    /** The provider-facing correlation id. You never need it. */
    public String getOrderId() {
        return orderId;
    }

    public String getCashierKey() {
        return cashierKey;
    }

    /** The widget credential. Never log it. */
    public String getCashierToken() {
        return cashierToken;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    /** When the session expires. */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /** {@link CheckoutIntegrations#WIDGET} or {@link CheckoutIntegrations#FIELDS}. */
    public String getIntegration() {
        return integration;
    }

    /** Only for card fields: the browser credential. Never log it. */
    public String getClientSecret() {
        return clientSecret;
    }

    public Boolean getVerifyOnly() {
        return verifyOnly;
    }

    public String getAgreementId() {
        return agreementId;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }

    @Override
    public String toString() {
        return "CheckoutSession{transactionId=" + transactionId + ", orderId=" + orderId + ", amount=" + amount
            + ", currency=" + currency + ", integration=" + integration
            + ", cashierToken=[REDACTED], clientSecret=[REDACTED]}";
    }
}
