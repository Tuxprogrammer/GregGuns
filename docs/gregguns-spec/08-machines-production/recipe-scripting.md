# Recipe scripting (MineTweaker)

How pack makers edit GregGuns recipes in GT New Horizons, in place of Techguns' seven `mods.techguns.*` ZenScript classes.

- **Techguns baseline:** [minetweaker.md](../../techguns-spec/08-machines-production/minetweaker.md#1-loading) (loading and the helper), and its sections per machine
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md)
- **Pack mods:**
  - CraftTweaker (`MineTweaker3`, 3.4.8), GTNH's build of MineTweaker 3
  - GregTech MineTweaker addon (`GTTweaker`, 2.3.4)
  - ModTweaker (`modtweaker2`, 0.14.0)
- **Roadmap:** M4

## What changes from Techguns

| Techguns ZenClass | GregGuns |
|---|---|
| `mods.techguns.AmmoPress`, `MetalPress`, `ChemLab`, `Fabricator`, `CharginStation` | None. These recipes are in GT recipe maps, scripted with GTTweaker (`mods.gregtech.Assembler`, `FormingPress`, `ChemicalReactor`, `Canner`, ...) |
| `mods.techguns.ReactionChamber` | New `mods.gregguns.ReactionChamber` for adding recipes. Removal uses GTTweaker's generic remover |
| `mods.techguns.OreCluster` | Out of scope here; worldgen area 11 |
| Registered in Techguns' init when `MineTweaker3` is loaded, via `@Optional.Interface` | Same soft-dependency guard on `MineTweaker3` |
| Removals shift list indices and corrupt running reactions ([MPRD-04](../../techguns-spec/08-machines-production/index.md#suspected-bugs)); null-ingredient crashes ([MPRD-05](../../techguns-spec/08-machines-production/index.md#suspected-bugs), [MPRD-06](../../techguns-spec/08-machines-production/index.md#suspected-bugs), [MPRD-25](../../techguns-spec/08-machines-production/index.md#suspected-bugs)) | Not reproduced. GT stores recipes by content, and our adder validates its arguments |

## Integration points

### GTTweaker: scripting GT recipe maps
- **Pack config:** none. The extracted pack has no `scripts/` folder under `reference/gtnh/2.9.0-RC-2/.minecraft/` (checked), and no MineTweaker config. Scripts are the pack maker's to add.
- **Source:**
  - GTTweaker is `@Mod(modid = "GTTweaker")` and registers one hand-written ZenClass per GT machine in its `init` ([GTTweaker#L74-L138](https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon/blob/2.3.4/src/main/java/gttweaker/GTTweaker.java#L74-L138)). For example `@ZenClass("mods.gregtech.Assembler")` ([Assembler.java](https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon/blob/2.3.4/src/main/java/gttweaker/mods/gregtech/machines/Assembler.java#L22-L26)).
  - Adds go through GT's recipe adder wrapped in `AddMultipleRecipeAction`, which extends `OneWayAction`, so **GTTweaker additions cannot be undone** by `/mt reload` ([AddMultipleRecipeAction#L21-L24](https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon/blob/2.3.4/src/main/java/gttweaker/mods/AddMultipleRecipeAction.java#L21-L24)).
  - There is **one generic remover**: `mods.gregtech.RecipeRemover.remove(String map, IIngredient[] inputs, IIngredient[] fluidInputs)` looks the map up by its `unlocalizedName` in `RecipeMap.ALL_RECIPE_MAPS` and removes the first matching recipe, undoably ([RecipeRemover#L24-L50](https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon/blob/2.3.4/src/main/java/gttweaker/mods/gregtech/RecipeRemover.java#L24-L50), [GTRecipeMap#L9-L19](https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon/blob/2.3.4/src/main/java/gttweaker/mods/gregtech/GTRecipeMap.java#L9-L19)).
  - There is **no generic adder**. A recipe map with no hand-written ZenClass cannot be added to from a script.
- **Consequences for GregGuns:**
  - Every GregGuns recipe in an existing GT map (Assembler, Forming Press, Canner, ...) is already scriptable with GTTweaker, with no GregGuns code.
  - A script that removes a GregGuns Assembler recipe works, because it matches by inputs.
  - Our own map `gregguns.recipe.reactionchamber` can be **removed from** with `RecipeRemover.remove("gregguns.recipe.reactionchamber", ...)` but not **added to**, unless GregGuns ships its own ZenClass.
- **GregGuns does:** ships `mods.gregguns.ReactionChamber.addRecipe(...)` as a ZenClass registered with `MineTweakerAPI.registerClass` from its `init`, only if `Loader.isModLoaded("MineTweaker3")`. Its arguments are: input `IIngredient`, focus `IItemStack`, fluid `ILiquidStack`, output stacks, ticks, required completion, preferred intensity, margin, risk name, `eut`. Adds are `OneWayAction`s to match GTTweaker's behavior. The class validates every argument and logs a MineTweaker error instead of throwing, which avoids [MPRD-05/06/25](../../techguns-spec/08-machines-production/index.md#suspected-bugs).
- **Pack-side change:** none.

### ModTweaker
- **Pack config:** none.
- **Finding:** ModTweaker 0.14.0 ([tree](https://github.com/GTNewHorizons/ModTweaker/tree/0.14.0/src/main/java/modtweaker2/mods)) has one package per supported mod, and none for GregTech. GT is scripted through GTTweaker only. No GregGuns integration.
- **Pack-side change:** none.

## Requirements

- **GGS-08-35** GregGuns registers no ZenClass for machines whose recipes are in GT maps.
- **GGS-08-36** `mods.gregguns.ReactionChamber.addRecipe` is registered only when `MineTweaker3` is loaded. With it absent, GregGuns starts and no MineTweaker class is loaded.
- **GGS-08-37** `addRecipe` rejects null, empty or out-of-range arguments (intensity outside 0-10, required completion above the tick count, unknown risk name) with a MineTweaker error, never an exception.
- **GGS-08-38** `RecipeRemover.remove("gregguns.recipe.reactionchamber", ...)` removes a recipe, and a Reaction Chamber that is mid-run is not corrupted (the run holds its own copy of the numbers).

## Tests

| Req | Verification |
|---|---|
| 35 | Code search for `@ZenClass` in `src/main`: only `ReactionChamber` |
| 36 | `runServer` with and without CraftTweaker on the classpath |
| 37 | Unit test of the argument validator (pure Java, no MineTweaker types) |
| 38 | `/gregguns test` scenario: start a run, remove its recipe, check the run finishes with its stored numbers |

## Open questions

- **Is a script API worth it at all?** The pack ships no scripts, so nobody is using MineTweaker for GT yet. If no pack maker asks for `addRecipe`, drop `mods.gregguns.ReactionChamber` and keep only the removal path. Low priority; it can wait until M4 closes.
