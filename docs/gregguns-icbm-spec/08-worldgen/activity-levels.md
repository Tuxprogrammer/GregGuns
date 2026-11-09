# Activity levels and the damage pass

- **Baseline:** none (the ICBM baseline has no worldgen).
- **Decisions:** [D36](../DECISIONS.md#d36-in-detail), [D128](../DECISIONS.md), [D17](../DECISIONS.md#d17-in-detail), D180 to D186, D229, D231, D284, [D301](../DECISIONS.md), D309, D315 to D317
- **Pack mods:** none beyond the layout's (GregTech, Galacticraft)
- **Roadmap:** [I6](../../ROADMAP-ICBM.md#i6--world-missile-silos-on-every-planet)

## What changes from the ICBM baseline

Nothing exists in the baseline. D36 gives each complex an activity level, rolled once from the complex's seed when the complex is first placed and recorded with its placement, so a later weight change never splits a complex (D304): **abandoned** 30%, **partly abandoned** 40%, **operational** 30% (config). The layout ([silo-layout](silo-layout.md)) is the same at every level. The level only decides which marks become what: `ComplexContents.build(plan, level, ceiling, other)` turns the layout's marks into `Placement`s.

## How the pass stays inside the populate window

Every roll is a hash of the plan's seed and the mark's own cell, never of call order or of another chunk. Each populate window rebuilds the placement list and applies only the cells it owns, so the damage is part of generation and never carves afterward (D17, D36). No placement reads a block outside its cell.

## Numbers (`ActivityProfile`, config starting values)

| Rate | Abandoned | Partly abandoned | Operational |
|---|---|---|---|
| Room collapse (cave-in) | 30% | 10% | 0% |
| Wall breach per damage cell | 12% | 4% | 0% |
| Rubble per wall or floor cell | 10% | 4% | 1% |
| Floor hole per floor cell | 12% | 3% | 0% |
| Light lit (by run, by section, all) | 5% | 45% | 100% |
| Machine slot left empty (D284) | 20% | 0% | 0% |
| Machine is a dead husk (of the rest) | 90% | by room: 10% in service, 90% mothballed | 0% |
| Loot chests in the whole complex (D315) | 100 | 200 | 300 |
| Pump room flooded | yes | yes | no |
| Fallout rubble in the magazine | all marks | 40% | none |
| Spawners per room, 0 / 1 / 2 (D326) | 80 / 20 / 0% | 25 / 55 / 20% | 0 / 75 / 25% |
| Room state (D317) | gutted | in service (lit room) or mothballed (dark room) | staffed |

**Spawners per room (D326).** Every room, the hub included, rolls its own count from the plan seed against the row above, so guards spread through the whole complex, the outer rooms and far wings among them. The layout offers each room up to two spawner spots (bare, reachable floor cells far apart), and the room holds the first N of them. A caved-in room holds none; halls hold none. An abandoned complex's spawners are never elite. About 8% of an abandoned complex's machine slots hold a working machine (D284), so a working machine in a ruin is a rare find.

Depth band: 0 is the hub, then one band per 30 blocks of distance from it, up to 3; at band 2 or more, 40% of a non-abandoned complex's spawners are elite. Collapses hit rooms only, never the hub or a hall (the silo gallery among them), so the complex stays walkable. Guard posts, the decontamination airlock and Launch Control never collapse at any level; Launch Control's lamps, rubble and breaches still follow the level (D252). A collapsed room turns its floor and wall damage cells to rubble, opens its roof cells, and the world side fills part of its box with rubble.

**Lamps die by circuit, not one by one (D231).** A lamp's mark carries its section and its run (one line of the section's lamp grid, ceiling lamps and the floor light tiles under them together). An abandoned complex rolls once per run, so a few runs still burn and most are dead. A partly abandoned complex rolls once per section, so whole rooms and halls are dark or lit. An operational complex lights everything. The share lit stays the D36 rate. A dead lamp stays in place as the style's dark lamp, flush in the ceiling or floor.

## The sign (D301)

The hub names its level. Under the hub's `Silo Complex / Class X` sign hangs a second wall sign, one block lower in the same column, facing the same way on the same wall: **Activity Level:** over the level's code name.

| Level | Code name | Lang key |
|---|---|---|
| Abandoned | **Alpha** (a gutted ruin) | `icbm.complex.activity.abandoned` |
| Partly abandoned | **Bravo** (half-staffed) | `icbm.complex.activity.partly_abandoned` |
| Operational | **Charlie** (fully staffed) | `icbm.complex.activity.operational` |

The sign's text is `icbm.complex.sign.activity` (`Activity Level:|%s`). Two lines, because a sign line holds 15 characters and "Activity Level:" is 15. The key is the level's enum name, so a change of the level weights never changes a name. It is written only where the cell under the tier sign is open and the wall behind it is solid (`ComplexSigns.activitySignAt`); the hub's furniture keeps that cell clear. Like every complex sign, it is not written in a column the terrain cuts off (D207, D345).

## Room looks (D309)

Each room has one state, `ComplexContents.roomState(seed, room, level)`, and everything in the room follows it, in every populate window.

| Level | State | How the room reads |
|---|---|---|
| Charlie (operational) | `STAFFED` | Lit, every piece in place, every machine working, furnaces burning, cauldrons full, crops sown in turns (half ripe, the rest at every stage) |
| Bravo (partly abandoned) | `IN_SERVICE` when the room's lamps burn (the section roll above, 45%) | Lit and worked: 5% of pieces gone, 10% of machines husks, crops at mixed growth (10% bare), furnaces burning |
| Bravo | `MOTHBALLED` when they are dark | Shut: 30% of pieces gone, half the metal props taken, 90% of machines husks, furnaces cold, cauldrons two-thirds full, anvils damaged, fields bare with dirt patches and mushrooms, half the brig doors open |
| Alpha (abandoned) | `GUTTED` | A ruin: 35% of pieces gone, 40% of those leave rubble, 70% of metal props and coal taken, a quarter of the containers lose their top layer, machines as D284, cauldrons a third full, fields dead (dirt, mushrooms, cobwebs, dry channels), glass broken, a huge mushroom cap on some cellar stems, doors open, one rubble pile per 40 floor cells |

**Wear by piece** (`FurnitureWear`, from the piece's wear class): a whole group goes or stays (a bed's two halves, a container prism, a piece's stacked layers). Rugs, water inlays, basins and partitions never wear.

**Crops grow only in lit rooms.** A vanilla crop pops off below light 8 on its first random tick, so the contents pass plants crops only in a staffed or in-service room. A dark field gets no crop: mushrooms grow there instead, which live in the dark. Mycelium beds keep their mushrooms at every level.

**Lit furnaces** are written with a tile that burns for a long while, coal in the fuel slot and sand to smelt, so a staffed room's furnaces are still lit when a player walks in.

**Rubble piles never cut anything off.** A pile cell stays only where the open cells around it stay joined without it and every chest, machine or guard beside it keeps another open side, so the reachability of every chest, machine and door holds at every level.

A collapsed room is filled as before, and its pieces are crushed to rubble.

**The Vending Machine (D355).** The one dining hall that holds the pack's Vending Machine (see [silo layout](silo-layout.md#furnishing-d208-d305-d306)) shows it by state (`VendingEgg`): staffed, in service and mothballed rooms (Charlie and Bravo) get a working machine that GregTech forms on its own first structure check, its screen glowing even in a dark hall; a gutted room (Alpha) gets a dead one, the controller standing dark and the top casing of the other column knocked out, so it never forms until a player puts a casing back. A caved-in hall crushes it to rubble. The machine holds nothing of its own: its trades are the pack's database, priced in coins.

## Furniture stock (D323 to D325)

Owner, play session 3: the furniture stood empty. Every piece that holds things holds them, by the room's state, so the levels read as Alpha a gutted ruin, Bravo half-staffed and Charlie fully staffed. `FurnitureStock` decides it (pure, one `STOCK` placement per stocked piece) and `FurnitureStocker` makes the items in the piece's own tile, after the piece is written. The stock goes only into the piece's own block (its BiblioCraft block, its prop block, a furnace of either state) in a column the level was written in, never into a container the terrain or another mod left in a cut-off column (D207, D345).

**Who holds anything** (`FurnitureStock.FILLED`): every piece in a staffed room, 90% in service, 55% mothballed, 30% gutted. A furnace and a flower pot always hold something. A piece the wear pass removed and every piece in a caved-in room hold nothing.

| Piece | Source | What it holds | Staffed / in service / mothballed / gutted |
|---|---|---|---|
| BiblioCraft bookcase | `books` | Crew notes and manuals as written books ([crew writing](#crew-writing-d324)), some plain books | 9-15 / 6-12 / 3-8 / 1-4 |
| BiblioCraft shelf | `room` | A draw of the room's chest table at the room's state (D316, D317), junk and spoiled food in a ruin | up to 4 slots |
| BiblioCraft table | `room` | One thing from the room's table, shown on its top; none under a plate or a pot | 1 |
| BiblioCraft potion shelf | `medicine` | Healing, regeneration, fire resistance and swiftness potions; empties and water bottles once shut | 6-11 / 4-9 / 2-6 / 1-3 |
| BiblioCraft tool rack | `weapons` in the armory, guard post, brig, range and hub, else `tools` | The complex's issue guns and sidearms and combat knives, or hand tools; gear worn in a ruin | 3-4 / 2-4 / 1-3 / 1-2 |
| BiblioCraft armor stand | `armor_combat` (armory, guard post, brig, range, hub, commander), `armor_hazmat` (decon airlock, warhead magazine), else `armor_crew` | A piece made for each filled slot, from the bunk, combat or hazmat pool at the ceiling; worn in a ruin; or the stand stands empty | 3-4 / 2-4 / 1-3 / 1-2 slots |
| BiblioCraft writing desk | `desk` | A note or a manual open on it, books stacked beside it | open book plus 3-8 / 2-6 / 1-4 / 0-2 |
| BiblioCraft typewriter | `typewriter` | Paper in the tray and a typed crew note | 8-32 / 8-32 / 4-15 / 1-4 sheets |
| BiblioCraft clipboard | `clipboard` | A clipboard with one page of the room's list; worked rooms part done, a mothballed room's shutdown list all done, a ruin's list stopped part way with a few crossed out | 1 |
| BiblioCraft dinner plate | `plate` | Meals and snacks; spoiled to rotten flesh at the state's D317 spoil chance | 2-3 / 1-3 / 1-2 / 1 |
| BiblioCraft cookie jar | `cookies` | Cookies | 5-8 / 3-7 / 1-4 / 1-2 |
| Flower pot | `plant` | A flower, fern, sapling or cactus in a lit room; a dead bush or a mushroom in a dark one | 1 |
| Furnace | `furnace` | Lit: the room's input (meat in a galley, iron ore or cobblestone in a works room, clay in the chem lab, sand elsewhere), coal, and what that input makes. Cold: coal and the last output. A ruin: a little charcoal, or none and its last output | |
| Brewing stand | `brewing` | Finished potions in the bottle slots and a reagent waiting while worked; water bottles mixed in once shut | 2-3 / 1-3 / 1-2 / 1 bottles |

**Dressing** (D323, [silo-layout](silo-layout.md#furnishing-d208-d305-d306)): plates, cookie jars, flower pots and clipboards are pieces the room's dressing pass stands on what its blueprint placed.

**GT machines hold finished work only** (D325): a working basic machine with a recipe map holds the outputs of one of its own recipes its tier can run, one to six batches, in its output slots, at 80% staffed, 60% in service, 30% mothballed and 10% gutted (`ComplexContents.STOCKED`). Never an input, a fluid or a job in progress: the machine stands idle until a player feeds it. Husks, generators, buffers and hatches hold nothing.

**Every cauldron holds water** (D325): full (3) in a lit room, two-thirds (2) mothballed, a third (1) gutted.

**Not placed** (orchestrator, alpha 8): item frames (entities, more per-chunk entity load, and they look staged) and dropped-item litter (it despawns after five minutes).

### Crew writing (D324)

All of it is fiction about the complex's own crew, every text a lang key (`icbm.complex.notice.*`, `icbm.complex.clip.*`, `icbm.complex.book.*`), numbered from 1; `ComplexWriting` finds a family's size by looking keys up, so adding a text needs no code.

- **Notices:** under each standing room's name sign (one block down, on the same jamb, where the cell is open, holds no piece and has the wall behind it, and never in a column the terrain cuts off, D345). A worked room shows a duty roster or a note on its own work (food, crew, arms, missile, command, works or stores, from the room's D316 theme group), half and half; a mothballed room a closing notice; a gutted room a last log. Four lines of 15 characters.
- **Clipboards:** a title of up to 26 characters and up to nine tasks of 23, by the room's group while worked, a shutdown list when mothballed, a ruin's last list when gutted.
- **Books:** the group's manual or the site orientation (while worked, four in ten) or a crew note of the room's state. Pages of up to 256 characters.

## Damage by mark

| Mark | Placement |
|---|---|
| `DAMAGE_CELL` wall | air (breach), rubble, or intact |
| `DAMAGE_CELL` floor | air (hole), rubble, or intact |
| `DAMAGE_CELL` roof | air, or intact (rubble is never placed overhead: it would fall) |
| `LIGHT` | the style's lit lamp, or the dark lamp the shell wrote. The lamps of a section form a grid flush in the ceiling, with light tiles in the floor of every section over 7 high, so a fully lit section reads block light 8 or more on every open floor cell (D217, D231) |
| `FLOOD_CELL` | water when not operational |
| `RUBBLE` | fallout rubble (placeholder until I5's fallout blocks are on master) |
| `MACHINE` | the machine, or a husk |
| `BLAST_DOOR` | the hall's blast door (D335): shut when staffed or mothballed, open in service, stuck open or breached when gutted; see [silo layout](silo-layout.md#blast-doors-d335-d337) |

The service runs (D336) follow their hall's state the same way: whole unless gutted, broken when gutted ([silo layout](silo-layout.md#service-runs-d336)).

## Requirements

1. GGI-08-20: `ActivityLevel.roll` is seeded by the complex's seed and follows the configured weights.
2. GGI-08-21: an abandoned complex's spawners are all guard (never elite) and it has far fewer working machines than an operational one (D284); its chests are the budget's 100 (D315).
3. GGI-08-22: damage and collapse fall and the spawner count rises from abandoned to operational; over many seeds the rooms' counts follow the D326 weights.
4. GGI-08-23: the same plan, level, ceiling and `other` always give the same placements.
5. GGI-08-24: the pump room floods unless operational.
6. GGI-08-25: no placement reads or writes outside its own cell or box.

## Tests

Furniture stock (D323 to D325): `FurnitureStockTest` (a staffed room stocks every piece, the share falls with the state, nothing in a caved-in room or on a removed piece, counts in range, every cauldron holds water, only working machines hold work, a staffed farm at mixed stages), `RoomDressingTest`, `ComplexWritingTest` (every text fits its medium, every state and group has text) and the real-pack scenario `icbm.complex.furniture_fill` (each level written and every stocked piece opened: it holds the right kind of thing, Charlie's furniture is full and the share falls to Bravo and Alpha, stocked machines hold outputs and none holds an input or a job, every cauldron has water, every notice is written, crops at mixed stages).

Vending Machine (D355): `VendingSpotTest` (the spot in every dining hall and lounge at many sizes and door places: against a wall, facing in, bare walked floor in front, no counter beside it, no walk, chest or aisle cut), `VendingHallTest` (one machine per complex with a hall, in the seed's hall, a dining hall before a lounge, against the wall), `VendingEggTest` (the controller on the viewer's right for every facing; working, dead or crushed by state, over real plans at each level) and scenario `icbm.complex.vending_machine` (Alpha dead with its gap, Bravo formed, Charlie through the complex keep: the record holds the controller's tile, a replaced controller and casing come back, and GregTech forms the machine on its own tick; skipped without the Vending Machine mod).

Fixtures (D335 to D337): `ComplexFixturesTest` (a door per hall mouth in the hall's wall layer, shut, open or gutted by state with a way through, rubble and fallen trays only on open floor no mark claims, Ross casings in the door's plane, runs in open air and only thinner when gutted); scenario `icbm.complex.fixtures` reads every piece back in the world.

Unit: `ActivityLevelTest`, `RoomStateTest` (lamps and machines agree per room), `RoomLooksTest` (no chest, machine or guard cut off by wear at any level; crops and lit furnaces only in lit rooms; Bravo keeps at least three quarters of Charlie's pieces, Alpha half, rubble counted; metal props under the ceiling), `ComplexContentsTest` (the per-room spawner weights over many seeds, the placed spawners on layout spots, working machines against the other levels, every chest spot a chest or a crate with the budget's count and the displays on top; lamps by run and by section at the D36 shares). Scenarios: `icbm.complex.prop_facing` (Alpha and Bravo written and every chest and furnace read back against its mark's facing, the bed-foot chests and the bank-end furnaces of dark rooms counted, D345), `icbm.complex.cutoff_fill` (a Bravo complex over a valley: foreign chests in the cut-off columns stay empty and no sign hangs there, D345), `icbm.complex.door_light` (a Bravo complex with its blast doors written in one tick, ArchaicFix's full light queue drained from every door piece, five seconds of ticks: the server lives and the doors stand, D345), `icbm.complex.door_reload` (the same doors saved, unloaded and loaded back from disk: every piece read back already knows its light value, and the same drain leaves the server alive, D345), `icbm.complex.rooms` (each level written and read back room by room: the room looks in place, crops only in lit rooms and still standing after fast random ticks, lit furnaces still burning, metal props under the ceiling, every chest, machine and guard reachable from the door over passable blocks), `icbm.complex.levels` (each level, its tier and activity signs read back, plus Operational at UMV, generated onto a slab one window at a time and read back: spawners per room (none over two, the outer rooms reached), working machines and chests (abandoned far below operational), each spawner's mobs and danger floor (its vanilla mob spawner NBT); the style's lit lamps at the layout's light marks; block light 8 or more on every open floor cell of every section when operational; the cost per window; flooded pump room; collapses with rubble; holes and rubble; every chest filled from its table; every machine the one the picker chose; no armed or filled warhead block; the levels order as D36 says) and `icbm.complex.guards` (see silo-guard-spawner.md).

## Open questions

The level weights are the config `icbm.siloComplexActivityWeights`.

Weights and rates are tuned in I7.
