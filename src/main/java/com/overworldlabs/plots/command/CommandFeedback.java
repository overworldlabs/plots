package com.overworldlabs.plots.command;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.overworldlabs.plots.command.feedback.CommandFeedbackService;
import com.overworldlabs.plots.manager.TranslationManager;

import javax.annotation.Nonnull;

@Deprecated
public final class CommandFeedback {
    private CommandFeedback() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void sendUsage(@Nonnull PlayerRef player, @Nonnull TranslationManager tm, @Nonnull String usageKey,
            @Nonnull String... argumentUsageKeys) {
        CommandFeedbackService.sendUsage(player, tm, usageKey, argumentUsageKeys);
    }

    public static void sendInvalidSubcommand(@Nonnull PlayerRef player, @Nonnull TranslationManager tm,
            @Nonnull String commandInput, @Nonnull String usageKey, @Nonnull String... argumentUsageKeys) {
        CommandFeedbackService.sendInvalidSubcommand(player, tm, commandInput, usageKey, argumentUsageKeys);
    }

    public static void sendUsage(@Nonnull CommandSender sender, @Nonnull TranslationManager tm, @Nonnull String usageKey,
            @Nonnull String... argumentUsageKeys) {
        CommandFeedbackService.sendUsage(sender, tm, usageKey, argumentUsageKeys);
    }

    public static void sendInvalidSubcommand(@Nonnull CommandSender sender, @Nonnull TranslationManager tm,
            @Nonnull String commandInput, @Nonnull String usageKey, @Nonnull String... argumentUsageKeys) {
        CommandFeedbackService.sendInvalidSubcommand(sender, tm, commandInput, usageKey, argumentUsageKeys);
    }
}
