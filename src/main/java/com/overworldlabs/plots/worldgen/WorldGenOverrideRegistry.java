package com.overworldlabs.plots.worldgen;

import com.overworldlabs.plots.api.PlotWorldGenOverride;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

public final class WorldGenOverrideRegistry {
    private static final AtomicReference<PlotWorldGenOverride> OVERRIDE = new AtomicReference<>();

    private WorldGenOverrideRegistry() {
    }

    public static void setOverride(@Nullable PlotWorldGenOverride worldGenOverride) {
        OVERRIDE.set(worldGenOverride);
    }

    @Nullable
    public static PlotWorldGenOverride getOverride() {
        return OVERRIDE.get();
    }

    public static void clear() {
        OVERRIDE.set(null);
    }
}
