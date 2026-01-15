package com.overworldlabs.plots.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command: /plot list
 * Lists plots owned by the player in chat using the new color system.
 */
public class PlotListCommand extends AbstractPlayerCommand {
    private final PlotManager plotManager;

    public PlotListCommand(@Nonnull PlotManager plotManager) {
        super("list", "List your plots");
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

        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), "plots.list", true)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("general.no_permission")));
            return;
        }

        String uuidStr = playerRef.getUuid().toString();
        List<Plot> myPlots = plotManager.getAllPlots().stream()
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
    }
}
