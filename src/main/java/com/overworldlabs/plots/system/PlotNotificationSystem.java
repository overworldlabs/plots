package com.overworldlabs.plots.system;

import com.overworldlabs.plots.util.PlayerIdentity;
import com.hypixel.hytale.math.vector.Vector3d;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.protocol.packets.world.UpdateWeather;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.api.IWorldManager;
import com.overworldlabs.plots.flag.FlagRegistry;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.ui.PlotInfoHUD;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * System to notify players when they enter the plot world or a plot area
 */
public class PlotNotificationSystem extends EntityTickingSystem<EntityStore> {
    private static final Pattern HEX_COLOR_TAG = Pattern.compile("\\{#[A-Fa-f0-9]{6}\\}");
    private static final long DENY_MESSAGE_COOLDOWN_MS = 2000L;
    private static final double MAX_PUSHBACK_DISTANCE_SQ = 36.0D;
    private final IPlotManager plotManager;
    private final TranslationManager translationManager;
    private final Map<UUID, String> lastPlotId = new HashMap<>();
    private final Map<UUID, String> lastHudState = new HashMap<>();
    private final Map<UUID, PlotInfoHUD> activeHuds = new HashMap<>();
    private final Map<UUID, Vector3d> lastSafePosition = new HashMap<>();
    private final Map<UUID, UUID> lastSafeWorld = new HashMap<>();
    private final Map<UUID, String> lastWeatherOverride = new HashMap<>();
    private final Map<UUID, Long> lastDenyMessageAt = new HashMap<>();

    public PlotNotificationSystem(@Nonnull IPlotManager plotManager, @Nonnull IWorldManager worldManager) {
        this.plotManager = plotManager;
        this.translationManager = Plots.getInstance().getTranslationManager();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer) {

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;
        Player player = chunk.getComponent(index, Player.getComponentType());
        if (player == null)
            return;

        UUID uuid = PlayerIdentity.uuid(playerRef);
        boolean isAdminBypass = PermissionUtil.hasAdminPermission(uuid);
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null)
            return;

        World currentWorld = Universe.get().getWorld(worldUuid);
        if (currentWorld == null) {
            return;
        }

        // Only notify in managed worlds.
        if (!plotManager.getConfig().isManagedWorld(currentWorld.getName())) {
            // Cleanup if they left the world
            restoreWorldWeather(playerRef, currentWorld);
            removeHud(player, playerRef, uuid);
            lastPlotId.remove(uuid);
            lastHudState.remove(uuid);
            lastWeatherOverride.remove(uuid);
            lastDenyMessageAt.remove(uuid);
            return;
        }

        PlotInfoHUD hud = activeHuds.get(uuid);
        if (hud == null) {
            hud = new PlotInfoHUD(playerRef);
            activeHuds.put(uuid, hud);
            attachHud(player, playerRef, hud);
            hud.show();
        }

        if (!lastPlotId.containsKey(uuid)) {
            // Refresh radar markers the first tick in managed world.
            Plots.getInstance().getRadarManager().refreshPlayerMarkers(playerRef);
        }

        // Plot entry notification
        Vector3d pos = playerRef.getTransform().getPosition();
        Plot plot = plotManager.getPlotAt(currentWorld.getName(), (int) pos.x, (int) pos.z);
        Plot weatherPlot = resolveWeatherPlot(currentWorld.getName(), (int) pos.x, (int) pos.z);
        String currentId = (plot == null) ? "road" : plot.getGridX() + "_" + plot.getGridZ();
        String lastId = lastPlotId.get(uuid);
        String newHudState = null;
        if (plot != null) {
            newHudState = plot.getName() + "|" + plot.getOwnerName();
        }
        String oldHudState = lastHudState.get(uuid);
        boolean hudContentChanged = (newHudState == null && oldHudState != null)
                || (newHudState != null && !newHudState.equals(oldHudState));

        if (!isAdminBypass && lastId != null && !currentId.equals(lastId) && plot != null && !plot.isOwnerOrMember(uuid)
                && !plot.getFlagValue(FlagRegistry.ENTRY)) {
            sendDenyMessageWithCooldown(playerRef, uuid, resolveDenyMessage(plot, tm().get("general.no_permission")));
            teleportToLastSafe(buffer, chunk.getReferenceTo(index), playerRef, worldUuid, pos);
            return;
        }

        if (!isAdminBypass && lastId != null && !currentId.equals(lastId) && !lastId.equals("road")) {
            String[] parts = lastId.split("_");
            if (parts.length == 2) {
                try {
                    Plot lastPlot = plotManager.getPlotByGrid(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                    if (lastPlot != null && !lastPlot.isOwnerOrMember(uuid) && !lastPlot.getFlagValue(FlagRegistry.EXIT)) {
                        sendDenyMessageWithCooldown(playerRef, uuid,
                                resolveDenyMessage(lastPlot, tm().get("general.no_permission")));
                        teleportToLastSafe(buffer, chunk.getReferenceTo(index), playerRef, worldUuid, pos);
                        return;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!currentId.equals(lastId) || hudContentChanged) {
            if (plot != null) {
                String greeting = plot.getFlagValue(FlagRegistry.GREET_MESSAGE);
                String safePlotName = sanitizeHudText(plot.getName());
                String safeOwnerName = sanitizeHudText(plot.getOwnerName());
                String hudNameLine = translationManager.get("notification.hud_name_line",
                        "name", safePlotName,
                        "plot_name", safePlotName,
                        "owner", safeOwnerName,
                        "owner_name", safeOwnerName,
                        "x", String.valueOf(plot.getGridX()),
                        "z", String.valueOf(plot.getGridZ()));
                String hudOwnerLine = translationManager.get("notification.hud_owner_line",
                        "name", safePlotName,
                        "plot_name", safePlotName,
                        "owner", safeOwnerName,
                        "owner_name", safeOwnerName,
                        "x", String.valueOf(plot.getGridX()),
                        "z", String.valueOf(plot.getGridZ()));

                String status = plot.getOwner() != null && plot.getOwner().equals(uuid)
                        ? tm().get("notification.hud_status_owner")
                        : (plot.isTrusted(uuid)
                                ? tm().get("notification.hud_status_trusted")
                                : tm().get("notification.hud_status_visitor"));
                String hudInfoLine = translationManager.get("notification.hud_info_line",
                        "status", status,
                        "trusted", String.valueOf(plot.getTrustedPlayers().size()),
                        "merged", String.valueOf(plot.getMergedPlots().size()));

                hud.setPlotInfo(hudNameLine, hudOwnerLine, hudInfoLine);
                safeRequestHudUpdate(hud);

                if (!greeting.isEmpty()) {
                    playerRef.sendMessage(ChatUtil.colorize(greeting));
                }
                if (weatherPlot != null) {
                    applyPlotWeather(playerRef, currentWorld, weatherPlot);
                } else {
                    restoreWorldWeather(playerRef, currentWorld);
                }
                lastHudState.put(uuid, newHudState);
            } else if (lastId != null && !lastId.equals("road")) {
                hud.setFallback(tm().get("notification.hud_fallback"));
                safeRequestHudUpdate(hud);
                lastHudState.remove(uuid);
                restoreWorldWeather(playerRef, currentWorld);

                // Leaving a plot, check farewell flag
                String[] parts = lastId.split("_");
                if (parts.length == 2) {
                    try {
                        Plot lastPlot = plotManager.getPlotByGrid(Integer.parseInt(parts[0]),
                                Integer.parseInt(parts[1]));
                        if (lastPlot != null) {
                            String farewell = lastPlot.getFlagValue(FlagRegistry.FAREWELL_MESSAGE);
                            if (!farewell.isEmpty()) {
                                playerRef.sendMessage(ChatUtil.colorize(farewell));
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else {
                // On a road (no plot) inside a managed world: show the menu hint.
                hud.setFallback(tm().get("notification.hud_fallback"));
                safeRequestHudUpdate(hud);
                lastHudState.remove(uuid);
                restoreWorldWeather(playerRef, currentWorld);
            }

            lastPlotId.put(uuid, currentId);
        } else if (weatherPlot != null) {
            applyPlotWeather(playerRef, currentWorld, weatherPlot);
        } else {
            restoreWorldWeather(playerRef, currentWorld);
        }

        lastSafePosition.put(uuid, new Vector3d(pos.x, pos.y, pos.z));
        lastSafeWorld.put(uuid, worldUuid);
    }

    private TranslationManager tm() {
        return translationManager != null ? translationManager : Plots.getInstance().getTranslationManager();
    }

    private String resolveDenyMessage(@Nonnull Plot plot, @Nonnull String fallback) {
        String custom = plot.getFlagValue(FlagRegistry.DENY_MESSAGE);
        if (custom != null && !custom.trim().isEmpty()) {
            return custom;
        }
        return fallback;
    }

    private void teleportToLastSafe(@Nonnull CommandBuffer<EntityStore> buffer, @Nonnull com.hypixel.hytale.component.Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef, @Nonnull UUID currentWorld, @Nonnull Vector3d currentPos) {
        UUID safeWorld = lastSafeWorld.get(playerRef.getUuid());
        Vector3d safePos = lastSafePosition.get(playerRef.getUuid());
        if (safeWorld == null || safePos == null) {
            safeWorld = currentWorld;
            safePos = currentPos;
        }
        if (!safeWorld.equals(currentWorld)) {
            return;
        }

        double dx = currentPos.x - safePos.x;
        double dy = currentPos.y - safePos.y;
        double dz = currentPos.z - safePos.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > MAX_PUSHBACK_DISTANCE_SQ) {
            return;
        }

        World world = Universe.get().getWorld(safeWorld);
        if (world == null) {
            return;
        }
        buffer.addComponent(ref, Teleport.getComponentType(),
                new Teleport(world, new Vector3d(safePos.x, safePos.y, safePos.z), new Vector3f(0, 0, 0)));
    }

    private void removeHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull UUID uuid) {
        PlotInfoHUD hud = activeHuds.remove(uuid);
        try {
            if (hud != null) {
                detachHud(player, playerRef, hud);
            }
        } catch (Exception ignored) {
        }
    }

    private void sendDenyMessageWithCooldown(@Nonnull PlayerRef playerRef, @Nonnull UUID playerUuid, @Nonnull String msg) {
        long now = System.currentTimeMillis();
        long last = lastDenyMessageAt.getOrDefault(playerUuid, 0L);
        if (now - last < DENY_MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastDenyMessageAt.put(playerUuid, now);
        playerRef.sendMessage(ChatUtil.error(msg));
    }

    private void applyPlotWeather(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull Plot plot) {
        String weatherValue = normalizeWeatherFlag(plot.getFlagValue(FlagRegistry.WEATHER));
        if (weatherValue == null) {
            restoreWorldWeather(playerRef, world);
            return;
        }

        UUID uuid = playerRef.getUuid();
        if (weatherValue.equals(lastWeatherOverride.get(uuid))) {
            return;
        }
        sendWeatherPacket(playerRef, weatherValue);
        lastWeatherOverride.put(uuid, weatherValue);
    }

    private Plot resolveWeatherPlot(@Nonnull String worldName, int x, int z) {
        if (!plotManager.isInPlot(worldName, x, z)) {
            return null;
        }
        return plotManager.getPlotAt(worldName, x, z);
    }

    private void restoreWorldWeather(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID uuid = playerRef.getUuid();
        String worldWeather = normalizeWeatherFlag(world.getWorldConfig().getForcedWeather());
        String target = worldWeather != null ? worldWeather : "clear";
        if (target.equals(lastWeatherOverride.get(uuid))) {
            return;
        }
        sendWeatherPacket(playerRef, target);
        lastWeatherOverride.put(uuid, target);
    }

    private void sendWeatherPacket(@Nonnull PlayerRef playerRef, @Nonnull String weatherId) {
        int weatherIndex = resolveWeatherIndex(weatherId);
        if (weatherIndex < 0) {
            return;
        }
        playerRef.getPacketHandler().write(new UpdateWeather(weatherIndex, 0.3f));
    }

    private String normalizeWeatherFlag(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private int resolveWeatherIndex(String weatherId) {
        if (weatherId == null || weatherId.isBlank()) {
            return -1;
        }
        String wanted = weatherId.trim().toLowerCase();

        var map = Weather.getAssetMap();
        int direct = map.getIndex(wanted);
        if (direct >= 0) {
            return direct;
        }

        Map<String, Weather> assets = map.getAssetMap();
        if (assets == null || assets.isEmpty()) {
            return -1;
        }

        Set<String> keys = assets.keySet();
        for (String key : keys) {
            if (key != null && key.equalsIgnoreCase(wanted)) {
                int idx = map.getIndex(key);
                if (idx >= 0) {
                    return idx;
                }
            }
        }

        String[] aliases = weatherAliases(wanted);
        for (String alias : aliases) {
            for (String key : keys) {
                if (key == null) {
                    continue;
                }
                String lower = key.toLowerCase();
                if (lower.equals(alias) || lower.contains(alias)) {
                    int idx = map.getIndex(key);
                    if (idx >= 0) {
                        return idx;
                    }
                }
            }
        }

        String first = keys.iterator().next();
        return first != null ? map.getIndex(first) : -1;
    }

    private String[] weatherAliases(String wanted) {
        if ("clear".equals(wanted)) {
            return new String[] { "clear", "sun", "sunny", "default" };
        }
        if ("rain".equals(wanted)) {
            return new String[] { "rain", "drizzle" };
        }
        if ("storm".equals(wanted)) {
            return new String[] { "storm", "thunder", "tempest" };
        }
        return new String[] { wanted };
    }

    private void safeRequestHudUpdate(@Nonnull PlotInfoHUD hud) {
        try {
            hud.requestUpdate();
        } catch (Exception ignored) {
        }
    }

    private String sanitizeHudText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        String normalized = raw
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim();
        if (normalized.isEmpty()) {
            return "";
        }

        String noColorTags = HEX_COLOR_TAG.matcher(normalized).replaceAll("");
        String safe = escapeHudSegment(noColorTags).trim();
        return safe.length() > 96 ? safe.substring(0, 96) : safe;
    }

    private String escapeHudSegment(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return HEX_COLOR_TAG.matcher(input)
                .replaceAll("")
                .replace("{", "(")
                .replace("}", ")")
                .replace("%", "％");
    }

    private void attachHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull PlotInfoHUD hud) {
        Object manager = player.getHudManager();
        if (invokeHudMethod(manager, "addCustomHud", playerRef, hud)) {
            return;
        }
        invokeHudMethod(manager, "setCustomHud", playerRef, hud);
    }

    private void detachHud(@Nonnull Player player, @Nonnull PlayerRef playerRef, @Nonnull PlotInfoHUD hud) {
        Object manager = player.getHudManager();
        if (invokeHudMethod(manager, "removeCustomHud", playerRef, hud)) {
            return;
        }
        if (invokeHudMethod(manager, "removeCustomHud", playerRef)) {
            return;
        }
        invokeHudMethod(manager, "resetHud", playerRef);
    }

    private boolean invokeHudMethod(@Nonnull Object manager, @Nonnull String name, @Nonnull Object... args) {
        try {
            Method[] methods = manager.getClass().getMethods();
            for (Method method : methods) {
                if (!name.equals(method.getName()) || method.getParameterCount() != args.length) {
                    continue;
                }
                Class<?>[] types = method.getParameterTypes();
                boolean compatible = true;
                for (int i = 0; i < types.length; i++) {
                    if (args[i] != null && !types[i].isAssignableFrom(args[i].getClass())) {
                        compatible = false;
                        break;
                    }
                }
                if (!compatible) {
                    continue;
                }
                method.invoke(manager, args);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(Player.getComponentType(), PlayerRef.getComponentType());
    }
}
