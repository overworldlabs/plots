package com.overworldlabs.plots.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class PlotInfoHUD extends CustomUIHud {
    private String plotName = "";
    private String ownerPlotName = "";
    private boolean visible = false;

    public PlotInfoHUD(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("HUD/PlotInfoHud.ui");
        pushData(builder);
    }

    public void setPlotInfo(@Nonnull String plotName, @Nonnull String ownerPlotName) {
        this.plotName = plotName;
        this.ownerPlotName = ownerPlotName;
        this.visible = true;
    }

    public void clearPlotInfo() {
        this.plotName = "";
        this.ownerPlotName = "";
        this.visible = false;
    }

    public void requestUpdate() {
        if (getPlayerRef() == null || !getPlayerRef().isValid()) {
            return;
        }

        UICommandBuilder builder = new UICommandBuilder();
        pushData(builder);
        super.update(false, builder);
    }

    private void pushData(@Nonnull UICommandBuilder builder) {
        builder.set("#PlotInfoRoot.Visible", visible);
        builder.set("#PlotName.Text", plotName);
        builder.set("#PlotOwner.Text", ownerPlotName);
    }
}
