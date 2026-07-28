package dev.stoshe.plots.command.feedback;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.util.ChatUtil;

import javax.annotation.Nonnull;

public final class CommandFeedbackService {
    private CommandFeedbackService() {
    }

    public static void sendUsage(@Nonnull PlayerRef player, @Nonnull TranslationManager tm, @Nonnull String usageKey,
            @Nonnull String... argumentUsageKeys) {
        CommandFeedbackRenderer.sendUsage(player, tm, usageKey, argumentUsageKeys);
    }

    public static void sendInvalidSubcommand(@Nonnull PlayerRef player, @Nonnull TranslationManager tm,
            @Nonnull String commandInput, @Nonnull String usageKey, @Nonnull String... argumentUsageKeys) {
        player.sendMessage(ChatUtil.colorize(
                "{#e74c3c}" + tm.get("commandsystem.command_not_recognized", "command", commandInput)));
        sendUsage(player, tm, usageKey, argumentUsageKeys);
    }

    public static void sendUsage(@Nonnull CommandSender sender, @Nonnull TranslationManager tm, @Nonnull String usageKey,
            @Nonnull String... argumentUsageKeys) {
        CommandFeedbackRenderer.sendUsage(sender, tm, usageKey, argumentUsageKeys);
    }

    public static void sendInvalidSubcommand(@Nonnull CommandSender sender, @Nonnull TranslationManager tm,
            @Nonnull String commandInput, @Nonnull String usageKey, @Nonnull String... argumentUsageKeys) {
        sender.sendMessage(ChatUtil.colorize(
                "{#e74c3c}" + tm.get("commandsystem.command_not_recognized", "command", commandInput)));
        sendUsage(sender, tm, usageKey, argumentUsageKeys);
    }

    public static void sendInvalidArgument(@Nonnull CommandSender sender, @Nonnull TranslationManager tm,
            @Nonnull String commandInput, @Nonnull String usageKey, @Nonnull String... argumentUsageKeys) {
        sender.sendMessage(ChatUtil.colorize(
                "{#e74c3c}" + tm.get("commandsystem.command_not_recognized", "command", commandInput)));
        sendUsage(sender, tm, usageKey, argumentUsageKeys);
    }
}
