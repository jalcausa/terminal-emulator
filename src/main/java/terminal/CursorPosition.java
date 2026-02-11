package terminal;

import java.util.Objects;

/**
 * Represents an immutable cursor position in the terminal grid.
 * Both column and row are 0-indexed.
 */
public final class CursorPosition {

    private final int column;
    private final int row;

    public CursorPosition(int column, int row) {
        this.column = column;
        this.row = row;
    }

    /** Returns the 0-based column index. */
    public int getColumn() {
        return column;
    }

    /** Returns the 0-based row index. */
    public int getRow() {
        return row;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CursorPosition that)) return false;
        return column == that.column && row == that.row;
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, row);
    }

    @Override
    public String toString() {
        return "CursorPosition[col=" + column + ", row=" + row + "]";
    }
}
