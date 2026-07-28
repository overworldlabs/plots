package dev.stoshe.plots.util;

import dev.stoshe.plots.Plots;
import dev.stoshe.plots.manager.TranslationManager;

/**
 * Tiny convenience facade over {@link TranslationManager} for UI code.
 *
 * <p>Translations are server-wide (driven by the configured language); there is
 * no per-player locale. A missing key resolves to the key itself, so callers can
 * reference keys before they exist in the lang files without crashing.
 */
public final class Tr {

    private Tr() {
    }

    /** Resolves a translation key to the active language, falling back to the key. */
    public static String t(String key) {
        if (key == null) {
            return "";
        }
        try {
            TranslationManager tm = Plots.getInstance().getTranslationManager();
            return tm != null ? tm.get(key) : key;
        } catch (Exception ex) {
            return key;
        }
    }

    /** Resolves a key and substitutes {@code %name%} placeholders (name, value pairs). */
    public static String t(String key, Object... args) {
        if (key == null) {
            return "";
        }
        try {
            TranslationManager tm = Plots.getInstance().getTranslationManager();
            return tm != null ? tm.get(key, args) : key;
        } catch (Exception ex) {
            return key;
        }
    }
}
