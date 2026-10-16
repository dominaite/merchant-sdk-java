package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The body of {@link DominaiteClient#createPlan}: a subscription plan. Immutable: build it
 * with {@link #builder()}.
 */
@JsonPropertyOrder({"name", "amount", "currency", "intervalUnit", "intervalCount", "periodCount", "trialDays", "cancelUrl"})
public final class CreatePlanRequest {

    private final String name;
    private final long amount;
    private final String currency;
    private final String intervalUnit;
    private final int intervalCount;
    private final Integer periodCount;
    private final Integer trialDays;
    private final String cancelUrl;
    @JsonIgnore
    private final String idempotencyKey;
    @JsonIgnore
    private final Map<String, Object> extra;

    private CreatePlanRequest(Builder b) {
        this.name = b.name;
        this.amount = b.amount;
        this.currency = b.currency;
        this.intervalUnit = b.intervalUnit;
        this.intervalCount = b.intervalCount;
        this.periodCount = b.periodCount;
        this.trialDays = b.trialDays;
        this.cancelUrl = b.cancelUrl;
        this.idempotencyKey = b.idempotencyKey;
        this.extra = Collections.unmodifiableMap(new LinkedHashMap<>(b.extra));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with every field of this object, to change one and build again. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.name = name;
        b.amount = amount;
        b.currency = currency;
        b.intervalUnit = intervalUnit;
        b.intervalCount = intervalCount;
        b.periodCount = periodCount;
        b.trialDays = trialDays;
        b.cancelUrl = cancelUrl;
        b.idempotencyKey = idempotencyKey;
        b.extra.putAll(extra);
        return b;
    }

    public String getName() {
        return name;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getIntervalUnit() {
        return intervalUnit;
    }

    public int getIntervalCount() {
        return intervalCount;
    }

    public Integer getPeriodCount() {
        return periodCount;
    }

    public Integer getTrialDays() {
        return trialDays;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    /** Builds a {@link CreatePlanRequest}. */
    public static final class Builder {

        private String name;
        private long amount;
        private String currency;
        private String intervalUnit;
        private int intervalCount = 1;
        private Integer periodCount;
        private Integer trialDays;
        private String cancelUrl;
        private String idempotencyKey;
        private final Map<String, Object> extra = new LinkedHashMap<>();

        private Builder() {
        }

        /** The plan name the payer sees. */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** The amount per period in MINOR units. */
        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }

        /** The ISO 4217 code. */
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        /** One of {@link IntervalUnits}. */
        public Builder intervalUnit(String intervalUnit) {
            this.intervalUnit = intervalUnit;
            return this;
        }

        /** How many interval units make one period. Defaults to 1. */
        public Builder intervalCount(int intervalCount) {
            this.intervalCount = intervalCount;
            return this;
        }

        /** How many periods to charge, or null to charge until cancelled. */
        public Builder periodCount(Integer periodCount) {
            this.periodCount = periodCount;
            return this;
        }

        /** Free days before the first charge. */
        public Builder trialDays(Integer trialDays) {
            this.trialDays = trialDays;
            return this;
        }

        /** Where the payer can cancel. */
        public Builder cancelUrl(String cancelUrl) {
            this.cancelUrl = cancelUrl;
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

        public CreatePlanRequest build() {
            return new CreatePlanRequest(this);
        }
    }
}
