# Loot, machines and ceilings

- **Baseline:** none.
- **Decisions:** [D38](../DECISIONS.md#d38-in-detail), [D17](../DECISIONS.md#d17-in-detail), [D128](../DECISIONS.md), [D129](../DECISIONS.md), D182 to D189
- **Pack mods:** GregTech 5.09.54.205 (`DimensionHelper`, `ItemList`, materials), Galacticraft (NASA Workbench), the pack's planets
- **Roadmap:** [I6](../../ROADMAP-ICBM.md#i6--world-missile-silos-on-every-planet)

## Ceilings (`SiloCeiling`)

A ceiling is a GT voltage index (EV 4 ... UMV 12). Keyed by GT's internal dimension name: Overworld and Moon EV; Mars, Phobos, Deimos IV; Ceres, Callisto, Europa, Ganymede LuV; Io, Mercury, Venus ZPM; Enceladus, Titan, Miranda, Oberon UV; Proteus, Triton UHV; Pluto, Haumea, Makemake UEV; Barnard C, E, F, Centauri A, T Ceti E, Vega B UIV; Anubis, Horus, Maahes, Neper, Seth UMV. Ross128b and Ross128ba roll EV to UMV per complex (`SiloCeiling.roll`, default weights 30, 22, 16, 11, 8, 6, 4, 2, 1). Belts and unlisted dimensions have none: no complex.

## Tables (`SiloLootTables`)

One table per loot key (`LootKeys`) and ceiling, registered as `gregguns:silo_<key>_<ceiling>`. Each line carries the voltage of its contents; lines above the ceiling are never built. The plate, ingot, rod, screw and gear **ladder** runs HV Stainless Steel, EV Titanium, IV Tungstensteel, LuV HSS-G, ZPM Naquadah Alloy, UV Neutronium, UHV Cosmic Neutronium, UEV Infinity, UIV Transcendent Metal, UMV Space-Time; the top rung at the ceiling weighs most. Guns come from the existing `weaponchest_tN` tables with N = min(ceiling, 7): GregGuns stops at ZPM, so above UHV the loot is GT material only (D38). Richness (0 to 2) comes from the activity level.

## Machines (D129)

`MACHINE` marks become real machines at the ceiling or one or two tiers below it (at least HV), never above it. Launch Control is GregGuns' MTE; the NASA Workbench is Galacticraft's block; generators and Battery Buffers are GT's at the highest tier GT has up to the pick; the rest (pump, air handler, fuel tank, comms, radar, workbench) use the GT machine of that tier where one exists and otherwise the GT hull of that tier. A dead machine is the hull. **No live warhead is ever placed (D17):** the magazine's casing stock is inert Stainless Steel frame boxes, and its chests hold Stainless Steel plates, the stock of a T1 casing. No warhead block and no warhead item appears anywhere in a complex (D210).

## Contents (`Placement.Bonus`)

| Bonus | Where | Chance |
|---|---|---|
| Damaged missile body of the planet's rocket tier, at most T3 (item damage 1: cannot be placed, is no body to the silo or any recipe, recycles for the plates only; D186, D211) | every `salvage` chest; operational: half | always (D17's main find) |
| Schematic chip (T1) | one `safe`, `comms` or `control` chest | 8%, 10%, 25% by level |
| Silo controller or Energy Hatch (whichever the silo lacks, D128) | one `safe`, `stockpile`, `workshop` or `control` chest, per silo | 25%, 40%, 60% |
| Data stick with another complex's block coordinates | one `comms` chest | half the chip chance, only when another complex exists |
| Stainless Steel plates (four) | `magazine` chests | always |
| Rocket fuel cells (8 to 16), the best rung the ceiling allows | `fuel` chests | always |

The `ITEM_FRAME_WALL` (armory) holds one gun per frame drawn from the armory table at the ceiling.

## Requirements

1. GGI-08-10: `SiloCeiling.forDimension` follows D38.
2. GGI-08-11: Ross128 rolls EV to UMV, seeded, weighted low.
3. GGI-08-12: no table line is above its ceiling, for every key and ceiling.
4. GGI-08-13: no chest drawn from a generated complex holds a stack above the ceiling (unit over many draws, scenario over generated chests).
5. GGI-08-14: no placed machine is above the ceiling; no live warhead is placed.

## Tests

Unit: `SiloCeilingTest`, `SiloLootTablesTest`, `ComplexContentsTest`. `CeilingSweepTest` (every hosted dimension, many layouts, all levels). Scenarios: `icbm.complex.loot_ceiling`, `icbm.complex.loot_sweep` (about 6,000 chests across every dimension, Ross128's rolled ceilings included), `icbm.complex.bonus_items`, `icbm.complex.machines`, `icbm.complex.salvage`.

## Open questions

Checked on the box: every ladder line resolves at every ceiling (`icbm.complex.loot_ceiling`, 0 unresolved).
