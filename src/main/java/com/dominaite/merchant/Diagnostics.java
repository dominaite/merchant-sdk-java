package com.dominaite.merchant;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The warning and debug lines of one client. A line never carries the secret, the signature, a
 * cashier token, a query string or a request or response body: callers pass only the method,
 * the canonical path, the status, two response headers and the idempotency key.
 */
final class Diagnostics {

    static final String NOTICE_HEADER = "Dominaite-Sdk-Notice";
    static final String REQUEST_ID_HEADER = "Dominaite-Request-Id";

    private static final System.Logger LOGGER = System.getLogger("com.dominaite.merchant");
    private static final String PRODUCT = "dominaite-java";
    private static final int MAX_NOTICES = 32;
    private static final int MAX_NOTICE_LENGTH = 512;
    private static final int MAX_REQUEST_ID_LENGTH = 128;

    private final Consumer<String> onWarning;
    private final Consumer<String> onDebug;
    private final Set<String> seenNotices = new HashSet<>();

    Diagnostics(Consumer<String> onWarning, Consumer<String> onDebug) {
        this.onWarning = onWarning;
        this.onDebug = onDebug;
    }

    /** Sends a warning to the caller's hook, or to the System.Logger at WARNING. Never throws. */
    void warn(String message) {
        try {
            if (onWarning != null) {
                onWarning.accept(message);
            } else {
                LOGGER.log(System.Logger.Level.WARNING, message);
            }
        } catch (RuntimeException ignored) {
            // A log sink must never fail a payment call.
        }
    }

    /** Warns once per distinct notice value. A missing or empty header does nothing. */
    void notice(String header) {
        if (header == null) {
            return;
        }
        String value = clean(header, MAX_NOTICE_LENGTH).trim();
        if (value.isEmpty()) {
            return;
        }
        // The lock covers only the set, never the hook.
        synchronized (seenNotices) {
            if (seenNotices.contains(value) || seenNotices.size() >= MAX_NOTICES) {
                return;
            }
            seenNotices.add(value);
        }
        warn(noticeMessage(value));
    }

    void clockSkew(long seconds) {
        if (Math.abs(seconds) > 60) {
            warn(PRODUCT + ": clock skew is " + seconds + " s; requests fail at 300 s. Sync this server's clock.");
        }
    }

    void retrying(String method, DominaiteException error, int attempt, int attempts) {
        String reason = error.getCode() != null ? error.getCode()
            : error.getHttpStatus() != null ? "HTTP " + error.getHttpStatus()
            : error.getClass().getSimpleName();
        warn(PRODUCT + ": retrying " + method + " after " + reason + " (attempt " + attempt + " of " + attempts + ")");
    }

    boolean debugEnabled() {
        return onDebug != null;
    }

    /** One line per HTTP request. {@code status} is null for a transport failure. */
    void request(String method, String path, Integer status, long elapsedNanos, String requestId, String idempotencyKey) {
        if (onDebug == null) {
            return;
        }
        StringBuilder line = new StringBuilder(PRODUCT).append(": ").append(method).append(' ').append(path)
            .append(' ').append(status == null ? "error" : status.toString())
            .append(' ').append(elapsedNanos / 1_000_000L).append("ms");
        if (requestId != null) {
            String id = clean(requestId, MAX_REQUEST_ID_LENGTH).trim();
            if (!id.isEmpty()) {
                line.append(" request_id=").append(id);
            }
        }
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            line.append(" idempotency_key=").append(idempotencyKey);
        }
        try {
            onDebug.accept(line.toString());
        } catch (RuntimeException ignored) {
            // A log sink must never fail a payment call.
        }
    }

    static String noticeMessage(String value) {
        String[] parts = value.split(";");
        String kind = parts[0].trim();
        if (!kind.toLowerCase(Locale.ROOT).equals("deprecated")) {
            return PRODUCT + " " + DominaiteClient.VERSION + ": " + value;
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String v = part.substring(eq + 1).trim();
            if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
                v = v.substring(1, v.length() - 1);
            }
            params.put(part.substring(0, eq).trim().toLowerCase(Locale.ROOT), v);
        }
        StringBuilder message = new StringBuilder(PRODUCT).append(' ').append(DominaiteClient.VERSION)
            .append(" is deprecated");
        String current = params.get("current");
        if (current != null && !current.isEmpty()) {
            message.append("; upgrade to ").append(current);
        }
        String sunset = params.get("sunset");
        if (sunset != null && !sunset.isEmpty()) {
            message.append(" (sunset ").append(sunset).append(')');
        }
        message.append('.');
        String info = params.get("info");
        if (info != null && !info.isEmpty()) {
            message.append(' ').append(info);
        }
        return message.toString();
    }

    /** Cuts to {@code max} characters, then drops control characters. */
    private static String clean(String value, int max) {
        String cut = value.length() > max ? value.substring(0, max) : value;
        StringBuilder out = new StringBuilder(cut.length());
        for (int i = 0; i < cut.length(); i++) {
            char c = cut.charAt(i);
            if (c >= 0x20 && c != 0x7F) {
                out.append(c);
            }
        }
        return out.toString();
    }
}
