# GUI handler and shared GUI classes

## 1. `TechgunsGuiHandler`: complete GUI ID table

Registered with `NetworkRegistry.registerGuiHandler(Techguns.instance, …)`. Both sides take the tile entity at (x, y, z) and cast it to the expected class.

| ID | Server container | Client GUI | Tile / source | Opened from | Area |
|---|---|---|---|---|---|
| 0 | `AmmoPressContainer` | `GuiAmmoPress` | `AmmoPressTileEnt` | `basicMachine:0` | 08 |
| 1 | `MetalPressContainer` | `GuiMetalPress` | `MetalPressTileEnt` | `basicMachine:1` | 08 |
| 2 | `TurretBaseContainer` | `GuiTurretBase` | `TurretBaseEnt` | `basicMachine:2` | 09 |
| 3 | `CamoBenchContainer` | `GuiCamoBench` | `CamoBenchTileEnt` | `basicMachine:3` | 09 |
| 4 | `ChemLabContainer` | `GuiChemLab` | `ChemLabTileEnt` | `basicMachine:4` | 08 |
| 5 | `RepairBenchContainer` | `GuiRepairBench` | `RepairBenchTileEnt` | `basicMachine:5` | 09 |
| 6 | `ExplosiveChargeContainer` | `GuiExplosiveCharge` | `ExplosiveChargeTileEnt` (both charge types) | `basicMachine:6/8` (unarmed only) | 09 |
| 9 | `ChargingStationContainer` | `GuiChargingStation` | `ChargingStationTileEnt` | `basicMachine:9` | 08 |
| 10 | `ScrapperContainer` | `GuiScrapper` | `ScrapperTileEnt` | never (scrapper block disabled) | 09 |
| 16 | `OreDrillContainer` | `GuiOreDrill` | `OreDrillTileEntMaster` | ore drill blocks (master or slave → master position) | 09 |
| 17 | `TGPlayerContainer` | `GuiTGPlayerInventory` | player's Techguns extended inventory (no tile) | inventory tab button (`PacketOpenPlayerGUI`) | 01/05 |
| 18 | `ReactionChamberContainer` | `GuiReactionChamber` | `ReactionChamberTileEntMaster` | RC controller or linked RC slave | 08 |
| 19 | `FabricatorContainer` | `GuiFabricator` | `FabricatorTileEntMaster` | Fabricator controller or linked slave | 08 |
| 20 | `TGChestContainer` | `GuiTechgunsChest` | `BlockTGChest` inventory (single or double) | reinforced / weapon chest (`BlockTGChest.GUI_ID_TGCHEST`) | 09 |

- IDs 7, 8 and 11–15 are unused.
- Basic machines use **meta = GUI ID**.
- Unknown IDs return null on both sides.
- ID 17 is hard-coded here, but the tab button sends the configurable `TechgunsGUI_TabID` (category "ID Conflicts", default 17). See MPRD-29.

## 2. `GuiTechgunsTileEntBase` (abstract `GuiContainer`)

- `drawTooltipText(String or String[], x, y)` draws a vanilla hovering-text tooltip with GL enable and lighting state saved and restored.
- `isInRect(mx, my, x, y, w, h)` is an inclusive rectangle test in GUI-relative coordinates.
- `trans(key)` translates `"techguns." + key`.

## 3. `GuiTechgunsPoweredTileEnt` (base GUI for every powered machine)

The size is the vanilla default, 176×166. Machine GUIs draw their own background and call `super` **last**, so these elements draw on top of the machine background.

| Element | Position (GUI-relative) | Texture source (`textures/gui/ammoPress_GUI.png`) | Behaviour |
|---|---|---|---|
| Power bar | x 8, y 17, 4×48 | u 251, v 1, 4×48 gradient (green top → yellow → red bottom) | Fills **bottom-up**. Filled height = stored·48/max; drawn from y 17+(48−h) using v 1+(48−h). The empty background is part of each machine's own texture. |
| Power tooltip | rect (7,17) 5×49 | none | "Power: \<stored>/\<max> RF" (`techguns.container.power`) |
| Redstone lamps | (6,5) and (11,5), 5×5 each | left lamp u240 v0 (off) / u240 v5 (red); right lamp u245 v5 (green) / u245 v0 (off) | Enabled: left off, right green. Disabled: left red, right off. |
| Redstone tab panel | (−27,5), 27×30 | u195 v0, 27×30 (rounded side tab) | backdrop for the redstone button |
| Security tab panel | (−27,35), 27×30 | same sprite | backdrop for the security button |
| Redstone button (ID 2) | (−22,10), 20×20 | vanilla button background plus icon u234 v(10+16·mode), 16×16 at +2,+2 | icon by mode: crossed-out dust (ignore), glowing dust (high), dark dust (low) |
| Security button (ID 1) | (−22,40), 20×20 | vanilla button background plus icon u234 v(58+16·level) | green open padlock (0), yellow padlock (1), red padlock (2) |
| Redstone tooltip | lamps rect (6,5) 10×5, or button rect (−22,10) 20×20 | none | "Redstone Info" / "Required: \<mode>" / "Signal: \<high/low>" |
| Security tooltip | rect (−22,40) 20×20 | none | "Owned by: \<name or Unowned…>" / "Security: \<colour>\<Public/Friends/Owner>" (green/yellow/red) / description for the level / "Only the owner can change this setting" |

- `actionPerformed` sends `GUIButtonClick(tile, button.id)` for **every** button, including the machine-specific ones.
- **Fluid-tank helper `drawFluidWithTesselator(icon, x, y, w, h, px)`.** Tiles the fluid's block icon upward from the bottom of an (x, y, w, h) box until `px` pixels are filled. Each segment is at most one icon tall and is cropped from the icon's top when partial. Bind the block atlas before calling it.
- The power texture is always `textures/gui/ammoPress_GUI.png`, whatever the machine. The redstone and security button classes bind the same file.

## 4. Buttons

| Class | Base | Visual |
|---|---|---|
| `GuiButtonRedstoneState` | Forge `GuiButtonExt` | Draws the vanilla button frame from the widgets texture, cut to any size with a 2/3/2/2 border, hover state included. Then binds `ammoPress_GUI.png` and draws the 16×16 redstone-mode icon at +2,+2. No text. |
| `GuiButtonSecurity` | `GuiButtonExt` | Same frame. Icon u234 v(58+16·security). No text. Also used by the TG chest (09). |
| `GuiButtonToggleDrainTank` | `GuiButtonExt` | Same frame, 14×14. Binds `chemLab_Gui.png` and draws a 12×12 icon from u188 v(31+12·drainMode) at +1,+1. Mode 0 = output tank drained, 1 = input tank drained. |

## 5. Slots

| Class | Rule |
|---|---|
| `SlotOutput` | `isItemValid` always false (take-only) |
| `SlotMachineUpgrade` | accepts only shared item meta 52 (Machine Upgrade); stack limit 7 |
| `SlotRestrictedToList` | accepts an item if any `ItemStackOreDict` in the given list matches it (`isEqualWithOreDict`) |

All machine containers add the player inventory at the vanilla positions:
- main inventory: (8+18·col, 84+18·row);
- hotbar: (8+18·col, 142).

## 6. `ItemStackOreDict` (recipe ingredient wrapper)

An ingredient is exactly one of:
- an ore dictionary name plus a stack size;
- a concrete `ItemStack` plus a stack size;
- a "null stack" (neither set), which means the slot **must be empty**.

| Member | Behaviour |
|---|---|
| `strict` (default false) | When false, a `WILDCARD_VALUE` damage on the recipe side matches any damage. When true, damage must match exactly. Forge `itemMatches` semantics. |
| `isEqualWithOreDict(stack)` | Item form: compare against the item (null stack matches only null). Ore form: true if any registered ore stack matches. **Stack size is not checked.** |
| `getItemStacks()` | Display stacks at `stackSize`. For the ore form it **mutates the shared ore dictionary stacks' sizes** (MPRD-23). |
| `getItemStacks(n)` | Fresh copies with size n |
| `matches(other)` | Equality between two ingredients. Both ore form: names equal. One ore form: test the other's item. Neither: `itemMatches` (crashes when both are null stacks, see MPRD-25). |
| `isNullStack()` | neither name nor item |
| `toString()` | `ItemStackOreDict:<name>`, the item's `toString`, or `EMPTY_ItemStackOreDict` |
