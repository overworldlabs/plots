package com.overworldlabs.plots.util;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

public final class EventCancelUtil {
    private EventCancelUtil() {
    }

    public static void forceCancel(@Nonnull Object event) {
        tryInvokeNoArg(event, "setCancelled", true);
        tryInvokeNoArg(event, "setCanceled", true);
        tryInvokeNoArg(event, "cancel");
        tryInvokeNoArg(event, "setCancel", true);
    }

    private static void tryInvokeNoArg(@Nonnull Object target, @Nonnull String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            m.invoke(target);
        } catch (Exception ignored) {
        }
    }

    private static void tryInvokeNoArg(@Nonnull Object target, @Nonnull String methodName, boolean arg) {
        try {
            Method m = target.getClass().getMethod(methodName, boolean.class);
            m.invoke(target, arg);
        } catch (Exception ignored) {
        }
    }
}
