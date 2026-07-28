package dev.stoshe.plots.integration.buildertools;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utility to create a proxy for accessors that filters modifications
 * based on plot permissions.
 */
public final class ProtectedChunkAccessor {
    // BuilderTools accessors commonly use chunk-local X/Z coordinates.
    // In this API, local coordinates are in the 0..31 range.
    private static final int LOCAL_CHUNK_COORD_MAX = 31;

    private ProtectedChunkAccessor() {
    }

    /**
     * Creates a proxied accessor that blocks any modifications on unauthorized
     * plots/roads. It proxies all interfaces implemented by the original object
     * to ensure compatibility.
     */
    public static Object createProxy(Object original, UUID playerUuid) {
        Class<?> originalClass = original.getClass();
        Set<Class<?>> interfaces = new HashSet<>();

        // Collect all interfaces from the class hierarchy
        Class<?> current = originalClass;
        while (current != null) {
            for (Class<?> iface : current.getInterfaces()) {
                interfaces.add(iface);
            }
            current = current.getSuperclass();
        }

        return Proxy.newProxyInstance(
                originalClass.getClassLoader(),
                interfaces.toArray(new Class<?>[0]),
                new ChunkAccessorHandler(original, playerUuid));
    }

    public static class ChunkAccessorHandler implements InvocationHandler {
        private final Object original;
        private final UUID playerUuid;
        private final IPlotManager plotManager;

        public ChunkAccessorHandler(Object original, UUID playerUuid) {
            this.original = original;
            this.playerUuid = playerUuid;
            this.plotManager = Plots.getInstance().getPlotManager();
        }

        private long lastMessageTime = 0;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // Intercept methods that modify blocks or fluids
            if (isMutatingMethod(methodName)) {

                int x = 0, y = 0, z = 0;
                boolean coordsFound = false;

                // Case 1: First three args are x, y, z (Integers)
                if (args != null && args.length >= 3 && args[0] instanceof Integer && args[1] instanceof Integer
                        && args[2] instanceof Integer) {
                    x = (Integer) args[0];
                    y = (Integer) args[1];
                    z = (Integer) args[2];
                    coordsFound = true;
                }
                // Case 2: First arg is a Vector3i
                else if (args != null && args.length > 0 && args[0] instanceof Vector3i) {
                    Vector3i pos = (Vector3i) args[0];
                    x = pos.x;
                    y = pos.y;
                    z = pos.z;
                    coordsFound = true;
                }
                // Case 3: first arg is a BlockAccessor that carries coordinates.
                else if (args != null && args.length > 0 && args[0] instanceof BlockAccessor) {
                    BlockAccessor accessor = (BlockAccessor) args[0];
                    x = accessor.getX();
                    z = accessor.getZ();
                    y = 0;
                    coordsFound = true;
                }
                // Case 4: mutating call without explicit position args (tool keeps internal cursor).
                else {
                    Integer accessorX = readAccessorInt(original, "getX");
                    Integer accessorZ = readAccessorInt(original, "getZ");
                    if (accessorX != null && accessorZ != null) {
                        x = accessorX;
                        z = accessorZ;
                        y = 0;
                        coordsFound = true;
                    }
                }

                if (coordsFound) {
                    int[] worldCoords = resolveWorldCoordinates(x, z);
                    int worldX = worldCoords[0];
                    int worldZ = worldCoords[1];
                    // Strict check: only resolved world coordinates are authoritative.
                    // Allowing a fallback local-coordinate pass can leak edits across plot borders.
                    if (!hasPermission(worldX, y, worldZ)) {
                        sendErrorMessage();

                        return blockedReturnValue(method.getReturnType());
                    }
                } else {
                    // Mutating call on managed world without resolvable coordinates:
                    // deny by default to avoid leaks past plot boundaries.
                    PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
                    if (playerRef == null || playerRef.getWorldUuid() == null) {
                        sendErrorMessage();
                        return blockedReturnValue(method.getReturnType());
                    }
                    var world = Universe.get().getWorld(playerRef.getWorldUuid());
                    if (world == null) {
                        sendErrorMessage();
                        return blockedReturnValue(method.getReturnType());
                    }
                    String worldName = world.getName();
                    if (plotManager.getConfig().isManagedWorld(worldName)) {
                        sendErrorMessage();
                        return blockedReturnValue(method.getReturnType());
                    }
                }
            }

            try {
                return method.invoke(original, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private Object blockedReturnValue(Class<?> returnType) {
            if (returnType.equals(void.class)) {
                return null;
            }
            if (returnType.equals(boolean.class)) {
                return false;
            }
            if (returnType.equals(byte.class)) {
                return (byte) 0;
            }
            if (returnType.equals(short.class)) {
                return (short) 0;
            }
            if (returnType.equals(int.class)) {
                return 0;
            }
            if (returnType.equals(long.class)) {
                return 0L;
            }
            if (returnType.equals(float.class)) {
                return 0f;
            }
            if (returnType.equals(double.class)) {
                return 0d;
            }
            if (returnType.equals(char.class)) {
                return '\0';
            }
            return null;
        }

        private boolean isMutatingMethod(String methodName) {
            if (methodName == null || methodName.isBlank()) {
                return false;
            }

            String normalized = methodName.toLowerCase(java.util.Locale.ROOT);
            return normalized.contains("set")
                    || normalized.contains("break")
                    || normalized.contains("place")
                    || normalized.contains("replace")
                    || normalized.contains("fill")
                    || normalized.contains("paint")
                    || normalized.contains("carve")
                    || normalized.contains("write")
                    || normalized.contains("apply")
                    || normalized.contains("extrude")
                    || normalized.contains("increment")
                    || normalized.contains("decrement")
                    || normalized.contains("overwrite")
                    || normalized.contains("add")
                    || normalized.contains("remove");
        }

        private int[] resolveWorldCoordinates(int x, int z) {
            Integer baseX = readAccessorInt(original, "getX");
            Integer baseZ = readAccessorInt(original, "getZ");
            if (baseX == null || baseZ == null) {
                return new int[] { x, z };
            }

            // Convert local chunk coordinates to world coordinates when detected.
            if (x >= 0 && z >= 0 && x <= LOCAL_CHUNK_COORD_MAX && z <= LOCAL_CHUNK_COORD_MAX) {
                return new int[] { baseX + x, baseZ + z };
            }
            return new int[] { x, z };
        }

        private Integer readAccessorInt(Object target, String methodName) {
            try {
                Method m = target.getClass().getMethod(methodName);
                Object value = m.invoke(target);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        private void sendErrorMessage() {
            long now = System.currentTimeMillis();
            if (now - lastMessageTime < 2000) {
                return; // Cooldown of 2 seconds
            }
            lastMessageTime = now;

            try {
                PlayerRef playerRef = Universe
                        .get().getPlayer(playerUuid);
                if (playerRef != null) {
                    playerRef.sendMessage(ChatUtil.error(
                            Plots.getInstance().getTranslationManager().get("protection.no_permission_buildertools")));
                }
            } catch (Exception e) {
                // Silently fail
            }
        }

        private boolean hasPermission(int x, int y, int z) {
            // Admin bypass
            if (PermissionUtil.hasAdminPermission(playerUuid)) {
                return true;
            }

            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef == null || playerRef.getWorldUuid() == null) {
                return false;
            }
            var world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world == null) {
                return false;
            }

            String worldName = world.getName();
            if (!plotManager.getConfig().isManagedWorld(worldName)) {
                return true;
            }

            if (!plotManager.isInPlot(worldName, x, z)) {
                return false;
            }

            return plotManager.canUseBuilderTools(playerUuid, worldName, x, z);
        }
    }
}
