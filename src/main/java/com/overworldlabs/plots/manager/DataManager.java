package com.overworldlabs.plots.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.overworldlabs.plots.model.Plot;

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
            System.out.println("[Plots] Saved " + plots.size() + " plots to disk");
        } catch (IOException e) {
            System.err.println("[Plots] Failed to save plots: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all plots from disk
     */
    public void loadPlots() {
        if (!dataFile.exists()) {
            System.out.println("[Plots] No plots file found, starting fresh");
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<HashMap<String, Plot>>() {
            }.getType();
            Map<String, Plot> loadedPlots = gson.fromJson(reader, type);

            if (loadedPlots != null) {
                plotManager.loadPlots(loadedPlots);
                System.out.println("[Plots] Loaded " + loadedPlots.size() + " plots from disk");
            } else {
                System.out.println("[Plots] Plots file was empty or invalid");
            }
        } catch (IOException e) {
            System.err.println("[Plots] Failed to load plots: " + e.getMessage());
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






