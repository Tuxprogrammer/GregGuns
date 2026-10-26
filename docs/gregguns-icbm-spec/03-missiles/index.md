# 03-missiles (GregGuns ICBM)

## Overview

Missile bodies, the stack-on-pad build, fuel, flight and tracking. Missiles ride on the Galacticraft pad (T1-T2) or the GregGuns silo (T3 and up). ICBM baseline: [icbm-spec 03-missiles](../../icbm-spec/03-missiles/index.md).

## Contents

- [build-and-recipes.md](build-and-recipes.md): chip, schematic, NASA Workbench page, Assembly Line recipes with research, guidance carry-through, cost (D8, D27).
- [missile-bodies.md](missile-bodies.md): the T1 and T2 bodies, their numbers, model, tooltip and recycling (D8, D24, D31).
- [stack-and-pad.md](stack-and-pad.md): placing the body, seating the warhead, arming into the entity, the pad API for launch control (D31).
- [galacticraft-pad.md](galacticraft-pad.md): `IDockable` without `EntityAutoRocket`, the removal guard, `icbm.pad.survives` (D32).
- [fuel.md](fuel.md): the tank, the `RocketFuels` lists, the unburned-fuel bonus (D19, D20).
- [missile-entity.md](missile-entity.md): the entity, its state, sync, rendering and damage.
- [flight.md](flight.md): phases, launch checks, the closed-form arc, impact fusing, the in-flight cap (D11, D26).
- [chunk-loading.md](chunk-loading.md): the two-chunk sliding window and when tickets are released (D25).
- [guidance.md](guidance.md): tiered scatter (D24).

Planned for later milestones:

- cluster-and-deferred.md: what is deferred (cargo, held-item missile) and whether clusters ship (D10).

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Missile crafting | Chip, schematic, NASA Workbench page; Assembly Line for automation | Galacticraft `SchematicRegistry`, GT Circuit Assembler, Laser Engraver, Assembly Line | [D8](../DECISIONS.md#d8-in-detail), [D27](../DECISIONS.md) |
| Missile on the pad | Body and warhead stacked by the player, arming fuses them | Galacticraft launch pad | [D31](../DECISIONS.md#d31-in-detail) |
| Pad removal on launch | Never; our missile does not extend `EntityAutoRocket` | Galacticraft 3.4.34-GTNH | [D32](../DECISIONS.md) |
| Fuel | Per-tier `RocketFuels` lists, fixed amount, bonus blast | Galacticraft fuel loader | [D19, D20](../DECISIONS.md#d20-in-detail) |
| Silo fueling and loading | GT fluid input hatch, GT input buses | GT5-Unofficial | [D22, D27](../DECISIONS.md) |
| Tier health, scatter | Circuit tier sets scatter; no duds; fins structural | GT circuits | [D24](../DECISIONS.md) |
| Flight and chunk tickets | Real entity, two-chunk sliding window, cap on missiles in flight | Forge chunk loading | [D25](../DECISIONS.md#d25-in-detail), [D11](../DECISIONS.md) |
| Fuses | Impact only at first | none | [D26](../DECISIONS.md) |
| Cluster, cargo, held-item, grenade, rocket launcher | Cargo and held-item deferred; grenade and launcher dropped; cluster to settle in the I2 sheet | none | [D10](../DECISIONS.md#d10-in-detail) |
| Missile cost | About one rocket launch of the tier | GC rockets | [D34](../DECISIONS.md) |

## Decisions carried

D8, D11, D19, D20, D22, D24, D25, D26, D27, D31, D32, D34 (cost).

## Roadmap

[I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2) (T1-T2, pad, fuel, flight), [I3](../../ROADMAP-ICBM.md#i3--the-silo-and-heavy-warheads-t3-nuclear) (T3 and the silo), [I5](../../ROADMAP-ICBM.md#i5--endgame-warheads-and-systems) (UV and UHV missiles).

## Pack-side changes

None yet. Fuel lists and the schematic page register through code; a pack change appears here if the pack's recipe or fuel config needs one.

## Related areas

[02-explosives](../02-explosives/index.md) (warheads), [04-launch-control](../04-launch-control/index.md) (silo and pad control), [05-detection-defense](../05-detection-defense/index.md) (what shoots missiles down), [07-visuals-audio](../07-visuals-audio/index.md) (body models).

## Open questions

- How charges are added to a warhead (one per craft or a GT machine step): to settle in the I2 build-and-recipes sheet (D23).
- Whether cluster missiles ship: to settle in the I2 sheet.
- ~~Per-tier fuel amounts~~: settled in [fuel.md](fuel.md) (T1 2,000 mB, T2 4,000 mB).
