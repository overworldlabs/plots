package dev.stoshe.plots.api;

import dev.stoshe.plots.model.Plot;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Interface for managing radar markers.
 */
public interface IRadarManager {
    void updatePlotMarker(@Nonnull Plot plot);

    void removePlotMarker(@Nonnull Plot plot);

    void refreshPlayerMarkers(@Nonnull PlayerRef playerRef);

    void refreshAllPlotMarkers();

    void clearAllMarkers();

    void clearPlayerMarkers(@Nonnull PlayerRef playerRef);
}
