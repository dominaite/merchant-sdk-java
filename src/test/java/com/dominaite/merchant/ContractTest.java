package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The SDK's hardcoded vocabularies against the vendored gateway contract fixtures. The fixtures
 * are the same files the other SDKs vendor; the contract-drift workflow refreshes them.
 */
class ContractTest {

    static JsonNode fixture(String name) throws IOException {
        try (InputStream in = ContractTest.class.getResourceAsStream("/" + name)) {
            return Json.MAPPER.readTree(in);
        }
    }

    static List<String> strings(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(node -> out.add(node.asText()));
        return out;
    }

    static List<String> codes(JsonNode entries) {
        List<String> out = new ArrayList<>();
        entries.forEach(node -> out.add(node.get("code").asText()));
        return out;
    }

    @Test
    void theStatusVocabularyMatchesTheWireContract() throws IOException {
        assertEquals(strings(fixture("merchant-api-wire-contract.json").get("statuses")), TransactionStatuses.ALL);
    }

    @Test
    void thePaymentEventsMatchTheWebhookEventCatalog() throws IOException {
        assertEquals(strings(fixture("merchant-api-wire-contract.json").get("webhookEventCatalog")),
            WebhookEventTypes.PAYMENT);
    }

    @Test
    void theRecurringEventsMatchTheWireContract() throws IOException {
        assertEquals(strings(fixture("merchant-api-wire-contract.json").get("recurringWebhookEvents")),
            WebhookEventTypes.RECURRING);
    }

    @Test
    void theWalletTypesAndReportingFieldsMatch() throws IOException {
        JsonNode wallets = fixture("merchant-api-wire-contract.json").get("wallets");
        assertEquals(strings(wallets.get("walletTypes")), WalletTypes.ALL);
        for (JsonNode field : wallets.get("reportingFields")) {
            String path = field.get("path").asText();
            // The SDK models each reporting field on the status read as an optional string.
            assertEquals("string", field.get("type").asText(), path);
            assertFalse(field.get("required").asBoolean(), path);
            assertTrue(hasField(CheckoutStatus.class, path), "CheckoutStatus does not model " + path);
        }
    }

    @Test
    void theStorefrontAndAuthorizationCodesMatch() throws IOException {
        JsonNode errorCodes = fixture("merchant-api-wire-contract.json").get("errorCodes");
        assertEquals(codes(errorCodes.get("storefront")), ErrorCodes.STOREFRONT);
        assertEquals(codes(errorCodes.get("authorization")), ErrorCodes.AUTHORIZATION);
        for (JsonNode entry : errorCodes.get("authorization")) {
            assertEquals(403, entry.get("httpStatus").asInt());
        }
        List<String> auth = codes(errorCodes.get("auth"));
        assertEquals(List.of(ErrorCodes.INVALID_API_KEY, ErrorCodes.INVALID_SIGNATURE,
            ErrorCodes.TIMESTAMP_OUT_OF_RANGE, ErrorCodes.IP_NOT_ALLOWED), auth);
    }

    @Test
    void theSigningParametersMatch() throws IOException {
        JsonNode wire = fixture("merchant-api-wire-contract.json");
        JsonNode signing = wire.get("requestSigning");
        assertEquals("{timestamp}\n{METHOD}\n{path}\n{idempotency_key}\n{sha256_hex_of_body}",
            signing.get("payloadTemplate").asText());
        assertEquals("HMAC-SHA256", signing.get("algorithm").asText());
        assertEquals(List.of("X-Api-Key-Id", "X-Timestamp", "X-Signature"), strings(signing.get("requiredHeaders")));

        JsonNode webhook = wire.get("webhookSigning");
        assertEquals("{timestamp}.{raw_body}", webhook.get("payloadTemplate").asText());
        assertEquals(Webhooks.SIGNATURE_HEADER, webhook.get("signatureHeader").asText());
        assertEquals(Webhooks.DEFAULT_TOLERANCE_SECONDS, webhook.get("timestampToleranceSeconds").asInt());

        assertEquals(IdempotencyKeys.MAX_LENGTH, wire.get("idempotency").get("maxKeyLength").asInt());
        assertEquals(DominaiteClient.PING_PATH, wire.get("pingPath").asText());
        assertEquals(DominaiteClient.SESSIONS_PATH, wire.get("sessionsPath").asText());
    }

    @Test
    void theRecordedVocabulariesMatchTheContractFixture() throws IOException {
        JsonNode contract = fixture("merchant-api-contract.json");
        assertEquals(strings(contract.get("statusVocabulary")), TransactionStatuses.ALL);
        assertEquals(strings(contract.get("chargeStatusVocabulary")), ChargeStatuses.ALL);
        assertEquals(strings(contract.get("declineClassVocabulary")), DeclineClasses.ALL);
        assertEquals(strings(contract.get("integrationVocabulary")), CheckoutIntegrations.ALL);
        assertEquals(strings(contract.get("refundStatusVocabulary")), RefundStatuses.ALL);
        assertEquals(strings(contract.get("storedPaymentMethodStatusVocabulary")), StoredPaymentMethodStatuses.ALL);
        assertEquals(strings(contract.get("storedPaymentMethodRetiredReasonVocabulary")),
            StoredPaymentMethodRetiredReasons.ALL);
        // The hand fixture lists an older subset of these codes. The SDK knows every one of them.
        assertTrue(ErrorCodes.CHARGE.containsAll(strings(contract.get("chargeErrorCodes"))));
        assertTrue(ErrorCodes.REFUND.containsAll(strings(contract.get("refundErrorCodes"))));
        assertEquals(strings(contract.get("refundFailureCodes")), ErrorCodes.REFUND_FAILURE);
        assertEquals(strings(contract.get("revokeErrorCodes")), ErrorCodes.REVOKE);
        assertTrue(ErrorCodes.SESSION_REFUSALS.containsAll(strings(contract.get("sessionRefusalErrorCodes"))));
        assertEquals(strings(contract.get("storefrontErrorCodes")), ErrorCodes.STOREFRONT);
    }

    private static boolean hasField(Class<?> type, String name) {
        try {
            type.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
