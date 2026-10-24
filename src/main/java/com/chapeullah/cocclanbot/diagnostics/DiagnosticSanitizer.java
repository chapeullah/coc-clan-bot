package com.chapeullah.cocclanbot.diagnostics;

import com.chapeullah.cocclanbot.telegram.TelegramUtils;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class DiagnosticSanitizer {
    private static final Set<String> SECRETS = ConcurrentHashMap.newKeySet();
    private static final String ESCAPED_LINE_BREAK = "\\\\u(?:000[ad]|0085|202[89])";
    private static final Pattern BEARER = Pattern.compile("(?i)(\\bBearer\\h++)"
            + "(?!<redacted>(?=$|[\\s\"',;}\\]]|" + ESCAPED_LINE_BREAK + "))[^\\s\"',;}\\]]+");
    private static final Pattern SECRET_FIELD = Pattern.compile(
            "(?i)(\"(?:authorization|token|api[_-]?token|access[_-]?token|password|secret)\"\\s*:\\s*\")(?:\\\\.|[^\"\\\\])*+(?:\\\\)?(\"|$)");
    private static final Pattern SINGLE_QUOTED_SECRET_FIELD = Pattern.compile(
            "(?i)('(?:authorization|token|api[_-]?token|access[_-]?token|password|secret)'\\s*:\\s*')(?:\\\\.|[^'\\\\])*+(?:\\\\)?('|$)");
    private static final String REDACTED_AUTHORIZATION =
            "<redacted>(?=$|[\\r\\n]|[\"']\\h*[,;}\\]]|" + ESCAPED_LINE_BREAK + ")";
    private static final Pattern QUOTED_AUTHORIZATION = Pattern.compile(
            "(?i)([\"'](?:proxy-)?authorization[\"']\\h*[:=]\\h*+)"
                    + "(?!(?:" + REDACTED_AUTHORIZATION
                    + "|(?:\"<redacted>\"|'<redacted>')(?=\\h*(?:$|[\\r\\n,;}\\]]))))[^\\r\\n]*");
    private static final Pattern AUTHORIZATION = Pattern.compile(
            "(?i)(\\b(?:proxy-)?authorization\\h*[:=]\\h*+)(?!" + REDACTED_AUTHORIZATION + ")[^\\r\\n]*");
    private static final Pattern ASSIGNED_SECRET = Pattern.compile(
            "(?i)(\\b(?:token|api[_-]?token|access[_-]?token|password|secret)\\h*=\\h*+)"
                    + "(?!<redacted>(?=$|[\\s\"'&,;\\]}]|" + ESCAPED_LINE_BREAK + "))[^\\s&,;\\]}]+");

    private DiagnosticSanitizer() {}

    public static void registerSecret(String secret) {
        if (secret != null && !secret.isBlank()) {
            SECRETS.add(secret);
        }
    }

    public static String mask(String text) {
        if (text == null) {
            return "none";
        }
        String masked = TelegramUtils.maskBotToken(text);
        for (String secret : SECRETS.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
            masked = masked.replace(secret, "<redacted>");
            // A converter or upstream error may already have truncated a token.
            int from = 0;
            while (secret.length() > 8 && (from = masked.indexOf(secret.substring(0, 8), from)) >= 0) {
                int length = 8;
                while (length < secret.length() && from + length < masked.length()
                        && masked.charAt(from + length) == secret.charAt(length)) {
                    length++;
                }
                if (from + length < masked.length()) {
                    char next = masked.charAt(from + length);
                    if (Character.isLetterOrDigit(next) || next == '-' || next == '_' || next == ':') {
                        from += length;
                        continue;
                    }
                }
                masked = masked.substring(0, from) + "<redacted>" + masked.substring(from + length);
                from += "<redacted>".length();
            }
        }
        masked = BEARER.matcher(masked).replaceAll("$1<redacted>");
        masked = SECRET_FIELD.matcher(masked).replaceAll("$1<redacted>$2");
        masked = SINGLE_QUOTED_SECRET_FIELD.matcher(masked).replaceAll("$1<redacted>$2");
        masked = QUOTED_AUTHORIZATION.matcher(masked).replaceAll("$1<redacted>");
        masked = AUTHORIZATION.matcher(masked).replaceAll("$1<redacted>");
        return ASSIGNED_SECRET.matcher(masked).replaceAll("$1<redacted>");
    }

    public static String singleLine(String text, int limit) {
        String masked = mask(text);
        masked = escapeControls(masked).replace('"', '\'');
        return masked.length() <= limit ? masked : masked.substring(0, limit) + "<truncated>";
    }

    public static String escapeControls(String text) {
        StringBuilder result = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> {
            if (Character.isISOControl(codePoint) || Character.getType(codePoint) == Character.FORMAT
                    || codePoint == 0x2028 || codePoint == 0x2029) {
                result.append(String.format("\\u%04x", codePoint));
            } else {
                result.appendCodePoint(codePoint);
            }
        });
        return result.toString();
    }

    public static String uri(URI uri) {
        String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
        return singleLine(uri.getScheme() + "://" + uri.getHost() + port + uri.getRawPath(), 512);
    }

    public static String bodySnippet(byte[] bytes, int limit) {
        String prefix = new String(bytes, 0, Math.min(bytes.length, limit), StandardCharsets.UTF_8);
        if (bytes.length > limit) {
            for (String secret : SECRETS) {
                for (int length = Math.min(secret.length(), prefix.length()); length > 0; length--) {
                    if (prefix.endsWith(secret.substring(0, length))) {
                        prefix = prefix.substring(0, prefix.length() - length) + "<redacted>";
                        break;
                    }
                }
            }
        }
        String snippet = singleLine(prefix, limit);
        return bytes.length > limit && !snippet.endsWith("<truncated>") ? snippet + "<truncated>" : snippet;
    }
}
