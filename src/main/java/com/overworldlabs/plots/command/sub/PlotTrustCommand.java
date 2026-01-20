package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.NameMatching;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot trust <player>
 * Grants building permission to a player on your plot
 */
public class PlotTrustCommand extends CommandBase {
    private final PlotManager plotManager;
    private final RequiredArg<String> playerArg;

    public PlotTrustCommand(@Nonnull PlotManager plotManager) {
        super("trust", "Grant build permissions to a player");
        this.plotManager = plotManager;
        this.playerArg = (RequiredArg<String>) withRequiredArg("player", "Player name", ArgTypes.STRING);
        requirePermission(PlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!context.sender().hasPermission(PlotManager.PERM_ADMIN)) {
            CommandUtil.requirePermission(context.sender(), PlotManager.PERM_TRUST);
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

            // Get plot at player location
            Vector3d pos = playerRef.getTransform().getPosition();
            Plot plot = this.plotManager.getPlotAt(currentWorld.getName(), (int) pos.x, (int) pos.z);

            if (plot == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            // Ownership check
            if (!plot.getOwner().equals(playerRef.getUuid())) {
                if (!context.sender().hasPermission(PlotManager.PERM_ADMIN)) {
                    CommandUtil.requirePermission(context.sender(), PlotManager.PERM_ADMIN);
                }
            }

            String targetPlayerName = playerArg.get(context);

            if (targetPlayerName == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("trust.usage")));
                return;
            }

            // Find the target player's UUID
            PlayerRef targetRef = Universe.get().getPlayerByUsername(targetPlayerName,
                    NameMatching.EXACT);
            java.util.UUID targetUuid = null;

            if (targetRef != null) {
                targetUuid = targetRef.getUuid();
            } else {
                // Player is offline - we need their UUID from somewhere
                playerRef.sendMessage(ChatUtil.error(tm.get("trust.player_offline")));
                return;
            }

            if (targetUuid.equals(plot.getOwner())) {
                playerRef.sendMessage(ChatUtil.error(tm.get("trust.cannot_owner")));
                return;
            }

            if (plot.isTrusted(targetUuid)) {
                playerRef.sendMessage(ChatUtil.error(tm.get("trust.already_trusted", "player", targetPlayerName)));
            } else {
                plot.addTrustedPlayer(targetUuid);
                playerRef.sendMessage(ChatUtil.success(tm.get("trust.added", "player", targetPlayerName)));
            }
        });
    }
}
