package com.dominaite.merchant;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The API answered, and the answer is a refusal. {@link #getRawResult()} has the whole response
 * envelope when there was one.
 */
public class ApiException extends DominaiteException {

    private static final long serialVersionUID = 1L;

    private final transient JsonNode rawResult;

    public ApiException(int httpStatus, String code, String message, JsonNode rawResult) {
        super(message, code, httpStatus, null);
        this.rawResult = rawResult;
    }

    /** The response envelope as JSON, or null when the response had no JSON body. */
    public JsonNode getRawResult() {
        return rawResult;
    }
}
