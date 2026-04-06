package com.overworldlabs.plots.command.sub;

import com.overworldlabs.plots.util.CommandSenderIdentity;

import com.overworldlabs.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.universe.Universe;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.integration.economy.PlotEconomyService;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;

public class PlotAutoCommand extends CommandBase {
    private final IPlotManager plotManager;

    public PlotAutoCommand(@Nonnull IPlotManager plotManager) {
        super("auto", "Automatically claim a plot nearby");
        this.plotManager = plotManager;
        requirePermission(IPlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

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

            // Find next available plot
            int[] freePlot = this.plotManager.findNextFreePlot();
            if (freePlot == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            int gridX = freePlot[0];
            int gridZ = freePlot[1];

            double autoClaimCost = this.plotManager.getConfig().getEconomyCostAutoClaim();
            boolean economyBypass = PermissionUtil.hasEconomyBypass(context.sender());
            if (!economyBypass && !chargeIfNeeded(playerRef, autoClaimCost, tm.get("economy.reason.auto_claim"), tm)) {
                return;
            }

            // Attempt to claim
            if (this.plotManager.claimPlot(context.sender(), playerRef, freePlot[0], freePlot[1])) {
                Plot plot = this.plotManager.getPlot(gridX, gridZ);
                String plotName = plot != null ? plot.getName() : "Plot";
                playerRef.sendMessage(
                        ChatUtil.success(tm.get("claim.plot_auto_claimed",
                                "name", plotName,
                                "plot_name", plotName,
                                "x", String.valueOf(gridX),
                                "y", String.valueOf((int) playerRef.getTransform().getPosition().y),
                                "z", String.valueOf(gridZ))));

                // Update radar marker
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

    private boolean chargeIfNeeded(PlayerRef playerRef, double amount, String reason, TranslationManager tm) {
        if (amount <= 0.0) {
            return true;
        }

        PlotEconomyService economy = Plots.getInstance().getEconomyService();
        if (economy == null || !economy.isEnabled()) {
            playerRef.sendMessage(ChatUtil.error(tm.get("economy.not_available")));
            return false;
        }

        if (!economy.has(PlayerIdentity.uuid(playerRef), amount)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("economy.insufficient_funds", "amount", economy.format(amount))));
            return false;
        }

        if (!economy.tryWithdraw(PlayerIdentity.uuid(playerRef), amount, reason)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("economy.withdraw_failed")));
            return false;
        }

        playerRef.sendMessage(ChatUtil.info(tm.get("economy.charged", "amount", economy.format(amount))));
        return true;
    }
}
