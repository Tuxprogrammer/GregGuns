# Lifecycle and registration

Source: `java/techguns/Techguns.java`, `CommonProxy.java`, `client/ClientProxy.java`, `resources/mcmod.info`, `resources/META-INF/techguns_at.cfg`.

## Mod identity

| Fact | Value |
|---|---|
| Mod id | `Techguns` (capital T; the asset domain is still lower-case `techguns`) |
| Mod name | `Techguns` |
| Version constant | `0.1.2_alphatest4.1` |
| Declared dependencies (all soft) | `after:ForgeMultipart; after:CoFHCore; after:Chisel; after:TConstruct; after:FTBU` |
| Sided proxy | client `techguns.client.ClientProxy`, server `techguns.CommonProxy` |
| Network channel | `Techguns` (see [networking.md](networking.md)) |
| Main config file | `config/Techguns.cfg` (Forge's suggested file for the mod id). See [config.md](config.md) |
| Second config file | `config/Techguns_WeaponStats.cfg`, written by [`TGWeaponStats.init`](../02-guns/gun-lifecycle.md#13-details-confirmed-in-the-consistency-pass) (described in [02-guns](../02-guns/index.md)) |

Chisel is declared soft but is really a hard dependency: the client proxy builds a Chisel CTM renderer without checking for the mod, and `blocks/TGChiselBlocks` ([07-blocks](../07-blocks/index.md)) imports Chisel classes directly (bug [CORE-03](index.md#suspected-bugs)).

### mcmod.info

One entry. `modid` = `Techguns`, `name` = `Techguns`, `description` = `Techguns`. `version` and `mcversion` are build placeholders (`${version}`, `${mcversion}`). `authorList` holds a single string, `"pWn3d_1337, Th3_St4lk3r"`, not two entries. `url`, `updateUrl`, `credits`, `logoFile`, `screenshots` and `dependencies` are all empty.

### Access transformer (`META-INF/techguns_at.cfg`)

The file has one line: it makes `net.minecraft.world.World.chunkExists(int,int)` (returns boolean) public. The only code that would use it is a commented-out chunk-loaded check in [`BlockUtils.selectEntitiesWithinAABB`](utilities.md#blockutils) (see [utilities.md](utilities.md)). Nothing calls it, so the AT is dead weight. It was presumably meant to stop entity scans from loading chunks.

### Version file and update check

The repo root `version.txt` has two lines: line 1 is the *recommended* (stable) version (`0.1.1.3`) and line 2 is the *latest* (beta) version (`0.1.2_alphatest4.1`). The client update checker fetches this file from GitHub. Details are in [client-effects-audio.md](client-effects-audio.md#version-checker).

## Creative tab

There is exactly one creative tab:

| Field | Value |
|---|---|
| Internal label | `Techguns` |
| Icon | the Handcannon gun item ([`TGuns.handcannon`](../02-guns/gun-catalogue.md)) |
| Translated label | the tab overrides the label key to `techguns.itemGroup.Techguns`, which `en_US.lang` maps to **Techguns** |

Every Techguns item and block uses this tab.

## Lifecycle overview

```
preInit  → config, items, guns, weapon-stats config, metals/ores, oredict, projectile entities,
           metal recipes, tile entities, recipe sorter, armors, fluids, blocks, NPC entities,
           packets, FX list, ore worldgen, dispenser behaviours
init     → renderers, Forge-bus event handler, client handlers (HUD, keybinds), FML-bus tick handler,
           GUI handler
postInit → fluids/blocks/items post-init, item + machine recipes, gun post-init, entity spawns,
           foreign-steel nuggets, mod integrations, structure + oil worldgen, version check
```

### preInit, in exact order

1. [`TGConfig.init(event)`](config.md) loads `Techguns.cfg` ([config.md](config.md)).
2. Four debug/creative tools are constructed (classes are described in [06-items](../06-items/index.md)):

   | Field | Class | Unlocalized name | Icon |
   |---|---|---|---|
   | structureTestTool | `items.StructureTestTool` | [`structureTestTool`](../06-items/item-catalogue.md#4-debug--developer-items-registered-unconditionally-in-techgunsjava) | `techguns:structureTestTool` |
   | blockReaderTool | `items.BlockReader` | [`blockReaderTool`](../06-items/item-catalogue.md#4-debug--developer-items-registered-unconditionally-in-techgunsjava) | `techguns:blockReaderTool` |
   | buildingScanTool | `items.BuildingScanTool` | [`buildingScanTool`](../06-items/item-catalogue.md#4-debug--developer-items-registered-unconditionally-in-techgunsjava) | `techguns:structureScanTool` |
   | oreClusterDebugTool | `items.oreClusterDebugger` | (set by the class) | (set by the class) |

3. [`TGItems.init()`](../06-items/item-catalogue.md) ([06-items](../06-items/index.md)), then `TGuns.init()` ([02-guns](../02-guns/index.md)), then `TGWeaponStats.init(event)` ([02-guns](../02-guns/index.md), writes `Techguns_WeaponStats.cfg`).
4. The metal items are **always constructed** as plain [`GenericItem`](../06-items/special-items.md#base-classes)s (max stack 64) and only **registered** if their config flag allows it. Ore blocks are constructed only when their flag is on:

   | Field | Unlocalized | Texture | Registered as | Condition |
   |---|---|---|---|---|
   | ingotSteel | [`ingotSteel`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | `techguns:ingotSteel` | `ingotSteel` | [`addSteelIngot`](config.md#category-general) |
   | ingotCopper | [`ingotCopper`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | `techguns:ingotCopper` | `ingotCopper` | copper/tin/bronze group (see below) |
   | ingotTin | [`ingotTin`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | `techguns:ingotTin` | `ingotTin` | copper/tin/bronze group |
   | ingotBronze | [`ingotBronze`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | `techguns:ingotBronze` | `ingotBronze` | copper/tin/bronze group |
   | ingotLead | [`ingotLead`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | `techguns:ingotLead` | `ingotLead` | [`addLeadIngot`](config.md#category-general) |
   | nuggetIron | [`nuggetIron`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | `techguns:nuggetIron` | `nuggetIron` | [`addIronNuggets`](config.md#category-general) (registered **twice**, which is bug [CORE-01](index.md#suspected-bugs)) |
   | nuggetCopper | [`nuggetCopper`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | `techguns:copperNugget` | `nuggetCopper` | [`addCopperTinBronzeNuggets`](config.md#category-general) |
   | nuggetLead | [`nuggetLead`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | `techguns:nuggetLead` | `nuggetLead` | [`addLeadNuggets`](config.md#category-general) |
   | nuggetSteel | [`nuggetSteel`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | `techguns:nuggetSteel` | `nuggetSteel` | [`addSteelNuggets`](config.md#category-general) |
   | oreLead (block, [`GenericBlock`](../07-blocks/block-catalogue.md#conventions-and-defaults), rock) | `oreLead` | `techguns:oreLeadBlock` | `oreLead` | [`doOreGenLead`](config.md#category-general) |
   | oreTin (block) | `oreTin` | `techguns:oreTinBlock` | `oreTin` | [`doOreGenCopperTin`](config.md#category-general) |
   | oreCopper (block) | `oreCopper` | `techguns:oreCopperBlock` | `oreCopper` | `doOreGenCopperTin` |

   The "copper/tin/bronze group" is registered unless **all three** of [`addBronzeCrafting`](config.md#category-general), `addCopperTinBronzeNuggets` and `doOreGenCopperTin` are false.

   `GenericItem` and `GenericBlock` add a `techguns.` prefix to the unlocalized name, so the lang keys are `techguns.item.<name>.name` and `techguns.tile.<name>.name`. See [textures-sounds-lang.md](textures-sounds-lang.md).

5. The four tools are registered under the names `structureTestTool`, `blockReaderTool`, `buildingScanTool` and [`oreClusterDebugger`](../06-items/special-items.md#ore-cluster-debugger-oreclusterdebugger), followed by the conditional metals and ores above.
6. **OreDictionary registrations made in this file:**

   | Ore name | Entry | Condition |
   |---|---|---|
   | `itemGunpowder` | vanilla gunpowder | always |
   | `oreLead` | oreLead block | `doOreGenLead` |
   | `oreTin`, `oreCopper` | the ore blocks | `doOreGenCopperTin` |
   | `ingotSteel` | ingotSteel | `addSteelIngot` |
   | `ingotTin`, `ingotCopper`, `ingotBronze` | ingots | copper/tin/bronze group |
   | `ingotLead` | ingotLead | `addLeadIngot` |
   | `nuggetIron` | nuggetIron | `addIronNuggets` |
   | `nuggetCopper` | nuggetCopper | `addCopperTinBronzeNuggets` |
   | `nuggetLead` | nuggetLead | `addLeadNuggets` |
   | `nuggetSteel` | nuggetSteel | `addSteelNuggets` |

   All other oredict registrations happen in `TGItems` and [`TGBlocks`](../07-blocks/block-catalogue.md#registered-blocks) ([06-items](../06-items/index.md), [07-blocks](../07-blocks/index.md)).
7. **Projectile and FX entity registrations.** These use `EntityRegistry.registerModEntity` with a shared counter `modEntityID` that is pre-incremented, so IDs start at 1. Every entry sends velocity updates. [`TGEntities.init`](../10-entities-npcs/mob-catalogue.md#registration-javatechgunstgentitiesjava) (step 11) continues the same counter for the NPCs ([10-entities-npcs](../10-entities-npcs/index.md)).

   | ID | Entity name | Class | Track range | Update freq |
   |---|---|---|---|---|
   | 1 | GenericProjectile | entities.projectiles.GenericProjectile | 80 | 3 |
   | 2 | SimpleProjectile | SimpleProjectile | 80 | 3 |
   | 3 | FlamethrowerProjectile | FlamethrowerProjectile | 80 | 3 |
   | 4 | LaserProjectile | LaserProjectile | 80 | 3 |
   | 5 | LaserBeam | LaserBeam | 80 | 3 |
   | 6 | RocketProjectile | RocketProjectile | 80 | 3 |
   | 7 | NukeRocketProjectile | NukeRocketProjectile | 80 | 3 |
   | 8 | BiogunProjectile | BiogunProjectile | 80 | 3 |
   | 9 | TeslaProjectile | TeslaProjectile | 80 | 3 |
   | 10 | TeslaBeam | TeslaBeam | 80 | 3 |
   | 11 | GrenadeProjectile | GrenadeProjectile | 120 | 1 |
   | 12 | DummyMiningProjectile | MiningDummyProjectile | 80 | 3 |
   | 13 | CyberdemonBlasterProjectile | CyberdemonBlasterProjectile | 80 | 3 |
   | 14 | BlasterRifleProjectile | BlasterRifleProjectile | 80 | 3 |
   | 15 | PowerHammerProjectile | PowerHammerProjectile | 80 | 3 |
   | 16 | FlyingGibs | FlyingGibs | 80 | 3 |
   | 17 | ParticleSystem | client.particle.EntityParticleSystem | 150 | 3 |
   | 18 | LaserBeam2 | LaserBeam2 | 80 | 3 |
   | 19 | GrenadeLauncherProjectile | GrenadeLauncherProjectile | 120 | 1 |
   | 20 | SonicShotgunProjectile | SonicShotgunProjectile | 80 | 1 |
   | 21 | InvisibleProjectile | InvisibleProjectile | 80 | 3 |
   | 22 | MovingBeam | MovingBeam | 80 | 3 |
   | 23 | BeamProjectile | BeamProjectile | 80 | 3 |
   | 24 | FragGrenadeProjectile | FragGrenadeProjectile | 120 | 1 |
   | 25 | NeutrinoBeam | NeutrinoBeam | 80 | 3 |
   | 26 | EnergyBlastProjectile | EnergyBlastProjectile | 80 | 3 |

   Behaviour of each entity is described in [04-projectiles-fx](../04-projectiles-fx/index.md).

8. **Metal recipes registered in `Techguns.java`:**

   | Recipe | Type | Condition |
   |---|---|---|
   | 3× `ingotCopper` + 1× `ingotTin` (oredict) → 4 bronze ingots | shapeless ore recipe | `addBronzeCrafting` |
   | iron ingot → 1 steel ingot, 0.35 XP | furnace | [`addSteelCrafting`](config.md#category-general) |
   | 1 iron ingot → 9 iron nuggets; 3×3 iron nuggets → 1 iron ingot | shapeless / shaped (exact item) | `addIronNuggets` |
   | `ingotCopper` → 9 copper nuggets; 3×3 `nuggetCopper` → 1 Techguns copper ingot | ore recipes | `addCopperTinBronzeNuggets` |
   | `ingotLead` → 9 lead nuggets; 3×3 `nuggetLead` → 1 Techguns lead ingot | ore recipes | `addLeadNuggets` |
   | Techguns steel ingot → 9 steel nuggets; 3×3 steel nuggets → 1 Techguns steel ingot | ore recipes with exact items | `addSteelNuggets` **and** `addSteelIngot` |
   | copper ore → copper ingot, tin ore → tin ingot (0.7 XP each) | furnace | `doOreGenCopperTin` |
   | lead ore → lead ingot (0.7 XP) | furnace | `doOreGenLead` |

   When `addSteelNuggets` is on but `addSteelIngot` is off, the steel nugget recipes are built in postInit instead (see below).

9. **Tile entity registrations** (registry name = class simple name): [`BioblobTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-bioblobtileent), [`AmmoPressTileEnt`](../08-machines-production/framework.md#1-class-hierarchy), [`OreDrillTileEnt`](../09-machines-utility/ore-drill.md#11-legacy-prototype-dead-code), [`MetalPressTileEnt`](../08-machines-production/framework.md#1-class-hierarchy), [`TurretBaseEnt`](../09-machines-utility/turret.md#tile-entity-turretbaseent), [`CamoBenchTileEnt`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine), [`ChemLabTileEnt`](../08-machines-production/framework.md#1-class-hierarchy), [`OreDrillTileEntMaster`](../09-machines-utility/ore-drill.md#5-operation-oredrilltileentmaster), [`OreDrillTileEntSlave`](../09-machines-utility/index.md#key-facts), [`RepairBenchTileEnt`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine), [`ExplosiveChargeTileEnt`](../09-machines-utility/explosive-charges.md#nbt-explosivechargetileent), [`CampFlagTileEnt`](../09-machines-utility/shared-rendering.md#1-basicmachine-metas-owned-by-area-09), [`ExplosiveChargeAdvTileEnt`](../09-machines-utility/explosive-charges.md), [`ReactionChamberTileEntMaster`](../08-machines-production/framework.md#1-class-hierarchy), [`ReactionChamberTileEntSlave`](../08-machines-production/blocks-and-items.md#2-multiblockmachineblock-registry-name-machinemultiblock), [`FabricatorTileEntMaster`](../08-machines-production/framework.md#1-class-hierarchy), [`FabricatorTileEntSlave`](../08-machines-production/blocks-and-items.md#2-multiblockmachineblock-registry-name-machinemultiblock), [`ChargingStationTileEnt`](../08-machines-production/framework.md#1-class-hierarchy), [`ScrapperTileEnt`](../09-machines-utility/scrapper.md), [`TGSpawnerTileEnt`](../10-entities-npcs/spawning.md#3-monster-spawner-blocks-tgspawnertileent), [`TGChestTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-tgchesttileent). Machines are covered in [08](../08-machines-production/index.md)/[09](../09-machines-utility/index.md) and the rest in 07.
10. **RecipeSorter registrations.** All three are SHAPELESS and sorted `after:minecraft:shapeless`:
    - `Techguns:akimboGun` → `items.RecipeAkimboGun`
    - `Techguns:akimboSplit` → `items.RecipeAkimboSplit`
    - `Techguns:shapelessNBTcopy` → `items.ShapelessOreRecipeCopyNBT`

    (Recipe classes are covered in [02-guns](../02-guns/index.md) and [06-items](../06-items/index.md).)
11. Category inits, in order: [`TGArmors.init()`](../05-armor/armor-catalogue.md) ([05](../05-armor/index.md)), [`TGFluids.init()`](../06-items/fluids.md) ([07](../07-blocks/index.md)), `TGBlocks.init()` ([07](../07-blocks/index.md)), `TGEntities.init()` ([10](../10-entities-npcs/index.md)), [`TGPackets.init()`](networking.md) ([networking.md](networking.md)).
12. `proxy.loadFXList()`. On the client this loads the particle definition list ([`particles/FXList.txt`](../04-projectiles-fx/fx-catalogue.md), see [04](../04-projectiles-fx/index.md)). On the server it does nothing.
13. **Ore world generator:** if any of [`doOreGenTitanium`](config.md#category-general), `doOreGenLead`, `doOreGenCopperTin` or [`doOreGenUranium`](config.md#category-general) is on, `worldgen.OreGenerator` is registered with weight 1 and receives the four flags ([11-worldgen](../11-worldgen/index.md)).
14. **Dispenser behaviours:**

    | Item | Behaviour |
    |---|---|
    | `TGItems.sharedItem` (the shared metadata item) | `BehaviorProjectileDispenseDamagevalue` (see [utilities.md](utilities.md#dispenser-behaviour)): fires rockets, 40 mm grenades and nuke rockets; any other metadata is dropped like a normal item |
    | `TGuns.stielGranate` (stick grenade) | vanilla projectile dispense creating a [`GrenadeProjectile`](../04-projectiles-fx/projectiles.md#37-grenadeprojectile-family). Settings: damage and radius from the grenade item, speed 1.0, time-to-live 200 ticks, spread 0.05, gravity 0.015, no block damage, penetration 0, max bounces from the item, origin = dispense position |
    | `TGuns.fragGrenade` | same as the stick grenade but spawns a [`FragGrenadeProjectile`](../04-projectiles-fx/projectiles.md#1-registration) with a 120-tick time-to-live |

    The vanilla projectile dispenser launches with velocity 1.1 and inaccuracy 6.0, plays the bow/launch sound (1002) and shows smoke.

### init ("load"), in order

1. `proxy.registerRenderers()` (client only; index below).
2. A [`TechgunsEventhandler`](events.md#techgunseventhandler-forge-bus) instance is registered on the **Forge event bus** ([events.md](events.md)).
3. `proxy.RegisterClientHandlers()` (client only):
   - [`GuiOverlayAmmoCount`](events.md#guioverlayammocount-forge-bus-client) on the Forge bus ([input-and-hud.md](input-and-hud.md))
   - if config [`enableDebugKeybinds`](config.md#category-debug) is true: the debug [`KeyInputHandler`](events.md) on the FML bus, plus `debug.Keybinds.init()`
   - [`TGKeybinds`](input-and-hud.md#keybinds-tgkeybinds-client-only) on the FML bus, plus `TGKeybinds.init()`
4. A [`TechgunsTickHandler`](events.md#techgunstickhandler-fml-bus) instance is registered on the **FML bus** (`FMLCommonHandler.bus()`).
5. [`TechgunsGuiHandler`](../08-machines-production/gui-handler.md#1-techgunsguihandler-complete-gui-id-table) is registered as the mod's GUI handler.

### postInit, in order

1. `TGFluids.postInit()`, `TGBlocks.postInit()`, `TGItems.postInit()`
2. `TGItems.registerRecipes()` ([06](../06-items/index.md)), [`TGMachineRecipes.addRecipes()`](../08-machines-production/index.md#tgmachinerecipesaddrecipes-what-it-registers) ([08](../08-machines-production/index.md))
3. `TGuns.postInit()` ([02](../02-guns/index.md)), `TGEntities.postInit()` (10: NPC spawn registration)
4. **Steel nuggets made from another mod's steel:** if `addSteelNuggets` is on and `addSteelIngot` is off, and the oredict has at least one `ingotSteel`, two recipes are added: `ingotSteel` → 9 Techguns steel nuggets, and 3×3 `nuggetSteel` → the first `ingotSteel` registered in the oredict.
5. **TConstruct:** if loaded, `isTConstructLoaded` is set. On the client side (`event.getSide()==CLIENT`) `TechgunsTConstructIntegrationClient` is instantiated by reflection, otherwise `TechgunsTConstructIntegration`; then `init()` is called. Reflection errors are only printed. See [integrations.md](integrations.md).
6. **ThermalExpansion:** if loaded, `TechgunsThermalExpansionIntegration.init()` is called. It does nothing.
7. **FTBU:** if `FTBU` is loaded, [`TGNpcFactions.friendsAPI`](../10-entities-npcs/ai.md#1-factions-tgnpcfaction-tgnpcfactions) is set to the real FTBU friends adapter, otherwise to a dummy that says everyone is a friend.
8. **MineTweaker3:** if loaded, `TechgunsMineTweakerIntegration.init()` is called ([08-machines-production](../08-machines-production/index.md)).
9. If [`SpawnStructures`](config.md#category-general) is on, [`WorldGenTGStructureSpawn`](../11-worldgen/index.md#overview) is registered with weight 4.
10. If [`spawnOilHoles`](config.md#category-spawn-controll-sic) is on and `TGFluids.OIL` is not lava (lava is the fallback when no oil fluid exists), [`WorldGenOilHoles`](../11-worldgen/ore-generation.md#3-oil-holes-worldgenoilholes) is registered with weight 3.
11. `proxy.postInit()`. On the client, if [`EnableVersionChecker`](config.md#category-clientside) is on, a [`TGVersionChecker`](client-effects-audio.md#version-checker) runs on a background thread named "Techguns VersionChecker".

## GUI handler ID table (high level)

`gui.TechgunsGuiHandler`. The server returns a container and the client returns a GUI. The tile entity is taken from the x/y/z passed in.

| ID | Server container | Client GUI | Owner area |
|---|---|---|---|
| 0 | AmmoPressContainer | GuiAmmoPress | [08](../08-machines-production/index.md) |
| 1 | MetalPressContainer | GuiMetalPress | [08](../08-machines-production/index.md) |
| 2 | TurretBaseContainer | GuiTurretBase | [09](../09-machines-utility/index.md) |
| 3 | CamoBenchContainer | GuiCamoBench | [09](../09-machines-utility/index.md) |
| 4 | ChemLabContainer | GuiChemLab | [08](../08-machines-production/index.md) |
| 5 | RepairBenchContainer | GuiRepairBench | [09](../09-machines-utility/index.md) |
| 6 | ExplosiveChargeContainer | GuiExplosiveCharge | [09](../09-machines-utility/index.md) |
| 7, 8 | (none) | (none) | (unused) |
| 9 | ChargingStationContainer | GuiChargingStation | [09](../09-machines-utility/index.md) |
| 10 | ScrapperContainer | GuiScrapper | [08](../08-machines-production/index.md) |
| 11–15 | (none) | (none) | (unused) |
| 16 | OreDrillContainer | GuiOreDrill | [08](../08-machines-production/index.md) |
| 17 | TGPlayerContainer (player extra-slot inventory) | GuiTGPlayerInventory | player inventory GUI; tab/HUD side in [input-and-hud.md](input-and-hud.md) |
| 18 | ReactionChamberContainer | GuiReactionChamber | [08](../08-machines-production/index.md) |
| 19 | FabricatorContainer | GuiFabricator | [08](../08-machines-production/index.md) |
| 20 | TGChestContainer, built from the inventory the block at x/y/z returns; null if there is none | GuiTechgunsChest | [07](../07-blocks/index.md) |

ID 17 is hard-coded here even though config [`TechgunsGUI_TabID`](config.md#category-id-conflicts) (default 17) is what the inventory tab buttons send (bug [CORE-07](index.md#suspected-bugs)).

## Client renderer registration index (`ClientProxy.registerRenderers`)

This index only says what is bound to what. Models, textures and animations are described by the owning areas.

### Entity renderers

| Entity class | Renderer | Notes |
|---|---|---|
| GenericProjectile | RenderGenericBullet | [04](../04-projectiles-fx/index.md) |
| SimpleProjectile | RenderSimpleProjectile, scale 1.0, [`textures/entity/handgunBullet.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | [04](../04-projectiles-fx/index.md) |
| FlamethrowerProjectile, LaserProjectile, TeslaProjectile, CyberdemonBlasterProjectile, AlienBlasterProjectile, EnergyBlastProjectile, EntityParticleSystem, InvisibleProjectile, BeamProjectile | RenderDummy (draws nothing; visuals come from particles) | [04](../04-projectiles-fx/index.md) |
| LaserBeam | RenderGenericLaser | [04](../04-projectiles-fx/index.md) |
| TeslaBeam | RenderTeslaBeam (width 0.25) | [04](../04-projectiles-fx/index.md) |
| RocketProjectile | RenderRocketProjectile + ModelRocket + [`textures/guns/rocketlauncher.png`](../03-gun-visuals-audio/textures.md#measured-colours-all-62-textures) | [04](../04-projectiles-fx/index.md) |
| NukeRocketProjectile | same, with [`textures/guns/nukerocketlauncher.png`](../03-gun-visuals-audio/textures.md#measured-colours-all-62-textures) | [04](../04-projectiles-fx/index.md) |
| BiogunProjectile | RenderBiogunProjectile, [`textures/entity/bioblob.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | [04](../04-projectiles-fx/index.md) |
| RailgunProjectile | RenderRailgunProjectile | [04](../04-projectiles-fx/index.md) |
| PowerHammerProjectile | RenderPowerHammerProjectile | [04](../04-projectiles-fx/index.md) |
| GrenadeProjectile | RenderStielgranateProjectile + ModelStielgranate + [`textures/guns/stielgranate.png`](../03-gun-visuals-audio/rendering-framework.md#12-grenade-renderers-rendergrenade) | [04](../04-projectiles-fx/index.md) |
| FragGrenadeProjectile | RenderStielgranateProjectile + ModelFragGrenade(false) + [`textures/guns/fragGrenade_texture.png`](../03-gun-visuals-audio/rendering-framework.md#12-grenade-renderers-rendergrenade) | [04](../04-projectiles-fx/index.md) |
| BlasterRifleProjectile | RenderBlasterRifleProjectile | [04](../04-projectiles-fx/index.md) |
| FlyingGibs | RenderFlyingGibs | [04](../04-projectiles-fx/index.md) |
| LaserBeam2 | RenderLaserBeam2 | [04](../04-projectiles-fx/index.md) |
| GrenadeLauncherProjectile | RenderGrenadeLauncherProjectile, OBJ [`models/entity/launchergrenade.obj`](../10-entities-npcs/rendering-and-models.md#other-obj-files-in-modelsentity-in-this-folder-used-elsewhere), [`textures/entity/launchergrenade.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | [04](../04-projectiles-fx/index.md) |
| SonicShotgunProjectile | RenderSonicShotgunProjectile | [04](../04-projectiles-fx/index.md) |
| MovingBeam, NeutrinoBeam | RenderMovingBeam | [04](../04-projectiles-fx/index.md) |
| NPC classes (GenericNPC, ZombieSoldier, ZombiePigmanSoldier, CyberDemon, ArmySoldier, NPCTurret, AttackHelicopter, ZombieFarmer, ZombieMiner, Bandit, Outcast, SkeletonSoldier, PsychoSteve, DictatorDave, AlienBug(+Domesticated), Commando, StormTrooper, SuperMutant Basic/Heavy/Elite) | RenderGenericNPC / RenderSimpleTurret / RenderHelicopter / RenderAlienBug / RenderSuperMutant with the matching models and textures | 10 |

### Item renderers

Each gun has its own `IItemRenderer` subclass built from a model and a texture under `textures/guns/` ([03-gun-visuals-audio](../03-gun-visuals-audio/index.md)): handcannon, pistol, sawedoff, boltaction, flamethrower, m4, combatShotgun, lasergun, blasterRifle, rocketlauncher, biogun, thompson, revolver, ak47, as50, teslagun, pdw, lmg, m4_uq, grimreaper, goldenrevolver, pulseRifle, alienBlaster, cyberdemonBlaster, minigun, powerHammer, nukeLauncher, powerHammerAdv, aug, grenadeLauncher, sonicShotgun, chainsaw, scatterBeamGun, ndr, stielGranate, fragGrenade, akimbo_pistol, akimbo_revoler, akimbo_goldenrevolver, mac10, akimbo_mac10, mibGun, neutrinoGun, scar, vector and akimbo_mibGun. `TGItems.sharedItem` uses [`RenderGenericItem`](../06-items/special-items.md#3d-rendered-shared-items-rendergenericitem) ([06](../06-items/index.md)). The steam and T3 power armor pieces use [`RenderArmorItem`](../05-armor/index.md#contents) ([05](../05-armor/index.md)). The [`basicMachine`](../07-blocks/block-catalogue.md#registered-blocks) block item uses a [`RenderMachineBlockItem`](../08-machines-production/rendering.md#5-machine-item-rendering) array holding one model renderer per metadata 0–10 ([08](../08-machines-production/index.md)/[09](../09-machines-utility/index.md)). [`tgchest`](../07-blocks/block-catalogue.md#registered-blocks) and [`tgchest_weapon`](../07-blocks/block-catalogue.md#registered-blocks) use chest model item renderers ([07](../07-blocks/index.md)).

### Tile entity special renderers (08/09/07)

AmmoPress, MetalPress, ChemLab (RenderAmmoPress with the matching model); TurretBase; CamoBench and RepairBench (tesselator cube renderers); CampFlag; ExplosiveCharge and ExplosiveChargeAdv (RenderExplosiveCharge with two textures each); OreDrillTileEntMaster; ReactionChamberTileEntMaster (OBJ [`models/entity/reactionchamber.obj`](../10-entities-npcs/rendering-and-models.md#other-obj-files-in-modelsentity-in-this-folder-used-elsewhere)); ChargingStation; FabricatorTileEntMaster; TGChestTileEnt.

### Block render handlers (07)

These ISBRHs are created and their render IDs assigned to blocks: RenderLadder → [`ladder01`](../07-blocks/block-catalogue.md#registered-blocks); RenderLamp → [`lamp01`](../07-blocks/block-catalogue.md#registered-blocks); RenderGenericPane → [`camoNetPane`](../07-blocks/block-catalogue.md#registered-blocks); RenderDirectionalBlock → [`screen`](../07-blocks/block-catalogue.md#registered-blocks) and [`controlpanel`](../07-blocks/block-catalogue.md#registered-blocks); Chisel `RendererCTM` (unconditional); RenderOrecluster → [`oreCluster`](../07-blocks/block-catalogue.md#registered-blocks), [`oreCluster2`](../07-blocks/block-catalogue.md#registered-blocks), and [`oreClusterCustom`](../07-blocks/block-catalogue.md#registered-blocks)/[`oreClusterCustom2`](../07-blocks/block-catalogue.md#registered-blocks) when those config flags are on.

### Static armor model table

`ClientProxy.armorModels` is a static array of 12 biped models used by armor items ([05](../05-armor/index.md)):
- 0–1: SteamArmor(0/1)
- 2–3: T3PowerArmor(0/1)
- 4–6: ExoSuit(0, 1.0), (1, 0.5), (0, 0.75)
- 7: Beret
- 8–11: ArmorCoat(0, 1.0), (1, 0.51), (2, 0.49), (3, 0.75)

## Proxy API surface (CommonProxy, overridden by ClientProxy)

| Method | Server (CommonProxy) | Client (ClientProxy) |
|---|---|---|
| registerRenderers | nothing | renderer index above |
| spawnParticle(name, world, pos, motion) *(deprecated)* | nothing | spawns a hard-coded named particle ([client-effects-audio.md](client-effects-audio.md#legacy-named-particles-clientproxyspawnparticle-deprecated-but-live)) |
| setGunTextures(gun, path, n) | nothing | adds `techguns:<path>.png`, then `<path>_1.png` … `<path>_(n-1).png` to the gun's texture list (camo variants, 03) |
| loadFXList | nothing | [`FXList.loadFXList()`](../04-projectiles-fx/particle-system.md) |
| createFX / createFXOnEntity / createFXOnEntity_ID | nothing / null | builds particle systems from FXList and spawns them in the world (the _ID form also returns their entity IDs) |
| initChisel | nothing | instantiates a Chisel CTM renderer (no callers) |
| getPlayerClient | null | `Minecraft.thePlayer` |
| RegisterClientHandlers | nothing | HUD and keybind registration |
| setHasStepassist / setHasNightvision / getters | nothing / false | stores the HUD indicator flags |
| setFlySpeed(v) | nothing | sets the local player's capability fly speed |
| handleSoundEvent / playSoundOnEntity / playSoundOnPosition | nothing | plays a [`TGSound`](client-effects-audio.md#tgsound) ([client-effects-audio.md](client-effects-audio.md)) |
| handlePlayerGliding | nothing | starts or stops the glider loop sound |
| registerExtendedProperties | registers [`TechgunsExtendedPlayerProperties`](player-state.md#techgunsextendedplayerproperties) | registers the client subclass (which adds jetpack/glider sound handles) |
| isClientPlayerAndIn1stPerson | false | entity is the local player and the camera is first person |
| clientInRangeSquared(x, z, d²) | false | horizontal distance² from the local player to (x, z) ≤ d² |
| setFluidIcons | nothing | not overridden |
| postInit | nothing | starts the version checker |

ClientProxy also holds client-only state: `player_zoom` (FOV multiplier, default 1.0), `attackTimes` (recoil/reload timing per shooter), `entityDeathTypes` (pending death animations), `keyFirePressed`, `forwardKeyPressed` (unused), `lastReloadsoundPlayed`, `hasStepassist`, `hasNightvision`, `renderPass` (used by the multi-pass block renderers in [07](../07-blocks/index.md)), the `player_inv_texture` resource ([`textures/gui/TGPlayerInventory.png`](../08-machines-production/gui-textures.md#14-texturesguitgplayerinventorypng-owned-by-the-player-inventory-area-0105)), and the version checker. `resolvePlayerNameFromUUID` returns the last known user name from Forge's `UsernameCache`, or `UNKNOW_PLAYERNAME` (sic).
