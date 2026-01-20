package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
public class PlotDeleteCommand extends CommandBase {
    private final PlotManager plotManager;
    private final OptionalArg<Integer> gridXArg;
    private final OptionalArg<Integer> gridZArg;

    public PlotDeleteCommand(@Nonnull PlotManager plotManager) {
        super("delete", "Delete your plot or any plot with coordinates");
        this.plotManager = plotManager;
        this.gridXArg = (OptionalArg<Integer>) withOptionalArg("gridX", "Grid X coordinate", ArgTypes.INTEGER);
        this.gridZArg = (OptionalArg<Integer>) withOptionalArg("gridZ", "Grid Z coordinate", ArgTypes.INTEGER);
        requirePermission(PlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!context.sender().hasPermission(PlotManager.PERM_ADMIN)) {
            CommandUtil.requirePermission(context.sender(), PlotManager.PERM_DELETE);
        }

        if (!context.isPlayer()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.only_players")));
            return;
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null)
            return;

        // Get the player object from Universe (thread-safe) to find their world
        java.util.UUID senderUuid = context.sender().getUuid();
        if (senderUuid == null)
            return;

        PlayerRef playerObj = com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(senderUuid);
        if (playerObj == null)
            return;

        java.util.UUID worldUuid = playerObj.getWorldUuid();
        if (worldUuid == null)
            return;

        World currentWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
        if (currentWorld == null)
            return;

        // Execute store operations on the world thread
        currentWorld.execute(() -> {
            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null)
                return;

            Vector3d pos = playerRef.getTransform().getPosition();
            PlotConfig config = this.plotManager.getConfig();

            // Check if coordinates were provided (requires plots.delete.* permission)
            Integer providedGridX = gridXArg.get(context);
            Integer providedGridZ = gridZArg.get(context);

            int[] grid;
            boolean isDeletingOtherPlot = false;

            if (providedGridX != null && providedGridZ != null) {
                // Deleting a specific plot by coordinates
                CommandUtil.requirePermission(context.sender(), PlotManager.PERM_DELETE_ANY);
                grid = new int[] { providedGridX, providedGridZ };
                isDeletingOtherPlot = true;
            } else {
                // Deleting the plot at current location
                grid = config.getPlotGridAt((int) pos.x, (int) pos.z);
            }

            Plot plot = this.plotManager.getPlot(grid[0], grid[1]);

            if (plot == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            // Ownership check (admins and plots.delete.* bypass)
            if (!isDeletingOtherPlot && !plot.getOwner().equals(playerRef.getUuid())) {
                if (!context.sender().hasPermission(PlotManager.PERM_ADMIN)) {
                    CommandUtil.requirePermission(context.sender(), PlotManager.PERM_DELETE_ANY);
                }
            }

            if (this.plotManager.unclaimPlot(grid[0], grid[1])) {
                playerRef.sendMessage(
                        ChatUtil.success(
                                tm.get("delete.success", "location", PlotUtil.formatPlotLocation(grid[0], grid[1]))));

                // Remove radar marker
                Plots.getInstance().getRadarManager().removePlotMarker(plot);
                Plots.getInstance().getHologramManager().removeHologram(plot, store);
            } else {
                playerRef.sendMessage(ChatUtil.error(tm.get("general.error_generic")));
            }
        });
    }
}
