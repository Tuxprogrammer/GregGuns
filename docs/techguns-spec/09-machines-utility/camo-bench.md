# Camo Bench

[`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) meta 3. English name "Camo Bench". The container title is `techguns.container.camobench` = "Camo Workbench".

| Part | Class / value |
|---|---|
| Tile entity | [`CamoBenchTileEnt`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine), registered as `CamoBenchTileEnt` |
| GUI id | 3 |
| Container | `CamoBenchContainer` |
| GUI | `GuiCamoBench` |
| TESR | [`RenderTileEntityBlock`](../08-machines-production/rendering.md#4-rendertileentityblock-used-by-09s-camo-and-repair-benches) with [`PaintMachineTesselatorCube`](../07-blocks/block-rendering-and-models.md#tesselatorcube-family-fixed-uvs-for-tesrs-and-items) |

## Purpose

The bench is an unpowered bench for cycling the camouflage variant of any item that implements [`ICamoChangeable`](../05-armor/armor-catalog.md#1-class-hierarchy):

- guns ([`GenericGun`](../02-guns/gun-lifecycle.md), 02)
- multi-camo armors ([`GenericArmorMultiCamo`](../05-armor/armor-catalog.md#1-class-hierarchy), 05)
- special-slot items ([`ItemTGSpecialSlot`](../05-armor/lang-and-strings.md#3-armor-tooltips), 05/06)
- tools ([`GenericTool`](../06-items/special-items.md#generictool-unused))
- camo-changeable item blocks ([`GenericItemBlockCamoChangeable`](../07-blocks/special-blocks.md#18-camo-changeable-item-blocks-genericitemblockcamochangeable), 07)

It works on:

- one item placed in the bench slot,
- each of the four armor pieces the player is wearing,
- intended, but disabled in the GUI: the player's Techguns back slot and face slot.

`ICamoChangeable` exposes:

- camo count;
- `switchCamo(stack, back)`, which steps the stored camo index forward or back and wraps;
- current camo index;
- current camo name, a lang key.

The camo storage and naming belong to the owning areas.

## Tile entity

- Extends [`BasicInventoryTileEnt`](../08-machines-production/framework.md#2-basicinventorytileent) with **1** slot (index 0).
- `isItemValidForSlot` accepts only `ICamoChangeable` items. The GUI slot is a plain `Slot`, so the player can place anything there by hand.
- **Automation:** no accessible slots; insert and extract are both refused.
- Inventory name: the custom name if one is set, else `techguns.container.camobench`.
- **NBT:** base `Items`, `rotation`, `CustomName`.
- `getItem()` returns slot 0 only if it is `ICamoChangeable`, else null.

### Button handling (`buttonClicked`, server, only if `isUseableByPlayer`: same tile and within 8 blocks)

| Button id | Target | Direction | Sync after change |
|---|---|---|---|
| 1 | bench slot item | forward | `needUpdate()` (block update, tile description packet) |
| 2 | bench slot item | back | same |
| 3 / 4 | worn **helmet** | 3 = forward, 4 = back | an explicit set-slot packet for container slot 37 + (3 − armor index) |
| 5 / 6 | worn chestplate | 5 fwd / 6 back | as above |
| 7 / 8 | worn leggings | 7 fwd / 8 back | as above |
| 9 / 10 | worn boots | 9 fwd / 10 back | as above |
| 11 / 12 | Techguns extended inventory **back slot** ([`TGPlayerInventory.SLOT_BACK`](../05-armor/extra-inventory.md#1-data-model-tgplayerinventory-iinventory-12-slots)) | 11 fwd / 12 back | none |
| 13 / 14 | extended inventory **face slot** (`SLOT_FACE`) | 13 fwd / 14 back | none |

How the ids map:

- Armor slot = 3 − (ceil((id − 2) / 2) − 1). Odd ids go forward; even ids go back.
- For ids 11–14, even means back.
- Items that are not `ICamoChangeable` are ignored.
- Buttons 11–14 have no GUI buttons, because they are commented out ([MUTL-06](index.md#suspected-bugs)).

## Container layout (`CamoBenchContainer`)

| Container index | Inventory | Slot type | Position |
|---|---|---|---|
| 0 | bench slot | plain `Slot` | (21, 18) |
| 1–27 | player main | plain | (8 + 18·col, 84 + 18·row) |
| 28–36 | hotbar | plain | (8 + 18·i, 142) |
| 37–40 | worn armor, 37 = helmet … 40 = boots | [`SlotArmor`](../05-armor/extra-inventory.md#4-container-tgplayercontainer) | (96 + 19·i, 18) |
| 41 | extended-inventory back slot | [`SlotTG`](../05-armor/extra-inventory.md), type `BACKSLOT` | (75, 18) |
| 42 | extended-inventory face slot | `SlotTG`, type `FACESLOT` | (56, 18) |

Slots 41–42 exist only if the player's extended properties are present.

Shift-click rules:

| From | Behavior |
|---|---|
| Bench slot | `ItemArmor` goes to the matching armor slot 37 + armor type. Anything else goes to player slots 1–36, filled from the end. |
| Any other slot | Moves into the bench slot, but only if the bench slot is empty. Otherwise nothing happens. |

`addCraftingToCrafters`, `detectAndSendChanges` and `updateProgressBar` are all overridden to do nothing. The container never pushes slot contents to the client itself ([MUTL-07](index.md#suspected-bugs)).

## GUI (`GuiCamoBench`)

The GUI extends vanilla `GuiContainer` directly. Size is the default 176 × 166.

Background texture: [`textures/gui/camoBench_GUI.png`](../08-machines-production/gui-textures.md#7-texturesguicamobench_guipng-owned-by-09), 256 × 256, cataloged by 08.

| Texture region (px) | Content | Drawn at |
|---|---|---|
| (0,0) 176×166 | Gray panel. Top-left: a raised frame around one slot (~(17,14)–(40,37)). Top right of that: a raised strip of six slot wells (~(53,15)–(172,36)), each with a small recessed tab underneath (~10 × 10) in which a digit is printed. No "Inventory" caption area. Bottom: 3×9 + hotbar. | (left, top) |
| (178,0) 45×84 | Side "mirror" panel: beveled light-gray frame filled with a vertical gradient from light cyan-blue (top) to saturated royal blue (bottom) | (left − 45, top + 6) |

**Player preview:** the client player is rendered live, like the vanilla inventory doll, at screen point (left − 20, top + 77), scale 30. It turns to follow the mouse:

- body yaw = atan(dx / 40) × 20°
- head yaw = atan(dx / 40) × 40°
- pitch = −atan(dy / 40) × 20°

dx and dy are the mouse offsets, negated.

**Foreground text** (color 0x404040):

- The title is centered at y = 6.
- (10, 40): `"(" + (index+1) + "/" + count + "):"` for the bench item, or empty.
- (10, 50): the translated camo name of the bench item, or `techguns.item.invalidcamo` = "No Camo".
- (101 + 19·i, 38): camo index + 1 of worn armor piece i (helmet first), or 0 when the piece is absent or not camo-changeable.
- (80, 38): back-slot item's index + 1. (61, 38): face-slot item's index + 1.

**Buttons** (vanilla `GuiButtonExt`; every click sends [`GUIButtonClick`](../01-core/networking.md#0--guibuttonclick-cs) with the id):

| Id | Label | Rect (x, y, w, h) relative to GUI |
|---|---|---|
| 1 | `>` | (26, 61, 16, 16) |
| 2 | `<` | (10, 61, 16, 16) |
| 3 / 4 | `+` / `-` | (99, 59, 10, 10) / (99, 69, 10, 10) |
| 5 / 6 | `+` / `-` | (118, 59) / (118, 69) |
| 7 / 8 | `+` / `-` | (137, 59) / (137, 69) |
| 9 / 10 | `+` / `-` | (156, 59) / (156, 69) |
| (11–14) | — | commented out. They would have sat at x = 78 and x = 59, the back and face columns. |

## World rendering

`RenderTileEntityBlock` + `PaintMachineTesselatorCube` draws a cube with the quadrant layout from shared-rendering.md §2. It rotates with `rotation`.

### Texture `textures/blocks/paintmachine_textures.png`

64 × 64, no animation, 2 × 2 atlas of 32 px quadrants.

| Quadrant | Used for | Depicts / palette |
|---|---|---|
| TL | back, sides, bottom | Plain dark-gray mottled metal panel with a light-gray beveled rim (same style as the repair bench) |
| TR | front | Three upright paint canisters side by side, each with a chrome/gray cap and base and a saturated band: red, green, blue. Black background between them. |
| BL | top | Machine top: gray housing with three small square indicator lamps (red, green, blue) along the top edge. Center: a white-and-gray louverd vent grille in a light frame. Left and right: two chrome cylindrical rollers. Black gaps. |
| BR | unused | transparent |

There are four older single-face textures for the same machine: [`textures/blocks/paintmachine_top.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09), `paintmachine_front.png`, `paintmachine_side.png` and `paintmachine_bottom.png`, each 32 × 32. Nothing in the code references them. [07](../07-blocks/index.md) catalogs them.

## Sounds

None.
