# Configuration (`config/Techguns.cfg`)

Source: `java/techguns/TGConfig.java`. The file is loaded once in preInit through Forge's `Configuration` class and saved straight away. `general` is Forge's default category. Ranges are inclusive, and Forge clamps out-of-range values to them. The config is not synced: every value is read locally on whichever side uses it, so server-side settings must be set on the server.

A second file, `config/Techguns_WeaponStats.cfg` (category `Weapon Stats`, one double-list key `WeaponStats_<gun unlocalized name>` per gun), is written by `TGWeaponStats` and documented in [02-guns](../02-guns/index.md).

## Category `general`

| Key | Type | Default | Range | Effect / where used |
|---|---|---|---|---|
| `doOreGenLead` | bool | false | | Adds the lead ore block, its oredict entry and furnace recipe, and enables lead in [`OreGenerator`](../11-worldgen/ore-generation.md#1-ore-veins-oregenerator) ([11](../11-worldgen/index.md)). Comment: "Generate Lead Ore, disable if other mod does (like Thermal Expansion)" |
| `doOreGenCopperTin` | bool | false | | Same, for the copper and tin ores. Also turns on the copper/tin/bronze ingot group |
| `doOreGenTitanium` | bool | true | | Passed to `OreGenerator` (titanium-iron ore, 11) |
| `doOreGenUranium` | bool | true | | Passed to `OreGenerator` (uranium; the comment mentions oredict `oreUranium`) |
| `addSteelCrafting` | bool | false | | Furnace recipe: iron ingot → Techguns steel ingot |
| `addBronzeCrafting` | bool | false | | Shapeless 3 copper + 1 tin → 4 bronze. Also turns on the copper/tin/bronze ingot group |
| `addCopperTinBronzeNuggets` | bool | false | | Adds the copper nugget item and its recipes. Also turns on the copper/tin/bronze ingot group |
| `addLeadIngot` | bool | false | | Registers the lead ingot item and its oredict entry |
| `addLeadNuggets` | bool | false | | Lead nugget item and recipes |
| `addSteelIngot` | bool | false | | Registers the steel ingot item and its oredict entry |
| `addSteelNuggets` | bool | false | | Steel nugget item and recipes. If `addSteelIngot` is false, the recipes are built in postInit against the first oredict [`ingotSteel`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) |
| `addIronNuggets` | bool | false | | Iron nugget item and recipes. **Crash when enabled** (double registration, [CORE-01](index.md#suspected-bugs)) |
| `disableAutofeeder` | bool | false | | Turns off automatic eating from the Techguns food slots ([player-state.md](player-state.md#auto-feeder)) |
| `addCustomOreCluster` | bool | false | | Adds block [`oreClusterCustom`](../07-blocks/block-catalogue.md#registered-blocks), an empty ore cluster that MineTweaker can fill ([07](../07-blocks/index.md)/[08](../08-machines-production/index.md)) |
| `addCustomOreCluster2` | bool | false | | Adds a second one, [`oreClusterCustom2`](../07-blocks/block-catalogue.md#registered-blocks) |
| `SpawnStructures` | bool | true | | Registers [`WorldGenTGStructureSpawn`](../11-worldgen/index.md#overview) ([11](../11-worldgen/index.md)) |
| `StructureSpawnWeightBig` | int | 64 | 1–100000 | A big structure is attempted in chunks where both chunkX and chunkZ are divisible by this value ([11](../11-worldgen/index.md)) |
| `StructureSpawnWeightSmall` | int | 16 | 1–100000 | Same, for small structures |
| `StructureSpawnWeightMedium` | int | 32 | 1–100000 | Same, for medium structures |
| `ForceSafeModeForNonOps` | bool | false | | Server side. When true, non-op players can't turn safe mode off: the toggle reverts and the flag is forced back on at load. Checked by `TGConfig.allowUnsafemode(player)`, which returns `isOp || !ForceSafeModeForNonOps` |
| `MachinesNeedNoPower` | bool | false | | Power checks in [`BasicPoweredTileEnt`](../08-machines-production/framework.md#4-energy-basicpoweredtileent) and the ore drill always succeed ([08](../08-machines-production/index.md)/[09](../09-machines-utility/index.md)) |
| `RestrictedLoottables` | bool | false | | Techguns NPCs drop only vanilla-mob-style or Techguns-specific loot ([10](../10-entities-npcs/index.md)) |
| `ExplosiveChargeMaxHardness` | float | 30.0 | 0–Float.MAX | A normal explosive charge can only break blocks with 0 ≤ hardness < value (obsidian is 50) ([09](../09-machines-utility/index.md)) |
| `ExplosiveChargeAdvancedMaxHardness` | float | 100.0 | 0–Float.MAX | Same, for the advanced charge |

## Category `ID Conflicts`

| Key | Type | Default | Range | Effect |
|---|---|---|---|---|
| `DataWatcherID_FaceSlot` | int | 23 | 2–31 | DataWatcher slot (type 5, ItemStack) on every `EntityPlayer`, used to sync the face slot item for rendering. Must not collide with vanilla (0, 1, 6, 7, 8, 9, 16, 17, 18) or other mods. A collision crashes on player construction |
| `DataWatcherID_BackSlot` | int | 24 | 2–31 | Same, for the back slot |
| `TechgunsGUI_TabID` | int | 17 | 0–1000 | GUI ID the vanilla-inventory tab button asks the server to open. **The GUI handler and the TConstruct tab ignore it and hard-code 17** ([CORE-07](index.md#suspected-bugs)) |

## Category `NPC Spawn`

| Key | Type | Default | Range | Effect ([10-entities-npcs](../10-entities-npcs/index.md)) |
|---|---|---|---|---|
| `Techguns Spawnweight Overworld` | int | 600 | 0–10000 | Spawn weight of the placeholder spawn entity [`TGDummySpawn`](../10-entities-npcs/per-mob.md#18-tgdummyspawn-tgdummyspawn) in all overworld biomes (monster type, group 1–2). The spawn manager replaces it with a real NPC |
| `Techguns Spawnweight NEther` (sic) | int | 300 | 0–10000 | Same, in Nether-type biomes |
| `SpawnWeightZombieSoldier` | int | 100 | 0–10000 | Weight in the Techguns spawn manager (danger level 1) |
| `SpawnWeightZombieFarmer` | int | 200 | 0–10000 | Danger level 0 |
| `SpawnWeightZombieMiner` | int | 200 | 0–10000 | Danger level 0 |
| `SpawnWeightZombiePigmanSoldier` | int | 100 | 0–10000 | Nether only (dim −1), level 0 |
| `SpawnWeightCyberDemon` | int | 30 | 0–10000 | Nether only, level 0 |
| `SpawnWeightBandit` | int | 50 | 0–10000 | Level 2 |
| `SpawnWeightSkeletonSoldier` | int | 100 | 0–10000 | Level 1 |
| `SpawnWeightPsychoSteve` | int | 3 | 0–10000 | Level 1 (early boss) |
| `DistanceSpawnLevel0` | int | 500 | 0–Int.MAX | Within this distance of world spawn (times a factor), only danger level ≤ 0 spawns |
| `DistanceSpawnLevel1` | int | 1000 | 0–Int.MAX | ≤ 1 within this distance |
| `DistanceSpawnLevel2` | int | 2500 | 0–Int.MAX | ≤ 2 within this distance; beyond it, level 3 |

The comments say "at 0 spawn will not be registered". Whether that holds depends on the spawn manager ([10](../10-entities-npcs/index.md)).

## Category `Ore Drills`

| Key | Type | Default | Range | Effect ([08-machines-production](../08-machines-production/index.md)) |
|---|---|---|---|---|
| `oreDrillMultiplierOre` | float | 1.0 | 0.001–1000 | Multiplies ore output per hour |
| `oreDrillMultiplierPower` | float | 1.0 | 0–1000 | Multiplies RF per tick |
| `oreDrillFuelMultiplier` | float | 1000 | 1–100000 | Internal RF per unit of furnace burn time (burnTime × value = RF) |
| `oreDrillFuelValueFuel` | float | 96 | 1–100000 | Fuel value of Techguns liquid Fuel per mB |
| `oreDrillFuelValueBioFuel` | float | 48 | 1–100000 | Fuel value of MFR BioFuel per mB |
| `oreClusterMultipliersAmount` | double[16] | 2.0, 1.0, 1.0, 1.2, 1.0, 0.25, 0.25, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.35, 0.25 | 0–1000 each | Per-metadata amount multiplier for [`oreCluster`](../07-blocks/block-catalogue.md#registered-blocks): iron, gold, redstone, coal, lapis, diamond, emerald, copper, tin, lead, osmium, silver, nickel, aluminium, platinum, uranium |
| `oreClusterMultipliersPower` | double[16] | 0.15, 0.4, 0.3, 0.25, 0.3, 1.0, 1.0, 0.1, 0.1, 0.2, 0.3, 0.35, 0.3, 0.2, 1.0, 1.0 | 0–1000 | Per-metadata power multiplier, same order |
| `oreClusterMultipliersAmount2` | double[16] | 1.5, 0.5, 1.5, 1.0, 1.5, then 1.0 × 11 | 0–1000 | For [`oreCluster2`](../07-blocks/block-catalogue.md#registered-blocks): nether quartz, titanium, certus quartz, oil, glowstone, then unused |
| `oreClusterMultipliersPower2` | double[16] | 0.35, 0.8, 0.4, 0.5, 0.35, then 1.0 × 11 | 0–1000 | Power multipliers, same order |

## Category `Spawn Controll` (sic)

| Key | Type | Default | Range | Effect |
|---|---|---|---|---|
| `spawnOilHoles` | bool | true | | Registers [`WorldGenOilHoles`](../11-worldgen/ore-generation.md#3-oil-holes-worldgenoilholes) if an oil fluid exists ([11](../11-worldgen/index.md)) |
| `oilHoleFrequency` | float | 0.05 | 0–1 | Per-chunk chance (`random.nextFloat() < value`) of spawning an oil hole |

## Category `Damage Factors` (used by `damagesystem.DamageSystem`, described in 02/05)

| Key | Type | Default | Range | Effect |
|---|---|---|---|---|
| `DamagePvP` | float | 0.5 | 0–100 | Damage multiplier when a player hits a player; 0 if server PvP is off |
| `DamageTurretToPlayer` | float | 0.5 | 0–100 | Multiplier when a turret ([`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret)) hits a player |
| `DamageFactorNPC` | float | 1.0 | 0–100 | Multiplier for other NPCs hitting players, and for NPC-vs-NPC |
| `ArmorPenetrationMultiplierNPCtoPlayer` | float | 1.0 | 0–1 | Scales armor penetration of non-player attackers against players |
| `ArmorPenetrationMultiplierPvP` | float | 1.0 | 0–1 | Scales armor penetration in PvP |

## Category `Debug`

| Key | Type | Default | Effect |
|---|---|---|---|
| `enableDebugKeybinds` | bool | false | Registers the numpad debug keybinds ([input-and-hud.md](input-and-hud.md#debug-keybinds)) |

## Category `Clientside`

| Key | Type | Default | Range | Effect |
|---|---|---|---|---|
| `EnableDeathEffects` | bool | true | | When false, the client ignores [`PacketEntityDeathType`](networking.md#6--packetentitydeathtype-sc) entirely (no gore, bio or laser death visuals or sounds) |
| `EnableGoreDeathEffect` | bool | true | | When false, GORE death packets are ignored but other death types still play |
| `LockSpeedDependantFov` | bool | true | | Cancels the vanilla speed-dependent FOV change ([input-and-hud.md](input-and-hud.md#fov-and-zoom)) |
| `FixedSprintFovMultiplier` | float | 1.15 | 1.0–10.0 | **Unused.** The FOV code hard-codes 1.2 ([CORE-12](index.md#suspected-bugs)) |
| `EnableVersionChecker` | bool | true | | Starts the background update check. **Setting it false crashes the client with an NPE every player tick** ([CORE-02](index.md#suspected-bugs)) |
| `EnableVersionCheckerBeta` | bool | true | | Also warns about newer beta versions ([client-effects-audio.md](client-effects-audio.md#version-checker)) |

## Helpers

- `TGConfig.allowUnsafemode(player)`: true if the player is an op on the running server, or if `ForceSafeModeForNonOps` is false.
- `TGConfig.getVolumeMultiplier(volume, category)`: a stub that returns the volume unchanged. [`TGSoundCategory`](client-effects-audio.md#tgsoundcategory) therefore has no effect on loudness.
