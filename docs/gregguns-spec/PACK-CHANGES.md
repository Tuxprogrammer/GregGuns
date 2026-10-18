# Pack-side changes

Every change GregGuns needs outside this repo, collected from the area indexes. GregGuns never writes another mod's config at runtime (see [CONVENTIONS § Rules](CONVENTIONS.md#rules)).

**Delivery:**
- Modpack changes go in one PR to [GTNewHorizons/GT-New-Horizons-Modpack](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack), in [M9](../ROADMAP.md#m9--pack-integration-and-release), after the registry names are fixed. Until then, every `gregguns:` name below is a placeholder.
- GT5-Unofficial changes go as their own PR once the GTNH team agrees them.

**Paths** are relative to the pack's `.minecraft/` folder. In the reference pack that is `reference/gtnh/2.9.0-RC-2/.minecraft/`.

## GT-New-Horizons-Modpack (config and data)

### Loot ([0007](../design/0007-pack-loot-and-infernal-mobs.md))

| File | Change | Why | Sheet |
|---|---|---|---|
| `config/TooMuchLoot/loot/gregguns_<category>.xml` (new, one per category) | `loading_mode="ADD"` files for `bonusChest`, `dungeonChest`, `mineshaftCorridor`, `villageBlacksmith`, `pyramidDesertyChest`, `pyramidJungleChest`, `strongholdCorridor`, `strongholdCrossing`, `chest1`–`chest4`, following the tier bands | The pack ships these categories as `OVERRIDE`, so additions from code are wiped. A missing or misspelt `loading_mode` defaults to `OVERRIDE` and wipes the category | [pack-loot.md](06-items/pack-loot.md#draft-add-files) |
| `config/EnhancedLootBags/LootBags.xml` | Add `Loot` lines to `steam_age` and `tier_1_lv` … `tier_7_zpm`, each holding items of its own tier | Quest-reward bags are data only | [pack-loot.md](06-items/pack-loot.md#enhancedlootbags) |
| `config/roguelike_dungeons/settings/loot_weapons.json`, `loot_supplies.json`, `loot_armor.json` | GregGuns loot rules per level: 0–1 LV, 2–3 MV, 4 rare HV | Dungeon chests are JSON only | [pack-loot.md](06-items/pack-loot.md#roguelike-dungeons) |
| `config/InfernalMobs.cfg` | Append **ammunition, magazines, grenades and consumables only** (LV–MV) to `droppedItemIDsElite`, `droppedItemIDsUltra` and `droppedItemIDsInfernal`. No guns or armour | The lists are global and apply to vanilla mobs too. Guns come from our own NPCs through the soft-dependency handler | [pack-loot.md](06-items/pack-loot.md#infernal-mobs) |
| `config/GTNewHorizons/CustomDrops.xml` | Optional: `CustomDrop` entries for boss NPCs (full class name; player kills only) | Pack-specific fixed rewards | [pack-loot.md](06-items/pack-loot.md#gtnh-customdrops), [mob-drops-nei.md](10-entities-npcs/mob-drops-nei.md) |

### Mobs

| File | Change | Why | Sheet |
|---|---|---|---|
| `config/InfernalMobs.cfg` | `permittedentities`: `AttackHelicopter=false`, and explicit `true` for every hostile NPC class, by simple class name | `useSimpleEntityClassnames=true`; the helicopter has its own boss bar | [infernal-mobs.md](10-entities-npcs/infernal-mobs.md) |
| `config/InfernalMobs.cfg` | `entitybasehealth`: base health for each class | Otherwise the first spawned instance's health is frozen into the file | [infernal-mobs.md](10-entities-npcs/infernal-mobs.md) |
| `config/InfernalMobs.cfg` | `mm_berserk.berserkMaxDamage`: a positive value, for example `20.0` | The shipped `0.0` makes Berserk mobs deal no damage. This is a pack-wide bug, not ours | [infernal-mobs.md](10-entities-npcs/infernal-mobs.md) |
| `config/mobsinfo/mobsinfo.cfg` | Add `gregguns.SimpleTurret` to `mobhandler.MobBlacklist` | With `IncludeEmptyMobs=true` it would be listed as an empty mob | [mob-drops-nei.md](10-entities-npcs/mob-drops-nei.md) |
| `config/angermod.cfg` | Add `gregguns.` to `kamikaze.butcherItems` | Otherwise melee kills with GregGuns tools trigger a 10% explosion | [spawning.md](10-entities-npcs/spawning.md) |
| `config/ThaumcraftMobAspects/gregguns.cfg` | A new file with aspect values for each NPC | Without it, no GregGuns mob can be scanned | [registry-and-pack-hooks.md](10-entities-npcs/registry-and-pack-hooks.md), [thaumcraft-aspects.md](06-items/thaumcraft-aspects.md) |
| `config/enderzoo/SpawnConfig_User.xml` | Optional: `entry` rows `mobName="gregguns.<Name>"` | Lets the pack tune spawns per biome or dimension | [spawning.md](10-entities-npcs/spawning.md) |

### Items, input and UI

| File | Change | Why | Sheet |
|---|---|---|---|
| `config/backhand.cfg` | Add each gun with right-click zoom, charge or reload to `general.mainhandUseStopsOffhandFallback` | Stops one right-click from also using the offhand item | [offhand-backhand.md](02-guns/offhand-backhand.md) |
| `config/backhand.cfg` | Add each gun to `general.offhandBlacklist` | A gun can't fire from the offhand | [offhand-backhand.md](02-guns/offhand-backhand.md) |
| `config/NEI/collapsibleitems.cfg` | GregGuns groups (magazines, tanks, ammo by calibre, armour sets), and the spawn egg added to the egg group | Collapsible groups can only be set in this file | [nei-visibility.md](06-items/nei-visibility.md#nei-collapsible-groups-file-only) |
| `config/NEI/handlerordering.csv` | Optional: `gregguns.recipe.reactionchamber,-5` | Puts the tab beside GT's chemical tabs | [nei-handlers.md](08-machines-production/nei-handlers.md#gt5-unofficial-one-nei-handler-per-recipe-category) |
| `config/GTNewHorizons/CustomToolTips.xml` | Optional: a `customtooltip.recharge_tier_machine` row for each powered item | Matches how the pack shows charger tiers | [powered-flight.md](05-armor/powered-flight.md) |
| `config/betterquesting/DefaultQuests/Quests/` | A GregGuns quest line beside the pack's tiered lines | Quests live in the pack | [01-core index](01-core/index.md) |

`config/Baubles.cfg` needs no change, because [0009](../design/0009-gtnh-integration-defaults.md) reuses the existing `head`, `wings` and `quiver` slot types.

## GT5-Unofficial (needs agreement with the GTNH team)

| File | Change | Why | Sheet |
|---|---|---|---|
| `src/main/java/gregtech/api/enums/MetaTileEntityIDs.java` | Add named entries `GregGuns<Machine>(20100)` … in sorted order, as CropsNH did for 28001-28055 (block 20100-20163 proposed) | GT has no automatic ID allocation for addons. Named entries in this enum are how external mods reserve IDs | [metatileentity-framework.md](08-machines-production/metatileentity-framework.md#gt5-unofficial-mte-id-reservation) |
| `src/main/java/gregtech/api/enums/Textures.java` (page-owner comment) | `page 50: GregGuns casings` (proposed) | Same, for casing texture pages | [metatileentity-framework.md](08-machines-production/metatileentity-framework.md#gt5-unofficial-casing-texture-pages) |
