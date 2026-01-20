package com.overworldlabs.plots.api.events;

import com.overworldlabs.plots.model.Plot;

import javax.annotation.Nonnull;

/**
 * Base class for all plot-related events
 */
public abstract class Event {
    private final Plot plot;
    private final long timestamp;

    public Event(@Nonnull Plot plot) {
        this.plot = plot;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get the plot associated with this event
     * 
     * @return The plot
     */
    @Nonnull
    public Plot getPlot() {
        return plot;
    }

    /**
     * Get the timestamp when this event occurred
     * 
     * @return Timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
}
