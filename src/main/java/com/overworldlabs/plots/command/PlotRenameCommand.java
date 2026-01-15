package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot rename <name>
 * Renames the current plot the player is standing on.
 * 
 * For multi-word names, use quotes: /plot rename "Minha Casa Boladona"
 * Single words don't need quotes: /plot rename MinhaCasa
 */
public class PlotRenameCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;
    private final RequiredArg<String> nameArg;

    public PlotRenameCommand(@Nonnull PlotManager plotManager) {
        super("rename", "Rename the plot you are standing on");
        this.plotManager = plotManager;
        this.nameArg = withRequiredArg("name", "The new name for the plot", ArgTypes.STRING);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        TranslationManager tm = Plots.getInstance().getTranslationManager();

        // Permission check
        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(),
                PlotManager.PERM_RENAME, true)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        // Get plot at player location
        Vector3d pos = playerRef.getTransform().getPosition();
        Plot plot = plotManager.getPlotAt(world.getName(), (int) pos.x, (int) pos.z);

        if (plot == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
            return;
        }

        // Ownership check
        if (!plot.getOwner().equals(playerRef.getUuid())
                && !PermissionsModule.get()
                        .hasPermission(playerRef.getUuid(), PlotManager.PERM_ADMIN)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.not_owner")));
            return;
        }

        String newName = nameArg.get(context);
        if (newName == null || newName.trim().isEmpty()) {
            playerRef.sendMessage(ChatUtil.error(tm.get("management.rename_provide_name")));
            return;
        }

        plot.setName(newName);
        playerRef.sendMessage(ChatUtil.success(tm.get("management.renamed", "name", newName)));

        // Update radar marker
        Plots.getInstance().getRadarManager().updatePlotMarker(plot);
    }
}
