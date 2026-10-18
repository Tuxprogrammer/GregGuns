# MineTweaker 3 integration (`java/techguns/plugins/minetweaker/`)

## 1. Loading

- [`Techguns`](../01-core/lifecycle-and-registration.md#lifecycle-overview) init: if mod `MineTweaker3` is loaded, it calls `new TechgunsMineTweakerIntegration().init()`. `ITechgunsMineTweakerIntegration` is the interface, with `@Optional.Interface` on `MineTweaker3`.
- `init()` registers seven Zen classes: `AmmoPressTweaker`, `CharginStationTweaker` (sic), `MetalPressTweaker`, [`ChemLabTweaker`](chem-lab.md#8-entity-sheet-summary-consistency-pass), [`FabricatorTweaker`](fabricator.md#8-entity-sheet-summary-consistency-pass), `ReactionChamberTweaker`, `OreClusterTweaker`.

**`TGMineTweakerHelper`:**
- `getItemNames(IIngredient)` produces `[name,name,…]` for log text.
- `toItemStackOreDict` converts an `IItemStack`, a `String` (ore name) or an `Object` into an [`ItemStackOreDict`](gui-handler.md#6-itemstackoredict-recipe-ingredient-wrapper). Anything else becomes a null stack.
- The item form keeps the stack's size and damage, and is matched with wildcard damage allowed.

**Undo.** "Undoable" actions support `/mt reload`. "One-way" actions (`OneWayAction`) can't be undone.

## 2. `mods.techguns.AmmoPress`

Edits the three input whitelists. No outputs are tweakable; the four build plans are fixed.

| Method | Parameters | Effect | Undo |
|---|---|---|---|
| `addMetal1` | `IIngredient input` | add every item of the ingredient to the metal1 list (slot 0) | yes |
| `addMetal2` | `IIngredient input` | add to the metal2 list (slot 1; still needs 2 per unit) | yes |
| `addPowder` | `IIngredient input` | add to the powder list (slot 2) | yes |
| `removeMetal1` | `IIngredient input` | remove every list entry that `itemMatches` (wildcard-tolerant) | yes |
| `removeMetal2` | `IIngredient input` | same, for metal2 | yes |
| `removePowder` | `IIngredient input` | same, for powder | yes |

Log text: "Add [names] to AmmoPressSlot: Metal1/Metal2/Powder", and "Remove … to AmmoPressSlot: …" (sic).

## 3. `mods.techguns.ChargingStation` (class `CharginStationTweaker`)

| Method | Parameters | Effect | Undo |
|---|---|---|---|
| `addRecipe` | `IItemStack input, IItemStack output, int RFAmount` | add a recipe (item input) | yes (removes that recipe object) |
| `addRecipe` | `String input` (ore name), `IItemStack output, int RFAmount` | add a recipe (ore input) | yes |
| `removeRecipe` | `IItemStack input` | remove every recipe whose input `matches` | yes (re-adds them) |
| `removeRecipe` | `String input` | same, by ore name | yes |

`RFAmount` is the recipe's `chargeAmount`: time = RFAmount/1000 ticks, and actual consumption is about 2% of it (see `charging-station.md`).

## 4. `mods.techguns.MetalPress`

| Method | Parameters | Effect | Undo |
|---|---|---|---|
| `addRecipe` | `(IItemStack \| String) input1, (IItemStack \| String) input2, IItemStack output, boolean allowSwap` (all 4 type combinations exist) | append a recipe | yes |
| `removeRecipe` | `IItemStack output` | remove every recipe whose output equals it (exact damage) | yes |
| `removeRecipe` | `(String \| IItemStack) input1, (String \| IItemStack) input2, IItemStack output` (all 4 combinations) | remove recipes with that output **and** `slot1.matches(input1)` **and** `slot2.matches(input2)` (exact order) | yes |

Each input still consumes 1 item per unit, whatever the stack size.

## 5. `mods.techguns.ChemLab`

| Method | Parameters | Undo |
|---|---|---|
| `addRecipe` | `(String \| IItemStack) input1, int amount1, (String \| IItemStack) input2, int amount2, ILiquidStack fluidIn, boolean allowSwap, IItemStack output, ILiquidStack fluidOut, int rfTick` (all 4 type combinations of input1/input2) | yes |
| `removeRecipe` | `IItemStack output, ILiquidStack fluidOut` | yes |

**Add rules:**
- `amount2 ≤ 0` turns input 2 into "must be empty".
- A `fluidIn` or `fluidOut` amount ≤ 0 means no fluid. A null `ILiquidStack` would throw ([MPRD-06](index.md#suspected-bugs)), so scripts must pass an amount of 0 instead.
- An output stack size ≤ 0 means no item output.
- The flask slot is always unused.
- `amount1` isn't validated.

**Remove** deletes every recipe whose output item (exact damage, null = null) **and** output fluid (both null, or `FluidStack.equals`) match. An internal constructor that also filters on inputs exists but isn't exposed.

## 6. `mods.techguns.Fabricator`

**`addRecipe`** (one-way). Parameters: `input, int amount, wire, int amount_wire, powder, int amount_powder, plate, int amount_plate, IItemStack output`. Each of input, wire, powder and plate may be an `IItemStack` or a `String` ore name; all 16 combinations are exposed.
- `amount_wire ≤ 0` or `amount_powder ≤ 0` makes that slot "not required".
- The output amount is `output.stackSize`.
- **Slot-list maintenance** (done in the action constructor, before `apply`):
  - If the wire, powder or plate ingredient isn't in any of the three slot whitelists, it's added to the list for its slot.
  - If it's already only in the correct list, nothing changes.
  - If it's in a different slot's list, the log gets "WARNING while adding Fabricator Recipe for …: One input already in use in another slot by other recipes! THIS WILL NOT WORK!" and the recipe is still added.
  - "Not required" null stacks get added to the lists too, which later crashes on comparison ([MPRD-25](index.md#suspected-bugs)).

**`removeRecipe`** (one-way). Parameter: `IItemStack output`. Removes every recipe whose output equals it (exact damage). Each removed recipe's wire, powder and plate ingredients are taken off the whitelists if no remaining recipe uses them in that slot.

A commented-out duplicate `addRecipe` is ignored.

## 7. `mods.techguns.ReactionChamber`

**`addRecipe`** (one-way). Parameters: `(IItemStack | String) input, ILiquidStack fluid, (IItemStack[] outputs | IItemStack output), IItemStack beamFocus, int ticks, int requiredCompletion, int preferredIntensity, int intensityMargin, int liquidLevel, int liquidConsumption, float instability, String risk, int RFTick`. All 4 combinations of input type × single or multiple outputs are exposed.

Validation happens at construction. On failure, the action logs "ERROR Adding ReactionChamber Recipe: \<msg>" and does nothing.

| Check | Message |
|---|---|
| focus item is a registered beam focus | "Item passed as Beam Focus is not valid!" |
| preferredIntensity in [0,10] | "Preferred Intensity must be in [0-10]!" |
| intensityMargin in [0,10] | "intensityMargin must be in [0-10]!" |
| liquidLevel in [0,10] | "liquidLeve must be in [0-10]!" (sic) |
| liquidConsumption in [0, liquidLevel·1000] | "liquidConsumption must not be negative and lower than 1000*liquidLevel!" |
| instability in [0,1] | "instability must be between 0.0 and 1.0" |
| risk is a `RiskType` constant name (`NONE`, `BREAK_ITEM`, `RAD_LOW`, `RAD_MEDIUM`, `RAD_HIGH`, `EXPLOSION_LOW`, `EXPLOSION_MEDIUM`, `EXPLOSION_HIGH`, `UNFORSEEN_CONSEQUENCES`) | "Invalid risktype: \<risk>" |

The values are stored as bytes or shorts (the range limits keep them safe). `ticks` and `requiredCompletion` are cast to byte without checks.

**`removeRecipe`** (one-way). Parameters: `(IItemStack | String) input, ILiquidStack fluid`. Removes every recipe whose input `matches` and whose fluid equals the given fluid. Removal shifts list indices, and saved reactions refer to recipes by index ([MPRD-04](index.md#suspected-bugs)).

## 8. `mods.techguns.OreCluster` (consumer documented in `09-machines-utility` / `11-worldgen`)

| Method | Parameters | Effect |
|---|---|---|
| `addOreCluster` | `int id, IItemStack ore, int miningLevel, float r, float g, float b, int textureType, float multPower, float multAmount` | `TGBlocks.oreClusterCustom.setOre(id, …)`: define meta `id` of custom ore cluster block 1 to yield `ore` |
| `addOreCluster2` | same parameters | the same on [`oreClusterCustom2`](../07-blocks/block-catalogue.md#registered-blocks) |
| `addOreCluster` | `int id, ILiquidStack liquid, …same rest` | `setFluid(id, …)`: a fluid-yielding cluster on block 1 |
| `addOreCluster2` | `int id, ILiquidStack liquid, …` | the same on block 2 |

These are one-way. The custom cluster blocks only exist when the config options `addCustomOreCluster` / `addCustomOreCluster2` (general, default false) are on. Log: "Add OreClusterType for \<ore> as META:\<id> to CustomCluster\<1|2>".
