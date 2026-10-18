# Components and materials

Which Techguns parts become GT parts, and which stay GregGuns items.

- **Techguns baseline:** [components-and-materials.md](../../techguns-spec/06-items/components-and-materials.md), [item-catalogue.md](../../techguns-spec/06-items/item-catalogue.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (last row: "Techguns ores, ingots, plates, wires, circuits: Drop")
- **Pack mods:** GregTech (GT5-Unofficial, `gregtech`, 5.09.54.205), IC2 (hard dependency)
- **Roadmap:** M4 (production chain)

## What changes from Techguns

Techguns registers about 20 metal, plate, wire, circuit and polymer items, several under shared OreDict names (`plateSteel`, `circuitBasic`, `sheetPlastic`, `itemRubber`). GT already provides all of these. If GregGuns registered them, `plateSteel` and `circuitBasic` would hold two unrelated items, and the second would appear in every GT recipe that accepts the name. So GregGuns registers **none** of them.

GregGuns items are what GT does not make: gun parts, ammo, emitters and the like. Each is built *from* GT parts ([CONTRIBUTING § GTNH integration](../../../CONTRIBUTING.md#gtnh-integration)).

## Integration points

### GregTech: parts through the unificator

- **Pack config:** none. The pack's `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/` files tune GT, not its part list.
- **Source:**
  - [`GTOreDictUnificator.get(OrePrefixes, Object, long)`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L126-L148) returns the unified stack for a prefix and material, or `null` (or the replacement) when none exists.
  - [`OrePrefixes`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/OrePrefixes.java) defines `plate` (L903), `plateDouble` (L892), `plateDense` (L834), `foil` (L925), `stick` (L949), `round` (L960), `bolt` (L971), `screw` (L990), `ring` (L1001), `springSmall` (L1012), `spring` (L1025), `wireFine` (L1036), `rotor` (L1048), `gearGtSmall` (L1058), `gearGt` (L1071), `cell` (L1119), `wireGt01` (L1896), `circuit` (L1974), `nugget` (L811), `ingot` (L670), `dust` (L801).
  - [`Materials`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Materials.java) has `Iron` (L93), `Copper` (L76), `Lead` (L95), `Gold` (L87), `Titanium` (L136), `Aluminium` (L59), `Uranium235` (L151), `Bronze` (L435), `Steel` (L658), `StainlessSteel` (L657), `TungstenSteel` (L770), `Rubber` (L507), `Polyethylene` (L496), `Electrum` (L456), `Carbon` (L70). There is no `Plastic`, no `ObsidianSteel` and no carbon-fibre material.
  - Circuits are addressed by tier material, for example `GTOreDictUnificator.get(OrePrefixes.circuit, Materials.MV, 1)`, as in [`Assembler.java` (BartWorks)](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/recipes/Assembler.java#L69).
  - GT machine parts live in [`ItemList`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java): `Electric_Motor_LV` (L358), `Electric_Piston_LV` (L435), `Robot_Arm_LV` (L450), `Emitter_LV` (L465), `Sensor_LV` (L480), `Circuit_Board_Basic` (L332), `ActivatedCarbonFilterMesh` (L2205), `Battery_RE_LV_Lithium` (L526). Each part has one entry per voltage tier.
- **GregGuns does:** fetches every GT part with `GTOreDictUnificator.get(...)` or `ItemList.<part>.get(n)` when it writes a recipe. A `null` return skips the recipe and logs a warning; it never falls back to a GregGuns copy.
- **Pack-side change:** none.

### Part by part

Legend: **GT** means GregGuns registers nothing and recipes use the GT part. **Keep** means a GregGuns item. **Drop** means the item does not exist in GregGuns. "Tier" is the D1 tier of the *consuming* recipe.

| Techguns item (meta, OreDict) | GregGuns | GT part or reason |
|---|---|---|
| Tin, Copper, Iron, Bronze, Steel, Lead, Titanium plate (41-45, 96, 104) | **GT** | `OrePrefixes.plate` with `Materials.Tin`, `Copper`, `Iron`, `Bronze`, `Steel`, `Titanium`, `Lead` |
| Titanium ingot, Titanium ore (95, 94) | **GT** | `OrePrefixes.ingot` `Materials.Titanium`; ore comes from the GTNH ore system ([0005](../../design/0005-world-presence.md): no ore generation) |
| Obsidian Steel ingot and plate (40, 65) | **Drop as a material** | GT has no such material. See [Open questions](#open-questions) for the tier remap |
| Copper Wire (57, `wireCopper`) | **GT** | `OrePrefixes.wireFine` with `Materials.Copper` |
| Gold Wire (86, `wireGold`) | **GT** | `OrePrefixes.wireFine` with `Materials.Gold` (or `Electrum`) |
| Circuit Board (68, `circuitBasic`) | **GT** | `OrePrefixes.circuit` with `Materials.LV`, or `ItemList.Circuit_Board_Basic` for the bare board |
| Elite Circuit Board (87, `circuitElite`) | **GT** | `OrePrefixes.circuit` with the consumer's tier material (MV for MV recipes, HV for HV recipes) |
| Raw Rubber, Rubber Bar (70, 73; `itemRawRubber`, `itemRubber`) | **GT** | `OrePrefixes.plate` with `Materials.Rubber`; raw rubber is `Materials.RubberRaw` |
| Raw Plastic, Plastic Sheet (71, 72; `rawPlastic`, `sheetPlastic`) | **GT** | `OrePrefixes.plate` with `Materials.Polyethylene` (GT's plastic) |
| Yellowcake, Enriched Uranium (101, 102; `dustUranium`, `ingotUraniumEnriched`) | **GT** | `dust`, `ingot` with `Materials.Uranium` and `Uranium235` |
| TGX (60) | **GT** | `Materials.TNT` and the GT explosive set; no GregGuns explosive unless a gun needs a distinct one |
| Electric Engine (64) | **GT** | `ItemList.Electric_Motor_LV` and up, by tier |
| Coil (69) | **GT** | Folded into the motor/emitter recipes; no standalone item |
| Machine Upgrade, Stack Operation (52) | **Drop** | Belongs to the dropped Techguns machines ([0002](../../design/0002-machine-deconfliction.md)) |
| Mechanical Parts T1, T2, T3 (30, 34, 37) | **Keep** | Built in an Assembler from `gearGtSmall`, `springSmall` and `screw` of the tier's material. No GT part matches the Techguns meaning, and receivers need a tier-specific input |
| Iron, Steel, Obsidian Steel, Carbon Receiver (53-56) | **Keep** | GregGuns gun parts, built from GT `plate`, `bolt`, `stick` |
| Stone, Iron, Hardened Steel, Carbon Barrel (25, 27, 33, 85); Laser Barrel (35) | **Keep** | GregGuns gun parts. Barrels from GT `stick` or `pipeTiny`-class parts of the tier's material |
| Wooden Stock, Plastic Gun Stock, Carbon Gunstock (26, 31, 84) | **Keep** | Wooden from planks; plastic from `Materials.Polyethylene` plates |
| Pump Mechanism (32) | **Keep** | Built from GT `springSmall`, `ItemList.Electric_Piston_LV`-class parts if needed |
| Laser Focus (36), UV Emitter (92), Sonic Emitter (97), Rad Emitter (108) | **Keep** | The emitters wrap `ItemList.Emitter_<tier>`, chosen by the consumer's tier. D1 keeps the UV Emitter as an EV component |
| Reaction Chamber Heat Ray Focus (98) | **Keep** | Component of the kept multiblock ([0002](../../design/0002-machine-deconfliction.md)) |
| Carbon Fibers, Carbon Plate (61, 62; `fiberCarbon`, `plateCarbon`) | **Open** | GT core has no carbon-fibre item. The pack may supply one through NewHorizonsCoreMod; check at M4 ([Open questions](#open-questions)) |
| Gas Mask Filter (74) | **Keep** | Built from `ItemList.ActivatedCarbonFilterMesh` |
| Heavy Cloth (39), Treated Leather (109), Bio Mass (38) | **Keep** | Vanilla wool/leather and GT `Materials.Leather`, `Materials.Biomass`; the Chem Lab path is dropped, so recipes use the GT Chemical Reactor, Mixer or the crafting grid |
| Cybernetic Parts (91), Power Plating (89), Anti Gravity Core (100) | **Keep** | GregGuns components, built from GT circuits, plates and `NetherStar` dust (`Materials.NetherStar`) |
| Small Nuclear Warhead (103), Nuclear Power Cell, Depleted (106, 107) | **Keep** | GregGuns items; their radiation comes from GT ([hazards-and-tooltips.md](hazards-and-tooltips.md#gt-radiation-for-nuclear-items)) |
| Nether Charge (63), Quartz Rod (99) | **Keep** / **Drop** | Nether Charge is ammo ([ammo.md](ammo.md)). Quartz Rod is a Reaction Chamber intermediate for blaze rods, which vanilla already gives; drop unless the chamber recipe needs it |
| Glider parts, Oxygen Mask, turret armour, ore drill heads | Other areas | Armour: 05. Turret armour: 09. Ore drill heads: dropped with the Ore Drill ([0002](../../design/0002-machine-deconfliction.md)) |

### Ore-dictionary names

- GregGuns registers **no** metal, plate, wire, circuit, plastic or rubber OreDict name. GT's unificator owns them.
- Items GregGuns keeps are registered under **namespaced** OreDict names so that only GregGuns recipes see them, for example `gregguns:receiver` or `partGunReceiver`. The exact scheme is a M4 decision; the rule is that a name must never collide with a GT `OrePrefixes` name (`plate`, `circuit`, `wireFine`, and so on).
- Every kept item that is made from GT materials carries `ItemData` so GT's recycler and Arc Furnace return its material: `GTOreDictUnificator.addItemData(stack, new ItemData(Materials.Steel, OrePrefixes.plate.getMaterialAmount()))`. Source: [`addItemData`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359) (does not overwrite an existing composition) and the [`ItemData` constructors](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/objects/ItemData.java#L51-L63).

### Recipes

Machine recipes use the GT recipe builder, `GTValues.RA.stdBuilder()`, with the EU/t from `TierEU` that matches the item's D1 tier. Precedent: [`ProcessingRound`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingRound.java#L28-L36) (`.eut(TierEU.RECIPE_ULV)`, `.addTo(latheRecipes)`). Tier constants: [`TierEU`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L25-L28). Which map each part uses is in [ammo.md](ammo.md#gregtech-recipe-maps-for-ammo) for ammo and left to M4 for the rest.

## Requirements

- **GGS-06-10.** GregGuns registers no item whose ore-dictionary name starts with a GT `OrePrefixes` name for a material GT provides (`plate*`, `ingot*`, `wire*`, `circuit*`, `nugget*`, `dust*`).
- **GGS-06-11.** Every GT part in a GregGuns recipe is fetched through `GTOreDictUnificator` or `ItemList`. A `null` result skips that recipe with a logged warning.
- **GGS-06-12.** Every kept GregGuns item made from GT materials has `ItemData` registered at init.
- **GGS-06-13.** Techguns' base-metal OreDict lookups (`ingotSteel`, `ingotLead`, `nuggetCopper` and similar, see [ITEM-02](../../techguns-spec/06-items/index.md#suspected-bugs)) are not ported.

## Tests

| ID | Verified by |
|---|---|
| GGS-06-10 | Unit test: iterate the item registry and fail on a forbidden OreDict prefix |
| GGS-06-11 | Unit test of the recipe loader with a stubbed unificator that returns `null` |
| GGS-06-12 | Unit test: each kept item resolves `GTOreDictUnificator.getItemData` to non-null |
| GGS-06-13 | Code review plus a grep check in the dev self-check |

## Open questions

- **Obsidian Steel and Carbon tiers.** Techguns' three material tiers (iron or steel, obsidian steel, carbon) do not match GT. GT has neither Obsidian Steel nor carbon fibre in core. Which GT material stands in for each Techguns tier (for example Steel, StainlessSteel, TungstenSteel) should be a new record, "0007 material remap", settled before M4. D1 gate materials are the starting point.
- **Carbon fibre.** Does GTNH 2.9.0-RC-2 carry a carbon-fibre item (GTNH-specific, not in GT core)? Search `reference/gtnh/2.9.0-RC-2/.minecraft/mods/` (NewHorizonsCoreMod) at M4.
- **Quartz Rod and the Reaction Chamber's intermediates.** Depends on the Reaction Chamber's recipe design in area 08.
