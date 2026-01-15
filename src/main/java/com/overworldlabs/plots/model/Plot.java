package com.overworldlabs.plots.model;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single plot in the world.
 * <p>
 * A plot is a claimable area in the plot world that can be owned by a player.
 * Players can build on their own plots and grant building permissions to
 * trusted players.
 * Each plot has a unique position in the grid system and maintains its own list
 * of trusted players.
 * </p>
 *
 * @author Overworld Labs
 * @author Gustavo Will
 * @version 1.0.0
 */
public class Plot {
    private final int gridX;
    private final int gridZ;
    private UUID owner;
    @Nonnull
    private String ownerName;
    @Nonnull
    private String name;
    private final List<UUID> trustedPlayers;
    private final long createdAt;

    /**
     * Constructs a new Plot with the specified grid coordinates and owner.
     * <p>
     * The plot name is automatically set to "Plot (gridX, gridZ)" and can be
     * changed later.
     * The creation timestamp is set to the current system time.
     * </p>
     *
     * @param gridX     The X coordinate in the plot grid
     * @param gridZ     The Z coordinate in the plot grid
     * @param owner     The UUID of the player who owns this plot
     * @param ownerName The display name of the plot owner
     */
    public Plot(int gridX, int gridZ, UUID owner, @Nonnull String ownerName) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.owner = owner;
        this.ownerName = ownerName;
        this.trustedPlayers = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.name = "Plot (" + gridX + ", " + gridZ + ")";
    }

    /**
     * Constructs a Plot with all fields specified (used for deserialization).
     * <p>
     * This constructor is primarily used when loading plots from persistent
     * storage.
     * </p>
     *
     * @param gridX          The X coordinate in the plot grid
     * @param gridZ          The Z coordinate in the plot grid
     * @param owner          The UUID of the plot owner
     * @param ownerName      The display name of the plot owner
     * @param name           The custom name of the plot
     * @param trustedPlayers List of UUIDs of players trusted on this plot
     * @param createdAt      The timestamp when this plot was created (milliseconds
     *                       since epoch)
     */
    public Plot(int gridX, int gridZ, UUID owner, @Nonnull String ownerName, @Nonnull String name,
            List<UUID> trustedPlayers,
            long createdAt) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.owner = owner;
        this.ownerName = ownerName;
        this.name = name;
        this.trustedPlayers = trustedPlayers != null ? new ArrayList<>(trustedPlayers) : new ArrayList<>();
        this.createdAt = createdAt;
    }

    /**
     * Gets the X coordinate of this plot in the grid system.
     *
     * @return The grid X coordinate
     */
    public int getGridX() {
        return gridX;
    }

    /**
     * Gets the Z coordinate of this plot in the grid system.
     *
     * @return The grid Z coordinate
     */
    public int getGridZ() {
        return gridZ;
    }

    /**
     * Gets the UUID of the player who owns this plot.
     *
     * @return The owner's UUID
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Sets the owner of this plot.
     *
     * @param owner The new owner's UUID
     */
    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    /**
     * Gets the display name of the plot owner.
     *
     * @return The owner's display name
     */
    @Nonnull
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Sets the display name of the plot owner.
     *
     * @param ownerName The new owner display name
     */
    public void setOwnerName(@Nonnull String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * Gets the custom name of this plot.
     *
     * @return The plot's custom name
     */
    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * Sets a custom name for this plot.
     *
     * @param name The new plot name
     */
    public void setName(@Nonnull String name) {
        this.name = name;
    }

    /**
     * Gets a copy of the list of trusted players.
     * <p>
     * Returns a new ArrayList to prevent external modification of the internal
     * list.
     * </p>
     *
     * @return A list of UUIDs of trusted players
     */
    public List<UUID> getTrustedPlayers() {
        return new ArrayList<>(trustedPlayers);
    }

    /**
     * Adds a player to the trusted players list.
     * <p>
     * If the player is already trusted, this method does nothing.
     * </p>
     *
     * @param playerUuid The UUID of the player to trust
     */
    public void addTrustedPlayer(UUID playerUuid) {
        if (!trustedPlayers.contains(playerUuid)) {
            trustedPlayers.add(playerUuid);
        }
    }

    /**
     * Removes a player from the trusted players list.
     *
     * @param playerUuid The UUID of the player to untrust
     */
    public void removeTrustedPlayer(UUID playerUuid) {
        trustedPlayers.remove(playerUuid);
    }

    /**
     * Checks if a player is trusted on this plot.
     *
     * @param playerUuid The UUID of the player to check
     * @return {@code true} if the player is trusted, {@code false} otherwise
     */
    public boolean isTrusted(UUID playerUuid) {
        return trustedPlayers.contains(playerUuid);
    }

    /**
     * Gets the timestamp when this plot was created.
     *
     * @return The creation time in milliseconds since epoch
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Checks if a player has permission to build in this plot.
     * <p>
     * A player has permission if they are either the owner or a trusted player.
     * </p>
     *
     * @param playerUuid The UUID of the player to check
     * @return {@code true} if the player can build, {@code false} otherwise
     */
    public boolean hasPermission(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        return playerUuid.equals(owner) || trustedPlayers.contains(playerUuid);
    }

    /**
     * Gets the minimum X coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The minimum world X coordinate (inclusive)
     */
    public int getMinX(PlotConfig config) {
        return gridX * (config.getPlotSize() + config.getRoadSize());
    }

    /**
     * Gets the maximum X coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The maximum world X coordinate (exclusive)
     */
    public int getMaxX(PlotConfig config) {
        return getMinX(config) + config.getPlotSize();
    }

    /**
     * Gets the minimum Z coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The minimum world Z coordinate (inclusive)
     */
    public int getMinZ(PlotConfig config) {
        return gridZ * (config.getPlotSize() + config.getRoadSize());
    }

    /**
     * Gets the maximum Z coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The maximum world Z coordinate (exclusive)
     */
    public int getMaxZ(PlotConfig config) {
        return getMinZ(config) + config.getPlotSize();
    }

    /**
     * Gets the center X coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The center world X coordinate
     */
    public double getCenterX(PlotConfig config) {
        return getMinX(config) + config.getPlotSize() / 2.0;
    }

    /**
     * Gets the center Z coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The center world Z coordinate
     */
    public double getCenterZ(PlotConfig config) {
        return getMinZ(config) + config.getPlotSize() / 2.0;
    }

    /**
     * Returns a string representation of this plot.
     *
     * @return A string containing the plot's grid coordinates, name, owner, and
     *         trusted player count
     */
    @Override
    public String toString() {
        return "Plot{" +
                "gridX=" + gridX +
                ", gridZ=" + gridZ +
                ", name='" + name + '\'' +
                ", owner=" + owner +
                ", trustedPlayers=" + trustedPlayers.size() +
                '}';
    }
}
