package dev.stoshe.plots.manager;

import dev.stoshe.plots.util.PlayerIdentity;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;

import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.stoshe.plots.api.IRadarManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.config.PlotConfig;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

/**
 * Manages radar markers for plots using packets
 */
public class RadarManager implements IRadarManager {
    private final PlotManager plotManager;
    private final WorldManager worldManager;

    public RadarManager(PlotManager plotManager, WorldManager worldManager) {
        this.plotManager = plotManager;
        this.worldManager = worldManager;
    }

    @Override
    public void updatePlotMarker(@Nonnull Plot plot) {
        PlotConfig config = plotManager.getConfig();
        Vector3d center = new Vector3d(plot.getCenterX(config), 64.0, plot.getCenterZ(config));

        World world = worldManager.getPlotWorld(plot.getWorld());
        if (world == null || world.getWorldMapManager() == null) {
            return;
        }

        String markerId = markerId(plot);
        MapMarker marker = new MapMarker();
        writeField(marker, "id", markerId);
        writeField(marker, "markerImage", "Home.png");
        writeField(marker, "transform", new Transform(
                new Position(center.x, center.y, center.z),
                new Direction(0f, 0f, 0f)));
        writeMarkerName(marker, plot.getName());
        world.getWorldMapManager().getPointsOfInterest().put(markerId, marker);
    }

    @Override
    public void removePlotMarker(@Nonnull Plot plot) {
        World world = worldManager.getPlotWorld(plot.getWorld());
        if (world == null || world.getWorldMapManager() == null) {
            return;
        }
        world.getWorldMapManager().getPointsOfInterest().remove(markerId(plot));
    }

    @Override
    public void refreshPlayerMarkers(@Nonnull PlayerRef playerRef) {
        for (Plot plot : plotManager.getPlayerPlots(PlayerIdentity.uuid(playerRef))) {
            updatePlotMarker(plot);
        }
    }

    @Override
    public void refreshAllPlotMarkers() {
        for (Plot plot : plotManager.getAllPlots()) {
            if (plot != null) {
                updatePlotMarker(plot);
            }
        }
    }

    @Override
    public void clearAllMarkers() {
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (player != null) {
                clearPlayerMarkers(player);
            }
        }
    }

    @Override
    public void clearPlayerMarkers(@Nonnull PlayerRef playerRef) {
        for (Plot plot : plotManager.getPlayerPlots(PlayerIdentity.uuid(playerRef))) {
            if (plot != null) {
                removePlotMarker(plot);
            }
        }
    }

    private String markerId(@Nonnull Plot plot) {
        return "plot_" + plot.getGridX() + "_" + plot.getGridZ();
    }

    private void writeMarkerName(@Nonnull MapMarker marker, @Nonnull String name) {
        try {
            Field field = marker.getClass().getField("name");
            Class<?> type = field.getType();
            if (type.equals(String.class)) {
                field.set(marker, name);
                return;
            }
            // Newer protocol versions may require FormattedMessage in this field.
            if ("com.hypixel.hytale.protocol.FormattedMessage".equals(type.getName())) {
                Object fm = type.getDeclaredConstructor().newInstance();
                Field rawText = type.getField("rawText");
                rawText.set(fm, name);
                field.set(marker, fm);
            }
        } catch (Exception ignored) {
        }
    }

    private void writeField(@Nonnull Object target, @Nonnull String fieldName, Object value) {
        try {
            Field field = target.getClass().getField(fieldName);
            field.set(target, value);
        } catch (Exception ignored) {
        }
    }
}
