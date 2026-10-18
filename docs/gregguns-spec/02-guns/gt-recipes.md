# Gun recipes on GT recipe maps

- **Techguns baseline:** [recipes.md](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) (shaped crafting-table grids), [recipes.md § shapeless](../../techguns-spec/02-guns/recipes.md#shapeless--custom-recipes) (akimbo combine and split)
- **Design records:** [0001 Tier map](../../design/0001-tier-map.md), [0002 Machine deconfliction](../../design/0002-machine-deconfliction.md)
- **Pack mods:** GregTech (`gregtech`, GT5-Unofficial `5.09.54.205`)
- **Roadmap:** M2 (ballistic arsenal), M4 (production chain), M5 (energy and exotic weapons)

## What changes from Techguns

Techguns builds every gun at a crafting table from one grid. GregGuns builds the **final assembly** of each gun on a GT recipe map, at a minimum machine voltage, with at least one gate material ([0001](../../design/0001-tier-map.md)). The Ammo Press, Fabricator and Metal Press are gone ([0002](../../design/0002-machine-deconfliction.md)); their jobs go to GT maps.

| Techguns | GregGuns | Why |
|---|---|---|
| One shaped crafting grid per gun, with "loaded" and "empty" variants | One GT recipe per gun that outputs an **unloaded** gun. Ammo is a separate item. | A GT recipe has no "consume a full magazine and keep its contents" step. The loaded variants were a crafting-table shortcut. |
| Tier is set by which Techguns table you own | Tier is set by the recipe's `eut` and its gate material | GT players read progress by voltage ([0001](../../design/0001-tier-map.md#alternatives-considered)). |
| Akimbo guns by shapeless crafting that merges NBT | Stays a **crafting-table** recipe | A GT recipe consumes its inputs and cannot copy their NBT (ammo, camo). It is not a GT map job. |
| Techguns plates, wires, circuits | GT plates, cables, circuits | [0002](../../design/0002-machine-deconfliction.md) |

## Integration points

### GT5-Unofficial: the recipe adder

- **Pack config:** none. Recipes are code, not config.
- **Source:**
  - `GTValues.RA` is the recipe adder: [GTValues.java#L264](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/GTValues.java#L264). Its one method is `stdBuilder()`: [IGTRecipeAdder.java#L7](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/interfaces/internal/IGTRecipeAdder.java#L7).
  - The builder takes `itemInputs`, `circuit(n)`, `fluidInputs`, `itemOutputs`, `duration`, `eut` and `metadata`, then `addTo(map...)`. `addTo` can feed several maps at once: [GTRecipeBuilder.java#L1201-L1206](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L1201-L1206).
  - Worked example of an Assembler recipe with `eut(TierEU...)`: [AssemblerRecipes.java#L76-L82](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L76-L82).
  - Recipe voltages: `TierEU.RECIPE_ULV` to `RECIPE_MAX` are `V x 30 / 32`, so `RECIPE_LV` = 30, `RECIPE_MV` = 120, `RECIPE_HV` = 480, `RECIPE_EV` = 1,920, `RECIPE_IV` = 7,680: [TierEU.java#L25-L39](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L25-L39) and [GTValues.java#L92-L108](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/GTValues.java#L92-L108).
- **GregGuns does:** registers recipes with `GTValues.RA.stdBuilder()` once GT's items and materials exist (post-init; GT is a hard dependency). Inputs use `GTOreDictUnificator.get(OrePrefixes..., Materials..., n)` or `ItemList`, never raw ore-dictionary strings of other mods. `eut` is always a `TierEU.RECIPE_*` constant, never a number.
- **Pack-side change:** none.

### GT5-Unofficial: how voltage gates a recipe

- **Source:** a machine only starts a recipe when `availableEUt >= recipe EUt`; otherwise GT reports "insufficient power": [ParallelHelper.java#L399-L401](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/ParallelHelper.java#L399-L401) and [ProcessingLogic.java#L634-L636](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/logic/ProcessingLogic.java#L634-L636).
- **GregGuns does:** sets the gun's tier **only** through `eut`. A `RECIPE_MV` Assembler recipe cannot run in an LV machine, so no extra gating code is needed.
- **Pack-side change:** none.

### GT5-Unofficial: recipe maps per gun family

Maps are `RecipeMaps` fields: [RecipeMaps.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java). Tiers and gate materials come from [0001](../../design/0001-tier-map.md). This table lists the final-assembly map and the component maps, not full recipes.

| 0001 tier (gate) | Gun families | Final assembly | Recipe `eut` |
|---|---|---|---|
| Steam / ULV (bronze, wrought iron) | Hand Cannon | Crafting table through `GTModHandler.addCraftingRecipe` (the route GT uses for [rounds](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingRound.java#L37-L46)). No electric machine exists at this tier. | none |
| LV (steel) | 9mm Pistol, Revolver, Sawed Off, Bolt Action Rifle; Combat Knife, Crowbar | [`assemblerRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266) | `RECIPE_LV` |
| MV (aluminium) | Thompson, Mac10, Combat Shotgun, M4, AK47; frag grenade, Flashbang | `assemblerRecipes` | `RECIPE_MV` |
| MV | Akimbo pistols and revolvers | Crafting table (see above) | none |
| HV (stainless steel) | AUG, SCAR, Vector, Infiltrator, AS50, LMG, Grenade Launcher, Rocket Launcher, Flamethrower, Chainsaw, Power Hammer | `assemblerRecipes` | `RECIPE_HV` |
| EV (titanium) | Laser Rifle, PDW, Pulse Rifle, Minigun, Tesla Rifle, Sonic Rifle, Bio Gun | `assemblerRecipes`. Control boards from [`circuitAssemblerRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1277). | `RECIPE_EV` |
| IV (tungstensteel) | Blaster Rifle, De-Atomizer Pistol, Scatter Laser Rifle, Golden Revolver | `assemblerRecipes`, with a GT battery as a component | `RECIPE_IV` |
| LuV (HSS-G and LuV alloys) | Nether Blaster, Alien Blaster, Grim Reaper | **Assembly Line** (below) | `RECIPE_LuV` |
| ZPM (naquadah alloys) | Nuclear Death Ray, Nuke Launcher | Assembly Line | `RECIPE_ZPM` |

Component maps used at every tier:

| Part | Map | Note |
|---|---|---|
| Barrels, tubes | [`latheRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1315) from rods; [`extruderRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1435) with `Shape_Extruder_Pipe_*` | Shapes: [ItemList.java#L151-L161](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L151-L161) |
| Receivers, plates | [`benderRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1229), [`hammerRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1447) | GT already makes plates from any material |
| Bullet slugs | GT's own `round` prefix: [OrePrefixes.java#L960](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/OrePrefixes.java#L960). GT makes it from nuggets in the Lathe at `RECIPE_ULV`: [ProcessingRound.java#L27-L35](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingRound.java#L27-L35) | Use `round` for metal slugs instead of a Techguns bullet item. Cartridge cases, magazines and shells are GregGuns items built on `assemblerRecipes`. |
| Wiring | [`wiremillRecipes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1219) | Energy weapons only |

### GT5-Unofficial: Assembly Line for LuV and ZPM

- **Source:** `GTRecipeConstants.AssemblyLine` takes 4 to 16 item inputs, 1 to 4 fluid inputs and exactly one output. It needs a `RESEARCH_ITEM` metadata value, and `SCANNING` gives the research time and voltage: [GTRecipeConstants.java#L559-L567](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L559-L567). The NEI display map is `assemblylineVisualRecipes` ([RecipeMaps.java#L426-L429](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L426-L429)). Worked example, the LuV motor: [AssemblyLineRecipes.java#L61-L74](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblyLineRecipes.java#L61-L74).
- **GregGuns does:** uses `.metadata(RESEARCH_ITEM, <previous-tier gun or component>)` and `.metadata(SCANNING, new Scanning(time, TierEU.RECIPE_*))`, then `.addTo(AssemblyLine)`. The research item is the tier-below weapon (for example the Blaster Rifle for the Nether Blaster), so players scan a real weapon.
- **Precedent to note:** GT builds its LuV motor with `eut(TierEU.RECIPE_IV)`, one tier **below** the item. [0001](../../design/0001-tier-map.md) says the final recipe sits at the item's own tier. See Open questions.
- **Pack-side change:** none.

## Requirements

- **GGS-02-01.** Every gun and grenade recipe except the Hand Cannon and the akimbo combine and split is added with `GTValues.RA.stdBuilder()...addTo(<GT map>)`.
- **GGS-02-02.** Each recipe's `eut` is the `TierEU.RECIPE_*` constant of the gun's [0001](../../design/0001-tier-map.md) tier. No literal EU/t numbers.
- **GGS-02-03.** Each recipe has at least one input whose material is the 0001 gate material for that tier, expressed through `GTOreDictUnificator`, `OrePrefixes` and `Materials`.
- **GGS-02-04.** LuV and ZPM guns are added to `AssemblyLine` with both `RESEARCH_ITEM` and `SCANNING` metadata.
- **GGS-02-05.** Metal bullet slugs use GT's `round` prefix. They are not a GregGuns item.
- **GGS-02-06.** Guns that are IC2 electric items are crafted **uncharged** (see [energy-electric-items.md](energy-electric-items.md)).
- **GGS-02-07.** Akimbo combine and split stay crafting-table recipes and keep the Techguns NBT behaviour from [recipes.md](../../techguns-spec/02-guns/recipes.md#shapeless--custom-recipes), with the bug GUN-26 in [index.md](../../techguns-spec/02-guns/index.md#suspected-bugs) fixed.
- **GGS-02-08.** A dev self-check iterates all registered gun recipes and fails if any recipe's `eut` exceeds its tier's `TierEU.RECIPE_*` value or if the recipe lacks a gate material.

## Tests

- GGS-02-01, 03, 04: unit test over the recipe registry (read back the `GTRecipe` objects from the `RecipeMaps` the builder wrote to).
- GGS-02-02, 08: the same test, comparing `mEUt` with the tier table.
- GGS-02-05: unit test that the bullet recipe's output is `OrePrefixes.round`.
- GGS-02-06, 07: `/gregguns test` scenario that crafts each item and reads its NBT.
- All: a `runServer` boot check that registration throws no exception with GT loaded.

## Open questions

- **LuV and ZPM `eut`.** GT's LuV motor (an LuV item) uses `RECIPE_IV` on the Assembly Line. [0001](../../design/0001-tier-map.md) puts the final recipe at the item's own tier. Settling this needs an amendment to 0001: follow GT's one-tier-below precedent, or keep `RECIPE_LuV` and `RECIPE_ZPM`.
- **Gate materials are starting points** ([0001](../../design/0001-tier-map.md)). Each must be re-checked against the pack's progression when its recipe is written (M2 to M5).
