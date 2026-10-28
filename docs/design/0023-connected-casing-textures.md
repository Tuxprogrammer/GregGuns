# 0023 · Connected textures for the base casings

- Status: Accepted
- Gate: D6 (art direction, [0006](0006-art-direction.md))
- Date: 2026-10-27

## Context

The Ammunition Works, Armory Gantry and Rampart halls are built from hundreds of one-block casings, each with its own bevel, so a wall reads as a sea of tiny panels ([the Works sheet](../assets/infographics/multiblock-ammunition-works.png)). Connected textures (CTM) would draw the frame only around a wall's outer edge.

GTNH 2.9.0-RC-2 has no standalone Connected Textures Mod. Its CTM engine is the MCPatcherForge code bundled in Angelica 2.2.28 (`config/mcpatcherforge.cfg`: `connected_textures.enabled=true`, `standard=true`). Angelica's `ResourceList` scans every resource pack, mod jars included, for `assets/<namespace>/mcpatcher/ctm/**.properties`, so a mod can ship CTM data in its own jar with no Java and no pack change.

## Decision

Ship MCPatcher-format CTM data in our jar for these metas of `gregguns:casings`: Works Casing (0), Gantry Casing (3), Bastion Plating Iron, Steel, Aluminum, Carbon (7 to 10).

- `method=ctm`, the standard 47-tile layout, one properties file per meta (`matchBlocks=gregguns:casings`, `metadata=N`, `connect=block`, `tiles=0-46`) in `assets/gregguns/mcpatcher/ctm/<texture name>/`, tiles beside it. `parseTileAddress` resolves a bare tile name against the properties file's folder in the file's own namespace.
- `connect=block` joins a face only to the same block with the same metadata (`shouldConnectByBlock` tests the neighbor's metadata against the face's), so the four plating metas never join each other and need no `connectBlocks`.
- Tile 0 (isolated, and the held icon) is byte-identical to the shipped base texture, so items, NEI, hatch backgrounds and clients without Angelica look as before.
- Ram Housing, Gantry Beam, Gantry Hoist, Repair Cradle, the retired Reaction Casing and the Radar Casing stay single blocks.
- The tiles are generated (`tools/asset-gen/ctm.py`, painters in `blockart.py`): the interior (noise, ribs, motif) is the same in every tile and tiles seamlessly, and each quadrant draws its frame only on the sides that have no neighbor. The infographics read the shipped files and repeat the engine's tile choice (`ctmsel.py`, `check_ctm.py`).

## Consequences

- No Java rendering code, no new dependency, no pack change. Without Angelica the block shows its plain icon.
- Six more tile sets to keep in step: a painter change in `blockart.py` needs `ctm.py` re-run (and `CtmCasingsTest` guards tile 0).
- The spec's casings section ([multiblocks.md](../gregguns-spec/08-machines-production/multiblocks.md)) records the rules. The three multiblock sheets show the connected look.
- Not yet verified in a live client; the first playtest should check that walls join, that a hatch and the controller get a frame around them, that Iron next to Steel plating does not join, and that held and inventory icons are unchanged.

## Alternatives considered

- **`ctm_compact` / `compact_expanded`** (Angelica's 5-tile layouts): new Angelica-only code paths, not part of MCPatcher or OptiFine, so they would break on any other engine; the 47-tile layout is the documented format.
- **Chisel or CTMLib** (a Java `ICTMBlock`/renderer): a new dependency and our own rendering code for something the pack already does; Chisel's connection rules are not metadata-aware per casing.
- **`connect=tile`**: compares icons, so every casing meta would need a distinct icon; `connect=block` states the rule directly.
- **Leave the single-block look**: keeps the "sea of panels" look in the hall.
