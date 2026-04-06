package com.overworldlabs.plots.integration.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MixinBridgeStatus {
    public static final String ACTIVE_PROPERTY = "plots.mixin.bridge.active";
    public static final String BOOTSTRAP_READY_PROPERTY = "plots.mixin.bridge.bootstrap.ready";
    public static final String MIXINS_LOADED_PROPERTY = "plots.mixins.loaded";
    public static final String MIXINS_LOADED_LIST_PROPERTY = "plots.mixins.loaded.list";

    private MixinBridgeStatus() {
    }

    public static boolean isActive() {
        return Boolean.parseBoolean(System.getProperty(ACTIVE_PROPERTY, "false"));
    }

    public static boolean isMixinsLoaded() {
        return Boolean.parseBoolean(System.getProperty(MIXINS_LOADED_PROPERTY, "false"));
    }

    public static boolean isBootstrapReady() {
        return Boolean.parseBoolean(System.getProperty(BOOTSTRAP_READY_PROPERTY, "false"));
    }

    public static boolean isReadyForMixinFlags() {
        return isActive() && isBootstrapReady();
    }

    public static List<String> getLoadedMixins() {
        String raw = System.getProperty(MIXINS_LOADED_LIST_PROPERTY, "");
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        String[] parts = raw.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (part != null) {
                String v = part.trim();
                if (!v.isEmpty()) {
                    result.add(v);
                }
            }
        }
        Collections.sort(result);
        return result;
    }
}
