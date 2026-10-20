# Notes: Alien Bug Nest (`alien_bug_nest`)

- **Purpose:** word-only brief for the clean-room builder. The nest is procedural (a graph of caves carved underground), so there is no fixed grid and no layout file to write. The builder writes a generator routine from these rules, or, if a fixed layout is wanted, one authored sample chamber set. The block roles below are for the bug-nest blocks, which do not exist yet.
- **Sources:** [structure-catalog A5](../structure-catalog.md#a5-alien-bug-nest-alienbugneststructure--alienbugnest), [structure-loot](../../../gregguns-spec/11-worldgen/structure-loot.md) (GGS-11-24, no chests), plan M8 section 2 (G11 Bug Hole spawners), Q2 (disabled by default).

## Footprint and placement

- **Shape:** an underground cave system. About 60 by 60 blocks across and 20 to 30 deep, centered on the grid cell's anchor. Only the entrance shaft reaches the surface.
- **Where:** sandy or wasteland biomes only (and not river, ocean or beach). Default off in config.
- **Vertical:** the entrance mouth is at the ground surface. The main chamber is 10 to 16 blocks under it. Satellite chambers sit up to 10 blocks above or 5 below the main one.
- **Front/rotation:** none. It is rotation-symmetric; the random angle of the satellites is the only variation, and it is seeded from the structure variant.
- **Terrain check:** if the surface is not valid, do not build (WGEN-09 built the nest very low instead).
- **Window rule (WGEN-04):** the footprint spans several populate windows. Each window's write must be a function of the world position plus the seed, so any window can compute its share without the others.

## The segment graph (rules)

- **Entrance:** a vertical shaft at the center, radius 2. It is not carved as a ball.
- **Main chamber:** a ball of radius 5, a little off-center (up to 2 blocks in x and z), linked to the entrance.
- **Satellite chambers:** five of them, spaced evenly around the main chamber (every 72 degrees, with no set start angle). Each is a ball of radius 3 to 5 (smaller more often), at horizontal distance about 15 to 25 from the center.
- **Links:** satellite 0 links to the main chamber. Each later satellite links to the previous one half the time (and also to the main chamber half the time), otherwise to the main chamber only.
- **Child chambers:** each satellite has half a chance of one extra child ball, same size rule, a further 15 to 25 out, within 45 degrees of its parent's direction, linked to that parent.
- **Total chambers:** the main, 5 satellites and 0 to 5 children, so 5 to 10 chambers that hold Bug Holes.

## Carving order

1. **Shells and air:** for each chamber, fill a ball with the shell material first (one block thicker than the air), then carve air inside radius minus 1.
2. **Tunnels:** for each link, a round tunnel with a shell and an air core runs from parent to child. Radius is 2 to 3, never larger than either end. The tunnel to the surface is extended 5 blocks, so it breaks through to open air.
3. **Re-carve:** each chamber is cleared to air again so no tunnel shell sticks into a room.
4. **Slime on the shaft:** the entrance tunnel is lined with climbable trail blocks on every wall cell, so players can climb up and down.
5. **Slime pillar:** the main chamber gets a vertical column of nest blocks, running from just under the entrance shaft down to a bit past the chamber center, offset a block or so from the center, with trail blocks on all four sides, so the shaft and chamber connect by climbing.
6. **Eggs and holes:** in each satellite and child chamber (not the main one):
   - One Bug Hole spawner at a random floor column in the middle of the chamber floor.
   - Egg piles: for each floor column, start a pile with chance rising toward the center of the room. The pile climbs from the floor, thinning each layer, up to the chamber radius.

## Block roles

| Role | Planned block | Fallback until it exists | Where |
|---|---|---|---|
| Nest wall | `bugnest` (hard resin) | `minecraft:sandstone#2` smooth sandstone | chamber and tunnel shells (16 to 1 over egg and sandstone in chambers; 12 to 1 over sandstone in tunnels) |
| Egg | `bugnesteggs` (breakable egg cluster) | `minecraft:stained_hardened_clay#13` | egg piles, scattered in chamber walls (1 in 19) |
| Slime trail | `bugnestslimy` (climbable, wall-attached, `@facing`) | `minecraft:vine@facing` | shaft walls, around the slime pillar |
| Plain wall | `minecraft:sandstone` | same | the 1-in-19 flecks in chamber shells |
| Spawner | Bug Hole | direct block once G11 lands; mark `spawner.bug_hole` | one per satellite or child chamber |
| Air | `air` | air | chambers, tunnels |

The shell material is picked per cell by weight: mostly nest wall, rare egg, rarer sandstone. Use the position hash from the format, not a running random (WGEN-14).

## Chests, spawners, flag

- **Chests:** none (GGS-11-24). Test: the nest places zero chests.
- **Spawners:** 5 to 10 Bug Holes, one per satellite or child chamber. Each spawns the Alien Bug mob; caps 5 nearby, 3 per try, player range 24. That is G11's concern, not the generator's. The generator only places the mark or block.
- **Flag:** none, no camp NPCs.

## Rotation and known bugs

- WGEN-09: a failed terrain check must skip the nest.
- WGEN-04: window-safe writes (above).
- Slime trails need `@facing` so they stay attached after rotation (none here) and when placed from the graph.
- Builder stub: until bug-nest blocks land, run on fallbacks. The nest must still be climbable (vines) and the spawner mark must still be reported.

## Confidence

Good: chamber count, radii, depth, link rules, carving order, Bug Hole count, no chests. Soft: the exact angle start, how thick the tunnel shell is, the egg density curve, and the 60-by-60 span (it falls out of the distances, so it varies per seed).
