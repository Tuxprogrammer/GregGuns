# Reaction Chamber as a GT multiblock

The one Techguns production machine that [0002](../../design/0002-machine-deconfliction.md) keeps. It becomes a GT `MTEExtendedPowerMultiBlockBase` with real casing blocks, GT hatches, EU, a custom recipe map and its own focus, intensity and risk rules.

- **Techguns baseline:** [reaction-chamber.md](../../techguns-spec/08-machines-production/reaction-chamber.md) (structure in [section 1](../../techguns-spec/08-machines-production/reaction-chamber.md#1-structure), logic in [section 4](../../techguns-spec/08-machines-production/reaction-chamber.md#4-reaction-logic), recipes in [section 5](../../techguns-spec/08-machines-production/reaction-chamber.md#5-full-recipe-list-tgmachinerecipes)), [framework.md](../../techguns-spec/08-machines-production/framework.md#9-multiblock-framework)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md), [0003 energy model](../../design/0003-energy-model.md)
- **Pack mods:**
  - GT5-Unofficial (`gregtech`, 5.09.54.205), which includes the BartWorks glass API
  - StructureLib (`structurelib`, 1.4.45)
  - ModularUI2 (`modularui2`, 2.3.92-1.7.10)
- **Roadmap:** M4. Tier from 0001: the UV Emitter and Laser Focus are EV, the Anti Gravity Core is IV.

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| 36 hidden blocks (17 housing, 18 glass, 1 controller) drawn by a TESR | Visible GregGuns casing blocks and GT glass, built with StructureLib; the Techguns chamber model becomes an optional decoration | Standard GT multiblock; fixes [MPRD-35](../../techguns-spec/08-machines-production/index.md#suspected-bugs) |
| Form by right-clicking the controller | Hologram projector or hand-built; GT checks the structure on its own timer | [metatileentity-framework.md](metatileentity-framework.md#structurelib-structure-definition-hologram-and-survival-build) |
| Power port at the top center, 1 M RF buffer | One or more GT Energy Hatches. EU/t is the recipe's `eut` | [0003](../../design/0003-energy-model.md) |
| Item and fluid ports (3 bottom blocks); one 10 000 mB tank | Input Bus, Output Bus and Input Hatch placed in any bottom-ring casing | GT hatches |
| Focus in a dedicated GUI slot | Focus is a **non-consumed recipe input** (stack size 0) held in an Input Bus | GT's convention for molds and lenses; shows in NEI |
| Liquid level setting 0-10 and exact tank match | Dropped. The recipe lists the fluid amount it consumes at start | GT consumes fluid at recipe start and has no level concept |
| Intensity setting 0-10, drifting preferred intensity | Kept as an MTE field with GUI buttons | The unique gameplay |
| 3-second reaction ticks, completion count, per-tick RF | Kept. Recipe duration is `ticks x 60` game ticks. `eut` is the per-tick draw | Same feel under GT's power rules |
| Redstone mode ignored for multiblocks ([MPRD-07](../../techguns-spec/08-machines-production/index.md#suspected-bugs)) | GT soft-mallet pause and redstone work | Inherited |
| `RAD_*`, `EXPLOSION_HIGH` and `UNFORSEEN_CONSEQUENCES` risks unimplemented | Implemented with GT hazards, see below | Techguns left them as TODOs |
| Final reaction tick skipped ([MPRD-18](../../techguns-spec/08-machines-production/index.md#suspected-bugs)), wrong recipe after reload ([MPRD-03](../../techguns-spec/08-machines-production/index.md#suspected-bugs), [MPRD-04](../../techguns-spec/08-machines-production/index.md#suspected-bugs)) | Not reproduced. GT recipes are looked up by content and saved by GT | |

## Single block and multiblock forms

**Update from the owner's machine policy.** [0002](../../design/0002-machine-deconfliction.md) made the Reaction Chamber multiblock only. GregGuns' own machines are now Techguns-style single blocks **and**, for machines used regularly, also a multiblock. The Reaction Chamber is a bottleneck machine (the sole source of the Laser Focus and the Anti Gravity Core), so it has both. The roster is [machine-roster.md](machine-roster.md).

| Form | What it is | IDs | Sheet |
|---|---|---|---|
| **Reaction Chamber (single block)** | A tiered `MTETieredMachineBlock` (EV and IV) that runs `gregguns.recipe.reactionchamber` one recipe at a time. It has the same focus input, intensity buttons, completion count, risks and pollution-free operation as below, with no structure to build. Its front texture is a glass window with an animated bubbling overlay while active. It needs no Muffler. The risk and radiation rules are identical | 20106 EV, 20107 IV | this file |
| **Retort (multiblock)** | The flask-shaped structure in [multiblocks.md](multiblocks.md#retort), with parallels and a glass-tier pressure rating. It carries the "Structure" section below forward in spirit; its shape replaces the 3 x 4 x 3 chamber | 20108 | [multiblocks.md](multiblocks.md#retort) |

How the sections below apply:

- "Recipe map", "Integration points" (processing, risks, pollution) and the recipe table apply to **both** forms. The single block's pollution is zero (no structure, no muffler); this is a deliberate asymmetry: the structure is what makes the multiblock a polluting industrial machine. A design record can revisit it.
- "Structure" below describes the original 3 x 4 x 3 chamber and is **superseded** by the Retort. It is kept so the requirement IDs below still read.
- [GGS-08-16](#requirements) and [GGS-08-17](#requirements) now apply to the Retort with the shape and casing minimum in [multiblocks.md](multiblocks.md#retort), not to the 3 x 4 x 3 shape. Nothing is renumbered.

## Structure

*Superseded by the Retort ([multiblocks.md](multiblocks.md#retort)); kept for reference.* Keeps the 3 x 4 x 3 footprint. Proposed layers (bottom to top), `~` = controller:

| Layer | Content |
|---|---|
| 0 | 3 x 3 casing ring. Controller front-center. Input Bus, Output Bus, Input Hatch, Maintenance Hatch may replace any casing here |
| 1 and 2 | 3 x 3 glass, including the center column |
| 3 | 3 x 3 casing. Energy Hatch and Muffler Hatch may replace casings here |

| Element | Choice | Evidence |
|---|---|---|
| Casing | New `Reaction Chamber Casing` block on texture page 50 | [metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-casing-texture-pages) |
| Glass | BartWorks `BorosilicateGlass.ofBoroGlassAnyTier()`, so any registered glass tier fits and the chamber is visible ([BorosilicateGlass#L159](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/API/BorosilicateGlass.java#L159)) | same file, `ofBoroGlass(int tier)` at [L144](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/API/BorosilicateGlass.java#L144) if a minimum tier is wanted |
| Hatch rules | `buildHatchAdder(MTEReactionChamber.class).atLeast(InputBus, OutputBus, InputHatch, Maintenance, Energy, Muffler).casingIndex(...)`, then the `check*` calls in `checkMachine` | Same pattern as [MTEIndustrialBendingMachine#L58-L68](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialBendingMachine.java#L58-L68) and [#L131-L141](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialBendingMachine.java#L131-L141) |
| Minimums | 1 Energy Hatch, 1 Input Bus, 1 Output Bus, 1 Input Hatch, 1 Maintenance, 1 Muffler. At least 8 casings (17 casing positions minus the hatches) | Mirrors the example's `checkCasingMin` and `checkHas*` |

## Recipe map and the 7 Techguns recipes

GregGuns defines its own recipe map. GT addons do this in a static class with `RecipeMapBuilder.of(name)...build()` and `SimpleRecipeMetadataKey.create(...)` ([IGRecipeMaps#L15-L37](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtnhintergalactic/recipe/IGRecipeMaps.java#L15-L37)). `RecipeMap` throws if the name is already used ([RecipeMap#L69-L73](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMap.java#L69-L73)).

```java
public static final RecipeMetadataKey<Integer> PREFERRED_INTENSITY =
    SimpleRecipeMetadataKey.create(Integer.class, "gregguns_rc_intensity");
// also: INTENSITY_MARGIN, REQUIRED_COMPLETION (Integer), RISK (a String id or enum name)

public static final RecipeMap<RecipeMapBackend> reactionChamberRecipes = RecipeMapBuilder
    .of("gregguns.recipe.reactionchamber")
    .maxIO(2, 4, 1, 0)          // input + non-consumed focus; 4 outputs as in Techguns
    .minInputs(1, 0)
    .build();
```

Which Techguns recipes survive ([0002](../../design/0002-machine-deconfliction.md): GT supplies GT-equivalent products):

| # | Techguns recipe | Decision | Reason |
|---|---|---|---|
| 1 | Wheat Seeds to Wheat + 2 Seeds | Drop | Farming demo; no GregGuns item uses it |
| 2 | `gemDiamond` to Laser Focus (Heat Ray focus, 4 buckets of liquid redstone) | **Keep**, EV | Laser Focus is a GregGuns component. Redstone fluid becomes a GT fluid (molten redstone or equivalent) |
| 3 | Titanium Iron Ore to Titanium Ore + Iron Ore | Drop | GT ore system |
| 4 | Quartz Rod to Blaze Rod | Drop unless a GregGuns recipe needs a Quartz Rod | Blaze rods exist in vanilla and GT |
| 5 | Redstone block to Glowstone block | Drop | GT already makes it; was the "medium explosion" demo |
| 6 | Nether Star to Anti Gravity Core (4 buckets of liquid ender, `EXPLOSION_LOW`) | **Keep**, IV | Anti-grav device is IV in [0001](../../design/0001-tier-map.md). Ender fluid becomes a GT fluid |
| 7 | Yellowcake to Enriched Uranium (`BREAK_ITEM`) | **Rework** in M5 on GT uranium; add a `RAD_*` risk | Depends on the nuclear items |

These are proposals for M4. Exact fluids, amounts, `eut` and durations are chosen when the recipes are written.

## Integration points

### GT5-Unofficial: running the Techguns mechanics on `ProcessingLogic`
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/MachineStats.cfg`, `machines` section, `disableMaintenanceChecks=false`. The Reaction Chamber therefore needs maintenance and shows the usual maintenance issues.
- **Source:**
  - Recipe lookup and consumption: `createProcessingLogic()` returning `new ProcessingLogic().setMaxParallel(1)`, `getRecipeMap()` returning our map ([pattern in MTEIndustrialBendingMachine#L167-L186](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialBendingMachine.java#L167-L186)). `ProcessingLogic#validateRecipe` is the hook for extra checks ([L552](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/logic/ProcessingLogic.java#L552)).
  - Per-tick hook: `MTEMultiBlockBase#onRunningTick(ItemStack)` is called every tick the machine runs; the default drains EU ([L1056-L1068](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L1056-L1068)).
  - Outputs are added only when `mProgresstime >= mMaxProgresstime` ([L822-L837](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L822-L837)). `stopMachine(reason)` nulls `mOutputItems` and `mOutputFluids` and disables the machine ([L1432-L1450](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L1432-L1450)). Custom reasons are made with `SimpleShutDownReason.ofNormal(id)` and `ShutDownReasonRegistry.register` ([ShutDownReasonRegistry#L23](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/shutdown/ShutDownReasonRegistry.java#L23)).
  - Non-consumed inputs are recipe inputs with stack size 0 ([RecipeMap#L92-L96](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMap.java#L90-L97), Javadoc: "stackSize == 0, like a mold").
- **GregGuns does:**
  - **Start.** GT consumes inputs and fluid, sets `mMaxProgresstime = ticks x 60` and `mEUt = -eut`. A custom `validateRecipe` rejects the start unless the controller's intensity setting equals the recipe's preferred intensity, as Techguns does.
  - **Reaction tick** (every 60 running ticks, counted in the MTE): the machine drew its EU through the default `onRunningTick`. If the intensity setting equals the current preferred intensity, `completion++`. Then roll the drift (1/3 down, 1/3 same, 1/3 up, clamped to preferred +/- margin, margin 0 means no drift). This is the Techguns rule, and the last tick **is** evaluated (fixes [MPRD-18](../../techguns-spec/08-machines-production/index.md#suspected-bugs)). The recipe's drift chance becomes a metadata value instead of a dead parameter (fixes [MPRD-17](../../techguns-spec/08-machines-production/index.md#suspected-bugs)).
  - **Early success.** When `completion >= required`, the MTE sets `mProgresstime = mMaxProgresstime` so GT delivers the outputs on its next tick.
  - **Failure.** When the duration ends with `completion < required`, the MTE applies the risk and calls `stopMachine(reason)`. Inputs are already gone, which matches "the input is lost". Outputs are dropped by `stopMachine`.
  - **State.** `intensity`, current preferred intensity, `completion` and the 60-tick counter go into the MTE's NBT (`saveNBTData`/`loadNBTData`) under our own keys, so reload does not lose the run.
- **Pack-side change:** none.

### GT5-Unofficial: risks, radiation and explosions
- **Source:**
  - `MetaTileEntity#doExplosion(long voltage)` removes the block and explodes with power from `GTValues.getExplosionPowerForVoltage` ([MetaTileEntity#L671-L687](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/MetaTileEntity.java#L671-L687), [GTValues#L468](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/GTValues.java#L468)). `BaseMetaTileEntity#doExplosion` also adds explosion pollution, drops the inventory and sends current into the cables when `machineWireFire` is on ([L1340-L1366](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L1340-L1366)).
  - `MTEMultiBlockBase#explodeMultiblock` explodes **every hatch and the controller at `V[8]`**, which is far larger than Techguns' `EXPLOSION_MEDIUM` ([L1520-L1543](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L1520-L1543)). Do not use it for the small risks.
  - Radiation: [`Hazard.RADIOLOGICAL`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/Hazard.java#L10-L16). [`GTUtility.applyRadioactivity(entity, level, amount)`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L2018-L2040) applies slowness, mining fatigue, nausea, weakness, hunger and more unless the entity is undead, an arthropod or wears a full radiological hazmat set ([HazardProtection#L28-L30](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/HazardProtection.java#L28-L30)). This is the same call GT's radioactive items use ([GTProxy#L1311](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTProxy.java#L1311)).
- **GregGuns does:** maps the Techguns `RiskType` values like this:

  | Techguns risk | GregGuns |
  |---|---|
  | `NONE`, `BREAK_ITEM` | `stopMachine`, nothing else |
  | `EXPLOSION_LOW` | `getBaseMetaTileEntity().doExplosion(GTValues.V[1])` on the controller only (blast power 3.0 from GT's lookup) and the glass ring removed. GT's own wire-fire and inventory-drop rules apply |
  | `EXPLOSION_MEDIUM` | `doExplosion(GTValues.V[2])` (blast power 4.0, the same as the vanilla-TNT-strength blast Techguns uses) and all the glass removed |
  | `RAD_LOW`, `RAD_MEDIUM`, `RAD_HIGH` | `applyRadioactivity` on players within a config radius, with level 1 / 3 / 5, plus a short `stopMachine`. Respects GT hazmat, and so respects GregGuns hazmat armor if area 05 registers it with `IHazardProtector` |
  | `EXPLOSION_HIGH`, `UNFORSEEN_CONSEQUENCES` | Not used by any kept recipe. Left out until a recipe needs them |

  The voltage index for each blast is a config option. The defaults above are far below `V[8]`. The power values come from `EXPLOSION_LOOKUP_V` and `EXPLOSION_LOOKUP_POWER` in [GTValues#L463-L466](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/GTValues.java#L463-L466).
- **Pack-side change:** none.

### GT5-Unofficial: pollution
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/Pollution.cfg`, `B:"Activate Pollution"=true`. GregGuns cannot add a key there (see [metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-pollution)).
- **Source:** `getPollutionPerSecond`, `polluteEnvironment`, `requiresMuffler` and `Pollution.addPollution`, all linked in the framework sheet.
- **GregGuns does:** the Reaction Chamber pollutes at a rate set in `config/gregguns.cfg` (default proposal: 100 per second while running, compared with the shipped `pollutionPerSecondMultiABS=200` and `pollutionPerSecondMultiAdvEBF=500` in `Pollution.cfg`; tune in M4). A failed reaction adds a one-off burst with `Pollution.addPollution`. The structure therefore requires a Muffler Hatch, which is the standard GT price for a polluting multiblock.
- **Pack-side change:** none.

## Requirements

- **GGS-08-16** The Reaction Chamber is one `MTEExtendedPowerMultiBlockBase` subclass with the shape in "Structure", registered with an ID from the GregGuns block ([GGS-08-01](metatileentity-framework.md#requirements)).
- **GGS-08-17** The structure needs at least 8 casings, at least one each of Energy Hatch, Maintenance Hatch, Muffler Hatch, Input Bus, Output Bus and Input Hatch. Anything missing gives a StructureLib error naming the missing part.
- **GGS-08-18** Recipes live in `gregguns.recipe.reactionchamber`. Each has a preferred intensity, margin, required completion and risk as metadata. The focus is a non-consumed (stack size 0) input.
- **GGS-08-19** A recipe starts only when the intensity setting equals its preferred intensity. A run ends successfully as soon as `completion >= required`.
- **GGS-08-20** Reaction ticks occur every 60 running ticks. Every tick including the last is evaluated. A margin of 0 never drifts.
- **GGS-08-21** On failure the inputs stay consumed, no outputs are produced, and the risk is applied once.
- **GGS-08-22** The in-progress state (intensity, preferred intensity, completion, tick counter) survives a world save and reload.
- **GGS-08-23** The Reaction Chamber consumes EU only, and stops with GT's power-loss reason when it cannot draw `eut`.
- **GGS-08-24** No risk calls `explodeMultiblock()`. Radiation risks use `GTUtility.applyRadioactivity` and so respect GT hazmat.
- **GGS-08-25** Pollution rate and risk strengths are config options with documented defaults.
- **GGS-08-46** The Reaction Chamber also exists as a single block at EV (ID 20106) and IV (ID 20107). It runs `gregguns.recipe.reactionchamber` with the same intensity, focus and risk rules as the multiblock, needs no structure and no Muffler Hatch, and starts a recipe only if its tier voltage is at least the recipe's.
- **GGS-08-47** GGS-08-16 and GGS-08-17 apply to the Retort ([multiblocks.md](multiblocks.md#retort)). The 3 x 4 x 3 shape in "Structure" is superseded.

## Tests

| Req | Verification |
|---|---|
| 16, 17 | `runServer` with a test world: build the structure with the hologram projector; remove each part in turn and check the error text |
| 18 | Unit test: the map has the expected recipes, each with all four metadata keys and one non-consumed input |
| 19, 20 | Unit test on the reaction state machine (pure Java, seeded `Random`): margin 0 never drifts, N-tick recipe gets N evaluations, early finish sets progress to max |
| 21 | `/gregguns test` scenario: run a recipe with wrong intensity; check inputs gone, no outputs, risk applied once |
| 22 | NBT round-trip unit test of the state |
| 23 | Manual: pull the energy hatch mid-run; machine stops with the power-loss reason |
| 24 | Code search for `explodeMultiblock`; manual: a radiation failure with and without hazmat |
| 25 | Config unit test |
| 46 | Unit test: the single-block MTEs at EV and IV share the recipe map and the reaction state machine with the multiblock; the IV single block starts the Anti Gravity Core recipe and the EV one does not |
| 47 | See the Retort tests in [multiblocks.md](multiblocks.md#tests) |

## Implementation notes (M4, single block)

Built in `com.gregguns.machine.reaction`. Deviations and decisions:

- **Base class.** The single block extends `MTEBasicMachineWithRecipe` (two item inputs, four item outputs, one fluid input, GT's MUI2 basic-machine panel), not `MTETieredMachineBlock` directly. GT's loop finds and consumes the recipe, so the Heat Ray Focus is an ordinary stack-size-0 input in the second input slot.
- **Intensity gate.** `checkRecipe` looks the recipe up first and returns "found but not met" if the intensity setting differs from the preferred one, so a wrong setting costs nothing. Changing the setting calls `markInventoryBeenModified()` so an idle machine re-checks at once instead of waiting up to 30 s.
- **No overclock.** `calculateCustomOverclock` is overridden: the duration is always reaction ticks times 60 and the draw is the recipe `eut`, whatever the hull tier. An IV chamber therefore runs the Laser Focus at 1,920 EU/t.
- **Run state.** `onPostTick` steps the pure `ReactionState` once per running tick, before GT's base class draws power, when the stored EU covers the draw. On success it sets `mProgresstime = mMaxProgresstime - 1` so GT's increment delivers the outputs. On failure it clears `mOutputItems`, ends the run and calls `ReactionEffects.applyFailure`. The state (10 ints) and the intensity are saved under `gregguns.rc.state` and `gregguns.rc.intensity`.
- **Risks.** `ReactionEffects` maps the risks as the sheet says. The Retort also shatters glass in a cube around the controller (radius 3 for `EXPLOSION_LOW`, 4 for `EXPLOSION_MEDIUM`) before `doExplosion`. The single block has no glass and no pollution.
- **Radiation recipe.** No kept recipe uses a `RAD_*` risk yet (the uranium recipe waits for M5); the radiation path is exercised by the `machines.reaction.radiation` scenario through `ReactionEffects` directly.
- **Fluids.** Laser Focus uses 1,152 mB molten Redstone, the Anti Gravity Core 1,152 mB molten Enderium, as the recipes sheet decided.
- **NEI.** The map has a special-info formatter that shows intensity, drift, required completion and the risk. The Heat Ray Focus shows as a non-consumed input.
- **GUI.** The intensity control is a minus button, a text readout ("Intensity n (target m)  Good a/b") and a plus button, shared with the Retort (`ReactionGuiWidgets`). Its placement inside the basic-machine panel has not been checked in a client yet.
- **Config.** `machines.*` keys in `docs/config.md` (GGS-08-25).
- **Items.** `part_heat_ray_focus`, `part_laser_focus` (EV) and `part_anti_gravity_core` (IV) are registered by the machines module (`ReactionParts`) so the machines work with only `modules.machines` and `modules.guns` on.

## Open questions

- **Gameplay shift from dropping the bath level.** Techguns made the player set and hold an exact fluid level. GT consumes the fluid at start. If the level mini-game matters, a second control could be added later; that needs a design record.
- **TecTech parameters.** TecTech's `TTMultiblockBase` has built-in parameter inputs ([`parametersInstantiation_EM`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/tectech/thing/metaTileEntity/multi/base/TTMultiblockBase.java#L426)) that would hold "intensity". It was not chosen because it pulls in TecTech's elemental-matter and uncertainty machinery, and its GUI. Revisit only if players should drive intensity from outside with a Parametrizer.
- **Recipe set.** Table decisions are M4 proposals. Row 7 and any radiation risk wait for M5.
- **Redstone and Quartz Rod.** Whether a Quartz Rod item exists in GregGuns is decided in area 06.
