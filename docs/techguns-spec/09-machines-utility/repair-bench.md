# Repair Bench

`BasicMachine` meta 5. Tile entity `RepairBenchTileEnt`, registered as `RepairBenchTileEnt`. GUI id 5. Container `RepairBenchContainer`, GUI `GuiRepairBench`, button `GuiButtonRepair`. TESR `RenderTileEntityBlock` with `RepairBenchTesselatorCube`.

## Purpose

The bench is an unpowered workbench that fully repairs the Techguns armour the player is **wearing**. The player stocks it with repair materials, then clicks one of four per-armour-slot repair buttons.

## Tile entity

- Extends `BasicInventoryTileEnt` with **9** slots, all general storage. Stack limit is 64.
- Any item is valid in any slot.
- **Automation:** every side exposes slots 0–8. Insertion is allowed into any slot; extraction is never allowed.
- Inventory name key: `techguns.container.repairbench` = "Repair Bench".
- **NBT:** `Items` (list of `{Slot: byte, item}`), `rotation` (byte), optional `CustomName`. These come from the base class; the bench adds nothing.
- It has no tick logic.

### Repair action (`buttonClicked`, server)

Handles button ids 1–4, and only if the player passes `isUseableByPlayer`: same tile at the position and within 8 blocks (distance² ≤ 64).

1. Map the button to a worn armour slot: button 1 → helmet, 2 → chestplate, 3 → leggings, 4 → boots. Internally, armour-inventory index = 3 − (id − 1).
2. If that slot holds a `GenericArmor` (see 05-armor), ask the armour for its **repair material list**. That list is computed by the armour from its current damage (formula summarised below; 05 owns it).
3. If the list is non-empty and the bench's 9 slots together hold **every** listed stack, then:
   1. Consume all of them. Matching is by item and damage value and may span several slots.
   2. Set the armour's damage to 0, which is a full repair.
4. Otherwise nothing happens. There is no partial repair and no feedback message.

The armour's `canRepairOnRepairBench` is not checked on the server. An armour with no repair materials returns an empty list, so the effect is the same.

**Repair-material formula** (from `GenericArmor`, for reference only; see 05-armor):

- damage fraction = damage ÷ (maxDamage − 1)
- total = ceil(repairMatCount × fraction)
- metal count = ceil(total × metalRatio)
- cloth count = total − metal count
- Each of the two entries appears only if its material is set and its count > 0.

The per-armour material pairs, ratios and counts are listed in 05-armor.

## Container layout (`RepairBenchContainer`)

| Container index | Inventory | Slot type | Position (x, y) |
|---|---|---|---|
| 0–8 | bench 0–8 | plain `Slot` | (8 + 18·i, 57), one row |
| 9–35 | player main 9–35 | plain | (8 + 18·col, 84 + 18·row) |
| 36–44 | player hotbar 0–8 | plain | (8 + 18·i, 142) |
| 45–48 | player armour (index 39 − i, so 45 = helmet … 48 = boots) | `SlotArmor` (stack limit 1, accepts only armour valid for that type, shows vanilla empty-armour background icons) | (89 + 20·i, 18) |

Shift-click rules:

| From | To |
|---|---|
| Bench slot (0–8) | Player inventory 9–44 |
| Player slot | If `ItemArmor`, the matching armour slot 45 + armourType. Otherwise bench slots 0–8. |
| Armour slot | Player inventory, filled from the end |

Container index 9 is excluded from the player-slot range by an off-by-one, so shift-clicking the top-left main-inventory slot does nothing (MUTL-05).

## GUI (`GuiRepairBench`)

The GUI extends `GuiTechgunsTileEntBase` and is not powered. Size is the default 176 × 166. The background is `textures/gui/repairBench_GUI.png` at u,v (0,0), size 176 × 166. The file is catalogued by 08.

How this GUI uses the texture:

| Texture region (px) | Content | Use |
|---|---|---|
| (0,0) 176×166 | Standard light-grey panel with bevelled border. Top-right: a raised frame (~(85,14)–(169,37)) containing 4 slot wells. Middle: a row of 9 slot wells at y ≈ 56–74. Bottom: 3×9 player inventory and the hotbar. | Background |
| (176,0) 12×12 | Small dark-grey wrench icon, diagonal, head at top-right | Drawn on each repair button |

Foreground text (colour 0x404040):

- The title is centred at y = 6. It shows the custom name, or the translated `techguns.container.repairbench`.
- `container.inventory` ("Inventory") is drawn at (8, ySize − 92), which is y = 74.

**Buttons:** four `GuiButtonRepair` controls, ids 1–4, each 14 × 14, at (90 + 20·i, 39) for i = 0..3. That places each one directly under the matching armour slot, helmet first. Each draws:

1. The vanilla button background from `textures/gui/widgets.png`, as a 9-slice continuous box; its hover state follows the vanilla button.
2. The wrench icon from (176,0), size 12 × 12, at button offset (1,1).

A click sends `GUIButtonClick` (tile position + button id; packet owned by 01) to the server.

**Hover tooltip** over each button area (90 + 20·i, 39, 14 × 14) describes the armour worn in the matching slot:

| Condition | Tooltip |
|---|---|
| Nothing worn | `techguns.repairBench.noarmor` = "No armor" |
| Not a `GenericArmor`, or `canRepairOnRepairBench` false (repairMatCount = 0) | `techguns.repairBench.cantrepair` = "Can't repair this armor" |
| Damage = 0 | `techguns.repairBench.fullcondition` = "Armor is fully repaired" |
| Damaged | First line `techguns.repairBench.requiredMats` = "Required materials for repair:", then one line per material: `<count>x <translated item name>`. A line is prefixed with §c (red) when the bench does not contain enough of that material. |

## World rendering

`RenderTileEntityBlock` + `RepairBenchTesselatorCube` draws a full cube using the quadrant atlas (see shared-rendering.md §2). The block rotates in 90° steps with `rotation`; a wrench cycles it.

### Texture `textures/blocks/repairbench_textures.png`

64 × 64, no animation, 2 × 2 atlas of 32 px quadrants.

| Quadrant (px) | Used for | Depicts / palette |
|---|---|---|
| TL (0,0)–(32,32) | back, left, right, bottom | Plain dark-grey brushed/scratched metal panel with a lighter grey bevelled rim (greys #4a–#9a) |
| TR (32,0)–(64,32) | front | Wooden wall shelving with a grey frame and two shelves. Top shelf: three glass jars (amber/orange, pale blue, lime green) with grey lids. Bottom shelf: a circular saw blade (grey with an orange hub cross) and a red-handled screwdriver lying flat. Dark brown back wall. |
| BL (0,32)–(32,64) | top | Brown wooden pegboard / tool tray with a grey rim. Holds a small hammer, a red cordless drill (top right), a beige-handled mallet, a hand saw with a black handle, two small wrenches/pins and a spanner. Light browns with grey/white tools and red accents. |
| BR | unused | transparent |

The same texture and cube are used for the item form of meta 5, and also, by mistake, for metas 7 and 10 (see shared-rendering.md).

## Sounds

None.
