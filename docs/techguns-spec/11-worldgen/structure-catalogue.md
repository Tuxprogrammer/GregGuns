# Structure catalogue

This catalogue covers every structure class in `worldgen/` and `worldgen/structures/`. Shared mechanics are in [structure-system.md](structure-system.md): terrain checks, rotation, the meaning of pass 0/1, and the file format. Chest loot is in [loot.md](loot.md). NPC and mob types are cross-referenced to **[10-entities-npcs](../10-entities-npcs/index.md)**. Block definitions (concrete, lamps, TG doors, ladders, camo nets, sandbags, bug-nest blocks, spawner, chests, camp flag) belong to **[07-blocks](../07-blocks/index.md)** and **[09-machines-utility](../09-machines-utility/index.md)**.

"Chisel `X` m" below means the Chisel 2 mod's block `team.chisel.init.ChiselBlocks.X` with metadata m. Techguns references these directly, so every scanned structure and most procedural ones need Chisel installed ([WGEN-24](index.md#suspected-bugs)). [`TGChiselBlocks`](../07-blocks/special-blocks.md#19-tgchiselblocks-block-constants-for-structures) names are aliases defined in `blocks/TGChiselBlocks`:

| Alias | Block |
|---|---|
| [`concrete`](../07-blocks/block-catalogue.md#registered-blocks) | Chisel `concrete` 0 |
| `concrete_dark` | Chisel `concrete` 10 |
| `woodenCrate` | Chisel `planks[0]` 10 |
| `glowblock` | Chisel `glowstone` 4 |
| `factory_box` | Chisel `factoryblock` 9 |
| `factory_wireframe` | Chisel `factoryblock` 5 |
| `factory_hazard` | Chisel `factoryblock` 6 |
| `factory_wall` | Chisel `factoryblock` 1 |
| `technical_scaffold` | Chisel `technical` 0 |
| `wood_scaffold` | Chisel `planks[0]` 12 |
| `wood_pillar` | Chisel `planks[1]` 12 |
| `wood_wall` | Chisel `planks[0]` 1 |
| `wood_floor` | Chisel `planks[0]` 5 |
| `wood_roof` | vanilla planks 1 |
| `wood_roofSlab` | vanilla wooden_slab 1 |
| `stonebrick_panel` | Chisel `stonebricksmooth` 12 |
| `stonebrick_small` | Chisel `stonebricksmooth` 5 |
| `stonebrick_sunken` | Chisel `stonebricksmooth` 13 |
| `cobblestone_largetile` | Chisel `cobblestone` 4 |
| `glass_bubble` | Chisel `glass` 1 |

TG `concrete` metas, with their lang descriptions (block owned by [07](../07-blocks/index.md)): 0 Light, 1 Dark, 2 Steel Frame Light, 3 Steel Frame Dark, 4 Weathered Column, 5 Light Noise, 6 Dark Noise. TG [`container`](../07-blocks/block-catalogue.md#registered-blocks) meta 0 is "Shipping Container (Red)".

Palette columns: **idx** = palette index used in the data file, **rot** = rotation type (blank = none), **pass** = 1 means placed in the second pass, **n** = how many file entries use it.

---

## Part A: Structures that spawn naturally

Part A covers the 8 structures in [`TGStructureSpawnRegister`](structure-system.md#registered-spawn-table-tgstructurespawnregister-static-init). All are Overworld only (a hard-coded dimension 0 check; the register's dimension list is never consulted, [WGEN-28](index.md#suspected-bugs)), all have weight 1, and all are gated by config [`SpawnStructures`](../01-core/config.md#category-general) (default true). Each section ends with a **Sheet** table giving class and register entry, spawn conditions, size (declared and as built), build steps, palette, loot, spawners and NPCs, camp flag and caps, NBT and bug IDs.

| Id | Structure | Class | Size class | Type | Biome whitelist | Pick chance at a grid point | Loot tier |
|---|---|---|---|---|---|---|---|
| A1 | Small Factory Building | `FactoryHouseSmall` | SMALL | LAND | none | 1/2 | 0 |
| A2 | Small Train Station | `SmallTrainstation` | SMALL | LAND | none | 1/2 | 0 |
| A3 | Medium House | `HouseMedium` | MEDIUM | LAND | none | 1/2 (1/3 in sandy or wasteland) | 0 |
| A4 | Big Bunker | `BigBunker` | MEDIUM | LAND | none | 1/2 (1/3 in sandy or wasteland) | 1 |
| A5 | Alien Bug Nest | `AlienBugNestStructure` | MEDIUM | LAND | SANDY, WASTELAND | 1/3 | none |
| A6 | Military Base | `MilitaryBaseStructure` | BIG | LAND | none | 1 | parts |
| A7 | Aircraft Carrier | `AircraftCarrierNew` | BIG | WATER | none | 1/2 | 2 |
| A8 | Submarine | `Submarine` | BIG | WATER | none | 1/2 | 2 |

Grid spacing with the default config: SMALL every 16 chunks (256 blocks), MEDIUM every 32 chunks (512 blocks), BIG every 64 chunks (1024 blocks). SMALL and MEDIUM grid points in ocean biomes never spawn anything, because no SMALL or MEDIUM structure accepts WATER.

### A1. Small Factory Building (`FactoryHouseSmall`)
* **Spawn:** SMALL grid, any LAND biome, weight 1. Constructor `(8,0,7, 9,5,10)` with `setSwapXZ(true)`, `setXZSize(11,10)`. The data is actually 9 (X) × 11 (Z) ([WGEN-30](index.md#suspected-bugs)). Loot tier 0. It also appears as a military-camp building with a separate instance `(9,6,10, 16,6,16)`.
* **Build:** scanned file [`structures/factoryBuildingSmall`](assets.md#structure-data-files) (462 entries, 9×7×11). Order of operations:
  1. `cleanUpwards` range 7 (clears 7 blocks above each footprint cell).
  2. `placeFoundation` range 3 (fills air up to 3 below the ground layer with the ground-layer block).
  3. Pass 0, then pass 1.

  World Y: file y0 sits at (spawn y − 1), on the surface block layer.
* **Layout:** an L-shaped two-storey industrial building.
  * **Ground floor:** a 9×5 hall (z1–5) with a 5×5 annex on the west side (x0–4, z5–9). The floor is Chisel technical 12 inside a TG concrete 5 slab border. Walls are Chisel technical 0, factoryblock 1 and vanilla glass panes.
  * **Contents:** two vanilla chests (DUNGEON loot) side by side, one crafting table, two furnaces. TG doors (door01 meta 7/8) are on the north wall and the south end of the annex.
  * **Upper level:** a 5×5 room (x0–4, z1–5, y4–6) with a factoryblock-0 roof and lamps. The roof of the rest of the hall is a terrace edged with Chisel iron_bars 6 railings.
  * **Access:** two TG ladders (ladder01) run y1–4, at (7,·,2) and (5,·,6). A small iron-bar fenced yard sits on the ground at x6–8, z7–10.
  * **Lighting:** TG lamps of several metas.

| idx | block | rot | pass | n |
|---|---|---|---|---|
| 0 | TG concrete 5 | | | 58 |
| 1 | Chisel technical 0 | | | 82 |
| 2 | Chisel factoryblock 1 | | | 62 |
| 3 | vanilla glass_pane | | | 18 |
| 4 | Chisel technical 12 | | | 35 |
| 5 | Chisel factoryblock 9 | | | 4 |
| 6 | vanilla chest 5 | CHEST | | 2 |
| 7 | TG airMarker (→ air) | | | 133 |
| 8 | TG lamp01 4 | TG | 1 | 3 |
| 9 | TG lamp01 6 | TG | 1 | 1 |
| 10 | Chisel factoryblock 0 | | | 26 |
| 11 | crafting_table | | | 1 |
| 12 | TG door01 7 | DOOR | 0 | 2 |
| 13 | TG door01 8 (upper) | DOOR | 1 | 2 |
| 14 | TG lamp01 1 | TG | 1 | 2 |
| 15 | TG lamp01 3 | TG | 1 | 3 |
| 16 | furnace 3 | CHEST | | 2 |
| 17 | TG lamp01 5 | TG | 1 | 1 |
| 18 | TG ladder01 1 | TG | 1 | 8 |
| 19 | Chisel iron_bars 6 | | | 16 |
| 20 | TG lamp01 7 | TG | 1 | 1 |

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `FactoryHouseSmall` in `TGStructureSpawnRegister.spawns_small`: weight 1, no biome whitelist, dimension list `[0]`, type LAND, size class SMALL. Constructor `(8,0,7, 9,5,10)` then `setXZSize(11,10)`, `setSwapXZ(true)`. A second instance `(9,6,10, 16,6,16)` with swap is the camp building |
| Spawn conditions | Overworld (hard-coded dim 0 check). A SMALL grid point (chunk x and z both multiples of 16, but not both multiples of 32) whose corner biome is not in the BiomeDictionary `OCEAN` set. Pick chance at such a point is 1/2 (shares the SMALL land pool with the Small Train Station). Terrain check: the default `Structure.spawnStructureWorldgen` uses `getValidSpawnYArea` with height-difference limit 3 and step 4 over the rotated footprint, no junk removal. Config gates: config `SpawnStructures` (general, default true) registers the generator; grid spacing [`StructureSpawnWeightSmall`](../01-core/config.md#category-general) (16) / [`StructureSpawnWeightMedium`](../01-core/config.md#category-general) (32) / [`StructureSpawnWeightBig`](../01-core/config.md#category-general) (64) chunks |
| Size | Declared 11 x 10 (X x Z) in the register. As built (decoded file extent) 9 x 7 x 11. See WGEN-30 |
| Build steps | pick direction 0-3 at random; terrain check; `cleanUpwards` range 7; `placeFoundation` range 3; scanned pass 0; scanned pass 1; no flag |
| Palette | 21 entries (idx 0-20), listed in the table below. Pass 1 holds lamps, upper door halves and ladders |
| Loot | tier 0: 2 vanilla chests on `DUNGEON_CHEST`; the 2 furnaces stay empty. See [loot.md](loot.md) |
| Spawners and NPCs | none |
| Camp flag and caps | none (as a camp building it is placed by the camp, which owns the flag) |
| NBT | no structure-level NBT. Placed chests carry the normal `Items` list, filled at placement |
| Bugs | WGEN-05, [WGEN-23](index.md#suspected-bugs), WGEN-24, [WGEN-25](index.md#suspected-bugs), WGEN-30 |


### A2. Small Train Station (`SmallTrainstation`)
* **Spawn:** SMALL grid, any LAND biome, weight 1. `setXZSize(11,12)`; the data is 11×11. Loot tier 0.
* **Build:** scanned file [`structures/smallTrainstation`](assets.md#structure-data-files) (327 entries, 11×7×11). Order of operations:
  1. `cleanUpwards` range 7.
  2. `placeFoundation` range 1. This runs before placement.
  3. Pass 0, then pass 1.

  Many palette entries are [`MultiMBlock`](structure-system.md#multimblock-random-palette-entry)s, so the station looks weathered and different every time.
* **Layout:**
  * **Tracks:** two parallel vanilla rail tracks run the full 11-block length along Z at x0 and x3, on a bed of gravel and cobble/mossy cobble. Each rail cell has a ⅔ nominal chance of rail and ⅓ of air, giving broken track.
  * **Station house:** 5×7 (x5–9, z1–7) with brick walls (Chisel brickCustom 3 or 5, random per block) and windows that are randomly glass pane or air. The floor is Chisel cobblestone 10. The doorway is on the track side.
  * **Furniture:** two fence-and-pressure-plate tables, a furnace and a crafting table. A vanilla ladder (meta 3) climbs to a loft made of upper wooden slabs, where one vanilla chest sits.
  * **Roof:** a pitched gable of Chisel limestone stairs. Each roof cell randomly picks one of two limestone stair blocks or air, so the roof has holes. Ridge and corner pieces use other stair metas.

| idx | block | rot | n |
|---|---|---|---|
| 0 | gravel | | 21 |
| 1 | Multi: rail 0 (w2) / air (w1) | | 21 |
| 2 | Multi: Chisel cobblestone 2 (w2) / Chisel mossy_cobblestone 1 (w1) | | 18 |
| 3 | Multi: Chisel limestoneStairs[0] 8 (STAIRS2_CHISEL, w1) / limestoneStairs[3] 0 (STAIRS, w1) | both | 27 |
| 4 | Chisel cobblestone 10 | | 35 |
| 5 | Multi: Chisel brickCustom 3 (w2) / brickCustom 5 (w2) | | 51 |
| 6 | TG airMarker | | 58 |
| 7 | Multi: Chisel glass_pane 4 (w1) / air (w1) | | 9 |
| 8 | Multi: limestoneStairs[0] 13 (w2) / limestoneStairs[3] 5 (w2) / air (w1) | stairs only | 4 |
| 9 | Multi: Chisel limestone 1 (w2) / limestone 6 (w2) / air (w1) | | 31 |
| 10 | fence | | 2 |
| 11 | wooden_pressure_plate | | 2 |
| 12 | wooden_slab 8 (top half) | | 9 |
| 13 | ladder 3 | LADDER | 3 |
| 14 | vanilla chest 2 | CHEST | 1 |
| 15 | Multi: limestoneStairs[0] 14 / [3] 6 / air (w2/w2/w1) | stairs only | 1 |
| 16 | Multi: limestoneStairs[0] 15 / [3] 7 / air | stairs only | 1 |
| 17 | furnace 4 | CHEST | 1 |
| 18 | crafting_table | | 1 |
| 19 | Multi: limestoneStairs[0] 12 / [3] 4 / air | stairs only | 4 |
| 20 | Multi: limestoneStairs[0] 9 / [3] 1 / air | stairs only | 27 |

All entries are pass 0. The multi-stair entries rotate the limestoneStairs[0] option with the `STAIRS2_CHISEL` table and the limestoneStairs[3] option with the vanilla `STAIRS` table. The air option never rotates.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `SmallTrainstation(0,0,0,0,0,0)` in `spawns_small`: weight 1, no whitelist, dim `[0]`, LAND, SMALL, `setXZSize(11,12)` |
| Spawn conditions | As A1: Overworld, SMALL grid point, non-ocean biome, 1/2 pick chance, area check limit 3 step 4. Config gates: config `SpawnStructures` (general, default true) registers the generator; grid spacing `StructureSpawnWeightSmall` (16) / `StructureSpawnWeightMedium` (32) / `StructureSpawnWeightBig` (64) chunks |
| Size | Declared 11 x 12. As built 11 x 7 x 11 (WGEN-30) |
| Build steps | random direction; terrain check; `cleanUpwards` range 7; `placeFoundation` range 1 (before placement); pass 0 only (every palette entry is pass 0, so pass 1 places nothing) |
| Palette | 21 entries, of which 9 are `MultiMBlock` random entries (rails, mossy cobble, stairs, windows, roof air gaps). Table below |
| Loot | tier 0: 1 vanilla chest on `DUNGEON_CHEST` in the loft; 1 empty furnace |
| Spawners and NPCs | none |
| Camp flag and caps | none |
| NBT | no structure-level NBT. Placed chests carry the normal `Items` list, filled at placement |
| Bugs | WGEN-05, [WGEN-14](index.md#suspected-bugs) (random palette weighting), WGEN-23, WGEN-24, WGEN-25, WGEN-30 |


### A3. Medium House (`HouseMedium`)
* **Spawn:** MEDIUM grid, any LAND biome, weight 1. `(16,12,16, 16,12,16)`, `setXZSize(16,16)`. Loot tier 0 (T1 TG chests).
* **Build:** scanned file [`structures/houseMedium`](assets.md#structure-data-files) (1637 entries, 16×12×16). Order of operations:
  1. `cleanUpwards` range 7.
  2. Pass 0.
  3. Pass 1.
  4. `placeFoundation` range 4.
* **Layout:** an irregular two-storey stone house with an attic. The footprint is roughly a 9×13 main wing (x1–9) plus a 6×5 east wing (x9–14, z3–8). There is a small porch and garden of Chisel cobblestone 4 at the north-west, and a 3-wide entrance path at the south (z13–15).
  * **Walls:** Chisel cobblestone variants 1, 13 and 10 (base course), with vanilla glass panes and Chisel iron_bars 5 windows.
  * **Roof:** nether-brick-stair roof, nether-brick gables, and a dark-oak-plank floor in parts.
  * **Ground floor:** rooms separated by internal walls, with wooden doors. Contents: bookshelves (11), carpets (Chisel carpet 4/8/10), a wall sign, fence-and-pressure-plate tables, and a "metal storage" corner of Chisel lead, copper, tin, iron and gold storage blocks.
  * **First floor (y5–7):** two bedrooms with beds (vanilla bed 1/9 and 3/11 pairs), crafting tables, more chests and lamps.
  * **Attic (y9–10):** reached by a TG ladder at (7, 5–9, 5). It holds TG chests.
  * **Lighting:** TG lamps (metas 1, 3–7).
* **Chests:** 8 vanilla (DUNGEON), 6 [`tgchest`](../07-blocks/block-catalogue.md#registered-blocks) (Reinforced T1), 4 [`tgchest_weapon`](../07-blocks/block-catalogue.md#registered-blocks) (Weapon T1).

| idx | block | rot | pass | n | | idx | block | rot | pass | n |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | airMarker | | | 449 | | 28 | lamp01 5 | TG | 1 | 4 |
| 1 | lamp01 7 | TG | 1 | 4 | | 29 | crafting_table | | | 2 |
| 2 | nether_brick_stairs 0 | STAIRS | | 61 | | 30 | Chisel leadblock 1 | | | 1 |
| 3 | Chisel cobblestone 1 | | | 402 | | 31 | lamp01 6 | TG | 1 | 3 |
| 4 | Chisel cobblestone 10 | | | 229 | | 32 | Chisel copperblock 1 | | | 2 |
| 5 | wooden_door 4 | DOOR | 0 | 3 | | 33 | wooden_door 6 | DOOR | 0 | 2 |
| 6 | wooden_door 8 (upper) | DOOR | 1 | 8 | | 34 | Chisel iron_block 3 | | | 1 |
| 7 | glass_pane | | | 31 | | 35 | Chisel tinblock 1 | | | 1 |
| 8 | Chisel cobblestone 13 | | | 77 | | 36 | Chisel carpet 10 | | | 25 |
| 9 | nether_brick_stairs 5 | STAIRS | | 8 | | 37 | tgchest 4 | CHEST | | 6 |
| 10 | Chisel cobblestone 4 | | | 22 | | 38 | nether_brick_stairs 3 | STAIRS | | 3 |
| 11 | Chisel carpet 8 | | | 2 | | 39 | Chisel iron_block 7 | | | 2 |
| 12 | wall_sign 2 | LADDER | 1 | 1 | | 40 | Chisel gold_block 5 | | | 1 |
| 13 | oak_stairs 1 | STAIRS | | 2 | | 41 | nether_brick_stairs 4 | STAIRS | | 12 |
| 14 | bookshelf | | | 11 | | 42 | nether_brick_stairs 1 | STAIRS | | 69 |
| 15 | dark_oak_stairs 4 | STAIRS | | 3 | | 43 | wooden_door 3 | DOOR | 1 | 1 |
| 16 | nether_brick_stairs 2 | STAIRS | | 5 | | 44 | lamp01 3 | TG | 1 | 2 |
| 17 | nether_brick | | | 82 | | 45 | Chisel iron_bars 5 | | | 7 |
| 18 | dark_oak_stairs 3 | STAIRS | | 2 | | 46 | TG ladder01 1 | TG | 1 | 5 |
| 19 | dark_oak_stairs 1 | STAIRS | | 4 | | 47 | wooden_door 5 | DOOR | 0 | 1 |
| 20 | bed 9 | BED | | 1 | | 48 | lamp01 4 | TG | 1 | 2 |
| 21 | Chisel carpet 4 | | | 10 | | 49 | tgchest_weapon 3 | CHEST | | 4 |
| 22 | vanilla chest 2 | CHEST | | 8 | | 50 | wooden_door 1 | DOOR | 1 | 1 |
| 23 | fence | | | 4 | | 51 | Chisel iron_block 1 | | | 1 |
| 24 | heavy_weighted_pressure_plate | | 1 | 4 | | 52 | bed 3 | BED | | 2 |
| 25 | bed 1 | BED | | 1 | | 53 | Chisel iron_block 10 | | | 1 |
| 26 | planks 5 (dark oak) | | | 35 | | 54 | Chisel iron_block 9 | | | 1 |
| 27 | lamp01 1 | TG | 1 | 4 | | 55 | bed 11 | BED | | 2 |

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `HouseMedium(16,12,16, 16,12,16)` in `spawns_medium`: weight 1, no whitelist, dim `[0]`, LAND, MEDIUM, `setXZSize(16,16)` |
| Spawn conditions | Overworld, MEDIUM grid point (chunk x and z both multiples of 32, but not both of 64), non-ocean biome. Pick chance 1/2 normally (shares with Big Bunker), 1/3 in SANDY or WASTELAND biomes (the Alien Bug Nest joins the pool). Area check limit 3 step 4. Config gates: config `SpawnStructures` (general, default true) registers the generator; grid spacing `StructureSpawnWeightSmall` (16) / `StructureSpawnWeightMedium` (32) / `StructureSpawnWeightBig` (64) chunks |
| Size | Declared 16 x 16; as built 16 x 12 x 16 (matches) |
| Build steps | random direction; terrain check; `cleanUpwards` range 7; pass 0; pass 1; `placeFoundation` range 4 |
| Palette | 56 entries (idx 0-55), table below |
| Loot | tier 0: 8 vanilla chests (`DUNGEON_CHEST`), 6 `tgchest` (Reinforced T1), 4 `tgchest_weapon` (Weapon T1) |
| Spawners and NPCs | none |
| Camp flag and caps | none |
| NBT | no structure-level NBT. Placed chests carry the normal `Items` list, filled at placement |
| Bugs | WGEN-05, WGEN-23, WGEN-24, WGEN-25 |


### A4. Big Bunker (`BigBunker`)
* **Spawn:** MEDIUM grid, any LAND biome, weight 1. `(32,14,17, 32,14,17)`, `setXZSize(32,17)`; the data is 33×18. Loot tier **1** (T2). It overrides the rotation shift:
  * directions 1 and 3: X shift `(int)(centX/2) − 1` = 7, Z shift `−(int)(centZ/2) − 3` = −7
  * other directions: 0

  These hand-tuned values partly compensate for [WGEN-05](index.md#suspected-bugs).
* **Build:** scanned file [`structures/bigbunker`](assets.md#structure-data-files) (4332 entries, 33×14×18), passes 0 and 1. It is placed at (spawn y − 1) **−10**, so file y10 is the original ground-surface layer and y0–9 are buried. There is no foundation or cleanup step; 1681 explicit air entries hollow out the interior.
* **Layout:** a mostly buried concrete bunker under a camouflaged surface emplacement.
  * **Surface berm (y8–11):** a podzol-and-sandbag earthwork encloses a concrete-6 blockhouse with an inner concrete-0 room (x11–21, z4–13). There are four sandbag pill-boxes at the corners, each with spruce-stair steps and a TG door. Stone-brick-stair ramps at the west and east ends (x8–9, x27) lead down.
  * **Roof:** a 25×10 concrete slab (y12) with a stone-brick-stair rim, covered entirely by a TG camo-net roof layer (y13).
  * **Middle level (y3–8):** two side wings with stone-brick stairs connect to a central hall at y6–8, which floors on a concrete-0 slab at y5. Interior walls use TG concrete variants 0/1/4/6, and TG doors divide the rooms.
    * A **Zombie Hole spawner** (TG monster spawner meta 1, spawns [`ZombieSoldier`](../10-entities-npcs/per-mob.md#1-zombie-soldier-zombiesoldier), see [10](../10-entities-npcs/index.md)) is set into the y5 slab at (14,5,9).
    * Two `tgchest` and two vanilla chests stand in the hall at y6.
  * **Surface room (y10–11):** under the roof slab, holding one vanilla chest and one `tgchest`.
  * **Lowest vault (y1–2, floor y0):** a 13-wide room. A second Zombie Hole spawner is in its floor at (17,0,8). It holds 8 `tgchest_weapon` (Weapon T2) in two rows of four against the north and south walls.
  * **Lighting:** TG lamp01 meta 1.

| idx | block | rot | pass | n | | idx | block | rot | pass | n |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | dirt 2 (podzol) | | | 251 | | 16 | TG door01 8 | DOOR | 1 | 16 |
| 1 | TG sandbags | | | 296 | | 17 | TG lamp01 1 | TG | 1 | 5 |
| 2 | air | | | 1681 | | 18 | TG concrete 5 | | | 2 |
| 3 | TG concrete 6 | | | 644 | | 19 | TG door01 0 | DOOR | 0 | 12 |
| 4 | stone_brick_stairs 2 | STAIRS | | 24 | | 20 | TG door01 9 | DOOR | 1 | 12 |
| 5 | stone_brick_stairs 0 | STAIRS | | 34 | | 21 | stone_brick_stairs 4 | STAIRS | | 12 |
| 6 | stone_brick_stairs 3 | STAIRS | | 25 | | 22 | TG door01 2 | DOOR | 0 | 12 |
| 7 | TG camoNetRoof 0 | | | 250 | | 23 | tgchest_weapon 3 | CHEST | | 4 |
| 8 | TG concrete 1 | | | 315 | | 24 | tgchest_weapon 2 | CHEST | | 4 |
| 9 | TG concrete 4 | | | 294 | | 25 | TG monsterspawner 1 | | | 2 |
| 10 | stone_brick_stairs 1 | STAIRS | | 35 | | 26 | tgchest 2 | CHEST | | 2 |
| 11 | TG concrete 0 | | | 372 | | 27 | vanilla chest 3 | CHEST | | 2 |
| 12 | spruce_stairs 3 | STAIRS | | 4 | | 28 | vanilla chest 2 | CHEST | | 1 |
| 13 | TG door01 3 | DOOR | 0 | 2 | | 29 | tgchest 3 | CHEST | | 1 |
| 14 | TG door01 1 | DOOR | 0 | 2 | | 30 | stone_brick_stairs 5 | STAIRS | | 12 |
| 15 | spruce_stairs 2 | STAIRS | | 4 | | | | | | |

Spawner tile values come from [`BlockTGMonsterSpawner`](../07-blocks/special-blocks.md#monster-spawner-holes-blocktgmonsterspawner) meta 1: entity [`Techguns.ZombieSoldier`](../01-core/lifecycle-and-registration.md#lifecycle-overview), Delay 20, MinSpawnDelay 100, MaxSpawnDelay 300, SpawnCount 3, MaxNearbyEntities 5, RequiredPlayerRange 24, SpawnRange 3.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `BigBunker(32,14,17, 32,14,17)` in `spawns_medium`: weight 1, no whitelist, dim `[0]`, LAND, MEDIUM, `setXZSize(32,17)`. Sets `lootTier` 1 |
| Spawn conditions | As A3 (MEDIUM grid, non-ocean, pick chance 1/2 or 1/3 in sandy or wasteland biomes). Area check limit 3 step 4 on the rotated footprint, with the hand-tuned rotation shift described above. Config gates: config `SpawnStructures` (general, default true) registers the generator; grid spacing `StructureSpawnWeightSmall` (16) / `StructureSpawnWeightMedium` (32) / `StructureSpawnWeightBig` (64) chunks |
| Size | Declared 32 x 17; as built 33 x 14 x 18 plus 10 buried layers (WGEN-30) |
| Build steps | random direction; terrain check; place pass 0 then pass 1 at (surface y - 1) - 10; no cleanup and no foundation (1681 explicit air entries hollow the interior) |
| Palette | 31 entries (idx 0-30), table below |
| Loot | tier 1 (T2 tables): 3 vanilla chests, 3 `tgchest` (Reinforced T2), 8 `tgchest_weapon` (Weapon T2) |
| Spawners and NPCs | 2 Zombie Hole spawners (`monsterspawner` meta 1) at local (14,5,9) and (17,0,8), each spawning the Zombie Soldier mob (see [10-entities-npcs](../10-entities-npcs/index.md)) |
| Camp flag and caps | none. The spawner caps are per spawner: 5 nearby, 3 per attempt, range 24 for the player |
| NBT | spawner tile entities carry vanilla spawner NBT written by `setValues` (`EntityId`, `Delay` 20, `MinSpawnDelay` 100, `MaxSpawnDelay` 300, `SpawnCount` 3, `MaxNearbyEntities` 5, `RequiredPlayerRange` 24, `SpawnRange` 3); chests carry `Items` |
| Bugs | WGEN-05, WGEN-23, WGEN-24, WGEN-25, WGEN-30 |


### A5. Alien Bug Nest (`AlienBugNestStructure` → `AlienBugNest`)
* **Spawn:** MEDIUM grid, LAND, weight 1, only in biomes of BiomeDictionary type `SANDY` or `WASTELAND`. `setXZSize(4,4)`.
* **Terrain check:** `getValidSpawnY(x, z, 4, 4, 5)` at the chunk corner. A failure (−1) is **not** honoured; the nest is just built very low ([WGEN-09](index.md#suspected-bugs)).
* **Generation:** procedural. `new AlienBugNest(x, y−7, z, 16+rnd(16), 8, 16+rnd(16), rnd)`, then `setBlocks`. The random X/Z size only serves to centre the nest at `(x + sizeX/2, z + sizeZ/2)`. The entrance point is that centre at height `(y−7) + 4 = y − 3`.
* **Block set:**
  * `wallBlock`: MultiMBlock of TG [`bugnest`](../07-blocks/block-catalogue.md#registered-blocks) 0 (weight 16), TG [`bugnesteggs`](../07-blocks/block-catalogue.md#registered-blocks) (1), vanilla sandstone (2).
  * `wallBlock2` (tunnels): `bugnest` (12) and sandstone (1).
  * Interior: air.
  * `slimeLadder`: TG [`bugnestslimy`](../07-blocks/block-catalogue.md#registered-blocks) ("slimetrail"), a climbable wall-attached block. Its meta is set with [`BlockBugSlime.getDirectionMeta`](../07-blocks/block-catalogue.md#registered-blocks).
  * `slimyBlock`: `bugnest`, which also attaches slime trails to any air side where they can be placed.
  * Spawner: TG `monsterspawner` meta 0, "Bug Hole". It spawns `Techguns.AlienBug` (see [10](../10-entities-npcs/index.md)) with the same timing values as the Zombie Hole.
  * Eggs: TG `bugnesteggs`.
* **Segment graph:**
  * Constants: `segments = 5`, `MIN_RADIUS = 3`, `MAX_RADIUS = 10`, `startRadius = min(10, 3 + 5/2) = 5`.
  * **Entrance** (type 2, radius 2): at the centre point. It is not carved as a sphere.
  * **Main chamber** (type 0, radius 5): at `(cx + rnd(3), mainY, cz + rnd(3))`, where `mainY = entranceY − (5 + 5 + rnd(7))`, so 10–16 below the entrance. It links to the entrance.
  * **Satellite chambers** (type 1), for `i = 0..4`:
    * radius `max(3, 5 − rnd(i+1))`
    * horizontal distance from the centre `5 + r + 5 + rnd·8`, at angle `i·72°`
    * y = `mainY + rnd(15) − 10`
    * linking: satellite 0 links from main. Every later satellite links from the previous one with 50 % chance (and then also from main with 50 %); otherwise it links from main only.
    * extra child: each satellite has a 50 % chance of a further child chamber. The child has the same radius rule, lies another `5 + r + 5 + rnd·8` out at angle `i·72° ± up to 45°`, and is linked from that satellite.
* **Carving order:**
  1. **Spheres:** every non-entrance segment, reached recursively from main, gets `fillSphere2` with an air inside (`d² < (r−1)²`) and a `wallBlock` shell (`d² < r²`).
  2. **Tunnels:** for each link, a cylinder runs from the parent's sphere surface toward the child, ending at the child's radius. The link to the entrance is extended 5 blocks further, so it breaks the surface. Tunnel radius is `min(parent r, 2 + rnd(2), child r)`, with an air core and a `wallBlock2` shell. The entrance tunnel is lined with slime ladders, filling every placeable cell within radius r−1.
  3. **Interiors:** each non-entrance sphere is re-carved with air to radius r−1, which clears tunnel shells inside rooms.
     * **Satellite chambers** get eggs and one spawner:
       * The spawner goes at a random column in `[−ri, ri−1]²`, where `ri = r−2` (minus 1 when r−2 > 1), at height `posY − (r−2)`.
       * Every other column `(i, j)` in `[−(r−2), r−3]²` starts an egg pile with probability `(f_i + f_j)/2`, where `f = (r−2 − |offset|)/(r−2)`.
       * A pile climbs from the chamber floor, filling air cells. After each layer it continues with probability `(f_i + f_j)/3`, up to r layers.
     * **Main chamber:** gets a vertical **slime pillar**, a 3D line of `bugnest` blocks each with slime ladders on its four sides. The line starts just above the chamber, on the entrance side (r+1 beyond centre), and runs 2(r−1) past the centre on the far side. It is offset 1–1.66 blocks horizontally in a random direction. Its purpose is to let the player climb from the chamber up into the entrance shaft.
* **Footprint:** roughly 60×60 blocks and 20–30 blocks deep, all underground except the entrance tunnel mouth.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `AlienBugNestStructure` in `spawns_medium`: weight 1, whitelist `[SANDY, WASTELAND]`, dim `[0]`, LAND, MEDIUM, `setXZSize(4,4)`. It builds a procedural `AlienBugNest` |
| Spawn conditions | Overworld, MEDIUM grid point, corner biome of BiomeDictionary type SANDY or WASTELAND. Pick chance 1/3 there (house, bunker and nest weigh 1 each). Terrain check `getValidSpawnY` over a 4 x 4 box with limit 5, whose failure is ignored (WGEN-09). Config gates: config `SpawnStructures` (general, default true) registers the generator; grid spacing `StructureSpawnWeightSmall` (16) / `StructureSpawnWeightMedium` (32) / `StructureSpawnWeightBig` (64) chunks |
| Size | Declared 4 x 4. As built roughly 60 x 60 x 20-30 (nest centred on the chunk corner plus half of a random 16-31 extent) |
| Build steps | see the segment graph and carving order above: sphere carving, tunnels, interior re-carve, eggs, spawners, slime pillar |
| Palette | none (no data file). Blocks: `bugnest` 0, `bugnesteggs`, `bugnestslimy`, vanilla sandstone, air, `monsterspawner` meta 0 |
| Loot | none |
| Spawners and NPCs | one Bug Hole spawner (`monsterspawner` meta 0) per satellite chamber, so 5 to 10 per nest; each spawns the Alien Bug mob (see [10-entities-npcs](../10-entities-npcs/index.md)) with Delay 20, Min 100, Max 300, Count 3, Max nearby 5, Player range 24, Spawn range 3 |
| Camp flag and caps | none |
| NBT | spawner NBT as in A4; no other NBT |
| Bugs | WGEN-09 |


### A6. Military Base (`MilitaryBaseStructure` → `MilitaryCamp`)
* **Spawn:** BIG grid, LAND, weight 1. Size `32 + rnd(48)` on each axis independently (32–79).
* **Terrain check:** `getValidSpawnYArea(x, z, sx, sz, 5, step 8)` at the chunk corner. If it fails and both sizes are ≥ 32, the check is retried with both sizes reduced by 16, recursively until a size drops below 32.
* **On success:** `removeJunkInArea(x−1, z−1, sx+2, sz+2)`, then `new MilitaryCamp(maxHeightDiff 4, rnd).init(x, y, z, sx, sz)` and `setBlocks(world, rnd)`.
* **Static visual constants:**

| Constant | Value |
|---|---|
| Outer fence | Chisel `iron_bars` 2, height 2, width 1, spacing 1 to the inner area |
| Road block | gravel (edge), `TGChiselBlocks.concrete` (Chisel concrete 0) centre lanes when width ≥ 4 |
| Road width | `clamp(max(sx,sz)/8, 1, 5)` at the root, −1 per subdivision level (min 1) |
| Ground block | none (keeps world blocks) |
| NPC list | `["ArmySoldier"]` (see [10](../10-entities-npcs/index.md)) |
| Border-structure sizes | min 30, max 80 → `borderChanceX = (sx − 30)/50`, `borderChanceZ = (sz − 30)/50` |
| Corner-structure sizes | min 20, max 60 → `cornerChance = ((sx+sz)/2 − 20)/40` |
| Sub-segment limits | min 5 if min(sx,sz) < 10 else 7; max 15 if min ≤ 45 else min/3 |

* **`setBlocks`:**
  1. Colour type comes from [`BlockUtils.getBiomeType`](../01-core/utilities.md#blockutils) at the camp centre.
  2. Terrain prep (because maxHeightDiff ≠ −1): `removeJunkInArea` (+1 border), `flattenArea(…, 4)`, then the 5×5 Gaussian smoothing (+1 border).
  3. Abort if either side is < 5.
  4. Fence: two layers of iron bars around the whole perimeter, each column at its own ground height + 1 and + 2.
  5. Roads and segments are placed recursively. The flag is placed last.
* **Segment subdivision (BSP):**
  * The root segment is the area inset by 2. A segment splits along its longer axis.
  * **No split** when `size < min`, or when `size < max` and `rand > s·0.75 + centerFactor·0.25`. Here `s = √((size − min)/(max − min))` and `centerFactor = 1 − dist²(segment centre, camp centre) / dist²(camp corner, camp centre)`. Central areas therefore split more.
  * **Split count:** 2 for the root. Other segments use 2, or with 50 % chance `max(2, round(1 + rand·0.5·(size − (min+1)/2)/(roadWidth + 2)))`.
  * **Part sizes:** each part gets a weight `1 + rand`. Non-last parts get `round(w/Σw · (size − roadWidth·count))`; the last part gets the remainder. A road of the parent's width separates consecutive parts.
  * **Child direction:** non-last children face 2 (or 3 for Z splits), the last child faces 0 (or 1), so buildings face the road between them.
  * **Centre flag:** children of a centre segment inherit the centre flag. When the parent's split step ≤ 1 this goes to the middle child and also the last child. The first centre leaf reached becomes the **flag segment**.
* **Roads:**
  * Between consecutive children, a road strip is flattened (`flattenArea` maxDiff 1) and paved at ground level, edge blocks gravel and centre concrete.
  * Root-level roads extend 3 blocks past the camp edge on both sides. They also open fence gates by removing iron-bar fence blocks across the road width, up to height + 10.
  * Each road adds a **spawn point** at its centre: local `(x − campX, groundY + 1 − campY, z − campZ)`.
* **Leaf segment (not flag):**
  1. Pick a structure: the corner list if the leaf touches two camp edges (within 2 blocks) and `cornerChance > rand`; else the border list if it touches an X edge and `borderChanceX > rand`, or a Z edge and `borderChanceZ > rand`; else the regular list.
  2. Matching uses min/max bounds (−1 = unbounded), with X/Z swap allowed when `canSwapXZ`. The pick is uniform among matches. If nothing matches, an `EmptyPlane(cobblestone)` replaces the ground layer.
  3. Height: `minY` if `maxY == −1` or `maxY < minY`, else `minY + rnd(maxY + 1 − minY)`.
  4. The leaf is flattened to 0 height difference, then the structure is built at (leaf x, ground y at the leaf centre, leaf z) with the leaf's size and direction.
* **Flag:**
  * The flag segment is flattened to 0. A **camp flag** (TG [`basicMachine`](../07-blocks/block-catalogue.md#registered-blocks) meta 7, lang `techguns.tile.BasicMachine_7.name=Flag`; machine owned by [09](../09-machines-utility/index.md)) is placed at the segment centre at ground + 1.
  * Its [`CampFlagTileEnt.init`](../09-machines-utility/shared-rendering.md#1-basicmachine-metas-owned-by-area-09) receives the camp size, the flag offset from the camp origin, the spawn-point list and `["ArmySoldier"]`.
  * Resulting NPC behaviour ([09](../09-machines-utility/index.md)): `maxNPCs = ((sx+sz)/2)·0.75`. Every 60 ticks the flag counts NPCs inside the camp box, expanded by 8 and from −3 to +20 in Y. While under the cap, it spawns one NPC every 200 ticks at a random spawn point.
  * Breaking the flag spawns an [`AttackHelicopter`](../10-entities-npcs/per-mob.md#16-attack-helicopter-attackhelicopter-boss) 50 blocks above it (see [10](../10-entities-npcs/index.md)).

**Camp structure lists** (instances are shared statics):

| List | Structure (min X,Y,Z / max X,Y,Z, extra) | swap |
|---|---|---|
| regular | Tent (5,4,5 / 7,5,7, type 0) | |
| regular | Tent (3,4,3 / 6,5,6, type 1) | |
| regular | Tent (4,4,4 / 8,6,8, type 2) | |
| regular | Containers (2,3,2 / 7,3,7, width 2, height 2, minLen 2, TG `container`) | |
| regular | CampProps (1,3,1 / 5,5,5, variant 0: crates) | |
| regular | CampProps (3,5,3 / 6,7,6, variant 1: crates + roof) | |
| regular | Bunker (5,3,5 / 8,3,8, type 0: open) | |
| regular | Barracks (7,7,7 / −1,−1,−1) | |
| regular | Helipad (7,7,7 / 10,−1,10) | |
| regular | Tanks (4,5,4 / 10,−1,10) | |
| regular | CampProps (3,5,3 / 10,7,10, variant 5: sandbags + roof + crates) | |
| regular | Bunker (3,3,5 / 15,3,15, type 2: closed) | yes |
| regular | Bunker (4,3,6 / 15,3,15, type 3: closed + roof sandbags) | yes |
| regular | FactoryHouseSmall (9,6,10 / 16,6,16) (scanned, A1) | yes |
| border | Bunker (5,3,5 / 8,3,8, type 0) | |
| border | Bunker (5,4,5 / −1,4,−1, type 1: roof sandbags + ladder) | |
| border | CampProps (3,2,3 / 8,2,8, variant 2: sandbag ring) | |
| border | CampProps (3,4,3 / 8,5,8, variant 3: sandbags + roof) | |
| border | CampProps (1,2,3 / −1,4,−1, variant 4: sandbag row) | yes |
| border | Bunker (3,3,5 / 10,3,10, type 2) | yes |
| border | Bunker (4,3,6 / 10,3,10, type 3) | yes |
| corner | WatchTowerSmall (3,8,3 / 4,8,−1, tower 3) | yes |
| corner | WatchTowerSmall (4,10,4 / −1,12,−1, tower 4) | |

The camp component structures are described in Part B.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `MilitaryBaseStructure(0,0,0,0,0,0)` in `spawns_big`: weight 1, no whitelist, dim `[0]`, LAND, BIG. No `setXZSize`; `getSizeX/Z` return `32 + nextInt(48)` each. `getStep()` = 8 |
| Spawn conditions | Overworld, BIG grid point (chunk coordinates both multiples of 64), corner biome not OCEAN. It is the only BIG land structure, so the pick chance is 1. Terrain: `getValidSpawnYArea` with limit 5 and step 8 at the unrotated chunk corner; a result of 0 or less counts as failure; on failure, if both sizes are still >= 32 both are cut by 16 and the check repeats. Config gates: config `SpawnStructures` (general, default true) registers the generator; grid spacing `StructureSpawnWeightSmall` (16) / `StructureSpawnWeightMedium` (32) / `StructureSpawnWeightBig` (64) chunks |
| Size | Declared: random 32-79 per axis. As built: the same, or 16 less per failed retry, so 16-79 (a retry stops once a side is below 32). The camp aborts if either side is below 5 |
| Build steps | see above: junk removal, flatten (max diff 4), 5x5 Gaussian smoothing, fence, BSP subdivision with roads, leaf structures, flag. The camp ignores the direction (no rotation) |
| Palette | none (procedural). Road: gravel edge and Chisel concrete centre; fence: Chisel `iron_bars` 2; camp parts use the blocks listed in Part B |
| Loot | from the parts: camp props and closed bunkers use tier 2 (T3 tables); barracks use `techgunsMilBaseBarracks`; the small factory building inside a camp uses tier 0 |
| Spawners and NPCs | NPCs through the camp flag: [`ArmySoldier`](../10-entities-npcs/per-mob.md#6-army-soldier-armysoldier) only (see [10-entities-npcs](../10-entities-npcs/index.md)). No spawner blocks |
| Camp flag and caps | one `basicMachine` meta 7 flag at the centre leaf; `maxNPCs` = int(((sx+sz)/2) x 0.75), so 24 for a 32 x 32 camp and 59 for a 79 x 79 camp (cap recomputed identically on reload); counted every 60 ticks inside the camp box expanded by 8, Y -3 to +20; one spawn per 200 ticks while under the cap. Breaking the flag spawns an Attack Helicopter 50 blocks above it |
| NBT | flag tile entity keys `sizeX`, `sizeZ`, `xoffset`, `yoffset`, `zoffset`, `spawnPositions`, `npcListSize`, `npcName0..n`; see [09-machines-utility](../09-machines-utility/misc-tile-entities.md) |
| Bugs | [WGEN-04](index.md#suspected-bugs), [WGEN-12](index.md#suspected-bugs), [WGEN-15](index.md#suspected-bugs), [WGEN-16](index.md#suspected-bugs), [WGEN-26](index.md#suspected-bugs), [WGEN-27](index.md#suspected-bugs), [WGEN-32](index.md#suspected-bugs), [WGEN-33](index.md#suspected-bugs) |


### A7. Aircraft Carrier (`AircraftCarrierNew`)
* **Spawn:** BIG grid, WATER (ocean biome), weight 1 (shares the pool with the Submarine). `(54,24,21, 54,24,21)`, `setXZSize(54,21)`. Loot tier **2** (T3).
* **Terrain check:** `getValidSpawnYWater(chunk x, chunk z, 54, 21, 2)`. All four corners of the *unrotated* 54×21 box must be liquid-topped, and their surface heights must differ from corner 0 by less than 2. The structure is then rotated randomly around (27,10), which for directions 1/3 moves it off the checked area ([WGEN-06](index.md#suspected-bugs)).
* **Build:** scanned file [`structures/airCraftCarrier`](assets.md#structure-data-files) (3921 entries, 54×24×21) at (water-surface air y) **−3**. File y2 is at the waterline and the flight deck (y7) sits 4 above the water surface. Passes 0 and 1, then the flag.
* **Layout:**
  * **Hull:** about 54 long (X) × 21 wide (Z). The bow at low X tapers to a point and the stern at x≈53 is square.
  * **Lower decks (y0–6):** steel hull (Chisel factoryblock 2 bottom and plating, factoryblock 15 and 4 trim) and interior compartments with TG doors. A hangar level (y3–6) has TG lamps, crafting tables, Chisel technical 13 floors and an elevator-style shaft at x36–41.
  * **Flight deck (y7):** Chisel factoryblock 3 over the whole length, with factoryblock 6 rim strips at both ends.
  * **Island superstructure:** on the north (low-Z) side at x25–47, z2–9, rising to y19. It has bridge windows of Chisel glass 12, glass panes, iron-bar railings and catwalks.
  * **Antenna mast:** Chisel iron_bars 3, up to y23 at x44.
  * **Access:** TG ladders and aluminium stairs connect the levels.
* **Chests:**
  * 6 vanilla (DUNGEON): two in the forward hold at (15,1,9–10), two on the hangar level at (30,4,9–10) and two in the island at y12
  * 8 `tgchest` (Reinforced T3)
  * 12 `tgchest_weapon` (Weapon T3)
* **Flag and NPCs:**
  * A camp flag (TG `basicMachine` 7) is placed at local (24, y+8, 9), on the flight deck, rotated like everything else.
  * The tile is initialised with `init(54, 21, 24, 8, 9, spawnPoints, ["ArmySoldier","Commando"], maxNPCs 9)`. The offset passed is unrotated ([WGEN-10](index.md#suspected-bugs)), and maxNPCs is not persisted ([WGEN-11](index.md#suspected-bugs)).
  * The 9 spawn points are given as local (x, y, z), each x/z rotated with the structure: (22,8,9), (28,12,6), (28,16,6), (43,8,6), (43,12,6), (18,4,9), (18,1,9), (43,4,9), (39,1,9).

| idx | block | rot | pass | n | | idx | block | rot | pass | n |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Chisel factoryblock 6 | | | 108 | | 19 | TG door01 8 | DOOR | 1 | 18 |
| 1 | Chisel factoryblock 3 | | | 900 | | 20 | Chisel factoryblock 12 | | | 412 |
| 2 | Chisel factoryblock 2 | | | 912 | | 21 | Chisel glass 12 | | | 32 |
| 3 | Chisel factoryblock 15 | | | 308 | | 22 | Chisel iron_bars 3 | | | 192 |
| 4 | Chisel factoryblock 4 | | | 297 | | 23 | Chisel technical2 0 | | | 79 |
| 5 | crafting_table | | | 2 | | 24 | TG lamp01 1 | TG | 1 | 6 |
| 6 | tgchest 5 | CHEST | | 2 | | 25 | TG door01 0 | DOOR | 0 | 10 |
| 7 | TG lamp01 4 | TG | 1 | 16 | | 26 | TG door01 1 | DOOR | 0 | 4 |
| 8 | TG lamp01 6 | TG | 1 | 18 | | 27 | TG door01 3 | DOOR | 0 | 4 |
| 9 | vanilla chest 5 | CHEST | | 4 | | 28 | vanilla chest 3 | CHEST | | 1 |
| 10 | air | | | 352 | | 29 | vanilla chest 2 | CHEST | | 1 |
| 11 | tgchest_weapon 3 | CHEST | | 4 | | 30 | Chisel aluminumStairs[1] 1 | STAIRS | | 3 |
| 12 | tgchest_weapon 2 | CHEST | | 8 | | 31 | Chisel aluminumStairs[1] 0 | STAIRS | | 4 |
| 13 | Chisel glass_pane 4 | | | 28 | | 32 | TG ladder01 1 | TG | 1 | 11 |
| 14 | Chisel technical 5 | | | 48 | | 33 | Chisel iron_block 14 | | | 12 |
| 15 | TG lamp01 3 | TG | 1 | 13 | | 34 | Chisel technical 13 | | | 64 |
| 16 | TG lamp01 5 | TG | 1 | 16 | | 35 | tgchest 3 | CHEST | | 2 |
| 17 | TG door01 2 | DOOR | 0 | 11 | | 36 | Chisel technical2 1 | | | 4 |
| 18 | TG door01 9 | DOOR | 1 | 11 | | 37 | tgchest 4 | CHEST | | 4 |

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `AircraftCarrierNew(54,24,21, 54,24,21)` in `spawns_big`: weight 1, no whitelist, dim `[0]`, type WATER, BIG, `setXZSize(54,21)`. `lootTier` 2. Overrides `spawnStructureWorldgen` |
| Spawn conditions | Overworld, BIG grid point, corner biome of type OCEAN. Pick chance 1/2 (shares the BIG water pool with the Submarine). Terrain: `getValidSpawnYWater` limit 2 over the unrotated 54 x 21 box at the chunk corner (WGEN-06). Config gates: config `SpawnStructures` (general, default true) registers the generator; grid spacing `StructureSpawnWeightSmall` (16) / `StructureSpawnWeightMedium` (32) / `StructureSpawnWeightBig` (64) chunks |
| Size | Declared 54 x 21; as built 54 x 24 x 21 (matches) |
| Build steps | random direction; water check; pass 0 and pass 1 at (water-surface y - 3); then the camp flag via `setBlockRotated` at local (24, y+8, 9) |
| Palette | 38 entries (idx 0-37), table below |
| Loot | tier 2 (T3 tables): 6 vanilla, 8 `tgchest` (Reinforced T3), 12 `tgchest_weapon` (Weapon T3) |
| Spawners and NPCs | no spawner blocks. NPCs through the flag: `ArmySoldier` and [`Commando`](../10-entities-npcs/per-mob.md#9-commando-commando) (see [10-entities-npcs](../10-entities-npcs/index.md)), 9 spawn points |
| Camp flag and caps | flag `init(54, 21, 24, 8, 9, spawnPoints, [ArmySoldier, Commando], 9)`: cap 9 at generation, 28 after a reload (WGEN-11); unrotated flag offset (WGEN-10) |
| NBT | flag NBT as in A6; chests `Items` |
| Bugs | WGEN-04, WGEN-06, WGEN-10, WGEN-11, WGEN-23, WGEN-24, WGEN-25 |


### A8. Submarine (`Submarine`)
* **Spawn:** BIG grid, WATER, weight 1. Default constructor, `setXZSize(33,7)`. The data is 7 (X) × 33 (Z), so the declared size is transposed ([WGEN-07](index.md#suspected-bugs)). Loot tier **2** (T3).
* **Terrain check:** `getValidSpawnYUnderwater(chunk x, chunk z, 33, 7, 2)`. The four corners' sea-floor heights must be within < 2 of corner 0, and the median is used. Nothing checks that water is actually present ([WGEN-08](index.md#suspected-bugs)).
* **Build:** scanned file [`structures/submarine`](assets.md#structure-data-files) (992 entries, 7×10×33). It is placed at the floor block y (file y0 replaces the sea-floor top), with a random rotation around (16,3). Passes 0 and 1.
* **Layout:** a sunken submarine lying on the sea floor, pointing along Z.
  * **Hull:** round section about 7 wide × 7 tall, 33 long, built from TG [`metalpanel`](../07-blocks/block-catalogue.md#registered-blocks) 3. The stern is at z0–1, with a vertical rudder fin and a horizontal fin row carrying a Chisel technical 14 hub. There is a band of [`metalpanel2`](../07-blocks/block-catalogue.md#registered-blocks) 3 plating aft at z5–7, and the bow tapers at z29–32.
  * **Conning tower (sail):** at z23–25, rising to y9, topped by a vanilla trapdoor hatch (meta 9, LEVER rotation). A TG ladder (ladder01 0) runs down from y8 to the upper deck.
  * **Interior, two decks:**
    * **Lower deck (y1–3):** Chisel technical 5 walls. It holds 2 `tgchest` aft at z8–9, 2 `tgchest_weapon` (z15, z24), a ladder up at z9, and a Chisel warningSign.
    * **Upper deck (y4–5):** floored with Chisel factoryblock 4 grating. It has bunks (3 vanilla beds, meta 3/11), 3 vanilla chests and 2 `tgchest_weapon`. TG door01 bulkhead doors are at z10, z19 and z25.
  * **Lighting:** TG lamps. Chisel planks are used for the furniture.
* **Chests:** 3 vanilla (DUNGEON), 2 `tgchest` (Reinforced T3), 4 `tgchest_weapon` (Weapon T3).

| idx | block | rot | pass | n | | idx | block | rot | pass | n |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | TG metalpanel 3 | | | 597 | | 13 | TG door01 8 | DOOR | 1 | 5 |
| 1 | Chisel technical 5 | | | 75 | | 14 | Chisel technical 14 | | | 1 |
| 2 | air | | | 187 | | 15 | Chisel warningSign 0 | | | 1 |
| 3 | TG ladder01 0 | TG | 1 | 8 | | 16 | TG lamp01 3 | TG | 1 | 1 |
| 4 | Chisel factoryblock 9 | | | 4 | | 17 | TG lamp01 4 | TG | 1 | 1 |
| 5 | tgchest_weapon 5 | CHEST | | 2 | | 18 | bed 3 | BED | | 3 |
| 6 | Chisel planks[0] 10 | | | 3 | | 19 | trapdoor 9 | LEVER | | 1 |
| 7 | Chisel planks[1] 10 | | | 2 | | 20 | tgchest 4 | CHEST | | 2 |
| 8 | Chisel factoryblock 4 | | | 53 | | 21 | Chisel aluminumStairs[1] 2 | STAIRS | | 1 |
| 9 | TG metalpanel2 3 | | | 24 | | 22 | bed 11 | BED | | 3 |
| 10 | Chisel aluminumStairs[1] 3 | STAIRS | | 2 | | 23 | vanilla chest 4 | CHEST | | 3 |
| 11 | Chisel factoryblock 8 | | | 6 | | 24 | tgchest_weapon 4 | CHEST | | 2 |
| 12 | TG door01 3 | DOOR | 0 | 5 | | | | | | |

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `Submarine()` in `spawns_big`: weight 1, no whitelist, dim `[0]`, WATER, BIG, `setXZSize(33,7)`. `lootTier` 2. Overrides `spawnStructureWorldgen` |
| Spawn conditions | Overworld, BIG grid point, corner biome of type OCEAN. Pick chance 1/2. Terrain: `getValidSpawnYUnderwater` limit 2 on a 33 x 7 box at the chunk corner (WGEN-07, WGEN-08). Config gates: config `SpawnStructures` (general, default true) registers the generator; grid spacing `StructureSpawnWeightSmall` (16) / `StructureSpawnWeightMedium` (32) / `StructureSpawnWeightBig` (64) chunks |
| Size | Declared 33 x 7 (transposed); as built 7 x 10 x 33 |
| Build steps | random direction; sea-floor check; pass 0 and pass 1 at the floor block y, rotated around (16,3); no foundation, no flag |
| Palette | 25 entries (idx 0-24), table below |
| Loot | tier 2 (T3 tables): 3 vanilla, 2 `tgchest` (Reinforced T3), 4 `tgchest_weapon` (Weapon T3) |
| Spawners and NPCs | none |
| Camp flag and caps | none |
| NBT | no structure-level NBT. Placed chests carry the normal `Items` list, filled at placement |
| Bugs | WGEN-06, WGEN-07, WGEN-08, WGEN-23, WGEN-24, WGEN-25 |


---

## Part B: Military-camp component structures (procedural)

Part B covers the 9 procedural classes that `MilitaryCamp` places into the leaves of its subdivision: `Tent`, `Containers`, `CampProps`, `Bunker`, `Barracks`, `Helipad`, `Tanks`, `WatchTowerSmall` and `EmptyPlane`. None is in the spawn register, none checks terrain of its own (the camp flattens each leaf to 0 height difference first), and none reads the world seed. Their NPCs, flag and caps belong to the camp (A6). Each section ends with a **Sheet** table.

All of these receive `(posX, posY = leaf ground y, posZ, sizeX, height, sizeZ, direction, colorType, rnd)`. Direction is the side the entrance faces: 0 = −X, 1 = −Z, 2 = +X, 3 = +Z. Camo colour comes from `colorType`: DESERT → camo meta 2, SNOW → 1, otherwise 0.

### B1. Tent (`Tent`, types 0/1/2)
* **Type 0** first insets the area by 1 on every side.
* **Shell:**
  * y0: gravel floor
  * y1 to sizeY−2: TG [`camoNetPane`](../07-blocks/block-catalogue.md#registered-blocks) walls around the perimeter, using the camo meta
  * y sizeY−1: TG [`camoNetRoof`](../07-blocks/block-catalogue.md#registered-blocks) over the whole area, using the camo meta
* **Types 0 and 1:** a 2-high doorway (air) in the middle of the facing wall. Type 0 also lays one gravel block outside the door.
* **Type 2 (open pavilion):** on the facing wall, every wall cell between the corners is removed except every third one. The test is `z % 3 == 0` (or `x % 3 == 1` on the X sides), which leaves the poles.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `Tent(minX..maxZ, type)`. Not in the world spawn register. Three camp-list instances: `(5,4,5 / 7,5,7)` type 0, `(3,4,3 / 6,5,6)` type 1, `(4,4,4 / 8,6,8)` type 2, all in the camp's regular list, no swap |
| Spawn conditions | only when `MilitaryCamp` picks it for a leaf whose size fits the min/max box. No biome, dimension or terrain check of its own; the leaf is flattened to 0 height difference first. Config gate: `SpawnStructures` |
| Size | X 5-7 / 3-6 / 4-8, Z the same, height 5 / 5 / 6 as the declared range; built to the leaf size and the picked height. Type 0 insets the area by 1 on each side first |
| Build steps | gravel floor y0; camo-net-pane walls y1 to sizeY-2; camo-net roof at sizeY-1; doorway (types 0, 1) or pole pattern (type 2) |
| Palette | none (procedural): `camoNetPane`, `camoNetRoof` (meta from biome: woodland 0, snow 1, desert 2), gravel, air |
| Loot, spawners, NPCs | none |
| Camp flag and caps | none |
| NBT | none |
| Bugs | WGEN-32 (colour from the camp, vanilla biome list only) |


### B2. Containers (`Containers`)
Parallel rows of shipping containers made of TG `container` block (meta 0, red). Each row is `containerWidth` (2) wide and `containerHeight` (2) high, placed at y+1 and running the full length of the longer axis. Rows are spaced width+1 apart. Row count is `floor((shortSide + 1)/(width + 1))`.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `Containers(minX..maxZ, width, height, minLength, block)` with a second constructor taking only the block (width 2, height 2, min length 2, default block `iron_block`). The camp instance is `(2,3,2 / 7,3,7)`, width 2, height 2, min length 2, block [`TGBlocks.container`](../07-blocks/block-catalogue.md#registered-blocks) (meta 0). Camp regular list, no swap |
| Spawn conditions | only via `MilitaryCamp` leaf matching: X and Z 2-7, height exactly 3. No terrain check of its own. Uses no direction, so it never rotates |
| Size | declared 2-7 x 3 x 2-7; as built: full leaf extent on the long axis, rows 2 wide and 2 high at y+1 and y+2; leftover width after the last row (up to 2 blocks) stays empty. If the short side is 1 (not possible with the declared minimum 2) the row count is 0 |
| Build steps | choose the axis: if sizeX > sizeZ rows run along X and stack along Z, otherwise rows run along Z and stack along X (equal sizes run along Z). Row count = floor((short + 1) / (width + 1)). Each row is one solid box fill of the container block, `height` blocks tall starting at y+1; rows are `width + 1` apart so one empty gap lane separates them |
| Palette | one block: `container` meta 0, lang `techguns.tile.container.0.desc` = Shipping Container (Red) (parent name key `techguns.tile.chisel.container.name` = Container Block), see [07-blocks](../07-blocks/index.md). Rows are solid (not hollow), with no door and no interior |
| Loot, spawners, NPCs | none |
| Camp flag and caps | none |
| NBT | none |
| Bugs | [WGEN-35](index.md#suspected-bugs) (the minimum-length parameter is stored but never read) |


### B3. Camp props (`CampProps`, variants 0–5)
Loot tier 2 (T3). Crate blocks are `factory_box` (Chisel factoryblock 9) or `woodenCrate` (Chisel planks[0] 10).
* **Crate pile (used by variants 0, 1, 5):**
  * Gravel is laid over the area at y0.
  * Each column then rolls once: 5 % `tgchest`, next 5 % `tgchest_weapon` (random facing meta 2–5) at y1. Otherwise, for y = 1 … sizeY−1, a random crate is placed with 50 % chance when the block below is not "free space" (air, snow layer or TG lamp).
  * Lantern: with probability `((sx+sz)/2)/6`, one TG [`lamp01`](../07-blocks/block-catalogue.md#registered-blocks) meta 7 is set on top of a pile. From the top down, it tries 3 random columns per level for an air cell above a non-free cell.
* **Roof:** vanilla fence posts at the 4 corners, height sizeY−2 from y+1. A full TG `camoNetRoof` layer (camo meta) sits at y+sizeY−1.
* **Sandbag ring:** TG [`sandbags`](../07-blocks/block-catalogue.md#registered-blocks) around the perimeter at y+1, with a 1-block gap in the middle of the facing side.
* **Sandbag row:** a stepped sandbag wall along the centre line of the longer axis. Level y (1 … sizeY−1) spans from y to length−y−1, so each level is one block shorter at each end.

| Variant | Contents |
|---|---|
| 0 (and default) | crate pile |
| 1 | roof + crate pile inset by 1 (height −2) |
| 2 | sandbag ring |
| 3 | roof (from y+1, height −1) + sandbag ring |
| 4 | sandbag row |
| 5 | roof + sandbag ring + crate pile inset by 1 |

The 6-arg constructor leaves variant −1, which is resolved once to a random 0/1 and then stays fixed on the shared instance. No camp entry uses it.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `CampProps(minX..maxZ, variant)`, `lootTier` 2. Camp instances: variant 0 `(1,3,1 / 5,5,5)`, variant 1 `(3,5,3 / 6,7,6)`, variant 5 `(3,5,3 / 10,7,10)` in the regular list; variant 2 `(3,2,3 / 8,2,8)`, variant 3 `(3,4,3 / 8,5,8)`, variant 4 `(1,2,3 / -1,4,-1)` with swap in the border list |
| Spawn conditions | only via `MilitaryCamp` leaf matching (regular list for 0, 1, 5; border list for 2, 3, 4). No own terrain check |
| Size | per instance as listed; built to the leaf size and the picked height |
| Build steps | see the variant table: crate pile, corner-post roof, sandbag ring or row |
| Palette | none (procedural): Chisel `factoryblock` 9, Chisel `planks` 10, gravel, vanilla fence, TG `camoNetRoof`, TG `sandbags`, TG `lamp01` 7, `tgchest`, `tgchest_weapon` |
| Loot | tier 2: per crate-pile column 5 % `tgchest` (Reinforced T3) and 5 % `tgchest_weapon` (Weapon T3) |
| Spawners and NPCs | none |
| Camp flag and caps | none |
| NBT | chest `Items` only |
| Bugs | WGEN-24 (Chisel crates), WGEN-25 (unseeded loot roll) |


### B4. Small bunker (`Bunker`, types 0–3)
Loot tier 2 (T3).
* **Thin areas:** when sizeX ≤ 4 and sizeZ ≥ 5 with direction 0/2, or the mirrored case, the direction is turned by +1.
* **Entrance space:** the facing side loses 2 blocks of length to make room for the entrance stairs.
* **Layers** (posY = ground top; the bunker floor is one block sunk):
  * y−1: TG concrete 5 floor
  * y0: TG concrete 6 wall ring, interior air
  * y1: types 0/1 are firing-slit walls (wall only at corners and where the index along the wall is a multiple of 3); types 2/3 are a solid ring
  * y2: concrete 6 ring + TG concrete 3 roof
  * y3: TG sandbags parapet ring (types 1 and 3 only)
* **Entrance:**
  * A TG `door01` is placed with the vanilla door placer in the middle of the facing wall.
  * Concrete 6 jambs on both sides at y0–1. The doorway is extended outward 2 blocks with walls on both sides at y−1..0.
  * Two TG `lamp01` (meta 0) sit on the jamb tops at y2 outside.
  * The concrete floor continues 1 block outward. A vanilla stone stair 2 blocks out steps up to ground level.
* **Inner lamp:** types 2/3 put one beside the door inside at y1; types 0/1 put one at the centre at y1.
* **Crates (types 2/3):**
  * They fill the interior inset by 2: per column 5 % `tgchest`, 5 % `tgchest_weapon` (meta 2–5, placed at y0).
  * Otherwise, for y0 and y1, a 50 % chance of a random crate when the block below is not air.
  * This uses `world.rand`.
* **Roof ladder (types 1/3):**
  * TG [`ladder01`](../07-blocks/block-catalogue.md#registered-blocks) (meta 0 + 2/0/3/1 for directions 0/1/2/3) climbs y0–2 in the inner corner opposite the entrance.
  * The roof cell above it is opened, and a wall block is put behind the ladder at y1.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `Bunker(minX..maxZ, type)`, `lootTier` 2. Camp regular list: type 0 `(5,3,5 / 8,3,8)`, type 2 `(3,3,5 / 15,3,15)` swap, type 3 `(4,3,6 / 15,3,15)` swap. Border list: type 0 `(5,3,5 / 8,3,8)`, type 1 `(5,4,5 / -1,4,-1)`, type 2 `(3,3,5 / 10,3,10)` swap, type 3 `(4,3,6 / 10,3,10)` swap |
| Spawn conditions | only via `MilitaryCamp` leaf matching; also reused by the dormant village piece. Direction is given by the camp (faces the road) |
| Size | as the ranges above; the facing side loses 2 blocks of length for the entrance stairs |
| Build steps | see layers above: floor, wall ring, slit or solid second layer, roof, optional sandbag parapet, entrance door, lamps, crates (types 2 and 3), roof ladder (types 1 and 3) |
| Palette | none (procedural): TG `concrete` 5 / 6 / 3, TG `sandbags`, TG `door01`, TG `lamp01` 0, TG `ladder01`, vanilla stone stairs, `tgchest`, `tgchest_weapon` |
| Loot | tier 2 (T3): types 2 and 3 only, per interior column 5 % `tgchest` and 5 % `tgchest_weapon` at y0, plus a 50 % random crate chance |
| Spawners and NPCs | none |
| Camp flag and caps | none |
| NBT | chest `Items` only |
| Bugs | WGEN-25 (crate and chest rolls use `world.rand`) |


### B5. Barracks (`Barracks`)
The height is fixed at 7.
* **Size:** the facing side loses 2 for the ramp. Each side is then snapped to the form 4k+1 (`size −= (size−1) % 4`).
* **Layers:**

| y | Content |
|---|---|
| 0 | gravel ground |
| 1 | stilts: `wood_pillar` at every 4th x and z |
| 2 | floor: `wood_scaffold` border, `wood_floor` inside |
| 3–4 | walls on the perimeter: scaffold at 4-grid posts, `wood_wall` elsewhere, vanilla glass windows at y4 where x%4==2 or z%4==2 |
| 5 | scaffold perimeter ring |
| 6 | roof: `wood_roofSlab` perimeter, `wood_roof` inside |

* **Entrance:**
  * A wooden door at y3 in the middle of the facing wall. Its offset is `(size/8)·4 + 2` along the wall.
  * A TG `lamp01` meta 7 above the door outside at y5.
  * Two oak stairs form a ramp down to the ground (y2 and y1, one and two blocks out).
* **Inside:**
  * **Inner perimeter ring (1 in from the walls):**
    * Lamps (lamp01 0) at y5 where x%4==2 or z%4==2.
    * At y3, where x%4==0 or z%4==0 (excluding the door row and column): **vanilla chests** with `techgunsMilBaseBarracks` loot, using `world.rand`.
  * **Second ring:** when the building is longer than 4 in either axis, **beds** at y3 at cells where x%4==2 and z%4==2. Each bed's foot is against the wall it faces.
  * **Ceiling lamps:** at y5 at multiples of 8.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `Barracks(7,7,7 / -1,-1,-1)` in the camp regular list; `lootTier` is left at its default 0 but is unused because barracks chests use a named table |
| Spawn conditions | only via `MilitaryCamp` leaf matching (any leaf of at least 7 x 7 that is not taken by a more specific pick) |
| Size | declared minimum 7 x 7, no maximum; height fixed 7; each horizontal side is snapped down to 4k + 1 after the entrance ramp takes 2 from the facing side |
| Build steps | see the layer table above |
| Palette | none (procedural): `wood_pillar`, `wood_scaffold`, `wood_floor`, `wood_wall`, `wood_roof`, `wood_roofSlab` (Chisel planks aliases), vanilla glass, wooden door, oak stairs, TG `lamp01` 0 / 7, vanilla chests, beds |
| Loot | vanilla chests on the `techgunsMilBaseBarracks` table (count 1-4; combat armour, gas masks, glider and air tanks, see [loot.md](loot.md)); rolled with `world.rand` |
| Spawners and NPCs | none |
| Camp flag and caps | none |
| NBT | chest `Items` only |
| Bugs | WGEN-24 (Chisel planks), WGEN-25 |


### B6. Helipad (`Helipad`)
* Requires ≥ 7×7. Larger areas are centred down to 9×9.
* For an exact 9×9 area, a Chisel factoryblock 5 (wireframe) border is laid at y0 without corners, and the pad is inset to 7×7. Otherwise the pad is 7×7 at the origin.
* **Pad (y+1):** `factory_hazard` (factoryblock 6) border, `concrete_dark` (Chisel concrete 10) inside.
* **"H" in `glowblock` (Chisel glowstone 4):** cells (2..4, 2), (3, 3), (2..4, 4), relative to the pad.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `Helipad(7,7,7 / 10,-1,10)` in the camp regular list |
| Spawn conditions | only via `MilitaryCamp` leaf matching (7 to 10 square, no height limit) |
| Size | declared 7-10 x any x 7-10; as built a 7 x 7 pad (9 x 9 with its border in an exact 9 x 9 area), one block above y0 |
| Build steps | optional wireframe border at y0, hazard border and dark-concrete fill at y+1, glowstone "H" |
| Palette | none (procedural): Chisel `factoryblock` 5, `factory_hazard` (factoryblock 6), `concrete_dark` (Chisel concrete 10), `glowblock` (Chisel glowstone 4) |
| Loot, spawners, NPCs, flag | none |
| NBT | none |
| Bugs | WGEN-24 (Chisel blocks) |


### B7. Tanks (`Tanks`)
Horizontal fuel tanks 4 wide, repeated every 5 blocks across the short side. Each tank runs the full long side. Per tank, from y+1:
* y1: `technical_scaffold` (Chisel technical 0) cradle under the two middle rows, plus corner supports at both ends of the outer rows
* y2: corner supports again, `factory_wall` (Chisel factoryblock 1) belly across the middle rows, `factory_hazard` end caps
* y3–4: wall sides, hazard corners at the ends, **real lava** filling the 2-wide interior
* y5: wall top, hazard end caps

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `Tanks(4,5,4 / 10,-1,10)` in the camp regular list |
| Spawn conditions | only via `MilitaryCamp` leaf matching (4 to 10 square) |
| Size | declared 4-10 x 5+ x 4-10; as built tanks of 4 width every 5 blocks across the short side, running the long side, 5 blocks tall from y+1 |
| Build steps | see the per-tank layers above |
| Palette | none (procedural): `technical_scaffold`, `factory_wall`, `factory_hazard` (Chisel), real vanilla lava |
| Loot, spawners, NPCs, flag | none. The lava is real source lava inside a sealed 2-wide core |
| NBT | none |
| Bugs | WGEN-24 (Chisel blocks) |


### B8. Small watch tower (`WatchTowerSmall`, size 3 or 4)
* Centred in its area. Its height is the camp height: 8 for size 3, 10–12 for size 4.
* Fence posts at the four corners from y1 to y+h−2.
* A solid vanilla-planks core of `(size−2)²` cells (1×1 or 2×2) up to y+h−4, which is also the standing platform.
* A 2-high hollow fence ring at y+h−4 and y+h−3. A TG `camoNetRoof` (meta 0 always) at y+h−1.
* A vanilla ladder (meta 4/2/5/3 for directions 0/1/2/3) on the facing side from y1 to y+h−4. A vanilla fence gate (meta `(d+1)%4`) at the top of the ladder.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `WatchTowerSmall(minX..maxZ, tower)`. Camp corner list: tower 3 `(3,8,3 / 4,8,-1)` with swap, tower 4 `(4,10,4 / -1,12,-1)`. A third entry in the regular list is commented out |
| Spawn conditions | only via `MilitaryCamp` corner matching: a leaf touching two camp edges within 2 blocks, passing the `cornerChance` roll = ((sx+sz)/2 - 20)/40 |
| Size | declared 3-4 x 8 x 3-4 / 4+ x 10-12 x 4+; built centred in the leaf at the declared tower footprint (3 or 4) and the leaf height |
| Build steps | see above: corner fence posts, planks core and platform, fence ring, camo roof, ladder, fence gate |
| Palette | none (procedural): vanilla fence, planks, ladder, fence gate, TG `camoNetRoof` 0 |
| Loot, spawners, NPCs, flag | none |
| NBT | none |
| Bugs | none |


### B9. Empty plane (`EmptyPlane`)
A single layer of one block (cobblestone in camps and cities) at posY over the area. Default bounds are `(1,1,1, −1,−1,−1)`. It is the fallback when no structure fits.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `EmptyPlane(block)` (bounds `1,1,1 / -1,-1,-1`) and an explicit-bounds constructor. The camp creates a throwaway `EmptyPlane(cobblestone)` when no list entry fits; a commented-out list entry with iron blocks also exists |
| Spawn conditions | fallback only |
| Size | the leaf area, one block thick |
| Build steps | sets one layer of the block at posY over the whole area (no rotation) |
| Palette | one vanilla block: cobblestone in camps and cities |
| Loot, spawners, NPCs, flag, NBT | none |
| Bugs | none |


---

## Part C: Dormant, test-only and legacy classes

Part C covers the 5 groups of classes that no normal worldgen path reaches: the city generator, the gas station, `SmallHouse`, `BlockStructure` and the two legacy carrier classes. The first two can still be run from the Structure Test Tool (mode 1 for the city generator) or by editing the tool; the rest are dead code. Each section ends with a **Sheet** table.

None of these spawn in normal worldgen.

### C1. City generator (`GenericCity`, `GenericCitySegment`, `CityStructure`, `CityBuilding`, `CityBuildingType`, `BuildingStyle`, `CityRoad`, `IRoad`, `parts/Staircase`)
* **Reachability:** only from the Structure Test Tool's mode 1. The `CityStructure` spawn entry is commented out.
* **`CityStructure`:** if it were enabled, it would be BIG, random 32–79 size, step 16, height limit 5. It would remove junk (asymmetric box, [WGEN-22](index.md#suspected-bugs)), flatten (maxDiff 1), smooth with the 5×5 Gaussian, then call `testCity.createCity` with WOODLAND. On failure it retries with sizes −16 and sizeY −1.
* **`testCity` parameters:**
  * sizeMin 20, sizeMax 80
  * sub-segment min 12 / max 30, overridden per call to min 7 (if the smaller side < 14) or 10, and max 15 (if ≤ 45) or side/3
  * maxHeightDiff −1 (no flattening)
  * road widths by depth [6, 4, 4, 2, 1]
  * road type `CityRoad(main = concrete_dark, side = concrete)`
  * fuzzyBorder 0.5
* **`createCity`:**
  1. Colour from `getBiomeType` if null.
  2. Builds the same BSP as the camp (`GenericCitySegment`). It differs from the camp as follows:
     * It splits only when the *shorter* side ≥ min.
     * The child road width comes from the depth table.
     * If the root does not split, nothing is built.
  3. `setBlocks`: remove junk (+1), Gaussian smoothing, then the segments.
* **Roads (`CityRoad`):** along the full segment length plus 1 extra block each end. The road gains height steps evenly to bridge the measured height difference between its two ends. Two layers are placed (y−1, y). The centre uses `main` when width ≥ 4 and the edges use `side`. It clears 3 blocks of air above (X-aligned) or 2 (Z-aligned, `SET_AIR_HEIGHT` 3). It prints `Set Road Blocks...` and `Set Road Blocks... DONE!` to stdout.
* **Leaves (`placeStructure`):**
  * Border leaves at depth > 2 are skipped with 50 % chance.
  * Otherwise 3 blocks of air are cleared, a fitting `CityBuilding` is picked, the leaf is flattened to 0, and the building is built at the segment's original y.
* **`CityBuildingType` parametric building:** each type has `wall_x` (columns per wall segment), `wall_y` (storey height), `wall_el` 1, `floor_height` 1, a `wallPattern` and `doorPattern` (rows top-down, −1 = air), `doorWidth` and `doorOffset`. `BuildingStyle` rolls one block per role per building: 0 wall, 1 glass, 2 floor, 3 border, 4 door decoration; plus a stair pair (stairs + support).
  1. Snap the size to whole wall segments: `segX = (sx − 1)/wall_x`. The storey count is `sizeY / wall_y`.
  2. For each storey: an interior floor slab, lamp01 meta 1 ceiling lamps on a 3-grid under every upper floor, and four walls built segment by segment from the pattern. On the ground floor the middle segment of the facing wall uses the door pattern and gets `doorWidth` wooden doors.
  3. Roof: a floor slab, a 1-high parapet of the wall block, and (if `roofLikeFloor`) a border-block ring at roof level.
  4. Vertical access: switchback `Staircase`s in a corner, alternating direction per storey. Small buildings (`max(sx−2, sz−2) < wall_y+1` or `min < 4`) get TG ladders instead ([WGEN-21](index.md#suspected-bugs)). A roof ladder ends at a vanilla trapdoor hatch.
* **Building types:**

| Type | wall_x × wall_y | Style | Pattern (top→bottom, by column) | Doors |
|---|---|---|---|---|
| building1 | 2 × 5 | stone (walls: stonebrick_panel / stonebrick_small / Chisel cobblestone 1,2,3 / Chisel andesite 3; floors: cobblestone_largetile / stonebrick_sunken / Chisel andesite 6 / diorite 6 / granite 6; borders: andesite 5 / andesite 4 / cobblestone 13 / cobblestone 15 / stonebricksmooth 15; glass: glass_bubble) | `00 / 01 / 01 / 00 / 33` | width 1, offset 1; roof like floor, border 3 |
| building2 | 3 × 4 | factory (factory_wall, glass_bubble, factory_wireframe) | `000 / 011 / 000 / 222` | width 2, offset 1 |
| building3 | 3 × 4 | stone | `000 / 011 / 000 / 333` | width 2; roof like floor |
| building4 | 3 × 4 | brick (Chisel brickCustom 3 walls, brickCustom 2 border, stone floors) | `000 / 011 / 000 / 333` | width 2; roof like floor |
| building5 | 3 × 5 | lab (Chisel laboratoryblock 2 walls, stainedGlassPane[3] 11 windows, laboratoryblock 8 floor, laboratoryblock 4 border, laboratoryblock 15 door decor; quartz stairs) | `000 / 011 / 011 / 000 / 333`, door `000 / 044 / 0-- / 0-- / 333` | width 2; roof like floor |
| building6 | 3 × 4 | limestone (Chisel limestone 5 walls, limestone 13 floor and door decor, limestone 9 border) | `000 / 011 / 000 / 333` | width 2; roof like floor |

  All styles except lab use the stair pair oak_stairs (STAIRS) + `wood_scaffold`. City building bounds: building1 `(3,5,5 / −1,16,−1)` with swap; the others `(4,5,4 / −1,16,−1)`. Height is minY + rnd(maxY+1−minY), i.e. 5–16.
* **`Staircase.placeStaircase(pos, size, dir, inverted, stairs, support)`:** builds a straight flight of `size` steps in direction dir (0 +X, 1 +Z, 2 −X, 3 −Z), ascending or (inverted) descending. Support blocks fill under every step and 3 blocks of air are cleared above each step. The stair rotation is `((3−dir)+1)%4`, or `((3−((dir+2)%4))+1)%4` when inverted.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `CityStructure` (extends `Structure`) is the spawn wrapper; its entry in `TGStructureSpawnRegister` is commented out. Other classes: `GenericCity` (BSP + roads), `GenericCitySegment`, `CityBuilding` / `CityBuildingType` / `BuildingStyle` (parametric buildings), `CityRoad` / `IRoad`, `parts/Staircase` |
| Spawn conditions | none in normal worldgen (unregistered). Reachable only by Structure Test Tool mode 1. If registered it would be BIG, LAND, step 16, height limit 5 and size 32-79 |
| Size | declared random 32-79; built to the same size, or 16 less (and one less height) per failed retry |
| Build steps | see above |
| Palette | none (procedural), 6 building types with random per-building style |
| Loot | none (buildings are empty; no chests are placed) |
| Spawners, NPCs, flag | none |
| NBT | none |
| Bugs | [WGEN-18](index.md#suspected-bugs), WGEN-21, WGEN-22, [WGEN-29](index.md#suspected-bugs) |


### C2. City gas station (`CityGasStation`)
* **Reachability:** default constructor, not registered, no lootTier (0). It is only reachable by editing the test tool. Uses [`structures/cityGasStation`](assets.md#structure-data-files) (1052 entries, 13×6×16), passes 0 and 1, with no foundation.
* **Layout:**
  * **Forecourt:** a Chisel concrete 10 apron with Chisel stonebricksmooth 13 paving.
  * **Shop:** 11×6 (x1–11, z3–8) with stonebricksmooth 5 walls and Chisel glass_pane 4 windows. Inside: a fence-and-pressure-plate counter, bookshelf shelving (vanilla and Chisel bookshelf 1/6/7/8), 2 vanilla chests, a fence gate and wooden doors.
  * **Canopy:** a Chisel factoryblock 15 roof over the shop (y4) and a canopy over the pumps (y5, z9–15) on two stonebricksmooth pillars.
  * **Pumps:** two at z13, made of factoryblock 6 + factoryblock 9 with levers (meta 10).
  * **Lighting:** TG lamps.
  * 561 explicit air cells clear the volume.
* **Palette (24):**
  0 Chisel concrete 10 · 1 Chisel stonebricksmooth 13 · 2 air · 3 stonebricksmooth 5 · 4 Chisel glass_pane 4 · 5 Chisel factoryblock 15 · 6 vanilla chest 3 (CHEST) · 7 factoryblock 6 · 8 lamp01 4 (TG, pass 1) · 9 Chisel bookshelf 7 · 10 bookshelf · 11 Chisel bookshelf 6 · 12 lever 10 (LEVER) · 13 lamp01 1 (TG, pass 1) · 14 Chisel bookshelf 8 · 15 Chisel bookshelf 1 · 16 factoryblock 9 · 17 fence_gate 3 (FENCE_GATE) · 18 fence · 19 wooden_door 3 (no rotation) · 20 wooden_pressure_plate · 21 wooden_door 8 (pass 1) · 22 wooden_door 1 (no rotation) · 23 vanilla chest 4 (CHEST).

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `CityGasStation`, default constructor, not in the register; no loot tier set (0) |
| Spawn conditions | none (unregistered; reachable only by editing the test tool) |
| Size | no declared size; as built 13 x 6 x 16 |
| Build steps | pass 0 then pass 1 of `structures/cityGasStation`; no cleanup and no foundation |
| Palette | 24 entries (listed below) |
| Loot | 2 vanilla chests on `DUNGEON_CHEST` at tier 0 |
| Spawners, NPCs, flag | none |
| NBT | no structure-level NBT. Placed chests carry the normal `Items` list, filled at placement |
| Bugs | WGEN-23, WGEN-24, [WGEN-31](index.md#suspected-bugs) |


### C3. `SmallHouse`
A procedural brick house. Unused; only present in a commented-out test line.
* Size snapped to 4k+1 per side. Its constructor forces minY = maxY = 5.
* **Layers:**
  * y0: stonebrick border, TG concrete 5 floor
  * y1–3: brick walls with stonebrick posts on the 4-grid, and Chisel glass_pane 2 windows at y2
  * y4: stonebrick ring
  * y5: stone-slab border with double-slab roof
* A wooden door on the facing side and an outside lamp01 meta 7.
* `destroyTop` then randomly knocks down the top 0–2 blocks of half the columns, starting from y+5, to make a ruin ([WGEN-20](index.md#suspected-bugs)).

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `SmallHouse`, not registered, only in a commented-out test line; constructor forces minY = maxY = 5 |
| Spawn conditions | none |
| Size | each horizontal side snapped to 4k + 1; height 6 (y0-y5) |
| Build steps | layers above, then `destroyTop` ruin pass |
| Palette | none (procedural): stonebrick, TG `concrete` 5, brick, Chisel glass_pane 2, stone slabs, wooden door, TG `lamp01` 7 |
| Loot, spawners, NPCs, flag, NBT | none |
| Bugs | WGEN-20 |


### C4. `BlockStructure`
Fills the whole box with one [`MBlock`](structure-system.md#mblock). Unused.

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | `BlockStructure(MBlock block, minX, minY, minZ, maxX, maxY, maxZ)` extends `Structure`. Nothing in the codebase constructs it |
| Spawn conditions | none (unreferenced, dead code) |
| Size | the full box passed to `setBlocks` |
| Build steps | three nested loops over the box call `MBlock.setBlock` with rotation 0 and the default loot tier 0; direction, colour type and random are ignored |
| Palette | one `MBlock` supplied by the caller. If it were a chest it would be filled at tier 0 and an air-marker block would be placed as air |
| Loot, spawners, NPCs, flag, NBT | none of its own |
| Bugs | none |


### C5. Legacy carrier (`AircraftCarrier`, `AircraftCarrier2`)
Plain classes, not [`Structure`](assets.md#structure-data-files) subclasses. They are unreferenced; the only call left is in a comment.
* The scan of an earlier carrier version is embedded as `int[][] {x,y,z,i}` literals: 1934 entries in `AircraftCarrier` (x ≤ 36), followed by 1579 entries in `AircraftCarrier2` (x 36–54).
* The data uses a 27-entry palette without loot or rotation types: Chisel factoryblock 6/3/2/15/4/12, Chisel `torches[1]` metas 1–4, vanilla glass, glass_pane, Chisel technical 5/13, technical2 0/1, iron_bars 3, wooden doors 1/3/8, aluminumStairs[1] 0/1, vanilla ladder 3, Chisel iron_block 14, iron doors 0/8/9.
* Placement is unrotated, from origin (0,0).

**Sheet**

| Field | Value |
|---|---|
| Class and register entry | plain classes (not `Structure` subclasses): `AircraftCarrier` and `AircraftCarrier2`, both unreferenced |
| Spawn conditions | none (dead code) |
| Size | as built: up to 54 blocks along X; the scan has 1934 + 1579 = 3513 embedded cells |
| Build steps | unrotated placement from origin (0,0) of the embedded `int[][] {x,y,z,i}` literals, split across two classes for the JVM method-size limit |
| Palette | 27 entries, no loot or rotation types |
| Loot, spawners, NPCs, flag, NBT | none |
| Bugs | WGEN-24 (Chisel blocks) |

