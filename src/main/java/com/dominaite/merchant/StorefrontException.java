package com.dominaite.merchant;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The storefront configuration refused the checkout session: {@code STOREFRONT_MISMATCH},
 * {@code STOREFRONT_INACTIVE} or {@code STOREFRONT_NOT_WHITELISTED}. This is configuration, not
 * input: a retry does not help.
 */
public final class StorefrontException extends ApiException {

    private static final long serialVersionUID = 1L;

    private final String transactionId;

    public StorefrontException(int httpStatus, String code, String message, String transactionId, JsonNode rawResult) {
        super(httpStatus, code, message, rawResult);
        this.transactionId = transactionId;
    }

    /** The transaction the refusal names, or null. */
    public String getTransactionId() {
        return transactionId;
    }
}
