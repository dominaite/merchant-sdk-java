package com.dominaite.merchant;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/**
 * The card of a {@code charge.*} event: brand and last four digits. Either can be null.
 */
public final class ChargeEventPaymentMethod {

    private final String brand;
    private final String last4;

    private ChargeEventPaymentMethod(String brand, String last4) {
        this.brand = brand;
        this.last4 = last4;
    }

    /** The card brand, for example {@code visa}, or null. */
    public String getBrand() {
        return brand;
    }

    /** The last four digits of the card, or null. */
    public String getLast4() {
        return last4;
    }

    /**
     * Reads the card, or returns null when the node is not an object or a field is not a string.
     * A signed event never fails verification because of this field.
     */
    static ChargeEventPaymentMethod fromNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode brand = node.get("brand");
        JsonNode last4 = node.get("last4");
        if (!isStringOrNull(brand) || !isStringOrNull(last4)) {
            return null;
        }
        return new ChargeEventPaymentMethod(textOrNull(brand), textOrNull(last4));
    }

    private static boolean isStringOrNull(JsonNode value) {
        return value == null || value.isNull() || value.isTextual();
    }

    private static String textOrNull(JsonNode value) {
        return value == null || value.isNull() ? null : value.asText();
    }

    /** Binds {@link ChargeEventData#getPaymentMethod()} through {@link #fromNode(JsonNode)}. */
    static final class Tolerant extends JsonDeserializer<ChargeEventPaymentMethod> {
        @Override
        public ChargeEventPaymentMethod deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            return fromNode(parser.readValueAsTree());
        }
    }
}
