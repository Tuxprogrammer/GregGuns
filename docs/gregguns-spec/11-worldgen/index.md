# 11: World generation (GregGuns delta)

## Overview

Techguns' world generation is ore veins, ore clusters, oil holes and structures ([baseline](../../techguns-spec/11-worldgen/index.md)). [Design 0005](../../design/0005-world-presence.md) keeps the structures and drops everything else:

- **No ore generation of any kind.** GT owns ores and dreamcraft owns oil.
- **Overworld-only structures.** Each is config-gated, uses dictionary-type biome rules and never cascades worldgen (WGEN-04).
- **No village bunker.**
- **Tier-appropriate loot.**

The pack makes four things matter: it forces the RWG world generator, it ships ArchaicFix and Hodgepodge worldgen throttles, it loads loot through TooMuchLoot with `OVERRIDE`, and it uses Chisel. The sheets below cover those. Most of this area is not a pack integration, so only five sheets exist.

## Contents

| Sheet | Topic |
|---|---|
| [rwg-biomes.md](rwg-biomes.md) | RWG as the only Overworld generator, and how Techguns' biome tests map to `BiomeDictionary` types |
| [structure-placement.md](structure-placement.md) | Generator order against GT, the populate window (no cascading worldgen), seeded grid, dimension allow-list, Roguelike-Dungeons and villages |
| [structure-loot.md](structure-loot.md) | Which loot category each structure draws from, and its tier ceiling. Table contents are in [06-items/pack-loot.md](../06-items/pack-loot.md) |
| [chisel-palette.md](chisel-palette.md) | Palette keys with Chisel resolved at `postInit` and a fallback (fixes WGEN-24) |
| [layout-format.md](layout-format.md) | The GregGuns layout text format, rotation tables, weighted palette entries, validation and the window-clipped stamper |
| [ore-and-oil.md](ore-and-oil.md) | What replaces Techguns' ores, clusters and oil holes: GT and dreamcraft |

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Biome tests for land, water, bug nest, camp color | `BiomeDictionary` types only, no IDs | RWG, Biomes O' Plenty | [rwg-biomes.md](rwg-biomes.md) |
| `WorldGenTGStructureSpawn`, weight 4 | Kept at weight 4, runs before GT ores | GT5-Unofficial `GTWorldgenerator` | [structure-placement.md](structure-placement.md) |
| Structures reaching into ungenerated chunks (WGEN-04) | Writes limited to the populate window; seeded plans | ArchaicFix cascade detector, Hodgepodge throttle | [structure-placement.md](structure-placement.md) |
| Hard-coded dimension 0 (WGEN-28) | Config allow-list, default `[0]` | TwilightForest, PersonalSpace, Galacticraft, GalaxySpace | [structure-placement.md](structure-placement.md) |
| Grid at chunk multiples (a BIG structure at spawn) | Seeded offset inside each grid cell | Roguelike-Dungeons (same idea) | [structure-placement.md](structure-placement.md) |
| Chest fill from `DUNGEON_CHEST` plus TG tables | GregGuns categories with a tier ceiling per structure | TooMuchLoot | [structure-loot.md](structure-loot.md) |
| Chisel blocks in palettes (WGEN-24) | Palette keys with fallback | Chisel | [chisel-palette.md](chisel-palette.md) |
| Titanium and uranium veins, ore clusters | Not built | GT ore generation | [ore-and-oil.md](ore-and-oil.md) |
| Oil holes | Not built | dreamcraft oil generator | [ore-and-oil.md](ore-and-oil.md) |

### No integration point

For these, the behavior is the Techguns spec plus the design records.

| Topic | Why no sheet |
|---|---|
| Village bunker ([village.md](../../techguns-spec/11-worldgen/village.md)) | Dropped by 0005. VillageNames only renames and signs vanilla villages, and RWG's villages are vanilla |
| City generator, `SmallHouse`, legacy carriers | Dormant in Techguns and not selected by 0005 |
| Monster spawners in structures | **Deviation:** Techguns has its own spawner blocks; GregGuns places the vanilla `minecraft:mob_spawner` with GregGuns NBT ([design record 0024](../../design/0024-vanilla-spawners.md), [structure-placement § Spawner marks](structure-placement.md#spawner-marks), [10-entities-npcs spawning](../10-entities-npcs/spawning.md#spawner-npcs)). `mobsinfo.cfg` has `addAllEnderIOSpawnersToNEI` = `false`, which concerns EnderIO's spawner only |
| Camp flag NPC spawning | Area 10 |
| Building Scan Tool, Structure Test Tool, structure file format ([structure-system.md § 6-7](../../techguns-spec/11-worldgen/structure-system.md#6-structure-data-file-format-assetstechgunsstructures)) | Authoring tools with no pack link. Structure files stay as in Techguns' format description, written fresh (originality rule) |
| Textures ([assets.md](../../techguns-spec/11-worldgen/assets.md)) | Art direction, not integration ([0006](../../design/0006-art-direction.md)) |

## Pack-side changes

None required. GregGuns structures use their own loot categories, their own config and no other mod's config.

For testing only, set `logCascadingWorldgen` = `true` in `config/archaicfix.cfg` of a development instance. Do not ship it.

| File | Change | Why | Sheet |
|---|---|---|---|
| (none) | – | – | – |

## Related areas

- [10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md): camp flags, spawners, natural spawn weights and caps; Infernal Mobs eligibility of camp NPCs.
- [06-items/pack-loot.md](../06-items/pack-loot.md): loot table contents and TooMuchLoot handling.
- 07-blocks: concrete, lamps, TG chests and the monster spawner block used by structures.
- 09-machines-utility: the camp flag.

## Open questions

- **Gas station.** 0005 lists it among the natural structures, but Techguns leaves it dormant: it has no register entry and no spawn size ([C2](../../techguns-spec/11-worldgen/structure-catalog.md#c2-city-gas-station-citygasstation)). The plan is to register it as a SMALL land structure (13 x 16 as built) and fix its door rotation (WGEN-31). That makes the SMALL pool three structures with a 1/3 pick each. A design record is only needed if that dilution is unwanted.
- **Alien Bug Nest default.** GGS-11-15 defaults it to disabled because 0005 gates the Alien Bug as late-game content. Confirm when area 10 fixes the Alien Bug config.
- **Ore Cluster Scanner, Ore Drill and cluster blocks.** See [ore-and-oil.md](ore-and-oil.md).
- **Village avoidance.** See [structure-placement.md](structure-placement.md).
- **Multi-chunk ground height.** See [structure-placement.md](structure-placement.md).
