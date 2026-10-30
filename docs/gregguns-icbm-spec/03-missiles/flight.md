# Flight

- **ICBM baseline:** [flight-logic](../../icbm-spec/03-missiles/flight-logic.md) (`path.arc` is `MIT-portable`; GregGuns rebuilds the arc from the description instead of porting it), [targeting-and-tracking](../../icbm-spec/03-missiles/targeting-and-tracking.md)
- **Design records:** [0018 §8](../../design/0018-icbm-program.md)
- **Decisions:** [D11](../DECISIONS.md), [D20](../DECISIONS.md#d20-in-detail), [D24](../DECISIONS.md), [D25](../DECISIONS.md#d25-in-detail), [D26](../DECISIONS.md), [D126](../DECISIONS.md), [D285](../DECISIONS.md)
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
| A short climb, then the arc from just above the launcher | A vertical climb to the build height, then the arc from that waypoint (D285), so nothing near the launcher is in the way | — |

## Phases

| Phase | What happens | Ends |
|---|---|---|
| `PAD` | Standing armed on the pad or ground; can be fueled | `launch` succeeds |
| `WARMUP` | The launch sequence on the pad (D88): a lead of the silo's door ticks (0 on a pad), then the countdown (`icbm.countdownTicks`, 200), with launch smoke; abortable back to `PAD` until it ends. The missile stays put | at ignition, when lead plus countdown has run |
| `CLIMB` | Straight up the launch point's column to the waypoint at the build height ([The climb](#the-climb)) | at the waypoint |
| `ARC` | Along the arc from the waypoint to the target, then down | impact, shot down, or timeout |

## Launch checks (`launch`, in order)

1. Already flying: `BUSY`. 2. Tank not full: `NOT_FUELED`. 3. Flat distance under 20: `TOO_CLOSE`; over the range: `OUT_OF_RANGE`; not finite: `BAD_TARGET`. 4. Missiles in flight at or over `icbm.maxMissilesInFlight` (default 20; counted over the loaded entities of every world): `OVER_CAP`. 5. Any block with a collision box in the **launch column** (the missile's footprint, from the block above the pad to the top of the body 12 blocks up, `FlightPath.MIN_CLIMB`): `BLOCKED` (ICBM-04-26). This is the roof right over a pad or a silo opening. Higher up the climb neither collides nor breaks anything (D285), so a launch does not need open sky all the way up. The arc still ray casts.

Then the target is **scattered** ([guidance.md](guidance.md)) and the missile takes its chunk ticket (`NO_TICKET` if Forge refuses, see [chunk-loading.md](chunk-loading.md)). The target chunk is **loaded once at launch**, with no ticket, and the ground height read there; the arc then aims at the real surface, whether or not a player was ever near it. The launch point is recorded.

## The climb

`FlightPath` (D285) is the whole flight as a function of the tick since ignition. The climb runs up the launch point's column (the LP, the missile's column center on the pad) and never moves sideways:

```
waypoint  = max(buildHeight, launchY + 12)     buildHeight = World.getHeight(), 256 in most dimensions
climbed(n) = 0.04 * n^2                         while n <= 37.5 (accelerating at 0.08 to 3 blocks per tick)
           = 56.25 + 3 * (n - 37.5)             after that
```

The climb ends on the first tick that reaches the waypoint; the missile stands there and turns onto the arc. A pad at y 64 climbs 192 blocks in 83 ticks (about 4 seconds); a silo pad deeper down takes a little longer. The waypoint is saved with the missile, so a reload in the climb resumes exactly.

- **No collision in the climb** (GGS-ICBM-03-52): the column right above the pad is checked at launch (`BLOCKED`); above it the missile passes whatever a player built later or the terrain holds, and breaks nothing. Its feet are at the build height when it turns, so no block in the world stands in the first steps of the arc.
- **The silo doors** open before ignition and close once the missile's bottom is 4 blocks above the rim (`icbm.doorCloseHeight`), as before: the climb is straight, so it is far above the panels when they swing.
- **Range and accuracy** are measured at the LP. The waypoint is straight above it, so the flat distance of the arc is the one the range check, the scatter (`siloScatterFactor`) and the fuel bonus (`siloRangeFactor`) used. The flight takes the climb's ticks longer.
- **Detection and EMP:** the climb is a flight phase like the arc: the radar sees it, a SAM can hit it, an EMP can destroy or fry it (a fried missile in the climb falls straight back down its column). The engine loop follows the entity the whole way.
- **Pads** climb the same way. The old 12-block climb turned a long pad shot onto an arc whose first slope is `4 * apex / flat` (0.4 for a 3,000-block T2 flight), so it flew within 20 blocks of the ground for its first 50 blocks and hit hills and trees near the pad.

## The arc

`ArcPath(start, target, speed)`: with `flat` the horizontal distance and `f = n / T` for flight tick `n`,

```
apex   = min(300, 20 + 0.3 * flat)          blocks above the chord
T      = max(1, ceil(flat / speed), ceil((4 * apex - dy) / 6))
x(n)   = sx + dx * f
z(n)   = sz + dz * f
y(n)   = sy + dy * f + 4 * apex * f * (1 - f)
```

It starts at the waypoint, reaches the target column and height at `n = T`, and **keeps descending** past `T`, so a missile that arrives above the ground still hits it. Each tick the entity sets its position from `n` and its motion to the step, which is what the client extrapolates.

The third term of `T` is the **dive cap** (`ArcPath.MAX_DIVE`, D285): the last step falls `(4 * apex - dy) / T` blocks, and from the build height a short flight would otherwise drop 150 to 250 blocks in a handful of ticks. A capped flight is slower sideways instead, so no missile ends its arc faster than about 6.2 blocks per tick, under the SAM's 9 (D126). Long flights are not affected: a 2,000-block flight still takes `flat / speed`.

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
- **GGS-ICBM-03-57** A launched missile climbs straight up its launch point's column to the build height before it moves sideways, and breaks no block on the way; from a silo under a roof and a chimney it breaks no block of either and still hits its target (D285).
- **GGS-ICBM-03-58** No arc dives faster than `ArcPath.MAX_DIVE` blocks per tick, whatever the distance and the drop from the build height (D126, D285).

## Tests

`ArcPathTest`, `FlightPathTest`, `MissileLimitsTest`, `SamRulesTest`; scenarios `icbm.missile.launch_rules` (includes the `BLOCKED` roof, the `NOT_ARMED` stack and the `NO_TICKET` refusal), `icbm.missile.target_height`, `icbm.missile.launch_detonate_t1`, `icbm.missile.launch_detonate_t2` (both check the straight climb to the build height), `icbm.missile.shot_down`, `icbm.silo.climb_clear`.

## Open questions

- Terminal guidance (refining the height in flight) is not needed: the height is read at launch, and the arc keeps descending until it hits something.
