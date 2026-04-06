package com.overworldlabs.plots.bridge.runtime;

import org.objectweb.asm.ClassReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public final class LaunchEnvironment {
    private static LaunchEnvironment instance;

    private final ClassLoader systemLoader;
    private final ClassLoader earlyPluginLoader;
    private ClassLoader runtimeLoader;

    private LaunchEnvironment(ClassLoader systemLoader, ClassLoader earlyPluginLoader) {
        this.systemLoader = systemLoader;
        this.earlyPluginLoader = earlyPluginLoader;
    }

    public static void create(ClassLoader systemLoader, ClassLoader earlyPluginLoader) {
        if (instance != null) {
            return;
        }
        instance = new LaunchEnvironment(systemLoader, earlyPluginLoader);
    }

    public static LaunchEnvironment get() {
        if (instance == null) {
            throw new IllegalStateException("LaunchEnvironment not initialized");
        }
        return instance;
    }

    public ClassLoader getRuntimeLoader() {
        return runtimeLoader;
    }

    public void captureRuntimeLoader(ClassLoader loader) {
        if (this.runtimeLoader == null) {
            this.runtimeLoader = loader;
        }
    }

    public ClassLoader findLoaderForClass(String className) throws ClassNotFoundException {
        try {
            return findLoaderFor(className.replace(".", "/").concat(".class"));
        } catch (IOException e) {
            throw new ClassNotFoundException("Could not find class '" + className + "'", e);
        }
    }

    public ClassLoader findLoaderFor(String resourceName) throws IOException {
        if (runtimeLoader != null && runtimeLoader.getResource(resourceName) != null) {
            return runtimeLoader;
        }
        if (earlyPluginLoader != null && earlyPluginLoader.getResource(resourceName) != null) {
            return earlyPluginLoader;
        }
        if (systemLoader != null && systemLoader.getResource(resourceName) != null) {
            return systemLoader;
        }
        throw new FileNotFoundException("Resource not found: " + resourceName);
    }

    public InputStream findResourceStream(String resourceName) throws IOException {
        return findLoaderFor(resourceName).getResourceAsStream(resourceName);
    }

    public ClassReader getClassReader(String className) throws IOException, ClassNotFoundException {
        String fileName = className.replace(".", "/").concat(".class");
        try (InputStream stream = openClassStream(fileName)) {
            if (stream != null) {
                return new ClassReader(stream);
            }
        }
        // JDK classes may come from modules (jrt) and not be visible in plugin loaders.
        if (className.startsWith("java.")) {
            return new ClassReader(className);
        }
        throw new ClassNotFoundException("Could not read class: " + className);
    }

    private InputStream openClassStream(String fileName) throws IOException {
        try {
            InputStream stream = findResourceStream(fileName);
            if (stream != null) {
                return stream;
            }
        } catch (FileNotFoundException ignored) {
        }
        InputStream systemStream = ClassLoader.getSystemResourceAsStream(fileName);
        if (systemStream != null) {
            return systemStream;
        }
        ClassLoader platform = ClassLoader.getPlatformClassLoader();
        if (platform != null) {
            InputStream platformStream = platform.getResourceAsStream(fileName);
            if (platformStream != null) {
                return platformStream;
            }
        }
        return null;
    }
}
