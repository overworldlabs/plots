package com.overworldlabs.plots.integration.buildertools;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.tooloperations.OperationFactory;
import com.hypixel.hytale.builtin.buildertools.tooloperations.ToolOperation;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles integration with BuilderTools using reflection to intercept
 * operations.
 */
public class BuilderToolsIntegration {

    private final Map<String, OperationFactory> originalFactories = new ConcurrentHashMap<>();
    private boolean initialized = false;

    public void initialize() {
        if (initialized)
            return;
        try {
            hookBrushes();
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hookBrushes() {
        Map<String, OperationFactory> operations = ToolOperation.OPERATIONS;

        // Hook ALL tools registered in ToolOperation.OPERATIONS
        for (String toolName : operations.keySet()) {
            OperationFactory original = operations.get(toolName);
            if (original != null) {
                originalFactories.put(toolName, original);
                operations.put(toolName, createProtectedFactory(toolName, original));
            }
        }
    }

    private OperationFactory createProtectedFactory(String toolName, OperationFactory original) {
        return (ref, player, packet, accessor) -> {
            ToolOperation operation = original.create(ref, player, packet, accessor);
            if (operation != null) {
                injectProtection(operation, player.getUuid(), toolName);
            }
            return operation;
        };
    }

    private void injectProtection(ToolOperation operation, UUID playerUuid, String toolName) {
        try {
            PlotProtectionMask protectedMask = new PlotProtectionMask(playerUuid, null);
            injectMaskIntoObject(operation, protectedMask, new HashSet<>(), 0);
        } catch (Exception e) {
            // Silently fail - protection not critical for plugin operation
        }
    }

    /**
     * Recursively inject mask into all BlockMask fields in an object and its nested
     * objects
     */
    private int injectMaskIntoObject(Object obj, PlotProtectionMask mask, Set<Object> visited, int depth) {
        if (obj == null || depth > 3 || visited.contains(obj)) {
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

                    // Check if this field is a BlockMask
                    if (value != null
                            && value instanceof com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask) {
                        if (!(value instanceof PlotProtectionMask)) {
                            // Wrap the existing mask
                            PlotProtectionMask wrappedMask = new PlotProtectionMask(mask.getPlayerUuid(),
                                    (com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask) value);
                            field.set(obj, wrappedMask);
                            count++;
                        }
                    }
                    // Recursively check nested objects
                    else if (value != null && !isPrimitiveOrWrapper(value.getClass())
                            && !value.getClass().getName().startsWith("java.lang.String")) {
                        count += injectMaskIntoObject(value, mask, visited, depth + 1);
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
            Player player = playerRef.getStore().getComponent(playerRef, Player.getComponentType());
            PlayerRef pr = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
            if (player == null || pr == null)
                return;

            BuilderToolsPlugin.BuilderState state = BuilderToolsPlugin.get().getBuilderState(player, pr);
            if (state == null)
                return;

            // Apply global mask (for tools that might check it, though Extrude doesn't)
            com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask current = state.getGlobalMask();
            if (!(current instanceof PlotProtectionMask)) {
                state.setGlobalMask(new PlotProtectionMask(pr.getUuid(), current), accessor);
            }
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
        if (!initialized)
            return;
        Map<String, OperationFactory> operations = ToolOperation.OPERATIONS;
        originalFactories.forEach(operations::put);
        originalFactories.clear();
        initialized = false;
    }
}
