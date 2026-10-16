package com.dominaite.merchant;

import java.util.List;

/**
 * The billing interval units of a plan.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class IntervalUnits {

    public static final String DAY = "day";
    public static final String WEEK = "week";
    public static final String MONTH = "month";
    public static final String YEAR = "year";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(DAY, WEEK, MONTH, YEAR);

    private IntervalUnits() {
    }
}
