# Production machine recipes

How the GregGuns production machines are built (Ammo Press family and Ammunition Works), plus the two component recipes that used to run in the Reaction Chamber and now live in GT's own maps. The Reaction Chamber and the Retort are retired ([0012](../../design/0012-retire-reaction-chamber-and-retort.md)); the rest of area 08 is recipes in GT's own maps, listed in [recipe-map-replacements.md](recipe-map-replacements.md).

- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0003 energy model](../../design/0003-energy-model.md), [0010 material remap](../../design/0010-material-remap-and-tier-additions.md), [0011 machine policy](../../design/0011-machine-policy.md), [0012 retire the Reaction Chamber and the Retort](../../design/0012-retire-reaction-chamber-and-retort.md); tiers and IDs in the [machine roster](machine-roster.md)
- **Sibling sheets:** [multiblocks.md](multiblocks.md), [metatileentity-framework.md](metatileentity-framework.md)
- **Format:** [CONVENTIONS § Recipe sheets](../CONVENTIONS.md#recipe-sheets)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205), StructureLib (`structurelib`)
- **Roadmap:** M4

## Integration points

### GregTech: multiblock controllers are crafted around a casing


- **Pack config:** none.
- **Source:** GT's multiblock controllers are shaped crafting recipes with the controller's casing in the center cell and a tier circuit, a cable and tier parts around it:
  - Vacuum Freezer (EV): casing `Casing_FrostProof`, `Electric_Pump_HV`, `circuit.EV`, `cableGt01.Gold` ([MTERecipeLoader.java#L3407-L3413](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3407-L3413)).
  - Blast Furnace and Implosion Compressor use the same layout with their own casing ([#L3401-L3406](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3401-L3406), [#L3414-L3419](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3414-L3419)).
  - Casings are Assembler recipes: 6 plates and a frame, programmed circuit 1, `RECIPE_LV / 2` ([AssemblerRecipes.java#L2671-L2700](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2671-L2700)).
- **GregGuns does:** follows that layout for the controller and the casing.
- **Pack-side change:** none.

## Build recipes

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
| Works Casing x2 | HV | `assemblerRecipes` | `plate.StainlessSteel` x6, `frameGt.StainlessSteel` x1 | 6 | `TierEU.RECIPE_LV / 2`, 2 s 10 ticks | The default hall needs 277 or more casings (interior parts and glass are GT blocks, not Works Casings); one per craft would cost about 1,740 plates, so this yields two. A proposal. Circuit 6, not 1: GT's own Stainless Steel casing is this mix on circuit 1 (recipe-clash fix) |
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


## Component recipes moved to GT maps

The Laser Focus and the Anti Gravity Core used to run in the Reaction Chamber. Both now use GT's own recipe maps and ship with the guns module (`ComponentTable`). Full detail and sources are in [0012](../../design/0012-retire-reaction-chamber-and-retort.md); the entries are RC-64 and RC-65 in [06-items/recipes.md](../06-items/recipes.md).

| Recipe | Output | Map | Tier | Inputs | EU/t | Ticks |
|---|---|---|---|---|---|---|
| RC-64 | `part_laser_focus` | `RecipeMaps.laserEngraverRecipes` | EV | `gem.Ruby` x1, `Emitter_EV` x1, `craftingLensWhite` (non-consumed) | 1,920 (`TierEU.RECIPE_EV`) | 600 |
| RC-65 | `part_anti_gravity_core` | `RecipeMaps.assemblerRecipes` | IV | `gem.NetherStar` x1, `Emitter_IV` x1, `circuit.IV` (Elite) x2, 1,152 mB molten Enderium | 7,680 (`TierEU.RECIPE_IV`) | 600 |

## Requirements

Numbering continues from [recipe-scripting.md](recipe-scripting.md).

- **GGS-08-39.** Withdrawn by 0012 (Reaction Casing, controller and Heat Ray Focus recipes are gone).
- **GGS-08-40.** Withdrawn by 0012 (the Retort controller recipe is gone).
- **GGS-08-41.** Withdrawn by 0012 (`gregguns.recipe.reactionchamber` no longer exists). RC-64 and RC-65 replace it as GT-map recipes.
- **GGS-08-42.** Withdrawn by 0012.
- **GGS-08-43.** Withdrawn by 0012.
- **GGS-08-44.** Withdrawn by 0012. No GregGuns hatch exists; the Ammunition Works accepts only GT hatches ([multiblocks.md](multiblocks.md)).
- **GGS-08-45.** Withdrawn by 0012.
- **GGS-08-85.** An Ammo Press recipe exists for each of the five tiers (Steam, LV, MV, HV, EV, MTE IDs 20100 to 20104). The Steam one uses `Hull_Bronze`, `pipeSmall.Bronze` and a piston. The LV to EV ones use the same grid, with that tier's hull, circuit, motor, piston and conveyor.
- **GGS-08-86.** The Ammunition Works has three recipes: Works Casing, Ram Housing and the controller (MTE 20105). The controller needs one `Hull_HV` and four Works Casings.
- **GGS-08-87.** Withdrawn by 0012 (the single-block Reaction Chamber is gone).
- **GGS-08-88.** Every machine recipe in this sheet includes the `Hull_<tier>` (`Hull_Bronze` for Steam) of the machine's `mTier`, and the other tier parts of the same tier. The dev self-check rejects a lower-tier part.
- **GGS-08-89.** The Works Casing recipe yields two blocks and every other casing recipe in this sheet yields what its table says.

## Tests

| Req | Verified by |
|---|---|
| 85, 86 | Unit test over the recipe registry: one recipe per MTE ID, and each has the inputs named here |
| 88 | Dev self-check: parse each machine recipe, compare each part's tier with the MTE's `mTier` |
| 89 | Unit test of the Works Casing output count |
| RC-64, RC-65 | Unit test of `ComponentTable` registration; manual: NEI shows the Laser Focus under the Laser Engraver and the Anti Gravity Core under the Assembler |

## Assumed components

| Component | Used in | Source |
|---|---|---|
| (none consumed from area 06) | n/a | Every input is a GT part or a vanilla item. The Works Casing and Ram Housing are produced and consumed within this sheet |

The two component recipes above **produce** the Laser Focus (a gun component, area 06 and the laser weapons) and the Anti Gravity Core (area 05, [armor recipes](../05-armor/recipes.md#anti-gravity-device-1-recipe)).

## Open questions

- **Works Casing yield.** Two per craft is a proposal; 1:1 would match GT but costs about 1,740 Stainless Steel plates for a default hall. Balance pass.
- **Ammo Press grids.** These are the recipes' shapes, not tuned costs. The press adds a conveyor and motor to GT's Forming Press, which can be dropped if the cost proves too high.
- **Laser Focus cost.** The Emitter is now consumed per focus rather than once inside a non-consumed Heat Ray Focus. Tune in review if playtests find it too steep ([0012](../../design/0012-retire-reaction-chamber-and-retort.md)).
