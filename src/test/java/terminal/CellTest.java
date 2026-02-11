package terminal;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class CellTest {

    @Test
    void emptyCellHasSpaceAndDefaultAttributes() {
        assertEquals(' ', Cell.EMPTY.getCharacter());
        assertEquals(TextAttributes.DEFAULT, Cell.EMPTY.getAttributes());
        assertEquals(1, Cell.EMPTY.getDisplayWidth());
        assertFalse(Cell.EMPTY.isPlaceholder());
    }

    @Test
    void simpleCellConstructor() {
        TextAttributes attrs = TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD);
        Cell cell = new Cell('A', attrs);

        assertEquals('A', cell.getCharacter());
        assertEquals(attrs, cell.getAttributes());
        assertEquals(1, cell.getDisplayWidth());
        assertFalse(cell.isPlaceholder());
    }

    @Test
    void fullCellConstructorWideCharacter() {
        TextAttributes attrs = TextAttributes.DEFAULT;
        Cell cell = new Cell('中', attrs, 2, false);

        assertEquals('中', cell.getCharacter());
        assertEquals(2, cell.getDisplayWidth());
        assertFalse(cell.isPlaceholder());
    }

    @Test
    void placeholderCell() {
        Cell cell = new Cell(' ', TextAttributes.DEFAULT, 1, true);

        assertEquals(' ', cell.getCharacter());
        assertTrue(cell.isPlaceholder());
    }

    @Test
    void equalityForIdenticalCells() {
        TextAttributes attrs = new TextAttributes(
                TerminalColor.of(AnsiColor.RED),
                TerminalColor.defaultColor(),
                EnumSet.of(StyleFlag.ITALIC));
        Cell a = new Cell('X', attrs);
        Cell b = new Cell('X', attrs);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityForDifferentCharacters() {
        Cell a = new Cell('A', TextAttributes.DEFAULT);
        Cell b = new Cell('B', TextAttributes.DEFAULT);
        assertNotEquals(a, b);
    }

    @Test
    void inequalityForDifferentAttributes() {
        Cell a = new Cell('A', TextAttributes.DEFAULT);
        Cell b = new Cell('A', TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD));
        assertNotEquals(a, b);
    }

    @Test
    void inequalityForDifferentWidth() {
        Cell a = new Cell('中', TextAttributes.DEFAULT, 1, false);
        Cell b = new Cell('中', TextAttributes.DEFAULT, 2, false);
        assertNotEquals(a, b);
    }

    @Test
    void inequalityForPlaceholderFlag() {
        Cell a = new Cell(' ', TextAttributes.DEFAULT, 1, false);
        Cell b = new Cell(' ', TextAttributes.DEFAULT, 1, true);
        assertNotEquals(a, b);
    }

    @Test
    void constructorRejectsNullAttributes() {
        assertThrows(NullPointerException.class, () -> new Cell('A', null));
    }

    @Test
    void toStringShowsCharacterInfo() {
        Cell cell = new Cell('Z', TextAttributes.DEFAULT);
        String str = cell.toString();
        assertTrue(str.contains("'Z'"));
        assertTrue(str.contains("width=1"));
    }

    @Test
    void toStringShowsPlaceholder() {
        Cell cell = new Cell(' ', TextAttributes.DEFAULT, 1, true);
        String str = cell.toString();
        assertTrue(str.contains("PLACEHOLDER"));
    }
}
