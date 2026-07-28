# Configuration and Permissions

::: warning Breaking change — plugin Group renamed to `Stoshe`
Starting with this version, the plugin manifest **Group** changed from `Hytale` to `Stoshe`.
Because the plugin's data directory is derived from the Group, it moved from
`mods/Hytale_Plots/` to `mods/Stoshe_Plots/`.

As a result, existing plot worlds and configuration are **not** picked up automatically.
It is **recommended to recreate the plot world and the plugin configs** after updating
(or manually migrate your old data into the new `mods/Stoshe_Plots/` directory).
:::

## Layout

The `config.json` file is automatically generated in the `mods/Stoshe_Plots/` directory
upon the first run. It is split into **global** sections and a `Worlds` map, where each
entry is a fully independent plot world:

```json
{
  "General":   { "Language": "en_us", "AutoSaveIntervalSeconds": 300 },
  "Limits":    { "GlobalSharedDefault": -1, "PerWorldDefault": 1, "HardCap": 50 },
  "Holograms": { "Enabled": true, "HeightOffset": 2.0, "TitleColor": "#55ff55" },
  "Database":  { "Enabled": false, "JdbcUrl": "jdbc:sqlite:plots.db" },
  "Economy":   { "Enabled": false, "Provider": "auto" },
  "Worlds": {
    "plotworld": {
      "PermissionKey": "plotworld",
      "DefaultWorldTime": "midday",
      "MaxPlotsDefault": -1,
      "Plots":   { "PlotSizeX": 32, "PlotSizeZ": 32, "RoadSizeX": 4, "RoadSizeZ": 4 },
      "Blocks":  { "PlotSurface": "Soil_Grass", "RoadSurface": "Rock_Stone_Cobble" },
      "Prefabs": { "Plot": "", "Road": "", "Intersection": "" },
      "Spawn":   { "CustomSpawn": false }
    },
    "premium": {
      "MaxPlotsDefault": 10,
      "Plots": { "PlotSizeX": 64, "PlotSizeZ": 64, "RoadSizeX": 6, "RoadSizeZ": 6 }
    }
  }
}
```

::: tip Adding a world
Add another entry under `Worlds` and restart — the plugin creates the world with its
own generator on boot. The **first** world in the map is the default world used by
legacy/single-world data and API calls.
:::

> Migrating from a single-world config? Old flat `World`/`Plots`/`Blocks`/`Prefabs`/`Spawn`
> sections are automatically folded into one world entry on first load.

## Global Settings

### General
- **`Language`**: Default translation language (e.g. `en_us`, `pt_br`).
- **`AutoSaveIntervalSeconds`**: How often plot data is flushed to storage.

### Limits
Per-player plot limits use a **hybrid** model — see [Plot Limits](../reference/permissions#plot-limits-hybrid-model).
- **`GlobalSharedDefault`**: Default total plots a player may own across **all** worlds (`-1` = unlimited).
- **`PerWorldDefault`**: Default plots a player may own **per world** when the world doesn't override it.
- **`HardCap`**: Absolute server ceiling on total plots a non-admin may own (`-1` = unlimited; `0` blocks all claims).

## Per-World Settings (`Worlds`)

Each entry under `Worlds` is keyed by the world name and accepts:

- **`PermissionKey`**: Segment used in `plots.<key>.limit.N` permissions (defaults to the sanitized world name).
- **`DefaultWorldTime`**: Time preset for the world (`midday`, `night`, `dawn`, a fraction like `0.5`, or an hour).
- **`MaxPlotsDefault`**: Per-world plot budget (`-1` = inherit `Limits.PerWorldDefault`).

### Plots
- **`PlotSizeX`, `PlotSizeZ`**: The actual buildable area for each plot.
- **`RoadSizeX`, `RoadSizeZ`**: The width of the roads separating plots. Set to `0` for no roads (adjacent plots).
- **`MaxWarpsPerPlot`**: Maximum named warps per plot.
- **`DefaultPlotName`**: Template for new plot names (`%owner%`, `%x%`, `%z%`).

### Blocks
Customizing these IDs changes the look of the generated world without changing prefabs:
- **`Bedrock`**: The bottom layer.
- **`PlotSurface`**: The top layer of the builder's area.
- **`PlotSubSurface`**: The material directly under the surface.
- **`RoadSurface`**: The material used for roads.
- **`Border`**: The block used for the plot perimeter.
- **`Filling`**: The mass material from Y=1 to Y=60.

### Prefabs
- **`Road`**: The prefab name for roads.
- **`Plot`**: The prefab name for plot areas.
- **`Intersection`**: The prefab name for crossroads.
- **`PasteRoadOnTop`**: If `true`, road and intersection prefabs are pasted on top of the ground height (default: true).
- **`AutoHeight`**: If `true`, use the ground height (Y=64) as the baseline for all prefabs.
- **`PasteMismatches`**: If `false`, plot prefabs larger than the plot size will be skipped.
- **`Rotation`**: Rotation of the plot prefab in degrees (0, 90, 180, 270).
- **`OffsetX`, `OffsetY`, `OffsetZ`**: Relative offsets for the plot prefab application.

### Spawn
- **`X`, `Y`, `Z`, `Pitch`, `Yaw`**: World coordinates for this world's spawn point.
- **`CustomSpawn`**: If `true`, the plugin always uses these coordinates for the world spawn.

## Other Global Settings

### Hologram Settings
> [!IMPORTANT]
> These settings require the [Hylograms Integration](../integrations/hylograms) to be enabled and the Hylograms mod to be installed on the server.

- **`Enabled`**: Whether to show plot info holograms at the border.
- **`HeightOffset`**: How many blocks above the ground the hologram appears.
- **`TitleColor`**: Hex color code for the plot owner's name.

### Database Settings
- **`Enabled`**: Enables SQL persistence.
- **`JdbcUrl`**: JDBC connection string.
  Supported backends: SQLite, MySQL, MariaDB, PostgreSQL.
  Examples:
  - `jdbc:sqlite:plots.db`
  - `jdbc:mysql://127.0.0.1:3306/plots`
  - `jdbc:mariadb://127.0.0.1:3306/plots`
  - `jdbc:postgresql://127.0.0.1:5432/plots`
- **`Username` / `Password`**: Credentials for SQL backends that require auth.
- **`MaxPoolSize`**: Maximum number of SQL connections.

### Economy Settings
- **`Enabled`**: Enables plot action costs.
- **`Provider`**: Economy plugin to use (`auto`, `eliteessentials`, `economysystem`, `ecotale`, `essentialsplus`).
- **`Costs.Claim`**: Cost to claim a plot with `/plot claim`.
- **`Costs.AutoClaim`**: Cost to claim with `/plot auto`.
- **`Costs.Merge`**: Cost per merge operation.
- **`Costs.Unmerge`**: Cost per unmerge operation.

> [!TIP]
> Economy costs can be bypassed with the `plots.economy.bypass` permission.
> Provider resolution is retried at runtime, so late-loading economy plugins are supported more reliably.

---

## Permissions

For a comprehensive list of permissions, please refer to the [Permissions](../reference/permissions) page.
