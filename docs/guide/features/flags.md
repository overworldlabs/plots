# Plot Flag System

The Plot Flag system allows plot owners and administrators to customize specific behaviors within a plot.

## Managing Flags

Flags are managed using the `/plot flag` command.

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/plot flag set <flag> <value>` | Sets a flag to a specific value | `plots` (and plot ownership) |
| `/plot flag remove <flag>` | Reverts a flag to its default value | `plots` (and plot ownership) |
| `/plot flag list` | Lists all available flags and their current values | `plots` |
| `/plot f ...` | Alias for `/plot flag ...` | `plots` |

### Validation and Feedback

- `flag` subcommands use standardized usage/error messaging.
- Unknown subcommands return a clear "command not recognized" response plus usage.
- Argument handling is null-safe and normalized before processing.

## Available Flags

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `interact` | Boolean | `true` | Allow block interaction (doors, chests, etc.). |
| `item-drop` | Boolean | `true` | Allow players to drop items. |
| `item-pickup` | Boolean | `true` | Allow players to pick up items. |
| `mob-damage` | Boolean | `true` | Allow mobs to damage players. |
| `greet-message` | String | (Empty) | Message shown when entering the plot. |
| `farewell-message` | String | (Empty) | Message shown when leaving the plot. |
| `player-chat` | Boolean | `true` | Allow players to send chat messages. |
| `crafting` | Boolean | `true` | Allow players to use crafting stations. |
| `mob-spawn` | Boolean | `true` | Allow hostile mobs to spawn. |
| `mob-cap` | Integer | `50` | Maximum number of mobs allowed on the plot. |
| `pvp` | Boolean | `false` | Allow player vs player combat. |
| `weather` | String | `clear` | Weather mode for the plot (`clear`, `rain`, `storm`). |

## Examples

- **Set a custom greeting:**
  `/plot flag set greet-message "{#FFD700}Welcome to my Kingdom!"`

- **Disable hostile mobs:**
  `/plot flag set mob-spawn false`

- **Enable PvP for an arena plot:**
  `/plot flag set pvp true`

- **Remove a flag setting:**
  `/plot flag remove pvp`
