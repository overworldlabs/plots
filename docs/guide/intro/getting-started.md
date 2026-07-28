# Getting Started

Plots is a high-performance plot management system designed specifically for Hytale servers. This guide will help you set up the plugin and create your first plot world.

## Installation

1. Download the latest `plots.jar` from the releases page.
2. Place the JAR file in your Hytale server's `mods` directory (NOT `plugins`).
3. Restart the server.
4. The plugin will create a `mods/Stoshe_Plots` directory with a default `config.json` and translations.

## Optional dependencies

Plots runs on its own — nothing below is required, and none of it is bundled.

| Plugin | What it adds | Without it |
| --- | --- | --- |
| [**TaleGuard**](/guide/integrations/taleguard) | The mixin-only flags: item pickup, mob spawning, keep inventory, explosions, BuilderTools internals | Every other flag is still enforced natively; the bridge-only ones are hidden instead of failing silently |
| [**Hylograms**](/guide/integrations/hylograms) | Floating ownership signs above plots | Holograms are disabled |
| [**PlaceholderAPI**](/guide/integrations/placeholders) | Plot placeholders in scoreboards, tab lists and chat | No placeholders are registered |
| An economy plugin | Charging for claims and merges | Plot actions are free |

TaleGuard goes in `earlyplugins/`, not `mods/` — it has to bootstrap before the classes it patches load.

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
