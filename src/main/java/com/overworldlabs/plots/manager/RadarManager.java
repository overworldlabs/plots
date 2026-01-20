package com.overworldlabs.plots.manager;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.util.PositionUtil;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.model.PlotConfig;

import javax.annotation.Nonnull;

/**
 * Manages radar markers for plots
 */
public class RadarManager {
    private final PlotManager plotManager;
    private final WorldManager worldManager;

    public RadarManager(PlotManager plotManager, WorldManager worldManager) {
        this.plotManager = plotManager;
        this.worldManager = worldManager;
    }

    /**
     * Add or update a radar marker for a plot for its owner
     */
    public void updatePlotMarker(@Nonnull Plot plot) {
        PlayerRef ownerRef = Universe.get().getPlayer(plot.getOwner());
        if (ownerRef == null)
            return;

        com.hypixel.hytale.component.Ref<EntityStore> playerEntityRef = ownerRef.getReference();
        if (playerEntityRef == null)
            return;

        PlotConfig config = plotManager.getConfig();
        Vector3d center = new Vector3d(plot.getCenterX(config), 64.0, plot.getCenterZ(config));

        // Remove existing marker if any
        removePlotMarker(plot);

        // Create new marker
        String markerId = "plot_" + plot.getGridX() + "_" + plot.getGridZ();
        MapMarker marker = new MapMarker();
        marker.id = markerId;
        marker.name = plot.getName();
        marker.markerImage = "Home.png"; // Bed icon
        marker.transform = PositionUtil.toTransformPacket(new Transform(center));

        // Use WorldMapManager to add it to player data
        // We ensure the marker is registered for the plot world specifically
        WorldMapManager.createPlayerMarker(playerEntityRef, marker, playerEntityRef.getStore());
    }

    /**
     * Remove a radar marker for a plot
     */
    public void removePlotMarker(@Nonnull Plot plot) {
        String markerId = "plot_" + plot.getGridX() + "_" + plot.getGridZ();

        // Marker reference for removal - handles removal even if player is offline
        // This is world-specific, so it avoids clearing markers in other worlds if
        // names collide
        new WorldMapManager.PlayerMarkerReference(plot.getOwner(), worldManager.getWorldName(), markerId).remove();
    }

    /**
     * Refresh all plot markers for a player (e.g. on join)
     */
    public void refreshPlayerMarkers(@Nonnull PlayerRef playerRef) {
        for (Plot plot : plotManager.getPlayerPlots(playerRef.getUuid())) {
            updatePlotMarker(plot);
        }
    }

    /**
     * Clear all plot markers for all players
     * Useful when world is deleted or on server startup
     */
    public void clearAllMarkers() {
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (player != null) {
                clearPlayerMarkers(player);
            }
        }
    }

    /**
     * Clear all plot markers for a specific player
     */
    public void clearPlayerMarkers(@Nonnull PlayerRef playerRef) {
        for (Plot plot : plotManager.getAllPlots()) {
            if (plot != null) {
                String markerId = "plot_" + plot.getGridX() + "_" + plot.getGridZ();
                new WorldMapManager.PlayerMarkerReference(playerRef.getUuid(), worldManager.getWorldName(), markerId)
                        .remove();
            }
        }
    }
}
