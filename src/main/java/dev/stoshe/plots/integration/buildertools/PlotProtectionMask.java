package dev.stoshe.plots.integration.buildertools;

import org.joml.Vector3i;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockFilter;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.accessor.ChunkAccessor;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Block mask that restricts BuilderTools operations to plots where the player
 * has permission
 */
@SuppressWarnings({ "rawtypes", "deprecation" })
public class PlotProtectionMask extends BlockMask {
    private final UUID playerUuid;
    private final IPlotManager plotManager;
    private final BlockMask originalMask;

    public PlotProtectionMask(@Nonnull UUID playerUuid, @Nullable BlockMask originalMask) {
        super(getFiltersFrom(originalMask));
        this.playerUuid = playerUuid;
        this.plotManager = Plots.getInstance().getPlotManager();
        this.originalMask = originalMask;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    private static BlockFilter[] getFiltersFrom(@Nullable BlockMask mask) {
        return mask != null ? mask.getFilters() : new BlockFilter[0];
    }

    @Override
    public boolean isExcluded(@Nonnull ChunkAccessor chunks, int x, int y, int z, Vector3i min, Vector3i max,
            int worldY) {
        // Check original mask first
        if (originalMask != null && originalMask.isExcluded(chunks, x, y, z, min, max, worldY)) {
            return true;
        }
        return isExcludedByPlot(chunks, x, y, z);
    }

    @Override
    public boolean isExcluded(@Nonnull ChunkAccessor chunks, int x, int y, int z, Vector3i min, Vector3i max,
            int worldY, int layer) {
        // Check original mask first
        if (originalMask != null && originalMask.isExcluded(chunks, x, y, z, min, max, worldY, layer)) {
            return true;
        }
        return isExcludedByPlot(chunks, x, y, z);
    }

    private boolean isExcludedByPlot(@Nullable ChunkAccessor chunks, int x, int y, int z) {
        // Bypass for admin
        if (PermissionUtil.hasAdminPermission(playerUuid)) {
            return false;
        }

        int[] worldCoords = resolveWorldCoordinates(chunks, x, z);
        int worldX = worldCoords[0];
        int worldZ = worldCoords[1];

        String worldName = resolveWorldName();
        if (worldName == null || !plotManager.isInPlot(worldName, worldX, worldZ)) {
            return true;
        }

        return !plotManager.canUseBuilderTools(playerUuid, worldName, worldX, worldZ);
    }

    @Nullable
    private String resolveWorldName() {
        PlayerRef ref = Universe.get().getPlayer(playerUuid);
        if (ref == null || ref.getWorldUuid() == null) {
            return null;
        }
        var world = Universe.get().getWorld(ref.getWorldUuid());
        return world != null ? world.getName() : null;
    }

    private int[] resolveWorldCoordinates(@Nullable ChunkAccessor chunks, int x, int z) {
        if (chunks == null) {
            return new int[] { x, z };
        }

        Integer baseX = readChunkInt(chunks, "getX");
        Integer baseZ = readChunkInt(chunks, "getZ");
        if (baseX == null || baseZ == null) {
            return new int[] { x, z };
        }

        // Local chunk coordinates are 0..31 in this accessor family.
        if (x >= 0 && z >= 0 && x <= 31 && z <= 31) {
            return new int[] { baseX + x, baseZ + z };
        }

        return new int[] { x, z };
    }

    @Nullable
    private Integer readChunkInt(@Nonnull ChunkAccessor chunks, @Nonnull String methodName) {
        try {
            var method = chunks.getClass().getMethod(methodName);
            Object value = method.invoke(chunks);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public boolean isInverted() {
        return false;
    }

    @Override
    @Nonnull
    public String toString() {
        return "PlotProtectionMask{player=" + playerUuid + "}";
    }

    @Override
    @Nonnull
    public String informativeToString() {
        return toString();
    }
}
