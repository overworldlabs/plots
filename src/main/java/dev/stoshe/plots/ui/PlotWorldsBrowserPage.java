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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal browser listing the managed plot worlds with a teleport button per
 * world. Opened from {@code /plot worlds}.
 */
public class PlotWorldsBrowserPage extends InteractiveCustomUIPage<PlotWorldsBrowserPage.PageData> {

    private static final int ROWS = 10;

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private int page;

    /** World names shown on the current page (parallel to row indices). */
    private List<String> pageView = new ArrayList<>();

    private PlotWorldsBrowserPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            int page) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.page = Math.max(0, page);
    }

    /**
     * Opens the plot-worlds browser for a player.
     *
     * @param player      the viewing player
     * @param ref         the player entity reference
     * @param store       the entity store
     * @param playerRef   the player ref
     * @param sourceWorld the world the player is currently in
     * @param plugin      the plugin instance
     */
    public static void open(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef,
            World sourceWorld, Plots plugin) {
        if (player == null || ref == null || store == null || playerRef == null || sourceWorld == null
                || plugin == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotWorldsBrowserPage(playerRef, sourceWorld, plugin, 0));
    }

    private List<String> collectWorlds() {
        return plugin.getPlotManager().getConfig().getWorldNames();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotWorldsBrowser.ui");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Action", "Close"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WlBack",
                EventData.of("Action", "Back"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WlPrev",
                EventData.of("Action", "Prev"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WlNext",
                EventData.of("Action", "Next"), false);

        List<String> all = collectWorlds();
        int totalPages = Math.max(1, (all.size() + ROWS - 1) / ROWS);
        if (this.page >= totalPages) {
            this.page = totalPages - 1;
        }
        int start = this.page * ROWS;

        this.pageView = new ArrayList<>();
        commandBuilder.set("#WlEmpty.Visible", all.isEmpty());

        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx < all.size()) {
                String world = all.get(idx);
                this.pageView.add(world);
                commandBuilder.set("#WlRow" + i + ".Visible", true);
                commandBuilder.set("#WlRowName" + i + ".Text", world);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WlRowTp" + i,
                        EventData.of("Action", "WlTp").append("Param", String.valueOf(i)), false);
            } else {
                commandBuilder.set("#WlRow" + i + ".Visible", false);
            }
        }

        commandBuilder.set("#WlPageLabel.Text", "Page " + (this.page + 1) + " / " + totalPages);
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
            case "WlTp" -> {
                int index;
                try {
                    index = Integer.parseInt(data.param == null ? "" : data.param.trim());
                } catch (NumberFormatException ex) {
                    return;
                }
                if (index >= 0 && index < this.pageView.size()) {
                    String world = this.pageView.get(index);
                    this.plugin.getPlotManager().teleportPlayerToWorldSpawn(store, ref, world);
                    player.getPageManager().setPage(ref, store, Page.None);
                }
            }
            default -> {
            }
        }
    }

    private void reopen(Player player, Ref<EntityStore> ref, Store<EntityStore> store, int newPage) {
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotWorldsBrowserPage(this.playerRef, resolveWorld(player), this.plugin,
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

    /** Codec-backed payload for browser button events. */
    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Param", Codec.STRING), (o, v) -> o.param = v, o -> o.param).add()
                .build();
        public String action;
        public String param;
    }
}
