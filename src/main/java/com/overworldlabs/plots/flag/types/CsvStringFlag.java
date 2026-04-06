package com.overworldlabs.plots.flag.types;

import com.overworldlabs.plots.flag.PlotFlag;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Optional;

/**
 * String flag that accepts comma-separated command tokens.
 */
public class CsvStringFlag extends PlotFlag<String> {
    public CsvStringFlag(@Nonnull String name, @Nonnull String description, @Nonnull String defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public Optional<String> parseValue(@Nonnull String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        String[] parts = normalized.split(",");
        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) {
                return Optional.empty();
            }
            // command root token only (no slash, no spaces), ex: spawn, home, msg, plot:spawn
            if (!token.matches("[a-z0-9:_-]+")) {
                return Optional.empty();
            }
        }

        return Optional.of(String.join(",", java.util.Arrays.stream(parts).map(String::trim).toList()));
    }
}
