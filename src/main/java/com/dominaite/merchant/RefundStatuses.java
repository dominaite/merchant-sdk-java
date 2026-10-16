package com.dominaite.merchant;

import java.util.List;

/**
 * The refund statuses.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class RefundStatuses {

    public static final String PENDING = "pending";
    public static final String PROCESSING = "processing";
    public static final String SUCCEEDED = "succeeded";
    public static final String FAILED = "failed";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(PENDING, PROCESSING, SUCCEEDED, FAILED);

    private RefundStatuses() {
    }
}
