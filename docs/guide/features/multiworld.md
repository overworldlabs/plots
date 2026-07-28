# Multi-World

Plots supports **multiple independent plot worlds**. Each world has its own plot
and road sizes, blocks, prefabs, spawn, time, and plot limits — all configured
under the `Worlds` map in `config.json`.

## Adding a world

You can create a world two ways:

- **In-game (admin panel):** run `/plot admin`, open the **Worlds** tab, and use
  **Create World**. The world is generated immediately with default sizes; tune it
  afterwards in `config.json`. The same tab lets you **delete** a world (its plots
  are removed; the loaded world is unloaded on the next restart).
- **Config:** add an entry under `Worlds` keyed by the world name and restart.

::: warning First-time setup
Until at least one plot world is loaded, normal commands are blocked and admins are
periodically reminded (in chat) to set one up via `/plot admin`. Setup/diagnostic
commands (`admin`, `refresh`, `debug`, `help`) stay available.
:::

A `config.json` `Worlds` entry looks like:

```json
"Worlds": {
  "plotworld": {
    "Plots": { "PlotSizeX": 32, "PlotSizeZ": 32, "RoadSizeX": 4, "RoadSizeZ": 4 }
  },
  "premium": {
    "MaxPlotsDefault": 10,
    "Plots":  { "PlotSizeX": 64, "PlotSizeZ": 64, "RoadSizeX": 6, "RoadSizeZ": 6 },
    "Blocks": { "PlotSurface": "Soil_Grass" }
  }
}
```

See [Configuration](../setup/config#per-world-settings-worlds) for every per-world field.

::: info Default world
The **first** world in the `Worlds` map is the *default world*. Legacy
single-world data and API calls that don't specify a world resolve to it. Existing
single-world configs are migrated into one world entry automatically on first load.
:::

## Plot limits across worlds

Limits are **hybrid**: a player must satisfy a per-world budget **and** a global
shared budget **and** an absolute hard cap. This lets you, for example, allow
3 plots per world but no more than 5 in total. The full resolution order and the
`plots.<world>.limit.N` / `plots.limit.N` permissions are documented in
[Plot Limits](../reference/permissions#plot-limits-hybrid-model).

## Claiming and navigation

- `/plot claim` and `/plot auto` operate in the **world the player is standing in**
  (auto-claim falls back to the default world from an unmanaged world).
- Each world keeps its own plot grid, spawn, holograms and map markers.

::: warning Island-style worlds
Plots ships a grid generator. An "island"-style plot world is provided by an
external plugin (e.g. skyblock) that registers a world-gen override through the
[Developer API](../api/api); Plots itself does not generate islands.
:::
