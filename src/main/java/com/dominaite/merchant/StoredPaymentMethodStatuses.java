package com.dominaite.merchant;

import java.util.List;

/**
 * The stored payment method statuses. Only {@code active} can be charged.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class StoredPaymentMethodStatuses {

    public static final String ACTIVE = "active";
    public static final String REVOKED = "revoked";
    public static final String EXPIRED = "expired";
    public static final String RETIRED = "retired";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(ACTIVE, REVOKED, EXPIRED, RETIRED);

    private StoredPaymentMethodStatuses() {
    }
}
