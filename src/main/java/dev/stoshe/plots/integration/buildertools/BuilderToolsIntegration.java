package dev.stoshe.plots.integration.buildertools;

import dev.stoshe.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.tooloperations.OperationFactory;
import com.hypixel.hytale.builtin.buildertools.tooloperations.ToolOperation;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.util.Console;
import dev.stoshe.plots.util.PermissionUtil;
import dev.stoshe.plots.util.ChatUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles integration with BuilderTools using reflection to intercept
 * operations.
 */
public class BuilderToolsIntegration {

    private final Map<String, OperationFactory> originalFactories = new ConcurrentHashMap<>();
    private final BuilderToolsScriptedBrushGuard scriptedBrushGuard = new BuilderToolsScriptedBrushGuard();
    private final Map<UUID, Long> builderToolsDenyMessageCooldown = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public void initialize() {
        try {
            ensureHooksInstalled();
        } catch (Exception e) {
            Console.error("Failed to install BuilderTools hooks: " + e.getMessage(), e);
        }
    }

    private synchronized void ensureHooksInstalled() {
        Map<String, OperationFactory> operations = ToolOperation.OPERATIONS;
        if (operations == null || operations.isEmpty()) {
            // BuilderTools may still be starting up; we'll retry from applyMask().
            return;
        }

        boolean hookedAny = false;
        for (Map.Entry<String, OperationFactory> entry : operations.entrySet()) {
            String toolName = entry.getKey();
            OperationFactory currentFactory = entry.getValue();
            if (toolName == null || currentFactory == null) {
                continue;
            }

            // Already wrapped by us
            if (originalFactories.containsKey(toolName)) {
                continue;
            }

            // Hook this operation now
            OperationFactory original = currentFactory;
            if (original != null) {
                originalFactories.put(toolName, original);
                operations.put(toolName, createProtectedFactory(toolName, original));
                hookedAny = true;
            }
        }

        if (hookedAny) {
            initialized = true;
        }
    }

    private OperationFactory createProtectedFactory(String toolName, OperationFactory original) {
        return (ref, player, playerRefArg, packet, accessor) -> {
            UUID playerUuid = null;
            PlayerRef playerRef = playerRefArg;
            if (playerRef == null && player != null) {
                Ref<EntityStore> playerEntityRef = player.getReference();
                if (playerEntityRef != null) {
                    playerRef = playerEntityRef.getStore().getComponent(playerEntityRef, PlayerRef.getComponentType());
                }
            }

            if (playerUuid == null && playerRef != null) {
                playerUuid = PlayerIdentity.uuid(playerRef);
            }

            // Ensure scripted brush config is protected before creating the operation,
            // because ToolOperation may snapshot brush masks during construction.
            if (playerUuid != null) {
                scriptedBrushGuard.ensureProtectedScriptedBrushConfig(player, playerUuid);
            }

            if (playerUuid != null && !canUseBuilderToolsAtPacket(playerUuid, player, packet)) {
                sendBuilderToolsDeniedMessage(playerUuid);
                ToolOperation blocked = createNoOpOperation(packet);
                if (blocked != null) {
                    return blocked;
                }
            }

            ToolOperation operation = original.create(ref, player, playerRefArg, packet, accessor);
            if (operation != null && playerUuid != null) {
                injectProtection(operation, playerUuid, toolName);
            }
            return operation;
        };
    }

    private boolean canUseBuilderToolsAtPacket(UUID playerUuid, Player player, Object packet) {
        if (playerUuid == null) {
            return false;
        }
        if (PermissionUtil.hasAdminPermission(playerUuid, player)) {
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
        if (!Plots.getInstance().getPlotManager().getConfig().isManagedWorld(worldName)) {
            return true;
        }

        // Packet-level gate should only validate the primary target point.
        // Fine-grained clipping (outside border) is handled per-block by masks/accessor proxy,
        // so edits inside the plot still apply even when the brush overlaps outside.
        Integer x = readIntField(packet, "x");
        Integer z = readIntField(packet, "z");
        if (x == null || z == null) {
            // Unknown packet coordinates on managed worlds are denied by default to prevent
            // scripted/tool packets from leaking edits outside plot borders.
            return false;
        }
        var pm = Plots.getInstance().getPlotManager();
        return pm.canUseBuilderTools(playerUuid, worldName, x, z);
    }

    private Integer readIntField(Object packet, String fieldName) {
        try {
            Field field = packet.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(packet);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private ToolOperation createNoOpOperation(Object packet) {
        if (!ToolOperation.class.isInterface()) {
            return null;
        }
        Vector3i fallbackPosition = extractPosition(packet);
        InvocationHandler handler = (proxy, method, args) -> defaultOperationReturn(method, fallbackPosition);
        try {
            Object proxy = Proxy.newProxyInstance(
                    ToolOperation.class.getClassLoader(),
                    new Class<?>[] { ToolOperation.class },
                    handler);
            return (ToolOperation) proxy;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object defaultOperationReturn(Method method, Vector3i fallbackPosition) {
        String name = method.getName();
        Class<?> returnType = method.getReturnType();

        if ("getPosition".equals(name) && returnType.isAssignableFrom(Vector3i.class)) {
            return fallbackPosition;
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

    private Vector3i extractPosition(Object packet) {
        Integer x = readIntField(packet, "x");
        Integer y = readIntField(packet, "y");
        Integer z = readIntField(packet, "z");
        return new Vector3i(
                x != null ? x : 0,
                y != null ? y : 64,
                z != null ? z : 0);
    }

    private void sendBuilderToolsDeniedMessage(UUID playerUuid) {
        long now = System.currentTimeMillis();
        long last = builderToolsDenyMessageCooldown.getOrDefault(playerUuid, 0L);
        if (now - last < 2000L) {
            return;
        }
        builderToolsDenyMessageCooldown.put(playerUuid, now);
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef != null) {
            playerRef.sendMessage(ChatUtil.error(
                    Plots.getInstance().getTranslationManager().get("protection.no_permission_buildertools")));
        }
    }

    private void injectProtection(ToolOperation operation, UUID playerUuid, String toolName) {
        try {
            PlotProtectionMask protectedMask = new PlotProtectionMask(playerUuid, null);
            injectMaskIntoObject(operation, protectedMask, playerUuid, new HashSet<>(), 0);
        } catch (Exception e) {
            Console.warning("[BuilderToolsIntegration] Failed to inject protection for " + toolName + ": "
                    + e.getMessage());
        }
    }

    /**
     * Recursively inject mask and protected accessor into all relevant fields in an
     * object
     */
    private int injectMaskIntoObject(Object obj, PlotProtectionMask mask, UUID playerUuid, Set<Object> visited,
            int depth) {
        if (obj == null || depth > 8 || visited.contains(obj)) {
            return 0;
        }
        visited.add(obj);

        int count = 0;
        Class<?> clazz = obj.getClass();

        // Get all fields including inherited ones
        while (clazz != null && !clazz.equals(Object.class)) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);

                    if (value == null)
                        continue;

                    // Check if this field is a BlockMask
                    if (value instanceof BlockMask) {
                        if (!(value instanceof PlotProtectionMask)) {
                            // Wrap the existing mask
                            PlotProtectionMask wrappedMask = new PlotProtectionMask(mask.getPlayerUuid(),
                                    (BlockMask) value);
                            field.set(obj, wrappedMask);
                            count++;
                        }
                    }
                    // Check if this field is a ChunkAccessor or related world accessor
                    else if (value instanceof BlockAccessor ||
                            value.getClass().getName().endsWith(".ChunkAccessor") ||
                            value.getClass().getName().contains("FluidTicker$Accessor")) {
                        // Determine if it's already a Proxy created by us
                        if (!Proxy.isProxyClass(value.getClass())
                                || !(Proxy.getInvocationHandler(
                                        value) instanceof ProtectedChunkAccessor.ChunkAccessorHandler)) {
                            Object wrappedAccessor = ProtectedChunkAccessor.createProxy(value, playerUuid);
                            field.set(obj, wrappedAccessor);
                            count++;
                        }
                    }
                    // Recursively check nested objects
                    else if (!isPrimitiveOrWrapper(value.getClass())
                            && !value.getClass().getName().startsWith("java.lang.String")) {
                        count += injectMaskIntoObject(value, mask, playerUuid, visited, depth + 1);
                    }
                } catch (Exception e) {
                    // Ignore fields we can't access
                }
            }
            clazz = clazz.getSuperclass();
        }

        return count;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(Boolean.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(Character.class);
    }

    public void applyMask(Ref<EntityStore> playerRef, ComponentAccessor<EntityStore> accessor) {
        try {
            ensureHooksInstalled();

            Player player = playerRef.getStore().getComponent(playerRef, Player.getComponentType());
            PlayerRef pr = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
            if (player == null || pr == null)
                return;

            BuilderToolsPlugin.BuilderState state = BuilderToolsPlugin.get().getBuilderState(player, pr);
            if (state == null)
                return;

            // Apply global mask (for tools that might check it, though Extrude doesn't)
            BlockMask current = state.getGlobalMask();
            if (!(current instanceof PlotProtectionMask)) {
                state.setGlobalMask(new PlotProtectionMask(PlayerIdentity.uuid(pr), current), accessor);
            }

            scriptedBrushGuard.ensureProtectedScriptedBrushConfig(player, PlayerIdentity.uuid(pr));
        } catch (Exception e) {
            // Silently fail
        }
    }

    public void clearMask(Ref<EntityStore> playerRef, ComponentAccessor<EntityStore> accessor) {
        try {
            Player player = playerRef.getStore().getComponent(playerRef, Player.getComponentType());
            PlayerRef pr = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
            if (player == null || pr == null)
                return;

            BuilderToolsPlugin.BuilderState state = BuilderToolsPlugin.get().getBuilderState(player, pr);
            if (state != null && state.getGlobalMask() instanceof PlotProtectionMask) {
                state.setGlobalMask(null, accessor);
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    public void shutdown() {
        if (!initialized && originalFactories.isEmpty())
            return;
        Map<String, OperationFactory> operations = ToolOperation.OPERATIONS;
        originalFactories.forEach(operations::put);
        originalFactories.clear();
        scriptedBrushGuard.reset();
        initialized = false;
    }
}
