package dev.stoshe.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.api.IWorldManager;

import javax.annotation.Nonnull;

/**
 * System to handle plot-specific crafting restrictions
 */
public class PlotCraftingSystem extends EntityEventSystem<EntityStore, CraftRecipeEvent> {
    private final IPlotManager plotManager;

    public PlotCraftingSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        super(CraftRecipeEvent.class);
        this.plotManager = plotManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull CraftRecipeEvent event) {

        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (!plotManager.getConfig().isManagedWorld(world.getName())) {
            return;
        }

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        // Currently no specific crafting restrictions implemented, but placeholder for
        // flags
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
