package dev.stoshe.plots.command;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;
import com.hypixel.hytale.server.core.command.system.exceptions.NoPermissionException;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.pages.CommandListPage;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.command.feedback.CommandFeedbackRenderer;
import dev.stoshe.plots.command.feedback.PlotCommandHelpCatalog;
import dev.stoshe.plots.command.sub.PlotAutoCommand;
import dev.stoshe.plots.command.sub.PlotCancelCommand;
import dev.stoshe.plots.command.sub.PlotClaimCommand;
import dev.stoshe.plots.command.sub.PlotConfirmCommand;
import dev.stoshe.plots.command.sub.PlotDeleteCommand;
import dev.stoshe.plots.command.sub.PlotDebugCommand;
import dev.stoshe.plots.command.sub.PlotFlagCommand;
import dev.stoshe.plots.command.sub.PlotInfoCommand;
import dev.stoshe.plots.command.sub.PlotListCommand;
import dev.stoshe.plots.command.sub.PlotMenuCommand;
import dev.stoshe.plots.command.sub.PlotAdminCommand;
import dev.stoshe.plots.command.sub.PlotWarpsCommand;
import dev.stoshe.plots.command.sub.PlotWorldsCommand;
import dev.stoshe.plots.command.sub.PlotHelpCommand;
import dev.stoshe.plots.util.PlotSetup;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import dev.stoshe.plots.command.sub.PlotMergeCommand;
import dev.stoshe.plots.command.sub.PlotRenameCommand;
import dev.stoshe.plots.command.sub.PlotRefreshCommand;
import dev.stoshe.plots.command.sub.PlotSetSpawnCommand;
import dev.stoshe.plots.command.sub.PlotSpawnCommand;
import dev.stoshe.plots.command.sub.PlotTrustCommand;
import dev.stoshe.plots.command.sub.PlotTransferCommand;
import dev.stoshe.plots.command.sub.PlotUnmergeCommand;
import dev.stoshe.plots.command.sub.PlotUntrustCommand;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.CommandSenderIdentity;
import dev.stoshe.plots.util.Console;
import dev.stoshe.plots.util.PermissionUtil;

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
        addSubCommand(new PlotWarpsCommand(plotManager));
        addSubCommand(new PlotWorldsCommand(plotManager));
        addSubCommand(new PlotHelpCommand(plotManager));
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

        // Until at least one plot world exists, block normal commands and show the
        // setup reminder. The setup/diagnostic commands stay available so admins can
        // create a world.
        if (requestedSubcommand != null && !isAllowedDuringSetup(requestedSubcommand) && !PlotSetup.isReady()) {
            PlotSetup.sendReminder(sender);
            suppressDefaultParseFeedback(parseResult);
            return CompletableFuture.completedFuture(null);
        }

        if (!parseResult.failed() && argsTokens.isEmpty()) {
            if (!openHelpModal(sender)) {
                CommandFeedbackRenderer.sendRootHelp(sender, tm, "/" + Objects.requireNonNullElse(getName(), "plot"));
            }
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

    /**
     * Opens the command-help modal for a player sender.
     *
     * @param sender the command sender
     * @return {@code true} when a player was resolved and the modal was opened,
     *         {@code false} for console/unresolved senders (use chat help instead)
     */
    private boolean openHelpModal(@Nonnull CommandSender sender) {
        UUID uuid = CommandSenderIdentity.uuid(sender);
        if (uuid == null) {
            return false;
        }
        PlayerRef playerObj = Universe.get().getPlayer(uuid);
        if (playerObj == null || playerObj.getWorldUuid() == null) {
            return false;
        }
        World world = Universe.get().getWorld(playerObj.getWorldUuid());
        if (world == null) {
            return false;
        }
        world.execute(() -> {
            Ref<EntityStore> ref = playerObj.getReference();
            if (ref == null) {
                return;
            }
            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            Player player = store.getComponent(ref, Player.getComponentType());
            if (playerRef == null || player == null) {
                return;
            }
            // Bare /plot opens the same native help UI as /plot help. See PlotHelpCommand.
            player.getPageManager().openCustomPage(ref, store, new CommandListPage(playerRef, "plot"));
        });
        return true;
    }

    /**
     * Commands that remain usable before any plot world exists, so admins can set
     * one up.
     *
     * @param subcommand the resolved subcommand name
     * @return {@code true} when the command bypasses the setup gate
     */
    private boolean isAllowedDuringSetup(@Nonnull String subcommand) {
        return switch (subcommand) {
            case "admin", "refresh", "debug", "help" -> true;
            default -> false;
        };
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
                List<String> guesses = suggestSubcommands(subcommand);
                if (!guesses.isEmpty()) {
                    sender.sendMessage(ChatUtil.colorize("{#95a5a6}" + tm.get("commandsystem.did_you_mean",
                            "suggestions", String.join(", ", guesses))));
                }
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}" + tm.get("commandsystem.help_tip_root")));
            }
        }
        suppressDefaultParseFeedback(parseResult);
    }

    /**
     * Finds the closest known subcommand names for a mistyped token, so the
     * "command not recognized" message can offer a short "did you mean" hint
     * instead of dumping the entire command list.
     *
     * @param token the unrecognised subcommand the player typed
     * @return up to three candidate subcommand names, best matches first
     */
    @Nonnull
    private List<String> suggestSubcommands(@Nonnull String token) {
        String needle = token.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> prefixMatches = new ArrayList<>();
        List<String> containsMatches = new ArrayList<>();
        for (String name : getSubCommands().keySet()) {
            if (name == null) {
                continue;
            }
            String candidate = name.toLowerCase(Locale.ROOT);
            if (candidate.equals(needle)) {
                continue;
            }
            if (candidate.startsWith(needle)) {
                prefixMatches.add(name);
            } else if (candidate.contains(needle) || needle.contains(candidate)) {
                containsMatches.add(name);
            }
        }

        List<String> guesses = new ArrayList<>(prefixMatches);
        guesses.addAll(containsMatches);
        return guesses.size() > 3 ? guesses.subList(0, 3) : guesses;
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
        Console.error("Failed to execute command '" + input + "' for " + sender.getUsername()
                + " (" + senderUuid + ")", cause);
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
