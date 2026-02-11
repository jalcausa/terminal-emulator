package terminal;

import java.util.Objects;

/**
 * Represents a terminal color, which can be either the terminal's default color
 * or one of the 16 standard ANSI colors.
 * <p>
 * This class is immutable. Use the factory methods {@link #defaultColor()} and
 * {@link #of(AnsiColor)} to create instances.
 */
public final class TerminalColor {

    private static final TerminalColor DEFAULT = new TerminalColor(null);

    private final AnsiColor color;

    private TerminalColor(AnsiColor color) {
        this.color = color;
    }

    /**
     * Returns the terminal's default color.
     */
    public static TerminalColor defaultColor() {
        return DEFAULT;
    }

    /**
     * Returns a terminal color for the given ANSI color.
     *
     * @param color the ANSI color; must not be null
     * @return a TerminalColor wrapping the given ANSI color
     * @throws NullPointerException if color is null
     */
    public static TerminalColor of(AnsiColor color) {
        Objects.requireNonNull(color, "color must not be null");
        return new TerminalColor(color);
    }

    /**
     * Returns whether this represents the terminal's default color.
     */
    public boolean isDefault() {
        return color == null;
    }

    /**
     * Returns the ANSI color, or {@code null} if this is the default color.
     */
    public AnsiColor getAnsiColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalColor that)) return false;
        return color == that.color;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(color);
    }

    @Override
    public String toString() {
        return color == null ? "TerminalColor[default]" : "TerminalColor[" + color + "]";
    }
}
