package com.dominaite.merchant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Verifies and parses webhook deliveries. The {@code X-Webhook-Signature} header is
 * {@code t=<unix seconds>,v1=<hex HMAC-SHA256>}, and the signed message is
 * {@code "{t}.{raw body}"} with your {@code whsec_} secret.
 *
 * <p>Always verify the raw body bytes, before any JSON parsing. A body that was parsed and
 * written again does not verify.
 */
public final class Webhooks {

    /** The default tolerance around the delivery timestamp, in seconds. */
    public static final int DEFAULT_TOLERANCE_SECONDS = 300;

    /** The header that carries the signature. */
    public static final String SIGNATURE_HEADER = "X-Webhook-Signature";

    private static final int MAC_LENGTH_BYTES = 32;

    private Webhooks() {
    }

    /**
     * Verifies a delivery with the default tolerance and the system clock.
     *
     * @param payload the raw body bytes
     * @param signatureHeader the {@code X-Webhook-Signature} header
     * @param secret the endpoint's {@code whsec_} secret
     * @throws WebhookVerificationException when the delivery is not authentic or is a replay
     */
    public static void verify(byte[] payload, String signatureHeader, String secret) {
        verify(payload, signatureHeader, secret, DEFAULT_TOLERANCE_SECONDS, Instant.now().getEpochSecond());
    }

    /**
     * Verifies a delivery. The MAC is checked first, in constant time; the timestamp only after
     * that, so a tolerance failure always means a replay of an authentic delivery.
     *
     * @param payload the raw body bytes
     * @param signatureHeader the {@code X-Webhook-Signature} header
     * @param secret the endpoint's {@code whsec_} secret
     * @param toleranceSeconds how far the delivery timestamp may be from now, in seconds
     * @param nowUnixSeconds the current time in Unix seconds
     * @throws WebhookVerificationException when the delivery is not authentic or is a replay
     */
    public static void verify(
            byte[] payload, String signatureHeader, String secret, int toleranceSeconds, long nowUnixSeconds) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(secret, "secret");
        ParsedHeader header = parseSignatureHeader(signatureHeader);

        // The timestamp exactly as it appeared in the header, never a number printed again.
        byte[] prefix = (header.rawTimestamp + ".").getBytes(StandardCharsets.UTF_8);
        byte[] signed = new byte[prefix.length + payload.length];
        System.arraycopy(prefix, 0, signed, 0, prefix.length);
        System.arraycopy(payload, 0, signed, prefix.length, payload.length);
        byte[] expected = RequestSigner.hmacSha256(secret.getBytes(StandardCharsets.UTF_8), signed);

        // Constant time: both arrays are 32 bytes, the grammar checked the length.
        if (!MessageDigest.isEqual(expected, header.mac)) {
            throw new WebhookVerificationException(
                WebhookFailureReason.SIGNATURE_MISMATCH, "The webhook signature does not match the payload.");
        }

        long drift = Math.abs(nowUnixSeconds - header.timestamp);
        if (drift > toleranceSeconds) {
            throw new WebhookVerificationException(
                WebhookFailureReason.TIMESTAMP_OUT_OF_TOLERANCE,
                "The webhook timestamp " + header.timestamp + " is outside the " + toleranceSeconds
                    + "s tolerance around " + nowUnixSeconds + ".");
        }
    }

    /**
     * Verifies a delivery whose body you hold as a string. The string is encoded as UTF-8, so it
     * must be the body exactly as received.
     */
    public static void verify(String payload, String signatureHeader, String secret) {
        Objects.requireNonNull(payload, "payload");
        verify(payload.getBytes(StandardCharsets.UTF_8), signatureHeader, secret);
    }

    /** Like {@link #verify(byte[], String, String, int, long)}, for a body held as a string. */
    public static void verify(
            String payload, String signatureHeader, String secret, int toleranceSeconds, long nowUnixSeconds) {
        Objects.requireNonNull(payload, "payload");
        verify(payload.getBytes(StandardCharsets.UTF_8), signatureHeader, secret, toleranceSeconds, nowUnixSeconds);
    }

    /**
     * Verifies a delivery and parses the event, with the default tolerance and the system clock.
     *
     * @return the event
     * @throws WebhookVerificationException when the delivery is not authentic, is a replay, or
     *     is not a readable event envelope
     */
    public static WebhookEvent verifyAndParse(byte[] payload, String signatureHeader, String secret) {
        verify(payload, signatureHeader, secret);
        return parseEvent(payload);
    }

    /** Like {@link #verifyAndParse(byte[], String, String)}, with your own tolerance and clock. */
    public static WebhookEvent verifyAndParse(
            byte[] payload, String signatureHeader, String secret, int toleranceSeconds, long nowUnixSeconds) {
        verify(payload, signatureHeader, secret, toleranceSeconds, nowUnixSeconds);
        return parseEvent(payload);
    }

    /** Like {@link #verifyAndParse(byte[], String, String)}, for a body held as a string. */
    public static WebhookEvent verifyAndParse(String payload, String signatureHeader, String secret) {
        Objects.requireNonNull(payload, "payload");
        return verifyAndParse(payload.getBytes(StandardCharsets.UTF_8), signatureHeader, secret);
    }

    /** Like {@link #verifyAndParse(byte[], String, String, int, long)}, for a body held as a string. */
    public static WebhookEvent verifyAndParse(
            String payload, String signatureHeader, String secret, int toleranceSeconds, long nowUnixSeconds) {
        Objects.requireNonNull(payload, "payload");
        return verifyAndParse(
            payload.getBytes(StandardCharsets.UTF_8), signatureHeader, secret, toleranceSeconds, nowUnixSeconds);
    }

    private static WebhookEvent parseEvent(byte[] payload) {
        JsonNode root;
        try {
            root = Json.MAPPER.readTree(payload);
        } catch (IOException e) {
            throw malformedPayload("the body is not a readable envelope (" + originalMessage(e) + ")");
        }
        if (root == null || !root.isObject()) {
            throw malformedPayload("the body is not a JSON object");
        }

        String id = requiredString(root, "id");
        String type = requiredString(root, "type");
        String apiVersion = optionalString(root, "apiVersion");
        Instant createdAt = optionalInstant(root, "createdAt");
        JsonNode data = root.get("data");

        PaymentEventData payment = null;
        AgreementEventData agreement = null;
        ChargeEventData charge = null;
        if (data != null && data.isObject()) {
            try {
                // A verify-only session's events carry the payment data shape too.
                if (type.startsWith("payment.") || type.startsWith("verification.")) {
                    payment = Json.MAPPER.treeToValue(data, PaymentEventData.class);
                    payment.setRaw(data);
                } else if (type.startsWith("agreement.")) {
                    agreement = Json.MAPPER.treeToValue(data, AgreementEventData.class);
                    agreement.setRaw(data);
                } else if (type.startsWith("charge.")) {
                    charge = Json.MAPPER.treeToValue(data, ChargeEventData.class);
                    charge.setRaw(data);
                }
            } catch (JsonProcessingException e) {
                throw malformedPayload("the data is not readable (" + originalMessage(e) + ")");
            }
        }
        return new WebhookEvent(id, type, apiVersion, createdAt, data, payment, agreement, charge, root);
    }

    private static String requiredString(JsonNode root, String name) {
        String value = optionalString(root, name);
        if (value == null || value.isEmpty()) {
            throw malformedPayload("no \"" + name + "\" string");
        }
        return value;
    }

    private static String optionalString(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw malformedPayload("\"" + name + "\" is not a string");
        }
        return value.asText();
    }

    private static Instant optionalInstant(JsonNode root, String name) {
        JsonNode value = root.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            try {
                return Json.parseInstant(value.asText());
            } catch (DateTimeParseException e) {
                // Falls through to the error below.
            }
        }
        throw malformedPayload("\"" + name + "\" is not an ISO 8601 instant");
    }

    private static String originalMessage(IOException e) {
        return e instanceof JsonProcessingException
            ? ((JsonProcessingException) e).getOriginalMessage()
            : e.getMessage();
    }

    private static WebhookVerificationException malformedPayload(String detail) {
        return new WebhookVerificationException(
            WebhookFailureReason.MALFORMED_PAYLOAD, "Malformed webhook payload: " + detail + ".");
    }

    private static ParsedHeader parseSignatureHeader(String header) {
        if (header == null || header.isEmpty()) {
            throw malformed("the signature header is empty");
        }
        String rawTimestamp = null;
        long timestamp = 0;
        byte[] mac = null;
        // split with -1 keeps empty trailing fields, so "t=1," is a field without "=".
        for (String field : header.split(",", -1)) {
            int separator = field.indexOf('=');
            if (separator < 0) {
                throw malformed("field \"" + truncate(field) + "\" is not name=value");
            }
            String name = field.substring(0, separator);
            String value = field.substring(separator + 1);
            if ("t".equals(name)) {
                if (rawTimestamp != null) {
                    throw malformed("repeated t field");
                }
                if (value.isEmpty() || !allAsciiDigits(value)) {
                    throw malformed("timestamp \"" + truncate(value) + "\" is not unix seconds");
                }
                try {
                    timestamp = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw malformed("timestamp \"" + truncate(value) + "\" is not unix seconds");
                }
                rawTimestamp = value;
            } else if ("v1".equals(name)) {
                if (mac != null) {
                    throw malformed("repeated v1 field");
                }
                if (value.length() != MAC_LENGTH_BYTES * 2 || !allLowercaseHex(value)) {
                    throw malformed("the v1 signature is not 64 lowercase hex characters");
                }
                mac = Hex.decodeLowercase(value);
            }
            // Any other name is a scheme version this SDK does not know yet. Ignoring it lets
            // v2 roll out next to v1 without breaking this verifier.
        }
        if (rawTimestamp == null && mac == null) {
            throw malformed("no t or v1 field");
        }
        if (rawTimestamp == null) {
            throw malformed("no t field");
        }
        if (mac == null) {
            throw malformed("no v1 field");
        }
        return new ParsedHeader(rawTimestamp, timestamp, mac);
    }

    private static boolean allAsciiDigits(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean allLowercaseHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Hex.isLowercaseHex(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static WebhookVerificationException malformed(String detail) {
        return new WebhookVerificationException(
            WebhookFailureReason.MALFORMED_SIGNATURE, "Malformed webhook signature: " + detail + ".");
    }

    private static String truncate(String value) {
        return value.length() <= 32 ? value : value.substring(0, 32) + "...";
    }

    private static final class ParsedHeader {
        final String rawTimestamp;
        final long timestamp;
        final byte[] mac;

        ParsedHeader(String rawTimestamp, long timestamp, byte[] mac) {
            this.rawTimestamp = rawTimestamp;
            this.timestamp = timestamp;
            this.mac = mac;
        }
    }
}
