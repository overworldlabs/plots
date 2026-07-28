package dev.stoshe.plots.flag;
import dev.stoshe.plots.flag.types.BooleanFlag;
import dev.stoshe.plots.flag.types.CsvStringFlag;
import dev.stoshe.plots.flag.types.StringFlag;
import dev.stoshe.plots.flag.types.WeatherAssetFlag;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for all available plot flags.
 */
public class FlagRegistry {
        private static final Map<String, PlotFlag<?>> FLAGS = new ConcurrentHashMap<>();
        private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();

        // Flag Definitions
        public static final BooleanFlag BLOCK_BREAK = register(
                        new BooleanFlag("block-break", "Allow block breaking", true));
        public static final BooleanFlag BLOCK_PLACE = register(
                        new BooleanFlag("block-place", "Allow block placement", true));
        public static final BooleanFlag INTERACT = register(
                        new BooleanFlag("interact", "Allow block interaction", true));
        public static final BooleanFlag USE = register(
                        new BooleanFlag("use", "Allow using interactions", true));
        public static final BooleanFlag BUILD = register(
                        new BooleanFlag("build", "Allow trusted players to use BuilderTools", true));
        public static final BooleanFlag HAMMER = register(
                        new BooleanFlag("hammer", "Allow builder tools heavy operations", true));
        public static final BooleanFlag EXPLOSIONS = register(
                        new BooleanFlag("explosions", "Allow server-side explosion damage", false));
        public static final BooleanFlag DAMAGE = register(
                        new BooleanFlag("damage", "Allow generic damage to players", true));
        public static final BooleanFlag FALL_DAMAGE = register(
                        new BooleanFlag("fall-damage", "Allow fall damage", true));
        public static final BooleanFlag ENTRY = register(
                        new BooleanFlag("entry", "Allow entering this plot", true));
        public static final BooleanFlag EXIT = register(
                        new BooleanFlag("exit", "Allow leaving this plot", true));
        public static final BooleanFlag ITEM_DROP = register(
                        new BooleanFlag("item-drop", "Allow dropping items", true));
        public static final BooleanFlag ITEM_PICKUP = register(
                        new BooleanFlag("item-pickup", "Allow picking up items", true));
        public static final BooleanFlag ITEM_PICKUP_MANUAL = register(
                        new BooleanFlag("item-pickup-manual", "Allow manual pickup interaction (F-key)", true));
        public static final BooleanFlag MOB_DAMAGE = register(
                        new BooleanFlag("mob-damage", "Allow mobs to damage players", true));
        public static final BooleanFlag SEAT = register(
                        new BooleanFlag("seat", "Allow sitting interactions", true));
        public static final BooleanFlag MOB_SPAWNING = register(
                        new BooleanFlag("mob-spawning", "Allow mob spawning", true));
        public static final BooleanFlag VISIT = register(
                        new BooleanFlag("visit", "List this plot's warps publicly and allow visitors to use them", true));
        public static final BooleanFlag KEEP_INVENTORY = register(
                        new BooleanFlag("keep-inventory", "Keep inventory on death", false));
        public static final BooleanFlag INVINCIBLE_ITEMS = register(
                        new BooleanFlag("invincible-items", "Prevent item durability loss", false));

        public static final StringFlag GREET_MESSAGE = register(
                        new StringFlag("greet-message", "Message shown when entering", ""));
        public static final StringFlag FAREWELL_MESSAGE = register(
                        new StringFlag("farewell-message", "Message shown when leaving", ""));
        public static final StringFlag DENY_MESSAGE = register(
                        new StringFlag("deny-message", "Custom deny message for this plot", ""));
        public static final CsvStringFlag ALLOWED_COMMANDS = register(
                        new CsvStringFlag("allowed-cmds", "Comma-separated list of allowed slash commands", ""));
        public static final CsvStringFlag BLOCKED_COMMANDS = register(
                        new CsvStringFlag("blocked-cmds", "Comma-separated list of blocked slash commands", ""));

        public static final BooleanFlag PLAYER_CHAT = register(
                        new BooleanFlag("player-chat", "Allow players to send chat messages", true));
        public static final BooleanFlag CRAFTING = register(
                        new BooleanFlag("crafting", "Allow crafting at crafting stations", true));
        public static final BooleanFlag PVP = register(
                        new BooleanFlag("pvp", "Allow player vs player combat", false));
        public static final WeatherAssetFlag WEATHER = register(
                        new WeatherAssetFlag("weather", "Weather asset id for this plot (from /weather list)", "Default_Flat"));

        static {
                registerAlias("greeting-message", GREET_MESSAGE.getName());
                registerAlias("greeting_message", GREET_MESSAGE.getName());
                registerAlias("greet_message", GREET_MESSAGE.getName());
                registerAlias("farewell-message", FAREWELL_MESSAGE.getName());
                registerAlias("farewell_message", FAREWELL_MESSAGE.getName());
        }

        /**
         * Registers a new flag.
         *
         * @param flag The flag to register
         * @param <T>  The flag type
         * @return The registered flag
         */
        public static <T extends PlotFlag<?>> T register(@Nonnull T flag) {
                FLAGS.put(flag.getName(), flag);
                return flag;
        }

        /**
         * Gets a flag by its name.
         *
         * @param name The flag name
         * @return The flag, or null if not found
         */
        public static PlotFlag<?> getFlag(@Nonnull String name) {
                String normalized = name.toLowerCase();
                String canonical = ALIASES.getOrDefault(normalized, normalized);
                return FLAGS.get(canonical);
        }

        public static void registerAlias(@Nonnull String alias, @Nonnull String canonicalName) {
                ALIASES.put(alias.toLowerCase(), canonicalName.toLowerCase());
        }

        /**
         * Gets all registered flags.
         *
         * @return A collection of all flags
         */
        public static Collection<PlotFlag<?>> getFlags() {
                return Collections.unmodifiableCollection(FLAGS.values());
        }
}
