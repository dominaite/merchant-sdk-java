package com.dominaite.merchant;

/**
 * HTTP 403 {@code INSUFFICIENT_SCOPE}: the API key does not have the scope this route needs. A
 * payments key never works on the webhook endpoint methods or on {@code listTransactions}; use a
 * reporting key there.
 */
public final class InsufficientScopeException extends PermissionException {

    private static final long serialVersionUID = 1L;

    public InsufficientScopeException(String message) {
        super(ErrorCodes.INSUFFICIENT_SCOPE, message);
    }
}
