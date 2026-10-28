# Complex palette: blocks, style sets and lamps

- **Baseline:** none (the ICBM baseline has no worldgen).
- **Decisions:** D229 (the redesign), D205 (the complex wall), D217 (floor light), D240 to D244
- **Pack mods:** Chisel, GregTech, Ztones, Galacticraft (Core and Mars); every one optional (D241)
- **Code:** `com.gregguns.icbm.complex.palette` (`ComplexPalette`, `RoomStyle`, `BlockRef`, `PaletteSets`)

## What it does

Every room, hall and the hub gets a `RoomStyle` from `ComplexPalette.styleFor(kind, roomSeed, dimensionKey, level)`. The call is pure and deterministic. A style is:

- **one wall block**, the same everywhere in the room, drawn once from the style set's wall choices;
- a **floor pattern** and a **ceiling pattern**, each a function of the room-local cell `(x, z)` and the room size (checker, border plus field, double ring, stripes, bands, grid lines, center strip);
- a **lamp pair**, lit and dead;
- an optional **accent** block and **furniture** blocks (table, console, bunk, shelf), all plain cubes or slabs, never tile entities.

Each block is a `BlockRef`: a chain of registry ids, pack block first, GregTech next, vanilla last. `PaletteResolver` walks the chain with `GameRegistry.findBlock` on first use on the server and caches the result; nothing resolves at class load. With no pack mod and no GregTech loaded, every chain ends on a vanilla block, so the dev environment still generates.

## Style sets (11)

| Set | Look | Groups it dresses |
|---|---|---|
| `lab_white` | Chisel Laboratory panels, tiles, screens; GT light concrete; vanilla quartz | medical, server room, command, cafeteria |
| `factory_tile` | Chisel Factory plating, metal box, hazard rows; GT dark bricks | power, workshop, vehicle bay, missile assembly, storage, halls, gallery |
| `tyrian_dark` | Chisel Tyrian dark plate, blue strip | command, hub, gallery, major hall, guard, server room |
| `concrete_bunker` | Silo Complex Wall (D205) or Chisel concrete, weathered when abandoned; GT light concrete | storage, guard, minor hall, crew, gallery, vehicle bay |
| `technical_works` | Chisel Technical insulation, bolted plates, pipes, cables; GT basalt bricks | power, water treatment, vehicle bay, workshop, storage, minor hall |
| `marble_admin` | Chisel marble and limestone; GT marble bricks | command, cafeteria, medical, hub |
| `granite_hall` | GT black and red granite bricks and smooth stone | major hall, hub, gallery, command |
| `ztones_server` | Ztones Vect and Korp dark panels with cyan screens | server room, command, guard |
| `ztones_tank` | Ztones Tank gray plating, Lave pale tile | water treatment, power, missile assembly, workshop |
| `crew_warm` | Chisel limestone tile, Ztones Agon color rows | crew, cafeteria, medical |
| `steel_panel` | Chisel Laboratory large and small steel | hub, major hall, gallery, missile assembly, minor hall |

A group with several sets gets variety from the set draw, then from the wall, floor and ceiling draws inside it. Every group has at least two sets; the wall pool of a group has at least four blocks (a test checks it).

## Lamps (D242)

Lit: Chisel light stone (`chisel:glowstone`, light 15, an opaque cube, metas 4, 5, 8, 10, 13, 14, 15 for the tile, neon, panel and bismuth looks), else vanilla glowstone. Dead: a dark panel from the style's own family (Laboratory clear screen, Factory dots, Tyrian black, concrete weathered, Technical vent), else its vanilla fallback. A lamp is a full cube and works in the ceiling plane and as a floor light tile. The writer picks lit or dead from the activity level and the lamp grid; the palette only supplies the pair.

Rejected as lamps: Project Red lamps (a tile entity per lamp, and the light comes from a per-world cache the tile feeds, so tens of thousands of them would start a tile tick each and need a power source); vanilla Redstone Lamp (a lit one turns itself off on the first neighbor update with no power); Ztones Flat Lamp (a thin, face-attached block, not a ceiling cube); Galacticraft Arc Lamp (a directional light block, not a cube).

## Planet stone (D244)

On Moon and Mars, a rugged group (the hub, storage, power, workshop, guard, halls, gallery, vehicle bay, missile assembly, water treatment) draws, per room, a one-in-three chance each of: local brick as the wall, a local-stone field inside a border (the room's wall block), a local stone and brick checker for the floor. Moon: `GalacticraftCore:tile.moonBlock` meta 4 (Moon Rock) and 14 (Moon Dungeon Brick). Mars: `GalacticraftMars:tile.mars` meta 9 (Mars Stone) and 7 (Dungeon Brick). Each falls back to vanilla stone and stone brick. Command, crew, medical, server and cafeteria rooms keep their finish.

## Block table

Worth is the value of mining the block in bulk. **Low** means it drops itself or a cobble, costs ordinary stone, steel-free ingredients or less, and gives no GT material. No block here is a GT casing, a machine, a frame, a metal block or an ore.

| Block (registry id) | Metas used | Look | Use | Worth |
|---|---|---|---|---|
| `chisel:laboratoryblock` | 0 wall panel, 1 dotted panel, 2 large wall, 3 roundel, 5 large tile, 6 small tile, 7 floor tile, 9 clear screen, 10 fuzz screen, 11 large steel, 12 small steel, 15 info console | White and steel-gray lab panels | wall, floor, ceiling, dead lamp, console | Low (carves from any stone) |
| `chisel:factoryblock` | 0 dots, 3 platex, 6 hazard, 7 hazard orange, 9 metal box, 13 plating, 15 column | Industrial gray plates, hazard stripes | wall, floor, ceiling, accent | Low |
| `chisel:tyrian` | 3 soft plate, 8 plate tiles, 11 blue plating, 12 black, 13 black 2, 15 plate | Dark sci-fi plate | wall, floor, ceiling | Low |
| `chisel:concrete` | 0 default, 1 block, 3 blocks, 4 weathered, 5 weathered block, 10 asphalt | Grey and worn concrete | wall, floor, ceiling, accent, worn | Low |
| `chisel:technical` | 3 large pipes, 5 small pipes, 7 vent, 9 insulation, 11 cables, 12 rusty plates, 13 grate, 15 rusty grate | Pipes, vents, grates | wall, ceiling, floor rows, accent | Low |
| `chisel:marble`, `chisel:limestone` | marble 0, 1, 4, 5, 8, 9, 12; limestone 0, 1, 5, 11, 15 | Pale stone, panels, brick | wall, floor, ceiling | Low |
| `chisel:glowstone` | 4, 5, 8, 10, 13, 14, 15 | Lit tile, neon, panel | lit lamp | Low (a Chisel variant, not 4 dust) |
| `chisel:bookshelf` | 0 | Bookcase | shelf | Low |
| `gregtech:gt.blockconcretes` | 3, 6, 7, 8, 11, 12, 13, 14, 15 | Dark and light concrete, bricks, smooth | GT fallback for walls and floors | Low (breaks into its cobble) |
| `gregtech:gt.blockstones` | 3 marble bricks, 11 basalt bricks | Marble and basalt | GT fallback | Low |
| `gregtech:gt.blockgranites` | 3, 6, 7 black; 11, 15 red | Granite brick, chiseled, smooth | `granite_hall` | Low |
| `Ztones:tile.ztones:vectBlock` (also `vectBlock`) | 0 to 3 | Dark panels | `ztones_server` | Low (decompiled ids) |
| `Ztones:tile.ztones:korpBlock` | 4, 12 | Dark, cyan screen | server accent and console | Low |
| `Ztones:tile.ztones:tankBlock` | 0, 1, 3, 5 | Grey plating | `ztones_tank` | Low |
| `Ztones:tile.ztones:laveBlock` | 0, 1 | Pale blue tile | `ztones_tank` | Low |
| `Ztones:tile.ztones:agonBlock` | 11 red, 13 gray | Solid color | crew accent rows | Low |
| `GalacticraftCore:tile.moonBlock` | 4, 14 | Moon Rock, Moon Dungeon Brick | local stone | Low |
| `GalacticraftMars:tile.mars` | 7, 9 | Mars Dungeon Brick, Stone | local stone | Low (never meta 8, Block of Desh) |
| `gregguns:complex_wall` | 0 | Steel Bastion Plating look | one wall choice (D205) | None (breaks into itself, no recipe) |
| vanilla: `stone`, `stonebrick` 0 2 3, `quartz_block` 0, `glowstone`, `sandstone` 2, `stained_hardened_clay` 7 14 15, `wool` 0 7 11, `wooden_slab` 1, `stone_slab` 7, `bookshelf` | as listed | Fallbacks and furniture | fallbacks, furniture | Low (glowstone gives 2 to 4 dust per block) |

Not used, on purpose: GT machine casings, GT frame boxes, GT metal and storage blocks, GT sheet-metal blocks (they cost plates), Chisel's metal-block carvings (the `chisel:*` blocks of iron, steel and the like, which carve from metal blocks), Galacticraft tin and aluminum decoration blocks (they pay in tin and aluminum), Desh blocks. BiblioCraft furniture is a tile entity per piece (and all rights reserved), so tables, shelves and bunks are plain cubes and slabs instead. Carpenter's Blocks are tile entities too.

The Ztones ids come from decompiling `ModBlocks.registerBlocks` (Ztones 2.2.2 has no public source): lamps and tiles register under their plain name, the metal sets under the block's unlocalized name `tile.ztones:<set>Block`. Both spellings are chained, and a wrong guess only means the fallback (a Chisel or GT block) is drawn. `icbm.palette.resolve` logs which chain entries the live pack fails to find.

## Tests

- `ComplexPaletteTest` (unit): with no pack mods every style resolves to a vanilla block; every registry name and meta is well formed; walls are uniform and styles are deterministic per seed; every group has at least two sets with at least four walls and four floor designs; the Silo Complex Wall is a wall choice; planets use local stone and other dimensions do not; lamps have a lit and a dead twin.
- `icbm.palette.resolve` (scenario): resolves every style in the live registry, checks lit lamps emit 15, dead lamps emit less than 15 and every block other than a lamp is a cube, and notes which mod supplied each block.
