# 08 · Machines: production (GregGuns spec)

Baseline: [Techguns 08-machines-production](../../techguns-spec/08-machines-production/index.md). Read each sheet next to the Techguns sheet it links to.

## Overview

[0002](../../design/0002-machine-deconfliction.md) drops five of the six Techguns production machines and rebuilds the sixth as a GT multiblock. This area therefore has three jobs:

1. Say how the kept machines plug into GT: MetaTileEntity IDs, the registration window, base classes, casing textures, inherited rules and StructureLib.
2. Say where each dropped machine's recipes go in GT's recipe maps, and how GregGuns registers them.
3. Say how the Reaction Chamber, its GUI, its NEI tab and its scripting work on top of that.

The MetaTileEntity facts are shared with area [09](../../techguns-spec/09-machines-utility/index.md) (Repair Bench, Camo Bench, Turret). Area 09 should link to the anchors in the second table below.

## Contents

| File | Topic |
|---|---|
| [metatileentity-framework.md](metatileentity-framework.md) | MTE ID block, registration window, base classes, casing texture pages, inherited explosion/rain/fire/maintenance rules, pollution, StructureLib |
| [recipe-map-replacements.md](recipe-map-replacements.md) | Ammo Press, Metal Press, Chem Lab, Fabricator and Charging Station as GT recipes; the recipe builder |
| [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md) | The Reaction Chamber as a `MTEExtendedPowerMultiBlockBase`: structure, recipe map, focus/intensity/risk rules, radiation, pollution |
| [modularui2-guis.md](modularui2-guis.md) | GUIs through GT's MUI2 handler instead of `IGuiHandler` |
| [nei-handlers.md](nei-handlers.md) | NEI tabs and catalysts, and the one handler-ordering row |
| [recipe-scripting.md](recipe-scripting.md) | MineTweaker: GTTweaker for GT maps, one GregGuns ZenClass |

### Anchors for area 09

| Need | Link |
|---|---|
| Reserving MTE IDs | [metatileentity-framework.md#gt5-unofficial-mte-id-reservation](metatileentity-framework.md#gt5-unofficial-mte-id-reservation) |
| When an MTE may be constructed | [metatileentity-framework.md#gt5-unofficial-registration-window](metatileentity-framework.md#gt5-unofficial-registration-window) |
| Which base class to extend | [metatileentity-framework.md#gt5-unofficial-base-classes-to-extend](metatileentity-framework.md#gt5-unofficial-base-classes-to-extend) |
| Casing and texture pages | [metatileentity-framework.md#gt5-unofficial-casing-texture-pages](metatileentity-framework.md#gt5-unofficial-casing-texture-pages) |
| Explosion, rain, fire and wrench rules | [metatileentity-framework.md#gt5-unofficial-world-rules-every-mte-inherits](metatileentity-framework.md#gt5-unofficial-world-rules-every-mte-inherits) |
| Opening a GUI | [modularui2-guis.md#gt5-unofficial-opening-an-mte-gui-through-mui2](modularui2-guis.md#gt5-unofficial-opening-an-mte-gui-through-mui2) |
| Requirements | GGS-08-01 to GGS-08-07 (framework), GGS-08-26 to GGS-08-30 (GUI) |

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Machine framework (base tile classes, RF, redstone modes, ownership) | One `MetaTileEntity` per kept machine; EU; GT soft-mallet and wrench rules; no ownership | GT5-Unofficial `MetaTileEntity`, `MTETieredMachineBlock` | [metatileentity-framework.md](metatileentity-framework.md) |
| Block metas as machine IDs | MTE ID block 20100-20163 (proposed) | `MetaTileEntityIDs`, `GregTechAPI.METATILEENTITIES` | [framework, ID reservation](metatileentity-framework.md#gt5-unofficial-mte-id-reservation) |
| Master/slave multiblock with hidden blocks | StructureLib structure, casing on texture page 50, hologram projector | StructureLib 1.4.45, `Textures.BlockIcons` | [framework, StructureLib](metatileentity-framework.md#structurelib-structure-definition-hologram-and-survival-build) |
| Ammo Press | Assembler recipes, programmed circuit as plan | `RecipeMaps.assemblerRecipes` | [recipe-map-replacements.md](recipe-map-replacements.md) |
| Metal Press | Bender, Wiremill and Forming Press already cover it; Assembler for parts | `benderRecipes`, `wiremillRecipes`, `formingPressRecipes` | [recipe-map-replacements.md](recipe-map-replacements.md) |
| Chem Lab | Chemical Reactor, Mixer, Fluid Canner | `UniversalChemical`, `mixerRecipes`, `cannerRecipes` | [recipe-map-replacements.md](recipe-map-replacements.md) |
| Fabricator | Assembler, Circuit Assembler, Assembly Line for top tier | `assemblerRecipes`, `circuitAssemblerRecipes`, `GTRecipeConstants.AssemblyLine` | [recipe-map-replacements.md](recipe-map-replacements.md) |
| Charging Station | GT Battery Buffer and IC2 electric items | `MTEBasicBatteryBuffer` | [recipe-map-replacements.md](recipe-map-replacements.md#charging-station) |
| Reaction Chamber | GT multiblock, own recipe map, focus as non-consumed input, GT hatches | `MTEExtendedPowerMultiBlockBase`, `RecipeMapBuilder` | [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md) |
| RC explosion and unimplemented radiation risks | `doExplosion(V[n])`, `GTUtility.applyRadioactivity`, GT hazmat | `Hazard.RADIOLOGICAL`, `HazardProtection` | [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#gt5-unofficial-risks-radiation-and-explosions) |
| (not in Techguns) pollution | RC pollutes through a Muffler Hatch at a GregGuns-config rate | `Pollution`, `MTEMultiBlockBase#polluteEnvironment` | [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#gt5-unofficial-pollution) |
| `TechgunsGuiHandler`, containers, GUI button packet | MUI2 panels via GT's MTE GUI handler; sync values | ModularUI2 2.3.92, `MetaTileEntityGuiHandler` | [modularui2-guis.md](modularui2-guis.md) |
| Six NEI handlers (`Techguns:*`) | None. GT's automatic handler per recipe map, auto catalysts | NEI 2.8.155, `NEIGTConfig` | [nei-handlers.md](nei-handlers.md) |
| Seven `mods.techguns.*` ZenClasses | GTTweaker for GT maps; one `mods.gregguns.ReactionChamber` | CraftTweaker 3.4.8, GTTweaker 2.3.4 | [recipe-scripting.md](recipe-scripting.md) |
| Rendering (TESRs, item renderer), sounds, lang, GUI PNG regions, block catalogue | No integration point; see the Techguns sheets and [0006](../../design/0006-art-direction.md). Sounds and renderers follow areas 03 and 07 | none | [rendering.md](../../techguns-spec/08-machines-production/rendering.md), [sounds.md](../../techguns-spec/08-machines-production/sounds.md), [lang.md](../../techguns-spec/08-machines-production/lang.md) |

## Pack-side changes

| File | Change | Why | Sheet |
|---|---|---|---|
| GT5-Unofficial `src/main/java/gregtech/api/enums/MetaTileEntityIDs.java` (header Javadoc; a PR to GT5-Unofficial, not the modpack repo) | Add a line: `20100 - 20163 are used by GregGuns` | The repo has no per-addon range registry. Writing the range down is the only way to keep another addon off it | [metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-mte-id-reservation) |
| GT5-Unofficial `src/main/java/gregtech/api/enums/Textures.java` (page-owner comment above `casingTexturePages`; a PR to GT5-Unofficial) | Add a line: `page 50: GregGuns casings` | Same reason, for casing texture pages | [metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-casing-texture-pages) |
| `config/NEI/handlerordering.csv` | Optional row `gregguns.recipe.reactionchamber,-5` | Put the Reaction Chamber tab beside GT's chemical tabs. Missing rows sort at 0, so this is cosmetic | [nei-handlers.md](nei-handlers.md#gt5-unofficial-one-nei-handler-per-recipe-category) |

No change is needed in `config/GregTech/GregTech.cfg`, `MachineStats.cfg`, `Pollution.cfg` or `hiddenhandlers.cfg`.

## Related areas

| Topic | Area |
|---|---|
| Repair Bench, Camo Bench and Turret as MTEs | [09-machines-utility](../../techguns-spec/09-machines-utility/index.md), which reuses the framework sheet |
| Machine block textures, casing block definition | [07-blocks](../../techguns-spec/07-blocks/index.md) |
| Ammo, plates, wires, foci and the Machine Upgrade item | [06-items](../../techguns-spec/06-items/index.md) |
| Hazmat armour that should register with GT's `IHazardProtector` | [05-armor](../../techguns-spec/05-armor/index.md) |
| Config file, packets (`GUIButtonClick` goes away), HUD | [01-core](../../techguns-spec/01-core/index.md) |

## Open questions

Each needs the named decision before M4 starts.

| Question | Settles under |
|---|---|
| Agree the MTE ID block (20100-20163) and texture page 50 with the GTNH team, or pick others | A short new design record, or a reply on the GT5-Unofficial PR |
| 0003 says items charge in a buffer of "that tier or higher", but GT's Battery Buffer takes an item only at **exactly** its tier ([recipe-map-replacements.md](recipe-map-replacements.md#open-questions)) | An amendment to [0003](../../design/0003-energy-model.md) |
| Fate of the Techguns stack upgrade (no GT counterpart) | Area 06 and a design record |
| Which Reaction Chamber recipes survive, and dropping the fluid-level mini-game ([reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#open-questions)) | A design record at M4 start, since it changes gameplay |
| Which MTE registration hook to use: `sAfterGTPreload` as GGFab does, or an `init` handler | M4 boot test (see the framework sheet) |

## Requirement index

| IDs | Sheet |
|---|---|
| GGS-08-01 to 07 | [metatileentity-framework.md](metatileentity-framework.md#requirements) |
| GGS-08-08 to 15 | [recipe-map-replacements.md](recipe-map-replacements.md#requirements) |
| GGS-08-16 to 25 | [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#requirements) |
| GGS-08-26 to 30 | [modularui2-guis.md](modularui2-guis.md#requirements) |
| GGS-08-31 to 34 | [nei-handlers.md](nei-handlers.md#requirements) |
| GGS-08-35 to 38 | [recipe-scripting.md](recipe-scripting.md#requirements) |
