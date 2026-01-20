package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
public class PlotRenameCommand extends CommandBase {
    private final PlotManager plotManager;
    private final RequiredArg<String> nameArg;

    public PlotRenameCommand(@Nonnull PlotManager plotManager) {
        super("rename", "Rename the plot you are standing on");
        this.plotManager = plotManager;
        this.nameArg = (RequiredArg<String>) withRequiredArg("name", "The new name for the plot", ArgTypes.STRING);
        requirePermission(PlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!context.sender().hasPermission(PlotManager.PERM_ADMIN)) {
            CommandUtil.requirePermission(context.sender(), PlotManager.PERM_RENAME);
        }

        if (!context.isPlayer()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.only_players")));
            return;
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null)
            return;

        // Get the player object from Universe (thread-safe) to find their world
        java.util.UUID senderUuid = context.sender().getUuid();
        if (senderUuid == null)
            return;

        PlayerRef playerObj = com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(senderUuid);
        if (playerObj == null)
            return;

        java.util.UUID worldUuid = playerObj.getWorldUuid();
        if (worldUuid == null)
            return;

        World currentWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
        if (currentWorld == null)
            return;

        // Execute store operations on the world thread
        currentWorld.execute(() -> {
            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null)
                return;

            // Get plot at player location
            Vector3d pos = playerRef.getTransform().getPosition();
            Plot plot = this.plotManager.getPlotAt(currentWorld.getName(), (int) pos.x, (int) pos.z);

            if (plot == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            // Ownership check
            if (!plot.getOwner().equals(playerRef.getUuid())) {
                if (!context.sender().hasPermission(PlotManager.PERM_ADMIN)) {
                    CommandUtil.requirePermission(context.sender(), PlotManager.PERM_ADMIN);
                }
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
            Plots.getInstance().getHologramManager().updateHologram(plot, store);
        });
    }
}
