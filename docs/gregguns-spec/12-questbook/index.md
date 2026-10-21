# 12 Questbook: GregGuns spec

## Overview

This area has **no Techguns counterpart**. Techguns has no questbook, so there is no baseline to compare against and no matching folder in the [Techguns spec](../../techguns-spec/README.md). The area exists because the GTNH pack hands progression to players through BetterQuesting, and the owner wants a GregGuns tab that steers a player into the mod.

The quest line **ships inside the GregGuns jar** ([design record 0015](../../design/0015-m9-self-contained-polish.md)), not in the pack repository ([GT-New-Horizons-Modpack](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack)). The jar carries 121 quests in 14 chapters as resource files in BetterQuesting's own format, and a hidden config key installs them into the server's BetterQuesting database. This area specifies the quest data, how the quests unlock, and how the resources are built and checked.

Findings from the pack (`reference/gtnh/2.9.0-RC-2/.minecraft/config/betterquesting/`) and BetterQuesting 3.8.89-GTNH source:

- **A quest line is a folder of small JSON files.** `DefaultQuests/` holds `QuestLinesOrder.txt` (tab order), `QuestSettings.json`, `QuestLines/<Name>-<id>/` (one `QuestLine.json` plus one position file per quest) and `Quests/<Name>-<id>/` (one file per quest). The shipped pack has 46 lines and 3813 quests. The loader reads the quest ID from the file body and walks `Quests/` recursively, so the folder grouping is the exporter's habit, not a rule ([QuestCommandDefaults#L420-L456](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L420-L456)).
- **IDs are UUIDs.** A quest ID is two 64-bit longs (`questIDHigh:4`, `questIDLow:4`). The file name suffix is the 16 bytes (high, then low, big-endian) as URL-safe Base64 ([UuidConverter#L21-L45](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api/utils/UuidConverter.java#L21-L45)). Old pack quests have `High = 0` and a small `Low`; newer ones are random UUIDs.
- **Text is inline.** The pack stores `name:8` and `desc:8` in the quest file and ships no lang keys. BetterQuesting does look up `betterquesting.quest.<id>.name` and `.desc` first, so translations can be layered on later ([QuestTranslation#L18-L21](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api2/utils/QuestTranslation.java#L18-L21), [L91-L97](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api2/utils/QuestTranslation.java#L91-L97)).
- **Task and reward types in use:** `retrieval` and `optional_retrieval`, `checkbox`, `crafting`, `hunt`, `location`, `fluid`, `meeting`; rewards `item`, `choice`, `questcompletion`, `xp`. No command reward is used anywhere in the pack.
- **Loot bags** are `enhancedlootbags:lootbag` with the tier group's `GroupMetaID` as the item damage (Steam 2, LV 4, MV 5, HV 6, EV 7, IV 8, LuV 41, ZPM 42; `reference/gtnh/2.9.0-RC-2/.minecraft/config/EnhancedLootBags/LootBags.xml`). The Electric Macerator quest in Tier 1 pays damage 4.
- **Hunt tasks** match an `EntityList` name, count player kills only, and compare NBT for an exact match ([TaskHunt#L119-L137](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/tasks/TaskHunt.java#L119-L137)). That is why Infernal ranks cannot be hunted ([quest-line.md](quest-line.md#npcs-and-bosses)).
- **Each tier line has a "Tier N (X)" milestone quest** that already depends on that tier's gate-material quest (for example Tier 3 (HV) depends on stainless steel). Two gates are not implied and must be listed: steel for LV and the wrought-iron quest for Steam.
- **Kill All the Things** is 83 hunt quests rooted in other lines' quests. GregGuns copies the pattern in its own tab.

## Contents

| File | Covers |
|---|---|
| [quest-line.md](quest-line.md) | All 121 quests: key, title, tier, hint, tasks, rewards, prerequisites, layout, per chapter. Design rules, reward budget, coverage matrix |
| [authoring.md](authoring.md) | The pack's file format, ID derivation, one fully worked quest, the builder and validator in `tools/questbook/`, how to rebuild and test, checklist |

The code for this area is the quest data and tools in `tools/questbook/` (`quests.json`, `build_quests.py`, `validate_quests.py`), the generated resources in `src/main/resources/assets/gregguns/questbook/`, and `QuestbookResourcesTest`. The installer is a separate piece, built behind the hidden config key.

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

Branch chapters unlock from one early quest and then gate each quest by its own tier quest: NPCs and Structures from 11 (Monster Hunter), Machines from 97 (LV assembler), Equipment and Ammo from 88. All chapters also require `gg.start.welcome`.

Rules that keep the gates honest:

1. **A tier gate is a pack quest.** GregGuns never gates on its own chapter order.
2. **Rewards cannot outrun the gate.** A bag or item never exceeds the quest's tier, and the quest's tier never exceeds the highest pack tier in its prerequisite chain ([GGS-12-07](#requirements)).
3. **Visibility is `NORMAL`** for almost every quest: a quest shows when its prerequisites are unlocked, so a player sees one step ahead ([EnumQuestVisibility](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api/enums/EnumQuestVisibility.java#L3-L11)). Three boss quests are `SECRET` because their NPCs do not spawn naturally.

## Referenced pack quests

Twenty-nine existing quests are prerequisites. All have `questIDHigh:4` = 0 in the shipped pack. `Tab` is the quest line the quest is drawn in.

| ID | Name | Tab | Tier | Used by chapters |
|---|---|---|---|---|
| 11 | Monster Hunter | ...Without Dying | Steam | start, npc, site |
| 44 | Welcome to Tier 0.5, Steam! | Tier 0.5 - Steam | Steam | start, steam |
| 76 | Time to Get Some Steel | Multiblock Goals | LV | lv |
| 88 | Welcome to Tier 1, LV | Tier 1 - LV | LV | lv, npc, site, gear, ammo |
| 97 | Avengers, Assemble! | Tier 1 - LV | LV | mach |
| 114 | Basic Transport: Items | Tier 1 - LV | LV | ammo |
| 160 | Tier 3 (HV) | Tier 3 - HV | HV | hv, npc, mach, gear, ammo |
| 176 | Tier 4 (EV) | Tier 4 - EV | EV | ev, npc, mach, ammo |
| 213 | Tier 5 (IV) | Tier 5 - IV | IV | iv, npc |
| 553 | Diesel Fuel | Tier 2 - MV | MV | hv, ammo |
| 619 | Battery Buffer | Storing and Transforming EU | LV | ammo |
| 753 | Stainless Steel EBF processing | Tier 2 - MV | HV | hv |
| 762 | TNT | Multiblock Goals | LV | mv, ammo |
| 822 | Oxygen Gear | Space Race | HV | gear |
| 824 | Oxygen Tanks | Space Race | HV | gear |
| 860 | Titanium, Finally | Tier 3 - HV | HV | ev |
| 861 | Tungstensteel | Tier 4 - EV | EV | iv |
| 948 | Better Than Iron | Tier 0.5 - Steam | Steam | steam |
| 1082 | MV Battery Buffers | Storing and Transforming EU | MV | ammo |
| 1142 | HV Battery Buffers | Storing and Transforming EU | HV | hv, ammo |
| 1148 | Tier 2 (MV) | Tier 2 - MV | MV | mv, npc, site, mach, gear |
| 1262 | EV Battery Buffer | Storing and Transforming EU | EV | ev |
| 1412 | Assembly Line | Multiblock Goals | LuV | luv |
| 1491 | HSS-G | Tier 4 - EV | EV | luv |
| 1493 | Tier 6 (LuV) | Tier 6 - LuV | LuV | luv |
| 1598 | Avengers, Assemble! 2 | Tier 2 - MV | MV | mv, ammo |
| 1667 | Your First Naquadah Ingot | Tier 5 - IV | IV | zpm |
| 1888 | Repairing Your Hazmat Suit | Tier 0.5 - Steam | Steam | gear |
| 2610 | Tier 7 (ZPM) | Tier 7 - ZPM | ZPM | zpm |

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| (none: Techguns has no quests) | One BetterQuesting line of 121 quests | BetterQuesting 3.8.89-GTNH | [authoring.md](authoring.md) |
| Hostile NPC factions, bosses ([10](../10-entities-npcs/index.md)) | `bq_standard:hunt` on `gregguns.<Name>` | BetterQuesting | [quest-line.md](quest-line.md#npcs-and-bosses) |
| Infernal variants ([infernal-mobs.md](../10-entities-npcs/infernal-mobs.md)) | Checkbox quests; a hunt cannot match "any infernal" | Infernal Mobs 1.10.7-GTNH | [quest-line.md](quest-line.md#npcs-and-bosses) |
| Structures and their chests ([11](../11-worldgen/index.md)) | Checkbox quests, one per structure group | TooMuchLoot, EnhancedLootBags | [quest-line.md](quest-line.md#structures) |
| Tier bag rewards ([pack-loot.md](../06-items/pack-loot.md#enhancedlootbags)) | `enhancedlootbags:lootbag` with the tier group's id as damage | EnhancedLootBags 1.3.5 | [quest-line.md](quest-line.md#reward-budget) |
| Tier map ([0001](../../design/0001-tier-map.md)) | One chapter per tier, gated on the pack's tier and material quests | GT progression quests | [Unlock design](#unlock-design) |
| Exact-tier charging ([0008](../../design/0008-exact-tier-charging.md)) | Taught in the HV, EV and ammo quests | GT battery buffers | [quest-line.md](quest-line.md#coverage-matrix) |
| Repair Bench, Turret ([08](../08-machines-production/index.md), [09](../09-machines-utility/index.md)) | A machines branch | GT multiblock framework | [quest-line.md](quest-line.md#machines) |
| Kill All the Things line | Same hunt pattern, separate tab, optional cross-link | BetterQuesting | [quest-line.md](quest-line.md#relation-to-the-packs-lines) |

## Delivered from the jar

Per [0015](../../design/0015-m9-self-contained-polish.md) nothing here is a change to the pack's repository. All paths are under `assets/gregguns/questbook/` in the jar, mirroring BetterQuesting 3.8.89-GTNH `DefaultQuests`. A jar cannot list a directory, so `index.json` names every file and the installer reads only those paths.

| Resource | Content | Sheet |
|---|---|---|
| `index.json` | `format`, the `line` path, `linePositions` (121 paths), `quests` (121 paths) and `orderEntry` (`D4B4PFfzWT2Q4DUgPjN9oQ==: GregGuns`, the entry the installer adds to the tab order) | [authoring.md](authoring.md#file-layout) |
| `QuestLines/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/QuestLine.json` | Line properties: name `GregGuns`, icon, `visibility` `UNLOCKED` | [authoring.md](authoring.md#line-files) |
| `QuestLines/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/<Name>-<id>.json` (121) | One position file per quest: `x`, `y`, `sizeX`, `sizeY` | [quest-line.md](quest-line.md) |
| `Quests/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/<Name>-<id>.json` (121) | One file per quest: properties, prerequisites, tasks, rewards | [authoring.md](authoring.md#quest-files) |

How the install behaves:

- **Hidden config key.** The installer, `compat/BetterQuestingInstall`, is built behind a hidden config key that is off by default and absent from the in-game config GUI. With it off nothing is installed and BetterQuesting is never touched.
- **Repeatable.** Quest and line IDs are stable UUIDs derived from the keys ([GGS-12-02](#requirements)), so installing twice, or after an update, replaces the same quests rather than adding copies.
- **No pack edits.** No `QuestSettings.json` bump, no `QuestLinesOrder.txt` edit, no `LootBags.xml` change. Quests only hand out existing bags ([pack-loot.md](../06-items/pack-loot.md#enhancedlootbags)).
- **Optional transition.** A one-quest cross link from the Kill All the Things line is left to a pack maintainer who wants it ([quest-line.md](quest-line.md#relation-to-the-packs-lines)).

[PACK-CHANGES.md](../PACK-CHANGES.md) and [01-core](../01-core/index.md) each have a one-line quest row; they point here.

## Requirements

- **GGS-12-01** The tab is one quest line named `GregGuns`, `visibility` `UNLOCKED`, added to the tab order after Kill All the Things (the `orderEntry` in `index.json`).
- **GGS-12-02** Every GregGuns quest and line ID is `uuid5(namespace 6f1f3c52-9d0a-4b6e-8a53-2c7e0f4b91d8, key)`, with the high and low halves written as signed longs and the file suffix as URL-safe Base64 of the 16 bytes. A key is never renamed after release; a retired quest is removed and its key is not reused.
- **GGS-12-03** `gg.start.welcome` unlocks when either pack quest 11 or pack quest 44 is complete (`questLogic` `OR`). Every other quest uses `AND`.
- **GGS-12-04** Every chapter intro lists the pack quests in the [Unlock design](#unlock-design) table, plus `gg.start.welcome`.
- **GGS-12-05** A prerequisite is a GregGuns key from the same file set, or a pack quest id from the [Referenced pack quests](#referenced-pack-quests) table. Nothing else.
- **GGS-12-06** Every prerequisite exists, and the prerequisite graph has no cycle.
- **GGS-12-07** A quest's tier is at most the highest pack tier in its prerequisite chain. A bag reward's tier is at most the quest's tier. A fixed item reward belongs to that tier or lower under [0001](../../design/0001-tier-map.md).
- **GGS-12-08** A bag reward is `enhancedlootbags:lootbag` with the tier group's `GroupMetaID` as damage. No command reward is used.
- **GGS-12-09** Items made in a GT machine are `retrieval` tasks with `consume` off. `crafting` tasks appear only in the Steam chapter.
- **GGS-12-10** Hunt tasks use `gregguns.<Name>` entity names with `subtypes` off, and every name is registered by GregGuns.
- **GGS-12-11** Infernal rank quests use a checkbox, not a hunt.
- **GGS-12-12** Structure quests use a checkbox. No `location` task names a GregGuns structure.
- **GGS-12-13** Only the three config-gated boss quests (Dictator Dave, Cyberdemon, Alien Bug) are `SECRET`.
- **GGS-12-14** Quest text is inline (`name:8`, `desc:8`), uses only the pack's `[warn]`, `[note]` and `[url]` tags, and teaches one mechanic per quest.
- **GGS-12-15** No two position boxes in the line overlap. Chapter heads are 28 units, others 24.
- **GGS-12-16** Every `gregguns:` item id and `gregguns.<Name>` entity name in the files is a name the mod registers, and every GregGuns machine is a `gregtech:gt.blockmachines` stack with one of the reserved MTE ids. `QuestbookResourcesTest` checks them against `NeiOrder`, `NpcMob` and `GregGunsMTEIDs`. No placeholder name is left.
- **GGS-12-17** The Repair Bench and Turret quests carry a provisional tier and are revisited when [09](../09-machines-utility/index.md) settles them.
- **GGS-12-18** The quest line ships in the GregGuns jar and is installed only when the hidden config key is on, so a pack without GregGuns, or with the key off, has no GregGuns tab and no dangling quests. The line and its quests have fixed IDs, so a later installer step can remove exactly them.

## Tests

| Requirement | Verified by |
|---|---|
| GGS-12-01, 03, 04, 05, 06, 15 | `QuestbookResourcesTest` (index lists every file, prerequisites resolve to a GregGuns quest or a pack id in the [Referenced pack quests](#referenced-pack-quests) table, no cycle, no overlapping boxes). `tools/questbook/validate_quests.py` runs the same checks on the files, and on a full `DefaultQuests` folder with `--only GregGuns-` |
| GGS-12-02 | `QuestbookResourcesTest` recomputes uuid5 of every key in `tools/questbook/quests.json` and compares it to the resources. Rebuilding with `build_quests.py` gives byte-identical files |
| GGS-12-07, 08, 09, 11, 12, 13 | Build-time checks in `build_quests.py` (bag tier at most the quest tier, crafting only in the Steam chapter, SECRET only on the three bosses, OR only on the welcome quest). A review confirms the table in [quest-line.md](quest-line.md) matches the data |
| GGS-12-10, 16 | `QuestbookResourcesTest` against `NeiOrder.names()`, `NpcMob` and `GregGunsMTEIDs` |
| GGS-12-14 | Manual review against the checklist in [authoring.md](authoring.md#checklist) |
| GGS-12-17 | Review when the 09 open questions close |
| GGS-12-18 | Installer tests (key off installs nothing) and a boot of a dev pack without BetterQuesting |
| All | A full playthrough of every tier gate in a GTNH dev pack with the key on, on a fresh world and on an old one |

## Related areas

- [06-items](../06-items/pack-loot.md): the tier bags and the loot tables the structure quests point at.
- [10-entities-npcs](../10-entities-npcs/index.md): entity names and Infernal behavior used by the NPC branch.
- [11-worldgen](../11-worldgen/index.md): structures and their loot ceilings.
- [08-machines-production](../08-machines-production/index.md) and [09-machines-utility](../09-machines-utility/index.md): the machines branch.
- [05-armor](../05-armor/index.md): the equipment branch.
- [01-core](../01-core/index.md): its one-line quest row.

## Open questions

1. **Infernal hunts.** A hunt cannot match "any infernal mob", so the three rank quests are honor-system checkboxes. A small GregGuns-registered BetterQuesting task (soft dependency on Infernal Mobs, reading `getIsRareEntity` and `getModSize`) would make them real. Needs a decision before M7.
2. **Structure visits.** Checkbox quests stand in for a visit check. If GregGuns registers its structures with a map-gen structure id, `location` tasks could name them. Needs the M8 structure design.
3. **Repair Bench and Turret tier.** The quests assume MV and HV. Settled by area 09 and a record on machines.
4. **Reward balance.** 26 bags may be too many or too few. The M9 balance pass decides.
5. **Bag content.** Bags draw random items from the whole tier group. Whether to add a GregGuns-only bag group is a server-owner step; it needs a new `LootBags.xml` group.
6. **Hunting with GregGuns damage.** `damageType` can require a damage source name. Whether GregGuns kills should be required (for example "kill with a bullet") depends on the names fixed in [0004](../../design/0004-damage-model.md).
7. **Translation.** The pack ships English inline. Whether GregGuns lang files should carry `betterquesting.quest.<id>.name` keys is a translation-readiness question for M9.
8. **Aluminum gate.** No pack quest is solely about aluminum. The MV gate relies on 1148 depending on 103. A direct prerequisite on 103 is optional.
