# Entity sheets (area 09)

One sheet per machine, tile entity and block family in this area, using the ten entity-sheet fields from [CONVENTIONS.md](../CONVENTIONS.md). The sheets restate and cross-link what the per-machine files describe in depth ([repair-bench.md](repair-bench.md), [camo-bench.md](camo-bench.md), [scrapper.md](scrapper.md), [ore-drill.md](ore-drill.md), [turret.md](turret.md), [explosive-charges.md](explosive-charges.md), [misc-tile-entities.md](misc-tile-entities.md), [shared-rendering.md](shared-rendering.md)). They are in addition to those files, not a replacement.

Facts common to all [`basicMachine`](../07-blocks/block-catalogue.md#registered-blocks) metas (registry `basicMachine`, item block [`ItemBlockMetaDataMachine`](../07-blocks/special-blocks.md#16-itemblockmetadatamachine)):

- Creative tab: Techguns (`tabTechgun`). Metas 0–9 are listed except 7 (the camp flag), so 6, 8 and the other area-08 metas appear; meta 10 is never listed.
- Block drop: the block item with `damageDropped` = the meta, except meta 7 (see [MUTL-48](index.md#suspected-bugs)).
- Material iron, hardness 4.0, resistance 20, metal step sound, pickaxe harvest level 1 (metas 0–5 and 7; the charges override hardness to 0.1).
- Oredict names: none for any block in this area.
- Machine item tooltip (all metas): when the stack has NBT `TileEntityData`, one extra line `techguns.block.hasTileEntityData` = "Has TileEntity Data". No other tooltip lines.
- Measured colours for every texture named below are in [textures.md](textures.md); every lang key is in [lang.md](lang.md); every sound is in [sounds.md](sounds.md).

---

## 1. Repair Bench (`basicMachine` meta 5)

| # | Field | Content |
|---|---|---|
| 1 | Identity | Block `basicMachine:5`; tile [`RepairBenchTileEnt`](index.md#key-facts) (registered id `RepairBenchTileEnt`); unlocalized `techguns.tile.BasicMachine_5.name` = "Repair Bench"; container title key `techguns.container.repairbench` = "Repair Bench"; GUI id 5; creative tab Techguns. |
| 2 | Obtaining | Shaped, 1 output: `pmp` / `ici` / `iii` with p = [`plateIron`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo), m = Mechanical Parts ([06](../06-items/index.md)), c = crafting table, i = [`nuggetIron`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems). See [shared-rendering.md](shared-rendering.md#recipes-for-area-09-blocks-shaped-ore-dictionary-aware-defined-in-tgitems) and [06-items](../06-items/index.md). |
| 3 | Numbers | 9 slots, stack limit 64, no energy, no tick logic, use distance: within 8 blocks (distance squared at most 64). No config key. |
| 4 | Behaviour | Server, buttons 1–4 repair the worn helmet, chestplate, leggings, boots from bench stock, all-or-nothing; no feedback. Full logic in [repair-bench.md](repair-bench.md#repair-action-buttonclicked-server). Automation: every side shows slots 0–8, insert only. |
| 5 | Persistence and sync | NBT `Items`, `rotation`, optional `CustomName`. GUI sync: none (plain container; the tooltip reads the client's own armour and the bench's synced slots). |
| 6 | UI | 176 × 166 GUI; slots (8 + 18·i, 57), armour mirror slots (89 + 20·i, 18), four 14 × 14 repair buttons at (90 + 20·i, 39). Tooltips: "No armor", "Can't repair this armor", "Armor is fully repaired", "Required materials for repair:" followed by "Nx item name" lines (red when short). Title `techguns.container.repairbench`; caption "Inventory" at (8, 74). |
| 7 | Visuals | TESR [`RenderTileEntityBlock`](../08-machines-production/rendering.md#4-rendertileentityblock-used-by-09s-camo-and-repair-benches) with [`RepairBenchTesselatorCube`](../07-blocks/textures.md#camo-bench-and-repair-bench-09), one cube from [`textures/blocks/repairbench_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) (64 × 64; front = top-right quadrant, top = bottom-left, other faces = top-left). Item: [`RenderBlockModelItem`](shared-rendering.md#item-rendering-inventory-and-hand) + the same cube. Faces in [textures.md](textures.md#31-repair-bench-and-camo-bench). |
| 8 | Audio | none. Step and break: metal. |
| 9 | Relationships | Reads armour repair lists from [`GenericArmor`](../05-armor/armor-catalogue.md#1-class-hierarchy) ([05-armor](../05-armor/index.md)). Sends [`GUIButtonClick`](../01-core/networking.md#0--guibuttonclick) ([01-core](../01-core/networking.md#0--guibuttonclick-cs)). GUI texture owned by [08](../08-machines-production/gui-textures.md#8-texturesguirepairbench_guipng-owned-by-09). |
| 10 | Bugs | [MUTL-05](index.md#suspected-bugs) (shift-click off by one), [MUTL-08](index.md#suspected-bugs) (negative rotation). |

## 2. Camo Bench (`basicMachine` meta 3)

| # | Field | Content |
|---|---|---|
| 1 | Identity | Block `basicMachine:3`; tile [`CamoBenchTileEnt`](index.md#key-facts) (id `CamoBenchTileEnt`); `techguns.tile.BasicMachine_3.name` = "Camo Bench"; container title `techguns.container.camobench` = "Camo Workbench"; GUI id 3; creative tab Techguns. |
| 2 | Obtaining | Shaped: `ddd` / `ici` / `iii` with d = any `dye`, c = crafting table, i = `nuggetIron`. |
| 3 | Numbers | 1 slot (index 0). No energy. Use distance 8 blocks. No config key. |
| 4 | Behaviour | Server buttons 1/2 step the bench item's camo forward or back; 3–10 step the worn armour pieces (two buttons per piece); 11–14 (back and face slots) have a server handler but no GUI button ([MUTL-06](index.md#suspected-bugs)). Table in [camo-bench.md](camo-bench.md#button-handling-buttonclicked-server-only-if-isuseablebyplayer-same-tile-and-within-8-blocks). Automation: none. |
| 5 | Persistence and sync | NBT `Items`, `rotation`, `CustomName`. After buttons 1 and 2 the tile does `needUpdate`; for worn armour a set-slot packet is sent. The container never syncs on its own ([MUTL-07](index.md#suspected-bugs)). |
| 6 | UI | 176 × 166 GUI; bench slot (21, 18); armour mirror slots (96 + 19·i, 18); back slot (75, 18); face slot (56, 18). Buttons: `>` (26,61) and `<` (10,61), 16 × 16; `+`/`-` pairs 10 × 10 at x = 99, 118, 137, 156 (y 59 and 69). Text: "(n/total):" at (10,40); camo lang key text at (10,50), or "No Camo" (`techguns.item.invalidcamo`); camo numbers at (101 + 19·i, 38). Live player preview at (left − 20, top + 77). |
| 7 | Visuals | TESR `RenderTileEntityBlock` with [`PaintMachineTesselatorCube`](../07-blocks/textures.md#camo-bench-and-repair-bench-09); [`textures/blocks/paintmachine_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) (64 × 64). Unused older faces `paintmachine_top/front/side/bottom.png` (32 × 32). |
| 8 | Audio | none. |
| 9 | Relationships | [`ICamoChangeable`](../05-armor/armor-catalogue.md#1-class-hierarchy) items from [02-guns](../02-guns/index.md), [05-armor](../05-armor/index.md), [06-items](../06-items/index.md), [07-blocks](../07-blocks/special-blocks.md#18-camo-changeable-item-blocks-genericitemblockcamochangeable). GUI texture: [08](../08-machines-production/gui-textures.md#7-texturesguicamobench_guipng-owned-by-09). |
| 10 | Bugs | MUTL-06, MUTL-07, MUTL-08. |

## 3. Scrapper (`basicMachine` meta 10, never created)

| # | Field | Content |
|---|---|---|
| 1 | Identity | Tile [`ScrapperTileEnt`](index.md#key-facts) (id `ScrapperTileEnt`); no block name key (`techguns.tile.BasicMachine_10.name` is absent); container title key `techguns.container.scrapper` (missing from the lang file); GUI id 10. |
| 2 | Obtaining | *Unobtainable*: no recipe, no creative entry, tile creation commented out ([MUTL-01](index.md#suspected-bugs)). |
| 3 | Numbers | 10 slots (0 input, 1–9 output); 10 000 RF buffer; scrap table: none. |
| 4 | Behaviour | Tick does nothing but print debug text for slot 0. Details in [scrapper.md](scrapper.md#tile-entity-as-written). |
| 5 | Persistence and sync | NBT as [`BasicMachineTileEntity`](../08-machines-production/framework.md#7-recipe-machine-state-basicmachinetileentity). Window properties 0 progress, 1 totalTime, 2 power. |
| 6 | UI | Background `ammoPress_GUI.png`; input slot (35,17); output 3 × 3 at (116 + 18·col, 17 + 18·row). No tooltips of its own. |
| 7 | Visuals | none of its own; the item-renderer slot for meta 10 draws the repair-bench cube. |
| 8 | Audio | none. |
| 9 | Relationships | Shares the ammo-press GUI texture ([08](../08-machines-production/gui-textures.md#1-texturesguiammopress_guipng)). |
| 10 | Bugs | MUTL-01, [MUTL-02](index.md#suspected-bugs), [MUTL-03](index.md#suspected-bugs), [MUTL-04](index.md#suspected-bugs). |

## 4. Ore Drill block family (`oreDrill` metas 0–4)

Shared identity: registry [`oreDrill`](../07-blocks/block-catalogue.md#registered-blocks), class `OreDrillBlock`, item block [`ItemBlockOreDrill`](../07-blocks/special-blocks.md#17-itemblockoredrill-placement-rules) (name key `techguns.tile.oreDrill_<meta>.name`), creative tab Techguns (variations with block meta at most 4), material iron, hardness 8, resistance 20, metal step, pickaxe level 1, drops itself with the same meta, no oredict names, no item tooltip. Formed variants use meta + 8 only for icon lookup; the stored meta never changes.

| Meta | Name and key | Obtaining | Tile | Numbers and behaviour | Textures |
|---|---|---|---|---|---|
| 0 | Ore Drill Frame (`techguns.tile.oreDrill_0.name`) | ×8: `pbp` / `bpb` / `pbp`, p = `plateIron`, b = iron bars | `OreDrillTileEntSlave` | Heavy frame: slice corners, first and last slice rings. No placement rule. | `drillFrame_outer`; formed `drillFrame_outer_c` |
| 1 | Ore Drill Scaffold (`_1`) | ×8: `bbb` / `bpb` / `bbb` | `OreDrillTileEntSlave` | Light frame: ring edges of middle slices and fill of the last slice. | `drillFrame`; formed `drillFrame_c` |
| 2 | Ore Drill Rod (`_2`) | ×4: `ipi` ×3 rows, i = `ingotIron`, p = `plateIron` | `OreDrillTileEntSlave` | Centre-line rod; placement needs an adjacent cluster (one rod per cluster) or an adjacent rod, error `techguns.msg.error.alreadyrod`. | `drillRod-ctmv` + `drillRod-top`; formed `invisible` |
| 3 | Ore Drill Engine (`_3`) | ×4: `bbb` / `beb` / `bbb`, b = iron bars, e = Electric Engine ([06](../06-items/index.md)) | `OreDrillTileEntSlave` | Engine: centre line next to the controller, interior of engine slices. | `drillEngine`; formed `drillEngine_c` |
| 4 | Ore Drill Controller (`_4`) | ×1: `pcp` / `cec` / `pcp`, p = `plateIron`, c = `circuitBasic`, e = Electric Engine | [`OreDrillTileEntMaster`](ore-drill.md#5-operation-oredrilltileentmaster) | All logic. Cannot touch another controller (`msg.error.oredrill.controllernext`, [MUTL-20](index.md#suspected-bugs)). Owner set on placement. | `drillEngineController` (never remapped) |

Master and slave sheet (fields 3–10):

| # | Field | Content |
|---|---|---|
| 3 | Numbers | Master: 11 slots, 500 000 RF, use range squared 128, input tank 8 000 mB, output tank 32 000 mB, fuel buffer. Formulas, size classes and defaults for [`oreDrillMultiplierOre`](../01-core/config.md#category-ore-drills) (1.0), [`oreDrillMultiplierPower`](../01-core/config.md#category-ore-drills) (1.0), [`oreDrillFuelMultiplier`](../01-core/config.md#category-ore-drills) (1000), [`oreDrillFuelValueFuel`](../01-core/config.md#category-ore-drills) (96), [`oreDrillFuelValueBioFuel`](../01-core/config.md#category-ore-drills) (48), [`MachinesNeedNoPower`](../01-core/config.md#category-general) (false) in [ore-drill.md](ore-drill.md#53-starting-an-operation-checkandstartoperation). |
| 4 | Behaviour | Formation by right-clicking the controller, linking, operation and fuel in [ore-drill.md](ore-drill.md#3-formation-right-clicking-the-controller). Automation: only the master exposes RF, fluid and slots 1–10. |
| 5 | Persistence and sync | Master NBT `complete`, `dir`, `radius`, `enginesize`, `rodsize`, `haspower`, `currentRFperTick`, `hasDrill`, `drillType`, `fuelBuffer`, `fuelBufferMax`, `inputTank`, `outputTank`, `currentLiquidOp`, plus the base keys. Slave NBT `hasMaster`, `masterX/Y/Z`, `bottomrow`, `direction`. GUI window properties 0–8 listed in [ore-drill.md](ore-drill.md#progress-bar-sync). |
| 6 | UI | GUI id 16, `oreDrill_GUI.png`; slots head (30,17), fuel (30,51), outputs 3 × 3 at (102, 17); tooltips: "RF/t: n", "p%", "Ores/h: n" (hard-coded English), tank tooltips (fluid name or "Empty", "amount/capacityMB"), fuel tooltip "Fuel n/m" and "Remaining time: N ticks." or "No Fuel"; red "Multiblock broken" / "or invalid Drill" when incomplete. |
| 7 | Visuals | CTM block icons above; [`RenderOreDrill`](ore-drill.md#9-tesr-renderoredrill) TESR draws a spinning auger from `drillhead.png`, `drillheadObsidian.png` or `drillheadCarbon.png` (32 × 32) selected by `drillType`; bare rods use `drillhead.png`. Geometry in [ore-drill.md](ore-drill.md#9-tesr-renderoredrill). |
| 8 | Audio | Client-side work loop events `machines.oredrillSmallWork` (1.5), `Medium` (2.25), `Large` (3.0), pitch 1.0, every 62 powered ticks. See [sounds.md](sounds.md#1-ore-drill-work-loop). |
| 9 | Relationships | Drill heads are [06](../06-items/index.md) items (`DRILL_SMALL/MEDIUM/LARGE` slot types); cluster blocks [07](../07-blocks/index.md)/[11](../11-worldgen/index.md); fluid ids [06](../06-items/index.md)/[08](../08-machines-production/index.md); sync base classes 08. |
| 10 | Bugs | [MUTL-09](index.md#suspected-bugs) to [MUTL-23](index.md#suspected-bugs), [MUTL-46](index.md#suspected-bugs), [MUTL-47](index.md#suspected-bugs). |

### Drill heads (06-owned items used here)

Nine items: `oreDrill{Small,Medium,Large}{Steel,ObsidianSteel,Carbon}` with English names such as "Small Ore Drill Head (Steel)" (keys `techguns.item.oreDrill<Size><Material>.name`). Obtaining: crafting, see [06-items](../06-items/recipes.md); behaviour here: head mining level 1 / 2 / 3 by material, `drillType` equals the level. The item tooltips (keys `techguns.oreDrillTooltip.size` "Valid for drills with size" and `techguns.oreDrillTooltip.mininglevel` "Bonus mining level") are [06](../06-items/index.md)'s.

## 5. Turret base (`basicMachine` meta 2)

| # | Field | Content |
|---|---|---|
| 1 | Identity | Block `basicMachine:2`; tile [`TurretBaseEnt`](turret.md#tile-entity-turretbaseent) (id `TurretBaseEnt`); `techguns.tile.BasicMachine_2.name` = "Turret"; container `techguns.container.turretbase` = "Turret Platform"; GUI id 2; mounted entity [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) (registered [`SimpleTurret`](../10-entities-npcs/mob-catalogue.md#registration-javatechgunstgentitiesjava), `entity.Techguns.SimpleTurret.name` = "Turret"). |
| 2 | Obtaining | Shaped ×1: `pip` / `cec` / `prp`, p = `plateIron`, i = `circuitBasic`, c = concrete block meta 0 ([07](../07-blocks/index.md)), e = Electric Engine ([06](../06-items/index.md)), r = redstone. Armour plates in slot 19 are [06](../06-items/index.md) items ([`turretArmorIron`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo), `…Steel`, `…ObsidianSteel`, `…Carbon`; English "Iron Turret Armor", "Steel Turret Armor", "Obsidian Steel Turret Armor", "Carbon Turret Armor"). |
| 3 | Numbers | 20 slots (0–8 ammo, 9–17 empty magazines, 18 gun, 19 armour); 1 000 RF buffer; upkeep 5 RF/t; heal 500 RF per 1 HP every 60 ticks; rebuild 200 ticks at 50 RF per tick of progress, new turret at 10 HP; armour 0 / 5 / 10 / 15 / 20; turret max health 30. No config key. |
| 4 | Behaviour | Per-tick power, healing, respawn, ammo handling, re-linking, targeting rules and wrench dismantling in [turret.md](turret.md#per-tick-logic-both-sides-power-consumption-is-authoritative-on-the-server). |
| 5 | Persistence and sync | NBT base keys plus `mountedTurret`, `turretDeath`, `repairTime`, `turretHealTime`, `attackAnimals`. Window properties 0 `repairTime`, 1 `turretDeath`, 2 power. Packets [`RequestTileEntitySync`](../01-core/networking.md#2--requesttileentitysync) and [`PacketRequestTurretSync`](../01-core/networking.md#12--packetrequestturretsync). |
| 6 | UI | `turretBase_GUI.png`; ammo 3 × 3 at (18, 17); magazines 3 × 3 at (116, 17); gun (85, 19); armour (85, 42); repair bar rect (115, 73, 53 × 5); button id 3 at (−22, 70) 20 × 20. Tooltips: "Repair time left Ns" (dead only), "Target Animals: Yes/No" (Yes red, No green, as the code colours them). Item tooltip: "Has TileEntity Data" when wrenched. |
| 7 | Visuals | [`RenderBlockTileEntModel`](../08-machines-production/rendering.md#3-renderblocktileentmodel-used-by-09s-turret-base-and-flag) + [`ModelTurretBase`](../07-blocks/block-rendering-and-models.md#modelturretbase-uv-6464-texture-turretbasepng); texture [`TurretBase.png`](../07-blocks/block-catalogue.md#blocks-constructed-but-not-registered-not-in-game) or `_2` to `_5` by armour (128 × 128). Box-by-box pixel rectangles in [textures.md](textures.md#32-turret-base). |
| 8 | Audio | Base plays none; `npcs.turretBeep` and `npcs.turretGunequip` are defined but never played ([MUTL-28](index.md#suspected-bugs)). Turret hurt `mob.irongolem.hit`, death `mob.irongolem.death`. |
| 9 | Relationships | `NPCTurret` AI and faction in [10-entities-npcs](../10-entities-npcs/ai.md#5-turret-npcturret); guns [02-guns](../02-guns/index.md); ammo items 06. |
| 10 | Bugs | [MUTL-24](index.md#suspected-bugs), [MUTL-25](index.md#suspected-bugs), [MUTL-26](index.md#suspected-bugs), [MUTL-27](index.md#suspected-bugs), MUTL-28, [MUTL-29](index.md#suspected-bugs). |

## 6. Explosive charges (`basicMachine` metas 6 and 8)

| # | Field | TNT charge (meta 6) | Advanced charge (meta 8) |
|---|---|---|---|
| 1 | Identity | Tile [`ExplosiveChargeTileEnt`](explosive-charges.md#nbt-explosivechargetileent); "Explosive Charge" (`techguns.tile.BasicMachine_6.name`); GUI title key `techguns.inventory.explosivecharge`; GUI id 6 | Tile [`ExplosiveChargeAdvTileEnt`](index.md#key-facts) (subclass); "Advanced Explosive Charge" (`…_8.name`); same GUI id 6 |
| 2 | Obtaining | ×8: `rtr` / `tct` / `rtr`, r = `itemRubber`, t = TNT, c = `circuitBasic` | ×4: same grid, r = `sheetPlastic`, t = TGX explosive item ([06](../06-items/index.md)), c = `circuitBasic` |
| 3 | Numbers | radius 3 fixed; length 2 / 3 / 5; fuse 60 / 100 / 200 ticks; hardness limit below [`ExplosiveChargeMaxHardness`](../01-core/config.md#category-general) (30.0) | radius 3 / 5 / 7; length 2 / 10 / 10; fuse 60 / 100 / 200; hardness at most [`ExplosiveChargeAdvancedMaxHardness`](../01-core/config.md#category-general) (100.0) |
| 4 | Behaviour | Plate placed on a face; arm with button 0; column-by-column tunnel when the fuse reaches 0; items drop; no entity damage. See [explosive-charges.md](explosive-charges.md#explosion-algorithm). No automation, wrench ignored, right-click ignored once armed. |
| 5 | Persistence and sync | NBT `orientation`, `blastradius`, `blastlength`, `fusetime`, `armed` plus `rotation`, `CustomName`, `security`, `owner`. No window properties (container has no slots). |
| 6 | UI | 176 × 166. Labels (hard-coded English, [MUTL-32](index.md#suspected-bugs)) "Radius: N" (37,40), "Length: N" (37,50), "Delay: Ns" (37,60); "Detonate" button (57,73) 60 × 20; `+`/`-` 10 × 10 pairs at x 120 / 130, y 40 / 50 / 60. Title is the translated `techguns.inventory.explosivecharge`. Item tooltip: "Has TileEntity Data" if present. |
| 7 | Visuals | [`RenderExplosiveCharge`](explosive-charges.md#world-rendering-renderexplosivecharge); [`ModelTNTCharge`](../07-blocks/block-rendering-and-models.md#modeltntcharge-uv-3232-textures-tntchargepng--tntcharge_2png) with `tntCharge.png` / `tntCharge_2.png` (armed) | [`ModelExpCharge`](../07-blocks/block-rendering-and-models.md#modelexpcharge-uv-3232-textures-expchargepng--expcharge_2png) with `ExpCharge.png` / `ExpCharge_2.png` (armed); all 64 × 64. Item renderer [`RenderBlockModelItemTNTCharge`](shared-rendering.md#item-rendering-inventory-and-hand). Pixel rectangles in [textures.md](textures.md#33-explosive-charges). GUI textures `tntChargeGui.png` / `expChargeGui.png` (256 × 256). |
| 8 | Audio | `tntcharge.plant` / `.init` / `.tick` (all 4.0 / 1.0), plus vanilla `random.explode` | `c4charge.plant` / `.init` / `.tick` (all 4.0 / 1.0), plus vanilla `random.explode`. Durations and rates in [sounds.md](sounds.md#2-explosive-charges). |
| 9 | Relationships | Forge `BlockEvent.BreakEvent`; owner lookup by UUID; particles from [04](../04-projectiles-fx/index.md). | same |
| 10 | Bugs | [MUTL-30](index.md#suspected-bugs), [MUTL-31](index.md#suspected-bugs), MUTL-32, [MUTL-33](index.md#suspected-bugs) | MUTL-30, MUTL-31, MUTL-32 |

## 7. Reinforced Chest and Weapon Chest

| # | Field | Reinforced Chest ([`tgchest`](../07-blocks/block-catalogue.md#registered-blocks)) | Weapon Chest ([`tgchest_weapon`](../07-blocks/block-catalogue.md#registered-blocks)) |
|---|---|---|---|
| 1 | Identity | Block `BlockTGChest(0)`, item block [`ItemBlockTGChest`](../07-blocks/block-catalogue.md#conventions-and-defaults) (unlocalized name prefixed `techguns.`); `techguns.tile.tgchest.name` = "Reinforced Chest"; tile [`TGChestTileEnt`](misc-tile-entities.md#tile-entity-tgchesttileent); GUI id 20; creative tab Techguns | `BlockTGChest(1)`; `techguns.tile.tgchest_weapon.name` = "Weapon Chest"; same tile and GUI |
| 2 | Obtaining | `n n` / ` c ` / `n n`, n = `nuggetIron`, c = vanilla chest | `pcp`, p = `plateIron`, c = vanilla chest |
| 3 | Numbers | hardness 5, resistance 500, wood step; 36 slots single (3 × 12), 72 double (6 × 12); lid speed 0.1 per tick | same; additionally redstone output 0–15 by viewer count ([MUTL-35](index.md#suspected-bugs)) |
| 4 | Behaviour | Vanilla chest rules (join one same-type neighbour, blocked by a solid block above or a sitting ocelot); sneak + right-click with a wrench picks it up with contents; all sides open to automation; comparator output. Security code is commented out, so chests are always public. | same, trapped-chest-like power |
| 5 | Persistence and sync | NBT `Items`, `rotation`, `CustomName`; item NBT `TileEntityData` on wrench pickup. Viewer count by block event id 1. | same |
| 6 | UI | GUI 230 wide, height 114 + 18·rows (168 single, 222 double); 12 columns; extra auto-food slots (3) and ammo slots (6) of the Techguns player inventory at x = 172. Titles: "Reinforced Chest" / "Large Reinforced Chest". Item tooltip "Has contents" (`techguns.block.chestHasContents`) when `TileEntityData` is present. | titles "Weapon Chest" / "Large Weapon Chest" |
| 7 | Visuals | [`RenderTGChest`](misc-tile-entities.md#1-reinforced-chest-and-weapon-chest) TESR (vanilla chest models) with `reinforced_chest.png` (128²) / `reinforced_chest_double.png` (256 × 128); item `RenderBlockModelItemChest` + [`ModelChestInv`](../07-blocks/block-rendering-and-models.md#modelchestinv-uv-6464-textures-reinforced_chestpng--weapon_chestpng) with the single texture. | `weapon_chest.png` / `weapon_chest_double.png` |
| 8 | Audio | vanilla `random.chestopen` and `random.chestclosed`, volume 0.5, pitch 0.9–1.0 (both types). Step and break: wood. | same |
| 9 | Relationships | Techguns player inventory slots ([05-armor](../05-armor/extra-inventory.md)); [`GuiTechgunsChest`](misc-tile-entities.md#gui-guitechgunschest) GUI texture [`tgchest_gui.png`](../08-machines-production/gui-textures.md#13-texturesguitgchest_guipng-owned-by-09); structure loot (11). | same |
| 10 | Bugs | [MUTL-34](index.md#suspected-bugs), [MUTL-36](index.md#suspected-bugs), [MUTL-37](index.md#suspected-bugs) | MUTL-34, MUTL-35, MUTL-36, MUTL-37 |

## 8. Camp Flag (`basicMachine` meta 7)

| # | Field | Content |
|---|---|---|
| 1 | Identity | Tile [`CampFlagTileEnt`](index.md#key-facts) (id `CampFlagTileEnt`); `techguns.tile.BasicMachine_7.name` = "Flag"; no GUI; inventory name `container.campflag` (missing key, never shown); not in the creative list. |
| 2 | Obtaining | *Unobtainable* by players: placed by world generation ([11-worldgen](../11-worldgen/structure-catalogue.md#a6-military-base-militarybasestructure--militarycamp)); `/give basicMachine:7` yields an uninitialised flag ([MUTL-39](index.md#suspected-bugs)). |
| 3 | Numbers | Check interval 60 ticks, spawn interval 200 ticks, margin 4, camp height 20, counting box widened by 8 horizontally; default cap int(((sizeX + sizeZ) / 2) × 0.75); carrier explicit cap 9. Breaking drops 1 iron ingot. |
| 4 | Behaviour | Server: count camp NPCs, spawn one every 10 s while below the cap; on break spawns an [`AttackHelicopter`](../10-entities-npcs/per-mob.md#16-attack-helicopter-attackhelicopter-boss) 50 blocks above. No right-click action, wrench ignored. [misc-tile-entities.md](misc-tile-entities.md#2-camp-flag). |
| 5 | Persistence and sync | NBT `sizeX`, `sizeZ`, `xoffset`, `yoffset`, `zoffset` (shorts), `spawnPositions` (int array), `npcListSize`, `npcName0..N`, `Items`, `rotation`. `maxNPCs` not saved ([MUTL-38](index.md#suspected-bugs)). |
| 6 | UI | none (no GUI, no tooltip beyond the generic one). |
| 7 | Visuals | `RenderBlockTileEntModel` + [`ModelFlagPost`](../07-blocks/block-rendering-and-models.md#modelflagpost-uv-6464-texture-flagpostpng) + `flagpost.png` (128 × 128), pole 4 blocks tall, waving flag; item slot wrongly uses the repair-bench cube ([MUTL-40](index.md#suspected-bugs)). Unused `flagBlock.png`. Pixel rectangles in [textures.md](textures.md#34-camp-flag). |
| 8 | Audio | none. |
| 9 | Relationships | Spawns NPCs through [`NPCUtil.spawnEntity`](../10-entities-npcs/spawning.md#4-camp-flags-and-npcutil) ([10](../10-entities-npcs/spawning.md#4-camp-flags-and-npcutil)); placed by [11-worldgen](../11-worldgen/index.md). |
| 10 | Bugs | MUTL-38, MUTL-39, MUTL-40, MUTL-48. |

## 9. Bio Blob (`BioBlobSmall`)

| # | Field | Content |
|---|---|---|
| 1 | Identity | Block class [`Bioblob`](../07-blocks/special-blocks.md#13-bio-blob-bioblobsmall-bioblob), registry [`BioBlobSmall`](../07-blocks/block-catalogue.md#registered-blocks), unlocalized `Bioblob`; tile [`BioblobTileEnt`](misc-tile-entities.md#tile-entity-bioblobtileent) (id `BioblobTileEnt`); lang `techguns.tile.Bioblob.name` = "Bio Blob" but the plain `ItemBlock` asks for `tile.Bioblob.name`, so the key is never found ([MUTL-43](index.md#suspected-bugs)); metadata 0–5 = the side it sticks to; no creative tab. |
| 2 | Obtaining | Produced only by Biogun projectiles hitting a block ([04-projectiles-fx](../04-projectiles-fx/index.md)); never drops (`canHarvestBlock` false); no recipe. |
| 3 | Numbers | Size 1–3 (starts at 1); decay timer 60 ticks per step; contact poison amplifier 2 (Poison III) for 100 ticks on `EntityLiving` only; burst when size would exceed 3: explosion radius 3, damage 15, inner 1.5, outer 4.5, armour penetration 0.35, gore chance 1.0, no terrain damage. Bounds width 10 + 6s and thickness 2 + 4s (1/32 units). |
| 4 | Behaviour | [misc-tile-entities.md](misc-tile-entities.md#3-bio-blob). Decay runs on both sides ([MUTL-44](index.md#suspected-bugs)). |
| 5 | Persistence and sync | NBT `BlobTicks` (int), `side` (byte, never set), `size` (byte); description packet carries the full NBT. |
| 6 | UI | none; no tooltip. |
| 7 | Visuals | Default cube renderer (non-opaque, every side drawn); texture `techguns:bio` = [`textures/blocks/bio.png`](../07-blocks/textures.md#8-bug-nest-bio-blob) (16 × 16) on all faces. Particle effect `bioblobExplosion` on burst ([04](../04-projectiles-fx/index.md)). |
| 8 | Audio | burst plays `effects.biodeath` (4.0, pitch about 0.56–0.84). See [sounds.md](sounds.md#3-bio-blob). Step/break: default (stone). |
| 9 | Relationships | [`BiogunProjectile`](../04-projectiles-fx/projectiles.md#35-biogunprojectile) ([04](../04-projectiles-fx/index.md)), [`ProjectileExplosion`](../04-projectiles-fx/explosions.md#projectileexplosion) ([04](../04-projectiles-fx/index.md)), poison damage type ([01](../01-core/index.md)). |
| 10 | Bugs | [MUTL-41](index.md#suspected-bugs), [MUTL-42](index.md#suspected-bugs), MUTL-43, MUTL-44, [MUTL-45](index.md#suspected-bugs). |

## 10. Legacy `OreDrill` / `OreDrillTileEnt` (dead code)

Tile `OreDrillTileEnt` is registered (id `OreDrillTileEnt`) but no block creates it; the `OreDrill` block's registration is commented out. Zero slots, drops one iron ore when the block below is a cluster, timer 1000 ÷ cluster size after an initial 100. No GUI, texture, sound or lang key. Bug: MUTL-23. See [ore-drill.md](ore-drill.md#11-legacy-prototype-dead-code).
