# Fuel

- **ICBM baseline:** none (ICBM launchers spend energy)
- **Design records:** [0018 §8](../../design/0018-icbm-program.md)
- **Decisions:** [D19](../DECISIONS.md), [D20](../DECISIONS.md#d20-in-detail), [D22](../DECISIONS.md), [fuel context](../DECISIONS.md#fuel-context-d19-d20-d22)
- **Pack mods:** Galacticraft `RocketFuels` and fuel loader, GT++ rocket fuels (in GregTech 5.09.54)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2), silo fuel in I3

## The tank

- A fixed amount per tier times `icbm.missileFuelFactor`: **T1 2,000 mB, T2 4,000 mB.** It is shown in the body tooltip, in the launch GUI, and by the entity's status line.
- The tank starts empty. It fills only while the missile is on the pad. A launch needs a **full** tank (`NOT_FUELED` otherwise).
- It cannot be drained (`removeFuel` gives nothing).

## What the pad and loader accept (D19)

The Galacticraft fuel loader asks `RocketFuels.isCorrectFuel(fuelable, fluid)`, which looks the fluid name up in a list keyed by the **entity class**. So each tier has its own entity class (`EntityMissileT1`, `EntityMissileT2`) and registers its list at init:

| Tier | Accepted fluids ("this fuel or better") |
|---|---|
| T1 (HV) | `nitrofuel` (Cetane-Boosted Diesel), `rocketfuelmixb` (RP-1), `rocketfuelmixd`, `rocketfuelmixc`, `rocketfuelmixa` |
| T2 (EV-IV) | `rocketfuelmixd` (Dense Hydrazine), `rocketfuelmixc` (Purple), `rocketfuelmixa` (Green) |

Plain Galacticraft `fuel` is not in either list (it is for GC's own rockets). The fluid the loader then pours into the entity is generic, so `addFuel` accepts what the loader offers; the list is the only gate, as for a GC rocket. A pad forwarding fuel directly (a pipe into the pad) goes through `addFuel` with no list check, exactly as it does for a GC rocket; the GC fuel loader is the supported way.

## The unburned-fuel bonus (D20)

- The tank is full at launch whatever the target is.
- The share burned is the flat distance flown over the tier's range; `f = 1 - distance / maxRange`.
- At impact the blast radius gets `radius * fuelBonusMax * f` added (`fuelBonusMax` default 0.25), through the `bonusRadius` argument of `IcbmBlasts.detonate`. A target at the minimum range gets almost the full bonus; one at the maximum range gets none; the minimum range keeps a point-blank shot from farming it.
- A missile shot down in the air detonates with the same bonus rule against the planned distance (it keeps its fuel).
- `PadMissiles.expectedRadius` gives the GUI the figure for a target, so the bonus is never hidden.

## Requirements

- **GGS-ICBM-03-30** Tank sizes are 2,000 and 4,000 mB times the config factor; a pour never exceeds the space.
- **GGS-ICBM-03-31** `RocketFuels` lists exist per entity class, exactly as the table, and reject water and plain GC fuel.
- **GGS-ICBM-03-32** The fuel bonus is `radius * bonusMax * (1 - d / range)`, never negative, zero at range, off when `fuelBonusMax` is 0.
- **GGS-ICBM-03-33** An unlaunched missile can be filled; a flying one cannot.

## Tests

`FuelMathTest`, `MissileTierTest`; scenarios `icbm.missile.fuel_rules` (tank rules, the lists through `RocketFuels.isCorrectFuel`, the loader-style `addFuel`) and `icbm.pad.survives` (fuel through the real pad).

## Open questions

None. (The silo's GT fluid input hatch uses the same lists: I3.)
