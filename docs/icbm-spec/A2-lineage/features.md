# Features of the older lineages that 1.12 dropped or changed

Compact sheets. "1.7" is ICBM-Classic `prod/1.7` @ `198999ed` (2.16.4, 1.7.10, no license: described only, nothing is portable). "1.12" is `prod/1.12` @ `9c1d985a`. Class paths are relative to `icbm/classic/` in the 1.7 tree. Each sheet gives what it did, the numbers, and how 1.12 differs. Bug IDs refer to [index.md](index.md).

## Camouflage block (`icbmCCamouflage`, `content/blocks/TileCamouflage`)

- **Did.** A tile block that renders as another full block. Right-click with a block item while you are the owner (or no owner is set) to copy that block and its damage value; the block must be a normal cube with render type 0 or 31 and not the camouflage block itself. A wrench toggles one face of "see-through" per click on that face (bitmask, one bit per direction, shown with glass or a vine block plus an inside render); a sneaking wrench click toggles whether the block has collision.
- **Numbers.** Recipe: 12 camouflage blocks from 4 wool and 4 vines in a ring (`WGW / G G / WGW`). Material grass, no ticking. NBT: `blockToMimic` (registry name), `metaToMimic`, `renderSides` (byte), `isSold` (sic, the collision flag). Config group `camo_block`: `use_glass_render` false, `vine_block` default `minecraft:vines` (not a real id, so it falls back to vines; ICBM-A2-13).
- **Quirks.** Color multiplier reads the wrong axis (ICBM-A2-11). Setting the mimic writes the mimic's damage value into the block's own world metadata (ICBM-A2-12).
- **1.12.** Gone. No camouflage block, no mimic logic, no config group.

## Signal disrupter (`icbmCSignalDisrupter`, `content/items/ItemSignalDisrupter`)

- **Did.** Intended as a handheld that jams ICBM radio frequencies. In the 1.7 source it is inert: the item stores a broadcast frequency (float, set over a packet), shows it in the tooltip, and opens GUI id 10002 on right-click, but both GUI factory methods return null and nothing else in the mod reads the item. Its `onUpdate` drain is commented out (ICBM-A2-08).
- **Numbers.** Stack size 1. Recipe: `WWW / SCS / SSS` with wire, iron ingot, tier-1 circuit. Uses the electrical-item base (energy handling is stubbed).
- **1.12.** Gone. The radio bus (`01-core`) has real channels and messages but no jammer item.

## S-Mine (proximity mine, explosive meta 14, `ExSMine` + `BlastMine`)

- **Did.** An explosive block shaped as a flat plate (collision and bounds 0.2 high, a custom model). Tier 2; no grenade, minecart or missile form. Recipe: Fragmentation over Attractive over Repulsive (vertical `S / L / R`). Fuse 20 ticks. The blast (size 5) first creates a vanilla 1.5 explosion with fire, then hurls the exploder entity upward (motion Y 0.7 to 1.1, small random sideways) and tumbles it. Once it has been in the air 40 ticks or has landed, it emits shrapnel (the Shrapnel blast) every second tick, and ends 20 ticks later (about 60 ticks total). Blast energy value 8000; the blast moves with the entity.
- **Gap.** Despite the name, no entity-contact or proximity trigger was found in the 1.7 explosive block code; the only S-Mine special cases are model, bounds and collision (ICBM-A2-14, unverified beyond that search). It behaves like any other placed explosive: redstone or ignition.
- **1.12.** Registration slot 14 is an empty "S-Mine" comment in `content/reg/ExplosiveInit`. Dropped.

## Rejuvenation (regen) blast (explosive meta 12, `ExRejuvenation` + `BlastRegen`)

- **Did.** Tier 2, recipe 4 clocks + 4 iron blocks + 1 diamond block (`ICI / CDC / ICI`). Despite the size 16 passed in, the blast ignores radius: it takes the single **chunk** at the blast position, asks the world generator for a fresh copy of that chunk, writes every block and metadata value of the fresh copy over the live column (all heights, flag 3), marks terrain unpopulated and re-runs population. Server side only, called directly (not through the threaded explosion controller). Errors are logged as "ICBM Rejuvenation Failed!".
- **Quirks.** Player buildings in the chunk are erased; entities and tile entities are not cleared (ICBM-A2-15). Energy value 0.
- **1.12.** Gone (slot 12 is a comment only). Reproducing it needs a decision: a chunk-regen weapon is a griefing tool on a shared pack server.

## Reinforced rail (`icbmCRail`, `content/blocks/BlockReinforcedRail`, also called the combat rail)

- **Did.** A vanilla rail subclass: hardness 10, resistance 10, metal step sound, max cart speed 0.6 (vanilla 0.4). No other behavior.
- **Numbers.** Recipe: 16 rails from 8 iron ingots around 1 concrete (meta 0) (`C C / CIC / C C`).
- **1.12.** Gone; the Bomb Cart (`02-explosives`) remains and rides vanilla rails.

## Proximity detector (`blockProximityDetector`)

- **Did.** Nothing. The block field is declared (`ICBMClassic.java:105`) but never assigned in this tree, yet a recipe (`ICBMClassic.java:258`: 8 iron ingots around a Tracker) is still registered with that null output (ICBM-A2-10).
- **1.12.** Absent.

## Nightmare missile (explosive id 29, `ExNightmare` + `BlastNightmare`)

- **Did.** A holiday novelty. Tier 1, no grenade form, no crafting recipe. Blast size 50. Through delayed spawns around the impact point it releases `size/10 + rand(size/10)` hostile bats (5 to 9 at size 50) carrying a small explosive, and the same count of zombies, each at a random delay up to about `size` seconds (bats) or `2 * size` seconds (zombies). Several TODOs in the source list unfinished ideas (screams, bone torches, monsters walking to players).
- **Gating.** The missile appears in the item list only if `holiday.force_enable_nightmare_missile` is true (default: true only in a dev environment) or the date is Halloween; the check is in the item's sub-item listing, not in launch code.
- **1.12.** Dropped.

## Missile coordinator (`icbmCMissileCoordinator`, `content/machines/coordinator/`)

- **Did.** Intended as a mission planner. The block opens a 2-slot inventory GUI that accepts only world-position items, plays the `interface` sound, and does nothing else. The client GUI factory returns null, so the screen cannot render (ICBM-A2-16). Hardness and resistance 10, not opaque, does not tick.
- **Numbers.** Recipe: tier-2 circuit, 3 primary plates, 2 remote detonators (`R R / SCS / SSS`).
- **1.12.** Gone.

## Tracker (`icbmCTracker`, `content/items/ItemTracker`)

- **Did.** Left-clicking an entity with it stores that entity's numeric id in NBT key `trackingEntity` and prints a "now tracking" message with the entity's name; the tooltip shows the target name; a custom icon texture points toward the target (client). The item targets its crafter at creation. The energy draw (1 per tick) is commented out, so it never discharges, and the one-second update body does nothing useful (ICBM-A2-09).
- **Numbers.** Stack size 1. Recipe: compass over a battery and a tier-1 circuit with 4 iron ingots (` Z / SBS / SCS`). A "ban_Tracker" world flag was planned (commented out).
- **1.12.** Gone. (Do not confuse it with `content/missile/tracker/` in 1.12, which tracks missiles across loaded chunks.)

## Sulfur and saltpeter chain (`icbmCSulfurDust`, `ItemSulfurDust`)

- **Did.** One item with two damage values: 0 sulfur dust (oredict `dustSulfur`), 1 saltpeter (oredict `dustSaltpeter`). Sugar cane (reeds) smelts into 4 saltpeter at 0 XP. Gunpowder: sulfur + saltpeter + (coal, charcoal, charcoal dust or coal dust) gives 2 gunpowder (four shapeless recipes). The sulfur source was a sulfur ore smelt (4 dust at 0.8 XP), but that line is commented out, so sulfur has no source of its own in this tree (ICBM-A2-17).
- **1.12.** Split into separate plain items: `sulfurDust`, `saltpeter`, and a new intermediate `saltpeter_ball`. Same oredict names. GTNH note: these are GT dust names, so a GregGuns rebuild uses GT dust and skips the items.

## EMP tower and radar: 1.7 against 1.12

| Aspect | 1.7 | 1.12 |
|---|---|---|
| EMP radius | default 60, max 150 | see `05-detection-defense/emp-tower.md` (extenders change the max) |
| EMP shot cost | 50,000 for any radius under 150 and 300,000 at exactly 150 (integer division, ICBM-A2-06); buffer 600,000 | `range^2 * 100` FE per shot plus `range * 10` per tick upkeep |
| Cooldown | 120 ticks | see the 1.12 sheet |
| Trigger | fires whenever it is ready (cooldown over and charged) and indirectly powered, so it repeats every 120 ticks while the signal is held | radio triggers plus redstone |
| Blast origin | tower center, 1.2 blocks up | EMP action centered on the tower |
| Modes | 0 all, 1 entities, 2 blocks (GUI) | modes in the GUI |
| Radar power | `ENERGY_USAGE` 20 per tick, buffer 20,000 | FE, drained twice per tick (ICBM-05-03) |
| Radar range | `MAX_DETECTION_RANGE` 500, alarm default 100, safety default 50 | config max; safety range stored but unused (ICBM-05-04) |
| Threat test | missile is within the alarm radius **and** its target is within the safety radius | any missile merely getting closer |
| Computer API | DataSystem `incomingMissileData`, `detectedEntityData`, `icbm.empTower.*` | `IMachineInfo` only |

## Per-tier launcher ranges (`Settings`, `TileLauncherBase`)

- **Did.** The launcher base has a tier (stored 0 to 2, displayed 1 to 3). Range is the horizontal (X-Z) distance, configured per tier in group `launcher`: `tier_1_range` 1000, `tier_2_range` 3000, `tier_3_range` 10000 (min 0, max int). A target closer than 10 blocks is "too close" and farther than the range is "too far"; the Launcher Screen and the fire check both use this. The cruise launcher (1.6.4 source) has no range cap but a minimum of 8 blocks.
- **1.12.** One range for all: `launcher.max_range` 10000 in `config/machines/ConfigLauncher`, plus `min_inaccuracy_range` 2 m, `scaled_inaccuracy_range` 10 m, `scaled_inaccuracy_per_launcher` 1 m, `power_capacity` 1200, `power_per_launch` 1200. Per-tier ranges are replaced by inaccuracy that grows with distance. A GTNH rebuild that wants tiers must add them back.
