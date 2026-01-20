package com.overworldlabs.plots.api.impl;

import com.overworldlabs.plots.api.*;
import com.overworldlabs.plots.api.events.*;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.WorldManager;
import com.overworldlabs.plots.model.Plot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Implementation of all Plot API interfaces
 */
public class PlotsAPIImpl implements PlotsAPI, PlotAPI, PlotEventAPI {
    private final PlotManager plotManager;
    private final WorldManager worldManager;

    // Event listeners
    private final Map<String, Consumer<ClaimEvent>> claimListeners = new ConcurrentHashMap<>();
    private final Map<String, Consumer<UnclaimEvent>> unclaimListeners = new ConcurrentHashMap<>();
    private final Map<String, Consumer<RenameEvent>> renameListeners = new ConcurrentHashMap<>();
    private final Map<String, Consumer<TrustEvent>> trustListeners = new ConcurrentHashMap<>();

    public PlotsAPIImpl(@Nonnull PlotManager plotManager, @Nonnull WorldManager worldManager) {
        this.plotManager = plotManager;
        this.worldManager = worldManager;
    }

    // ========== PlotsAPI Implementation ==========

    @Override
    @Nullable
    public Plot getPlotAt(int x, int z) {
        return plotManager.getPlotAt(plotManager.getConfig().getPlotWorldName(), x, z);
    }

    @Override
    @Nullable
    public Plot getPlotByGrid(int gridX, int gridZ) {
        return plotManager.getPlot(gridX, gridZ);
    }

    @Override
    @Nonnull
    public Collection<Plot> getPlotsByOwner(@Nonnull UUID ownerUuid) {
        return plotManager.getAllPlots().stream()
                .filter(plot -> plot.getOwner() != null && plot.getOwner().equals(ownerUuid))
                .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public Collection<Plot> getAllPlots() {
        return new ArrayList<>(plotManager.getAllPlots());
    }

    @Override
    public boolean isPlayerTrusted(@Nonnull Plot plot, @Nonnull UUID playerUuid) {
        return (plot.getOwner() != null && plot.getOwner().equals(playerUuid))
                || plot.getTrustedPlayers().contains(playerUuid);
    }

    @Override
    public boolean isPlotOwner(@Nonnull Plot plot, @Nonnull UUID playerUuid) {
        return plot.getOwner() != null && plot.getOwner().equals(playerUuid);
    }

    @Override
    public boolean canPlayerBuild(@Nonnull UUID playerUuid, int x, int z) {
        Plot plot = getPlotAt(x, z);
        if (plot == null) {
            return true; // Can build on roads
        }
        return isPlayerTrusted(plot, playerUuid);
    }

    @Override
    public int getTotalPlots() {
        return plotManager.getAllPlots().size();
    }

    @Override
    public int getPlotCount(@Nonnull UUID ownerUuid) {
        return (int) plotManager.getAllPlots().stream()
                .filter(plot -> plot.getOwner() != null && plot.getOwner().equals(ownerUuid))
                .count();
    }

    @Override
    @Nonnull
    public String getPlotWorldName() {
        return plotManager.getConfig().getPlotWorldName();
    }

    @Override
    public int getPlotSize() {
        return plotManager.getConfig().getPlotSize();
    }

    @Override
    public int getRoadWidth() {
        return plotManager.getConfig().getRoadSize();
    }

    // ========== PlotAPI Implementation ==========

    @Override
    @Nullable
    public Plot claimPlot(int gridX, int gridZ, @Nonnull UUID ownerUuid, @Nonnull String ownerName) {
        // Note: Claiming plots requires CommandSender context
        // This method is not fully implemented - use PlotCommand instead
        return null;
    }

    @Override
    public boolean unclaimPlot(@Nonnull Plot plot) {
        // Note: Unclaiming plots requires proper cleanup (holograms, radar, etc)
        // This method is not fully implemented - use PlotCommand instead
        return false;
    }

    @Override
    public boolean renamePlot(@Nonnull Plot plot, @Nonnull String newName) {
        String oldName = plot.getName();
        plot.setName(newName);
        fireRenameEvent(new RenameEvent(plot, oldName, newName));
        return true;
    }

    @Override
    public boolean trustPlayer(@Nonnull Plot plot, @Nonnull UUID playerUuid) {
        if (plot.getTrustedPlayers().contains(playerUuid)) {
            return false;
        }
        plot.getTrustedPlayers().add(playerUuid);
        fireTrustEvent(new TrustEvent(plot, playerUuid, true));
        return true;
    }

    @Override
    public boolean untrustPlayer(@Nonnull Plot plot, @Nonnull UUID playerUuid) {
        boolean removed = plot.getTrustedPlayers().remove(playerUuid);
        if (removed) {
            fireTrustEvent(new TrustEvent(plot, playerUuid, false));
        }
        return removed;
    }

    @Override
    @Nonnull
    public Set<UUID> getTrustedPlayers(@Nonnull Plot plot) {
        return new HashSet<>(plot.getTrustedPlayers());
    }

    @Override
    @Nullable
    public UUID getPlotOwner(@Nonnull Plot plot) {
        return plot.getOwner();
    }

    @Override
    public boolean isPlotClaimed(int gridX, int gridZ) {
        return plotManager.getPlot(gridX, gridZ) != null;
    }

    @Override
    public int[] getPlotMinCorner(@Nonnull Plot plot) {
        int plotSize = plotManager.getConfig().getPlotSize();
        int roadWidth = plotManager.getConfig().getRoadSize();
        int totalSize = plotSize + roadWidth;

        int minX = plot.getGridX() * totalSize;
        int minZ = plot.getGridZ() * totalSize;

        return new int[] { minX, minZ };
    }

    @Override
    public int[] getPlotMaxCorner(@Nonnull Plot plot) {
        int plotSize = plotManager.getConfig().getPlotSize();
        int roadWidth = plotManager.getConfig().getRoadSize();
        int totalSize = plotSize + roadWidth;

        int maxX = plot.getGridX() * totalSize + plotSize - 1;
        int maxZ = plot.getGridZ() * totalSize + plotSize - 1;

        return new int[] { maxX, maxZ };
    }

    @Override
    public int[] getPlotCenter(@Nonnull Plot plot) {
        int plotSize = plotManager.getConfig().getPlotSize();
        int roadWidth = plotManager.getConfig().getRoadSize();
        int totalSize = plotSize + roadWidth;

        int centerX = plot.getGridX() * totalSize + plotSize / 2;
        int centerZ = plot.getGridZ() * totalSize + plotSize / 2;

        return new int[] { centerX, centerZ };
    }

    @Override
    public boolean isWithinPlot(@Nonnull Plot plot, int x, int z) {
        int[] min = getPlotMinCorner(plot);
        int[] max = getPlotMaxCorner(plot);

        return x >= min[0] && x <= max[0] && z >= min[1] && z <= max[1];
    }

    // ========== PlotEventAPI Implementation ==========

    @Override
    @Nonnull
    public String onPlotClaim(@Nonnull Consumer<ClaimEvent> listener) {
        String id = UUID.randomUUID().toString();
        claimListeners.put(id, listener);
        return id;
    }

    @Override
    @Nonnull
    public String onPlotUnclaim(@Nonnull Consumer<UnclaimEvent> listener) {
        String id = UUID.randomUUID().toString();
        unclaimListeners.put(id, listener);
        return id;
    }

    @Override
    @Nonnull
    public String onPlotRename(@Nonnull Consumer<RenameEvent> listener) {
        String id = UUID.randomUUID().toString();
        renameListeners.put(id, listener);
        return id;
    }

    @Override
    @Nonnull
    public String onPlotTrust(@Nonnull Consumer<TrustEvent> listener) {
        String id = UUID.randomUUID().toString();
        trustListeners.put(id, listener);
        return id;
    }

    @Override
    public boolean unregisterListener(@Nonnull String listenerId) {
        return claimListeners.remove(listenerId) != null
                || unclaimListeners.remove(listenerId) != null
                || renameListeners.remove(listenerId) != null
                || trustListeners.remove(listenerId) != null;
    }

    @Override
    public void unregisterAllListeners() {
        claimListeners.clear();
        unclaimListeners.clear();
        renameListeners.clear();
        trustListeners.clear();
    }

    // ========== Event Firing Methods ==========

    public void fireClaimEvent(ClaimEvent event) {
        claimListeners.values().forEach(listener -> {
            try {
                listener.accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void fireUnclaimEvent(UnclaimEvent event) {
        unclaimListeners.values().forEach(listener -> {
            try {
                listener.accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void fireRenameEvent(RenameEvent event) {
        renameListeners.values().forEach(listener -> {
            try {
                listener.accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void fireTrustEvent(TrustEvent event) {
        trustListeners.values().forEach(listener -> {
            try {
                listener.accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
