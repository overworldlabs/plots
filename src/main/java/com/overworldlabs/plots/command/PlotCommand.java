package com.overworldlabs.plots.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.overworldlabs.plots.manager.PlotManager;

import javax.annotation.Nonnull;

/**
 * Main /plot command collection - handles subcommands like /plot claim, /plot
 * info, etc.
 */
public class PlotCommand extends AbstractCommandCollection {

    public PlotCommand(@Nonnull PlotManager plotManager) {
        super("plot", "Plot management commands");

        // Add all subcommands
        addSubCommand(new PlotClaimCommand(plotManager));
        addSubCommand(new PlotAutoCommand(plotManager));
        addSubCommand(new PlotInfoCommand(plotManager));
        addSubCommand(new PlotDeleteCommand(plotManager));
        addSubCommand(new PlotListCommand(plotManager));
        addSubCommand(new PlotSpawnCommand(plotManager));
        addSubCommand(new PlotRenameCommand(plotManager));
        addSubCommand(new PlotTrustCommand(plotManager));
        addSubCommand(new PlotUntrustCommand(plotManager));
        addSubCommand(new PlotTpCommand(plotManager));
    }
}
