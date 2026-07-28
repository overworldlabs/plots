package dev.stoshe.plots.util;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.UUID;

public final class PlayerIdentity {
    private static final Field UUID_FIELD;

    static {
        try {
            UUID_FIELD = PlayerRef.class.getDeclaredField("uuid");
            UUID_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private PlayerIdentity() {
    }

    @Nonnull
    public static UUID uuid(@Nonnull PlayerRef playerRef) {
        try {
            Object value = UUID_FIELD.get(playerRef);
            if (value instanceof UUID uuid) {
                return uuid;
            }
        } catch (IllegalAccessException ignored) {
        }

        throw new IllegalStateException("Unable to resolve PlayerRef UUID");
    }
}
