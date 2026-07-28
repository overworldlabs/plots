package dev.stoshe.plots.config;

/**
 * Read-only view of a single plot world's generation parameters (sizes, blocks,
 * prefabs) plus the coordinate math derived from them. Implemented by
 * {@link PlotConfig.WorldEntry} so the world generator and terrain services can
 * operate on a specific world without reaching back into the global config.
 */
public interface PlotWorldView {

    int getPlotSizeX();

    int getPlotSizeZ();

    int getRoadSizeX();

    int getRoadSizeZ();

    String getBedrockBlock();

    String getPlotSurfaceBlock();

    String getPlotSubSurfaceBlock();

    String getRoadSurfaceBlock();

    String getBorderBlock();

    String getFillingBlock();

    String getRoadPrefab();

    String getPlotPrefab();

    String getIntersectionPrefab();

    int getPrefabOffsetX();

    int getPrefabOffsetY();

    int getPrefabOffsetZ();

    boolean isPrefabAutoHeight();

    boolean isPrefabPasteRoadOnTop();

    boolean isPrefabPasteMismatches();

    int getPrefabRotation();

    /** Convert a world coordinate to a grid coordinate on the X axis. */
    default int worldToGridX(int worldX) {
        int totalSize = getPlotSizeX() + getRoadSizeX();
        return (worldX >= 0) ? worldX / totalSize : (worldX - totalSize + 1) / totalSize;
    }

    /** Convert a world coordinate to a grid coordinate on the Z axis. */
    default int worldToGridZ(int worldZ) {
        int totalSize = getPlotSizeZ() + getRoadSizeZ();
        return (worldZ >= 0) ? worldZ / totalSize : (worldZ - totalSize + 1) / totalSize;
    }

    default int gridToWorldX(int gridX) {
        return gridX * (getPlotSizeX() + getRoadSizeX());
    }

    default int gridToWorldZ(int gridZ) {
        return gridZ * (getPlotSizeZ() + getRoadSizeZ());
    }

    /** True when the world coordinates fall inside a plot cell (not on a road). */
    default boolean isInPlotLocal(int worldX, int worldZ) {
        int totalSizeX = getPlotSizeX() + getRoadSizeX();
        int totalSizeZ = getPlotSizeZ() + getRoadSizeZ();
        int localX = Math.floorMod(worldX, totalSizeX);
        int localZ = Math.floorMod(worldZ, totalSizeZ);
        return localX < getPlotSizeX() && localZ < getPlotSizeZ();
    }

    default int[] getPlotGridAt(int worldX, int worldZ) {
        return new int[] { worldToGridX(worldX), worldToGridZ(worldZ) };
    }
}
