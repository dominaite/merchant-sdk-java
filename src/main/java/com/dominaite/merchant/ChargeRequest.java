package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The body of {@link DominaiteClient#chargePaymentMethod}: a merchant-initiated charge of a
 * stored payment method. Immutable: build it with {@link #builder()}.
 */
@JsonPropertyOrder({"amount", "currency", "orderReference", "description", "agreementId"})
public final class ChargeRequest {

    private final long amount;
    private final String currency;
    private final String orderReference;
    private final String description;
    private final String agreementId;
    @JsonIgnore
    private final String idempotencyKey;
    @JsonIgnore
    private final Map<String, Object> extra;

    private ChargeRequest(Builder b) {
        this.amount = b.amount;
        this.currency = b.currency;
        this.orderReference = b.orderReference;
        this.description = b.description;
        this.agreementId = b.agreementId;
        this.idempotencyKey = b.idempotencyKey;
        this.extra = Collections.unmodifiableMap(new LinkedHashMap<>(b.extra));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with every field of this object, to change one and build again. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.amount = amount;
        b.currency = currency;
        b.orderReference = orderReference;
        b.description = description;
        b.agreementId = agreementId;
        b.idempotencyKey = idempotencyKey;
        b.extra.putAll(extra);
        return b;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public String getDescription() {
        return description;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    /** Builds a {@link ChargeRequest}. */
    public static final class Builder {

        private long amount;
        private String currency;
        private String orderReference;
        private String description;
        private String agreementId;
        private String idempotencyKey;
        private final Map<String, Object> extra = new LinkedHashMap<>();

        private Builder() {
        }

        /** The amount in MINOR units (2500 is 25.00 EUR). Must be positive. */
        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }

        /** The ISO 4217 code. It must be the currency the card was saved with. */
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        /** Your own order id, at most 100 characters. Required. */
        public Builder orderReference(String orderReference) {
            this.orderReference = orderReference;
            return this;
        }

        /** Free-text description stored on the transaction. */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** The agreement this charge belongs to. Optional. */
        public Builder agreementId(String agreementId) {
            this.agreementId = agreementId;
            return this;
        }

        /**
         * Required. It travels in the header and in the signature, never in the body. Derive it
         * from the order with {@link IdempotencyKeys#forOrder}.
         */
        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        /**
         * A body field this SDK version does not model yet. A field the request sets itself
         * wins over an extra one with the same name.
         */
        public Builder extra(String name, Object value) {
            this.extra.put(name, value);
            return this;
        }

        public ChargeRequest build() {
            return new ChargeRequest(this);
        }
    }
}
