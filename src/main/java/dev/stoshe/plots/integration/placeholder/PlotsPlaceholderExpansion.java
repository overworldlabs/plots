package dev.stoshe.plots.integration.placeholder;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.placeholder.api.PlaceholderExpansion;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.manager.PlotManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.PermissionUtil;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * PlaceholderAPI expansion exposing plot information under the {@code plots}
 * identifier (e.g. {@code %plots_currentplot%}, {@code %plots_plot_count%}).
 *
 * <p>Modelled on PlotSquared's placeholder set, adapted to this plugin and its
 * hybrid per-world / global plot limits.</p>
 */
public class PlotsPlaceholderExpansion extends PlaceholderExpansion {

    /** Displayed for an unlimited (admin or uncapped) limit. */
    private static final String UNLIMITED = "unlimited";

    @Override
    public String getIdentifier() {
        return "plots";
    }

    @Override
    public String getAuthor() {
        return "Stoshe Labs";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Object context, String params) {
        if (params == null) {
            return null;
        }
        PlayerRef playerRef = resolvePlayerRef(context);
        if (playerRef == null) {
            return null;
        }
        PlotManager manager = resolveManager();
        if (manager == null) {
            return null;
        }

        String key = params.toLowerCase();

        String serverValue = resolveServerPlaceholder(manager, key);
        if (serverValue != null) {
            return serverValue;
        }

        String playerValue = resolvePlayerPlaceholder(manager, playerRef, key);
        if (playerValue != null) {
            return playerValue;
        }

        return resolveCurrentPlotPlaceholder(manager, playerRef, key);
    }

    /** Server-wide placeholders that don't depend on the player. */
    private String resolveServerPlaceholder(PlotManager manager, String key) {
        switch (key) {
            case "worlds":
                return String.valueOf(manager.getConfig().getWorldNames().size());
            case "total_plots":
                return String.valueOf(manager.getAllPlots().size());
            default:
                return null;
        }
    }

    /** Player-scoped placeholders (counts and limits). */
    private String resolvePlayerPlaceholder(PlotManager manager, PlayerRef playerRef, String key) {
        UUID uuid = playerRef.getUuid();
        Predicate<String> hasPermission = playerRef::hasPermission;
        boolean admin = PermissionUtil.hasAdminPermission(uuid);

        if (key.startsWith("plot_count_")) {
            String world = key.substring("plot_count_".length());
            return String.valueOf(countOwnedInWorld(manager, uuid, world));
        }
        if (key.startsWith("max_plots_")) {
            String world = key.substring("max_plots_".length());
            return formatCap(manager.perWorldCap(world, hasPermission, admin));
        }

        switch (key) {
            case "has_plot":
                return String.valueOf(!manager.getPlayerPlots(uuid).isEmpty());
            case "plot_count":
                return String.valueOf(manager.getPlayerPlots(uuid).size());
            case "max_plots":
                return formatCap(manager.globalCap(hasPermission, admin));
            case "plots_left":
                return formatCap(manager.remainingClaims(uuid, currentWorldName(playerRef), hasPermission, admin));
            case "total_warps":
                return String.valueOf(countWarps(manager, uuid));
            default:
                return null;
        }
    }

    /** Placeholders about the plot the player is currently standing on. */
    private String resolveCurrentPlotPlaceholder(PlotManager manager, PlayerRef playerRef, String key) {
        if (!key.startsWith("currentplot") && !key.equals("is_owner") && !key.equals("is_trusted")) {
            return null;
        }

        Plot plot = currentPlot(manager, playerRef);
        UUID uuid = playerRef.getUuid();

        switch (key) {
            case "currentplot":
                return plot == null ? "none" : plot.getGridX() + ";" + plot.getGridZ();
            case "currentplot_name":
                return plot == null ? "" : plot.getName();
            case "currentplot_owner":
                return plot == null ? "" : plot.getOwnerName();
            case "currentplot_world":
                return plot == null ? "" : plot.getWorld();
            case "currentplot_members":
                return plot == null ? "0" : String.valueOf(plot.getTrustedPlayers().size());
            case "currentplot_x":
                return plot == null ? "" : String.valueOf(plot.getGridX());
            case "currentplot_z":
                return plot == null ? "" : String.valueOf(plot.getGridZ());
            case "currentplot_merged":
                return plot == null ? "0" : String.valueOf(manager.getMergedComponent(plot).size());
            case "is_owner":
                return String.valueOf(plot != null && uuid.equals(plot.getOwner()));
            case "is_trusted":
                return String.valueOf(plot != null && plot.isTrusted(uuid));
            default:
                return null;
        }
    }

    private Plot currentPlot(PlotManager manager, PlayerRef playerRef) {
        String world = currentWorldName(playerRef);
        if (world == null) {
            return null;
        }
        Vector3d position = playerRef.getTransform().getPosition();
        return manager.getPlotAt(world, (int) position.x, (int) position.z);
    }

    private String currentWorldName(PlayerRef playerRef) {
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            return null;
        }
        World world = Universe.get().getWorld(worldUuid);
        return world == null ? null : world.getName();
    }

    private int countOwnedInWorld(PlotManager manager, UUID uuid, String world) {
        int count = 0;
        for (Plot plot : manager.getPlayerPlots(uuid)) {
            if (world.equalsIgnoreCase(plot.getWorld())) {
                count++;
            }
        }
        return count;
    }

    private int countWarps(PlotManager manager, UUID uuid) {
        int total = 0;
        for (Plot plot : manager.getPlayerPlots(uuid)) {
            total += plot.getWarpCount();
        }
        return total;
    }

    private String formatCap(int cap) {
        return cap == Integer.MAX_VALUE ? UNLIMITED : String.valueOf(cap);
    }

    @SuppressWarnings("unchecked")
    private PlayerRef resolvePlayerRef(Object context) {
        if (context instanceof PlayerRef ref) {
            return ref;
        }
        if (context instanceof Ref) {
            Ref<EntityStore> entityRef = (Ref<EntityStore>) context;
            if (!entityRef.isValid()) {
                return null;
            }
            try {
                Store<EntityStore> store = entityRef.getStore();
                return store.getComponent(entityRef, PlayerRef.getComponentType());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private PlotManager resolveManager() {
        Plots plugin = Plots.getInstance();
        if (plugin == null) {
            return null;
        }
        return (plugin.getPlotManager() instanceof PlotManager manager) ? manager : null;
    }
}
