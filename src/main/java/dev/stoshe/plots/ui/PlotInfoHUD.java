package dev.stoshe.plots.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.stoshe.plots.util.Tr;

import javax.annotation.Nonnull;

/**
 * Per-player HUD shown inside plot worlds. Renders the current plot's name,
 * owner and size while the player stands on a plot; hidden entirely on roads.
 * Driven by {@code PlotNotificationSystem}.
 */
public class PlotInfoHUD extends CustomUIHud {

    private enum Mode {
        PLOT, HIDDEN
    }

    private String plotName = "";
    private String ownerName = "";
    private String sizeText = "";
    private Mode mode = Mode.HIDDEN;
    /** Temporarily hides the HUD (e.g. while a full-screen plot page is open) without losing the plot state. */
    private boolean suppressed = false;

    public PlotInfoHUD(@Nonnull PlayerRef playerRef) {
        super(playerRef, "plots_info_hud");
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("HUD/PlotInfoHud.ui");
        pushData(builder);
    }

    /** Show the plot banner (name in the image, owner + size in the bottom bar). */
    public void setPlotInfo(@Nonnull String plotName, @Nonnull String ownerName, @Nonnull String sizeText) {
        this.plotName = plotName;
        this.ownerName = ownerName;
        this.sizeText = sizeText;
        this.mode = Mode.PLOT;
    }

    /** Backwards-compatible overload (no size). */
    public void setPlotInfo(@Nonnull String plotName, @Nonnull String ownerName) {
        setPlotInfo(plotName, ownerName, "");
    }

    /**
     * Hides/restores the HUD without discarding the current plot info. Used to get
     * out of the way while a full-screen page (menu/admin/popup) is open, mirroring
     * how the native HUD disappears.
     */
    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
    }

    /** Hide the HUD entirely (player stepped onto a road / left the plot world). */
    public void clearPlotInfo() {
        this.plotName = "";
        this.ownerName = "";
        this.sizeText = "";
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
        boolean show = mode == Mode.PLOT && !suppressed;

        builder.set("#PlotInfoRoot.Visible", show);
        builder.set("#PlotBanner.Visible", show);

        // Static, localized captions (kept in sync with the active language).
        builder.set("#PlotHudTitle.Text", Tr.t("notification.hud_title"));
        builder.set("#PlotOwnerTitle.Text", Tr.t("notification.hud_status_owner"));
        builder.set("#PlotSizeTitle.Text", Tr.t("notification.hud_size"));
        builder.set("#PlotHudFooter.Text", Tr.t("notification.hud_hint"));

        builder.set("#PlotName.Text", plotName);
        builder.set("#PlotOwnerValue.Text", ownerName);
        builder.set("#PlotSizeValue.Text", sizeText);
    }
}
