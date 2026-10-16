package com.dominaite.merchant;

import java.util.List;

/**
 * The {@code walletType} values on a status read.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class WalletTypes {

    public static final String APPLE_PAY = "apple_pay";
    public static final String GOOGLE_PAY = "google_pay";
    public static final String SAMSUNG_PAY = "samsung_pay";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(APPLE_PAY, GOOGLE_PAY, SAMSUNG_PAY);

    private WalletTypes() {
    }
}
