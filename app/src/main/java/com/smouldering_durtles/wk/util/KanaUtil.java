package com.smouldering_durtles.wk.util;

/**
 * Helper class for converting Kana.
 */
public class KanaUtil {
    private KanaUtil() {
    }

    /**
     * Converts all Katakana characters in the provided String to their respective Hiragana versions.
     * Non-Katakana characters remain the same.
     *
     * @param text the text whose Katakana should be converted
     * @return the new text where all Katakana characters are replaced with Hiragana
     */
    public static String convertKatakanaToHiragana(String text) {
        StringBuilder stringBuilder = new StringBuilder();

        for (char c : text.toCharArray()) {
            stringBuilder.append(toHiragana(c));
        }

        return stringBuilder.toString();
    }

    /**
     * Converts the provided character into its Hiragana representation if it's a Katakana character.
     * Otherwise returns the original character.
     */
    private static char toHiragana(char c) {
        if (isFullWidthKatakana(c)) {
            return (char) (c - 0x60);
        } else if (isHalfWidthKatakana(c)) {
            return (char) (c - 0xcf25);
        }

        return c;
    }

    /**
     * Whether this character is a full width Katakana character.
     */
    private static boolean isFullWidthKatakana(char c) {
        return (('\u30a1' <= c) && (c <= '\u30fe'));
    }

    /**
     * Whether this character is a half width Katakana character.
     */
    private static boolean isHalfWidthKatakana(char c) {
        return (('\uff66' <= c) && (c <= '\uff9d'));
    }
}
