package dev.stoshe.plots.api;

import dev.stoshe.plots.model.Plot;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;
import java.util.List;

/**
 * Interface for plot data access and persistence
 */
public interface IPlotRepository {

    /**
     * Get a plot by its world + grid coordinates
     */
    @Nullable
    Plot getPlot(@Nonnull String world, int gridX, int gridZ);

    /**
     * Get all plots owned by a player (across all worlds)
     */
    @Nonnull
    List<Plot> getPlotsByOwner(@Nonnull UUID ownerUuid);

    /**
     * Get all plots owned by a player in a specific world
     */
    @Nonnull
    List<Plot> getPlotsByOwnerInWorld(@Nonnull UUID ownerUuid, @Nonnull String world);

    /**
     * Count plots owned by a player in a specific world
     */
    int countByOwnerInWorld(@Nonnull UUID ownerUuid, @Nonnull String world);

    /**
     * Get all plots in a specific world
     */
    @Nonnull
    List<Plot> getPlotsInWorld(@Nonnull String world);

    /**
     * Get all plots
     */
    @Nonnull
    Collection<Plot> getAllPlots();

    /**
     * Save a plot
     */
    void savePlot(@Nonnull Plot plot);

    /**
     * Delete a plot by world + grid coordinates
     */
    void deletePlot(@Nonnull String world, int gridX, int gridZ);

    /**
     * Load all plots from persistent storage
     */
    void loadAll();

    /**
     * Save all plots to persistent storage
     */
    void saveAll();

    /**
     * Release underlying resources (connections, pools, etc).
     */
    default void close() {
    }
}
