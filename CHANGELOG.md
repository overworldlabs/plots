# Changelog

All notable changes to this project will be documented in this file.

## [1.1.3] - 2026-02-20
### Added
- New translation bundles: Spanish (`es_es`) and Russian (`ru_ru`).
- Economy bypass permission node: `plots.economy.bypass`.
- Root command alias `/p` (alongside `/plot`, `/plots`, `/plotme`).

### Changed
- BuilderTools protection now also covers scripted brushes and extrude flows.
- Permission checks were centralized through `PermissionUtil` for consistent behavior across native server permissions and LuckPerms-backed setups.
- Admin bypass logic is standardized across integrations and command flows.

### Fixed
- Economy provider resolution now retries at runtime, improving compatibility when economy plugins initialize after Plots.
- Economy validation/charging in claim/auto/merge/unmerge flows now respects dedicated economy bypass without weakening admin protections.
- Multiple null-type safety and command nullability warnings resolved.

## [1.1.2] - 2026-02-19
### Added
- Command aliases:
  - `/plot f` for `/plot flag`
  - `/plot link` for `/plot merge`
  - `/plot unlink` for `/plot unmerge`
- Centralized command feedback utility for usage/error formatting.
- Centralized typed command-argument utility to consolidate unchecked generic casts.
- Root command dispatcher improvements for:
  - invalid subcommand handling with usage + subcommand list
  - global command exception handling with full input logging

### Changed
- Moved `unlink` implementation to alias on `unmerge` subcommand class (no duplicated command registration).
- Standardized messaging behavior for `flag`, `merge`, `unmerge`, `trust`, `untrust`, and `delete`.
- Updated documentation for command aliases, protections, and Hylograms integration behavior.

### Fixed
- Fixed `/plot flag` world-thread safety issue (`Store` component access on wrong thread).
- Fixed multiple null-safety edge cases in command argument normalization and UUID handling.
- Added trust/untrust safeguards for null target UUID resolution.
- Improved Hylograms API compatibility handling to avoid runtime class errors.

## [1.1.1] - 2026-01-20
### Fixed
- **Hylograms Detection**: Fixed runtime detection of Hylograms plugin using `PluginManager.getPlugin()` instead of `Class.forName()` for proper plugin dependency resolution.
- **Plugin Identifier**: Corrected Hylograms plugin identifier to use the correct group `"ehko"` and name `"Hylograms"`.
- **Optional Dependencies**: Added `OptionalDependencies` declaration in manifest.json for proper Hylograms integration.

### Added
- **Public API**: Implemented comprehensive public API (`PlotsAPI`) for external plugins to interact with plot system programmatically. Includes:
  - `PlotAPI`: Query plots, check ownership, get trusted players
  - `PlotEventAPI`: Listen to plot events (claim, unclaim, trust, untrust, rename)
- **BuilderTools Protection**: Implemented plot protection for 85% of BuilderTools operations (67 tools) including Paint, Box, Sphere, Cylinder, Replace, Set, and all other tools using `ToolOperation.OPERATIONS`. 
  - **Known Limitation**: Extrude and Scripted Brushes cannot be protected at this time as the Hytale API does not provide hooks for packet handlers and command executors. We continue investigating alternative approaches for future versions.

### Changed
- **Code Cleanup**: Removed unused hologram spawning methods and simplified `HologramManager` implementation.

## [1.1.0] - 2026-01-20
### Added
- **Holograms Integration**: Added support for [Hylograms](https://www.curseforge.com/hytale/mods/hylograms) to display plot information at the corner of claimed plots.
  
  ![Holograms](assets/holograms.png)
  
- **Java 25 Support**: Successfully updated the project toolchain to Java 25.
- **Update Checker**: Admins are now notified when joining the server if a new version is available on GitHub.

### Changed
- **Redundant Logic**: Removed multiple redundant permission checks across all subcommand classes.

### Removed
- **Cleaned Up**: Removed unused imports and deprecated logic in various command files.

### Fixed
- **Permission Validation**: Fixed a critical issue where permissions were not being correctly validated, preventing non-OP players from using commands.
- **Translations**: Replaced hardcoded "player-only" error messages with the `general.only_players` translation key.

## [1.0.0] - 2026-01-15
### Added
- Initial release of the Plots plugin with fundamental land management features.
- Dynamic generation system with prefab support.
- Granular protection and masking logic.
