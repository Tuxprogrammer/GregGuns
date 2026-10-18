# Structure system: spawning, placement, file format, authoring

This file covers the machinery shared by every Techguns structure. Individual structures are in [structure-catalogue.md](structure-catalogue.md). Loot tables are in [loot.md](loot.md).

Classes covered: `worldgen/WorldGenTGStructureSpawn`, `TGStructureSpawnRegister`, `TGStructureSpawn`, `StructureSize`, `StructureType`, `worldgen/structures/Structure`, `util/BlockUtils`, `util/MBlock`, `util/MultiMBlock`, `util/BlockPosInd`, `items/BuildingScanTool`, `items/StructureTestTool`.

---

## 1. Spawn pipeline (world generator)

`WorldGenTGStructureSpawn` is an FML `IWorldGenerator`. It is registered in `Techguns.postInit` with generator weight **4**, and only when config `SpawnStructures` is true (see [index.md § Config](index.md#config-keys-that-gate-worldgen)).

Per generated chunk `(cx, cz)`:

1. **Dimension gate.** It only runs when `world.provider.dimensionId == 0` (Overworld). The Nether/End handlers exist as empty stubs.
2. **Biome sample.** It reads the biome once, at the chunk's corner block `(cx*16, cz*16)`, through `getWorldChunkManager().getBiomeGenAt`.
3. **Grid test for size class.** The size class is picked by modulo on chunk coordinates (Java `%`, which also works for negative multiples). The first match wins:
   | Order | Condition | Size class | Config key (default) |
   |---|---|---|---|
   | 1 | `cx % Big == 0 && cz % Big == 0` | `BIG` | `StructureSpawnWeightBig` (64) |
   | 2 | `cx % Medium == 0 && cz % Medium == 0` | `MEDIUM` | `StructureSpawnWeightMedium` (32) |
   | 3 | `cx % Small == 0 && cz % Small == 0` | `SMALL` | `StructureSpawnWeightSmall` (16) |

   No random roll is involved. Every grid point gets exactly one attempt. With the defaults, each 64×64-chunk tile has 1 BIG, 3 MEDIUM and 12 SMALL attempt points. Whether an attempt succeeds depends only on the terrain checks below.
4. **Land or water.** `StructureType.WATER` is used if the sampled biome has the BiomeDictionary type `OCEAN`. Otherwise the type is `LAND`.
5. **Weighted pick.** `TGStructureSpawnRegister.choseStructure(rnd, biome, size, type)` sums `getWeightForBiome` over the list for that size class. A structure's weight is 0 when its size class differs, when the type is not in its `allowedTypes`, or when it has a biome whitelist that the biome matches none of. Otherwise its weight is `spawnWeight`. The function then rolls `nextInt(total)+1` and walks the cumulative weights. It returns null (no structure) when the total is 0.
6. **Spawn.** If a structure was chosen, it calls `s.spawnStructureWorldgen(world, cx, cz, s.getSizeX(rnd), 8, s.getSizeZ(rnd), rnd, biome)`. The `8` is a nominal sizeY.

### Registered spawn table (`TGStructureSpawnRegister` static init)

| Size class | Structure (constructor args) | setXZSize | Weight | Biome whitelist | Type |
|---|---|---|---|---|---|
| SMALL | `FactoryHouseSmall(8,0,7, 9,5,10)` | 11×10 | 1 | any | LAND |
| SMALL | `SmallTrainstation(0…0)` | 11×12 | 1 | any | LAND |
| MEDIUM | `HouseMedium(16,12,16, 16,12,16)` | 16×16 | 1 | any | LAND |
| MEDIUM | `AlienBugNestStructure()` | 4×4 | 1 | `SANDY` or `WASTELAND` | LAND |
| MEDIUM | `BigBunker(32,14,17, 32,14,17)` | 32×17 | 1 | any | LAND |
| BIG | `MilitaryBaseStructure(0…0)` | (random 32–79 each axis) | 1 | any | LAND |
| BIG | `AircraftCarrierNew(54,24,21, 54,24,21)` | 54×21 | 1 | any | WATER |
| BIG | `Submarine()` | 33×7 | 1 | any | WATER |
| (commented out) | `CityStructure()` | random 32–79 | 1 | any | LAND |

Every entry is given the dimension list `[0]`. `TGStructureSpawn.dimensionMatches()` is never called, so this list has no effect (WGEN-28). No SMALL or MEDIUM structure accepts WATER, so SMALL and MEDIUM grid points in ocean biomes never spawn anything.

`StructureSize` is the enum `SMALL, BIG, MEDIUM`. `StructureType` is the enum `LAND, WATER`.

---

## 2. `Structure` base class

These fields are set by the constructor `(minX, minY, minZ, maxX, maxY, maxZ)`:

* `min*` / `max*`: allowed size range. Camp and city segment matching uses them, and `-1` means unbounded. Scanned structures use them for the X/Z swap test below.
* `sizeX`, `sizeZ`, `centX = (int)(sizeX/2)`, `centZ = (int)(sizeZ/2)`: set by `setXZSize`. `getSizeX/Z(rnd)` return them by default. The military base and city override these to return `32 + nextInt(48)`.
* `canSwapXZ`: set by `setSwapXZ`. When true, segment matching also tries the X/Z-swapped size.
* `removeJunkOnWorldspawn` (default false). No registered structure sets it to true.
* `lootTier` (default 0). This is the index into the TG chest loot-tier arrays (see [loot.md](loot.md)).
* `getStep()` returns 4 by default. It is the sample spacing for the terrain check.
* Biome type lists used to choose the colour type:
  * `coldTypes = {COLD, SNOWY}` → `SNOW`
  * `sandTypes = {SANDY, BEACH, MESA}` → `DESERT`. The source spells `SAVANNA.BEACH`, which resolves to BEACH (WGEN-17).
  * `netherTypes = {NETHER}` → `NETHER`
  * anything else → `WOODLAND`

  The lists are checked in the order shown and the first match wins.
* The abstract `setBlocks(world, posX, posY, posZ, sizeX, sizeY, sizeZ, direction, colorType, rnd)` builds the structure. Its documented `direction` values are 0 = faces WEST (−X), 1 = NORTH (−Z), 2 = EAST (+X), 3 = SOUTH (+Z).

### `rotatePoint(x, z, direction, cx, cz)`
This rotates a local point around `(cx, cz)` by `direction` quarter turns. Each turn maps the offset `(a, b) = (x−cx, z−cz)` to `(b, −a)`, so +X becomes −Z. The result is `(a+cx, b+cz)`. Every scanned structure uses this function for both block positions and spawn points.

### Default `spawnStructureWorldgen(world, chunkX, chunkZ, sizeX, sizeY, sizeZ, rnd, biome)`
Steps:
1. `direction = rnd.nextInt(4)`.
2. Compute the rotated sizes `sizeXr/sizeZr`. They are swapped when the direction is 1 or 3.
3. Rotate the four corners `(0,0) (sizeX,0) (0,sizeZ) (sizeX,sizeZ)` around `(sizeX/2, sizeZ/2)` and take the minimum X and Z → `minX, minZ`.
4. Set `x = chunkX*16 + minX`, `z = chunkZ*16 + minZ`, plus the per-class shift `getRotationShiftX/Z(direction)`. The shift is 0 by default. BigBunker overrides it.
5. `y = BlockUtils.getValidSpawnYArea(world, x+shiftX, z+shiftZ, sizeXr, sizeZr, heightDiffLimit=3, step=getStep())`. Abort if `y < 0`.
6. If `removeJunkOnWorldspawn` is set, clear junk on the footprint plus a 1-block border.
7. `setBlocks(world, x, y−1, z, sizeX, sizeY, sizeZ, direction, colorTypeFromBiome, rnd)`. Note that `x/z` already include `minX/minZ`, and `setBlocks` rotates again around the centre. For non-square footprints the area that was checked and the area actually built differ (WGEN-05).

Subclasses override this method for different terrain rules: water, underwater, a camp area, or the bug nest. See the catalogue.

### Common `setBlocks` for scanned structures
`HouseMedium`, `FactoryHouseSmall`, `SmallTrainstation`, `CityGasStation` and `AircraftCarrierNew` share this preamble. When `(sizeX < minX && sizeZ > minX && sizeX >= minZ)` or `(sizeZ < minZ && sizeX > minZ && sizeZ >= minX)`, the direction is incremented by 1 and the centre swapped to `(sizeZ/2, sizeX/2)`. Otherwise the centre is `(sizeX/2, sizeZ/2)`. With the registered sizes this swap never triggers. `BigBunker` and `Submarine` skip it.

---

## 3. Terrain helpers (`util/BlockUtils`)

### Height probes
| Method | Returns |
|---|---|
| `getGroundY(x,z)` | `world.getHeightValue − 1` (vanilla heightmap). |
| `getHeightValueTopBlockAll(x,z)` | Scans down from the chunk's top filled segment +15 and returns (first non-air block y) + 1, or −1. Uses `block.isAir`. |
| private `getTopSolidOrLiquidBlock(x,z)` | Like vanilla, but liquids also stop the scan. Returns hit y + 1. A hit is a block whose material blocks movement and is not leaves or foliage, or any liquid. |
| `getHeightValueNoTrees(x,z)` | Takes the private probe above, then steps down while the block is leaves, wood or foliage. |
| `getHeightValueNoTreesIgnoreLiquid(x,z)` | Uses vanilla `world.getTopSolidOrLiquidBlock`, which skips down through water to the floor and returns floor+1. Returns −1 if the block at that y is liquid, so water surfaces are rejected. Then steps down through leaves and foliage, but not wood. |
| `getHeightValueUnderwater(x,z)` | Vanilla top-solid probe, then steps down through leaves, foliage and liquid. Returns the y of the solid sea-floor block. |
| `getHeightValueLiquid(x,z)` | Returns (first non-air block y) + 1, scanning from the top. Returns −1 if that block is not liquid. Used for water-surface spawns. |

### Spawn-Y validators
| Method | Algorithm |
|---|---|
| `getValidSpawnY(x,z,sx,sz,limit)` | Samples the four corners `(x,z) (x+sx,z) (x,z+sz) (x+sx,z+sz)` with `…IgnoreLiquid`. Fails (−1) if any corner is −1 or if any corner differs from corner 0 by `limit` or more. Returns the median of the 4 corners, computed as the average of the middle two after sorting. |
| `getValidSpawnYUnderwater` | Same algorithm, with the `Underwater` probe. |
| `getValidSpawnYWater` | Same algorithm, with the `Liquid` probe. All corners must be liquid-topped. |
| `getValidSpawnYArea(x,z,sx,sz,limit,step)` | Samples a grid `ix = 0, step, 2·step … ≤ sx` and the same for z with `…IgnoreLiquid`. Fails on any −1 sample or when `max − min > limit`. Returns the integer mean. The code intends to clamp the last sample to the far edge, but that branch is unreachable, so the far edge is only sampled when the size is a multiple of `step` (WGEN-15). |

### Terrain shaping
* `isGroundBlock(b)`: material is ground, grass, rock, sand or clay.
* `adjustHeightAtPos(x,z,y,newY)`: to raise a column, it repeats the current top block (or dirt, if the top block is not ground) with the same meta, using flag 2. To lower a column, it sets air from `y` down to `newY+1`.
* `flattenArea(x,z,sx,sz,maxDiff)`:
  1. Collect every column height with `NoTrees − 1`, and track min, max and a "median". The median is computed incorrectly (WGEN-16).
  2. If `span ≤ maxDiff`, columns are left unchanged.
  3. Otherwise each column is remapped linearly into `[median − maxDiff/2, median + maxDiff/2]`: `newY = round((y−min)/span · maxDiff + median − maxDiff/2)`.
* `flattenAreaToMaxHeight`: raises every column to the area maximum. Unused by worldgen.
* `apply2DHeightmapFilter(x,z,sx,sz,kernel)`: convolves the `NoTrees` heightmap with the kernel and writes each column immediately, so later columns read heights that were already modified. Two kernels exist: `FILTER_GAUSSIAN_3x3` (1/16, 1/8, 1/16 / 1/8, 1/4, 1/8 / …) and `FILTER_GAUSSIAN_5x5` (rows `.00325 .01375 .0235 .01375 .00325`, `.01375 .059 .097 .059 .01375`, `.0235 .097 .159 .097 .0235`, then symmetric).
* `removeJunkInArea(x,z,sx,sz)`: for each column, starts at `TopBlockAll` and sets blocks to air going down until it reaches a ground-material block. This removes trees, plants and snow, and also water and lava (WGEN-26). `removeJunkInAreaOLD` is a deprecated variant.
* `fillBlocks`, `fillBlocksAir`, `fillBlocksWithMeta`, `fillBlocksHollow` (walls only): plain box fills.
* `fillSphere(c, r, block)`: places the block at all cells with squared distance < r².
* `fillSphere2(c, r, thickness, inner, shell)`: places `inner` where d² < (r−thickness)² and `shell` where d² < r².
* `fillCylinder(p1, p2, r, inner, shell)`: point-to-line distance over the bounding box expanded by r. Places `inner` where distance < r−1 and `shell` where distance < r.
* `drawLine(p1, p2, block)`: 3D Bresenham line. On a Y-dominant line it also places the block at each horizontal step, so the line stays 4-connected.
* `getBiomeType(world,x,z)`: maps vanilla biome identity to a colour type.
  * DESERT: beach, desert, desertHills, mesa, mesaPlateau(_F), savanna, savannaPlateau.
  * SNOW: coldBeach, coldTaiga(Hills), frozenOcean, frozenRiver, iceMountains, icePlains, megaTaiga(Hills), taiga(Hills).
  * NETHER: hell.
  * WOODLAND: anything else.

  The military camp and the city use this function. Scanned structures use the BiomeDictionary rule in § 2 instead.
* `BiomeColorType` enum: `WOODLAND, SNOW, DESERT, NETHER`. Only the procedural camp props and tents use it, to pick the camo-net meta: DESERT→2, SNOW→1, otherwise 0. Scanned structures ignore it.
* `selectEntitiesWithinAABB(world, bb, filter)`: a chunk-iterating entity query used by the camp flag (see 09). It calls `getChunkFromChunkCoords` with no existence check, so it can load chunks (WGEN-27).
* `exists(path, postfix)`: client-only check for whether a block texture exists. Not used by worldgen.

---

## 4. Block descriptors: `MBlock`, `MultiMBlock`, `BlockPosInd`

### `MBlock`
Fields: `block`, `meta`, `hasRotation`, `rotationType` (a `BlockUtils.BlockType`), and `pass` (default 0, set with `setPass`). Equality compares block and meta only. `isAirBlock()` is true when the block is `TGBlocks.airMarker`.

`setBlock(world, x, y, z, rotation, lootTier)`:
1. If the block is the air marker, set air.
2. Otherwise compute the rotated meta and place the block with flag 2. For the `CHEST` rotation type the meta is applied a second time with `setBlockMetadataWithNotify`, because the first placement does not stick.
3. Fill loot right after placement, using a **static unseeded** `Random` (WGEN-25):
   * vanilla `Blocks.chest` → `ChestGenHooks.DUNGEON_CHEST`
   * `TGBlocks.tgchest_weapon` → `WEAPONCHEST_LOOT_TIER[lootTier]`
   * `TGBlocks.tgchest` → `REINFORCEDCHEST_LOOT_TIER[lootTier]`

   The item count comes from `ChestGenHooks.getCount` and the contents from `WeightedRandomChestContent.generateChestContents`.

### Rotation types (`BlockUtils.BlockType`)
For each rotation step (applied `rotation` times), the meta is remapped as shown. Metas not listed stay unchanged.

| Type | Used for | One step mapping |
|---|---|---|
| `DOOR` | vanilla and TG doors (lower half) | 0→3, 3→2, 2→1, 1→0; 7→6, 6→5, 5→4, 4→7 (upper-half metas 8–11 untouched) |
| `FENCE_GATE` | fence gates | `(m+r) % 4`, keeping bit 4 (open) |
| `LADDER` | vanilla ladders, wall signs | 5→2, 4→3, 3→5, 2→4 |
| `STAIRS` | vanilla and Chisel stairs | 2→0, 1→2, 3→1, 0→3; 4→7, 6→4, 5→6, 7→5 (upside-down set) |
| `BED` | beds | 0→3, 3→2, 2→1, 1→0; 8→11, 11→10, 10→9, 9→8 |
| `STAIRS2_CHISEL` | Chisel limestone stairs (packed metas) | 8→11, 11→9, 9→10, 10→8; 12→15, 15→13, 13→14, 14→12 |
| `LEVER` | levers, trapdoors | 1→4, 4→2, 2→3, 3→1; 9→12, 12→10, 10→11, 11→9 |
| `CHEST` | chests, furnaces | 3→5, 5→2, 2→4, 4→3 |
| `TG` | TG blocks (lamps, ladders) | delegated to `GenericBlock.getRotationMeta(meta, r)`. The base returns meta unchanged; subclasses (see 07-blocks) rotate. |

These tables are consistent with the position rotation (+X→−Z). For example, a chest facing east (5) ends up facing north (2).

For procedural builders, `getBlockDirMeta(type, direction)` gives a meta for a structure direction:
* DOOR: dir 2→0, 3→1, 0→2, 1→3
* FENCE_GATE: `(d+1)%4`
* LADDER: 0→5, 1→3, 2→4, 3→2
* STAIRS: 0→0, 1→2, 2→1, 3→3
* any other type: 0

`getBlockForgeDirMeta` maps ForgeDirection to these direction ints: DOWN→4, EAST→2, NORTH→1, SOUTH→3, UP→5, WEST→0.

### `MultiMBlock` (random palette entry)
It is built from parallel arrays `blocks[]`, `metas[]` and `weights[]`, plus optional `rotationTypes[]` and `hasRotations[]`. Every time it is placed it rolls an index and places that block, rotating it only when that entry has a rotation. Vanilla chests among the options get DUNGEON loot. The roll uses `nextInt(total+1)` and takes the first cumulative weight ≥ the roll, which gives option 0 one extra weight point (WGEN-14).

### `BlockPosInd`
A `BlockCoords (x,y,z)` plus an `index` into a block list. `toString()` gives `"x,y,z,index"`, which is exactly the line format of the structure data files.

---

## 5. Scanned-structure placement (`BlockUtils`)

| Method | Behaviour |
|---|---|
| `loadStructureFromFile(name)` | Opens the classpath resource `/assets/techguns/structures/<name>` (Java resource, not resource-pack aware). Line 1 is the entry count N. Each following line is `x,y,z,index` (comma-separated shorts), loaded into a `short[N][4]`. Only `IOException` is caught; a missing file throws a null-stream exception inside the static initialiser (WGEN-23). Line endings in the shipped files are CRLF. |
| `placeScannedStructure(world, data, palette, posX, posY, posZ, cx, cz, rotation, pass, lootTier)` | For each entry whose palette `MBlock.pass == pass`, places it at `(posX, posY+y, posZ) + rotatePoint(x, z, rotation, cx, cz)` with rotated meta and loot tier. Callers always run pass 0 and then pass 1. Pass 1 holds attachments: upper door halves, lamps, ladders, pressure plates and signs, so their supports exist first. |
| `placeFoundation(…, pass, range)` | For every entry with `y == 0` in the given pass, places the same palette block at depths 1…`range` below, but only into air cells. Water is not filled. |
| `cleanUpwards(…, pass, range)` | For every `y == 0` entry, sets air at heights 1…`range` above that cell (rotated). The palette block is ignored. |
| `setBlockRotated(world, mblock, posX, posY, posZ, x, z, cx, cz, rotation, lootTier)` | Places one block at a rotated local position. |
| `setBlockRotatedReplaceAirOnly(…)` | The same, but only into air. Ignores loot tier. |

---

## 6. Structure data file format (`assets/techguns/structures/*`)

The seven files are plain ASCII text with no extension:

```
<N>                 total number of entries
x,y,z,i             N lines; local coordinates (≥0) relative to the scan's min corner, i = palette index
```

* Entries are ordered x-major, then y, then z, because that is the scanner's loop order. No position is duplicated.
* The palette is **not** in the file. It is the hand-written `blockList` in the Java class that loads the file. Palette order equals first-encounter order during the scan, re-typed by hand. See the per-structure tables in the catalogue.
* Cells absent from the file are left untouched in the world. Cells that must be cleared are stored explicitly with an air palette entry, either `Blocks.air` or the `airMarker` MBlock, which places air.
* Natural cover is never captured: the scanner skipped air, plain dirt (meta 0) and grass. Tile-entity contents and NBT are never captured either.

Summary of the shipped files (counts and bounds were verified by decoding):

| File | Entries | Extent X×Y×Z (local max+1) | Palette size | Loaded by | Spawns naturally? |
|---|---|---|---|---|---|
| `structures/airCraftCarrier` | 3921 | 54×24×21 | 38 | `AircraftCarrierNew` | yes (BIG, ocean) |
| `structures/bigbunker` | 4332 | 33×14×18 | 31 | `BigBunker` | yes (MEDIUM) |
| `structures/cityGasStation` | 1052 | 13×6×16 | 24 | `CityGasStation` | no (test tool only, not registered) |
| `structures/factoryBuildingSmall` | 462 | 9×7×11 | 21 | `FactoryHouseSmall` | yes (SMALL; also a camp building) |
| `structures/houseMedium` | 1637 | 16×12×16 | 56 | `HouseMedium` | yes (MEDIUM) |
| `structures/smallTrainstation` | 327 | 11×7×11 | 21 | `SmallTrainstation` | yes (SMALL) |
| `structures/submarine` | 992 | 7×10×33 | 25 | `Submarine` | yes (BIG, ocean) |

---

## 7. Authoring workflow and tools

The structures were built by hand in a test world, scanned into text, and wired up as a resource file plus a hand-written palette. Two older carrier classes embedded the scan output directly as Java `int[][]` literals instead, split across `AircraftCarrier` and `AircraftCarrier2` to stay under the JVM method-size limit. Moving the data to resource files (`AircraftCarrierNew`) replaced that approach.

To mark cells that must become air on placement, the author placed `TGBlocks.airMarker` blocks ("Air Marker"), because real air is not scanned. In the palettes these appear either as the `airMarker` MBlock or as `Blocks.air`.

### Building Scan Tool (`items/BuildingScanTool`)
* Registry name `buildingScanTool`, unlocalized `buildingScanTool`, lang `techguns.item.buildingScanTool.name=Building Scan Tool`, texture `techguns:structureScanTool` → `textures/items/structureScanTool.png`. Max stack 1, Techguns creative tab. No tooltip. The item side is owned by 06-items.
* NBT on the stack: `x1`, `y1`, `z1` (int).
* **First right-click on a block:** stores the clicked position. The client prints the chat message `Position1 set (x/y/z).`
* **Second right-click:** the client prints `Position2 set (x/y/z).` and `Size: (sx/sy/sz).`, where size is |Δ|+1 on each axis. The server scans the box from the min corner, and the tag is cleared on both sides.
* **Scan.** Iterates x, then y, then z, and skips air, dirt with meta 0, and grass. Each other block becomes an `MBlock(block, meta)`. A new block/meta pair is appended to the palette and emits one generated Java line of the form `blockList.add(new MBlock(Block.getBlockFromName("<unlocalized name>"), meta));`. That line does not work as written, because `getBlockFromName` wants a registry name, so it was rewritten by hand. Every scanned cell also records a `BlockPosInd(ix, iy, iz, paletteIndex)`.
* **Output.** Writes the file `structure_scan.txt` in the game's working directory, overwriting it:
  1. the header line `ArrayList<MBlock> blockList = new ArrayList<MBlock>();`
  2. the palette lines
  3. a line `---`
  4. an empty line
  5. the entry count
  6. one `x,y,z,i` line per entry

  Items 5 and 6 are byte-for-byte the structure-file format.

### Structure Test Tool (`items/StructureTestTool`)
* Registry name `structureTestTool`, texture `textures/items/structureTestTool.png`, max stack 1. Lang: `techguns.item.structureTestTool.name=Structure Test Tool`. Tooltip line from `techguns.structureTestTool.Tooltip=Tool for structure spawning/world editing [WIP]`.
* NBT on the stack: `x1`, `y1`, `z1`, `mode` (int).
* **Sneak + right-click** cycles the mode: `mode = (mode+1) % 7`. When there is no `mode` tag yet, it is set to 1. The client prints `structure tool mode set to "<name>".`
* **Normal right-click** works in two clicks, with the same Position1/Position2/Size chat as the scan tool. On the second click the server runs the current mode on the selected box (min corner + size). The default mode is 0 when the tag is absent.

| Mode | Label shown | What actually runs |
|---|---|---|
| 0 | Structure | Places `AircraftCarrierNew` at the min corner with a random rotation and sizeY 24, using the selection's sizeX/sizeZ for the rotation centre. Prints the origin to stdout. Other structures are left commented out in the source as alternatives. |
| 1 | Camp | **Runs the city generator** (`GenericCity.testCity.createCity` with colour type from biome). The label is stale (WGEN-18). |
| 2 | Smooth | `apply2DHeightmapFilter` with the 5×5 Gaussian over the box. |
| 3 | Flatten | `flattenArea(maxDiff = 2)`. |
| 4 | Remove Junk | `removeJunkInArea`. |
| 5 | Sphere | Hollow glass sphere: radius `(sizeX+sizeZ)/4`, centre `(x+sizeX/2, y+radius+1, z+sizeZ/2)`, inside cleared to air with the air marker, 1-thick glass shell. |
| 6 | Cylinder | Glass-walled tube of radius 3 between the two raw clicked positions (not the min corner). Inside is air. |

### Block Reader Tool
`blockReaderTool` (lang `techguns.item.blockReaderTool.name=Block Reader Tool`) is registered next to these tools, but its class is outside this area. See 06-items.
