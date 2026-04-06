package com.overworldlabs.plots.system;

import com.overworldlabs.plots.util.PlayerIdentity;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.api.IRadarManager;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * System to manage radar markers for players on join
 */
public class RadarMarkerSystem extends EntityTickingSystem<EntityStore> {
    private final IRadarManager radarManager;
    private final Set<UUID> trackedPlayers = new HashSet<>();

    public RadarMarkerSystem(@Nonnull IRadarManager radarManager) {
        this.radarManager = radarManager;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer) {

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        UUID uuid = PlayerIdentity.uuid(playerRef);
        if (!trackedPlayers.contains(uuid)) {
            radarManager.refreshPlayerMarkers(playerRef);
            trackedPlayers.add(uuid);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }
}
