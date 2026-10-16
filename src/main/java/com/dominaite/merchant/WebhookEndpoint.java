package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

/**
 * A webhook endpoint. {@link #getSecret()} is set only in the create answer: store it then.
 */
public final class WebhookEndpoint {

    private String id;
    private String url;
    private List<String> events;
    private String status;
    private Instant createdAt;
    private String secret;
    @JsonIgnore
    private JsonNode raw;

    private WebhookEndpoint() {
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public List<String> getEvents() {
        return events;
    }

    /** One of {@link WebhookEndpointStatuses}. */
    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** The {@code whsec_} signing secret, only in the create answer. Never log it. */
    public String getSecret() {
        return secret;
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
        return WebhookEndpointStatuses.ACTIVE.equals(status);
    }

    @Override
    public String toString() {
        return "WebhookEndpoint{id=" + id + ", url=" + url + ", status=" + status + ", secret="
            + (secret == null ? "null" : "[REDACTED]") + "}";
    }
}
