# Getting Started

Plots is a high-performance plot management system designed specifically for Hytale servers. This guide will help you set up the plugin and create your first plot world.

## Installation

1. Download the latest `plots.jar` from the releases page.
2. Place the JAR file in your Hytale server's `mods` directory (NOT `plugins`).
3. Restart the server.
4. The plugin will create a `mods/Stoshe_Plots` directory with a default `config.json` and translations.

## Configuration

Before generating your world, you can customize the plot dimensions and materials in the `config.json`.

```json
{
  "Plots": {
    "PlotSizeX": 32,
    "PlotSizeZ": 32,
    "RoadSizeX": 4,
    "RoadSizeZ": 4
  }
}
```

## Creating a Plot World

Plots registers its own world generator in Hytale. You can create a world using this generator via the Hytale world management system:

1. Configure your world to use the `Plots` generator.
2. Join the world.
3. Use `/plot auto` or `/plot claim` to start building!

::: info
If you are using the plugin for the first time, check out the [Commands](/guide/reference/commands) page to see what you can do.
:::
