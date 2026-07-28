package dev.stoshe.plots.manager;

import com.hypixel.hytale.server.core.universe.world.World;
import dev.stoshe.plots.model.Plot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates merge/unmerge domain rules and merge pipelines.
 * <p>
 * PlotManager keeps orchestration/API concerns while this service owns the
 * adjacency, ownership and merge-link invariants.
 */
final class PlotMergeService {
    private final PlotManager manager;

    PlotMergeService(@Nonnull PlotManager manager) {
        this.manager = manager;
    }

    /**
     * Checks whether two plots belong to the same owner and are currently linked.
     */
    boolean isMergedPairWithSameOwner(@Nullable Plot a, @Nullable Plot b) {
        if (a == null || b == null) {
            return false;
        }
        if (!a.getOwner().equals(b.getOwner())) {
            return false;
        }
        return arePlotsMerged(a, b);
    }

    /**
     * Creates a bidirectional merge link when plots are adjacent and share owner.
     */
    boolean mergePlots(@Nonnull Plot a, @Nonnull Plot b) {
        if (!a.getOwner().equals(b.getOwner())) {
            return false;
        }
        if (!areAdjacent(a, b)) {
            return false;
        }
        a.addMergedPlot(b.getGridX(), b.getGridZ());
        b.addMergedPlot(a.getGridX(), a.getGridZ());
        return true;
    }

    /**
     * Removes a bidirectional merge link when plots are adjacent and linked.
     */
    boolean unmergePlots(@Nonnull Plot a, @Nonnull Plot b) {
        if (!areAdjacent(a, b)) {
            return false;
        }
        if (!arePlotsMerged(a, b)) {
            return false;
        }
        a.removeMergedPlot(b.getGridX(), b.getGridZ());
        b.removeMergedPlot(a.getGridX(), a.getGridZ());
        return true;
    }

    /**
     * Returns true only when both sides declare the merge link.
     */
    boolean arePlotsMerged(@Nonnull Plot a, @Nonnull Plot b) {
        return a.isMergedWith(b.getGridX(), b.getGridZ()) && b.isMergedWith(a.getGridX(), a.getGridZ());
    }

    /**
     * Applies merge link and optional terrain update policy in one operation.
     */
    boolean mergePlotsWithRoadPolicy(@Nonnull World world, @Nonnull Plot a, @Nonnull Plot b, boolean removeRoads) {
        if (!mergePlots(a, b)) {
            return false;
        }
        if (removeRoads) {
            manager.refreshBoundarySurface(world, a, b, true);
        }
        return true;
    }

    /**
     * Applies unmerge link and optional terrain update policy in one operation.
     */
    boolean unmergePlotsWithRoadPolicy(@Nonnull World world, @Nonnull Plot a, @Nonnull Plot b, boolean createRoad) {
        if (!unmergePlots(a, b)) {
            return false;
        }
        if (createRoad) {
            manager.refreshBoundarySurface(world, a, b, false);
        }
        return true;
    }

    /**
     * Executes merge operations for a list of adjacency pairs.
     */
    int applyMergePipeline(@Nonnull World world, @Nonnull List<Plot[]> pairs, boolean removeRoads) {
        int changed = 0;
        for (Plot[] pair : pairs) {
            if (pair == null || pair.length < 2 || pair[0] == null || pair[1] == null) {
                continue;
            }
            if (mergePlotsWithRoadPolicy(world, pair[0], pair[1], removeRoads)) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * Executes unmerge operations for a list of adjacency pairs.
     */
    int applyUnmergePipeline(@Nonnull World world, @Nonnull List<Plot[]> pairs, boolean createRoad) {
        int changed = 0;
        for (Plot[] pair : pairs) {
            if (pair == null || pair.length < 2 || pair[0] == null || pair[1] == null) {
                continue;
            }
            if (unmergePlotsWithRoadPolicy(world, pair[0], pair[1], createRoad)) {
                changed++;
            }
        }
        return changed;
    }

    @Nonnull
    /**
     * Reads merged-neighbor coordinates from plot state and resolves valid neighbors.
     */
    List<Plot> getMergedNeighbors(@Nonnull Plot plot) {
        List<Plot> neighbors = new ArrayList<>();
        for (String key : plot.getMergedPlots()) {
            String[] parts = key.split(",");
            if (parts.length != 2) {
                continue;
            }
            try {
                int gx = Integer.parseInt(parts[0].trim());
                int gz = Integer.parseInt(parts[1].trim());
                Plot neighbor = manager.getPlotByGrid(plot.getWorld(), gx, gz);
                if (neighbor != null && arePlotsMerged(plot, neighbor)) {
                    neighbors.add(neighbor);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return neighbors;
    }

    private boolean areAdjacent(@Nonnull Plot a, @Nonnull Plot b) {
        int dx = Math.abs(a.getGridX() - b.getGridX());
        int dz = Math.abs(a.getGridZ() - b.getGridZ());
        return dx + dz == 1;
    }
}
