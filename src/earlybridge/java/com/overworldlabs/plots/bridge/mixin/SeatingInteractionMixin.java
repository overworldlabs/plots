package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@Mixin(com.hypixel.hytale.builtin.mounts.interactions.SeatingInteraction.class)
public abstract class SeatingInteractionMixin {

    @Redirect(method = "interactWithBlock", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/component/CommandBuffer;getComponent(Lcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/component/ComponentType;)Lcom/hypixel/hytale/component/Component;"), require = 0)
    private Object plots$guardSeatLookup(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            ComponentType<EntityStore, ?> type, World world, CommandBuffer<EntityStore> bufferArg,
            InteractionType interactionType, InteractionContext ctx, ItemStack itemStack, Vector3i target,
            CooldownHandler cooldown) {
        if (ctx != null && ctx.getEntity() != null && target != null) {
            PlayerRef playerRef = bufferArg.getComponent(ctx.getEntity(), PlayerRef.getComponentType());
            if (playerRef != null
                    && !check(playerRef.getUuid(), world != null ? world.getName() : "", target.x, target.y,
                            target.z, "SEAT")) {
                notifyDenied(playerRef.getUuid(), world != null ? world.getName() : "", target.x, target.y,
                        target.z, "SEAT");
                return null;
            }
        }
        return buffer.getComponent(ref, type);
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
