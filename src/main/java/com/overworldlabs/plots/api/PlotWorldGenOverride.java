package com.overworldlabs.plots.api;

import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.core.universe.world.worldgen.WorldGenLoadException;
import com.overworldlabs.plots.worldgen.PlotGenerationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * World generation override hook for external plugins.
 */
@FunctionalInterface
public interface PlotWorldGenOverride {

    /**
     * Return a custom generator to replace the default Plots generator.
     * Return null to keep the default generator behavior.
     */
    @Nullable
    IWorldGen createGenerator(@Nonnull PlotGenerationContext context, int tintArgb) throws WorldGenLoadException;
}
