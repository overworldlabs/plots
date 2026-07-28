package dev.stoshe.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.api.IWorldManager;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;

import javax.annotation.Nonnull;

/**
 * System to protect blocks from being broken.
 */
public class BreakProtectionSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private final IPlotManager plotManager;

    public BreakProtectionSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
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

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        Vector3i pos = event.getTargetBlock();

        if (!plotManager.canModify(playerRef, world, pos.x, pos.y, pos.z, IPlotManager.ActionType.BREAK)) {
            event.setCancelled(true);
            TranslationManager tm = Plots.getInstance().getTranslationManager();
            Plot plot = plotManager.getPlotAt(world.getName(), pos.x, pos.z);
            String custom = plot != null ? plot.getFlagValue(FlagRegistry.DENY_MESSAGE) : "";
            playerRef.sendMessage(ChatUtil.error(
                    custom != null && !custom.trim().isEmpty() ? custom : tm.get("protection.no_permission_break")));
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
