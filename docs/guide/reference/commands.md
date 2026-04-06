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
| `/plot auto` | Automatically claims the nearest free plot. | `plots.auto` |
| `/plot info` | Shows information about the current plot. | `plots.info` |
| `/plot list` | Lists all plots you own. | `plots.list` |
| `/plot rename <name>` | Renames your plot. | `plots.rename` |
| `/plot trust <player>` | Grants a player permission to build on your plot. | `plots.trust` |
| `/plot untrust <player>` | Removes build permission from a player. | `plots.untrust` |
| `/plot flag <set/remove/list>` | Manages plot settings and behaviors. | `plots.flag` |
| `/plot f <set/remove/list>` | Alias for `flag`. | `plots.flag` |
| `/plot merge <north|south|east|west|all>` | Merges your current plot with adjacent plots you also own. | `plots.merge` |
| `/plot link <north|south|east|west|all>` | Alias for `merge`. | `plots.merge` |
| `/plot unmerge <north|south|east|west|all>` | Unmerges your current plot from adjacent merged plots. | `plots.unmerge` |
| `/plot unlink <north|south|east|west|all>` | Alias for `unmerge`. | `plots.unmerge` |
| `/plot spawn` | Teleports you to the plot world spawn. | `plots.spawn` |

## Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/plot delete [x,z]` | Deletes the current plot or one at specific coordinates. | `plots.*` |
| `/plot setspawn` | Sets the global spawn point for the plot world. | `plots.*` |

::: tip
Admins with `plots.*`, `plots.admin.bypass`, or `plots.admin` bypass all plot protection and can manage any plot on the server.
:::
