package com.overworldlabs.plots.util;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.api.IPlotManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class PermissionUtil {
    public static final String PERM_ADMIN_BYPASS = "plots.admin.bypass";
    public static final String PERM_ADMIN_LEGACY = "plots.admin";
    public static final String PERM_ECONOMY_BYPASS = "plots.economy.bypass";

    private PermissionUtil() {
    }

    public static boolean hasAdminPermission(@Nonnull CommandSender sender) {
        try {
            UUID senderUuid = CommandSenderIdentity.uuid(sender);
            return hasAdminPermission(senderUuid);
        } catch (Exception ignored) {
            return sender.hasPermission(PERM_ADMIN_BYPASS)
                    || sender.hasPermission(PERM_ADMIN_LEGACY)
                    || sender.hasPermission(IPlotManager.PERM_ADMIN);
        }
    }

    public static boolean hasEconomyBypass(@Nonnull CommandSender sender) {
        return sender.hasPermission(PERM_ECONOMY_BYPASS);
    }

    public static boolean hasAdminPermission(@Nonnull UUID playerUuid) {
        Player onlinePlayer = getOnlinePlayer(playerUuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getPlayerRef().hasPermission(PERM_ADMIN_BYPASS)
                    || onlinePlayer.getPlayerRef().hasPermission(PERM_ADMIN_LEGACY)
                    || onlinePlayer.getPlayerRef().hasPermission(IPlotManager.PERM_ADMIN);
        }

        return hasPermission(playerUuid, PERM_ADMIN_BYPASS)
                || hasPermission(playerUuid, PERM_ADMIN_LEGACY)
                || hasPermission(playerUuid, IPlotManager.PERM_ADMIN);
    }

    public static boolean hasAdminPermission(@Nonnull UUID playerUuid, @Nullable Player player) {
        if (player != null) {
            try {
                if (player.getPlayerRef().hasPermission(PERM_ADMIN_BYPASS)
                        || player.getPlayerRef().hasPermission(PERM_ADMIN_LEGACY)
                        || player.getPlayerRef().hasPermission(IPlotManager.PERM_ADMIN)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return hasAdminPermission(playerUuid);
    }

    @Nullable
    private static Player getOnlinePlayer(@Nonnull UUID playerUuid) {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }

            PlayerRef playerRef = universe.getPlayer(playerUuid);
            if (playerRef == null) {
                return null;
            }

            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null) {
                return null;
            }

            return entityRef.getStore().getComponent(entityRef, Player.getComponentType());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean hasPermission(@Nonnull UUID playerUuid, @Nonnull String node) {
        try {
            return PermissionsModule.get().hasPermission(playerUuid, node);
        } catch (Exception ignored) {
            return false;
        }
    }
}
