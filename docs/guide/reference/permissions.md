# Permissions

Plots uses a granular permission system to control access to features and protection bypass.

## Basic Permissions

These permissions are typically granted to players to allow standard plot interactions.

| Node | Description | Default |
|------|-------------|---------|
| `plots` | **Mandatory base permission** required to use any command. | Player |
| `plots.claim` | Permission to claim plots with `/plot claim`. | Player |
| `plots.auto` | Permission to use `/plot auto`. `plots.claim` also grants it, so 1.1.x setups keep working. | Player |
| `plots.info` | Permission to view plot metadata. | Player |
| `plots.list` | Permission to list your own plots. | Player |
| `plots.spawn` | Permission to teleport to world spawn. | Player |
| `plots.limit.N` | Global shared plot limit across **all** worlds (e.g. `plots.limit.5`). See [Plot Limits](#plot-limits-hybrid-model). | — |
| `plots.<world>.limit.N` | Independent plot limit for a **specific** world (e.g. `plots.premium.limit.10`). | — |
| `plots.trust` | Permission to trust other players in your plots. | Player |
| `plots.untrust` | Permission to remove trusted players from your plots. `plots.trust` also grants it, so 1.1.x setups keep working. | Player |
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

## Plot Limits (Hybrid Model)

With multiple plot worlds, a player's limit is resolved from **three independent
ceilings** — a claim is only allowed when **all three** pass:

1. **Per-world budget** — how many plots the player may own *in that world*.
2. **Global shared budget** — total plots the player may own *across all worlds*.
3. **Hard cap** — an absolute server ceiling (`Limits.HardCap` in `config.json`).

**Resolution order** (most specific wins):

- Per-world budget: `plots.<world>.limit.N` permission → else the world's
  `MaxPlotsDefault` in config → else `Limits.PerWorldDefault`.
- Global budget: `plots.limit.N` permission → else `Limits.GlobalSharedDefault`
  (`-1` = unlimited).
- Admins (`plots.*` / `plots.admin`) bypass all limits.

::: tip Examples
- `plots.limit.5` → at most **5 plots total** across every world.
- `plots.premium.limit.10` → up to **10 plots in the `premium` world**, counted
  independently from other worlds.
- Grant both and the player gets per-world budgets *capped* by the global total.
:::

> The `<world>` segment is the world's **permission key** (lower-cased, with any
> non-alphanumeric character replaced by `_`). It defaults to the sanitized world
> name and can be overridden per world via `PermissionKey` in `config.json`.

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
