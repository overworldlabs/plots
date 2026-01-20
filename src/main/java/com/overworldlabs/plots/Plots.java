package com.overworldlabs.plots;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import com.overworldlabs.plots.command.PlotCommand;
import com.overworldlabs.plots.manager.DataManager;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.manager.WorldManager;
import com.overworldlabs.plots.manager.RadarManager;
import com.overworldlabs.plots.manager.PrefabManager;
import com.overworldlabs.plots.integration.holograms.HologramManager;
import com.overworldlabs.plots.model.PlotConfig;
import com.overworldlabs.plots.system.BreakProtectionSystem;
import com.overworldlabs.plots.system.PlaceProtectionSystem;
import com.overworldlabs.plots.system.PlotNotificationSystem;
import com.overworldlabs.plots.system.UpdateNotificationSystem;
import com.overworldlabs.plots.system.RadarMarkerSystem;
import com.overworldlabs.plots.util.ConsoleColors;
import com.overworldlabs.plots.util.UpdateChecker;
import com.overworldlabs.plots.worldgen.PlotWorldGenProvider;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * Main plugin class for the Plots system
 */
public class Plots extends JavaPlugin {
    private static Plots instance;
    private PlotManager plotManager;
    private DataManager dataManager;
    private WorldManager worldManager;
    private TranslationManager translationManager;
    private RadarManager radarManager;
    private PrefabManager prefabManager;
    private HologramManager hologramManager;

    public Plots(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static Plots getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        ConsoleColors.info("Setting up Plots plugin...");

        // Initialize configuration
        PlotConfig config = loadConfig();

        // Initialize translation manager
        File dataDir = getDataDirectory().toFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        String lang = (String) config.getLanguage();
        translationManager = new TranslationManager(dataDir, lang != null ? lang : "en_us");

        // Initialize managers
        printBanner();

        prefabManager = new PrefabManager(dataDir);
        plotManager = new PlotManager(config);
        plotManager.syncConfigWithPrefabs();

        PlotManager pm = this.plotManager;
        if (pm != null) {
            worldManager = new WorldManager(config);
            radarManager = new RadarManager(pm, worldManager);
            hologramManager = new HologramManager(pm);

            dataManager = new DataManager(dataDir, pm);

            // Load plots from disk
            dataManager.loadPlots();

            // Register the main plot command collection
            getCommandRegistry().registerCommand(new PlotCommand(pm));
        }

        // Register custom world generator provider
        try {
            IWorldGenProvider.CODEC.register(
                    PlotWorldGenProvider.ID,
                    PlotWorldGenProvider.class,
                    PlotWorldGenProvider.CODEC);
            ConsoleColors.success("Registered custom world generator: " + PlotWorldGenProvider.ID);
        } catch (Exception e) {
            ConsoleColors.error("Failed to register world generator: " + e.getMessage());
        }

        // Register protection systems
        var registry = getEntityStoreRegistry();
        registry.registerSystem(new BreakProtectionSystem(plotManager, worldManager));
        registry.registerSystem(new PlaceProtectionSystem(plotManager, worldManager));
        registry.registerSystem(new PlotNotificationSystem(plotManager, worldManager));
        registry.registerSystem(new UpdateNotificationSystem(getVersion()));
        registry.registerSystem(new RadarMarkerSystem(radarManager));

        ConsoleColors.success("Setup complete! Plugin is ready.");

        // Check for updates on startup
        UpdateChecker.checkForUpdates(getVersion()).thenAccept(latestVersion -> {
            if (latestVersion != null && UpdateChecker.isNewerVersion(getVersion(), latestVersion)) {
                ConsoleColors.info("A new version is available: " + latestVersion);
                ConsoleColors.info("Download: https://github.com/overworldlabs/plots/releases");
            } else if (latestVersion != null) {
                ConsoleColors.success("You are running the latest version (" + getVersion() + ")");
            }
        });

        // Clear all existing markers first (removes stale markers from deleted worlds)
        radarManager.clearAllMarkers();

        // Spawn holograms for all existing plots
        hologramManager.spawnAllHologramsForOnlinePlayers();
    }

    public RadarManager getRadarManager() {
        return radarManager;
    }

    @Override
    protected void start() {
        super.start();
        System.out.println("[Plots] Starting Plots...");
        worldManager.createWorldIfNeeded();
    }

    @Override
    protected void shutdown() {
        System.out.println("[Plots] Shutting down...");
        if (dataManager != null) {
            dataManager.savePlots();
        }
        super.shutdown();
        System.out.println("[Plots] Shutdown complete!");
    }

    /**
     * Load configuration from config.json
     */
    private PlotConfig loadConfig() {
        File dataDir = getDataDirectory().toFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File configFile = new File(dataDir, "config.json");
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

        if (!configFile.exists()) {
            PlotConfig defaultConfig = PlotConfig.getDefault();
            try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
                gson.toJson(defaultConfig, writer);
                System.out.println("[Plots] Created default config.json");
            } catch (java.io.IOException e) {
                System.err.println("[Plots] Failed to save default config: " + e.getMessage());
            }
            return defaultConfig;
        }

        try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
            PlotConfig config = gson.fromJson(reader, PlotConfig.class);
            return (config != null) ? config : PlotConfig.getDefault();
        } catch (java.io.IOException e) {
            System.err.println("[Plots] Failed to load config: " + e.getMessage());
            return PlotConfig.getDefault();
        }
    }

    public PlotManager getPlotManager() {
        return plotManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public TranslationManager getTranslationManager() {
        return translationManager;
    }

    public PrefabManager getPrefabManager() {
        return prefabManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    /**
     * Gets the plugin version from the JAR manifest
     */
    private String getVersion() {
        String version = getClass().getPackage().getImplementationVersion();

        if (version == null) {
            throw new RuntimeException("Failed to read plugin version from JAR manifest");
        }

        return version;
    }

    /**
     * Prints the plugin banner on startup
     */
    private void printBanner() {
        String cyan = "\u001B[36m";
        String green = "\u001B[32m";
        String white = "\u001B[37m";
        String reset = "\u001B[0m";

        System.out.println();
        System.out.println(cyan + "  ____  _       _       " + reset);
        System.out.println(cyan + " |  _ \\| |     | |      " + reset);
        System.out.println(cyan + " | |_) | | ___ | |_ ___ " + reset + green + "  Plots v" + getVersion() + reset);
        System.out.println(cyan + " |  __/| |/ _ \\| __/ __|" + reset + white + "  Running on Hytale" + reset);
        System.out.println(cyan + " | |   | | (_) | |_\\__ \\" + reset);
        System.out.println(cyan + " |_|   |_|\\___/ \\__|___/" + reset);
        System.out.println();
    }
}
