package terminal;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TextAttributesTest {

    @Test
    void defaultAttributesHaveDefaultColors() {
        TextAttributes attrs = TextAttributes.DEFAULT;
        assertTrue(attrs.getForeground().isDefault());
        assertTrue(attrs.getBackground().isDefault());
    }

    @Test
    void defaultAttributesHaveNoStyles() {
        TextAttributes attrs = TextAttributes.DEFAULT;
        assertTrue(attrs.getStyles().isEmpty());
        assertFalse(attrs.hasStyle(StyleFlag.BOLD));
        assertFalse(attrs.hasStyle(StyleFlag.ITALIC));
        assertFalse(attrs.hasStyle(StyleFlag.UNDERLINE));
    }

    @Test
    void constructorSetsFieldsCorrectly() {
        TerminalColor fg = TerminalColor.of(AnsiColor.RED);
        TerminalColor bg = TerminalColor.of(AnsiColor.BLUE);
        Set<StyleFlag> styles = EnumSet.of(StyleFlag.BOLD, StyleFlag.ITALIC);

        TextAttributes attrs = new TextAttributes(fg, bg, styles);

        assertEquals(fg, attrs.getForeground());
        assertEquals(bg, attrs.getBackground());
        assertEquals(styles, attrs.getStyles());
        assertTrue(attrs.hasStyle(StyleFlag.BOLD));
        assertTrue(attrs.hasStyle(StyleFlag.ITALIC));
        assertFalse(attrs.hasStyle(StyleFlag.UNDERLINE));
    }

    @Test
    void constructorDefensivelyCopiesStyles() {
        EnumSet<StyleFlag> styles = EnumSet.of(StyleFlag.BOLD);
        TextAttributes attrs = new TextAttributes(
                TerminalColor.defaultColor(), TerminalColor.defaultColor(), styles);

        // Mutate the original set — should not affect the attributes
        styles.add(StyleFlag.ITALIC);
        assertFalse(attrs.hasStyle(StyleFlag.ITALIC));
    }

    @Test
    void getStylesReturnsUnmodifiableSet() {
        TextAttributes attrs = TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD);
        assertThrows(UnsupportedOperationException.class,
                () -> attrs.getStyles().add(StyleFlag.ITALIC));
    }

    @Test
    void withForegroundReturnsCopyWithNewForeground() {
        TextAttributes original = TextAttributes.DEFAULT;
        TerminalColor red = TerminalColor.of(AnsiColor.RED);
        TextAttributes modified = original.withForeground(red);

        assertEquals(red, modified.getForeground());
        assertTrue(modified.getBackground().isDefault());
        assertTrue(original.getForeground().isDefault()); // original unchanged
    }

    @Test
    void withBackgroundReturnsCopyWithNewBackground() {
        TextAttributes original = TextAttributes.DEFAULT;
        TerminalColor blue = TerminalColor.of(AnsiColor.BLUE);
        TextAttributes modified = original.withBackground(blue);

        assertTrue(modified.getForeground().isDefault());
        assertEquals(blue, modified.getBackground());
    }

    @Test
    void withStyleAddsFlag() {
        TextAttributes attrs = TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD);

        assertTrue(attrs.hasStyle(StyleFlag.BOLD));
        assertFalse(attrs.hasStyle(StyleFlag.ITALIC));
    }

    @Test
    void withStylePreservesExistingFlags() {
        TextAttributes attrs = TextAttributes.DEFAULT
                .withStyle(StyleFlag.BOLD)
                .withStyle(StyleFlag.ITALIC);

        assertTrue(attrs.hasStyle(StyleFlag.BOLD));
        assertTrue(attrs.hasStyle(StyleFlag.ITALIC));
    }

    @Test
    void withoutStyleRemovesFlag() {
        TextAttributes attrs = TextAttributes.DEFAULT
                .withStyle(StyleFlag.BOLD)
                .withStyle(StyleFlag.ITALIC)
                .withoutStyle(StyleFlag.BOLD);

        assertFalse(attrs.hasStyle(StyleFlag.BOLD));
        assertTrue(attrs.hasStyle(StyleFlag.ITALIC));
    }

    @Test
    void withoutStyleOnAbsentFlagDoesNothing() {
        TextAttributes attrs = TextAttributes.DEFAULT.withoutStyle(StyleFlag.BOLD);
        assertFalse(attrs.hasStyle(StyleFlag.BOLD));
        assertEquals(TextAttributes.DEFAULT, attrs);
    }

    @Test
    void equalityWorks() {
        TextAttributes a = new TextAttributes(
                TerminalColor.of(AnsiColor.RED),
                TerminalColor.defaultColor(),
                EnumSet.of(StyleFlag.BOLD));
        TextAttributes b = new TextAttributes(
                TerminalColor.of(AnsiColor.RED),
                TerminalColor.defaultColor(),
                EnumSet.of(StyleFlag.BOLD));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityOnDifferentFields() {
        TextAttributes base = TextAttributes.DEFAULT;
        TextAttributes withFg = base.withForeground(TerminalColor.of(AnsiColor.RED));
        TextAttributes withBg = base.withBackground(TerminalColor.of(AnsiColor.GREEN));
        TextAttributes withStyle = base.withStyle(StyleFlag.BOLD);

        assertNotEquals(base, withFg);
        assertNotEquals(base, withBg);
        assertNotEquals(base, withStyle);
        assertNotEquals(withFg, withBg);
    }

    @Test
    void constructorRejectsNullForeground() {
        assertThrows(NullPointerException.class,
                () -> new TextAttributes(null, TerminalColor.defaultColor(), EnumSet.noneOf(StyleFlag.class)));
    }

    @Test
    void constructorRejectsNullBackground() {
        assertThrows(NullPointerException.class,
                () -> new TextAttributes(TerminalColor.defaultColor(), null, EnumSet.noneOf(StyleFlag.class)));
    }

    @Test
    void chainingBuilderMethodsWorks() {
        TextAttributes attrs = TextAttributes.DEFAULT
                .withForeground(TerminalColor.of(AnsiColor.CYAN))
                .withBackground(TerminalColor.of(AnsiColor.BLACK))
                .withStyle(StyleFlag.BOLD)
                .withStyle(StyleFlag.UNDERLINE);

        assertEquals(TerminalColor.of(AnsiColor.CYAN), attrs.getForeground());
        assertEquals(TerminalColor.of(AnsiColor.BLACK), attrs.getBackground());
        assertTrue(attrs.hasStyle(StyleFlag.BOLD));
        assertTrue(attrs.hasStyle(StyleFlag.UNDERLINE));
        assertFalse(attrs.hasStyle(StyleFlag.ITALIC));
    }
}
