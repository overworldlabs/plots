package dev.stoshe.plots.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;
import dev.stoshe.plots.util.Tr;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plot administration dashboard ({@code /plot admin}).
 *
 * <p>Tabbed dashboard with three sections:
 * <ul>
 *   <li>SETTINGS — global {@link PlotConfig} values; editable where setters exist, read-only otherwise.</li>
 *   <li>PLOTS — paginated list of every claimed plot with teleport / delete / transfer actions.</li>
 *   <li>STATS — aggregate totals and a top-owner breakdown.</li>
 * </ul>
 */
public class PlotAdminPage extends InteractiveCustomUIPage<PlotAdminPage.PageData> {

    private static final int MAX_PLOT_ROWS = 10;
    private static final int TOP_OWNERS = 8;

    private enum Tab {
        SETTINGS, PLOTS, STATS
    }

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private Tab currentTab;
    private int page;

    /** Grid of the plot currently being transferred (null when not transferring). */
    private final int[] transferGrid;

    /** Cached, page-ordered view of plots rendered in the PLOTS tab. */
    private List<Plot> plotView = new ArrayList<>();

    private PlotAdminPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            @Nonnull Tab tab, int page, int[] transferGrid) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.currentTab = tab;
        this.page = Math.max(0, page);
        this.transferGrid = transferGrid;
    }

    public static void open(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef,
            World sourceWorld, Plots plugin) {
        if (player == null || ref == null || store == null || playerRef == null || sourceWorld == null
                || plugin == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotAdminPage(playerRef, sourceWorld, plugin, Tab.SETTINGS, 0, null));
    }

    // ===================================================================== build

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotAdmin.ui");

        // Aero-style sidebar nav; both variants of each entry trigger the same tab
        // switch. The top-right close (#CloseButton) is dismissed natively by the
        // client, so we don't bind it (binding it would break native close).
        bindNav(eventBuilder, "#NavSettings", "#NavSettingsActive", "TabSettings");
        bindNav(eventBuilder, "#NavPlots", "#NavPlotsActive", "TabPlots");
        bindNav(eventBuilder, "#NavStats", "#NavStatsActive", "TabStats");

        boolean admin = isAdmin();
        commandBuilder.set("#PermNotice.Visible", !admin);
        commandBuilder.set("#PermNotice.Text", admin ? "" : Tr.t("ui.admin.no_permission"));

        // Sidebar labels + active/inactive highlight for the current section.
        commandBuilder.set("#NavSettings.Text", Tr.t("ui.admin.nav.settings"));
        commandBuilder.set("#NavSettingsActive.Text", Tr.t("ui.admin.nav.settings"));
        commandBuilder.set("#NavPlots.Text", Tr.t("ui.admin.nav.plots"));
        commandBuilder.set("#NavPlotsActive.Text", Tr.t("ui.admin.nav.plots"));
        commandBuilder.set("#NavStats.Text", Tr.t("ui.admin.nav.stats"));
        commandBuilder.set("#NavStatsActive.Text", Tr.t("ui.admin.nav.stats"));
        commandBuilder.set("#NavWorlds.Text", Tr.t("ui.admin.nav.worlds"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#NavWorlds",
                EventData.of("Action", "OpenWorlds"), false);

        commandBuilder.set("#NavSettingsActive.Visible", this.currentTab == Tab.SETTINGS);
        commandBuilder.set("#NavSettings.Visible", this.currentTab != Tab.SETTINGS);
        commandBuilder.set("#NavPlotsActive.Visible", this.currentTab == Tab.PLOTS);
        commandBuilder.set("#NavPlots.Visible", this.currentTab != Tab.PLOTS);
        commandBuilder.set("#NavStatsActive.Visible", this.currentTab == Tab.STATS);
        commandBuilder.set("#NavStats.Visible", this.currentTab != Tab.STATS);

        // The container header stays static; the section name sits above the panel body.
        commandBuilder.set("#AdminPanelTitle.Text", switch (this.currentTab) {
            case SETTINGS -> Tr.t("ui.admin.title.settings");
            case PLOTS -> Tr.t("ui.admin.title.plots");
            case STATS -> Tr.t("ui.admin.title.stats");
        });

        commandBuilder.set("#PanelSettings.Visible", this.currentTab == Tab.SETTINGS);
        commandBuilder.set("#PanelPlots.Visible", this.currentTab == Tab.PLOTS);
        commandBuilder.set("#PanelStats.Visible", this.currentTab == Tab.STATS);

        buildSettings(commandBuilder, eventBuilder, admin);
        buildPlots(commandBuilder, eventBuilder, admin);
        buildStats(commandBuilder);
    }

    /** Binds both the inactive and active variant of a sidebar nav entry to one action. */
    private void bindNav(UIEventBuilder eb, String inactiveId, String activeId, String action) {
        eb.addEventBinding(CustomUIEventBindingType.Activating, inactiveId, EventData.of("Action", action), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, activeId, EventData.of("Action", action), false);
    }

    // -------------------------------------------------------------------- SETTINGS

    private void buildSettings(UICommandBuilder cb, UIEventBuilder eb, boolean admin) {
        PlotConfig config = resolveConfig();

        // Editable (setters exist on PlotConfig).
        cb.set("#SetPlotSizeX.Value", config != null ? String.valueOf(config.getPlotSizeX()) : "");
        cb.set("#SetPlotSizeZ.Value", config != null ? String.valueOf(config.getPlotSizeZ()) : "");
        cb.set("#SetRoadSizeX.Value", config != null ? String.valueOf(config.getRoadSizeX()) : "");
        cb.set("#SetRoadSizeZ.Value", config != null ? String.valueOf(config.getRoadSizeZ()) : "");

        // Editable settings that persist to config.json on save.
        cb.set("#SetDefaultName.Value", config != null ? safe(config.getDefaultPlotNameTemplate()) : "");
        cb.set("#SetMaxPlots.Value", config != null ? String.valueOf(config.getPerWorldDefault()) : "");
        cb.set("#SetMaxLimit.Value", config != null ? String.valueOf(config.getHardCap()) : "");
        cb.set("#SetHologram.Value", config != null && config.isHologramEnabled());
        cb.set("#SetEconomy.Value", config != null && config.isEconomyEnabled());

        // Structural values remain read-only (changing them would break existing data).
        cb.set("#SetWorldName.Text", config != null ? safe(config.getDefaultWorldName()) : "-");
        cb.set("#SetManagedWorld.Text", config != null && config.getDefaultWorldName() != null
                && config.isManagedWorld(config.getDefaultWorldName()) ? "Yes" : "No");

        cb.set("#SettingsNote.Text", Tr.t("ui.admin.settings.note"));

        // Static labels / button captions → translations.
        cb.set("#SettingsEditableLabel.Text", Tr.t("ui.admin.settings.editable"));
        cb.set("#SettingsReadonlyLabel.Text", Tr.t("ui.admin.settings.readonly"));
        cb.set("#SetPlotSizeXLabel.Text", Tr.t("ui.admin.settings.plot_size_x"));
        cb.set("#SetPlotSizeZLabel.Text", Tr.t("ui.admin.settings.plot_size_z"));
        cb.set("#SetRoadSizeXLabel.Text", Tr.t("ui.admin.settings.road_size_x"));
        cb.set("#SetRoadSizeZLabel.Text", Tr.t("ui.admin.settings.road_size_z"));
        cb.set("#SetWorldNameLabel.Text", Tr.t("ui.admin.settings.world_name"));
        cb.set("#SetManagedWorldLabel.Text", Tr.t("ui.admin.settings.managed_world"));
        cb.set("#SetDefaultNameLabel.Text", Tr.t("ui.admin.settings.default_name"));
        cb.set("#SetMaxPlotsLabel.Text", Tr.t("ui.admin.settings.max_plots"));
        cb.set("#SetMaxLimitLabel.Text", Tr.t("ui.admin.settings.max_limit"));
        cb.set("#SetHologramLabel.Text", Tr.t("ui.admin.settings.holograms"));
        cb.set("#SetEconomyLabel.Text", Tr.t("ui.admin.settings.economy"));
        cb.set("#BtnSaveSettings.Text", Tr.t("ui.admin.settings.save"));

        EventData save = EventData.of("Action", "SaveSettings")
                .append("@PlotSizeX", "#SetPlotSizeX.Value")
                .append("@PlotSizeZ", "#SetPlotSizeZ.Value")
                .append("@RoadSizeX", "#SetRoadSizeX.Value")
                .append("@RoadSizeZ", "#SetRoadSizeZ.Value")
                .append("@DefaultName", "#SetDefaultName.Value")
                .append("@MaxPlots", "#SetMaxPlots.Value")
                .append("@MaxLimit", "#SetMaxLimit.Value")
                .append("@Holograms", "#SetHologram.Value")
                .append("@Economy", "#SetEconomy.Value");
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSaveSettings", save, false);
    }

    // ----------------------------------------------------------------------- PLOTS

    private void buildPlots(UICommandBuilder cb, UIEventBuilder eb, boolean admin) {
        List<Plot> all = new ArrayList<>();
        try {
            if (this.plugin.getPlotManager().getAllPlots() != null) {
                all.addAll(this.plugin.getPlotManager().getAllPlots());
            }
        } catch (Exception ignored) {
        }
        all.sort(Comparator
                .comparingInt(Plot::getGridX)
                .thenComparingInt(Plot::getGridZ));

        int total = all.size();
        int maxPage = total == 0 ? 0 : (total - 1) / MAX_PLOT_ROWS;
        if (this.page > maxPage) {
            this.page = maxPage;
        }
        int start = this.page * MAX_PLOT_ROWS;

        this.plotView = new ArrayList<>();
        for (int i = 0; i < MAX_PLOT_ROWS; i++) {
            int srcIndex = start + i;
            if (srcIndex < total) {
                Plot plot = all.get(srcIndex);
                this.plotView.add(plot);
                cb.set("#AdmRow" + i + ".Visible", true);
                cb.set("#AdmRowName" + i + ".Text", safe(plot.getName()));
                cb.set("#AdmRowOwner" + i + ".Text", safe(plot.getOwnerName()));
                cb.set("#AdmRowGrid" + i + ".Text", "(" + plot.getGridX() + ", " + plot.getGridZ() + ")");
                cb.set("#AdmRowTp" + i + ".Text", Tr.t("ui.admin.plots.tp"));
                cb.set("#AdmRowTransfer" + i + ".Text", Tr.t("ui.common.transfer"));
                cb.set("#AdmRowDelete" + i + ".Text", Tr.t("ui.common.delete"));
                eb.addEventBinding(CustomUIEventBindingType.Activating, "#AdmRowTp" + i,
                        EventData.of("Action", "RowTp").append("Param", String.valueOf(i)), false);
                eb.addEventBinding(CustomUIEventBindingType.Activating, "#AdmRowDelete" + i,
                        EventData.of("Action", "RowDelete").append("Param", String.valueOf(i)), false);
                eb.addEventBinding(CustomUIEventBindingType.Activating, "#AdmRowTransfer" + i,
                        EventData.of("Action", "RowTransfer").append("Param", String.valueOf(i)), false);
            } else {
                cb.set("#AdmRow" + i + ".Visible", false);
            }
        }

        cb.set("#AdmEmpty.Visible", total == 0);
        cb.set("#AdmEmpty.Text", Tr.t("ui.admin.plots.empty"));
        cb.set("#AdmPrev.Text", Tr.t("ui.common.prev"));
        cb.set("#AdmNext.Text", Tr.t("ui.common.next"));
        cb.set("#AdmPageLabel.Text", total == 0
                ? Tr.t("ui.admin.plots.none")
                : Tr.t("ui.admin.plots.page", "page", String.valueOf(this.page + 1),
                        "total", String.valueOf(maxPage + 1), "count", String.valueOf(total)));

        eb.addEventBinding(CustomUIEventBindingType.Activating, "#AdmPrev",
                EventData.of("Action", "PrevPage"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#AdmNext",
                EventData.of("Action", "NextPage"), false);

        // Transfer sub-panel (only meaningful while a transfer target is selected).
        boolean transferring = this.transferGrid != null;
        cb.set("#AdmTransferPanel.Visible", transferring);
        if (transferring) {
            Plot target = safePlotByGrid(this.transferGrid[0], this.transferGrid[1]);
            cb.set("#AdmTransferLabel.Text", Tr.t("ui.admin.plots.transfer_to", "plot",
                    (target != null ? "'" + safe(target.getName()) + "' " : "")
                            + "(" + this.transferGrid[0] + ", " + this.transferGrid[1] + ")"));
            cb.set("#AdmTransferSubmit.Text", Tr.t("ui.common.transfer"));
            cb.set("#AdmTransferCancel.Text", Tr.t("ui.common.cancel"));
            cb.set("#AdmTransferField.Value", "");
            EventData confirm = EventData.of("Action", "TransferConfirm")
                    .append("@TransferName", "#AdmTransferField.Value");
            eb.addEventBinding(CustomUIEventBindingType.Activating, "#AdmTransferSubmit", confirm, false);
            eb.addEventBinding(CustomUIEventBindingType.Activating, "#AdmTransferCancel",
                    EventData.of("Action", "TransferCancel"), false);
        }
    }

    // ----------------------------------------------------------------------- STATS

    private void buildStats(UICommandBuilder cb) {
        List<Plot> all = new ArrayList<>();
        try {
            if (this.plugin.getPlotManager().getAllPlots() != null) {
                all.addAll(this.plugin.getPlotManager().getAllPlots());
            }
        } catch (Exception ignored) {
        }

        int total = all.size();
        int claimed = 0;
        Map<String, Integer> byOwner = new HashMap<>();
        for (Plot plot : all) {
            if (plot.getOwner() != null) {
                claimed++;
            }
            String owner = plot.getOwnerName();
            if (owner == null || owner.isBlank()) {
                owner = plot.getOwner() != null ? plot.getOwner().toString() : "Unclaimed";
            }
            byOwner.merge(owner, 1, Integer::sum);
        }

        cb.set("#StatTotal.Text", String.valueOf(total));
        cb.set("#StatClaimed.Text", String.valueOf(claimed));
        cb.set("#StatUnclaimed.Text", String.valueOf(total - claimed));
        cb.set("#StatOwners.Text", String.valueOf(byOwner.size()));

        cb.set("#StatTotalLabel.Text", Tr.t("ui.admin.stats.total"));
        cb.set("#StatClaimedLabel.Text", Tr.t("ui.admin.stats.claimed"));
        cb.set("#StatUnclaimedLabel.Text", Tr.t("ui.admin.stats.unclaimed"));
        cb.set("#StatOwnersLabel.Text", Tr.t("ui.admin.stats.owners"));
        cb.set("#StatTopLabel.Text", Tr.t("ui.admin.stats.top_owners"));
        cb.set("#StatTopEmpty.Text", Tr.t("ui.admin.stats.no_data"));

        List<Map.Entry<String, Integer>> top = new ArrayList<>(byOwner.entrySet());
        top.sort(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue()).reversed());

        cb.set("#StatTopEmpty.Visible", top.isEmpty());
        for (int i = 0; i < TOP_OWNERS; i++) {
            if (i < top.size()) {
                Map.Entry<String, Integer> e = top.get(i);
                cb.set("#StatTopRow" + i + ".Visible", true);
                cb.set("#StatTopName" + i + ".Text", safe(e.getKey()));
                cb.set("#StatTopCount" + i + ".Text", String.valueOf(e.getValue()));
            } else {
                cb.set("#StatTopRow" + i + ".Visible", false);
            }
        }
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

        if ("Close".equals(action)) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if (!isAdmin()) {
            player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("ui.admin.no_permission")));
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        switch (action) {
            case "TabSettings" -> reopen(player, ref, store, Tab.SETTINGS, 0, null);
            case "TabPlots" -> reopen(player, ref, store, Tab.PLOTS, this.page, null);
            case "TabStats" -> reopen(player, ref, store, Tab.STATS, 0, null);
            case "OpenWorlds" ->
                PlotAdminWorldsPage.open(player, ref, store, this.playerRef, this.sourceWorld, this.plugin);
            case "PrevPage" -> reopen(player, ref, store, Tab.PLOTS, this.page - 1, this.transferGrid);
            case "NextPage" -> reopen(player, ref, store, Tab.PLOTS, this.page + 1, this.transferGrid);
            case "SaveSettings" -> handleSaveSettings(player, ref, store, data);
            case "RowTp" -> handleRowTeleport(player, ref, store, data.param);
            case "RowDelete" -> handleRowDelete(player, ref, store, data.param);
            case "RowTransfer" -> {
                int[] grid = gridForRow(data.param);
                reopen(player, ref, store, Tab.PLOTS, this.page, grid);
            }
            case "TransferConfirm" -> {
                handleTransferConfirm(player, ref, store, data.transferName);
            }
            case "TransferCancel" -> reopen(player, ref, store, Tab.PLOTS, this.page, null);
            default -> reopen(player, ref, store, this.currentTab, this.page, this.transferGrid);
        }
    }

    private void handleSaveSettings(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PageData data) {
        World world = resolveWorld(player);
        PlotConfig config = resolveConfig();
        if (config == null) {
            String m = Tr.t("ui.admin.settings.unavailable");
            player.getPlayerRef().sendMessage(ChatUtil.error(m));
            PlotResultPopupPage.show(player, ref, store, this.playerRef, world, this.plugin, false, m,
                    PlotResultPopupPage.Return.ADMIN, null);
            return;
        }
        boolean changed = false;
        Integer px = parsePositive(data.plotSizeX);
        Integer pz = parsePositive(data.plotSizeZ);
        Integer rx = parsePositive(data.roadSizeX);
        Integer rz = parsePositive(data.roadSizeZ);
        if (px != null) {
            config.setPlotSizeX(px);
            changed = true;
        }
        if (pz != null) {
            config.setPlotSizeZ(pz);
            changed = true;
        }
        if (rx != null) {
            config.setRoadSizeX(rx);
            changed = true;
        }
        if (rz != null) {
            config.setRoadSizeZ(rz);
            changed = true;
        }
        String defaultName = safe(data.defaultName).trim();
        if (!defaultName.isEmpty() && !defaultName.equals(config.getDefaultPlotNameTemplate())) {
            config.setDefaultPlotNameTemplate(defaultName);
            changed = true;
        }
        Integer mp = parsePositive(data.maxPlots);
        Integer ml = parsePositive(data.maxLimit);
        if (mp != null && mp != config.getPerWorldDefault()) {
            config.setMaxPlotsDefaultValue(mp);
            changed = true;
        }
        if (ml != null && ml != config.getHardCap()) {
            config.setMaxPlotLimit(ml);
            changed = true;
        }
        if (data.holograms != null && data.holograms != config.isHologramEnabled()) {
            config.setHologramEnabled(data.holograms);
            changed = true;
        }
        if (data.economy != null && data.economy != config.isEconomyEnabled()) {
            config.setEconomyEnabled(data.economy);
            changed = true;
        }
        if (changed) {
            this.plugin.saveConfig(config);
            String m = Tr.t("ui.admin.settings.saved");
            player.getPlayerRef().sendMessage(ChatUtil.success(m));
            PlotResultPopupPage.show(player, ref, store, this.playerRef, world, this.plugin, true, m,
                    PlotResultPopupPage.Return.ADMIN, null);
        } else {
            String m = Tr.t("ui.admin.settings.no_changes");
            player.getPlayerRef().sendMessage(ChatUtil.info(m));
            PlotResultPopupPage.show(player, ref, store, this.playerRef, world, this.plugin, false, m,
                    PlotResultPopupPage.Return.ADMIN, null);
        }
    }

    private void handleRowTeleport(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String param) {
        Plot plot = plotForRow(param);
        if (plot == null) {
            reopen(player, ref, store, Tab.PLOTS, this.page, null);
            return;
        }
        try {
            this.plugin.getPlotManager().teleportPlayerToPlot(store, ref, plot);
            player.getPageManager().setPage(ref, store, Page.None);
        } catch (Exception ex) {
            player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("ui.admin.plots.tp_failed")));
            reopen(player, ref, store, Tab.PLOTS, this.page, null);
        }
    }

    private void handleRowDelete(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String param) {
        Plot plot = plotForRow(param);
        if (plot == null) {
            reopen(player, ref, store, Tab.PLOTS, this.page, null);
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) PlotConfirmPopupPage.forDelete(this.playerRef, resolveWorld(player), this.plugin,
                        plot.getGridX(), plot.getGridZ(), safe(plot.getName())));
    }

    private void handleTransferConfirm(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String name) {
        if (this.transferGrid == null) {
            reopen(player, ref, store, Tab.PLOTS, this.page, null);
            return;
        }
        String typed = safe(name).trim();
        if (typed.isEmpty()) {
            player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("ui.admin.plots.transfer_enter_name")));
            reopen(player, ref, store, Tab.PLOTS, this.page, this.transferGrid);
            return;
        }
        PlayerRef target = null;
        try {
            target = Universe.get().getPlayerByUsername(typed, NameMatching.EXACT);
        } catch (Exception ignored) {
        }
        if (target == null || target.getUuid() == null) {
            player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("ui.admin.plots.player_offline", "player", typed)));
            reopen(player, ref, store, Tab.PLOTS, this.page, this.transferGrid);
            return;
        }
        Plot plot = safePlotByGrid(this.transferGrid[0], this.transferGrid[1]);
        if (plot == null) {
            player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("ui.admin.plots.plot_gone")));
            reopen(player, ref, store, Tab.PLOTS, this.page, null);
            return;
        }
        try {
            plot.setOwner(target.getUuid());
            plot.setOwnerName(safe(target.getUsername()));
            plot.removeTrustedPlayer(target.getUuid());
            this.plugin.getPlotManager().savePlots();
            player.getPlayerRef().sendMessage(ChatUtil.success(
                    Tr.t("ui.admin.plots.transferred", "player", safe(target.getUsername()))));
        } catch (Exception ex) {
            player.getPlayerRef().sendMessage(ChatUtil.error(Tr.t("ui.admin.plots.transfer_failed")));
        }
        reopen(player, ref, store, Tab.PLOTS, this.page, null);
    }

    // ======================================================================== helpers

    private void reopen(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Tab tab, int page,
            int[] transferGrid) {
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotAdminPage(this.playerRef, resolveWorld(player), this.plugin, tab,
                        Math.max(0, page), transferGrid));
    }

    private Plot plotForRow(String param) {
        int index;
        try {
            index = Integer.parseInt(safe(param).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
        if (index < 0 || index >= this.plotView.size()) {
            return null;
        }
        return this.plotView.get(index);
    }

    private int[] gridForRow(String param) {
        Plot plot = plotForRow(param);
        return plot == null ? null : new int[] { plot.getGridX(), plot.getGridZ() };
    }

    private Plot safePlotByGrid(int gx, int gz) {
        try {
            String world = this.plugin.getPlotManager().getConfig().getDefaultWorldName();
            return this.plugin.getPlotManager().getPlotByGrid(world, gx, gz);
        } catch (Exception ex) {
            return null;
        }
    }

    private PlotConfig resolveConfig() {
        try {
            PlotConfig config = this.plugin.getConfig();
            if (config != null) {
                return config;
            }
        } catch (Exception ignored) {
        }
        try {
            return this.plugin.getPlotManager().getConfig();
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isAdmin() {
        try {
            UUID uuid = this.playerRef.getUuid();
            return uuid != null && PermissionUtil.hasAdminPermission(uuid);
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

    private static Integer parsePositive(String value) {
        try {
            int parsed = Integer.parseInt(safe(value).trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
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
                .append(new KeyedCodec<>("@PlotSizeX", Codec.STRING), (o, v) -> o.plotSizeX = v, o -> o.plotSizeX).add()
                .append(new KeyedCodec<>("@PlotSizeZ", Codec.STRING), (o, v) -> o.plotSizeZ = v, o -> o.plotSizeZ).add()
                .append(new KeyedCodec<>("@RoadSizeX", Codec.STRING), (o, v) -> o.roadSizeX = v, o -> o.roadSizeX).add()
                .append(new KeyedCodec<>("@RoadSizeZ", Codec.STRING), (o, v) -> o.roadSizeZ = v, o -> o.roadSizeZ).add()
                .append(new KeyedCodec<>("@DefaultName", Codec.STRING), (o, v) -> o.defaultName = v, o -> o.defaultName)
                .add()
                .append(new KeyedCodec<>("@MaxPlots", Codec.STRING), (o, v) -> o.maxPlots = v, o -> o.maxPlots).add()
                .append(new KeyedCodec<>("@MaxLimit", Codec.STRING), (o, v) -> o.maxLimit = v, o -> o.maxLimit).add()
                .append(new KeyedCodec<>("@Holograms", Codec.BOOLEAN), (o, v) -> o.holograms = v, o -> o.holograms).add()
                .append(new KeyedCodec<>("@Economy", Codec.BOOLEAN), (o, v) -> o.economy = v, o -> o.economy).add()
                .append(new KeyedCodec<>("@TransferName", Codec.STRING), (o, v) -> o.transferName = v,
                        o -> o.transferName)
                .add()
                .build();

        public String action;
        public String param;
        public String plotSizeX;
        public String plotSizeZ;
        public String roadSizeX;
        public String roadSizeZ;
        public String defaultName;
        public String maxPlots;
        public String maxLimit;
        public Boolean holograms;
        public Boolean economy;
        public String transferName;
    }
}
