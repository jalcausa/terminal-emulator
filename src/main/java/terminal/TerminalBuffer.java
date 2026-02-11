package terminal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A terminal text buffer — the core data structure that terminal emulators use
 * to store and manipulate displayed text.
 * The buffer consists of two logical parts:
 *   Screen — the last {@code height} lines that fit the screen dimensions.
 *       This is the editable area and what users see.
 *  Scrollback — lines that scrolled off the top of the screen, preserved
 *       for history. These are unmodifiable through normal editing operations.
 * The buffer maintains a cursor position and current text attributes that are used
 * for subsequent editing operations.
 */
public final class TerminalBuffer {

    private int width;
    private int height;
    private final int maxScrollbackSize;

    private final List<TerminalLine> screen;
    private final Deque<TerminalLine> scrollback;

    private int cursorColumn;
    private int cursorRow;
    private TextAttributes currentAttributes;

    /**
     * Creates a new terminal buffer with the given dimensions.
     *
     * @param width             number of columns; must be at least 1
     * @param height            number of rows (visible screen lines); must be at least 1
     * @param maxScrollbackSize maximum number of lines kept in scrollback history; 0 or more
     * @throws IllegalArgumentException if width or height is less than 1, or maxScrollbackSize is negative
     */
    public TerminalBuffer(int width, int height, int maxScrollbackSize) {
        if (width < 1) {
            throw new IllegalArgumentException("Width must be at least 1, got: " + width);
        }
        if (height < 1) {
            throw new IllegalArgumentException("Height must be at least 1, got: " + height);
        }
        if (maxScrollbackSize < 0) {
            throw new IllegalArgumentException("maxScrollbackSize must be non-negative, got: " + maxScrollbackSize);
        }

        this.width = width;
        this.height = height;
        this.maxScrollbackSize = maxScrollbackSize;

        this.screen = new ArrayList<>(height);
        for (int i = 0; i < height; i++) {
            screen.add(new TerminalLine(width));
        }

        this.scrollback = new ArrayDeque<>();
        this.cursorColumn = 0;
        this.cursorRow = 0;
        this.currentAttributes = TextAttributes.DEFAULT;
    }

    // ========================================================================
    // Dimension accessors
    // ========================================================================

    /** Returns the screen width (number of columns). */
    public int getWidth() {
        return width;
    }

    /** Returns the screen height (number of visible rows). */
    public int getHeight() {
        return height;
    }

    /** Returns the maximum number of scrollback lines. */
    public int getMaxScrollbackSize() {
        return maxScrollbackSize;
    }

    // ========================================================================
    // Cursor operations
    // ========================================================================

    /**
     * Returns the current cursor position.
     */
    public CursorPosition getCursorPosition() {
        return new CursorPosition(cursorColumn, cursorRow);
    }

    /**
     * Sets the cursor position, clamping to valid screen bounds.
     * Column is clamped to [0, width-1] and row to [0, height-1].
     *
     * @param column the desired column (0-based)
     * @param row    the desired row (0-based)
     */
    public void setCursorPosition(int column, int row) {
        this.cursorColumn = clamp(column, 0, width - 1);
        this.cursorRow = clamp(row, 0, height - 1);
    }

    /**
     * Moves the cursor up by {@code n} rows. Stops at the top edge (row 0).
     *
     * @param n number of rows to move; if 0 or negative, does nothing
     */
    public void moveCursorUp(int n) {
        if (n > 0) {
            cursorRow = Math.max(0, cursorRow - n);
        }
    }

    /**
     * Moves the cursor down by {@code n} rows. Stops at the bottom edge (row height-1).
     *
     * @param n number of rows to move; if 0 or negative, does nothing
     */
    public void moveCursorDown(int n) {
        if (n > 0) {
            cursorRow = Math.min(height - 1, cursorRow + n);
        }
    }

    /**
     * Moves the cursor left by {@code n} columns. Stops at the left edge (column 0).
     *
     * @param n number of columns to move; if 0 or negative, does nothing
     */
    public void moveCursorLeft(int n) {
        if (n > 0) {
            cursorColumn = Math.max(0, cursorColumn - n);
        }
    }

    /**
     * Moves the cursor right by {@code n} columns. Stops at the right edge (column width-1).
     *
     * @param n number of columns to move; if 0 or negative, does nothing
     */
    public void moveCursorRight(int n) {
        if (n > 0) {
            cursorColumn = Math.min(width - 1, cursorColumn + n);
        }
    }

    // ========================================================================
    // Attribute operations
    // ========================================================================

    /** Returns the current text attributes used for editing operations. */
    public TextAttributes getCurrentAttributes() {
        return currentAttributes;
    }

    /** Sets the current text attributes. */
    public void setCurrentAttributes(TextAttributes attrs) {
        this.currentAttributes = attrs;
    }

    /** Sets the foreground color of the current attributes. */
    public void setForeground(TerminalColor color) {
        this.currentAttributes = currentAttributes.withForeground(color);
    }

    /** Sets the background color of the current attributes. */
    public void setBackground(TerminalColor color) {
        this.currentAttributes = currentAttributes.withBackground(color);
    }

    /** Adds a style flag to the current attributes. */
    public void addStyle(StyleFlag flag) {
        this.currentAttributes = currentAttributes.withStyle(flag);
    }

    /** Removes a style flag from the current attributes. */
    public void removeStyle(StyleFlag flag) {
        this.currentAttributes = currentAttributes.withoutStyle(flag);
    }

    /** Resets the current attributes to defaults. */
    public void resetAttributes() {
        this.currentAttributes = TextAttributes.DEFAULT;
    }

    // ========================================================================
    // Editing operations
    // ========================================================================

    /**
     * Writes text at the current cursor position, overriding existing content.
     * Each character is written with the current attributes. The cursor advances
     * after each character. When the cursor reaches the end of a line, it wraps
     * to the beginning of the next line. When it reaches the bottom of the screen,
     * the screen scrolls up (top line moves to scrollback) and writing continues
     * on the new bottom line.
     *
     * @param text the text to write; if empty, does nothing
     */
    public void writeText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            writeCharacter(c);
        }
    }

    /**
     * Inserts text at the current cursor position, shifting existing content
     * to the right. Content that goes beyond the line width is discarded.
     * The cursor advances after each inserted character.
     *
     * @param text the text to insert; if empty, does nothing
     */
    public void insertText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            insertCharacter(c);
        }
    }

    /**
     * Fills the current line (at the cursor's row) with the given character
     * using the current attributes.
     * Does not move the cursor.
     *
     * @param c the character to fill with
     */
    public void fillLine(char c) {
        screen.get(cursorRow).fill(c, currentAttributes);
    }

    // ========================================================================
    // Screen operations
    // ========================================================================

    /**
     * Inserts an empty line at the bottom of the screen.
     * The top line of the screen is moved to the scrollback, and a new empty line
     * is added at the bottom. If the scrollback exceeds its maximum size, the
     * oldest line is discarded.
     */
    public void insertEmptyLineAtBottom() {
        scrollUp();
    }

    /**
     * Clears the entire screen (replaces all lines with empty lines).
     * The scrollback is not affected. The cursor is reset to (0, 0).
     */
    public void clearScreen() {
        for (int i = 0; i < height; i++) {
            screen.set(i, new TerminalLine(width));
        }
        cursorColumn = 0;
        cursorRow = 0;
    }

    /**
     * Clears both the screen and the scrollback history.
     * The cursor is reset to (0, 0).
     */
    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    // ========================================================================
    // Content access — Screen
    // ========================================================================

    /**
     * Returns the character at the given screen position.
     *
     * @param column 0-based column
     * @param row    0-based row (0 = first visible line)
     * @throws IndexOutOfBoundsException if position is out of range
     */
    public char getCharAt(int column, int row) {
        checkScreenBounds(column, row);
        return screen.get(row).getCell(column).getCharacter();
    }

    /**
     * Returns the text attributes at the given screen position.
     *
     * @param column 0-based column
     * @param row    0-based row
     * @throws IndexOutOfBoundsException if position is out of range
     */
    public TextAttributes getAttributesAt(int column, int row) {
        checkScreenBounds(column, row);
        return screen.get(row).getCell(column).getAttributes();
    }

    /**
     * Returns the text content of a screen line (trailing spaces trimmed).
     *
     * @param row 0-based row index
     * @throws IndexOutOfBoundsException if row is out of range
     */
    public String getLine(int row) {
        checkRowBounds(row, height, "Screen row");
        return screen.get(row).getText();
    }

    /**
     * Returns the full {@link Cell} at the given screen position.
     * <p>
     * This provides access to all cell properties including display width and
     * placeholder status, which are relevant for wide character handling.
     *
     * @param column 0-based column
     * @param row    0-based row (0 = first visible line)
     * @throws IndexOutOfBoundsException if position is out of range
     */
    public Cell getCellAt(int column, int row) {
        checkScreenBounds(column, row);
        return screen.get(row).getCell(column);
    }

    /**
     * Returns the entire screen content as a string with lines separated by newlines.
     * Trailing spaces on each line are trimmed.
     */
    public String getScreenContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < height; i++) {
            if (i > 0) sb.append('\n');
            sb.append(screen.get(i).getText());
        }
        return sb.toString();
    }

    // ========================================================================
    // Content access — Scrollback
    // ========================================================================

    /**
     * Returns the number of lines currently in the scrollback.
     */
    public int getScrollbackSize() {
        return scrollback.size();
    }

    /**
     * Returns the character at the given scrollback position.
     *
     * @param column 0-based column
     * @param row    0-based row (0 = oldest line in scrollback)
     * @throws IndexOutOfBoundsException if position is out of range
     */
    public char getScrollbackCharAt(int column, int row) {
        TerminalLine line = getScrollbackLineInternal(row);
        if (column < 0 || column >= width) {
            throw new IndexOutOfBoundsException(
                    "Column " + column + " out of range [0, " + (width - 1) + "]");
        }
        return line.getCell(column).getCharacter();
    }

    /**
     * Returns the text attributes at the given scrollback position.
     *
     * @param column 0-based column
     * @param row    0-based row (0 = oldest line in scrollback)
     * @throws IndexOutOfBoundsException if position is out of range
     */
    public TextAttributes getScrollbackAttributesAt(int column, int row) {
        TerminalLine line = getScrollbackLineInternal(row);
        if (column < 0 || column >= width) {
            throw new IndexOutOfBoundsException(
                    "Column " + column + " out of range [0, " + (width - 1) + "]");
        }
        return line.getCell(column).getAttributes();
    }

    /**
     * Returns the text content of a scrollback line (trailing spaces trimmed).
     *
     * @param row 0-based row (0 = oldest line)
     * @throws IndexOutOfBoundsException if row is out of range
     */
    public String getScrollbackLine(int row) {
        return getScrollbackLineInternal(row).getText();
    }

    /**
     * Returns all content (scrollback + screen) as a string.
     * Scrollback lines appear first (oldest at top), followed by screen lines.
     * Lines are separated by newlines.
     */
    public String getAllContent() {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (TerminalLine line : scrollback) {
            if (index > 0) sb.append('\n');
            sb.append(line.getText());
            index++;
        }
        for (int i = 0; i < height; i++) {
            if (index > 0) sb.append('\n');
            sb.append(screen.get(i).getText());
            index++;
        }
        return sb.toString();
    }

    // ========================================================================
    // Resize
    // ========================================================================

    /**
     * Resizes the terminal buffer to new dimensions.
     * <p>
     * <b>Width change:</b> Each line (screen and scrollback) is resized. If a wide
     * character straddles the new right boundary (its main cell is at the last column
     * but its placeholder would be truncated), it is replaced with an empty cell.
     * <p>
     * <b>Height decrease:</b> Excess top lines are pushed into the scrollback
     * (subject to the scrollback size limit). The cursor row is adjusted accordingly.
     * <p>
     * <b>Height increase:</b> Lines are recovered from scrollback (most recent first)
     * and prepended to the screen. Any remaining rows are added as empty lines at the
     * bottom. The cursor row is adjusted to account for recovered scrollback lines.
     * <p>
     * After resizing, the cursor is clamped to valid bounds.
     *
     * @param newWidth  new number of columns; must be at least 1
     * @param newHeight new number of visible rows; must be at least 1
     * @throws IllegalArgumentException if newWidth or newHeight is less than 1
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth < 1) {
            throw new IllegalArgumentException("Width must be at least 1, got: " + newWidth);
        }
        if (newHeight < 1) {
            throw new IllegalArgumentException("Height must be at least 1, got: " + newHeight);
        }

        // --- Handle width change ---
        if (newWidth != width) {
            for (TerminalLine line : screen) {
                resizeLineWidth(line, newWidth);
            }
            for (TerminalLine line : scrollback) {
                resizeLineWidth(line, newWidth);
            }
            width = newWidth;
        }

        // --- Handle height change ---
        if (newHeight < height) {
            // Height decreased: move excess top screen lines to scrollback
            int linesToRemove = height - newHeight;
            for (int i = 0; i < linesToRemove; i++) {
                TerminalLine topLine = screen.remove(0);
                if (maxScrollbackSize > 0) {
                    scrollback.addLast(topLine);
                    while (scrollback.size() > maxScrollbackSize) {
                        scrollback.removeFirst();
                    }
                }
            }
            cursorRow -= linesToRemove;
            height = newHeight;
        } else if (newHeight > height) {
            // Height increased: recover lines from scrollback, add empty at bottom
            int linesToAdd = newHeight - height;
            int fromScrollback = Math.min(linesToAdd, scrollback.size());
            for (int i = 0; i < fromScrollback; i++) {
                TerminalLine line = scrollback.removeLast();
                screen.add(0, line);
            }
            for (int i = fromScrollback; i < linesToAdd; i++) {
                screen.add(new TerminalLine(width));
            }
            cursorRow += fromScrollback;
            height = newHeight;
        }

        // Clamp cursor to new bounds
        cursorColumn = clamp(cursorColumn, 0, width - 1);
        cursorRow = clamp(cursorRow, 0, height - 1);
    }

    /**
     * Resizes a single line, cleaning up any wide character that would be
     * split by the new boundary before truncation.
     */
    private void resizeLineWidth(TerminalLine line, int newWidth) {
        if (newWidth < line.getWidth()) {
            // Check if a wide character's main cell sits at the new last column
            Cell cellAtBoundary = line.getCell(newWidth - 1);
            if (cellAtBoundary.getDisplayWidth() == 2 && !cellAtBoundary.isPlaceholder()) {
                line.setCell(newWidth - 1, Cell.EMPTY);
            }
        }
        line.resize(newWidth);
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    /**
     * Writes a single character at the cursor position, advancing the cursor.
     * Handles line wrapping, screen scrolling, and wide characters.
     */
    private void writeCharacter(char c) {
        int charWidth = CharWidth.displayWidth(c);

        // If cursor is beyond the last column after a previous write, wrap first
        if (cursorColumn >= width) {
            cursorColumn = 0;
            cursorRow++;
            if (cursorRow >= height) {
                scrollUp();
                cursorRow = height - 1;
            }
        }

        if (charWidth == 2) {
            // Wide character needs 2 columns
            if (width < 2) {
                // Buffer too narrow for wide characters — write a space as fallback
                screen.get(cursorRow).setCell(cursorColumn, Cell.EMPTY);
                cursorColumn++;
                return;
            }
            if (cursorColumn == width - 1) {
                // Not enough room on this line — fill current cell with space and wrap
                screen.get(cursorRow).setCell(cursorColumn, Cell.EMPTY);
                cursorColumn = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scrollUp();
                    cursorRow = height - 1;
                }
            }
            // Clear any wide character/placeholder that we're overwriting
            clearWideCharAt(cursorRow, cursorColumn);
            clearWideCharAt(cursorRow, cursorColumn + 1);

            screen.get(cursorRow).setCell(cursorColumn,
                    new Cell(c, currentAttributes, 2, false));
            screen.get(cursorRow).setCell(cursorColumn + 1,
                    new Cell(' ', currentAttributes, 1, true));
            cursorColumn += 2;
        } else {
            // Normal single-width character
            clearWideCharAt(cursorRow, cursorColumn);
            screen.get(cursorRow).setCell(cursorColumn, new Cell(c, currentAttributes));
            cursorColumn++;
        }
    }

    /**
     * Inserts a single character at the cursor position, shifting existing content right.
     * Content beyond line width is discarded. Advances the cursor.
     */
    private void insertCharacter(char c) {
        if (cursorColumn >= width) {
            cursorColumn = 0;
            cursorRow++;
            if (cursorRow >= height) {
                scrollUp();
                cursorRow = height - 1;
            }
        }

        TerminalLine line = screen.get(cursorRow);
        int charWidth = CharWidth.displayWidth(c);

        if (charWidth == 2 && width < 2) {
            // Buffer too narrow for wide characters — write a space as fallback
            line.setCell(cursorColumn, Cell.EMPTY);
            cursorColumn++;
            return;
        }

        if (charWidth == 2 && cursorColumn == width - 1) {
            // Wide char at last column: fill with space and wrap
            line.setCell(cursorColumn, Cell.EMPTY);
            cursorColumn = 0;
            cursorRow++;
            if (cursorRow >= height) {
                scrollUp();
                cursorRow = height - 1;
            }
            line = screen.get(cursorRow);
        }

        if (charWidth == 2) {
            // Shift cells right by 2
            for (int col = width - 1; col > cursorColumn + 1; col--) {
                line.setCell(col, line.getCell(col - 2));
            }
            line.setCell(cursorColumn, new Cell(c, currentAttributes, 2, false));
            line.setCell(cursorColumn + 1, new Cell(' ', currentAttributes, 1, true));
            cursorColumn += 2;
        } else {
            // Shift cells right by 1
            for (int col = width - 1; col > cursorColumn; col--) {
                line.setCell(col, line.getCell(col - 1));
            }
            line.setCell(cursorColumn, new Cell(c, currentAttributes));
            cursorColumn++;
        }
    }

    /**
     * When overwriting a cell that is part of a wide character (either the main cell
     * or its placeholder), clears both cells to avoid rendering artifacts.
     */
    private void clearWideCharAt(int row, int col) {
        if (col < 0 || col >= width) return;
        TerminalLine line = screen.get(row);
        Cell cell = line.getCell(col);
        if (cell.getDisplayWidth() == 2 && !cell.isPlaceholder()) {
            // This is the main cell of a wide char; clear both it and its placeholder
            line.setCell(col, Cell.EMPTY);
            if (col + 1 < width) {
                line.setCell(col + 1, Cell.EMPTY);
            }
        } else if (cell.isPlaceholder()) {
            // This is the placeholder; clear both the main cell (to the left) and this
            line.setCell(col, Cell.EMPTY);
            if (col - 1 >= 0) {
                line.setCell(col - 1, Cell.EMPTY);
            }
        }
    }

    /**
     * Scrolls the screen up by one line: the top line moves to scrollback,
     * and a new empty line is added at the bottom.
     */
    private void scrollUp() {
        // Move top line to scrollback
        TerminalLine topLine = screen.remove(0);
        if (maxScrollbackSize > 0) {
            scrollback.addLast(topLine.copy());
            // Trim scrollback if over limit
            while (scrollback.size() > maxScrollbackSize) {
                scrollback.removeFirst();
            }
        }
        // Add new empty line at bottom
        screen.add(new TerminalLine(width));
    }

    private TerminalLine getScrollbackLineInternal(int row) {
        if (row < 0 || row >= scrollback.size()) {
            throw new IndexOutOfBoundsException(
                    "Scrollback row " + row + " out of range [0, " + (scrollback.size() - 1) + "]");
        }
        // ArrayDeque doesn't support indexed access, so we iterate
        int i = 0;
        for (TerminalLine line : scrollback) {
            if (i == row) return line;
            i++;
        }
        throw new AssertionError("Unreachable");
    }

    private void checkScreenBounds(int column, int row) {
        if (column < 0 || column >= width) {
            throw new IndexOutOfBoundsException(
                    "Column " + column + " out of range [0, " + (width - 1) + "]");
        }
        checkRowBounds(row, height, "Screen row");
    }

    private void checkRowBounds(int row, int max, String label) {
        if (row < 0 || row >= max) {
            throw new IndexOutOfBoundsException(
                    label + " " + row + " out of range [0, " + (max - 1) + "]");
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
