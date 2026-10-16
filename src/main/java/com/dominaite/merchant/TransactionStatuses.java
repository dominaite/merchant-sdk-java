package com.dominaite.merchant;

import java.util.List;

/**
 * The payment statuses.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class TransactionStatuses {

    public static final String PENDING = "pending";
    public static final String PROCESSING = "processing";
    public static final String SUCCEEDED = "succeeded";
    public static final String FAILED = "failed";
    public static final String REFUNDED = "refunded";
    public static final String PARTIALLY_REFUNDED = "partially_refunded";
    public static final String CANCELLED = "cancelled";
    public static final String DISPUTED = "disputed";
    public static final String REQUIRES_CAPTURE = "requires_capture";
    public static final String ABANDONED = "abandoned";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(PENDING, PROCESSING, SUCCEEDED, FAILED, REFUNDED, PARTIALLY_REFUNDED, CANCELLED, DISPUTED, REQUIRES_CAPTURE, ABANDONED);

    private TransactionStatuses() {
    }
}
