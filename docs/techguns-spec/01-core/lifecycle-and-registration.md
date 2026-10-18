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
| Second config file | `config/Techguns_WeaponStats.cfg`, written by `TGWeaponStats.init` (described in 02-guns) |

Chisel is declared soft but is really a hard dependency: the client proxy builds a Chisel CTM renderer without checking for the mod, and `blocks/TGChiselBlocks` (07-blocks) imports Chisel classes directly (bug CORE-03).

### mcmod.info

One entry. `modid` = `Techguns`, `name` = `Techguns`, `description` = `Techguns`. `version` and `mcversion` are build placeholders (`${version}`, `${mcversion}`). `authorList` holds a single string, `"pWn3d_1337, Th3_St4lk3r"`, not two entries. `url`, `updateUrl`, `credits`, `logoFile`, `screenshots` and `dependencies` are all empty.

### Access transformer (`META-INF/techguns_at.cfg`)

The file has one line: it makes `net.minecraft.world.World.chunkExists(int,int)` (returns boolean) public. The only code that would use it is a commented-out chunk-loaded check in `BlockUtils.selectEntitiesWithinAABB` (see [utilities.md](utilities.md)). Nothing calls it, so the AT is dead weight. It was presumably meant to stop entity scans from loading chunks.

### Version file and update check

The repo root `version.txt` has two lines: line 1 is the *recommended* (stable) version (`0.1.1.3`) and line 2 is the *latest* (beta) version (`0.1.2_alphatest4.1`). The client update checker fetches this file from GitHub. Details are in [client-effects-audio.md](client-effects-audio.md#version-checker).

## Creative tab

There is exactly one creative tab:

| Field | Value |
|---|---|
| Internal label | `Techguns` |
| Icon | the Handcannon gun item (`TGuns.handcannon`) |
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

1. `TGConfig.init(event)` loads `Techguns.cfg` ([config.md](config.md)).
2. Four debug/creative tools are constructed (classes are described in 06-items):

   | Field | Class | Unlocalized name | Icon |
   |---|---|---|---|
   | structureTestTool | `items.StructureTestTool` | `structureTestTool` | `techguns:structureTestTool` |
   | blockReaderTool | `items.BlockReader` | `blockReaderTool` | `techguns:blockReaderTool` |
   | buildingScanTool | `items.BuildingScanTool` | `buildingScanTool` | `techguns:structureScanTool` |
   | oreClusterDebugTool | `items.oreClusterDebugger` | (set by the class) | (set by the class) |

3. `TGItems.init()` (06-items), then `TGuns.init()` (02-guns), then `TGWeaponStats.init(event)` (02-guns, writes `Techguns_WeaponStats.cfg`).
4. The metal items are **always constructed** as plain `GenericItem`s (max stack 64) and only **registered** if their config flag allows it. Ore blocks are constructed only when their flag is on:

   | Field | Unlocalized | Texture | Registered as | Condition |
   |---|---|---|---|---|
   | ingotSteel | `ingotSteel` | `techguns:ingotSteel` | `ingotSteel` | `addSteelIngot` |
   | ingotCopper | `ingotCopper` | `techguns:ingotCopper` | `ingotCopper` | copper/tin/bronze group (see below) |
   | ingotTin | `ingotTin` | `techguns:ingotTin` | `ingotTin` | copper/tin/bronze group |
   | ingotBronze | `ingotBronze` | `techguns:ingotBronze` | `ingotBronze` | copper/tin/bronze group |
   | ingotLead | `ingotLead` | `techguns:ingotLead` | `ingotLead` | `addLeadIngot` |
   | nuggetIron | `nuggetIron` | `techguns:nuggetIron` | `nuggetIron` | `addIronNuggets` (registered **twice**, which is bug CORE-01) |
   | nuggetCopper | `nuggetCopper` | `techguns:copperNugget` | `nuggetCopper` | `addCopperTinBronzeNuggets` |
   | nuggetLead | `nuggetLead` | `techguns:nuggetLead` | `nuggetLead` | `addLeadNuggets` |
   | nuggetSteel | `nuggetSteel` | `techguns:nuggetSteel` | `nuggetSteel` | `addSteelNuggets` |
   | oreLead (block, `GenericBlock`, rock) | `oreLead` | `techguns:oreLeadBlock` | `oreLead` | `doOreGenLead` |
   | oreTin (block) | `oreTin` | `techguns:oreTinBlock` | `oreTin` | `doOreGenCopperTin` |
   | oreCopper (block) | `oreCopper` | `techguns:oreCopperBlock` | `oreCopper` | `doOreGenCopperTin` |

   The "copper/tin/bronze group" is registered unless **all three** of `addBronzeCrafting`, `addCopperTinBronzeNuggets` and `doOreGenCopperTin` are false.

   `GenericItem` and `GenericBlock` add a `techguns.` prefix to the unlocalized name, so the lang keys are `techguns.item.<name>.name` and `techguns.tile.<name>.name`. See [textures-sounds-lang.md](textures-sounds-lang.md).

5. The four tools are registered under the names `structureTestTool`, `blockReaderTool`, `buildingScanTool` and `oreClusterDebugger`, followed by the conditional metals and ores above.
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

   All other oredict registrations happen in `TGItems` and `TGBlocks` (06-items, 07-blocks).
7. **Projectile and FX entity registrations.** These use `EntityRegistry.registerModEntity` with a shared counter `modEntityID` that is pre-incremented, so IDs start at 1. Every entry sends velocity updates. `TGEntities.init` (step 11) continues the same counter for the NPCs (10-entities-npcs).

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

   Behaviour of each entity is described in 04-projectiles-fx.

8. **Metal recipes registered in `Techguns.java`:**

   | Recipe | Type | Condition |
   |---|---|---|
   | 3× `ingotCopper` + 1× `ingotTin` (oredict) → 4 bronze ingots | shapeless ore recipe | `addBronzeCrafting` |
   | iron ingot → 1 steel ingot, 0.35 XP | furnace | `addSteelCrafting` |
   | 1 iron ingot → 9 iron nuggets; 3×3 iron nuggets → 1 iron ingot | shapeless / shaped (exact item) | `addIronNuggets` |
   | `ingotCopper` → 9 copper nuggets; 3×3 `nuggetCopper` → 1 Techguns copper ingot | ore recipes | `addCopperTinBronzeNuggets` |
   | `ingotLead` → 9 lead nuggets; 3×3 `nuggetLead` → 1 Techguns lead ingot | ore recipes | `addLeadNuggets` |
   | Techguns steel ingot → 9 steel nuggets; 3×3 steel nuggets → 1 Techguns steel ingot | ore recipes with exact items | `addSteelNuggets` **and** `addSteelIngot` |
   | copper ore → copper ingot, tin ore → tin ingot (0.7 XP each) | furnace | `doOreGenCopperTin` |
   | lead ore → lead ingot (0.7 XP) | furnace | `doOreGenLead` |

   When `addSteelNuggets` is on but `addSteelIngot` is off, the steel nugget recipes are built in postInit instead (see below).

9. **Tile entity registrations** (registry name = class simple name): `BioblobTileEnt`, `AmmoPressTileEnt`, `OreDrillTileEnt`, `MetalPressTileEnt`, `TurretBaseEnt`, `CamoBenchTileEnt`, `ChemLabTileEnt`, `OreDrillTileEntMaster`, `OreDrillTileEntSlave`, `RepairBenchTileEnt`, `ExplosiveChargeTileEnt`, `CampFlagTileEnt`, `ExplosiveChargeAdvTileEnt`, `ReactionChamberTileEntMaster`, `ReactionChamberTileEntSlave`, `FabricatorTileEntMaster`, `FabricatorTileEntSlave`, `ChargingStationTileEnt`, `ScrapperTileEnt`, `TGSpawnerTileEnt`, `TGChestTileEnt`. Machines are covered in 08/09 and the rest in 07.
10. **RecipeSorter registrations.** All three are SHAPELESS and sorted `after:minecraft:shapeless`:
    - `Techguns:akimboGun` → `items.RecipeAkimboGun`
    - `Techguns:akimboSplit` → `items.RecipeAkimboSplit`
    - `Techguns:shapelessNBTcopy` → `items.ShapelessOreRecipeCopyNBT`

    (Recipe classes are covered in 02-guns and 06-items.)
11. Category inits, in order: `TGArmors.init()` (05), `TGFluids.init()` (07), `TGBlocks.init()` (07), `TGEntities.init()` (10), `TGPackets.init()` ([networking.md](networking.md)).
12. `proxy.loadFXList()`. On the client this loads the particle definition list (`particles/FXList.txt`, see 04). On the server it does nothing.
13. **Ore world generator:** if any of `doOreGenTitanium`, `doOreGenLead`, `doOreGenCopperTin` or `doOreGenUranium` is on, `worldgen.OreGenerator` is registered with weight 1 and receives the four flags (11-worldgen).
14. **Dispenser behaviours:**

    | Item | Behaviour |
    |---|---|
    | `TGItems.sharedItem` (the shared metadata item) | `BehaviorProjectileDispenseDamagevalue` (see [utilities.md](utilities.md#dispenser-behaviour)): fires rockets, 40 mm grenades and nuke rockets; any other metadata is dropped like a normal item |
    | `TGuns.stielGranate` (stick grenade) | vanilla projectile dispense creating a `GrenadeProjectile`. Settings: damage and radius from the grenade item, speed 1.0, time-to-live 200 ticks, spread 0.05, gravity 0.015, no block damage, penetration 0, max bounces from the item, origin = dispense position |
    | `TGuns.fragGrenade` | same as the stick grenade but spawns a `FragGrenadeProjectile` with a 120-tick time-to-live |

    The vanilla projectile dispenser launches with velocity 1.1 and inaccuracy 6.0, plays the bow/launch sound (1002) and shows smoke.

### init ("load"), in order

1. `proxy.registerRenderers()` (client only; index below).
2. A `TechgunsEventhandler` instance is registered on the **Forge event bus** ([events.md](events.md)).
3. `proxy.RegisterClientHandlers()` (client only):
   - `GuiOverlayAmmoCount` on the Forge bus ([input-and-hud.md](input-and-hud.md))
   - if config `enableDebugKeybinds` is true: the debug `KeyInputHandler` on the FML bus, plus `debug.Keybinds.init()`
   - `TGKeybinds` on the FML bus, plus `TGKeybinds.init()`
4. A `TechgunsTickHandler` instance is registered on the **FML bus** (`FMLCommonHandler.bus()`).
5. `TechgunsGuiHandler` is registered as the mod's GUI handler.

### postInit, in order

1. `TGFluids.postInit()`, `TGBlocks.postInit()`, `TGItems.postInit()`
2. `TGItems.registerRecipes()` (06), `TGMachineRecipes.addRecipes()` (08)
3. `TGuns.postInit()` (02), `TGEntities.postInit()` (10: NPC spawn registration)
4. **Steel nuggets made from another mod's steel:** if `addSteelNuggets` is on and `addSteelIngot` is off, and the oredict has at least one `ingotSteel`, two recipes are added: `ingotSteel` → 9 Techguns steel nuggets, and 3×3 `nuggetSteel` → the first `ingotSteel` registered in the oredict.
5. **TConstruct:** if loaded, `isTConstructLoaded` is set. On the client side (`event.getSide()==CLIENT`) `TechgunsTConstructIntegrationClient` is instantiated by reflection, otherwise `TechgunsTConstructIntegration`; then `init()` is called. Reflection errors are only printed. See [integrations.md](integrations.md).
6. **ThermalExpansion:** if loaded, `TechgunsThermalExpansionIntegration.init()` is called. It does nothing.
7. **FTBU:** if `FTBU` is loaded, `TGNpcFactions.friendsAPI` is set to the real FTBU friends adapter, otherwise to a dummy that says everyone is a friend.
8. **MineTweaker3:** if loaded, `TechgunsMineTweakerIntegration.init()` is called (08-machines-production).
9. If `SpawnStructures` is on, `WorldGenTGStructureSpawn` is registered with weight 4.
10. If `spawnOilHoles` is on and `TGFluids.OIL` is not lava (lava is the fallback when no oil fluid exists), `WorldGenOilHoles` is registered with weight 3.
11. `proxy.postInit()`. On the client, if `EnableVersionChecker` is on, a `TGVersionChecker` runs on a background thread named "Techguns VersionChecker".

## GUI handler ID table (high level)

`gui.TechgunsGuiHandler`. The server returns a container and the client returns a GUI. The tile entity is taken from the x/y/z passed in.

| ID | Server container | Client GUI | Owner area |
|---|---|---|---|
| 0 | AmmoPressContainer | GuiAmmoPress | 08 |
| 1 | MetalPressContainer | GuiMetalPress | 08 |
| 2 | TurretBaseContainer | GuiTurretBase | 09 |
| 3 | CamoBenchContainer | GuiCamoBench | 09 |
| 4 | ChemLabContainer | GuiChemLab | 08 |
| 5 | RepairBenchContainer | GuiRepairBench | 09 |
| 6 | ExplosiveChargeContainer | GuiExplosiveCharge | 09 |
| 7, 8 | (none) | (none) | (unused) |
| 9 | ChargingStationContainer | GuiChargingStation | 09 |
| 10 | ScrapperContainer | GuiScrapper | 08 |
| 11–15 | (none) | (none) | (unused) |
| 16 | OreDrillContainer | GuiOreDrill | 08 |
| 17 | TGPlayerContainer (player extra-slot inventory) | GuiTGPlayerInventory | player inventory GUI; tab/HUD side in [input-and-hud.md](input-and-hud.md) |
| 18 | ReactionChamberContainer | GuiReactionChamber | 08 |
| 19 | FabricatorContainer | GuiFabricator | 08 |
| 20 | TGChestContainer, built from the inventory the block at x/y/z returns; null if there is none | GuiTechgunsChest | 07 |

ID 17 is hard-coded here even though config `TechgunsGUI_TabID` (default 17) is what the inventory tab buttons send (bug CORE-07).

## Client renderer registration index (`ClientProxy.registerRenderers`)

This index only says what is bound to what. Models, textures and animations are described by the owning areas.

### Entity renderers

| Entity class | Renderer | Notes |
|---|---|---|
| GenericProjectile | RenderGenericBullet | 04 |
| SimpleProjectile | RenderSimpleProjectile, scale 1.0, `textures/entity/handgunBullet.png` | 04 |
| FlamethrowerProjectile, LaserProjectile, TeslaProjectile, CyberdemonBlasterProjectile, AlienBlasterProjectile, EnergyBlastProjectile, EntityParticleSystem, InvisibleProjectile, BeamProjectile | RenderDummy (draws nothing; visuals come from particles) | 04 |
| LaserBeam | RenderGenericLaser | 04 |
| TeslaBeam | RenderTeslaBeam (width 0.25) | 04 |
| RocketProjectile | RenderRocketProjectile + ModelRocket + `textures/guns/rocketlauncher.png` | 04 |
| NukeRocketProjectile | same, with `textures/guns/nukerocketlauncher.png` | 04 |
| BiogunProjectile | RenderBiogunProjectile, `textures/entity/bioblob.png` | 04 |
| RailgunProjectile | RenderRailgunProjectile | 04 |
| PowerHammerProjectile | RenderPowerHammerProjectile | 04 |
| GrenadeProjectile | RenderStielgranateProjectile + ModelStielgranate + `textures/guns/stielgranate.png` | 04 |
| FragGrenadeProjectile | RenderStielgranateProjectile + ModelFragGrenade(false) + `textures/guns/fragGrenade_texture.png` | 04 |
| BlasterRifleProjectile | RenderBlasterRifleProjectile | 04 |
| FlyingGibs | RenderFlyingGibs | 04 |
| LaserBeam2 | RenderLaserBeam2 | 04 |
| GrenadeLauncherProjectile | RenderGrenadeLauncherProjectile, OBJ `models/entity/launchergrenade.obj`, `textures/entity/launchergrenade.png` | 04 |
| SonicShotgunProjectile | RenderSonicShotgunProjectile | 04 |
| MovingBeam, NeutrinoBeam | RenderMovingBeam | 04 |
| NPC classes (GenericNPC, ZombieSoldier, ZombiePigmanSoldier, CyberDemon, ArmySoldier, NPCTurret, AttackHelicopter, ZombieFarmer, ZombieMiner, Bandit, Outcast, SkeletonSoldier, PsychoSteve, DictatorDave, AlienBug(+Domesticated), Commando, StormTrooper, SuperMutant Basic/Heavy/Elite) | RenderGenericNPC / RenderSimpleTurret / RenderHelicopter / RenderAlienBug / RenderSuperMutant with the matching models and textures | 10 |

### Item renderers

Each gun has its own `IItemRenderer` subclass built from a model and a texture under `textures/guns/` (03-gun-visuals-audio): handcannon, pistol, sawedoff, boltaction, flamethrower, m4, combatShotgun, lasergun, blasterRifle, rocketlauncher, biogun, thompson, revolver, ak47, as50, teslagun, pdw, lmg, m4_uq, grimreaper, goldenrevolver, pulseRifle, alienBlaster, cyberdemonBlaster, minigun, powerHammer, nukeLauncher, powerHammerAdv, aug, grenadeLauncher, sonicShotgun, chainsaw, scatterBeamGun, ndr, stielGranate, fragGrenade, akimbo_pistol, akimbo_revoler, akimbo_goldenrevolver, mac10, akimbo_mac10, mibGun, neutrinoGun, scar, vector and akimbo_mibGun. `TGItems.sharedItem` uses `RenderGenericItem` (06). The steam and T3 power armor pieces use `RenderArmorItem` (05). The `basicMachine` block item uses a `RenderMachineBlockItem` array holding one model renderer per metadata 0–10 (08/09). `tgchest` and `tgchest_weapon` use chest model item renderers (07).

### Tile entity special renderers (08/09/07)

AmmoPress, MetalPress, ChemLab (RenderAmmoPress with the matching model); TurretBase; CamoBench and RepairBench (tesselator cube renderers); CampFlag; ExplosiveCharge and ExplosiveChargeAdv (RenderExplosiveCharge with two textures each); OreDrillTileEntMaster; ReactionChamberTileEntMaster (OBJ `models/entity/reactionchamber.obj`); ChargingStation; FabricatorTileEntMaster; TGChestTileEnt.

### Block render handlers (07)

These ISBRHs are created and their render IDs assigned to blocks: RenderLadder → `ladder01`; RenderLamp → `lamp01`; RenderGenericPane → `camoNetPane`; RenderDirectionalBlock → `screen` and `controlpanel`; Chisel `RendererCTM` (unconditional); RenderOrecluster → `oreCluster`, `oreCluster2`, and `oreClusterCustom`/`oreClusterCustom2` when those config flags are on.

### Static armor model table

`ClientProxy.armorModels` is a static array of 12 biped models used by armor items (05):
- 0–1: SteamArmor(0/1)
- 2–3: T3PowerArmor(0/1)
- 4–6: ExoSuit(0, 1.0), (1, 0.5), (0, 0.75)
- 7: Beret
- 8–11: ArmorCoat(0, 1.0), (1, 0.51), (2, 0.49), (3, 0.75)

## Proxy API surface (CommonProxy, overridden by ClientProxy)

| Method | Server (CommonProxy) | Client (ClientProxy) |
|---|---|---|
| registerRenderers | nothing | renderer index above |
| spawnParticle(name, world, pos, motion) *(deprecated)* | nothing | spawns a hard-coded named particle ([client-effects-audio.md](client-effects-audio.md#legacy-named-particles)) |
| setGunTextures(gun, path, n) | nothing | adds `techguns:<path>.png`, then `<path>_1.png` … `<path>_(n-1).png` to the gun's texture list (camo variants, 03) |
| loadFXList | nothing | `FXList.loadFXList()` |
| createFX / createFXOnEntity / createFXOnEntity_ID | nothing / null | builds particle systems from FXList and spawns them in the world (the _ID form also returns their entity IDs) |
| initChisel | nothing | instantiates a Chisel CTM renderer (no callers) |
| getPlayerClient | null | `Minecraft.thePlayer` |
| RegisterClientHandlers | nothing | HUD and keybind registration |
| setHasStepassist / setHasNightvision / getters | nothing / false | stores the HUD indicator flags |
| setFlySpeed(v) | nothing | sets the local player's capability fly speed |
| handleSoundEvent / playSoundOnEntity / playSoundOnPosition | nothing | plays a `TGSound` ([client-effects-audio.md](client-effects-audio.md)) |
| handlePlayerGliding | nothing | starts or stops the glider loop sound |
| registerExtendedProperties | registers `TechgunsExtendedPlayerProperties` | registers the client subclass (which adds jetpack/glider sound handles) |
| isClientPlayerAndIn1stPerson | false | entity is the local player and the camera is first person |
| clientInRangeSquared(x, z, d²) | false | horizontal distance² from the local player to (x, z) ≤ d² |
| setFluidIcons | nothing | not overridden |
| postInit | nothing | starts the version checker |

ClientProxy also holds client-only state: `player_zoom` (FOV multiplier, default 1.0), `attackTimes` (recoil/reload timing per shooter), `entityDeathTypes` (pending death animations), `keyFirePressed`, `forwardKeyPressed` (unused), `lastReloadsoundPlayed`, `hasStepassist`, `hasNightvision`, `renderPass` (used by the multi-pass block renderers in 07), the `player_inv_texture` resource (`textures/gui/TGPlayerInventory.png`), and the version checker. `resolvePlayerNameFromUUID` returns the last known user name from Forge's `UsernameCache`, or `UNKNOW_PLAYERNAME` (sic).
