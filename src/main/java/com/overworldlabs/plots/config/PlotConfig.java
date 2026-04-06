package com.overworldlabs.plots.config;

/**
 * Configuration for the plot system
 */
public class PlotConfig {

    public static class GeneralSettings {
        public String Language = "en_us";
        public int AutoSaveIntervalSeconds = 300;
    }

    public static class WorldSettings {
        public String PlotWorldName = "plotworld";
        public String DefaultWorldTime = "midday";
    }

    public static class PlotSettings {
        public int PlotSizeX = 32;
        public int PlotSizeZ = 32;
        public int RoadSizeX = 4;
        public int RoadSizeZ = 4;
        public int MaxPlotsDefault = 1;
        public int MaxPlotLimit = 50;
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

    public static class HologramSettings {
        public double HeightOffset = 2.0;
        public boolean Enabled = true;
        public String TitleColor = "#55ff55";
    }

    public static class SpawnSettings {
        public double X = 0;
        public double Y = 0;
        public double Z = 0;
        public float Pitch = 0;
        public float Yaw = 0;
        public boolean CustomSpawn = false;
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

    private GeneralSettings General = new GeneralSettings();
    private WorldSettings World = new WorldSettings();
    private PlotSettings Plots = new PlotSettings();
    private BlockSettings Blocks = new BlockSettings();
    private PrefabSettings Prefabs = new PrefabSettings();
    private HologramSettings Holograms = new HologramSettings();
    private SpawnSettings Spawn = new SpawnSettings();
    private DatabaseSettings Database = new DatabaseSettings();
    private EconomySettings Economy = new EconomySettings();

    public void overwriteWith(PlotConfig other) {
        if (other == null) {
            return;
        }
        this.General = other.General != null ? other.General : new GeneralSettings();
        this.World = other.World != null ? other.World : new WorldSettings();
        this.Plots = other.Plots != null ? other.Plots : new PlotSettings();
        this.Blocks = other.Blocks != null ? other.Blocks : new BlockSettings();
        this.Prefabs = other.Prefabs != null ? other.Prefabs : new PrefabSettings();
        this.Holograms = other.Holograms != null ? other.Holograms : new HologramSettings();
        this.Spawn = other.Spawn != null ? other.Spawn : new SpawnSettings();
        this.Database = other.Database != null ? other.Database : new DatabaseSettings();
        this.Economy = other.Economy != null ? other.Economy : new EconomySettings();
    }

    public static PlotConfig getDefault() {
        return new PlotConfig();
    }

    public SpawnSettings getSpawn() {
        return Spawn;
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

    public String getPlotWorldName() {
        return World.PlotWorldName;
    }

    public String getDefaultWorldTime() {
        return World.DefaultWorldTime;
    }

    public boolean isManagedWorld(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return false;
        }
        return worldName.equalsIgnoreCase(getPlotWorldName());
    }

    public int getMaxPlotsDefaultValue() {
        return Plots.MaxPlotsDefault;
    }

    public int getMaxPlotLimit() {
        return Plots.MaxPlotLimit;
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

    public int getPlotSizeX() {
        return Plots.PlotSizeX;
    }

    public int getPlotSizeZ() {
        return Plots.PlotSizeZ;
    }

    public int getRoadSizeX() {
        return Plots.RoadSizeX;
    }

    public int getRoadSizeZ() {
        return Plots.RoadSizeZ;
    }

    public void setPlotSizeX(int size) {
        Plots.PlotSizeX = size;
    }

    public void setPlotSizeZ(int size) {
        Plots.PlotSizeZ = size;
    }

    public void setRoadSizeX(int size) {
        Plots.RoadSizeX = size;
    }

    public void setRoadSizeZ(int size) {
        Plots.RoadSizeZ = size;
    }

    public String getRoadPrefab() {
        return Prefabs.Road;
    }

    public String getPlotPrefab() {
        return Prefabs.Plot;
    }

    public String getIntersectionPrefab() {
        return Prefabs.Intersection;
    }

    public int getPrefabOffsetX() {
        return Prefabs.OffsetX;
    }

    public int getPrefabOffsetY() {
        return Prefabs.OffsetY;
    }

    public int getPrefabOffsetZ() {
        return Prefabs.OffsetZ;
    }

    public boolean isPrefabAutoHeight() {
        return Prefabs.AutoHeight;
    }

    public boolean isPrefabPasteRoadOnTop() {
        return Prefabs.PasteRoadOnTop;
    }

    public boolean isPrefabPasteMismatches() {
        return Prefabs.PasteMismatches;
    }

    public int getPrefabRotation() {
        return Prefabs.Rotation;
    }

    public double getHologramHeightOffset() {
        return Holograms.HeightOffset;
    }

    public boolean isHologramEnabled() {
        return Holograms.Enabled;
    }

    public String getHologramTitleColor() {
        return Holograms.TitleColor;
    }

    private String normalizeBlockName(String name) {
        if (name == null || name.isEmpty())
            return "Empty";
        String lower = name.toLowerCase().trim();
        if (lower.equals("air") || lower.equals("hytale:air") || lower.equals("null")) {
            return "Empty";
        }
        return name;
    }

    public String getBedrockBlock() {
        return normalizeBlockName(Blocks.Bedrock);
    }

    public String getPlotSurfaceBlock() {
        return normalizeBlockName(Blocks.PlotSurface);
    }

    public String getPlotSubSurfaceBlock() {
        return normalizeBlockName(Blocks.PlotSubSurface);
    }

    public String getRoadSurfaceBlock() {
        return normalizeBlockName(Blocks.RoadSurface);
    }

    public String getBorderBlock() {
        return normalizeBlockName(Blocks.Border);
    }

    public String getFillingBlock() {
        return normalizeBlockName(Blocks.Filling);
    }

    /**
     * Convert world coordinate to grid coordinate
     */
    public int worldToGridX(int worldX) {
        int totalSize = getPlotSizeX() + getRoadSizeX();
        return (worldX >= 0) ? worldX / totalSize : (worldX - totalSize + 1) / totalSize;
    }

    public int worldToGridZ(int worldZ) {
        int totalSize = getPlotSizeZ() + getRoadSizeZ();
        return (worldZ >= 0) ? worldZ / totalSize : (worldZ - totalSize + 1) / totalSize;
    }

    public int gridToWorldX(int gridX) {
        return gridX * (getPlotSizeX() + getRoadSizeX());
    }

    public int gridToWorldZ(int gridZ) {
        return gridZ * (getPlotSizeZ() + getRoadSizeZ());
    }

    /**
     * Check if world coordinates are within a plot (not on a road) in the correct
     * world
     */
    public boolean isInPlot(String worldName, int worldX, int worldZ) {
        if (!isManagedWorld(worldName)) {
            return false;
        }
        int totalSizeX = getPlotSizeX() + getRoadSizeX();
        int totalSizeZ = getPlotSizeZ() + getRoadSizeZ();
        int localX = Math.floorMod(worldX, totalSizeX);
        int localZ = Math.floorMod(worldZ, totalSizeZ);
        return localX < getPlotSizeX() && localZ < getPlotSizeZ();
    }

    /**
     * Get the grid coordinates for a world position
     */
    public int[] getPlotGridAt(int worldX, int worldZ) {
        return new int[] { worldToGridX(worldX), worldToGridZ(worldZ) };
    }
}
