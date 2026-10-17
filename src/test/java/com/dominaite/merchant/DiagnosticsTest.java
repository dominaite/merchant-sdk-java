package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The warning hook, the Dominaite-Sdk-Notice header and the debug log. */
class DiagnosticsTest {

    private static final String NOTICE = "deprecated; current=\"0.7.0\"; sunset=\"2027-01-31\"; info=\"https://docs.example/sdk\"";
    private static final String PING = "{\"merchantId\":\"m\",\"clockSkewSeconds\":0}";

    private final List<String> warnings = Collections.synchronizedList(new ArrayList<>());
    private final List<String> debug = Collections.synchronizedList(new ArrayList<>());

    private static String data(String json) {
        return "{\"success\":true,\"data\":" + json + ",\"metadata\":{\"requestId\":\"req-ok\"}}";
    }

    private DominaiteClient client(MockServer server, boolean withDebug) {
        DominaiteClient.Builder b = DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
            .baseUrl(server.baseUrl()).onWarning(warnings::add);
        return (withDebug ? b.onDebug(debug::add) : b).build();
    }

    @Test
    void oneWarningPerDistinctNoticeValueOnSuccessAndErrorResponses() throws Exception {
        try (MockServer server = new MockServer()) {
            for (int i = 0; i < 3; i++) {
                server.respond(200, data(PING), Map.of(Diagnostics.NOTICE_HEADER, NOTICE));
            }
            server.respond(200, data(PING), Map.of(Diagnostics.NOTICE_HEADER, "deprecated; current=\"0.8.0\""));
            server.respond(400, "{\"success\":false,\"error\":{\"code\":\"VALIDATION_ERROR\",\"message\":\"no\"}}",
                Map.of(Diagnostics.NOTICE_HEADER, "retired"));
            DominaiteClient client = client(server, false);
            for (int i = 0; i < 4; i++) {
                client.ping();
            }
            assertThrows(DominaiteException.class, client::ping);

            assertEquals(Arrays.asList(
                "dominaite-java " + DominaiteClient.VERSION
                    + " is deprecated; upgrade to 0.7.0 (sunset 2027-01-31). https://docs.example/sdk",
                "dominaite-java " + DominaiteClient.VERSION + " is deprecated; upgrade to 0.8.0.",
                "dominaite-java " + DominaiteClient.VERSION + ": retired"), warnings);
        }
    }

    @Test
    void noHeaderNoWarning() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, PING).respond(200, data(PING), Map.of(Diagnostics.NOTICE_HEADER, "  "));
            DominaiteClient client = client(server, false);
            client.ping();
            client.ping();
            assertEquals(Collections.emptyList(), warnings);
        }
    }

    @Test
    void debugIsOffByDefaultAndNeverGoesToTheWarningHook() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(200, data(PING), Map.of(Diagnostics.REQUEST_ID_HEADER, "rid-1"));
            client(server, false).ping();
            assertEquals(Collections.emptyList(), warnings);
            assertEquals(Collections.emptyList(), debug);
        }
    }

    @Test
    void theDebugLogHasOneLinePerRequest() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(201, data(ClientWireTest.SESSION), Map.of(Diagnostics.REQUEST_ID_HEADER, "rid-42"))
                .respondData(200, PING);
            DominaiteClient client = client(server, true);
            client.createCheckoutSession(ClientWireTest.session().build());
            client.ping();

            assertEquals(2, debug.size(), debug.toString());
            assertTrue(debug.get(0).matches("^dominaite-java: POST /merchant-api/checkout/sessions 201 \\d+ms"
                + " request_id=rid-42 idempotency_key=checkout-order-1042-8440-EUR$"), debug.get(0));
            assertTrue(debug.get(1).matches("^dominaite-java: GET /merchant-api/ping 200 \\d+ms$"), debug.get(1));
        }
    }

    @Test
    void aTransportFailureIsADebugLineWithError() {
        DominaiteClient client = DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
            .baseUrl("http://127.0.0.1:1").timeout(Duration.ofSeconds(2)).onDebug(debug::add).build();
        assertThrows(TransportException.class, client::ping);
        assertEquals(1, debug.size());
        assertTrue(debug.get(0).matches("^dominaite-java: GET /merchant-api/ping error \\d+ms$"), debug.get(0));
    }

    @Test
    void noLineCarriesTheSecretTheSignatureTheCashierTokenOrABody() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(201, data(ClientWireTest.SESSION), Map.of(Diagnostics.NOTICE_HEADER, NOTICE))
                .respond(422, "{\"success\":false,\"error\":{\"code\":\"VALIDATION_ERROR\",\"message\":\"body-fragment-x\"}}",
                    Map.of(Diagnostics.NOTICE_HEADER, "deprecated; current=\"0.9.0\""));
            DominaiteClient client = client(server, true);
            client.createCheckoutSession(ClientWireTest.session().build());
            assertThrows(DominaiteException.class, () -> client.createCheckoutSession(ClientWireTest.session().build()));

            List<String> lines = new ArrayList<>(warnings);
            lines.addAll(debug);
            assertEquals(4, lines.size(), lines.toString());
            List<String> forbidden = new ArrayList<>(Arrays.asList(MockServer.SECRET, "ctok_5e4f3a2b1c0d9e8f",
                "body-fragment-x", "order-1042\"", "8440,", "{"));
            for (MockServer.Recorded request : server.requests()) {
                forbidden.add(request.header("X-Signature"));
            }
            for (String line : lines) {
                for (String secret : forbidden) {
                    assertFalse(line.contains(secret), line + " contains " + secret);
                }
            }
        }
    }

    @Test
    void aThrowingHookDoesNotFailTheRequest() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(200, data("{\"merchantId\":\"m\",\"clockSkewSeconds\":90}"),
                Map.of(Diagnostics.NOTICE_HEADER, NOTICE));
            DominaiteClient client = DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
                .baseUrl(server.baseUrl())
                .onWarning(m -> { throw new IllegalStateException("hook"); })
                .onDebug(m -> { throw new IllegalStateException("hook"); })
                .build();
            assertEquals(90, client.ping().getClockSkewSeconds());
        }
    }

    @Test
    void aClockSkewOver60SecondsWarnsOnce() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"merchantId\":\"m\",\"clockSkewSeconds\":60}")
                .respondData(200, "{\"merchantId\":\"m\",\"clockSkewSeconds\":-60}")
                .respondData(200, "{\"merchantId\":\"m\",\"clockSkewSeconds\":61}");
            DominaiteClient client = client(server, false);
            client.ping();
            client.ping();
            assertEquals(Collections.emptyList(), warnings);
            client.ping();
            assertEquals(Collections.singletonList(
                "dominaite-java: clock skew is 61 s; requests fail at 300 s. Sync this server's clock."), warnings);
        }
    }

    @Test
    void aRetryAfterAFailureWarns() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respond(503, "unavailable").respondData(201, ClientWireTest.SESSION);
            client(server, false).createCheckoutSessionWithRetry(ClientWireTest.session().build(), 3, Duration.ofMillis(1));
            assertEquals(Collections.singletonList(
                "dominaite-java: retrying createCheckoutSessionWithRetry after HTTP 503 (attempt 2 of 3)"), warnings);
        }
    }
}
