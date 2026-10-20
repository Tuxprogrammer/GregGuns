# A2-lineage: what 1.6.4 and 1.7.10 had that 1.12 dropped or changed

## Overview

ICBM-Classic is a three-generation line: Calclavia's ICBM for 1.6.4, the BuiltBroken `prod/1.7` port for 1.7.10 (2.16.4), and `prod/1.12`. The GregGuns rebuild targets 1.7.10, so the 1.7 tree is the closest ancestor and the place to look for features the 1.12 spec does not cover. This appendix records those features and the 1.6.4 computer integration. Licenses: 1.12 is MIT (portable under permalinks elsewhere in this spec); the 1.7 tree has **no license** and 1.6.4 is LGPL, so everything here is described in words and nothing is marked portable.

## Contents

- [features.md](features.md): one sheet per feature (camouflage, signal disrupter, S-Mine, rejuvenation, reinforced rail, proximity detector, Nightmare, missile coordinator, tracker, sulfur and saltpeter chain), the EMP tower and radar differences, and per-tier launcher ranges.
- [computer-integration.md](computer-integration.md): every method of the 1.6.4 `ICBMLauncher` and `ICBMRadar` peripherals and the `emptower` OpenComputers component, plus the 1.7 DataSystem and the 1.12 `IMachineInfo`.

## Feature table

"?" means not examined in this appendix.

| Feature | 1.6.4 | 1.7.10 | 1.12 | Note |
|---|---|---|---|---|
| Camouflage block | ? | yes: mimic, per-face see-through, collision toggle | no | |
| Signal disrupter | ? | item present, no effect | no | Dead in 1.7 |
| S-Mine | ? | yes (flat block, shrapnel hop); no proximity trigger found | no | Slot 14 empty in 1.12 |
| Rejuvenation | ? | yes, regenerates the whole chunk | no | Slot 12 empty in 1.12 |
| Reinforced rail | ? | yes, speed 0.6, hardness 10 | no | |
| Proximity detector | ? | declared, never registered | no | |
| Nightmare missile | ? | holiday-gated novelty | no | |
| Missile coordinator | ? | block, no behavior | no | |
| Tracker | ? | item, tracks one entity, no drain | no | |
| Sulfur and saltpeter | ? | one item, two metas; no sulfur source | separate items, adds saltpeter ball | |
| Per-tier launcher range | ? | 1000 / 3000 / 10000 config | one `max_range` 10000 | |
| EMP tower | yes, OC component `emptower` | yes, GUI + DataSystem | yes, extenders, FE | Cost scaling bug in 1.6.4 and 1.7 |
| Radar station | yes, CC `ICBMRadar` | yes, DataSystem | yes | Safety range used only in 1.6.4/1.7 |
| Launcher computer API | yes, CC `ICBMLauncher` | DataSystem shared methods | `IMachineInfo` | |
| Cruise launcher | yes (min distance 8) | yes | yes | |

## Key facts

- The 1.6.4 computer API is poll-only and tiny: 7 launcher methods, 2 radar methods, 13 EMP-tower callbacks. Nothing queues events.
- Four features in the 1.7 tree are visibly unfinished: signal disrupter, missile coordinator, proximity detector and the S-Mine's proximity trigger.
- The rejuvenation blast is a chunk-reset weapon, not a heal.
- The EMP tower cost scaling is broken by integer division in both 1.6.4 and 1.7, so the radius barely changes the cost.

## Related areas

| Area | Why |
|---|---|
| `02-explosives` | The 1.12 blast list the retired explosives (S-Mine, Rejuvenation, Nightmare) were removed from |
| `04-launch-control` | The 1.12 launcher with its single range config |
| `05-detection-defense` | The 1.12 radar and EMP tower these sheets compare with |
| `06-blocks-items` | Remaining blocks and items |

## Coverage

Read: Calclavia 1.6.4 `TileLauncherPrefab.java` (peripheral parts and fields), `TileRadarStation.java` (peripheral parts and constants), `TileEMPTower.java` (callbacks, capacity, fire), `TileCruiseLauncher.java` (`canLaunch`, `launch`, `isTooClose`); ICBM-Classic 1.7 `TileCamouflage`, `ItemSignalDisrupter`, `BlockReinforcedRail`, `ExSMine`, `BlastMine`, `BlastRegen`, `ExRejuvenation`, `ExNightmare`, `BlastNightmare`, `ItemSulfurDust`, `ItemTracker`, `TileMissileCoordinator`, `DataSystemICBMLoader`, `DSEmpTower`, `Settings` in full, and the relevant parts of `TileEMPTower`, `TileRadarStation`, `TileLauncherBase`, `BlockExplosive`, `ICBMClassic.java`. 1.12: `ExplosiveInit` (slot comments), `ItemReg`, `ConfigLauncher`. The 1.7 and computer recons supplied the rest.

## Known limits

- No code was run. 1.6.4 behavior of the non-computer features was not examined (hence the "?" cells).
- The S-Mine trigger absence is a negative search (unverified). The 1.6.4 silo launcher screen inheriting `ICBMLauncher` is assumed.
- The 1.7 radar and EMP tower logic was read only for constants, packets and computer methods.
- Line numbers for the camouflage, signal disrupter, tracker and coordinator bugs are given where grepped; others cite the file.

## Suspected bugs

"Logic" = we could inherit it in a rebuild. "1.6.4-only" or "1.7-only" = does not matter to a rebuild that follows 1.12 and GT.

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-A2-01 | Peripheral `equals` recurses forever (1.6.4-only) | 1.6.4 `TileLauncherPrefab.java:156`, `TileRadarStation.java:548` | `equals(IPeripheral)` calls itself, so any peripheral comparison overflows the stack | crash |
| ICBM-A2-02 | `setTarget` argument handling (1.6.4-only) | 1.6.4 `TileLauncherPrefab.java:78` | Fewer than three args throws a raw index error; any nil arg silently skips the call | gameplay |
| ICBM-A2-03 | EMP capacity ignores radius (logic) | 1.6.4 `TileEMPTower.java:131` | `empRadius / MAX_RADIUS` is integer division: 0 below 150, 1 at 150; the `max` with 1e9 always wins | gameplay |
| ICBM-A2-04 | Static `@Callback` (1.6.4-only, unverified) | 1.6.4 `TileEMPTower.java:277` | `getMaxRadius` is static but tagged as a callback; OC may not expose it | cosmetic |
| ICBM-A2-05 | Radar data methods return after one entity (1.7-only) | 1.7 `TileRadarStation.java:99,122` | `return` sits inside the loop, so `size` can be 5 while only entry 0 is present | gameplay |
| ICBM-A2-06 | EMP cost scaling collapses (logic) | 1.7 `TileEMPTower.java:166` | Same integer division: cost is 50,000 under radius 150 and 300,000 at 150 | gameplay |
| ICBM-A2-07 | EMP DataSystem gaps (1.7-only) | 1.7 `DSEmpTower.java` | `getEmpMaxCooldown` registered twice; no mode or radius setters unlike 1.6.4 | cosmetic |
| ICBM-A2-08 | Signal disrupter does nothing (1.7-only) | 1.7 `ItemSignalDisrupter.java` | Stores a frequency but nothing reads it; both GUI factories return null | gameplay |
| ICBM-A2-09 | Tracker keeps only a raw entity id (logic) | 1.7 `ItemTracker.java` | The id is not stable across restarts; the drain and update body are commented out | cosmetic |
| ICBM-A2-10 | Proximity detector recipe has a null output (1.7-only) | 1.7 `ICBMClassic.java:105,258` | Field never assigned, recipe registered anyway | cosmetic |
| ICBM-A2-11 | Camouflage color reads the wrong axis (logic) | 1.7 `TileCamouflage.java:446` | `colorMultiplier(world, x, y, x)` passes X for Z | visual |
| ICBM-A2-12 | Camouflage meta handling (logic) | 1.7 `TileCamouflage.java:151,353` | Compares the held damage value to the camo block's own metadata; setting the mimic overwrites that metadata with the mimic's | gameplay |
| ICBM-A2-13 | Default vine block id invalid (1.7-only) | 1.7 `Settings.java` (`CAMO_BLOCK_TO_RENDER_AS_VINES`) | `minecraft:vines` is not a block id (it is `vine`), so the lookup always falls back | cosmetic |
| ICBM-A2-14 | S-Mine has no contact trigger (logic, unverified) | 1.7 `ExSMine.java`, `BlockExplosive.java:96,126,229` | Only shape and render special cases exist | gameplay |
| ICBM-A2-15 | Rejuvenation resets the whole chunk (logic) | 1.7 `BlastRegen.java` | Radius ignored; overwrites player blocks; entities and tile entities stay | gameplay |
| ICBM-A2-16 | Missile coordinator has no function (1.7-only) | 1.7 `TileMissileCoordinator.java:105` | Client GUI is null; the container only holds position items | gameplay |
| ICBM-A2-17 | Sulfur has no source (1.7-only) | 1.7 `ICBMClassic.java:229` | The ore smelt is commented out; gunpowder recipes need the dust | gameplay |
