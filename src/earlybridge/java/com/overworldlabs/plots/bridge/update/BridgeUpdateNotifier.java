package com.overworldlabs.plots.bridge.update;

import java.util.concurrent.atomic.AtomicBoolean;

public final class BridgeUpdateNotifier {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final String RELEASES_URL_DEFAULT = "https://github.com/overworldlabs/plots-mixin-bridge/releases";

    private BridgeUpdateNotifier() {
    }

    public static void checkOnce(String currentVersion) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        BridgeUpdateChecker.checkForUpdates(currentVersion).thenAccept(latestVersion -> {
            if (latestVersion == null) {
                return;
            }
            if (BridgeUpdateChecker.isNewerVersion(currentVersion, latestVersion)) {
                System.out.println("[Plots-MixinBridge] A new version is available: " + latestVersion);
                System.out.println("[Plots-MixinBridge] Download: " + releasesUrl());
            }
        });
    }

    private static String releasesUrl() {
        String fromProperty = System.getProperty("plots.mixin.bridge.update.releases");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        return RELEASES_URL_DEFAULT;
    }
}
