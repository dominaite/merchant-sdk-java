package com.dominaite.merchant;

import com.fasterxml.jackson.databind.JsonNode;

/** A refund call was refused with a code from {@link ErrorCodes#REFUND}. */
public final class RefundException extends ApiException {

    private static final long serialVersionUID = 1L;

    public RefundException(int httpStatus, String code, String message, JsonNode rawResult) {
        super(httpStatus, code, message, rawResult);
    }

    /** {@code DUPLICATE_REQUEST} and {@code REFUND_NOT_FOUND} can succeed on a later retry. */
    @Override
    public boolean isRetryable() {
        return ErrorCodes.DUPLICATE_REQUEST.equals(getCode()) || ErrorCodes.REFUND_NOT_FOUND.equals(getCode());
    }
}
