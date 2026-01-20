package com.overworldlabs.plots.manager;

import com.google.gson.Gson;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.overworldlabs.plots.model.Prefab;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for loading and accessing prefabs
 */
public class PrefabManager {
    private static final Logger LOGGER = Logger.getLogger("Plots");
    private final Map<String, Prefab> loadedPrefabs = new HashMap<>();
    private final Gson gson = new Gson();
    private final Path prefabDir;

    public PrefabManager(File dataDir) {
        this.prefabDir = dataDir.toPath().resolve("prefabs");
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        File dir = prefabDir.toFile();
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                LOGGER.info("Created prefabs directory at: " + dir.getAbsolutePath());
            } else {
                LOGGER.severe("Failed to create prefabs directory!");
            }
        }
    }

    public Prefab getOrLoadPrefab(String inputName) {
        if (inputName == null || inputName.isEmpty()) {
            return null;
        }

        // Maintain old references but handle extension-less inputs
        String fileName = inputName;
        if (!fileName.toLowerCase().endsWith(".prefab.json")) {
            fileName += ".prefab.json";
        }

        if (loadedPrefabs.containsKey(fileName)) {
            return loadedPrefabs.get(fileName);
        }

        File file = prefabDir.resolve(fileName).toFile();
        if (!file.exists()) {
            LOGGER.log(Level.WARNING, "Prefab file not found: " + file.getAbsolutePath());
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            Prefab prefab = gson.fromJson(reader, Prefab.class);
            if (prefab != null) {
                resolveBlockIds(prefab);
                loadedPrefabs.put(fileName, prefab);
                LOGGER.info("Loaded prefab: " + fileName + " with " + prefab.getBlocks().size() + " blocks.");
                return prefab;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load prefab: " + fileName, e);
        }

        return null;
    }

    /**
     * Resolves block names to IDs using the Hytale API
     */
    private void resolveBlockIds(Prefab prefab) {
        if (prefab == null || prefab.getBlocks() == null)
            return;

        var assetMap = BlockType.getAssetMap();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (Prefab.PrefabBlock block : prefab.getBlocks()) {
            // Stats
            if (block.getX() < minX)
                minX = block.getX();
            if (block.getX() > maxX)
                maxX = block.getX();
            if (block.getY() < minY)
                minY = block.getY();
            if (block.getY() > maxY)
                maxY = block.getY();
            if (block.getZ() < minZ)
                minZ = block.getZ();
            if (block.getZ() > maxZ)
                maxZ = block.getZ();

            // Resolve ID
            if (assetMap != null) {
                int id = assetMap.getIndex(block.getName());
                if (id != Integer.MIN_VALUE) {
                    block.setBlockId(id);
                }
            }
        }

        prefab.setMinX(minX);
        prefab.setMaxX(maxX);
        prefab.setMinY(minY);
        prefab.setMaxY(maxY);
        prefab.setMinZ(minZ);
        prefab.setMaxZ(maxZ);
    }

    /**
     * Force re-resolve block IDs (useful if they weren't available at load time)
     */
    public void resolveAllBlockIds() {
        for (Prefab prefab : loadedPrefabs.values()) {
            resolveBlockIds(prefab);
        }
    }
}
