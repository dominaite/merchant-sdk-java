package com.dominaite.merchant;

import java.util.List;

/**
 * Why the platform retired a stored payment method.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class StoredPaymentMethodRetiredReasons {

    public static final String HARD_DECLINE = "hard_decline";
    public static final String CHARGEBACK = "chargeback";
    public static final String SOURCE_SALE_REVERSED = "source_sale_reversed";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(HARD_DECLINE, CHARGEBACK, SOURCE_SALE_REVERSED);

    private StoredPaymentMethodRetiredReasons() {
    }
}
