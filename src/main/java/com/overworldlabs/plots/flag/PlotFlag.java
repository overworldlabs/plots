package com.overworldlabs.plots.flag;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Abstract base class for all plot flags.
 *
 * @param <T> The type of value this flag holds
 */
public abstract class PlotFlag<T> {
    private final String name;
    private final String description;
    private final T defaultValue;

    public PlotFlag(@Nonnull String name, @Nonnull String description, @Nonnull T defaultValue) {
        this.name = name.toLowerCase();
        this.description = description;
        this.defaultValue = defaultValue;
    }

    @Nonnull
    @SuppressWarnings("null")
    public String getName() {
        return name;
    }

    @Nonnull
    @SuppressWarnings("null")
    public String getDescription() {
        return description;
    }

    @Nonnull
    @SuppressWarnings("null")
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Parses a string input into the flag's value type.
     *
     * @param input The string input from a command or config
     * @return An Optional containing the parsed value, or empty if parsing failed
     */
    public abstract Optional<T> parseValue(@Nonnull String input);

    /**
     * Serializes the flag value to a string for storage.
     *
     * @param value The value to serialize
     * @return The string representation
     */
    @Nonnull
    @SuppressWarnings("null")
    public String serializeValue(@Nonnull T value) {
        return value.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}
