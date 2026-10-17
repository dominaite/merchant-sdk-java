package com.dominaite.merchant;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A local HTTP server for wire tests. It answers queued responses in order and records every
 * request, with the raw path, the raw query, the headers and the body bytes.
 */
final class MockServer implements AutoCloseable {

    static final String KEY_ID = "dmk_0123456789abcdef0123456789abcdef";
    static final String SECRET = "dms_0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final HttpServer server;
    // One thread per exchange, so a slow answer does not hold up the retry behind it.
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Deque<Response> responses = new ArrayDeque<>();
    private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<>());

    MockServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            byte[] body;
            try (InputStream in = exchange.getRequestBody()) {
                body = in.readAllBytes();
            }
            Map<String, String> headers = new LinkedHashMap<>();
            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), values.get(0)));
            requests.add(new Recorded(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(),
                headers,
                new String(body, StandardCharsets.UTF_8)));

            Response response;
            synchronized (responses) {
                response = responses.isEmpty() ? new Response(500, "{}", Map.of(), 0) : responses.poll();
            }
            if (response.delayMillis > 0) {
                try {
                    Thread.sleep(response.delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            response.headers.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
            byte[] out = response.body.getBytes(StandardCharsets.UTF_8);
            if (response.status == 204 || out.length == 0) {
                exchange.sendResponseHeaders(response.status, -1);
            } else {
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(response.status, out.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(out);
                }
            }
            exchange.close();
        });
        server.setExecutor(executor);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/payments";
    }

    /** A client with the default retry count that does not wait between attempts. */
    DominaiteClient client() {
        return builder().build();
    }

    DominaiteClient.Builder builder() {
        return DominaiteClient.builder().keyId(KEY_ID).secret(SECRET).baseUrl(baseUrl()).sleeper(delay -> { });
    }

    MockServer respond(int status, String body) {
        return respond(status, body, Map.of());
    }

    MockServer respond(int status, String body, Map<String, String> headers) {
        synchronized (responses) {
            responses.add(new Response(status, body, headers, 0));
        }
        return this;
    }

    /** Answers only after {@code delayMillis}, so a client with a shorter timeout gives up first. */
    MockServer respondLate(long delayMillis) {
        synchronized (responses) {
            responses.add(new Response(200, "{}", Map.of(), delayMillis));
        }
        return this;
    }

    /** The envelope the gateway wraps successful answers in. */
    MockServer respondData(int status, String dataJson) {
        return respond(status, "{\"success\":true,\"data\":" + dataJson
            + ",\"metadata\":{\"requestId\":\"req-ok\"}}");
    }

    /** The envelope of a refusal, with a request id in the metadata. */
    MockServer respondError(int status, String code, String message) {
        return respond(status, "{\"success\":false,\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message
            + "\"},\"metadata\":{\"requestId\":\"req-123\"}}");
    }

    Recorded last() {
        return requests.get(requests.size() - 1);
    }

    List<Recorded> requests() {
        return requests;
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    static final class Response {
        final int status;
        final String body;
        final Map<String, String> headers;
        final long delayMillis;

        Response(int status, String body, Map<String, String> headers, long delayMillis) {
            this.status = status;
            this.body = body;
            this.headers = headers;
            this.delayMillis = delayMillis;
        }
    }

    static final class Recorded {
        final String method;
        final String path;
        final String query;
        final Map<String, String> headers;
        final String body;

        Recorded(String method, String path, String query, Map<String, String> headers, String body) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.headers = headers;
            this.body = body;
        }

        String header(String name) {
            return headers.get(name.toLowerCase());
        }

        /** The path without the base URL's /payments prefix: what the gateway verifies. */
        String canonicalPath() {
            return path.startsWith("/payments") ? path.substring("/payments".length()) : path;
        }

        /** Recomputes the signature the way the gateway does, from the canonical path only. */
        String expectedSignature() {
            String key = header("Idempotency-Key");
            return RequestSigner.sign(new SignatureInput(
                SECRET, header("X-Timestamp"), method, canonicalPath(), key == null ? "" : key, body));
        }
    }
}
