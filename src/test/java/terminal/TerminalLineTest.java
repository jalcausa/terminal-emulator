package terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerminalLineTest {

    @Test
    void newLineIsFilledWithEmptyCells() {
        TerminalLine line = new TerminalLine(5);
        assertEquals(5, line.getWidth());
        for (int i = 0; i < 5; i++) {
            assertEquals(Cell.EMPTY, line.getCell(i));
        }
    }

    @Test
    void constructorRejectsZeroWidth() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalLine(0));
    }

    @Test
    void constructorRejectsNegativeWidth() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalLine(-1));
    }

    @Test
    void setCellAndGetCell() {
        TerminalLine line = new TerminalLine(10);
        Cell cell = new Cell('A', TextAttributes.DEFAULT.withStyle(StyleFlag.BOLD));
        line.setCell(3, cell);

        assertEquals(cell, line.getCell(3));
        assertEquals(Cell.EMPTY, line.getCell(0)); // other cells unchanged
    }

    @Test
    void getCellThrowsOnNegativeIndex() {
        TerminalLine line = new TerminalLine(5);
        assertThrows(IndexOutOfBoundsException.class, () -> line.getCell(-1));
    }

    @Test
    void getCellThrowsOnIndexBeyondWidth() {
        TerminalLine line = new TerminalLine(5);
        assertThrows(IndexOutOfBoundsException.class, () -> line.getCell(5));
    }

    @Test
    void setCellThrowsOnInvalidIndex() {
        TerminalLine line = new TerminalLine(5);
        assertThrows(IndexOutOfBoundsException.class,
                () -> line.setCell(5, Cell.EMPTY));
    }

    @Test
    void getTextReturnsEmptyForBlankLine() {
        TerminalLine line = new TerminalLine(10);
        assertEquals("", line.getText());
    }

    @Test
    void getTextReturnsContentTrimmingTrailingSpaces() {
        TerminalLine line = new TerminalLine(10);
        line.setCell(0, new Cell('H', TextAttributes.DEFAULT));
        line.setCell(1, new Cell('i', TextAttributes.DEFAULT));

        assertEquals("Hi", line.getText());
    }

    @Test
    void getTextPreservesInternalSpaces() {
        TerminalLine line = new TerminalLine(10);
        line.setCell(0, new Cell('A', TextAttributes.DEFAULT));
        line.setCell(1, new Cell(' ', TextAttributes.DEFAULT));
        line.setCell(2, new Cell('B', TextAttributes.DEFAULT));

        assertEquals("A B", line.getText());
    }

    @Test
    void getTextSkipsPlaceholderCells() {
        TerminalLine line = new TerminalLine(5);
        line.setCell(0, new Cell('中', TextAttributes.DEFAULT, 2, false));
        line.setCell(1, new Cell(' ', TextAttributes.DEFAULT, 1, true)); // placeholder

        assertEquals("中", line.getText());
    }

    @Test
    void fillSetsAllCells() {
        TerminalLine line = new TerminalLine(4);
        TextAttributes attrs = TextAttributes.DEFAULT.withForeground(TerminalColor.of(AnsiColor.RED));
        line.fill('X', attrs);

        for (int i = 0; i < 4; i++) {
            assertEquals('X', line.getCell(i).getCharacter());
            assertEquals(attrs, line.getCell(i).getAttributes());
        }
        assertEquals("XXXX", line.getText());
    }

    @Test
    void clearResetsAllCellsToEmpty() {
        TerminalLine line = new TerminalLine(5);
        line.setCell(0, new Cell('A', TextAttributes.DEFAULT));
        line.setCell(1, new Cell('B', TextAttributes.DEFAULT));

        line.clear();

        for (int i = 0; i < 5; i++) {
            assertEquals(Cell.EMPTY, line.getCell(i));
        }
        assertEquals("", line.getText());
    }

    @Test
    void copyCreatesIndependentCopy() {
        TerminalLine original = new TerminalLine(5);
        original.setCell(0, new Cell('A', TextAttributes.DEFAULT));

        TerminalLine copy = original.copy();

        assertEquals(original.getCell(0), copy.getCell(0));
        assertEquals(original.getWidth(), copy.getWidth());

        // Mutating the copy should not affect the original
        copy.setCell(0, new Cell('Z', TextAttributes.DEFAULT));
        assertEquals('A', original.getCell(0).getCharacter());
        assertEquals('Z', copy.getCell(0).getCharacter());
    }

    @Test
    void resizeShrinksTruncatesCells() {
        TerminalLine line = new TerminalLine(5);
        line.setCell(0, new Cell('A', TextAttributes.DEFAULT));
        line.setCell(4, new Cell('E', TextAttributes.DEFAULT));

        line.resize(3);

        assertEquals(3, line.getWidth());
        assertEquals('A', line.getCell(0).getCharacter());
        assertThrows(IndexOutOfBoundsException.class, () -> line.getCell(4));
    }

    @Test
    void resizeGrowExtendsWithEmptyCells() {
        TerminalLine line = new TerminalLine(3);
        line.setCell(0, new Cell('A', TextAttributes.DEFAULT));

        line.resize(6);

        assertEquals(6, line.getWidth());
        assertEquals('A', line.getCell(0).getCharacter());
        assertEquals(Cell.EMPTY, line.getCell(3));
        assertEquals(Cell.EMPTY, line.getCell(5));
    }

    @Test
    void resizeToSameWidthDoesNothing() {
        TerminalLine line = new TerminalLine(5);
        line.setCell(0, new Cell('A', TextAttributes.DEFAULT));

        line.resize(5);

        assertEquals(5, line.getWidth());
        assertEquals('A', line.getCell(0).getCharacter());
    }

    @Test
    void resizeRejectsInvalidWidth() {
        TerminalLine line = new TerminalLine(5);
        assertThrows(IllegalArgumentException.class, () -> line.resize(0));
        assertThrows(IllegalArgumentException.class, () -> line.resize(-1));
    }

    @Test
    void widthOneLineWorks() {
        TerminalLine line = new TerminalLine(1);
        assertEquals(1, line.getWidth());
        assertEquals("", line.getText());

        line.setCell(0, new Cell('X', TextAttributes.DEFAULT));
        assertEquals("X", line.getText());
    }
}
