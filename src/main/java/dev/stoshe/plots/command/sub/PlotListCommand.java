package dev.stoshe.plots.command.sub;

import dev.stoshe.plots.util.CommandSenderIdentity;

import dev.stoshe.plots.util.PlayerIdentity;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command: /plot list
 * Lists plots owned by the player in chat using the new color system.
 */
public class PlotListCommand extends CommandBase {
    private final IPlotManager plotManager;

    public PlotListCommand(@Nonnull IPlotManager plotManager) {
        super("list", "List your plots");
        this.plotManager = plotManager;
        requirePermission(IPlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(),
                    IPlotManager.PERM_LIST);
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

            String uuidStr = PlayerIdentity.uuid(playerRef).toString();
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
                        "plot_name", plot.getName(),
                        "x", String.valueOf(plot.getGridX()),
                        "z", String.valueOf(plot.getGridZ()))));
            }
        });
    }
}
