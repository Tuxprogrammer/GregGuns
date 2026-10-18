# NEI integration (`java/techguns/nei/`)

## 1. `NEITechgunsConfig` (`IConfigureNEI`, optional on mod `NotEnoughItems`)

- `getName()` returns `Techguns.MODNAME`; `getVersion()` returns `Techguns.VERSION`.
- `loadConfig()` creates six handlers and registers each one as **both** a recipe handler and a usage handler: Ammo Press, Metal Press, Chem Lab, Charging Station, Fabricator, Reaction Chamber.
- It sets the NEI GUI offset to (+5, +11) for `GuiAmmoPress` and `GuiChemLab`, so NEI's transfer-rect clicks line up. The Metal Press offset call is commented out.

## 2. Common conventions

- **Layout.** Every handler draws the machine's own GUI texture as the background. NEI's default background is the 166×65 region starting at texture (5,11). Every handler defines `ICON_OFFSET_X = −5` and `ICON_OFFSET_Y = −11`, so a GUI coordinate (x, y) is drawn at (x−5, y−11) in NEI. All the positions below are **GUI coordinates**.
- **Animated power bar.** Every handler draws one from `ammoPress_GUI.png`: sprite u251 v1, 4×48, at (8,17), cycling over 100 ticks with NEI direction 7 (drains or fills vertically).
- **Ingredient cycling.** Ore-dictionary ingredients cycle every 24 NEI ticks (`getCycledIngredients(cycleticks/24, …)`).
- **Transfer rects.** Each handler registers its own ID string (e.g. `Techguns:AmmoPress`). Clicking the rect in the machine GUI, or the arrow in NEI, lists all recipes of that machine.

## 3. Handlers

### Ammo Press: `TechgunsAmmoPressNEIRecipeHandler`, ID `Techguns:AmmoPress`

- **Name and GUI.** Recipe name `techguns.container.ammopress`. GUI class `GuiAmmoPress`, texture `ammoPress_GUI.png`.
- **Transfer rect.** (122,35) 12×23.
- **Extras.**
  - Animated arrow: sprite u180 v0, 10×21, at (123,36), direction 1 (fills downward), 100 ticks.
  - Text "500 RF" at (57,25) and "100 ticks" at (57,35), centred, white. The power is always 5·100.
- **Positions.** metal1 (100,17), metal2 (120,17), powder (140,17), result (120,60).
- **All recipes:** the four ammo outputs (9mm ×12, shotgun ×16, rifle ×8, high-calibre ×4), each with the full metal1/metal2/powder lists cycling.
- **Crafting lookup:** for any of the four ammo items, one recipe whose output stack size comes from the plan table.
- **Usage lookup:** an ingredient in the metal1, metal2 or powder list shows all four plans with that slot fixed to the ingredient (metal2 shown ×2). The fixed-ingredient list is reused and grows each iteration (MPRD-30).

### Metal Press: `TechgunsMetalPressNEIRecipeHandler`, ID `Techguns:MetalPress`

- **Name and texture.** Name `techguns.container.metalpress`. Texture `metalPress_GUI.png`. `getGuiClass` isn't overridden.
- **Transfer rect.** (122,35) 12×23.
- **Extras.**
  - The arrow sprite is taken from `ammoPress_GUI.png` (u180 v0, 10×21, at (123,36)).
  - Text "\<20·100> RF" ("2000 RF") and "100 ticks".
- **Positions.** input 1 (110,17), input 2 (130,17), output (120,60).
- **Lookups.**
  - All recipes: one entry per recipe, with ore lists cycling.
  - Crafting: the **first** recipe whose output matches (exact damage).
  - Usage: every recipe that uses the item in either slot.

  Swapped orders aren't listed separately.

### Chem Lab: `TechgunsChemLabNEIRecipeHandler`, ID `Techguns:ChemLab`

This handler extends `TechgunsLiquidTemplateRecipeHandler`.

- **Transfer rect.** (75,4) 53×30.
- **Positions.** input 1 (35,17), input 2 (57,17), flask (35,40), output (135,17). Ingredient display sizes are set to the recipe amounts. This **writes into shared ore stacks** (MPRD-23).
- **Extras.**
  - The 5-stage progress animation exactly as in the GUI (`chem-lab.md`), looping every 100 NEI ticks.
  - "\<RF/t> RF/t" centred at (105,50).
  - Input fluid drawn in the left tank (19,17) and output fluid in the right tank (158,17). The fill height is amount·50/**2000**, so 2000 mB looks full.
  - "\<amount> mB" with shadow under each tank, at (35,60) and (115,60).
  - Tank gauges from `chemLab_Gui.png` (u176 v32).
  - The power bar.
- **Tooltips.** Over the left tank (18,17) 12×52: the input fluid's translated name, or `techguns.chemlab.nofluidin` ("No Fluid Input"). Over the right tank (157,17): the output fluid name, or `techguns.chemlab.nofluidout` ("No Fluid Output").
- **Fluid navigation.** Left-click or the recipe key on a tank opens NEI "recipes" for that fluid. Right-click or the usage key opens "usages". Both use NEI input/output ID `liquid` with a `FluidStack`.
- **Lookups.**
  - Crafting by item: the **first** recipe with that output item.
  - Crafting by fluid: all recipes producing that fluid.
  - Usage by item: all recipes using it.
  - Usage by fluid: all recipes consuming that fluid.

### Charging Station: `TechgunsChargingStationNEIRecipeHandler`, ID `Techguns:ChargingStation`

- **Transfer rect.** (38,18) 28×12.
- **Extras.**
  - Animated lightning arrow from `ChargingStation_Gui.png`: u0 v167, 30×10, at (39,19), direction 0 (left→right), 100 ticks.
  - The power bar.
  - "\<chargeAmount> RF" at (57,40). It's looked up by NEI list index rather than recipe index (MPRD-22).
- **Positions.** input (19,17), output (68,17).
- **Lookups.** Crafting matches the output with exact damage. Usage matches the input through the ore-dict wrapper.

### Fabricator: `TechgunsFabricatorNEIRecipeHandler`, ID `Techguns:Fabricator`

- **Transfer rect.** (20,55) 92×12.
- **Extras.**
  - Animated golden arrow: u0 v167, 91×10, at (21,53), direction 0, 100 ticks.
  - The power bar.
  - "8000 RF" (80·100, constant) at (57,40).
- **Positions.** input (19,17), wire (47,17; omitted when not required), powder (68,17; omitted when not required), plate (89,17), output (116,50) with the output amount. Ingredient stacks are shown at the recipe amounts.
- **Lookups.** Crafting: all recipes whose output matches. Usage: all recipes that use the item in any slot.

### Reaction Chamber: `TechgunsReactionChamberNEIRecipeHandler`, ID `Techguns:ReactionChamber`

This handler extends `TechgunsLiquidTemplateRecipeHandler`.

- **Transfer rect.** (66,60) 102×14.
- **Positions.** input (35,17), focus (93,17), first output (134,17), extra outputs (152,17), (134,35), (152,35).
- **Extras:**

| Element | Detail |
|---|---|
| Completion bar | u0 v167, 105×6, at (67,61), direction 0, cycle `requiredCompletion·20` ticks |
| Duration bar | u0 v174, 105×6, at (67,68), cycle `ticks·20` ticks |
| Intensity bar | u190 v px, 6 × (40−px), at (114,16+px), px = (10−preferred)·4 |
| Fluid | the recipe fluid drawn in the tank at (19,17) with height level·5 px; gauge u176 v31 12×52 at (18,16); level marker u178 v32 8×1 at (18, 17+50−level·5) |
| Danger icon | u205 v(12·danger), 12×12, at (94,37). Danger 0 = NONE or BREAK_ITEM with margin 0 (green); 1 = NONE or BREAK_ITEM with margin > 0 (yellow); 2 = EXPLOSION_LOW (orange); 3 = any other risk (red). It's looked up with the NEI list index instead of the recipe (MPRD-22) |
| Text | "\<RFTick/60>" at (57,35) and "RF/T" at (57,45), centred; "\<level×10>%" at (34,58); preferred intensity at (121,16) |

- **Tooltips.**
  - Over the tank: the fluid name and "Consumed \<liquidConsumtion>mB" (`techguns.reactionTooltip.consume`), or "No Fluid Input".
  - Over the danger icon: "Unstable: Yes/No" (`techguns.reactionChamberNEI.Instability`, `techguns.yes`/`techguns.no`; yellow Yes when margin > 0, green No otherwise) and "Risk: \<RiskType text>" (`techguns.reactionChamberNEI.Danger`; the risk text comes from `RiskType.toString`, colour-coded green/yellow/red/white).
- **Fluid navigation.** Clicking the tank opens recipes or usages for (fluid, level·1000 mB).
- **Lookups.**
  - Crafting: all recipes that list the item as any output.
  - Usage: all recipes whose input matches.
  - Usage by fluid: all recipes using the fluid.
  - Crafting by fluid: none (the method body is empty).

## 4. `TechgunsLiquidTemplateRecipeHandler` (abstract)

- Routes NEI's `liquid` lookups (`FluidStack` argument) to the subclass's fluid methods, and the handler's own ID to "load all".
- Intercepts mouse clicks (button 0 → recipes, button 1 → usages) and the NEI recipe and usage keys. If the subclass reports a fluid under the cursor, it opens `GuiCraftingRecipe` or `GuiUsageRecipe` for `liquid`.
- Provides `isInRect`, its own `drawTexturedModalRect` (256×256 UV scale) and the static fluid-tiling helper, which works like the GUI's.
