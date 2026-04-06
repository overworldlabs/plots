package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Mixin(com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems.DropPlayerDeathItems.class)
public abstract class DeathItemDropMixin {

    @Redirect(method = "onComponentAdded", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/core/modules/entity/damage/DeathComponent;getItemsLossMode()Lcom/hypixel/hytale/server/core/asset/type/gameplay/DeathConfig$ItemsLossMode;"), require = 0)
    private DeathConfig.ItemsLossMode plots$keepInventory(DeathComponent deathComponent, Ref<EntityStore> ref,
            DeathComponent component, Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        if (ref != null && store != null) {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            Object external = store.getExternalData();
            if (playerRef != null && transform != null && external instanceof EntityStore es && es.getWorld() != null) {
                var pos = transform.getPosition();
                if (shouldKeepInventory(playerRef.getUuid(), es.getWorld().getName(),
                        (int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z))) {
                    return DeathConfig.ItemsLossMode.NONE;
                }
            }
        }
        return deathComponent.getItemsLossMode();
    }

    private static boolean shouldKeepInventory(UUID playerUuid, String worldName, int x, int y, int z) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) return false;
        Object hook = map.get("plots.death.hook");
        if (hook == null) return false;
        try {
            Method m = hook.getClass().getMethod("shouldKeepInventory", UUID.class, String.class, int.class,
                    int.class, int.class);
            Object r = m.invoke(hook, playerUuid, worldName, x, y, z);
            return r instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }
}
