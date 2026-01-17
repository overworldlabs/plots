package com.overworldlabs.plots.worldgen;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.universe.world.worldgen.*;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PrefabManager;
import com.overworldlabs.plots.model.Prefab;
import com.overworldlabs.plots.model.PlotConfig;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongPredicate;

/**
 * Custom world generator for plot worlds
 * Generates a grid of plots separated by roads with borders
 */
public class PlotWorldGenProvider implements IWorldGenProvider {
    public static final String ID = "Plots";
    public static final BuilderCodec<PlotWorldGenProvider> CODEC;

    static {
        CODEC = BuilderCodec.builder(PlotWorldGenProvider.class, PlotWorldGenProvider::new)
                .documentation("A world generation provider that generates a grid of plots separated by roads.")
                .append(new KeyedCodec<>("PlotSize", Codec.INTEGER), (p, val) -> p.plotSizeX = p.plotSizeZ = val,
                        p -> p.plotSizeX)
                .documentation("The size of each plot in blocks.").add()
                .append(new KeyedCodec<>("RoadSize", Codec.INTEGER), (p, val) -> p.roadSizeX = p.roadSizeZ = val,
                        p -> p.roadSizeX)
                .documentation("The width of roads between plots.").add()
                .build();
    }

    private int plotSizeX = 32;
    private int plotSizeZ = 32;
    private int roadSizeX = 4;
    private int roadSizeZ = 4;
    private Color tint = new Color((byte) 91, (byte) -98, (byte) 40); // Default grass tint

    public PlotWorldGenProvider() {
    }

    public PlotWorldGenProvider(int plotSizeX, int plotSizeZ, int roadSizeX, int roadSizeZ) {
        this.plotSizeX = plotSizeX;
        this.plotSizeZ = plotSizeZ;
        this.roadSizeX = roadSizeX;
        this.roadSizeZ = roadSizeZ;
    }

    @Nonnull
    @Override
    public IWorldGen getGenerator() throws WorldGenLoadException {
        // Fallback to provider fields (from Hytale CODEC)
        int px = plotSizeX, pz = plotSizeZ, rx = roadSizeX, rz = roadSizeZ;

        // Use synced dimensions from PlotConfig if available
        try {
            if (Plots.getInstance() != null && Plots.getInstance().getPlotManager() != null) {
                PlotConfig config = Plots.getInstance().getPlotManager().getConfig();
                px = config.getPlotSizeX();
                pz = config.getPlotSizeZ();
                rx = config.getRoadSizeX();
                rz = config.getRoadSizeZ();
            }
        } catch (Exception ignored) {
        }

        int safePlotSizeX = Math.max(1, px);
        int safePlotSizeZ = Math.max(1, pz);
        int safeRoadSizeX = Math.max(0, rx);
        int safeRoadSizeZ = Math.max(0, rz);

        // Safety check for tint
        int tintId = (this.tint != null) ? ColorParseUtil.colorToARGBInt(this.tint) : 0;

        return new PlotWorldGen(safePlotSizeX, safePlotSizeZ, safeRoadSizeX, safeRoadSizeZ, tintId);
    }

    private enum PositionType {
        PLOT,
        PLOT_BORDER,
        ROAD_X,
        ROAD_Z,
        INTERSECTION
    }

    /**
     * Inner class that implements the actual world generation logic
     */
    private static class PlotWorldGen implements IWorldGen {
        private final int plotSizeX;
        private final int plotSizeZ;
        private final int totalSizeX;
        private final int totalSizeZ;
        private final int tintId;
        private final int groundHeight = 64;

        private final int bedrockBlockId;
        private final int grassBlockId;
        private final int dirtBlockId;
        private final int stoneBlockId;
        private final int roadBlockId;
        private final int borderBlockId;
        private final int environmentId;

        private final Prefab roadPrefab;
        private final Prefab plotPrefab;
        private final Prefab intersectionPrefab;

        public PlotWorldGen(int plotSizeX, int plotSizeZ, int roadSizeX, int roadSizeZ, int tintId) {
            this.plotSizeX = plotSizeX;
            this.plotSizeZ = plotSizeZ;
            this.totalSizeX = Math.max(1, plotSizeX + roadSizeX);
            this.totalSizeZ = Math.max(1, plotSizeZ + roadSizeZ);
            this.tintId = tintId;

            // Load prefabs through PrefabManager
            PrefabManager pm = Plots.getInstance().getPrefabManager();
            PlotConfig config = Plots.getInstance().getPlotManager().getConfig();
            this.roadPrefab = pm.getOrLoadPrefab(config.getRoadPrefab());
            this.plotPrefab = pm.getOrLoadPrefab(config.getPlotPrefab());
            this.intersectionPrefab = pm.getOrLoadPrefab(config.getIntersectionPrefab());

            // Cache IDs with safety checks from config
            this.bedrockBlockId = getBlockId(config.getBedrockBlock(), 0);
            this.grassBlockId = getBlockId(config.getPlotSurfaceBlock(), 0);
            this.dirtBlockId = getBlockId(config.getPlotSubSurfaceBlock(), 0);
            this.stoneBlockId = getBlockId(config.getFillingBlock(), 0);
            this.roadBlockId = getBlockId(config.getRoadSurfaceBlock(), stoneBlockId);
            this.borderBlockId = getBlockId(config.getBorderBlock(), roadBlockId);

            // Safe environment lookup
            int envId = 0;
            try {
                var assetMap = Environment.getAssetMap();
                if (assetMap != null) {
                    envId = assetMap.getIndex(Environment.UNKNOWN.getId());
                }
            } catch (Exception ignored) {
            }
            this.environmentId = (envId == Integer.MIN_VALUE) ? 0 : envId;
        }

        @Nullable
        @Override
        public WorldGenTimingsCollector getTimings() {
            return null;
        }

        @Nonnull
        @Override
        public Transform[] getSpawnPoints(int seed) {
            return new Transform[] { new Transform(plotSizeX / 2.0, groundHeight + 1.5, plotSizeZ / 2.0) };
        }

        @Nonnull
        @Override
        public CompletableFuture<GeneratedChunk> generate(int seed, long index, int cx, int cz,
                @Nullable LongPredicate stillNeededParam) {

            // Normalize stillNeeded to avoid NPEs throughout the method
            final LongPredicate stillNeeded = (stillNeededParam != null) ? stillNeededParam : (i -> true);

            GeneratedBlockChunk blockChunk = new GeneratedBlockChunk(index, cx, cz);

            for (int x = 0; x < 32; x++) {
                int worldX = cx * 32 + x;
                for (int z = 0; z < 32; z++) {
                    int worldZ = cz * 32 + z;

                    PositionType posType = getPositionType(worldX, worldZ);
                    blockChunk.setTint(x, z, this.tintId);

                    // Set environment once per column if possible (sets the whole column)
                    blockChunk.setEnvironment(x, 0, z, environmentId);

                    // Layer 0: Bedrock
                    blockChunk.setBlock(x, 0, z, bedrockBlockId, 0, 0);

                    // Layer 1 to groundHeight - 4: Stone (Mass fill)
                    for (int y = 1; y < groundHeight - 3; y++) {
                        blockChunk.setBlock(x, y, z, stoneBlockId, 0, 0);
                    }

                    // Layer groundHeight - 3 to groundHeight - 1: Dirt or Stone (Road)
                    boolean isAnyRoad = posType == PositionType.ROAD_X || posType == PositionType.ROAD_Z
                            || posType == PositionType.INTERSECTION;
                    int subSurfaceBlockId = isAnyRoad ? stoneBlockId : dirtBlockId;
                    for (int y = groundHeight - 3; y < groundHeight; y++) {
                        blockChunk.setBlock(x, y, z, subSurfaceBlockId, 0, 0);
                    }

                    // Layer groundHeight (Surface)
                    int surfaceBlockId = isAnyRoad ? roadBlockId : grassBlockId;
                    blockChunk.setBlock(x, groundHeight, z, surfaceBlockId, 0, 0);

                    // Layer groundHeight + 1: Border (if applicable)
                    if (posType == PositionType.PLOT_BORDER) {
                        blockChunk.setBlock(x, groundHeight + 1, z, borderBlockId, 0, 0);
                    }
                }

                // Early exit check
                if (x % 8 == 0 && !stillNeeded.test(index)) {
                    return CompletableFuture.completedFuture(null);
                }
            }

            // Apply Prefabs
            applyPrefabs(blockChunk, cx, cz);

            GeneratedChunk gc = new GeneratedChunk(blockChunk, new GeneratedBlockStateChunk(),
                    new GeneratedEntityChunk(),
                    GeneratedChunk.makeSections());
            return CompletableFuture.completedFuture(gc);
        }

        private int getBlockId(String name, int fallback) {
            try {
                var assetMap = BlockType.getAssetMap();
                if (assetMap == null)
                    return fallback;
                int id = assetMap.getIndex(name);
                return (id == Integer.MIN_VALUE) ? fallback : id;
            } catch (Exception e) {
                return fallback;
            }
        }

        private PositionType getPositionType(int worldX, int worldZ) {
            int modX = Math.floorMod(worldX, totalSizeX);
            int modZ = Math.floorMod(worldZ, totalSizeZ);

            if (modX >= plotSizeX && modZ >= plotSizeZ) {
                return PositionType.INTERSECTION;
            }
            if (modX >= plotSizeX) {
                return PositionType.ROAD_Z;
            }
            if (modZ >= plotSizeZ) {
                return PositionType.ROAD_X;
            }

            if (modX == 0 || modX == plotSizeX - 1 || modZ == 0 || modZ == plotSizeZ - 1) {
                return PositionType.PLOT_BORDER;
            }

            return PositionType.PLOT;
        }

        /**
         * Applies prefabs to the current chunk based on its content
         */
        private void applyPrefabs(GeneratedBlockChunk chunk, int cx, int cz) {
            int startX = cx * 32;
            int startZ = cz * 32;

            int minGridX = Math.floorDiv(startX, totalSizeX);
            int maxGridX = Math.floorDiv(startX + 31, totalSizeX);
            int minGridZ = Math.floorDiv(startZ, totalSizeZ);
            int maxGridZ = Math.floorDiv(startZ + 31, totalSizeZ);

            for (int gx = minGridX; gx <= maxGridX; gx++) {
                for (int gz = minGridZ; gz <= maxGridZ; gz++) {
                    int originX = gx * totalSizeX;
                    int originZ = gz * totalSizeZ;

                    // Plot
                    if (plotPrefab != null) {
                        applyPrefabAt(chunk, plotPrefab, cx, cz, originX, originZ, 0);
                    }

                    // Road Z (Vertical, separates plots horizontally)
                    if (roadPrefab != null) {
                        applyPrefabAt(chunk, roadPrefab, cx, cz, originX + plotSizeX, originZ, 1);
                    }

                    // Road X (Horizontal, separates plots vertically)
                    if (roadPrefab != null) {
                        applyPrefabAt(chunk, roadPrefab, cx, cz, originX, originZ + plotSizeZ, 0);
                    }

                    // Intersection
                    if (intersectionPrefab != null) {
                        applyPrefabAt(chunk, intersectionPrefab, cx, cz, originX + plotSizeX, originZ + plotSizeZ, 0);
                    }
                }
            }
        }

        private void applyPrefabAt(GeneratedBlockChunk chunk, Prefab prefab, int cx, int cz, int originX, int originZ,
                int rotation) {
            int chunkMinX = cx * 32;
            int chunkMaxX = chunkMinX + 31;
            int chunkMinZ = cz * 32;
            int chunkMaxZ = chunkMinZ + 31;

            // Alignment logic: Use bounds to map (MinX, MinY, MinZ) to (originX,
            // groundHeight, originZ)
            // This ensures the prefab stays within its allocated area and at the correct
            // height.

            for (Prefab.PrefabBlock block : prefab.getBlocks()) {
                // Standardization check for prefab Air/Empty blocks

                // Do not skip "Empty" blocks, place them.
                // if (block.getName().equalsIgnoreCase("Empty")) continue;

                int relX = block.getX() - prefab.getMinX();
                int relY = block.getY() - prefab.getMinY();
                int relZ = block.getZ() - prefab.getMinZ();

                int worldX, worldZ;
                if (rotation == 1) { // 90 degrees
                    worldX = originX + relZ;
                    worldZ = originZ + relX;
                } else {
                    worldX = originX + relX;
                    worldZ = originZ + relZ;
                }

                // For RoadY, if they wanted 0-64 to be at world 64, we use the maxY of prefab
                // as reference
                // But let's try mapping the top of the prefab to the ground height if it's a
                // road.
                // Or simpler: map minY of prefab to world Y=0 if they want it exact.
                // The user said they made it 0-64. 0 is bedrock. 64 is ground.
                // So minY(0) should be at world Y(0).
                int worldY = relY; // Since minY is mapped to 0, relY is the absolute world height

                if (worldX >= chunkMinX && worldX <= chunkMaxX &&
                        worldZ >= chunkMinZ && worldZ <= chunkMaxZ &&
                        worldY >= 0 && worldY < 256) {

                    int blockId = block.getBlockId();
                    if (blockId == -1) {
                        blockId = this.getBlockId(block.getName(), 0);
                        block.setBlockId(blockId);
                    }

                    chunk.setBlock(worldX - chunkMinX, worldY, worldZ - chunkMinZ, blockId, 0, 0);
                }
            }
        }
    }
}
