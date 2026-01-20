package com.overworldlabs.plots.api.events;

import com.overworldlabs.plots.model.Plot;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Event fired when a plot is claimed by a player
 */
public class ClaimEvent extends Event {
    private final UUID claimerUuid;
    private final String claimerName;

    public ClaimEvent(@Nonnull Plot plot, @Nonnull UUID claimerUuid, @Nonnull String claimerName) {
        super(plot);
        this.claimerUuid = claimerUuid;
        this.claimerName = claimerName;
    }

    /**
     * Get the UUID of the player who claimed the plot
     * 
     * @return Player UUID
     */
    @Nonnull
    public UUID getClaimerUuid() {
        return claimerUuid;
    }

    /**
     * Get the name of the player who claimed the plot
     * 
     * @return Player name
     */
    @Nonnull
    public String getClaimerName() {
        return claimerName;
    }
}
