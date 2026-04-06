package com.overworldlabs.plots.api;

import com.overworldlabs.plots.model.Plot;
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
     * Get a plot by its grid coordinates
     */
    @Nullable
    Plot getPlot(int gridX, int gridZ);

    /**
     * Get all plots owned by a player
     */
    @Nonnull
    List<Plot> getPlotsByOwner(@Nonnull UUID ownerUuid);

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
     * Delete a plot
     */
    void deletePlot(int gridX, int gridZ);

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
