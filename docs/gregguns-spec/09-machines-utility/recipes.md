# Utility machine recipes

The recipes for the Turret and its armour plates, the Repair Bench, the Camo Bench and the two explosive charges.

- **Techguns baseline:** build recipes B04 to B09 and R58 to R61 in [06-items/recipes.md](../../techguns-spec/06-items/recipes.md); [turret.md](../../techguns-spec/09-machines-utility/turret.md), [repair-bench.md](../../techguns-spec/09-machines-utility/repair-bench.md), [camo-bench.md](../../techguns-spec/09-machines-utility/camo-bench.md), [explosive-charges.md](../../techguns-spec/09-machines-utility/explosive-charges.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md) (no machine tiers), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (Turret, Repair Bench, Camo Bench and charges kept as MTEs and blocks; charges tier-gated), [0003 energy model](../../design/0003-energy-model.md)
- **Sibling sheets:** [turret.md](turret.md), [repair-bench.md](repair-bench.md), [explosive-charges.md](explosive-charges.md); machine framework in [08](../08-machines-production/metatileentity-framework.md); Concrete from [07 recipes](../07-blocks/recipes.md)
- **Format:** [CONVENTIONS § Recipe sheets](../CONVENTIONS.md#recipe-sheets)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205)
- **Roadmap:** the utility-machines milestone in [ROADMAP](../../ROADMAP.md)

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| Machines are blocks of one `basicMachine` with iron plates, circuits and an Electric Engine | The Turret, Repair Bench and Camo Bench are GT MetaTileEntities. Their recipes start from `ItemList.Hull_<tier>` and tier parts, the way GT's own single-block machines are made | [0002](../../design/0002-machine-deconfliction.md) and [metatileentity-framework.md](../08-machines-production/metatileentity-framework.md) |
| No machine has a tier | Each kept machine gets one. **[0001](../../design/0001-tier-map.md) assigns none**, so the tiers below are this sheet's proposals | GT machines are tiered; the hull tier is the gate |
| `plateIron`, `circuitBasic`, Electric Engine, Mechanical Parts | `plate.<m>`, `circuit.<tier>`, `ItemList.Robot_Arm_<tier>`, `ItemList.Sensor_<tier>` | GT parts ([components-and-materials.md](../06-items/components-and-materials.md)) |
| Explosive Charge: TNT and rubber. Advanced: TGX and plastic | TNT and rubber plates in the Assembler at MV. Advanced adds glyceryl trinitrate and runs at HV | [0002](../../design/0002-machine-deconfliction.md) says tier-gated; TGX becomes the GT explosive set ([components-and-materials.md](../06-items/components-and-materials.md)) |
| Turret armour: Iron, Steel, Obsidian Steel, Carbon plates | Wrought iron, steel, aluminium, carbon-and-titanium plates | Obsidian Steel and carbon remap, as in [05 recipes](../05-armor/recipes.md#what-changes-from-techguns) |

## Integration points

### GregTech: single-block machines are made from a hull and tier parts

- **Pack config:** none.
- **Source:** GT crafts single-block machines in the crafting table around a hull, a tier circuit, a cable and tier parts, with no tool letter:
  - Miner, LV: `"EEE" / "WMW" / "CSC"` with `Hull_LV`, `Electric_Motor_LV`, `circuit.LV`, `cableGt01.Tin`, `Sensor_LV` ([MTERecipeLoader.java#L3382-L3388](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3382-L3388)).
  - Miner, HV: adds `Robot_Arm_HV`, `Electric_Piston_HV` ([#L3394-L3400](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3394-L3400)).
  - The hull itself: Casing plus cables in the crafting table, or cables plus casing and 2 `Polyethylene` ingots in the Assembler ([AssemblerRecipes.java#L2854-L2860](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2854-L2860)).
  - Items that are not machines but need a hull still use the Assembler, for example the LV Input Bus ([AssemblerRecipes.java#L3472-L3480](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L3472-L3480)).
- **GregGuns does:** the three MTEs are crafted by shaped recipes in the same style as GT's machines. The exception to "final step is a GT machine" in [CONVENTIONS](../CONVENTIONS.md#recipe-sheets) is for the machine blocks themselves, because GT does the same for its own. The consumables and parts (armour plates, charges) use the Assembler.
- **Pack-side change:** none.

## Machines

All three are `MTETieredMachineBlock` subclasses ([metatileentity-framework.md](../08-machines-production/metatileentity-framework.md#gt5-unofficial-base-classes-to-extend)) with MTE IDs from the GregGuns block (20100 to 20163).

### Turret (1 recipe)

| Field | Value |
|---|---|
| Output | Turret (MTE) x1 |
| Tier | MV (proposal). A turret fires player-owned guns from the whole range, so the tier must support MV guns ([0001](../../design/0001-tier-map.md): SMG, M4, AK47 at MV) |
| Method | Hand, shaped, GT machine style |
| Inputs | See grid |
| Gate | `Hull_MV`, `plate.Aluminium` (MV gate material), `Robot_Arm_MV` |
| Notes | Replaces B04 (`pip` / `cec` / `prp`: iron plates, circuit, concrete, engine, redstone). The Concrete is GregGuns' own block from [07 recipes](../07-blocks/recipes.md#concrete) as ballast. The Electric Engine becomes the robot arm that aims the gun. Techguns' unused key `b` is not carried |

```
 P  S  P
 C  M  C
 K  R  K
```

| Key | Item |
|---|---|
| `P` | `OrePrefixes.plate.get(Materials.Aluminium)` |
| `S` | `ItemList.Sensor_MV` (target detection) |
| `C` | `OrePrefixes.circuit.get(Materials.MV)` |
| `M` | `ItemList.Hull_MV` |
| `K` | GregGuns Concrete (`concrete:0`) |
| `R` | `ItemList.Robot_Arm_MV` (aiming) |

### Repair Bench (1 recipe)

| Field | Value |
|---|---|
| Output | Repair Bench (MTE) x1 |
| Tier | LV (proposal) |
| Method | Hand, shaped, GT machine style |
| Gate | `Hull_LV`, `plate.Steel` (LV gate material) |
| Notes | Replaces B07 (`pmp` / `ici` / `iii`: iron plates, Mechanical Parts, crafting table, iron nuggets). The bench's repair inputs follow [repair-bench.md](repair-bench.md). Whether it draws EU is open (see Open questions); the recipe and tier are the same either way, except that a no-EU bench would use a `Casing_LV` in place of the hull |

```
 P  R  P
 W  M  W
 C  T  C
```

| Key | Item |
|---|---|
| `P` | `OrePrefixes.plate.get(Materials.Steel)` |
| `R` | `ItemList.Robot_Arm_LV` (replaces Mechanical Parts) |
| `W` | `OrePrefixes.cableGt01.get(Materials.Tin)` |
| `M` | `ItemList.Hull_LV` |
| `C` | `OrePrefixes.circuit.get(Materials.LV)` |
| `T` | vanilla Crafting Table (`Blocks.crafting_table`) |

### Camo Bench (1 recipe)

| Field | Value |
|---|---|
| Output | Camo Bench (MTE) x1 |
| Tier | LV (proposal). Cosmetic only, so the lowest GT tier that has hulls |
| Method | Hand, shaped, GT machine style |
| Gate | `Hull_LV`, `circuit.LV` |
| Notes | Replaces B05 (`ddd` / `ici` / `iii`: three dyes, crafting table, iron nuggets, circuit) |

```
 D  D  D
 W  M  W
 C  T  C
```

| Key | Item |
|---|---|
| `D` | any dye (ore name `dye`, three stacks of any colour) |
| `W` | `OrePrefixes.cableGt01.get(Materials.Tin)` |
| `M` | `ItemList.Hull_LV` |
| `C` | `OrePrefixes.circuit.get(Materials.LV)` |
| `T` | vanilla Crafting Table |

## Turret armour plates (4 recipes)

The slot-19 item of the turret ([turret.md](../../techguns-spec/09-machines-utility/turret.md#armour)). Armour value and base texture stay as in Techguns (5, 10, 15, 20). [components-and-materials.md](../06-items/components-and-materials.md) assigns "turret armour" to this area. The Techguns shape is a T of five plates: `ppp` / `_p_` / `_p_`.

| Output (armour value) | Techguns | Tier | Method | Inputs | Circuit | EU/t, time |
|---|---|---|---|---|---|---|
| Iron Turret Armor (5) | R58, `plateIron` | Steam / ULV | Hand, shaped `PPP` / `_P_` / `_P_` | `plate.WroughtIron` x5 | n/a | n/a |
| Steel Turret Armor (10) | R59, `plateSteel` | LV | Assembler | `plate.Steel` x5 | 1 | `TierEU.RECIPE_LV`, 10 s |
| Aluminium Turret Armor (15) | R60, `plateObsidianSteel` | MV | Assembler | `plate.Aluminium` x5 | 1 | `TierEU.RECIPE_MV`, 10 s |
| Carbon Turret Armor (20) | R61, `plateCarbon` | EV | Assembler | `plateAlloy.Carbon` x3, `plate.Titanium` x2 | 1 | `TierEU.RECIPE_EV`, 10 s |

Notes:
- The third plate is renamed from "Obsidian Steel" because there is no such GT material; the lang key and registry name change with it. This follows the remap proposed in [05 recipes](../05-armor/recipes.md#open-questions).
- The Iron plate is wrought iron, which is the Steam/ULV gate material ([0001](../../design/0001-tier-map.md)). GT's own hand-made armour is the precedent: [CraftingRecipeLoader.java#L350-L368](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/CraftingRecipeLoader.java#L350-L368).
- The carbon plate is IC2's, unified by GT ([LoaderGTOreDictionary.java#L55](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/preload/LoaderGTOreDictionary.java#L55)) and used by GT in the GraviSuite recipes ([CraftingRecipeLoader.java#L1909-L1923](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/CraftingRecipeLoader.java#L1909-L1923)).

## Explosive charges (2 recipes)

Both are charge blocks (`basicMachine:6` and `:8` in Techguns) placed on a face and configured in a GUI. Hardness caps are 30 and 100 ([explosive-charges.md](../../techguns-spec/09-machines-utility/explosive-charges.md#parameters)). [0002](../../design/0002-machine-deconfliction.md) says "tier-gated" but names no tier; the gate below is this sheet's proposal (see Open questions).

| Output | Techguns | Tier | Method | Inputs | Circuit | EU/t, time |
|---|---|---|---|---|---|---|
| Explosive Charge x8 | B08 (`rtr` / `tct` / `rtr`: 4 rubber, 4 TNT, 1 circuit) | MV | `assemblerRecipes` | vanilla TNT (`Blocks.tnt`) x4, `plate.Rubber` x4, `circuit.MV` x1, `plate.Aluminium` x1 | 1 | `TierEU.RECIPE_MV`, 20 s |
| Advanced Explosive Charge x4 | B09 (`rtr` / `tct` / `rtr`: 4 plastic sheets, 4 TGX, 1 circuit) | HV | `assemblerRecipes` | vanilla TNT x4, `plate.Polyethylene` x4, `circuit.HV` x1, `plate.StainlessSteel` x1; fluid `Materials.Glyceryl.getFluid(1_000)` | 2 | `TierEU.RECIPE_HV`, 20 s |

Gate: the Aluminium plate (MV) and the Stainless steel plate with glyceryl trinitrate (HV). Glyceryl trinitrate is a GT chemistry fluid ([MaterialsInit.java#L6478-L6490](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L6478-L6490)), so the advanced charge needs the chemical line that produces it. The Assembler precedent for a plate-and-circuit recipe with a fluid is the Plascrete block ([AssemblerRecipes.java#L606-L614](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L606-L614)).

## Not carried over

| Techguns recipe | Reason |
|---|---|
| B10 Charging Station | Dropped ([0002](../../design/0002-machine-deconfliction.md)) |
| B16 to B20 Ore Drill parts | Dropped ([0002](../../design/0002-machine-deconfliction.md)) |
| R22 Machine Upgrade (Stack Operation) | Belongs to dropped machines |
| Scrapper | Dropped ([0002](../../design/0002-machine-deconfliction.md)) |
| Camp Flag, Bio Blob | No Techguns recipe |

## Requirements

Numbering continues from [repair-bench.md](repair-bench.md).

- **GGS-09-26.** The Turret, Repair Bench and Camo Bench recipes exist as specified here and resolve at boot. A `null` GT part skips that recipe with one logged warning ([GGS-06-11](../06-items/components-and-materials.md#requirements)).
- **GGS-09-27.** Each machine recipe includes `ItemList.Hull_<tier>` for the machine's tier, and each MTE's `mTier` equals that tier.
- **GGS-09-28.** No machine, plate or charge recipe uses a Techguns plate, circuit or engine item ([GGS-06-10](../06-items/components-and-materials.md#requirements)).
- **GGS-09-29.** The Steel, Aluminium and Carbon turret armour recipes are Assembler recipes at `TierEU.RECIPE_<tier>` of their row. The Iron plate is the only turret armour made in the crafting table.
- **GGS-09-30.** Each turret armour recipe has an input from its tier's gate material (wrought iron, steel, aluminium, titanium).
- **GGS-09-31.** The Explosive Charge recipe runs at `TierEU.RECIPE_MV` or above and the Advanced Explosive Charge recipe at `TierEU.RECIPE_HV` or above. Neither uses a fluid that can be had below the tier.
- **GGS-09-32.** The charge recipes use only vanilla TNT and GT parts. They do not call a Techguns explosive item.

## Tests

| Req | Verified by |
|---|---|
| 26 | Unit test of the recipe loader with a stubbed unificator that returns `null` |
| 27 | Unit test over the MTE registry against the recipe inputs |
| 28 | Unit test that scans inputs for removed Techguns item names |
| 29, 30, 31 | Dev self-check: unit test over the recipe registry against the tier table |
| 32 | Unit test over the charge recipe inputs |
| All | Manual: NEI shows each recipe under the crafting grid or the Assembler |

## Assumed components

| Component | Used in | Source |
|---|---|---|
| Concrete (`concrete:0`) | Turret | [07 recipes](../07-blocks/recipes.md#concrete) (Mixer recipe), block from [decorative-blocks.md](../07-blocks/decorative-blocks.md) |

No component from area 06 is consumed. The turret armour plates (Iron, Steel, Aluminium, Carbon) are defined in this sheet because [components-and-materials.md](../06-items/components-and-materials.md) assigns them to area 09.

## Open questions

- **Machine tiers.** [0001](../../design/0001-tier-map.md) lists items only. Turret MV, Repair Bench LV, Camo Bench LV are proposals; a line in 0001 or a short record settles them.
- **Repair Bench EU.** Already open in [repair-bench.md](repair-bench.md#open-questions). If it draws no EU, its hull recipe becomes a casing recipe.
- **Charge tiers.** MV and HV are proposals. The overlap with GT mining explosives is already open in [explosive-charges.md](explosive-charges.md#open-questions); a balance pass would also confirm whether Glyceryl is the right HV gate.
- **Turret armour name.** "Aluminium Turret Armor" replaces "Obsidian Steel Turret Armor". A name decision is part of the material remap.
