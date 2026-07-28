# API Usage & Dependency

Learn how to integrate with Plots and manipulate plot data programmatically.

## Dependency

Add the Plots JAR as a `compileOnly` dependency in your `build.gradle`:

```groovy
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    compileOnly name: 'Plots'
}
```

## Accessing the API

The entry point for all API operations is the `Plots` class.

```java
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.api.PlotsAPI;

// Get the API instance
if (!Plots.enabled() || Plots.getAPI() == null) {
    return;
}
PlotsAPI api = Plots.getAPI();
```

## Querying Plot Data

Use the `PlotManager` for advanced queries.

### Get Plot at Coordinates
```java
import dev.stoshe.plots.model.Plot;

// Use the API for read-only access
Plot plot = Plots.getAPI().getPlotAt(x, z);

if (plot != null) {
    UUID owner = plot.getOwner(); // Updated getter name if changed
}
```

### Get All Plots by Owner
```java
Collection<Plot> myPlots = Plots.getAPI().getPlotsByOwner(playerUuid);
```

## Modifying Plots

You can modify plot properties directly on the `Plot` object, but you must look up the Repository to save changes.

```java
plot.setName("Awesome Area");

// Access the repository to save changes
Plots.getInstance().getPlotRepository().savePlot(plot);
```
