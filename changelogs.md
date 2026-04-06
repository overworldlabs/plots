# Changelogs

## [Unreleased] - 2026-02-20
### Added
- New translations: `es_es` (Spanish) and `ru_ru` (Russian).
- Economy bypass permission node: `plots.economy.bypass`.
- Root command alias `/p` for `/plot`.

### Changed
- BuilderTools protection now includes extrude and scripted brush paths.
- Permission validation has been centralized through `PermissionUtil` to keep behavior consistent between native server permissions and LuckPerms.
- Admin bypass handling is now unified across integration and command checks.

### Fixed
- Economy provider lookup now retries lazily at runtime, avoiding startup-order issues with external economy plugins.
- Economy charge/validation flow for `claim`, `auto`, `merge`, and `unmerge` now correctly uses economy bypass checks.
- Additional null-type safety issues resolved in command and bootstrap paths.

## [Snapshot] - 2026-02-19
### Added
- Command aliases:
  - `/plot f` for `/plot flag`
  - `/plot link` for `/plot merge`
  - `/plot unlink` for `/plot unmerge`
- Centralized command feedback helper (`CommandFeedback`) for consistent usage/errors.
- Centralized typed argument helper (`CommandArgs`) to keep unchecked casts in one place.
- Root `/plot` dispatcher behavior with:
  - standardized invalid-subcommand feedback
  - root usage + subcommand listing
  - global command exception logging with full input

### Changed
- `unlink` is now implemented as a true alias on `unmerge` command class (no duplicated subcommand instance).
- Standardized usage/argument messaging across flag/merge/unmerge/trust/untrust/delete flows.
- Documentation updated for aliases, command system behavior, protection behavior, and Hylograms resilience.

### Fixed
- Fixed world-thread violation in `/plot flag` by moving store/component access to world thread execution.
- Added null-safety guards for command argument normalization (`toLowerCase`) and UUID resolution.
- Added defensive null checks for target player UUID in trust/untrust.
- Improved compatibility handling for Hylograms API variations without hard crashes.

## [Snapshot] - 2026-02-18
### Added
- Command aliases: `/plots`, `/plotme` (in addition to `/plot`).
- Plot merge command: `/plot merge <north|south|east|west|all>`.
- Plot unmerge command: `/plot unmerge <north|south|east|west|all>`.
- Plot unlink alias: `/plot unlink <north|south|east|west|all>`.
- New plot flag: `weather` (`clear`, `rain`, `storm`).
- Config support for managed worlds beyond plotworld: `EnableInDefaultWorld` and `AdditionalWorlds`.
- Public API hook to override Plots world generator from another plugin.
- API unification: `PlotsAPI` now aggregates plot operations + event API.
- Added `Plots.enabled()` helper for safe external plugin checks.
- Optional SQL persistence with HikariCP (SQLite/MySQL/MariaDB/PostgreSQL) via config.
- Optional economy integration with auto provider detection for:
  `EliteEssentials`, `EconomySystem`, `Ecotale`, `EssentialsPlus`.
- Configurable plot economy costs:
  `Economy.Costs.Claim`, `Economy.Costs.AutoClaim`, `Economy.Costs.Merge`, `Economy.Costs.Unmerge`.
- Optional dependencies declaration for supported economy plugins in `manifest.json`.

### Changed
- BuilderTools usage is now restricted to plot owners (admin still bypasses).
- Liquid placement at borders is blocked when it would flow outside owned area.
- Protection checks can run in configured managed worlds, not only `plotworld`.
