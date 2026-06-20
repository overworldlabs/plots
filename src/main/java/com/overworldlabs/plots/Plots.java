package com.overworldlabs.plots;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.api.IPlotRepository;
import com.overworldlabs.plots.api.IRadarManager;
import com.overworldlabs.plots.api.IServiceRegistry;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.api.PlotsAPI;
import com.overworldlabs.plots.api.impl.PlotsAPIImpl;
import com.overworldlabs.plots.command.PlotCommand;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.core.ServiceRegistryImpl;
import com.overworldlabs.plots.data.JsonPlotRepository;
import com.overworldlabs.plots.data.SqlPlotRepository;
import com.overworldlabs.plots.integration.buildertools.BuilderToolsIntegration;
import com.overworldlabs.plots.integration.economy.PlotEconomyService;
import com.overworldlabs.plots.integration.holograms.HologramManager;
import com.overworldlabs.plots.integration.mixin.MixinBridgeStatus;
import com.overworldlabs.plots.integration.mixin.PlotsMixinsCompatibility;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.PrefabManager;
import com.overworldlabs.plots.manager.RadarManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.manager.WorldManager;
import com.overworldlabs.plots.system.BreakProtectionSystem;
import com.overworldlabs.plots.system.BuilderToolsMaskSystem;
import com.overworldlabs.plots.system.BuilderToolsPacketInterceptor;
import com.overworldlabs.plots.system.DamageBlockProtectionSystem;
import com.overworldlabs.plots.system.PlaceProtectionSystem;
import com.overworldlabs.plots.system.PlotCraftingSystem;
import com.overworldlabs.plots.system.PlotDamageFlagSystem;
import com.overworldlabs.plots.system.PlotItemDropSystem;
import com.overworldlabs.plots.system.PlotItemPickupSystem;
import com.overworldlabs.plots.system.PlotMobProtectionSystem;
import com.overworldlabs.plots.system.PlotNotificationSystem;
import com.overworldlabs.plots.system.RadarMarkerSystem;
import com.overworldlabs.plots.system.ServerBlockProtectionSystem;
import com.overworldlabs.plots.system.UpdateNotificationSystem;
import com.overworldlabs.plots.util.ConsoleColors;
import com.overworldlabs.plots.util.UpdateChecker;
import com.overworldlabs.plots.worldgen.PlotWorldGenProvider;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;


import javax.annotation.Nonnull;
import java.io.File;
import java.util.logging.Logger;

/**
 * Main plugin class for the Plots system
 */
public class Plots extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("Plots");
    private static Plots instance;
    private static PlotsAPI api;
    private static volatile boolean started;

    private IServiceRegistry serviceRegistry;
    private PlotEconomyService economyService;
    private com.overworldlabs.plots.menu.PlotMenuKeybindManager menuKeybindManager;

    public Plots(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static Plots getInstance() {
        return instance;
    }

    public IServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public PlotEconomyService getEconomyService() {
        return economyService;
    }

    /**
     * Get the public API for external plugins
     * 
     * @return The Plots API instance
     */
    public static PlotsAPI getAPI() {
        return api;
    }

    public static boolean enabled() {
        Plots plugin = getInstance();
        return plugin != null
                && plugin.getServiceRegistry() != null
                && api != null
                && started;
    }

    @Override
    protected void setup() {
        super.setup();
        ConsoleColors.info("Setting up Plots plugin...");

        serviceRegistry = new ServiceRegistryImpl();
        PlotConfig config = loadConfig();
        File dataDir = getDataDirectory().toFile();

        economyService = new PlotEconomyService(config);
        economyService.initialize();
        serviceRegistry.register(PlotEconomyService.class, economyService);

        initializeTranslationManager(dataDir, config);
        printBanner();
        initializeManagers(dataDir, config);
        registerWorldGenerator();
        registerSystems();

        // Basic protection (block break/place/interact, containers, mobs, item
        // drop/pickup, etc.) is always enforced by the ECS protection systems
        // registered above — it does not require any bridge. The TaleGuard bridge
        // adds the mixin-only flags (item-pickup, builder tools, mob-spawning,
        // keep-inventory, commands, ...). Plots prefers TaleGuard when present and
        // gracefully falls back to basic protection when it is absent.
        if (MixinBridgeStatus.isReadyForMixinFlags()) {
            if (MixinBridgeStatus.isMixinsLoaded()) {
                ConsoleColors.success("TaleGuard bridge active — full protection coverage enabled.");
            } else {
                ConsoleColors.info("TaleGuard bridge bootstrap is ready. Mixins will be applied as target classes load.");
            }
        } else if (MixinBridgeStatus.isActive()) {
            ConsoleColors.warning("Protection bridge detected, but bootstrap is not ready — running with basic protection only.");
        } else {
            ConsoleColors.warning("No protection bridge detected — running with BASIC protection (ECS) only.");
            ConsoleColors.warning("Mixin-required flags are disabled. Install TaleGuard for full protection coverage.");
        }
        // Keep the legacy plots-native registry populated (harmless if unused) and
        // register the adapter hook with the shared TaleGuard bridge registry.
        PlotsMixinsCompatibility.register(getPlotManager());
        PlotsMixinsCompatibility.registerTaleGuard(getPlotManager());

        ConsoleColors.success("Setup complete! Plugin is ready.");

        checkForUpdates();
    }

    /**
     * Initialize the translation manager
     */
    private void initializeTranslationManager(@Nonnull File dataDir, @Nonnull PlotConfig config) {
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        String lang = (String) config.getLanguage();
        TranslationManager translationManager = new TranslationManager(dataDir, lang != null ? lang : "en_us");
        serviceRegistry.register(TranslationManager.class, translationManager);
    }

    /**
     * Initialize all plugin managers
     */
    private void initializeManagers(@Nonnull File dataDir, @Nonnull PlotConfig config) {
        PrefabManager prefabManager = new PrefabManager(dataDir);
        serviceRegistry.register(PrefabManager.class, prefabManager);

        IPlotRepository repository = createRepository(dataDir, config);
        serviceRegistry.register(IPlotRepository.class, repository);

        PlotManager plotManager = new PlotManager(config, repository);
        plotManager.syncConfigWithPrefabs();
        serviceRegistry.register(IPlotManager.class, plotManager);

        WorldManager worldManager = new WorldManager(config);
        serviceRegistry.register(IWorldManager.class, worldManager);

        RadarManager radarManager = new RadarManager(plotManager, worldManager);
        serviceRegistry.register(IRadarManager.class, radarManager);

        HologramManager hologramManager = new HologramManager(plotManager);
        serviceRegistry.register(HologramManager.class, hologramManager);

        api = new PlotsAPIImpl(plotManager, worldManager);

        plotManager.loadPlots();

        PlotCommand plotCommand = new PlotCommand("plot", plotManager);
        plotCommand.addAliases("plots", "plotme", "p");
        getCommandRegistry().registerCommand(plotCommand);

        // Integrations
        BuilderToolsIntegration builderToolsIntegration = new BuilderToolsIntegration();
        builderToolsIntegration.initialize();

        serviceRegistry.register(BuilderToolsIntegration.class, builderToolsIntegration);
    }

    private IPlotRepository createRepository(@Nonnull File dataDir, @Nonnull PlotConfig config) {
        PlotConfig.DatabaseSettings db = config.getDatabase();
        if (db != null && db.Enabled) {
            String jdbcUrl = db.JdbcUrl;
            if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:sqlite:") && !jdbcUrl.contains("/") && !jdbcUrl.contains("\\")) {
                jdbcUrl = "jdbc:sqlite:" + new File(dataDir, jdbcUrl.substring("jdbc:sqlite:".length())).getAbsolutePath();
            }
            ConsoleColors.info("Using SQL repository (HikariCP): " + jdbcUrl);
            return new SqlPlotRepository(jdbcUrl, db.Username, db.Password, db.MaxPoolSize);
        }
        ConsoleColors.info("Using JSON repository.");
        return new JsonPlotRepository(new File(dataDir, "plots.json"));
    }

    /**
     * Register the custom world generator provider
     */
    private void registerWorldGenerator() {
        try {
            IWorldGenProvider.CODEC.register(
                    PlotWorldGenProvider.ID,
                    PlotWorldGenProvider.class,
                    PlotWorldGenProvider.CODEC);
            ConsoleColors.success("Registered custom world generator: " + PlotWorldGenProvider.ID);
        } catch (Exception e) {
            ConsoleColors.error("Failed to register world generator: " + e.getMessage());
        }
    }

    /**
     * Register all entity systems
     */
    private void registerSystems() {
        var registry = getEntityStoreRegistry();
        @Nonnull
        IPlotManager plotManager = getPlotManager();
        @Nonnull
        IWorldManager worldManager = getWorldManager();

        registry.registerSystem(new BreakProtectionSystem(plotManager, worldManager));
        registry.registerSystem(new DamageBlockProtectionSystem(plotManager, worldManager));
        registry.registerSystem(new PlaceProtectionSystem(plotManager, worldManager));
        registry.registerSystem(new ServerBlockProtectionSystem(plotManager, worldManager));

        registry.registerSystem(new PlotNotificationSystem(plotManager, worldManager));
        registry.registerSystem(new UpdateNotificationSystem(getVersion()));
        registry.registerSystem(new RadarMarkerSystem(getRadarManager()));

        registry.registerSystem(new PlotDamageFlagSystem(plotManager, worldManager));
        registry.registerSystem(new PlotCraftingSystem(plotManager, worldManager));
        registry.registerSystem(new PlotMobProtectionSystem(plotManager, worldManager));
        registry.registerSystem(new PlotItemDropSystem(plotManager, worldManager));
        registry.registerSystem(new PlotItemPickupSystem(plotManager, worldManager));

        BuilderToolsIntegration builderToolsIntegration = serviceRegistry.getService(BuilderToolsIntegration.class)
                .orElse(null);
        if (builderToolsIntegration != null) {
            registry.registerSystem(new BuilderToolsMaskSystem(worldManager, builderToolsIntegration));
        }

        // Initialize packet interceptor
        TranslationManager translationManager = getTranslationManager();
        BuilderToolsPacketInterceptor interceptor = new BuilderToolsPacketInterceptor(
                (PlotManager) plotManager, translationManager);
        if (!interceptor.hookPacketHandler()) {
            ConsoleColors.warning("BuilderTools packet interceptor hook was not applied. Using mask/accessor protection only.");
        }

    }

    /**
     * Check for plugin updates
     */
    private void checkForUpdates() {
        UpdateChecker.checkForUpdates(getVersion()).thenAccept(latestVersion -> {
            if (latestVersion != null && UpdateChecker.isNewerVersion(getVersion(), latestVersion)) {
                ConsoleColors.info("A new version is available: " + latestVersion);
                ConsoleColors.info("Download: https://github.com/overworldlabs/plots/releases");
            } else if (latestVersion != null) {
                ConsoleColors.success("You are running the latest version (" + getVersion() + ")");
            }
        });
    }

    public IRadarManager getRadarManager() {
        return serviceRegistry.getServiceOrThrow(IRadarManager.class);
    }

    @Override
    protected void start() {
        super.start();
        ConsoleColors.info("Starting Plots...");
        getWorldManager().createWorldIfNeeded();
        getRadarManager().refreshAllPlotMarkers();
        menuKeybindManager = new com.overworldlabs.plots.menu.PlotMenuKeybindManager(this);
        menuKeybindManager.start();
        started = true;
    }

    @Override
    protected void shutdown() {
        ConsoleColors.info("Shutting down...");

        if (menuKeybindManager != null) {
            menuKeybindManager.shutdown();
            menuKeybindManager = null;
        }

        IPlotManager plotManager = getPlotManager();
        if (plotManager != null) {
            plotManager.savePlots();
        }
        IPlotRepository repo = getPlotRepository();
        if (repo != null) {
            repo.close();
        }

        super.shutdown();
        started = false;
        ConsoleColors.success("Shutdown complete!");
    }

    /**
     * Load configuration from config.json
     */
    @Nonnull
    private PlotConfig loadConfig() {
        File dataDir = getDataDirectory().toFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File configFile = new File(dataDir, "config.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (!configFile.exists()) {
            PlotConfig defaultConfig = PlotConfig.getDefault();

            try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
                gson.toJson(defaultConfig, writer);
                ConsoleColors.info("Created default config.json");
            } catch (java.io.IOException e) {
                ConsoleColors.error("Failed to save default config: " + e.getMessage());
            }

            return defaultConfig;
        }

        try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
            PlotConfig config = gson.fromJson(reader, PlotConfig.class);
            return (config != null) ? config : PlotConfig.getDefault();
        } catch (java.io.IOException e) {
            ConsoleColors.error("Failed to load config: " + e.getMessage());
            return PlotConfig.getDefault();
        }
    }

    @Nonnull
    public PlotConfig getConfig() {
        return loadConfig();
    }

    public void saveConfig(@Nonnull PlotConfig config) {
        File dataDir = getDataDirectory().toFile();
        File configFile = new File(dataDir, "config.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
            gson.toJson(config, writer);
            ConsoleColors.success("Successfully saved config.json");
        } catch (java.io.IOException e) {
            ConsoleColors.error("Failed to save config: " + e.getMessage());
        }
    }

    @Nonnull
    public IPlotManager getPlotManager() {
        return serviceRegistry.getServiceOrThrow(IPlotManager.class);
    }

    public IPlotRepository getPlotRepository() {
        return serviceRegistry.getService(IPlotRepository.class).orElse(null);
    }

    @Nonnull
    public IWorldManager getWorldManager() {
        return serviceRegistry.getServiceOrThrow(IWorldManager.class);
    }

    @Nonnull
    public TranslationManager getTranslationManager() {
        return serviceRegistry.getServiceOrThrow(TranslationManager.class);
    }

    public PrefabManager getPrefabManager() {
        return serviceRegistry.getServiceOrThrow(PrefabManager.class);
    }

    public HologramManager getHologramManager() {
        return serviceRegistry.getServiceOrThrow(HologramManager.class);
    }

    /**
     * Gets the plugin version from the JAR manifest
     */
    private String getVersion() {
        String version = getClass().getPackage().getImplementationVersion();

        if (version == null) {
            // Fallback for development environments
            return "1.0.0-DEV";
        }

        return version;
    }

    /**
     * Prints the plugin banner on startup
     */
    private void printBanner() {
        LOGGER.info(" ");
        LOGGER.info("  ____  _       _");
        LOGGER.info(" |  _ \\| |     | |");
        LOGGER.info(" | |_) | | ___ | |_ ___   Plots v" + getVersion());
        LOGGER.info(" |  __/| |/ _ \\| __/ __|  Running on Hytale");
        LOGGER.info(" | |   | | (_) | |_\\__ \\");
        LOGGER.info(" |_|   |_|\\___/ \\__|___/");
        LOGGER.info(" ");
    }
}
