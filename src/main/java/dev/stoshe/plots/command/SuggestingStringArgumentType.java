package dev.stoshe.plots.command;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * A plain string argument that feeds the in-game tab-completion with a fixed or
 * dynamic set of choices.
 *
 * <p>It parses exactly like {@link com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes#STRING}
 * (the raw token is returned untouched, never rejected), so it is a drop-in
 * replacement anywhere a string argument is declared — required or optional.
 * The only added behaviour is {@link #suggest}, which the engine calls to drive
 * autocomplete. Because validation is unchanged, swapping a {@code STRING}
 * argument for this type cannot change command execution, only the suggestions.
 */
public final class SuggestingStringArgumentType extends SingleArgumentType<String> {
    private final Supplier<? extends Collection<String>> choices;

    public SuggestingStringArgumentType(@Nonnull String name, @Nonnull String description,
            @Nonnull Supplier<? extends Collection<String>> choices) {
        super(name, description);
        this.choices = choices;
    }

    /**
     * Creates a suggestion argument backed by a fixed list of choices.
     *
     * @param name        the argument-type name (shown in usage)
     * @param description  a short human description of the argument
     * @param choices      the fixed suggestion values
     * @return the suggesting argument type
     */
    @Nonnull
    public static SuggestingStringArgumentType of(@Nonnull String name, @Nonnull String description,
            @Nonnull String... choices) {
        List<String> fixed = List.of(choices);
        return new SuggestingStringArgumentType(name, description, () -> fixed);
    }

    @Override
    public String parse(@Nonnull String raw, @Nonnull ParseResult result) {
        return raw;
    }

    @Override
    public void suggest(@Nonnull CommandSender sender, String input, int cursor,
            @Nonnull SuggestionResult result) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        for (String choice : choices.get()) {
            if (choice != null && choice.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.suggest(choice);
            }
        }
    }
}
