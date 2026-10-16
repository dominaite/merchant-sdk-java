package com.dominaite.merchant;

import java.time.Instant;

/**
 * The consent stored on an agreement, with when and from where the platform received it.
 */
public final class AgreementConsentRecord {

    private String textVersion;
    private Instant acceptedAt;
    private String ip;
    private Instant receivedAt;
    private String receivedFromIp;

    private AgreementConsentRecord() {
    }

    public String getTextVersion() {
        return textVersion;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public String getIp() {
        return ip;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getReceivedFromIp() {
        return receivedFromIp;
    }
}
