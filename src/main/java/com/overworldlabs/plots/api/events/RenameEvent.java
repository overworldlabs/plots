package com.overworldlabs.plots.api.events;

import com.overworldlabs.plots.model.Plot;

import javax.annotation.Nonnull;

/**
 * Event fired when a plot is renamed
 */
public class RenameEvent extends Event {
    private final String oldName;
    private final String newName;

    public RenameEvent(@Nonnull Plot plot, @Nonnull String oldName, @Nonnull String newName) {
        super(plot);
        this.oldName = oldName;
        this.newName = newName;
    }

    /**
     * Get the old name of the plot
     * 
     * @return Old plot name
     */
    @Nonnull
    public String getOldName() {
        return oldName;
    }

    /**
     * Get the new name of the plot
     * 
     * @return New plot name
     */
    @Nonnull
    public String getNewName() {
        return newName;
    }
}
