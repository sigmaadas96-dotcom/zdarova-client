# Zdarova Client (Fabric 1.21.1)

Client-side utility mod. Modules:

| Module | What it does |
|---|---|
| Chunk Finder | Outlines chunks with many storage blocks (chests, barrels, shulkers, ender chests). Stash/base finder. |
| Sus Chunk Finder | Outlines chunks with a high "player-made" score (beds, containers, redstone, glass, torches, planks, ...). Villages/mineshafts can trigger it too - raise the threshold. |
| Storage ESP | Boxes around chests, trapped chests, ender chests, barrels, shulkers, hoppers, droppers/dispensers, furnaces. |
| Player ESP | Boxes + tracers on other players. |
| Block ESP | Boxes on chosen blocks (default: ancient debris, diamond, emerald, spawner). |
| Swing Speed | Shortens your own hand swing animation (visual only). |

## Controls
- **Right Shift** - opens the GUI (toggle modules, change settings). Rebind in Controls.
- `/zd toggle <module>` - ids: chunkfinder, suschunkfinder, storageesp, playeresp, blockesp, swingspeed
- `/zd blockesp add <block>` / `remove <block>` / `list` / `reset` (e.g. `/zd blockesp add ancient_debris`)

## Build
Requires JDK 21.

```
gradle wrapper --gradle-version 8.10.2   # once, if there is no gradlew yet
./gradlew build
```

The jar appears in `build/libs/zdarova-client-1.0.0.jar`. Put it in `.minecraft/mods` together with
Fabric Loader 0.16+ and Fabric API for 1.21.1.

Settings are not saved between launches yet.
