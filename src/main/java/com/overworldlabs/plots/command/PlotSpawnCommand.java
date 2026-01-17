package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot spawn
 * Teleports player to the plot world spawn
 */
public class PlotSpawnCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;

    public PlotSpawnCommand(@Nonnull PlotManager plotManager) {
        super("spawn", "Teleport to plot world spawn");
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

        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(),
                PlotManager.PERM_SPAWN, true)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        String plotWorldName = plotManager.getConfig().getPlotWorldName();

        // Get the plot world from Universe
        World plotWorld = Universe.get().getWorlds().get(plotWorldName);

        if (plotWorld == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("teleport.world_not_loaded")));
            return;
        }

        try {
            // Get spawn point of plot world (center at 0, 64, 0)
            Vector3d spawnPos = new Vector3d(0.5, 64, 0.5);
            Vector3f spawnRot = new Vector3f(0, 0, 0); // No rotation

            // Create teleport to plot world
            Teleport teleport = new Teleport(plotWorld, spawnPos, spawnRot);

            // Add teleport component to player
            store.addComponent(ref, Teleport.getComponentType(), teleport);

            playerRef.sendMessage(ChatUtil.success(tm.get("teleport.teleporting")));
        } catch (Exception e) {
            playerRef.sendMessage(ChatUtil.error(tm.get("teleport.failed", "error", e.getMessage())));
        }
    }
}
