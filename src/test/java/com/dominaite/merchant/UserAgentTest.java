package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The User-Agent is dominaite-java/version (java runtime-version), then the caller's suffix. */
class UserAgentTest {

    private static DominaiteClient.Builder builder() {
        return DominaiteClient.builder().keyId(MockServer.KEY_ID).secret(MockServer.SECRET)
            .baseUrl(DominaiteClient.SANDBOX_BASE_URL);
    }

    @Test
    void withoutASuffixItIsTheSdkAndTheRuntime() {
        String ua = builder().build().getUserAgent();
        assertTrue(Pattern.matches("^dominaite-java/" + Pattern.quote(DominaiteClient.VERSION)
            + " \\(java [^ ()]+\\)$", ua), ua);
        assertEquals("dominaite-java/" + DominaiteClient.VERSION + " (java "
            + System.getProperty("java.version") + ")", ua);
        assertEquals(ua, builder().userAgentSuffix("   ").build().getUserAgent());
    }

    @Test
    void theSuffixFollowsAfterOneSpace() {
        String ua = builder().userAgentSuffix("  shop/1.2 ").build().getUserAgent();
        assertTrue(Pattern.matches("^dominaite-java/" + Pattern.quote(DominaiteClient.VERSION)
            + " \\(java [^ ()]+\\) shop/1\\.2$", ua), ua);
    }

    @Test
    void theSentHeaderIsTheUserAgent() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(200, "{\"merchantId\":\"m\",\"clockSkewSeconds\":0}");
            DominaiteClient client = server.client();
            client.ping();
            assertEquals(client.getUserAgent(), server.last().header("User-Agent"));
        }
    }

    @Test
    void aSuffixWithAControlCharacterIsRefused() {
        for (String suffix : Arrays.asList("shop\r\nX-Evil: 1", "shop\n1", "a\u0000b", "a\u007Fb", "a\tb")) {
            assertThrows(ValidationException.class, () -> builder().userAgentSuffix(suffix).build(), suffix);
        }
    }
}
