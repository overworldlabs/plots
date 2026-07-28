package dev.stoshe.plots.util;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin console output. Normal messages go through the server's native {@link Logger} (named "Plots"),
 * which already colours them by level. Only the one-off boot banner is written raw (bypassing the logger)
 * so its art isn't broken up by the log prefix.
 *
 * <p>Same shape as the other Stoshe plugins (AeroWars, AntiXray). Note that neither path is mirrored into
 * {@code logs/*_server.log}: the server only captures {@link System#out}, so plugin output lives in the
 * terminal alone.
 */
public final class Console {
    private static final Logger LOGGER = Logger.getLogger("Plots");
    /** Direct handle to the real terminal (fd 1), used only for the banner. */
    private static final PrintStream OUT = new PrintStream(new FileOutputStream(FileDescriptor.out), true);
    private static final String ESC = ((char) 27) + "[";
    private static final String RESET = ESC + "0m";

    private Console() {
    }

    public static void info(String msg) {
        LOGGER.info(msg);
    }

    public static void success(String msg) {
        LOGGER.info(msg);
    }

    public static void warning(String msg) {
        LOGGER.warning(msg);
    }

    public static void error(String msg) {
        LOGGER.severe(msg);
    }

    /** Error plus its stack trace, formatted by the logger rather than printed to System.err. */
    public static void error(String msg, Throwable t) {
        LOGGER.log(Level.SEVERE, msg, t);
    }

    /** Raw line straight to the terminal (no log prefix) — banner spacing. */
    public static void log(String msg) {
        OUT.println(msg);
    }

    /** High-visibility boxed banner straight to the terminal in a single 256-colour. */
    public static void banner(int color256, String... lines) {
        int width = 0;
        for (String l : lines) {
            width = Math.max(width, l == null ? 0 : l.length());
        }
        String c = ESC + "38;5;" + color256 + "m";
        String rule = "═".repeat(width + 2);
        OUT.println();
        OUT.println(c + "╔" + rule + "╗");
        for (String l : lines) {
            String s = l == null ? "" : l;
            OUT.println("║ " + s + " ".repeat(width - s.length()) + " ║");
        }
        OUT.println("╚" + rule + "╝" + RESET);
        OUT.println();
    }
}
