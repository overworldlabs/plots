package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.BlockInteractionUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Mixin(com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils.class)
public abstract class BlockHarvestUtilsMixin {

    private static final ThreadLocal<Boolean> plots$denied = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Vector3i> plots$target = new ThreadLocal<>();

    @Shadow
    protected static void removeBlock(Vector3i target, BlockType blockType, int setSettings, Ref<ChunkStore> chunkRef,
            ComponentAccessor<ChunkStore> chunkAccessor) {
        throw new UnsupportedOperationException("shadow");
    }

    @Redirect(method = "performPickupByInteraction", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/core/asset/type/blocktype/config/BlockType;isUnknown()Z"), require = 0)
    private static boolean plots$initState(BlockType blockType) {
        plots$denied.set(Boolean.FALSE);
        plots$target.remove();
        return blockType.isUnknown();
    }

    @Redirect(method = "performPickupByInteraction", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/core/universe/world/chunk/section/BlockSection;getRotationIndex(III)I"), require = 0)
    private static int plots$captureTarget(BlockSection section, int x, int y, int z) {
        plots$target.set(new Vector3i(x, y, z));
        return section.getRotationIndex(x, y, z);
    }

    @Redirect(method = "performPickupByInteraction", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/core/modules/interaction/BlockInteractionUtils;isNaturalAction(Lcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/component/ComponentAccessor;)Z"), require = 0)
    private static boolean plots$checkPermission(Ref<EntityStore> playerRefEntity,
            ComponentAccessor<EntityStore> entityAccessor) {
        try {
            if (playerRefEntity != null && entityAccessor != null) {
                PlayerRef playerRef = entityAccessor.getComponent(playerRefEntity, PlayerRef.getComponentType());
                Vector3i target = plots$target.get();
                if (playerRef != null && target != null) {
                    String worldName = "";
                    Object ext = entityAccessor.getExternalData();
                    if (ext instanceof EntityStore es && es.getWorld() != null) {
                        worldName = es.getWorld().getName();
                    }
                    if (!check(playerRef.getUuid(), worldName, target.x, target.y, target.z, "HARVEST")) {
                        plots$denied.set(Boolean.TRUE);
                        notifyDenied(playerRef.getUuid(), worldName, target.x, target.y, target.z, "HARVEST");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return BlockInteractionUtils.isNaturalAction(playerRefEntity, entityAccessor);
    }

    @Redirect(method = "performPickupByInteraction", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/core/modules/interaction/BlockHarvestUtils;removeBlock(Lcom/hypixel/hytale/math/vector/Vector3i;Lcom/hypixel/hytale/server/core/asset/type/blocktype/config/BlockType;ILcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/component/ComponentAccessor;)V"), require = 0)
    private static void plots$guardRemoveBlock(Vector3i target, BlockType blockType, int setSettings,
            Ref<ChunkStore> chunkRef, ComponentAccessor<ChunkStore> chunkAccessor) {
        if (Boolean.TRUE.equals(plots$denied.get())) {
            try {
                BlockChunk chunk = chunkAccessor.getComponent(chunkRef, BlockChunk.getComponentType());
                if (chunk != null) {
                    BlockSection section = chunk.getSectionAtBlockY(target.getY());
                    if (section != null) {
                        section.invalidateBlock(target.getX(), target.getY(), target.getZ());
                    }
                }
            } catch (Exception ignored) {
            }
            return;
        }
        removeBlock(target, blockType, setSettings, chunkRef, chunkAccessor);
    }

    @Redirect(method = "performPickupByInteraction", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/core/entity/ItemUtils;interactivelyPickupItem(Lcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/server/core/inventory/ItemStack;Lcom/hypixel/hytale/math/vector/Vector3d;Lcom/hypixel/hytale/component/ComponentAccessor;)V"), require = 0)
    private static void plots$guardPickup(Ref<EntityStore> playerRefEntity, ItemStack drop, Vector3d pos,
            ComponentAccessor<EntityStore> entityAccessor) {
        if (Boolean.TRUE.equals(plots$denied.get())) {
            return;
        }
        ItemUtils.interactivelyPickupItem(playerRefEntity, drop, pos, entityAccessor);
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
}
