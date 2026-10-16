package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * A subscription plan.
 */
public final class Plan {

    private String id;
    private String name;
    private long amount;
    private String currency;
    private String intervalUnit;
    private int intervalCount;
    private Integer periodCount;
    private int trialDays;
    private String status;
    private String cancelUrl;
    private Instant createdAt;
    private Instant archivedAt;
    @JsonIgnore
    private JsonNode raw;

    private Plan() {
    }

    public String getId() {
        return id;
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

    /** One of {@link IntervalUnits}. */
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

    /** One of {@link PlanStatuses}. */
    public String getStatus() {
        return status;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }

    /** True when the status is {@code active}. */
    public boolean isActive() {
        return PlanStatuses.ACTIVE.equals(status);
    }
}
