package com.dominaite.merchant;

/**
 * A card kept on file. Display fields only: the card number never reaches this SDK.
 */
public final class StoredPaymentMethod {

    private String id;
    private String brand;
    private String last4;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String status;
    private String retiredReason;

    private StoredPaymentMethod() {
    }

    /** The id to charge with chargePaymentMethod. */
    public String getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getLast4() {
        return last4;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    /** One of {@link StoredPaymentMethodStatuses}. */
    public String getStatus() {
        return status;
    }

    /** One of {@link StoredPaymentMethodRetiredReasons}, or null. */
    public String getRetiredReason() {
        return retiredReason;
    }

    /** True when the status is {@code active}. */
    public boolean isChargeable() {
        return StoredPaymentMethodStatuses.ACTIVE.equals(status);
    }
}
