package com.dominaite.merchant;

/**
 * The card of a {@code charge.*} event: brand and last four digits. Either can be null.
 */
public final class ChargeEventPaymentMethod {

    private String brand;
    private String last4;

    private ChargeEventPaymentMethod() {
    }

    /** The card brand, for example {@code visa}, or null. */
    public String getBrand() {
        return brand;
    }

    /** The last four digits of the card, or null. */
    public String getLast4() {
        return last4;
    }
}
