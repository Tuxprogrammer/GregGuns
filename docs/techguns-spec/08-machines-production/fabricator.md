# Fabricator (2×2×2 multiblock)

The Fabricator is a four-ingredient assembler for high-tier components.

Classes: [`FabricatorTileEntMaster`](framework.md#1-class-hierarchy) ([`MultiBlockMasterTileEntBase`](framework.md#master-multiblockmastertileentbase)), [`FabricatorTileEntSlave`](blocks-and-items.md#2-multiblockmachineblock-registry-name-machinemultiblock) ([`MultiBlockSlaveTileEntBase`](framework.md#slave-multiblockslavetileentbase), `IEnergyReceiver`, `ISidedInventory`), `FabricatorContainer`, `GuiFabricator`, `FabricatorRecipe`, `RenderFabricator` with [`ModelFabricator`](../07-blocks/block-rendering-and-models.md#modelfabricator-uv-128128-texture-fabricatorpng).

## 1. Structure

Blocks (block [`machineMultiBlock`](../07-blocks/block-catalogue.md#registered-blocks)):
- Fabricator Controller (meta 4): ×1
- Fabricator Housing (meta 3): ×3
- Fabricator Glass (meta 5): ×4

**Shape.** A 2×2×2 cube. The bottom layer is the controller plus three housings; the top layer is four glass blocks. The controller is a bottom corner.

**Forming.** Right-click the unformed controller on one of its four **side** faces. The clicked face (`sideHit`) becomes `dir`, the structure's front. The cube must extend **away from the clicked face and to the player's left** as the player faces the controller. Clicking the top or bottom face does nothing.

Offsets relative to the controller (layer y 0 = housings, y +1 = glass):

| `dir` (face clicked) | X range | Z range |
|---|---|---|
| 2 (north face, player at −Z) | 0..+1 | 0..+1 |
| 3 (south face, player at +Z) | −1..0 | −1..0 |
| 4 (west face, player at −X) | 0..+1 | −1..0 |
| 5 (east face, player at +X) | −1..0 | 0..+1 |

**Validation** ([`MultiBlockMachineBlock.CheckFormFabricator`](blocks-and-items.md#2-multiblockmachineblock-registry-name-machinemultiblock)):
1. Every bottom-layer position except the controller must be `machineMultiBlock` meta 3 whose slave tile **isn't already linked**.
2. Then every top-layer position must be meta 5, also unlinked.

At the first failure the server sends `PacketTGMessage(0, x, y, z)` and forming stops. The chat shows "Invalid Block for Reaction Chamber at x, y, z" (wrong machine name, [MPRD-27](index.md#suspected-bugs)).

**On success:**
- the 3 housings link to the master with **type 1** (item and energy port);
- the 4 glass blocks link with **type 0** (inert);
- the master sets `complete = true` and `dir = sideHit`, then sends block and description updates.

All 8 blocks switch to the `invisible` icon and the controller's TESR draws the machine.

**Opening the GUI.** Right-click the complete controller, or any linked slave, to open GUI 19 at the controller. The block doesn't check security first ([MPRD-08](index.md#suspected-bugs)). The container's `canInteractWith` uses the security rule, so an unauthorized player's GUI closes on the next server tick. Use range is 8 blocks.

**Unforming.** Breaking the controller or any linked slave calls `master.unform()`:
- every block in the 2×2×2 box is unlinked (type reset to 0), and their description is resent;
- the master is set to incomplete with `dir` 0;
- breaking the controller also spills its inventory.

The operation state is kept. `updateEntity` **doesn't check `complete`**, so an operation keeps running while unformed as long as stored power lasts ([MPRD-16](index.md#suspected-bugs)).

## 2. Ports

| Block | Energy | Items |
|---|---|---|
| Controller | accepts RF from every side except the `dir` face, only while complete | sided access from every side except `dir`, only while complete: slots 0–4 visible; insert into 0–3 (hoppers also check `isItemValidForSlot`); extract slot 4 |
| Housing (type 1) | forwards RF to the master (`canConnectEnergy` true while type 1) | forwards inventory calls to the master. Slots 0–4 visible; insert 0–3; extract 4 |
| Glass (type 0) | none | none |

Capacity: 100 000 RF.

## 3. Inventory

| Slot | Constant | Role | GUI pos | Container slot | Accepted items |
|---|---|---|---|---|---|
| 0 | `SLOT_INPUT` | main input | (19,17) | `Slot` | anything not in the three lists below and not an upgrade |
| 1 | `SLOT_WIRES` | wires / circuits | (47,17) | `SlotRestrictedToList(items_wireslot)` | Copper Wire (shared 57), Gold Wire (86), `circuitBasic`, `circuitElite` |
| 2 | `SLOT_POWDER` | powder / parts | (68,17) | `SlotRestrictedToList(items_powderslot)` | `dustRedstone`, Mechanical Parts (Carbon) (37) |
| 3 | `SLOT_PLATE` | plate | (89,17) | `SlotRestrictedToList(items_plateslot)` | `sheetPlastic`, `plateCarbon`, [`plateTitanium`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo), [`plateLead`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) |
| 4 | `SLOT_OUTPUT` | output | (116,50) | [`SlotOutput`](ammo-press.md#1-inventory) | none |
| 5 | `SLOT_UPGRADE` | stack upgrade | (150,50) | [`SlotMachineUpgrade`](ammo-press.md#1-inventory) | Machine Upgrade |

- `isItemValidForSlot(slot, item)` is true when `getValidSlotForItemInMachine(item) == slot`. The routing checks the wire list, then the powder list, then the plate list, then the upgrade, and falls back to input.
- MineTweaker can extend the three lists (`minetweaker.md`).
- Shift-click from the player inventory uses this routing. The container ranges are correct (machine 0–5, player 6–41).
- **Master NBT extras:** `hasPower` (unused flag) and `dir`, both written only when complete; `plateUsed` (an item, kept for rendering).

## 4. Recipes

A `FabricatorRecipe` has `inputItem`/`amountInput`, `wireSlot`/`amountWire`, `powderSlot`/`amountPowder`, `plateSlot`/`amountPlates` (each an [`ItemStackOreDict`](gui-handler.md#6-itemstackoredict-recipe-ingredient-wrapper); a null stack = not required) and `outputItem`/`amountOutput`.

**Matching** (first recipe in list order):
- the input matches and has ≥ `amountInput`;
- for wire, powder and plate: the recipe slot is a null stack, **or** the machine slot is non-empty, matches and has enough.

A null-stack recipe slot ignores whatever sits in that machine slot and consumes nothing from it.

**Multiplier**, with max = min(upgrades+1, 8):
- for each input, the largest m ≤ max with m·amount ≤ stack size; an empty wire or powder slot counts as 8;
- the output limit: the largest m with stack+m·amountOutput ≤ max stack size;
- m is the minimum of all of these.

The recipe also needs the output slot empty, or holding the same item and damage with room for one unit.

**Operation** (server):
1. Copy the plate (size 1) into `plateUsed` if plates are consumed.
2. Consume m·amount from every slot.
3. Set `currentOperation` = output × `amountOutput` and totalTime = 100.

Each tick consumes 80 RF × m (8 000 RF per unit at m = 1). At 100 ticks the server adds output × m to slot 4, clears `plateUsed` and starts the next operation.

### Full recipe list (`TGMachineRecipes`)

| # | Input (amt) | Wire slot (amt) | Powder slot (amt) | Plate slot (amt) | Output |
|---|---|---|---|---|---|
| 1 | `ingotGold` (1) | Copper Wire (1) | `dustRedstone` (3) | `sheetPlastic` (1) | Energy Cell (Empty) ×1 (shared 16) |
| 2 | Soul Sand (1) | Gold Wire (1) | `dustRedstone` (1) | `sheetPlastic` (1) | Cybernetic Parts ×1 (91) |
| 3 | [`ingotTitanium`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) (2) | `circuitBasic` (4) | Mechanical Parts (Carbon) (1) | `plateCarbon` (4) | Power Plating ×2 (89) |
| 4 | Coil (1) (shared 69) | `circuitElite` (2) | Mechanical Parts (Carbon) (1) | `plateTitanium` (1) | Sonic Emitter ×1 (97) |
| 5 | Enriched Uranium (1) (102) | `circuitElite` (2) | Mechanical Parts (Carbon) (2) | `plateLead` (2) | Rad Emitter ×1 (108) |
| 6 | [`ingotSteel`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (1; ingredient declared with size 2, which is ignored) | `circuitBasic` (1) | `dustRedstone` (4) | `plateLead` (2) | Nuclear Power Cell (Depleted) ×1 (107) |

## 5. GUI (`GuiFabricator`, `textures/gui/Fabricator_Gui.png`)

- **Title.** `techguns.container.fabricator` ("Fabricator"). Inventory label as usual.
- **Slot backgrounds.** The texture has ghost silhouettes printed in the wire slot (coiled wire), the powder slot (dust pile) and the plate slot (square plate).
- **Upgrade slot frame.** Copied from the inventory slot frame at u7 v83, 18×18, drawn at (149,49).
- **Progress.** A long arrow outline is printed at about (20,53). While working, the golden gradient arrow sprite u0 v167, width progress·90/total + 1, height 11, is drawn at (21,53). It fills **left→right**.
- The power bar, redstone and security controls come from the base class.
- **Container sync:** 0 progress, 1 totalTime, 2 power/100.

## 6. Sound

`techguns:machines.fabricatorWork` plays client-side once per operation, at progress tick 1. The clip is about 5.6 s, roughly the 100-tick operation.

## 7. Visuals (`RenderFabricator`)

- **What renders.** The TESR draws only while the structure is complete; unformed blocks show their CTM textures ([07-blocks](../07-blocks/index.md)).
- **Model and texture.** `ModelFabricator` (declared 128×128) with texture [`textures/blocks/Fabricator.png`](../07-blocks/textures.md#11-model-textures) (256×256, catalogued in [07-blocks](../07-blocks/index.md)). The texture is mostly dark grey/black panels, a framed glass window region, a small screen with a cyan display (top right), and red and yellow indicator bits.
- **Transform.**
  1. Translate to (x+0.5, y+1.5, z+0.5) and flip 180° about Z.
  2. Rotate about Y by `dir`: 2 → 0°, 3 → −180°, 4 → −90°, 5 → +90°.
  3. Render bounding box when complete: controller ±1.5 in X and Z, height 2.
- **Model parts** (rendered by index):

| Part | Content |
|---|---|
| 0 | static body: base plate, inner and mid plates, frame posts and top frame, corners, side plates, side bars, connection pieces, controller panel |
| 1 | moving gantry bar (`FrameMove1`) |
| 2 | working head with first drill, first tube, laser housing and a laser-beam bar |
| 3 | drill bit |
| 4 | tube nozzle |
| 5 | laser emitter |
| 6 | glass shell (no alpha blending set) |

- **Workpiece.** While an operation runs, an `EntityItem` lies flat on the work table at local offset (0.6, 0.275, −0.5). It shows `plateUsed` until progress 0.8, then the output item.
- **Animation**, with p = progress/totalTime:
  - **Idle (p = 0):** parts 0–4 static, part 6. The laser emitter is hidden.
  - **Working:** part 0 static. The gantry (part 1, carrying everything below) slides along local Z by 0.25·sin(4πp), which is 2 sweeps. Inside it, the head (parts 2–5) slides along local X by 0.125·sin(8πp), 4 sweeps. Then:

| p range | Drill (3) | Tube (4) | Laser (5) |
|---|---|---|---|
| < 0.1 or > 0.9 | static | static | hidden |
| 0.1–0.3 | spins 8 full turns about a vertical axis at local (−0.575, ·, 0.5) and lifts by 0.3·sin²(π·s), s = (p−0.1)·5 | static | hidden |
| 0.3–0.6 | static | bobs up by 0.3·sin²(4π·s), s = (p−0.3)/0.3 | hidden |
| 0.6–0.9 | static | static | shown |

  The glass (part 6) is always drawn last.

## 8. Entity sheet summary (consistency pass)

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Block `machineMultiBlock` metas 3 (Housing), 4 (Controller), 5 (Glass); tiles `FabricatorTileEntSlave` (3, 5) and `FabricatorTileEntMaster` (4); names `techguns.tile.machineMultiBlock_3/4/5.name` = **Fabricator Housing**, **Fabricator Controller**, **Fabricator Glass**; container `FabricatorContainer`; creative tab Techguns; oredict none |
| 2 | Obtaining | Shaped ore recipes for Housing ×4, Controller ×1 and Glass ×4; full grids in [blocks-and-items.md](blocks-and-items.md#crafting-recipes) |
| 3 | Numbers | Buffer 100 000 RF; 80 RF/t times the multiplier; 100 ticks per operation; multiplier clamped to 8; structure 2×2×2; hardness 8.0, resistance 50.0; 6 recipes |
| 4 | Behaviour | Sections 1 to 4 |
| 5 | Persistence and sync | Master NBT: `complete`, `hasPower`, `dir` (the last two only when complete), `plateUsed` (item), plus base keys. Slave NBT: `hasMaster`, `masterX/Y/Z`, `type`. GUI progress-bar fields: 0 progress, 1 totalTime, 2 power/100 |
| 6 | UI | Section 5. Tooltips: standard power, redstone and security. Chat on a failed form: `techguns.tgmessage.0` "Invalid Block for Reaction Chamber at x, y, z" (MPRD-27) |
| 7 | Visuals | Unformed blocks use CTM textures `fabricatorHousing` (32×32, average #949494, dominant #F0F0F0 36 %, #101010 24 %, #D0D0D0 13 %), `fabricatorController` (32×32, average #787D7E, #101010 24 %, #D0D0D0 21 %, #B0B0B0 12 %) and `fabricatorGlass` (32×32, average #1A1919, #101010 99 %, 23 % opaque) (CTM variations; per-face differences were not examined); formed blocks show the transparent `invisible.png` (16×16, 0 % opaque). TESR `RenderFabricator` + `ModelFabricator`; atlas `textures/blocks/Fabricator.png`, 256×256, average #535151, dominant #303030 56 %, #909090 10 %, #707070 8 %, 36 % opaque. Item renderer: ordinary block icons (no custom item renderer) |
| 8 | Audio | `machines.fabricatorWork` volume 1, pitch 1, once at progress tick 1 per operation (client); three clips of 5.58 s, 44100 Hz mono |
| 9 | Relationships | `FabricatorRecipe` registry fed by [`TGMachineRecipes`](index.md#tgmachinerecipesaddrecipes-what-it-registers) and `FabricatorTweaker`; NEI handler; wires, powders and plates from [`06-items`](../06-items/index.md) |
| 10 | Bugs | [MPRD-05](index.md#suspected-bugs), [MPRD-07](index.md#suspected-bugs), [MPRD-08](index.md#suspected-bugs), [MPRD-16](index.md#suspected-bugs), [MPRD-25](index.md#suspected-bugs), [MPRD-27](index.md#suspected-bugs), [MPRD-35](index.md#suspected-bugs) |
