package com.overworldlabs.plots.integration.buildertools;

import com.hypixel.hytale.builtin.buildertools.scriptedbrushes.BrushConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.overworldlabs.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * BrushConfig wrapper that always applies plot protection on top of any
 * configured scripted-brush mask.
 */
public class ProtectedBrushConfig extends BrushConfig {
    private final UUID playerUuid;
    private final Player player;

    public ProtectedBrushConfig(@Nonnull Player player, @Nonnull UUID playerUuid) {
        super();
        this.player = player;
        this.playerUuid = playerUuid;
    }

    public ProtectedBrushConfig(@Nonnull Player player, @Nonnull UUID playerUuid, @Nonnull BrushConfig original) {
        super();
        this.player = player;
        this.playerUuid = playerUuid;
        copyFromOriginal(original);
        applyProtectionMask();
    }

    @Override
    public BlockMask getBlockMask() {
        BlockMask current = super.getBlockMask();
        if (PermissionUtil.hasAdminPermission(playerUuid, player)) {
            return current;
        }
        if (current instanceof PlotProtectionMask) {
            return current;
        }

        return new PlotProtectionMask(playerUuid, current);
    }

    @Override
    public void setBrushMask(BlockMask brushMask) {
        if (PermissionUtil.hasAdminPermission(playerUuid, player)) {
            super.setBrushMask(brushMask);
            return;
        }
        if (brushMask instanceof PlotProtectionMask) {
            super.setBrushMask(brushMask);
        } else {
            super.setBrushMask(new PlotProtectionMask(playerUuid, brushMask));
        }
        applyProtectionMask();
    }

    @Override
    public void setOperationMask(BlockMask operationMask) {
        if (PermissionUtil.hasAdminPermission(playerUuid, player)) {
            super.setOperationMask(operationMask);
            return;
        }
        if (operationMask instanceof PlotProtectionMask) {
            super.setOperationMask(operationMask);
        } else {
            super.setOperationMask(new PlotProtectionMask(playerUuid, operationMask));
        }
        applyProtectionMask();
    }

    @Override
    public void appendOperationMask(BlockMask operationMask) {
        if (PermissionUtil.hasAdminPermission(playerUuid, player)) {
            super.appendOperationMask(operationMask);
            return;
        }
        if (operationMask instanceof PlotProtectionMask) {
            super.appendOperationMask(operationMask);
        } else {
            super.appendOperationMask(new PlotProtectionMask(playerUuid, operationMask));
        }
        applyProtectionMask();
    }

    private void applyProtectionMask() {
        if (PermissionUtil.hasAdminPermission(playerUuid, player)) {
            return;
        }
        super.setUseBrushMask(true);
        super.setUseOperationMask(true);
        super.appendOperationMask(new PlotProtectionMask(playerUuid, null));
    }

    @Nullable
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    private void copyFromOriginal(@Nonnull BrushConfig original) {
        try {
            super.setBrushMask(original.getBlockMask());
        } catch (Exception ignored) {
        }
        try {
            super.setDensity(original.getDensity());
        } catch (Exception ignored) {
        }
        try {
            super.setHistoryMask(original.getHistoryMask());
        } catch (Exception ignored) {
        }
        try {
            super.setShapeWidth(original.getShapeWidth());
            super.setShapeHeight(original.getShapeHeight());
            super.setShapeThickness(original.getShapeThickness());
            super.setCapped(original.isCapped());
            super.setShape(original.getShape());
        } catch (Exception ignored) {
        }
        try {
            super.setPattern(original.getPattern());
        } catch (Exception ignored) {
        }
        try {
            if (original.getOriginOffset() != null) {
                super.setOriginOffset(original.getOriginOffset().clone());
            }
        } catch (Exception ignored) {
        }
    }
}
