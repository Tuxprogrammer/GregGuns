# 0008 · Exact-tier charging

- Status: Accepted (by default during the MVP push; revisit after MVP)
- Gate: D3 (amends [0003](0003-energy-model.md))
- Date: 2026-10-18

## Context
[0003](0003-energy-model.md) says each electric item "charges only in a GT charger or battery buffer of that tier or higher". GT doesn't work that way. A battery buffer or charger accepts an item only when the item's tier **equals** the machine's tier:

- `MTEBasicBatteryBuffer.allowPutStack` calls `GTModHandler.isElectricItem(stack, mTier)` ([MTEBasicBatteryBuffer.java#L221-L227](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicBatteryBuffer.java#L221-L227));
- that method tests `getTier(stack) == aTier` ([GTModHandler.java#L2295-L2298](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L2295-L2298)).

IC2's own charge manager would accept a higher-tier charger, but GT's machines never offer the item one. Areas [02-guns](../gregguns-spec/02-guns/energy-electric-items.md) and [08-machines-production](../gregguns-spec/08-machines-production/index.md) both found this.

## Decision
- A GregGuns electric item charges in a GT charger or battery buffer of **exactly** its D1 tier, the same as GT's own batteries and tools.
- Its `getTier` returns that tier, and its `getTransferLimit` matches GT's battery of the same tier.
- Charging from other sources (an IC2 battery item in the inventory, or armour that charges held items) follows the IC2 API's `tier` argument unchanged.
- Everything else in 0003 stands.

## Consequences
- The rest of 0003 is unchanged. Its status line points here.
- Players need a charger at each tier they own weapons for. That is normal in GTNH.
- The dev self-check verifies that every electric item's `getTier` matches its 0001 tier.

## Alternatives considered
- **Report a lower tier so that more chargers accept the item:** rejected, because it misreports the item's voltage and breaks tier gating.
