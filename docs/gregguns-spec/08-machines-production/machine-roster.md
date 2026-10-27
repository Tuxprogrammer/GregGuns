# Machine roster

The single authoritative list of every machine GregGuns implements: what it is, whether it is a single block or a multiblock, how heavily players will use it, its tier range, its MTE IDs and its sheet. If another sheet disagrees with this one, this one wins and the other sheet is stale.

- **Techguns baseline:** [08 index](../../techguns-spec/08-machines-production/index.md), [09 index](../../techguns-spec/09-machines-utility/index.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (amended by the owner's machine policy and the Ammo Press decision, see below), [0003 energy model](../../design/0003-energy-model.md), [0008 exact-tier charging](../../design/0008-exact-tier-charging.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205; the jar also carries GT++, GoodGenerator, BartWorks, KubaTech, TecTech and GGFab), StructureLib (`structurelib`, 1.4.45)
- **Roadmap:** M4 (production), M6 (utility and defense)

## What changes from Techguns and from 0002

| Item | Before | Now | Why |
|---|---|---|---|
| Machine policy | Kept machines were single blocks, except the Reaction Chamber, which was multiblock only | GregGuns' own machines are Techguns-style **single blocks**, and every machine players use regularly also gets a **multiblock**. The bottleneck machines are the bare minimum that must have one. Multiblocks have distinctive shapes, never plain 3 x 3 x 3 cubes | Owner policy. GTNH is a large-scale production pack |
| Reaction Chamber, Retort | Single block (EV, IV) and the Retort multiblock | **Retired** by [0012](../../design/0012-retire-reaction-chamber-and-retort.md). The Laser Focus is made on GT's Laser Engraver and the Anti Gravity Core on GT's Assembler. MTE ids 20106 to 20108 are reserved forever. The Retort is archived (branch `archive/retort-multiblock`, tag `archive/retort-v1`) | The owner judged a bespoke machine for two recipes not worth keeping in a pack this rich |
| Ammo Press | "Replace" by GT Assembler, Forming Press and Extruder ([0002](../../design/0002-machine-deconfliction.md)) | **Reinstated** as an animated GregGuns single block with its own recipe map, plus the Ammunition Works multiblock | Owner decision: ammo is the exception to "use GT machines everywhere" |
| Metal Press, Chem Lab, Fabricator, Charging Station, Ore Drill, Scrapper | Dropped or replaced | Unchanged. Still dropped, still GT recipe maps ([recipe-map-replacements.md](recipe-map-replacements.md)) | GT covers them and the owner did not reinstate them |
| Turret, Repair Bench | Single block | Single block **and** a multiblock each (Rampart, Armory Gantry) | Both are used regularly |

"Usage class" is defined here:

| Class | Meaning | Multiblock? |
|---|---|---|
| **Bottleneck** | The machine whose throughput gates progression or whose volume dwarfs every other machine. Players will build many of them or run them flat out | **Yes, mandatory** (the bare minimum set) |
| **Regular** | Used every play session, in numbers | **Yes** |
| **Occasional or cosmetic** | Used rarely, once per item, or only for looks or storage | No. The reason is stated per machine |

## Roster

> **Built so far:** the five Ammo Press tiers, MTE **20100** to **20104** (`GregGunsMTEIDs.AMMO_PRESS_*`), with the front overlay synced to recipe progress ([ammo-press.md](ammo-press.md#m4-tiers-deviation)). The Ammunition Works, MTE **20105** (`GregGunsMTEIDs.AMMO_WORKS`, `MTEAmmoWorks`), is built ([multiblocks.md](multiblocks.md#ammunition-works-built-notes)). The Turret, MTE **20116** to **20119** (`GregGunsMTEIDs.TURRET_*`, `MTETurret`), is built in M6 ([turret built notes](../09-machines-utility/index.md#turret-built-notes-m6-chunk-2)). The Rampart (**20120**, `MTERampart`) and the Gun Mount Hatches (**20121** to **20124**, `MTEGunMountHatch`) are built in M6 ([Rampart built notes](../09-machines-utility/index.md#rampart-built-notes-m6-chunk-5)). The other machines stay M4. The roster has no ID for a steel High Pressure steam press.

IDs come from the proposed block 20100-20163 ([metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-mte-id-reservation)). Single-block tiers are contiguous. Tier names follow GT: Steam is the Bronze steam machine, then LV, MV and so on.

| Machine | Kind | Role | Usage class | Tier range | MTE IDs | Sheet |
|---|---|---|---|---|---|---|
| **Ammo Press** | Single block, tiers | Makes bullets, shells, grenades, rockets, empty and filled magazines in its own recipe map `gregguns.ammoPress`. Animated | Bottleneck (single form is the unit; the multiblock is the scale) | Steam (Bronze), LV, MV, HV, EV | 20100 Steam, 20101 LV, 20102 MV, 20103 HV, 20104 EV | [ammo-press.md](ammo-press.md) |
| **Ammunition Works** | Multiblock | Same map as the Ammo Press, high parallels; glass side windows and a visible press line inside (frame rods, pipe runs, gearboxes, an assembler arch); an optional filling bench behind the packing end loads magazines from its output | **Bottleneck** | Controller HV; hatches LV to IV | 20105 | [multiblocks.md](multiblocks.md#ammunition-works) |
| ~~Reaction Chamber~~, ~~Retort~~ | Retired | Removed by [0012](../../design/0012-retire-reaction-chamber-and-retort.md); the full Retort is archived on `archive/retort-multiblock` / tag `archive/retort-v1` | n/a | n/a | 20106 to 20108 **reserved forever, never reused** | [0012](../../design/0012-retire-reaction-chamber-and-retort.md) |
| **Repair Bench** | Single block, tiers | Repairs worn armor and held guns with GT materials. Bench tier must be at least the item's tier | Regular | LV, MV, HV, EV, IV | 20109 LV, 20110 MV, 20111 HV, 20112 EV, 20113 IV | [repair-bench.md](../09-machines-utility/repair-bench.md) |
| **Armory Gantry** | Multiblock | Bulk repair of NBT items (armor, guns) in a row of bays; a hoist (gearbox and motors), floor rails and pipe runs inside the portals | **Regular** | Controller HV; hatches up to ZPM, which sets the highest item tier it repairs | 20114 | [multiblocks.md](multiblocks.md#armory-gantry) |
| **Camo Bench** | Single block | Cycles the camo index of guns, armor and camo nets | Occasional or cosmetic | LV | 20115 | [camo-bench.md](../../techguns-spec/09-machines-utility/camo-bench.md) |
| **Turret** | Single block, tiers | One gun on a platform with a 30 HP mounted turret entity | Regular | MV, HV, EV, IV (turret tier at least the gun's tier) | 20116 MV, 20117 HV, 20118 EV, 20119 IV | [turret.md](../09-machines-utility/turret.md) |
| **Rampart** | Multiblock | A crenellated wall of up to ten turret mounts on one ammo feed and one power feed | **Regular** | Controller HV; mount tier is the Gun Mount Hatch tier | 20120 | [multiblocks.md](multiblocks.md#rampart) |
| **Gun Mount Hatch** | Hatch (part of the Rampart) | Holds one gun for one Rampart mount | Part of Rampart | MV, HV, EV, IV | 20121 MV, 20122 HV, 20123 EV, 20124 IV | [multiblocks.md](multiblocks.md#rampart) |
| **ICBM machines** (ICBM module; Launch Control built, the rest reserved) | Reserved sub-range | Launch control, missile silo, radar HV, radar EV, radar tower, battery, EMP tower; the module's plan is [ICBM roadmap](../../ROADMAP-ICBM.md). Names and exact IDs are fixed when each is built | per the ICBM spec | see ICBM spec | 20140 to 20163 reserved (`ICBM_FIRST` to `ICBM_LAST`); **20140 launch control is built (I2)**; **20141 missile silo is built (I3)**; **20146 EMP tower is built (I4)**; planned: 20142 radar HV, 20143 radar EV, 20144 radar tower, 20145 battery | [icbm-spec](../../icbm-spec/README.md) |
| Explosive Charge, Advanced Explosive Charge | Blocks with a tile entity, **not MTEs** | Placed, configured, detonated. See [explosive-charges.md](../09-machines-utility/explosive-charges.md) | Occasional | MV, HV (item tiers) | none | [09 recipes](../09-machines-utility/recipes.md#explosive-charges-2-recipes) |
| Weapon Chest, Reinforced Chest | Blocks, **not MTEs** | Decorative storage | Cosmetic | none | none | [storage-chests.md](../07-blocks/storage-chests.md) |

Retired and reserved forever: **20106-20108** (Reaction Chamber EV and IV, Retort; each keeps an inert placeholder so old worlds and inventories do not crash). Reserved for the ICBM module: **20140-20163**. Reserved and free: **20125-20139** (15 IDs). The first call on them is higher tiers (a LuV Ammo Press for the Nether Charge, a ZPM Turret) if a design record wants them.

### Why each class

Numbers use the recipe sheets' EU/t and durations. Demand figures are **planning assumptions** (labeled), not facts from the pack.

| Machine | Throughput justification |
|---|---|
| Ammo Press, Ammunition Works | One LV run is 16 bullets in 5 to 6 s at 30 EU/t ([06 recipes](../06-items/recipes.md)), about 160 to 190 bullets a minute. Every shot of every weapon tier consumes ammo, and each magazine is a three-step chain (bullets, empty magazine, filled magazine). Assumption: a defended base with 20 turrets burns about one 100-round magazine a minute per turret. That is 20 magazines or 160 rifle bullets a minute, which already keeps an LV press busy for bullets alone, before empty magazines, player kits and the hand-refill of 9-slot turret inventories. At 64 turrets or a group of players the number is far beyond what stacks of single blocks will do. Ammo is the highest-volume consumable in GregGuns, so it is the bottleneck by volume |
| Repair Bench, Armory Gantry | Proposed cost (needs a record, see Open questions): 2 s plus 8 s times the damage fraction, at half the recipe voltage of the bench tier. Assumption: 10 players with 4 armor pieces and 2 guns each is 60 items, about 5 minutes on one bench at an average of 5 s each, and under a minute on an 8-bay gantry. Repair load repeats after every fight, so it is regular |
| Turret, Rampart | Techguns upkeep is 5 RF/t and 1 HP of healing costs 500 RF ([turret.md](../../techguns-spec/09-machines-utility/turret.md#per-tick-logic-both-sides-power-consumption-is-authoritative-on-the-server)). Proposal: 4 EU/t upkeep and 120 EU per HP. The real cost is logistics: each turret has its own 9-slot ammo inventory and its own gun. 20 turrets means 20 inventories to restock. The Rampart puts up to ten on one Input Bus and one Energy Hatch |
| Camo Bench | No throughput: a click per item, once. A multiblock would be a bigger block for the same click. No multiblock |
| Explosive charges | A consumable placed and used once per job. No machine process to scale. No multiblock |
| Chests | Storage, no process. IronChest, JABBA and Drawers already do bulk storage. No multiblock |

## The GT coverage check

For every GT recipe map that GregGuns recipes use, which existing GT multiblock already parallelizes it. Verified in the clone at `reference/src/GT5-Unofficial@5.09.54.205/`. Where a map is covered, GregGuns adds no multiblock for it. Parallel formulas are per multiblock; EU/t is parallels times the recipe's EU/t, so the multiblock's Energy Hatches must supply it ([ParallelHelper#L398-L402](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/ParallelHelper.java#L398-L402)).

| Recipe map | GregGuns content using it | GT multiblock that runs it | Parallels and speed | Evidence |
|---|---|---|---|---|
| `assemblerRecipes` | Gun parts, guns (LV to IV), armor, turret armor plates, the Anti Gravity Core (RC-65, IV), the Rampart casings, charges, explosive and medkit items. Everything **except ammo** | GT++ Large Scale Auto-Assembler (`MTEAutoCrafter`, machine type "Assembler, LSAA") | `2 x tier` parallels, duration times 1/3 | [MTEAutoCrafter#L156-L158](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtPlusPlus/xmod/gregtech/common/tileentities/machines/multi/production/MTEAutoCrafter.java#L156-L158), [#L161-L169](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtPlusPlus/xmod/gregtech/common/tileentities/machines/multi/production/MTEAutoCrafter.java#L161-L169) |
| `assemblylineVisualRecipes` (the Assembly Line map) | LuV and ZPM weapons and the nuclear rocket and Nether Charge ([02 recipes](../02-guns/recipes.md)) | GT Assembly Line (`MTEAssemblyLine`) and GGFab's Advanced Assembly Line (`MTEAdvAssLine`) | One craft per run, with under-one-tick overclock inside the run. These are one-off high-tier items, so no parallel is needed | [MTEAssemblyLine#L219-L221](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEAssemblyLine.java#L219-L221), [#L280-L295](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEAssemblyLine.java#L280-L295), file `src/main/java/ggfab/mte/MTEAdvAssLine.java` |
| `cannerRecipes` | Filling and emptying fuel, bio and compressed-air tanks ([06 recipes](../06-items/recipes.md)) | Multi-Canner (`MTEMultiCanner`) | `8 x tier`, duration times 1/2 | [MTEMultiCanner#L148-L159](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEMultiCanner.java#L148-L159) |
| `chemicalBathRecipes` | Treated Leather | Industrial Chemical Bath (`MTEIndustrialChemicalBath`) | `4 x tier`, duration times 1/5 | [MTEIndustrialChemicalBath#L205-L223](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialChemicalBath.java#L205-L223) |
| `formingPressRecipes` | None now (Steam Armor Plating was retired by [0022](../../design/0022-steam-armor-steam-age.md)) | Industrial Forming Press (`MTEIndustrialFormingPress`) | `6 x tier`, duration times 1/6 | [MTEIndustrialFormingPress#L185-L202](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialFormingPress.java#L185-L202) |
| `mixerRecipes` | Concrete ([07 recipes](../07-blocks/recipes.md)) | Industrial Mixer (`MTEIndustrialMixer`) runs a **different map**, `mixerNonCellRecipes` | `8 x tier`, speed from the item-pipe casing tier | [MTEIndustrialMixer#L65](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L65), [#L219-L226](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L219-L226), [#L292-L294](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L292-L294); maps at [RecipeMaps#L534](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L534) and [#L2185](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L2185) |
| `latheRecipes`, `benderRecipes`, `wiremillRecipes` (GT's own recipes for rounds, foil, wire) | None of ours. GT makes them | Industrial multis exist for the bender and wiremill (`MTEIndustrialBendingMachine`, `MTEIndustrialWireMill`) | per GT | not GregGuns work |
| **`gregguns.ammoPress`** | All ammo and magazine recipes | **None. Not covered by GT.** The Ammunition Works is ours | see [multiblocks.md](multiblocks.md#ammunition-works) | n/a |
| `laserEngraverRecipes` | Laser Focus (RC-64, EV; [0012](../../design/0012-retire-reaction-chamber-and-retort.md)) | GT Industrial Laser Engraver (`MTEIndustrialLaserEngraver`) | per GT; a one-off gate item, so no parallel is needed | [MTEIndustrialLaserEngraver](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialLaserEngraver.java) |

Two findings that change recipes elsewhere:

1. **The Industrial Mixer does not run `mixerRecipes`.** A GregGuns Concrete recipe registered only to `RecipeMaps.mixerRecipes` is invisible to the multiblock. Register it to `mixerNonCellRecipes` as well, when it needs no cell. GT does this for its own acid chains ([AcidRecipes#L114](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/chains/AcidRecipes.java#L114)). Same trap as `chemicalReactorRecipes` in [recipe-map-replacements.md](recipe-map-replacements.md#integration-points).
2. **Ammo leaves `assemblerRecipes`.** The Large Scale Auto-Assembler covers the rest of the Assembler content, but ammo has its own machine, so nothing in the ammo chain should remain in the Assembler map. The move is applied; see [ammo-press.md](ammo-press.md#recipe-move-applied).

## Machines with no multiblock, and why

- **Camo Bench.** Cosmetic. One click per item. A larger block could not make it faster or more useful.
- **Explosive charges.** Not machines. Each is placed and fired once. Scale comes from placing more.
- **Weapon and Reinforced Chests.** Storage blocks with no process.
- **Hatches.** A hatch is a part of a multiblock, not a machine.

## Requirements

GGS-08-46 and 47 belonged to the retired Reaction Chamber sheet and are withdrawn ([0012](../../design/0012-retire-reaction-chamber-and-retort.md)).

- **GGS-08-48** The IDs in the Roster table are the IDs in `GregGunsMTEIDs`. Single-block tiers are contiguous. No MTE ID falls outside 20100-20163.
- **GGS-08-49** Every machine classed Bottleneck or Regular has a multiblock listed in [multiblocks.md](multiblocks.md). Every machine classed Occasional or Cosmetic states why it has none.
- **GGS-08-50** The Ammo Press, Repair Bench and Turret tier ranges are exactly those in the Roster table. For the Repair Bench and Turret the machine tier must be at least the item's [0001](../../design/0001-tier-map.md) tier.
- **GGS-08-51** A GregGuns recipe is registered to every GT map that a GT multiblock for the same job reads. Concrete is in both `mixerRecipes` and `mixerNonCellRecipes`.
- **GGS-08-52** The explosive charges and the chests register no MTE.

## Tests

| Req | Verification |
|---|---|
| 48 | Unit test over `GregGunsMTEIDs`: no duplicates, all in range, single-block tier IDs contiguous |
| 49 | Doc test: every Bottleneck or Regular row in this file has a heading in `multiblocks.md` |
| 50 | Unit test over the MTE registry: tier lists match |
| 51 | `runServer` smoke test: Concrete is present in both mixer maps after boot |
| 52 | Unit test: the charge and chest blocks are not `BaseMetaTileEntity` metas |

## Open questions

- **Repair Bench EU and timing.** The 2 s plus 8 s times damage fraction at half recipe voltage is a proposal. It settles the open question in [repair-bench.md](../09-machines-utility/repair-bench.md#open-questions). It needs a design record.
- **Machine tiers.** All tier ranges are proposals, as [0001](../../design/0001-tier-map.md) lists items only. A line in 0001 or a record settles them.
- **Free IDs.** The first use of 20125-20163 is not decided.
- **ID block.** Still needs the GTNH team's agreement ([index.md](index.md#open-questions)).
