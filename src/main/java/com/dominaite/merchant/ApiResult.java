package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The base of every type a client call returns. It carries the request id of the response that
 * the result was read from.
 */
public abstract class ApiResult {

    @JsonIgnore
    private String requestId;

    ApiResult() {
    }

    /**
     * The request id of the response: the {@code Dominaite-Request-Id} header when the response
     * has one, else {@code metadata.requestId} in the body, else null. Give it to Dominaite
     * support when you ask about one call.
     */
    public String getRequestId() {
        return requestId;
    }

    void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
