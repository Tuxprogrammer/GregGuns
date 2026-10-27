# Radar

- **Techguns baseline:** none (ICBM module)
- **ICBM baseline:** [radar-station.md](../../icbm-spec/05-detection-defense/radar-station.md)
- **Design records:** [0018](../../design/0018-icbm-program.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205), StructureLib, ModularUI2
- **Roadmap:** [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense)

Status: the contacts section below is final (the API other code builds on). The rest is written in the radar commit.

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
