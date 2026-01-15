package com.overworldlabs.plots.util;

import javax.annotation.Nonnull;

/**
 * Utility class for colorized console output using ANSI escape codes.
 * Provides methods for printing colored messages to the terminal/console.
 */
public final class ConsoleColors {

    // Reset
    public static final String RESET = "\u001B[0m";

    // Regular Colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bold
    public static final String BOLD = "\u001B[1m";
    public static final String BLACK_BOLD = "\u001B[1;30m";
    public static final String RED_BOLD = "\u001B[1;31m";
    public static final String GREEN_BOLD = "\u001B[1;32m";
    public static final String YELLOW_BOLD = "\u001B[1;33m";
    public static final String BLUE_BOLD = "\u001B[1;34m";
    public static final String PURPLE_BOLD = "\u001B[1;35m";
    public static final String CYAN_BOLD = "\u001B[1;36m";
    public static final String WHITE_BOLD = "\u001B[1;37m";

    // Bright
    public static final String BLACK_BRIGHT = "\u001B[90m";
    public static final String RED_BRIGHT = "\u001B[91m";
    public static final String GREEN_BRIGHT = "\u001B[92m";
    public static final String YELLOW_BRIGHT = "\u001B[93m";
    public static final String BLUE_BRIGHT = "\u001B[94m";
    public static final String PURPLE_BRIGHT = "\u001B[95m";
    public static final String CYAN_BRIGHT = "\u001B[96m";
    public static final String WHITE_BRIGHT = "\u001B[97m";

    // Background
    public static final String BLACK_BG = "\u001B[40m";
    public static final String RED_BG = "\u001B[41m";
    public static final String GREEN_BG = "\u001B[42m";
    public static final String YELLOW_BG = "\u001B[43m";
    public static final String BLUE_BG = "\u001B[44m";
    public static final String PURPLE_BG = "\u001B[45m";
    public static final String CYAN_BG = "\u001B[46m";
    public static final String WHITE_BG = "\u001B[47m";

    // Plugin specific colors
    public static final String PLUGIN_PREFIX = CYAN_BOLD + "[Plots] " + RESET;
    public static final String SUCCESS = GREEN_BRIGHT;
    public static final String ERROR = RED_BRIGHT;
    public static final String WARNING = YELLOW_BRIGHT;
    public static final String INFO = BLUE_BRIGHT;

    private ConsoleColors() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Prints a success message to the console with a green checkmark.
     * <p>
     * Output format: [Plots] ✓ message
     * </p>
     *
     * @param message The success message to display
     */
    public static void success(@Nonnull String message) {
        System.out.println(PLUGIN_PREFIX + SUCCESS + "✓ " + message + RESET);
    }

    /**
     * Prints an error message to the console (stderr) with a red X.
     * <p>
     * Output format: [Plots] ✗ message
     * </p>
     *
     * @param message The error message to display
     */
    public static void error(@Nonnull String message) {
        System.err.println(PLUGIN_PREFIX + ERROR + "✗ " + message + RESET);
    }

    /**
     * Prints a warning message to the console with a yellow warning symbol.
     * <p>
     * Output format: [Plots] ⚠ message
     * </p>
     *
     * @param message The warning message to display
     */
    public static void warning(@Nonnull String message) {
        System.out.println(PLUGIN_PREFIX + WARNING + "⚠ " + message + RESET);
    }

    /**
     * Prints an informational message to the console with a blue info symbol.
     * <p>
     * Output format: [Plots] ℹ message
     * </p>
     *
     * @param message The informational message to display
     */
    public static void info(@Nonnull String message) {
        System.out.println(PLUGIN_PREFIX + INFO + "ℹ " + message + RESET);
    }

    /**
     * Prints a debug message to the console with a gray gear symbol.
     * <p>
     * Output format: [Plots] ⚙ message
     * </p>
     *
     * @param message The debug message to display
     */
    public static void debug(@Nonnull String message) {
        System.out.println(PLUGIN_PREFIX + BLACK_BRIGHT + "⚙ " + message + RESET);
    }

    /**
     * Prints a custom colored message to the console.
     * <p>
     * Output format: [Plots] message (in specified color)
     * </p>
     *
     * @param color   The ANSI color code to use (e.g., ConsoleColors.RED)
     * @param message The message to display
     */
    public static void print(@Nonnull String color, @Nonnull String message) {
        System.out.println(PLUGIN_PREFIX + color + message + RESET);
    }

    /**
     * Colorizes a string with the specified color without printing it.
     * <p>
     * This method wraps the text with ANSI color codes and automatically resets
     * the color at the end.
     * </p>
     *
     * @param color The ANSI color code to use (e.g., ConsoleColors.GREEN)
     * @param text  The text to colorize
     * @return The colorized string with ANSI codes
     */
    @Nonnull
    public static String colorize(@Nonnull String color, @Nonnull String text) {
        return color + text + RESET;
    }
}
