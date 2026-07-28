package dev.stoshe.plots.integration.economy;

import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.util.ConsoleColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlotEconomyService {
    private static final DecimalFormat FALLBACK_MONEY = new DecimalFormat("#,##0.00");
    private static final long ASYNC_TIMEOUT_MILLIS = 3000L;

    private final PlotConfig config;
    private String preferredProvider = "auto";

    @Nullable
    private Provider provider;

    public PlotEconomyService(@Nonnull PlotConfig config) {
        this.config = config;
    }

    public void initialize() {
        if (!config.isEconomyEnabled()) {
            provider = null;
            ConsoleColors.info("Economy support is disabled in config.");
            return;
        }

        preferredProvider = config.getEconomyProvider().toLowerCase(Locale.ROOT);
        provider = resolveProvider(preferredProvider);

        if (provider == null) {
            ConsoleColors.warning("Economy enabled, but no compatible provider was found.");
            return;
        }

        ConsoleColors.success("Economy provider active: " + provider.name());
    }

    public boolean isEnabled() {
        ensureProviderAvailable();
        return provider != null;
    }

    public boolean tryWithdraw(@Nonnull UUID playerId, double amount, @Nonnull String reason) {
        if (amount <= 0.0) {
            return true;
        }

        ensureProviderAvailable();
        if (provider == null) {
            return false;
        }

        return provider.withdraw(playerId, amount, reason);
    }

    public boolean has(@Nonnull UUID playerId, double amount) {
        if (amount <= 0.0) {
            return true;
        }

        ensureProviderAvailable();
        if (provider == null) {
            return false;
        }

        return provider.has(playerId, amount);
    }

    @Nonnull
    public String format(double amount) {
        ensureProviderAvailable();
        if (provider == null) {
            return FALLBACK_MONEY.format(amount);
        }

        String formatted = provider.format(amount);
        if (formatted == null || formatted.isBlank()) {
            return FALLBACK_MONEY.format(amount);
        }

        return formatted;
    }

    private synchronized void ensureProviderAvailable() {
        if (!config.isEconomyEnabled() || provider != null) {
            return;
        }

        provider = resolveProvider(preferredProvider);
        if (provider != null) {
            ConsoleColors.success("Economy provider active: " + provider.name());
        }
    }

    @Nullable
    private Provider resolveProvider(@Nonnull String preferred) {
        if (!"auto".equals(preferred)) {
            Provider chosen = createProvider(preferred);
            if (chosen != null) {
                return chosen;
            }
            ConsoleColors.warning("Configured economy provider '" + preferred + "' is unavailable. Falling back to auto.");
        }

        String[] order = new String[] { "eliteessentials", "economysystem", "ecotale", "essentialsplus" };
        for (String id : order) {
            Provider candidate = createProvider(id);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private Provider createProvider(@Nonnull String id) {
        switch (id) {
            case "eliteessentials":
                return buildEliteEssentials();
            case "economysystem":
                return buildEconomySystem();
            case "ecotale":
                return buildEcotale();
            case "essentialsplus":
                return buildEssentialsPlus();
            default:
                return null;
        }
    }

    @Nullable
    private Provider buildEliteEssentials() {
        try {
            Class<?> api = Class.forName("com.eliteessentials.api.EconomyAPI");
            Method isEnabled = api.getMethod("isEnabled");
            if (!Boolean.TRUE.equals(isEnabled.invoke(null))) {
                return null;
            }
            return new Provider("eliteessentials") {
                @Override
                boolean has(UUID uuid, double amount) {
                    return invokeBoolean(api, "has", new Class<?>[] { UUID.class, double.class }, uuid, amount);
                }

                @Override
                boolean withdraw(UUID uuid, double amount, String reason) {
                    return invokeBoolean(api, "withdraw", new Class<?>[] { UUID.class, double.class }, uuid, amount);
                }

                @Override
                String format(double amount) {
                    return invokeString(api, "format", new Class<?>[] { double.class }, amount);
                }
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private Provider buildEconomySystem() {
        try {
            Class<?> api = Class.forName("com.economy.api.EconomyAPI");
            Method getInstance = api.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            if (instance == null) {
                return null;
            }
            return new Provider("economysystem") {
                @Override
                boolean has(UUID uuid, double amount) {
                    return invokeBoolean(api, instance, "hasBalance", new Class<?>[] { UUID.class, double.class }, uuid,
                            amount);
                }

                @Override
                boolean withdraw(UUID uuid, double amount, String reason) {
                    return invokeBoolean(api, instance, "removeBalance", new Class<?>[] { UUID.class, double.class },
                            uuid, amount);
                }

                @Override
                String format(double amount) {
                    return null;
                }
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private Provider buildEcotale() {
        try {
            Class<?> api = Class.forName("com.ecotale.api.EcotaleAPI");
            Method isAvailable = api.getMethod("isAvailable");
            if (!Boolean.TRUE.equals(isAvailable.invoke(null))) {
                return null;
            }
            return new Provider("ecotale") {
                @Override
                boolean has(UUID uuid, double amount) {
                    return invokeBoolean(api, "hasBalance", new Class<?>[] { UUID.class, double.class }, uuid, amount);
                }

                @Override
                boolean withdraw(UUID uuid, double amount, String reason) {
                    return invokeBoolean(api, "withdraw", new Class<?>[] { UUID.class, double.class, String.class },
                            uuid, amount, reason);
                }

                @Override
                String format(double amount) {
                    return invokeString(api, "format", new Class<?>[] { double.class }, amount);
                }
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private Provider buildEssentialsPlus() {
        try {
            Class<?> api = Class.forName("de.fof1092.essentialsplus.economy.EconomyAPI");
            Method isEnabled = api.getMethod("isEnabled");
            if (!Boolean.TRUE.equals(isEnabled.invoke(null))) {
                return null;
            }
            return new Provider("essentialsplus") {
                @Override
                boolean has(UUID uuid, double amount) {
                    Object future = invoke(api, "hasBalance", new Class<?>[] { UUID.class, double.class }, uuid, amount);
                    return awaitBoolean(future);
                }

                @Override
                boolean withdraw(UUID uuid, double amount, String reason) {
                    Object future = invoke(api, "decreaseBalance",
                            new Class<?>[] { UUID.class, double.class, String.class },
                            uuid, amount, reason);
                    return awaitNumber(future) != null;
                }

                @Override
                String format(double amount) {
                    return invokeString(api, "formatCurrency", new Class<?>[] { double.class }, amount);
                }
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private abstract static class Provider {
        private final String name;

        Provider(@Nonnull String name) {
            this.name = name;
        }

        String name() {
            return name;
        }

        abstract boolean has(UUID uuid, double amount);

        abstract boolean withdraw(UUID uuid, double amount, String reason);

        abstract String format(double amount);
    }

    @Nullable
    private static Object invoke(Class<?> type, String methodName, Class<?>[] parameterTypes, Object... args) {
        return invoke(type, null, methodName, parameterTypes, args);
    }

    @Nullable
    private static Object invoke(Class<?> type, Object target, String methodName, Class<?>[] parameterTypes,
            Object... args) {
        try {
            Method method = type.getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeBoolean(Class<?> type, String methodName, Class<?>[] parameterTypes, Object... args) {
        return invokeBoolean(type, null, methodName, parameterTypes, args);
    }

    private static boolean invokeBoolean(Class<?> type, Object target, String methodName, Class<?>[] parameterTypes,
            Object... args) {
        Object value = invoke(type, target, methodName, parameterTypes, args);
        return Boolean.TRUE.equals(value);
    }

    @Nullable
    private static String invokeString(Class<?> type, String methodName, Class<?>[] parameterTypes, Object... args) {
        Object value = invoke(type, null, methodName, parameterTypes, args);
        return value != null ? String.valueOf(value) : null;
    }

    private static boolean awaitBoolean(@Nullable Object candidate) {
        Number number = awaitNumber(candidate);
        if (number != null) {
            return number.doubleValue() > 0.0;
        }

        if (!(candidate instanceof CompletableFuture<?> future)) {
            return false;
        }

        try {
            Object value = future.get(ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static Number awaitNumber(@Nullable Object candidate) {
        if (!(candidate instanceof CompletableFuture<?> future)) {
            return null;
        }

        try {
            Object value = future.get(ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (value instanceof Number number) {
                return number;
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }
}
