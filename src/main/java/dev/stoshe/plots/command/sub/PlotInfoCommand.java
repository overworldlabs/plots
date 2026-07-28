package dev.stoshe.plots.command.sub;

import dev.stoshe.plots.util.CommandSenderIdentity;
import com.hypixel.hytale.server.core.universe.Universe;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ColorConstants;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Command: /plot info
 * Displays information about the current plot in chat using the new color
 * system.
 */
public class PlotInfoCommand extends CommandBase {
        private final IPlotManager plotManager;

        public PlotInfoCommand(@Nonnull IPlotManager plotManager) {
                super("info", "Show plot information");
                this.plotManager = plotManager;
                requirePermission(IPlotManager.PERM_PLOT);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
                TranslationManager tm = Plots.getInstance().getTranslationManager();

                if (!PermissionUtil.hasAdminPermission(context.sender())) {
                        CommandUtil.requirePermission(context.sender(), IPlotManager.PERM_INFO);
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

                        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                        if (transform == null)
                                return;

                        Vector3d pos = transform.getPosition();
                        Plot plot = this.plotManager.getPlotAt(currentWorld.getName(), (int) pos.x, (int) pos.z);

                        if (plot == null) {
                                playerRef.sendMessage(ChatUtil.error(tm.get("info.not_in_plot")));
                                return;
                        }

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        String date = sdf.format(new Date(plot.getCreatedAt()));

                        playerRef.sendMessage(ChatUtil.colorize(tm.get("info.header")));

                        playerRef.sendMessage(ChatUtil.builder()
                                        .color(ColorConstants.WHITE).append(tm.get("info.label.name"))
                                        .color(ColorConstants.PLOT_NAME).append(plot.getName())
                                        .build());

                        playerRef.sendMessage(ChatUtil.builder()
                                        .color(ColorConstants.WHITE).append(tm.get("info.label.owner"))
                                        .color(ColorConstants.OWNER_NAME).append(plot.getOwnerName())
                                        .build());

                        playerRef.sendMessage(ChatUtil.builder()
                                        .color(ColorConstants.WHITE).append(tm.get("info.label.grid"))
                                        .color(ColorConstants.SECONDARY)
                                        .append("(" + plot.getGridX() + ", " + plot.getGridZ() + ")")
                                        .build());

                        playerRef.sendMessage(ChatUtil.builder()
                                        .color(ColorConstants.WHITE).append(tm.get("info.label.trusted"))
                                        .color(ColorConstants.SECONDARY)
                                        .append(String.valueOf(plot.getTrustedPlayers().size()))
                                        .build());

                        playerRef.sendMessage(ChatUtil.builder()
                                        .color(ColorConstants.WHITE).append(tm.get("info.label.created"))
                                        .color(ColorConstants.SECONDARY).append(date)
                                        .build());

                        playerRef.sendMessage(ChatUtil.colorize(tm.get("info.footer")));
                });
        }
}
