package dev.stoshe.plots.command.sub;

import dev.stoshe.plots.util.CommandSenderIdentity;

import dev.stoshe.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandUtil;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.NameMatching;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.command.CommandArgs;
import dev.stoshe.plots.command.feedback.CommandFeedbackService;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot trust <player>
 * Grants building permission to a player on your plot
 */
public class PlotTrustCommand extends CommandBase {
    private final IPlotManager plotManager;
    private final RequiredArg<String> playerArg;

    public PlotTrustCommand(@Nonnull IPlotManager plotManager) {
        super("trust", "Grant build permissions to a player");
        setAllowsExtraArguments(true);
        this.plotManager = plotManager;
        this.playerArg = CommandArgs.required(this, "player", "Player name",
                new dev.stoshe.plots.command.SuggestingStringArgumentType("player", "Player name",
                        dev.stoshe.plots.command.CommandSuggestions::onlinePlayerNames));
        requirePermission(IPlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(),
                    IPlotManager.PERM_TRUST);
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

            // Get plot at player location
            Vector3d pos = playerRef.getTransform().getPosition();
            Plot plot = this.plotManager.getPlotAt(currentWorld.getName(), (int) pos.x, (int) pos.z);

            if (plot == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            // Ownership check
            if (!plot.getOwner().equals(PlayerIdentity.uuid(playerRef))) {
                if (!PermissionUtil.hasAdminPermission(context.sender())) {
                    CommandUtil.requirePermission(context.sender(),
                            IPlotManager.PERM_ADMIN);
                }
            }

            String targetPlayerName = playerArg.get(context);

            if (targetPlayerName == null) {
                CommandFeedbackService.sendUsage(playerRef, tm, "trust.usage");
                return;
            }

            // Find the target player's UUID
            PlayerRef targetRef = Universe.get().getPlayerByUsername(targetPlayerName,
                    NameMatching.EXACT);
            java.util.UUID targetUuid = null;

            if (targetRef != null) {
                targetUuid = PlayerIdentity.uuid(targetRef);
            } else {
                // Player is offline - we need their UUID from somewhere
                playerRef.sendMessage(ChatUtil.error(tm.get("trust.player_offline")));
                return;
            }
            if (targetUuid == null) {
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
