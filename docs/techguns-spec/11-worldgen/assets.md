# Worldgen assets: textures, data files, sounds, lang

## Structure data files
The seven files under `structures/` are decoded in [structure-system.md § 6](structure-system.md#6-structure-data-file-format-assetstechgunsstructures) and described one by one in [structure-catalogue.md](structure-catalogue.md):

* `structures/airCraftCarrier`
* `structures/bigbunker`
* `structures/cityGasStation`
* `structures/factoryBuildingSmall`
* `structures/houseMedium`
* `structures/smallTrainstation`
* `structures/submarine`

## Textures

This area has no textures of its own. The textures below belong to the ores, ore clusters and authoring tools that worldgen places or uses. They are owned by 07-blocks or 06-items, and are described here so the worldgen picture is complete. All are PNGs with alpha. "Transparent" means alpha 0. Paths are relative to `assets/techguns`.

### Authoring and cluster tools (items, 32×32, transparent background)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/items/structureScanTool.png` | 32×32 | A silver magnifying glass: thick metallic ring with a pale lens at the upper left, handle running to the lower right, black 1-px outline. It sits over a short dark-red brick wall (two brick courses) at the lower left. | black outline; greys #868686–#e5e5e5 (metal gradient); dark red #711a1a–#944f4f (bricks, lighter top edges), mortar #2c1212 | Building Scan Tool |
| `textures/items/structureTestTool.png` | 32×32 | A dark-red brick block in the centre, flanked by a silver open-ended wrench (left, head up) and a hammer (right: black head at top, silver handle). Black outlines. | black #000000; greys #828282–#c0c0c0; dark red #711a1a–#731a1a | Structure Test Tool |
| `textures/items/oreClusterScanner.png` | 32×32 | A hand-held detector: rounded dark-grey box with two thin antennae ending in black knobs at the top corners. Left half is a green screen with a 3×4 grid of bright cells; right half has a dark-red indicator strip above a light-grey round keypad/dial. | box #404040–#494949; screen greens #003a00, #006400, #007900, #00a900, #00cc00; red strip ~#7a0000; dial light grey/white; screen background #002c00 | Ore Cluster Scanner |
| `textures/items/testItem.png` | 32×32 | Placeholder art: the hand-written black word "LEL", anti-aliased, on a transparent background. | black with partial alpha | Ore Cluster Debugger; default `GenericItem` texture |

### Air marker (block, 32×32)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/blocks/airMarker.png` | 32×32 | A 2-px dark-grey frame around a transparent face, with the word "AIR" in mid-grey block letters centred inside. It reads as an obvious editor-only marker. | frame #3b3b3b–#484848; letters grey gradient | TG `airMarker` block. Authors placed it in builds to mark cells that must become air; structure palettes map it to air. |

### Ores placed by `OreGenerator` (blocks)

All three 16×16 ores below share one fleck layout drawn over the same vanilla-like stone (greys #757575–#8d8d8d, mostly #7f7f7f). About 10 small irregular flecks, 1–4 px each, are spread evenly. Only the fleck colour differs. None are animated.

| Path | Size | Fleck colour | Used by |
|---|---|---|---|
| `textures/blocks/oreCopperBlock.png` | 16×16 | copper orange-brown (#b37646–#d9966a, mostly #bb7546 / #cd7940 / #c06a30) | `oreCopper` |
| `textures/blocks/oreTinBlock.png` | 16×16 | pale blue-grey (#8fadba–#c4d5de) | `oreTin` |
| `textures/blocks/oreLeadBlock.png` | 16×16 | dull periwinkle blue (#6d7db7–#9ca3c2) | `oreLead` |

The two animated ores below are vertical 4-frame strips. Each frame is 16×16, stacked top to bottom. Their `.mcmeta` sets `frametime 8`, so each frame shows for 8 ticks and the loop is 32 ticks, in file order. Every frame shares the same stone and fleck layout; only fleck brightness changes between frames, giving a slow glint.

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/blocks/oreTitanIron.png` | 16×64 | Stone with clusters of pale silver-white flakes. | stone #686868–#8f8f8f; flakes almost unsaturated pale grey-cyan #bdcbcb–#ccd8d8 and near-white | `oreTitanIron` meta 0 (Titanium Iron Ore) |
| `textures/blocks/oreUranium.png` | 16×64 | Stone tinted olive-green around flecks of bright toxic green, as a radioactive glow. | stone #747474–#8f8f8f; tinted stone #7a7e74–#989d8f; flecks #41d64a–#baf8bd (mostly #5fe566–#7ff985) | `oreTitanIron` meta 1 (Uranium Ore) |

### Ore-cluster overlays (active, blocks, 32×32, transparent background)

These are greyscale masks. They are tinted at render time by each cluster's RGB and drawn over a stone, netherrack, end-stone or bedrock base cube (see [ore-generation.md § 2.1](ore-generation.md#21-orecluster-block-worldgen-relevant-facts)).

| Path | Overlay index | Depicts | Palette | Used by (cluster metas) |
|---|---|---|---|---|
| `textures/blocks/oreClusterNew1.png` | 0 | Three large elongated lumps on a diagonal (top-left, top-right, bottom-centre), each ~10×7 px, plus 4 small chips near the edges. Shading is bright on the upper-left and dark on the lower-right. | white #ffffff highlights to near-black #161616 | 0 iron, 1 gold, 2 redstone, 7 copper, 8 tin, 9 lead, 11 silver, 12 nickel, 15 uranium; cluster2: 0 nether quartz |
| `textures/blocks/oreClusterNew2.png` | 1 | About 8–9 small diagonal nuggets (~4×6 px) scattered over the face. Darker overall. | #111111–#4a4a4a with grey highlights | 3 coal, 4 lapis, 10 osmium, 13 aluminium, 14 platinum; cluster2: 1 titanium |
| `textures/blocks/oreClusterNew3.png` | 2 | One large faceted gem (rhombus, ~11×11 px) in the centre, with four small rounded gems near the corners. Glossy white-to-grey shading. | #3e3e3e–#a4a4a4, white tops | 5 diamond, 6 emerald; cluster2: 2 certus quartz, 4 glowstone |
| `textures/blocks/oreClusterNew4.png` | 3 | One big irregular blob covering ~26×26 px, mottled with lighter swirls, with ragged dark edges. Looks like a puddle or slick. | greys #939393–#b6b6b6, dark rim | cluster2: 3 oil (tinted near-black #1a1a1a on a bedrock base) |

### Legacy ore-cluster textures (unused)

These files are loaded nowhere: their `registerIcon` calls are commented out in `OreCluster`. They come from an earlier design in which each cluster look was a full base texture with transparent holes plus a separate overlay that filled those holes. The commented-out code paired them as base → overlay:

* slot 0: `oreCluster` → `oreCluster1`
* slot 1: `oreCluster_1` → `oreCluster_11`
* slot 2: `oreCluster_2` → `oreCluster_21`
* slot 3: `oreCluster_3` → `oreCluster_31`
* slot 4: `oreCluster_4` → `oreCluster_41`
* slot 5: `oreCluster_5` → `oreCluster_21`

| Path | Size | Role | Depicts / palette | Near-duplicate of |
|---|---|---|---|---|
| `textures/blocks/oreCluster.png` | 32×32 | base | Grey stone (#656565–#909090, brick-like noise) with transparent holes in the three-lump layout | holes match `oreClusterNew1` |
| `textures/blocks/oreCluster1.png` | 32×32 | overlay | Three-lump greyscale overlay with stronger pure-white highlights | `oreClusterNew1` |
| `textures/blocks/oreCluster_1.png` | 32×32 | base | Grey stone with ~9 transparent oval holes | holes match `oreClusterNew2` |
| `textures/blocks/oreCluster_11.png` | 32×32 | overlay | Scattered-nugget overlay | `oreClusterNew2` |
| `textures/blocks/oreCluster_2.png` | 32×32 | base | Grey stone with holes in the gem layout (large central rhombus plus 4 corners) | holes match `oreClusterNew3` |
| `textures/blocks/oreCluster_21.png` | 32×32 | overlay | Gem overlay with sharper facets and more white | `oreClusterNew3` |
| `textures/blocks/oreCluster_3.png` | 32×32 | base | **Netherrack-red** base (#c27373, #a65959, #4e2727, #3c0808 noise) with three-lump holes | nether variant of `oreCluster` |
| `textures/blocks/oreCluster_31.png` | 32×32 | overlay | Three-lump overlay in high contrast (mostly black #000000 with white highlights) | dark `oreClusterNew1` |
| `textures/blocks/oreCluster_4.png` | 32×32 | base | Dark stone frame (#2f2f2f–#575757 with black specks) around one large transparent square-ish blob hole | blob hole of `oreClusterNew4` |
| `textures/blocks/oreCluster_41.png` | 32×32 | overlay | Mottled grey blob | `oreClusterNew4` |
| `textures/blocks/oreCluster_5.png` | 32×32 | base | Netherrack-red base with gem-layout holes | nether variant of `oreCluster_2` |

Coverage: 4 item textures, 1 air-marker texture, 5 ore textures, 4 active and 11 legacy cluster textures, for **25 textures**.

## Sounds
**None.** No class in this area plays a sound and no sound file belongs to it. Ore-drill sounds belong to 08. The camp-flag helicopter and NPC sounds belong to 09/10.

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
| `techguns.tile.oreCluster_0.name` … `_15.name` | Ore Cluster (Iron / Gold / Redstone / Coal / Lapis / Diamond / Emerald / Copper / Tin / Lead / Osmium / Silver / Nickel / Aluminium / Platinum / Uranium) |
| `techguns.tile.oreCluster2_0.name` … `_4.name` | Ore Cluster (Nether Quartz / Titanium Iron / Certus Quartz / Oil Hole / Glowstone) |
| `techguns.tile.oreClusterCustom.name`, `techguns.tile.oreClusterCustom2.name` | Ore Cluster |
| `techguns.tile.AirMarker.name` | Air Marker |
| `techguns.tile.BasicMachine_7.name` | Flag |
| `techguns.tile.TGMonsterSpawner_0.name` / `_1` | Bug Hole / Zombie Hole |
| `techguns.tile.chisel.bugnest.name`, `techguns.tile.bugnest.0.desc` | Hardened Sand, Bugnest Sand |
| `techguns.tile.bugnesteggs.name` | Strange Glowing Eggs |
| `techguns.tooltip.crowbar.destroycluster` | Can destroy Ore Clusters (crowbar tooltip, owned by 06) |
| `entity.Techguns.ArmySoldier.name` / `entity.Techguns.Commando.name` | Army Soldier / Commando (camp NPCs, owned by 10) |

### Strings hard-coded in Java (not localised)
| Source | Text | When |
|---|---|---|
| BuildingScanTool, StructureTestTool | `Position1 set (<x>/<y>/<z>).` | first click (client) |
| BuildingScanTool, StructureTestTool | `Position2 set (<x>/<y>/<z>).` then `Size: (<sx>/<sy>/<sz>).` | second click (client) |
| StructureTestTool | `structure tool mode set to "<mode name>".` | sneak-click (client); mode names `Structure`, `Camp`, `Smooth`, `Flatten`, `Remove Junk`, `Sphere`, `Cylinder` |
| OreClusterScanner tooltip | `<stored>/30000 RF` | always |
| CityRoad (stdout) | `Set Road Blocks...`, `Set Road Blocks... DONE!` | every city road |
| MilitaryCamp (stdout) | `WTF!` | a camp segment of size ≤ 0 |
| BuildingScanTool output file | `structure_scan.txt` | written on every scan |
