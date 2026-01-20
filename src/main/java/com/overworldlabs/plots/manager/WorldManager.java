package com.overworldlabs.plots.manager;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import com.overworldlabs.plots.model.PlotConfig;
import com.overworldlabs.plots.worldgen.PlotWorldGenProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Manages the plot world creation and access
 */
public class WorldManager {
    private final PlotConfig plotConfig;
    private final String worldName;
    private final String defaultTime;

    public WorldManager(@Nonnull PlotConfig config) {
        this.plotConfig = config;
        this.worldName = config.getPlotWorldName();
        this.defaultTime = config.getDefaultWorldTime();
    }

    /**
     * Get the plot world name
     */
    @Nonnull
    public String getWorldName() {
        return worldName;
    }

    /**
     * Get the plot world if it exists
     */
    @Nullable
    public World getPlotWorld() {
        return Universe.get().getWorld(worldName);
    }

    /**
     * Check if the plot world exists
     */
    public boolean worldExists() {
        return getPlotWorld() != null;
    }

    /**
     * Create the plot world if it doesn't exist
     */
    public void createWorldIfNeeded() {
        if (worldExists()) {
            System.out.println("[Plots] Plot world '" + worldName + "' already exists");
            return;
        }

        System.out.println("[Plots] Creating plot world '" + worldName + "' with time: " + defaultTime);

        try {
            Path universePath = Universe.get().getPath();
            Path worldPath = universePath.resolve("worlds").resolve(worldName);

            WorldConfig config = new WorldConfig();
            config.setDisplayName(worldName);

            int plotSizeX = plotConfig.getPlotSizeX();
            int plotSizeZ = plotConfig.getPlotSizeZ();
            int roadSizeX = plotConfig.getRoadSizeX();
            int roadSizeZ = plotConfig.getRoadSizeZ();

            PlotWorldGenProvider plotGen = new PlotWorldGenProvider(plotSizeX, plotSizeZ, roadSizeX, roadSizeZ);
            config.setWorldGenProvider(plotGen);

            config.setTicking(true);
            config.setBlockTicking(true);
            config.setPvpEnabled(false);
            config.setSpawningNPC(false);

            // Keep time permanent
            config.setGameTimePaused(true);

            // Set the world time
            config.setGameTime(parseTime(defaultTime));

            // Default spawn: at the center of the road intersection at origin
            // Roads are between plots, so the intersection at (0,0) is actually at negative
            // coordinates
            // The road before plot (0,0) starts at -(roadSize) and goes to 0
            // So the center of the intersection is at -(roadSize/2)
            double intersectionCoordX = -(roadSizeX / 2.0);
            double intersectionCoordZ = -(roadSizeZ / 2.0);

            Transform spawnTransform = new Transform(intersectionCoordX, 66.0, intersectionCoordZ);
            config.setSpawnProvider(new GlobalSpawnProvider(spawnTransform));

            config.markChanged();

            Universe.get()
                    .makeWorld(worldName, java.util.Objects.requireNonNull(worldPath), config)
                    .thenAccept(world -> {
                        if (world != null) {
                            System.out.println("[Plots] Successfully created plot world '" + worldName + "'");
                        }
                    })
                    .exceptionally(throwable -> {
                        System.err.println("[Plots] ERROR: Failed to create plot world '" + worldName + "'!");
                        throwable.printStackTrace();
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("[Plots] ERROR: Exception while creating plot world!");
            e.printStackTrace();
        }
    }

    /**
     * Parse time string to Hytale game instant
     */
    private Instant parseTime(String timeStr) {
        if (timeStr == null)
            return getMidday();

        String normalized = timeStr.toLowerCase().trim();

        // Zero point for Hytale time seems to be Year 1
        Instant base = Instant.parse("0001-01-01T00:00:00.00Z");

        switch (normalized) {
            case "midnight":
                return base;
            case "morning":
                return base.plus(6, ChronoUnit.HOURS);
            case "midday":
                return base.plus(12, ChronoUnit.HOURS);
            case "noon":
                return base.plus(12, ChronoUnit.HOURS);
            case "afternoon":
                return base.plus(15, ChronoUnit.HOURS);
            case "night":
                return base.plus(20, ChronoUnit.HOURS);
            case "dawn":
                return base.plus(5, ChronoUnit.HOURS);
            case "dusk":
                return base.plus(19, ChronoUnit.HOURS);
            default:
                try {
                    // Try to parse as double (0.5 = midday)
                    double factor = Double.parseDouble(normalized);
                    return base.plus((long) (factor * 86400 * 1_000_000_000L), ChronoUnit.NANOS);
                } catch (NumberFormatException e) {
                    // Try to parse as integer hour
                    try {
                        int hour = Integer.parseInt(normalized);
                        return base.plus(hour % 24, ChronoUnit.HOURS);
                    } catch (NumberFormatException e2) {
                        return getMidday(); // Fallback
                    }
                }
        }
    }

    private Instant getMidday() {
        return Instant.parse("0001-01-01T12:00:00.00Z");
    }
}
