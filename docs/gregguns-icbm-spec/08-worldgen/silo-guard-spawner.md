# Silo guard spawner

- **Baseline:** none. Follows M8: [structure-placement](../../gregguns-spec/11-worldgen/structure-placement.md) (spawner marks), `BlockHoleSpawner`.
- **Decisions:** [D36](../DECISIONS.md#d36-in-detail), [D38](../DECISIONS.md#d38-in-detail), [D41](../DECISIONS.md), D180, D181
- **Roadmap:** [I6](../../ROADMAP-ICBM.md#i6--world-missile-silos-on-every-planet)

## What changes from M8

The two M8 holes (Zombie Hole, Bug Hole) are `HoleKind` constants. I6 adds two more, so the silo guards use the same block, tile, caps, range and light rules, with no new spawner code:

| Block | Name | NPCs | Where |
|---|---|---|---|
| `silo_guard` | Silo Guard Post | Army Soldier, Commando | `post` and `room` marks, and `room` marks nearer the hall |
| `silo_elite` | Silo Elite Post | Commando, Stormtrooper | `deep` marks, and 40% of `room` marks at depth band 2 or more |

Each attempt picks evenly among the registered kinds (`HoleKind.pick`). The block is Tiered EV (the Overworld ceiling). The layout tag is `spawner.silo_guard` or `spawner.silo_elite`.

## Rules (M8's, unchanged)

Acts only while a player is within 24 blocks and its own chunk is loaded; at most 5 of its kind near it (box 12 wide, 6 below, 8 above), at most 3 per try, every 5 seconds; never loads a chunk; marks each NPC before it joins the world (Infernal opt-out). **Breaking the block removes the tile, so a broken spawner stays broken.** Like a vanilla spawner it drops nothing and is hidden from the creative tab.

## Scaling

The tile stores the complex's ceiling. `HoleSpawning.attempt` raises each NPC's danger to at least `RolledCeiling.danger(ceiling)`, so equipment follows the loot: EV and IV 3, LuV and ZPM 4, UV and above 5 (`SiloCeiling.guardDanger`, tested equal). The count per level is in [activity-levels](activity-levels.md).

## Tooltip (D41)

`ItemBlockHoleSpawner`: tier line (`TierText`), one summary line, "Press [SHIFT] for detailed tooltip"; behind Shift the range, caps and "Break it and it stays broken", then "Added by: GregGuns". Layout is `HoleTooltipFormat`, unit tested. Lang keys `gregguns.hole_spawner.*`.

## Texture

The two blocks borrow existing tiles (`concrete_hole`, `metalpanel_hazard`). A dedicated texture needs the orchestrator's approval (D47: generation is a last resort).

## Requirements

1. GGI-08-40: `spawner.silo_guard` and `spawner.silo_elite` map to their kinds; the mobs are as the table says.
2. GGI-08-41: a placed post spawns guards when a player is near, and respects the cap.
3. GGI-08-42: breaking a post stops it for good.
4. GGI-08-43: guards start at the ceiling's danger.

## Tests

Unit: `HoleKindTest`, `HoleTooltipFormatTest`, `SiloCeilingTest`. Scenarios: `icbm.complex.guards` (both posts place their own mobs at the stored ceiling's danger within the cap; a guard targets a nearby fake player; a broken post spawns nothing more, also after its chunk is saved, unloaded and reloaded, while the intact post reloads with kind and ceiling; the intact post keeps spawning, so the check can tell the two apart). NEI: the posts are `HiddenFromCreativeTab`, which the NEI config hides.
