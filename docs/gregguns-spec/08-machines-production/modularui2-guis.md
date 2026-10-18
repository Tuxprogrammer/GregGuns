# ModularUI2 GUIs

How kept GregGuns machines open and build their GUIs, in place of Techguns' `IGuiHandler`, containers and `GuiContainer` classes.

- **Techguns baseline:** [gui-handler.md](../../techguns-spec/08-machines-production/gui-handler.md#1-techgunsguihandler-complete-gui-id-table) (ID table, base GUI classes, slots), [gui-textures.md](../../techguns-spec/08-machines-production/gui-textures.md), and the button packet in [01-core networking](../../techguns-spec/01-core/networking.md#0--guibuttonclick-cs)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (only the kept machines need GUIs), [0006 art direction](../../design/0006-art-direction.md)
- **Pack mods:**
  - ModularUI2 (`modularui2`, 2.3.92-1.7.10)
  - GT5-Unofficial (`gregtech`, 5.09.54.205), which wraps ModularUI2 for its machines
  - ModularUI (`modularui`, 1.3.4), the old API that GT still ships next to MUI2
- **Roadmap:** M4 (Reaction Chamber), M6 (benches, turret)

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| `TechgunsGuiHandler` maps numeric GUI IDs to a `Container` and a `GuiContainer` | No GregGuns handler. GT's `MetaTileEntityGuiHandler` ("gregtech:mte") opens the GUI of whichever MTE is at the clicked position | The GUI belongs to the MTE; GT already handles distance, access and the client screen |
| One `Container` plus one `GuiContainer` class per machine, slots as `SlotOutput` and similar | One `buildUI` returning a `ModularPanel`; slots are widgets bound to the MTE's inventory | MUI2 model |
| Buttons send `GUIButtonClick` and the server switches on a button ID | Widgets bind to `IntSyncValue` / `BooleanSyncValue` objects with `allowC2S()`; no GregGuns packet | Removes a custom packet |
| `isUseableByPlayer` with a 12-block range | GT's `MAX_INTERACTION_DISTANCE = 64` as a squared distance, so 8 blocks | Inherited. Not configurable from our side |
| Hand-drawn GUI PNGs with region maps | Panel from GT's builder plus GregGuns textures through `UITexture.fullImage(modid, path)` | Only the Reaction Chamber and the area 09 benches need art ([0002](../../design/0002-machine-deconfliction.md)); the Techguns GUI PNGs of dropped machines are not needed |
| English strings hard-coded ([MPRD-31](../../techguns-spec/08-machines-production/index.md#suspected-bugs)) | Every string through `StatCollector` / lang keys | Fixes MPRD-31 |
| Inventory-tab and player-inventory GUI ID ([MPRD-29](../../techguns-spec/08-machines-production/index.md#suspected-bugs)) | Not needed in machine GUIs | |

## Integration points

### GT5-Unofficial: opening an MTE GUI through MUI2
- **Pack config:** none. `GTGuis.GLOBAL_SWITCH_MUI2` is `true` in code ([GTGuis#L29](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/modularui2/GTGuis.java#L29)).
- **Source:**
  - `MetaTileEntity#openGui` uses MUI2 when `GLOBAL_SWITCH_MUI2 && useMui2()` or `forceUseMui2()`, otherwise the old MUI1 `addUIWidgets` path ([CommonMetaTileEntity#L626-L640](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L626-L640)). `useMui2()` defaults to `false` ([L650](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L650)).
  - `buildUI(PosGuiData, PanelSyncManager, UISettings)` returns `null` by default; the Javadoc says it runs on both sides and that sync handlers must be registered with the `PanelSyncManager` ([L686-L694](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L686-L694)).
  - [`MetaTileEntityGuiHandler`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/modularui2/MetaTileEntityGuiHandler.java#L20-L60) is the factory. It rejects fake players and out-of-dimension opens, and a squared distance above 64.
  - `MTEMultiBlockBase` already returns `true` from `useMui2()` ([L3575-L3578](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L3575-L3578)) and builds its panel with `getGui().build(...)` ([L4044-L4050](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L4044-L4050)).
  - Not every single block is on MUI2: `MTEBasicMachine` overrides `useMui2()` to return `false` ([L1795](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachine.java#L1795)). Hatches and tanks vary.
- **GregGuns does:**
  - Multiblocks use the inherited MUI2 path and override `getGui()`.
  - Single-block MTEs (area 09) override `useMui2()` to return `true` and implement `buildUI` with `GTBaseGuiBuilder` ([GTBaseGuiBuilder#L48](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/modularui2/factory/GTBaseGuiBuilder.java#L48)) for the frame, or a `MTETieredMachineBlockBaseGui` subclass ([L51-L110](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/gui/modularui/singleblock/base/MTETieredMachineBlockBaseGui.java#L51-L110)). They never use MUI1 `addUIWidgets`.
  - GregGuns registers no `IGuiHandler`, no GUI IDs, no container classes.
- **Pack-side change:** none.

### GT5-Unofficial: extending the multiblock panel
- **Source:** `MTEMultiBlockBaseGui` has overridable builders for each region: `registerSyncValues` ([L1175](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/gui/modularui/multiblock/base/MTEMultiBlockBaseGui.java#L1175)), `createTerminalTextWidget`, `createLeftPanelGapRow` / `createRightPanelGapRow` ([L704-L725](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/gui/modularui/multiblock/base/MTEMultiBlockBaseGui.java#L704-L725)), `initPanelMap` and the panel size getters. Precedent for a subclass: [`AntimatterForgeGui`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/gui/modularui/multiblock/AntimatterForgeGui.java#L21-L50) adds sync values and text lines. The MTE side is a `getGui()` override, as in [`MTEPreciseAssembler#L435`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/goodgenerator/blocks/tileEntity/MTEPreciseAssembler.java#L435).
- **GregGuns does:** `ReactionChamberGui extends MTEMultiBlockBaseGui<MTEReactionChamber>`:
  - `registerSyncValues`: an `IntSyncValue` for `intensity` with `allowC2S()` (the GUI writes it), and read-only sync values for the preferred intensity, completion, required completion and elapsed fraction.
  - `createLeftPanelGapRow`: adds the intensity + and - buttons and a 0-10 gauge with a marker for the current preferred intensity, drawn from GregGuns textures. This replaces Techguns buttons 5 and 6 and the intensity bar.
  - `createTerminalTextWidget`: lines for "Preferred intensity", "Completion x/y" and the risk name.
  - The server validates the written intensity (0-10) in the sync value's setter.
  - GT's standard terminal already shows structure errors, maintenance issues and shutdown reasons; GregGuns writes no replacement.
- **Pack-side change:** none.

### ModularUI2: textures and widgets
- **Source:** [`UITexture.fullImage(String mod, String location)`](https://github.com/GTNewHorizons/ModularUI2/blob/2.3.92-1.7.10/src/main/java/com/cleanroommc/modularui/drawable/UITexture.java#L126) builds a texture from a PNG under `assets/<mod>/textures/`. `IGuiHolder`, `PanelSyncManager` and `ModularPanel` live in `com.cleanroommc.modularui` ([IGuiHolder](https://github.com/GTNewHorizons/ModularUI2/blob/2.3.92-1.7.10/src/main/java/com/cleanroommc/modularui/api/IGuiHolder.java), [PanelSyncManager](https://github.com/GTNewHorizons/ModularUI2/blob/2.3.92-1.7.10/src/main/java/com/cleanroommc/modularui/value/sync/PanelSyncManager.java)).
- **GregGuns does:** keeps a `GregGunsGuiTextures` class with one `UITexture` per sprite. Sprites are cut from GregGuns-owned PNGs under the art direction in [0006](../../design/0006-art-direction.md). Nothing is copied from the Techguns GUI PNGs ([originality rule](../../../CONTRIBUTING.md#originality-the-techguns-boundary)).
- **Pack-side change:** none.

## Requirements

- **GGS-08-26** GregGuns registers no `IGuiHandler` and no `NetworkRegistry.registerGuiHandler`. Every machine GUI opens through GT's MTE handler.
- **GGS-08-27** Every GregGuns MTE with a GUI returns `true` from `useMui2()` (multiblocks inherit this) and implements its panel in `buildUI` or `getGui()`.
- **GGS-08-28** GUI input from the client (intensity buttons, toggles) goes only through `allowC2S()` sync values, and the server clamps or rejects out-of-range values.
- **GGS-08-29** Every visible GUI string is a lang key. There is no hard-coded English.
- **GGS-08-30** GregGuns GUI sprites live in one `GregGunsGuiTextures` class and are not taken from Techguns assets.

## Tests

| Req | Verification |
|---|---|
| 26 | Code search for `IGuiHandler` and `registerGuiHandler` in `src/main` |
| 27 | Unit test: for each registered MTE class, `useMui2()` is true (reflection) |
| 28 | Unit test on the sync value setters: values below 0 and above 10 are rejected |
| 29 | Lang test: every key used in the GUI classes exists in `en_US.lang`; code search for string literals passed to `IKey.str` |
| 30 | Review checklist against [CONTRIBUTING originality](../../../CONTRIBUTING.md#originality-the-techguns-boundary) |

## Open questions

- **MUI2 availability on the dev classpath.** `dependencies.gradle` should list `modularui2` at 2.3.92-1.7.10 (pack version). Confirm when M4 starts.
- **Cover tabs.** GT's source notes that cover tabs are unfinished on MUI2 ([CommonMetaTileEntity#L653-L658](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L653-L658)). The Turret (area 09) should not depend on covers.
