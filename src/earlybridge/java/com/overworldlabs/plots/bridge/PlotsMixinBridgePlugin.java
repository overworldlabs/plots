package com.overworldlabs.plots.bridge;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Bridge plugin entrypoint for runtime diagnostics/state flags.
 */
public final class PlotsMixinBridgePlugin extends JavaPlugin {
    private static final String ACTIVE_PROPERTY = "plots.mixin.bridge.active";
    private static final String BOOTSTRAP_READY_PROPERTY = "plots.mixin.bridge.bootstrap.ready";

    public PlotsMixinBridgePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        System.setProperty(ACTIVE_PROPERTY, "true");
        System.setProperty(BOOTSTRAP_READY_PROPERTY, "true");
        getLogger().at(Level.INFO).log("Plots-MixinBridge plugin loaded.");
    }
}
