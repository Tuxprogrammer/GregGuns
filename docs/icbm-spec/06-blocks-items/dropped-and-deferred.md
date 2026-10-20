# Dropped and Deferred Content (compact sheets)

Compact entity sheets (identity, numbers, behavior, bugs) for everything in `06-blocks-items` that GregGuns does not rebuild as-is. [D10](../../gregguns-icbm-spec/DECISIONS.md#d10-in-detail) drops the crafting items, battery, sulfur and saltpeter, concrete and reinforced glass, and defers spikes, the glass button and the glass pressure plate. The kept item, the [antidote](antidote.md), has its own full sheet. Source classes are under `content/blocks/`, `content/items/` and `config/` at `9c1d985a`; cite them relative to `src/main/java/icbm/classic/`. Recipes are only summarized here; the exact rows are in [recipes.md](recipes.md).

Shared facts for the blocks:
- All five blocks register in `BlockReg.registerBlocks` with no config gate. Their item forms register in `ItemReg.registerItems` (`ItemBlock`, or `ItemBlockSubTypes` for metadata blocks).
- Metadata on `concrete` and `spikes` is the block property `type` (0 to 2), a *1.12-only mechanism* for the blockstate. Out-of-range metadata maps to type 0.
- `ItemBlockSubTypes` builds the unlocalized name as `tile.icbmclassic:<block>.<meta>` when that `.name` key exists, else the bare block key.
- Textures, models and blockstates are catalog depth, owned by [07-visuals-audio](../README.md#areas).

## A. Concrete (dropped)

| Field | Value |
|---|---|
| Identity | `content/blocks/BlockConcrete`, registry `icbmclassic:concrete`, unlocalized `tile.icbmclassic:concrete`, ICBM tab, no oredict. Three types (metadata = enum ordinal) |
| English names | meta 0 "Concrete", meta 1 "Compact Concrete", meta 2 "Reinforced Concrete" (`tile.icbmclassic:concrete.0.name` .. `.2.name`) |
| Obtaining | Meta 0: 4 sand + 4 gravel + water bucket, gives 8. Meta 1: 5 concrete (meta 0) + 4 obsidian, gives 8. Meta 2: 5 compact concrete + 4 iron ingots (oredict), gives 8 (`recipes/concrete.json`, `.1.json`, `.2.json`) |
| Material, hardness | `Material.ROCK`, hardness **10** for all three types, no `setResistance` (the base resistance is 0) |
| Explosion resistance | Overridden per type: **28** (normal), **280** (compact), **2800** (reinforced). The values are returned as-is; vanilla blocks return `resistance * 3 / 5`, so these are not on the vanilla scale (see ICBM-06-08). The blast engine reads this value as ray-energy cost ([blast-engine.md](../02-explosives/blast-engine.md)) |
| Drops | Itself with its metadata (`damageDropped` is the metadata). Rock material, so a pickaxe is needed to get the drop |
| Behavior | Full opaque cube with no tile entity, no interaction, no redstone behavior. Placement takes the item's metadata as the type |
| Visuals | `blockstates/concrete.json` variants `type=normal|compact|reinforced`, one block model `concrete` with an `all` texture: `textures/blocks/concrete.png`, `concrete_compact.png`, `concrete_reinforced.png` (each 16 x 16, license CC BY-NC-SA 4.0 per `Credits.md`). Item models `concrete.json`, `concrete_compact.json`, `concrete_reinforced.json` |
| Audio | none (default stone step and break sounds) |
| Bugs | ICBM-06-08 |

## B. Reinforced Glass (dropped)

| Field | Value |
|---|---|
| Identity | `content/blocks/BlockReinforcedGlass`, registry `icbmclassic:reinforcedglass` (source spelling `reinforcedGlass`), unlocalized `tile.icbmclassic:reinforcedGlass`, English "Reinforced Glass", ICBM tab |
| Obtaining | Shaped: iron ingots (oredict `ingotIron`) in the four corners and center, glass in the four edges, gives 8 (`recipes/reinforcedGlass.json`) |
| Numbers | `Material.GLASS`, hardness 10, `setResistance(48)` (effective explosion resistance 28.8 on the vanilla scale), sound type is the default for the glass material |
| Behavior | Not an opaque cube and not a full cube, cutout render layer. Faces between two adjacent reinforced glass blocks are not drawn (the usual glass rule, client only, *1.12-only mechanism*). `canSilkHarvest` is true but has no effect because the block drops itself anyway (ICBM-06-11) |
| Visuals | `blockstates/reinforcedglass.json`, block model `glass_reinforced`, texture `textures/blocks/glass_reinforced.png` (16 x 16), item model `reinforcedglass.json` |
| Audio | none of its own |
| Bugs | ICBM-06-11 |

## C. Glass Button (deferred)

| Field | Value |
|---|---|
| Identity | `content/blocks/BlockGlassButton` extends vanilla `BlockButton`; registry `icbmclassic:glassbutton` (source spelling `glassButton`); English "Glass Button" (`tile.icbmclassic:glassButton.name`) |
| Creative tab | Redstone (vanilla `BlockButton` sets it; the class sets none), not the ICBM tab (ICBM-06-14) |
| Obtaining | Three glass in a column gives **2** buttons (`recipes/glassButton.json`) |
| Numbers | Constructed as a non-wooden button, so arrows do not press it. Hardness 0.5, glass sound type. Click sound is the stone button click at volume 0.3, pitch 0.6; release is the stone release click at 0.3, 0.5. Pulse length is the vanilla non-wooden length (20 ticks, inherited, not set here) |
| Behavior | A vanilla button in every respect: attaches to any of six faces, emits power while pressed, springs back after the pulse. Cutout layer, not opaque. It ticks randomly, as every vanilla button does |
| Visuals | `blockstates/glassbutton.json` (`facing` x `powered`), models `glass_button`, `glass_button_pressed`, `glass_button_inventory`, texture `textures/blocks/glass_button.png` (**32 x 32**, unlike the other 16 x 16 textures), item model `glassbutton.json` |
| Bugs | ICBM-06-14 |

## D. Glass Pressure Plate (deferred)

| Field | Value |
|---|---|
| Identity | `content/blocks/BlockGlassPressurePlate` extends vanilla `BlockPressurePlate`; registry `icbmclassic:glasspressureplate` (source spelling `glassPressurePlate`); English "Glass Pressure Plate" |
| Creative tab | Redstone (set explicitly) |
| Obtaining | Three glass in a row gives 1 (`recipes/glassPressurePlate.json`) |
| Numbers | `Material.GLASS`, sensitivity `EVERYTHING` (any entity standing on it counts: players, mobs, items, arrows), hardness 0.3, resistance 1 (effective 0.6), glass sound type, default state unpowered |
| Behavior | Vanilla pressure plate: output 15 while any entity overlaps, released after the entity leaves. Cutout layer, not opaque |
| Visuals | `blockstates/glasspressureplate.json` (uses the vanilla `pressure_plate_up`/`_down` models; the unpowered state uses `textures/blocks/glass_pressure_plate.png`, **32 x 32**; the powered and inventory variants fall back to the vanilla glass texture) |
| Bugs | ICBM-06-14 |

## E. Spikes (deferred)

| Field | Value |
|---|---|
| Identity | `content/blocks/BlockSpikes`, registry `icbmclassic:spikes`, unlocalized `tile.icbmclassic:spikes`, ICBM tab. Three types: 0 normal, 1 poison, 2 fire |
| English names | "Spikes", "Poison Spikes", "Flammable Spikes" (`tile.icbmclassic:spikes.0.name` .. `.2.name`) |
| Obtaining | Normal: 3 cactus over 3 iron ingots (oredict), gives **6**. Poison: poison powder over a normal spike, gives 1. Fire: sulfur dust (oredict `dustSulfur`) over a normal spike, gives 1 (`recipes/spikes.json`, `.1.json`, `.2.json`; groups `icbmclassic:spikes`, `..._poison`, `..._fire`) |
| Numbers | `Material.IRON`, hardness 1, no `setResistance`. Config `icbmclassic/blocks`, group `spikes`: `normal_spike_damage` 1, `poison_spike_damage` 1, `fire_spike_damage` 1 (all floats, minimum 1, so they can never be set below one point) |
| Shape | No collision box (entities walk through), not a normal, opaque or full cube, cutout layer, no face shape. The trigger is the entity overlapping the block's cell |
| Behavior | `onEntityCollidedWithBlock`, any `EntityLivingBase` (mobs and players; not items or arrows). Every tick it overlaps: **normal** deals the normal damage with damage source `icbmclassic:spikes`. **Poison** deals the poison damage and adds Poison I for 7 s (140 ticks, amplifier 0). **Fire** deals the fire damage and sets the entity on fire for 7 s. The game's per-entity invulnerability window (10 ticks) limits real hits to about two per second. No fall-damage bonus, no knockback |
| Death message | `death.attack.icbmclassic:spikes` = "%s died to spikes" |
| Drops | Itself with its metadata. Iron material, so a pickaxe is needed |
| Visuals | `blockstates/spikes.json` (variants `type=normal|fire|poison`, one `spikes` model with a `cross` texture), textures `textures/blocks/spikes.png`, `spikes_fire.png`, `spikes_poison.png` (all 16 x 16), item models `spikes.json`, `spikes_fire.json`, `spikes_poison.json`; the client picks the item model per metadata with a mesh definition (`client/SpikeMeshDefinition`, *1.12-only mechanism*) |
| Bugs | ICBM-06-18 |

## F. Battery (dropped)

| Field | Value |
|---|---|
| Identity | `content/items/ItemBattery` extends the deprecated `prefab/item/ItemICBMBase`, registry `icbmclassic:battery`, unlocalized `item.icbmclassic:battery`, English "Battery", tooltip `.info` "Used to store and transport power", ICBM tab, stack size 1, flagged as having subtypes but none defined |
| Gate | `icbmclassic/item` `enable_battery` (default true, restart required); when false the item does not register |
| Obtaining | Shaped `RCR/PPP/SIS`: R = redstone, C = copper ingot (oredict), P = paper (oredict, three), S = sulfur dust (oredict, two), I = iron ingot (oredict), gives 1 (`recipes/parts/battery.json`). No loot, no creative variant that is full |
| Numbers | `icbmclassic/battery`: `battery_tier_1_capacity` 100,000 FE; `battery_tier_1_input` 10,000; `battery_tier_1_output` 10,000 |
| Behavior | Carries one FE energy buffer through the capability system (*1.12-only mechanism*), starts empty, and fills from any FE source. **It cannot be drained** (ICBM-01-03), and **no ICBM machine charges from it or reads it**; the only use inside the mod is as a recipe ingredient (EMP tower, tower extender, EMP explosive). Full model: [energy-model.md](../01-core/energy-model.md#5-the-battery-item) |
| Tooltip | `L: <percent>%` and `E: <stored>/<max> FE`, shown only when the capability is present (a TODO in source adds shift info) |
| Visuals | `models/item/battery.json` (generated, one layer), `textures/items/battery.png` (16 x 16) |
| Bugs | ICBM-06-13 (plus ICBM-01-03) |

## G. Crafting items (dropped)

Five `ItemCrafting` item classes, one registry name each, with a subtype per metadata (`content/items/ItemCrafting`, extends `ItemICBMBase`). They register only under `icbmclassic/item` flags: `enable_crafting_items` (master, default true) plus one per family (`enable_ingots`, `enable_plates`, `enable_circuits`, `enable_wires`), all restart-required. The ingot flag covers the clump too. Each subtype registers an oredict name `<family><Subtype>` (capitalized) in the recipe event at highest priority (`ICBMClassic.registerOres`).

| Registry | Meta | Subtype | Oredict | English name | Texture (16 x 16) | Obtain |
|---|---|---|---|---|---|---|
| `icbmclassic:ingot` | 0 | steel | `ingotSteel` | Steel Ingot | `items/ingot_steel.png` | Smelt a clump (xp 0.1); chest loot (bug ICBM-06-01) |
| | 1 | copper | `ingotCopper` | Copper Ingot | `items/ingot_copper.png` | Chest loot only |
| `icbmclassic:clump` | 0 | steel | `clumpSteel` | Coal Encased Iron | `items/clump_steel.png` | Shaped `ic/cc`: 1 iron ingot + 3 coal or charcoal, gives 1; tooltip "Old method for steel creation using coal casing to enrich the iron with carbon" |
| `icbmclassic:plate` | 0 | steel | `plateSteel` | Steel Plate | `items/plate_steel.png` | 2 x 2 steel ingots, gives 4; chest loot (ICBM-06-01) |
| | 1 | iron | `plateIron` | Iron Plate | `items/plate_iron.png` | **Unobtainable** (no recipe, no loot); smelts to an iron ingot at 0 xp (ICBM-06-10) |
| `icbmclassic:wire` | 0 | copper | `wireCopper` | Copper Wire | `items/wire_copper.png` | 6 copper ingots + 2 planks + 1 stick, gives **18**; chest loot (ICBM-06-01) |
| | 1 | gold | `wireGold` | Gold Wire | `items/wire_gold.png` | 6 gold ingots + 2 planks + 1 stick, gives 18; chest loot |
| `icbmclassic:circuit` | 0 | basic | `circuitBasic` | Basic Circuit | `items/circuit_basic.png` | 2 redstone, 1 clay ball, 3 copper ingots, 3 planks, gives 4; chest loot (ICBM-06-01) |
| | 1 | advanced | `circuitAdvanced` | Advanced Circuit | `items/circuit_advanced.png` | Wire/ingot/torch ring around a basic circuit, gives 1; chest loot |
| | 2 | elite | `circuitElite` | Elite Circuit | `items/circuit_elite.png` | Gold version, with quartz, around a **basic** circuit, gives 1; chest loot |

Behavior and plumbing:
- Unlocalized name is `item.icbmclassic:<family>.<subtype>`; the family-level keys have no English text, only the per-subtype names above and the clump `.info`.
- `getStack(name, count)` finds a subtype by name (see ICBM-06-01).
- Models: `blockstates/ingot.json`, `clump.json`, `plate.json`, `wire.json`, `circuit.json` map each subtype to a generated item model (a *1.12-only mechanism* for item variants by name).
- Chest loot (config `enable_loot_drops`, default true) adds these to the `main` pool of `chests/simple_dungeon` and `chests/abandoned_mineshaft`; the exact weights are in [lifecycle-and-registration.md](../01-core/lifecycle-and-registration.md#loot-injection). Because of ICBM-06-01, four of the eight entries add nothing.
- Smelting registered in code (`ICBMClassic.registerRecipes`): clump to steel ingot (xp 0.1) under the ingot flag; iron plate to vanilla iron ingot (xp 0) under the plate flag.
- Which recipes consume which items is summarized in [recipes.md](recipes.md#material-demand-by-ore-name).

## H. Powders (dropped)

All four are plain `ItemBase` items with the ICBM tab and no config gate. Oredict `dustSulfur` and `dustSaltpeter` are registered for the first two (twice, see ICBM-01-19).

| Registry | English name | Tooltip | Oredict | Obtain | Uses | Texture (16 x 16) |
|---|---|---|---|---|---|---|
| `icbmclassic:poisonpowder` (source `poisonPowder`) | Poison Powder | "Used in the creation of explosives" | none | Spider eye + rotten flesh side by side, gives 3 (`recipes/powder.poison.json`) | chemical explosive; poison spikes | `items/poison_powder.png` |
| `icbmclassic:sulfurdust` (source `sulfurDust`) | Organic Sulfur | "Used in the creation of gunpowder" | `dustSulfur` | Dropped by creepers and blazes (loot entry 10 count, weight 2, config `enable_sulfur_drops`, default true) | battery, incendiary, debilitation, fire spikes, gunpowder | `items/sulfur_dust.png` |
| `icbmclassic:saltpeter` | Saltpeter | "Used in the creation of gunpowder" | `dustSaltpeter` | Smelt a saltpeter starter (xp 0.1; unconditional) | gunpowder | `items/saltpeter.png` |
| `icbmclassic:saltpeter_ball` | Saltpeter Starter | "Use in furnace to extract saltpeter" | none | Shaped `DPD/PWP/DPD`: dirt (oredict) on the corners, leaves or saplings (oredict, each slot independent) on the edges, water bucket in the center, gives 4 (`recipes/parts/vanilla/saltpeter.json`, no condition) | saltpeter | `items/saltpeter_ball.png` |

Gunpowder (`recipes/parts/mod/gunpowder.json`, `recipes/parts/vanilla/gunpowder.json`): both shapeless, 1 carbon source + sulfur dust + saltpeter dust gives **3** gunpowder. The "mod" variant takes any of `dustCarbon`, `dustCharcoal`, `dustCoal` and is enabled when at least one of those names exists; the "vanilla" variant takes a coal item (coal or charcoal) and is enabled only when none of the three exists. Details in [recipes.md](recipes.md#06-own-recipes).

Sulfur loot: the entry is added to the `main` pool of `entities/creeper` and `entities/blaze`; the stack count is a uniform integer from 0 to 9 (ICBM-01-27).

## Bugs

ICBM-06-01, 06-08, 06-10, 06-11, 06-13, 06-14, 06-18. Full list in [index.md](index.md#suspected-bugs).
