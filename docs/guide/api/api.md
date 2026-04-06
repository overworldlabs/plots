# Developer API Overview

Welcome to the Plots Developer API. This API allows you to extend the functionality of the plot system, listen to important events, and interact with plot data programmatically.

## How to use this Guide

To get started, follow these sections:

- **[Usage & Dependency](./api-usage)**: How to add Plots to your project and access core managers.
- **[API Events](./api-events)**: A complete list of events you can listen to.

## Quick Example

```java
import com.overworldlabs.plots.Plots;
import com.overworldlabs.plots.api.events.ClaimEvent;

public void onEnable() {
    if (!Plots.enabled() || Plots.getAPI() == null) {
        return;
    }
    Plots.getAPI().onPlotClaim(event -> {
        // Handle plot claim
        System.out.println("Plot claimed by: " + event.getOwnerName());
    });
}
```

> [!TIP]
> Always check for `null` when retrieving plots, as players might be in non-plot worlds or on roads.

## WorldGen Override Hook

External plugins can replace Plots' default world generator:

```java
Plots.getAPI().setWorldGenOverride((context, tintArgb) -> {
    // Return your custom IWorldGen (or null to keep default)
    return myCustomGenerator;
});
```

To restore default behavior:

```java
Plots.getAPI().clearWorldGenOverride();
```
