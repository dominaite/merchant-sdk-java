package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The body of {@link DominaiteClient#createAgreement}: one payer's subscription to a plan.
 * Immutable: build it with {@link #builder()}.
 */
@JsonPropertyOrder({"planId", "customerReference", "storedPaymentMethodId", "consent", "maxAmountPerPeriod"})
public final class CreateAgreementRequest {

    private final String planId;
    private final String customerReference;
    private final String storedPaymentMethodId;
    private final AgreementConsent consent;
    private final Long maxAmountPerPeriod;
    @JsonIgnore
    private final String idempotencyKey;
    @JsonIgnore
    private final Map<String, Object> extra;

    private CreateAgreementRequest(Builder b) {
        this.planId = b.planId;
        this.customerReference = b.customerReference;
        this.storedPaymentMethodId = b.storedPaymentMethodId;
        this.consent = b.consent;
        this.maxAmountPerPeriod = b.maxAmountPerPeriod;
        this.idempotencyKey = b.idempotencyKey;
        this.extra = Collections.unmodifiableMap(new LinkedHashMap<>(b.extra));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with every field of this object, to change one and build again. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.planId = planId;
        b.customerReference = customerReference;
        b.storedPaymentMethodId = storedPaymentMethodId;
        b.consent = consent;
        b.maxAmountPerPeriod = maxAmountPerPeriod;
        b.idempotencyKey = idempotencyKey;
        b.extra.putAll(extra);
        return b;
    }

    public String getPlanId() {
        return planId;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public String getStoredPaymentMethodId() {
        return storedPaymentMethodId;
    }

    public AgreementConsent getConsent() {
        return consent;
    }

    public Long getMaxAmountPerPeriod() {
        return maxAmountPerPeriod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    /** Builds a {@link CreateAgreementRequest}. */
    public static final class Builder {

        private String planId;
        private String customerReference;
        private String storedPaymentMethodId;
        private AgreementConsent consent;
        private Long maxAmountPerPeriod;
        private String idempotencyKey;
        private final Map<String, Object> extra = new LinkedHashMap<>();

        private Builder() {
        }

        /** The plan id from createPlan. */
        public Builder planId(String planId) {
            this.planId = planId;
            return this;
        }

        /** Your own id for the payer. */
        public Builder customerReference(String customerReference) {
            this.customerReference = customerReference;
            return this;
        }

        /** A stored payment method to charge, or null to take the first payment in a checkout session. */
        public Builder storedPaymentMethodId(String storedPaymentMethodId) {
            this.storedPaymentMethodId = storedPaymentMethodId;
            return this;
        }

        /** The payer accepted the recurring-payment wording. */
        public Builder consent(AgreementConsent consent) {
            this.consent = consent;
            return this;
        }

        /** A cap per period in MINOR units, for a variable amount. */
        public Builder maxAmountPerPeriod(Long maxAmountPerPeriod) {
            this.maxAmountPerPeriod = maxAmountPerPeriod;
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

        public CreateAgreementRequest build() {
            return new CreateAgreementRequest(this);
        }
    }
}
