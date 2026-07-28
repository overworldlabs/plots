package dev.stoshe.plots.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.model.Plot;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Browser listing every public ({@code visit} flag enabled) plot warp, with
 * teleport buttons. Opened from the plot menu Warps tab or {@code /plot warps}.
 */
public class PlotWarpsBrowserPage extends InteractiveCustomUIPage<PlotWarpsBrowserPage.PageData> {

    private static final int ROWS = 10;

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private int page;

    /** Warp entries shown on the current page (parallel to row indices). */
    private List<Entry> pageView = new ArrayList<>();

    private PlotWarpsBrowserPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            int page) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.page = Math.max(0, page);
    }

    public static void open(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef,
            World sourceWorld, Plots plugin) {
        if (player == null || ref == null || store == null || playerRef == null || sourceWorld == null
                || plugin == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotWarpsBrowserPage(playerRef, sourceWorld, plugin, 0));
    }

    /** A single public warp row: its display label, owning world and warp data. */
    private static final class Entry {
        final String label;
        final String world;
        final Plot.PlotWarp warp;

        Entry(String label, String world, Plot.PlotWarp warp) {
            this.label = label;
            this.world = world;
            this.warp = warp;
        }
    }

    private List<Entry> collectPublicWarps() {
        List<Entry> entries = new ArrayList<>();
        try {
            for (Plot plot : this.plugin.getPlotManager().getAllPlots()) {
                if (plot == null || !plot.getFlagValue(FlagRegistry.VISIT)) {
                    continue;
                }
                String plotName = plot.getName() != null ? plot.getName() : "Plot";
                for (Plot.PlotWarp w : plot.getWarps()) {
                    entries.add(new Entry(plotName + " — " + (w.name != null ? w.name : ""), plot.getWorld(), w));
                }
            }
        } catch (Exception ignored) {
        }
        entries.sort(Comparator.comparing(e -> e.label.toLowerCase()));
        return entries;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotWarpsBrowser.ui");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Action", "Close"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WbBack",
                EventData.of("Action", "Back"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WbPrev",
                EventData.of("Action", "Prev"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WbNext",
                EventData.of("Action", "Next"), false);

        List<Entry> all = collectPublicWarps();
        int totalPages = Math.max(1, (all.size() + ROWS - 1) / ROWS);
        if (this.page >= totalPages) {
            this.page = totalPages - 1;
        }
        int start = this.page * ROWS;

        this.pageView = new ArrayList<>();
        commandBuilder.set("#WbEmpty.Visible", all.isEmpty());

        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx < all.size()) {
                Entry e = all.get(idx);
                this.pageView.add(e);
                commandBuilder.set("#WbRow" + i + ".Visible", true);
                commandBuilder.set("#WbRowName" + i + ".Text", e.label);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WbRowTp" + i,
                        EventData.of("Action", "WbTp").append("Param", String.valueOf(i)), false);
            } else {
                commandBuilder.set("#WbRow" + i + ".Visible", false);
            }
        }

        commandBuilder.set("#WbPageLabel.Text", "Page " + (this.page + 1) + " / " + totalPages);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        String action = data.action == null ? "" : data.action.trim();
        switch (action) {
            case "Close" -> player.getPageManager().setPage(ref, store, Page.None);
            case "Back" -> PlotMenuPage.open(player, ref, store, this.playerRef, resolveWorld(player), this.plugin);
            case "Prev" -> reopen(player, ref, store, this.page - 1);
            case "Next" -> reopen(player, ref, store, this.page + 1);
            case "WbTp" -> {
                int index;
                try {
                    index = Integer.parseInt(data.param == null ? "" : data.param.trim());
                } catch (NumberFormatException ex) {
                    return;
                }
                if (index >= 0 && index < this.pageView.size()) {
                    Entry entry = this.pageView.get(index);
                    this.plugin.getPlotManager().teleportPlayerToWarp(store, ref, entry.world, entry.warp);
                    player.getPageManager().setPage(ref, store, Page.None);
                }
            }
            default -> {
            }
        }
    }

    private void reopen(Player player, Ref<EntityStore> ref, Store<EntityStore> store, int newPage) {
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotWarpsBrowserPage(this.playerRef, resolveWorld(player), this.plugin,
                        Math.max(0, newPage)));
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

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Param", Codec.STRING), (o, v) -> o.param = v, o -> o.param).add()
                .build();
        public String action;
        public String param;
    }
}
