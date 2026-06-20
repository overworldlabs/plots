package com.overworldlabs.plots.integration.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MixinBridgeStatus {
    public static final String ACTIVE_PROPERTY = "plots.mixin.bridge.active";
    public static final String BOOTSTRAP_READY_PROPERTY = "plots.mixin.bridge.bootstrap.ready";
    public static final String MIXINS_LOADED_PROPERTY = "plots.mixins.loaded";
    public static final String MIXINS_LOADED_LIST_PROPERTY = "plots.mixins.loaded.list";

    // Shared TaleGuard bridge equivalents. Plots now consumes TaleGuard, but the
    // legacy plots bridge properties are still honoured for backwards compatibility.
    public static final String TG_ACTIVE_PROPERTY = "taleguard.bridge.active";
    public static final String TG_BOOTSTRAP_READY_PROPERTY = "taleguard.bridge.bootstrap.ready";
    public static final String TG_MIXINS_LOADED_LIST_PROPERTY = "taleguard.mixins.loaded.list";

    private MixinBridgeStatus() {
    }

    private static boolean flag(String property) {
        return Boolean.parseBoolean(System.getProperty(property, "false"));
    }

    public static boolean isActive() {
        return flag(ACTIVE_PROPERTY) || flag(TG_ACTIVE_PROPERTY);
    }

    public static boolean isMixinsLoaded() {
        if (flag(MIXINS_LOADED_PROPERTY)) {
            return true;
        }
        String tg = System.getProperty(TG_MIXINS_LOADED_LIST_PROPERTY, "");
        return tg != null && !tg.isBlank();
    }

    public static boolean isBootstrapReady() {
        return flag(BOOTSTRAP_READY_PROPERTY) || flag(TG_BOOTSTRAP_READY_PROPERTY);
    }

    public static boolean isReadyForMixinFlags() {
        return isActive() && isBootstrapReady();
    }

    public static List<String> getLoadedMixins() {
        String raw = System.getProperty(MIXINS_LOADED_LIST_PROPERTY, "");
        if (raw == null || raw.isBlank()) {
            raw = System.getProperty(TG_MIXINS_LOADED_LIST_PROPERTY, "");
        }
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
