package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolExtrudeAction;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolLineAction;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolOnUseInteraction;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPasteClipboard;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolStackArea;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.sneakythrow.consumer.ThrowableTriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Mixin(com.hypixel.hytale.builtin.buildertools.BuilderToolsPacketHandler.class)
public abstract class BuilderToolsPacketHandlerMixin {

    @Redirect(method = "handle(Lcom/hypixel/hytale/protocol/packets/buildertools/BuilderToolOnUseInteraction;)V", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/builtin/buildertools/BuilderToolsPlugin;addToQueue(Lcom/hypixel/hytale/server/core/entity/entities/Player;Lcom/hypixel/hytale/server/core/universe/PlayerRef;Lcom/hypixel/hytale/sneakythrow/consumer/ThrowableTriConsumer;)V"), require = 0)
    private void plots$guardOnUse(Player player, PlayerRef playerRef, ThrowableTriConsumer op,
            BuilderToolOnUseInteraction packet) {
        if (deny(playerRef, packet.x, packet.y, packet.z)) {
            return;
        }
        BuilderToolsPlugin.addToQueue(player, playerRef, op);
    }

    @Redirect(method = "handle(Lcom/hypixel/hytale/protocol/packets/buildertools/BuilderToolPasteClipboard;)V", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/builtin/buildertools/BuilderToolsPlugin;addToQueue(Lcom/hypixel/hytale/server/core/entity/entities/Player;Lcom/hypixel/hytale/server/core/universe/PlayerRef;Lcom/hypixel/hytale/sneakythrow/consumer/ThrowableTriConsumer;)V"), require = 0)
    private void plots$guardPaste(Player player, PlayerRef playerRef, ThrowableTriConsumer op,
            BuilderToolPasteClipboard packet) {
        if (deny(playerRef, packet.x, packet.y, packet.z)) {
            return;
        }
        BuilderToolsPlugin.addToQueue(player, playerRef, op);
    }

    @Redirect(method = "handle(Lcom/hypixel/hytale/protocol/packets/buildertools/BuilderToolExtrudeAction;)V", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/builtin/buildertools/BuilderToolsPlugin;addToQueue(Lcom/hypixel/hytale/server/core/entity/entities/Player;Lcom/hypixel/hytale/server/core/universe/PlayerRef;Lcom/hypixel/hytale/sneakythrow/consumer/ThrowableTriConsumer;)V"), require = 0)
    private void plots$guardExtrude(Player player, PlayerRef playerRef, ThrowableTriConsumer op,
            BuilderToolExtrudeAction packet) {
        if (deny(playerRef, packet.x, packet.y, packet.z)) {
            return;
        }
        BuilderToolsPlugin.addToQueue(player, playerRef, op);
    }

    @Redirect(method = "handle(Lcom/hypixel/hytale/protocol/packets/buildertools/BuilderToolLineAction;)V", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/builtin/buildertools/BuilderToolsPlugin;addToQueue(Lcom/hypixel/hytale/server/core/entity/entities/Player;Lcom/hypixel/hytale/server/core/universe/PlayerRef;Lcom/hypixel/hytale/sneakythrow/consumer/ThrowableTriConsumer;)V"), require = 0)
    private void plots$guardLine(Player player, PlayerRef playerRef, ThrowableTriConsumer op,
            BuilderToolLineAction packet) {
        if (deny(playerRef, packet.xStart, packet.yStart, packet.zStart)
                || deny(playerRef, packet.xEnd, packet.yEnd, packet.zEnd)) {
            return;
        }
        BuilderToolsPlugin.addToQueue(player, playerRef, op);
    }

    @Redirect(method = "handle(Lcom/hypixel/hytale/protocol/packets/buildertools/BuilderToolStackArea;)V", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/builtin/buildertools/BuilderToolsPlugin;addToQueue(Lcom/hypixel/hytale/server/core/entity/entities/Player;Lcom/hypixel/hytale/server/core/universe/PlayerRef;Lcom/hypixel/hytale/sneakythrow/consumer/ThrowableTriConsumer;)V"), require = 0)
    private void plots$guardStack(Player player, PlayerRef playerRef, ThrowableTriConsumer op,
            BuilderToolStackArea packet) {
        BlockPosition min = packet.selectionMin;
        BlockPosition max = packet.selectionMax;
        if (min != null && deny(playerRef, min.x, min.y, min.z)) {
            return;
        }
        if (max != null && deny(playerRef, max.x, max.y, max.z)) {
            return;
        }
        BuilderToolsPlugin.addToQueue(player, playerRef, op);
    }

    private boolean deny(PlayerRef playerRef, int x, int y, int z) {
        if (playerRef == null) {
            return false;
        }
        World world = null;
        if (playerRef.getWorldUuid() != null) {
            world = Universe.get().getWorld(playerRef.getWorldUuid());
        }
        if (world == null) {
            return false;
        }
        String worldName = world.getName();
        UUID uuid = playerRef.getUuid();
        boolean allowed = checkBuilderTools(uuid, worldName, x, y, z);
        if (!allowed) {
            notifyDenied(uuid, worldName, x, y, z, "BUILDER");
            return true;
        }
        return false;
    }

    private static boolean checkBuilderTools(UUID playerUuid, String worldName, int x, int y, int z) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) {
            return true;
        }
        Object hook = map.get("plots.buildertools.hook");
        if (hook == null) {
            hook = map.get("plots.use.hook");
        }
        if (hook == null) {
            return true;
        }
        try {
            Method m = hook.getClass().getMethod("check", UUID.class, String.class, double.class, double.class,
                    double.class, String.class);
            Object r = m.invoke(hook, playerUuid, worldName != null ? worldName : "", (double) x, (double) y,
                    (double) z, "BUILDER");
            return !(r instanceof Boolean) || (Boolean) r;
        } catch (Exception ignored) {
            return true;
        }
    }

    private static void notifyDenied(UUID playerUuid, String worldName, int x, int y, int z, String mode) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) return;
        Object hook = map.get("plots.use.hook");
        if (hook == null) return;
        try {
            Method m = hook.getClass().getMethod("notifyDenied", UUID.class, String.class, double.class,
                    double.class, double.class, String.class);
            m.invoke(hook, playerUuid, worldName != null ? worldName : "", (double) x, (double) y, (double) z,
                    mode);
        } catch (Exception ignored) {
        }
    }
}
