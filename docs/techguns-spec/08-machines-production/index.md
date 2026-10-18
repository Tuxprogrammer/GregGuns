# 08 · Machines: production and shared framework

This area covers the Techguns 1.7.10 (v0.1.2 alpha) **machine framework**, which every machine block shares, and the six **recipe-processing machines**:

| Machine | Block | Type | Power | Purpose |
|---|---|---|---|---|
| Ammo Press | `basicMachine:0` | single block | 20 k RF buffer, 5 RF/t | metals + gunpowder → bullets / shells by build plan |
| Metal Press | `basicMachine:1` | single block | 20 k, 20 RF/t | two-input press: plates, wires, parts |
| Chemical Laboratory | `basicMachine:4` | single block, two tanks | 20 k, recipe RF/t | items + fluid → item and/or fluid |
| Charging Station | `basicMachine:9` | single block | 100 k, 20 RF/t or 100 RF/t item charging | charge energy items, "empty → full" recipes |
| Fabricator | [`machineMultiBlock`](../07-blocks/block-catalogue.md#registered-blocks) 3/4/5 | 2×2×2 multiblock | 100 k, 80 RF/t | four-ingredient high-tier component assembly |
| Reaction Chamber | `machineMultiBlock` 0/1/2 | 3×4×3 multiblock | 1 M, RF per 3 s reaction tick | focus beam + fluid bath + intensity tuning mini-game |

All machines use CoFH RF and Forge fluids, with optional NEI and MineTweaker 3 integration. The utility machines (turret, camo and repair benches, explosive charges, camp flag, scrapper, ore drill, chests, spawner, bioblob) are in [`09-machines-utility`](../09-machines-utility/index.md). They build on the classes documented in `framework.md`.

## Contents

| File | Topic |
|---|---|
| [framework.md](framework.md) | Base tile-entity classes: inventory, rotation, ownership and security, RF energy, redstone modes, the button protocol, operation state, the stack upgrade, sync, the multiblock base |
| [blocks-and-items.md](blocks-and-items.md) | [`basicMachine`](../07-blocks/block-catalogue.md#registered-blocks) and `machineMultiBlock`: every meta, hardness, placement and facing, wrench behaviour, drops, crafting recipes for the machines |
| [gui-handler.md](gui-handler.md) | Full GUI ID → container/GUI table, the base GUI classes, buttons, slot classes, [`ItemStackOreDict`](gui-handler.md#6-itemstackoredict-recipe-ingredient-wrapper) |
| [ammo-press.md](ammo-press.md) | Ammo Press |
| [metal-press.md](metal-press.md) | Metal Press: all 18 recipes and auto-split |
| [chem-lab.md](chem-lab.md) | Chemical Laboratory: all recipes, tanks, fluid containers, plus furnace recipes from `TGMachineRecipes` |
| [fabricator.md](fabricator.md) | Fabricator multiblock: structure, ports, recipes, renderer |
| [reaction-chamber.md](reaction-chamber.md) | Reaction Chamber multiblock: structure, reaction mechanics, risks and explosions, recipes, renderer |
| [charging-station.md](charging-station.md) | Charging Station |
| [rendering.md](rendering.md) | Shared TESRs and the machine item renderer |
| [nei.md](nei.md) | All NEI handlers |
| [minetweaker.md](minetweaker.md) | Every ZenScript class and method |
| [gui-textures.md](gui-textures.md) | All 14 `textures/gui/` files, region by region |
| [sounds.md](sounds.md) | All 35 `sounds/machines/` files and their events |
| [lang.md](lang.md) | Lang keys and hard-coded strings |

## `TGMachineRecipes.addRecipes()`: what it registers

It's called once during init. In order, it registers:

1. **Ammo Press input lists:** metal1 = [`ingotSteel`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), [`ingotLead`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems); metal2 = [`ingotCopper`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), [`ingotTin`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), `ingotIron`, [`ingotBronze`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems); powder = `itemGunpowder` (`ammo-press.md`).
2. **18 Metal Press recipes** (`metal-press.md` §2).
3. **Chem Lab recipes.** 17 base entries, some of them conditional on the Oil, Fuel and `itemBioFuel` / `dustCoal` ore names (`chem-lab.md` §4).
4. **Five furnace recipes** (`chem-lab.md` §4).
5. **Two Reaction Chamber beam foci and 7 Reaction Chamber recipes** (`reaction-chamber.md` §4–5).
6. **6 Fabricator recipes** (`fabricator.md` §4).
7. **1 Charging Station recipe** (`charging-station.md`).

## Cross-references

| Topic | Area |
|---|---|
| Machine block textures ([`textures/blocks/AmmoPress.png`](../07-blocks/textures.md#11-model-textures), `MetalPress.png`, `ChemLab.png`, `ChargingStation.png`, `Fabricator.png`, `rc_*`, `fabricator*`, `invisible`, `reactionChamber*.png`) | [`07-blocks`](../07-blocks/index.md) |
| Machine Upgrade, plates, wires, foci, ammo and other ingredient items | [`06-items`](../06-items/index.md) |
| `ChargeFlare` and `FragGrenadeExplosion` particle systems; [`textures/fx/heatray.png`](../04-projectiles-fx/textures.md#single-images) | [`04-projectiles-fx`](../04-projectiles-fx/index.md) |
| Packet registration ([`GUIButtonClick`](../01-core/networking.md#0--guibuttonclick-cs), [`PacketTGMessage`](../01-core/networking.md#15--packettgmessage-sc)), config loading | [`01-core`](../01-core/index.md) |
| Friends / factions ([`TGNpcFactions`](../10-entities-npcs/ai.md#1-factions-tgnpcfaction-tgnpcfactions), FTBUtilities) | [`10-entities-npcs`](../10-entities-npcs/index.md) |
| Ore clusters fed by `mods.techguns.OreCluster` | [`09-machines-utility`](../09-machines-utility/index.md) / [`11-worldgen`](../11-worldgen/index.md) |

## Coverage notes

- [`textures/entity/reactionchamber.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) and [`textures/entity/reactionchamberglass.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) are used only by the Reaction Chamber renderer, so they're described in `reaction-chamber.md` §7.
- The [`ModelAmmoPress`](../07-blocks/block-rendering-and-models.md#modelammopress-uv-12864-texture-ammopresspng), [`ModelMetalPress`](../07-blocks/block-rendering-and-models.md#modelmetalpress-uv-12864-texture-metalpresspng), [`ModelChemLab`](../07-blocks/block-rendering-and-models.md#modelchemlab-uv-12864-texture-chemlabpng), [`ModelChargingStation`](../07-blocks/block-rendering-and-models.md#modelchargingstation-uv-6464-texture-chargingstationpng) and [`ModelFabricator`](../07-blocks/block-rendering-and-models.md#modelfabricator-uv-128128-texture-fabricatorpng) classes were read for their animation logic and parts. Individual box coordinates aren't transcribed.
- [`RenderBlockTileEntModel`](rendering.md#3-renderblocktileentmodel-used-by-09s-turret-base-and-flag) and [`RenderTileEntityBlock`](rendering.md#4-rendertileentityblock-used-by-09s-camo-and-repair-benches) (used by [09](../09-machines-utility/index.md)'s machines) are summarised in `rendering.md`.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| MPRD-01 | Master `complete` change never triggers a client re-render | [`java/techguns/tileentities/MultiBlockMasterTileEntBase.java:40`](framework.md#master-multiblockmastertileentbase) | The field is assigned in the same expression it's compared against, so the "changed" test is always false and `causeRenderUpdate()` never runs. The controller block can keep the wrong icon (visible or invisible) until the chunk re-renders. | visual |
| MPRD-02 | `getPowerScaled` always returns the full value | [`java/techguns/tileentities/BasicMachineTileEntity.java:83`](framework.md#7-recipe-machine-state-basicmachinetileentity) | Computes max·limit/max instead of stored·limit/max, and is gated on `totalTime`. It's unused (the callers are commented out). | cosmetic |
| MPRD-03 | Reaction operation recipe-ID key mismatch | [`java/techguns/tileentities/ReactionChamberOperation.java:64`](reaction-chamber.md) / [`java/techguns/tileentities/ReactionChamberTileEntMaster.java:172`](framework.md#1-class-hierarchy) | Written as `recipeID`, read as `ID`, so every load resolves recipe index 0 (Wheat Seeds). That applies after a world reload and on every client sync. A running reaction then finishes with the wrong outputs, fluid use and risk. | gameplay |
| MPRD-04 | RC recipes referenced by list index | [`java/techguns/inventory/ReactionChamberRecipe.java:40`](reaction-chamber.md#recipe-fields-reactionchamberrecipe), [`java/techguns/plugins/minetweaker/ReactionChamberTweaker.java:201`](minetweaker.md#1-loading) | `ID` is the list size at creation, and `getById` indexes the live list. MineTweaker removals shift indices, so saved reactions pick the wrong recipe or throw `IndexOutOfBounds` on load. | crash |
| MPRD-05 | Fabricator NPE when a recipe has no plate | [`java/techguns/inventory/FabricatorRecipe.java:139`](fabricator.md) | The multiplier calculation dereferences the plate stack unconditionally. Wire and powder are null-guarded, plate isn't. A MineTweaker recipe with a null plate and an empty plate slot crashes the server tick. | crash |
| MPRD-06 | ChemLab MineTweaker add crashes on null fluids | [`java/techguns/plugins/minetweaker/ChemLabTweaker.java:80`](minetweaker.md#1-loading) | Reads `.amount` of the converted fluid stacks without a null check. Scripts must pass a 0-amount liquid instead of null. | crash |
| MPRD-07 | Multiblock controllers never see redstone | [`java/techguns/blocks/machines/MultiBlockMachineBlock.java:128`](blocks-and-items.md#2-multiblockmachineblock-registry-name-machinemultiblock) | Forwarding to the tile's `onNeighborBlockChange` is commented out, so `hasSignal` never changes. "High" mode disables the Fabricator or RC permanently; "Low" never stops them. | gameplay |
| MPRD-08 | Fabricator GUI opened without a security check | `java/techguns/blocks/machines/MultiBlockMachineBlock.java:304` and `:324` | Unlike the RC and basic machines, there's no `isUseableByPlayer` check and no denial message. The container is then closed by `canInteractWith`, which looks like a GUI flicker. | gameplay |
| MPRD-09 | Wrench ignores ownership | [`java/techguns/blocks/machines/BasicMachine.java:377`](blocks-and-items.md#1-basicmachine-registry-name-basicmachine) | Anyone holding a wrench can rotate a secured machine, or sneak-dismantle it and take it with its full contents and energy. | gameplay |
| MPRD-10 | Charging Station multiplier is an item count when the output is empty | [`java/techguns/tileentities/ChargingStationTileEnt.java:198`](framework.md#1-class-hierarchy) | Returns min(maxStack, outputSize·multi) instead of a multiplier. For recipes whose output is more than 1 item, it consumes more input than exists (the stack goes ≤0 and is deleted) and overproduces. | gameplay |
| MPRD-11 | Chem Lab swapped matches consume unswapped amounts | [`java/techguns/tileentities/ChemLabTileEnt.java:294`](framework.md#1-class-hierarchy) (also `:284`) | Matching honours `allowSwap`, but consumption and the multiplier check always take amount[0] from slot 0 and amount[1] from slot 1. Recipes with unequal amounts (MineTweaker) eat the wrong counts or delete short stacks. | gameplay |
| MPRD-12 | Chem Lab power ignores the stack multiplier | `java/techguns/tileentities/ChemLabTileEnt.java:206` | Every other machine multiplies RF/t by the multiplier; the Chem Lab processes up to 8× for the price of 1×. | gameplay |
| MPRD-13 | Charging Station resyncs every tick when its output is blocked | `java/techguns/tileentities/ChargingStationTileEnt.java:143` | A fully charged item that can't move to an occupied output slot triggers `needUpdate()` every tick, flooding description packets. | gameplay |
| MPRD-14 | A glass bottle in the flask slot blocks every Chem Lab recipe | [`java/techguns/inventory/ChemLabRecipes.java:231`](chem-lab.md) | No recipe uses the flask slot, and a non-empty flask slot fails every match. The slot only accepts glass bottles, so it's a trap for players. | gameplay |
| MPRD-15 | [`MachinesNeedNoPower`](../01-core/config.md#category-general) not fully honoured | `java/techguns/tileentities/ReactionChamberTileEntMaster.java:238`; `java/techguns/tileentities/ChargingStationTileEnt.java:123` | The RC still needs `RFTick` stored before it starts, and item-mode charging still draws from the buffer, so neither works without power even with the config on. | gameplay |
| MPRD-16 | Fabricator keeps processing while unformed | [`java/techguns/tileentities/FabricatorTileEntMaster.java:253`](framework.md#1-class-hierarchy) | `updateEntity` has no `isComplete()` check (the RC does). | gameplay |
| MPRD-17 | RC `instability` parameter unused | `java/techguns/inventory/ReactionChamberRecipe.java:30` | Documented as the per-tick drift chance and validated by MineTweaker, but the drift is hard-coded to 1/3 down, 1/3 up. | gameplay |
| MPRD-18 | RC final reaction tick never evaluated | `java/techguns/tileentities/ReactionChamberOperation.java:85` | The `duration < maxDuration` guard excludes the tick at `maxDuration`, so an N-tick recipe gives only N−1 completion chances. The Titanium Iron Ore recipe gets exactly 1. | gameplay |
| MPRD-19 | RC medium explosion places glass over the fluid | `java/techguns/tileentities/ReactionChamberTileEntMaster.java:607` | After the blast it puts a fluid source at the centre and a vanilla glass block above it. Looks unintended. | visual |
| MPRD-20 | Shift-click slot ranges off by one | [`java/techguns/inventory/AmmoPressContainer.java:149`](ammo-press.md), `:153`; [`java/techguns/inventory/MetalPressContainer.java:122`](gui-handler.md#1-techgunsguihandler-complete-gui-id-table), `:126` | The machine→player merge range includes the output and upgrade slots and excludes the last hotbar slot(s). The upgrade slot is treated as player inventory. Shift-clicking from the last one or two hotbar slots does nothing. | gameplay |
| MPRD-21 | Metal Press lets automation insert into its output slot | [`java/techguns/tileentities/MetalPressTileEnt.java:64`](framework.md#1-class-hierarchy) | The "can stack" rule applies to every slot, including output slot 2. Completion then adds output without re-checking space, so stacks can exceed 64. | gameplay |
| MPRD-22 | NEI handlers treat the NEI list index as a recipe index | [`java/techguns/nei/TechgunsChargingStationNEIRecipeHandler.java:62`](nei.md#charging-station-techgunschargingstationneirecipehandler-id-techgunschargingstation); [`java/techguns/nei/TechgunsReactionChamberNEIRecipeHandler.java:193`](nei.md#reaction-chamber-techgunsreactionchamberneirecipehandler-id-techgunsreactionchamber) | In filtered (usage or crafting) views, the RF amount and danger icon come from the wrong recipe. | visual |
| MPRD-23 | `ItemStackOreDict` mutates shared ore-dictionary stacks | `java/techguns/inventory/ItemStackOreDict.java:87` (also [`java/techguns/nei/TechgunsChemLabNEIRecipeHandler.java:67`](nei.md#chem-lab-techgunschemlabneirecipehandler-id-techgunschemlab)) | Sets `stackSize` on the live `ItemStack` objects returned by `OreDictionary.getOres`, changing them for every mod that reads them. | gameplay |
| MPRD-24 | Machine item renderer crashes on unknown meta | [`java/techguns/client/renderer/item/RenderMachineBlockItem.java:18`](rendering.md#5-machine-item-rendering) | Indexes an 11-entry array by damage. Any `basicMachine` stack with meta >10 (e.g. `/give`) throws in rendering. | crash |
| MPRD-25 | Fabricator MineTweaker puts null-stack ingredients into the slot whitelists | [`java/techguns/plugins/minetweaker/FabricatorTweaker.java:203`](minetweaker.md#1-loading) | When `amount_wire` or `amount_powder` ≤0, an empty `ItemStackOreDict` is added to a whitelist. The next comparison of two empty entries (`matches` → `OreDictionary.itemMatches(null, null)`) throws an NPE when a later recipe is added or removed. | crash |
| MPRD-26 | RC heat-ray beam drawn for every focus | [`java/techguns/client/renderer/tileentity/RenderReactionChamber.java:121`](reaction-chamber.md#7-visuals-renderreactionchamber) | A TODO was left in; the UV Emitter also shows the yellow heat-ray billboard. | visual |
| MPRD-27 | Fabricator form error names the Reaction Chamber | `java/techguns/blocks/machines/MultiBlockMachineBlock.java:551` | Reuses `techguns.tgmessage.0`, "Invalid Block for Reaction Chamber at". | cosmetic |
| MPRD-28 | Charging Station shows no progress while charging items | [`java/techguns/gui/GuiChargingStation.java:53`](charging-station.md#3-gui-guichargingstation-texturesguichargingstation_guipng) | Item mode leaves `totalTime` at 0, so the bar is a 1-px sliver while `isWorking()` is true. | visual |
| MPRD-29 | Player-inventory GUI ID is hard-coded | [`java/techguns/gui/TechgunsGuiHandler.java:69`](gui-handler.md#1-techgunsguihandler-complete-gui-id-table) and `:121` | The tab button uses the configurable [`TechgunsGUI_TabID`](../01-core/config.md#category-id-conflicts), but the handler only answers ID 17. Changing the config breaks the tab. | gameplay |
| MPRD-30 | Ammo Press NEI usage list grows duplicates | [`java/techguns/nei/TechgunsAmmoPressNEIRecipeHandler.java:94`](nei.md#ammo-press-techgunsammopressneirecipehandler-id-techgunsammopress) | One ingredient list is shared across the four plan entries and appended each iteration, so later entries cycle duplicate copies. | cosmetic |
| MPRD-31 | Hard-coded English GUI strings | [`java/techguns/gui/GuiAmmoPress.java:57`](ammo-press.md#5-gui-guiammopress-texture-texturesguiammopress_guipng); [`java/techguns/gui/GuiReactionChamber.java:49`](reaction-chamber.md#6-gui-guireactionchamber-texturesguireactionchamber_guipng) | "Building:" and "Empty" aren't localized (the Chem Lab uses `techguns.gui.empty`). | cosmetic |
| MPRD-32 | Typos in keys and strings | [`java/techguns/gui/GuiChemLab.java:55`](chem-lab.md#5-gui-guichemlab-texturesguichemlab_guipng); `resources/assets/techguns/lang/en_US.lang:590` | Key `techguns.chemlab.toogledrain` (other keys say "toggledrain"); text "Only onwer can access"; also `UNKNOW_PLAYERNAME` and the MineTweaker message "liquidLeve". | cosmetic |
| MPRD-33 | Legacy [`AmmoPressRecipe`](ammo-press.md) compares an item with itself | `java/techguns/inventory/AmmoPressRecipe.java:38` | The item-ID test compares `i1` with `i1`, so it's always true. The class is dead code (the press uses [`AmmoPressBuildPlans`](ammo-press.md)). | cosmetic |
| MPRD-34 | Scrapper GUI 10 is unreachable | `java/techguns/blocks/machines/BasicMachine.java:203`; `java/techguns/gui/TechgunsGuiHandler.java:65` | Tile creation for meta 10 is commented out and creative lists only up to meta 9, but GUI 10 and an item renderer slot remain. | cosmetic |
| MPRD-35 | Multiblock slaves can NPE when the master tile is missing | [`java/techguns/tileentities/FabricatorTileEntSlave.java:105`](blocks-and-items.md#2-multiblockmachineblock-registry-name-machinemultiblock); [`java/techguns/tileentities/ReactionChamberTileEntSlave.java:102`](blocks-and-items.md#2-multiblockmachineblock-registry-name-machinemultiblock) | `getMaster()` returns null when the stored master position no longer holds a master (e.g. removed without `breakBlock`), but every forwarded energy, inventory and fluid call dereferences it, so pipes or cables touching the slave crash. | crash |
