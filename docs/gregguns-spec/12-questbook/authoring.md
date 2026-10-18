# Authoring the GregGuns quest line

How a pack maintainer turns the tables in [quest-line.md](quest-line.md) into files for GT-New-Horizons-Modpack, and how to test them.

- **Techguns baseline:** none (see [index.md](index.md)).
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0007](../../design/0007-pack-loot-and-infernal-mobs.md)
- **Pack mods:** BetterQuesting (`betterquesting`, `bq_standard`, 3.8.89-GTNH), EnhancedLootBags (`enhancedlootbags`, 1.3.5)
- **Roadmap:** M9

Evidence: the pack's files in `reference/gtnh/2.9.0-RC-2/.minecraft/config/betterquesting/DefaultQuests/`, and BetterQuesting source at tag `3.8.89-GTNH`. The file format is the pack's real one; both scripts below were run against it.

## File layout

All paths are under `config/betterquesting/DefaultQuests/`.

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
- **File name** is the name with colour codes and non-alphanumerics removed, cut to 16 characters, then `-`, then the ID in Base64 ([#L168-L177](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L168-L177)). `Underground Geodes` becomes `UndergroundGeode-w00gbWZ4QG60V0neazNERw==.json`. The line entry and the quest file of one quest have the same file name.
- **Tab order** is the order of lines in `QuestLinesOrder.txt`. Only the text before the first colon is read ([#L429](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L429)).
- The GregGuns files are the only new files; the sole edits to existing files are one appended line in `QuestLinesOrder.txt` and the `pack_version:3` bump in `QuestSettings.json` ([index.md](index.md#pack-side-changes)).

## IDs

A BetterQuesting ID is a Java `UUID`. In JSON it is two signed 64-bit longs; in a file name it is the 16 bytes, high half first, as URL-safe Base64.

GregGuns derives every ID from its key, so the tables stay the single source of truth and a rebuild yields the same files:

```
id = uuid5(6f1f3c52-9d0a-4b6e-8a53-2c7e0f4b91d8, key)
```

The line's key is `line:gregguns`; a quest's key is its `gg.<chapter>.<name>` key. `qid()` in the [builder](#builder-script) does this.

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

The exporter sorts keys, indents two spaces, and writes `'` as `'` and `=` as `=`. The loader accepts any valid JSON, so hand-written files work; a later `/bq_admin default save` normalises them.

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
| `isMain:1` | 1 for chapter intros and summaries |
| `questLogic:8` | Prerequisite logic. `OR` only on `gg.start.welcome` |
| `taskLogic:8` | Task logic, `AND` everywhere (alternatives use one retrieval with `requireOnlyOneItem`) |
| `visibility:8` | `NORMAL`; `SECRET` for the three boss quests. The other values are `HIDDEN`, `UNLOCKED`, `COMPLETED`, `CHAIN`, `ALWAYS` ([EnumQuestVisibility](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api/enums/EnumQuestVisibility.java#L3-L11)) |
| `autoClaim:1`, `repeatTime:3` | 0 and -1: claim by hand, never repeat |
| `snd_complete:8`, `completion_particle:8` | `random.levelup` and `default`; chapter summaries may use `confetti` |

### Tasks

| `taskID:8` | JSON | Used for |
|---|---|---|
| `bq_standard:checkbox` | `{"index:3": 0, "taskID:8": "bq_standard:checkbox"}` | Intros, summaries, honour-system lessons |
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

## Text and localisation

- **Inline.** Write `name:8` and `desc:8` in the quest file, as the pack does. The pack Readme asks for information first; GregGuns quests say what to do, which machine or slot is involved, and what the reward is for.
- **Tags.** `[warn]…[/warn]` (red), `[note]…[/note]` (aqua, for a postscript) and `[url]…[/url]` (blue link). Use `[warn]` for explosives and nuclear items. Colour codes (`§`) are used by some pack titles; GregGuns avoids them except on the chapter intros.
- **Length.** Two to four short paragraphs. The Hint column in [quest-line.md](quest-line.md) is the first paragraph.
- **Icon.** The first task item. A checkbox-only quest uses the chapter icon: `gregguns:pistol_9mm` (Start, LV), `hand_cannon` (Steam), `thompson` (MV), `lmg` (HV), `laser_rifle` (EV), `blaster_rifle` (IV), `alien_blaster` (LuV), `nuclear_death_ray` (ZPM), `spawn_egg` (NPCs), `weaponchest_t1` (Structures), `repair_bench` (Machines), `gas_mask` (Equipment), `ammo_9mm_bullet` (Ammo).
- **Translations.** BetterQuesting looks for `betterquesting.quest.<id>.name` and `.desc` before the inline text. `<id>` is the Base64 ID with the trailing `=` removed. `/bq_admin default exportlang` writes a `template.lang` with every key, escaping `%` as `%%` and newlines as `%n` ([QuestCommandDefaults#L570-L612](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L570-L612)). The pack ships none today, so GregGuns ships none.

## Builder script

This script builds the exact file shapes above. It was used to generate the worked example and a full 118-quest line that the validator accepts. Keep a copy in the pack tooling; it is not part of this repo.

```python
"""Reference builder for GregGuns BetterQuesting files (BQ 3.8.89-GTNH format)."""
import base64, json, re, struct, uuid

NS = uuid.UUID('6f1f3c52-9d0a-4b6e-8a53-2c7e0f4b91d8')  # fixed GregGuns namespace


def qid(key):
    """Quest or line key -> (high, low, base64) the way BetterQuesting stores a UUID."""
    b = uuid.uuid5(NS, key).bytes
    hi, lo = struct.unpack('>qq', b)
    return hi, lo, base64.urlsafe_b64encode(b).decode()


def file_name(title, b64):
    plain = re.sub('§.', '', title)
    return re.sub('[^a-zA-Z0-9]', '', plain)[:16] + '-' + b64 + '.json'


def stack(item, count=1, damage=0):
    return {'Count:3': count, 'Damage:2': damage, 'OreDict:8': '', 'id:8': item}


def pre(ref):
    if isinstance(ref, int):                 # pack quest: high 0, low = number
        return {'questIDHigh:4': 0, 'questIDLow:4': ref}
    hi, lo, _ = qid(ref)                     # GregGuns quest key
    return {'questIDHigh:4': hi, 'questIDLow:4': lo}


def retrieval(items, any_one=False, consume=False):
    return {'autoConsume:1': 0, 'consume:1': int(consume), 'groupDetect:1': 0, 'ignoreNBT:1': 1,
            'partialMatch:1': 1, 'requireOnlyOneItem:1': int(any_one),
            'requiredItems:9': {'%d:10' % i: s for i, s in enumerate(items)},
            'taskID:8': 'bq_standard:retrieval'}


def crafting(items):
    return {'allowAnvil:1': 0, 'allowCraft:1': 1, 'allowCraftedFromStatistics:1': 0,
            'allowSmelt:1': 1, 'ignoreNBT:1': 0, 'partialMatch:1': 1,
            'requiredItems:9': {'%d:10' % i: s for i, s in enumerate(items)},
            'taskID:8': 'bq_standard:crafting'}


def checkbox():
    return {'taskID:8': 'bq_standard:checkbox'}


def hunt(entity, n):
    return {'damageType:8': '', 'ignoreNBT:1': 1, 'required:3': n, 'subtypes:1': 0,
            'target:8': entity, 'targetNBT:10': {}, 'taskID:8': 'bq_standard:hunt'}


def reward_item(stacks):
    return {'ignoreDisabled:1': 0, 'rewardID:8': 'bq_standard:item',
            'rewards:9': {'%d:10' % i: s for i, s in enumerate(stacks)}}


def quest(key, title, desc, icon, prereqs, tasks, rewards, main=False, vis='NORMAL',
          logic='AND', task_logic='AND'):
    hi, lo, _ = qid(key)
    props = {  # every key the exporter writes, with the pack's defaults
        'autoClaim:1': 0, 'completion_animation:8': 'default', 'completion_particle:8': 'default',
        'confetti_icon:10': stack('minecraft:stick', 0), 'countAsQuest:1': 1, 'desc:8': desc,
        'globalShare:1': 1, 'icon:10': icon, 'isGlobal:1': 0, 'isMain:1': int(main),
        'isSilent:1': 0, 'lockedProgress:1': 0, 'name:8': title,
        'notification_duration:5': -1.0, 'notification_effect:3': -1, 'notification_fade_in:5': -1.0,
        'notification_fade_out:5': -1.0, 'notification_icon_offset_y:3': -2147483648,
        'notification_icon_scale:5': -1.0, 'notification_pos_x:3': -2147483648,
        'notification_pos_y:3': -2147483648, 'notification_show_icon:8': 'default',
        'notification_style:8': 'default', 'notification_subtitle:8': '',
        'notification_subtitle_scale:5': -1.0, 'notification_title:8': '',
        'notification_title_scale:5': -1.0, 'particle_count:3': -1, 'partySingleReward:1': 0,
        'questLogic:8': logic, 'repeatTime:3': -1, 'repeat_relative:1': 1, 'simultaneous:1': 0,
        'snd_complete:8': 'random.levelup', 'snd_update:8': 'random.levelup',
        'taskLogic:8': task_logic, 'visibility:8': vis}
    out = {'preRequisites:9': {'%d:10' % i: pre(p) for i, p in enumerate(prereqs)},
           'properties:10': {'betterquesting:10': props},
           'questIDHigh:4': hi, 'questIDLow:4': lo,
           'rewards:9': {}, 'tasks:9': {}}
    for i, t in enumerate(tasks):
        out['tasks:9']['%d:10' % i] = dict(t, **{'index:3': i})
    for i, r in enumerate(rewards):
        out['rewards:9']['%d:10' % i] = dict(r, **{'index:3': i})
    return out


def line_entry(key, x, y, size=24):
    hi, lo, _ = qid(key)
    return {'questIDHigh:4': hi, 'questIDLow:4': lo, 'sizeX:3': size, 'sizeY:3': size,
            'x:3': x, 'y:3': y}


def dump(obj):
    """Two-space JSON with sorted keys, like the exporter (which also escapes ' and =)."""
    text = json.dumps(obj, indent=2, sort_keys=True, ensure_ascii=False)
    return text.replace("'", '\\u0027').replace('=', '\\u003d')
```

A driver reads the tables in [quest-line.md](quest-line.md) (key, title, hint, tasks, rewards, prerequisites, layout) and writes, per quest, one file under `Quests/` and one under `QuestLines/`. Task text maps to builders: `checkbox` to `checkbox()`, `retrieval` to `retrieval(items, any_one=…)`, `crafting` to `crafting(items)`, `hunt` to `hunt(name, n)`. A `bag` reward is `reward_item([stack('enhancedlootbags:lootbag', 1, <meta>)])`. A prerequisite written `pack N` is the int `N`; a `gg.` key is passed as the string.

## Validator

`validate_quests.py` checks a `DefaultQuests` folder:

- every quest file name matches the ID inside it;
- every prerequisite exists and there is no cycle;
- every line-entry file points at a quest, and boxes in a line do not overlap;
- every line in `QuestLinesOrder.txt` exists;
- with `--registry` (one item name per line) every `gregguns:` item resolves;
- with `--entities` (one `EntityList` name per line) every hunt target resolves.

```
python -I validate_quests.py <DefaultQuests dir> --only GregGuns- [--registry items.txt] [--entities entities.txt]
```

`--only GregGuns-` limits the report to the GregGuns folders. Without it the unmodified reference pack reports 20 findings of its own (3 missing prerequisites in the `Oh Magic Armor` quests and 17 cycle reports, mostly the `The Choice` quests), so a whole-pack run is only a baseline. With the generated GregGuns line merged into a copy of the reference pack the validator reports 3931 quests, 47 lines and 0 GregGuns findings.

```python
"""Validate a BetterQuesting DefaultQuests folder.

usage: python -I validate_quests.py <DefaultQuests dir> [--only GregGuns-] [--registry registry.txt] [--entities entities.txt]

registry.txt: one item registry name per line (modid:name), dumped from a running client.
entities.txt: one EntityList string name per line.
--only: report only errors whose message contains this text (the shipped pack has older findings of its own).
Exit code 1 if any check fails.
"""
import base64, glob, json, os, re, struct, sys


def uid(hi, lo):
    return (hi, lo)


def decode(b64):
    hi, lo = struct.unpack('>qq', base64.urlsafe_b64decode(b64))
    return hi, lo


def load(path):
    with open(path, encoding='utf8') as fh:
        return json.load(fh)


def main(root, registry=None, entities=None, only=None):
    errors = []
    quests = {}
    rel = lambda p: os.path.relpath(p, root).replace(os.sep, '/')
    for path in glob.glob(os.path.join(root, 'Quests', '**', '*.json'), recursive=True):
        d = load(path)
        qid = (d['questIDHigh:4'], d['questIDLow:4'])
        if qid in quests:
            errors.append('duplicate quest id in %s' % rel(path))
        quests[qid] = (path, d)
        m = re.search(r'-([A-Za-z0-9_-]{22}==)\.json$', path)
        if not m or decode(m.group(1)) != qid:
            errors.append('file name does not match quest id: %s' % rel(path))
    # prerequisites exist, no cycles
    graph = {}
    for qid, (path, d) in quests.items():
        deps = [(p['questIDHigh:4'], p['questIDLow:4']) for p in d.get('preRequisites:9', {}).values()]
        for dep in deps:
            if dep not in quests:
                errors.append('missing prerequisite %s in %s' % (dep, rel(path)))
        graph[qid] = [x for x in deps if x in quests]
    state = {}
    def visit(n, stack):
        state[n] = 1
        for m in graph[n]:
            if state.get(m) == 1:
                errors.append('prerequisite cycle through %s' % rel(quests[m][0]))
            elif m not in state:
                visit(m, stack)
        state[n] = 2
    sys.setrecursionlimit(10000)
    for n in graph:
        if n not in state:
            visit(n, [])
    # lines
    lines = {}
    for qlpath in glob.glob(os.path.join(root, 'QuestLines', '*', 'QuestLine.json')):
        d = load(qlpath)
        lines[(d['questLineIDHigh:4'], d['questLineIDLow:4'])] = os.path.dirname(qlpath)
    order = [l.split(':')[0] for l in open(os.path.join(root, 'QuestLinesOrder.txt'), encoding='utf8').read().splitlines() if l]
    for b64 in order:
        if decode(b64) not in lines:
            errors.append('QuestLinesOrder.txt entry without a line: %s' % b64)
    for lid, folder in lines.items():
        boxes = []
        for path in glob.glob(os.path.join(folder, '*.json')):
            if path.endswith('QuestLine.json'):
                continue
            d = load(path)
            qid = (d['questIDHigh:4'], d['questIDLow:4'])
            if qid not in quests:
                errors.append('line entry for unknown quest: %s' % rel(path))
            boxes.append((d['x:3'], d['y:3'], d['sizeX:3'], d['sizeY:3'], rel(path)))
        for i, a in enumerate(boxes):
            for b in boxes[i + 1:]:
                if a[0] < b[0] + b[2] and b[0] < a[0] + a[2] and a[1] < b[1] + b[3] and b[1] < a[1] + a[3]:
                    errors.append('overlap in %s: %s / %s' % (rel(folder), a[4], b[4]))
    # item and entity names
    if registry or entities:
        reg = set(open(registry).read().split()) if registry else None
        ents = set(open(entities).read().split()) if entities else None
        for qid, (path, d) in quests.items():
            blob = json.dumps(d)
            if reg is not None:
                for name in set(re.findall(r'"id:8": "([a-zA-Z0-9_]+:[^"]+)"', blob)):
                    if name.startswith('gregguns:') and name not in reg:
                        errors.append('%s: unknown item %s' % (rel(path), name))
            if ents is not None:
                for t in d.get('tasks:9', {}).values():
                    if t.get('taskID:8') == 'bq_standard:hunt' and t['target:8'] not in ents:
                        errors.append('%s: unknown entity %s' % (rel(path), t['target:8']))
    if only:
        errors = [e for e in errors if only in e]
    print('quests %d, lines %d, errors %d' % (len(quests), len(lines), len(errors)))
    for e in errors[:50]:
        print('ERROR', e)
    return 1 if errors else 0


if __name__ == '__main__':
    args = sys.argv[1:]
    reg = ents = None
    if '--registry' in args:
        reg = args[args.index('--registry') + 1]
    if '--entities' in args:
        ents = args[args.index('--entities') + 1]
    only = args[args.index('--only') + 1] if '--only' in args else None
    sys.exit(main(args[0], reg, ents, only))
```

## Import and test

1. **Copy** the new folders and the two edited files into a dev pack's `config/betterquesting/DefaultQuests/`.
2. **New world:** start it. A world with no saved quest database loads the defaults ([SaveLoadHandler#L236-L246](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/handlers/SaveLoadHandler.java#L236-L246)).
3. **Existing world:** run `/bq_admin default load`. It reloads the defaults and restores each player's progress from a snapshot taken first ([QuestCommandDefaults#L353-L365](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L353-L365), [#L464](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L464)). The log line `Loaded N quests` after a world load should rise by 118.
4. **Look.** Open the questbook. Check the tab order, that the GregGuns tab is last, the layout and every icon. Nudge positions in edit mode (`/bq_admin edit`).
5. **Gate test.** With `/bq_admin complete` (admin subcommands include `complete` and `reset`; run `/bq_admin` in game for the usage), complete the pack quests in the [Unlock design](index.md#unlock-design) table in turn and confirm each chapter intro appears only then.
6. **Task test.** Obtain each item with `/give`, and confirm the retrieval completes. Kill each hunt target from its spawn egg and confirm the count rises.
7. **Reward test.** Claim a bag and an item reward in survival. Confirm the bag opens and the item matches the tier.
8. **Warning.** `/bq_admin default save` **deletes the whole `DefaultQuests` folder before rewriting it** ([QuestCommandDefaults#L166-L196](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L166-L196)). Only run it in a world that was loaded from the full default set, and commit first.

## Checklist

- [ ] Every quest in [quest-line.md](quest-line.md) has a Quests file and a position file with the same name.
- [ ] IDs come from the builder; no hand-picked UUIDs.
- [ ] The line is last in `QuestLinesOrder.txt`; `pack_version:3` is raised.
- [ ] Every `gregguns:` item and `gregguns.` entity name matches the final registry (no placeholder left).
- [ ] Validator: 0 findings with `--only GregGuns-`, and with `--registry` and `--entities`.
- [ ] Every bag's tier is at most its quest's tier. Every quest teaches one mechanic.
- [ ] Only the Steam chapter uses `crafting`. No command rewards.
- [ ] Only the three boss quests are `SECRET`.
- [ ] The Repair Bench and Turret tiers match area 09.
- [ ] Text uses only `[warn]`, `[note]`, `[url]`; hazards and nuclear items carry a `[warn]`.
- [ ] Tested on a new world and on an old one with `/bq_admin default load`.
- [ ] Discussed in the pack's `#quest-dev` channel first, as the pack Readme requires for larger changes.
