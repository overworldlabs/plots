package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PlotUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot auto
 * Finds an available plot, claims it for the player, and teleports them there.
 */
public class PlotAutoCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;

    public PlotAutoCommand(@Nonnull PlotManager plotManager) {
        super("auto", "Automatically claim a plot nearby");
        this.plotManager = plotManager;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        TranslationManager tm = Plots.getInstance().getTranslationManager();

        // Permission check
        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(),
                PlotManager.PERM_CLAIM, true)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        // Find next available plot
        int[] freePlot = plotManager.findNextFreePlot();
        if (freePlot == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found"))); // Reuse plot_not_found or add new
            return;
        }

        int gridX = freePlot[0];
        int gridZ = freePlot[1];

        // Attempt to claim
        if (plotManager.claimPlot(playerRef.getUuid(), playerRef.getUsername(), gridX, gridZ)) {
            playerRef.sendMessage(
                    ChatUtil.success(
                            tm.get("claim.plot_auto_claimed", "location", PlotUtil.formatPlotLocation(gridX, gridZ))));

            // Update radar marker
            Plot plot = plotManager.getPlot(gridX, gridZ);
            if (plot != null) {
                Plots.getInstance().getRadarManager().updatePlotMarker(plot);

                // Use centralized teleport method
                plotManager.teleportPlayerToPlot(store, ref, plot);
                playerRef.sendMessage(ChatUtil.success(tm.get("teleport.teleporting")));
            }
        } else {
            playerRef.sendMessage(ChatUtil.error(tm.get("claim.max_plots_reached")));
        }
    }
}
