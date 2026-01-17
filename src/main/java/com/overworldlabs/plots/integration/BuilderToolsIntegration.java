package com.overworldlabs.plots.integration;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.tooloperations.OperationFactory;
import com.hypixel.hytale.builtin.buildertools.tooloperations.ToolOperation;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.util.ConsoleColors;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles integration with BuilderTools, including direct action protection
 * (Extrude, Paste, etc).
 */
public class BuilderToolsIntegration {
    private static final List<String> PROTECTED_TOOLS = Arrays.asList(
            "Paint", "Sculpt", "Flood", "Noise", "Scatter", "Smooth", "Tint", "Layers", "Paste",
            "Extrude", "Selection", "LaserPointer", "Entity", "Line", "Liner", "Box", "Sphere",
            "Cylinder", "Erode", "Overlay", "Replace", "Set", "Undo", "Redo", "Stack");

    private final Map<String, OperationFactory> originalFactories = new ConcurrentHashMap<>();
    private boolean initialized = false;

    private static Field toolOperationMaskField;
    private static Field toolOperationEditField;
    private static Field editOperationMaskField;

    public void initialize() {
        if (initialized)
            return;
        try {
            initReflection();
            hookBrushes();
            initialized = true;
            ConsoleColors.success("BuilderTools Direct-Action integration initialized.");
        } catch (Exception e) {
            ConsoleColors.error("Failed to initialize BuilderTools integration: " + e.getMessage());
        }
    }

    private void initReflection() {
        toolOperationMaskField = findField(ToolOperation.class, "mask");
        if (toolOperationMaskField != null)
            toolOperationMaskField.setAccessible(true);

        toolOperationEditField = findField(ToolOperation.class, "edit");
        if (toolOperationEditField != null)
            toolOperationEditField.setAccessible(true);

        try {
            Class<?> editOpClass = Class.forName("com.hypixel.hytale.builtin.buildertools.EditOperation");
            editOperationMaskField = findField(editOpClass, "blockMask");
            if (editOperationMaskField != null)
                editOperationMaskField.setAccessible(true);
        } catch (Exception e) {
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private void hookBrushes() {
        Map<String, OperationFactory> operations = ToolOperation.OPERATIONS;

        // Log para diagnóstico (ver no console qual o nome exato do Extrude)
        System.out.println("[Plots-Debug] List of server-registered operations:");
        operations.keySet().forEach(k -> System.out.println(" - " + k));

        for (String toolName : PROTECTED_TOOLS) {
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
                injectProtection(operation, player.getUuid());
            }
            return operation;
        };
    }

    private void injectProtection(ToolOperation operation, UUID playerUuid) {
        try {
            PlotBlockMask protectedMask = null;
            if (toolOperationMaskField != null) {
                com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask current = (com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask) toolOperationMaskField
                        .get(operation);
                protectedMask = new PlotBlockMask(playerUuid, current);
                toolOperationMaskField.set(operation, protectedMask);
            }

            if (toolOperationEditField != null && editOperationMaskField != null) {
                Object edit = toolOperationEditField.get(operation);
                if (edit != null) {
                    if (protectedMask == null) {
                        protectedMask = new PlotBlockMask(playerUuid,
                                (com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask) editOperationMaskField
                                        .get(edit));
                    }
                    editOperationMaskField.set(edit, protectedMask);
                }
            }
        } catch (Exception e) {
        }
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

            // FORÇA A MÁSCARA GLOBAL: ISSO DEVE PEGAR FERRAMENTAS QUE NÃO USAM
            // TOOLOPERATION
            com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask current = state.getGlobalMask();
            if (!(current instanceof PlotBlockMask)) {
                System.out.println("[Plots-Debug] Forcing global protection for: " + player.getDisplayName());
                state.setGlobalMask(new PlotBlockMask(pr.getUuid(), current), accessor);
            }
        } catch (Exception e) {
        }
    }

    public void clearMask(Ref<EntityStore> playerRef, ComponentAccessor<EntityStore> accessor) {
        try {
            Player player = playerRef.getStore().getComponent(playerRef, Player.getComponentType());
            PlayerRef pr = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
            if (player == null || pr == null)
                return;

            BuilderToolsPlugin.BuilderState state = BuilderToolsPlugin.get().getBuilderState(player, pr);
            if (state != null && state.getGlobalMask() instanceof PlotBlockMask) {
                state.setGlobalMask(null, accessor);
            }
        } catch (Exception e) {
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
