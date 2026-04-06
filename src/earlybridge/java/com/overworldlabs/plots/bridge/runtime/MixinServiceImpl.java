package com.overworldlabs.plots.bridge.runtime;

import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;
import org.spongepowered.asm.util.IConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

public final class MixinServiceImpl extends MixinServiceAbstract {
    public static IMixinTransformer transformer;
    private static IConsumer<MixinEnvironment.Phase> phaseConsumer;

    private final IClassProvider classProvider = new MixinClassProvider();
    private final IClassBytecodeProvider bytecodeProvider = new MixinBytecodeProvider();

    @Override
    public String getName() {
        return "PlotsMixinBridge";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return classProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return bytecodeProvider;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return List.of("com.overworldlabs.plots.bridge.runtime.MixinPlatformAgent");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        try {
            return new ContainerHandleURI(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException ignored) {
            return new ContainerHandleVirtual(getName());
        }
    }

    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory factory) {
            transformer = factory.createTransformer();
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        try {
            return LaunchEnvironment.get().findResourceStream(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> consumer) {
        super.wire(phase, consumer);
        phaseConsumer = consumer;
    }

    public static void changePhase(MixinEnvironment.Phase phase) {
        if (phaseConsumer != null) {
            phaseConsumer.accept(phase);
        }
    }
}
