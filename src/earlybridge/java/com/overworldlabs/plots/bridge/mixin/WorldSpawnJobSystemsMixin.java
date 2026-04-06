package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.server.spawning.SpawnTestResult;
import com.hypixel.hytale.server.spawning.SpawningContext;
import com.hypixel.hytale.server.spawning.world.system.WorldSpawnJobSystems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;

@Mixin(WorldSpawnJobSystems.class)
public class WorldSpawnJobSystemsMixin {

    @Redirect(method = "trySpawn", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/spawning/SpawningContext;canSpawn()Lcom/hypixel/hytale/server/spawning/SpawnTestResult;"), require = 0)
    private static SpawnTestResult plots$redirectCanSpawnNoArg(SpawningContext context) {
        if (context != null && context.world != null && shouldBlockSpawn(
                context.world.getName(),
                (int) Math.floor(context.xSpawn),
                (int) Math.floor(context.ySpawn),
                (int) Math.floor(context.zSpawn))) {
            return SpawnTestResult.FAIL_INVALID_POSITION;
        }
        return context.canSpawn();
    }

    @Redirect(method = "trySpawn", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/spawning/SpawningContext;canSpawn(ZZ)Lcom/hypixel/hytale/server/spawning/SpawnTestResult;"), require = 0)
    private static SpawnTestResult plots$redirectCanSpawn2Arg(SpawningContext context, boolean testOverlapBlocks,
            boolean testOverlapEntities) {
        if (context != null && context.world != null && shouldBlockSpawn(
                context.world.getName(),
                (int) Math.floor(context.xSpawn),
                (int) Math.floor(context.ySpawn),
                (int) Math.floor(context.zSpawn))) {
            return SpawnTestResult.FAIL_INVALID_POSITION;
        }
        return context.canSpawn(testOverlapBlocks, testOverlapEntities);
    }

    private static boolean shouldBlockSpawn(String worldName, int x, int y, int z) {
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
            Object result = m.invoke(hook, worldName, x, y, z);
            return result instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }
}
