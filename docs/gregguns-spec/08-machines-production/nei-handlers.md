# NEI handlers

What GregGuns needs to write for NEI, which is almost nothing: GT turns every recipe map into an NEI tab and attaches the machines as catalysts.

- **Techguns baseline:** [nei.md](../../techguns-spec/08-machines-production/nei.md#3-handlers) (six handlers with IDs `Techguns:*`, registered from an optional `IConfigureNEI`)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md)
- **Pack mods:**
  - NotEnoughItems (`NotEnoughItems`, 2.8.155-GTNH)
  - GT5-Unofficial (`gregtech`, 5.09.54.205)
  - NEI Custom Diagram (`neicustomdiagram`, 1.8.36)
- **Roadmap:** M4

## What changes from Techguns

| Techguns handler | GregGuns |
|---|---|
| `Techguns:AmmoPress`, `Techguns:MetalPress`, `Techguns:ChemLab`, `Techguns:ChargingStation`, `Techguns:Fabricator` | None. The recipes are in GT's own tabs (Assembler, Forming Press, Chemical Reactor and so on), shown by GT's handler. See [recipe-map-replacements.md](recipe-map-replacements.md) |
| `Techguns:ReactionChamber` (custom drawing of RF, intensity and the danger icon) | One automatic GT tab for `gregguns.recipe.reactionchamber` |
| `NEITechgunsConfig` (`IConfigureNEI`) and the optional-dependency guard | None. NEI is reached only through GT, and GT is a hard dependency |
| NEI list index treated as a recipe index ([MPRD-22](../../techguns-spec/08-machines-production/index.md#suspected-bugs)); shared ingredient list grows ([MPRD-30](../../techguns-spec/08-machines-production/index.md#suspected-bugs)); `ItemStackOreDict` mutates shared stacks ([MPRD-23](../../techguns-spec/08-machines-production/index.md#suspected-bugs)) | Not reproduced, because GT's handler is not ours |

## Integration points

### GT5-Unofficial: one NEI handler per recipe category
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/NEI/handlerordering.csv`. Lines are `<handler ID>,<number>`, "Handlers will be sorted in order of number ascending", and "Any handlers that are missing from this file will be assigned to 0" (file header). GT's IDs are the recipe map names, for example `gt.recipe.largechemicalreactor,-10` and `gt.recipe.chemicalreactor,-5`. The file has 434 lines and no GregGuns entry, as expected.
  `reference/gtnh/2.9.0-RC-2/.minecraft/config/NEI/hiddenhandlers.cfg` lists 7 handlers to hide (BuildCraft, IC2 and Galacticraft ones). Nothing there touches a GregGuns ID.
- **Source:**
  - `NEIGTConfig#registerHandlers` creates a `GTNEIDefaultHandler` for **every** `RecipeCategory` whose map has `registerNEI` on ([NEIGTConfig#L103-L114](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/nei/NEIGTConfig.java#L103-L114)). `RecipeMapBuilder#disableRegisterNEI` turns it off ([RecipeMapBuilder#L425](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMapBuilder.java#L425)).
  - Every `RecipeMap` makes a default `RecipeCategory` named after the map ([RecipeCategory#L61-L67](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeCategory.java#L61-L67)), so the NEI handler ID and tab title key are the map's `unlocalizedName` ([GTNEIDefaultHandler#L408-L409](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/nei/GTNEIDefaultHandler.java#L408-L409), [#L473-L474](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/nei/GTNEIDefaultHandler.java#L473-L474)).
  - **The category records its owner mod** from `Loader.instance().activeModContainer()` when it is constructed ([RecipeCategory#L50-L51](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeCategory.java#L50-L51)). NEI's handler badge uses it ([NEIGTConfig#L257-L260](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/nei/NEIGTConfig.java#L257-L260)).
  - Catalysts need no code. `generateRecipeCatalystIndex` loops over `GregTechAPI.METATILEENTITIES`, takes each `RecipeMapWorkable` and attaches it to its map's categories, ordered by `getRecipeCatalystPriority` ([NEIGTConfig#L267-L281](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/nei/NEIGTConfig.java#L267-L281)). `MTEMultiBlockBase` supplies `getRecipeMap`, so the Reaction Chamber controller appears on its own tab with no registration.
  - The tab title is `StatCollector.translateToLocal(unlocalizedName)`, so the lang file needs a key named exactly like the map.
- **GregGuns does:**
  - Creates `reactionChamberRecipes` in a class whose static initialiser runs **during GregGuns' own FML event** (call `GregGunsRecipeMaps.init()` from our `preInit`). If the class first loads inside GT's `sAfterGTPreload` callback, the active mod container is GregTech's, and NEI shows the tab as a GregTech tab with the wrong badge.
  - Adds `gregguns.recipe.reactionchamber=Reaction Chamber` to `en_US.lang`.
  - Gives the map a `neiHandlerInfo` display stack (the controller) only if the inferred catalyst icon looks wrong in play.
  - Writes no `IConfigureNEI`, no `TemplateRecipeHandler`, no `INEIGuiHandler`.
- **Pack-side change:** none required. Optionally one row in `config/NEI/handlerordering.csv`, `gregguns.recipe.reactionchamber,0`, to pin the tab position next to GT's chemical tabs (use a value near `-5`). Missing rows default to 0, so this is cosmetic.

### NEI Custom Diagram
- **Pack config:** `config/NEI/handlerordering.csv` lines 9-11: `neicustomdiagram.diagramgroup.gregtech.circuits`, `.lenses` and `.oreprocessing`, all `-1000`. These are GT's diagram tabs.
- **Source:** the diagram groups are GT's. GT also tests `NEICustomDiagrams.isModLoaded()` when it builds some maps ([RecipeMaps#L1287](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1287)).
- **GregGuns does:** nothing in M4. A diagram would be a way to show ammo tiers or Reaction Chamber intensity, but that is a later nicety, so there is no requirement yet.
- **Pack-side change:** none.

## Requirements

- **GGS-08-31** GregGuns contains no NEI API code: no `IConfigureNEI`, no `TemplateRecipeHandler`, no NEI imports outside tests.
- **GGS-08-32** `reactionChamberRecipes` is created from a class that GregGuns' own `preInit` initialises, so its `RecipeCategory.ownerMod` is GregGuns.
- **GGS-08-33** The map name `gregguns.recipe.reactionchamber` has an `en_US.lang` entry.
- **GGS-08-34** Every recipe a GregGuns item is made by is visible in an NEI tab that GT or GregGuns registers, and every GregGuns MTE with a recipe map appears as a catalyst.

## Tests

| Req | Verification |
|---|---|
| 31 | Code search for `codechicken.nei` in `src/main` |
| 32 | Unit test (needs the Forge loader stub) or `runClient` manual check: the Reaction Chamber tab badge reads GregGuns |
| 33 | Lang test |
| 34 | Manual NEI check with `runClient`: search a GregGuns item, press R; see the Assembler tab; press U on the Reaction Chamber controller |

## Open questions

- **Ordering row.** Whether the optional `handlerordering.csv` row is worth a modpack PR. Probably not alone; bundle it with the other pack-side changes.
