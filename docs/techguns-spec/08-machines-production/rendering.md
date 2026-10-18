# Shared machine renderers

Per-machine animation is described in each machine's file. This file covers the shared TESR and item-renderer plumbing in `client/renderer/tileentity/` and `client/renderer/item/`.

## 1. TESR bindings (`ClientProxy`)

| Tile | Renderer | Model | Texture |
|---|---|---|---|
| [`AmmoPressTileEnt`](framework.md#1-class-hierarchy) | `RenderAmmoPress` | [`ModelAmmoPress`](../07-blocks/block-rendering-and-models.md#modelammopress-uv-12864-texture-ammopresspng) | [`textures/blocks/AmmoPress.png`](../07-blocks/textures.md#11-model-textures) |
| [`MetalPressTileEnt`](framework.md#1-class-hierarchy) | `RenderAmmoPress` | [`ModelMetalPress`](../07-blocks/block-rendering-and-models.md#modelmetalpress-uv-12864-texture-metalpresspng) | [`textures/blocks/MetalPress.png`](../07-blocks/textures.md#11-model-textures) |
| [`ChemLabTileEnt`](framework.md#1-class-hierarchy) | `RenderAmmoPress` | [`ModelChemLab`](../07-blocks/block-rendering-and-models.md#modelchemlab-uv-12864-texture-chemlabpng) | [`textures/blocks/ChemLab.png`](../07-blocks/textures.md#11-model-textures) |
| [`ChargingStationTileEnt`](framework.md#1-class-hierarchy) | [`RenderChargingStation`](charging-station.md) | [`ModelChargingStation`](../07-blocks/block-rendering-and-models.md#modelchargingstation-uv-6464-texture-chargingstationpng) | [`textures/blocks/ChargingStation.png`](../07-blocks/textures.md#11-model-textures) |
| [`FabricatorTileEntMaster`](framework.md#1-class-hierarchy) | [`RenderFabricator`](fabricator.md#7-visuals-renderfabricator) | [`ModelFabricator`](../07-blocks/block-rendering-and-models.md#modelfabricator-uv-128128-texture-fabricatorpng) | [`textures/blocks/Fabricator.png`](../07-blocks/textures.md#11-model-textures) |
| [`ReactionChamberTileEntMaster`](framework.md#1-class-hierarchy) | [`RenderReactionChamber`](reaction-chamber.md#7-visuals-renderreactionchamber) | OBJ [`models/entity/reactionchamber.obj`](../10-entities-npcs/rendering-and-models.md#other-obj-files-in-modelsentity-in-this-folder-used-elsewhere) | [`textures/entity/reactionchamber.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) + [`textures/entity/reactionchamberglass.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) + [`textures/fx/heatray.png`](../04-projectiles-fx/textures.md#single-images) |
| [`TurretBaseEnt`](../09-machines-utility/turret.md#tile-entity-turretbaseent), [`CampFlagTileEnt`](../09-machines-utility/shared-rendering.md#1-basicmachine-metas-owned-by-area-09) | `RenderBlockTileEntModel` | ([09](../09-machines-utility/index.md)) | ([09](../09-machines-utility/index.md)) |
| [`CamoBenchTileEnt`](blocks-and-items.md#1-basicmachine-registry-name-basicmachine), [`RepairBenchTileEnt`](blocks-and-items.md#1-basicmachine-registry-name-basicmachine) | `RenderTileEntityBlock` | tesselator cubes ([09](../09-machines-utility/index.md)) | ([09](../09-machines-utility/index.md)) |
| explosive charges, ore drill, TG chest | [`RenderExplosiveCharge`](../09-machines-utility/explosive-charges.md#world-rendering-renderexplosivecharge), [`RenderOreDrill`](../09-machines-utility/ore-drill.md#9-tesr-renderoredrill), [`RenderTGChest`](../09-machines-utility/misc-tile-entities.md#1-reinforced-chest-and-weapon-chest) | ([09](../09-machines-utility/index.md)) | ([09](../09-machines-utility/index.md)) |

The textures under `textures/blocks/` are cataloged in [`07-blocks`](../07-blocks/index.md). The model classes live in `client/models/blocks/`. [`ModelMachine`](../07-blocks/block-rendering-and-models.md#modelmachine-and-modelbasetesselator) is their abstract base: it adds a `render(..., progress)` overload, and the plain render calls it with progress 0.

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

Translates to the block corner, rotates `90° × rotation` about the block's vertical center axis, applies the block light value and sky light to the tessellator and lightmap, then draws a [`TesselatorCube`](../07-blocks/block-rendering-and-models.md#tesselatorcube-family-fixed-uvs-for-tesrs-and-items).

## 5. Machine item rendering

- **`RenderMachineBlockItem`** is the `IItemRenderer` registered for the [`basicMachine`](../07-blocks/block-catalog.md#registered-blocks) item. It holds an array of `RenderMachineBlockItemBase` delegates and forward every call to `renderers[itemDamage]`. Damage values outside 0..10 throw an index error ([MPRD-24](index.md#suspected-bugs)).

| Meta | Delegate | Texture | Model |
|---|---|---|---|
| 0 | [`RenderBlockModelItem`](../09-machines-utility/shared-rendering.md#item-rendering-inventory-and-hand) | `textures/blocks/AmmoPress.png` | `ModelAmmoPress` |
| 1 | `RenderBlockModelItem` | `textures/blocks/MetalPress.png` | `ModelMetalPress` |
| 2 | `RenderBlockModelItem` | [`textures/blocks/TurretBase.png`](../07-blocks/textures.md#11-model-textures) | [`ModelTurretBase`](../07-blocks/block-rendering-and-models.md#modelturretbase-uv-6464-texture-turretbasepng) |
| 3 | `RenderBlockModelItem` | [`textures/blocks/paintmachine_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | tesselator model (camo bench) |
| 4 | `RenderBlockModelItem` | `textures/blocks/ChemLab.png` | `ModelChemLab` |
| 5 | `RenderBlockModelItem` | [`textures/blocks/repairbench_textures.png`](../07-blocks/textures.md#camo-bench-and-repair-bench-09) | tesselator model (repair bench) |
| 6 | [`RenderBlockModelItemTNTCharge`](../09-machines-utility/shared-rendering.md#item-rendering-inventory-and-hand) | [`textures/blocks/tntCharge.png`](../07-blocks/textures.md#11-model-textures) | [`ModelTNTCharge`](../07-blocks/block-rendering-and-models.md#modeltntcharge-uv-3232-textures-tntchargepng--tntcharge_2png) |
| 7 | `RenderBlockModelItem` | repair bench (placeholder for the flag) | repair bench |
| 8 | `RenderBlockModelItemTNTCharge` | [`textures/blocks/ExpCharge.png`](../07-blocks/textures.md#11-model-textures) | [`ModelExpCharge`](../07-blocks/block-rendering-and-models.md#modelexpcharge-uv-3232-textures-expchargepng--expcharge_2png) |
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

- The [`machineMultiBlock`](../07-blocks/block-catalog.md#registered-blocks) items use normal block-icon rendering (their CTM textures).

## 6. Entity sheet summary for the shared renderers (consistency pass)

| # | Field | Contents |
|---|---|---|
| 1 | Identity | `RenderAmmoPress`, `RenderChargingStation`, `RenderFabricator`, `RenderReactionChamber` (TESRs bound in [`ClientProxy`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy)); `RenderMachineBlockItem` and `RenderMachineBlockItemBase`/`RenderBlockModelItem` (item renderers); model base `ModelMachine` |
| 2 | Obtaining | not applicable (client code) |
| 3 | Numbers | Model scale 1/16 for the `ModelMachine` classes; OBJ scale 1/32 for the Reaction Chamber; item transforms in section 5 |
| 4 | Behavior | Sections 2 to 5; every TESR is client-only and runs each frame; the model animation depends on `getProgress()` |
| 5 | Persistence and sync | none of its own; reads tile fields synced by the description packet (`progress`, `totalTime`, `rotation`, `dir`, `complete`) |
| 6 | UI | none |
| 7 | Visuals | Table in section 1 (model and texture per tile) and section 5 (item delegates). Measured sizes and colors for the machine textures are in each machine file's sheet summary |
| 8 | Audio | none (sounds belong to the tile entities, see [sounds.md](sounds.md)) |
| 9 | Relationships | Models are cataloged in [`07-blocks`](../07-blocks/index.md) ([block-rendering-and-models.md](../07-blocks/block-rendering-and-models.md)); `RenderBlockTileEntModel` and `RenderTileEntityBlock` serve [`09-machines-utility`](../09-machines-utility/index.md) |
| 10 | Bugs | [MPRD-24](index.md#suspected-bugs) (unknown meta crashes the item renderer) |
