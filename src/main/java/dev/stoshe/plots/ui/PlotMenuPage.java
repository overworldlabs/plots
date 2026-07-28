package dev.stoshe.plots.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.flag.MixinRequiredFlags;
import dev.stoshe.plots.flag.PlotFlag;
import dev.stoshe.plots.flag.types.BooleanFlag;
import dev.stoshe.plots.integration.mixin.MixinBridgeStatus;
import dev.stoshe.plots.manager.PlotManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;
import dev.stoshe.plots.util.Tr;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Full tabbed plot management menu (Alt-key / {@code /plot menu}).
 */
public class PlotMenuPage extends InteractiveCustomUIPage<PlotMenuPage.PageData> {

    private static final int MAX_TRUST_ROWS = 12;
    private static final int MAX_WARP_ROWS = 10;

    private enum Tab {
        TRUST, FLAGS, WARPS, MERGE, DANGER
    }

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private Tab currentTab;

    /** Cached trusted UUIDs for the rendered TRUST tab (parallel to row indices). */
    private List<UUID> trustedView = new ArrayList<>();
    /** Cached warps for the rendered WARPS tab (parallel to row indices). */
    private List<Plot.PlotWarp> warpView = new ArrayList<>();

    private PlotMenuPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            @Nonnull Tab tab) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.currentTab = tab;
    }

    public static void open(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef,
            World sourceWorld, Plots plugin) {
        if (player == null || ref == null || store == null || playerRef == null || sourceWorld == null
                || plugin == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotMenuPage(playerRef, sourceWorld, plugin, Tab.TRUST));
    }

    /** Opens the menu on a specific tab (used when returning from popups). */
    public static void open(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef,
            World sourceWorld, Plots plugin, String tabId) {
        if (player == null || ref == null || store == null || playerRef == null || sourceWorld == null
                || plugin == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotMenuPage(playerRef, sourceWorld, plugin, tabFromId(tabId)));
    }

    private static Tab tabFromId(String tabId) {
        if (tabId == null) {
            return Tab.TRUST;
        }
        return switch (tabId.trim().toLowerCase()) {
            case "flags" -> Tab.FLAGS;
            case "warps" -> Tab.WARPS;
            case "merge" -> Tab.MERGE;
            case "danger" -> Tab.DANGER;
            default -> Tab.TRUST;
        };
    }

    // ===================================================================== build

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotMenu.ui");

        // Aero-style sidebar nav; both variants of each entry trigger the same tab
        // switch. The container's top-right close (#CloseButton) is dismissed natively
        // by the client — binding it would break that, so we don't.
        bindNav(eventBuilder, "#TabTrust", "#TabTrustActive", "TabTrust");
        bindNav(eventBuilder, "#TabFlags", "#TabFlagsActive", "TabFlags");
        bindNav(eventBuilder, "#TabWarps", "#TabWarpsActive", "TabWarps");
        bindNav(eventBuilder, "#TabMerge", "#TabMergeActive", "TabMerge");
        bindNav(eventBuilder, "#TabDanger", "#TabDangerActive", "TabDanger");

        Plot plot = resolvePlot(ref, store);
        boolean canManage = plot != null && canManage(plot);

        // Sidebar labels + active/inactive highlight for the current section.
        setNavLabel(commandBuilder, "#TabTrust", "ui.menu.nav.trust");
        setNavLabel(commandBuilder, "#TabFlags", "ui.menu.nav.flags");
        setNavLabel(commandBuilder, "#TabWarps", "ui.menu.nav.warps");
        setNavLabel(commandBuilder, "#TabMerge", "ui.menu.nav.merge");
        setNavLabel(commandBuilder, "#TabDanger", "ui.menu.nav.danger");

        setNavActive(commandBuilder, "#TabTrust", this.currentTab == Tab.TRUST);
        setNavActive(commandBuilder, "#TabFlags", this.currentTab == Tab.FLAGS);
        setNavActive(commandBuilder, "#TabWarps", this.currentTab == Tab.WARPS);
        setNavActive(commandBuilder, "#TabMerge", this.currentTab == Tab.MERGE);
        setNavActive(commandBuilder, "#TabDanger", this.currentTab == Tab.DANGER);

        commandBuilder.set("#RightTitle.Text", switch (this.currentTab) {
            case TRUST -> Tr.t("ui.menu.title.trust");
            case FLAGS -> Tr.t("ui.menu.title.flags");
            case WARPS -> Tr.t("ui.menu.title.warps");
            case MERGE -> Tr.t("ui.menu.title.merge");
            case DANGER -> Tr.t("ui.menu.title.danger");
        });

        commandBuilder.set("#PanelTrust.Visible", this.currentTab == Tab.TRUST);
        commandBuilder.set("#PanelFlags.Visible", this.currentTab == Tab.FLAGS);
        commandBuilder.set("#PanelWarps.Visible", this.currentTab == Tab.WARPS);
        commandBuilder.set("#PanelMerge.Visible", this.currentTab == Tab.MERGE);
        commandBuilder.set("#PanelDanger.Visible", this.currentTab == Tab.DANGER);

        boolean notice = plot == null || !canManage;
        commandBuilder.set("#PermNotice.Visible", notice);
        if (plot == null) {
            commandBuilder.set("#PermNotice.Text", Tr.t("ui.menu.notice.not_on_plot"));
        } else if (!canManage) {
            commandBuilder.set("#PermNotice.Text", Tr.t("ui.menu.notice.read_only"));
        } else {
            commandBuilder.set("#PermNotice.Text", "");
        }

        buildHero(commandBuilder, eventBuilder, plot, canManage);
        buildTrust(commandBuilder, eventBuilder, plot, canManage);
        buildFlags(commandBuilder, eventBuilder, plot, canManage);
        buildWarps(commandBuilder, eventBuilder, plot, canManage);
        buildMerge(commandBuilder, eventBuilder, plot, canManage);
        buildDanger(commandBuilder, eventBuilder, plot, canManage);
    }

    /** Both variants of a sidebar entry open the same section, so both are bound. */
    private void bindNav(UIEventBuilder eb, String inactiveId, String activeId, String action) {
        eb.addEventBinding(CustomUIEventBindingType.Activating, inactiveId, EventData.of("Action", action), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, activeId, EventData.of("Action", action), false);
    }

    private void setNavLabel(UICommandBuilder cb, String inactiveId, String translationKey) {
        String text = Tr.t(translationKey);
        cb.set(inactiveId + ".Text", text);
        cb.set(inactiveId + "Active.Text", text);
    }

    /** Shows the highlighted variant of the current section and the plain one for the rest. */
    private void setNavActive(UICommandBuilder cb, String inactiveId, boolean active) {
        cb.set(inactiveId + "Active.Visible", active);
        cb.set(inactiveId + ".Visible", !active);
    }

    private void buildWarps(UICommandBuilder cb, UIEventBuilder eb, Plot plot, boolean canManage) {
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#WarpBrowse",
                EventData.of("Action", "OpenWarpsBrowser"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#WarpAdd",
                EventData.of("Action", "OpenAddWarp"), false);

        int max = this.plugin.getPlotManager().getMaxWarpsPerPlot();
        java.util.List<Plot.PlotWarp> warps = plot != null ? plot.getWarps() : new ArrayList<>();
        this.warpView = warps;
        cb.set("#WarpCount.Text",
                Tr.t("ui.menu.warps.count", "count", String.valueOf(warps.size()), "max", String.valueOf(max)));
        cb.set("#WarpEmpty.Visible", warps.isEmpty());

        for (int i = 0; i < MAX_WARP_ROWS; i++) {
            if (i < warps.size()) {
                Plot.PlotWarp w = warps.get(i);
                cb.set("#WarpRow" + i + ".Visible", true);
                cb.set("#WarpName" + i + ".Text", safe(w.name));
                eb.addEventBinding(CustomUIEventBindingType.Activating, "#WarpTp" + i,
                        EventData.of("Action", "WarpTp").append("Param", String.valueOf(i)), false);
                eb.addEventBinding(CustomUIEventBindingType.Activating, "#WarpDel" + i,
                        EventData.of("Action", "WarpDel").append("Param", String.valueOf(i)), false);
            } else {
                cb.set("#WarpRow" + i + ".Visible", false);
            }
        }
    }

    /**
     * Populates the always-visible left "hero" panel (plot identity, size, stat
     * grid and quick-action buttons). Runs on every build regardless of the
     * selected tab.
     */
    private void buildHero(UICommandBuilder cb, UIEventBuilder eb, Plot plot, boolean canManage) {
        // Quick actions live in the hero and are always wired; the mutating ones
        // are still gated by canManage in handleDataEvent.
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnHeroRename",
                EventData.of("Action", "OpenRename"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnHeroSetSpawn",
                EventData.of("Action", "SetSpawn"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnHeroTeleport",
                EventData.of("Action", "Teleport"), false);
        cb.set("#BtnHeroTeleport.Text", Tr.t("ui.menu.hero.teleport"));
        cb.set("#BtnHeroSetSpawn.Text", Tr.t("ui.menu.hero.set_spawn"));
        cb.set("#BtnHeroRename.Text", Tr.t("ui.menu.hero.rename"));

        int sizeX = this.plugin.getPlotManager().getConfig().getPlotSizeX();
        int sizeZ = this.plugin.getPlotManager().getConfig().getPlotSizeZ();
        cb.set("#HeroSizeA.Text", String.valueOf(sizeX));
        cb.set("#HeroSizeB.Text", String.valueOf(sizeZ));

        if (plot == null) {
            cb.set("#HeroPlotName.Text", "-");
            cb.set("#HeroOwner.Text", "-");
            cb.set("#HeroTrusted.Text", "0");
            cb.set("#HeroWarps.Text", "0");
            cb.set("#HeroMerged.Text", "0");
            cb.set("#HeroCreated.Text", "-");
            return;
        }
        cb.set("#HeroPlotName.Text", safe(plot.getName()));
        cb.set("#HeroOwner.Text", safe(plot.getOwnerName()));
        cb.set("#HeroTrusted.Text", String.valueOf(plot.getTrustedPlayers().size()));
        cb.set("#HeroWarps.Text", String.valueOf(plot.getWarpCount()));
        cb.set("#HeroMerged.Text", String.valueOf(plot.getMergedPlots().size()));
        cb.set("#HeroCreated.Text", formatDate(plot.getCreatedAt()));
    }

    private void buildTrust(UICommandBuilder cb, UIEventBuilder eb, Plot plot, boolean canManage) {
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnAddTrust",
                EventData.of("Action", "OpenAddTrust"), false);

        this.trustedView = plot == null ? new ArrayList<>() : plot.getTrustedPlayers();
        cb.set("#TrustEmpty.Visible", this.trustedView.isEmpty());

        for (int i = 0; i < MAX_TRUST_ROWS; i++) {
            if (i < this.trustedView.size()) {
                UUID uuid = this.trustedView.get(i);
                cb.set("#TrustRow" + i + ".Visible", true);
                cb.set("#TrustName" + i + ".Text", resolveName(uuid));
                eb.addEventBinding(CustomUIEventBindingType.Activating, "#TrustRemove" + i,
                        EventData.of("Action", "RemoveTrust").append("Param", String.valueOf(i)), false);
            } else {
                cb.set("#TrustRow" + i + ".Visible", false);
            }
        }
    }

    private void buildFlags(UICommandBuilder cb, UIEventBuilder eb, Plot plot, boolean canManage) {
        if (plot == null) {
            return;
        }
        // Boolean toggles: reflect current values and apply each change instantly
        // (a single Save button does not reliably capture checkbox state in this
        // engine, so every checkbox pushes its own ValueChanged event).
        bindToggle(cb, eb, "#FlagBlockBreak", plot, FlagRegistry.BLOCK_BREAK);
        bindToggle(cb, eb, "#FlagBlockPlace", plot, FlagRegistry.BLOCK_PLACE);
        bindToggle(cb, eb, "#FlagInteract", plot, FlagRegistry.INTERACT);
        bindToggle(cb, eb, "#FlagCrafting", plot, FlagRegistry.CRAFTING);
        bindToggle(cb, eb, "#FlagEntry", plot, FlagRegistry.ENTRY);
        bindToggle(cb, eb, "#FlagExit", plot, FlagRegistry.EXIT);
        bindToggle(cb, eb, "#FlagVisit", plot, FlagRegistry.VISIT);
        bindToggle(cb, eb, "#FlagPvp", plot, FlagRegistry.PVP);
        bindToggle(cb, eb, "#FlagMobDamage", plot, FlagRegistry.MOB_DAMAGE);
        bindToggle(cb, eb, "#FlagMobSpawning", plot, FlagRegistry.MOB_SPAWNING);
        bindToggle(cb, eb, "#FlagItemDrop", plot, FlagRegistry.ITEM_DROP);
        bindToggle(cb, eb, "#FlagItemPickup", plot, FlagRegistry.ITEM_PICKUP);
        bindToggle(cb, eb, "#FlagKeepInventory", plot, FlagRegistry.KEEP_INVENTORY);
        bindToggle(cb, eb, "#FlagExplosions", plot, FlagRegistry.EXPLOSIONS);
        bindToggle(cb, eb, "#FlagPlayerChat", plot, FlagRegistry.PLAYER_CHAT);

        // Bridge-only flags are hidden entirely when TaleGuard is not available,
        // so the menu never shows a toggle the server can't actually enforce.
        boolean bridge = MixinBridgeStatus.isReadyForMixinFlags();
        cb.set("#FlagExplosionsRow.Visible", bridge);
        cb.set("#FlagMobSpawningRow.Visible", bridge);
        cb.set("#FlagItemPickupRow.Visible", bridge);
        cb.set("#FlagKeepInventoryRow.Visible", bridge);

        // String inputs are captured on the dedicated Save Messages button.
        cb.set("#FlagGreetMessage.Value", safe(plot.getFlagValue(FlagRegistry.GREET_MESSAGE)));
        cb.set("#FlagFarewellMessage.Value", safe(plot.getFlagValue(FlagRegistry.FAREWELL_MESSAGE)));
        cb.set("#FlagDenyMessage.Value", safe(plot.getFlagValue(FlagRegistry.DENY_MESSAGE)));

        EventData saveMsgs = EventData.of("Action", "SaveMessages")
                .append("@GreetMessage", "#FlagGreetMessage.Value")
                .append("@FarewellMessage", "#FlagFarewellMessage.Value")
                .append("@DenyMessage", "#FlagDenyMessage.Value");
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSaveMessages", saveMsgs, false);
        cb.set("#BtnSaveMessages.Text", Tr.t("ui.menu.flags.save_messages"));

        // Weather selector (opens the weather picker popup).
        cb.set("#FlagEnvironmentLabel.Text", Tr.t("ui.menu.section.environment"));
        cb.set("#FlagWeatherLabel.Text", Tr.t("ui.weather.label"));
        String weather = safe(plot.getFlagValue(FlagRegistry.WEATHER));
        cb.set("#FlagWeatherValue.Text", weather.isBlank() ? Tr.t("ui.weather.default") : weather);
        cb.set("#BtnWeatherChange.Text", Tr.t("ui.weather.change"));
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWeatherChange",
                EventData.of("Action", "OpenWeather"), false);
    }

    /** Reflects the flag's current value on its checkbox and wires its instant-apply toggle. */
    private void bindToggle(UICommandBuilder cb, UIEventBuilder eb, String checkboxId, Plot plot, BooleanFlag flag) {
        cb.set(checkboxId + ".Value", Boolean.TRUE.equals(plot.getFlagValue(flag)));
        eb.addEventBinding(CustomUIEventBindingType.ValueChanged, checkboxId,
                EventData.of("Action", "ToggleFlag")
                        .append("Param", flag.getName())
                        .append("@FlagValue", checkboxId + ".Value"),
                false);
    }

    private void buildMerge(UICommandBuilder cb, UIEventBuilder eb, Plot plot, boolean canManage) {
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnMergeNorth",
                EventData.of("Action", "Merge").append("Param", "north"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnMergeSouth",
                EventData.of("Action", "Merge").append("Param", "south"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnMergeEast",
                EventData.of("Action", "Merge").append("Param", "east"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnMergeWest",
                EventData.of("Action", "Merge").append("Param", "west"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnUnmerge",
                EventData.of("Action", "Unmerge"), false);
    }

    private void buildDanger(UICommandBuilder cb, UIEventBuilder eb, Plot plot, boolean canManage) {
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTransfer",
                EventData.of("Action", "OpenTransfer"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnDelete",
                EventData.of("Action", "OpenDelete"), false);
    }

    // =============================================================== event handling

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        String action = safe(data.action).trim();
        if (action.isEmpty()) {
            return;
        }

        switch (action) {
            case "Close" -> {
                player.getPageManager().setPage(ref, store, Page.None);
                return;
            }
            case "TabTrust" -> {
                reopen(player, ref, store, Tab.TRUST);
                return;
            }
            case "TabFlags" -> {
                reopen(player, ref, store, Tab.FLAGS);
                return;
            }
            case "TabMerge" -> {
                reopen(player, ref, store, Tab.MERGE);
                return;
            }
            case "TabDanger" -> {
                reopen(player, ref, store, Tab.DANGER);
                return;
            }
            case "TabWarps" -> {
                reopen(player, ref, store, Tab.WARPS);
                return;
            }
            case "OpenWarpsBrowser" -> {
                PlotWarpsBrowserPage.open(player, ref, store, this.playerRef, resolveWorld(player), this.plugin);
                return;
            }
        }

        Plot plot = resolvePlot(ref, store);
        if (plot == null) {
            reopen(player, ref, store, this.currentTab);
            return;
        }
        boolean canManage = canManage(plot);

        // TEMP DIAGNOSTIC: trace exactly why a mutation is allowed/blocked.
        try {
            UUID viewer = this.playerRef.getUuid();
            Object owner = plot.getOwner();
            boolean isOwner = viewer != null && viewer.equals(owner);
            boolean admin = viewer != null && PermissionUtil.hasAdminPermission(viewer);
            dev.stoshe.plots.util.ConsoleColors.warning("[PlotsPermDebug] action=" + action
                    + " viewer=" + viewer + " owner=" + owner + " ownerName=" + plot.getOwnerName()
                    + " isOwner=" + isOwner + " hasAdmin=" + admin + " canManage=" + canManage);
        } catch (Exception ignored) {
        }

        // Mutating actions require management permission.
        if (!canManage) {
            player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("management.no_permission")));
            reopen(player, ref, store, this.currentTab);
            return;
        }

        switch (action) {
            case "OpenRename" -> player.getPageManager().openCustomPage(ref, store,
                    (CustomUIPage) PlotRenamePopupPage.create(this.playerRef, resolveWorld(player), this.plugin));
            case "SetSpawn" -> {
                Outcome o = handleSetSpawn(player, ref, store, plot);
                PlotResultPopupPage.show(player, ref, store, this.playerRef, resolveWorld(player), this.plugin,
                        o.ok(), o.message(), PlotResultPopupPage.Return.PLOT_MENU, currentTabId());
            }
            case "Teleport" -> {
                this.plugin.getPlotManager().teleportPlayerToPlot(store, ref, plot);
                player.getPageManager().setPage(ref, store, Page.None);
            }
            case "OpenAddTrust" -> player.getPageManager().openCustomPage(ref, store,
                    (CustomUIPage) PlotPlayerPickerPage.create(this.playerRef, resolveWorld(player), this.plugin,
                            PlotPlayerPickerPage.Mode.TRUST));
            case "RemoveTrust" -> {
                handleRemoveTrust(player, ref, store, plot, data.param);
                reopen(player, ref, store, Tab.TRUST);
            }
            case "ToggleFlag" -> {
                boolean forcedOff = handleToggleFlag(plot, data);
                if (forcedOff) {
                    player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("ui.menu.flags.bridge_required")));
                    // Push the checkbox back to off; otherwise the client keeps it on.
                    reopen(player, ref, store, Tab.FLAGS);
                }
                // Normal toggles: the checkbox already shows the new state, no reopen needed.
            }
            case "OpenWeather" -> player.getPageManager().openCustomPage(ref, store,
                    (CustomUIPage) PlotWeatherPickerPage.create(this.playerRef, resolveWorld(player), this.plugin));
            case "SaveMessages" -> {
                plot.setFlagValue(FlagRegistry.GREET_MESSAGE, safe(data.greetMessage));
                plot.setFlagValue(FlagRegistry.FAREWELL_MESSAGE, safe(data.farewellMessage));
                plot.setFlagValue(FlagRegistry.DENY_MESSAGE, safe(data.denyMessage));
                this.plugin.getPlotManager().savePlots();
                String msg = Tr.t("ui.menu.msg.messages_saved");
                player.getPlayerRef().sendMessage(ChatUtil.success(msg));
                PlotResultPopupPage.show(player, ref, store, this.playerRef, resolveWorld(player), this.plugin,
                        true, msg, PlotResultPopupPage.Return.PLOT_MENU, "Flags");
            }
            case "Merge" -> {
                Outcome o = handleMerge(player, ref, store, plot, data.param);
                PlotResultPopupPage.show(player, ref, store, this.playerRef, resolveWorld(player), this.plugin,
                        o.ok(), o.message(), PlotResultPopupPage.Return.PLOT_MENU, "Merge");
            }
            case "Unmerge" -> {
                Outcome o = handleUnmerge(player, ref, store, plot);
                PlotResultPopupPage.show(player, ref, store, this.playerRef, resolveWorld(player), this.plugin,
                        o.ok(), o.message(), PlotResultPopupPage.Return.PLOT_MENU, "Merge");
            }
            case "OpenAddWarp" -> {
                int max = this.plugin.getPlotManager().getMaxWarpsPerPlot();
                if (plot.getWarpCount() >= max) {
                    player.getPlayerRef()
                            .sendMessage(ChatUtil.error(Tr.t("ui.menu.warps.limit_reached", "max", String.valueOf(max))));
                    reopen(player, ref, store, Tab.WARPS);
                } else {
                    player.getPageManager().openCustomPage(ref, store,
                            (CustomUIPage) PlotWarpNamePopupPage.create(this.playerRef, resolveWorld(player), this.plugin));
                }
            }
            case "WarpTp" -> handleWarpTp(player, ref, store, data.param);
            case "WarpDel" -> {
                handleWarpDel(player, ref, store, plot, data.param);
                reopen(player, ref, store, Tab.WARPS);
            }
            case "OpenTransfer" -> player.getPageManager().openCustomPage(ref, store,
                    (CustomUIPage) PlotPlayerPickerPage.create(this.playerRef, resolveWorld(player), this.plugin,
                            PlotPlayerPickerPage.Mode.TRANSFER));
            case "OpenDelete" -> player.getPageManager().openCustomPage(ref, store,
                    (CustomUIPage) PlotConfirmPopupPage.forDelete(this.playerRef, resolveWorld(player), this.plugin,
                            plot.getGridX(), plot.getGridZ(), safe(plot.getName())));
            default -> reopen(player, ref, store, this.currentTab);
        }
    }

    private void handleRemoveTrust(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Plot plot,
            String param) {
        int index;
        try {
            index = Integer.parseInt(safe(param).trim());
        } catch (NumberFormatException ex) {
            return;
        }
        if (index < 0 || index >= this.trustedView.size()) {
            return;
        }
        UUID target = this.trustedView.get(index);
        plot.removeTrustedPlayer(target);
        this.plugin.getPlotManager().savePlots();
        player.getPlayerRef().sendMessage(ChatUtil.success(Tr.t("trust.removed", "player", resolveName(target))));
    }

    private void handleWarpTp(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String param) {
        int index;
        try {
            index = Integer.parseInt(safe(param).trim());
        } catch (NumberFormatException ex) {
            return;
        }
        if (index < 0 || index >= this.warpView.size()) {
            return;
        }
        this.plugin.getPlotManager().teleportPlayerToWarp(store, ref, this.sourceWorld.getName(),
                this.warpView.get(index));
        player.getPageManager().setPage(ref, store, Page.None);
    }

    private void handleWarpDel(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Plot plot, String param) {
        int index;
        try {
            index = Integer.parseInt(safe(param).trim());
        } catch (NumberFormatException ex) {
            return;
        }
        if (index < 0 || index >= this.warpView.size()) {
            return;
        }
        Plot.PlotWarp w = this.warpView.get(index);
        if (plot.removeWarp(w.name)) {
            this.plugin.getPlotManager().savePlots();
            player.getPlayerRef().sendMessage(ChatUtil.success(Tr.t("ui.menu.warps.removed", "name", safe(w.name))));
        }
    }

    /**
     * Applies a single boolean flag toggle and persists. Flags that can only be
     * enforced through the mixin bridge ({@link MixinRequiredFlags}) are forced
     * off when no bridge is present (basic ECS protection still applies). Returns
     * true if the flag was forced off for that reason.
     */
    private boolean handleToggleFlag(Plot plot, PageData data) {
        PlotFlag<?> flag = FlagRegistry.getFlag(safe(data.param).trim());
        if (!(flag instanceof BooleanFlag booleanFlag)) {
            return false;
        }
        int skipped = applyGuarded(plot, booleanFlag, Boolean.TRUE.equals(data.flagValue));
        this.plugin.getPlotManager().savePlots();
        return skipped > 0;
    }

    /** Applies a boolean flag, refusing to enable bridge-only flags when no bridge is ready. */
    private int applyGuarded(Plot plot, BooleanFlag flag, Boolean value) {
        boolean enabled = value != null && value;
        if (enabled && MixinRequiredFlags.requiresMixinBridge(flag)
                && !MixinBridgeStatus.isReadyForMixinFlags()) {
            plot.setFlagValue(flag, false);
            return 1;
        }
        plot.setFlagValue(flag, enabled);
        return 0;
    }

    private Outcome handleMerge(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Plot plot,
            String dirRaw) {
        String dir = safe(dirRaw).trim().toLowerCase();
        int dx = 0;
        int dz = 0;
        switch (dir) {
            case "north" -> dz = -1;
            case "south" -> dz = 1;
            case "east" -> dx = 1;
            case "west" -> dx = -1;
            default -> {
                return fail(player, Tr.t("ui.menu.msg.merge_invalid_dir"));
            }
        }
        if (!(this.plugin.getPlotManager() instanceof PlotManager pm)) {
            return fail(player, Tr.t("ui.menu.msg.merge_unavailable"));
        }
        Plot neighbor = pm.getPlotByGrid(plot.getWorld(), plot.getGridX() + dx, plot.getGridZ() + dz);
        if (neighbor == null) {
            return fail(player, Tr.t("merge.no_adjacent_claimed"));
        }
        if (!neighbor.getOwner().equals(plot.getOwner())) {
            return fail(player, Tr.t("merge.owner_mismatch"));
        }
        if (pm.arePlotsMerged(plot, neighbor)) {
            String m = Tr.t("merge.already_merged");
            player.getPlayerRef().sendMessage(ChatUtil.info(m));
            return new Outcome(false, m);
        }
        World world = resolveWorld(player);
        if (world == null || !pm.mergePlotsWithRoadPolicy(world, plot, neighbor, true)) {
            return fail(player, Tr.t("ui.menu.msg.merge_failed"));
        }
        this.plugin.getRadarManager().updatePlotMarker(plot);
        this.plugin.getRadarManager().updatePlotMarker(neighbor);
        this.plugin.getRadarManager().refreshAllPlotMarkers();
        this.plugin.getHologramManager().refreshMergedHolograms(java.util.List.of(plot, neighbor), store);
        this.plugin.getPlotManager().savePlots();
        String m = Tr.t("ui.menu.msg.merged", "dir", dir);
        player.getPlayerRef().sendMessage(ChatUtil.success(m));
        return new Outcome(true, m);
    }

    private Outcome handleUnmerge(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Plot plot) {
        if (!(this.plugin.getPlotManager() instanceof PlotManager pm)) {
            return fail(player, Tr.t("ui.menu.msg.unmerge_unavailable"));
        }
        java.util.List<Plot> neighbors = pm.getMergedNeighbors(plot);
        if (neighbors.isEmpty()) {
            String m = Tr.t("ui.menu.msg.not_merged");
            player.getPlayerRef().sendMessage(ChatUtil.info(m));
            return new Outcome(false, m);
        }
        World world = resolveWorld(player);
        if (world == null) {
            return fail(player, Tr.t("ui.menu.msg.unmerge_failed"));
        }
        java.util.List<Plot> affected = new java.util.ArrayList<>();
        int count = 0;
        for (Plot neighbor : neighbors) {
            if (pm.unmergePlotsWithRoadPolicy(world, plot, neighbor, true)) {
                count++;
                affected.add(plot);
                affected.add(neighbor);
                this.plugin.getRadarManager().updatePlotMarker(neighbor);
            }
        }
        this.plugin.getRadarManager().updatePlotMarker(plot);
        this.plugin.getRadarManager().refreshAllPlotMarkers();
        if (!affected.isEmpty()) {
            this.plugin.getHologramManager().refreshMergedHolograms(affected, store);
        }
        this.plugin.getPlotManager().savePlots();
        String m = Tr.t("ui.menu.msg.unmerged", "count", String.valueOf(count));
        player.getPlayerRef().sendMessage(ChatUtil.success(m));
        return new Outcome(true, m);
    }

    /** Sends an error chat line and wraps the same message as a failed {@link Outcome}. */
    private Outcome fail(Player player, String message) {
        player.getPlayerRef().sendMessage(ChatUtil.error(message));
        return new Outcome(false, message);
    }

    /** Result of a mutating menu action: success flag plus the message to surface. */
    private record Outcome(boolean ok, String message) {
    }

    private Outcome handleSetSpawn(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Plot plot) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return fail(player, Tr.t("teleport.position_unavailable"));
        }
        org.joml.Vector3d pos = transform.getPosition();
        com.hypixel.hytale.math.vector.Rotation3f rot = transform.getRotation();
        // Keep the spawn inside this plot's bounds so it can't be set elsewhere.
        if (!plot.equals(this.plugin.getPlotManager().getPlotAt(this.sourceWorld.getName(),
                (int) Math.floor(pos.x), (int) Math.floor(pos.z)))) {
            return fail(player, Tr.t("ui.menu.msg.spawn_outside"));
        }
        float yaw = rot != null ? rot.yaw() : 0f;
        float pitch = rot != null ? rot.pitch() : 0f;
        plot.setSpawn(pos.x, pos.y, pos.z, yaw, pitch);
        this.plugin.getPlotManager().savePlots();
        String m = Tr.t("ui.menu.msg.spawn_set");
        player.getPlayerRef().sendMessage(ChatUtil.success(m));
        return new Outcome(true, m);
    }

    /** The tab id string for the currently selected tab (used to return after a popup). */
    private String currentTabId() {
        return switch (this.currentTab) {
            case TRUST -> "Trust";
            case FLAGS -> "Flags";
            case WARPS -> "Warps";
            case MERGE -> "Merge";
            case DANGER -> "Danger";
        };
    }

    // ======================================================================== helpers

    private void reopen(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Tab tab) {
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotMenuPage(this.playerRef, resolveWorld(player), this.plugin, tab));
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

    private World resolveWorld(Player player) {
        try {
            if (player != null && player.getWorld() != null) {
                return player.getWorld();
            }
        } catch (Exception ignored) {
        }
        return this.sourceWorld;
    }

    private static String resolveName(UUID uuid) {
        if (uuid == null) {
            return "Unknown";
        }
        try {
            PlayerRef ref = Universe.get().getPlayer(uuid);
            if (ref != null && ref.getUsername() != null && !ref.getUsername().isBlank()) {
                return ref.getUsername();
            }
        } catch (Exception ignored) {
        }
        return uuid.toString();
    }


    private static String formatDate(long ms) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").format(new Date(ms));
        } catch (Exception ex) {
            return "-";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // ============================================================================ data

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Param", Codec.STRING), (o, v) -> o.param = v, o -> o.param).add()
                .append(new KeyedCodec<>("@FlagValue", Codec.BOOLEAN), (o, v) -> o.flagValue = v, o -> o.flagValue).add()
                .append(new KeyedCodec<>("@BlockBreak", Codec.BOOLEAN), (o, v) -> o.blockBreak = v, o -> o.blockBreak).add()
                .append(new KeyedCodec<>("@BlockPlace", Codec.BOOLEAN), (o, v) -> o.blockPlace = v, o -> o.blockPlace).add()
                .append(new KeyedCodec<>("@Interact", Codec.BOOLEAN), (o, v) -> o.interact = v, o -> o.interact).add()
                .append(new KeyedCodec<>("@Entry", Codec.BOOLEAN), (o, v) -> o.entry = v, o -> o.entry).add()
                .append(new KeyedCodec<>("@Exit", Codec.BOOLEAN), (o, v) -> o.exit = v, o -> o.exit).add()
                .append(new KeyedCodec<>("@Pvp", Codec.BOOLEAN), (o, v) -> o.pvp = v, o -> o.pvp).add()
                .append(new KeyedCodec<>("@Explosions", Codec.BOOLEAN), (o, v) -> o.explosions = v, o -> o.explosions).add()
                .append(new KeyedCodec<>("@MobSpawning", Codec.BOOLEAN), (o, v) -> o.mobSpawning = v, o -> o.mobSpawning)
                .add()
                .append(new KeyedCodec<>("@MobDamage", Codec.BOOLEAN), (o, v) -> o.mobDamage = v, o -> o.mobDamage).add()
                .append(new KeyedCodec<>("@ItemPickup", Codec.BOOLEAN), (o, v) -> o.itemPickup = v, o -> o.itemPickup).add()
                .append(new KeyedCodec<>("@ItemDrop", Codec.BOOLEAN), (o, v) -> o.itemDrop = v, o -> o.itemDrop).add()
                .append(new KeyedCodec<>("@KeepInventory", Codec.BOOLEAN), (o, v) -> o.keepInventory = v,
                        o -> o.keepInventory)
                .add()
                .append(new KeyedCodec<>("@Crafting", Codec.BOOLEAN), (o, v) -> o.crafting = v, o -> o.crafting).add()
                .append(new KeyedCodec<>("@PlayerChat", Codec.BOOLEAN), (o, v) -> o.playerChat = v, o -> o.playerChat).add()
                .append(new KeyedCodec<>("@Visit", Codec.BOOLEAN), (o, v) -> o.visit = v, o -> o.visit).add()
                .append(new KeyedCodec<>("@GreetMessage", Codec.STRING), (o, v) -> o.greetMessage = v,
                        o -> o.greetMessage)
                .add()
                .append(new KeyedCodec<>("@FarewellMessage", Codec.STRING), (o, v) -> o.farewellMessage = v,
                        o -> o.farewellMessage)
                .add()
                .append(new KeyedCodec<>("@DenyMessage", Codec.STRING), (o, v) -> o.denyMessage = v, o -> o.denyMessage)
                .add()
                .build();

        public String action;
        public String param;
        public Boolean flagValue;
        public Boolean blockBreak;
        public Boolean blockPlace;
        public Boolean interact;
        public Boolean entry;
        public Boolean exit;
        public Boolean pvp;
        public Boolean explosions;
        public Boolean mobSpawning;
        public Boolean mobDamage;
        public Boolean itemPickup;
        public Boolean itemDrop;
        public Boolean keepInventory;
        public Boolean crafting;
        public Boolean playerChat;
        public Boolean visit;
        public String greetMessage;
        public String farewellMessage;
        public String denyMessage;
    }
}
