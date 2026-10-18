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

- **Used by** `GuiAmmoPress`, `GuiScrapper` (09), and **every** powered-machine GUI for the power bar, redstone and security sprites (`GuiTechgunsPoweredTileEnt`, `GuiButtonRedstoneState`, `GuiButtonSecurity`). `GuiTechgunsChest` (09) for its security button. Also used by the NEI handlers.
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

- **Used by** `GuiMetalPress` and the Metal Press NEI handler.
- **Main panel:** the power-bar well as above; two input frames at (109,16) and (129,16); a funnel/pipe outline down to the output frame at (119,59).
- **Sprites:** only the progress funnel at u176 v0 (19–20×21), identical to the ammo press one. The upgrade frame is copied from (119,59), 20×20, to (149,59).

## 3. `textures/gui/chemLab_Gui.png`

- **Used by** `GuiChemLab`, `GuiButtonToggleDrainTank`, the Chem Lab NEI handler.
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

- **Used by** `GuiFabricator` and the Fabricator NEI handler.
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

- **Used by** `GuiReactionChamber` and the RC NEI handler.
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

- **Used by** `GuiChargingStation` and its NEI handler.
- **Main panel:** power well; the input frame at (18,16); a grey zig-zag **lightning-bolt arrow** outline at about (37,18)–(65,30); the output frame at (67,16). Code copies the upgrade frame from (7,83) to (149,49).
- **Sprite:** u0 v167, about 30×10. A lightning arrow in a red → orange → yellow gradient. Progress fill, left→right at (39,19).

## 7. `textures/gui/camoBench_GUI.png` (owned by 09)

- **Used by** `GuiCamoBench`.
- **Main panel:**
  - one framed slot at (17,14) (camo target);
  - a raised frame holding 6 slot wells at (54,16)–(171,35), with small square tabs under each;
  - per the container: armor slots at (96+19i, 18), back slot (75,18), face slot (56,18).
- **Sprite:** u178 v0, 45×84. A tall preview panel with a vivid blue vertical gradient (#0090F0 top → #0030F0 bottom) inside a light bevelled frame. It's drawn at (−45,6), to the left of the GUI, as the camo preview backdrop.

## 8. `textures/gui/repairBench_GUI.png` (owned by 09)

- **Used by** `GuiRepairBench` and `GuiButtonRepair`.
- **Main panel:** a raised frame with 4 armor slot wells at (89+20i, 18); a row of 9 slot wells at (8+18i, 57); the player inventory.
- **Sprite:** u176 v0, 12×12. A dark-grey wrench icon drawn on the repair buttons (14×14 at (90+20i, 39)).

## 9. `textures/gui/oreDrill_GUI.png` (owned by 09)

- **Used by** `GuiOreDrill`.
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

- **Used by** `GuiExplosiveCharge` (advanced charge).
- **Content:** a full 176×166 background, no slots. A dark olive-green (#204020 / #203020) military panel with two vertical grey-concrete stripes and a dark vignette. A light-grey bevelled inset display panel sits at about (28,18)–(148,100). No sprites.

## 11. `textures/gui/tntChargeGui.png` (owned by 09)

- **Used by** `GuiExplosiveCharge` (TNT charge).
- **Content:** the same layout as `expChargeGui.png`, but the background is red dynamite-stick texture (#B01010 / #901010 with black gaps and a vignette), with the same light-grey inset display panel. No sprites.

## 12. `textures/gui/turretBase_GUI.png` (owned by 09)

- **Used by** `GuiTurretBase` and `GuiButtonTurretTargetAnimals`.
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
| Target-animals icon, mode 0 | 176,6 | 16×16 | pink pig with green check mark | target-animals button (mode semantics in 09) |
| Target-animals icon, mode 1 | 176,22 | 16×16 | pig with red crosshair/X | target-animals button |

## 13. `textures/gui/tgchest_gui.png` (owned by 09)

- **Used by** `GuiTechgunsChest` (xSize 230).
- **Layout:** top region (0,0)–(229, rows·18+17) with up to 6 rows × 12 columns of slot wells starting at (8,18). The bottom part, drawn from v126, 96 tall, holds the player inventory at y 103+ and hotbar at y 161+ (offset by the row count). To its right there's a 3-slot food row (172+18i) and a 2×3 ammo grid. No sprites.

## 14. `textures/gui/TGPlayerInventory.png` (owned by the player-inventory area, 01/05)

- **Used by** `GuiTGPlayerInventory`, `TGGuiTabButton`, `TGGuiToggleButton`.
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
