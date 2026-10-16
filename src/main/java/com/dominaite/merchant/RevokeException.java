package com.dominaite.merchant;

import com.fasterxml.jackson.databind.JsonNode;

/** A stored payment method revoke was refused with a code from {@link ErrorCodes#REVOKE}. */
public final class RevokeException extends ApiException {

    private static final long serialVersionUID = 1L;

    public RevokeException(int httpStatus, String code, String message, JsonNode rawResult) {
        super(httpStatus, code, message, rawResult);
    }
}
