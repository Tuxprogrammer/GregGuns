# Silo complex layout (D128)

Status: layout, stamp and surface features built (I6 coder A). Placement and planets: [planets-and-placement](planets-and-placement.md).

## Shape

All coordinates are complex-local: x and z in [0, 240), y 0 at the level floor (floor y 0, air 1 to 5, ceiling 6). One flat underground level.

- **Assembly bay (hall):** 36 x 28, x 102 to 138, z 106 to 134.
- **Wings:** four (N, E, S, W), each a 5-wide corridor 90 blocks long off the hall. A guard post annex (6 x 6, low side) sits at every wing mouth; one randomly picked wing also has a decontamination airlock (8 x 8, high side).
- **Room slots:** 5 per side per wing (40 total), pitch 16, first slot 10 blocks from the hall. 60 to 85% are used. Every mandatory kind is always present (launch control, comms, commander's office, warhead magazine, generator hall, armory); the rest are drawn from the optional kinds.
- **Silos:** 1 (78%), 2 (15%), 3 (5%), 4 (2%), in a cluster in the north-east quadrant, real Missile Silo structures (5 x 5 x 12, the controller in the north face) on a 2 x 2 grid, joined to the hall by a stub, a trunk and one row corridor per silo row along the controllers' faces. Each has a seeded surface variant (open, half open, closed) and a missing part (controller or Energy Hatch).
- **Surface:** silo shafts, 0 to 2 radar stations (south-west quadrant), one 1 x 1 escape ladder at the end of a random wing.

## Marks (the seam with activity, guards and loot)

`com.gregguns.icbm.complex`: `ComplexLayout.generate(worldSeed, cellX, cellZ)` returns a `ComplexPlan` of `Room`s, `Tunnel`s and `SiloSlot`s. Each room and tunnel carries `Mark`s (`MarkKind`, x, y, z, key): loot chest (loot-table key from `LootKeys`), spawner (key `post`, `room` or `deep`), machine (`MachineRole`), item-frame wall, light, damage cell (`wall`, `floor`, `roof`), flood cell, rubble, sign, blast door. The plan is pure geometry and deterministic; it never changes by activity level.

## Depth, stamp and surface

- **Level:** the floor is 24 blocks under the recorded ground; rooms are floor + 5 air cells + ceiling (7 layers). Walls are GT Light and Dark Concrete bricks.
- **Doors:** every room opens onto its wing corridor (2 wide, 3 high); each wing opens into the hall; the stub opens into the hall's east wall.
- **Tunnels:** a vanilla rail along each wing's center line. Lamps, signs and blast doors are marks.
- **Silo:** the `SiloShape` piece cell by cell: concrete `C`/`F`, clear glass `G`, Gantry Beam `B`, Galacticraft pad `P` in the bore floor, the 3 x 3 bore open. The controller and hatches (input bus and maintenance beside it, energy and input HV below) are real GT machines. Exactly one part is missing: the controller (a concrete block in its place) or the Energy Hatch (a glass pane). A walkway cell in front of the controller opens onto the row corridor.
- **Shaft:** above the silo, a 5 x 5 concrete ring around the bore up to the ground height read in the shaft's window. Mouth variants: open, half open (west half of the bore capped), closed (all capped, two of the eight outer cells stay air to build height).
- **Escape:** a 1 x 1 ladder from the end of a wing to a closed trapdoor at ground level.
- **Not built yet:** radar stations on the surface (the plan carries their positions); item-frame walls need a facing; the silo guard "deep" spawner marks.
