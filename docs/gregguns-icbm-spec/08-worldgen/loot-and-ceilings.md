# Loot, machines and ceilings

- **Baseline:** none.
- **Decisions:** [D38](../DECISIONS.md#d38-in-detail), [D17](../DECISIONS.md#d17-in-detail), [D128](../DECISIONS.md), [D129](../DECISIONS.md), D182 to D189, D315 to D317
- **Pack mods:** GregTech 5.09.54.205 (`DimensionHelper`, `ItemList`, materials), Galacticraft (NASA Workbench), the pack's planets
- **Roadmap:** [I6](../../ROADMAP-ICBM.md#i6--world-missile-silos-on-every-planet)

## Ceilings (`SiloCeiling`)

A ceiling is a GT voltage index (EV 4 ... UMV 12). Keyed by the dimension's key (`DimensionKey`: a Galacticraft body's unlocalized name, as GalacticGreg keys it; any other dimension keeps its provider name): Overworld and Moon EV; Mars, Phobos, Deimos IV; Ceres, Callisto, Europa, Ganymede LuV; Io, Mercury, Venus ZPM; Enceladus, Titan, Miranda, Oberon UV; Proteus, Triton UHV; Pluto, Haumea, Makemake UEV; Barnard C, E, F, Centauri A, Vega B UIV (T Ceti E has no row: it is an ocean world, D239); Anubis, Horus, Maahes, Neper, Seth UMV. Ross128b and Ross128ba roll EV to UMV per complex (`SiloCeiling.roll`, default weights 30, 22, 16, 11, 8, 6, 4, 2, 1, config `icbm.siloComplexRoss128Weights`, nine values EV to UMV; `ComplexPlacer.ceilingFor` is the one entry point). Belts are not in the config list, so they get no complex; a dimension the owner adds to `siloComplexDimensions` that the table lacks gets the lowest ceiling, EV.

## Chest budget (`ChestBudget`, D315)

A whole complex holds exactly **100** loot chests when abandoned, **200** when partly abandoned and **300** when operational, the same on every planet (config `icbm.siloComplexChestTargets`). Every one of them holds loot: no chest is ever empty.

- **Spots.** The room blueprints stamp `LOOT_CHEST` marks, about 550 to 650 per complex. The spots are those of the hub and of every room that has not caved in; tunnels hold none. A caved-in room gets neither chests nor crates.
- **Outside the total.** Armory display chests (`GUN_DISPLAY`, a gun in a frame over a chest of its ammo) and item-frame walls are not spots. Every display of a standing armory is placed at every level, on top of the 100, 200 or 300.
- **The split.**
  1. Every room with a spot gets one chest, and the commander's `safe` spot is always chosen.
  2. The rest is split by largest remainder on the room's weight: its kind's share of a typical complex (`RoomLoot`, for example 7.5% bunks, 6.5% missile assembly, 4.5% cafeteria, 1% showers) times its size (interior over the kind's nominal interior, 0.6 to 1.8) times its state (in a partly abandoned complex, 1.3 for a lit room and 0.7 for a dark one).
  3. No room takes more chests than it has spots. A full room's overflow goes first to the rooms of its theme group (food, crew, arms, missile, command, works, stores), then to every room.
  4. Inside a room, the chests spread evenly through the blueprint's spot order (`floor((i + 0.5) x spots / chests)`), so they are not bunched at one end.
- **Left-over spots** become an empty wooden crate (Chisel's crate planks in the room's wood, `chisel:<wood>_planks` meta 10, vanilla planks without Chisel).
- **Pure and window-safe.** The answer depends on the plan, the level and the room states only, so every populate window agrees.
- **Per plan, not per world.** The budget cannot see terrain. A chosen chest in a column the terrain cuts off (D207) is not written, so a complex under a valley or a lake holds fewer than the target (D345).

## Room tables (`SiloLootTables`)

One table per room theme and ceiling, registered as `gregguns:silo_<table>_<ceiling>`, plus `gregguns:silo_junk`, so TooMuchLoot can retune any of them ([pack-loot](../../gregguns-spec/06-items/pack-loot.md#toomuchloot)). The room a chest stands in picks its table (`RoomLoot`); a mark's key matters only for the commander's `safe` and the armory's `armory_ammo` display chests. Each table mixes shared pools (`SiloPools`) by share with `LootMix`, and every table carries 5 to 20% of everyday clutter (`common`: torches, coal, paper, iron, bread, coffee).

| Room | Table | Theme |
|---|---|---|
| Mess galley | `galley` | ingredients 30, meals 25, produce 15, cookware 15, raw meat 5: food 75% |
| Cafeteria | `canteen` | meals 35, snacks 20, drinks 20, ingredients 10: food 85% |
| Cold storage | `pantry` | raw meat 30, preserved 25, produce 20, ingredients 15, drinks 5 |
| Underground farm, mushroom cellar | `farm`, `cellar` | seeds, produce, farm supplies; mushrooms |
| Bunks | `bunks` | **armor 50** (GregGuns combat sets, vanilla, Pam's hardened leather, goggles, masks), personal effects 20, sidearm ammo 10, snacks 10 |
| Showers, rec lounge | `laundry`, `lounge` | linens and armor; snacks, drinks, records, coins |
| Infirmary, airlock | `medical`, `decon` | bandages, medkits, potions; hazmat gear, filters |
| Guard post, brig | `guard`, `brig` | sidearms and their ammo 50; confiscated evidence |
| Shooting range | `range` | **ammo 45, guns 15, grenades 5**, gun parts 10, targets 10 |
| Armory back wall | `armory` | ammo 40, guns 15, grenades 10, gun parts 10, combat armor 10 |
| Launch control, radar, comms, server room | `control`, `radar`, `comms`, `server` | circuits, data sticks and orbs, sensors, emitters, guidance computers, maps |
| Commander's office | `office` (+ one `safe`) | papers, clipboards, glasses; the safe holds the ladder plates and ingots, gold, silver, coins and a few arms |
| Missile assembly | `assembly` | **missile body parts 50** (Galacticraft compressed steel, heavy plating, fins, engines, nose cones, guidance computers, now and then a whole body), **warhead parts 15**, hardware 15, components 10 |
| Warhead magazine | `magazine` | warhead parts 45 (inert warheads, plastic explosive, fuzes, liners, sleeves, canisters, launcher warheads), explosives 30 |
| Works rooms | `workshop`, `motorpool`, `fuel`, `power`, `hvac`, `pumps`, `chemlab`, `incinerator` | GT hardware and components, rover parts, fuel and canisters, batteries and cable, fans and vents, pumps and pipes, chemical cells, ash and scrap |
| Stockpile, hub | `stockpile`, `salvage` | bulk ingots and plates (the ladder above EV), preserved food, hardware |

- **Sources.** GregGuns, GregTech (materials through the unificator, circuits, components, cells), Pam's HarvestCraft and vanilla (food, ingredients, produce, seeds), IC2 (canned food, hazmat, scrap), Galacticraft (the missile body's rocket parts, D69), BiblioCraft (office clutter), Forestry and Railcraft. No magic mods, no line with NBT, and no live warhead, missile chip or schematic (D17; the chip and the silo parts stay one-off bonuses).
- **Whole missile bodies** are rare finds on the assembly floor only: intact in a partly abandoned or operational complex, a salvage body (D186) in an abandoned one. The body is the planet's rocket tier (T1 on the Overworld).
- **Silo arms** (`SiloArms`) are the complex's issue kit: the guns of the `Arsenal` tables from two below the ceiling's table up to it (never below MV), each table's weight halving per step down, ammo three to five times the guns' weight, the top table's grenades. Sidearms are the pistols, revolvers, sawed-offs and SMGs of that kit.
- **Ceilings (D38, D129).** Every line declares its tier and a table built at a ceiling drops every line above it, so an Overworld complex holds nothing above EV. GregGuns stops at ZPM; above it only the GT material **ladder** grows: HV Stainless Steel, EV Titanium, IV Tungstensteel, LuV HSS-G, ZPM Naquadah Alloy, UV Neutronium, UHV Cosmic Neutronium, UEV Infinity, UIV Transcendent Metal, UMV Space-Time, the top rung at the ceiling weighing three times the rest. The armory frames pick their guns from the GregGuns table N = min(ceiling, 7) ([pack-loot § Structure chest mix](../../gregguns-spec/06-items/pack-loot.md#structure-chest-mix)).

## Draw by room state (`RoomLootDraw`, D317)

The room's state (`RoomState`, from the activity level and, when partly abandoned, the room's own lamp roll) sets what one chest holds:

| State | Real picks | Junk | Counts | Spoiled | Worn gear | Richness |
|---|---|---|---|---|---|---|
| Staffed (operational) | 5 to 8 | none | full | none | none | 2 |
| In service (partly abandoned, lit) | 4 to 7 | none | full | none | none | 1 |
| Mothballed (partly abandoned, dark) | 2 to 5 | 1 to 2 | three quarters | 20% | half the gear, 10 to 40% | 0 |
| Gutted (abandoned) | 1 to 3 | 1 to 3 | half | 50% | all gear, 40 to 90% | 0 |

- A big rack holds one pick more (`stockpile`, `pantry`, `salvage`), a safe or a foot locker one fewer (`safe`, `bunks`, `laundry`); never fewer than one.
- **Spoil.** Meals, snacks, produce, ingredients and raw meat turn to rotten flesh (stews and soups to a bowl), drinks to an empty bottle, circuits and boards to scrap, batteries to a discharged one. Canned and preserved food, mushrooms, metals, ammo, guns, parts and armor never spoil. A spoiled stack is half the count.
- **Wear** takes durability off armor, tools and blades only; guns and magazines are never worn. A gun always brings its ammo, and spoil and wear never touch that ammo.
- **Never empty.** The first pick never spoils, so every chest holds at least one real find at every level; a table a pack has emptied falls back to torches.

## Machines (D129)

`MACHINE` marks become real machines, never above the ceiling. Radars and comms gear stand at the ceiling or one or two tiers below it (at least HV). **Every other room machine draws its tier from the metal props' ladder (D330, the I7 balance pass):** LV and MV weigh 4, the tiers between weigh 2, and the top two weigh 1, so on the Overworld (EV) about 80% are LV or MV and HV and EV are finds. The room keeps every machine its activity level gives it; only the tier, and so the free value, falls. Measured over 12 Overworld plans (`ComplexFreeValueTest`): about 22, 125 and 278 working room machines per Alpha, Bravo and Charlie complex, of which about 4, 25 and 55 are HV or EV (before: all of them, two thirds HV and one third EV). The metal props stay as D308 set them: about 34, 96 and 155 storage blocks per complex (Overworld, 9 ingots each, about a quarter of them stainless steel or titanium). Launch Control is GregGuns' MTE; the NASA Workbench is Galacticraft's block; generators and Battery Buffers are GT's at the highest tier GT has up to the pick; the rest (pump, air handler, fuel tank, comms, radar, workbench) use the GT machine of that tier where one exists and otherwise the GT hull of that tier. A dead machine is the hull. **No live warhead is ever placed (D17):** the magazine's casing stock is inert Stainless Steel frame boxes, and its chests hold Stainless Steel plates, the stock of a T1 casing. No warhead block and no warhead item appears anywhere in a complex (D210).

## Contents (`Placement.Bonus`)

| Bonus | Where | Chance |
|---|---|---|
| Damaged missile body of the planet's rocket tier, at most T3 (item damage 1: cannot be placed, is no body to the silo or any recipe, recycles for the plates only; D186, D211) | every `salvage` chest (the hub); operational: half | always (D17's main find) |
| Schematic chip (T1) | one `safe`, `comms` or `control` chest | 8%, 10%, 25% by level |
| Silo controller or Energy Hatch (whichever the silo lacks, D128) | one `safe`, `stockpile`, `workshop` or `control` chest, per silo | 25%, 40%, 60% |
| Data stick with another complex's block coordinates | one `comms` chest | half the chip chance, only when another complex exists |
| Stainless Steel plates (four) | `magazine` chests | always |
| Rocket fuel cells (8 to 16), the best rung the ceiling allows | `fuel` chests | always |

A `GUN_DISPLAY` (armory) holds one gun per frame drawn from the weapon tables at or below the ceiling, with an `armory_ammo` chest under it holding that gun's magazines and rounds (see silo-layout, Armory display walls).

## World-loot cap (`loot.maxTier`, D364)

The ceiling picks each table, but no stack a complex hands out is above the lower of the complex's ceiling and `loot.maxTier` (default EV), judged by the item's whole crafting chain (D363), and no stack with no known tier appears. That covers room chests and junk, bonus items and fuel rungs (`ComplexContents.lootTable`), furniture and furnaces (`TierGatedInventory`), stocked GT machines (every output of the stocked recipe allowed at the machine's tier and the cap, so an MV Fluid Solidifier never holds endgame metal) and armory frames (a gun above the cap leaves its frame empty; the display chest holds only allowed ammo). Spawners, guard posts, NPC equipment, machine tiers and metal props keep the planet ceiling: they are not loot. The full source list is in [structure-loot § Loot tier cap](../../gregguns-spec/11-worldgen/structure-loot.md#loot-tier-cap).

## Requirements

1. GGI-08-10: `SiloCeiling.forDimension` follows D38.
2. GGI-08-11: Ross128 rolls EV to UMV, seeded, weighted low.
3. GGI-08-12: no table line is above its ceiling, for every key and ceiling.
4. GGI-08-13: no chest drawn from a generated complex holds a stack above the ceiling (unit over many draws, scenario over generated chests).
5. GGI-08-14: no placed machine is above the ceiling; no live warhead is placed.
6. GGI-08-15: a complex holds exactly 100, 200 or 300 room chests (abandoned, partly abandoned, operational; config), with every armory display chest on top, whenever its blueprints hold that many spots.
7. GGI-08-16: every standing room with a spot holds at least one chest, a caved-in room none, and the commander's safe is always a chest; every spot left over is an empty wooden crate.
8. GGI-08-17: no chest is empty, and every chest holds at least one real find (not junk, not spoiled).
9. GGI-08-18: junk, spoiled food and worn gear appear only in mothballed and gutted rooms; real finds per chest rise with the activity level.
10. GGI-08-19: the room themes hold: the mess and cafeteria are at least 60% food, the bunks at least 40% armor, the range and the armory at least 55% guns, ammo and grenades, the assembly floor at least 50% missile body and warhead parts; no table holds a live warhead, a chip or a schematic, and only the assembly floor holds a whole body.
11. GGI-08-45: no stack a complex hands out (room chest, junk, bonus, furniture, furnace, stocked GT machine, frame or display) is above the lower of the ceiling and `loot.maxTier` by its whole crafting chain, or of unknown tier (D364).

## Tests

Unit: `SiloCeilingTest`, `SiloLootTablesTest` (ceilings, themes, no live warhead, every room kind has a table), `SiloPoolsRefsTest` (every pack item is in the pack's id list or checked against its source, every GregGuns item has a name), `ChestBudgetTest` (exactly the target over 1,000 synthetic complexes per level, one chest per standing room, none in a caved-in room, the safe, theme shares, lit over dark, spread), `RoomLootDrawTest` (never empty, picks per state, junk, spoil, wear, counts, companions), `ComplexContentsTest`, `CeilingSweepTest` (every hosted dimension, many layouts, all levels). Scenarios: `icbm.complex.chest_loot` (a complex at each level written and every chest opened: the count, none empty, a real find in each, every item resolves, nothing above the ceiling, junk, spoil and wear by level, crates on left-over spots, a chest under every display; then the room themes over 24 staffed complexes filled through the registered tables), `icbm.complex.loot_ceiling`, `icbm.complex.loot_sweep` (about 6,000 chests across every dimension, Ross128's rolled ceilings included), `icbm.complex.bonus_items`, `icbm.complex.machines`, `icbm.complex.salvage`, and for GGI-08-45 `loot.obtain.silo`, `loot.obtain.machines`, `loot.obtain.furniture` and `loot.obtain.frames` (every table and pool listed, and chests, racks, machines and frames drawn at every ceiling from EV to UMV with the cap at EV and at UMV).

## Open questions

Checked on the box: every ladder line resolves at every ceiling (`icbm.complex.loot_ceiling`, 0 unresolved), and every room table line of a loaded mod resolves (`icbm.complex.chest_loot`). The pack has no `gt:dust.SodiumHydroxide` or `gt:cell.HydrochloricAcid`, so the pools leave them out.
