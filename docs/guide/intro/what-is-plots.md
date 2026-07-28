# What is Plots?

Plots turns a Hytale world into a **plot server**: a generated grid of parcels separated by roads,
where every player claims a piece of land they fully control and cannot touch anyone else's.

It is built for servers where building is the point — creative servers, build competitions, city
projects — and where the admin needs the protection to hold even against a determined builder with
BuilderTools in hand.

## Core Scope

- **Plot ownership** — claim, auto-claim, rename, transfer (even to offline players), delete, trust and untrust.
- **World structure** — road-based grids, per-plot spawn points and warps, and merge/unmerge between adjacent plots you own.
- **Protection** — roads and foreign plots are protected against breaking, placing, interaction, liquid spill, mobs and item flow, with explicit staff bypass permissions.
- **BuilderTools safety** — enforced across the packet, mask and chunk-accessor paths, so extrude and scripted brushes cannot reach past a plot border.
- **Multi-world** — more than one plot world at a time, each with its own grid and rules.
- **Server integration** — optional economy charging, SQL persistence, translation bundles, PlaceholderAPI, and a public plugin API.

![World Generation Demo](/assets/prefab-island-plot-map.png)
*Grid-based world generation with prefab terrain.*

## Visual Feedback

Plots ships an in-game **menu and HUD** for the everyday actions, and integrates with **Hylograms**
to display ownership information floating in the world itself.

![Hologram Demo](/assets/hylograms.png)
*Dynamic holograms showing plot ownership.*

## Downloads

- [**GitHub Releases**](https://github.com/stoshelabs/plots/releases) — official builds and source.
- [**Modtale**](https://modtale.net/mod/plots-f9ee11f8-1fa2-4bb2-aa9b-d726474c712b)
- [**CurseForge**](https://www.curseforge.com/hytale/mods/plot)

## Support

- [**Discord**](https://discord.gg/rC9eSzH3tf) — support, announcements and community.
- [**Issue tracker**](https://github.com/stoshelabs/plots/issues) — bugs and feature requests.
- [**Crowdin**](https://crowdin.com/project/hytaleplots) — help translate Plots into your language.

Next: [Getting Started](/guide/intro/getting-started).
