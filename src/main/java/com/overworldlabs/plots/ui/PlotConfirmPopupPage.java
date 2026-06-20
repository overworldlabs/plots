package com.overworldlabs.plots.ui;

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
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Generic confirm popup for destructive plot actions (delete / transfer).
 */
public class PlotConfirmPopupPage extends InteractiveCustomUIPage<PlotConfirmPopupPage.PageData> {

    public enum ActionType {
        DELETE, TRANSFER
    }

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private final ActionType actionType;
    private final int gridX;
    private final int gridZ;
    private final String plotName;
    private final UUID targetUuid;
    private final String targetName;

    private PlotConfirmPopupPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            @Nonnull ActionType actionType, int gridX, int gridZ, @Nonnull String plotName,
            @Nullable UUID targetUuid, @Nullable String targetName) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.actionType = actionType;
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.plotName = plotName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    public static PlotConfirmPopupPage forDelete(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld,
            @Nonnull Plots plugin, int gridX, int gridZ, @Nonnull String plotName) {
        return new PlotConfirmPopupPage(playerRef, sourceWorld, plugin, ActionType.DELETE, gridX, gridZ, plotName,
                null, null);
    }

    public static PlotConfirmPopupPage forTransfer(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld,
            @Nonnull Plots plugin, int gridX, int gridZ, @Nonnull String plotName, @Nonnull UUID targetUuid,
            @Nonnull String targetName) {
        return new PlotConfirmPopupPage(playerRef, sourceWorld, plugin, ActionType.TRANSFER, gridX, gridZ, plotName,
                targetUuid, targetName);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotConfirmPopup.ui");
        commandBuilder.set("#ConfirmPopupTitle.Text", this.actionType == ActionType.DELETE
                ? "DELETE PLOT" : "TRANSFER PLOT");
        commandBuilder.set("#ConfirmPopupMessage.Text", this.actionType == ActionType.DELETE
                ? "Delete plot '" + safe(this.plotName) + "'? This cannot be undone."
                : "Transfer plot '" + safe(this.plotName) + "' to " + safe(this.targetName) + "?");
        commandBuilder.set("#BtnConfirmCancel.Text", "Cancel");
        commandBuilder.set("#BtnConfirmConfirm.Text", this.actionType == ActionType.DELETE ? "Delete" : "Transfer");
        commandBuilder.set("#ConfirmPopupError.Visible", false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnConfirmCancel",
                EventData.of("Action", "Cancel"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnConfirmConfirm",
                EventData.of("Action", "Confirm"), false);
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
        if ("Cancel".equals(action)) {
            PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin);
            return;
        }
        if (!"Confirm".equals(action)) {
            return;
        }
        if (this.actionType == ActionType.DELETE) {
            performDelete(player, ref, store);
        } else {
            performTransfer(player, ref, store, world);
        }
    }

    private void performDelete(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            this.plugin.getPlotManager().unclaimPlot(this.gridX, this.gridZ);
            this.plugin.getPlotManager().savePlots();
            player.sendMessage(ChatUtil.success("Plot deleted."));
        } catch (Exception ex) {
            player.sendMessage(ChatUtil.error("Failed to delete plot."));
        }
        player.getPageManager().setPage(ref, store, Page.None);
    }

    private void performTransfer(Player player, Ref<EntityStore> ref, Store<EntityStore> store, World world) {
        try {
            Plot plot = this.plugin.getPlotManager().getPlotByGrid(this.gridX, this.gridZ);
            if (plot != null && this.targetUuid != null) {
                plot.setOwner(this.targetUuid);
                plot.setOwnerName(safe(this.targetName));
                plot.removeTrustedPlayer(this.targetUuid);
                this.plugin.getPlotManager().savePlots();
                player.sendMessage(ChatUtil.success("Plot transferred to " + safe(this.targetName) + "."));
            } else {
                player.sendMessage(ChatUtil.error("Could not transfer plot."));
            }
        } catch (Exception ex) {
            player.sendMessage(ChatUtil.error("Failed to transfer plot."));
        }
        PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin);
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
