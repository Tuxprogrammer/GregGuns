# Radar

- **Techguns baseline:** none (ICBM module)
- **ICBM baseline:** [radar-station.md](../../icbm-spec/05-detection-defense/radar-station.md)
- **Design records:** [0018](../../design/0018-icbm-program.md)
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

| Item | HV radar | EV radar | Radar Tower (hatch tier) |
|---|---|---|---|
| MTE ID | 20142 | 20143 | 20144 (IV hull) |
| Range, flat circle | 128 | 256 | IV 512, LuV 768, ZPM 1024, UV 1280, UHV and up 1536 |
| Power | quarter amp while scanning (HV 128 EU/t, EV 512 EU/t) | | quarter amp of the weakest hatch |
| Scan | every 5 ticks | | every 5 ticks, while the cycle runs |
| Protected radius | default 64, min 8, max the range; typed in the GUI | | default 64 (not yet settable: no screen; D94) |

Nothing below HV (D69). The tower needs an IV or better hatch; a lower one fails with a message and the tower stays off.

## Behavior

- Sweep (clean-room; D14 replaces ICBM-Classic's chunk query and distance-trend test, so nothing is ported): `Contacts.within(world, x, z, range)`, then `Sweep.of(...)` splits "detected" from "incoming" (impact inside the protected radius), nearest first.
- Outputs: redstone strength = incoming count (max 15) on all six sides, set only when it changes; single blocks set the GT active flag while a threat exists (a machine-state cover reads it); the tower's active flag is its own running state, so it drives redstone only.
- Link: right-click a radar or the tower controller with a data stick, or press Save link in a single block's GUI. The stick keeps `{dim, x, y, z}` (`RadarLink`).
- Single blocks work without any structure; the tower is a 3x3x7 Gantry Casing tower (`RadarTowerShape`) with a Gantry Beam mast, hatches (Energy, Maintenance) in the bottom ring.

## Requirements (continued)

- **GGI-05-05.** Range by tier follows the table; a radar below HV does not exist.
- **GGI-05-06.** A radar with no power reports OFF and clears its sweep.
- **GGI-05-07.** The protected radius is clamped to 8 to the range.
- **GGI-05-08.** Outputs change only on a transition.
- **GGI-05-09.** The tower's range comes from its weakest hatch.

## Tests

`RadarRulesTest`, `SamRulesTest` (unit); scenarios `icbm.defense.registered`, `.intercept`, `.not_incoming`, `.range_by_tier`, `.tower`, `.save_reload`.

## Open

- A GUI for the tower's protected radius; the radar casing art (meta 11) is not wired, so the tower uses Gantry Casing.
