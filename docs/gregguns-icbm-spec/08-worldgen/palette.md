# Complex palette: blocks, style sets and lamps

- **Baseline:** none (the ICBM baseline has no worldgen).
- **Decisions:** D229 (the redesign), D205 (the complex wall), D217 (floor light), D240 to D244, D248, D251, D259 to D262
- **Pack mods:** Chisel, GregTech, Ztones, Galacticraft (Core and Mars), BiblioCraft (furniture); every one optional (D241, D262)
- **Code:** `com.gregguns.icbm.complex.palette` (`ComplexPalette`, `RoomStyle`, `BlockRef`, `PaletteSets`)

## What it does

Every room, hall and the hub gets a `RoomStyle` from `ComplexPalette.styleFor(kind, roomSeed, dimensionKey, level)`. The call is pure and deterministic. A style is:

- **one wall block**, the same everywhere in the room, drawn once from the style set's wall choices;
- a **floor pattern** and a **ceiling pattern**, each a function of the interior cell `(x, z)` and the interior size (checker, border plus field, double ring, stripes, bands, grid lines, center strip). Interior cell 0 is the first cell inside the section's wall, so a border of width 1 runs along the wall;
- a **lamp pair**, lit and dead;
- an optional **accent** block and **furniture** blocks (table, console, bunk, shelf), all plain cubes or slabs, never tile entities. They are the small rooms' cube furniture, and what a room shows of a BiblioCraft piece when BiblioCraft is absent (see [Furniture](#furniture-d259-d262-d263)).

**The ceiling never shows the wall's block (D261).** A ceiling cell tries, in order: the drawn ceiling design, the set's other ceiling designs, the accent, then vanilla stone and vanilla stone bricks. It takes the first whose resolved block and meta differ from the wall's. The last two differ from each other, so one of them always differs from the wall, in every set and with any mix of pack mods missing. `RoomStyle.ceiling` does this on the server; `RoomStyle.ceilingEntry` is the pure twin the test uses.

Each block is a `BlockRef`: a chain of registry ids, pack block first, GregTech next, vanilla last. `PaletteResolver` walks the chain with `GameRegistry.findBlock` on first use on the server and caches the result; nothing resolves at class load. With no pack mod and no GregTech loaded, every chain ends on a vanilla block, so the dev environment still generates.

## Style sets (12)

| Set | Look | Groups it dresses |
|---|---|---|
| `lab_white` | Chisel Laboratory panels, tiles, screens, the dark gray steel walls and the two-tone checker floor; GT light concrete; vanilla quartz | medical, server room, command, cafeteria |
| `lab_dark` | Chisel Laboratory dark gray steel walls (large and small steel), the two-tone checker tile floor, white panel ceilings (D260) | medical, server room, command |
| `factory_tile` | Chisel Factory plating, metal box, hazard rows; GT dark bricks | power, workshop, vehicle bay, missile assembly, storage, halls, gallery |
| `tyrian_dark` | Chisel Tyrian dark plate, blue strip | command, hub, gallery, major hall, guard, server room |
| `concrete_bunker` | Silo Complex Wall (D205) or Chisel concrete, weathered when abandoned; GT light concrete | storage, guard, minor hall, crew, gallery, vehicle bay |
| `technical_works` | Chisel Technical insulation, bolted plates, pipes, cables; GT basalt bricks | power, water treatment, vehicle bay, workshop, storage, minor hall |
| `marble_admin` | Chisel marble and limestone; GT marble bricks | command, cafeteria, medical, hub |
| `granite_hall` | GT black and red granite bricks and smooth stone | major hall, hub, gallery, command |
| `ztones_server` | Ztones Vect and Korp dark panels with cyan screens | server room, command, guard |
| `ztones_tank` | Ztones Tank grey plating, Lave pale tile | water treatment, power, missile assembly, workshop |
| `crew_warm` | Chisel limestone tile, Ztones Agon color rows | crew, cafeteria, medical |
| `steel_panel` | Chisel Laboratory large and small steel | hub, major hall, gallery, missile assembly, minor hall |

A group with several sets gets variety from the set draw, then from the wall, floor and ceiling draws inside it. Every group has at least two sets; the wall pool of a group has at least four blocks (a test checks it).

## Lamps (D242)

Lit: Chisel light stone (`chisel:glowstone`, light 15, an opaque cube, metas 4, 5, 8, 10, 13, 14, 15 for the tile, neon, panel and bismuth looks), else vanilla glowstone. Dead: a dark panel from the style's own family (Laboratory clear screen, Factory dots, Tyrian black, concrete weathered, Technical vent), else its vanilla fallback. A lamp is a full cube and works in the ceiling plane and as a floor light tile. The writer picks lit or dead from the activity level and the lamp grid; the palette only supplies the pair.

A lit lamp drops 2 to 4 glowstone dust when mined, as vanilla glowstone does: Chisel's light stone drops the dust, not itself. A complex holds about 3,000 lamp marks (ceiling lamps and floor tiles), so an operational one, every lamp lit, yields about 9,000 dust, and a partly abandoned one about 4,000. A complex is a loot site and glowstone is cheap in GTNH, so the yield stands (D248). `ComplexContentCountsTest` prints the lamp count per complex and holds it between 2,000 and 4,500.

Rejected as lamps: Project Red lamps (a tile entity per lamp, and the light comes from a per-world cache the tile feeds, so tens of thousands of them would start a tile tick each and need a power source); vanilla Redstone Lamp (a lit one turns itself off on the first neighbor update with no power); Ztones Flat Lamp (a thin, face-attached block, not a ceiling cube); Galacticraft Arc Lamp (a directional light block, not a cube).

## Planet stone (D244)

On Moon and Mars, a rugged group (storage, power, workshop, guard, halls, gallery, vehicle bay, missile assembly, water treatment) draws, per room, a one-in-three chance each of: local brick as the wall, a local-stone field inside a border (the room's wall block), a local stone and brick checker for the floor. Moon: `GalacticraftCore:tile.moonBlock` meta 4 (Moon Rock) and 14 (Moon Dungeon Brick). Mars: `GalacticraftMars:tile.mars` meta 9 (Mars Stone), 4 (Cobblestone) and 7 (Dungeon Brick). Each falls back to vanilla stone and stone brick. Command, crew, medical, server and cafeteria rooms keep their finish.

## Block table

Worth is the value of mining the block in bulk. **Low** means it drops itself or a cobble, costs ordinary stone, steel-free ingredients or less, and gives no GT material. No block here is a GT casing, a machine, a frame, a metal block or an ore.

| Block (registry id) | Metas used | Look | Use | Worth |
|---|---|---|---|---|
| `chisel:laboratoryblock` | 0 wall panel, 1 dotted panel, 2 large wall, 3 roundel, 5 large tile, 6 small tile, 7 floor tile, 8 checker tile (two-tone), 9 clear screen, 10 fuzz screen, 11 large steel, 12 small steel, 15 info console | White and steel-grey lab panels | wall, floor, ceiling, dead lamp, console | Low (carves from any stone) |
| `chisel:factoryblock` | 0 dots, 3 platex, 6 hazard, 7 hazard orange, 9 metal box, 13 plating, 15 column | Industrial grey plates, hazard stripes | wall, floor, ceiling, accent | Low |
| `chisel:tyrian` | 3 soft plate, 8 plate tiles, 11 blue plating, 12 black, 13 black 2, 15 plate | Dark sci-fi plate | wall, floor, ceiling | Low |
| `chisel:concrete` | 0 default, 1 block, 3 blocks, 4 weathered, 5 weathered block, 10 asphalt | Grey and worn concrete | wall, floor, ceiling, accent, worn | Low |
| `chisel:technical` | 3 large pipes, 5 small pipes, 7 vent, 9 insulation, 11 cables, 12 rusty plates, 13 grate, 15 rusty grate | Pipes, vents, grates | wall, ceiling, floor rows, accent | Low |
| `chisel:marble`, `chisel:limestone` | marble 0, 1, 4, 5, 8, 9, 12; limestone 0, 1, 5, 11, 15 | Pale stone, panels, brick | wall, floor, ceiling | Low |
| `chisel:glowstone` | 4, 5, 8, 10, 13, 14, 15 | Lit tile, neon, panel | lit lamp | Low per block, but drops 2 to 4 glowstone dust like vanilla glowstone: about 9,000 dust from an operational complex (D248) |
| `chisel:bookshelf` | 5 (abandoned) | Bookcase | shelf | Low (drops 3 books, like vanilla). Chisel's own bookshelf block has metas 1 to 8; meta 0 of the group is the vanilla bookshelf, so meta 0 of this block draws the missing texture |
| `gregtech:gt.blockconcretes` | 3, 6, 7, 8, 11, 12, 13, 14, 15 | Dark and light concrete, bricks, smooth | GT fallback for walls and floors | Low (breaks into its cobble) |
| `gregtech:gt.blockstones` | 3 marble bricks, 11 basalt bricks | Marble and basalt | GT fallback | Low |
| `gregtech:gt.blockgranites` | 3, 6, 7 black; 11, 15 red | Granite brick, chiseled, smooth | `granite_hall` | Low, but slow to dig: harvest level 3 (a diamond-level pick; steel is not enough) and blast resistance 60 |
| `Ztones:tile.ztones:vectBlock` (also `vectBlock`) | 0 to 3 | Dark panels | `ztones_server` | Low (decompiled ids) |
| `Ztones:tile.ztones:korpBlock` | 4, 12 | Dark, cyan screen | server accent and console | Low |
| `Ztones:tile.ztones:tankBlock` | 0, 1, 3, 5 | Grey plating | `ztones_tank` | Low |
| `Ztones:tile.ztones:laveBlock` | 0, 1 | Pale blue tile | `ztones_tank` | Low |
| `Ztones:tile.ztones:agonBlock` | 11 red, 13 grey | Solid color | crew accent rows | Low |
| `GalacticraftCore:tile.moonBlock` | 4, 14 | Moon Rock, Moon Dungeon Brick | local stone | Low |
| `GalacticraftMars:tile.mars` | 4, 7, 9 | Mars Cobblestone, Dungeon Brick, Stone | local stone | Low (never meta 8, Block of Desh) |
| `gregguns:complex_wall` | 0 | Steel Bastion Plating look | one wall choice (D205) | None (breaks into itself, no recipe) |
| vanilla: `stone`, `stonebrick` 0 2 3, `quartz_block` 0, `glowstone`, `sandstone` 2, `stained_hardened_clay` 7 14 15, `wool` 0 7 11, `wooden_slab` 1, `stone_slab` 7, `bookshelf` | as listed | Fallbacks and furniture | fallbacks, furniture | Low (glowstone gives 2 to 4 dust per block) |

Not used, on purpose: GT machine casings, GT frame boxes, GT metal and storage blocks, GT sheet-metal blocks (they cost plates), Chisel steel and metal blocks (they carve from metal blocks), Galacticraft tin and aluminum decoration blocks (they pay in tin and aluminum), Desh blocks. Carpenter's Blocks are tile entities per block and are not used.

## Furniture (D259, D262, D263)

The small rooms (see [silo-layout](silo-layout.md#furnishing-d208-d234-d259)) get real furniture from BiblioCraft 1.11.7 when it is loaded, and the cube furniture above when it is not. Each piece is a tile entity, so only the small rooms hold them: about 19 rooms of a complex, and about 900 tile-entity pieces (BiblioCraft and lockers) over 300 plans.

| Piece | BiblioCraft block (registry name) | Meta | Facing (tile NBT) | Without BiblioCraft |
|---|---|---|---|---|
| Chair, stool | `BiblioSeats` | wood | `Angle`; `hasBack` 1 for a chair, 0 for a stool; `backWoodType` | a wooden stair, its back away from the table |
| Table | `BiblioTable` | wood | none; a lone table's `leg1`..`leg4` off, `top1`..`top4` and `monoleg` on (neighbors join when a player builds next to it) | the style's table |
| Desk | `BiblioDesk` | wood | `deskAngle` | the style's table |
| Typewriter | `BiblioTypewriter` | 3 (black) | `angle`, which for the typewriter is the keys' facing itself (seen in the real-pack renders) | none |
| Clipboard | `BiblioClipboard` | 0 | `angle` | none |
| Bookcase | `Bibliotheca` | wood | `bookcaseAngle` | the style's shelf |
| Medicine cabinet | `BiblioPotionShelf` | wood | `potionshelfAngle` | the style's shelf |
| Tool rack | `BiblioRack` | wood | `rackAngle` | the style's shelf |
| Armor stand | `Armor Stand` | angle below, angle + 4 above | the meta | none |
| Locker | GregTech's ULV Locker (`gregtech:gt.blockmachines`, the `Locker_ULV` item), facing into the room (D263) | | GT front facing | the style's shelf |
| Server rack | the style's console block, two high | | | the same |

- **Wood:** one per room, spruce, dark oak, birch or oak, from the plan seed and the room id.
- **Facing:** BiblioCraft turns a piece to face the player who places it, storing `(playerFacing + 1) % 4` (player facing 0 south, 1 west, 2 north, 3 east). A piece whose front faces `f` gets `(f + 3) % 4`; the typewriter's model is a quarter turn off, so it gets `f`. The real-pack renders confirm chairs face their tables, desks face their chairs, and shelves, bookcases, cabinets, clipboards and lockers face into the room.
- **How it is placed:** `FurnitureFiller` (the complex's mark filler) sets the block through the window's sink by registry name (`GameRegistry.findBlock`, guarded by `Loader.isModLoaded("BiblioCraft")`), then writes the facing into the tile's own NBT (`writeToNBT`, set the keys, `readFromNBT`). No BiblioCraft class is linked and none of its assets ship (D262).
- **Worth:** BiblioCraft furniture is wood and drops itself; a GT ULV Locker is a cheap machine. Nothing here is a material sink worth farming.

The Ztones ids come from decompiling `ModBlocks.registerBlocks` (Ztones 2.2.2 has no public source): lamps and tiles register under their plain name, the metal sets under the block's unlocalized name `tile.ztones:<set>Block`. Both spellings are chained, and a wrong guess only means the fallback (a Chisel or GT block) is drawn. `icbm.palette.resolve` logs which chain entries the live pack fails to find.

## Tests

- `CeilingContrastTest` (unit, D261): over every style group, dimension, activity level and 48 seeds, rooms of three sizes and all 64 mixes of the six mods a chain names, no ceiling cell resolves to the wall's block; the drawn design stands wherever it differs; the `lab_dark` set has the steel walls and the checker floor.
- `BiblioFurnitureTest` (unit): every BiblioCraft piece has a registry name, the facing turns all four ways, each piece writes its facing key, and a stair chair's back is opposite its facing.
- `ComplexPaletteTest` (unit): with no pack mods every style resolves to a vanilla block; every registry name and meta is well formed; walls are uniform and styles are deterministic per seed; every group has at least two sets with at least four walls and four floor designs; the Silo Complex Wall is a wall choice; planets use local stone and other dimensions do not; lamps have a lit and a dead twin.
- `icbm.palette.resolve` (scenario): resolves every block of every style set in the live registry (walls, floors, ceilings, lamps, the accent, the worn block and the furniture), checks lit lamps emit 15, dead lamps do not, building blocks are cubes (furniture may be a slab), and that a Chisel block has a variation at its meta (`ICarvable.getManager(meta)` is not null), and notes which mod supplied each block.
