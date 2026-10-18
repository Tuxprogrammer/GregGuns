# 0010 · Material remap and tier additions

- Status: Accepted (by default during the MVP push; revisit after MVP)
- Gate: D1 (extends [0001](0001-tier-map.md))
- Date: 2026-10-18

## Context
The recipe sheets ([02](../gregguns-spec/02-guns/recipes.md), [05](../gregguns-spec/05-armor/recipes.md), [06](../gregguns-spec/06-items/recipes.md), [07](../gregguns-spec/07-blocks/recipes.md), [08](../gregguns-spec/08-machines-production/recipes.md), [09](../gregguns-spec/09-machines-utility/recipes.md)) were written in parallel and disagree on five points:

1. **Obsidian steel.** GT has no such material ([components-and-materials.md](../gregguns-spec/06-items/components-and-materials.md)). 06 maps it to Stainless Steel; 05 and 09 map it to Aluminum.
2. **Carbon fiber.** GT has no such material. 06 uses `plate.EpoxidFiberReinforced`; 05 uses IC2 carbon plate plus Titanium.
3. **Tank capacity.** 06 makes fuel and air tanks 1,000 mB; 02 assumes 250 mB.
4. **Energy cells.** 06 keeps `cell_energy`, but 02 makes every energy weapon its own IC2 battery ([0003](0003-energy-model.md)).
5. **Missing tiers.** 0001 gives no tier for blocks, machines, charges, the Beret, the HEV suit, the Tactical Mask, Bandages, Cybernetic Parts, High Caliber Rounds or Advanced Bullets.

## Decision
1. **Materials follow the tier, not the Techguns name.** Wherever Techguns uses iron, steel, obsidian steel or carbon as a *tier* material, GregGuns uses the [0001](0001-tier-map.md) gate material of the tier the **consuming item** sits in:
   - Steam/ULV Bronze, LV Steel, MV Aluminum, HV Stainless Steel, EV Titanium, IV Tungstensteel, LuV HSS-G, ZPM Naquadah alloy.
   - So an MV armor plate that was "obsidian steel" is Aluminum, and an HV gun receiver is Stainless Steel. Both 06's ladder and 05/09's choice are instances of this one rule.
   - The component's display name drops the Techguns material: "Hardened Receiver", not "Obsidian Steel Receiver".
2. **Carbon** is GT `plate.EpoxidFiberReinforced` plus the tier material, everywhere. IC2 carbon plate is not used.
3. **Tanks** hold **1,000 mB**, the same as a GT cell, and are filled in the GT Fluid Canner. Per-shot consumption in 02 is restated in mB against that capacity.
4. **No energy-cell ammo.** Energy weapons are their own IC2 batteries (0003, [0008](0008-exact-tier-charging.md)). `cell_energy` is dropped. The nuclear power cell stays as the Nuclear Death Ray's consumable fuel.
5. **Tier additions.** The tiers the recipe agents proposed are adopted:
   - Beret MV, Tactical Mask HV, HEV suit EV;
   - Bandages LV, Cybernetic Parts EV, High Caliber Rounds HV, Advanced Bullets EV;
   - decorative blocks LV;
   - charges and the kept machines at the tier of their sheet's recipe. The machines' tiers are listed in the [machine roster](../gregguns-spec/08-machines-production/machine-roster.md).
6. **Names.** The component index in [06 recipes](../gregguns-spec/06-items/recipes.md#component-index) is the single source of component names. Other sheets use its display names and registry names exactly.

## Consequences
- The dev self-check verifies that every recipe's tier material matches the consuming item's tier.
- 02, 05, 06 and 09 are edited to follow this record. The Obsidian Steel and carbon questions in 06's open questions are closed.

## Alternatives considered
- **Add GregGuns materials (Obsidian Steel, Carbon) to GT through the materials API:** rejected. CONTRIBUTING forbids duplicating what GT provides, and new materials would need their whole processing chain.
- **One fixed material per Techguns name** (Obsidian Steel always Stainless Steel): rejected, because MV armor would then need an HV material.
