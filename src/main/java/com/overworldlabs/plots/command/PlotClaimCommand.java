package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
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
import com.overworldlabs.plots.model.PlotConfig;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PlotUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot claim
 * Claims the plot at the player's current location
 */
public class PlotClaimCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;

    public PlotClaimCommand(@Nonnull PlotManager plotManager) {
        super("claim", "Claim the current plot");
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

        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(),
                PlotManager.PERM_CLAIM, true)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        Vector3d pos = playerRef.getTransform().getPosition();
        PlotConfig config = plotManager.getConfig();

        if (!config.isInPlot(world.getName(), (int) pos.x, (int) pos.z)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("claim.standing_on_road")));
            return;
        }

        int[] grid = config.getPlotGridAt((int) pos.x, (int) pos.z);
        int gridX = grid[0];
        int gridZ = grid[1];

        if (plotManager.claimPlot(playerRef.getUuid(), playerRef.getUsername(), gridX, gridZ)) {
            playerRef.sendMessage(
                    ChatUtil.success(
                            tm.get("claim.plot_claimed", "location", PlotUtil.formatPlotLocation(gridX, gridZ))));

            // Update radar marker
            Plot plot = plotManager.getPlot(gridX, gridZ);
            if (plot != null) {
                Plots.getInstance().getRadarManager().updatePlotMarker(plot);
            }
        } else {
            Plot existingPlot = plotManager.getPlot(gridX, gridZ);
            if (existingPlot != null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("claim.plot_already_claimed")));
            } else {
                playerRef.sendMessage(ChatUtil.error(tm.get("claim.max_plots_reached")));
            }
        }
    }
}
