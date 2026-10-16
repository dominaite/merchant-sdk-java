package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * One payer's subscription to a plan.
 */
public final class Agreement {

    private String id;
    private String planId;
    private String customerReference;
    private String storedPaymentMethodId;
    private String status;
    private long amount;
    private Long maxAmountPerPeriod;
    private String currency;
    private String intervalUnit;
    private int intervalCount;
    private Integer periodCount;
    private int trialDays;
    private Instant nextChargeAt;
    private String pastDueReason;
    private Instant pastDueAt;
    private AgreementConsentRecord consent;
    private int version;
    private long sequence;
    private Instant createdAt;
    private Instant activatedAt;
    private Instant cancelledAt;
    @JsonIgnore
    private JsonNode raw;

    private Agreement() {
    }

    public String getId() {
        return id;
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

    /** One of {@link AgreementStatuses}. */
    public String getStatus() {
        return status;
    }

    public long getAmount() {
        return amount;
    }

    public Long getMaxAmountPerPeriod() {
        return maxAmountPerPeriod;
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

    public int getTrialDays() {
        return trialDays;
    }

    public Instant getNextChargeAt() {
        return nextChargeAt;
    }

    /** One of {@link PastDueReasons}, or null. */
    public String getPastDueReason() {
        return pastDueReason;
    }

    public Instant getPastDueAt() {
        return pastDueAt;
    }

    public AgreementConsentRecord getConsent() {
        return consent;
    }

    public int getVersion() {
        return version;
    }

    public long getSequence() {
        return sequence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }
}
