package com.overworldlabs.plots.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.model.PlotConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all plots in the world
 */
public class PlotManager {
    public static final String PERM_ADMIN = "plots.*";
    public static final String PERM_USER = "plots.user";
    public static final String PERM_CLAIM = "plots.claim";
    public static final String PERM_DELETE = "plots.delete";
    public static final String PERM_DELETE_ANY = "plots.delete.*";
    public static final String PERM_WORLD = "plots.world";
    public static final String PERM_LIST = "plots.list";
    public static final String PERM_INFO = "plots.info";
    public static final String PERM_RENAME = "plots.rename";
    public static final String PERM_TRUST = "plots.trust";

    private final Map<String, Plot> plots;
    private final PlotConfig config;

    /**
     * Constructs a new PlotManager with the specified configuration.
     *
     * @param config The plot configuration containing world settings and limits
     */
    public PlotManager(PlotConfig config) {
        this.plots = new ConcurrentHashMap<>();
        this.config = config;
    }

    /**
     * Gets the plot configuration.
     *
     * @return The current PlotConfig instance
     */
    public PlotConfig getConfig() {
        return config;
    }

    /**
     * Generates a unique key for a plot based on its grid coordinates.
     *
     * @param gridX The X coordinate in the plot grid
     * @param gridZ The Z coordinate in the plot grid
     * @return A string key in the format "gridX,gridZ"
     */
    private String getPlotKey(int gridX, int gridZ) {
        return gridX + "," + gridZ;
    }

    /**
     * Claims a plot for a player at the specified grid coordinates.
     * <p>
     * This method checks if:
     * - The plot is not already claimed
     * - The player hasn't reached their maximum plot limit
     * </p>
     *
     * @param playerUuid The UUID of the player claiming the plot
     * @param ownerName  The display name of the player
     * @param gridX      The X coordinate in the plot grid
     * @param gridZ      The Z coordinate in the plot grid
     * @return {@code true} if the plot was successfully claimed, {@code false}
     *         otherwise
     */
    public boolean claimPlot(@Nonnull UUID playerUuid, @Nonnull String ownerName, int gridX, int gridZ) {
        String key = getPlotKey(gridX, gridZ);
        if (plots.containsKey(key))
            return false;

        long playerPlotCount = plots.values().stream()
                .filter(plot -> playerUuid.equals(plot.getOwner()))
                .count();

        if (playerPlotCount >= getMaxPlots(playerUuid))
            return false;

        Plot plot = new Plot(gridX, gridZ, playerUuid, ownerName);
        plots.put(key, plot);
        return true;
    }

    /**
     * Gets the maximum number of plots a player can claim based on their
     * permissions.
     * <p>
     * Permission hierarchy:
     * 1. plots.* - Unlimited plots
     * 2. plots.limit.N - Specific limit (checks from max down to 1)
     * 3. Default limit from config
     * </p>
     *
     * @param playerUuid The UUID of the player
     * @return The maximum number of plots the player can claim
     */
    public int getMaxPlots(@Nonnull UUID playerUuid) {
        if (PermissionsModule.get().hasPermission(playerUuid, PERM_ADMIN)) {
            return Integer.MAX_VALUE;
        }

        // Check for specific limit permissions plots.limit.N
        for (int i = config.getMaxPlotLimit(); i > 0; i--) {
            if (PermissionsModule.get().hasPermission(playerUuid,
                    "plots.limit." + i)) {
                return i;
            }
        }

        return config.getMaxPlotsDefault();
    }

    /**
     * Unclaims (deletes) a plot at the specified grid coordinates.
     *
     * @param gridX The X coordinate in the plot grid
     * @param gridZ The Z coordinate in the plot grid
     * @return {@code true} if the plot was successfully unclaimed, {@code false} if
     *         no plot existed
     */
    public boolean unclaimPlot(int gridX, int gridZ) {
        String key = getPlotKey(gridX, gridZ);
        return plots.remove(key) != null;
    }

    /**
     * Renames a plot at the specified grid coordinates.
     *
     * @param gridX The X coordinate in the plot grid
     * @param gridZ The Z coordinate in the plot grid
     * @param name  The new name for the plot
     * @return {@code true} if the plot was successfully renamed, {@code false} if
     *         no plot existed
     */
    public boolean renamePlot(int gridX, int gridZ, @Nonnull String name) {
        Plot plot = getPlot(gridX, gridZ);
        if (plot == null)
            return false;
        plot.setName(name);
        return true;
    }

    /**
     * Gets a plot by its grid coordinates.
     *
     * @param gridX The X coordinate in the plot grid
     * @param gridZ The Z coordinate in the plot grid
     * @return The Plot at the specified coordinates, or {@code null} if no plot
     *         exists
     */
    @Nullable
    public Plot getPlot(int gridX, int gridZ) {
        return plots.get(getPlotKey(gridX, gridZ));
    }

    /**
     * Gets a plot by its grid coordinates (alias for {@link #getPlot(int, int)}).
     *
     * @param gridX The X coordinate in the plot grid
     * @param gridZ The Z coordinate in the plot grid
     * @return The Plot at the specified coordinates, or {@code null} if no plot
     *         exists
     */
    @Nullable
    public Plot getPlotByGrid(int gridX, int gridZ) {
        return getPlot(gridX, gridZ);
    }

    /**
     * Saves all plots to persistent storage.
     * <p>
     * Note: This is a placeholder method. Actual persistence is handled by
     * DataManager.
     * </p>
     */
    public void savePlots() {
        // TODO: Implement actual save logic
        // This should trigger your PlotDataManager to save plots to disk
    }

    /**
     * Gets the plot at the specified world coordinates.
     * <p>
     * This method first checks if the coordinates are within the plot world,
     * then converts world coordinates to grid coordinates to find the plot.
     * </p>
     *
     * @param worldName The name of the world
     * @param worldX    The X coordinate in world space
     * @param worldZ    The Z coordinate in world space
     * @return The Plot at the specified world coordinates, or {@code null} if not
     *         in a plot
     */
    @Nullable
    public Plot getPlotAt(String worldName, int worldX, int worldZ) {
        if (!config.isInPlot(worldName, worldX, worldZ))
            return null;
        int[] grid = config.getPlotGridAt(worldX, worldZ);
        return getPlot(grid[0], grid[1]);
    }

    /**
     * Gets all plots owned by a specific player.
     *
     * @param playerUuid The UUID of the player
     * @return A list of all plots owned by the player (may be empty)
     */
    public List<Plot> getPlayerPlots(@Nonnull UUID playerUuid) {
        return plots.values().stream()
                .filter(plot -> playerUuid.equals(plot.getOwner()))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a player can modify blocks at the specified world coordinates.
     * <p>
     * A player can modify if:
     * - They have admin permission (plots.*)
     * - The location is in a plot they own
     * - The location is in a plot where they are trusted
     * </p>
     *
     * @param player The player attempting to modify
     * @param world  The world where the modification is attempted
     * @param worldX The X coordinate in world space
     * @param worldY The Y coordinate in world space (currently unused)
     * @param worldZ The Z coordinate in world space
     * @return {@code true} if the player can modify at this location, {@code false}
     *         otherwise
     */
    public boolean canModify(@Nonnull PlayerRef player, @Nonnull World world, int worldX, int worldY, int worldZ) {
        if (PermissionsModule.get().hasPermission(player.getUuid(),
                PERM_ADMIN)) {
            return true;
        }

        if (!config.isInPlot(world.getName(), worldX, worldZ)) {
            return false;
        }

        Plot plot = getPlotAt(world.getName(), worldX, worldZ);

        if (plot == null) {
            return false;
        }

        return plot.hasPermission(player.getUuid());
    }

    /**
     * Gets all plots currently managed by this PlotManager.
     *
     * @return A collection of all plots (returns a copy to prevent external
     *         modification)
     */
    public Collection<Plot> getAllPlots() {
        return new ArrayList<>(plots.values());
    }

    /**
     * Loads plots from a map, replacing all currently managed plots.
     * <p>
     * This method is typically called by DataManager during server startup.
     * </p>
     *
     * @param loadedPlots A map of plot keys to Plot objects to load
     */
    public void loadPlots(@Nonnull Map<String, Plot> loadedPlots) {
        plots.clear();
        plots.putAll(loadedPlots);
    }

    /**
     * Gets a copy of the internal plots map.
     * <p>
     * Returns a new HashMap to prevent external modification of the internal state.
     * </p>
     *
     * @return A map of plot keys to Plot objects
     */
    @Nonnull
    public Map<String, Plot> getPlotsMap() {
        return new HashMap<>(plots);
    }

    /**
     * Gets the total number of claimed plots.
     *
     * @return The number of plots currently managed
     */
    public int getPlotCount() {
        return plots.size();
    }

    /**
     * Finds the next available (unclaimed) plot using a spiral search pattern.
     * <p>
     * The search starts at (0,0) and spirals outward, checking up to 10,000 plots.
     * This ensures plots are claimed in a compact, organized pattern.
     * </p>
     *
     * @return An array containing [gridX, gridZ] of the next free plot, or
     *         {@code null} if none found
     */
    public int[] findNextFreePlot() {
        int x = 0;
        int z = 0;
        int dx = 0;
        int dz = -1;
        int maxChecks = 10000;

        for (int i = 0; i < maxChecks; i++) {
            if (!plots.containsKey(getPlotKey(x, z))) {
                return new int[] { x, z };
            }

            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }

            x += dx;
            z += dz;
        }

        return null;
    }

    /**
     * Teleports a player to the specified plot.
     * <p>
     * The player is teleported to the center-front of the plot (on the road),
     * facing south (into the plot) for optimal orientation.
     * </p>
     *
     * @param store The entity store containing the player entity
     * @param ref   The reference to the player entity
     * @param plot  The plot to teleport to
     */
    public void teleportPlayerToPlot(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
            @Nonnull Plot plot) {
        PlotConfig plotConfig = getConfig();
        String plotWorldName = plotConfig.getPlotWorldName();
        World plotWorld = Universe.get().getWorlds().get(plotWorldName);

        if (plotWorld == null) {
            return;
        }

        int gridX = plot.getGridX();
        int gridZ = plot.getGridZ();
        int worldX = plotConfig.gridToWorld(gridX) + (plotConfig.getPlotSize() / 2);
        int worldZ = plotConfig.gridToWorld(gridZ) - 2;
        double spawnY = 66.5;

        Vector3d pos = new Vector3d(worldX + 0.5, spawnY, worldZ + 0.5);
        // Rotation: Y=180 means facing south (into the plot), X=0 (level), Z=0 (no
        // tilt)
        Vector3f rot = new Vector3f(0, 180, 0);

        try {
            Teleport teleport = new Teleport(plotWorld, pos, rot);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
