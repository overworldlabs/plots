package dev.stoshe.plots.command.sub;

import dev.stoshe.plots.util.CommandSenderIdentity;
import dev.stoshe.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

import org.joml.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.command.CommandArgs;
import dev.stoshe.plots.command.CommandSuggestions;
import dev.stoshe.plots.command.PlotConfirmationService;
import dev.stoshe.plots.command.SuggestingStringArgumentType;
import dev.stoshe.plots.command.feedback.CommandFeedbackService;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.flag.MixinRequiredFlags;
import dev.stoshe.plots.flag.PlotFlag;
import dev.stoshe.plots.flag.types.BooleanFlag;
import dev.stoshe.plots.flag.types.IntegerFlag;
import dev.stoshe.plots.flag.types.StringFlag;
import dev.stoshe.plots.flag.types.WeatherAssetFlag;
import dev.stoshe.plots.integration.mixin.MixinBridgeStatus;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.manager.PlotManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Command: /plot flag <set|remove|list> [flag] [value]
 * Manages plot-specific flags.
 */
public class PlotFlagCommand extends CommandBase {
    private final IPlotManager plotManager;
    private final RequiredArg<String> actionArg;

    public PlotFlagCommand(@Nonnull IPlotManager plotManager) {
        super("flag", "Manage plot flags");
        addAliases("f");
        setAllowsExtraArguments(true);
        this.plotManager = plotManager;
        this.actionArg = CommandArgs.required(this, "action", "set, remove, or list",
                SuggestingStringArgumentType.of("action", "set, remove, or list", "set", "remove", "list"));
        // Declared only to drive tab-completion for "/plot flag set <flag> <value>".
        // The actual flag name and value are still read from the raw input by the
        // handlers below (parseSetInput/extractSetValue); these args parse their raw
        // token untouched and a missing token simply resolves to null (like actionArg
        // above), so "/plot flag list" and "/plot flag remove <flag>" keep working.
        // They must be REQUIRED (positional), not optional: the command framework
        // renders optional args as "--flag=" / "--value=" in autocomplete, whereas
        // required args suggest the actual flag names and values positionally.
        CommandArgs.required(this, "flag", "Flag name",
                new SuggestingStringArgumentType("flag", "Flag name", CommandSuggestions::flagNames));
        CommandArgs.required(this, "value", "New value (true/false for toggle flags)",
                SuggestingStringArgumentType.of("value", "New value", "true", "false"));
        requirePermission(IPlotManager.PERM_FLAG);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(),
                    IPlotManager.PERM_FLAG);
        }

        if (!context.isPlayer()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.only_players")));
            return;
        }

        Ref<EntityStore> ref = context
                .senderAsPlayerRef();
        if (ref == null)
            return;

        java.util.UUID senderUuid = CommandSenderIdentity.uuid(context.sender());
        if (senderUuid == null)
            return;

        PlayerRef playerObj = Universe.get().getPlayer(senderUuid);
        if (playerObj == null)
            return;

        java.util.UUID worldUuid = playerObj.getWorldUuid();
        if (worldUuid == null)
            return;

        World currentWorld = Universe.get().getWorld(worldUuid);
        if (currentWorld == null)
            return;

        currentWorld.execute(() -> {
            Store<EntityStore> store = ref.getStore();
            PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
            if (player == null)
                return;

            String worldName = currentWorld.getName();
            Vector3d pos = player.getTransform().getPosition();
            Plot plot = plotManager.getPlotAt(worldName, (int) pos.x, (int) pos.z);

            if (plot == null) {
                player.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            // Ownership check (Admins bypass)
            if (!plot.getOwner().equals(PlayerIdentity.uuid(player))
                    && !PermissionUtil.hasAdminPermission(context.sender())) {
                player.sendMessage(ChatUtil.error(tm.get("management.no_permission")));
                return;
            }

            String actionRaw = actionArg.get(context);
            if (actionRaw == null || actionRaw.trim().isEmpty()) {
                CommandFeedbackService.sendUsage(player, tm, "flag.usage", "flag.usage_set", "flag.usage_remove",
                        "flag.usage_list");
                return;
            }

            String action = actionRaw.trim().toLowerCase(Locale.ROOT);
            switch (action) {
                case "set":
                    handleSet(context, currentWorld, plot, player, tm);
                    break;
                case "remove":
                    handleRemove(context, currentWorld, plot, player, tm);
                    break;
                case "list":
                    handleList(plot, player, tm);
                    break;
                default:
                    CommandFeedbackService.sendInvalidSubcommand(player, tm, "/plot flag " + action, "flag.usage",
                            "flag.usage_set", "flag.usage_remove", "flag.usage_list");
                    break;
            }
        });
    }

    private void handleSet(CommandContext context, World world, Plot plot, PlayerRef player, TranslationManager tm) {
        ParsedFlagSetInput parsedInput = parseSetInput(context);
        String flagName = parsedInput.flagName;
        String flagValueStr = parsedInput.flagValue != null ? parsedInput.flagValue : extractSetValue(context, flagName);

        if (flagName == null || flagValueStr == null) {
            CommandFeedbackService.sendUsage(player, tm, "flag.usage_set");
            return;
        }

        PlotFlag<?> flag = FlagRegistry.getFlag(flagName);
        if (flag == null) {
            player.sendMessage(ChatUtil.error(tm.get("flag.unknown", "flag", flagName)));
            return;
        }

        if (MixinRequiredFlags.requiresMixinBridge(flag) && !MixinBridgeStatus.isReadyForMixinFlags()) {
            player.sendMessage(ChatUtil.error(tm.get("flag.requires_mixin_bridge", "flag", flag.getName())));
            return;
        }

        Optional<?> parsedValue = flag.parseValue(flagValueStr);
        if (parsedValue.isPresent()) {
            List<Plot> targets = resolveFlagTargets(plot);
            Object value = parsedValue.get();
            if (targets.size() > 1 && (hasMixedValues(targets, flag, value) || hasMixedPermissions(targets))) {
                PlotConfirmationService.getInstance().request(context.sender(), tm,
                        tm.get("confirm.action_flag_apply_merged",
                                "flag", flag.getName(),
                                "value", flagValueStr,
                                "count", String.valueOf(targets.size())),
                        () -> world.execute(() -> applySetFlag(targets, flag, value, flagValueStr, player, tm)));
                return;
            }

            applySetFlag(targets, flag, value, flagValueStr, player, tm);
        } else {
            String expected = resolveExpectedValueHint(flag, tm);
            player.sendMessage(ChatUtil.error(
                    tm.get("flag.invalid_value_with_hint",
                            "flag", flag.getName(),
                            "value", flagValueStr,
                            "expected", expected)));
        }
    }

    private String resolveExpectedValueHint(@Nonnull PlotFlag<?> flag, @Nonnull TranslationManager tm) {
        if (flag instanceof WeatherAssetFlag) {
            return ((WeatherAssetFlag) flag).buildAllowedValuesHint(20);
        }

        String specificKey = "flag.value_hint." + flag.getName().replace('-', '_');
        String specific = tm.get(specificKey);
        if (!specificKey.equals(specific)) {
            return specific;
        }

        if (flag instanceof BooleanFlag) {
            return tm.get("flag.value_hint.boolean");
        }
        if (flag instanceof IntegerFlag) {
            return tm.get("flag.value_hint.integer");
        }
        if (flag instanceof StringFlag) {
            return tm.get("flag.value_hint.string");
        }
        return tm.get("flag.value_hint.generic");
    }

    private String extractSetValue(@Nonnull CommandContext context, String flagName) {
        String raw = context.getInputString();
        if (raw == null || raw.isBlank() || flagName == null || flagName.isBlank()) {
            return null;
        }

        List<String> parts = tokenizeCommandInput(raw);
        if (parts.isEmpty()) {
            return null;
        }
        int actionIdx = findActionTokenIndex(parts, "set");
        if (actionIdx < 0 || actionIdx + 2 >= parts.size()) {
            return null;
        }

        String rawValue = String.join(" ", parts.subList(actionIdx + 2, parts.size()));
        return normalizeFlagValue(rawValue);
    }

    private ParsedFlagSetInput parseSetInput(@Nonnull CommandContext context) {
        String raw = context.getInputString();
        if (raw == null || raw.isBlank()) {
            return ParsedFlagSetInput.empty();
        }
        List<String> parts = tokenizeCommandInput(raw);
        if (parts.isEmpty()) {
            return ParsedFlagSetInput.empty();
        }

        int actionIdx = findActionTokenIndex(parts, "set");
        if (actionIdx < 0 || actionIdx + 1 >= parts.size()) {
            return ParsedFlagSetInput.empty();
        }

        String parsedFlagName = parts.get(actionIdx + 1);
        String parsedFlagValue = null;
        if (actionIdx + 2 < parts.size()) {
            parsedFlagValue = String.join(" ", parts.subList(actionIdx + 2, parts.size()));
        }
        return new ParsedFlagSetInput(normalizeFlagValue(parsedFlagName), normalizeFlagValue(parsedFlagValue));
    }

    private List<String> tokenizeCommandInput(@Nonnull String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.isEmpty()) {
            return List.of();
        }

        String[] split = trimmed.split("\\s+");
        List<String> tokens = new ArrayList<>(split.length);
        for (String token : split) {
            if (token != null && !token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private int findActionTokenIndex(@Nonnull List<String> tokens, @Nonnull String action) {
        String target = action.toLowerCase(Locale.ROOT);
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase(target)) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeFlagValue(String joined) {
        if (joined == null) {
            return null;
        }
        joined = joined.trim();
        if (joined.isEmpty()) {
            return null;
        }
        if ((joined.startsWith("\"") && joined.endsWith("\""))
                || (joined.startsWith("'") && joined.endsWith("'"))) {
            joined = joined.substring(1, joined.length() - 1).trim();
        }
        return joined.isEmpty() ? null : joined;
    }

    private static final class ParsedFlagSetInput {
        private final String flagName;
        private final String flagValue;

        private ParsedFlagSetInput(String flagName, String flagValue) {
            this.flagName = flagName;
            this.flagValue = flagValue;
        }

        private static ParsedFlagSetInput empty() {
            return new ParsedFlagSetInput(null, null);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void setFlagHelper(Plot plot, PlotFlag<T> flag, Object value) {
        plot.setFlagValue(flag, (T) value);
    }

    private void handleRemove(CommandContext context, World world, Plot plot, PlayerRef player, TranslationManager tm) {
        String flagName = parseRemoveFlagName(context);
        if (flagName == null) {
            CommandFeedbackService.sendUsage(player, tm, "flag.usage_remove");
            return;
        }

        PlotFlag<?> flag = FlagRegistry.getFlag(flagName);
        if (flag == null) {
            player.sendMessage(ChatUtil.error(tm.get("flag.unknown", "flag", flagName)));
            return;
        }

        List<Plot> targets = resolveFlagTargets(plot);
        if (targets.size() > 1 && (hasAnyCustomValue(targets, flag) || hasMixedPermissions(targets))) {
            PlotConfirmationService.getInstance().request(context.sender(), tm,
                    tm.get("confirm.action_flag_remove_merged",
                            "flag", flag.getName(),
                            "count", String.valueOf(targets.size())),
                    () -> world.execute(() -> applyRemoveFlag(targets, flag, player, tm)));
            return;
        }

        applyRemoveFlag(targets, flag, player, tm);
    }

    private void applySetFlag(@Nonnull List<Plot> targets, @Nonnull PlotFlag<?> flag, @Nonnull Object value,
            @Nonnull String originalValue, @Nonnull PlayerRef player, @Nonnull TranslationManager tm) {
        for (Plot target : targets) {
            setFlagHelper(target, flag, value);
        }
        player.sendMessage(ChatUtil.success(tm.get("flag.set", "flag", flag.getName(), "value", originalValue)));
        plotManager.savePlots();
    }

    private void applyRemoveFlag(@Nonnull List<Plot> targets, @Nonnull PlotFlag<?> flag, @Nonnull PlayerRef player,
            @Nonnull TranslationManager tm) {
        for (Plot target : targets) {
            target.removeFlag(flag);
        }
        player.sendMessage(ChatUtil.success(tm.get("flag.removed", "flag", flag.getName())));
        plotManager.savePlots();
    }

    private List<Plot> resolveFlagTargets(@Nonnull Plot plot) {
        if (plotManager instanceof PlotManager pm) {
            Plot canonical = pm.getCanonicalPlot(plot);
            List<Plot> merged = pm.getMergedComponent(canonical);
            if (merged != null && !merged.isEmpty()) {
                return merged;
            }
        }
        return List.of(plot);
    }

    private boolean hasMixedValues(@Nonnull List<Plot> targets, @Nonnull PlotFlag<?> flag, @Nonnull Object targetValue) {
        for (Plot plot : targets) {
            Object current = plot.getFlagValue(flag);
            if (!targetValue.equals(current)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyCustomValue(@Nonnull List<Plot> targets, @Nonnull PlotFlag<?> flag) {
        Object defaultValue = flag.getDefaultValue();
        for (Plot plot : targets) {
            Object current = plot.getFlagValue(flag);
            if (!defaultValue.equals(current)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMixedPermissions(@Nonnull List<Plot> targets) {
        if (targets.isEmpty()) {
            return false;
        }
        Plot first = targets.get(0);
        List<java.util.UUID> baseline = first.getTrustedPlayers();
        baseline.sort(java.util.Comparator.naturalOrder());

        for (int i = 1; i < targets.size(); i++) {
            Plot current = targets.get(i);
            if (!first.getOwner().equals(current.getOwner())) {
                return true;
            }
            List<java.util.UUID> currentTrusted = current.getTrustedPlayers();
            currentTrusted.sort(java.util.Comparator.naturalOrder());
            if (!baseline.equals(currentTrusted)) {
                return true;
            }
        }
        return false;
    }

    private String parseRemoveFlagName(@Nonnull CommandContext context) {
        String raw = context.getInputString();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        List<String> parts = tokenizeCommandInput(raw);
        if (parts.isEmpty()) {
            return null;
        }
        int actionIdx = findActionTokenIndex(parts, "remove");
        if (actionIdx < 0 || actionIdx + 1 >= parts.size()) {
            return null;
        }
        return normalizeFlagValue(parts.get(actionIdx + 1));
    }

    private void handleList(Plot plot, PlayerRef player, TranslationManager tm) {
        List<PlotFlag<?>> flags = new ArrayList<>(FlagRegistry.getFlags());
        flags.sort(Comparator.comparing(flag -> flag.getName()));

        StringBuilder content = new StringBuilder();
        content.append(tm.get("flag.list_header"));
        for (PlotFlag<?> flag : flags) {
            Object current = plot.getFlagValue(flag);
            String status = current.equals(flag.getDefaultValue()) ? "(Default)" : "(Custom)";
            content.append("\n")
                    .append(tm.get("flag.list_item", "flag", flag.getName(), "value", String.valueOf(current),
                            "status", status));
        }
        content.append("\n").append(tm.get("flag.list_footer"));
        player.sendMessage(ChatUtil.info(content.toString()));
    }
}
