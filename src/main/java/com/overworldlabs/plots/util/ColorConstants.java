package com.overworldlabs.plots.util;

import javax.annotation.Nonnull;

/**
 * Constants for chat colors used throughout the plugin.
 * Uses hex codes compatible with ChatUtil's {#RRGGBB} format.
 * 
 * Themes:
 * - Primary: Blue (#3498db)
 * - Success: Green (#2ecc71)
 * - Error: Red (#e74c3c)
 */
public final class ColorConstants {

    // Theme Colors
    @Nonnull
    public static final String PRIMARY = "{#3498db}"; // Blue
    @Nonnull
    public static final String SECONDARY = "{#95a5a6}"; // Gray
    @Nonnull
    public static final String ACCENT = "{#9b59b6}"; // Purple

    // Status Colors
    @Nonnull
    public static final String SUCCESS = "{#2ecc71}"; // Green
    @Nonnull
    public static final String ERROR = "{#e74c3c}"; // Red
    @Nonnull
    public static final String WARNING = "{#f1c40f}"; // Yellow
    @Nonnull
    public static final String INFO = "{#3498db}"; // Blue

    // Neutral Colors
    @Nonnull
    public static final String WHITE = "{#ffffff}";
    @Nonnull
    public static final String BLACK = "{#000000}";
    @Nonnull
    public static final String GRAY = "{#7f8c8d}";

    // Plot Branding
    @Nonnull
    public static final String PLOT_NAME = "{#1abc9c}"; // Turquoise
    @Nonnull
    public static final String OWNER_NAME = "{#e67e22}"; // Orange
    @Nonnull
    public static final String BORDER = "{#55ffff}"; // Cyan

    private ColorConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
