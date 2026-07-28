# Placeholders

Plots integrates with the **PlaceholderAPI** plugin, exposing plot information
under the `plots` identifier. Placeholders use the form `%plots_<name>%` and can
be used anywhere the PlaceholderAPI is resolved (scoreboards, chat formats, holograms, etc.).

::: tip Optional integration
The integration is **optional** and self-contained: if the PlaceholderAPI plugin
isn't installed, Plots simply skips registering the expansion — nothing breaks.
The expansion re-registers automatically if the PlaceholderAPI loads or reloads
after Plots.
:::

## Current plot

These resolve against the plot the player is currently standing on (`none`/empty when not on a plot):

| Placeholder | Description |
|-------------|-------------|
| `%plots_currentplot%` | Grid id of the current plot (`x;z`), or `none`. |
| `%plots_currentplot_name%` | The plot's name. |
| `%plots_currentplot_owner%` | The plot owner's name. |
| `%plots_currentplot_world%` | The plot's world. |
| `%plots_currentplot_members%` | Number of trusted members. |
| `%plots_currentplot_x%` | The plot's grid X. |
| `%plots_currentplot_z%` | The plot's grid Z. |
| `%plots_currentplot_merged%` | Number of plots in the merged group. |
| `%plots_is_owner%` | `true` if the player owns the current plot. |
| `%plots_is_trusted%` | `true` if the player is trusted on the current plot. |

## Player

| Placeholder | Description |
|-------------|-------------|
| `%plots_has_plot%` | `true` if the player owns at least one plot. |
| `%plots_plot_count%` | Total plots the player owns (all worlds). |
| `%plots_plot_count_<world>%` | Plots the player owns in a specific world. |
| `%plots_max_plots%` | The player's global plot limit (`unlimited` for admins). |
| `%plots_max_plots_<world>%` | The player's plot limit in a specific world. |
| `%plots_plots_left%` | Plots the player can still claim in their current world. |
| `%plots_total_warps%` | Total warps across the player's plots. |

## Server

| Placeholder | Description |
|-------------|-------------|
| `%plots_worlds%` | Number of managed plot worlds. |
| `%plots_total_plots%` | Total plots claimed on the server. |

> Limit placeholders honour the [hybrid limit model](../reference/permissions#plot-limits-hybrid-model),
> so `%plots_max_plots%` and `%plots_max_plots_<world>%` reflect the player's
> permissions and per-world config.
