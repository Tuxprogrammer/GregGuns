# Recipe Catalog

Every crafting, smelting and code-registered recipe in ICBM-Classic `prod/1.12` @ `9c1d985a`, one row each. This is the table the GregGuns side remaps onto GT recipe maps, so it records exact grids, counts, alternates and enabling conditions. Source: the 129 recipe JSON files under `src/main/resources/assets/icbmclassic/recipes/` (plus `_factories.json`), `lib/recipe/` (`ConditionOreValue`, `ExIngredientFactory`, `ExRecipeFactory`), `RecipeGeneration.java`, and the recipe code in `ICBMClassic.java`, cited relative to `src/main/java/icbm/classic/` and `recipes/`. Which items GregGuns keeps is in [D10](../../gregguns-icbm-spec/DECISIONS.md#d10-in-detail); the remap itself is in the GregGuns spec, not here.

## How to read the tables

- **Grid** is written row by row, top to bottom, rows separated by `/`; `_` is an empty slot. A letter is defined in the **Keys** cell of the same row. Shaped means position matters, but Forge shaped recipes also match the mirrored grid, so `ci` and `ic` are the same recipe.
- `[name]` means the **explosive block item** `icbmclassic:explosives` whose metadata is that explosive's ID ([explosive-registry.md](../02-explosives/explosive-registry.md#2-the-catalog)). The JSON writes it two ways (a raw item with a meta number, or an `icbmclassic:explosive` ingredient with a registry name); they mean the same item. `[name] cart`, `[name] grenade`, `[name] missile` are the same explosive in the other device forms.
- `ore X` is an oredict name: any item registered under it matches. A plain item name is an exact match.
- **Meta rule (important):** a JSON item ingredient with no `data` field matches **only damage value 0**. `minecraft:carpet` is white carpet only, `minecraft:anvil` is an undamaged anvil only, `minecraft:flint_and_steel` without `data` is an undamaged one (ICBM-06-04, ICBM-06-09). `data` 32767 is the wildcard (any dye, any flint and steel).
- **Count** shows the number in the JSON. For recipes of type `icbmclassic:explosive` the in-game yield is **always 1**, whatever the JSON says (ICBM-06-02); those rows show `listed -> effective`.
- **Enabled when** is the recipe's own condition. Blank means unconditional. Conditions are evaluated when the recipe loads (ICBM-06-17).
- Container items: crafting ingredients leave their container item behind only if the item defines one. Water and lava buckets leave an empty bucket (vanilla), flint and steel and shears have none and are **consumed**.
- Config flags do not gate any recipe (ICBM-06-06). If an ICBM item family is disabled, recipes that name the item directly (`icbmclassic:battery`, `icbmclassic:wire`, `icbmclassic:radargun` ...) fail to load; recipes that use its oredict name simply never match unless another mod supplies the name.
- 1.12 mechanisms here (recipe JSON loader, conditions, ingredient factories) are *1.12-only mechanisms*; the grids and counts are the behavior.

## Loader facts

| Piece | Behavior |
|---|---|
| `recipes/_factories.json` | Declares the condition `icbmclassic:ore_value`, the ingredient type `icbmclassic:explosive` and the recipe type `icbmclassic:explosive` |
| Condition `icbmclassic:ore_value` (`lib/recipe/ConditionOreValue`) | Fields `value` (an oredict name) and `condition` (the string "true" or "false"). With "true": passes when the name has at least one non-empty stack. With "false": passes when the name has none |
| Condition `minecraft:item_exists` (Forge) | Used with `item` = `mekanism:atomicalloy`. The recipes also carry a `condition` field that Forge's condition does not read (ICBM-06-03) |
| Conditions `forge:and`, `forge:or` | Combine the above |
| Ingredient `icbmclassic:explosive` (`ExIngredientFactory`) | Fields `explosive` (registry name) and optional `device` (default `icbmclassic:block`). Resolves to the device stack of that explosive (block, grenade, minecart, missile). The special device `icbmclassic:missile.module` resolves to explosive missile meta 24; the special device `icbmclassic:explosive_bomblet` resolves through the grenade registry's meta. An unknown explosive or device throws and the recipe is skipped |
| Recipe `icbmclassic:explosive` (`ExRecipeFactory`) | Resolves the result stack the same way, rewrites the result to a plain item and meta, then delegates to Forge's shaped ore recipe. The count comes from the **top level** of the JSON, which no file sets, so it is always 1 (ICBM-06-02). NBT results are a TODO |
| `RecipeGeneration` | A developer helper that writes the cart, grenade and missile JSON into a folder relative to the working directory. Nothing calls it (ICBM-06-12) |

## Effective variants in a GTNH-style pack

Several recipes have alternates chosen by which oredict names exist. A GT pack supplies `circuitBasic`, `ingotSteel`, `dustSulfur`, `dustSaltpeter` and the carbon dusts, and has no Mekanism. If the GT names behave as expected, the live variant of each pair is:

| Recipe | Variant that would load | Why |
|---|---|---|
| Missile module | the steel and circuit version (`missile/missile.module.json`) | both `circuitBasic` and `ingotSteel` exist |
| SAM | the circuit version (`missile/special/missile.anti_ballistic.json`) | `circuitBasic` exists |
| Gunpowder | the dust version (`parts/mod/gunpowder.json`) | a carbon dust exists |
| Nuclear explosive | the uranium version (`explosives/nuclear.json`) | `ingotUranium` or `pelletUranium235` exists. If neither exists, the other two nuclear rows need the alloy item to exist (ICBM-06-03), so with no Mekanism there would be no nuclear recipe |
| Antimatter, red matter | the nether-star versions, but only if the `condition` field of `item_exists` is honored | if it is not honored they are uncraftable without Mekanism (ICBM-06-03) |

These are expectations from the conditions, not run results.

## Material demand by ore name

What the recipes below need from ICBM's own items (all of which GregGuns drops, so each must be supplied by GT). Counts are per craft, from the grids.

| Ore name | Needed by (count per craft) |
|---|---|
| `ingotSteel` | steel plate 4; missile module 5; empty bomblets 5; launcher base 2; launcher connector 4; launcher screen 1; cruise launcher 2; remote detonator 6; radar gun 1; rocket launcher 3 |
| `plateSteel` | launcher base 3; cruise launcher 1; EMP tower 4; tower extender 4; radar station 2 |
| `circuitBasic` | radar gun, defuser, launcher base, launcher screen, missile module, empty bomblets, SAM, cluster missile, held-item missile, rocket launcher (1 each); advanced circuit 1; elite circuit 1 |
| `circuitAdvanced` | remote detonator 1; cruise launcher 1; radar station 2 |
| `circuitElite` | laser detonator 1; EMP tower 1 |
| `wireCopper` | launcher connector 4; launcher screen 1; rocket launcher 1; advanced circuit 4 |
| `wireGold` | EMP tower 1; tower extender 1; elite circuit 4 |
| `ingotCopper` | battery 1; defuser 2; advanced circuit 2; basic circuit 3; copper wire 6 |
| `ingotGold` | radar station 1; gold wire 6; elite circuit 2 |
| `ingotIron` | clump 1; battery 1; reinforced concrete 4; reinforced glass 5; spikes 3; launcher frame 4; sonic explosive 4; compact-to-reinforced concrete 4 |
| `icbmclassic:battery` | EMP tower 2; tower extender 2; EMP explosive 1 |
| `icbmclassic:wire` (copper, item-keyed) | EMP explosive 5 |
| `dustSulfur` | battery 2; incendiary explosive 7; debilitation explosive 6; fire spikes 1; gunpowder 1 |
| `dustSaltpeter` | gunpowder 1 |
| `icbmclassic:poisonpowder` | chemical explosive 8; poison spikes 1 |

## 06 own recipes

Powders, crafting items, battery, blocks, antidote and smelting. Item details are in [dropped-and-deferred.md](dropped-and-deferred.md) and [antidote.md](antidote.md).

| Output (count) | Grid | Keys | Alternates | Enabled when | File |
|---|---|---|---|---|---|
| Antidote (6) | `sss/sss/sss` | s = melon seeds, pumpkin seeds or wheat seeds (each slot independent) | none | | `antidote.json` |
| Poison powder (3) | `er` | e = spider eye, r = rotten flesh | none | | `powder.poison.json` |
| Saltpeter starter (4) | `DPD/PWP/DPD` | D = ore `dirt`; P = ore `treeLeaves` or ore `treeSapling` (each slot independent); W = water bucket | none | | `parts/vanilla/saltpeter.json` |
| Gunpowder (3), shapeless | carbon + sulfur + saltpeter | carbon = ore `dustCarbon`, `dustCharcoal` or `dustCoal`; ore `dustSulfur`; ore `dustSaltpeter` | the coal-item version below | a carbon dust exists (`dustCarbon` or `dustCharcoal` or `dustCoal`) | `parts/mod/gunpowder.json` |
| Gunpowder (3), shapeless | coal + sulfur + saltpeter | coal (meta 0) or charcoal (meta 1); ore `dustSulfur`; ore `dustSaltpeter` | the dust version above | none of `dustCarbon`, `dustCharcoal`, `dustCoal` exists | `parts/vanilla/gunpowder.json` |
| Steel clump "Coal Encased Iron" (1) | `ic/cc` | i = ore `ingotIron`; c = coal or charcoal (each slot independent) | none | | `parts/clump.steel.json` |
| Steel plate (4) | `ii/ii` | i = ore `ingotSteel` | none | | `parts/plate.steel.json` |
| Copper wire (18) | `cwc/csc/cwc` | c = ore `ingotCopper` (6); w = ore `plankWood` (2); s = ore `stickWood` (1) | none | | `parts/wire.copper.json` |
| Gold wire (18) | `gwg/gsg/gwg` | g = ore `ingotGold` (6); w = ore `plankWood`; s = ore `stickWood` | none | | `parts/wire.gold.json` |
| Basic circuit (4) | `rcr/iii/bbb` | r = redstone; c = clay ball; i = ore `ingotCopper`; b = ore `plankWood` | none | | `parts/circuit.basic.json` |
| Advanced circuit (1) | `wcw/ibi/wcw` | w = ore `wireCopper`; c = ore `ingotCopper`; i = redstone torch; b = ore `circuitBasic` | none | | `parts/circuit.advanced.json` |
| Elite circuit (1) | `wcw/ibi/wcw` | w = ore `wireGold`; c = nether quartz; i = ore `ingotGold`; b = ore `circuitBasic` (not the advanced one) | none | | `parts/circuit.elite.json` |
| Battery (1) | `RCR/PPP/SIS` | R = redstone; C = ore `ingotCopper`; P = ore `paper`; S = ore `dustSulfur`; I = ore `ingotIron` | none | | `parts/battery.json` |
| Concrete (8) | `sgs/gwg/sgs` | s = sand (meta 0); g = gravel; w = water bucket | none | | `concrete.json` |
| Compact concrete (8) | `coc/oco/coc` | c = concrete meta 0; o = obsidian | none | | `concrete.1.json` |
| Reinforced concrete (8) | `coc/oco/coc` | c = compact concrete (meta 1); o = ore `ingotIron` | none | | `concrete.2.json` |
| Reinforced glass (8) | `igi/gig/igi` | i = ore `ingotIron` (5); g = glass (4) | none | | `reinforcedGlass.json` |
| Glass button (2) | `g/g/g` | g = glass | none | | `glassButton.json` |
| Glass pressure plate (1) | `ggg` | g = glass | none | | `glassPressurePlate.json` |
| Spikes (6) | `ccc/iii` | c = cactus; i = ore `ingotIron` | none | | `spikes.json` |
| Poison spikes (1) | `c/i` | c = poison powder; i = spikes meta 0 | none | | `spikes.1.json` |
| Flammable spikes (1) | `c/i` | c = ore `dustSulfur`; i = spikes meta 0 | none | | `spikes.2.json` |

Smelting (registered in `ICBMClassic.registerRecipes`; furnace default cook time):

| Input | Output | XP | Enabled when |
|---|---|---|---|
| Steel clump | Steel ingot | 0.1 | `enable_crafting_items` and `enable_ingots` |
| Iron plate | Vanilla iron ingot | 0 | `enable_crafting_items` and `enable_plates` |
| Saltpeter starter | Saltpeter | 0.1 | always |

## 02 explosive block recipes

The 23 explosives and their variants (27 files). Rows marked "type: explosive" go through `ExRecipeFactory` and yield 1 whatever is listed (ICBM-06-02). The raw-meta rows (`icbmclassic:explosives` with a number) use the explosive's legacy numeric ID (ICBM-06-05) and honor their counts. Explosive behavior and tiers: [explosive-registry.md](../02-explosives/explosive-registry.md).

| Output (count listed -> effective) | Grid | Keys | Alternates | Enabled when | File (`explosives/`) |
|---|---|---|---|---|---|
| Condensed (3 -> 1), type: explosive | `trt/rtr/trt` | t = TNT; r = redstone | none | | `condensed.json` |
| Shrapnel (1) | `aaa/ara/aaa` | a = arrow; r = [condensed] | none | | `shrapnel.json` |
| Incendiary (1) | `aaa/ara/ala` | a = ore `dustSulfur` (7); l = lava bucket; r = [shrapnel] | none | | `incendiary.json` |
| Debilitation (3 -> 1), type: explosive | `sss/wrw/sss` | s = ore `dustSulfur` (6); w = water bucket (2); r = [shrapnel] | none | | `debilitation.json` |
| Chemical (1), type: explosive | `ppp/prp/ppp` | p = poison powder (8); r = [debilitation] | none | | `chemical.json` |
| Anvil (10 -> 1), type: explosive | `aaa/ara/aaa` | a = anvil, undamaged (8); r = [repulsive] | none | | `anvil.json` |
| Repulsive (1) | `c/c` | c = [condensed] | none | | `repulsive.json` |
| Attractive (1), type: explosive | `cc` | c = [condensed] | none | | `attractive.json` |
| Colors (1), type: explosive | `CCC/CrC/CCC` | C = any dye (meta wildcard, 8); r = [repulsive] | none | | `colors.json` |
| Smoke (16 -> 1), type: explosive | `CCC/CrC/CCC` | C = charcoal (coal meta 1, 8); r = [repulsive] | none | | `smoke.json` |
| Fragmentation (1) | `_a_/ara/_a_` | a = [shrapnel]; r = [incendiary] | none | | `fragmentation.json` |
| Contagious (2 -> 1), type: explosive | `_c_/crc/_c_` | c = [chemical]; r = rotten flesh | none | | `contagious.json` |
| Sonic (1) | `ini/nrn/ini` | i = ore `ingotIron`; n = note block; r = [repulsive] | none | | `sonic.json` |
| Breaching (2 -> 1), type: explosive | `grg/grg/grg` | g = gunpowder (6); r = [condensed] (3) | none | | `breaching.json` |
| Thermobaric (1) | `cic/iri/cic` | c = [chemical]; i = [incendiary]; r = [repulsive] | none | | `thermobaric.json` |
| Nuclear (1) | `tit/iii/tit` | t = [thermobaric]; i = ore `ingotUranium` or ore `pelletUranium235` (each slot independent) | the Mekanism and skull versions below | `ingotUranium` or `pelletUranium235` exists | `nuclear.json` |
| Nuclear (1) | `tit/iii/tit` | t = [thermobaric]; i = Mekanism atomic alloy (`mekanism:atomicalloy`) | the uranium and skull versions | no `ingotUranium` and no `pelletUranium235`, and the alloy item exists | `mekanism/nuclear.json` |
| Nuclear (1) | `tit/iii/tit` | t = [thermobaric]; i = wither skeleton skull (`minecraft:skull` meta 1) | the uranium and alloy versions | no uranium names and the alloy item "absent" (ICBM-06-03) | `vanilla/nuclear.json` |
| EMP (1), type: explosive | `rer/sas/sss` | r = redstone; e = battery (`icbmclassic:battery`); s = copper wire (`icbmclassic:wire` meta 0, 5); a = [repulsive] | none | | `emp.json` |
| Exothermic (1) | `iii/igi/iii` | i = [incendiary]; g = glass | none | | `exothermic.json` |
| Endothermic (1) | `isi/sas/isi` | i = ice; s = snow block; a = [attractive] | none | | `endothermic.json` |
| Anti-gravitational (1), type: explosive | `eee/ere/eee` | e = eye of ender; r = [shrapnel] | none | | `anti_grav.json` |
| Ender (1) | `ses/eae/ses` | s = end stone; e = eye of ender; a = [attractive] | none | | `ender.json` |
| Antimatter (1), type: explosive | `nnn/nrn/nnn` | n = nether star; r = [nuclear] | the alloy version | the alloy item "absent" (ICBM-06-03) | `vanilla/antimatter.json` |
| Antimatter (1), type: explosive | `nnn/nrn/nnn` | n = Mekanism atomic alloy; r = [nuclear] | the nether-star version | the alloy item exists | `mekanism/antimatter.json` |
| Red matter (1) | `nnn/nrn/nnn` | n = nether star; r = [antimatter] | the alloy version | the alloy item "absent" (ICBM-06-03) | `vanilla/redmatter.json` |
| Red matter (1) | `nnn/nrn/nnn` | n = Mekanism atomic alloy; r = [antimatter] | the nether-star version | the alloy item exists | `mekanism/redmatter.json` |

Notes: the rows marked "type: explosive" are the ones whose JSON type is `icbmclassic:explosive`. The others (shrapnel, incendiary, repulsive, fragmentation, sonic, thermobaric, exothermic, endothermic, ender, the three nuclear rows, the two red matter rows) are plain `minecraft:crafting_shaped` recipes that write the output as `icbmclassic:explosives` with the numeric ID, so their counts are honored (all 1 here). The two ICBM-item ingredients in the EMP recipe (`icbmclassic:battery`, `icbmclassic:wire`) are named directly, so the recipe fails to load if either family is disabled (ICBM-06-06). There is no recipe for the explosives with IDs 21 (hypersonic), 24 (missile module placeholder), 25 (rot) or 26 (mutation).

## 02 and 03 device recipes

Each device recipe wraps one explosive block. These are all type `icbmclassic:explosive`, count 1 (so unaffected by ICBM-06-02).

### Grenades (tier 1 only, 10 recipes)

Grid `s/x`: s = ore `string` on top, x = [name] below. Output is `icbmclassic:grenade` with the explosive's meta. Files `grenades/<name>.json`. No alternates, unconditional. Documented in [03 thrown-and-handheld](../03-missiles/thrown-and-handheld.md).

| Explosive | Meta (ID) | Output |
|---|---|---|
| Condensed | 0 | Condensed grenade |
| Shrapnel | 1 | Shrapnel grenade |
| Incendiary | 2 | Incendiary grenade |
| Debilitation | 3 | Debilitation grenade |
| Chemical | 4 | Chemical grenade |
| Anvil | 5 | Anvil grenade |
| Repulsive | 6 | Repulsive grenade |
| Attractive | 7 | Attractive grenade |
| Colors | dynamic | Colors grenade |
| Smoke | dynamic | Smoke grenade |


### Bomb carts (23 recipes)

Grid `a/c`: a = [name] on top, c = minecart below. Output is `icbmclassic:bombcart` with the explosive's meta. Files `bombcarts/<name>.json`. No alternates, unconditional. Documented in [03 bomb-cart](../03-missiles/bomb-cart.md).

| Explosive | Meta (ID) | Output |
|---|---|---|
| Condensed | 0 | Condensed bomb cart |
| Shrapnel | 1 | Shrapnel bomb cart |
| Incendiary | 2 | Incendiary bomb cart |
| Debilitation | 3 | Debilitation bomb cart |
| Chemical | 4 | Chemical bomb cart |
| Anvil | 5 | Anvil bomb cart |
| Repulsive | 6 | Repulsive bomb cart |
| Attractive | 7 | Attractive bomb cart |
| Fragmentation | 8 | Fragmentation bomb cart |
| Contagious | 9 | Contagious bomb cart |
| Sonic | 10 | Sonic bomb cart |
| Breaching | 11 | Breaching bomb cart |
| Thermobaric | 13 | Thermobaric bomb cart |
| Nuclear | 15 | Nuclear bomb cart |
| EMP | 16 | EMP bomb cart |
| Exothermic | 17 | Exothermic bomb cart |
| Endothermic | 18 | Endothermic bomb cart |
| Anti-gravitational | 19 | Anti-gravitational bomb cart |
| Ender | 20 | Ender bomb cart |
| Antimatter | 22 | Antimatter bomb cart |
| Red matter | 23 | Red matter bomb cart |
| Colors | dynamic | Colors bomb cart |
| Smoke | dynamic | Smoke bomb cart |


### Missiles (23 recipes)

Grid `mx` (one row, two slots): m = the missile module (`icbmclassic:explosive_missile` meta 24, see below), x = [name]. Output is `icbmclassic:explosive_missile` with the explosive's meta. Files `missile/ex/<name>.json`. No alternates, unconditional. Documented in [03 missile-item](../03-missiles/missile-item.md).

| Explosive | Meta (ID) | Output |
|---|---|---|
| Condensed | 0 | Condensed missile |
| Shrapnel | 1 | Shrapnel missile |
| Incendiary | 2 | Incendiary missile |
| Debilitation | 3 | Debilitation missile |
| Chemical | 4 | Chemical missile |
| Anvil | 5 | Anvil missile |
| Repulsive | 6 | Repulsive missile |
| Attractive | 7 | Attractive missile |
| Fragmentation | 8 | Fragmentation missile |
| Contagious | 9 | Contagious missile |
| Sonic | 10 | Sonic missile |
| Breaching | 11 | Breaching missile |
| Thermobaric | 13 | Thermobaric missile |
| Nuclear | 15 | Nuclear missile |
| EMP | 16 | EMP missile |
| Exothermic | 17 | Exothermic missile |
| Endothermic | 18 | Endothermic missile |
| Anti-gravitational | 19 | Anti-gravitational missile |
| Ender | 20 | Ender missile |
| Antimatter | 22 | Antimatter missile |
| Red matter | 23 | Red matter missile |
| Colors | dynamic | Colors missile |
| Smoke | dynamic | Smoke missile |


Dynamic (dye) and smoke explosives have a runtime ID rather than a fixed one.

### Missile module, SAM and special missiles

| Output (count) | Grid | Keys | Alternates | Enabled when | File |
|---|---|---|---|---|---|
| Missile module (1) = explosive missile meta 24 | `_i_/ici/ifi` | i = ore `ingotSteel` (5); c = ore `circuitBasic`; f = flint and steel, **undamaged only** (ICBM-06-04), consumed | the iron version below | `circuitBasic` and `ingotSteel` both exist | `missile/missile.module.json` |
| Missile module (1) | `_i_/ici/ifi` | i = ore `ingotIron`; c = redstone block; f = flint and steel, any damage, consumed | the steel version above | neither `circuitBasic` nor `ingotSteel` exists | `missile/vanilla/missile.module.json` |
| Surface-to-air missile (1) | `c/e/m` | m = missile module; e = [condensed]; c = ore `circuitBasic` | the repeater version below | `circuitBasic` exists | `missile/special/missile.anti_ballistic.json` |
| Surface-to-air missile (1) | `c/e/m` | m = missile module; e = [condensed]; c = repeater | the circuit version above | `circuitBasic` does not exist | `missile/vanilla/missile.anti_ballistic.json` |
| Cluster missile (1) | `c/e/m` | m = missile module; e = dispenser; c = ore `circuitBasic` | none | `circuitBasic` exists | `missile/special/missile.cluster.json` |
| Held-item missile (1) | `e/c/m` | m = missile module; e = sticky piston; c = ore `circuitBasic` | none | `circuitBasic` exists | `missile/special/missile.held_item.json` |
| Empty bomblet "Droplet" (16) | `_i_/ici/ipi` | i = ore `ingotSteel` (5); c = ore `circuitBasic`; p = wooden button | none | `circuitBasic` and `ingotSteel` both exist | `bomblet/bomblet.json` |
| Condensed bomblet (1) | `mx` | m = empty bomblet; x = [condensed] | none | | `bomblet/condensed.json` |
| Parachute (4) | `_C_/CGC/SSS` | C = carpet, white only (ICBM-06-09); G = slime ball; S = string | none | | `parachute.json` |
| Balloon (4) | `SCS/CGC/SCS` | C = carpet, white only; G = blaze powder; S = string | none | | `balloon.json` |
| Rocket launcher, direct (1) | `sss/rcg/w_w` | s = ore `ingotSteel`; r = radar gun; c = ore `circuitBasic`; g = ore `wireCopper`; w = ore `plankWood` | none | | `tools/rpg.json` |
| Rocket launcher, ballistic (1), shapeless | direct launcher + launcher screen | `icbmclassic:rocketlauncher`; `icbmclassic:launcherscreen` | none | | `tools/rpgBallistic.json` |

No other bomblet kind has a crafting recipe (conventional only), and the SAM warhead is always the condensed explosive.

### Recipes registered in code (no JSON)

These are dynamic recipes with NBT-driven results, registered in `ICBMClassic.registerRecipes`. They are documented in the 03 sheets and only listed here.

| Recipe ID | Class | Does | Documented in |
|---|---|---|---|
| `icbmclassic:balloon_cargo`, `icbmclassic:parachute_cargo` | `RecipeCargoData` | Combines a balloon or parachute with an allowed item to make a loaded holder | [03 cargo](../03-missiles/cargo.md) |
| `icbmclassic:balloon_cargo_decraft`, `icbmclassic:parachute_cargo_decraft` | `RecipeCargoDataDecraft` | Takes the cargo back out | [03 cargo](../03-missiles/cargo.md) |
| `icbmclassic:cluster_missile` | `RecipeCluster` | Adds item stacks to a cluster missile | [03 cluster-missile](../03-missiles/cluster-missile.md) |
| `icbmclassic:held_item_missile` | `RecipeHeldItemMissile` | Loads the held item | [03 missile-item](../03-missiles/missile-item.md) |
| `icbmclassic:held_item_missile_decraft` | `RecipeHeldItemMissileDecraft` | Unloads it | [03 missile-item](../03-missiles/missile-item.md) |

## 04 launch control recipes

Block recipes. Behavior: [04-launch-control](../04-launch-control/index.md).

| Output (count) | Grid | Keys | Alternates | Enabled when | File (`launcher/`, `machine/`) |
|---|---|---|---|---|---|
| Launcher base "Launcher Pad" (1) | `ici/ppp` | i = ore `ingotSteel`; c = ore `circuitBasic`; p = ore `plateSteel` | none | | `launcher/base.json` |
| Launcher connector (4) | `iwi/whw/iwi` | i = ore `ingotSteel`; w = ore `wireCopper`; h = hopper | none | | `launcher/connector.json` |
| Launcher screen (1) | `cg/sw` | c = ore `circuitBasic`; g = glass; s = ore `ingotSteel`; w = ore `wireCopper` | none | | `launcher/screen.json` |
| Launcher frame "support" (1) | `ibi/ibi` | i = ore `ingotIron`; b = iron bars | none | | `launcher/support.json` |
| Cruise launcher (1) | `CLP/SBS` | C = ore `circuitAdvanced`; L = launcher base; P = ore `plateSteel`; S = ore `ingotSteel`; B = launcher screen | none | | `machine/cruiselauncher.json` |

## 05 detection and defense recipes

Behavior: [05-detection-defense](../05-detection-defense/index.md). The pilot sheets write two of these grids incorrectly; the rows below are checked against the JSON (see Coverage in the [index](index.md#coverage)).

| Output (count) | Grid | Keys | Alternates | Enabled when | File |
|---|---|---|---|---|---|
| Radar station (1) | `gcs/pcp` | g = ore `ingotGold`; c = ore `circuitAdvanced` (2); s = launcher screen; p = ore `plateSteel` (2) | none | | `machine/radar.json` |
| EMP tower (1), meta 0 | `pwp/ece/prp` | p = ore `plateSteel` (4); w = ore `wireGold`; e = battery (2); c = ore `circuitElite`; r = redstone | none | | `machine/emptower.json` |
| EMP tower extender (1), meta 1 | `prp/ewe/prp` | p = ore `plateSteel` (4); r = redstone (2); e = battery (2); w = ore `wireGold` | none | | `machine/emptower.booster.json` |
| Radar gun "Laser Range Finder" (1) | `gci/_bw` | g = glass; c = ore `circuitBasic`; i = ore `ingotSteel`; b = stone button; w = ore `plankWood` | none | | `tools/radargun.json` |
| Remote detonator (1) | `iri/ici/ibi` | i = ore `ingotSteel` (6); r = redstone torch; c = ore `circuitAdvanced`; b = wooden button | none | | `tools/remote.json` |
| Laser detonator "Laser Designator" (1) | `r__/_c_/__h` (diagonal) | r = radar gun; c = ore `circuitElite`; h = remote detonator | none | | `tools/laser.json` |
| Defuser "Defuse Kit" (1) | `w_w/rar/ici` | w = ore `plankWood` (2); r = redstone (2); a = shears, consumed; i = ore `ingotCopper` (2); c = ore `circuitBasic` | none | | `tools/defuser.json` |

## Audit

| Folder | Files | Rows above |
|---|---|---|
| top level (antidote, balloon, concrete x3, glass button, glass plate, parachute, poison powder, reinforced glass, spikes x3) | 13 | 06 own and 03 |
| `parts/` (battery, circuits x3, clump, plate, wires x2) plus `parts/mod/`, `parts/vanilla/` | 8 + 1 + 2 | 06 own |
| `explosives/` plus `mekanism/` and `vanilla/` | 21 + 3 + 3 | 02 |
| `grenades/` | 10 | device |
| `bombcarts/` | 23 | device |
| `missile/ex/` | 23 | device |
| `missile/` module, `special/`, `vanilla/` | 1 + 3 + 2 | missile |
| `bomblet/` | 2 | missile |
| `launcher/`, `machine/` | 4 + 4 | 04, 05 |
| `tools/` | 6 | 05, 03 |
| Total | 129 recipes, plus `_factories.json` | |

Smelting adds 3 recipes and the code registers 7 dynamic recipes (not JSON).
