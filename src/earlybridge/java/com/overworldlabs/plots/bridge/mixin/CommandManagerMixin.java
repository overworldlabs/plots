package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {

    @Redirect(method = "runCommand", at = @At(value = "INVOKE", target = "Lcom/hypixel/hytale/server/core/command/system/AbstractCommand;acceptCall(Lcom/hypixel/hytale/server/core/command/system/CommandSender;Lcom/hypixel/hytale/server/core/command/system/ParserContext;Lcom/hypixel/hytale/server/core/command/system/ParseResult;)Ljava/util/concurrent/CompletableFuture;"), require = 0)
    private CompletableFuture<Void> plots$guardCommand(AbstractCommand command, CommandSender sender,
            ParserContext context, ParseResult parseResult, CommandSender senderArg, String rawCommand,
            AbstractCommand commandArg, CompletableFuture<Void> completionFuture) {
        if (shouldBlockCommand(sender, rawCommand)) {
            String deny = getCommandDenialMessage();
            if (deny == null || deny.isBlank()) {
                deny = "You don't have permission to do this.";
            }
            sender.sendMessage(Message.raw(deny));
            return CompletableFuture.completedFuture(null);
        }
        return command.acceptCall(sender, context, parseResult);
    }

    private static boolean shouldBlockCommand(CommandSender sender, String rawCommand) {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) return false;
        Object hook = map.get("plots.command.hook");
        if (hook == null) return false;
        try {
            Method m = hook.getClass().getMethod("shouldBlockCommand", CommandSender.class, String.class);
            Object r = m.invoke(hook, sender, rawCommand != null ? rawCommand : "");
            return r instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String getCommandDenialMessage() {
        Object raw = System.getProperties().get("plots.hook.registry");
        if (!(raw instanceof Map<?, ?> map)) return null;
        Object hook = map.get("plots.command.hook");
        if (hook == null) return null;
        try {
            Method m = hook.getClass().getMethod("getDenialMessage");
            Object r = m.invoke(hook);
            return r != null ? String.valueOf(r) : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
