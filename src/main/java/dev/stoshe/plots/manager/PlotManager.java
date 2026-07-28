package dev.stoshe.plots.manager;

import dev.stoshe.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.command.system.CommandSender;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.api.IPlotRepository;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.model.Prefab;
import dev.stoshe.plots.util.Console;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Manages all plots in the world
 */
public class PlotManager implements IPlotManager {
    private final IPlotRepository repository;
    private final PlotConfig config;
    private final PlotMergeService mergeService;
    private final PlotTerrainService terrainService;

    public PlotManager(PlotConfig config, IPlotRepository repository) {
        this.config = config;
        this.repository = repository;
        this.mergeService = new PlotMergeService(this);
        this.terrainService = new PlotTerrainService(this, config, this.mergeService);
    }

    @Override
    public PlotConfig getConfig() {
        return config;
    }

    public void syncConfigWithPrefabs() {
        PrefabManager pm = Plots.getInstance().getPrefabManager();
        if (pm == null)
            return;

        Prefab roadPrefab = pm.getOrLoadPrefab(config.getRoadPrefab());
        Prefab plotPrefab = pm.getOrLoadPrefab(config.getPlotPrefab());

        if (roadPrefab != null) {
            int detectedPlotSizeX = roadPrefab.getWidthX();
            int detectedRoadSizeZ = roadPrefab.getDepthZ();
            config.setPlotSizeX(detectedPlotSizeX);
            config.setRoadSizeZ(detectedRoadSizeZ);
            Console.info("Auto-adjusted dimensions from Road Prefab: PlotSizeX=" + detectedPlotSizeX
                    + ", RoadSizeZ=" + detectedRoadSizeZ);
        }

        if (plotPrefab != null) {
            config.setPlotSizeX(plotPrefab.getWidthX());
            config.setPlotSizeZ(plotPrefab.getDepthZ());
            Console.info("Auto-adjusted dimensions from Plot Prefab: PlotSizeX=" + config.getPlotSizeX()
                    + ", PlotSizeZ=" + config.getPlotSizeZ());
        }
    }

    /** Highest numeric suffix probed on {@code plots.limit.N} style permissions. */
    private static final int MAX_PERMISSION_LIMIT = 100;

    @Override
    public boolean claimPlot(@Nonnull CommandSender sender, @Nonnull PlayerRef playerRef, @Nonnull String world,
            int gridX, int gridZ) {
        boolean alreadyClaimed = getPlot(world, gridX, gridZ) != null;
        if (alreadyClaimed) {
            return false;
        }

        UUID owner = PlayerIdentity.uuid(playerRef);
        if (!canClaimIn(sender, owner, world)) {
            return false;
        }

        Plot plot = new Plot(gridX, gridZ, owner, playerRef.getUsername(), world);
        plot.setName(config.formatDefaultPlotName(world, playerRef.getUsername(), gridX, gridZ));
        repository.savePlot(plot);
        return true;
    }

    /**
     * Whether the player may claim one more plot in {@code world}.
     * <p>
     * A non-admin must satisfy three independent ceilings: the absolute hard cap
     * on total plots, the per-world budget, and the global shared budget.
     * </p>
     */
    public boolean canClaimIn(@Nonnull CommandSender sender, @Nonnull UUID owner, @Nonnull String world) {
        return canClaim(owner, world, sender::hasPermission, PermissionUtil.hasAdminPermission(sender));
    }

    /**
     * Permission-source-agnostic claim check, usable from a command
     * ({@link CommandSender}) or a placeholder ({@code PlayerRef}).
     *
     * @param owner         the prospective owner's UUID
     * @param world         the plot world to test
     * @param hasPermission permission predicate for the owner
     * @param admin         whether the owner bypasses limits
     * @return {@code true} when another claim is allowed
     */
    public boolean canClaim(@Nonnull UUID owner, @Nonnull String world, @Nonnull Predicate<String> hasPermission,
            boolean admin) {
        if (admin) {
            return true;
        }

        int ownedInWorld = repository.countByOwnerInWorld(owner, world);
        int ownedTotal = repository.getPlotsByOwner(owner).size();

        boolean withinHardCap = ownedTotal < config.getHardCap();
        boolean withinPerWorld = ownedInWorld < resolvePerWorldCap(world, hasPermission);
        boolean withinGlobal = ownedTotal < resolveGlobalCap(hasPermission);

        return withinHardCap && withinPerWorld && withinGlobal;
    }

    /** Plots a player may still claim in {@code world} (0 when at the limit). */
    public int remainingClaimsInWorld(@Nonnull CommandSender sender, @Nonnull UUID owner, @Nonnull String world) {
        return remainingClaims(owner, world, sender::hasPermission, PermissionUtil.hasAdminPermission(sender));
    }

    /**
     * Plots a permission holder may still claim in {@code world} (0 at the limit,
     * {@link Integer#MAX_VALUE} for admins).
     *
     * @param owner         the owner's UUID
     * @param world         the plot world
     * @param hasPermission permission predicate for the owner
     * @param admin         whether the owner bypasses limits
     * @return the remaining number of claims
     */
    public int remainingClaims(@Nonnull UUID owner, @Nonnull String world, @Nonnull Predicate<String> hasPermission,
            boolean admin) {
        if (admin) {
            return Integer.MAX_VALUE;
        }

        int ownedInWorld = repository.countByOwnerInWorld(owner, world);
        int ownedTotal = repository.getPlotsByOwner(owner).size();

        int byPerWorld = resolvePerWorldCap(world, hasPermission) - ownedInWorld;
        int byGlobal = resolveGlobalCap(hasPermission) - ownedTotal;
        int byHardCap = config.getHardCap() - ownedTotal;

        int remaining = Math.min(byPerWorld, Math.min(byGlobal, byHardCap));
        return Math.max(0, remaining);
    }

    /**
     * The resolved per-world plot cap for a permission holder.
     *
     * @param world         the plot world
     * @param hasPermission permission predicate for the owner
     * @param admin         whether the owner bypasses limits
     * @return the per-world cap ({@link Integer#MAX_VALUE} for admins/unlimited)
     */
    public int perWorldCap(@Nonnull String world, @Nonnull Predicate<String> hasPermission, boolean admin) {
        return admin ? Integer.MAX_VALUE : resolvePerWorldCap(world, hasPermission);
    }

    /**
     * The resolved global shared plot cap for a permission holder.
     *
     * @param hasPermission permission predicate for the owner
     * @param admin         whether the owner bypasses limits
     * @return the global cap ({@link Integer#MAX_VALUE} for admins/unlimited)
     */
    public int globalCap(@Nonnull Predicate<String> hasPermission, boolean admin) {
        return admin ? Integer.MAX_VALUE : resolveGlobalCap(hasPermission);
    }

    private int resolvePerWorldCap(@Nonnull String world, @Nonnull Predicate<String> hasPermission) {
        String permissionKey = config.world(world).getPermissionKey();
        int fromPermission = highestPermissionSuffix(hasPermission, "plots." + permissionKey + ".limit.");
        if (fromPermission >= 0) {
            return fromPermission;
        }

        int worldDefault = config.world(world).MaxPlotsDefault;
        int base = (worldDefault >= 0) ? worldDefault : config.getPerWorldDefault();
        return (base < 0) ? Integer.MAX_VALUE : base;
    }

    private int resolveGlobalCap(@Nonnull Predicate<String> hasPermission) {
        int fromPermission = highestPermissionSuffix(hasPermission, "plots.limit.");
        if (fromPermission >= 0) {
            return fromPermission;
        }

        int configured = config.getGlobalSharedDefault();
        return (configured < 0) ? Integer.MAX_VALUE : configured;
    }

    private int highestPermissionSuffix(@Nonnull Predicate<String> hasPermission, @Nonnull String prefix) {
        for (int limit = MAX_PERMISSION_LIMIT; limit >= 1; limit--) {
            if (hasPermission.test(prefix + limit)) {
                return limit;
            }
        }
        return -1;
    }

    @Override
    public boolean unclaimPlot(@Nonnull String world, int gridX, int gridZ) {
        boolean exists = getPlot(world, gridX, gridZ) != null;
        if (!exists) {
            return false;
        }
        repository.deletePlot(world, gridX, gridZ);
        return true;
    }

    @Override
    public boolean renamePlot(@Nonnull String world, int gridX, int gridZ, @Nonnull String name) {
        Plot plot = getPlot(world, gridX, gridZ);
        if (plot == null) {
            return false;
        }
        plot.setName(name);
        repository.savePlot(plot);
        return true;
    }

    @Override
    @Nullable
    public Plot getPlot(@Nonnull String world, int gridX, int gridZ) {
        return repository.getPlot(world, gridX, gridZ);
    }

    @Override
    @Nullable
    public Plot getPlotByGrid(@Nonnull String world, int gridX, int gridZ) {
        return getPlot(world, gridX, gridZ);
    }

    @Override
    public void savePlots() {
        repository.saveAll();
    }

    @Override
    @Nullable
    public Plot getPlotAt(String worldName, int worldX, int worldZ) {
        Plot raw = getRawPlotAt(worldName, worldX, worldZ);
        if (raw == null) {
            return null;
        }
        return getCanonicalPlot(raw);
    }

    @Override
    public boolean isInPlot(@Nonnull String worldName, int worldX, int worldZ) {
        if (!config.isManagedWorld(worldName)) {
            return false;
        }
        if (config.isInPlot(worldName, worldX, worldZ)) {
            return true;
        }
        return getRawPlotAt(worldName, worldX, worldZ) != null;
    }

    /**
     * Resolves the raw (non-canonical) plot covering a world-space position.
     * <p>
     * A position inside a plot cell maps directly to that plot. A position on a
     * road or intersection only resolves to a plot when the surrounding plots are
     * merged under a single owner.
     * </p>
     *
     * @param worldName the world name
     * @param worldX    the world-space X coordinate
     * @param worldZ    the world-space Z coordinate
     * @return the covering plot, or {@code null} when the position is unclaimed
     */
    @Nullable
    private Plot getRawPlotAt(String worldName, int worldX, int worldZ) {
        PlotConfig.WorldEntry view = config.worldOrNull(worldName);
        if (view == null) {
            return null;
        }

        if (view.isInPlotLocal(worldX, worldZ)) {
            int[] grid = view.getPlotGridAt(worldX, worldZ);
            return getPlot(worldName, grid[0], grid[1]);
        }

        return resolveRoadOwner(worldName, view, worldX, worldZ);
    }

    /**
     * Resolves the owner of a road/intersection position, bridging merged plots.
     *
     * @param worldName the world name
     * @param view      the world's generation parameters
     * @param worldX    the world-space X coordinate
     * @param worldZ    the world-space Z coordinate
     * @return the owning plot, or {@code null} when not part of a merged plot
     */
    @Nullable
    private Plot resolveRoadOwner(String worldName, PlotConfig.WorldEntry view, int worldX, int worldZ) {
        int totalSizeX = view.getPlotSizeX() + view.getRoadSizeX();
        int totalSizeZ = view.getPlotSizeZ() + view.getRoadSizeZ();
        int localX = Math.floorMod(worldX, totalSizeX);
        int localZ = Math.floorMod(worldZ, totalSizeZ);

        int[] grid = view.getPlotGridAt(worldX, worldZ);
        int gridX = grid[0];
        int gridZ = grid[1];

        boolean onVerticalRoad = localX >= view.getPlotSizeX() && localZ < view.getPlotSizeZ();
        if (onVerticalRoad) {
            return mergedRoadOwner(worldName, gridX, gridZ, gridX + 1, gridZ);
        }

        boolean onHorizontalRoad = localZ >= view.getPlotSizeZ() && localX < view.getPlotSizeX();
        if (onHorizontalRoad) {
            return mergedRoadOwner(worldName, gridX, gridZ, gridX, gridZ + 1);
        }

        return resolveIntersectionOwner(worldName, gridX, gridZ);
    }

    /**
     * Returns the shared owner of two adjacent plots when they are merged.
     *
     * @return the first plot when both are merged under the same owner, else
     *         {@code null}
     */
    @Nullable
    private Plot mergedRoadOwner(String worldName, int aGridX, int aGridZ, int bGridX, int bGridZ) {
        Plot a = getPlot(worldName, aGridX, aGridZ);
        Plot b = getPlot(worldName, bGridX, bGridZ);
        if (a == null || b == null) {
            return null;
        }
        if (!a.getOwner().equals(b.getOwner())) {
            return null;
        }
        if (!arePlotsMerged(a, b)) {
            return null;
        }
        return a;
    }

    /**
     * Resolves the owner of an intersection cell, which is only owned when all
     * four surrounding plots belong to one merged component.
     *
     * @param worldName the world name
     * @param gridX     the north-west plot grid X coordinate
     * @param gridZ     the north-west plot grid Z coordinate
     * @return the north-west plot when the 2x2 block is a single merged plot,
     *         else {@code null}
     */
    @Nullable
    private Plot resolveIntersectionOwner(String worldName, int gridX, int gridZ) {
        Plot northWest = getPlot(worldName, gridX, gridZ);
        Plot northEast = getPlot(worldName, gridX + 1, gridZ);
        Plot southWest = getPlot(worldName, gridX, gridZ + 1);
        Plot southEast = getPlot(worldName, gridX + 1, gridZ + 1);
        if (northWest == null || northEast == null || southWest == null || southEast == null) {
            return null;
        }

        UUID owner = northWest.getOwner();
        boolean sameOwner = owner.equals(northEast.getOwner())
                && owner.equals(southWest.getOwner())
                && owner.equals(southEast.getOwner());
        if (!sameOwner) {
            return null;
        }

        List<Plot> component = getMergedComponent(northWest);
        boolean allMerged = component.contains(northEast)
                && component.contains(southWest)
                && component.contains(southEast);
        return allMerged ? northWest : null;
    }

    /**
     * Returns every plot reachable from {@code start} through merge links, staying
     * within the start plot's world.
     *
     * @param start the plot to expand from
     * @return the connected merged component, always including {@code start}
     */
    @Nonnull
    public List<Plot> getMergedComponent(@Nonnull Plot start) {
        String world = start.getWorld();
        List<Plot> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        ArrayDeque<Plot> queue = new ArrayDeque<>();
        queue.add(start);

        int[][] neighborOffsets = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        while (!queue.isEmpty()) {
            Plot current = queue.removeFirst();
            String currentKey = current.getGridX() + "," + current.getGridZ();
            if (!visited.add(currentKey)) {
                continue;
            }
            result.add(current);

            for (int[] offset : neighborOffsets) {
                Plot neighbor = getPlot(world, current.getGridX() + offset[0], current.getGridZ() + offset[1]);
                if (neighbor == null) {
                    continue;
                }
                if (!current.getOwner().equals(neighbor.getOwner())) {
                    continue;
                }
                if (!arePlotsMerged(current, neighbor)) {
                    continue;
                }
                queue.addLast(neighbor);
            }
        }

        return result;
    }

    @Nonnull
    public Plot getCanonicalPlot(@Nonnull Plot start) {
        List<Plot> component = getMergedComponent(start);
        Plot canonical = start;
        for (Plot plot : component) {
            if (plot.getGridX() < canonical.getGridX()
                    || (plot.getGridX() == canonical.getGridX() && plot.getGridZ() < canonical.getGridZ())) {
                canonical = plot;
            }
        }
        return canonical;
    }

    private boolean isMergedPairWithSameOwner(@Nullable Plot a, @Nullable Plot b) {
        return mergeService.isMergedPairWithSameOwner(a, b);
    }

    public boolean mergePlots(@Nonnull Plot a, @Nonnull Plot b) {
        return mergeService.mergePlots(a, b);
    }

    public boolean unmergePlots(@Nonnull Plot a, @Nonnull Plot b) {
        return mergeService.unmergePlots(a, b);
    }

    public boolean arePlotsMerged(@Nonnull Plot a, @Nonnull Plot b) {
        return mergeService.arePlotsMerged(a, b);
    }

    public boolean mergePlotsWithRoadPolicy(@Nonnull World world, @Nonnull Plot a, @Nonnull Plot b, boolean removeRoads) {
        return mergeService.mergePlotsWithRoadPolicy(world, a, b, removeRoads);
    }

    public boolean unmergePlotsWithRoadPolicy(@Nonnull World world, @Nonnull Plot a, @Nonnull Plot b, boolean createRoad) {
        return mergeService.unmergePlotsWithRoadPolicy(world, a, b, createRoad);
    }

    public int applyMergePipeline(@Nonnull World world, @Nonnull java.util.List<Plot[]> pairs, boolean removeRoads) {
        return mergeService.applyMergePipeline(world, pairs, removeRoads);
    }

    public int applyUnmergePipeline(@Nonnull World world, @Nonnull java.util.List<Plot[]> pairs, boolean createRoad) {
        return mergeService.applyUnmergePipeline(world, pairs, createRoad);
    }

    @Nonnull
    public java.util.List<Plot> getMergedNeighbors(@Nonnull Plot plot) {
        return mergeService.getMergedNeighbors(plot);
    }

    public void regeneratePlotCell(@Nonnull World world, int gridX, int gridZ) {
        terrainService.regeneratePlotCell(world, gridX, gridZ);
    }

    public boolean deletePlotAndRegenerate(@Nonnull World world, int gridX, int gridZ) {
        return terrainService.deletePlotAndRegenerate(world, gridX, gridZ);
    }

    public void refreshBoundarySurface(@Nonnull World world, @Nonnull Plot a, @Nonnull Plot b, boolean merged) {
        terrainService.refreshBoundarySurface(world, a, b, merged);
    }

    @Override
    public List<Plot> getPlayerPlots(@Nonnull UUID playerUuid) {
        return repository.getPlotsByOwner(playerUuid);
    }

    @Override
    public boolean canModify(@Nonnull PlayerRef player, @Nonnull World world, int worldX, int worldY, int worldZ,
            @Nonnull ActionType action) {
        if (PermissionUtil.hasAdminPermission(PlayerIdentity.uuid(player))) {
            return true;
        }

        if (!config.isManagedWorld(world.getName())) {
            return false;
        }

        if (!isInPlot(world.getName(), worldX, worldZ)) {
            return false;
        }

        Plot plot = getPlotAt(world.getName(), worldX, worldZ);
        if (plot == null) {
            return false;
        }

        // Flag checks
        UUID playerUuid = PlayerIdentity.uuid(player);
        if (action == ActionType.BREAK) {
            boolean val = plot.getFlagValue(FlagRegistry.BLOCK_BREAK);
            if (!val && !plot.isOwnerOrMember(playerUuid))
                return false;
        }

        if (action == ActionType.PLACE) {
            boolean val = plot.getFlagValue(FlagRegistry.BLOCK_PLACE);
            if (!val && !plot.isOwnerOrMember(playerUuid))
                return false;
        }

        if (action == ActionType.INTERACT) {
            boolean interact = plot.getFlagValue(FlagRegistry.INTERACT);
            boolean use = plot.getFlagValue(FlagRegistry.USE);
            if ((!interact || !use) && !plot.isOwnerOrMember(playerUuid))
                return false;
        }

        if (!plot.hasPermission(playerUuid)) {
            return false;
        }

        return isWithinPrefabMask(world.getName(), worldX, worldY, worldZ);
    }

    /**
     * Applies prefab-based masking inside a plot cell: when the world's plot
     * prefab does not occupy the targeted column, modification is denied so the
     * decorative prefab footprint is protected.
     *
     * @param worldName the world name
     * @param worldX    the world-space X coordinate
     * @param worldY    the world-space Y coordinate
     * @param worldZ    the world-space Z coordinate
     * @return {@code true} when the position is buildable under the prefab mask
     */
    private boolean isWithinPrefabMask(String worldName, int worldX, int worldY, int worldZ) {
        String prefabPath = config.world(worldName).getPlotPrefab();
        boolean prefabMaskActive = config.isInPlot(worldName, worldX, worldZ)
                && prefabPath != null
                && !prefabPath.isEmpty();
        if (!prefabMaskActive) {
            return true;
        }

        Prefab prefab = Plots.getInstance().getPrefabManager().getOrLoadPrefab(prefabPath);
        if (prefab == null) {
            return true;
        }
        if (worldY < 0) {
            return false;
        }

        int[] currentGrid = config.getPlotGridAt(worldName, worldX, worldZ);
        int originX = config.gridToWorldX(worldName, currentGrid[0]);
        int originZ = config.gridToWorldZ(worldName, currentGrid[1]);
        int relativeX = worldX - originX;
        int relativeZ = worldZ - originZ;
        return prefab.hasColumnAt(relativeX + prefab.getMinX(), relativeZ + prefab.getMinZ());
    }

    @Override
    public boolean canUseBuilderTools(@Nonnull UUID playerUuid, @Nonnull String worldName, int worldX, int worldZ) {
        if (PermissionUtil.hasAdminPermission(playerUuid)) {
            return true;
        }
        if (!config.isManagedWorld(worldName)) {
            return true;
        }
        if (!isInPlot(worldName, worldX, worldZ)) {
            return false;
        }

        int[] grid = config.getPlotGridAt(worldName, worldX, worldZ);
        Plot plot = getPlotByGrid(worldName, grid[0], grid[1]);
        if (plot == null) {
            return false;
        }

        if (playerUuid.equals(plot.getOwner())) {
            return true;
        }
        if (!plot.isTrusted(playerUuid)) {
            return false;
        }
        return plot.getFlagValue(FlagRegistry.BUILD) && plot.getFlagValue(FlagRegistry.HAMMER);
    }

    @Override
    public Collection<Plot> getAllPlots() {
        return repository.getAllPlots();
    }

    public void loadPlots() {
        repository.loadAll();
    }

    @Nonnull
    /**
     * Builds a snapshot map of every plot keyed by {@code world,gridX,gridZ}.
     *
     * @return a new map of all plots across all worlds
     */
    public Map<String, Plot> getPlotsMap() {
        Map<String, Plot> map = new HashMap<>();
        for (Plot plot : repository.getAllPlots()) {
            String key = plot.getWorld() + "," + plot.getGridX() + "," + plot.getGridZ();
            map.put(key, plot);
        }
        return map;
    }

    @Override
    public int[] findNextFreePlot(@Nonnull String world) {
        int gridX = 0;
        int gridZ = 0;
        int stepX = 0;
        int stepZ = -1;
        int maxChecks = 10000;

        for (int i = 0; i < maxChecks; i++) {
            if (getPlot(world, gridX, gridZ) == null) {
                return new int[] { gridX, gridZ };
            }

            boolean atSpiralCorner = gridX == gridZ
                    || (gridX < 0 && gridX == -gridZ)
                    || (gridX > 0 && gridX == 1 - gridZ);
            if (atSpiralCorner) {
                int previousStepX = stepX;
                stepX = -stepZ;
                stepZ = previousStepX;
            }

            gridX += stepX;
            gridZ += stepZ;
        }

        return null;
    }

    @Override
    public void teleportPlayerToPlot(Store<EntityStore> store, Ref<EntityStore> ref, Plot plot) {
        String worldName = plot.getWorld() != null ? plot.getWorld() : config.getDefaultWorldName();
        World plotWorld = Universe.get().getWorlds().get(worldName);
        if (plotWorld == null) {
            return;
        }

        Vector3d position;
        Rotation3f rotation;
        if (plot.hasSpawn()) {
            Plot.PlotSpawn spawn = plot.getSpawn();
            position = new Vector3d(spawn.x, spawn.y, spawn.z);
            rotation = new Rotation3f(0, spawn.yaw, 0);
        } else {
            PlotConfig.WorldEntry view = config.world(worldName);
            int worldX = view.gridToWorldX(plot.getGridX()) + (view.getPlotSizeX() / 2);
            int worldZ = view.gridToWorldZ(plot.getGridZ()) - 2;
            double spawnY = 66.5;
            position = new Vector3d(worldX + 0.5, spawnY, worldZ + 0.5);
            rotation = new Rotation3f(0, 180, 0);
        }

        applyTeleport(store, ref, plotWorld, position, rotation);
    }

    @Override
    public void teleportPlayerToWarp(Store<EntityStore> store, Ref<EntityStore> ref, String world,
            Plot.PlotWarp warp) {
        if (warp == null) {
            return;
        }
        String worldName = (world != null && !world.isBlank()) ? world : config.getDefaultWorldName();
        World plotWorld = Universe.get().getWorlds().get(worldName);
        if (plotWorld == null) {
            return;
        }
        Vector3d position = new Vector3d(warp.x, warp.y, warp.z);
        Rotation3f rotation = new Rotation3f(0, warp.yaw, 0);
        applyTeleport(store, ref, plotWorld, position, rotation);
    }

    /**
     * Teleports a player to a plot world's spawn (its custom spawn, or the
     * computed origin-intersection spawn when none is set).
     *
     * @param store the entity store
     * @param ref   the player entity reference
     * @param world the destination plot world name
     */
    public void teleportPlayerToWorldSpawn(Store<EntityStore> store, Ref<EntityStore> ref, @Nonnull String world) {
        World plotWorld = Universe.get().getWorlds().get(world);
        if (plotWorld == null) {
            return;
        }

        PlotConfig.WorldEntry entry = config.world(world);
        PlotConfig.SpawnSettings spawn = entry.Spawn;

        Vector3d position;
        Rotation3f rotation;
        if (spawn != null && spawn.CustomSpawn) {
            position = new Vector3d(spawn.X, spawn.Y, spawn.Z);
            rotation = new Rotation3f(spawn.Pitch, spawn.Yaw, 0);
        } else {
            // Match the world's generated spawn (origin intersection, surface + 2),
            // which is where GlobalSpawnProvider places new joins. See
            // WorldManager.buildSpawnTransform.
            double x = -(entry.getRoadSizeX() / 2.0);
            double z = -(entry.getRoadSizeZ() / 2.0);
            position = new Vector3d(x, 66, z);
            rotation = new Rotation3f(0, 0, 0);
        }

        applyTeleport(store, ref, plotWorld, position, rotation);
    }

    /**
     * Queues a teleport component to move a player to a position in a world.
     *
     * @param store     the entity store
     * @param ref       the player entity reference
     * @param world     the destination world
     * @param position  the destination position
     * @param rotation  the destination rotation
     */
    private void applyTeleport(Store<EntityStore> store, Ref<EntityStore> ref, World world, Vector3d position,
            Rotation3f rotation) {
        try {
            Teleport teleport = new Teleport(world, position, rotation);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
        } catch (Exception e) {
            Console.error("Failed to teleport entity: " + e.getMessage(), e);
        }
    }

    @Override
    public int getMaxWarpsPerPlot() {
        return getConfig().getMaxWarpsPerPlot();
    }
}
