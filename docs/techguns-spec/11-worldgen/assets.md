# Worldgen assets: textures, data files, sounds, lang

## Structure data files
The seven files under `structures/` are decoded in [structure-system.md § 6](structure-system.md#6-structure-data-file-format-assetstechgunsstructures) and described one by one in [structure-catalog.md](structure-catalog.md):

* `structures/airCraftCarrier`
* `structures/bigbunker`
* `structures/cityGasStation`
* `structures/factoryBuildingSmall`
* `structures/houseMedium`
* `structures/smallTrainstation`
* `structures/submarine`

## Textures

This area has no textures of its own. The textures below belong to the ores, ore clusters and authoring tools that worldgen places or uses. They are owned by [07-blocks](../07-blocks/index.md) or [06-items](../06-items/index.md), and are described here so the worldgen picture is complete. All are PNGs with alpha. "Transparent" means alpha 0. Paths are relative to `assets/techguns`.

### Authoring and cluster tools (items, 32×32, transparent background)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| [`textures/items/structureScanTool.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) | 32×32 | A silver magnifying glass: thick metallic ring with a pale lens at the upper left, handle running to the lower right, black 1-px outline. It sits over a short dark-red brick wall (two brick courses) at the lower left. | black outline; grays #868686–#e5e5e5 (metal gradient); dark red #711a1a–#944f4f (bricks, lighter top edges), mortar #2c1212 | Building Scan Tool |
| [`textures/items/structureTestTool.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) | 32×32 | A dark-red brick block in the center, flanked by a silver open-ended wrench (left, head up) and a hammer (right: black head at top, silver handle). Black outlines. | black #000000; grays #828282–#c0c0c0; dark red #711a1a–#731a1a | Structure Test Tool |
| [`textures/items/oreClusterScanner.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) | 32×32 | A hand-held detector: rounded dark-gray box with two thin antennae ending in black knobs at the top corners. Left half is a green screen with a 3×4 grid of bright cells; right half has a dark-red indicator strip above a light-gray round keypad/dial. | box #404040–#494949; screen greens #003a00, #006400, #007900, #00a900, #00cc00; red strip ~#7a0000; dial light gray/white; screen background #002c00 | Ore Cluster Scanner |
| [`textures/items/testItem.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) | 32×32 | Placeholder art: the hand-written black word "LEL", anti-aliased, on a transparent background. | black with partial alpha | Ore Cluster Debugger; default [`GenericItem`](../06-items/special-items.md#base-classes) texture |

### Air marker (block, 32×32)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| [`textures/blocks/airMarker.png`](../07-blocks/textures.md#4-camo-nets-sandbags-tent-flag-air-marker-invisible) | 32×32 | A 2-px dark-gray frame around a transparent face, with the word "AIR" in mid-gray block letters centered inside. It reads as an obvious editor-only marker. | frame #3b3b3b–#484848; letters gray gradient | TG [`airMarker`](../07-blocks/block-catalog.md#registered-blocks) block. Authors placed it in builds to mark cells that must become air; structure palettes map it to air. |

### Ores placed by `OreGenerator` (blocks)

All three 16×16 ores below share one fleck layout drawn over the same vanilla-like stone (grays #757575–#8d8d8d, mostly #7f7f7f). About 10 small irregular flecks, 1–4 px each, are spread evenly. Only the fleck color differs. None are animated.

| Path | Size | Fleck color | Used by |
|---|---|---|---|
| [`textures/blocks/oreCopperBlock.png`](../07-blocks/textures.md#7-ores) | 16×16 | copper orange-brown (#b37646–#d9966a, mostly #bb7546 / #cd7940 / #c06a30) | `oreCopper` |
| [`textures/blocks/oreTinBlock.png`](../07-blocks/textures.md#7-ores) | 16×16 | pale blue-gray (#8fadba–#c4d5de) | `oreTin` |
| [`textures/blocks/oreLeadBlock.png`](../07-blocks/textures.md#7-ores) | 16×16 | dull periwinkle blue (#6d7db7–#9ca3c2) | `oreLead` |

The two animated ores below are vertical 4-frame strips. Each frame is 16×16, stacked top to bottom. Their `.mcmeta` sets `frametime 8`, so each frame shows for 8 ticks and the loop is 32 ticks, in file order. Every frame shares the same stone and fleck layout; only fleck brightness changes between frames, giving a slow glint.

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| [`textures/blocks/oreTitanIron.png`](../07-blocks/textures.md#7-ores) | 16×64 | Stone with clusters of pale silver-white flakes. | stone #686868–#8f8f8f; flakes almost unsaturated pale gray-cyan #bdcbcb–#ccd8d8 and near-white | [`oreTitanIron`](../07-blocks/block-catalog.md#registered-blocks) meta 0 (Titanium Iron Ore) |
| [`textures/blocks/oreUranium.png`](../07-blocks/textures.md#7-ores) | 16×64 | Stone tinted olive-green around flecks of bright toxic green, as a radioactive glow. | stone #747474–#8f8f8f; tinted stone #7a7e74–#989d8f; flecks #41d64a–#baf8bd (mostly #5fe566–#7ff985) | `oreTitanIron` meta 1 (Uranium Ore) |

### Ore-cluster overlays (active, blocks, 32×32, transparent background)

These are grayscale masks. They are tinted at render time by each cluster's RGB and drawn over a stone, netherrack, end-stone or bedrock base cube (see [ore-generation.md § 2.1](ore-generation.md#21-orecluster-block-worldgen-relevant-facts)).

| Path | Overlay index | Depicts | Palette | Used by (cluster metas) |
|---|---|---|---|---|
| [`textures/blocks/oreClusterNew1.png`](../07-blocks/textures.md#ore-cluster-overlays) | 0 | Three large elongated lumps on a diagonal (top-left, top-right, bottom-center), each ~10×7 px, plus 4 small chips near the edges. Shading is bright on the upper-left and dark on the lower-right. | white #ffffff highlights to near-black #161616 | 0 iron, 1 gold, 2 redstone, 7 copper, 8 tin, 9 lead, 11 silver, 12 nickel, 15 uranium; cluster2: 0 nether quartz |
| [`textures/blocks/oreClusterNew2.png`](../07-blocks/textures.md#ore-cluster-overlays) | 1 | About 8–9 small diagonal nuggets (~4×6 px) scattered over the face. Darker overall. | #111111–#4a4a4a with gray highlights | 3 coal, 4 lapis, 10 osmium, 13 aluminum, 14 platinum; cluster2: 1 titanium |
| [`textures/blocks/oreClusterNew3.png`](../07-blocks/textures.md#ore-cluster-overlays) | 2 | One large faceted gem (rhombus, ~11×11 px) in the center, with four small rounded gems near the corners. Glossy white-to-gray shading. | #3e3e3e–#a4a4a4, white tops | 5 diamond, 6 emerald; cluster2: 2 certus quartz, 4 glowstone |
| [`textures/blocks/oreClusterNew4.png`](../07-blocks/textures.md#ore-cluster-overlays) | 3 | One big irregular blob covering ~26×26 px, mottled with lighter swirls, with ragged dark edges. Looks like a puddle or slick. | grays #939393–#b6b6b6, dark rim | cluster2: 3 oil (tinted near-black #1a1a1a on a bedrock base) |

### Legacy ore-cluster textures (unused)

These files are loaded nowhere: their `registerIcon` calls are commented out in [`OreCluster`](../07-blocks/block-catalog.md#registered-blocks). They come from an earlier design in which each cluster look was a full base texture with transparent holes plus a separate overlay that filled those holes. The commented-out code paired them as base → overlay:

* slot 0: [`oreCluster`](../07-blocks/block-catalog.md#registered-blocks) → `oreCluster1`
* slot 1: `oreCluster_1` → `oreCluster_11`
* slot 2: `oreCluster_2` → `oreCluster_21`
* slot 3: `oreCluster_3` → `oreCluster_31`
* slot 4: `oreCluster_4` → `oreCluster_41`
* slot 5: `oreCluster_5` → `oreCluster_21`

| Path | Size | Role | Depicts / palette | Near-duplicate of |
|---|---|---|---|---|
| [`textures/blocks/oreCluster.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | base | Gray stone (#656565–#909090, brick-like noise) with transparent holes in the three-lump layout | holes match `oreClusterNew1` |
| [`textures/blocks/oreCluster1.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | overlay | Three-lump grayscale overlay with stronger pure-white highlights | `oreClusterNew1` |
| [`textures/blocks/oreCluster_1.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | base | Gray stone with ~9 transparent oval holes | holes match `oreClusterNew2` |
| [`textures/blocks/oreCluster_11.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | overlay | Scattered-nugget overlay | `oreClusterNew2` |
| [`textures/blocks/oreCluster_2.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | base | Gray stone with holes in the gem layout (large central rhombus plus 4 corners) | holes match `oreClusterNew3` |
| [`textures/blocks/oreCluster_21.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | overlay | Gem overlay with sharper facets and more white | `oreClusterNew3` |
| [`textures/blocks/oreCluster_3.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | base | **Netherrack-red** base (#c27373, #a65959, #4e2727, #3c0808 noise) with three-lump holes | nether variant of `oreCluster` |
| [`textures/blocks/oreCluster_31.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | overlay | Three-lump overlay in high contrast (mostly black #000000 with white highlights) | dark `oreClusterNew1` |
| [`textures/blocks/oreCluster_4.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | base | Dark stone frame (#2f2f2f–#575757 with black specks) around one large transparent square-ish blob hole | blob hole of `oreClusterNew4` |
| [`textures/blocks/oreCluster_41.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | overlay | Mottled gray blob | `oreClusterNew4` |
| [`textures/blocks/oreCluster_5.png`](../07-blocks/textures.md#legacy-ore-cluster-art) | 32×32 | base | Netherrack-red base with gem-layout holes | nether variant of `oreCluster_2` |

Coverage: 4 item textures, 1 air-marker texture, 5 ore textures, 4 active and 11 legacy cluster textures, for **25 textures**.

## Sounds
**None.** No class in this area plays a sound and no sound file belongs to it. Ore-drill sounds belong to 08. The camp-flag helicopter and NPC sounds belong to [09](../09-machines-utility/index.md)/10.

## Lang keys and in-game strings

### Keys in `lang/en_US.lang` relevant to worldgen
| Key | English |
|---|---|
| `techguns.item.buildingScanTool.name` | Building Scan Tool |
| `techguns.item.structureTestTool.name` | Structure Test Tool |
| `techguns.structureTestTool.Tooltip` | Tool for structure spawning/world editing [WIP] |
| `techguns.item.blockReaderTool.name` | Block Reader Tool |
| `techguns.item.oreClusterDebugger.name` | Ore Cluster Debugger |
| `techguns.oreClusterDebugger.Tooltip` | WARNING: Removes all destroyable Blocks in a huge Radius, used to debug Ore cluster spawns |
| `techguns.item.oreClusterScanner.name` | Ore Cluster Scanner |
| `techguns.OreClusterScanner.msg.found` | Found Ore Clusters in this Chunk |
| `techguns.OreClusterScanner.msg.notFound` | No Clusters in this Chunk |
| `techguns.tile.oreCopper.name` / `oreTin` / `oreLead` | Copper Ore / Tin Ore / Lead Ore |
| `techguns.tile.oreTitanIron_0.name` / `_1` | Titanium Iron Ore / Uranium Ore |
| `techguns.tile.oreCluster.name` | Ore Cluster |
| `techguns.tile.oreCluster_0.name` … `_15.name` | Ore Cluster (Iron / Gold / Redstone / Coal / Lapis / Diamond / Emerald / Copper / Tin / Lead / Osmium / Silver / Nickel / Aluminum / Platinum / Uranium) |
| `techguns.tile.oreCluster2_0.name` … `_4.name` | Ore Cluster (Nether Quartz / Titanium Iron / Certus Quartz / Oil Hole / Glowstone) |
| `techguns.tile.oreClusterCustom.name`, `techguns.tile.oreClusterCustom2.name` | Ore Cluster |
| `techguns.tile.AirMarker.name` | Air Marker |
| `techguns.tile.BasicMachine_7.name` | Flag |
| `techguns.tile.TGMonsterSpawner_0.name` / `_1` | Bug Hole / Zombie Hole |
| `techguns.tile.chisel.bugnest.name`, `techguns.tile.bugnest.0.desc` | Hardened Sand, Bugnest Sand |
| `techguns.tile.bugnesteggs.name` | Strange Glowing Eggs |
| `techguns.tooltip.crowbar.destroycluster` | Can destroy Ore Clusters (crowbar tooltip, owned by [06](../06-items/index.md)) |
| `entity.Techguns.ArmySoldier.name` / `entity.Techguns.Commando.name` | Army Soldier / Commando (camp NPCs, owned by [10](../10-entities-npcs/index.md)) |

### Strings hard-coded in Java (not localized)
| Source | Text | When |
|---|---|---|
| BuildingScanTool, StructureTestTool | `Position1 set (<x>/<y>/<z>).` | first click (client) |
| BuildingScanTool, StructureTestTool | `Position2 set (<x>/<y>/<z>).` then `Size: (<sx>/<sy>/<sz>).` | second click (client) |
| StructureTestTool | `structure tool mode set to "<mode name>".` | sneak-click (client); mode names `Structure`, `Camp`, `Smooth`, `Flatten`, `Remove Junk`, `Sphere`, `Cylinder` |
| OreClusterScanner tooltip | `<stored>/30000 RF` | always |
| CityRoad (stdout) | `Set Road Blocks...`, `Set Road Blocks... DONE!` | every city road |
| MilitaryCamp (stdout) | `WTF!` | a camp segment of size ≤ 0 |
| BuildingScanTool output file | `structure_scan.txt` | written on every scan |


---

## Measured texture values (consistency pass)

Numbers are quoted from [ASSET-FACTS.md](../ASSET-FACTS.md). **Average** is the mean of opaque pixels (alpha > 16). **Dominant** is the three most common colors after bucketing each channel into 8 levels, with their share of opaque pixels. **Opaque** is the share of all pixels that are opaque. The descriptions in the tables above are the visual reading; this section adds the measurements and the frame or pixel-rectangle layout.

### Item textures (32x32, no animation)

| Path | Size | Average | Dominant (share of opaque) | Opaque | Layout (pixel rectangle) |
|---|---|---|---|---|---|
| `textures/items/structureScanTool.png` | 32x32 | #5A4C4C | #101010 36 %, #909090 11 %, #F0F0F0 7 % | 50 % | one icon, (0,0)-(31,31). Magnifier upper left, brick wall lower left |
| `textures/items/structureTestTool.png` | 32x32 | #534444 | #101010 29 %, #303030 12 %, #909090 9 % | 62 % | one icon, (0,0)-(31,31). Brick block center, wrench left, hammer right |
| `textures/items/oreClusterScanner.png` | 32x32 | #3E553C | #505050 41 %, #303030 15 %, #707070 12 % | 48 % | one icon, (0,0)-(31,31). Screen on the left half, indicator and dial on the right half |
| `textures/items/testItem.png` | 32x32 | #000000 | #101010 100 % | 28 % | one icon, (0,0)-(31,31). The word "LEL" only; the remaining 72 % is transparent |

In words: the two authoring tools are dark, nearly black icons with a reddish-brown cast from the brick art (average #5A4C4C and #534444). The scanner is the greenest of the four (average #3E553C, from the screen) over a dark-gray body. The placeholder has a pure black average because its only opaque pixels are black.

### Block textures: air marker and ores

| Path | Size | Frames | Average | Dominant | Opaque | Layout |
|---|---|---|---|---|---|---|
| `textures/blocks/airMarker.png` | 32x32 | 1 | #4E4E4E | #505050 38 %, #303030 35 %, #707070 21 % | 38 % | frame (0,0)-(31,31); the face inside the 2-px border is transparent, so only the border and the "AIR" letters are opaque |
| `textures/blocks/oreCopperBlock.png` | 16x16 | 1 | #897D75 | #707070 48 %, #909090 26 %, #907070 8 % | 100 % | (0,0)-(15,15) |
| `textures/blocks/oreTinBlock.png` | 16x16 | 1 | #838789 | #707070 49 %, #909090 30 %, #B0D0D0 5 % | 100 % | (0,0)-(15,15) |
| `textures/blocks/oreLeadBlock.png` | 16x16 | 1 | #7E8087 | #707070 51 %, #909090 27 %, #7090B0 5 % | 100 % | (0,0)-(15,15) |
| `textures/blocks/oreTitanIron.png` | 16x64 | 4 (frametime 8) | #878989 | #707070 65 %, #909090 19 %, #D0D0D0 6 % | 100 % | frames at y 0-15, 16-31, 32-47, 48-63, each (0,y)-(15,y+15) |
| `textures/blocks/oreUranium.png` | 16x64 | 4 (frametime 8) | #7F927C | #707070 38 %, #909090 19 %, #709070 14 % | 100 % | frames at y 0-15, 16-31, 32-47, 48-63 |

The ore averages show how little of each block is ore: copper, tin and lead are all within a few levels of gray (#897D75, #838789, #7E8087), with copper warmest and lead coolest and bluest. Uranium is the only one whose average is clearly tinted green (#7F927C).

### Ore-cluster overlays and legacy cluster art (32x32, no animation)

Active overlays (each is one frame, (0,0)-(31,31)):

| Path | Average | Dominant | Opaque |
|---|---|---|---|
| `textures/blocks/oreClusterNew1.png` | #696969 | #303030 20 %, #101010 15 %, #707070 15 % | 37 % |
| `textures/blocks/oreClusterNew2.png` | #545454 | #303030 29 %, #505050 20 %, #707070 15 % | 33 % |
| `textures/blocks/oreClusterNew3.png` | #838383 | #303030 17 %, #707070 16 %, #B0B0B0 15 % | 23 % |
| `textures/blocks/oreClusterNew4.png` | #929292 | #909090 30 %, #B0B0B0 30 %, #D0D0D0 12 % | 57 % |

Legacy cluster textures (unused):

| Path | Role | Average | Dominant | Opaque |
|---|---|---|---|---|
| `textures/blocks/oreCluster.png` | base | #727272 | #707070 59 %, #909090 26 %, #505050 14 % | 62 % |
| `textures/blocks/oreCluster1.png` | overlay | #808080 | #707070 16 %, #909090 15 %, #303030 13 % | 37 % |
| `textures/blocks/oreCluster_1.png` | base | #797979 | #707070 52 %, #909090 43 %, #505050 3 % | 66 % |
| `textures/blocks/oreCluster_11.png` | overlay | #6F6F6F | #303030 20 %, #505050 19 %, #909090 18 % | 33 % |
| `textures/blocks/oreCluster_2.png` | base | #7A7A7A | #909090 48 %, #707070 47 %, #505050 4 % | 76 % |
| `textures/blocks/oreCluster_21.png` | overlay | #A9A9A9 | #F0F0F0 26 %, #707070 19 %, #909090 16 % | 23 % |
| `textures/blocks/oreCluster_3.png` | base (nether) | #612F2D | #301010 20 %, #503030 17 %, #501010 11 % | 62 % |
| `textures/blocks/oreCluster_31.png` | overlay | #6B6B6B | #101010 30 %, #F0F0F0 13 %, #303030 11 % | 37 % |
| `textures/blocks/oreCluster_4.png` | base | #4D4D4D | #505050 39 %, #303030 27 %, #909090 15 % | 42 % |
| `textures/blocks/oreCluster_41.png` | overlay | #A1A1A1 | #B0B0B0 36 %, #909090 27 %, #D0D0D0 19 % | 57 % |
| `textures/blocks/oreCluster_5.png` | base (nether) | #6B3331 | #503030 16 %, #301010 15 %, #905050 12 % | 76 % |

In words: the four active overlays are grayscale masks (no color at all in the averages), so their final color comes entirely from the per-meta tint in [ore-generation.md](ore-generation.md). `oreClusterNew4` is the lightest (#929292) and the most opaque of the four (57 %); `oreClusterNew3` the sparsest (23 %). The two nether-red legacy bases (`oreCluster_3`, `oreCluster_5`) are the only colored files in the set (averages #612F2D and #6B3331).

Opaque shares tie each base to its overlay: a legacy base and the overlay that fills its holes together cover the face, so `oreCluster` (62 % opaque) pairs with `oreCluster1` (37 %), `oreCluster_1` (66 %) with `oreCluster_11` (33 %) and `oreCluster_2` (76 %) with `oreCluster_21` (23 %); each pair sums to about 100 %.

Structure data files (`structures/*`) are text, not textures; their sizes and entry counts are in [structure-system.md](structure-system.md#6-structure-data-file-format-assetstechgunsstructures).

---

## Lang section (full)

The language file is `assets/techguns/lang/en_US.lang` (the only language shipped; 697 lines). Line numbers below are from that file. The keys are grouped by owner; every key whose subject is placed, mined or used by worldgen is listed, with its exact English text. Keys owned by other areas appear only where worldgen depends on them.

### Keys in scope

| Key | Line | English | Owner |
|---|---|---|---|
| `techguns.tile.oreTin.name` | 69 | Tin Ore | [07](../07-blocks/index.md) |
| `techguns.tile.oreCopper.name` | 70 | Copper Ore | [07](../07-blocks/index.md) |
| `techguns.tile.oreLead.name` | 71 | Lead Ore | [07](../07-blocks/index.md) |
| `techguns.tile.oreTitanIron_0.name` | 532 | Titanium Iron Ore | [07](../07-blocks/index.md) |
| `techguns.tile.oreTitanIron_1.name` | 533 | Uranium Ore | [07](../07-blocks/index.md) |
| `techguns.tile.oreCluster.name` | 226 | Ore Cluster | [07](../07-blocks/index.md) |
| `techguns.tile.oreCluster_0.name` to `_15.name` | 227-242 | Ore Cluster (Iron), (Gold), (Redstone), (Coal), (Lapis), (Diamond), (Emerald), (Copper), (Tin), (Lead), (Osmium), (Silver), (Nickel), (Aluminum), (Platinum), (Uranium) | [07](../07-blocks/index.md) |
| `techguns.tile.oreCluster2_0.name` to `_3.name` | 243-246 | Ore Cluster (Nether Quartz), (Titanium Iron), (Certus Quartz), (Oil Hole) | [07](../07-blocks/index.md) |
| `techguns.tile.oreCluster2_4.name` | 647 | Ore Cluster (Glowstone) | [07](../07-blocks/index.md) |
| `techguns.tile.oreClusterCustom.name` | 677 | Ore Cluster | [07](../07-blocks/index.md) |
| `techguns.tile.oreClusterCustom2.name` | 681 | Ore Cluster | [07](../07-blocks/index.md) |
| `techguns.item.oreClusterScanner.name` | 541 | Ore Cluster Scanner | [06](../06-items/index.md) |
| `techguns.OreClusterScanner.msg.found` | 539 | Found Ore Clusters in this Chunk | [06](../06-items/index.md) |
| `techguns.OreClusterScanner.msg.notFound` | 540 | No Clusters in this Chunk | [06](../06-items/index.md) |
| `techguns.item.oreClusterDebugger.name` | 472 | Ore Cluster Debugger | [06](../06-items/index.md) |
| `techguns.oreClusterDebugger.Tooltip` | 467 | WARNING: Removes all destroyable Blocks in a huge Radius, used to debug Ore cluster spawns | [06](../06-items/index.md) |
| `techguns.item.structureTestTool.name` | 469 | Structure Test Tool | [06](../06-items/index.md) |
| `techguns.structureTestTool.Tooltip` | 468 | Tool for structure spawning/world editing [WIP] | [06](../06-items/index.md) |
| `techguns.item.buildingScanTool.name` | 470 | Building Scan Tool | [06](../06-items/index.md) |
| `techguns.item.blockReaderTool.name` | 471 | Block Reader Tool | [06](../06-items/index.md) |
| `techguns.tooltip.crowbar.destroycluster` | 679 | Can destroy Ore Clusters | [06](../06-items/index.md) |
| `techguns.tile.AirMarker.name` | 617 | Air Marker | [07](../07-blocks/index.md) |
| `techguns.tile.BasicMachine_7.name` | 193 | Flag | [09](../09-machines-utility/index.md) |
| `techguns.tile.TGMonsterSpawner_0.name` | 582 | Bug Hole | [07](../07-blocks/index.md) |
| `techguns.tile.TGMonsterSpawner_1.name` | 583 | Zombie Hole | [07](../07-blocks/index.md) |
| `techguns.tile.chisel.bugnest.name` | 576 | Hardened Sand | [07](../07-blocks/index.md) |
| `techguns.tile.bugnest.0.desc` | 596 | Bugnest Sand | [07](../07-blocks/index.md) |
| `techguns.tile.bugnesteggs.name` | 577 | Strange Glowing Eggs | [07](../07-blocks/index.md) |
| `techguns.tile.chisel.container.name` | 146 | Container Block | [07](../07-blocks/index.md) |
| `techguns.tile.container.0.desc` | 147 | Shipping Container (Red) (used by the camp [`Containers`](structure-catalog.md#b2-containers-containers) part) | [07](../07-blocks/index.md) |
| `techguns.tile.chisel.concrete.name` | 153 | Concrete | [07](../07-blocks/index.md) |
| `techguns.tile.concrete.0.desc` to `.6.desc` | 154-160 | Light, Dark, Steel Frame Light, Steel Frame Dark, Weathered Column, Light Noise, Dark Noise | [07](../07-blocks/index.md) |
| `techguns.tile.chisel.metalpanel.name` | 161 | Metal Panel | [07](../07-blocks/index.md) |
| `techguns.tile.metalpanel.3.desc` | 165 | Dark (the submarine hull) | [07](../07-blocks/index.md) |
| `techguns.tile.chisel.metalpanel2.name` | 170 | Metal Panel (Connected) | [07](../07-blocks/index.md) |
| `techguns.tile.metalpanel2.3.desc` | 174 | Dark Bars | [07](../07-blocks/index.md) |
| `techguns.tile.camoNetPane.name` / `camoNetRoof.name` | 269 / 270 | Camo Net / Camo Net Roof | [07](../07-blocks/index.md) |
| `techguns.tile.camoNetPane_0.name` / `_2` / `_1` | 413 / 414 / 415 | Camo Net (Woodland) / (Desert) / (Snow) | [07](../07-blocks/index.md) |
| `techguns.tile.sandbags.name` | 412 | Sandbags | [07](../07-blocks/index.md) |
| `techguns.tile.tgchest.name` | 635 | Reinforced Chest | [07](../07-blocks/index.md) |
| `techguns.tile.tgchest_weapon.name` | 636 | Weapon Chest | [07](../07-blocks/index.md) |
| `techguns.container.tgchest` / `.double` | 628 / 629 | Reinforced Chest / Large Reinforced Chest | [07](../07-blocks/index.md) |
| `techguns.container.tgchest_weapon` / `.double` | 637 / 638 | Weapon Chest / Large Weapon Chest | [07](../07-blocks/index.md) |
| `techguns.item.bunkerDoor.name` | 473 | Bunker Door | [06](../06-items/index.md) |
| `techguns.tile.Lamp_0.name` to `_15.name` | 337-352 | Lamp (Yellow) for 0-6, Lamp (White) for 8-14, Lantern (Yellow) for 7, Lantern (White) for 15 | [07](../07-blocks/index.md) |
| `techguns.tile.LadderBlock_0.name` to `_15.name` | 364-379 | Metal Ladder (Gray) / (Rusty) / (Shiny) / (Black) in groups of four | [07](../07-blocks/index.md) |
| `entity.Techguns.ArmySoldier.name` | 272 | Army Soldier | 10 |
| `entity.Techguns.Commando.name` | 644 | Commando | 10 |
| `entity.Techguns.ZombieSoldier.name` | 273 | Zombie Soldier | 10 |
| `entity.Techguns.AlienBug.name` | 282 | Alien Bug | 10 |
| `entity.Techguns.AttackHelicopter.name` | 271 and 275 | Attack Helicopter (defined twice, same text) | 10 |

Notes on the table:

* The `techguns.tile.chisel.*` keys and the `.desc` keys are the parent-name and per-variant description keys of the concrete, container, metal-panel and bug-nest blocks. How they are looked up is documented in [07-blocks](../07-blocks/index.md).
* The Zombie Hole and Bug Hole names are the block names; the mob names are the `entity.Techguns.*` keys.
* Structure names (Small Factory Building, Aircraft Carrier, and so on) have no lang keys. Structures are not items and have no in-game name.

### Strings generated in code

| Source | Text or format | Condition |
|---|---|---|
| [`OreClusterScanner`](../06-items/special-items.md#ore-cluster-scanner-oreclusterscanner-techgunsoreclusterscanner) tooltip | `<stored>/30000 RF` | always shown on the item |
| `OreClusterScanner` chat | the translated names `techguns.tile.<block>_<meta>.name`, one line per distinct cluster, after the "found" message | a successful scan found at least one cluster |
| `OreClusterScanner` chat | `techguns.OreClusterScanner.msg.found` / `msg.notFound` | end of a completed scan (client only) |
| [`BuildingScanTool`](../06-items/special-items.md#building-scan-tool-buildingscantool), [`StructureTestTool`](../06-items/special-items.md#structure-test-tool-structuretesttool) chat | `Position1 set (x/y/z).` | first click (client) |
| `BuildingScanTool`, `StructureTestTool` chat | `Position2 set (x/y/z).` and `Size: (sx/sy/sz).` | second click (client) |
| `StructureTestTool` chat | `structure tool mode set to "<mode>".` with modes `Structure`, `Camp`, `Smooth`, `Flatten`, `Remove Junk`, `Sphere`, `Cylinder` | sneak-click (client) |
| `BuildingScanTool` output | the file `structure_scan.txt` and generated Java lines | every completed scan (server) |
| [`CityRoad`](structure-catalog.md#c1-city-generator-genericcity-genericcitysegment-citystructure-citybuilding-citybuildingtype-buildingstyle-cityroad-iroad-partsstaircase) stdout | `Set Road Blocks...`, `Set Road Blocks... DONE!` | every city road (dormant code) |
| [`MilitaryCamp`](structure-catalog.md#a6-military-base-militarybasestructure--militarycamp) stdout | `WTF!` | a camp segment of size 0 or less |

### Hard-coded English (not localized)

All of the chat strings above for the two authoring tools (`Position1 set`, `Position2 set`, `Size:`, `structure tool mode set to`, and the seven mode names), the two stdout debug strings, and the `structure_scan.txt` file name. None passes through the lang file, so none can be translated.

### Missing keys and oddities

| Item | Detail |
|---|---|
| `techguns.tile.oreCluster2.name` | absent. `oreCluster.name` ("Ore Cluster") exists, but its twin does not. The per-meta keys `oreCluster2_0` to `_4` exist, so the gap is only visible if a name is requested for the block without a meta. |
| `techguns.tile.oreCluster2_5.name` to `_15.name` | absent. Those metas are unused (mining level 0, hidden from creative) so the gap is harmless |
| `techguns.tile.oreClusterCustom_<meta>.name` | absent; every custom-cluster item shows the shared text "Ore Cluster" (`oreClusterCustom.name`, `oreClusterCustom2.name`) regardless of meta |
| `techguns.tile.oreTitanIron.name` | absent (only the per-meta `_0` and `_1`) |
| `techguns.tile.Lamp_*` and `LadderBlock_*` | several metas share one text, so the creative list shows duplicate-looking names (for example seven "Lamp (Yellow)" entries) |
| Chisel parent names | `techguns.tile.chisel.bugnest.name` is "Hardened Sand" while the variant text is "Bugnest Sand", so the same block has two English names depending on where it is shown |
| Duplicate definitions | `entity.Techguns.AttackHelicopter.name` (lines 271 and 275) is defined twice with the same text |
| Structure names | none; structures are unlocalized by design |
