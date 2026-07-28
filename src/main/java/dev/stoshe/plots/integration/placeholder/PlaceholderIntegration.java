package dev.stoshe.plots.integration.placeholder;

import com.overworldlabs.placeholder.api.PlaceholderAPI;
import dev.stoshe.plots.util.ConsoleColors;

/**
 * Optional integration with the PlaceholderAPI plugin. All references to the
 * PlaceholderAPI live here so the rest of the plugin never touches those classes
 * directly; the caller guards invocation with a {@code try/catch (Throwable)} so
 * a missing PlaceholderAPI simply disables the integration.
 */
public final class PlaceholderIntegration {

    private PlaceholderIntegration() {
    }

    /**
     * Registers the {@code plots} expansion and a reload listener so the
     * expansion is re-registered if the PlaceholderAPI loads after this plugin
     * or reloads later.
     */
    public static void register() {
        PlotsPlaceholderExpansion expansion = new PlotsPlaceholderExpansion();
        expansion.register();
        PlaceholderAPI.registerListener(manager -> expansion.register());
        ConsoleColors.success("Registered PlaceholderAPI expansion: %plots_...%");
    }
}
