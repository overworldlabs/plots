# TaleGuard Integration

[TaleGuard](https://github.com/stoshelabs/taleguard) is the shared protection bridge used across the
Stoshe plugins. Plots consumes it at runtime to enforce the flags that can only be implemented with
bytecode mixins.

**It is optional.** Plots protects a server on its own — every core grief protection runs as an ECS
system with no extra plugin. TaleGuard only adds the layer the server API cannot reach.

- Download: [CurseForge](https://www.curseforge.com/hytale/mods/taleguard) · [GitHub Releases](https://github.com/stoshelabs/taleguard/releases)

## What it adds

Without TaleGuard, Plots still enforces block breaking and placing, block damage, PvP, mob damage,
item drops, container access, crafting, chat and entry/exit. Those never depend on the bridge.

With TaleGuard installed, these become available as well:

| Flag / capability | Why it needs the bridge |
| --- | --- |
| `item-pickup` | Auto item pickup happens inside the server's inventory path |
| `mob-spawning` | Spawn decisions are made before any event the API exposes |
| `keep-inventory` | Death handling is internal |
| `explosions` | Explosion damage resolution is internal |
| BuilderTools internals | Extrude and scripted brushes bypass the normal block hooks |
| Command filtering | Command dispatch has no per-plot hook |

See [Protection Coverage](/guide/features/protection#protection-coverage-native-vs-taleguard) for the
full matrix.

## Installing

1. Drop the TaleGuard jar into your server's `earlyplugins/` folder — **not** `mods/`. The bridge has
   to bootstrap before the classes it patches are loaded.
2. Restart the server.
3. Check the console. Plots reports the mode it resolved at startup:

```
TaleGuard bridge active — full protection coverage enabled.
```

If it is missing you will see this instead, and the bridge-only flags stay hidden in the menu rather
than pretending to work:

```
No protection bridge detected — running with BASIC protection (ECS) only.
```

::: tip
`/plot debug bridge` prints the bridge and mixin state at any time, which is the quickest way to tell
"not installed" apart from "installed but the mixins did not apply".
:::

## Without it

Nothing breaks. Plots hides the bridge-only flags in the menu and refuses to set them from
`/plot flag`, with a message explaining why, so a flag never silently fails to protect. Everything
else behaves identically.
