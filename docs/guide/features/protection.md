# Features

Plots offers a variety of features to make plot management easy and flexible.

## World Generation

The plugin includes a custom world generator that creates a grid-based terrain. You can configure:
- **Plot Dimensions:** Custom X and Z sizes.
- **Road Width:** Configurable spacing between plots.
- **Materials:** Bedrock, surface, and sub-surface blocks can be customized.
- **Fluids:** Support for liquid backgrounds (like water for islands).

## Smart Prefabs

Unlike traditional plot plugins that just fill area with blocks, Plots uses Hytale's prefab system:
- **Automatic Centralization:** If a prefab is smaller than the plot, it is automatically centered.
- **Skip-Air Logic:** Background blocks (like water) show through "Empty" areas of your prefab.
- **Anchor Support:** Uses prefab anchors for precise vertical and horizontal alignment.

## Protection System

- **Grief Protection:** Only owners and trusted players can build.
- **Fluid Flow:** Prevents liquids from spreading into roads or other plots.
- **BuilderTools Integration:** Restricted to the plot owner (trusted members cannot use BuilderTools by default).
- **Scripted/Extrude Coverage:** BuilderTools scripted brushes and extrude paths are also validated against plot ownership.
- **Admin Bypass:** Staff with admin bypass permission can use tools anywhere.
- **Merge-Aware Borders:** Merged adjacent plots are treated as connected for build permissions.
- **Managed Worlds:** Protection is enforced in configured managed worlds, not only in `plotworld`.
- **Merged Area Access:** After merge, former road area between merged plots is treated as owned buildable area.
- **Unmerge Restoration:** On unmerge, road/border restoration is applied to return map boundaries.

## Protection Coverage: Native vs TaleGuard

Plots enforces protection in two layers, mirroring the skyblock design:

- **Native protection (ECS systems)** — always active, requires **no** extra plugin. These
  are the core grief protections and work out of the box.
- **Bridge protection (TaleGuard)** — adds the protections that can only be enforced by
  bytecode mixins (auto item pickup, builder tools internals, command filtering, etc.).
  Plots consumes the shared [**TaleGuard**](https://github.com/) bridge and registers a
  protection hook with it. When TaleGuard is **not** installed, the plugin **refuses to
  enable** the bridge-only flags (and warns the player), so a flag never silently fails to
  protect — native protection still applies.

> Plots automatically detects TaleGuard at startup and logs the active mode:
> `TaleGuard bridge active — full protection coverage enabled` or
> `running with BASIC protection (ECS) only`.

### Covered natively (no bridge needed)

| Capability | Flag | Enforced by |
| --- | --- | --- |
| Block breaking | `block-break` | `BreakProtectionSystem`, `ServerBlockProtectionSystem` |
| Block placing | `block-place` | `PlaceProtectionSystem` |
| Block damage (mining hits) | `block-break` | `DamageBlockProtectionSystem` |
| PvP | `pvp` | `PlotDamageFlagSystem` |
| Generic damage / fall damage | `damage`, `fall-damage` | `PlotDamageFlagSystem` |
| Mob damage to players | `mob-damage` | `PlotMobProtectionSystem`, `PlotDamageFlagSystem` |
| Item drop | `item-drop` | `PlotItemDropSystem` |
| Manual / F-key item pickup | `item-pickup-manual` | `PlotItemPickupSystem` |
| Crafting at stations | `crafting` | `PlotCraftingSystem` |
| Plot entry / exit | `entry`, `exit` | `PlotNotificationSystem` (pushback) |
| Greet / farewell / deny messages | `greet-message`, `farewell-message`, `deny-message` | `PlotNotificationSystem` |
| Per-plot weather | `weather` | `PlotNotificationSystem` |
| BuilderTools (mask, brushes, extrude, scripted) | `build`, `hammer` | `BuilderToolsMaskSystem` + packet interceptor (basic coverage) |
| Block interaction (partial) | `interact`, `use` | `canModify` via drop/pickup paths |

### Requires TaleGuard (mixin-only — no native fallback)

If TaleGuard is missing, these flags cannot be enabled from `/plot flag` or the menu:

| Flag / capability | TaleGuard mixin |
| --- | --- |
| `item-pickup` (auto pickup, walking over items) | PlayerItemEntityPickupSystem |
| `item-pickup-manual` (full pickup control) | PlayerItemEntityPickupSystem |
| `build` / `hammer` (deep BuilderTools coverage) | BuilderToolsPacketHandler |
| `allowed-cmds` / `blocked-cmds` (command filtering) | CommandManager |
| `seat` (sitting) | SeatingInteraction |
| `mob-spawning` | NPCPluginSpawn, SpawnMarkerEntity, WorldSpawnJobSystems |
| `keep-inventory` | DeathItemDrop |
| `invincible-items` (no durability loss) | ItemDurability |
| `explosions` (server-side explosion damage) | ExplosionBlockDamage |
| Crop harvesting / farming-stage interactions | HarvestCropInteraction, ChangeFarmingStageInteraction, CycleBlockGroup, ChangeStateInteraction |
| Fluid flow across plot borders | DefaultFluidTicker, FiniteFluidTicker |

> Some flags (e.g. `item-pickup-manual`, `build`/`hammer`, `interact`) have **partial**
> native coverage and **full** coverage with TaleGuard.
