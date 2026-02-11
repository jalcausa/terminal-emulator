package terminal;

import java.util.Objects;

/**
 * Represents a single cell in the terminal grid.
 * <p>
 * Each cell holds a character, its text attributes (colors and styles),
 * a display width (1 for normal characters, 2 for wide CJK characters),
 * and a placeholder flag for the trailing cell of a wide character.
 * <p>
 * This class is immutable.
 */
public final class Cell {

    /** An empty cell: a space character with default attributes. */
    public static final Cell EMPTY = new Cell(' ', TextAttributes.DEFAULT, 1, false);

    private final char character;
    private final TextAttributes attributes;
    private final int displayWidth;
    private final boolean placeholder;

    /**
     * Creates a cell with normal width and no placeholder flag.
     *
     * @param character  the character to display
     * @param attributes the text attributes
     */
    public Cell(char character, TextAttributes attributes) {
        this(character, attributes, 1, false);
    }

    /**
     * Creates a cell with full control over all fields.
     *
     * @param character    the character to display
     * @param attributes   the text attributes
     * @param displayWidth the number of columns this character occupies (1 or 2)
     * @param placeholder  true if this cell is the trailing placeholder of a wide character
     */
    public Cell(char character, TextAttributes attributes, int displayWidth, boolean placeholder) {
        this.character = character;
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
        this.displayWidth = displayWidth;
        this.placeholder = placeholder;
    }

    public char getCharacter() {
        return character;
    }

    public TextAttributes getAttributes() {
        return attributes;
    }

    /**
     * Returns the display width: 1 for normal characters, 2 for wide characters.
     */
    public int getDisplayWidth() {
        return displayWidth;
    }

    /**
     * Returns true if this cell is a placeholder occupying the second column of a wide character.
     */
    public boolean isPlaceholder() {
        return placeholder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cell cell)) return false;
        return character == cell.character
                && displayWidth == cell.displayWidth
                && placeholder == cell.placeholder
                && Objects.equals(attributes, cell.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(character, attributes, displayWidth, placeholder);
    }

    @Override
    public String toString() {
        String repr = placeholder ? "PLACEHOLDER" : "'" + character + "'";
        return "Cell[" + repr + ", width=" + displayWidth + ", " + attributes + "]";
    }
}
