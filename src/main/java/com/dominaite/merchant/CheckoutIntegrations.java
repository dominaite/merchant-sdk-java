package com.dominaite.merchant;

import java.util.List;

/**
 * How the checkout renders: the hosted widget or card fields.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class CheckoutIntegrations {

    public static final String WIDGET = "widget";
    public static final String FIELDS = "fields";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(WIDGET, FIELDS);

    private CheckoutIntegrations() {
    }
}
