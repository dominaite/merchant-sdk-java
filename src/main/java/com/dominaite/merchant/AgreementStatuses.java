package com.dominaite.merchant;

import java.util.List;

/**
 * The subscription agreement statuses.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class AgreementStatuses {

    public static final String PENDING = "pending";
    public static final String ACTIVE = "active";
    public static final String PAST_DUE = "past_due";
    public static final String CANCELLED = "cancelled";
    public static final String COMPLETED = "completed";
    public static final String FAILED = "failed";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(PENDING, ACTIVE, PAST_DUE, CANCELLED, COMPLETED, FAILED);

    private AgreementStatuses() {
    }
}
