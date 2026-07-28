package dev.stoshe.plots.command.sub;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.manager.PlotManager;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

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
