package com.dominaite.merchant;

import java.util.Locale;

/**
 * Builds and checks idempotency keys. Every create call needs one. Derive it from the order, so
 * a reload or a retry replays the same request instead of making a second payment.
 */
public final class IdempotencyKeys {

    /** The longest key the gateway accepts. */
    public static final int MAX_LENGTH = 100;

    private IdempotencyKeys() {
    }

    /**
     * A key derived from the order: {@code "{scope}-{orderId}-{amountMinor}-{CURRENCY}"}. A new
     * amount or currency gives a new key, so a changed cart opens a new session.
     *
     * @param scope what the key is for, for example {@code "checkout"}
     * @param orderId your order id
     * @param amountMinor the amount in minor units; must be positive
     * @param currency the ISO 4217 code
     * @return the key
     */
    public static String forOrder(String scope, String orderId, long amountMinor, String currency) {
        if (scope == null || scope.trim().isEmpty()) {
            throw new ValidationException("scope must not be empty");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new ValidationException("orderId must not be empty");
        }
        if (amountMinor <= 0) {
            throw new ValidationException(
                "amountMinor must be a positive integer in MINOR units (e.g. 2500 for 25.00 EUR)");
        }
        String code = currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
        if (code == null || code.length() != 3 || !code.chars().allMatch(c -> c >= 'A' && c <= 'Z')) {
            throw new ValidationException("currency must be a three-letter ISO 4217 code, e.g. EUR");
        }
        return validate(scope.trim() + "-" + orderId.trim() + "-" + amountMinor + "-" + code);
    }

    /**
     * Checks a key against the gateway's rules: present, 1 to 100 characters, visible ASCII only.
     *
     * @param key the key
     * @return the same key
     * @throws ValidationException when the key breaks a rule
     */
    public static String validate(String key) {
        if (key == null) {
            throw new ValidationException(
                "idempotencyKey is required. Derive it from the order with IdempotencyKeys.forOrder, "
                    + "so a reload or a retry replays the same session instead of opening a second payment");
        }
        if (key.trim().isEmpty()) {
            throw new ValidationException("idempotencyKey must not be empty");
        }
        if (key.length() > MAX_LENGTH) {
            throw new ValidationException("idempotencyKey must be at most " + MAX_LENGTH + " characters");
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < 0x21 || c > 0x7e) {
                throw new ValidationException(
                    "idempotencyKey must be visible ASCII only (no spaces, control or non-ASCII characters)");
            }
        }
        return key;
    }
}
