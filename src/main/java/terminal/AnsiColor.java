package terminal;

/**
 * The 16 standard ANSI terminal colors.
 * <p>
 * These correspond to the SGR (Select Graphic Rendition) color codes 30-37 (normal)
 * and 90-97 (bright) for foreground, and 40-47 / 100-107 for background.
 */
public enum AnsiColor {
    BLACK,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    MAGENTA,
    CYAN,
    WHITE,
    BRIGHT_BLACK,
    BRIGHT_RED,
    BRIGHT_GREEN,
    BRIGHT_YELLOW,
    BRIGHT_BLUE,
    BRIGHT_MAGENTA,
    BRIGHT_CYAN,
    BRIGHT_WHITE
}
