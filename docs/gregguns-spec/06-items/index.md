# 06-items: GregGuns spec

## Overview

This area records how GregGuns items differ from [Techguns' non-gun, non-armor items](../../techguns-spec/06-items/index.md) and how they plug into GTNH. The central change is that GT now supplies the metals, plates, wires, circuits, polymers and fluids that Techguns made itself ([0002](../../design/0002-machine-deconfliction.md)). GregGuns keeps only what GT does not make, and builds it from GT parts. The other half is loot: the pack's loot mods replace vanilla chest tables, so GregGuns items reach GTNH loot through pack-side data, not through code.

Techguns topics with no integration point have no sheet here. Their behavior is the Techguns spec plus the design records:

- sounds ([sounds.md](../../techguns-spec/06-items/sounds.md)), textures ([textures.md](../../techguns-spec/06-items/textures.md)) and lang ([lang.md](../../techguns-spec/06-items/lang.md)): art direction is [0006](../../design/0006-art-direction.md); no pack file touches them;
- the shared meta item, glider, camo net placer, door item, tools and spawn eggs ([special-items.md](../../techguns-spec/06-items/special-items.md)): no pack mod or GT system replaces them, apart from the NEI display in [nei-visibility.md](nei-visibility.md);
- recipe grids ([Techguns recipes.md](../../techguns-spec/06-items/recipes.md)): rewritten against GT in [recipes.md](recipes.md), which also holds the component index the other areas use.

## Contents

| File | What it holds |
|---|---|
| [recipes.md](recipes.md) | Every GregGuns recipe in this area (63 rows: parts, emitters, plating, ammo, magazines, tanks, cells, consumables), with GT recipe maps, EU/t, durations and programmed circuits, and the **component index** of placeholder registry names for areas 02, 05 and 07 to 09 |
| [pack-loot.md](pack-loot.md) | Cross-cutting. How GregGuns items enter GTNH loot: TooMuchLoot, LootGames, Roguelike Dungeons, EnhancedLootBags, Infernal Mobs, dreamcraft CustomDrops, Mobs Info. Draft `ADD` XML per category. Anchors for areas 10 and 11 |
| [components-and-materials.md](components-and-materials.md) | Which Techguns parts become GT parts, which stay GregGuns items, and ore-dict rules |
| [fluids.md](fluids.md) | Techguns fluids to GT materials' fluids; tanks as fluid containers |
| [ammo.md](ammo.md) | GT recipe maps for ammo; GT's `round` and bullet prefixes |
| [hazards-and-tooltips.md](hazards-and-tooltips.md) | dreamcraft hazards, tooltips and fuels; GT radiation for nuclear items |
| [nei-visibility.md](nei-visibility.md) | NEI hiding and collapsing of ammo variants, spawn eggs and technical items |
| [nei-order.md](nei-order.md) | The one NEI and creative-tab order of every GregGuns item and block, and how NEI applies it in existing worlds |
| [thaumcraft-aspects.md](thaumcraft-aspects.md) | Aspects for items through GT's Thaumcraft bridge; mob aspects contract |

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Chest loot added by code to vanilla categories | Pack `ADD` files; own `gregguns:*` categories kept in code | TooMuchLoot 4.3.0-GTNH | [pack-loot.md](pack-loot.md#toomuchloot) |
| Structure chest categories | Registered `gregguns:*` categories | Forge `ChestGenHooks`, TooMuchLoot | [pack-loot.md](pack-loot.md#gregguns-loot-categories) |
| Minigame rewards | Follow `chest1..4` ADD files | LootGames 2.2.14 | [pack-loot.md](pack-loot.md#lootgames) |
| Dungeon chests | Pack JSON loot rules | Roguelike-Dungeons 1.6.6-GTNH | [pack-loot.md](pack-loot.md#roguelike-dungeons) |
| Quest rewards | Entries in the tier groups | EnhancedLootBags 1.3.5 | [pack-loot.md](pack-loot.md#enhancedlootbags) |
| NPC drops | Infernal drop lists, or a soft-dependency handler | Infernal-Mobs 1.10.7-GTNH | [pack-loot.md](pack-loot.md#infernal-mobs) |
| Fixed per-mob drops | Optional `CustomDrop` entries | dreamcraft CustomDrops | [pack-loot.md](pack-loot.md#gtnh-customdrops) |
| Metals, plates, wires, circuits, plastic, rubber | Not registered; GT parts through the unificator | GT5-Unofficial 5.09.54.205 | [components-and-materials.md](components-and-materials.md#gregtech-parts-through-the-unificator) |
| Oil, fuel, redstone, ender, biofuel, milk, acid | GT materials' fluids; acid dropped | GT5-Unofficial | [fluids.md](fluids.md#fluid-mapping) |
| Fuel, bio, air and oxygen tanks | `IFluidContainerItem` items GT machines can fill | GT fluid containers | [fluids.md](fluids.md#gregtech-fluid-containers) |
| Ammo Press | Reinstated ([0011](../../design/0011-machine-policy.md)): one `gregguns.ammoPress` recipe per ammo type, picked by programmed circuit; slugs, cases and powder are GT `round`, `foil`, `itemCasing` and gunpowder | GregGuns Ammo Press and Ammunition Works ([ammo-press.md](../08-machines-production/ammo-press.md)) | [recipes.md](recipes.md#ammunition) |
| Metal Press, Chem Lab, Fabricator recipes for parts | Assembler, Chemical Bath and Assembly Line recipes at the part's tier | GT recipe maps | [recipes.md](recipes.md#gun-parts) |
| Per-item crafting grids (R01 to R87) | Crafting table only for Steam/ULV items and magazine refills; everything else on a GT map | GT recipe maps, `TierEU` | [recipes.md](recipes.md) |
| Chem Lab tank fill | GT Fluid Canner fills `IFluidContainerItem` tanks | GT `cannerRecipes` | [recipes.md](recipes.md#filling-tanks) |
| Energy Cell | Dropped: energy weapons are their own IC2 batteries ([0010](../../design/0010-material-remap-and-tier-additions.md)) | IC2 `IElectricItem`, GT chargers | [recipes.md](recipes.md#energy-cell-note) |
| Bullets from nuggets | Built from GT `round` parts; `bulletGt*` prefixes unused by GT | GT `OrePrefixes` | [ammo.md](ammo.md#gregtech-bullet-prefixes-and-round) |
| Nuclear Power Cell and warhead | Radioactive through GT `ItemData` | GT radiation, hazard protection | [hazards-and-tooltips.md](hazards-and-tooltips.md#gt-radiation-for-nuclear-items) |
| Item hazards, tooltips, fuels | No entries needed | dreamcraft modules | [hazards-and-tooltips.md](hazards-and-tooltips.md) |
| Creative-tab listing of 110 metas | Hide technical items, collapse variants | NEI 2.8.155-GTNH | [nei-visibility.md](nei-visibility.md) |
| No aspects | Aspects through GT's bridge | Thaumcraft 4.2.3.5, GT compat | [thaumcraft-aspects.md](thaumcraft-aspects.md) |
| (none) Mob aspects | `gregguns.`-prefixed entity names | ThaumcraftMobAspects 1.2.1-GTNH | [thaumcraft-aspects.md](thaumcraft-aspects.md#thaumcraftmobaspects-entities) |

## Pack-side changes

These land as a PR to the [GTNH modpack repository](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack). Item names are placeholders until M2 fixes the registry.

| File | Change | Why | Sheet |
|---|---|---|---|
| `config/TooMuchLoot/loot/gregguns_<category>.xml` (new, one per category) | `loading_mode="ADD"` files for `bonusChest`, `dungeonChest`, `mineshaftCorridor`, `villageBlacksmith`, `pyramidDesertyChest`, `pyramidJungleChest`, `strongholdCorridor`, `strongholdCrossing`, `chest1`, `chest2`, `chest3`, `chest4` | The vanilla and pack categories ship as `OVERRIDE`, so code-side additions are wiped | [pack-loot.md](pack-loot.md#draft-add-files) |
| `config/EnhancedLootBags/LootBags.xml` | Add `Loot` lines to `steam_age` and `tier_1_lv` to `tier_7_zpm` | Quest-reward bags are data only | [pack-loot.md](pack-loot.md#enhancedlootbags) |
| `config/roguelike_dungeons/settings/loot_weapons.json`, `loot_supplies.json`, `loot_armor.json` | Add GregGuns loot rules per dungeon level | Dungeon chests are JSON only | [pack-loot.md](pack-loot.md#roguelike-dungeons) |
| `config/InfernalMobs.cfg` | Append GregGuns entries to `droppedItemIDsElite`, `droppedItemIDsUltra`, `droppedItemIDsInfernal` (route A only) | Drop lists are global config | [pack-loot.md](pack-loot.md#infernal-mobs) |
| `config/InfernalMobs.cfg` | Optional: set `permittedentities` or `entitiesalwaysinfernal` keys for GregGuns NPCs, by simple class name | `useSimpleEntityClassnames=true` in the pack | [pack-loot.md](pack-loot.md#infernal-mobs) |
| `config/GTNewHorizons/CustomDrops.xml` | Optional: `CustomDrop` entries for boss NPCs, by full class name | Fixed, player-kill-only drops | [pack-loot.md](pack-loot.md#gtnh-customdrops) |
| `config/NEI/collapsibleitems.cfg` | Add GregGuns groups (magazines, tanks, ammo by caliber, armor sets) and append the spawn egg to the existing egg group | Collapsible groups are file-only | [nei-visibility.md](nei-visibility.md#nei-collapsible-groups-file-only) |
| `config/ThaumcraftMobAspects/gregguns.cfg` | Optional: fill in aspect values after first run | The file is created empty | [thaumcraft-aspects.md](thaumcraft-aspects.md#thaumcraftmobaspects-entities) |

## Related areas

- [02-guns](../../techguns-spec/02-guns/index.md): guns that consume this ammo; gun recipes use the parts in [components-and-materials.md](components-and-materials.md).
- [05-armor](../05-armor/baubles-slots.md): hazmat protection for radiation ([hazards-and-tooltips.md](hazards-and-tooltips.md)), jetpack stance.
- [08-machines-production](../../techguns-spec/08-machines-production/index.md): GT recipe maps for components (the Reaction Chamber is retired, [0012](../../design/0012-retire-reaction-chamber-and-retort.md)).
- [10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md): NPC drops and class naming; see [pack-loot.md](pack-loot.md#infernal-mobs).
- [11-worldgen](../11-worldgen/structure-placement.md): structure chests; see [pack-loot.md](pack-loot.md#gregguns-loot-categories).

## Open questions

Each needs a decision before the sheet's requirement is final.

- **Material remap (closed).** Settled by [0010](../../design/0010-material-remap-and-tier-additions.md). See [components-and-materials.md](components-and-materials.md#open-questions).
- **Infernal HV drops.** D5 caps surface-structure loot at LV to MV, but Infernal's lists are global. An amendment to [0005](../../design/0005-world-presence.md) should say whether infernal drops may include rare HV items. See [pack-loot.md](pack-loot.md#open-questions).
- **Route A or B for NPC loot.** Do vanilla infernal mobs drop GregGuns items (global lists), or only our NPCs? See [pack-loot.md](pack-loot.md#open-questions).
- **Nuclear item recycling.** Radioactivity via `ItemData` also makes the item recyclable into uranium. Decide between that and the enchantment route. See [hazards-and-tooltips.md](hazards-and-tooltips.md#open-questions).
- **Recipe-sheet decisions.** Several choices in [recipes.md](recipes.md#open-questions) need a ruling: the Steel gate for loose LV ammo and the nuclear refuel route. The material remap, the tiers for Bandages, Cybernetic Parts and the two unnamed bullets, and the Energy Cell are closed by [0010](../../design/0010-material-remap-and-tier-additions.md).
- **`bulletGt*` ore names.** Whether to claim GT's unused bullet prefixes. See [ammo.md](ammo.md#open-questions).
