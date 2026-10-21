# tools/questbook

Builds the GregGuns quest line that ships inside the jar (`src/main/resources/assets/gregguns/questbook`). Python 3, standard library only. Design: `docs/gregguns-spec/12-questbook/`.

- `quests.json`: the quest data, the single source of truth (keys, titles, tasks, rewards, prerequisites, layout). Quest IDs are `uuid5(namespace, key)`, so a key never changes after release.
- `build_quests.py`: writes `index.json`, the line file, and one quest file plus one position file per quest. Output is byte-identical across rebuilds.
- `validate_quests.py`: checks a quest folder (file names, prerequisites, cycles, overlaps, `index.json`).

```sh
python -I tools/questbook/build_quests.py --out src/main/resources/assets/gregguns/questbook
python -I tools/questbook/validate_quests.py src/main/resources/assets/gregguns/questbook
```

After editing `quests.json`, rebuild, update the tables in `quest-line.md` and the quest count, and run `./gradlew test`. `QuestbookResourcesTest` fails if the resources are stale or name something GregGuns does not register.

## Layout and prerequisites

`quests.json` per quest: `x`, `y`, `size`, `pre` (quest keys, or pack quest numbers), optional `implicit` (a subset of `pre` drawn as BetterQuesting implicit prerequisites) and optional `main` (default: `size > 24`; GregGuns sets it false everywhere).

Rules: 36 unit pitch, 24 unit nodes, 28 unit chapter heads, at most two in-line prerequisites per quest, only the first one drawn, no hub fan-out from `gg.start.welcome`.

### How BetterQuesting draws a line

Read from GTNewHorizons/BetterQuesting at tag 3.8.89-GTNH:

- Line thickness is `main ? 8 : 4`, where `main` is the property of the quest that has the prerequisite (the dependent), not of the prerequisite: [CanvasQuestLine.setQuestLine](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api2/client/gui/panels/lists/CanvasQuestLine.java#L454-L462). So every incoming line of a main quest is double thick. That is the thick red fan on the old tab.
- A line runs from the center of the prerequisite's box to the center of the dependent's box, so a box size only changes where it ends, and x, y, size are the canvas units directly (the canvas zooms to fit the window with a 24 unit margin, `fitToWindow`).
- Per-prerequisite type, `RequirementType` NORMAL, IMPLICIT, HIDDEN ([IQuest](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/api/questing/IQuest.java#L96-L100)), stored as a `type` byte on the prerequisite entry ([QuestInstance](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/questing/QuestInstance.java#L517-L533)). IMPLICIT draws the line only while either end is hovered or Shift is held (and pulses); HIDDEN is never drawn. The type does not change gating: `isUnlocked` uses the whole prerequisite set. Prerequisites that are not in the line (pack quests) have no button, so they draw nothing.
- Hence GregGuns uses IMPLICIT for secondary and cross-chapter prerequisites, and keeps pack numeric prerequisites as they were.

### Preview renderer

`render_preview.py` draws the same rules to a PNG so a layout can be reviewed without Minecraft. It is the only script here that needs Pillow (`pip install pillow`) and is dev-only.
