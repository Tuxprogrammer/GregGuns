# GregGuns multiblocks

The three multiblocks of the [machine roster](machine-roster.md): the Ammunition Works, the Armory Gantry and the Rampart. (The Retort was retired by [0012](../../design/0012-retire-reaction-chamber-and-retort.md) and is archived.) Each has a distinctive silhouette, a length or size that players choose, and a tier mechanic that is not just "more hatches".

- **Techguns baseline:** [reaction-chamber.md](../../techguns-spec/08-machines-production/reaction-chamber.md), [ammo-press.md](../../techguns-spec/08-machines-production/ammo-press.md), [repair-bench.md](../../techguns-spec/09-machines-utility/repair-bench.md), [turret.md](../../techguns-spec/09-machines-utility/turret.md), [framework.md § multiblock](../../techguns-spec/08-machines-production/framework.md#9-multiblock-framework)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (amended: see [machine-roster.md](machine-roster.md#what-changes-from-techguns-and-from-0002)), [0003 energy model](../../design/0003-energy-model.md), [0006 art direction](../../design/0006-art-direction.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205), StructureLib (`structurelib`, 1.4.45), ModularUI2 (`modularui2`, 2.3.92-1.7.10)
- **Roadmap:** M4 (Ammunition Works), M6 (Gantry, Rampart)

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| One multiblock, the Reaction Chamber, a hidden-block 3 x 4 x 3 | Three multiblocks, each a real StructureLib structure with visible casings and none a cube | Owner policy: a multiblock for every machine used regularly, with interesting shapes |
| Ammo Press, Repair Bench and Turret were single blocks with no scaling | Each keeps its single block and gains a multiblock for scale | Same |
| Fixed size | All three grow or shrink by repeating a segment or a bay | Players size the machine to their need, as GT's Distillation Tower and Neutron Activator do |

## Conventions for the diagrams

- Layers are listed **top to bottom**. Inside a layer, rows run from the **front** (the controller's face) to the **back**; columns run left to right as seen from the front. This is StructureLib's `transpose(...)` layout, the one `MTEAutoCrafter` and `MTEDistillationTower` use.
- `~` is the controller. `.` is "anything" (StructureLib's blank). `-` is "must be air". Other letters are in each legend. The `.`/`-` meanings are StructureLib's built-in ones; 1.4.45's builder wires `isAir`/`notAir` for its default characters (jar inspection, **decompiled evidence**), so the first prototype confirms exactly which character is which before the strings are frozen.
- Every shape string below is a design, not Java. Dimensions are width x height x length.
- GregGuns casings live on **texture page 50** ([metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-casing-texture-pages)). Each casing is one block with several metas. Their recipes are Assembler recipes in the same style as the Reaction Chamber Casing ([recipes.md](recipes.md#reaction-chamber-casing-1-recipe)); exact inputs are written when each multiblock reaches its milestone.
- All four extend `MTEExtendedPowerMultiBlockBase`, implement `ISurvivalConstructable`, and require one Energy Hatch and one Maintenance Hatch ([MachineStats.cfg](metatileentity-framework.md#gt5-unofficial-world-rules-every-mte-inherits) has `disableMaintenanceChecks=false`).

## GT precedents used

Six GT multiblocks carry the shapes and mechanics. Permalinks pin the 5.09.54.205 tag.

| Precedent | Used for | Evidence |
|---|---|---|
| **Distillation Tower** (`MTEDistillationTower`): a base piece, then a repeated `layer` piece up to 12 high, hatches per layer | Variable height and length from a repeated piece; hologram and survival build that add one piece at a time | [pieces L63-L76](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEDistillationTower.java#L63-L76), [check loop L254-L290](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEDistillationTower.java#L254-L290), [construct L315-L345](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEDistillationTower.java#L315-L345) |
| **Neutron Activator** (GoodGenerator): `bottom`, `mid` and `top` pieces, height counted in `checkMachine`, duration `0.9^(height-4)`; the hologram takes the stack size as the height | A growth count that changes performance; hologram sized by item stack size | [pieces L106-L108](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTENeutronActivator.java#L106-L108), [shapes L231-L233](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTENeutronActivator.java#L231-L233), [check L279-L297](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTENeutronActivator.java#L279-L297), [duration L126-L127](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTENeutronActivator.java#L126-L127), [construct L411-L419](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTENeutronActivator.java#L411-L419) |
| **Component Assembly Line** (GoodGenerator): 9 x 10 x 33, casing tier gates the recipe and sets a speed factor | Very long, non-cube; tier-by-casing gate (`insufficientMachineTier`) | [size L225](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTEComponentAssemblyLine.java#L225), [gate and speed L286-L305](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTEComponentAssemblyLine.java#L286-L305) |
| **Industrial Extruder**: 4 x 4 x 9, controller in a back corner, three casing types, a hatch adder with a custom hatch element | An asymmetric tunnel silhouette; adding a GregGuns hatch kind (`ExtruderHatchElement`) | [shape L60-L72](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialExtruder.java#L60-L72), [custom hatch element L231](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialExtruder.java#L231) |
| **Industrial Mixer**: 5 x 7 x 5, speed from the **item-pipe casing tier** (`chainItemPipeCasings`), `8 x tier` parallels | A belt of tiered casings that sets speed; the parallel formula | [shape size L193](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L193), [tiered pipe casing L136](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L136), [parallel and speed L219-L226](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L219-L226) |
| **Multi Smelter** (`MTEMultiFurnace`) and the tier-by-block helpers | Tier read from a block (coil level) with `ofCoil`; glass tier with `chainAllGlasses` | [coil element L84](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEMultiFurnace.java#L84), [parallel from coil L291](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEMultiFurnace.java#L291), [`ofCoil`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java#L608), [`chainAllGlasses`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java#L891) |
| **Cleanroom** (`MTECleanroom`): a non-recipe multiblock that sets its own EU draw and cycle length in `checkProcessing` | The Rampart, which runs without a recipe | [checkProcessing L213-L233](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTECleanroom.java#L213-L233) |
| **Assembly Line** (`MTEAssemblyLine`) overrides `checkProcessing` and reads item NBT (data sticks) instead of a plain recipe lookup | The Gantry, which prices a repair from item NBT | [checkProcessing L224](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEAssemblyLine.java#L224) |
| **Industrial Cutting Machine**: client tick, `IMTERenderer`, blade drawn in `renderTESR`, render toggle | The animated parts of the Ammunition Works | [ammo-press.md § Animation](ammo-press.md#animation) |
| **Large Scale Auto-Assembler** (`MTEAutoCrafter`): 3 x 3 x 3, `2 x tier` parallels | The plain cube we **avoid**, and the parallel formula we are measured against | [MTEAutoCrafter#L161-L169](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtPlusPlus/xmod/gregtech/common/tileentities/machines/multi/production/MTEAutoCrafter.java#L161-L169) |

## Casings used by the three multiblocks

All of these are GregGuns blocks on page 50 unless a GT name is given.

| Casing | Where | Notes |
|---|---|---|
| **Works Casing** | Ammunition Works hull | Hatches allowed on its `F` and `O` positions only |
| **Ram Housing** | Ammunition Works press columns | Active texture is animated (frame strip) |
| **Item pipe casing** (GT, any tier) | Ammunition Works belt | Tier sets speed, as in the Industrial Mixer |
| ~~Reaction Casing~~ (meta 2) | Retired; formerly the Retort | Kept as an inert decorative block for one alpha so placed Retorts leave no holes, then removed. Meta 2 is reserved forever |
| **Borosilicate glass** (GT, any tier, read with `chainAllGlasses`) | Gantry windows | GT glass |
| **Gantry Casing**, **Gantry Beam**, **Gantry Hoist**, **Repair Cradle** | Armory Gantry | Cradle is one per bay floor pad |
| **Bastion Plating** | Rampart, four metas: Iron, Steel, Aluminum, Carbon | The meta sets the armor value of every mount |
| `frameGt` Steel / Titanium | Gantry legs | GT frame boxes |

## Ammunition Works

**Concept.** A production line laid out along its length: a **feeder** with hopper towers, a row of **press bays** each topped by a ram housing that stands above the roof like a saw-tooth, and a taller **packing station** with a crane at the far end. Players lengthen the hall by adding press bays. Ammunition in, boxed ammunition out.

**Silhouette.** Seen from the side: a low feeder with two hoppers, then a run of rooftop teeth (one per bay), then a tall stepped end building. Seen from above: a 5-wide strip, 13 to 31 long. Never a cube.

**Dimensions.** 5 wide x 7 tall x (7 + 3n) long, where `n` is the number of press bays, 2 to 8. Default `n` = 4 is 5 x 7 x 19.

**Class and tiers.** Bottleneck. Controller tier HV (hull and parts at HV). Energy Hatches LV to IV. It runs the same map as the Ammo Press, `gregguns.ammoPress` ([ammo-press.md](ammo-press.md#the-recipe-map)).

**Optional filling bench.** A short annex on the outside of the packing end wall, built from Works Casing around six workbench tables. When it is complete the Works also loads magazines: it pulls finished rounds out of the packing station's Output Bus and empty magazines out of the bench's own Input Bus, and puts loaded magazines into the bench's own Output Bus. See [Filling bench](#filling-bench-optional).

### Legend

| Char | Block | Hatches allowed |
|---|---|---|
| `F` | Works Casing, feeder | Input Bus, Input Hatch, Energy Hatch, Maintenance Hatch, Muffler Hatch |
| `A` | Works Casing, plain | none |
| `O` | Works Casing, packing | Output Bus, Output Hatch, Energy Hatch |
| `M` | Ram Housing (roof tooth, and the press head over the belt) | none |
| `G` | Any glass: every tier GT lists, mixed is fine (the channel is Borosilicate Glass) | none |
| `R` | Steel Frame Box (press rods, crane rod) | none |
| `P` | Steel Pipe Casing (ceiling pipe runs) | none |
| `S` | Interior lamp: any block that gives light (glowstone, a lit redstone lamp, any mod lamp) or an unlit redstone lamp; the hologram shows glowstone | none |
| `B` | Steel Gear Box Casing (press drives, packing arch legs) | none |
| `N` | Motor Machine Casing (feeder belt drive) | none |
| `Y` | Assembler Machine Casing (packing arch over the belt) | none |
| `I` | GT item pipe casing, any tier | none. Tier is read |
| `K` | Works Casing, bench (the filling bench only; not counted toward the casing minimum) | Bench Input Bus, Bench Output Bus |
| `T` | Bench table: Natura wooden Workbench (any wood), else the vanilla Crafting Table or an Avaritia Dire Crafting Table | none |
| `~` | Controller | n/a |

### Pieces and layers

**Feeder** (3 long; the controller is at the front, middle, second layer from the bottom):

```
y6      y5      y4      y3      y2      y1      y0
.....   F...F   AAAAA   FFFFF   FFFFF   FF~FF   AAAAA
.....   .....   AAAAA   APSPA   G---G   AN-NA   AAIAA
.....   F...F   AAAAA   AP-PA   G---G   A---A   AAIAA
```

(Each column is one layer, `.` is anything. Within a column, rows run front to back.)

**Press bay** (3 long, repeated `n` times):

```
y6      y5      y4      y3      y2      y1      y0
.....   .....   AAAAA   APSPA   G---G   A---A   AAIAA
.....   ..M..   AAMAA   APRPA   G-R-G   ABMBA   AAIAA
.....   .....   AAAAA   AP-PA   G---G   A---A   AAIAA
```

**Packing station** (4 long, once, at the far end):

```
y6      y5      y4      y3      y2      y1      y0
.....   AAAAA   APSPA   G---G   G---G   A---A   AAIAA
..O..   AAOAA   AP-PA   GYYYG   GB-BG   AB-BA   AAIAA
..O..   AAOAA   APRPA   G-R-G   G---G   A---A   AAIAA
.....   AAAAA   AOOOA   AOOOA   AOOOA   AOOOA   AAAAA
```

The belt `I` runs down the middle of the floor, feeder to station. The hall between the walls is a 3-wide, 3-high open tunnel (4 high in the station), which is where the animated rounds travel.

**Windows and interior.** The side walls have glass windows (`G`) so the line can be seen from outside: one row in the feeder and the bays, two rows in the station. The front wall, the roof and the output wall carry no glass, so every hatch face stays a casing. Glass is the shared GT glass element, which accepts every registered tier (vanilla glass up to the top borosilicate) and does not require one tier: it is a look, not a progression wall. Inside, a pipe run (`P`) follows the ceiling on both sides end to end, each bay's ram housing drives a press: two steel frame boxes (`R`) down to a Ram Housing press head (`M`) one block above the belt, with a steel gearbox (`B`) on each side of the head; the feeder has a motor casing (`N`) on each side of the belt as its drive; the station has an assembler casing arch (`Y`) on gearbox legs that spans the belt (the packing core) and a frame rod hanging from the crane. One lamp (`S`) hangs under the ceiling of the feeder, of every bay and of the station, so the hall is lit behind the glass. The interior parts are required but are not casings: they are not counted toward the casing minimum and never sit on a hatch face.

**Block counts** (for `checkCasingMin`): feeder 18 `F`, 36 `A`, 2 `I`, 4 `G`, 4 `P`, 2 `N`; each bay 38 `A`, 3 `M`, 3 `I`, 6 `G`, 2 `R`, 6 `P`, 2 `B`; station 16 `O`, 55 `A`, 3 `I`, 12 `G`, 2 `R`, 6 `P`, 4 `B`, 3 `Y`. The casing minimum is the sum of `F`, `A`, `O` and `M` positions minus a fixed allowance of 12 hatches. For `n` = 4 that is 54 + 4 x 41 + 71 = 289 casing positions, so the check is 277. The interior totals are glass 16 + 6n, lamps 2 + n, pipes 10 + 6n, gearboxes 4 + 2n, frame boxes 2 + 2n, 2 motor casings and 3 assembler casings.

### Filling bench (optional)

**What it is.** A 5 x 3 x 3 annex built against the outside of the station's back wall, in the bottom three layers, so the upper rows of the back wall (`O` at `y4` and `y3`) stay free for the station's Output Buses. Works Casing `K` forms the floor, the outer ring and a hood; six tables `T` stand in two rows of three on the floor; one air pocket sits over the second row. Seen from the side it is a low shed tucked against the tall end building, which adds to the stepped silhouette and keeps the hall non-convex.

**Piece.** `bench` (3 long). The offset is the hall's full length, `7 + 3n` blocks behind the controller's aisle. The rows above the annex are `.` (anything).

```
y6..y3  .....  (all four layers: anything)
y2      y1      y0
KKKKK   KTTTK   KKKKK     aisle 0, against the station's back wall
K---K   KTTTK   KKKKK     aisle 1
KKKKK   KKKKK   KKKKK     aisle 2
```

(Each aisle is one line; columns are the layers `y2`, `y1`, `y0`, and within a column the characters run left to right as seen from the front.)

**Block counts.** 36 `K`, 6 `T`, 3 air. The bench adds no `F`, `A`, `O` or `M`, so the casing minimum is unchanged (for `n` = 4 it is still 277). The roof layers above the annex stay open and the station stays taller than the shed, so the whole Works still never forms a convex cuboid (GGS-08-82, checked by a unit test with and without the bench).

**Optional, and never a reason to unform.** `checkMachine` checks `feeder`, the bays and `station` as before and then probes `bench` with its errors thrown away. If the probe matches and the bench has at least one Bench Input Bus and one Bench Output Bus, the bench is **active**. In every other case (no bench, a half-built bench, a wrong block where a table goes, a missing bus) the probe leaves no trace: its hatches are dropped again, the Works stays formed, runs as it always did, and the bench is simply not active. The controller tooltip says the bench is optional, and the scanner (`getInfoData`) ends with "Filling bench: active" or "Filling bench: not active".

**Hatches.** The bench has its own buses, on any `K`: at least 1 Bench Input Bus (empty magazines in) and at least 1 Bench Output Bus (loaded magazines out). They live in separate lists from the hall's buses, so the normal recipe never reads or fills them. A bench Input Bus must be a normal bus (an ME Input Bus is not accepted there); a bench Output Bus may be a normal bus or the ME Output Bus. The bench uses the controller's Energy Hatches and needs none of its own. The station's Output Bus has to be a plain bus for the bench to use it (see below).

**The table block.** Chisel (`chisel:*` in the pack) has no table or workbench: its carving groups are stone, metal, glass, planks and decor. Searching every jar's textures in the GTNH 2.9.0-RC-2 pack found these full-size cubes that read as a work table: **Natura's wooden Workbench** (`Natura:Natura.workbench`, metas 0 to 12, one per wood, a plain `BlockWorkbench` with no tile entity; a wicker-style round grid top in the wood's color and tool-covered sides, the closest to a chisel-and-bench look), the **vanilla Crafting Table** (`minecraft:crafting_table`; the familiar grid and tools) and **Avaritia's Dire Crafting Table** (`Avaritia:Dire_Crafting`; a cyan lattice top on blue sides, a late-game look). The bench accepts all three, matched by registry name and meta. The hologram and the survival build place Natura's redwood workbench when Natura is loaded and the vanilla table otherwise. The Forestry Worktable, Galacticraft NASA Workbench, ProjectRed IC Workbench and BiblioCraft tables were passed over: they are machines or non-cubic models with tile entities. If none of the three is registered (not possible with vanilla present) a Works Casing is accepted at those six positions instead, so the structure still builds.

**Mechanic.** While the bench is active the controller also runs a fill step on its own timer, next to the normal recipe and never blocking it. It reads the existing loaded-magazine rows of `gregguns.ammoPress` (RC-44 for the 9mm pistol magazine, RC-45 for the SMG magazine, and so on: every row whose output is a loaded magazine and whose inputs are exactly that magazine's empty item and its loose round) and registers **no new recipes**.

- **Ratio.** One run is one such row: `N` empty magazines plus `N x r` rounds make `N` loaded magazines (RC-44: 4 empties and 12 rounds make 4 loaded). The rounds come from the station's Output Bus(es), the empties from the bench Input Bus(es), and the loaded magazines go to the bench Output Bus(es).
- **Runs per cycle** = the least of: the parallel cap (`4 x n x tier`, the Works' own), `empties / N`, `rounds / (N x r)`, the runs the Energy Hatches can power (`maxEU/t / (eut x 4^oc)`), and the runs whose output fits the bench Output Bus.
- **Time and power.** `base = row duration x belt multiplier`. Overclocks `oc` = voltage tier of the hatches minus the row's tier (at least 0, never so many that a cycle is under one tick), each halving the time and quadrupling the power: cycle time `ceil(base / 2^oc)` ticks, draw `eut x 4^oc x runs` EU/t while the cycle runs. The same overclock rule as the Works' normal recipes. A cycle whose hatches run dry pauses and resumes. RC-44 at an HV hall with a tier 2 belt: 160 ticks x 0.571 = 92, `oc` = 2, a 23 tick cycle at 480 EU/t per run.
- **Simulate, then commit.** A cycle is planned at its start from counts and a simulated store into the bench Output Bus (GT's `storePartial` with its simulate flag). The items move at the end of the cycle, once: the run count is worked out again against the buses as they are then, the output fit is checked again, and only then are empties and rounds removed and loaded magazines stored. A full or missing Output Bus therefore stops the bench with every input untouched, and nothing is duplicated or lost. A store that cannot give what it counted gets everything put back.
- **Fairness.** One magazine kind runs per cycle, starting from a rotating cursor, so every kind gets its turn. An idle bench looks again every 20 ticks.
- **ME note.** The bench takes only rounds that sit in a plain station Output Bus. An ME Output Bus pushes straight to the network, so there is nothing in it to pull; with only ME Output Buses on the station the bench has no source and idles. Keep one plain Output Bus on the back wall if the bench is built.
- **Not saved.** A cycle in progress is not written to disk: nothing leaves any bus until a cycle ends, so a restart loses at most the progress of one cycle.

**Hologram and survival build.** By default the projector and the survival tool build the Works without the bench. To include it, set the projector's **height** channel (StructureLib's `height` sub-channel, which the Works has no other use for) to 1 or more. The NEI structure preview cannot show an optional piece and stays without it; the bench is documented in the controller tooltip. `/gregguns build <x> <y> <z> [count] [bench]` places the bench and its two buses.

### Hatches, controller, special rules

| Rule | Value |
|---|---|
| Controller | Front, middle, second layer from the bottom of the feeder (`~`) |
| Input Bus | At least 1, on any `F`. Hopper tops (`F` on layer y5) are the intended spots |
| Output Bus | At least 1, on the back wall `O` |
| Energy, Maintenance, Muffler | One of each on any `F` (Muffler required) |
| Input Hatch | 0 or more, any `F` (some recipes need a fluid) |
| Bench Input Bus, Bench Output Bus (optional bench) | At least 1 of each on any bench `K`, or the bench is not active. See [Filling bench](#filling-bench-optional) |
| Belt | One tier for the whole `I` line. Mixed tiers fail the check, as in the Mixer ([chainItemPipeCasings](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L136)) |

### Tier and parallel mechanic

- **Parallels** = `4 x n x tier`, where `tier` is `GTUtility.getTier(getMaxInputVoltage())`, the same call every GT industrial multiblock uses. At HV with 4 bays that is 48 and at HV with 8 bays it is 96. For comparison the Multi Canner gives `8 x tier` and the Large Scale Auto-Assembler `2 x tier` ([roster](machine-roster.md#the-gt-coverage-check)). The cap is high on purpose: the hall is the bottleneck machine.
- **Power is the real limit.** `ParallelHelper` never starts more parallels than the Energy Hatches can power ([ParallelHelper#L398-L402](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/ParallelHelper.java#L398-L402)). An LV bullet run is 30 EU/t, so one HV hatch (480 EU/t) feeds 16 of them. Big parallel counts therefore need several amps or a higher hatch. This makes a big hall a real investment.
- **Belt speed.** Duration multiplier `1 / (1 + 0.25 x (beltTier + 1))`, a gentler form of the Mixer's `1 / (1 + (itemPipeTier + 1))` ([L225](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialMixer.java#L225)). Belt tier 2 gives 0.571. Proposal; tune in M4.
- **Worked example (proposal).** HV hall, 8 bays, belt tier 2, enough hatches: 16 LV rifle-bullet runs at once, 16 bullets each, in `6 s x 0.571` = 3.4 s. That is 256 bullets per 3.4 s, about 4,500 a minute, against 160 a minute from one LV Ammo Press. Two orders of magnitude at 480 EU/t.

### Single block versus multiblock

| | Ammo Press (single) | Ammunition Works |
|---|---|---|
| Recipes | `gregguns.ammoPress` | Same map, same recipes |
| Parallels | 1 | `4 x n x tier` |
| EU/t | Recipe `eut` | Parallels x recipe `eut` |
| Speed | Tier overclock | Tier overclock x belt multiplier |
| Needs | One MTE | Hatches, maintenance, a muffler, about 290 casings plus glass and interior parts |

### Special mechanics

- **ME and output-bus friendliness.** The structure uses GT's own `InputBus` and `OutputBus` hatch elements, which accept subclasses ([HatchElement#L74](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/HatchElement.java#L74), [#L98](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/HatchElement.java#L98)). GT's ME Input Bus and ME Output Bus extend them ([MTEHatchInputBusME#L91](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/MTEHatchInputBusME.java#L91), [MTEHatchOutputBusME#L77](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/outputme/MTEHatchOutputBusME.java#L77)). So an AE2 network can feed the hall and take the ammo with no special code. Hopper tops are `F` so ME buses sit where the lines arrive. The Works never deletes its output: a full Output Bus stops the machine with GT's output-full result. Optional voiding is a screwdriver mode, off by default.
- **Batch mode.** Support GT's batch mode if the base class provides it, so very short recipes (2 s magazines) do not waste ticks.
- **Muffler and pollution.** Pollutes while running (proposal 40 per second), so a Muffler Hatch is required ([metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-pollution)).
- **Animation.** The Works draws, via `IMTERenderer` on the controller: ram pistons through the press bays, boxed rounds scrolling along the belt at the belt tier's rate, and a crane lowering a crate at the station. The `Ram Housing` casing has an animated active texture. A screwdriver toggle turns drawing off, as the Cutting Machine does. See [ammo-press.md § Animation](ammo-press.md#animation) for the mechanism and its citations. The bounding box is the whole structure, so `getRenderBoundingBox` is overridden as the Cutting Machine does.
- **StructureLib build.** Pieces `feeder`, `bay`, `station`. `checkMachine` checks `feeder` at offset 0, loops `bay` while it matches (2 to 8), then checks `station`, as the Distillation Tower and Neutron Activator do. The hologram stack size is `n`. Survival build places feeder, bays one by one within the element budget, then station.

### Requirements

- **GGS-08-57** The Ammunition Works is one MTE, ID 20105, built from pieces `feeder`, one to eight `bay` and `station`, with the shape above. A structure with fewer than 2 or more than 8 bays fails with a named error.
- **GGS-08-58** It reads `gregguns.ammoPress`. Every Ammo Press recipe runs in it.
- **GGS-08-59** Parallels are `4 x n x tier`, capped by the power the Energy Hatches supply.
- **GGS-08-60** The belt is one tier of GT item pipe casing. Its tier sets the duration multiplier. Mixed tiers fail the check.
- **GGS-08-61** It accepts GT's ME Input Bus and ME Output Bus and any normal bus. A full Output Bus pauses it without voiding.
- **GGS-08-62** It requires an Energy Hatch, a Maintenance Hatch, a Muffler Hatch, an Input Bus and an Output Bus.
- **GGS-08-63** Its animation is client only and has a screwdriver toggle.
- **GGS-08-90** The Ammunition Works has an optional `bench` piece (36 `K` Works Casing, 6 `T` tables, 3 air) built outside the station's back wall. Without it, or with it half built, the Works forms and runs exactly as without a bench; the bench is just not active.
- **GGS-08-91** The bench is active only when its piece matches and it has at least one Bench Input Bus and one Bench Output Bus. Those buses are separate from the hall's buses and use the controller's Energy Hatches. The controller tooltip and the scanner say whether the bench is active.
- **GGS-08-92** The table `T` is Natura's wooden Workbench (any wood), the vanilla Crafting Table or an Avaritia Dire Crafting Table, matched by registry name and meta; a Works Casing is accepted instead only when none of these is registered.
- **GGS-08-93** The bench's fill recipes are the existing loaded-magazine rows of `gregguns.ammoPress` and no others; it registers no recipes. A run is the row's empties plus rounds into its loaded magazines.
- **GGS-08-94** Runs per cycle are capped by the Works' parallels, the inputs on hand, the power the Energy Hatches supply and the room in the Bench Output Bus. Time and EU/t are the row's, with the belt multiplier and the Works' overclock rule.
- **GGS-08-95** A fill cycle plans by simulation and moves items once, at its end, after re-checking the output fit. It never loses or duplicates an item, and a full Bench Output Bus stops it with its inputs untouched.
- **GGS-08-96** The bench takes rounds only from plain Output Buses of the station; an ME Output Bus there supplies none. It never blocks or delays the normal recipe.
- **GGS-08-97** The projector and the survival build leave the bench out unless the `height` channel of the trigger stack is 1 or more.

## Retort (retired)

The Retort, the Reaction Chamber's multiblock form, was **removed** by design record [0012](../../design/0012-retire-reaction-chamber-and-retort.md). Its shape, bulb-layer pieces, hatch rules, reaction state machine, risks and textures are not described here any more, and MTE ID 20108 is reserved forever.

The owner loves the concept, but a bulk-reaction multiblock for two recipes does not fit this mod. The whole design is archived, interior parts included, on branch `archive/retort-multiblock` and tag `archive/retort-v1` (cut from `fa818412`), and may be pitched to the GTNH community as a standalone multiblock. Read it there. The Laser Focus is now made on GT's Laser Engraver and the Anti Gravity Core on GT's Assembler.

## Armory Gantry

**Concept.** A repair **gantry**: a row of portal frames on rails, each frame a repair bay with a cradle on its floor and a hoist on its beam. Armor and guns go in on an Input Bus, repair materials alongside, and repaired items come out on an Output Bus, damage zero and NBT intact.

**Silhouette.** A line of open portal arches under a shared beam, hoists standing above the beam, and a closed booth at the front. Seen from the side it is a row of arches. Never a cube.

**Dimensions.** 7 wide x 7 tall x (3 + 2n) long, where `n` is the number of bays, 2 to 8. Default `n` = 4 is 7 x 7 x 11.

**Class and tiers.** Regular. Controller HV. Energy Hatches LV to ZPM. The **lowest Energy Hatch tier is the highest item tier it will repair**, so a LuV or ZPM item needs a LuV or ZPM hatch even though the controller is HV.

### Legend

| Char | Block | Hatches allowed |
|---|---|---|
| `R` | Gantry Casing | Input Bus, Output Bus, Energy, Maintenance (on head and tail walls and floors) |
| `B` | Gantry Beam | none |
| `H` | Gantry Hoist | none |
| `L` | `frameGt` Titanium (legs) | none |
| `G` | GT borosilicate glass, any tier | none |
| `T` | Repair Cradle | none. One per floor pad |
| `F` | Steel Frame Box (floor rails, the hoist hook) | none |
| `X` | Steel Gear Box Casing (the hoist drive) | none |
| `N` | Motor Machine Casing (hoist motors) | none |
| `P` | Steel Pipe Casing (hydraulic runs under the beam) | none |
| `S` | Interior lamp: any block that gives light (glowstone, a lit redstone lamp, any mod lamp) or an unlit redstone lamp; the hologram shows glowstone | none |
| `~` | Controller | n/a |

### Pieces and layers

**Head** (2 long, the booth; the controller is in the front wall at middle height):

```
y6        y5        y4        y3        y2        y1        y0
...H...   BBBBBBB   LGGGGGL   LGGGGGL   LRR~RRL   LRRRRRL   LRRRRRL
.......   BBBBBBB   LP-S-PL   L-----L   L-----L   LF---FL   LRRRRRL
```

**Bay** (2 long, repeated `n` times; the front row is the portal frame, the back row is the open gap):

```
y6        y5        y4        y3        y2        y1        y0
...H...   BBBBBBB   L-NXN-L   L--F--L   L-----L   LF---FL   LRTTTRL
.......   BBBBBBB   -P-S-P-   -------   -------   -F---F-   RRTTTRR
```

The hoist is real: under each portal beam a steel gearbox (`X`) sits between two motor casings (`N`) with a frame-box hook (`F`) hanging from it, below the Gantry Hoist on the roof. The floor rails (`F`) run along both sides of the whole row, from the booth through every bay, and a steel pipe run (`P`) hangs under the beam in the gap aisle. A lamp (`S`) hangs under the beam in the booth and in the gap aisle of every bay, so the interior is lit behind the glass.

**Tail** (1 long):

```
y6        y5        y4        y3        y2        y1        y0
.......   BBBBBBB   LRRRRRL   LRRRRRL   LRRRRRL   LRRRRRL   LRRRRRL
```

**Block counts.** Head `B` 14, `L` 20, `G` 10, `R` 19, `H` 1, `P` 2, `F` 2, `S` 1. Bay `B` 14, `L` 10, `R` 6, `T` 6, `H` 1, `X` 1, `N` 2, `F` 5, `P` 2, `S` 1. Tail `B` 7, `L` 10, `R` 25. The interior parts (`F`, `X`, `N`, `P`, `S`) are required, are not Gantry Casings, and never sit on a hatch face. `G` is any glass tier, mixed is fine.

### Hatches and tier mechanic

- Input Bus at least 1, Output Bus at least 1, Energy at least 1, Maintenance 1, all on `R`. No muffler and no pollution: repairing makes none.
- **Parallels** = `n` bays. Each bay repairs one item per cycle, so a cycle repairs up to `n` items. The Input Bus is scanned in order. A 2-bay Gantry already beats the single bench; an 8-bay one clears a locker in one cycle.
- **Tier gate.** Item tier is its [0001](../../design/0001-tier-map.md) tier. An item above the lowest Energy Hatch tier is skipped, and the controller says so. Same rule as the single Repair Bench (bench tier at least item tier).

### Process: repair of NBT items in bulk

There is **no recipe map**: damage is item NBT, so the cost depends on the item. Like the Assembly Line, `checkProcessing` is overridden and reads item data ([L224](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEAssemblyLine.java#L224)). Each cycle:

1. Collect up to `n` damaged items from the Input Bus, in order, that pass the tier gate. GT tools are never touched (their damage is in GT's own NBT; [repair-bench.md](../09-machines-utility/repair-bench.md)).
2. For each, ask the item for its repair list (area 05 owns the list; the formula is Techguns' `damage fraction x base count`).
3. Match the list against the Input Bus stacks with `GTOreDictUnificator.isInputStackEqual` ([GGS-09-21](../09-machines-utility/repair-bench.md#requirements)). Take the stacks only if **every** entry is present, all or nothing **per item**. An item that lacks materials is skipped and the others still repair.
4. Duration and power: the cycle lasts `2 s + 8 s x (the largest damage fraction in the batch)`, and draws `RECIPE_<tier>/2 x (items repaired)`. These are the same proposals as the single bench.
5. At the end, each repaired item goes to the Output Bus with damage 0. **Every other NBT key is copied through** unchanged: enchantments, the camo index, an IC2 charge and Infernal-style tags. A repair never resets charge or camo.

There is no NEI recipe tab. NEI shows the controller's tooltip and the multiblock structure. An info page for "repair materials per item" is an open question.

### Single block versus multiblock

| | Repair Bench (single) | Armory Gantry |
|---|---|---|
| Items | Worn armor (by player) and held guns | Any damaged armor or gun in the Input Bus |
| Batch | One item per click | Up to `n` per cycle |
| Tier | Bench tier at least item tier | Lowest Energy Hatch tier at least item tier |
| Interaction | GUI, click | Bus automation, ME-friendly |

### Requirements

- **GGS-08-69** The Armory Gantry is one MTE, ID 20114, built from `head`, two to eight `bay` and `tail`, with the shape above.
- **GGS-08-70** It repairs up to `n` items per cycle, each all or nothing against its own repair list, matched with `GTOreDictUnificator.isInputStackEqual`.
- **GGS-08-71** Repaired items come out with damage 0 and every other NBT key unchanged.
- **GGS-08-72** An item above the lowest Energy Hatch tier is skipped and reported, never repaired.
- **GGS-08-73** GT tools are never repaired.

### Armory Gantry built notes (M4 wave A)

Built as `MTEArmoryGantry` (ID 20114) with pure data in `GantryShape`, the repair logic in `RepairPlanner` and the casings as metas 3 to 6 of the shared `gregguns:casings` block (Gantry Casing, Beam, Hoist, Repair Cradle). Decisions, each marked `// Deviation:` in code:

- **Shape characters.** As for the Ammunition Works, the sheet's `.` is a space.
- **Items per cycle** is the bay count capped by the power the hatches supply: each item draws half the recipe voltage of the lowest Energy Hatch tier, and GT shuts a machine down that over-draws. One HV hatch therefore repairs two items per cycle; more hatches (or multi-amp hatches) raise it. The sheet says "up to n" without a cap.
- **Tier gate.** The weakest Energy Hatch. When every damaged item in the buses is above it, the controller shows GT's insufficient-machine-tier message with the tier needed.
- **No NEI tab and no NEI code** ([GGS-08-31](nei-handlers.md#requirements)). The controller tooltip states the rules. The open "Gantry NEI" question stays open.
- **Glass.** Any glass tier, mixed. The getter passed to `chainAllGlasses` returned the stored tier, which forced one tier for the whole booth; it now always answers "not set",. The Borosilicate Glass channel is on the tooltip.
- **Interior.** Added on the owner's request (see inside, more character). Fixed GT blocks: `gt.blockcasings2` metas 3 (steel gearbox), 11 (motor) and 13 (steel pipe), and `ofFrame(Materials.Steel)`; the GregGuns accent is the existing Gantry Beam, Hoist and Repair Cradle.
- **Not built:** the cradle's active texture, the hoist animation, a screwdriver mode.

## Rampart

**Concept.** A **crenellated wall** of turret mounts. A gatehouse tower on the left holds the controller, the ammo feed and the power feed. A run of bays, each a stone-style merlon and an open crenel with a **Gun Mount Hatch** on its sill, extends to the right. A turret stands in each crenel. Plating tier is armor tier.

**Silhouette.** Alternating tall merlons and low crenels along the wall, a taller gatehouse at one end and a one-block end cap at the other: a battlement. Never a cube.

**Dimensions.** (4 + 2n) long x 6 tall (the gatehouse; the wall is 5 tall) x 3 deep, where `n` is the number of mounts, 2 to 10. Default `n` = 6 is 16 x 6 x 3.

**Class and tiers.** Regular. Controller HV. Each mount is a Gun Mount Hatch (MV, HV, EV, IV). A turret mount fires guns up to the hatch's tier, as the single Turret does ([roster](machine-roster.md#roster)).

### Legend

| Char | Block | Hatches allowed |
|---|---|---|
| `T` | Bastion Plating, gatehouse | Input Bus, Output Bus, Energy, Maintenance |
| `P` | Bastion Plating, wall | none |
| `G` | GT borosilicate glass, any tier | none |
| `H` | **Gun Mount Hatch** (exactly one per bay) | n/a |
| `~` | Controller | n/a |

### Pieces and layers

**Gatehouse** (3 long, 3 deep, 6 tall; the controller is at the front, middle, second layer):

```
y5     y4     y3     y2     y1     y0
TTT    TGT    TGT    TTT    T~T    TTT
TTT    T-T    T-T    T-T    T-T    TTT
TTT    TTT    TTT    TTT    TTT    TTT
```

**Bay** (2 long, repeated `n` times; the mount is on the sill, front row):

```
y4    y3    y2    y1    y0
P-    P-    PH    PP    PP
P-    P-    P-    PP    PP
P-    P-    P-    PP    PP
```

**End cap** (1 long):

```
y4  y3  y2  y1  y0
P   P   P   P   P
P   P   P   P   P
P   P   P   P   P
```

**Block counts.** Gatehouse `T` 47, `G` 2. Bay `P` 21, `H` 1. Cap `P` 15. Each bay's merlon column is `P` for all five layers, its crenel is open above the sill, and the hatch sits in the sill at the front.

### Hatches, controller, mounts

- **Gun Mount Hatch.** A new hatch kind, `MTEHatch` subclass ([MTEHatch#L31](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEHatch.java#L31)), tiers MV to IV, IDs 20121-20124. It holds **one gun**. A custom `IHatchElement` lets the structure accept it only at `H`, the way the Extruder adds `ExtruderHatchElement` ([L231](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialExtruder.java#L231)). This is the one GregGuns hatch, so [GGS-08-44](recipes.md#requirements) (no GregGuns hatch in the **Reaction Chamber** structure) is unaffected.
- **Ammo feed.** The controller's Input Buses are one shared ammo store for every mount. On a mount's reload, the Rampart takes ammo from the buses exactly as Techguns' base takes it from slots 0-8 ([turret.md](../../techguns-spec/09-machines-utility/turret.md#ammunition-consumeammo-called-by-the-turret-for-each-shot)), and puts the empty magazines on the Output Bus. One Input Bus replaces up to ten hand-fed inventories. An ME Input Bus works.
- **Mounted entity.** Each hatch spawns the existing 30 HP `NPCTurret` above itself, owned by the controller's owner, with the friend rules of [turret.md](../09-machines-utility/turret.md). Breaking a hatch, or the structure, kills that turret. The respawn timer and healing are per mount.
- **One security setting.** Owner, team status (ServerUtilities, ALLY or higher) and the "target animals" switch are set once on the controller and apply to every mount.
- **Turret armor from casing.** The lowest Bastion Plating meta found gives every mount the same armor value: Iron 5, Steel 10, Aluminum 15, Carbon 20, the Techguns values ([turret.md](../../techguns-spec/09-machines-utility/turret.md#armor)). This replaces the per-turret armor slot. Each plating block is crafted from its matching turret armor item ([09 recipes](../09-machines-utility/recipes.md#turret-armor-plates-4-recipes)). The meta is read the way GT reads coil and glass tier ([`ofCoil`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java#L608)).
- **No recipe.** Like the Cleanroom ([L213-L233](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTECleanroom.java#L213-L233)) the Rampart sets its own EU/t in `checkProcessing` and runs in rolling cycles. Draw is the sum of mount upkeep, 4 EU/t each (proposal, from Techguns' 5 RF/t), plus 120 EU per HP healed. If the hatch cannot pay, the mounts go inactive, as the single turret does.

### Parallel and EU

The "parallel" is `n` mounts. EU is the same per mount as a single Turret: there is no energy discount. The gain is logistics (one feed, one setting) and the shared armor. A 10-mount Rampart upkeep is 40 EU/t, inside an MV Energy Hatch.

### Single block versus multiblock

| | Turret (single, MV to IV) | Rampart |
|---|---|---|
| Guns | One, in its own slot 18 | One per Gun Mount Hatch, up to ten |
| Ammo | 9 slots per turret, hand or automation | Shared Input Bus (ME ok) |
| Armor | One slot, Techguns plates | Plating tier, all mounts |
| Security | Per turret | Per Rampart |
| Power | Per turret hatch | One Energy Hatch |

### Muffler, maintenance, pollution

No muffler, no pollution (it makes none). Maintenance required, as every GT multiblock in this pack. `willExplodeInRain()` returns `false` on the controller and on the Gun Mount Hatches, because the Rampart stands outside ([metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-world-rules-every-mte-inherits)); the single Turret does the same.

### StructureLib build

Pieces `gate`, `bay` and `cap`. The check places `gate`, loops `bay` while it matches (2 to 10), then `cap`, exactly as the Neutron Activator loops `mid` ([L279-L297](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTENeutronActivator.java#L279-L297)). The hologram stack size is `n`. Survival build places the gatehouse, the bays in order, then the cap. Gun Mount Hatches must be in the player's inventory (the hologram shows a hatch outline). Mounts are not placed by the survival builder with a gun in them.

### Requirements

- **GGS-08-74** The Rampart is one MTE, ID 20120, built from `gate`, two to ten `bay` and `cap`, with the shape above.
- **GGS-08-75** There is exactly one Gun Mount Hatch per bay, at `H`. A missing one is a structure error.
- **GGS-08-76** The Gun Mount Hatch is an MTE for each of MV, HV, EV and IV (IDs 20121-20124) and holds one gun. A gun above the hatch's tier is refused.
- **GGS-08-77** One shared ammo store: reloads draw from the controller's Input Buses and return empty magazines to its Output Bus.
- **GGS-08-78** Every mount's armor value is that of the lowest Bastion Plating meta in the structure.
- **GGS-08-79** Owner, team and animal-target settings are on the controller and apply to every mount.
- **GGS-08-80** Breaking a mount hatch or the structure kills that mount's turret entity.
- **GGS-08-81** The Rampart draws EU only (upkeep per mount plus healing) and never explodes in rain.

## Shared requirements

- **GGS-08-82** None of the four silhouettes has a bounding box of 3 x 3 x 3 or a plain cuboid of casings. A unit test checks that each assembled shape is non-convex (it has at least one empty cell inside its bounding box on the outer layer).
- **GGS-08-83** Every multiblock implements `ISurvivalConstructable`. The hologram stack size selects `n` for the variable pieces.
- **GGS-08-84** Every multiblock texture comes from page 50 ([GGS-08-04](metatileentity-framework.md#requirements)), apart from GT's own blocks named above.

## Tests

| Req | Verification |
|---|---|
| 57, 64, 69, 74 | `runServer` with a test world: build each at its minimum, default and maximum size with the hologram projector; add or remove one segment and check the error text |
| 58 | Unit test: an Ammo Press recipe runs in the Works |
| 59, 65 | Unit test of the parallel formula for each size and tier, including the power cap |
| 60 | Unit test of the speed multiplier for belt tiers 0 to 7; `/gregguns test` with a mixed-tier belt fails |
| 61 | `/gregguns test` with an ME Output Bus and a full normal bus |
| 62, 68 | Remove each required hatch in turn; check the error text |
| 63, 81 | Manual client check of the animation and its toggle; dedicated server boot with no client class loaded |
| 66 | Unit test: a recipe at tier EV with IV and EV glass |
| 70, 71 | Unit test over the repair routine: all or nothing per item; NBT other than damage is identical before and after, including charge and camo |
| 72, 73 | Unit test: tier gate and tool rejection |
| 75-77 | `/gregguns test`: remove a hatch; shoot until reload; check the ammo and the empty magazines |
| 78-80 | Unit test of the plating-tier read; manual team-status check |
| 82 | Unit test over the shape strings |
| 83, 84 | Code review and the texture-page unit test |

## Ammunition Works built notes

Built as `MTEAmmoWorks` (ID 20105) with pure data in `AmmoWorksShape` and `AmmoWorksRecipeSpec`, the casing block `GregGunsCasings` (`gregguns:casings`, metas Works Casing and Ram Housing, texture page 50). Deviations from the sheet, all marked `// Deviation:` in code:

- **Shape characters.** StructureLib 1.4.45 reserves space (skip), `-` (air) and `~`; `.` is not a wildcard, so the sheet's `.` is a space. Feeder and bay layers get an empty top row to share the station's seven rows.
- **Bay loop.** A failed bay probe is checked into a scratch error list and its casing count is rolled back; the station check then reports a real mismatch. Fewer than 2 bays gives the named error `gregguns.machine.ammo_works.error.too_few_bays`; a ninth bay fails the station check.
- **Belt tier** is GT's item pipe casing value (1 to 8, as the Mixer numbers it), so "belt tier 2" is the second pipe casing. Mixed tiers fail inside GT's tiered element.
- **No energy hatch tier limit.** The sheet says LV to IV; parallels are already power-limited, so higher hatches are allowed, as are multi-amp hatches.
- **Animation not built.** No ram, belt-round or crane drawing and no screwdriver toggle; the screwdriver toggles input separation. The Ram Housing active texture is not wired (GT casings have no active state), and casings render as plain cubes without GT's machine-metal tint.
- **Windows and interior.** Added on the owner's request (see-inside and more character). Glass uses `GTStructureUtility.chainAllGlasses()` ([L891-L925](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java#L891-L925)) on the Borosilicate Glass channel, so the projector's glass channel picks the tier it previews. The interior is fixed GT blocks (`gt.blockcasings2` metas 3, 9, 11, 13 and `ofFrame(Materials.Steel)`); the Ram Housing is the GregGuns accent. No active texture while running (the overlay system has no per-casing state).
- **Interior lamps.** The interiors were dark behind the glass, so every feeder, bay and station (Ammunition Works) and the booth and every bay (Armory Gantry) has one required lamp position `S` under the ceiling (`InteriorLamp`, shared by both). It accepts any block whose light value is 12 or more (glowstone, a lit redstone lamp, any pack mod's lamp) and the unlit redstone lamp. The pack ships no GT lamp block (GT++'s redstone lamp is a machine tile entity), so there is no GT element to chain; the light-value test stays tier-free and lets other mods' lamps through. The hologram and creative build show glowstone, so `/gregguns build` needs no change. The projector has no lamp channel in GT 5.09.54.205 (the channels are length, item pipe casing and borosilicate glass), so the lamp is listed as a structure line in the controller tooltip instead. Chosen over a casing that glows while active because a lit casing variant would be a new block and texture per casing and could not light an idle hall.
- **Hologram** takes the bay count from the length channel if present, else the stack size (2 to 8).
- **Voiding** uses GT's void protection mode (item modes only), on by default; a full Output Bus pauses the hall.
- **Filling bench** (GGS-08-90 to GGS-08-97). `AmmoWorksShape.BENCH` and `benchDepth`, the pure fill logic in `AmmoWorksFill` (recipes read from the ammo definitions, plan and commit over three item stores, overclock and cycle time), the table matcher `BenchTable` and the controller glue in `MTEAmmoWorks` (`checkBench`, `fillTick`, the two bench hatch elements, the scanner line). The bench rides on the trigger stack's `height` channel because the pack's channel list has no generic toggle and a second stack size does not exist; the channel is otherwise unused by the Works. No NEI preview of the optional piece. Decisions: one magazine kind runs per cycle, the cycle pays per tick and moves items at its end, an ME Input Bus is not accepted as a bench input.

## Open questions

- **Glass tier numbering.** Does `BorosilicateGlass` tier map to GT voltage indices, and is there glass up to IV? Withdrawn with the Retort (0012); it only mattered for the Retort rating.
- **Parallel and belt numbers.** `4 x n x tier` and the 0.25 belt step are proposals. A balance pass fixes them.
- **Gantry NEI.** A page showing repair materials per item, or none.
- **Gun Mount Hatch contents.** Does the hatch also hold a camo-changeable gun's camo? It is gun NBT, so yes by default.
- **Rampart range.** Gun range is a property of the gun and its AI ([10](../../techguns-spec/10-entities-npcs/index.md)); a wall mount may need a higher view check.
- **ID block.** All IDs depend on the GTNH team's agreement ([index.md](index.md#open-questions)).
