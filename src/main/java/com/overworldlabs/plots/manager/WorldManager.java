package com.overworldlabs.plots.manager;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.util.ConsoleColors;
import com.overworldlabs.plots.worldgen.PlotWorldGenProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Manages the plot world creation and access
 */
public class WorldManager implements IWorldManager {
    private final PlotConfig plotConfig;
    private final String worldName;
    private final String defaultTime;

    public WorldManager(@Nonnull PlotConfig config) {
        this.plotConfig = config;
        this.worldName = config.getPlotWorldName();
        this.defaultTime = config.getDefaultWorldTime();
    }

    @Override
    @Nonnull
    public String getWorldName() {
        return worldName;
    }

    @Override
    public boolean isPlotWorld(@Nonnull String worldName) {
        return this.worldName.equals(worldName);
    }

    @Override
    @Nullable
    public World getPlotWorld() {
        return Universe.get().getWorld(worldName);
    }

    public boolean worldExists() {
        return getPlotWorld() != null;
    }

    @Override
    public void createWorldIfNeeded() {
        if (worldExists()) {
            ConsoleColors.info("Plot world '" + worldName + "' already exists");
            return;
        }

        ConsoleColors.info("Creating plot world '" + worldName + "' with time: " + defaultTime);

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
            config.setGameTimePaused(true);
            config.setGameTime(parseTime(defaultTime));

            double intersectionCoordX = -(roadSizeX / 2.0);
            double intersectionCoordZ = -(roadSizeZ / 2.0);

            Transform spawnTransform = new Transform(intersectionCoordX, 66.0, intersectionCoordZ);
            config.setSpawnProvider(new GlobalSpawnProvider(spawnTransform));

            config.markChanged();

            Universe.get()
                    .makeWorld(worldName, java.util.Objects.requireNonNull(worldPath), config)
                    .thenAccept(world -> {
                        if (world != null) {
                            ConsoleColors.success("Successfully created plot world '" + worldName + "'");
                        }
                    })
                    .exceptionally(throwable -> {
                        ConsoleColors.error("Failed to create plot world '" + worldName + "'!");
                        throwable.printStackTrace();
                        return null;
                    });
        } catch (Exception e) {
            ConsoleColors.error("Exception while creating plot world!");
            e.printStackTrace();
        }
    }

    private Instant parseTime(String timeStr) {
        if (timeStr == null)
            return getMidday();

        String normalized = timeStr.toLowerCase().trim();
        Instant base = Instant.parse("0001-01-01T00:00:00.00Z");

        switch (normalized) {
            case "midnight":
                return base;
            case "morning":
                return base.plus(6, ChronoUnit.HOURS);
            case "midday":
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
                    double factor = Double.parseDouble(normalized);
                    return base.plus((long) (factor * 86400 * 1_000_000_000L), ChronoUnit.NANOS);
                } catch (NumberFormatException e) {
                    try {
                        int hour = Integer.parseInt(normalized);
                        return base.plus(hour % 24, ChronoUnit.HOURS);
                    } catch (NumberFormatException e2) {
                        return getMidday();
                    }
                }
        }
    }

    private Instant getMidday() {
        return Instant.parse("0001-01-01T12:00:00.00Z");
    }
}
