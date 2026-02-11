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

    // ========================================================================
    // insertText
    // ========================================================================

    @Nested
    class InsertText {

        @Test
        void insertOnEmptyLine() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.insertText("Hi");

            assertEquals("Hi", buf.getLine(0));
            assertEquals(new CursorPosition(2, 0), buf.getCursorPosition());
        }

        @Test
        void insertShiftsExistingContent() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("ABCD");
            buf.setCursorPosition(1, 0);
            buf.insertText("XY");

            // Original: ABCD______
            // After insert at col 1: AXYBCD____
            assertEquals("AXYBCD", buf.getLine(0));
        }

        @Test
        void insertTruncatesContentBeyondWidth() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("ABCDE");
            buf.setCursorPosition(0, 0);
            buf.insertText("XY");

            // Original: ABCDE
            // Insert XY at 0: XYABC (DE pushed off)
            assertEquals("XYABC", buf.getLine(0));
        }

        @Test
        void insertEmptyStringDoesNothing() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("Test");
            CursorPosition before = buf.getCursorPosition();
            buf.setCursorPosition(0, 0);
            buf.insertText("");
            assertEquals("Test", buf.getLine(0));
        }

        @Test
        void insertAtEndOfLineActsLikeWrite() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("AB");
            // Cursor is at col 2
            buf.insertText("CD");

            assertEquals("ABCD", buf.getLine(0));
        }

        @Test
        void insertUsesCurrentAttributes() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("AB");
            buf.setCursorPosition(1, 0);

            buf.setCurrentAttributes(TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD));
            buf.insertText("X");

            // X at column 1 should be bold
            assertTrue(buf.getAttributesAt(1, 0).hasStyle(StyleFlag.BOLD));
            // A at column 0 should not be bold
            assertFalse(buf.getAttributesAt(0, 0).hasStyle(StyleFlag.BOLD));
        }

        @Test
        void insertWrapsToNextLineWhenCursorAtEnd() {
            TerminalBuffer buf = new TerminalBuffer(3, 3, 0);
            buf.writeText("ABC");
            // Cursor is at col 3 (past end), next insert wraps
            buf.insertText("XY");

            assertEquals("ABC", buf.getLine(0));
            assertEquals("XY", buf.getLine(1));
        }
    }

    // ========================================================================
    // fillLine
    // ========================================================================

    @Nested
    class FillLine {

        @Test
        void fillLineWithCharacter() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.fillLine('X');

            assertEquals("XXXXX", buf.getLine(0));
        }

        @Test
        void fillLineUsesCurrentAttributes() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            TextAttributes attrs = TextAttributes.DEFAULT
                    .withForeground(TerminalColor.of(AnsiColor.GREEN));
            buf.setCurrentAttributes(attrs);
            buf.fillLine('#');

            for (int col = 0; col < 5; col++) {
                assertEquals('#', buf.getCharAt(col, 0));
                assertEquals(attrs, buf.getAttributesAt(col, 0));
            }
        }

        @Test
        void fillLineOnSpecificRow() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.setCursorPosition(0, 2);
            buf.fillLine('=');

            assertEquals("", buf.getLine(0)); // row 0 untouched
            assertEquals("=====", buf.getLine(2));
        }

        @Test
        void fillLineDoesNotMoveCursor() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.setCursorPosition(2, 1);
            buf.fillLine('Z');

            assertEquals(new CursorPosition(2, 1), buf.getCursorPosition());
        }

        @Test
        void fillLineOverridesExistingContent() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("Hello");
            buf.setCursorPosition(0, 0);
            buf.fillLine(' ');

            assertEquals("", buf.getLine(0)); // all spaces = empty after trim
        }

        @Test
        void fillLineWithSpace() {
            TerminalBuffer buf = new TerminalBuffer(3, 2, 0);
            buf.writeText("ABC");
            buf.setCursorPosition(0, 0);
            buf.fillLine(' ');

            // All spaces, getText trims trailing spaces → ""
            assertEquals("", buf.getLine(0));
        }
    }

    // ========================================================================
    // Screen operations
    // ========================================================================

    @Nested
    class ScreenOperations {

        @Test
        void insertEmptyLineAtBottomScrollsScreen() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
            buf.writeText("AAAAA");
            buf.setCursorPosition(0, 1);
            buf.writeText("BBBBB");
            buf.setCursorPosition(0, 2);
            buf.writeText("CCCCC");

            buf.insertEmptyLineAtBottom();

            assertEquals("BBBBB", buf.getLine(0));
            assertEquals("CCCCC", buf.getLine(1));
            assertEquals("", buf.getLine(2)); // new empty line
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("AAAAA", buf.getScrollbackLine(0));
        }

        @Test
        void insertEmptyLineAtBottomRespectsMaxScrollback() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 1);
            buf.writeText("AAA");
            buf.insertEmptyLineAtBottom(); // AAA → scrollback
            buf.writeText("BBB");
            buf.insertEmptyLineAtBottom(); // BBB → scrollback, AAA discarded

            assertEquals(1, buf.getScrollbackSize());
            assertEquals("BBB", buf.getScrollbackLine(0));
        }

        @Test
        void clearScreenReplacesAllLinesWithEmpty() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
            buf.writeText("Hello");
            buf.setCursorPosition(0, 1);
            buf.writeText("World");

            buf.clearScreen();

            for (int i = 0; i < 3; i++) {
                assertEquals("", buf.getLine(i));
            }
        }

        @Test
        void clearScreenResetsCursor() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setCursorPosition(5, 3);
            buf.clearScreen();
            assertEquals(new CursorPosition(0, 0), buf.getCursorPosition());
        }

        @Test
        void clearScreenPreservesScrollback() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 10);
            buf.writeText("AAABBB"); // scrolls, "AAA" in scrollback
            buf.clearScreen();

            assertEquals(1, buf.getScrollbackSize());
            assertEquals("AAA", buf.getScrollbackLine(0));
        }

        @Test
        void clearScreenAndScrollbackClearsBoth() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 10);
            buf.writeText("AAABBB"); // "AAA" in scrollback
            buf.clearScreenAndScrollback();

            assertEquals("", buf.getLine(0));
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test
        void clearScreenAndScrollbackResetsCursor() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setCursorPosition(7, 4);
            buf.clearScreenAndScrollback();
            assertEquals(new CursorPosition(0, 0), buf.getCursorPosition());
        }

        @Test
        void multipleClearScreenCalls() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("Hello");
            buf.clearScreen();
            buf.clearScreen();
            buf.clearScreen();

            for (int i = 0; i < 3; i++) {
                assertEquals("", buf.getLine(i));
            }
            assertEquals(new CursorPosition(0, 0), buf.getCursorPosition());
        }
    }

    // ========================================================================
    // Content access
    // ========================================================================

    @Nested
    class ContentAccess {

        @Test
        void getCharAtScreen() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("ABCDE");

            assertEquals('A', buf.getCharAt(0, 0));
            assertEquals('E', buf.getCharAt(4, 0));
            assertEquals(' ', buf.getCharAt(5, 0)); // empty
        }

        @Test
        void getCharAtThrowsOnInvalidColumn() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(10, 0));
        }

        @Test
        void getCharAtThrowsOnInvalidRow() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(0, 5));
        }

        @Test
        void getAttributesAtScreen() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes attrs = TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD);
            buf.setCurrentAttributes(attrs);
            buf.writeText("A");

            assertEquals(attrs, buf.getAttributesAt(0, 0));
            assertEquals(TextAttributes.DEFAULT, buf.getAttributesAt(1, 0));
        }

        @Test
        void getLineFromScreen() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("Hello");
            buf.setCursorPosition(0, 1);
            buf.writeText("World");

            assertEquals("Hello", buf.getLine(0));
            assertEquals("World", buf.getLine(1));
            assertEquals("", buf.getLine(2));
        }

        @Test
        void getLineThrowsOnInvalidRow() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getLine(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getLine(5));
        }

        @Test
        void getScreenContent() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("AAAAA");
            buf.setCursorPosition(0, 1);
            buf.writeText("BBB");

            String expected = "AAAAA\nBBB\n";
            assertEquals(expected, buf.getScreenContent());
        }

        @Test
        void getScreenContentAllEmpty() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertEquals("\n\n", buf.getScreenContent());
        }

        @Test
        void getScrollbackCharAt() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 10);
            buf.writeText("ABCDEF");
            // "ABC" in scrollback (row 0), "DEF" on screen

            assertEquals('A', buf.getScrollbackCharAt(0, 0));
            assertEquals('B', buf.getScrollbackCharAt(1, 0));
            assertEquals('C', buf.getScrollbackCharAt(2, 0));
        }

        @Test
        void getScrollbackCharAtThrowsOnInvalidRow() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 10);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getScrollbackCharAt(0, 0));
        }

        @Test
        void getScrollbackAttributesAt() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 10);
            TextAttributes bold = TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD);
            buf.setCurrentAttributes(bold);
            buf.writeText("ABCDEF");

            assertEquals(bold, buf.getScrollbackAttributesAt(0, 0));
        }

        @Test
        void getScrollbackLine() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 10);
            buf.writeText("ABCDEF");

            assertEquals("ABC", buf.getScrollbackLine(0));
        }

        @Test
        void getAllContentWithScrollback() {
            TerminalBuffer buf = new TerminalBuffer(3, 2, 10);
            buf.writeText("AAABBBCCCDDD");

            // Scrollback: AAA, BBB  |  Screen: CCC, DDD
            String expected = "AAA\nBBB\nCCC\nDDD";
            assertEquals(expected, buf.getAllContent());
        }

        @Test
        void getAllContentWithoutScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 2, 0);
            buf.writeText("Hello");
            buf.setCursorPosition(0, 1);
            buf.writeText("World");

            assertEquals("Hello\nWorld", buf.getAllContent());
        }

        @Test
        void getAllContentEmptyBuffer() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertEquals("\n\n", buf.getAllContent());
        }

        @Test
        void scrollbackSize() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 10);
            assertEquals(0, buf.getScrollbackSize());

            buf.writeText("AAABBB");
            assertEquals(1, buf.getScrollbackSize());

            buf.writeText("CCCDDD");
            assertEquals(3, buf.getScrollbackSize());
        }
    }

    // ========================================================================
    // Edge cases
    // ========================================================================

    @Nested
    class EdgeCases {

        @Test
        void minimumBuffer1x1() {
            TerminalBuffer buf = new TerminalBuffer(1, 1, 0);
            buf.writeText("A");
            assertEquals("A", buf.getLine(0));
            assertEquals(new CursorPosition(1, 0), buf.getCursorPosition());
        }

        @Test
        void minimumBuffer1x1ScrollsOnSecondChar() {
            TerminalBuffer buf = new TerminalBuffer(1, 1, 5);
            buf.writeText("ABCDE");

            assertEquals("E", buf.getLine(0));
            assertEquals(4, buf.getScrollbackSize());
            assertEquals("A", buf.getScrollbackLine(0));
            assertEquals("D", buf.getScrollbackLine(3));
        }

        @Test
        void zeroScrollbackNeverStoresHistory() {
            TerminalBuffer buf = new TerminalBuffer(5, 2, 0);
            for (int i = 0; i < 10; i++) {
                buf.insertEmptyLineAtBottom();
            }
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test
        void cursorAtLastCellThenWriteOneChar() {
            TerminalBuffer buf = new TerminalBuffer(5, 2, 0);
            buf.setCursorPosition(4, 0);
            buf.writeText("X");

            assertEquals('X', buf.getCharAt(4, 0));
            // Cursor should be at col 5 (past end), next write will wrap
            assertEquals(new CursorPosition(5, 0), buf.getCursorPosition());
        }

        @Test
        void cursorAtLastCellThenWriteTwoChars() {
            TerminalBuffer buf = new TerminalBuffer(5, 2, 0);
            buf.setCursorPosition(4, 0);
            buf.writeText("XY");

            assertEquals('X', buf.getCharAt(4, 0));
            assertEquals('Y', buf.getCharAt(0, 1));
        }

        @Test
        void fillLineOnFreshBuffer() {
            TerminalBuffer buf = new TerminalBuffer(3, 2, 0);
            buf.fillLine('.');
            assertEquals("...", buf.getLine(0));
            assertEquals("", buf.getLine(1)); // other rows unaffected
        }

        @Test
        void scrollbackFullThenClearScreenAndScrollback() {
            TerminalBuffer buf = new TerminalBuffer(3, 1, 2);
            buf.writeText("AAABBBCCC"); // scrollback: [AAA, BBB], screen: CCC
            assertEquals(2, buf.getScrollbackSize());

            buf.clearScreenAndScrollback();
            assertEquals(0, buf.getScrollbackSize());
            assertEquals("", buf.getLine(0));
        }

        @Test
        void setCursorPositionWithLargeNegativeValues() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setCursorPosition(-1000, -1000);
            assertEquals(new CursorPosition(0, 0), buf.getCursorPosition());
        }

        @Test
        void setCursorPositionWithLargePositiveValues() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.setCursorPosition(Integer.MAX_VALUE, Integer.MAX_VALUE);
            assertEquals(new CursorPosition(9, 4), buf.getCursorPosition());
        }

        @Test
        void writeTextAfterClearScreen() {
            TerminalBuffer buf = new TerminalBuffer(10, 3, 0);
            buf.writeText("Hello");
            buf.clearScreen();
            buf.writeText("World");
            assertEquals("World", buf.getLine(0));
        }

        @Test
        void multipleWritesOnDifferentLines() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("Line0");
            buf.setCursorPosition(0, 1);
            buf.writeText("Line1");
            buf.setCursorPosition(0, 2);
            buf.writeText("Line2");

            assertEquals("Line0", buf.getLine(0));
            assertEquals("Line1", buf.getLine(1));
            assertEquals("Line2", buf.getLine(2));
        }

        @Test
        void insertTextShiftPreservesAttributes() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes bold = TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD);
            buf.setCurrentAttributes(bold);
            buf.writeText("AB");

            buf.setCursorPosition(1, 0);
            buf.resetAttributes();
            buf.insertText("X");

            // Column 0: A (bold), Column 1: X (default), Column 2: B (bold)
            assertTrue(buf.getAttributesAt(0, 0).hasStyle(StyleFlag.BOLD));
            assertFalse(buf.getAttributesAt(1, 0).hasStyle(StyleFlag.BOLD));
            assertTrue(buf.getAttributesAt(2, 0).hasStyle(StyleFlag.BOLD));
        }

        @Test
        void getAllContentSingleLineNoScrollback() {
            TerminalBuffer buf = new TerminalBuffer(10, 1, 0);
            buf.writeText("Test");
            assertEquals("Test", buf.getAllContent());
        }

        @Test
        void screenContentOfBufferAfterHeavyScrolling() {
            TerminalBuffer buf = new TerminalBuffer(5, 2, 3);
            // Write 25 chars = 5 full lines, in a 2-line screen with 3 scrollback
            // Lines produced: AAAAA, BBBBB, CCCCC, DDDDD, EEEEE
            // Screen holds 2, so 3 lines scroll off → exactly fits maxScrollback=3
            buf.writeText("AAAAABBBBBCCCCCDDDDDEEEEE");

            assertEquals("DDDDD", buf.getLine(0));
            assertEquals("EEEEE", buf.getLine(1));

            assertEquals(3, buf.getScrollbackSize());
            assertEquals("AAAAA", buf.getScrollbackLine(0));
            assertEquals("BBBBB", buf.getScrollbackLine(1));
            assertEquals("CCCCC", buf.getScrollbackLine(2));
        }

        @Test
        void scrollbackEvictionWithHeavyScrolling() {
            TerminalBuffer buf = new TerminalBuffer(5, 2, 2);
            // 5 full lines, screen=2, scrollback max=2 → oldest line evicted
            buf.writeText("AAAAABBBBBCCCCCDDDDDEEEEE");

            assertEquals("DDDDD", buf.getLine(0));
            assertEquals("EEEEE", buf.getLine(1));

            assertEquals(2, buf.getScrollbackSize());
            // AAAAA was evicted since only 2 scrollback slots
            assertEquals("BBBBB", buf.getScrollbackLine(0));
            assertEquals("CCCCC", buf.getScrollbackLine(1));
        }
    }

    // ========================================================================
    // Wide character support (bonus)
    // ========================================================================

    @Nested
    class WideCharacters {

        @Test
        void writeWideCharacterOccupiesTwoCells() {
            TerminalBuffer buf = new TerminalBuffer(10, 3, 0);
            buf.writeText("中");

            assertEquals('中', buf.getCharAt(0, 0));
            // Main cell should have displayWidth 2
            assertEquals(2, buf.getCellAt(0, 0).getDisplayWidth());
            // Second cell should be a placeholder
            assertTrue(buf.getCellAt(1, 0).isPlaceholder());
            // Cursor should advance by 2
            assertEquals(new CursorPosition(2, 0), buf.getCursorPosition());
        }

        @Test
        void writeWideCharGetText() {
            TerminalBuffer buf = new TerminalBuffer(10, 3, 0);
            buf.writeText("中文");

            assertEquals("中文", buf.getLine(0));
        }

        @Test
        void writeWideCharactersMixedWithNarrow() {
            TerminalBuffer buf = new TerminalBuffer(10, 3, 0);
            buf.writeText("A中B");

            assertEquals("A中B", buf.getLine(0));
            // A=1col, 中=2cols, B=1col → total 4 columns used
            assertEquals(new CursorPosition(4, 0), buf.getCursorPosition());
        }

        @Test
        void wideCharWrapsWhenAtPenultimateColumn() {
            // Width 5: if cursor at col 4, wide char doesn't fit (needs 2 cells)
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("ABCD");  // cursor at col 4
            buf.writeText("中");    // doesn't fit, should wrap

            assertEquals("ABCD", buf.getLine(0));  // col 4 filled with space (trimmed)
            assertEquals("中", buf.getLine(1));
        }

        @Test
        void wideCharAtExactEndOfLine() {
            // Width 6, cursor at 4 → 2 cells left → wide char fits exactly
            TerminalBuffer buf = new TerminalBuffer(6, 3, 0);
            buf.writeText("ABCD");  // cursor at col 4
            buf.writeText("中");    // fits in cols 4-5

            assertEquals("ABCD中", buf.getLine(0));
            assertEquals(new CursorPosition(6, 0), buf.getCursorPosition());
        }

        @Test
        void overwriteWideCharWithNarrowClearsBothCells() {
            TerminalBuffer buf = new TerminalBuffer(10, 3, 0);
            buf.writeText("中");  // occupies cols 0-1
            buf.setCursorPosition(0, 0);
            buf.writeText("A");   // overwrite main cell

            assertEquals('A', buf.getCharAt(0, 0));
            assertEquals(' ', buf.getCharAt(1, 0)); // placeholder cleared
            assertFalse(buf.getCellAt(1, 0).isPlaceholder());
        }

        @Test
        void overwritePlaceholderClearsWideChar() {
            TerminalBuffer buf = new TerminalBuffer(10, 3, 0);
            buf.writeText("中");  // occupies cols 0-1
            buf.setCursorPosition(1, 0);
            buf.writeText("X");   // overwrite placeholder

            assertEquals(' ', buf.getCharAt(0, 0)); // main cell cleared
            assertEquals('X', buf.getCharAt(1, 0));
        }

        @Test
        void multipleWideCharacters() {
            TerminalBuffer buf = new TerminalBuffer(10, 3, 0);
            buf.writeText("你好世界");  // 4 wide chars = 8 columns

            assertEquals("你好世界", buf.getLine(0));
            assertEquals(new CursorPosition(8, 0), buf.getCursorPosition());
        }

        @Test
        void wideCharScrollsScreen() {
            TerminalBuffer buf = new TerminalBuffer(4, 2, 10);
            buf.writeText("AB中CD");
            // AB at row 0 cols 0-1; 中 needs 2 cols, cursor at 2, fits at 2-3
            // then C wraps to row 1 col 0, D at row 1 col 1
            assertEquals("AB中", buf.getLine(0));
            assertEquals("CD", buf.getLine(1));
        }

        @Test
        void charWidthDetectsCJK() {
            assertTrue(CharWidth.isWide('中'));   // CJK Unified Ideograph
            assertTrue(CharWidth.isWide('文'));   // CJK Unified Ideograph
            assertTrue(CharWidth.isWide('你'));   // CJK Unified Ideograph
            assertTrue(CharWidth.isWide('Ａ'));   // Fullwidth Latin A (U+FF21)
            assertFalse(CharWidth.isWide('A'));   // ASCII
            assertFalse(CharWidth.isWide(' '));   // Space
            assertFalse(CharWidth.isWide('é'));   // Latin with accent
        }

        @Test
        void wideCharInMinimumWidthBuffer() {
            // Buffer width 2: wide char fits exactly
            TerminalBuffer buf = new TerminalBuffer(2, 2, 0);
            buf.writeText("中");

            assertEquals("中", buf.getLine(0));
            assertEquals(new CursorPosition(2, 0), buf.getCursorPosition());
        }

        @Test
        void wideCharInWidth1BufferWraps() {
            // Buffer width 1: wide char can never fit, writes space & wraps continuously
            TerminalBuffer buf = new TerminalBuffer(1, 3, 0);
            buf.writeText("中");

            // Wide char needs 2 cols, but width is 1. It loops wrapping.
            // This is a degenerate case — just verify it doesn't throw/hang.
            assertNotNull(buf.getLine(0));
        }

    }
}
