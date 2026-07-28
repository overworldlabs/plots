<p align="center">
  <img src="https://github.com/stoshelabs/plots/blob/main/plots.png?raw=true" alt="Plots" width="720">
</p>

<p align="center">
  <b>Grid-based plot management for Hytale.</b><br>
  A generated world of roads and parcels, a claim every player fully controls, and protection that holds even against BuilderTools.
</p>

<p align="center">
  <a href="https://stoshelabs.github.io/plots/"><img src="https://img.shields.io/badge/docs-online-8B2BFF?style=for-the-badge" alt="Documentation"></a>
  <a href="https://github.com/stoshelabs/plots/releases/latest"><img src="https://img.shields.io/github/v/release/stoshelabs/plots?style=for-the-badge&label=version&color=2ea44f" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/Hytale-server%20plugin-22D3EE?style=for-the-badge" alt="Hytale server plugin">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/worlds-multi--world-8B2BFF?style=flat-square" alt="multi-world">
  <img src="https://img.shields.io/badge/protection-BuilderTools--safe-8B2BFF?style=flat-square" alt="BuilderTools-safe protection">
  <img src="https://img.shields.io/badge/i18n-en__us%20%7C%20pt__br%20%7C%20es__es%20%7C%20ru__ru-8B2BFF?style=flat-square" alt="i18n en_us | pt_br | es_es | ru_ru">
</p>

<p align="center">
  📖 <b><a href="https://stoshelabs.github.io/plots/">Read the full documentation →</a></b>
</p>

---

## Overview

Plots turns a Hytale world into a **plot server**. It generates a grid of parcels separated by roads,
hands each player a claim they own outright, and keeps everything outside that claim protected —
roads, neighbours, and the rest of the world.

The protection is the part that matters. Plots enforces it across the packet, mask and chunk-accessor
paths, so a builder with **BuilderTools** cannot extrude or brush their way past a plot border — the
flow most plot plugins leave open.

- **Grid world generation** — configurable plot and road sizes, borders, and prefab-driven terrain
- **Full claim workflow** — claim, auto-claim, rename, transfer (offline players included), trust, merge/unmerge
- **BuilderTools-safe protection** — break, place, interact, liquid, mobs and item flow, with staff bypass permissions
- **Multi-world** — several plot worlds side by side, each with its own grid and rules
- **In-game menu &amp; HUD** — the everyday actions without memorising commands, plus per-plot warps and spawn points
- **Integrations** — optional economy charging, Hylograms ownership signs, PlaceholderAPI, SQL persistence
- **Public API** — query plots and hook claim/unclaim/trust/rename events from your own plugin

---

## Quick start

1. Drop `Plots-<version>.jar` from the **[latest release](https://github.com/stoshelabs/plots/releases/latest)** into your server's `mods/` folder.
2. Start the server once to generate `config.json` and the language files.
3. Run `/plot worlds` and create your first plot world (or follow the setup prompt shown on join).
4. Head into that world and run `/plot auto` to claim a plot and start building.

Aliases `/plots`, `/plotme` and `/p` all work. The
**[Getting Started](https://stoshelabs.github.io/plots/guide/intro/getting-started)** guide covers this
in full, including plot/road sizing and economy setup.

---

## Documentation

Everything lives on the docs site — mechanics, every config key, and the full command reference:

| | |
| --- | --- |
| 🚀 [Getting Started](https://stoshelabs.github.io/plots/guide/intro/getting-started) | Install to first claimed plot |
| 🧱 [World Generation](https://stoshelabs.github.io/plots/guide/features/world-gen) · [Multi-World](https://stoshelabs.github.io/plots/guide/features/multiworld) · [Protection](https://stoshelabs.github.io/plots/guide/features/protection) · [Flags](https://stoshelabs.github.io/plots/guide/features/flags) | How it works |
| ⚙️ [Config Reference](https://stoshelabs.github.io/plots/guide/setup/config) · [Prefabs](https://stoshelabs.github.io/plots/guide/setup/prefabs-customization) · [Translations](https://stoshelabs.github.io/plots/guide/setup/translations) | Configuration |
| 🔌 [Hylograms](https://stoshelabs.github.io/plots/guide/integrations/hylograms) · [PlaceholderAPI](https://stoshelabs.github.io/plots/guide/integrations/placeholders) · [Developer API](https://stoshelabs.github.io/plots/guide/api/api) | Integrations &amp; API |
| 📖 [Commands](https://stoshelabs.github.io/plots/guide/reference/commands) · [Permissions](https://stoshelabs.github.io/plots/guide/reference/permissions) | Reference |
| 📝 [Changelog](https://stoshelabs.github.io/plots/guide/changelog) | What changed in each release |

---

## Building from source

```sh
./gradlew jar        # → build/libs/Plots-<version>.jar
```

The build compiles against `HytaleServer.jar` from your Hytale install; point it elsewhere with
`-Phytale_home=/path/to/Hytale`. The docs live in [`docs/`](docs) (VitePress) and deploy to GitHub
Pages automatically on push to `main`.

---

<sub>Built for Hytale by <a href="https://github.com/gitgusilva">Gustavo Will</a> · Stoshe Labs · <a href="LICENSE">MIT</a><br>
Also on <a href="https://modtale.net/mod/plots-f9ee11f8-1fa2-4bb2-aa9b-d726474c712b">Modtale</a> &amp; <a href="https://www.curseforge.com/hytale/mods/plot">CurseForge</a> · <a href="https://discord.gg/rC9eSzH3tf">Discord</a> · help translate on <a href="https://crowdin.com/project/hytaleplots">Crowdin</a></sub>
