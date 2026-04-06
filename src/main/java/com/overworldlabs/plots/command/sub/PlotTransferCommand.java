package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.command.CommandArgs;
import com.overworldlabs.plots.command.PlotConfirmationService;
import com.overworldlabs.plots.command.feedback.CommandFeedbackService;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.CommandSenderIdentity;
import com.overworldlabs.plots.util.PermissionUtil;
import com.overworldlabs.plots.util.PlayerIdentity;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public class PlotTransferCommand extends CommandBase {
    private final IPlotManager plotManager;
    private final RequiredArg<String> playerArg;

    public PlotTransferCommand(@Nonnull IPlotManager plotManager) {
        super("transfer", "Transfer your current plot to another player");
        setAllowsExtraArguments(true);
        this.plotManager = plotManager;
        this.playerArg = CommandArgs.required(this, "player", "Player name", ArgTypes.STRING);
        requirePermission(IPlotManager.PERM_TRANSFER);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(), IPlotManager.PERM_TRANSFER);
        }

        if (!context.isPlayer()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.only_players")));
            return;
        }

        String targetPlayerName = playerArg.get(context);
        if (targetPlayerName == null || targetPlayerName.trim().isEmpty()) {
            CommandFeedbackService.sendUsage(context.sender(), tm, "transfer.usage");
            return;
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null) {
            return;
        }

        UUID senderUuid = CommandSenderIdentity.uuid(context.sender());
        if (senderUuid == null) {
            return;
        }

        PlayerRef senderObj = Universe.get().getPlayer(senderUuid);
        if (senderObj == null || senderObj.getWorldUuid() == null) {
            return;
        }

        World currentWorld = Universe.get().getWorld(senderObj.getWorldUuid());
        if (currentWorld == null) {
            return;
        }

        currentWorld.execute(() -> {
            Store<EntityStore> store = ref.getStore();
            PlayerRef senderRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (senderRef == null) {
                return;
            }

            Vector3d pos = senderRef.getTransform().getPosition();
            Plot current = this.plotManager.getPlotAt(currentWorld.getName(), (int) pos.x, (int) pos.z);
            if (current == null) {
                senderRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            boolean isAdmin = PermissionUtil.hasAdminPermission(context.sender());
            if (!current.getOwner().equals(PlayerIdentity.uuid(senderRef)) && !isAdmin) {
                senderRef.sendMessage(ChatUtil.error(tm.get("general.not_owner")));
                return;
            }

            PlayerRef targetRef = Universe.get().getPlayerByUsername(targetPlayerName, NameMatching.EXACT);
            if (targetRef == null) {
                senderRef.sendMessage(ChatUtil.error(tm.get("trust.player_offline")));
                return;
            }

            UUID targetUuid = PlayerIdentity.uuid(targetRef);
            if (targetUuid == null) {
                senderRef.sendMessage(ChatUtil.error(tm.get("trust.player_offline")));
                return;
            }

            if (targetUuid.equals(current.getOwner())) {
                senderRef.sendMessage(ChatUtil.error(tm.get("transfer.already_owner")));
                return;
            }

            PlotManager pm = (PlotManager) this.plotManager;
            Plot canonical = pm.getCanonicalPlot(current);
            List<Plot> group = pm.getMergedComponent(canonical);
            int transferCount = group.size();

            PlotConfirmationService.getInstance().request(context.sender(), tm,
                    tm.get("confirm.action_transfer",
                            "player", targetRef.getUsername(),
                            "count", String.valueOf(transferCount),
                            "plot_name", canonical.getName()),
                    () -> currentWorld.execute(() -> {
                        Plot latest = this.plotManager.getPlot(canonical.getGridX(), canonical.getGridZ());
                        if (latest == null) {
                            senderRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                            return;
                        }

                        Plot latestCanonical = pm.getCanonicalPlot(latest);
                        List<Plot> latestGroup = pm.getMergedComponent(latestCanonical);
                        UUID actorUuid = PlayerIdentity.uuid(senderRef);
                        boolean allowed = PermissionUtil.hasAdminPermission(context.sender())
                                || actorUuid != null && actorUuid.equals(latestCanonical.getOwner());
                        if (!allowed) {
                            senderRef.sendMessage(ChatUtil.error(tm.get("management.no_permission")));
                            return;
                        }

                        for (Plot plot : latestGroup) {
                            plot.setOwner(targetUuid);
                            plot.setOwnerName(targetRef.getUsername());
                            Plots.getInstance().getRadarManager().updatePlotMarker(plot);
                            Plots.getInstance().getHologramManager().updateHologram(plot, store);
                        }
                        this.plotManager.savePlots();

                        senderRef.sendMessage(ChatUtil.success(tm.get("transfer.success",
                                "player", targetRef.getUsername(),
                                "count", String.valueOf(latestGroup.size()))));
                    }));
        });
    }
}
