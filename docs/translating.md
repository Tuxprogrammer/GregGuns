# Translating GregGuns

GregGuns ships English only. Everything a player reads is a lang key, so a community translation is one new file and needs no code change.

## Add a language

1. Copy `src/main/resources/assets/gregguns/lang/en_US.lang` to `<code>.lang` in the same folder, with Minecraft's code for the language (`de_DE.lang`, `fr_FR.lang`, `ru_RU.lang`, `zh_CN.lang`).
2. Keep the file UTF-8.
3. Translate the values only. Never change a key.
4. You may delete the lines you do not translate. Minecraft falls back to English for a missing key. Never add a key that `en_US.lang` lacks.
5. Run `./gradlew test --tests '*LangFilesTest'`. It fails on an unknown key, a duplicate key, or a line whose placeholders differ from English.

A pack can also ship the same file in a resource pack at `assets/gregguns/lang/<code>.lang`.

## Placeholders

`%s`, `%d`, `%1$s` and the like are filled in by the game. Keep the same number and kind in your line. You may reorder them with positions (`%2$s ... %1$s`) when your grammar needs it. `%%` is a literal percent sign and `%n` is a line break. A line with the wrong placeholders prints a "Format error" or the wrong value in game.

Color codes (`§c`) are added by the code, not by the lang file, so a translation never carries them.

## Key conventions

| Prefix | What it names |
|---|---|
| `item.gregguns.<name>.name`, `tile.gregguns.<name>.name`, `entity.gregguns.<name>.name` | Item, block and mob names, from the registered name |
| `gregguns.<area>.<key>` | Tooltips, chat and command output, HUD text, machine text, NEI tab titles (the recipe map's name is the key) |
| `gui.gregguns.*`, `container.gregguns.*` | GUI titles and inventory names |
| `key.gregguns.*`, `key.categories.gregguns` | Key bind names and the Controls category |
| `death.attack.<damageType>[.item|.player]` | Death messages |
| `gt.blockmachines.gregguns.<name>.name` | GT machine names (see below) |
| `betterquesting.quest.<id>.name`, `.desc` and `betterquesting.questline.<id>.*` | Questbook text (see below) |

`en_US.lang` is grouped by area with a comment line above each group. Keep new keys stable once released: renaming one breaks every translation.

## Machine names

GT names a machine from the key `gt.blockmachines.<internal name>.name`. If the language system already has that key, GT uses it and does not write its own `GregTech.lang`. Our English lines are in `en_US.lang`; the same English is also passed to GT in the Java constructor (GT needs one), and the two must match. If a name stays English in game after you translate it, GT has already written it to its own `config/GregTech/GregTech.lang` on that install; delete the line there.

## Questbook

BetterQuesting looks up `betterquesting.quest.<id>.name` and `.desc` (and `betterquesting.questline.<id>.name` and `.desc`) before the text stored in the quest file ([QuestTranslation](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api2/utils/QuestTranslation.java)), so our quest text is translatable. The block between `# BEGIN questbook` and `# END questbook` in `en_US.lang` is generated from `tools/questbook/quests.json`: do not edit it in English by hand, run `python -I tools/questbook/build_quests.py --out src/main/resources/assets/gregguns/questbook --lang src/main/resources/assets/gregguns/lang/en_US.lang`. `LangFilesTest` fails when it is stale. A translation copies these lines and translates the values. Limits: a quest's task and reward descriptions (item names, "Retrieve", "Check") come from BetterQuesting and the item names, so they follow the game language; quests a pack author edits in game keep their edited text only when no lang key exists for them, so a translated pack sees the lang text.

## Intentionally not translated

| Text | Why |
|---|---|
| `/gregguns test`, `dumprecipes` and `build` output, scenario names, autotest lines | Developer tools. Scripts and logs parse them, and they only run with the dev switches on |
| Log lines and exceptions | They go to the log for bug reports, which are written in English |
| Config keys, config comments, `modules.<name>` flags | They are the file format. Server owners search for them by name |
| Registry names, texture and sound ids, NBT tags | Identifiers, not text |
| The NEI plugin's internal name | Never shown to players |
| `[GregGuns]` chat prefix, mod name, GT and tier abbreviations (`LV`, `MV`) | Names and symbols that GT and the pack use the same in every language |
| Credits and license text | Legal text stays as written |

The player guides (`docs/guides/`, the infographics) are English only. They are separate documents, not part of the jar's lang system.

## How it is checked

`LangFilesTest` (unit tests) fails when:

- a lang-key literal in `src/main/java` has no line in `en_US.lang`;
- any other `.lang` file has a key English lacks, a duplicate key, or a different set of `%` placeholders than English;
- the generated questbook block is out of date.

Registered item, block and entity names are built from dynamic names in the code, so `ContentChecks` checks them at boot in the dev self-check, against the same file.
