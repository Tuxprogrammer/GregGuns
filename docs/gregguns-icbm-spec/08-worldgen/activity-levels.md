# Activity levels and the damage pass

- **Baseline:** none (the ICBM baseline has no worldgen).
- **Decisions:** [D36](../DECISIONS.md#d36-in-detail), [D128](../DECISIONS.md), [D17](../DECISIONS.md#d17-in-detail), D180 to D186, D229, D231
- **Pack mods:** none beyond the layout's (GregTech, Galacticraft)
- **Roadmap:** [I6](../../ROADMAP-ICBM.md#i6--world-missile-silos-on-every-planet)

## What changes from the ICBM baseline

Nothing exists in the baseline. D36 gives each complex an activity level, rolled once from the complex's seed: **abandoned** 50%, **partly abandoned** 35%, **operational** 15% (config). The layout ([silo-layout](silo-layout.md)) is the same at every level. The level only decides which marks become what: `ComplexContents.build(plan, level, ceiling, other)` turns the layout's marks into `Placement`s.

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
| Machine is a dead husk | 35% | 45% | 0% |
| Pump room flooded | yes | yes | no |
| Fallout rubble in the magazine | all marks | 40% | none |
| Guard spawner chance by depth band 0, 1, 2, 3 | 0 | 10, 20, 45, 65% | 50, 75, 100, 100% |
| Chest richness (`LootDraw`) | 0 | 1 | 2 |

Depth band: 0 is the hub, then one band per 30 blocks of distance from it, up to 3. Rooms and tunnels are judged by their box. A guard post gets +25% when the level is not abandoned, and the launch control room and every "deep" mark use at least the shaft chance (100% operational). Collapses hit rooms only, never the hub or a hall (the silo gallery among them), so the complex stays walkable. Guard posts, the decontamination airlock and Launch Control never collapse at any level; Launch Control's lamps, rubble and breaches still follow the level (D252). A collapsed room turns its floor and wall damage cells to rubble, opens its roof cells, and the world side fills part of its box with rubble.

**Lamps die by circuit, not one by one (D231).** A lamp's mark carries its section and its run (one line of the section's lamp grid, ceiling lamps and the floor light tiles under them together). An abandoned complex rolls once per run, so a few runs still burn and most are dead. A partly abandoned complex rolls once per section, so whole rooms and halls are dark or lit. An operational complex lights everything. The share lit stays the D36 rate. A dead lamp stays in place as the style's dark lamp, flush in the ceiling or floor.

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
2. GGI-08-21: an abandoned complex has no spawner placements.
3. GGI-08-22: damage, collapse and spawner counts fall (damage) and rise (guards) from abandoned to operational.
4. GGI-08-23: the same plan, level, ceiling and `other` always give the same placements.
5. GGI-08-24: the pump room floods unless operational.
6. GGI-08-25: no placement reads or writes outside its own cell or box.

## Tests

Unit: `ActivityLevelTest`, `ComplexContentsTest` (lamps by run and by section at the D36 shares). Scenarios: `icbm.complex.levels` (each level, plus Operational at UMV, generated onto a slab one window at a time and read back: spawner count, each spawner's mobs and danger floor (its vanilla mob spawner NBT); the style's lit lamps at the layout's light marks; block light 8 or more on every open floor cell of every section when operational; the cost per window; flooded pump room; collapses with rubble; holes and rubble; every chest filled from its table; every machine the one the picker chose; no armed or filled warhead block; the levels order as D36 says) and `icbm.complex.guards` (see silo-guard-spawner.md).

## Open questions

Guard chances scale with the ceiling: 1.0 at EV rising evenly to 1.5 at UMV, capped at certainty (D212). The level weights are the config `icbm.siloComplexActivityWeights`.

Weights and rates are tuned in I7.
