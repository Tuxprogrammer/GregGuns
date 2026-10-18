# 06-items: non-gun, non-armour items

This is a behavioural specification of Techguns 1.7.10 (v0.1.2 alpha, `VERSION = "0.1.2_alphatest4.1"`) for every item that is neither a gun (02) nor armour (05). It covers crafting components, metals, ammunition and magazines, tanks and cells, gun parts, machine upgrades, drill heads, turret armour, foci, the glider and oxygen mask, the camo net placer, the bunker door item, the acid bucket and fluids, the ore cluster scanner, tools (combat knife, crowbar), spawn eggs and the developer/debug items. It also lists every recipe and loot table that `TGItems` registers, all 177 textures in `textures/items/`, and the 15 sounds in `sounds/items/`.

## Contents

| File | What it holds |
|---|---|
| [item-catalogue.md](item-catalogue.md) | Every registered item and meta: registry id, meta, internal name, English name and lang key, stack size, slot type, OreDict, texture. Also config-dependent metals, debug items and spawn eggs. |
| [ammo.md](ammo.md) | Gun → ammo table (consumed per reload, clip size, empty-mag return), magazine refill and emptying, tanks/cells, loose-ammo sources, dispenser behaviour |
| [components-and-materials.md](components-and-materials.md) | Production tree: what is made from what (crafting, smelting, and one-line machine summaries) |
| [recipes.md](recipes.md) | Every recipe registered in `TGItems.registerRecipes()` with exact grids, plus related smelting and config-dependent metal recipes |
| [loot.md](loot.md) | All `ChestGenHooks` loot from `TGItems.postInit()` |
| [special-items.md](special-items.md) | Behaviour of the shared meta item (ammo bag, mag emptying, glider physics, tooltips, 3D renderers), the door item, camo net placer, medkit (unregistered), ore cluster scanner, tools, debug tools, spawn eggs |
| [fluids.md](fluids.md) | `TGFluids`: every fluid, its fallbacks and properties, fluid containers, the acid bucket, fluid textures |
| [textures.md](textures.md) | All 177 `textures/items/*.png`: size, depiction, palette, coverage, user |
| [sounds.md](sounds.md) | All 15 `sounds/items/*.ogg`: event names and triggers |
| [lang.md](lang.md) | Tooltip, chat and other lang strings; generated text; lang-file anomalies |

## Key architecture facts

- **Almost every item is one meta item.** `GenericItemShared`, registered as `Techguns:TechgunsAmmo`, holds 110 sub-items (metas 0–109). The meta is the position in `GenericItemShared.names[]`, which mirrors the registration order in `TGItems.init()`. Each meta has its own max stack size (`short[]`) and `TGSlotType` (`TGSlotType[]`). `TGItems` keeps template `ItemStack`s (one per meta) as public static fields. Code compares items by `getItemDamage()` against those templates. `TGItems.newStack(template, n)` copies a template with a new count.
- Thirteen stand-alone items are registered in `TGItems`. Seven of them are special-slot gear owned by 05. Metals and nuggets are registered in `Techguns.java`, only behind config flags. Four debug tools are registered unconditionally. Spawn eggs are registered by `TGEntities`.
- **Initialisation order**:
  1. preInit: `TGItems.init()` (items + OreDict), then metals and debug items, then `TGFluids.init()`, then `TGBlocks.init()` (which registers the acid fluid).
  2. postInit: `TGFluids.postInit()` (fluid fallbacks), then `TGItems.postInit()` (door link, OreDict fallbacks, acid bucket, fluid containers, loot), then `TGItems.registerRecipes()`, then `TGMachineRecipes.addRecipes()` (08).
- `ClientProxy` registers `RenderGenericItem` for the shared item. It draws 3D models for 8 metas (AR/AS50/LMG mags full and empty, rocket, nuke rocket).
- The shared item has a dispenser behaviour: rockets, nuke rockets and 40 mm grenades are fired as projectiles.
- There are no fuel (burn-time) registrations. The container items are: the acid bucket → bucket; Fuel Tank ↔ Fuel Tank (Empty) as a Forge fluid container.

## Coverage

| Scope | Read / covered |
|---|---|
| Java files in scope | 19 / 19 read in full: `TGItems`, `TGFluids`, `items/GenericItem`, `GenericItemBucket`, `GenericItemShared`, `ItemMedkit`, `ItemCamoNetPlacer`, `ItemTGDoor`, `BlockReader`, `TGMonsterPlacer`, `BuildingScanTool`, `StructureTestTool`, `OreClusterScanner`, `oreClusterDebugger`, `tools/GenericTool`, `TGCrowbar`, `TGPickaxe`, `TGSword`, `client/renderer/item/RenderGenericItem`. `items/TGSlotType` was also read in full and documented. `items/RecipeAkimboGun`, `RecipeAkimboSplit` and `ShapelessOreRecipeCopyNBT` sit in the same folder but were not named in this scope. They are gun-recipe helpers used only by `TGuns` (02) and are left to 02. |
| Supporting files read (not documented as owned) | `Techguns.java` (item/metal registration, inventory helpers), `TGMachineRecipes`, `TGEntities` (eggs), `TGuns` (ammo data), `GenericGun` (reload/empty mags), `RenderM4Mag` / `RenderAS50Mag` / `RenderLMGMag` / `RenderRocketAmmo`, `BehaviorProjectileDispenseDamagevalue`, `BlockFluidAcid`, `OreCluster`, `BasicMachine`, `TGConfig`, `TechgunsTickHandler`, `ClientProxy`, the lang file, `sounds.json` |
| Textures in `textures/items/` | 177 / 177 viewed and described (plus the 4 fluid textures in `textures/blocks/`) |
| Sounds in `sounds/items/` | 15 / 15 |

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ITEM-01 | Iron nugget registered twice | `java/techguns/Techguns.java:360` and `:374` | With `addIronNuggets=true`, `GameRegistry.registerItem(nuggetIron, "nuggetIron")` runs twice. FML rejects duplicate registration with an exception at startup. | crash |
| ITEM-02 | Config recipes output unregistered items | `java/techguns/Techguns.java:528`, `:548`, `:565` | Steel smelting (`addSteelCrafting`) outputs `ingotSteel` even when `addSteelIngot=false`. The lead-nugget → ingot recipe (`addLeadNuggets`) and lead-ore smelting (`doOreGenLead`) output `ingotLead` even when `addLeadIngot=false`. Those Item objects are never registered, which gives invalid item IDs when crafted, saved or synced. | crash |
| ITEM-03 | Liquid-coal fallback ignores oil | `java/techguns/TGFluids.java:72-76` | When `coal` is missing, it sets `OIL`, then unconditionally overwrites with `WATER`. The intended fallback chain is oil → water. The field is otherwise unused. | cosmetic |
| ITEM-04 | Empty bucket turns acid into water | `java/techguns/blocks/BlockFluidAcid.java:24` (Material.water), no `FillBucketEvent` handler anywhere | A vanilla empty bucket on an acid source block removes it and yields a **water** bucket. The acid bucket cannot be refilled in-world. | gameplay |
| ITEM-05 | No range check on shared-item meta | `java/techguns/items/GenericItemShared.java:174`, `:339`, `:351` | `getUnlocalizedName` and `onItemRightClick` index `names[]` / `slotType[]` directly. A stack with meta ≥ 110 (`/give`, a removed meta) throws `ArrayIndexOutOfBoundsException`, although the stack-limit and icon code guard against it. | crash |
| ITEM-06 | Emptying a magazine can void items | `java/techguns/items/GenericItemShared.java:291-334` | The return value (leftover count) of `addAmmoToPlayerInventory` is ignored, so with a full inventory the empty mag and bullets vanish. It also runs on both client and server. | gameplay |
| ITEM-07 | Ammo-add helper duplicates on partial fit | `java/techguns/Techguns.java:766-768` | If the stack only *partly* fits the ammo slots, the **whole original** stack (not the remainder) is then added to the main inventory. This duplicates items for mag emptying and for empty mags returned on gun reload. | gameplay |
| ITEM-08 | Icons requested for 3D-only metas | `java/techguns/items/GenericItemShared.java:134` | `registerIcons` asks for `m4Mag`, `m4MagEmpty`, `rocketAmmo`, `as50Mag`, `as50MagEmpty`, `lmgMag`, `lmgMagEmpty` and `rocketAmmoNuclear` PNGs that do not exist, giving missing-texture log errors. Any context that bypasses the IItemRenderer shows the purple-black checkerboard. | visual |
| ITEM-09 | Scanner charges energy on early release; scan is client-only | `java/techguns/items/OreClusterScanner.java:76-82` | 300 RF is extracted on **every** stop-using, even when released before the 40-tick scan, so no scan happens. The scan itself and its chat output run only on the client. | gameplay |
| ITEM-10 | Camo placer charges for occupied cells | `java/techguns/items/ItemCamoNetPlacer.java:150-162` | The 64-block limit and the inventory check use the full box volume, although only air cells are filled and consumed. A player with enough nets for the actual gaps can be refused. | gameplay |
| ITEM-11 | Spawn egg returns a stale entity | `java/techguns/items/TGMonsterPlacer.java:177-191` | `entityToSpawn` is an instance field. If the lookup fails, the previously spawned entity is returned, so the egg is consumed and the old entity may be renamed. | gameplay |
| ITEM-12 | Spawn egg liquid spawn and name | `java/techguns/items/TGMonsterPlacer.java:141`, `:220` | Spawning into liquid uses the integer block corner (no +0.5 centring). The display name is a hard-coded `"Spawn " + id`, not localized. | cosmetic |
| ITEM-13 | Destructive debug tools exposed | `java/techguns/Techguns.java:337-340`; `items/oreClusterDebugger.java:24-36`; `items/BuildingScanTool.java:96` | Debug items are always registered and appear in the creative tab. The Ore Cluster Debugger wipes a 32×32 column down to y=1 in one click. The Building Scan Tool writes `structure_scan.txt` to the server's working directory. | gameplay |
| ITEM-14 | Structure Test Tool mode labels/cycling | `java/techguns/items/StructureTestTool.java:30`, `:42`, `:123` | Mode 1 is labelled "Camp" but runs the city generator. The first sneak-cycle on a fresh tool jumps to mode 1, skipping 0. | cosmetic |
| ITEM-15 | Crowbar tool-class data is inconsistent | `java/techguns/items/tools/TGCrowbar.java:23-30` | `getToolClasses` advertises `"default"` and `"wrench"` at level 2, but `getHarvestLevel` is not overridden, so `wrench` reports −1 to Forge. `"default"` is not a real tool class. | cosmetic |
| ITEM-16 | Hard-coded tooltip text | `java/techguns/items/tools/TGSword.java:53-55`, `items/tools/TGPickaxe.java:51-53`, `items/OreClusterScanner.java:63` | `DamageType:`, `ArmorPen:` and the `RF` line are English literals, not lang keys. | cosmetic |
| ITEM-17 | Medkit implemented but never registered | `java/techguns/TGItems.java:467-468`; `items/ItemMedkit.java:73`, `:124` | A finished healing item with textures and lang is commented out. If enabled, its arrays are indexed by meta with no bounds check. | gameplay |
| ITEM-18 | UV Emitter unobtainable | `java/techguns/TGItems.java:396` | Shared meta 92 has no recipe or loot entry, so the Reaction Chamber's UV recipe (wheat seeds) is unreachable in survival. | gameplay |
| ITEM-19 | No fallback for oil/fuel; oil holes without oil | `java/techguns/TGFluids.java:59-60`; `java/techguns/Techguns.java:751` | `OIL` and `FUEL` stay `null` when absent. `spawnOilHoles && OIL != LAVA` is true for null, so oil-hole worldgen still runs and places oil clusters whose liquid was never configured (11). | gameplay |
| ITEM-20 | Nether Charge cannot go in ammo slots | `java/techguns/TGItems.java:356` | Nether Charge is Nether Blaster ammo but is registered with slot type NORMAL. Right-click-to-ammo-bag and the ammo slots reject it; it only works from the main inventory. | gameplay |
| ITEM-21 | Missing damage-type lang key | `java/techguns/damagesystem/DamageType.java:34` (key), `lang/en_US.lang` | `techguns.TGDamageType.UNRESISTABLE` has no translation and shows the raw key in tooltips. | cosmetic |
| ITEM-22 | Lang typos | `resources/assets/techguns/lang/en_US.lang:82`, `:12` | `lmgMagEmpty` has a leading space; `techguns.item.ironGunstock` lacks `.name`. Also duplicate and unused keys (see lang.md). | cosmetic |
| ITEM-23 | Unused key in recipe definitions | `java/techguns/TGItems.java:622`, `:649` | The Turret recipe defines key `b` and the Ore Drill Engine recipe defines key `p`, but neither appears in its grid. Harmless, but suggests the intended grids differ. | cosmetic |
