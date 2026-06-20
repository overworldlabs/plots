package com.overworldlabs.plots.ui;

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
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;

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

        // Navigation bindings.
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Action", "Close"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabSettings",
                EventData.of("Action", "TabSettings"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabPlots",
                EventData.of("Action", "TabPlots"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabStats",
                EventData.of("Action", "TabStats"), false);

        boolean admin = isAdmin();
        commandBuilder.set("#PermNotice.Visible", !admin);
        commandBuilder.set("#PermNotice.Text", admin ? "" : "You do not have permission to use the admin dashboard.");

        String selectedTab = switch (this.currentTab) {
            case SETTINGS -> "Settings";
            case PLOTS -> "Plots";
            case STATS -> "Stats";
        };
        commandBuilder.set("#TabNav.SelectedTab", selectedTab);
        commandBuilder.set("#RightTitle.Text", switch (this.currentTab) {
            case SETTINGS -> "GLOBAL SETTINGS";
            case PLOTS -> "ALL PLOTS";
            case STATS -> "STATISTICS";
        });

        commandBuilder.set("#PanelSettings.Visible", this.currentTab == Tab.SETTINGS);
        commandBuilder.set("#PanelPlots.Visible", this.currentTab == Tab.PLOTS);
        commandBuilder.set("#PanelStats.Visible", this.currentTab == Tab.STATS);

        buildSettings(commandBuilder, eventBuilder, admin);
        buildPlots(commandBuilder, eventBuilder, admin);
        buildStats(commandBuilder);
    }

    // -------------------------------------------------------------------- SETTINGS

    private void buildSettings(UICommandBuilder cb, UIEventBuilder eb, boolean admin) {
        PlotConfig config = resolveConfig();

        // Editable (setters exist on PlotConfig).
        cb.set("#SetPlotSizeX.Value", config != null ? String.valueOf(config.getPlotSizeX()) : "");
        cb.set("#SetPlotSizeZ.Value", config != null ? String.valueOf(config.getPlotSizeZ()) : "");
        cb.set("#SetRoadSizeX.Value", config != null ? String.valueOf(config.getRoadSizeX()) : "");
        cb.set("#SetRoadSizeZ.Value", config != null ? String.valueOf(config.getRoadSizeZ()) : "");

        // Read-only (no public setter / no exposed config save API).
        cb.set("#SetWorldName.Text", config != null ? safe(config.getPlotWorldName()) : "-");
        cb.set("#SetManagedWorld.Text", config != null && config.getPlotWorldName() != null
                && config.isManagedWorld(config.getPlotWorldName()) ? "Yes" : "No");
        cb.set("#SetDefaultName.Text", config != null ? safe(config.getDefaultPlotNameTemplate()) : "-");
        cb.set("#SetMaxPlots.Text", config != null ? String.valueOf(config.getMaxPlotsDefaultValue()) : "-");
        cb.set("#SetMaxLimit.Text", config != null ? String.valueOf(config.getMaxPlotLimit()) : "-");
        cb.set("#SetHologram.Text", config != null && config.isHologramEnabled() ? "Enabled" : "Disabled");
        cb.set("#SetEconomy.Text", config != null && config.isEconomyEnabled() ? "Enabled" : "Disabled");

        cb.set("#SettingsNote.Text",
                "Size changes are saved to config.json and persist across restarts. "
                        + "They affect newly generated plots; existing plots keep their layout.");

        EventData save = EventData.of("Action", "SaveSettings")
                .append("@PlotSizeX", "#SetPlotSizeX.Value")
                .append("@PlotSizeZ", "#SetPlotSizeZ.Value")
                .append("@RoadSizeX", "#SetRoadSizeX.Value")
                .append("@RoadSizeZ", "#SetRoadSizeZ.Value");
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
        cb.set("#AdmPageLabel.Text", total == 0
                ? "No plots"
                : "Page " + (this.page + 1) + " / " + (maxPage + 1) + "  (" + total + " plots)");

        eb.addEventBinding(CustomUIEventBindingType.Activating, "#AdmPrev",
                EventData.of("Action", "PrevPage"), false);
        eb.addEventBinding(CustomUIEventBindingType.Activating, "#AdmNext",
                EventData.of("Action", "NextPage"), false);

        // Transfer sub-panel (only meaningful while a transfer target is selected).
        boolean transferring = this.transferGrid != null;
        cb.set("#AdmTransferPanel.Visible", transferring);
        if (transferring) {
            Plot target = safePlotByGrid(this.transferGrid[0], this.transferGrid[1]);
            cb.set("#AdmTransferLabel.Text", "Transfer plot "
                    + (target != null ? "'" + safe(target.getName()) + "' " : "")
                    + "(" + this.transferGrid[0] + ", " + this.transferGrid[1] + ") to:");
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
            player.sendMessage(ChatUtil.error("You do not have permission to use the admin dashboard."));
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        switch (action) {
            case "TabSettings" -> reopen(player, ref, store, Tab.SETTINGS, 0, null);
            case "TabPlots" -> reopen(player, ref, store, Tab.PLOTS, this.page, null);
            case "TabStats" -> reopen(player, ref, store, Tab.STATS, 0, null);
            case "PrevPage" -> reopen(player, ref, store, Tab.PLOTS, this.page - 1, this.transferGrid);
            case "NextPage" -> reopen(player, ref, store, Tab.PLOTS, this.page + 1, this.transferGrid);
            case "SaveSettings" -> {
                handleSaveSettings(player, data);
                reopen(player, ref, store, Tab.SETTINGS, 0, null);
            }
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

    private void handleSaveSettings(Player player, PageData data) {
        PlotConfig config = resolveConfig();
        if (config == null) {
            player.sendMessage(ChatUtil.error("Configuration is unavailable."));
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
        if (changed) {
            this.plugin.saveConfig(config);
            player.sendMessage(ChatUtil.success("Settings saved to config.json — persists across restarts."));
        } else {
            player.sendMessage(ChatUtil.info("No valid changes to apply."));
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
            player.sendMessage(ChatUtil.error("Failed to teleport to plot."));
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
            player.sendMessage(ChatUtil.error("Enter a player name to transfer to."));
            reopen(player, ref, store, Tab.PLOTS, this.page, this.transferGrid);
            return;
        }
        PlayerRef target = null;
        try {
            target = Universe.get().getPlayerByUsername(typed, NameMatching.EXACT);
        } catch (Exception ignored) {
        }
        if (target == null || target.getUuid() == null) {
            player.sendMessage(ChatUtil.error("Player '" + typed + "' is not online."));
            reopen(player, ref, store, Tab.PLOTS, this.page, this.transferGrid);
            return;
        }
        Plot plot = safePlotByGrid(this.transferGrid[0], this.transferGrid[1]);
        if (plot == null) {
            player.sendMessage(ChatUtil.error("Plot no longer exists."));
            reopen(player, ref, store, Tab.PLOTS, this.page, null);
            return;
        }
        try {
            plot.setOwner(target.getUuid());
            plot.setOwnerName(safe(target.getUsername()));
            plot.removeTrustedPlayer(target.getUuid());
            this.plugin.getPlotManager().savePlots();
            player.sendMessage(ChatUtil.success("Transferred plot to " + safe(target.getUsername()) + "."));
        } catch (Exception ex) {
            player.sendMessage(ChatUtil.error("Failed to transfer plot."));
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
            return this.plugin.getPlotManager().getPlotByGrid(gx, gz);
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
        public String transferName;
    }
}
