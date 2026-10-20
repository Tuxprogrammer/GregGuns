# 05-detection-defense

## Overview

How an ICBM base sees incoming missiles and fights back: the **radar station** and its per-dimension radar map; the **EMP tower** and the **EMP action** with its receiver capability (what a pulse does to entities, items, missiles and machines); and the four handheld tools (**radar gun**, **remote detonator**, **laser detonator**, **defuser**). The radio bus, SAM flight, blasts and launchers they talk to are other areas and are only linked. Source: ICBM-Classic `prod/1.12` @ `9c1d985a`; cite classes relative to `src/main/java/icbm/classic/`.

## Contents

- [radar-station.md](radar-station.md): the radar block, the radar map, the scan (MIT-portable), threat classification, redstone and SAM trigger, GUI and states.
- [emp-tower.md](emp-tower.md): the tower and extenders, energy and cooldown, radio triggers, the EMP action, per-target effects and the missile kill chain.
- [handheld-tools.md](handheld-tools.md): compact entity sheets for the radar gun, remote detonator, laser detonator and defuser.

## Key facts

- The radar only tracks missiles that carry an explosive and are in flight for more than 1 tick. It treats a missile as "incoming" if it is simply getting closer; the per-tile **trigger range is stored and shown but never used**.
- The detection area is a chunk-aligned square reaching 16-32 blocks beyond the configured range, and ignores Y. The per-entity range test is dead code.
- The radar block drains twice its stated power per tick (its state query pays power).
- One EMP pulse is a cube of 8 * size^3 blocks, skewed one block toward negative. At the default range of 60 the tower checks about 1.7 million positions in one tick.
- EMP vs missiles with default config: 50% destroyed (with a small blast), 40% guidance fried (it falls), 1% fires its warhead, 9% unaffected.
- Firing the tower costs `range^2 * 100` FE, plus continuous upkeep of `range * 10` FE per tick. The buffer holds only the shot plus 100 ticks of upkeep.
- The defuser is free to use and never runs out of charge, because the electrical base class is a stub.
- Capabilities, the registry events, blockstates, JSON models, packet codex classes and Forge Energy are *1.12-only mechanisms*. The sheets describe the behavior they produce.

## Related areas

| Area | Why |
|---|---|
| `01-core` | Radio bus (channels, `IncomingMissileMessage`, trigger messages, `FakeRadioSender`), energy buffer and `use_energy`, config groups, the EMP receiver API, the GPS capability |
| `02-explosives` | EMP blast types and EMP explosive/cart/bomblet that reuse the EMP action; what `onDefuse` does; explosive entities |
| `03-missiles` | Missile entities, ticks in air, SAM, flight logic (the "dead" flight logic the EMP applies), what the missile kill chain interacts with |
| `04-launch-control` | Launchers that react to the radar broadcast and to the laser target; the launcher screen used in the radar recipe |
| `06-blocks-items` | Recipes, battery, base block class (hardness) |
| `07-visuals-audio` | Full texture, model and sound catalog (this area lists its own at catalog depth) |

## Coverage

Source files read in full: 37 of 37 in scope.
- `content/blocks/radarstation/` 12 of 12 (`BlockRadarStation`, `EnumRadarState`, `PropertyRadarState`, `RadarObjectType`, `RadioRadar`, `TileRadarStation`, `data/` 3, `gui/` 3).
- `content/blocks/emptower/` 9 of 9 (incl. `gui/` 2).
- `content/actions/emp/` 3 of 3; `lib/capability/emp/` 6 of 6; `lib/radar/` 3 of 3.
- `content/items/` `ItemRadarGun`, `ItemRemoteDetonator`, `ItemLaserDetonator`, `ItemDefuser`: 4 of 4.

Also read, to resolve references: `config/blast/types/ConfigEmp`, `config/machines/ConfigEmpTower`, `config/machines/ConfigRadar` (all in full); `content/missile/entity/CapabilityEmpMissile` (in full, belongs to `03-missiles` but defines the missile kill chain); `prefab/item/ItemICBMElectrical` (in full); `EnergyBuffer.consumePower` (only the one method, for the simulate flag); `ItemRadio` (only the channel NBT key); `CapabilityMissile`/`EntityMissile` (only the registration and removal lines); `EntityExplosive` (the EMP capability hook).

Assets at catalog depth: GUI textures `gui_radar.png`, `gui_emp_tower.png` (256 x 256, regions given by component coordinates in the sheets, not measured against the PNG); block textures `screen/radar*.png` (4 states, 16 x 16), `emp_tower/base.png`, `coil.png`, `electric.png` (16 x 64, animated); item textures (4, 16 x 16); block models and blockstates (`radar_screen`, `emp_tower*`, `emptower.json`, `radarstation.json`); sounds `emp`, `machinehum`; lang keys in all sheets; recipes (7). Not covered: sound durations and sample rates, pixel palettes, the `electric.png.mcmeta` frame timing, `ItemRadio` tooltip and channel-edit logic.

## Known limits

- No code was run. All timings, probabilities and counts come from reading the source. The percentages in the missile chain are products of the default config values.
- Whether a missile that has its warhead triggered by EMP is removed afterwards lives in `03-missiles` and was not checked (ICBM-05-14).
- `ItemRadio` (shared by both detonators) is outside this area; the channel NBT key `hz` was confirmed, its tooltip logic was not read.
- The interface contracts `IEMPReceiver`, `IncomingMissileMessage` and `RadioTile` are `01-core` and are described here only by use.
- Sounds and textures could not be seen or heard; descriptions are inferred from names and use.
- The recipes for the radar station, tower and tools use GT-compatible oredict names that the pack changes (`docs/gregguns-icbm-spec`).

## Suspected bugs

"Logic" means we could inherit it in a rebuild. "1.12-only" means it does not matter to a 1.7.10 rebuild.

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-05-01 | Radar range filter is dead code (logic) | `lib/radar/RadarMap.java:284` | The condition `exact \|\| exact && cube.isWithin(...)` is true whenever `exact` is true, and every caller passes true, so the per-entity box test never runs. Detection reaches a whole chunk ring beyond the configured range, as a square, at any height | gameplay |
| ICBM-05-02 | Chunk-move check always sees a change (logic) | `lib/radar/RadarMap.java:72` | Compares the stored `ChunkPos` to a freshly built one by reference, so it is always "different". Every 20 ticks every tracked entity is removed and re-added. Output is still right, but it wastes work and the add/remove guard can drop entries mid-update | gameplay |
| ICBM-05-03 | Radar station pays power twice per tick (logic) | `content/blocks/radarstation/TileRadarStation.java:149,200,315` | `consumePower(cost, false)` means "simulate = false", i.e. a real extraction. The tick pays once at line 149, then `getRadarState()` (line 315, called from line 200) pays again. Real drain is 2 * `POWER_COST`; with 1,000 to 1,999 FE left it scans and then shows OFF. The next line's `consumePower(cost, true)` is the simulate call, the reverse of what the names suggest | gameplay |
| ICBM-05-04 | Trigger range is never used (logic) | `content/blocks/radarstation/TileRadarStation.java:50,70,292-307` | `safetyRadius` is saved, synced and drawn but `isMissileGoingToHit` only checks that the distance is shrinking. The tooltip claims it defines the threat area | gameplay |
| ICBM-05-05 | Threat sort key is wrong (logic) | `content/blocks/radarstation/TileRadarStation.java:267-273` | `abs` is applied to the block coordinate only, and the three signed deltas are summed. Targets in opposite directions cancel, so the "nearest" threat sent to SAMs (list element 0) can be any of them | gameplay |
| ICBM-05-06 | Range inputs are unbounded; tiny ranges hang the GUI (logic and 1.12-only) | `TileRadarStation.java` (range setters and packets), `gui/RadarComponent.java:42,70`, `data/RadarRenderData.java:48` | The client can send any int. Detection range below 3 gives a 0 grid spacing and a division by zero; the loop count then becomes huge and the client can freeze. Range 0 divides dot positions by zero. Only the upper bound is applied, and only at scan time | crash |
| ICBM-05-07 | Radar gun block-use stores the wrong Y (logic) | `content/items/ItemRadarGun.java:156` | Position built as `(x + hitX, y + hitX, z + hitZ)`; should add `hitY`. Using the gun on a block stores a Y that is off by the hit fraction mix-up (up to 1 block) | gameplay |
| ICBM-05-08 | Defuser burns charge and blocks attacks on any entity (logic) | `content/items/ItemDefuser.java:41-84` | After the three type checks, any other entity falls through to the discharge and `return true`, so the swing is consumed (nothing is attacked) with no feedback | gameplay |
| ICBM-05-09 | Defuser energy is a stub; low-power message key missing (logic) | `prefab/item/ItemICBMElectrical.java`, `ItemDefuser.java:88` | `getEnergy` returns `Integer.MAX_VALUE` and `discharge` is empty, so the 2,000 cost is never charged and the "no power" branch cannot run. Its key `message.defuser.nopower` is not in `en_us.lang` | gameplay |
| ICBM-05-10 | EMP cube is skewed by one block (logic) | `content/actions/emp/ActionEmpArea.java:94-96` | Loops run from `-size` to `size - 1`; the entity box is `+/- size`. So tiles are hit on one side less than entities. It is also a cube, not a sphere, and loads 8 * size^3 positions in a single tick | gameplay |
| ICBM-05-11 | EMP tower range is not re-clamped (logic) | `content/blocks/emptower/TileEMPTower.java:288,276-281,394-404` | `setRange` clamps only the top, and only at the moment of setting. Breaking an extender, or loading NBT, leaves `range` above the new max, and `fire` uses it. A negative range is accepted and makes the upkeep cost negative (effect on the buffer unverified) | gameplay |
| ICBM-05-12 | Server trusts client target positions (logic) | `content/items/ItemLaserDetonator.java:91-123`, `ItemRadarGun.java:166-177` | No range, line-of-sight or loaded-chunk check on the received position. A modified client can designate or store any coordinate in the world. The launcher exclusion is also client-only. Laser server side reads only the main-hand item | gameplay |
| ICBM-05-13 | EMP smoke particles are spawned on the server (logic) | `content/actions/emp/ActionEmpArea.java:110-112` | `World.spawnParticle` on the server world does nothing visible, so the "large smoke along the tower's Y layer" effect is never seen | visual |
| ICBM-05-14 | Triggered EMP missile may survive (unverified) | `content/missile/entity/CapabilityEmpMissile.java:47-59` | Branch 3 runs the missile's explosive or action but never kills the entity. Whether the explosive already ends the entity is in `03-missiles` and was not checked | gameplay |
| ICBM-05-15 | Laser cooldown is shared across all stacks (logic) | `content/items/ItemLaserDetonator.java:38-39,85-86` | The cooldown counter is a field on the single item instance and is decremented once per stack per tick, so carrying more than one detonator shortens the cooldown | cosmetic |

Minor, not given IDs: the EMP block swap skips the post event (`ActionEmpArea.java:126`); `tile.emptower.radio.success` is not in `en_us.lang`; the laser lang key `laserDetonator.info.info` looks like a typo; the radar gun tooltip says "targetted".
