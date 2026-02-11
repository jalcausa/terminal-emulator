# Terminal Text Buffer

A terminal text buffer implementation in Java — the core data structure that terminal emulators use to store and manipulate displayed text.

## Building and Testing

```bash
# Build
mvn compile

# Run tests (177 tests)
mvn test

# Run a specific test class
mvn test -Dtest=TerminalBufferTest

# Run a specific nested test group (e.g. Resize)
mvn test -Dtest="TerminalBufferTest\$Resize"
```

**Requirements:** Java 17+, Maven 3.8+. No external dependencies besides JUnit 5 for testing.

## Interactive Demo

An interactive REPL is included to try the buffer from your terminal:

```bash
mvn compile -q && java -cp target/classes terminal.Demo
```

Once running, type commands to interact with the buffer. After each command
the buffer is rendered with ANSI colors and a visible cursor:

```
> write Hello, world!
> fg red
> bold
> cursor 0 2
> write This is bold red
> fill =
> scroll
> resize 20 5
> scrollback
> info
> help        (full command list)
> quit
```

## Architecture

The buffer is modeled as a three-tier structure, each layer building on the one below:

```
TerminalBuffer
├── Screen: ArrayList<TerminalLine>    (visible area, editable)
├── Scrollback: ArrayDeque<TerminalLine> (history, read-only)
├── Cursor: (column, row) position
└── Current Attributes: fg/bg color + styles

TerminalLine
└── Cell[] cells  (fixed-width array)

Cell (immutable)
├── char character
├── TextAttributes (fg, bg, styles)
├── int displayWidth (1 or 2)
└── boolean placeholder (for wide chars)
```

### Core Classes

| Class            | Responsibility                                            |
| ---------------- | --------------------------------------------------------- |
| `Cell`           | Immutable character + attributes + width metadata         |
| `TerminalLine`   | Mutable row of cells; get/set/fill/clear/resize           |
| `TerminalBuffer` | Main API — cursor, attributes, editing, screen/scrollback |
| `TextAttributes` | Immutable fg/bg color + style flags, builder pattern      |
| `TerminalColor`  | Wraps an optional `AnsiColor` (null = default)            |
| `AnsiColor`      | Enum of 16 standard ANSI colors                           |
| `StyleFlag`      | Enum: `BOLD`, `ITALIC`, `UNDERLINE`                       |
| `CursorPosition` | Immutable `(column, row)` value                           |
| `CharWidth`      | Static utility for wide character detection (CJK ranges)  |

## Design Decisions and Trade-Offs

### Immutable value objects vs mutable containers

`Cell`, `TextAttributes`, `TerminalColor`, and `CursorPosition` are immutable. This gives strong guarantees: a cell stored in a line can't be silently mutated from outside, attributes can be shared freely without defensive copies, and value equality is straightforward. The cost is allocation on every character write, but modern JVMs handle short-lived objects efficiently, and this is a data structure exercise, not a hot-path renderer.

`TerminalLine` and `TerminalBuffer` are mutable by nature — they represent state that changes constantly during terminal operation.

### ArrayList for screen, ArrayDeque for scrollback

The **screen** uses `ArrayList<TerminalLine>` because:

- Random access by row index is O(1), which is critical for cursor-addressed editing.
- The number of rows is fixed (equals `height`), so no resizing during normal operations.
- `remove(0)` during scroll-up is O(n) where n = height, but height is typically 24–50. For a production buffer an array-backed ring buffer would eliminate this cost, but the complexity isn't warranted here.

The **scrollback** uses `ArrayDeque<TerminalLine>` because:

- Lines are always added at one end (`addLast`) and evicted from the other (`removeFirst`), making it a natural FIFO/deque.
- No indexed access is needed during normal operation (scrollback is read sequentially).
- The `getScrollbackLineInternal` method iterates the deque for indexed access in content-access APIs, which is O(n) but acceptable for diagnostic/test reads.

### 0-indexed coordinates

All coordinates are 0-based (column 0, row 0 = top-left). This matches array indexing directly, avoids off-by-one errors at the implementation level, and is consistent with how real terminal protocols (ECMA-48) address cells internally (though the wire protocol uses 1-based CSI sequences).

### Cursor clamping

`setCursorPosition` clamps to `[0, width-1]` × `[0, height-1]` rather than throwing. This mirrors real terminal behavior — sending a cursor beyond bounds simply pins it at the edge. The `writeText` method allows the cursor column to temporarily reach `width` (one past the last column) to defer the line-wrap decision until the next character arrives. This avoids premature wrapping when writing exactly `width` characters.

### Insert overflow truncation

`insertText` shifts existing characters right, discarding any that overflow past the line width. This is the standard behavior in terminals (ICH / CSI @ sequence). An alternative would be to wrap overflowed content to the next line, but real terminals don't do this for insert operations.

### Scroll-up pushes to scrollback, not just discards

When the screen scrolls up (during `writeText` wrap, `insertEmptyLineAtBottom`, or height-decrease resize), the top line is pushed into the scrollback deque. If the scrollback exceeds `maxScrollbackSize`, the oldest line is evicted from the front. This preserves history within the configured limit.

### Wide character handling (bonus)

Wide characters (CJK ideographs, fullwidth forms, Hangul) occupy 2 columns. The implementation:

1. **Two-cell model:** A wide character writes a main `Cell` (displayWidth=2) followed by a placeholder cell (displayWidth=1, placeholder=true). This keeps the cell array aligned — every column index maps to exactly one Cell.
2. **Boundary wrapping:** If a wide char would start at the last column (only 1 cell available), the current cell is filled with a space and the wide char wraps to the next line. This avoids split-glyph rendering artifacts.
3. **Overwrite cleanup:** When overwriting a cell that's part of a wide pair (either the main cell or placeholder), both cells are cleared. This prevents orphaned half-glyphs.
4. **Width detection without libraries:** `CharWidth` checks Unicode block ranges (CJK Unified Ideographs, Hangul Syllables, Fullwidth Forms, etc.) directly. A production implementation would use ICU or a UAX #11 East Asian Width table, but inline range checks satisfy the no-external-deps constraint.
5. **Degenerate case:** A 1-column-wide buffer can't fit any wide character. Rather than infinite-looping on wrap attempts, such characters are replaced with a space.

`TerminalLine.getText()` skips placeholder cells, so serialized text output matches the logical character sequence.

### Resize strategy (bonus)

Resize handles width and height independently, width first:

- **Width decrease:** Each line is truncated. If truncation splits a wide character (main cell at the new last column, placeholder beyond), the orphaned main cell is replaced with an empty cell before truncation.
- **Width increase:** Lines are extended with empty cells.
- **Height decrease:** Excess top-of-screen lines are pushed to scrollback (subject to `maxScrollbackSize`). The cursor row is adjusted by the number of lines removed.
- **Height increase:** Lines are recovered from scrollback (most-recently-scrolled-off first) and prepended to the screen. Remaining rows are added as empty lines at the bottom. The cursor row shifts down by the number of recovered lines.
- **Cursor clamped** to new bounds after resize.

This is a **simple truncation** strategy. Real terminals (xterm, Alacritty) use **reflow**: when width decreases, long lines are re-wrapped across multiple rows, and when width increases, wrapped lines are re-joined. Reflow requires tracking which line breaks were "soft" (caused by wrapping) vs "hard" (caused by newline/cursor movement), adding significant complexity. The truncation approach was chosen as a pragmatic trade-off for this exercise.

## Possible Future Improvements

- **256-color / RGB color** — extend `TerminalColor` to support indexed (256) and true-color (24-bit RGB) palettes alongside the current 16-color ANSI set.
- **Line reflow on resize** — track soft vs hard line breaks to re-wrap content when width changes, instead of truncating.
- **Alternate screen buffer** — support switching between primary and alternate screen buffers (used by vim, less, etc.).
- **Scroll regions** — support for DECSTBM (set top/bottom margins) to enable partial-screen scrolling.
- **Unicode combining characters** — allow multiple code points per cell for accents, diacritics, and emoji modifiers.
- **Emoji and ZWJ sequences** — handle multi-code-point emoji (family, flags) that occupy 2 columns but span multiple `char` values (surrogate pairs + ZWJ).
- **Damage tracking** — track which cells changed since the last render pass to support efficient partial redraws.
- **Tab stops** — horizontal tab stop management and HT/CHT handling.
- **Ring buffer for screen** — replace `ArrayList` + `remove(0)` scrolling with a circular buffer for O(1) scroll-up.
