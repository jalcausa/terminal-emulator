package terminal;

import java.util.Scanner;

/**
 * Interactive terminal buffer demo.
 * Run: mvn compile -q && java -cp target/classes terminal.Demo
 *
 * Type commands to interact with the buffer and see the result rendered live.
 */
public final class Demo {

    private static TerminalBuffer buf;

    public static void main(String[] args) {
        buf = new TerminalBuffer(40, 10, 100);

        Scanner scanner = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     Terminal Buffer — Interactive Demo   ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  write <text>         Write text at cursor (overwrites)");
        System.out.println("  insert <text>        Insert text at cursor (shifts right)");
        System.out.println("  cursor <col> <row>   Move cursor to position");
        System.out.println("  up/down/left/right <n>  Move cursor by n cells");
        System.out.println("  fill <char>          Fill current line with character");
        System.out.println("  clear                Clear screen");
        System.out.println("  clearall             Clear screen and scrollback");
        System.out.println("  scroll               Scroll screen up (insert empty line)");
        System.out.println("  fg <color>           Set foreground (red, green, blue, yellow,");
        System.out.println("                       cyan, magenta, white, black, default)");
        System.out.println("  bg <color>           Set background color");
        System.out.println("  bold / italic / underline   Toggle style on");
        System.out.println("  nobold / noitalic / nounderline  Toggle style off");
        System.out.println("  reset                Reset attributes to defaults");
        System.out.println("  resize <w> <h>       Resize buffer");
        System.out.println("  scrollback           Show scrollback content");
        System.out.println("  info                 Show buffer info (dimensions, cursor, attrs)");
        System.out.println("  help                 Show this help");
        System.out.println("  quit                 Exit");
        System.out.println();

        printScreen();

        while (true) {
            System.out.print("\n> ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            try {
                if (!handleCommand(input)) break;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("Bye!");
    }

    private static boolean handleCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "write" -> {
                buf.writeText(arg);
                printScreen();
            }
            case "insert" -> {
                buf.insertText(arg);
                printScreen();
            }
            case "cursor" -> {
                String[] coords = arg.split("\\s+");
                if (coords.length != 2) {
                    System.out.println("Usage: cursor <col> <row>");
                    return true;
                }
                buf.setCursorPosition(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                printScreen();
            }
            case "up" -> { buf.moveCursorUp(parseIntOrOne(arg)); printScreen(); }
            case "down" -> { buf.moveCursorDown(parseIntOrOne(arg)); printScreen(); }
            case "left" -> { buf.moveCursorLeft(parseIntOrOne(arg)); printScreen(); }
            case "right" -> { buf.moveCursorRight(parseIntOrOne(arg)); printScreen(); }
            case "fill" -> {
                char c = arg.isEmpty() ? ' ' : arg.charAt(0);
                buf.fillLine(c);
                printScreen();
            }
            case "clear" -> { buf.clearScreen(); printScreen(); }
            case "clearall" -> { buf.clearScreenAndScrollback(); printScreen(); }
            case "scroll" -> { buf.insertEmptyLineAtBottom(); printScreen(); }
            case "fg" -> { buf.setForeground(parseColor(arg)); printAttrs(); }
            case "bg" -> { buf.setBackground(parseColor(arg)); printAttrs(); }
            case "bold" -> { buf.addStyle(StyleFlag.BOLD); printAttrs(); }
            case "italic" -> { buf.addStyle(StyleFlag.ITALIC); printAttrs(); }
            case "underline" -> { buf.addStyle(StyleFlag.UNDERLINE); printAttrs(); }
            case "nobold" -> { buf.removeStyle(StyleFlag.BOLD); printAttrs(); }
            case "noitalic" -> { buf.removeStyle(StyleFlag.ITALIC); printAttrs(); }
            case "nounderline" -> { buf.removeStyle(StyleFlag.UNDERLINE); printAttrs(); }
            case "reset" -> { buf.resetAttributes(); printAttrs(); }
            case "resize" -> {
                String[] dims = arg.split("\\s+");
                if (dims.length != 2) {
                    System.out.println("Usage: resize <width> <height>");
                    return true;
                }
                buf.resize(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
                printScreen();
            }
            case "scrollback" -> printScrollback();
            case "info" -> printInfo();
            case "help" -> printHelp();
            case "quit", "exit", "q" -> { return false; }
            default -> System.out.println("Unknown command: " + cmd + " (type 'help')");
        }
        return true;
    }

    private static int parseIntOrOne(String s) {
        if (s.isEmpty()) return 1;
        return Integer.parseInt(s);
    }

    private static TerminalColor parseColor(String name) {
        return switch (name.toLowerCase().trim()) {
            case "black" -> TerminalColor.of(AnsiColor.BLACK);
            case "red" -> TerminalColor.of(AnsiColor.RED);
            case "green" -> TerminalColor.of(AnsiColor.GREEN);
            case "yellow" -> TerminalColor.of(AnsiColor.YELLOW);
            case "blue" -> TerminalColor.of(AnsiColor.BLUE);
            case "magenta" -> TerminalColor.of(AnsiColor.MAGENTA);
            case "cyan" -> TerminalColor.of(AnsiColor.CYAN);
            case "white" -> TerminalColor.of(AnsiColor.WHITE);
            case "bright_black" -> TerminalColor.of(AnsiColor.BRIGHT_BLACK);
            case "bright_red" -> TerminalColor.of(AnsiColor.BRIGHT_RED);
            case "bright_green" -> TerminalColor.of(AnsiColor.BRIGHT_GREEN);
            case "bright_yellow" -> TerminalColor.of(AnsiColor.BRIGHT_YELLOW);
            case "bright_blue" -> TerminalColor.of(AnsiColor.BRIGHT_BLUE);
            case "bright_magenta" -> TerminalColor.of(AnsiColor.BRIGHT_MAGENTA);
            case "bright_cyan" -> TerminalColor.of(AnsiColor.BRIGHT_CYAN);
            case "bright_white" -> TerminalColor.of(AnsiColor.BRIGHT_WHITE);
            case "default", "" -> TerminalColor.defaultColor();
            default -> {
                System.out.println("Unknown color: " + name +
                        " (use: black, red, green, yellow, blue, magenta, cyan, white, default)");
                yield buf.getCurrentAttributes().getForeground();
            }
        };
    }

    // ── Rendering ──────────────────────────────────────────

    private static void printScreen() {
        CursorPosition cursor = buf.getCursorPosition();
        System.out.println("┌" + "─".repeat(buf.getWidth()) + "┐");
        for (int row = 0; row < buf.getHeight(); row++) {
            System.out.print("│");
            for (int col = 0; col < buf.getWidth(); col++) {
                Cell cell = buf.getCellAt(col, row);
                if (cell.isPlaceholder()) continue; // skip wide-char trailing cell

                String ch = cell.getCharacter() == ' ' && col == cursor.getColumn() && row == cursor.getRow()
                        ? "▋" : String.valueOf(cell.getCharacter());

                // Apply ANSI escape for color/style
                String ansi = cellAnsi(cell);
                if (!ansi.isEmpty()) {
                    System.out.print("\033[" + ansi + "m" + ch + "\033[0m");
                } else if (col == cursor.getColumn() && row == cursor.getRow()) {
                    System.out.print("\033[7m" + ch + "\033[0m"); // invert for cursor
                } else {
                    System.out.print(ch);
                }
            }
            System.out.println("│");
        }
        System.out.println("└" + "─".repeat(buf.getWidth()) + "┘");
        System.out.printf("Cursor: (%d, %d)  Size: %dx%d  Scrollback: %d lines%n",
                cursor.getColumn(), cursor.getRow(), buf.getWidth(), buf.getHeight(), buf.getScrollbackSize());
    }

    private static String cellAnsi(Cell cell) {
        TextAttributes attrs = cell.getAttributes();
        StringBuilder sb = new StringBuilder();

        TerminalColor fg = attrs.getForeground();
        if (fg.getAnsiColor() != null) {
            sb.append(ansiFgCode(fg.getAnsiColor()));
        }
        TerminalColor bg = attrs.getBackground();
        if (bg.getAnsiColor() != null) {
            if (!sb.isEmpty()) sb.append(';');
            sb.append(ansiBgCode(bg.getAnsiColor()));
        }
        if (attrs.hasStyle(StyleFlag.BOLD)) {
            if (!sb.isEmpty()) sb.append(';');
            sb.append('1');
        }
        if (attrs.hasStyle(StyleFlag.ITALIC)) {
            if (!sb.isEmpty()) sb.append(';');
            sb.append('3');
        }
        if (attrs.hasStyle(StyleFlag.UNDERLINE)) {
            if (!sb.isEmpty()) sb.append(';');
            sb.append('4');
        }
        return sb.toString();
    }

    private static int ansiFgCode(AnsiColor c) {
        return switch (c) {
            case BLACK -> 30; case RED -> 31; case GREEN -> 32; case YELLOW -> 33;
            case BLUE -> 34; case MAGENTA -> 35; case CYAN -> 36; case WHITE -> 37;
            case BRIGHT_BLACK -> 90; case BRIGHT_RED -> 91; case BRIGHT_GREEN -> 92;
            case BRIGHT_YELLOW -> 93; case BRIGHT_BLUE -> 94; case BRIGHT_MAGENTA -> 95;
            case BRIGHT_CYAN -> 96; case BRIGHT_WHITE -> 97;
        };
    }

    private static int ansiBgCode(AnsiColor c) {
        return ansiFgCode(c) + 10;
    }

    private static void printScrollback() {
        int size = buf.getScrollbackSize();
        if (size == 0) {
            System.out.println("(scrollback is empty)");
            return;
        }
        System.out.println("╔═ Scrollback (" + size + " lines) " + "═".repeat(Math.max(0, buf.getWidth() - 20)) + "╗");
        for (int i = 0; i < size; i++) {
            String line = buf.getScrollbackLine(i);
            String padded = line + " ".repeat(Math.max(0, buf.getWidth() - line.length()));
            System.out.println("║" + padded + "║");
        }
        System.out.println("╚" + "═".repeat(buf.getWidth()) + "╝");
    }

    private static void printInfo() {
        CursorPosition c = buf.getCursorPosition();
        TextAttributes a = buf.getCurrentAttributes();
        System.out.println("Size:       " + buf.getWidth() + "x" + buf.getHeight());
        System.out.println("Cursor:     (" + c.getColumn() + ", " + c.getRow() + ")");
        System.out.println("Scrollback: " + buf.getScrollbackSize() + " / " + buf.getMaxScrollbackSize());
        System.out.println("Foreground: " + (a.getForeground().getAnsiColor() != null ? a.getForeground().getAnsiColor() : "default"));
        System.out.println("Background: " + (a.getBackground().getAnsiColor() != null ? a.getBackground().getAnsiColor() : "default"));
        System.out.println("Styles:     " + a.getStyles());
    }

    private static void printAttrs() {
        TextAttributes a = buf.getCurrentAttributes();
        System.out.println("Attributes: fg=" +
                (a.getForeground().getAnsiColor() != null ? a.getForeground().getAnsiColor() : "default") +
                " bg=" +
                (a.getBackground().getAnsiColor() != null ? a.getBackground().getAnsiColor() : "default") +
                " styles=" + a.getStyles());
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  write <text>         Write text at cursor (overwrites)");
        System.out.println("  insert <text>        Insert text at cursor (shifts right)");
        System.out.println("  cursor <col> <row>   Move cursor to position");
        System.out.println("  up/down/left/right <n>  Move cursor by n cells");
        System.out.println("  fill <char>          Fill current line with character");
        System.out.println("  clear                Clear screen");
        System.out.println("  clearall             Clear screen and scrollback");
        System.out.println("  scroll               Scroll screen up (insert empty line)");
        System.out.println("  fg <color>           Set foreground color");
        System.out.println("  bg <color>           Set background color");
        System.out.println("  bold / italic / underline   Toggle style on");
        System.out.println("  nobold / noitalic / nounderline  Toggle style off");
        System.out.println("  reset                Reset attributes to defaults");
        System.out.println("  resize <w> <h>       Resize buffer");
        System.out.println("  scrollback           Show scrollback content");
        System.out.println("  info                 Show buffer info");
        System.out.println("  help                 Show this help");
        System.out.println("  quit                 Exit");
    }
}
