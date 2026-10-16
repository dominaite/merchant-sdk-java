package com.dominaite.merchant;

/** A webhook delivery failed verification. Answer it with HTTP 400 and do not act on it. */
public final class WebhookVerificationException extends DominaiteException {

    private static final long serialVersionUID = 1L;

    private final WebhookFailureReason reason;

    public WebhookVerificationException(WebhookFailureReason reason, String message) {
        super(message, null, null, null);
        this.reason = reason;
    }

    public WebhookFailureReason getReason() {
        return reason;
    }
}
