package terminal;

/**
 * Utility class for determining the display width of characters in a terminal.
 * In terminals, most characters occupy 1 column, but certain characters
 * (CJK ideographs, fullwidth forms, and some symbols) occupy 2 columns.
 * This implementation covers the most common wide character ranges without
 * relying on external libraries.
 */
public final class CharWidth {

    private CharWidth() {
        // Utility class
    }

    /**
     * Returns the number of terminal columns a character occupies.
     *
     * @param c the character to measure
     * @return 2 for wide (CJK/fullwidth) characters, 1 for all others
     */
    public static int displayWidth(char c) {
        return isWide(c) ? 2 : 1;
    }

    /**
     * Returns true if the character is a "wide" character that occupies
     * 2 columns in a terminal.
     */
    public static boolean isWide(char c) {
        // CJK Radicals Supplement and Kangxi Radicals
        if (c >= 0x2E80 && c <= 0x2FDF) return true;

        // CJK Symbols and Punctuation, Hiragana, Katakana
        if (c >= 0x3000 && c <= 0x30FF) return true;

        // Bopomofo, Hangul Compatibility Jamo, Kanbun, Bopomofo Extended
        if (c >= 0x3100 && c <= 0x31FF) return true;

        // CJK Strokes, Katakana Phonetic Extensions, Enclosed CJK Letters
        if (c >= 0x31F0 && c <= 0x33FF) return true;

        // CJK Compatibility
        if (c >= 0x3400 && c <= 0x4DBF) return true;

        // CJK Unified Ideographs
        if (c >= 0x4E00 && c <= 0x9FFF) return true;

        // Yi Syllables and Yi Radicals
        if (c >= 0xA000 && c <= 0xA4CF) return true;

        // Hangul Syllables
        if (c >= 0xAC00 && c <= 0xD7AF) return true;

        // CJK Compatibility Ideographs
        if (c >= 0xF900 && c <= 0xFAFF) return true;

        // Vertical Forms
        if (c >= 0xFE10 && c <= 0xFE1F) return true;

        // CJK Compatibility Forms
        if (c >= 0xFE30 && c <= 0xFE6F) return true;

        // Fullwidth Forms (fullwidth ASCII variants and halfwidth Katakana excluded)
        if (c >= 0xFF01 && c <= 0xFF60) return true;

        // Fullwidth won sign, fullwidth cent sign, etc.
        if (c >= 0xFFE0 && c <= 0xFFE6) return true;

        return false;
    }
}
