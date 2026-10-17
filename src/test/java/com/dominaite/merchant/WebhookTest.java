package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

/**
 * Webhook verification. The canonical vector and the header grammar are shared by every
 * Dominaite SDK, so each one accepts and rejects the same bytes.
 */
class WebhookTest {

    static final String SECRET = "whsec_abababababababababababababababababababababababababababababababab";
    static final String BODY = "{\"id\":\"7f9c24e5-1d1f-4c0a-9b6c-2f3a4d5e6f70\",\"type\":\"payment.succeeded\","
        + "\"createdAt\":\"2026-08-20T14:00:00Z\",\"data\":{\"transactionId\":\"0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0\","
        + "\"status\":\"succeeded\",\"previousStatus\":\"pending\",\"kind\":\"sale\",\"amount\":8440,"
        + "\"grossAmount\":8701,\"surchargeAmount\":261,\"currency\":\"EUR\",\"originalTransactionId\":null,"
        + "\"idempotencyKey\":\"order-123\"}}";
    static final String TS = "1755700000";
    static final long TIMESTAMP = 1755700000L;
    static final String MAC = "5305bcf1302fdaba8f8c19a20c899e916fb4d2a7d8d547c62529ff87c4697b72";
    static final String HEADER = "t=" + TS + ",v1=" + MAC;
    static final int TOL = Webhooks.DEFAULT_TOLERANCE_SECONDS;

    static String sign(String timestamp, String body) {
        return Hex.encode(RequestSigner.hmacSha256(
            SECRET.getBytes(StandardCharsets.UTF_8), (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    static WebhookFailureReason reason(String body, String header, long now) {
        return assertThrows(WebhookVerificationException.class,
            () -> Webhooks.verify(body, header, SECRET, TOL, now)).getReason();
    }

    @Test
    void theCanonicalVectorVerifies() {
        assertEquals(MAC, sign(TS, BODY));
        assertDoesNotThrow(() -> Webhooks.verify(BODY, HEADER, SECRET, TOL, TIMESTAMP + 10));
        assertDoesNotThrow(() -> Webhooks.verify(BODY.getBytes(StandardCharsets.UTF_8), HEADER, SECRET, TOL, TIMESTAMP));
    }

    @Test
    void aSingleByteTamperFails() {
        String tampered = BODY.replace("\"amount\":8440", "\"amount\":8441");
        assertNotEquals(BODY, tampered);
        assertEquals(WebhookFailureReason.SIGNATURE_MISMATCH, reason(tampered, HEADER, TIMESTAMP + 10));
    }

    @Test
    void theWrongSecretFails() {
        WebhookVerificationException error = assertThrows(WebhookVerificationException.class,
            () -> Webhooks.verify(BODY, HEADER, "whsec_" + "cd".repeat(32), TOL, TIMESTAMP + 10));
        assertEquals(WebhookFailureReason.SIGNATURE_MISMATCH, error.getReason());
    }

    @Test
    void aStaleTimestampFailsEvenWithAValidMac() {
        assertEquals(WebhookFailureReason.TIMESTAMP_OUT_OF_TOLERANCE, reason(BODY, HEADER, TIMESTAMP + TOL + 1));
    }

    @Test
    void aTimestampFromTheFutureFailsToo() {
        assertEquals(WebhookFailureReason.TIMESTAMP_OUT_OF_TOLERANCE, reason(BODY, HEADER, TIMESTAMP - TOL - 1));
    }

    @Test
    void theEdgeOfTheToleranceStillVerifies() {
        assertDoesNotThrow(() -> Webhooks.verify(BODY, HEADER, SECRET, TOL, TIMESTAMP + TOL));
        assertDoesNotThrow(() -> Webhooks.verify(BODY, HEADER, SECRET, TOL, TIMESTAMP - TOL));
    }

    @Test
    void theToleranceIsThreeHundredSecondsLikeEverySdk() {
        assertEquals(300, Webhooks.DEFAULT_TOLERANCE_SECONDS);
    }

    @Test
    void theMacIsCheckedBeforeTheTimestamp() {
        String badMac = "t=" + TS + ",v1=" + "0".repeat(64);
        assertEquals(WebhookFailureReason.SIGNATURE_MISMATCH, reason(BODY, badMac, TIMESTAMP + 100_000));
    }

    @Test
    void theDefaultClockRejectsTheOldVector() {
        WebhookVerificationException error = assertThrows(WebhookVerificationException.class,
            () -> Webhooks.verify(BODY, HEADER, SECRET));
        assertEquals(WebhookFailureReason.TIMESTAMP_OUT_OF_TOLERANCE, error.getReason());
    }

    @Test
    void aFreshDeliveryVerifiesWithTheDefaultClock() {
        String now = Long.toString(Instant.now().getEpochSecond());
        assertDoesNotThrow(() -> Webhooks.verify(BODY, "t=" + now + ",v1=" + sign(now, BODY), SECRET));
    }

    static Stream<Arguments> malformedHeaders() {
        return Stream.of(
            Arguments.of("1 missing v1", "t=" + TS),
            Arguments.of("2 missing t", "v1=" + MAC),
            Arguments.of("3 uppercase hex", "t=" + TS + ",v1=" + MAC.toUpperCase(Locale.ROOT)),
            Arguments.of("4 repeated v1", "t=" + TS + ",v1=" + MAC + ",v1=" + MAC),
            Arguments.of("5 repeated t", "t=" + TS + ",t=" + TS + ",v1=" + MAC),
            Arguments.of("6 empty t plus repeated v1", "t=,v1=garbage,v1=" + MAC),
            Arguments.of("7 whitespace after comma", "t=" + TS + ", v1=" + MAC),
            Arguments.of("8 non-digit in t", "t=+" + TS + ",v1=" + MAC),
            Arguments.of("9 element without equals", "garbage"),
            Arguments.of("10 empty header", ""),
            Arguments.of("11 short mac", "t=" + TS + ",v1=" + MAC.substring(2)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedHeaders")
    void theGrammarRejects(String name, String header) {
        WebhookFailureReason reason = reason(BODY, header, TIMESTAMP);
        assertTrue(reason == WebhookFailureReason.MALFORMED_SIGNATURE, name + " gave " + reason);
    }

    @Test
    void anUnknownSchemeVersionIsIgnored() {
        assertDoesNotThrow(() -> Webhooks.verify(BODY, HEADER + ",v9=deadbeef", SECRET, TOL, TIMESTAMP));
    }

    @Test
    void theMacCoversTheRawTimestampSubstring() {
        String padded = "01755700000";
        assertDoesNotThrow(() -> Webhooks.verify(BODY, "t=" + padded + ",v1=" + sign(padded, BODY), SECRET, TOL, TIMESTAMP));
        assertEquals(WebhookFailureReason.SIGNATURE_MISMATCH,
            reason(BODY, "t=" + padded + ",v1=" + sign(TS, BODY), TIMESTAMP));
    }

    @Test
    void verifyAndParseGivesTheTypedPaymentData() {
        WebhookEvent event = Webhooks.verifyAndParse(BODY, HEADER, SECRET, TOL, TIMESTAMP);

        assertEquals("7f9c24e5-1d1f-4c0a-9b6c-2f3a4d5e6f70", event.getId());
        assertEquals(WebhookEventTypes.PAYMENT_SUCCEEDED, event.getType());
        assertEquals(Instant.parse("2026-08-20T14:00:00Z"), event.getCreatedAt());
        assertEquals(8440, event.getPayment().getAmount());
        assertEquals(8701, event.getPayment().getGrossAmount());
        assertEquals(Long.valueOf(261), event.getPayment().getSurchargeAmount());
        assertNull(event.getPayment().getOriginalTransactionId());
        assertEquals("order-123", event.getPayment().getIdempotencyKey());
        assertNull(event.getAgreement());
        assertNull(event.getCharge());
    }

    @Test
    void agreementChargeAndVerificationEventsAreTyped() {
        String agreement = "{\"id\":\"e1\",\"type\":\"agreement.past_due\",\"data\":{\"id\":\"agr_1\",\"status\":\"past_due\","
            + "\"amount\":999,\"currency\":\"EUR\",\"sequence\":3}}";
        WebhookEvent a = Webhooks.verifyAndParse(agreement, "t=" + TS + ",v1=" + sign(TS, agreement), SECRET, TOL, TIMESTAMP);
        assertEquals("agr_1", a.getAgreement().getOrderingKey());
        assertEquals(Long.valueOf(3), a.getAgreement().getSequence());

        String charge = "{\"id\":\"e2\",\"type\":\"charge.retrying\",\"data\":{\"chargeId\":\"ch_1\",\"agreementId\":\"agr_1\","
            + "\"periodNumber\":2,\"outcome\":\"retrying\",\"amount\":999,\"currency\":\"EUR\",\"declineClass\":\"soft_funds\"}}";
        WebhookEvent c = Webhooks.verifyAndParse(charge, "t=" + TS + ",v1=" + sign(TS, charge), SECRET, TOL, TIMESTAMP);
        assertEquals("agr_1/2", c.getCharge().getOrderingKey());
        assertNull(c.getCharge().getPaymentMethod());

        String verification = "{\"id\":\"e3\",\"type\":\"verification.failed\",\"data\":{\"transactionId\":\"t\","
            + "\"status\":\"failed\",\"verifyOnly\":true,\"declineClass\":null}}";
        WebhookEvent v = Webhooks.verifyAndParse(verification, "t=" + TS + ",v1=" + sign(TS, verification), SECRET, TOL, TIMESTAMP);
        assertTrue(v.getPayment().isVerifyOnly());
        assertNull(v.getPayment().getDeclineClass());
    }

    @Test
    void aChargeEventCarriesTheCardAndToleratesExplicitNulls() {
        String charge = "{\"id\":\"e4\",\"type\":\"charge.failed\",\"createdAt\":\"2026-10-17T10:00:00Z\","
            + "\"data\":{\"chargeId\":\"ch_1\",\"transactionId\":null,\"storedPaymentMethodId\":\"pm_1\","
            + "\"agreementId\":\"agr_1\",\"customerReference\":\"cust-1\",\"outcome\":\"failed\",\"periodNumber\":1,"
            + "\"attemptNumber\":3,\"amount\":999,\"currency\":\"EUR\",\"paymentMethod\":{\"brand\":\"visa\","
            + "\"last4\":\"4242\"},\"orderReference\":null,\"description\":null,\"declineClass\":\"hard\","
            + "\"declineCode\":\"05\",\"nextAttemptAt\":null,\"nextChargeAt\":null,\"sequence\":7}}";
        ChargeEventData data = Webhooks.verifyAndParse(charge, "t=" + TS + ",v1=" + sign(TS, charge), SECRET, TOL, TIMESTAMP)
            .getCharge();
        assertEquals("visa", data.getPaymentMethod().getBrand());
        assertEquals("4242", data.getPaymentMethod().getLast4());
        assertEquals(Integer.valueOf(3), data.getAttemptNumber());
        assertNull(data.getTransactionId());
        assertNull(data.getNextAttemptAt());

        String nullCard = charge.replace("{\"brand\":\"visa\",\"last4\":\"4242\"}", "null");
        assertNull(Webhooks.verifyAndParse(nullCard, "t=" + TS + ",v1=" + sign(TS, nullCard), SECRET, TOL, TIMESTAMP)
            .getCharge().getPaymentMethod());
    }

    static Stream<Arguments> wrongShapedCards() {
        return Stream.of(
            Arguments.of("\"visa\""),
            Arguments.of("\"\""),
            Arguments.of("[\"visa\",\"4242\"]"),
            Arguments.of("{\"brand\":{\"name\":\"visa\"},\"last4\":\"4242\"}"));
    }

    @ParameterizedTest
    @MethodSource("wrongShapedCards")
    void aSignedChargeEventWithAWrongShapedCardStillVerifies(String card) {
        String charge = "{\"id\":\"e5\",\"type\":\"charge.succeeded\",\"data\":{\"chargeId\":\"ch_1\","
            + "\"amount\":999,\"currency\":\"EUR\",\"paymentMethod\":" + card + "}}";
        WebhookEvent event = assertDoesNotThrow(
            () -> Webhooks.verifyAndParse(charge, "t=" + TS + ",v1=" + sign(TS, charge), SECRET, TOL, TIMESTAMP));
        ChargeEventData data = event.getCharge();
        assertNull(data.getPaymentMethod());
        assertEquals("ch_1", data.getChargeId());
        assertEquals(999, data.getAmount());
        assertEquals(card, data.getRaw().get("paymentMethod").toString());
    }

    static Stream<Arguments> wrongTypedScalars() {
        return Stream.of(
            Arguments.of("payment.succeeded", "amount", "{\"transactionId\":\"t\",\"amount\":\"ten\",\"currency\":\"EUR\"}"),
            Arguments.of("verification.failed", "verifyOnly",
                "{\"transactionId\":\"t\",\"status\":\"failed\",\"verifyOnly\":\"yes\"}"),
            Arguments.of("agreement.activated", "intervalCount",
                "{\"id\":\"agr_1\",\"amount\":999,\"currency\":\"EUR\",\"intervalCount\":\"two\"}"),
            Arguments.of("charge.retrying", "periodNumber",
                "{\"chargeId\":\"ch_1\",\"agreementId\":\"agr_1\",\"periodNumber\":\"two\",\"amount\":999}"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wrongTypedScalars")
    void aSignedEventWhoseDataDoesNotMapStillVerifiesWithTheRawData(String type, String field, String data) {
        String body = "{\"id\":\"e6\",\"type\":\"" + type + "\",\"data\":" + data + "}";
        WebhookEvent event = assertDoesNotThrow(
            () -> Webhooks.verifyAndParse(body, "t=" + TS + ",v1=" + sign(TS, body), SECRET, TOL, TIMESTAMP));

        assertEquals("e6", event.getId());
        assertEquals(type, event.getType());
        assertNull(event.getPayment());
        assertNull(event.getAgreement());
        assertNull(event.getCharge());
        assertEquals(data, event.getData().toString());
        assertEquals(event.getData(), event.getRaw().get("data"));
        assertTrue(event.getData().get(field).isTextual(), field + " is kept as sent");
    }

    @Test
    void aBodyThatIsNotAnEnvelopeIsMalformedPayload() {
        String body = "[1,2]";
        WebhookVerificationException error = assertThrows(WebhookVerificationException.class,
            () -> Webhooks.verifyAndParse(body, "t=" + TS + ",v1=" + sign(TS, body), SECRET, TOL, TIMESTAMP));
        assertEquals(WebhookFailureReason.MALFORMED_PAYLOAD, error.getReason());

        String truncated = "{\"id\":\"e1\",\"type\":\"payment.succeeded\",\"data\":{\"amount\":1}";
        WebhookVerificationException notJson = assertThrows(WebhookVerificationException.class,
            () -> Webhooks.verifyAndParse(truncated, "t=" + TS + ",v1=" + sign(TS, truncated), SECRET, TOL, TIMESTAMP));
        assertEquals(WebhookFailureReason.MALFORMED_PAYLOAD, notJson.getReason());

        String noType = "{\"id\":\"e1\"}";
        WebhookVerificationException missing = assertThrows(WebhookVerificationException.class,
            () -> Webhooks.verifyAndParse(noType, "t=" + TS + ",v1=" + sign(TS, noType), SECRET, TOL, TIMESTAMP));
        assertEquals(WebhookFailureReason.MALFORMED_PAYLOAD, missing.getReason());
    }
}
