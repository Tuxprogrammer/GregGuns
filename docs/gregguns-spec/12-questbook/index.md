# 12 Questbook: GregGuns spec

## Overview

This area has **no Techguns counterpart**. Techguns has no questbook, so there is no baseline to compare against and no matching folder in the [Techguns spec](../../techguns-spec/README.md). The area exists because the GTNH pack hands progression to players through BetterQuesting, and the owner wants a GregGuns tab that steers a player into the mod.

The quest line **ships inside the GregGuns jar** ([design record 0015](../../design/0015-m9-self-contained-polish.md)), not in the pack repository ([GT-New-Horizons-Modpack](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack)). The jar carries 135 quests in 15 chapters, plus 7 hidden tier-skip triggers (see [quest-line.md](quest-line.md#hidden-tier-skip-triggers)) as resource files in BetterQuesting's own format, and a hidden config key installs them into the server's BetterQuesting database. This area specifies the quest data, how the quests unlock, and how the resources are built and checked.

Findings from the pack (`reference/gtnh/2.9.0-RC-2/.minecraft/config/betterquesting/`) and BetterQuesting 3.8.89-GTNH source:

- **A quest line is a folder of small JSON files.** `DefaultQuests/` holds `QuestLinesOrder.txt` (tab order), `QuestSettings.json`, `QuestLines/<Name>-<id>/` (one `QuestLine.json` plus one position file per quest) and `Quests/<Name>-<id>/` (one file per quest). The shipped pack has 46 lines and 3813 quests. The loader reads the quest ID from the file body and walks `Quests/` recursively, so the folder grouping is the exporter's habit, not a rule ([QuestCommandDefaults#L420-L456](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L420-L456)).
- **IDs are UUIDs.** A quest ID is two 64-bit longs (`questIDHigh:4`, `questIDLow:4`). The file name suffix is the 16 bytes (high, then low, big-endian) as URL-safe Base64 ([UuidConverter#L21-L45](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api/utils/UuidConverter.java#L21-L45)). Old pack quests have `High = 0` and a small `Low`; newer ones are random UUIDs.
- **Text is inline.** The pack stores `name:8` and `desc:8` in the quest file and ships no lang keys. BetterQuesting does look up `betterquesting.quest.<id>.name` and `.desc` first, so translations can be layered on later ([QuestTranslation#L18-L21](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api2/utils/QuestTranslation.java#L18-L21), [L91-L97](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api2/utils/QuestTranslation.java#L91-L97)).
- **Task and reward types in use:** `retrieval` and `optional_retrieval`, `checkbox`, `crafting`, `hunt`, `location`, `fluid`, `meeting`; rewards `item`, `choice`, `questcompletion`, `xp`. No command reward is used anywhere in the pack.
- **Loot bags** are `enhancedlootbags:lootbag` with a group's `GroupMetaID` as the item damage. The pack's tier groups are Steam 2, LV 4, MV 5, HV 6, EV 7, IV 8, LuV 41 and ZPM 42 (`reference/gtnh/2.9.0-RC-2/.minecraft/config/EnhancedLootBags/LootBags.xml`); the Electric Macerator quest in Tier 1 pays damage 4. GregGuns quests pay the GregGuns groups 1701 to 1712, which GregGuns adds to EnhancedLootBags at startup ([pack-loot.md](../06-items/pack-loot.md#enhancedlootbags)).
- **Hunt tasks** match an `EntityList` name, count player kills only, and compare NBT for an exact match ([TaskHunt#L119-L137](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/tasks/TaskHunt.java#L119-L137)). That is why Infernal ranks cannot be hunted ([quest-line.md](quest-line.md#npcs-and-bosses)).
- **Each tier line has a "Tier N (X)" milestone quest** that already depends on that tier's gate-material quest (for example Tier 3 (HV) depends on stainless steel). Two gates are not implied and must be listed: steel for LV and the wrought-iron quest for Steam.
- **Kill All the Things** is 83 hunt quests rooted in other lines' quests. GregGuns copies the pattern in its own tab.

## Contents

| File | Covers |
|---|---|
| [quest-line.md](quest-line.md) | All 135 quests: key, title, tier, hint, tasks, rewards, prerequisites, layout, per chapter. Design rules, reward budget, coverage matrix |
| [authoring.md](authoring.md) | The pack's file format, ID derivation, one fully worked quest, the builder and validator in `tools/questbook/`, how to rebuild and test, checklist |

The code for this area is the quest data and tools in `tools/questbook/` (`quests/`, `build_quests.py`, `validate_quests.py`), the generated resources in `src/main/resources/assets/gregguns/questbook/`, and `QuestbookResourcesTest`. The installer is a separate piece, built behind the hidden config key.

## Tab placement

- **Position:** the installer adds the line last in the tab order, which is what one new line at the end of `DefaultQuests/QuestLinesOrder.txt` would do, after `AAAAAAAAAAAAAAAAAAAAJQ==: Kill All the Things`. That file is 46 lines, one per tab, and the order of lines is the order of tabs ([QuestCommandDefaults#L420-L440](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L420-L440)). The text after the colon is only a label.
- **Why last:** appending shifts no other tab. The pack's last tab is already the combat tab, so GregGuns sits next to its closest relative. The other candidate, directly after "Tier 7 - ZPM", would split the tier tabs from their branches.
- **Line ID:** `D4B4PFfzWT2Q4DUgPjN9oQ==`, derived from the key `line:gregguns` ([authoring.md](authoring.md#ids)). The folders are `QuestLines/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/` and `Quests/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/`.
- **Visibility:** `UNLOCKED`, as every tier line and Kill All the Things use.
- **Icon:** `gregguns:pistol_9mm`.

## Unlock design

The tab gates on GTNH progression. Each chapter intro lists the pack's tier quest and the pack's gate-material quest from [0001](../../design/0001-tier-map.md). Quest IDs are the `questIDLow:4` of the pack quest (their `High` is 0).

| Chapter | Pack quests required (all) | Gate material (0001) | Already implied by the tier quest? |
|---|---|---|---|
| Start here | any of 11, 44 | none | n/a |
| Steam / ULV | 44, 948 | Bronze, wrought iron (948 asks for wrought iron plates) | 948 is not |
| LV | 88, 76 | Steel (76 is the Bricked Blast Furnace quest) | 76 is not |
| MV | 1148, 1598 | Aluminum (1148 depends on 103, which asks for 8 aluminum plates) | 103 yes, 1598 not |
| HV | 160, 753 | Stainless steel (753 asks for 64 plates) | yes |
| EV | 176, 860 | Titanium (860 asks for 16 plates) | yes |
| IV | 213, 861 | Tungstensteel (861 asks for 64 plates) | yes |
| LuV | 1493, 1491, 1412 | HSS-G (1491 asks for 8 plates); Assembly Line (1412) | yes |
| ZPM | 2610, 1667 | Naquadah alloy (1667) | yes |

Branch chapters unlock from one early quest and then gate each quest by its own tier quest: NPCs and Structures from 11 (Monster Hunter), Machines from 97 (LV assembler), Equipment and Ammo from 88, and ICBM warheads from `gg.ammo.explosives`. All chapters also require `gg.start.welcome`.

Rules that keep the gates honest:

1. **A tier gate is a pack quest.** GregGuns never gates on its own chapter order.
2. **Rewards cannot outrun the gate.** A bag or item never exceeds the quest's tier, and the quest's tier never exceeds the highest pack tier in its prerequisite chain ([GGS-12-07](#requirements)).
3. **Visibility is `NORMAL`** for almost every quest: a quest shows when its prerequisites are unlocked, so a player sees one step ahead ([EnumQuestVisibility](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api/enums/EnumQuestVisibility.java#L3-L11)). Three boss quests are `SECRET` because their NPCs do not spawn naturally.

## Referenced pack quests

Thirty-one existing quests are prerequisites. All have `questIDHigh:4` = 0 in the shipped pack. `Tab` is the quest line the quest is drawn in.

| ID | Name | Tab | Tier | Used by chapters |
|---|---|---|---|---|
| 11 | Monster Hunter | ...Without Dying | Steam | start, npc, site |
| 44 | Welcome to Tier 0.5, Steam! | Tier 0.5 - Steam | Steam | start, steam |
| 76 | Time to Get Some Steel | Multiblock Goals | LV | lv |
| 88 | Welcome to Tier 1, LV | Tier 1 - LV | LV | lv, npc, site, gear, ammo |
| 97 | Avengers, Assemble! | Tier 1 - LV | LV | mach |
| 114 | Basic Transport: Items | Tier 1 - LV | LV | ammo |
| 160 | Tier 3 (HV) | Tier 3 - HV | HV | hv, npc, mach, gear, ammo, icbm |
| 176 | Tier 4 (EV) | Tier 4 - EV | EV | ev, npc, mach, ammo, icbm |
| 213 | Tier 5 (IV) | Tier 5 - IV | IV | iv, npc |
| 553 | Diesel Fuel | Tier 2 - MV | MV | hv, ammo |
| 619 | Battery Buffer | Storing and Transforming EU | LV | ammo |
| 753 | Stainless Steel EBF processing | Tier 2 - MV | HV | hv, icbm |
| 762 | TNT | Multiblock Goals | LV | mv, ammo |
| 822 | Oxygen Gear | Space Race | HV | gear |
| 824 | Oxygen Tanks | Space Race | HV | gear |
| 860 | Titanium, Finally | Tier 3 - HV | HV | ev, icbm |
| 861 | Tungstensteel | Tier 4 - EV | EV | iv |
| 948 | Better Than Iron | Tier 0.5 - Steam | Steam | steam |
| 1082 | MV Battery Buffers | Storing and Transforming EU | MV | ammo |
| 1142 | HV Battery Buffers | Storing and Transforming EU | HV | hv, ammo |
| 1148 | Tier 2 (MV) | Tier 2 - MV | MV | mv, npc, site, mach, gear |
| 1262 | EV Battery Buffer | Storing and Transforming EU | EV | ev |
| 1412 | Assembly Line | Multiblock Goals | LuV | luv |
| 1458 | Heavy Rocket Engine | Space Race | EV | icbm |
| 1459 | Heavy Rocket Fins | Space Race | EV | icbm |
| 1491 | HSS-G | Tier 4 - EV | EV | luv |
| 1493 | Tier 6 (LuV) | Tier 6 - LuV | LuV | luv |
| 1598 | Avengers, Assemble! 2 | Tier 2 - MV | MV | mv, ammo |
| 1667 | Your First Naquadah Ingot | Tier 5 - IV | IV | zpm |
| 1888 | Repairing Your Hazmat Suit | Tier 0.5 - Steam | Steam | gear |
| 2610 | Tier 7 (ZPM) | Tier 7 - ZPM | ZPM | zpm |

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| (none: Techguns has no quests) | One BetterQuesting line of 148 quests | BetterQuesting 3.8.89-GTNH | [authoring.md](authoring.md) |
| Hostile NPC factions, bosses ([10](../10-entities-npcs/index.md)) | `bq_standard:hunt` on `gregguns.<Name>` | BetterQuesting | [quest-line.md](quest-line.md#npcs-and-bosses) |
| Infernal variants ([infernal-mobs.md](../10-entities-npcs/infernal-mobs.md)) | No hunt: a hunt cannot match "any infernal" (GGS-12-11) | Infernal Mobs 1.10.7-GTNH | [quest-line.md](quest-line.md#npcs-and-bosses) |
| Structures and their chests ([11](../11-worldgen/index.md)) | Quests that ask for what the structure holds (GGS-12-12) | TooMuchLoot, EnhancedLootBags | [quest-line.md](quest-line.md#structures) |
| Tier bag rewards ([pack-loot.md](../06-items/pack-loot.md#enhancedlootbags)) | A `bag` reward resolved through the builder's `BAGS` table: `enhancedlootbags:lootbag` with a GregGuns group id (1701 to 1712) as damage | EnhancedLootBags 1.3.5 | [authoring.md](authoring.md#quest-data) |
| Tier map ([0001](../../design/0001-tier-map.md)) | One chapter per tier, gated on the pack's tier and material quests | GT progression quests | [Unlock design](#unlock-design) |
| Exact-tier charging ([0008](../../design/0008-exact-tier-charging.md)) | Taught in the HV, EV and ammo quests | GT battery buffers | [quest-line.md](quest-line.md#coverage-matrix) |
| Repair Bench, Turret ([08](../08-machines-production/index.md), [09](../09-machines-utility/index.md)) | A machines branch | GT multiblock framework | [quest-line.md](quest-line.md#machines) |
| Kill All the Things line | Same hunt pattern, separate tab, optional cross-link | BetterQuesting | [quest-line.md](quest-line.md#relation-to-the-packs-lines) |

## Delivered from the jar

Per [0015](../../design/0015-m9-self-contained-polish.md) nothing here is a change to the pack's repository. All paths are under `assets/gregguns/questbook/` in the jar, mirroring BetterQuesting 3.8.89-GTNH `DefaultQuests`. A jar cannot list a directory, so `index.json` names every file and the installer reads only those paths.

| Resource | Content | Sheet |
|---|---|---|
| `index.json` | `format`, the `line` path, `linePositions` (133 paths), `quests` (131 paths: 133 plus the 7 triggers), `orderEntry` (`D4B4PFfzWT2Q4DUgPjN9oQ==: GregGuns`, the entry the installer adds to the tab order) and `retired` (the ids the installer deletes) | [authoring.md](authoring.md#file-layout) |
| `QuestLines/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/QuestLine.json` | Line properties: name `GregGuns`, icon, `visibility` `UNLOCKED` | [authoring.md](authoring.md#line-files) |
| `QuestLines/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/<Name>-<id>.json` (133) | One position file per quest: `x`, `y`, `sizeX`, `sizeY` | [quest-line.md](quest-line.md) |
| `Quests/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/<Name>-<id>.json` (133) and `Quests/NoQuestLine/<Name>-<id>.json` (7 triggers) | One file per quest: properties, prerequisites, tasks, rewards | [authoring.md](authoring.md#quest-files) |

How the install behaves:

- **Hidden config key.** The installer, `compat/BetterQuestingInstall`, is built behind a hidden config key that is off by default and absent from the in-game config GUI. With it off nothing is installed and BetterQuesting is never touched.
- **Repeatable.** Quest and line IDs are stable UUIDs derived from the keys ([GGS-12-02](#requirements)), so installing twice, or after an update, replaces the same quests rather than adding copies. Retired quests are deleted from the database, with their line entries and progress.
- **No pack edits.** No `QuestSettings.json` bump, no `QuestLinesOrder.txt` edit, no `LootBags.xml` change. Quests hand out the GregGuns bags, whose groups the jar adds to EnhancedLootBags in memory ([pack-loot.md](../06-items/pack-loot.md#enhancedlootbags)).
- **Optional transition.** A one-quest cross link from the Kill All the Things line is left to a pack maintainer who wants it ([quest-line.md](quest-line.md#relation-to-the-packs-lines)).

[PACK-CHANGES.md](../PACK-CHANGES.md) and [01-core](../01-core/index.md) each have a one-line quest row; they point here.

## Requirements

- **GGS-12-01** The tab is one quest line named `GregGuns`, `visibility` `UNLOCKED`, added to the tab order after Kill All the Things (the `orderEntry` in `index.json`).
- **GGS-12-02** Every GregGuns quest and line ID is `uuid5(namespace 6f1f3c52-9d0a-4b6e-8a53-2c7e0f4b91d8, key)`, with the high and low halves written as signed longs and the file suffix as URL-safe Base64 of the 16 bytes. A key is never renamed after release. A removed quest's key goes in a `retired` list in the data; `index.json` lists the retired ids and the installer deletes exactly those quests from an existing database. A retired key is never reused.
- **GGS-12-03** `gg.start.welcome` unlocks when either pack quest 11 or pack quest 44 is complete (`questLogic` `OR`). Prerequisite logic is `AND` unless a quest has a reason for `OR`. Task logic (`taskLogic`) is `AND` by default and `OR` where any one of two or more tasks completes the quest; `OR` never sits on a quest with an `optional_retrieval`, which BetterQuesting would then complete by itself.
- **GGS-12-04** Every chapter intro lists the pack quests in the [Unlock design](#unlock-design) table, plus `gg.start.welcome`.
- **GGS-12-05** A prerequisite is a GregGuns key from the same file set, or a pack quest id from the [Referenced pack quests](#referenced-pack-quests) table. Nothing else.
- **GGS-12-06** Every prerequisite exists, and the prerequisite graph has no cycle.
- **GGS-12-07** A quest's tier is at most the highest pack tier in its prerequisite chain. A bag reward's tier, read from the builder's bag table, is at most the quest's tier, wherever the bag appears (an item, choice or bag reward). A fixed item reward belongs to that tier or lower under [0001](../../design/0001-tier-map.md).
- **GGS-12-08** Rewards are `item`, `choice`, `xp` and `bag`, in the pack's shape: a `choice` of useful items of the quest's tier with a loot bag as one option, plus an `item` reward of coins (`dreamcraft:Coin*`), main quests paying about twice the coins. A bag names a tier (`{"bag": "lv"}` in any reward list, or a `bag` reward), and one table in `build_quests.py` (`BAGS`) maps it to the stack handed out: `enhancedlootbags:lootbag` with a GregGuns group's `GroupMetaID` as damage. Bags never hold food; food rewards are Pam's HarvestCraft items in an `item` reward. No command reward is used.
- **GGS-12-09** A quest asks for the thing it teaches. Items made in a GT machine are `retrieval` tasks with `consume` off: "one of this family" is one retrieval with `requireOnlyOneItem`, "all these parts" is one retrieval listing every part, and alternative sets are several tasks with `taskLogic` `OR`. Recipe inputs may be shown in an `optional_retrieval` beside the required task. An OreDict entry may stand for a material. `crafting` tasks are for items made in the crafting grid, furnace or anvil only, since BetterQuesting counts nothing else.
- **GGS-12-10** Hunt tasks use `gregguns.<Name>` entity names with `subtypes` off, and every name is registered by GregGuns.
- **GGS-12-11** Only `gg.start.welcome` uses a checkbox. Every other quest has a task the game checks: an item, a craft or a kill. A hunt cannot match an Infernal rank, so no quest hunts one.
- **GGS-12-12** A structure quest asks for something the player brings back from it ([11-worldgen](../11-worldgen/index.md)). No `location` task names a GregGuns structure.
- **GGS-12-13** Only the three config-gated boss quests (Dictator Dave, Cyberdemon, Alien Bug) are `SECRET`.
- **GGS-12-14** Quest text is inline (`name:8`, `desc:8`) and mirrored in the generated `en_US.lang` block, reads as prose in paragraphs the way the pack's quests do, and uses only the pack's `[warn]`, `[note]`, `[quest]`, `[url]` and `[questlink]` tags; a `[questlink]` names a quest key, which the builder turns into its id. Each title carries its tier's color and bold, as the pack's tier tabs do (Steam `§c`, LV `§2`, MV `§6`, HV `§5`, EV `§8`, IV `§9`, LuV `§d`, ZPM `§b`); the builder adds it. Each quest teaches one mechanic.
- **GGS-12-15** No two position boxes in the line overlap. A box is 24 units; a main quest (`main: true`, the spine a player must follow, drawn with thick incoming lines) or a chapter head may be 48, a tier milestone 72, and the older chapter heads are 28.
- **GGS-12-16** Every `gregguns:` item id and `gregguns.<Name>` entity name in the files is a name the mod registers, and every GregGuns machine is a `gregtech:gt.blockmachines` stack with one of the reserved MTE ids. `QuestbookResourcesTest` checks them against `NeiOrder`, `NpcMob` and `GregGunsMTEIDs`. Every other item id (coins, Galacticraft parts, GT materials, food, loot bags) is one the pack itself uses: it is in `tools/questbook/pack_items.txt`, built by `pack_items.py` from the pack's `DefaultQuests`, `LootBags.xml` and the HarvestCraft foods. A GT machine is ours or one the pack's quests name. No placeholder name is left.
- **GGS-12-17** The Repair Bench and Turret quests carry a provisional tier and are revisited when [09](../09-machines-utility/index.md) settles them.
- **GGS-12-18** The quest line ships in the GregGuns jar and is installed only when the hidden config key is on, so a pack without GregGuns, or with the key off, has no GregGuns tab and no dangling quests. The line and its quests have fixed IDs, so the installer can remove exactly them: on every install it deletes the retired ids that the database still holds (GGS-12-02) and touches no other quest.

## Tests

| Requirement | Verified by |
|---|---|
| GGS-12-01, 03, 04, 05, 06, 15 | `QuestbookResourcesTest` (index lists every file, prerequisites resolve to a GregGuns quest or a pack id in the [Referenced pack quests](#referenced-pack-quests) table, no cycle, no overlapping boxes). `tools/questbook/validate_quests.py` runs the same checks on the files, and on a full `DefaultQuests` folder with `--only GregGuns-` |
| GGS-12-02 | `QuestbookResourcesTest` recomputes uuid5 of every key and every retired key in `tools/questbook/quests/` and compares them to the resources and the `retired` list of `index.json`. Rebuilding with `build_quests.py` gives byte-identical files. `QuestbookInstallerTest` covers the retirement plan: only retired ids the database holds are deleted, and a shipped id can never be retired |
| GGS-12-03, 07, 08, 09, 11, 12, 13, 14, 15 | Build-time checks in `build_quests.py` (bag tier at most the quest tier, known bag names, pack item ids, checkbox only on the welcome quest, task and reward shapes, no `taskLogic` `OR` with an optional task, box sizes, description tags and `[questlink]` keys, SECRET only on the three bosses). `validate_quests.py --data` runs them too. A review confirms the table in [quest-line.md](quest-line.md) matches the data |
| GGS-12-10, 16 | `QuestbookResourcesTest` against `NeiOrder.names()`, `NpcMob` and `GregGunsMTEIDs` |
| GGS-12-14 | Manual review against the checklist in [authoring.md](authoring.md#checklist) |
| GGS-12-17 | Review when the 09 open questions close |
| GGS-12-18 | Installer tests (key off installs nothing, retirement deletes exactly the retired ids) and a boot of a dev pack without BetterQuesting |
| All | A full playthrough of every tier gate in a GTNH dev pack with the key on, on a fresh world and on an old one |

## Related areas

- [06-items](../06-items/pack-loot.md): the tier bags and the loot tables the structure quests point at.
- [10-entities-npcs](../10-entities-npcs/index.md): entity names and Infernal behavior used by the NPC branch.
- [11-worldgen](../11-worldgen/index.md): structures and their loot ceilings.
- [08-machines-production](../08-machines-production/index.md) and [09-machines-utility](../09-machines-utility/index.md): the machines branch.
- [05-armor](../05-armor/index.md): the equipment branch.
- [01-core](../01-core/index.md): its one-line quest row.

## Open questions

1. **Infernal hunts.** A hunt cannot match "any infernal mob", so the rank quests are retired and the NPC hunt quests explain the ranks. A small GregGuns-registered BetterQuesting task (soft dependency on Infernal Mobs, reading `getIsRareEntity` and `getModSize`) would make rank quests possible again, under new keys.
2. **Structure visits.** Structure quests ask for what the structure holds. If GregGuns registers its structures with a map-gen structure id, `location` tasks could name them. Needs the M8 structure design.
3. **Repair Bench and Turret tier.** The quests assume MV and HV. Settled by area 09 and a record on machines.
4. **Reward balance.** A bag on nearly every quest, plus coins, follows the pack's habit; the balance pass after the rewrite checks the totals.
5. **Bag content.** Each GregGuns bag draws from its own group (GregGuns items and the precursors GregGuns recipes consume) and, without Fortune III, the bag one tier down ([pack-loot.md](../06-items/pack-loot.md#enhancedlootbags)). A new bag is a group in `groups.xml` plus a `BAGS` row.
6. **Hunting with GregGuns damage.** `damageType` can require a damage source name. Whether GregGuns kills should be required (for example "kill with a bullet") depends on the names fixed in [0004](../../design/0004-damage-model.md).
7. **Translation.** The pack ships English inline. Resolved in M9: `en_US.lang` carries a generated `betterquesting.*` block (`build_quests.py --lang`), checked by `LangFilesTest`; see [translating.md](../../translating.md).
8. **Aluminum gate.** No pack quest is solely about aluminum. The MV gate relies on 1148 depending on 103. A direct prerequisite on 103 is optional.
