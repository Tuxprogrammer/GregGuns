# 11: World generation (Techguns 1.7.10, v0.1.2 alpha)

## Overview


This area covers everything Techguns adds to world generation:
* overworld ore veins
* the ore-cluster deposit system (generation, oil holes, scanner and debugger tools)
* every structure: procedural ones and the scanned ones stored in `assets/techguns/structures/`
* structure loot
* the dormant village piece
* the tools used to author structures


| System | Active? | Where |
|---|---|---|
| Copper / tin / lead veins | only when their config flags are on (default **off**) | `OreGenerator` |
| Titanium and uranium veins | on by default | `OreGenerator` |
| Natural ore-cluster deposits | **off**: generator registration is commented out | [`OreClusterWorldspawn`](ore-generation.md#23-natural-cluster-generation-oreclusterworldspawn-disabled) |
| Oil holes (oil ore-cluster columns in bedrock) | on by default, 5 % of chunks | [`WorldGenOilHoles`](ore-generation.md#3-oil-holes-worldgenoilholes) |
| Structures | on by default | `WorldGenTGStructureSpawn` |
| Village bunker | **off**: registration commented out | `village/*` |
| City generator | **off**: spawn entry commented out; reachable from the test tool | [`GenericCity`](structure-catalog.md#c1-city-generator-genericcity-genericcitysegment-citystructure-citybuilding-citybuildingtype-buildingstyle-cityroad-iroad-partsstaircase) and related classes |

Natural structures (all Overworld only):

| Size class (grid) | Land | Water (ocean biomes) |
|---|---|---|
| SMALL (every 16 chunks) | Small Factory Building, Small Train Station | – |
| MEDIUM (every 32 chunks) | Medium House, Big Bunker, Alien Bug Nest (sandy/wasteland only) | – |
| BIG (every 64 chunks) | Military Base (random 32–79 × 32–79, NPC flag) | Aircraft Carrier (NPC flag), Submarine |

## Contents

| File | Topic |
|---|---|
| [ore-generation.md](ore-generation.md) | Ore veins ([`OreGenerator`](ore-generation.md#1-ore-veins-oregenerator)), the ore-cluster block data and the 21 registered cluster types, the disabled natural cluster spawner, oil holes, the Ore Cluster Scanner and Debugger |
| [structure-system.md](structure-system.md) | Spawn grid and weighted structure pick, [`Structure`](assets.md#structure-data-files) base class, rotation, terrain probes and validators, terrain shaping, [`MBlock`](structure-system.md#mblock) / [`MultiMBlock`](structure-system.md#multimblock-random-palette-entry) and rotation tables, the scanned-structure placement passes, **the structure file format**, and the authoring workflow (Building Scan Tool, Structure Test Tool) |
| [structure-catalog.md](structure-catalog.md) | One section per structure: the 8 natural spawns (with full palettes for the scanned ones), the 9 military-camp parts, and the dormant city / gas station / small house / legacy carrier |
| [loot.md](loot.md) | Which chest uses which table, chest counts per structure, and the full contents of all TG loot tables plus additions to vanilla pools |
| [village.md](village.md) | The (unregistered) village bunker piece. There are no professions or trades. |
| [assets.md](assets.md) | Structure data files, the 25 related textures, sounds (none), and lang keys and hard-coded strings |
| [structure-notes/factory_small.md](structure-notes/factory_small.md) | Word-only builder notes: Small Factory Building (L-shaped shed, 2 chests) |
| [structure-notes/train_station.md](structure-notes/train_station.md) | Word-only builder notes: Small Train Station (broken rails, gabled house, loft chest) |
| [structure-notes/gas_station.md](structure-notes/gas_station.md) | Design brief for the new Gas Station (shop, pump canopy, fixed doors) |
| [structure-notes/house_medium.md](structure-notes/house_medium.md) | Word-only builder notes: Medium House (two wings, attic, 18 chests) |
| [structure-notes/big_bunker.md](structure-notes/big_bunker.md) | Word-only builder notes: Big Bunker (buried levels, 2 Zombie Holes, 14 chests) |

## Config keys that gate worldgen

The keys are defined in [`TGConfig`](../01-core/config.md); see **[01-core](../01-core/index.md) / config.md** for the full config file. This table lists what each key gates.

| Key (category) | Default | Gates |
|---|---|---|
| [`doOreGenLead`](../01-core/config.md#category-general) (general) | false | Creates and registers the `oreLead` block and its `oreLead` OreDict entry, and adds lead veins. If any of the four ore flags is true, `OreGenerator` is registered. |
| [`doOreGenCopperTin`](../01-core/config.md#category-general) (general) | false | `oreCopper` / `oreTin` blocks, OreDict entries and veins |
| [`doOreGenTitanium`](../01-core/config.md#category-general) (general) | true | titanium veins ([`oreTitanIron`](../07-blocks/block-catalog.md#registered-blocks) meta 0; the block itself is always registered) |
| [`doOreGenUranium`](../01-core/config.md#category-general) (general) | true | uranium veins (`oreTitanIron` meta 1) |
| `addCustomOreCluster`, `addCustomOreCluster2` (general) | false | Adds empty MineTweaker-fillable cluster blocks to [`TGBlocks.oreclusters`](../07-blocks/block-catalog.md#registered-blocks), so the scanner recognizes them |
| [`SpawnStructures`](../01-core/config.md#category-general) (general) | true | Registers `WorldGenTGStructureSpawn` |
| [`StructureSpawnWeightBig`](../01-core/config.md#category-general) (general) | 64 (1–100000) | BIG grid spacing in chunks: `cx % v == 0 && cz % v == 0` |
| [`StructureSpawnWeightMedium`](../01-core/config.md#category-general) (general) | 32 (1–100000) | MEDIUM grid spacing (only checked if the BIG test failed) |
| [`StructureSpawnWeightSmall`](../01-core/config.md#category-general) (general) | 16 (1–100000) | SMALL grid spacing (only checked if BIG and MEDIUM failed) |
| [`spawnOilHoles`](../01-core/config.md#category-spawn-controll-sic) ("Spawn Controll") | true | Registers `WorldGenOilHoles`, only if also `TGFluids.OIL != TGFluids.LAVA` |
| [`oilHoleFrequency`](../01-core/config.md#category-spawn-controll-sic) ("Spawn Controll") | 0.05 (0–1) | Per-chunk probability of an oil hole |
| [`oreClusterMultipliersAmount`](../01-core/config.md#category-ore-drills), [`oreClusterMultipliersPower`](../01-core/config.md#category-ore-drills), and `…2` variants ("Ore Drills") | per-type arrays, see ore-generation.md | Ore Drill yield per cluster meta (consumed by [08](../08-machines-production/index.md)) |

Registration order and weights: `OreGenerator` in preInit (weight 1). `WorldGenTGStructureSpawn` (weight 4) and `WorldGenOilHoles` (weight 3) in postInit.

## Related areas
* Blocks used by structures (concrete, sandbags, camo nets, lamps, TG doors and ladders, TG chests, monster spawner, bug-nest blocks, air marker, container, metal panels, ore clusters): **[07-blocks](../07-blocks/index.md)**
* Camp flag ([`basicMachine`](../07-blocks/block-catalog.md#registered-blocks) meta 7, [`CampFlagTileEnt`](../09-machines-utility/shared-rendering.md#1-basicmachine-metas-owned-by-area-09)) NPC-spawn logic: **[09-machines-utility](../09-machines-utility/index.md)**
* Ore Drill (cluster mining, config multipliers): **[08-machines-production](../08-machines-production/index.md)**
* NPCs: Army Soldier, Commando, Zombie Soldier, Alien Bug, Attack Helicopter: **[10-entities-npcs](../10-entities-npcs/index.md)**
* Scanner and debugger items, crowbar, loot-table registration code: **[06-items](../06-items/index.md)**. "OreClusterPing" particle: **[04-projectiles-fx](../04-projectiles-fx/index.md)**.

## Coverage
* **Source in scope, all read in full:**
  * `worldgen/` 13 top-level classes
  * `worldgen/structures/` 28 classes and `parts/Staircase`
  * `worldgen/village/` 2 classes
  * `util/MBlock`, `MultiMBlock`, [`BlockPosInd`](structure-system.md#blockposind), [`BlockUtils`](../01-core/utilities.md#blockutils)
  * `items/BuildingScanTool`, [`StructureTestTool`](../06-items/special-items.md#structure-test-tool-structuretesttool)

  That is 50 Java files. The embedded `int[][]` literals of the legacy [`AircraftCarrier`](structure-catalog.md#c5-legacy-carrier-aircraftcarrier-aircraftcarrier2) / [`AircraftCarrier2`](structure-catalog.md#c5-legacy-carrier-aircraftcarrier-aircraftcarrier2) were analyzed by counting and extent, not inspected cell by cell. All 7 structure data files were fully decoded.
* **Supporting files read:**
  * ore-cluster and loot sections of `TGBlocks`, [`TGItems`](../06-items/item-catalog.md), `TGConfig`, [`Techguns`](../01-core/lifecycle-and-registration.md#lifecycle-overview), [`TGFluids`](../06-items/fluids.md)
  * [`OreCluster`](../07-blocks/block-catalog.md#registered-blocks), [`OreClusterScanner`](../06-items/special-items.md#ore-cluster-scanner-oreclusterscanner-techgunsoreclusterscanner), [`oreClusterDebugger`](../06-items/special-items.md#ore-cluster-debugger-oreclusterdebugger)
  * `CampFlagTileEnt`, [`BlockTGMonsterSpawner`](../07-blocks/special-blocks.md#monster-spawner-holes-blocktgmonsterspawner), [`TGSpawnerTileEnt`](../10-entities-npcs/spawning.md#3-monster-spawner-blocks-tgspawnertileent)
  * [`NPCUtil`](../10-entities-npcs/spawning.md#4-camp-flags-and-npcutil), [`MathUtil`](../01-core/utilities.md#mathutil), [`TGChiselBlocks`](../07-blocks/special-blocks.md#19-tgchiselblocks-block-constants-for-structures), [`GenericBlock`](../07-blocks/block-catalog.md#conventions-and-defaults)
* **Textures:** 25 described (see assets.md). **Sounds:** none in scope.
* **Consistency pass additions:** every structure and camp part in [structure-catalog.md](structure-catalog.md) now ends with a **Sheet** table (class and register entry, spawn conditions, size declared and as built, build steps, palette, loot, spawners and NPCs, camp flag and caps, NBT, bugs); [ore-generation.md](ore-generation.md) section 4 has per-ore, oil-hole and per-cluster sheets with default yields; [assets.md](assets.md) quotes the measured size and colors of all 25 textures and carries the full lang section (key table with line numbers, generated strings, hard-coded English, missing keys). Lang keys covered: all keys in `en_US.lang` whose subject worldgen places, mines or uses.

## Known limits
* **Sounds cannot be heard.** None are in scope (no class here plays one), so this limit is moot for the area.
* **Texture colors are measured, not seen.** The visual descriptions in [assets.md](assets.md) were written in the earlier pass; the measured averages and dominant colors were added from `ASSET-FACTS.md` and cross-check them.
* **Legacy carrier data.** The embedded `int[][]` literals of `AircraftCarrier` and `AircraftCarrier2` were analyzed by count and extent only, not cell by cell. They are dead code.
* **No runtime verification.** Nothing was run in a game. Spawn chances (1/2, 1/3), per-chunk ore counts and the cluster yield figures are derived by arithmetic from the source and config defaults. The ore "upper bound per chunk" is attempts times mean vein size and ignores clipping by caves and non-stone blocks.
* **Chisel and other mods.** Chisel block names and metas are taken from the source's references; the mod itself was not inspected, so what a given Chisel meta looks like is not described.
* **Camp randomness.** The binary-space subdivision and leaf matching depend on the camp's `Random`; outcomes are described as rules, not enumerated.
* **Sound-free, GUI-free area.** The area has no sounds, no GUI and no tile entities of its own, so those sheet fields read *none*. The camp flag and monster spawner tile entities are documented in [09-machines-utility](../09-machines-utility/index.md) and [10-entities-npcs](../10-entities-npcs/spawning.md).

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| WGEN-01 | Natural ore clusters never generate | `java/techguns/Techguns.java:647` | `OreClusterWorldspawn` registration is commented out. Apart from oil holes, the whole Ore Drill economy depends on clusters that players cannot find in survival. | gameplay |
| WGEN-02 | Cluster veins would all be meta 0 | `java/techguns/worldgen/OreClusterWorldspawn.java:98` | Uses the meta-less `WorldGenMinable` constructor, so every cluster type spawns as meta 0 (iron, or nether quartz). Rarity ≥ 5 would crash with `nextInt(0)`. | gameplay |
| WGEN-03 | Oil holes spawn without an oil fluid | `java/techguns/Techguns.java:751`, `worldgen/WorldGenOilHoles.java:60` | The guard compares `OIL != LAVA`, but `OIL` is null when no mod registers "oil". Holes still spawn, as [`oreCluster2`](../07-blocks/block-catalog.md#registered-blocks) meta 3, which is then a stone placeholder (mining level 0, hidden from creative). | gameplay |
| WGEN-04 | Cascading worldgen | `java/techguns/worldgen/WorldGenTGStructureSpawn.java:96` | Structures are anchored at the chunk corner, and carrier, base and bunker extend 17–79 blocks into neighboring, possibly ungenerated, chunks. This forces their generation (lag and runaway chunk loading). Structures also ignore existing structures. | gameplay |
| WGEN-05 | Terrain check and build area differ after rotation | `java/techguns/worldgen/structures/Structure.java:163-205` | The origin passed to `setBlocks` already includes the rotated-bbox minimum, then `setBlocks` rotates again around the center. For non-square footprints the built area is shifted from the checked one. Only BigBunker has hand-tuned compensating shifts. | gameplay |
| WGEN-06 | Ocean structures checked unrotated | [`java/techguns/worldgen/structures/AircraftCarrierNew.java:79-92`](structure-catalog.md#a7-aircraft-carrier-aircraftcarriernew), [`Submarine.java:64-77`](structure-catalog.md#a8-submarine-submarine) | The water or sea-floor check samples the unrotated box at the chunk corner, but the structure is rotated around its center. For directions 1 and 3 the carrier occupies x 17–38, z −17…37 instead of the checked x 0–54, z 0–21, so it can extend onto land or into cliffs. | gameplay |
| WGEN-07 | Submarine size transposed | [`java/techguns/worldgen/TGStructureSpawnRegister.java:48`](structure-system.md#registered-spawn-table-tgstructurespawnregister-static-init) | `setXZSize(33,7)`, but the data is 7 (X) × 33 (Z). Corner checks and the rotation center (16,3) do not match the hull. | gameplay |
| WGEN-08 | Submarine can spawn on dry land | `java/techguns/worldgen/structures/Submarine.java:64` | The only water test is the biome type OCEAN at the chunk corner. The underwater probe happily returns land heights, so islands or shallows in ocean biomes get a submarine on the surface. | gameplay |
| WGEN-09 | Bug nest ignores failed terrain check | [`java/techguns/worldgen/structures/AlienBugNestStructure.java:24-26`](structure-catalog.md#a5-alien-bug-nest-alienbugneststructure--alienbugnest) | No `y < 0` test. A failed check builds the nest from y = −8, where everything is skipped (wasted work), or unpredictably low. | gameplay |
| WGEN-10 | Carrier NPC flag uses unrotated offset | `java/techguns/worldgen/structures/AircraftCarrierNew.java:158` | `flag.init(54,21,24,8,9,…)` passes the unrotated flag offset while the flag and spawn points are rotated. For rotations ≠ 0 the camp origin, NPC-count box and spawn positions are wrong (NPCs can spawn in the water or inside walls). | gameplay |
| WGEN-11 | Carrier NPC cap lost on reload | `java/techguns/worldgen/structures/AircraftCarrierNew.java:158` (and `tileentities/CampFlagTileEnt.java:144`) | The cap of 9 is not saved. After a chunk reload it is recomputed from size as (54+21)/2·0.75 = 28. | gameplay |
| WGEN-12 | Possible NPE placing camp flag | [`java/techguns/worldgen/MilitaryCamp.java:228-232`](structure-catalog.md#a6-military-base-militarybasestructure--militarycamp) | Casts `getTileEntity` to `CampFlagTileEnt` without a null or instance check. If the flag block fails to place (y out of range, or replaced), worldgen crashes. | crash |
| WGEN-13 | Village bunker builds from the wrong box | [`java/techguns/worldgen/village/VillageBunker.java:52`](village.md#villagebunker-extends-vanilla-structurevillagepieceshouse1) | Uses the per-chunk clipping box `bb` instead of the piece's `boundingBox`, so position and size are wrong and a multi-chunk piece is built repeatedly. Registration is commented out (`Techguns.java:755-757`). | gameplay |
| WGEN-14 | MultiMBlock weight bias | `java/techguns/util/MultiMBlock.java:60` | `nextInt(total+1)` with `roll <= cumulative` gives option 0 one extra weight point. For example, rails come out at 3/4 instead of 2/3. | cosmetic |
| WGEN-15 | Area validator skips the far edge | `java/techguns/util/BlockUtils.java:683-694` | The "clamp to edge" branch can never run, so the far edge is sampled only when the size is a multiple of `step`. Overhangs and drops at the far edge go undetected. | gameplay |
| WGEN-16 | `flattenArea` median wrong | `java/techguns/util/BlockUtils.java:111-123` | Off-by-one index and integer math; with ≤ 2 columns the median stays 0, so a flatten with span > maxDiff would dig columns down to y≈0. | gameplay |
| WGEN-17 | SAVANNA missing from sand types | `java/techguns/worldgen/structures/Structure.java:37` | `BiomeDictionary.Type.SAVANNA.BEACH` evaluates to BEACH; SAVANNA was presumably meant as its own entry. | cosmetic |
| WGEN-18 | Test-tool mode labels wrong | `java/techguns/items/StructureTestTool.java:30,42-44,123` | Mode 1 is labeled "Camp" but runs the city generator. The first sneak-click with no tag jumps straight to mode 1. | cosmetic |
| WGEN-19 | Scanner charges for aborted scans | `java/techguns/items/OreClusterScanner.java:76` | 300 RF is extracted on every release, even when held < 40 ticks and no scan happens. | gameplay |
| WGEN-20 | SmallHouse ruin pass uses sizeY as sizeZ | [`java/techguns/worldgen/structures/SmallHouse.java:126`](structure-catalog.md#c3-smallhouse) | `destroyTop(world, sizeX, sizeY, …)` passes 7 as the Z extent. Unused class. | cosmetic |
| WGEN-21 | City ladder backing uses wrong index | [`java/techguns/worldgen/structures/CityBuildingType.java:368-369`](structure-catalog.md#c1-city-generator-genericcity-genericcitysegment-citystructure-citybuilding-citybuildingtype-buildingstyle-cityroad-iroad-partsstaircase) | The ladder loop places backing blocks at `y+i+1` (story index) instead of `y+j+1`. `(stairsdir−1)%4` is −1 for direction 0. Dormant code. | visual |
| WGEN-22 | City junk-removal box asymmetric | [`java/techguns/worldgen/structures/CityStructure.java:33`](structure-catalog.md#c1-city-generator-genericcity-genericcitysegment-citystructure-citybuilding-citybuildingtype-buildingstyle-cityroad-iroad-partsstaircase) | Starts at −5 but extends only sizeX+5 (should be +10). Dormant. | cosmetic |
| WGEN-23 | Structure-file loading is fragile | `java/techguns/util/BlockUtils.java:835-857` | A missing resource throws an uncaught exception inside a static initializer (`ExceptionInInitializerError` the first time a structure class loads). A header count smaller than the line count causes AIOOBE; a larger count leaves zero rows that place palette[0] at (0,0,0). | crash |
| WGEN-24 | Hard Chisel dependency | `java/techguns/worldgen/structures/AircraftCarrierNew.java:10` (also [`HouseMedium`](structure-catalog.md#a3-medium-house-housemedium), [`FactoryHouseSmall`](structure-catalog.md#a1-small-factory-building-factoryhousesmall), [`SmallTrainstation`](structure-catalog.md#a2-small-train-station-smalltrainstation), `Submarine`, `MilitaryCamp`, `TGChiselBlocks`); `Techguns.java:114` | Worldgen classes reference `team.chisel.init.ChiselBlocks` in static initializers, but the mod declares only `after:Chisel`. Without Chisel, structure generation throws `NoClassDefFoundError`. | crash |
| WGEN-25 | Loot not seed-deterministic | `java/techguns/util/MBlock.java:28`; [`structures/Barracks.java:184`](structure-catalog.md#b5-barracks-barracks); [`structures/Bunker.java:242`](structure-catalog.md#b4-small-bunker-bunker-types-03) | Chest contents and crate rolls use a static unseeded `Random` or `world.rand` instead of the chunk-seeded random. | cosmetic |
| WGEN-26 | Junk removal drains water | `java/techguns/util/BlockUtils.java:242-257` | Clears every non-ground-material block from the top down, including water, lava, ice and snow. Camps at lake edges cut holes. | gameplay |
| WGEN-27 | NPC count query loads chunks | `java/techguns/util/BlockUtils.java:940-946` | The `chunkExists` guard is commented out, so `getChunkFromChunkCoords` may load chunks every 60 ticks from the camp flag. | gameplay |
| WGEN-28 | Dimension whitelist ignored | [`java/techguns/worldgen/TGStructureSpawn.java:55-61`](structure-system.md) | `dimensionIDs` / `dimensionMatches` is never consulted (only the hard-coded dim-0 check applies). | cosmetic |
| WGEN-29 | City building ignores ground height | `java/techguns/worldgen/GenericCity.java:131-132` | Computes the ground y after flattening, then builds at `segment.y` (the city base y). Dormant. | visual |
| WGEN-30 | Declared XZ sizes don't match data | `java/techguns/worldgen/TGStructureSpawnRegister.java:38-43` | BigBunker 32×17 (data 33×18), FactoryHouseSmall 11×10 (data 9×11), SmallTrainstation 11×12 (data 11×11). Rotation center and terrain check are off by 1–2 blocks. | visual |
| WGEN-31 | Gas station doors not rotated | [`java/techguns/worldgen/structures/CityGasStation.java:38-41`](structure-catalog.md#c2-city-gas-station-citygasstation) | Lower door halves have no DOOR rotation type, and their metas don't pair with the upper halves. Dormant. | cosmetic |
| WGEN-32 | Camp color ignores modded biomes | `java/techguns/worldgen/MilitaryCamp.java:171` | Uses `BlockUtils.getBiomeType` (vanilla biome identity list) instead of the BiomeDictionary rule, so modded deserts and snow biomes always get woodland camo. | cosmetic |
| WGEN-33 | Tree trunks count as ground | `java/techguns/util/BlockUtils.java:515` | The wood check in `getHeightValueNoTreesIgnoreLiquid` is commented out, so trunks raise the sampled height. Spawns in forests mostly fail or sit on trunk height. | gameplay |
| WGEN-34 | Scan tool emits unusable palette code | [`java/techguns/items/BuildingScanTool.java:83`](../06-items/special-items.md#building-scan-tool-buildingscantool) | Generates `Block.getBlockFromName(<unlocalized name>)`, which does not resolve. Palettes had to be rewritten by hand. | cosmetic |
| WGEN-35 | Container length parameter ignored | [`java/techguns/worldgen/structures/Containers.java:15,22`](structure-catalog.md#b2-containers-containers) | `containerMinLength` is stored by the constructor but never read, so the minimum row length has no effect (rows always run the full long side, and a leftover strip narrower than width + 1 stays empty). | cosmetic |
