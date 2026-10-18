package com.chapeullah.guccigoblin.telegram;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TelegramUtils {

    private static final Pattern GRAPHEME_PATTERN = Pattern.compile("\\X");

    private TelegramUtils() {}

    public static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static String replaceEmojisWithSquares(String text) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = GRAPHEME_PATTERN.matcher(text);
        while (matcher.find()) {
            String symbol = matcher.group();
            boolean emoji = symbol.codePoints().anyMatch(codePoint ->
                    Character.isEmojiPresentation(codePoint)
                            || Character.isExtendedPictographic(codePoint)
                            || codePoint == 0x20E3);
            result.append(emoji ? "□" : symbol);
        }
        return result.toString();
    }

}
