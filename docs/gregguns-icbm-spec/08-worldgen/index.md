# 08-worldgen (GregGuns ICBM)

## Overview

GregGuns-only area (the ICBM baseline has no worldgen): Abandoned Missile Silos on the Overworld and on other planets, their activity levels, guards and loot ceilings. It builds on the M8 placement system ([structure-placement](../../gregguns-spec/11-worldgen/structure-placement.md)) and [0019](../../design/0019-structures-on-ross128.md).

## Contents

All sheets are planned.

- silo-layout.md (planned): shaft, hall, side rooms, the O, D and Closed surface variants, never cascading (D17).
- activity-levels.md (planned): abandoned, partly abandoned, operational; the per-level damage pass in the layout (D36).
- silo-guard-spawner.md (planned): the spawner block on the M8 spawner system, mob lists (D36).
- loot-and-ceilings.md (planned): loot by tier, salvage, the planet table (D38).
- planets-and-placement.md (planned): sparse grid per dimension, flags, Ross128 rolled ceilings (D17, D38, 0019).

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Worldgen (none in the baseline) | Abandoned Missile Silos on a sparse grid, config-gated per dimension | M8 placement system | [D17](../DECISIONS.md#d17-in-detail) |
| Guards | GregGuns NPCs (Army Soldier, Commando, Stormtrooper) from silo guard spawners | M8 `BlockHoleSpawner` system | [D36](../DECISIONS.md#d36-in-detail) |
| Loot | Ceiling EV on the Overworld, rising one voltage tier per rocket tier on planets | GT, Galacticraft, GalaxySpace, Amun-Ra | [D38](../DECISIONS.md#d38-in-detail) |
| Ross128b, Ross128ba | Ceiling rolled per silo, EV to UMV | Amun-Ra / pack planets | [D38](../DECISIONS.md#d38-in-detail), [0019](../../design/0019-structures-on-ross128.md) |
| Planet tiers | GT's own dimension table | GT5-Unofficial `DimensionHelper` | [D38](../DECISIONS.md#d38-in-detail) |
| Salvage | Damaged T1 missile body (GT recycling), rare schematic chip | GT recycling | [D17](../DECISIONS.md#d17-in-detail) |
| Contents | Inert decorative GT casings, no live warheads | GT casings | [D17](../DECISIONS.md#d17-in-detail) |

## Decisions carried

D17, D36, D38; 0019 for Ross128.

## Roadmap

[I6](../../ROADMAP-ICBM.md#i6--world-missile-silos-on-every-planet); tuning in I7.

## Pack-side changes

None yet.

## Related areas

[03-missiles](../03-missiles/index.md) (salvaged bodies), [06-blocks-items](../06-blocks-items/index.md) (spawner block), [gregguns-spec 11-worldgen](../../gregguns-spec/11-worldgen/index.md).

## Open questions

- Silo dimensions (shaft about 21 across, hall about 64 x 64 x 24 are starting targets): to settle in the I6 layout sheet.
- Planets whose terrain cannot host the layout are skipped and logged; the list is built in I6.
