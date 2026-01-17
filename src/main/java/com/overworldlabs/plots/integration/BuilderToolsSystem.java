package com.overworldlabs.plots.integration;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.manager.WorldManager;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * ECS System to manage BuilderTools global masking for players in the plot
 * world.
 */
public class BuilderToolsSystem extends EntityTickingSystem<EntityStore> {
    private final WorldManager worldManager;
    private final BuilderToolsIntegration integration;
    private final Set<UUID> playersWithMask = new HashSet<>();

    public BuilderToolsSystem(WorldManager worldManager, BuilderToolsIntegration integration) {
        this.worldManager = worldManager;
        this.integration = integration;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer) {

        World plotWorld = worldManager.getPlotWorld();
        if (plotWorld == null)
            return;

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        UUID uuid = playerRef.getUuid();
        UUID playerWorldUuid = playerRef.getWorldUuid();
        if (playerWorldUuid == null)
            return;

        boolean inPlotWorld = playerWorldUuid.equals(plotWorld.getWorldConfig().getUuid());

        if (inPlotWorld && !playersWithMask.contains(uuid)) {
            // Player entered plot world, apply mask
            integration.applyMask(chunk.getReferenceTo(index), buffer);
            playersWithMask.add(uuid);
        } else if (!inPlotWorld && playersWithMask.contains(uuid)) {
            // Player left plot world, clear mask
            integration.clearMask(chunk.getReferenceTo(index), buffer);
            playersWithMask.remove(uuid);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }

    public void onPlayerDisconnect(UUID uuid) {
        playersWithMask.remove(uuid);
    }
}
