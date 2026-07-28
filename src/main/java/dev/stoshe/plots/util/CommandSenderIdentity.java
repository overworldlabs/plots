package dev.stoshe.plots.util;

import com.hypixel.hytale.server.core.command.system.CommandSender;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class CommandSenderIdentity {
    private CommandSenderIdentity() {
    }

    @Nonnull
    public static UUID uuid(@Nonnull CommandSender sender) {
        try {
            Method method = sender.getClass().getMethod("getUuid");
            Object value = method.invoke(sender);
            if (value instanceof UUID uuid) {
                return uuid;
            }
        } catch (Exception ignored) {
        }

        try {
            Field uuidField = sender.getClass().getDeclaredField("uuid");
            uuidField.setAccessible(true);
            Object value = uuidField.get(sender);

            if (value instanceof UUID uuid) {
                return uuid;
            }
        } catch (Exception ignored) {
        }

        throw new IllegalStateException("Unable to resolve CommandSender UUID");
    }
}
