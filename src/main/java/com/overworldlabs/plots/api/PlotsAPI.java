package com.overworldlabs.plots.api;

import com.overworldlabs.plots.model.Plot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

/**
 * Main API interface for the Plots plugin.
 * Other plugins can use this to interact with the plot system.
 */
public interface PlotsAPI {

    /**
     * Get a plot at specific coordinates in the plot world
     * 
     * @param x X coordinate
     * @param z Z coordinate
     * @return Plot at the coordinates, or null if no plot exists
     */
    @Nullable
    Plot getPlotAt(int x, int z);

    /**
     * Get a plot by its grid coordinates
     * 
     * @param gridX Grid X coordinate
     * @param gridZ Grid Z coordinate
     * @return Plot at the grid coordinates, or null if not claimed
     */
    @Nullable
    Plot getPlotByGrid(int gridX, int gridZ);

    /**
     * Get all plots owned by a player
     * 
     * @param ownerUuid UUID of the plot owner
     * @return Collection of plots owned by the player
     */
    @Nonnull
    Collection<Plot> getPlotsByOwner(@Nonnull UUID ownerUuid);

    /**
     * Get all claimed plots
     * 
     * @return Collection of all claimed plots
     */
    @Nonnull
    Collection<Plot> getAllPlots();

    /**
     * Check if a player is trusted in a plot (owner or explicitly trusted)
     * 
     * @param plot       Plot to check
     * @param playerUuid UUID of the player
     * @return true if the player is the owner or is trusted
     */
    boolean isPlayerTrusted(@Nonnull Plot plot, @Nonnull UUID playerUuid);

    /**
     * Check if a player is the owner of a plot
     * 
     * @param plot       Plot to check
     * @param playerUuid UUID of the player
     * @return true if the player is the owner
     */
    boolean isPlotOwner(@Nonnull Plot plot, @Nonnull UUID playerUuid);

    /**
     * Check if a player can build at specific coordinates
     * 
     * @param playerUuid UUID of the player
     * @param x          X coordinate
     * @param z          Z coordinate
     * @return true if the player can build at the location
     */
    boolean canPlayerBuild(@Nonnull UUID playerUuid, int x, int z);

    /**
     * Get the total number of claimed plots
     * 
     * @return Number of claimed plots
     */
    int getTotalPlots();

    /**
     * Get the number of plots owned by a specific player
     * 
     * @param ownerUuid UUID of the owner
     * @return Number of plots owned
     */
    int getPlotCount(@Nonnull UUID ownerUuid);

    /**
     * Get the name of the plot world
     * 
     * @return Name of the plot world
     */
    @Nonnull
    String getPlotWorldName();

    /**
     * Get the plot size from configuration
     * 
     * @return Size of plots in blocks
     */
    int getPlotSize();

    /**
     * Get the road width from configuration
     * 
     * @return Width of roads in blocks
     */
    int getRoadWidth();
}
