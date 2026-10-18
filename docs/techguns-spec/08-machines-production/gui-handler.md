# GUI handler and shared GUI classes

## 1. `TechgunsGuiHandler`: complete GUI ID table

Registered with `NetworkRegistry.registerGuiHandler(Techguns.instance, …)`. Both sides take the tile entity at (x, y, z) and cast it to the expected class.

| ID | Server container | Client GUI | Tile / source | Opened from | Area |
|---|---|---|---|---|---|
| 0 | [`AmmoPressContainer`](ammo-press.md) | [`GuiAmmoPress`](ammo-press.md#5-gui-guiammopress-texture-texturesguiammopress_guipng) | [`AmmoPressTileEnt`](framework.md#1-class-hierarchy) | `basicMachine:0` | 08 |
| 1 | [`MetalPressContainer`](metal-press.md) | [`GuiMetalPress`](metal-press.md#4-gui-guimetalpress-texturesguimetalpress_guipng) | [`MetalPressTileEnt`](framework.md#1-class-hierarchy) | `basicMachine:1` | 08 |
| 2 | [`TurretBaseContainer`](../09-machines-utility/turret.md#container-turretbasecontainer) | [`GuiTurretBase`](../09-machines-utility/turret.md#gui-guiturretbase) | [`TurretBaseEnt`](../09-machines-utility/turret.md#tile-entity-turretbaseent) | `basicMachine:2` | [09](../09-machines-utility/index.md) |
| 3 | [`CamoBenchContainer`](../09-machines-utility/camo-bench.md#container-layout-camobenchcontainer) | [`GuiCamoBench`](../09-machines-utility/camo-bench.md#gui-guicamobench) | [`CamoBenchTileEnt`](blocks-and-items.md#1-basicmachine-registry-name-basicmachine) | `basicMachine:3` | [09](../09-machines-utility/index.md) |
| 4 | [`ChemLabContainer`](chem-lab.md) | [`GuiChemLab`](chem-lab.md#5-gui-guichemlab-texturesguichemlab_guipng) | [`ChemLabTileEnt`](framework.md#1-class-hierarchy) | `basicMachine:4` | 08 |
| 5 | [`RepairBenchContainer`](../09-machines-utility/repair-bench.md#container-layout-repairbenchcontainer) | [`GuiRepairBench`](../09-machines-utility/repair-bench.md#gui-guirepairbench) | [`RepairBenchTileEnt`](blocks-and-items.md#1-basicmachine-registry-name-basicmachine) | `basicMachine:5` | [09](../09-machines-utility/index.md) |
| 6 | `ExplosiveChargeContainer` | [`GuiExplosiveCharge`](gui-textures.md#10-texturesguiexpchargeguipng-owned-by-09) | [`ExplosiveChargeTileEnt`](../09-machines-utility/explosive-charges.md#nbt-explosivechargetileent) (both charge types) | `basicMachine:6/8` (unarmed only) | [09](../09-machines-utility/index.md) |
| 9 | [`ChargingStationContainer`](charging-station.md) | [`GuiChargingStation`](charging-station.md#3-gui-guichargingstation-texturesguichargingstation_guipng) | [`ChargingStationTileEnt`](framework.md#1-class-hierarchy) | `basicMachine:9` | 08 |
| 10 | [`ScrapperContainer`](../09-machines-utility/scrapper.md#container-scrappercontainer) | [`GuiScrapper`](../09-machines-utility/scrapper.md#gui-guiscrapper) | [`ScrapperTileEnt`](../09-machines-utility/scrapper.md) | never (scrapper block disabled) | [09](../09-machines-utility/index.md) |
| 16 | [`OreDrillContainer`](../09-machines-utility/ore-drill.md#container-slots-oredrillcontainer) | [`GuiOreDrill`](../09-machines-utility/ore-drill.md#7-gui-guioredrill-id-16) | [`OreDrillTileEntMaster`](../09-machines-utility/ore-drill.md#5-operation-oredrilltileentmaster) | ore drill blocks (master or slave → master position) | [09](../09-machines-utility/index.md) |
| 17 | [`TGPlayerContainer`](../05-armor/extra-inventory.md#4-container-tgplayercontainer) | [`GuiTGPlayerInventory`](../05-armor/extra-inventory.md#5-gui-guitgplayerinventory) | player's Techguns extended inventory (no tile) | inventory tab button ([`PacketOpenPlayerGUI`](../01-core/networking.md#4--packetopenplayergui-cs)) | [01](../01-core/index.md)/[05](../05-armor/index.md) |
| 18 | [`ReactionChamberContainer`](reaction-chamber.md) | [`GuiReactionChamber`](reaction-chamber.md#6-gui-guireactionchamber-texturesguireactionchamber_guipng) | [`ReactionChamberTileEntMaster`](framework.md#1-class-hierarchy) | RC controller or linked RC slave | 08 |
| 19 | [`FabricatorContainer`](fabricator.md) | [`GuiFabricator`](fabricator.md#5-gui-guifabricator-texturesguifabricator_guipng) | [`FabricatorTileEntMaster`](framework.md#1-class-hierarchy) | Fabricator controller or linked slave | 08 |
| 20 | [`TGChestContainer`](../09-machines-utility/misc-tile-entities.md#container-tgchestcontainer) | [`GuiTechgunsChest`](../09-machines-utility/misc-tile-entities.md#gui-guitechgunschest) | [`BlockTGChest`](../07-blocks/special-blocks.md#9-reinforced-chest-and-weapon-chest-tgchest-tgchest_weapon-blocktgchest) inventory (single or double) | reinforced / weapon chest (`BlockTGChest.GUI_ID_TGCHEST`) | [09](../09-machines-utility/index.md) |

- IDs 7, 8 and 11–15 are unused.
- Basic machines use **meta = GUI ID**.
- Unknown IDs return null on both sides.
- ID 17 is hard-coded here, but the tab button sends the configurable [`TechgunsGUI_TabID`](../01-core/config.md#category-id-conflicts) (category "ID Conflicts", default 17). See [MPRD-29](index.md#suspected-bugs).

## 2. `GuiTechgunsTileEntBase` (abstract `GuiContainer`)

- `drawTooltipText(String or String[], x, y)` draws a vanilla hovering-text tooltip with GL enable and lighting state saved and restored.
- `isInRect(mx, my, x, y, w, h)` is an inclusive rectangle test in GUI-relative coordinates.
- `trans(key)` translates `"techguns." + key`.

## 3. `GuiTechgunsPoweredTileEnt` (base GUI for every powered machine)

The size is the vanilla default, 176×166. Machine GUIs draw their own background and call `super` **last**, so these elements draw on top of the machine background.

| Element | Position (GUI-relative) | Texture source ([`textures/gui/ammoPress_GUI.png`](gui-textures.md#1-texturesguiammopress_guipng)) | Behaviour |
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
| `GuiButtonSecurity` | `GuiButtonExt` | Same frame. Icon u234 v(58+16·security). No text. Also used by the TG chest ([09](../09-machines-utility/index.md)). |
| [`GuiButtonToggleDrainTank`](chem-lab.md) | `GuiButtonExt` | Same frame, 14×14. Binds `chemLab_Gui.png` and draws a 12×12 icon from u188 v(31+12·drainMode) at +1,+1. Mode 0 = output tank drained, 1 = input tank drained. |

## 5. Slots

| Class | Rule |
|---|---|
| [`SlotOutput`](ammo-press.md#1-inventory) | `isItemValid` always false (take-only) |
| [`SlotMachineUpgrade`](ammo-press.md#1-inventory) | accepts only shared item meta 52 (Machine Upgrade); stack limit 7 |
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
| `getItemStacks()` | Display stacks at `stackSize`. For the ore form it **mutates the shared ore dictionary stacks' sizes** ([MPRD-23](index.md#suspected-bugs)). |
| `getItemStacks(n)` | Fresh copies with size n |
| `matches(other)` | Equality between two ingredients. Both ore form: names equal. One ore form: test the other's item. Neither: `itemMatches` (crashes when both are null stacks, see [MPRD-25](index.md#suspected-bugs)). |
| `isNullStack()` | neither name nor item |
| `toString()` | `ItemStackOreDict:<name>`, the item's `toString`, or `EMPTY_ItemStackOreDict` |
