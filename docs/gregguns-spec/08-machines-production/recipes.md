# Reaction Chamber recipes

How the Reaction Chamber is built, and the recipes it runs. This is the only kept production machine ([0002](../../design/0002-machine-deconfliction.md)); the rest of area 08 is recipes in GT's own maps, listed in [recipe-map-replacements.md](recipe-map-replacements.md).

- **Techguns baseline:** [reaction-chamber.md](../../techguns-spec/08-machines-production/reaction-chamber.md) ([§ 5 recipe list](../../techguns-spec/08-machines-production/reaction-chamber.md#5-full-recipe-list-tgmachinerecipes)), build recipes B25 to B27 and R30 in [06-items/recipes.md](../../techguns-spec/06-items/recipes.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md) (UV Emitter and Laser Focus at EV, Anti Gravity Core at IV via the device), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md), [0003 energy model](../../design/0003-energy-model.md)
- **Sibling sheets:** [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md) (structure, rules, risks), [metatileentity-framework.md](metatileentity-framework.md)
- **Format:** [CONVENTIONS § Recipe sheets](../CONVENTIONS.md#recipe-sheets)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205), StructureLib (`structurelib`)
- **Roadmap:** M4

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| Housing x9 (B25), Glass x6 (B27), Controller (B26) built in the table from Cybernetic Parts, Mechanical Parts, elite circuits and a Chemical Laboratory | One **Reaction Chamber Casing** block (Assembler), GT glass for the ring, and a **controller** crafted from GT components around the casing | [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#what-changes-from-techguns): visible GT-style casings, GT hatches |
| Hatches are three of the housing blocks | GT hatches. No GregGuns hatch exists | The Input Bus, Output Bus, Input Hatch, Energy Hatch, Maintenance Hatch and Muffler Hatch of GT are used as they are |
| Heat Ray Focus in a GUI slot (R30) | Heat Ray Focus is a non-consumed (stack size 0) recipe input held in an Input Bus | GT's convention for moulds and lenses |
| Process cost in RF per reaction tick | `eut` at a `TierEU.RECIPE_*` value, which also gates the recipe by hatch voltage | Tier gating as in [0001](../../design/0001-tier-map.md) |
| Seven Techguns recipes | Two kept, one deferred, four dropped | [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#recipe-map-and-the-7-techguns-recipes) |

## Integration points

### GregTech: multiblock controllers are crafted around a casing

- **Pack config:** none.
- **Source:** GT's multiblock controllers are shaped crafting recipes with the controller's casing in the centre cell and a tier circuit, a cable and tier parts around it:
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

### Reaction Chamber controller (1 recipe)

| Field | Value |
|---|---|
| Output | Reaction Chamber controller (the MTE of [GGS-08-16](reaction-chamber-multiblock.md#requirements)) x1 |
| Tier | EV |
| Method | Hand, shaped, no tool letter. Same flags as GT's controllers (`BITS`, `DO_NOT_CHECK_FOR_COLLISIONS`) |
| Gate | `ItemList.Emitter_EV`, `ItemList.Sensor_EV`, `ItemList.Electric_Pump_EV` and `circuit.EV` |
| Notes | Techguns replaced: Controller (B26), which was a housing, two glass and three elite circuits |

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

## Tests

| Req | Verified by |
|---|---|
| 39, 43 | Unit test of the recipe loader with a stubbed unificator and a material with no molten form |
| 40 | Unit test over the controller recipe's inputs |
| 41 | Unit test: the map holds two recipes, each with four metadata keys and one input of stack size 0 |
| 42 | `/gregguns test` scenario: run the Laser Focus recipe with an EV hatch, then try the Anti Gravity Core recipe with the same hatch and check it does not start |
| 44 | Unit test that no GregGuns hatch class is registered |
| 45 | Dev self-check over the recipe table |
| All | Manual: NEI shows the casing, controller and Heat Ray Focus recipes, and the two processing recipes under the Reaction Chamber tab ([nei-handlers.md](nei-handlers.md)) |

## Assumed components

| Component | Used in | Source |
|---|---|---|
| (none consumed from area 06) | n/a | Every input is a GT part or a vanilla item |

The recipes in this sheet **produce** components that other areas consume: Laser Focus (a gun component, area 06 and the laser weapons), Anti Gravity Core (area 05, [armour recipes](../05-armor/recipes.md#anti-gravity-device-1-recipe)) and Heat Ray Focus (this sheet).

## Open questions

- **Controller tier.** EV is chosen because the cheapest processing recipe is EV. If a later recipe needs a larger machine, a second controller tier needs a record.
- **Fluid amounts.** 1 152 L of molten redstone or Enderium is a proposal. Tune in M4 balance.
- **Yellowcake.** Deferred to M5 with the nuclear items.
- **Drift chance metadata.** The multiblock sheet turns Techguns' dead "instability" parameter into metadata ([MPRD-17](../../techguns-spec/08-machines-production/index.md#suspected-bugs)). The Techguns values (0 for the Laser Focus, 1 for the Anti Gravity Core) have no stated unit, so they are not carried here. The margin already controls drift.
