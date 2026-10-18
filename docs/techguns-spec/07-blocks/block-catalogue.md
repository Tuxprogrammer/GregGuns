# 07-blocks: block catalogue

Every block constructed in `java/techguns/TGBlocks.java`, with its registration data. Behaviour beyond plain properties is in [special-blocks.md](special-blocks.md). Rendering is in [block-rendering-and-models.md](block-rendering-and-models.md). Textures are in [textures.md](textures.md).

## Conventions and defaults

**`GenericBlock` defaults.** Most Techguns blocks extend `techguns.blocks.GenericBlock`. Its constructor sets:

- creative tab [`Techguns.tabTechgun`](../01-core/lifecycle-and-registration.md#lifecycle-overview) (owned by [01-core](../01-core/index.md))
- hardness 2.0
- step sound stone
- harvest tool `pickaxe`, level 1, for every metadata
- render type 0 (vanilla cube), changeable with `setRenderType(int)` (used by the client proxy to plug in custom ISBRH renderers)
- `getRotationMeta(meta, rotations)`, which returns `meta` unchanged. Subclasses override it so structure generation can rotate oriented blocks (see [11-worldgen](../11-worldgen/index.md)). The rotation tables are in [special-blocks.md](special-blocks.md).
- `getUnlocalizedName()` returns `"techguns." + "tile.<name>"`, so lang keys look like `techguns.tile.<name>.name`.

**Harvesting.** The pickaxe-level-1 harvest setting only matters for materials that need a tool (rock, iron). For those, a stone or better pickaxe is needed to get a drop. Cloth and sand materials drop by hand regardless.

**Resistance.** "–" means `setResistance` is never called. Vanilla then raises the internal resistance to hardness × 5, which equals `setResistance(hardness × 5 / 3)`.

**Item blocks.** The item-block classes used are listed below. Their naming rules decide the lang keys (see [lang-and-tooltips.md](lang-and-tooltips.md)).

| Item block class | Name rule | Notes |
|---|---|---|
| vanilla `ItemBlock` | block's unlocalized name | |
| `GenericItemBlockMetadata` (extends `ItemBlockWithMetadata`) | `<block unlocalized>_<damage>` | has subtypes; icon from `block.getIcon(side, damage)` |
| `GenericItemBlockMetadataStringPrefix` | `"techguns." + <GenericItemBlockMetadata name>` | used only by [`ItemBlockOreDrill`](special-blocks.md#17-itemblockoredrill-placement-rules) |
| [`GenericItemBlockCamoChangeable`](special-blocks.md#18-camo-changeable-item-blocks-genericitemblockcamochangeable) | as `GenericItemBlockMetadata` | implements [`ICamoChangeable`](../05-armor/armor-catalogue.md#1-class-hierarchy) (Camo Bench, [09-machines-utility](../09-machines-utility/index.md)) and adds a "Current Camo" tooltip |
| [`ItemBlockMetaDataMachine`](special-blocks.md#16-itemblockmetadatamachine) | `"techguns." + <GenericItemBlockMetadata name>` | machine placement hooks, "Has TileEntity Data" tooltip |
| `ItemBlockTGChest` | `"techguns." + <block unlocalized>` | "Has contents" tooltip |
| `ItemBlockOreClusterCustom` | block's unlocalized name | tooltip shows the ore or fluid of that meta |
| `ItemBlockOreDrill` | prefix variant | placement rules for drill rods and controllers |
| Chisel `ItemCarvable` (external) | see [connected-textures.md](connected-textures.md) | registered by Chisel's `CarvableHelper.registerAll` |

## Registered blocks

Registry names are given without the `Techguns:` mod prefix.

| Registry name | Field / class | Unlocalized (`setBlockName`) | Material | Hardness / resistance | Light | Sound | Harvest | Item block | Metas |
|---|---|---|---|---|---|---|---|---|---|
| `BioBlobSmall` | `bioBlobSmall` / [`Bioblob`](special-blocks.md#13-bio-blob-bioblobsmall-bioblob) (BlockContainer + [`BioblobTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-bioblobtileent)) | `Bioblob` (no `techguns.` prefix) | rock | 0 / 0 (unset) | 0 | stone | cannot be harvested (`canHarvestBlock` false), so no drop | ItemBlock | 0–5 = attached face (set by [04-projectiles-fx](../04-projectiles-fx/index.md)) |
| `camoNetPane` | `camoNetPane` / [`GenericBlockPane`](special-blocks.md#3-camo-net-camonetpane-genericblockpane) (extends vanilla `BlockPane`, drops itself) | `camoNetPane` | cloth | 1.5 / 2.0 | 0 | cloth | none | `GenericItemBlockCamoChangeable` | 0 Woodland, 1 Snow, 2 Desert |
| `camoNetRoof` | `camoNetRoof` / [`GenericBlockVerticalPane`](special-blocks.md#4-camo-net-roof-camonetroof-genericblockverticalpane) | `camoNetRoof` | cloth | 2.0 / – | 0 | cloth | pickaxe 1 (not needed for cloth) | `GenericItemBlockCamoChangeable` | 0 Woodland, 1 Snow, 2 Desert |
| `screen` | `screen` / `GenericBlockDirectional("screen", overlay=true)` | `screen` | iron | 2.0 / – | 0 | stone | pickaxe 1 | ItemBlock | 2–5 facing |
| `controlpanel` | `controlpanel` / `GenericBlockDirectional("controlpanel", overlay=true)` | `controlpanel` | iron | 2.0 / – | 0 | stone | pickaxe 1 | ItemBlock | 2–5 facing |
| `basicMachine` | `basicMachine` / `machines.BasicMachine` | [`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) | – | – | – | – | – | `ItemBlockMetaDataMachine` | 0–9 machines. **Owned by [08](../08-machines-production/index.md)/09.** The item block is described in [special-blocks.md](special-blocks.md#16-itemblockmetadatamachine). |
| `oreCluster` | `oreCluster` / `OreCluster` | `oreCluster` | rock | −1 (unbreakable) / 6,000,000 | 0 | stone | crowbar only (see special) | `GenericItemBlockMetadata` | 0–15 ore types |
| `oreCluster2` | `oreCluster2` / `OreCluster` | `oreCluster2` | rock | −1 / 6,000,000 | 0 | stone | crowbar only | `GenericItemBlockMetadata` | 0–4 ore types |
| `oreClusterCustom` (only if config `addCustomOreCluster`) | `OreClusterCustom` | `oreClusterCustom` | rock | −1 / 6,000,000 | 0 | stone | crowbar only | `ItemBlockOreClusterCustom` | 0–15, filled by MineTweaker |
| `oreClusterCustom2` (only if config `addCustomOreCluster2`) | `OreClusterCustom` | `oreClusterCustom2` | as above | | | | | `ItemBlockOreClusterCustom` | 0–15 |
| `ladder01` | `ladder01` / [`BlockTGLadder`](special-blocks.md#6-metal-ladder-ladder01-blocktgladder) | `LadderBlock` | iron | 10.0 / 1000 (the TGBlocks values override the constructor's 2.0) | 0 | metal | pickaxe 1 | `GenericItemBlockCamoChangeable` | `variant*4 + facing`. Variants: 0 grey, 1 rusty, 2 shiny, 3 carbon/black. Facings 0–3. |
| `lamp01` | `lamp01` / [`BlockTGLamp`](special-blocks.md#7-lamps-and-lanterns-lamp01-blocktglamp) | `Lamp` | iron | 0.25 / – | 15 (`setLightLevel(1.0)`), light opacity 0 | glass | pickaxe 1 (iron material, so a pickaxe is needed for the drop) | `GenericItemBlockMetadata` | 0–6 yellow lamp, 7 yellow lantern, 8–14 white lamp, 15 white lantern |
| `door01` | `door01` / [`BlockTGDoor`](special-blocks.md#8-metal-door-door01-blocktgdoor) (extends vanilla `BlockDoor`) | **none set** (vanilla gives `tile.null`) | iron | 20.0 / 2000 | 0 | vanilla door (stone) | vanilla door rules | ItemBlock (raw block item; the real placer is [`TGItems.door01`](../06-items/item-catalogue.md), [06-items](../06-items/index.md)) | vanilla door metas |
| `sandbags` | `sandbags` / [`BlockSandbags`](special-blocks.md#5-sandbags-blocksandbags) | `sandbags` | cloth | 2.0 / 10.0 | 0 | stone (GenericBlock default) | none needed | ItemBlock | 0 |
| `bugnesteggs` | `bugnesteggs` / `GenericBlock` | `bugnesteggs` | sand | 3.0 / 2.0 | `setLightLevel(5.0f)`, which gives **75** (bug [BLK-10](index.md#suspected-bugs)) | stone | none needed | ItemBlock | 0 |
| `bugnestslimy` | `bugnestslimy` / `BlockBugSlime` (extends vanilla `BlockLadder`) | `slimetrail` | circuits (vanilla ladder material) | 3.0 / 2.0 | 0 | cloth | vanilla ladder | ItemBlock | vanilla ladder facings 2–5 |
| `airMarker` | `airMarker` / [`BlockAirMarker`](special-blocks.md#15-air-marker-airmarker-blockairmarker) | `AirMarker` | cloth | 1.0 / – | 0 (`useNeighborBrightness`) | cloth | none needed | ItemBlock | 0 |
| `oreTitanIron` | `oreTitanIron` / `GenericBlockMetadata(rock, 2)` | `oreTitanIron` | rock | 4.0 / 7.0 | meta 0: 3, meta 1: 5 (per-meta `getLightValue`) | stone | meta 0: pickaxe **3**; meta 1: pickaxe **2** | `GenericItemBlockMetadata` | 0 Titanium Iron Ore, 1 Uranium Ore |
| `TGMonsterSpawner` | `monsterspawner` / `BlockTGMonsterSpawner(rock, 2)` (BlockContainer + [`TGSpawnerTileEnt`](../10-entities-npcs/spawning.md#3-monster-spawner-blocks-tgspawnertileent)) | `TGMonsterSpawner` | rock | 8.0 / – | 0 | stone | pickaxe 1 | `GenericItemBlockMetadata` | 0 Bug Hole, 1 Zombie Hole |
| `acid` | `fluidBlockAcid` / [`BlockFluidAcid`](special-blocks.md#14-creeper-acid-acid-blockfluidacid) (Forge `BlockFluidClassic`) | `creeperAcid` | water | unbreakable | 0 | – | – | ItemBlock | fluid levels |
| `tgchest` | `tgchest` / `BlockTGChest(0)` (BlockContainer + [`TGChestTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-tgchesttileent)) | `tgchest` | wood | 5.0 / 500 | 0 | wood | none | `ItemBlockTGChest` | 2–5 facing |
| `tgchest_weapon` | `tgchest_weapon` / `BlockTGChest(1)` | `tgchest_weapon` | wood | 5.0 / 500 | 0 | wood | none | `ItemBlockTGChest` | 2–5 facing |
| `container` | `container` / `GenericBlockCTM` | `container` (Chisel renames it `chisel.container`) | iron | 1.5 / 3.0 | 0 | piston | pickaxe 1 | Chisel `ItemCarvable` | 0–5, see below |
| `concrete` | `concrete` / `GenericBlockCTM` | `concrete` (becomes `chisel.concrete`) | rock | 3.0 / 10.0 | 0 | stone | pickaxe 1 | Chisel | 0–6 |
| `metalpanel` | `metalpanel` / `GenericBlockCTM` | `metalpanel` | iron | 2.0 / 8.0 | 0 | piston | pickaxe 1 | Chisel | 0–7 |
| `metalpanel2` | `metalpanel2` / `GenericBlockCTM` | `metalpanel2` | iron | 2.0 / 8.0 | 0 | piston | pickaxe 1 | Chisel | 0–6 |
| `bugnest` | `bugnest` / `BlockBugSand` (extends `GenericBlockCTM`) | `bugnest` | sand | 2.0 / 2.0 | 0 | sand | none needed | Chisel | 0 |
| `oreDrill` | `oreDrill` / `machines.OreDrillBlock` | `oreDrill` | – | – | – | – | – | `ItemBlockOreDrill` | **Owned by 08.** Variations 0–4 and 8–12 are registered in TGBlocks (texture list below). |
| `machineMultiBlock` | `machineMultiBlock` / `machines.MultiBlockMachineBlock` | – | – | – | – | – | – | `ItemBlockMetaDataMachine` | **Owned by 08.** Variations 0–5 and 15 (texture list below). |

Drops are "itself, same metadata" (`damageDropped(meta) = meta`) unless noted here:

- **Bioblob:** nothing.
- **Ore clusters:** nothing (`getItemDropped` returns null).
- **Camo net pane and roof:** always drop meta 0, so the variant is lost (bug [BLK-04](index.md#suspected-bugs)).
- **Ladder:** drops `meta − meta%4`, i.e. the variant's item damage (0, 4, 8 or 12).
- **Lamp:** metas 0–6 drop 0, 7 drops 7, 8–14 drop 8, 15 drops 15.
- **Door:** drops `TGItems.door01` except for upper-half meta 8. Upper-half meta 9 also drops one (bug [BLK-01](index.md#suspected-bugs)).
- **Monster spawner:** meta 0 drops `bugnest:0`. Meta 1 drops `concrete:1`, the dark variant, even though it looks like `concrete:0` (bug [BLK-12](index.md#suspected-bugs)).
- **Chest:** drops itself plus its inventory contents.
- **Acid:** nothing.

### Metadata variants of the Chisel-style blocks

All are registered through `carverHelper.addVariation(descKey, meta, textureBase, "techguns")`. The texture type is picked from the files that exist (see [connected-textures.md](connected-textures.md)).

| Block | Meta | Texture base | Texture type | Lang description (tooltip) |
|---|---|---|---|---|
| container | 0 | `container_red` | CTM | Shipping Container (Red) |
| container | 1 | `container_green` | CTM | Shipping Container (Green) |
| container | 2 | `container_blue` | CTM | Shipping Container (Blue) |
| container | 3 | `container_orange` | CTM | Shipping Container (Orange) |
| container | 4 | `container_scifi1` | CTM | Futuristic Container |
| container | 5 | `container_scifi_rusty` | CTM | Futuristic Container (Rusty) |
| concrete | 0 | `concrete01` | plain | Light |
| concrete | 1 | `concrete02` | plain | Dark |
| concrete | 2 | `concrete_r` | CTM | Steel Frame Light |
| concrete | 3 | `concrete_r02` | CTM | Steel Frame Dark |
| concrete | 4 | `concrete_old2` | CTMV (pillar) | Weathered Column |
| concrete | 5 | `concrete03` | plain | Light Noise |
| concrete | 6 | `concrete04` | plain | Dark Noise |
| metalpanel | 0 | `metalpanel_rusty` | plain | Rusty |
| metalpanel | 1 | `metalpanel_rusty2` | plain | Rusty 2 |
| metalpanel | 2 | `metalpanel_hazard` | plain | Hazard |
| metalpanel | 3 | `metalpanel_dark` | plain | Dark |
| metalpanel | 4 | `metalgrate_rusty` | plain | Rusty Grate |
| metalpanel | 5 | `metalgrate_rusty2` | plain | Rusty Grate 2 |
| metalpanel | 6 | `metalgrate_hazard` | plain | Hazard Grate |
| metalpanel | 7 | `metalgrate_dark` | plain | Dark Grate |
| metalpanel2 | 0 | `metalpanel2_rusty` | CTM | Rusty |
| metalpanel2 | 1 | `metalpanel2_hazard` | CTM | Hazard |
| metalpanel2 | 2 | `metalpanel16_largeborder` | CTM | Silver Bars |
| metalpanel2 | 3 | `steelframe_dark` | CTM | Dark Bars |
| metalpanel2 | 4 | `steelframe_blue` | CTM | Shiny |
| metalpanel2 | 5 | `steelframe_scaffold` | CTM | Dark |
| metalpanel2 | 6 | `metalpanel_techgrid` | V9 (3×3 pattern) | Silver |
| bugnest | 0 | `bugnestblock` | plain | Bugnest Sand |

The lang descriptions for metalpanel2 metas 2–6 do not match the art very well (e.g. "Shiny" is a blue-grey steel frame). They are recorded verbatim.

**Machine variations registered here (owned by [08](../08-machines-production/index.md)).** All have an empty description and `forbidChiseling = true`.

| Block | Meta → texture base |
|---|---|
| `oreDrill` | 0 `drillFrame_outer`, 1 `drillFrame`, 2 `drillRod` (CTMV), 3 `drillEngine`, 4 `drillEngineController`, 8 `drillFrame_outer_c` (CTM), 9 `drillFrame_c` (CTM), 10 `invisible`, 11 `drillEngine_c` (CTM), 12 `drillEngineTop_c` (CTM). Meta 5 (`drillRodSide`) is commented out. |
| `machineMultiBlock` | 0 `rc_block`, 1 `rc_controller`, 2 `rc_glass`, 3 `fabricatorHousing`, 4 `fabricatorController`, 5 `fabricatorGlass`, 15 `invisible`. Metas 8–11 (`invisible`) are commented out. |

## Ore dictionary (registered in TGBlocks)

| Ore name | Stack |
|---|---|
| `oreTitaniumIron` | `oreTitanIron:0` |
| `oreIllmenite` (sic; the mineral is spelled "ilmenite", bug [BLK-19](index.md#suspected-bugs)) | `oreTitanIron:0` |
| `oreUranium` | `oreTitanIron:1` |

`TGBlocks.oreUranium` is set to `oreTitanIron:1` straight after registration. `postInit` only fills ore references that are still null, so other mods' uranium is never used by the ore cluster ([BLK-25](index.md#suspected-bugs)).

**Ore references resolved in `postInit`.** Each reference is filled with the first `OreDictionary` entry, stack size 1, if it is still null. The names are `oreCopper`, `oreTin`, `oreLead`, `oreOsmium`, `oreNickel`, `oreSilver`, `oreAluminium`, `orePlatinum`, `oreUranium` and `oreCertusQuartz`. They feed the ore-cluster type tables in [special-blocks.md](special-blocks.md#11-ore-clusters-orecluster-orecluster2-oreclustercustom-oreclustercustom2).

`TGBlocks.postInit` also calls `door01.setDoorItem(TGItems.door01)`, which links the block to its item ([06-items](../06-items/index.md)).

## Recipes

`TGBlocks` registers **no** recipes. Recipes that produce these blocks are registered in `TGItems` and documented in [06-items/recipes.md](../06-items/recipes.md):

| Block | Recipe |
|---|---|
| Metal Ladder ×16 | B11 |
| Lamps and lanterns | B12–B15 |
| Shipping Container (Red) ×8 | B21 |
| Sandbags ×8 | B22 |
| Camo Net ×8 | B23 |
| Camo Net Roof ×16 | B24 |
| Metal Panel ×32 | B31 |
| Metal Panel (Connected) ×64 | B32 |
| Reinforced Chest | B33 |
| Weapon Chest | B34 |
| Door item (2) | from 6 [`plateIron`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) ([06-items](../06-items/index.md)) |

Concrete has no crafting recipe. It comes from the Chemical Laboratory (sand + gravel + 250 mB water → 2 `concrete:0`, [08-machines-production](../08-machines-production/index.md)). The other Chisel variants are reachable only with the Chisel mod's chisel (the carving groups are named `container`, `concrete`, `metalpanel`, `metalpanel2`, `bugnest`). The screen, control panel, steel-frame variants, bug blocks, monster spawner, air marker and Bioblob have no recipes. They come from world generation, the creative tab or gameplay.

## Blocks constructed but not registered (not in game)

| Field | Class | Properties | Why absent |
|---|---|---|---|
| `lamp01FMP` | [`BlockTGLampFMP`](multipart.md#status-inactive) | glass, hardness 0.25, glass sound, pickaxe 1, texture `techguns:lamp01`, light 15, opacity 0, meta = clicked side, thin 0.4×0.4×0.1 plate against the support | `registerBlock` commented out. It was a ForgeMultipart test (see [multipart.md](multipart.md)). |
| `smallCube` | `GenericBlock` | iron, vanilla `redstone_block` texture, bounds 0.2–0.8 cube | registration commented out (multipart test) |
| `steelframe_glass` | `GenericBlock` | glass, hardness 4, resistance 20, texture `techguns:steelframe_glass` | registration commented out |

The unused classes `TurretBase`, `BlockBugNest` and `ItemBlockOreCluster` are described at the end of [special-blocks.md](special-blocks.md#20-unused-classes).

## Ore blocks registered outside TGBlocks (for texture coverage)

`Techguns.java` ([01-core](../01-core/index.md)) builds three plain `GenericBlock` ores when the config flags allow it. They are rock material with GenericBlock defaults, and `techguns.tile.oreX.name` keys exist for them:

| Block | Texture | Config flag |
|---|---|---|
| `oreLead` | `techguns:oreLeadBlock` | [`doOreGenLead`](../01-core/config.md#category-general) |
| `oreTin` | `techguns:oreTinBlock` | [`doOreGenCopperTin`](../01-core/config.md#category-general) |
| `oreCopper` | `techguns:oreCopperBlock` | `doOreGenCopperTin` |

Their textures are catalogued in [textures.md](textures.md). Registration, oredict and smelting are in [01-core/lifecycle-and-registration.md](../01-core/lifecycle-and-registration.md).
