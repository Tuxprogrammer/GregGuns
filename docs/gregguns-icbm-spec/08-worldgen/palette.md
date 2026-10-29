# Complex palette: blocks, style sets and lamps

- **Baseline:** none (the ICBM baseline has no worldgen).
- **Decisions:** D229 (the redesign), D205 (the complex wall), D217 (floor light), D240 to D244, D248, D251, D259 to D267
- **Pack mods:** Chisel, GregTech, Ztones, Galacticraft (Core and Mars), BiblioCraft (furniture); every one optional (D241, D262)
- **Code:** `com.gregguns.icbm.complex.palette` (`ComplexPalette`, `RoomStyle`, `BlockRef`, `PaletteSets`); the role audit in `com.gregguns.icbm.complex.gen` (`RoleAudit`, `RoleAuditScenarios`)

## What it does

Every room, hall and the hub gets a `RoomStyle` from `ComplexPalette.styleFor(kind, roomSeed, dimensionKey, level)`. The call is pure and deterministic. A style is:

- **one wall block**, the same everywhere in the room, drawn once from the style set's wall choices;
- a **floor pattern** and a **ceiling pattern**, each a function of the interior cell `(x, z)` and the interior size (checker, border plus field, double ring, stripes, bands, grid lines, center strip). Interior cell 0 is the first cell inside the section's wall, so a border of width 1 runs along the wall;
- a **lamp pair**, lit and dead;
- an optional **accent** block and **furniture** blocks (table, console, bunk, shelf), all plain cubes or slabs, never tile entities. They are the small rooms' cube furniture, and what a room shows of a BiblioCraft piece when BiblioCraft is absent (see [Furniture](#furniture-d259-d262-d263)).

### Three blocks per room (D265)

Within a room, a hall or the hub, **the floor, the wall and the ceiling are three different blocks**: different registry names, not metas of one block. Lamps, light tiles, door frames, accents, furniture, machines, loot and spawners are exempt.

- **By construction.** Each role of a style set has its own block at every level of its chains: its own Chisel block, its own GregTech block and its own vanilla stand-in. Walls fall back to GT concrete, then stone bricks (quartz or smooth sandstone in the pale sets); floors to GT marble or basalt, then smooth double slabs; ceilings to quartz or smooth sandstone, with no GT step. No mix of present and missing mods can make two roles meet.
- **GregTech's stone layers.** GT's stone generator overwrites plain stone, stained clay, cobblestone and its own granite and marble/basalt wherever a blob passes, after the complex is written. A blob through two roles that both name such blocks would leave them sharing the blob's block, so at most one role of a set names any of them. `granite_hall` keeps its GT granite walls over GT concrete floors.
- **The guard.** `RoomStyle.roles(lookup)` resolves a room against a registry and takes, per role, the first design whose blocks are free: the drawn design, the set's other designs, then plain vanilla stand-ins (three floors, six ceilings, the ones GT's stone layers leave alone first). It never fires for these sets: the tests and `icbm.palette.resolve` check every room keeps its drawn designs. It keeps the rule for any future set or pack. `RoomStyle.floor` and `RoomStyle.ceiling` apply it on the server.
- **Without a palette** (scenarios and tests that pass no styles) the shell writes a smooth slab floor and a quartz ceiling under the complex wall.

Each block is a `BlockRef`: a chain of registry ids, pack block first, GregTech next, vanilla last. `PaletteResolver` walks the chain with `GameRegistry.findBlock` on first use on the server and caches the result; nothing resolves at class load. With no pack mod and no GregTech loaded, every chain ends on a vanilla block, so the dev environment still generates.

## Style sets (12)

| Set | Look | Groups it dresses |
|---|---|---|
| `lab_white` | Chisel Laboratory wall panels and its dark gray steel (walls), light Nucrete tiles (floor), Marble panels (ceiling) | medical, server room, command, cafeteria |
| `lab_dark` | dark Nucrete panels (walls), the Laboratory two-tone checker tile and floor tiles (floor, D260), Marble panels (ceiling) | medical, server room, command |
| `factory_tile` | Chisel Factory plating and metal box (walls), Chisel stone panels (floor, weathered concrete when abandoned), Technical insulation and vents (ceiling) | power, workshop, vehicle bay, missile assembly, storage, halls, gallery |
| `tyrian_dark` | Chisel Tyrian dark plate (walls), dark Nucrete tiles (floor), Technical cinder panels (ceiling) | command, hub, gallery, major hall, guard, server room |
| `concrete_bunker` | Silo Complex Wall (D205) or Chisel concrete (walls), Chisel Andesite tiles (floor, weathered when abandoned), light Nucrete (ceiling) | storage, guard, minor hall, crew, gallery, vehicle bay |
| `technical_works` | Chisel Technical insulation, rusty plates, pipes, vents (walls), Factory plating (floor), dark Nucrete (ceiling) | power, water treatment, vehicle bay, workshop, storage, minor hall |
| `marble_admin` | Chisel Marble panels (walls), polished Andesite (floor), Limestone (ceiling) | command, cafeteria, medical, hub |
| `granite_hall` | GT black and red granite (walls), GT dark concrete bricks and smooth (floor), Technical cinder panels (ceiling) | major hall, hub, gallery, command |
| `ztones_server` | Ztones Vect dark panels (walls), Ztones Tank plating (floor), Ztones Korp dark (ceiling), a cyan Korp screen accent | server room, command, guard |
| `ztones_tank` | Ztones Tank plating (walls), Ztones Lave pale tile (floor), Ztones Vect dark (ceiling) | water treatment, power, missile assembly, workshop |
| `crew_warm` | Chisel Limestone (walls), light Nucrete with Ztones Agon tan and gray rows (floor), Marble (ceiling) | crew, cafeteria, medical |
| `steel_panel` | the Laboratory's large and small steel and Tyrian plate (walls), dark Nucrete (floor), Technical insulation and vents (ceiling) | hub, major hall, gallery, missile assembly, minor hall |

The Laboratory's checker tile and its steel walls are metas of one block (`chisel:laboratoryblock`), so they never share a room (D265): the checker floor dresses `lab_dark` rooms, the steel walls `lab_white` and `steel_panel` rooms.

A group with several sets gets variety from the set draw, then from the wall, floor and ceiling draws inside it. Every group has at least two sets; the wall pool of a group has at least four blocks (a test checks it).

## Lamps (D242)

Lit: Chisel light stone (`chisel:glowstone`, light 15, an opaque cube, metas 4, 5, 8, 10, 13, 14, 15 for the tile, neon, panel and bismuth looks), else vanilla glowstone. Dead: a dark panel from the style's own family (Laboratory clear screen, Factory dots, Tyrian black, concrete weathered, Technical vent), else its vanilla fallback. A lamp is a full cube and works in the ceiling plane and as a floor light tile. The writer picks lit or dead from the activity level and the lamp grid; the palette only supplies the pair.

A lit lamp drops 2 to 4 glowstone dust when mined, as vanilla glowstone does: Chisel's light stone drops the dust, not itself. A complex holds about 3,000 lamp marks (ceiling lamps and floor tiles), so an operational one, every lamp lit, yields about 9,000 dust, and a partly abandoned one about 4,000. A complex is a loot site and glowstone is cheap in GTNH, so the yield stands (D248). `ComplexContentCountsTest` prints the lamp count per complex and holds it between 2,000 and 4,500.

Rejected as lamps: Project Red lamps (a tile entity per lamp, and the light comes from a per-world cache the tile feeds, so tens of thousands of them would start a tile tick each and need a power source); vanilla Redstone Lamp (a lit one turns itself off on the first neighbor update with no power); Ztones Flat Lamp (a thin, face-attached block, not a ceiling cube); Galacticraft Arc Lamp (a directional light block, not a cube).

## Planet stone (D244)

On Moon and Mars, a rugged group (storage, power, workshop, guard, halls, gallery, vehicle bay, missile assembly, water treatment) draws, per room, a one-in-three chance of local brick as the wall (falling back to the set's own wall chain), and otherwise a two-in-three chance of a local floor: a local-stone field inside a border of the set's own floor design, or a local stone and brick checker that falls back to the floor design's blocks. Never both: the planet's stone and brick are metas of one block (D266). Moon: `GalacticraftCore:tile.moonBlock` meta 4 (Moon Rock) and 14 (Moon Dungeon Brick). Mars: `GalacticraftMars:tile.mars` meta 9 (Mars Stone) and 4 (Cobblestone, which stands in as the Mars brick: Galacticraft's Mars Dungeon Brick, meta 7, is green and mossy, too loud beside the red stone). Command, crew, medical, server and cafeteria rooms keep their finish.

## Block table

Worth is the value of mining the block in bulk. **Low** means it drops itself or a cobble, costs ordinary stone, steel-free ingredients or less, and gives no GT material. No block here is a GT casing, a machine, a frame, a metal block or an ore.

| Block (registry id) | Metas used | Look | Use | Worth |
|---|---|---|---|---|
| `chisel:laboratoryblock` | 0 wall panel, 2 large wall, 3 roundel, 5 large tile, 6 small tile, 7 floor tile, 8 checker tile (two-tone), 9 clear screen, 10 fuzz screen, 11 large steel, 12 small steel, 15 info console | White and steel-gray lab panels | wall (lab_white, steel_panel), floor (lab_dark), dead lamp, console, accent | Low (carves from any stone) |
| `chisel:nucrete` | dark 1, 2, 3, 4, 5, 7; light 8, 9, 11, 12, 13, 15 | Dark and light concrete panels, tiles, strips | wall (lab_dark), floor, ceiling | Low |
| `chisel:andesite` | 1, 3, 4, 6, 10, 11 (weathered) | Polished, brick and tiled andesite | floor, worn | Low |
| `chisel:stonebricksmooth` | 4, 5, 12, 13 | Small and large bricks, hard and sunken panels | floor (factory_tile) | Low |
| `chisel:technical4` | 2 sturdy, 9 polished cinderblock | Gray panels | ceiling | Low |
| `chisel:factoryblock` | 0 dots, 3 platex, 6 hazard, 9 metal box, 13 plating | Industrial gray plates, hazard stripes | wall, floor (technical_works), dead lamp, accent | Low |
| `chisel:tyrian` | 6 gray plate, 8 plate tiles, 11 blue plating, 12 black, 13 black 2, 15 plate | Dark sci-fi plate | wall, dead lamp, accent | Low |
| `chisel:concrete` | 1 block, 3 blocks, 4 weathered, 5 weathered block, 10 asphalt | Gray and worn concrete | wall, worn floor patch, dead lamp, accent | Low |
| `chisel:technical` | 3 large pipes, 5 small pipes, 7 vent, 9 insulation, 12 rusty plates | Pipes, vents, insulation | wall (technical_works), ceiling, accent | Low |
| `chisel:marble`, `chisel:limestone` | marble 0, 1, 4, 5, 7, 8, 12; limestone 0, 1, 5, 11, 15 | Pale stone, panels, brick | wall, ceiling | Low |
| `chisel:glowstone` | 4, 5, 8, 10, 13, 14, 15 | Lit tile, neon, panel | lit lamp | Low per block, but drops 2 to 4 glowstone dust like vanilla glowstone: about 9,000 dust from an operational complex (D248) |
| `chisel:bookshelf` | 5 (abandoned) | Bookcase | shelf | Low (drops 3 books, like vanilla). Chisel's own bookshelf block has metas 1 to 8; meta 0 of the group is the vanilla bookshelf, so meta 0 of this block draws the missing texture |
| `gregtech:gt.blockconcretes` | 3, 6, 7, 8, 11, 12, 13, 15 | Dark and light concrete, bricks, smooth | GT fallback for walls (and floors where walls are GT stone); `granite_hall` floors | Low (breaks into its cobble) |
| `gregtech:gt.blockstones` | 3, 7 marble; 11, 15 basalt | Marble and basalt bricks, smooth | GT fallback for floors (and walls where floors are GT concrete) | Low. GT's stone layers overwrite it, so it is in one role per set |
| `gregtech:gt.blockgranites` | 3, 6, 7 black; 11, 15 red | Granite brick, chiseled, smooth | `granite_hall` walls | Low, but slow to dig: harvest level 3 (a diamond-level pick; steel is not enough) and blast resistance 60 |
| `Ztones:tile.vectBlock` | 0 to 3 | Dark panels | `ztones_server` | Low (decompiled ids) |
| `Ztones:tile.korpBlock` | 4, 12 | Dark, cyan screen | server accent and console | Low |
| `Ztones:tile.tankBlock` | 0, 1, 3, 5 | Grey plating | `ztones_tank` | Low |
| `Ztones:tile.laveBlock` | 0, 1 | Pale blue-gray tile | `ztones_tank` | Low |
| `Ztones:tile.agonBlock` | 12 tan, 13 dark gray | Solid color | crew accent rows | Low |
| `GalacticraftCore:tile.moonBlock` | 4, 14 | Moon Rock, Moon Dungeon Brick | local stone | Low |
| `GalacticraftMars:tile.mars` | 4, 9 | Mars Cobblestone, Stone | local stone | Low (never meta 8, Block of Desh) |
| `gregguns:complex_wall` | 0 | Steel Bastion Plating look | one wall choice (D205) | None (breaks into itself, no recipe) |
| vanilla: `stone`, `stonebrick` 0 2 3, `double_stone_slab` 0, `quartz_block` 0, `glowstone`, `sandstone` 2, `stained_hardened_clay` 7 12 15, `wool` 0 7 8, `wooden_slab` 1, `stone_slab` 7, `bookshelf` | as listed | Fallbacks and furniture | fallbacks (walls stone bricks, quartz or smooth sandstone; floors smooth double slab or clay; ceilings quartz or smooth sandstone), furniture | Low (glowstone gives 2 to 4 dust per block) |

Not used, on purpose: GT machine casings, GT frame boxes, GT metal and storage blocks, GT sheet-metal blocks (they cost plates), Chisel steel and metal blocks (they carve from metal blocks), Galacticraft tin and aluminum decoration blocks (they pay in tin and aluminum), Desh blocks. Carpenter's Blocks are tile entities per block and are not used.

## Furniture (D259, D262, D263)

The small rooms (see [silo-layout](silo-layout.md#furnishing-d208-d234-d259)) get real furniture from BiblioCraft 1.11.7 when it is loaded, and the cube furniture above when it is not. Each piece is a tile entity, so only the small rooms hold them: about 17 rooms of a complex, and about 535 tile-entity pieces (BiblioCraft and lockers) over 300 plans.

| Piece | BiblioCraft block (registry name) | Meta | Facing (tile NBT) | Without BiblioCraft |
|---|---|---|---|---|
| Chair, stool | `BiblioSeats` | wood | `Angle`; `hasBack` 1 for a chair, 0 for a stool; `backWoodType` | a wooden stair, its back away from the table |
| Table | `BiblioTable` | wood | none; a lone table's `leg1`..`leg4` off, `top1`..`top4` and `monoleg` on (neighbors join when a player builds next to it) | the style's table |
| Desk | `BiblioDesk` | wood | `deskAngle` | the style's table |
| Typewriter | `BiblioTypewriter` | 3 (black) | `angle` | none |
| Clipboard | `BiblioClipboard` | 0 | `angle` | none |
| Bookcase | `Bibliotheca` | wood | `bookcaseAngle` | the style's shelf |
| Medicine cabinet | `BiblioPotionShelf` | wood | `potionshelfAngle` | the style's shelf |
| Tool rack | `BiblioRack` | wood | `rackAngle` | the style's shelf |
| Armor stand | `Armor Stand` | angle below, angle + 4 above | the meta | none |
| Locker | GregTech's ULV Locker (`gregtech:gt.blockmachines`, the `Locker_ULV` item), facing into the room (D263) | | GT front facing | the GT Locker (the style's shelf only without a world, or when the pack has no Locker) |
| Server rack | the style's console block, two high | | | the same |

- **Wood:** one per room, spruce, dark oak, birch or oak, from the plan seed and the room id.
- **Facing:** BiblioCraft turns a piece to face the player who places it, storing `(playerFacing + 1) % 4` (player facing 0 south, 1 west, 2 north, 3 east). A piece whose front faces `f` gets `(f + 3) % 4`. The real-pack renders confirm it (a typewriter set at each of the four angles shows its keys west, north, east and south), and that chairs face their tables, desks and typewriters face their chairs, and shelves, bookcases, cabinets, clipboards and lockers face into the room.
- **How it is placed:** `FurnitureFiller` (the complex's mark filler) sets the block through the window's sink by registry name (`GameRegistry.findBlock`, guarded by `Loader.isModLoaded("BiblioCraft")`), then writes the facing into the tile's own NBT (`writeToNBT`, set the keys, `readFromNBT`). No BiblioCraft class is linked and none of its assets ship (D262).
- **Worth:** BiblioCraft furniture is wood and drops itself; a GT ULV Locker is a cheap machine. Nothing here is a material sink worth farming.

The Ztones ids come from the jar (Ztones 2.2.2 has no public source): each metal set block is named `<set>Block` and registers under its unlocalized name, so its id is `Ztones:tile.<set>Block`, as the live pack's FML registry lists it. Each set has 16 metas; the ones used were checked against the jar's textures (`sets/<set>/<set>_ (n).png`). `icbm.palette.resolve` fails when any chain entry of a loaded mod is missing, so a wrong id cannot fall back silently (D272).

**A quiet palette (D272).** Every vanilla stand-in at the end of a chain is a muted block: stone, stone bricks, smooth double slabs, quartz, smooth sandstone, gray, brown or black clay, white or gray wool (`PaletteLookTest` pins the list). Saturated color is kept to deliberate small accents from the pack blocks (hazard stripes, the Tyrian blue strip, red granite, a cyan screen). Every set has a dark console screen, so a console never borrows the accent.

## The role audit (D267)

`RoleAudit` checks the rule in the world. The writer tags every write with the pass that made it (shell, marks, contents, signs, shafts). When a complex's last window is written, the audit reads the floor, wall and ceiling cells back from the world by raster role, section by section, and checks the three sets share no block. A wall of another section that faces into a section counts as that section's wall. Only cells the shell wrote last count, so lamps and light tiles, door frames, furniture, loot, machines, spawners and every damage write (holes, rubble, collapses, floods) are left out by the path that wrote them, never by block name; air is left out too. So is a cell whose block another writer changed after the complex was written: the audit compares the world with the block the last tagged write placed. In the real pack those are GT's stone blobs and the odd cobblestone from vanilla worldgen (a dungeon, lava meeting water); they are counted by block and noted, not failed, since the palette cannot prevent them (GT's stone layers it can, see [Three blocks per room](#three-blocks-per-room-d265)).

A violation is one WARN line per section: where (dimension, cell, anchor, floor), the room and its kind, the style set, the activity level, the shared block, its roles, and per role one cell and the path that wrote it: the design and where it came from (drawn, guard alternate, planet stone, worn), the chain entry the registry resolved and whether it is a fallback.

It is on in the dev environment and with the test hooks (`-Dgregguns.testHooks=true`), so in every scenario run, and with `-Dgregguns.icbm.roleAudit=true`, which also logs one INFO line per complex with its cell counts by last writer and the cells other worldgen changed. In normal play it is off and costs one boolean test per populate window.

## Tests

- `RoleBlocksTest` (unit, D265): no block appears in two roles of a set at any level of any chain, and at most one role names a block GT's stone layers overwrite; over every style group, five dimensions, every activity level, 64 seeds, three room sizes and all 64 mixes of the six mods a chain names, every room keeps its drawn designs and its floor, wall and ceiling are three different blocks, counting every block a patterned floor or ceiling uses; the guard steps past a design that shares a block; the Laboratory checker floor and steel walls stay in use.
- `RoleAuditTest` (unit, D267): the audit's core over whole generated complexes (12 plans, four dimensions, three levels, full pack, vanilla only, GT without Chisel) finds nothing; one block in every role is one line per section naming its roles and write paths; cells another pass wrote are left out.
- `PaletteLookTest` (unit, D272): Ztones ids have the `tile.<set>Block` form, every vanilla stand-in is on the muted list, the Mars brick is its cobblestone.
- `BiblioFurnitureTest` (unit): every BiblioCraft piece has a registry name, the facing turns all four ways, each piece writes its facing key, and a stair chair's back is opposite its facing.
- `ComplexPaletteTest` (unit): with no pack mods every style resolves to a vanilla block; every registry name and meta is well formed; walls are uniform and styles are deterministic per seed; every group has at least two sets with at least four walls and four floor designs; the Silo Complex Wall is a wall choice; planets use local stone and other dimensions do not; lamps have a lit and a dead twin.
- `icbm.complex.*` (scenarios, D267): each fails on a role audit violation and notes the complexes and cells it read back; `icbm.complex.role_audit_probe` writes a one-block complex and checks the audit reports every section.
- `icbm.palette.resolve` (scenario): resolves every block of every style set in the live registry (walls, floors, ceilings, lamps, the accent, the worn block and the furniture), fails when a chain entry of a loaded mod is missing, checks lit lamps emit 15, dead lamps do not, building blocks are cubes (furniture may be a slab), and that a Chisel block has a variation at its meta (`ICarvable.getManager(meta)` is not null), and notes which mod supplied each block. It also fails when a set shares a block between roles in the live registry, or when any of 12,288 rooms (every group, four dimensions, every level, 64 seeds) needs the guard or shows a block, read from the resolved blocks, in two roles.
