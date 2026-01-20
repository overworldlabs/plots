package com.overworldlabs.plots.command.sub;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command: /plot list
 * Lists plots owned by the player in chat using the new color system.
 */
public class PlotListCommand extends CommandBase {
    private final PlotManager plotManager;

    public PlotListCommand(@Nonnull PlotManager plotManager) {
        super("list", "List your plots");
        this.plotManager = plotManager;
        requirePermission(PlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!context.sender().hasPermission(PlotManager.PERM_ADMIN)) {
            CommandUtil.requirePermission(context.sender(), PlotManager.PERM_LIST);
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

            String uuidStr = playerRef.getUuid().toString();
            List<Plot> myPlots = this.plotManager.getAllPlots().stream()
                    .filter(p -> p.getOwner().toString().equals(uuidStr))
                    .collect(Collectors.toList());

            if (myPlots.isEmpty()) {
                playerRef.sendMessage(ChatUtil.error(tm.get("list.empty")));
                return;
            }

            playerRef.sendMessage(ChatUtil.colorize(tm.get("list.header", "count", String.valueOf(myPlots.size()))));
            for (Plot plot : myPlots) {
                playerRef.sendMessage(ChatUtil.colorize(tm.get("list.item",
                        "name", plot.getName(),
                        "x", String.valueOf(plot.getGridX()),
                        "z", String.valueOf(plot.getGridZ()))));
            }
        });
    }
}
