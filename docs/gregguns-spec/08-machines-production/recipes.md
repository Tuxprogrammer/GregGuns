# Reaction Chamber recipes

How every GregGuns production machine is built (Ammo Press, Ammunition Works, Reaction Chamber, Retort), and the recipes the Reaction Chamber runs. Under the owner's machine policy ([0011](../../design/0011-machine-policy.md), amending [0002](../../design/0002-machine-deconfliction.md)) these are the kept production machines; the rest of area 08 is recipes in GT's own maps, listed in [recipe-map-replacements.md](recipe-map-replacements.md).

- **Techguns baseline:** [reaction-chamber.md](../../techguns-spec/08-machines-production/reaction-chamber.md) ([§ 5 recipe list](../../techguns-spec/08-machines-production/reaction-chamber.md#5-full-recipe-list-tgmachinerecipes)), build recipes B25 to B27 and R30 in [06-items/recipes.md](../../techguns-spec/06-items/recipes.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md) (UV Emitter and Laser Focus at EV, Anti Gravity Core at IV via the device), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md), [0003 energy model](../../design/0003-energy-model.md), [0010 material remap](../../design/0010-material-remap-and-tier-additions.md), [0011 machine policy](../../design/0011-machine-policy.md); tiers and IDs in the [machine roster](machine-roster.md)
- **Sibling sheets:** [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md) (structure, rules, risks), [metatileentity-framework.md](metatileentity-framework.md)
- **Format:** [CONVENTIONS § Recipe sheets](../CONVENTIONS.md#recipe-sheets)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205), StructureLib (`structurelib`)
- **Roadmap:** M4

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| Housing x9 (B25), Glass x6 (B27), Controller (B26) built in the table from Cybernetic Parts, Mechanical Parts, elite circuits and a Chemical Laboratory | One **Reaction Chamber Casing** block (Assembler), GT glass for the ring, and a **controller** crafted from GT components around the casing | [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#what-changes-from-techguns): visible GT-style casings, GT hatches |
| Hatches are three of the housing blocks | GT hatches. No GregGuns hatch exists | The Input Bus, Output Bus, Input Hatch, Energy Hatch, Maintenance Hatch and Muffler Hatch of GT are used as they are |
| Heat Ray Focus in a GUI slot (R30) | Heat Ray Focus is a non-consumed (stack size 0) recipe input held in an Input Bus | GT's convention for molds and lenses |
| Process cost in RF per reaction tick | `eut` at a `TierEU.RECIPE_*` value, which also gates the recipe by hatch voltage | Tier gating as in [0001](../../design/0001-tier-map.md) |
| Seven Techguns recipes | Two kept, one deferred, four dropped | [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#recipe-map-and-the-7-techguns-recipes) |

## Integration points

### GregTech: multiblock controllers are crafted around a casing

- **Pack config:** none.
- **Source:** GT's multiblock controllers are shaped crafting recipes with the controller's casing in the center cell and a tier circuit, a cable and tier parts around it:
  - Vacuum Freezer (EV): casing `Casing_FrostProof`, `Electric_Pump_HV`, `circuit.EV`, `cableGt01.Gold` ([MTERecipeLoader.java#L3407-L3413](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3407-L3413)).
  - Blast Furnace and Implosion Compressor use the same layout with their own casing ([#L3401-L3406](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3401-L3406), [#L3414-L3419](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3414-L3419)).
  - Casings are Assembler recipes: 6 plates and a frame, programmed circuit 1, `RECIPE_LV / 2` ([AssemblerRecipes.java#L2671-L2700](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2671-L2700)).
- **GregGuns does:** follows that layout for the controller and the casing.
- **Pack-side change:** none.

### GregTech: recipe voltage gates by energy hatch

- **Source:** `ParallelHelper` returns `insufficientPower` when the available EU/t is below the recipe's EU/t ([ParallelHelper.java#L398-L402](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/ParallelHelper.java#L398-L402)). A GT multiblock therefore only starts a recipe its energy hatches can power, so a recipe's `eut` at `TierEU.RECIPE_EV` needs an EV Energy Hatch and one at `RECIPE_IV` needs an IV hatch ([TierEU.java#L25-L39](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L25-L39) gives 1 920 and 7 680 EU/t). The controller therefore does not need a different hull per recipe.
- **GregGuns does:** the controller is one EV block. The Laser Focus recipe runs with any Energy Hatch of EV or above. The Anti Gravity Core recipe runs only with an IV or above hatch.
- **Pack-side change:** none.

## Build recipes

### Reaction Chamber Casing (1 recipe)

| Field | Value |
|---|---|
| Output | Reaction Chamber Casing x1 (a GregGuns block on casing texture page 50, [metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-casing-texture-pages)) |
| Tier | EV (Titanium) |
| Method | `RecipeMaps.assemblerRecipes` |
| Inputs | `plate.Titanium` x4, `plate.Lead` x2, `frameGt.Titanium` x1, programmed circuit 1 |
| EU/t, time | `TierEU.RECIPE_LV / 2`, 2 s 10 ticks (the same as every GT casing) |
| Gate | `plate.Titanium` and `frameGt.Titanium`. The casing itself costs little; the controller below and the EV Energy Hatch are the real gate |
| Notes | The two lead plates stand for the radiation shielding the chamber's `RAD_*` risks need. Techguns replaced: Housing (B25) |

### Retort controller (1 recipe)

This is the controller of the multiblock form, now the Retort (MTE 20108, [multiblocks.md](multiblocks.md#retort)). It was written for the original 3 x 4 x 3 chamber ([GGS-08-16](reaction-chamber-multiblock.md#requirements)); the Retort keeps the same casing, so the recipe stands. The **single-block** Reaction Chamber has its own recipes in [Machine build recipes](#machine-build-recipes). The Retort needs no other GregGuns casing: its walls are the Reaction Casing above, its vessel is GT borosilicate glass and its feet are GT `frameGt.Steel` ([multiblocks.md](multiblocks.md#casings-used-by-the-four-multiblocks)).

| Field | Value |
|---|---|
| Output | Retort controller (MTE 20108) x1 |
| Tier | EV |
| Method | Hand, shaped, no tool letter. Same flags as GT's controllers (`BITS`, `DO_NOT_CHECK_FOR_COLLISIONS`) |
| Gate | `ItemList.Emitter_EV`, `ItemList.Sensor_EV`, `ItemList.Electric_Pump_EV` and `circuit.EV` |
| Notes | Techguns replaced: Controller (B26), which was a housing, two glass and three elite circuits. EV matches the Retort's controller tier in the roster |

```
 E  P  E
 C  M  C
 W  S  W
```

| Key | Item |
|---|---|
| `E` | `ItemList.Emitter_EV` (the heat-ray source) |
| `P` | `ItemList.Electric_Pump_EV` (fluid handling) |
| `C` | `OrePrefixes.circuit.get(Materials.EV)` |
| `M` | Reaction Chamber Casing |
| `W` | `OrePrefixes.cableGt01.get(Materials.Gold)` (as the EV Vacuum Freezer) |
| `S` | `ItemList.Sensor_EV` (reads intensity) |

### Heat Ray Focus (1 recipe)

The non-consumed input of every Reaction Chamber recipe. Techguns "Reaction Chamber Heat Ray Focus" (shared item 98, recipe R30: `cwc` / `plp` / `plp`). In [components-and-materials.md](../06-items/components-and-materials.md) it is a "Keep" component of the chamber, so it is defined here and area 06 should link to this sheet.

| Field | Value |
|---|---|
| Output | Heat Ray Focus x1 |
| Tier | EV |
| Method | `RecipeMaps.assemblerRecipes` |
| Inputs | `circuit.EV` x2, `ItemList.Emitter_EV` x1, `wireFine.Gold` x1, `plate.Titanium` x4, vanilla Redstone Lamp (`Blocks.redstone_lamp`) x2, programmed circuit 1 |
| EU/t, time | `TierEU.RECIPE_EV`, 20 s |
| Gate | `plate.Titanium`, `Emitter_EV` and EV voltage |
| Notes | `circuitElite` is `circuit.EV`; `plateSteel` is `plate.Titanium` for the EV gate; the emitter is the heat-ray source that Techguns left implicit |

### Hatches (no GregGuns recipe)

Every hatch is a GT hatch with GT's own recipe. The chamber needs these ([GGS-08-17](reaction-chamber-multiblock.md#requirements)):

| Hatch | `ItemList` entry | Tier needed |
|---|---|---|
| Energy Hatch | `Hatch_Energy_EV` or higher | EV for the Laser Focus recipe, IV for the Anti Gravity Core |
| Input Bus | `Hatch_Input_Bus_LV` and up | Any. It holds the non-consumed Heat Ray Focus and the inputs |
| Output Bus | `Hatch_Output_Bus_LV` and up | Any |
| Input Hatch | `Hatch_Input_LV` and up | Any, as long as it takes the recipe's fluid amount |
| Maintenance Hatch | `Hatch_Maintenance` | n/a |
| Muffler Hatch | `Hatch_Muffler_LV` and up | Any |

The structure's glass ring is BartWorks borosilicate glass of any tier (`BorosilicateGlass.ofBoroGlassAnyTier()`); GT makes it.

## Machine build recipes

Every machine below is crafted in the crafting table around `ItemList.Hull_<tier>` and tier parts, with no tool letter and the same flags as GT's own machines (`BITS`, `DO_NOT_CHECK_FOR_COLLISIONS`). Casings and segment blocks are Assembler recipes in the style of GT's casings. The `W` cable per tier follows GT's own tier table, and the hull carries the tier material: GT builds `Casing_LV` to `Casing_IV` from 8 plates of Steel, Aluminum, Stainless Steel, Titanium and Tungstensteel ([AssemblerRecipes.java#L2589-L2640](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2589-L2640)), which are exactly the [0001](../../design/0001-tier-map.md) gate materials. So no machine is craftable below its tier ([0010](../../design/0010-material-remap-and-tier-additions.md) decision 1).

| Tier | Hull | Circuit `C` | `W` cable | Motor | Piston | Conveyor |
|---|---|---|---|---|---|---|
| LV | `Hull_LV` | `circuit.LV` | `cableGt01.Tin` | `Electric_Motor_LV` | `Electric_Piston_LV` | `Conveyor_Module_LV` |
| MV | `Hull_MV` | `circuit.MV` | `cableGt01.AnyCopper` | `Electric_Motor_MV` | `Electric_Piston_MV` | `Conveyor_Module_MV` |
| HV | `Hull_HV` | `circuit.HV` | `cableGt01.Gold` | `Electric_Motor_HV` | `Electric_Piston_HV` | `Conveyor_Module_HV` |
| EV | `Hull_EV` | `circuit.EV` | `cableGt01.Aluminium` | `Electric_Motor_EV` | `Electric_Piston_EV` | `Conveyor_Module_EV` |
| IV | `Hull_IV` | `circuit.IV` | `cableGt01.Tungsten` | `Electric_Motor_IV` | `Electric_Piston_IV` | `Conveyor_Module_IV` |

Precedents (all in the clone at `reference/src/GT5-Unofficial@5.09.54.205/`):

- **Press, Assembler and other tiered single blocks.** GT's Forming Press is `"WPW" / "CMC" / "WPW"` with hull, piston, circuit and wire at LV to IV ([MTERecipeLoader.java#L1057-L1083](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L1057-L1083)). Its Assembler adds conveyors and robot arms ([#L175-L204](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L175-L204)). The cable-per-tier table is in the Brewery recipes ([#L3351-L3380](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3351-L3380)).
- **Steam machine.** The Bronze Compressor is `"XXX" / "PMP" / "XXX"` with `Hull_Bronze`, small bronze pipes and a vanilla-ore-dict piston ([#L2849-L2853](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L2849-L2853)).
- **Multiblock controller with a hull.** The Assembly Line controller is `Hull_IV` with four casings, two circuits and a robot arm ([#L3767-L3771](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3767-L3771)). The Large Chemical Reactor adds a rotor and pipes ([#L3827-L3834](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3827-L3834)).
- **Casings.** 6 plates and a frame of the same metal, programmed circuit 1, `RECIPE_LV / 2` ([AssemblerRecipes.java#L2671-L2700](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2671-L2700)).

### Ammo Press, Steam (1 recipe)

| Field | Value |
|---|---|
| Output | Ammo Press, Steam (MTE 20100) x1 |
| Tier | Steam / ULV (Bronze) |
| Method | Hand, shaped, GT machine style |
| Gate | `Hull_Bronze`, `pipeSmall.Bronze`, a piston. Bronze is the Steam gate material ([0001](../../design/0001-tier-map.md)) |
| Notes | The Stone Bullet is the only ULV ammo ([ammo-press.md](ammo-press.md#tiers-and-what-each-runs)) |

```
 X  X  X
 P  M  P
 B  B  B
```

| Key | Item |
|---|---|
| `X` | `OrePrefixes.pipeSmall.get(Materials.Bronze)` |
| `P` | `OreDictNames.craftingPiston` (as the Bronze Compressor) |
| `M` | `ItemList.Hull_Bronze` |
| `B` | `OrePrefixes.plate.get(Materials.Bronze)` (the feed ramp) |

### Ammo Press, LV to EV (4 recipes)

One grid, four tiers from the table above. IDs 20101 (LV), 20102 (MV), 20103 (HV), 20104 (EV).

| Field | Value |
|---|---|
| Output | Ammo Press of the tier x1 |
| Tier | LV, MV, HV, EV |
| Method | Hand, shaped, GT machine style |
| Gate | The tier's `Hull`, `Electric_Piston` and `circuit`. Hull metal is Steel, Aluminum, Stainless Steel, Titanium |
| Notes | Adds the conveyor (feed belt) and motor (ram drive) to the Forming Press layout, because the press feeds rounds along a belt ([ammo-press.md](ammo-press.md#animation)) |

```
 V  P  V
 C  M  C
 W  E  W
```

| Key | Item |
|---|---|
| `V` | `ItemList.Conveyor_Module_<tier>` |
| `P` | `ItemList.Electric_Piston_<tier>` |
| `C` | `OrePrefixes.circuit.get(Materials.<tier>)` |
| `M` | `ItemList.Hull_<tier>` |
| `W` | `cableGt01` of the tier (Tin, AnyCopper, Gold, Aluminum) |
| `E` | `ItemList.Electric_Motor_<tier>` |

### Ammunition Works (3 recipes)

Controller tier HV (Stainless Steel), hatches LV to IV ([multiblocks.md](multiblocks.md#ammunition-works)). The item pipe casing belt and the hatches are GT's. Works Casing covers the feeder `F`, plain `A` and packing `O` positions: they differ only by which hatches the structure allows, not by block.

| Output | Tier | Method | Inputs | Circuit | EU/t, time | Notes |
|---|---|---|---|---|---|---|
| Works Casing x2 | HV | `assemblerRecipes` | `plate.StainlessSteel` x6, `frameGt.StainlessSteel` x1 | 1 | `TierEU.RECIPE_LV / 2`, 2 s 10 ticks | The default hall needs 313 or more casings; one per craft would cost about 1,900 plates, so this yields two. A proposal |
| Ram Housing x1 | HV | `assemblerRecipes` | Works Casing x1, `Electric_Piston_HV` x1, `gearGt.StainlessSteel` x1 | 2 | `TierEU.RECIPE_HV`, 10 s | Active texture is animated |

Ammunition Works controller (MTE 20105), hand-crafted at HV, shaped as GT's Assembly Line controller. Gate: `Hull_HV` and the Stainless Steel casings.

```
 W  P  W
 E  M  E
 W  V  W
```

| Key | Item |
|---|---|
| `W` | Works Casing |
| `P` | `ItemList.Electric_Piston_HV` |
| `E` | `OrePrefixes.circuit.get(Materials.HV)` |
| `M` | `ItemList.Hull_HV` |
| `V` | `ItemList.Conveyor_Module_HV` |

### Reaction Chamber, single block (2 recipes)

EV (MTE 20106) and IV (MTE 20107), [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#single-block-and-multiblock-forms).

| Field | Value |
|---|---|
| Output | Reaction Chamber of the tier x1 |
| Tier | EV, IV |
| Method | Hand, shaped, GT machine style |
| Gate | `Hull_EV` or `Hull_IV`, `Emitter_<tier>`, `Electric_Pump_<tier>`, `Sensor_<tier>` and `circuit.<tier>`. Only the IV chamber can start the Anti Gravity Core ([GGS-08-46](reaction-chamber-multiblock.md#requirements)) |
| Notes | Same three tier parts as the Retort controller. Cable `W` is Aluminum (EV) and Tungsten (IV) |

```
 E  P  E
 C  M  C
 W  S  W
```

| Key | Item |
|---|---|
| `E` | `ItemList.Emitter_<tier>` |
| `P` | `ItemList.Electric_Pump_<tier>` |
| `C` | `OrePrefixes.circuit.get(Materials.<tier>)` |
| `M` | `ItemList.Hull_<tier>` |
| `W` | `cableGt01` Aluminum (EV) or Tungsten (IV) |
| `S` | `ItemList.Sensor_<tier>` |

### Retort

Covered by the [Retort controller](#retort-controller-1-recipe) and [Reaction Chamber Casing](#reaction-chamber-casing-1-recipe) recipes above. No further casing is GregGuns-made.

## Processing recipes (`gregguns.recipe.reactionchamber`)

The multiblock sheet gives the rules and the survival decisions for the Techguns recipes but leaves the exact fluids, amounts, `eut` and duration open ("chosen when the recipes are written"). They are chosen here. Each recipe carries four metadata keys from [GGS-08-18](reaction-chamber-multiblock.md#requirements): `PREFERRED_INTENSITY`, `INTENSITY_MARGIN`, `REQUIRED_COMPLETION` and `RISK`.

How a Techguns row converts: duration is *reaction ticks x 60* game ticks. Required completion, preferred intensity and margin carry over unchanged. The Techguns fluid level (buckets) is dropped. The fluid amount moves to GT units (a multiple of 144 L, one ingot). Cost in RF becomes `eut` at the tier constant that gates the recipe.

### Laser Focus (EV)

| Field | Value |
|---|---|
| Output | Laser Focus x1 (the EV gun component, [components-and-materials.md](../06-items/components-and-materials.md)) |
| Tier | EV |
| Techguns row | 2 (Diamond, Heat Ray, Liquid Redstone, Laser Focus) |
| Inputs | `gem.Diamond` x1; Heat Ray Focus x1 (non-consumed, stack size 0) |
| Fluid | `Materials.Redstone.getMolten(1_152)`. Precedent for the fluid: [AssemblerRecipes.java#L1498-L1503](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L1498-L1503). Techguns used 4 000 mB of Liquid Redstone; 8 ingots keeps the cost of the same order in GT units |
| EU/t, duration | `TierEU.RECIPE_EV`, 600 ticks (10 reaction ticks, 30 s) |
| Metadata | `PREFERRED_INTENSITY` 3, `INTENSITY_MARGIN` 2, `REQUIRED_COMPLETION` 5, `RISK` BREAK_ITEM |
| Gate | EV Energy Hatch (the `eut` is above anything lower), the Heat Ray Focus, `gem.Diamond` |
| Notes | Fixes nothing in Techguns; the drift and completion rules are in [the multiblock sheet](reaction-chamber-multiblock.md#integration-points) |

### Anti Gravity Core (IV)

| Field | Value |
|---|---|
| Output | Anti Gravity Core x1 |
| Tier | IV (the device it feeds is IV in [0001](../../design/0001-tier-map.md)) |
| Techguns row | 6 (Nether Star, Heat Ray, Liquid Ender, Anti Gravity Core) |
| Inputs | `gem.NetherStar` x1 (the vanilla Nether Star through the unificator, [LoaderGTOreDictionary.java#L74](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/preload/LoaderGTOreDictionary.java#L74)); Heat Ray Focus x1 (non-consumed) |
| Fluid | `Materials.Enderium.getMolten(1_152)`. See the note below |
| EU/t, duration | `TierEU.RECIPE_IV`, 600 ticks (10 reaction ticks, 30 s) |
| Metadata | `PREFERRED_INTENSITY` 8, `INTENSITY_MARGIN` 1, `REQUIRED_COMPLETION` 7, `RISK` EXPLOSION_LOW |
| Gate | IV Energy Hatch, the Nether Star, molten Enderium |
| Notes | The failure blast is `doExplosion(V[1])` on the controller only ([reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#integration-points)) |

**The "Liquid Ender" fluid.** [fluids.md](../06-items/fluids.md) proposes `Materials.Ender.getFluid(...)`. In the pinned GT, `Materials.Ender` is built with dust items only, so it has no fluid ([MaterialsInit.java#L2502-L2507](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L2502-L2507)). `Materials.Enderium` is a metal with ingots ([MaterialsInit.java#L13230-L13244](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L13230-L13244)), so it has a molten form. This sheet uses it; the fluids sheet should follow.

### Other Techguns recipes

| Techguns row | Decision here | Reason |
|---|---|---|
| 1 Wheat Seeds | Dropped | [Multiblock sheet](reaction-chamber-multiblock.md#recipe-map-and-the-7-techguns-recipes) |
| 3 Titanium Iron Ore | Dropped | GT ore system |
| 4 Quartz Rod to Blaze Rod | Dropped | No GregGuns recipe needs a Quartz Rod, so there is no Quartz Rod item either |
| 5 Redstone block to Glowstone | Dropped | GT makes it |
| 7 Yellowcake to Enriched Uranium | **Deferred to M5** | Depends on the nuclear items ([reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#open-questions)). It will use GT uranium dust and a `RAD_*` risk |

## Requirements

Numbering continues from [recipe-scripting.md](recipe-scripting.md).

- **GGS-08-39.** The Reaction Chamber Casing, controller and Heat Ray Focus recipes exist as specified here and resolve at boot ([GGS-06-11](../06-items/components-and-materials.md#requirements)).
- **GGS-08-40.** The controller's recipe needs one `Emitter_EV`, one `Sensor_EV`, one `Electric_Pump_EV` and two `circuit.EV`. The dev self-check rejects a lower-tier substitute.
- **GGS-08-41.** `gregguns.recipe.reactionchamber` contains exactly the Laser Focus and Anti Gravity Core recipes until M5. Both have the four metadata keys and a non-consumed Heat Ray Focus.
- **GGS-08-42.** The Laser Focus recipe's `eut` is `TierEU.RECIPE_EV` and the Anti Gravity Core recipe's is `TierEU.RECIPE_IV`. With only an EV Energy Hatch the second recipe does not start.
- **GGS-08-43.** The fluid for each recipe is looked up through `Materials.<m>.getMolten`. A `null` result skips the recipe with one logged warning; it never falls back to lava, as Techguns did.
- **GGS-08-44.** No GregGuns hatch exists. The structure accepts only GT hatches.
- **GGS-08-45.** Every output of a Reaction Chamber recipe has a 0001 tier of EV or above, and no recipe is craftable below it.
- **GGS-08-85.** An Ammo Press recipe exists for each of the five tiers (Steam, LV, MV, HV, EV, MTE IDs 20100 to 20104). The Steam one uses `Hull_Bronze`, `pipeSmall.Bronze` and a piston. The LV to EV ones use the same grid, with that tier's hull, circuit, motor, piston and conveyor.
- **GGS-08-86.** The Ammunition Works has three recipes: Works Casing, Ram Housing and the controller (MTE 20105). The controller needs one `Hull_HV` and four Works Casings.
- **GGS-08-87.** The single-block Reaction Chamber has an EV and an IV recipe, each with `Emitter`, `Electric_Pump` and `Sensor` of its own tier, and the hull of that tier.
- **GGS-08-88.** Every machine recipe in this sheet includes the `Hull_<tier>` (`Hull_Bronze` for Steam) of the machine's `mTier`, and the other tier parts of the same tier. The dev self-check rejects a lower-tier part.
- **GGS-08-89.** The Works Casing recipe yields two blocks and every other casing recipe in this sheet yields what its table says. The Retort has no casing recipe beyond the Reaction Casing.

## Tests

| Req | Verified by |
|---|---|
| 39, 43 | Unit test of the recipe loader with a stubbed unificator and a material with no molten form |
| 40 | Unit test over the controller recipe's inputs |
| 41 | Unit test: the map holds two recipes, each with four metadata keys and one input of stack size 0 |
| 42 | `/gregguns test` scenario: run the Laser Focus recipe with an EV hatch, then try the Anti Gravity Core recipe with the same hatch and check it does not start |
| 44 | Unit test that no GregGuns hatch class is registered |
| 85, 86, 87 | Unit test over the recipe registry: one recipe per MTE ID, and each has the inputs named here |
| 88 | Dev self-check: parse each machine recipe, compare each part's tier with the MTE's `mTier` |
| 89 | Unit test of the Works Casing output count; doc test that the Retort recipe list is the Reaction Casing and the controller |
| 45 | Dev self-check over the recipe table |
| All | Manual: NEI shows the casing, controller and Heat Ray Focus recipes, and the two processing recipes under the Reaction Chamber tab ([nei-handlers.md](nei-handlers.md)) |

## Assumed components

| Component | Used in | Source |
|---|---|---|
| (none consumed from area 06) | n/a | Every input is a GT part or a vanilla item. The Works Casing and Ram Housing are produced and consumed within this sheet |

The recipes in this sheet **produce** components that other areas consume: Laser Focus (a gun component, area 06 and the laser weapons), Anti Gravity Core (area 05, [armor recipes](../05-armor/recipes.md#anti-gravity-device-1-recipe)) and Heat Ray Focus (this sheet).

## Open questions

- **Works Casing yield.** Two per craft is a proposal; 1:1 would match GT but costs about 1,900 Stainless Steel plates for a default hall. Balance pass.
- **Ammo Press and Reaction Chamber single-block grids.** These are the recipes' shapes, not tuned costs. The press adds a conveyor and motor to GT's Forming Press, which can be dropped if the cost proves too high.
- **Controller tier.** EV is chosen because the cheapest processing recipe is EV. If a later recipe needs a larger machine, a second controller tier needs a record.
- **Fluid amounts.** 1 152 L of molten redstone or Enderium is a proposal. Tune in M4 balance.
- **Yellowcake.** Deferred to M5 with the nuclear items.
- **Drift chance metadata.** The multiblock sheet turns Techguns' dead "instability" parameter into metadata ([MPRD-17](../../techguns-spec/08-machines-production/index.md#suspected-bugs)). The Techguns values (0 for the Laser Focus, 1 for the Anti Gravity Core) have no stated unit, so they are not carried here. The margin already controls drift.
