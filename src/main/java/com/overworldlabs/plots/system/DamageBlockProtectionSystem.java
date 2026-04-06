package com.overworldlabs.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;

/**
 * System to protect blocks from being damaged by tools before break resolves.
 */
public class DamageBlockProtectionSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {
    private final IPlotManager plotManager;

    public DamageBlockProtectionSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        super(DamageBlockEvent.class);
        this.plotManager = plotManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull DamageBlockEvent event) {

        if (event.isCancelled()) {
            return;
        }

        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (!plotManager.getConfig().isManagedWorld(world.getName())) {
            return;
        }

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        Vector3i pos = event.getTargetBlock();
        if (!plotManager.canModify(playerRef, world, pos.x, pos.y, pos.z, IPlotManager.ActionType.BREAK)) {
            event.setCancelled(true);
            TranslationManager tm = Plots.getInstance().getTranslationManager();
            playerRef.sendMessage(ChatUtil.error(tm.get("protection.no_permission_break")));
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
