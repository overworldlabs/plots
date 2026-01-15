package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;

public class PlotTpCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;
    private final RequiredArg<String> nameArg;

    public PlotTpCommand(@Nonnull PlotManager plotManager) {
        super("tp", "Teleport to a specific plot");
        this.plotManager = plotManager;
        this.nameArg = withRequiredArg("name", "The plot identifier", ArgTypes.STRING);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        TranslationManager tm = Plots.getInstance().getTranslationManager();
        String plotName = nameArg.get(context);

        if (plotName == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("teleport.usage")));
            return;
        }

        // Find plot
        Plot targetPlot = null;
        for (Plot plot : plotManager.getAllPlots()) {
            if (plot.getName().equalsIgnoreCase(plotName)) {
                targetPlot = plot;
                break;
            }
        }

        if (targetPlot == null) {
            // Try prefix search
            for (Plot plot : plotManager.getAllPlots()) {
                if (plot.getName().toLowerCase().startsWith(plotName.toLowerCase())) {
                    targetPlot = plot;
                    break;
                }
            }
        }

        if (targetPlot == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
            return;
        }

        // Use centralized teleport method
        plotManager.teleportPlayerToPlot(store, ref, targetPlot);
        playerRef.sendMessage(ChatUtil.success(tm.get("teleport.teleporting")));
    }
}
