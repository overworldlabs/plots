package dev.stoshe.plots;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.api.IPlotRepository;
import dev.stoshe.plots.api.IRadarManager;
import dev.stoshe.plots.api.IServiceRegistry;
import dev.stoshe.plots.api.IWorldManager;
import dev.stoshe.plots.api.PlotsAPI;
import dev.stoshe.plots.api.impl.PlotsAPIImpl;
import dev.stoshe.plots.command.PlotCommand;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.core.ServiceRegistryImpl;
import dev.stoshe.plots.data.JsonPlotRepository;
import dev.stoshe.plots.data.SqlPlotRepository;
import dev.stoshe.plots.integration.buildertools.BuilderToolsIntegration;
import dev.stoshe.plots.integration.economy.PlotEconomyService;
import dev.stoshe.plots.integration.holograms.HologramManager;
import dev.stoshe.plots.integration.mixin.MixinBridgeStatus;
import dev.stoshe.plots.integration.mixin.PlotsMixinsCompatibility;
import dev.stoshe.plots.manager.PlotManager;
import dev.stoshe.plots.manager.PrefabManager;
import dev.stoshe.plots.manager.RadarManager;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.manager.WorldManager;
import dev.stoshe.plots.system.BreakProtectionSystem;
import dev.stoshe.plots.system.BuilderToolsMaskSystem;
import dev.stoshe.plots.system.BuilderToolsPacketInterceptor;
import dev.stoshe.plots.system.DamageBlockProtectionSystem;
import dev.stoshe.plots.system.PlaceProtectionSystem;
import dev.stoshe.plots.system.PlotCraftingSystem;
import dev.stoshe.plots.system.PlotDamageFlagSystem;
import dev.stoshe.plots.system.PlotItemDropSystem;
import dev.stoshe.plots.system.PlotItemPickupSystem;
import dev.stoshe.plots.system.PlotMobProtectionSystem;
import dev.stoshe.plots.system.PlotNotificationSystem;
import dev.stoshe.plots.system.RadarMarkerSystem;
import dev.stoshe.plots.system.ServerBlockProtectionSystem;
import dev.stoshe.plots.system.UpdateNotificationSystem;
import dev.stoshe.plots.util.Console;
import dev.stoshe.plots.util.UpdateChecker;
import dev.stoshe.plots.worldgen.PlotWorldGenProvider;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;


import javax.annotation.Nonnull;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Main plugin class for the Plots system
 */
public class Plots extends JavaPlugin {
    private static Plots instance;
    private static PlotsAPI api;
    private static volatile boolean started;

    private IServiceRegistry serviceRegistry;
    private volatile PlotConfig cachedConfig;
    private PlotEconomyService economyService;
    private dev.stoshe.plots.menu.PlotMenuKeybindManager menuKeybindManager;
    private dev.stoshe.plots.manager.KnownPlayersService knownPlayers;

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
    public CompletableFuture<Void> preLoad() {
        // CRITICAL: register the custom plot world generator here, in preLoad —
        // NOT in setup(). preLoad runs in the plugin SETUP phase, which completes
        // for every plugin before the START phase, where the Universe module loads
        // existing worlds from disk. If we register later (setup/start), a saved
        // world whose config has "WorldGen: { Type: Plots }" can no longer resolve
        // its generator: the engine silently falls back to the default (empty/void)
        // generator, so the world generates nothing and renders pitch black on join.
        registerWorldGenerator();
        return super.preLoad();
    }

    @Override
    protected void setup() {
        super.setup();
        printBanner();

        // Force every plugin class to load now, on the enable thread, before any
        // world/worker thread can reference one lazily. See preloadPluginClasses().
        preloadPluginClasses();

        serviceRegistry = new ServiceRegistryImpl();
        PlotConfig config = getConfig();
        File dataDir = getDataDirectory().toFile();

        economyService = new PlotEconomyService(config);
        economyService.initialize();
        serviceRegistry.register(PlotEconomyService.class, economyService);

        initializeTranslationManager(dataDir, config);
        initializeManagers(dataDir, config);
        // World generator is registered earlier, in preLoad(), so it exists before
        // the Universe module loads saved worlds. See preLoad().
        registerSystems();

        // Basic protection (block break/place/interact, containers, mobs, item
        // drop/pickup, etc.) is always enforced by the ECS protection systems
        // registered above — it does not require any bridge. The TaleGuard bridge
        // adds the mixin-only flags (item-pickup, builder tools, mob-spawning,
        // keep-inventory, commands, ...). Plots prefers TaleGuard when present and
        // gracefully falls back to basic protection when it is absent.
        if (MixinBridgeStatus.isReadyForMixinFlags()) {
            if (MixinBridgeStatus.isMixinsLoaded()) {
                Console.success("TaleGuard bridge active — full protection coverage enabled.");
            } else {
                Console.info("TaleGuard bridge bootstrap is ready. Mixins will be applied as target classes load.");
            }
        } else if (MixinBridgeStatus.isActive()) {
            Console.warning("Protection bridge detected, but bootstrap is not ready — running with basic protection only.");
        } else {
            Console.warning("No protection bridge detected — running with BASIC protection (ECS) only.");
            Console.warning("Mixin-required flags are disabled. Install TaleGuard for full protection coverage.");
        }
        // Keep the legacy plots-native registry populated (harmless if unused) and
        // register the adapter hook with the shared TaleGuard bridge registry.
        PlotsMixinsCompatibility.register(getPlotManager());
        PlotsMixinsCompatibility.registerTaleGuard(getPlotManager());

        Console.success("Plots enabled.");

        checkForUpdates();
    }

    /**
     * Eagerly load every plugin class on the enable thread.
     *
     * <p>This Hytale build's {@code PluginClassLoader} intermittently fails to resolve
     * a plugin class the first time it is referenced from a worker thread (the world
     * {@code TickingThread}, the world-gen {@code ForkJoinPool}, ...), throwing
     * {@code ClassNotFoundException} for a class that is physically present in the jar
     * and tearing the world down. Observed offenders: {@code FlagRegistry} (HUD tick)
     * and {@code PlotTerrainGenerator$PositionType} (async world-gen).
     *
     * <p>Loading — not initializing — every class here, on the thread that already
     * owns the plugin classloader, caches their definitions so later lazy lookups from
     * any thread hit the cache and never re-enter the flaky load path.
     */
    private void preloadPluginClasses() {
        ClassLoader loader = getClass().getClassLoader();
        java.security.CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            Console.warning("Could not locate plugin jar; skipping class pre-load.");
            return;
        }

        File jarFile;
        try {
            jarFile = new File(codeSource.getLocation().toURI());
        } catch (Exception ex) {
            jarFile = new File(codeSource.getLocation().getPath());
        }
        if (!jarFile.isFile()) {
            // Running from exploded classes (e.g. gradle runServer) — nothing to scan.
            return;
        }

        int loaded = 0;
        int skipped = 0;
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class") || !name.startsWith("dev/stoshe/plots/")) {
                    continue;
                }
                String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                try {
                    // initialize=false: load + link without running static initializers
                    // (loading is the step that fails on worker threads).
                    Class.forName(className, false, loader);
                    loaded++;
                } catch (Throwable t) {
                    // Optional-integration classes can fail to link when their dependency
                    // is absent; that is expected — skip and keep going.
                    skipped++;
                }
            }
        } catch (Exception ex) {
            Console.warning("Class pre-load aborted: " + ex.getMessage());
            return;
        }
        Console.info("Pre-loaded " + loaded + " plugin classes (" + skipped
                + " skipped) to avoid lazy class-loading crashes.");
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
        knownPlayers = new dev.stoshe.plots.manager.KnownPlayersService(dataDir);

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
        // Plots created before the multi-world migration carry no world; they are
        // assigned to this (the first configured) world on load.
        String legacyWorldName = config.getDefaultWorldName();
        PlotConfig.DatabaseSettings db = config.getDatabase();
        if (db != null && db.Enabled) {
            String jdbcUrl = db.JdbcUrl;
            if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:sqlite:") && !jdbcUrl.contains("/") && !jdbcUrl.contains("\\")) {
                jdbcUrl = "jdbc:sqlite:" + new File(dataDir, jdbcUrl.substring("jdbc:sqlite:".length())).getAbsolutePath();
            }
            Console.info("Using SQL repository (HikariCP): " + jdbcUrl);
            return new SqlPlotRepository(jdbcUrl, db.Username, db.Password, db.MaxPoolSize, legacyWorldName);
        }
        Console.info("Using JSON repository.");
        return new JsonPlotRepository(new File(dataDir, "plots.json"), legacyWorldName);
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
            Console.success("Registered custom world generator: " + PlotWorldGenProvider.ID);
        } catch (Exception e) {
            Console.error("Failed to register world generator: " + e.getMessage());
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
        registry.registerSystem(new dev.stoshe.plots.system.PlotSetupNudgeSystem());
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
            Console.warning("BuilderTools packet interceptor hook was not applied. Using mask/accessor protection only.");
        }

    }

    /**
     * Check for plugin updates
     */
    private void checkForUpdates() {
        UpdateChecker.checkForUpdates(getVersion()).thenAccept(latestVersion -> {
            if (latestVersion != null && UpdateChecker.isNewerVersion(getVersion(), latestVersion)) {
                // Gold high-visibility banner (colour 220), same terminal treatment as the boot banner.
                Console.banner(220,
                        ">>  Plots UPDATE AVAILABLE",
                        "",
                        "    You have  v" + getVersion() + "   ->   latest is  v" + latestVersion,
                        "    Download:  " + UpdateChecker.RELEASES_URL,
                        "    Discord:   https://discord.gg/rC9eSzH3tf");
            } else if (latestVersion != null) {
                Console.success("You are running the latest version (" + getVersion() + ").");
            }
        });
    }

    public IRadarManager getRadarManager() {
        return serviceRegistry.getServiceOrThrow(IRadarManager.class);
    }

    @Override
    protected void start() {
        super.start();
        getWorldManager().createWorldsIfNeeded();
        getRadarManager().refreshAllPlotMarkers();
        menuKeybindManager = new dev.stoshe.plots.menu.PlotMenuKeybindManager(this);
        menuKeybindManager.start();
        registerPlaceholderExpansion();
        started = true;
    }

    /**
     * Registers the optional PlaceholderAPI expansion. Guarded so a missing
     * PlaceholderAPI plugin (no classes on the classpath) just skips it.
     */
    private void registerPlaceholderExpansion() {
        try {
            dev.stoshe.plots.integration.placeholder.PlaceholderIntegration.register();
        } catch (Throwable t) {
            Console.info("PlaceholderAPI not present; skipping plots placeholder expansion.");
        }
    }

    @Override
    protected void shutdown() {
        if (menuKeybindManager != null) {
            menuKeybindManager.shutdown();
            menuKeybindManager = null;
        }

        if (knownPlayers != null) {
            knownPlayers.save();
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
        Console.info("Plots disabled.");
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
                Console.info("Created default config.json");
            } catch (java.io.IOException e) {
                Console.error("Failed to save default config: " + e.getMessage());
            }

            return defaultConfig;
        }

        try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
            PlotConfig config = gson.fromJson(reader, PlotConfig.class);
            if (config == null) {
                return PlotConfig.getDefault();
            }
            config.normalizeAfterLoad();
            return config;
        } catch (java.io.IOException e) {
            Console.error("Failed to load config: " + e.getMessage());
            return PlotConfig.getDefault();
        }
    }

    /**
     * Returns the in-memory config, loading it from disk once and caching it.
     * This is the same instance held by the managers, so per-world lookups on the
     * generation/protection hot path don't re-read config.json every call.
     */
    @Nonnull
    public PlotConfig getConfig() {
        PlotConfig local = cachedConfig;
        if (local == null) {
            synchronized (this) {
                local = cachedConfig;
                if (local == null) {
                    local = loadConfig();
                    cachedConfig = local;
                }
            }
        }
        return local;
    }

    /** Re-reads config.json from disk and merges it into the cached instance in place. */
    public void reloadConfig() {
        PlotConfig fresh = loadConfig();
        PlotConfig local = cachedConfig;
        if (local != null) {
            local.overwriteWith(fresh);
        } else {
            cachedConfig = fresh;
        }
    }

    public void saveConfig(@Nonnull PlotConfig config) {
        File dataDir = getDataDirectory().toFile();
        File configFile = new File(dataDir, "config.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        config.normalizeAfterLoad();
        cachedConfig = config;
        try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
            gson.toJson(config, writer);
            Console.success("Successfully saved config.json");
        } catch (java.io.IOException e) {
            Console.error("Failed to save config: " + e.getMessage());
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
    public dev.stoshe.plots.manager.KnownPlayersService getKnownPlayers() {
        return knownPlayers;
    }

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
     * Prints the boot banner — same boxed, single-colour treatment the other Stoshe
     * plugins use (AeroWars, AntiXray, Voltis), in plot green.
     */
    private void printBanner() {
        Console.banner(40, "Plots v" + getVersion(), "Grid-based plot management  |  Running on Hytale");
    }
}
