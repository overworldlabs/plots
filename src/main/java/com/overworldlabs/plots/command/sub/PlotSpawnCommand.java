package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot spawn
 * Teleports player to the plot world spawn
 */
public class PlotSpawnCommand extends CommandBase {
    private final PlotManager plotManager;

    public PlotSpawnCommand(@Nonnull PlotManager plotManager) {
        super("spawn", "Teleport to plot world spawn");
        this.plotManager = plotManager;
        requirePermission(PlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!context.sender().hasPermission(PlotManager.PERM_ADMIN)) {
            CommandUtil.requirePermission(context.sender(), PlotManager.PERM_SPAWN);
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

        PlayerRef playerObj = Universe.get().getPlayer(senderUuid);
        if (playerObj == null)
            return;

        java.util.UUID worldUuid = playerObj.getWorldUuid();
        if (worldUuid == null)
            return;

        World currentWorld = Universe.get().getWorld(worldUuid);
        if (currentWorld == null)
            return;

        // Execute store operations on the world thread
        currentWorld.execute(() -> {
            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null)
                return;

            String plotWorldName = this.plotManager.getConfig().getPlotWorldName();

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
        });
    }
}
