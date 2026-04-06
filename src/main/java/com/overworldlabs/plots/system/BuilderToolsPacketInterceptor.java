package com.overworldlabs.plots.system;

import com.overworldlabs.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.PrototypePlayerBuilderToolSettings;
import com.hypixel.hytale.builtin.buildertools.scriptedbrushes.BrushConfig;
import com.hypixel.hytale.builtin.buildertools.tooloperations.ToolOperation;
import com.hypixel.hytale.server.core.universe.Universe;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolExtrudeAction;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolOnUseInteraction;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPasteClipboard;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.ConsoleColors;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class to intercept BuilderTools packets using reflection
 */
public class BuilderToolsPacketInterceptor {
    private final PlotManager plotManager;
    private final TranslationManager translationManager;
    private Field hookedHandlerField;
    private Object originalHandler;
    private boolean hooked = false;

    public BuilderToolsPacketInterceptor(@Nonnull PlotManager plotManager,
            @Nonnull TranslationManager translationManager) {
        this.plotManager = plotManager;
        this.translationManager = translationManager;
    }

    /**
     * Hook into BuilderToolsPacketHandler to intercept packet handling
     */
    public boolean hookPacketHandler() {
        if (hooked) {
            return true;
        }

        try {
            Field handlerField = resolveHandlerField();
            if (handlerField == null) {
                ConsoleColors.warning(
                        "[BuilderToolsPacketInterceptor] Failed to hook packet handler: no compatible handler field found.");
                return false;
            }

            originalHandler = handlerField.get(BuilderToolsPlugin.get());

            if (originalHandler == null) {
                return false;
            }

            Class<?>[] interfaces = resolveProxyInterfaces(originalHandler, handlerField);
            if (interfaces.length == 0) {
                ConsoleColors.warning("[BuilderToolsPacketInterceptor] Could not hook packet handler: no interfaces.");
                return false;
            }

            Object proxiedHandler = Proxy.newProxyInstance(
                    originalHandler.getClass().getClassLoader(),
                    interfaces,
                    new PacketHandlerProxy(originalHandler));

            if (!handlerField.getType().isInstance(proxiedHandler)) {
                ConsoleColors.warning(
                        "[BuilderToolsPacketInterceptor] Could not hook packet handler: proxy type not assignable.");
                return false;
            }

            // Replace the handler
            handlerField.set(BuilderToolsPlugin.get(), proxiedHandler);
            hookedHandlerField = handlerField;

            hooked = true;
            return true;
        } catch (Exception e) {
            ConsoleColors.warning("[BuilderToolsPacketInterceptor] Failed to hook packet handler: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unhook the packet handler
     */
    public void unhook() {
        if (!hooked || originalHandler == null) {
            return;
        }

        try {
            Field handlerField = hookedHandlerField != null ? hookedHandlerField : resolveHandlerField();
            if (handlerField == null) {
                return;
            }
            handlerField.setAccessible(true);
            handlerField.set(BuilderToolsPlugin.get(), originalHandler);

            hooked = false;

        } catch (Exception e) {
            ConsoleColors.warning("[BuilderToolsPacketInterceptor] Failed to unhook packet handler: " + e.getMessage());
        }
    }

    @Nullable
    private Field resolveHandlerField() {
        Object plugin = BuilderToolsPlugin.get();
        if (plugin == null) {
            return null;
        }

        String[] preferredNames = { "packetHandler", "builderToolsPacketHandler", "handler" };
        for (String name : preferredNames) {
            try {
                Field field = BuilderToolsPlugin.class.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(plugin);
                if (value != null) {
                    return field;
                }
            } catch (Exception ignored) {
            }
        }

        try {
            Field[] fields = BuilderToolsPlugin.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(plugin);
                if (value == null) {
                    continue;
                }
                Class<?> valueClass = value.getClass();
                String valueClassName = valueClass.getName();
                if (valueClassName.contains("PacketHandler")
                        || valueClassName.contains("packet")
                        || valueClassName.contains("buildertools")) {
                    return field;
                }
                if (valueClass.getInterfaces().length > 0) {
                    return field;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    @Nonnull
    private Class<?>[] resolveProxyInterfaces(@Nonnull Object handler, @Nonnull Field handlerField) {
        Set<Class<?>> result = new LinkedHashSet<>();
        Class<?> clazz = handler.getClass();
        while (clazz != null) {
            for (Class<?> iface : clazz.getInterfaces()) {
                if (isProxyableInterface(iface)) {
                    result.add(iface);
                }
            }
            clazz = clazz.getSuperclass();
        }

        Class<?> declaredType = handlerField.getType();
        if (declaredType.isInterface() && isProxyableInterface(declaredType)) {
            result.add(declaredType);
        }

        return result.toArray(new Class<?>[0]);
    }

    private boolean isProxyableInterface(@Nullable Class<?> iface) {
        if (iface == null || !iface.isInterface()) {
            return false;
        }
        // Dynamic proxies cannot implement sealed interfaces unless explicitly permitted.
        if (iface.isSealed()) {
            return false;
        }
        return !iface.getName().equals("java.lang.constant.ConstantDesc");
    }

    /**
     * Check if a packet should be blocked based on plot permissions
     */
    @SuppressWarnings("null")
    private boolean shouldBlockPacket(Object packet, @Nonnull UUID playerUuid, @Nullable Player player) {
        // Only care about BuilderTools packets
        if (packet == null || packet.getClass().getName() == null ||
                !packet.getClass().getName().contains(".packets.buildertools.")) {
            return false;
        }

        // Admin bypass
        if (PermissionUtil.hasAdminPermission(playerUuid, player)) {
            return false;
        }

        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null || playerRef.getWorldUuid() == null) {
            return true;
        }
        var world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return true;
        }
        String worldName = world.getName();
        if (!plotManager.getConfig().isManagedWorld(worldName)) {
            return false;
        }

        List<int[]> points = extractPoints(packet, playerUuid);
        if (points.isEmpty()) {
            Integer x = readIntField(packet, "x");
            Integer z = readIntField(packet, "z");
            if (x == null || z == null) {
                // Managed worlds must fail-closed for unknown packet coordinates.
                // Some scripted/complex tools may omit canonical x/z fields.
                return true;
            }
            return !hasPermissionAt(playerUuid, worldName, x, z);
        }

        // Strict policy for scripted brushes/extrude: if any sampled point is outside
        // permission, block the full packet to prevent leaks beyond plot borders.
        for (int[] point : points) {
            if (point == null || point.length < 2) {
                continue;
            }
            if (!hasPermissionAt(playerUuid, worldName, point[0], point[1])) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermissionAt(@Nonnull UUID playerUuid, @Nonnull String worldName, int x, int z) {
        return plotManager.canUseBuilderTools(playerUuid, worldName, x, z);
    }

    @Nonnull
    private List<int[]> extractPoints(@Nonnull Object packet, @Nonnull UUID playerUuid) {
        List<int[]> points = new ArrayList<>();

        if (packet instanceof BuilderToolExtrudeAction) {
            BuilderToolExtrudeAction p = (BuilderToolExtrudeAction) packet;
            points.add(new int[] { p.x, p.z });
            return points;
        }

        if (packet instanceof BuilderToolOnUseInteraction) {
            BuilderToolOnUseInteraction p = (BuilderToolOnUseInteraction) packet;
            points.add(new int[] { p.x, p.z });
            return points;
        }

        if (packet instanceof BuilderToolPasteClipboard) {
            BuilderToolPasteClipboard p = (BuilderToolPasteClipboard) packet;
            points.add(new int[] { p.x, p.z });
            return points;
        }

        Integer x = readIntField(packet, "x");
        Integer z = readIntField(packet, "z");
        if (x != null && z != null) {
            points.add(new int[] { x, z });
        }

        Integer xStart = readIntField(packet, "xStart");
        Integer zStart = readIntField(packet, "zStart");
        Integer xEnd = readIntField(packet, "xEnd");
        Integer zEnd = readIntField(packet, "zEnd");
        if (xStart != null && zStart != null && xEnd != null && zEnd != null) {
            int minX = Math.min(xStart, xEnd);
            int maxX = Math.max(xStart, xEnd);
            int minZ = Math.min(zStart, zEnd);
            int maxZ = Math.max(zStart, zEnd);
            points.add(new int[] { minX, minZ });
            points.add(new int[] { minX, maxZ });
            points.add(new int[] { maxX, minZ });
            points.add(new int[] { maxX, maxZ });
        }

        Integer xMin = readIntField(packet, "xMin");
        Integer zMin = readIntField(packet, "zMin");
        Integer xMax = readIntField(packet, "xMax");
        Integer zMax = readIntField(packet, "zMax");
        if (xMin != null && zMin != null && xMax != null && zMax != null) {
            points.add(new int[] { xMin, zMin });
            points.add(new int[] { xMin, zMax });
            points.add(new int[] { xMax, zMin });
            points.add(new int[] { xMax, zMax });
        }

        expandPointsByBrushSize(points, playerUuid);

        return points;
    }

    private void expandPointsByBrushSize(@Nonnull List<int[]> points, @Nonnull UUID playerUuid) {
        if (points.isEmpty()) {
            return;
        }
        int radius = getBrushRadius(playerUuid);
        if (radius <= 0) {
            return;
        }

        List<int[]> bases = new ArrayList<>(points);
        points.clear();
        for (int[] base : bases) {
            if (base == null || base.length < 2) {
                continue;
            }
            int minX = base[0] - radius;
            int maxX = base[0] + radius;
            int minZ = base[1] - radius;
            int maxZ = base[1] + radius;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    points.add(new int[] { x, z });
                }
            }
        }
    }

    private int getBrushRadius(@Nonnull UUID playerUuid) {
        try {
            PrototypePlayerBuilderToolSettings settings = ToolOperation.getOrCreatePrototypeSettings(playerUuid);
            if (settings == null) {
                return 0;
            }
            BrushConfig brushConfig = settings.getBrushConfig();
            if (brushConfig == null) {
                return 0;
            }
            int width = Math.max(1, brushConfig.getShapeWidth());
            int thickness = Math.max(1, brushConfig.getShapeThickness());
            int diameter = Math.max(width, thickness);
            return Math.max(0, (diameter - 1) / 2);
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Nullable
    private Integer readIntField(@Nonnull Object packet, @Nonnull String fieldName) {
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

    /**
     * InvocationHandler that intercepts packet handling
     */
    private class PacketHandlerProxy implements InvocationHandler {
        private final Object original;

        public PacketHandlerProxy(Object original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Intercept handle methods for BuilderTools packets
            if (method.getName().equals("handle") && args != null && args.length > 0) {
                Object packet = args[0];

                UUID playerUuid = getPlayerUuidFromArgs(args);
                Player player = getPlayerFromArgs(args);
                if (playerUuid == null) {
                    // Fallback for handler implementations that keep a "player" field.
                    playerUuid = getPlayerUuidFromContext();
                }
                if (player == null) {
                    player = getPlayerFromContext();
                }

                if (playerUuid != null && shouldBlockPacket(packet, playerUuid, player)) {
                    // Send error message to player
                    sendErrorToPlayer(playerUuid);

                    return null; // Block the packet
                }
            }

            // Forward all other calls to the original handler
            return method.invoke(original, args);
        }

        @Nullable
        private Player getPlayerFromArgs(@Nonnull Object[] args) {
            for (Object arg : args) {
                if (arg instanceof Player) {
                    return (Player) arg;
                }
            }
            return null;
        }

        @Nullable
        private UUID getPlayerUuidFromArgs(@Nonnull Object[] args) {
            for (Object arg : args) {
                if (arg instanceof UUID) {
                    return (UUID) arg;
                }
                if (arg instanceof PlayerRef) {
                    return PlayerIdentity.uuid((PlayerRef) arg);
                }
                if (arg instanceof Player) {
                    Player player = (Player) arg;
                    Ref<EntityStore> playerEntityRef = player.getReference();
                    if (playerEntityRef != null) {
                        PlayerRef playerRef = playerEntityRef.getStore().getComponent(playerEntityRef,
                                PlayerRef.getComponentType());
                        if (playerRef != null) {
                            return PlayerIdentity.uuid(playerRef);
                        }
                    }
                }
            }
            return null;
        }

        private UUID getPlayerUuidFromContext() {
            try {
                // Try to get the current player from the packet handler
                java.lang.reflect.Field playerField = original.getClass().getDeclaredField("player");
                playerField.setAccessible(true);
                Player player = (Player) playerField
                        .get(original);
                if (player == null) {
                    return null;
                }

                Ref<EntityStore> playerEntityRef = player.getReference();
                if (playerEntityRef == null) {
                    return null;
                }

                PlayerRef playerRef = playerEntityRef.getStore().getComponent(playerEntityRef, PlayerRef.getComponentType());
                return playerRef != null ? PlayerIdentity.uuid(playerRef) : null;
            } catch (Exception e) {
                return null;
            }
        }

        @Nullable
        private Player getPlayerFromContext() {
            try {
                java.lang.reflect.Field playerField = original.getClass().getDeclaredField("player");
                playerField.setAccessible(true);
                return (Player) playerField.get(original);
            } catch (Exception e) {
                return null;
            }
        }

        private void sendErrorToPlayer(UUID playerUuid) {
            try {
                Universe universe = Universe
                        .get();
                if (universe != null) {
                    PlayerRef playerRef = universe.getPlayer(playerUuid);
                    if (playerRef != null) {
                        playerRef.sendMessage(
                                ChatUtil.error(translationManager.get("protection.no_permission_buildertools")));
                    }
                }
            } catch (Exception e) {
                // Silently fail
            }
        }
    }
}
