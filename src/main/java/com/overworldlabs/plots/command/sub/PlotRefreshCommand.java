package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot refresh
 * Reloads runtime config/translations and refreshes plot cache/markers.
 */
public class PlotRefreshCommand extends CommandBase {
    private final IPlotManager plotManager;

    public PlotRefreshCommand(@Nonnull IPlotManager plotManager) {
        super("refresh", "Reload plots runtime config and caches");
        this.plotManager = plotManager;
        requirePermission(IPlotManager.PERM_BASE);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();
        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        try {
            Plots plugin = Plots.getInstance();
            PlotConfig diskConfig = plugin.getConfig();
            PlotConfig liveConfig = plotManager.getConfig();
            liveConfig.overwriteWith(diskConfig);

            if (plotManager instanceof PlotManager pm) {
                pm.syncConfigWithPrefabs();
                pm.loadPlots();
            }

            tm.reload(liveConfig.getLanguage());
            plugin.getRadarManager().refreshAllPlotMarkers();

            context.sender().sendMessage(ChatUtil.success(tm.get("refresh.success")));
        } catch (Exception e) {
            context.sender().sendMessage(ChatUtil.error(tm.get("refresh.failed", "error", e.getClass().getSimpleName())));
        }
    }
}
