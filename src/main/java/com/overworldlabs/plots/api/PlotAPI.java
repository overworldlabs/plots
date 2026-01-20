package com.overworldlabs.plots.api;

import com.overworldlabs.plots.model.Plot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

/**
 * API for individual plot operations.
 * Provides methods to modify and query plot data.
 */
public interface PlotAPI {

    /**
     * Claim a plot for a player
     * 
     * @param gridX     Grid X coordinate
     * @param gridZ     Grid Z coordinate
     * @param ownerUuid UUID of the new owner
     * @param ownerName Name of the new owner
     * @return The claimed plot, or null if already claimed
     */
    @Nullable
    Plot claimPlot(int gridX, int gridZ, @Nonnull UUID ownerUuid, @Nonnull String ownerName);

    /**
     * Unclaim/delete a plot
     * 
     * @param plot Plot to unclaim
     * @return true if the plot was successfully unclaimed
     */
    boolean unclaimPlot(@Nonnull Plot plot);

    /**
     * Rename a plot
     * 
     * @param plot    Plot to rename
     * @param newName New name for the plot
     * @return true if the plot was successfully renamed
     */
    boolean renamePlot(@Nonnull Plot plot, @Nonnull String newName);

    /**
     * Add a trusted player to a plot
     * 
     * @param plot       Plot to modify
     * @param playerUuid UUID of the player to trust
     * @return true if the player was successfully added
     */
    boolean trustPlayer(@Nonnull Plot plot, @Nonnull UUID playerUuid);

    /**
     * Remove a trusted player from a plot
     * 
     * @param plot       Plot to modify
     * @param playerUuid UUID of the player to untrust
     * @return true if the player was successfully removed
     */
    boolean untrustPlayer(@Nonnull Plot plot, @Nonnull UUID playerUuid);

    /**
     * Get all trusted players for a plot
     * 
     * @param plot Plot to query
     * @return Set of trusted player UUIDs
     */
    @Nonnull
    Set<UUID> getTrustedPlayers(@Nonnull Plot plot);

    /**
     * Get the owner of a plot
     * 
     * @param plot Plot to query
     * @return UUID of the owner, or null if unclaimed
     */
    @Nullable
    UUID getPlotOwner(@Nonnull Plot plot);

    /**
     * Check if a plot is claimed
     * 
     * @param gridX Grid X coordinate
     * @param gridZ Grid Z coordinate
     * @return true if the plot is claimed
     */
    boolean isPlotClaimed(int gridX, int gridZ);

    /**
     * Get the minimum corner coordinates of a plot (world coordinates)
     * 
     * @param plot Plot to get boundaries for
     * @return Array containing [minX, minZ]
     */
    int[] getPlotMinCorner(@Nonnull Plot plot);

    /**
     * Get the maximum corner coordinates of a plot (world coordinates)
     * 
     * @param plot Plot to get boundaries for
     * @return Array containing [maxX, maxZ]
     */
    int[] getPlotMaxCorner(@Nonnull Plot plot);

    /**
     * Get the center coordinates of a plot (world coordinates)
     * 
     * @param plot Plot to get center for
     * @return Array containing [centerX, centerZ]
     */
    int[] getPlotCenter(@Nonnull Plot plot);

    /**
     * Check if world coordinates are within a plot's boundaries
     * 
     * @param plot Plot to check
     * @param x    World X coordinate
     * @param z    World Z coordinate
     * @return true if the coordinates are within the plot
     */
    boolean isWithinPlot(@Nonnull Plot plot, int x, int z);
}
