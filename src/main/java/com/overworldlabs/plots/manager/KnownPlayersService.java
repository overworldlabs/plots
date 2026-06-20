package com.overworldlabs.plots.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.overworldlabs.plots.util.ConsoleColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent name&lt;-&gt;UUID directory of every player the server has seen.
 * <p>
 * Populated whenever a player is observed online, it lets owner-only actions
 * (e.g. plot transfer) resolve <b>offline</b> players by name. Backed by a small
 * {@code known_players.json} file in the plugin data directory.
 */
public final class KnownPlayersService {
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();
    private final Map<UUID, String> byUuid = new ConcurrentHashMap<>();

    public KnownPlayersService(@Nonnull File dataDir) {
        this.file = new File(dataDir, "known_players.json");
        load();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> raw = gson.fromJson(reader, type);
            if (raw != null) {
                for (Map.Entry<String, String> e : raw.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(e.getKey());
                        String name = e.getValue();
                        if (name != null && !name.isBlank()) {
                            byUuid.put(uuid, name);
                            byName.put(name.toLowerCase(Locale.ROOT), uuid);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            ConsoleColors.info("Loaded " + byUuid.size() + " known players.");
        } catch (Exception e) {
            ConsoleColors.warning("Failed to load known players: " + e.getMessage());
        }
    }

    public synchronized void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Map<String, String> raw = new HashMap<>();
            for (Map.Entry<UUID, String> e : byUuid.entrySet()) {
                raw.put(e.getKey().toString(), e.getValue());
            }
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(raw, writer);
            }
        } catch (Exception e) {
            ConsoleColors.warning("Failed to save known players: " + e.getMessage());
        }
    }

    /** Records a seen player; persists only when the mapping actually changes. */
    public void record(@Nullable UUID uuid, @Nullable String name) {
        if (uuid == null || name == null || name.isBlank()) {
            return;
        }
        String previous = byUuid.get(uuid);
        if (name.equals(previous)) {
            return;
        }
        if (previous != null) {
            byName.remove(previous.toLowerCase(Locale.ROOT));
        }
        byUuid.put(uuid, name);
        byName.put(name.toLowerCase(Locale.ROOT), uuid);
        save();
    }

    /** Resolves an offline/online player UUID by exact name (case-insensitive). */
    @Nullable
    public UUID resolveUuid(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return byName.get(name.trim().toLowerCase(Locale.ROOT));
    }

    /** Returns the last known display name for a UUID, or null. */
    @Nullable
    public String getName(@Nullable UUID uuid) {
        return uuid == null ? null : byUuid.get(uuid);
    }
}
