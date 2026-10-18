# 07-blocks: block rendering, block models and block item renderers

Scope:

- `client/renderer/block/*`
- `client/models/blocks/*`
- `client/renderer/item/RenderBlockModelItem*.java`

The tile-entity special renderers (TESRs) that drive the machine models ([`RenderAmmoPress`](../08-machines-production/rendering.md#2-renderammopress-generic-model--rotation--progress-tesr), [`RenderFabricator`](../08-machines-production/fabricator.md#7-visuals-renderfabricator), [`RenderChargingStation`](../08-machines-production/charging-station.md), [`RenderExplosiveCharge`](../09-machines-utility/explosive-charges.md#world-rendering-renderexplosivecharge), [`RenderOreDrill`](../09-machines-utility/ore-drill.md#9-tesr-renderoredrill), [`RenderTileEntityBlock`](../08-machines-production/rendering.md#4-rendertileentityblock-used-by-09s-camo-and-repair-benches), [`RenderBlockTileEntModel`](../08-machines-production/rendering.md#3-renderblocktileentmodel-used-by-09s-turret-base-and-flag), [`RenderTGChest`](../09-machines-utility/misc-tile-entities.md#1-reinforced-chest-and-weapon-chest)) live in `client/renderer/tileentity` and are owned by [08](../08-machines-production/index.md)/09. Only the model geometry and the model-side animation formulas are described here.

**Model-space convention.** All `ModelBase` models use the standard Minecraft model space: 16 units = 1 block, Y pointing down, and the block's floor at model y = 24. They are drawn after a 180° flip about X. To get heights, read **height above floor (px) = 24 − model y**. X and Z are centred on the block (−8 … +8).

## 1. ISBRH renderers and render IDs

No render ID is a constant. Each `ISimpleBlockRenderingHandler` takes `RenderingRegistry.getNextAvailableRenderId()` when it is constructed (`RenderGenericBlock` does it in a field initializer, `RenderGenericPane` the same way). [`ClientProxy.registerRenderers()`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) then pushes that id into the block with `setRenderType(id)` and registers the handler. Construction order is ladder, lamp, pane, directional, Chisel CTM, ore cluster.

| Handler | Blocks | World rendering | Inventory | 3D in inventory |
|---|---|---|---|---|
| `RenderLadder(ModelLadder01, ladder01_inv.png, 1.0)` | [`ladder01`](block-catalogue.md#registered-blocks) | Rails and rungs built with `TessellatorCubeProper` (§2). The model and texture arguments are stored but never used. | Facing-0 ladder drawn with an offset of (−0.6, −0.45, −1.0) | yes |
| `RenderLamp(ModelLadder01, ladder01_inv.png, 1.0)` | [`lamp01`](block-catalogue.md#registered-blocks) | Socket + lens, or lantern + arms (see special-blocks §7). Brightness is the block's mixed brightness and the colour is white. An inset of 0.001 on lens/socket faces avoids z-fighting. | Rotated 180° about X and scaled 1.5. Lamps are offset (−0.55, −0.80, −0.45) and drawn as the ceiling lamp; lanterns are offset (−1.0, −0.05, 0.05). | yes |
| `RenderGenericPane` | [`camoNetPane`](block-catalogue.md#registered-blocks) | Vanilla pane renderer when any side connects, plus the 2-px pole (special-blocks §3). A non-pane block falls back to a standard cube. | nothing (flat icon) | no |
| `RenderDirectionalBlock` (extends `RenderGenericBlock`) | [`screen`](block-catalogue.md#registered-blocks), [`controlpanel`](block-catalogue.md#registered-blocks) | Pass 0: standard cube. Pass 1: the overlay icon on the front face, with `TGRenderHelper.enableFXLighting/disableFXLighting` around it (see [BLK-20](index.md#suspected-bugs)). | nothing (flat `_top` icon) | no |
| Chisel `RendererCTM` (external) | [`container`](block-catalogue.md#registered-blocks), [`concrete`](block-catalogue.md#registered-blocks), [`metalpanel`](block-catalogue.md#registered-blocks), [`metalpanel2`](block-catalogue.md#registered-blocks), [`bugnest`](block-catalogue.md#registered-blocks), [`oreDrill`](block-catalogue.md#registered-blocks), [`machineMultiBlock`](block-catalogue.md#registered-blocks) | see [connected-textures.md](connected-textures.md) | Chisel | Chisel |
| `RenderOrecluster` | [`oreCluster`](block-catalogue.md#registered-blocks), [`oreCluster2`](block-catalogue.md#registered-blocks), both custom clusters (only if enabled) | Standard cube with the base icon, then six faces with the tinted overlay (special-blocks §11) | Stone cube plus tinted overlay, standard item lighting, offset (0.15, 0, 0.15) | yes |
| `RenderGenericBlock` (base class) | none directly | does nothing; owns the render id | – | no |

**The "8 empty vertices" workaround.** `RenderDirectionalBlock` and `RenderOrecluster` add eight `(0,0,0)` vertices (two degenerate quads) to the tessellator before drawing. The source calls this a workaround; it guards against a render pass that would otherwise produce no geometry. It is harmless.

**Other blocks use vanilla render types:**

| Render type | Blocks |
|---|---|
| 0 (cube, bounds from `setBlockBoundsBasedOnState`) | sandbags, camoNetRoof, bugnesteggs, airMarker, oreTitanIron, TGMonsterSpawner, Bioblob |
| 7 (door) | door01 |
| 8 (ladder) | bugnestslimy |
| Forge fluid renderer | acid |
| −1 (TESR only) | the two chests |

## 2. Cube helpers

### `TessellatorCubeProper(block, meta)` (world-projected UVs)

`drawStandardCube(tess, minX, minY, minZ, maxX, maxY, maxZ [, overrideIcon])` emits the six faces of an axis-aligned box in block-local coordinates (0–1). For each face it takes `block.getIcon(side, meta)`, or the override icon if one is given. The UVs come from the box's **position inside the block**, exactly like vanilla block rendering:

| Face | U from | V from |
|---|---|---|
| top, bottom | x | z |
| north, south (minZ / maxZ) | x | y, flipped so the texture's top row is at the box top |
| west, east (minX / maxX) | z | y, flipped |

So a small box samples only the matching sub-rectangle of a 16×16-unit icon. That is how the ladder and lamp textures are designed: the art is painted where the geometry sits (see textures.md).

- A "scaleUVs" mode stretches the whole icon over each face instead. No in-scope caller uses it.
- The face normals are set inconsistently (e.g. "back" uses (−1, 1, 0)). This only matters for lit inventory rendering ([BLK-29](index.md#suspected-bugs)).

### `TesselatorCube` family (fixed UVs; for TESRs and items)

These draw a unit cube (0–1) bound to their own `ResourceLocation` texture. Each face has a fixed UV rectangle:

- **top:** y = 1, U along x, V reversed along z
- **bottom:** y = 0
- **front:** z = 0, the north face
- **back:** z = 1
- **left:** x = 0
- **right:** x = 1

`drawCube()` binds the texture and draws one quad batch.

| Subclass | Texture | UV quadrants of the 64×64 atlas | Used by |
|---|---|---|---|
| `PaintMachineTesselatorCube` | [`textures/blocks/paintmachine_textures.png`](textures.md#camo-bench-and-repair-bench-09) | top = bottom-left quadrant (u 0–0.5, v 0.5–1); front = top-right (u 0.5–1, v 0–0.5); back, left, right, bottom = top-left (0–0.5, 0–0.5) | Camo Bench TESR and item ([09](../09-machines-utility/index.md)) |
| `RepairBenchTesselatorCube(path)` (default [`textures/blocks/repairbench_textures.png`](textures.md#camo-bench-and-repair-bench-09)) | same layout | same quadrants | Repair Bench TESR and item ([09](../09-machines-utility/index.md)); also the Flag item (see §4) |
| `OreDrillCube(texture)` | the given texture | full 0–1 on every face | Draws a box centred on the origin with half-sizes (sx, sy, sz). Used by `RenderOreDrill` ([08](../08-machines-production/index.md)) for the spinning drill head with `drillhead`, `drillheadObsidian` and `drillheadCarbon`. |

`ReactionChamberFluidBox` is an empty `TessellatorCubeProper` subclass with a null block. It is unreferenced, and would throw a NullPointerException unless an override icon is always passed.

## 3. Block models (`client/models/blocks`)

UV origins are listed so artists can locate the parts on the textures in [textures.md](textures.md). Box unwrap is the standard Minecraft layout: a box W×H×D at (u, v) uses the rectangle u…u+2(W+D), v…v+D+H. All machine textures are drawn at **2×** the model's declared texture size: one model texel = 2×2 PNG pixels.

### `ModelMachine` and `ModelBaseTesselator`

- `ModelMachine`: an abstract `ModelBase` adding `render(entity, …, scale, progress)`. The plain `render(...)` forwards with progress 0.
- `ModelBaseTesselator(TesselatorCube)`: a `ModelBase` adapter. It flips 180° about X and Y, offsets by (−0.5, −1.5, −0.5) and calls `cube.drawCube()`. Used to show the camo and repair bench cubes through the model-based item renderer.

### `ModelAmmoPress` (UV 128×64, texture `AmmoPress.png`)

A one-block press on a floor plate.

**Static frame.**

- floor plate 16×16×2 (0–2 px)
- two 3×2×10 side rails and a 10×2×12 centre plate on the floor
- four 1×12×1 corner posts (1.5–13.5 px), each with small 1×1×3 top brackets, plus front cross-pieces
- two 2×8×8 side housings and a third housing at the front
- a 3×6×6 front motor block and two 1×6×6 side discs
- a 2×2×10 front rail
- top assembly: an 8×4×10 head with two 3×4×10 side blocks (12–16 px), two 1×3×8 inner plates and a 6×1×8 die plate (11–12 px)
- two fixed 2×6×7 guide blocks either side of centre

**Moving parts.**

- left jaw (3×5×6 + 1×5×7) and right jaw (same, mirrored), 4–9 px high
- a 4×3×6 top punch (10–13 px)
- a 1×5×5 blank "metal piece" in the centre (3.5–8.5 px)
- three 1×5×1 bullets turned 45°, standing in a row along Z

**Animation by progress p (0–1, from the tile entity):**

- **p = 0:** jaws and punch in the rest position, no piece or bullets.
- **p > 0:** run the cycle twice per operation, with `p′ = 2·(p mod 0.5)`:
  - **p′ < 0.25:** the blank is visible and the jaws close by `ab = √(4p′)`.
  - **0.25 ≤ p′ < 0.66:** the bullets are visible and the jaws reopen smoothly. With `q = (p′−0.25)·2.439`, `ab = ½ + ½·sin(2π·((1−q)/2 + 0.75))`, going from 1 to 0.
  - **p′ ≥ 0.66:** the punch strikes. With `r = min(1, (p′−0.66)·3)`, `c = ½ + ½·sin(2π·(√r + 0.75))`, giving 0 → 1 → 0. The bullets stay visible until p′ reaches 0.9.
- Each jaw moves `2·ab` px toward the centre (0.125 block per unit). The punch moves `1.5·c` px down.

### `ModelMetalPress` (UV 128×64, texture `MetalPress.png`)

Shares the frame layout of the ammo press (floor plate, rails, posts, side housings, motor). It adds:

- a 16×2×12 top beam (14–16 px)
- a 12×2×7 cross piece
- a 10×3×10 upper block
- a 9×1×9 anvil plate (5–6 px)

The press head is an 8×5×8 block with a 9×1×9 plate and four 1×5×1 guide rods (10–15 px).

**Animation.** When p > 0, a 6×1×6 metal sheet shows on the anvil (5–6 px). With `p′ = p mod 0.5`, `y = ½ + ½·sin(2π·(√(2p′) + 0.75))`, giving 0 → 1 → 0. The head translates `4·y` px down. That is two strokes per operation, fast down and slower up. The translation is not pushed or popped inside the model; the TESR's matrix stack contains it.

### `ModelChemLab` (UV 128×64, texture `ChemLab.png`)

A lab bench on a floor plate (16×16×2).

**Main fixtures.**

- a central 5×10×5 reaction column with a 6×1×6 cap
- a front 5×9×5 cabinet with a 6×6×1 front panel
- two 5×5×5 side boxes and two 1×6×6 side motor discs
- an overhead 12×2×2 pipe bar with drip nozzles (2×1×2, 2×3×2) and small 3×1×3 plates under them

**Glassware** (drawn last with alpha blending):

- two tall 4×9×4 tubes at the front corners
- three 2×9×2 test tubes turned 45° on orange 2×1×2 stands at the back
- a round-bottomed flask stacked from 2×1×2, 1×1×1, 2×1×2, 3×2×3, 4×2×4 and 3×1×3 boxes, standing on a small 5×1×5 table with four 1×1×1 feet

**Back rack.** Two 10×10 back panes, a 10×3 shelf and two 10×3 side panes (all zero-thickness planes). Thin 1×1×N tubing runs along the floor.

**Liquid parts:**

- L7 and L8: 3×1×3 slices in the two front tubes
- L1–L3: 1×1×1 slices in the test tubes
- L4 (3×2×3) and L5 (2×1×2): in the flask

**Animation (p > 0):**

- The flask liquid (L4, L5) is visible.
- **Test tube i** (i = 1, 2, 3, starting at 0.2, 0.4 and 0.6) fills over 0.2 of progress with fraction `√((p − start)·5)` (capped at 1). After p = 0.8 all three drain linearly: `1 − (p−0.8)·5`. A liquid column is drawn as `round(f·16)` stacked slices, each 1/32 block (0.5 px) higher.
- **Front-left tube (L7)** starts full and empties over p 0 → ⅓: `1 − 3p`.
- **Front-right tube (L8)** fills over p ⅔ → 1: `3(p − 0.666)`.

### `ModelChargingStation` (UV 64×64, texture `ChargingStation.png`)

An open-fronted cage.

- 16×16×2 bottom plate (0–2 px) and top plate (14–16 px)
- four 2×12×2 corner pillars (2–14 px)
- inner plates 10×1×10 and 8×1×8 stacked on the floor and under the roof (a stepped pad)
- side plates 1×12×12 on the east, west and south faces

Each side plate carries:

- an outward 2×6×6 connector boss
- an inward 1×4×4 emitter and a 2×2×2 tip (the "power" electrodes)

A 0×12×12 front glass pane is defined (UV origin (40, −12)) but its render is commented out. The model has no animation; the TESR ([09](../09-machines-utility/index.md)) adds effects.

### `ModelFabricator` (UV 128×128, texture `Fabricator.png`)

A 2×2-block machine: its footprint spans 32×32 units, centred at (−8, +8) in model space. It is rendered in **parts** chosen by index. `render(…, progress)` draws only part 0. The TESR ([08](../08-machines-production/index.md)) draws and moves the others.

| Part | Contents |
|---|---|
| 0 (static) | 32×32×2 base plate; four 3×14×3 corner posts; four 26×14×3 low side walls (2–16 px); seven 8×8×1 connection flanges on the outer faces; a 24×2×24 inner plate (11–13 px) and a 12×1×12 mid plate; an 8×8×3 controller screen tilted −7.5° about X on the front; top edge bars (two 32×2×4, two 24×2×4) at 16–18 px; four 2×16×4 gantry posts rising to 29 px; two 2×2×16 gantry top rails along Z (27–29 px) |
| 1 | `FrameMove1`: a 20×2×4 moving gantry beam along X |
| 2 | carriage: a 6×2×6 work head with a 2×5×2 drill housing, a 2×6×2 tube, a 2×3×2 laser housing and a 1×2×1 laser beam stub (all turned 45°) |
| 3 | a 1×8×1 drill bit |
| 4 | a 1×9×1 tube extension |
| 5 | a 1×7×1 laser emitter extension |
| 6 | a 30×12×30 glass hood (18–30 px), drawn translucent by the TESR |

### `ModelTurretBase` (UV 64×64, texture `TurretBase.png`)

The static pedestal of the turret machine (`basicMachine:2`, 09).

- a 14×14×14 core cube (0–14 px)
- four 2×15×2 corner posts
- an 8×2×8 top plate (14–16 px)
- four 1×2×12 bottom skirts around the base
- four 1×6×6 sockets centred on the four sides (5–11 px)
- four 6×1×2 diagonal braces from the posts to the top plate (14–15 px)

### `ModelTurret` and `ModelSimpleTurret` (UV 64×64 / 64×32)

Both are [`ModelMultipart`](../03-gun-visuals-audio/rendering-framework.md#1-registration-client-proxy) pedestals rendered by part:

- part 0: an 8×8×1 base and the strut(s), 11 px tall
- other parts: the mount (a small plate at 10–11 px that rotates with the gun)

| Model | Struts | Mount | Used by |
|---|---|---|---|
| `ModelTurret` | two 2×11×2 | 4×1×4 | [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) entity renderer ([10-entities-npcs](../10-entities-npcs/index.md)), with textures `TurretBase_2`…`_5` |
| `ModelSimpleTurret` | one | 2×1×4 | unused |

### `ModelTNTCharge` (UV 32×32, textures `tntCharge.png` / `tntCharge_2.png`)

Three 4×4×12 dynamite sticks turned 45° about Y (diamond section), standing upright side by side near the north edge (2–14 px). An 8×4×2 detonator box (6–10 px) sits in front of them. The TESR ([08](../08-machines-production/index.md)/[09](../09-machines-utility/index.md)) orients the model to the attached face and swaps to `_2` (the "ARM" display) when armed.

### `ModelExpCharge` (UV 32×32, textures `ExpCharge.png` / `ExpCharge_2.png`)

A 10×10×5 strapped charge pack against the north side (3–13 px), plus the same 8×4×2 detonator box.

### `ModelFlagPost` (UV 64×64, texture `flagpost.png`)

A 4-block-tall flag pole (the Flag machine `basicMachine:7`, 09):

| Part | Size | Height |
|---|---|---|
| base slab | 10×10×2 | 0–2 px |
| plinth | 5×5×14 | 2–16 px |
| lower pole | 3×3×23 | 16–39 px |
| upper pole | 2×2×23 | 39–62 px |
| cap | 3×3×2 | 62–64 px |

**Flag.** Three flat 6×14 segments in a chain, extending 18 px toward +X at 47.5–61.5 px height.

**Waving** uses world total time `t` (whole ticks, no partial-tick smoothing). Each segment rotates about a vertical hinge at its root, and each later rotation is nested inside the earlier ones:

| Segment | Hinge (x) | Angle |
|---|---|---|
| 1 | 1 px | 30°·sin(2π·(t mod 60)/60) |
| 2 | 7 px | an extra 30°·sin(2π·(t mod 50)/50) |
| 3 | 13 px | an extra 30°·sin(2π·(t mod 40)/40) |

### `ModelChestInv` (UV 64×64, textures `reinforced_chest.png` / `weapon_chest.png`)

A copy of the vanilla chest model, with lid 14×5×14, knob 2×4×1 and base 14×10×14. It renders all parts with the lid closed (the knob follows the lid angle). Used only by the chest item renderer.

### `ModelLadder01` (UV 32×32)

A free-standing ladder model: two 2×16×2 rails and four 12×2×2 rungs. It is passed to `RenderLadder` and `RenderLamp` but never drawn (dead). Its UV skin is `ladder01_inv.png`.

## 4. Block item renderers

[`RenderMachineBlockItemBase`](../08-machines-production/rendering.md#5-machine-item-rendering) (out of scope, summarised) is an `IItemRenderer` for every render type:

1. push the matrix
2. apply `GL_Transform_Translate`
3. apply the per-type transform
4. bind the texture
5. draw `model.render(null, 0, 0, 0, 0, 0, 1/16)`
6. pop

[`RenderMachineBlockItem`](../08-machines-production/rendering.md#5-machine-item-rendering) dispatches to an array of these by item damage.

| Class | Translate (all types) | First person | Third person | Ground | Inventory |
|---|---|---|---|---|---|
| [`RenderBlockModelItem`](../09-machines-utility/shared-rendering.md#item-rendering-inventory-and-hand) | rotate 180° about X | translate (0.3, −1.7, −0.5), scale 0.9 | translate (0.5, −1.6, −0.5) | translate (0, −1, 0) | translate (0, −1, 0) |
| `RenderBlockModelItemChest` | rotate 180° about X | translate (−0.1, −1.0, −0.9), scale 0.9 | translate (0.5, −1.6, −0.5), **plus the debug offset (Keybinds.X, Y, Z)** ([BLK-16](index.md#suspected-bugs)) | translate (0, −1, 0) | translate (−0.5, 0.3, 0.5), scale 1.1, rotate −90° about Y |
| [`RenderBlockModelItemTNTCharge`](../09-machines-utility/shared-rendering.md#item-rendering-inventory-and-hand) | rotate 180° about X | translate (0.3, −1.7, −0.5), scale 1.2, rotate −122.5° about Y, translate (−0.2, −0.2, 0) | translate (0.4, −1.9, −0.4), rotate −45° about Y, scale 1.5 | scale 1.5, translate (0, −1, 0.3) | translate (0, −1.2, 0), rotate 135° about Y, scale 1.5 |

**Item-renderer wiring** (set up in `ClientProxy`):

| Item | Model / cube | Texture |
|---|---|---|
| `basicMachine:0` | `ModelAmmoPress` | `AmmoPress.png` |
| `basicMachine:1` | `ModelMetalPress` | `MetalPress.png` |
| `basicMachine:2` | `ModelTurretBase` | [`TurretBase.png`](block-catalogue.md#blocks-constructed-but-not-registered-not-in-game) |
| `basicMachine:3` | paint-machine cube | `paintmachine_textures.png` |
| `basicMachine:4` | `ModelChemLab` | `ChemLab.png` |
| `basicMachine:5` | repair-bench cube | `repairbench_textures.png` |
| `basicMachine:6` | TNT charge (TNT transforms) | `tntCharge.png` |
| `basicMachine:7` (Flag) | **repair-bench cube** | `repairbench_textures.png` (not the flag model; [BLK-31](index.md#suspected-bugs)) |
| `basicMachine:8` | `ModelExpCharge` (TNT transforms) | `ExpCharge.png` |
| `basicMachine:9` | `ModelChargingStation` | `ChargingStation.png` |
| `basicMachine:10` | repair-bench cube | spare entry, no such meta |
| [`tgchest`](block-catalogue.md#registered-blocks) | `ModelChestInv` | `reinforced_chest.png` |
| [`tgchest_weapon`](block-catalogue.md#registered-blocks) | `ModelChestInv` | `weapon_chest.png` |
