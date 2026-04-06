package com.overworldlabs.plots.flag.types;

import com.overworldlabs.plots.flag.PlotFlag;
import javax.annotation.Nonnull;
import java.util.Optional;

public class StringFlag extends PlotFlag<String> {
    public StringFlag(@Nonnull String name, @Nonnull String description, @Nonnull String defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public Optional<String> parseValue(@Nonnull String input) {
        return Optional.of(input);
    }
}
