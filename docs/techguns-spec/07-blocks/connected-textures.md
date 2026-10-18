# 07-blocks: connected textures (Chisel CTM)

## What is and is not in the Techguns source

Techguns does **not** contain its own connected-texture algorithm. The relevant pieces are:

- [`GenericBlockCTM`](block-catalogue.md#registered-blocks), `machines/OreDrillBlock` and `machines/MultiBlockMachineBlock` delegate icons and rendering to the **Chisel 2 API** class `com.cricketcraft.chisel.api.carving.CarvableHelper`. Their render type is `com.cricketcraft.chisel.api.rendering.ClientUtils.renderCTMId`.
- The client creates Chisel's `team.chisel.client.render.RendererCTM` twice:
  - in [`ClientProxy.initChisel()`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy), called from [`TGBlocks.init`](block-catalogue.md#registered-blocks) before the CTM blocks are built (commented "Workaround?");
  - in `ClientProxy.registerRenderers()`, where it is passed to `RenderingRegistry.registerBlockHandler`.
- Neither class is bundled in the source tree; they come from the Chisel mod jar. Chisel is therefore a hard client dependency ([01-core](../01-core/index.md) [CORE-03](../01-core/index.md#suspected-bugs)).
- `techguns.blocks.ctm.CarvingRegistry`, `CarvingGroup` and `CarvingVariation` are a small stand-alone implementation of Chisel's `ICarvingRegistry`, `ICarvingGroup` and `ICarvingVariation` interfaces. `TGBlocks.chiselRegistry` is set to a new `CarvingRegistry` but **nothing reads it**. It is never populated or handed to Chisel, so it is dead code. Its behaviour is described at the end for completeness.

The rest of this page describes the behaviour Techguns gets from Chisel and the texture layouts its assets follow, so a re-implementation can reproduce the visuals with its own CTM code.

## Registration flow (per block)

1. `carverHelper.addVariation(descriptionKey, meta, textureBase, "techguns")` is called for each metadata variation. Chisel looks for texture files named from `textures/blocks/<textureBase>` plus the suffixes below and picks a **texture type** from which files exist.
2. `carverHelper.registerAll(block, name)` (container, concrete, metalpanel, metalpanel2, bugnest):
   - registers the block under registry name `<name>` with Chisel's carvable item block;
   - adds every variation to the Chisel carving group `<name>`, so the Chisel chisel can convert between them;
   - evidently renames the block to `chisel.<name>`. The lang file keys are `techguns.tile.chisel.<name>.name`, e.g. "Concrete".
   - Chisel's item shows each variation's description key as a tooltip line.
3. The ore drill and multiblock machine blocks only call `addVariation` (with empty descriptions), set `forbidChiseling = true`, and register themselves with `GameRegistry` using their own item blocks.

## Texture types used by Techguns assets

All paths are relative to `assets/techguns/`.

| Type | Files | Used by |
|---|---|---|
| Plain ("normal") | `textures/blocks/<base>.png` only | concrete 0/1/5/6, metalpanel 0–7, bugnest 0, drill `drillFrame_outer`, `drillFrame`, `drillEngine`, `drillEngineController`, `invisible`, multiblock `rc_*`, `fabricator*` |
| CTM (classic 4-quadrant) | `<base>.png` plus `<base>-ctm.png` (twice the size of the base) | container 0–5, concrete 2/3, metalpanel2 0–5, drill `drillFrame_outer_c`, `drillFrame_c`, `drillEngine_c`, `drillEngineTop_c` |
| CTMV (vertical column) | `<base>-ctmv.png` (2×2 tiles) plus `<base>-top.png` (no plain base file) | concrete 4 (`concrete_old2`), drill rod (`drillRod`) |
| V9 (3×3 world-tiled pattern) | `<base>-v9.png` (3×3 tiles) | metalpanel2 6 (`metalpanel_techgrid`) |

### Classic CTM: tile layout and quadrant algorithm

**The five source images.** The base texture is the **unconnected** look: the panel with its border on all four edges. The `-ctm` sheet is a 2×2 arrangement of full tiles:

```
+-----------------------+-----------------------+
| A: fully connected    | B: connected up/down  |
|    no border at all   |    border on LEFT and |
|                       |    RIGHT edges only   |
+-----------------------+-----------------------+
| C: connected          | D: inner corners only |
|    left/right         |    no edge borders;   |
|    border on TOP and  |    small border notch |
|    BOTTOM edges only  |    in each corner     |
+-----------------------+-----------------------+
```

These assignments were confirmed against the art. For example, in `container_scifi1-ctm.png` the hazard-stripe border appears on:

- the left and right edges of the top-right tile;
- the top and bottom edges of the bottom-left tile;
- only the four corner notches of the bottom-right tile.

The top-left tile is clean plating.

**Per-face rendering.** Each block face is split into four quadrants. Each quadrant is copied from the *same* quadrant position of one of the five images, chosen from three neighbour tests made in the plane of that face. For the quadrant at a given corner, test:

- the neighbour beside it along the face's horizontal axis (H)
- the neighbour along its vertical axis (V)
- the diagonal neighbour between them (D)

| H connected | V connected | D connected | Quadrant source |
|---|---|---|---|
| no | no | – | base texture (outer corner, two borders) |
| yes | no | – | tile C (horizontal run: top/bottom border) |
| no | yes | – | tile B (vertical run: left/right border) |
| yes | yes | no | tile D (inner corner notch) |
| yes | yes | yes | tile A (seamless interior) |

"Connected" is Chisel's test: the neighbour is the same block with the same metadata. Chisel's default also refuses a connection if that neighbour's face is covered by a matching block directly in front of it. The exact rule lives in Chisel, not Techguns.

The inventory and item icon is the base texture.

### CTMV (vertical column / pillar)

- `<base>-top.png` is used on the top and bottom faces.
- For side faces, the 2×2 `-ctmv` sheet holds four full tiles, chosen by whether the same block and metadata is directly above and/or below:

| Above? | Below? | Tile | Example art (`concrete_old2-ctmv`) |
|---|---|---|---|
| no | no | top-left: single block, capped at both ends | dark rim at top, rust stain at bottom |
| no | yes | top-right: top of column | rim at top, fades down |
| yes | yes | bottom-left: middle segment | plain concrete |
| yes | no | bottom-right: bottom of column | rust stain at the bottom |

`drillRod-ctmv` follows the same layout: rounded caps where the column ends and open, ribbed sections in between.

### V9 (3×3 pattern)

`metalpanel_techgrid-v9.png` is 48×48: nine 16×16 tiles in a 3×3 grid. Each block face shows one tile, picked from the block's world coordinates modulo 3 (the two axes in the plane of the face). A large 3×3-block "tech grid" pattern therefore repeats seamlessly across walls and floors. Note the tile resolution is 16 px, not the 32 px used by most other Techguns blocks.

## Dead stand-in registry (`blocks/ctm/*`)

These are kept only so the classes can be removed or replaced knowingly.

- **`CarvingVariation`:** a value object holding block, block metadata, item metadata and order. The short constructor uses the metadata for all three.
- **`CarvingGroup`:** a name, an optional sound and ore name, and a list of variations (add and remove).
- **`CarvingRegistry`:** a linear list of groups. It supports:
  - looking up the group, variation, sibling list, ore name or sound for a (block, meta) pair;
  - looking up a group by name;
  - sorted group names;
  - add and remove of groups and variations;
  - `getItemsForChiseling`, which returns one stack per variation of the input's group, keeping the stack size.

  There is no caching. `getItemsForChiseling`, `addVariation(name, …)`, `registerOre` and `setVariationSound` throw a NullPointerException if the group does not exist.
