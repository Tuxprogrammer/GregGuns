# Flight

- **ICBM baseline:** [flight-logic](../../icbm-spec/03-missiles/flight-logic.md) (`path.arc` is `MIT-portable`; GregGuns rebuilds the arc from the description instead of porting it), [targeting-and-tracking](../../icbm-spec/03-missiles/targeting-and-tracking.md)
- **Design records:** [0018 §8](../../design/0018-icbm-program.md)
- **Decisions:** [D11](../DECISIONS.md), [D20](../DECISIONS.md#d20-in-detail), [D24](../DECISIONS.md), [D25](../DECISIONS.md#d25-in-detail), [D26](../DECISIONS.md)
- **Pack mods:** none
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Bug avoided |
|---|---|---|
| The arc ignores the target height | The path is a parabola over the straight line from start to the target *point*, height included | ICBM-03-18 |
| Flat distance 0 divides by zero; under 1 dives | At least one tick, no division by the distance | ICBM-03-17 |
| No target means the arc heads for the world origin | `launch` needs a finite target; a non-finite one is `BAD_TARGET` | ICBM-03-19, -25 |
| Motion integrated tick by tick, so a reload drifts | The position is a **closed-form function of the flight tick**, so a reload resumes exactly | ICBM-03-24, -42 |
| Simulated while its chunk is unloaded, with a separate tracker | A real entity for the whole flight that keeps its own two chunks loaded (D25); no tracker | ICBM-03-22, -29, -30, -31 |
| Ticks and fuse in several places | Impact is the only fuse (D26) | ICBM-03-28 |

## Phases

| Phase | What happens | Ends |
|---|---|---|
| `PAD` | Standing armed on the pad or ground; can be fueled | `launch` succeeds |
| `WARMUP` | The launch sequence on the pad (D88): a lead of the silo's door ticks (0 on a pad), then the countdown (`icbm.countdownTicks`, 200), with launch smoke; abortable back to `PAD` until it ends. The missile stays put | at ignition, when lead plus countdown has run |
| `CLIMB` | Straight up with acceleration 0.08 blocks per tick per tick | 12 blocks climbed |
| `ARC` | Along the arc to the target, then down | impact, shot down, or timeout |

## Launch checks (`launch`, in order)

1. Already flying: `BUSY`. 2. Tank not full: `NOT_FUELED`. 3. Flat distance under 20: `TOO_CLOSE`; over the range: `OUT_OF_RANGE`; not finite: `BAD_TARGET`. 4. Missiles in flight at or over `icbm.maxMissilesInFlight` (default 20; counted over the loaded entities of every world): `OVER_CAP`. 5. Any block with a collision box in the **climb column** (the missile's footprint, from the block above the pad to the top of the body at the end of the 12-block climb): `BLOCKED` (ICBM-04-26). The arc itself still ray casts, so terrain ahead of the missile ends it as before.

Then the target is **scattered** ([guidance.md](guidance.md)) and the missile takes its chunk ticket (`NO_TICKET` if Forge refuses, see [chunk-loading.md](chunk-loading.md)). The target chunk is **loaded once at launch**, with no ticket, and the ground height read there; the arc then aims at the real surface, whether or not a player was ever near it. The launch point is recorded.

## The arc

`ArcPath(start, target, speed)`: with `flat` the horizontal distance and `f = n / T` for flight tick `n`,

```
T      = max(1, ceil(flat / speed))
apex   = min(300, 20 + 0.3 * flat)          blocks above the chord
x(n)   = sx + dx * f
z(n)   = sz + dz * f
y(n)   = sy + dy * f + 4 * apex * f * (1 - f)
```

It starts at the end of the climb, reaches the target column and height at `n = T`, and **keeps descending** past `T`, so a missile that arrives above the ground still hits it. Each tick the entity sets its position from `n` and its motion to the step, which is what the client extrapolates.

## Impact

Each tick a ray from the old position to the new one is cast through the blocks (so a fast missile cannot skip a wall). The first solid block it hits is the impact; the blast starts at the hit point through `IcbmBlasts.detonate` with the warhead's explosive and fill, the fuel bonus ([fuel.md](fuel.md)) and the owner as the source, and the entity and its tickets are removed. The blast engine decides everything else (claims, terrain switches, queue limits).

Two safety nets: past `T + icbm.missileTimeoutTicks` (default 600), or below y = 0, the missile detonates where it is.

## Requirements

- **GGS-ICBM-03-50** The path starts at its start, reaches the target point at tick `T`, and never produces NaN for any distance, including 0.
- **GGS-ICBM-03-51** A launch that passes all checks leaves the pad exactly as the phases say; every refusal leaves the missile untouched on the pad.
- **GGS-ICBM-03-52** Impact is the only fuse: a missile does not detonate on the pad, in the climb, or by proximity.
- **GGS-ICBM-03-53** At most `icbm.maxMissilesInFlight` missiles fly at once.
- **GGS-ICBM-03-55** A solid block in the climb column refuses the launch with `BLOCKED` and leaves the missile on the pad.
- **GGS-ICBM-03-56** The target height is read from the target chunk even when it was not loaded at launch.
- **GGS-ICBM-03-54** A launched T1 and T2 detonates on its aimed target and breaks terrain where the tier's terrain switch allows.

## Tests

`ArcPathTest`, `MissileLimitsTest`; scenarios `icbm.missile.launch_rules` (includes the `BLOCKED` roof, the `NOT_ARMED` stack and the `NO_TICKET` refusal), `icbm.missile.target_height`, `icbm.missile.launch_detonate_t1`, `icbm.missile.launch_detonate_t2`, `icbm.missile.shot_down`.

## Open questions

- Terminal guidance (refining the height in flight) is not needed: the height is read at launch, and the arc keeps descending until it hits something.
