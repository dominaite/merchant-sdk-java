package com.dominaite.merchant;

/**
 * HTTP 403: the credentials are valid, but this key may not make this call. The code tells you
 * why: {@code INSUFFICIENT_SCOPE} (see {@link InsufficientScopeException}) or
 * {@code VERIFY_ONLY_NOT_ENABLED}.
 */
public class PermissionException extends DominaiteException {

    private static final long serialVersionUID = 1L;

    public PermissionException(String code, String message) {
        super(message, code, 403, null);
    }
}
