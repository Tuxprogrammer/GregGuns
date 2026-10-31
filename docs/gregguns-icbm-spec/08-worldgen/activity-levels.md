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

The sign's text is `icbm.complex.sign.activity` (`Activity Level:|%s`). Two lines, because a sign line holds 15 characters and "Activity Level:" is 15. The key is the level's enum name, so a change of the level weights never changes a name. It is written only where the cell under the tier sign is open and the wall behind it is solid (`ComplexSigns.activitySignAt`); the hub's furniture keeps that cell clear.

## Room looks (D309)

Each room has one state, `ComplexContents.roomState(seed, room, level)`, and everything in the room follows it, in every populate window.

| Level | State | How the room reads |
|---|---|---|
| Charlie (operational) | `STAFFED` | Lit, every piece in place, every machine working, furnaces burning, cauldrons full, crops ripe |
| Bravo (partly abandoned) | `IN_SERVICE` when the room's lamps burn (the section roll above, 45%) | Lit and worked: 5% of pieces gone, 10% of machines husks, crops at mixed growth (10% bare), furnaces burning |
| Bravo | `MOTHBALLED` when they are dark | Shut: 30% of pieces gone, half the metal props taken, 90% of machines husks, furnaces cold, cauldrons dry, anvils damaged, fields bare with dirt patches and mushrooms, half the brig doors open |
| Alpha (abandoned) | `GUTTED` | A ruin: 35% of pieces gone, 40% of those leave rubble, 70% of metal props and coal taken, a quarter of the containers lose their top layer, machines as D284, fields dead (dirt, mushrooms, cobwebs, dry channels), glass broken, a huge mushroom cap on some cellar stems, doors open, one rubble pile per 40 floor cells |

**Wear by piece** (`FurnitureWear`, from the piece's wear class): a whole group goes or stays (a bed's two halves, a container prism, a piece's stacked layers). Rugs, water inlays, basins and partitions never wear.

**Crops grow only in lit rooms.** A vanilla crop pops off below light 8 on its first random tick, so the contents pass plants crops only in a staffed or in-service room. A dark field gets no crop: mushrooms grow there instead, which live in the dark. Mycelium beds keep their mushrooms at every level.

**Lit furnaces** are written with a tile that burns for a long while, coal in the fuel slot and sand to smelt, so a staffed room's furnaces are still lit when a player walks in.

**Rubble piles never cut anything off.** A pile cell stays only where the open cells around it stay joined without it and every chest, machine or guard beside it keeps another open side, so the reachability of every chest, machine and door holds at every level.

A collapsed room is filled as before, and its pieces are crushed to rubble.

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

## Requirements

1. GGI-08-20: `ActivityLevel.roll` is seeded by the complex's seed and follows the configured weights.
2. GGI-08-21: an abandoned complex's spawners are all guard (never elite) and it has far fewer working machines than an operational one (D284); its chests are the budget's 100 (D315).
3. GGI-08-22: damage and collapse fall and the spawner count rises from abandoned to operational; over many seeds the rooms' counts follow the D326 weights.
4. GGI-08-23: the same plan, level, ceiling and `other` always give the same placements.
5. GGI-08-24: the pump room floods unless operational.
6. GGI-08-25: no placement reads or writes outside its own cell or box.

## Tests

Unit: `ActivityLevelTest`, `RoomStateTest` (lamps and machines agree per room), `RoomLooksTest` (no chest, machine or guard cut off by wear at any level; crops and lit furnaces only in lit rooms; Bravo keeps at least three quarters of Charlie's pieces, Alpha half, rubble counted; metal props under the ceiling), `ComplexContentsTest` (the per-room spawner weights over many seeds, the placed spawners on layout spots, working machines against the other levels, every chest spot a chest or a crate with the budget's count and the displays on top; lamps by run and by section at the D36 shares). Scenarios: `icbm.complex.rooms` (each level written and read back room by room: the room looks in place, crops only in lit rooms and still standing after fast random ticks, lit furnaces still burning, metal props under the ceiling, every chest, machine and guard reachable from the door over passable blocks), `icbm.complex.levels` (each level, its tier and activity signs read back, plus Operational at UMV, generated onto a slab one window at a time and read back: spawners per room (none over two, the outer rooms reached), working machines and chests (abandoned far below operational), each spawner's mobs and danger floor (its vanilla mob spawner NBT); the style's lit lamps at the layout's light marks; block light 8 or more on every open floor cell of every section when operational; the cost per window; flooded pump room; collapses with rubble; holes and rubble; every chest filled from its table; every machine the one the picker chose; no armed or filled warhead block; the levels order as D36 says) and `icbm.complex.guards` (see silo-guard-spawner.md).

## Open questions

The level weights are the config `icbm.siloComplexActivityWeights`.

Weights and rates are tuned in I7.
