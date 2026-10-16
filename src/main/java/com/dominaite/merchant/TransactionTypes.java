package com.dominaite.merchant;

import java.util.List;

/**
 * The transaction types on {@code listTransactions}. A {@code verification} is a card check, not a sale.
 * The values are plain strings, so a value the SDK has never seen never throws.
 */
public final class TransactionTypes {

    public static final String PAYMENT = "payment";
    public static final String CHARGE = "charge";
    public static final String VERIFICATION = "verification";

    /** Every value this SDK version knows, in the gateway's order. */
    public static final List<String> ALL = ErrorCodes.list(PAYMENT, CHARGE, VERIFICATION);

    private TransactionTypes() {
    }
}
