package com.overworldlabs.plots.model;

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
        public int PlotSize = 32;
        public int PlotSizeX = 32;
        public int PlotSizeZ = 32;
        public int RoadSizeX = 4;
        public int RoadSizeZ = 4;
        public int RoadSize = 4;
        public int MaxPlotsDefault = 1;
        public int MaxPlotLimit = 50;
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
    }

    public static class HologramSettings {
        public double HeightOffset = 2.0;
        public boolean Enabled = true;
        public String TitleColor = "#55ff55";
    }

    private GeneralSettings General = new GeneralSettings();
    private WorldSettings World = new WorldSettings();
    private PlotSettings Plots = new PlotSettings();
    private BlockSettings Blocks = new BlockSettings();
    private PrefabSettings Prefabs = new PrefabSettings();
    private HologramSettings Holograms = new HologramSettings();

    public static PlotConfig getDefault() {
        return new PlotConfig();
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

    public int getMaxPlotsDefaultValue() {
        return Plots.MaxPlotsDefault;
    }

    public int getMaxPlotLimit() {
        return Plots.MaxPlotLimit;
    }

    public int getPlotSizeX() {
        return (Plots.PlotSize != 32 && Plots.PlotSizeX == 32) ? Plots.PlotSize : Plots.PlotSizeX;
    }

    public int getPlotSizeZ() {
        return (Plots.PlotSize != 32 && Plots.PlotSizeZ == 32) ? Plots.PlotSize : Plots.PlotSizeZ;
    }

    public int getRoadSizeX() {
        return (Plots.RoadSize != 4 && Plots.RoadSizeX == 4) ? Plots.RoadSize : Plots.RoadSizeX;
    }

    public int getRoadSizeZ() {
        return (Plots.RoadSize != 4 && Plots.RoadSizeZ == 4) ? Plots.RoadSize : Plots.RoadSizeZ;
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

    @Deprecated
    public int getPlotSize() {
        return getPlotSizeX();
    }

    @Deprecated
    public int getRoadSize() {
        return getRoadSizeX();
    }

    @Deprecated
    public void setPlotSize(int size) {
        Plots.PlotSize = Plots.PlotSizeX = Plots.PlotSizeZ = size;
    }

    @Deprecated
    public void setRoadSize(int size) {
        Plots.RoadSize = Plots.RoadSizeX = Plots.RoadSizeZ = size;
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
        if (!worldName.equals(getPlotWorldName())) {
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
