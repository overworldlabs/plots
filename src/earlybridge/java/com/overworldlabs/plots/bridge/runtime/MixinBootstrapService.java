package com.overworldlabs.plots.bridge.runtime;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public final class MixinBootstrapService implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return "PlotsMixinBridge";
    }

    @Override
    public String getServiceClassName() {
        return "com.overworldlabs.plots.bridge.runtime.MixinServiceImpl";
    }

    @Override
    public void bootstrap() {
        // no-op
    }
}
