package com.dominaite.merchant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The error codes the SDK branches on. Codes are plain strings: a code this list does not know
 * still arrives in {@link DominaiteException#getCode()}, and the SDK never throws because of it.
 */
public final class ErrorCodes {

    // Authentication (HTTP 401).
    public static final String INVALID_API_KEY = "INVALID_API_KEY";
    public static final String INVALID_SIGNATURE = "INVALID_SIGNATURE";
    public static final String TIMESTAMP_OUT_OF_RANGE = "TIMESTAMP_OUT_OF_RANGE";
    public static final String IP_NOT_ALLOWED = "IP_NOT_ALLOWED";

    // Permission (HTTP 403).
    public static final String INSUFFICIENT_SCOPE = "INSUFFICIENT_SCOPE";
    public static final String VERIFY_ONLY_NOT_ENABLED = "VERIFY_ONLY_NOT_ENABLED";

    // Rate limits (HTTP 429).
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String CHARGE_VELOCITY_EXCEEDED = "CHARGE_VELOCITY_EXCEEDED";
    public static final String CHARGE_DECLINE_RATE_EXCEEDED = "CHARGE_DECLINE_RATE_EXCEEDED";

    // Storefront.
    public static final String STOREFRONT_NOT_WHITELISTED = "STOREFRONT_NOT_WHITELISTED";
    public static final String STOREFRONT_INACTIVE = "STOREFRONT_INACTIVE";
    public static final String STOREFRONT_MISMATCH = "STOREFRONT_MISMATCH";

    // Idempotency and session refusals.
    public static final String ALREADY_PROCESSED = "ALREADY_PROCESSED";
    public static final String PRIOR_ATTEMPT_FAILED = "PRIOR_ATTEMPT_FAILED";
    public static final String DUPLICATE_REQUEST = "DUPLICATE_REQUEST";
    public static final String PAYMENT_PROCESSING_UNAVAILABLE = "PAYMENT_PROCESSING_UNAVAILABLE";
    public static final String IDEMPOTENCY_KEY_REUSED = "IDEMPOTENCY_KEY_REUSED";
    public static final String IDEMPOTENCY_KEY_REQUIRED = "IDEMPOTENCY_KEY_REQUIRED";

    // Stored payment method charges.
    public static final String PAYMENT_METHOD_NOT_ACTIVE = "PAYMENT_METHOD_NOT_ACTIVE";
    public static final String CHARGE_OUTCOME_UNKNOWN = "CHARGE_OUTCOME_UNKNOWN";
    public static final String CHARGE_FAILED = "CHARGE_FAILED";
    public static final String CHARGE_DECLINED = "CHARGE_DECLINED";
    public static final String PAYMENT_METHOD_CHARGES_DISABLED = "PAYMENT_METHOD_CHARGES_DISABLED";

    // Revoke.
    public static final String UPSTREAM_CONTRACT_ERROR = "UPSTREAM_CONTRACT_ERROR";
    public static final String MERCHANT_API_UNAVAILABLE = "MERCHANT_API_UNAVAILABLE";

    // Refunds.
    public static final String PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND";
    public static final String REFUND_NOT_FOUND = "REFUND_NOT_FOUND";
    public static final String PAYMENT_NOT_REFUNDABLE = "PAYMENT_NOT_REFUNDABLE";
    public static final String REFUND_AMOUNT_EXCEEDED = "REFUND_AMOUNT_EXCEEDED";
    public static final String REFUND_FAILED = "REFUND_FAILED";

    /** The codes the gateway contract lists under {@code authorization} (HTTP 403). */
    public static final List<String> AUTHORIZATION = list(INSUFFICIENT_SCOPE);

    /** Every 403 code the SDK gives its own message. */
    public static final List<String> PERMISSION = list(INSUFFICIENT_SCOPE, VERIFY_ONLY_NOT_ENABLED);

    /** The 429 codes. A plain 429 can also arrive without a code. */
    public static final List<String> RATE_LIMIT = list(
        RATE_LIMIT_EXCEEDED, CHARGE_VELOCITY_EXCEEDED, CHARGE_DECLINE_RATE_EXCEEDED);

    /** The storefront codes, in the gateway contract's order. */
    public static final List<String> STOREFRONT = list(
        STOREFRONT_MISMATCH, STOREFRONT_INACTIVE, STOREFRONT_NOT_WHITELISTED);

    /** The codes of a checkout session answered with {@code success: false}. */
    public static final List<String> SESSION_REFUSALS = list(
        PAYMENT_PROCESSING_UNAVAILABLE, DUPLICATE_REQUEST, ALREADY_PROCESSED, IDEMPOTENCY_KEY_REUSED,
        PRIOR_ATTEMPT_FAILED);

    /** The codes a stored payment method charge is refused with. */
    public static final List<String> CHARGE = list(
        PAYMENT_METHOD_NOT_ACTIVE, DUPLICATE_REQUEST, IDEMPOTENCY_KEY_REUSED, CHARGE_OUTCOME_UNKNOWN,
        CHARGE_FAILED, PAYMENT_METHOD_CHARGES_DISABLED, PAYMENT_PROCESSING_UNAVAILABLE);

    /** The codes a revoke is refused with. */
    public static final List<String> REVOKE = list(UPSTREAM_CONTRACT_ERROR, MERCHANT_API_UNAVAILABLE);

    /** The codes a refund call is refused with. */
    public static final List<String> REFUND = list(
        PAYMENT_NOT_FOUND, REFUND_NOT_FOUND, PAYMENT_NOT_REFUNDABLE, REFUND_AMOUNT_EXCEEDED,
        IDEMPOTENCY_KEY_REUSED, DUPLICATE_REQUEST, IDEMPOTENCY_KEY_REQUIRED);

    /** The {@link Refund#getFailureCode()} values of a failed refund. */
    public static final List<String> REFUND_FAILURE = list(
        REFUND_AMOUNT_EXCEEDED, PAYMENT_NOT_REFUNDABLE, REFUND_FAILED);

    private ErrorCodes() {
    }

    static List<String> list(String... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }
}
