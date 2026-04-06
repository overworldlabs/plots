package com.overworldlabs.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.integration.buildertools.BuilderToolsIntegration;

import javax.annotation.Nonnull;

/**
 * System that continuously applies the plot protection mask to BuilderTools
 * state.
 */
public class BuilderToolsMaskSystem extends EntityTickingSystem<EntityStore> {
    private final BuilderToolsIntegration builderToolsIntegration;

    public BuilderToolsMaskSystem(@Nonnull IWorldManager worldManager,
            @Nonnull BuilderToolsIntegration builderToolsIntegration) {
        this.builderToolsIntegration = builderToolsIntegration;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer) {

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        // Only apply in the plot world
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null || !Plots.getInstance().getPlotManager().getConfig().isManagedWorld(world.getName())) {
            return;
        }

        // Apply protection mask
        builderToolsIntegration.applyMask(chunk.getReferenceTo(index), buffer);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
