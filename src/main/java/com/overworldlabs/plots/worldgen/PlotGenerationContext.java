package com.overworldlabs.plots.worldgen;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.model.Prefab;
import com.overworldlabs.plots.util.ConsoleColors;

/**
 * Data Transfer Object containing a snapshot of the generation requirements.
 * Caches block and fluid IDs to avoid repetitive lookups during chunk
 * generation.
 */
public class PlotGenerationContext {
    private final PlotConfig config;
    private final int bedrockBlockId;
    private final int grassBlockId;
    private final int dirtBlockId;
    private final int stoneBlockId;
    private final int roadBlockId;
    private final int borderBlockId;
    private final int environmentId;
    private final int groundHeight = 64;
    /** Guaranteed-valid (non-zero) block id used when a configured name can't be resolved. */
    private final int safeDefaultBlockId;

    private final Prefab roadPrefab;
    private final Prefab plotPrefab;
    private final Prefab intersectionPrefab;

    public PlotGenerationContext(PlotConfig config) {
        this.config = (config != null) ? config : PlotConfig.getDefault();

        // Load prefabs
        var pm = Plots.getInstance().getPrefabManager();
        this.roadPrefab = pm.getOrLoadPrefab(this.config.getRoadPrefab());
        this.plotPrefab = pm.getOrLoadPrefab(this.config.getPlotPrefab());
        this.intersectionPrefab = pm.getOrLoadPrefab(this.config.getIntersectionPrefab());

        // Cache Block IDs. Block id 0 is invalid ("air") and, if written into a
        // generated chunk, corrupts the global item asset registry (an item ends
        // up referencing block 0, which crashes asset sending on player join).
        // So every terrain block resolves to a guaranteed non-zero block id.
        this.safeDefaultBlockId = computeSafeDefaultBlockId();
        this.bedrockBlockId = resolveBlock(this.config.getBedrockBlock());
        this.grassBlockId = resolveBlock(this.config.getPlotSurfaceBlock());
        this.dirtBlockId = resolveBlock(this.config.getPlotSubSurfaceBlock());
        this.stoneBlockId = resolveBlock(this.config.getFillingBlock());
        this.roadBlockId = resolveBlock(this.config.getRoadSurfaceBlock());
        this.borderBlockId = resolveBlock(this.config.getBorderBlock());

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

    private int getBlockIdFromAsset(String name, int fallback) {
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

    /** Resolves a configured block name to a valid id, never returning 0/invalid. */
    private int resolveBlock(String name) {
        int id = getBlockIdFromAsset(name, -1);
        if (id > 0) {
            return id;
        }
        ConsoleColors.warning("Plot world block '" + name + "' not found on this server build; "
                + "using a safe default block instead. Update the Blocks section of config.json.");
        return safeDefaultBlockId;
    }

    /** Finds a guaranteed-valid (non-zero) block id to use as a last-resort terrain block. */
    private int computeSafeDefaultBlockId() {
        try {
            var assetMap = BlockType.getAssetMap();
            if (assetMap != null) {
                for (String candidate : new String[] { "Rock_Stone", "Stone", "Soil_Dirt", "Dirt", "Rock_Bedrock",
                        "Soil_Grass" }) {
                    int id = assetMap.getIndex(candidate);
                    if (id != Integer.MIN_VALUE && id > 0) {
                        return id;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        // Absolute last resort: id 1 is a real block (0 is air/invalid).
        return 1;
    }

    public int getFluidIdByName(String name) {
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("Empty"))
            return -1;
        try {
            var assetMap = Fluid.getAssetMap();
            if (assetMap == null)
                return -1;
            int id = assetMap.getIndex(name);
            if (id == Integer.MIN_VALUE
                    && (name.toLowerCase().contains("water") || name.toLowerCase().contains("lava"))) {
                return (name.toLowerCase().contains("water")) ? 1 : 2;
            }
            return (id == Integer.MIN_VALUE) ? -1 : id;
        } catch (Exception e) {
            return -1;
        }
    }

    // Getters
    public PlotConfig getConfig() {
        return config;
    }

    public int getBedrockBlockId() {
        return bedrockBlockId;
    }

    public int getGrassBlockId() {
        return grassBlockId;
    }

    public int getDirtBlockId() {
        return dirtBlockId;
    }

    public int getStoneBlockId() {
        return stoneBlockId;
    }

    public int getRoadBlockId() {
        return roadBlockId;
    }

    public int getBorderBlockId() {
        return borderBlockId;
    }

    public int getEnvironmentId() {
        return environmentId;
    }

    public int getGroundHeight() {
        return groundHeight;
    }

    public Prefab getRoadPrefab() {
        return roadPrefab;
    }

    public Prefab getPlotPrefab() {
        return plotPrefab;
    }

    public Prefab getIntersectionPrefab() {
        return intersectionPrefab;
    }

    public int getPlotSizeX() {
        return config.getPlotSizeX();
    }

    public int getPlotSizeZ() {
        return config.getPlotSizeZ();
    }

    public int getRoadSizeX() {
        return config.getRoadSizeX();
    }

    public int getRoadSizeZ() {
        return config.getRoadSizeZ();
    }

    public int getTotalSizeX() {
        return getPlotSizeX() + getRoadSizeX();
    }

    public int getTotalSizeZ() {
        return getPlotSizeZ() + getRoadSizeZ();
    }
}
