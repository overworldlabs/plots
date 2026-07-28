package dev.stoshe.plots.flag.types;

import dev.stoshe.plots.flag.PlotFlag;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * A flag that holds an enum value.
 *
 * @param <E> The enum type
 */
public class EnumFlag<E extends Enum<E>> extends PlotFlag<E> {
    private final Class<E> enumClass;

    public EnumFlag(@Nonnull String name, @Nonnull String description, @Nonnull E defaultValue,
            @Nonnull Class<E> enumClass) {
        super(name, description, defaultValue);
        this.enumClass = enumClass;
    }

    @Override
    public Optional<E> parseValue(@Nonnull String input) {
        try {
            return Optional.of(Enum.valueOf(enumClass, input.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
