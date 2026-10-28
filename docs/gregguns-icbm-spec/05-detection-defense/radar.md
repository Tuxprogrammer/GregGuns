# Radar

- **Techguns baseline:** none (ICBM module)
- **ICBM baseline:** [radar-station.md](../../icbm-spec/05-detection-defense/radar-station.md)
- **Design records:** [0018](../../design/0018-icbm-program.md); **decisions:** [D14, D125, D190 to D194](../DECISIONS.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205), StructureLib, ModularUI2
- **Roadmap:** [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense)


## Contacts (the tracker API)

Radars, the air-defense battery and the OpenComputers adapter all read the same server-side type, `com.gregguns.icbm.detect.Contact`, from `Contacts` (D91).

| Field | Meaning |
|---|---|
| `id()` | The missile's entity id; stable for the whole flight |
| `dimension()` | Dimension id |
| `x()`, `y()`, `z()` | Position now |
| `vx()`, `vy()`, `vz()` | Velocity, blocks per tick (last move) |
| `tier()` | Missile tier, 1 to 3 |
| `impactX()`, `impactZ()` | Predicted impact point (the aimed, scattered target; the arc is deterministic) |
| `incoming(cx, cz, r)` | True when the predicted impact point lies inside the circle, edge included (D14) |

Queries: `Contacts.all(world)`, `Contacts.within(world, x, z, range)` (nearest first), `Contacts.incoming(list, cx, cz, r)`, `Contacts.of(missile)`, `Contacts.missile(world, id)`. They read `world.loadedEntityList`, never chunks, on the server thread only.

### Requirements

- **GGI-05-01.** A missile is a contact only in the climb and arc phases. A missile on the pad or warming up is not (ICBM skipped the first tick of flight; ours skips the whole countdown, which the Launch Control already reports).
- **GGI-05-02.** Range is a flat circle (X and Z) with no height limit. ICBM-Classic's chunk-aligned square reached 16 to 32 blocks past the stated range (ICBM-05-01).
- **GGI-05-03.** "Incoming" is decided by the predicted impact point alone. A missile heading at the radar but landing elsewhere is not incoming; one passing the radar and landing inside the circle is (ICBM-05-04).
- **GGI-05-04.** Lists sort by flat distance to the radar, ties by id (ICBM-05-05).

### Tests

`ContactTest` (unit): GGI-05-02 to 05-04. GGI-05-01 is checked by the radar scenarios.

## Numbers

| Item | HV radar (controller 20142) | EV radar (controller 20143) | Radar Tower (hatch tier) |
|---|---|---|---|
| MTE ID | 20142 | 20143 | 20144 (IV hull) |
| Range, flat circle | 128 | 256 | IV 512, LuV 768, ZPM 1024, UV 1280, UHV and up 1536 |
| Power | quarter amp of the controller's tier while it runs (HV 128 EU/t, EV 512 EU/t) | | quarter amp of the weakest hatch |
| Scan | every 5 ticks | | every 5 ticks, while the cycle runs |
| Protected radius | default 64, min 8, max the range; typed in the GUI | | default 64; the tower's R panel sets it (D99, "Tower screen" below) |

Nothing below HV (D69). The tower needs an IV or better hatch, and a small radar a hatch of its controller's tier or better; a lower one fails with a message and the radar stays off.

## Behavior

- Sweep (clean-room; D14 replaces ICBM-Classic's chunk query and distance-trend test, so nothing is ported): `Contacts.within(world, x, z, range)`, then `Sweep.of(...)` splits "detected" from "incoming" (impact inside the protected radius), nearest first.
- Outputs: redstone strength = incoming count (max 15) on all six sides of the controller, set only when it changes. The small radar's and the tower's active flag is the multiblock's own running state, so both drive redstone only (D191).
- Link: right-click a radar or the tower controller with a data stick. The stick keeps `{dim, x, y, z}` (`RadarLink`).
- A radar controller with no structure does nothing (D190). The small radar is described in "The small radar" below; the tower is a 3x3x7 Radar Casing tower (`RadarTowerShape`, casing meta 11 of `gregguns:casings`) with a Gantry Beam mast, hatches (Energy, Maintenance) in the bottom ring.

## Requirements (continued)

- **GGI-05-05.** Range by tier follows the table; a radar below HV does not exist.
- **GGI-05-06.** A radar with no power reports OFF and clears its sweep.
- **GGI-05-07.** The protected radius is clamped to 8 to the range.
- **GGI-05-08.** Outputs change only on a transition.
- **GGI-05-09.** The tower's range comes from its weakest hatch.

## Tests

`RadarRulesTest`, `SamRulesTest`, `RadarShapeTest`, `RadarTowerShapeTest`, `DishSpinTest` (unit); scenarios `icbm.defense.registered`, `.intercept`, `.not_incoming`, `.range_by_tier`, `.tower`, `.tower_detects`, `.tower_save_reload`, `.save_reload`, `.radar_unformed`, `.radar_dish`, `.radar_dish_reload`; the two-client check `testing.icbm_mp_defense` (the radar's dish at two different angles on both clients, and a SAM in flight, via `mp_check.py`).

## Open

- Nothing blocking. The data stick only carries a radar's address (`RadarLink`), so it does not also carry a radius (D99): the radius is set in the controller's screen (the "R" panel), small radar and tower alike.

## Tower screen and casing (D99)

- GT's multiblock screen gains one "R" button (left of the power panel button) that opens a small radar panel: the protected radius field (digits only, clamped on the server to 8 up to the hatch range, or the IV range while no hatch is fitted), the range, and the detected and incoming counts. It is saved in the controller's NBT with the rest of the radar state.
- Radar Casing (meta 11, NTM-derived art) replaces Gantry Casing in the tower; the Gantry Beam mast stays. Recipe (D193): 6 Stainless Steel plate, programmed circuit 31, Assembler at HV (`TierEU.RECIPE_HV`), 10 s, makes 4. It recycles to 1.5 plates of Stainless Steel each.
- Recycling for the rest: the SAM returns 2 Stainless Steel plates, the radar gun 2, each machine its hull's 8 (D44).

## The small radar (D125, D190, D191)

The HV and EV radars (MTE ids 20142 and 20143) are the controllers of a small multiblock. A controller with no structure does nothing (no scan, no outputs, no dish). *(Design note, D190: these ids were I4's single blocks, so a world that holds one keeps its tile entity, which is simply an unformed controller until its structure is built.)*

- **Shape** (`RadarShape`, one StructureLib piece): a 3x3 base two layers high, with one Radar Casing as a pedestal in the middle of the top. The controller is in the middle of the front wall's upper layer, so its front faces out. 19 blocks with the controller; at least 14 of the 18 casing positions must be Radar Casing (meta 11 of `gregguns:casings`), and any casing position of the base may be a hatch instead.
- **Hatches:** one Energy Hatch and one Maintenance Hatch, like the tower. The Energy Hatch must be the controller's tier or better.
- **Range and power:** fixed by the controller's tier (table above), a quarter amp of that tier's voltage while it runs. The GT cycle is one second and repeats while it is paid for.
- **Everything else is the tower's:** contacts, the protected radius, links by data stick, redstone, covers and the OpenComputers component `gg_radar` all go through `RadarUnit` and `RadarView`. The GUI is GT's multiblock screen with the tower's "R" panel.
- **Recipes:** the controllers take a hull, 2 sensors, an emitter and 2 circuits, in the Assembler at HV and EV (D69). The casing is in "Tower screen and casing" above.

| Requirement | |
|---|---|
| **GGI-05-20** | A controller with no formed structure is not online, reports OFF with an empty sweep, drives no redstone and has no dish. |
| **GGI-05-21** | The small radar forms from `RadarShape` with an Energy Hatch and a Maintenance Hatch; a hatch below the controller's tier leaves it off with a message. |
| **GGI-05-22** | The small radar's range is its controller's tier (HV 128, EV 256), formed or not; `range()` reads 0 while it is not running. |

## The dish (D125, D192)

- The controller (small radar or tower) spawns one `EntityRadarDish` (entity id 126) once formed, on the top plane of the structure and one block behind the controller, so it is in the middle of the footprint. The small radar's dish stands on the pedestal; the tower's on top of the mast, 1.5 times larger.
- It turns about the vertical axis at 6 degrees a tick (one turn in three seconds) while the radar is formed and running, and holds its angle when it is not.
- **Sync (GGS-01-11, no DataWatcher):** `PacketRadarDish` sends the spinning flag, the angle and the tick of the last change, and the scale, to the clients tracking the entity when they change and once a second. The angle is then a function of the game time (`DishSpin`), so there is no per-tick traffic and every client draws the same angle.
- **Lifecycle:** the entity is never saved. The controller spawns it when it is formed, looks for a live one that already serves it before spawning (so a reloaded tile never doubles it), and removes it when the structure is gone (after a short grace while a freshly loaded controller has not yet checked) or when the controller block is broken. The entity removes itself within a second if its controller is gone or unformed, so a lost controller chunk cannot leave one standing.
- **Art (D47, D194):** HBM's NTM radar dish model, LGPL-3.0, converted to strict Forge OBJ.

| Requirement | |
|---|---|
| **GGI-05-23** | A formed radar has exactly one dish; an unformed one has none. |
| **GGI-05-24** | The dish turns while the radar runs and is still without power or when unformed. |
| **GGI-05-25** | A tile reload leaves one dish; a dish lost with its chunk is replaced by exactly one. |
| **GGI-05-26** | Breaking a casing or the controller removes the dish. |
| **GGI-05-27** | Two clients draw the same dish angle at the same game time. |

GGI-05-23 to 05-26 are the scenarios `icbm.defense.radar_unformed`, `.radar_dish` and `.radar_dish_reload`; GGI-05-27 is `DishSpinTest` (the server's numbers restored on a client give the same angle) and the two-client check.
