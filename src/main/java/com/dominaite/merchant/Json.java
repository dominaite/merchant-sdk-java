package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * The one JSON configuration of the SDK. Models bind by field, so the public classes stay
 * immutable; unknown fields are ignored, so a new gateway field never breaks a read.
 */
final class Json {

    static final ObjectMapper MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
        .addModule(new SimpleModule("dominaite-instant")
            .addSerializer(Instant.class, new InstantSerializer())
            .addDeserializer(Instant.class, new InstantDeserializer()))
        .build();

    private Json() {
    }

    /** Serializes a request and adds each extra field the request does not set itself. */
    static String body(Object request, Map<String, Object> extra) {
        ObjectNode node = MAPPER.valueToTree(request);
        for (Map.Entry<String, Object> field : extra.entrySet()) {
            if (!node.has(field.getKey())) {
                node.set(field.getKey(), MAPPER.valueToTree(field.getValue()));
            }
        }
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new ValidationException("The request could not be serialized as JSON: " + e.getOriginalMessage());
        }
    }

    /** Parses an ISO 8601 instant with Z or an offset. A value without one is read as UTC. */
    static Instant parseInstant(String text) {
        try {
            // Instant.parse rejects offsets before Java 12, so go through OffsetDateTime.
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException withoutOffset) {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
        }
    }

    /** ISO 8601 UTC with Z, and fractional seconds only when there are any. */
    static String formatInstant(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    static String text(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isEmpty()) {
            return null;
        }
        return value.asText();
    }

    private static final class InstantSerializer extends JsonSerializer<Instant> {
        @Override
        public void serialize(Instant value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(formatInstant(value));
        }
    }

    private static final class InstantDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                return (Instant) context.handleUnexpectedToken(Instant.class, parser);
            }
            String text = parser.getText().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return parseInstant(text);
            } catch (DateTimeParseException e) {
                return (Instant) context.handleWeirdStringValue(Instant.class, text, "not an ISO 8601 instant");
            }
        }
    }
}
