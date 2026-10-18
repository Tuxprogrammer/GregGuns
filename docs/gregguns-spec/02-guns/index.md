# 02-guns: GregGuns integration

The Techguns baseline is [02-guns](../../techguns-spec/02-guns/index.md). This folder records only where GregGuns differs from it and how it plugs into the GTNH pack. See [CONVENTIONS.md](../CONVENTIONS.md) for the sheet format and [INTEGRATION.md](../INTEGRATION.md) for the evidence rules.

## Overview

Guns touch the pack in five places: how they are built (GT recipe maps), how energy weapons store and gain charge (IC2 electric items and GT chargers), how damage meets GT's and other mods' systems, how the offhand slot (Backhand) interacts with right-click, and how fuel guns take GT fluids. Everything else about guns (stats, firing, reload, projectiles, tooltips, NPC firing, the catalogue) has no integration point; see the Techguns spec and the design records.

All evidence is pinned to GT5-Unofficial `5.09.54.205`, Backhand `1.8.16`, Battlegear2-for-Backhand `1.6.8-backhand` and IC2 `2.2.2.828` (API source plus decompiled internals, hedged where used).

## Contents

| Sheet | Topic | Requirements |
|---|---|---|
| [gt-recipes.md](gt-recipes.md) | Final assembly on GT recipe maps, per-family map and tier table, Assembly Line for LuV and ZPM | GGS-02-01 to 08 |
| [energy-electric-items.md](energy-electric-items.md) | Energy guns as `IElectricItem`, GT charging rules, capacity baselines | GGS-02-09 to 16 |
| [damage-interop.md](damage-interop.md) | Damage sources, GT hazmat, other mods' armour, `LivingAttackEvent` listeners | GGS-02-17 to 23 |
| [offhand-backhand.md](offhand-backhand.md) | Right-click fallback, offhand blacklist, Battlegear2, enchantments | GGS-02-24 to 28 |
| [fluid-ammo.md](fluid-ammo.md) | Fuel, bio and air tanks on GT fluids and the Fluid Canner | GGS-02-29 to 34 |

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Gun crafting-table recipes | Final assembly on `assemblerRecipes` at the 0001 tier voltage; Assembly Line for LuV and ZPM | GT5-Unofficial recipe maps | [gt-recipes.md](gt-recipes.md) |
| Ammo Press, Fabricator, Metal Press | GT Assembler, Lathe, Extruder, Bender, Hammer; GT `round` prefix for slugs | GT5-Unofficial | [gt-recipes.md](gt-recipes.md) |
| Energy Cell ammo, Charging Station | The gun is an `IElectricItem`; GT chargers and battery buffers charge it | IC2 API, GT5-Unofficial | [energy-electric-items.md](energy-electric-items.md) |
| Cancel-and-reimplement damage event | Keep the model, keep other mods' listeners and armour working | GT5-Unofficial (hazmat, armour), GT++, Forge | [damage-interop.md](damage-interop.md) |
| Unflagged fire damage | Fire flag set; secondary effects respect GT hazmat | GT5-Unofficial `HazardProtection` | [damage-interop.md](damage-interop.md) |
| RMB zoom and charge; akimbo as one item | Stop-list entries so the offhand is not also used; guns blacklisted from the offhand | Backhand, Battlegear2 | [offhand-backhand.md](offhand-backhand.md) |
| Fuel, bio and air tanks filled in the Chem Lab | `IFluidContainerItem` tanks filled in the GT Fluid Canner; fuels from GT fuel maps | GT5-Unofficial | [fluid-ammo.md](fluid-ammo.md) |
| Gun enchantability, enchanting | Unchanged: not enchantable (enchantability 0). No enchantment is registered, so no ID collides. | Battlegear2 (IDs 126 to 132), Infernal Mobs | [offhand-backhand.md](offhand-backhand.md#battlegear2-backhand-edition) |

**No integration point** (see [the Techguns spec](../../techguns-spec/02-guns/index.md) and the [design records](../../design/README.md)): gun stats and catalogue, firing algorithm, spread and zoom maths, reload rules, NBT layout, tooltips and lang keys, NPC firing, the weapon-stats config, grenade behaviour. Their pack-facing parts (keybinds, networking, config library) are in [01-core](../01-core/).

## Pack-side changes

These land as a PR to the [GTNH modpack repository](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack) and are collected in [PACK-CHANGES.md](../PACK-CHANGES.md).

| File | Change | Why | Sheet |
|---|---|---|---|
| `config/backhand.cfg` | In `general`, add `gregguns:<registry name>` for each gun with right-click zoom, charge or reload (and each hold-to-charge tool gun) to `mainhandUseStopsOffhandFallback` | Stops one right-click from also using the offhand item | [offhand-backhand.md](offhand-backhand.md) |
| `config/backhand.cfg` | In `general`, add `gregguns:<registry name>` for each gun item to `offhandBlacklist` | A gun in the offhand cannot fire; the slot would only hide a loaded weapon | [offhand-backhand.md](offhand-backhand.md) |

Registry names are fixed when M1 and M2 register the items. GregGuns never writes to the pack's configs at runtime.

## Related areas

- [01-core](../01-core/): keybinds (the zoom decision in the offhand sheet's open questions), config library.
- [05-armor](../05-armor/): which GregGuns armour sets implement GT's `IHazardProtector`; the baubles sheet for equipment slots.
- [06-items](../../techguns-spec/06-items/index.md): ammo and tank items, whose recipes also use the GT maps described here.
- [10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md): NPC-held guns (no EU spend), infernal-mob drops of guns.

## Open questions

| Question | Needs | Sheet |
|---|---|---|
| LuV and ZPM final recipes: GT's own LuV motor uses `RECIPE_IV` on the Assembly Line, one tier below the item. 0001 says the item's own tier. | Amendment to 0001 | [gt-recipes.md](gt-recipes.md) |
| 0003 says an item charges in a charger "of that tier or higher". GT's chargers and buffers accept the **exact** tier only. | Amendment to 0003 | [energy-electric-items.md](energy-electric-items.md) |
| How the damage model runs inside vanilla's hit: keep Techguns' cancel-and-reimplement, or run in `LivingHurtEvent` with an unblockable source and a direct `ApplyArmor` call. The sheets assume the second. | New record under D4 (addendum to 0004) | [damage-interop.md](damage-interop.md) |
| RMB zoom conflicts with an offhand item (stop list) and blocks an offhand grenade. Moving zoom to a key removes the conflict. | Input decision in [01-core](../01-core/) | [offhand-backhand.md](offhand-backhand.md) |
