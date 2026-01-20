package com.overworldlabs.plots.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.UpdateChecker;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * System to notify admins about available updates when they join the server
 */
public class UpdateNotificationSystem extends EntityTickingSystem<EntityStore> {
    private final Map<UUID, Float> playerJoinTime = new HashMap<>();
    private String latestVersion = null;
    private boolean updateCheckInProgress = false;
    private final String currentVersion;
    private static final float NOTIFICATION_DELAY = 3.0f; // 3 seconds after join

    public UpdateNotificationSystem(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer) {

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        UUID uuid = playerRef.getUuid();

        // Track when player joins
        if (!playerJoinTime.containsKey(uuid)) {
            // Check if player has permission before tracking (PlayerRef extends
            // CommandSender)
            if (playerRef instanceof com.hypixel.hytale.server.core.command.system.CommandSender) {
                com.hypixel.hytale.server.core.command.system.CommandSender sender = (com.hypixel.hytale.server.core.command.system.CommandSender) playerRef;

                if (sender.hasPermission(PlotManager.PERM_ADMIN)) {
                    playerJoinTime.put(uuid, 0.0f);

                    // Start update check if not already in progress
                    if (latestVersion == null && !updateCheckInProgress) {
                        updateCheckInProgress = true;
                        UpdateChecker.checkForUpdates(currentVersion).thenAccept(version -> {
                            latestVersion = version;
                            updateCheckInProgress = false;
                        });
                    }
                }
            }
            return;
        }

        // Increment time since join
        float timeSinceJoin = playerJoinTime.get(uuid);
        if (timeSinceJoin < NOTIFICATION_DELAY) {
            playerJoinTime.put(uuid, timeSinceJoin + dt);
            return;
        }

        // Notify if update is available (only once)
        if (timeSinceJoin >= NOTIFICATION_DELAY && timeSinceJoin < NOTIFICATION_DELAY + 1.0f) {
            if (latestVersion != null && UpdateChecker.isNewerVersion(currentVersion, latestVersion)) {
                playerRef.sendMessage(ChatUtil
                        .colorize("{#ffaa00}[Plots] {#ffffff}A new version is available: {#55ff55}" + latestVersion));
                playerRef.sendMessage(ChatUtil.colorize(
                        "{#ffaa00}[Plots] {#ffffff}Download: {#55ffff}https://github.com/overworldlabs/plots/releases"));
            }
            // Mark as notified by setting time to a high value
            playerJoinTime.put(uuid, 999999.0f);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
