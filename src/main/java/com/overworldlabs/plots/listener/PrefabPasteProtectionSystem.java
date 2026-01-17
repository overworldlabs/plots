package com.overworldlabs.plots.listener;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.server.core.prefab.event.PrefabPasteEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.WorldManager;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles protection for prefab paste events.
 */
public class PrefabPasteProtectionSystem extends WorldEventSystem<EntityStore, PrefabPasteEvent> {
    private static final ConcurrentHashMap<Integer, UUID> pendingPastes = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    private final PlotManager plotManager;
    @SuppressWarnings("unused")
    private final WorldManager worldManager;

    public PrefabPasteProtectionSystem(PlotManager plotManager, WorldManager worldManager) {
        super(PrefabPasteEvent.class);
        this.plotManager = plotManager;
        this.worldManager = worldManager;
    }

    /**
     * Registers a player as the initiator of a prefab paste.
     */
    public static void registerPendingPaste(int prefabId, UUID playerUuid) {
        pendingPastes.put(prefabId, playerUuid);
    }

    /**
     * Clears a pending paste registration.
     */
    public static void clearPendingPaste(int prefabId) {
        pendingPastes.remove(prefabId);
    }

    @Override
    public void handle(@Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull PrefabPasteEvent event) {
        if (!event.isPasteStart()) {
            clearPendingPaste(event.getPrefabId());
            return;
        }

        UUID playerUuid = pendingPastes.get(event.getPrefabId());
        if (playerUuid == null) {
            return;
        }

        if (com.hypixel.hytale.server.core.permissions.PermissionsModule.get().hasPermission(playerUuid,
                PlotManager.PERM_ADMIN)) {
            return;
        }

        // Final protection usually happens block-by-block in our BlockMask.
    }
}
