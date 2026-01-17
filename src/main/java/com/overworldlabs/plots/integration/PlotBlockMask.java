package com.overworldlabs.plots.integration;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockFilter;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.universe.world.accessor.ChunkAccessor;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A block mask that restricts BuilderTools operations to plots where the player
 * has permission.
 */
@SuppressWarnings({ "rawtypes", "deprecation" })
public class PlotBlockMask extends BlockMask {
    private final UUID playerUuid;
    private final PlotManager plotManager;
    private final BlockMask originalMask;

    // Message cooldown to prevent chat spam
    private static final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 3000;

    public PlotBlockMask(@Nonnull UUID playerUuid, @Nullable BlockMask originalMask) {
        super(getFiltersFrom(originalMask));
        this.playerUuid = playerUuid;
        this.plotManager = Plots.getInstance().getPlotManager();
        this.originalMask = originalMask;
    }

    private static BlockFilter[] getFiltersFrom(@Nullable BlockMask mask) {
        return mask != null ? mask.getFilters() : new BlockFilter[0];
    }

    @Override
    public boolean isExcluded(@Nonnull ChunkAccessor chunks, int x, int y, int z, Vector3i min, Vector3i max,
            int worldY) {
        if (originalMask != null && originalMask.isExcluded(chunks, x, y, z, min, max, worldY)) {
            return true;
        }
        return isExcludedByPlot(x, worldY, z);
    }

    @Override
    public boolean isExcluded(@Nonnull ChunkAccessor chunks, int x, int y, int z, Vector3i min, Vector3i max,
            int worldY, int layer) {
        if (originalMask != null && originalMask.isExcluded(chunks, x, y, z, min, max, worldY, layer)) {
            return true;
        }
        return isExcludedByPlot(x, worldY, z);
    }

    private boolean isExcludedByPlot(int x, int y, int z) {
        // Bypass for admin (perm: plots.admin)
        if (com.hypixel.hytale.server.core.permissions.PermissionsModule.get().hasPermission(playerUuid,
                PlotManager.PERM_ADMIN)) {
            return false;
        }

        String configWorld = plotManager.getConfig().getPlotWorldName();
        // Use global coordinates x and z to find the plot
        Plot plot = plotManager.getPlotAt(configWorld, x, z);

        boolean isExcluded;
        if (plot == null) {
            // Check if we are in the plot world
            String currentWorld = Plots.getInstance().getWorldManager().getWorldName();
            if (configWorld.equalsIgnoreCase(currentWorld)) {
                isExcluded = true; // Road or empty space in plot world
            } else {
                isExcluded = false; // Other world
            }
        } else {
            isExcluded = !plot.hasPermission(playerUuid);
        }

        if (isExcluded) {
            sendNoPermissionMessage();
        }

        return isExcluded;
    }

    private void sendNoPermissionMessage() {
        long now = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(playerUuid);
        if (lastTime == null || (now - lastTime) > COOLDOWN_MS) {
            lastMessageTime.put(playerUuid, now);

            com.hypixel.hytale.server.core.universe.Universe universe = com.hypixel.hytale.server.core.universe.Universe
                    .get();
            if (universe != null) {
                for (com.hypixel.hytale.server.core.universe.PlayerRef player : universe.getPlayers()) {
                    if (player.getUuid().equals(playerUuid)) {
                        TranslationManager tm = Plots.getInstance().getTranslationManager();
                        player.sendMessage(ChatUtil.colorize(tm.get("protection.no_permission")));
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isInverted() {
        return false;
    }

    @Override
    @Nonnull
    public String toString() {
        return "PlotBlockMask{player=" + playerUuid + "}";
    }

    @Override
    @Nonnull
    public String informativeToString() {
        return toString();
    }
}
