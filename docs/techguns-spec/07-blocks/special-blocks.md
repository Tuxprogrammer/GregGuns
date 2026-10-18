# 07-blocks: block behaviours

Behaviour of each block family, beyond the property table in [block-catalogue.md](block-catalogue.md). Coordinates: +X east, +Z south, +Y up. Vanilla side indices: 0 down, 1 up, 2 north (−Z), 3 south (+Z), 4 west (−X), 5 east (+X). Model and renderer details are in [block-rendering-and-models.md](block-rendering-and-models.md).

---

## 1. Decorative Chisel blocks (container, concrete, metalpanel, metalpanel2, bugnest)

[`GenericBlockCTM`](block-catalogue.md#registered-blocks) implements Chisel's `ICarvable`. It does nothing itself: icons, sub-blocks (one creative entry per variation), the world icon and the render type all come from Chisel's `CarvableHelper` and its connected-texture renderer. Metadata = variation, and it drops itself. With the Chisel mod's chisel the variations can be swapped within each carving group. See [connected-textures.md](connected-textures.md) for the texture behaviour.

[`BlockBugSand`](block-catalogue.md#registered-blocks) (bugnest) adds an `updateTick` that, on the server, spawns one [`AlienBug`](../10-entities-npcs/per-mob.md#15-alien-bug-alienbug) ([10-entities-npcs](../10-entities-npcs/index.md)) centred on top of the block with random yaw and pitch if the block above is air. The constructor calls `setTickRandomly(false)` and nothing schedules ticks, so **this never runs** ([BLK-11](index.md#suspected-bugs)). In practice the block is inert hardened sand used by alien bug nest world generation ([11-worldgen](../11-worldgen/index.md)).

---

## 2. Radar Screen and Control Panel (`GenericBlockDirectional`)

**Textures.** Built from `techguns:<name>` plus suffixes, each checked for existence through [`BlockUtils.exists`](../01-core/utilities.md#blockutils) ([01-core](../01-core/index.md)):

| Slot | File | Fallback |
|---|---|---|
| top (1) | `<name>_top` | required |
| bottom (0) | `<name>_bottom` | top |
| sides (4, 5) | `<name>_side` | required |
| front (2) | `<name>_front` | side |
| back (3) | `<name>_back` | side |

When the overlay flag is set (it is for both blocks), the optional overlay icons `<name>_overlay_bottom`, `_overlay_top`, `_overlay_front`, `_overlay_back` and `_overlay_side` are loaded too. Only `_overlay_front` exists, for both [`screen`](block-catalogue.md#registered-blocks) and [`controlpanel`](block-catalogue.md#registered-blocks).

**Facing on placement.** Uses the placer's yaw quadrant `q = floor(yaw·4/360 + 0.5) & 3`, then sets the metadata:

| q | Player faces | Meta |
|---|---|---|
| 0 | south | 2 |
| 1 | west | 3 |
| 2 | north | 5 |
| 3 | east | 4 |

**Face selection.** For metadata `m` ≥ 2 and a horizontal side `s`, the icon index is `2 + ((s + m) mod 4)`: 2 = front, 3 = back, 4 or 5 = side. The front therefore always faces the player who placed the block. Sides 0 and 1, and metadata below 2 (the item form), use the slot table above directly.

**Rendering.** The block reports render pass 1 but answers `canRenderInPass` true for both passes, storing the current pass in [`ClientProxy.renderPass`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy).

- **Pass 0:** a normal opaque cube.
- **Pass 1:** the animated overlay icon is drawn on the front face only, just above the surface, with "FX lighting" calls meant to make it full-bright (see [BLK-20](index.md#suspected-bugs)).

Result: a brown/grey metal box whose front shows a glowing green animated display. On the screen it is a radar sweep; on the control panel it is an oscilloscope trace with blinking LEDs.

In the inventory the renderer draws nothing in 3D, so the item shows the flat `_top` icon.

There is no interaction, redstone or tile entity. The blocks are used by structures ([11-worldgen](../11-worldgen/index.md)).

---

## 3. Camo Net (`camoNetPane`, `GenericBlockPane`)

A vanilla-pane-style vertical net, with three camo variants by metadata.

**Connections.** The net connects only to:

- another block of the same type, or
- any instance of vanilla `BlockPane` (glass panes, stained panes, iron bars).

Unlike vanilla panes, it does **not** connect to solid blocks.

**Collision and selection box.** A 2-px (0.4375–0.5625) thick, full-height slab:

- With no connections it is a 2×2-px post.
- On each axis, it extends to the 0 or 1 edge toward each connected neighbour.
- When both neighbours on an axis connect, it spans that axis fully.

**World rendering** ([`RenderGenericPane`](block-rendering-and-models.md#1-isbrh-renderers-and-render-ids)):

- **Net:** if it has any connection, vanilla pane rendering draws the net texture (variant icon) on the pane quads. The pane's top and edge strip icon is `techguns:invisible`, so the 2-px top and edges are transparent. An isolated net draws no net at all.
- **Pole:** a 2×2-px full-height centre post, textured with the variant's pole icon, is drawn when the net is:
  - isolated (no connections),
  - an end piece (exactly one connection), or
  - an L-corner (two connections on different axes).

  It is not drawn for a straight run, a T junction or a cross.

| Meta | Net texture | Pole texture (vanilla) | Lang |
|---|---|---|---|
| 0 | `techguns:camoNet` | `planks_oak` | Woodland |
| 1 | `techguns:camoNet_snow` | `log_spruce` | Snow |
| 2 | `techguns:camoNet_desert` | `planks_jungle` | Desert |

**Notes.**

- It drops itself but always as meta 0 ([BLK-04](index.md#suspected-bugs)).
- The item renders as a flat icon (the net texture).
- Flamethrower and Cyberdemon blaster projectiles treat camo nets specially ([04-projectiles-fx](../04-projectiles-fx/index.md)).
- The [`camoNetPlacer`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems) item places whole planes of nets ([06-items](../06-items/index.md)).

---

## 4. Camo Net Roof (`camoNetRoof`, `GenericBlockVerticalPane`)

A flat, horizontal net slab 3 px (0.1875) high at the bottom of the block. It uses the same three variants and textures as the camo net, on all faces.

**Connections.** A neighbour connects if it is:

- the same block, or
- a fence gate, or
- a block whose material is opaque, that renders as a normal cube, and whose material is not gourd.

**Horizontal extent, per axis.**

| Neighbours that connect on this axis | Extent |
|---|---|
| both | 0 → 1 |
| only the + side | 0.4375 → 1 |
| only the − side | 0 → 0.5625 |
| neither | 0.25 → 0.75 |

This uses "width" 0.5625 and "width2" 0.75. A `setDimensions` helper exists but is never called.

**Rendering.** A standard cube using those bounds, with every side always rendered. The item uses bounds 1 × 0.1875 × 1. Drops meta 0 (BLK-04).

---

## 5. Sandbags (`BlockSandbags`)

A connecting wall of sandbags. The box is computed with the same per-axis rule as the camo net roof, but with both widths set to 0.75 and full height 1.0:

| Neighbours that connect on this axis | Extent |
|---|---|
| both | 0 → 1 |
| only + | 0.25 → 1 |
| only − | 0 → 0.75 |
| neither | 0.25 → 0.75 |

A lone sandbag is therefore a 0.5 × 1 × 0.5 pillar, and a straight run is a 0.5-thick wall.

- **Connections:** same block, fence gate, or an opaque, normally rendered, non-gourd block.
- **Collision:** the box is recomputed before collision boxes are added, so collision matches the visual.
- **Textures:** `techguns:sandbags-top` on the top and bottom, `techguns:sandbags-side` on all horizontal faces. Every face is always rendered, through the standard cube renderer.
- **Item:** rendered with bounds 0–1 × 0–1 × 0.25–0.75 (a wall slice).

---

## 6. Metal Ladder (`ladder01`, `BlockTGLadder`)

A wall ladder in four colour variants, with the orientation packed into metadata as `meta = variant·4 + facing`.

| Facing (meta % 4) | Attached to the solid face of | Rails and collision slab at |
|---|---|---|
| 0 | block at z+1 (its north face) | z 0.875–1 |
| 1 | block at z−1 (its south face) | z 0–0.125 |
| 2 | block at x+1 (its west face) | x 0.875–1 |
| 3 | block at x−1 (its east face) | x 0–0.125 |

| Variant | Item damage | Texture | Lang (tooltip "Current Camo") |
|---|---|---|---|
| 0 | 0 | `techguns:metalLadder` | Grey |
| 1 | 4 | `techguns:metalLadderRusty` | Rusty |
| 2 | 8 | `techguns:metalLadderShiny` | Shiny |
| 3 | 12 | `techguns:metalLadderCarbon` | Black |

**Behaviour.**

- `isLadder` is always true, so it is climbable.
- **Collision and selection:** a 2-px (0.125) slab against the supporting wall, full height.
- **Placement** is allowed if any horizontal neighbour presents a solid face toward it.
- **Facing on placement** checks the four supports in the order z+1, z−1, x+1, x−1. Each check runs if nothing is chosen yet *or* the clicked side matches that direction, and sets the facing if that support is solid, otherwise "none". A later failing check can wipe out an earlier success, so the result can be "none" (−1). The stored metadata is then `itemDamage − 1` ([BLK-07](index.md#suspected-bugs)).
- **Neighbour change:** if the support for the current facing is gone, the ladder pops off ([BLK-06](index.md#suspected-bugs): it drops variant 0).
- **Drops:** 1 item, damage `meta − meta%4`.
- **Rotation for structures:** facing cycles 0 → 2 → 1 → 3 → 0 per clockwise step, keeping the variant.
- **Creative tab:** lists damages 0, 4, 8 and 12.

**Rendering** (ISBRH [`RenderLadder`](block-rendering-and-models.md#modelladder01-uv-3232)). Two square rails, 2×2 px and full height, sit in the two corners against the wall. Four rungs, each 2 px tall and 2 px deep, span between the rails flush with the wall, at y = 1–3, 5–7, 9–11 and 13–15 px. Textures are projected by block-space position (see the renderer page), so the 32×32 ladder texture is laid out as it would look on the wall. In inventory, facing 0 is drawn in 3D.

---

## 7. Lamps and lanterns (`lamp01`, `BlockTGLamp`)

One block id holds two colours, each as a wall-mounted lamp or a free-standing lantern.

| Meta | Kind | Icon (all faces) |
|---|---|---|
| 0 | yellow lamp, item form ("unattached") | [`lamp01`](block-catalogue.md#registered-blocks) |
| 1 | yellow lamp on a ceiling (attached to the block above) | `lamp01` |
| 2 | yellow lamp on a floor | `lamp01` |
| 3 | yellow lamp on the block at z+1 | `lamp01` |
| 4 | yellow lamp on the block at z−1 | `lamp01` |
| 5 | yellow lamp on the block at x+1 | `lamp01` |
| 6 | yellow lamp on the block at x−1 | `lamp01` |
| 7 | yellow lantern | `lantern01_top` (Y faces), `lantern01_side` (others) |
| 8–14 | white lamp, same orientation scheme as 0–6 | `lamp02` |
| 15 | white lantern | `lantern02_top` / `lantern02_side` |

**Common properties.** Light 15, light opacity 0, not opaque, not a normal cube. Creative entries are 0, 8, 7 and 15.

**Placement.**

- A lantern (7 or 15) keeps its metadata.
- A lamp item (0 or 8) checks the clicked face. If the block behind that face presents a solid face, the matching orientation is chosen:

  | Clicked side | Support | Orientation |
  |---|---|---|
  | 0 | above | 1 |
  | 1 | below | 2 |
  | 2 | block at z+1 | 3 |
  | 3 | block at z−1 | 4 |
  | 4 | block at x+1 | 5 |
  | 5 | block at x−1 | 6 |

  A white lamp adds 8.
- If the metadata is still 0 or 8 when the block is added (or on a scheduled tick), it auto-attaches to the first solid neighbour in the order above, below, z+1, z−1, x+1, x−1.
- `canPlaceBlockAt` needs at least one solid neighbouring face in any of the six directions.

**Support checks.**

- On every neighbour change, the block is dropped (with its normalised item metadata) if no solid neighbour face exists at all.
- A lamp is also dropped when its own specific support is gone.
- A lantern only needs any one solid neighbour.

**Bounds.** Computed at 32-px resolution:

- **Lamp:** a 16/32 × 16/32 square (0.25–0.75), 6/32 deep from the supporting face.
- **Lantern:** 0.25–0.75 horizontally, 0.125–0.875 vertically.

**Rendered geometry.**

- **Lamp:** a socket 16/32 wide and 2/32 thick against the support, plus a lens box 14/32 wide and 4/32 thick in front of it. Both use the same icon, projected by position. The texture has a bright centre square and four bright edge bars, so the lens front and sides glow and the socket stays dark.
- **Lantern:** a central post (0.375–0.625, y 0.125–0.875), two ring plates (0.28125–0.71875; y 0.1875–0.25 and 0.75–0.8125), and a lens body (0.3125–0.6875, y 0.25–0.75).
- **Lantern connector arms** are added toward each neighbour that is an opaque, normally rendered, non-gourd, non-lamp block:
  - above or below: a 0.25 × 0.25 stub filling the gap to the block edge;
  - horizontal: two 0.25-wide bars (at the two ring heights) running from the ring to the block edge.

**Rotation for structures.**

- Metas 1, 2, 7–10 and 15 are unchanged. Metas 0 and 8 are also unchanged because they hit the early return.
- Wall metas rotate 3 → 5 → 4 → 6 → 3 per clockwise step. The white set (11–14) follows the same cycle offset by 8.

---

## 8. Metal Door (`door01`, `BlockTGDoor`)

A reskinned vanilla iron door with these changes:

- **Opens by hand** (right-click), unlike a vanilla iron door. Redstone opening is inherited from vanilla.
- **Double doors:** after toggling, it looks at the two neighbours across the door's width and toggles any `door01` there too. That covers ±X when the lower-half metadata is 1, 3, 5 or 7, and ±Z when it is 0, 2, 4 or 6. This makes double doors open together. The neighbour toggle does not cascade further.
- **Sound:** `techguns:blocks.metaldoorOpen` (volume 1, pitch 1) through `World.playSound`, which is client-only. Only the clicking client hears it, once per door leaf toggled ([BLK-02](index.md#suspected-bugs)). Redstone toggling uses vanilla's door sound.
- **Icons:**
  - `techguns:door01` for the top and bottom faces;
  - `techguns:door01_upper` and `techguns:door01_lower` for the vertical faces of the two halves;
  - horizontally mirrored copies (vanilla `IconFlipped`) for the flipped side.

  Which side is mirrored depends on the facing, the open state and the hinge flag, so the texture always reads the right way round from both sides. The item-form icon is `door01_upper`.
- **Drops and pick-block:** both give [`TGItems.door01`](../06-items/item-catalogue.md) (Bunker Door item, [06-items](../06-items/index.md)), except upper-half metadata exactly 8 ([BLK-01](index.md#suspected-bugs)).
- It sits in the Techguns creative tab as a raw block item ([BLK-03](index.md#suspected-bugs)). It is placed by structures ([11-worldgen](../11-worldgen/index.md)).

---

## 9. Reinforced Chest and Weapon Chest (`tgchest`, `tgchest_weapon`, `BlockTGChest`)

This is a near-copy of the vanilla chest. Type 0 is the reinforced chest and type 1 the weapon chest. The tile entity is [`TGChestTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-tgchesttileent) and the TESR is [`RenderTGChest`](../09-machines-utility/misc-tile-entities.md#1-reinforced-chest-and-weapon-chest). Both sit outside this area's file scope, but their key facts are summarised here so the block is implementable.

- **Shape:** 1/16 inset on every side, 14/16 tall. The box extends to the shared edge when a chest of the same block is adjacent.
- **Placement:**
  - It faces the placer: q 0 → meta 2, 1 → 5, 2 → 3, 3 → 4.
  - If one neighbour is the same chest, both halves are aligned the way vanilla does it.
  - At most two in a row, with no third chest next to a double.
  - On the server, if the item has an NBT compound `TileEntityData`, it is loaded into the new tile entity. That restores the contents.
- **Inventory:** 36 slots per chest (vanilla has 27), all sides insert and extract. A double chest opens as a vanilla `InventoryLargeChest`.

  | | Single | Double |
  |---|---|---|
  | Reinforced | "Reinforced Chest" (`techguns.container.tgchest`) | "Large Reinforced Chest" (`techguns.container.tgchest.double`) |
  | Weapon | "Weapon Chest" (`techguns.container.tgchest_weapon`) | "Large Weapon Chest" (`techguns.container.tgchest_weapon.double`) |
- **Right-click:**
  - **Sneaking while holding an item whose tool classes include `wrench`:** on the server the contents are written to a new item's `TileEntityData` tag (`CustomName` if set, `Items` list with `Slot` bytes). The chest is emptied and removed, and the item is spawned at the block's corner coordinates ([BLK-15](index.md#suspected-bugs)). The call returns false.
  - **Otherwise:** opens Techguns GUI id 20 ([`GuiTechgunsChest`](../09-machines-utility/misc-tile-entities.md#gui-guitechgunschest)).
  - **Usability check:** the tile entity's base check is distance only (≤ 8 blocks), because the owner/security override is commented out. If it fails, the client shows `techguns.container.security.denied` ("[DENIED] You have no permission to use"). This is effectively unreachable ([BLK-14](index.md#suspected-bugs)).
- **Breaking:** spills the contents like vanilla, in random stacks of 10–30, keeping NBT.
- **Redstone:**
  - Comparator output follows the (double) inventory fill. It is 0 when the chest is blocked by a solid block above or a sitting ocelot.
  - **The weapon chest (type 1) is a power source**, like a vanilla trapped chest: weak power equals the number of players using it, clamped to 0–15, and strong power on the top side ([BLK-13](index.md#suspected-bugs)).
- **Visuals:** render type −1 (TESR). The textures are `reinforced_chest(_double).png` and `weapon_chest(_double).png`, laid out like vanilla chest skins at 2× resolution. The lid animation and the `random.chestopen` / `random.chestclosed` sounds (volume 0.5, pitch 0.9–1.0) are vanilla-style and come from the tile entity. The particle icon is vanilla `planks_oak`.
- **Item:** rendered with [`ModelChestInv`](block-rendering-and-models.md#modelchestinv-uv-6464-textures-reinforced_chestpng--weapon_chestpng) (see the rendering page). Tooltip "Has contents" when it carries `TileEntityData`.

---

## 10. Titanium Iron and Uranium ore (`oreTitanIron`, `GenericBlockMetadata`)

| Meta | Name | Texture (animated, 4 frames × 8 ticks) | Light | Pickaxe level |
|---|---|---|---|---|
| 0 | Titanium Iron Ore | [`oreTitanIron`](block-catalogue.md#registered-blocks) | 3 | 3 |
| 1 | Uranium Ore | `oreUranium` | 5 | 2 |

`GenericBlockMetadata` is a generic per-meta texture and light holder: `setBlockTypeForMeta(meta, texture)`, `setLightLevelForMeta(meta, level)`, per-meta `getLightValue` from the world metadata, `damageDropped = meta`, and creative entries for each meta in use.

- Spawned by [`OreGenerator`](../11-worldgen/ore-generation.md#1-ore-veins-oregenerator) ([11-worldgen](../11-worldgen/index.md)).
- The Reaction Chamber processes titanium iron ore ([08-machines-production](../08-machines-production/index.md)).
- Titanium iron is also ore-cluster type `oreCluster2:1`, and uranium is `oreCluster:15`.

---

## 11. Ore clusters (`oreCluster`, `oreCluster2`, `oreClusterCustom`, `oreClusterCustom2`)

An ore cluster is an unbreakable, ore-rich block that an Ore Drill ([08-machines-production](../08-machines-production/index.md)) extracts from. World spawning is in [11-worldgen](../11-worldgen/index.md). Each metadata is an "ore type" with these parallel arrays (16 entries):

- `ores` (ItemStack): the product. For fluid types it is the cluster block itself.
- `mininglevels`: the drill tier needed. 0 means a placeholder: hidden from the creative tab and treated as unused.
- `colors` (RGB 0–1): tint for the overlay.
- `textureType`: overlay index 0–3, mapped to `oreClusterNew1`–`oreClusterNew4`.
- `rarity` and `maxy`: world-generation data ([11](../11-worldgen/index.md)).
- `fluids`: a FluidStack for liquid clusters.
- `isBedrock`: draws the base as bedrock.
- `multipliersAmount` and `multipliersPower`: from config ([01-core](../01-core/index.md) config, "Ore Drills" category), used by the drill.

**Unbreakable.** Hardness −1 and resistance 6,000,000. Exception: if the player holds `TGItems.crowbar` and the type is not a bedrock type, the per-tick break progress is 0.005, so a hand break takes 200 ticks. It drops nothing.

**Rendering** ([`RenderOrecluster`](block-rendering-and-models.md#1-isbrh-renderers-and-render-ids)). First a full cube with the base icon, then all six faces again with the overlay icon tinted by the type's colour.

- **Base icon:** bedrock if the type is a bedrock type. Otherwise it depends on the client player's current dimension: netherrack in −1, end stone in 1, stone elsewhere ([BLK-23](index.md#suspected-bugs)).
- **Inventory:** a stone cube plus the tinted overlay, with standard item lighting.

### `oreCluster` (16 types)

| Meta | Ore | Level | Tint (r,g,b) | Overlay | Rarity | maxY | Amount ×, Power × (config defaults) |
|---|---|---|---|---|---|---|---|
| 0 | iron ore | 1 | .8,.6,.5 | 0 | 1 | 64 | 2.0, 0.15 |
| 1 | gold ore | 2 | 1,1,.6 | 0 | 3 | 48 | 1.0, 0.4 |
| 2 | redstone ore | 2 | .8,0,0 | 0 | 2 | 32 | 1.0, 0.3 |
| 3 | coal ore | 1 | .3,.3,.3 | 1 | 1 | 64 | 1.2, 0.25 |
| 4 | lapis ore | 2 | .5,.5,1 | 1 | 2 | 48 | 1.0, 0.3 |
| 5 | diamond ore | 3 | .6,.9,1 | 2 | 4 | 32 | 0.25, 1.0 |
| 6 | emerald ore | 3 | .2,1,.45 | 2 | 4 | 32 | 0.25, 1.0 |
| 7 | first `oreCopper` | 1 | .65,.5,.05 | 0 | 1 | 48 | 2.0, 0.1 |
| 8 | first `oreTin` | 1 | .65,.95,1 | 0 | 1 | 48 | 2.0, 0.1 |
| 9 | first `oreLead` | 2 | .55,.65,1 | 0 | 2 | 48 | 1.0, 0.2 |
| 10 | first `oreOsmium` | 2 | .75,.75,1 | 1 | 2 | 48 | 1.0, 0.3 |
| 11 | first `oreSilver` | 2 | .9,.9,1 | 0 | 3 | 32 | 1.0, 0.35 |
| 12 | first `oreNickel` | 2 | 1,.8,.6 | 0 | 2 | 32 | 1.0, 0.3 |
| 13 | first `oreAluminium` | 2 | .95,.95,.95 | 1 | 2 | 32 | 1.0, 0.2 |
| 14 | first `orePlatinum` | 2 | .9,.9,1 | 1 | 4 | 32 | 0.35, 1.0 |
| 15 | uranium (always `oreTitanIron:1`) | 3 | .5,1,.5 | 0 | 4 | 32 | 0.25, 1.0 |

Metas 7–15 are filled in `postInit`. If an oredict entry is missing, a placeholder is added: vanilla stone, level 0, all zeros. The meta numbering is therefore fixed.

### `oreCluster2` (5 types used)

| Meta | Ore | Level | Tint | Overlay | Rarity | maxY | Bedrock | Amount ×, Power × |
|---|---|---|---|---|---|---|---|---|
| 0 | nether quartz ore | 2 | 1,1,1 | 0 | 1 | 64 | no | 1.5, 0.35 |
| 1 | `oreTitanIron:0` | 3 | .85,.85,.95 | 1 | 4 | 32 | no | 0.5, 0.8 |
| 2 | first `oreCertusQuartz` (else placeholder) | 3 | .75,.95,1 | 2 | 3 | 32 | no | 1.5, 0.4 |
| 3 | fluid: 1000 mB of `oil` (else placeholder) — "Oil Hole" | 3 | .1,.1,.1 | 3 | 3 | 5 | **yes** | 1.0, 0.5 |
| 4 | glowstone block | 2 | .9,.9,.2 | 2 | 2 | 96 | no | 1.5, 0.35 |

### Custom clusters

[`OreClusterCustom`](block-catalogue.md#registered-blocks) is created only when the config flags are on. It starts with all 16 slots empty (multipliers all 1). MineTweaker fills slots with `setOre(id, ore, level, r, g, b, tex, multPower, multAmount)` or `setFluid(...)`, which also set rarity and maxY to 0 ([08-machines-production](../08-machines-production/index.md) documents the tweaker).

- **Creative tab:** lists every slot with level > 0.
- **Item name:** "Ore Cluster" for every meta.
- **Tooltip:** the fluid's localized name, or the ore's `<unlocalized>.name` translated.

Helper [`TGBlocks.isOrecluster(block)`](block-catalogue.md#registered-blocks) tells whether a block is one of the registered clusters.

---

## 12. Alien bug nest blocks

| Block | Behaviour |
|---|---|
| [`bugnest`](block-catalogue.md#registered-blocks) (Hardened Sand) | Plain sand-coloured block (see §1). The bug-spawning tick is dead code (BLK-11). |
| [`bugnesteggs`](block-catalogue.md#registered-blocks) (Strange Glowing Eggs) | Plain [`GenericBlock`](block-catalogue.md#conventions-and-defaults), sand material. The animated egg texture plays frames 0,1,2,3,2,1,0 at 2 ticks each. It emits light, but `setLightLevel(5.0)` gives an out-of-range 75 ([BLK-10](index.md#suspected-bugs)). |
| [`bugnestslimy`](block-catalogue.md#registered-blocks) (Slime Trail) | Subclass of vanilla `BlockLadder`: climbable, attaches to walls with vanilla ladder rules and vanilla render type 8. Texture `techguns:bugnestslimy` (a transparent slime streak). A helper `getDirectionMeta` (prints to stdout) is never called. |
| [`TGMonsterSpawner`](block-catalogue.md#registered-blocks) | See below. |

### Monster spawner holes (`BlockTGMonsterSpawner`)

A breakable mob spawner disguised as nest sand or concrete with a dark hole on top. The tile entity [`TGSpawnerTileEnt`](../10-entities-npcs/spawning.md#3-monster-spawner-blocks-tgspawnertileent) extends vanilla `TileEntityMobSpawner`. `setValues(...)` writes a vanilla spawner NBT and reloads it.

| Meta | Name | Side / bottom texture | Top texture (side 1) | EntityId | Delay | Min / max spawn delay | Spawn count | Max nearby | Player range | Spawn range |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Bug Hole | `bugnestblock` | `bugnestblock_hole` | [`Techguns.AlienBug`](../01-core/lifecycle-and-registration.md#lifecycle-overview) | 20 | 100 / 300 | 3 | 5 | 24 | 3 |
| 1 | Zombie Hole | `concrete01` | `concrete_hole` | `Techguns.ZombieSoldier` | 20 | 100 / 300 | 3 | 5 | 24 | 3 |

The values are applied in `createNewTileEntity`, i.e. when placed. Saved NBT overrides them on reload. Drops: meta 0 → `bugnest:0`, meta 1 → `concrete:1` ([BLK-12](index.md#suspected-bugs)). No XP. Used by alien bug nests and city structures ([11-worldgen](../11-worldgen/index.md)). The mobs are in [10-entities-npcs](../10-entities-npcs/index.md).

---

## 13. Bio Blob (`BioBlobSmall`, `Bioblob`)

A small glob of green slime stuck to a block face, created by the Bio Gun projectile ([04-projectiles-fx](../04-projectiles-fx/index.md)). It is a BlockContainer with [`BioblobTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-bioblobtileent).

**Shape.** Metadata says which side of the cell the blob is pressed against:

| Meta | Blob sits against | Attached to |
|---|---|---|
| 0 | the top of the cell | the block above |
| 1 | the floor of the cell | the block below |
| 2 | the +Z side | the block to the south |
| 3 | the −Z side | the block to the north |
| 4 | the +X side | the block to the east |
| 5 | the −X side | the block to the west |

Size `s` = 1–3, read from the tile entity. Bounds are computed at 32-px resolution:

- width = (10 + 6s)/32: 16, 22 or 28 px
- thickness = (2 + 4s)/32: 6, 10 or 14 px

The box is centred on the face, flush against the attached side.

**Behaviour.**

- **Contact:** any `EntityLiving` touching it gets Poison III (amplifier 2) for 100 ticks. Players are not `EntityLiving` and are never poisoned ([BLK-08](index.md#suspected-bugs)).
- Not harvestable, no drop, instant break (hardness 0), not opaque, all faces rendered with `techguns:bio`.
- Not in any creative tab.
- **Lifecycle** (in `BioblobTileEnt`, summarised for completeness):
  - Every 60 ticks the size drops by 1. At size 1 the blob vanishes.
  - Each further Bio Gun hit adds power to the size and resets the timer.
  - Above size 3 it bursts: a 3-block bio explosion with `techguns:effects.biodeath` and the `bioblobExplosion` particle. Owned by [04-projectiles-fx](../04-projectiles-fx/index.md).

---

## 14. Creeper Acid (`acid`, `BlockFluidAcid`)

The Forge classic fluid block for fluid `creeper_acid`. Fluid properties (from [`TGFluids`](../06-items/fluids.md)): density 100, luminosity 0, not gaseous, unlocalized `fluid.creeperAcid`.

- Material water, so it behaves like water for swimming and drowning. Unbreakable. It is in the Techguns creative tab ([BLK-26](index.md#suspected-bugs)).
- **Icons:** `techguns:acid_still` on the top and bottom faces, `techguns:acid_flow` on the sides. They are also pushed into the Fluid object as its still/flowing icons.
- It will not displace or flow into any other liquid.
- **Contact:** every collision tick with an `EntityLivingBase` deals 2.0 Techguns poison damage:
  - death type BIO (gory: gore chance 1.0)
  - no knockback
  - respects the normal hurt-resistance window, so in practice once every 10 ticks
- The bucket item is in [06-items](../06-items/index.md).

---

## 15. Air Marker (`airMarker`, `BlockAirMarker`)

An editor-only block. Structure scanning ([`StructureTestTool`](../06-items/special-items.md#structure-test-tool-structuretesttool)) and [`MBlock`](../11-worldgen/structure-system.md#mblock) use it to mean "force air here" in structure templates ([11-worldgen](../11-worldgen/index.md)).

- Cloth material, hardness 1, not a normal cube, not opaque, uses neighbour brightness.
- Rendered as a plain cube with the see-through [`airMarker`](block-catalogue.md#registered-blocks) texture (an "AIR" stencil in a frame).

---

## 16. `ItemBlockMetaDataMachine`

The item block for [`basicMachine`](block-catalogue.md#registered-blocks) and [`machineMultiBlock`](block-catalogue.md#registered-blocks) (blocks owned by [08](../08-machines-production/index.md) and [09](../09-machines-utility/index.md)).

- **Tooltip:** "Has TileEntity Data" when the stack has NBT `TileEntityData`.

After a successful placement:

| Metadata | Effect |
|---|---|
| 6, 8 (explosive charges) | The [`ExplosiveChargeTileEnt`](../09-machines-utility/explosive-charges.md#nbt-explosivechargetileent) orientation is set to the clicked side. On the server, the charge's "plant" sound is played at the block centre, volume 4.0, pitch 1.0. |
| 0, 1, 4, 9 | A [`BasicInventoryTileEnt`](../08-machines-production/framework.md#2-basicinventorytileent) gets `rotation = (q + 2) mod 4`, where q is the yaw quadrant. Used for ammo press, metal press, chem lab and charging station; for `machineMultiBlock` the same metas are RC housing, RC controller and fabricator controller. |
| 3, 5 (camo bench, repair bench) | q is remapped 0↔2, then `rotation = (q − 2) mod 4` with Java remainder, giving values −2, −1, 0 or 1 ([BLK-22](index.md#suspected-bugs), comment "just don't ask why"). |
| others | nothing |

---

## 17. `ItemBlockOreDrill` placement rules

The item block for [`oreDrill`](block-catalogue.md#registered-blocks) (owned by [08](../08-machines-production/index.md)). Constants: rod meta 2, controller meta 4.

**Placing a rod (meta 2).**

1. Look for an ore cluster (any registered cluster block) in the six neighbours, in the order up, down, z−1, x+1, z+1, x−1.
2. If one is found, flood-fill through all connected blocks of that cluster block **and metadata**. If any cell of the cluster touches an existing drill rod, refuse with `techguns.msg.error.alreadyrod` ("Can't place here, this cluster already has a connected drill rod"), shown client-side. Otherwise place.
3. If no cluster is adjacent, the rod may only be placed next to another rod (meta 2). Otherwise refuse silently.

**Placing a controller (meta 4).** Refuse if another controller is adjacent, with chat message key `msg.error.oredrill.controllernext` (missing the `techguns.` prefix, [BLK-17](index.md#suspected-bugs)).

**Other metas** place normally.

Helpers:

- `isOreCluster(world, x, y, z)`
- `isBlock(world, x, y, z, block, meta or −1)`: compares block ids
- `checkForBlockInNeighbourhood`: returns the offset of the first match in the order above, or null

---

## 18. Camo-changeable item blocks (`GenericItemBlockCamoChangeable`)

Used by [`camoNetPane`](block-catalogue.md#registered-blocks), [`camoNetRoof`](block-catalogue.md#registered-blocks) and [`ladder01`](block-catalogue.md#registered-blocks). The blocks implement `IBlockCamoChangeable`:

| Block | `getVariationCount` | `getMetaOffset` | `getVariationName(damage)` |
|---|---|---|---|
| camo net / roof | 3 | 1 | `techguns.tile.<name>.camo.<damage>` |
| ladder | 4 | 4 | the already-translated `techguns.tile.LadderBlock.<damage>` |

The item implements [`ICamoChangeable`](../05-armor/armor-catalogue.md#1-class-hierarchy), so the Camo Bench ([09-machines-utility](../09-machines-utility/index.md)) can cycle it:

- `getCurrentCamoIndex = damage / offset`
- forward: `damage = ((index + 1) mod count) · offset`
- back: `damage = ((index − 1) mod count) · offset`. Java's remainder makes this negative from index 0, which crashes icon lookup ([BLK-05](index.md#suspected-bugs)).

**Tooltip:** `"<Current Camo>: <variation name>"`, e.g. "Current Camo: Woodland" or "Current Camo: Rusty".

---

## 19. `TGChiselBlocks` (block constants for structures)

Static `MBlock` (block + meta) shortcuts into Chisel's block instances (`ChiselBlocks`) and vanilla blocks. Structure generators ([11-worldgen](../11-worldgen/index.md)) use them:

| Field | Block : meta | Comment in source |
|---|---|---|
| [`concrete`](block-catalogue.md#registered-blocks) | Chisel `concrete` : 0 | |
| `concrete_dark` | Chisel `concrete` : 10 | |
| `woodenCrate` | Chisel `planks[0]` (oak) : 10 | wooden box |
| `glowblock` | Chisel `glowstone` : 4 | neon glowstone |
| `factory_box` | Chisel `factoryblock` : 9 | |
| `factory_wireframe` | Chisel `factoryblock` : 5 | |
| `factory_hazard` | Chisel `factoryblock` : 6 | |
| `factory_wall` | Chisel `factoryblock` : 1 | |
| `technical_scaffold` | Chisel `technical` : 0 | |
| `wood_scaffold` | Chisel `planks[0]` : 12 | barracks |
| `wood_pillar` | Chisel `planks[1]` (spruce) : 12 | barracks |
| `wood_wall` | Chisel `planks[0]` : 1 | barracks |
| `wood_floor` | Chisel `planks[0]` : 5 | barracks |
| `wood_roof` | vanilla planks : 1 (spruce) | |
| `wood_roofSlab` | vanilla wooden slab : 1 | |
| `stonebrick_panel` | Chisel `stonebricksmooth` : 12 | panel-hard |
| `stonebrick_small` | Chisel `stonebricksmooth` : 5 | small bricks |
| `stonebrick_sunken` | Chisel `stonebricksmooth` : 13 | sunken |
| `cobblestone_largetile` | Chisel `cobblestone` : 4 | large tile, dark |
| `glass_bubble` | Chisel `glass` : 1 | |

This class makes Chisel a hard dependency ([01-core](../01-core/index.md) bug [CORE-03](../01-core/index.md#suspected-bugs)).

---

## 20. Unused classes

These are in scope but never instantiated or never registered:

- **[`TurretBase`](block-catalogue.md#blocks-constructed-but-not-registered-not-in-game)** (BlockContainer, [`TurretBaseEnt`](../09-machines-utility/turret.md#tile-entity-turretbaseent)): stone sound, hardness 4, pickaxe 2. On add it spawns a [`TurretEntityUNUSED`](../10-entities-npcs/per-mob.md#19-turretentityunused-dead-code) one block up. On break it calls `breakTurret()`. Not referenced. The live turret is `basicMachine:2` ([09](../09-machines-utility/index.md)).
- **[`BlockBugNest`](block-catalogue.md#blocks-constructed-but-not-registered-not-in-game):** a ladder-like 0.35-wide vertical pillar (bounds 0.325–0.675) that is always a ladder. Never instantiated.
- **[`ItemBlockOreCluster`](block-catalogue.md#blocks-constructed-but-not-registered-not-in-game):** names an item "Ore Cluster (<ore name>)". Never used. Clusters use [`GenericItemBlockMetadata`](block-catalogue.md#conventions-and-defaults) and the per-meta lang keys.
- **[`GenericItemBlockMetadataStringPrefix`](block-catalogue.md#conventions-and-defaults):** only used as the superclass of `ItemBlockOreDrill`.
- **[`BlockTGLampFMP`](multipart.md#status-inactive), `smallCube`, `steelframe_glass`:** constructed but not registered (see the catalogue).
- **The multipart classes:** see [multipart.md](multipart.md).
