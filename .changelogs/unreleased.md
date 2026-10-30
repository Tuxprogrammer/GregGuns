# Unreleased

Notes for the next release. Fold these into that release's changelog when it is cut.

### Quest loot bags

- GregGuns quests reward GregGuns loot bags: eight tier bags from Recruit (Steam) to General (ZPM), Spoils of War for boss kills and three ICBM Ordnance crates. They are EnhancedLootBags bags (ids 1701 to 1712), so they look, sort and take Fortune like the pack's own, and they hold GregGuns ammunition, magazines, parts, guns, armor and the materials GregGuns recipes use.
- GregGuns adds these groups to EnhancedLootBags at startup on both sides; the pack's `LootBags.xml` is not touched. `compat.lootBags` turns this off, and `config/gregguns-lootbags.xml` replaces the contents.
- A bag whose group is missing is kept with a chat line instead of crashing, and `/lootbags reload` keeps the GregGuns groups.
