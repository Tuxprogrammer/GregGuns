# 09 – Machines (utility / non-recipe tile entities)

## Overview


This area covers the Techguns 0.1.2 tile entities that do **not** process recipes:

- **Repair Bench:** repairs worn armour from stocked materials.
- **Camo Bench:** cycles camo variants of guns and armour.
- **Scrapper:** an unfinished stub.
- **Ore Drill:** an axis-aligned multiblock that mines ore clusters indefinitely, run on RF or fuel.
- **Turret platform:** powers, arms, heals and respawns an [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) entity.
- **Explosive charges:** TNT and C4 tunnel-boring charges.
- **Reinforced and Weapon chests**
- **Camp Flag:** the NPC spawner for generated structures.
- **Bio Blob:** a slime block left by the Biogun.

## Contents

| File | Covers |
|---|---|
| [shared-rendering.md](shared-rendering.md) | `BasicMachine` metas owned here, their recipes and item renderers, [`RenderTileEntityBlock`](../08-machines-production/rendering.md#4-rendertileentityblock-used-by-09s-camo-and-repair-benches)/[`TesselatorCube`](../07-blocks/block-rendering-and-models.md#tesselatorcube-family-fixed-uvs-for-tesrs-and-items), [`RenderBlockTileEntModel`](../08-machines-production/rendering.md#3-renderblocktileentmodel-used-by-09s-turret-base-and-flag), box-UV convention |
| [repair-bench.md](repair-bench.md) | Repair Bench (meta 5) |
| [camo-bench.md](camo-bench.md) | Camo Bench (meta 3) |
| [scrapper.md](scrapper.md) | Scrapper stub (no scrap table exists) |
| [ore-drill.md](ore-drill.md) | Ore Drill multiblock: blocks, placement, structure, formation, drill heads, yield and power formulas, fuel, GUI, TESR, sounds |
| [turret.md](turret.md) | Turret platform (meta 2) and its link to `NPCTurret`; targeting rules; ammo; armour |
| [explosive-charges.md](explosive-charges.md) | TNT charge (meta 6) and advanced charge (meta 8): timer, blast shape, GUI, models, sounds |
| [misc-tile-entities.md](misc-tile-entities.md) | Reinforced and Weapon chests, Camp Flag (meta 7), Bio Blob |
| [entity-sheets.md](entity-sheets.md) | One ten-field entity sheet per machine, tile entity and block family (identity, obtaining, numbers, behaviour, sync, UI, visuals, audio, relationships, bugs) |
| [textures.md](textures.md) | Measured size and colours for every texture and GUI file, palette in words, derived pixel rectangles for the model textures |
| [lang.md](lang.md) | Every lang key with English, generated strings, hard-coded English, missing and stale keys |
| [sounds.md](sounds.md) | Every sound event and file: trigger, volume, pitch, duration, rate, channels |

## Key facts

| Thing | Block / meta | Tile (registry id) | GUI id | Power |
|---|---|---|---|---|
| Turret | `basicMachine:2` | [`TurretBaseEnt`](turret.md#tile-entity-turretbaseent) | 2 | 1 000 RF buffer. 5 RF/t upkeep, 500 RF per heal, 50 RF/t while rebuilding. |
| Camo Bench | `basicMachine:3` | [`CamoBenchTileEnt`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) | 3 | none |
| Repair Bench | `basicMachine:5` | [`RepairBenchTileEnt`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) | 5 | none |
| Explosive Charge | `basicMachine:6` | [`ExplosiveChargeTileEnt`](explosive-charges.md#nbt-explosivechargetileent) | 6 | none |
| Camp Flag | `basicMachine:7` | [`CampFlagTileEnt`](shared-rendering.md#1-basicmachine-metas-owned-by-area-09) | — | none |
| Adv. Explosive Charge | `basicMachine:8` | [`ExplosiveChargeAdvTileEnt`](explosive-charges.md) | 6 | none |
| Scrapper | (meta 10, never created) | [`ScrapperTileEnt`](scrapper.md) | 10 | 10 000 RF (unused) |
| Ore Drill | `oreDrill:0–4` | [`OreDrillTileEntMaster`](ore-drill.md#5-operation-oredrilltileentmaster) (meta 4), [`OreDrillTileEntSlave`](entity-sheets.md#4-ore-drill-block-family-oredrill-metas-04) (0–3) | 16 | 500 000 RF, plus furnace fuel or liquid fuel |
| Reinforced / Weapon Chest | [`tgchest`](../07-blocks/block-catalogue.md#registered-blocks), [`tgchest_weapon`](../07-blocks/block-catalogue.md#registered-blocks) | [`TGChestTileEnt`](misc-tile-entities.md#tile-entity-tgchesttileent) | 20 | — |
| Bio Blob | [`BioBlobSmall`](../07-blocks/block-catalogue.md#registered-blocks) | [`BioblobTileEnt`](misc-tile-entities.md#tile-entity-bioblobtileent) | — | — |
| (legacy) | none | [`OreDrillTileEnt`](ore-drill.md#11-legacy-prototype-dead-code) | — | — |

## Sounds (all in area 09)

| sounds.json event | Files (relative to `assets/techguns`) | When |
|---|---|---|
| `machines.oredrillSmallWork` | [`sounds/machines/oredrill_s_work_a.ogg`](../08-machines-production/sounds.md#1-events-and-files), [`sounds/machines/oredrill_s_work_b.ogg`](../08-machines-production/sounds.md#1-events-and-files), [`sounds/machines/oredrill_s_work_c.ogg`](../08-machines-production/sounds.md#1-events-and-files), [`sounds/machines/oredrill_s_work_d.ogg`](../08-machines-production/sounds.md#1-events-and-files) | radius-0 drill working, client, every 62 ticks, volume 1.5 |
| `machines.oredrillMediumWork` | [`sounds/machines/oredrill_m_work_a.ogg`](../08-machines-production/sounds.md#1-events-and-files), [`sounds/machines/oredrill_m_work_b.ogg`](../08-machines-production/sounds.md#1-events-and-files), [`sounds/machines/oredrill_m_work_c.ogg`](../08-machines-production/sounds.md#1-events-and-files), [`sounds/machines/oredrill_m_work_d.ogg`](../08-machines-production/sounds.md#1-events-and-files) | radius 1–2, volume 2.25 |
| `machines.oredrillLargeWork` | [`sounds/machines/oredrill_l_work_a.ogg`](../08-machines-production/sounds.md#1-events-and-files), [`sounds/machines/oredrill_l_work_b.ogg`](../08-machines-production/sounds.md#1-events-and-files), [`sounds/machines/oredrill_l_work_c.ogg`](../08-machines-production/sounds.md#1-events-and-files), [`sounds/machines/oredrill_l_work_d.ogg`](../08-machines-production/sounds.md#1-events-and-files) | radius 3–4, volume 3.0 |
| `tntcharge.plant` / `.init` / `.tick` | [`sounds/effects/DemoCharge_Plant.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/DemoCharge_Init.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/DemoCharge_Tick.ogg`](../04-projectiles-fx/sounds.md) | TNT charge placed / armed / each second of fuse (volume 4) |
| `c4charge.plant` / `.init` / `.tick` | [`sounds/effects/C4Charge_Plant.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/C4Charge_Init.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/C4Charge_Tick.ogg`](../04-projectiles-fx/sounds.md) | the same for the advanced charge |
| `npcs.turretBeep` | [`sounds/npcs/turret_beep-01.ogg`](../10-entities-npcs/sounds.md#event-table) … [`sounds/npcs/turret_beep-07.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | **never played** |
| `npcs.turretGunequip` | [`sounds/npcs/turret_gunequip-01.ogg`](../10-entities-npcs/sounds.md#event-table), [`sounds/npcs/turret_gunequip-02.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54), [`sounds/npcs/turret_gunequip-03.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | **never played** |
| `effects.biodeath` | [`sounds/effects/biodeath.ogg`](../04-projectiles-fx/sounds.md) | Bio Blob burst (file owned by [04](../04-projectiles-fx/index.md)/[10](../10-entities-npcs/index.md)) |
| vanilla `random.explode`, `random.chestopen`, `random.chestclosed` | — | charge detonation; chest lid |

## Textures described in this area

### Owned here (model and TESR skins)

| Path | Size | Doc |
|---|---|---|
| [`textures/blocks/TurretBase.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/TurretBase_2.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/TurretBase_3.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/TurretBase_4.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/TurretBase_5.png`](../07-blocks/textures.md#11-model-textures) | 128² | turret.md |
| [`textures/blocks/flagpost.png`](../07-blocks/textures.md#11-model-textures) | 128² | misc-tile-entities.md |
| [`textures/blocks/tntCharge.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/tntCharge_2.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/ExpCharge.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/ExpCharge_2.png`](../07-blocks/textures.md#11-model-textures) | 64² | explosive-charges.md |
| [`textures/blocks/reinforced_chest.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/weapon_chest.png`](../07-blocks/textures.md#11-model-textures) | 128² | misc-tile-entities.md |
| [`textures/blocks/reinforced_chest_double.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/weapon_chest_double.png`](../07-blocks/textures.md#11-model-textures) | 256×128 | misc-tile-entities.md |
| [`textures/blocks/drillhead.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillheadObsidian.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillheadCarbon.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32² | ore-drill.md §9 |
| [`textures/blocks/repairbench_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | 64² | repair-bench.md |
| [`textures/blocks/paintmachine_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | 64² | camo-bench.md |
| [`textures/blocks/bio.png`](../07-blocks/textures.md#8-bug-nest-bio-blob) | 16² | misc-tile-entities.md |

None of these is animated (no `.mcmeta`).

### Usage only (files catalogued by 07)

- Ore drill block textures, ore-drill.md §1.2:
  - [`textures/blocks/drillFrame_outer.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillFrame.png`](../07-blocks/textures.md#ore-drill-oredrill-08)
  - [`textures/blocks/drillRod-ctmv.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillRod-top.png`](../07-blocks/textures.md#ore-drill-oredrill-08)
  - [`textures/blocks/drillEngine.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillEngineController.png`](../07-blocks/textures.md#ore-drill-oredrill-08)
  - [`textures/blocks/drillFrame_outer_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillFrame_outer_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08)
  - [`textures/blocks/drillFrame_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillFrame_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08)
  - [`textures/blocks/drillEngine_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillEngine_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08)
  - [`textures/blocks/drillEngineTop_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillEngineTop_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08)
  - [`textures/blocks/invisible.png`](../07-blocks/textures.md#4-camo-nets-sandbags-tent-flag-air-marker-invisible)
  - unused: [`textures/blocks/drillFrameCorner.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillRodSide.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillRodTop.png`](../07-blocks/textures.md#ore-drill-oredrill-08)
- Unused bench and flag textures: [`textures/blocks/paintmachine_top.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09), [`textures/blocks/paintmachine_front.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09), [`textures/blocks/paintmachine_side.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09), [`textures/blocks/paintmachine_bottom.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09), [`textures/blocks/flagBlock.png`](../07-blocks/textures.md#4-camo-nets-sandbags-tent-flag-air-marker-invisible).

### Usage only (GUI files catalogued by 08)

[`textures/gui/repairBench_GUI.png`](../08-machines-production/gui-textures.md#8-texturesguirepairbench_guipng-owned-by-09), [`textures/gui/camoBench_GUI.png`](../08-machines-production/gui-textures.md#7-texturesguicamobench_guipng-owned-by-09), [`textures/gui/oreDrill_GUI.png`](../08-machines-production/gui-textures.md#9-texturesguioredrill_guipng-owned-by-09), [`textures/gui/turretBase_GUI.png`](../08-machines-production/gui-textures.md#12-texturesguiturretbase_guipng-owned-by-09), [`textures/gui/tntChargeGui.png`](../08-machines-production/gui-textures.md#11-texturesguitntchargeguipng-owned-by-09), [`textures/gui/expChargeGui.png`](../08-machines-production/gui-textures.md#10-texturesguiexpchargeguipng-owned-by-09), [`textures/gui/tgchest_gui.png`](../08-machines-production/gui-textures.md#13-texturesguitgchest_guipng-owned-by-09), and [`textures/gui/ammoPress_GUI.png`](../08-machines-production/gui-textures.md#1-texturesguiammopress_guipng) (scrapper background and the powered-GUI side widgets).

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

[`MultiBlockMasterTileEntBase.readEntityDataFromNBT`](../08-machines-production/framework.md#master-multiblockmastertileentbase) assigns `complete` before comparing it with the new value, so the comparison is always equal and its render-refresh call never runs. This affects the ore drill controller's texture refresh on the client. Recorded here only as a cross-reference; [08](../08-machines-production/index.md) owns the class.

## Related areas

- [08-machines-production](../08-machines-production/index.md): the shared machine framework: energy, security, redstone, upgrades, multiblock bases, GUI handler and GUI textures.
- [07-blocks](../07-blocks/index.md): the block registrations and every block texture these machines use.
- [10-entities-npcs](../10-entities-npcs/index.md): the turret entity's AI, camp-flag NPCs and spawners.
- [02-guns](../02-guns/index.md): guns mounted in turrets and repaired on the repair bench.
- [05-armor](../05-armor/index.md): armour on the camo bench and turret armour.
- [11-worldgen](../11-worldgen/index.md): ore clusters the drill mines, and structures that place camp flags and chests.
- [04-projectiles-fx](../04-projectiles-fx/index.md): explosion behaviour behind the explosive charges, and Bio Blob effects.

## Coverage

Source files read for this area (all under `java/techguns/`):

- Blocks: `blocks/machines/BasicMachine`, `OreDrillBlock`, `blocks/BlockTGChest`, `blocks/Bioblob`, `blocks/ItemBlockMetaDataMachine`, `ItemBlockOreDrill`, [`ItemBlockTGChest`](../07-blocks/block-catalogue.md#conventions-and-defaults).
- Tiles: `RepairBenchTileEnt`, `CamoBenchTileEnt`, `ScrapperTileEnt`, `OreDrillTileEntMaster`, `OreDrillTileEntSlave`, `OreDrillTileEnt`, `TurretBaseEnt`, `ExplosiveChargeTileEnt`, `ExplosiveChargeAdvTileEnt`, `TGChestTileEnt`, `CampFlagTileEnt`, `BioblobTileEnt`.
- GUIs and containers: the matching `Gui*` and `*Container` classes, plus the TESRs and item renderers named in the per-machine files.
- Re-checked in the consistency pass against the reference source: the lang file, GUI string calls, registration lines in `TGBlocks` and `Techguns`, creative-tab and drop logic in `BasicMachine`, and the item-block tooltips.

Assets covered:

- Textures: the 20 model and TESR textures owned here, the 18 ore drill block textures (3 of them unreferenced), the 5 unused bench and flag files, and the 8 GUI files used. See [textures.md](textures.md).
- Sounds: 29 files across 12 `sounds.json` events, plus the vanilla chest and explosion sounds. See [sounds.md](sounds.md).
- Lang: every key in [lang.md](lang.md).
- Entity sheets: 10, covering every machine, tile and block family. See [entity-sheets.md](entity-sheets.md).

Corrections to existing facts in the consistency pass: none. (An earlier correction about the ore drill clip length is recorded in [ore-drill.md](ore-drill.md#8-sounds).)

### Ownership boundaries

| Topic | Owner |
|---|---|
| Base tile classes ([`BasicInventoryTileEnt`](../08-machines-production/framework.md#2-basicinventorytileent), [`BasicOwnedTileEnt`](../08-machines-production/framework.md#3-ownership-and-security-basicownedtileent-itgtileentsecurity), [`BasicPoweredTileEnt`](../08-machines-production/framework.md#4-energy-basicpoweredtileent), [`BasicMachineTileEntity`](../08-machines-production/framework.md#7-recipe-machine-state-basicmachinetileentity), `MultiBlock*TileEntBase`), base GUIs (power bar, redstone and security buttons), [`TechgunsGuiHandler`](../08-machines-production/gui-handler.md#1-techgunsguihandler-complete-gui-id-table), the shared [`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) block, GUI texture files | **[08-machines-production](../08-machines-production/index.md)** |
| Ore cluster blocks and the drill block textures | **[07-blocks](../07-blocks/index.md)** / **[11-worldgen](../11-worldgen/index.md)** |
| Drill heads, turret armour plates, fuel and engine items | **[06-items](../06-items/index.md)** |
| Gun ammo mechanics | **[02-guns](../02-guns/index.md)** |
| Armour repair materials | **[05-armor](../05-armor/index.md)** |
| `NPCTurret` AI, the attack helicopter, NPC spawning | **[10-entities-npcs](../10-entities-npcs/index.md)** |
| Particles, [`ProjectileExplosion`](../04-projectiles-fx/explosions.md#projectileexplosion), the Biogun projectile | **[04-projectiles-fx](../04-projectiles-fx/index.md)** |
| [`GUIButtonClick`](../01-core/networking.md#0--guibuttonclick-cs) and the sync packets | **[01-core](../01-core/index.md)** |

## Known limits

- Audio and images were not heard or viewed. Sound-character notes, texture palette words and "depicts" notes are inferred from names, measurements and earlier descriptions.
- Pixel rectangles for model textures in [textures.md](textures.md) are computed from the standard box unwrap and the model parameters, not measured from the images.
- Recipes for drill heads and turret armour belong to [06](../06-items/index.md) and are linked, not restated.
- GUI region maps rely on the existing descriptions in [08](../08-machines-production/index.md)'s [gui-textures.md](../08-machines-production/gui-textures.md).
- [MUTL-48](#suspected-bugs) and the effect of the 8 kHz and 176.4 kHz sound rates are inferred from code and file headers; neither was run.
- The turret's FTBUtilities friendship rules are summarised from the code path and not tested with that mod present.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| MUTL-01 | Scrapper unreachable / unfinished | `java/techguns/blocks/machines/BasicMachine.java:203`, `java/techguns/tileentities/ScrapperTileEnt.java:57` | Meta 10 tile creation is commented out, there is no recipe and no creative entry, and `canScrap` never produces anything. The GUI, container and tile registration are all live. | gameplay |
| MUTL-02 | Scrapper tool check inverted | `java/techguns/tileentities/ScrapperTileEnt.java:71` | The tool-material branch only runs when the material name is the *empty* string. It should be "not empty". | gameplay |
| MUTL-03 | Scrapper debug spam | `java/techguns/tileentities/ScrapperTileEnt.java:67` | It prints to stdout every tick while slot 0 is occupied. | cosmetic |
| MUTL-04 | Scrapper shift-click recursion | [`java/techguns/inventory/ScrapperContainer.java:106`](scrapper.md#container-scrappercontainer) | `transferStackInSlot` defers to vanilla `Container`, which returns the slot stack unchanged. Vanilla's shift-click retry loop then recurses until the stack overflows. | crash |
| MUTL-05 | Repair bench shift-click off-by-one | [`java/techguns/inventory/RepairBenchContainer.java:67`](repair-bench.md#container-layout-repairbenchcontainer) | The player range test is `> 9`, so container slot 9 (the first main-inventory slot) cannot be shift-clicked. | gameplay |
| MUTL-06 | Camo bench back/face buttons missing | [`java/techguns/gui/GuiCamoBench.java:179`](camo-bench.md#gui-guicamobench) | The server handles ids 11–14 and the GUI shows the camo numbers, but the buttons are commented out, so face and back items cannot be re-camoed. | gameplay |
| MUTL-07 | Camo bench container never syncs | [`java/techguns/inventory/CamoBenchContainer.java:56`](camo-bench.md#container-layout-camobenchcontainer) | `addCraftingToCrafters` and `detectAndSendChanges` are empty overrides. Server-side slot changes are not pushed to the client (desync risk). | gameplay |
| MUTL-08 | Bench placement gives negative rotation | [`java/techguns/blocks/ItemBlockMetaDataMachine.java:73`](../07-blocks/special-blocks.md#16-itemblockmetadatamachine) | Signed modulo produces rotation −1 or −2 ("just don't ask why"). It renders correctly but the stored values are inconsistent. | cosmetic |
| MUTL-09 | Ore drill tick counts overflow `short` | `java/techguns/tileentities/OreDrillTileEntMaster.java:290`, `java/techguns/tileentities/BasicMachineTileEntity.java:49`, [`java/techguns/inventory/OreDrillContainer.java:87`](ore-drill.md#container-slots-oredrillcontainer) | `totalTime` and `progress` are saved as shorts and synced as window-property shorts. Slow clusters (orePerHour < 2.2, e.g. a diamond, emerald or uranium cluster on a radius-0 drill with a carbon head: 96 000 ticks) wrap after a reload and show garbage in the GUI. | gameplay |
| MUTL-10 | Missing cluster leaves slaves linked | `java/techguns/tileentities/OreDrillTileEntMaster.java:312` | `complete` is set false without `unform()`. Slaves keep their formed textures (invisible rods) while the TESR stops drawing. | visual |
| MUTL-11 | Ore drill server runs client branch | `java/techguns/tileentities/OreDrillTileEntMaster.java:122` | When `checkDrill()` fails on the server, the `else` meant for the client runs and increments progress if `haspower`. | gameplay |
| MUTL-12 | Ore drill shift-click off-by-one | `java/techguns/inventory/OreDrillContainer.java:190` | The player range is `< 10+36`, so the last hotbar slot (46) is ignored. | gameplay |
| MUTL-13 | Fuel containers go to output slots | `java/techguns/tileentities/OreDrillTileEntMaster.java:759` | Empty buckets from lava-bucket fuel are inserted into the ore output slots 1–9. | gameplay |
| MUTL-14 | Liquid ore voided when tank full | `java/techguns/tileentities/OreDrillTileEntMaster.java:165` | The output tank's fill result is ignored and the excess is lost. | gameplay |
| MUTL-15 | Drill render box too small for r 3–4 | `java/techguns/tileentities/OreDrillTileEntMaster.java:78` | The half-width is 2 only for radius 2, else 1, so large drills cull early. | visual |
| MUTL-16 | Drill TESR samples light on the wrong axis | [`java/techguns/client/renderer/tileentity/RenderOreDrill.java:95`](ore-drill.md#9-tesr-renderoredrill) (also 125, 178, 208) | It uses the inventory `rotation` (always 0 → +Y) instead of `dir` for per-segment lighting positions. | visual |
| MUTL-17 | Drill spin interpolation wrong | `java/techguns/client/renderer/tileentity/RenderOreDrill.java:65` | It adds partialTick ÷ 36 degrees instead of partialTick × 10, so the spin stutters at 10° steps. | cosmetic |
| MUTL-18 | Engine-top texture never chosen | [`java/techguns/blocks/machines/OreDrillBlock.java:232`](ore-drill.md#1-blocks) | The direction-based icon selection is marked "does not work", and the CTM path (`getManager`) ignores it. `drillEngineTop_c` is unused. | visual |
| MUTL-19 | Slave `bottomrow` never cleared | `java/techguns/blocks/machines/OreDrillBlock.java:770` | It is only ever set true, so a block that was in a previous last slice keeps hidden faces after re-forming in another role (until reload). | visual |
| MUTL-20 | Controller message key unprefixed | [`java/techguns/blocks/ItemBlockOreDrill.java:137`](../07-blocks/special-blocks.md#17-itemblockoredrill-placement-rules) | `I18n` looks up `msg.error.oredrill.controllernext`, but the lang key is `techguns.msg.…`, so the raw key is shown. | cosmetic |
| MUTL-21 | Ore drill blocks claim to be normal cubes | `java/techguns/blocks/machines/OreDrillBlock.java:884` | `isNormalCube` is true for the see-through scaffold and invisible formed rods, which affects lighting and suffocation. | visual |
| MUTL-22 | Drill tooltip divides by zero | [`java/techguns/gui/GuiOreDrill.java:58`](ore-drill.md#7-gui-guioredrill-id-16) | The "%" and "Ores/h" lines divide by `totalTime`, which can be 0 (NaN/Infinity). | cosmetic |
| MUTL-23 | Legacy ore drill still registered | `java/techguns/tileentities/OreDrillTileEnt.java:21`, [`java/techguns/Techguns.java:576`](../01-core/lifecycle-and-registration.md#lifecycle-overview) | A dead prototype tile (hard-coded iron ore, 1000 ÷ size timer) is registered; [`OreDrill`](entity-sheets.md#10-legacy-oredrill--oredrilltileent-dead-code) block is unused. | cosmetic |
| MUTL-24 | Turret reload drops empty stacks | `java/techguns/tileentities/TurretBaseEnt.java:196` | An item entity with the overflow count is spawned even when the overflow is 0. | cosmetic |
| MUTL-25 | Turret links an arbitrary entity by stale id | `java/techguns/tileentities/TurretBaseEnt.java:89` | `mountedTurret` stores an entity id. On read (for example placing a wrenched turret item) whatever entity has that id is cast to `NPCTurret`, which throws ClassCastException if it is a different type. | crash |
| MUTL-26 | Turret power synced every tick | [`java/techguns/inventory/TurretBaseContainer.java:106`](turret.md#container-turretbasecontainer) | A window property is sent each tick unconditionally. | cosmetic |
| MUTL-27 | Client class referenced in common tile | `java/techguns/tileentities/TurretBaseEnt.java:248` | `Minecraft.getSystemTime()` is in common code. It is guarded by `isRemote`, but is a dedicated-server class-loading risk. | crash |
| MUTL-28 | Turret sound events unused | `assets/techguns/sounds.json` (`npcs.turretBeep`, `npcs.turretGunequip`) | Defined with 10 files, but nothing plays them. | cosmetic |
| MUTL-29 | Dead [`TurretBase`](../07-blocks/block-catalogue.md#blocks-constructed-but-not-registered-not-in-game) block | `java/techguns/blocks/TurretBase.java:43` | An unregistered block that would spawn [`TurretEntityUNUSED`](../10-entities-npcs/per-mob.md#19-turretentityunused-dead-code). Dead code. | cosmetic |
| MUTL-30 | Charge break event with null player | `java/techguns/tileentities/ExplosiveChargeTileEnt.java:150` | `BlockEvent.BreakEvent` is built with the owner looked up by UUID. That is null when the owner is offline, in another dimension, or the charge is unowned, which is likely an NPE inside Forge's event/harvest checks. The fuse and explosion also run on the client, which posts the event there too. | crash |
| MUTL-31 | Charge settings lack permission check | `java/techguns/tileentities/ExplosiveChargeTileEnt.java:283` | Button ids 1–6 change radius, length and fuse without `isUseableByPlayer`, so any client packet can edit any unarmed charge. | gameplay |
| MUTL-32 | Charge GUI not localised | [`java/techguns/gui/GuiExplosiveCharge.java:50`](../08-machines-production/gui-textures.md#10-texturesguiexpchargeguipng-owned-by-09) | "Radius:", "Length:", "Delay:" and "Detonate" are literals. | cosmetic |
| MUTL-33 | TNT radius buttons are no-ops | `java/techguns/tileentities/ExplosiveChargeTileEnt.java:24` | Min = max = 3 for the TNT charge, but +/− radius buttons are still shown. | cosmetic |
| MUTL-34 | Chest viewer recount resets while open | `java/techguns/tileentities/TGChestTileEnt.java:343` | The recount only counts `ContainerChest` viewers. [`TGChestContainer`](misc-tile-entities.md#container-tgchestcontainer) is not one, so the server sets `numPlayersUsing` to 0 every 200 ticks (weapon-chest redstone drops; closing can go negative). | gameplay |
| MUTL-35 | Weapon chest acts as a trapped chest | [`java/techguns/blocks/BlockTGChest.java:613`](../07-blocks/special-blocks.md#9-reinforced-chest-and-weapon-chest-tgchest-tgchest_weapon-blocktgchest) | `canProvidePower` is true for type 1 (copied from the vanilla trapped-chest type flag), so opening a weapon chest emits redstone. | gameplay |
| MUTL-36 | Chest shift-click fills TG slots | `java/techguns/inventory/TGChestContainer.java:84` | Merging from the chest targets all later slots in reverse, so the TG ammo and auto-food slots are filled first, with no item-validity check. | gameplay |
| MUTL-37 | Chest item renderer debug offset | [`java/techguns/client/renderer/item/RenderBlockModelItemChest.java:27`](../07-blocks/block-rendering-and-models.md#4-block-item-renderers) | The equipped transform adds `Keybinds.X/Y/Z` debug offsets. | visual |
| MUTL-38 | Camp flag cap lost on reload | `java/techguns/tileentities/CampFlagTileEnt.java:144` | `maxNPCs` is recomputed from size on load. The carrier's explicit 9 becomes 28. | gameplay |
| MUTL-39 | Uninitialised camp flag crashes | `java/techguns/tileentities/CampFlagTileEnt.java:123` (also 161) | A flag placed without `init` (pick-block / give `basicMachine:7`) has a null NPC list and spawn array. Counting NPCs throws an NPE, and saving writes a null int array. | crash |
| MUTL-40 | Flag item renders as repair bench | [`java/techguns/client/ClientProxy.java:607`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) | The item-renderer slot for meta 7 uses the repair-bench cube and texture. | visual |
| MUTL-41 | Bio Blob poison skips players | [`java/techguns/blocks/Bioblob.java:147`](../07-blocks/special-blocks.md#13-bio-blob-bioblobsmall-bioblob) | It casts to `EntityLiving` and swallows ClassCastException, so players (`EntityLivingBase` only) are never poisoned. | gameplay |
| MUTL-42 | Bio Blob collision box not per-state | `java/techguns/blocks/Bioblob.java:116` | Bounds are only set for selection and render. The collision box uses whatever bounds were last set on the shared block instance. | gameplay |
| MUTL-43 | Bio Blob name key mismatch | [`java/techguns/TGBlocks.java:220`](../07-blocks/block-catalogue.md#registered-blocks) | Registered with a plain `ItemBlock` (key `tile.Bioblob.name`), but the lang defines `techguns.tile.Bioblob.name`. | cosmetic |
| MUTL-44 | Bio Blob decays client-side too | `java/techguns/tileentities/BioblobTileEnt.java:125` | The decay timer and removal run on both sides, so the client can delete or shrink the blob independently. | visual |
| MUTL-45 | Bio Blob `side` field dead | `java/techguns/tileentities/BioblobTileEnt.java:25` | It is saved and synced, but never set. The side lives in block metadata instead. | cosmetic |
| MUTL-46 | Ore drill work sound overlaps itself | `java/techguns/tileentities/OreDrillTileEntMaster.java` (client sound delay of 61 ticks) | The loop is re-triggered every 62 powered ticks (about 3.1 s), but every clip is 4.01 to 9.71 s long, so plays overlap and stack. | cosmetic |
| MUTL-47 | Ore drill sound files have odd sample rates | `assets/techguns/sounds/machines/oredrill_s_work_a.ogg` to `_d`, `oredrill_l_work_b.ogg` to `_d` | Small-drill files declare 176 400 Hz and three large-drill files 8 000 Hz, while their siblings are 44 100 or 48 000 Hz. The 8 kHz files will be low-fidelity. | cosmetic |
| MUTL-48 | Flag drops an iron ingot with damage 7 | `java/techguns/blocks/machines/BasicMachine.java:95` and `:237` | `getItemDropped` swaps meta 7 for `iron_ingot`, but `damageDropped` still returns the metadata (7). The dropped ingot probably has damage 7 and may not stack with, or match recipes for, a normal ingot. Inferred from the code; not tested. | gameplay |
