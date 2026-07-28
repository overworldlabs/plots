package dev.stoshe.plots.integration.buildertools;

import com.hypixel.hytale.builtin.buildertools.PrototypePlayerBuilderToolSettings;
import com.hypixel.hytale.builtin.buildertools.scriptedbrushes.BrushConfig;
import com.hypixel.hytale.builtin.buildertools.scriptedbrushes.BrushConfigCommandExecutor;
import com.hypixel.hytale.builtin.buildertools.tooloperations.ToolOperation;
import com.hypixel.hytale.server.core.entity.entities.Player;
import dev.stoshe.plots.util.Console;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolates reflection-heavy scripted brush hardening.
 * <p>
 * BuilderToolsIntegration delegates here to keep operation hook logic separate
 * from settings/brush instrumentation concerns.
 */
final class BuilderToolsScriptedBrushGuard {
    private final Set<UUID> protectedBrushPlayers = ConcurrentHashMap.newKeySet();
    private volatile Field brushConfigField;
    private volatile Field brushConfigExecutorField;
    private volatile boolean brushFieldsInitialized = false;
    private volatile boolean scriptedBrushProtectionUnavailable = false;

    /**
     * Ensures the player's scripted brush config is wrapped with plot protection.
     */
    void ensureProtectedScriptedBrushConfig(Player player, UUID playerUuid) {
        if (scriptedBrushProtectionUnavailable
                || player == null
                || playerUuid == null
                || protectedBrushPlayers.contains(playerUuid)) {
            return;
        }
        try {
            initializeBrushFields();
            if (brushConfigField == null || brushConfigExecutorField == null) {
                return;
            }

            PrototypePlayerBuilderToolSettings settings = ToolOperation.getOrCreatePrototypeSettings(playerUuid);
            if (settings == null) {
                return;
            }

            BrushConfig current = (BrushConfig) brushConfigField.get(settings);
            if (!(current instanceof ProtectedBrushConfig)) {
                ProtectedBrushConfig wrapped = current != null
                        ? new ProtectedBrushConfig(player, playerUuid, current)
                        : new ProtectedBrushConfig(player, playerUuid);
                brushConfigField.set(settings, wrapped);
                brushConfigExecutorField.set(settings, new BrushConfigCommandExecutor(wrapped));
            }
            protectedBrushPlayers.add(playerUuid);
        } catch (Exception e) {
            scriptedBrushProtectionUnavailable = true;
            String reason = e.getClass().getSimpleName();
            if (e.getMessage() != null && !e.getMessage().isBlank()) {
                reason += " - " + e.getMessage();
            }
            Console.warning(
                    "[BuilderToolsIntegration] Scripted brush protection disabled for this session: " + reason);
        }
    }

    /**
     * Clears runtime cache so protection can be rebuilt after reload.
     */
    void reset() {
        protectedBrushPlayers.clear();
        scriptedBrushProtectionUnavailable = false;
    }

    private synchronized void initializeBrushFields() {
        if (brushFieldsInitialized) {
            return;
        }
        try {
            brushConfigField = PrototypePlayerBuilderToolSettings.class.getDeclaredField("brushConfig");
            brushConfigField.setAccessible(true);
            brushConfigExecutorField = PrototypePlayerBuilderToolSettings.class
                    .getDeclaredField("brushConfigCommandExecutor");
            brushConfigExecutorField.setAccessible(true);
        } catch (Exception e) {
            Console.warning("[BuilderToolsIntegration] Failed to access scripted brush fields: " + e.getMessage());
        } finally {
            brushFieldsInitialized = true;
        }
    }
}
