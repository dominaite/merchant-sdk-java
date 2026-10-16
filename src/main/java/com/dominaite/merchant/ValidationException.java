package com.dominaite.merchant;

/** The SDK refused the call before it sent anything: an argument is missing or wrong. */
public final class ValidationException extends DominaiteException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message, null, null, null);
    }
}
