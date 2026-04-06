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
