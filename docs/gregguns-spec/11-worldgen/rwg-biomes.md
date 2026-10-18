# World type and biomes

- **Techguns baseline:** [structure-system.md § Spawn pipeline](../../techguns-spec/11-worldgen/structure-system.md#1-spawn-pipeline-world-generator), [structure-system.md § Structure base class](../../techguns-spec/11-worldgen/structure-system.md#2-structure-base-class) (color types), [structure-catalog.md § A5](../../techguns-spec/11-worldgen/structure-catalog.md#a5-alien-bug-nest-alienbugneststructure--alienbugnest)
- **Design records:** [0005](../../design/0005-world-presence.md)
- **Pack mods:** Realistic-World-Gen (`RWG`, alpha-1.5.2); DefaultWorldGenerator (`defaultworldgenerator`, 0.4.0); Biomes O' Plenty (`BiomesOPlenty`, 2.1.0.2308); Roguelike-Dungeons (`Roguelike`, 1.6.6-GTNH, as precedent)
- **Roadmap:** M8

## What changes from Techguns

Techguns picks structures by the biome at the chunk corner and tests it with `BiomeDictionary` types, except for the camp color, which tests vanilla biome identity ([WGEN-32](../../techguns-spec/11-worldgen/index.md#suspected-bugs)). GregGuns keeps the type-based tests and removes every identity test and every biome ID. The pack forces RWG, so the biome set is not vanilla.

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| Land or water is `OCEAN` in the dictionary types | Same, via `BiomeDictionary.isBiomeOfType(biome, OCEAN)` | RWG registers its ocean base biomes with `OCEAN` (below) |
| Alien Bug Nest: `SANDY` or `WASTELAND` | `(SANDY or WASTELAND) and not (RIVER or OCEAN or BEACH)`, from a config list of type names | RWG's hot river is `SANDY`, and its cold plains are `WASTELAND` |
| Camp color: `BlockUtils.getBiomeType` (vanilla identity) | Dictionary types: `COLD`/`SNOWY` snow, `SANDY`/`MESA` desert, `NETHER` nether, else woodland. `BEACH` is dropped from the desert list | Fixes WGEN-17 and WGEN-32. In RWG every ocean base biome is `BEACH` |
| Any biome for most structures | Any non-`OCEAN` biome by default, plus a config blacklist of type names | Same default, but admins can exclude `MUSHROOM`, `RIVER` and so on |

## Integration points

### Realistic-World-Gen: the pack's only Overworld generator

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/defaultworldgenerator.cfg`, `World Generator` = `RWG`, `Lock World Generator` = `true`. New worlds are RWG and the player cannot change that. `reference/gtnh/2.9.0-RC-2/.minecraft/config/RWG.cfg` has `Generate Villages` = `true` and `Generate Mineshafts` = `true`. Existing saves of another world type can still be loaded, so GregGuns must not assume RWG.
- **Source:**
  - [`WorldTypeRealistic`](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.2/src/main/java/rwg/world/WorldTypeRealistic.java) supplies the chunk manager and generator.
  - [`ChunkManagerRealistic#getBiomeGenAt`](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.2/src/main/java/rwg/world/ChunkManagerRealistic.java#L195-L196) returns the realistic biome's `baseBiome`, a normal `BiomeGenBase`. A `getBiomeGenAt` call at a chunk corner therefore gives a biome that has dictionary types.
  - [`BaseBiomes`](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.2/src/main/java/rwg/biomes/base/BaseBiomes.java#L40-L77) registers those base biomes: rivers (L40-L46), oceans (L48-L63) and land (L65-L77).
  - [`ChunkGeneratorRealistic`](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.2/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L190-L195) runs vanilla village, mineshaft and stronghold generators ([populate: L487-L492](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.2/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L487-L492)).
- **Dictionary types the RWG base biomes carry** (from `BaseBiomes`; Biomes O' Plenty biomes keep their own registrations):

  | RWG base biome | Dictionary types | Techguns effect |
  |---|---|---|
  | Ice/Cold/Temperate/Hot/Wet/Oasis Ocean | `OCEAN`, `BEACH` + climate (hot adds `SANDY`) | WATER type. Carrier and Submarine candidates |
  | Ice/Cold/Temperate/Hot/Wet/Oasis River | `RIVER` + climate (hot adds `SANDY`) | LAND type. Hot River passes the bug-nest `SANDY` test unless excluded |
  | Snow Desert | `COLD`, `SNOWY`, `WASTELAND` | Snow camo. Bug-nest candidate |
  | Cold Plains | `COLD`, `WASTELAND` | Snow camo. Bug-nest candidate |
  | Snow Forest, Cold Forest, Redwood | `COLD`, `CONIFEROUS`, `FOREST` (+ `SNOWY`) | Snow camo |
  | Hot Plains, Hot Forest | `HOT`, `SAVANNA`, `PLAINS`, `SPARSE` | Woodland camo (not `SANDY`) |
  | Hot Desert | `HOT`, `DRY`, `SANDY` | Desert camo. Bug-nest candidate |
  | Plains | `PLAINS` | Woodland camo |
  | Jungle, Tropical Island | `HOT`, `WET`, `JUNGLE` | Woodland camo |

- **Precedent:** Roguelike-Dungeons excludes `RIVER`, `BEACH`, `MUSHROOM` and `OCEAN` by type ([`Dungeon#validLocation`](https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/dungeon/Dungeon.java#L165-L211)). That is the pack's own idiom for "not a river, coast or sea".
- **Biomes O' Plenty:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/biomesoplenty/biomeweights.cfg` sets `overworld biome weights`. BoP biomes reach RWG only where `SupportBOP` wires them in (many entries are commented out), and BoP registers its own dictionary types. GregGuns neither lists nor reads BoP biomes by name or ID.
- **GregGuns does:** one `StructureBiomes` helper that takes a `BiomeGenBase` and answers `isWater`, `allowsLand(structure)` and `campColor`. All answers come from `BiomeDictionary`. It reads no biome ID.
- **Pack-side change:** none.

## Requirements

- **GGS-11-01** No GregGuns worldgen code stores, compares or configures a biome ID. Config uses `BiomeDictionary.Type` names.
- **GGS-11-02** `isWater(biome)` is `isBiomeOfType(biome, OCEAN)`. A `RIVER` biome is LAND.
- **GGS-11-03** The Alien Bug Nest whitelist defaults to `(SANDY or WASTELAND) and not (RIVER or OCEAN or BEACH)`. It is a config list of type names.
- **GGS-11-04** Camp color comes from dictionary types as in the table above. `BEACH` does not select desert.
- **GGS-11-05** Every structure has a config blacklist of dictionary type names. The default blacklist for land structures is empty.
- **GGS-11-06** Code that finds no dictionary types for a biome (a modded biome that never registered any) treats it as LAND with woodland color and does not throw.

## Tests

- GGS-11-01: a source scan test (no `biomeID` in `worldgen` classes).
- GGS-11-02 to 04, 06: unit tests with stub biomes registered in `BiomeDictionary`, one per row of the table above.
- GGS-11-05: unit test of the config parser.
- Manual: `./gradlew runServer`, new world, then a `/gregguns test` scenario that reports the answers at a hot desert, a cold plain and an ocean.

## Open questions

- Should the Alien Bug Nest also require `HOT`, so it never appears in cold plains? Settled by a design record only if the default above looks wrong in play.
