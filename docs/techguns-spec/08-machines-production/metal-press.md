# Metal Press (`basicMachine:1`)

The Metal Press is a two-input press: ingots into plates, plus a handful of combination recipes. It has an optional auto-split mode.

Classes: `MetalPressTileEnt`, `MetalPressContainer`, `GuiMetalPress`, `MetalPressRecipes` (with inner class `MetalPressRecipe`).

## 1. Inventory

| Slot | Role | GUI pos | Container slot |
|---|---|---|---|
| 0 | Input A | (110,17) | `Slot` |
| 1 | Input B | (130,17) | `Slot` |
| 2 | Output | (120,60) | `SlotOutput` |
| 3 | Upgrade | (150,60) | `SlotMachineUpgrade` |

- **Sided access.** Slots 0, 1 and 2 from every side. Insert if `isItemValidForSlot`; extract only slot 2.
- **`isItemValidForSlot(slot, item)`** is true if any of these holds:
  - the slot already holds the same item (exact damage) and the total fits; or
  - slot 0 is empty, this is slot 0, and some recipe uses the item; or
  - slot 0 is filled, this is slot 1, slot 1 is empty, and (slot 0, item) forms a recipe.

  The first rule also lets automation push matching items into the **output** slot (MPRD-21).
- **Shift-click target (`getValidSlotForItemInMachine`)**, first match wins:
  1. same item as slot 0 → slot 0;
  2. same item as slot 1 → slot 1;
  3. slot 0 empty and the item is used in any recipe → slot 0;
  4. slot 0 filled, slot 1 empty, and it forms a recipe → slot 1;
  5. upgrade → slot 3;
  6. otherwise rejected.

  The container ranges are off by one (MPRD-20).
- Capacity: 20 000 RF.

## 2. Recipe model

Each `MetalPressRecipe` has `slot1` and `slot2` (`ItemStackOreDict`), `allowSwap` and `output`.

- **Match.** (A matches slot1 and B matches slot2), or, if `allowSwap`, the reverse. **Each input consumes exactly 1 item per unit**, whatever size the ingredient declares.
- **Lookup** returns the first matching recipe in list order.
- **Time** 100 ticks. **Power** 20 RF/t × multiplier (2000 RF per unit).
- **Multiplier.** Max = upgrades + 1. Pick the largest m ≥ 2 where slot 0 ≥ m, slot 1 ≥ m and output×m fits; otherwise m = 1.

### Full recipe list (`TGMachineRecipes`, all registered with allowSwap = true)

| # | Input A | Input B | Output |
|---|---|---|---|
| 1 | `ingotSteel` | Obsidian (block) ×1 | Obsidian Steel Ingot ×1 (shared 40) |
| 2 | `ingotTin` | `ingotTin` | Tin Plate ×2 (shared 41) |
| 3 | `ingotCopper` | `ingotCopper` | Copper Plate ×2 (42) |
| 4 | `ingotBronze` | `ingotBronze` | Bronze Plate ×2 (44) |
| 5 | `ingotIron` | `ingotIron` | Iron Plate ×2 (43) |
| 6 | `ingotSteel` | `ingotSteel` | Steel Plate ×2 (45) |
| 7 | `ingotLead` | `ingotLead` | Lead Plate ×2 (104) |
| 8 | `plateIron` | Flint | Mechanical Parts ×1 (30) |
| 9 | `plateObsidianSteel` | `gemQuartz` | Mechanical Parts (Obsidian Steel) ×1 (34) |
| 10 | `plateCarbon` | Blaze Rod | Mechanical Parts (Carbon) ×2 (37) |
| 11 | `plateCopper` | `plateCopper` | Copper Wire ×8 (57) |
| 12 | `fiberCarbon` | `fiberCarbon` | Carbon Plate ×2 (62) |
| 13 | `plateObsidianSteel` | TGX ×1 (60) | Advanced Bullets ×16 (22) |
| 14 | `ingotObsidianSteel` | `ingotObsidianSteel` | Obsidian Steel Plate ×2 (65) |
| 15 | `plateSteel` | `plateBronze` | Steam Armor Plating ×1 (67) |
| 16 | `ingotGold` | `ingotGold` | Gold Wire ×2 (86) |
| 17 | `plateIron` | TNT (block) | 40mm Grenade ×16 (93) |
| 18 | `ingotTitanium` | `ingotTitanium` | Titanium Plate ×2 (96) |

The shared-item numbers are metadata of `TGItems.sharedItem` (see `06-items`).

## 3. Auto-split mode

`autoSlitMode` (sic): byte 0 = off, 1 = on. NBT `autoSplitMode`.

While idle with redstone enabled:

1. **Both input slots empty:** nothing happens.
2. **Auto-split on and exactly one input slot filled:**
   - If slot 1 is empty, slot 0 has more than 1 item, and the item pressed against itself is a recipe: move floor(n/2) into slot 1 and keep ceil(n/2) in slot 0.
   - If slot 0 is empty, slot 1 has more than 1 item, and it's a self-recipe: move floor(n/2) into slot 0 and keep ceil(n/2) in slot 1.
   - Then try to start an operation.
3. **Auto-split on, both slots filled with the same item and damage, and the counts differ by more than 1:** rebalance to ceil(total/2) in slot 0 and floor(total/2) in slot 1. Then try to start.
4. **Otherwise:** try to start.

The effect is that a player can drop one stack of ingots in and both slots are fed evenly.

## 4. GUI (`GuiMetalPress`, `textures/gui/metalPress_GUI.png`)

- **Title** `techguns.container.metalpress` ("Metal Press"). Inventory label as usual.
- **Mode text.** "Stack split:" (`techguns.container.metalpress.autosplit` + ":") at (20,30), with "On" or "Off" (`techguns.container.info.on/off`) at (20,40).
- **Button.** ID 3 at (20,50), 40×20, labelled `techguns.button.switch` ("Switch"). It toggles auto-split and requires access.
- **Upgrade slot frame.** Copied from u119 v59, 20×20, drawn at (149,59).
- **Progress.** Sprite u176 v0, **20** wide × (progress·21/total + 1) tall, at (119,36). Fills top-down.
- **Container sync:** 0 progress, 1 totalTime, 2 power (raw RF), 3 auto-split mode.
- The NEI GUI offset isn't registered for this GUI (the call is commented out).

## 5. Sounds

`techguns:machines.metalpressWork` at progress tick 8 and tick 58: round(7.5% of total), and that + round(half). Both sides, volume 1, pitch 1. The event `machines.metalpressLargeWork` exists in sounds.json but is never played.

## 6. Visuals

TESR `RenderAmmoPress` (shared class) with `ModelMetalPress` (declared 128×64) and texture `textures/blocks/MetalPress.png` (256×128, catalogued in 07-blocks).

- **Look.** A grey steel hydraulic press: a base table, columns and a heavy top block. The texture is mostly grey/silver weathered metal with a hazard-striped strip.
- **Animation.**
  - Working: the `MetalPiece` blank shows on the table, and the press head (parts `p1..p6`) moves vertically by 0.25·y blocks, where y = 0.5 + sin((√(2q)+0.75)·2π)/2 and q = p mod 0.5. That's **two strokes per operation**; y starts and ends each stroke at 0.
  - Idle: the head is at rest and there's no blank.
