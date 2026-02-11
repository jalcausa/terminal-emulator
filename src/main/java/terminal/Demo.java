package terminal;

/**
 * Quick demo showing how to use the TerminalBuffer API.
 * Run: mvn compile exec:java -Dexec.mainClass=terminal.Demo
 */
public final class Demo {

    public static void main(String[] args) {
        // Create a 40×10 terminal with 100 lines of scrollback
        TerminalBuffer buf = new TerminalBuffer(40, 10, 100);

        // --- Basic writing ---
        buf.writeText("Hello, Terminal!");
        System.out.println("=== After writing 'Hello, Terminal!' ===");
        printScreen(buf);

        // --- Colored text ---
        buf.setCursorPosition(0, 2);
        buf.setForeground(TerminalColor.of(AnsiColor.RED));
        buf.addStyle(StyleFlag.BOLD);
        buf.writeText("This is bold red text");
        buf.resetAttributes();

        buf.setCursorPosition(0, 3);
        buf.setForeground(TerminalColor.of(AnsiColor.GREEN));
        buf.writeText("This is green text");
        buf.resetAttributes();

        System.out.println("\n=== After colored text ===");
        printScreen(buf);

        // --- Insert text (shifts existing content right) ---
        buf.setCursorPosition(0, 2);
        buf.writeText(">>> ");  // overwrite beginning
        System.out.println("\n=== After overwriting with '>>> ' ===");
        printScreen(buf);

        // --- Fill a line ---
        buf.setCursorPosition(0, 5);
        buf.setForeground(TerminalColor.of(AnsiColor.YELLOW));
        buf.fillLine('-');
        buf.resetAttributes();
        System.out.println("\n=== After filling line 5 with dashes ===");
        printScreen(buf);

        // --- Wide characters (CJK) ---
        buf.setCursorPosition(0, 7);
        buf.writeText("Wide: 中文漢字テスト");
        System.out.println("\n=== After writing CJK characters ===");
        printScreen(buf);

        // --- Scrolling ---
        System.out.println("\n=== Writing enough to cause scrolling ===");
        for (int i = 0; i < 12; i++) {
            buf.setCursorPosition(0, 9);
            buf.insertEmptyLineAtBottom();
            buf.writeText("Scroll line " + i);
        }
        printScreen(buf);
        System.out.println("Scrollback lines: " + buf.getScrollbackSize());

        // --- Resize ---
        System.out.println("\n=== After resize to 20x5 ===");
        buf.resize(20, 5);
        printScreen(buf);

        // --- Cursor position ---
        buf.setCursorPosition(3, 2);
        System.out.println("\nCursor at: " + buf.getCursorPosition());

        // --- Inspect attributes ---
        System.out.println("\nAttributes at (0,0): " + buf.getAttributesAt(0, 0));
    }

    private static void printScreen(TerminalBuffer buf) {
        System.out.println("┌" + "─".repeat(buf.getWidth()) + "┐");
        for (int row = 0; row < buf.getHeight(); row++) {
            String line = buf.getLine(row);
            // Pad to width for visual alignment
            String padded = line + " ".repeat(Math.max(0, buf.getWidth() - line.length()));
            System.out.println("│" + padded + "│");
        }
        System.out.println("└" + "─".repeat(buf.getWidth()) + "┘");
    }
}
