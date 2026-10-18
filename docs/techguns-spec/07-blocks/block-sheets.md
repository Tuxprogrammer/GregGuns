# 07-blocks: block family sheets

One sheet per block family, using the ten CONVENTIONS fields (identity, obtaining, numbers, behaviour, persistence and sync, UI, visuals, audio, relationships, bugs). The tables in [block-catalogue.md](block-catalogue.md) stay the quick reference. [special-blocks.md](special-blocks.md) has the long-form logic, and these sheets restate the facts per family and add what was missing: obtaining, drops, tile entity and NBT, exact tooltips, step and break sounds, and relationships.

**Conventions used in every sheet.**

- **Creative tab:** [`GenericBlock`](block-catalogue.md#conventions-and-defaults) sets [`Techguns.tabTechgun`](../01-core/lifecycle-and-registration.md#lifecycle-overview). A different value is stated.
- **Oredict:** *none* unless stated.
- **Step and break sounds.** Techguns sets only the block's *sound type*. The actual files are vanilla Minecraft 1.7.10 ones (they are not in the Techguns source). The vanilla sound types resolve as follows, and a sound type supplies the walk step, the dig (break) sound and the place sound:

  | Sound type | Step event | Break and place event | Volume / pitch of the type |
  |---|---|---|---|
  | `soundTypeStone` | `step.stone` | `dig.stone` | 1.0 / 1.0 |
  | `soundTypePiston` | `step.stone` | `dig.stone` | 1.0 / 1.0 |
  | `soundTypeMetal` | `step.stone` | `dig.stone` | 1.0 / 1.5 |
  | `soundTypeGlass` | `step.stone` | `dig.glass` | 1.0 / 1.0 |
  | `soundTypeCloth` | `step.cloth` | `dig.cloth` | 1.0 / 1.0 |
  | `soundTypeSand` | `step.sand` | `dig.sand` | 1.0 / 1.0 |
  | `soundTypeWood` | `step.wood` | `dig.wood` | 1.0 / 1.0 |

  The engine plays steps at 0.15 × the type volume, and break and place at (volume + 1) / 2 with pitch × 0.8 (these engine factors are vanilla knowledge, not verified against the Techguns source). The only custom block sound is `techguns:blocks.metaldoorOpen` ([sounds.md](sounds.md)).
- **Recipes** are numbered as in [06-items/recipes.md](../06-items/recipes.md).
- **Not heard.** Descriptions of how a sound *sounds* are not made here. Only triggers, volumes and pitches are recorded.

---

## S1. Shipping containers (`container`, metas 0–5)

| Field | Content |
|---|---|
| Identity | Block [`container`](block-catalogue.md#registered-blocks), registry `Techguns:container`, class [`GenericBlockCTM`](block-catalogue.md#registered-blocks), unlocalized `container` (Chisel renames it to `chisel.container`). Metas 0 Red, 1 Green, 2 Blue, 3 Orange, 4 Futuristic, 5 Futuristic (Rusty). Block name key `techguns.tile.chisel.container.name` = "Container Block". Item block: Chisel `ItemCarvable`. |
| Obtaining | Meta 0 only: recipe B21 (8 for 8 iron ingots plus a chest). Metas 1–5: Chisel's chisel (carving group `container`) or the creative tab. Also placed by structures ([11-worldgen](../11-worldgen/index.md)). |
| Numbers | Material iron, hardness 1.5, resistance 3.0, light 0, harvest pickaxe level 1. |
| Behaviour | Inert full cube with connected textures (see [connected-textures.md](connected-textures.md)). No rotation (`getRotationMeta` returns meta). |
| Persistence and sync | Metadata only. No tile entity, no NBT. |
| UI | Tooltip line from Chisel: the variation description (`techguns.tile.container.<m>.desc`): "Shipping Container (Red)", "(Green)", "(Blue)", "(Orange)", "Futuristic Container", "Futuristic Container (Rusty)". |
| Visuals | Standard cube through Chisel's CTM renderer. Textures: `container_red`, `_green`, `_blue`, `_orange` (16×16 base plus 32×32 `-ctm`) and `container_scifi1`, `container_scifi_rusty` (32×32 base plus 64×64 `-ctm`); see [textures.md](textures.md) §1. |
| Audio | Step `step.stone`, break and place `dig.stone` (piston type, volume 1.0, pitch 1.0). |
| Relationships | Recipe B21 in [06-items](../06-items/index.md). Used by city and bunker structures in [11-worldgen](../11-worldgen/index.md). Chisel is a hard dependency ([connected-textures.md](connected-textures.md)). |
| Bugs | none |

## S2. Concrete (`concrete`, metas 0–6)

| Field | Content |
|---|---|
| Identity | Block [`concrete`](block-catalogue.md#registered-blocks), class `GenericBlockCTM`, unlocalized `concrete` (Chisel: `chisel.concrete`). Metas 0 Light, 1 Dark, 2 Steel Frame Light, 3 Steel Frame Dark, 4 Weathered Column (pillar), 5 Light Noise, 6 Dark Noise. Name key `techguns.tile.chisel.concrete.name` = "Concrete". |
| Obtaining | Meta 0: the Chemical Laboratory recipe (sand + gravel + 250 mB water gives 2, [08-machines-production](../08-machines-production/index.md)). Other metas: Chisel carving group `concrete` or the creative tab. No crafting-table recipe. |
| Numbers | Material rock, hardness 3.0, resistance 10.0, light 0, pickaxe level 1. |
| Behaviour | Inert cube. Meta 4 uses the vertical CTMV column layout (`concrete_old2-ctmv` on the sides, `concrete_old2-top` on top and bottom). Meta 1 is also what the Zombie Hole wrongly drops ([BLK-12](index.md#suspected-bugs)). |
| Persistence and sync | Metadata only. |
| UI | Chisel variation tooltips: "Light", "Dark", "Steel Frame Light", "Steel Frame Dark", "Weathered Column", "Light Noise", "Dark Noise". |
| Visuals | `concrete01`, `concrete02`, `concrete_r`(-ctm), `concrete_r02`(-ctm), `concrete_old2-ctmv` / `-top`, `concrete03`, `concrete04`. See [textures.md](textures.md) §2. Art for metas 5 and 6 is the reverse of their lang names (5 is dark, 6 is light). |
| Audio | Step `step.stone`, break and place `dig.stone` (stone type). |
| Relationships | Machine recipe in 08. Worldgen structures (a [`TGChiselBlocks`](special-blocks.md#19-tgchiselblocks-block-constants-for-structures) constant refers to Chisel's own concrete, not this one: [special-blocks.md](special-blocks.md#19-tgchiselblocks-block-constants-for-structures)). |
| Bugs | [BLK-12](index.md#suspected-bugs) (spawner drop) |

## S3. Metal panels (`metalpanel` 0–7, `metalpanel2` 0–6)

| Field | Content |
|---|---|
| Identity | Blocks [`metalpanel`](block-catalogue.md#registered-blocks) ("Metal Panel", `techguns.tile.chisel.metalpanel.name`) and [`metalpanel2`](block-catalogue.md#registered-blocks) ("Metal Panel (Connected)", `techguns.tile.chisel.metalpanel2.name`), class `GenericBlockCTM`. Variation names are in [block-catalogue.md](block-catalogue.md#metadata-variants-of-the-chisel-style-blocks). |
| Obtaining | `metalpanel:0` x32 by recipe B31; `metalpanel2:0` x64 by recipe B32 (both 8 stone around one copper plate). Other metas: Chisel's chisel (groups `metalpanel`, `metalpanel2`) or the creative tab. |
| Numbers | Material iron, hardness 2.0, resistance 8.0, light 0, pickaxe level 1. |
| Behaviour | Inert cubes. `metalpanel` metas are plain textures. `metalpanel2` metas 0–5 are 2×2 CTM sheets, meta 6 is a 3×3 V9 pattern. |
| Persistence and sync | Metadata only. |
| UI | Chisel variation tooltips: metalpanel "Rusty", "Rusty 2", "Hazard", "Dark", "Rusty Grate", "Rusty Grate 2", "Hazard Grate", "Dark Grate"; metalpanel2 "Rusty", "Hazard", "Silver Bars", "Dark Bars", "Shiny", "Dark", "Silver". |
| Visuals | See [textures.md](textures.md) §3. The unregistered `steelframe_glass` is in the same section. |
| Audio | Step `step.stone`, break and place `dig.stone` (piston type). |
| Relationships | Recipes in [06-items](../06-items/index.md). Structures in [11-worldgen](../11-worldgen/index.md). |
| Bugs | none (lang names for metalpanel2 metas 2–6 fit the art poorly, noted in the catalogue) |

## S4. Hardened Sand (`bugnest`)

| Field | Content |
|---|---|
| Identity | Block [`bugnest`](block-catalogue.md#registered-blocks), class [`BlockBugSand`](block-catalogue.md#registered-blocks) (extends `GenericBlockCTM`), meta 0 "Bugnest Sand" (variation). Name key `techguns.tile.chisel.bugnest.name` = "Hardened Sand". |
| Obtaining | Creative tab, Chisel group `bugnest`, or alien-nest structures ([11-worldgen](../11-worldgen/index.md)). Also the drop of the Bug Hole (S17). No crafting recipe. |
| Numbers | Material sand, hardness 2.0, resistance 2.0, no tool needed. |
| Behaviour | The `updateTick` that would spawn an alien bug on top is unreachable ([BLK-11](index.md#suspected-bugs)). |
| Persistence and sync | Metadata only. |
| UI | Chisel tooltip "Bugnest Sand". |
| Visuals | `bugnestblock` (16×16). |
| Audio | Step `step.sand`, break and place `dig.sand`. |
| Relationships | Bug spawner S17. Mobs in [10-entities-npcs](../10-entities-npcs/index.md). |
| Bugs | [BLK-11](index.md#suspected-bugs) |

## S5. Radar Screen and Control Panel (`screen`, `controlpanel`)

| Field | Content |
|---|---|
| Identity | Blocks [`screen`](block-catalogue.md#registered-blocks) ("Radar Screen", `techguns.tile.screen.name`) and [`controlpanel`](block-catalogue.md#registered-blocks) ("Control Panel", `techguns.tile.controlpanel.name`), class [`GenericBlockDirectional`](special-blocks.md#2-radar-screen-and-control-panel-genericblockdirectional) with overlay on, ISBRH render. Item block vanilla `ItemBlock`. Metas 2–5 = facing. |
| Obtaining | No recipe. Creative tab and structure worldgen ([11-worldgen](../11-worldgen/index.md)). |
| Numbers | Material iron, hardness 2.0, resistance unset (so 10), light 0, pickaxe level 1. |
| Behaviour | Faces the player on placement (yaw quadrant to meta, table in [special-blocks.md](special-blocks.md#2-radar-screen-and-control-panel-genericblockdirectional)). Two render passes. No GUI, no redstone, no tile entity. |
| Persistence and sync | Metadata only. |
| UI | none. No tooltips. |
| Visuals | Base faces `<name>_front/_side/_top` and the animated `<name>_overlay_front` (32×256, 8 frames x 2 ticks); see [textures.md](textures.md) §6. The overlay is drawn only on the front face. |
| Audio | Step `step.stone`, break and place `dig.stone` (stone type). |
| Relationships | Only worldgen. |
| Bugs | [BLK-20](index.md#suspected-bugs) |

## S6. Camo Net (`camoNetPane`)

| Field | Content |
|---|---|
| Identity | Block [`camoNetPane`](block-catalogue.md#registered-blocks), class [`GenericBlockPane`](special-blocks.md#3-camo-net-camonetpane-genericblockpane) (extends vanilla `BlockPane`). Metas 0 Woodland, 1 Snow, 2 Desert. Item block [`GenericItemBlockCamoChangeable`](special-blocks.md#18-camo-changeable-item-blocks-genericitemblockcamochangeable). Names: block `techguns.tile.camoNetPane.name` "Camo Net"; items `techguns.tile.camoNetPane_<m>.name` "Camo Net (Woodland)" / "(Snow)" / "(Desert)". |
| Obtaining | Recipe B23 (stick, dirt, stick over the same row, x8, meta 0 only). Other variants through the Camo Bench ([09-machines-utility](../09-machines-utility/index.md)). The [`camoNetPlacer`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems) item also places them ([06-items](../06-items/index.md)). |
| Numbers | Material cloth, hardness 1.5, resistance 2.0, light 0, no tool needed. |
| Behaviour | Connects to the same block or vanilla panes only. Collision is a 2-px slab. A pole is drawn for isolated, end and L-corner pieces. |
| Persistence and sync | Metadata only. |
| UI | Tooltip "Current Camo: Woodland / Snow / Desert" (`techguns.tile.camoNet.tooltip` plus `techguns.tile.camoNetPane.camo.<m>`). |
| Visuals | Net `camoNet`, `camoNet_snow`, `camoNet_desert` (32×32, 66% opaque); poles are vanilla `planks_oak`, `log_spruce`, `planks_jungle`; edges `invisible`. |
| Audio | Step `step.cloth`, break and place `dig.cloth`. |
| Drops | Always meta 0 ([BLK-04](index.md#suspected-bugs)). |
| Relationships | Burned by flamethrower and blaster projectiles ([04-projectiles-fx](../04-projectiles-fx/index.md)). Camo Bench via [`ICamoChangeable`](../05-armor/armor-catalogue.md#1-class-hierarchy). |
| Bugs | [BLK-04](index.md#suspected-bugs), [BLK-05](index.md#suspected-bugs) |

## S7. Camo Net Roof (`camoNetRoof`)

| Field | Content |
|---|---|
| Identity | Block [`camoNetRoof`](block-catalogue.md#registered-blocks), class [`GenericBlockVerticalPane`](special-blocks.md#4-camo-net-roof-camonetroof-genericblockverticalpane), metas 0–2 as S6. Names `techguns.tile.camoNetRoof.name` "Camo Net Roof"; `..._<m>.name` "Camo Net Roof (Woodland)" / "(Snow)" / "(Desert)". Item block `GenericItemBlockCamoChangeable`. |
| Obtaining | Recipe B24 (x16, meta 0). Other variants via the Camo Bench. |
| Numbers | Material cloth, hardness 2.0, resistance unset, light 0. Slab height 0.1875 (3 px). |
| Behaviour | Flat net, extent per axis as in [special-blocks.md](special-blocks.md#4-camo-net-roof-camonetroof-genericblockverticalpane). |
| Persistence and sync | Metadata only. |
| UI | Tooltip "Current Camo: <variant>" (keys `techguns.tile.camoNetRoof.camo.<m>`). |
| Visuals | Same three net textures on all faces. |
| Audio | `step.cloth`, `dig.cloth`. |
| Relationships | As S6. |
| Bugs | [BLK-04](index.md#suspected-bugs), [BLK-05](index.md#suspected-bugs) |

## S8. Sandbags (`sandbags`)

| Field | Content |
|---|---|
| Identity | Block [`sandbags`](block-catalogue.md#registered-blocks), class [`BlockSandbags`](special-blocks.md#5-sandbags-blocksandbags), meta 0 only. Name `techguns.tile.sandbags.name` "Sandbags". Item block vanilla `ItemBlock`. |
| Obtaining | Recipe B22 (shapeless: one rubber plus 8 sand gives 8). |
| Numbers | Material cloth, hardness 2.0, resistance 10.0, light 0, no tool needed. Box 0.5 wide pillar when alone. |
| Behaviour | Connects to the same block, fence gates and opaque cubes. Collision matches the box. |
| Persistence and sync | None. |
| UI | none |
| Visuals | `sandbags-top`, `sandbags-side` (32×32). Item bounds 0–1 × 0–1 × 0.25–0.75. |
| Audio | Step, break and place use the `GenericBlock` default stone type (`step.stone`, `dig.stone`), although the material is cloth. |
| Relationships | Recipe in [06-items](../06-items/index.md). Structures in [11-worldgen](../11-worldgen/index.md). |
| Bugs | none |

## S9. Metal Ladder (`ladder01`)

| Field | Content |
|---|---|
| Identity | Block [`ladder01`](block-catalogue.md#registered-blocks), class [`BlockTGLadder`](special-blocks.md#6-metal-ladder-ladder01-blocktgladder), unlocalized `LadderBlock`. Metas `variant*4 + facing`. Names `techguns.tile.LadderBlock.name` "Metal Ladder"; per-meta items "Metal Ladder (Grey)", "(Rusty)", "(Shiny)", "(Black)". Item block `GenericItemBlockCamoChangeable`. |
| Obtaining | Recipe B11 (x16, grey; [`plateTin`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) in a U/H shape). Other colours via the Camo Bench or the creative tab (damages 0, 4, 8, 12). |
| Numbers | Material iron, hardness 10.0, resistance 1000 (set in TGBlocks), light 0, pickaxe level 1. Collision slab 2 px thick. |
| Behaviour | Climbable (`isLadder` true). Pops off if its support disappears. Drops `meta - meta%4`. |
| Persistence and sync | Metadata only. |
| UI | Tooltip "Current Camo: Grey / Rusty / Shiny / Black" (keys `techguns.tile.LadderBlock.<0,4,8,12>`). |
| Visuals | ISBRH [`RenderLadder`](block-rendering-and-models.md#modelladder01-uv-3232) with projected-UV textures `metalLadder`, `metalLadderRusty`, `metalLadderShiny`, `metalLadderCarbon` (32×32, 71% opaque). |
| Audio | Step `step.stone`, break and place `dig.stone`, with pitch 1.5 (metal type). |
| Relationships | Camo Bench. Recipe in [06-items](../06-items/index.md). |
| Bugs | [BLK-05](index.md#suspected-bugs), [BLK-06](index.md#suspected-bugs), [BLK-07](index.md#suspected-bugs) |

## S10. Lamps and lanterns (`lamp01`)

| Field | Content |
|---|---|
| Identity | Block [`lamp01`](block-catalogue.md#registered-blocks), class [`BlockTGLamp`](special-blocks.md#7-lamps-and-lanterns-lamp01-blocktglamp), unlocalized `Lamp`. Metas 0–6 yellow lamp, 7 yellow lantern, 8–14 white lamp, 15 white lantern. Names `techguns.tile.Lamp_<m>.name`: "Lamp (Yellow)", "Lantern (Yellow)", "Lamp (White)", "Lantern (White)". Item block [`GenericItemBlockMetadata`](block-catalogue.md#conventions-and-defaults). |
| Obtaining | B12 (yellow lamp x16), B13 (yellow and white lamp convert), B14 (yellow lantern x16), B15 (lantern colour convert). |
| Numbers | Material iron, hardness 0.25, resistance unset, light 15, light opacity 0, pickaxe level 1. Creative entries: metas 0, 8, 7, 15. |
| Behaviour | Auto-attaches to a neighbouring solid face. Drops when unsupported. Lanterns grow connector arms. |
| Persistence and sync | Metadata only. |
| UI | none |
| Visuals | ISBRH [`RenderLamp`](block-rendering-and-models.md#modelladder01-uv-3232) with `lamp01`, `lamp02`, `lantern01_side/_top`, `lantern02_side/_top`. |
| Audio | Step `step.stone`, break and place `dig.glass` (glass type). |
| Relationships | Recipes in [06-items](../06-items/index.md). |
| Bugs | none |

## S11. Bunker Door block (`door01`)

| Field | Content |
|---|---|
| Identity | Block `door01`, class [`BlockTGDoor`](special-blocks.md#8-metal-door-door01-blocktgdoor) (extends vanilla `BlockDoor`). No block name, so it shows `tile.null.name` ([BLK-03](index.md#suspected-bugs)). The real placer is the Bunker Door item (`techguns.item.bunkerDoor.name` "Bunker Door", [06-items](../06-items/special-items.md)). |
| Obtaining | The item: 6 [`plateIron`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) give 2 doors (see the item recipe in [06-items/recipes.md](../06-items/recipes.md)). The raw block is also in the creative tab. Placed by structures. |
| Numbers | Material iron, hardness 20.0, resistance 2000, light 0. |
| Behaviour | Opens by hand. Opens neighbouring leaves as a double door. Uses vanilla redstone opening. |
| Persistence and sync | Metadata only (vanilla door bits). |
| UI | none |
| Visuals | Icons `door01`, `door01_upper`, `door01_lower` (32×32) plus mirrored copies. |
| Audio | `techguns:blocks.metaldoorOpen`, volume 1.0, pitch 1.0, on every hand toggle (client only). Step and break: stone type. |
| Drops | The door item, except upper-half meta 8 ([BLK-01](index.md#suspected-bugs)). |
| Relationships | Item in [06-items](../06-items/index.md). Structures in [11-worldgen](../11-worldgen/index.md). |
| Bugs | [BLK-01](index.md#suspected-bugs), [BLK-02](index.md#suspected-bugs), [BLK-03](index.md#suspected-bugs), [BLK-32](index.md#suspected-bugs) |

## S12. Reinforced Chest and Weapon Chest (`tgchest`, `tgchest_weapon`)

| Field | Content |
|---|---|
| Identity | Blocks [`tgchest`](block-catalogue.md#registered-blocks) ("Reinforced Chest") and [`tgchest_weapon`](block-catalogue.md#registered-blocks) ("Weapon Chest"), class `BlockTGChest(type)`, BlockContainer with tile entity [`TGChestTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-tgchesttileent). Item block [`ItemBlockTGChest`](block-catalogue.md#conventions-and-defaults). Name keys `techguns.tile.tgchest.name` and `techguns.tile.tgchest_weapon.name`. |
| Obtaining | B33 (Reinforced: 4 iron nuggets in the corners plus a chest in the centre); B34 (Weapon: iron plate, chest, iron plate in a row). |
| Numbers | Material wood, hardness 5.0, resistance 500, no tool needed. 36 slots. Double chests are 72 slots. GUI id 20. |
| Behaviour | See [special-blocks.md](special-blocks.md#9-reinforced-chest-and-weapon-chest-tgchest-tgchest_weapon-blocktgchest). Weapon chest emits redstone power ([BLK-13](index.md#suspected-bugs)). |
| Persistence and sync | Tile NBT: `Items` list (each entry a `Slot` byte plus the item stack), `CustomName` string (when set), `security` byte and `owner` string (written, never meaningfully used). Item stack NBT `TileEntityData` carries the picked-up contents. |
| UI | Titles `techguns.container.tgchest` "Reinforced Chest", `.double` "Large Reinforced Chest", `techguns.container.tgchest_weapon` "Weapon Chest", `.double` "Large Weapon Chest". Item tooltip "Has contents" (`techguns.block.chestHasContents`) when `TileEntityData` is present. Chat: "[DENIED] You have no permission to use" (effectively unreachable). |
| Visuals | TESR [`RenderTGChest`](../09-machines-utility/misc-tile-entities.md#1-reinforced-chest-and-weapon-chest), textures `reinforced_chest(_double)` and `weapon_chest(_double)`; item model [`ModelChestInv`](block-rendering-and-models.md#modelchestinv-uv-6464-textures-reinforced_chestpng--weapon_chestpng). |
| Audio | Vanilla `random.chestopen` and `random.chestclosed`, volume 0.5, pitch random in 0.9–1.0 (0.9 plus 0.1 times a random float), from the tile entity. Step and break: wood type. |
| Relationships | Tile entity and GUI in [09-machines-utility](../09-machines-utility/misc-tile-entities.md). Recipes in [06-items](../06-items/index.md). |
| Bugs | [BLK-13](index.md#suspected-bugs), [BLK-14](index.md#suspected-bugs), [BLK-15](index.md#suspected-bugs), [BLK-16](index.md#suspected-bugs) |

## S13. Titanium Iron Ore and Uranium Ore (`oreTitanIron`)

| Field | Content |
|---|---|
| Identity | Block [`oreTitanIron`](block-catalogue.md#registered-blocks), class [`GenericBlockMetadata`](special-blocks.md#10-titanium-iron-and-uranium-ore-oretitaniron-genericblockmetadata). Meta 0 "Titanium Iron Ore", meta 1 "Uranium Ore" (`techguns.tile.oreTitanIron_<m>.name`). Item block `GenericItemBlockMetadata`. Oredict: meta 0 `oreTitaniumIron` and `oreIllmenite` (sic), meta 1 `oreUranium`. |
| Obtaining | Ore worldgen ([11-worldgen/ore-generation.md](../11-worldgen/ore-generation.md)). Drops itself. |
| Numbers | Material rock, hardness 4.0, resistance 7.0. Meta 0: light 3, pickaxe level 3. Meta 1: light 5, pickaxe level 2. |
| Behaviour | Plain ore. Drops itself with its meta. |
| Persistence and sync | Metadata only. |
| UI | none |
| Visuals | Animated `oreTitanIron` and `oreUranium` (16×64, 4 frames x 8 ticks). |
| Audio | Stone type. |
| Relationships | Reaction Chamber input ([08-machines-production](../08-machines-production/index.md)). Cluster types `oreCluster2:1` and `oreCluster:15`. |
| Bugs | [BLK-19](index.md#suspected-bugs), [BLK-21](index.md#suspected-bugs), [BLK-25](index.md#suspected-bugs) |

## S14. Plain ores registered in the main class (`oreCopper`, `oreTin`, `oreLead`)

| Field | Content |
|---|---|
| Identity | `GenericBlock`s built in `Techguns.java`, only when the config flags allow. Names "Copper Ore", "Tin Ore", "Lead Ore" (`techguns.tile.oreCopper.name` and so on). |
| Obtaining | World generation, config-gated ([`doOreGenCopperTin`](../01-core/config.md#category-general), [`doOreGenLead`](../01-core/config.md#category-general) in [01-core/config.md](../01-core/config.md)). |
| Numbers | `GenericBlock` defaults: hardness 2.0, stone sound, pickaxe level 1. |
| Behaviour | Plain. Smelting is registered in [01-core](../01-core/lifecycle-and-registration.md). |
| Persistence and sync | None. |
| UI | none |
| Visuals | `oreCopperBlock`, `oreTinBlock`, `oreLeadBlock` (16×16). |
| Audio | Stone type. |
| Relationships | Source of cluster types 7, 8 and 9 of [`oreCluster`](block-catalogue.md#registered-blocks) when present. |
| Bugs | none |

## S15. Ore clusters (`oreCluster`, `oreCluster2`, `oreClusterCustom`, `oreClusterCustom2`)

| Field | Content |
|---|---|
| Identity | [`OreCluster`](block-catalogue.md#registered-blocks) (two blocks) and [`OreClusterCustom`](block-catalogue.md#registered-blocks) (two optional blocks). Names `techguns.tile.oreCluster_<N>.name` "Ore Cluster (Iron)" and so on (full list in [lang-and-tooltips.md](lang-and-tooltips.md)); custom blocks "Ore Cluster". Item blocks `GenericItemBlockMetadata` and [`ItemBlockOreClusterCustom`](block-catalogue.md#conventions-and-defaults). |
| Obtaining | World generation ([11-worldgen](../11-worldgen/ore-generation.md)). Creative tab. Cannot be harvested for a drop. |
| Numbers | Hardness -1, resistance 6,000,000. Crowbar break progress 0.005 per tick (200 ticks) for non-bedrock types. Type tables are in [special-blocks.md](special-blocks.md#11-ore-clusters-orecluster-orecluster2-oreclustercustom-oreclustercustom2). Config: `addCustomOreCluster`, `addCustomOreCluster2`, per-type multipliers (ore-drill category). |
| Behaviour | Drops nothing. The Ore Drill extracts from it. |
| Persistence and sync | Metadata only. Custom clusters hold their ore array in memory, set by MineTweaker. |
| UI | Custom-cluster tooltip: the fluid name or the ore's name, e.g. "Iron Ore". Creative tab lists only types with level above 0. A slot never set crashes the tooltip ([BLK-27](index.md#suspected-bugs)). |
| Visuals | [`RenderOrecluster`](block-rendering-and-models.md#1-isbrh-renderers-and-render-ids): base cube (stone, netherrack, end stone or bedrock) plus overlay `oreClusterNew1`–`4` tinted by type colour. |
| Audio | Stone type. |
| Relationships | Ore Drill ([08](../08-machines-production/index.md), [09](../09-machines-utility/ore-drill.md)). Ore drill placement rules in S21. |
| Bugs | [BLK-23](index.md#suspected-bugs), [BLK-25](index.md#suspected-bugs), [BLK-27](index.md#suspected-bugs), [BLK-30](index.md#suspected-bugs) |

## S16. Strange Glowing Eggs (`bugnesteggs`) and Slime Trail (`bugnestslimy`)

| Field | Content |
|---|---|
| Identity | [`bugnesteggs`](block-catalogue.md#registered-blocks) (`GenericBlock`, "Strange Glowing Eggs", `techguns.tile.bugnesteggs.name`) and [`bugnestslimy`](block-catalogue.md#registered-blocks) ([`BlockBugSlime`](block-catalogue.md#registered-blocks), extends `BlockLadder`, "Slime Trail", `techguns.tile.slimetrail.name`). |
| Obtaining | Creative tab and alien-nest worldgen only. No recipes. |
| Numbers | Eggs: sand material, hardness 3.0, resistance 2.0, light 75 requested (bug). Slime: circuits material, hardness 3.0, resistance 2.0, ladder rules. |
| Behaviour | Eggs inert. Slime Trail is climbable and attaches to walls like a ladder. |
| Persistence and sync | Metadata only (slime stores facing 2–5). |
| UI | none |
| Visuals | `bugnesteggs` (16×64, animated frames 0,1,2,3,2,1,0 at 2 ticks), `bugnestslimy` (16×16). |
| Audio | Eggs: stone type. Slime: cloth type. |
| Relationships | [11-worldgen](../11-worldgen/index.md) alien nests. |
| Bugs | [BLK-10](index.md#suspected-bugs), [BLK-24](index.md#suspected-bugs) |

## S17. Monster spawner holes (`TGMonsterSpawner`)

| Field | Content |
|---|---|
| Identity | Block [`TGMonsterSpawner`](block-catalogue.md#registered-blocks), class [`BlockTGMonsterSpawner`](special-blocks.md#monster-spawner-holes-blocktgmonsterspawner) (BlockContainer), tile entity [`TGSpawnerTileEnt`](../10-entities-npcs/spawning.md#3-monster-spawner-blocks-tgspawnertileent). Meta 0 "Bug Hole", meta 1 "Zombie Hole" (`techguns.tile.TGMonsterSpawner_<m>.name`). Item block `GenericItemBlockMetadata`. |
| Obtaining | Creative tab and worldgen. Breaking drops `bugnest:0` (meta 0) or `concrete:1` (meta 1). |
| Numbers | Rock material, hardness 8.0, pickaxe level 1. Spawn values per meta are in [special-blocks.md](special-blocks.md#monster-spawner-holes-blocktgmonsterspawner). |
| Behaviour | A vanilla mob-spawner block that spawns `Techguns.AlienBug` (meta 0) or `Techguns.ZombieSoldier` (meta 1). No XP. |
| Persistence and sync | Vanilla spawner NBT: `EntityId`, `Delay`, `MinSpawnDelay`, `MaxSpawnDelay`, `SpawnCount`, `MaxNearbyEntities`, `RequiredPlayerRange`, `SpawnRange` (all shorts except the id string). |
| UI | none |
| Visuals | Side `bugnestblock` or `concrete01`, top `bugnestblock_hole` or `concrete_hole`. |
| Audio | Stone type. The mob-spawner flame particles and spawn sounds are vanilla. |
| Relationships | Mobs in [10-entities-npcs](../10-entities-npcs/spawning.md). Alien nest and city structures. |
| Bugs | [BLK-12](index.md#suspected-bugs), [BLK-21](index.md#suspected-bugs) |

## S18. Bio Blob (`BioBlobSmall`)

| Field | Content |
|---|---|
| Identity | Block [`BioBlobSmall`](block-catalogue.md#registered-blocks), class [`Bioblob`](special-blocks.md#13-bio-blob-bioblobsmall-bioblob) (BlockContainer), tile entity [`BioblobTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-bioblobtileent). Unlocalized `Bioblob`, which does not resolve ([BLK-09](index.md#suspected-bugs)); the lang has `techguns.tile.Bioblob.name` = "Bio Blob". |
| Obtaining | Created only by Bio Gun projectiles ([04-projectiles-fx](../04-projectiles-fx/index.md)). Not harvestable. Not in a creative tab. |
| Numbers | Hardness 0, light 0. Size 1–3. Width (10 + 6s)/32, thickness (2 + 4s)/32 block units. Poison III for 100 ticks on contact. Shrinks every 60 ticks. |
| Behaviour | See [special-blocks.md](special-blocks.md#13-bio-blob-bioblobsmall-bioblob). |
| Persistence and sync | Tile NBT: `BlobTicks` (int), `side` (byte), `size` (byte). The same tag is sent to the client as the tile-entity description packet. Block meta 0–5 is the attached face. |
| UI | none |
| Visuals | All faces `bio` (16×16). |
| Audio | Burst plays `techguns:effects.biodeath` ([04-projectiles-fx](../04-projectiles-fx/index.md)). Step, break: stone type. |
| Relationships | Bio Gun in [02-guns](../02-guns/index.md). |
| Bugs | [BLK-08](index.md#suspected-bugs), [BLK-09](index.md#suspected-bugs), [BLK-28](index.md#suspected-bugs) |

## S19. Creeper Acid (`acid`)

| Field | Content |
|---|---|
| Identity | Block [`acid`](block-catalogue.md#registered-blocks), class [`BlockFluidAcid`](special-blocks.md#14-creeper-acid-acid-blockfluidacid) (Forge `BlockFluidClassic`), fluid `creeper_acid`. Name `techguns.tile.creeperAcid.name` "Creeper Acid"; fluid name `fluid.creeperAcid`. Item block vanilla `ItemBlock`. |
| Obtaining | Placed from its bucket item ([06-items/fluids.md](../06-items/fluids.md)) or the creative tab (a bug). Not breakable. Any mob source is outside this area. |
| Numbers | Density 100, luminosity 0, water material. Contact damage 2.0 Techguns BIO. |
| Behaviour | See [special-blocks.md](special-blocks.md#14-creeper-acid-acid-blockfluidacid). |
| Persistence and sync | Fluid level metadata only. |
| UI | none |
| Visuals | `acid_still` (16×512, 32 frames x 2 ticks), `acid_flow` (32×1024, 32 frames x 1 tick). |
| Audio | none from Techguns (vanilla fluid ambient sounds only). |
| Relationships | Bucket item in [06-items](../06-items/index.md). |
| Bugs | [BLK-26](index.md#suspected-bugs) |

## S20. Air Marker (`airMarker`)

| Field | Content |
|---|---|
| Identity | Block [`airMarker`](block-catalogue.md#registered-blocks), class [`BlockAirMarker`](special-blocks.md#15-air-marker-airmarker-blockairmarker), unlocalized `AirMarker`, "Air Marker" (`techguns.tile.AirMarker.name`). |
| Obtaining | Creative tab only. Editor block. |
| Numbers | Cloth material, hardness 1.0, uses neighbour brightness. |
| Behaviour | Means "force air" in structure templates. |
| Persistence and sync | None. |
| UI | none |
| Visuals | Cube with `airMarker` (32×32, 38% opaque). |
| Audio | Cloth type. |
| Relationships | [`StructureTestTool`](../06-items/special-items.md#structure-test-tool-structuretesttool) ([06-items](../06-items/special-items.md)), [`MBlock`](../11-worldgen/structure-system.md#mblock) ([11-worldgen](../11-worldgen/structure-system.md)). |
| Bugs | none |

## S21. Machine blocks in `TGBlocks` (owned elsewhere)

| Block | Item block | Where documented |
|---|---|---|
| [`basicMachine`](block-catalogue.md#registered-blocks) | [`ItemBlockMetaDataMachine`](special-blocks.md#16-itemblockmetadatamachine) | [08-machines-production](../08-machines-production/blocks-and-items.md) and [09-machines-utility](../09-machines-utility/index.md) |
| [`oreDrill`](block-catalogue.md#registered-blocks) | [`ItemBlockOreDrill`](special-blocks.md#17-itemblockoredrill-placement-rules) (placement rules in [special-blocks.md](special-blocks.md#17-itemblockoredrill-placement-rules)) | [08](../08-machines-production/index.md) and [09](../09-machines-utility/index.md) (`ore-drill.md`) |
| [`machineMultiBlock`](block-catalogue.md#registered-blocks) | `ItemBlockMetaDataMachine` | [08](../08-machines-production/index.md) |

Their texture lists are in [block-catalogue.md](block-catalogue.md#metadata-variants-of-the-chisel-style-blocks) and [textures.md](textures.md) §10. Their tooltip "Has TileEntity Data" (`techguns.block.hasTileEntityData`) comes from `ItemBlockMetaDataMachine`.
