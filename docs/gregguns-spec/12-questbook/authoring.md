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
| `isMain:1` | 1 on the spine a player must follow (`main: true`): a main quest draws its incoming lines 8 units thick instead of 4 |
| `questLogic:8` | Prerequisite logic, `AND` unless the data sets `logic` |
| `taskLogic:8` | Task logic: `AND` (every task) by default, `OR` (any one task) when the data sets `taskLogic`. Alternatives inside one task use a retrieval with `requireOnlyOneItem` |
| `visibility:8` | `NORMAL`; `SECRET` for the three boss quests. The other values are `HIDDEN`, `UNLOCKED`, `COMPLETED`, `CHAIN`, `ALWAYS` ([EnumQuestVisibility](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api/enums/EnumQuestVisibility.java#L3-L11)) |
| `autoClaim:1`, `repeatTime:3` | 0 and -1: claim by hand, never repeat; `repeatTime` in ticks for a repeatable quest |
| `snd_complete:8`, `completion_particle:8` | `random.levelup` and `default`; chapter summaries may use `confetti` |

### Tasks

| `taskID:8` | JSON | Used for |
|---|---|---|
| `bq_standard:checkbox` | `{"index:3": 0, "taskID:8": "bq_standard:checkbox"}` | `gg.start.welcome` only (GGS-12-11) |
| `bq_standard:retrieval` | `requiredItems:9` (stacks), `requireOnlyOneItem:1` (1 = any one of the stacks), `consume:1` (0), `ignoreNBT:1` (1, or 0 to match a stack's NBT: the Nuclear Warhead quest uses `matchNBT` and a stack `tag` in the data), `partialMatch:1` (1), `autoConsume:1`, `groupDetect:1` | Anything made in a machine or found |
| `bq_standard:crafting` | `requiredItems:9`, `allowCraft:1`, `allowSmelt:1`, `allowAnvil:1`, `ignoreNBT:1`, `partialMatch:1`. Listens for the crafting table, furnace and anvil only ([TaskCrafting#L189-L200](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/tasks/TaskCrafting.java#L189-L200)) | Items made in the crafting grid |
| `bq_standard:optional_retrieval` | As `retrieval`. Its `ignored()` is true, so it never gates the quest ([TaskOptionalRetrieval](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/tasks/TaskOptionalRetrieval.java)); the periodic update counts it as done, so a quest with `taskLogic` `OR` and an optional task completes by itself | Recipe inputs beside the required item |
| `bq_standard:fluid` | `requiredFluids:9` (`Amount:3`, `FluidName:8`), `consume:1`, `ignoreNBT:1`, `autoConsume:1`, `groupDetect:1` | Fluids in cells |
| `bq_standard:hunt` | `target:8` (an `EntityList` name), `required:3`, `subtypes:1` (0 = exact class), `ignoreNBT:1` (1), `targetNBT:10`, `damageType:8` (empty = any) | NPC branch |
| `bq_standard:location` | `dimension:3`, `posX/Y/Z:3`, `range:3`, `structure:8`, `biome:3`, `name:8`, `invert:1`, `taxiCabDist:1`, `hideInfo:1`, `visible:1` | "Visit dimension N" (range -1), as the pack's 34 uses |

Stack item names are registry names. Guns and ammo use damage 0. A stack inside `requiredItems:9` uses the shape `{"Count:3": 1, "Damage:2": 0, "OreDict:8": "", "id:8": "gregguns:pistol_9mm"}`. A non-empty `OreDict:8` makes the task accept any item in that OreDict entry as well as the stack itself ([TaskRetrieval#L216-L219](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/tasks/TaskRetrieval.java#L216-L219)). The pack writes such a stack with a real item and damage, which the task shows, plus the OreDict name. `Damage:2` 32767 is the wildcard: any damage of that item.

### Rewards

| `rewardID:8` | JSON | Used for |
|---|---|---|
| `bq_standard:item` | `rewards:9` is a list of stacks, all given | Ammo, magazines, food, loot bags |
| `bq_standard:choice` | `choices:9` is a list of stacks, the player picks one ([RewardChoice](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/rewards/RewardChoice.java)) | A pick between rewards. Present in the pack (976 times) |
| `bq_standard:xp` | `amount:3`, `isLevels:1` ([RewardXP](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/rewards/RewardXP.java)) | Experience. Present in the pack (twice) |
| `bq_standard:questcompletion` | `questID:8` (Base64 ID) | The hidden tier-skip triggers |
| `bq_standard:command` | `command:8`, `hideCommand:1`, `viaPlayer:1`. `VAR_NAME` and `VAR_UUID` are replaced ([RewardCommand#L54-L70](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/rewards/RewardCommand.java#L54-L70)) | Not used (GGS-12-08). Keys are from source; the pack has no example |

A loot bag is a stack of `enhancedlootbags:lootbag` with the group id as `Damage:2`. The quests hand out the GregGuns groups, which GregGuns adds to EnhancedLootBags at startup ([pack-loot.md](../06-items/pack-loot.md#enhancedlootbags)). The `BAGS` table in the builder names them ([Quest data](#quest-data)); "lowest tier" is the lowest quest tier that may pay the bag (GGS-12-07):

| Name | `Damage:2` | Group | Lowest tier |
|---|---|---|---|
| `steam` | 1701 | Recruit | Steam |
| `lv` | 1702 | Rifleman | LV |
| `mv` | 1703 | Sergeant | MV |
| `hv` | 1704 | Lieutenant | HV |
| `ev` | 1705 | Captain | EV |
| `iv` | 1706 | Major | IV |
| `luv` | 1707 | Colonel | LuV |
| `zpm` | 1708 | General | ZPM |
| `spoils` | 1709 | Spoils of War (boss kills) | HV |
| `ordnance1` | 1710 | Ordnance I (ICBM) | HV |
| `ordnance2` | 1711 | Ordnance II (ICBM) | IV |
| `ordnance3` | 1712 | Ordnance III (ICBM) | LuV |

Group 1700 is reserved and unused. Bags hold GregGuns items and the precursors GregGuns recipes consume; food is a separate `item` reward (Pam's HarvestCraft), never in a bag. A hand-written stack of one of the pack's generic tier groups (damage 2, 4, 5, 6, 7, 8, 41, 42) still passes the tier check, with a warning to use a bag name.

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
- **Tags.** `[warn]…[/warn]` (red), `[note]…[/note]` (aqua, for a postscript), `[quest]…[/quest]` (green and underlined, for a quest or tab name, as the pack writes them) and `[url]…[/url]` (blue link) ([FormattingTag](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api2/client/gui/panels/content/FormattingTag.java#L16-L49)). Use `[warn]` for explosives and nuclear items. Titles carry their tier's color and bold, which the builder adds; in the text, `§l…§r` may highlight a key number or name (always closed with `§r`).
- **Length.** Prose in several paragraphs, the way the pack's quests read. In the data, `desc` is a list of paragraphs; in the quest file they are joined with `\n\n`. The Hint column in [quest-line.md](quest-line.md) is the first paragraph.
- **Icon.** The first task item. A chapter intro uses the chapter icon: `gregguns:pistol_9mm` (Start, LV), `handcannon` (Steam), `thompson_smg` (MV), `lmg` (HV), `laser_rifle` (EV), `blaster_rifle` (IV), `alien_blaster` (LuV), `nuclear_death_ray` (ZPM), `npc_egg` (NPCs), `weapon_chest` (Structures), the Repair Bench machine stack (Machines), `gas_mask` (Equipment), `ammo_9mm_bullet` (Ammo). A GregGuns machine is `gregtech:gt.blockmachines` with its MTE id as `Damage:2`.
- **Translations.** BetterQuesting looks for `betterquesting.quest.<id>.name` and `.desc` before the inline text. `<id>` is the Base64 ID with the trailing `=` removed. `/bq_admin default exportlang` writes a `template.lang` with every key, escaping `%` as `%%` and newlines as `%n` ([QuestCommandDefaults#L570-L612](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L570-L612)), and the pack's own quest translations (`config/txloader/load/betterquesting/lang/*.lang`) write paragraph breaks as `%n%n`. `build_quests.py --lang` writes the same encoding into the generated block of `en_US.lang` ([translating.md](../../translating.md)).

## Builder and validator

All in `tools/questbook/`, Python 3, standard library only. Run them with `python -I` so no stray module on the path is picked up.

| File | Role |
|---|---|
| `quests/` | The quest data, the single source of truth. `_line.json` holds the header (`format`, the uuid5 `namespace`, the `line`, `retired`); each `<chapter>.json` holds `{"chapter": "<chapter>", "retired": [...], "quests": [...]}`, one object per quest. The file name is the chapter, so writers on different chapters never touch the same file |
| `build_quests.py` | Reads the data, checks it, and writes the resource tree, `index.json` included. `BAGS` in it is the loot bag table, `TIER_COLORS` the title colors |
| `validate_quests.py` | Checks a quest folder: the in-jar tree or a pack `DefaultQuests`. With `--data`, also the data against the tree |
| `pack_items.txt` | Every pack item id a quest may name besides `gregguns:` ones: the ids the pack's own `DefaultQuests` and `LootBags.xml` use, and the HarvestCraft foods. Regenerate it with `pack_items.py <pack .minecraft>` when the pack moves |
| `render_preview.py` | Draws the line to a PNG (dev only, needs Pillow) |

### Quest data

A quest in `quests/lv.json`, in the pack's reward shape (a choice with a bag as one option, plus coins):

```json
{
  "key": "gg.lv.pistol",
  "title": "9mm Pistol",
  "tier": "LV",
  "main": true,
  "desc": [
    "Your first real sidearm. ...",
    "It's an LV weapon with a steel gate. ... It fires the rounds from [questlink]gg.lv.ammo_9mm[/questlink].",
    "[note]Press your reload key to swap magazines.[/note]"
  ],
  "pre": ["gg.lv.ammo_9mm"],
  "tasks": [
    {"type": "optional_retrieval", "items": [{"id": "gregtech:gt.metaitem.01", "damage": 17305, "ore": "plateSteel", "count": 4}]},
    {"type": "retrieval", "items": [{"id": "gregguns:pistol_9mm"}]}
  ],
  "rewards": [
    {"type": "choice", "items": [{"id": "gregguns:ammo_9mm_bullet", "count": 64}, {"id": "gregguns:mag_9mm_pistol", "count": 2}, {"bag": "lv"}]},
    {"type": "item", "items": [{"id": "dreamcraft:CoinSmith", "count": 30}, {"id": "harvestcraft:hamburgerItem", "count": 2}]}
  ],
  "x": 276, "y": 264, "size": 24,
  "icon": {"id": "gregguns:pistol_9mm"}
}
```

| Field | Meaning |
|---|---|
| `key` | `gg.<chapter>.<name>`. The id is uuid5 of it, so it never changes after release (GGS-12-02) |
| `title` | The plain title. The builder puts the tier color and bold in front (`§2§l` for LV) unless the title starts with its own `§` code; hidden triggers stay plain |
| `desc` | A string, or a list of paragraphs that the builder joins with a blank line. Tags: `[note]`, `[warn]`, `[quest]`, `[url]`, `[questlink]` (GGS-12-14). `[questlink]gg.lv.pistol[/questlink]` (or `[questlink]gg.lv.pistol the pistol[/questlink]` for other link text) names a quest key, or a pack quest number; the builder writes its id in place, and an unknown key fails the build |
| `tier` | `Steam`, `LV`, `MV`, `HV`, `EV`, `IV`, `LuV`, `ZPM`. Caps the bag tier (GGS-12-07) and picks the title color |
| `pre`, `implicit`, `logic` | Prerequisites: GregGuns keys (strings) and pack quest numbers (integers, `High = 0`). `implicit` is the subset drawn as BetterQuesting implicit lines. `logic` is the prerequisite logic, `AND` (default) or `OR` (GGS-12-03) |
| `tasks` | One or more tasks, below |
| `taskLogic` | `AND` (default): every task must be done. `OR`: any one task completes the quest. `OR` needs at least two tasks and no `optional_retrieval` |
| `rewards` | Zero or more rewards, below. Each becomes its own BetterQuesting reward, in order |
| `x`, `y`, `size`, `main` | Layout ([README](../../../tools/questbook/README.md#layout-and-prerequisites)). `size` is 24, or 48 for a main quest or chapter head, 72 for a tier milestone (28 for the older chapter heads). `main: true` marks the spine (thick incoming lines, jagged edge); the default is `size > 24` |
| `icon` | A stack |
| `repeatTime` | Optional: the quest repeats after this many ticks (the pack's repeatable hunts use 432000, six hours) |
| `visibility` | `NORMAL` (default); `SECRET` only on the three bosses (GGS-12-13) |
| `skip`, `completes` | Hidden skip triggers only ([quest-line.md](quest-line.md#hidden-tier-skip-triggers)): `completes` lists the keys the trigger completes, after any normal rewards |
| `_anything` | A writer's note. Fields that start with `_` are ignored; any other unknown field is an error |

Tasks (`type`):

| Type | Fields | BetterQuesting |
|---|---|---|
| `retrieval` | `items` (stacks); `any` true: any one of the items (OR within the task), otherwise all of them (AND); `matchNBT` true compares the stacks' `tag` | `bq_standard:retrieval`, `requireOnlyOneItem:1` = `any`, `consume:1` 0 |
| `optional_retrieval` | as `retrieval` | `bq_standard:optional_retrieval`: lists items (recipe inputs) without gating the quest. Never with `taskLogic` `OR` |
| `crafting` | `items`, `matchNBT` | `bq_standard:crafting`; crafting grid, furnace and anvil only. NBT is ignored unless `matchNBT` is true |
| `hunt` | `target` (`gregguns.<Name>` or `Zombie`), `count` | `bq_standard:hunt` |
| `fluid` | `fluids`: a list of `{"name": "<fluid registry name>", "amount": <mB>}` | `bq_standard:fluid`: fluid in containers (cells, buckets) in the inventory, `consume:1` 0 |
| `location` | `dimension` (id), `name` (shown) | `bq_standard:location`, dimension only (range -1), the one way the pack uses it |
| `checkbox` | none | `bq_standard:checkbox`; only on `gg.start.welcome` (GGS-12-11) |

"One of this family" is one `retrieval` with `any`. "All these parts" is one `retrieval` listing every part. "Any one of these sets" is several tasks with `taskLogic` `OR`. "Here is what it takes" is an `optional_retrieval` of the inputs before the required `retrieval`.

Rewards (`type`):

| Type | Fields | BetterQuesting |
|---|---|---|
| `item` | `items` (stacks), all given | `bq_standard:item` |
| `choice` | `items` (two or more stacks); the player picks one. When a player claims without choosing, BetterQuesting prefers the loot bag | `bq_standard:choice` |
| `bag` | `tier` (a `BAGS` name), `count` (default 1) | `bq_standard:item` with the bag stack the table names |
| `xp` | `amount`; `levels` (default true: levels, false: points) | `bq_standard:xp` |

Stacks: `id`, `count` (default 1), `damage` (default 0; `"*"` matches every damage value, written 32767), `ore` (an OreDict name: any item in it matches as well, the way the pack asks for "any steel plate"; keep a real `id` and `damage` for the item the task shows), `tag` (string NBT, with `matchNBT`). A stack `{"bag": "<tier>", "count": N}` is a bag inside any reward list; writers never write the bag item's id. An id other than `gregguns:` must be in `pack_items.txt` (coins such as `dreamcraft:CoinSmith`, Galacticraft parts, GT materials, HarvestCraft food).

**Loot bags.** `BAGS` in `build_quests.py` is the one table that maps a bag name (`steam`, `lv`, `mv`, `hv`, `ev`, `iv`, `luv`, `zpm`, `spoils`, `ordnance1`, `ordnance2`, `ordnance3`; case-insensitive) to an item id, a damage and the lowest quest tier that may pay it (the [table above](#rewards)). Pointing a name at another group, or adding a name, is a change to that table alone.

**Retired quests.** To remove a quest, delete it from its chapter file and add its key to that file's `retired` list (or to `_line.json`). The builder writes the uuid5 of every retired key to `index.json` (`retired`, UUID strings), and the installer deletes exactly those quests from an existing database: the definition, everyone's progress on it, its line entries, and its place in other quests' prerequisites. A retired key is never reused (GGS-12-02); the builder and `QuestbookResourcesTest` reject a retired key that is also a quest.

Everything else (the forty exporter properties, the position files, the line file) comes from the builder's defaults.

Rebuild after any change to the data, then run the test:

```
python -I tools/questbook/build_quests.py --out src/main/resources/assets/gregguns/questbook --lang src/main/resources/assets/gregguns/lang/en_US.lang
python -I tools/questbook/validate_quests.py src/main/resources/assets/gregguns/questbook --data tools/questbook/quests
```

The builder deletes `QuestLines/` and `Quests/` under `--out` and rewrites them, so a removed quest leaves no stale file. The output is a pure function of the data: ids are uuid5 of the keys, keys are sorted, no timestamps, `\n` line endings. Two builds are byte-identical (GGS-12-02). Before it writes anything the builder checks the rules the file shape cannot show: unknown fields, a bag never above its quest's tier, known bag names, pack item ids, a checkbox only on the welcome quest, `any` and `choice` with at least two items, `taskLogic` `OR` with at least two tasks and no optional task, box sizes, description tags and `[questlink]` keys, `SECRET` only on the three bosses, retired keys never reused, and every GregGuns prerequisite key existing.

The validator checks that every file name matches the ID inside it, every prerequisite exists (pack quests have `High = 0` and are accepted), there is no cycle, every line-entry file points at a quest, boxes do not overlap, `index.json` lists exactly the files on disk, and every line in `QuestLinesOrder.txt` exists when that file is present. With `--registry` (one item name per line) every `gregguns:` item resolves; with `--entities` (one `EntityList` name per line) every hunt target resolves. `--only GregGuns-` limits the report to the GregGuns folders, which matters on a whole pack: the unmodified reference pack reports 20 findings of its own (3 missing prerequisites in the `Oh Magic Armor` quests and 17 cycle reports, mostly the `The Choice` quests).

## Rebuild and test

1. **Edit** the chapter file in `tools/questbook/quests/`. Keep a key stable once released: a removed quest goes in a `retired` list and its key is never reused (GGS-12-02).
2. **Rebuild** with the two commands above, and update the tables in [quest-line.md](quest-line.md) and the quest count where it is stated.
3. **Unit test.** `QuestbookResourcesTest` loads the resources from the classpath and checks the index, the ids, the prerequisites (against the [Referenced pack quests](index.md#referenced-pack-quests) table), cycles, registered item, entity and machine names, and overlapping boxes. It fails with "resources are stale" if the data and the resources disagree.
4. **In game** (batched client pass). Turn the hidden install key on in a dev pack with BetterQuesting, start a new world and check the tab order, the layout and every icon. Nudge positions in edit mode (`/bq_admin edit`). With `/bq_admin complete`, complete the pack quests in the [Unlock design](index.md#unlock-design) table in turn and confirm each chapter intro appears only then. Obtain each item with `/give` and confirm the retrieval completes; kill each hunt target from its spawn egg and confirm the count rises; claim a bag and an item reward in survival.
5. **Warning.** `/bq_admin default save` **deletes the whole `DefaultQuests` folder before rewriting it** ([QuestCommandDefaults#L166-L196](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L166-L196)). Only run it in a world that was loaded from the full default set.

## Checklist

- [ ] Every quest in `quests/` has a Quests file and a position file with the same name, and [quest-line.md](quest-line.md) matches the data.
- [ ] IDs come from the builder; no hand-picked UUIDs.
- [ ] `index.json` lists every file; the installer adds the line last in the tab order.
- [ ] Every `gregguns:` item, `gregguns.` entity and machine id is registered (`QuestbookResourcesTest`); no placeholder left.
- [ ] Validator: 0 findings.
- [ ] Every bag's tier is at most its quest's tier. Every quest teaches one mechanic.
- [ ] No checkbox except `gg.start.welcome`; every other quest asks for something the game can check.
- [ ] A removed quest's key is in a `retired` list, and no retired key is reused.
- [ ] `crafting` only for grid, furnace or anvil items. No command rewards. No `taskLogic` `OR` with an `optional_retrieval`.
- [ ] Only the three boss quests are `SECRET`.
- [ ] The Repair Bench and Turret tiers match area 09.
- [ ] Text uses only `[warn]`, `[note]`, `[quest]`, `[url]`, `[questlink]`; hazards and nuclear items carry a `[warn]`.
- [ ] Tested in game with the install key on, on a new world and on an old one.
