package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.overworldlabs.plots.manager.PlotManager;

import javax.annotation.Nonnull;

/**
 * Command: /plotworld
 * Teleports player to the plot world
 */
public class PlotWorldCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;

    public PlotWorldCommand(@Nonnull PlotManager plotManager) {
        super("world", "Teleport to plot world");
        this.plotManager = plotManager;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(),
                "plots.world", true)) {
            playerRef.sendMessage(Message.raw("§cYou don't have permission to visit the plot world!"));
            return;
        }

        String plotWorldName = plotManager.getConfig().getPlotWorldName();

        // Get the plot world from Universe
        World plotWorld = Universe.get().getWorlds().get(plotWorldName);

        if (plotWorld == null) {
            // Log to console for administrators
            System.err.println("[Plots] ERROR: Plot world '" + plotWorldName + "' not found!");
            System.err.println("[Plots] Available worlds: " + Universe.get().getWorlds().keySet());
            System.err.println("[Plots] Player '" + playerRef.getUsername() + "' attempted to use /plotworld");

            // Send user-friendly message
            playerRef.sendMessage(Message.raw("§c§lPlot world not found!"));
            playerRef.sendMessage(
                    Message.raw("§7The configured plot world '§e" + plotWorldName + "§7' does not exist."));
            playerRef.sendMessage(Message.raw(""));
            playerRef.sendMessage(Message.raw("§6Available worlds:"));

            // List all available worlds
            for (String worldName : Universe.get().getWorlds().keySet()) {
                playerRef.sendMessage(Message.raw("§e  - " + worldName));
            }

            playerRef.sendMessage(Message.raw(""));
            playerRef.sendMessage(Message.raw("§7Please ask an administrator to:"));
            playerRef.sendMessage(Message.raw("§7  1. Create a world named '§e" + plotWorldName + "§7'"));
            playerRef.sendMessage(Message.raw("§7  2. Or change the plot world name in §econfig.json"));
            return;
        }

        // Check if player is already in the plot world
        if (world.getName().equals(plotWorldName)) {
            playerRef.sendMessage(Message.raw("§eYou are already in the plot world!"));
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

            System.out.println("[Plots] Player '" + playerRef.getUsername() + "' teleported to plot world '"
                    + plotWorldName + "'");
            playerRef.sendMessage(Message.raw("§aTeleporting to plot world..."));
        } catch (Exception e) {
            System.err.println(
                    "[Plots] ERROR: Failed to teleport player '" + playerRef.getUsername() + "' to plot world!");
            System.err.println("[Plots] Exception: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();

            playerRef.sendMessage(Message.raw("§cFailed to teleport to plot world!"));
            playerRef.sendMessage(Message.raw("§7Error: " + e.getMessage()));
        }
    }
}
