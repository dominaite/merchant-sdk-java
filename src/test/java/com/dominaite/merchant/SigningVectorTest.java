package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * The cross-SDK known-answer vectors. The same inputs give the same signatures in the node,
 * python, go, .NET, php and rust SDKs; the values here are copied from their test suites.
 */
class SigningVectorTest {

    private static final String SECRET =
        "dms_0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String PAYMENT_METHOD_ID = "pm_0123456789abcdef0123456789abcdef";
    private static final String TIMESTAMP = "1755302400";

    @Test
    void thePostVectorReproducesByteForByte() {
        String body = "{\"amount\":2500,\"currency\":\"EUR\",\"orderReference\":\"order-1042\"}";
        assertEquals(
            "aa3edd72cd1829f4e053abb048b08c1ae91c2d67b08955997c4b6c4dab4f98ff",
            RequestSigner.sha256Hex(body));

        String signature = RequestSigner.sign(new SignatureInput(
            SECRET, TIMESTAMP, "POST", DominaiteClient.SESSIONS_PATH,
            "00000000-0000-4000-8000-000000000001", body));

        assertEquals("8f5fba0b29a8eea81b76a0e6d7119e79ec68f586910f77713b045652e5ce9b74", signature);
    }

    @Test
    void theGetVectorReproducesByteForByte() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            RequestSigner.sha256Hex(""));

        String signature = RequestSigner.sign(new SignatureInput(
            SECRET, TIMESTAMP, "GET",
            DominaiteClient.SESSIONS_PATH + "/00000000-0000-4000-8000-000000000002", "", ""));

        assertEquals("70002896ec8411efb7754de6c49c2fd6f35bb2d001966978a2f573de1914e68d", signature);
    }

    @Test
    void theNonAsciiVectorReproducesByteForByte() {
        String body = "{\"amount\":2500,\"currency\":\"EUR\",\"orderReference\":\"order-1042\","
            + "\"customer\":{\"firstName\":\"Анна\",\"lastName\":\"Müller\"}}";
        assertEquals(
            "baf00d6116d9f2eec6c3a422af0bc2c342717f669aa2350ef6ed556f57ac34b5",
            RequestSigner.sha256Hex(body));

        String signature = RequestSigner.sign(new SignatureInput(
            SECRET, TIMESTAMP, "POST", DominaiteClient.SESSIONS_PATH,
            "00000000-0000-4000-8000-000000000003", body));

        assertEquals("dd809cb0b902326704a380110c29d9f789cc355864e1ed1de663157342834010", signature);
    }

    @Test
    void theChargeVectorReproducesByteForByte() {
        String body = "{\"amount\":2500,\"currency\":\"EUR\",\"orderReference\":\"order-1043\"}";
        assertEquals(
            "641a0d2b08f88ebc458dca49410dede0a166359a5030bff5c977e507f13ab828",
            RequestSigner.sha256Hex(body));

        String signature = RequestSigner.sign(new SignatureInput(
            SECRET, TIMESTAMP, "POST",
            DominaiteClient.PAYMENT_METHODS_PATH + "/" + PAYMENT_METHOD_ID + "/charges",
            "00000000-0000-4000-8000-000000000003", body));

        assertEquals("9ce9f54efa2533a46aa4493b97b56aeb657f41d6a18f1c008c7fd412029aebf9", signature);
    }

    @Test
    void theRevokeVectorSignsAnEmptyKeyAndAnEmptyBody() {
        String path = DominaiteClient.PAYMENT_METHODS_PATH + "/" + PAYMENT_METHOD_ID;

        String signature = RequestSigner.sign(new SignatureInput(SECRET, TIMESTAMP, "DELETE", path, "", ""));
        assertEquals("9330100343c4b820504890a09829a193d5815ca39e92160fdfc13d320a802a02", signature);

        String asGet = RequestSigner.sign(new SignatureInput(SECRET, TIMESTAMP, "GET", path, "", ""));
        assertNotEquals(signature, asGet);
    }

    @Test
    void theMethodIsUppercasedBeforeSigning() {
        String lower = RequestSigner.sign(new SignatureInput(SECRET, TIMESTAMP, "get", DominaiteClient.PING_PATH, "", ""));
        String upper = RequestSigner.sign(new SignatureInput(SECRET, TIMESTAMP, "GET", DominaiteClient.PING_PATH, "", ""));
        assertEquals(upper, lower);
    }

    @Test
    void theIdempotencyKeyIsInsideTheSignature() {
        String body = "{\"amount\":2500,\"currency\":\"EUR\",\"orderReference\":\"order-1042\"}";
        String other = RequestSigner.sign(new SignatureInput(
            SECRET, TIMESTAMP, "POST", DominaiteClient.SESSIONS_PATH,
            "00000000-0000-4000-8000-000000000009", body));
        assertNotEquals("8f5fba0b29a8eea81b76a0e6d7119e79ec68f586910f77713b045652e5ce9b74", other);
    }

    @Test
    void theGetPayloadIsFiveLinesComputedIndependently() {
        String path = DominaiteClient.TRANSACTIONS_PATH;
        String payload = TIMESTAMP + "\nGET\n" + path + "\n\n"
            + "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String expected = Hex.encode(RequestSigner.hmacSha256(
            SECRET.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8)));

        assertEquals(expected, RequestSigner.sign(new SignatureInput(SECRET, TIMESTAMP, "GET", path, "", "")));
    }

    @Test
    void toStringNeverPrintsTheSecret() {
        SignatureInput input = new SignatureInput(SECRET, TIMESTAMP, "GET", DominaiteClient.PING_PATH, "", "");
        assertFalse(input.toString().contains(SECRET));
        assertFalse(input.toString().contains("0123456789abcdef"));
    }
}
