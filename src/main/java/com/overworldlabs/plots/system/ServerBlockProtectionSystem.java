package com.overworldlabs.plots.system;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.flag.FlagRegistry;
import com.overworldlabs.plots.model.Plot;

import javax.annotation.Nonnull;

/**
 * System to prevent server-side block breaking (e.g. explosives, fire) in plots
 */
public class ServerBlockProtectionSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private final IPlotManager plotManager;

    public ServerBlockProtectionSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        super(BreakBlockEvent.class);
        this.plotManager = plotManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull BreakBlockEvent event) {

        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (!plotManager.getConfig().isManagedWorld(world.getName())) {
            return;
        }

        // Server-side events usually don't have a player component at the index
        // If there's no player, it's a server-side action (explosion, fire, etc.)
        if (chunk.getComponent(index, PlayerRef.getComponentType()) != null) {
            return; // Handled by BreakProtectionSystem
        }

        Vector3i pos = event.getTargetBlock();
        Plot plot = plotManager.getPlotAt(world.getName(), pos.x, pos.z);
        if (plot == null) {
            event.setCancelled(true);
            return;
        }
        boolean allowExplosions = plot.getFlagValue(FlagRegistry.EXPLOSIONS);
        event.setCancelled(!allowExplosions);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of();
    }
}
