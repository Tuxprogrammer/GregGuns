# GUI textures (`textures/gui/`, all 14)

## Shared style

Every file is a 256×256 RGBA PNG. None has an `.mcmeta` and none is animated. All except the explosive-charge backgrounds use the **vanilla container style**:

| Element | Colour |
|---|---|
| Panel body | #C6C6C6 light grey |
| Bevel highlight | white (#FFFFFF) on top/left |
| Bevel shadow | #555555 / #373737 on bottom/right |
| Outline | 1 px black |
| Slot wells | #8B8B8B with dark top/left and white bottom/right edges |

**Main panel and player inventory.** The main panel is the region (0,0)–(175,165) (176×166) unless noted. In every container texture the player inventory is printed at the vanilla positions:
- 3×9 grid with slot origins (8+18c, 84+18r) (the 18×18 frames start 1 px earlier);
- hotbar at y 142;
- label area around y 72.

**Sprite area.** The space to the right of x 176 (and below y 166) holds sprites copied by code. Everything not listed as used is transparent.

The coordinate notation is (u, v) for the source rectangle and (x, y) for where it's drawn, relative to the GUI's top-left.

## 1. `textures/gui/ammoPress_GUI.png`

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #A4A4A2; dominant #D0D0D0 46%, #909090 33%, #F0F0F0 7%; 48% opaque.
- **Look:** the vanilla light-grey container panel with three slot wells in a row on the right, a funnel-shaped pipe outline down to a single output well, a slim power trough at the left; in the sprite column, a coloured strip of icons (lamps, redstone-dust and padlock icons) and a green-to-red power gradient bar.

- **Used by** [`GuiAmmoPress`](ammo-press.md#5-gui-guiammopress-texture-texturesguiammopress_guipng), [`GuiScrapper`](../09-machines-utility/scrapper.md#gui-guiscrapper) ([09](../09-machines-utility/index.md)), and **every** powered-machine GUI for the power bar, redstone and security sprites ([`GuiTechgunsPoweredTileEnt`](gui-handler.md#3-guitechgunspoweredtileent-base-gui-for-every-powered-machine), [`GuiButtonRedstoneState`](gui-handler.md#4-buttons), [`GuiButtonSecurity`](gui-handler.md#4-buttons)). [`GuiTechgunsChest`](../09-machines-utility/misc-tile-entities.md#gui-guitechgunschest) ([09](../09-machines-utility/index.md)) for its security button. Also used by the NEI handlers.
- **Main panel:**
  - power-bar well at (7,16)–(12,66), a sunken 6×51 trough;
  - three input slot frames at (99,16), (119,16) and (139,16);
  - a vertical funnel/pipe outline from the middle input down to the output, about (122,35)–(134,57);
  - the output slot at (119,59).

  There's no upgrade-slot frame; code copies the output frame (u119 v59, 20×20) to (151,59).

| Sprite | u,v | Size | Content | Use |
|---|---|---|---|---|
| Progress funnel | 176,0 | 19×21 (18 wide visible) | light-grey pipe with ladder-like ticks widening into a nozzle | Ammo Press / Metal Press / Scrapper progress, filled top-down at (119,36) |
| Side tab | 195,0 | 36×30 (27×30 used) | raised grey rounded panel with black outline, open on the right | backdrop behind the side buttons at (−27,5) / (−27,35) |
| Lamp off (left) | 240,0 | 5×5 | dark grey square, black dot | redstone indicator |
| Lamp off (right) | 245,0 | 5×5 | dark grey | redstone indicator |
| Lamp red | 240,5 | 5×5 | red | "disabled" |
| Lamp green | 245,5 | 5×5 | green | "enabled" |
| Redstone: ignore | 234,10 | 16×16 | redstone-dust pile crossed out with a black X | redstone button, mode 0 |
| Redstone: high | 234,26 | 16×16 | bright red redstone dust | mode 1 |
| Redstone: low | 234,42 | 16×16 | dark grey/black dust | mode 2 |
| Security public | 234,58 | 16×16 | black padlock, **open** shackle, green outline | security 0 |
| Security friends | 234,74 | 16×16 | closed padlock, yellow outline | security 1 |
| Security owner | 234,90 | 16×16 | closed padlock, red outline | security 2 |
| Power gradient | 250,0–255,50 (code reads u251 v1, 4×48) | 6×51 | vertical gradient: green (top) → yellow → orange → red (bottom), white frame | power bar fill, bottom-up |

## 2. `textures/gui/metalPress_GUI.png`

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #A7A7A7; dominant #D0D0D0 48%, #909090 35%, #F0F0F0 6%; 45% opaque.
- **Look:** the same grey panel as the Ammo Press but with two input wells over the funnel and output; the only sprite is the grey progress funnel.

- **Used by** [`GuiMetalPress`](metal-press.md#4-gui-guimetalpress-texturesguimetalpress_guipng) and the Metal Press NEI handler.
- **Main panel:** the power-bar well as above; two input frames at (109,16) and (129,16); a funnel/pipe outline down to the output frame at (119,59).
- **Sprites:** only the progress funnel at u176 v0 (19–20×21), identical to the ammo press one. The upgrade frame is copied from (119,59), 20×20, to (149,59).

## 3. `textures/gui/chemLab_Gui.png`

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #A2A2A2; dominant #D0D0D0 41%, #909090 37%, #F0F0F0 7%; 47% opaque.
- **Look:** grey panel framed by two tall graduated tank wells (left input, right output), two input wells and a bottle-silhouette well on the left, a grey line drawing of lab glassware in the middle; the sprite column holds the same glassware in saturated blue/cyan/green/orange/red, a tank gauge and two valve icons.

- **Used by** [`GuiChemLab`](chem-lab.md#5-gui-guichemlab-texturesguichemlab_guipng), [`GuiButtonToggleDrainTank`](chem-lab.md), the Chem Lab NEI handler.
- **Main panel:**
  - power well at (7,16);
  - left tank well at (18,16), 12×52;
  - input frames at (34,16) and (56,16);
  - the flask slot at (34,39), with a grey glass-bottle silhouette printed in it;
  - a grey line-art illustration of a lab apparatus in the centre: a funnel at about (80,18), a small beaker at (80,32), a round-bottom flask at (96,23), a distillation arm and an Erlenmeyer flask at (116,28);
  - the output frame at (134,16);
  - the right tank well at (157,16).

  Code copies the upgrade frame from (134,16) to (134,46).

| Sprite | u,v | Size | Content | Use |
|---|---|---|---|---|
| Coloured apparatus | 176,0 to ~231,30 | about 56×30 | a full-colour version of the centre illustration, used as 5 progress stages: blue funnel and drips (178,5 8×25), cyan pipe (186,25 9×1), green round flask (194,9 14×20), orange distillation tube (205,3 16×6), red Erlenmeyer (214,6 14×25) | progress animation |
| Tank gauge | 176,32 | 12×52 | transparent window with dark tick marks on the left (long every 10 px) and a red line at the top | overlay on both tanks |
| Drain icon: output | 188,31 | 12×12 | valve symbol, blue body with red/orange arrow | drain toggle, mode 0 |
| Drain icon: input | 188,43 | 12×12 | the same valve with mirrored colours | drain toggle, mode 1 |

## 4. `textures/gui/Fabricator_Gui.png`

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #A4A4A3; dominant #D0D0D0 43%, #909090 36%, #F0F0F0 7%; 45% opaque.
- **Look:** grey panel with a row of four input wells (three with dark ghost icons), a long right-pointing arrow outline across the middle and one output well; a golden gradient arrow below the panel.

- **Used by** [`GuiFabricator`](fabricator.md#5-gui-guifabricator-texturesguifabricator_guipng) and the Fabricator NEI handler.
- **Main panel:**
  - power well at (7,16);
  - the input frame at (18,16);
  - the wire frame at (46,16) with a dark coiled-wire silhouette;
  - the powder frame at (67,16) with a dust-pile silhouette;
  - the plate frame at (88,16) with a square-plate silhouette (lighter inset);
  - a long horizontal arrow outline, about (20,52)–(110,63), pointing right at the output frame at (115,49).

  Code copies the upgrade frame from (7,83) to (149,49).
- **Sprite:** u0 v167, 91×11. A golden gradient arrow, dark olive at the left end brightening to pale yellow in the middle and darker gold at the tip, with a white underline. It's the progress fill, left→right at (21,53).

## 5. `textures/gui/ReactionChamber_Gui.png`

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #9FA099; dominant #909090 39%, #D0D0D0 35%, #F0F0F0 8%; 47% opaque.
- **Look:** grey panel with a graduated tank on the left, input and focus wells, a vertical intensity scale, a 2×2 output block and two long tick-marked bar wells along the bottom; sprites in light green, orange, red and yellow (bars, warning triangles).

- **Used by** [`GuiReactionChamber`](reaction-chamber.md#6-gui-guireactionchamber-texturesguireactionchamber_guipng) and the RC NEI handler.
- **Main panel:**
  - power well at (7,16);
  - the tank well at (18,16);
  - the input frame at (34,16);
  - the focus frame at (92,16);
  - a vertical intensity scale at (113,16), 6×41, with tick marks;
  - a 2×2 output block at (133,16);
  - two long horizontal bar wells at (66,60) and (66,68), about 102×6, each with dot ticks every 10 px and a centre marker;
  - a small green/red "I"-shaped marker pair at the right end of the bars, about (164,58)–(168,75).
- **Sprites:**

| Sprite | u,v | Size | Content | Use |
|---|---|---|---|---|
| Small level bars | 176,0 and 182,0 | 5×20 each | short green and orange graduated bars | not referenced by code |
| Intensity fill, OK | 190,0 | 5×40 | green bar with tick marks | intensity bar, bottom-up |
| Intensity fill, off-target | 198,0 | 5×40 | orange bar with ticks | intensity bar while mismatched |
| Preferred marker | 178,22 | 5×3 | small red arrow/pointer | preferred-intensity marker |
| Tank gauge | 176,32 | 12×52 | tick-marked window with red top line (as in the Chem Lab) | tank overlay; v32 row also used as the 7×1 level marker |
| Danger icons | 205,0 / 205,12 / 205,24 / 205,36 | 12×12 each | warning triangles with "!" in green, yellow, orange and red | NEI risk icon |
| Completion bar | 0,167 | 105×6 | light green bar with black dot ticks every 10 px and a centre mark | completion progress |
| Duration bar | 0,174 (GUI reads v175) | 105×6 | orange bar with dot ticks | duration progress |

## 6. `textures/gui/ChargingStation_Gui.png`

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #A7A7A7; dominant #D0D0D0 49%, #909090 34%, #F0F0F0 6%; 44% opaque.
- **Look:** a mostly empty grey panel with one input well, a grey zig-zag lightning arrow and one output well; a red-orange-yellow lightning arrow sprite below the panel.

- **Used by** [`GuiChargingStation`](charging-station.md#3-gui-guichargingstation-texturesguichargingstation_guipng) and its NEI handler.
- **Main panel:** power well; the input frame at (18,16); a grey zig-zag **lightning-bolt arrow** outline at about (37,18)–(65,30); the output frame at (67,16). Code copies the upgrade frame from (7,83) to (149,49).
- **Sprite:** u0 v167, about 30×10. A lightning arrow in a red → orange → yellow gradient. Progress fill, left→right at (39,19).

## 7. `textures/gui/camoBench_GUI.png` (owned by 09)

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #969EAB; dominant #D0D0D0 38%, #909090 34%, #F0F0F0 8%; 50% opaque.
- **Look:** grey panel with a framed single well on the left and a raised frame of six wells with small tabs; a large vivid-blue gradient preview panel in the sprite column (it pulls the average colour towards blue).

- **Used by** [`GuiCamoBench`](../09-machines-utility/camo-bench.md#gui-guicamobench).
- **Main panel:**
  - one framed slot at (17,14) (camo target);
  - a raised frame holding 6 slot wells at (54,16)–(171,35), with small square tabs under each;
  - per the container: armor slots at (96+19i, 18), back slot (75,18), face slot (56,18).
- **Sprite:** u178 v0, 45×84. A tall preview panel with a vivid blue vertical gradient (#0090F0 top → #0030F0 bottom) inside a light bevelled frame. It's drawn at (−45,6), to the left of the GUI, as the camo preview backdrop.

## 8. `textures/gui/repairBench_GUI.png` (owned by 09)

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #A1A1A1; dominant #909090 43%, #D0D0D0 38%, #F0F0F0 8%; 44% opaque.
- **Look:** grey panel with a raised four-well armour frame at the top right and a full 9-well row above the inventory; one small dark wrench icon sprite.

- **Used by** [`GuiRepairBench`](../09-machines-utility/repair-bench.md#gui-guirepairbench) and [`GuiButtonRepair`](../09-machines-utility/repair-bench.md).
- **Main panel:** a raised frame with 4 armor slot wells at (89+20i, 18); a row of 9 slot wells at (8+18i, 57); the player inventory.
- **Sprite:** u176 v0, 12×12. A dark-grey wrench icon drawn on the repair buttons (14×14 at (90+20i, 39)).

## 9. `textures/gui/oreDrill_GUI.png` (owned by 09)

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #9F9E9E; dominant #909090 44%, #D0D0D0 34%, #F0F0F0 9%; 46% opaque.
- **Look:** grey panel with two graduated tank wells, a pipe-linked fuel/input well pair, a large inverted drill-cone outline and a 3×3 output grid; sprites: a striped grey cone, a small flame gradient and a tank gauge.

- **Used by** [`GuiOreDrill`](../09-machines-utility/ore-drill.md#7-gui-guioredrill-id-16).
- **Main panel:**
  - power well;
  - a left tank well at (15,16) and a right tank well at (157,16);
  - fuel and input slots at (30,17) and (30,51), linked by a pipe shape, with a flame silhouette in the lower slot;
  - a large inverted-triangle drill-bit outline at (72,16)–(100,55);
  - a 3×3 output grid at (102+18i, 17+18j).

| Sprite | u,v | Size | Content | Use |
|---|---|---|---|---|
| Drill cone fill | 177,1 | 25×~38 | light-grey striped inverted cone (drill bit) | progress fill, top-down at (73,17) |
| Flame / heat | 188,39 | 6×12 | vertical red → orange → yellow gradient | fuel indicator, bottom-up at (35,36) |
| Tank gauge | 176,40 | 11–12×52 | tick-marked window, red top line | both tank overlays |

## 10. `textures/gui/expChargeGui.png` (owned by 09)

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #545A54; dominant #909090 24%, #505050 21%, #305030 20%; 44% opaque.
- **Look:** a dark olive-green military crate background with two vertical concrete-grey stripes and a dark vignette, with a light-grey bevelled display inset.

- **Used by** `GuiExplosiveCharge` (advanced charge).
- **Content:** a full 176×166 background, no slots. A dark olive-green (#204020 / #203020) military panel with two vertical grey-concrete stripes and a dark vignette. A light-grey bevelled inset display panel sits at about (28,18)–(148,100). No sprites.

## 11. `textures/gui/tntChargeGui.png` (owned by 09)

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #763A3A; dominant #909090 24%, #101010 13%, #B01010 10%; 44% opaque.
- **Look:** red dynamite sticks with black gaps and a dark vignette, with the same light-grey display inset.

- **Used by** `GuiExplosiveCharge` (TNT charge).
- **Content:** the same layout as `expChargeGui.png`, but the background is red dynamite-stick texture (#B01010 / #901010 with black gaps and a vignette), with the same light-grey inset display panel. No sprites.

## 12. `textures/gui/turretBase_GUI.png` (owned by 09)

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #989896; dominant #909090 41%, #D0D0D0 28%, #505050 10%; 45% opaque.
- **Look:** grey panel with two 3×3 grids of wells carrying darker bullet/cartridge silhouettes, a pistol-silhouette well and a plate-silhouette well between them, a thin health-bar well; sprites: a red-yellow-green bar and two pig icons.

- **Used by** [`GuiTurretBase`](../09-machines-utility/turret.md#gui-guiturretbase) and [`GuiButtonTurretTargetAnimals`](../09-machines-utility/turret.md).
- **Main panel:**
  - power well;
  - a left 3×3 ammo grid (18+18i, 17+18j) with bullet-silhouette slot backgrounds;
  - a gun slot (85,19) with a pistol silhouette;
  - an armor slot (85,42) with a plate-and-rivets silhouette;
  - a right 3×3 grid (116+18i, 17+18j) with small-cartridge silhouettes;
  - a health-bar well at (115,73) 53×5 with a centre marker.

| Sprite | u,v | Size | Content | Use |
|---|---|---|---|---|
| Health bar | 177,1 | ~52×4 | gradient red (left) → yellow → green (right), with a grey centre marker | turret health fill, left→right at (116,74) |
| Target-animals icon, mode 0 | 176,6 | 16×16 | pink pig with green check mark | target-animals button (mode semantics in [09](../09-machines-utility/index.md)) |
| Target-animals icon, mode 1 | 176,22 | 16×16 | pig with red crosshair/X | target-animals button |

## 13. `textures/gui/tgchest_gui.png` (owned by 09)

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #989898; dominant #909090 59%, #D0D0D0 20%, #F0F0F0 9%; 77% opaque (the largest panel, 230 wide).
- **Look:** a wide grey panel almost entirely filled with slot wells: 12 columns × 6 rows on top, the player inventory below, and a small food row and ammo grid at the bottom right.

- **Used by** `GuiTechgunsChest` (xSize 230).
- **Layout:** top region (0,0)–(229, rows·18+17) with up to 6 rows × 12 columns of slot wells starting at (8,18). The bottom part, drawn from v126, 96 tall, holds the player inventory at y 103+ and hotbar at y 161+ (offset by the row count). To its right there's a 3-slot food row (172+18i) and a 2×3 ammo grid. No sprites.

## 14. `textures/gui/TGPlayerInventory.png` (owned by the player-inventory area, 01/05)

- **Measured** (`ASSET-FACTS.md`): 256×256, no animation; average #868685; dominant #909090 45%, #D0D0D0 21%, #101010 14%; 48% opaque.
- **Look:** grey panel with a black player-preview window, armour column and Techguns special slots; in the sprite column two tab shapes and a grid of tiny red/green/yellow 7×7 toggle icons.

- **Used by** [`GuiTGPlayerInventory`](../05-armor/extra-inventory.md#5-gui-guitgplayerinventory), [`TGGuiTabButton`](../05-armor/extra-inventory.md), [`TGGuiToggleButton`](../05-armor/extra-inventory.md).
- **Main panel (176×166):**
  - 4 armor slots at (8, 8+18i);
  - a black player-preview window at about (25,7)–(78,78);
  - face (80,8) and back (80,26) slots;
  - a shield-shaped grey icon area at (99,7) 14×14;
  - a tick-marked bar at (115,7) 54×6;
  - 3 food slots (116+18i, 24);
  - a heal slot (96,62);
  - a 2×3 ammo grid (116+18j, 44+18i);
  - a column of 11×11 toggle buttons at (173, 7+11i).

| Sprite | u,v | Size | Content | Use |
|---|---|---|---|---|
| Tab, inactive | 177,0 | 26×26 (sprite ~28×26) | raised light-grey tab, open on the right | inventory-tab button (enabled state draws v26) |
| Tab, active | 177,26 | 26×26 | darker grey tab | active tab |
| Tab connector | 213,0 | 3×26 | white bevel strip | joins the active tab to the panel |
| Toggle icons | 242+7·state, 7·mode (+14 for mode>3) | 7×7 each | pairs (state 0 = red/off, 1 = green/on): X / check, goggles, "H"/"S" letter blocks, "K" block, "!" warning (yellow/red), padlock, crossed-out device | toggle buttons for the player's equipment options |
| Small rivet tiles | 0–23, 168–175 | three 7×7 | dark rivet/dice squares | not referenced by code |

## 15. Draw-call rectangle table for the 08 GUIs (consistency supplement)

Read from the GUI classes' background-layer methods. All coordinates are relative to the GUI's top-left (k, l); all sources are in the same 256×256 texture. "Fill" gives the direction in which the visible part grows. Rectangles are (u, v, width, height) for the source and (x, y) for the destination.

### 15.1 Shared base (`GuiTechgunsPoweredTileEnt`, every powered machine)

| Region | Source rect | Dest | Role and fill direction | States |
|---|---|---|---|---|
| Panel | (0,0,176,166) | (0,0) | background, drawn by each GUI | none |
| Power bar | (251, 1+d, 4, 48) with d = 48 − stored·48/max | (8, 17+d) | RF bar; the gradient is cut from the top, so it fills **bottom-up** | 48 steps; empty at d = 48 |
| Side tab (redstone) | (195,0,27,30) | (−27,5) | backdrop for the redstone button | one state |
| Side tab (security) | (195,0,27,30) | (−27,35) | backdrop for the security button | one state |
| Redstone lamps, machine enabled | (240,0,5,5) at (6,5); (245,5,5,5) at (11,5) | | left lamp dark, right lamp green | **enabled** |
| Redstone lamps, machine disabled | (240,5,5,5) at (6,5); (245,0,5,5) at (11,5) | | left lamp red, right lamp dark | **disabled** |
| Redstone button icon | (234, 10+16·mode, 16, 16) | button + (2,2) | mode 0 ignore, 1 high, 2 low | 3 modes; the button frame is the vanilla widget strip |
| Security button icon | (234, 58+16·mode, 16, 16) | button + (2,2) | mode 0 public (green), 1 friends (yellow), 2 owner (red) | 3 modes |

Button frames (redstone, security, drain toggle) are drawn with the **vanilla** `widgets.png` button strip, not with this texture: a stretched box from v 46 + 20·k (k = 0 disabled, 1 normal, 2 hovered), 200×20 source, border 2/3/2/2 px. Only the 16×16 icons come from the machine GUI texture.

### 15.2 Ammo Press (`ammoPress_GUI.png`)

| Region | Source rect | Dest | Role and fill direction |
|---|---|---|---|
| Upgrade frame | (119,59,20,20) | (151,59) | slot well copy |
| Progress funnel | (176,0,19,h+1), h = progress·21/total | (119,36) | fills **top-down** |
| Slot wells in the panel | printed in the panel | (99,16), (119,16), (139,16), output (119,59) | metal 1, metal 2, powder, output |
| Button `<` / `>` | vanilla widget frames | (20,50) and (40,50), 20×20 each | plan previous / next |

### 15.3 Metal Press (`metalPress_GUI.png`)

| Region | Source rect | Dest | Role and fill direction |
|---|---|---|---|
| Upgrade frame | (119,59,20,20) | (149,59) | slot well copy |
| Progress funnel | (176,0,20,h+1), h = progress·21/total | (119,36) | **top-down** (this one is 20 wide, the Ammo Press one 19) |
| Switch button | vanilla widget frame | (20,50), 40×20 | auto-split toggle (state shown by text "On"/"Off") |

### 15.4 Chemical Laboratory (`chemLab_Gui.png`)

| Region | Source rect | Dest | Role and fill direction |
|---|---|---|---|
| Upgrade frame | (134,16,20,20) | (134,46) | slot well copy |
| Input-tank gauge | (176,32,12,52) | (18,17) | overlay on the left tank; fluid is drawn separately as a tiled icon, 10 wide, up to 50 high, **bottom-up** |
| Output-tank gauge | (176,32,12,52) | (157,17) | overlay on the right tank, same fill |
| Stage 1 funnel drip | (178,5,8,i1), i1 up to 25 | (81,19) | prog 0 to 0.2, **top-down** |
| Stage 2 pipe | (186,25,i2,1), i2 up to 9 | (88,39) | prog 0.2 to 0.4, **left-to-right** |
| Stage 3 round flask | (194, 9+20−i3, 14, i3), i3 up to 20 | (97, 23+20−i3) | prog 0.4 to 0.6, **bottom-up** |
| Stage 4 distillation tube | (205,3,i4,6), i4 up to 16 | (108,17) | prog 0.6 to 0.8, **left-to-right** |
| Stage 5 Erlenmeyer | (214,6,14,i5+1), i5 up to 24 | (117,20) | prog 0.8 to 1.0, **top-down** |
| Drain toggle icon | (188, 31+12·mode, 12, 12) | button + (1,1) | mode 0 = output tank drained (shown at v 31), mode 1 = input tank drained (v 43). The button is (156,68) 14×14 |
| Dump buttons | vanilla widget frames, label "x" | (20,7) and (159,7), 8×8 | dump input / dump output |

### 15.5 Fabricator (`Fabricator_Gui.png`)

| Region | Source rect | Dest | Role and fill direction |
|---|---|---|---|
| Upgrade frame | (7,83,18,18) | (149,49) | copy of the player-inventory slot well |
| Progress arrow | (0,167,w+1,11), w = progress·90/total | (21,53) | **left-to-right** |
| Slot wells in the panel | printed in the panel | input (18,16), wire (46,16), powder (67,16), plate (88,16), output (115,49) | the ghost silhouettes (wire coil, dust, plate) are part of the panel art |

### 15.6 Charging Station (`ChargingStation_Gui.png`)

| Region | Source rect | Dest | Role and fill direction |
|---|---|---|---|
| Upgrade frame | (7,83,18,18) | (149,49) | slot well copy |
| Progress bolt | (0,167,w+1,10), w = progress·26/total | (39,19) | **left-to-right**; in item mode `totalTime` is 0, so only a sliver shows |
| Slot wells | printed in the panel | input (18,16), output (67,16) | |

### 15.7 Reaction Chamber (`ReactionChamber_Gui.png`)

| Region | Source rect | Dest | Role and fill direction | States |
|---|---|---|---|---|
| Tank gauge | (176,32,12,52) | (18,17) | overlay on the tank; fluid area [10](../10-entities-npcs/index.md) wide, up to 50 high, **bottom-up** | empty to 10 000 mB |
| Level marker | (177,32,7,1) | (19, 17+px_height) | target liquid level, one 5 px step per bucket | levels 0 to 10 |
| Completion bar | (0,167,c,4), c = completion/required·100 | (67,61) | **left-to-right** | shown only while a reaction runs |
| Duration bar | (0,175,d,4), d = duration/maxDuration·100 | (67,69) | **left-to-right** | same |
| Intensity fill (matching) | (190, px, 5, 40−px), px = (10−intensity)·4 | (114, 16+px) | **bottom-up** | green when idle or when the setting equals the preferred intensity |
| Intensity fill (off target) | (198, px, 5, 40−px) | (114, 16+px) | **bottom-up** | orange while working and the setting differs from the preferred value |
| Preferred-intensity marker | (178,22,5,3) | (112, 15+40−pref·4) | pointer | only while working |

Source note: the code reads the duration bar from v 175 even though the sprite starts at v 174; the completion bar reads v 167. The small level bars at (176,0) and (182,0) are unreferenced.
