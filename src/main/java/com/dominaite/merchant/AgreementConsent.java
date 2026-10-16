package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;

/**
 * The payer accepted the platform's recurring-payment wording: which version, when, and
 * from which IP address.
 */
@JsonPropertyOrder({"textVersion", "acceptedAt", "ip"})
public final class AgreementConsent {

    private final String textVersion;
    private final Instant acceptedAt;
    private final String ip;

    private AgreementConsent(Builder b) {
        this.textVersion = b.textVersion;
        this.acceptedAt = b.acceptedAt;
        this.ip = b.ip;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with every field of this object, to change one and build again. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.textVersion = textVersion;
        b.acceptedAt = acceptedAt;
        b.ip = ip;
        return b;
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

    /** Builds a {@link AgreementConsent}. */
    public static final class Builder {

        private String textVersion;
        private Instant acceptedAt;
        private String ip;

        private Builder() {
        }

        /** The wording version, one of the contract's consentTextVersions. */
        public Builder textVersion(String textVersion) {
            this.textVersion = textVersion;
            return this;
        }

        /** When the payer accepted, in UTC. */
        public Builder acceptedAt(Instant acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        /** The payer's IP address when they accepted. */
        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }

        public AgreementConsent build() {
            return new AgreementConsent(this);
        }
    }
}
