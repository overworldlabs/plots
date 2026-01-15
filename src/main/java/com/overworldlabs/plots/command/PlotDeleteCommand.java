package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
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
 * Command: /plot delete
 * Deletes/unclaims the current plot
 */
public class PlotDeleteCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;
    private final OptionalArg<Integer> gridXArg;
    private final OptionalArg<Integer> gridZArg;

    public PlotDeleteCommand(@Nonnull PlotManager plotManager) {
        super("delete", "Delete your plot or any plot with coordinates");
        this.plotManager = plotManager;
        this.gridXArg = withOptionalArg("gridX", "Grid X coordinate", ArgTypes.INTEGER);
        this.gridZArg = withOptionalArg("gridZ", "Grid Z coordinate", ArgTypes.INTEGER);
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
                PlotManager.PERM_DELETE, true)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        Vector3d pos = playerRef.getTransform().getPosition();
        PlotConfig config = plotManager.getConfig();

        // Check if coordinates were provided (requires plots.delete.* permission)
        Integer providedGridX = gridXArg.get(context);
        Integer providedGridZ = gridZArg.get(context);

        int[] grid;
        boolean isDeletingOtherPlot = false;

        if (providedGridX != null && providedGridZ != null) {
            // Deleting a specific plot by coordinates
            if (!PermissionsModule.get().hasPermission(playerRef.getUuid(),
                    PlotManager.PERM_DELETE_ANY)) {
                playerRef.sendMessage(ChatUtil.error(tm.get("delete.need_permission")));
                return;
            }
            grid = new int[] { providedGridX, providedGridZ };
            isDeletingOtherPlot = true;
        } else {
            // Deleting the plot at current location
            grid = config.getPlotGridAt((int) pos.x, (int) pos.z);
        }

        Plot plot = plotManager.getPlot(grid[0], grid[1]);

        if (plot == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
            return;
        }

        // Ownership check (admins and plots.delete.* bypass)
        if (!isDeletingOtherPlot && !plot.getOwner().equals(playerRef.getUuid()) &&
                !PermissionsModule.get().hasPermission(playerRef.getUuid(),
                        PlotManager.PERM_ADMIN)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.not_owner")));
            return;
        }

        if (plotManager.unclaimPlot(grid[0], grid[1])) {
            playerRef.sendMessage(
                    ChatUtil.success(
                            tm.get("delete.success", "location", PlotUtil.formatPlotLocation(grid[0], grid[1]))));

            // Remove radar marker
            Plots.getInstance().getRadarManager().removePlotMarker(plot);
        } else {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.error_generic")));
        }
    }
}
