package com.overworldlabs.plots.command.sub;

import com.overworldlabs.plots.util.CommandSenderIdentity;

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
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.config.PlotConfig;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;

/**
 * Command: /plot spawn
 * Teleports player to the plot world spawn
 */
public class PlotSpawnCommand extends CommandBase {
    private final IPlotManager plotManager;

    public PlotSpawnCommand(@Nonnull IPlotManager plotManager) {
        super("spawn", "Teleport to plot world spawn");
        this.plotManager = plotManager;
        requirePermission(IPlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(),
                    IPlotManager.PERM_SPAWN);
        }

        if (!context.isPlayer()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.only_players")));
            return;
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null)
            return;

        // Get the player object from Universe (thread-safe) to find their world
        java.util.UUID senderUuid = CommandSenderIdentity.uuid(context.sender());
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
                PlotConfig config = Plots.getInstance().getConfig();
                PlotConfig.SpawnSettings customSpawn = config.getSpawn();

                Vector3d spawnPos;
                Vector3f spawnRot;

                if (customSpawn.CustomSpawn) {
                    spawnPos = new Vector3d(customSpawn.X, customSpawn.Y, customSpawn.Z);
                    spawnRot = new Vector3f(customSpawn.Pitch, customSpawn.Yaw, 0);
                } else {
                    // Get default spawn point (intersection between plot 0,0 and 1,1)
                    int plotX = config.getPlotSizeX();
                    int plotZ = config.getPlotSizeZ();
                    int roadX = config.getRoadSizeX();
                    int roadZ = config.getRoadSizeZ();

                    spawnPos = new Vector3d(plotX + roadX / 2.0, 65, plotZ + roadZ / 2.0);
                    spawnRot = new Vector3f(0, 0, 0);
                }

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
