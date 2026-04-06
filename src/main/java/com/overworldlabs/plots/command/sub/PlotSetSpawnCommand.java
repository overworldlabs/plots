package com.overworldlabs.plots.command.sub;

import com.overworldlabs.plots.util.CommandSenderIdentity;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
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
 * Command: /plot setspawn
 * Sets the plot world spawn point to the player's current position
 */
public class PlotSetSpawnCommand extends CommandBase {
    public PlotSetSpawnCommand(@Nonnull IPlotManager plotManager) {
        super("setspawn", "Set the plot world spawn point");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!context.isPlayer()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.only_players")));
            return;
        }

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null)
            return;

        java.util.UUID senderUuid = CommandSenderIdentity.uuid(context.sender());
        if (senderUuid == null)
            return;

        PlayerRef playerObj = Universe.get().getPlayer(senderUuid);
        if (playerObj == null)
            return;

        java.util.UUID worldUuid = playerObj.getWorldUuid();
        if (worldUuid == null)
            return;

        World world = Universe.get().getWorld(worldUuid);
        if (world == null)
            return;

        world.execute(() -> {
            Store<EntityStore> store = ref.getStore();
            TransformComponent transformComp = store.getComponent(ref, TransformComponent.getComponentType());

            if (transformComp == null) {
                playerObj.sendMessage(ChatUtil.error(tm.get("teleport.position_unavailable")));
                return;
            }

            Vector3d pos = transformComp.getPosition();
            Vector3f rot = transformComp.getRotation();

            PlotConfig config = Plots.getInstance().getConfig();
            PlotConfig.SpawnSettings spawn = config.getSpawn();

            spawn.X = pos.x;
            spawn.Y = pos.y;
            spawn.Z = pos.z;
            spawn.Pitch = rot.getPitch();
            spawn.Yaw = rot.getYaw();
            spawn.CustomSpawn = true;

            Plots.getInstance().saveConfig(config);

            playerObj.sendMessage(ChatUtil.success(tm.get("teleport.set_spawn", "world", world.getName())));
        });
    }
}
