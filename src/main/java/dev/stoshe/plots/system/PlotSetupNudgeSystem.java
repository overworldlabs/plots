package dev.stoshe.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.util.PermissionUtil;
import dev.stoshe.plots.util.PlayerIdentity;
import dev.stoshe.plots.util.PlotSetup;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Periodically reminds online admins/ops to configure a plot world when none is
 * loaded yet (fresh install, failed creation, or all worlds removed). The
 * reminder only targets staff and stops automatically once a plot world exists.
 */
public class PlotSetupNudgeSystem extends EntityTickingSystem<EntityStore> {

    /** Seconds between reminders for a given admin. */
    private static final float NUDGE_INTERVAL_SECONDS = 30.0f;

    /** Per-admin time accumulator since their last reminder. */
    private final Map<UUID, Float> elapsedSinceReminder = new HashMap<>();

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer) {
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        UUID uuid = PlayerIdentity.uuid(playerRef);

        if (!isAdmin(playerRef)) {
            return;
        }

        if (PlotSetup.isReady()) {
            elapsedSinceReminder.remove(uuid);
            return;
        }

        float elapsed = elapsedSinceReminder.getOrDefault(uuid, NUDGE_INTERVAL_SECONDS) + dt;
        if (elapsed < NUDGE_INTERVAL_SECONDS) {
            elapsedSinceReminder.put(uuid, elapsed);
            return;
        }

        PlotSetup.sendReminder(playerRef);
        elapsedSinceReminder.put(uuid, 0.0f);
    }

    private boolean isAdmin(@Nonnull PlayerRef playerRef) {
        return playerRef instanceof CommandSender sender && PermissionUtil.hasAdminPermission(sender);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
