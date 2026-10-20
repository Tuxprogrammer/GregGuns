# Notes: Military Base parts (`camp_parts`)

- **Purpose:** word-only brief for the clean-room builder of the Military Base. One file, nine part sections plus the base's overall rules. These are rules, not scans: the base and all its parts are procedural in Techguns. The builder decides, for each part, whether to write a small set of fixed-size layouts in the [layout format](../../../gregguns-spec/11-worldgen/layout-format.md) (recommended: two or three sizes per part, picked by leaf size) or a short generator routine. Either way the rules below are the contract.
- **Sources:** [structure-catalog A6 and Part B](../structure-catalog.md#a6-military-base-militarybasestructure--militarycamp), [structure-loot](../../../gregguns-spec/11-worldgen/structure-loot.md), plan M8 sections 1 (G10) and 2, and Q12 (no NPCs at generation).

## Base rules

### Size and terrain

- **Footprint:** a flat rectangle, 32 to 79 blocks on each axis, picked independently. The base is axis-aligned and does not rotate as a whole; parts inside it face roads.
- **Ground:** the area is cleared of trees and junk, flattened (no more than 4 blocks of height difference allowed), and smoothed at the edge so it blends into the terrain. Never drain water (WGEN-26) and never count tree trunks as ground (WGEN-33). Abort if either side ends below 5.
- **Fence:** two layers of iron bars around the whole perimeter, each column at its own ground height. Gates are gaps opened by the roads.

### Subdivision (BSP)

- The usable area is the footprint inset by 2. It splits along its longer axis into 2 parts at the root, and later splits into 2 or sometimes 3 or more parts. Parts get random relative weights between 1 and 2; a road lane of the parent's width separates neighbors.
- **When to stop:** a part smaller than the minimum never splits. A part between the minimum and maximum splits less and less likely the smaller it is, and parts near the center of the base split more than parts at the edge. Minimum part side is 5 when the base is under 10 wide in its short side, otherwise 7. Maximum is 15 for bases up to 45 wide, otherwise a third of the short side.
- **Roads:** width is the longer side divided by 8, clamped between 1 and 5 at the root, minus one per subdivision level, never below 1. A road is flat, gravel at the edges and concrete down the middle when 4 or wider. Root-level roads extend 3 blocks past the fence and open the fence where they cross it.
- **Facing:** each part's entrance faces the road beside it. Of the children of a split, the non-last children face one way and the last child faces the other, so buildings face the road between them.
- **Spawn points:** each road center adds a standing spot, one block above ground, for the flag's NPCs.

### Choosing a part for a leaf

- A leaf touching two outer edges (within 2 blocks) rolls for a **corner** part with chance `((sx + sz) / 2 - 20) / 40`, so bigger bases get towers more often. Corner part: the watch tower (section 7).
- Otherwise a leaf touching an outer edge rolls a **border** part with chance `(side - 30) / 50` for that axis. Border parts: small bunker types, sandbag ring or row, sandbag shelter.
- Otherwise a **regular** part is picked uniformly among those that fit the leaf's size. If none fits, the leaf gets a single flat layer of cobblestone and nothing else.
- Parts may swap their X and Z extents to fit a leaf. The leaf is flattened to 0 difference before the part is built.

### Flag

- The first central leaf reached becomes the **flag leaf**. It is flattened and holds one camp flag at its center, one block above ground. It gets no other building.
- Declare it as the `flag` mark. NPC type is `ArmySoldier` only. The NPC cap is `floor(((sx + sz) / 2) * 0.75)`, so 24 for a 32 by 32 base and 59 for 79 by 79, saved with the flag. Spawn points are the road centers.
- Breaking the flag summons the attack helicopter 50 blocks above it, only if flagged for it. The base itself spawns no NPC while generating.
- Fix WGEN-12: if the flag block fails to place, skip the tile setup instead of crashing.

### Camo by biome

- Camo nets (tent walls, roofs) come in three color types: woodland (default), snow, and desert. Pick from the base center's biome by BiomeDictionary type, not the vanilla biome list (WGEN-32): snowy types get snow, sandy or desert types get desert, everything else (including modded biomes with no types) gets woodland. One color for the whole base.

### Crates at 5% per column

- Crate piles and closed-bunker interiors scatter containers. For each floor column, one roll: 5 percent a reinforced chest, the next 5 percent a weapon chest, and the other 90 percent either empty floor or a crate (a plain decorative box, 50 percent when something solid is below). So about one column in ten holds a chest. Chests face a random side.
- Chest categories: reinforced `gregguns:reinforcedchest_t2` and weapon `gregguns:weaponchest_t2` at MV. Rolls use the structure's seeded draw (WGEN-25), never an unseeded one.
- A part with `chest.*` marks cannot use per-column rolls inside a fixed layout. A fixed layout should mark a few chest spots at about that density instead, or the part should be a routine.

## Part palette roles (shared)

| Role | `PaletteKeys` key | Vanilla fallback |
|---|---|---|
| Concrete (several shades, called light, dark and floor) | `concrete` | stone brick |
| Plating, steel | `factory_wall` | iron block |
| Hazard stripe | `hazard` | yellow hardened clay |
| Lamps | `lantern` | glowstone |
| Wood panels, floors, roof | `minecraft:planks` | same |
| Wood pillars | `minecraft:log@axis` | same |
| Sandbags | no key yet (future decor block); stand-in `minecraft:stained_hardened_clay#12` | same |
| Camo net wall and roof | no key yet; stand-in `minecraft:leaves#4` (woodland), `minecraft:snow` (snow), `minecraft:sandstone` (desert) | same |
| Shipping container | no key yet; stand-in `minecraft:stained_hardened_clay#14` | same |
| Crates | no key yet; stand-in `minecraft:planks#1` | same |
| Gravel | `minecraft:gravel` | same |
| Air | `air` | air |

Doors use `@door` on both halves. Stairs use `@stairs`. Ladders and chests use `@facing`. Fix WGEN-31 by never hand-rotating these.

## 1. Props (camp props)

- **Sizes:** six variants, from 1 by 1 up to 10 by 10, 2 to 7 high. Each part is one of:
  - **Crate pile** (variants with crates): gravel floor, a loose heap of crates 1 to 2 high with lamp on top of one pile at probability `((sx + sz) / 2) / 6`. Chests at the 5 percent rule above.
  - **Roofed pile:** the crate pile, inset by 1, under a camo-net roof held up by four fence-post corners. Roof sits 1 block under the part's top.
  - **Sandbag ring:** a sandbag wall one block high around the perimeter with a 1-block gap in the middle of the side facing the road.
  - **Sandbag row:** a stepped wall along the longer axis, centered; each level up is two blocks shorter, giving a pyramid profile.
  - **Roofed sandbag ring:** the ring plus the roof.
  - **Sandbag ring with roofed pile inside:** the biggest, 10 by 10 at most.
- **Loot:** crate piles only: the 5 percent rule. MV.
- **Where:** crate piles and the big one are regular parts; rings and rows are border parts.

## 2. Containers

- **Sizes:** 2 to 7 by 2 to 7, exactly 3 high.
- **Shape:** parallel rows of solid shipping containers, each 2 wide and 2 tall, starting one block above ground and running the full length of the longer side. One empty lane (1 block) between rows. Row count is `floor((short side + 1) / 3)`; a leftover strip narrower than 3 stays empty. Rows are solid blocks: no door, no interior.
- **Loot, spawners, flag:** none.
- **Orientation:** none (no rotation); rows run along the longer axis, along z when equal.
- WGEN-35: Techguns stored a minimum row length and never read it. Just run full length.

## 3. Closed bunker

- **Sizes:** 3 to 15 by 5 to 15 for a base leaf (up to 10 by 10 on borders), 3 high. May swap axes. There are two closed types: plain, and one with a sandbag parapet on the roof.
- **Shape (rules, from bottom):**
  - A concrete floor one block sunk under the surface.
  - A wall ring one layer high at ground level, interior air.
  - A second layer: a solid ring (closed types).
  - A roof layer: wall ring plus slab roof.
  - Plain: nothing more. Parapet type: a sandbag ring on top of the roof. In that type a ladder in the inner corner away from the door climbs to a hatch in the roof.
- **Entrance:** a door in the middle of the side facing the road. Concrete jambs both sides, a short outward passage two blocks long, and a step up to ground. Two lamps on the jamb tops outside; one lamp beside the door inside.
- **Interior:** inset by 2, filled by the 5 percent crate rule: chests and crates at floor level, a second crate layer 50 percent of the time when the cell below is not air. Thin leaves (under 5 wide) turn the entrance by 90 degrees.
- **Loot:** reinforced and weapon chests only, from the 5 percent rule. MV.
- **Where:** regular, and (smaller) border parts.

## 4. Barracks

- **Sizes:** minimum 7 by 7, no maximum, exactly 7 high. After 2 blocks are taken by the entrance ramp, each side snaps down to `4k + 1` (9, 13, 17, ...).
- **Shape (rules):**
  - Gravel ground, then wood pillars every 4th cell on stilts one block high.
  - A wood floor with a scaffold-style border.
  - Walls two high with a post every 4th cell and panels between; glass windows on the upper wall row where the grid hits the middle.
  - A ring beam, then a gabled or flat wood roof with a slab edge.
- **Entrance:** a door in the middle of the road-facing wall, a lamp above it, two oak stairs forming a ramp down to the ground.
- **Interior:**
  - Chests with the barracks table (vanilla chests) along the inner wall ring at every 4th cell, skipping the door row.
  - Bunk beds in the second ring where the grid hits (if the building is longer than 4 in either axis), each bed's foot against its wall.
  - Ceiling lamps on an 8-block grid, and wall lamps on the inner ring.
- **Loot:** vanilla chests only, category `gregguns:barracks` (see pack-loot): combat armor, gas mask, night vision; no glider or oxygen gear. Count 1 to 4 per chest. MV.
- **Where:** regular.

## 5. Helipad

- **Sizes:** 7 to 10 square, any height; the pad itself is 7 by 7, one block above ground. In an exact 9 by 9 area, add a thin wire-frame border one block outside the pad at ground level, with no corners.
- **Shape:** a hazard-stripe border ring, a dark-concrete fill, and a letter H made of glowing blocks (three-wide strokes on both sides joined by a one-block bar), centered.
- **Loot, NPCs, spawners:** none.
- **Where:** regular.

## 6. Tank farm

- **Sizes:** 4 to 10 by 4 to 10, at least 5 high.
- **Shape:** horizontal fuel tanks, each 4 wide and as long as the long side, repeated every 5 blocks across the short side. Per tank, from the ground up: a scaffold cradle and corner supports, a belly of plating with hazard end caps, two layers of tank wall with hazard corners and a sealed 2-wide lava core, then a wall top with hazard end caps. Total height 5 above ground.
- **Danger:** the lava is real source lava sealed on all sides; never leave it open.
- **Loot, NPCs:** none.
- **Where:** regular.

## 7. Tower (watch tower)

- **Sizes:** two. Size 3: 3 by 3 footprint, 8 high. Size 4: 4 by 4 footprint, 10 to 12 high. Centered in the corner leaf.
- **Shape:**
  - Fence-post corner supports from ground to just under the platform.
  - A solid core of planks (1 by 1 for size 3, 2 by 2 for size 4) up to the platform.
  - A two-high hollow fence ring around the platform.
  - A camo-net roof (woodland color always) at the top.
  - A ladder on the road-facing side from ground to the platform, with a fence gate at its top.
- **Use:** corner leaves only (rule above). Four of these frame a base, since a leaf only touches two edges at a corner.
- **Loot, NPCs:** none.

## 8. Small bunker (open types)

- **Sizes:** 5 by 5 to 8 by 8, 3 or 4 high. Used for regular and border leaves.
- **Shape:** same build as the closed bunker, except the second layer has firing slits: wall only at corners and every 3rd cell, so it is see-through. Border version adds a roof sandbag parapet and a ladder to the roof. Roof is a slab layer.
- **Entrance:** as the closed bunker, with the single interior lamp at the center instead of beside the door.
- **Interior:** empty. No chests, no crates.
- **Loot:** none.
- **Where:** regular and border.

## 9. Factory (small factory building)

- **Sizes:** 9 to 16 by 6 to 16, 6 high. May swap axes.
- **Shape and loot:** this is the same building as the standalone [Small Factory Building](factory_small.md). Reuse that layout inside the base, stamped at the leaf's origin with the leaf's rotation, rather than writing a second one. Its loot ceiling inside a base stays LV (the standalone ceiling).
- **Where:** regular.

## Other part types in the leaf pool (not separate sections)

- **Tents:** three types, 3 to 8 square, 4 to 6 high; gravel floor, camo-net walls with a doorway or open pavilion pattern, camo-net roof. These carry no loot.
- **Empty plane:** the cobblestone fallback.

## Rotation and known bugs

- The base has no overall rotation. Parts rotate to face roads, quarter turns only. Use `@kind` on every directional block.
- WGEN-12: null-check the flag tile. WGEN-04: the base is bigger than one populate window, so the generator writes only inside the current window. WGEN-15 and WGEN-16: terrain checks must sample the far edge and use a correct median. WGEN-26, 27, 32 and 33: as noted above. WGEN-24 palette keys only; WGEN-25 seeded rolls.
- Fixed layouts for the parts must follow the exact-row rule (WGEN-23).

## Confidence

Good: base size range, BSP rules and constants, road widths, corner and border chances, the flag and its cap, the 5 percent crate rule, camo types. Soft: the exact heights chosen for each part, the barracks roof shape, the stand-in blocks, how many fixed-size layouts to author per part, and the border/regular split of the open small bunker.
