package com.overworldlabs.plots.bridge.runtime;

import org.spongepowered.asm.service.IClassProvider;

import java.net.URL;

public final class MixinClassProvider implements IClassProvider {
    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, false);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        if (name != null && name.startsWith("java.")) {
            // java.* classes are loaded by bootstrap, not platform/app loaders.
            return Class.forName(name, initialize, null);
        }
        ClassLoader runtime = LaunchEnvironment.get().getRuntimeLoader();
        if (runtime != null) {
            try {
                return Class.forName(name, initialize, runtime);
            } catch (ClassNotFoundException ignored) {
            }
        }
        try {
            ClassLoader early = this.getClass().getClassLoader();
            if (early != null) {
                return Class.forName(name, initialize, early);
            }
        } catch (ClassNotFoundException ignored) {
        }
        return Class.forName(name, initialize, ClassLoader.getSystemClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return findClass(name, initialize);
    }
}
