package dev.stoshe.plots.api;

import java.util.Optional;

/**
 * A central registry for plugin services to facilitate decoupling.
 */
public interface IServiceRegistry {
    <T> void register(Class<T> serviceClass, T serviceInstance);

    <T> Optional<T> getService(Class<T> serviceClass);

    <T> T getServiceOrThrow(Class<T> serviceClass);
}
