# MetaTileEntity framework

The shared GT plumbing every GregGuns machine uses: MTE IDs, the registration window, base classes, casing textures, the world rules machines inherit, pollution and StructureLib. Area [09](../../techguns-spec/09-machines-utility/index.md) machines (Repair Bench, Camo Bench, Turret) link here for the facts below.

- **Techguns baseline:** [framework.md](../../techguns-spec/08-machines-production/framework.md) (base tile classes, energy, redstone, multiblock master/slave), [blocks-and-items.md](../../techguns-spec/08-machines-production/blocks-and-items.md)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md), [0003 energy model](../../design/0003-energy-model.md)
- **Pack mods:**
  - GT5-Unofficial (`gregtech`, 5.09.54.205). The same jar also carries TecTech, GoodGenerator, GGFab and others.
  - StructureLib (`structurelib`, 1.4.45)
  - GTNHLib (`gtnhlib`, 0.11.52)
- **Roadmap:** M4 (production chain and machines), M6 (utility and defence)

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| Own tile-entity tree (`BasicInventoryTileEnt` to `BasicMachineTileEntity`), one `basicMachine` block with a meta per machine | One `MetaTileEntity` subclass per kept machine, registered by ID in GT's `BaseMetaTileEntity` block | Kept machines must behave like any GT machine: EU, tiers, covers, wrench, NEI catalysts |
| RF buffer plus own redstone modes | GT energy, GT tier voltage, GT soft mallet on/off | [0003](../../design/0003-energy-model.md) |
| Own ownership and security (`BasicOwnedTileEnt`) | None in the MTE layer; GT has no owner concept | Dropping it also drops [MPRD-09](../../techguns-spec/08-machines-production/index.md#suspected-bugs) (wrench ignores ownership) |
| Master/slave multiblock with 36 hidden blocks and a TESR | A GT multiblock controller with real casing blocks, built by StructureLib | Fixes [MPRD-07](../../techguns-spec/08-machines-production/index.md#suspected-bugs), [MPRD-16](../../techguns-spec/08-machines-production/index.md#suspected-bugs) and [MPRD-35](../../techguns-spec/08-machines-production/index.md#suspected-bugs) by construction |
| `TechgunsGuiHandler` ID table | GT's MUI2 handler, see [modularui2-guis.md](modularui2-guis.md) | |

## Integration points

### GT5-Unofficial: MTE ID reservation
- **Pack config:** none. IDs are a code-level registry.
- **Source:**
  - [`GregTechAPI.METATILEENTITIES`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/GregTechAPI.java#L73) is an array of `Short.MAX_VALUE - 1` slots (IDs 1 to 32766).
  - The constructor throws `IllegalArgumentException` if a slot is taken ([CommonMetaTileEntity#L87-L103](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L87-L103)).
  - [`MetaTileEntityIDs`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/MetaTileEntityIDs.java#L3-L13) lists most IDs, "sorted by ID". Its Javadoc says it "does not list all used IDs", that `-Dgt.debug=true` prints every ID in use ([GTMod#L453-L460](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L453-L460)), and that 4096-5095 (frames) and 5096-6099 (pipes) are blocked.
  - There is **no documented per-addon range** in the repo. We searched the `docs/`, `README.md` and `CODEOWNERS` of the tag.
- **Findings:** the enum holds 2033 distinct IDs. The longest runs with no enum entry are 17000-19999 (3000 IDs) and 20001-28000 (about 8000). The enum is not authoritative (see the Javadoc), so a run being free in the enum does not make it free in the pack.
- **GregGuns does:** claims one block of 64 consecutive IDs and keeps a `GregGunsMTEIDs` enum in our own package. Proposed block: **20100-20163**. Before M4 starts, check the block with `-Dgt.debug=true` on the pack, and open a PR on GT5-Unofficial that adds the range to the `MetaTileEntityIDs` Javadoc, so nobody else takes it. GregGuns needs about 10 IDs in total (Reaction Chamber, Repair Bench, Camo Bench, Turret, hatches if any).
- **Pack-side change:** a one-line comment PR to GT5-Unofficial (not the modpack repo): add `20100 - 20163 are used by GregGuns` to the `MetaTileEntityIDs` header.

### GT5-Unofficial: registration window
- **Source:**
  - The constructor above also throws `IllegalAccessError` unless `GregTechAPI.sPreloadStarted && !GregTechAPI.sPostloadStarted` ([CommonMetaTileEntity#L87-L89](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L87-L89)). So `new MyMTE(id, ...)` is legal from GT's preload start until GT's postload start.
  - GT exposes hook lists: `sBeforeGTPreload`, `sAfterGTPreload`, `sBeforeGTLoad`, `sAfterGTLoad`, `sBeforeGTPostload`, `sAfterGTPostload` ([GregTechAPI#L151-L153](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/GregTechAPI.java#L151-L153)). GT nulls them at the end of its postload ([GTMod#L615-L621](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L615-L621)).
  - Precedent for a mod outside `gregtech.*`: GGFab declares `required-after:IC2;required-before:gregtech` and adds a `Runnable` to `sAfterGTPreload` that calls the MTE constructors ([GigaGramFab#L40-L75](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/ggfab/GigaGramFab.java#L40-L75)). Detrav's scanner mod does the same ([DetravScannerMod#L37](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/detrav/DetravScannerMod.java#L37)).
  - The `MetaTileEntity` class comment still says "load phase (not preload and also not postload)". The constructor check above is the real rule, and the comment is stale.
- **GregGuns does:** in `preInit`, adds a `Runnable` to `GregTechAPI.sAfterGTPreload` that constructs every GregGuns MTE. The `@Mod` dependency string includes `required-after:gregtech` plus whatever ordering the precedent needs (see Open questions). Recipes are registered in `sBeforeGTPostload` or later, never in the constructor.
- **Pack-side change:** none.

### GT5-Unofficial: base classes to extend
- **Source:**
  - Single block, no recipe: [`MTETieredMachineBlock`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTETieredMachineBlock.java#L22) (`mTier` 0-9, textures array, description array). Subclass of `MetaTileEntity`.
  - Single block with a recipe map: `MTEBasicMachineWithRecipe` / `MTEBasicMachine` (item and fluid slots, auto-output). **We do not need it.** Every Techguns single-block recipe machine is replaced by an existing GT machine ([recipe-map-replacements.md](recipe-map-replacements.md)).
  - Multiblock with EU above `int`: [`MTEExtendedPowerMultiBlockBase`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEExtendedPowerMultiBlockBase.java#L25), which extends `MTEEnhancedMultiBlockBase` (StructureLib-aware, `checkMachine(..., List<StructureError>)`, `survivalBuildPiece`, casing and hatch counters), which extends `MTEMultiBlockBase` (hatch lists, maintenance, muffler, `ProcessingLogic`, `explodeMultiblock`).
  - Worked example of the smallest useful shape: [`MTEIndustrialBendingMachine`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialBendingMachine.java#L43-L207) (about 160 lines: structure string, `Casings`, `buildHatchAdder(...).atLeast(...)`, tooltip builder, `getRecipeMap`, `createProcessingLogic`, `getPollutionPerSecond`).
- **GregGuns does:** Repair Bench, Camo Bench and Turret extend `MTETieredMachineBlock` (area 09 decides the details). The Reaction Chamber extends `MTEExtendedPowerMultiBlockBase`. Every MTE also needs the "prototype" constructor `(int id, String name, String regional)` and the instance constructor `(String name)`, and overrides `newMetaEntity`.
- **Pack-side change:** none.

### GT5-Unofficial: casing texture pages
- **Source:** [`Textures.BlockIcons`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Textures.java#L2621-L2634) keeps a 128-page by 128-index table of casing textures. A comment above it records who owns which page (page 8 TecTech and GT casing 6, page 12 GlodBlock, page 16 GT glass and casings 9-12, page 20 CropsNH, page 42 glee8e and KekzTech). New pages are created with [`GTUtility.addTexturePage`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1847-L1853) and filled with `Textures.BlockIcons.setCasingTextureForId(id, texture)`. A hatch adder takes the numeric `casingIndex(id)` ([used in MTEIndustrialBendingMachine#L65](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialBendingMachine.java#L65)). GT's own abstract casing block registers its metas in its constructor ([BlockCasingsAbstract#L56-L61](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockCasingsAbstract.java#L56-L61)).
- **Findings:** pages 3-7, 9-11, 13-15, 18-19, 21-41 and 43-127 are unclaimed in the comment.
- **GregGuns does:** claims **page 50** for the Reaction Chamber casing (and any later casing). Its casing block calls `GTUtility.addTexturePage((byte) 50)` once, then registers `(50 << 7) | meta` for each meta. The multiblock uses that ID in `casingIndex(...)` and `ICasingTextureProvider#getCasingTexture`.
- **Pack-side change:** a comment PR to GT5-Unofficial adding `page 50: GregGuns casings` to the page list.

### GT5-Unofficial: world rules every MTE inherits
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/GregTech.cfg`, all as shipped:

  | Key | Value | Effect on a GregGuns MTE |
  |---|---|---|
  | `machineExplosions` | `true` | `MetaTileEntity#doExplosion` removes the block and creates an explosion ([MetaTileEntity#L671-L687](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/MetaTileEntity.java#L671-L687)) |
  | `machineRainExplosions` | `true` | A machine that returns `true` from `willExplodeInRain()` (the default, [L548](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/MetaTileEntity.java#L548)) can explode in rain, handled in [BaseMetaTileEntity#L507-L520](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L507-L520) |
  | `machineThunderExplosions`, `machineFireExplosions` | `true` | same family: thunder and adjacent fire |
  | `machineFlammable` | `true` | the machine block burns ([BlockMachines#L161-L178](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockMachines.java#L161-L178)) |
  | `machineNonWrenchExplosions` | `true` | breaking without a wrench can explode ([BlockMachines#L606](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockMachines.java#L606)) |
  | `machineWireFire` | `true` | cables burn on explosion |
  | `ticksBetweenSounds` | `30` | GT's own sound throttle for electric machines |

  `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/MachineStats.cfg` has no per-machine keys we can use (its sections are boilers, mass fabricator, teleporter, cleanroom and so on). Its `machines` section sets `disableMaintenanceChecks=false`, so **GT multiblocks need maintenance** in this pack ([MTEMultiBlockBase#L178-L286](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L178-L286)).
- **GregGuns does:**
  - Does not add its own explosion switches for machine damage. A GregGuns MTE is exposed to rain exactly as a GT machine is. A machine that is meant to stand outside (the Turret) overrides `willExplodeInRain()` to return `false`. That is area 09's call, flagged here because it is a one-line override.
  - The Reaction Chamber keeps maintenance. It is part of why it is a GT multiblock.
- **Pack-side change:** none.

### GT5-Unofficial: pollution
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/Pollution.cfg`, `B:"Activate Pollution"=true`. Every GT multiblock's rate has its own key in that file, defined in GT's `PollutionConfig`. **An external mod cannot add keys to it**, so a GregGuns rate must live in our own config.
- **Source:**
  - `MTEMultiBlockBase#getPollutionPerSecond(ItemStack)` returns 0 unless overridden ([L1386](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L1386)).
  - `polluteEnvironment` accumulates into `mPollution` and vents 10 000 at a time through a Muffler Hatch. With no muffler, or a blocked one, it returns false and the machine stops with `POLLUTION_FAIL` ([L885-L931](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L885-L931), caller at [L817-L819](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L817-L819)).
  - `requiresMuffler()` is true when the rate is above 0 ([L4057-L4059](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L4056-L4058)).
  - Chunk pollution is added with [`Pollution.addPollution`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/pollution/Pollution.java#L340-L396).
- **GregGuns does:** see [reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#gt5-unofficial-pollution). Single-block kept machines do not pollute.
- **Pack-side change:** none.

### StructureLib: structure definition, hologram and survival build
- **Pack config:** none.
- **Source:**
  - [`IConstructable`](https://github.com/GTNewHorizons/StructureLib/blob/1.4.45/src/main/java/com/gtnewhorizon/structurelib/alignment/constructable/IConstructable.java#L13-L25) (`construct(ItemStack, boolean hintsOnly)`, `getStructureDefinition`, `getStructureDescription`) and [`ISurvivalConstructable`](https://github.com/GTNewHorizons/StructureLib/blob/1.4.45/src/main/java/com/gtnewhorizon/structurelib/alignment/constructable/ISurvivalConstructable.java#L21) (`survivalConstruct`).
  - The pack's "Multiblock Structure Hologram Projector" is StructureLib's [`ItemConstructableTrigger`](https://github.com/GTNewHorizons/StructureLib/blob/1.4.45/src/main/java/com/gtnewhorizon/structurelib/item/ItemConstructableTrigger.java#L21-L28) (name from StructureLib's `en_US.lang`, key `item.structurelib.constructableTrigger.name`). It calls `ConstructableUtility.handle`, which finds an `IConstructable` on the clicked block. Right-click shows the hologram; sneak right-click builds. It works on any controller that implements the interfaces, with no registration.
  - Worked example: `MTEIndustrialBendingMachine` above (`construct`, `survivalConstruct`, `checkMachine`).
- **GregGuns does:** the Reaction Chamber implements `ISurvivalConstructable`. The structure is a `String[][]` shape with `~` as the controller, built with `StructureDefinition.builder()`, and each hatch position uses `buildHatchAdder(...).atLeast(...).casingIndex(...).hint(n).buildAndChain(...)`. No GregGuns code touches the hologram item.
- **Pack-side change:** none.

## Requirements

- **GGS-08-01** GregGuns reserves one block of 64 consecutive MTE IDs, 20100-20163, in a `GregGunsMTEIDs` enum. No GregGuns MTE uses an ID outside the block.
- **GGS-08-02** All GregGuns MTEs are constructed inside a `GregTechAPI.sAfterGTPreload` (or equivalent) callback, so construction happens between GT's preload start and postload start.
- **GGS-08-03** Booting the pack with `-Dgt.debug=true` shows no `MetaTileEntity id N is already occupied` error and lists every GregGuns MTE under its ID.
- **GGS-08-04** Every GregGuns multiblock casing texture uses texture page 50, registered with `GTUtility.addTexturePage`.
- **GGS-08-05** Every GregGuns multiblock implements `ISurvivalConstructable`, and the StructureLib hologram projector shows its structure when used on the controller.
- **GGS-08-06** No GregGuns code reads or writes RF. Machines take EU through GT energy hatches ([0003](../../design/0003-energy-model.md)).
- **GGS-08-07** No GregGuns MTE changes `GregTechAPI.sMachineExplosions` or the rain, fire or thunder rules. Only `willExplodeInRain()` may be overridden, and each override is justified in its machine's sheet.

## Tests

| Req | Verification |
|---|---|
| 01, 03 | Unit test: the enum has no duplicate IDs and every ID is in 20100-20163. `runServer` with `-Dgt.debug=true`: the log has `META <id> <name>` lines for ours and no duplicate-ID exception |
| 02 | `runServer` smoke test: no `IllegalAccessError` from the MTE constructor |
| 04 | Unit test on the casing block: `Textures.BlockIcons.getCasingTextureForId((50 << 7) \| meta)` is non-null after load |
| 05 | Manual: the hologram shows, and sneak-use builds in survival from the inventory |
| 06 | Code search: no `cofh.api.energy` import (architecture test) |
| 07 | Code review plus a grep test for `sMachineExplosions` |

## Open questions

- **ID block and texture page.** 20100-20163 and page 50 are proposals. They need the GTNH team's agreement. If they ask for a different range, that needs a short design record.
- **Hook choice.** GGFab needs `required-before:gregtech` to use `sAfterGTPreload`. A GregGuns `init` handler with `required-after:gregtech` also sits inside the legal window, but this tag has no external-mod precedent for it. Try both in the M4 boot test and keep the one that works with the pack's load order.
- **`willExplodeInRain` for the Turret.** Area 09 should decide.
