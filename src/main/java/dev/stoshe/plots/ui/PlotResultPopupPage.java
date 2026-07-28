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
import dev.stoshe.plots.util.Tr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Reusable success/error result modal shown after save/update actions in the
 * plot and admin menus. Clicking OK returns to the originating dashboard so the
 * player keeps their place after confirming the outcome.
 */
public class PlotResultPopupPage extends InteractiveCustomUIPage<PlotResultPopupPage.PageData> {

    /** Where OK returns the player. */
    public enum Return {
        NONE, PLOT_MENU, ADMIN
    }

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private final boolean success;
    private final String message;
    private final Return returnTo;
    private final String tabId;

    private PlotResultPopupPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            boolean success, @Nonnull String message, @Nonnull Return returnTo, @Nullable String tabId) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.success = success;
        this.message = message;
        this.returnTo = returnTo;
        this.tabId = tabId;
    }

    /** Opens the modal, replacing the current page. */
    public static void show(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef,
            World world, Plots plugin, boolean success, String message, Return returnTo, String tabId) {
        if (player == null || ref == null || store == null || playerRef == null || world == null || plugin == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage) new PlotResultPopupPage(playerRef, world,
                plugin, success, safe(message), returnTo != null ? returnTo : Return.NONE, tabId));
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotResultPopup.ui");
        commandBuilder.set("#ResultPopupTitle.Text",
                this.success ? Tr.t("ui.common.success") : Tr.t("ui.common.error"));
        commandBuilder.set("#ResultPopupMessage.Text", safe(this.message));
        commandBuilder.set("#ResultAccentSuccess.Visible", this.success);
        commandBuilder.set("#ResultAccentError.Visible", !this.success);
        commandBuilder.set("#BtnResultOk.Text", Tr.t("ui.common.ok"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnResultOk",
                EventData.of("Action", "Ok"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        World world = resolveWorld(player);
        switch (this.returnTo) {
            case PLOT_MENU -> PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin, this.tabId);
            case ADMIN -> PlotAdminPage.open(player, ref, store, this.playerRef, world, this.plugin);
            default -> player.getPageManager().setPage(ref, store, Page.None);
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
                .build();
        public String action;
    }
}
