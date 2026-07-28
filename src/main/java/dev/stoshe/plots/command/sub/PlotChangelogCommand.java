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
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.ui.PlotChangelogPage;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.CommandSenderIdentity;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Command: {@code /plot changelog} — re-opens the latest release notes popup (admin).
 */
public class PlotChangelogCommand extends CommandBase {

    public PlotChangelogCommand() {
        super("changelog", "Show what changed in this release");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();
        if (!context.isPlayer()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.only_players")));
            return;
        }
        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }
        if (!Plots.getInstance().getChangelogManager().isReady()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("changelog.unavailable")));
            return;
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        UUID senderUuid = CommandSenderIdentity.uuid(context.sender());
        if (ref == null || senderUuid == null) {
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
            PlotChangelogPage.open(player, ref, store, playerRef, Plots.getInstance());
        });
    }
}
