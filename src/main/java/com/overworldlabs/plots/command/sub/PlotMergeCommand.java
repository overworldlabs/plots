package com.overworldlabs.plots.command.sub;

import com.overworldlabs.plots.util.CommandSenderIdentity;
import com.overworldlabs.plots.util.PlayerIdentity;
import com.hypixel.hytale.server.core.command.system.CommandUtil;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.IPlotManager;
import com.overworldlabs.plots.command.CommandArgs;
import com.overworldlabs.plots.command.feedback.CommandFeedbackService;
import com.overworldlabs.plots.integration.economy.PlotEconomyService;
import com.overworldlabs.plots.integration.holograms.HologramManager;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ChatUtil;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PlotMergeCommand extends CommandBase {
    private final IPlotManager plotManager;
    private final RequiredArg<String> directionArg;
    private final OptionalArg<String> removeRoadsArg;

    public PlotMergeCommand(@Nonnull IPlotManager plotManager) {
        super("merge", "Merge adjacent plots owned by you");
        addAliases("link");
        setAllowsExtraArguments(true);
        this.plotManager = plotManager;
        this.directionArg = CommandArgs.required(this, "direction", "north|south|east|west|all", ArgTypes.STRING);
        this.removeRoadsArg = CommandArgs.optional(this, "removeRoads", "true|false", ArgTypes.STRING);
        requirePermission(IPlotManager.PERM_MERGE);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        TranslationManager tm = Plots.getInstance().getTranslationManager();
        if (!PermissionUtil.hasAdminPermission(context.sender())) {
            CommandUtil.requirePermission(context.sender(),
                    IPlotManager.PERM_MERGE);
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

        String directionRaw = directionArg.get(context);
        if (directionRaw == null || directionRaw.trim().isEmpty()) {
            CommandFeedbackService.sendUsage(context.sender(), tm, "merge.usage");
            return;
        }

        String direction = directionRaw.trim().toLowerCase(Locale.ROOT);
        boolean isAdmin = PermissionUtil.hasAdminPermission(context.sender());
        String removeRoadsRaw = removeRoadsArg.get(context);
        final Boolean removeRoadsParsed = parseOptionalBoolean(removeRoadsRaw);
        if (removeRoadsRaw != null && removeRoadsParsed == null) {
            CommandFeedbackService.sendInvalidArgument(context.sender(), tm, "/plot merge " + direction + " " + removeRoadsRaw,
                    "merge.usage");
            return;
        }
        final boolean removeRoads = removeRoadsParsed != null ? removeRoadsParsed : true;

        world.execute(() -> {
            Store<EntityStore> store = ref.getStore();
            PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
            if (player == null) {
                return;
            }

            Vector3d pos = player.getTransform().getPosition();
            String worldName = world.getName();
            Plot current = plotManager.getPlotAt(worldName, (int) pos.x, (int) pos.z);
            if (current == null) {
                player.sendMessage(ChatUtil.error(tm.get("management.not_found")));
                return;
            }
            if (!current.getOwner().equals(PlayerIdentity.uuid(player)) && !isAdmin) {
                player.sendMessage(ChatUtil.error(tm.get("general.not_owner")));
                return;
            }

            PlotManager pm = (PlotManager) plotManager;

            if ("all".equals(direction)) {
                List<Plot[]> mergeCandidates = collectMergeCandidates(pm, current, Direction.ALL);
                if (mergeCandidates.isEmpty()) {
                    player.sendMessage(ChatUtil.info(tm.get("merge.none")));
                    return;
                }

                double mergeCost = this.plotManager.getConfig().getEconomyCostMerge() * mergeCandidates.size();
                boolean economyBypass = PermissionUtil.hasEconomyBypass(context.sender());
                if (!economyBypass && !chargeIfNeeded(player, mergeCost, tm.get("economy.reason.merge"), tm)) {
                    return;
                }

                int merged = pm.applyMergePipeline(world, mergeCandidates, removeRoads);
                if (merged > 0) {
                    HologramManager holograms = Plots.getInstance().getHologramManager();
                    var radar = Plots.getInstance().getRadarManager();
                    List<Plot> affected = new ArrayList<>();
                    for (Plot[] pair : mergeCandidates) {
                        affected.add(pair[0]);
                        affected.add(pair[1]);
                        radar.updatePlotMarker(pair[0]);
                        radar.updatePlotMarker(pair[1]);
                    }
                    holograms.refreshMergedHolograms(affected, store);
                    radar.refreshAllPlotMarkers();
                }

                plotManager.savePlots();
                player.sendMessage(ChatUtil.success(tm.get("merge.merged_count", "count", String.valueOf(merged))));
                return;
            }

            int dx = 0;
            int dz = 0;
            switch (direction) {
                case "north":
                    dz = -1;
                    break;
                case "south":
                    dz = 1;
                    break;
                case "east":
                    dx = 1;
                    break;
                case "west":
                    dx = -1;
                    break;
                default:
                    CommandFeedbackService.sendInvalidSubcommand(player, tm, "/plot merge " + direction, "merge.usage");
                    return;
            }

            Plot neighbor = plotManager.getPlotByGrid(current.getGridX() + dx, current.getGridZ() + dz);
            if (neighbor == null) {
                player.sendMessage(ChatUtil.error(tm.get("merge.no_adjacent_claimed")));
                return;
            }
            if (!neighbor.getOwner().equals(current.getOwner())) {
                player.sendMessage(ChatUtil.error(tm.get("merge.owner_mismatch")));
                return;
            }

            if (pm.arePlotsMerged(current, neighbor)) {
                player.sendMessage(ChatUtil.info(tm.get("merge.already_merged")));
                return;
            }

            boolean economyBypass = PermissionUtil.hasEconomyBypass(context.sender());
            if (!economyBypass
                    && !chargeIfNeeded(player, this.plotManager.getConfig().getEconomyCostMerge(),
                            tm.get("economy.reason.merge"), tm)) {
                return;
            }

            if (!pm.mergePlotsWithRoadPolicy(world, current, neighbor, removeRoads)) {
                player.sendMessage(ChatUtil.error(tm.get("merge.failed")));
                return;
            }

            Plots.getInstance().getRadarManager().updatePlotMarker(current);
            Plots.getInstance().getRadarManager().updatePlotMarker(neighbor);
            Plots.getInstance().getRadarManager().refreshAllPlotMarkers();
            Plots.getInstance().getHologramManager().refreshMergedHolograms(List.of(current, neighbor), store);
            plotManager.savePlots();
            player.sendMessage(ChatUtil.success(tm.get("merge.success")));
        });
    }

    private Boolean parseOptionalBoolean(String value) {
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return null;
    }

    private enum Direction {
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0), ALL(0, 0);
        final int dx;
        final int dz;

        Direction(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }
    }

    private List<Plot[]> collectMergeCandidates(PlotManager pm, Plot start, Direction direction) {
        List<Plot[]> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> seenEdges = new HashSet<>();
        ArrayList<Plot> frontier = new ArrayList<>();
        frontier.add(start);
        String owner = start.getOwner().toString();

        for (int i = 0; i < frontier.size(); i++) {
            Plot current = frontier.get(i);
            String currentKey = current.getGridX() + "," + current.getGridZ();
            if (!visited.add(currentKey)) {
                continue;
            }

            int[][] dirs = direction == Direction.ALL
                    ? new int[][] { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } }
                    : new int[][] { { direction.dx, direction.dz } };

            for (int[] d : dirs) {
                Plot neighbor = plotManager.getPlotByGrid(current.getGridX() + d[0], current.getGridZ() + d[1]);
                if (neighbor == null || !owner.equals(neighbor.getOwner().toString())) {
                    continue;
                }

                String edge = edgeKey(current, neighbor);
                if (!pm.arePlotsMerged(current, neighbor) && seenEdges.add(edge)) {
                    result.add(new Plot[] { current, neighbor });
                }

                String neighborKey = neighbor.getGridX() + "," + neighbor.getGridZ();
                if (!visited.contains(neighborKey)) {
                    frontier.add(neighbor);
                }
            }
        }
        return result;
    }

    private String edgeKey(Plot a, Plot b) {
        String ka = a.getGridX() + "," + a.getGridZ();
        String kb = b.getGridX() + "," + b.getGridZ();
        return ka.compareTo(kb) <= 0 ? ka + "|" + kb : kb + "|" + ka;
    }

    private boolean chargeIfNeeded(PlayerRef playerRef, double amount, String reason, TranslationManager tm) {
        if (amount <= 0.0) {
            return true;
        }

        PlotEconomyService economy = Plots.getInstance().getEconomyService();
        if (economy == null || !economy.isEnabled()) {
            playerRef.sendMessage(ChatUtil.error(tm.get("economy.not_available")));
            return false;
        }

        if (!economy.has(PlayerIdentity.uuid(playerRef), amount)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("economy.insufficient_funds", "amount", economy.format(amount))));
            return false;
        }

        if (!economy.tryWithdraw(PlayerIdentity.uuid(playerRef), amount, reason)) {
            playerRef.sendMessage(ChatUtil.error(tm.get("economy.withdraw_failed")));
            return false;
        }

        playerRef.sendMessage(ChatUtil.info(tm.get("economy.charged", "amount", economy.format(amount))));
        return true;
    }
}
