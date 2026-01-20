package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PlotUtil;

import javax.annotation.Nonnull;

public class PlotAutoCommand extends CommandBase {
    private final PlotManager plotManager;

    public PlotAutoCommand(@Nonnull PlotManager plotManager) {
        super("auto", "Automatically claim a plot nearby");
        this.plotManager = plotManager;
        requirePermission(PlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!context.isPlayer()) {
            context.sender().sendMessage(com.overworldlabs.plots.util.ChatUtil.error(tm.get("general.only_players")));
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

            // Find next available plot
            int[] freePlot = this.plotManager.findNextFreePlot();
            if (freePlot == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            int gridX = freePlot[0];
            int gridZ = freePlot[1];

            // Attempt to claim
            if (this.plotManager.claimPlot(context.sender(), playerRef, freePlot[0], freePlot[1])) {
                playerRef.sendMessage(
                        ChatUtil.success(
                                tm.get("claim.plot_auto_claimed", "location",
                                        PlotUtil.formatPlotLocation(gridX, gridZ))));

                // Update radar marker
                Plot plot = this.plotManager.getPlot(gridX, gridZ);
                if (plot != null) {
                    Plots.getInstance().getRadarManager().updatePlotMarker(plot);
                    Plots.getInstance().getHologramManager().updateHologram(plot, store);

                    // Use centralized teleport method
                    this.plotManager.teleportPlayerToPlot(store, ref, plot);
                    playerRef.sendMessage(ChatUtil.success(tm.get("teleport.teleporting")));
                }
            } else {
                playerRef.sendMessage(ChatUtil.error(tm.get("claim.max_plots_reached")));
            }
        });
    }
}
