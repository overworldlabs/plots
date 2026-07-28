package dev.stoshe.plots.command.sub;

import dev.stoshe.plots.util.CommandSenderIdentity;

import dev.stoshe.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.universe.Universe;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.command.CommandArgs;
import dev.stoshe.plots.command.PlotConfirmationService;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Command: /plot rename <name>
 * Renames the current plot the player is standing on.
 * 
 * For multi-word names, use quotes: /plot rename "Minha Casa Boladona"
 * Single words don't need quotes: /plot rename MinhaCasa
 */
public class PlotRenameCommand extends CommandBase {
    private final IPlotManager plotManager;
    private final RequiredArg<String> nameArg;

    public PlotRenameCommand(@Nonnull IPlotManager plotManager) {
        super("rename", "Rename the plot you are standing on");
        setAllowsExtraArguments(true);
        this.plotManager = plotManager;
        this.nameArg = CommandArgs.required(this, "name", "The new name for the plot", ArgTypes.STRING);
        requirePermission(IPlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(), IPlotManager.PERM_RENAME);
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

            // Get plot at player location
            Vector3d pos = playerRef.getTransform().getPosition();
            Plot plot = this.plotManager.getPlotAt(currentWorld.getName(), (int) pos.x, (int) pos.z);

            if (plot == null) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }

            // Ownership check
            if (!plot.getOwner().equals(PlayerIdentity.uuid(playerRef))) {
                if (!PermissionUtil.hasAdminPermission(context.sender())) {
                    CommandUtil.requirePermission(context.sender(), IPlotManager.PERM_ADMIN);
                }
            }

            String newName = extractRenameValue(context);
            if (newName == null || newName.trim().isEmpty()) {
                playerRef.sendMessage(ChatUtil.error(tm.get("management.rename_provide_name")));
                return;
            }

            final int gridX = plot.getGridX();
            final int gridZ = plot.getGridZ();
            final String worldName = plot.getWorld();
            final String confirmedName = newName.trim();

            PlotConfirmationService.getInstance().request(context.sender(), tm,
                    tm.get("confirm.action_rename",
                            "name", confirmedName,
                            "plot_name", confirmedName), () -> currentWorld.execute(() -> {
                        Plot currentPlot = this.plotManager.getPlot(worldName, gridX, gridZ);
                        if (currentPlot == null) {
                            playerRef.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                            return;
                        }

                        if (!currentPlot.hasPermission(PlayerIdentity.uuid(playerRef))
                                && !PermissionUtil.hasAdminPermission(context.sender())) {
                            playerRef.sendMessage(ChatUtil.error(tm.get("management.no_permission")));
                            return;
                        }

                        currentPlot.setName(confirmedName);
                        playerRef.sendMessage(ChatUtil.success(tm.get("management.renamed",
                                "name", confirmedName,
                                "plot_name", confirmedName)));

                        Plots.getInstance().getRadarManager().updatePlotMarker(currentPlot);
                        Plots.getInstance().getHologramManager().updateHologram(currentPlot, store);
                    }));
        });
    }

    private String extractRenameValue(@Nonnull CommandContext context) {
        String raw = context.getInputString();
        if (raw == null || raw.isBlank()) {
            return normalizeName(nameArg.get(context));
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 3) {
            return normalizeName(nameArg.get(context));
        }

        String sub = parts[1].toLowerCase(Locale.ROOT);
        if (!sub.equals("rename")) {
            return normalizeName(nameArg.get(context));
        }

        int valueStart = indexAfterTokens(trimmed, 2);
        if (valueStart < 0 || valueStart >= trimmed.length()) {
            return normalizeName(nameArg.get(context));
        }
        return normalizeName(trimmed.substring(valueStart));
    }

    private int indexAfterTokens(String value, int tokenCount) {
        boolean inToken = false;
        int consumed = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean ws = Character.isWhitespace(ch);
            if (!ws && !inToken) {
                inToken = true;
            } else if (ws && inToken) {
                inToken = false;
                consumed++;
                if (consumed == tokenCount) {
                    while (i < value.length() && Character.isWhitespace(value.charAt(i))) {
                        i++;
                    }
                    return i;
                }
            }
        }
        if (inToken) {
            consumed++;
            if (consumed == tokenCount) {
                return value.length();
            }
        }
        return -1;
    }

    private String normalizeName(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (value.isEmpty()) {
            return null;
        }
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value.isEmpty() ? null : value;
    }
}
