package com.overworldlabs.plots.api.events;

import com.overworldlabs.plots.model.Plot;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Event fired when a plot is unclaimed/deleted
 */
public class UnclaimEvent extends Event {
    private final UUID formerOwnerUuid;

    public UnclaimEvent(@Nonnull Plot plot, @Nonnull UUID formerOwnerUuid) {
        super(plot);
        this.formerOwnerUuid = formerOwnerUuid;
    }

    /**
     * Get the UUID of the former owner
     * 
     * @return Former owner UUID
     */
    @Nonnull
    public UUID getFormerOwnerUuid() {
        return formerOwnerUuid;
    }
}
