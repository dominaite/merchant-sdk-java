package com.dominaite.merchant;

import java.util.List;

/**
 * The webhook endpoint statuses.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class WebhookEndpointStatuses {

    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(ACTIVE, INACTIVE);

    private WebhookEndpointStatuses() {
    }
}
