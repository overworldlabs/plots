package dev.stoshe.plots.system;

import dev.stoshe.plots.util.PlayerIdentity;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.Universe;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.api.IWorldManager;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * System to manage plot chat and chat flags
 */
public class PlotChatSystem implements Consumer<PlayerChatEvent> {
    private final IPlotManager plotManager;

    public PlotChatSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        this.plotManager = plotManager;
    }

    @Override
    public void accept(@Nonnull PlayerChatEvent event) {
        PlayerRef playerRef = event.getSender();
        if (playerRef == null)
            return;
        if (PermissionUtil.hasAdminPermission(PlayerIdentity.uuid(playerRef))) {
            return;
        }

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null || !plotManager.getConfig().isManagedWorld(world.getName())) {
            return;
        }

        Vector3d pos = playerRef.getTransform().getPosition();
        Plot plot = plotManager.getPlotAt(world.getName(), (int) pos.x, (int) pos.z);

        if (plot != null) {
            String content = event.getContent();
            if (content != null && content.startsWith("/")) {
                String command = content.substring(1).trim().toLowerCase();
                String root = command.isEmpty() ? "" : command.split("\\s+")[0];

                String allowedRaw = plot.getFlagValue(FlagRegistry.ALLOWED_COMMANDS);
                if (isCommandListEnabled(allowedRaw) && !containsCommand(allowedRaw, root)) {
                    event.setCancelled(true);
                    TranslationManager tm = Plots.getInstance().getTranslationManager();
                    playerRef.sendMessage(ChatUtil.error(resolveDenyMessage(plot, tm.get("general.no_permission"))));
                    return;
                }

                String blockedRaw = plot.getFlagValue(FlagRegistry.BLOCKED_COMMANDS);
                if (containsCommand(blockedRaw, root)) {
                    event.setCancelled(true);
                    TranslationManager tm = Plots.getInstance().getTranslationManager();
                    playerRef.sendMessage(ChatUtil.error(resolveDenyMessage(plot, tm.get("general.no_permission"))));
                    return;
                }
            }

            boolean chatEnabled = plot.getFlagValue(FlagRegistry.PLAYER_CHAT);
            if (!chatEnabled && !plot.isOwnerOrMember(PlayerIdentity.uuid(playerRef))) {
                event.setCancelled(true);
                TranslationManager tm = Plots.getInstance().getTranslationManager();
                playerRef.sendMessage(ChatUtil.error(tm.get("protection.chat_disabled")));
                return;
            }

            // Custom chat prefix logic can go here
        }
    }

    private boolean isCommandListEnabled(String raw) {
        return raw != null && !raw.trim().isEmpty();
    }

    private boolean containsCommand(String csv, String root) {
        if (csv == null || csv.trim().isEmpty() || root == null || root.isEmpty()) {
            return false;
        }
        for (String token : csv.toLowerCase().split(",")) {
            if (root.equals(token.trim())) {
                return true;
            }
        }
        return false;
    }

    private String resolveDenyMessage(Plot plot, String fallback) {
        String custom = plot.getFlagValue(FlagRegistry.DENY_MESSAGE);
        if (custom != null && !custom.trim().isEmpty()) {
            return custom;
        }
        return fallback;
    }
}
