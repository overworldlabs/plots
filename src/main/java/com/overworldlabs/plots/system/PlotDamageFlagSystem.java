package com.overworldlabs.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector4d;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.flag.FlagRegistry;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;
import com.overworldlabs.plots.util.PlayerIdentity;

import javax.annotation.Nonnull;

/**
 * Handles non-PvP player damage flags such as damage and fall-damage.
 */
public class PlotDamageFlagSystem extends EntityEventSystem<EntityStore, Damage> {
    private final IPlotManager plotManager;

    public PlotDamageFlagSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        super(Damage.class);
        this.plotManager = plotManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull Damage event) {
        if (event.isCancelled()) {
            return;
        }

        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (!plotManager.getConfig().isManagedWorld(world.getName())) {
            return;
        }

        PlayerRef victim = chunk.getComponent(index, PlayerRef.getComponentType());
        if (victim == null) {
            return;
        }
        if (PermissionUtil.hasAdminPermission(PlayerIdentity.uuid(victim))) {
            return;
        }

        Vector3d pos = resolveHitPosition(event, victim);
        Plot plot = plotManager.getPlotAt(world.getName(), (int) pos.x, (int) pos.z);
        if (plot == null) {
            return;
        }

        PlayerRef attackerPlayer = resolveAttackerPlayer(event, store);
        if (attackerPlayer != null) {
            if (PermissionUtil.hasAdminPermission(PlayerIdentity.uuid(attackerPlayer))) {
                return;
            }

            Vector3d attackerPos = attackerPlayer.getTransform().getPosition();
            Plot attackerPlot = plotManager.getPlotAt(world.getName(), (int) attackerPos.x, (int) attackerPos.z);

            boolean denyByVictimPlot = !plot.getFlagValue(FlagRegistry.PVP);
            boolean denyByAttackerPlot = attackerPlot != null && !attackerPlot.getFlagValue(FlagRegistry.PVP);
            if (denyByVictimPlot || denyByAttackerPlot) {
                blockDamage(event, chunk.getReferenceTo(index), store, buffer);
                var tm = Plots.getInstance().getTranslationManager();
                attackerPlayer.sendMessage(ChatUtil.error(tm.get("protection.no_permission_pvp")));
                return;
            }
        }

        boolean isEntityDamage = event.getSource() instanceof Damage.EntitySource;
        boolean blocked = false;
        if (isFallDamage(event) && !plot.getFlagValue(FlagRegistry.FALL_DAMAGE)) {
            blocked = true;
        } else if (isEntityDamage && !plot.getFlagValue(FlagRegistry.MOB_DAMAGE)) {
            blocked = true;
        } else if (!plot.getFlagValue(FlagRegistry.DAMAGE)) {
            blocked = true;
        }

        if (blocked) {
            blockDamage(event, chunk.getReferenceTo(index), store, buffer);
            String defaultMsg = Plots.getInstance().getTranslationManager().get("general.no_permission");
            String custom = plot.getFlagValue(FlagRegistry.DENY_MESSAGE);
            victim.sendMessage(ChatUtil.error(custom != null && !custom.trim().isEmpty() ? custom : defaultMsg));
        }
    }

    private boolean isFallDamage(@Nonnull Damage event) {
        try {
            int causeIndex = event.getDamageCauseIndex();
            DamageCause cause = DamageCause.getAssetMap().getAsset(causeIndex);
            if (cause == null) {
                return false;
            }
            String id = cause.getId();
            if (id != null && id.equalsIgnoreCase("fall")) {
                return true;
            }
            String text = cause.toString();
            return text != null && text.toLowerCase().contains("fall");
        } catch (Exception ignored) {
            return false;
        }
    }

    private Vector3d resolveHitPosition(@Nonnull Damage event, @Nonnull PlayerRef victim) {
        try {
            if (event.hasMetaObject(Damage.HIT_LOCATION)) {
                Vector4d v4 = event.getMetaObject(Damage.HIT_LOCATION);
                if (v4 != null) {
                    return new Vector3d(v4.x, v4.y, v4.z);
                }
            }
        } catch (Exception ignored) {
        }
        return victim.getTransform().getPosition();
    }

    private void blockDamage(@Nonnull Damage event, @Nonnull Ref<EntityStore> victimRef,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        event.setCancelled(true);
    }

    private PlayerRef resolveAttackerPlayer(@Nonnull Damage event, @Nonnull Store<EntityStore> store) {
        try {
            if (event.getSource() instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> attackerRef = entitySource.getRef();
                if (attackerRef != null) {
                    return attackerRef.getStore().getComponent(attackerRef, PlayerRef.getComponentType());
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
