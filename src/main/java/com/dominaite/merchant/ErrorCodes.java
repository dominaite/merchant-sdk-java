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
    public static final String PAYMENT_METHOD_NOT_FOUND = "PAYMENT_METHOD_NOT_FOUND";
    public static final String CHARGE_IN_PROGRESS = "CHARGE_IN_PROGRESS";
    public static final String CHARGE_CURRENCY_MISMATCH = "CHARGE_CURRENCY_MISMATCH";
    public static final String CHARGE_AMOUNT_ABOVE_AGREEMENT = "CHARGE_AMOUNT_ABOVE_AGREEMENT";
    public static final String CHARGE_AMOUNT_ABOVE_CAP = "CHARGE_AMOUNT_ABOVE_CAP";

    // Agreements, on a charge, a checkout session or the agreement routes.
    public static final String AGREEMENT_NOT_FOUND = "AGREEMENT_NOT_FOUND";
    public static final String AGREEMENT_NOT_PENDING = "AGREEMENT_NOT_PENDING";
    public static final String AGREEMENT_NOT_STARTED = "AGREEMENT_NOT_STARTED";
    public static final String AGREEMENT_ENDED = "AGREEMENT_ENDED";
    public static final String AGREEMENT_METHOD_MISMATCH = "AGREEMENT_METHOD_MISMATCH";
    public static final String AGREEMENT_CONSENT_MISSING = "AGREEMENT_CONSENT_MISSING";
    public static final String AGREEMENT_CONSENT_REQUIRED = "AGREEMENT_CONSENT_REQUIRED";
    public static final String AGREEMENT_CURRENCY_MISMATCH = "AGREEMENT_CURRENCY_MISMATCH";
    public static final String AGREEMENT_AMOUNT_MISMATCH = "AGREEMENT_AMOUNT_MISMATCH";
    public static final String AGREEMENT_PERIOD_ALREADY_CHARGED = "AGREEMENT_PERIOD_ALREADY_CHARGED";
    public static final String AGREEMENT_SESSION_OPEN = "AGREEMENT_SESSION_OPEN";
    public static final String AGREEMENT_ALREADY_PAID = "AGREEMENT_ALREADY_PAID";
    public static final String AGREEMENT_VARIABLE_AMOUNT_UNSUPPORTED = "AGREEMENT_VARIABLE_AMOUNT_UNSUPPORTED";
    public static final String SUBSCRIPTIONS_NOT_ENABLED = "SUBSCRIPTIONS_NOT_ENABLED";

    // Plans and the agreement routes.
    public static final String PLAN_NOT_FOUND = "PLAN_NOT_FOUND";
    public static final String PLAN_NOT_ACTIVE = "PLAN_NOT_ACTIVE";
    public static final String MAX_AMOUNT_BELOW_PLAN_AMOUNT = "MAX_AMOUNT_BELOW_PLAN_AMOUNT";
    public static final String PAYMENT_METHOD_CURRENCY_MISMATCH = "PAYMENT_METHOD_CURRENCY_MISMATCH";
    public static final String PAYMENT_METHOD_CURRENCY_UNKNOWN = "PAYMENT_METHOD_CURRENCY_UNKNOWN";

    // Webhook endpoints.
    public static final String WEBHOOK_NOT_FOUND = "WEBHOOK_NOT_FOUND";
    public static final String WEBHOOK_LIMIT_REACHED = "WEBHOOK_LIMIT_REACHED";
    public static final String WEBHOOK_ALREADY_REGISTERED = "WEBHOOK_ALREADY_REGISTERED";
    public static final String WEBHOOK_URL_REJECTED = "WEBHOOK_URL_REJECTED";

    // Revoke.
    public static final String UPSTREAM_CONTRACT_ERROR = "UPSTREAM_CONTRACT_ERROR";
    public static final String MERCHANT_API_UNAVAILABLE = "MERCHANT_API_UNAVAILABLE";

    // Refunds.
    public static final String PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND";
    public static final String REFUND_NOT_FOUND = "REFUND_NOT_FOUND";
    public static final String PAYMENT_NOT_REFUNDABLE = "PAYMENT_NOT_REFUNDABLE";
    public static final String REFUND_AMOUNT_EXCEEDED = "REFUND_AMOUNT_EXCEEDED";
    public static final String REFUND_FAILED = "REFUND_FAILED";

    // Field codes in error.validationErrors[].code of a 400.
    public static final String REQUIRED = "REQUIRED";
    public static final String MIN_VALUE = "MIN_VALUE";
    public static final String MAX_LENGTH = "MAX_LENGTH";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";
    public static final String INVALID_SELECTION = "INVALID_SELECTION";
    public static final String DATE_IN_FUTURE = "DATE_IN_FUTURE";
    public static final String INVALID_EMAIL = "INVALID_EMAIL";
    public static final String INVALID_CURRENCY = "INVALID_CURRENCY";

    // Generic codes of a refusal that has no specific code, by HTTP status.
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";

    /** The codes the gateway contract lists under {@code auth} (HTTP 401). */
    public static final List<String> AUTHENTICATION = list(
        INVALID_API_KEY, INVALID_SIGNATURE, TIMESTAMP_OUT_OF_RANGE, IP_NOT_ALLOWED);

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

    /** The codes the gateway contract lists under {@code idempotency}. */
    public static final List<String> IDEMPOTENCY = list(
        IDEMPOTENCY_KEY_REQUIRED, DUPLICATE_REQUEST, ALREADY_PROCESSED, PRIOR_ATTEMPT_FAILED, IDEMPOTENCY_KEY_REUSED);

    /** The codes the gateway contract lists under {@code transient}: retry later with the same key. */
    public static final List<String> TRANSIENT = list(MERCHANT_API_UNAVAILABLE, PAYMENT_PROCESSING_UNAVAILABLE);

    /** The field codes of a 400, which the gateway contract lists under {@code validation}. */
    public static final List<String> VALIDATION = list(
        REQUIRED, MIN_VALUE, MAX_LENGTH, INVALID_FORMAT, INVALID_SELECTION, DATE_IN_FUTURE, INVALID_EMAIL,
        INVALID_CURRENCY);

    /**
     * The codes of a checkout session answered with {@code success: false}. A
     * {@code STOREFRONT_MISMATCH} among them arrives as a {@link StorefrontException}, every other
     * code as a {@link RefusalException}.
     */
    public static final List<String> SESSION_REFUSALS = list(
        PAYMENT_PROCESSING_UNAVAILABLE, DUPLICATE_REQUEST, ALREADY_PROCESSED, IDEMPOTENCY_KEY_REUSED,
        PRIOR_ATTEMPT_FAILED, STOREFRONT_MISMATCH, AGREEMENT_NOT_FOUND, AGREEMENT_NOT_PENDING,
        AGREEMENT_AMOUNT_MISMATCH, AGREEMENT_CONSENT_REQUIRED, AGREEMENT_SESSION_OPEN, AGREEMENT_ALREADY_PAID,
        AGREEMENT_VARIABLE_AMOUNT_UNSUPPORTED, SUBSCRIPTIONS_NOT_ENABLED);

    /**
     * The codes a stored payment method charge is refused with. {@code CHARGE_DECLINED} comes with
     * the declined charge, which the SDK returns as a result. The 429 codes are in {@link #RATE_LIMIT}.
     */
    public static final List<String> CHARGE = list(
        PAYMENT_METHOD_NOT_ACTIVE, DUPLICATE_REQUEST, IDEMPOTENCY_KEY_REUSED, CHARGE_OUTCOME_UNKNOWN,
        CHARGE_FAILED, PAYMENT_METHOD_CHARGES_DISABLED, PAYMENT_PROCESSING_UNAVAILABLE, CHARGE_DECLINED,
        PAYMENT_METHOD_NOT_FOUND, CHARGE_IN_PROGRESS, CHARGE_CURRENCY_MISMATCH, AGREEMENT_NOT_FOUND,
        AGREEMENT_METHOD_MISMATCH, AGREEMENT_CONSENT_MISSING, AGREEMENT_CURRENCY_MISMATCH, AGREEMENT_AMOUNT_MISMATCH,
        CHARGE_AMOUNT_ABOVE_AGREEMENT, AGREEMENT_PERIOD_ALREADY_CHARGED, AGREEMENT_ENDED, AGREEMENT_NOT_STARTED,
        CHARGE_AMOUNT_ABOVE_CAP);

    /** The codes a revoke is refused with. */
    public static final List<String> REVOKE = list(UPSTREAM_CONTRACT_ERROR, MERCHANT_API_UNAVAILABLE);

    /** The codes a refund call is refused with. */
    public static final List<String> REFUND = list(
        PAYMENT_NOT_FOUND, REFUND_NOT_FOUND, PAYMENT_NOT_REFUNDABLE, REFUND_AMOUNT_EXCEEDED,
        IDEMPOTENCY_KEY_REUSED, DUPLICATE_REQUEST, IDEMPOTENCY_KEY_REQUIRED, MERCHANT_API_UNAVAILABLE);

    /** The codes the plan and agreement routes are refused with. */
    public static final List<String> RECURRING = list(
        PLAN_NOT_FOUND, PLAN_NOT_ACTIVE, AGREEMENT_NOT_FOUND, AGREEMENT_ENDED, PAYMENT_METHOD_NOT_FOUND,
        PAYMENT_METHOD_NOT_ACTIVE, PAYMENT_METHOD_CURRENCY_MISMATCH, PAYMENT_METHOD_CURRENCY_UNKNOWN,
        MAX_AMOUNT_BELOW_PLAN_AMOUNT, AGREEMENT_CONSENT_REQUIRED, IDEMPOTENCY_KEY_REUSED, INVALID_CURRENCY,
        INVALID_SELECTION, MERCHANT_API_UNAVAILABLE);

    /** The codes the webhook endpoint routes are refused with. */
    public static final List<String> WEBHOOK = list(
        WEBHOOK_NOT_FOUND, WEBHOOK_LIMIT_REACHED, WEBHOOK_ALREADY_REGISTERED, WEBHOOK_URL_REJECTED,
        IDEMPOTENCY_KEY_REUSED, DUPLICATE_REQUEST, MERCHANT_API_UNAVAILABLE);

    /** The {@link Refund#getFailureCode()} values of a failed refund. */
    public static final List<String> REFUND_FAILURE = list(
        REFUND_AMOUNT_EXCEEDED, PAYMENT_NOT_REFUNDABLE, REFUND_FAILED);

    private ErrorCodes() {
    }

    static List<String> list(String... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }
}
