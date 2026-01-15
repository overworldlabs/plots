package com.overworldlabs.plots.worldgen;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.universe.world.worldgen.*;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.codec.ProtocolCodecs;
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
                .append(new KeyedCodec<>("PlotSize", Codec.INTEGER), (p, val) -> p.plotSize = val, p -> p.plotSize)
                .documentation("The size of each plot in blocks.").add()
                .append(new KeyedCodec<>("RoadSize", Codec.INTEGER), (p, val) -> p.roadSize = val, p -> p.roadSize)
                .documentation("The width of roads between plots.").add()
                .append(new KeyedCodec<>("Tint", ProtocolCodecs.COLOR), (p, val) -> p.tint = val, p -> p.tint)
                .documentation("The tint color for the ground.").add()
                .build();
    }

    private int plotSize = 32;
    private int roadSize = 4;
    private Color tint = new Color((byte) 91, (byte) -98, (byte) 40); // Default grass tint

    public PlotWorldGenProvider() {
    }

    public PlotWorldGenProvider(int plotSize, int roadSize) {
        this.plotSize = plotSize;
        this.roadSize = roadSize;
    }

    @Nonnull
    @Override
    public IWorldGen getGenerator() throws WorldGenLoadException {
        // Safety check for tint
        int tintId = (this.tint != null) ? ColorParseUtil.colorToARGBInt(this.tint) : 0;

        // Ensure sizes are sane to avoid division by zero
        int safePlotSize = Math.max(1, this.plotSize);
        int safeRoadSize = Math.max(0, this.roadSize);

        return new PlotWorldGen(safePlotSize, safeRoadSize, tintId);
    }

    private enum PositionType {
        PLOT,
        PLOT_BORDER,
        ROAD
    }

    /**
     * Inner class that implements the actual world generation logic
     */
    private static class PlotWorldGen implements IWorldGen {
        private final int plotSize;
        private final int totalSize;
        private final int tintId;
        private final int groundHeight = 64;

        private final int bedrockBlockId;
        private final int grassBlockId;
        private final int dirtBlockId;
        private final int stoneBlockId;
        private final int roadBlockId;
        private final int borderBlockId;
        private final int environmentId;

        public PlotWorldGen(int plotSize, int roadSize, int tintId) {
            this.plotSize = plotSize;
            this.totalSize = Math.max(1, plotSize + roadSize); // Ensure > 0 for Math.floorMod
            this.tintId = tintId;

            // Cache IDs with safety checks
            this.bedrockBlockId = getBlockId("Rock_Bedrock", 0);
            this.grassBlockId = getBlockId("Soil_Grass", 0);
            this.dirtBlockId = getBlockId("Soil_Dirt", 0);
            this.stoneBlockId = getBlockId("Rock_Stone", 0);
            this.roadBlockId = getBlockId("Rock_Stone_Cobble", stoneBlockId);
            this.borderBlockId = getBlockId("Rock_calcite_brick_smooth_half", roadBlockId);

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
            return new Transform[] { new Transform(plotSize / 2.0, groundHeight + 1.5, plotSize / 2.0) };
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
                    int subSurfaceBlockId = (posType == PositionType.ROAD) ? stoneBlockId : dirtBlockId;
                    for (int y = groundHeight - 3; y < groundHeight; y++) {
                        blockChunk.setBlock(x, y, z, subSurfaceBlockId, 0, 0);
                    }

                    // Layer groundHeight (Surface)
                    int surfaceBlockId = (posType == PositionType.ROAD) ? roadBlockId : grassBlockId;
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

            return CompletableFuture.completedFuture(
                    new GeneratedChunk(blockChunk, new GeneratedBlockStateChunk(), new GeneratedEntityChunk(),
                            GeneratedChunk.makeSections()));
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
            int modX = Math.floorMod(worldX, totalSize);
            int modZ = Math.floorMod(worldZ, totalSize);

            if (modX >= plotSize || modZ >= plotSize) {
                return PositionType.ROAD;
            }

            if (modX == 0 || modX == plotSize - 1 || modZ == 0 || modZ == plotSize - 1) {
                return PositionType.PLOT_BORDER;
            }

            return PositionType.PLOT;
        }
    }
}
