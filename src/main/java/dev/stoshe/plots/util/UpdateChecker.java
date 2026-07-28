package dev.stoshe.plots.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;

/**
 * Reads the plugin's GitHub releases: the latest tag for the update notice, and a release's notes
 * for the changelog popup.
 */
public class UpdateChecker {
    private static final String RELEASES_API = "https://api.github.com/repos/stoshelabs/plots/releases";
    private static final String GITHUB_API_URL = RELEASES_API + "/latest";
    public static final String RELEASES_URL = "https://github.com/stoshelabs/plots/releases";
    private static final int TIMEOUT_MS = 5000;

    /** The release's version (tag without a leading {@code v}) and its markdown notes (body). */
    public record ReleaseInfo(String version, String notes) {
    }

    /**
     * Fetches the latest release tag asynchronously.
     *
     * @return the latest version, or null when the check fails (offline / rate-limited / no release)
     */
    public static CompletableFuture<String> checkForUpdates(String currentVersion) {
        return CompletableFuture.supplyAsync(() -> {
            ReleaseInfo info = fetchReleaseFrom(GITHUB_API_URL);
            return info == null ? null : info.version();
        });
    }

    /**
     * Fetches the release notes for a specific running {@code version}, falling back to the latest
     * published release when that exact version has no release yet (e.g. before it's been cut).
     * Resolves to {@code null} only when neither can be reached, so callers simply skip the
     * changelog UI rather than erroring.
     */
    public static CompletableFuture<ReleaseInfo> fetchReleaseForVersion(String version) {
        return CompletableFuture.supplyAsync(() -> {
            if (version != null && !version.isBlank()) {
                ReleaseInfo exact = fetchReleaseFrom(RELEASES_API + "/tags/v" + version.trim());

                if (exact != null) {
                    return exact;
                }
            }

            // No release for the current version yet → show the latest available (previous) one.
            return fetchReleaseFrom(GITHUB_API_URL);
        });
    }

    /** Synchronously GETs a GitHub release endpoint and parses {tag_name, body}, or null on any failure. */
    @Nullable
    private static ReleaseInfo fetchReleaseFrom(String urlStr) {
        try {
            var url = new java.net.URI(urlStr).toURL();
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
                    response.append(line).append('\n');
                }
            }

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            String tag = json.has("tag_name") ? json.get("tag_name").getAsString() : null;
            if (tag == null) {
                return null;
            }

            String version = tag.startsWith("v") ? tag.substring(1) : tag;
            String notes = json.has("body") && !json.get("body").isJsonNull()
                    ? json.get("body").getAsString()
                    : "";
            return new ReleaseInfo(version, notes);
        } catch (Exception e) {
            // Offline / private repo / rate-limited — silently give up (no update info).
            return null;
        }
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
