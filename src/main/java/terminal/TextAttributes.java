package terminal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable set of text attributes for a terminal cell: foreground color,
 * background color, and style flags (bold, italic, underline).
 * Use the builder-style methods ({@link #withForeground}, {@link #withBackground},
 * {@link #withStyle}, {@link #withoutStyle}) to derive new instances from existing ones.
 */
public final class TextAttributes {

    /** Default attributes: default foreground and background colors, no styles. */
    public static final TextAttributes DEFAULT = new TextAttributes(
            TerminalColor.defaultColor(),
            TerminalColor.defaultColor(),
            EnumSet.noneOf(StyleFlag.class)
    );

    private final TerminalColor foreground;
    private final TerminalColor background;
    private final Set<StyleFlag> styles;

    /**
     * Creates a new TextAttributes instance.
     *
     * @param foreground foreground color
     * @param background background color
     * @param styles     set of style flags (defensively copied)
     */
    public TextAttributes(TerminalColor foreground, TerminalColor background, Set<StyleFlag> styles) {
        this.foreground = Objects.requireNonNull(foreground, "foreground must not be null");
        this.background = Objects.requireNonNull(background, "background must not be null");
        this.styles = styles.isEmpty()
                ? EnumSet.noneOf(StyleFlag.class)
                : EnumSet.copyOf(styles);
    }

    public TerminalColor getForeground() {
        return foreground;
    }

    public TerminalColor getBackground() {
        return background;
    }

    /**
     * Returns an unmodifiable view of the style flags.
     */
    public Set<StyleFlag> getStyles() {
        return Collections.unmodifiableSet(styles);
    }

    public boolean hasStyle(StyleFlag flag) {
        return styles.contains(flag);
    }

    /** Returns a new TextAttributes with a different foreground color. */
    public TextAttributes withForeground(TerminalColor fg) {
        return new TextAttributes(fg, this.background, this.styles);
    }

    /** Returns a new TextAttributes with a different background color. */
    public TextAttributes withBackground(TerminalColor bg) {
        return new TextAttributes(this.foreground, bg, this.styles);
    }

    /** Returns a new TextAttributes with the given style flag added. */
    public TextAttributes withStyle(StyleFlag flag) {
        EnumSet<StyleFlag> newStyles = EnumSet.copyOf(this.styles);
        newStyles.add(flag);
        return new TextAttributes(this.foreground, this.background, newStyles);
    }

    /** Returns a new TextAttributes with the given style flag removed. */
    public TextAttributes withoutStyle(StyleFlag flag) {
        EnumSet<StyleFlag> newStyles = EnumSet.copyOf(this.styles);
        newStyles.remove(flag);
        return new TextAttributes(this.foreground, this.background, newStyles);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextAttributes that)) return false;
        return Objects.equals(foreground, that.foreground)
                && Objects.equals(background, that.background)
                && Objects.equals(styles, that.styles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foreground, background, styles);
    }

    @Override
    public String toString() {
        return "TextAttributes[fg=" + foreground + ", bg=" + background + ", styles=" + styles + "]";
    }
}
