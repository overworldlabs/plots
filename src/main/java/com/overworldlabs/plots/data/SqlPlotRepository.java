package com.overworldlabs.plots.data;

import com.google.gson.Gson;
import com.overworldlabs.plots.api.IPlotRepository;
import com.overworldlabs.plots.model.Plot;
import com.overworldlabs.plots.util.ConsoleColors;
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
    private static final String DELETE_SQL = "DELETE FROM " + TABLE + " WHERE grid_x = ? AND grid_z = ?";
    private static final String UPSERT_SQL = "INSERT INTO " + TABLE
            + " (grid_x, grid_z, owner_uuid, owner_name, name, trusted_json, flags_json, merged_json, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT(grid_x, grid_z) DO UPDATE SET "
            + "owner_uuid = excluded.owner_uuid, "
            + "owner_name = excluded.owner_name, "
            + "name = excluded.name, "
            + "trusted_json = excluded.trusted_json, "
            + "flags_json = excluded.flags_json, "
            + "merged_json = excluded.merged_json, "
            + "created_at = excluded.created_at";

    private final HikariDataSource dataSource;
    private final Gson gson = new Gson();
    private final Map<String, Plot> cache = new ConcurrentHashMap<>();

    public SqlPlotRepository(@Nonnull String jdbcUrl, @Nullable String username, @Nullable String password,
            int maxPoolSize) {
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
                "grid_x INT NOT NULL, " +
                "grid_z INT NOT NULL, " +
                "owner_uuid VARCHAR(64) NOT NULL, " +
                "owner_name VARCHAR(64) NOT NULL, " +
                "name VARCHAR(128) NOT NULL, " +
                "trusted_json TEXT NOT NULL, " +
                "flags_json TEXT NOT NULL, " +
                "merged_json TEXT NOT NULL, " +
                "created_at BIGINT NOT NULL, " +
                "PRIMARY KEY (grid_x, grid_z)" +
                ")";
        String ownerIndexSql = "CREATE INDEX IF NOT EXISTS idx_plots_owner_uuid ON " + TABLE + " (owner_uuid)";

        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(sql);
            st.execute(ownerIndexSql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize plots table", e);
        }
    }

    @Override
    @Nullable
    public Plot getPlot(int gridX, int gridZ) {
        return cache.get(key(gridX, gridZ));
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
    public Collection<Plot> getAllPlots() {
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public void savePlot(@Nonnull Plot plot) {
        cache.put(key(plot.getGridX(), plot.getGridZ()), plot);
        upsert(plot);
    }

    @Override
    public void deletePlot(int gridX, int gridZ) {
        cache.remove(key(gridX, gridZ));
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setInt(1, gridX);
            ps.setInt(2, gridZ);
            ps.executeUpdate();
        } catch (SQLException e) {
            ConsoleColors.error("Failed to delete plot [" + gridX + "," + gridZ + "]: " + e.getMessage());
        }
    }

    @Override
    public void loadAll() {
        cache.clear();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SQL);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
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
                cache.put(key(gridX, gridZ), plot);
            }
            ConsoleColors.info("Loaded " + cache.size() + " plots from SQL storage.");
        } catch (SQLException e) {
            ConsoleColors.error("Failed to load plots from SQL: " + e.getMessage());
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
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            ps.setInt(1, plot.getGridX());
            ps.setInt(2, plot.getGridZ());
            ps.setString(3, String.valueOf(plot.getOwner()));
            ps.setString(4, plot.getOwnerName());
            ps.setString(5, plot.getName());
            ps.setString(6, gson.toJson(plot.getTrustedPlayers().stream().map(UUID::toString).collect(Collectors.toList())));
            ps.setString(7, gson.toJson(plot.getFlags()));
            ps.setString(8, gson.toJson(plot.getMergedPlots()));
            ps.setLong(9, plot.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            ConsoleColors.error("Failed to save plot [" + plot.getGridX() + "," + plot.getGridZ() + "]: " + e.getMessage());
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

    private String key(int x, int z) {
        return x + ";" + z;
    }
}
