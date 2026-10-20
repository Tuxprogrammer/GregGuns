# Launcher Frame and the Launcher Layout

The frame is the scaffold tower block. This sheet also answers the "multiblock layout" question for the whole launcher. Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Frames carry the network ([launcher-connector-and-network.md](launcher-connector-and-network.md)); the connector is a frame with a different look.

## 1. Identity

| Item | Value |
|---|---|
| Classes | `content/blocks/launcher/frame/BlockLaunchFrame`, `TileLauncherFrame`, `EnumFrameState`, `PropertyFrameState`; the tile base `network/ILauncherComponent`; `content/blocks/multiblock/TileMulti` (deprecated stub) |
| Registry name | block `icbmclassic:launcherframe`, tile `icbmclassic:launcherframe` |
| English name | "Launcher Frame" (`tile.icbmclassic:launcherframe.name`); tooltip "Optional decorative launcher connection block" (wrong: it carries the network, ICBM-04-28) |
| Block properties | extends the shared ICBM block base: iron material, hardness 10, resistance 10. Not opaque, but it keeps the default full-cube collision (the visual lattice has a full-block hitbox). Render layer: cutout. No inventory drop |
| State (*1.12-only mechanism*) | `rotation` (6 values, stored in meta, set to the placer's horizontal facing) and `type` = `top` / `middle` / `bottom`, a computed property (not stored) |
| Tile | `TileLauncherFrame`: a plain tile with no ticking, no inventory and no energy buffer. It is a network node only |

## 2. Obtaining

Shaped, 2 x 2: `i b i` / `i b i`; i = `ingotIron`, b = iron bars. Result 1 (`recipes/launcher/support.json`; the recipe file is named "support"). Recipes belong to `06-blocks-items`.

## 3. Numbers

None beyond hardness. The frame has no config.

## 4. Behavior

### Frame state (`type`), computed from neighbors, never stored

A "frame neighbor" is a block that is the frame itself **or the launcher screen**. The check looks only directly above and below.

| Frame above? | Frame below? | `type` | Model |
|---|---|---|---|
| yes | yes | `middle` | full-height lattice |
| no | yes | `top` | shorter lattice (posts 13/16 tall, center pipe 11/16) with an open top |
| yes | no | `bottom` | a 6/16 tall base plate with the lattice above it |
| no | no | `middle` | full-height lattice (a lone frame) |

So a column renders as plate, lattice ..., then either an open top (if it ends in a frame) or an unchanged lattice meeting the screen (if the column ends in the screen, the screen counts as "above"). The base pad and the connector do **not** count.

### Network role

On load it joins the network of any loaded adjacent component; on removal (break, or chunk unload) it detaches and the network is rebuilt without it. No state of its own. It forwards the **network's** energy and item-handler capabilities to the outside (see below).

### Interactions

Right click with a stone axe prints a debug network line (ICBM-04-18); otherwise nothing. Frames and connectors do not open a GUI.

### Capabilities (*1.12-only mechanism*)

If the frame is on a network, `hasCapability`/`getCapability` for Forge Energy and the item handler return the **network's** shared energy sink and shared inventory, on every side. A cable, hopper or pipe touching any frame therefore feeds every pad on the network. Capabilities for anything else come from the base tile (none).

## 5. Persistence and sync

None. The tile stores nothing. The network is rebuilt on every tile load and after every removal; it is never saved.

## 6. UI

None.

## 7. Visuals

- Models by `type`, via `blockstates/launcherframe.json`: `models/block/frame_bottom.json`, `frame_middle.json`, `frame_top.json` (credit field: "QueenOfMissiles"), each with a `rotation` variant (north y 0, south y 180, east y 90, west y 270).
- Geometry (16-pixel units): four corner posts 2 pixels wide at x/z in [2..4] and [12..14], a center pipe at x [6..8], z [2..4], and four thin "wire" planes at z = 3 and 13 and x = 3 and 13 between the posts. **The inside, x and z from 4 to 12, is empty**: the lattice is a hollow shaft. `bottom` adds a 16 x 6 x 16 plate under the lattice (lattice from y 6 to 16).
- Textures: `blocks/silo_frame/frame.png`, `base_bottom.png`, `base_side.png`, `base_top.png` (all 16 x 16).

## 8. Audio

None.

## 9. Relationships

- [launcher-connector-and-network.md](launcher-connector-and-network.md): the connector extends this tile and block family.
- [launch-screen.md](launch-screen.md): the screen is the intended cap of a frame column.
- `06-blocks-items`: recipe and block family; `07-visuals-audio`: models.

## 10. Bugs

ICBM-04-18 (debug right click), ICBM-04-28 (tooltip).

---

## The launcher layout (what "multiblock" means here)

**There is no enforced multiblock.** Nothing in the code validates a footprint, a frame height, or the relative position of the pad, frames and screen. What exists is (1) a graph of adjacent blocks, which gives the network, (2) a fixed spawn point and render position for the missile, and (3) a lattice model whose hollow shaft is meant to sit around that missile. The old `TileMulti` ("host position" tile) is a deprecated stub that does nothing; it is registered as `icbmclassic:multiblock`, and its `onLoad` is a TODO. No block uses it.

### Network connectivity rules (exact)

- Components are: launcher base, cruise launcher, launcher screen, launcher frame, launcher connector.
- Two components are linked when their blocks share a **face** (the six neighbors). Diagonals and gaps do not link. There is no distance or count limit.
- Linking is transitive, so any face-connected chain of components is one network. Two chains that touch merge into one.
- Only loaded neighbors link. A chunk boundary does not split a network permanently: whichever tile loads later connects to the already loaded one.
- An isolated component with no component neighbor has **no network** at all.
- The pad and the cruise launcher are the only components that fire; the screen is the only one that commands them in bulk.

### Footprint the code assumes

| Aspect | Value | Source |
|---|---|---|
| Pad | 1 block | `BlockLauncherBase` |
| Missile drawn | center at 2.0 blocks out from the pad center along the facing (UP: 2.5 above the pad's base, so over the second block above) | `TESRLauncherBase` |
| Missile spawned | 2.6 above the center for UP (3.1 below for DOWN; 2.8 sideways, 0.2 down, for horizontals) | `LauncherCapability` |
| Insertion blocked while a missile entity is in | box 1 x 5 x 1, from the pad's own Y up to Y + 5 | `TileLauncherBase.checkForMissileInBounds` |
| Shaft clearance check at launch | **none** (TODO in source) | `LauncherCapability` |
| Frame column | any height; each frame is 1 block | `BlockLaunchFrame` |
| Screen | 1 block, any side of the column | |

So an UP pad needs about 4 clear blocks above it (the missile is roughly 3 tall, drawn centered 2.5 above the pad base), and the frame lattice is built to be stacked **around that shaft**: vertical stacks starting at the pad's top face, or beside it, with the screen on top. No rule says this; players can place frames anywhere. Because the frame hitbox is a full cube while the visual is a hollow lattice, frames stacked in the shaft put full-cube hitboxes in the missile's path (what flight then does is `03-missiles`; not verified here). The intended arrangement from the models and names is:

```
 [screen]          <- top of the column (counts as "frame above" for the frame below it)
 [frame  ]         <- type middle (frame above and below)
 [frame  ]         <- type bottom (frame above, none below): has the 6/16 base plate
 [ pad   ]  facing UP, missile in the free space above/next to it
```

with the pad usually standing next to the column and every part face-adjacent in some chain (a connector can bridge corners, since only faces count). The pad's `facing` can point anywhere, so a pad on a wall launches sideways with a 2.8 block spawn offset and a flat start.

### Frame height rules

- **None in code.** No minimum, no maximum, no relation to the missile length.
- Visual only: `bottom` plate on the lowest frame of a column, `top` open cap on the highest frame that has no frame or screen above it, `middle` otherwise.
- The cruise launcher has its own check for free space above (3 x 3 x 2, [cruise-launcher.md](cruise-launcher.md)). The ballistic pad has none.

### GregGuns note

If the rebuild wants a true multiblock, it must define it; the original provides only the adjacency graph, the spawn/render offsets and the `facing` set. A rebuilt shaft check should be at the pad's launch direction, clearance of about 4 blocks, and should be cheap to evaluate once at placement or on a status refresh rather than per shot.
