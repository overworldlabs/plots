package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
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
 * Command: /plot claim
 * Claims the plot at the player's current location
 */
public class PlotClaimCommand extends CommandBase {
    private final PlotManager plotManager;

    public PlotClaimCommand(@Nonnull PlotManager plotManager) {
        super("claim", "Claim the current plot");
        this.plotManager = plotManager;
        requirePermission(PlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!context.sender().hasPermission(PlotManager.PERM_ADMIN)) {
            CommandUtil.requirePermission(context.sender(), PlotManager.PERM_CLAIM);
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

            if (!config.isInPlot(currentWorld.getName(), (int) pos.x, (int) pos.z)) {
                playerRef.sendMessage(ChatUtil.error(tm.get("claim.standing_on_road")));
                return;
            }

            int[] grid = config.getPlotGridAt((int) pos.x, (int) pos.z);
            int gridX = grid[0];
            int gridZ = grid[1];

            if (this.plotManager.claimPlot(context.sender(), playerRef, gridX, gridZ)) {
                playerRef.sendMessage(
                        ChatUtil.success(
                                tm.get("claim.plot_claimed", "location", PlotUtil.formatPlotLocation(gridX, gridZ))));

                // Update radar marker
                Plot plot = this.plotManager.getPlot(gridX, gridZ);
                if (plot != null) {
                    Plots.getInstance().getRadarManager().updatePlotMarker(plot);
                    Plots.getInstance().getHologramManager().updateHologram(plot, store);
                }
            } else {
                Plot existingPlot = this.plotManager.getPlot(gridX, gridZ);
                if (existingPlot != null) {
                    playerRef.sendMessage(ChatUtil.error(tm.get("claim.plot_already_claimed")));
                } else {
                    playerRef.sendMessage(ChatUtil.error(tm.get("claim.max_plots_reached")));
                }
            }
        });
    }
}
