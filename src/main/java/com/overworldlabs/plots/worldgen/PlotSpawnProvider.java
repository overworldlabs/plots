package com.overworldlabs.plots.worldgen;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.model.Prefab;

/**
 * Handles the calculation of spawn points for plot worlds.
 */
public class PlotSpawnProvider {

    private final PlotGenerationContext ctx;

    public PlotSpawnProvider(PlotGenerationContext ctx) {
        this.ctx = ctx;
    }

    public Transform[] getSpawnPoints() {
        // Priority 1: Custom spawn point set by command
        PlotConfig.SpawnSettings customSpawn = ctx.getConfig().getSpawn();
        if (customSpawn != null && customSpawn.CustomSpawn) {
            return new Transform[] { new Transform(
                    new Vector3d(customSpawn.X, customSpawn.Y, customSpawn.Z),
                    new Vector3f(customSpawn.Pitch, customSpawn.Yaw, 0)) };
        }

        // Priority 2: Try to find a safe floor in the plot prefab
        if (ctx.getPlotPrefab() != null && ctx.getPlotPrefab().getBlocks() != null) {
            int highestSolidY = -1;
            int centerX = ctx.getPlotSizeX() / 2;
            int centerZ = ctx.getPlotSizeZ() / 2;

            for (Prefab.PrefabBlock block : ctx.getPlotPrefab().getBlocks()) {
                int relX = block.getX() - ctx.getPlotPrefab().getMinX();
                int relZ = block.getZ() - ctx.getPlotPrefab().getMinZ();
                int relY = block.getY() - ctx.getPlotPrefab().getMinY();

                if (Math.abs(relX - centerX) <= 3 && Math.abs(relZ - centerZ) <= 3) {
                    if (isSafeFloor(block.getName()) && relY > highestSolidY) {
                        highestSolidY = relY;
                    }
                }
            }

            if (highestSolidY != -1) {
                int baseHeight = ctx.getConfig().isPrefabAutoHeight() ? ctx.getGroundHeight() : 1;
                return new Transform[] { new Transform(
                        new Vector3d(centerX + 0.5, baseHeight + highestSolidY + 1.5, centerZ + 0.5),
                        new Vector3f(0f, 0f, 0f)) };
            }
        }

        // Priority 3: Default to intersection center
        double spawnX = ctx.getPlotSizeX() + (ctx.getRoadSizeX() / 2.0);
        double spawnZ = ctx.getPlotSizeZ() + (ctx.getRoadSizeZ() / 2.0);
        double spawnY = ctx.getGroundHeight() + 1.5;

        return new Transform[] {
                new Transform(new Vector3d(spawnX + 0.5, spawnY, spawnZ + 0.5), new Vector3f(0f, 0f, 0f)) };
    }

    private boolean isSafeFloor(String name) {
        if (name == null)
            return false;
        String lower = name.toLowerCase();
        return !lower.contains("leaf") && !lower.contains("leaves") &&
                !lower.contains("log") && !lower.contains("wood") &&
                !lower.contains("fence") && !lower.contains("gate") &&
                !lower.contains("flower") && !lower.contains("grass_tall") &&
                !lower.contains("bush") && !lower.equalsIgnoreCase("Empty") &&
                !lower.contains("water") && !lower.contains("lava");
    }
}
