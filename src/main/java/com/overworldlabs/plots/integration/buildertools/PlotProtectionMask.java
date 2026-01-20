package com.overworldlabs.plots.integration.buildertools;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockFilter;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.universe.world.accessor.ChunkAccessor;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ConsoleColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Block mask that restricts BuilderTools operations to plots where the player
 * has permission
 */
@SuppressWarnings({ "rawtypes", "deprecation" })
public class PlotProtectionMask extends BlockMask {
    private final UUID playerUuid;
    private final PlotManager plotManager;
    private final BlockMask originalMask;

    public PlotProtectionMask(@Nonnull UUID playerUuid, @Nullable BlockMask originalMask) {
        super(getFiltersFrom(originalMask));
        this.playerUuid = playerUuid;
        this.plotManager = Plots.getInstance().getPlotManager();
        this.originalMask = originalMask;
        ConsoleColors.success("[PlotProtectionMask] ✓ Created for player: " + playerUuid + " (wrapping: "
                + (originalMask != null ? originalMask.getClass().getSimpleName() : "none") + ")");
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    private static BlockFilter[] getFiltersFrom(@Nullable BlockMask mask) {
        return mask != null ? mask.getFilters() : new BlockFilter[0];
    }

    @Override
    public boolean isExcluded(@Nonnull ChunkAccessor chunks, int x, int y, int z, Vector3i min, Vector3i max,
            int worldY) {
        ConsoleColors.warning("[PlotProtectionMask] ⚡ isExcluded() called at [" + x + "," + y + "," + z
                + "] for player " + playerUuid);
        // Check original mask first
        if (originalMask != null && originalMask.isExcluded(chunks, x, y, z, min, max, worldY)) {
            ConsoleColors.info("[PlotProtectionMask]   → Original mask excluded");
            return true;
        }
        boolean result = isExcludedByPlot(x, y, z);
        ConsoleColors.info("[PlotProtectionMask]   → Result: " + (result ? "BLOCKED" : "ALLOWED"));
        return result;
    }

    @Override
    public boolean isExcluded(@Nonnull ChunkAccessor chunks, int x, int y, int z, Vector3i min, Vector3i max,
            int worldY, int layer) {
        ConsoleColors.warning("[PlotProtectionMask] ⚡ isExcluded(layer) called at [" + x + "," + y + "," + z
                + "] layer=" + layer + " for player " + playerUuid);
        // Check original mask first
        if (originalMask != null && originalMask.isExcluded(chunks, x, y, z, min, max, worldY, layer)) {
            ConsoleColors.info("[PlotProtectionMask]   → Original mask excluded");
            return true;
        }
        boolean result = isExcludedByPlot(x, y, z);
        ConsoleColors.info("[PlotProtectionMask]   → Result: " + (result ? "BLOCKED" : "ALLOWED"));
        return result;
    }

    private boolean isExcludedByPlot(int x, int y, int z) {
        // Bypass for admin
        if (com.hypixel.hytale.server.core.permissions.PermissionsModule.get().hasPermission(playerUuid,
                PlotManager.PERM_ADMIN)) {
            ConsoleColors.info("[PlotProtectionMask] ALLOWING - Admin bypass for " + playerUuid);
            return false;
        }

        String configWorld = plotManager.getConfig().getPlotWorldName();
        Plot plot = plotManager.getPlotAt(configWorld, x, z);

        boolean isExcluded;
        if (plot == null) {
            // Check if we are in the plot world
            String currentWorld = Plots.getInstance().getWorldManager().getWorldName();
            if (configWorld.equalsIgnoreCase(currentWorld)) {
                isExcluded = true; // Road or empty space in plot world
                ConsoleColors.warning("[PlotProtectionMask] BLOCKING - Road/empty at " + x + "," + z);
            } else {
                isExcluded = false; // Other world
            }
        } else {
            isExcluded = !plot.hasPermission(playerUuid);
            if (isExcluded) {
                ConsoleColors.warning("[PlotProtectionMask] BLOCKING - No permission for plot at " + x + "," + z +
                        " (owner: " + plot.getOwner() + ")");
            } else {
                ConsoleColors.info("[PlotProtectionMask] ALLOWING - Has permission for plot at " + x + "," + z);
            }
        }

        return isExcluded;
    }

    @Override
    public boolean isInverted() {
        return false;
    }

    @Override
    @Nonnull
    public String toString() {
        return "PlotProtectionMask{player=" + playerUuid + "}";
    }

    @Override
    @Nonnull
    public String informativeToString() {
        return toString();
    }
}
