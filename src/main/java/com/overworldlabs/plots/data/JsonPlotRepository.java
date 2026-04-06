package com.overworldlabs.plots.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.overworldlabs.plots.api.IPlotRepository;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ConsoleColors;

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
    private final Map<String, Plot> plots = new ConcurrentHashMap<>();

    public JsonPlotRepository(@Nonnull File dataFile) {
        this.dataFile = dataFile;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    @Override
    @Nullable
    public Plot getPlot(int gridX, int gridZ) {
        return plots.get(gridX + ";" + gridZ);
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
    public Collection<Plot> getAllPlots() {
        return Collections.unmodifiableCollection(plots.values());
    }

    @Override
    public void savePlot(@Nonnull Plot plot) {
        plots.put(plot.getGridX() + ";" + plot.getGridZ(), plot);
    }

    @Override
    public void deletePlot(int gridX, int gridZ) {
        plots.remove(gridX + ";" + gridZ);
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
                plots.putAll(loadedPlots);
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
