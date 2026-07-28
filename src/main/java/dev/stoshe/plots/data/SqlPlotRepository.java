package dev.stoshe.plots.data;

import com.google.gson.Gson;
import dev.stoshe.plots.api.IPlotRepository;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.Console;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.google.gson.reflect.TypeToken;

public class SqlPlotRepository implements IPlotRepository {
    private static final String TABLE = "plots";
    private static final String SELECT_ALL_SQL = "SELECT * FROM " + TABLE;
    private static final String DELETE_SQL = "DELETE FROM " + TABLE
            + " WHERE LOWER(world) = LOWER(?) AND grid_x = ? AND grid_z = ?";
    private static final String UPSERT_SQL = "INSERT INTO " + TABLE
            + " (world, grid_x, grid_z, owner_uuid, owner_name, name, trusted_json, flags_json, merged_json, created_at, spawn_json, warps_json) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT(world, grid_x, grid_z) DO UPDATE SET "
            + "owner_uuid = excluded.owner_uuid, "
            + "owner_name = excluded.owner_name, "
            + "name = excluded.name, "
            + "trusted_json = excluded.trusted_json, "
            + "flags_json = excluded.flags_json, "
            + "merged_json = excluded.merged_json, "
            + "created_at = excluded.created_at, "
            + "spawn_json = excluded.spawn_json, "
            + "warps_json = excluded.warps_json";

    private final HikariDataSource dataSource;
    private final String jdbcUrl;
    private final String legacyWorldName;
    private final Gson gson = new Gson();
    private final Map<String, Plot> cache = new ConcurrentHashMap<>();

    public SqlPlotRepository(@Nonnull String jdbcUrl, @Nullable String username, @Nullable String password,
            int maxPoolSize, @Nonnull String legacyWorldName) {
        this.jdbcUrl = jdbcUrl;
        this.legacyWorldName = legacyWorldName;
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        if (username != null && !username.isEmpty()) {
            cfg.setUsername(username);
        }
        if (password != null && !password.isEmpty()) {
            cfg.setPassword(password);
        }
        cfg.setMaximumPoolSize(Math.max(2, maxPoolSize));
        cfg.setPoolName("Plots-Hikari");

        this.dataSource = new HikariDataSource(cfg);
        ensureSchema();
    }

    private void ensureSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "world VARCHAR(64) NOT NULL, " +
                "grid_x INT NOT NULL, " +
                "grid_z INT NOT NULL, " +
                "owner_uuid VARCHAR(64) NOT NULL, " +
                "owner_name VARCHAR(64) NOT NULL, " +
                "name VARCHAR(128) NOT NULL, " +
                "trusted_json TEXT NOT NULL, " +
                "flags_json TEXT NOT NULL, " +
                "merged_json TEXT NOT NULL, " +
                "created_at BIGINT NOT NULL, " +
                "spawn_json TEXT, " +
                "warps_json TEXT, " +
                "PRIMARY KEY (world, grid_x, grid_z)" +
                ")";
        String ownerIndexSql = "CREATE INDEX IF NOT EXISTS idx_plots_owner_uuid ON " + TABLE + " (owner_uuid)";

        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize plots table", e);
        }
        // Migrate pre-existing tables that lack the spawn_json/warps_json columns.
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE " + TABLE + " ADD COLUMN spawn_json TEXT");
        } catch (SQLException ignored) {
        }
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE " + TABLE + " ADD COLUMN warps_json TEXT");
        } catch (SQLException ignored) {
        }
        // Migrate legacy single-world tables (no `world` column / PK) by rebuilding
        // the table with the new (world, grid_x, grid_z) primary key.
        //
        // The rebuild relies on transactional DDL (RENAME/CREATE/INSERT/DROP rolled
        // back together on failure). SQLite honours that; MySQL/MariaDB implicitly
        // commit each DDL statement, so a mid-rebuild failure there would strand the
        // data with no rollback. We therefore only auto-rebuild on SQLite and ask
        // admins on other engines to migrate manually rather than risk data loss.
        if (!hasWorldColumn()) {
            if (isSqlite()) {
                migrateToWorldKeyed();
            } else {
                Console.error("Legacy plots table detected on a non-SQLite database. Automatic"
                        + " migration to the multi-world schema is disabled here to avoid data loss."
                        + " Please add a 'world' column + (world, grid_x, grid_z) primary key manually,"
                        + " backfilling world='" + legacyWorldName + "'.");
            }
        }
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(ownerIndexSql);
        } catch (SQLException e) {
            Console.error("Failed to create owner index: " + e.getMessage());
        }
    }

    /** Whether the configured datasource is a SQLite database. */
    private boolean isSqlite() {
        return jdbcUrl != null && jdbcUrl.startsWith("jdbc:sqlite:");
    }

    /** Portable check: a missing {@code world} column makes this SELECT fail. */
    private boolean hasWorldColumn() {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT world FROM " + TABLE + " LIMIT 1")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Rebuilds the legacy table assigning every existing row to the legacy world. */
    private void migrateToWorldKeyed() {
        backupSqliteFile();
        Console.info("Migrating plots table to multi-world schema (assigning existing plots to '"
                + legacyWorldName + "')...");
        String createNew = "CREATE TABLE " + TABLE + " (" +
                "world VARCHAR(64) NOT NULL, grid_x INT NOT NULL, grid_z INT NOT NULL, " +
                "owner_uuid VARCHAR(64) NOT NULL, owner_name VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, " +
                "trusted_json TEXT NOT NULL, flags_json TEXT NOT NULL, merged_json TEXT NOT NULL, " +
                "created_at BIGINT NOT NULL, spawn_json TEXT, warps_json TEXT, " +
                "PRIMARY KEY (world, grid_x, grid_z))";
        try (Connection conn = dataSource.getConnection()) {
            boolean prevAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + TABLE + " RENAME TO plots_old");
                st.execute(createNew);
                st.execute("INSERT INTO " + TABLE
                        + " (world, grid_x, grid_z, owner_uuid, owner_name, name, trusted_json, flags_json, merged_json, created_at, spawn_json, warps_json) "
                        + "SELECT '" + legacyWorldName.replace("'", "''")
                        + "', grid_x, grid_z, owner_uuid, owner_name, name, trusted_json, flags_json, merged_json, created_at, spawn_json, warps_json FROM plots_old");
                st.execute("DROP TABLE plots_old");
                conn.commit();
                Console.success("Plots table migrated to multi-world schema.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(prevAuto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to migrate plots table to multi-world schema", e);
        }
    }

    /** Best-effort copy of the SQLite db file before a destructive migration. */
    private void backupSqliteFile() {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:sqlite:")) {
            return;
        }
        String path = jdbcUrl.substring("jdbc:sqlite:".length());
        if (path.isEmpty() || path.startsWith(":")) {
            return; // in-memory or unsupported
        }
        try {
            java.nio.file.Path src = java.nio.file.Paths.get(path);
            if (java.nio.file.Files.exists(src)) {
                java.nio.file.Path bak = java.nio.file.Paths.get(path + ".pre-multiworld.bak");
                java.nio.file.Files.copy(src, bak, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Console.info("Backed up plots database to " + bak.getFileName());
            }
        } catch (Exception e) {
            Console.error("Could not back up plots database before migration: " + e.getMessage());
        }
    }

    @Override
    @Nullable
    public Plot getPlot(@Nonnull String world, int gridX, int gridZ) {
        return cache.get(key(world, gridX, gridZ));
    }

    @Override
    @Nonnull
    public List<Plot> getPlotsByOwner(@Nonnull UUID ownerUuid) {
        return cache.values().stream()
                .filter(plot -> ownerUuid.equals(plot.getOwner()))
                .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public List<Plot> getPlotsByOwnerInWorld(@Nonnull UUID ownerUuid, @Nonnull String world) {
        return cache.values().stream()
                .filter(plot -> ownerUuid.equals(plot.getOwner()) && world.equalsIgnoreCase(plot.getWorld()))
                .collect(Collectors.toList());
    }

    @Override
    public int countByOwnerInWorld(@Nonnull UUID ownerUuid, @Nonnull String world) {
        return (int) cache.values().stream()
                .filter(plot -> ownerUuid.equals(plot.getOwner()) && world.equalsIgnoreCase(plot.getWorld()))
                .count();
    }

    @Override
    @Nonnull
    public List<Plot> getPlotsInWorld(@Nonnull String world) {
        return cache.values().stream()
                .filter(plot -> world.equalsIgnoreCase(plot.getWorld()))
                .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public Collection<Plot> getAllPlots() {
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public void savePlot(@Nonnull Plot plot) {
        if (plot.getWorld() == null) {
            plot.setWorld(legacyWorldName);
        }
        cache.put(key(plot.getWorld(), plot.getGridX(), plot.getGridZ()), plot);
        upsert(plot);
    }

    @Override
    public void deletePlot(@Nonnull String world, int gridX, int gridZ) {
        cache.remove(key(world, gridX, gridZ));
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setString(1, world);
            ps.setInt(2, gridX);
            ps.setInt(3, gridZ);
            ps.executeUpdate();
        } catch (SQLException e) {
            Console.error("Failed to delete plot [" + world + ":" + gridX + "," + gridZ + "]: " + e.getMessage());
        }
    }

    @Override
    public void loadAll() {
        cache.clear();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SQL);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String world = rs.getString("world");
                if (world == null || world.isBlank()) {
                    world = legacyWorldName;
                }
                int gridX = rs.getInt("grid_x");
                int gridZ = rs.getInt("grid_z");
                UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                String ownerName = rs.getString("owner_name");
                String name = rs.getString("name");
                long createdAt = rs.getLong("created_at");

                List<UUID> trusted = parseTrusted(rs.getString("trusted_json"));
                Map<String, Object> flags = parseFlags(rs.getString("flags_json"));
                Set<String> merged = parseMerged(rs.getString("merged_json"));

                Plot plot = new Plot(gridX, gridZ, owner, ownerName, name, trusted, flags, merged, createdAt);
                plot.setWorld(world);
                String spawnJson = rs.getString("spawn_json");
                if (spawnJson != null && !spawnJson.isEmpty()) {
                    try {
                        Plot.PlotSpawn spawn = gson.fromJson(spawnJson, Plot.PlotSpawn.class);
                        if (spawn != null) {
                            plot.setSpawn(spawn);
                        }
                    } catch (Exception ignored) {
                    }
                }
                String warpsJson = rs.getString("warps_json");
                if (warpsJson != null && !warpsJson.isEmpty()) {
                    try {
                        Type warpType = new TypeToken<List<Plot.PlotWarp>>() {
                        }.getType();
                        List<Plot.PlotWarp> warps = gson.fromJson(warpsJson, warpType);
                        if (warps != null) {
                            plot.setWarps(warps);
                        }
                    } catch (Exception ignored) {
                    }
                }
                cache.put(key(world, gridX, gridZ), plot);
            }
            Console.info("Loaded " + cache.size() + " plots from SQL storage.");
        } catch (SQLException e) {
            Console.error("Failed to load plots from SQL: " + e.getMessage());
        }
    }

    @Override
    public void saveAll() {
        for (Plot plot : cache.values()) {
            upsert(plot);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private void upsert(Plot plot) {
        String world = plot.getWorld() != null ? plot.getWorld() : legacyWorldName;
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, world);
            ps.setInt(2, plot.getGridX());
            ps.setInt(3, plot.getGridZ());
            ps.setString(4, String.valueOf(plot.getOwner()));
            ps.setString(5, plot.getOwnerName());
            ps.setString(6, plot.getName());
            ps.setString(7, gson.toJson(plot.getTrustedPlayers().stream().map(UUID::toString).collect(Collectors.toList())));
            ps.setString(8, gson.toJson(plot.getFlags()));
            ps.setString(9, gson.toJson(plot.getMergedPlots()));
            ps.setLong(10, plot.getCreatedAt());
            ps.setString(11, plot.getSpawn() != null ? gson.toJson(plot.getSpawn()) : null);
            ps.setString(12, plot.getWarpCount() > 0 ? gson.toJson(plot.getWarps()) : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            Console.error("Failed to save plot [" + world + ":" + plot.getGridX() + "," + plot.getGridZ()
                    + "]: " + e.getMessage());
        }
    }

    private List<UUID> parseTrusted(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<String>>() {
        }.getType();
        List<String> raw = gson.fromJson(json, type);
        if (raw == null) {
            return new ArrayList<>();
        }
        return raw.stream().map(UUID::fromString).collect(Collectors.toList());
    }

    private Map<String, Object> parseFlags(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> map = gson.fromJson(json, type);
        return map != null ? map : new HashMap<>();
    }

    private Set<String> parseMerged(String json) {
        if (json == null || json.isEmpty()) {
            return new HashSet<>();
        }
        Type type = new TypeToken<Set<String>>() {
        }.getType();
        Set<String> set = gson.fromJson(json, type);
        return set != null ? set : new HashSet<>();
    }

    /**
     * Builds the in-memory cache key for a plot. The world segment is lower-cased
     * so cache lookups are case-insensitive and consistent with the
     * {@code equalsIgnoreCase} world filters.
     *
     * @param world the plot world (nullable; treated as empty)
     * @param x     the plot grid X coordinate
     * @param z     the plot grid Z coordinate
     * @return the cache key
     */
    private String key(String world, int x, int z) {
        String normalizedWorld = world == null ? "" : world.toLowerCase();
        return normalizedWorld + ";" + x + ";" + z;
    }
}
