# Weapon recipes

Every GregGuns weapon recipe: guns, akimbo variants, melee and tool weapons, grenades. The recipe adder, voltage gating and Assembly Line mechanics that used to live in `gt-recipes.md` are here too.

- **Techguns baseline:** [recipes.md](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) (shaped gun grids), [recipes.md § shapeless](../../techguns-spec/02-guns/recipes.md#shapeless--custom-recipes) (akimbo combine and split), [recipes.md § grenades](../../techguns-spec/02-guns/recipes.md#grenade-recipes), [06-items recipes](../../techguns-spec/06-items/recipes.md) (Combat Knife R82, Crowbar R87)
- **Design records:** [0001 Tier map](../../design/0001-tier-map.md), [0002 Machine deconfliction](../../design/0002-machine-deconfliction.md), [0003 Energy model](../../design/0003-energy-model.md), [0008 Exact-tier charging](../../design/0008-exact-tier-charging.md)
- **Pack mods:** GregTech (`gregtech`, GT5-Unofficial `5.09.54.205`), IC2 (`IC2`, API only)
- **Roadmap:** M2 (ballistic arsenal), M4 (production chain), M5 (energy and exotic weapons)
- **Related sheets:** [energy-electric-items.md](energy-electric-items.md) (capacity and tier of the battery component), [fluid-ammo.md](fluid-ammo.md) (tanks), [06-items components](../06-items/components-and-materials.md) and [ammo](../06-items/ammo.md) (the parts and ammo these recipes consume)

## What changes from Techguns

Techguns builds every gun at a crafting table from one grid and often consumes a full magazine, tank or Energy Cell. GregGuns drops the Energy Cell ([0010](../../design/0010-material-remap-and-tier-additions.md)): energy weapons are their own batteries. GregGuns builds the **final assembly** of each gun on a GT recipe map at the tier's minimum voltage, with a gate material from [0001](../../design/0001-tier-map.md). The Ammo Press, Fabricator and Metal Press are gone ([0002](../../design/0002-machine-deconfliction.md)).

| Techguns | GregGuns | Why |
|---|---|---|
| One shaped grid per gun, with "loaded" and "empty" variants | One recipe per gun, outputting an **unloaded** gun. Magazines, tanks, rockets and cells are never inputs. | A GT recipe cannot keep a consumed magazine's contents, and energy guns now hold their own charge ([energy-electric-items.md](energy-electric-items.md)). |
| Tier is set by which Techguns table you own | Tier is set by the recipe's `eut` and by its gate material | GT players read progress by voltage. |
| Plates, ingots, wire, sheets and circuits are Techguns items | `OrePrefixes` + `Materials` from GT, and `ItemList` components | [0002](../../design/0002-machine-deconfliction.md), [components-and-materials.md](../06-items/components-and-materials.md) |
| Flint and Steel as the igniter of the Hand Cannon, Revolver, Sawed Off, Flamethrower | Hand Cannon keeps it; the Revolver and Sawed Off drop it (the hammer is part of the mechanism); the Flamethrower uses a Kanthal fine-wire coil | A consumable vanilla tool is a poor machine input |
| Upgrade recipes (Golden Revolver, Infiltrator, Grim Reaper, Advanced Power Hammer) copy the input gun's NBT | Assembler recipes that consume the base gun. Camo is **not** kept. | A GT recipe cannot copy NBT (see [Open questions](#open-questions)) |
| Akimbo guns by shapeless crafting that merges NBT | Stay **crafting-table** recipes | Same reason: the NBT (ammo, camo, charge) has to survive |
| Alien Blaster has no recipe; Scatter Laser Rifle is a work-in-progress with no recipe | Both get a recipe at their [0001](../../design/0001-tier-map.md) tier (LuV, IV) | 0001: "build the Scatter Laser Rifle as a finished design" |

## Integration points

### GT5-Unofficial: the recipe adder

- **Pack config:** none. Recipes are code, not config.
- **Source:**
  - `GTValues.RA` is the recipe adder: [GTValues.java#L264](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/GTValues.java#L264). Its one method is `stdBuilder()`: [IGTRecipeAdder.java#L7](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/interfaces/internal/IGTRecipeAdder.java#L7).
  - The builder takes `itemInputs`, `circuit(n)`, `fluidInputs`, `itemOutputs`, `duration`, `eut` and `metadata`, then `addTo(map...)`: [GTRecipeBuilder.java#L1201-L1206](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L1201-L1206). Worked Assembler example: [AssemblerRecipes.java#L76-L82](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L76-L82).
  - Recipe voltages: `RECIPE_ULV` to `RECIPE_MAX` are `V x 30 / 32`, so `RECIPE_LV` = 30, `RECIPE_MV` = 120, `RECIPE_HV` = 480, `RECIPE_EV` = 1,920, `RECIPE_IV` = 7,680: [TierEU.java#L25-L39](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L25-L39).
  - `GTModHandler.addCraftingRecipe` is the crafting-table adder. Lower-case letters in the pattern are tools: `h` hard hammer, `f` file, `s` saw, `w` wrench, `d` screwdriver, `x` wire cutter, `k` knife, `c` crowbar, `i` soldering iron, `r` soft mallet, `m` mortar, `p` drawplate, `b` blade: [GTModHandler.java#L611-L625](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L611-L625).
- **GregGuns does:** registers recipes with `GTValues.RA.stdBuilder()` after GT's items and materials exist (post-init; GT is a hard dependency). Parts come from `GTOreDictUnificator.get(OrePrefixes..., Materials..., n)` or `ItemList`, never raw ore-dictionary strings. `eut` is always a `TierEU.RECIPE_*` constant. A part that resolves to `null` skips its recipe with a logged warning ([GGS-06-11](../06-items/components-and-materials.md#requirements)).
- **Pack-side change:** none.

### GT5-Unofficial: how voltage gates a recipe

- **Source:** a machine only starts a recipe when `availableEUt >= recipe EUt`: [ParallelHelper.java#L399-L401](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/ParallelHelper.java#L399-L401), [ProcessingLogic.java#L634-L636](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/logic/ProcessingLogic.java#L634-L636).
- **GregGuns does:** sets the tier through `eut`. A `RECIPE_MV` Assembler recipe cannot run in an LV machine. The gate material is a second, independent lock: it stops a player who has the voltage early (through a tier skip) from using cheaper metal.
- **Pack-side change:** none.

### GT5-Unofficial: precedent for electric tools and weapons

- **Source:** GT's electric tools are crafted at the **crafting table** from a tier motor or piston, a battery, and the tier metal's plate, screw and small gear. The Chainsaw LV is `"SXd" / "GMG" / "PBP"` with `Electric_Motor_LV`, `Battery_RE_LV_Lithium` and Steel parts; the MV and HV versions swap in Aluminium and Stainless Steel parts and the MV and HV motor and battery: [ProcessingToolHead.java#L164-L250](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingToolHead.java#L164-L250). The tool is created with `{ maxCharge, transfer limit, tier, -1 }` stats and no stored charge.
- **GregGuns does:** copies the **input mix** (tier motor or piston, battery, tier-metal plate, screw, small spring) but moves the final step to the Assembler, as [CONVENTIONS § Recipe sheets](../CONVENTIONS.md#recipe-sheets) requires from LV up. Costs scale the way GT's tool costs do: one battery per energy weapon, a tier-matched motor, piston or emitter where the weapon has that part.
- **Pack-side change:** none.

### GT5-Unofficial: the Assembly Line for LuV and ZPM

- **Source:**
  - `GTRecipeConstants.AssemblyLine` takes 4 to 16 item inputs, **1 to 4 fluid inputs** (so every Assembly Line recipe needs a fluid) and exactly one output. It needs `RESEARCH_ITEM` metadata, and `SCANNING` gives the research time and voltage: [GTRecipeConstants.java#L559-L567](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L559-L567).
  - Research is done in a Scanner. It matches the research item **ignoring NBT** (`areStacksEqual(recipe.mResearchItem, aInput, true)`), needs the item's stack size, and the result of `doAssemblyLineResearch` carries `mResearchItem.stackSize` as the amount to consume. So scanning a weapon uses one weapon up, and the data stick it writes can be copied: [ScannerHandlerLoader.java#L272-L297](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/ScannerHandlerLoader.java#L272-L297). That "consumed" reading comes from the `ALScannerResult` argument list; the Scanner's tick code that applies it was not read.
  - Worked examples: the LuV motor ([AssemblyLineRecipes.java#L61-L74](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblyLineRecipes.java#L61-L74)) uses `eut(RECIPE_IV)` and `Scanning(1 min, RECIPE_EV)`; the ZPM motor ([#L76-L92](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblyLineRecipes.java#L76-L92)) uses `eut(RECIPE_LuV)` and `Scanning(1 min, RECIPE_IV)`. The same pattern repeats for every LuV and ZPM pump, piston, robot arm, emitter, sensor and field generator in that file: `eut` is **one tier below the item**, and scanning voltage is one tier below that.
- **GregGuns does:** `.metadata(RESEARCH_ITEM, <weapon>)`, `.metadata(SCANNING, new Scanning(60 s, <tier below eut>))`, then `.addTo(AssemblyLine)`. The research item is an existing weapon, as the table in [LuV and ZPM](#luv-assembly-line-and-zpm-assembly-line) names. See the answer to the precedent question under [Open questions](#open-questions): GregGuns keeps `eut` at the item's own tier.
- **Pack-side change:** none.

## Conventions used in the recipe tables

- **Method** is stated once per tier. `Asm` is `RecipeMaps.assemblerRecipes` ([RecipeMaps.java#L1266](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266)). `AL` is the Assembly Line.
- **Inputs** use the CONVENTIONS notation. `plate.Steel` is `OrePrefixes.plate` + `Materials.Steel`; `circuit.MV` is `OrePrefixes.circuit` + `Materials.MV`. A number after `x` is the stack size.
- **Circuit tiers.** `circuit.LV` to `circuit.ZPM` are GT's per-tier circuits (basic, good, advanced, data, elite, master, ultimate) addressed as in [MTERecipeLoader.java#L3087](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3087).
- **GregGuns components** are written by their name in [components-and-materials.md](../06-items/components-and-materials.md) (for example Hardened Steel Barrel, Iron Receiver, Mechanical Parts T2). The [Assumed components](#assumed-components) table lists them all.
- **Programmed circuit.** `cfg n` is the `circuit(n)` selector. Recipes in the same tier that share a component set use different numbers so NEI shows them apart.
- **Time** is in seconds.
- **Output** is the unloaded gun, with no stored charge (energy guns: [GGS-02-06](#requirements)). Registry names are placeholders until M2 registers them.
- **Gate** is the 0001 gate input plus the voltage. Every recipe at a tier carries that tier's gate plate, so the dev self-check can test one rule.
- **Tier materials** (0001): Steel (LV), Aluminium (MV), Stainless Steel (HV), Titanium (EV), Tungstensteel (IV), HSS-G (LuV), Naquadah Alloy (ZPM).

## Steam / ULV

| Output | Method | Inputs | Gate | Replaces |
|---|---|---|---|---|
| Hand Cannon | Crafting table, `GTModHandler.addCraftingRecipe` | See grid below | `plate.Bronze` | [#1](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |

```
B  F  S
.  P  h
```

`B` Stone Barrel, `F` `flint_and_steel` (any damage), `S` Wooden Stock, `P` `plate.Bronze`, `h` the hard-hammer tool (takes one use). Row 2 is `. P h`. The pattern is not mirrored. No electric machine exists at this tier, so this follows the way GT makes its own early parts ([ProcessingRound.java#L37-L46](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingRound.java#L37-L46)). Wrought iron is an allowed alternative to bronze for `P` (ore-dict entry for either).

## LV (Steel)

Method: `Asm`, `eut(TierEU.RECIPE_LV)` (30 EU/t). Gate: `plate.Steel` and an LV Assembler. Precedent: the LV Chainsaw above, which uses `plate.Steel`, `screw.Steel` and `gearGtSmall.Steel`.

| Output | Inputs | Time | cfg | Notes and replaces |
|---|---|---|---|---|
| 9mm Pistol | Hardened Steel Barrel x1, Mechanical Parts T2 x1, `plate.Steel` x2, `plate.Rubber` x2, `screw.Steel` x4, `springSmall.Steel` x1 | 15 | 1 | Rubber stands for the Techguns plastic grip, because plastic is an MV material. [#2](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Revolver | Iron Barrel x1, Mechanical Parts x1, Wooden Stock x1, `plate.Steel` x2, `screw.Steel` x4, `springSmall.Steel` x1 | 15 | 2 | Flint and Steel dropped. [#3](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Sawed Off | Iron Barrel x2, Wooden Stock x1, `plate.Steel` x1, `screw.Steel` x2, `springSmall.Steel` x1 | 10 | 3 | Two barrels, as the Techguns grid. [#4](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Bolt Action Rifle | Hardened Steel Barrel x1, Iron Receiver x1, Wooden Stock x1, `lens.Glass` x1, `plate.Steel` x2, `screw.Steel` x4, `springSmall.Steel` x1 | 20 | 4 | `lens.Glass` is the scope. [#7](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Combat Knife | `plate.Steel` x2, `plate.Rubber` x1, `screw.Steel` x2 | 10 | 5 | Melee weapon. [R82](../../techguns-spec/06-items/recipes.md) |
| Crowbar | `stickLong.Steel` x2, `plate.Steel` x1, `screw.Steel` x1 | 10 | 6 | Melee and block-breaking tool. [R87](../../techguns-spec/06-items/recipes.md) |
| Stielgranate x16 | `plate.Steel` x2, `dust.Gunpowder` x4, `stick.Wood` x1 | 10 | 7 | **Not in 0001.** Placed at LV provisionally ([Open questions](#open-questions)). [Grenade table](../../techguns-spec/02-guns/recipes.md#grenade-recipes) |

## MV (Aluminium)

Method: `Asm`, `eut(TierEU.RECIPE_MV)` (120 EU/t). Gate: `plate.Aluminium` and an MV Assembler. `plate.Polyethylene` replaces Techguns' plastic sheet from here up.

| Output | Inputs | Time | cfg | Notes and replaces |
|---|---|---|---|---|
| Thompson SMG | Iron Barrel x1, Iron Receiver x1, Wooden Stock x1, `plate.Aluminium` x2, `screw.Aluminium` x4, `springSmall.Aluminium` x2 | 20 | 1 | [#5](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Mac10 | Hardened Steel Barrel x1, Steel Receiver x1, `plate.Polyethylene` x1, `plate.Aluminium` x2, `screw.Aluminium` x4, `springSmall.Aluminium` x2 | 20 | 2 | [#32](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Combat Shotgun | Hardened Steel Barrel x1, Steel Receiver x1, Plastic Gun Stock x1, `stick.Steel` x2, `plate.Aluminium` x2, `screw.Aluminium` x4, `springSmall.Aluminium` x2 | 25 | 3 | The steel sticks are the pump tube. [#8](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| M4 Assault Rifle | Hardened Steel Barrel x1, Steel Receiver x1, Plastic Gun Stock x1, `lens.Glass` x1, `plate.Aluminium` x2, `screw.Aluminium` x4, `springSmall.Aluminium` x2 | 25 | 4 | Redstone and iron nuggets of the Techguns grid are dropped. [#6](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| AK47 | Hardened Steel Barrel x1, Iron Receiver x1, Wooden Stock x1, `stick.Wood` x2, `plate.Aluminium` x2, `screw.Aluminium` x4, `springSmall.Aluminium` x2 | 25 | 5 | The wood sticks are the foregrip (`logWood` in Techguns). [#13](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Frag Grenade x16 | `plate.Aluminium` x2, `round.Steel` x16, `dust.TNT` x4 | 15 | 6 | Steel rounds are the fragments. [Grenade table](../../techguns-spec/02-guns/recipes.md#grenade-recipes) |
| Flashbang x16 | `plate.Aluminium` x2, `dust.Magnesium` x4, `dust.Gunpowder` x2 | 15 | 7 | **New design.** Techguns never registered the Flashbang ([GUN-24](../../techguns-spec/02-guns/index.md#suspected-bugs)). Magnesium is the flash charge. |
| Akimbo Pistols | Crafting table, 2x 9mm Pistol | n/a | n/a | See [Akimbo](#akimbo-combine-and-split) |
| Akimbo Revolvers | Crafting table, 2x Revolver | n/a | n/a | See [Akimbo](#akimbo-combine-and-split) |

Akimbo Pistols and Akimbo Revolvers sit at MV in 0001 although their two inputs are LV guns: the combine adds no new material. See [Open questions](#open-questions).

## HV (Stainless Steel)

Method: `Asm`, `eut(TierEU.RECIPE_HV)` (480 EU/t). Gate: `plate.StainlessSteel` and an HV Assembler. `StainlessSteel` plate, screw and small spring are the same prefixes GT's HV tools use.

| Output | Inputs | Time | cfg | Notes and replaces |
|---|---|---|---|---|
| AUG | Hardened Steel Barrel x1, Steel Receiver x1, Plastic Gun Stock x1, `lens.Glass` x2, `plate.StainlessSteel` x2, `screw.StainlessSteel` x4, `springSmall.StainlessSteel` x2 | 30 | 1 | [#26](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| SCAR | Hardened Steel Barrel x1, Hardened Receiver x1, Plastic Gun Stock x1, `lens.Diamond` x1, `plate.StainlessSteel` x2, `screw.StainlessSteel` x4, `springSmall.StainlessSteel` x2 | 30 | 2 | [#33](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Vector | Hardened Steel Barrel x1, Hardened Receiver x1, Plastic Gun Stock x1, `lens.Glass` x1, `plate.StainlessSteel` x2, `screw.StainlessSteel` x4, `springSmall.StainlessSteel` x2 | 30 | 3 | [#34](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| The Infiltrator | **M4 Assault Rifle x1** (any NBT), `plate.StainlessSteel` x2, `plate.Rubber` x2, `stickLong.StainlessSteel` x1, `lens.Glass` x2 | 30 | 4 | Upgrade: the rubber plates and long rod are the suppressor. Camo not kept. [#24](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| AS50 Sniper Rifle | Hardened Steel Barrel x2, Hardened Receiver x1, Plastic Gun Stock x1, `lens.Diamond` x1, `plate.StainlessSteel` x3, `screw.StainlessSteel` x6, `springSmall.StainlessSteel` x2 | 35 | 5 | [#14](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| LMG | Hardened Steel Barrel x1, Hardened Receiver x1, Plastic Gun Stock x1, `lens.Glass` x1, `plate.StainlessSteel` x3, `screw.StainlessSteel` x6, `springSmall.StainlessSteel` x3 | 35 | 6 | [#16](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Grenade Launcher | Hardened Steel Barrel x1, Steel Receiver x1, Plastic Gun Stock x1, `plate.StainlessSteel` x2, `screw.StainlessSteel` x4, `springSmall.StainlessSteel` x2 | 30 | 7 | [#27](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Rocket Launcher | Hardened Steel Barrel x2, Steel Receiver x1, `pipeSmall.StainlessSteel` x1, `plate.StainlessSteel` x2, `screw.StainlessSteel` x4 | 30 | 8 | Unloaded: the rocket is ammo. [#15](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Flamethrower | Pump Mechanism x1, Iron Receiver x1, Plastic Gun Stock x1, `wireFine.Kanthal` x4, `pipeSmall.StainlessSteel` x1, `plate.StainlessSteel` x2, `screw.StainlessSteel` x4 | 30 | 9 | Kanthal fine wire is the igniter coil. Fuel Tank is ammo ([fluid-ammo.md](fluid-ammo.md)). [#9](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Chainsaw | Mechanical Parts x4, Iron Receiver x1, `toolHeadChainsaw.Steel` x1, `ItemList.Electric_Piston_HV` x1, `plate.Polyethylene` x1, `plate.StainlessSteel` x2, `screw.StainlessSteel` x4 | 35 | 10 | GT's own chainsaw head. The piston stands for the fuel engine. Fuel Tank is ammo. [#28](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Power Hammer | Mechanical Parts x2, Iron Receiver x1, `ItemList.Electric_Piston_HV` x1, `plateDouble.StainlessSteel` x2, `plate.StainlessSteel` x2, `screw.StainlessSteel` x4 | 35 | 11 | Double plates are the striker. The Techguns grid used a vanilla piston. Compressed Air Tank is ammo. [#20](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Akimbo Mac10s | Crafting table, 2x Mac10 | n/a | n/a | See [Akimbo](#akimbo-combine-and-split) |

## EV (Titanium)

Method: `Asm`, `eut(TierEU.RECIPE_EV)` (1,920 EU/t). Gate: `plate.Titanium` and an EV Assembler. Energy guns also take `Materials.SolderingAlloy.getMolten(144)`, the way GT's circuit-bearing assembler recipes do. Energy guns are IC2 electric items of tier 4 ([0003](../../design/0003-energy-model.md), [0008](../../design/0008-exact-tier-charging.md)). The battery input is `ItemList.BatteryHull_EV_Full`, the table baseline in [energy-electric-items.md](energy-electric-items.md#gt-charging-and-the-tier-match). It is **consumed**, and the gun comes out uncharged, as GT's Chainsaw consumes its battery.

| Output | Inputs | Fluid | Time | cfg | Notes and replaces |
|---|---|---|---|---|---|
| Laser Rifle | Laser Barrel x1, Hardened Receiver x1, Plastic Gun Stock x1, Mechanical Parts T3 x1, `BatteryHull_EV_Full` x1, `circuit.EV` x1, `plate.Titanium` x2, `screw.Titanium` x4 | Solder 144 | 30 | 1 | Electric. [#11](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| PDW | Carbon Barrel x1, Carbon Receiver x1, Carbon Gunstock x1, `plate.Titanium` x2, `screw.Titanium` x4, `springSmall.Titanium` x2 | none | 30 | 2 | Ballistic. [#18](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Pulse Rifle | Carbon Barrel x1, Carbon Receiver x1, Carbon Gunstock x1, `lens.Diamond` x1, `circuit.EV` x1, `plate.Titanium` x2, `screw.Titanium` x4 | Solder 144 | 35 | 3 | Ballistic with a fire-control circuit. [#19](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Minigun | Hardened Steel Barrel x6, Hardened Receiver x1, `ItemList.Electric_Motor_EV` x1, `plate.Titanium` x3, `screw.Titanium` x6 | none | 40 | 4 | The motor is Techguns' Electric Engine. Ballistic, not an electric item. Ammo Drum is ammo. [#17](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Tesla Rifle | Carbon Receiver x1, Carbon Gunstock x1, `ItemList.Emitter_EV` x1, `BatteryHull_EV_Full` x1, `circuit.EV` x1, `plate.Titanium` x2, `screw.Titanium` x4 | Solder 144 | 30 | 5 | Electric. The emitter stands for the Techguns Coil. [#21](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Sonic Rifle | Sonic Emitter x1, Carbon Receiver x1, Carbon Gunstock x1, `BatteryHull_EV_Full` x1, `circuit.EV` x1, `plate.Titanium` x2, `screw.Titanium` x4 | Solder 144 | 30 | 6 | Electric. [#29](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Bio Gun | Pump Mechanism x1, Steel Receiver x1, Plastic Gun Stock x1, `pipeSmall.Titanium` x1, `plate.Titanium` x2, `screw.Titanium` x4 | none | 30 | 7 | Fluid gun, not electric. Bio Tank is ammo. [#10](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Power Hammer (Advanced) | **Power Hammer x1** (any NBT), `gem.Diamond` x3, `ItemList.Electric_Piston_EV` x1, `plate.Titanium` x2 | none | 30 | 8 | Upgrade; Techguns copied NBT. Camo not kept. [Shapeless table](../../techguns-spec/02-guns/recipes.md#shapeless--custom-recipes) |

## IV (Tungstensteel)

Method: `Asm`, `eut(TierEU.RECIPE_IV)` (7,680 EU/t). Gate: `plate.TungstenSteel` and an IV Assembler. Energy guns are tier 5 and take `ItemList.Energy_LapotronicOrb` as the battery (consumed; output uncharged) and `Materials.SolderingAlloy.getMolten(288)`.

| Output | Inputs | Time | cfg | Notes and replaces |
|---|---|---|---|---|
| Blaster Rifle | Laser Barrel x1, Carbon Receiver x1, Carbon Gunstock x1, `ItemList.Emitter_IV` x1, `Energy_LapotronicOrb` x1, `circuit.IV` x2, `lens.Glass` x1, `plate.TungstenSteel` x2, `screw.TungstenSteel` x4 | 40 | 1 | Electric. [#12](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| De-Atomizer Pistol | Laser Barrel x1, Sonic Emitter x1, Mechanical Parts T3 x1, `Energy_LapotronicOrb` x1, `circuit.IV` x1, `plate.Titanium` x2, `plate.TungstenSteel` x2, `screw.TungstenSteel` x4 | 40 | 2 | Electric. [#35](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Scatter Laser Rifle | Laser Barrel x2, Carbon Receiver x1, Carbon Gunstock x1, `Emitter_IV` x2, `Energy_LapotronicOrb` x1, `circuit.IV` x2, `plate.TungstenSteel` x3, `screw.TungstenSteel` x6 | 45 | 3 | **New design.** The five-beam gun is "[WIP]" in Techguns ([catalogue](../../techguns-spec/02-guns/gun-catalogue.md)); two barrels and two emitters pay for the extra beams. |
| Golden Revolver | **Revolver x1** (any NBT), `plate.Gold` x8, `plate.TungstenSteel` x2, `screw.TungstenSteel` x4 | 30 | 4 | Upgrade; camo not kept. [#22](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Akimbo De-Atomizer Pistols | Crafting table, 2x De-Atomizer Pistol | n/a | n/a | Electric. See [Akimbo](#akimbo-combine-and-split) |
| Akimbo Golden Revolvers | Crafting table, 2x Golden Revolver | n/a | n/a | See [Akimbo](#akimbo-combine-and-split) |

## LuV (Assembly Line) and ZPM (Assembly Line)

Method: `AL`, with the metadata below. `eut` is the item's own tier (see the proposal under [Open questions](#open-questions)): `RECIPE_LuV` for LuV items and `RECIPE_ZPM` for ZPM items. Scanning takes 1 minute at the tier below `eut`, as GT's motors do. Every recipe has 4 to 16 item inputs and 1 to 4 fluid inputs, as the adder requires.

Gate: `plate.HSSG` (LuV) or `plate.NaquadahAlloy` (ZPM), a tier circuit, and one tier-matched GT component. None of these can be made before the Assembly Line exists. Energy guns take the tier battery (`Energy_LapotronicOrb2` for LuV, tier 6, 1,000,000,000 EU; consumed, output uncharged).

Fluid column: `Sol n` is `Materials.SolderingAlloy.getMolten(n)`, `Lub n` is `Materials.Lubricant.getFluid(n)`.

| Output | Research item (scanned, consumed) | Inputs | Fluids | Time | Notes and replaces |
|---|---|---|---|---|---|
| Nether Blaster (LuV) | Blaster Rifle | Cybernetic Parts x4, Hardened Steel Barrel x1, Pump Mechanism x1, Carbon Receiver x1, `ItemList.Electric_Piston_LuV` x1, `circuit.LuV` x2, `plate.HSSG` x4, `screw.HSSG` x8 | Sol 576, Lub 250 | 45 | Ammo-fed (Nether Charge), not electric. [#25](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Alien Blaster (LuV) | De-Atomizer Pistol | `ItemList.Emitter_LuV` x2, `ItemList.Sensor_LuV` x1, Carbon Receiver x1, Carbon Gunstock x1, `Energy_LapotronicOrb2` x1, `circuit.LuV` x2, `plate.HSSG` x4, `screw.HSSG` x8 | Sol 576, Lub 250 | 60 | Electric, tier 6. **New design:** Techguns has no recipe (loot only, see [Open questions](#open-questions)). |
| Grim Reaper (LuV) | Rocket Launcher | **Rocket Launcher x4**, Carbon Receiver x1, `Electric_Piston_LuV` x1, `circuit.LuV` x1, `plate.HSSG` x4 | Lub 250 | 45 | Upgrade; four launchers are consumed as in [#23](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes). |
| Nuclear Death Ray (ZPM) | Alien Blaster | Rad Emitter x1, Carbon Receiver x1, Carbon Gunstock x1, `ItemList.Field_Generator_LuV` x1, `circuit.ZPM` x2, `plate.Lead` x4, `plate.NaquadahAlloy` x4, `screw.NaquadahAlloy` x8 | Sol 1152, Lub 500 | 60 | Fed by Nuclear Power Cells, not electric. The lead is the shielding. [#31](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |
| Nuke Launcher (ZPM) | Grim Reaper | Hardened Steel Barrel x2, Carbon Receiver x1, `ItemList.Electric_Motor_ZPM` x1, `circuit.ZPM` x2, `plate.Lead` x4, `plate.NaquadahAlloy` x4, `screw.NaquadahAlloy` x8 | Sol 1152, Lub 500 | 60 | Unloaded: the tactical nuke rocket is ammo. [#30](../../techguns-spec/02-guns/recipes.md#shaped-gun-recipes) |

The research items follow "scan the weapon one tier below": the Blaster Rifle (IV) for the Nether Blaster, the De-Atomizer Pistol (IV) for the Alien Blaster, the Rocket Launcher (HV, the base gun the Grim Reaper is made from) for the Grim Reaper, the Alien Blaster (LuV) for the Death Ray and the Grim Reaper (LuV) for the Nuke Launcher. A scanned weapon is matched ignoring its NBT, so a used or charged gun works.

## Akimbo combine and split

Akimbo recipes stay in the crafting table and keep Techguns' behaviour ([RecipeAkimboGun and RecipeAkimboSplit](../../techguns-spec/02-guns/recipes.md#shapeless--custom-recipes)), with the camo and ammo bug GUN-26 in [index.md](../../techguns-spec/02-guns/index.md#suspected-bugs) fixed. A GT recipe consumes its inputs and cannot copy their NBT, so a machine cannot do this job.

| Combine (2 identical guns in, 1 out) | Split (1 in, 2 out) | 0001 tier | Electric |
|---|---|---|---|
| Akimbo Pistols from 9mm Pistol | 9mm Pistol x2 | MV | no |
| Akimbo Revolvers from Revolver | Revolver x2 | MV | no |
| Akimbo Mac10s from Mac10 | Mac10 x2 | HV | no |
| Akimbo De-Atomizer Pistols from De-Atomizer Pistol | De-Atomizer Pistol x2 | IV | yes, tier 5 |
| Akimbo Golden Revolvers from Golden Revolver | Golden Revolver x2 | IV | no |

- **Gate.** The tier is carried by the inputs: each input is itself a gate-material gun. No other item is needed.
- **Electric akimbo.** The Akimbo De-Atomizer Pistols is an IC2 electric item of the same tier as one pistol, with **twice** the capacity. Combining stores the sum of both guns' EU. Splitting gives each gun half, rounded down for the first and the remainder for the second, as ammo is split in Techguns.
- **Camo and ammo.** The first gun's camo wins, as in Techguns. Magazine contents sum and split as in the baseline.

## Requirements

The first eight are carried over unchanged from the retired `gt-recipes.md`.

- **GGS-02-01.** Every gun and grenade recipe except the Hand Cannon and the akimbo combine and split is added with `GTValues.RA.stdBuilder()...addTo(<GT map>)`.
- **GGS-02-02.** Each recipe's `eut` is the `TierEU.RECIPE_*` constant of the gun's [0001](../../design/0001-tier-map.md) tier. No literal EU/t numbers. (Assembly Line recipes: see GGS-02-39.)
- **GGS-02-03.** Each recipe has at least one input whose material is the 0001 gate material for that tier, expressed through `GTOreDictUnificator`, `OrePrefixes` and `Materials`.
- **GGS-02-04.** LuV and ZPM guns are added to `AssemblyLine` with both `RESEARCH_ITEM` and `SCANNING` metadata.
- **GGS-02-05.** Metal bullet slugs use GT's `round` prefix. They are not a GregGuns item. (The Frag Grenade's fragments use it here.)
- **GGS-02-06.** Guns that are IC2 electric items are crafted **uncharged** (see [energy-electric-items.md](energy-electric-items.md)).
- **GGS-02-07.** Akimbo combine and split stay crafting-table recipes and keep the Techguns NBT behaviour, with bug GUN-26 fixed.
- **GGS-02-08.** A dev self-check iterates all registered gun recipes and fails if any recipe's `eut` exceeds its tier's `TierEU.RECIPE_*` value or if the recipe lacks a gate material.
- **GGS-02-35.** Every weapon in the [0001](../../design/0001-tier-map.md) Weapons column has a recipe in this sheet, and every recipe here outputs a 0001 weapon. The only exception is the Stielgranate ([Open questions](#open-questions)).
- **GGS-02-36.** No weapon recipe takes a magazine, tank, rocket, Nether Charge or Nuclear Power Cell as an input. Weapons are crafted unloaded.
- **GGS-02-37.** A weapon recipe below Assembly Line is a `RecipeMaps.assemblerRecipes` recipe with a programmed circuit, except the Hand Cannon (crafting table, `plate.Bronze` gate) and the akimbo recipes.
- **GGS-02-38.** Every electric weapon's recipe takes exactly one battery item of its tier from the baseline table in [energy-electric-items.md](energy-electric-items.md) (EV `BatteryHull_EV_Full`, IV `Energy_LapotronicOrb`, LuV `Energy_LapotronicOrb2`), and its output has a stored charge of 0.
- **GGS-02-39.** Assembly Line recipes use `eut` equal to the weapon's own tier constant (`RECIPE_LuV` or `RECIPE_ZPM`), a `SCANNING` voltage one tier below `eut`, at least one fluid input, and a research item that is a registered GregGuns weapon of a lower tier or an equal tier that is itself recipe-reachable earlier (no cycles).
- **GGS-02-40.** Upgrade recipes (Infiltrator, Golden Revolver, Grim Reaper, Power Hammer (Advanced)) match the base gun ignoring NBT, so a loaded, damaged or camouflaged base gun is accepted.
- **GGS-02-41.** The Akimbo De-Atomizer Pistols has twice the capacity of one pistol, and combine and split conserve the total stored EU.
- **GGS-02-42.** A recipe whose GT input resolves to `null` is skipped with a logged warning; the dev self-check lists every skipped weapon recipe so none disappears silently.

## Tests

- GGS-02-01, 03, 04, 37: unit test over the recipe registry (read back the `GTRecipe` objects from the `RecipeMaps` the builder wrote to).
- GGS-02-02, 08, 39: the same test, comparing `mEUt` with the tier table, and `AssemblyLine` metadata for LuV and ZPM.
- GGS-02-05: unit test that the Frag Grenade recipe contains `OrePrefixes.round`.
- GGS-02-35: unit test that diffs the set of recipe outputs against the Weapons column of the 0001 table (kept as a test fixture).
- GGS-02-36: unit test that no recipe input stack is a GregGuns ammo, tank or cell item.
- GGS-02-38: unit test that reads each electric recipe's input list and the output's stored charge.
- GGS-02-06, 07, 40, 41: `/gregguns test` scenario that crafts each upgrade and akimbo recipe and reads the NBT and charge.
- GGS-02-42: the self-check run in a `runServer` boot check with GT loaded.
- All: a `runServer` boot check that registration throws no exception.

## Assumed components

Components these recipes consume that are defined by [06-items](../06-items/components-and-materials.md). Names and registry names match the [component index](../06-items/recipes.md#component-index) exactly ([0010](../../design/0010-material-remap-and-tier-additions.md) decision 6).

| Component (name as used here) | Used by | Source in 06 | Status |
|---|---|---|---|
| Stone Barrel | Hand Cannon | components-and-materials (Barrels) | `part_barrel_stone` |
| Iron Barrel | Revolver, Sawed Off (x2), Thompson | same | `part_barrel_iron` |
| Hardened Steel Barrel | 9mm Pistol, Bolt Action, Mac10, Combat Shotgun, M4, AK47, AUG, SCAR, Vector, AS50 (x2), LMG, Grenade Launcher, Rocket Launcher (x2), Minigun (x6), Nether Blaster, Nuke Launcher (x2) | same | `part_barrel_hardened`, LV Steel |
| Carbon Barrel | PDW, Pulse Rifle | same | `part_barrel_carbon`; EV Titanium plus `plate.EpoxidFiberReinforced` |
| Laser Barrel | Laser Rifle, Blaster Rifle, De-Atomizer Pistol, Scatter Laser Rifle (x2) | same | `part_barrel_laser` |
| Wooden Stock | Hand Cannon, Revolver, Sawed Off, Bolt Action, Thompson, AK47 | components-and-materials (Stocks) | `part_stock_wooden` |
| Plastic Gun Stock | Combat Shotgun, M4, AUG, SCAR, Vector, AS50, LMG, Grenade Launcher, Flamethrower, Laser Rifle, Bio Gun | same | `part_stock_plastic`. Built from `plate.Polyethylene` (MV), so also the reason LV guns use rubber |
| Carbon Gunstock | PDW, Pulse Rifle, Tesla Rifle, Sonic Rifle, Blaster Rifle, Scatter Laser Rifle, Alien Blaster, Nuclear Death Ray | same | `part_stock_carbon` |
| Iron Receiver | Bolt Action, Thompson, AK47, Flamethrower, Chainsaw, Power Hammer | components-and-materials (Receivers) | `part_receiver_iron`. LV recipes use it with Steel plates, so the receiver's own material does not set the tier |
| Steel Receiver | Mac10, Combat Shotgun, M4, AUG, Grenade Launcher, Rocket Launcher, Bio Gun | same | `part_receiver_steel` |
| Hardened Receiver | SCAR, Vector, AS50, LMG, Minigun, Laser Rifle | same | `part_receiver_hardened`; Techguns "Obsidian Steel Receiver", renamed by [0010](../../design/0010-material-remap-and-tier-additions.md) |
| Carbon Receiver | Tesla, Sonic, Blaster Rifle, Scatter, Nether Blaster, Alien Blaster, Grim Reaper, Nuclear Death Ray, Nuke Launcher | same | `part_receiver_carbon`. Carbon is `plate.EpoxidFiberReinforced` plus Titanium ([0010](../../design/0010-material-remap-and-tier-additions.md)) |
| Mechanical Parts | Revolver, Chainsaw (x4), Power Hammer (x2) | components-and-materials (Mechanical Parts) | `part_mechanical_1` (Techguns "Mechanical Parts T1") |
| Mechanical Parts T2 | 9mm Pistol | same | `part_mechanical_2`; the Techguns "Obsidian Steel" variant, LV Steel under 0010 |
| Mechanical Parts T3 | Laser Rifle, De-Atomizer Pistol | same | `part_mechanical_3` |
| Pump Mechanism | Flamethrower, Bio Gun, Nether Blaster | same | `part_pump_mechanism` |
| Sonic Emitter | Sonic Rifle, De-Atomizer Pistol | same (wraps `Emitter_<tier>`) | `part_sonic_emitter` |
| Rad Emitter | Nuclear Death Ray | same | `part_rad_emitter` |
| Cybernetic Parts | Nether Blaster (x4) | same | `part_cybernetic` |

Other GT parts used (not GregGuns components): `ItemList.Electric_Motor_EV`, `Electric_Motor_ZPM`, `Electric_Piston_HV`, `Electric_Piston_EV`, `Electric_Piston_LuV`, `Emitter_EV`, `Emitter_IV`, `Emitter_LuV`, `Sensor_LuV`, `Field_Generator_LuV`, `BatteryHull_EV_Full`, `Energy_LapotronicOrb`, `Energy_LapotronicOrb2`, `circuit` LV to ZPM, and plates, screws, small springs, sticks, long sticks, rounds, lenses, fine wire and small pipes of the tier metals, plus `toolHeadChainsaw.Steel`. All are listed in `ItemList.java` and `OrePrefixes.java` at the pinned tag; per-material prefix availability (for example `plate.HSSG`, `pipeSmall.Titanium`, `wireFine.Kanthal`) is checked at boot (GGS-02-42), not assumed.

Every component above is in the 06 component index; none is missing. The ammo, tank, cell and rocket items the guns fire are in [ammo.md](../06-items/ammo.md) and [fluid-ammo.md](fluid-ammo.md), and no recipe here consumes them.

## Open questions

- **LuV and ZPM `eut` (answer proposed).** GT's LuV motor uses `RECIPE_IV` and its ZPM motor `RECIPE_LuV`, one tier below the item. The reason is a loop, not progression: the LuV Energy Hatch that would be needed to run an LuV-voltage Assembly Line recipe is itself built from LuV motors, so the motor cannot require LuV power. A weapon is the input to no hatch, machine or component, so there is no loop. **Proposal: keep 0001 as written** (`RECIPE_LuV` for LuV weapons, `RECIPE_ZPM` for ZPM weapons), with scanning one tier below, which is GT's own scanning-to-`eut` spacing. This keeps one rule across all tiers (GGS-02-02) and makes weapons a step later than the parts they need. If the team prefers GT's pattern, only GGS-02-39 and the `eut` of five recipes change. Needs: confirmation, no amendment to 0001.
- **Stielgranate.** [0001](../../design/0001-tier-map.md) lists the frag grenade and Flashbang but not the Stielgranate. It is placed at LV (Steel) here. Settle with a one-line addition to 0001, or drop the item.
- **Akimbo tier.** 0001 puts Akimbo Pistols and Revolvers at MV while their inputs are LV guns, and Akimbo Golden Revolvers at IV like their inputs. Crafting-table combine cannot enforce a different tier from its inputs, so the akimbo tier is really "the input gun's tier or later". If MV is wanted for the pistols, they need a machine recipe, which loses NBT. Needs: a 0001 note or a decision that the combine is effectively LV.
- **Upgrade recipes lose camo.** A machine recipe cannot copy the base gun's NBT. Options: keep Assembler recipes (camo lost, used here), or make upgrades crafting-table recipes with `RecipeBits.KEEPNBT`, which breaks the voltage rule. Needs: a decision; a Camo Bench re-apply ([09](../../techguns-spec/09-machines-utility/index.md)) softens the loss.
- **Alien Blaster as craftable.** Techguns gives it no recipe (loot and structures only). 0001 gives it LuV, which implies craftable, so it has a recipe here. If it is meant to be loot only, delete the row and the ZPM research chain of the Death Ray moves to the Grim Reaper.
- **Gate materials stay starting points.** [0001](../../design/0001-tier-map.md): re-check each against the live GTNH progression when M2 to M5 write them, especially Tungstensteel at IV, HSS-G at LuV and Naquadah Alloy at ZPM.
- **Receiver and barrel materials (closed).** Settled by [0010](../../design/0010-material-remap-and-tier-additions.md): hardware follows the tier material of the consuming gun, "Obsidian Steel Receiver" is "Hardened Receiver" (`part_receiver_hardened`), and carbon is `plate.EpoxidFiberReinforced` plus the tier material. Tank capacity (1,000 mB) and the dropped Energy Cell are settled by the same record.
- **EV, IV batteries as inputs.** `BatteryHull_EV_Full` is a charged item and is consumed; an empty `BatteryHull_EV` would be cheaper and equal in effect. Decide at M5 which the pack prefers.
