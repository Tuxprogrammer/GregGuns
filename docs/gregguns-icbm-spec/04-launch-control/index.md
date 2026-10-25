# 04-launch-control (GregGuns ICBM)

## Overview

How a base fires: the launch-control block, targeting, the silo multiblock, and the outputs every machine exposes. T1-T2 launch from the Galacticraft pad; T3 and up from the GregGuns silo (a GT multiblock under [0011](../../design/0011-machine-policy.md), EU powered). ICBM baseline: [icbm-spec 04-launch-control](../../icbm-spec/04-launch-control/index.md).

## Contents

All sheets are planned.

- launch-control-block.md (planned): the block beside the pad, targeting by coordinates, data stick, ModularUI2 GUI (D13).
- silo-multiblock.md (planned): shape, hatches (fluid input, item input buses), EU use, mating body and warhead (D8, D22, D27).
- launch-sequence.md (planned): checks, minimum range, scatter, firing by redstone or GT machine-controller cover, in-flight cap (D11, D24).
- outputs-and-components.md (planned): redstone, covers, data sticks, OC components; links to [golden-dome.md](../01-core/golden-dome.md) (D13, D18).

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Launcher base and frame | Galacticraft pad for T1-T2; GregGuns silo multiblock for T3+ | Galacticraft, GT5-Unofficial | [D8](../DECISIONS.md#d8-in-detail) |
| Launcher screen | Launch-control block with ModularUI2 GUI | ModularUI2 | [D13](../DECISIONS.md) |
| Target copy | Data stick | GT5-Unofficial | [D13](../DECISIONS.md) |
| Fire inputs | Redstone, GT machine-controller cover, OC | GT, OpenComputers 1.12.64-GTNH | [D13](../DECISIONS.md), [golden-dome.md](../01-core/golden-dome.md) |
| Energy (1,200 FE per shot) | EU | GT5-Unofficial | [0003](../../design/0003-energy-model.md) |
| Silo fuel and missile feed | GT fluid input hatch; GT input bus | GT5-Unofficial | [D22, D27](../DECISIONS.md) |
| Inaccuracy | Guidance tier sets scatter plus ICBM's distance term | GT circuits | [D24](../DECISIONS.md) |
| Cruise launcher | Not mentioned in the decisions: to settle in the I2 sheet | to settle | [D10](../DECISIONS.md#d10-in-detail) |
| Missile count limit | Config cap | none | [D11](../DECISIONS.md) |

## Decisions carried

D8, D11, D13, D18, D22, D24, D27.

## Roadmap

[I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2) (launch-control block beside the pad), [I3](../../ROADMAP-ICBM.md#i3--the-silo-and-heavy-warheads-t3-nuclear) (silo), [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense) (outputs on every machine).

## Pack-side changes

None yet.

## Related areas

[03-missiles](../03-missiles/index.md), [05-detection-defense](../05-detection-defense/index.md), [01-core](../01-core/index.md) (OC components).

## Open questions

- Whether a cruise launcher exists in GregGuns: to settle in the I2 sheet.
- Silo footprint and hatch positions: to settle in the I3 silo sheet.
