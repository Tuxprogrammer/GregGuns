# Lang keys, generated strings and hard-coded English (area 09)

Every lang key this area uses, with the exact English text from `assets/techguns/lang/en_US.lang`, the strings that code builds at runtime, and the gaps. Model: [05-armor/lang-and-strings.md](../05-armor/lang-and-strings.md). The condensed key table in [index.md](index.md#lang-keys-used-by-this-area-en_us) is kept; this file adds the code locations, conditions and gaps.

[`TextUtil.trans`](../01-core/utilities.md#textutil) is the mod's wrapper for translating a key on the client; the vanilla `I18n.format` is used in a few places. Both look the key up exactly as written, so a key without the `techguns.` prefix will not resolve to the prefixed entry.

## 1. Block and item names

| Key | English | Where it is requested |
|---|---|---|
| `techguns.tile.BasicMachine_2.name` | Turret | item name of `basicMachine:2` |
| `techguns.tile.BasicMachine_3.name` | Camo Bench | `basicMachine:3` |
| `techguns.tile.BasicMachine_5.name` | Repair Bench | `basicMachine:5` |
| `techguns.tile.BasicMachine_6.name` | Explosive Charge | `basicMachine:6` |
| `techguns.tile.BasicMachine_7.name` | Flag | `basicMachine:7` (not in the creative list, but shown if given) |
| `techguns.tile.BasicMachine_8.name` | Advanced Explosive Charge | `basicMachine:8` |
| `techguns.tile.oreDrill_0.name` | Ore Drill Frame | `oreDrill:0` |
| `techguns.tile.oreDrill_1.name` | Ore Drill Scaffold | `oreDrill:1` |
| `techguns.tile.oreDrill_2.name` | Ore Drill Rod | `oreDrill:2` |
| `techguns.tile.oreDrill_3.name` | Ore Drill Engine | `oreDrill:3` |
| `techguns.tile.oreDrill_4.name` | Ore Drill Controller | `oreDrill:4` |
| `techguns.tile.tgchest.name` | Reinforced Chest | [`tgchest`](../07-blocks/block-catalog.md#registered-blocks) (the item block adds the `techguns.` prefix) |
| `techguns.tile.tgchest_weapon.name` | Weapon Chest | [`tgchest_weapon`](../07-blocks/block-catalog.md#registered-blocks) |
| `techguns.tile.Bioblob.name` | Bio Blob | defined, but the plain item block asks for `tile.Bioblob.name` ([MUTL-43](index.md#suspected-bugs)) |
| `entity.Techguns.SimpleTurret.name` | Turret | the mounted [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) entity (10 owns the entity) |

Adjacent area-08 names in the same numbering (listed for orientation only): `BasicMachine_0` Ammo Press, `_1` Metal Press, `_4` Chemical Laboratory, `_9` Charging Station.

## 2. Container and inventory titles

| Key | English | Used by |
|---|---|---|
| `techguns.container.repairbench` | Repair Bench | repair bench GUI title, via `getInventoryName` |
| `techguns.container.camobench` | Camo Workbench | camo bench GUI title (differs from the block name "Camo Bench") |
| `techguns.container.turretbase` | Turret Platform | turret GUI title |
| `techguns.container.oredrill` | Ore Drill | ore drill GUI title |
| `techguns.container.tgchest` | Reinforced Chest | single reinforced chest |
| `techguns.container.tgchest.double` | Large Reinforced Chest | double reinforced chest |
| `techguns.container.tgchest_weapon` | Weapon Chest | single weapon chest |
| `techguns.container.tgchest_weapon.double` | Large Weapon Chest | double weapon chest |
| `techguns.inventory.explosivecharge` | Explosive Charge | both charge GUIs |
| `container.inventory` (vanilla) | Inventory | caption in repair bench, turret, ore drill, scrapper and chest GUIs; the camo bench's call is commented out |

## 3. GUI text and tooltips

| Key | English | Condition and place |
|---|---|---|
| `techguns.repairBench.noarmor` | No armor | hover over a repair button while nothing is worn in that slot |
| `techguns.repairBench.cantrepair` | Can't repair this armor | worn item is not repairable on the bench |
| `techguns.repairBench.fullcondition` | Armor is fully repaired | worn armor has damage 0 |
| `techguns.repairBench.requiredMats` | Required materials for repair: | first line of the damaged-armor tooltip |
| `techguns.item.invalidcamo` | No Camo | camo bench, when the bench slot item has no camo name |
| `techguns.container.repair` | Repair time left | turret GUI repair-bar tooltip, followed by " " and seconds and "s" |
| `techguns.turret.targetAnimals` | Target Animals | turret GUI button tooltip, followed by ": " and Yes or No |
| `techguns.yes` / `techguns.no` | Yes / No | the target-animals tooltip (Yes in red, No in green, as coded) |
| `techguns.gui.empty` | Empty | ore drill tank tooltips with no fluid |
| `techguns.oredrill.nofuel` | No Fuel | ore drill fuel flame tooltip when the buffer maximum is 0 |
| `techguns.oredrill.currentfuel` | Fuel | first fuel line, formatted "Fuel n/m" |
| `techguns.oredrill.remaining` | Remaining time | second fuel line, formatted "Remaining time: N ticks." |
| `techguns.oredrill.ticks` | ticks. | the unit at the end of that line |
| `techguns.container.oredrill.broken` | Multiblock broken | red line in the ore drill GUI while incomplete |
| `techguns.container.oredrill.broken2` | or invalid Drill | red line under it |
| `techguns.block.hasTileEntityData` | Has TileEntity Data | machine item tooltip when NBT `TileEntityData` exists |
| `techguns.block.chestHasContents` | Has contents | chest item tooltip when NBT `TileEntityData` exists |
| `techguns.container.security.denied` | [DENIED] You have no permission to use | chat message when the security check fails (turret, other powered tiles; the chest branch cannot trigger) |
| `techguns.oreDrillTooltip.size`, `.mininglevel` | Valid for drills with size / Bonus mining level | drill head item tooltips (owned by [06](../06-items/index.md)) |

Camo names shown by the camo bench are lang keys returned by each item's `getCurrentCamoName`; they are owned by [03](../03-gun-visuals-audio/index.md), [05](../05-armor/index.md), [06](../06-items/index.md) and 07.

## 4. Chat messages from the ore drill

| Key | English | Condition |
|---|---|---|
| `techguns.msg.error.alreadyrod` | Can't place here, this cluster already has a connected drill rod | placing a rod next to a cluster that already has one |
| `techguns.msg.error.oredrill.controllernext` | Can't place 2 ore drill controllers next to each other | placing a controller next to another; looked up without the `techguns.` prefix, so the raw key is printed ([MUTL-20](index.md#suspected-bugs)) |
| `techguns.msg.error.oredrill.invalidmulti.dir` | The main engine block must have exactly 1 neighboring engine or rod | formation: zero or several neighboring engines or rods |
| `techguns.msg.error.oredrill.invalid1` | There must be engines followed by rods that are connected to a cluster | formation: center line is wrong |
| `techguns.msg.error.oredrill.invalid2` | A drill bigger than 1 rod and 1 engine needs a frame | radius 0 check failing |
| `techguns.msg.error.oredrill.invalid2.1` | A medium drill needs at least 1 engine and a rod of length 2 or more | radius 1 too short |
| `techguns.msg.error.oredrill.invalid2.2` | A large drill needs at least 2 engine rows and a rod of length 3 or more | radius 2 to 4 too short (shown for four different failures) |
| `techguns.msg.error.oredrill.invalid3` | Invalid block in slice around (two trailing spaces) | slice mismatch, then the code appends a parenthesized coordinate triple |
| `techguns.msg.oredrill.formed1` / `formed2` / `formed3` | "Ore driller multiblock formed with radius: " / ", extra engine size: " / " and drill size: " | success, concatenated with the numbers |

The three `formed` strings carry leading and trailing spaces in the lang file, and the code adds more spaces between parts. Example output: "Ore driller multiblock formed with radius: 2 , extra engine size: 2  and drill size: 3".

## 5. Strings generated in code

| String | Built from | Condition |
|---|---|---|
| "Nx item name" per material | stack size, "x ", then the translated item name | each line of the damaged-armor tooltip; prefixed with the red color code when the bench lacks the material |
| "Repair time left 7s" | `techguns.container.repair`, space, seconds, "s" | turret GUI, while the turret is dead |
| "Target Animals: Yes" / "...: No" | key, ": ", colored Yes/No | turret GUI button |
| "RF/t: n", "p.pp%", "Ores/h: n.nn" | numbers with hard-coded labels | ore drill GUI hover over the cone; the percentage divides by `totalTime` ([MUTL-22](index.md#suspected-bugs)) |
| "tank fluid name", "amount/capacityMB" | fluid unlocalized name plus ".name"-style lookup, then numbers | ore drill tank tooltips |
| "(i/n):" | one-based camo index and count | camo bench, left caption |
| "Fuel a/b" and "Remaining time: N ticks." | buffer numbers | ore drill fuel tooltip; N is -1 when RF/t is 0 |
| numbers 0..n | camo index + 1 per armor, back and face slot | camo bench footer row |

## 6. Hard-coded English (not in the lang file)

| Text | Place | Bug |
|---|---|---|
| "Radius:", "Length:", "Delay:", "Detonate" | [`GuiExplosiveCharge`](../08-machines-production/gui-textures.md#10-texturesguiexpchargeguipng-owned-by-09) labels and the arm button | [MUTL-32](index.md#suspected-bugs) |
| "RF/t:", "Ores/h:" | [`GuiOreDrill`](ore-drill.md#7-gui-guioredrill-id-16) progress tooltip | recorded under MUTL-32's pattern; no separate bug |
| "+", "-", ">", "<" | camo bench button labels | symbols, not translatable text |
| "Power: n/max RF" | base powered-GUI power-bar tooltip ([08](../08-machines-production/index.md)) | owned by [08](../08-machines-production/index.md) |

## 7. Missing, stale and mismatched keys

| Key | Status |
|---|---|
| `techguns.container.scrapper` | missing; the scrapper title would print the raw key if the scrapper could be opened |
| `container.campflag` | missing; the flag has no GUI, so it is never shown |
| `techguns.tile.BasicMachine_10.name` | missing; meta 10 is never obtainable |
| `tile.Bioblob.name` | the key the Bio Blob item block actually requests; the file defines `techguns.tile.Bioblob.name` (MUTL-43) |
| `msg.error.oredrill.controllernext` | requested without the `techguns.` prefix (MUTL-20) |
| `techguns.tile.oreDrill.name` = Ore Drill | stale; no code requests it |
| `techguns.tile.OreDrillBlock_0.name` to `_3.name` = Ore Drill Heavy Frame, Light Frame, Rod, Engine | stale; superseded by `oreDrill_0..4` |
| `techguns.container.security.tooltip.*`, `.owner`, `.unowned` | the chest GUI's security tooltip code is commented out, so these are not shown on chests; they belong to [08](../08-machines-production/index.md) |

## 8. Notes

- Name pairs that differ: the Camo Bench block is "Camo Bench" but its GUI title is "Camo Workbench"; the Turret block is "Turret" but its GUI title is "Turret Platform"; the Flag is named "Flag".
- The Scrapper has no English name anywhere.
