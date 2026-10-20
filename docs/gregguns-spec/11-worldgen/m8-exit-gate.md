# M8 exit gate audit

Evidence for the three M8 exit-gate boxes in [ROADMAP.md](../../ROADMAP.md#m8--worldgen-and-structures), plus the per-structure checks the plan adds. Scenario names and what each asserts are in [docs/testing/README.md](../../testing/README.md). Unit tests are under `com.gregguns.worldgen`. Scenarios ran headless with `GG_MODULES=guns,armor,machines,utility,npcs,worldgen` on 2026-10-19; the unit suite ran 2196 tests with 0 failures.

Status words: PASS means the named test or scenario ran green. Pending means it does not exist yet or needs a client.

## No cascading worldgen (WGEN-04)

| Rule | Evidence | Status |
|---|---|---|
| Every shipped layout, at all four rotations and at three anchors, stamps with every write inside its window, no rejected write, and no foreign chunk request (GGS-11-08, GGS-11-09) | `CascadeHarnessTest.everyLayoutAtEveryRotationStaysInItsWindowsAndAddsUpToOneStamp` | PASS |
| The union of all windows a footprint overlaps equals one full stamp: each cell once, each mark once | same test (expected set computed independently from the layout and `Rotations`) | PASS |
| Window order does not change the result | `CascadeHarnessTest.windowOrderDoesNotChangeTheResult` | PASS |
| The whole populate pipeline (biome, terrain check, flatten, stamp) keeps writes and terrain reads inside the window, per layout | `CascadeHarnessTest.everyLayoutThroughThePopulatePipelineStaysInItsWindows` | PASS |
| A new layout file is covered with no edit | the harness lists `assets/gregguns/structures/*.txt` (so `military_base` and `alien_bug_nest` join automatically) | PASS |
| One rotation convention for plans and stamper | `CascadeHarnessTest.planToWorldAgreesWithRotations`, `planToWorldLandsOnTheColumnTheStamperWrote` (`StructurePlan.toWorld` now delegates to `Rotations`) | PASS |
| Headless: 40x40 fresh chunks far from spawn, every shipped structure on a forced 2-chunk grid, `PopulateGuard` records zero chunk loads, structures were stamped, and the loaded-chunk set equals a structure-less control square | scenario `worldgen.no_cascade` (about 30 s for both squares) | PASS |
| Pack evidence: ArchaicFix `logCascadingWorldgen=true` shows no line naming `gregguns` | batched client session on a pack that carries ArchaicFix | Pending (optional) |

Notes on the scenario. A world cannot un-generate terrain, so the control is a second fresh square compared by chunk offsets from its own corner, not the same square twice. The forced spacing lives in the generator override (`StructureGenerator.setOverride`), which the scenario clears in a `finally`; no config value is touched, so nothing needs restoring. The control uses an override with empty pools, so the real configured generator cannot stamp into it.

## Structures respect the dimension whitelist and GTNH's world types

| Rule | Evidence | Status |
|---|---|---|
| Dimensions 0, -1, 1, 7, 180, 27, 28: only the listed ones pass; no literal in code (GGS-11-12) | `WorldgenGateTest.defaultDimensionListAllowsOnlyTheOverworld`, `dimensionListIsTheOnlyAuthority` | PASS |
| World types `default`, `RWG`, `largeBiomes`, `amplified` allowed; `flat` and unknown out | `WorldgenGateTest.defaultWorldTypeListMatchesTheDocumentedFour`, `flatAndUnknownWorldTypesAreOut`, `worldTypeMatchIgnoresCaseAndBlanks`, `placementNeedsBothLists` | PASS |
| A non-listed dimension or world type makes the real generator write nothing; restored lists place structures | scenario `worldgen.dimension_gate` | PASS |
| Config defaults | scenario `worldgen.config_defaults` | PASS |
| Plan records survive a save | scenario `worldgen.state_persists` | PASS |
| Real RWG world-type name against `WorldTypeRealistic` | checked when G1 was written; a live RWG world is part of the client session | Pending |

## Loot is tier-appropriate for where the structure spawns

| Rule | Evidence | Status |
|---|---|---|
| Seven categories, each at or below its declared ceiling; material lines are GT parts | `LootCategoriesTest` (`sevenCategoriesWithTheSpecCeilings`, `everyLineIsAtOrBelowItsCeiling`, `materialsAreGtPartsNotRawMetals`) | PASS |
| 1000 chests per category at richness 0, 1, 2 stay under the ceiling; an item lying about its tier is caught | `LootCategoriesTest.thousandChestsStayUnderTheCeilingAtEveryRichness`, `aLineAboveTheCeilingIsCaught`, `anItemLyingAboutItsTierIsCaught`; scenarios `worldgen.loot_categories`, `worldgen.loot_tier_ceiling` | PASS |
| Chests fill lazily, once, from a per-chest seed, on any access and on break | `LazyChestTest`; scenarios `worldgen.lazy_chest_fill`, `worldgen.lazy_chest_break` | PASS |
| A placed structure's chests carry the category for the structure's ceiling | `StructurePopulatorTest.chestSeedsDifferPerChestAndUseTheWeaponCategory` | PASS |
| Weapon and reinforced chest marks end up as the GregGuns chest block with an order, not a vanilla chest | fixed in `StructureGenerator` (this branch); exercised by `worldgen.no_cascade` and the per-structure scenarios | PASS (no dedicated assertion yet) |
| A chest placed by the real generator is opened and each stack checked against the 0001 table | scenario `worldgen.loot_ceiling` | PASS |

## Per-structure scenarios

Each `worldgen.<id>_places` scenario (fixed seed and cell: footprint block counts, chest count and category, spawner or flag count, writes in window, same seed twice gives the same result) ran green: all seven structure scenarios passed, plus `worldgen.loot_ceiling`.

| Structure | Layout | Rotation and footprint unit test | Scenario | Status |
|---|---|---|---|---|
| Small Factory | shipped | `StructureLayoutsS1Test`, `CascadeHarnessTest` | `worldgen.factory_small_places` | PASS |
| Train Station | shipped | same | `worldgen.train_station_places` | PASS |
| Gas Station | shipped | same | `worldgen.gas_station_places` | PASS |
| Medium House | shipped | `StructureLayoutsS2Test`, `CascadeHarnessTest` | `worldgen.house_medium_places` | PASS |
| Big Bunker | shipped | same | `worldgen.big_bunker_places` | PASS |
| Aircraft Carrier | shipped | `StructureLayoutsS3Test`, `CascadeHarnessTest` | `worldgen.aircraft_carrier_places` | PASS |
| Submarine | shipped | same | `worldgen.submarine_places` | PASS |
| Military Base | shipped (9 parts) | `CampPartLayoutsTest`, `CascadeHarnessTest` | `worldgen.military_base_places`, `worldgen.carrier_flag`, `worldgen.camp_flag_init` | PASS |
| Alien Bug Nest (default off) | not yet | pending | `worldgen.alien_bug_nest_places` | Pending |


## Beta review gate

Client checks still owed for M8. None can be judged headless.

- [ ] Screenshots of every shipped structure from outside and inside (one batched client session after S2 and S4), each at two rotations: shape, doors and ladders facing the right way, no floating or buried parts, water structures sitting at the waterline.
- [ ] Chisel palette: with Chisel present, each palette key resolves to the intended variant and reads well; with Chisel absent, the vanilla fallbacks look acceptable (GGS-11-25 to 27).
- [ ] GT ore pass over structures (Q4, answered: GT's ore pass replaces `minecraft:stone`). Confirm in game that ore veins appear inside structure stone, and decide whether the layouts need non-stone walls. Palette keys avoid raw stone and cobblestone until this is checked.
- [ ] Weapon and reinforced chests in structures show the GregGuns chest model, face the right way, and fill when opened.
- [ ] Optional evidence: a pack that carries ArchaicFix with `logCascadingWorldgen=true`, flying or teleporting across a few thousand blocks of fresh terrain, with no log line naming `gregguns`.

## Open items found while building the harness

- Layouts mark `chest.weapon` and `chest.reinforced` on vanilla `minecraft:chest` blocks, while the generator attached a `TileGregChest` assuming a GregGuns chest block. That crashed the server in `TileEntityChest.checkForAdjacentChests` on the first tick after generation. Fixed by making the generator place the GregGuns chest block with the layout's facing before attaching the order. The layout spec should say chest marks of those kinds are placed over a vanilla chest block.

Fixed on `m8/polish`: `worldgen.generator_test_layout` now compares only the cells the layout writes (it was flaky because neighboring-chunk trees grew into the hut's box); plan keys now include the structure id (NBT version 2); camp chest tags map to loot categories (`ChestTags`, with a unit test over every layout file).

## Known limits

- A road-tail-only window populated before the footprint's own windows skips its tail blocks (it waits for a decision record, so those few road blocks are never written).
- A sliver window that decides first samples only a narrow strip of the footprint (Q3), so its verdict can differ from a full-footprint sample.
- `modules.worldgen` stays default false until the gate is closed (Q10).

## Still to come

- G11 hole spawners (Zombie Hole, Bug Hole): wait on the Infernal hook.
- The Alien Bug Nest (S5, `worldgen.alien_bug_nest_places`).
- Decorative blocks and textures (Q9): layouts use palette fallbacks until they land.
