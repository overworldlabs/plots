package dev.stoshe.plots.command.sub;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.command.PlotConfirmationService;
import dev.stoshe.plots.manager.TranslationManager;

import javax.annotation.Nonnull;

public class PlotCancelCommand extends CommandBase {
    public PlotCancelCommand() {
        super("cancel", "Cancel a pending plot action");
        requirePermission(IPlotManager.PERM_BASE);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();
        PlotConfirmationService.getInstance().cancel(context.sender(), tm);
    }
}
