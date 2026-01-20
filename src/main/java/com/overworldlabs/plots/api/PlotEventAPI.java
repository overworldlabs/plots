package com.overworldlabs.plots.api;

import com.overworldlabs.plots.api.events.*;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * API for registering event listeners for plot events.
 * Allows other plugins to react to plot changes.
 */
public interface PlotEventAPI {

    /**
     * Register a listener for plot claim events
     * 
     * @param listener Consumer that will be called when a plot is claimed
     * @return Listener ID for unregistering later
     */
    @Nonnull
    String onPlotClaim(@Nonnull Consumer<ClaimEvent> listener);

    /**
     * Register a listener for plot unclaim events
     * 
     * @param listener Consumer that will be called when a plot is unclaimed
     * @return Listener ID for unregistering later
     */
    @Nonnull
    String onPlotUnclaim(@Nonnull Consumer<UnclaimEvent> listener);

    /**
     * Register a listener for plot rename events
     * 
     * @param listener Consumer that will be called when a plot is renamed
     * @return Listener ID for unregistering later
     */
    @Nonnull
    String onPlotRename(@Nonnull Consumer<RenameEvent> listener);

    /**
     * Register a listener for plot trust events
     * 
     * @param listener Consumer that will be called when a player is
     *                 trusted/untrusted
     * @return Listener ID for unregistering later
     */
    @Nonnull
    String onPlotTrust(@Nonnull Consumer<TrustEvent> listener);

    /**
     * Unregister a listener by its ID
     * 
     * @param listenerId ID returned when registering the listener
     * @return true if the listener was found and removed
     */
    boolean unregisterListener(@Nonnull String listenerId);

    /**
     * Unregister all listeners
     */
    void unregisterAllListeners();
}
