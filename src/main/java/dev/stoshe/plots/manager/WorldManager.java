package dev.stoshe.plots.manager;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import dev.stoshe.plots.api.IWorldManager;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.util.Console;
import dev.stoshe.plots.worldgen.PlotWorldGenProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Creates and accesses the plugin's plot worlds. Each managed world is generated
 * from its own {@link PlotConfig.WorldEntry} (sizes, blocks, prefabs, spawn).
 */
public class WorldManager implements IWorldManager {

    /** Y coordinate the world spawn is placed at. */
    private static final double SPAWN_Y = 66.0;

    private final PlotConfig plotConfig;

    /**
     * @param config the plugin config providing the managed worlds
     */
    public WorldManager(@Nonnull PlotConfig config) {
        this.plotConfig = config;
    }

    @Override
    public String getWorldName() {
        return plotConfig.getDefaultWorldName();
    }

    @Override
    @Nonnull
    public List<String> getManagedWorldNames() {
        return plotConfig.getWorldNames();
    }

    @Override
    public boolean isPlotWorld(@Nonnull String worldName) {
        return plotConfig.isManagedWorld(worldName);
    }

    @Override
    public boolean hasLoadedPlotWorld() {
        for (String name : getManagedWorldNames()) {
            if (getPlotWorld(name) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public World getPlotWorld() {
        return getPlotWorld(getWorldName());
    }

    @Override
    @Nullable
    public World getPlotWorld(@Nonnull String worldName) {
        return Universe.get().getWorld(worldName);
    }

    @Override
    public void createWorldsIfNeeded() {
        // Iterate a snapshot of the names: createWorld can mutate the live Worlds map.
        for (String name : plotConfig.getWorldNames()) {
            if (getPlotWorld(name) != null) {
                Console.info("Plot world '" + name + "' already exists");
                continue;
            }
            createWorld(name, plotConfig.world(name));
        }
    }

    @Override
    @Nonnull
    public CompletableFuture<World> createWorld(@Nonnull String name, @Nonnull PlotConfig.WorldEntry entry) {
        World existing = getPlotWorld(name);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        String defaultTime = entry.getDefaultWorldTime();
        Console.info("Creating plot world '" + name + "' with time: " + defaultTime);

        try {
            Path worldPath = Universe.get().getPath().resolve("worlds").resolve(name);

            int plotSizeX = entry.getPlotSizeX();
            int plotSizeZ = entry.getPlotSizeZ();
            int roadSizeX = entry.getRoadSizeX();
            int roadSizeZ = entry.getRoadSizeZ();

            WorldConfig config = new WorldConfig();
            config.setDisplayName(name);
            config.setWorldGenProvider(new PlotWorldGenProvider(name, plotSizeX, plotSizeZ, roadSizeX, roadSizeZ));
            config.setTicking(true);
            config.setBlockTicking(true);
            config.setPvpEnabled(false);
            config.setSpawningNPC(false);
            config.setGameTimePaused(true);
            config.setGameTime(parseTime(defaultTime));
            config.setSpawnProvider(new GlobalSpawnProvider(buildSpawnTransform(roadSizeX, roadSizeZ)));
            config.markChanged();

            return Universe.get()
                    .makeWorld(name, Objects.requireNonNull(worldPath), config)
                    .whenComplete((world, throwable) -> {
                        if (throwable != null) {
                            Console.error("Failed to create plot world '" + name + "'!", throwable);
                        } else if (world != null) {
                            Console.success("Successfully created plot world '" + name + "'");
                        }
                    });
        } catch (Exception e) {
            Console.error("Exception while creating plot world '" + name + "'!", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Builds the world spawn transform, centred on the origin intersection.
     *
     * @param roadSizeX the world's road width on the X axis
     * @param roadSizeZ the world's road width on the Z axis
     * @return the spawn transform
     */
    private Transform buildSpawnTransform(int roadSizeX, int roadSizeZ) {
        double intersectionCoordX = -(roadSizeX / 2.0);
        double intersectionCoordZ = -(roadSizeZ / 2.0);
        return new Transform(intersectionCoordX, SPAWN_Y, intersectionCoordZ);
    }

    /**
     * Parses a configured world time (named preset, fraction of a day, or hour)
     * into an {@link Instant} offset within a day.
     *
     * @param timeStr the configured time string
     * @return the resolved time, defaulting to midday on unrecognised input
     */
    private Instant parseTime(String timeStr) {
        if (timeStr == null) {
            return getMidday();
        }

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
                return parseNumericTime(normalized, base);
        }
    }

    /**
     * Parses a numeric world time. A value containing a decimal point is treated
     * as a fraction of a day ({@code 0.5} = midday); a whole number is treated as
     * an hour ({@code 13} = 13:00).
     *
     * @param normalized the lower-cased time string
     * @param base       the start-of-day instant
     * @return the resolved time, defaulting to midday when not numeric
     */
    private Instant parseNumericTime(String normalized, Instant base) {
        try {
            if (normalized.contains(".")) {
                double factor = Double.parseDouble(normalized);
                return base.plus((long) (factor * 86400 * 1_000_000_000L), ChronoUnit.NANOS);
            }
            int hour = Integer.parseInt(normalized);
            return base.plus(Math.floorMod(hour, 24), ChronoUnit.HOURS);
        } catch (NumberFormatException notNumeric) {
            return getMidday();
        }
    }

    private Instant getMidday() {
        return Instant.parse("0001-01-01T12:00:00.00Z");
    }
}
