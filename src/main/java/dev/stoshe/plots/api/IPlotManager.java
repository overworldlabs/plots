package dev.stoshe.plots.api;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;

import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.config.PlotConfig;
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
    String PERM_AUTO = "plots.auto";
    String PERM_DELETE = "plots.delete";
    String PERM_DELETE_ANY = "plots.delete.*";
    String PERM_SPAWN = "plots.spawn";
    String PERM_LIST = "plots.list";
    String PERM_INFO = "plots.info";
    String PERM_RENAME = "plots.rename";
    String PERM_TRUST = "plots.trust";
    String PERM_UNTRUST = "plots.untrust";
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

    /**
     * Claims the plot at the given world + grid coordinates for the sender,
     * enforcing the per-world and global plot limits.
     *
     * @param sender    the command sender requesting the claim (used for limits)
     * @param playerRef the player who will own the plot
     * @param world     the plot world to claim in
     * @param gridX     the plot grid X coordinate
     * @param gridZ     the plot grid Z coordinate
     * @return {@code true} if the plot was claimed, {@code false} if it is taken
     *         or the player is at their limit
     */
    boolean claimPlot(@Nonnull CommandSender sender, @Nonnull PlayerRef playerRef, @Nonnull String world,
            int gridX, int gridZ);

    /**
     * Whether the player may claim one more plot in {@code world}, honouring the
     * per-world budget, the global shared budget and the absolute hard cap.
     * <p>
     * Intended as a pre-check (e.g. before charging economy) so a claim that
     * would be rejected by {@link #claimPlot} never has side effects.
     * </p>
     *
     * @param sender the command sender (used to read permissions / admin bypass)
     * @param owner  the prospective owner's UUID
     * @param world  the plot world to test
     * @return {@code true} when at least one more claim is allowed
     */
    boolean canClaimIn(@Nonnull CommandSender sender, @Nonnull UUID owner, @Nonnull String world);

    /**
     * Removes the claim on the plot at the given world + grid coordinates.
     *
     * @param world the plot world
     * @param gridX the plot grid X coordinate
     * @param gridZ the plot grid Z coordinate
     * @return {@code true} if a plot was removed, {@code false} if none existed
     */
    boolean unclaimPlot(@Nonnull String world, int gridX, int gridZ);

    /**
     * Renames the plot at the given world + grid coordinates.
     *
     * @param world the plot world
     * @param gridX the plot grid X coordinate
     * @param gridZ the plot grid Z coordinate
     * @param name  the new plot name
     * @return {@code true} if the plot existed and was renamed
     */
    boolean renamePlot(@Nonnull String world, int gridX, int gridZ, @Nonnull String name);

    /**
     * Returns the plot stored at the given world + grid coordinates, if any.
     *
     * @param world the plot world
     * @param gridX the plot grid X coordinate
     * @param gridZ the plot grid Z coordinate
     * @return the plot, or {@code null} when the cell is unclaimed
     */
    @Nullable
    Plot getPlot(@Nonnull String world, int gridX, int gridZ);

    /**
     * Alias of {@link #getPlot(String, int, int)} for call sites that work in
     * grid space.
     *
     * @param world the plot world
     * @param gridX the plot grid X coordinate
     * @param gridZ the plot grid Z coordinate
     * @return the plot, or {@code null} when the cell is unclaimed
     */
    @Nullable
    Plot getPlotByGrid(@Nonnull String world, int gridX, int gridZ);

    /**
     * Resolves the canonical plot covering a world-space position, following
     * merges (roads/intersections inside a merged plot resolve to the owner).
     *
     * @param worldName the world name
     * @param worldX    the world-space X coordinate
     * @param worldZ    the world-space Z coordinate
     * @return the owning plot, or {@code null} when the position is unclaimed
     */
    @Nullable
    Plot getPlotAt(String worldName, int worldX, int worldZ);

    boolean isInPlot(@Nonnull String worldName, int worldX, int worldZ);

    List<Plot> getPlayerPlots(@Nonnull UUID playerUuid);

    boolean canModify(@Nonnull PlayerRef player, @Nonnull World world, int worldX, int worldY, int worldZ,
            @Nonnull ActionType action);

    boolean canUseBuilderTools(@Nonnull UUID playerUuid, @Nonnull String worldName, int worldX, int worldZ);

    Collection<Plot> getAllPlots();

    void savePlots();

    /**
     * Finds the nearest free plot cell in {@code world} by spiralling out from
     * the origin.
     *
     * @param world the plot world to search
     * @return the {@code [gridX, gridZ]} of a free cell, or {@code null} if none
     *         was found within the search bound
     */
    int[] findNextFreePlot(@Nonnull String world);

    /**
     * Teleports a player to a plot's spawn (or computed centre).
     *
     * @param store     the entity store
     * @param playerRef the player entity reference
     * @param plot      the destination plot
     */
    void teleportPlayerToPlot(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            Plot plot);

    /**
     * Teleports a player to a named warp inside a plot world.
     *
     * @param store     the entity store
     * @param playerRef the player entity reference
     * @param world     the world the warp lives in; falls back to the default
     *                  plot world when {@code null} or blank
     * @param warp      the destination warp
     */
    void teleportPlayerToWarp(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            @Nullable String world,
            Plot.PlotWarp warp);

    /**
     * Teleports a player to a plot world's spawn point.
     *
     * @param store     the entity store
     * @param playerRef the player entity reference
     * @param world     the destination plot world name
     */
    void teleportPlayerToWorldSpawn(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            @Nonnull String world);

    int getMaxWarpsPerPlot();
}
