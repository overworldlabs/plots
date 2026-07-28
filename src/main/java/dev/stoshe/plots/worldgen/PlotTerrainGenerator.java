package dev.stoshe.plots.worldgen;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedBlockChunk;
import com.hypixel.hytale.component.Holder;

/**
 * Responsible for generating the base terrain (bedrock, filling, and surface)
 * for plot worlds.
 */
public class PlotTerrainGenerator {

    private final PlotGenerationContext ctx;

    public PlotTerrainGenerator(PlotGenerationContext ctx) {
        this.ctx = ctx;
    }

    public void generateBaseTerrain(GeneratedBlockChunk blockChunk, Holder<ChunkStore>[] sections, int worldX,
            int worldZ, int lx, int lz) {
        PositionType posType = getPositionType(worldX, worldZ);

        blockChunk.setEnvironment(lx, 0, lz, ctx.getEnvironmentId());
        blockChunk.setBlock(lx, 0, lz, ctx.getBedrockBlockId(), 0, 0);

        // Underground filling
        for (int y = 1; y < ctx.getGroundHeight() - 3; y++) {
            blockChunk.setBlock(lx, y, lz, ctx.getStoneBlockId(), 0, 0);
        }

        // Sub-surface
        boolean isAnyRoad = isRoad(posType);
        int subSurfaceBlockId = isAnyRoad ? ctx.getStoneBlockId() : ctx.getDirtBlockId();
        for (int y = ctx.getGroundHeight() - 3; y < ctx.getGroundHeight(); y++) {
            setBlockOrFluid(blockChunk, sections, lx, y, lz, subSurfaceBlockId);
        }

        // Surface
        int surfaceBlockId = isAnyRoad ? ctx.getRoadBlockId() : ctx.getGrassBlockId();
        setBlockOrFluid(blockChunk, sections, lx, ctx.getGroundHeight(), lz, surfaceBlockId);

        // Optional Border
        if (posType == PositionType.PLOT_BORDER) {
            setBlockOrFluid(blockChunk, sections, lx, ctx.getGroundHeight() + 1, lz, ctx.getBorderBlockId());
        }
    }

    private boolean isRoad(PositionType type) {
        return type == PositionType.ROAD_X || type == PositionType.ROAD_Z || type == PositionType.INTERSECTION;
    }

    private PositionType getPositionType(int worldX, int worldZ) {
        int modX = Math.floorMod(worldX, ctx.getTotalSizeX());
        int modZ = Math.floorMod(worldZ, ctx.getTotalSizeZ());

        if (modX >= ctx.getPlotSizeX() && modZ >= ctx.getPlotSizeZ())
            return PositionType.INTERSECTION;
        if (modX >= ctx.getPlotSizeX())
            return PositionType.ROAD_Z;
        if (modZ >= ctx.getPlotSizeZ())
            return PositionType.ROAD_X;

        if (modX == 0 || modX == ctx.getPlotSizeX() - 1 || modZ == 0 || modZ == ctx.getPlotSizeZ() - 1) {
            return PositionType.PLOT_BORDER;
        }
        return PositionType.PLOT;
    }

    protected void setBlockOrFluid(GeneratedBlockChunk blockChunk, Holder<ChunkStore>[] sections, int x, int y, int z,
            int id) {
        if (id <= 0)
            return;

        String name = getBlockName(id);
        int fluidId = ctx.getFluidIdByName(name);

        if (fluidId != -1) {
            setFluid(sections, x, y, z, fluidId, (byte) 1);
            blockChunk.setBlock(x, y, z, 0, 0, 0);
        } else {
            blockChunk.setBlock(x, y, z, id, 0, 0);
        }
    }

    @SuppressWarnings("null")
    protected void setFluid(Holder<ChunkStore>[] sections, int lx, int worldY, int lz, int fluidId, byte level) {
        int sectionIndex = worldY >> 4;
        Holder<ChunkStore> section = sections[sectionIndex];
        FluidSection fluidSection = section.getComponent(FluidSection.getComponentType());
        if (fluidSection == null) {
            fluidSection = new FluidSection();
            section.putComponent(FluidSection.getComponentType(), fluidSection);
        }
        fluidSection.setFluid(lx, worldY % 16, lz, fluidId, level);
    }

    private String getBlockName(int id) {
        try {
            var assetMap = BlockType.getAssetMap();
            if (assetMap != null) {
                var asset = assetMap.getAsset(id);
                if (asset != null)
                    return asset.getId();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public enum PositionType {
        PLOT, PLOT_BORDER, ROAD_X, ROAD_Z, INTERSECTION
    }
}
