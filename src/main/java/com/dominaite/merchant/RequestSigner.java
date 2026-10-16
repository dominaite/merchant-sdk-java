package com.dominaite.merchant;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The request signing recipe, public so you can pin it against the offline vectors and debug an
 * {@code INVALID_SIGNATURE} without reading the client's source.
 */
public final class RequestSigner {

    private RequestSigner() {
    }

    /**
     * Builds the {@code X-Signature} value for one request: lowercase hex HMAC-SHA256 over
     * {@code "{timestamp}\n{METHOD}\n{path}\n{idempotencyKey}\n{sha256hex(body)}"}, UTF-8
     * throughout.
     *
     * <p>The idempotency key sits inside the signature, so a captured request cannot be replayed
     * with a different key. The payload is five lines even on a GET, where the key and the body
     * are both empty. The server rejects timestamps more than 5 minutes off, so keep the server
     * clock on NTP.
     *
     * @param input what to sign
     * @return the lowercase hex signature
     */
    public static String sign(SignatureInput input) {
        Objects.requireNonNull(input, "input");
        String payload = input.getTimestamp()
            + "\n" + input.getMethod().toUpperCase(Locale.ROOT)
            + "\n" + input.getPath()
            + "\n" + input.getIdempotencyKey()
            + "\n" + sha256Hex(input.getBody());
        return Hex.encode(hmacSha256(
            input.getSecret().getBytes(StandardCharsets.UTF_8),
            payload.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Lowercase hex SHA-256 of a body. The empty string hashes to the well-known empty digest,
     * which is what a GET signs.
     *
     * @param body the exact body, as a string that is sent as UTF-8
     * @return the lowercase hex digest
     */
    public static String sha256Hex(String body) {
        Objects.requireNonNull(body, "body");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Hex.encode(digest.digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }

    static byte[] hmacSha256(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available in this JVM", e);
        }
    }
}
