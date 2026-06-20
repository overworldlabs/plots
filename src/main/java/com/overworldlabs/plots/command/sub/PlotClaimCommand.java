package com.overworldlabs.plots.command.sub;

import com.overworldlabs.plots.util.CommandSenderIdentity;

import com.overworldlabs.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.universe.Universe;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.integration.economy.PlotEconomyService;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot claim
 * Claims the plot at the player's current location
 */
public class PlotClaimCommand extends CommandBase {
    private final IPlotManager plotManager;

    public PlotClaimCommand(@Nonnull IPlotManager plotManager) {
        super("claim", "Claim the current plot");
        this.plotManager = plotManager;
        requirePermission(IPlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(),
                    IPlotManager.PERM_CLAIM);
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

            if (!config.isInPlot(currentWorld.getName(), (int) pos.x, (int) pos.z)) {
                playerRef.sendMessage(ChatUtil.error(tm.get("claim.standing_on_road")));
                return;
            }

            int[] grid = config.getPlotGridAt((int) pos.x, (int) pos.z);
            int gridX = grid[0];
            int gridZ = grid[1];

            double claimCost = config.getEconomyCostClaim();
            boolean economyBypass = PermissionUtil.hasEconomyBypass(context.sender());
            if (!economyBypass && !chargeIfNeeded(playerRef, claimCost, tm.get("economy.reason.claim"), tm)) {
                return;
            }

            if (this.plotManager.claimPlot(context.sender(), playerRef, gridX, gridZ)) {
                Plot claimedPlot = this.plotManager.getPlot(gridX, gridZ);
                String plotName = claimedPlot != null ? claimedPlot.getName() : "Plot";
                playerRef.sendMessage(
                        ChatUtil.success(tm.get("claim.plot_claimed",
                                "name", plotName,
                                "plot_name", plotName,
                                "x", String.valueOf(gridX),
                                "y", String.valueOf((int) pos.y),
                                "z", String.valueOf(gridZ))));

                // Update radar marker
                if (claimedPlot != null) {
                    Plots.getInstance().getRadarManager().updatePlotMarker(claimedPlot);
                    Plots.getInstance().getHologramManager().updateHologram(claimedPlot, store);
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
