# Permissions

Plots uses a granular permission system to control access to features and protection bypass.

## Basic Permissions

These permissions are typically granted to players to allow standard plot interactions.

| Node | Description | Default |
|------|-------------|---------|
| `plots` | **Mandatory base permission** required to use any command. | Player |
| `plots.claim` | Permission to claim plots. | Player |
| `plots.auto` | Permission to use `/plot auto`. | Player |
| `plots.info` | Permission to view plot metadata. | Player |
| `plots.list` | Permission to list your own plots. | Player |
| `plots.spawn` | Permission to teleport to world spawn. | Player |
| `plots.limit.N` | Sets the maximum number of plots a player can have (e.g., `plots.limit.5`). | 1 |
| `plots.trust` | Permission to trust other players in your plots. | Player |
| `plots.untrust` | Permission to remove trusted players from your plots. | Player |
| `plots.rename` | Permission to rename your own plots. | Player |
| `plots.flag` | Permission to manage flags on your own plots. | Player |
| `plots.merge` | Permission to merge adjacent plots you own. | Player |
| `plots.unmerge` | Permission to unmerge adjacent plots you own. | Player |
| `plots.delete` | Permission to unclaim your own plots. | Player |

## Admin Permissions

These permissions should only be granted to staff members as they bypass standard protection.

| Node | Description | Default |
|------|-------------|---------|
| `plots.*` | **Absolute administrative permission**. Full protection bypass and access to all plots. | OP / Admin |
| `plots.delete.*` | Permission to delete (unclaim) any player's plot. | OP / Admin |
| `plots.admin.bypass` | Explicit admin bypass node (equivalent admin access). | OP / Admin |
| `plots.admin` | Legacy-compatible admin node still accepted by permission checks. | OP / Admin |
| `plots.economy.bypass` | Bypasses economy costs for paid plot actions only. | OP / Admin |

## Grouping Recommendations

### Default Player
- `plots` (Mandatory)
- `plots.claim`
- `plots.auto`
- `plots.info`
- `plots.spawn`
- `plots.trust`
- `plots.rename`
- `plots.flag`
- `plots.merge`
- `plots.unmerge`
- `plots.delete`
- `plots.limit.3` (Example limit)

### Administrator
- `plots.*`
- `plots.economy.bypass` (optional, if you want cost bypass without changing other permissions)
