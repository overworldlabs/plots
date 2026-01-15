package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.NameMatching;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Command: /plot trust <add|remove> <player>
 * Manages trusted players for a plot
 */
public class PlotTrustCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;
    private final RequiredArg<String> actionArg;
    private final RequiredArg<String> playerArg;

    public PlotTrustCommand(@Nonnull PlotManager plotManager) {
        super("trust", "Manage trusted players on your plot");
        this.plotManager = plotManager;
        this.actionArg = withRequiredArg("action", "add or remove", ArgTypes.STRING);
        this.playerArg = withRequiredArg("player", "Player name", ArgTypes.STRING);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        TranslationManager tm = Plots.getInstance().getTranslationManager();

        // Permission check
        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(),
                PlotManager.PERM_TRUST, true)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        // Get plot at player location
        Vector3d pos = playerRef.getTransform().getPosition();
        Plot plot = plotManager.getPlotAt(world.getName(), (int) pos.x, (int) pos.z);

        if (plot == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
            return;
        }

        // Ownership check
        if (!plot.getOwner().equals(playerRef.getUuid())
                && !PermissionsModule.get()
                        .hasPermission(playerRef.getUuid(), PlotManager.PERM_ADMIN)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.not_owner")));
            return;
        }

        String action = actionArg.get(context);
        String targetPlayerName = playerArg.get(context);

        if (action == null || targetPlayerName == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("trust.usage")));
            return;
        }

        // Find the target player's UUID
        PlayerRef targetRef = Universe.get().getPlayerByUsername(targetPlayerName,
                NameMatching.EXACT);
        UUID targetUuid = null;

        if (targetRef != null) {
            targetUuid = targetRef.getUuid();
        } else {
            // Player is offline - we need their UUID from somewhere
            // For now, show an error
            playerRef.sendMessage(ChatUtil.error(tm.get("trust.player_offline")));
            return;
        }

        if (targetUuid.equals(plot.getOwner())) {
            playerRef.sendMessage(ChatUtil.error(tm.get("trust.cannot_owner")));
            return;
        }

        if (action.equalsIgnoreCase("add")) {
            if (plot.isTrusted(targetUuid)) {
                playerRef.sendMessage(ChatUtil.error(tm.get("trust.already_trusted", "player", targetPlayerName)));
            } else {
                plot.addTrustedPlayer(targetUuid);
                playerRef.sendMessage(ChatUtil.success(tm.get("trust.added", "player", targetPlayerName)));
            }
        } else if (action.equalsIgnoreCase("remove")) {
            if (!plot.isTrusted(targetUuid)) {
                playerRef.sendMessage(ChatUtil.error(tm.get("trust.not_trusted", "player", targetPlayerName)));
            } else {
                plot.removeTrustedPlayer(targetUuid);
                playerRef.sendMessage(ChatUtil.success(tm.get("trust.removed", "player", targetPlayerName)));
            }
        } else {
            playerRef.sendMessage(ChatUtil.error(tm.get("trust.invalid_action")));
        }
    }
}
