package com.chapeullah.cocclanbot.diagnostics;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class DiagnosticSanitizerTest {
    @Test
    void repeatedMaskingPreservesFollowingFieldsAndEscapedLines() {
        for (String source : new String[]{"Bearer fictional-private\nfollowing-message",
                "Authorization: Basic fictional-private\r\nfollowing-message",
                "access_token=fictional-private\nfollowing-message",
                "{\"Authorization\":\"Basic fictional-private\",\"next\":\"visible\"}"}) {
            String once = DiagnosticSanitizer.mask(source);
            assertEquals(once, DiagnosticSanitizer.mask(once));
            String escaped = DiagnosticSanitizer.escapeControls(once);
            assertEquals(escaped, DiagnosticSanitizer.mask(escaped));
        }
        String summary = "causeMessage=\"Authorization: <redacted>\", responseDetails=\"visible\"";
        assertEquals(summary, DiagnosticSanitizer.mask(summary));
        String assignedSummary = "causeMessage=\"access_token=<redacted>\", responseDetails=\"visible\"";
        assertEquals(assignedSummary, DiagnosticSanitizer.mask(assignedSummary));
    }

    @Test
    void literalRedactionMarkerDoesNotHideAdditionalAuthorizationCredentialsFromTheSanitizer() {
        for (String value : new String[]{"<redacted> Basic still-private", "\"<redacted> Basic still-private\"",
                "\"<redacted>\" Basic still-private", "\"<redacted>\", Basic still-private", "<redacted>still-private"}) {
            String result = DiagnosticSanitizer.mask("Authorization:   " + value);
            assertFalse(result.contains("still-private"), result);
        }
    }

    @Test
    void longAndTruncatedQuotedValuesCannotOverflowTheRegexStackOrLeak() {
        for (String value : new String[]{"x".repeat(1_000_000), "\\\"".repeat(100_000), "unfinished\\"}) {
            for (String quote : new String[]{"\"", "'"}) {
                String source = quote + "token" + quote + ":" + quote + value;
                String result = assertDoesNotThrow(() -> DiagnosticSanitizer.mask(source));
                assertEquals(quote + "token" + quote + ":" + quote + "<redacted>", result);
            }
        }
    }

    @Test
    void escapedQuotedFieldsAndNonBearerAuthorizationAreMasked() {
        String source = "{\"token\":\"first\\\"remaining-secret\",\"ok\":true}\n"
                + "{'password':'second\\'remaining-password'}\n"
                + "Authorization: Digest username=\"private-user\", response=\"private-digest\"\n"
                + "{\"Authorization\":[\"Basic array-credential\"]}\n"
                + "{'Authorization': [Basic map-credential]}\n"
                + "proxy-authorization=[Basic fictional-basic]\naccess_token=fictional-query&ok=true";
        String result = DiagnosticSanitizer.mask(source);
        for (String secret : new String[]{"first", "remaining-secret", "second", "remaining-password",
                "private-user", "private-digest", "array-credential", "map-credential", "fictional-basic", "fictional-query"}) {
            assertFalse(result.contains(secret), result);
        }
        assertTrue(result.contains("\"ok\":true"));
        assertTrue(result.contains("&ok=true"));
    }

    @Test
    void truncationDoesNotExposeEvenAShortPrefixAtTheBodyBoundary() {
        String secret = "Wxyz1234-completely-fictional-token";
        DiagnosticSanitizer.registerSecret(secret);
        int limit = 32;
        String result = DiagnosticSanitizer.bodySnippet(
                ("a".repeat(limit - 3) + secret).getBytes(StandardCharsets.UTF_8), limit);
        assertFalse(result.contains("Wxy"), result);
        assertTrue(result.endsWith("<truncated>"), result);
        assertTrue(result.length() <= limit + "<truncated>".length(), result);
    }

    @Test
    void truncatedRegisteredTokensAreMaskedWithoutDestroyingUnrelatedCommonPrefixes() {
        String secret = "uniquely-shared-token-secret";
        DiagnosticSanitizer.registerSecret(secret);
        String result = DiagnosticSanitizer.mask("token='" + secret.substring(0, 19)
                + "' context=uniquely-shared-context");
        assertFalse(result.contains(secret.substring(0, 19)), result);
        assertTrue(result.contains("context=uniquely-shared-context"), result);
    }

    @Test
    void diagnosticFieldsEscapeUnicodeAndAsciiControlsBeforeBoundingOutput() {
        String result = DiagnosticSanitizer.singleLine("line\r\n\t\u001b\u0085\u2028\u2029\u202eend", 80);
        assertFalse(result.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                || Character.getType(codePoint) == Character.FORMAT || codePoint == 0x2028 || codePoint == 0x2029));
        assertTrue(result.contains("\\u000a"));
    }
}
