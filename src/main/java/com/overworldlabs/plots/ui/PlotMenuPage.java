package com.overworldlabs.plots.ui;

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
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.flag.FlagRegistry;
import com.overworldlabs.plots.flag.MixinRequiredFlags;
import com.overworldlabs.plots.flag.types.BooleanFlag;
import com.overworldlabs.plots.integration.mixin.MixinBridgeStatus;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;

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
        INFO, TRUST, FLAGS, WARPS, MERGE, DANGER
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
                (CustomUIPage) new PlotMenuPage(playerRef, sourceWorld, plugin, Tab.INFO));
    }

    // ===================================================================== build

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotMenu.ui");

        // Static / navigation bindings.
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Action", "Close"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabInfo",
                EventData.of("Action", "TabInfo"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabTrust",
                EventData.of("Action", "TabTrust"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabFlags",
                EventData.of("Action", "TabFlags"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabMerge",
                EventData.of("Action", "TabMerge"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabDanger",
                EventData.of("Action", "TabDanger"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabWarps",
                EventData.of("Action", "TabWarps"), false);

        Plot plot = resolvePlot(ref, store);
        boolean canManage = plot != null && canManage(plot);

        String selectedTab = switch (this.currentTab) {
            case INFO -> "Info";
            case TRUST -> "Trust";
            case FLAGS -> "Flags";
            case WARPS -> "Warps";
            case MERGE -> "Merge";
            case DANGER -> "Danger";
        };
        commandBuilder.set("#TabNav.SelectedTab", selectedTab);
        commandBuilder.set("#RightTitle.Text", switch (this.currentTab) {
            case INFO -> "PLOT INFO";
            case TRUST -> "TRUSTED PLAYERS";
            case FLAGS -> "PLOT FLAGS";
            case WARPS -> "PLOT WARPS";
            case MERGE -> "MERGE PLOTS";
            case DANGER -> "DANGER ZONE";
        });

        commandBuilder.set("#PanelInfo.Visible", this.currentTab == Tab.INFO);
        commandBuilder.set("#PanelTrust.Visible", this.currentTab == Tab.TRUST);
        commandBuilder.set("#PanelFlags.Visible", this.currentTab == Tab.FLAGS);
        commandBuilder.set("#PanelWarps.Visible", this.currentTab == Tab.WARPS);
        commandBuilder.set("#PanelMerge.Visible", this.currentTab == Tab.MERGE);
        commandBuilder.set("#PanelDanger.Visible", this.currentTab == Tab.DANGER);

        boolean notice = plot == null || !canManage;
        commandBuilder.set("#PermNotice.Visible", notice);
        if (plot == null) {
            commandBuilder.set("#PermNotice.Text", "You are not standing on a plot.");
        } else if (!canManage) {
            commandBuilder.set("#PermNotice.Text", "Read-only: you are not the owner of this plot.");
        } else {
            commandBuilder.set("#PermNotice.Text", "");
        }

        buildInfo(commandBuilder, eventBuilder, plot, canManage);
        buildTrust(commandBuilder, eventBuilder, plot, canManage);
        buildFlags(commandBuilder, eventBuilder, plot, canManage);
        buildWarps(commandBuilder, eventBuilder, plot, canManage);
        buildMerge(commandBuilder, eventBuilder, plot, canManage);
        buildDanger(commandBuilder, eventBuilder, plot, canManage);
    }

    private void buildWarps(UICommandBuilder cb, UIEventBuilder eb, Plot plot, boolean canManage) {
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#WarpBrowse",
                EventData.of("Action", "OpenWarpsBrowser"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#WarpAdd",
                EventData.of("Action", "OpenAddWarp"), false);

        int max = this.plugin.getPlotManager().getMaxWarpsPerPlot();
        java.util.List<Plot.PlotWarp> warps = plot != null ? plot.getWarps() : new ArrayList<>();
        this.warpView = warps;
        cb.set("#WarpCount.Text", "Warps: " + warps.size() + " / " + max);

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

    private void buildInfo(UICommandBuilder cb, UIEventBuilder eb, Plot plot, boolean canManage) {
        if (plot == null) {
            cb.set("#InfoName.Text", "-");
            cb.set("#InfoOwner.Text", "-");
            cb.set("#InfoGrid.Text", "-");
            cb.set("#InfoCreated.Text", "-");
            cb.set("#InfoTrusted.Text", "0");
            cb.set("#InfoMerged.Text", "0");
            return;
        }
        cb.set("#InfoName.Text", safe(plot.getName()));
        cb.set("#InfoOwner.Text", safe(plot.getOwnerName()));
        cb.set("#InfoGrid.Text", "(" + plot.getGridX() + ", " + plot.getGridZ() + ")");
        cb.set("#InfoCreated.Text", formatDate(plot.getCreatedAt()));
        cb.set("#InfoTrusted.Text", String.valueOf(plot.getTrustedPlayers().size()));
        cb.set("#InfoMerged.Text", String.valueOf(plot.getMergedPlots().size()));

        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnRename",
                EventData.of("Action", "OpenRename"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSetSpawn",
                EventData.of("Action", "SetSpawn"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnTeleport",
                EventData.of("Action", "Teleport"), false);
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
        // Boolean toggles.
        setBool(cb, "#FlagBlockBreak.Value", plot, FlagRegistry.BLOCK_BREAK);
        setBool(cb, "#FlagBlockPlace.Value", plot, FlagRegistry.BLOCK_PLACE);
        setBool(cb, "#FlagInteract.Value", plot, FlagRegistry.INTERACT);
        setBool(cb, "#FlagEntry.Value", plot, FlagRegistry.ENTRY);
        setBool(cb, "#FlagExit.Value", plot, FlagRegistry.EXIT);
        setBool(cb, "#FlagPvp.Value", plot, FlagRegistry.PVP);
        setBool(cb, "#FlagExplosions.Value", plot, FlagRegistry.EXPLOSIONS);
        setBool(cb, "#FlagMobSpawning.Value", plot, FlagRegistry.MOB_SPAWNING);
        setBool(cb, "#FlagMobDamage.Value", plot, FlagRegistry.MOB_DAMAGE);
        setBool(cb, "#FlagItemPickup.Value", plot, FlagRegistry.ITEM_PICKUP);
        setBool(cb, "#FlagItemDrop.Value", plot, FlagRegistry.ITEM_DROP);
        setBool(cb, "#FlagKeepInventory.Value", plot, FlagRegistry.KEEP_INVENTORY);
        setBool(cb, "#FlagCrafting.Value", plot, FlagRegistry.CRAFTING);
        setBool(cb, "#FlagPlayerChat.Value", plot, FlagRegistry.PLAYER_CHAT);
        setBool(cb, "#FlagVisit.Value", plot, FlagRegistry.VISIT);

        // Bridge-only flags are hidden entirely when TaleGuard is not available,
        // so the menu never shows a toggle the server can't actually enforce.
        boolean bridge = MixinBridgeStatus.isReadyForMixinFlags();
        cb.set("#FlagExplosionsRow.Visible", bridge);
        cb.set("#FlagMobSpawningRow.Visible", bridge);
        cb.set("#FlagItemPickupRow.Visible", bridge);
        cb.set("#FlagKeepInventoryRow.Visible", bridge);

        // String inputs.
        cb.set("#FlagGreetMessage.Value", safe(plot.getFlagValue(FlagRegistry.GREET_MESSAGE)));
        cb.set("#FlagFarewellMessage.Value", safe(plot.getFlagValue(FlagRegistry.FAREWELL_MESSAGE)));
        cb.set("#FlagDenyMessage.Value", safe(plot.getFlagValue(FlagRegistry.DENY_MESSAGE)));

        // Capture current values back on save.
        EventData save = EventData.of("Action", "SaveFlags")
                .append("@BlockBreak", "#FlagBlockBreak.Value")
                .append("@BlockPlace", "#FlagBlockPlace.Value")
                .append("@Interact", "#FlagInteract.Value")
                .append("@Entry", "#FlagEntry.Value")
                .append("@Exit", "#FlagExit.Value")
                .append("@Pvp", "#FlagPvp.Value")
                .append("@Explosions", "#FlagExplosions.Value")
                .append("@MobSpawning", "#FlagMobSpawning.Value")
                .append("@MobDamage", "#FlagMobDamage.Value")
                .append("@ItemPickup", "#FlagItemPickup.Value")
                .append("@ItemDrop", "#FlagItemDrop.Value")
                .append("@KeepInventory", "#FlagKeepInventory.Value")
                .append("@Crafting", "#FlagCrafting.Value")
                .append("@PlayerChat", "#FlagPlayerChat.Value")
                .append("@Visit", "#FlagVisit.Value")
                .append("@GreetMessage", "#FlagGreetMessage.Value")
                .append("@FarewellMessage", "#FlagFarewellMessage.Value")
                .append("@DenyMessage", "#FlagDenyMessage.Value");
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSaveFlags", save, false);
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
            case "TabInfo" -> {
                reopen(player, ref, store, Tab.INFO);
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

        // Mutating actions require management permission.
        if (!canManage) {
            player.getPlayerRef().sendMessage(ChatUtil.error("You do not have permission to manage this plot."));
            reopen(player, ref, store, this.currentTab);
            return;
        }

        switch (action) {
            case "OpenRename" -> player.getPageManager().openCustomPage(ref, store,
                    (CustomUIPage) PlotRenamePopupPage.create(this.playerRef, resolveWorld(player), this.plugin));
            case "SetSpawn" -> {
                handleSetSpawn(player, ref, store, plot);
                reopen(player, ref, store, this.currentTab);
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
            case "SaveFlags" -> {
                int skipped = handleSaveFlags(plot, data);
                if (skipped > 0) {
                    player.getPlayerRef().sendMessage(ChatUtil.error(
                            "Some flags need the TaleGuard bridge and were left off. Install TaleGuard to enable them; basic protection still applies."));
                } else {
                    player.getPlayerRef().sendMessage(ChatUtil.success("Plot flags saved."));
                }
                reopen(player, ref, store, Tab.FLAGS);
            }
            case "Merge" -> {
                handleMerge(player, ref, store, plot, data.param);
                reopen(player, ref, store, Tab.MERGE);
            }
            case "Unmerge" -> {
                handleUnmerge(player, ref, store, plot);
                reopen(player, ref, store, Tab.MERGE);
            }
            case "OpenAddWarp" -> {
                int max = this.plugin.getPlotManager().getMaxWarpsPerPlot();
                if (plot.getWarpCount() >= max) {
                    player.getPlayerRef().sendMessage(ChatUtil.error("Warp limit reached (max " + max + ")."));
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
        player.getPlayerRef().sendMessage(ChatUtil.success("Removed " + resolveName(target) + " from trusted players."));
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
        this.plugin.getPlotManager().teleportPlayerToWarp(store, ref, this.warpView.get(index));
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
            player.getPlayerRef().sendMessage(ChatUtil.success("Warp '" + safe(w.name) + "' removed."));
        }
    }

    /**
     * Persists flag toggles. Flags that can only be enforced through the mixin
     * bridge ({@link MixinRequiredFlags}) are forced off when no bridge is
     * present, so the menu never enables a protection that would silently no-op
     * (basic ECS protection still applies regardless). Returns the number of
     * flags left off for that reason.
     */
    private int handleSaveFlags(Plot plot, PageData data) {
        int skipped = 0;
        skipped += applyGuarded(plot, FlagRegistry.BLOCK_BREAK, data.blockBreak);
        skipped += applyGuarded(plot, FlagRegistry.BLOCK_PLACE, data.blockPlace);
        skipped += applyGuarded(plot, FlagRegistry.INTERACT, data.interact);
        skipped += applyGuarded(plot, FlagRegistry.ENTRY, data.entry);
        skipped += applyGuarded(plot, FlagRegistry.EXIT, data.exit);
        skipped += applyGuarded(plot, FlagRegistry.PVP, data.pvp);
        skipped += applyGuarded(plot, FlagRegistry.EXPLOSIONS, data.explosions);
        skipped += applyGuarded(plot, FlagRegistry.MOB_SPAWNING, data.mobSpawning);
        skipped += applyGuarded(plot, FlagRegistry.MOB_DAMAGE, data.mobDamage);
        skipped += applyGuarded(plot, FlagRegistry.ITEM_PICKUP, data.itemPickup);
        skipped += applyGuarded(plot, FlagRegistry.ITEM_DROP, data.itemDrop);
        skipped += applyGuarded(plot, FlagRegistry.KEEP_INVENTORY, data.keepInventory);
        skipped += applyGuarded(plot, FlagRegistry.CRAFTING, data.crafting);
        skipped += applyGuarded(plot, FlagRegistry.PLAYER_CHAT, data.playerChat);
        skipped += applyGuarded(plot, FlagRegistry.VISIT, data.visit);

        plot.setFlagValue(FlagRegistry.GREET_MESSAGE, safe(data.greetMessage));
        plot.setFlagValue(FlagRegistry.FAREWELL_MESSAGE, safe(data.farewellMessage));
        plot.setFlagValue(FlagRegistry.DENY_MESSAGE, safe(data.denyMessage));

        this.plugin.getPlotManager().savePlots();
        return skipped;
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

    private void handleMerge(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Plot plot, String dirRaw) {
        String dir = safe(dirRaw).trim().toLowerCase();
        int dx = 0;
        int dz = 0;
        switch (dir) {
            case "north" -> dz = -1;
            case "south" -> dz = 1;
            case "east" -> dx = 1;
            case "west" -> dx = -1;
            default -> {
                player.getPlayerRef().sendMessage(ChatUtil.error("Invalid merge direction."));
                return;
            }
        }
        if (!(this.plugin.getPlotManager() instanceof PlotManager pm)) {
            player.getPlayerRef().sendMessage(ChatUtil.error("Merge is unavailable."));
            return;
        }
        Plot neighbor = pm.getPlotByGrid(plot.getGridX() + dx, plot.getGridZ() + dz);
        if (neighbor == null) {
            player.getPlayerRef().sendMessage(ChatUtil.error("No adjacent claimed plot to the " + dir + "."));
            return;
        }
        if (!neighbor.getOwner().equals(plot.getOwner())) {
            player.getPlayerRef().sendMessage(ChatUtil.error("That plot is owned by someone else."));
            return;
        }
        if (pm.arePlotsMerged(plot, neighbor)) {
            player.getPlayerRef().sendMessage(ChatUtil.info("Those plots are already merged."));
            return;
        }
        World world = resolveWorld(player);
        if (world == null || !pm.mergePlotsWithRoadPolicy(world, plot, neighbor, true)) {
            player.getPlayerRef().sendMessage(ChatUtil.error("Merge failed."));
            return;
        }
        this.plugin.getRadarManager().updatePlotMarker(plot);
        this.plugin.getRadarManager().updatePlotMarker(neighbor);
        this.plugin.getRadarManager().refreshAllPlotMarkers();
        this.plugin.getHologramManager().refreshMergedHolograms(java.util.List.of(plot, neighbor), store);
        this.plugin.getPlotManager().savePlots();
        player.getPlayerRef().sendMessage(ChatUtil.success("Merged plot to the " + dir + "."));
    }

    private void handleUnmerge(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Plot plot) {
        if (!(this.plugin.getPlotManager() instanceof PlotManager pm)) {
            player.getPlayerRef().sendMessage(ChatUtil.error("Unmerge is unavailable."));
            return;
        }
        java.util.List<Plot> neighbors = pm.getMergedNeighbors(plot);
        if (neighbors.isEmpty()) {
            player.getPlayerRef().sendMessage(ChatUtil.info("This plot is not merged with anything."));
            return;
        }
        World world = resolveWorld(player);
        if (world == null) {
            player.getPlayerRef().sendMessage(ChatUtil.error("Unmerge failed."));
            return;
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
        player.getPlayerRef().sendMessage(ChatUtil.success("Unmerged " + count + " link(s)."));
    }

    private void handleSetSpawn(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Plot plot) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            player.getPlayerRef().sendMessage(ChatUtil.error("Your position is unavailable."));
            return;
        }
        org.joml.Vector3d pos = transform.getPosition();
        com.hypixel.hytale.math.vector.Rotation3f rot = transform.getRotation();
        // Keep the spawn inside this plot's bounds so it can't be set elsewhere.
        if (!plot.equals(this.plugin.getPlotManager().getPlotAt(this.sourceWorld.getName(),
                (int) Math.floor(pos.x), (int) Math.floor(pos.z)))) {
            player.getPlayerRef().sendMessage(ChatUtil.error("Stand inside this plot to set its spawn."));
            return;
        }
        float yaw = rot != null ? rot.yaw() : 0f;
        float pitch = rot != null ? rot.pitch() : 0f;
        plot.setSpawn(pos.x, pos.y, pos.z, yaw, pitch);
        this.plugin.getPlotManager().savePlots();
        player.getPlayerRef().sendMessage(ChatUtil.success("Plot spawn set to your current position."));
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

    private static void setBool(UICommandBuilder cb, String id, Plot plot, BooleanFlag flag) {
        boolean value = Boolean.TRUE.equals(plot.getFlagValue(flag));
        cb.set(id, value);
    }

    private static void applyBool(Plot plot, BooleanFlag flag, Boolean value) {
        plot.setFlagValue(flag, value != null && value);
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
