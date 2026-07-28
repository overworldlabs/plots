package dev.stoshe.plots.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.stoshe.plots.api.IPlotRepository;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ConsoleColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * JSON-based implementation of IPlotRepository
 */
public class JsonPlotRepository implements IPlotRepository {
    private final File dataFile;
    private final Gson gson;
    private final String legacyWorldName;
    private final Map<String, Plot> plots = new ConcurrentHashMap<>();

    public JsonPlotRepository(@Nonnull File dataFile, @Nonnull String legacyWorldName) {
        this.dataFile = dataFile;
        this.legacyWorldName = legacyWorldName;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Builds the cache key for a plot. The world segment is lower-cased so lookups
     * are case-insensitive, matching the {@code equalsIgnoreCase} owner/world
     * filters and avoiding duplicate records when a world name's case differs
     * between storage and runtime.
     *
     * @param world the plot world (nullable; treated as empty)
     * @param gridX the plot grid X coordinate
     * @param gridZ the plot grid Z coordinate
     * @return the cache key
     */
    private static String key(String world, int gridX, int gridZ) {
        String normalizedWorld = world == null ? "" : world.toLowerCase();
        return normalizedWorld + ";" + gridX + ";" + gridZ;
    }

    @Override
    @Nullable
    public Plot getPlot(@Nonnull String world, int gridX, int gridZ) {
        return plots.get(key(world, gridX, gridZ));
    }

    @Override
    @Nonnull
    public List<Plot> getPlotsByOwner(@Nonnull UUID ownerUuid) {
        return plots.values().stream()
                .filter(plot -> ownerUuid.equals(plot.getOwner()))
                .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public List<Plot> getPlotsByOwnerInWorld(@Nonnull UUID ownerUuid, @Nonnull String world) {
        return plots.values().stream()
                .filter(plot -> ownerUuid.equals(plot.getOwner()) && world.equalsIgnoreCase(plot.getWorld()))
                .collect(Collectors.toList());
    }

    @Override
    public int countByOwnerInWorld(@Nonnull UUID ownerUuid, @Nonnull String world) {
        return (int) plots.values().stream()
                .filter(plot -> ownerUuid.equals(plot.getOwner()) && world.equalsIgnoreCase(plot.getWorld()))
                .count();
    }

    @Override
    @Nonnull
    public List<Plot> getPlotsInWorld(@Nonnull String world) {
        return plots.values().stream()
                .filter(plot -> world.equalsIgnoreCase(plot.getWorld()))
                .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public Collection<Plot> getAllPlots() {
        return Collections.unmodifiableCollection(plots.values());
    }

    @Override
    public void savePlot(@Nonnull Plot plot) {
        if (plot.getWorld() == null) {
            plot.setWorld(legacyWorldName);
        }
        plots.put(key(plot.getWorld(), plot.getGridX(), plot.getGridZ()), plot);
    }

    @Override
    public void deletePlot(@Nonnull String world, int gridX, int gridZ) {
        plots.remove(key(world, gridX, gridZ));
    }

    @Override
    public void loadAll() {
        if (!dataFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, Plot>>() {
            }.getType();
            Map<String, Plot> loadedPlots = gson.fromJson(reader, type);
            if (loadedPlots != null) {
                plots.clear();
                // Re-key by world;x;z. Legacy entries lacking a world are assigned the
                // legacy/default world so old single-world data upgrades transparently.
                for (Plot plot : loadedPlots.values()) {
                    if (plot == null) {
                        continue;
                    }
                    if (plot.getWorld() == null || plot.getWorld().isBlank()) {
                        plot.setWorld(legacyWorldName);
                    }
                    plots.put(key(plot.getWorld(), plot.getGridX(), plot.getGridZ()), plot);
                }
                ConsoleColors.info("Loaded " + plots.size() + " plots from " + dataFile.getName());
            }
        } catch (IOException e) {
            ConsoleColors.error("Failed to load plots: " + e.getMessage());
        }
    }

    @Override
    public void saveAll() {
        try {
            if (!dataFile.exists()) {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            }

            try (Writer writer = new FileWriter(dataFile)) {
                gson.toJson(plots, writer);
            }
        } catch (IOException e) {
            ConsoleColors.error("Failed to save plots: " + e.getMessage());
        }
    }
}
