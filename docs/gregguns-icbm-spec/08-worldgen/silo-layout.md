# Silo complex layout (D128)

Status: layout, stamp, surface features, armory frames, deep guard posts and radar stations built (I6 coder A and finisher). Placement and planets: [planets-and-placement](planets-and-placement.md).

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

- **Level:** the floor is 24 blocks under the recorded ground; rooms are floor + 5 air cells + ceiling (7 layers). Walls, rooms and tunnels are GregGuns Bastion Plating (steel), which joins into one plated mass through the connected-texture system (design record 0023). The silo and its shaft stay GT Light and Dark Concrete, the silo's own accepted finish; no connected-texture rule is ever shipped for a `gregtech:*` block.
- **Doors:** every room opens onto its wing corridor (2 wide, 3 high); each wing opens into the hall; the stub opens into the hall's east wall.
- **Tunnels:** a vanilla rail along each wing's center line. Lamps and signs are real; blast doors are marks that write nothing yet, and so are cable trays and pipes (I7, D213).
- **Signs:** a SIGN mark becomes a wall sign on the wall beside its cell, its lines from the `icbm.complex.sign.<room kind>` lang key (up to four lines divided by `|`); the central hall's sign reads `icbm.complex.sign.hall` with the rolled tier name (D212).
- **NASA Workbench:** written with Galacticraft's dummy blocks in its full footprint, each tied to the main block (D214).
- **Flooded pump room:** the water of a flood mark sits one cell down, in the floor, so it cannot flow out (D214).
- **Silo:** the `SiloShape` piece cell by cell: concrete `C`/`F`, clear glass `G`, Gantry Beam `B`, Galacticraft pad `P` in the bore floor, the 3 x 3 bore open. The controller and hatches (input bus and maintenance beside it, energy and input HV below) are real GT machines. Exactly one part is missing: the controller (a concrete block in its place) or the Energy Hatch (a glass pane). A walkway cell in front of the controller opens onto the row corridor.
- **Shaft:** above the silo, a 5 x 5 concrete ring around the bore up to the ground height read in the shaft's window. Mouth variants: open, half open (west half of the bore capped), closed (all capped, two of the eight outer cells stay air to build height).
- **Escape:** a 1 x 1 ladder from the end of a wing to a closed trapdoor at ground level. The ladder's metadata (`ComplexPlan.escapeMeta`) makes it cling to the wing's end wall behind it (N 3, S 2, E 4, W 5).
- **`landY`:** `HeightProbe.landY` is the **top solid block** (foliage, wood and ice skipped), not the air above it. So the trapdoor replaces that block and is a closed top-half trapdoor (metadata 8): its top face is at landY + 1, level with the terrain around it. A shaft ring is written up to and including landY, so its top face meets the terrain, and an open bore is air from the silo up through landY. `icbm.complex.dim.*` check both on real terrain.
- **Radar stations:** 0 to 2, in the south-west quadrant, each a 5 x 5 hut of Works Casing walls three high (a two-high doorway on the south side, a Gantry Casing roof) with an iron-bar mast of seven on the roof. The hut is built off the recorded ground height, so it stays level when it spans windows (the foundation fills below, a bump above is cleared; a water column is left alone). The dish is a `RadarDishHook`; the shipped one, `PLACEHOLDER_RADAR_DISH`, is a redstone block on the mast top. **Placeholder:** the radar multiblock rework (D125, D190) is not on this base; the orchestrator replaces the hook with the real dish or station controller when it lands.
- **Armory frames:** the `ITEM_FRAME_WALL` mark is the first frame cell (the room cell in front of the wall) and carries `facing` (Minecraft's hanging direction: 0 south, 1 west, 2 north, 3 east) and `extent` (frame count, one every second cell along +X or +Z). The armory marks two rows (y 2 and 3) on the wall opposite the doorway. The world side creates each frame at its wall block (that is what `EntityItemFrame`'s constructor takes), puts a gun from the weapon tables up to the ceiling in it (`SiloFrames`), and the damage pass never breaks the wall behind a frame.
- **Deep guard posts:** each silo's walkway in the row corridor and the trunk's far end carry a `deep` spawner mark (D36); the contents pass makes them elite posts.
