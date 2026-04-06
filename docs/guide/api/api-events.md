# API Events

Plots uses a custom event system to allow developers to react to plot changes. All events extend the base `Event` class and are located in `com.overworldlabs.plots.api.events`.

## Listening to Events

You can register a listener through the Plots API.

```java
import com.overworldlabs.plots.api.events.ClaimEvent;
import com.hypixel.hytale.server.core.universe.Universe;

Plots.getApi().registerListener(ClaimEvent.class, event -> {
    // Your logic here
    System.out.println("Plot claimed: " + event.getPlot().getId());
});
```

## Event Types

### `ClaimEvent`
Fired when a player successfully claims a plot.
- `getClaimerUuid()`: The UUID of the player.
- `getClaimerName()`: The name of the player.

### `UnclaimEvent`
Fired when a plot is unclaimed or deleted.
- `getFormerOwnerUuid()`: The UUID of the previous owner.

### `RenameEvent`
Fired when a plot title or subtitle is updated.
- `getOldName()`: The name before the change.
- `getNewName()`: The new name.

### `TrustEvent`
Fired when a player's trust level is modified.
- `getSubjectUuid()`: The player being trusted/untrusted.
- `isTrusted()`: True if added, false if removed.

## Base Event Methods
Every event includes:
- `getPlot()`: Access the `Plot` model.
- `getTimestamp()`: When the event occurred.
