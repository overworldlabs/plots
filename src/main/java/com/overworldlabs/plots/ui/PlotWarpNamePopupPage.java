package com.overworldlabs.plots.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
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

/**
 * Popup to add a named warp at the player's current position inside their plot.
 */
public class PlotWarpNamePopupPage extends InteractiveCustomUIPage<PlotWarpNamePopupPage.PageData> {

    private static final int MAX_NAME_LENGTH = 32;

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private final String error;

    private PlotWarpNamePopupPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            @Nullable String error) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.error = error;
    }

    public static PlotWarpNamePopupPage create(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld,
            @Nonnull Plots plugin) {
        return new PlotWarpNamePopupPage(playerRef, sourceWorld, plugin, null);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotWarpNamePopup.ui");
        commandBuilder.set("#WarpNamePopupTitle.Text", "ADD WARP");
        commandBuilder.set("#WarpNameHint.Text", "Warp name (max " + MAX_NAME_LENGTH + " characters):");
        commandBuilder.set("#WarpNameInput.Value", "");
        commandBuilder.set("#BtnWarpNameCancel.Text", "Cancel");
        commandBuilder.set("#BtnWarpNameSave.Text", "Save");
        boolean hasError = this.error != null && !this.error.isBlank();
        commandBuilder.set("#WarpNameError.Visible", hasError);
        commandBuilder.set("#WarpNameError.Text", hasError ? this.error : "");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWarpNameCancel",
                EventData.of("Action", "Cancel"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWarpNameSave",
                EventData.of("Action", "Save").append("@NameInput", "#WarpNameInput.Value"), false);
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
        if (!"Save".equals(action)) {
            return;
        }
        Plot plot = resolvePlot(ref, store);
        if (plot == null) {
            PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin);
            return;
        }
        String name = normalize(data.nameInput);
        if (name.isBlank()) {
            reopenWithError(player, ref, store, "Name cannot be empty.");
            return;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            reopenWithError(player, ref, store, "Name is too long (max " + MAX_NAME_LENGTH + ").");
            return;
        }

        // Must stand inside this plot to capture the warp position.
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            reopenWithError(player, ref, store, "Your position is unavailable.");
            return;
        }
        org.joml.Vector3d pos = transform.getPosition();
        com.hypixel.hytale.math.vector.Rotation3f rot = transform.getRotation();
        if (!plot.equals(this.plugin.getPlotManager().getPlotAt(this.sourceWorld.getName(),
                (int) Math.floor(pos.x), (int) Math.floor(pos.z)))) {
            reopenWithError(player, ref, store, "Stand inside this plot to add a warp.");
            return;
        }

        // Re-check the per-plot warp limit (a warp may not already exist with this name).
        int max = this.plugin.getPlotManager().getMaxWarpsPerPlot();
        if (plot.getWarp(name) == null && plot.getWarpCount() >= max) {
            reopenWithError(player, ref, store, "Warp limit reached (max " + max + ").");
            return;
        }

        float yaw = rot != null ? rot.yaw() : 0f;
        float pitch = rot != null ? rot.pitch() : 0f;
        try {
            plot.setWarp(name, pos.x, pos.y, pos.z, yaw, pitch);
            this.plugin.getPlotManager().savePlots();
            player.sendMessage(ChatUtil.success("Warp '" + name + "' set to your current position."));
        } catch (Exception ignored) {
        }
        PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin);
    }

    private void reopenWithError(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String err) {
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotWarpNamePopupPage(this.playerRef, resolveWorld(player), this.plugin, err));
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

    private World resolveWorld(Player player) {
        try {
            if (player != null && player.getWorld() != null) {
                return player.getWorld();
            }
        } catch (Exception ignored) {
        }
        return this.sourceWorld;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("@NameInput", Codec.STRING), (o, v) -> o.nameInput = v, o -> o.nameInput).add()
                .build();
        public String action;
        public String nameInput;
    }
}
