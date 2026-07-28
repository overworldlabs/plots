package dev.stoshe.plots.command.sub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.ui.PlotMenuPage;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.CommandSenderIdentity;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Command: /plot menu — opens the plot management menu.
 */
public class PlotMenuCommand extends CommandBase {
    private final IPlotManager plotManager;

    public PlotMenuCommand(@Nonnull IPlotManager plotManager) {
        super("menu", "Open the plot management menu");
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
        if (ref == null) {
            return;
        }
        UUID senderUuid = CommandSenderIdentity.uuid(context.sender());
        if (senderUuid == null) {
            return;
        }
        PlayerRef playerObj = Universe.get().getPlayer(senderUuid);
        if (playerObj == null || playerObj.getWorldUuid() == null) {
            return;
        }
        World currentWorld = Universe.get().getWorld(playerObj.getWorldUuid());
        if (currentWorld == null) {
            return;
        }
        currentWorld.execute(() -> {
            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            Player player = store.getComponent(ref, Player.getComponentType());
            if (playerRef == null || player == null) {
                return;
            }
            // Only open the menu when the player is standing on a plot; off-plot
            // (roads, intersections) there is nothing to manage.
            org.joml.Vector3d pos = playerRef.getTransform() != null
                    ? playerRef.getTransform().getPosition()
                    : null;
            Plot plot = pos == null ? null
                    : plotManager.getPlotAt(currentWorld.getName(),
                            (int) Math.floor(pos.x), (int) Math.floor(pos.z));
            if (plot == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }
            // Access is limited to the plot's owner and its trusted members, not
            // gated by a game permission node.
            if (!plot.isOwnerOrMember(senderUuid) && !PermissionUtil.hasAdminPermission(senderUuid)) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.no_permission")));
                return;
            }
            PlotMenuPage.open(player, ref, store, playerRef, currentWorld, Plots.getInstance());
        });
    }
}
