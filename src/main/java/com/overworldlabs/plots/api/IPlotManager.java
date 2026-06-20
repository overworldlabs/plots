package com.overworldlabs.plots.api;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;

import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.config.PlotConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandSender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Interface for managing plots.
 */
public interface IPlotManager {
    String PERM_BASE = "plots";
    String PERM_PLOT = "plots";
    String PERM_ADMIN = "plots.*";
    String PERM_CLAIM = "plots.claim";
    String PERM_DELETE = "plots.delete";
    String PERM_DELETE_ANY = "plots.delete.*";
    String PERM_SPAWN = "plots.spawn";
    String PERM_LIST = "plots.list";
    String PERM_INFO = "plots.info";
    String PERM_RENAME = "plots.rename";
    String PERM_TRUST = "plots.trust";
    String PERM_TRANSFER = "plots.transfer";
    String PERM_FLAG = "plots.flag";
    String PERM_MERGE = "plots.merge";
    String PERM_UNMERGE = "plots.unmerge";

    enum ActionType {
        BREAK,
        PLACE,
        INTERACT,
        CONTAINER_ACCESS,
        PVP,
        MOB_DAMAGE
    }

    PlotConfig getConfig();

    boolean claimPlot(@Nonnull CommandSender sender, @Nonnull PlayerRef playerRef, int gridX, int gridZ);

    boolean unclaimPlot(int gridX, int gridZ);

    boolean renamePlot(int gridX, int gridZ, @Nonnull String name);

    @Nullable
    Plot getPlot(int gridX, int gridZ);

    @Nullable
    Plot getPlotByGrid(int gridX, int gridZ);

    @Nullable
    Plot getPlotAt(String worldName, int worldX, int worldZ);

    boolean isInPlot(@Nonnull String worldName, int worldX, int worldZ);

    List<Plot> getPlayerPlots(@Nonnull UUID playerUuid);

    boolean canModify(@Nonnull PlayerRef player, @Nonnull World world, int worldX, int worldY, int worldZ,
            @Nonnull ActionType action);

    boolean canUseBuilderTools(@Nonnull UUID playerUuid, @Nonnull String worldName, int worldX, int worldZ);

    Collection<Plot> getAllPlots();

    void savePlots();

    int[] findNextFreePlot();

    void teleportPlayerToPlot(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            Plot plot);

    void teleportPlayerToWarp(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            Plot.PlotWarp warp);

    int getMaxWarpsPerPlot();
}
