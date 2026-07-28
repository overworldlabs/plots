package dev.stoshe.plots.manager;

import com.hypixel.hytale.server.core.universe.world.World;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.model.Prefab;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles terrain-facing plot operations (regen, merge surface rebuild, borders).
 * <p>
 * This keeps heavy block-edit logic out of PlotManager and centralizes visual
 * invariants for plot roads/intersections.
 */
final class PlotTerrainService {
    private static final int DEFAULT_WORLD_MIN_Y = 0;
    private static final int DEFAULT_WORLD_MAX_Y = 320;

    private final PlotManager manager;
    private final PlotConfig config;
    private final PlotMergeService mergeService;
    private volatile Method setTintMethod;

    PlotTerrainService(@Nonnull PlotManager manager, @Nonnull PlotConfig config, @Nonnull PlotMergeService mergeService) {
        this.manager = manager;
        this.config = config;
        this.mergeService = mergeService;
    }

    /**
     * Rebuilds a single plot cell to default terrain and reapplies configured prefab.
     */
    void regeneratePlotCell(@Nonnull World world, int gridX, int gridZ) {
        if (!config.isManagedWorld(world.getName())) {
            return;
        }

        int minX = config.gridToWorldX(gridX);
        int minZ = config.gridToWorldZ(gridZ);
        int maxX = minX + config.getPlotSizeX() - 1;
        int maxZ = minZ + config.getPlotSizeZ() - 1;

        int minBuildY = resolveWorldMinY(world);
        int maxBuildY = resolveWorldMaxY(world);
        int yTop = 64;
        int yBorder = 65;
        int clearMaxY = maxBuildY;
        String bedrock = config.getBedrockBlock();
        String filling = config.getFillingBlock();
        String subSurface = config.getPlotSubSurfaceBlock();
        String surface = config.getPlotSurfaceBlock();
        String border = config.getBorderBlock();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlock(x, minBuildY, z, bedrock);
                for (int y = minBuildY + 1; y <= yTop - 3; y++) {
                    world.setBlock(x, y, z, filling);
                }
                world.setBlock(x, yTop - 2, z, subSurface);
                world.setBlock(x, yTop - 1, z, subSurface);
                world.setBlock(x, yTop, z, surface);
                applyDefaultTint(world, x, z);

                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                world.setBlock(x, yBorder, z, edge ? border : "Empty");

                for (int y = yBorder + 1; y <= clearMaxY; y++) {
                    world.setBlock(x, y, z, "Empty");
                }
            }
        }

        applyPlotPrefab(world, minX, minZ, yTop);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.performBlockUpdate(x, yTop, z);
                world.performBlockUpdate(x, yBorder, z);
            }
        }
        clearWorldMapImages(world);
    }

    /**
     * Unlinks a plot from merged neighbors, regenerates it and then unclaims it.
     */
    boolean deletePlotAndRegenerate(@Nonnull World world, int gridX, int gridZ) {
        String worldName = world.getName();
        Plot target = manager.getPlot(worldName, gridX, gridZ);
        if (target == null) {
            return false;
        }

        for (Plot neighbor : mergeService.getMergedNeighbors(target)) {
            mergeService.unmergePlotsWithRoadPolicy(world, target, neighbor, true);
        }

        regeneratePlotCell(world, gridX, gridZ);
        return manager.unclaimPlot(worldName, gridX, gridZ);
    }

    /**
     * Recomputes merged terrain and border state for the affected merged component.
     */
    void refreshBoundarySurface(@Nonnull World world, @Nonnull Plot a, @Nonnull Plot b, boolean merged) {
        if (!config.isManagedWorld(world.getName())) {
            return;
        }
        rebuildMergedSurfaceAndBorders(world, a, b, merged);
        clearWorldMapImages(world);
    }

    private void applyPlotPrefab(@Nonnull World world, int plotOriginX, int plotOriginZ, int yTop) {
        String prefabPath = config.getPlotPrefab();
        if (prefabPath == null || prefabPath.isBlank()) {
            return;
        }

        PrefabManager pm = Plots.getInstance().getPrefabManager();
        if (pm == null) {
            return;
        }
        Prefab prefab = pm.getOrLoadPrefab(prefabPath);
        if (prefab == null || prefab.getBlocks() == null || prefab.getBlocks().isEmpty()) {
            return;
        }

        if (!config.isPrefabPasteMismatches()
                && (prefab.getWidthX() > config.getPlotSizeX() || prefab.getDepthZ() > config.getPlotSizeZ())) {
            return;
        }

        int centerX = (config.getPlotSizeX() - prefab.getWidthX()) / 2;
        int centerZ = (config.getPlotSizeZ() - prefab.getDepthZ()) / 2;
        int baseX = plotOriginX + centerX + config.getPrefabOffsetX();
        int baseZ = plotOriginZ + centerZ + config.getPrefabOffsetZ();
        int minBuildY = resolveWorldMinY(world);
        int maxBuildY = resolveWorldMaxY(world);
        int baseY = config.isPrefabAutoHeight()
                ? yTop + config.getPrefabOffsetY()
                : Math.max(minBuildY + 1, config.getPrefabOffsetY());
        int rotation = Math.floorMod(config.getPrefabRotation(), 360);

        for (Prefab.PrefabBlock block : prefab.getBlocks()) {
            if (block == null) {
                continue;
            }
            String name = block.getName();
            if (name == null || name.equalsIgnoreCase("Empty") || name.equalsIgnoreCase("Air")) {
                continue;
            }

            int relX;
            int relY;
            int relZ;
            if (prefab.getAnchorX() != 0 || prefab.getAnchorY() != 0 || prefab.getAnchorZ() != 0) {
                relX = block.getX() - prefab.getAnchorX();
                relY = block.getY() - prefab.getAnchorY();
                relZ = block.getZ() - prefab.getAnchorZ();
            } else {
                relX = block.getX() - prefab.getMinX();
                relY = block.getY() - prefab.getMinY();
                relZ = block.getZ() - prefab.getMinZ();
            }

            int rotX;
            int rotZ;
            switch (rotation) {
                case 90:
                    rotX = -relZ;
                    rotZ = relX;
                    break;
                case 180:
                    rotX = -relX;
                    rotZ = -relZ;
                    break;
                case 270:
                    rotX = relZ;
                    rotZ = -relX;
                    break;
                default:
                    rotX = relX;
                    rotZ = relZ;
                    break;
            }

            int worldX = baseX + rotX;
            int worldY = baseY + relY;
            int worldZ = baseZ + rotZ;
            if (worldY < (minBuildY + 1) || worldY > maxBuildY) {
                continue;
            }
            world.setBlock(worldX, worldY, worldZ, name);
        }
    }

    private void rebuildMergedSurfaceAndBorders(@Nonnull World world, @Nonnull Plot a, @Nonnull Plot b, boolean merged) {
        Set<String> componentKeys = new HashSet<>();
        for (Plot p : manager.getMergedComponent(a)) {
            componentKeys.add(p.getGridX() + "," + p.getGridZ());
        }
        for (Plot p : manager.getMergedComponent(b)) {
            componentKeys.add(p.getGridX() + "," + p.getGridZ());
        }
        if (componentKeys.isEmpty()) {
            componentKeys.add(a.getGridX() + "," + a.getGridZ());
            componentKeys.add(b.getGridX() + "," + b.getGridZ());
        }

        Map<String, Plot> componentPlots = new HashMap<>();
        for (String key : componentKeys) {
            String[] parts = key.split(",");
            if (parts.length != 2) {
                continue;
            }
            try {
                int gx = Integer.parseInt(parts[0]);
                int gz = Integer.parseInt(parts[1]);
                Plot plot = manager.getPlotByGrid(world.getName(), gx, gz);
                if (plot != null) {
                    componentPlots.put(key, plot);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        int minGridX = Integer.MAX_VALUE;
        int minGridZ = Integer.MAX_VALUE;
        int maxGridX = Integer.MIN_VALUE;
        int maxGridZ = Integer.MIN_VALUE;
        for (String key : componentKeys) {
            String[] parts = key.split(",");
            int gx = Integer.parseInt(parts[0]);
            int gz = Integer.parseInt(parts[1]);
            minGridX = Math.min(minGridX, gx);
            minGridZ = Math.min(minGridZ, gz);
            maxGridX = Math.max(maxGridX, gx);
            maxGridZ = Math.max(maxGridZ, gz);
        }

        minGridX -= 1;
        minGridZ -= 1;
        maxGridX += 1;
        maxGridZ += 1;

        int minX = config.gridToWorldX(minGridX);
        int minZ = config.gridToWorldZ(minGridZ);
        int maxX = config.gridToWorldX(maxGridX) + config.getPlotSizeX() + config.getRoadSizeX() - 1;
        int maxZ = config.gridToWorldZ(maxGridZ) + config.getPlotSizeZ() + config.getRoadSizeZ() - 1;

        int maxBuildY = resolveWorldMaxY(world);
        int yTop = 64;
        int yBorder = 65;
        int clearMaxY = maxBuildY;
        String surface = config.getPlotSurfaceBlock();
        String subSurface = config.getPlotSubSurfaceBlock();
        String filling = config.getFillingBlock();
        String roadSurface = config.getRoadSurfaceBlock();
        String border = config.getBorderBlock();

        Set<Long> plotBodyArea = new HashSet<>();
        Set<Long> insideMergedArea = new HashSet<>();
        for (Plot plot : componentPlots.values()) {
            addPlotBody(plotBodyArea, plot);
        }
        insideMergedArea.addAll(plotBodyArea);
        for (Plot plot : componentPlots.values()) {
            if (plot.isMergedWith(plot.getGridX() + 1, plot.getGridZ())) {
                removeRoadEast(insideMergedArea, plot);
            }
            if (plot.isMergedWith(plot.getGridX(), plot.getGridZ() + 1)) {
                removeRoadSouth(insideMergedArea, plot);
            }
            if (plot.isMergedWith(plot.getGridX() + 1, plot.getGridZ())
                    && plot.isMergedWith(plot.getGridX(), plot.getGridZ() + 1)) {
                Plot east = manager.getPlotByGrid(plot.getWorld(), plot.getGridX() + 1, plot.getGridZ());
                Plot south = manager.getPlotByGrid(plot.getWorld(), plot.getGridX(), plot.getGridZ() + 1);
                if (east != null && south != null && east.isMergedWith(south.getGridX(), south.getGridZ())) {
                    removeRoadSouthEast(insideMergedArea, plot);
                }
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long packedPos = pack(x, z);
                boolean connected = insideMergedArea.contains(packedPos);
                boolean plotBody = plotBodyArea.contains(packedPos);
                if (connected) {
                    // Preserve existing user-built terrain inside plot cells.
                    // Only merged road strips should be rebuilt to join/split plots.
                    if (!plotBody) {
                        world.setBlock(x, yTop - 1, z, subSurface);
                        world.setBlock(x, yTop - 2, z, subSurface);
                        world.setBlock(x, yTop - 3, z, subSurface);
                        world.setBlock(x, yTop, z, surface);
                        applyDefaultTint(world, x, z);
                    }
                    // Border layer (yBorder) is handled by the perimeter pass below so that
                    // player-placed blocks in the plot interior are not wiped here.
                } else if (isRoadCell(x, z)) {
                    world.setBlock(x, yTop - 1, z, filling);
                    world.setBlock(x, yTop - 2, z, filling);
                    world.setBlock(x, yTop - 3, z, filling);
                    world.setBlock(x, yTop, z, roadSurface);
                    applyDefaultTint(world, x, z);
                    world.setBlock(x, yBorder, z, "Empty");
                    if (!merged) {
                        for (int y = yBorder + 1; y <= clearMaxY; y++) {
                            world.setBlock(x, y, z, "Empty");
                        }
                    }
                }
            }
        }

        for (Long packed : insideMergedArea) {
            int x = unpackX(packed);
            int z = unpackZ(packed);
            boolean edge = hasOutsideNeighbor(insideMergedArea, x, z);
            if (edge) {
                // Outer perimeter of the merged shape carries the border block.
                world.setBlock(x, yBorder, z, border);
                world.performBlockUpdate(x, yBorder, z);
            } else if (!plotBodyArea.contains(packed) || isRoadAdjacent(x, z)) {
                // Plugin-owned border-layer cells: former road strips that became plot
                // interior, and plot-edge seams between merged plots. Clear stale borders.
                world.setBlock(x, yBorder, z, "Empty");
                world.performBlockUpdate(x, yBorder, z);
            }
            // Deep interior plot cells are left untouched so player-placed blocks at the
            // border layer survive merge/unmerge.
            world.performBlockUpdate(x, yTop, z);
        }

        rebuildBoundariesAround(world, a, b);
    }

    private void addPlotBody(@Nonnull Set<Long> area, @Nonnull Plot plot) {
        int minX = config.gridToWorldX(plot.getGridX());
        int minZ = config.gridToWorldZ(plot.getGridZ());
        int maxX = minX + config.getPlotSizeX() - 1;
        int maxZ = minZ + config.getPlotSizeZ() - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                area.add(pack(x, z));
            }
        }
    }

    private void removeRoadEast(@Nonnull Set<Long> area, @Nonnull Plot plot) {
        int originX = config.gridToWorldX(plot.getGridX());
        int originZ = config.gridToWorldZ(plot.getGridZ());
        int minX = originX + config.getPlotSizeX();
        int maxX = minX + config.getRoadSizeX() - 1;
        int minZ = originZ;
        int maxZ = originZ + config.getPlotSizeZ() - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                area.add(pack(x, z));
            }
        }
    }

    private void removeRoadSouth(@Nonnull Set<Long> area, @Nonnull Plot plot) {
        int originX = config.gridToWorldX(plot.getGridX());
        int originZ = config.gridToWorldZ(plot.getGridZ());
        int minX = originX;
        int maxX = originX + config.getPlotSizeX() - 1;
        int minZ = originZ + config.getPlotSizeZ();
        int maxZ = minZ + config.getRoadSizeZ() - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                area.add(pack(x, z));
            }
        }
    }

    private void removeRoadSouthEast(@Nonnull Set<Long> area, @Nonnull Plot plot) {
        int originX = config.gridToWorldX(plot.getGridX());
        int originZ = config.gridToWorldZ(plot.getGridZ());
        int minX = originX + config.getPlotSizeX();
        int maxX = minX + config.getRoadSizeX() - 1;
        int minZ = originZ + config.getPlotSizeZ();
        int maxZ = minZ + config.getRoadSizeZ() - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                area.add(pack(x, z));
            }
        }
    }

    private long pack(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private boolean hasOutsideNeighbor(@Nonnull Set<Long> insideMergedArea, int x, int z) {
        return !insideMergedArea.contains(pack(x + 1, z))
                || !insideMergedArea.contains(pack(x - 1, z))
                || !insideMergedArea.contains(pack(x, z + 1))
                || !insideMergedArea.contains(pack(x, z - 1))
                || !insideMergedArea.contains(pack(x + 1, z + 1))
                || !insideMergedArea.contains(pack(x + 1, z - 1))
                || !insideMergedArea.contains(pack(x - 1, z + 1))
                || !insideMergedArea.contains(pack(x - 1, z - 1));
    }

    private int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private int unpackZ(long packed) {
        return (int) packed;
    }

    private boolean isRoadCell(int worldX, int worldZ) {
        int totalSizeX = config.getPlotSizeX() + config.getRoadSizeX();
        int totalSizeZ = config.getPlotSizeZ() + config.getRoadSizeZ();
        int localX = Math.floorMod(worldX, totalSizeX);
        int localZ = Math.floorMod(worldZ, totalSizeZ);
        return localX >= config.getPlotSizeX() || localZ >= config.getPlotSizeZ();
    }

    /**
     * True when a plot cell touches a road/intersection cell on any of its 4 sides.
     * These are the plot-edge cells that legitimately host (or hosted) a border at the
     * border layer, as opposed to deep-interior cells owned by the player.
     */
    private boolean isRoadAdjacent(int worldX, int worldZ) {
        return isRoadCell(worldX + 1, worldZ)
                || isRoadCell(worldX - 1, worldZ)
                || isRoadCell(worldX, worldZ + 1)
                || isRoadCell(worldX, worldZ - 1);
    }

    private void rebuildBoundariesAround(@Nonnull World world, @Nonnull Plot a, @Nonnull Plot b) {
        Set<String> toRebuild = new HashSet<>();
        addPlotAndNeighbors(toRebuild, a.getGridX(), a.getGridZ());
        addPlotAndNeighbors(toRebuild, b.getGridX(), b.getGridZ());

        for (String key : toRebuild) {
            String[] parts = key.split(",");
            int gx = Integer.parseInt(parts[0]);
            int gz = Integer.parseInt(parts[1]);
            Plot p = manager.getPlotByGrid(world.getName(), gx, gz);
            if (p != null) {
                rebuildPlotBoundary(world, p);
            }
        }
    }

    private void addPlotAndNeighbors(Set<String> target, int gx, int gz) {
        target.add(gx + "," + gz);
        target.add((gx + 1) + "," + gz);
        target.add((gx - 1) + "," + gz);
        target.add(gx + "," + (gz + 1));
        target.add(gx + "," + (gz - 1));
    }

    private void rebuildPlotBoundary(@Nonnull World world, @Nonnull Plot plot) {
        int minX = config.gridToWorldX(plot.getGridX());
        int minZ = config.gridToWorldZ(plot.getGridZ());
        int maxX = minX + config.getPlotSizeX() - 1;
        int maxZ = minZ + config.getPlotSizeZ() - 1;
        int yBorder = 65;
        String border = config.getBorderBlock();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean onEdge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (!onEdge) {
                    continue;
                }

                boolean shouldHaveBorder = false;
                if (x == minX) {
                    shouldHaveBorder |= needsBorderOnSide(plot, -1, 0);
                }
                if (x == maxX) {
                    shouldHaveBorder |= needsBorderOnSide(plot, 1, 0);
                }
                if (z == minZ) {
                    shouldHaveBorder |= needsBorderOnSide(plot, 0, -1);
                }
                if (z == maxZ) {
                    shouldHaveBorder |= needsBorderOnSide(plot, 0, 1);
                }

                world.setBlock(x, yBorder, z, shouldHaveBorder ? border : "Empty");
                world.performBlockUpdate(x, yBorder, z);
            }
        }
    }

    private boolean needsBorderOnSide(@Nonnull Plot plot, int dx, int dz) {
        Plot neighbor = manager.getPlotByGrid(plot.getWorld(), plot.getGridX() + dx, plot.getGridZ() + dz);
        if (neighbor == null) {
            return true;
        }
        if (!plot.getOwner().equals(neighbor.getOwner())) {
            return true;
        }
        return !mergeService.arePlotsMerged(plot, neighbor);
    }

    /**
     * Clears custom tint layers applied on floor tiles when API supports it.
     */
    private void applyDefaultTint(@Nonnull World world, int x, int z) {
        Method method = resolveSetTintMethod(world);
        if (method == null) {
            return;
        }
        try {
            method.invoke(world, x, z, 0);
        } catch (Exception ignored) {
        }
    }

    private Method resolveSetTintMethod(@Nonnull World world) {
        Method local = setTintMethod;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (setTintMethod != null) {
                return setTintMethod;
            }
            for (String name : new String[] { "setTint", "setBlockTint", "setMapTint" }) {
                try {
                    Method candidate = world.getClass().getMethod(name, int.class, int.class, int.class);
                    candidate.setAccessible(true);
                    setTintMethod = candidate;
                    return candidate;
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    private void clearWorldMapImages(@Nonnull World world) {
        try {
            var worldMapManager = world.getWorldMapManager();
            if (worldMapManager == null) {
                return;
            }
            worldMapManager.clearImages();
            worldMapManager.generate();
            worldMapManager.sendSettings();
        } catch (Exception ignored) {
        }
    }

    private int resolveWorldMinY(@Nonnull World world) {
        return DEFAULT_WORLD_MIN_Y;
    }

    private int resolveWorldMaxY(@Nonnull World world) {
        return DEFAULT_WORLD_MAX_Y;
    }
}
