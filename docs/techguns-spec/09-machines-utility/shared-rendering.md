# Shared host block and renderers (area 09)

This file covers the parts of the shared `BasicMachine` block that apply to area 09's metadata values, and the two generic tile-entity renderers that only area 09 uses. The block class itself (all metas, wrench handling, fluid interaction) is catalogued in full by **[08-machines-production](../08-machines-production/index.md)**. The base tile-entity classes ([`BasicInventoryTileEnt`](../08-machines-production/framework.md#2-basicinventorytileent), [`BasicOwnedTileEnt`](../08-machines-production/framework.md#3-ownership-and-security-basicownedtileent-itgtileentsecurity), [`BasicPoweredTileEnt`](../08-machines-production/framework.md#4-energy-basicpoweredtileent), [`BasicMachineTileEntity`](../08-machines-production/framework.md#7-recipe-machine-state-basicmachinetileentity), the multiblock bases), the base GUIs ([`GuiTechgunsTileEntBase`](../08-machines-production/gui-handler.md#2-guitechgunstileentbase-abstract-guicontainer), [`GuiTechgunsPoweredTileEnt`](../08-machines-production/gui-handler.md#3-guitechgunspoweredtileent-base-gui-for-every-powered-machine)) and [`TechgunsGuiHandler`](../08-machines-production/gui-handler.md#1-techgunsguihandler-complete-gui-id-table) are also [08](../08-machines-production/index.md)'s.

## 1. `BasicMachine` metas owned by area 09

Registry name [`basicMachine`](../07-blocks/block-catalogue.md#registered-blocks), item block [`ItemBlockMetaDataMachine`](../07-blocks/special-blocks.md#16-itemblockmetadatamachine). The unlocalized item name is `techguns.tile.BasicMachine_<meta>.name`.

| Meta | Tile entity (registry id) | English name (lang key `techguns.tile.BasicMachine_N.name`) | GUI id | In creative list | Doc |
|---|---|---|---|---|---|
| 2 | [`TurretBaseEnt`](turret.md#tile-entity-turretbaseent) (`TurretBaseEnt`) | Turret | 2 | yes | [turret.md](turret.md) |
| 3 | [`CamoBenchTileEnt`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) (`CamoBenchTileEnt`) | Camo Bench | 3 | yes | [camo-bench.md](camo-bench.md) |
| 5 | [`RepairBenchTileEnt`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) (`RepairBenchTileEnt`) | Repair Bench | 5 | yes | [repair-bench.md](repair-bench.md) |
| 6 | [`ExplosiveChargeTileEnt`](explosive-charges.md#nbt-explosivechargetileent) (`ExplosiveChargeTileEnt`) | Explosive Charge | 6 | yes | [explosive-charges.md](explosive-charges.md) |
| 7 | `CampFlagTileEnt` (`CampFlagTileEnt`) | Flag | none | **no** (excluded) | [misc-tile-entities.md](misc-tile-entities.md) |
| 8 | [`ExplosiveChargeAdvTileEnt`](explosive-charges.md) (`ExplosiveChargeAdvTileEnt`) | Advanced Explosive Charge | 6 (shared) | yes | [explosive-charges.md](explosive-charges.md) |
| (10) | [`ScrapperTileEnt`](scrapper.md) (`ScrapperTileEnt`) | none | 10 | no, never created | [scrapper.md](scrapper.md) |

Block properties that matter for these metas:

- **Material** iron, **hardness** 4.0, **resistance** 20, metal step sound, pickaxe harvest level 1 for metas 0–5 and 7.
- **Metas 6 and 8 (charges):** hardness 0.1 and harvestable by hand. Their selection and collision box is a thin plate on the face they were planted on (see explosive-charges.md).
- **Meta 7 (flag):** it drops one **iron ingot**, not itself. Right-clicking it does nothing, and it cannot be wrenched.
- `isNormalCube` is true for metas ≤ 5, so the turret, camo bench and repair bench are normal cubes. It is false for 6–9.
- The render type is -1 (no static block model), so everything is drawn by a TESR. The particle icon is `techguns:screen_top`, owned by 07.
- `canCreatureSpawn` is false.
- **Interaction:** a wrench (any held item whose tool classes contain `wrench`) on metas 2/3/5 rotates the block without sneaking. With sneaking it dismantles the block into an item that carries NBT `TileEntityData`, holding the full tile data. Metas 6/7/8 ignore wrenches. Otherwise the GUI with id = meta opens. Charges open GUI 6 only while not armed. Powered tiles (the turret) refuse with chat `techguns.container.security.denied` ("[DENIED] You have no permission to use") when the security check fails.
- **Placing** an item that has `TileEntityData` restores that data and sets rotation to `(yaw-facing + 2) mod 4`. Placing a turret also spawns its turret entity (see turret.md).
- **Breaking:** every `BasicInventoryTileEnt` spills its inventory. A turret base also kills its mounted turret entity. A camp flag also spawns a helicopter (see misc-tile-entities.md).
- **Item tooltip** (`ItemBlockMetaDataMachine`): if the stack has `TileEntityData`, it adds the line `techguns.block.hasTileEntityData` = "Has TileEntity Data".
- **Placement rotation** (`ItemBlockMetaDataMachine.placeBlockAt`):
  - Metas 3 and 5 take the player's 4-way facing, swap 0↔2, then set rotation = (facing − 2) using Java's signed remainder. That gives rotations 0, −1, −2 or 1. Negative values render correctly, because they are multiplied by 90°.
  - Metas 6 and 8 store the clicked face as `orientation` and play the charge's *plant* sound.

### Recipes for area-09 blocks (shaped, ore-dictionary aware; defined in `TGItems`)

| Output | Grid (rows) | Key |
|---|---|---|
| Turret ×1 (meta 2) | `pip` / `cec` / `prp` | p = [`plateIron`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo), i = `circuitBasic`, c = concrete block (meta 0, 07), e = Electric Engine ([06](../06-items/index.md)), r = redstone |
| Camo Bench ×1 (meta 3) | `ddd` / `ici` / `iii` | d = any `dye`, c = crafting table, i = [`nuggetIron`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) |
| Repair Bench ×1 (meta 5) | `pmp` / `ici` / `iii` | p = `plateIron`, m = Mechanical Parts ([06](../06-items/index.md)), c = crafting table, i = `nuggetIron` |
| Explosive Charge ×8 (meta 6) | `rtr` / `tct` / `rtr` | r = `itemRubber`, t = TNT, c = `circuitBasic` |
| Advanced Explosive Charge ×4 (meta 8) | `rtr` / `tct` / `rtr` | r = `sheetPlastic`, t = TGX explosive item ([06](../06-items/index.md)), c = `circuitBasic` |

The ore drill and chest recipes are in their own files.

### Item rendering (inventory and hand)

[`RenderMachineBlockItem`](../08-machines-production/rendering.md#5-machine-item-rendering) (an `IItemRenderer` registered for the `basicMachine` item, owned by [08](../08-machines-production/index.md)) chooses a per-meta renderer. These are the entries for 09's metas:

| Meta | Renderer | Model | Texture |
|---|---|---|---|
| 2 | `RenderBlockModelItem` | [`ModelTurretBase`](../07-blocks/block-rendering-and-models.md#modelturretbase-uv-6464-texture-turretbasepng) | [`textures/blocks/TurretBase.png`](../07-blocks/textures.md#11-model-textures) |
| 3 | `RenderBlockModelItem` | tesselated cube ([`PaintMachineTesselatorCube`](../07-blocks/block-rendering-and-models.md#tesselatorcube-family-fixed-uvs-for-tesrs-and-items)) | [`textures/blocks/paintmachine_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) |
| 5 | `RenderBlockModelItem` | tesselated cube ([`RepairBenchTesselatorCube`](../07-blocks/textures.md#camo-bench-and-repair-bench-09)) | [`textures/blocks/repairbench_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) |
| 6 | `RenderBlockModelItemTNTCharge` | [`ModelTNTCharge`](../07-blocks/block-rendering-and-models.md#modeltntcharge-uv-3232-textures-tntchargepng--tntcharge_2png) | [`textures/blocks/tntCharge.png`](../07-blocks/textures.md#11-model-textures) |
| 7 | `RenderBlockModelItem` | **repair-bench cube** (wrong, see [MUTL-40](index.md#suspected-bugs)) | `textures/blocks/repairbench_textures.png` |
| 8 | `RenderBlockModelItemTNTCharge` | [`ModelExpCharge`](../07-blocks/block-rendering-and-models.md#modelexpcharge-uv-3232-textures-expchargepng--expcharge_2png) | [`textures/blocks/ExpCharge.png`](../07-blocks/textures.md#11-model-textures) |
| 10 | `RenderBlockModelItem` | repair-bench cube (placeholder) | `textures/blocks/repairbench_textures.png` |

`RenderBlockModelItemTNTCharge` uses its own per-view transforms:

| View | Transform |
|---|---|
| First person | translate (0.3, −1.7, −0.5), scale 1.2, yaw −122.5°, then translate (−0.2, −0.2, 0) |
| Equipped | translate (0.4, −1.9, −0.4), yaw −45°, scale 1.5 |
| Ground | scale 1.5, translate (0, −1, 0.3) |
| Icon | translate (0, −1.2, 0), yaw 135°, scale 1.5 |
| Base flip | 180° about X |

`RenderBlockModelItem` (generic, 08) uses:

| View | Transform |
|---|---|
| First person | translate (0.3, −1.7, −0.5), scale 0.9 |
| Equipped | translate (0.5, −1.6, −0.5) |
| Ground | translate (0, −1, 0) |
| Icon | translate (0, −1, 0) |
| Base flip | 180° about X |

## 2. `RenderTileEntityBlock` + `TesselatorCube` (camo bench and repair bench)

`RenderTileEntityBlock` is a TESR that draws one unit cube with the Tessellator, taking each face's UVs from a [`TesselatorCube`](../07-blocks/block-rendering-and-models.md#tesselatorcube-family-fixed-uvs-for-tesrs-and-items) subclass. At draw time it:

1. Translates to the block corner.
2. Rotates about the block's vertical centre axis by `90° × rotation`, where `rotation` is `BasicInventoryTileEnt.rotation`.
3. Sets the tessellator colour to the block's light value and the lightmap coordinates to the block's combined sky/block brightness.
4. Draws all six faces in one `startDrawingQuads`/`draw` pass.

`TesselatorCube` holds a texture `ResourceLocation` plus `(u0,u1,v0,v1)` rectangles for top, bottom, front, back, left and right, each defaulting to the full texture. Face geometry:

- **Top** at y = 1.
- **Bottom** at y = 0.
- **Front** in the plane z = 0, so it faces north (−Z) at rotation 0.
- **Back** at z = 1.
- **Left** at x = 0.
- **Right** at x = 1.

Both bench cubes use the same 2×2 quadrant atlas:

| Face | UV rectangle | Quadrant |
|---|---|---|
| Top | u 0–0.5, v 0.5–1 | bottom-left |
| Front | u 0.5–1, v 0–0.5 | top-right |
| Back, left, right, bottom | u 0–0.5, v 0–0.5 | top-left |
| — | — | bottom-right quadrant unused |

[`ModelBaseTesselator`](../07-blocks/block-rendering-and-models.md#modelmachine-and-modelbasetesselator) wraps a `TesselatorCube` as a `ModelBase` for the item renderer. It flips 180° about X and Y and offsets by (−0.5, −1.5, −0.5).

## 3. `RenderBlockTileEntModel` (turret base and camp flag)

`RenderBlockTileEntModel` is a TESR that draws a `ModelBase` with one texture:

1. Translates to the block centre, raised by 1.5 (x + 0.5, y + 1.5, z + 0.5).
2. Applies `TGRenderHelper.adjustLightFixture`, which does the same light setup as above.
3. Binds the texture. If the tile is a `TurretBaseEnt`, it binds the texture chosen by the turret's armour instead (see turret.md).
4. Rotates 180° about Z, the standard "model upside-down" fix.
5. Renders the model at scale 1/16.

The tile's rotation is ignored. A private helper that would rotate by metadata is never called.

| Tile | Model | Texture |
|---|---|---|
| `TurretBaseEnt` | `ModelTurretBase` | `textures/blocks/TurretBase.png` (or an armour variant) |
| `CampFlagTileEnt` | [`ModelFlagPost`](../07-blocks/block-rendering-and-models.md#modelflagpost-uv-6464-texture-flagpostpng) | [`textures/blocks/flagpost.png`](../07-blocks/textures.md#11-model-textures) |

## 4. Box-UV convention used in the texture descriptions

All `ModelRenderer` boxes use the standard Minecraft box unwrap. For a box of size w × h × d with texture offset (u, v), in model texture units:

- Top face: at (u+d, v), size w × d.
- Bottom face: at (u+d+w, v), size w × d.
- Side strip: starts at row v+d, height h, laid out left to right as `[d (−X side) | w (front, −Z) | d (+X side) | w (back, +Z)]`, in model space before the 180° flip.

When a model declares `textureWidth/Height` smaller than the PNG (for example a 32-unit model with a 64 px PNG), every unit is 2 px. Region coordinates in this area's docs are given in **model units** unless marked "px".
