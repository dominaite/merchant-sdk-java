package com.dominaite.merchant;

/**
 * The API could not be reached, did not answer in time, or answered with HTTP 5xx. The outcome
 * of a write is unknown: retry with the same idempotency key.
 */
public final class TransportException extends DominaiteException {

    private static final long serialVersionUID = 1L;

    public TransportException(String message, Integer httpStatus, String code, Throwable cause) {
        super(message, code, httpStatus, cause);
    }

    @Override
    public boolean isRetryable() {
        return true;
    }
}
