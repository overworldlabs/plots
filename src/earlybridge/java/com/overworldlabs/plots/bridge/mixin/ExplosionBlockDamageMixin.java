package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Mixin(com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils.class)
public abstract class ExplosionBlockDamageMixin {

    @Redirect(method = "performBlockDamage(Lcom/hypixel/hytale/server/core/entity/LivingEntity;Lcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/math/vector/Vector3i;Lcom/hypixel/hytale/server/core/inventory/ItemStack;Lcom/hypixel/hytale/server/core/asset/type/item/config/ItemTool;Ljava/lang/String;ZFILcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/component/ComponentAccessor;Lcom/hypixel/hytale/component/ComponentAccessor;)Z", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/component/ComponentAccessor;getComponent(Lcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/component/ComponentType;)Lcom/hypixel/hytale/component/Component;"), require = 0)
    private static Object plots$guardExplosionChunkLookup(ComponentAccessor<ChunkStore> chunkAccessor,
            Ref<ChunkStore> chunkRef, Object worldChunkType, LivingEntity entity, Ref<EntityStore> ref,
            Vector3i targetBlockPos, Object itemStack, Object tool, String toolId, boolean matchTool,
            float damageScale, int setBlockSettings, Ref<ChunkStore> chunkReference,
            ComponentAccessor<EntityStore> entityStore, ComponentAccessor<ChunkStore> chunkStore) {
        if (targetBlockPos != null && toolId != null && toolId.toLowerCase().contains("explosion")) {
            Object external = entityStore.getExternalData();
            if (external instanceof EntityStore es && es.getWorld() != null
                    && shouldBlockExplosion(es.getWorld(), targetBlockPos.x, targetBlockPos.y, targetBlockPos.z)) {
                return null;
            }
        }
        if (ref != null && targetBlockPos != null) {
            PlayerRef playerRef = entityStore.getComponent(ref, PlayerRef.getComponentType());
            Object external = entityStore.getExternalData();
            if (playerRef != null && external instanceof EntityStore es && es.getWorld() != null
                    && !check(playerRef.getUuid(), es.getWorld().getName(), targetBlockPos.x, targetBlockPos.y,
                            targetBlockPos.z, "HARVEST")) {
                notifyDenied(playerRef.getUuid(), es.getWorld().getName(), targetBlockPos.x, targetBlockPos.y,
                        targetBlockPos.z, "HARVEST");
                return null;
            }
        }
        return chunkAccessor.getComponent(chunkRef, (com.hypixel.hytale.component.ComponentType<ChunkStore, WorldChunk>) worldChunkType);
    }

    private static boolean check(UUID playerUuid, String worldName, int x, int y, int z, String mode) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) return true;
        Object hook = map.get("plots.use.hook");
        if (hook == null) return true;
        try {
            Method m = hook.getClass().getMethod("check", UUID.class, String.class, double.class, double.class,
                    double.class, String.class);
            Object r = m.invoke(hook, playerUuid, worldName != null ? worldName : "", (double) x, (double) y,
                    (double) z, mode);
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

    private static boolean shouldBlockExplosion(Object world, int x, int y, int z) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) return false;
        Object hook = map.get("plots.explosion.hook");
        if (hook == null) return false;
        try {
            Class<?> worldType = Class.forName("com.hypixel.hytale.server.core.universe.world.World");
            Method m = hook.getClass().getMethod("shouldBlockExplosion", worldType, int.class, int.class, int.class);
            Object r = m.invoke(hook, world, x, y, z);
            return r instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }
}
