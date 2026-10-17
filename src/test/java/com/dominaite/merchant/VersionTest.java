package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The User-Agent version, the pom version and the CHANGELOG move together. */
class VersionTest {

    @Test
    void theUserAgentVersionIsThePomVersion() throws Exception {
        String pom = new String(Files.readAllBytes(Paths.get("pom.xml")), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<artifactId>merchant-sdk</artifactId>\\s*<version>([^<]+)</version>").matcher(pom);
        assertTrue(m.find(), "no project version in pom.xml");
        assertEquals(m.group(1), DominaiteClient.VERSION);
    }

    @Test
    void theChangelogStartsWithThisVersion() throws Exception {
        String changelog = new String(Files.readAllBytes(Paths.get("CHANGELOG.md")), StandardCharsets.UTF_8);
        // An "(unreleased)" section may sit above the released version.
        Matcher m = Pattern.compile("(?m)^## (\\S+)(?! \\(unreleased\\))$").matcher(changelog);
        assertTrue(m.find(), "no version heading in CHANGELOG.md");
        assertEquals(DominaiteClient.VERSION, m.group(1));
    }
}
