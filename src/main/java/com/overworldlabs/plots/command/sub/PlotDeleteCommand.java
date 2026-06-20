package com.overworldlabs.plots.command.sub;

import com.overworldlabs.plots.util.CommandSenderIdentity;

import com.overworldlabs.plots.util.PlayerIdentity;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.command.CommandArgs;
import com.overworldlabs.plots.command.feedback.CommandFeedbackService;
import com.overworldlabs.plots.command.PlotConfirmationService;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;
import com.overworldlabs.plots.manager.PlotManager;

import javax.annotation.Nonnull;

/**
 * Command: /plot delete
 * Deletes/unclaims the current plot
 */
public class PlotDeleteCommand extends CommandBase {
    private final IPlotManager plotManager;
    private final OptionalArg<Integer> gridXArg;
    private final OptionalArg<Integer> gridZArg;

    public PlotDeleteCommand(@Nonnull IPlotManager plotManager) {
        super("delete", "Delete your plot or any plot with coordinates");
        setAllowsExtraArguments(true);
        this.plotManager = plotManager;
        this.gridXArg = CommandArgs.optional(this, "gridX", "Grid X coordinate", ArgTypes.INTEGER);
        this.gridZArg = CommandArgs.optional(this, "gridZ", "Grid Z coordinate", ArgTypes.INTEGER);
        requirePermission(IPlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(),
                    IPlotManager.PERM_DELETE);
        }

        if (!context.isPlayer()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.only_players")));
            return;
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null)
            return;

        // Get the player object from Universe (thread-safe) to find their world
        java.util.UUID senderUuid = CommandSenderIdentity.uuid(context.sender());
        if (senderUuid == null)
            return;

        PlayerRef playerObj = Universe.get().getPlayer(senderUuid);
        if (playerObj == null)
            return;

        java.util.UUID worldUuid = playerObj.getWorldUuid();
        if (worldUuid == null)
            return;

        World currentWorld = Universe.get().getWorld(worldUuid);
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
            if ((providedGridX == null) != (providedGridZ == null)) {
                CommandFeedbackService.sendUsage(playerRef, tm, "delete.usage");
                return;
            }

            int[] grid;
            boolean isDeletingOtherPlot = false;

            if (providedGridX != null && providedGridZ != null) {
                // Deleting a specific plot by coordinates
                CommandUtil.requirePermission(context.sender(), IPlotManager.PERM_DELETE_ANY);
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
            if (!isDeletingOtherPlot && !plot.getOwner().equals(PlayerIdentity.uuid(playerRef))) {
                if (!PermissionUtil.hasAdminPermission(context.sender())) {
                    CommandUtil.requirePermission(context.sender(),
                            IPlotManager.PERM_DELETE_ANY);
                }
            }

            final int targetGridX = grid[0];
            final int targetGridZ = grid[1];
            final String targetPlotName = plot.getName() != null ? plot.getName() : "Plot";
            final String targetY = String.valueOf((int) pos.y);

            PlotConfirmationService.getInstance().request(context.sender(), tm,
                    tm.get("confirm.action_delete",
                            "name", targetPlotName,
                            "plot_name", targetPlotName,
                            "x", String.valueOf(targetGridX),
                            "y", targetY,
                            "z", String.valueOf(targetGridZ)),
                    () -> currentWorld.execute(() -> {
                        Plot currentPlot = this.plotManager.getPlot(targetGridX, targetGridZ);
                        if (currentPlot == null) {
                            playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                            return;
                        }

                        if (!currentPlot.getOwner().equals(PlayerIdentity.uuid(playerRef))
                                && !PermissionUtil.hasAdminPermission(context.sender())
                                && !context.sender().hasPermission(IPlotManager.PERM_DELETE_ANY)) {
                            playerRef.sendMessage(ChatUtil.error(tm.get("management.no_permission")));
                            return;
                        }

                        boolean deleted;
                        if (this.plotManager instanceof PlotManager pm) {
                            deleted = pm.deletePlotAndRegenerate(currentWorld, targetGridX, targetGridZ);
                        } else {
                            deleted = this.plotManager.unclaimPlot(targetGridX, targetGridZ);
                        }

                        if (deleted) {
                            String deletedName = currentPlot.getName() != null ? currentPlot.getName() : "Plot";
                            playerRef.sendMessage(ChatUtil.success(
                                    tm.get("delete.success",
                                            "name", deletedName,
                                            "plot_name", deletedName,
                                            "x", String.valueOf(targetGridX),
                                            "y", targetY,
                                            "z", String.valueOf(targetGridZ))));

                            Plots.getInstance().getRadarManager().removePlotMarker(currentPlot);
                            Plots.getInstance().getHologramManager().removeHologram(currentPlot, store);
                        } else {
                            playerRef.sendMessage(ChatUtil.error(tm.get("general.error_generic")));
                        }
                    }));
        });
    }
}
