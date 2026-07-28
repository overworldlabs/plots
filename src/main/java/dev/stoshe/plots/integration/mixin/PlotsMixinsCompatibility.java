package dev.stoshe.plots.integration.mixin;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.flag.FlagRegistry;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.ConsoleColors;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native hook registry consumed by Plots mixins module.
 */
public final class PlotsMixinsCompatibility {
    public static final String REGISTRY_KEY = "plots.hook.registry";
    /** Shared TaleGuard bridge registry key (cross-classloader, via System properties). */
    public static final String TALEGUARD_REGISTRY_KEY = "taleguard.hook.registry";
    /** Identifier for the Plots adapter hook within the TaleGuard registry. */
    public static final String TALEGUARD_HOOK_KEY = "plots";
    public static final String PICKUP_HOOK = "plots.pickup.hook";
    public static final String HAMMER_HOOK = "plots.hammer.hook";
    public static final String HARVEST_HOOK = "plots.harvest.hook";
    public static final String PLACE_HOOK = "plots.place.hook";
    public static final String DEATH_HOOK = "plots.death.hook";
    public static final String DURABILITY_HOOK = "plots.durability.hook";
    public static final String USE_HOOK = "plots.use.hook";
    public static final String SEAT_HOOK = "plots.seat.hook";
    public static final String SPAWN_HOOK = "plots.spawn.hook";
    public static final String EXPLOSION_HOOK = "plots.explosion.hook";
    public static final String COMMAND_HOOK = "plots.command.hook";
    public static final String BUILDERTOOLS_HOOK = "plots.buildertools.hook";
    public static final String FIRE_HOOK = "plots.fire.hook";
    public static final String FLUID_FLOW_HOOK = "plots.fluid.flow.hook";

    private PlotsMixinsCompatibility() {
    }

    @SuppressWarnings("unchecked")
    public static void register(@Nonnull IPlotManager plotManager) {
        try {
            Map<String, Object> registry = (Map<String, Object>) System.getProperties().get(REGISTRY_KEY);
            if (registry == null) {
                registry = new ConcurrentHashMap<>();
                System.getProperties().put(REGISTRY_KEY, registry);
            }

            GenericCheckHook generic = new GenericCheckHook(plotManager);
            registry.put(PICKUP_HOOK, generic);
            registry.put(HAMMER_HOOK, generic);
            registry.put(HARVEST_HOOK, generic);
            registry.put(PLACE_HOOK, generic);
            registry.put(USE_HOOK, generic);
            registry.put(SEAT_HOOK, generic);
            registry.put(BUILDERTOOLS_HOOK, generic);
            registry.put(FIRE_HOOK, generic);
            registry.put(FLUID_FLOW_HOOK, new FluidFlowHook(plotManager));
            registry.put(EXPLOSION_HOOK, new ExplosionHook(plotManager));
            registry.put(COMMAND_HOOK, new CommandHook(plotManager));
            registry.put(SPAWN_HOOK, new SpawnHook(plotManager));
            registry.put(DEATH_HOOK, new DeathHook(plotManager));
            registry.put(DURABILITY_HOOK, new DurabilityHook(plotManager));

            ConsoleColors.info("Plots mixin hooks registered.");
        } catch (Exception e) {
            ConsoleColors.warning("Failed to register plots mixin hooks: " + e.getMessage());
        }
    }

    /**
     * Registers a single adapter hook into the shared TaleGuard bridge registry
     * ({@value #TALEGUARD_REGISTRY_KEY}). TaleGuard's mixins discover hooks by
     * reflection over the method names exposed by {@link PlotsProtectionHook},
     * so no compile-time dependency on TaleGuard is required.
     */
    @SuppressWarnings("unchecked")
    public static void registerTaleGuard(@Nonnull IPlotManager plotManager) {
        try {
            Map<String, Object> registry = (Map<String, Object>) System.getProperties().get(TALEGUARD_REGISTRY_KEY);
            if (registry == null) {
                registry = new ConcurrentHashMap<>();
                System.getProperties().put(TALEGUARD_REGISTRY_KEY, registry);
            }
            registry.put(TALEGUARD_HOOK_KEY, new PlotsProtectionHook(plotManager));
            ConsoleColors.info("Plots protection hook registered with TaleGuard bridge.");
        } catch (Exception e) {
            ConsoleColors.warning("Failed to register Plots hook with TaleGuard: " + e.getMessage());
        }
    }

    /**
     * Adapter exposing the method surface expected by TaleGuard's reflective
     * {@code HookRegistry}. Delegates to the existing, battle-tested per-action
     * hooks. Enforcement is scoped to plot worlds so the hook stays inert when
     * other TaleGuard consumers (e.g. skyblock) own the world.
     */
    public static final class PlotsProtectionHook {
        private final IPlotManager plotManager;
        private final GenericCheckHook generic;
        private final FluidFlowHook fluid;
        private final ExplosionHook explosion;
        private final CommandHook command;
        private final SpawnHook spawn;
        private final DeathHook death;
        private final DurabilityHook durability;

        PlotsProtectionHook(@Nonnull IPlotManager plotManager) {
            this.plotManager = plotManager;
            this.generic = new GenericCheckHook(plotManager);
            this.fluid = new FluidFlowHook(plotManager);
            this.explosion = new ExplosionHook(plotManager);
            this.command = new CommandHook(plotManager);
            this.spawn = new SpawnHook(plotManager);
            this.death = new DeathHook(plotManager);
            this.durability = new DurabilityHook(plotManager);
        }

        public int getPriority() {
            return 2;
        }

        private boolean isPlotWorld(String worldName) {
            if (worldName == null || worldName.isBlank()) {
                return true;
            }
            try {
                Plots plugin = Plots.getInstance();
                if (plugin != null && plugin.getWorldManager() != null) {
                    return plugin.getWorldManager().isPlotWorld(worldName);
                }
            } catch (Exception ignored) {
            }
            return true;
        }

        public boolean isAllowed(UUID playerUuid, String worldName, double x, double y, double z, String type) {
            if (type == null) {
                return true;
            }
            if (!isPlotWorld(worldName)) {
                return true;
            }
            int ix = (int) Math.floor(x);
            int iy = (int) Math.floor(y);
            int iz = (int) Math.floor(z);
            switch (type) {
                case "EXPLOSION":
                    return !explosion.shouldBlockExplosion(worldName, ix, iy, iz);
                case "MOB_SPAWN":
                    return !spawn.shouldBlockSpawn(worldName, ix, iy, iz);
                case "DEATH_DROP":
                    // allowed == false => keep inventory / block the drop
                    return !death.shouldKeepInventory(playerUuid, worldName, ix, iy, iz);
                case "DURABILITY":
                    // allowed == false => prevent durability loss
                    return !durability.shouldPreventDurabilityLoss(playerUuid, worldName, ix, iy, iz);
                default:
                    return generic.check(playerUuid, worldName, x, y, z, translateMode(type));
            }
        }

        public void notifyDenied(UUID playerUuid, String worldName, double x, double y, double z, String type) {
            if (type == null) {
                return;
            }
            switch (type) {
                case "EXPLOSION":
                case "MOB_SPAWN":
                case "DEATH_DROP":
                case "DURABILITY":
                    // Non player-facing world rules: no chat feedback.
                    return;
                default:
                    generic.notifyDenied(playerUuid, worldName, x, y, z, translateMode(type));
            }
        }

        /**
         * Bridges TaleGuard's interaction-type vocabulary to the action modes the
         * plot protection logic understands ({@link GenericCheckHook}).
         */
        private static String translateMode(String type) {
            if (type == null) {
                return "INTERACT";
            }
            switch (type) {
                case "CROP_HARVEST":
                    return "HARVEST";
                case "ITEM_PICKUP":
                    return "AUTO";
                case "ENTITY_INTERACT":
                    return "INTERACT";
                default:
                    return type; // HARVEST, BUILDER, HAMMER, FLUID, INTERACT, PLACE, FIRE, DROP, AUTO, MANUAL...
            }
        }

        public boolean isFluidFlowAllowed(String worldName, int fromX, int fromY, int fromZ, int toX, int toY,
                int toZ) {
            if (!isPlotWorld(worldName)) {
                return true;
            }
            return !fluid.shouldBlockFluidSpread(worldName, fromX, fromY, fromZ, toX, toY, toZ);
        }

        public boolean isCommandAllowed(UUID senderUuid, String rawCommand) {
            return !command.shouldBlockCommand(senderUuid, rawCommand);
        }

        public String getCommandDenialMessage() {
            return command.getDenialMessage();
        }
    }

    public static final class GenericCheckHook {
        private final IPlotManager plotManager;
        private final Map<UUID, Long> denyMessageCooldown = new ConcurrentHashMap<>();
        private static final long DENY_MESSAGE_COOLDOWN_MS = 900L;

        private GenericCheckHook(IPlotManager plotManager) {
            this.plotManager = plotManager;
        }

        public boolean check(UUID playerUuid, String worldName, double x, double y, double z, String mode) {
            if (playerUuid == null) {
                return true;
            }
            if (PermissionUtil.hasAdminPermission(playerUuid)) {
                return true;
            }
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef == null || playerRef.getWorldUuid() == null) {
                return false;
            }
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world == null) {
                return false;
            }
            String worldToUse = (worldName != null && !worldName.isBlank()) ? worldName : world.getName();
            int ix = (int) Math.floor(x);
            int iy = (int) Math.floor(y);
            int iz = (int) Math.floor(z);
            Plot plot = plotManager.getPlotAt(worldToUse, ix, iz);

            if ("AUTO".equalsIgnoreCase(mode)) {
                if (plot == null) {
                    return false;
                }
                if (!plot.getFlagValue(FlagRegistry.ITEM_PICKUP) && !plot.isOwnerOrMember(playerUuid)) {
                    return false;
                }
                return plot.hasPermission(playerUuid);
            }

            if ("MANUAL".equalsIgnoreCase(mode)) {
                if (plot == null) {
                    return false;
                }
                if (!plot.getFlagValue(FlagRegistry.ITEM_PICKUP_MANUAL) && !plot.isOwnerOrMember(playerUuid)) {
                    return false;
                }
                return plotManager.canModify(playerRef, world, ix, iy, iz, IPlotManager.ActionType.INTERACT);
            }

            if ("HAMMER".equalsIgnoreCase(mode) || "BUILDER".equalsIgnoreCase(mode)) {
                return plotManager.canUseBuilderTools(playerUuid, worldToUse, ix, iz);
            }
            if ("DROP".equalsIgnoreCase(mode)) {
                if (plot == null) {
                    return true;
                }
                if (!plotManager.canModify(playerRef, world, ix, iy, iz, IPlotManager.ActionType.INTERACT)) {
                    return false;
                }
                return plot.getFlagValue(FlagRegistry.ITEM_DROP) || plot.isOwnerOrMember(playerUuid);
            }
            if ("BREAK".equalsIgnoreCase(mode) || "HARVEST".equalsIgnoreCase(mode)) {
                return plotManager.canModify(playerRef, world, ix, iy, iz, IPlotManager.ActionType.BREAK);
            }
            if ("PLACE".equalsIgnoreCase(mode) || "FLUID".equalsIgnoreCase(mode) || "FIRE".equalsIgnoreCase(mode)) {
                return plotManager.canModify(playerRef, world, ix, iy, iz, IPlotManager.ActionType.PLACE);
            }
            return plotManager.canModify(playerRef, world, ix, iy, iz, IPlotManager.ActionType.INTERACT);
        }

        public void notifyDenied(UUID playerUuid, String worldName, double x, double y, double z, String mode) {
            if (playerUuid == null) {
                return;
            }
            long now = System.currentTimeMillis();
            Long last = denyMessageCooldown.get(playerUuid);
            if (last != null && (now - last) < DENY_MESSAGE_COOLDOWN_MS) {
                return;
            }
            denyMessageCooldown.put(playerUuid, now);

            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef == null) {
                return;
            }
            String resolvedWorld = worldName;
            if (resolvedWorld == null || resolvedWorld.isBlank()) {
                if (playerRef.getWorldUuid() != null) {
                    World world = Universe.get().getWorld(playerRef.getWorldUuid());
                    if (world != null) {
                        resolvedWorld = world.getName();
                    }
                }
            }
            if (resolvedWorld == null || resolvedWorld.isBlank()) {
                resolvedWorld = "";
            }

            int ix = (int) Math.floor(x);
            int iz = (int) Math.floor(z);
            Plot plot = plotManager.getPlotAt(resolvedWorld, ix, iz);
            String custom = plot != null ? plot.getFlagValue(FlagRegistry.DENY_MESSAGE) : "";
            String msg;
            if (custom != null && !custom.trim().isEmpty()) {
                msg = custom;
            } else {
                msg = Plots.getInstance().getTranslationManager().get(resolveTranslationKey(mode));
            }
            playerRef.sendMessage(ChatUtil.error(msg));
        }

        private String resolveTranslationKey(String mode) {
            if (mode == null) {
                return "protection.no_permission_interact";
            }
            if ("BREAK".equalsIgnoreCase(mode) || "HARVEST".equalsIgnoreCase(mode)) {
                return "protection.no_permission_break";
            }
            if ("PLACE".equalsIgnoreCase(mode)) {
                return "protection.no_permission_place";
            }
            if ("AUTO".equalsIgnoreCase(mode) || "MANUAL".equalsIgnoreCase(mode)) {
                return "protection.item_pickup_disabled";
            }
            if ("DROP".equalsIgnoreCase(mode)) {
                return "protection.item_drop_disabled";
            }
            if ("FLUID".equalsIgnoreCase(mode)) {
                return "protection.no_permission_place_liquid";
            }
            if ("HAMMER".equalsIgnoreCase(mode) || "BUILDER".equalsIgnoreCase(mode)) {
                return "protection.no_permission_buildertools";
            }
            return "protection.no_permission_interact";
        }
    }

    public static final class ExplosionHook {
        private final IPlotManager plotManager;

        private ExplosionHook(IPlotManager plotManager) {
            this.plotManager = plotManager;
        }

        public boolean shouldBlockExplosion(World world, int x, int y, int z) {
            Plot plot = plotManager.getPlotAt(world.getName(), x, z);
            return plot != null && !plot.getFlagValue(FlagRegistry.EXPLOSIONS);
        }

        public boolean shouldBlockExplosion(String worldName, int x, int y, int z) {
            if (worldName == null || worldName.isBlank()) {
                return false;
            }
            Plot plot = plotManager.getPlotAt(worldName, x, z);
            return plot != null && !plot.getFlagValue(FlagRegistry.EXPLOSIONS);
        }
    }

    public static final class FluidFlowHook {
        private final IPlotManager plotManager;

        private FluidFlowHook(IPlotManager plotManager) {
            this.plotManager = plotManager;
        }

        public boolean shouldBlockFluidSpread(String worldName, int fromX, int fromY, int fromZ, int toX, int toY,
                int toZ) {
            if (worldName == null || worldName.isBlank()) {
                return false;
            }
            if (fromX == toX && fromY == toY && fromZ == toZ) {
                return false;
            }

            Plot fromPlot = plotManager.getPlotAt(worldName, fromX, fromZ);
            Plot toPlot = plotManager.getPlotAt(worldName, toX, toZ);

            if (fromPlot == null && toPlot == null) {
                return false;
            }
            if (fromPlot == null || toPlot == null) {
                return true;
            }

            if (fromPlot.getGridX() == toPlot.getGridX() && fromPlot.getGridZ() == toPlot.getGridZ()) {
                return false;
            }

            return !(fromPlot.isMergedWith(toPlot.getGridX(), toPlot.getGridZ())
                    || toPlot.isMergedWith(fromPlot.getGridX(), fromPlot.getGridZ()));
        }
    }

    public static final class SpawnHook {
        private final IPlotManager plotManager;

        private SpawnHook(IPlotManager plotManager) {
            this.plotManager = plotManager;
        }

        public boolean shouldBlockSpawn(String worldName, int x, int y, int z) {
            if (worldName == null || worldName.isBlank()) {
                return false;
            }
            Plot plot = plotManager.getPlotAt(worldName, x, z);
            return plot != null && !plot.getFlagValue(FlagRegistry.MOB_SPAWNING);
        }
    }

    public static final class DeathHook {
        private final IPlotManager plotManager;

        private DeathHook(IPlotManager plotManager) {
            this.plotManager = plotManager;
        }

        public boolean shouldKeepInventory(UUID playerUuid, String worldName, int x, int y, int z) {
            if (playerUuid == null || worldName == null || worldName.isBlank()) {
                return false;
            }
            Plot plot = plotManager.getPlotAt(worldName, x, z);
            if (plot == null) {
                return false;
            }
            return plot.isOwnerOrMember(playerUuid) && plot.getFlagValue(FlagRegistry.KEEP_INVENTORY);
        }
    }

    public static final class DurabilityHook {
        private final IPlotManager plotManager;

        private DurabilityHook(IPlotManager plotManager) {
            this.plotManager = plotManager;
        }

        public boolean shouldPreventDurabilityLoss(UUID playerUuid, String worldName, int x, int y, int z) {
            if (playerUuid == null || worldName == null || worldName.isBlank()) {
                return false;
            }
            Plot plot = plotManager.getPlotAt(worldName, x, z);
            if (plot == null) {
                return false;
            }
            return plot.isOwnerOrMember(playerUuid) && plot.getFlagValue(FlagRegistry.INVINCIBLE_ITEMS);
        }
    }

    public static final class CommandHook {
        private final IPlotManager plotManager;

        private CommandHook(IPlotManager plotManager) {
            this.plotManager = plotManager;
        }

        public boolean shouldBlockCommand(CommandSender sender, String rawCommand) {
            if (sender == null || sender.hasPermission(IPlotManager.PERM_ADMIN)) {
                return false;
            }
            PlayerRef playerRef = Universe.get().getPlayer(sender.getUuid());
            if (playerRef == null || playerRef.getWorldUuid() == null) {
                return false;
            }
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world == null) {
                return false;
            }
            var transform = playerRef.getTransform();
            if (transform == null) {
                return false;
            }
            int x = (int) transform.getPosition().x;
            int z = (int) transform.getPosition().z;
            Plot plot = plotManager.getPlotAt(world.getName(), x, z);
            if (plot == null || plot.isOwnerOrMember(playerRef.getUuid())) {
                return false;
            }
            return matchesBlock(plot, rawCommand);
        }

        public boolean shouldBlockCommand(UUID senderUuid, String rawCommand) {
            if (senderUuid == null || PermissionUtil.hasAdminPermission(senderUuid)) {
                return false;
            }
            PlayerRef playerRef = Universe.get().getPlayer(senderUuid);
            if (playerRef == null || playerRef.getWorldUuid() == null) {
                return false;
            }
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world == null) {
                return false;
            }
            var transform = playerRef.getTransform();
            if (transform == null) {
                return false;
            }
            int x = (int) transform.getPosition().x;
            int z = (int) transform.getPosition().z;
            Plot plot = plotManager.getPlotAt(world.getName(), x, z);
            if (plot == null || plot.isOwnerOrMember(senderUuid)) {
                return false;
            }
            return matchesBlock(plot, rawCommand);
        }

        private boolean matchesBlock(Plot plot, String rawCommand) {
            String cmd = normalizeCommand(rawCommand);
            String allowedRaw = plot.getFlagValue(FlagRegistry.ALLOWED_COMMANDS);
            if (allowedRaw != null && !allowedRaw.trim().isEmpty()) {
                return Arrays.stream(allowedRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .noneMatch(allow -> allow.equalsIgnoreCase(cmd));
            }
            String blockedRaw = plot.getFlagValue(FlagRegistry.BLOCKED_COMMANDS);
            if (blockedRaw != null && !blockedRaw.trim().isEmpty()) {
                return Arrays.stream(blockedRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .anyMatch(block -> block.equalsIgnoreCase(cmd));
            }
            return false;
        }

        public String getDenialMessage() {
            return Plots.getInstance().getTranslationManager().get("general.no_permission");
        }

        private String normalizeCommand(String raw) {
            String s = raw == null ? "" : raw.trim();
            if (s.startsWith("/")) {
                s = s.substring(1);
            }
            int space = s.indexOf(' ');
            if (space > 0) {
                s = s.substring(0, space);
            }
            return s.toLowerCase();
        }
    }
}
