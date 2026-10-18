# Ammo and magazines

How GregGuns ammo plugs into GT recipe maps and GT's own ammo-like parts.

- **Techguns baseline:** [ammo.md](../../techguns-spec/06-items/ammo.md) (gun to ammo table, magazines, refills), [recipes.md](../../techguns-spec/06-items/recipes.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md) (ammo column per tier), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (Ammo Press: "GT Assembler, Forming Press and Extruder recipes, per ammo type"), [0003 energy model](../../design/0003-energy-model.md)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205), IC2 (hard dependency)
- **Roadmap:** M2 (ammo exists), M4 (recipes)

## What changes from Techguns

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Ammo Press makes bullets and shells ([ammo.md](../../techguns-spec/06-items/ammo.md#loose-ammunition-sources)) | GT Assembler, Forming Press and Extruder recipes, per ammo type | [0002](../../design/0002-machine-deconfliction.md) |
| Bullets from iron, copper and lead nuggets by OreDict lookup | Bullets built from GT `round` parts and GT dusts, through the unificator | GT first; avoids the OreDict lookups behind [ITEM-02](../../techguns-spec/06-items/index.md#suspected-bugs) |
| Magazine refill by crafting grid; sneak-click to empty ([magazine items](../../techguns-spec/06-items/ammo.md#magazine-items)) | Same behaviour, kept. Empty-magazine recipes use GT plates and springs | Behaviour is gameplay, not a GT overlap |
| Energy Cell and Nuclear Power Cell are consumables filled in the Charging Station | Rechargeable electric items ([0003](../../design/0003-energy-model.md)); fluid ammo uses GT fluids ([fluids.md](fluids.md)) | Charging Station dropped |
| Ammo comes out of a Techguns-specific slot in the extra inventory | Unchanged here; the slot is area 05's | n/a |

Ammo tiers are in [0001](../../design/0001-tier-map.md): stone bullets at Steam/ULV; 9mm rounds, pistol magazines, shotgun shells and rifle bullets at LV; SMG and assault-rifle magazines and 40 mm grenades at MV; LMG and AS50 magazines, rockets, fuel and compressed-air tanks at HV; energy cells, advanced magazines, minigun drums and bio tanks at EV; Nether charges at LuV; nuclear power cells and tactical nuke rockets at ZPM.

## Integration points

### GregTech: recipe maps for ammo

- **Pack config:** none. GT recipes are code, and the pack may edit them with scripts; GregGuns recipes use the same maps so scripts see them.
- **Source:**
  - [`RecipeMaps.assemblerRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1267): up to 9 item inputs, 1 item output, 1 fluid input; the circuit slot overlay shows a programmed circuit can select a variant. This is the main ammo map.
  - [`RecipeMaps.formingPressRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L489): up to 6 item inputs, 1 output; at least 2 inputs. For stamped casings and pressed parts.
  - [`RecipeMaps.extruderRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1435): 2 inputs, 1 output; the second slot takes an extruder **shape** item. GT's shapes in `ItemList` are `Shape_Extruder_*` (`Plate`, `Cell`, `Ring`, `Rod`, and others from [L148](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L148)); none is bullet-shaped. A casing made by extruder needs an existing shape (`Rod` or `Cell`) or a new GregGuns shape item.
  - [`RecipeMaps.packagerRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L748): 2 inputs, 1 output (item plus a box). Not used for magazines; the magazine is an item, not a GT box.
  - Builder and tiers: `GTValues.RA.stdBuilder()` ([`GTValues` L264](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/GTValues.java#L264)); `TierEU.RECIPE_LV`, `RECIPE_MV`, `RECIPE_HV` ([L26-L28](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L26-L28)).
- **GregGuns does:** registers each ammo recipe with `GTValues.RA.stdBuilder()`, with `eut` from `TierEU` matching the ammo's D1 tier. The dev self-check from D1 fails if a recipe's inputs or EU/t exceed the tier of its output.
- **Pack-side change:** none.

Starting design for M4. Final inputs are set against the live GT progression ([D1](../../design/0001-tier-map.md)).

| Ammo | Map | Inputs | Tier |
|---|---|---|---|
| Stone Bullet | Crafting table | Cobblestone, gunpowder | Steam/ULV |
| 9mm Bullets | Assembler | One Lead `round`, one casing, gunpowder dust | LV |
| Shotgun Shell | Assembler | Casing plate, lead `round` x N, gunpowder dust | LV |
| Rifle Bullets | Assembler | Steel `round`, casing, gunpowder dust | LV |
| Casing (internal part) | Forming Press | Brass or copper `plate` | LV |
| Empty magazines | Assembler | Steel `plate`, `springSmall` | LV or MV by size |
| Filled magazines | Crafting table (as Techguns) and Assembler | Empty magazine, bullets | Same as bullets |
| 40 mm Grenade, Rocket | Assembler | Plates, explosive dust (`Materials.TNT`), circuit | MV, HV |
| Advanced Bullets | Assembler | Higher-tier `round`, gunpowder dust | EV |
| Nether Charge | Assembler | Netherrack, soul sand, gold, circuit | LuV |
| Nuclear Power Cell | Assembler | Plates, `Uranium235` dust, circuit | ZPM |

### GregTech: bullet prefixes and `round`

- **Source:**
  - [`OrePrefixes.round`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/OrePrefixes.java#L960-L969) is a unifiable metal part worth one ninth of an ingot. [`ProcessingRound`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingRound.java#L28-L48) gives it a lathe recipe from a nugget and two crafting recipes (hammer and file). It is also registered as AE2 matter-cannon ammo. A `round` is already a bullet slug, so GregGuns bullets are *made from* it rather than from nuggets.
  - [`OrePrefixes.bulletGtSmall`, `bulletGtMedium`, `bulletGtLarge`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/OrePrefixes.java#L1183-L1219) are defined with material amounts of 1/9, 1/6 and 1/3 of an ingot, `unifiable` and `recyclable`, but with `materialGenerationBits(TOOL | EMPTY)`. A search of GT5-Unofficial at 5.09.54.205 finds no item registered under them. They exist as names only.
- **GregGuns does:** keeps its own bullet and magazine items (they carry gun-specific meaning), and gives each bullet `ItemData` through `GTOreDictUnificator.addItemData` ([components-and-materials.md](components-and-materials.md#ore-dictionary-names)) so GT recycling returns lead, copper or steel. Registering the bullets under the `bulletGt*` ore names (for example `bulletGtSmallLead`) is an option: small for pistol rounds, medium for rifle rounds, large for the AS50's High Caliber Rounds. GT would then treat them as its own prefix. It is not required for any requirement, and is an open question.
- **GT has no ammo of its own.** A word search for ammo, bullet and cartridge across the `gregtech` package at this tag matches only `OrePrefixes.java`; the bullet prefixes and `round` are the only ammo-like data.
- **Pack-side change:** none.

### Dispenser behaviour and projectile ammo

Rockets, 40 mm grenades and tactical nuke rockets are fired by dispensers in Techguns ([dispenser behaviour](../../techguns-spec/06-items/ammo.md#dispenser-behaviour)). GT does not change vanilla dispensers, so there is no integration point; the behaviour is kept as the Techguns spec says.

## Requirements

- **GGS-06-18.** Every machine-made ammo or magazine recipe is registered through `GTValues.RA.stdBuilder()` into `assemblerRecipes`, `formingPressRecipes` or `extruderRecipes`, with `eut` equal to the `TierEU` constant for its D1 tier.
- **GGS-06-19.** GregGuns registers no Ammo Press machine and no `RecipeMap` of its own for ammo.
- **GGS-06-20.** Bullet recipes take metal as GT `round` or `nugget` through the unificator, never as Techguns OreDict `ingot*` lookups.
- **GGS-06-21.** Every ammo item has `ItemData` so GT recycling returns its metal.
- **GGS-06-22.** If an extruder recipe is used for a casing, its shape is an existing GT shape or a GregGuns item with its own recipe. No recipe depends on a missing shape.

## Tests

| ID | Verified by |
|---|---|
| GGS-06-18 | D1 dev self-check over the registered recipes |
| GGS-06-19 | Unit test: no registered `RecipeMap` name starts with `gregguns` for ammo; no ammo-press block exists |
| GGS-06-20 | Code review and a unit test of the recipe loader |
| GGS-06-21 | Unit test over the ammo items |
| GGS-06-22 | Unit test: every extruder recipe's shape input resolves to a non-null stack |

## Open questions

- **`bulletGt*` ore names.** Register GregGuns bullets under GT's unused bullet prefixes, or keep them private? A record is needed only if we choose to register.
- **Extruder shape.** Is a bullet-casing recipe in the extruder wanted, or is the Forming Press enough? Decide in M4.
