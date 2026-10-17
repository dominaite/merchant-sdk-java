package com.dominaite.merchant;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * A verified webhook event. The typed data is set by the event type's prefix: {@code payment.}
 * and {@code verification.} fill {@link #getPayment()}, {@code agreement.} fills
 * {@link #getAgreement()}, {@code charge.} fills {@link #getCharge()}. An event type the SDK has
 * never seen leaves all three null; its data is still in {@link #getData()}. Data that does not
 * map to the typed model, for example a number sent as text, also leaves the typed getter null
 * and the event still verifies. Check for null before you read the typed data.
 */
public final class WebhookEvent {

    private final String id;
    private final String type;
    private final String apiVersion;
    private final Instant createdAt;
    private final JsonNode data;
    private final PaymentEventData payment;
    private final AgreementEventData agreement;
    private final ChargeEventData charge;
    private final JsonNode raw;

    WebhookEvent(
            String id,
            String type,
            String apiVersion,
            Instant createdAt,
            JsonNode data,
            PaymentEventData payment,
            AgreementEventData agreement,
            ChargeEventData charge,
            JsonNode raw) {
        this.id = id;
        this.type = type;
        this.apiVersion = apiVersion;
        this.createdAt = createdAt;
        this.data = data;
        this.payment = payment;
        this.agreement = agreement;
        this.charge = charge;
        this.raw = raw;
    }

    /** The event id. Deliveries can repeat: skip an id you already handled. */
    public String getId() {
        return id;
    }

    /** The event type, for example {@code payment.succeeded}. See {@link WebhookEventTypes}. */
    public String getType() {
        return type;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** The {@code data} object as JSON, or null when the event has none. */
    public JsonNode getData() {
        return data;
    }

    public PaymentEventData getPayment() {
        return payment;
    }

    public AgreementEventData getAgreement() {
        return agreement;
    }

    public ChargeEventData getCharge() {
        return charge;
    }

    /** The whole event envelope as JSON. */
    public JsonNode getRaw() {
        return raw;
    }
}
