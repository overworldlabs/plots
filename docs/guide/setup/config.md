# Configuration and Permissions

## General Settings

The `config.json` file is automatically generated in the `mods/Hytale_Plots/` directory upon the first run.

### World Settings
- **`PlotWorldName`**: The specific world where plot rules and generation apply.
- **`DefaultWorldTime`**: The standard time (in ticks) for the plot world (e.g., `6000` for noon).

### Plot Settings
- **`PlotSizeX`, `PlotSizeZ`**: The actual buildable area for each plot.
- **`RoadSizeX`, `RoadSizeZ`**: The width of the roads separating plots. Set to `0` if you want no roads (plots will be adjacent).
- **`MaxPlotsDefault`**: Number of plots a player can claim by default.
- **`MaxPlotLimit`**: Absolute maximum plots a player can have (regardless of permissions).

### Block Settings
Customizing these IDs allows you to change the look of the generated world without changing prefabs:
- **`Bedrock`**: The bottom layer.
- **`PlotSurface`**: The top layer of the builder's area.
- **`PlotSubSurface`**: The material directly under the surface.
- **`RoadSurface`**: The material used for roads.
- **`Border`**: The block used for the plot perimeter.
- **`Filling`**: The mass material from Y=1 to Y=60.

### Prefab Settings
- **`Road`**: The prefab name for roads.
- **`Plot`**: The prefab name for plot areas.
- **`Intersection`**: The prefab name for crossroads.
- **`PasteRoadOnTop`**: If `true`, road and intersection prefabs are pasted on top of the ground height (default: true).
- **`AutoHeight`**: If `true`, use the ground height (Y=64) as the baseline for all prefabs.
- **`PasteMismatches`**: If `false`, plot prefabs larger than the plot size will be skipped.
- **`Rotation`**: Rotation of the plot prefab in degrees (0, 90, 180, 270).
- **`OffsetX`, `OffsetY`, `OffsetZ`**: Relative offsets for the plot prefab application.

### Hologram Settings
> [!IMPORTANT]
> These settings require the [Hylograms Integration](../integrations/hylograms) to be enabled and the Hylograms mod to be installed on the server.

- **`Enabled`**: Whether to show plot info holograms at the border.
- **`HeightOffset`**: How many blocks above the ground the hologram appears.
- **`TitleColor`**: Hex color code for the plot owner's name.

### Spawn Settings
- **`X`, `Y`, `Z`, `Pitch`, `Yaw`**: World coordinates for the default spawn point.
- **`CustomSpawn`**: If `true`, the plugin will always use these coordinates for the world spawn.

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
