package com.overworldlabs.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.manager.WorldManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * System to notify players when they enter the plot world or a plot area
 */
public class PlotNotificationSystem extends EntityTickingSystem<EntityStore> {
    private final PlotManager plotManager;
    private final WorldManager worldManager;
    private final Map<UUID, String> lastPlotId = new HashMap<>();
    private final Set<UUID> notifiedWorld = new HashSet<>();

    public PlotNotificationSystem(PlotManager plotManager, WorldManager worldManager) {
        this.plotManager = plotManager;
        this.worldManager = worldManager;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer) {

        World plotWorld = worldManager.getPlotWorld();
        if (plotWorld == null)
            return;

        // Fix deprecation: use store.getComponent instead of playerRef.getComponent
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        UUID uuid = playerRef.getUuid();
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null || plotWorld.getWorldConfig() == null)
            return;

        // Only notify if in plot world
        if (!worldUuid.equals(plotWorld.getWorldConfig().getUuid())) {
            // Cleanup if they left the world
            notifiedWorld.remove(uuid);
            lastPlotId.remove(uuid);
            return;
        }

        TranslationManager tm = Plots.getInstance().getTranslationManager();

        // World entry notification
        if (!notifiedWorld.contains(uuid)) {
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    ChatUtil.colorize(tm.get("notification.world_enter.title")),
                    ChatUtil.colorize(tm.get("notification.world_enter.subtitle")),
                    true);
            notifiedWorld.add(uuid);

            // Refresh radar markers
            Plots.getInstance().getRadarManager().refreshPlayerMarkers(playerRef);
        }

        // Plot entry notification
        com.hypixel.hytale.math.vector.Vector3d pos = playerRef.getTransform().getPosition();
        Plot plot = plotManager.getPlotAt(plotWorld.getName(), (int) pos.x, (int) pos.z);
        String currentId = (plot == null) ? "road" : plot.getGridX() + "_" + plot.getGridZ();
        String lastId = lastPlotId.get(uuid);

        if (!currentId.equals(lastId)) {
            if (plot != null) {
                String ownerName = plot.getOwnerName();
                EventTitleUtil.showEventTitleToPlayer(
                        playerRef,
                        ChatUtil.colorize(tm.get("notification.plot_enter.title", "name", plot.getName())),
                        ChatUtil.colorize(tm.get("notification.plot_enter.subtitle", "owner", ownerName)),
                        false);
            }

            lastPlotId.put(uuid, currentId);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
