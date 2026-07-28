package dev.stoshe.plots.api;

import com.hypixel.hytale.server.core.universe.world.World;
import dev.stoshe.plots.config.PlotConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for creating and accessing the plugin's plot worlds.
 */
public interface IWorldManager {

    /**
     * @return the default (first configured) plot world name
     */
    String getWorldName();

    /**
     * @return the names of every managed plot world, in config order
     */
    @Nonnull
    List<String> getManagedWorldNames();

    /**
     * Creates every configured plot world that does not already exist.
     */
    void createWorldsIfNeeded();

    /**
     * Creates a single plot world from its config entry.
     *
     * @param name  the world name
     * @param entry the per-world configuration to generate from
     * @return a future completing with the created world (or null on failure)
     */
    @Nonnull
    CompletableFuture<World> createWorld(@Nonnull String name, @Nonnull PlotConfig.WorldEntry entry);

    /**
     * @param worldName the world name to test
     * @return {@code true} when the name is a managed plot world
     */
    boolean isPlotWorld(@Nonnull String worldName);

    /**
     * @return {@code true} when at least one managed plot world is currently
     *         loaded in the universe
     */
    boolean hasLoadedPlotWorld();

    /**
     * @return the default plot world, or {@code null} when it is not loaded
     */
    @Nullable
    World getPlotWorld();

    /**
     * @param worldName the plot world name
     * @return the loaded world, or {@code null} when not loaded / not managed
     */
    @Nullable
    World getPlotWorld(@Nonnull String worldName);
}
