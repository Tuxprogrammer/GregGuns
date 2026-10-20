# Utility machine recipes

The recipes for every tier of the Turret and the Repair Bench, the Camo Bench, the turret armor plates, the Armory Gantry and Rampart controllers and casings, the Gun Mount Hatch and the two explosive charges.

- **Techguns baseline:** build recipes B04 to B09 and R58 to R61 in [06-items/recipes.md](../../techguns-spec/06-items/recipes.md); [turret.md](../../techguns-spec/09-machines-utility/turret.md), [repair-bench.md](../../techguns-spec/09-machines-utility/repair-bench.md), [camo-bench.md](../../techguns-spec/09-machines-utility/camo-bench.md), [explosive-charges.md](../../techguns-spec/09-machines-utility/explosive-charges.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md) (no machine tiers), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (Turret, Repair Bench, Camo Bench and charges kept as MTEs and blocks; charges tier-gated), [0003 energy model](../../design/0003-energy-model.md), [0010 material remap](../../design/0010-material-remap-and-tier-additions.md), [0011 machine policy](../../design/0011-machine-policy.md)
- **Sibling sheets:** [turret.md](turret.md), [repair-bench.md](repair-bench.md), [explosive-charges.md](explosive-charges.md); machine framework in [08](../08-machines-production/metatileentity-framework.md); Concrete from [07 recipes](../07-blocks/recipes.md)
- **Format:** [CONVENTIONS § Recipe sheets](../CONVENTIONS.md#recipe-sheets)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205)
- **Roadmap:** the utility-machines milestone in [ROADMAP](../../ROADMAP.md)

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| Machines are blocks of one `basicMachine` with iron plates, circuits and an Electric Engine | The Turret, Repair Bench and Camo Bench are GT MetaTileEntities. Their recipes start from `ItemList.Hull_<tier>` and tier parts, the way GT's own single-block machines are made | [0002](../../design/0002-machine-deconfliction.md) and [metatileentity-framework.md](../08-machines-production/metatileentity-framework.md) |
| No machine has a tier | Each kept machine gets one. [0001](../../design/0001-tier-map.md) assigns none; [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5 adopts the tiers below, which the [machine roster](../08-machines-production/machine-roster.md) also lists | GT machines are tiered; the hull tier is the gate |
| `plateIron`, `circuitBasic`, Electric Engine, Mechanical Parts | `plate.<m>`, `circuit.<tier>`, `ItemList.Robot_Arm_<tier>`, `ItemList.Sensor_<tier>` | GT parts ([components-and-materials.md](../06-items/components-and-materials.md)) |
| Explosive Charge: TNT and rubber. Advanced: TGX and plastic | TNT and rubber plates in the Assembler at MV. Advanced adds glyceryl trinitrate and runs at HV | [0002](../../design/0002-machine-deconfliction.md) says tier-gated; TGX becomes the GT explosive set ([components-and-materials.md](../06-items/components-and-materials.md)) |
| Turret armor: Iron, Steel, Obsidian Steel, Carbon plates | Wrought iron, steel, aluminum, `plate.EpoxidFiberReinforced` plus titanium plates | The tier material of the turret armor's tier replaces obsidian steel; carbon is `plate.EpoxidFiberReinforced` plus the tier material ([0010](../../design/0010-material-remap-and-tier-additions.md) decisions 1 and 2) |

## Integration points

### GregTech: single-block machines are made from a hull and tier parts

- **Pack config:** none.
- **Source:** GT crafts single-block machines in the crafting table around a hull, a tier circuit, a cable and tier parts, with no tool letter:
  - Miner, LV: `"EEE" / "WMW" / "CSC"` with `Hull_LV`, `Electric_Motor_LV`, `circuit.LV`, `cableGt01.Tin`, `Sensor_LV` ([MTERecipeLoader.java#L3382-L3388](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3382-L3388)).
  - Miner, HV: adds `Robot_Arm_HV`, `Electric_Piston_HV` ([#L3394-L3400](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3394-L3400)).
  - The hull itself: Casing plus cables in the crafting table, or cables plus casing and 2 `Polyethylene` ingots in the Assembler ([AssemblerRecipes.java#L2854-L2860](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2854-L2860)).
  - Items that are not machines but need a hull still use the Assembler, for example the LV Input Bus ([AssemblerRecipes.java#L3472-L3480](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L3472-L3480)).
- **GregGuns does:** the three MTEs are crafted by shaped recipes in the same style as GT's machines. The exception to "final step is a GT machine" in [CONVENTIONS](../CONVENTIONS.md#recipe-sheets) is for the machine blocks themselves, because GT does the same for its own. The consumables and parts (armor plates, charges) use the Assembler.
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
| `D` | any dye (ore name `dye`, three stacks of any color) |
| `W` | `OrePrefixes.cableGt01.get(Materials.Tin)` |
| `M` | `ItemList.Hull_LV` |
| `C` | `OrePrefixes.circuit.get(Materials.LV)` |
| `T` | vanilla Crafting Table |

### Tiered machines: parts per tier

The Turret, Repair Bench and Gun Mount Hatch recipes below share these tier parts. The hull carries the tier material (GT's `Casing_LV` to `Casing_IV` are 8 plates of Steel, Aluminum, Stainless Steel, Titanium and Tungstensteel, [AssemblerRecipes.java#L2589-L2640](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2589-L2640)). The cable per tier follows GT's Brewery and Miner recipes ([MTERecipeLoader.java#L3351-L3380](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3351-L3380)).

| Tier | Plate `P` (tier material) | Cable `W` | Hull | Circuit | Robot arm | Sensor | Recipe voltage |
|---|---|---|---|---|---|---|---|
| LV | `plate.Steel` | `cableGt01.Tin` | `Hull_LV` | `circuit.LV` | `Robot_Arm_LV` | `Sensor_LV` | `TierEU.RECIPE_LV` |
| MV | `plate.Aluminium` | `cableGt01.AnyCopper` | `Hull_MV` | `circuit.MV` | `Robot_Arm_MV` | `Sensor_MV` | `TierEU.RECIPE_MV` |
| HV | `plate.StainlessSteel` | `cableGt01.Gold` | `Hull_HV` | `circuit.HV` | `Robot_Arm_HV` | `Sensor_HV` | `TierEU.RECIPE_HV` |
| EV | `plate.Titanium` | `cableGt01.Aluminium` | `Hull_EV` | `circuit.EV` | `Robot_Arm_EV` | `Sensor_EV` | `TierEU.RECIPE_EV` |
| IV | `plate.TungstenSteel` | `cableGt01.Tungsten` | `Hull_IV` | `circuit.IV` | `Robot_Arm_IV` | `Sensor_IV` | `TierEU.RECIPE_IV` |

### Turret, HV to IV (3 recipes)

The MV turret is above. HV is MTE 20117, EV 20118, IV 20119. Same grid as the MV one, every part switched to the row of the table. The Concrete ballast stays the same GregGuns block. A turret fires guns up to its own tier ([machine-roster.md](../08-machines-production/machine-roster.md#roster)).

| Field | Value |
|---|---|
| Output | Turret (MTE) of the tier x1 |
| Tier | HV, EV, IV |
| Method | Hand, shaped, GT machine style |
| Gate | The tier's hull, plate, `Sensor` and `Robot_Arm` |

```
 P  S  P
 C  M  C
 K  R  K
```

Keys as for the MV Turret, with `P`, `S`, `C`, `M`, `R` from the tier table and `K` Concrete.

### Repair Bench, MV to IV (4 recipes)

The LV bench is above. MV is MTE 20110, HV 20111, EV 20112, IV 20113. Same grid as the LV one with the tier table's parts. A bench repairs items up to its own tier only ([GGS-08-50](../08-machines-production/machine-roster.md#requirements)); LuV and ZPM items need the [Armory Gantry](#armory-gantry-5-recipes) with a hatch of that tier.

| Field | Value |
|---|---|
| Output | Repair Bench (MTE) of the tier x1 |
| Tier | MV, HV, EV, IV |
| Method | Hand, shaped, GT machine style |
| Gate | The tier's hull, plate and `Robot_Arm` |

```
 P  R  P
 W  M  W
 C  T  C
```

Keys as for the LV Repair Bench, with `P`, `R`, `W`, `M`, `C` from the tier table and `T` the vanilla Crafting Table.

## Armory Gantry (5 recipes)

Multiblock, controller HV (MTE 20114, [multiblocks.md](../08-machines-production/multiblocks.md#armory-gantry)). The legs are GT `frameGt.Titanium`, the windows are GT glass and the hatches are GT's, so only the casings below and the controller are GregGuns recipes. All casings are HV (Stainless Steel) Assembler recipes.

| Output | Method | Inputs | Circuit | EU/t, time | Notes |
|---|---|---|---|---|---|
| Gantry Casing x1 | `assemblerRecipes` | `plate.StainlessSteel` x6, `frameGt.StainlessSteel` x1 | 7 | `TierEU.RECIPE_LV / 2`, 2 s 10 ticks | The GT casing recipe on circuit 7, since GT's own Stainless Steel casing takes circuit 1. Used by `R` |
| Gantry Beam x2 | `assemblerRecipes` | `stickLong.StainlessSteel` x3, `plate.StainlessSteel` x3 | 2 | `TierEU.RECIPE_HV`, 10 s | The rail across the top. Used by `B` |
| Gantry Hoist x1 | `assemblerRecipes` | Gantry Casing x1, `Electric_Motor_HV` x1, `gearGt.StainlessSteel` x2 | 2 | `TierEU.RECIPE_HV`, 15 s | The crane head on the beam. Used by `H` |
| Repair Cradle x2 | `assemblerRecipes` | Gantry Casing x1, `Electric_Piston_HV` x1, `plate.Rubber` x2 | 3 | `TierEU.RECIPE_HV`, 10 s | The pad under each bay. Used by `T` |

Armory Gantry controller (hand, shaped, HV, GT machine style). Gate: `Hull_HV`, `Robot_Arm_HV`, Gantry Hoist.

```
 A  H  A
 C  M  C
 W  S  W
```

| Key | Item |
|---|---|
| `A` | `ItemList.Robot_Arm_HV` |
| `H` | Gantry Hoist |
| `C` | `OrePrefixes.circuit.get(Materials.HV)` |
| `M` | `ItemList.Hull_HV` |
| `W` | `OrePrefixes.cableGt01.get(Materials.Gold)` |
| `S` | `ItemList.Sensor_HV` (reads item damage) |

The Gantry's repair tier is set by its Energy Hatch, not by any recipe ([multiblocks.md](../08-machines-production/multiblocks.md#hatches-and-tier-mechanic)).

## Rampart and Gun Mount Hatch (9 recipes)

Multiblock, controller HV (MTE 20120, [multiblocks.md](../08-machines-production/multiblocks.md#rampart)).

### Bastion Plating (4 recipes)

One block with four metas. Each consumes the matching **turret armor plate** from [the plates above](#turret-armor-plates-4-recipes), as [multiblocks.md](../08-machines-production/multiblocks.md#hatches-controller-mounts) asks. One plate makes four blocks, because the default Rampart is about 190 plating blocks and one plate per block would cost five plates each. Concrete is the ballast, as in the Turret.

| Output | Tier | Method | Inputs | Circuit | EU/t, time | Armor value |
|---|---|---|---|---|---|---|
| Bastion Plating, Iron x4 | Steam / ULV item, made at LV | `assemblerRecipes` | Iron Turret Armor x1, Concrete x2 | 1 | `TierEU.RECIPE_LV`, 10 s | 5 |
| Bastion Plating, Steel x4 | LV | `assemblerRecipes` | Steel Turret Armor x1, Concrete x2 | 2 | `TierEU.RECIPE_LV`, 10 s | 10 |
| Bastion Plating, Aluminum x4 | MV | `assemblerRecipes` | Aluminum Turret Armor x1, Concrete x2 | 3 | `TierEU.RECIPE_MV`, 10 s | 15 |
| Bastion Plating, Carbon x4 | EV | `assemblerRecipes` | Carbon Turret Armor x1, Concrete x2 | 10 | `TierEU.RECIPE_EV`, 10 s | 20 |

The armor values are the Techguns ones ([turret.md](../../techguns-spec/09-machines-utility/turret.md#armor)). The Iron meta needs an Assembler, so its recipe runs at LV even though the plate is a Steam item; the Rampart itself needs an HV controller, so this does not let a Steam player build one. The plate's own recipe carries the tier material (wrought iron, steel, aluminum, titanium with fiber-reinforced epoxy), so the plating recipes do not name a plate again.

### Rampart controller (1 recipe)

| Field | Value |
|---|---|
| Output | Rampart controller (MTE 20120) x1 |
| Tier | HV |
| Method | Hand, shaped, GT machine style |
| Gate | `Hull_HV`, `Robot_Arm_HV`, `Sensor_HV` |
| Notes | A turret wall is the Turret's recipe made large, so it keeps the Turret's Concrete and Sensor and adds Steel Bastion Plating, the lowest plating that still counts as armor |

```
 R  S  R
 C  M  C
 B  K  B
```

| Key | Item |
|---|---|
| `R` | `ItemList.Robot_Arm_HV` |
| `S` | `ItemList.Sensor_HV` |
| `C` | `OrePrefixes.circuit.get(Materials.HV)` |
| `M` | `ItemList.Hull_HV` |
| `B` | Bastion Plating, Steel |
| `K` | GregGuns Concrete (`concrete:0`) |

### Gun Mount Hatch, MV to IV (4 recipes)

MV is MTE 20121, HV 20122, EV 20123, IV 20124. A hatch is not a machine, so it follows GT's hatch recipes, which are Assembler recipes around the tier's hull ([AssemblerRecipes.java#L3471-L3480](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L3471-L3480)).

| Field | Value |
|---|---|
| Output | Gun Mount Hatch of the tier x1 |
| Tier | MV, HV, EV, IV |
| Method | `assemblerRecipes` |
| Inputs | `Hull_<tier>` x1, `Robot_Arm_<tier>` x1, `Sensor_<tier>` x1, `plate.<tier material>` x2 (Aluminum, Stainless Steel, Titanium, Tungstensteel) |
| Circuit | 1 |
| EU/t, time | `TierEU.RECIPE_<tier>`, 15 s |
| Gate | The tier's hull and plate. The hatch's tier is the highest gun tier the mount takes ([GGS-08-76](../08-machines-production/multiblocks.md#requirements-3)) |
| Notes | The GT bus recipe also takes a Glue fluid; the mount hatch takes none (a proposal) |

## Turret armor plates (4 recipes)

The slot-19 item of the turret ([turret.md](../../techguns-spec/09-machines-utility/turret.md#armor)). Armor value and base texture stay as in Techguns (5, 10, 15, 20). [components-and-materials.md](../06-items/components-and-materials.md) assigns "turret armor" to this area. The Techguns shape is a T of five plates: `ppp` / `_p_` / `_p_`.

| Output (armor value) | Techguns | Tier | Method | Inputs | Circuit | EU/t, time |
|---|---|---|---|---|---|---|
| Iron Turret Armor (5) | R58, `plateIron` | Steam / ULV | Hand, shaped `PPP` / `_P_` / `_P_` | `plate.WroughtIron` x5 | n/a | n/a |
| Steel Turret Armor (10) | R59, `plateSteel` | LV | Assembler | `plate.Steel` x5 | 14 | `TierEU.RECIPE_LV`, 10 s |
| Aluminum Turret Armor (15) | R60, `plateObsidianSteel` | MV | Assembler | `plate.Aluminium` x5 | 15 | `TierEU.RECIPE_MV`, 10 s |
| Carbon Turret Armor (20) | R61, `plateCarbon` | EV | Assembler | `plate.EpoxidFiberReinforced` x3, `plate.Titanium` x2 | 12 | `TierEU.RECIPE_EV`, 10 s |

Notes:
- Built deviation (M6 utility core): in GT 5.09.54.205 `Materials.WroughtIron` is an alias of Cast Iron and `plateWroughtIron` has no items, so the Iron plate takes `plate.CastIron` x5 (the same substitution the Miner armor uses). Everything else on this row is unchanged.
- The third plate is renamed from "Obsidian Steel" because there is no such GT material; the lang key and registry name change with it. It is Aluminum because the plate sits in the MV tier ([0010](../../design/0010-material-remap-and-tier-additions.md) decision 1).
- The Iron plate is wrought iron, which is the Steam/ULV gate material ([0001](../../design/0001-tier-map.md)). GT's own hand-made armor is the precedent: [CraftingRecipeLoader.java#L350-L368](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/CraftingRecipeLoader.java#L350-L368).
- The carbon plate is GT's `plate.EpoxidFiberReinforced` plus the EV tier material, Titanium ([0010](../../design/0010-material-remap-and-tier-additions.md) decision 2); IC2's carbon plate is not used, although GT uses it in the GraviSuite recipes ([CraftingRecipeLoader.java#L1909-L1923](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/CraftingRecipeLoader.java#L1909-L1923)).

## Explosive charges (2 recipes)

Both are charge blocks (`basicMachine:6` and `:8` in Techguns) placed on a face and configured in a GUI. Hardness caps are 30 and 100 ([explosive-charges.md](../../techguns-spec/09-machines-utility/explosive-charges.md#parameters)). [0002](../../design/0002-machine-deconfliction.md) says "tier-gated" but names no tier; the gate below is this sheet's proposal (see Open questions).

| Output | Techguns | Tier | Method | Inputs | Circuit | EU/t, time |
|---|---|---|---|---|---|---|
| Explosive Charge x8 | B08 (`rtr` / `tct` / `rtr`: 4 rubber, 4 TNT, 1 circuit) | MV | `assemblerRecipes` | vanilla TNT (`Blocks.tnt`) x4, `plate.Rubber` x4, `circuit.MV` x1, `plate.Aluminium` x1 | 1 | `TierEU.RECIPE_MV`, 20 s |
| Advanced Explosive Charge x4 | B09 (`rtr` / `tct` / `rtr`: 4 plastic sheets, 4 TGX, 1 circuit) | HV | `assemblerRecipes` | vanilla TNT x4, `plate.Polyethylene` x4, `circuit.HV` x1, `plate.StainlessSteel` x1; fluid `Materials.Glyceryl.getFluid(1_000)` | 2 | `TierEU.RECIPE_HV`, 20 s |

Gate: the Aluminum plate (MV) and the Stainless steel plate with glyceryl trinitrate (HV). Glyceryl trinitrate is a GT chemistry fluid ([MaterialsInit.java#L6478-L6490](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L6478-L6490)), so the advanced charge needs the chemical line that produces it. The Assembler precedent for a plate-and-circuit recipe with a fluid is the Plascrete block ([AssemblerRecipes.java#L606-L614](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L606-L614)).

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
- **GGS-09-29.** The Steel, Aluminum and Carbon turret armor recipes are Assembler recipes at `TierEU.RECIPE_<tier>` of their row. The Iron plate is the only turret armor made in the crafting table.
- **GGS-09-30.** Each turret armor recipe has an input from its tier's gate material (wrought iron, steel, aluminum, titanium).
- **GGS-09-31.** The Explosive Charge recipe runs at `TierEU.RECIPE_MV` or above and the Advanced Explosive Charge recipe at `TierEU.RECIPE_HV` or above. Neither uses a fluid that can be had below the tier.
- **GGS-09-32.** The charge recipes use only vanilla TNT and GT parts. They do not call a Techguns explosive item.
- **GGS-09-33.** The Turret has a recipe for each of MV, HV, EV and IV (IDs 20116 to 20119), and the Repair Bench for each of LV, MV, HV, EV and IV (IDs 20109 to 20113). Each includes the `Hull`, `Robot_Arm` and plate of its own tier from the tier table.
- **GGS-09-34.** The Armory Gantry controller (ID 20114) needs one `Hull_HV`, two `Robot_Arm_HV` and one Gantry Hoist. The four Gantry casing recipes (Casing, Beam, Hoist, Cradle) are Assembler recipes at HV or `RECIPE_LV / 2`.
- **GGS-09-35.** Bastion Plating has four recipes, one per meta, and each consumes exactly one matching turret armor plate (Iron, Steel, Aluminum, Carbon) and yields four blocks. The meta's armor value is 5, 10, 15 and 20.
- **GGS-09-36.** The Rampart controller (ID 20120) needs one `Hull_HV` and two Steel Bastion Plating.
- **GGS-09-37.** The Gun Mount Hatch has a recipe for each of MV, HV, EV and IV (IDs 20121 to 20124). Each needs the `Hull`, `Robot_Arm`, `Sensor` and plate of its own tier. The hatch recipe runs at `TierEU.RECIPE_<tier>`.
- **GGS-09-38.** No GregGuns recipe in this sheet is craftable below its tier: the dev self-check matches each recipe's tier parts to the MTE's `mTier`, or to the item tier of the plate that a casing consumes.

## Tests

| Req | Verified by |
|---|---|
| 26 | Unit test of the recipe loader with a stubbed unificator that returns `null` |
| 27 | Unit test over the MTE registry against the recipe inputs |
| 28 | Unit test that scans inputs for removed Techguns item names |
| 29, 30, 31 | Dev self-check: unit test over the recipe registry against the tier table |
| 33, 37 | Unit test over the MTE registry: one recipe per ID, tier parts match `mTier` |
| 34, 36 | Unit test over the controller recipe inputs |
| 35 | Unit test: each plating meta's recipe holds exactly one turret armor plate, and the armor values are 5, 10, 15, 20 |
| 38 | Dev self-check over all recipes in this sheet |
| 32 | Unit test over the charge recipe inputs |
| All | Manual: NEI shows each recipe under the crafting grid or the Assembler |

## Assumed components

| Component | Used in | Source |
|---|---|---|
| Concrete (`concrete:0`) | Turret | [07 recipes](../07-blocks/recipes.md#concrete) (Mixer recipe), block from [decorative-blocks.md](../07-blocks/decorative-blocks.md) |

Gantry Casing, Beam, Hoist, Repair Cradle and Bastion Plating are defined in this sheet and used by the multiblocks of [08](../08-machines-production/multiblocks.md). The Works Casing and Ram Housing belong to [08 recipes](../08-machines-production/recipes.md#ammunition-works-3-recipes). No component from area 06 is consumed. The turret armor plates (Iron, Steel, Aluminum, Carbon) are defined in this sheet because [components-and-materials.md](../06-items/components-and-materials.md) assigns them to area 09.

## Open questions

- **Machine tiers (closed).** [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5 adopts Turret MV, Repair Bench LV, Camo Bench LV, as in the [machine roster](../08-machines-production/machine-roster.md).
- **Repair Bench EU.** Already open in [repair-bench.md](repair-bench.md#open-questions). If it draws no EU, its hull recipe becomes a casing recipe.
- **Charge tiers.** MV and HV are adopted by [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5. The overlap with GT mining explosives is already open in [explosive-charges.md](explosive-charges.md#open-questions); a balance pass would also confirm whether Glyceryl is the right HV gate.
- **Machine and casing costs.** The tier grids, the Bastion Plating yield of four, the Hoist and Cradle parts and the Gun Mount Hatch parts are proposals. A balance pass fixes them.
- **Turret armor name (closed).** "Aluminum Turret Armor" replaces "Obsidian Steel Turret Armor", following [0010](../../design/0010-material-remap-and-tier-additions.md) decision 1.
