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
import dev.stoshe.plots.api.IPlotRepository;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin modal listing the configured plot worlds, with buttons to create a new
 * world and to delete an existing one (with an inline confirmation step).
 */
public class PlotAdminWorldsPage extends InteractiveCustomUIPage<PlotAdminWorldsPage.PageData> {

    private static final int ROWS = 10;

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private final int page;
    /** World pending a delete confirmation, or {@code null} when none. */
    private final String pendingDelete;

    /** World names shown on the current page (parallel to row indices). */
    private List<String> pageView = new ArrayList<>();

    private PlotAdminWorldsPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            int page, @Nullable String pendingDelete) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.page = Math.max(0, page);
        this.pendingDelete = pendingDelete;
    }

    /**
     * Opens the admin worlds-management modal.
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
                (CustomUIPage) new PlotAdminWorldsPage(playerRef, sourceWorld, plugin, 0, null));
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotAdminWorlds.ui");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Action", "Close"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WaBack",
                EventData.of("Action", "Back"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WaCreate",
                EventData.of("Action", "Create"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WaPrev",
                EventData.of("Action", "PrevPage"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WaNext",
                EventData.of("Action", "NextPage"), false);

        boolean confirming = this.pendingDelete != null;
        commandBuilder.set("#WaConfirmBar.Visible", confirming);
        if (confirming) {
            commandBuilder.set("#WaConfirmText.Text", "Delete world '" + this.pendingDelete + "' and its plots?");
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WaConfirmNo",
                    EventData.of("Action", "ConfirmNo"), false);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WaConfirmYes",
                    EventData.of("Action", "ConfirmYes"), false);
        }

        List<String> all = this.plugin.getConfig().getWorldNames();
        int totalPages = Math.max(1, (all.size() + ROWS - 1) / ROWS);
        int currentPage = Math.min(this.page, totalPages - 1);
        int start = currentPage * ROWS;
        this.pageView = new ArrayList<>();
        commandBuilder.set("#WaEmpty.Visible", all.isEmpty());

        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx < all.size()) {
                String world = all.get(idx);
                this.pageView.add(world);
                commandBuilder.set("#WaRow" + i + ".Visible", true);
                commandBuilder.set("#WaRowName" + i + ".Text", world);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#WaRowDelete" + i,
                        EventData.of("Action", "Delete").append("Param", String.valueOf(i)), false);
            } else {
                commandBuilder.set("#WaRow" + i + ".Visible", false);
            }
        }

        commandBuilder.set("#WaPageLabel.Text", "Page " + (currentPage + 1) + " / " + totalPages);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        String action = data.action == null ? "" : data.action.trim();
        World world = resolveWorld(player);

        switch (action) {
            case "Close" -> player.getPageManager().setPage(ref, store, Page.None);
            case "Back" -> PlotAdminPage.open(player, ref, store, this.playerRef, world, this.plugin);
            case "Create" -> PlotWorldCreatePopupPage.open(player, ref, store, this.playerRef, world, this.plugin);
            case "PrevPage" -> reopenAtPage(player, ref, store, this.page - 1);
            case "NextPage" -> reopenAtPage(player, ref, store, this.page + 1);
            case "Delete" -> reopen(player, ref, store, worldForParam(data.param));
            case "ConfirmNo" -> reopen(player, ref, store, null);
            case "ConfirmYes" -> {
                deletePendingWorld(player);
                reopen(player, ref, store, null);
            }
            default -> {
            }
        }
    }

    @Nullable
    private String worldForParam(@Nullable String param) {
        try {
            int index = Integer.parseInt(param == null ? "" : param.trim());
            if (index >= 0 && index < this.pageView.size()) {
                return this.pageView.get(index);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /** Removes the pending world from config and deletes its plots. */
    private void deletePendingWorld(Player player) {
        if (this.pendingDelete == null) {
            return;
        }
        PlotConfig config = this.plugin.getConfig();
        // Keep at least one world: an empty Worlds map is re-seeded with a default
        // "plotworld" on the next save, which would silently resurrect a world.
        if (config.getWorldNames().size() <= 1) {
            player.getPlayerRef().sendMessage(ChatUtil.error(
                    "Cannot delete the only plot world — create another first."));
            return;
        }
        IPlotRepository repository = this.plugin.getPlotRepository();
        if (repository != null) {
            for (Plot plot : repository.getPlotsInWorld(this.pendingDelete)) {
                repository.deletePlot(plot.getWorld(), plot.getGridX(), plot.getGridZ());
            }
            repository.saveAll();
        }
        config.removeWorld(this.pendingDelete);
        this.plugin.saveConfig(config);
        player.getPlayerRef().sendMessage(ChatUtil.success("Removed plot world '" + this.pendingDelete
                + "' from config (the loaded world is unloaded on next restart)."));
    }

    private void reopen(Player player, Ref<EntityStore> ref, Store<EntityStore> store, @Nullable String pending) {
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotAdminWorldsPage(this.playerRef, resolveWorld(player), this.plugin, this.page,
                        pending));
    }

    private void reopenAtPage(Player player, Ref<EntityStore> ref, Store<EntityStore> store, int newPage) {
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotAdminWorldsPage(this.playerRef, resolveWorld(player), this.plugin,
                        Math.max(0, newPage), null));
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

    /** Codec-backed payload for worlds-management button events. */
    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Param", Codec.STRING), (o, v) -> o.param = v, o -> o.param).add()
                .build();
        public String action;
        public String param;
    }
}
