package com.dominaite.merchant;

import java.util.List;

/**
 * The decline classes of a failed charge or payment. A decline can also have no class (null).
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class DeclineClasses {

    public static final String HARD = "hard";
    public static final String SOFT_FUNDS = "soft_funds";
    public static final String SOFT_SCA_REQUIRED = "soft_sca_required";
    public static final String SOFT_OTHER = "soft_other";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(HARD, SOFT_FUNDS, SOFT_SCA_REQUIRED, SOFT_OTHER);

    private DeclineClasses() {
    }
}
