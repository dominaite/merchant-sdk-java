package com.dominaite.merchant;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The API answered a checkout session request with {@code success: false}. The code is one of
 * {@link ErrorCodes#SESSION_REFUSALS}. A replay refusal names the transaction the key collided
 * with: reconcile with {@code getStatus} instead of opening a second payment for the order.
 */
public final class RefusalException extends ApiException {

    private static final long serialVersionUID = 1L;

    private final String transactionId;

    public RefusalException(int httpStatus, String code, String message, String transactionId, JsonNode rawResult) {
        super(httpStatus, code, message, rawResult);
        this.transactionId = transactionId;
    }

    /** The transaction the refusal names, or null. */
    public String getTransactionId() {
        return transactionId;
    }
}
