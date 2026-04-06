package com.overworldlabs.plots.flag.types;

import com.overworldlabs.plots.flag.PlotFlag;
import javax.annotation.Nonnull;
import java.util.Optional;

public class IntegerFlag extends PlotFlag<Integer> {
    public IntegerFlag(@Nonnull String name, @Nonnull String description, int defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public Optional<Integer> parseValue(@Nonnull String input) {
        try {
            return Optional.of(Integer.parseInt(input));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
