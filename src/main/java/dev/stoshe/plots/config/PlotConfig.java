package dev.stoshe.plots.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for the plot system.
 *
 * <p>The config is split into <b>global</b> sections (general, limits, holograms,
 * database, economy) and a map of <b>plot worlds</b> ({@link #Worlds}), each
 * described by a {@link WorldEntry} bundling its plot/road sizes, blocks,
 * prefabs and spawn. Legacy single-world configs (flat {@code World}/{@code Plots}
 * /{@code Blocks}/{@code Prefabs}/{@code Spawn} sections) are migrated into a
 * single world entry on load via {@link #normalizeAfterLoad()}.</p>
 *
 * <p>Most legacy getters/setters are preserved and delegate to the <b>default
 * world</b> (the first/legacy world) so existing call sites keep working; new
 * world-aware overloads take a world name.</p>
 */
public class PlotConfig {

    // ---------------------------------------------------------------- Global

    public static class GeneralSettings {
        public String Language = "en_us";
        public int AutoSaveIntervalSeconds = 300;
    }

    /** Global plot-limit policy. Per-world budgets may override {@link #PerWorldDefault}. */
    public static class LimitSettings {
        /** Total plots a player may own across ALL worlds. -1 = unlimited. */
        public int GlobalSharedDefault = -1;
        /** Default budget per world when a world entry doesn't override it. */
        public int PerWorldDefault = 1;
        /** Absolute hard ceiling on total plots a non-admin can own. */
        public int HardCap = 50;
    }

    public static class HologramSettings {
        public double HeightOffset = 2.0;
        public boolean Enabled = true;
        public String TitleColor = "#55ff55";
    }

    public static class DatabaseSettings {
        public boolean Enabled = false;
        public String JdbcUrl = "jdbc:sqlite:plots.db";
        public String Username = "";
        public String Password = "";
        public int MaxPoolSize = 10;
    }

    public static class EconomySettings {
        public boolean Enabled = false;
        public String Provider = "auto";
        public CostsSettings Costs = new CostsSettings();
    }

    public static class CostsSettings {
        public double Claim = 0.0;
        public double AutoClaim = 0.0;
        public double Merge = 0.0;
        public double Unmerge = 0.0;
    }

    // ------------------------------------------------------------ Per-world

    public static class PlotSettings {
        public int PlotSizeX = 32;
        public int PlotSizeZ = 32;
        public int RoadSizeX = 4;
        public int RoadSizeZ = 4;
        public int MaxWarpsPerPlot = 5;
        public String DefaultPlotName = "%owner%'s Plot";
    }

    public static class BlockSettings {
        public String Bedrock = "Rock_Bedrock";
        public String PlotSurface = "Soil_Grass";
        public String PlotSubSurface = "Soil_Dirt";
        public String RoadSurface = "Rock_Stone_Cobble";
        public String Border = "Rock_calcite_brick_smooth_half";
        public String Filling = "Rock_Stone";
    }

    public static class PrefabSettings {
        public String Road = "";
        public String Plot = "";
        public String Intersection = "";
        public boolean PasteRoadOnTop = true;
        public int OffsetX = 0;
        public int OffsetY = 0;
        public int OffsetZ = 0;
        public boolean AutoHeight = true;
        public boolean PasteMismatches = true;
        public int Rotation = 0;
    }

    public static class SpawnSettings {
        public double X = 0;
        public double Y = 0;
        public double Z = 0;
        public float Pitch = 0;
        public float Yaw = 0;
        public boolean CustomSpawn = false;
    }

    /** One plot world's full configuration. */
    public static class WorldEntry implements PlotWorldView {
        /** Sanitized permission segment, e.g. {@code plots.<PermissionKey>.limit.N}. */
        public String PermissionKey;
        public String DefaultWorldTime = "midday";
        /** Per-world plot budget; -1 = inherit {@link LimitSettings#PerWorldDefault}. */
        public int MaxPlotsDefault = -1;
        public PlotSettings Plots = new PlotSettings();
        public BlockSettings Blocks = new BlockSettings();
        public PrefabSettings Prefabs = new PrefabSettings();
        public SpawnSettings Spawn = new SpawnSettings();

        void ensureSections() {
            if (Plots == null) Plots = new PlotSettings();
            if (Blocks == null) Blocks = new BlockSettings();
            if (Prefabs == null) Prefabs = new PrefabSettings();
            if (Spawn == null) Spawn = new SpawnSettings();
        }

        public String getPermissionKey() {
            return (PermissionKey != null && !PermissionKey.isBlank()) ? PermissionKey : "world";
        }

        public String getDefaultWorldTime() {
            return (DefaultWorldTime != null && !DefaultWorldTime.isBlank()) ? DefaultWorldTime : "midday";
        }

        public int getMaxWarpsPerPlot() {
            return Plots.MaxWarpsPerPlot > 0 ? Plots.MaxWarpsPerPlot : 5;
        }

        public String getDefaultPlotNameTemplate() {
            if (Plots.DefaultPlotName == null || Plots.DefaultPlotName.isBlank()) {
                return "%owner%'s Plot";
            }
            return Plots.DefaultPlotName;
        }

        public String formatDefaultPlotName(String ownerName, int gridX, int gridZ) {
            String owner = ownerName != null && !ownerName.isBlank() ? ownerName : "Player";
            String formatted = getDefaultPlotNameTemplate()
                    .replace("%owner%", owner)
                    .replace("%x%", String.valueOf(gridX))
                    .replace("%z%", String.valueOf(gridZ));
            return formatted.isBlank() ? "Plot (" + gridX + ", " + gridZ + ")" : formatted;
        }

        @Override public int getPlotSizeX() { return Plots.PlotSizeX; }
        @Override public int getPlotSizeZ() { return Plots.PlotSizeZ; }
        @Override public int getRoadSizeX() { return Plots.RoadSizeX; }
        @Override public int getRoadSizeZ() { return Plots.RoadSizeZ; }

        @Override public String getBedrockBlock() { return normalizeBlockName(Blocks.Bedrock); }
        @Override public String getPlotSurfaceBlock() { return normalizeBlockName(Blocks.PlotSurface); }
        @Override public String getPlotSubSurfaceBlock() { return normalizeBlockName(Blocks.PlotSubSurface); }
        @Override public String getRoadSurfaceBlock() { return normalizeBlockName(Blocks.RoadSurface); }
        @Override public String getBorderBlock() { return normalizeBlockName(Blocks.Border); }
        @Override public String getFillingBlock() { return normalizeBlockName(Blocks.Filling); }

        @Override public String getRoadPrefab() { return Prefabs.Road; }
        @Override public String getPlotPrefab() { return Prefabs.Plot; }
        @Override public String getIntersectionPrefab() { return Prefabs.Intersection; }
        @Override public int getPrefabOffsetX() { return Prefabs.OffsetX; }
        @Override public int getPrefabOffsetY() { return Prefabs.OffsetY; }
        @Override public int getPrefabOffsetZ() { return Prefabs.OffsetZ; }
        @Override public boolean isPrefabAutoHeight() { return Prefabs.AutoHeight; }
        @Override public boolean isPrefabPasteRoadOnTop() { return Prefabs.PasteRoadOnTop; }
        @Override public boolean isPrefabPasteMismatches() { return Prefabs.PasteMismatches; }
        @Override public int getPrefabRotation() { return Prefabs.Rotation; }
    }

    // -------------------------------------------------------- Legacy (flat)

    /** @deprecated single-world legacy section; only read during migration. */
    @Deprecated
    public static class WorldSettings {
        public String PlotWorldName = "plotworld";
        public String DefaultWorldTime = "midday";
    }

    // ------------------------------------------------------------- Fields

    private GeneralSettings General = new GeneralSettings();
    private LimitSettings Limits = new LimitSettings();
    private HologramSettings Holograms = new HologramSettings();
    private DatabaseSettings Database = new DatabaseSettings();
    private EconomySettings Economy = new EconomySettings();
    private LinkedHashMap<String, WorldEntry> Worlds = new LinkedHashMap<>();

    // Legacy flat sections kept only so old config.json migrates cleanly.
    @Deprecated private WorldSettings World;
    @Deprecated private PlotSettings Plots;
    @Deprecated private BlockSettings Blocks;
    @Deprecated private PrefabSettings Prefabs;
    @Deprecated private SpawnSettings Spawn;

    // Runtime caches (not serialized).
    private transient volatile Map<String, WorldEntry> worldsByLower;
    private transient volatile Set<String> managedLower;
    private transient volatile String legacyWorldName;
    private transient volatile boolean normalized;

    // ------------------------------------------------------- Normalization

    public static PlotConfig getDefault() {
        PlotConfig c = new PlotConfig();
        c.normalizeAfterLoad();
        return c;
    }

    /**
     * Migrates a legacy flat config into the {@link #Worlds} map (if empty) and
     * rebuilds the lookup caches. Idempotent.
     */
    public synchronized void normalizeAfterLoad() {
        if (Worlds == null) {
            Worlds = new LinkedHashMap<>();
        }
        // Only synthesize a world entry when migrating a legacy flat config. A
        // brand-new install (no legacy sections at all) is intentionally left with
        // an EMPTY Worlds map so no plot world is auto-created on startup — an admin
        // must create one explicitly (/plot admin).
        boolean hasLegacyData = World != null || this.Plots != null || this.Blocks != null
                || this.Prefabs != null || this.Spawn != null;
        if (Worlds.isEmpty() && hasLegacyData) {
            String name = (World != null && World.PlotWorldName != null && !World.PlotWorldName.isBlank())
                    ? World.PlotWorldName
                    : "plotworld";
            WorldEntry e = new WorldEntry();
            e.DefaultWorldTime = (World != null && World.DefaultWorldTime != null && !World.DefaultWorldTime.isBlank())
                    ? World.DefaultWorldTime
                    : "midday";
            if (this.Plots != null) e.Plots = this.Plots;
            if (this.Blocks != null) e.Blocks = this.Blocks;
            if (this.Prefabs != null) e.Prefabs = this.Prefabs;
            if (this.Spawn != null) e.Spawn = this.Spawn;
            Worlds.put(name, e);
        }
        // Legacy flat sections are now folded in; drop them so re-save emits the new shape.
        this.World = null;
        this.Plots = null;
        this.Blocks = null;
        this.Prefabs = null;
        this.Spawn = null;
        if (Limits == null) Limits = new LimitSettings();

        for (Map.Entry<String, WorldEntry> en : Worlds.entrySet()) {
            WorldEntry e = en.getValue();
            if (e == null) {
                e = new WorldEntry();
                en.setValue(e);
            }
            e.ensureSections();
            if (e.PermissionKey == null || e.PermissionKey.isBlank()) {
                e.PermissionKey = sanitizePermKey(en.getKey());
            }
        }
        rebuildCaches();
        normalized = true;
    }

    private void rebuildCaches() {
        Map<String, WorldEntry> byLower = new LinkedHashMap<>();
        Set<String> managed = new java.util.HashSet<>();
        String first = null;
        for (Map.Entry<String, WorldEntry> en : Worlds.entrySet()) {
            if (first == null) first = en.getKey();
            byLower.put(en.getKey().toLowerCase(), en.getValue());
            managed.add(en.getKey().toLowerCase());
        }
        this.worldsByLower = Collections.unmodifiableMap(byLower);
        this.managedLower = Collections.unmodifiableSet(managed);
        this.legacyWorldName = first;
    }

    private void ensureNormalized() {
        if (!normalized || worldsByLower == null) {
            normalizeAfterLoad();
        }
    }

    public static String sanitizePermKey(String name) {
        if (name == null) return "world";
        String s = name.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return s.isBlank() ? "world" : s;
    }

    private static String normalizeBlockName(String name) {
        if (name == null || name.isEmpty()) return "Empty";
        String lower = name.toLowerCase().trim();
        if (lower.equals("air") || lower.equals("hytale:air") || lower.equals("null")) {
            return "Empty";
        }
        return name;
    }

    // ----------------------------------------------------- World accessors

    /** The first/legacy world name (the implicit "default" world). */
    public String getDefaultWorldName() {
        ensureNormalized();
        return legacyWorldName;
    }

    /** Returns the entry for {@code worldName}, or the default world if unknown/null. */
    public WorldEntry world(String worldName) {
        ensureNormalized();
        if (worldName != null) {
            WorldEntry e = worldsByLower.get(worldName.toLowerCase());
            if (e != null) return e;
        }
        return defaultEntry();
    }

    /** Returns the entry for {@code worldName} or {@code null} if it is not a managed world. */
    public WorldEntry worldOrNull(String worldName) {
        ensureNormalized();
        return worldName == null ? null : worldsByLower.get(worldName.toLowerCase());
    }

    private WorldEntry defaultEntry() {
        WorldEntry e = (legacyWorldName != null) ? worldsByLower.get(legacyWorldName.toLowerCase()) : null;
        if (e == null && !Worlds.isEmpty()) e = Worlds.values().iterator().next();
        if (e == null) {
            e = new WorldEntry();
            e.ensureSections();
        }
        return e;
    }

    public Map<String, WorldEntry> getWorlds() {
        ensureNormalized();
        return Worlds;
    }

    public List<String> getWorldNames() {
        ensureNormalized();
        return new ArrayList<>(Worlds.keySet());
    }

    public boolean isManagedWorld(String worldName) {
        if (worldName == null || worldName.isEmpty()) return false;
        ensureNormalized();
        return managedLower.contains(worldName.toLowerCase());
    }

    /** Adds (or replaces) a world entry and rebuilds caches. */
    public synchronized void putWorld(String name, WorldEntry entry) {
        ensureNormalized();
        if (name == null || name.isBlank() || entry == null) return;
        entry.ensureSections();
        if (entry.PermissionKey == null || entry.PermissionKey.isBlank()) {
            entry.PermissionKey = sanitizePermKey(name);
        }
        Worlds.put(name, entry);
        rebuildCaches();
    }

    /** Removes a world entry (by name, case-insensitive) and rebuilds caches. */
    public synchronized boolean removeWorld(String name) {
        ensureNormalized();
        if (name == null) return false;
        String match = null;
        for (String key : Worlds.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                match = key;
                break;
            }
        }
        if (match == null) return false;
        Worlds.remove(match);
        rebuildCaches();
        return true;
    }

    // ----------------------------------------------------- Global getters

    /**
     * Replaces this config's contents with another's (used by in-place reloads so
     * existing references stay valid). Synchronized and rebuilds the lookup caches
     * atomically, matching the rest of the world-mutation API.
     *
     * @param other the source config to copy from; ignored when {@code null}
     */
    public synchronized void overwriteWith(PlotConfig other) {
        if (other == null) return;
        other.ensureNormalized();
        this.General = other.General != null ? other.General : new GeneralSettings();
        this.Limits = other.Limits != null ? other.Limits : new LimitSettings();
        this.Holograms = other.Holograms != null ? other.Holograms : new HologramSettings();
        this.Database = other.Database != null ? other.Database : new DatabaseSettings();
        this.Economy = other.Economy != null ? other.Economy : new EconomySettings();
        this.Worlds = new LinkedHashMap<>(other.Worlds);
        this.World = null;
        this.Plots = null;
        this.Blocks = null;
        this.Prefabs = null;
        this.Spawn = null;
        rebuildCaches();
        this.normalized = true;
    }

    public LimitSettings getLimits() {
        if (Limits == null) Limits = new LimitSettings();
        return Limits;
    }

    public int getGlobalSharedDefault() {
        return getLimits().GlobalSharedDefault;
    }

    /**
     * The global default per-world plot budget. A negative value means unlimited.
     *
     * @return the per-world default budget
     */
    public int getPerWorldDefault() {
        return getLimits().PerWorldDefault;
    }

    /**
     * The absolute hard cap on total plots a non-admin may own. A negative value
     * means unlimited; {@code 0} blocks all non-admin claims (consistent with the
     * per-world and global budgets, where only negatives are unlimited).
     *
     * @return the effective hard cap
     */
    public int getHardCap() {
        int cap = getLimits().HardCap;
        return cap < 0 ? Integer.MAX_VALUE : cap;
    }

    public SpawnSettings getSpawn() {
        return world(getDefaultWorldName()).Spawn;
    }

    public SpawnSettings getSpawn(String worldName) {
        return world(worldName).Spawn;
    }

    public DatabaseSettings getDatabase() {
        return Database;
    }

    public EconomySettings getEconomy() {
        return Economy;
    }

    public boolean isEconomyEnabled() {
        return Economy != null && Economy.Enabled;
    }

    public String getEconomyProvider() {
        if (Economy == null || Economy.Provider == null || Economy.Provider.trim().isEmpty()) {
            return "auto";
        }
        return Economy.Provider.trim();
    }

    public double getEconomyCostClaim() {
        return Economy != null && Economy.Costs != null ? Economy.Costs.Claim : 0.0;
    }

    public double getEconomyCostAutoClaim() {
        return Economy != null && Economy.Costs != null ? Economy.Costs.AutoClaim : 0.0;
    }

    public double getEconomyCostMerge() {
        return Economy != null && Economy.Costs != null ? Economy.Costs.Merge : 0.0;
    }

    public double getEconomyCostUnmerge() {
        return Economy != null && Economy.Costs != null ? Economy.Costs.Unmerge : 0.0;
    }

    public String getLanguage() {
        return General.Language;
    }

    public int getAutoSaveIntervalSeconds() {
        return General.AutoSaveIntervalSeconds;
    }

    public String getDefaultWorldTime() {
        return world(getDefaultWorldName()).getDefaultWorldTime();
    }

    public String getDefaultWorldTime(String worldName) {
        return world(worldName).getDefaultWorldTime();
    }

    // ------------------------------------------------------- Limit setters

    /**
     * Sets the global default per-world plot budget.
     *
     * @param value the new per-world default
     */
    public void setMaxPlotsDefaultValue(int value) {
        getLimits().PerWorldDefault = value;
    }

    /**
     * Sets the absolute hard cap on total plots a non-admin may own.
     *
     * @param value the new hard cap
     */
    public void setMaxPlotLimit(int value) {
        getLimits().HardCap = value;
    }

    // -------------------------------- Per-world warp/name (legacy + world)

    public int getMaxWarpsPerPlot() {
        return world(getDefaultWorldName()).getMaxWarpsPerPlot();
    }

    public int getMaxWarpsPerPlot(String worldName) {
        return world(worldName).getMaxWarpsPerPlot();
    }

    public void setMaxWarpsPerPlot(int max) {
        world(getDefaultWorldName()).Plots.MaxWarpsPerPlot = max;
    }

    public void setDefaultPlotNameTemplate(String template) {
        if (template != null && !template.isBlank()) {
            world(getDefaultWorldName()).Plots.DefaultPlotName = template;
        }
    }

    public String getDefaultPlotNameTemplate() {
        return world(getDefaultWorldName()).getDefaultPlotNameTemplate();
    }

    public String formatDefaultPlotName(String ownerName, int gridX, int gridZ) {
        return world(getDefaultWorldName()).formatDefaultPlotName(ownerName, gridX, gridZ);
    }

    public String formatDefaultPlotName(String worldName, String ownerName, int gridX, int gridZ) {
        return world(worldName).formatDefaultPlotName(ownerName, gridX, gridZ);
    }

    public void setHologramEnabled(boolean enabled) {
        Holograms.Enabled = enabled;
    }

    public void setEconomyEnabled(boolean enabled) {
        Economy.Enabled = enabled;
    }

    // ------------------------- Sizes/blocks/prefabs (legacy → default world)

    public int getPlotSizeX() { return world(getDefaultWorldName()).getPlotSizeX(); }
    public int getPlotSizeZ() { return world(getDefaultWorldName()).getPlotSizeZ(); }
    public int getRoadSizeX() { return world(getDefaultWorldName()).getRoadSizeX(); }
    public int getRoadSizeZ() { return world(getDefaultWorldName()).getRoadSizeZ(); }

    public void setPlotSizeX(int size) { world(getDefaultWorldName()).Plots.PlotSizeX = size; }
    public void setPlotSizeZ(int size) { world(getDefaultWorldName()).Plots.PlotSizeZ = size; }
    public void setRoadSizeX(int size) { world(getDefaultWorldName()).Plots.RoadSizeX = size; }
    public void setRoadSizeZ(int size) { world(getDefaultWorldName()).Plots.RoadSizeZ = size; }

    public String getRoadPrefab() { return world(getDefaultWorldName()).getRoadPrefab(); }
    public String getPlotPrefab() { return world(getDefaultWorldName()).getPlotPrefab(); }
    public String getIntersectionPrefab() { return world(getDefaultWorldName()).getIntersectionPrefab(); }
    public int getPrefabOffsetX() { return world(getDefaultWorldName()).getPrefabOffsetX(); }
    public int getPrefabOffsetY() { return world(getDefaultWorldName()).getPrefabOffsetY(); }
    public int getPrefabOffsetZ() { return world(getDefaultWorldName()).getPrefabOffsetZ(); }
    public boolean isPrefabAutoHeight() { return world(getDefaultWorldName()).isPrefabAutoHeight(); }
    public boolean isPrefabPasteRoadOnTop() { return world(getDefaultWorldName()).isPrefabPasteRoadOnTop(); }
    public boolean isPrefabPasteMismatches() { return world(getDefaultWorldName()).isPrefabPasteMismatches(); }
    public int getPrefabRotation() { return world(getDefaultWorldName()).getPrefabRotation(); }

    public double getHologramHeightOffset() { return Holograms.HeightOffset; }
    public boolean isHologramEnabled() { return Holograms.Enabled; }
    public String getHologramTitleColor() { return Holograms.TitleColor; }

    public String getBedrockBlock() { return world(getDefaultWorldName()).getBedrockBlock(); }
    public String getPlotSurfaceBlock() { return world(getDefaultWorldName()).getPlotSurfaceBlock(); }
    public String getPlotSubSurfaceBlock() { return world(getDefaultWorldName()).getPlotSubSurfaceBlock(); }
    public String getRoadSurfaceBlock() { return world(getDefaultWorldName()).getRoadSurfaceBlock(); }
    public String getBorderBlock() { return world(getDefaultWorldName()).getBorderBlock(); }
    public String getFillingBlock() { return world(getDefaultWorldName()).getFillingBlock(); }

    // ------------------------------------------- Coordinate math (per-world)

    public int worldToGridX(int worldX) { return world(getDefaultWorldName()).worldToGridX(worldX); }
    public int worldToGridZ(int worldZ) { return world(getDefaultWorldName()).worldToGridZ(worldZ); }
    public int gridToWorldX(int gridX) { return world(getDefaultWorldName()).gridToWorldX(gridX); }
    public int gridToWorldZ(int gridZ) { return world(getDefaultWorldName()).gridToWorldZ(gridZ); }

    public int worldToGridX(String worldName, int worldX) { return world(worldName).worldToGridX(worldX); }
    public int worldToGridZ(String worldName, int worldZ) { return world(worldName).worldToGridZ(worldZ); }
    public int gridToWorldX(String worldName, int gridX) { return world(worldName).gridToWorldX(gridX); }
    public int gridToWorldZ(String worldName, int gridZ) { return world(worldName).gridToWorldZ(gridZ); }

    /**
     * Check if world coordinates are within a plot (not on a road) in a managed world.
     */
    public boolean isInPlot(String worldName, int worldX, int worldZ) {
        WorldEntry e = worldOrNull(worldName);
        if (e == null) return false;
        return e.isInPlotLocal(worldX, worldZ);
    }

    /** Grid coordinates for a world position in the default world. */
    public int[] getPlotGridAt(int worldX, int worldZ) {
        return world(getDefaultWorldName()).getPlotGridAt(worldX, worldZ);
    }

    /** Grid coordinates for a world position in a specific world. */
    public int[] getPlotGridAt(String worldName, int worldX, int worldZ) {
        return world(worldName).getPlotGridAt(worldX, worldZ);
    }
}
