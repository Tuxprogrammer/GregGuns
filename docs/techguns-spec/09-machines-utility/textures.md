# Textures used by area 09

Measured size, average color, dominant colors and opaque share for every texture this area owns or uses. Numbers are quoted from [ASSET-FACTS.md](../ASSET-FACTS.md) (average = mean of opaque pixels; dominant = three most common colors after quantizing each channel to 8 levels, with their share of opaque pixels; opaque = share of all pixels). None of these files has a `.mcmeta`, so none is animated. The "palette in words" column is a reading of those numbers plus the existing descriptions in this area; it is not a fresh visual inspection.

Related: [entity-sheets.md](entity-sheets.md) (which sheet uses what), [index.md](index.md#textures-described-in-this-area) (ownership), [07-blocks textures](../07-blocks/textures.md), [08 GUI textures](../08-machines-production/gui-textures.md).

## 1. Model and TESR textures owned here

| Texture | Size | Average | Dominant | Opaque | Palette in words | Used by |
|---|---|---|---|---|---|---|
| [`textures/blocks/TurretBase.png`](../07-blocks/textures.md#11-model-textures) | 128×128 | #717171 | #909090 32%, #B0B0B0 18%, #303030 15% | 47% | Mid to light concrete gray body with near-black charcoal trim | turret base, no armor |
| [`textures/blocks/TurretBase_2.png`](../07-blocks/textures.md#11-model-textures) | 128×128 | #757575 | #505050 27%, #B0B0B0 26%, #909090 18% | 47% | Slightly lighter, cleaner brushed gray, dark trim | iron armor |
| [`textures/blocks/TurretBase_3.png`](../07-blocks/textures.md#11-model-textures) | 128×128 | #585858 | #303030 26%, #909090 20%, #101010 17% | 47% | Darker gray with light speckle, black trim | steel armor |
| [`textures/blocks/TurretBase_4.png`](../07-blocks/textures.md#11-model-textures) | 128×128 | #322A34 | #303030 30%, #101010 25%, #503050 17% | 47% | Very dark purple-black with lilac specks | obsidian steel armor |
| [`textures/blocks/TurretBase_5.png`](../07-blocks/textures.md#11-model-textures) | 128×128 | #161616 | #101010 70%, #303030 28%, #505050 0% | 47% | Almost black throughout | carbon armor |
| [`textures/blocks/flagpost.png`](../07-blocks/textures.md#11-model-textures) | 128×128 | #8B7D86 | #D0D0D0 16%, #B0B0B0 14%, #909090 12% | 40% | Pale gray poles and plates; the flag strip adds red, white and navy | camp flag model |
| [`textures/blocks/tntCharge.png`](../07-blocks/textures.md#11-model-textures) | 64×64 | #5F201D | #901010 25%, #501010 24%, #505050 20% | 29% | Dark dynamite red with gray detonator | TNT charge, unarmed (LCD reads "RDY") |
| [`textures/blocks/tntCharge_2.png`](../07-blocks/textures.md#11-model-textures) | 64×64 | #671D1C | #901010 25%, #501010 24%, #701010 20% | 29% | Same, slightly redder (the red "ARM" LCD) | TNT charge, armed |
| [`textures/blocks/ExpCharge.png`](../07-blocks/textures.md#11-model-textures) | 64×64 | #283337 | #303030 52%, #505050 12%, #305050 11% | 50% | Dark olive and blue-gray plastic block, gray detonator | advanced charge, unarmed |
| [`textures/blocks/ExpCharge_2.png`](../07-blocks/textures.md#11-model-textures) | 64×64 | #2D3136 | #303030 52%, #505050 11%, #305050 11% | 50% | Same, with the red LCD | advanced charge, armed |
| [`textures/blocks/reinforced_chest.png`](../07-blocks/textures.md#11-model-textures) | 128×128 | #5B513E | #907050 22%, #303030 19%, #505050 16% | 40% | Tan oak planks with dark iron banding | reinforced chest, single |
| [`textures/blocks/reinforced_chest_double.png`](../07-blocks/textures.md#11-model-textures) | 256×128 | #5B503C | #907050 23%, #303030 16%, #505050 13% | 36% | Same, wider | reinforced chest, double |
| [`textures/blocks/weapon_chest.png`](../07-blocks/textures.md#11-model-textures) | 128×128 | #1C2117 | #303010 50%, #101010 35%, #103010 11% | 40% | Dark olive-drab paint, near-black banding | weapon chest, single |
| [`textures/blocks/weapon_chest_double.png`](../07-blocks/textures.md#11-model-textures) | 256×128 | #1B2016 | #303010 52%, #101010 36%, #103010 9% | 36% | Same, wider | weapon chest, double |
| [`textures/blocks/drillhead.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #5E5E5E | #303030 26%, #505050 21%, #707070 17% | 100% | Neutral brushed steel grays | steel head, and bare rods |
| [`textures/blocks/drillheadObsidian.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #393A4D | #505070 19%, #303030 17%, #303050 15% | 100% | Dark blue-violet steel | obsidian steel head |
| [`textures/blocks/drillheadCarbon.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #252525 | #303030 45%, #101010 44%, #505050 10% | 100% | Near-black graphite | carbon head |
| [`textures/blocks/repairbench_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | 64×64 | #5B554E | #505050 21%, #909090 17%, #705030 11% | 75% | Dark metal grays with brown wood shelving and tool accents | repair bench cube |
| [`textures/blocks/paintmachine_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | 64×64 | #5A5B5A | #505050 23%, #909090 17%, #303030 15% | 75% | Dark mottled grays with small red, green, blue accents | camo bench cube |
| [`textures/blocks/bio.png`](../07-blocks/textures.md#8-bug-nest-bio-blob) | 16×16 | #33A40C | #30B010 55%, #309010 28%, #307010 6% | 100% | Saturated slime green | Bio Blob, all faces |

## 2. Ore drill block textures (cataloged by 07, used here)

| Texture | Size | Average | Dominant | Opaque | Palette in words | Use |
|---|---|---|---|---|---|---|
| [`textures/blocks/drillFrame_outer.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #705854 | #705050 25%, #907070 13%, #503030 7% | 100% | Rusty red-brown steel | meta 0, unformed |
| [`textures/blocks/drillFrame_outer_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #705853 | #705050 20%, #907070 14%, #303030 8% | 100% | Same, formed CTM base | meta 0, formed |
| [`textures/blocks/drillFrame_outer_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 64×64 | #715A57 | #705050 22%, #907070 15%, #503030 8% | 82% | Same, connected-texture sheet | meta 0, formed |
| [`textures/blocks/drillFrame.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #6A5F62 | #303030 19%, #505050 17%, #705050 9% | 48% | Gray X-brace, half transparent | meta 1, unformed |
| [`textures/blocks/drillFrame_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #6A5F62 | #303030 19%, #505050 17%, #705050 9% | 48% | Identical measurements to `drillFrame.png` | meta 1, formed |
| [`textures/blocks/drillFrame_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 64×64 | #6F6467 | #303030 19%, #505050 14%, #B0B0B0 11% | 33% | Connected scaffold sheet, mostly transparent | meta 1, formed |
| [`textures/blocks/drillRod-ctmv.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 64×64 | #767574 | #505050 16%, #B0B0B0 15%, #707070 14% | 100% | Chrome grays, vertical-connect sheet | meta 2 sides |
| [`textures/blocks/drillRod-top.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #5A5A5A | #303030 25%, #505050 22%, #707070 18% | 100% | Mid gray flange | meta 2 ends |
| [`textures/blocks/drillEngine.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #4A4745 | #303030 24%, #101010 20%, #505050 20% | 100% | Dark charcoal machinery, warm gray cast | meta 3, unformed |
| [`textures/blocks/drillEngine_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #4B4746 | #101010 22%, #303030 20%, #505050 19% | 100% | Same, formed | meta 3, formed |
| [`textures/blocks/drillEngine_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 64×64 | #4F4B49 | #101010 21%, #303030 18%, #505050 18% | 100% | Same, CTM sheet | meta 3, formed |
| [`textures/blocks/drillEngineController.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #4F4C4A | #101010 20%, #303030 19%, #505050 17% | 100% | Dark machinery with four small colored LEDs | meta 4 |
| [`textures/blocks/drillEngineTop_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #4B4746 | #101010 22%, #303030 20%, #505050 19% | 100% | Dark; **never selected** ([MUTL-18](index.md#suspected-bugs)) | unused variant 12 |
| [`textures/blocks/drillEngineTop_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 64×64 | #4F4B49 | #101010 21%, #303030 18%, #505050 18% | 100% | Same, CTM sheet | unused variant 12 |
| [`textures/blocks/invisible.png`](../07-blocks/textures.md#4-camo-nets-sandbags-tent-flag-air-marker-invisible) | 16×16 | fully transparent | none | 0% | Nothing is drawn | formed rods |
| [`textures/blocks/drillFrameCorner.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #2D5638 | #305030 28%, #307050 27%, #103010 17% | 55% | Green X-brace; **unreferenced** | none |
| [`textures/blocks/drillRodSide.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #6A6A6A | #505050 18%, #707070 18%, #303030 17% | 100% | Neutral gray; **unreferenced** | none |
| [`textures/blocks/drillRodTop.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | 32×32 | #4D4D4D | #101010 31%, #303030 16%, #707070 13% | 100% | Dark gray; **unreferenced** | none |

## 3. Unused bench and flag files (full description)

Nothing in the code references these. [07](../07-blocks/index.md) holds the catalog entry; the measurements are repeated here because the files belong to this area's machines.

| Texture | Size | Average | Dominant | Opaque | Reading (inferred from the measurements and file names; the images were not re-inspected) |
|---|---|---|---|---|---|
| [`textures/blocks/paintmachine_top.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | 32×32 | #555555 | #101010 24%, #909090 18%, #707070 14% | 100% | Dark gray top with black gaps and lighter grille parts; an older, single-face version of the atlas top |
| [`textures/blocks/paintmachine_front.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | 32×32 | #676B68 | #909090 21%, #B0B0B0 13%, #101010 11% | 100% | Lighter, slightly green-gray front; older version of the canister face |
| [`textures/blocks/paintmachine_side.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | 32×32 | #515150 | #505050 48%, #303030 26%, #909090 12% | 100% | Plain mid-dark gray metal side |
| [`textures/blocks/paintmachine_bottom.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | 32×32 | #515150 | #505050 48%, #303030 26%, #909090 12% | 100% | Same measurements as the side, so most likely the same artwork |
| [`textures/blocks/flagBlock.png`](../07-blocks/textures.md#4-camo-nets-sandbags-tent-flag-air-marker-invisible) | 32×32 | #A296A4 | #B0B0B0 17%, #D0D0D0 15%, #F0F0F0 15% | 100% | Pale lavender-gray, light cloth or banner-like block face; no code reads it, so its intended model is unknown |

The `paintmachine_side` and `paintmachine_bottom` rows have identical measurements; whether they are byte-identical was not checked.

## 3.1 Repair bench and camo bench

Both cubes use the 64 × 64 atlas in four 32 × 32 quadrants (pixel rectangles, half-open):

| Face | Rectangle (px) | Repair bench shows | Camo bench shows |
|---|---|---|---|
| Top | (0,32)–(32,64) | pegboard with tools | gray housing, lamps, louverd vent and rollers |
| Front | (32,0)–(64,32) | shelf with jars, saw blade, screwdriver | three paint canisters |
| Back, left, right, bottom | (0,0)–(32,32) | plain scratched metal | plain mottled metal |
| unused | (32,32)–(64,64) | transparent | transparent |

The 75% opaque share in both files is consistent with three of the four quadrants being filled. The block rotates in 90° steps; see [shared-rendering.md](shared-rendering.md#2-rendertileentityblock--tesselatorcube-camo-bench-and-repair-bench).

## 3.2 Turret base

[`ModelTurretBase`](../07-blocks/block-rendering-and-models.md#modelturretbase-uv-6464-texture-turretbasepng) uses 64 × 64 texture units drawn on the 128 × 128 file, 2 px per unit. Face rectangles below are derived from the standard box unwrap (top, bottom, then the four side faces in the order -X, front, +X, back) and are in pixels. They were computed, not measured from the image.

| Part | Box (units) | Top | Bottom | -X | Front | +X | Back |
|---|---|---|---|---|---|---|---|
| Base cube | 14×14×14 at (8,36) | (44,72)–(72,100) | (72,72)–(100,100) | (16,100)–(44,128) | (44,100)–(72,128) | (72,100)–(100,128) | (100,100)–(128,128) |
| Top plate | 8×2×8 at (30,0) | (76,0)–(92,16) | (92,0)–(108,16) | (60,16)–(76,20) | (76,16)–(92,20) | (92,16)–(108,20) | (108,16)–(124,20) |
| Corner post (×4, same UV) | 2×15×2 at (0,30) | (4,60)–(8,64) | (8,60)–(12,64) | (0,64)–(4,94) | (4,64)–(8,94) | (8,64)–(12,94) | (12,64)–(16,94) |
| Diagonal bar (×4) | 6×1×2 at (36,18) | (76,36)–(88,40) | (88,36)–(100,40) | (72,40)–(76,42) | (76,40)–(88,42) | (88,40)–(92,42) | (92,40)–(104,42) |
| Bottom rail (×4) | 1×2×12 at (0,15) | (24,30)–(26,54) | (26,30)–(28,54) | (0,54)–(24,58) | (24,54)–(26,58) | (26,54)–(50,58) | (50,54)–(52,58) |
| Socket (×4) | 1×6×6 at (0,0) | (12,0)–(14,12) | (14,0)–(16,12) | (0,12)–(12,24) | (12,12)–(14,24) | (14,12)–(26,24) | (26,12)–(28,24) |

Head parts that share this texture ([`ModelTurret`](../07-blocks/block-rendering-and-models.md#modelturret-and-modelsimpleturret-uv-6464--6432), owned by [10](../10-entities-npcs/index.md)):

| Part | Box (units) | Top | -X | Front | +X | Back |
|---|---|---|---|---|---|---|
| Mount | 4×1×4 at (34,12) | (76,24)–(84,32) | (68,32)–(76,34) | (76,32)–(84,34) | (84,32)–(92,34) | (92,32)–(100,34) |
| Plate | 8×1×8 at (32,22) | (80,44)–(96,60) | (64,60)–(80,62) | (80,60)–(96,62) | (96,60)–(112,62) | (112,60)–(128,62) |
| Arm (×2) | 2×11×2 at (9,30) | (22,60)–(26,64) | (18,64)–(22,86) | (22,64)–(26,86) | (26,64)–(30,86) | (30,64)–(34,86) |

The 47% opaque share was not reconciled against these rectangles. All five variants share the layout, so the rectangles are identical across [`TurretBase.png`](../07-blocks/block-catalog.md#blocks-constructed-but-not-registered-not-in-game) to `TurretBase_5.png`; only color changes (table in section 1).

## 3.3 Explosive charges

The models declare 32 × 32 texture units on 64 × 64 px files (2 px per unit). Derived rectangles in pixels:

| Part | Box (units) | Top | Bottom | -X | Front | +X | Back |
|---|---|---|---|---|---|---|---|
| Dynamite stick (×3, same UV) | 4×12×4 at (0,0) | (8,0)–(16,8) | (16,0)–(24,8) | (0,8)–(8,32) | (8,8)–(16,32) | (16,8)–(24,32) | (24,8)–(32,32) |
| Detonator (both models) | 8×4×2 at (0,17) | (4,34)–(20,38) | (20,34)–(36,38) | (0,38)–(4,46) | (4,38)–(20,46) | (20,38)–(24,46) | (24,38)–(40,46) |
| C4 brick | 10×10×5 at (0,0) | (10,0)–(30,10) | (30,0)–(50,10) | (0,10)–(10,30) | (10,10)–(30,30) | (30,10)–(40,30) | (40,10)–(60,30) |

The armed "_2" textures change only the LCD pixels on the detonator front face, from green "RDY" to red "ARM" (existing description in [explosive-charges.md](explosive-charges.md#textures-64--64-no-animation)).

## 3.4 Camp flag

[`ModelFlagPost`](../07-blocks/block-rendering-and-models.md#modelflagpost-uv-6464-texture-flagpostpng) uses 64 × 64 units on the 128 × 128 file (2 px per unit).

| Part | Box (units) | Top | -X | Front | +X | Back |
|---|---|---|---|---|---|---|
| Foot plate | 10×2×10 at (0,0) | (20,0)–(40,20) | (0,20)–(20,24) | (20,20)–(40,24) | (40,20)–(60,24) | (60,20)–(80,24) |
| Lower post | 5×14×5 at (0,14) | (10,28)–(20,38) | (0,38)–(10,66) | (10,38)–(20,66) | (20,38)–(30,66) | (30,38)–(40,66) |
| Middle pole | 3×23×3 at (22,14) | (50,28)–(56,34) | (44,34)–(50,80) | (50,34)–(56,80) | (56,34)–(62,80) | (62,34)–(68,80) |
| Top pole | 2×23×2 at (34,15) | (72,30)–(76,34) | (68,34)–(72,80) | (72,34)–(76,80) | (76,34)–(80,80) | (80,34)–(84,80) |
| Finial cap | 3×2×3 at (33,3) | (72,6)–(78,12) | (66,12)–(72,16) | (72,12)–(78,16) | (78,12)–(84,16) | (84,12)–(90,16) |

The three cloth segments are 6 × 14 zero-thickness planes at UV origins (0,50), (12,50) and (24,50); their front and back faces occupy the strip from about (0,100) to (72,128) px. Overall the file is only 40% opaque.

## 3.5 Chests

Both chest types use the vanilla layout at 2×: single files are 64 × 64 units on 128 × 128 px; double files are 128 × 64 units on 256 × 128 px. Derived rectangles in pixels for the single chest:

| Part | Box (units) | Top | Bottom | -X | Front | +X | Back |
|---|---|---|---|---|---|---|---|
| Lid | 14×5×14 at (0,0) | (28,0)–(56,28) | (56,0)–(84,28) | (0,28)–(28,38) | (28,28)–(56,38) | (56,28)–(84,38) | (84,28)–(112,38) |
| Body | 14×10×14 at (0,19) | (28,38)–(56,66) | (56,38)–(84,66) | (0,66)–(28,86) | (28,66)–(56,86) | (56,66)–(84,86) | (84,66)–(112,86) |
| Latch | 2×4×1 at (0,0) | (2,0)–(6,2) | (6,0)–(10,2) | (0,2)–(2,10) | (2,2)–(6,10) | (6,2)–(8,10) | (8,2)–(12,10) |

The latch box overlaps the lid's rectangle in the corner of the sheet, as in the vanilla chest texture. The double chest adds 16 more units of width to each strip (30-unit-wide lid and body boxes) at the same origins.

## 3.6 Ore drill head (TESR box)

`OreDrillCube` maps the whole 32 × 32 texture, rectangle (0,0)–(32,32), onto every face of every box, so there is no sub-region layout to record.

## 4. GUI textures used by area 09

All are 256 × 256; the GUIs use only the regions listed. Region maps with fill directions are in the per-machine files; [08](../08-machines-production/index.md) catalogs the files.

| Texture | Average | Dominant | Opaque | Palette in words | Regions used (px) | Detail |
|---|---|---|---|---|---|---|
| [`textures/gui/repairBench_GUI.png`](../08-machines-production/gui-textures.md#8-texturesguirepairbench_guipng-owned-by-09) | #A1A1A1 | #909090 43%, #D0D0D0 38%, #F0F0F0 8% | 44% | Standard light-gray panel, white bevels | panel (0,0) 176×166; wrench icon (176,0) 12×12 | [repair-bench.md](repair-bench.md#gui-guirepairbench) |
| [`textures/gui/camoBench_GUI.png`](../08-machines-production/gui-textures.md#7-texturesguicamobench_guipng-owned-by-09) | #969EAB | #D0D0D0 38%, #909090 34%, #F0F0F0 8% | 50% | Gray panel with a blue-tinged mirror panel | panel (0,0) 176×166; mirror panel (178,0) 45×84 | [camo-bench.md](camo-bench.md#gui-guicamobench) |
| [`textures/gui/oreDrill_GUI.png`](../08-machines-production/gui-textures.md#9-texturesguioredrill_guipng-owned-by-09) | #9F9E9E | #909090 44%, #D0D0D0 34%, #F0F0F0 9% | 46% | Gray panel with silhouette icons | panel (0,0) 176×166; drill cone (177,1) 25 wide; flame (188,39) 6 wide; tank overlay (176,40) 11–12 × 52 | [ore-drill.md](ore-drill.md#texture-use) |
| [`textures/gui/turretBase_GUI.png`](../08-machines-production/gui-textures.md#12-texturesguiturretbase_guipng-owned-by-09) | #989896 | #909090 41%, #D0D0D0 28%, #505050 10% | 45% | Gray panel, dark silhouettes | panel (0,0) 176×166; repair fill (177,1) 52×4; pig icons (176,6) and (176,22) 16×16 | [turret.md](turret.md#gui-guiturretbase) |
| [`textures/gui/tntChargeGui.png`](../08-machines-production/gui-textures.md#11-texturesguitntchargeguipng-owned-by-09) | #763A3A | #909090 24%, #101010 13%, #B01010 10% | 44% | Red dynamite sticks with black outline and gray screen | panel (0,0) 176×166 | [explosive-charges.md](explosive-charges.md#container-and-gui) |
| [`textures/gui/expChargeGui.png`](../08-machines-production/gui-textures.md#10-texturesguiexpchargeguipng-owned-by-09) | #545A54 | #909090 24%, #505050 21%, #305030 20% | 44% | Olive-green C4 with gray tape and gray screen | panel (0,0) 176×166 | same |
| [`textures/gui/tgchest_gui.png`](../08-machines-production/gui-textures.md#13-texturesguitgchest_guipng-owned-by-09) | #989898 | #909090 59%, #D0D0D0 20%, #F0F0F0 9% | 77% | Mostly flat gray (the largest opaque share of any GUI here) | top part (0,0) 230 × (rows·18 + 17); bottom part from v = 126, height 96 | [misc-tile-entities.md](misc-tile-entities.md#gui-guitechgunschest) |
| [`textures/gui/ammoPress_GUI.png`](../08-machines-production/gui-textures.md#1-texturesguiammopress_guipng) | #A4A4A2 | #D0D0D0 46%, #909090 33%, #F0F0F0 7% | 48% | Gray panel; borrowed by the scrapper and for the turret side backdrop | scrapper: panel (0,0) 176×166, patch (119,59) 20×20, arrow (176,0) 19 wide; turret: backdrop (195,0) 27×30 | [scrapper.md](scrapper.md#gui-guiscrapper) |
