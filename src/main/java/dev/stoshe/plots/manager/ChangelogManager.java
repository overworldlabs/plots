package dev.stoshe.plots.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.stoshe.plots.util.Console;
import dev.stoshe.plots.util.UpdateChecker;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the latest release notes (fetched once from GitHub) and remembers which release each admin
 * dismissed, so the changelog popup auto-shows once per new version and never again after "don't show
 * again" (until the next version). Admin-only. If the GitHub fetch fails the popup simply never shows.
 */
public class ChangelogManager {
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /** admin uuid -> the release version they dismissed with "don't show again". */
    private final Map<UUID, String> dismissed = new ConcurrentHashMap<>();
    private volatile UpdateChecker.ReleaseInfo release;

    public ChangelogManager(File dataDir) {
        this.file = new File(dataDir, "changelog_seen.json");
        load();
    }

    /**
     * Kicks off the async GitHub fetch of release notes for the running {@code version}, falling back
     * to the latest published release when that version has no release yet.
     */
    public void fetch(String version) {
        UpdateChecker.fetchReleaseForVersion(version).thenAccept(info -> {
            this.release = info;

            if (info != null) {
                Console.info("Changelog available for v" + info.version() + ".");
            }
        });
    }

    /** True when release notes were fetched and are non-empty (nothing to show otherwise). */
    public boolean isReady() {
        UpdateChecker.ReleaseInfo r = release;
        return r != null && r.notes() != null && !r.notes().isBlank();
    }

    public String version() {
        UpdateChecker.ReleaseInfo r = release;
        return r == null ? "" : r.version();
    }

    public String notes() {
        UpdateChecker.ReleaseInfo r = release;
        return r == null ? "" : r.notes();
    }

    /** Auto-show the popup only when notes exist and this admin hasn't dismissed THIS version. */
    public boolean shouldAutoShow(UUID uuid) {
        return isReady() && !version().equals(dismissed.get(uuid));
    }

    /** Marks the current release dismissed for an admin ("don't show again" until the next version). */
    public void dismiss(UUID uuid) {
        if (uuid == null || !isReady()) {
            return;
        }

        dismissed.put(uuid, version());
        save();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        try (Reader reader = new FileReader(file)) {
            Map<String, String> raw = gson.fromJson(reader,
                    new TypeToken<Map<String, String>>() {
                    }.getType());

            if (raw != null) {
                raw.forEach((k, v) -> {
                    try {
                        dismissed.put(UUID.fromString(k), v);
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
        } catch (Exception e) {
            Console.error("Failed to load changelog state: " + e.getMessage());
        }
    }

    private void save() {
        Map<String, String> raw = new HashMap<>();
        dismissed.forEach((k, v) -> raw.put(k.toString(), v));

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(raw, writer);
        } catch (Exception e) {
            Console.error("Failed to save changelog state: " + e.getMessage());
        }
    }
}
