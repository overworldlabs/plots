package com.overworldlabs.plots.model;

import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.flag.PlotFlag;
import javax.annotation.Nonnull;
import java.util.*;

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
    private final Map<String, Object> flags;
    private final Set<String> mergedPlots;
    private final long createdAt;
    /** Optional per-plot spawn point; null means use the computed plot center. */
    private PlotSpawn spawn;

    /** Per-plot spawn location (world coordinates + rotation). */
    public static final class PlotSpawn {
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;

        public PlotSpawn() {
        }

        public PlotSpawn(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

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
        this.flags = new HashMap<>();
        this.mergedPlots = new HashSet<>();
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
            List<UUID> trustedPlayers, Map<String, Object> flags,
            long createdAt) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.owner = owner;
        this.ownerName = ownerName;
        this.name = name;
        this.trustedPlayers = trustedPlayers != null ? new ArrayList<>(trustedPlayers) : new ArrayList<>();
        this.flags = flags != null ? new HashMap<>(flags) : new HashMap<>();
        this.mergedPlots = new HashSet<>();
        this.createdAt = createdAt;
    }

    public Plot(int gridX, int gridZ, UUID owner, @Nonnull String ownerName, @Nonnull String name,
            List<UUID> trustedPlayers, Map<String, Object> flags, Set<String> mergedPlots, long createdAt) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.owner = owner;
        this.ownerName = ownerName;
        this.name = name;
        this.trustedPlayers = trustedPlayers != null ? new ArrayList<>(trustedPlayers) : new ArrayList<>();
        this.flags = flags != null ? new HashMap<>(flags) : new HashMap<>();
        this.mergedPlots = mergedPlots != null ? new HashSet<>(mergedPlots) : new HashSet<>();
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

    /** Returns the per-plot spawn, or null when none is set. */
    public PlotSpawn getSpawn() {
        return spawn;
    }

    public boolean hasSpawn() {
        return spawn != null;
    }

    /** Sets a custom per-plot spawn point. */
    public void setSpawn(double x, double y, double z, float yaw, float pitch) {
        this.spawn = new PlotSpawn(x, y, z, yaw, pitch);
    }

    public void setSpawn(PlotSpawn spawn) {
        this.spawn = spawn;
    }

    /** Clears the custom spawn so the plot falls back to its computed center. */
    public void clearSpawn() {
        this.spawn = null;
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
     * Gets the value of a specific flag.
     *
     * @param flag The flag to get
     * @param <T>  The value type
     * @return The current value, or the default value if not set
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T> T getFlagValue(@Nonnull PlotFlag<T> flag) {
        Object value = flags.get(flag.getName());
        if (value == null) {
            return flag.getDefaultValue();
        }
        return (T) value;
    }

    /**
     * Sets the value of a specific flag.
     *
     * @param flag  The flag to set
     * @param value The value to set
     * @param <T>   The value type
     */
    public <T> void setFlagValue(@Nonnull PlotFlag<T> flag, @Nonnull T value) {
        flags.put(flag.getName(), value);
    }

    /**
     * Removes a flag setting from this plot, reverting it to default.
     *
     * @param flag The flag to remove
     */
    public void removeFlag(@Nonnull PlotFlag<?> flag) {
        flags.remove(flag.getName());
    }

    /**
     * Gets a map of all custom flags set on this plot.
     *
     * @return A map of flag names to their custom values
     */
    @Nonnull
    public Map<String, Object> getFlags() {
        return new HashMap<>(flags);
    }

    @Nonnull
    public Set<String> getMergedPlots() {
        return new HashSet<>(mergedPlots);
    }

    public void addMergedPlot(int gridX, int gridZ) {
        mergedPlots.add(gridX + "," + gridZ);
    }

    public void removeMergedPlot(int gridX, int gridZ) {
        mergedPlots.remove(gridX + "," + gridZ);
    }

    public boolean isMergedWith(int gridX, int gridZ) {
        return mergedPlots.contains(gridX + "," + gridZ);
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
     * Alias for hasPermission, checks if a player is the owner or a trusted member.
     *
     * @param playerUuid The UUID of the player to check
     * @return {@code true} if the player is owner or member, {@code false}
     *         otherwise
     */
    public boolean isOwnerOrMember(UUID playerUuid) {
        return hasPermission(playerUuid);
    }

    /**
     * Gets the minimum X coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The minimum world X coordinate (inclusive)
     */
    public int getMinX(PlotConfig config) {
        return gridX * (config.getPlotSizeX() + config.getRoadSizeX());
    }

    /**
     * Gets the maximum X coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The maximum world X coordinate (exclusive)
     */
    public int getMaxX(PlotConfig config) {
        return getMinX(config) + config.getPlotSizeX();
    }

    /**
     * Gets the minimum Z coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The minimum world Z coordinate (inclusive)
     */
    public int getMinZ(PlotConfig config) {
        return gridZ * (config.getPlotSizeZ() + config.getRoadSizeZ());
    }

    /**
     * Gets the maximum Z coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The maximum world Z coordinate (exclusive)
     */
    public int getMaxZ(PlotConfig config) {
        return getMinZ(config) + config.getPlotSizeZ();
    }

    /**
     * Gets the center X coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The center world X coordinate
     */
    public double getCenterX(PlotConfig config) {
        return getMinX(config) + config.getPlotSizeX() / 2.0;
    }

    /**
     * Gets the center Z coordinate of this plot in world space.
     *
     * @param config The plot configuration containing size information
     * @return The center world Z coordinate
     */
    public double getCenterZ(PlotConfig config) {
        return getMinZ(config) + config.getPlotSizeZ() / 2.0;
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
