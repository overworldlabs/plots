package dev.stoshe.plots.system;

import dev.stoshe.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import org.joml.Vector3d;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.api.IWorldManager;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;

/**
 * System to protect mobs in plots from being damaged by non-members if disabled
 * via flags
 */
public class PlotMobProtectionSystem extends EntityEventSystem<EntityStore, Damage> {
    private final IPlotManager plotManager;

    public PlotMobProtectionSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        super(Damage.class);
        this.plotManager = plotManager;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull Damage event) {

        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (!plotManager.getConfig().isManagedWorld(world.getName())) {
            return;
        }

        // Damage source is usually an entity
        if (!(event.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        PlayerRef attacker = store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (attacker == null)
            return;
        if (PermissionUtil.hasAdminPermission(PlayerIdentity.uuid(attacker))) {
            return;
        }

        // Victim is not a player (handled by PlotPvPProtectionSystem)
        // Since it's an EntityEventSystem and it's matching any entity (based on
        // getQuery),
        // we should check if victim is a player.
        if (chunk.getComponent(index, PlayerRef.getComponentType()) != null)
            return;

        // Get position from TransformComponent
        TransformComponent transform = chunk.getComponent(index,
                TransformComponent.getComponentType());
        if (transform == null)
            return;

        Vector3d pos = transform.getPosition();

        Plot plot = plotManager.getPlotAt(world.getName(), (int) pos.x, (int) pos.z);

        if (plot != null) {
            boolean mobDamageEnabled = plot.getFlagValue(FlagRegistry.MOB_DAMAGE);
            if (!mobDamageEnabled && !plot.isOwnerOrMember(PlayerIdentity.uuid(attacker))) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(); // Match all entities
    }
}
