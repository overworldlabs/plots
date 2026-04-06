package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(LivingEntity.class)
public abstract class ItemDurabilityMixin {

    /**
     * Base LivingEntity already returns false; keep explicit to avoid callback-based injection.
     */
    @Overwrite
    public boolean canDecreaseItemStackDurability(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {
        return false;
    }
}
