package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** IdempotencyKeys and MinorUnits. */
class HelpersTest {

    @Test
    void forOrderBuildsTheDocumentedKey() {
        assertEquals("checkout-order-1042-2500-EUR", IdempotencyKeys.forOrder(" checkout ", "order-1042", 2500, "eur"));
    }

    @Test
    void forOrderRefusesBadParts() {
        assertThrows(ValidationException.class, () -> IdempotencyKeys.forOrder("", "o", 1, "EUR"));
        assertThrows(ValidationException.class, () -> IdempotencyKeys.forOrder("s", " ", 1, "EUR"));
        assertThrows(ValidationException.class, () -> IdempotencyKeys.forOrder("s", "o", 0, "EUR"));
        assertThrows(ValidationException.class, () -> IdempotencyKeys.forOrder("s", "o", 1, "EURO"));
        assertThrows(ValidationException.class, () -> IdempotencyKeys.forOrder("s", "o", 1, "E1R"));
    }

    @Test
    void validateEnforcesTheGatewayRules() {
        assertEquals("a-b_c.1", IdempotencyKeys.validate("a-b_c.1"));
        assertEquals("x".repeat(100), IdempotencyKeys.validate("x".repeat(100)));
        assertThrows(ValidationException.class, () -> IdempotencyKeys.validate(null));
        assertThrows(ValidationException.class, () -> IdempotencyKeys.validate("   "));
        assertThrows(ValidationException.class, () -> IdempotencyKeys.validate("x".repeat(101)));
        assertThrows(ValidationException.class, () -> IdempotencyKeys.validate("has space"));
        assertThrows(ValidationException.class, () -> IdempotencyKeys.validate("café"));
    }

    @Test
    void minorUnitsFollowTheGatewayExponents() {
        assertEquals(2500, MinorUnits.from(new BigDecimal("25.00"), "EUR"));
        assertEquals(2550, MinorUnits.from(new BigDecimal("25.5"), "eur"));
        assertEquals(1000, MinorUnits.from(new BigDecimal("1000"), "HUF"));
        assertEquals(1234, MinorUnits.from(new BigDecimal("1.234"), "KWD"));
        assertEquals(0, MinorUnits.exponent("JPY"));
    }

    @Test
    void minorUnitsRefuseWhatCannotConvertSafely() {
        assertThrows(ValidationException.class, () -> MinorUnits.from(new BigDecimal("25.001"), "EUR"));
        assertThrows(ValidationException.class, () -> MinorUnits.from(new BigDecimal("25.000"), "EUR"));
        assertThrows(ValidationException.class, () -> MinorUnits.from(new BigDecimal("-1"), "EUR"));
        assertThrows(ValidationException.class, () -> MinorUnits.from(new BigDecimal("1"), "ISK"));
        assertThrows(ValidationException.class, () -> MinorUnits.from(new BigDecimal("1"), "XXX"));
        assertThrows(ValidationException.class, () -> MinorUnits.from(new BigDecimal("1e30"), "EUR"));
    }
}
