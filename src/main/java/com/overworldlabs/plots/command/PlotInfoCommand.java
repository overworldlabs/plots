package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ColorConstants;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Command: /plot info
 * Displays information about the current plot in chat using the new color
 * system.
 */
public class PlotInfoCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;

    public PlotInfoCommand(@Nonnull PlotManager plotManager) {
        super("info", "Show plot information");
        this.plotManager = plotManager;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), "plots.info", true)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null)
            return;

        Vector3d pos = transform.getPosition();
        Plot plot = plotManager.getPlotAt(world.getName(), (int) pos.x, (int) pos.z);

        if (plot == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("info.not_in_plot")));
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(new Date(plot.getCreatedAt()));

        playerRef.sendMessage(ChatUtil.colorize(tm.get("info.header")));

        playerRef.sendMessage(ChatUtil.builder()
                .color(ColorConstants.WHITE).append(tm.get("info.label.name"))
                .color(ColorConstants.PLOT_NAME).append(plot.getName())
                .build());

        playerRef.sendMessage(ChatUtil.builder()
                .color(ColorConstants.WHITE).append(tm.get("info.label.owner"))
                .color(ColorConstants.OWNER_NAME).append(plot.getOwnerName())
                .build());

        playerRef.sendMessage(ChatUtil.builder()
                .color(ColorConstants.WHITE).append(tm.get("info.label.grid"))
                .color(ColorConstants.SECONDARY).append("(" + plot.getGridX() + ", " + plot.getGridZ() + ")")
                .build());

        playerRef.sendMessage(ChatUtil.builder()
                .color(ColorConstants.WHITE).append(tm.get("info.label.trusted"))
                .color(ColorConstants.SECONDARY).append(String.valueOf(plot.getTrustedPlayers().size()))
                .build());

        playerRef.sendMessage(ChatUtil.builder()
                .color(ColorConstants.WHITE).append(tm.get("info.label.created"))
                .color(ColorConstants.SECONDARY).append(date)
                .build());

        playerRef.sendMessage(ChatUtil.colorize(tm.get("info.footer")));
    }
}
