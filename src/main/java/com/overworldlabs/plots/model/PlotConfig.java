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
        public int RoadSize = 4;
        public int MaxPlotsDefault = 1;
        public int MaxPlotLimit = 50;
    }

    private GeneralSettings General = new GeneralSettings();
    private WorldSettings World = new WorldSettings();
    private PlotSettings Plots = new PlotSettings();

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

    public int getPlotSize() {
        return Plots.PlotSize;
    }

    public int getRoadSize() {
        return Plots.RoadSize;
    }

    public int getMaxPlotsDefault() {
        return Plots.MaxPlotsDefault;
    }

    public int getMaxPlotLimit() {
        return Plots.MaxPlotLimit;
    }

    /**
     * Convert world coordinate to grid coordinate
     */
    public int worldToGrid(int worldCoord) {
        int totalSize = getPlotSize() + getRoadSize();
        if (worldCoord >= 0) {
            return worldCoord / totalSize;
        } else {
            return (worldCoord - totalSize + 1) / totalSize;
        }
    }

    /**
     * Convert grid coordinate to world coordinate (minimum corner)
     */
    public int gridToWorld(int gridCoord) {
        return gridCoord * (getPlotSize() + getRoadSize());
    }

    /**
     * Check if world coordinates are within a plot (not on a road) in the correct
     * world
     */
    public boolean isInPlot(String worldName, int worldX, int worldZ) {
        if (!worldName.equals(getPlotWorldName())) {
            return false;
        }
        int totalSize = getPlotSize() + getRoadSize();
        int localX = Math.floorMod(worldX, totalSize);
        int localZ = Math.floorMod(worldZ, totalSize);
        return localX < getPlotSize() && localZ < getPlotSize();
    }

    /**
     * Get the grid coordinates for a world position
     */
    public int[] getPlotGridAt(int worldX, int worldZ) {
        return new int[] { worldToGrid(worldX), worldToGrid(worldZ) };
    }
}






