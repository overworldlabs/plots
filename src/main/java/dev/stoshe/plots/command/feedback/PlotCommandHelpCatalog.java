package dev.stoshe.plots.command.feedback;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlotCommandHelpCatalog {
    private static final Map<String, String[]> SUBCOMMAND_USAGE_KEYS = createSubcommandUsageMap();

    private PlotCommandHelpCatalog() {
    }

    @Nonnull
    public static Map<String, String[]> subcommandUsageKeys() {
        return SUBCOMMAND_USAGE_KEYS;
    }

    @Nonnull
    public static List<String> usageKeysFor(@Nonnull List<String> commandTokens) {
        if (commandTokens.isEmpty()) {
            return Collections.emptyList();
        }

        int maxPrefix = commandTokens.size();
        for (int prefix = maxPrefix; prefix >= 1; prefix--) {
            String key = String.join(" ", commandTokens.subList(0, prefix));
            String[] usageKeys = SUBCOMMAND_USAGE_KEYS.get(key);
            if (usageKeys == null || usageKeys.length == 0) {
                continue;
            }

            List<String> result = new ArrayList<>(usageKeys.length);
            for (String usageKey : usageKeys) {
                if (usageKey != null && !usageKey.isBlank()) {
                    result.add(usageKey);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        return Collections.emptyList();
    }

    private static Map<String, String[]> createSubcommandUsageMap() {
        Map<String, String[]> map = new HashMap<>();

        map.put("flag", new String[] { "flag.usage", "flag.usage_set", "flag.usage_remove", "flag.usage_list" });
        map.put("f", new String[] { "flag.usage", "flag.usage_set", "flag.usage_remove", "flag.usage_list" });
        map.put("flag set", new String[] { "flag.usage_set" });
        map.put("flag remove", new String[] { "flag.usage_remove" });
        map.put("flag list", new String[] { "flag.usage_list" });
        map.put("f set", new String[] { "flag.usage_set" });
        map.put("f remove", new String[] { "flag.usage_remove" });
        map.put("f list", new String[] { "flag.usage_list" });
        map.put("merge", new String[] { "merge.usage" });
        map.put("link", new String[] { "merge.usage" });
        map.put("unmerge", new String[] { "unmerge.usage" });
        map.put("unlink", new String[] { "unmerge.usage" });
        map.put("trust", new String[] { "trust.usage" });
        map.put("untrust", new String[] { "trust.usage_untrust" });
        map.put("transfer", new String[] { "transfer.usage" });
        map.put("rename", new String[] { "rename.usage" });
        map.put("delete", new String[] { "delete.usage" });
        map.put("confirm", new String[] { "confirm.usage_confirm", "confirm.usage_cancel" });
        map.put("cancel", new String[] { "confirm.usage_cancel", "confirm.usage_confirm" });
        map.put("refresh", new String[] { "refresh.usage" });
        map.put("tp", new String[] { "teleport.usage" });
        map.put("spawn", new String[] { "teleport.usage" });
        map.put("claim", new String[] { "commandsystem.root_usage", "commandsystem.subcommands_hint" });
        map.put("auto", new String[] { "commandsystem.root_usage", "commandsystem.subcommands_hint" });

        return map;
    }
}
