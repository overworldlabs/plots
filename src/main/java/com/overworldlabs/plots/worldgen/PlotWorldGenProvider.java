package com.overworldlabs.plots.worldgen;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.universe.world.worldgen.*;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.api.PlotWorldGenOverride;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongPredicate;

/**
 * High-level provider for plot world generation.
 * This class handles Hytale integration and delegates generation logic to
 * specialized components.
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
    private Color tint = new Color((byte) 91, (byte) -98, (byte) 40);

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
        PlotConfig config = PlotConfig.getDefault();

        // Sync dimensions from asset configuration or constructor
        config.setPlotSizeX(plotSizeX);
        config.setPlotSizeZ(plotSizeZ);
        config.setRoadSizeX(roadSizeX);
        config.setRoadSizeZ(roadSizeZ);

        if (Plots.getInstance() != null && Plots.getInstance().getPlotManager() != null) {
            PlotConfig runtimeConfig = Plots.getInstance().getPlotManager().getConfig();
            // Use runtime config values if they exist (they usually override asset
            // defaults)
            config = runtimeConfig;
        }

        PlotGenerationContext context = new PlotGenerationContext(config);
        int tintId = (this.tint != null) ? ColorParseUtil.colorToARGBInt(this.tint) : 0;

        PlotWorldGenOverride override = WorldGenOverrideRegistry
                .getOverride();
        if (override != null) {
            IWorldGen custom = override.createGenerator(context, tintId);
            if (custom != null) {
                return custom;
            }
        }

        return new PlotWorldGen(context, tintId);
    }

    /**
     * Managed implementation of IWorldGen that delegates to specialized generators.
     */
    private static class PlotWorldGen implements IWorldGen {
        @SuppressWarnings("unused")
        private final PlotGenerationContext ctx;
        private final int tintId;

        private final PlotTerrainGenerator terrainGenerator;
        private final PlotPrefabApplier prefabApplier;
        private final PlotSpawnProvider spawnProvider;

        public PlotWorldGen(PlotGenerationContext ctx, int tintId) {
            this.ctx = ctx;
            this.tintId = tintId;
            this.terrainGenerator = new PlotTerrainGenerator(ctx);
            this.prefabApplier = new PlotPrefabApplier(ctx, terrainGenerator);
            this.spawnProvider = new PlotSpawnProvider(ctx);
        }

        @Nullable
        @Override
        public WorldGenTimingsCollector getTimings() {
            return null;
        }

        @Nonnull
        @Override
        @SuppressWarnings({ "null", "deprecation" })
        public Transform[] getSpawnPoints(int seed) {
            return spawnProvider.getSpawnPoints();
        }

        @Nonnull
        @Override
        @SuppressWarnings("null")
        public CompletableFuture<GeneratedChunk> generate(int seed, long index, int cx, int cz,
                @Nullable LongPredicate stillNeededParam) {
            final LongPredicate stillNeeded = (stillNeededParam != null) ? stillNeededParam : (i -> true);

            GeneratedBlockChunk blockChunk = new GeneratedBlockChunk(index, cx, cz);
            GeneratedBlockStateChunk blockStateChunk = new GeneratedBlockStateChunk();
            GeneratedEntityChunk entityChunk = new GeneratedEntityChunk();
            Holder<ChunkStore>[] sections = GeneratedChunk.makeSections();

            for (int x = 0; x < 32; x++) {
                int worldX = cx * 32 + x;
                for (int z = 0; z < 32; z++) {
                    int worldZ = cz * 32 + z;
                    blockChunk.setTint(x, z, this.tintId);
                    terrainGenerator.generateBaseTerrain(blockChunk, sections, worldX, worldZ, x, z);
                }

                if (x % 8 == 0 && !stillNeeded.test(index)) {
                    return CompletableFuture.completedFuture(null);
                }
            }

            prefabApplier.applyPrefabs(blockChunk, blockStateChunk, sections, cx, cz);

            GeneratedChunk gc = new GeneratedChunk(blockChunk, blockStateChunk, entityChunk, sections);
            return CompletableFuture.completedFuture(gc);
        }
    }
}
