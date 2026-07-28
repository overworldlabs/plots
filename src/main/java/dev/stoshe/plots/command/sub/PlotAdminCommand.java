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
import dev.stoshe.plots.ui.PlotAdminPage;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.CommandSenderIdentity;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Command: /plot admin — opens the administrative dashboard.
 */
public class PlotAdminCommand extends CommandBase {
    private final IPlotManager plotManager;

    public PlotAdminCommand(@Nonnull IPlotManager plotManager) {
        super("admin", "Open the plot administration dashboard");
        this.plotManager = plotManager;
        requirePermission(IPlotManager.PERM_ADMIN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();
        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }
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
            PlotAdminPage.open(player, ref, store, playerRef, currentWorld, Plots.getInstance());
        });
    }
}
