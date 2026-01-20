# Changelog

All notable changes to this project will be documented in this file.

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
