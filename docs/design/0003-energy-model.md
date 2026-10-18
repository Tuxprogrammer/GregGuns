# 0003 · Energy model

- Status: Accepted (by default during the MVP push; revisit after MVP)
- Gate: D3
- Date: 2026-10-17

## Context
Techguns uses RF for machines, energy weapons and powered armour. GTNH runs on GT EU. See the [framework spec](../techguns-spec/08-machines-production/framework.md).

## Decision
- **EU only.** No RF anywhere in GregGuns.
- **Electric items.** Energy weapons, powered armour, the jetpack and the anti-grav device are IC2 electric items, the same way GT's own items are. Each has the voltage tier of its D1 tier, so it charges only in a GT charger or battery buffer of that tier or higher.
- **Capacity.** As a baseline, an item holds what a GT battery of the same tier holds. Each item is tuned in its milestone, and every value is a config option.
- **Machines.** The kept machines are GT MetaTileEntities. They take EU at their tier voltage and follow GT's overvoltage rules.
- **Fuel items.** The flamethrower, chainsaw and power hammers use GT fluids: a fuel or compressed air, chosen per item in M2 and M5.

## Consequences
- Techguns' Charging Station and RF cells are dropped.
- Energy-cell ammo becomes rechargeable batteries rather than consumables.

## Alternatives considered
- **Keep RF through a GT bridge:** rejected, because it isn't native to the pack.
- **Consumable energy cells only:** rejected, because GTNH players expect rechargeable electric items.
