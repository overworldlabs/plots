package com.overworldlabs.plots.core;

import com.overworldlabs.plots.api.IServiceRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the service registry.
 */
public class ServiceRegistryImpl implements IServiceRegistry {
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    @Override
    public <T> void register(Class<T> serviceClass, T serviceInstance) {
        services.put(serviceClass, serviceInstance);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getService(Class<T> serviceClass) {
        return Optional.ofNullable((T) services.get(serviceClass));
    }

    @Override
    public <T> T getServiceOrThrow(Class<T> serviceClass) {
        return getService(serviceClass)
                .orElseThrow(() -> new IllegalStateException("Service not registered: " + serviceClass.getName()));
    }
}
