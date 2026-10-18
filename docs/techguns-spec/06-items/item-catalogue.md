# Item catalogue (06-items)

Every non-gun, non-armour item Techguns registers, plus the special-slot armour-like items that `TGItems` registers (listed for completeness; their wearable behaviour belongs to **05-armor**).

Notation:
- **Registry id** is the `GameRegistry.registerItem` name. FML prefixes it with the mod id `Techguns`, so the full id is `Techguns:<name>`.
- **Lang key** is what the client looks up. For shared items it is `techguns.item.<internalName>.name`.
- **Slot** is the `TGSlotType` stored for that metadata (see [special-items.md](special-items.md#slot-types)). `NORMAL` means a plain crafting item.
- All items appear in the creative tab `Techguns` (lang `techguns.itemGroup.Techguns=Techguns`) unless noted.
- No item in this area has a furnace burn time. Techguns registers no `IFuelHandler`.

## 1. The shared meta item `Techguns:TechgunsAmmo`

One `Item` instance (`GenericItemShared`, field `TGItems.sharedItem`) carries 110 sub-items, metas 0–109. The meta is fixed by registration order in `TGItems.init()`, and the internal name comes from the parallel `GenericItemShared.names[]` array. **Save compatibility depends on these meta numbers.**

Common properties:
- `hasSubtypes = true`. The creative tab lists every meta 0–109 in order.
- Default max stack is 64. A per-meta override comes from the table below.
- The icon is `techguns:<internalName>`, i.e. `textures/items/<internalName>.png`. Eight metas have no PNG because they are drawn by a 3D model renderer instead (see [special-items.md](special-items.md#3d-rendered-shared-items)).
- The unlocalized name is `techguns.item.<internalName>`.

| Meta | Internal name | `TGItems` field | English name | Stack | Slot | OreDict | Texture (`textures/items/…`) | Notes |
|---|---|---|---|---|---|---|---|---|
| 0 | stonebullets | stonebullets | Stone Bullet | 64 | AMMOSLOT | – | stonebullets.png | Hand Cannon ammo |
| 1 | bullets9mm | bullets9mm | 9mm Bullets | 64 | AMMOSLOT | – | bullets9mm.png | |
| 2 | pistolMag9mm | pistolMag9mm | 9mm Pistol Magazine | 64 | AMMOSLOT | – | pistolMag9mm.png | magazine tooltip; sneak-use empties it |
| 3 | pistolMag9mmEmpty | pistolMag9mmEmtpy *(sic)* | 9mm Pistol Magazine (Empty) | 64 | AMMOSLOT | – | pistolMag9mmEmpty.png | |
| 4 | shotgunShell | shotgunShell | Shotgun Shell | 64 | AMMOSLOT | – | shotgunShell.png | |
| 5 | smgMag | smgMag | SMG Magazine | 64 | AMMOSLOT | – | smgMag.png | magazine tooltip; sneak-use empties it |
| 6 | smgMagEmpty | smgMagEmpty | SMG Magazine (Empty) | 64 | AMMOSLOT | – | smgMagEmpty.png | |
| 7 | rifleBullets | rifleBullets | Rifle Bullets | 64 | AMMOSLOT | – | rifleBullets.png | |
| 8 | flamethrowerTank | flamethrowerTank | Fuel Tank | 64 | AMMOSLOT | – | flamethrowerTank.png | Forge fluid container (see fluids.md) |
| 9 | flamethrowerTankEmpty | flamethrowerTankEmpty | Fuel Tank (Empty) | 64 | AMMOSLOT | – | flamethrowerTankEmpty.png | |
| 10 | bioTank | bioTank | Bio Tank | 64 | AMMOSLOT | – | bioTank.png | |
| 11 | bioTankEmpty | bioTankEmpty | Bio Tank (Empty) | 64 | AMMOSLOT | – | bioTankEmpty.png | |
| 12 | m4Mag | m4Mag | Assault Rifle Magazine | 64 | AMMOSLOT | – | *(none, 3D model)* | magazine tooltip; sneak-use empties it |
| 13 | m4MagEmpty | m4MagEmpty | Assault Rifle Magazine (Empty) | 64 | AMMOSLOT | – | *(none, 3D model)* | |
| 14 | rocketAmmo | rocketAmmo | Rocket | 64 | AMMOSLOT | – | *(none, 3D model)* | dispensable as a projectile |
| 15 | energyCell | energyCell | Energy Cell | 64 | AMMOSLOT | – | energyCell.png | |
| 16 | energyCellEmpty | energyCellEmpty | Energy Cell (Empty) | 64 | AMMOSLOT | – | energyCellEmpty.png | |
| 17 | as50Mag | as50Mag | AS50 Magazine | 64 | AMMOSLOT | – | *(none, 3D model)* | magazine tooltip; sneak-use empties it |
| 18 | as50MagEmpty | as50MagEmpty | AS50 Magazine (Empty) | 64 | AMMOSLOT | – | *(none, 3D model)* | |
| 19 | highCalibreBullets | highCalibreBullets | High Caliber Rounds | 64 | AMMOSLOT | – | highCalibreBullets.png | |
| 20 | lmgMag | lmgMag | LMG Magazine | 64 | AMMOSLOT | – | *(none, 3D model)* | magazine tooltip; sneak-use empties it |
| 21 | lmgMagEmpty | lmgMagEmpty | ` LMG Magazine (Empty)` (leading space in lang) | 64 | AMMOSLOT | – | *(none, 3D model)* | |
| 22 | advBullets | advBullets | Advanced Bullets | 64 | AMMOSLOT | – | advBullets.png | |
| 23 | pdwMag | pdwMag | Advanced Magazine | 64 | AMMOSLOT | – | pdwMag.png | magazine tooltip; sneak-use empties it |
| 24 | pdwMagEmpty | pdwMagEmpty | Advanced Magazine (Empty) | 64 | AMMOSLOT | – | pdwMagEmpty.png | |
| 25 | stoneBarrel | stoneBarrel | Stone Barrel | 64 | NORMAL | – | stoneBarrel.png | gun part |
| 26 | woodStock | woodStock | Wooden Stock | 64 | NORMAL | – | woodStock.png | gun part |
| 27 | ironBarrel | ironBarrel | Iron Barrel | 64 | NORMAL | – | ironBarrel.png | gun part |
| 28 | compressedAirTank | compressedAirTank | Compressed Air Tank | 64 | AMMOSLOT | – | compressedAirTank.png | Power Hammer ammo; also used by Jump Pack and Oxygen Tanks |
| 29 | compressedAirTankEmpty | compressedAirTankEmpty | Compressed Air Tank (Empty) | 64 | AMMOSLOT | – | compressedAirTankEmpty.png | |
| 30 | mechanicalParts | mechanicalParts | Mechanical Parts | 64 | NORMAL | – | mechanicalParts.png | tier-1 component |
| 31 | plasticGunstock | plasticGunstock | Plastic Gun Stock | 64 | NORMAL | – | plasticGunstock.png | gun part |
| 32 | pumpMechanism | pumpMechanism | Pump Mechanism | 64 | NORMAL | – | pumpMechanism.png | gun part |
| 33 | obsidianSteelBarrel | obsidianSteelBarrel | Hardened Steel Barrel | 64 | NORMAL | – | obsidianSteelBarrel.png | gun part |
| 34 | mechanicalPartsT2 | mechanicalPartsT2 | Mechanical Parts (Obsidian Steel) | 64 | NORMAL | – | mechanicalPartsT2.png | tier-2 component |
| 35 | laserBarrel | laserBarrel | Laser Barrel | 64 | NORMAL | – | laserBarrel.png | gun part |
| 36 | laserFocus | laserFocus | Laser Focus | 64 | NORMAL | – | laserFocus.png | made in the Reaction Chamber |
| 37 | mechanicalPartsT3 | mechanicalPartsT3 | Mechanical Parts (Carbon) | 64 | NORMAL | – | mechanicalPartsT3.png | tier-3 component |
| 38 | bioMass | bioMass | Bio Mass | 64 | NORMAL | – | bioMass.png | |
| 39 | heavyCloth | heavyCloth | Heavy Cloth | 64 | NORMAL | – | heavyCloth.png | armour material |
| 40 | ingotObsidianSteel | ingotObsidianSteel | Obsidian Steel Ingot | 64 | NORMAL | `ingotObsidianSteel` | ingotObsidianSteel.png | |
| 41 | plateTin | plateTin | Tin Plate | 64 | NORMAL | `plateTin` | plateTin.png | |
| 42 | plateCopper | plateCopper | Copper Plate | 64 | NORMAL | `plateCopper` | plateCopper.png | |
| 43 | plateIron | plateIron | Iron Plate | 64 | NORMAL | `plateIron` | plateIron.png | |
| 44 | plateBronze | plateBronze | Bronze Plate | 64 | NORMAL | `plateBronze` | plateBronze.png | |
| 45 | plateSteel | plateSteel | Steel Plate | 64 | NORMAL | `plateSteel` | plateSteel.png | |
| 46 | minigunDrum | minigunMag | Minigun Ammo Drum | 64 | AMMOSLOT | – | minigunDrum.png | magazine tooltip; sneak-use empties it |
| 47 | minigunDrum_empty | minigunMagEmpty | Minigun Ammo Drum (empty) | 64 | AMMOSLOT | – | minigunDrum_empty.png | |
| 48 | turretArmorIron | turretArmorIron | Iron Turret Armor | **1** | TURRETARMOR | – | turretArmorIron.png | turret armour +5 |
| 49 | turretArmorSteel | turretArmorSteel | Steel Turret Armor | **1** | TURRETARMOR | – | turretArmorSteel.png | turret armour +10 |
| 50 | turretArmorObsidianSteel | turretArmorObsidianSteel | Obsidian Steel Turret Armor | **1** | TURRETARMOR | – | turretArmorObsidianSteel.png | turret armour +15 |
| 51 | glider | glider | Glider | **1** | BACKSLOT | – | glider.png | wearable, see special-items |
| 52 | machineStackUpgrade | machineStackUpgrade | Machine Upgrade (Stack Operation) | **7** | NORMAL | – | machineStackUpgrade.png | machine upgrade slot |
| 53 | ironReceiver | ironReceiver | Iron Receiver | 64 | NORMAL | – | ironReceiver.png | gun part |
| 54 | steelReceiver | steelReceiver | Steel Receiver | 64 | NORMAL | – | steelReceiver.png | gun part |
| 55 | obsidianSteelReceiver | obsidianSteelReceiver | Obsidian Steel Receiver | 64 | NORMAL | – | obsidianSteelReceiver.png | gun part |
| 56 | carbonReceiver | carbonReceiver | Carbon Receiver | 64 | NORMAL | – | carbonReceiver.png | gun part |
| 57 | wireCopper | copperWire | Copper Wire | 64 | NORMAL | `wireCopper` | wireCopper.png | |
| 58 | gliderWing | gliderWing | Glider Wing | 64 | NORMAL | – | gliderWing.png | |
| 59 | gliderBackback | gliderBackback | Glider Backback *(sic)* | 64 | NORMAL | – | gliderBackback.png | |
| 60 | tgx | tgx | TGX | 64 | NORMAL | – | tgx.png | explosive compound |
| 61 | carbonFibers | carbonFibers | Carbon Fibers | 64 | NORMAL | `fiberCarbon` | carbonFibers.png | |
| 62 | carbonPlate | carbonPlate | Carbon Plate | 64 | NORMAL | `plateCarbon` | carbonPlate.png | |
| 63 | nethercharge | nethercharge | Nether Charge | 64 | NORMAL | – | nethercharge.png | Nether Blaster ammo (slot NORMAL, not AMMOSLOT) |
| 64 | electricEngine | electricEngine | Electric Engine | 64 | NORMAL | – | electricEngine.png | |
| 65 | plateObsidianSteel | plateObsidianSteel | Obsidian Steel Plate | 64 | NORMAL | `plateObsidianSteel` | plateObsidianSteel.png | |
| 66 | turretArmorCarbon | turretArmorCarbon | Carbon Turret Armor | **1** | TURRETARMOR | – | turretArmorCarbon.png | turret armour +20 |
| 67 | steamArmorPlate | steamArmorPlate | Steam Armor Plating | 64 | NORMAL | – | steamArmorPlate.png | armour material |
| 68 | circuitBoard | circuitBoard | Circuit Board | 64 | NORMAL | `circuitBasic` | circuitBoard.png | |
| 69 | coil | coil | Coil | 64 | NORMAL | – | coil.png | |
| 70 | rawRubber | rawRubber | Raw Rubber | 64 | NORMAL | `itemRawRubber` | rawRubber.png | |
| 71 | rawPlastic | rawPlastic | Raw Plastic | 64 | NORMAL | `rawPlastic` | rawPlastic.png | |
| 72 | plasticSheet | plasticSheet | Plastic Sheet | 64 | NORMAL | `sheetPlastic` | plasticSheet.png | |
| 73 | rubberBar | rubberBar | Rubber Bar | 64 | NORMAL | `itemRubber` | rubberBar.png | |
| 74 | gasMaskFilter | gasMaskFilter | Gas Mask Filter | 64 | NORMAL | – | gasMaskFilter.png | |
| 75 | oreDrillSmallSteel | oreDrillSmallSteel | Small Ore Drill Head (Steel) | **1** | DRILL_SMALL | – | oreDrillSmallSteel.png | drill tooltip, size 1x1x1, level 1 |
| 76 | oreDrillSmallObsidianSteel | oreDrillSmallObsidianSteel | Small Ore Drill Head (Obsidian Steel) | **1** | DRILL_SMALL | – | oreDrillSmallObsidianSteel.png | level 2 |
| 77 | oreDrillSmallCarbon | oreDrillSmallCarbon | Small Ore Drill Head (Carbon) | **1** | DRILL_SMALL | – | oreDrillSmallCarbon.png | level 3 |
| 78 | oreDrillMediumSteel | oreDrillMediumSteel | Medium Ore Drill Head (Steel) | **1** | DRILL_MEDIUM | – | oreDrillMediumSteel.png | size "1x1xN or 3x3xN", level 1 |
| 79 | oreDrillMediumObsidianSteel | oreDrillMediumObsidianSteel | Medium Ore Drill Head (Obsidian Steel) | **1** | DRILL_MEDIUM | – | oreDrillMediumObsidianSteel.png | level 2 |
| 80 | oreDrillMediumCarbon | oreDrillMediumCarbon | Medium Ore Drill Head (Carbon) | **1** | DRILL_MEDIUM | – | oreDrillMediumCarbon.png | level 3 |
| 81 | oreDrillLargeSteel | oreDrillLargeSteel | Large Ore Drill Head (Steel) | **1** | DRILL_LARGE | – | oreDrillLargeSteel.png | size "5x5xN or 7x7xN", level 1 |
| 82 | oreDrillLargeObsidianSteel | oreDrillLargeObsidianSteel | Large Ore Drill Head (Obsidian Steel) | **1** | DRILL_LARGE | – | oreDrillLargeObsidianSteel.png | level 2 |
| 83 | oreDrillLargeCarbon | oreDrillLargeCarbon | Large Ore Drill Head (Carbon) | **1** | DRILL_LARGE | – | oreDrillLargeCarbon.png | level 3 |
| 84 | carbonGunstock | carbonGunstock | Carbon Gunstock | 64 | NORMAL | – | carbonGunstock.png | gun part |
| 85 | carbonBarrel | carbonBarrel | Carbon Barrel | 64 | NORMAL | – | carbonBarrel.png | gun part |
| 86 | goldWire | goldWire | Gold Wire | 64 | NORMAL | `wireGold` | goldWire.png | |
| 87 | circuitBoardElite | circuitBoardElite | Elite Circuit Board | 64 | NORMAL | `circuitElite` | circuitBoardElite.png | |
| 88 | rifleBulletsStack | rifleBulletsStack | Rifle Bullets Stack | 64 | AMMOSLOT | – | rifleBulletsStack.png | holds 4 Rifle Bullets |
| 89 | powerPlating | powerPlating | Power Plating | 64 | NORMAL | – | powerPlating.png | armour material |
| 90 | oxygenMask | oxygenMask | Oxygen Mask | **1** | FACESLOT | – | oxygenMask.png | gives OXYGEN_GEAR bonus 1.0 |
| 91 | cyberneticParts | cyberneticParts | Cybernetic Parts | 64 | NORMAL | – | cyberneticParts.png | |
| 92 | uvemitter | focus_uvemitter | UV Emitter | **1** | REACTION_CHAMBER_FOCUS | – | uvemitter.png | Reaction Chamber beam focus |
| 93 | 40mmGrenade | _40mmGrenade | 40mm Grenade | 64 | AMMOSLOT | – | 40mmGrenade.png | dispensable as a projectile |
| 94 | oreTitanium | oreTitanium | Titanium Ore | 64 | NORMAL | `oreTitanium` | oreTitanium.png | an item, not a block |
| 95 | ingotTitanium | ingotTitanium | Titanium Ingot | 64 | NORMAL | `ingotTitanium` | ingotTitanium.png | |
| 96 | plateTitanium | plateTitanium | Titanium Plate | 64 | NORMAL | `plateTitanium` | plateTitanium.png | |
| 97 | sonicEmitter | sonicEmitter | Sonic Emitter | 64 | NORMAL | – | sonicEmitter.png | |
| 98 | rcHeatRay | rcHeatRay | Reaction Chamber Heat Ray Focus | **1** | REACTION_CHAMBER_FOCUS | – | rcHeatRay.png | Reaction Chamber beam focus |
| 99 | quartz_rod | quartzRod | Quartz Rod | 64 | NORMAL | – | quartz_rod.png | |
| 100 | antiGravityCore | antiGravityCore | Anti Gravity Core | 64 | NORMAL | – | antiGravityCore.png | |
| 101 | yellowcake | yellowcake | Yellowcake | 64 | NORMAL | `dustUranium` | yellowcake.png | |
| 102 | enrichedUranium | enrichedUranium | Enriched Uranium | 64 | NORMAL | `ingotUraniumEnriched` | enrichedUranium.png | |
| 103 | tacticalNuclearWarhead | nuclearWarheadSmall | Small Nuclear Warhead | 64 | NORMAL | – | tacticalNuclearWarhead.png | |
| 104 | plateLead | plateLead | Lead Plate | 64 | NORMAL | `plateLead` | plateLead.png | |
| 105 | rocketAmmoNuclear | rocketAmmoNuclear | Rocket (Tactical Nuke) | 64 | AMMOSLOT | – | *(none, 3D model)* | dispensable as a projectile |
| 106 | nuclearPowerCell | nuclearPowerCell | Nuclear Power Cell | 64 | AMMOSLOT | – | nuclearPowerCell.png | |
| 107 | nuclearPowerCellDepleted | nuclearPowerCellDepleted | Nuclear Power Cell (Depleted) | 64 | AMMOSLOT | – | nuclearPowerCellDepleted.png | |
| 108 | radEmitter | radEmitter | Rad Emitter | 64 | NORMAL | – | radEmitter.png | |
| 109 | treatedLeather | treatedLeather | Treated Leather | 64 | NORMAL | – | treatedLeather.png | armour material |

Magazine tooltips (two extra lines, see [lang.md](lang.md#generated-tooltips)) are attached to metas **2, 5, 12, 17, 20, 23, 46**. Drill-head tooltips go on metas 75–83.

All the OreDict registrations in the table happen in `TGItems.init()`.

## 2. Stand-alone items registered by `TGItems`

| Registry id | Field | Class | English name (lang key) | Stack | Max damage | Texture | Area owning behaviour |
|---|---|---|---|---|---|---|---|
| `doorItem01` | `door01` | `ItemTGDoor` | Bunker Door (`techguns.item.bunkerDoor.name`) | 64 | – | `textures/items/metalDoor.png` | 06 (item); door block in 07 |
| `gasMask` | `gasMask` | `ItemGasMask` | Gas Mask | 1 | 300 | `textures/items/gasMask.png` | 05 |
| `nightVisionGoggles` | `nightVisionGoggles` | `ItemNightVisionGoggles` | Nightvision Goggles | 1 | 24000 | `textures/items/nightVisionGoggles.png` | 05 |
| `jetpack` | `jetpack` | `ItemJetpack` | Jetpack | 1 | 5000 | `textures/items/jetpack.png` | 05 |
| `scubaTanks` | `scubaTanks` | `ItemScubaTanks` | Oxygen Tanks | 1 | 600 | `textures/items/scubaTanks.png` | 05 |
| `tacticalMask` | `tacticalMask` | `ItemTacticalMask` | Tactical Mask | 1 | 24000 | `textures/items/tacticalMask.png` | 05 |
| `oreClusterScanner` | `oreClusterScanner` | `OreClusterScanner` | Ore Cluster Scanner | 1 | (RF bar) | `textures/items/oreClusterScanner.png` | 06 |
| `combatKnife` | `combatKnife` | `TGSword` | Combat Knife | 1 | 1000 (material) | `textures/items/combatKnife.png` | 06 |
| `jumppack` | `jumppack` | `ItemJumppack` | Jump Pack | 1 | 1000 | `textures/items/jumppack.png` | 05 |
| `antiGravPack` | `antiGravPack` | `ItemAntiGravPack` | Anti Gravity Device | 1 | 12000 | `textures/items/antiGravPack.png` | 05 |
| `camoNetPlacer` | `camoNetPlacer` | `ItemCamoNetPlacer` | Camo Net Placer (all metas) | 1 | – (meta 0–2 = camo) | `textures/items/camoNetPlacer0.png` … `camoNetPlacer2.png` | 06 |
| `crowbar` | `crowbar` | `TGCrowbar` | Crowbar | 1 | 1000 (material) | `textures/items/crowbar.png` | 06 |
| `bucket_acid` | `bucketAcid` | `GenericItemBucket` | Creeper Acid Bucket | 1 | – | `textures/items/bucket_acid.png` | 06 (registered in `postInit`) |

For the special-slot items the "max damage" column is the last constructor argument in `TGItems`. Its meaning (charges, ticks, air) is in 05-armor.

The armour-item constructor's second argument (`"nightVisionGoggles"`, `"jetPackIcon"`, `"oxygenFlasksIcon"`, `"tacticalMaskItem"`, `"jumpPackIcon"`) is used by 05. The inventory icon always comes from the unlocalized name, so it is `textures/items/<registry id>.png`.

Tool material `TG_STEEL` is created with `EnumHelper.addToolMaterial("Steel", harvestLevel 2, maxUses 1000, efficiency 7.5, damage 2.5, enchantability 12)`. Both tools use it. See [special-items.md](special-items.md#tools).

### Registered but commented out
- `medkit` (`ItemMedkit("medkit", 2)`): the registration lines in `TGItems.init()` are commented out, and so is its recipe (and a Chem Lab recipe in `TGMachineRecipes`). The class, the textures `textures/items/medkit_0.png` and `medkit_1.png`, and the lang keys (`techguns.item.medkit_0.name=Bandages`, `techguns.item.medkit_1.name=Medkit`) all exist. Its behaviour is in [special-items.md](special-items.md#medkit-unregistered).
- `gasMaskHeavy`: commented out.

## 3. Items registered in `Techguns.java` (preInit), not in `TGItems`

These are plain `GenericItem`s (stack 64, unlocalized `techguns.item.<name>`). **Registration is conditional on config flags** (config file handled by 01-core; all defaults are `false`).

| Registry id | English name | Texture | Registered when | OreDict (same condition) |
|---|---|---|---|---|
| `ingotSteel` | Steel Ingot | `textures/items/ingotSteel.png` | `addSteelIngot` | `ingotSteel` |
| `ingotCopper` | Copper Ingot | `textures/items/ingotCopper.png` | not `dontAddCopperTinBronze()` † | `ingotCopper` |
| `ingotTin` | Tin Ingot | `textures/items/ingotTin.png` | † | `ingotTin` |
| `ingotBronze` | Bronze Ingot | `textures/items/ingotBronze.png` | † | `ingotBronze` |
| `ingotLead` | Lead Ingot | `textures/items/ingotLead.png` | `addLeadIngot` | `ingotLead` |
| `nuggetIron` | Iron Nugget | `textures/items/nuggetIron.png` | `addIronNuggets` (registered **twice**, see bug ITEM-01) | `nuggetIron` |
| `nuggetCopper` | Copper Nugget | `textures/items/copperNugget.png` | `addCopperTinBronzeNuggets` | `nuggetCopper` |
| `nuggetLead` | Lead Nugget | `textures/items/nuggetLead.png` | `addLeadNuggets` | `nuggetLead` |
| `nuggetSteel` | Steel Nugget | `textures/items/nuggetSteel.png` | `addSteelNuggets` | `nuggetSteel` |

† `dontAddCopperTinBronze()` is true when `addBronzeCrafting`, `addCopperTinBronzeNuggets` and `doOreGenCopperTin` are **all** false. So the three ingots are registered if any one of those flags is on.

`Techguns.java` also registers `OreDictionary "itemGunpowder"` → vanilla gunpowder. The Ammo Press uses it.

Related recipes are in [recipes.md](recipes.md#7-config-dependent-metal-recipes-techgunsjava).

Note that `ingotTitanium` and `ingotObsidianSteel` are always present (shared metas 95 and 40), while steel, lead and copper must come from other mods unless these flags are on. Many core recipes need `ingotSteel`, `ingotLead`, `nuggetCopper`, `nuggetLead`, `nuggetSteel` or `nuggetIron` from the OreDict.

## 4. Debug / developer items (registered unconditionally in `Techguns.java`)

| Registry id | Class | Unlocalized | English name | Texture | Stack |
|---|---|---|---|---|---|
| `structureTestTool` | `StructureTestTool` | `techguns.item.structureTestTool` | Structure Test Tool | `textures/items/structureTestTool.png` | 1 |
| `blockReaderTool` | `BlockReader` | `techguns.item.blockReaderTool` | Block Reader Tool | `textures/items/blockReaderTool.png` | 1 |
| `buildingScanTool` | `BuildingScanTool` | `techguns.item.buildingScanTool` | Building Scan Tool | `textures/items/structureScanTool.png` | 1 |
| `oreClusterDebugger` | `oreClusterDebugger` | `techguns.item.oreClusterDebugger` | Ore Cluster Debugger | `textures/items/testItem.png` | 1 |

All four go in the normal Techguns creative tab. Behaviour is in [special-items.md](special-items.md#debug-and-scanner-tools).

## 5. Spawn eggs (`TGMonsterPlacer`, registered by `TGEntities`)

There is one item per NPC: registry id `spawnEgg<EntityName>`, unlocalized `spawn_egg_<entityname lower-case>`, texture `techguns:spawn_egg` with overlay `techguns:spawn_egg_overlay`. The display name is hard-coded `"Spawn " + <EntityName>` (not localized). Colours (base / spots):

| Entity name | Base | Spots |
|---|---|---|
| ZombieSoldier | 0x757468 | 0x38B038 |
| ZombieFarmer | 0x757468 | 0x38B038 |
| ZombieMiner | 0x757468 | 0x38B038 |
| ZombiePigmanSoldier | 0xFF1111 | 0x770000 |
| AttackHelicopter | 0x373d23 | 0x8ec0d7 |
| AlienBug | 0xc6a96b | 0x71552e |
| CyberDemon | 0xFF1111 | 0x777777 |
| ArmySoldier | 0x74806e | 0x191512 |
| Bandit | 0x8f9d59 | 0x2c3117 |
| Outcast | 0x101010 | 0xFF0000 |
| SkeletonSoldier | 0x404040 | 0xF0F0F0 |
| PsychoSteve | 0x757468 | 0xF0F0F0 |
| DictatorDave | 0x8f9d59 | 0xA0A000 |
| Commando | 0x191512 | 0x74806e |
| Stormtrooper | 0xF0F0F0 | 0x404040 |
| SuperMutantBasic | 0xc6a96b | 0x71552e |
| SuperMutantHeavy | 0xc6a96b | 0x71552e |
| SuperMutantElite | 0xc6a96b | 0x71552e |

The NPCs themselves are in 10-entities-npcs. The item behaviour is in [special-items.md](special-items.md#spawn-eggs).

## 6. Where the items are consumed (cross-reference)

| Consumer | Items |
|---|---|
| Gun recipes and gun ammo (02-guns) | gun parts (barrels, stocks, receivers, pump), mech parts T1–T3, laser parts, all ammo/mags/tanks/cells, nethercharge, coil, cybernetic parts, sonic emitter, rad emitter, carbon parts |
| Armour recipes (05-armor) | heavyCloth, treatedLeather, steamArmorPlate, powerPlating, carbonFibers, carbonPlate, ingotObsidianSteel, circuitBoardElite, copperWire, goldWire, electricEngine, energyCell(Empty), compressedAirTank(Empty), mechanicalPartsT3 |
| Machines (08/09) | machineStackUpgrade (Ammo Press, Metal Press, Chem Lab, Fabricator, Charging Station upgrade slot); turretArmor* (turret base armour slot); oreDrill* heads (Ore Drill); uvemitter / rcHeatRay (Reaction Chamber focus) |
| NPC drops (10) | bullets9mm, rifleBullets, shotgunShell, heavyCloth, circuitBoard, cyberneticParts, energyCell, ingotObsidianSteel, plasticSheet, plateSteel, rubberBar, flamethrowerTank, rocketAmmo |
| Crowbar | ore cluster blocks (07/11) |
