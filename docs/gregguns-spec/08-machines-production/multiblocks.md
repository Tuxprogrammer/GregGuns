# GregGuns multiblocks

The four multiblocks of the [machine roster](machine-roster.md): the Ammunition Works, the Retort, the Armory Gantry and the Rampart. Each has a distinctive silhouette, a length or size that players choose, and a tier mechanic that is not just "more hatches".

- **Techguns baseline:** [reaction-chamber.md](../../techguns-spec/08-machines-production/reaction-chamber.md), [ammo-press.md](../../techguns-spec/08-machines-production/ammo-press.md), [repair-bench.md](../../techguns-spec/09-machines-utility/repair-bench.md), [turret.md](../../techguns-spec/09-machines-utility/turret.md), [framework.md § multiblock](../../techguns-spec/08-machines-production/framework.md#9-multiblock-framework)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (amended: see [machine-roster.md](machine-roster.md#what-changes-from-techguns-and-from-0002)), [0003 energy model](../../design/0003-energy-model.md), [0006 art direction](../../design/0006-art-direction.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205), StructureLib (`structurelib`, 1.4.45), ModularUI2 (`modularui2`, 2.3.92-1.7.10)
- **Roadmap:** M4 (Ammunition Works, Retort), M6 (Gantry, Rampart)

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| One multiblock, the Reaction Chamber, a hidden-block 3 x 4 x 3 | Four multiblocks, each a real StructureLib structure with visible casings and none a cube | Owner policy: a multiblock for every machine used regularly, with interesting shapes |
| Ammo Press, Repair Bench and Turret were single blocks with no scaling | Each keeps its single block and gains a multiblock for scale | Same |
| Fixed size | Three of the four grow or shrink by repeating a segment; the Retort changes height | Players size the machine to their need, as GT's Distillation Tower and Neutron Activator do |

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

## Casings used by the four multiblocks

All of these are GregGuns blocks on page 50 unless a GT name is given.

| Casing | Where | Notes |
|---|---|---|
| **Works Casing** | Ammunition Works hull | Hatches allowed on its `F` and `O` positions only |
| **Ram Housing** | Ammunition Works press columns | Active texture is animated (frame strip) |
| **Item pipe casing** (GT, any tier) | Ammunition Works belt | Tier sets speed, as in the Industrial Mixer |
| **Reaction Casing** (the Reaction Chamber Casing of [recipes.md](recipes.md#reaction-chamber-casing-1-recipe)) | Retort | Reused, not renamed |
| **Borosilicate glass** (GT, any tier, read with `chainAllGlasses`) | Retort vessel, Gantry windows | Retort glass tier is the vessel's pressure rating |
| **Gantry Casing**, **Gantry Beam**, **Gantry Hoist**, **Repair Cradle** | Armory Gantry | Cradle is one per bay floor pad |
| **Bastion Plating** | Rampart, four metas: Iron, Steel, Aluminum, Carbon | The meta sets the armor value of every mount |
| `frameGt` Steel / Titanium | Retort feet, Gantry legs | GT frame boxes |

## Ammunition Works

**Concept.** A production line laid out along its length: a **feeder** with hopper towers, a row of **press bays** each topped by a ram housing that stands above the roof like a saw-tooth, and a taller **packing station** with a crane at the far end. Players lengthen the hall by adding press bays. Ammunition in, boxed ammunition out.

**Silhouette.** Seen from the side: a low feeder with two hoppers, then a run of rooftop teeth (one per bay), then a tall stepped end building. Seen from above: a 5-wide strip, 13 to 31 long. Never a cube.

**Dimensions.** 5 wide x 7 tall x (7 + 3n) long, where `n` is the number of press bays, 2 to 8. Default `n` = 4 is 5 x 7 x 19.

**Class and tiers.** Bottleneck. Controller tier HV (hull and parts at HV). Energy Hatches LV to IV. It runs the same map as the Ammo Press, `gregguns.ammoPress` ([ammo-press.md](ammo-press.md#the-recipe-map)).

### Legend

| Char | Block | Hatches allowed |
|---|---|---|
| `F` | Works Casing, feeder | Input Bus, Input Hatch, Energy Hatch, Maintenance Hatch, Muffler Hatch |
| `A` | Works Casing, plain | none |
| `O` | Works Casing, packing | Output Bus, Output Hatch, Energy Hatch |
| `M` | Ram Housing | none |
| `I` | GT item pipe casing, any tier | none. Tier is read |
| `~` | Controller | n/a |

### Pieces and layers

**Feeder** (3 long; the controller is at the front, middle, second layer from the bottom):

```
y5 (top)      y4          y3          y2          y1          y0 (bottom)
F...F         AAAAA       FFFFF       FFFFF       FF~FF       AAAAA
.....         AAAAA       A---A       A---A       A---A       AAIAA
F...F         AAAAA       A---A       A---A       A---A       AAIAA
```

(Each column is one layer. Within a column, rows run front to back.)

**Press bay** (3 long, repeated `n` times):

```
y5            y4          y3          y2          y1          y0
.....         AAAAA       A---A       A---A       A---A       AAIAA
..M..         AAMAA       A---A       A---A       A---A       AAIAA
.....         AAAAA       A---A       A---A       A---A       AAIAA
```

**Packing station** (4 long, once, at the far end):

```
y6      y5      y4      y3      y2      y1      y0
.....   AAAAA   A---A   A---A   A---A   A---A   AAIAA
..O..   AAOAA   A---A   A---A   A---A   A---A   AAIAA
..O..   AAOAA   A---A   A---A   A---A   A---A   AAIAA
.....   AAAAA   AOOOA   AOOOA   AOOOA   AOOOA   AAAAA
```

The belt `I` runs down the middle of the floor, feeder to station. The hall between the walls is a 3-wide, 3-high open tunnel (4 high in the station), which is where the animated rounds travel.

**Block counts** (for `checkCasingMin`): feeder 18 `F`, 40 `A`, 2 `I`; each bay 44 `A`, 2 `M`, 3 `I`; station 16 `O`, 67 `A`, 3 `I`. The casing minimum is the sum of `F`, `A`, `O` and `M` positions minus a fixed allowance of 12 hatches. For `n` = 4 that is 18 + 40 + 4 x 46 + 16 + 67 = 325 casing positions, so the check is 313.

### Hatches, controller, special rules

| Rule | Value |
|---|---|
| Controller | Front, middle, second layer from the bottom of the feeder (`~`) |
| Input Bus | At least 1, on any `F`. Hopper tops (`F` on layer y5) are the intended spots |
| Output Bus | At least 1, on the back wall `O` |
| Energy, Maintenance, Muffler | One of each on any `F` (Muffler required) |
| Input Hatch | 0 or more, any `F` (some recipes need a fluid) |
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
| Needs | One MTE | Hatches, maintenance, a muffler, about 300 casings |

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

## Retort

**Concept.** The Reaction Chamber's multiblock form: a lab **retort** (a round-bottom flask) of glass on a three-legged stand, with a narrow neck and a stopper. You raise the flask by adding bulb layers. Glass tier is the vessel's pressure rating.

**Silhouette.** A bulb and neck: a 3-wide neck and cap over a 5-wide rounded body (corners cut), on four stilt feet. Never a cube.

**Dimensions.** 5 wide x (7 + n) tall x 5 deep, where `n` is the number of bulb layers, 2 to 6. Default `n` = 3 is 5 x 10 x 5. The old 3 x 4 x 3 chamber ([reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#structure)) is superseded.

**Class and tiers.** Bottleneck. Controller EV. Energy Hatches EV to ZPM. Reads `gregguns.recipe.reactionchamber`.

### Legend

| Char | Block | Hatches allowed |
|---|---|---|
| `R` | Reaction Casing | Input Bus, Output Bus, Input Hatch, Energy, Maintenance on the **floor and low ring**; Muffler on the **cap** |
| `G` | GT borosilicate glass, any tier (`chainAllGlasses`) | none. **Lowest tier found** is the pressure rating |
| `F` | `frameGt` Steel | none |
| `P` | Steel Pipe Casing (the dip tube and the floor pipe runs) | none |
| `U` | Pump Machine Casing (the foot of the dip tube) | none |
| `H` | Heating coil, **any tier**, mixed is fine (`ofCoil` with a getter that never records a level) | none |
| `~` | Controller | n/a |

### Layers (top to bottom; `n` = 3 shown, the mid layer repeats `n` times)

```
cap       neck      neck      shoulder   mid (x n)  low ring   floor      feet
.....     .....     .....     .RRR.      .GGG.      .RRR.      .R~R.      .....
.RRR.     .GGG.     .GGG.     RRRRR      G---G      RHPHR      RRRRR      .F.F.
.RRR.     .G-G.     .G-G.     RR-RR      G-P-G      R-U-R      RRRRR      .....
.RRR.     .GGG.     .GGG.     RRRRR      G---G      RHPHR      RRRRR      .F.F.
.....     .....     .....     .RRR.      .GGG.      .RRR.      .RRR.      .....
```

The controller `~` is in the front, middle of the **floor** layer. The shoulder has a one-block hole in the middle, which joins the 3 x 3 x `n` bulb interior to the neck.

**Plumbing.** The inside of the flask is not empty: a pump casing (`U`) sits on the floor at the center, and a steel pipe column (`P`) rises from it through every bulb layer, a dip tube that stops under the shoulder hole so the neck stays an open air column. Two pipe runs on the floor join the pump to the four heating coils (`H`) in the low ring's corners. All of it is required, none of it is a Reaction Casing, so it is not counted toward the casing minimum and never sits where a hatch goes. The coils are tier-free: any coil tier, mixed, because the recipes do not gate on coils (the pressure rating is the glass). The hologram builds the cheapest coil whatever the bulb count.

**Block counts.** `R` 61 (cap 9, shoulder 20, low ring 12, floor 20), `G` = 16 + 12 x `n`, `F` 4, `H` 4, `U` 1, `P` = 2 + `n`. Casing minimum 51 (61 minus 10 for hatches); it does not change with `n`.

### Hatch rules

Input Bus (holds the non-consumed Heat Ray Focus and inputs), Output Bus, Input Hatch, Energy Hatch and Maintenance Hatch go on `R` in the floor and low ring. The Muffler Hatch goes on the cap. All hatches are GT's ([GGS-08-44](recipes.md#requirements)).

### Tier and parallel mechanic

- **Parallels** = `2 x n` (4 to 12). Power-limited like every parallel multiblock: a Laser Focus run is 1,920 EU/t, so six parallels need 11,520 EU/t, which means an IV hatch plus another, or multi-amp hatches.
- **Pressure rating.** A recipe has a voltage tier (the Laser Focus EV, the Anti Gravity Core IV). The vessel runs it only if the lowest glass tier is at least that, with `insufficientMachineTier` otherwise, the same shape as the Component Assembly Line's casing gate ([L294](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTEComponentAssemblyLine.java#L294)). Glass tier numbering is assumed to follow GT voltage indices; confirm against `BorosilicateGlass` in M4. The single block has no glass and no such gate.
- **Shared intensity.** The controller has one intensity setting. All parallel runs in a batch share it, so they succeed or fail together. A failed batch applies the risk **once**, not once per parallel run. The batch still loses all of its inputs. That is the gamble: a bigger batch is a bigger loss.
- **Early finish.** The batch finishes when `completion >= required` ([reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#integration-points)).

### Single block versus multiblock

| | Reaction Chamber (single, EV and IV) | Retort |
|---|---|---|
| Recipes | `gregguns.recipe.reactionchamber` | Same map |
| Parallels | 1 | `2 x n` |
| Glass | none | rating gate |
| Intensity, focus, risks | as [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#integration-points) | identical rules, applied to the batch |
| EU/t | recipe `eut` | parallels x `eut` |

### Special mechanics (carried from Techguns)

Focus (non-consumed input), intensity with drift, 3-second reaction ticks, completion count, risks (breakage, small explosion, radiation) are exactly the rules in [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md). One change: explosion strength is unchanged by parallel count (the risk is applied once), so a large Retort is not more explosive.

- **Muffler and pollution.** As the Reaction Chamber sheet: required Muffler Hatch, 100 per second proposal, a one-off burst on failure.
- **StructureLib build.** Pieces `top` (cap, neck, shoulder), `bulb` (repeated `n` times), `base` (low ring, floor, feet). The hologram stack size is `n`. Survival build places `base`, then bulbs, then `top`. This is a vertical version of the Neutron Activator's `bottom`/`mid`/`top`.

### Requirements

- **GGS-08-64** The Retort is one MTE, ID 20108, built from pieces `base`, two to six `bulb` and `top`, with the shape above.
- **GGS-08-65** Parallels are `2 x n`, capped by power. Every run in a batch shares the controller's intensity.
- **GGS-08-66** A recipe whose voltage tier exceeds the lowest glass tier does not start and names the missing glass tier.
- **GGS-08-67** A failed batch applies its risk once.
- **GGS-08-68** The Retort accepts only GT hatches. It requires an Energy, a Maintenance, a Muffler, an Input Bus, an Output Bus and an Input Hatch.

### Retort implementation notes (M4)

- **Pieces and offsets.** `base` (low ring, floor, feet; controller offset x 2, y 1), `bulb` (one layer, probed upward from y offset 2) and `top` (shoulder, two neck layers, cap; y offset `n + 5`). The shape strings are in `RetortShape`, with `B` (floor and low ring casing, takes the five non-muffler hatches), `S` (shoulder casing, no hatches) and `C` (cap casing, takes the Muffler Hatch); all three are the Reaction Casing. The spec's `.` is written as StructureLib's blank (space) at build time. The block counts in the sheet (61 casings, 16 + 12 n glass, 4 frames) are unit-tested.
- **Counting bulbs.** Like the Neutron Activator: `checkMachine` probes `bulb` upward (at most 6) with the error list, clears the errors, then checks the top. Fewer than 2 bulbs gives GT's `TOO_SHORT_HEIGHT`.
- **Glass tier.** The sheet says "lowest tier found", which GT's `ofBlocksTiered` does not allow (it demands one tier). `chainAllGlasses` is given a getter that always answers "not set" and a setter that keeps the minimum, so mixed tiers pass and the minimum is the pressure rating. The tier numbers are GT voltage indices (GT `GlassTier`), so a recipe `eut` maps to the smallest index whose voltage covers it (Laser Focus 4, Anti Gravity Core 5). A recipe above the rating fails with GT `insufficientMachineTier`.
- **Energy hatch tier.** The sheet says EV to ZPM. Anything below EV is a structure error (`gregguns.retort.error.energy_tier`); the upper bound is not enforced, as GT's hatch element would not either.
- **Run rules.** `ProcessingLogic` is anonymous: `validateRecipe` gates on intensity and glass tier, `createOverclockCalculator` sets no-overclock, `onRecipeStart` starts the shared `ReactionState`. `onRunningTick` steps the state after GT draws power; success sets `mProgresstime = mMaxProgresstime - 1`; failure calls `stopMachine` with the shutdown reason `gregguns_rc_failed` (outputs dropped), then `ReactionEffects.applyFailure` once. Parallels are `2 x n`, power-limited by `ParallelHelper`.
- **Pollution.** `machines.retortPollutionPerSecond` (default 100) through `getPollutionPerSecond`, plus a one-off `machines.reactionFailurePollution` (default 500) burst on failure.
- **Casing block.** The Reaction Chamber Casing is meta 2 of the shared `gregguns:casings` block (`GregGunsCasings`, formerly `BlockWorksCasing`), page-50 index 2; the Works Casing and Ram Housing are metas 0 and 1. The meta table is in [metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-casing-texture-pages).
- **Interior plumbing.** Added on the owner's request (see inside, more character). `P` and `U` are fixed `gt.blockcasings2` blocks (metas 13 and 10, [Casings.java L80-L113](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/casing/Casings.java#L80-L113)). `H` is GT's `ofCoil` ([L638-L682](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTStructureUtility.java#L638-L682)) with a getter that answers null, so every coil level passes; it sits on the Heating Coil channel, and the trigger stack is pinned to size 1 because the stack size is the bulb count here and `ofCoil` would read it as a coil tier. The glass element is on the Borosilicate Glass channel.
- **Not covered by a scenario.** A full Retort production run (it needs hatch inventories filled by a test) and the hologram look; `machines.reaction.retort` builds the structure with `construct`, adds hatches and checks it forms and un-forms, and also removes and restores the pump, the dip tube, a floor pipe run and a coil (mixed coil tiers pass).

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
| `~` | Controller | n/a |

### Pieces and layers

**Head** (2 long, the booth; the controller is in the front wall at middle height):

```
y6        y5        y4        y3        y2        y1        y0
...H...   BBBBBBB   LGGGGGL   LGGGGGL   LRR~RRL   LRRRRRL   LRRRRRL
.......   BBBBBBB   LP---PL   L-----L   L-----L   LF---FL   LRRRRRL
```

**Bay** (2 long, repeated `n` times; the front row is the portal frame, the back row is the open gap):

```
y6        y5        y4        y3        y2        y1        y0
...H...   BBBBBBB   L-NXN-L   L--F--L   L-----L   LF---FL   LRTTTRL
.......   BBBBBBB   -P---P-   -------   -------   -F---F-   RRTTTRR
```

The hoist is real: under each portal beam a steel gearbox (`X`) sits between two motor casings (`N`) with a frame-box hook (`F`) hanging from it, below the Gantry Hoist on the roof. The floor rails (`F`) run along both sides of the whole row, from the booth through every bay, and a steel pipe run (`P`) hangs under the beam in the gap aisle.

**Tail** (1 long):

```
y6        y5        y4        y3        y2        y1        y0
.......   BBBBBBB   LRRRRRL   LRRRRRL   LRRRRRL   LRRRRRL   LRRRRRL
```

**Block counts.** Head `B` 14, `L` 20, `G` 10, `R` 19, `H` 1, `P` 2, `F` 2. Bay `B` 14, `L` 10, `R` 6, `T` 6, `H` 1, `X` 1, `N` 2, `F` 5, `P` 2. Tail `B` 7, `L` 10, `R` 25. The interior parts (`F`, `X`, `N`, `P`) are required, are not Gantry Casings, and never sit on a hatch face. `G` is any glass tier, mixed is fine.

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
- **Glass.** Any glass tier, mixed. The getter passed to `chainAllGlasses` returned the stored tier, which forced one tier for the whole booth; it now always answers "not set", as the Retort's does. The Borosilicate Glass channel is on the tooltip.
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
| 58 | Unit test: an Ammo Press recipe runs in the Works; the Retort map is the Reaction Chamber map |
| 59, 65 | Unit test of the parallel formula for each size and tier, including the power cap |
| 60 | Unit test of the speed multiplier for belt tiers 0 to 7; `/gregguns test` with a mixed-tier belt fails |
| 61 | `/gregguns test` with an ME Output Bus and a full normal bus |
| 62, 68 | Remove each required hatch in turn; check the error text |
| 63, 81 | Manual client check of the animation and its toggle; dedicated server boot with no client class loaded |
| 66 | Unit test: a recipe at tier EV with IV and EV glass |
| 67 | Seeded unit test of the risk state machine (shares code with [GGS-08-19](reaction-chamber-multiblock.md#requirements) to 21): one application per batch |
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
- **Hologram** takes the bay count from the length channel if present, else the stack size (2 to 8).
- **Voiding** uses GT's void protection mode (item modes only), on by default; a full Output Bus pauses the hall.

## Open questions

- **Glass tier numbering.** Does `BorosilicateGlass` tier map to GT voltage indices, and is there glass up to IV? The Retort's rating gate needs it. Confirm in M4.
- **Parallel and belt numbers.** `4 x n x tier` and the 0.25 belt step are proposals. A balance pass fixes them.
- **Gantry NEI.** A page showing repair materials per item, or none.
- **Gun Mount Hatch contents.** Does the hatch also hold a camo-changeable gun's camo? It is gun NBT, so yes by default.
- **Rampart range.** Gun range is a property of the gun and its AI ([10](../../techguns-spec/10-entities-npcs/index.md)); a wall mount may need a higher view check.
- **ID block.** All IDs depend on the GTNH team's agreement ([index.md](index.md#open-questions)).
