# Armor and equipment recipes

Every armor piece and every piece of special equipment that GregGuns keeps, as a GT recipe.

- **Techguns baseline:** [recipes.md](../../techguns-spec/05-armor/recipes.md) ([§ 1 Armor](../../techguns-spec/05-armor/recipes.md#1-armor), [§ 2 Special-slot equipment](../../techguns-spec/05-armor/recipes.md#2-special-slot-equipment-registered-in-tgitems)), [armor-catalog.md](../../techguns-spec/05-armor/armor-catalog.md), [special-equipment.md](../../techguns-spec/05-armor/special-equipment.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md) (authoritative for tiers), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (Fabricator replaced by Assembler), [0003 energy model](../../design/0003-energy-model.md) and [0008 exact-tier charging](../../design/0008-exact-tier-charging.md) (electric items), [0009](../../design/0009-gtnh-integration-defaults.md) (Jetpack dropped, so it has no recipe)
- **Sibling sheets:** [hazard-protection.md](hazard-protection.md), [oxygen-gear.md](oxygen-gear.md), [baubles-slots.md](baubles-slots.md), [powered-flight.md](powered-flight.md)
- **Format:** [CONVENTIONS § Recipe sheets](../CONVENTIONS.md#recipe-sheets)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205)
- **Roadmap:** M3 (armor and equipment)

This sheet absorbs the old `armor-recipes.md`, which is now a pointer.

## What changes from Techguns

Techguns builds every piece in the crafting table from Techguns plates, cloth and circuits. GregGuns makes the final step a GT recipe at the item's tier voltage, and each recipe has an input that cannot be had before that tier.

| Techguns | GregGuns | Why |
|---|---|---|
| Crafting-table grids for all 45 armor pieces | Steam/ULV sets stay in the crafting table; every other piece is an Assembler recipe | [CONVENTIONS](../CONVENTIONS.md#recipe-sheets): from LV up the final step is a GT machine |
| `ingotIron`, `ingotObsidianSteel`, `plateCarbon`, `circuitElite`, `itemRubber`, `sheetPlastic` | `plate.<tier material>`, `plate.EpoxidFiberReinforced`, `circuit.<tier>`, `plate.Rubber`, `plate.Polyethylene` | [0002](../../design/0002-machine-deconfliction.md) drops Techguns materials; [components-and-materials.md](../06-items/components-and-materials.md) has the mapping |
| Obsidian Steel (T2) and carbon (T3) have no GT equivalent | The tier material of the consuming item replaces Obsidian Steel (the T2 sets are MV, so Aluminum). Carbon is `plate.EpoxidFiberReinforced` plus the tier material (Titanium for the EV sets); IC2 carbon plate is not used | [0010](../../design/0010-material-remap-and-tier-additions.md) decisions 1 and 2. See [Open questions](#open-questions) |
| Energy Cell in the Nightvision Goggles recipe, charged and empty variants | One recipe. The goggles are an electric item and come out empty | [0003](../../design/0003-energy-model.md) |
| Nuclear Power Cell in the Anti Gravity Device | `Field_Generator_IV` | The cell is a consumable fuel in Techguns; the device is now an IV electric item |
| Tactical Mask folded NVG, Gas Mask and Oxygen Mask in one grid | Same idea, in the Assembler | Keeps the item |
| Jetpack recipes (two) | None | [0009](../../design/0009-gtnh-integration-defaults.md) drops the Jetpack |

## Integration points

### GregTech: the Assembler, programmed circuits and the voltage constants

- **Pack config:** none.
- **Source:**
  - `RecipeMaps.assemblerRecipes` takes up to 9 item inputs, 1 output and 1 fluid input: [RecipeMaps.java#L1266-L1272](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266-L1272).
  - Recipe voltage uses `TierEU.RECIPE_*` (30 EU/t at LV, 120 at MV, 480 at HV, 1 920 at EV, 7 680 at IV), not the raw tier voltages: [TierEU.java#L7-L34](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L7-L34).
  - The builder shape (`itemInputs`, `.circuit(n)`, `itemOutputs`, `duration`, `eut`, `addTo`): the casing recipes at [AssemblerRecipes.java#L2671-L2700](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2671-L2700) (6 plates plus a frame, programmed circuit 1).
  - A multi-part assembly with a hull, a circuit and a motor: the Muffler Hatch at [AssemblerRecipes.java#L1303-L1313](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L1303-L1313).
- **GregGuns does:** registers the recipes in this sheet through `GTValues.RA.stdBuilder()` into `assemblerRecipes`, fetches every GT part through `GTOreDictUnificator.get` or `ItemList` ([GGS-06-11](../06-items/components-and-materials.md#requirements)), and takes the voltage from `TierEU.RECIPE_<tier>`.
- **Pack-side change:** none.

### GregTech: hand-made armor at the Steam/ULV tier

- **Source:** GT's own Bronze armor is a hand recipe with a hammer in the grid, from the item's own plates: [CraftingRecipeLoader.java#L350-L368](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/CraftingRecipeLoader.java#L350-L368) (`"RhR", "RRR", "RRR"` with `plate.Bronze`).
- **GregGuns does:** the two steam-age sets (T1 Scout, T1 Miner) use shaped crafting with `plate.Bronze` or `plate.WroughtIron`. This answers the old open question "is a crafting-table recipe acceptable for the Steam/ULV tier". It is, because GT does the same for its own steam-age armor.
- **Pack-side change:** none.

### GregTech: powered armor precedent

- **Source:** the pack-adjusted GraviSuite chestplate, lappack and jetpack recipes are GT recipes: `plateAlloy.Carbon`, `plate.Titanium` or `plate.TungstenSteel`, a tier circuit and platinum wire around an IC2 armor item ([CraftingRecipeLoader.java#L1895-L1925](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/CraftingRecipeLoader.java#L1895-L1925)). GregGuns follows the shape (a plate, a tier plate and a tier circuit) but does not use IC2's carbon plate: the carbon plate here is `plate.EpoxidFiberReinforced` ([0010](../../design/0010-material-remap-and-tier-additions.md)).

## Conventions used in every table

- **Method.** "Hand" is shaped crafting. "Assembler" is `RecipeMaps.assemblerRecipes`.
- **EU/t.** `TierEU.RECIPE_<tier>` of the set. No recipe in this sheet uses a lower voltage.
- **Time.** 2 seconds per input item, at least 10 seconds. The programmed circuit is not counted.
- **Programmed circuit** (`.circuit(n)`): helmet 1, chestplate 2, leggings 3, boots 4 in every set, except the Hazardous Environment boots, which use 5 (the Combat boots on circuit 4 are a subset of their inputs). Single items say their own number. The number keeps the four pieces of one set apart when a player loads a bus.
- **Notation.** `HC` is Heavy Cloth, `TL` is Treated Leather, `PP` is Power Plating, `MP3` is Mechanical Parts T3, `CAT` is Compressed Air Tank. All are [assumed components](#assumed-components). `glass pane` is vanilla `Blocks.glass_pane`; `leather` is vanilla leather.
- **Fresh output.** Every output of this sheet has camo index 0 and no stored charge, as Techguns' `onCreated` does for crafted multi-camo and powered armor.
- **Gate.** The input that cannot be had below the tier, plus the machine voltage.

## Steam / ULV (hand-crafted)

Tier ([0001](../../design/0001-tier-map.md)): Steam / ULV. Gate material: Bronze, wrought iron. Method: Hand. The Techguns recipe is [§ 1](../../techguns-spec/05-armor/recipes.md#1-armor), "T1 Scout" and "T1 Miner". The Steam armor joined this tier in [0022](../../design/0022-steam-armor-steam-age.md). Notation in the grids: `c` Heavy Cloth, `B` `plate.Bronze`, `i` `plate.WroughtIron`, `y` yellow dye (`Dyes.dyeYellow`), `n` `nugget.WroughtIron`. Techguns' empty center cells of the Scout grids now hold the gate plate.

### Steam armor (4 recipes, gate: `plate.Bronze`)

Design record [0022](../../design/0022-steam-armor-steam-age.md) (owner request 2026-10-25): the Steam set is a Steam-age hand-crafted set. Bronze plates in the hard places, Heavy Cloth in the rest, no other metal. Row ids AR-34 to AR-37 (once the EV Assembler rows). Steam Armor Plating (RC-23) is retired.

| Output | Grid | Bronze plates |
|---|---|---|
| Steam Helmet | `BBB` / `c_c` | 3 |
| Steam Chestplate | `B_B` / `BcB` / `cBc` | 5 |
| Steam Legplates | `BcB` / `BcB` / `c_c` | 4 |
| Steam Boots | `c_c` / `B_B` | 2 |

### T1 Scout (4 recipes, gate: `plate.Bronze`)

| Output | Grid |
|---|---|
| Bandit Mask | `ccc` / `cBc` |
| Bandit Vest | `cBc` / `ccc` / `ccc` |
| Bandit Pants | `ccc` / `cBc` / `c_c` |
| Bandit Boots | `cBc` / `c_c` |

### T1 Miner (4 recipes, gate: `plate.WroughtIron`, built as `plate.CastIron`)

Implementation note: in GT 5.09.54.205 wrought iron is an alias of Cast Iron, so the ore names are `plateCastIron` and `nuggetCastIron` ([implementation-notes.md](implementation-notes.md), ARM-DEV-10).

| Output | Grid |
|---|---|
| Miner's Helmet | `iyi` / `c_c` |
| Miner's Jacket | `c_c` / `ccc` / `ici` |
| Miner's Pants | `ici` / `c_c` / `c_c` |
| Miner's Boots | `c_c` / `n_n` |

Notes: the Miner grids are the Techguns grids with `ingotIron` as `plate.WroughtIron` and the boots' `nuggetIron` as `nugget.WroughtIron`. Mirrors the hand-made Bronze armor at [CraftingRecipeLoader.java#L350-L368](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/CraftingRecipeLoader.java#L350-L368).

## LV: T1 Combat (4 recipes)

Tier: LV. Method: Assembler, `TierEU.RECIPE_LV`. Gate: `plate.Steel` (Steel is the LV gate material) and the LV voltage. Techguns: "T1 Combat" (`i` was `ingotIron`).

| Output | Inputs | Circuit | Time |
|---|---|---|---|
| Soldier's Helmet | `plate.Steel` ×3, HC ×2 | 1 | 10 s |
| Soldier's Vest | `plate.Steel` ×3, HC ×5 | 2 | 16 s |
| Soldier's Pants | `plate.Steel` ×3, HC ×4 | 3 | 14 s |
| Soldier's Boots | `plate.Steel` ×2, HC ×2 | 4 | 10 s |

## MV: T2 sets, Beret and the MV equipment

Tier: MV. Method: Assembler, `TierEU.RECIPE_MV`. Gate material: `plate.Aluminium`.

### T2 Combat (4 recipes)

Techguns: "T2 Combat" (`i` was `ingotObsidianSteel`). Gate: `plate.Aluminium`.

| Output | Inputs | Circuit | Time |
|---|---|---|---|
| Combat Helmet | `plate.Aluminium` ×3, HC ×2 | 1 | 10 s |
| Combat Vest | `plate.Aluminium` ×6, HC ×2 | 2 | 16 s |
| Combat Pants | `plate.Aluminium` ×4, HC ×3 | 3 | 14 s |
| Combat Boots | `plate.Aluminium` ×2, HC ×2 | 4 | 10 s |

### T2 Commando (4 recipes)

Techguns: "T2 Commando" (`h` `itemRubber`, `i` `ingotObsidianSteel`, `g` `paneGlass`). Gate: `plate.Aluminium`. Commando has no repair materials in Techguns and none here.

| Output | Inputs | Circuit | Time |
|---|---|---|---|
| Commando Headgear | `plate.Aluminium` ×2, `plate.Rubber` ×3, glass pane ×1 | 1 | 12 s |
| Commando Vest | `plate.Aluminium` ×4, `plate.Rubber` ×4 | 2 | 16 s |
| Commando Pants | `plate.Aluminium` ×3, `plate.Rubber` ×4 | 3 | 14 s |
| Commando Boots | `plate.Aluminium` ×2, `plate.Rubber` ×2 | 4 | 10 s |

### T2 Riot / Ranger Veteran (4 recipes)

Techguns: "T2 Riot" (upgrade of the matching Combat piece, `p` `plateLead`, `l` Treated Leather). The Combat piece is an input at any damage and any camo, and the output is fresh. Gate: the Combat piece, which carries Aluminum and was itself made at MV.

| Output | Inputs | Circuit | Time |
|---|---|---|---|
| Ranger Veteran Helmet | Combat Helmet ×1, `plate.Lead` ×2, TL ×2 | 1 | 10 s |
| Ranger Veteran Jacket | Combat Vest ×1, `plate.Lead` ×3, TL ×4 | 2 | 16 s |
| Ranger Veteran Pants | Combat Pants ×1, `plate.Lead` ×2, TL ×4 | 3 | 14 s |
| Ranger Veteran Boots | Combat Boots ×1, `plate.Lead` ×2, TL ×2 | 4 | 10 s |

### Beret (1 recipe)

Techguns: "Beret" (`cc_` / `l_l`). Not in [0001](../../design/0001-tier-map.md); MV per [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5. Gate: `ring.Aluminium` as the badge.

| Output | Inputs | Circuit | Time |
|---|---|---|---|
| Beret | HC ×2, leather ×2, `ring.Aluminium` ×1 | 1 | 10 s |

### Gas Mask, refilter and Night Vision Goggles (3 recipes)

| Output | Inputs | Circuit | Time | Notes |
|---|---|---|---|---|
| Gas Mask | `plate.Rubber` ×5, glass pane ×2, `plate.Aluminium` ×1, Gas Mask Filter ×1 | 1 | 18 s | Helmet-slot item ([hazard-protection.md](hazard-protection.md)). Techguns `grg` / `rrr` / `rfr` with one rubber plate swapped for the gate plate |
| Gas Mask (refilter) | Gas Mask (any damage) + Gas Mask Filter, shapeless, **Hand** | n/a | n/a | Same as Techguns: the output is the mask at damage 0. It only refills a mask that was already made at MV |
| Night Vision Goggles | `plate.Rubber` ×2, HC ×1, `circuit.MV` ×2, `ItemList.Sensor_MV` ×1, glass ×2 (`Blocks.glass`), `plate.Aluminium` ×1 | 2 | 18 s | Baubles `head` item ([baubles-slots.md](baubles-slots.md)). IC2 electric item, tier MV. `Sensor_MV` replaces the Techguns Energy Cell as the working part; the item comes out empty |

## HV: Oxygen gear, Glider, Jump Pack, Tactical Mask

Tier: HV. Method: Assembler, `TierEU.RECIPE_HV`. Gate material: `plate.StainlessSteel`.

| Output | Inputs | Circuit | Time | Notes |
|---|---|---|---|---|
| Oxygen Mask | `plate.Polyethylene` ×2, `plate.Rubber` ×2, `plate.StainlessSteel` ×1 | 1 | 10 s | Subclass of Galacticraft `ItemOxygenMask` ([oxygen-gear.md](oxygen-gear.md)). Techguns `_p_` / `rpr` |
| Oxygen Tanks | `plate.Polyethylene` ×4, CAT ×2, `plate.StainlessSteel` ×2 | 1 | 16 s | Subclass of `ItemOxygenTank`. Techguns `p_p` / `aia` / `p_p`. Comes out empty and is refilled with compressed air |
| Glider Backpack | HC ×7, `plate.StainlessSteel` ×2 | 1 | 18 s | Part. Techguns "Glider Backback" (sic) |
| Glider Wing | HC ×6, `stick.StainlessSteel` ×3 | 2 | 18 s | Part. Techguns "Glider Wing" |
| Glider | Glider Backpack ×1, Glider Wing ×2 | 3 | 10 s | Techguns made this shapeless in the table; GregGuns does it in the Assembler. `wings` bauble, no energy |
| Jump Pack | `plate.StainlessSteel` ×2, `screw.StainlessSteel` ×2, Glider Backpack ×1, CAT ×2, `ItemList.Electric_Piston_HV` ×1, `circuit.HV` ×1 | 1 | 18 s | `wings` bauble. IC2 electric item, tier HV. Techguns `n_n` / `pbp` / `c_c`; the piston and circuit are the electric drive. See [Open questions](#open-questions) for the air tanks |
| Tactical Mask | `plate.Polyethylene` ×4, `circuit.HV` ×2, Night Vision Goggles ×1, Gas Mask ×1, Oxygen Mask ×1 | 1 | 18 s | Helmet-slot item. Techguns `pnp` / `cgc` / `pop`. HV per [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5, and because it consumes the HV Oxygen Mask |

Gate for the Glider parts: `plate.StainlessSteel` (Backpack) and `stick.StainlessSteel` (Wing). The Glider itself is gated by its parts.

## EV: T3 Combat and HEV

Tier: EV. Method: Assembler, `TierEU.RECIPE_EV`. Gate material: `plate.Titanium`.

### T3 Combat / Advanced Combat (4 recipes)

Techguns: "T3 Combat" (`p` `plateCarbon`, `f` `fiberCarbon`). Here `p` is `plate.EpoxidFiberReinforced` and `f` is `plate.Titanium`, the EV tier material. Gate: `plate.Titanium`.

| Output | Inputs | Circuit | Time |
|---|---|---|---|
| Advanced Combat Helmet | `plate.EpoxidFiberReinforced` ×3, `plate.Titanium` ×2 | 1 | 10 s |
| Advanced Combat Chestplate | `plate.EpoxidFiberReinforced` ×6, `plate.Titanium` ×2 | 2 | 16 s |
| Advanced Combat Leggings | `plate.EpoxidFiberReinforced` ×4, `plate.Titanium` ×3 | 3 | 14 s |
| Advanced Combat Boots | `plate.EpoxidFiberReinforced` ×2, `plate.Titanium` ×2 | 4 | 10 s |

### T3 Hazardous Environment / HEV (4 recipes)

Techguns: "T3 Hazardous Environment" (`p` `plateCarbon`, `l` `plateTitanium`, `c` `circuitElite`, `g` `paneGlass`). Not in [0001](../../design/0001-tier-map.md); EV per [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5 ([hazard-protection.md](hazard-protection.md) agrees). Electric chestplate. Gate: `plate.Titanium` and `circuit.EV`.

| Output | Inputs | Circuit | Time |
|---|---|---|---|
| Hazardous Environment Helmet | `plate.EpoxidFiberReinforced` ×2, `plate.Titanium` ×2, `circuit.EV` ×1, glass pane ×1 | 1 | 12 s |
| Hazardous Environment Chestplate | `plate.EpoxidFiberReinforced` ×3, `plate.Titanium` ×4, `circuit.EV` ×1 | 2 | 16 s |
| Hazardous Environment Leggings | `plate.EpoxidFiberReinforced` ×3, `plate.Titanium` ×2, `circuit.EV` ×2 | 3 | 14 s |
| Hazardous Environment Boots | `plate.EpoxidFiberReinforced` ×2, `plate.Titanium` ×2, `circuit.EV` ×1 (so they differ from the Combat boots) | 5 | 10 s |

## IV: Power armor, Exo suit and Anti Gravity Device

Tier: IV. Method: Assembler, `TierEU.RECIPE_IV`. Gate material: `plate.TungstenSteel`. All outputs are IC2 electric items at tier IV and come out empty.

### T3 Power Armor (4 recipes)

Techguns: "T3 Power Armor" (`p` Power Plating, `g` `paneGlass`, `c` `circuitElite`, `e` Electric Engine). Here `c` is `circuit.IV` and `e` is `ItemList.Electric_Motor_IV`. A tungstensteel plate in each piece makes the gate explicit.

| Output | Inputs | Circuit | Time |
|---|---|---|---|
| Power Armor Helmet | PP ×3, `circuit.IV` ×2, glass pane ×1, `plate.TungstenSteel` ×1 | 1 | 14 s |
| Power Armor Chestplate | PP ×5, `Electric_Motor_IV` ×2, `circuit.IV` ×1, `plate.TungstenSteel` ×2 | 2 | 20 s |
| Power Armor Leggings | PP ×4, `circuit.IV` ×1, `Electric_Motor_IV` ×2, `plate.TungstenSteel` ×2 | 3 | 18 s |
| Power Armor Boots | `Electric_Motor_IV` ×2, PP ×2, `plate.TungstenSteel` ×1 | 4 | 10 s |

### T3 Exo Suit (4 recipes)

Techguns: "T3 Exo Suit", a shapeless upgrade of Advanced Combat. Here the Advanced Combat piece is an input at any damage and camo, and the output is fresh. The piece is made at EV and the upgrade runs at IV, so the suit needs both tiers.

| Output | Inputs | Circuit | Time |
|---|---|---|---|
| Exo Suit Helmet | Advanced Combat Helmet ×1, MP3 ×1, `circuit.IV` ×1, `Electric_Motor_IV` ×1, `plate.TungstenSteel` ×1 | 1 | 10 s |
| Exo Suit Chestplate | Advanced Combat Chestplate ×1, MP3 ×1, `circuit.IV` ×1, `Electric_Motor_IV` ×1, `plate.TungstenSteel` ×2 | 2 | 12 s |
| Exo Suit Leggings | Advanced Combat Leggings ×1, MP3 ×1, `circuit.IV` ×1, `Electric_Motor_IV` ×1, `plate.TungstenSteel` ×2 | 3 | 12 s |
| Exo Suit Boots | Advanced Combat Boots ×1, MP3 ×1, `circuit.IV` ×1, `Electric_Motor_IV` ×1, `plate.TungstenSteel` ×1 | 4 | 10 s |

### Anti Gravity Device (1 recipe)

Techguns: `tet` / `cac` / `mbm`. Gate: `plate.TungstenSteel`, `Field_Generator_IV`, and the Anti Gravity Core, which only the GT Assembler makes at IV ([RC-65](../06-items/recipes.md#part-anti-gravity-core)).

| Output | Inputs | Circuit | Time | Notes |
|---|---|---|---|---|
| Anti Gravity Device | `plate.TungstenSteel` ×2, `ItemList.Field_Generator_IV` ×1, `circuit.IV` ×2, Anti Gravity Core ×1, MP3 ×2, Glider Backpack ×1 | 1 | 18 s | `wings` bauble; IV electric item. The Nuclear Power Cell (`e`) becomes the field generator |

## Not carried over

| Techguns recipe | Reason |
|---|---|
| Jetpack and Jetpack (alternative) | Dropped by [0009](../../design/0009-gtnh-integration-defaults.md) |
| Nightvision Goggles (empty, damage 24 000) | Merged into the single electric item |
| Ammo Pouch (6 slots) | Belongs to the ammo pouch item, outside this sheet. [baubles-slots.md](baubles-slots.md) owns the item; its recipe is assumed to come from area 06 |

## Requirements

Numbering continues from [powered-flight.md](powered-flight.md). GGS-05-29 to GGS-05-31 are moved here unchanged from the old `armor-recipes.md`.

- **GGS-05-29.** Every armor and equipment recipe is in `assemblerRecipes` (or, for the two steam-age sets, a shaped crafting recipe) with voltage `TierEU.RECIPE_<tier>` for its [0001](../../design/0001-tier-map.md) tier.
- **GGS-05-30.** Every recipe has at least one input from that tier's gate material.
- **GGS-05-31.** No recipe uses a Techguns plate, ingot or circuit.
- **GGS-05-32.** Every recipe in this sheet resolves at boot. A `null` input from the unificator or `ItemList` skips that recipe and logs one warning ([GGS-06-11](../06-items/components-and-materials.md#requirements)).
- **GGS-05-33.** No recipe in this sheet runs below its tier. The dev self-check compares each recipe's `eut` with the tier table in [0001](../../design/0001-tier-map.md); Beret (MV), HEV (EV) and Tactical Mask (HV) use the tiers fixed by [0010](../../design/0010-material-remap-and-tier-additions.md).
- **GGS-05-34.** Each output of an armor or equipment recipe has camo index 0 and no stored charge.
- **GGS-05-35.** The Riot and Exo Suit recipes accept the base piece at any damage and any camo, and carry none of its NBT into the output.
- **GGS-05-36.** The T1 Scout and T1 Miner sets are the only armor and equipment items made in the crafting table, apart from the Gas Mask refilter. Each has a GT `plate.Bronze` or `plate.WroughtIron` input.
- **GGS-05-37.** The Gas Mask refilter recipe returns a mask at damage 0 and cannot produce a mask from a stack that is not a Gas Mask.
- **GGS-05-38.** No recipe produces a Jetpack.
- **GGS-05-39.** Night Vision Goggles, Jump Pack, HEV, Power Armor, Exo Suit and Anti Gravity Device are IC2 electric items whose `getTier` equals the tier of their recipe ([GGS-05-14](powered-flight.md#requirements), [0008](../../design/0008-exact-tier-charging.md)).

## Tests

| Req | Verified by |
|---|---|
| 29, 30, 33 | Dev self-check: unit test over the recipe registry, as [0001](../../design/0001-tier-map.md) requires |
| 31 | Unit test that scans recipe inputs for the removed Techguns item names |
| 32 | Unit test of the recipe loader with a stubbed unificator that returns `null` |
| 34, 35 | Unit test: run each recipe's output hook and assert camo 0, charge 0 and no input NBT |
| 36 | Unit test listing crafting-table recipes whose output is a GregGuns armor or equipment item |
| 37 | Unit test of the shapeless refilter matcher |
| 38 | Unit test that the registry has no jetpack output |
| 39 | Unit test over the electric items' `getTier` against the recipe table |
| All | Manual: NEI shows every recipe under the Assembler (or the crafting grid for the two Hand sets) |

## Assumed components

GregGuns components that these recipes consume. All except where noted are defined by area 06 ([components-and-materials.md](../06-items/components-and-materials.md), [fluids.md](../06-items/fluids.md)); this sheet does not define their recipes.

| Component | Used in | Source in area 06 |
|---|---|---|
| Heavy Cloth (`HC`, `part_heavy_cloth`) | Scout, Miner, T1 Combat, T2 Combat, Beret, NVG, Glider parts | components-and-materials.md: "Heavy Cloth ... Keep" |
| Treated Leather (`TL`, `part_treated_leather`) | Riot | same row |
| Mechanical Parts T3 (`MP3`, `part_mechanical_3`) | Exo Suit, Anti Gravity Device | "Mechanical Parts T1, T2, T3 ... Keep" |
| Power Plating (`PP`, `part_power_plating`) | Power Armor | "Cybernetic Parts, Power Plating, Anti Gravity Core ... Keep" |
| Gas Mask Filter (`part_gas_mask_filter`) | Gas Mask, refilter | "Gas Mask Filter ... Keep" |
| Compressed Air Tank (`CAT`, `tank_air`) | Oxygen Tanks, Jump Pack | [fluids.md](../06-items/fluids.md) (a 1 000 mB `IFluidContainerItem`) |
| Anti Gravity Core (`part_anti_gravity_core`) | Anti Gravity Device | Made on the GT Assembler at IV, [RC-65](../06-items/recipes.md#part-anti-gravity-core) |

Defined in this sheet and consumed by it: Glider Backpack, Glider Wing, Oxygen Mask. Area 06 should link here rather than repeat them ("Glider parts, Oxygen Mask ... Other areas" in components-and-materials.md).

## Open questions

- **Beret, HEV and Tactical Mask tiers (closed).** Adopted as MV, EV and HV by [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5. hazard-protection.md lists the Tactical Mask at MV, but it cannot be below its HV Oxygen Mask input, so that sheet's table still needs the same fix.
- **Material remap (closed).** [0010](../../design/0010-material-remap-and-tier-additions.md): the tier material of the consuming item replaces Obsidian Steel, and carbon is `plate.EpoxidFiberReinforced` plus the tier material.
- **Jump Pack power source.** powered-flight.md says the Jump Pack is an electric item; fluids.md says it uses compressed air. The recipe carries both a piston and two Compressed Air Tanks so either reading is buildable. A record "Jump Pack power" should pick one.
- **Night Vision Goggles as an electric item.** baubles-slots.md says nothing about the goggles' power. This sheet makes them an MV electric item, as GT's night-vision augment drains EU.
- **Crafting-table sets.** Resolved here for Steam/ULV (Hand, with a GT precedent). A note in [0001](../../design/0001-tier-map.md) would record it.
