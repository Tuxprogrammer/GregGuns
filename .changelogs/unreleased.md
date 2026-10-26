# Unreleased

Notes for the next release. Fold these into that release's changelog when it is cut.

- Player guides: the recipes, enemies, Ammunition Works and Armory Gantry sheets now show Minecraft's real textures (ingredient icons, the Skeleton and Zombie Pigman Soldier skins, the interior lamp) instead of our stand-ins, credited in each sheet's footer.
- OpenModularTurrets overlap investigation finished (design record 0020): the Turret and Rampart stay, with no code or pack changes. A new unit test pins which of our entities OMT turrets will and will not shoot.
- Translation readiness: every player-visible string is a lang key. The "module is off" tooltip and chat line, the Ammo Pouch inventory name and every GT machine name moved into `en_US.lang`; the questbook text is now in the lang file too (`betterquesting.quest.<id>.*`), which BetterQuesting reads first. A new `LangFilesTest` checks code keys, extra translation files and placeholders. How to add a language is in `docs/translating.md`.
