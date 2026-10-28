# Structure placement in a GTNH world

- **Techguns baseline:** [structure-system.md § Spawn pipeline](../../techguns-spec/11-worldgen/structure-system.md#1-spawn-pipeline-world-generator), [§ Structure base class](../../techguns-spec/11-worldgen/structure-system.md#2-structure-base-class), [index.md § Config keys](../../techguns-spec/11-worldgen/index.md#config-keys-that-gate-worldgen), bugs [WGEN-04, 05, 06, 07, 08, 28](../../techguns-spec/11-worldgen/index.md#suspected-bugs)
- **Design records:** [0005](../../design/0005-world-presence.md)
- **Pack mods:**
  - GT5-Unofficial (`gregtech`, 5.09.54.205)
  - ArchaicFix (`archaicfix`, 0.8.0)
  - Hodgepodge (`hodgepodge`, 2.7.215)
  - Roguelike-Dungeons (`Roguelike`, 1.6.6-GTNH)
  - VillageNames (`VillageNames`, 4.5.19-GTNH)
  - TwilightForest (`TwilightForest`, 2.7.44)
  - PersonalSpace (`personalspace`, 1.0.41)
  - Galacticraft and GalaxySpace
- **Roadmap:** M8 (exit gates: no cascading worldgen, dimension whitelist, GTNH world types)

## What changes from Techguns

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| `IWorldGenerator` weight 4, set in `postInit` | Weight 4 kept | GT forces its ore generator to the end whatever its weight, so ours runs before ores |
| Structures anchored at the chunk corner, up to 79 blocks into ungenerated neighbors ([WGEN-04](../../techguns-spec/11-worldgen/index.md#suspected-bugs)) | Every populate call writes only inside the populate window (below). A structure bigger than the window is built across several calls | ArchaicFix detects cascading worldgen; the M8 exit gate needs none |
| Grid points at chunk multiples of 16/32/64, so a BIG structure sits at chunk (0,0), the world spawn | Each grid cell holds one attempt at a seeded offset inside the cell | Roguelike-Dungeons does the same; no structure on the spawn point |
| Hard-coded dimension 0 check; the dimension list is never consulted (WGEN-28) | One allow-list of dimension IDs from config, default `[0]` | Excludes every pack dimension without naming it |
| Terrain checked on an unrotated box (WGEN-05, 06, 07, 08) | Terrain checked on the rotated footprint, with a water-depth test for ships | Fixes the listed bugs |
| `SpawnStructures` plus three spacing keys | Keep the keys. Add one `enabled` flag per structure (0005: "each config-gated") | Admin control |
| Village bunker (dormant) | Not built (0005) | No integration point; see the index |

## Integration points

### Forge `IWorldGenerator`: order and the populate window

- **Pack config:** none. Order is set by the weight passed to `GameRegistry.registerWorldGenerator`.
- **Source:**
  - GT registers its ore generator with `Integer.MAX_VALUE` ([GTWorldgenerator#L150](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTWorldgenerator.java#L150)). A mixin then moves it to the end of the sorted list, because "the ore world gen expects to be the last pass over the world" ([GameRegistryMixin#L17-L39](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/mixin/java/gregtech/mixin/mixins/early/forge/GameRegistryMixin.java#L17-L39)).
  - Roguelike-Dungeons registers with weight 0 ([Roguelike#L34](https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/Roguelike.java#L34)).
  - GT queues ore work and runs at most 5 queued chunks per call, under a comment "Hack to prevent cascading worldgen lag" ([GTWorldgenerator#L176-L213](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTWorldgenerator.java#L176-L213)).
- **Consequence:** any weight below `MAX_VALUE` runs before GT ores. GregGuns structures are placed first and GT veins are generated afterward. Whether GT replaces only natural stone is a test (below), not a claim.
- **The populate window.** `populate(cx, cz)` runs once the neighboring chunks exist, and vanilla convention treats the 16x16 square from `(cx*16 + 8, cz*16 + 8)` to `(cx*16 + 23, cz*16 + 23)` as the safe area. A block write outside it can force chunk generation.
- **GregGuns does:**
  - plans each structure from the world seed and its grid cell (position, rotation, variant) with no world access;
  - on each populate call, writes only the part of each overlapping plan that falls in the window;
  - for a multi-chunk structure, reads the ground height once, from a column inside the window of the chunk that owns the structure's anchor, and keeps it in a `WorldSavedData` entry so the other chunks use the same value.
- **Pack-side change:** none.

### ArchaicFix: cascading-worldgen detector

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/archaicfix.cfg`: `fixVanillaCascadingWorldgen` = `true`, `logCascadingWorldgen` = `false`, `logCascadingWorldgenStacktrace` = `false`. The pack ships with logging off.
- **Source:** [`MixinChunkProviderServer#populate2WithCascade`](https://github.com/embeddedt/ArchaicFix/blob/0.8.0/src/main/java/org/embeddedt/archaicfix/mixins/common/core/MixinChunkProviderServer.java#L43) wraps every populate in [`CascadeDetectionHelper`](https://github.com/embeddedt/ArchaicFix/blob/0.8.0/src/main/java/org/embeddedt/archaicfix/helpers/CascadeDetectionHelper.java). It logs which mod (found by walking the calling stack) caused a chunk to generate while another chunk was populating.
- **GregGuns does:** nothing at runtime. The chunk-load test in the M8 exit gate sets `logCascadingWorldgen` = `true` in the dev instance and fails on any log line that names `gregguns`.
- **Pack-side change:** none. Do not ship `logCascadingWorldgen` = `true`.

### Hodgepodge: deferred chunk generation

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/hodgepodge.cfg`: `throttleChunkGeneration` = `true`, `chunkGenBudgetMs` = `20`, `maxChunkGenPerPlayerPerTick` = `4`, `preventEntityChunkLoading` = `true`, `limitRecursiveBlockUpdateDepth` = `256`.
- **Source:** [`MixinPlayerManager_ThrottleChunkGen`](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.215/src/main/java/com/mitchej123/hodgepodge/mixins/early/minecraft/fastload/MixinPlayerManager_ThrottleChunkGen.java) spreads generation across ticks.
- **Consequence:** population is not synchronous with a player's move, so a structure may appear a few ticks after its chunk loads. Generation has a 20 ms per-tick budget, so a cascade shows up as server lag as well as a log line.
- **GregGuns does:** keeps each populate call small (no entity ticks, no chunk loads) and never assumes a neighboring chunk is generated.
- **Pack-side change:** none.

### Dimension gate

- **Pack config:**
  - `reference/gtnh/2.9.0-RC-2/.minecraft/config/TwilightForest.cfg`: `dimensionID` = `7`.
  - `reference/gtnh/2.9.0-RC-2/.minecraft/config/personalspace.cfg`: `firstDimensionId` = `180`.
  - `reference/gtnh/2.9.0-RC-2/.minecraft/config/Galacticraft/core.conf`: `idDimensionMoon` = `28`, `idDimensionOverworldOrbit` = `27`, `idDimensionOverworldOrbitStatic` = `26`. GalaxySpace adds more bodies.
- **Source:** dreamcraft's oil generator keeps an allow-list, `OilDimensionWhitelist` = `0` ([OilGeneratorFix#L379](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modfixes/oilgen/OilGeneratorFix.java#L379)). GT asks a per-dimension definition before it generates ore ([GTWorldgenerator#L170](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTWorldgenerator.java#L170)). Both are allow-lists, not block-lists.
- **GregGuns does:** allows only dimension IDs in the config list `structureDimensions`, default `[0]`, plus Ross128b and Ross128ba while `structureRoss128` is on (below). The Nether, End, Twilight Forest, every Personal Space dimension and every Galacticraft or GalaxySpace body are out without being named, and a future dimension is out until added. 0005 says support beyond the Overworld needs a new record.
- **Pack-side change:** none.

### Ross128b and Ross128ba (design record 0019, M9)

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/bartworks.cfg`: `ross128BID` = `64`, `ross128BAID` = `63`, `Ross128Enabled` = `true`, `ross128bRuinChance` = `512`.
- **Source:**
  - The IDs and the on/off switch: [`Configuration.CrossModInteractions`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/configs/Configuration.java#L22-L45).
  - Ross128b is `ChunkProviderRoss128b extends ChunkProviderGenerate` ([source](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bwcrossmod/galacticraft/planets/ross128b/ChunkProviderRoss128b.java)) with the vanilla `WorldChunkManager` ([`WorldProviderRoss128b#L81-L87`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bwcrossmod/galacticraft/planets/ross128b/WorldProviderRoss128b.java#L81-L87)), so it has the Overworld's biomes, oceans included. It overrides `populate` and drops its own ruins in it.
  - Ross128ba is `ChunkProviderRoss128ba extends ChunkProviderMoon` ([source](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bwcrossmod/galacticraft/planets/ross128ba/ChunkProviderRoss128ba.java)) with `WorldChunkManagerMoon`: one flat biome, no ocean.
- **Populate path: the standard path does NOT reach us, so these dimensions get their own hook.** Both providers are the world's chunk provider and `ChunkProviderServer.populate` does call `GameRegistry.generateWorld`, but Galacticraft's [`ChunkProviderServerMixin`](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/mixins/early/minecraft/ChunkProviderServerMixin.java) wraps that call and [`WorldUtil.otherModPreventGenerate`](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/util/WorldUtil.java#L1457-L1470) drops it on every Galacticraft world provider (the BartWorks providers extend `AbstractWorldProviderSpace`), except for a whitelist of GT ore, GalacticGreg, CoFH, Dense Ores, AE2 meteorites and Thaumcraft nodes, unless Galacticraft's `enableOtherModsFeatures` is on (the pack does not set it; the server log shows "Whitelisting GregTech oregen on planets"). The first headless run confirmed it: the dimensions load, the gate allows them, and the generator never ran. **Deviation from M8:** `Ross128ProviderHook` wraps the chunk provider of those two worlds when they load and runs `StructureGenerator` right after the provider's own `populate`, inside the same call for the same chunk, so the populate window and no-cascade rules are unchanged (the hook uses no mixin, as the mod has none). If `enableOtherModsFeatures` is on, Forge's own call for the same chunk is skipped. **ICBM I6 extension:** the same hook now also wraps every non-Overworld world where the silo complex is enabled (`StructureGenerator.complexWanted`), since Galacticraft drops the generator on every planet, not only Ross128. RWG-style chunk providers elsewhere are unaffected.
- **GregGuns does:** adds the two IDs, read from `Configuration.crossModInteractions` (`Ross128Dimensions`), to the dimension gate while `worldgen.structureRoss128` is true (default). If BartWorks has `Ross128Enabled` off, or its config cannot be read, nothing is added. The world type is checked as before (a derived world reports the Overworld's type).
- **Placement:** the same grid (the seed mixes in the dimension ID), populate window, land checks and village scan. The Aircraft Carrier and Submarine need ocean biomes and deep water: Ross128b has them, so they generate there where its oceans allow; Ross128ba has none, so its ship plans are always rejected.
- **Biomes:** placement uses per-structure *blacklists* of biome types (GGS-11-05), so the vanilla biomes of Ross128b and the moon's one biome pass every land structure without a list change. The Alien Bug Nest whitelist (sandy or wasteland) already matches Ross128b's deserts and badlands; Galacticraft does not register types for the moon biome, so the nest does not generate on Ross128ba unless an admin adds a type for it. Ross128b's mushroom and Thaumcraft biomes are re-skinned only in its chunk data, so the biome manager still reports them; mushroom islands are ocean-adjacent and rejected for land by the terrain check as they are in the Overworld.
- **Pack-side change:** none. (Setting Galacticraft's `enableOtherModsFeatures` would also work but lets every mod's generator run on every planet; the hook avoids that.)

### Roguelike-Dungeons and vanilla villages: no coordination possible

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/roguelike_dungeons/roguelike.cfg`: `dimensionWL` = `0`, `spawnFrequency` = `16`, `doNaturalSpawn` = `true`. `reference/gtnh/2.9.0-RC-2/.minecraft/config/VillageNames4/general.cfg` only names and signs vanilla villages.
- **Source:** [`Dungeon#canSpawnInChunk`](https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/dungeon/Dungeon.java#L112-L143): with frequency 16 the cell is 51 chunks wide (`max = 32*16/10`), with one dungeon per cell at a seeded offset. [`validLocation`](https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/dungeon/Dungeon.java#L165-L211) checks biome type and ground only, never other structures. RWG's [`func_147416_a`](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.2/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L775-L779) answers only for strongholds, so GregGuns cannot ask the chunk provider "is there a village here".
- **GregGuns does:** nothing about dungeons (underground with a small entrance; overlap is rare and harmless). For vanilla villages it skips a footprint that already contains village-style blocks (planks, cobblestone, glass panes, doors) that GregGuns did not place.
- **Pack-side change:** none.

## Requirements

- **GGS-11-07** The structure generator is registered once, with weight 4, and only when `SpawnStructures` is true.
- **GGS-11-08** No block, tile entity or entity write by a populate call lies outside the populate window of the chunk being populated.
- **GGS-11-09** A populate call never requests (`loadChunk`, `provideChunk`, `getChunkFromChunkCoords`) a chunk other than the one being populated.
- **GGS-11-10** Structure position, rotation and variant derive only from the world seed, the dimension and the grid cell. Two worlds with the same seed place the same structures.
- **GGS-11-11** No attempt is made within 8 chunks of chunk `(0, 0)`.
- **GGS-11-12** The dimension gate is the allow-list `structureDimensions`, default `[0]`. No code compares a dimension ID with a literal.
- **GGS-11-13** The terrain check runs on the rotated footprint. A ship needs water at its sampled columns, at least as deep as its hull.
- **GGS-11-14** A footprint that contains village-style blocks not placed by GregGuns is skipped.
- **GGS-11-12a** On Ross128b and Ross128ba the dimension gate is the BartWorks IDs plus `structureRoss128`; no code compares them with a literal.
- **GGS-11-15** Each structure has its own `enabled` flag, default `true`, except the Alien Bug Nest, which defaults to `false` (0005: gated content).

### Generator wiring (M8 G9)

- **Code:** `StructureGenerator` (the `IWorldGenerator`), `StructurePopulator` (the pure pipeline), `StructureCatalog` (size classes, pools), `StructureHooks` (spawner and flag no-ops for G11 and G10). Registered in `WorldgenModule.postInit` after the layouts load, only when `generatorWanted()`.
- **Order per populate:** gate on dimension and world type, `plansForWindow`, `StructureState.decideOnce` (biome, then land or water check on the rotated footprint from this window's columns, then the village scan), flatten to the recorded Y, stamp at anchor Y = recorded Y + 1 (the layout's `ground` layer sits on top of the ground), then marks. All inside a `PopulateGuard` scope and a `ClippingSink`.
- **Mark tags:** `chest.vanilla` (the layout places a vanilla chest; the generator fills it now), `chest.weapon` and `chest.reinforced` (the layout places the GregGuns chest block; the generator attaches a `LootPending`, filled on first access), `spawner*` and `flag` (hooks). Chest category is `weaponchest_t<ceiling>` or `reinforcedchest_t<ceiling>` (Q11), seeded by `StructurePlan.chestSeed(index)` where the index is the chest's place among the layout's chest marks.
- **Deviation:** a window that sees only a sliver of a multi-window footprint decides from that sliver (after the coarse sample grid finds nothing it samples every column it has), so a slope visible only to other windows is not seen (PLAN-M8 Q3, accepted). Pool weights are equal per structure.

- **RWG worlds do get structures (checked in the pack, 2026-10-20).** The pack's RWG world type goes through the normal `ChunkProviderServer.populate`, which calls `GameRegistry.generateWorld`, so `StructureGenerator` runs there. `worldgen.no_cascade` reported 0 populated chunks in the pack only because its far-away squares are repeatable and a reused test world had already saved them: loaded from disk, they are never populated again. `ScenarioRegions.allocate` now also skips squares whose chunks are saved on disk (`AnvilChunkLoader.chunkExists`). After a restart on a world that already ran the scenario it fails without that check and passes with it, stamping structures on fresh RWG terrain.

## Tests

- GGS-11-12a: `WorldgenGateTest`; scenarios `worldgen.ross128b_structures` and `worldgen.ross128ba_structures` (gate, generation through the real populate path, no cascading, land and water checks).
- GGS-11-07, 12, 15: unit tests of the config and the registration call.
- GGS-11-08, 09: a unit test that places each structure into a recording world stub and asserts that every write is inside the window and that no foreign chunk is requested.
- GGS-11-10, 11: unit tests (same seed twice; first 1000 grid cells).
- GGS-11-13, 14: unit tests with synthetic height maps and block grids.
- M8 exit gate: a dev instance with `logCascadingWorldgen` = `true`; fly 5000 blocks in a new RWG world; the log has no cascade line naming `gregguns`.
- Manual: with GT ores on, dig around a bunker and check that its walls and chests are intact (first open question).

## Open questions

- **Answered by the G9 headless run (PLAN-M8 Q4):** GT's ore pass runs after ours and DOES replace a structure's `minecraft:stone` (a stamped slab came back as a GT ore block with huge metadata). Structure layouts must not use raw stone; use bricks, Chisel or concrete palette keys. Original question: does GT's ore pass replace only natural stone, or also a structure's stone-type blocks? If it replaces them, structure walls must use blocks GT ignores.
- A multi-chunk structure needs one ground height. The default above is the owner-column height. The alternative is one Y per structure derived from the seed, which fails on RWG's hills. Needs a design record before M8 starts if the default proves too bumpy.
- Villages cannot be detected through the chunk provider (RWG answers only for strongholds). Is the block-scan skip (GGS-11-14) enough, or do we drop the check?
