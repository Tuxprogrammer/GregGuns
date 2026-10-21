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
