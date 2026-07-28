# Changelog

All notable changes to Plots. Dates are in UTC. The docs always describe the **current** release.

## 1.2.0

The biggest release since launch: Plots now runs **more than one plot world**, ships a full in-game
menu and HUD, and moves its protection onto the shared **TaleGuard** bridge.

**Multi-world**
- Run several plot worlds side by side, each with its own grid, plot/road sizes and terrain — no longer a single `PlotWorldName`. See [Multi-World](/guide/features/multiworld).
- Create and browse worlds from the admin panel (**Worlds** tab), or with `/plot worlds`.
- A first-run setup nudge walks a fresh server through creating its first plot world instead of leaving you on an empty void.

**In-game UI**
- A plot **menu** covering the everyday actions — claim, trust, flags, warps, rename, delete — so players don't have to memorise commands.
- A plot **HUD** showing where you are and who owns it.
- New **help page**, **weather picker**, **worlds browser** and result popups, all reachable from the menu.

**Plots & players**
- **Per-plot warps**, plus a public warps browser and `/plot warps`.
- **Per-plot spawn point** — `/plot setspawn`, and `/plot spawn` to return to it.
- **Offline transfer**: plots can be transferred to a player who has ever joined, not just one who is online right now.

**Commands**
- Tab-completion on `/plot` arguments — subcommands, flag names and values now suggest as you type.
- An unknown subcommand gives you a short, targeted correction instead of dumping the whole help text.

**Protection**
- Protection mixins now come from the shared **TaleGuard** bridge, consumed at runtime through the hook registry. Plots keeps a pure-ECS fallback, so it still protects without TaleGuard installed — flags that genuinely need the bridge (such as explosions) are hidden when it is absent rather than silently doing nothing.

**Integrations**
- **PlaceholderAPI** expansion — expose plot ownership and counts in scoreboards, tab lists and chat. See [PlaceholderAPI](/guide/integrations/placeholders).

**Under the hood**
- Ported to the current Hytale server build (`org.joml` vectors and the new world/entity APIs).
- Package renamed from `com.overworldlabs.plots` to **`dev.stoshe.plots`**. Plugins compiled against the 1.1.x API must update their imports — see [API Usage](/guide/api/api-usage).

::: warning Upgrading from 1.1.x
The world layout is now described per world instead of by a single top-level `PlotWorldName`.
Back up `config.json` and your plot data before the first 1.2.0 start, and read
[Multi-World](/guide/features/multiworld) for what the new shape looks like.
:::

## 1.1.3

- Spanish (`es_es`) and Russian (`ru_ru`) translation bundles.
- Economy bypass permission node `plots.economy.bypass`, honoured by `claim`, `auto`, `merge` and `unmerge` without weakening the admin protections.
- BuilderTools protection extended to **scripted brushes and extrude** — the two flows 1.1.1 could not cover.
- Permission checks centralized through a single helper, so native server permissions and LuckPerms-backed setups behave identically.
- Economy provider lookup now retries at runtime, so an economy plugin that initializes after Plots is still detected.
- Root alias `/p`, alongside `/plot`, `/plots` and `/plotme`.

## 1.1.2

- Aliases `/plot f`, `/plot link` and `/plot unlink` for `flag`, `merge` and `unmerge`.
- Consistent usage and error formatting across every subcommand, plus a root dispatcher that answers an invalid subcommand with usage and the subcommand list.
- Fixed a world-thread safety issue in `/plot flag` (component access from the wrong thread).
- Fixed null-safety edge cases in argument normalization, UUID handling, and trust/untrust target resolution.
- Improved Hylograms API compatibility handling to avoid runtime class errors.

## 1.1.1

- **Public API** (`PlotsAPI`): query plots, check ownership and trusted players, and listen to claim, unclaim, trust, untrust and rename events. See [Developer API](/guide/api/api).
- **BuilderTools protection** for the tool-operation path — Paint, Box, Sphere, Cylinder, Replace, Set and the rest.
- Fixed Hylograms detection to resolve through the plugin manager under the correct `ehko:Hylograms` identifier, declared as an optional dependency.

## 1.1.0

- **Hylograms integration** — plot information displayed in-world at the corner of a claimed plot.
- **Update checker** — admins are told on join when a newer release is available on GitHub.
- Java 25 toolchain.
- Fixed a permission validation bug that blocked non-OP players from using commands.

## 1.0.0

Initial release — grid plot worlds with prefab-driven generation, the claim/trust/flag/merge
workflow, and the protection and masking layer.
