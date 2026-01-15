package com.overworldlabs.plots.util;

import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.model.PlotConfig;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility methods for plot operations
 */
public class PlotUtil {

    /**
     * Format plot information for display
     */
    @Nonnull
    public static String formatPlotInfo(@Nonnull Plot plot, @Nonnull PlotConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Plot Info ===\n");
        sb.append("Grid: (").append(plot.getGridX()).append(", ").append(plot.getGridZ()).append(")\n");
        sb.append("World: X[").append(plot.getMinX(config)).append(" to ").append(plot.getMaxX(config) - 1)
                .append("] Z[").append(plot.getMinZ(config)).append(" to ").append(plot.getMaxZ(config) - 1)
                .append("]\n");
        sb.append("Owner: ").append(plot.getOwner()).append("\n");
        sb.append("Trusted Players: ").append(plot.getTrustedPlayers().size()).append("\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sb.append("Created: ").append(sdf.format(new Date(plot.getCreatedAt())));

        return sb.toString();
    }

    /**
     * Format a simple plot location string
     */
    @Nonnull
    public static String formatPlotLocation(int gridX, int gridZ) {
        return "Plot (" + gridX + ", " + gridZ + ")";
    }

    /**
     * Check if coordinates are within plot boundaries
     */
    public static boolean isInPlotBounds(int x, int z, @Nonnull Plot plot, @Nonnull PlotConfig config) {
        return x >= plot.getMinX(config) && x < plot.getMaxX(config) &&
                z >= plot.getMinZ(config) && z < plot.getMaxZ(config);
    }

    /**
     * Calculate the spawn Y coordinate (default ground level)
     */
    public static int getSpawnY() {
        return 64; // Default ground level
    }
}






