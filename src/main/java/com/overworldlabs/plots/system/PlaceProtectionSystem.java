package com.overworldlabs.plots.system;

import com.overworldlabs.plots.util.PlayerIdentity;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.flag.FlagRegistry;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;

/**
 * System to protect blocks from being placed.
 */
public class PlaceProtectionSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private final IPlotManager plotManager;

    public PlaceProtectionSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        super(PlaceBlockEvent.class);
        this.plotManager = plotManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull PlaceBlockEvent event) {

        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (!plotManager.getConfig().isManagedWorld(world.getName())) {
            return;
        }

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        Vector3i pos = event.getTargetBlock();

        if (isLiquidPlacement(event) && wouldLiquidFlowOutsideOwnedArea(world.getName(), playerRef, pos)) {
            event.setCancelled(true);
            TranslationManager tm = Plots.getInstance().getTranslationManager();
            playerRef.sendMessage(ChatUtil.error(tm.get("protection.no_permission_place_liquid")));
            return;
        }

        if (!plotManager.canModify(playerRef, world, pos.x, pos.y, pos.z, IPlotManager.ActionType.PLACE)) {
            event.setCancelled(true);
            TranslationManager tm = Plots.getInstance().getTranslationManager();
            Plot plot = plotManager.getPlotAt(world.getName(), pos.x, pos.z);
            String custom = plot != null ? plot.getFlagValue(FlagRegistry.DENY_MESSAGE) : "";
            playerRef.sendMessage(ChatUtil.error(
                    custom != null && !custom.trim().isEmpty() ? custom : tm.get("protection.no_permission_place")));
        }
    }

    private boolean wouldLiquidFlowOutsideOwnedArea(String worldName, PlayerRef playerRef, Vector3i pos) {
        Plot origin = plotManager.getPlotAt(worldName, pos.x, pos.z);
        if (origin == null || !origin.hasPermission(PlayerIdentity.uuid(playerRef))) {
            return false;
        }

        int[][] offsets = new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] off : offsets) {
            Plot adjacent = plotManager.getPlotAt(worldName, pos.x + off[0], pos.z + off[1]);
            if (adjacent == null) {
                return true;
            }
            if (!adjacent.getOwner().equals(origin.getOwner())) {
                return true;
            }
        }
        return false;
    }

    private boolean isLiquidPlacement(PlaceBlockEvent event) {
        try {
            for (java.lang.reflect.Method m : event.getClass().getMethods()) {
                if (m.getParameterCount() != 0) {
                    continue;
                }
                String lower = m.getName().toLowerCase();
                if (!lower.contains("block") && !lower.contains("item") && !lower.contains("state")) {
                    continue;
                }
                Object value = m.invoke(event);
                if (value == null) {
                    continue;
                }
                String text = value.toString().toLowerCase();
                if (text.contains("water") || text.contains("lava") || text.contains("fluid")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
