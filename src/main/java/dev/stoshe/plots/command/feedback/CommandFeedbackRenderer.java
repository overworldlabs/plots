package dev.stoshe.plots.command.feedback;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.flag.PlotFlag;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandFeedbackRenderer {
    private static final String SEPARATOR = "{#f5c542}----------------------------------";

    private CommandFeedbackRenderer() {
    }

    public static void sendRootHelp(@Nonnull CommandSender sender, @Nonnull TranslationManager tm, @Nonnull String root) {
        sender.sendMessage(ChatUtil.colorize(SEPARATOR));
        sender.sendMessage(ChatUtil.colorize("{#f5c542}" + tm.get("commandsystem.help_title")
                + " {#95a5a6}• {#ffffff}" + tm.get("commandsystem.root_usage", "root", root)));
        sender.sendMessage(ChatUtil.colorize("{#95a5a6}" + tm.get("commandsystem.subcommands_hint")));
        boolean canClaim = canUse(sender, IPlotManager.PERM_CLAIM);
        boolean canInfo = canUse(sender, IPlotManager.PERM_INFO);
        boolean canList = canUse(sender, IPlotManager.PERM_LIST);
        boolean canSpawn = canUse(sender, IPlotManager.PERM_SPAWN);
        boolean canRename = canUse(sender, IPlotManager.PERM_RENAME);
        boolean canDelete = canUse(sender, IPlotManager.PERM_DELETE);
        boolean canMerge = canUse(sender, IPlotManager.PERM_MERGE);
        boolean canUnmerge = canUse(sender, IPlotManager.PERM_UNMERGE);
        boolean canTrust = canUse(sender, IPlotManager.PERM_TRUST);
        boolean canTransfer = canUse(sender, IPlotManager.PERM_TRANSFER);
        boolean canFlag = canUse(sender, IPlotManager.PERM_FLAG);
        boolean canSetSpawn = canUse(sender, IPlotManager.PERM_ADMIN);
        boolean canRefresh = canUse(sender, IPlotManager.PERM_ADMIN);
        boolean canConfirm = canUse(sender, IPlotManager.PERM_BASE);

        if (canClaim || canInfo || canList || canSpawn) {
            sender.sendMessage(ChatUtil.colorize("{#55ffff}"));
            sender.sendMessage(ChatUtil.colorize("{#55ffff}" + tm.get("commandsystem.help_section_basic")));
            if (canClaim) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " claim"));
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " auto"));
            }
            if (canInfo) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " info"));
            }
            if (canList) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " list {#95a5a6}[player]"));
            }
            if (canSpawn) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " spawn {#95a5a6}<name>"));
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " tp {#95a5a6}<name>"));
            }
        }

        if (canRename || canDelete || canMerge || canUnmerge || canSetSpawn || canRefresh) {
            sender.sendMessage(ChatUtil.colorize("{#55ffff}"));
            sender.sendMessage(ChatUtil.colorize("{#55ffff}" + tm.get("commandsystem.help_section_management")));
            if (canRename) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " rename {#95a5a6}<name>"));
            }
            if (canDelete) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " delete {#95a5a6}[gridX] [gridZ]"));
            }
            if (canMerge) {
                sender.sendMessage(ChatUtil.colorize(
                        "{#95a5a6}• {#ffffff}" + root + " merge {#95a5a6}<north|south|east|west|all> [true|false]"));
            }
            if (canUnmerge) {
                sender.sendMessage(ChatUtil.colorize(
                        "{#95a5a6}• {#ffffff}" + root + " unmerge {#95a5a6}<north|south|east|west|all> [true|false]"));
            }
            if (canSetSpawn) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " setspawn {#95a5a6}<world>"));
            }
            if (canRefresh) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " refresh"));
            }
        }

        if (canTrust || canTransfer || canFlag) {
            sender.sendMessage(ChatUtil.colorize("{#55ffff}"));
            sender.sendMessage(ChatUtil.colorize("{#55ffff}" + tm.get("commandsystem.help_section_trust_flags")));
            if (canTrust) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " trust {#95a5a6}<player>"));
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " untrust {#95a5a6}<player>"));
            }
            if (canTransfer) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " transfer {#95a5a6}<player>"));
            }
            if (canFlag) {
                sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " flag {#95a5a6}<set|remove|list> ..."));
            }
        }

        if (canConfirm) {
            sender.sendMessage(ChatUtil.colorize("{#55ffff}"));
            sender.sendMessage(ChatUtil.colorize("{#55ffff}" + tm.get("commandsystem.help_section_confirmation")));
            sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " confirm"));
            sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + root + " cancel"));
        }
        sender.sendMessage(ChatUtil.colorize(SEPARATOR));
    }

    public static void sendSubcommandHelp(@Nonnull CommandSender sender, @Nonnull TranslationManager tm,
            @Nonnull List<String> usageKeys) {
        sender.sendMessage(ChatUtil.colorize(SEPARATOR));
        sender.sendMessage(ChatUtil.colorize("{#f5c542}" + tm.get("commandsystem.usage_header")
                + " {#95a5a6}• {#ffffff}" + tm.get("commandsystem.help_sub_usage_title")));

        boolean first = true;
        for (String key : usageKeys) {
            String line = tm.get(key);
            if (line == null || line.isBlank()) {
                continue;
            }
            String color = first ? "{#55ffff}" : "{#95a5a6}";
            sender.sendMessage(ChatUtil.colorize(color + "• {#ffffff}" + line));
            first = false;
        }

        if (usageKeys.stream().anyMatch(key -> key != null && key.startsWith("flag."))) {
            sender.sendMessage(ChatUtil.colorize(buildFlagsSummaryLine()));
        }

        sender.sendMessage(ChatUtil.colorize("{#95a5a6}" + tm.get("commandsystem.help_tip_root")));
        sender.sendMessage(ChatUtil.colorize(SEPARATOR));
    }

    public static void sendUsage(@Nonnull CommandSender sender, @Nonnull TranslationManager tm, @Nonnull String usageKey,
            @Nonnull String... argumentUsageKeys) {
        sender.sendMessage(ChatUtil.colorize(SEPARATOR));
        sender.sendMessage(ChatUtil.colorize("{#f5c542}" + tm.get("commandsystem.usage_header")
                + " {#95a5a6}• {#ffffff}" + tm.get("commandsystem.help_sub_usage_title")));
        sender.sendMessage(ChatUtil.colorize("{#55ffff}• {#ffffff}" + tm.get(usageKey)));
        for (String key : argumentUsageKeys) {
            sender.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + tm.get(key)));
        }
        if (usageKey.startsWith("flag.") || hasFlagUsageKey(argumentUsageKeys)) {
            sender.sendMessage(ChatUtil.colorize(buildFlagsSummaryLine()));
        }
        sender.sendMessage(ChatUtil.colorize("{#95a5a6}" + tm.get("commandsystem.help_tip_root")));
        sender.sendMessage(ChatUtil.colorize(SEPARATOR));
    }

    public static void sendUsage(@Nonnull PlayerRef player, @Nonnull TranslationManager tm, @Nonnull String usageKey,
            @Nonnull String... argumentUsageKeys) {
        player.sendMessage(ChatUtil.colorize(SEPARATOR));
        player.sendMessage(ChatUtil.colorize("{#f5c542}" + tm.get("commandsystem.usage_header")
                + " {#95a5a6}• {#ffffff}" + tm.get("commandsystem.help_sub_usage_title")));
        player.sendMessage(ChatUtil.colorize("{#55ffff}• {#ffffff}" + tm.get(usageKey)));
        for (String key : argumentUsageKeys) {
            player.sendMessage(ChatUtil.colorize("{#95a5a6}• {#ffffff}" + tm.get(key)));
        }
        if (usageKey.startsWith("flag.") || hasFlagUsageKey(argumentUsageKeys)) {
            player.sendMessage(ChatUtil.colorize(buildFlagsSummaryLine()));
        }
        player.sendMessage(ChatUtil.colorize("{#95a5a6}" + tm.get("commandsystem.help_tip_root")));
        player.sendMessage(ChatUtil.colorize(SEPARATOR));
    }

    private static boolean hasFlagUsageKey(@Nonnull String[] keys) {
        for (String key : keys) {
            if (key != null && key.startsWith("flag.")) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static String buildFlagsSummaryLine() {
        List<String> names = new ArrayList<>();
        for (PlotFlag<?> flag : FlagRegistry.getFlags()) {
            names.add(flag.getName());
        }
        names.sort(Comparator.naturalOrder());
        return "{#95a5a6}• {#ffffff}Flags: {#95a5a6}" + names.stream().collect(Collectors.joining(", "));
    }

    private static boolean canUse(@Nonnull CommandSender sender, @Nonnull String permission) {
        return sender.hasPermission(permission) || sender.hasPermission(IPlotManager.PERM_ADMIN);
    }
}
