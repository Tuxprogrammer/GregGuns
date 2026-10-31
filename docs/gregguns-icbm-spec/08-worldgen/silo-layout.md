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
  - Every mandatory kind is always present: launch control, comms, commander's office, warhead magazine, generator hall, armory and missile assembly. The rest come from the 22 optional kinds: radar and tracking, fuel depot, air handling, pump room, workshop, bunks, stockpile, mess galley, infirmary, vehicle bay, server room, cafeteria, water treatment, underground farm, mushroom cellar, cold storage, shooting range, brig, chemistry lab, showers and laundry, recreation lounge and incinerator.
  - **The deck (D307):** the kinds are dealt like cards. Round 1 holds every mandatory and optional kind once, shuffled together; each later round holds every optional kind again, freshly shuffled. A slot takes the first card in deck order that fits it, and a room just placed goes to the first card that fits its real size, so no kind repeats while a kind that fits is missing. The mandatory kinds always get a slot.
  - **Sizes:** the farm needs a slot of at least 20 x 16 walls; the range at least 12 x 22, with its door on a short wall (it never grows wider than it is deep); the showers never grow past 18 x 16.
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
  - Each silo gets a wall sign reading `Silo N` (N counts north to south, then west to east by shaft), in the walkway cell two above the controller (y 4, `SiloShape.NAME_SIGN_RISE`), hanging on the silo's concrete over the glass window and turned toward the gallery (D299). It is not on the controller, since a sign takes the click that would open the controller GUI, and not on the window, which it would hide. A silo missing its controller gets the same sign. `ComplexSigns.writeSilos`.
- **Turned to face the hall:** each silo's structure is stamped turned so its controller face is toward the gallery (`SiloSlot.facing`, `x(column, aisle)`, `z(column, aisle)`). The controller and hatches are placed facing that way. A walkway niche opens in the gallery wall in front of the silo's whole five-wide face (D299).
- **Launch Control** caps the gallery's far end. Its doorway opens onto the gallery, and both wall layers on either side of the doorway are clear glass blocks from layer 3 to 6, so the room overlooks the silos. If the far end is taken, Launch Control opens off the gallery's side.
- **Missile assembly floor** is the first room placed on the gallery's sides, in the span the silos leave free.

## Doors

`ComplexPlan.doors()`: each `Door` cuts both wall layers between two touching sections.

| Kind | Size | Where |
|---|---|---|
| Portal | The whole shared interior span; three under the lower ceiling (layer 8 or 9) between tall halls (a lintel above), the full height into a minor hallway | Hub to hall, hall to hall, hall to minor hallway |
| Door | 3 wide, up to layer 4 (vehicle bay off a hall: 5 wide, up to layer 5; small rooms: 2 wide, up to layer 3) | Room to its parent; extra room-to-room doors |
| Walkway | 5 wide, layers 1 to 4, the gallery wall only (D299) | In front of each silo's face: the window, both hatch columns and the `Silo N` sign |
| Window | Clear glass blocks, layers 3 to 6 | Launch Control over the gallery |

Doors and portals get **door frames** (the DOOR role): jambs either side and a lintel over the opening, in both wall layers, drawn in the room's accent block.

## Raster (cell roles)

`ComplexRaster` holds one role per cell (`Cell`) and, per column, the section that owns it.

- Each section writes its shell: WALL on its perimeter at every layer, FLOOR in layer 0 and CEILING in its top layer inside, air between.
- Then doors (air), door frames, window panes, lamps, furniture and rails. A blueprint's solid prop is `PROP`: the shell writes air there and the marks pass fills it; it is not open for walking and not a floor, wall or ceiling role.
- The silo roles stay as they were (concrete, glass, beam, pad, controller, hatches).

The writer resolves the building roles through the owning section's palette style: WALL, FLOOR, CEILING, LAMP, DOOR_FRAME and furniture. Glass and water resolve by themselves. See [palette](palette.md) and D235.

## Lighting (D230, D231)

- **Lamps are flush in the ceiling layer**, on a grid aligned to their section.
  - Lines run at most 6 apart, spread evenly so the grid is centered (`ComplexFurnisher.grid`).
  - In halls the rows run along the hall's axis. In rooms they run along the door wall.
  - A room's blueprint sets its gap (D305): 6, 7 for the head-to-head bed bands, 4 for dense racks and display walls. The shadow fill adds floor tiles where props would leave an open cell under 8 (see Furnishing).
- **Light tiles in the floor:** a section taller than 7 also gets a lamp flush in the floor layer under every ceiling lamp. Minecraft's block light falls one level per block, so a ceiling lamp 8 or more blocks up cannot light the floor to 8. A floor light tile lights every floor cell within 6 to 8 or more. A minor hallway (7 high) has ceiling lamps only, every 3 cells along its centerline, which keeps D217's floor light of 8.
- **Silo walkways (D296):** each silo's walkway (the niche in the gallery wall in front of the window) gets one lamp of its own, flush in the gallery wall right over the niche's middle column (layer 5, over the four-high niche). The gallery's grid lamps sit up to 12 cells up and several cells out, which left the niche and the silo's window face dark. It is an ordinary light mark of the gallery, key `lamp:<gallery>:<1000 + silo index>`, so each walkway is its own run and follows the activity-level roll like any other run. The walkway floor reads 11 or more (D299).
- **Light marks:** a `LIGHT` mark sits at each lamp (ceiling layer or floor layer), with the key `lamp:<section>:<run>`. A run is one grid line. The shell writes the style's dark lamp; the contents pass lights it with the style's lit lamp.
- **Activity level:** an abandoned complex lights or kills whole runs, a partly abandoned one whole sections, an operational one everything. The D36 shares stay: 5%, 45% and 100%.
- **Proof:**
  - `ComplexLayoutTest` simulates block light over 12 plans, with machines, guards, cube furniture and props counted solid (beds, bars, panes and the like let light through). Every open floor cell reads 8 or more.
  - `icbm.complex.levels` reads the saved block light of every open floor cell of every section in an operational complex.

## Marks (the seam with activity, guards and loot)

`ComplexLayout.generate(worldSeed, cellX, cellZ)` returns a `ComplexPlan` of `Room`s (the hub is `hall()`), `Tunnel`s (major and minor halls), `SiloSlot`s and `Door`s. Each room and hall carries `Mark`s (`MarkKind`, x, y, z, key):

- loot chest, with a loot-table key from `LootKeys`;
- spawner, keyed `post`, `room` or `deep`;
- machine, keyed by `MachineRole`;
- item-frame wall, light, damage cell (`wall`, `floor`, `roof`), flood cell, rubble, sign and blast door;
- furniture (D234).

The plan is pure geometry and deterministic. It never changes by activity level.

## Furnishing (D208, D305, D306)

Every room kind, and the hub, is furnished by its own **blueprint** (`complex/blueprint/rooms/`, registered by kind name in `RoomBlueprints`) on a pure **kit** (`Kit`), in the room's own frame (`ComplexLayout.Frame`, D208): lateral `a` along the door wall, depth `d` from the back wall (0) to the door wall. The back wall is the wall opposite the doorway to the room's parent, whichever side that is. The sign hangs on the doorway's jamb, facing in.

- **Modules:** each blueprint writes its repeating pieces as ASCII rows (the text the owner reviewed in C-blueprints), parsed through a legend (`Legend`: a character is a stack of pieces, a chest, a machine, a guard or a kept cell). A module is stamped whole or not at all: one that would cover a doorway zone, a floor light tile, a floor hole, a reserved cell or another piece is dropped, so no bed loses its head. Thin, see-through pieces (bars, panes, rugs, the NASA ring) may stand on a light tile.
- **Lamp rows are aisles:** modules fill the bands between the rows of floor light tiles, so every tile lies in an aisle. They repeat outward from the door's spine on both sides, mirrored, so a room reads symmetric at any width.
- **The spine:** the door's clear path to the back wall (or to a stop: a range's firing line, a brig's corridor). A door in the back or door wall gets a path in to the nearest lamp row and along it to the spine; a door in a side wall keeps its doorway zone. The hub has a cross through its middle and a lane from each portal's middle to it.
- **Kept clear:** the cells inside every doorway, two deep and one wider each side; the NASA Workbench's 5 x 5 block (its 3 x 3 core strictly free, its ring free from layer 1 up, a floor tile allowed in the ring; the fitting place nearest the wanted cell, D252); the cells in front of the hub's signs.
- **Reachability:** after the blueprint, the kit clears the shortest way through pieces (a grouped piece as a whole) from the doorway to every chest, machine and guard, to every other door of the room (D278) and to every open floor cell. A chest walled in by other marks takes the nearest chest in its way as a last resort; a cell sealed only by marks gets a crate.
- **Light:** a blueprint picks its lamp gap: 6, 7 for the head-to-head bed bands (an odd gap of 7 keeps every floor cell within 3 + 3 of a tile) or 4 for dense racks and display walls. The **shadow fill** then adds a floor light tile on, or nearest to, the darkest open cell that the tiles leave under 8, counting props, cube furniture, machines and guards as shadows. The cells checked are open floor and cells under chairs, stools and stands. Each added tile takes the key of the room's nearest tile, so it lights and dies with that run (D231).
- **Facing:** every piece faces a direction in the frame, turned into a world facing on commit. A piece left to the kit faces: for a chair, the table, desk, console or counter it sits at (a chair with none is dropped); for a desk or console, its chair; otherwise away from the wall, or away from the piece behind it into the aisle. Machines face a horizontal side.
- **Chests:** no chest touches two chests, and no chest touches a chest that already touches one. Chest marks come last in the room's mark list, in slot order: the safe first, then outward from the spine, back to front. The loot budget reads that order (D315).
- **Counts:** room guards keep one per 450 interior cells (D234); chests and machines are the blueprint's own.
- **Groups:** a bed's two halves and the cells of a container prism or a pool table share a group (the key's `#group`, the packed column of the group's first cell), so they wear whole.
- **Furniture marks:** `FURNITURE`, one per layer, `NAME:variant` (the wood for a wooden piece), with the facing; a floor inlay (water, farmland, mycelium) at y 0. The raster writes a style cube, an open cell, or `PROP` (a solid prop the marks pass fills); `FurnitureFiller` sets the real piece ([palette](palette.md#props-d305-d306-d308)).
- **Beds (D306):** vanilla beds, the foot's meta the direction to the head and the head's that plus 8, each half written by the window that owns it. In the bunks and the infirmary they lie head to head in any band of 4 or more rows (foot chest, foot, head, head, foot, foot chest), a GT Locker between the heads (a white glass-pane curtain in the ward). A foot chest stands beyond each foot, its front to the aisle, out on the lamp row when the band has no room. A band of 3 takes one row, heads to the back.

| Kind | Blueprint |
|---|---|
| Hub (assembly bay) | A container yard: shipping containers two deep along the walls with salvage chests between them; rows of containers and crate pallets with chests in the bands; metal props and crates along the side walls; a workbench near the middle; corner caches; the hall posts at its branch points and three room guards |
| Bunks | Head-to-head bed pairs with a locker between the heads and a foot chest at each foot; kit shelves and lockers on the back wall, lockers down the left wall, rifle racks down the right; corner caches; room guards |
| Infirmary | The bunks' bed pairs with pane curtains; every third band of treatment bays (an exam table, a brewing stand, two chairs); medicine cabinets and sinks on the back wall, cabinets down the left; the nurse's station by the door |
| Mess galley, cafeteria | The kitchen line (furnaces, cauldrons, counters, pantry chests) on the back wall; a serving counter; runs of 3 tables with a chair each side in every band (D259's density); pantry chests and crates down the side walls; the cafeteria adds two salad bars |
| Recreation lounge | Bands of pool tables on rugs and of card tables; a bar with stools and a sofa corner facing a screen wall in front; bookcases and jukeboxes |
| Showers and laundry | Shower stalls on the back wall (a drain in the floor under a shower head, bar dividers); lockers and chests down the left wall with a bench; a laundry corner of wash tubs, linen piles and chests |
| Guard post | The `post` spawner; lockers, a rifle rack and the duty desk with its chair and chest on the back wall; a card table; ammo chests and a rack on the side walls |
| Decontamination airlock | A decon shower (cauldrons in a bar frame) in the middle; a bench and chests down one side, suits on stands and chests down the other; lockers and chests on the back wall |
| Brig | A row of cells on the back wall (bunk, cauldron, bars, a closed iron door), a second row facing it in a deep brig, never across another door's way in; guard desks with consoles; evidence lockers and chests; an interrogation table; crates; guards |
| Shooting range | A hay-bale backstop; a wool target on a fence post in each lane; bar lane dividers; booths at the firing line; ammo chests and rifle racks down both side walls and behind the line; the range officer's desk |
| Launch control | The Launch Control machine in the middle; the console row with chairs along the window wall; plot tables and console pods in alternate bands; server racks with control chests down both side walls; shelves, lockers and chests on the back wall; guards |
| Radar and tracking | The radar machine; plot tables in the first band, back-to-back console pods in the rest; a console wall with chairs on the left |
| Comms | The comms machine at the back; four-operator pods (desks with typewriters) in every band; a console wall on the left; racks, files and comms chests |
| Commander's office | The desk with a typewriter on the axis, the commander's chair behind and two visitors' chairs in front; bookcases on the back wall, bookcases and filing chests down the left, armor stands and filing shelves down the right; secondary desks with typewriters and filing chests in the front corners; each band in turn a meeting table, a center desk island with chairs, a couch corner on a rug, a map table or filing shelves, stepping down in a short or narrow band (a quarter of the floor or more at every size); the safe, always the first slot |
| Server room | Two-high rack pairs in every band (lamp gap 4); UPS battery buffers and comms chests on the back wall; the comms machine and the operator's desk at the front |
| Generator hall | Bands of generator sets (a 3-long vent-riser turbine housing with the GT generator at its aisle end), fuel container prisms and spares pallets (metal props, crates, a chest) in turn, every 5 cells; battery buffers and container prisms on the back wall; a control desk at the front; at least 8 chests; room guards |
| Air handling | Air-handler units (two machines inside a ring of floor-to-ceiling vent risers) every 5 cells in each band; filter crates and chests on the back wall; crates down the left wall |
| Pump room | Pump bays (a pipe riser, the pump between two sumps sunk in the floor) every 5 cells in each band; drums down the left wall; flood cells along the aisles |
| Water treatment | Basin strips three wide every 6 cells, a column of filter tanks and dosing drums beside each; pumps and chests on the back wall |
| Workshop | The NASA Workbench in the back-left corner with its kept-free ring (D252); assemblers and a row of GT machine tools (lathe, bender, press, wiremill, cutter); crafting-table and anvil islands with stools in the bands; tool racks down the right wall; stock pallets (metal props around a chest) in the front band; at least 15 chests |
| Fuel depot | Tank islands (the fuel tank ringed by drums, a fuel chest at one corner) every 4 cells in each band; fuel container prisms two deep on the back wall; a drum wall down the left side |
| Vehicle bay | A drive lane as wide as the doorway to the back wall; shipping container prisms and crate pallets with a chest every 7 cells either side; charging buffers along the back wall; a 2 x 2 fuel point by the door |
| Missile assembly | The NASA Workbench in the middle with its ring and a workbench either side; casing racks (frame boxes, two high) with a parts chest at the outer end, every 5 cells, kept 4 cells clear of the spine, which stops at the jig; body-part container prisms on the back wall; plate stock and chests down the side walls; at least 24 chests; room guards |
| Chemistry lab | Working GT Chemical Reactors along the back wall with glass between; lab benches (stools either side of a counter with a brewing stand, a cauldron and a chest) every 4 cells in the bands; safety-shower cauldrons and chests down the left wall; a row of GT lab machines (centrifuge, electrolyzer, mixer, distillery, chemical bath, brewery) at the front |
| Incinerator | A bank of two-high furnaces with iron-bar chimney risers on the back wall; GT recyclers, electric furnaces and arc furnaces in the first band; ash heaps and coal bunkers, then sorting stations (crates, a chest, a table), in the bands after |
| Armory | D289's display partitions every 6 rows while they fit, a gun every 5 cells on each face with its ammo chest, no gun cap; ammo chests and crates on the back wall; rifle racks with chests down the left wall, armor stands with chests down the right; chests at the partition ends; cleaning benches at the front; at least 27 chests besides the displays; room guards (lamp gap 4) |
| Warhead magazine | 2 x 4 racks of inert casing frame boxes (two high) with a chest and a crate at their ends, every 3 cells, a 2-wide lane kept along each side wall; steel container prisms two deep on the back wall; fallout rubble marks; room guards |
| Stockpile | Back-to-back pallet racks of chests with wooden crate stacks between (no block above a chest) every 8 cells; container prisms two deep on the back wall; metal props (D308) down the left wall and at the front |
| Cold storage | Freezer racks of packed ice with chests set in them every 8 cells; packed ice down both side walls; reefer prisms (light iron crates) two deep on the back wall |
| Underground farm | Every band between the lamp rows is a field strip (crop, crop, water channel, crop, crop; one crop per strip: wheat, carrots or potatoes), the lamp rows the walkways, the fields 2 cells clear of the side walls; hay bales and seed chests on the back wall; tool racks and crates down the sides. Crops grow only in a lit room (D309) |
| Mushroom cellar | Mycelium beds three wide framed by two-high mushroom stems every 6 cells; crate-and-chest racks in the front band and down the side walls |

- **The hub's signs:** the hub's sign hangs beside a portal (`Silo Complex / Class X`), with the activity sign under it (`Activity Level:` over Alpha, Bravo or Charlie, D301; [activity-levels](activity-levels.md#the-sign-d301)). The hub blueprint keeps the 3 x 3 cells in front of the sign clear.
- **Wear and the activity levels** follow the room's state (D309): see [activity levels](activity-levels.md).
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
  - concrete `C`/`F`/`H`, clear glass `G`, Gantry Beam `B`, Galacticraft pad `P`, with the 3 x 3 bore open;
  - the controller and hatches are real GT machines facing the gallery. The face is five wide (D299, the owner's build): a clear 3 x 3 glass window around the controller with a hatch column on each side. Seen from the gallery: Input Hatch over Maintenance Hatch on the left, Input Bus over Energy Hatch on the right (`SiloSlot.hatchCell`). All eight window cells around the controller are clear glass, so the missile shows;
  - exactly one part is missing: the controller (concrete in its place) or the Energy Hatch (the hatch column's concrete in its place);
  - its own Launch Control is a real GT machine on top of the right hatch column (cell `SILO_LAUNCH_CONTROL`, D288, D299), facing the gallery; concrete tops the left column. It is never the missing part, so the silo fires through it once the missing part is in. It runs on the silo's EU (D300).
- **Shaft:** above the silo, a 5 x 5 concrete ring around the bore up to the ground height read in the shaft's window, with the open, half open or closed mouth variants. Natural cover over each mouth cell is cleared up to 32 blocks over the ground, so a mouth never sits under a canopy: plants, vines, snow layers, cactus, logs and leaves (`isWood` or `isLeaves`) and ice. Ice goes on being cleared above 32 while it lasts, so an ice spike is cut whole. Any other block stays (a chest, a fence, another mod's build), and so does every block with a tile entity (D239, D250).
- **Owner:** every generated GT machine (silo controllers and hatches, radar controllers, room machines) carries the owner "[Silo Complex]" with a fixed UUID, so GT's power-fail tracker has an owner when one stops or is broken (D239). The first real player (not a fake player) to right-click a generated Missile Silo, Launch Control or radar takes it over, so its power-fail notices, wireless energy and launches are that player's. GT's own machines in the rooms (generators, buffers, pumps) keep the complex as owner (D250).
- **Escape:** a 1 x 1 ladder at the dead end of a minor hallway (else of a major hall, else the hub's corner), clinging to the end wall (`ComplexPlan.escapeMeta`). It runs up through its section's ceiling (`escapeTop`) to a closed top-half trapdoor flush with the ground, with the natural cover over the trapdoor cleared as over a mouth. A lake over the column gets no trapdoor. The cell behind it is stone up to just under the surface block.
- **`landY`:** `HeightProbe.landY` is the top solid block, so the trapdoor replaces it and lies flush (D201).
- **Radar stations (D215):** 0 to 2 huts anywhere on the surface, at least 12 blocks apart, 3 blocks clear of every silo shaft and clear of the escape column. Each is the same 5 x 5 hut with the small radar on its roof as before, inert without its Energy Hatch.
- **Armory display walls:**
  - `ComplexFurnisher.displays` lays up to two rows of partition (`DISPLAY_ROWS`) parallel to the door wall, at depths 4 and 10 from the back wall, each as long as the room allows between three-cell open ends. A row is dropped when its front chest would crowd the door rows. A partition is two blocks tall (`PARTITION_HEIGHT`), so the space above it stays open to the ceiling. Its cells are `FURNITURE` marks keyed `PARTITION` that the raster writes as the wall role: the room's own wall block, so the three-different-blocks rule and `RoleAudit` hold. Wear never removes them.
  - Each face carries a `GUN_DISPLAY` mark every `DISPLAY_STRIDE` (5) cells, the two faces staggered by two. The mark sits at the floor cell in front of the wall (the chest cell) and carries the frame's facing; the frame hangs one layer up, on the partition. No frame hangs on the outer walls, and the old `ITEM_FRAME_WALL` mark is no longer used for the armory.
  - The armory takes the closer lamp grid of the furniture-row rooms (`DENSE_GAP`), since the partitions cast shadows.
  - An armory shows at most `MAX_DISPLAYS` (24) guns, so a very large room does not outgrow the old two rows of frames.
  - Walkways of three cells separate the rows; the reachability pass keeps every chest reachable.
  - The contents pass turns a mark into an ammo chest (`armory_ammo`) and a one-frame `FRAME_WALL` placement. The gun is `SiloFrames.gunFor` of the cell, so frame and chest agree whichever window builds them. A collapsed armory shows no displays; an abandoned complex keeps the share of display slots that `ActivityProfile.chestKeepChance()` gives its other chests, so one knob rules both (D289).
  - The chest holds the gun's ammo (`ArmoryDisplay.stockFor`): 4 to 8 loaded magazines and 24 to 64 loose rounds for a magazine gun, 32 to 64 rounds for a loose-round gun, 8 to 24 of the one item a rocket, grenade or fuel-tank gun burns. An energy gun's chest draws the armory table.
  - The damage pass never breaks the wall behind a frame.

## Tests

- **Unit:**
  - `ComplexLayoutTest`: coverage over 200 seeds, rooms and kinds, sizes and heights, no overlaps, silo distribution over 1000 seeds, both sides and at most two halls, Launch Control and the assembly floor, posts at branch points, the escape ladder, armory frames, door-relative furnishing over 150 seeds with every mark reachable, reachability of every section and walkway, the lamp grid, the walkway lamps, the light simulation, and determinism.
  - `ComplexRasterTest`, `ComplexPlacerTest` (never cascading, order independence), `ComplexSignsTest`, `ComplexContentsTest` (lamps by run and section), `PaletteStylesTest` (interior pattern coordinates), `ComplexContentCountsTest` (a NASA Workbench in every missile assembly floor and workshop, room guards per area step, the lamp count printed and held in a band), `GtMachineHookTest` (who takes a generated machine over), `KitTest` (atomic stamps, thin pieces on tiles, the chest rule, beds paired, chests facing their aisle, walled-in cells opened, D305), `RoomBlueprintTest` (every crew, command and security blueprint and the hub at three sizes: nothing on a doorway, hole or tile, every mark, door and open cell reached, light 8 on every open cell, chairs seated, beds whole, the chest rule, the chest spots the loot budget needs and the fill bands; it prints the maps), `BiblioFurnitureTest` and `ComplexExportTest` (with `GREGGUNS_EXPORT_ROOMS`, each crew, command and security room and the hub at its 10th, 50th and 90th percentile size at every activity level).
- **Scenarios:** `icbm.complex.*` (places, skips, levels, guards, dim.*, loot and machines). The fixed-cell scenarios build on `ComplexArena`: every column cleared from the chunk's top, stone below the floor the placer really uses (D239).
