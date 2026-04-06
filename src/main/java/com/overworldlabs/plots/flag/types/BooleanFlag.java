package com.overworldlabs.plots.flag.types;

import com.overworldlabs.plots.flag.PlotFlag;
import javax.annotation.Nonnull;
import java.util.Optional;

public class BooleanFlag extends PlotFlag<Boolean> {
    public BooleanFlag(@Nonnull String name, @Nonnull String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public Optional<Boolean> parseValue(@Nonnull String input) {
        if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("1")
                || input.equalsIgnoreCase("on")) {
            return Optional.of(true);
        }

        if (input.equalsIgnoreCase("false") || input.equalsIgnoreCase("no") || input.equalsIgnoreCase("0")
                || input.equalsIgnoreCase("off")) {
            return Optional.of(false);
        }

        return Optional.empty();
    }
}
