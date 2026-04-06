package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;

@Mixin(NPCPlugin.class)
public class NPCPluginSpawnMixin {

    @Redirect(method = "spawnEntity(Lcom/hypixel/hytale/component/Store;ILcom/hypixel/hytale/math/vector/Vector3d;Lcom/hypixel/hytale/math/vector/Vector3f;Lcom/hypixel/hytale/server/core/asset/type/model/config/Model;Lcom/hypixel/hytale/function/consumer/TriConsumer;Lcom/hypixel/hytale/function/consumer/TriConsumer;)Lit/unimi/dsi/fastutil/Pair;", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/component/Store;addEntity(Lcom/hypixel/hytale/component/Holder;Lcom/hypixel/hytale/component/AddReason;)Lcom/hypixel/hytale/component/Ref;"), require = 0)
    private Ref<EntityStore> plots$redirectAddEntity(Store<EntityStore> store, Holder<EntityStore> holder, AddReason reason) {
        TransformComponent transform = holder.getComponent(TransformComponent.getComponentType());
        Vector3d pos = transform != null ? transform.getPosition() : null;
        String worldName = "";
        Object external = store.getExternalData();
        if (external instanceof EntityStore es && es.getWorld() != null) {
            worldName = es.getWorld().getName();
        }

        if (pos != null && shouldBlockSpawn(worldName, (int) Math.floor(pos.x), (int) Math.floor(pos.y),
                (int) Math.floor(pos.z))) {
            return null;
        }
        return store.addEntity(holder, reason);
    }

    private static boolean shouldBlockSpawn(String worldName, int x, int y, int z) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) return false;
        Object hook = map.get("plots.spawn.hook");
        if (hook == null) return false;
        try {
            Method m = hook.getClass().getMethod("shouldBlockSpawn", String.class, int.class, int.class, int.class);
            Object r = m.invoke(hook, worldName, x, y, z);
            return r instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }
}
