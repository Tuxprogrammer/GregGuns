# Shared machine renderers

Per-machine animation is described in each machine's file. This file covers the shared TESR and item-renderer plumbing in `client/renderer/tileentity/` and `client/renderer/item/`.

## 1. TESR bindings (`ClientProxy`)

| Tile | Renderer | Model | Texture |
|---|---|---|---|
| `AmmoPressTileEnt` | `RenderAmmoPress` | `ModelAmmoPress` | `textures/blocks/AmmoPress.png` |
| `MetalPressTileEnt` | `RenderAmmoPress` | `ModelMetalPress` | `textures/blocks/MetalPress.png` |
| `ChemLabTileEnt` | `RenderAmmoPress` | `ModelChemLab` | `textures/blocks/ChemLab.png` |
| `ChargingStationTileEnt` | `RenderChargingStation` | `ModelChargingStation` | `textures/blocks/ChargingStation.png` |
| `FabricatorTileEntMaster` | `RenderFabricator` | `ModelFabricator` | `textures/blocks/Fabricator.png` |
| `ReactionChamberTileEntMaster` | `RenderReactionChamber` | OBJ `models/entity/reactionchamber.obj` | `textures/entity/reactionchamber.png` + `textures/entity/reactionchamberglass.png` + `textures/fx/heatray.png` |
| `TurretBaseEnt`, `CampFlagTileEnt` | `RenderBlockTileEntModel` | (09) | (09) |
| `CamoBenchTileEnt`, `RepairBenchTileEnt` | `RenderTileEntityBlock` | tesselator cubes (09) | (09) |
| explosive charges, ore drill, TG chest | `RenderExplosiveCharge`, `RenderOreDrill`, `RenderTGChest` | (09) | (09) |

The textures under `textures/blocks/` are catalogued in `07-blocks`. The model classes live in `client/models/blocks/`. `ModelMachine` is their abstract base: it adds a `render(..., progress)` overload, and the plain render calls it with progress 0.

## 2. `RenderAmmoPress` (generic "model + rotation + progress" TESR)

1. Push a matrix and translate to (x+0.5, y+1.5, z+0.5), the usual Techne-model origin.
2. Bind the texture.
3. Rotate 180° about Z (flip upright).
4. Rotate `90° × tile.rotation` about Y.
5. Render the model at scale 1/16, passing `tile.getProgress()` (0..1) so the model can animate.

Lighting isn't adjusted. A private light helper exists but is never called. A `adjustRotatePivotViaMeta` helper is also present but does nothing (it pushes and pops a matrix around a rotation).

## 3. `RenderBlockTileEntModel` (used by 09's turret base and flag)

The same transform as above, but without rotation. It calls `TGRenderHelper.adjustLightFixture` (it sets the lightmap from the block's brightness at the tile position). For turret bases it binds the turret's own base texture.

## 4. `RenderTileEntityBlock` (used by 09's camo and repair benches)

Translates to the block corner, rotates `90° × rotation` about the block's vertical centre axis, applies the block light value and sky light to the tessellator and lightmap, then draws a `TesselatorCube`.

## 5. Machine item rendering

- **`RenderMachineBlockItem`** is the `IItemRenderer` registered for the `basicMachine` item. It holds an array of `RenderMachineBlockItemBase` delegates and forwards every call to `renderers[itemDamage]`. Damage values outside 0..10 throw an index error (MPRD-24).

| Meta | Delegate | Texture | Model |
|---|---|---|---|
| 0 | `RenderBlockModelItem` | `textures/blocks/AmmoPress.png` | `ModelAmmoPress` |
| 1 | `RenderBlockModelItem` | `textures/blocks/MetalPress.png` | `ModelMetalPress` |
| 2 | `RenderBlockModelItem` | `textures/blocks/TurretBase.png` | `ModelTurretBase` |
| 3 | `RenderBlockModelItem` | `textures/blocks/paintmachine_textures.png` | tesselator model (camo bench) |
| 4 | `RenderBlockModelItem` | `textures/blocks/ChemLab.png` | `ModelChemLab` |
| 5 | `RenderBlockModelItem` | `textures/blocks/repairbench_textures.png` | tesselator model (repair bench) |
| 6 | `RenderBlockModelItemTNTCharge` | `textures/blocks/tntCharge.png` | `ModelTNTCharge` |
| 7 | `RenderBlockModelItem` | repair bench (placeholder for the flag) | repair bench |
| 8 | `RenderBlockModelItemTNTCharge` | `textures/blocks/ExpCharge.png` | `ModelExpCharge` |
| 9 | `RenderBlockModelItem` | `textures/blocks/ChargingStation.png` | `ModelChargingStation` |
| 10 | `RenderBlockModelItem` | repair bench (placeholder for the scrapper) | repair bench |

- **`RenderMachineBlockItemBase`** (abstract). It handles every render type and uses the render helpers. For each render it:
  1. pushes a matrix and applies `GL_Transform_Translate()`;
  2. applies the transform for the context: first person, equipped (third person), entity (dropped) or inventory icon;
  3. binds the texture and renders the model at scale 1/16 (progress 0, so static);
  4. pops.

  Subclasses supply the five transform hooks.
- **`RenderBlockModelItem`** transforms:

| Hook | Transform |
|---|---|
| Translate (always first) | rotate 180° about X |
| First person | translate (0.3, −1.7, −0.5), scale 0.9 |
| Equipped | translate (0.5, −1.6, −0.5) |
| Entity (dropped) | translate (0, −1, 0) |
| Inventory | translate (0, −1, 0) |

- The `machineMultiBlock` items use normal block-icon rendering (their CTM textures).
