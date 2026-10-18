# Fluids and fluid containers

Techguns fluids become GT materials' fluids, and Techguns' tanks become fluid containers that hold them.

- **Techguns baseline:** [fluids.md](../../techguns-spec/06-items/fluids.md) (fluid table, containers), [ammo.md](../../techguns-spec/06-items/ammo.md#tanks-cells-and-other-refillables)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md) (HV: "fuel tanks (GT fuels), compressed-air tanks"), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) ("Techguns fluids (oil, fuel, acid): Drop"), [0003 energy model](../../design/0003-energy-model.md) ("Fuel items … use GT fluids")
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205)
- **Roadmap:** M2 (flamethrower, chainsaw, bio gun), M5 (power hammers, jump pack, oxygen)

## What changes from Techguns

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| `TGFluids` looks up `oil`, `fuel`, `redstone`, `ender`, `biofuel` by name and falls back to lava or `null` ([fluid table](../../techguns-spec/06-items/fluids.md#fluid-table)) | Fluids come from `Materials.<X>.getFluid(n)` or `.getMolten(n)`. No name lookup and no lava fallback | GT owns the fluids; a missing one is a bug in our recipe, not a reason to substitute lava. Avoids [ITEM-19](../../techguns-spec/06-items/index.md#suspected-bugs) |
| Creates `creeper_acid` always, and `milk` if no other mod has one | Creates **no** fluid. Acid is dropped. Milk is `Materials.Milk` | [0002](../../design/0002-machine-deconfliction.md); GT has milk |
| Creeper Acid Bucket and `BlockFluidAcid` | Dropped (bucket here; the block is area 07) | No recipe needs it once the Chem Lab is gone. Fixes [ITEM-04](../../techguns-spec/06-items/index.md#suspected-bugs) by removal |
| Fuel Tank takes 250 mB of `fuel`, or 500 mB of lava | Fuel Tank takes a configurable set of GT fuels, 1000 mB | Lava as fuel makes no sense in GT |
| Tanks are filled in the Chem Lab | Tanks are filled by any GT fluid-container machine (Fluid Canner, hatches, cells) | Forge `FluidContainerRegistry` / `IFluidContainerItem`, as GT's own cells |

## Integration points

### GregTech: materials and their fluids

- **Pack config:** none.
- **Source:**
  - [`Materials.getFluid`, `getGas`, `getPlasma`, `getMolten`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Materials.java#L2042-L2060) each return a `FluidStack`, or **`null`** when the material has no fluid of that kind.
  - Material fields: `Oil` (L379), `OilLight` (L534), `OilMedium` (L535), `OilHeavy` (L533), `Diesel` (L363), `BioDiesel` (L568), `Ethanol` (L364), `NitroFuel` (L738), `GasolineRegular` (L840), `GasolinePremium` (L838), `Biomass` (L353), `Redstone` (L752), `Ender` (L192), `Milk` (L377), `Air` (L417), `LiquidAir` (L727), `Oxygen` (L113), `LiquidOxygen` (L475), `SulfuricAcid` (L660), `NitricAcid` (L600), `Lava` (L277).
- **GregGuns does:** resolves each fluid it needs once, at post-init, through the table below. A `null` result disables the recipes or containers that need it and logs a warning. It never substitutes another fluid.
- **Pack-side change:** none.

### Fluid mapping

| Techguns field | GT fluid | Use in GregGuns |
|---|---|---|
| `WATER`, `LAVA` | Forge water and lava (`Materials.Lava`) | Only where a GT recipe already uses them |
| `OIL` | `Materials.Oil` (also `OilLight`, `OilMedium`, `OilHeavy`) | Not used by GregGuns. Oil is a GT process input; the Techguns oil-hole and Chem Lab uses are dropped ([0002](../../design/0002-machine-deconfliction.md), [0005](../../design/0005-world-presence.md)) |
| `FUEL` | `Materials.Diesel`, `GasolineRegular`, `GasolinePremium`, `NitroFuel`, `BioDiesel`, `Ethanol` | Fuel Tank contents. Default accepted set: `Diesel`, `GasolineRegular`, `BioDiesel`, `Ethanol`; `NitroFuel` and `GasolinePremium` give a damage bonus, a balance choice for M2 |
| `BIOFUEL` | `Materials.BioDiesel` | Bio Tank: accepted fluid for the bio gun |
| `LIQUID_REDSTONE` | `Materials.Redstone.getMolten(...)` | Reaction Chamber laser-focus recipe (area 08) |
| `LIQUID_ENDER` | `Materials.Ender.getFluid(...)`, checked for `null` | Reaction Chamber anti-gravity-core recipe (area 08). The fluid kind GT gives `Ender` is not confirmed here; check at M5 |
| `LIQUID_COAL` | Dropped | Unused in Techguns outside `TGFluids` |
| `MILK` | `Materials.Milk` | Only if a GregGuns recipe needs it; GregGuns creates nothing |
| `ACID` (`creeper_acid`) | Dropped. Acid steps use `Materials.SulfuricAcid` or `NitricAcid` | Only the yellowcake and treated-leather recipes used it, and both move to GT recipe maps ([components-and-materials.md](components-and-materials.md)) |
| Compressed air (Compressed Air Tank contents) | `Materials.Air` (gas) | Power hammers, jump pack, oxygen tanks: HV in D1 |
| Oxygen (oxygen tanks) | `Materials.Oxygen` (gas) or `LiquidOxygen` | Oxygen mask and tank gear (area 05) |

### GregTech: fluid containers

- **Source:**
  - [`GTItemCell`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/GTItemCell.java#L42) registers each filled cell with `FluidContainerRegistry.registerFluidContainer(fluid, filledCell, ItemList.Cell_Empty.get(1))`. `Cell_Empty` is at [`ItemList` L200](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L200).
  - [`GTFluid`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/fluid/GTFluid.java#L130) does the same for other containers.
  - [`GTUtility.fillFluidContainer`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1186) is the helper GT machines use.
  - Precedent for a purpose-built GT container item: the plastic [Fuel Can](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/MetaGeneratedItem01.java#L1093-L1107), made from Polyethylene plates and carrying `ItemData` and Thaumcraft aspects.
- **GregGuns does:**
  - The Fuel Tank, Bio Tank, Compressed Air Tank and Oxygen Tank are GregGuns items that implement Forge `IFluidContainerItem` (capacity 1000 mB each, config) and are also registered with `FluidContainerRegistry` for the empty-to-full pair of each accepted fluid. GT machines then fill and drain them.
  - A gun reads and drains the tank's stored fluid directly. The per-shot cost is a gun stat, not a tank property.
  - The tank's recipe uses GT plates and `Cell_Empty`-class parts ([components-and-materials.md](components-and-materials.md)).
  - Tanks are not stackable once they hold fluid (NBT), and they carry no radiation or heat tag.
- **Pack-side change:** none.

### Energy cells

Per [0003](../../design/0003-energy-model.md), the Energy Cell and Nuclear Power Cell are no longer consumable fluid-style ammo. The Energy Cell becomes a rechargeable IC2 electric item. The Nuclear Power Cell is a GregGuns item whose recharge route is a decision for M2. GT's lithium battery family (`ItemList.Battery_RE_LV_Lithium`, [L526](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L526)) is the capacity baseline.

## Requirements

- **GGS-06-14.** GregGuns registers no `Fluid` and no fluid block.
- **GGS-06-15.** Every fluid in a GregGuns recipe or container comes from `Materials.<X>.getFluid/getGas/getMolten` and is `null`-checked. A `null` result disables the dependent recipe or container and logs a warning.
- **GGS-06-16.** GregGuns tanks implement `IFluidContainerItem` and accept only the config-listed GT fluids.
- **GGS-06-17.** A GT Fluid Canner (or any machine using `FluidContainerRegistry`) can fill and drain every GregGuns tank.

## Tests

| ID | Verified by |
|---|---|
| GGS-06-14 | Unit test: `FluidRegistry` holds no fluid with a `gregguns` prefix after init |
| GGS-06-15 | Unit test with a stubbed `Materials` that returns `null` |
| GGS-06-16 | Unit test: `fill` with a rejected fluid returns 0 |
| GGS-06-17 | `runServer` check with a Fluid Canner recipe lookup, then a manual fill in game |

## Open questions

- **`Materials.Ender`.** Whether GT 5.09.54.205 gives `Ender` a fluid, and of which kind, is not confirmed. It decides the Reaction Chamber's anti-gravity-core recipe (area 08).
- **Fuel set and bonuses.** Which fuels the Fuel Tank accepts, and whether better fuel increases flamethrower damage, is an M2 balance decision.
- **Acid.** Dropping creeper acid removes a Techguns hazard fluid. If a hazard fluid is wanted in the world, use a GT fluid and record the choice in a design record.
