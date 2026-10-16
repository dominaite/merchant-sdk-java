package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The body of {@link DominaiteClient#updatePlan}. Set at least one field. Amount, currency and
 * interval cannot change: create a new plan instead.
 */
@JsonPropertyOrder({"name", "trialDays", "cancelUrl"})
public final class UpdatePlanRequest {

    private final String name;
    private final Integer trialDays;
    private final String cancelUrl;
    @JsonIgnore
    private final Map<String, Object> extra;

    private UpdatePlanRequest(Builder b) {
        this.name = b.name;
        this.trialDays = b.trialDays;
        this.cancelUrl = b.cancelUrl;
        this.extra = Collections.unmodifiableMap(new LinkedHashMap<>(b.extra));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with every field of this object, to change one and build again. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.name = name;
        b.trialDays = trialDays;
        b.cancelUrl = cancelUrl;
        b.extra.putAll(extra);
        return b;
    }

    public String getName() {
        return name;
    }

    public Integer getTrialDays() {
        return trialDays;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    /** Builds a {@link UpdatePlanRequest}. */
    public static final class Builder {

        private String name;
        private Integer trialDays;
        private String cancelUrl;
        private final Map<String, Object> extra = new LinkedHashMap<>();

        private Builder() {
        }

        /** The new plan name. */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** The new trial length, for agreements created from now on. */
        public Builder trialDays(Integer trialDays) {
            this.trialDays = trialDays;
            return this;
        }

        /** The new cancel URL. */
        public Builder cancelUrl(String cancelUrl) {
            this.cancelUrl = cancelUrl;
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

        public UpdatePlanRequest build() {
            return new UpdatePlanRequest(this);
        }
    }
}
