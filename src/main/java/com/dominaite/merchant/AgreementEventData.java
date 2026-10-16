package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * The data of an {@code agreement.*} event.
 */
public final class AgreementEventData {

    private String id;
    private String planId;
    private String customerReference;
    private String storedPaymentMethodId;
    private String status;
    private String previousStatus;
    private long amount;
    private String currency;
    private String intervalUnit;
    private Integer intervalCount;
    private Integer periodCount;
    private Integer trialDays;
    private Instant nextChargeAt;
    private Instant activatedAt;
    private Instant cancelledAt;
    private Integer version;
    private Long sequence;
    @JsonIgnore
    private JsonNode raw;

    private AgreementEventData() {
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

    public String getStatus() {
        return status;
    }

    public String getPreviousStatus() {
        return previousStatus;
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

    public Integer getIntervalCount() {
        return intervalCount;
    }

    public Integer getPeriodCount() {
        return periodCount;
    }

    public Integer getTrialDays() {
        return trialDays;
    }

    public Instant getNextChargeAt() {
        return nextChargeAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Integer getVersion() {
        return version;
    }

    public Long getSequence() {
        return sequence;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }

    /** Order the events of one agreement by {@link #getSequence()} within this key. */
    public String getOrderingKey() {
        return id;
    }
}
