# tools/questbook

Builds the GregGuns quest line that ships inside the jar (`src/main/resources/assets/gregguns/questbook`). Python 3, standard library only. Design: `docs/gregguns-spec/12-questbook/`.

- `quests/`: the quest data, the single source of truth (keys, titles, tasks, rewards, prerequisites, layout). `_line.json` holds the header (`format`, `namespace`, `line`, `retired`); each `<chapter>.json` is `{"chapter": "<chapter>", "retired": [...], "quests": [...]}` (`retired` optional), so parallel edits to different chapters never conflict. Quest IDs are `uuid5(namespace, key)`, so a key never changes after release.
- `build_quests.py`: writes `index.json`, the line file, and one quest file plus one position file per quest. Output is byte-identical across rebuilds.
- `quest_doc.py`: regenerates the quest tables, reward budget, totals and trigger table of `docs/gregguns-spec/12-questbook/quest-line.md` from the data (`--check` reports a stale doc and writes nothing).
- `validate_quests.py`: checks a quest folder (file names, prerequisites, cycles, overlaps, `index.json`); with `--data`, also the data's own checks and that the resources are not stale.
- `pack_items.txt`: the pack item ids quests may name besides `gregguns:` ones (coins, Galacticraft parts, GT materials, HarvestCraft food, loot bags). `pack_items.py <pack .minecraft>` regenerates it from the pack's `DefaultQuests`, `LootBags.xml` and the HarvestCraft jar's lang.

The data schema (tasks, `taskLogic`, rewards, `{"bag": "lv"}` stacks, `[questlink]`, retired keys) is in [authoring.md](../../docs/gregguns-spec/12-questbook/authoring.md#quest-data). Loot bags resolve through the `BAGS` table in `build_quests.py`; title colors come from `TIER_COLORS`.

```sh
python -I tools/questbook/build_quests.py --out src/main/resources/assets/gregguns/questbook --lang src/main/resources/assets/gregguns/lang/en_US.lang
python -I tools/questbook/validate_quests.py src/main/resources/assets/gregguns/questbook --data tools/questbook/quests
```

After editing a chapter file, rebuild, run `python -I tools/questbook/quest_doc.py` to regenerate the tables in `quest-line.md`, and run `./gradlew test`. `QuestbookResourcesTest` fails if the resources are stale or name something GregGuns does not register.

## Layout and prerequisites

Per quest: `x`, `y`, `size`, `pre` (quest keys, or pack quest numbers), optional `implicit` (a subset of `pre` drawn as BetterQuesting implicit prerequisites) and optional `main` (default: `size > 24`; `true` on the spine a player must follow, which BetterQuesting draws with thick incoming lines).

Rules: 36 unit pitch, 24 unit nodes, 28 or 48 unit chapter heads and main quests (72 for a tier milestone), at most two in-line prerequisites per quest, only the first one drawn, no hub fan-out from `gg.start.welcome`.

### How BetterQuesting draws a line

Read from GTNewHorizons/BetterQuesting at tag 3.8.89-GTNH:

- Line thickness is `main ? 8 : 4`, where `main` is the property of the quest that has the prerequisite (the dependent), not of the prerequisite: [CanvasQuestLine.setQuestLine](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api2/client/gui/panels/lists/CanvasQuestLine.java#L454-L462). So every incoming line of a main quest is double thick. That is the thick red fan on the old tab.
- A line runs from the center of the prerequisite's box to the center of the dependent's box, so a box size only changes where it ends, and x, y, size are the canvas units directly (the canvas zooms to fit the window with a 24 unit margin, `fitToWindow`).
- Per-prerequisite type, `RequirementType` NORMAL, IMPLICIT, HIDDEN ([IQuest](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api/questing/IQuest.java#L96-L100)), stored as a `type` byte on the prerequisite entry ([QuestInstance](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/questing/QuestInstance.java#L517-L533)). IMPLICIT draws the line only while either end is hovered or Shift is held (and pulses); HIDDEN is never drawn. The type does not change gating: `isUnlocked` uses the whole prerequisite set. Prerequisites that are not in the line (pack quests) have no button, so they draw nothing.
- Hence GregGuns uses IMPLICIT for secondary and cross-chapter prerequisites, and keeps pack numeric prerequisites as they were.

### Preview renderer

`render_preview.py` draws the same rules to a PNG so a layout can be reviewed without Minecraft. It is the only script here that needs Pillow (`pip install pillow`) and is dev-only.
