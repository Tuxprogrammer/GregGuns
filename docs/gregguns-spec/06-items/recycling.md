# GT recycling

- **Techguns baseline:** none. Techguns has no recycling; its items were only crafted.
- **Design records:** [0002](../../design/0002-machine-deconfliction.md) (GT owns materials and machines), [0011](../../design/0011-machine-policy.md); ICBM [D44](../../gregguns-icbm-spec/DECISIONS.md) (recycling data is a carried-over check), D84 and D155 (no fallout recycling), D205 (complex walls do not recycle)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205)
- **Roadmap:** GregGuns M3 (armor), ICBM roadmap "Carried over" (GT recycling data); this sheet closes it for every item

## What changes from Techguns

Every GregGuns item, block and machine that is built from GT materials gives those materials back in GT's recyclers: the Macerator returns dusts, the Arc Furnace returns ingots, and the Fluid Extractor and furnaces do what GT does for its own items. GregGuns adds no recycling recipes of its own. It gives each stack GT `ItemData` (its material content), and GT generates the recipes from that data, as it does for its own machines.

## Integration points

### GT5-Unofficial: material data on a stack makes GT write the recycling recipes

- **Source:**
  - [`GTOreDictUnificator.addItemData`, `setItemData`, `processRecycling`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L353-L445): data never overwrites an explicit composition; amounts are divided by the stack size, rounded down; under one ingot of recyclable material the stack goes on IC2's recycler blacklist; recycling recipes are registered at once after GT's load starts.
  - [`GTRecipeRegistrator.registerMaterialRecycling`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeRegistrator.java#L184-L210): the Macerator (main material plus three byproducts, as dust of their macerate-into form), smelting for one ingot or less, the Fluid Extractor, and the Arc Furnace (metals to their arc-smelt form, explosives and flammables to ash, others lost). The Arc Furnace recycling recipe takes programmed circuit 1 and no gas ([`UniversalArcFurnace`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L344-L380)).
  - [`GTModHandler.addCraftingRecipe`, reversible bit](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L1114-L1160): a crafted GT machine's data is the merged data of its ingredients. Circuits have a negative prefix amount and add nothing; a few ore names (planks, chests, pistons, furnaces and so on) get fixed contents.
  - [Hull recycling data](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L2484-L2600): taken from the cheaper Assembler recipe, because the crafting recipe "would create an exploit loop".
- **GregGuns does:** hand-written data for armor, gun parts, ammo, magazines, components and the ICBM bodies, warheads, parts, tools and machines (each next to its recipe), and GT's reversible crafting bit on the machines crafted in a grid. `RecyclingDerivation` then gives every remaining stack the data GT's rules give it (below).
- **Pack-side change:** none.

## The rule

`RecyclingDerivation` runs on GT's after-postload hook, when every GregGuns recipe exists. For each GregGuns stack with no data (`RecyclingCatalog`: every registry item at its variants, and every GregGuns machine id), it reads every recipe that makes the stack: crafting-table recipes, every GT recipe map, and the Assembly Line list. `RecyclingSolver` costs each recipe:

1. **Sum of the consumed inputs' data**, as GT's reversible crafting recipes merge it. An input with no data adds nothing: circuits, programmed circuits, GT tools, inputs a machine keeps (stack size 0), and filled containers that return their container. The GT ore-name special cases apply.
2. **Per item, rounded down**, as `setItemData` divides.
3. **Filter (GregGuns' own):** radioactive materials and explosives never come back (GGS-06-46: no uranium from recycling; the hand-written ammo data already lets powder burn away), and a material GT can return in no form is dropped so it does not take a Macerator slot.
4. **The cheapest recipe wins** (least material in total), so recycling never pays more than a craft costs.

A machine recipe that makes anything besides the stack, or makes it by chance, is not used: its inputs would be split between products. Items built from other items being derived wait for them; a conversion cycle settles its cheapest member first.

Data is attached with `addItemData` only when no Macerator or Arc Furnace recipe already takes the stack, so nothing clashes with an existing recycling recipe.

Worn armor and other damaged stacks do not match damage-0 data, as for GT's own items. Guns are always damage 0 in play (their state is NBT), so they recycle loaded or empty.

## Exclusions

`RecyclingExclusions` lists the stacks with no data on purpose. A bare name covers every variant; `name@damage` covers one.

| Stacks | Reason |
|---|---|
| `coin` | Currency for the Vending Machine; no recipe, and GT recycles no money |
| `npc_egg`, `probe_tool_gun` | Creative spawn egg; dev-only scenario tool. No recipe |
| `missile_filler`, `warhead_filler` | Technical blocks that fill a placed missile's or warhead's space |
| `casings@2` | Retired Reaction Casing, kept so placed Retorts leave no holes ([0012](../../design/0012-retire-reaction-chamber-and-retort.md)); no recipe |
| `irradiated_earth`, `irradiated_rubble`, `trinitite`, `trinitite_shard`, `fallout_goo`, `fallout_goo_item` | Fallout; data would turn free rubble into uranium (D84, D155) |
| `complex_wall` | Silo complex wall; a complex must not be a casing mine (D205) |
| `bug_nest` (both variants), `bug_nest_slime`, `bio_blob`, `camp_flag` | World blocks placed by structures; no recipe |
| `part_stock_wooden` | Logs: GT gives them no material data, so GT would recover nothing |
| `antidote` | Consumable; its sheet gives it no data ([antidote.md](../../gregguns-icbm-spec/06-blocks-items/antidote.md)) |
| `launcher_warhead_*` | Consumable launcher charges; their sheet gives them no data ([launcher-warheads.md](../../gregguns-icbm-spec/02-explosives/launcher-warheads.md)) |
| `missile_chip_t*`, `missile_schematic_t*` | Guidance chips from the Circuit Assembler, engraved or not: circuits, and GT recycles no circuits |

The warhead blocks (`warhead_t*`) and inert warheads (`inert_warhead_t*`) carry data written by the warhead code, which owns it; the derivation leaves them alone.

## Coverage (as of this sheet)

With every module on: 279 stacks. 154 carry hand-written or reversible-crafting data, 79 get derived data, 36 are excluded, 10 are warhead stacks owned by the warhead code. Samples, with GT's material names (amounts in ingots, one plate is 1):

| Stack | Data | From |
|---|---|---|
| AK-47 (`ak47`) | Bronze 4.25, Aluminium 3, Iron 3, Steel 2.75, Wood 1 | Assembler |
| Gun Mount Hatch MV (`mte:20121`) | Aluminium 23, Cupronickel 12, AnyCopper 6.5, Emerald 2, SteelMagnetic 1.5, Electrum 0.5 | Assembler |
| Steel Bastion Plating (`casings@8`) | Steel 1.25 | Assembler (one Steel Turret Armor and two concrete make four) |
| Steel Turret Armor (`turret_armor_steel`) | Steel 5 | Assembler |
| Minigun | Steel 16.5, BlackSteel 8, Titanium 4.75, StainlessSteel 3.25, Aluminium 2, NeodymiumMagnetic 0.5 | Assembler |
| Nuke Launcher (`nuke_launcher`) | NaquadahAlloy 5, Steel 5, Titanium 4.75, Lead 4, EpoxidFiberReinforced 1 | Assembly Line |
| Rocket (`ammo_rocket`) | StainlessSteel 1 | Ammo Press (the powder burns) |

## Requirements

- **GGS-06-49.** Every GregGuns stack carries GT item data with a material, or is on the exclusion list with a reason, or is a warhead stack whose data the warhead code owns.
- **GGS-06-50.** Derived data is the cheapest recipe's consumed inputs' data, summed, per item and rounded down, as GT derives its own machines' data.
- **GGS-06-51.** The catalog covers every registry item at each variant, and every GregGuns machine id. An item with subtypes is listed variant by variant or excluded whole.
- **GGS-06-52.** Hand-written data is never overwritten by the derivation.
- **GGS-06-53.** No radioactive material and no explosive comes back from recycling a derived stack.
- **GGS-06-54.** No derived data is attached to a stack that an existing Macerator or Arc Furnace recipe already takes.
- **GGS-06-55.** No exclusion names a stack or family that does not exist.

## Tests

| Requirement | Test |
|---|---|
| GGS-06-49, GGS-06-51, GGS-06-55 | Scenario `recycling.coverage` (fails on any stack with no data and no exclusion, on an excluded stack that has data, on a stale exclusion, and on an unlisted item with subtypes); unit test `RecyclingExclusionsTest` |
| GGS-06-50, GGS-06-53 | Unit test `RecyclingSolverTest` (sum, merge, rounding, cheapest recipe, filter, chains and cycles); scenario `recycling.spot` checks two derived compositions worked out by hand |
| GGS-06-52 | `recycling.coverage` reports the hand-written stacks as explicit; `armor.recycling` still checks the armor figures |
| GGS-06-54 | `RecyclingDerivation` reports a clash, and `recycling.coverage` fails on it |
| Recipes exist | Scenario `recycling.spot`: Macerator and Arc Furnace recipes for two guns, a part, an armor piece, two machines, an ICBM item, a casing and a turret plate, with the outputs GT derives from the data |

## Open questions

None.
