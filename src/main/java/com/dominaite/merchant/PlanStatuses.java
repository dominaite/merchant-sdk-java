package com.dominaite.merchant;

import java.util.List;

/**
 * The plan statuses.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class PlanStatuses {

    public static final String ACTIVE = "active";
    public static final String ARCHIVED = "archived";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(ACTIVE, ARCHIVED);

    private PlanStatuses() {
    }
}
