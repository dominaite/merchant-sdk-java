package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The error code constants against the {@code errorCodes} of the vendored wire contract. A new
 * group or code in the fixture fails here until the SDK has a constant for it.
 */
class ErrorCodesTest {

    private static Map<String, List<String>> groups() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("auth", ErrorCodes.AUTHENTICATION);
        groups.put("authorization", ErrorCodes.AUTHORIZATION);
        groups.put("idempotency", ErrorCodes.IDEMPOTENCY);
        groups.put("storefront", ErrorCodes.STOREFRONT);
        groups.put("transient", ErrorCodes.TRANSIENT);
        groups.put("validation", ErrorCodes.VALIDATION);
        return groups;
    }

    @Test
    void everyGroupOfTheWireContractHasAListInTheSameOrder() throws IOException {
        JsonNode errorCodes = ContractTest.fixture("merchant-api-wire-contract.json").get("errorCodes");
        List<String> names = new ArrayList<>();
        errorCodes.fieldNames().forEachRemaining(names::add);
        assertEquals(new ArrayList<>(groups().keySet()), names);
        for (Map.Entry<String, List<String>> group : groups().entrySet()) {
            assertEquals(ContractTest.codes(errorCodes.get(group.getKey())), group.getValue(), group.getKey());
        }
    }

    @Test
    void everyCodeOfTheWireContractIsAConstantOfTheSameName() throws Exception {
        JsonNode errorCodes = ContractTest.fixture("merchant-api-wire-contract.json").get("errorCodes");
        for (JsonNode group : errorCodes) {
            for (String code : ContractTest.codes(group)) {
                Field field = ErrorCodes.class.getField(code);
                assertTrue(Modifier.isStatic(field.getModifiers()), code);
                assertEquals(code, field.get(null));
            }
        }
    }

    @Test
    void everyListedCodeIsAConstant() throws Exception {
        List<List<String>> lists = List.of(ErrorCodes.SESSION_REFUSALS, ErrorCodes.CHARGE, ErrorCodes.REFUND,
            ErrorCodes.REVOKE, ErrorCodes.RECURRING, ErrorCodes.WEBHOOK, ErrorCodes.RATE_LIMIT,
            ErrorCodes.REFUND_FAILURE, ErrorCodes.PERMISSION);
        for (List<String> list : lists) {
            assertEquals(list.size(), list.stream().distinct().count(), list.toString());
            for (String code : list) {
                assertEquals(code, ErrorCodes.class.getField(code).get(null));
            }
        }
    }

    @Test
    void theChargePlanAgreementAndWebhookCodesHaveConstants() {
        // The wire contract has no group for these routes. The codes come from the gateway's
        // published contract for each route.
        assertTrue(ErrorCodes.CHARGE.containsAll(List.of("PAYMENT_METHOD_NOT_FOUND", "CHARGE_IN_PROGRESS",
            "CHARGE_CURRENCY_MISMATCH", "AGREEMENT_NOT_FOUND", "AGREEMENT_ENDED", "CHARGE_AMOUNT_ABOVE_CAP",
            "CHARGE_DECLINED")));
        assertTrue(ErrorCodes.SESSION_REFUSALS.containsAll(List.of("STOREFRONT_MISMATCH", "AGREEMENT_SESSION_OPEN",
            "SUBSCRIPTIONS_NOT_ENABLED")));
        assertTrue(ErrorCodes.RECURRING.containsAll(List.of("PLAN_NOT_FOUND", "PLAN_NOT_ACTIVE", "AGREEMENT_ENDED",
            "MAX_AMOUNT_BELOW_PLAN_AMOUNT")));
        assertTrue(ErrorCodes.WEBHOOK.containsAll(List.of("WEBHOOK_NOT_FOUND", "WEBHOOK_LIMIT_REACHED",
            "WEBHOOK_ALREADY_REGISTERED", "WEBHOOK_URL_REJECTED")));
    }
}
