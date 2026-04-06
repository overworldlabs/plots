package com.overworldlabs.plots.system;

import com.overworldlabs.plots.util.PlayerIdentity;
import com.hypixel.hytale.math.vector.Vector3d;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.flag.FlagRegistry;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.EventCancelUtil;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;

/**
 * System to prevent item dropping in plots if disabled via flags
 */
public class PlotItemDropSystem extends EntityEventSystem<EntityStore, DropItemEvent> {
    private final IPlotManager plotManager;

    public PlotItemDropSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        super(DropItemEvent.class);
        this.plotManager = plotManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull DropItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (!plotManager.getConfig().isManagedWorld(world.getName())) {
            return;
        }

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;
        if (PermissionUtil.hasAdminPermission(PlayerIdentity.uuid(playerRef))) {
            return;
        }

        Vector3d pos = playerRef.getTransform().getPosition();
        Plot plot = plotManager.getPlotAt(world.getName(), (int) pos.x, (int) pos.z);

        if (plot != null) {
            if (!plotManager.canModify(playerRef, world, (int) pos.x, (int) pos.y, (int) pos.z,
                    IPlotManager.ActionType.INTERACT)) {
                EventCancelUtil.forceCancel(event);
                clearDropEventStack(event);
                TranslationManager tm = Plots.getInstance().getTranslationManager();
                String custom = plot.getFlagValue(FlagRegistry.DENY_MESSAGE);
                playerRef.sendMessage(ChatUtil.error(
                        custom != null && !custom.trim().isEmpty() ? custom : tm.get("protection.no_permission_interact")));
                return;
            }

            boolean canDrop = plot.getFlagValue(FlagRegistry.ITEM_DROP);
            if (!canDrop && !plot.isOwnerOrMember(PlayerIdentity.uuid(playerRef))) {
                EventCancelUtil.forceCancel(event);
                clearDropEventStack(event);
                TranslationManager tm = Plots.getInstance().getTranslationManager();
                playerRef.sendMessage(ChatUtil.error(tm.get("protection.item_drop_disabled")));
            }
        }
    }

    private void clearDropEventStack(@Nonnull DropItemEvent event) {
        String[] candidates = new String[] { "setItemStack", "setDroppedItemStack", "setDroppedItem" };
        for (String methodName : candidates) {
            try {
                var m = event.getClass().getMethod(methodName, ItemStack.class);
                m.invoke(event, ItemStack.EMPTY);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
