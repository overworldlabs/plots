package com.overworldlabs.plots;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.overworldlabs.plots.command.*;
import com.overworldlabs.plots.manager.DataManager;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.manager.WorldManager;
import com.overworldlabs.plots.model.PlotConfig;
import com.overworldlabs.plots.util.ConsoleColors;

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
    private com.overworldlabs.plots.manager.RadarManager radarManager;
    private com.overworldlabs.plots.manager.PrefabManager prefabManager;
    private com.overworldlabs.plots.integration.BuilderToolsIntegration builderToolsIntegration;
    private com.overworldlabs.plots.integration.BuilderToolsSystem builderToolsSystem;

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
        translationManager = new TranslationManager(config.getLanguage());

        // Initialize managers
        prefabManager = new com.overworldlabs.plots.manager.PrefabManager();
        plotManager = new PlotManager(config);
        plotManager.syncConfigWithPrefabs();

        worldManager = new WorldManager(config);
        radarManager = new com.overworldlabs.plots.manager.RadarManager(plotManager, worldManager);

        // Register custom world generator provider
        try {
            com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider.CODEC.register(
                    com.overworldlabs.plots.worldgen.PlotWorldGenProvider.ID,
                    com.overworldlabs.plots.worldgen.PlotWorldGenProvider.class,
                    com.overworldlabs.plots.worldgen.PlotWorldGenProvider.CODEC);
            ConsoleColors.success("Registered custom world generator: "
                    + com.overworldlabs.plots.worldgen.PlotWorldGenProvider.ID);
        } catch (Exception e) {
            ConsoleColors.error("Failed to register world generator: " + e.getMessage());
        }

        // Initialize BuilderTools Integration
        try {
            Class.forName("com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin");
            builderToolsIntegration = new com.overworldlabs.plots.integration.BuilderToolsIntegration();
            builderToolsIntegration.initialize();

            builderToolsSystem = new com.overworldlabs.plots.integration.BuilderToolsSystem(worldManager,
                    builderToolsIntegration);
            getEntityStoreRegistry().registerSystem(builderToolsSystem);
            ConsoleColors.success("BuilderTools integration enabled.");
        } catch (ClassNotFoundException e) {
            ConsoleColors.info("BuilderTools not found, skipping integration.");
        } catch (Exception e) {
            ConsoleColors.error("Error initializing BuilderTools integration: " + e.getMessage());
        }

        File dataDirectory = getDataDirectory().toFile();
        dataManager = new DataManager(dataDirectory, plotManager);

        // Load plots from disk
        dataManager.loadPlots();

        // Register the main plot command collection
        getCommandRegistry().registerCommand(new PlotCommand(plotManager));

        // Register protection systems
        getEntityStoreRegistry()
                .registerSystem(new com.overworldlabs.plots.listener.BreakProtectionSystem(plotManager, worldManager));
        getEntityStoreRegistry()
                .registerSystem(new com.overworldlabs.plots.listener.PlotNotificationSystem(plotManager, worldManager));

        // Register Prefab Paste Protection
        getEntityStoreRegistry().registerSystem(
                new com.overworldlabs.plots.listener.PrefabPasteProtectionSystem(plotManager, worldManager));

        ConsoleColors.success("Setup complete! Plugin is ready.");
    }

    public com.overworldlabs.plots.manager.RadarManager getRadarManager() {
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
        if (builderToolsIntegration != null) {
            builderToolsIntegration.shutdown();
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

    public com.overworldlabs.plots.manager.PrefabManager getPrefabManager() {
        return prefabManager;
    }
}
