package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The body of {@link DominaiteClient#createCheckoutSession}. Immutable: build it with
 * {@link #builder()}. Null fields are left out of the body.
 */
@JsonPropertyOrder({
    "amount", "currency", "orderReference", "customer", "country", "language", "theme", "description",
    "locationId", "saveCard", "agreementId", "consent", "integration", "verifyOnly"})
public final class CheckoutSessionRequest {

    private final long amount;
    private final String currency;
    private final String orderReference;
    private final Customer customer;
    private final String country;
    private final String language;
    private final String theme;
    private final String description;
    private final UUID locationId;
    private final Boolean saveCard;
    private final String agreementId;
    private final AgreementConsent consent;
    private final String integration;
    private final Boolean verifyOnly;
    @JsonIgnore
    private final String idempotencyKey;
    @JsonIgnore
    private final Map<String, Object> extra;

    private CheckoutSessionRequest(Builder b) {
        this.amount = b.amount;
        this.currency = b.currency;
        this.orderReference = b.orderReference;
        this.customer = b.customer;
        this.country = b.country;
        this.language = b.language;
        this.theme = b.theme;
        this.description = b.description;
        this.locationId = b.locationId;
        this.saveCard = b.saveCard;
        this.agreementId = b.agreementId;
        this.consent = b.consent;
        this.integration = b.integration;
        this.verifyOnly = b.verifyOnly;
        this.idempotencyKey = b.idempotencyKey;
        this.extra = Collections.unmodifiableMap(new LinkedHashMap<>(b.extra));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with every field of this request, to change one and build again. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.amount = amount;
        b.currency = currency;
        b.orderReference = orderReference;
        b.customer = customer;
        b.country = country;
        b.language = language;
        b.theme = theme;
        b.description = description;
        b.locationId = locationId;
        b.saveCard = saveCard;
        b.agreementId = agreementId;
        b.consent = consent;
        b.integration = integration;
        b.verifyOnly = verifyOnly;
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

    public Customer getCustomer() {
        return customer;
    }

    public String getCountry() {
        return country;
    }

    public String getLanguage() {
        return language;
    }

    public String getTheme() {
        return theme;
    }

    public String getDescription() {
        return description;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public Boolean getSaveCard() {
        return saveCard;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public AgreementConsent getConsent() {
        return consent;
    }

    public String getIntegration() {
        return integration;
    }

    public Boolean getVerifyOnly() {
        return verifyOnly;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    /** Builds a {@link CheckoutSessionRequest}. */
    public static final class Builder {

        private long amount;
        private String currency;
        private String orderReference;
        private Customer customer;
        private String country;
        private String language;
        private String theme;
        private String description;
        private UUID locationId;
        private Boolean saveCard;
        private String agreementId;
        private AgreementConsent consent;
        private String integration;
        private Boolean verifyOnly;
        private String idempotencyKey;
        private final Map<String, Object> extra = new LinkedHashMap<>();

        private Builder() {
        }

        /** The amount in MINOR units (2500 is 25.00 EUR). Must be positive, or 0 with verifyOnly. */
        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }

        /** The ISO 4217 code. Required. */
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        /** Your own order id, at most 100 characters. Required. */
        public Builder orderReference(String orderReference) {
            this.orderReference = orderReference;
            return this;
        }

        /** The payer. Fields you send are not asked again in the widget. */
        public Builder customer(Customer customer) {
            this.customer = customer;
            return this;
        }

        /** ISO 3166-1 alpha-2 payer country. */
        public Builder country(String country) {
            this.country = country;
            return this;
        }

        /** ISO 639-1 widget UI language. */
        public Builder language(String language) {
            this.language = language;
            return this;
        }

        /** "light", "dark" or "bright". */
        public Builder theme(String theme) {
            this.theme = theme;
            return this;
        }

        /** Free-text description stored on the transaction. */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * The storefront (one of your websites) this session belongs to. A key bound to one
         * storefront refuses any other.
         */
        public Builder locationId(UUID locationId) {
            this.locationId = locationId;
            return this;
        }

        /** Ask the payer to save the card for later charges with chargePaymentMethod. */
        public Builder saveCard(Boolean saveCard) {
            this.saveCard = saveCard;
            return this;
        }

        /**
         * The {@code agr_} id of a pending agreement. The session then takes the agreement's
         * first payment. Amount and currency must be the agreement's own.
         */
        public Builder agreementId(String agreementId) {
            this.agreementId = agreementId;
            return this;
        }

        /** The payer accepted the recurring-payment wording. Only with agreementId. */
        public Builder consent(AgreementConsent consent) {
            this.consent = consent;
            return this;
        }

        /** {@link CheckoutIntegrations#WIDGET} (the default) or {@link CheckoutIntegrations#FIELDS}. */
        public Builder integration(String integration) {
            this.integration = integration;
            return this;
        }

        /**
         * Save the payer's card without a payment. Leave the amount at 0. We switch it on for
         * your account; until then the call throws {@link PermissionException} with
         * {@code VERIFY_ONLY_NOT_ENABLED}.
         */
        public Builder verifyOnly(Boolean verifyOnly) {
            this.verifyOnly = verifyOnly;
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
         * wins over an extra field with the same name.
         */
        public Builder extra(String name, Object value) {
            this.extra.put(name, value);
            return this;
        }

        public CheckoutSessionRequest build() {
            return new CheckoutSessionRequest(this);
        }
    }
}
