# 08-worldgen (GregGuns ICBM)

## Overview

GregGuns-only area (the ICBM baseline has no worldgen): Abandoned Missile Silos on the Overworld and on other planets, their activity levels, guards and loot ceilings. It builds on the M8 placement system ([structure-placement](../../gregguns-spec/11-worldgen/structure-placement.md)) and [0019](../../design/0019-structures-on-ross128.md).

## Contents

All sheets are written and match what is built; the tuning that remains is I7.

- silo-layout.md: the D128 complex: about 240 x 240 blocks on one flat level, a hub, major and minor halls, 35 to 40 rooms and a gallery of 4 to 10 real silos with their shafts (D128, D229) and the O, D and Closed variants, the room set, the escape shaft, never cascading (D17, D128).
- activity-levels.md: abandoned, partly abandoned, operational; the per-level damage pass in the layout (D36).
- silo-guard-spawner.md: the spawner block on the M8 spawner system, mob lists (D36).
- loot-and-ceilings.md: loot by tier, salvage, the planet table (D38).
- palette.md: the decorative blocks of a complex: 11 style sets, fallback chains, lamps, planet stone and the mining-worth rule (D240 to D244).
- planets-and-placement.md: sparse grid per dimension, flags, Ross128 rolled ceilings (D17, D38, 0019).

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Worldgen (none in the baseline) | Abandoned Missile Silos on a sparse grid, config-gated per dimension | M8 placement system | [D17](../DECISIONS.md#d17-in-detail) |
| Guards | GregGuns NPCs (Army Soldier, Commando, Stormtrooper) from silo guard spawners | M8 `BlockHoleSpawner` system | [D36](../DECISIONS.md#d36-in-detail) |
| Loot | Ceiling EV on the Overworld, rising one voltage tier per rocket tier on planets | GT, Galacticraft, GalaxySpace, Amun-Ra | [D38](../DECISIONS.md#d38-in-detail) |
| Ross128b, Ross128ba | Ceiling rolled per silo, EV to UMV | Amun-Ra / pack planets | [D38](../DECISIONS.md#d38-in-detail), [0019](../../design/0019-structures-on-ross128.md) |
| Planet tiers | GT's own dimension table | GT5-Unofficial `DimensionHelper` | [D38](../DECISIONS.md#d38-in-detail) |
| Salvage | Damaged T1 missile body (GT recycling), rare schematic chip | GT recycling | [D17](../DECISIONS.md#d17-in-detail) |
| Layout | A flat complex: hub, halls, 35 to 40 rooms, a gallery of 4 to 10 real silos | GregGuns Missile Silo (20141) | [D128](../DECISIONS.md) |
| Contents | Real machines (NASA Workbench, GT generators) within the D38 ceiling, no live warheads | Galacticraft, GT | [D129](../DECISIONS.md) |

## Decisions carried

D17, D36, D38, D128, D129; 0019 for Ross128.

## Roadmap

[I6](../../ROADMAP-ICBM.md#i6--world-missile-silos-on-every-planet); tuning in I7.

## Pack-side changes

None yet.

## Related areas

[03-missiles](../03-missiles/index.md) (salvaged bodies), [06-blocks-items](../06-blocks-items/index.md) (spawner block), [gregguns-spec 11-worldgen](../../gregguns-spec/11-worldgen/index.md).

## Open questions

- The level's depth below the surface, the hall's size and the room sizes (D128 sets the complex at about 240 x 240): to settle in the I6 layout sheet.
- Planets whose terrain cannot host the layout are skipped and logged; the list is built in I6.
