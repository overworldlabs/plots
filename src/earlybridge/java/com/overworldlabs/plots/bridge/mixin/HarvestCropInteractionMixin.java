package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Mixin(com.hypixel.hytale.builtin.adventure.farming.interactions.HarvestCropInteraction.class)
public abstract class HarvestCropInteractionMixin {

    @Redirect(method = "interactWithBlock", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/builtin/adventure/farming/FarmingUtil;harvest(Lcom/hypixel/hytale/server/core/universe/world/World;Lcom/hypixel/hytale/component/ComponentAccessor;Lcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/server/core/asset/type/blocktype/config/BlockType;ILcom/hypixel/hytale/math/vector/Vector3i;)V"), require = 0)
    private void plots$guardHarvest(World world, ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref,
            BlockType blockType, int rotation, Vector3i target) {
        PlayerRef playerRef = accessor.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null
                && !check(playerRef.getUuid(), world != null ? world.getName() : "", target.x, target.y, target.z,
                        "HARVEST")) {
            notifyDenied(playerRef.getUuid(), world != null ? world.getName() : "", target.x, target.y,
                    target.z, "HARVEST");
            return;
        }
        com.hypixel.hytale.builtin.adventure.farming.FarmingUtil.harvest(world, accessor, ref, blockType, rotation,
                target);
    }

    private static boolean check(UUID playerUuid, String worldName, int x, int y, int z, String mode) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) return true;
        Object hook = map.get("plots.use.hook");
        if (hook == null) return true;
        try {
            Method m = hook.getClass().getMethod("check", UUID.class, String.class, double.class, double.class, double.class, String.class);
            Object r = m.invoke(hook, playerUuid, worldName != null ? worldName : "", (double) x, (double) y, (double) z, mode);
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
            Method m = hook.getClass().getMethod("notifyDenied", UUID.class, String.class, double.class, double.class, double.class, String.class);
            m.invoke(hook, playerUuid, worldName != null ? worldName : "", (double) x, (double) y, (double) z, mode);
        } catch (Exception ignored) {
        }
    }
}
