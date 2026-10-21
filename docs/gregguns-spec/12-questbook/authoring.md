# Authoring the GregGuns quest line

How the tables in [quest-line.md](quest-line.md) become the resource files in the GregGuns jar, and how to rebuild and test them. The files are in BetterQuesting's own format, so a pack maintainer could also copy them into `DefaultQuests`, but the supported route is the in-jar install ([0015](../../design/0015-m9-self-contained-polish.md)).

- **Techguns baseline:** none (see [index.md](index.md)).
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0007](../../design/0007-pack-loot-and-infernal-mobs.md)
- **Pack mods:** BetterQuesting (`betterquesting`, `bq_standard`, 3.8.89-GTNH), EnhancedLootBags (`enhancedlootbags`, 1.3.5)
- **Roadmap:** M9
- **Code:** `tools/questbook/` (data, builder, validator), `src/main/resources/assets/gregguns/questbook/` (the generated files), `QuestbookResourcesTest`

Evidence: the pack's files in `reference/gtnh/2.9.0-RC-2/.minecraft/config/betterquesting/DefaultQuests/`, and BetterQuesting source at tag `3.8.89-GTNH`. The file format is the pack's real one; both scripts in `tools/questbook/` were run against it.

## File layout

All paths are under `config/betterquesting/DefaultQuests/` in the pack. In the jar the same tree sits under `assets/gregguns/questbook/`, without `QuestLinesOrder.txt` and `QuestSettings.json`, plus an `index.json` that lists every file because a jar cannot list a directory:

```json
{"format": 1, "line": "QuestLines/GregGuns-<b64>/QuestLine.json", "linePositions": ["QuestLines/GregGuns-<b64>/<Name>-<id>.json"], "quests": ["Quests/GregGuns-<b64>/<Name>-<id>.json"], "orderEntry": "<b64>: GregGuns"}
```

```
QuestLinesOrder.txt                        one line per tab: <line id>: <label>
QuestSettings.json                         pack name and version, edit mode, lives
QuestLines/<LineName>-<line id>/
    QuestLine.json                         the tab: name, description, icon, visibility
    <QuestName>-<quest id>.json            one per quest: position and size on the canvas
Quests/<LineName>-<line id>/
    <QuestName>-<quest id>.json            one per quest: everything about the quest
```

- The exporter puts quests that belong to one line in that line's folder, quests in several lines in `MultipleQuestLine/` and quests in none in `NoQuestLine/`. The loader reads every file under `Quests/` and takes the ID from the file body, so the folder is for humans ([QuestCommandDefaults#L69-L82](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L69-L82), [#L444-L456](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L444-L456)).
- **File name** is the name with color codes and non-alphanumerics removed, cut to 16 characters, then `-`, then the ID in Base64 ([#L168-L177](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L168-L177)). `Underground Geodes` becomes `UndergroundGeode-w00gbWZ4QG60V0neazNERw==.json`. The line entry and the quest file of one quest have the same file name.
- **Tab order** is the order of lines in `QuestLinesOrder.txt`. Only the text before the first colon is read ([#L429](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L429)).
- The jar carries only GregGuns files and edits nothing in the pack. The installer adds `orderEntry` to the tab order itself ([index.md](index.md#delivered-from-the-jar)).

## IDs

A BetterQuesting ID is a Java `UUID`. In JSON it is two signed 64-bit longs; in a file name it is the 16 bytes, high half first, as URL-safe Base64.

GregGuns derives every ID from its key, so the tables stay the single source of truth and a rebuild yields the same files:

```
id = uuid5(6f1f3c52-9d0a-4b6e-8a53-2c7e0f4b91d8, key)
```

The line's key is `line:gregguns`; a quest's key is its `gg.<chapter>.<name>` key. `qid()` in the [builder](#builder-and-validator) does this.

| Key | High | Low | Base64 |
|---|---|---|---|
| `line:gregguns` | 1117024908156819773 | -8007341724865954399 | `D4B4PFfzWT2Q4DUgPjN9oQ==` |
| `gg.lv.ammo_9mm` | 3692861790055784130 | -7779398022041441911 | `Mz-uO4DjXsKUCga3SdfpiQ==` |
| `gg.lv.pistol` | -7452196324363907060 | -6634400740834056797 | `mJR67SnRUAyj7d-tQP9low==` |

Pack quests are referenced by `High = 0` and `Low = <number>`, for example `{"questIDHigh:4": 0, "questIDLow:4": 88}` for Welcome to Tier 1, LV.

## JSON conventions

BetterQuesting writes NBT as JSON. Every key carries a type suffix, and a list is an object keyed `"0:10"`, `"1:10"`, …

| Suffix | Type | Suffix | Type |
|---|---|---|---|
| `:1` | byte (boolean, 0 or 1) | `:6` | double |
| `:2` | short | `:8` | string |
| `:3` | int | `:9` | list |
| `:4` | long | `:10` | compound |
| `:5` | float | | |

The exporter sorts keys, indents two spaces, and writes `'` as `'` and `=` as `=`. The loader accepts any valid JSON, so hand-written files work; a later `/bq_admin default save` normalizes them.

## Quest files

Top-level keys of `Quests/…/<file>.json`:

| Key | Meaning |
|---|---|
| `questIDHigh:4`, `questIDLow:4` | The quest ID |
| `preRequisites:9` | List of `{questIDHigh:4, questIDLow:4}`. Combined with `properties.questLogic:8` (`AND`, `OR`, `XOR`) |
| `properties:10.betterquesting:10` | All the quest properties, below |
| `tasks:9` | Tasks, each with `index:3` and `taskID:8` |
| `rewards:9` | Rewards, each with `index:3` and `rewardID:8` |

Properties that matter (the exporter writes about forty; the builder writes all of them with the pack's defaults):

| Property | Use in GregGuns |
|---|---|
| `name:8`, `desc:8` | Title and text. Inline, with `\n` for line breaks |
| `icon:10` | An item stack (`id:8`, `Damage:2`, `Count:3`, `OreDict:8`) |
| `isMain:1` | 0 for every quest: a main quest draws its incoming lines 8 units thick instead of 4 |
| `questLogic:8` | Prerequisite logic. `OR` only on `gg.start.welcome` |
| `taskLogic:8` | Task logic, `AND` everywhere (alternatives use one retrieval with `requireOnlyOneItem`) |
| `visibility:8` | `NORMAL`; `SECRET` for the three boss quests. The other values are `HIDDEN`, `UNLOCKED`, `COMPLETED`, `CHAIN`, `ALWAYS` ([EnumQuestVisibility](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api/enums/EnumQuestVisibility.java#L3-L11)) |
| `autoClaim:1`, `repeatTime:3` | 0 and -1: claim by hand, never repeat |
| `snd_complete:8`, `completion_particle:8` | `random.levelup` and `default`; chapter summaries may use `confetti` |

### Tasks

| `taskID:8` | JSON | Used for |
|---|---|---|
| `bq_standard:checkbox` | `{"index:3": 0, "taskID:8": "bq_standard:checkbox"}` | Intros, summaries, honor-system lessons |
| `bq_standard:retrieval` | `requiredItems:9` (stacks), `requireOnlyOneItem:1` (1 = any one of the stacks), `consume:1` (0), `ignoreNBT:1` (1), `partialMatch:1` (1), `autoConsume:1`, `groupDetect:1` | Anything made in a machine or found |
| `bq_standard:crafting` | `requiredItems:9`, `allowCraft:1`, `allowSmelt:1`, `allowAnvil:1`, `ignoreNBT:1`, `partialMatch:1`. Listens for the crafting table, furnace and anvil only ([TaskCrafting#L189-L200](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/tasks/TaskCrafting.java#L189-L200)) | Steam-chapter quests |
| `bq_standard:hunt` | `target:8` (an `EntityList` name), `required:3`, `subtypes:1` (0 = exact class), `ignoreNBT:1` (1), `targetNBT:10`, `damageType:8` (empty = any) | NPC branch |
| `bq_standard:location` | `dimension:3`, `posX/Y/Z:3`, `range:3`, `structure:8`, `biome:3`, `name:8`, `invert:1`, `taxiCabDist:1`, `hideInfo:1`, `visible:1` | Not used. Present in the pack (34 times) |

Stack item names are registry names. Guns and ammo use damage 0. A stack inside `requiredItems:9` uses the shape `{"Count:3": 1, "Damage:2": 0, "OreDict:8": "", "id:8": "gregguns:pistol_9mm"}`.

### Rewards

| `rewardID:8` | JSON | Used for |
|---|---|---|
| `bq_standard:item` | `rewards:9` is a list of stacks, all given | Ammo, magazines, loot bags |
| `bq_standard:choice` | `choices:9` is a list of stacks, the player picks one | Not used in the first build. Present in the pack (976 times) |
| `bq_standard:questcompletion` | `questID:8` (Base64 ID) | Not used |
| `bq_standard:command` | `command:8`, `hideCommand:1`, `viaPlayer:1`. `VAR_NAME` and `VAR_UUID` are replaced ([RewardCommand#L54-L70](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/rewards/RewardCommand.java#L54-L70)) | Not used (GGS-12-08). Keys are from source; the pack has no example |

A loot bag is a stack of `enhancedlootbags:lootbag` with the tier group id as `Damage:2`:

| Tier | Steam | LV | MV | HV | EV | IV | LuV | ZPM |
|---|---|---|---|---|---|---|---|---|
| `Damage:2` | 2 | 4 | 5 | 6 | 7 | 8 | 41 | 42 |

### Line files

`QuestLine.json`:

```json
{
  "properties:10": {
    "betterquesting:10": {
      "bg_image:8": "",
      "bg_size:3": 256,
      "desc:8": "Guns that follow GregTech progression.",
      "icon:10": { "Count:3": 1, "Damage:2": 0, "OreDict:8": "", "id:8": "gregguns:pistol_9mm" },
      "name:8": "GregGuns",
      "visibility:8": "UNLOCKED"
    }
  },
  "questLineIDHigh:4": 1117024908156819773,
  "questLineIDLow:4": -8007341724865954399
}
```

A per-quest position file has the quest ID, a top-left `x:3`, `y:3` and `sizeX:3`, `sizeY:3` (24, 48 or 72).

## Worked example

The quest `gg.lv.pistol` (LV chapter, [quest-line.md](quest-line.md#lv)).

`Quests/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/9mmPistol-mJR67SnRUAyj7d-tQP9low==.json`

```json
{
  "preRequisites:9": {
    "0:10": {
      "questIDHigh:4": 3692861790055784130,
      "questIDLow:4": -7779398022041441911
    }
  },
  "properties:10": {
    "betterquesting:10": {
      "autoClaim:1": 0,
      "completion_animation:8": "default",
      "completion_particle:8": "default",
      "confetti_icon:10": {
        "Count:3": 0,
        "Damage:2": 0,
        "OreDict:8": "",
        "id:8": "minecraft:stick"
      },
      "countAsQuest:1": 1,
      "desc:8": "The 9mm Pistol is a magazine-fed sidearm. The reward is one magazine for it.\n\nIt is an LV item: the last recipe step is an LV Assembler, and steel is the gate material. NEI shows the recipe under the Assembler tab.\n\n[note]Press the reload key to swap magazines. Sneak-click a magazine to empty it.[/note]",
      "globalShare:1": 1,
      "icon:10": {
        "Count:3": 1,
        "Damage:2": 0,
        "OreDict:8": "",
        "id:8": "gregguns:pistol_9mm"
      },
      "isGlobal:1": 0,
      "isMain:1": 0,
      "isSilent:1": 0,
      "lockedProgress:1": 0,
      "name:8": "9mm Pistol",
      "notification_duration:5": -1.0,
      "notification_effect:3": -1,
      "notification_fade_in:5": -1.0,
      "notification_fade_out:5": -1.0,
      "notification_icon_offset_y:3": -2147483648,
      "notification_icon_scale:5": -1.0,
      "notification_pos_x:3": -2147483648,
      "notification_pos_y:3": -2147483648,
      "notification_show_icon:8": "default",
      "notification_style:8": "default",
      "notification_subtitle:8": "",
      "notification_subtitle_scale:5": -1.0,
      "notification_title:8": "",
      "notification_title_scale:5": -1.0,
      "particle_count:3": -1,
      "partySingleReward:1": 0,
      "questLogic:8": "AND",
      "repeatTime:3": -1,
      "repeat_relative:1": 1,
      "simultaneous:1": 0,
      "snd_complete:8": "random.levelup",
      "snd_update:8": "random.levelup",
      "taskLogic:8": "AND",
      "visibility:8": "NORMAL"
    }
  },
  "questIDHigh:4": -7452196324363907060,
  "questIDLow:4": -6634400740834056797,
  "rewards:9": {
    "0:10": {
      "ignoreDisabled:1": 0,
      "index:3": 0,
      "rewardID:8": "bq_standard:item",
      "rewards:9": {
        "0:10": {
          "Count:3": 1,
          "Damage:2": 0,
          "OreDict:8": "",
          "id:8": "gregguns:mag_9mm_pistol"
        }
      }
    }
  },
  "tasks:9": {
    "0:10": {
      "autoConsume:1": 0,
      "consume:1": 0,
      "groupDetect:1": 0,
      "ignoreNBT:1": 1,
      "index:3": 0,
      "partialMatch:1": 1,
      "requireOnlyOneItem:1": 0,
      "requiredItems:9": {
        "0:10": {
          "Count:3": 1,
          "Damage:2": 0,
          "OreDict:8": "",
          "id:8": "gregguns:pistol_9mm"
        }
      },
      "taskID:8": "bq_standard:retrieval"
    }
  }
}
```

Its position file, `QuestLines/GregGuns-D4B4PFfzWT2Q4DUgPjN9oQ==/9mmPistol-mJR67SnRUAyj7d-tQP9low==.json`:

```json
{
  "questIDHigh:4": -7452196324363907060,
  "questIDLow:4": -6634400740834056797,
  "sizeX:3": 24,
  "sizeY:3": 24,
  "x:3": 276,
  "y:3": 264
}
```

The line is added to `QuestLinesOrder.txt` as its last line:

```
D4B4PFfzWT2Q4DUgPjN9oQ==: GregGuns
```

What the example shows: one prerequisite (`gg.lv.ammo_9mm`, whose ID the builder derives); one `retrieval` task with `consume:1` = 0; one `bq_standard:item` reward of a single magazine; `visibility` `NORMAL`; the text kept in `desc:8`.

## Text and localization

- **Inline.** Write `name:8` and `desc:8` in the quest file, as the pack does. The pack Readme asks for information first; GregGuns quests say what to do, which machine or slot is involved, and what the reward is for.
- **Tags.** `[warn]…[/warn]` (red), `[note]…[/note]` (aqua, for a postscript) and `[url]…[/url]` (blue link). Use `[warn]` for explosives and nuclear items. Color codes (`§`) are used by some pack titles; GregGuns avoids them except on the chapter intros.
- **Length.** Two to four short paragraphs. The Hint column in [quest-line.md](quest-line.md) is the first paragraph.
- **Icon.** The first task item. A checkbox-only quest uses the chapter icon: `gregguns:pistol_9mm` (Start, LV), `handcannon` (Steam), `thompson_smg` (MV), `lmg` (HV), `laser_rifle` (EV), `blaster_rifle` (IV), `alien_blaster` (LuV), `nuclear_death_ray` (ZPM), `npc_egg` (NPCs), `weapon_chest` (Structures), the Repair Bench machine stack (Machines), `gas_mask` (Equipment), `ammo_9mm_bullet` (Ammo). A GregGuns machine is `gregtech:gt.blockmachines` with its MTE id as `Damage:2`.
- **Translations.** BetterQuesting looks for `betterquesting.quest.<id>.name` and `.desc` before the inline text. `<id>` is the Base64 ID with the trailing `=` removed. `/bq_admin default exportlang` writes a `template.lang` with every key, escaping `%` as `%%` and newlines as `%n` ([QuestCommandDefaults#L570-L612](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L570-L612)). The pack ships none today, so GregGuns ships none.

## Builder and validator

All in `tools/questbook/`, Python 3, standard library only. Run them with `python -I` so no stray module on the path is picked up.

| File | Role |
|---|---|
| `quests.json` | The quest data, the single source of truth. A header (`format`, the uuid5 `namespace`, the `line`) and one object per quest |
| `build_quests.py` | Reads the data and writes the resource tree, `index.json` included |
| `validate_quests.py` | Checks a quest folder: the in-jar tree or a pack `DefaultQuests` |

A quest in `quests.json`:

```json
{"key": "gg.lv.pistol", "chapter": "lv", "title": "9mm Pistol", "tier": "LV", "desc": "...", "pre": ["gg.lv.ammo_9mm", 97],
 "tasks": [{"type": "retrieval", "items": [{"id": "gregguns:pistol_9mm", "count": 1, "damage": 0}]}],
 "rewards": [{"type": "item", "items": [{"id": "gregguns:mag_9mm_pistol", "count": 1, "damage": 0}]}],
 "x": 276, "y": 264, "size": 24, "main": false, "icon": {"id": "gregguns:pistol_9mm", "count": 1, "damage": 0}}
```

- `pre` holds GregGuns keys (strings) and pack quest numbers (integers, written with `High = 0`). `logic` is `OR` only on the welcome quest. `visibility` is `SECRET` only on the three bosses.
- Task types are `checkbox`, `retrieval` (`any` true means any one of the items), `crafting` and `hunt` (`target`, `count`). A reward is `item` with a list of stacks. A bag is the `enhancedlootbags:lootbag` stack with the tier group as `damage`.
- Everything else (the forty exporter properties, the position files, the line file) comes from the builder's defaults.

Rebuild after any change to the data, then run the test:

```
python -I tools/questbook/build_quests.py --out src/main/resources/assets/gregguns/questbook
python -I tools/questbook/validate_quests.py src/main/resources/assets/gregguns/questbook
```

The builder deletes `QuestLines/` and `Quests/` under `--out` and rewrites them, so a removed quest leaves no stale file. The output is a pure function of the data: ids are uuid5 of the keys, keys are sorted, no timestamps, `\n` line endings. Two builds are byte-identical (GGS-12-02). Before it writes anything the builder checks the rules the file shape cannot show: a bag never above its quest's tier, `crafting` only in the Steam chapter, `SECRET` only on the three bosses, `OR` only on the welcome quest, and every GregGuns prerequisite key existing.

The validator checks that every file name matches the ID inside it, every prerequisite exists (pack quests have `High = 0` and are accepted), there is no cycle, every line-entry file points at a quest, boxes do not overlap, `index.json` lists exactly the files on disk, and every line in `QuestLinesOrder.txt` exists when that file is present. With `--registry` (one item name per line) every `gregguns:` item resolves; with `--entities` (one `EntityList` name per line) every hunt target resolves. `--only GregGuns-` limits the report to the GregGuns folders, which matters on a whole pack: the unmodified reference pack reports 20 findings of its own (3 missing prerequisites in the `Oh Magic Armor` quests and 17 cycle reports, mostly the `The Choice` quests).

## Rebuild and test

1. **Edit** `tools/questbook/quests.json`. Keep a key stable once released: a retired quest is removed and its key never reused (GGS-12-02).
2. **Rebuild** with the two commands above, and update the tables in [quest-line.md](quest-line.md) and the quest count where it is stated.
3. **Unit test.** `QuestbookResourcesTest` loads the resources from the classpath and checks the index, the ids, the prerequisites (against the [Referenced pack quests](index.md#referenced-pack-quests) table), cycles, registered item, entity and machine names, and overlapping boxes. It fails with "resources are stale" if the data and the resources disagree.
4. **In game** (batched client pass). Turn the hidden install key on in a dev pack with BetterQuesting, start a new world and check the tab order, the layout and every icon. Nudge positions in edit mode (`/bq_admin edit`). With `/bq_admin complete`, complete the pack quests in the [Unlock design](index.md#unlock-design) table in turn and confirm each chapter intro appears only then. Obtain each item with `/give` and confirm the retrieval completes; kill each hunt target from its spawn egg and confirm the count rises; claim a bag and an item reward in survival.
5. **Warning.** `/bq_admin default save` **deletes the whole `DefaultQuests` folder before rewriting it** ([QuestCommandDefaults#L166-L196](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L166-L196)). Only run it in a world that was loaded from the full default set.

## Checklist

- [ ] Every quest in `quests.json` has a Quests file and a position file with the same name, and [quest-line.md](quest-line.md) matches the data.
- [ ] IDs come from the builder; no hand-picked UUIDs.
- [ ] `index.json` lists every file; the installer adds the line last in the tab order.
- [ ] Every `gregguns:` item, `gregguns.` entity and machine id is registered (`QuestbookResourcesTest`); no placeholder left.
- [ ] Validator: 0 findings.
- [ ] Every bag's tier is at most its quest's tier. Every quest teaches one mechanic.
- [ ] Only the Steam chapter uses `crafting`. No command rewards.
- [ ] Only the three boss quests are `SECRET`.
- [ ] The Repair Bench and Turret tiers match area 09.
- [ ] Text uses only `[warn]`, `[note]`, `[url]`; hazards and nuclear items carry a `[warn]`.
- [ ] Tested in game with the install key on, on a new world and on an old one.
