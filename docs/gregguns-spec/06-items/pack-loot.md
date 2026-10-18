# Pack loot

How GregGuns items get into GTNH loot. This sheet is cross-cutting: [10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md) (NPC drops) and [11-worldgen](../../techguns-spec/11-worldgen/index.md) (structure chests) link to the anchors below.

- **Techguns baseline:** [loot.md](../../techguns-spec/06-items/loot.md) (all `ChestGenHooks` loot), [ammo.md](../../techguns-spec/06-items/ammo.md#loose-ammunition-sources) (loose ammo sources)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0005 world presence](../../design/0005-world-presence.md)
- **Pack mods:**
  - TooMuchLoot (`TML`, 4.3.0-GTNH)
  - LootGames (`lootgames`, 2.2.14)
  - Roguelike-Dungeons (`Roguelike`, 1.6.6-GTNH)
  - EnhancedLootBags (`enhancedlootbags`, 1.3.5)
  - Infernal-Mobs (`InfernalMobs`, 1.10.7-GTNH)
  - NewHorizonsCoreMod (`dreamcraft`, 2.9.84): `CustomDrops`
  - Mobs Info (`mobsinfo`, 0.6.0)
- **Roadmap:** M2 (guns and ammo exist), M7 (NPC drops), M8 (structure loot). Pack-side XML lands once the item registry names are final.

## Anchors for other areas

| You need | Link |
|---|---|
| Put GregGuns items in vanilla or pack chests | [TooMuchLoot](#toomuchloot) |
| Register chest categories for GregGuns structures | [GregGuns loot categories](#gregguns-loot-categories) |
| Tier bands per chest | [Tier bands](#tier-bands) |
| Draft `ADD` XML per category | [Draft ADD files](#draft-add-files) |
| Minigame (LootGames) rewards | [LootGames](#lootgames) |
| Dungeon (Roguelike) chests | [Roguelike Dungeons](#roguelike-dungeons) |
| Quest-reward bags | [EnhancedLootBags](#enhancedlootbags) |
| Elite, ultra and infernal drops | [Infernal Mobs](#infernal-mobs) |
| Soft-dependency drop handler for our NPCs | [Infernal Mobs: handler](#infernal-mobs-handler) |
| Per-entity drops from the pack's own file | [GTNH CustomDrops](#gtnh-customdrops) |
| NEI drop display | [Mobs Info](#mobs-info) |

## What changes from Techguns

Techguns calls `ChestGenHooks.addItem` into vanilla categories from its own code. In GTNH that is wiped: every vanilla category ships as an `OVERRIDE` file in TooMuchLoot, which replaces the category wholesale after our code has run. GregGuns therefore splits loot into two routes.

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Adds guns, ammo and gear to vanilla `bonusChest` and `dungeonChest` in code ([loot.md](../../techguns-spec/06-items/loot.md#vanilla-categories)) | Adds nothing to vanilla categories in code. The pack ships `ADD` XML files ([draft](#draft-add-files)) | TML `OVERRIDE` replaces the category after our code has run |
| Seven private categories `techgunsWeaponchestT1..T3`, `techgunsReinforcedchestT1..T3`, `techgunsMilBaseBarracks` | Private categories `gregguns:*`, still registered in code ([below](#gregguns-loot-categories)) | No pack file overrides them, and a pack author can retune them through TML |
| Reinforced chests hold raw metals (coal, tin, copper, lead, steel, plastic) | Reinforced chests hold GT parts, fetched through `GTOreDictUnificator` | GT first (CONTRIBUTING § GTNH integration) |
| Loot is flat T1 to T3 | Loot follows the [D1 tier map](../../design/0001-tier-map.md); a chest never holds items above its [tier band](#tier-bands) | D5: "loot never contains items above the tier the location implies" |
| NPC drops are fixed per mob | Infernal tiers (elite, ultra, infernal) can drop tiered GregGuns items, through the pack's drop lists or a soft-dependency handler | The user wants hostile NPCs to be infernal-eligible |

## Tier bands

Tiers are the D1 tiers: Steam/ULV, LV, MV, HV, EV and up. D5 says surface structures default to LV to MV. The bands below are the proposal for each loot surface.

| Surface | Band | Reason |
|---|---|---|
| `bonusChest` | Steam/ULV | Start-of-game chest. Hand Cannon and stone bullets, as in Techguns |
| `chest1`, `chest2` (LootGames stages 1, 2) | LV | Early minigame rewards |
| `chest3` (stage 3) | LV to MV | Pack's own `chest3` already holds OpenModularTurrets walls and a GT mid-game item mix |
| `chest4` (stage 4) | MV, rare HV | Pack's own `chest4` holds the Coal Jetpack and Copter Pack |
| `mineshaftCorridor`, `villageBlacksmith`, `dungeonChest` | LV, rare MV | Early-game surface and shallow structures |
| `pyramidDesertyChest`, `pyramidJungleChest` | LV to MV | Mid-game temples |
| `strongholdCorridor`, `strongholdCrossing` | MV, rare HV | Deep structures |
| `strongholdLibrary`, `naturalistChest`, `railcraft:workshop`, `pyramidJungleDispenser` | None | Wrong theme, or not a chest |
| `gregguns:weaponchest_t1` / `_t2` / `_t3` | LV / MV / HV | T3 only where a structure's config allows it |
| Roguelike dungeon levels 0 to 4 | Level 0-1 LV, 2-3 MV, 4 rare HV | Dungeon level rises with depth |
| Elite / ultra / infernal drops | LV-MV / MV / MV, rare HV | See [Infernal Mobs](#infernal-mobs) |
| EnhancedLootBags `tier_N_*` | The bag's own tier | The bag name is the tier |

## Integration points

### TooMuchLoot

TooMuchLoot (`TML`) reads every `*.xml` in `config/TooMuchLoot/loot/` when the server starts and applies it to Forge's `ChestGenHooks` table.

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/TooMuchLoot/loot/`.
  - Every vanilla category ships with `loading_mode="OVERRIDE"`: `bonusChest`, `dungeonChest`, `mineshaftCorridor`, `pyramidDesertyChest`, `pyramidJungleChest`, `pyramidJungleDispenser`, `strongholdCorridor`, `strongholdCrossing`, `strongholdLibrary`, `villageBlacksmith`.
  - Also `OVERRIDE`: the pack's own categories `chest1`, `chest2`, `chest3`, `chest4`, `naturalistChest` and `railcraft:workshop`.
  - Total weights as shipped (they set the odds below): `bonusChest` 77, `chest1` 825, `chest2` 970, `chest3` 865, `chest4` 555, `dungeonChest` 1886, `mineshaftCorridor` 629, `pyramidDesertyChest` 425, `pyramidJungleChest` 434, `strongholdCorridor` 456, `strongholdCrossing` 310, `strongholdLibrary` 243, `villageBlacksmith` 686.
- **Source:**
  - [`TooMuchLoot.serverStarting`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/TooMuchLoot.java#L71-L104) caches the table, then calls `loadFiles`.
  - [`ChestLootLoader.loadFiles`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/data/chest/ChestLootLoader.java#L117-L170): `OVERRIDE` entries are collected and `putAll`-ed into the table first (L159-L165). `ADD` entries are then added one by one with `ChestGenHooks.getInfo(category).addItem` (L167-L170), so an `ADD` file also works on a category an `OVERRIDE` file has just replaced. `REMOVE` entries are matched by item stack.
  - [`LootGroupXML`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/data/LootGroupXML.java#L13-L63): the XML attributes are `category`, `loading_mode`, `count_min`, `count_max` on `LootGroup`, and `item`, `damage`, `nbt`, `weight`, `count_min`, `count_max` on `loot`.
  - [`XMLDataBridge.parseToChestLootCat`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/data/XMLDataBridge.java#L47-L77): a missing or misspelt `loading_mode` **falls back to `OVERRIDE`** (L47-L56), which would wipe the category. `nbt` is parsed with `JsonToNBT` (L66-L68).
  - [`ChestLootItem`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/data/chest/ChestLootItem.java#L32-L53): a missing count becomes 1; `item` is a registry name.
  - [`LootLoadingMode`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/data/LootLoadingMode.java): `OVERRIDE`, `ADD`, `REMOVE`.
  - [`CommandChestLoot`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/command/CommandChestLoot.java#L24-L56): `/loot reload`, `/loot generate`, `/loot reset`, `/loot spawnDebugChest`. Use them to test the XML below.
- **GregGuns does:** nothing at runtime for vanilla categories. The pack adds the files.
- **Pack-side change:** one file per category, named `gregguns_<category>.xml` (the file name does not matter; `*.xml` is scanned, but one category per file keeps diffs small). Rules:
  - `loading_mode="ADD"`, spelt exactly.
  - `count_min` and `count_max` on `LootGroup` are **ignored** in `ADD` mode (only the `loot` children are read), so write them as `0` to make that obvious.
  - Item names use the registry name `gregguns:<name>`. The names below are **placeholders**; M2 fixes the real ones.
  - Gun and ammo `damage` is `0`.
  - Chance per pick is `weight / (category total + added weight)`. A category with 8 picks, such as `dungeonChest`, gives each pick the same odds.

#### Draft ADD files

Each block is one file. Weights are chosen against the totals above. A weight of 20 in `dungeonChest` is about 1% per pick, about 8% per chest with 8 picks.

`gregguns_bonusChest.xml` (Steam/ULV, total 77, 10 picks)
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<LootGroup category="bonusChest" loading_mode="ADD" count_min="0" count_max="0">
    <loot item="gregguns:hand_cannon" damage="0" weight="4" count_min="1" count_max="1"/>
    <loot item="gregguns:ammo_stone_bullet" damage="0" weight="6" count_min="8" count_max="16"/>
</LootGroup>
```

`gregguns_dungeonChest.xml` (LV, rare MV, total 1886)
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<LootGroup category="dungeonChest" loading_mode="ADD" count_min="0" count_max="0">
    <loot item="gregguns:ammo_9mm_bullet" damage="0" weight="20" count_min="4" count_max="8"/>
    <loot item="gregguns:ammo_shotgun_shell" damage="0" weight="20" count_min="4" count_max="16"/>
    <loot item="gregguns:ammo_rifle_bullet" damage="0" weight="10" count_min="2" count_max="8"/>
    <loot item="gregguns:mag_9mm_pistol" damage="0" weight="12" count_min="1" count_max="3"/>
    <loot item="gregguns:revolver" damage="0" weight="8" count_min="1" count_max="1"/>
    <loot item="gregguns:sawed_off" damage="0" weight="8" count_min="1" count_max="1"/>
    <loot item="gregguns:pistol_9mm" damage="0" weight="6" count_min="1" count_max="1"/>
    <loot item="gregguns:bolt_action_rifle" damage="0" weight="3" count_min="1" count_max="1"/>
    <loot item="gregguns:combat_knife" damage="0" weight="8" count_min="1" count_max="1"/>
    <loot item="gregguns:crowbar" damage="0" weight="6" count_min="1" count_max="1"/>
    <loot item="gregguns:thompson" damage="0" weight="2" count_min="1" count_max="1"/>
    <loot item="gregguns:gas_mask" damage="0" weight="3" count_min="1" count_max="1"/>
</LootGroup>
```

`gregguns_mineshaftCorridor.xml` (LV, total 629)
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<LootGroup category="mineshaftCorridor" loading_mode="ADD" count_min="0" count_max="0">
    <loot item="gregguns:ammo_9mm_bullet" damage="0" weight="12" count_min="4" count_max="8"/>
    <loot item="gregguns:ammo_shotgun_shell" damage="0" weight="10" count_min="4" count_max="12"/>
    <loot item="gregguns:crowbar" damage="0" weight="8" count_min="1" count_max="1"/>
    <loot item="gregguns:combat_knife" damage="0" weight="6" count_min="1" count_max="1"/>
    <loot item="gregguns:hand_cannon" damage="0" weight="4" count_min="1" count_max="1"/>
    <loot item="gregguns:revolver" damage="0" weight="4" count_min="1" count_max="1"/>
    <loot item="gregguns:armor_t1_miner_helmet" damage="0" weight="3" count_min="1" count_max="1"/>
</LootGroup>
```

`gregguns_villageBlacksmith.xml` (LV, total 686; blacksmiths hold parts and melee)
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<LootGroup category="villageBlacksmith" loading_mode="ADD" count_min="0" count_max="0">
    <loot item="gregguns:combat_knife" damage="0" weight="10" count_min="1" count_max="1"/>
    <loot item="gregguns:crowbar" damage="0" weight="10" count_min="1" count_max="1"/>
    <loot item="gregguns:part_barrel_iron" damage="0" weight="8" count_min="1" count_max="2"/>
    <loot item="gregguns:part_stock_wooden" damage="0" weight="8" count_min="1" count_max="2"/>
    <loot item="gregguns:pistol_9mm" damage="0" weight="4" count_min="1" count_max="1"/>
    <loot item="gregguns:revolver" damage="0" weight="4" count_min="1" count_max="1"/>
    <loot item="gregguns:armor_t1_combat_chestplate" damage="0" weight="3" count_min="1" count_max="1"/>
</LootGroup>
```

`gregguns_pyramidDesertyChest.xml` and `gregguns_pyramidJungleChest.xml` (LV to MV, totals 425 and 434; same body, different `category`)
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<LootGroup category="pyramidDesertyChest" loading_mode="ADD" count_min="0" count_max="0">
    <loot item="gregguns:ammo_rifle_bullet" damage="0" weight="8" count_min="4" count_max="12"/>
    <loot item="gregguns:ammo_9mm_bullet" damage="0" weight="8" count_min="4" count_max="12"/>
    <loot item="gregguns:bolt_action_rifle" damage="0" weight="4" count_min="1" count_max="1"/>
    <loot item="gregguns:revolver" damage="0" weight="4" count_min="1" count_max="1"/>
    <loot item="gregguns:medkit" damage="0" weight="4" count_min="1" count_max="2"/>
    <loot item="gregguns:grenade_frag" damage="0" weight="6" count_min="2" count_max="6"/>
</LootGroup>
```

`gregguns_strongholdCorridor.xml` (MV, rare HV, total 456)
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<LootGroup category="strongholdCorridor" loading_mode="ADD" count_min="0" count_max="0">
    <loot item="gregguns:thompson" damage="0" weight="4" count_min="1" count_max="1"/>
    <loot item="gregguns:combat_shotgun" damage="0" weight="3" count_min="1" count_max="1"/>
    <loot item="gregguns:m4" damage="0" weight="2" count_min="1" count_max="1"/>
    <loot item="gregguns:ak47" damage="0" weight="2" count_min="1" count_max="1"/>
    <loot item="gregguns:mag_smg" damage="0" weight="8" count_min="1" count_max="3"/>
    <loot item="gregguns:mag_assault_rifle" damage="0" weight="8" count_min="1" count_max="3"/>
    <loot item="gregguns:medkit" damage="0" weight="6" count_min="1" count_max="2"/>
    <loot item="gregguns:flashbang" damage="0" weight="6" count_min="2" count_max="5"/>
    <loot item="gregguns:night_vision_goggles" damage="0" weight="3" count_min="1" count_max="1"/>
    <loot item="gregguns:ammo_40mm_grenade" damage="0" weight="3" count_min="2" count_max="6"/>
    <loot item="gregguns:aug" damage="0" weight="1" count_min="1" count_max="1"/>
    <loot item="gregguns:mag_lmg" damage="0" weight="1" count_min="1" count_max="1"/>
</LootGroup>
```

`gregguns_strongholdCrossing.xml` (MV, total 310)
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<LootGroup category="strongholdCrossing" loading_mode="ADD" count_min="0" count_max="0">
    <loot item="gregguns:mag_smg" damage="0" weight="6" count_min="1" count_max="2"/>
    <loot item="gregguns:mag_assault_rifle" damage="0" weight="6" count_min="1" count_max="2"/>
    <loot item="gregguns:medkit" damage="0" weight="4" count_min="1" count_max="1"/>
    <loot item="gregguns:flashbang" damage="0" weight="4" count_min="2" count_max="4"/>
    <loot item="gregguns:grenade_frag" damage="0" weight="4" count_min="2" count_max="4"/>
</LootGroup>
```

`gregguns_chest1.xml` to `gregguns_chest4.xml` (LootGames stages; see [LootGames](#lootgames)). Shown: stage 1 and stage 4.
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<LootGroup category="chest1" loading_mode="ADD" count_min="0" count_max="0">
    <loot item="gregguns:ammo_9mm_bullet" damage="0" weight="15" count_min="6" count_max="12"/>
    <loot item="gregguns:ammo_shotgun_shell" damage="0" weight="10" count_min="4" count_max="8"/>
    <loot item="gregguns:combat_knife" damage="0" weight="6" count_min="1" count_max="1"/>
    <loot item="gregguns:crowbar" damage="0" weight="6" count_min="1" count_max="1"/>
</LootGroup>
```
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<LootGroup category="chest4" loading_mode="ADD" count_min="0" count_max="0">
    <loot item="gregguns:m4" damage="0" weight="6" count_min="1" count_max="1"/>
    <loot item="gregguns:combat_shotgun" damage="0" weight="6" count_min="1" count_max="1"/>
    <loot item="gregguns:mag_assault_rifle" damage="0" weight="15" count_min="2" count_max="4"/>
    <loot item="gregguns:medkit" damage="0" weight="10" count_min="1" count_max="2"/>
    <loot item="gregguns:grenade_frag" damage="0" weight="8" count_min="3" count_max="6"/>
    <loot item="gregguns:aug" damage="0" weight="2" count_min="1" count_max="1"/>
</LootGroup>
```
`chest2` is `chest1` plus the Revolver and Sawed Off at weight 6. `chest3` is `chest2` plus Thompson, SMG magazines and Flashbangs at weight 6 to 8.

Not drafted: `strongholdLibrary`, `naturalistChest`, `railcraft:workshop` and `pyramidJungleDispenser` (see [Tier bands](#tier-bands)).

### GregGuns loot categories

GregGuns registers its own categories for its structures, so no pack file is needed for them.

- **Categories:**

| Category | Replaces (Techguns) | Band | Min / max items |
|---|---|---|---|
| `gregguns:weaponchest_t1` | `techgunsWeaponchestT1` | LV | 2 / 8 |
| `gregguns:weaponchest_t2` | `techgunsWeaponchestT2` | MV | 4 / 8 |
| `gregguns:weaponchest_t3` | `techgunsWeaponchestT3` | HV | 5 / 10 |
| `gregguns:reinforcedchest_t1` | `techgunsReinforcedchestT1` | LV parts | 3 / 6 |
| `gregguns:reinforcedchest_t2` | `techgunsReinforcedchestT2` | MV parts | 4 / 8 |
| `gregguns:reinforcedchest_t3` | `techgunsReinforcedchestT3` | HV parts | 5 / 9 |
| `gregguns:barracks` | `techgunsMilBaseBarracks` | LV to MV gear | 1 / 4 |

  The min and max values and the slot counts are the Techguns values ([loot.md](../../techguns-spec/06-items/loot.md)); retune in M8.
- **Pack config:** none ships these categories. The TML loader copies the whole table at server start ([TooMuchLoot.java#L71-L104](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/TooMuchLoot.java#L71-L104)), so a category registered in code is kept unless an `OVERRIDE` file names it. A pack author can then retune ours with an `OVERRIDE` file named after the category. TML turns a category name into a file name by replacing every character outside `[a-zA-Z0-9._]` with `_` ([`generateFiles`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/data/chest/ChestLootLoader.java#L79-L115)), for example `gregguns_weaponchest_t1.xml`. The pack's own `railcraft_workshop.xml` (category `railcraft:workshop`) is the precedent for a namespaced category.
- **GregGuns does:**
  - At init, `ChestGenHooks.getInfo(name)` sets min and max, then `addItem(new WeightedRandomChestContent(stack, min, max, weight))` adds each entry. Do this after GT has registered its items (post-init or later).
  - Material entries in reinforced chests use `GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Steel, 1)` and similar. They are never raw Techguns OreDict lookups, so they stay valid ([ITEM-02](../../techguns-spec/06-items/index.md#suspected-bugs)).
- **Pack-side change:** none required. Optional retuning files go in `config/TooMuchLoot/loot/`.

### LootGames

LootGames (Game of Light, Minesweeper, Sudoku) pays out of Forge chest categories.

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/lootgames/rewards.cfg`. For all three games, `stage_1..4` use `default_loot_table` = `chest1`, `chest2`, `chest3`, `chest4`. `per_dim_configs` is empty. `min_items` and `max_items` are 2-4, 3-6, 4-8 and 5-10 for Game of Light.
- **Source:**
  - [`RewardUtils.spawnLootChest`](https://github.com/GTNewHorizons/LootGames/blob/2.2.14/src/main/java/ru/timeconqueror/lootgames/api/util/RewardUtils.java#L72-L77) reads the table with `ChestGenHooks.getItems(lootTable, ...)`. So anything TML adds to `chest1..4` is paid out.
  - [`StagedRewards`](https://github.com/GTNewHorizons/LootGames/blob/2.2.14/src/main/java/ru/timeconqueror/lootgames/common/config/StagedRewards.java#L20-L23) holds the defaults the pack replaced with `chest1..4`.
- **GregGuns does:** nothing. The `chest1..4` ADD files in [Draft ADD files](#draft-add-files) are the whole integration.
- **Pack-side change:** the `chest1..4` ADD files. No change to `rewards.cfg`.

### Roguelike Dungeons

Roguelike Dungeons builds its chests from JSON loot rules.

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/roguelike_dungeons/settings/`.
  - `roguelike.cfg`: `dimensionWL=0` (Overworld only), `dimensionBL` empty.
  - `loot_builtin_override.json` inherits `loot_all`, which inherits `loot_weapons`, `loot_armor`, `loot_supplies`, `loot_tools`, `loot_reward` and others.
  - A rule is `{"level": N or [N..], "type": "WEAPONS", "each": true, "quantity": N, "loot": [{"data": {"name","meta","min","max","nbt"}, "weight": N}]}`. The shipped rule types include `WEAPONS`, `ARMOUR` (British spelling), `SUPPLIES`, `TOOLS`, `REWARD`, `SMITH`, `STARTER`.
- **Source:**
  - [`WeightedRandomLoot(JsonObject, int)`](https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/treasure/loot/WeightedRandomLoot.java#L60-L83): `name` is looked up in the item registry, `meta` defaults to 0, `min` and `max` apply only when both are present, `nbt` is parsed from JSON.
  - [`LootRuleManager`](https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/treasure/loot/LootRuleManager.java#L86-L88): `type` is optional, `each` and `quantity` are required.
- **GregGuns does:** nothing in code.
- **Pack-side change:** append rules to `loot_weapons.json` (guns, knife, crowbar), `loot_supplies.json` (ammo, medkit) and `loot_armor.json` (armour). Dungeon levels map to tiers as in [Tier bands](#tier-bands). Example for `loot_weapons.json`:
```json
{"level": [0, 1], "type": "WEAPONS", "each": true, "quantity": 1, "loot": [
    {"data": {"name": "gregguns:pistol_9mm", "meta": 0}, "weight": 4},
    {"data": {"name": "gregguns:revolver", "meta": 0}, "weight": 4},
    {"data": {"name": "gregguns:ammo_9mm_bullet", "meta": 0, "min": 8, "max": 16}, "weight": 8}
]},
{"level": [2, 3], "type": "WEAPONS", "each": true, "quantity": 1, "loot": [
    {"data": {"name": "gregguns:thompson", "meta": 0}, "weight": 3},
    {"data": {"name": "gregguns:combat_shotgun", "meta": 0}, "weight": 3},
    {"data": {"name": "gregguns:mag_smg", "meta": 0, "min": 1, "max": 2}, "weight": 6}
]},
{"level": 4, "type": "WEAPONS", "each": true, "quantity": 1, "loot": [
    {"data": {"name": "gregguns:m4", "meta": 0}, "weight": 2},
    {"data": {"name": "gregguns:aug", "meta": 0}, "weight": 1}
]}
```

### EnhancedLootBags

EnhancedLootBags gives quest rewards as bags; each bag draws from a tier group.

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/EnhancedLootBags/LootBags.xml`.
  - Tier groups: `dreamcraft.lootbag.group.steam_age` (id 2), `tier_1_lv` (4), `tier_2_mv` (5), `tier_3_hv` (6), `tier_4_ev` (7), `tier_5_iv` (8), `tier_6_luv` (41), `tier_7_zpm` (42), `tier_8_uv` (43), `tier_9_uhv` (44). Each draws 1 to 2 items.
  - Sums of `Chance` as shipped: steam_age 4367, tier_1_lv 4943, tier_2_mv 3180, tier_3_hv 3715, tier_4_ev 3060, tier_5_iv 1600, tier_6_luv 1375, tier_7_zpm 1405.
  - A line is `<Loot Identifier="<unique GUID>" ItemName="modid:name[:meta]" Amount="N" NBTTag="" Chance="N" LimitedDropCount="0" RandomAmount="false"/>`. Existing lines use names like `gregtech:gt.metaitem.01:32601`.
  - The pack already puts IC2 jetpacks and the Hazmat helmet in these bags, so adding powered gear fits.
- **Source:** [`LootGroups`](https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/serializer/LootGroups.java#L88): `Chance` is a relative weight, summed per group. `ItemName` goes through `ItemDescriptor.fromString` ([L189](https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/serializer/LootGroups.java#L189)). `ItemGroup` and `LimitedDropCount` are read but their rules are not verified here.
- **GregGuns does:** nothing in code.
- **Pack-side change:** add `Loot` lines to these groups. Weights are about 1% to 2% per line of the group sum.

| Group | GregGuns items | `Chance` |
|---|---|---|
| `steam_age` | Hand Cannon; stone bullets (Amount 16) | 60; 60 |
| `tier_1_lv` | 9mm Pistol; Revolver; Combat Knife; Crowbar; 9mm bullets (Amount 16); shotgun shells (Amount 16) | 50; 50; 40; 40; 60; 60 |
| `tier_2_mv` | Thompson; M4; Combat Shotgun; Medkit; Flashbang (Amount 4); SMG magazine (Amount 2) | 40; 30; 30; 50; 50; 50 |
| `tier_3_hv` | AUG; Vector; LMG magazine (Amount 2); rocket (Amount 4); jump pack | 40; 40; 40; 40; 30 |
| `tier_4_ev` | Pulse Rifle; PDW; Laser Rifle; advanced magazine (Amount 2); energy cell | 40; 40; 30; 40; 40 |
| `tier_5_iv` | Blaster Rifle; Scatter Laser Rifle; Golden Revolver | 20; 20; 20 |
| `tier_6_luv` | Nether Blaster; Alien Blaster; Nether Charge (Amount 8) | 20; 20; 20 |
| `tier_7_zpm` | Nuclear Death Ray; Nuke Launcher; Nuclear Power Cell | 15; 15; 15 |

Tier assignment is [D1](../../design/0001-tier-map.md). The IV to ZPM sums are small (1375 to 1600), so the `Chance` values there are about 1% to 4% per line.

### Infernal Mobs

Infernal Mobs turns some hostile mobs into elite, ultra or infernal variants, and those drop items from three **global** lists.

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/InfernalMobs.cfg`.
  - `eliteRarity=20`, `ultraRarity=10`, `dimensionIDBlackList=50`.
  - **`useSimpleEntityClassnames=true`**: the `permittedentities` and `entitiesalwaysinfernal` keys are the entity's **simple Java class name**, not the registered name.
  - `permittedentities` defaults to `true` for any class it has not seen (it writes the key on first sight).
  - `droppedItemIDsElite`, `droppedItemIDsUltra` and `droppedItemIDsInfernal` hold the lists. Shipped entries are vanilla items, for example `iron_sword`, `diamond_sword`, `ender_pearl`, `enchanted_book`.
- **Source** ([InfernalMobsCore.java](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java)):
  - Eligibility, [L411-L419](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L411-L419) and [L438-L451](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L438-L451): the entity must be `IMob` and not `IEntityOwnable`, and `permittedentities.<name>` must be true.
  - Trigger, [`EntityEventHandler.onEntityJoinedWorld`](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L69-L82) and [`processEntitySpawn` L382+](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L382-L400): the roll happens whenever an `EntityLivingBase` **joins the world**, so structure-spawned and egg-spawned NPCs are rolled like natural ones. Dimensions in the blacklist are skipped.
  - List syntax, [`parseItemsForList` L338-L360](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L338-L360): `ID-meta-stackSize-randomizer`, split on `-`, everything after `ID` optional. The final stack size is `stackSize + nextInt(randomizer + 1)`.
  - Name lookup, [`parseOrFind` L362+](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L362-L376): the item registry first, then the block registry. A registry name containing `-` can never be listed.
  - Drops, [`dropRandomEnchantedItems` L662-L683](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L662-L683): one roll per 5 points of modifier size, tier by size (up to 5 elite, up to 10 ultra, above that infernal), each roll picks a list entry with equal odds. Non-book items go through `enchantRandomly`, which null-checks the enchantment list.
  - Death path, [`MobModifier` L171](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/modifiers/MobModifier.java#L171) calls `dropLootForEnt`; [`getModSize` L352](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/modifiers/MobModifier.java#L352).
- **GregGuns does:**
  - GregGuns hostile NPC classes implement `IMob` and not `IEntityOwnable`, so they are eligible with no work.
  - **Class naming (for area 10).** The pack config already holds `EntityAlienBug=true`, a key for **Amun-Ra's** class `de.katzenpapst.amunra.mob.entity.EntityAlienBug` (found in `reference/gtnh/2.9.0-RC-2/.minecraft/mods/AmunRa-GC-0.8.14.jar`). With `useSimpleEntityClassnames=true`, two classes with one simple name share one switch. GregGuns NPC classes need unique simple names, for example a `GG` prefix.
  - Bosses (Cyberdemon, Alien Bug) are gated by config under D5. Whether they may roll infernal is the pack's call through `permittedentities`.
- **Pack-side change (route A, global lists):** append GregGuns entries to the three lists. Entries are equally likely, so each added entry takes `1 / (list length + added)` of each roll. The lists are global: they also fire for vanilla zombies and every other infernal mob, which is the "GregGuns in GTNH loot" the user wants. They are not tied to location, so cap the tiers by rarity.

| List | Existing length | Add | Tier |
|---|---|---|---|
| `droppedItemIDsElite` | 26 | `gregguns:ammo_9mm_bullet-0-6-6`, `gregguns:ammo_shotgun_shell-0-4-4`, `gregguns:mag_9mm_pistol`, `gregguns:combat_knife`, `gregguns:grenade_frag-0-2-2` | LV, MV |
| `droppedItemIDsUltra` | 21 | `gregguns:mag_smg`, `gregguns:mag_assault_rifle`, `gregguns:medkit`, `gregguns:flashbang-0-2-2`, `gregguns:night_vision_goggles` | MV |
| `droppedItemIDsInfernal` | 17 | `gregguns:mag_lmg`, `gregguns:ammo_40mm_grenade-0-3-3`, `gregguns:thompson`, `gregguns:m4`, `gregguns:aug` | MV, rare HV |

  Because the lists are global, D5's "LV-MV for surface structures" rule does not strictly cover infernal HV drops. See Open questions.

#### Infernal Mobs handler

**Route B (soft dependency, our NPCs only).** If the pack does not want guns dropping from vanilla infernals, GregGuns drops tiered items from its own NPCs and leaves the lists alone.

- **Source:** public static [`getIsRareEntity`](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L626-L629) and [`getMobModifiers`](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L621-L624). Infernal's own `LivingDropsEvent` handler calls `removeEntFromElites` after it runs ([L222-L236](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L222-L236)).
- **GregGuns does:** a `LivingDropsEvent` handler, registered only when `Loader.isModLoaded("InfernalMobs")` and kept in a class that is not loaded otherwise. It runs at `EventPriority.HIGH` so the entity is still in Infernal's map (Infernal's handler removes it at normal priority). For a GregGuns NPC that `getIsRareEntity` reports as rare, it takes `getMobModifiers(ent).getModSize()`, maps it to elite (up to 5), ultra (up to 10) or infernal, and adds `EntityItem`s from a GregGuns drop table for that tier (one roll per 5 points, as Infernal does). Tables and the on/off switch are GregGuns config. The tables follow [Tier bands](#tier-bands).
- **Pack-side change:** none. If route B is on, leave the three lists as shipped, or the NPC may drop twice (once from the list at death, once from us).

### GTNH CustomDrops

The pack has its own per-entity drop file.

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GTNewHorizons/CustomDrops.xml`, enabled by `B:CustomDrops=true` in `dreamcraft.cfg`. 15 `CustomDrop` entries as shipped. A drop is `<Drop Identifier ItemName Amount NBTTag Chance LimitedDropCount RandomAmount/>` under `<CustomDrop EntityClassName="…">`.
- **Source:**
  - [`CustomDropsHandler.onMobDrops`](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modcustomdrops/CustomDropsHandler.java#L173-L212): drops apply **only when a real player kills the mob** (no fake players).
  - [`CustomDrops.FindDropEntry`](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modcustomdrops/CustomDrops.java#L27-L41): matches the **full class name**, case-insensitive, with no subclass match.
  - [`provideExtraDropsInformation`](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modcustomdrops/CustomDropsHandler.java#L205-L225) feeds the drops to Mobs Info for NEI.
- **GregGuns does:** nothing in code.
- **Pack-side change:** optional route C. For mobs where a fixed, player-kill-only drop is wanted (for example a mini-boss dropping a unique gun), add a `CustomDrop` with the NPC's full class name. Use it for a boss, not for tiered infernal loot.

### Mobs Info

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/mobsinfo/mobsinfo.cfg`: `enableInfernalDrops=true`, `Enabled=true`, `MobBlacklist` (Special Mobs variants and others; no GregGuns entries needed).
- **Source:** [Mobs-Info 0.6.0-GTNH](https://github.com/GTNewHorizons/Mobs-Info/tree/0.6.0-GTNH): the repository has `com.kuba6000.mobsinfo.nei.MobHandlerInfernal` (the NEI page) and an `InfernalMobsCoreAccessor` mixin on Infernal's core class, so route A entries should show up on the "Infernal Drops" NEI page. This is inferred from the class names and the `enableInfernalDrops` key; the list-reading code was not read. `IMobExtraInfoProvider` is the API CustomDrops uses.
- **GregGuns does:** nothing for route A or C. For route B (our own handler), the drops are invisible in NEI unless GregGuns implements `IMobExtraInfoProvider`, a soft dependency on `mobsinfo`. That is an option for M7, not a requirement.
- **Pack-side change:** none.

## Requirements

- **GGS-06-01.** GregGuns adds no item to a vanilla `ChestGenHooks` category from code. Vanilla-category loot comes only from pack `ADD` files.
- **GGS-06-02.** Every GregGuns structure chest category is named `gregguns:<name>` and is registered at init through `ChestGenHooks`.
- **GGS-06-03.** Material entries in GregGuns chests are built with `GTOreDictUnificator.get(...)` and are omitted when the lookup returns `null`.
- **GGS-06-04.** No GregGuns loot entry, in code or in the shipped draft files, is above the tier band for its surface in [Tier bands](#tier-bands).
- **GGS-06-05.** Every shipped draft `ADD` file spells `loading_mode="ADD"`, and a unit test parses each draft file and fails on any other mode.
- **GGS-06-06.** GregGuns item registry names contain no `-` character, so they can be written in an Infernal Mobs drop list.
- **GGS-06-07.** GregGuns hostile NPC classes implement `IMob`, do not implement `IEntityOwnable`, and have simple class names that are unique in the pack (no `EntityAlienBug`).
- **GGS-06-08.** The Infernal Mobs drop handler is inert when `InfernalMobs` is not loaded, and runs at `EventPriority.HIGH`.
- **GGS-06-09.** The Infernal Mobs drop handler can be switched off in config, and tier tables are config.

## Tests

| ID | Verified by |
|---|---|
| GGS-06-01 | Unit test: after init, every vanilla category's item count is unchanged from the baseline table |
| GGS-06-02 | Unit test: the seven category names exist in `ChestGenHooks` after init |
| GGS-06-03 | Unit test with GT absent from the lookup: no `null` stack is added |
| GGS-06-04 | Dev self-check: each loot entry's D1 tier is at or below its surface's band |
| GGS-06-05 | Unit test on the draft files (kept under `src/test/resources`) |
| GGS-06-06 | Unit test over the item registry |
| GGS-06-07 | Unit test over the entity registry |
| GGS-06-08 | `runServer` boot with and without Infernal Mobs; manual kill of an infernal NPC with `/loot`-style debug logging |
| GGS-06-09 | Unit test on config defaults |
| Draft files | Manual: copy into `config/TooMuchLoot/loot/`, run `/loot reload`, then `/loot spawnDebugChest <category>` |

## Open questions

- **Infernal HV drops.** D5 says "LV-MV by default for surface structures", but the global Infernal lists are not tied to location. Does the infernal list stay at MV, or may it carry rare HV items? A short addition to [0005](../../design/0005-world-presence.md) would settle it.
- **Route A or route B.** Do vanilla zombies and skeletons drop GregGuns items when infernal (route A), or only our NPCs (route B)? The user asked for GregGuns items in GTNH loot, so route A is the default here, but route B is safer for balance.
- **Gun stack state.** A Techguns gun at damage 0 is fully loaded. Whether a looted GregGuns gun comes loaded or empty is a game-design choice for M2. The drafts use `damage="0"`.
- **Registry names.** Every `gregguns:` name above is a placeholder until M2 fixes the registry.
- **Tier bands for chests.** The bands are a proposal and need the lead's sign-off.
