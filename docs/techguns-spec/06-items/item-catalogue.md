# Item catalogue (06-items)

Every non-gun, non-armour item Techguns registers, plus the special-slot armour-like items that `TGItems` registers (listed for completeness; their wearable behaviour belongs to **[05-armor](../05-armor/index.md)**).

Notation:
- **Registry id** is the `GameRegistry.registerItem` name. FML prefixes it with the mod id [`Techguns`](../01-core/lifecycle-and-registration.md#lifecycle-overview), so the full id is `Techguns:<name>`.
- **Lang key** is what the client looks up. For shared items it is `techguns.item.<internalName>.name`.
- **Slot** is the [`TGSlotType`](../05-armor/extra-inventory.md#2-tgslottype-enum) stored for that metadata (see [special-items.md](special-items.md#slot-types-tgslottype)). `NORMAL` means a plain crafting item.
- All items appear in the creative tab `Techguns` (lang `techguns.itemGroup.Techguns=Techguns`) unless noted.
- No item in this area has a furnace burn time. Techguns registers no `IFuelHandler`.

## 1. The shared meta item `Techguns:TechgunsAmmo`

One `Item` instance ([`GenericItemShared`](special-items.md#shared-item-genericitemshared-behaviour), field `TGItems.sharedItem`) carries 110 sub-items, metas 0–109. The meta is fixed by registration order in `TGItems.init()`, and the internal name comes from the parallel `GenericItemShared.names[]` array. **Save compatibility depends on these meta numbers.**

Common properties:
- `hasSubtypes = true`. The creative tab lists every meta 0–109 in order.
- Default max stack is 64. A per-meta override comes from the table below.
- The icon is `techguns:<internalName>`, i.e. `textures/items/<internalName>.png`. Eight metas have no PNG because they are drawn by a 3D model renderer instead (see [special-items.md](special-items.md#3d-rendered-shared-items-rendergenericitem)).
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

Magazine tooltips (two extra lines, see [lang.md](lang.md#tooltip-lines)) are attached to metas **2, 5, 12, 17, 20, 23, 46**. Drill-head tooltips go on metas 75–83.

All the OreDict registrations in the table happen in `TGItems.init()`.


## 1b. Lang key and "Obtained from" for every shared meta (consistency pass)

The lang key is `techguns.item.<internalName>.name`; all 110 keys exist in `en_US.lang` and the English names are in the table above. "Obtained from" gives the shortest route: `R..` and `B..` numbers are rows in [recipes.md](recipes.md); "Metal Press / Chem Lab / Fabricator / Reaction Chamber / Charging Station recipe N" are the numbered rows in [08-machines-production](../08-machines-production/index.md); loot is in [loot.md](loot.md); "NPC drop" refers to the drop list in [10-entities-npcs](../10-entities-npcs/index.md). "Returned on reload" means the empty version comes back to the player when a gun reloads (see [ammo.md](ammo.md)). Only meta 92 (UV Emitter) is unobtainable in survival.

| Meta | Lang key | Obtained from |
|---|---|---|
| 0 | `techguns.item.stonebullets.name` | Crafting R32 (3 cobblestone + gunpowder, x16); weapon-chest T1 and bonus-chest loot |
| 1 | `techguns.item.bullets9mm.name` | Crafting R33 (x8); Ammo Press plan 0 (x12, 08); dungeon and weapon-chest loot; NPC drop |
| 2 | `techguns.item.pistolMag9mm.name` | Crafting R36 (empty mag + 3 9mm bullets); dungeon and weapon-chest loot; NPC drop |
| 3 | `techguns.item.pistolMag9mmEmpty.name` | Crafting R35 (x4); returned when a 9mm gun reloads or a full mag is emptied |
| 4 | `techguns.item.shotgunShell.name` | Crafting R34 (x5); Ammo Press plan 1 (x16, 08); dungeon and weapon-chest loot; NPC drop |
| 5 | `techguns.item.smgMag.name` | Crafting R38 (empty mag + 2 9mm bullets); dungeon and weapon-chest loot |
| 6 | `techguns.item.smgMagEmpty.name` | Crafting R37 (x4); returned on reload or emptying |
| 7 | `techguns.item.rifleBullets.name` | Ammo Press plan 2 (x8, 08); loot; NPC drop; Rifle Bullets Stack split R52 (x4) |
| 8 | `techguns.item.flamethrowerTank.name` | Chem Lab fill (Fuel Tank (Empty) + 250 mB fuel, or 500 mB lava if no fuel fluid; [08](../08-machines-production/index.md)); weapon-chest T3 loot; NPC drop; Forge fluid container |
| 9 | `techguns.item.flamethrowerTankEmpty.name` | Crafting R50 (x4); returned when a flamethrower or chainsaw reloads; Chem Lab drain ([08](../08-machines-production/index.md)) |
| 10 | `techguns.item.bioTank.name` | Chem Lab (Bio Mass + Empty Bio Tank + 500 mB water; [08](../08-machines-production/index.md)) |
| 11 | `techguns.item.bioTankEmpty.name` | Crafting R41; returned when the bio gun reloads |
| 12 | `techguns.item.m4Mag.name` | Crafting R40 (empty mag + 3 rifle bullets); dungeon and weapon-chest loot |
| 13 | `techguns.item.m4MagEmpty.name` | Crafting R39 (x4); returned on reload or emptying |
| 14 | `techguns.item.rocketAmmo.name` | Crafting R42 (x4); weapon-chest T3 loot; NPC drop; dispenser projectile |
| 15 | `techguns.item.energyCell.name` | Charging Station (Empty Energy Cell, 08); NPC drop |
| 16 | `techguns.item.energyCellEmpty.name` | Fabricator recipe 1 (gold ingot + copper wire + 3 redstone + plastic sheet; [08](../08-machines-production/index.md)); returned when an energy-cell gun reloads |
| 17 | `techguns.item.as50Mag.name` | Crafting R47 (empty mag + 2 High Caliber Rounds) |
| 18 | `techguns.item.as50MagEmpty.name` | Crafting R46 (x4); returned on reload or emptying |
| 19 | `techguns.item.highCalibreBullets.name` | Ammo Press plan 3 (x4, 08); dungeon-chest loot |
| 20 | `techguns.item.lmgMag.name` | Crafting R44 or R45 (empty mag + bullets or stacks) |
| 21 | `techguns.item.lmgMagEmpty.name` | Crafting R43 (x4); returned on reload or emptying |
| 22 | `techguns.item.advBullets.name` | Metal Press recipe 13 (obsidian steel plate + TGX, x16; [08](../08-machines-production/index.md)) |
| 23 | `techguns.item.pdwMag.name` | Crafting R49 (empty mag + 3 advanced bullets) |
| 24 | `techguns.item.pdwMagEmpty.name` | Crafting R48 (x4); returned on reload or emptying |
| 25 | `techguns.item.stoneBarrel.name` | Crafting R01 |
| 26 | `techguns.item.woodStock.name` | Crafting R02 |
| 27 | `techguns.item.ironBarrel.name` | Crafting R03 |
| 28 | `techguns.item.compressedAirTank.name` | Chem Lab (Coal + Empty Compressed Air Tank + 250 mB water; [08](../08-machines-production/index.md)); barracks loot |
| 29 | `techguns.item.compressedAirTankEmpty.name` | Crafting R55 or R56 (x7); returned when a power hammer reloads |
| 30 | `techguns.item.mechanicalParts.name` | Crafting R04; Metal Press recipe 8 (iron plate + flint) |
| 31 | `techguns.item.plasticGunstock.name` | Crafting R05 |
| 32 | `techguns.item.pumpMechanism.name` | Crafting R07 |
| 33 | `techguns.item.obsidianSteelBarrel.name` | Crafting R06 |
| 34 | `techguns.item.mechanicalPartsT2.name` | Metal Press recipe 9 (obsidian steel plate + quartz) |
| 35 | `techguns.item.laserBarrel.name` | Crafting R08 |
| 36 | `techguns.item.laserFocus.name` | Reaction Chamber recipe 2 (diamond, heat ray, liquid redstone; [08](../08-machines-production/index.md)) |
| 37 | `techguns.item.mechanicalPartsT3.name` | Metal Press recipe 10 (carbon plate + blaze rod, x2) |
| 38 | `techguns.item.bioMass.name` | Crafting R26 (x4) |
| 39 | `techguns.item.heavyCloth.name` | Crafting R09 (x3); NPC drop |
| 40 | `techguns.item.ingotObsidianSteel.name` | Metal Press recipe 1 (steel ingot + obsidian); smelting Obsidian Steel Plate (06 recipes section 6); NPC drop |
| 41 | `techguns.item.plateTin.name` | Metal Press recipe 2 (2 tin ingots, x2) |
| 42 | `techguns.item.plateCopper.name` | Metal Press recipe 3 (2 copper ingots, x2) |
| 43 | `techguns.item.plateIron.name` | Metal Press recipe 5 (2 iron ingots, x2) |
| 44 | `techguns.item.plateBronze.name` | Metal Press recipe 4 (2 bronze ingots, x2) |
| 45 | `techguns.item.plateSteel.name` | Metal Press recipe 6 (2 steel ingots, x2); NPC drop |
| 46 | `techguns.item.minigunDrum.name` | Crafting R54 (empty drum + 4 rifle bullet stacks) |
| 47 | `techguns.item.minigunDrum_empty.name` | Crafting R53 (x4) |
| 48 | `techguns.item.turretArmorIron.name` | Crafting R58 |
| 49 | `techguns.item.turretArmorSteel.name` | Crafting R59 |
| 50 | `techguns.item.turretArmorObsidianSteel.name` | Crafting R60 |
| 51 | `techguns.item.glider.name` | Crafting R17 (backpack + 2 wings); barracks loot |
| 52 | `techguns.item.machineStackUpgrade.name` | Crafting R22 |
| 53 | `techguns.item.ironReceiver.name` | Crafting R10 |
| 54 | `techguns.item.steelReceiver.name` | Crafting R11 |
| 55 | `techguns.item.obsidianSteelReceiver.name` | Crafting R12 |
| 56 | `techguns.item.carbonReceiver.name` | Crafting R13 |
| 57 | `techguns.item.wireCopper.name` | Crafting R14; Metal Press recipe 11 (2 copper plates, x8) |
| 58 | `techguns.item.gliderWing.name` | Crafting R16 |
| 59 | `techguns.item.gliderBackback.name` | Crafting R15 |
| 60 | `techguns.item.tgx.name` | Chem Lab recipe 6 (gunpowder + lapis + fuel or lava; [08](../08-machines-production/index.md)) |
| 61 | `techguns.item.carbonFibers.name` | Chem Lab recipe 7 (diamond + blaze powder + 1000 mB lava, x2; [08](../08-machines-production/index.md)) |
| 62 | `techguns.item.carbonPlate.name` | Metal Press recipe 12 (2 carbon fibers, x2) |
| 63 | `techguns.item.nethercharge.name` | Chem Lab recipe 11 (netherrack + soul sand + 1000 mB lava, x4; [08](../08-machines-production/index.md)) |
| 64 | `techguns.item.electricEngine.name` | Crafting R18 |
| 65 | `techguns.item.plateObsidianSteel.name` | Metal Press recipe 14 (2 obsidian steel ingots, x2) |
| 66 | `techguns.item.turretArmorCarbon.name` | Crafting R61 |
| 67 | `techguns.item.steamArmorPlate.name` | Metal Press recipe 15 (steel plate + bronze plate) |
| 68 | `techguns.item.circuitBoard.name` | Crafting R19 (x4); NPC drop |
| 69 | `techguns.item.coil.name` | Crafting R21 |
| 70 | `techguns.item.rawRubber.name` | Chem Lab recipe 8 (log + 1000 mB water; [08](../08-machines-production/index.md)) |
| 71 | `techguns.item.rawPlastic.name` | Chem Lab recipe 3b or 3c (raw rubber + oil, or raw rubber + coal + water; [08](../08-machines-production/index.md)) |
| 72 | `techguns.item.plasticSheet.name` | Smelting Raw Plastic (06 recipes section 6); NPC drop; reinforced-chest T3 loot |
| 73 | `techguns.item.rubberBar.name` | Smelting Raw Rubber (06 recipes section 6); NPC drop; reinforced-chest T2/T3 loot |
| 74 | `techguns.item.gasMaskFilter.name` | Crafting R25; barracks loot |
| 75 | `techguns.item.oreDrillSmallSteel.name` | Crafting R62 |
| 76 | `techguns.item.oreDrillSmallObsidianSteel.name` | Crafting R63 |
| 77 | `techguns.item.oreDrillSmallCarbon.name` | Crafting R64 |
| 78 | `techguns.item.oreDrillMediumSteel.name` | Crafting R65 (2 small heads) |
| 79 | `techguns.item.oreDrillMediumObsidianSteel.name` | Crafting R66 |
| 80 | `techguns.item.oreDrillMediumCarbon.name` | Crafting R67 |
| 81 | `techguns.item.oreDrillLargeSteel.name` | Crafting R68 (medium heads) |
| 82 | `techguns.item.oreDrillLargeObsidianSteel.name` | Crafting R69 |
| 83 | `techguns.item.oreDrillLargeCarbon.name` | Crafting R70 |
| 84 | `techguns.item.carbonGunstock.name` | Crafting R24 |
| 85 | `techguns.item.carbonBarrel.name` | Crafting R23 |
| 86 | `techguns.item.goldWire.name` | Metal Press recipe 16 (2 gold ingots, x2) |
| 87 | `techguns.item.circuitBoardElite.name` | Crafting R20 (circuit board + gold wire + lapis) |
| 88 | `techguns.item.rifleBulletsStack.name` | Crafting R51 (4 rifle bullets) |
| 89 | `techguns.item.powerPlating.name` | Fabricator recipe 3 ([08](../08-machines-production/index.md)); no crafting recipe (commented out) |
| 90 | `techguns.item.oxygenMask.name` | Crafting R31; barracks loot |
| 91 | `techguns.item.cyberneticParts.name` | Fabricator recipe 2 (soul sand + gold wire + redstone + plastic; [08](../08-machines-production/index.md)); NPC drop |
| 92 | `techguns.item.uvemitter.name` | Unobtainable in survival (no recipe, no loot; [ITEM-18](index.md#suspected-bugs)); creative tab only |
| 93 | `techguns.item.40mmGrenade.name` | Metal Press recipe 17 (iron plate + TNT, x16; [08](../08-machines-production/index.md)) |
| 94 | `techguns.item.oreTitanium.name` | Reaction Chamber recipe 3 (titanium iron ore; [08](../08-machines-production/index.md)) |
| 95 | `techguns.item.ingotTitanium.name` | Smelting Titanium Ore or Titanium Plate (06 recipes section 6) |
| 96 | `techguns.item.plateTitanium.name` | Metal Press recipe 18 (2 titanium ingots, x2) |
| 97 | `techguns.item.sonicEmitter.name` | Fabricator recipe 4 ([08](../08-machines-production/index.md)) |
| 98 | `techguns.item.rcHeatRay.name` | Crafting R30 |
| 99 | `techguns.item.quartz_rod.name` | Crafting R27 or R28 (3 quartz diagonal) |
| 100 | `techguns.item.antiGravityCore.name` | Reaction Chamber recipe 6 (nether star; [08](../08-machines-production/index.md)) |
| 101 | `techguns.item.yellowcake.name` | Chem Lab recipe 12 (uranium ore + 250 mB creeper acid; [08](../08-machines-production/index.md)) |
| 102 | `techguns.item.enrichedUranium.name` | Reaction Chamber recipe 7 (yellowcake; [08](../08-machines-production/index.md)) |
| 103 | `techguns.item.tacticalNuclearWarhead.name` | Crafting R29 (x2) |
| 104 | `techguns.item.plateLead.name` | Metal Press recipe 7 (2 lead ingots, x2) |
| 105 | `techguns.item.rocketAmmoNuclear.name` | Crafting R57 (rocket + small nuclear warhead) |
| 106 | `techguns.item.nuclearPowerCell.name` | Chem Lab recipe 13 (depleted cell + enriched uranium + water; [08](../08-machines-production/index.md)) |
| 107 | `techguns.item.nuclearPowerCellDepleted.name` | Fabricator recipe 6 ([08](../08-machines-production/index.md)); returned when the nuclear death ray reloads |
| 108 | `techguns.item.radEmitter.name` | Fabricator recipe 5 ([08](../08-machines-production/index.md)) |
| 109 | `techguns.item.treatedLeather.name` | Chem Lab recipe 15 (2 leather + slimeball + creeper acid, x2; [08](../08-machines-production/index.md)) |

## 2. Stand-alone items registered by `TGItems`

| Registry id | Field | Class | English name (lang key) | Stack | Max damage | Texture | Area owning behaviour |
|---|---|---|---|---|---|---|---|
| `doorItem01` | `door01` | [`ItemTGDoor`](special-items.md#bunker-door-item-itemtgdoor-techgunsdooritem01) | Bunker Door (`techguns.item.bunkerDoor.name`) | 64 | – | [`textures/items/metalDoor.png`](textures.md#11-tools-placers-door-bucket-debug-items) | 06 (item); door block in [07](../07-blocks/index.md) |
| `gasMask` | `gasMask` | [`ItemGasMask`](../05-armor/special-equipment.md#31-gas-mask-itemgasmask) | Gas Mask | 1 | 300 | [`textures/items/gasMask.png`](textures.md#9-wearables-and-special-slot-icons) | [05](../05-armor/index.md) |
| `nightVisionGoggles` | `nightVisionGoggles` | [`ItemNightVisionGoggles`](../05-armor/special-equipment.md#32-night-vision-goggles-itemnightvisiongoggles) | Nightvision Goggles | 1 | 24000 | [`textures/items/nightVisionGoggles.png`](textures.md#9-wearables-and-special-slot-icons) | [05](../05-armor/index.md) |
| `jetpack` | `jetpack` | [`ItemJetpack`](../05-armor/special-equipment.md#43-jetpack-itemjetpack) | Jetpack | 1 | 5000 | [`textures/items/jetpack.png`](textures.md#9-wearables-and-special-slot-icons) | [05](../05-armor/index.md) |
| `scubaTanks` | `scubaTanks` | [`ItemScubaTanks`](../05-armor/special-equipment.md#41-oxygen-tanks-itemscubatanks) | Oxygen Tanks | 1 | 600 | [`textures/items/scubaTanks.png`](textures.md#9-wearables-and-special-slot-icons) | [05](../05-armor/index.md) |
| `tacticalMask` | `tacticalMask` | [`ItemTacticalMask`](../05-armor/special-equipment.md#33-tactical-mask-itemtacticalmask) | Tactical Mask | 1 | 24000 | [`textures/items/tacticalMask.png`](textures.md#9-wearables-and-special-slot-icons) | [05](../05-armor/index.md) |
| `oreClusterScanner` | `oreClusterScanner` | [`OreClusterScanner`](special-items.md#ore-cluster-scanner-oreclusterscanner-techgunsoreclusterscanner) | Ore Cluster Scanner | 1 | (RF bar) | [`textures/items/oreClusterScanner.png`](textures.md#11-tools-placers-door-bucket-debug-items) | 06 |
| `combatKnife` | `combatKnife` | [`TGSword`](special-items.md#combat-knife-tgsword-techgunscombatknife) | Combat Knife | 1 | 1000 (material) | [`textures/items/combatKnife.png`](textures.md#11-tools-placers-door-bucket-debug-items) | 06 |
| `jumppack` | `jumppack` | [`ItemJumppack`](../05-armor/special-equipment.md#44-jump-pack-itemjumppack) | Jump Pack | 1 | 1000 | [`textures/items/jumppack.png`](textures.md#9-wearables-and-special-slot-icons) | [05](../05-armor/index.md) |
| `antiGravPack` | `antiGravPack` | [`ItemAntiGravPack`](../05-armor/special-equipment.md#45-anti-gravity-device-itemantigravpack) | Anti Gravity Device | 1 | 12000 | [`textures/items/antiGravPack.png`](textures.md#9-wearables-and-special-slot-icons) | [05](../05-armor/index.md) |
| `camoNetPlacer` | `camoNetPlacer` | [`ItemCamoNetPlacer`](special-items.md#camo-net-placer-itemcamonetplacer-techgunscamonetplacer) | Camo Net Placer (all metas) | 1 | – (meta 0–2 = camo) | [`textures/items/camoNetPlacer0.png`](textures.md#11-tools-placers-door-bucket-debug-items) … `camoNetPlacer2.png` | 06 |
| `crowbar` | `crowbar` | [`TGCrowbar`](special-items.md#crowbar-tgcrowbar--tgpickaxe-techgunscrowbar) | Crowbar | 1 | 1000 (material) | [`textures/items/crowbar.png`](textures.md#11-tools-placers-door-bucket-debug-items) | 06 |
| `bucket_acid` | `bucketAcid` | [`GenericItemBucket`](fluids.md#creeper-acid-bucket-item) | Creeper Acid Bucket | 1 | – | [`textures/items/bucket_acid.png`](textures.md#11-tools-placers-door-bucket-debug-items) | 06 (registered in `postInit`) |

For the special-slot items the "max damage" column is the last constructor argument in `TGItems`. Its meaning (charges, ticks, air) is in [05-armor](../05-armor/index.md).

The armour-item constructor's second argument (`"nightVisionGoggles"`, `"jetPackIcon"`, `"oxygenFlasksIcon"`, `"tacticalMaskItem"`, `"jumpPackIcon"`) is used by 05. The inventory icon always comes from the unlocalized name, so it is `textures/items/<registry id>.png`.

Tool material `TG_STEEL` is created with `EnumHelper.addToolMaterial("Steel", harvestLevel 2, maxUses 1000, efficiency 7.5, damage 2.5, enchantability 12)`. Both tools use it. See [special-items.md](special-items.md#tools).

### Registered but commented out
- `medkit` (`ItemMedkit("medkit", 2)`): the registration lines in `TGItems.init()` are commented out, and so is its recipe (and a Chem Lab recipe in [`TGMachineRecipes`](../08-machines-production/index.md#tgmachinerecipesaddrecipes-what-it-registers)). The class, the textures [`textures/items/medkit_0.png`](textures.md#12-unused--unregistered) and `medkit_1.png`, and the lang keys (`techguns.item.medkit_0.name=Bandages`, `techguns.item.medkit_1.name=Medkit`) all exist. Its behaviour is in [special-items.md](special-items.md#medkit-itemmedkit-present-in-code-not-registered).
- `gasMaskHeavy`: commented out.

## 3. Items registered in `Techguns.java` (preInit), not in `TGItems`

These are plain [`GenericItem`](special-items.md#base-classes)s (stack 64, unlocalized `techguns.item.<name>`). **Registration is conditional on config flags** (config file handled by [01-core](../01-core/index.md); all defaults are `false`).

| Registry id | English name | Texture | Registered when | OreDict (same condition) |
|---|---|---|---|---|
| `ingotSteel` | Steel Ingot | [`textures/items/ingotSteel.png`](textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | [`addSteelIngot`](../01-core/config.md#category-general) | `ingotSteel` |
| `ingotCopper` | Copper Ingot | [`textures/items/ingotCopper.png`](textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | not `dontAddCopperTinBronze()` † | `ingotCopper` |
| `ingotTin` | Tin Ingot | [`textures/items/ingotTin.png`](textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | † | `ingotTin` |
| `ingotBronze` | Bronze Ingot | [`textures/items/ingotBronze.png`](textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | † | `ingotBronze` |
| `ingotLead` | Lead Ingot | [`textures/items/ingotLead.png`](textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | [`addLeadIngot`](../01-core/config.md#category-general) | `ingotLead` |
| `nuggetIron` | Iron Nugget | [`textures/items/nuggetIron.png`](textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | [`addIronNuggets`](../01-core/config.md#category-general) (registered **twice**, see bug [ITEM-01](index.md#suspected-bugs)) | `nuggetIron` |
| `nuggetCopper` | Copper Nugget | [`textures/items/copperNugget.png`](textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | [`addCopperTinBronzeNuggets`](../01-core/config.md#category-general) | `nuggetCopper` |
| `nuggetLead` | Lead Nugget | [`textures/items/nuggetLead.png`](textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | [`addLeadNuggets`](../01-core/config.md#category-general) | `nuggetLead` |
| `nuggetSteel` | Steel Nugget | [`textures/items/nuggetSteel.png`](textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | [`addSteelNuggets`](../01-core/config.md#category-general) | `nuggetSteel` |

† `dontAddCopperTinBronze()` is true when [`addBronzeCrafting`](../01-core/config.md#category-general), `addCopperTinBronzeNuggets` and [`doOreGenCopperTin`](../01-core/config.md#category-general) are **all** false. So the three ingots are registered if any one of those flags is on.

`Techguns.java` also registers `OreDictionary "itemGunpowder"` → vanilla gunpowder. The Ammo Press uses it.

Related recipes are in [recipes.md](recipes.md#7-config-dependent-metal-recipes-techgunsjava).

Note that `ingotTitanium` and `ingotObsidianSteel` are always present (shared metas 95 and 40), while steel, lead and copper must come from other mods unless these flags are on. Many core recipes need `ingotSteel`, `ingotLead`, `nuggetCopper`, `nuggetLead`, `nuggetSteel` or `nuggetIron` from the OreDict.

## 4. Debug / developer items (registered unconditionally in `Techguns.java`)

| Registry id | Class | Unlocalized | English name | Texture | Stack |
|---|---|---|---|---|---|
| `structureTestTool` | [`StructureTestTool`](special-items.md#structure-test-tool-structuretesttool) | `techguns.item.structureTestTool` | Structure Test Tool | [`textures/items/structureTestTool.png`](textures.md#11-tools-placers-door-bucket-debug-items) | 1 |
| `blockReaderTool` | [`BlockReader`](special-items.md#block-reader-tool-blockreader) | `techguns.item.blockReaderTool` | Block Reader Tool | [`textures/items/blockReaderTool.png`](textures.md#11-tools-placers-door-bucket-debug-items) | 1 |
| `buildingScanTool` | [`BuildingScanTool`](special-items.md#building-scan-tool-buildingscantool) | `techguns.item.buildingScanTool` | Building Scan Tool | [`textures/items/structureScanTool.png`](textures.md#11-tools-placers-door-bucket-debug-items) | 1 |
| [`oreClusterDebugger`](special-items.md#ore-cluster-debugger-oreclusterdebugger) | `oreClusterDebugger` | `techguns.item.oreClusterDebugger` | Ore Cluster Debugger | [`textures/items/testItem.png`](textures.md#11-tools-placers-door-bucket-debug-items) | 1 |

All four go in the normal Techguns creative tab. Behaviour is in [special-items.md](special-items.md#debug-and-developer-tools).

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

The NPCs themselves are in [10-entities-npcs](../10-entities-npcs/index.md). The item behaviour is in [special-items.md](special-items.md#spawn-eggs-tgmonsterplacer).

## 6. Where the items are consumed (cross-reference)

| Consumer | Items |
|---|---|
| Gun recipes and gun ammo ([02-guns](../02-guns/index.md)) | gun parts (barrels, stocks, receivers, pump), mech parts T1–T3, laser parts, all ammo/mags/tanks/cells, nethercharge, coil, cybernetic parts, sonic emitter, rad emitter, carbon parts |
| Armour recipes ([05-armor](../05-armor/index.md)) | heavyCloth, treatedLeather, steamArmorPlate, powerPlating, carbonFibers, carbonPlate, ingotObsidianSteel, circuitBoardElite, copperWire, goldWire, electricEngine, energyCell(Empty), compressedAirTank(Empty), mechanicalPartsT3 |
| Machines ([08](../08-machines-production/index.md)/[09](../09-machines-utility/index.md)) | machineStackUpgrade (Ammo Press, Metal Press, Chem Lab, Fabricator, Charging Station upgrade slot); turretArmor* (turret base armour slot); oreDrill* heads (Ore Drill); uvemitter / rcHeatRay (Reaction Chamber focus) |
| NPC drops ([10](../10-entities-npcs/index.md)) | bullets9mm, rifleBullets, shotgunShell, heavyCloth, circuitBoard, cyberneticParts, energyCell, ingotObsidianSteel, plasticSheet, plateSteel, rubberBar, flamethrowerTank, rocketAmmo |
| Crowbar | ore cluster blocks ([07](../07-blocks/index.md)/[11](../11-worldgen/index.md)) |

## 7. Lang keys and "Obtained from" for the non-shared items (consistency pass)

### 7.1 Stand-alone items (section 2)

| Registry id | Lang key | Obtained from |
|---|---|---|
| `doorItem01` | `techguns.item.bunkerDoor.name` | Crafting R71 (6 iron plates, yields 2) |
| `gasMask` | `techguns.item.gasMask.name` | Crafting R72; refill R73; dungeon-chest loot |
| `nightVisionGoggles` | `techguns.item.nightVisionGoggles.name` | Crafting R74 (charged) or R75 (empty); dungeon-chest and barracks loot |
| `jetpack` | `techguns.item.jetpack.name` (camo names `techguns.item.jetpack.camoname.1` to `.3`) | Crafting R76 or R77 |
| `scubaTanks` | `techguns.item.scubaTanks.name` | Crafting R79; barracks loot |
| `tacticalMask` | `techguns.item.tacticalMask.name` (camo names `techguns.item.tacticalMask.camoname.0` to `.2`) | Crafting R81 |
| `oreClusterScanner` | `techguns.item.oreClusterScanner.name` | Crafting R83 |
| `combatKnife` | `techguns.item.combatKnife.name` | Crafting R82 |
| `jumppack` | `techguns.item.jumppack.name` (camo names `techguns.item.jumppack.camoname.1` to `.3`) | Crafting R80 |
| `antiGravPack` | `techguns.item.antiGravPack.name` (camo names `techguns.item.antiGravPack.camoname.1` to `.4`) | Crafting R78 |
| `camoNetPlacer` | `techguns.item.camoNetPlacer.name` (messages `techguns.item.camoNetPlacer.info.*`) | Crafting R84 to R86 |
| `crowbar` | `techguns.item.crowbar.name` | Crafting R87 |
| `bucket_acid` | `techguns.item.bucket_acid.name` | No crafting recipe. Creative tab, or filling a vanilla bucket with 1000 mB of creeper acid through the Forge fluid-container mapping registered in `TGItems.postInit` (acid itself comes from the Chem Lab, 08). See [fluids.md](fluids.md) |

### 7.2 Config-dependent metals (section 3)

| Registry id | Lang key | Obtained from |
|---|---|---|
| `ingotSteel` | `techguns.item.ingotSteel.name` | Smelting an iron ingot ([`addSteelCrafting`](../01-core/config.md#category-general)); nugget compression; otherwise from other mods |
| `ingotCopper` | `techguns.item.ingotCopper.name` | Smelting Techguns copper ore (`doOreGenCopperTin`); nugget compression |
| `ingotTin` | `techguns.item.ingotTin.name` | Smelting Techguns tin ore (`doOreGenCopperTin`) |
| `ingotBronze` | `techguns.item.ingotBronze.name` | Shapeless 3 copper + 1 tin (`addBronzeCrafting`) |
| `ingotLead` | `techguns.item.ingotLead.name` | Smelting Techguns lead ore ([`doOreGenLead`](../01-core/config.md#category-general)); nugget compression |
| `nuggetIron` | `techguns.item.nuggetIron.name` | Iron ingot split (`addIronNuggets`) |
| `nuggetCopper` | `techguns.item.nuggetCopper.name` | Copper ingot split (`addCopperTinBronzeNuggets`) |
| `nuggetLead` | `techguns.item.nuggetLead.name` | Lead ingot split (`addLeadNuggets`) |
| `nuggetSteel` | `techguns.item.nuggetSteel.name` | Steel ingot split (`addSteelNuggets`) |

Every default for these flags is false, so with default config these items are *unobtainable from Techguns* (not even registered, in most cases) unless another mod supplies the OreDict item.

### 7.3 Debug items (section 4) and spawn eggs (section 5)

| Item | Lang key | Obtained from |
|---|---|---|
| Structure Test Tool | `techguns.item.structureTestTool.name` (tooltip `techguns.structureTestTool.Tooltip`) | Creative tab only; no recipe |
| Block Reader Tool | `techguns.item.blockReaderTool.name` | Creative tab only |
| Building Scan Tool | `techguns.item.buildingScanTool.name` | Creative tab only |
| Ore Cluster Debugger | `techguns.item.oreClusterDebugger.name` (tooltip `techguns.oreClusterDebugger.Tooltip`) | Creative tab only |
| Spawn eggs (18) | none: the name is the literal "Spawn " plus the entity name ([ITEM-12](index.md#suspected-bugs)) | Creative tab only (meta 0); no recipe |
