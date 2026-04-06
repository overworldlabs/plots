package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Mixin(com.hypixel.hytale.server.core.modules.entity.player.PlayerItemEntityPickupSystem.class)
public abstract class PlayerItemEntityPickupSystemMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/component/spatial/SpatialStructure;closest(Lcom/hypixel/hytale/math/vector/Vector3d;)Ljava/lang/Object;"), require = 0)
    private Object plots$guardAutoPickup(SpatialStructure<?> structure, Vector3d position, float dt, int index,
            ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        Object nearest = structure.closest(position);
        if (!(nearest instanceof Ref<?> ref)) {
            return nearest;
        }
        try {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) {
                return nearest;
            }
            Object external = store.getExternalData();
            String worldName = "";
            if (external instanceof EntityStore es && es.getWorld() != null) {
                worldName = es.getWorld().getName();
            }
            if (!check(playerRef.getUuid(), worldName, position.x, position.y, position.z, "AUTO")) {
                notifyDenied(playerRef.getUuid(), worldName, position.x, position.y, position.z, "AUTO");
                return null;
            }
        } catch (Exception ignored) {
        }
        return nearest;
    }

    private static boolean check(UUID playerUuid, String worldName, double x, double y, double z, String mode) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) {
            return true;
        }
        Object hook = map.get("plots.use.hook");
        if (hook == null) {
            return true;
        }
        try {
            Method m = hook.getClass().getMethod("check", UUID.class, String.class, double.class, double.class,
                    double.class, String.class);
            Object result = m.invoke(hook, playerUuid, worldName != null ? worldName : "", x, y, z,
                    mode != null ? mode : "AUTO");
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Exception ignored) {
            return true;
        }
    }

    private static void notifyDenied(UUID playerUuid, String worldName, double x, double y, double z, String mode) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) {
            return;
        }
        Object hook = map.get("plots.use.hook");
        if (hook == null) {
            return;
        }
        try {
            Method m = hook.getClass().getMethod("notifyDenied", UUID.class, String.class, double.class, double.class,
                    double.class, String.class);
            m.invoke(hook, playerUuid, worldName != null ? worldName : "", x, y, z, mode != null ? mode : "AUTO");
        } catch (Exception ignored) {
        }
    }
}
