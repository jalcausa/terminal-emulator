package terminal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    // ========================================================================
    // Construction
    // ========================================================================

    @Nested
    class Construction {

        @Test
        void createsBufferWithCorrectDimensions() {
            TerminalBuffer buf = new TerminalBuffer(80, 24, 1000);
            assertEquals(80, buf.getWidth());
            assertEquals(24, buf.getHeight());
            assertEquals(1000, buf.getMaxScrollbackSize());
        }

        @Test
        void initialCursorIsAtOrigin() {
            TerminalBuffer buf = new TerminalBuffer(80, 24, 100);
            assertEquals(new CursorPosition(0, 0), buf.getCursorPosition());
        }

        @Test
        void initialScreenIsEmpty() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            for (int row = 0; row < 3; row++) {
                assertEquals("", buf.getLine(row));
            }
        }

        @Test
        void initialScrollbackIsEmpty() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 100);
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test
        void rejectsInvalidWidth() {
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(0, 24, 100));
        }

        @Test
        void rejectsInvalidHeight() {
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(80, 0, 100));
        }

        @Test
        void rejectsNegativeScrollback() {
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(80, 24, -1));
        }

        @Test
        void allowsZeroScrollback() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            assertEquals(0, buf.getMaxScrollbackSize());
        }

        @Test
        void minimumSizeBuffer() {
            TerminalBuffer buf = new TerminalBuffer(1, 1, 0);
            assertEquals(1, buf.getWidth());
            assertEquals(1, buf.getHeight());
        }
    }

    // ========================================================================
    // Cursor movement
    // ========================================================================

    @Nested
    class CursorMovement {

        @Test
        void setCursorPositionWithinBounds() {
            TerminalBuffer buf = new TerminalBuffer(80, 24, 0);
            buf.setCursorPosition(10, 5);
            assertEquals(new CursorPosition(10, 5), buf.getCursorPosition());
        }

        @Test
        void setCursorClampsColumnTooHigh() {
            TerminalBuffer buf = new TerminalBuffer(80, 24, 0);
            buf.setCursorPosition(100, 5);
            assertEquals(new CursorPosition(79, 5), buf.getCursorPosition());
        }

        @Test
        void setCursorClampsRowTooHigh() {
            TerminalBuffer buf = new TerminalBuffer(80, 24, 0);
            buf.setCursorPosition(10, 30);
            assertEquals(new CursorPosition(10, 23), buf.getCursorPosition());
        }

        @Test
        void setCursorClampsNegativeValues() {
            TerminalBuffer buf = new TerminalBuffer(80, 24, 0);
            buf.setCursorPosition(-5, -3);
            assertEquals(new CursorPosition(0, 0), buf.getCursorPosition());
        }

        @Test
        void moveCursorDown() {
            TerminalBuffer buf = new TerminalBuffer(10, 10, 0);
            buf.moveCursorDown(3);
            assertEquals(new CursorPosition(0, 3), buf.getCursorPosition());
        }

        @Test
        void moveCursorDownStopsAtBottom() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.moveCursorDown(100);
            assertEquals(new CursorPosition(0, 4), buf.getCursorPosition());
        }

        @Test
        void moveCursorUp() {
            TerminalBuffer buf = new TerminalBuffer(10, 10, 0);
            buf.setCursorPosition(0, 5);
            buf.moveCursorUp(2);
            assertEquals(new CursorPosition(0, 3), buf.getCursorPosition());
        }

        @Test
        void moveCursorUpStopsAtTop() {
            TerminalBuffer buf = new TerminalBuffer(10, 10, 0);
            buf.setCursorPosition(0, 2);
            buf.moveCursorUp(10);
            assertEquals(new CursorPosition(0, 0), buf.getCursorPosition());
        }

        @Test
        void moveCursorRight() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.moveCursorRight(4);
            assertEquals(new CursorPosition(4, 0), buf.getCursorPosition());
        }

        @Test
        void moveCursorRightStopsAtEdge() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.moveCursorRight(100);
            assertEquals(new CursorPosition(9, 0), buf.getCursorPosition());
        }

        @Test
        void moveCursorLeft() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setCursorPosition(5, 0);
            buf.moveCursorLeft(3);
            assertEquals(new CursorPosition(2, 0), buf.getCursorPosition());
        }

        @Test
        void moveCursorLeftStopsAtEdge() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setCursorPosition(2, 0);
            buf.moveCursorLeft(10);
            assertEquals(new CursorPosition(0, 0), buf.getCursorPosition());
        }

        @Test
        void moveCursorWithZeroDoesNothing() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setCursorPosition(3, 3);
            buf.moveCursorUp(0);
            buf.moveCursorDown(0);
            buf.moveCursorLeft(0);
            buf.moveCursorRight(0);
            assertEquals(new CursorPosition(3, 3), buf.getCursorPosition());
        }

        @Test
        void moveCursorWithNegativeDoesNothing() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setCursorPosition(3, 3);
            buf.moveCursorUp(-1);
            buf.moveCursorDown(-1);
            buf.moveCursorLeft(-1);
            buf.moveCursorRight(-1);
            assertEquals(new CursorPosition(3, 3), buf.getCursorPosition());
        }
    }

    // ========================================================================
    // Attribute management
    // ========================================================================

    @Nested
    class AttributeManagement {

        @Test
        void initialAttributesAreDefault() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            assertEquals(TextAttributes.DEFAULT, buf.getCurrentAttributes());
        }

        @Test
        void setCurrentAttributes() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes attrs = TextAttributes.DEFAULT
                    .withForeground(TerminalColor.of(AnsiColor.RED))
                    .withStyle(StyleFlag.BOLD);
            buf.setCurrentAttributes(attrs);
            assertEquals(attrs, buf.getCurrentAttributes());
        }

        @Test
        void setForeground() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setForeground(TerminalColor.of(AnsiColor.GREEN));

            assertEquals(TerminalColor.of(AnsiColor.GREEN),
                    buf.getCurrentAttributes().getForeground());
            assertTrue(buf.getCurrentAttributes().getBackground().isDefault());
        }

        @Test
        void setBackground() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setBackground(TerminalColor.of(AnsiColor.BLUE));

            assertTrue(buf.getCurrentAttributes().getForeground().isDefault());
            assertEquals(TerminalColor.of(AnsiColor.BLUE),
                    buf.getCurrentAttributes().getBackground());
        }

        @Test
        void addAndRemoveStyle() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.addStyle(StyleFlag.BOLD);
            buf.addStyle(StyleFlag.ITALIC);

            assertTrue(buf.getCurrentAttributes().hasStyle(StyleFlag.BOLD));
            assertTrue(buf.getCurrentAttributes().hasStyle(StyleFlag.ITALIC));

            buf.removeStyle(StyleFlag.BOLD);
            assertFalse(buf.getCurrentAttributes().hasStyle(StyleFlag.BOLD));
            assertTrue(buf.getCurrentAttributes().hasStyle(StyleFlag.ITALIC));
        }

        @Test
        void resetAttributes() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setForeground(TerminalColor.of(AnsiColor.RED));
            buf.addStyle(StyleFlag.BOLD);

            buf.resetAttributes();

            assertEquals(TextAttributes.DEFAULT, buf.getCurrentAttributes());
        }

        @Test
        void attributesAreUsedWhenWriting() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes attrs = TextAttributes.DEFAULT
                    .withForeground(TerminalColor.of(AnsiColor.YELLOW))
                    .withStyle(StyleFlag.UNDERLINE);
            buf.setCurrentAttributes(attrs);
            buf.writeText("A");

            assertEquals(attrs, buf.getAttributesAt(0, 0));
        }

        @Test
        void changingAttributesMidWrite() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setForeground(TerminalColor.of(AnsiColor.RED));
            buf.writeText("A");

            buf.setForeground(TerminalColor.of(AnsiColor.BLUE));
            buf.writeText("B");

            assertEquals(TerminalColor.of(AnsiColor.RED),
                    buf.getAttributesAt(0, 0).getForeground());
            assertEquals(TerminalColor.of(AnsiColor.BLUE),
                    buf.getAttributesAt(1, 0).getForeground());
        }
    }

    // ========================================================================
    // writeText
    // ========================================================================

    @Nested
    class WriteText {

        @Test
        void writeSimpleText() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("Hello");

            assertEquals("Hello", buf.getLine(0));
            assertEquals(new CursorPosition(5, 0), buf.getCursorPosition());
        }

        @Test
        void writeEmptyStringDoesNothing() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("");
            assertEquals("", buf.getLine(0));
            assertEquals(new CursorPosition(0, 0), buf.getCursorPosition());
        }

        @Test
        void writeOverridesExistingContent() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("AAAA");
            buf.setCursorPosition(1, 0);
            buf.writeText("BB");

            assertEquals("ABBA", buf.getLine(0));
        }

        @Test
        void writeExactlyFillsLine() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("12345");

            assertEquals("12345", buf.getLine(0));
            // Cursor at column 5 (past the end, will wrap on next write)
            assertEquals(new CursorPosition(5, 0), buf.getCursorPosition());
        }

        @Test
        void writeWrapsToNextLine() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("12345AB");

            assertEquals("12345", buf.getLine(0));
            assertEquals("AB", buf.getLine(1));
            assertEquals(new CursorPosition(2, 1), buf.getCursorPosition());
        }

        @Test
        void writeWrapsMultipleLines() {
            TerminalBuffer buf = new TerminalBuffer(3, 5, 0);
            buf.writeText("ABCDEFGHI");

            assertEquals("ABC", buf.getLine(0));
            assertEquals("DEF", buf.getLine(1));
            assertEquals("GHI", buf.getLine(2));
        }

        @Test
        void writeScrollsWhenReachingBottom() {
            TerminalBuffer buf = new TerminalBuffer(5, 2, 10);
            buf.writeText("AAAAABBBBBCCC");

            // Screen should show last 2 lines
            assertEquals("BBBBB", buf.getLine(0));
            assertEquals("CCC", buf.getLine(1));
            // First line should be in scrollback
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("AAAAA", buf.getScrollbackLine(0));
        }

        @Test
        void writeScrollsFillsScrollback() {
            TerminalBuffer buf = new TerminalBuffer(3, 2, 100);
            // Write enough to scroll multiple times
            buf.writeText("AAABBBCCCDDD");

            assertEquals("CCC", buf.getLine(0));
            assertEquals("DDD", buf.getLine(1));
            assertEquals(2, buf.getScrollbackSize());
            assertEquals("AAA", buf.getScrollbackLine(0));
            assertEquals("BBB", buf.getScrollbackLine(1));
        }

        @Test
        void scrollbackTrimmedWhenOverMax() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 2);
            // Each 3 chars fills a line, then scrolls
            buf.writeText("AAABBBCCCDDD");

            assertEquals("DDD", buf.getLine(0));
            assertEquals(2, buf.getScrollbackSize());
            // Oldest ("AAA") should have been discarded
            assertEquals("BBB", buf.getScrollbackLine(0));
            assertEquals("CCC", buf.getScrollbackLine(1));
        }

        @Test
        void writeWithZeroScrollbackDiscardsHistory() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 0);
            buf.writeText("AAABBB");

            assertEquals("BBB", buf.getLine(0));
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test
        void writeSingleCharacter() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("X");

            assertEquals('X', buf.getCharAt(0, 0));
            assertEquals(new CursorPosition(1, 0), buf.getCursorPosition());
        }

        @Test
        void writeAtSpecificCursorPosition() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setCursorPosition(3, 2);
            buf.writeText("Hi");

            assertEquals("Hi", buf.getLine(2).trim());
            assertEquals('H', buf.getCharAt(3, 2));
            assertEquals('i', buf.getCharAt(4, 2));
        }

        @Test
        void writePreservesAttributesPerCell() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes bold = TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD);
            TextAttributes italic = TextAttributes.DEFAULT.withStyle(StyleFlag.ITALIC);

            buf.setCurrentAttributes(bold);
            buf.writeText("A");
            buf.setCurrentAttributes(italic);
            buf.writeText("B");

            assertTrue(buf.getAttributesAt(0, 0).hasStyle(StyleFlag.BOLD));
            assertFalse(buf.getAttributesAt(0, 0).hasStyle(StyleFlag.ITALIC));
            assertFalse(buf.getAttributesAt(1, 0).hasStyle(StyleFlag.BOLD));
            assertTrue(buf.getAttributesAt(1, 0).hasStyle(StyleFlag.ITALIC));
        }
    }
}
