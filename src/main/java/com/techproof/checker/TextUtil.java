package com.techproof.checker;

public final class TextUtil {
    private TextUtil() {}

    public static boolean isHangulSyllable(char ch) {
        return ch >= 0xAC00 && ch <= 0xD7A3;
    }

    public static boolean hasBatchim(char ch) {
        if (!isHangulSyllable(ch)) return false;
        return (ch - 0xAC00) % 28 != 0;
    }

    public static String context(String text, int start, int end) {
        int left = Math.max(0, start - 30);
        int right = Math.min(text.length(), end + 30);
        return text.substring(left, right);
    }

    public static boolean isKoreanWordChar(char ch) {
        return isHangulSyllable(ch)
                || (ch >= 0x3131 && ch <= 0x318E) // Hangul Compatibility Jamo
                || (ch >= 0x1100 && ch <= 0x11FF) // Hangul Jamo
                || Character.isLetterOrDigit(ch);
    }
}
