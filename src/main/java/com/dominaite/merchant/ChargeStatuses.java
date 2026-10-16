package com.dominaite.merchant;

import java.util.List;

/**
 * The stored payment method charge statuses.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class ChargeStatuses {

    public static final String SUCCEEDED = "succeeded";
    public static final String FAILED = "failed";
    public static final String PENDING = "pending";
    public static final String CANCELLED = "cancelled";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(SUCCEEDED, FAILED, PENDING, CANCELLED);

    private ChargeStatuses() {
    }
}
