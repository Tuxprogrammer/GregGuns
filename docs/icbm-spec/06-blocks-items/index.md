# 06-blocks-items

## Overview

The small blocks and items of ICBM-Classic that are not machines, weapons or warheads, plus the catalog that ties the whole mod together: the **antidote** (the one item GregGuns keeps), the content GregGuns **drops** (concrete, reinforced glass, the battery, the ingots, plates, wires and circuits, sulfur and saltpeter, poison powder) or **defers** (spikes, the glass button and the glass pressure plate), a **registry catalog** of every item, block, tile entity and entity with the area that documents it, and the **recipe catalog**: every one of the 129 recipe files, the smelting recipes and the code-registered recipes, with exact grids, counts, alternates and conditions. The recipe catalog is the table the GregGuns side remaps onto GT recipe maps. Source: ICBM-Classic `prod/1.12` @ `9c1d985a`; cite classes relative to `src/main/java/icbm/classic/` and recipes relative to `src/main/resources/assets/icbmclassic/recipes/`. The owner decisions behind the split are in [D10](../../gregguns-icbm-spec/DECISIONS.md#d10-in-detail).

## Contents

- [antidote.md](antidote.md): the full sheet for the antidote: use, cure scope, recipe, the contract a GT-hazards rebuild must keep.
- [dropped-and-deferred.md](dropped-and-deferred.md): compact sheets for concrete, reinforced glass, the glass button, the glass pressure plate, spikes, the battery, the five crafting-item families and the four powders.
- [registry-catalog.md](registry-catalog.md): 27 items, 14 blocks, 10 tile entities and 17 entities with registry names, classes, metadata, config gates and owning areas; remaps; oredict names; code registrations.
- [recipes.md](recipes.md): every recipe, one row each, grouped by area, with a loader-facts section, the variants that would be live in a GT pack, and a table of which recipes need which ICBM crafting items.

## Key facts

- **The explosive-type recipes always produce 1.** `ExRecipeFactory` reads the count from the top of the JSON, but every file puts it inside `result`. Condensed (listed 3), anvil (10), smoke (16), breaching (2), contagious (2) and debilitation (3) all yield 1. The `02-explosives` sheet that lists condensed as "3" repeats the JSON, not the game (ICBM-06-02).
- **ICBM's own crafting items are a hidden dependency of nearly everything.** Steel ingots, steel plates, the three circuits, copper and gold wire, and the battery are named by the launcher, radar, EMP, missile, bomblet and tool recipes ([demand table](recipes.md#material-demand-by-ore-name)). Dropping them means every one of those recipes needs a GT equivalent.
- **Four of the eight chest-loot entries for those items add nothing.** `ItemCrafting.getStack` skips subtype index 0, so steel ingot, steel plate, copper wire and basic circuit return an empty stack (ICBM-06-01).
- **The antimatter, red matter and no-uranium nuclear recipes may be uncraftable without Mekanism.** Their "vanilla" variants use `minecraft:item_exists` with a `condition` field Forge does not read (ICBM-06-03, believed, not verified against the Forge source).
- **The antidote clears every potion effect**, buffs included, and is spent even when there is nothing to cure. It is 6 doses for 9 seeds (ICBM-06-07).
- **The battery is a recipe ingredient only.** No ICBM machine uses its energy, and it cannot be drained ([energy-model.md](../01-core/energy-model.md#5-the-battery-item)).
- **Concrete's blast resistance is on the wrong scale.** 28 / 280 / 2800 are returned as-is where vanilla blocks return `resistance * 3 / 5`; reinforced concrete is 2800 against obsidian's 1200 (ICBM-06-08).
- The iron plate is unobtainable (no recipe, no loot) and only smelts to an iron ingot (ICBM-06-10). The laser detonator grid is a diagonal, not the column the pilot sheet shows.
- Recipe JSON, conditions, ingredient factories, capabilities on the battery, blockstates and registry events are *1.12-only mechanisms*. The sheets describe the behavior they produce.

## Related areas

| Area | Why |
|---|---|
| `01-core` | Config flags (`ConfigItems`, `ConfigBattery`, `ConfigBlocks`), lifecycle and load order, loot injection, the energy model and battery capability, the creative tab |
| `02-explosives` | The explosive IDs used as metadata in recipes, the explosive block and its tile, the gas effects the antidote cures, radioactive blocks, the effect entities in the entity catalog |
| `03-missiles` | Missile, SAM, cluster, held-item and bomblet items; grenade, bomb cart, rocket launchers, cargo and seat; the dynamic code recipes |
| `04-launch-control` | Launcher base, frame, connector, screen and cruise launcher recipes and registry entries |
| `05-detection-defense` | Radar station, EMP tower and the four handheld tools: their recipes (two grids in the pilot sheets are wrong, see Coverage) and registry entries |
| `07-visuals-audio` | Every texture, model, blockstate and lang key; this area lists its own at catalog depth |

## Coverage

Source files read in full: 18 of 18 in scope, plus the recipe data.
- `content/blocks/` top level: `BlockConcrete`, `BlockGlassButton`, `BlockGlassPressurePlate`, `BlockReinforcedGlass`, `BlockSpikes` (5 of 5); `config/blocks/ConfigSpikes`, `ConfigBlocks` (in full).
- `content/items/`: `ItemAntidote`, `ItemBattery`, `ItemCrafting` (3 of 3).
- `content/reg/`: `ItemReg`, `BlockReg`, `EntityReg` (3 of 3; `ExplosiveInit` is [02](../02-explosives/explosive-registry.md)). `api/refs/ICBMEntities` (in full).
- `RecipeGeneration.java`, `lib/recipe/` (`ConditionOreValue`, `ExIngredientFactory`, `ExRecipeFactory`): 4 of 4.
- Recipe JSON: all 129 recipe files and `_factories.json`, parsed with a script and cross-checked row by row.

Also read, to resolve references: `config/ConfigItems`, `config/ConfigBattery`, `prefab/item/ItemBase`, `ItemICBMBase`, `ItemBlockSubTypes`, `LootEntryItemStack`, `client/ICBMCreativeTab`, the loot and recipe code in `ICBMClassic.java`, `en_US.lang` item and tile keys, the blockstates and item models of the 06 blocks and items, PNG headers of the 06 textures, and the 01, 02, 03 and 05 sheets for cross-checks.

Assets at catalog depth: textures (items 16 at 16 x 16; blocks `concrete*.png`, `glass_reinforced.png`, `spikes*.png` at 16 x 16; `glass_button.png` and `glass_pressure_plate.png` at 32 x 32), item models, blockstates, lang keys (in the sheets).

Corrections to other sheets, found while checking recipe grids against the JSON:
- [05 handheld-tools](../05-detection-defense/handheld-tools.md): the radar gun's second row is `_bw` (empty, stone button, planks), not `b w .`; the laser detonator's grid is the diagonal `r__/_c_/__h`, not one column.
- [02 explosive-registry](../02-explosives/explosive-registry.md) (and any 02 or 03 sheet quoting the same counts): the "result count" column shows the JSON number, not the in-game yield (ICBM-06-02).

## Known limits

- No code was run. Everything about recipe loading comes from reading the JSON and the three factory classes. Forge's recipe loader, condition classes and `clearActivePotions` are not in the checkout; statements about them (ICBM-06-03, ICBM-06-17, the antidote's per-effect event) are from knowledge of Forge 1.12.2 and marked unverified.
- Whether the oredict names `ingotUranium` and `pelletUranium235` exist in a GT pack, and the exact GT names that would supply `circuitBasic` and friends, are expectations (see the variants table in [recipes.md](recipes.md#effective-variants-in-a-gtnh-style-pack)).
- Sounds, textures and models could not be seen or heard; descriptions come from names, JSON and use.
- Spike hit rate (about two per second) comes from the vanilla invulnerability window, not a measured value.
- Mining speed and harvest requirements of concrete, spikes and reinforced glass come from material and tool rules, not from a run.

## Suspected bugs

"Logic" means we could inherit it in a rebuild. "1.12-only" means it does not matter to a 1.7.10 rebuild.

| ID | Title | Location | What looks wrong | Severity | Kind |
|---|---|---|---|---|---|
| ICBM-06-01 | `ItemCrafting.getStack` never finds index 0 | `content/items/ItemCrafting.java:102`; callers `ICBMClassic.java:213,217,221,226` | The test is `index > 0` instead of `>= 0`, so steel ingot, steel plate, copper wire and basic circuit (all index 0) return an empty stack. The four matching chest-loot entries add nothing (`LootEntryItemStack` skips empty stacks). Copper ingot, gold wire, advanced and elite circuits work | gameplay | logic |
| ICBM-06-02 | Explosive-type recipes ignore their listed count | `lib/recipe/ExRecipeFactory.java:35`; the `result.count` of 13 recipes | The factory reads `count` from the top-level JSON, never from `result`. Condensed 3, anvil 10, smoke 16, breaching 2, contagious 2 and debilitation 3 all craft 1 | gameplay | logic (data); the loader is 1.12-only |
| ICBM-06-03 | `item_exists` ignores the `condition` field (believed) | `recipes/explosives/vanilla/antimatter.json`, `vanilla/redmatter.json`, `vanilla/nuclear.json`, `mekanism/*.json` | The "vanilla" variants are written as "alloy absent" with `condition: false`, but Forge's `minecraft:item_exists` has no such field, so it passes only when the alloy exists. Without Mekanism the antimatter, red matter and no-uranium nuclear recipes would never load; with it, both variants of antimatter and red matter are live. Not verified against the Forge source | gameplay | 1.12-only, unverified |
| ICBM-06-04 | Missile module needs an undamaged flint and steel | `recipes/missile/missile.module.json:22-25` | The ore-dictionary variant names flint and steel with `data` 0, which matches only damage 0; the fallback variant uses the 32767 wildcard. The tool is consumed. In a pack that supplies the circuit and steel names (the normal case) a used flint and steel does not work | gameplay | logic |
| ICBM-06-05 | Recipes hard-code legacy numeric explosive IDs | 16 recipe files: 14 under `recipes/explosives/` and the 2 surface-to-air missile recipes under `recipes/missile/` (raw `icbmclassic:explosives` with `data`) | The IDs are persisted in `explosive_reg.json` and can be renumbered; a recipe that says meta 7 then means whatever has ID 7. The `icbmclassic:explosive` ingredient type (by name) does not have the problem | gameplay | 1.12-only |
| ICBM-06-06 | Config flags do not gate recipes | `config/ConfigItems.java`; `content/reg/ItemReg.java:160-182` | Disabling a crafting family or the battery leaves its recipes in place. Recipes naming the items directly (EMP explosive, EMP tower and extender) fail to load; ore-name recipes silently never match | gameplay | 1.12-only |
| ICBM-06-07 | Antidote strips beneficial effects and is spent with nothing to cure | `content/items/ItemAntidote.java:21,43-48` | The use always starts, and the finish clears every active effect, buffs included. Nothing checks for a harmful effect | gameplay | logic |
| ICBM-06-08 | Concrete explosion resistance is on the wrong scale | `content/blocks/BlockConcrete.java:69-83` | 28 / 280 / 2800 are returned directly, where vanilla returns `resistance * 3 / 5` (obsidian is 1200, not the 2000 the comment cites). Reinforced concrete is 2.3 times obsidian and normal concrete is about 5 times stone. The blast engine charges this value per half-step ([blast-engine.md](../02-explosives/blast-engine.md)) | gameplay | logic |
| ICBM-06-09 | Item ingredients without `data` match meta 0 only | `recipes/balloon.json`, `parachute.json`, `explosives/anvil.json` | Carpet is white only (balloon, parachute); the anvil explosive needs eight undamaged anvils (a used anvil is chipped or damaged and does not match) | gameplay | 1.12-only |
| ICBM-06-10 | Iron plate is unobtainable | `ICBMClassic.java:153`; `content/reg/ItemReg.java` (plate "iron") | A registered, named, smeltable item with no crafting recipe and no loot entry. Smelting it gives an iron ingot at 0 xp (the code comment calls it "a fix for removing recipe of plate") | cosmetic | logic |
| ICBM-06-11 | Reinforced glass: the silk-touch flag does nothing | `content/blocks/BlockReinforcedGlass.java:28-31` | `canSilkHarvest` is true, but the class extends plain `Block`, which always drops itself (vanilla glass overrides the drop count to zero). Silk touch is irrelevant and the block never "breaks" like glass | cosmetic | logic |
| ICBM-06-12 | `RecipeGeneration` is dead code | `RecipeGeneration.java:16-160` | Three static generators for cart, grenade and missile JSON write into the current directory; nothing calls them. The checked-in files are the only source of truth | cosmetic | logic |
| ICBM-06-13 | Battery durability bar is inverted and never drawn | `content/items/ItemBattery.java:63-73` | `getDurabilityForDisplay` returns stored / max (a full battery would show an empty bar), but the item is never damaged so vanilla never draws the bar. The tooltip percent uses the same number and is right | cosmetic | 1.12-only |
| ICBM-06-14 | Glass button and plate sit in the Redstone tab | `content/blocks/BlockGlassButton.java`, `BlockGlassPressurePlate.java:24` | Every other ICBM block is in the ICBM tab; these two are in Redstone (the button through the vanilla base class) | cosmetic | 1.12-only |
| ICBM-06-15 | Registry names written in mixed case | `content/reg/ItemReg.java`, `BlockReg.java`, item and block constructors | `radarGun`, `glassButton`, `poisonPowder` and others are written with capitals and lowercased by 1.12. Recipes and the lang files use both spellings (`poisonPowder` and `poisonpowder`). 1.7.10 does not lowercase | cosmetic | 1.12-only |
| ICBM-06-16 | Entities have no English names | `content/reg/EntityReg.java:81-88`; `en_US.lang` | The entity entry name is the full `icbmclassic:...` string and there are no `entity.*` keys, so anything that prints an entity name shows the raw key | cosmetic | 1.12-only |
| ICBM-06-17 | Recipe variants depend on when conditions are evaluated (unverified) | `lib/recipe/ConditionOreValue.java:21-40`; `ICBMClassic.java:170-188` | The oredict conditions are checked as the recipes load. ICBM registers its own `circuitBasic` and `ingotSteel` at highest priority on the recipe event, but whether that is before its own JSON loads, and before other mods' `dustCarbon` and `ingotUranium`, was not verified. A wrong order would pick the vanilla variants (redstone block, iron ingots) over the steel ones | gameplay | 1.12-only, unverified |
| ICBM-06-18 | Spike damage cannot be set below 1 | `config/blocks/ConfigSpikes.java:8,12,16` | All three damage keys have a minimum of 1, so spikes cannot be made harmless or non-lethal for a hazard-free build; poison and fire spikes also deal as much as normal ones by default | cosmetic | logic |
| ICBM-06-19 | Lang keys for blocks that no longer exist | `en_US.lang` | `missilecoordinator`, `camouflage`, `proximityDetector` and `batterybox` have names but no registration; the clump, wire, plate and circuit family-level keys have no text either | cosmetic | logic |

Minor, not given IDs: `BlockSpikes` re-reads the block state from the world instead of using the one passed in; `glass_button.png` and `glass_pressure_plate.png` are 32 x 32 where every other 06 texture is 16 x 16; `ItemBattery` declares subtypes it never defines; `ItemBlockSubTypes` shows the raw key for a metadata value with no lang entry; the unconditional second `dustSulfur`/`dustSaltpeter` registration is ICBM-01-19; the loot-count rule and private random are ICBM-01-27; the battery's missing discharge is ICBM-01-03.
