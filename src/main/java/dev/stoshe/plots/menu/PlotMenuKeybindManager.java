package dev.stoshe.plots.menu;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.ui.PlotMenuPage;
import dev.stoshe.plots.util.PermissionUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens the plot management menu when the player taps the walk key (Alt),
 * mirroring the skyblock menu-open keybind. The walk-key edge is detected from
 * inbound {@link ClientMovement} packets.
 */
public class PlotMenuKeybindManager {
    private final Plots plugin;
    private final Map<UUID, Boolean> walkKeyState = new ConcurrentHashMap<>();
    private PacketFilter packetFilter;
    private boolean started = false;

    public PlotMenuKeybindManager(Plots plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (this.started) {
            return;
        }
        this.packetFilter = (packetHandler, packet) -> {
            if (!(packetHandler instanceof GamePacketHandler gameHandler)) {
                return false;
            }
            if (!(packet instanceof ClientMovement movement) || movement.movementStates == null) {
                return false;
            }
            PlayerRef playerRef = gameHandler.getPlayerRef();
            if (playerRef == null || playerRef.getUuid() == null) {
                return false;
            }
            boolean walkingNow = movement.movementStates.walking
                    && !movement.movementStates.running
                    && !movement.movementStates.sprinting;
            Boolean previous = this.walkKeyState.put(playerRef.getUuid(), walkingNow);
            if (walkingNow && (previous == null || !previous)) {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref != null && ref.isValid()) {
                    Store<EntityStore> store = ref.getStore();
                    EntityStore entityStore = store == null ? null : (EntityStore) store.getExternalData();
                    World world = entityStore == null ? null : entityStore.getWorld();
                    if (world != null) {
                        world.execute(() -> this.openMenuForPlayer(playerRef));
                    }
                }
            }
            return false;
        };
        PacketAdapters.registerInbound(this.packetFilter);
        this.started = true;
    }

    public void shutdown() {
        if (!this.started) {
            return;
        }
        try {
            if (this.packetFilter != null) {
                PacketAdapters.deregisterInbound(this.packetFilter);
            }
        } catch (Exception ignored) {
        }
        this.walkKeyState.clear();
        this.packetFilter = null;
        this.started = false;
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event == null || event.getPlayerRef() == null || event.getPlayerRef().getUuid() == null) {
            return;
        }
        this.walkKeyState.remove(event.getPlayerRef().getUuid());
    }

    private void openMenuForPlayer(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        World world = player.getWorld();
        if (world == null) {
            EntityStore entityStore = (EntityStore) store.getExternalData();
            world = entityStore == null ? null : entityStore.getWorld();
        }
        if (world == null) {
            return;
        }
        // Only open the menu inside managed plot worlds.
        if (plugin.getWorldManager() == null || !plugin.getWorldManager().isPlotWorld(world.getName())) {
            return;
        }
        // Only open the menu when the player is actually standing on a plot. Off-plot
        // (roads, intersections) the Alt tap does nothing — no menu, no chat spam.
        if (plugin.getPlotManager() == null || playerRef.getTransform() == null
                || playerRef.getTransform().getPosition() == null) {
            return;
        }
        org.joml.Vector3d pos = playerRef.getTransform().getPosition();
        Plot plot = plugin.getPlotManager().getPlotAt(world.getName(),
                (int) Math.floor(pos.x), (int) Math.floor(pos.z));
        if (plot == null) {
            return;
        }
        // Access is limited to the plot's owner and its trusted members (admins too).
        // The Alt tap silently does nothing for anyone else — no menu, no chat spam.
        UUID uuid = playerRef.getUuid();
        if (!plot.isOwnerOrMember(uuid) && !PermissionUtil.hasAdminPermission(uuid)) {
            return;
        }
        PlotMenuPage.open(player, ref, store, playerRef, world, this.plugin);
    }
}
