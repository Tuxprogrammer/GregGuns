# Block recipes

Every recipe that makes a kept GregGuns block, written with GT parts and GT machines.

- **Techguns baseline:** [block-catalogue.md § Recipes](../../techguns-spec/07-blocks/block-catalogue.md#recipes) (recipes B11 to B34 in [06-items/recipes.md](../../techguns-spec/06-items/recipes.md)), [block-sheets.md](../../techguns-spec/07-blocks/block-sheets.md)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (decorative blocks kept; weapon and ammo chests kept as low-priority decorative storage; Chem Lab dropped), [0001 tier map](../../design/0001-tier-map.md) (lists no blocks)
- **Sibling sheets:** [decorative-blocks.md](decorative-blocks.md), [storage-chests.md](storage-chests.md), [block-platform.md](block-platform.md)
- **Format:** [CONVENTIONS § Recipe sheets](../CONVENTIONS.md#recipe-sheets)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205)
- **Roadmap:** the blocks milestone in [ROADMAP](../../ROADMAP.md)

## What changes from Techguns

Techguns' block recipes are crafting-table grids over Techguns plates and nuggets. [0001](../../design/0001-tier-map.md) assigns no tier to blocks; [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5 places decorative blocks at LV, so every machine recipe here runs at `TierEU.RECIPE_LV` or a fraction of it. The Techguns iron, steel, obsidian steel and carbon plates in blocks use the plate of the consuming block's tier (Steel at LV), as that record requires. It keeps the GT shapes instead: small hand recipes stay in the crafting table, bulk recipes (16 or more outputs, or a stack of a dozen inputs) move to the Assembler the way GT builds its own casings and covers, and the Chem Lab concrete recipe becomes a Mixer recipe.

| Techguns | GregGuns | Why |
|---|---|---|
| `plateTin`, `plateIron`, `plateSteel`, `plateCopper`, `nuggetIron`, `itemRubber` | `plate.Tin`, `plate.Iron`, `plate.Steel`, `plate.Copper`, `nugget.Iron`, `plate.Rubber` | Techguns materials are dropped ([components-and-materials.md](../06-items/components-and-materials.md)) |
| Bulk grids (ladder x16, lamp x16, panel x32 and x64, roof x16) | Assembler at `RECIPE_LV / 2` | Matches GT's casing and cover recipes; keeps a 9-slot grid from producing 64 blocks |
| Concrete from the Chem Lab (sand, gravel, 250 mB water gives 2) | Mixer, same inputs and output | [0002](../../design/0002-machine-deconfliction.md) drops the Chem Lab |
| Reinforced and Weapon Chest from iron nuggets and plates | Assembler at `RECIPE_LV` with `plate.Steel` | Steel makes them LV items, which fits "reinforced" |
| Machine-block recipes (Charging Station, Ore Drill, Fabricator and Reaction Chamber housings, Turret, benches, charges) | Not here | Dropped, or owned by [08](../08-machines-production/recipes.md) and [09](../09-machines-utility/recipes.md) |

## Integration points

### GregTech: bulk block recipes and the Mixer

- **Pack config:** none.
- **Source:**
  - Bulk, low-cost Assembler recipes with a programmed circuit and `RECIPE_LV / 2`: the casings at [AssemblerRecipes.java#L2671-L2700](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2671-L2700), and the Drain Cover (2 aluminium plates and 2 iron bars) at [AssemblerRecipes.java#L326-L335](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L326-L335).
  - A block that wraps a chest in an Assembler recipe: the LV Input Bus (hull, chest, programmed circuit 1) at [AssemblerRecipes.java#L3472-L3480](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L3472-L3480).
  - The Mixer map is `RecipeMaps.mixerRecipes` ([RecipeMaps.java#L534](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L534)). The Industrial Mixer multiblock reads a **different** map, `mixerNonCellRecipes` ([RecipeMaps.java#L2185](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L2185), [MTEIndustrialMixer.java#L65](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L65)), so a recipe that needs no cell is registered to both, as GT does for its acid chains ([AcidRecipes.java#L114](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/chains/AcidRecipes.java#L114); [machine-roster.md](../08-machines-production/machine-roster.md#the-gt-coverage-check)). GT's own concrete block recipe is a Chemical Bath of a steel frame in molten concrete ([ChemicalBathRecipes.java#L64-L70](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/ChemicalBathRecipes.java#L64-L70)); GregGuns' concrete is a different block and keeps the Techguns inputs.
- **GregGuns does:** registers these through `GTValues.RA.stdBuilder()`. Part lookups go through `GTOreDictUnificator.get` ([GGS-06-11](../06-items/components-and-materials.md#requirements)).
- **Pack-side change:** none.

## Conventions

- **Method.** "Hand" is a shaped or shapeless crafting recipe. "Assembler" is `assemblerRecipes`. "Mixer" is `mixerRecipes`, and for a recipe without a cell also `mixerNonCellRecipes`.
- **Time.** 2 seconds per input item, at least 10 seconds.
- **EU/t.** `TierEU.RECIPE_LV / 2` unless the row says otherwise.
- **Tier and gate.** LV for decorative blocks and chests ([0010](../../design/0010-material-remap-and-tier-additions.md) decision 5); [0001](../../design/0001-tier-map.md) lists none. Blocks are decoration and storage, so no recipe gates progress beyond the LV machine; the "Gate" is only the input worth noting. Iron plates and nuggets in decorative rows are cheap filler, not a tier gate.
- **Metadata.** Output metadata is the first variant of the block (meta 0, or the lamp metas stated). Other variants come from Chisel and the Camo Bench, as in Techguns.
- `glass pane` is vanilla `Blocks.glass_pane`, `chest` is vanilla `Blocks.chest`, `stone`, `sand`, `dirt` and `stick` are vanilla.

## Metal Ladder, lamps and lanterns

Techguns recipes B11 to B15. Lamp variants: meta 0 yellow lamp, meta 8 white lamp, meta 7 yellow lantern, meta 15 white lantern ([block-catalogue.md](../../techguns-spec/07-blocks/block-catalogue.md#registered-blocks)).

| Output | Method | Inputs | Circuit | Time | Notes |
|---|---|---|---|---|---|
| Metal Ladder (`ladder01`, grey) x16 | Assembler | `plate.Tin` x7 | 1 | 14 s | B11 (`iii` / `.i.` / `iii`) |
| Lamp (`lamp01:0`, yellow) x16 | Assembler | glass pane x5, `dust.Redstone` x1, `nugget.Iron` x3 | 1 | 18 s | B12 (`iii` / `grg` / `ggg`) |
| Lantern (`lamp01:7`, yellow) x16 | Assembler | `nugget.Iron` x6, glass pane x2, `dust.Redstone` x1 | 2 | 18 s | B14 (`iii` / `grg` / `iii`) |
| Lamp, yellow to white (`lamp01:8`) x1 | Hand, shapeless | `lamp01:0` | n/a | n/a | B13, one direction |
| Lamp, white to yellow (`lamp01:0`) x1 | Hand, shapeless | `lamp01:8` | n/a | n/a | B13, other direction |
| Lantern, yellow to white (`lamp01:15`) x1 | Hand, shapeless | `lamp01:7` | n/a | n/a | B15, one direction |
| Lantern, white to yellow (`lamp01:7`) x1 | Hand, shapeless | `lamp01:15` | n/a | n/a | B15, other direction |

## Shipping container, sandbags, camo nets, panels

Techguns recipes B21 to B24, B31, B32.

| Output | Method | Inputs | Circuit | Time | Notes |
|---|---|---|---|---|---|
| Shipping Container, Red (`container:0`) x8 | Assembler | chest x1, `plate.Iron` x4 | 3 | 10 s | B21 (`i.i` / `.c.` / `i.i`, `ingotIron` is now a plate) |
| Sandbags x8 | Hand, shapeless | `plate.Rubber` x1, sand x8 | n/a | n/a | B22. Keeps the Techguns shape |
| Camo Net (`camoNetPane:0`, Woodland) x8 | Hand, shaped `sds` / `sds` | `s` stick, `d` dirt | n/a | n/a | B23, unchanged |
| Camo Net Roof (`camoNetRoof:0`) x16 | Hand, shaped `wdw` / `dsd` / `wdw` | `w` stick, `d` dirt, `s` string | n/a | n/a | B24, unchanged |
| Metal Panel (`metalpanel:0`) x32 | Assembler | `stone` x8, `plate.Copper` x1 | 1 | 18 s | B31 (`sss` / `sps` / `sss`) |
| Metal Panel, Connected (`metalpanel2:0`) x64 | Assembler | `stone` x8, `plate.Copper` x1 | 2 | 18 s | B32 (`sss` / `psp` / `sss`). The circuit number is what separates it from the plain panel |

## Concrete

| Output | Method | Inputs | EU/t, time | Notes |
|---|---|---|---|---|
| Concrete (`concrete:0`) x2 | Mixer: `mixerRecipes` **and** `mixerNonCellRecipes` | sand x1, gravel x1, `Materials.Water.getFluid(250)` | `TierEU.RECIPE_LV / 2`, 10 s | Techguns Chem Lab row 2. The recipe needs no cell, so the same inputs, output, EU/t and time are registered to both mixer maps, which lets the Industrial Mixer run it. The block is GregGuns' concrete in the `gregguns_concrete` carving group, not Chisel's ([GGS-07-02](decorative-blocks.md#requirements)) |

## Chests and the door

| Output | Method | Inputs | Circuit | EU/t, time | Notes |
|---|---|---|---|---|---|
| Reinforced Chest (`tgchest`) x1 | Assembler | chest x1, `plate.Steel` x4 | 1 | `TierEU.RECIPE_LV`, 10 s | B33 (`n.n` / `.c.` / `n.n`). Gate: `plate.Steel`, which makes it an LV item |
| Weapon Chest (`tgchest_weapon`) x1 | Assembler | chest x1, `plate.Steel` x2 | 2 | `TierEU.RECIPE_LV`, 10 s | B34 (`pcp`). Whether this block survives at all is open in [storage-chests.md](storage-chests.md#open-questions) |
| Bunker Door (`bunker_door`, Techguns `doorItem01`) x2 | Assembler | `plate.Iron` x6 | 4 | `TierEU.RECIPE_LV / 2`, 12 s | Techguns `pp` / `pp` / `pp`. The item is listed with area 06's items (R71 in [06-items/recipes.md](../../techguns-spec/06-items/recipes.md)); if 06 gives it a recipe, the two must match |

## Not carried over

| Techguns recipe | Reason |
|---|---|
| B04 to B09 (Turret, Camo Bench, Repair Bench, Explosive Charge, Advanced Explosive Charge) | Kept machines, now in [09 recipes](../09-machines-utility/recipes.md) |
| B10 Charging Station | Dropped ([0002](../../design/0002-machine-deconfliction.md)) |
| B16 to B20 Ore Drill parts | Dropped ([0002](../../design/0002-machine-deconfliction.md)) |
| B25 to B27 Reaction Chamber Housing, Controller, Glass | Replaced by real casings and a GT controller in [08 recipes](../08-machines-production/recipes.md); the chamber glass is GT's |
| B28 to B30 Fabricator Housing, Controller, Glass | Dropped ([0002](../../design/0002-machine-deconfliction.md)) |
| Camo Net Placer items (R84 to R86) | Items, not blocks. Area 06 |
| Screen, Control Panel, steel-frame variants, bug blocks, Monster Spawner, Air Marker, Bio Blob | No Techguns recipe ([block-catalogue.md](../../techguns-spec/07-blocks/block-catalogue.md#recipes)) |

## Requirements

Numbering continues from [block-platform.md](block-platform.md).

- **GGS-07-26.** Every recipe in this sheet resolves at boot. A `null` GT part skips that recipe and logs one warning ([GGS-06-11](../06-items/components-and-materials.md#requirements)).
- **GGS-07-27.** No recipe input is a Techguns plate, nugget, ingot, circuit or rubber item ([GGS-06-10](../06-items/components-and-materials.md#requirements), [GGS-05-31](../05-armor/recipes.md#requirements) apply the same rule).
- **GGS-07-28.** The bulk recipes (ladder, lamp, lantern, metal panel, connected metal panel, container) are Assembler recipes. Sandbags, camo nets and the lamp colour swaps are crafting-table recipes.
- **GGS-07-29.** The lamp and lantern colour swaps are reversible: crafting one way and then back returns the original item, one for one.
- **GGS-07-30.** The concrete recipe outputs the GregGuns concrete block, never a block from a Chisel carving group.
- **GGS-07-31.** The Reinforced Chest and Weapon Chest recipes accept only an empty vanilla chest, so no stored items are consumed or lost.
- **GGS-07-32.** No recipe produces an ore drill, charging station, fabricator or ore cluster block.
- **GGS-07-33.** The concrete recipe is registered to both `RecipeMaps.mixerRecipes` and `RecipeMaps.mixerNonCellRecipes`, with identical inputs, output, EU/t and duration ([GGS-08-51](../08-machines-production/machine-roster.md#requirements)).

## Tests

| Req | Verified by |
|---|---|
| 26 | Unit test of the recipe loader with a stubbed unificator that returns `null` |
| 27 | Unit test scanning recipe inputs for removed Techguns item names |
| 28 | Unit test listing recipe method per output |
| 29 | Unit test: apply each swap twice and compare |
| 30 | Unit test on the registered concrete output |
| 31 | Unit test of the recipe's input matcher with a filled chest |
| 32 | Unit test over the registry |
| 33 | `runServer` smoke test: the concrete recipe is present in both mixer maps after boot |
| All | Manual: NEI shows each recipe under the Assembler, Mixer or crafting grid |

## Assumed components

None. Every input is a GT part or a vanilla item.

## Open questions

- **Block tiers (closed).** [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5: decorative blocks LV. The chests are LV (Steel).
- **Weapon Chest.** Kept only if [storage-chests.md](storage-chests.md#open-questions) keeps it.
- **Door item ownership.** This sheet owns the recipe; [06 recipes](../06-items/recipes.md#component-index) lists `bunker_door` and points here.
