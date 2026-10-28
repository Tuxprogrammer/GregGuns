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
  - The first hall is straight and 50 to 62 long; it carries the silo gallery.
  - The others bend at their far end (85%), and some bend again (40%), never back toward the hub.
  - Up to three side branches leave the middle of a long segment.
  - A hall stops 20 blocks short of the edge, so rooms can cap it. A free far end grows toward the edge.
- **Minor hallways:** 8 to 15 hallways, 30 to 70 long, leave a hall's side. Some leave another minor hallway's side as sub-hallways between branches. Their sides and far end take rooms.
- **Rooms:** 35 to 40 rooms in all, the hub not counted.
  - Every mandatory kind is always present: launch control, comms, commander's office, warhead magazine, generator hall, armory and missile assembly. The rest come from the optional kinds, repeated as needed.
  - Rooms hang off halls side by side, each touching the last.
  - Rooms also hang off rooms, up to three deep, so not every room has a direct line to the hub.
  - About one in seven pairs of rooms that end up side by side gets an extra door, so the plan is a graph, not just a tree.
- **Growth to coverage:** every room then grows a cell at a time into the rock around it.
  - The limit is 150% of its kind's nominal size per axis. A side never grows past twice the other.
  - While the built footprint is under 78%, the limit rises 20 points, up to six times.
  - A plan that still misses 76%, 35 rooms or 4 silos is grown again from a seed mixed from the plan seed and the attempt number (up to eight attempts), and the best attempt is kept.
  - Over 1000 seeds: coverage is at least 76% (mean 78.5%). The mean room footprint is about 1000 cells, six times the I6 room's 168: 75% coverage with 35 to 40 rooms needs that.
- **Walls:** neighboring sections touch but never share a wall. A wall between two rooms is two blocks thick, so each side shows its own room's one wall block (D229 point 5).

## Silo gallery (D232)

- **Count:** 4 to 10 silos: 4 (26%), 5 (24%), 6 (18%), 7 (12%), 8 (10%), 9 (6%), 10 (4%). Each misses exactly one part (D128).
- **Where:** the silos line the gallery hall on both sides.
  - They stand in facing pairs, 7 blocks apart (5 of silo, 2 of rock), from the far end toward the hub. The last 14 blocks at the hub end stay clear.
  - Seven or more silos split across a second straight hall when one exists, never more than two halls.
  - A hall takes single silos only after it has a facing pair, so a gallery always has silos on both sides.
- **Turned to face the hall:** each silo's structure is stamped turned so its controller face is toward the gallery (`SiloSlot.facing`, `x(column, aisle)`, `z(column, aisle)`). The controller and hatches are placed facing that way. A 3-wide walkway niche opens in the gallery wall in front of the controller.
- **Launch Control** caps the gallery's far end. Its doorway opens onto the gallery, and both wall layers on either side of the doorway are glass panes from layer 3 to 6, so the room overlooks the silos. If the far end is taken, Launch Control opens off the gallery's side.
- **Missile assembly floor** is the first room placed on the gallery's sides, in the span the silos leave free.

## Doors

`ComplexPlan.doors()`: each `Door` cuts both wall layers between two touching sections.

| Kind | Size | Where |
|---|---|---|
| Portal | The whole shared interior span; up to layer 8 between tall halls (a lintel above), the full height into a minor hallway | Hub to hall, hall to hall, hall to minor hallway |
| Door | 3 wide, up to layer 4 (vehicle bay off a hall: 5 wide, up to layer 5; small rooms: 2 wide, up to layer 3) | Room to its parent; extra room-to-room doors |
| Walkway | 3 wide, up to layer 3, the gallery wall only | In front of each silo's controller |
| Window | Panes, layers 3 to 6 | Launch Control over the gallery |

Doors and portals get **door frames** (the DOOR role): jambs either side and a lintel over the opening, in both wall layers, drawn in the room's accent block.

## Raster (cell roles)

`ComplexRaster` holds one role per cell (`Cell`) and, per column, the section that owns it.

- Each section writes its shell: WALL on its perimeter at every layer, FLOOR in layer 0 and CEILING in its top layer inside, air between.
- Then doors (air), door frames, window panes, rails, lamps and furniture.
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

## Furnishing (D208, D234)

- **The frame:** every room is furnished in its own frame (`ComplexLayout.Frame`), not in world axes.
  - The back wall is the wall opposite the doorway to the room's parent, whichever of the four sides that is.
  - Chests and wall machines stand against the back wall; machines stand in the middle row.
  - The sign hangs on the doorway's jamb, facing in.
- **Kept clear:** the cells inside every doorway, two deep and one wider each side, any door of the room. Light tiles are kept clear too. If furniture walls a chest, machine or guard off from the doorway, the shortest way through the furniture is cleared.
- **Scaling:** chest, machine and guard counts grow by one step per 450 interior cells.
- **Furniture:** `FURNITURE` marks with the key TABLE, CONSOLE, BUNK or SHELF (shelves and racks two high). The raster writes them as furniture roles drawn from the style. `BASIN` is standing water sunk into the floor layer.

| Kind | Furnishing |
|---|---|
| Launch control | Launch Control machine in the middle; a console row facing the windows; control chests; guards |
| Radar and tracking, comms | The radar or comms machine in the middle; consoles along the side walls; chests on the back wall |
| Commander's office | A safe and a stockpile chest in the back corners; a desk; shelves |
| Warhead magazine | A grid of inert Stainless Steel frame boxes 3 apart; fallout rubble marks; magazine chests; guards |
| Fuel depot | Fuel tanks along both side walls; fuel chests |
| Generator hall | A row of generators; a battery buffer in a back corner; guards |
| Air handling, pump room | Their machines in the middle row; shelves (air); flood cells (pump) |
| Workshop | The NASA Workbench with its free ring; workbenches; a workshop chest; tables |
| Bunks, infirmary | Bunks or beds along the side walls; chests; shelves (infirmary) |
| Stockpile | Chests along the back wall; rows of shelves |
| Mess galley, cafeteria | A counter, rows of tables, galley chests |
| Armory | Two staggered rows of item frames on the back wall, split around a doorway; chests; tables; guards |
| Vehicle bay | Battery buffers and a workbench on the back wall; a workshop chest; a bench of tables |
| Server room | Rows of two-high racks; a comms machine; a comms chest |
| Missile assembly | The NASA Workbench in the middle; two workbenches; casing frame boxes on the back wall; salvage chests; guards |
| Water treatment | Basins of standing water in rows; pumps on the back wall |
| Guard post | A `post` spawner |

- **The hub:** salvage chests spread along its walls, clear of every portal, plus a workbench in the middle, guards toward the corners and the hub's sign beside a portal.
- **Halls:**
  - A blast-door mark and a sign naming the hall (`wing.<0..3>` by compass direction, or `silo_gallery`) at each mouth off the hub.
  - Roof, floor and wall damage cells, scaled by area, kept off lamps and openings.
  - A minecart rail along each major hall's centerline.

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
- **Shaft:** above the silo, a 5 x 5 concrete ring around the bore up to the ground height read in the shaft's window, with the open, half open or closed mouth variants. The 32 blocks over each mouth cell are cleared, so a mouth never sits under a canopy (only leaves, logs, snow or ice can lie above the ground there, D239).
- **Owner:** every generated GT machine (silo controllers and hatches, radar controllers, room machines) carries the owner "[Silo Complex]" with a fixed UUID, so GT's power-fail tracker has an owner when one stops or is broken (D239).
- **Escape:** a 1 x 1 ladder at the dead end of a minor hallway (else of a major hall, else the hub's corner), clinging to the end wall (`ComplexPlan.escapeMeta`). It runs up through its section's ceiling (`escapeTop`) to a closed top-half trapdoor flush with the ground, with the 32 blocks over the trapdoor cleared. A lake over the column gets no trapdoor. The cell behind it is stone up to just under the surface block.
- **`landY`:** `HeightProbe.landY` is the top solid block, so the trapdoor replaces it and lies flush (D201).
- **Radar stations (D215):** 0 to 2 huts anywhere on the surface, at least 12 blocks apart, 3 blocks clear of every silo shaft and clear of the escape column. Each is the same 5 x 5 hut with the small radar on its roof as before, inert without its Energy Hatch.
- **Armory frames:**
  - The `ITEM_FRAME_WALL` mark is the first frame cell. It carries the frames' facing and how many frames hang every third cell along +X or +Z.
  - The world side hangs a gun from the weapon tables up to the ceiling in each frame.
  - The damage pass never breaks the wall behind a frame.

## Tests

- **Unit:**
  - `ComplexLayoutTest`: coverage over 200 seeds, rooms and kinds, sizes and heights, no overlaps, silo distribution over 1000 seeds, both sides and at most two halls, Launch Control and the assembly floor, posts at branch points, the escape ladder, armory frames, door-relative furnishing over 150 seeds with every mark reachable, reachability of every section and walkway, the lamp grid, the light simulation, and determinism.
  - `ComplexRasterTest`, `ComplexPlacerTest` (never cascading, order independence), `ComplexSignsTest`, `ComplexContentsTest` (lamps by run and section), `PaletteStylesTest` and `ComplexExportTest`.
- **Scenarios:** `icbm.complex.*` (places, skips, levels, guards, dim.*, loot and machines). The fixed-cell scenarios build on `ComplexArena`: every column cleared from the chunk's top, stone below the floor the placer really uses (D239).
