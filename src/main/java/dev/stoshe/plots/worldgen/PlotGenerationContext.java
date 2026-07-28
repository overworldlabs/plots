package dev.stoshe.plots.worldgen;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.model.Prefab;
import dev.stoshe.plots.util.ConsoleColors;

/**
 * Data Transfer Object containing a snapshot of the generation requirements.
 * Caches block and fluid IDs to avoid repetitive lookups during chunk
 * generation.
 */
public class PlotGenerationContext {
    private final PlotConfig config;
    private final PlotConfig.WorldEntry view;
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

    /**
     * Builds a generation context for one specific plot world.
     *
     * @param config the global config (exposed via {@link #getConfig()} for
     *               world-gen override consumers); defaults are used when null
     * @param view   the per-world generation parameters (sizes/blocks/prefabs/
     *               spawn); the config's default world is used when null
     */
    public PlotGenerationContext(PlotConfig config, PlotConfig.WorldEntry view) {
        this.config = (config != null) ? config : PlotConfig.getDefault();
        this.view = (view != null) ? view : this.config.world(this.config.getDefaultWorldName());

        // Load prefabs
        var pm = Plots.getInstance().getPrefabManager();
        this.roadPrefab = pm.getOrLoadPrefab(this.view.getRoadPrefab());
        this.plotPrefab = pm.getOrLoadPrefab(this.view.getPlotPrefab());
        this.intersectionPrefab = pm.getOrLoadPrefab(this.view.getIntersectionPrefab());

        // Cache Block IDs. Block id 0 is invalid ("air") and, if written into a
        // generated chunk, corrupts the global item asset registry (an item ends
        // up referencing block 0, which crashes asset sending on player join).
        // So every terrain block resolves to a guaranteed non-zero block id.
        this.safeDefaultBlockId = computeSafeDefaultBlockId();
        this.bedrockBlockId = resolveBlock(this.view.getBedrockBlock());
        this.grassBlockId = resolveBlock(this.view.getPlotSurfaceBlock());
        this.dirtBlockId = resolveBlock(this.view.getPlotSubSurfaceBlock());
        this.stoneBlockId = resolveBlock(this.view.getFillingBlock());
        this.roadBlockId = resolveBlock(this.view.getRoadSurfaceBlock());
        this.borderBlockId = resolveBlock(this.view.getBorderBlock());

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

    /** The global config (for world-gen override consumers). */
    public PlotConfig getConfig() {
        return config;
    }

    /** The per-world generation parameters this context was built for. */
    public PlotConfig.WorldEntry getView() {
        return view;
    }

    /** The per-world spawn settings. */
    public PlotConfig.SpawnSettings getSpawnSettings() {
        return view.Spawn;
    }

    /** Whether prefabs are pasted at the ground height baseline for this world. */
    public boolean isPrefabAutoHeight() {
        return view.isPrefabAutoHeight();
    }

    /** Whether prefabs larger than the plot are still pasted for this world. */
    public boolean isPrefabPasteMismatches() {
        return view.isPrefabPasteMismatches();
    }

    /** Whether road/intersection prefabs are pasted on top of the ground. */
    public boolean isPrefabPasteRoadOnTop() {
        return view.isPrefabPasteRoadOnTop();
    }

    /** The plot prefab X offset for this world. */
    public int getPrefabOffsetX() {
        return view.getPrefabOffsetX();
    }

    /** The plot prefab Y offset for this world. */
    public int getPrefabOffsetY() {
        return view.getPrefabOffsetY();
    }

    /** The plot prefab Z offset for this world. */
    public int getPrefabOffsetZ() {
        return view.getPrefabOffsetZ();
    }

    /** The plot prefab rotation (degrees) for this world. */
    public int getPrefabRotation() {
        return view.getPrefabRotation();
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
        return view.getPlotSizeX();
    }

    public int getPlotSizeZ() {
        return view.getPlotSizeZ();
    }

    public int getRoadSizeX() {
        return view.getRoadSizeX();
    }

    public int getRoadSizeZ() {
        return view.getRoadSizeZ();
    }

    public int getTotalSizeX() {
        return getPlotSizeX() + getRoadSizeX();
    }

    public int getTotalSizeZ() {
        return getPlotSizeZ() + getRoadSizeZ();
    }
}
