package com.overworldlabs.plots.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Model class for Hytale Prefabs loaded from JSON
 */
public class Prefab {
    private int version;
    private int blockIdVersion;
    private int anchorX;
    private int anchorY;
    private int anchorZ;
    private List<PrefabBlock> blocks;

    public int getVersion() {
        return version;
    }

    public int getBlockIdVersion() {
        return blockIdVersion;
    }

    public int getAnchorX() {
        return anchorX;
    }

    public int getAnchorY() {
        return anchorY;
    }

    public int getAnchorZ() {
        return anchorZ;
    }

    public List<PrefabBlock> getBlocks() {
        return blocks;
    }

    private Set<String> blockPositions;
    private Set<String> columnPositions;

    public void setBlocks(List<PrefabBlock> blocks) {
        this.blocks = blocks;
        this.blockPositions = null; // Reset cache
        this.columnPositions = null;
    }

    public boolean hasBlockAt(int x, int y, int z) {
        if (blockPositions == null) {
            initializeOccupancy();
        }
        return blockPositions.contains(x + "," + y + "," + z);
    }

    public boolean hasColumnAt(int x, int z) {
        if (columnPositions == null) {
            initializeOccupancy();
        }
        return columnPositions.contains(x + "," + z);
    }

    private void initializeOccupancy() {
        blockPositions = new HashSet<>();
        columnPositions = new HashSet<>();
        if (blocks != null) {
            for (PrefabBlock block : blocks) {
                blockPositions.add(block.getX() + "," + block.getY() + "," + block.getZ());
                columnPositions.add(block.getX() + "," + block.getZ());
            }
        }
    }

    // Bounds
    private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    private int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

    public int getMinX() {
        return minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public int getMinZ() {
        return minZ;
    }

    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
    }

    public int getWidthX() {
        return (maxX < minX) ? 0 : maxX - minX + 1;
    }

    public int getHeightY() {
        return (maxY < minY) ? 0 : maxY - minY + 1;
    }

    public int getDepthZ() {
        return (maxZ < minZ) ? 0 : maxZ - minZ + 1;
    }

    public static class PrefabBlock {
        private int x;
        private int y;
        private int z;
        private String name;

        // Cached block ID
        private int blockId = -1;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public String getName() {
            return name;
        }

        public int getBlockId() {
            return blockId;
        }

        public void setBlockId(int blockId) {
            this.blockId = blockId;
        }
    }
}
