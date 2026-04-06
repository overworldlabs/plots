package com.overworldlabs.plots.command;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.CommandSenderIdentity;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlotConfirmationService {
    private static final long DEFAULT_TTL_MILLIS = 30_000L;
    private static final PlotConfirmationService INSTANCE = new PlotConfirmationService();

    private final ConcurrentMap<UUID, PendingAction> pendingActions = new ConcurrentHashMap<>();

    private PlotConfirmationService() {
    }

    @Nonnull
    public static PlotConfirmationService getInstance() {
        return INSTANCE;
    }

    public void request(@Nonnull CommandSender sender, @Nonnull TranslationManager tm, @Nonnull String action,
            @Nonnull Runnable onConfirm) {
        UUID senderUuid = CommandSenderIdentity.uuid(sender);
        long expiresAt = System.currentTimeMillis() + DEFAULT_TTL_MILLIS;
        pendingActions.put(senderUuid, new PendingAction(onConfirm, action, expiresAt));

        String message = tm.get("confirm.request", "action", action, "seconds",
                String.valueOf(DEFAULT_TTL_MILLIS / 1000L),
                "confirm_cmd", resolveCommandToken(tm, "confirm.command_confirm", "/plot confirm"),
                "cancel_cmd", resolveCommandToken(tm, "confirm.command_cancel", "/plot cancel"),
                "deny_cmd", resolveCommandToken(tm, "confirm.command_deny", "/plot deny"));
        sender.sendMessage(ChatUtil.info(message));
    }

    public void confirm(@Nonnull CommandSender sender, @Nonnull TranslationManager tm) {
        UUID senderUuid = CommandSenderIdentity.uuid(sender);
        PendingAction pending = pendingActions.remove(senderUuid);
        if (pending == null) {
            sender.sendMessage(ChatUtil.error(tm.get("confirm.none")));
            return;
        }

        if (pending.isExpired()) {
            sender.sendMessage(ChatUtil.error(tm.get("confirm.expired")));
            return;
        }

        pending.action.run();
        sender.sendMessage(ChatUtil.success(tm.get("confirm.confirmed", "action", pending.actionName)));
    }

    public void cancel(@Nonnull CommandSender sender, @Nonnull TranslationManager tm) {
        UUID senderUuid = CommandSenderIdentity.uuid(sender);
        PendingAction pending = pendingActions.remove(senderUuid);
        if (pending == null) {
            sender.sendMessage(ChatUtil.error(tm.get("confirm.none")));
            return;
        }
        sender.sendMessage(ChatUtil.info(tm.get("confirm.cancelled", "action", pending.actionName)));
    }

    @Nonnull
    private String resolveCommandToken(@Nonnull TranslationManager tm, @Nonnull String key, @Nonnull String fallback) {
        String value = tm.get(key);
        if (value == null || value.isBlank() || key.equals(value)) {
            return fallback;
        }
        return value;
    }

    private static final class PendingAction {
        private final Runnable action;
        private final String actionName;
        private final long expiresAtMillis;

        private PendingAction(@Nonnull Runnable action, @Nonnull String actionName, long expiresAtMillis) {
            this.action = action;
            this.actionName = actionName;
            this.expiresAtMillis = expiresAtMillis;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
