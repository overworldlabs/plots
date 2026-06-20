package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.integration.mixin.MixinBridgeStatus;
import com.overworldlabs.plots.integration.mixin.PlotsMixinsCompatibility;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public class PlotDebugCommand extends CommandBase {

    public PlotDebugCommand() {
        super("debug", "Debug tools for bridge/mixins state");
        setAllowsExtraArguments(true);
        requirePermission(IPlotManager.PERM_BASE);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();
        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        String input = context.getInputString();
        String target = resolveTarget(input);
        if (!"bridge".equalsIgnoreCase(target)) {
            context.sender().sendMessage(ChatUtil.info("Usage: /plot debug bridge"));
            return;
        }

        boolean bridgeActive = MixinBridgeStatus.isActive();
        boolean mixinsLoaded = MixinBridgeStatus.isMixinsLoaded();
        boolean bootstrapReady = MixinBridgeStatus.isBootstrapReady();
        boolean ready = MixinBridgeStatus.isReadyForMixinFlags();

        Object rawRegistry = System.getProperties().get(PlotsMixinsCompatibility.REGISTRY_KEY);
        boolean registryPresent = rawRegistry instanceof Map<?, ?>;
        int hookCount = registryPresent ? ((Map<?, ?>) rawRegistry).size() : 0;

        context.sender().sendMessage(ChatUtil.info("Bridge Active: " + bridgeActive));
        context.sender().sendMessage(ChatUtil.info("Mixins Loaded: " + mixinsLoaded));
        context.sender().sendMessage(ChatUtil.info("Bootstrap Ready: " + bootstrapReady));
        context.sender().sendMessage(ChatUtil.info("Ready For Mixin Flags: " + ready));
        context.sender().sendMessage(ChatUtil.info("Hook Registry Present: " + registryPresent));
        context.sender().sendMessage(ChatUtil.info("Registered Hooks: " + hookCount));
        List<String> loadedMixins = MixinBridgeStatus.getLoadedMixins();
        context.sender().sendMessage(ChatUtil.info("Applied Mixins: " + loadedMixins.size()));
        if (!loadedMixins.isEmpty()) {
            context.sender().sendMessage(ChatUtil.info("Mixin List: " + String.join(", ", loadedMixins)));
        }
        if (bridgeActive && bootstrapReady && !mixinsLoaded) {
            context.sender().sendMessage(ChatUtil.colorize(
                    "{#ffaa00}[Plots] {#ffffff}Bridge is ready, but no mixins were applied."));
            context.sender().sendMessage(ChatUtil.colorize(
                    "{#ffaa00}[Plots] {#ffffff}Check TaleGuard version compatibility with this server build."));
        }
    }

    private String resolveTarget(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = input.startsWith("/") ? input.substring(1) : input;
        String[] parts = normalized.trim().split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if ("debug".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return "";
    }
}
