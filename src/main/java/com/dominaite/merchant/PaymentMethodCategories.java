package com.dominaite.merchant;

import java.util.List;

/**
 * The {@code paymentMethod} values on a status read.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class PaymentMethodCategories {

    public static final String CARD = "card";
    public static final String WALLET = "wallet";
    public static final String BANK_TRANSFER = "bank_transfer";
    public static final String SEPA = "sepa";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(CARD, WALLET, BANK_TRANSFER, SEPA);

    private PaymentMethodCategories() {
    }
}
