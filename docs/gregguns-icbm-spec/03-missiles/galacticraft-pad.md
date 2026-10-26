# The Galacticraft pad

- **ICBM baseline:** none (ICBM launches from its own launcher blocks)
- **Design records:** [0018 §9](../../design/0018-icbm-program.md)
- **Decisions:** [D32](../DECISIONS.md), [D19](../DECISIONS.md), [D46](../DECISIONS.md)
- **Pack mods:** Galacticraft (`GalacticraftCore`, `3.4.34-GTNH`)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

A GT New Horizons rocket removes the pad it launches from. Our missile does not: it is not a rocket and never touches the pad.

## Integration points

### Galacticraft: `IDockable` without `EntityAutoRocket`

- **Source:** [`IDockable`](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/api/entity/IDockable.java), [`TileEntityLandingPad`](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/tile/TileEntityLandingPad.java) (`updateEntity`, `tryDockEntity`, `addFuel`), [`EntityAutoRocket#L719-L738`](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/api/prefab/entity/EntityAutoRocket.java#L719-L738) (the only place the pad is removed, behind `EventLandingPadRemoval`).
- **How the pad finds a missile:** every tick the pad's centre tile looks for any `IFuelable` entity in the column one block high above its centre, and calls `setPad` on an `IDockable` one. `EntityMissile` implements `IDockable` (with `IFuelable` and `ICargoEntity`) through `@Optional.Interface`, so the interface is stripped when Galacticraft is absent, and `setPad`, `isDockValid`, `addFuel` and the rest are `@Optional.Method`. `isDockValid` is true only while the missile is on the pad.
- **Fuel path:** the fuel loader (and the pad's own `addFuel`) forwards generic fuel to the docked entity's `addFuel`, which fills the tank. The loader decides *whether* to pour by asking `RocketFuels.isCorrectFuel(entity, tankFluid)`, see [fuel.md](fuel.md). `removeFuel` returns null: a missile's tank cannot be drained.
- **`onPadDestroyed`:** if the pad is broken under an unlaunched missile, the missile comes apart into items rather than hanging in the air.
- **GregGuns does:** never posts or relies on `EventLandingPadRemoval` itself; and **denies** it for any pad with a missile in its 3x3 area (`MissileGc.PadGuard`, an event handler registered only after the module gate and a Galacticraft presence check), so another mod's rocket logic cannot remove a pad that holds our missile.
- **Pack-side change:** none.

### Isolation

Galacticraft types appear only in `MissileGc` (fuel lists, pad lookup, the guard) and in the `@Optional` members of `EntityMissile`; both are reached only behind `modules.icbm` being active and `IcbmAvailability.galacticraftPresent()`. Without Galacticraft the module turns itself off (D9), the content still registers, and `PadMissiles` still works with a missile standing on any solid block (the scenarios fuel it directly).

## Requirements

- **GGS-ICBM-03-20** A launch never removes, replaces or damages the pad, its nine cells, or an attached fuel loader.
- **GGS-ICBM-03-21** The pad docks an armed missile standing on its centre and forwards fuel to it.
- **GGS-ICBM-03-22** `EventLandingPadRemoval` is denied for a pad holding a missile and untouched otherwise.
- **GGS-ICBM-03-23** With Galacticraft absent no Galacticraft class is loaded by the missile content.

## Tests

Scenario `icbm.pad.survives` (builds a real 3x3 pad and a fuel loader, arms, docks, fuels through the pad, posts the removal event, launches, and checks every tick of the flight and the impact), and the full suite with `--no-galacticraft` (D46), where it skips and the rest runs.

## Open questions

None.
