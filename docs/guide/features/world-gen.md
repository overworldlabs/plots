# World Generation and Smart Prefabs

## World Generation

The plugin uses a custom implementation of `IWorldGenProvider` to generate a repeatable grid of plots.

### World Name Configuration
You can customize the name of the plot world in `config.json` under the `World` section:

```json
{
  "World": {
    "PlotWorldName": "my_plot_world",
    "DefaultWorldTime": "midday"
  }
}
```
If the world does not exist, it will be automatically created with the specified name during plugin startup.

### Grid Logic
The world is divided into regions based on the configuration:
- **Plots** are generated at the start of each grid cell using `PlotSizeX` and `PlotSizeZ`.
- **Roads** fill the space between plots using `RoadSizeX` and `RoadSizeZ`.
- **Intersections** are placed at the crossing of two roads.

### Terrain Layers
- **Bedrock (Y=0):** Prevents falling into the void.
- **Sub-surface (Y=1 to 63):** Filled with the configured `Filling` material.
- **Surface (Y=64):** Uses `PlotSurface` for plots and `RoadSurface` for roads.

---

## World Spawning Logic

The generator uses a prioritized search for the initial world spawn point:

1. **Custom Spawn:** If `CustomSpawn` is set in `config.json`, it is always used.
2. **Safe Floor Search:** If a plot prefab is loaded, the generator searches its center area (6x6 blocks) for the highest solid floor (filtering out foliage like leaves, logs, or flowers).
3. **Roadless / Island Situations:** If no safe floor is found in the prefab, or if roads/intersections are disabled (`RoadSize: 0`), the generator defaults to the corner of the plot grid coordinate. 

::: tip
For roadless "infinite sea" worlds or island prefabs, we highly recommend using `/plot setspawn` to define a pleasant starting point for your players, as the default math may place them at a junction corner.
:::

---

## Smart Prefabs

The smart prefab system automatically handles how your schematic looks in the world using PlotSquared-style insertion logic.

### Automatic Centralization
If your plot prefab is smaller than the grid size (e.g., a 20x20 island in a 32x32 plot), the plugin will calculate the offsets automatically to place it in the center.

### PlotSquared-style Offsets
In `config.json`, you can define manual offsets for your prefabs:

- **`OffsetX` / `OffsetZ`**: Additional horizontal displacement added after centering.
- **`OffsetY`**: Vertical displacement.

- `PasteRoadOnTop`: (boolean) If true, road and intersection prefabs are pasted on top of the ground height (default: true).
- `AutoHeight`: (boolean) If true, use the ground height as baseline for all prefabs (default: true).
- `PasteMismatches`: (boolean) If false, plot prefabs larger than the plot size will be skipped (default: true).
- `Rotation`: (int) Rotation of the plot prefab in degrees (0, 90, 180, 270).
- `OffsetX/Y/Z`: (int) Relative offsets for the plot prefab.

### Example Configuration

```json
"Prefabs": {
  "Road": "stoshe:road_standard",
  "Plot": "stoshe:plot_house_base",
  "Intersection": "stoshe:road_intersection",
  "PasteRoadOnTop": true,
  "AutoHeight": true,
  "PasteMismatches": false,
  "Rotation": 90,
  "OffsetX": 0,
  "OffsetY": -1,
  "OffsetZ": 0
}
```
$Y=64$ as the base.

### Skip-Air Logic
When a prefab contains blocks named "Empty" or "Air", the plugin skips them. This allows the underlying terrain (like plot water) to show through, creating perfect islands.

### Fluid Support
If you place fluids in your prefab, the plugin will correctly register them as Hytale fluids, enabling rendering and physics.
