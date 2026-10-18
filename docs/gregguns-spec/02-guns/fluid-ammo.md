# Fluid ammo: fuel, bio and air tanks on GT fluids

- **Techguns baseline:** [06-items/ammo.md](../../techguns-spec/06-items/ammo.md) (Fuel Tank, Bio Tank and Compressed Air Tank as ammo), [06-items/fluids.md](../../techguns-spec/06-items/fluids.md), [sheets-energy-special.md](../../techguns-spec/02-guns/sheets-energy-special.md) (Flamethrower, Bio Gun)
- **Design records:** [0003 Energy model](../../design/0003-energy-model.md) (fuel items use GT fluids), [0002 Machine deconfliction](../../design/0002-machine-deconfliction.md) (Techguns fluids dropped for GT fluids), [0001 Tier map](../../design/0001-tier-map.md)
- **Pack mods:** GregTech (`gregtech`, `5.09.54.205`)
- **Roadmap:** M2 (Flamethrower, Chainsaw), M5 (Bio Gun, Power Hammer)

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| Fuel Tank: filled in the Chem Lab with 250 mB of Techguns `fuel` (or 500 mB lava). 100 rounds for the Flamethrower, 300 for the Chainsaw. Two items, full and empty. | A tank item that is a Forge `IFluidContainerItem` holding a GT fluid. Filled and emptied in the **GT Fluid Canner**. One item, with or without fluid. | The Chem Lab is dropped ([0002](../../design/0002-machine-deconfliction.md)). The Canner already fills any such item. |
| The accepted fuel is whatever the `fuel` fluid is | The accepted fuels are the liquids that GT's fuel recipe maps list, so GT's fuels, and any added later, work | "GT first" ([CONTRIBUTING](../../../CONTRIBUTING.md#gtnh-integration)). |
| Bio Tank: bio mass, water and an empty tank | A tank that holds GT `Biomass` | Same reason. |
| Compressed Air Tank: coal, water, empty tank | A tank that holds GT `Air` gas | GT has no "compressed air" fluid. The tank itself is the compression. |

## Integration points

### Forge `IFluidContainerItem` and the GT Fluid Canner

- **Pack config:** none.
- **Source:**
  - GT's Fluid Canner map is `RecipeMaps.cannerRecipes`: [RecipeMaps.java#L1289](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1289). Its backend invents a recipe on the fly for any item that implements `IFluidContainerItem` with a capacity above 0. It uses `eut(1)`, to fill an empty container or to empty a filled one: [FluidCannerBackend.java#L28-L68](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/maps/FluidCannerBackend.java#L28-L68) and [L70-L75](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/maps/FluidCannerBackend.java#L70-L75).
  - The fill helper only fills a container that is **empty** (`getFluid(stack) == null`) and only when the supplied fluid amount is **at least the container's whole capacity**: [GTUtility.java#L1186-L1209](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1186-L1209). Reading the fluid back is [GTUtility.java#L1276-L1283](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1276-L1283).
  - GT's own `MetaBaseItem` is also an `IFluidContainerItem` ([MetaBaseItem.java#L54-L55](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaBaseItem.java#L54-L55)), so this is a normal pack contract.
- **GregGuns does:**
  - Each tank item class implements `IFluidContainerItem`. Techguns' Fuel Tank already does (see the baseline), so the contract is not new.
  - The capacity is the tank's size in mB, a config option. The Techguns figure is the starting point: 250 mB for the Fuel Tank.
  - A tank that is partly used is still "filled" to GT. The Canner can empty it, but it cannot top it up. The tooltip says so.
  - GregGuns writes **no** Canner recipe. The tank is craftable on `assemblerRecipes` per [gt-recipes.md](gt-recipes.md).
- **Pack-side change:** none.

### GT fuel maps: which liquids the Flamethrower and Chainsaw accept

- **Source:**
  - GT keeps its fuels in `RecipeMap<FuelBackend>` fields: `dieselFuels`, `gasTurbineFuels`, `denseLiquidFuels`, `hotFuels` and others, from [RecipeMaps.java#L1513](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1513) onwards.
  - `FuelBackend.findFuel(Fluid)` returns the fuel recipe for a fluid, or null: [FuelBackend.java#L96-L103](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/maps/FuelBackend.java#L96-L103). The map is reached with `RecipeMaps.dieselFuels.getBackend()`.
  - How GT's own generators use it: `getFuelValue(FluidStack)` multiplies the recipe's `mSpecialValue` by the generator's efficiency and the amount consumed: [MTEBasicGenerator.java#L283-L290](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicGenerator.java#L283-L290). `mSpecialValue` is the fuel value (`FUEL_VALUE` is an alias for it, [GTRecipeConstants.java#L790](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L790)).
  - Other mods add fuels through the same maps (the `Fuel` adder and `FUEL_TYPE`), for example [RecipeLoader.java#L507-L512](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/loader/RecipeLoader.java#L507-L512), so GregGuns sees them with no extra work.
- **GregGuns does:**
  - A tank accepts a fluid only if `dieselFuels` or `denseLiquidFuels` has a fuel for it (the liquid fuel maps; gas turbine and plasma fuels are not fed to a handheld flamethrower). The list of maps is a config option.
  - Rounds per mB are scaled by the fluid's fuel value against a reference fuel, so denser fuels give more rounds. The reference fuel and the base ratio are tuned in M2 ([0003](../../design/0003-energy-model.md): "chosen per item in M2 and M5").
- **Pack-side change:** none.

### Bio and air tanks

- **Source:** the GT materials are `Materials.Biomass` ([Materials.java#L353](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Materials.java#L353)) and `Materials.Air` ([Materials.java#L417](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Materials.java#L417)). Fluids are fetched with `getFluid(n)` or `getGas(n)`; GT does this for Air at [CentrifugeRecipes.java#L80](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/CentrifugeRecipes.java#L80).
- **GregGuns does:** the Bio Gun's tank accepts `Materials.Biomass`; the Power Hammer's accepts `Materials.Air`. Both lists are config options. Neither is a "fuel" in GT's sense, so there is no value scaling.
- **Pack-side change:** none.

## Requirements

- **GGS-02-29.** Every fluid-ammo tank item implements `net.minecraftforge.fluids.IFluidContainerItem` with a capacity above 0.
- **GGS-02-30.** A GT Fluid Canner with no GregGuns recipe can fill an empty tank and empty a filled tank.
- **GGS-02-31.** The Flamethrower and Chainsaw tanks accept a fluid exactly when `FuelBackend.findFuel` finds it in one of the configured fuel maps.
- **GGS-02-32.** Rounds per tank scale with the fluid's `mSpecialValue` against a configured reference. A fluid with no fuel entry is rejected, not treated as a default fuel.
- **GGS-02-33.** The Bio Gun's tank accepts only `Materials.Biomass`. The Power Hammer's accepts only `Materials.Air`.
- **GGS-02-34.** GregGuns registers no fluid of its own for ammo. Techguns' `fuel`, acid and oil fluids are not recreated.

## Tests

- GGS-02-29, 33, 34: unit test on the tank item classes and the fluid registry.
- GGS-02-30: `/gregguns test` scenario that runs a tank through a Fluid Canner (or calls `GTUtility.fillFluidContainer` and `GTUtility.getFluidForFilledItem`).
- GGS-02-31, 32: unit test with a fake `FuelBackend` entry, and a `runServer` check that Diesel is accepted and Water is rejected.

## Open questions

- **Tank capacity versus Canner rule.** The fill helper fills a tank only from a fluid amount at least as big as the capacity. A 250 mB tank therefore needs 250 mB in the Canner's input. This is fine for M2, but check it when the tank sizes are tuned.
- **Chainsaw and Power Hammer fuel.** [0003](../../design/0003-energy-model.md) says "a fuel or compressed air, chosen per item in M2 and M5". This sheet assumes fuel for the Chainsaw and air for the Power Hammer, as in Techguns. Confirm in those milestones.
