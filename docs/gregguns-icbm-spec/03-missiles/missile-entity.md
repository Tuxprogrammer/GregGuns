# The missile entity

- **ICBM baseline:** [missile-entity](../../icbm-spec/03-missiles/missile-entity.md), [projectile-base](../../icbm-spec/03-missiles/projectile-base.md)
- **Design records:** [0018 §8, §9](../../design/0018-icbm-program.md)
- **Decisions:** [D25](../DECISIONS.md#d25-in-detail), [D26](../DECISIONS.md), [D31](../DECISIONS.md#d31-in-detail), [D32](../DECISIONS.md)
- **Pack mods:** Forge (entity registry, `RenderingRegistry`), Galacticraft (`IDockable`, behind `@Optional`)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Bug avoided |
|---|---|---|
| Health set by a constructor that does not know the explosive yet | Health is a constant of the tier; the tier is the entity class | ICBM-03-01 |
| Health clamped to the wrong maximum on load because the explosive loads later | The tier is fixed by the class, so the clamp is right in any key order | ICBM-03-04 |
| Lethal damage from arrows, melee and the like left the missile alive | Any damage that takes a flying missile's health to zero detonates it where it is | ICBM-03-03 |
| Warmup timer not saved | Phase and phase tick are saved | ICBM-03-23 |
| Shooter lost on reload | Owner UUID and name are saved | ICBM-03-43 |
| EMP-triggered warhead left the missile flying | No EMP trigger in I2; I4 will end the entity on any trigger | ICBM-03-05 |
| Air-burst fuse fired on the pad | Impact is the only fuse (D26); nothing triggers on the pad | ICBM-03-28 |

## The class

`EntityMissile` (abstract, both sides) with `EntityMissileT1` and `EntityMissileT2`. Two classes because Galacticraft's `RocketFuels` keys by class (see [fuel.md](fuel.md)). Registered as `gregguns.missile_t1` and `gregguns.missile_t2`, entity ids 120 and 121, tracking range 160, update every tick with velocity. It extends `Entity` directly, with no collision (`noClip`) and no gravity of its own: its position is computed ([flight.md](flight.md)).

Size 0.8 wide and body height + 1 tall, so the pad's centre column finds it and it can be hit.

## State

| Field | Saved | Notes |
|---|---|---|
| phase (`PAD`, `WARMUP`, `CLIMB`, `ARC`), phase tick | yes | the position is a function of these |
| fuel (mB) | yes, clamped to the tank | |
| guidance, explosive, fill, casing | yes | read back with safe defaults (`condensed`, fill at least 1) |
| owner UUID and name | yes | the blast is credited to this player |
| launch point, arc start, aimed target, planned distance | yes | the arc is rebuilt from these |
| health | yes, clamped to the tier | |

Chunk tickets are not saved; see [chunk-loading.md](chunk-loading.md).

## Client sync

No DataWatcher slot (GGS-01-11). `PacketMissileState` (wire id appended to the clientbound list: entity id, phase, casing, fuel, warhead blast, warm-up clock) is sent to the clients tracking the entity on every change and about once a second, so late trackers catch up. The client uses the phase for the engine flames and the casing and the warhead blast (one byte: 0 for none, else the `WarheadBlast` ordinal plus one) for the warhead model and skin, so every T3 type draws its own skin on a missile. The position and velocity come from the entity tracker; between updates the client carries the last velocity forward and derives the heading from it.

## Rendering

`RenderMissile` draws the body and the warhead models in one pass, turned about the body's middle so the nose points along the heading. The same models as the placed stack, so arming shows no jump (D31).

## Damage and interaction

- **On the pad:** immune, except a sneaking player's hit, which disarms it. Right-click shows the status line (`gregguns.icbm.missile.status`: tier, fuel, guidance); sneak with an empty hand disarms.
- **In flight:** takes damage; at zero health the warhead detonates at the missile's position (the shot-down case, which I4's defenses will use). A missile cannot be picked up or mounted.

## Requirements

- **GGS-ICBM-03-40** The class fixes the tier, health and fuel list; both tiers can be told apart by class.
- **GGS-ICBM-03-41** The saved entity restores phase, tick, fuel, warhead, guidance, owner and the arc inputs; an oversized saved health is clamped to the tier.
- **GGS-ICBM-03-42** Lethal damage to a flying missile detonates it and releases its tickets; damage never harms a missile on the pad except a sneaking player's disarm.
- **GGS-ICBM-03-43** The entity references no DataWatcher.

## Tests

Scenarios `icbm.missile.save_round_trip`, `icbm.missile.shot_down`, `icbm.missile.stack_and_arm`; `NoDataWatcherTest` (existing).

## Open questions

None.
