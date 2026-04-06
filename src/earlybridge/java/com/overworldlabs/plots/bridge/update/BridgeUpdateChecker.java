package com.overworldlabs.plots.bridge.update;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BridgeUpdateChecker {
    private static final String DEFAULT_GITHUB_API_URL = "https://api.github.com/repos/overworldlabs/plots-mixin-bridge/releases/latest";
    private static final int TIMEOUT_MS = 5000;
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");

    private BridgeUpdateChecker() {
    }

    public static CompletableFuture<String> checkForUpdates(@Nullable String currentVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new java.net.URI(apiUrl()).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (connection.getResponseCode() != 200) {
                    return null;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                Matcher matcher = TAG_NAME_PATTERN.matcher(response.toString());
                if (!matcher.find()) {
                    return null;
                }

                String latestVersion = matcher.group(1);
                if (latestVersion.startsWith("v")) {
                    latestVersion = latestVersion.substring(1);
                }
                return latestVersion;
            } catch (Exception ignored) {
                return null;
            }
        });
    }

    public static boolean isNewerVersion(@Nullable String currentVersion, @Nullable String newVersion) {
        if (currentVersion == null || newVersion == null) {
            return false;
        }
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] newParts = newVersion.split("\\.");
            int length = Math.max(currentParts.length, newParts.length);
            for (int i = 0; i < length; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
                if (newPart > currentPart) {
                    return true;
                }
                if (newPart < currentPart) {
                    return false;
                }
            }
            return false;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String apiUrl() {
        String fromProperty = System.getProperty("plots.mixin.bridge.update.api");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        return DEFAULT_GITHUB_API_URL;
    }
}
