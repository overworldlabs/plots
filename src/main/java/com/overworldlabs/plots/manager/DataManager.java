package com.overworldlabs.plots.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ConsoleColors;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages saving and loading plot data
 */
public class DataManager {
    private final File dataFile;
    private final Gson gson;
    private final PlotManager plotManager;

    public DataManager(@Nonnull File dataDirectory, @Nonnull PlotManager plotManager) {
        this.dataFile = new File(dataDirectory, "plots.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.plotManager = plotManager;

        // Create data directory if it doesn't exist
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
    }

    /**
     * Save all plots to disk
     */
    public void savePlots() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            Map<String, Plot> plots = plotManager.getPlotsMap();
            gson.toJson(plots, writer);
            ConsoleColors.success("Saved " + plots.size() + " plots to disk");
        } catch (IOException e) {
            ConsoleColors.error("Failed to save plots: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all plots from disk
     */
    public void loadPlots() {
        if (!dataFile.exists()) {
            ConsoleColors.info("No plots file found, starting fresh");
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<HashMap<String, Plot>>() {
            }.getType();
            Map<String, Plot> loadedPlots = gson.fromJson(reader, type);

            if (loadedPlots != null) {
                plotManager.loadPlots(loadedPlots);
                ConsoleColors.success("Loaded " + loadedPlots.size() + " plots from disk");
            } else {
                ConsoleColors.error("Plots file was empty or invalid");
            }
        } catch (IOException e) {
            ConsoleColors.error("Failed to load plots: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the data file
     */
    public File getDataFile() {
        return dataFile;
    }
}
