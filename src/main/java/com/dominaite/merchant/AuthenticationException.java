package com.dominaite.merchant;

/**
 * HTTP 401: the API did not accept the credentials. Check your key id, secret, and server clock.
 * The codes are {@code INVALID_API_KEY}, {@code INVALID_SIGNATURE},
 * {@code TIMESTAMP_OUT_OF_RANGE} and {@code IP_NOT_ALLOWED}.
 */
public final class AuthenticationException extends DominaiteException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String code, String message, int httpStatus) {
        super(message, code, httpStatus, null);
    }
}
