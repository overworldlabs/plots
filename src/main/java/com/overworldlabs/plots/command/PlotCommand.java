package com.overworldlabs.plots.command;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;
import com.hypixel.hytale.server.core.command.system.exceptions.NoPermissionException;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.command.feedback.CommandFeedbackRenderer;
import com.overworldlabs.plots.command.feedback.PlotCommandHelpCatalog;
import com.overworldlabs.plots.command.sub.PlotAutoCommand;
import com.overworldlabs.plots.command.sub.PlotCancelCommand;
import com.overworldlabs.plots.command.sub.PlotClaimCommand;
import com.overworldlabs.plots.command.sub.PlotConfirmCommand;
import com.overworldlabs.plots.command.sub.PlotDeleteCommand;
import com.overworldlabs.plots.command.sub.PlotDebugCommand;
import com.overworldlabs.plots.command.sub.PlotFlagCommand;
import com.overworldlabs.plots.command.sub.PlotInfoCommand;
import com.overworldlabs.plots.command.sub.PlotListCommand;
import com.overworldlabs.plots.command.sub.PlotMenuCommand;
import com.overworldlabs.plots.command.sub.PlotAdminCommand;
import com.overworldlabs.plots.command.sub.PlotMergeCommand;
import com.overworldlabs.plots.command.sub.PlotRenameCommand;
import com.overworldlabs.plots.command.sub.PlotRefreshCommand;
import com.overworldlabs.plots.command.sub.PlotSetSpawnCommand;
import com.overworldlabs.plots.command.sub.PlotSpawnCommand;
import com.overworldlabs.plots.command.sub.PlotTrustCommand;
import com.overworldlabs.plots.command.sub.PlotTransferCommand;
import com.overworldlabs.plots.command.sub.PlotUnmergeCommand;
import com.overworldlabs.plots.command.sub.PlotUntrustCommand;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.CommandSenderIdentity;
import com.overworldlabs.plots.util.ConsoleColors;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Main /plot command collection - handles subcommands like /plot claim, /plot
 * info, etc.
 */
public class PlotCommand extends AbstractCommandCollection {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("Plots");
    private static final Map<String, String[]> SUBCOMMAND_USAGE_KEYS = PlotCommandHelpCatalog.subcommandUsageKeys();
    private static final Field PARSE_FAILED_FIELD = resolveParseField("failed");
    private static final Field PARSE_REASONS_FIELD = resolveParseField("reasons");

    public PlotCommand(@Nonnull IPlotManager plotManager) {
        this("plot", plotManager);
    }

    public PlotCommand(@Nonnull String rootName, @Nonnull IPlotManager plotManager) {
        super(rootName, "Plot management commands");
        requirePermission(IPlotManager.PERM_BASE);
        setAllowsExtraArguments(true);

        // Add all subcommands
        addSubCommand(new PlotClaimCommand(plotManager));
        addSubCommand(new PlotAutoCommand(plotManager));
        addSubCommand(new PlotConfirmCommand());
        addSubCommand(new PlotCancelCommand());
        addSubCommand(new PlotInfoCommand(plotManager));
        addSubCommand(new PlotDeleteCommand(plotManager));
        addSubCommand(new PlotDebugCommand());
        addSubCommand(new PlotListCommand(plotManager));
        addSubCommand(new PlotSpawnCommand(plotManager));
        addSubCommand(new PlotRenameCommand(plotManager));
        addSubCommand(new PlotTrustCommand(plotManager));
        addSubCommand(new PlotUntrustCommand(plotManager));
        addSubCommand(new PlotTransferCommand(plotManager));
        addSubCommand(new PlotFlagCommand(plotManager));
        addSubCommand(new PlotMergeCommand(plotManager));
        addSubCommand(new PlotUnmergeCommand(plotManager));
        addSubCommand(new PlotSetSpawnCommand(plotManager));
        addSubCommand(new PlotRefreshCommand(plotManager));
        addSubCommand(new PlotMenuCommand(plotManager));
        addSubCommand(new PlotAdminCommand(plotManager));
    }

    @Override
    public CompletableFuture<Void> acceptCall(@Nonnull CommandSender sender, @Nonnull ParserContext parserContext,
            @Nonnull ParseResult parseResult) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();
        List<String> argsTokens = extractCommandArgs(parserContext);
        String requestedSubcommand = resolveRequestedSubcommand(argsTokens);
        if (requestedSubcommand != null && !hasPermissionForSubcommand(sender, requestedSubcommand)) {
            sender.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            suppressDefaultParseFeedback(parseResult);
            return CompletableFuture.completedFuture(null);
        }

        if (!parseResult.failed() && argsTokens.isEmpty()) {
            CommandFeedbackRenderer.sendRootHelp(sender, tm, "/" + Objects.requireNonNullElse(getName(), "plot"));
            return CompletableFuture.completedFuture(null);
        }

        if (parseResult.failed()) {
            handleCustomParseFeedback(sender, parserContext, parseResult, tm);
            return CompletableFuture.completedFuture(null);
        }

        try {
            CompletableFuture<Void> future = super.acceptCall(sender, parserContext, parseResult);
            if (parseResult.failed()) {
                handleCustomParseFeedback(sender, parserContext, parseResult, tm);
                return CompletableFuture.completedFuture(null);
            }
            if (future == null) {
                return CompletableFuture.completedFuture(null);
            }
            return future.exceptionally(throwable -> {
                handleExecutionException(sender, parserContext, tm, throwable);
                return null;
            });
        } catch (Throwable throwable) {
            handleExecutionException(sender, parserContext, tm, throwable);
            return CompletableFuture.completedFuture(null);
        }
    }

    private String resolveRequestedSubcommand(@Nonnull List<String> argsTokens) {
        if (argsTokens.isEmpty()) {
            return null;
        }

        String token = argsTokens.get(0);
        if (token == null || token.isBlank()) {
            return null;
        }

        String normalized = token.trim().toLowerCase(Locale.ROOT);
        return getSubCommands().values().stream()
                .filter(Objects::nonNull)
                .filter(cmd -> {
                    String name = cmd.getName();
                    if (name != null && name.equalsIgnoreCase(normalized)) {
                        return true;
                    }
                    Set<String> aliases = cmd.getAliases();
                    return aliases != null
                            && aliases.stream().anyMatch(alias -> alias != null && alias.equalsIgnoreCase(normalized));
                })
                .map(cmd -> cmd.getName() != null ? cmd.getName().toLowerCase(Locale.ROOT) : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean hasPermissionForSubcommand(@Nonnull CommandSender sender, @Nonnull String subcommand) {
        if (PermissionUtil.hasAdminPermission(sender)) {
            return true;
        }

        return switch (subcommand) {
            case "claim", "auto" -> sender.hasPermission(IPlotManager.PERM_CLAIM);
            case "delete" -> sender.hasPermission(IPlotManager.PERM_DELETE);
            case "info" -> sender.hasPermission(IPlotManager.PERM_INFO);
            case "list" -> sender.hasPermission(IPlotManager.PERM_LIST);
            case "spawn" -> sender.hasPermission(IPlotManager.PERM_SPAWN);
            case "rename" -> sender.hasPermission(IPlotManager.PERM_RENAME);
            case "trust", "untrust" -> sender.hasPermission(IPlotManager.PERM_TRUST);
            case "transfer" -> sender.hasPermission(IPlotManager.PERM_TRANSFER);
            case "flag" -> sender.hasPermission(IPlotManager.PERM_FLAG);
            case "merge" -> sender.hasPermission(IPlotManager.PERM_MERGE);
            case "unmerge" -> sender.hasPermission(IPlotManager.PERM_UNMERGE);
            case "setspawn", "refresh", "debug" -> false;
            case "confirm", "cancel" -> sender.hasPermission(IPlotManager.PERM_BASE);
            default -> sender.hasPermission(IPlotManager.PERM_BASE);
        };
    }

    private boolean isKnownSubcommand(@Nonnull String token) {
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        return getSubCommands().values().stream().anyMatch(cmd -> {
            String name = cmd.getName();
            if (name != null && name.equalsIgnoreCase(normalized)) {
                return true;
            }
            var aliases = cmd.getAliases();
            return aliases != null && aliases.stream().anyMatch(alias -> alias != null && alias.equalsIgnoreCase(normalized));
        });
    }

    private void sendSubcommandUsage(@Nonnull CommandSender sender, @Nonnull TranslationManager tm, @Nonnull String subcommand) {
        String[] usage = SUBCOMMAND_USAGE_KEYS.get(subcommand);

        if (usage == null || usage.length == 0) {
            CommandFeedbackRenderer.sendRootHelp(sender, tm, "/" + Objects.requireNonNullElse(getName(), "plot"));
            return;
        }

        List<String> keys = new ArrayList<>(usage.length);
        for (String key : usage) {
            if (key != null && !key.isBlank()) {
                keys.add(key);
            }
        }

        if (keys.isEmpty()) {
            CommandFeedbackRenderer.sendRootHelp(sender, tm, "/" + Objects.requireNonNullElse(getName(), "plot"));
            return;
        }

        CommandFeedbackRenderer.sendSubcommandHelp(sender, tm, keys);
    }

    private void handleCustomParseFeedback(@Nonnull CommandSender sender, @Nonnull ParserContext parserContext,
            @Nonnull ParseResult parseResult, @Nonnull TranslationManager tm) {
        String root = "/" + Objects.requireNonNullElse(getName(), "plot");
        List<String> argsTokens = extractCommandArgs(parserContext);

        if (argsTokens.isEmpty()) {
            CommandFeedbackRenderer.sendRootHelp(sender, tm, root);
        } else {
            String subcommand = argsTokens.get(0);
            List<String> specializedUsageKeys = PlotCommandHelpCatalog.usageKeysFor(argsTokens);
            if (!specializedUsageKeys.isEmpty()) {
                CommandFeedbackRenderer.sendSubcommandHelp(sender, tm, specializedUsageKeys);
            } else if (isKnownSubcommand(subcommand)) {
                sendSubcommandUsage(sender, tm, subcommand);
            } else {
                sender.sendMessage(ChatUtil.colorize("{#e74c3c}" + tm.get("commandsystem.command_not_recognized",
                        "command", root + " " + subcommand)));
                CommandFeedbackRenderer.sendRootHelp(sender, tm, root);
            }
        }
        suppressDefaultParseFeedback(parseResult);
    }

    @Nonnull
    private List<String> extractCommandArgs(@Nonnull ParserContext parserContext) {
        String input = parserContext.getInputString();
        if (input == null || input.isBlank()) {
            return Collections.emptyList();
        }

        String normalized = input.startsWith("/") ? input.substring(1) : input;
        String[] parts = normalized.trim().split("\\s+");
        if (parts.length == 0) {
            return Collections.emptyList();
        }

        String rootName = Objects.requireNonNullElse(getName(), "plot").toLowerCase(Locale.ROOT);
        Set<String> aliases = getAliases() != null ? getAliases() : Collections.emptySet();
        String first = parts[0].toLowerCase(Locale.ROOT);

        int startIndex = 0;
        if (first.equals(rootName) || aliases.stream().anyMatch(a -> a != null && first.equals(a.toLowerCase(Locale.ROOT)))) {
            startIndex = 1;
        }

        if (startIndex >= parts.length) {
            return Collections.emptyList();
        }

        List<String> args = new ArrayList<>(parts.length - startIndex);
        for (int i = startIndex; i < parts.length; i++) {
            if (parts[i] != null && !parts[i].isBlank()) {
                args.add(parts[i].toLowerCase(Locale.ROOT));
            }
        }
        return args;
    }

    private void handleExecutionException(@Nonnull CommandSender sender, @Nonnull ParserContext parserContext, @Nonnull TranslationManager tm, @Nonnull Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof NoPermissionException) {
            sender.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        String input = parserContext.getInputString();
        String senderUuid = "unknown";
        try {
            senderUuid = String.valueOf(CommandSenderIdentity.uuid(sender));
        } catch (Exception ignored) {
        }
        LOGGER.log(java.util.logging.Level.SEVERE,
                "Failed to execute command '" + input + "' for " + sender.getDisplayName() + " (" + senderUuid
                        + ")",
                cause);
        ConsoleColors.error("Command error for input: " + input + " (" + cause.getClass().getSimpleName() + ")");
        sender.sendMessage(ChatUtil.error(tm.get("general.error_generic")));
    }

    private Throwable unwrap(@Nonnull Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)) {
            current = current.getCause();
        }

        return current;
    }

    private static Field resolveParseField(@Nonnull String name) {
        try {
            Field field = ParseResult.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void suppressDefaultParseFeedback(@Nonnull ParseResult parseResult) {
        try {
            if (PARSE_FAILED_FIELD != null) {
                PARSE_FAILED_FIELD.setBoolean(parseResult, false);
            }
            if (PARSE_REASONS_FIELD != null) {
                PARSE_REASONS_FIELD.set(parseResult, null);
            }
        } catch (Throwable ignored) {
            // Best-effort: if reflection breaks on future engine versions,
            // command feedback still works and only fallback messages may reappear.
        }
    }
}
