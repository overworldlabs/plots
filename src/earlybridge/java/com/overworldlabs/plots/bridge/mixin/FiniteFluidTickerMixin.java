package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;

@Mixin(com.hypixel.hytale.server.core.asset.type.fluid.FiniteFluidTicker.class)
public abstract class FiniteFluidTickerMixin {

    @Redirect(method = "spread", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/core/universe/world/chunk/section/FluidSection;setFluid(IIIIB)Z"), require = 0)
    private boolean plots$guardSetFluid(FluidSection section, int localX, int localY, int localZ, int fluidId, byte level,
            World world, long gameTick, FluidTicker.Accessor accessor, FluidSection sourceSection, BlockSection blockSection,
            Fluid fluid, int currentFluidId, byte currentLevel, int sourceX, int sourceY, int sourceZ) {
        int toX = (section.getX() << 4) + localX;
        int toY = (section.getY() << 4) + localY;
        int toZ = (section.getZ() << 4) + localZ;
        if (shouldBlockFluidSpread(world, sourceX, sourceY, sourceZ, toX, toY, toZ)) {
            return false;
        }
        return section.setFluid(localX, localY, localZ, fluidId, level);
    }

    private static boolean shouldBlockFluidSpread(World world, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        if (world == null) {
            return false;
        }
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) {
            return false;
        }
        Object hook = map.get("plots.fluid.flow.hook");
        if (hook == null) {
            return false;
        }
        try {
            Method m = hook.getClass().getMethod("shouldBlockFluidSpread", String.class, int.class, int.class, int.class,
                    int.class, int.class, int.class);
            Object result = m.invoke(hook, world.getName(), fromX, fromY, fromZ, toX, toY, toZ);
            return result instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }
}
