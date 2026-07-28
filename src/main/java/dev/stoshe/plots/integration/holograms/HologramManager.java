package dev.stoshe.plots.integration.holograms;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.manager.PlotManager;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.Console;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages holograms for plots using Hylograms API.
 */
public class HologramManager {
    private final IPlotManager plotManager;

    private final Object developerApi;
    private final Method developerExistsMethod;
    private final Method developerDeleteMethod;
    private final Method developerCreateMethod;
    private final Method developerAddLineMethod;
    private final Method developerRespawnMethod;

    private final Method legacyExistsMethod;
    private final Method legacyDeleteMethod;
    private final Method legacyCreateMethod;

    private final boolean available;

    public HologramManager(@Nonnull IPlotManager plotManager) {
        this.plotManager = plotManager;

        Object devApi = null;
        Method devExists = null;
        Method devDelete = null;
        Method devCreate = null;
        Method devAddLine = null;
        Method devRespawn = null;

        Method legacyExists = null;
        Method legacyDelete = null;
        Method legacyCreate = null;

        boolean isAvailable = false;

        try {
            Class<?> entrypointClass;
            try {
                entrypointClass = Class.forName("dev.ehko.hylograms.framework.api.HylogramsApi");
            } catch (ClassNotFoundException ignored) {
                entrypointClass = Class.forName("dev.ehko.hylograms.api.HologramsAPI");
            }

            Method getMethod = entrypointClass.getMethod("get");
            devApi = getMethod.invoke(null);

            if (devApi != null) {
                Class<?> apiClass = devApi.getClass();
                devExists = apiClass.getMethod("exists", String.class);
                devDelete = apiClass.getMethod("delete", String.class);

                Class<?> vector3dClass = Class.forName("org.joml.Vector3d");
                Class<?> vector3fClass = Class.forName("com.hypixel.hytale.math.vector.Rotation3f");

                devCreate = apiClass.getMethod(
                        "create",
                        String.class,
                        String.class,
                        vector3dClass,
                        vector3fClass,
                        String.class);
                devAddLine = apiClass.getMethod("addLine", String.class, String.class);
                devRespawn = apiClass.getMethod("respawn", String.class);

                isAvailable = true;
            }
        } catch (ClassNotFoundException ignored) {
            // Try legacy API below.
        } catch (ReflectiveOperationException e) {
            Console.error("Failed to initialize Hylograms developer API: " + e.getMessage());
        }

        if (!isAvailable) {
            try {
                Class<?> legacyApiClass = Class.forName("dev.ehko.hylograms.api.HologramsAPI");
                legacyExists = legacyApiClass.getMethod("exists", String.class);
                legacyDelete = legacyApiClass.getMethod("delete", String.class, Store.class);
                legacyCreate = legacyApiClass.getMethod("create", String.class, Store.class);
                isAvailable = true;
            } catch (ClassNotFoundException e) {
                Console.warning("Hylograms not found. Hologram integration disabled.");
            } catch (ReflectiveOperationException e) {
                Console.error("Failed to initialize Hylograms legacy API: " + e.getMessage());
            }
        }

        if (isAvailable) {
            Console.info("Hylograms integration enabled.");
        }

        this.developerApi = devApi;
        this.developerExistsMethod = devExists;
        this.developerDeleteMethod = devDelete;
        this.developerCreateMethod = devCreate;
        this.developerAddLineMethod = devAddLine;
        this.developerRespawnMethod = devRespawn;

        this.legacyExistsMethod = legacyExists;
        this.legacyDeleteMethod = legacyDelete;
        this.legacyCreateMethod = legacyCreate;

        this.available = isAvailable;
    }

    public void updateHologram(@Nonnull Plot plot, @Nonnull Store<EntityStore> store) {
        PlotConfig config = plotManager.getConfig();
        if (!config.isHologramEnabled() || !available) {
            return;
        }

        String id = "plot_" + plot.getGridX() + "_" + plot.getGridZ();
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        int worldX = config.gridToWorldX(plot.getGridX());
        int worldZ = config.gridToWorldZ(plot.getGridZ());
        double y = 64.0 + config.getHologramHeightOffset();

        String title = tm.get("hologram.title",
                "name", plot.getName(),
                "plot_name", plot.getName());
        String subtitle = tm.get("hologram.subtitle",
                "owner", plot.getOwnerName(),
                "owner_name", plot.getOwnerName());
        String safeTitle = title != null ? title : "Plot";
        String safeSubtitle = subtitle != null ? subtitle : "Owner";

        try {
            String worldName = plot.getWorld();
            if (usesDeveloperApi()) {
                updateWithDeveloperApi(id, worldName, worldX + 0.5, y, worldZ + 0.5, safeTitle, safeSubtitle);
            } else if (usesLegacyApi()) {
                updateWithLegacyApi(
                        id,
                        store,
                        worldX + 0.5,
                        y,
                        worldZ + 0.5,
                        worldName,
                        safeTitle,
                        safeSubtitle,
                        config.getHologramTitleColor());
            }
        } catch (Throwable e) {
            Console.error("Failed to update hologram for plot at " + plot.getGridX() + ","
                    + plot.getGridZ() + ": " + e.getMessage(), e);
        }
    }

    public void removeHologram(@Nonnull Plot plot, @Nonnull Store<EntityStore> store) {
        if (!available) {
            return;
        }

        String id = "plot_" + plot.getGridX() + "_" + plot.getGridZ();
        try {
            if (usesDeveloperApi()) {
                removeWithDeveloperApi(id);
            } else if (usesLegacyApi()) {
                removeWithLegacyApi(id, store);
            }
        } catch (Throwable e) {
            Console.error("Failed to remove hologram for plot at " + plot.getGridX() + ","
                    + plot.getGridZ() + ": " + e.getMessage());
        }
    }

    public void spawnAllHolograms(@Nonnull Store<EntityStore> store) {
        for (Plot plot : plotManager.getAllPlots()) {
            if (plot != null) {
                updateHologram(plot, store);
            }
        }
    }

    public void spawnAllHologramsForOnlinePlayers() {
        PlotConfig config = plotManager.getConfig();
        if (!config.isHologramEnabled()) {
            return;
        }

        Console.info("Hologram system is enabled. Holograms will appear when plots are claimed or renamed.");
    }

    /**
     * Rebuilds holograms for affected plots based on current merge state.
     *
     * <p>All touched plot holograms are removed first. Then one hologram is
     * created per merged component (using its canonical plot).</p>
     */
    public void refreshMergedHolograms(@Nonnull Collection<Plot> affectedPlots, @Nonnull Store<EntityStore> store) {
        if (affectedPlots.isEmpty() || !available) {
            return;
        }

        if (!(plotManager instanceof PlotManager pm)) {
            for (Plot plot : affectedPlots) {
                if (plot != null) {
                    updateHologram(plot, store);
                }
            }
            return;
        }

        Set<Plot> touched = new HashSet<>();
        for (Plot plot : affectedPlots) {
            if (plot == null) {
                continue;
            }
            List<Plot> component = pm.getMergedComponent(plot);
            if (component.isEmpty()) {
                touched.add(plot);
                continue;
            }
            touched.addAll(component);
        }

        for (Plot plot : touched) {
            removeHologram(plot, store);
        }

        Set<String> canonicalKeys = new HashSet<>();
        List<Plot> canonicalPlots = new ArrayList<>();
        for (Plot plot : touched) {
            Plot canonical = pm.getCanonicalPlot(plot);
            String key = canonical.getGridX() + "," + canonical.getGridZ();
            if (canonicalKeys.add(key)) {
                canonicalPlots.add(canonical);
            }
        }

        for (Plot canonical : canonicalPlots) {
            updateHologram(canonical, store);
        }
    }

    private boolean usesDeveloperApi() {
        return developerApi != null
                && developerExistsMethod != null
                && developerDeleteMethod != null
                && developerCreateMethod != null
                && developerAddLineMethod != null
                && developerRespawnMethod != null;
    }

    private boolean usesLegacyApi() {
        return legacyExistsMethod != null && legacyDeleteMethod != null && legacyCreateMethod != null;
    }

    private void updateWithDeveloperApi(String id, String worldName, double x, double y, double z, String title, String subtitle)
            throws ReflectiveOperationException {
        if (Boolean.TRUE.equals(developerExistsMethod.invoke(developerApi, id))) {
            developerDeleteMethod.invoke(developerApi, id);
        }

        Object pos = newVector3d(x, y, z);
        Object rot = newVector3f(0f, 180f, 0f);

        developerCreateMethod.invoke(developerApi, id, worldName, pos, rot, title);
        developerAddLineMethod.invoke(developerApi, id, subtitle);
        developerRespawnMethod.invoke(developerApi, id);
    }

    private void updateWithLegacyApi(
            String id,
            Store<EntityStore> store,
            double x,
            double y,
            double z,
            String worldName,
            String title,
            String subtitle,
            String color) throws ReflectiveOperationException {
        if (Boolean.TRUE.equals(legacyExistsMethod.invoke(null, id))) {
            legacyDeleteMethod.invoke(null, id, store);
        }

        Object builder = legacyCreateMethod.invoke(null, id, store);
        builder = invokeBuilder(builder, "at", new Class<?>[] { double.class, double.class, double.class }, x, y, z);
        builder = invokeBuilder(builder, "inWorld", new Class<?>[] { String.class }, worldName);
        builder = invokeBuilder(builder, "color", new Class<?>[] { String.class }, color);
        builder = invokeBuilder(builder, "addLine", new Class<?>[] { String.class }, title);
        builder = invokeBuilder(builder, "addLine", new Class<?>[] { String.class }, subtitle);
        builder.getClass().getMethod("spawn").invoke(builder);
    }

    private void removeWithDeveloperApi(String id) throws ReflectiveOperationException {
        if (Boolean.TRUE.equals(developerExistsMethod.invoke(developerApi, id))) {
            developerDeleteMethod.invoke(developerApi, id);
        }
    }

    private void removeWithLegacyApi(String id, Store<EntityStore> store) throws ReflectiveOperationException {
        if (Boolean.TRUE.equals(legacyExistsMethod.invoke(null, id))) {
            legacyDeleteMethod.invoke(null, id, store);
        }
    }

    @Nonnull
    private Object newVector3d(double x, double y, double z) throws ReflectiveOperationException {
        Class<?> vector3dClass = Class.forName("org.joml.Vector3d");
        Constructor<?> ctor = vector3dClass.getConstructor(double.class, double.class, double.class);
        return ctor.newInstance(x, y, z);
    }

    @Nonnull
    private Object newVector3f(float x, float y, float z) throws ReflectiveOperationException {
        Class<?> vector3fClass = Class.forName("com.hypixel.hytale.math.vector.Rotation3f");
        Constructor<?> ctor = vector3fClass.getConstructor(float.class, float.class, float.class);
        return ctor.newInstance(x, y, z);
    }

    private Object invokeBuilder(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws ReflectiveOperationException {
        Object next = target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
        return next != null ? next : target;
    }
}
