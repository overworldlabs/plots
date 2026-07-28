package dev.stoshe.plots.command;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.flag.PlotFlag;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dynamic suggestion sources for {@link SuggestingStringArgumentType}. Each
 * method is evaluated lazily, every time the engine asks for completions, so the
 * suggestions always reflect the current server state (online players, configured
 * worlds, registered flags). All lookups are best-effort and return an empty list
 * if the relevant subsystem is not ready yet.
 */
public final class CommandSuggestions {
    private CommandSuggestions() {
    }

    /** Names of the players currently online. */
    @Nonnull
    public static List<String> onlinePlayerNames() {
        try {
            List<String> names = new ArrayList<>();
            for (PlayerRef player : Universe.get().getPlayers()) {
                if (player == null) {
                    continue;
                }
                String name = player.getUsername();
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
            return names;
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    /** Names of the configured plot worlds. */
    @Nonnull
    public static List<String> plotWorldNames() {
        try {
            return new ArrayList<>(Plots.getInstance().getConfig().getWorldNames());
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    /** Names of all registered plot flags. */
    @Nonnull
    public static List<String> flagNames() {
        try {
            List<String> names = new ArrayList<>();
            for (PlotFlag<?> flag : FlagRegistry.getFlags()) {
                if (flag != null && flag.getName() != null) {
                    names.add(flag.getName());
                }
            }
            return names;
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }
}
