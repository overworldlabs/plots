package com.overworldlabs.plots.flag;

import javax.annotation.Nonnull;
import java.util.Set;

public final class MixinRequiredFlags {
    private static final Set<String> REQUIRED = Set.of(
            "item-pickup",
            "item-pickup-manual",
            "build",
            "hammer",
            "allowed-cmds",
            "blocked-cmds",
            "seat",
            "mob-spawning",
            "keep-inventory",
            "invincible-items",
            // Enforced only by the ExplosionBlockDamage mixin — no ECS fallback,
            // so it must not be settable without the TaleGuard bridge.
            "explosions");

    private MixinRequiredFlags() {
    }

    public static boolean requiresMixinBridge(@Nonnull PlotFlag<?> flag) {
        return REQUIRED.contains(flag.getName());
    }
}
