package dev.stoshe.plots.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Plugin console output, matching the other Stoshe plugins (AeroWars, AntiXray, Voltis):
 * level-prefixed lines for normal messages and a boxed, coloured banner for the boot header
 * and other one-off notices.
 *
 * <p>Everything goes through {@link System#out} — <b>not</b> {@link java.util.logging.Logger}
 * and not a fresh stream on {@link java.io.FileDescriptor}{@code .out}. The server replaces
 * {@code System.out} with a capturing stream that mirrors every line into
 * {@code logs/*_server.log} as {@code [SOUT]}. Logger lines and streams opened straight on
 * fd 1 both escape that capture, so they reach the terminal and nothing else — a diagnostic
 * you cannot read afterwards is worse than none, because you trust its silence.
 */
public final class Console {
    private static final String ESC = ((char) 27) + "[";
    private static final String RESET = ESC + "0m";

    private Console() {
    }

    public static void info(String msg) {
        out("INFO", msg);
    }

    public static void success(String msg) {
        out("INFO", msg);
    }

    public static void warning(String msg) {
        out("WARN", msg);
    }

    public static void error(String msg) {
        out("ERROR", msg);
    }

    /**
     * Error plus its stack trace. The trace goes through {@link System#out} as well —
     * {@code printStackTrace()} writes to {@code System.err}, which the server does not
     * mirror into the log file, so those traces only ever existed in the terminal.
     */
    public static void error(String msg, Throwable t) {
        out("ERROR", msg);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            System.out.print(sw);
        }
    }

    private static void out(String level, String msg) {
        System.out.println("[Plots/" + level + "] " + msg);
    }

    /** Raw line, no level prefix — captured by the server into the log file as {@code [SOUT]}. */
    public static void log(String msg) {
        System.out.println(msg);
    }

    /** High-visibility boxed banner in a single 256-colour. Lines must be plain text so the box aligns. */
    public static void banner(int color256, String... lines) {
        int width = 0;
        for (String l : lines) {
            width = Math.max(width, l == null ? 0 : l.length());
        }
        String c = ESC + "38;5;" + color256 + "m";
        String rule = "═".repeat(width + 2);
        System.out.println();
        System.out.println(c + "╔" + rule + "╗");
        for (String l : lines) {
            String s = l == null ? "" : l;
            System.out.println("║ " + s + " ".repeat(width - s.length()) + " ║");
        }
        System.out.println("╚" + rule + "╝" + RESET);
        System.out.println();
    }
}
