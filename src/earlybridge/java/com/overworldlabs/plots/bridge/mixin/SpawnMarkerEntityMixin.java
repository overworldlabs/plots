package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.server.spawning.SpawningContext;
import com.hypixel.hytale.server.spawning.SpawnTestResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;

@Mixin(com.hypixel.hytale.server.spawning.spawnmarkers.SpawnMarkerEntity.class)
public abstract class SpawnMarkerEntityMixin {

    @Redirect(method = "spawnNPC", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/spawning/SpawningContext;canSpawn(ZZ)Lcom/hypixel/hytale/server/spawning/SpawnTestResult;"), require = 0)
    private SpawnTestResult plots$guardSpawn(SpawningContext context, boolean testOverlapBlocks,
            boolean testOverlapEntities) {
        if (shouldBlockSpawn(context)) {
            return SpawnTestResult.FAIL_INVALID_POSITION;
        }
        return context.canSpawn(testOverlapBlocks, testOverlapEntities);
    }

    private static boolean shouldBlockSpawn(SpawningContext context) {
        if (context == null || context.world == null) {
            return false;
        }
        String world = context.world.getName();
        int x = (int) Math.floor(context.xSpawn);
        int y = (int) Math.floor(context.ySpawn);
        int z = (int) Math.floor(context.zSpawn);
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) {
            return false;
        }
        Object hook = map.get("plots.spawn.hook");
        if (hook == null) {
            return false;
        }
        try {
            Method m = hook.getClass().getMethod("shouldBlockSpawn", String.class, int.class, int.class, int.class);
            Object result = m.invoke(hook, world, x, y, z);
            return result instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }
}
