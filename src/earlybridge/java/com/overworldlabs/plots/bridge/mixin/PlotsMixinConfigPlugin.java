package com.overworldlabs.plots.bridge.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class PlotsMixinConfigPlugin implements IMixinConfigPlugin {
    private static final Set<String> BLOCKED_BY_DEFAULT = Set.of();

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simple = mixinClassName;
        int idx = mixinClassName.lastIndexOf('.');
        if (idx >= 0 && idx + 1 < mixinClassName.length()) {
            simple = mixinClassName.substring(idx + 1);
        }
        String disableProp = "plots.mixin.bridge.disable." + simple;
        if (Boolean.parseBoolean(System.getProperty(disableProp, "false"))) {
            return false;
        }
        if (BLOCKED_BY_DEFAULT.isEmpty()) {
            return true;
        }
        if (!BLOCKED_BY_DEFAULT.contains(simple)) {
            return true;
        }
        String enableProp = "plots.mixin.bridge.enable." + simple;
        return Boolean.parseBoolean(System.getProperty(enableProp, "false"));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
