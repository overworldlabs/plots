package com.overworldlabs.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.flag.FlagRegistry;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;
import com.overworldlabs.plots.util.PlayerIdentity;

import javax.annotation.Nonnull;

/**
 * System to protect manual item pickup (interaction/F-key).
 */
public class PlotItemPickupSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {
    private final IPlotManager plotManager;

    public PlotItemPickupSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        super(InteractivelyPickupItemEvent.class);
        this.plotManager = plotManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull InteractivelyPickupItemEvent event) {
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
        if (PermissionUtil.hasAdminPermission(PlayerIdentity.uuid(playerRef))) {
            return;
        }

        Vector3d pos = playerRef.getTransform().getPosition();
        Plot plot = plotManager.getPlotAt(world.getName(), (int) pos.x, (int) pos.z);
        if (plot == null) {
            return;
        }

        TranslationManager tm = Plots.getInstance().getTranslationManager();
        if (!plotManager.canModify(playerRef, world, (int) pos.x, (int) pos.y, (int) pos.z,
                IPlotManager.ActionType.INTERACT)) {
            event.setCancelled(true);
            event.setItemStack(ItemStack.EMPTY);
            String custom = plot.getFlagValue(FlagRegistry.DENY_MESSAGE);
            playerRef.sendMessage(ChatUtil.error(
                    custom != null && !custom.trim().isEmpty() ? custom : tm.get("protection.no_permission_interact")));
            return;
        }

        if (!plot.getFlagValue(FlagRegistry.ITEM_PICKUP_MANUAL) && !plot.isOwnerOrMember(PlayerIdentity.uuid(playerRef))) {
            event.setCancelled(true);
            event.setItemStack(ItemStack.EMPTY);
            String custom = plot.getFlagValue(FlagRegistry.DENY_MESSAGE);
            playerRef.sendMessage(ChatUtil.error(
                    custom != null && !custom.trim().isEmpty() ? custom : tm.get("protection.item_pickup_disabled")));
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
