# Hylograms Integration

Plots seamlessly integrates with [Hylograms](https://www.curseforge.com/hytale/mods/hylograms) to provide beautiful, non-intrusive floating displays for plot information.

## Features

- **Owner Displays:** Show the plot owner's name and skin head above the plot.
- **Dynamic Updates:** Holograms update instantly when a plot is renamed, sold, or unclaimed.
- **Instant Recognition:** Players can quickly identify claimed territory and ownership at a glance.
- **Compatibility Fallbacks:** Supports current Hylograms API entrypoints and compatibility layers.

## Configuration

To enable holograms, ensure the `Hylograms` plugin is installed on your server. Plots will automatically detect it and start spawning markers.

Plots is resilient when Hylograms is not present or when API package names change between versions. In those cases, holograms are disabled gracefully instead of crashing command execution.

Check the `config.json` under `Holograms`:

```json
{
  "Holograms": {
    "Enabled": true,
    "HeightOffset": 2.0,
    "TitleColor": "#55ff55"
  }
}
```
