package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * The answer of {@link DominaiteClient#ping}: the credentials work, and the clocks agree.
 */
public final class PingResponse extends ApiResult {

    private boolean pong;
    private String merchantId;
    private Instant serverTime;
    private Long serverUnixTime;
    private long clockSkewSeconds;
    @JsonIgnore
    private JsonNode raw;

    private PingResponse() {
    }

    public boolean isPong() {
        return pong;
    }

    /** Your merchant id. */
    public String getMerchantId() {
        return merchantId;
    }

    /** The server clock. */
    public Instant getServerTime() {
        return serverTime;
    }

    /** The server clock in Unix seconds. */
    public Long getServerUnixTime() {
        return serverUnixTime;
    }

    /** Your clock minus the server clock, in seconds. Keep it well under 300. */
    public long getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    /** The JSON object this was read from, with every field, also the ones this class does not model. */
    public JsonNode getRaw() {
        return raw;
    }

    void setRaw(JsonNode raw) {
        this.raw = raw;
    }
}
