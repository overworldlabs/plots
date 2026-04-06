package com.overworldlabs.plots.command;

import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public final class CommandArgs {
    private CommandArgs() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    @SuppressWarnings("unchecked")
    public static <T> RequiredArg<T> required(@Nonnull CommandBase command, @Nonnull String name,
            @Nonnull String description, @Nonnull ArgumentType<T> type) {
        return (RequiredArg<T>) command.withRequiredArg(name, description, type);
    }

    @SuppressWarnings("unchecked")
    public static <T> OptionalArg<T> optional(@Nonnull CommandBase command, @Nonnull String name,
            @Nonnull String description, @Nonnull ArgumentType<T> type) {
        return (OptionalArg<T>) command.withOptionalArg(name, description, type);
    }
}
