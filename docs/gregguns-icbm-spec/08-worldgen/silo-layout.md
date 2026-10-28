# Silo complex layout (D128)

Status: first cut (room-marks model); the surface, terrain and placement sections follow in the next commits.

## Shape

All coordinates are complex-local: x and z in [0, 240), y 0 at the level floor (floor y 0, air 1 to 5, ceiling 6). One flat underground level.

- **Assembly bay (hall):** 36 x 28, x 102 to 138, z 106 to 134.
- **Wings:** four (N, E, S, W), each a 5-wide corridor 90 blocks long off the hall. A guard post annex (6 x 6, low side) sits at every wing mouth; one randomly picked wing also has a decontamination airlock (8 x 8, high side).
- **Room slots:** 5 per side per wing (40 total), pitch 16, first slot 10 blocks from the hall. 60 to 85% are used. Every mandatory kind is always present (launch control, comms, commander's office, warhead magazine, generator hall, armory); the rest are drawn from the optional kinds.
- **Silos:** 1 (78%), 2 (15%), 3 (5%), 4 (2%), in a cluster in the north-east quadrant, 16 x 16 reservations on a 2 x 2 grid, joined to the hall by a stub, a run and a trunk tunnel. Each has a seeded surface variant (open, half open, closed) and a missing part (controller or Energy Hatch).
- **Surface:** silo shafts, 0 to 2 radar stations (south-west quadrant), one 1 x 1 escape ladder at the end of a random wing.

## Marks (the seam with activity, guards and loot)

`com.gregguns.icbm.complex`: `ComplexLayout.generate(worldSeed, cellX, cellZ)` returns a `ComplexPlan` of `Room`s, `Tunnel`s and `SiloSlot`s. Each room and tunnel carries `Mark`s (`MarkKind`, x, y, z, key): loot chest (loot-table key from `LootKeys`), spawner (key `post`, `room` or `deep`), machine (`MachineRole`), item-frame wall, light, damage cell (`wall`, `floor`, `roof`), flood cell, rubble, sign, blast door. The plan is pure geometry and deterministic; it never changes by activity level.
