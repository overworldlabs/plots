---
layout: home

hero:
  image:
    src: /logo.png
    alt: Plots — plot worlds for Hytale
  tagline: "A road-and-parcel grid, a claim each player fully owns, and protection that holds even against BuilderTools."
  actions:
    - theme: brand
      text: What is Plots?
      link: /guide/intro/what-is-plots
    - theme: alt
      text: Getting Started
      link: /guide/intro/getting-started
    - theme: alt
      text: Commands
      link: /guide/reference/commands

features:
  - icon: 🧱
    title: Grid World Generation
    details: A generated plot world with roads, borders and configurable parcel sizes — plus prefab-driven terrain, so your plots look like your server instead of a flat slab.
  - icon: 📜
    title: Full Claim Workflow
    details: Claim, auto-claim, rename, transfer, delete, trust and untrust. Players get a plot they own outright, plus list and info views to keep track of it.
  - icon: 🛡️
    title: Strict Protection
    details: Roads and other players' plots are protected against breaking, placing, interaction, liquid spill, mobs and item flow — with explicit staff bypass permissions.
  - icon: 🔧
    title: BuilderTools-Safe
    details: Protection is enforced across the packet, mask and chunk-accessor paths, so extrude and scripted brushes cannot tunnel through a neighbour's plot.
  - icon: 🌍
    title: Multi-World
    details: Run more than one plot world side by side, each with its own grid, sizes and rules — created and browsed straight from the in-game admin panel.
  - icon: 🚩
    title: Plot Flags
    details: Per-plot toggles for PvP, damage, weather, chat, item pickup and more, so each owner tunes their own parcel without anyone touching the server config.
  - icon: 💰
    title: Economy & Integrations
    details: Optional charging for claim, auto, merge and unmerge with automatic provider detection, plus Hylograms ownership signs and PlaceholderAPI support.
  - icon: 🔌
    title: Public API & Events
    details: Query and mutate plots programmatically, and hook claim, unclaim, rename and trust events from your own plugin.
  - icon: 🌐
    title: Localized
    details: English, Portuguese, Spanish and Russian bundles out of the box — every string overridable, with translations open on Crowdin.
---

<style>
:root {
  --vp-home-hero-image-filter: drop-shadow(0 12px 40px rgba(139, 43, 255, 0.32));
}
</style>
