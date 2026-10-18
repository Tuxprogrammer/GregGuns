# 09 – Machines (utility / non-recipe tile entities)

This area covers the Techguns 0.1.2 tile entities that do **not** process recipes:

- **Repair Bench:** repairs worn armour from stocked materials.
- **Camo Bench:** cycles camo variants of guns and armour.
- **Scrapper:** an unfinished stub.
- **Ore Drill:** an axis-aligned multiblock that mines ore clusters indefinitely, run on RF or fuel.
- **Turret platform:** powers, arms, heals and respawns an `NPCTurret` entity.
- **Explosive charges:** TNT and C4 tunnel-boring charges.
- **Reinforced and Weapon chests**
- **Camp Flag:** the NPC spawner for generated structures.
- **Bio Blob:** a slime block left by the Biogun.

## Ownership boundaries

| Topic | Owner |
|---|---|
| Base tile classes (`BasicInventoryTileEnt`, `BasicOwnedTileEnt`, `BasicPoweredTileEnt`, `BasicMachineTileEntity`, `MultiBlock*TileEntBase`), base GUIs (power bar, redstone and security buttons), `TechgunsGuiHandler`, the shared `BasicMachine` block, GUI texture files | **08-machines-production** |
| Ore cluster blocks and the drill block textures | **07-blocks** / **11-worldgen** |
| Drill heads, turret armour plates, fuel and engine items | **06-items** |
| Gun ammo mechanics | **02-guns** |
| Armour repair materials | **05-armor** |
| `NPCTurret` AI, the attack helicopter, NPC spawning | **10-entities-npcs** |
| Particles, `ProjectileExplosion`, the Biogun projectile | **04-projectiles-fx** |
| `GUIButtonClick` and the sync packets | **01-core** |

## Contents

| File | Covers |
|---|---|
| [shared-rendering.md](shared-rendering.md) | `BasicMachine` metas owned here, their recipes and item renderers, `RenderTileEntityBlock`/`TesselatorCube`, `RenderBlockTileEntModel`, box-UV convention |
| [repair-bench.md](repair-bench.md) | Repair Bench (meta 5) |
| [camo-bench.md](camo-bench.md) | Camo Bench (meta 3) |
| [scrapper.md](scrapper.md) | Scrapper stub (no scrap table exists) |
| [ore-drill.md](ore-drill.md) | Ore Drill multiblock: blocks, placement, structure, formation, drill heads, yield and power formulas, fuel, GUI, TESR, sounds |
| [turret.md](turret.md) | Turret platform (meta 2) and its link to `NPCTurret`; targeting rules; ammo; armour |
| [explosive-charges.md](explosive-charges.md) | TNT charge (meta 6) and advanced charge (meta 8): timer, blast shape, GUI, models, sounds |
| [misc-tile-entities.md](misc-tile-entities.md) | Reinforced and Weapon chests, Camp Flag (meta 7), Bio Blob |

## Quick reference

| Thing | Block / meta | Tile (registry id) | GUI id | Power |
|---|---|---|---|---|
| Turret | `basicMachine:2` | `TurretBaseEnt` | 2 | 1 000 RF buffer. 5 RF/t upkeep, 500 RF per heal, 50 RF/t while rebuilding. |
| Camo Bench | `basicMachine:3` | `CamoBenchTileEnt` | 3 | none |
| Repair Bench | `basicMachine:5` | `RepairBenchTileEnt` | 5 | none |
| Explosive Charge | `basicMachine:6` | `ExplosiveChargeTileEnt` | 6 | none |
| Camp Flag | `basicMachine:7` | `CampFlagTileEnt` | — | none |
| Adv. Explosive Charge | `basicMachine:8` | `ExplosiveChargeAdvTileEnt` | 6 | none |
| Scrapper | (meta 10, never created) | `ScrapperTileEnt` | 10 | 10 000 RF (unused) |
| Ore Drill | `oreDrill:0–4` | `OreDrillTileEntMaster` (meta 4), `OreDrillTileEntSlave` (0–3) | 16 | 500 000 RF, plus furnace fuel or liquid fuel |
| Reinforced / Weapon Chest | `tgchest`, `tgchest_weapon` | `TGChestTileEnt` | 20 | — |
| Bio Blob | `BioBlobSmall` | `BioblobTileEnt` | — | — |
| (legacy) | none | `OreDrillTileEnt` | — | — |

## Sounds (all in area 09)

| sounds.json event | Files (relative to `assets/techguns`) | When |
|---|---|---|
| `machines.oredrillSmallWork` | `sounds/machines/oredrill_s_work_a.ogg`, `sounds/machines/oredrill_s_work_b.ogg`, `sounds/machines/oredrill_s_work_c.ogg`, `sounds/machines/oredrill_s_work_d.ogg` | radius-0 drill working, client, every 62 ticks, volume 1.5 |
| `machines.oredrillMediumWork` | `sounds/machines/oredrill_m_work_a.ogg`, `sounds/machines/oredrill_m_work_b.ogg`, `sounds/machines/oredrill_m_work_c.ogg`, `sounds/machines/oredrill_m_work_d.ogg` | radius 1–2, volume 2.25 |
| `machines.oredrillLargeWork` | `sounds/machines/oredrill_l_work_a.ogg`, `sounds/machines/oredrill_l_work_b.ogg`, `sounds/machines/oredrill_l_work_c.ogg`, `sounds/machines/oredrill_l_work_d.ogg` | radius 3–4, volume 3.0 |
| `tntcharge.plant` / `.init` / `.tick` | `sounds/effects/DemoCharge_Plant.ogg`, `sounds/effects/DemoCharge_Init.ogg`, `sounds/effects/DemoCharge_Tick.ogg` | TNT charge placed / armed / each second of fuse (volume 4) |
| `c4charge.plant` / `.init` / `.tick` | `sounds/effects/C4Charge_Plant.ogg`, `sounds/effects/C4Charge_Init.ogg`, `sounds/effects/C4Charge_Tick.ogg` | the same for the advanced charge |
| `npcs.turretBeep` | `sounds/npcs/turret_beep-01.ogg` … `sounds/npcs/turret_beep-07.ogg` | **never played** |
| `npcs.turretGunequip` | `sounds/npcs/turret_gunequip-01.ogg`, `sounds/npcs/turret_gunequip-02.ogg`, `sounds/npcs/turret_gunequip-03.ogg` | **never played** |
| `effects.biodeath` | `sounds/effects/biodeath.ogg` | Bio Blob burst (file owned by 04/10) |
| vanilla `random.explode`, `random.chestopen`, `random.chestclosed` | — | charge detonation; chest lid |

## Textures described in this area

### Owned here (model and TESR skins)

| Path | Size | Doc |
|---|---|---|
| `textures/blocks/TurretBase.png`, `textures/blocks/TurretBase_2.png`, `textures/blocks/TurretBase_3.png`, `textures/blocks/TurretBase_4.png`, `textures/blocks/TurretBase_5.png` | 128² | turret.md |
| `textures/blocks/flagpost.png` | 128² | misc-tile-entities.md |
| `textures/blocks/tntCharge.png`, `textures/blocks/tntCharge_2.png`, `textures/blocks/ExpCharge.png`, `textures/blocks/ExpCharge_2.png` | 64² | explosive-charges.md |
| `textures/blocks/reinforced_chest.png`, `textures/blocks/weapon_chest.png` | 128² | misc-tile-entities.md |
| `textures/blocks/reinforced_chest_double.png`, `textures/blocks/weapon_chest_double.png` | 256×128 | misc-tile-entities.md |
| `textures/blocks/drillhead.png`, `textures/blocks/drillheadObsidian.png`, `textures/blocks/drillheadCarbon.png` | 32² | ore-drill.md §9 |
| `textures/blocks/repairbench_textures.png` | 64² | repair-bench.md |
| `textures/blocks/paintmachine_textures.png` | 64² | camo-bench.md |
| `textures/blocks/bio.png` | 16² | misc-tile-entities.md |

None of these is animated (no `.mcmeta`).

### Usage only (files catalogued by 07)

- Ore drill block textures, ore-drill.md §1.2:
  - `textures/blocks/drillFrame_outer.png`, `textures/blocks/drillFrame.png`
  - `textures/blocks/drillRod-ctmv.png`, `textures/blocks/drillRod-top.png`
  - `textures/blocks/drillEngine.png`, `textures/blocks/drillEngineController.png`
  - `textures/blocks/drillFrame_outer_c.png`, `textures/blocks/drillFrame_outer_c-ctm.png`
  - `textures/blocks/drillFrame_c.png`, `textures/blocks/drillFrame_c-ctm.png`
  - `textures/blocks/drillEngine_c.png`, `textures/blocks/drillEngine_c-ctm.png`
  - `textures/blocks/drillEngineTop_c.png`, `textures/blocks/drillEngineTop_c-ctm.png`
  - `textures/blocks/invisible.png`
  - unused: `textures/blocks/drillFrameCorner.png`, `textures/blocks/drillRodSide.png`, `textures/blocks/drillRodTop.png`
- Unused bench and flag textures: `textures/blocks/paintmachine_top.png`, `textures/blocks/paintmachine_front.png`, `textures/blocks/paintmachine_side.png`, `textures/blocks/paintmachine_bottom.png`, `textures/blocks/flagBlock.png`.

### Usage only (GUI files catalogued by 08)

`textures/gui/repairBench_GUI.png`, `textures/gui/camoBench_GUI.png`, `textures/gui/oreDrill_GUI.png`, `textures/gui/turretBase_GUI.png`, `textures/gui/tntChargeGui.png`, `textures/gui/expChargeGui.png`, `textures/gui/tgchest_gui.png`, and `textures/gui/ammoPress_GUI.png` (scrapper background and the powered-GUI side widgets).

## Lang keys used by this area (en_US)

| Key | English |
|---|---|
| `techguns.tile.BasicMachine_2.name` | Turret |
| `techguns.tile.BasicMachine_3.name` | Camo Bench |
| `techguns.tile.BasicMachine_5.name` | Repair Bench |
| `techguns.tile.BasicMachine_6.name` | Explosive Charge |
| `techguns.tile.BasicMachine_7.name` | Flag |
| `techguns.tile.BasicMachine_8.name` | Advanced Explosive Charge |
| `techguns.tile.oreDrill_0.name` … `_4.name` | Ore Drill Frame / Ore Drill Scaffold / Ore Drill Rod / Ore Drill Engine / Ore Drill Controller |
| `techguns.tile.tgchest.name` / `techguns.tile.tgchest_weapon.name` | Reinforced Chest / Weapon Chest |
| `techguns.tile.Bioblob.name` | Bio Blob (key never looked up) |
| `techguns.container.repairbench` | Repair Bench |
| `techguns.container.camobench` | Camo Workbench |
| `techguns.container.turretbase` | Turret Platform |
| `techguns.container.oredrill` | Ore Drill |
| `techguns.container.tgchest` / `.double` | Reinforced Chest / Large Reinforced Chest |
| `techguns.container.tgchest_weapon` / `.double` | Weapon Chest / Large Weapon Chest |
| `techguns.inventory.explosivecharge` | Explosive Charge |
| `techguns.container.repair` | Repair time left |
| `techguns.container.oredrill.broken` / `.broken2` | Multiblock broken / or invalid Drill |
| `techguns.repairBench.noarmor` | No armor |
| `techguns.repairBench.cantrepair` | Can't repair this armor |
| `techguns.repairBench.fullcondition` | Armor is fully repaired |
| `techguns.repairBench.requiredMats` | Required materials for repair: |
| `techguns.item.invalidcamo` | No Camo |
| `techguns.turret.targetAnimals` | Target Animals |
| `techguns.yes` / `techguns.no` | Yes / No |
| `techguns.gui.empty` | Empty |
| `techguns.oredrill.nofuel` | No Fuel |
| `techguns.oredrill.currentfuel` | Fuel |
| `techguns.oredrill.remaining` | Remaining time |
| `techguns.oredrill.ticks` | ticks. |
| `techguns.msg.error.alreadyrod` | Can't place here, this cluster already has a connected drill rod |
| `techguns.msg.error.oredrill.invalidmulti.dir` | The main engine block must have exactly 1 neighboring engine or rod |
| `techguns.msg.error.oredrill.invalid1` | There must be engines followed by rods that are connected to a cluster |
| `techguns.msg.error.oredrill.invalid2` | A drill bigger than 1 rod and 1 engine needs a frame |
| `techguns.msg.error.oredrill.invalid2.1` | A medium drill needs at least 1 engine and a rod of length 2 or more |
| `techguns.msg.error.oredrill.invalid2.2` | A large drill needs at least 2 engine rows and a rod of length 3 or more |
| `techguns.msg.error.oredrill.invalid3` | Invalid block in slice around  (two trailing spaces; the coordinates are appended) |
| `techguns.msg.error.oredrill.controllernext` | Can't place 2 ore drill controllers next to each other (looked up **without** the `techguns.` prefix) |
| `techguns.msg.oredrill.formed1` / `formed2` / `formed3` | "Ore driller multiblock formed with radius: " / ", extra engine size: " / " and drill size: " |
| `techguns.block.hasTileEntityData` | Has TileEntity Data (machine item tooltip) |
| `techguns.block.chestHasContents` | Has contents (chest item tooltip) |
| `techguns.container.security.denied` | [DENIED] You have no permission to use |
| `entity.Techguns.SimpleTurret.name` | Turret |

**Missing keys:** `techguns.container.scrapper` (scrapper title) and `container.campflag` (flag inventory name, never shown).

**Stale keys:** `techguns.tile.OreDrillBlock_0..3.name` and `techguns.tile.oreDrill.name`.

**Hardcoded English in code:**

- Charge GUI: "Radius:", "Length:", "Delay:", "Detonate"
- Ore drill tooltip: "RF/t:", "Ores/h:"

## Shared-class issue noticed (owned by 08)

`MultiBlockMasterTileEntBase.readEntityDataFromNBT` assigns `complete` before comparing it with the new value, so the comparison is always equal and its render-refresh call never runs. This affects the ore drill controller's texture refresh on the client. Recorded here only as a cross-reference; 08 owns the class.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| MUTL-01 | Scrapper unreachable / unfinished | `java/techguns/blocks/machines/BasicMachine.java:203`, `java/techguns/tileentities/ScrapperTileEnt.java:57` | Meta 10 tile creation is commented out, there is no recipe and no creative entry, and `canScrap` never produces anything. The GUI, container and tile registration are all live. | gameplay |
| MUTL-02 | Scrapper tool check inverted | `java/techguns/tileentities/ScrapperTileEnt.java:71` | The tool-material branch only runs when the material name is the *empty* string. It should be "not empty". | gameplay |
| MUTL-03 | Scrapper debug spam | `java/techguns/tileentities/ScrapperTileEnt.java:67` | It prints to stdout every tick while slot 0 is occupied. | cosmetic |
| MUTL-04 | Scrapper shift-click recursion | `java/techguns/inventory/ScrapperContainer.java:106` | `transferStackInSlot` defers to vanilla `Container`, which returns the slot stack unchanged. Vanilla's shift-click retry loop then recurses until the stack overflows. | crash |
| MUTL-05 | Repair bench shift-click off-by-one | `java/techguns/inventory/RepairBenchContainer.java:67` | The player range test is `> 9`, so container slot 9 (the first main-inventory slot) cannot be shift-clicked. | gameplay |
| MUTL-06 | Camo bench back/face buttons missing | `java/techguns/gui/GuiCamoBench.java:179` | The server handles ids 11–14 and the GUI shows the camo numbers, but the buttons are commented out, so face and back items cannot be re-camoed. | gameplay |
| MUTL-07 | Camo bench container never syncs | `java/techguns/inventory/CamoBenchContainer.java:56` | `addCraftingToCrafters` and `detectAndSendChanges` are empty overrides. Server-side slot changes are not pushed to the client (desync risk). | gameplay |
| MUTL-08 | Bench placement gives negative rotation | `java/techguns/blocks/ItemBlockMetaDataMachine.java:73` | Signed modulo produces rotation −1 or −2 ("just don't ask why"). It renders correctly but the stored values are inconsistent. | cosmetic |
| MUTL-09 | Ore drill tick counts overflow `short` | `java/techguns/tileentities/OreDrillTileEntMaster.java:290`, `java/techguns/tileentities/BasicMachineTileEntity.java:49`, `java/techguns/inventory/OreDrillContainer.java:87` | `totalTime` and `progress` are saved as shorts and synced as window-property shorts. Slow clusters (orePerHour < 2.2, e.g. a diamond, emerald or uranium cluster on a radius-0 drill with a carbon head: 96 000 ticks) wrap after a reload and show garbage in the GUI. | gameplay |
| MUTL-10 | Missing cluster leaves slaves linked | `java/techguns/tileentities/OreDrillTileEntMaster.java:312` | `complete` is set false without `unform()`. Slaves keep their formed textures (invisible rods) while the TESR stops drawing. | visual |
| MUTL-11 | Ore drill server runs client branch | `java/techguns/tileentities/OreDrillTileEntMaster.java:122` | When `checkDrill()` fails on the server, the `else` meant for the client runs and increments progress if `haspower`. | gameplay |
| MUTL-12 | Ore drill shift-click off-by-one | `java/techguns/inventory/OreDrillContainer.java:190` | The player range is `< 10+36`, so the last hotbar slot (46) is ignored. | gameplay |
| MUTL-13 | Fuel containers go to output slots | `java/techguns/tileentities/OreDrillTileEntMaster.java:759` | Empty buckets from lava-bucket fuel are inserted into the ore output slots 1–9. | gameplay |
| MUTL-14 | Liquid ore voided when tank full | `java/techguns/tileentities/OreDrillTileEntMaster.java:165` | The output tank's fill result is ignored and the excess is lost. | gameplay |
| MUTL-15 | Drill render box too small for r 3–4 | `java/techguns/tileentities/OreDrillTileEntMaster.java:78` | The half-width is 2 only for radius 2, else 1, so large drills cull early. | visual |
| MUTL-16 | Drill TESR samples light on the wrong axis | `java/techguns/client/renderer/tileentity/RenderOreDrill.java:95` (also 125, 178, 208) | It uses the inventory `rotation` (always 0 → +Y) instead of `dir` for per-segment lighting positions. | visual |
| MUTL-17 | Drill spin interpolation wrong | `java/techguns/client/renderer/tileentity/RenderOreDrill.java:65` | It adds partialTick ÷ 36 degrees instead of partialTick × 10, so the spin stutters at 10° steps. | cosmetic |
| MUTL-18 | Engine-top texture never chosen | `java/techguns/blocks/machines/OreDrillBlock.java:232` | The direction-based icon selection is marked "does not work", and the CTM path (`getManager`) ignores it. `drillEngineTop_c` is unused. | visual |
| MUTL-19 | Slave `bottomrow` never cleared | `java/techguns/blocks/machines/OreDrillBlock.java:770` | It is only ever set true, so a block that was in a previous last slice keeps hidden faces after re-forming in another role (until reload). | visual |
| MUTL-20 | Controller message key unprefixed | `java/techguns/blocks/ItemBlockOreDrill.java:137` | `I18n` looks up `msg.error.oredrill.controllernext`, but the lang key is `techguns.msg.…`, so the raw key is shown. | cosmetic |
| MUTL-21 | Ore drill blocks claim to be normal cubes | `java/techguns/blocks/machines/OreDrillBlock.java:884` | `isNormalCube` is true for the see-through scaffold and invisible formed rods, which affects lighting and suffocation. | visual |
| MUTL-22 | Drill tooltip divides by zero | `java/techguns/gui/GuiOreDrill.java:58` | The "%" and "Ores/h" lines divide by `totalTime`, which can be 0 (NaN/Infinity). | cosmetic |
| MUTL-23 | Legacy ore drill still registered | `java/techguns/tileentities/OreDrillTileEnt.java:21`, `java/techguns/Techguns.java:576` | A dead prototype tile (hard-coded iron ore, 1000 ÷ size timer) is registered; `OreDrill` block is unused. | cosmetic |
| MUTL-24 | Turret reload drops empty stacks | `java/techguns/tileentities/TurretBaseEnt.java:196` | An item entity with the overflow count is spawned even when the overflow is 0. | cosmetic |
| MUTL-25 | Turret links an arbitrary entity by stale id | `java/techguns/tileentities/TurretBaseEnt.java:89` | `mountedTurret` stores an entity id. On read (for example placing a wrenched turret item) whatever entity has that id is cast to `NPCTurret`, which throws ClassCastException if it is a different type. | crash |
| MUTL-26 | Turret power synced every tick | `java/techguns/inventory/TurretBaseContainer.java:106` | A window property is sent each tick unconditionally. | cosmetic |
| MUTL-27 | Client class referenced in common tile | `java/techguns/tileentities/TurretBaseEnt.java:248` | `Minecraft.getSystemTime()` is in common code. It is guarded by `isRemote`, but is a dedicated-server class-loading risk. | crash |
| MUTL-28 | Turret sound events unused | `assets/techguns/sounds.json` (`npcs.turretBeep`, `npcs.turretGunequip`) | Defined with 10 files, but nothing plays them. | cosmetic |
| MUTL-29 | Dead `TurretBase` block | `java/techguns/blocks/TurretBase.java:43` | An unregistered block that would spawn `TurretEntityUNUSED`. Dead code. | cosmetic |
| MUTL-30 | Charge break event with null player | `java/techguns/tileentities/ExplosiveChargeTileEnt.java:150` | `BlockEvent.BreakEvent` is built with the owner looked up by UUID. That is null when the owner is offline, in another dimension, or the charge is unowned, which is likely an NPE inside Forge's event/harvest checks. The fuse and explosion also run on the client, which posts the event there too. | crash |
| MUTL-31 | Charge settings lack permission check | `java/techguns/tileentities/ExplosiveChargeTileEnt.java:283` | Button ids 1–6 change radius, length and fuse without `isUseableByPlayer`, so any client packet can edit any unarmed charge. | gameplay |
| MUTL-32 | Charge GUI not localised | `java/techguns/gui/GuiExplosiveCharge.java:50` | "Radius:", "Length:", "Delay:" and "Detonate" are literals. | cosmetic |
| MUTL-33 | TNT radius buttons are no-ops | `java/techguns/tileentities/ExplosiveChargeTileEnt.java:24` | Min = max = 3 for the TNT charge, but +/− radius buttons are still shown. | cosmetic |
| MUTL-34 | Chest viewer recount resets while open | `java/techguns/tileentities/TGChestTileEnt.java:343` | The recount only counts `ContainerChest` viewers. `TGChestContainer` is not one, so the server sets `numPlayersUsing` to 0 every 200 ticks (weapon-chest redstone drops; closing can go negative). | gameplay |
| MUTL-35 | Weapon chest acts as a trapped chest | `java/techguns/blocks/BlockTGChest.java:613` | `canProvidePower` is true for type 1 (copied from the vanilla trapped-chest type flag), so opening a weapon chest emits redstone. | gameplay |
| MUTL-36 | Chest shift-click fills TG slots | `java/techguns/inventory/TGChestContainer.java:84` | Merging from the chest targets all later slots in reverse, so the TG ammo and auto-food slots are filled first, with no item-validity check. | gameplay |
| MUTL-37 | Chest item renderer debug offset | `java/techguns/client/renderer/item/RenderBlockModelItemChest.java:27` | The equipped transform adds `Keybinds.X/Y/Z` debug offsets. | visual |
| MUTL-38 | Camp flag cap lost on reload | `java/techguns/tileentities/CampFlagTileEnt.java:144` | `maxNPCs` is recomputed from size on load. The carrier's explicit 9 becomes 28. | gameplay |
| MUTL-39 | Uninitialised camp flag crashes | `java/techguns/tileentities/CampFlagTileEnt.java:123` (also 161) | A flag placed without `init` (pick-block / give `basicMachine:7`) has a null NPC list and spawn array. Counting NPCs throws an NPE, and saving writes a null int array. | crash |
| MUTL-40 | Flag item renders as repair bench | `java/techguns/client/ClientProxy.java:607` | The item-renderer slot for meta 7 uses the repair-bench cube and texture. | visual |
| MUTL-41 | Bio Blob poison skips players | `java/techguns/blocks/Bioblob.java:147` | It casts to `EntityLiving` and swallows ClassCastException, so players (`EntityLivingBase` only) are never poisoned. | gameplay |
| MUTL-42 | Bio Blob collision box not per-state | `java/techguns/blocks/Bioblob.java:116` | Bounds are only set for selection and render. The collision box uses whatever bounds were last set on the shared block instance. | gameplay |
| MUTL-43 | Bio Blob name key mismatch | `java/techguns/TGBlocks.java:220` | Registered with a plain `ItemBlock` (key `tile.Bioblob.name`), but the lang defines `techguns.tile.Bioblob.name`. | cosmetic |
| MUTL-44 | Bio Blob decays client-side too | `java/techguns/tileentities/BioblobTileEnt.java:125` | The decay timer and removal run on both sides, so the client can delete or shrink the blob independently. | visual |
| MUTL-45 | Bio Blob `side` field dead | `java/techguns/tileentities/BioblobTileEnt.java:25` | It is saved and synced, but never set. The side lives in block metadata instead. | cosmetic |
