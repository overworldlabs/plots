package com.overworldlabs.plots.manager;

import com.overworldlabs.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.command.system.CommandSender;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.api.IPlotRepository;
import com.overworldlabs.plots.flag.FlagRegistry;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.model.Prefab;
import com.overworldlabs.plots.util.ConsoleColors;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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
            ConsoleColors.info("Auto-adjusted dimensions from Road Prefab: PlotSizeX=" + detectedPlotSizeX
                    + ", RoadSizeZ=" + detectedRoadSizeZ);
        }

        if (plotPrefab != null) {
            config.setPlotSizeX(plotPrefab.getWidthX());
            config.setPlotSizeZ(plotPrefab.getDepthZ());
            ConsoleColors.info("Auto-adjusted dimensions from Plot Prefab: PlotSizeX=" + config.getPlotSizeX()
                    + ", PlotSizeZ=" + config.getPlotSizeZ());
        }
    }

    @Override
    public boolean claimPlot(@Nonnull CommandSender sender,
            @Nonnull PlayerRef playerRef, int gridX, int gridZ) {
        if (repository.getPlot(gridX, gridZ) != null)
            return false;

        long playerPlotCount = repository.getAllPlots().stream()
                .filter(plot -> PlayerIdentity.uuid(playerRef).equals(plot.getOwner()))
                .count();

        if (playerPlotCount >= getMaxPlots(sender))
            return false;

        Plot plot = new Plot(gridX, gridZ, PlayerIdentity.uuid(playerRef), playerRef.getUsername());
        plot.setName(config.formatDefaultPlotName(playerRef.getUsername(), gridX, gridZ));
        repository.savePlot(plot);
        return true;
    }

    public int getMaxPlots(@Nonnull CommandSender sender) {
        if (PermissionUtil.hasAdminPermission(sender)) {
            return Integer.MAX_VALUE;
        }

        for (int i = 100; i > 0; i--) { // Fixed upper bound for check
            if (sender.hasPermission("plots.limit." + i)) {
                return i;
            }
        }

        return config.getMaxPlotsDefaultValue();
    }

    @Override
    public boolean unclaimPlot(int gridX, int gridZ) {
        if (repository.getPlot(gridX, gridZ) == null)
            return false;
        repository.deletePlot(gridX, gridZ);
        return true;
    }

    @Override
    public boolean renamePlot(int gridX, int gridZ, @Nonnull String name) {
        Plot plot = getPlot(gridX, gridZ);
        if (plot == null)
            return false;
        plot.setName(name);
        return true;
    }

    @Override
    @Nullable
    public Plot getPlot(int gridX, int gridZ) {
        return repository.getPlot(gridX, gridZ);
    }

    @Override
    @Nullable
    public Plot getPlotByGrid(int gridX, int gridZ) {
        return getPlot(gridX, gridZ);
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

    @Nullable
    private Plot getRawPlotAt(String worldName, int worldX, int worldZ) {
        Plot direct = null;
        if (config.isInPlot(worldName, worldX, worldZ)) {
            int[] grid = config.getPlotGridAt(worldX, worldZ);
            direct = getPlot(grid[0], grid[1]);
        }
        if (direct != null) {
            return direct;
        }
        if (!config.isManagedWorld(worldName)) {
            return null;
        }

        int totalSizeX = config.getPlotSizeX() + config.getRoadSizeX();
        int totalSizeZ = config.getPlotSizeZ() + config.getRoadSizeZ();
        int localX = Math.floorMod(worldX, totalSizeX);
        int localZ = Math.floorMod(worldZ, totalSizeZ);

        // Only bridge merge logic on roads between plots.
        if (localX < config.getPlotSizeX() && localZ < config.getPlotSizeZ()) {
            return null;
        }

        int[] grid = config.getPlotGridAt(worldX, worldZ);
        int gx = grid[0];
        int gz = grid[1];

        Plot a = null;
        Plot b = null;

        // Vertical road strip (between west/east plots)
        if (localX >= config.getPlotSizeX() && localZ < config.getPlotSizeZ()) {
            a = getPlot(gx, gz);
            b = getPlot(gx + 1, gz);
        }
        // Horizontal road strip (between north/south plots)
        else if (localZ >= config.getPlotSizeZ() && localX < config.getPlotSizeX()) {
            a = getPlot(gx, gz);
            b = getPlot(gx, gz + 1);
        } else {
            return resolveIntersectionOwner(gx, gz);
        }

        if (a != null && b != null && a.getOwner().equals(b.getOwner()) && arePlotsMerged(a, b)) {
            return a;
        }
        return null;
    }

    @Nullable
    private Plot resolveIntersectionOwner(int gx, int gz) {
        Plot nw = getPlot(gx, gz);
        Plot ne = getPlot(gx + 1, gz);
        Plot sw = getPlot(gx, gz + 1);
        Plot se = getPlot(gx + 1, gz + 1);
        if (nw == null || ne == null || sw == null || se == null) {
            return null;
        }
        UUID owner = nw.getOwner();
        if (!owner.equals(ne.getOwner()) || !owner.equals(sw.getOwner()) || !owner.equals(se.getOwner())) {
            return null;
        }

        List<Plot> component = getMergedComponent(nw);
        boolean hasNe = false;
        boolean hasSw = false;
        boolean hasSe = false;
        for (Plot plot : component) {
            if (plot == ne) {
                hasNe = true;
            } else if (plot == sw) {
                hasSw = true;
            } else if (plot == se) {
                hasSe = true;
            }
            if (hasNe && hasSw && hasSe) {
                return nw;
            }
        }
        return null;
    }

    @Nonnull
    public List<Plot> getMergedComponent(@Nonnull Plot start) {
        List<Plot> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        ArrayDeque<Plot> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            Plot current = queue.removeFirst();
            String currentKey = current.getGridX() + "," + current.getGridZ();
            if (!visited.add(currentKey)) {
                continue;
            }
            result.add(current);

            for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                Plot neighbor = getPlotByGrid(current.getGridX() + d[0], current.getGridZ() + d[1]);
                if (neighbor == null) {
                    continue;
                }
                if (!current.getOwner().equals(neighbor.getOwner())) {
                    continue;
                }
                if (!arePlotsMerged(current, neighbor)) {
                    continue;
                }
                String neighborKey = neighbor.getGridX() + "," + neighbor.getGridZ();
                if (!visited.contains(neighborKey)) {
                    queue.addLast(neighbor);
                }
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

        // Prefab-based masking inside a plot cell.
        String prefabPath = config.getPlotPrefab();
        if (config.isInPlot(world.getName(), worldX, worldZ) && prefabPath != null && !prefabPath.isEmpty()) {
            PrefabManager pm = Plots.getInstance().getPrefabManager();
            Prefab prefab = pm.getOrLoadPrefab(prefabPath);
            if (prefab != null) {
                if (worldY < 0)
                    return false;
                int[] currentGrid = config.getPlotGridAt(worldX, worldZ);
                int originX = config.gridToWorldX(currentGrid[0]);
                int originZ = config.gridToWorldZ(currentGrid[1]);
                int relX = worldX - originX;
                int relZ = worldZ - originZ;
                if (!prefab.hasColumnAt(relX + prefab.getMinX(), relZ + prefab.getMinZ())) {
                    return false;
                }
            }
        }

        return true;
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

        int[] grid = config.getPlotGridAt(worldX, worldZ);
        Plot plot = getPlotByGrid(grid[0], grid[1]);
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
    public Map<String, Plot> getPlotsMap() {
        Map<String, Plot> map = new HashMap<>();
        for (Plot plot : repository.getAllPlots()) {
            map.put(plot.getGridX() + "," + plot.getGridZ(), plot);
        }
        return map;
    }

    @Override
    public int[] findNextFreePlot() {
        int x = 0;
        int z = 0;
        int dx = 0;
        int dz = -1;
        int maxChecks = 10000;

        for (int i = 0; i < maxChecks; i++) {
            if (repository.getPlot(x, z) == null) {
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

    public void teleportPlayerToPlot(Store<EntityStore> store, Ref<EntityStore> ref,
            Plot plot) {
        PlotConfig plotConfig = getConfig();
        String plotWorldName = plotConfig.getPlotWorldName();
        World plotWorld = Universe.get().getWorlds().get(plotWorldName);

        if (plotWorld == null) {
            return;
        }

        Vector3d pos;
        Vector3f rot;
        if (plot.hasSpawn()) {
            Plot.PlotSpawn s = plot.getSpawn();
            pos = new Vector3d(s.x, s.y, s.z);
            rot = new Vector3f(0, s.yaw, 0);
        } else {
            int gridX = plot.getGridX();
            int gridZ = plot.getGridZ();
            int worldX = plotConfig.gridToWorldX(gridX) + (plotConfig.getPlotSizeX() / 2);
            int worldZ = plotConfig.gridToWorldZ(gridZ) - 2;
            double spawnY = 66.5;
            pos = new Vector3d(worldX + 0.5, spawnY, worldZ + 0.5);
            rot = new Vector3f(0, 180, 0);
        }

        try {
            Teleport teleport = new Teleport(plotWorld, pos, rot);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void teleportPlayerToWarp(Store<EntityStore> store, Ref<EntityStore> ref, Plot.PlotWarp warp) {
        if (warp == null) {
            return;
        }
        PlotConfig plotConfig = getConfig();
        World plotWorld = Universe.get().getWorlds().get(plotConfig.getPlotWorldName());
        if (plotWorld == null) {
            return;
        }
        Vector3d pos = new Vector3d(warp.x, warp.y, warp.z);
        Vector3f rot = new Vector3f(0, warp.yaw, 0);
        try {
            Teleport teleport = new Teleport(plotWorld, pos, rot);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMaxWarpsPerPlot() {
        return getConfig().getMaxWarpsPerPlot();
    }
}
