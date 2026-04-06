package com.overworldlabs.plots.worldgen;

import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedBlockChunk;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedBlockStateChunk;
import com.overworldlabs.plots.model.Prefab;
import com.hypixel.hytale.component.Holder;

/**
 * Handles the application of prefabs to the world during chunk generation.
 */
public class PlotPrefabApplier {

    private final PlotGenerationContext ctx;
    private final PlotTerrainGenerator terrainGen;

    public PlotPrefabApplier(PlotGenerationContext ctx, PlotTerrainGenerator terrainGen) {
        this.ctx = ctx;
        this.terrainGen = terrainGen;
    }

    /**
     * Applies prefabs to the given chunk.
     * Calculates the position of each prefab based on the grid coordinates and
     * configuration-defined offsets.
     * 
     * The plot prefab is centered within the plot area by default, with an
     * additional
     * configurable offset.
     * 
     * @param chunk           The chunk to apply blocks to
     * @param blockStateChunk The chunk to apply block states to
     * @param sections        Chunk store sections
     * @param cx              Chunk X coordinate
     * @param cz              Chunk Z coordinate
     */
    public void applyPrefabs(GeneratedBlockChunk chunk, GeneratedBlockStateChunk blockStateChunk,
            Holder<ChunkStore>[] sections, int cx, int cz) {
        int startX = cx * 32;
        int startZ = cz * 32;

        int minGridX = Math.floorDiv(startX, ctx.getTotalSizeX());
        int maxGridX = Math.floorDiv(startX + 31, ctx.getTotalSizeX());
        int minGridZ = Math.floorDiv(startZ, ctx.getTotalSizeZ());
        int maxGridZ = Math.floorDiv(startZ + 31, ctx.getTotalSizeZ());

        for (int gx = minGridX; gx <= maxGridX; gx++) {
            for (int gz = minGridZ; gz <= maxGridZ; gz++) {
                int originX = gx * ctx.getTotalSizeX();
                int originZ = gz * ctx.getTotalSizeZ();

                // Plot Prefab Calculation (Centered + Offset + Rotation)
                if (ctx.getPlotPrefab() != null) {
                    Prefab prefab = ctx.getPlotPrefab();

                    // Check for size mismatches if configured
                    if (!ctx.getConfig().isPrefabPasteMismatches()) {
                        if (prefab.getWidthX() > ctx.getPlotSizeX() || prefab.getDepthZ() > ctx.getPlotSizeZ()) {
                            // Skip application if it doesn't fit and mismatches are disabled
                            continue;
                        }
                    }

                    // Calculate centering offset
                    int centerX = (ctx.getPlotSizeX() - prefab.getWidthX()) / 2;
                    int centerZ = (ctx.getPlotSizeZ() - prefab.getDepthZ()) / 2;

                    // Apply configuration offsets
                    int finalX = originX + centerX + ctx.getConfig().getPrefabOffsetX();
                    int finalZ = originZ + centerZ + ctx.getConfig().getPrefabOffsetZ();
                    int finalY = ctx.getConfig().getPrefabOffsetY();
                    int rotation = ctx.getConfig().getPrefabRotation();

                    applyPrefabAt(chunk, blockStateChunk, sections, prefab, cx, cz, finalX, finalZ,
                            0, 0, finalY, rotation, false);
                }

                // Road Prefabs
                if (ctx.getRoadPrefab() != null) {
                    // X-axis road
                    applyPrefabAt(chunk, blockStateChunk, sections, ctx.getRoadPrefab(), cx, cz,
                            originX + ctx.getPlotSizeX(), originZ, 0, 0, 0, 90,
                            ctx.getConfig().isPrefabPasteRoadOnTop());
                    // Z-axis road
                    applyPrefabAt(chunk, blockStateChunk, sections, ctx.getRoadPrefab(), cx, cz, originX,
                            originZ + ctx.getPlotSizeZ(), 0, 0, 0, 0, ctx.getConfig().isPrefabPasteRoadOnTop());
                }

                // Intersection Prefab
                if (ctx.getIntersectionPrefab() != null) {
                    applyPrefabAt(chunk, blockStateChunk, sections, ctx.getIntersectionPrefab(), cx, cz,
                            originX + ctx.getPlotSizeX(), originZ + ctx.getPlotSizeZ(), 0, 0, 0, 0,
                            ctx.getConfig().isPrefabPasteRoadOnTop());
                }
            }
        }
    }

    /**
     * Applies a prefab at a specific world position with optional relative offsets
     * and rotation.
     * 
     * @param chunk           The chunk to apply blocks to
     * @param blockStateChunk The chunk to apply block states to
     * @param sections        Chunk store sections
     * @param prefab          The prefab to apply
     * @param cx              Chunk X coordinate
     * @param cz              Chunk Z coordinate
     * @param worldX          Base world X position
     * @param worldZ          Base world Z position
     * @param offsetX         Relative X offset
     * @param offsetZ         Relative Z offset
     * @param offsetY         Base height offset
     * @param rotation        Rotation in degrees (0, 90, 180, 270)
     */
    private void applyPrefabAt(GeneratedBlockChunk chunk, GeneratedBlockStateChunk blockStateChunk,
            Holder<ChunkStore>[] sections, Prefab prefab,
            int cx, int cz, int worldX, int worldZ, int offsetX, int offsetZ, int offsetY, int rotation,
            boolean forcePasteOnTop) {
        int chunkMinX = cx * 32;
        int chunkMaxX = chunkMinX + 31;
        int chunkMinZ = cz * 32;
        int chunkMaxZ = chunkMinZ + 31;

        if (prefab.getBlocks() != null) {
            for (Prefab.PrefabBlock block : prefab.getBlocks()) {
                int relX, relY, relZ;

                if (prefab.getAnchorX() != 0 || prefab.getAnchorY() != 0 || prefab.getAnchorZ() != 0) {
                    relX = block.getX() - prefab.getAnchorX();
                    relY = block.getY() - prefab.getAnchorY();
                    relZ = block.getZ() - prefab.getAnchorZ();
                } else {
                    relX = block.getX() - prefab.getMinX();
                    relY = block.getY() - prefab.getMinY();
                    relZ = block.getZ() - prefab.getMinZ();
                }

                // Apply prefab rotation around its logical origin.
                int rotX, rotZ;
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
                    case 0:
                    default:
                        rotX = relX;
                        rotZ = relZ;
                        break;
                }

                int fWorldX = worldX + offsetX + rotX;
                int fWorldZ = worldZ + offsetZ + rotZ;

                // Height policy: either align with terrain top or use absolute offset.
                int baseHeight;
                if (ctx.getConfig().isPrefabAutoHeight() || forcePasteOnTop) {
                    // Auto-height or forced: place on top of ground height + relative offset
                    baseHeight = ctx.getGroundHeight() + offsetY;
                } else {
                    // Absolute height: use offset directly (minimum 1 to avoid bedrock/void issues)
                    baseHeight = Math.max(1, offsetY);
                }

                int fWorldY = baseHeight + relY;

                if (fWorldX >= chunkMinX && fWorldX <= chunkMaxX && fWorldZ >= chunkMinZ && fWorldZ <= chunkMaxZ
                        && fWorldY >= 0 && fWorldY < 256) {
                    String name = block.getName();
                    if (name == null || name.equalsIgnoreCase("Empty") || name.equalsIgnoreCase("Air"))
                        continue;

                    int blockId = block.getBlockId();
                    terrainGen.setBlockOrFluid(chunk, sections, fWorldX - chunkMinX, fWorldY, fWorldZ - chunkMinZ,
                            blockId);
                }
            }
        }

        // Apply Fluids
        if (prefab.getFluids() != null) {
            for (Prefab.PrefabFluid fluid : prefab.getFluids()) {
                int relX, relY, relZ;

                if (prefab.getAnchorX() != 0 || prefab.getAnchorY() != 0 || prefab.getAnchorZ() != 0) {
                    relX = fluid.getX() - prefab.getAnchorX();
                    relY = fluid.getY() - prefab.getAnchorY();
                    relZ = fluid.getZ() - prefab.getAnchorZ();
                } else {
                    relX = fluid.getX() - prefab.getMinX();
                    relY = fluid.getY() - prefab.getMinY();
                    relZ = fluid.getZ() - prefab.getMinZ();
                }

                int rotX, rotZ;
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
                    case 0:
                    default:
                        rotX = relX;
                        rotZ = relZ;
                        break;
                }

                int fWorldX = worldX + offsetX + rotX;
                int fWorldZ = worldZ + offsetZ + rotZ;

                int baseHeight;
                if (ctx.getConfig().isPrefabAutoHeight() || forcePasteOnTop) {
                    baseHeight = ctx.getGroundHeight() + offsetY;
                } else {
                    baseHeight = Math.max(1, offsetY);
                }

                int fWorldY = baseHeight + relY;

                if (fWorldX >= chunkMinX && fWorldX <= chunkMaxX && fWorldZ >= chunkMinZ && fWorldZ <= chunkMaxZ
                        && fWorldY >= 0 && fWorldY < 256) {
                    String name = fluid.getName();
                    if (name == null || name.equalsIgnoreCase("Empty") || name.equalsIgnoreCase("Air"))
                        continue;

                    int fluidId = ctx.getFluidIdByName(name);
                    if (fluidId != -1) {
                        terrainGen.setFluid(sections, fWorldX - chunkMinX, fWorldY, fWorldZ - chunkMinZ, fluidId,
                                (byte) fluid.getLevel());
                    }
                }
            }
        }
    }
}
