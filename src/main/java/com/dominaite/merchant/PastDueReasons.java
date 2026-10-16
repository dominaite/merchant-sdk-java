package com.dominaite.merchant;

import java.util.List;

/**
 * Why an agreement is past due.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class PastDueReasons {

    public static final String CARD_EXPIRED = "card_expired";
    public static final String CARD_REVOKED = "card_revoked";
    public static final String HARD_DECLINE = "hard_decline";
    public static final String DUNNING_EXHAUSTED = "dunning_exhausted";
    public static final String SCA_REQUIRED = "sca_required";
    public static final String UNPROCESSABLE = "unprocessable";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(CARD_EXPIRED, CARD_REVOKED, HARD_DECLINE, DUNNING_EXHAUSTED, SCA_REQUIRED, UNPROCESSABLE);

    private PastDueReasons() {
    }
}
