package com.overworldlabs.plots.integration.holograms;

import dev.ehko.hylograms.api.HologramsAPI;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.manager.PlotManager;
import com.overworldlabs.plots.manager.TranslationManager;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.model.PlotConfig;
import com.overworldlabs.plots.util.ConsoleColors;

import javax.annotation.Nonnull;

/**
 * Manages holograms for plots using Hylograms API
 */
public class HologramManager {
    private final PlotManager plotManager;

    public HologramManager(@Nonnull PlotManager plotManager) {
        this.plotManager = plotManager;
    }

    /**
     * Updates or creates a hologram for a plot
     */
    public void updateHologram(@Nonnull Plot plot, @Nonnull Store<EntityStore> store) {
        PlotConfig config = plotManager.getConfig();
        if (!config.isHologramEnabled()) {
            return;
        }

        String id = "plot_" + plot.getGridX() + "_" + plot.getGridZ();
        TranslationManager tm = Plots.getInstance().getTranslationManager();

        // Calculate position at the corner of the plot
        int worldX = config.gridToWorldX(plot.getGridX());
        int worldZ = config.gridToWorldZ(plot.getGridZ());
        double y = 64.0 + config.getHologramHeightOffset();

        String title = tm.get("hologram.title", "name", plot.getName());
        String subtitle = tm.get("hologram.subtitle", "owner", plot.getOwnerName());

        try {
            if (HologramsAPI.exists(id)) {
                // Delete existing hologram and recreate to apply color
                HologramsAPI.delete(id, store);
            }

            // Create hologram with current data
            HologramsAPI.create(id, store)
                    .at(worldX + 0.5, y, worldZ + 0.5)
                    .inWorld(config.getPlotWorldName())
                    .color(config.getHologramTitleColor())
                    .addLine(title != null ? title : "Plot")
                    .addLine(subtitle != null ? subtitle : "Owner")
                    .spawn();
        } catch (Exception e) {
            ConsoleColors.error("[Plots] Failed to update hologram for plot at " + plot.getGridX() + ","
                    + plot.getGridZ() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes a hologram for a plot
     */
    public void removeHologram(@Nonnull Plot plot, @Nonnull Store<EntityStore> store) {
        String id = "plot_" + plot.getGridX() + "_" + plot.getGridZ();
        try {
            if (HologramsAPI.exists(id)) {
                HologramsAPI.delete(id, store);
            }
        } catch (Exception e) {
            ConsoleColors.error("[Plots] Failed to remove hologram for plot at " + plot.getGridX() + ","
                    + plot.getGridZ() + ": " + e.getMessage());
        }
    }

    /**
     * Spawns holograms for all managed plots
     */
    public void spawnAllHolograms(@Nonnull Store<EntityStore> store) {
        for (Plot plot : plotManager.getAllPlots()) {
            if (plot != null) {
                updateHologram(plot, store);
            }
        }
    }

    /**
     * Spawns holograms for all plots using an online player's store
     * Note: Currently just logs a message - holograms will be created when plots
     * are claimed/renamed
     */
    public void spawnAllHologramsForOnlinePlayers() {
        PlotConfig config = plotManager.getConfig();
        if (!config.isHologramEnabled()) {
            return;
        }

        System.out.println(
                "[Plots] Hologram system is enabled. Holograms will appear when plots are claimed or renamed.");
    }
}
