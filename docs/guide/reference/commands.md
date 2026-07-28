# Commands

Commands can be executed with `/plot`, `/plots`, `/plotme`, or `/p`. Admin-only commands are marked below.

## Command System Notes

- Root command now has centralized usage/error handling.
- Invalid subcommands return structured usage + available subcommands.
- Command execution failures are logged with full input for easier diagnostics.

## Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/plot claim` | Claims the plot you are standing on. | `plots.claim` |
| `/plot auto` | Automatically claims the nearest free plot. | `plots` |
| `/plot info` | Shows information about the current plot. | `plots.info` |
| `/plot list` | Lists all plots you own. | `plots.list` |
| `/plot rename <name>` | Renames your plot. | `plots.rename` |
| `/plot trust <player>` | Grants a player permission to build on your plot. | `plots.trust` |
| `/plot untrust <player>` | Removes build permission from a player. | `plots.trust` |
| `/plot transfer <player>` | Transfers ownership of your current plot to another player. | `plots.transfer` |
| `/plot flag <set\|remove\|list>` | Manages plot settings and behaviors. | `plots.flag` |
| `/plot f <set\|remove\|list>` | Alias for `flag`. | `plots.flag` |
| `/plot merge <north\|south\|east\|west\|all> [removeRoads]` | Merges your current plot with adjacent plots you also own. | `plots.merge` |
| `/plot link <north\|south\|east\|west\|all> [removeRoads]` | Alias for `merge`. | `plots.merge` |
| `/plot unmerge <north\|south\|east\|west\|all> [createRoad]` | Unmerges your current plot from adjacent merged plots. | `plots.unmerge` |
| `/plot unlink <north\|south\|east\|west\|all> [createRoad]` | Alias for `unmerge`. | `plots.unmerge` |
| `/plot menu` | Opens the plot management menu (UI). | `plots` |
| `/plot warps` | Browse public plot warps. | `plots` |
| `/plot worlds` | Opens a modal listing plot worlds, with teleport buttons. | `plots` |
| `/plot help` | Opens a modal listing the plot commands. | `plots` |
| `/plot spawn [world]` | Teleports you to a plot world's spawn (current/default world if omitted). | `plots.spawn` |
| `/plot confirm` | Confirms a pending plot action. | `plots` |
| `/plot cancel` | Cancels a pending plot action. | `plots` |

## Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/plot admin` | Opens the plot administration dashboard (UI). | `plots.*` |
| `/plot delete [x z]` | Deletes the current plot or one at specific grid coordinates. | `plots.delete` (others: `plots.delete.*`) |
| `/plot setspawn` | Sets the global spawn point for the plot world. | `plots.*` |
| `/plot refresh` | Reloads the plots runtime config and caches. | `plots.*` |
| `/plot debug bridge` | Shows bridge/mixin diagnostic state. | `plots.*` |

::: tip
Admins with `plots.*`, `plots.admin.bypass`, or `plots.admin` bypass all plot protection and can manage any plot on the server.
:::
