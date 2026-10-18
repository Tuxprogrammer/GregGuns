# Armour and equipment recipes (GT recipe maps)

- **Techguns baseline:** [recipes.md](../../techguns-spec/05-armor/recipes.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (Fabricator replaced by Assembler and Circuit Assembler)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205)
- **Roadmap:** M3 (armour and equipment)

## What changes from Techguns

Techguns builds armour in the crafting table from Techguns plates, cloth and circuits. GregGuns makes the final step a **GT recipe at the item's tier voltage**, with at least one gate material from [0001](../../design/0001-tier-map.md). Techguns plates, ingots and circuits are dropped for GT materials ([0002](../../design/0002-machine-deconfliction.md)). Camo variants and repair stay with the Camo Bench and Repair Bench, which have their own area sheets.

## Integration points

### GregTech: the Assembler and the voltage helpers

- **Pack config:** none.
- **Source:** the Assembler recipe map is `RecipeMaps.assemblerRecipes` (up to 9 item inputs, 1 output, 1 fluid input): [RecipeMaps.java#L1266-L1272](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266-L1272). Recipe voltage uses the `TierEU.RECIPE_*` constants, not the raw tier voltages: [TierEU.java#L7-L34](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L7-L34). Circuit Assembler is [RecipeMaps.java#L1277](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1277).
- **GregGuns does:** registers armour and equipment recipes through GT's recipe builder into `assemblerRecipes`, looks materials up through GT's material unification, and takes the voltage from `TierEU.RECIPE_*`. The self-check from [0001](../../design/0001-tier-map.md) fails the build if an item's recipe voltage is below its tier or has no gate material.
- **Pack-side change:** none.

### Starting recipe table (proposal; confirm each against GTNH progression when written)

| Tier ([0001](../../design/0001-tier-map.md)) | Items | Map | Gate material | Notes |
|---|---|---|---|---|
| Steam / ULV | T1 Scout, T1 Miner | Crafting table | Bronze plate, wrought iron | Hand-made, as steam-age gear |
| LV | T1 Combat | Assembler (LV) | Steel plate | |
| MV | T2 Combat, Commando, Riot; Beret; Gas Mask; Night Vision Goggles | Assembler (MV) | Aluminium plate | Riot adds lead plate; Gas Mask adds a filter item |
| HV | Jump Pack, Glider, Oxygen Mask, Oxygen Tanks | Assembler (HV) | Stainless steel plate | Tanks also need a compressed-air tank |
| EV | T3 Combat, Steam armour, Jetpack (if kept) | Assembler (EV) | Titanium plate | Carbon plates and fibres from Techguns map to GT carbon materials |
| IV | Power armour, Exo suit, Anti Gravity Device | Assembler (IV), with circuits from the Circuit Assembler | Tungstensteel plate | |

## Requirements

- **GGS-05-29.** Every armour and equipment recipe is in `assemblerRecipes` (or, for the two steam-age sets, a shaped crafting recipe) with voltage `TierEU.RECIPE_<tier>` for its [0001](../../design/0001-tier-map.md) tier.
- **GGS-05-30.** Every recipe has at least one input from that tier's gate material.
- **GGS-05-31.** No recipe uses a Techguns plate, ingot or circuit.

## Tests

- GGS-05-29, 30: dev self-check (unit test over the recipe registry, as [0001](../../design/0001-tier-map.md) requires).
- GGS-05-31: unit test that scans recipe inputs for the removed Techguns item names.
- Manual: NEI shows each recipe under the Assembler.

## Open questions

- **Steam-age crafting.** Is a crafting-table recipe acceptable for the Steam / ULV tier, or must it use a steam-powered GT machine? Needs a check of GT's steam machines, then a note in [0001](../../design/0001-tier-map.md) or a new record.
