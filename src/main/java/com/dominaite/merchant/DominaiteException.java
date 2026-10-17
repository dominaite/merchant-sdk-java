package com.dominaite.merchant;

/**
 * The base of every error this SDK throws. It is unchecked: catch it, or one of its subclasses,
 * where you can act on it.
 */
public class DominaiteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final Integer httpStatus;
    private String idempotencyKey;
    private String requestId;

    protected DominaiteException(String message, String code, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /** The machine-readable error code from the API, or null when there is none. */
    public String getCode() {
        return code;
    }

    /** The HTTP status, or null when no response arrived. */
    public Integer getHttpStatus() {
        return httpStatus;
    }

    /**
     * The idempotency key of the call that failed, or null for calls that take none. Retry with
     * this same key, so a retry replays the first request instead of making a second one.
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The request id of the response: the {@code Dominaite-Request-Id} header when the response
     * has one, else {@code metadata.requestId} in the body, else null (also when no response
     * arrived). Give it to Dominaite support when you ask about one call.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * True when the same call, with the same idempotency key, can succeed later. A transport
     * failure and {@code PAYMENT_PROCESSING_UNAVAILABLE} are retryable.
     */
    public boolean isRetryable() {
        return ErrorCodes.PAYMENT_PROCESSING_UNAVAILABLE.equals(code);
    }

    void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    void setRequestId(String requestId) {
        if (this.requestId == null) {
            this.requestId = requestId;
        }
    }
}
