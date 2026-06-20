package com.overworldlabs.plots.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Per-player HUD shown inside plot worlds. Renders the current plot's name,
 * owner and a short info line, or a "press Alt" hint when the player stands on
 * a road (no plot). Driven by {@code PlotNotificationSystem}.
 */
public class PlotInfoHUD extends CustomUIHud {

    private enum Mode {
        PLOT, FALLBACK, HIDDEN
    }

    private String plotName = "";
    private String ownerPlotName = "";
    private String infoLine = "";
    private String fallbackText = "";
    private Mode mode = Mode.HIDDEN;

    public PlotInfoHUD(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("HUD/PlotInfoHud.ui");
        pushData(builder);
    }

    /** Show the plot panel (name / owner / info). */
    public void setPlotInfo(@Nonnull String plotName, @Nonnull String ownerPlotName, @Nonnull String infoLine) {
        this.plotName = plotName;
        this.ownerPlotName = ownerPlotName;
        this.infoLine = infoLine;
        this.mode = Mode.PLOT;
    }

    /** Backwards-compatible overload (no info line). */
    public void setPlotInfo(@Nonnull String plotName, @Nonnull String ownerPlotName) {
        setPlotInfo(plotName, ownerPlotName, "");
    }

    /** Show the "press Alt to manage" hint (used on roads inside a plot world). */
    public void setFallback(@Nonnull String fallbackText) {
        this.fallbackText = fallbackText;
        this.mode = Mode.FALLBACK;
    }

    /** Hide the HUD entirely. */
    public void clearPlotInfo() {
        this.plotName = "";
        this.ownerPlotName = "";
        this.infoLine = "";
        this.fallbackText = "";
        this.mode = Mode.HIDDEN;
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
        boolean visible = mode != Mode.HIDDEN;
        boolean plotMode = mode == Mode.PLOT;
        boolean fallbackMode = mode == Mode.FALLBACK;

        builder.set("#PlotInfoRoot.Visible", visible);
        builder.set("#PlotInfoBody.Visible", plotMode);
        builder.set("#PlotFallback.Visible", fallbackMode);

        builder.set("#PlotName.Text", plotName);
        builder.set("#PlotOwner.Text", ownerPlotName);
        builder.set("#PlotInfoLine.Text", infoLine);
        builder.set("#PlotFallbackText.Text", fallbackText);
    }
}
