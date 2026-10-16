package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The body of {@link DominaiteClient#createWebhookEndpoint}. Immutable: build it with
 * {@link #builder()}.
 */
@JsonPropertyOrder({"url", "events"})
public final class CreateWebhookEndpointRequest {

    private final String url;
    private final List<String> events;
    @JsonIgnore
    private final String idempotencyKey;
    @JsonIgnore
    private final Map<String, Object> extra;

    private CreateWebhookEndpointRequest(Builder b) {
        this.url = b.url;
        this.events = b.events == null ? null : Collections.unmodifiableList(new ArrayList<>(b.events));
        this.idempotencyKey = b.idempotencyKey;
        this.extra = Collections.unmodifiableMap(new LinkedHashMap<>(b.extra));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with every field of this object, to change one and build again. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.url = url;
        b.events = events;
        b.idempotencyKey = idempotencyKey;
        b.extra.putAll(extra);
        return b;
    }

    public String getUrl() {
        return url;
    }

    public List<String> getEvents() {
        return events;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    /** Builds a {@link CreateWebhookEndpointRequest}. */
    public static final class Builder {

        private String url;
        private List<String> events;
        private String idempotencyKey;
        private final Map<String, Object> extra = new LinkedHashMap<>();

        private Builder() {
        }

        /** The HTTPS URL that receives the events. */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /** The event types to send, from {@link WebhookEventTypes}. */
        public Builder events(List<String> events) {
            this.events = events;
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

        public CreateWebhookEndpointRequest build() {
            return new CreateWebhookEndpointRequest(this);
        }
    }
}
