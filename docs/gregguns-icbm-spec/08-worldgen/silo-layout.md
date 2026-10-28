# Silo complex layout (D128, D229)

Status: grown layout, gallery, lamp grid, furnishing, palette integration, surface features built (I6b layout). Placement and planets: [planets-and-placement](planets-and-placement.md). Blocks and styles: [palette](palette.md).

## Shape

All coordinates are complex-local: x and z in [0, 240), y 0 at the level floor. It is one flat underground level: every section's floor is layer 0, and sections differ only in how high their ceiling is.

The layout is grown, not stamped (`ComplexGrower`, D230). Every choice comes from one `Random` seeded by the plan seed. Lists are used throughout, with no hash order, so a plan is a pure function of (seed, cell).

| Section | Footprint (walls included) | Height (floor to ceiling layer) |
|---|---|---|
| Hub (the assembly bay) | 51 x 40, either way round, near the center | 12 |
| Major hall | 20 wide | 11; 12 for the silo gallery |
| Minor hallway | 5 wide | 7 |
| Room | about 22 x 20 to 36 x 30 nominal, then grown (below) | 9 |
| Guard post, decontamination airlock | 10 x 10, 12 x 12 | 7 |

- **Major halls:** three (25%) or four (75%) leave the hub's sides at a seeded offset.
  - The first hall is straight and 50 to 62 long; it carries the silo gallery. The strips a silo deep along both its sides stay free while the other halls grow, so no bend or side branch takes them (D245).
  - The others bend at their far end (85%), and some bend again (40%), never back toward the hub.
  - Up to three side branches leave the middle of a long segment.
  - A hall stops 20 blocks short of the edge, so rooms can cap it. A free far end grows toward the edge.
- **Minor hallways:** up to 15 hallways, 30 to 70 long, leave a hall's side. Over 10,000 seeds a plan has 0 to 15, median 5 (mean 5.5), and 3 to 8 in three plans in four (D246). Some leave another minor hallway's side as sub-hallways between branches. Their sides and far end take rooms.
- **Rooms:** 35 to 40 rooms in all, the hub not counted.
  - Every mandatory kind is always present: launch control, comms, commander's office, warhead magazine, generator hall, armory and missile assembly. The rest come from the optional kinds, repeated as needed.
  - Rooms hang off halls side by side, each touching the last.
  - Rooms also hang off rooms, up to three deep, so not every room has a direct line to the hub.
  - About one in seven pairs of rooms that end up side by side gets an extra door, so the plan is a graph, not just a tree.
- **Growth to coverage:** every room then grows a cell at a time into the rock around it.
  - The limit is 150% of its kind's nominal size per axis. A side never grows past twice the other.
  - While the built footprint is under 78%, the limit rises 20 points, up to six times.
  - A plan that still misses 76%, 35 rooms, 4 silos or 3 guard posts is grown again from a seed mixed from the plan seed and the attempt number (up to eight attempts), and the best attempt is kept.
  - Over 1000 seeds: coverage is at least 76% (mean 78.5%). The mean room footprint is about 1000 cells, six times the I6 room's 168: 75% coverage with 35 to 40 rooms needs that.
- **Walls:** neighboring sections touch but never share a wall. A wall between two rooms is two blocks thick, so each side shows its own room's one wall block (D229 point 5).

## Silo gallery (D232)

- **Count:** 4 to 10 silos, rolled at 4 (26%), 5 (24%), 6 (18%), 7 (12%), 8 (10%), 9 (6%) and 10 (4%); over 1000 seeds 4 to 6 come out in 77% of plans (D232). Each misses exactly one part (D128).
- **Where:** the silos line the gallery hall on both sides.
  - They stand in facing pairs, 7 blocks apart (5 of silo, 2 of rock), from the far end toward the hub. The last 14 blocks at the hub end stay clear.
  - Seven or more silos split across a second straight hall when one exists (a hall no other major hall leaves), never more than two halls. The first hall takes the larger half, and whatever the second cannot hold goes back to the first, which has room for ten. Every rolled silo is placed (D245).
  - A hall takes single silos only after it has a facing pair, so a gallery always has silos on both sides. Only a hall that holds silos is a gallery (12 high, its sign and palette).
- **Turned to face the hall:** each silo's structure is stamped turned so its controller face is toward the gallery (`SiloSlot.facing`, `x(column, aisle)`, `z(column, aisle)`). The controller and hatches are placed facing that way. A 3-wide walkway niche opens in the gallery wall in front of the controller.
- **Launch Control** caps the gallery's far end. Its doorway opens onto the gallery, and both wall layers on either side of the doorway are clear glass blocks from layer 3 to 6, so the room overlooks the silos. If the far end is taken, Launch Control opens off the gallery's side.
- **Missile assembly floor** is the first room placed on the gallery's sides, in the span the silos leave free.

## Doors

`ComplexPlan.doors()`: each `Door` cuts both wall layers between two touching sections.

| Kind | Size | Where |
|---|---|---|
| Portal | The whole shared interior span; three under the lower ceiling (layer 8 or 9) between tall halls (a lintel above), the full height into a minor hallway | Hub to hall, hall to hall, hall to minor hallway |
| Door | 3 wide, up to layer 4 (vehicle bay off a hall: 5 wide, up to layer 5; small rooms: 2 wide, up to layer 3) | Room to its parent; extra room-to-room doors |
| Walkway | 3 wide, up to layer 3, the gallery wall only | In front of each silo's controller |
| Window | Clear glass blocks, layers 3 to 6 | Launch Control over the gallery |

Doors and portals get **door frames** (the DOOR role): jambs either side and a lintel over the opening, in both wall layers, drawn in the room's accent block.

## Raster (cell roles)

`ComplexRaster` holds one role per cell (`Cell`) and, per column, the section that owns it.

- Each section writes its shell: WALL on its perimeter at every layer, FLOOR in layer 0 and CEILING in its top layer inside, air between.
- Then doors (air), door frames, window panes, lamps, furniture and rails.
- The silo roles stay as they were (concrete, glass, beam, pad, controller, hatches).

The writer resolves the building roles through the owning section's palette style: WALL, FLOOR, CEILING, LAMP, DOOR_FRAME and furniture. Glass and water resolve by themselves. See [palette](palette.md) and D235.

## Lighting (D230, D231)

- **Lamps are flush in the ceiling layer**, on a grid aligned to their section.
  - Lines run at most 6 apart, spread evenly so the grid is centered (`ComplexFurnisher.grid`).
  - In halls the rows run along the hall's axis. In rooms they run along the door wall.
  - Rooms furnished with rows of tables, shelves or racks (cafeteria, mess galley, server room, stockpile) use lines at most 4 apart.
- **Light tiles in the floor:** a section taller than 7 also gets a lamp flush in the floor layer under every ceiling lamp. Minecraft's block light falls one level per block, so a ceiling lamp 8 or more blocks up cannot light the floor to 8. A floor light tile lights every floor cell within 6 to 8 or more. A minor hallway (7 high) has ceiling lamps only, every 3 cells along its centerline, which keeps D217's floor light of 8.
- **Light marks:** a `LIGHT` mark sits at each lamp (ceiling layer or floor layer), with the key `lamp:<section>:<run>`. A run is one grid line. The shell writes the style's dark lamp; the contents pass lights it with the style's lit lamp.
- **Activity level:** an abandoned complex lights or kills whole runs, a partly abandoned one whole sections, an operational one everything. The D36 shares stay: 5%, 45% and 100%.
- **Proof:**
  - `ComplexLayoutTest` simulates block light over 12 plans, with machines, guards and furniture counted solid. Every open floor cell reads 8 or more.
  - `icbm.complex.levels` reads the saved block light of every open floor cell of every section in an operational complex.

## Marks (the seam with activity, guards and loot)

`ComplexLayout.generate(worldSeed, cellX, cellZ)` returns a `ComplexPlan` of `Room`s (the hub is `hall()`), `Tunnel`s (major and minor halls), `SiloSlot`s and `Door`s. Each room and hall carries `Mark`s (`MarkKind`, x, y, z, key):

- loot chest, with a loot-table key from `LootKeys`;
- spawner, keyed `post`, `room` or `deep`;
- machine, keyed by `MachineRole`;
- item-frame wall, light, damage cell (`wall`, `floor`, `roof`), flood cell, rubble, sign and blast door;
- furniture (D234).

The plan is pure geometry and deterministic. It never changes by activity level.

## Furnishing (D208, D234, D259)

- **The frame:** every room is furnished in its own frame (`ComplexLayout.Frame`), not in world axes.
  - The back wall is the wall opposite the doorway to the room's parent, whichever of the four sides that is.
  - Chests and wall machines stand against the back wall; machines stand in the middle row.
  - The sign hangs on the doorway's jamb, facing in.
- **Kept clear:** the cells inside every doorway, two deep and one wider each side, any door of the room. Light tiles are kept clear too. The NASA Workbench needs a 5 x 5 block: its 3 x 3 core strictly free (no light tile under Galacticraft's fake blocks), its outer ring free from layer 1 up (a floor light tile may lie in it). It takes the fitting center nearest the wanted cell (D252). If furniture walls a chest, machine or guard off from the doorway, the shortest way through the furniture is cleared.
- **Scaling:** chest, machine and guard counts grow by one step per 450 interior cells: a room of `q = max(1, interior / 450)` steps gets `q` room guards, `q` back-row chests and `q` repeated machines (D252).
- **Small rooms only (D259):** furniture goes only into rooms that are not bays and whose interior is at most 1,200 cells (`RoomFurniture.MAX_AREA`, about 35 x 35).
  - The bays never get furniture: the hub, workshop, pump room, generator hall, vehicle bay, missile assembly, air handling, water treatment and fuel depot, and the halls. Their machines, chests, guards and basins stay.
  - The cutoff comes from the real room areas: over 300 plans the other kinds run from 64 cells (a guard post) to about 3,500, medians 580 to 930, 90th percentile 1,330 to 2,090. 83% of those rooms are at or under 1,200; the rest read as bays and stay empty. About 19 rooms of a complex are furnished.
- **Furniture:** `FURNITURE` marks, one per layer, with the piece's facing (`Mark.facing`). The key is a style cube (TABLE, CONSOLE, BUNK, SHELF) or a `FurniturePiece` as `NAME:wood` (CHAIR, STOOL, WOOD_TABLE, DESK, TYPEWRITER, CLIPBOARD, BOOKCASE, MEDICINE_SHELF, TOOL_RACK, ARMOR_STAND and its top, LOCKER, SERVER_RACK). The raster writes the piece's style cube, or leaves its cell open; `FurnitureFiller` then sets the BiblioCraft piece, the GT Locker or a stair chair over it ([palette](palette.md#furniture-d259-d262-d263)). `BASIN` is standing water sunk into the floor layer.
- **Placed by `RoomFurniture`:** after the room's chests, machines and guards, only on free cells (never a doorway zone, a light tile or a mark). Chairs face their table, desk or console; a chair whose table the reachability pass cleared is dropped. Typewriters, clipboards and an armor stand's top sit on the piece below.
- **Wear (D36):** an operational complex keeps all its furniture. A partly abandoned one loses 15% of the pieces and an abandoned one 35%, whole pieces at a time. A room that caved in has its furniture crushed to rubble (`FurnitureWear`).

| Kind | Furnishing |
|---|---|
| Launch control | Launch Control machine in the middle; a console row facing the windows with a chair behind each console; control chests; guards |
| Radar and tracking, comms | The radar or comms machine in the middle; consoles along the side walls (both for radar, one for comms) with a chair at each and a clipboard over every other one; comms adds a desk with a typewriter on the other side wall; chests on the back wall |
| Commander's office | A safe and a stockpile chest in the back corners; a two-cell desk facing the door with a typewriter, the commander's chair behind it and two visitors' chairs in front; two-high bookcases along a side wall; an armor stand |
| Warhead magazine | A grid of inert Stainless Steel frame boxes 3 apart; fallout rubble marks; magazine chests; guards |
| Fuel depot | Fuel tanks along both side walls; fuel chests |
| Generator hall | A row of generators; a battery buffer in a back corner; guards |
| Air handling, pump room | Their machines in the middle row; flood cells (pump) |
| Workshop | The NASA Workbench with its free ring, near the back-left corner; workbenches; a workshop chest |
| Bunks | Bunks along the side walls with a locker in each gap; a table with chairs in the middle; chests; guards |
| Infirmary | Beds along the side walls, a clipboard chart over each; two-high medicine cabinets on the back wall; a desk and chair in the middle; medical chests |
| Stockpile | Chests along the back wall; rows of two-high shelves |
| Mess galley, cafeteria | A serving counter; rows of tables with a chair on each side, runs of 3 tables with 2-cell gaps, a 3-cell aisle between rows and 3 clear cells along each side wall; galley chests |
| Armory | Two staggered rows of item frames on the back wall, split around a doorway; chests; tool racks along one side wall, armor stands along the other; guards |
| Vehicle bay | Battery buffers and a workbench on the back wall; a workshop chest |
| Server room | Rows of two-high server racks (the style's console block); the operator's desk with a typewriter on the back wall and its chair; a comms machine; a comms chest |
| Missile assembly | The NASA Workbench in the middle, or the fitting place nearest it; two workbenches; casing frame boxes on the back wall; salvage chests; guards (no furniture: a bay) |
| Water treatment | Basins of standing water in rows; pumps on the back wall |
| Guard post | A `post` spawner; two lockers in a back corner; a desk with a clipboard over it in the other, its chair in front |
| Decontamination airlock | A bench of stools along one side wall, suits on armor stands along the other |

- **The hub:** salvage chests spread along its walls, clear of every portal, plus a workbench in the middle, guards toward the corners and the hub's sign beside a portal.
- **Halls:**
  - A blast-door mark and a sign naming the hall (`wing.<0..3>` by compass direction, or `silo_gallery`) at each mouth off the hub.
  - Roof, floor and wall damage cells, scaled by area, kept off lamps and openings.
  - A minecart rail along each major hall, one cell off its centerline: the centerline carries the middle row of floor light tiles, and a rail on glowstone drops off at the first block update beside it. A rail goes only over a FLOOR cell (D247).

## Guards (D233)

- **Branch points:** every opening from the hub or a hall into another hall (a mouth, a bend, a side branch, a minor hallway) has a `post` spawner beside it, two cells in from the wall and two clear of the opening.
- **Guard posts:** 3 to 5 guard-post rooms stand beside major hall mouths at the hub and beside minor hallway mouths.
- **Decontamination airlock:** sits in the corner between the hub and a major hall, with a door onto each.
- **Deep posts:** each silo has a `deep` post three cells into the gallery from its walkway, and the gallery's far end has one more. The contents pass makes them elite posts.

## Depth, stamp and surface

- **Level:** the floor is 24 blocks under the recorded ground (D207). The tallest section (12) still leaves 12 blocks of rock over it.
- **Silo:** the `SiloShape` piece cell by cell, turned to face its gallery:
  - concrete `C`/`F`, clear glass `G`, Gantry Beam `B`, Galacticraft pad `P`, with the 3 x 3 bore open;
  - the controller and hatches (input bus and maintenance beside it, energy and input HV below) are real GT machines facing the gallery;
  - exactly one part is missing: the controller (concrete in its place) or the Energy Hatch.
- **Shaft:** above the silo, a 5 x 5 concrete ring around the bore up to the ground height read in the shaft's window, with the open, half open or closed mouth variants. Natural cover over each mouth cell is cleared up to 32 blocks over the ground, so a mouth never sits under a canopy: plants, vines, snow layers, cactus, logs and leaves (`isWood` or `isLeaves`) and ice. Ice goes on being cleared above 32 while it lasts, so an ice spike is cut whole. Any other block stays (a chest, a fence, another mod's build), and so does every block with a tile entity (D239, D250).
- **Owner:** every generated GT machine (silo controllers and hatches, radar controllers, room machines) carries the owner "[Silo Complex]" with a fixed UUID, so GT's power-fail tracker has an owner when one stops or is broken (D239). The first real player (not a fake player) to right-click a generated Missile Silo, Launch Control or radar takes it over, so its power-fail notices, wireless energy and launches are that player's. GT's own machines in the rooms (generators, buffers, pumps) keep the complex as owner (D250).
- **Escape:** a 1 x 1 ladder at the dead end of a minor hallway (else of a major hall, else the hub's corner), clinging to the end wall (`ComplexPlan.escapeMeta`). It runs up through its section's ceiling (`escapeTop`) to a closed top-half trapdoor flush with the ground, with the natural cover over the trapdoor cleared as over a mouth. A lake over the column gets no trapdoor. The cell behind it is stone up to just under the surface block.
- **`landY`:** `HeightProbe.landY` is the top solid block, so the trapdoor replaces it and lies flush (D201).
- **Radar stations (D215):** 0 to 2 huts anywhere on the surface, at least 12 blocks apart, 3 blocks clear of every silo shaft and clear of the escape column. Each is the same 5 x 5 hut with the small radar on its roof as before, inert without its Energy Hatch.
- **Armory frames:**
  - The `ITEM_FRAME_WALL` mark is the first frame cell. It carries the frames' facing and how many frames hang every third cell along +X or +Z.
  - The world side hangs a gun from the weapon tables up to the ceiling in each frame.
  - The damage pass never breaks the wall behind a frame.

## Tests

- **Unit:**
  - `ComplexLayoutTest`: coverage over 200 seeds, rooms and kinds, sizes and heights, no overlaps, silo distribution over 1000 seeds, both sides and at most two halls, Launch Control and the assembly floor, posts at branch points, the escape ladder, armory frames, door-relative furnishing over 150 seeds with every mark reachable, reachability of every section and walkway, the lamp grid, the light simulation, and determinism.
  - `ComplexRasterTest`, `ComplexPlacerTest` (never cascading, order independence), `ComplexSignsTest`, `ComplexContentsTest` (lamps by run and section), `PaletteStylesTest` (interior pattern coordinates), `ComplexContentCountsTest` (a NASA Workbench in every missile assembly floor and workshop, room guards per area step, the lamp count printed and held in a band), `GtMachineHookTest` (who takes a generated machine over), `RoomFurnitureTest` (furniture only in small rooms, no doorway or path to a mark blocked, chairs face a table, desk or console, upper layers sit on something, every piece has a vanilla fallback, wear by level, D259), `BiblioFurnitureTest` and `ComplexExportTest`.
- **Scenarios:** `icbm.complex.*` (places, skips, levels, guards, dim.*, loot and machines). The fixed-cell scenarios build on `ComplexArena`: every column cleared from the chunk's top, stone below the floor the placer really uses (D239).
