package com.dominaite.merchant;

import java.util.Objects;

/**
 * Everything that goes into one request signature.
 *
 * <p>The secret is here only long enough to be hashed. {@link #toString()} redacts it, so it
 * cannot leak through a log line or an exception dump.
 */
public final class SignatureInput {

    private final String secret;
    private final String timestamp;
    private final String method;
    private final String path;
    private final String idempotencyKey;
    private final String body;

    /**
     * @param secret your API secret ({@code dms_...})
     * @param timestamp Unix seconds, exactly as sent in {@code X-Timestamp}
     * @param method the HTTP method; uppercased before signing
     * @param path the canonical path only: no host, no query string, and never the base URL's
     *     own prefix (production's {@code /payments} is not part of the signed path)
     * @param idempotencyKey the {@code Idempotency-Key} header value; empty for GET, DELETE and
     *     PATCH
     * @param body the exact request body that is sent; empty when there is none
     */
    public SignatureInput(
            String secret,
            String timestamp,
            String method,
            String path,
            String idempotencyKey,
            String body) {
        this.secret = Objects.requireNonNull(secret, "secret");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.method = Objects.requireNonNull(method, "method");
        this.path = Objects.requireNonNull(path, "path");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.body = Objects.requireNonNull(body, "body");
    }

    public String getSecret() {
        return secret;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getBody() {
        return body;
    }

    /** A description with the secret redacted. */
    @Override
    public String toString() {
        return "SignatureInput{method=" + method + ", path=" + path + ", secret=[REDACTED]}";
    }
}
