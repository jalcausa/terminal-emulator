package terminal;

import java.util.Arrays;

/**
 * Represents a single line (row) in the terminal buffer.
 * A line is a fixed-width array of {@link Cell} instances. It provides operations
 * for getting/setting individual cells, filling, clearing, and extracting text content.
 * This class is mutable and not thread-safe.
 */
public final class TerminalLine {

    private Cell[] cells;

    /**
     * Creates a new terminal line filled with empty cells.
     *
     * @param width the number of columns; must be at least 1
     * @throws IllegalArgumentException if width is less than 1
     */
    public TerminalLine(int width) {
        if (width < 1) {
            throw new IllegalArgumentException("Width must be at least 1, got: " + width);
        }
        this.cells = new Cell[width];
        Arrays.fill(this.cells, Cell.EMPTY);
    }

    /**
     * Private constructor for creating a line from an existing cell array (no copy).
     */
    private TerminalLine(Cell[] cells) {
        this.cells = cells;
    }

    /**
     * Returns the width (number of columns) of this line.
     */
    public int getWidth() {
        return cells.length;
    }

    /**
     * Returns the cell at the given column.
     *
     * @param col the 0-based column index
     * @throws IndexOutOfBoundsException if col is out of range
     */
    public Cell getCell(int col) {
        checkColumn(col);
        return cells[col];
    }

    /**
     * Sets the cell at the given column.
     *
     * @param col  the 0-based column index
     * @param cell the cell to set; must not be null
     * @throws IndexOutOfBoundsException if col is out of range
     */
    public void setCell(int col, Cell cell) {
        checkColumn(col);
        cells[col] = cell;
    }

    /**
     * Returns the text content of this line as a string.
     * Placeholder cells (trailing cells of wide characters) are skipped.
     * Trailing spaces are trimmed.
     */
    public String getText() {
        StringBuilder sb = new StringBuilder(cells.length);
        for (Cell cell : cells) {
            if (!cell.isPlaceholder()) {
                sb.append(cell.getCharacter());
            }
        }
        // Trim trailing spaces
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') {
            end--;
        }
        return sb.substring(0, end);
    }

    /**
     * Fills the entire line with the given character and attributes.
     *
     * @param c     the character to fill with
     * @param attrs the attributes to apply
     */
    public void fill(char c, TextAttributes attrs) {
        Cell cell = new Cell(c, attrs);
        Arrays.fill(cells, cell);
    }

    /**
     * Clears the entire line (fills with empty cells).
     */
    public void clear() {
        Arrays.fill(cells, Cell.EMPTY);
    }

    /**
     * Creates a deep copy of this line.
     */
    public TerminalLine copy() {
        Cell[] copied = Arrays.copyOf(cells, cells.length);
        return new TerminalLine(copied);
    }

    /**
     * Resizes this line to a new width.
     * If the new width is smaller, cells beyond the new width are discarded.
     * If the new width is larger, the line is extended with empty cells.
     *
     * @param newWidth the new width; must be at least 1
     * @throws IllegalArgumentException if newWidth is less than 1
     */
    public void resize(int newWidth) {
        if (newWidth < 1) {
            throw new IllegalArgumentException("Width must be at least 1, got: " + newWidth);
        }
        if (newWidth == cells.length) {
            return;
        }
        Cell[] newCells = new Cell[newWidth];
        int copyLen = Math.min(cells.length, newWidth);
        System.arraycopy(cells, 0, newCells, 0, copyLen);
        for (int i = copyLen; i < newWidth; i++) {
            newCells[i] = Cell.EMPTY;
        }
        this.cells = newCells;
    }

    private void checkColumn(int col) {
        if (col < 0 || col >= cells.length) {
            throw new IndexOutOfBoundsException(
                    "Column " + col + " out of range [0, " + (cells.length - 1) + "]");
        }
    }
}
