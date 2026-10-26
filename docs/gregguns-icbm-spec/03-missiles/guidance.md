# Guidance

- **ICBM baseline:** [targeting-and-tracking](../../icbm-spec/03-missiles/targeting-and-tracking.md) (scatter, distance term)
- **Design records:** none
- **Decisions:** [D24](../DECISIONS.md)
- **Pack mods:** GregTech (the circuit tier of the guidance computer, written by the recipe track)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

There are no duds and no failure roll. Fins are structural: they are an ingredient of the body, not a state of the missile. The accuracy of a launch is one number, the per-axis half-width, drawn once at launch.

## The scatter

```
share     = max(0.1, 1 - 0.25 * guidance)         guidance = circuit tiers above the body's own (0 to 3)
halfWidth = baseScatter * share + 10 * (distance / range)^2
offset    = (u1 - u2) * halfWidth                 per axis, u1 and u2 uniform in [0, 1)
```

`offset` is triangular: most shots land near the aim point, and none farther than `halfWidth`. The distance term is the baseline's `10 * (d / range)^2`. `baseScatter` is 12 blocks for T1 and 8 for T2.

| Guidance | T1 half-width at zero range | T2 |
|---|---|---|
| +0 | 12.0 | 8.0 |
| +1 | 9.0 | 6.0 |
| +2 | 6.0 | 4.0 |
| +3 | 3.0 | 2.0 |

At full range the distance term adds 10 blocks to each.

## Requirements

- **GGS-ICBM-03-70** The half-width follows the formula for every guidance 0..3, clamps to 0..3, and never goes below 10% of the base.
- **GGS-ICBM-03-71** The offset is triangular, centered, bounded by the half-width, and has no failure case.

## Tests

`ScatterTest`; the launch scenarios use guidance 3 so the blast lands on the target platform.

## Open questions

None.
