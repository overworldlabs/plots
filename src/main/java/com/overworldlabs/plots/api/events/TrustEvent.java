package com.overworldlabs.plots.api.events;

import com.overworldlabs.plots.model.Plot;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Event fired when a player is trusted or untrusted in a plot
 */
public class TrustEvent extends Event {
    private final UUID playerUuid;
    private final boolean trusted;

    /**
     * Create a new PlotTrustEvent
     * 
     * @param plot       Plot where trust was modified
     * @param playerUuid UUID of the player
     * @param trusted    true if player was trusted, false if untrusted
     */
    public TrustEvent(@Nonnull Plot plot, @Nonnull UUID playerUuid, boolean trusted) {
        super(plot);
        this.playerUuid = playerUuid;
        this.trusted = trusted;
    }

    /**
     * Get the UUID of the player who was trusted/untrusted
     * 
     * @return Player UUID
     */
    @Nonnull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Check if the player was trusted or untrusted
     * 
     * @return true if player was trusted, false if untrusted
     */
    public boolean isTrusted() {
        return trusted;
    }
}
