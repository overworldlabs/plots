package com.overworldlabs.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.manager.WorldManager;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;

/**
 * System to protect blocks from being placed.
 */
public class PlaceProtectionSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private final PlotManager plotManager;
    private final WorldManager worldManager;

    public PlaceProtectionSystem(PlotManager plotManager, WorldManager worldManager) {
        super(PlaceBlockEvent.class);
        this.plotManager = plotManager;
        this.worldManager = worldManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull PlaceBlockEvent event) {

        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (!world.getName().equals(worldManager.getWorldName())) {
            return;
        }

        // Fix deprecation: use store.getComponent instead of playerRef.getComponent
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        Vector3i pos = event.getTargetBlock();

        if (!plotManager.canModify(playerRef, world, pos.x, pos.y, pos.z)) {
            event.setCancelled(true);
            TranslationManager tm = Plots.getInstance().getTranslationManager();
            playerRef.sendMessage(ChatUtil.error(tm.get("protection.no_permission_place")));
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
