package dev.stoshe.plots.util;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import dev.stoshe.plots.Plots;

import javax.annotation.Nonnull;

/**
 * Shared "is the plugin set up?" check and the setup reminder message. Used both
 * by the periodic admin nudge and by the command gate that blocks normal
 * commands until at least one plot world exists.
 */
public final class PlotSetup {

    private PlotSetup() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * @return {@code true} when at least one managed plot world is loaded
     */
    public static boolean isReady() {
        Plots plugin = Plots.getInstance();
        return plugin != null && plugin.getWorldManager() != null && plugin.getWorldManager().hasLoadedPlotWorld();
    }

    /**
     * Sends the setup reminder (same message used by the periodic nudge) to a
     * recipient. Admins are told how to start the configuration; everyone else is
     * pointed at an admin, since they cannot set the world up themselves.
     *
     * @param recipient the command sender / player to message
     */
    public static void sendReminder(@Nonnull CommandSender recipient) {
        if (PermissionUtil.hasAdminPermission(recipient)) {
            recipient.sendMessage(ChatUtil.colorize(
                    "{#ffaa00}[Plots] {#ffffff}No plot world is set up yet."));
            recipient.sendMessage(ChatUtil.colorize(
                    "{#ffaa00}[Plots] {#ffffff}Open {#55ffff}/plot admin{#ffffff} to create and configure a plot world."));
            return;
        }
        recipient.sendMessage(ChatUtil.colorize(
                "{#ffaa00}[Plots] {#ffffff}Plots is not configured yet. Please contact an admin."));
    }
}
