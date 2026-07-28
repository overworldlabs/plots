package dev.stoshe.plots.command.sub;

import dev.stoshe.plots.util.CommandSenderIdentity;

import dev.stoshe.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.universe.Universe;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.integration.economy.PlotEconomyService;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

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

            // Auto-claim in the player's current plot world, or the default plot
            // world when they are standing somewhere unmanaged.
            PlotConfig config = this.plotManager.getConfig();
            String worldName = config.isManagedWorld(currentWorld.getName())
                    ? currentWorld.getName()
                    : config.getDefaultWorldName();

            // Pre-check the limit BEFORE searching/charging so players are never billed
            // for an auto-claim that cannot proceed.
            if (!this.plotManager.canClaimIn(context.sender(), PlayerIdentity.uuid(playerRef), worldName)) {
                playerRef.sendMessage(ChatUtil.error(tm.get("claim.max_plots_reached")));
                return;
            }

            int[] freePlot = this.plotManager.findNextFreePlot(worldName);
            if (freePlot == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            int gridX = freePlot[0];
            int gridZ = freePlot[1];

            double autoClaimCost = config.getEconomyCostAutoClaim();
            boolean economyBypass = PermissionUtil.hasEconomyBypass(context.sender());
            if (!economyBypass && !chargeIfNeeded(playerRef, autoClaimCost, tm.get("economy.reason.auto_claim"), tm)) {
                return;
            }

            // Attempt to claim
            if (this.plotManager.claimPlot(context.sender(), playerRef, worldName, gridX, gridZ)) {
                Plot plot = this.plotManager.getPlot(worldName, gridX, gridZ);
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
