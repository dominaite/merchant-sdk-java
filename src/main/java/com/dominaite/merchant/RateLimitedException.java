package com.dominaite.merchant;

import java.time.Duration;

/**
 * HTTP 429: too many requests. Wait at least {@link #getRetryAfter()}, then retry with the same
 * idempotency key. The SDK never retries a 429 by itself.
 */
public final class RateLimitedException extends DominaiteException {

    private static final long serialVersionUID = 1L;

    private final Duration retryAfter;

    public RateLimitedException(String code, String message, Duration retryAfter) {
        super(message, code, 429, null);
        this.retryAfter = retryAfter;
    }

    /** The {@code Retry-After} header as a duration, or null when the response had none. */
    public Duration getRetryAfter() {
        return retryAfter;
    }

    @Override
    public boolean isRetryable() {
        return false;
    }
}
