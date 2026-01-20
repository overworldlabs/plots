package com.overworldlabs.plots.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Utility to check for plugin updates from GitHub releases
 */
public class UpdateChecker {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/overworldlabs/plots/releases/latest";
    private static final int TIMEOUT_MS = 5000;

    /**
     * Checks for updates asynchronously
     * 
     * @param currentVersion The current plugin version
     * @return CompletableFuture with the latest version, or null if check failed
     */
    public static CompletableFuture<String> checkForUpdates(String currentVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new java.net.URI(GITHUB_API_URL).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return null;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                String latestVersion = json.get("tag_name").getAsString();

                // Remove 'v' prefix if present
                if (latestVersion.startsWith("v")) {
                    latestVersion = latestVersion.substring(1);
                }

                return latestVersion;
            } catch (Exception e) {
                System.err.println("[Plots] Failed to check for updates: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Compares two version strings
     * 
     * @return true if newVersion is greater than currentVersion
     */
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
                } else if (newPart < currentPart) {
                    return false;
                }
            }

            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
