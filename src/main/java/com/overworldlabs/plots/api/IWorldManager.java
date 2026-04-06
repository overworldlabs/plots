package com.overworldlabs.plots.api;

import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface for managing the plot world.
 */
public interface IWorldManager {
    String getWorldName();

    void createWorldIfNeeded();

    boolean isPlotWorld(@Nonnull String worldName);

    @Nullable
    World getPlotWorld();
}
