package dev.stoshe.plots.flag.types;

import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import dev.stoshe.plots.flag.PlotFlag;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * String flag that validates against weather assets available in the runtime
 * namespace.
 */
public class WeatherAssetFlag extends PlotFlag<String> {
    public WeatherAssetFlag(@Nonnull String name, @Nonnull String description, @Nonnull String defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public Optional<String> parseValue(@Nonnull String input) {
        String raw = input.trim();
        if (raw.isEmpty()) {
            return Optional.empty();
        }

        try {
            var map = Weather.getAssetMap();
            if (map == null) {
                return Optional.empty();
            }
            Map<String, Weather> assets = map.getAssetMap();
            if (assets == null || assets.isEmpty()) {
                return Optional.empty();
            }

            if (assets.containsKey(raw)) {
                return Optional.of(raw);
            }

            String wanted = raw.toLowerCase(Locale.ROOT);
            for (String key : assets.keySet()) {
                if (key != null && key.toLowerCase(Locale.ROOT).equals(wanted)) {
                    return Optional.of(key);
                }
            }
            return Optional.empty();
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    @Nonnull
    public String buildAllowedValuesHint(int maxItems) {
        try {
            var map = Weather.getAssetMap();
            if (map == null || map.getAssetMap() == null || map.getAssetMap().isEmpty()) {
                return "weather asset id";
            }

            var keys = map.getAssetMap().keySet().stream()
                    .filter(k -> k != null && !k.isBlank())
                    .sorted(String::compareToIgnoreCase)
                    .collect(Collectors.toList());

            if (keys.isEmpty()) {
                return "weather asset id";
            }

            int limit = Math.max(1, maxItems);
            String joined = keys.stream().limit(limit).collect(Collectors.joining(", "));
            if (keys.size() > limit) {
                return joined + ", ...";
            }
            return joined;
        } catch (Throwable ignored) {
            return "weather asset id";
        }
    }
}
