package dev.stoshe.plots.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;
import dev.stoshe.plots.util.Tr;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Weather picker popup for the plot menu. Lists every weather asset id available
 * on the server and lets the owner apply one to the plot (stored on the
 * {@code WEATHER} flag, applied by {@code PlotNotificationSystem}).
 */
public class PlotWeatherPickerPage extends InteractiveCustomUIPage<PlotWeatherPickerPage.PageData> {

    private static final int MAX_ROWS = 10;

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private int page;

    /** Page-ordered view of weather ids rendered on this page (parallel to row indices). */
    private List<String> view = new ArrayList<>();

    private PlotWeatherPickerPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            int page) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.page = Math.max(0, page);
    }

    public static PlotWeatherPickerPage create(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld,
            @Nonnull Plots plugin) {
        return new PlotWeatherPickerPage(playerRef, sourceWorld, plugin, 0);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotWeatherPicker.ui");
        commandBuilder.set("#WeatherTitle.Text", Tr.t("ui.weather.title"));
        commandBuilder.set("#WeatherSubtitle.Text", Tr.t("ui.weather.subtitle"));
        commandBuilder.set("#WeatherBackButton.Text", Tr.t("ui.common.back"));
        commandBuilder.set("#WeatherPrev.Text", Tr.t("ui.common.prev"));
        commandBuilder.set("#WeatherNext.Text", Tr.t("ui.common.next"));

        Plot plot = resolvePlot(ref, store);
        String current = plot != null ? safe(plot.getFlagValue(FlagRegistry.WEATHER)) : "";
        commandBuilder.set("#WeatherCurrent.Text", Tr.t("ui.weather.current", "weather",
                current.isBlank() ? Tr.t("ui.weather.default") : current));

        List<String> all = listWeatherIds();
        commandBuilder.set("#WeatherEmpty.Visible", all.isEmpty());

        int total = all.size();
        int maxPage = total == 0 ? 0 : (total - 1) / MAX_ROWS;
        if (this.page > maxPage) {
            this.page = maxPage;
        }
        int start = this.page * MAX_ROWS;

        this.view = new ArrayList<>();
        for (int i = 0; i < MAX_ROWS; i++) {
            int srcIndex = start + i;
            if (srcIndex < total) {
                String id = all.get(srcIndex);
                this.view.add(id);
                boolean isCurrent = id.equalsIgnoreCase(current);
                commandBuilder.set("#WeatherRow" + i + ".Visible", true);
                commandBuilder.set("#WeatherName" + i + ".Text", isCurrent ? id + "  ✓" : id);
                commandBuilder.set("#WeatherPick" + i + ".Text",
                        isCurrent ? Tr.t("ui.weather.active") : Tr.t("ui.common.select"));
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherPick" + i,
                        EventData.of("Action", "Select").append("Param", String.valueOf(i)), false);
            } else {
                commandBuilder.set("#WeatherRow" + i + ".Visible", false);
            }
        }

        commandBuilder.set("#WeatherPageLabel.Text", total == 0
                ? Tr.t("ui.weather.none")
                : Tr.t("ui.common.page_of", "page", String.valueOf(this.page + 1), "total", String.valueOf(maxPage + 1)));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherPrev",
                EventData.of("Action", "PrevPage"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherNext",
                EventData.of("Action", "NextPage"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WeatherBackButton",
                EventData.of("Action", "Back"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        String action = safe(data.action).trim();
        World world = resolveWorld(player);
        switch (action) {
            case "Back" -> PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin, "Flags");
            case "PrevPage" -> reopen(player, ref, store, this.page - 1);
            case "NextPage" -> reopen(player, ref, store, this.page + 1);
            case "Select" -> handleSelect(player, ref, store, world, data.param);
            default -> reopen(player, ref, store, this.page);
        }
    }

    private void handleSelect(Player player, Ref<EntityStore> ref, Store<EntityStore> store, World world,
            String param) {
        Plot plot = resolvePlot(ref, store);
        if (plot == null) {
            player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("ui.menu.notice.not_on_plot")));
            PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin, "Flags");
            return;
        }
        if (!canManage(plot)) {
            player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("management.no_permission")));
            PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin, "Flags");
            return;
        }
        int index;
        try {
            index = Integer.parseInt(safe(param).trim());
        } catch (NumberFormatException ex) {
            reopen(player, ref, store, this.page);
            return;
        }
        if (index < 0 || index >= this.view.size()) {
            reopen(player, ref, store, this.page);
            return;
        }
        String id = this.view.get(index);
        plot.setFlagValue(FlagRegistry.WEATHER, id);
        this.plugin.getPlotManager().savePlots();
        player.getPlayerRef().sendMessage(ChatUtil.success(Tr.t("ui.weather.set", "weather", id)));
        // Stay on the weather list so the new selection is reflected (Active marker).
        reopen(player, ref, store, this.page);
    }

    private List<String> listWeatherIds() {
        List<String> result = new ArrayList<>();
        try {
            Map<String, Weather> assets = Weather.getAssetMap().getAssetMap();
            if (assets != null) {
                // Sorted, case-insensitive, de-duplicated for a stable list.
                TreeSet<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (String key : assets.keySet()) {
                    if (key != null && !key.isBlank()) {
                        sorted.add(key);
                    }
                }
                result.addAll(sorted);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private void reopen(Player player, Ref<EntityStore> ref, Store<EntityStore> store, int page) {
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage) new PlotWeatherPickerPage(this.playerRef,
                resolveWorld(player), this.plugin, Math.max(0, page)));
    }

    private boolean canManage(Plot plot) {
        UUID uuid = this.playerRef.getUuid();
        if (uuid == null || plot == null) {
            return false;
        }
        if (uuid.equals(plot.getOwner())) {
            return true;
        }
        try {
            return PermissionUtil.hasAdminPermission(uuid);
        } catch (Exception ex) {
            return false;
        }
    }

    private Plot resolvePlot(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            int x;
            int z;
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform != null && transform.getPosition() != null) {
                x = (int) Math.floor(transform.getPosition().x);
                z = (int) Math.floor(transform.getPosition().z);
            } else {
                Transform t = this.playerRef.getTransform();
                if (t == null || t.getPosition() == null) {
                    return null;
                }
                x = (int) Math.floor(t.getPosition().x);
                z = (int) Math.floor(t.getPosition().z);
            }
            return this.plugin.getPlotManager().getPlotAt(this.sourceWorld.getName(), x, z);
        } catch (Exception ex) {
            return null;
        }
    }

    private World resolveWorld(Player player) {
        try {
            if (player != null && player.getWorld() != null) {
                return player.getWorld();
            }
        } catch (Exception ignored) {
        }
        return this.sourceWorld;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Param", Codec.STRING), (o, v) -> o.param = v, o -> o.param).add()
                .build();
        public String action;
        public String param;
    }
}
