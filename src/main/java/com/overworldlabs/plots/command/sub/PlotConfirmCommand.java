package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.command.PlotConfirmationService;
import com.overworldlabs.plots.manager.TranslationManager;

import javax.annotation.Nonnull;

public class PlotConfirmCommand extends CommandBase {
    public PlotConfirmCommand() {
        super("confirm", "Confirm a pending plot action");
        requirePermission(IPlotManager.PERM_BASE);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();
        PlotConfirmationService.getInstance().confirm(context.sender(), tm);
    }
}
