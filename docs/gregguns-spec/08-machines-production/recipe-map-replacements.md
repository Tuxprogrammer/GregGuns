# Recipe-map replacements

How the five dropped or replaced Techguns processing machines (Ammo Press, Metal Press, Chemical Laboratory, Fabricator, Charging Station) turn into GT recipes, and how GregGuns registers them.

- **Techguns baseline:** [ammo-press.md](../../techguns-spec/08-machines-production/ammo-press.md), [metal-press.md](../../techguns-spec/08-machines-production/metal-press.md), [chem-lab.md](../../techguns-spec/08-machines-production/chem-lab.md), [fabricator.md](../../techguns-spec/08-machines-production/fabricator.md), [charging-station.md](../../techguns-spec/08-machines-production/charging-station.md), and the registration order in [index.md](../../techguns-spec/08-machines-production/index.md#tgmachinerecipesaddrecipes-what-it-registers)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md), [0003 energy model](../../design/0003-energy-model.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205), IC2 (electric items)
- **Roadmap:** M4

## What changes from Techguns

GregGuns ships **no** machine block for these five. Each Techguns recipe becomes a recipe in a GT recipe map, so any GT machine of the right type and tier (single block or industrial multiblock) can run it. The tier gate in [0001](../../design/0001-tier-map.md) is the recipe's `eut`.

| Techguns machine | GT recipe map (`RecipeMaps` field) | GT machine that runs it | What replaces its special features |
|---|---|---|---|
| Ammo Press | `assemblerRecipes`, plus `formingPressRecipes` and `extruderRecipes` for pressed or extruded parts | Assembler (LV and up) | The "build plan" selector becomes a **programmed circuit** in the recipe (`.circuit(n)`). Output stack size per recipe replaces "units per cycle" |
| Metal Press | `benderRecipes`, `formingPressRecipes`, `wiremillRecipes`, `assemblerRecipes` | Bending Machine, Forming Press, Wiremill, Assembler | GT already generates plate and wire recipes for every material |
| Chem Lab | `chemicalReactorRecipes` / `multiblockChemicalReactorRecipes` (via `UniversalChemical`), `mixerRecipes`, `cannerRecipes`, `chemicalBathRecipes` | Chemical Reactor, Mixer, Fluid Canner, Chemical Bath | One tank pair becomes GT fluid hatches or cells |
| Fabricator | `assemblerRecipes`, `circuitAssemblerRecipes`; optionally the Assembly Line (`GTRecipeConstants.AssemblyLine`) for the top tier | Assembler, Circuit Assembler | Four fixed slots become up to 9 item inputs and 1 fluid |
| Charging Station | none. No recipes | GT Battery Buffer (any tier) and GT chargers | The "empty cell to full cell" recipe disappears, see below |

Side effects:

- The Techguns **stack upgrade** (multiplier from upgrade items) has no GT counterpart. Throughput comes from machine tier, GT overclocking and multiblock parallel. Area [06](../../techguns-spec/06-items/index.md) decides whether the upgrade item survives.
- Most Techguns machine bugs vanish with the machine: [MPRD-10](../../techguns-spec/08-machines-production/index.md#suspected-bugs), 13, 22 and 28 (Charging Station); 11, 12 and 14 (Chem Lab); 05 and 25 (Fabricator); 20, 21, 30 and 33 (presses). The GT machines have their own behaviour and their own bugs.

## Per-recipe mapping

The numbers are the row numbers of the Techguns sheets. "Drop" means GT already supplies the product, so no GregGuns recipe is written (per [0002](../../design/0002-machine-deconfliction.md), Techguns ores, plates, wires, circuits and fluids are dropped).

### Ammo Press

| Techguns | GregGuns |
|---|---|
| Plans 0-3: 9mm bullets x12, shotgun shell x16, rifle bullets x8, high-calibre rounds x4. Inputs 1 metal1 + 2 metal2 + 1 powder, 100 ticks, 5 RF/t | One Assembler recipe per ammo type, `.circuit(1)` to `.circuit(4)` as the plan. Inputs are GT plates or rods of the gate material for the tier ([0001](../../design/0001-tier-map.md)) plus gunpowder dust. `eut` = the ammo's tier (LV for 9mm, shells, rifle rounds). Later ammo (SMG magazines, grenades, rockets) is added by the milestone that adds the item |

### Metal Press (18 recipes)

| Techguns rows | GregGuns |
|---|---|
| 2-7, 14, 18 (ingot + ingot to 2 plates) | Drop. `benderRecipes` already holds a plate recipe for every GT material ([ProcessingPlate#L180-L197](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingPlate.java#L180-L197)) |
| 11, 16 (plates or ingots to wire) | Drop. `wiremillRecipes` ([ProcessingWire#L90](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingWire.java#L90)) |
| 1, 12 (Obsidian Steel, carbon plate) | Drop. GT steel and GT carbon materials |
| 8-10 (Mechanical Parts) | Assembler. If GT already has a matching small-gear or motor part, use that instead of a new item |
| 13, 17 (advanced bullets x16, 40 mm grenade x16) | Assembler, same pattern as the Ammo Press |
| 15 (Steam Armor Plating) | Assembler or Forming Press, gated at the EV steam-armour tier of [0001](../../design/0001-tier-map.md) |

### Chem Lab (17 recipes)

| Techguns rows | GregGuns |
|---|---|
| 1 (gunpowder), 2 (concrete), 16 (clay), 17 (leather), 14 (slimeball) | Drop. Vanilla or GT chains already make these |
| 3, 8 (rubber, plastic) | Drop. GT polymer chain |
| 4, 5b, 15 (Creeper Acid, Treated Leather) | Drop the fluid ([0002](../../design/0002-machine-deconfliction.md)). Treated Leather becomes a Chemical Bath recipe only if a GregGuns item still needs it |
| 5a, 6b, 6c, 9, 10 (fill or empty Bio, Fuel and Compressed Air tanks) | Fluid Canner. `cannerRecipes` fills any `IFluidContainerItem` with capacity above 0 ([FluidCannerBackend#L72-L73](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/maps/FluidCannerBackend.java#L72-L73)). So the GregGuns tank items implement `IFluidContainerItem` and use GT fuels ([0001](../../design/0001-tier-map.md) lists "fuel tanks (GT fuels), compressed-air tanks") |
| 6a, 6d (TGX explosive), 11 (Nether Charge), 7 (carbon fibres) | Chemical Reactor or Mixer through `UniversalChemical`, which adds to both the single-block and multiblock reactor maps ([GTRecipeConstants#L386-L400](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L386-L400)) |
| 12, 13 (Yellowcake, Nuclear Power Cell refuel) | Rework on GT's uranium materials in M5; no GregGuns recipe in M4 |

### Fabricator (6 recipes)

All six go to `assemblerRecipes`. Rows 3 to 5 use Techguns circuit names (`circuitBasic`, `circuitElite`); in GregGuns those are GT circuits chosen for the tier. Row 5 (Rad Emitter) sits at ZPM in [0001](../../design/0001-tier-map.md) and may use the Assembly Line instead of the Assembler. Row 6 (Nuclear Power Cell, Depleted) is produced by use, not crafting, once M5 defines the cell.

### Charging Station

[0003](../../design/0003-energy-model.md) makes energy cells and weapons IC2 electric items. GT's Battery Buffer takes an electric item only when the item's IC2 tier **equals** the buffer's tier ([MTEBasicBatteryBuffer#L206](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicBatteryBuffer.java#L206) and [#L226](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicBatteryBuffer.java#L226) check `GTModHandler.isElectricItem(stack, tier)`, which tests `getTier(stack) == tier`, [GTModHandler#L2295-L2298](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L2295-L2298)). So:

- No Charging Station block and no charging recipe.
- The "Energy Cell (Empty)" Fabricator recipe produces an uncharged electric item.
- A GregGuns electric item must report a tier through the IC2 `IElectricItem` API, or the buffer refuses it.

## Integration points

### GT5-Unofficial: registering a recipe
- **Pack config:** none.
- **Source:**
  - The recipe adder is `GTValues.RA.stdBuilder()` ([IGTRecipeAdder#L7](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/interfaces/internal/IGTRecipeAdder.java#L7)), with `itemInputs`, `circuit`, `itemOutputs`, `fluidInputs`, `fluidOutputs`, `duration`, `eut`, `metadata` and `addTo` ([GTRecipeBuilder#L281](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L281), [#L700](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L700), [#L1201-L1208](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L1201-L1208)). GT's own conventions are in [`docs/RecipeBuilder.md`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/docs/RecipeBuilder.md).
  - Voltage constants are in [`TierEU`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L26-L29) (`RECIPE_LV`, `RECIPE_MV`, `RECIPE_HV`, `RECIPE_EV`, ...).
  - Map fields: [assembler L1266](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266) (max 9 item inputs, 1 fluid, circuit slot), [circuit assembler L1277](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1277), [forming press L489](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L489), [extruder L1435](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1435), [bender L1229](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1229), [mixer L534](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L534), [chemical bath L611](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L611), [canner L1289](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1289).
  - `chemicalReactorRecipes` is the single-block map **only**; use `GTRecipeConstants.UniversalChemical` to feed both reactor maps ([RecipeMaps#L1123-L1127](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1123-L1127)).
  - Timing: GGFab registers its recipes from a `sBeforeGTPostload` callback ([GigaGramFab#L100](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/ggfab/GigaGramFab.java#L100)).
- **GregGuns does:** one `GregGunsRecipes` class per area, called from a `sBeforeGTPostload` callback. A recipe looks like this (our own code, not copied):

  ```java
  GTValues.RA.stdBuilder()
      .itemInputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 1L), gunpowderDust)
      .circuit(1)                              // ammo "plan"
      .itemOutputs(GregGunsItems.AMMO_9MM.stack(12))
      .duration(5 * SECONDS)
      .eut(TierEU.RECIPE_LV)
      .addTo(RecipeMaps.assemblerRecipes);
  ```

  Inputs use GT materials through `GTOreDictUnificator`, never raw ore names, so unification works.
- **Pack-side change:** none. GT adds the NEI tab, the catalyst list and the machines itself.

### IC2: electric items as chargeable products
- **Source:** the Battery Buffer's `isElectricItem` / `IElectricItem` check, linked above. IC2's API source is the evidence for the interface itself (see [INTEGRATION.md](../INTEGRATION.md#order-of-evidence)).
- **GregGuns does:** every chargeable GregGuns item implements `IElectricItem` with a `getTier` matching [0001](../../design/0001-tier-map.md). This is area [02](../../techguns-spec/02-guns/index.md) and [05](../../techguns-spec/05-armor/index.md) work, recorded here because it is why the Charging Station can be dropped.
- **Pack-side change:** none.

## Requirements

- **GGS-08-08** GregGuns registers no machine block for the Ammo Press, Metal Press, Chem Lab, Fabricator or Charging Station.
- **GGS-08-09** Every GregGuns ammo recipe is an Assembler (or Forming Press or Extruder) recipe whose `eut` equals the minimum voltage of the ammo's tier in [0001](../../design/0001-tier-map.md).
- **GGS-08-10** Ammo recipes that differ only by output use a distinct programmed-circuit number, and the numbers are listed in one enum so no two ammo types share a circuit with the same inputs.
- **GGS-08-11** GregGuns writes no recipe for a product GT already makes (plates, wires, rubber, plastic, gunpowder, concrete).
- **GGS-08-12** Every input comes from `GTOreDictUnificator` or a GregGuns `ItemStack`, not from a raw ore-dictionary name string.
- **GGS-08-13** Recipes are registered from a `GregTechAPI.sBeforeGTPostload` callback (or later), never from a constructor or static initialiser.
- **GGS-08-14** Fluid-filled GregGuns tanks implement `IFluidContainerItem` with capacity above 0, so GT's Fluid Canner can fill and empty them.
- **GGS-08-15** Every chargeable GregGuns item reports one IC2 tier through `IElectricItem#getTier`, equal to its tier in [0001](../../design/0001-tier-map.md), and can be inserted into a GT Battery Buffer of that tier.

## Tests

| Req | Verification |
|---|---|
| 08 | Unit test: the block registry has no entry named `ammoPress`, `metalPress`, `chemLab`, `fabricator` or `chargingStation` |
| 09, 10 | Unit test over `GregGunsRecipes`: for each ammo recipe, `getEUt()` equals the tier voltage and (map, inputs, circuit) is unique |
| 11, 12 | Unit test: no recipe output is a GT-material plate or wire. Code search for `OreDictionary.getOres("` in the recipes package |
| 13 | `runServer` smoke test: all expected recipes are present in `RecipeMaps.assemblerRecipes` after boot |
| 14 | Unit test on each tank item |
| 15 | `/gregguns test` scenario: place an item in a buffer of its own tier and in one of a different tier; only the first accepts it |

## Open questions

- **Stack upgrade.** Drop it, or turn it into a GT-style parallel or speed item for the one kept production multiblock? Settles under area 06; probably a new design record.
- **Assembly Line for ZPM-tier components.** Needs the research item chain from [0001](../../design/0001-tier-map.md). Decide in M5.
- **Nuclear items.** Yellowcake and the Nuclear Power Cell depend on GT's uranium and radioactive materials; defer to M5.
- **Charging tier rule conflicts with 0003.** [0003](../../design/0003-energy-model.md) says an item "charges only in a GT charger or battery buffer of that tier **or higher**". GT's Battery Buffer accepts an item only when the tiers are **equal** (see the Charging Station section above). A tier-IV rifle would not fit an EV or LuV buffer. Either 0003 is reworded to "of that tier", or GregGuns items report a lower tier than their recipe tier. Needs a short amendment to 0003.
