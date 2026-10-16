package com.dominaite.merchant;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts a decimal amount to the minor units the API takes. Every amount in this SDK is in
 * minor units: 2500 is 25.00 EUR.
 */
public final class MinorUnits {

    private static final Map<String, Integer> EXPONENTS;
    private static final Set<String> UNSUPPORTED = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("ISK", "KRW", "OMR", "JOD", "TND")));

    static {
        Map<String, Integer> exponents = new LinkedHashMap<>();
        for (String code : new String[] {
            "EUR", "USD", "GBP", "CAD", "AUD", "CHF", "BGN", "RON", "PLN", "CZK", "SEK", "DKK", "NOK"}) {
            exponents.put(code, 2);
        }
        exponents.put("JPY", 0);
        // Whole forints: the gateway's exponent, not the 2 of ISO 4217.
        exponents.put("HUF", 0);
        exponents.put("BHD", 3);
        exponents.put("KWD", 3);
        EXPONENTS = Collections.unmodifiableMap(exponents);
    }

    private MinorUnits() {
    }

    /** The currencies this class can convert. */
    public static Set<String> currencies() {
        return EXPONENTS.keySet();
    }

    /**
     * The number of decimals of a currency's minor unit.
     *
     * @param currency the ISO 4217 code
     * @return the exponent
     * @throws ValidationException for a currency the SDK cannot convert safely
     */
    public static int exponent(String currency) {
        String code = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        if (UNSUPPORTED.contains(code)) {
            throw new ValidationException("Currency " + code + " is not supported: the gateway and ISO 4217 "
                + "disagree on its minor unit, so the amount cannot be converted safely");
        }
        Integer exponent = EXPONENTS.get(code);
        if (exponent == null) {
            throw new ValidationException("Unknown currency '" + currency
                + "': no minor-unit exponent for it, so the amount cannot be converted safely");
        }
        return exponent;
    }

    /**
     * Converts a decimal amount to minor units. Refuses an amount with more decimals than the
     * currency allows, so {@code 25.000} is refused for EUR: round it yourself first.
     *
     * @param amount the amount, for example {@code new BigDecimal("25.00")}
     * @param currency the ISO 4217 code
     * @return the amount in minor units, for example 2500
     */
    public static long from(BigDecimal amount, String currency) {
        int exponent = exponent(currency);
        String code = currency.trim().toUpperCase(Locale.ROOT);
        if (amount.signum() < 0) {
            throw new ValidationException("Amount " + amount + " " + code + " must not be negative");
        }
        if (amount.scale() > exponent) {
            throw new ValidationException("Amount " + amount + " has more decimals than " + code
                + " allows (" + exponent + "); round it yourself first");
        }
        try {
            return amount.movePointRight(exponent).longValueExact();
        } catch (ArithmeticException e) {
            throw new ValidationException("Amount " + amount + " " + code + " is too large to convert");
        }
    }
}
