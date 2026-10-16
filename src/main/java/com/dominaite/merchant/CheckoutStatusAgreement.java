package com.dominaite.merchant;

import java.time.Instant;

/**
 * The agreement a payment belongs to.
 */
public final class CheckoutStatusAgreement {

    private String id;
    private String status;
    private Instant nextChargeAt;

    private CheckoutStatusAgreement() {
    }

    public String getId() {
        return id;
    }

    /** One of {@link AgreementStatuses}. */
    public String getStatus() {
        return status;
    }

    public Instant getNextChargeAt() {
        return nextChargeAt;
    }
}
