package com.overworldlabs.plots.bridge.hook;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HookResolver {
    private static final String REGISTRY_KEY = "plots.hook.registry";
    private static final String GENERIC_CHECK_HOOK_KEY = "plots.use.hook";
    private static final String COMMAND_HOOK_KEY = "plots.command.hook";
    private static final String EXPLOSION_HOOK_KEY = "plots.explosion.hook";
    private static final String SPAWN_HOOK_KEY = "plots.spawn.hook";
    private static final String DEATH_HOOK_KEY = "plots.death.hook";
    private static final String DURABILITY_HOOK_KEY = "plots.durability.hook";
    private static final Map<Class<?>, MethodHandle> CHECK_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> NOTIFY_DENIED_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> COMMAND_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> DENY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> EXPLOSION_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> SPAWN_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> KEEP_INV_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> DURABILITY_CACHE = new ConcurrentHashMap<>();

    private HookResolver() {
    }

    public static boolean check(@Nullable UUID playerUuid, @Nullable String worldName, int x, int y, int z,
            @Nullable String mode) {
        if (playerUuid == null) {
            return true;
        }
        Object hook = getHook(GENERIC_CHECK_HOOK_KEY);
        if (hook == null) {
            return true;
        }
        MethodHandle mh = CHECK_CACHE.computeIfAbsent(hook.getClass(), HookResolver::findCheckHandle);
        if (mh == null) {
            return true;
        }
        String useWorld = worldName;
        if (useWorld == null || useWorld.isBlank()) {
            PlayerRef ref = Universe.get().getPlayer(playerUuid);
            if (ref != null && ref.getWorldUuid() != null) {
                World world = Universe.get().getWorld(ref.getWorldUuid());
                if (world != null) {
                    useWorld = world.getName();
                }
            }
        }
        if (useWorld == null) {
            useWorld = "";
        }
        if (mode == null) {
            mode = "INTERACT";
        }
        try {
            return (boolean) mh.invoke(hook, playerUuid, useWorld, (double) x, (double) y, (double) z, mode);
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void notifyDenied(@Nullable UUID playerUuid, @Nullable String worldName, int x, int y, int z,
            @Nullable String mode) {
        if (playerUuid == null) {
            return;
        }
        Object hook = getHook(GENERIC_CHECK_HOOK_KEY);
        if (hook == null) {
            return;
        }
        MethodHandle mh = NOTIFY_DENIED_CACHE.computeIfAbsent(hook.getClass(), HookResolver::findNotifyDeniedHandle);
        if (mh == null) {
            return;
        }
        String useWorld = worldName;
        if (useWorld == null || useWorld.isBlank()) {
            PlayerRef ref = Universe.get().getPlayer(playerUuid);
            if (ref != null && ref.getWorldUuid() != null) {
                World world = Universe.get().getWorld(ref.getWorldUuid());
                if (world != null) {
                    useWorld = world.getName();
                }
            }
        }
        if (useWorld == null) {
            useWorld = "";
        }
        if (mode == null) {
            mode = "INTERACT";
        }
        try {
            mh.invoke(hook, playerUuid, useWorld, (double) x, (double) y, (double) z, mode);
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    private static Object getHook(String key) {
        Object raw = System.getProperties().get(REGISTRY_KEY);
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        return map.get(key);
    }

    @Nullable
    private static MethodHandle findCheckHandle(Class<?> hookClass) {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    hookClass,
                    "check",
                    MethodType.methodType(boolean.class, UUID.class, String.class, double.class, double.class,
                            double.class, String.class));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static MethodHandle findNotifyDeniedHandle(Class<?> hookClass) {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    hookClass,
                    "notifyDenied",
                    MethodType.methodType(void.class, UUID.class, String.class, double.class, double.class,
                            double.class, String.class));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean shouldBlockCommand(@Nullable CommandSender sender, @Nullable String rawCommand) {
        if (sender == null) {
            return false;
        }
        Object hook = getHook(COMMAND_HOOK_KEY);
        if (hook == null) {
            return false;
        }
        MethodHandle mh = COMMAND_CACHE.computeIfAbsent(hook.getClass(), HookResolver::findCommandHandle);
        if (mh == null) {
            return false;
        }
        try {
            return (boolean) mh.invoke(hook, sender, rawCommand != null ? rawCommand : "");
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    public static String getCommandDenialMessage() {
        Object hook = getHook(COMMAND_HOOK_KEY);
        if (hook == null) {
            return null;
        }
        MethodHandle mh = DENY_CACHE.computeIfAbsent(hook.getClass(), HookResolver::findDenyHandle);
        if (mh == null) {
            return null;
        }
        try {
            Object value = mh.invoke(hook);
            return value != null ? String.valueOf(value) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean shouldBlockExplosion(@Nullable World world, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        Object hook = getHook(EXPLOSION_HOOK_KEY);
        if (hook == null) {
            return false;
        }
        MethodHandle mh = EXPLOSION_CACHE.computeIfAbsent(hook.getClass(), HookResolver::findExplosionHandle);
        if (mh == null) {
            return false;
        }
        try {
            return (boolean) mh.invoke(hook, world, x, y, z);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static MethodHandle findCommandHandle(Class<?> hookClass) {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    hookClass,
                    "shouldBlockCommand",
                    MethodType.methodType(boolean.class, CommandSender.class, String.class));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static MethodHandle findDenyHandle(Class<?> hookClass) {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    hookClass,
                    "getDenialMessage",
                    MethodType.methodType(String.class));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static MethodHandle findExplosionHandle(Class<?> hookClass) {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    hookClass,
                    "shouldBlockExplosion",
                    MethodType.methodType(boolean.class, World.class, int.class, int.class, int.class));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean shouldBlockSpawn(@Nullable String worldName, int x, int y, int z) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        Object hook = getHook(SPAWN_HOOK_KEY);
        if (hook == null) {
            return false;
        }
        MethodHandle mh = SPAWN_CACHE.computeIfAbsent(hook.getClass(), HookResolver::findSpawnHandle);
        if (mh == null) {
            return false;
        }
        try {
            return (boolean) mh.invoke(hook, worldName, x, y, z);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean shouldKeepInventory(@Nullable UUID uuid, @Nullable String worldName, int x, int y, int z) {
        if (uuid == null || worldName == null || worldName.isBlank()) {
            return false;
        }
        Object hook = getHook(DEATH_HOOK_KEY);
        if (hook == null) {
            return false;
        }
        MethodHandle mh = KEEP_INV_CACHE.computeIfAbsent(hook.getClass(), HookResolver::findKeepInvHandle);
        if (mh == null) {
            return false;
        }
        try {
            return (boolean) mh.invoke(hook, uuid, worldName, x, y, z);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean shouldPreventDurabilityLoss(@Nullable UUID uuid, @Nullable String worldName, int x, int y,
            int z) {
        if (uuid == null || worldName == null || worldName.isBlank()) {
            return false;
        }
        Object hook = getHook(DURABILITY_HOOK_KEY);
        if (hook == null) {
            return false;
        }
        MethodHandle mh = DURABILITY_CACHE.computeIfAbsent(hook.getClass(), HookResolver::findDurabilityHandle);
        if (mh == null) {
            return false;
        }
        try {
            return (boolean) mh.invoke(hook, uuid, worldName, x, y, z);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static MethodHandle findSpawnHandle(Class<?> hookClass) {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    hookClass,
                    "shouldBlockSpawn",
                    MethodType.methodType(boolean.class, String.class, int.class, int.class, int.class));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static MethodHandle findKeepInvHandle(Class<?> hookClass) {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    hookClass,
                    "shouldKeepInventory",
                    MethodType.methodType(boolean.class, UUID.class, String.class, int.class, int.class, int.class));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static MethodHandle findDurabilityHandle(Class<?> hookClass) {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    hookClass,
                    "shouldPreventDurabilityLoss",
                    MethodType.methodType(boolean.class, UUID.class, String.class, int.class, int.class, int.class));
        } catch (Exception ignored) {
            return null;
        }
    }
}
