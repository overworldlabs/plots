package dev.stoshe.plots.command.sub;

import dev.stoshe.plots.util.CommandSenderIdentity;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.command.CommandArgs;
import dev.stoshe.plots.manager.TranslationManager;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Command: {@code /plot spawn [world]} — teleports the player to a plot world's
 * spawn. With no argument it uses the player's current plot world, falling back
 * to the default world.
 */
public class PlotSpawnCommand extends CommandBase {
    private final IPlotManager plotManager;
    private final OptionalArg<String> worldArg;

    /**
     * @param plotManager the plot manager
     */
    public PlotSpawnCommand(@Nonnull IPlotManager plotManager) {
        super("spawn", "Teleport to a plot world spawn");
        this.plotManager = plotManager;
        this.worldArg = CommandArgs.optional(this, "world", "Plot world to teleport to",
                new dev.stoshe.plots.command.SuggestingStringArgumentType("world", "Plot world to teleport to",
                        dev.stoshe.plots.command.CommandSuggestions::plotWorldNames));
        requirePermission(IPlotManager.PERM_PLOT);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(), IPlotManager.PERM_SPAWN);
        }

        if (!context.isPlayer()) {
            context.sender().sendMessage(ChatUtil.error(tm.get("general.only_players")));
            return;
        }

        Ref<EntityStore> ref = context.senderAsPlayerRef();
        if (ref == null) {
            return;
        }

        java.util.UUID senderUuid = CommandSenderIdentity.uuid(context.sender());
        if (senderUuid == null) {
            return;
        }

        PlayerRef playerObj = Universe.get().getPlayer(senderUuid);
        if (playerObj == null || playerObj.getWorldUuid() == null) {
            return;
        }

        World currentWorld = Universe.get().getWorld(playerObj.getWorldUuid());
        if (currentWorld == null) {
            return;
        }

        String requestedWorld = worldArg.get(context);

        currentWorld.execute(() -> teleportToSpawn(ref, currentWorld, requestedWorld, tm));
    }

    /**
     * Resolves the target world and teleports the player to its spawn.
     *
     * @param ref            the player entity reference
     * @param currentWorld   the world the player is currently in
     * @param requestedWorld the requested world name, or {@code null} for default
     * @param tm             the translation manager
     */
    private void teleportToSpawn(Ref<EntityStore> ref, World currentWorld, @Nullable String requestedWorld,
            TranslationManager tm) {
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        PlotConfig config = this.plotManager.getConfig();
        String targetWorld = resolveTargetWorld(config, currentWorld.getName(), requestedWorld);
        if (targetWorld == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("teleport.world_not_managed", "world",
                    String.valueOf(requestedWorld))));
            return;
        }

        World plotWorld = Universe.get().getWorlds().get(targetWorld);
        if (plotWorld == null) {
            playerRef.sendMessage(ChatUtil.error(tm.get("teleport.world_not_loaded")));
            return;
        }

        try {
            this.plotManager.teleportPlayerToWorldSpawn(store, ref, targetWorld);
            playerRef.sendMessage(ChatUtil.success(tm.get("teleport.teleporting")));
        } catch (Exception e) {
            playerRef.sendMessage(ChatUtil.error(tm.get("teleport.failed", "error", e.getMessage())));
        }
    }

    /**
     * Resolves which world to teleport to.
     *
     * @param config         the plugin config
     * @param currentWorld   the player's current world name
     * @param requestedWorld the requested world name, or {@code null}
     * @return the canonical managed world name, or {@code null} when a requested
     *         world is not managed
     */
    @Nullable
    private String resolveTargetWorld(PlotConfig config, String currentWorld, @Nullable String requestedWorld) {
        if (requestedWorld != null && !requestedWorld.isBlank()) {
            return canonicalWorldName(config, requestedWorld);
        }
        if (config.isManagedWorld(currentWorld)) {
            return currentWorld;
        }
        return config.getDefaultWorldName();
    }

    /**
     * @return the configured world name matching {@code requested} (case-insensitive),
     *         or {@code null} when no managed world matches
     */
    @Nullable
    private String canonicalWorldName(PlotConfig config, String requested) {
        for (String name : config.getWorldNames()) {
            if (name.equalsIgnoreCase(requested)) {
                return name;
            }
        }
        return null;
    }
}
