# Notes: Medium House (`house_medium`)

- **Purpose:** word-only brief for the clean-room layout builder. Write `assets/gregguns/structures/house_medium.txt` in the [layout format](../../../gregguns-spec/11-worldgen/layout-format.md).
- **Sources:** [structure-catalog A3](../structure-catalog.md#a3-medium-house-housemedium), [structure-loot](../../../gregguns-spec/11-worldgen/structure-loot.md), plan M8 section 2.

## Footprint and orientation

- `size: 16 12 16`. The declared and built sizes agree here.
- **Front is south (z = 15).** A 3-wide entrance path runs from the south edge (z 13 to 15) north to the front door, centered on the main wing (about x 5). The porch/garden is at the north-west corner, a secondary feature.
- `ground: 1`: layer 0 is the foundation/surface layer. Techguns also filled up to 4 blocks of support under the footprint after placing; the stamper does not, so make layer 0 a solid course and keep the house on that.

## Shape

An irregular two-story stone house with an attic, built from an L of two wings.

- **Main wing:** about 9 wide by 13 deep (x 1 to 9, z 1 to 13).
- **East wing:** about 6 by 5 (x 9 to 14, z 3 to 8), shorter, sharing the main wing's east wall.
- **Heights:** ground floor layers 1 to 4, first floor layers 5 to 8 (floor slab at 4 or 5), attic and gable layers 9 to 11. The east wing may stop one level lower than the main wing.
- **Walls:** stone-brick or cobble-look blocks, three looks mixed (a base course, a main course, a trim course). Glass-pane windows spaced about every three blocks on both floors; a few iron-bar windows in the ground floor of the east wing (a "reinforced" look).
- **Roof:** a steep gable of dark brick-red stairs (`@stairs`) with solid gable ends and a full-block ridge. The east wing gets a lower gable crossing the main one.
- **Entry:** wooden front door at the south wall of the main wing (`@door`, both halves). A back door at the north end. Two interior doors dividing ground-floor rooms.
- **Porch and garden (north-west):** a small cobble-tile terrace with a fence rail and a few lamp posts or plants; keep it flat and easy to remove if footprint conflicts.

## Rooms (approximate)

- **Ground floor:** an entry hall behind the front door, a living room in the main wing (bookshelves along a wall, carpets, a wall sign without text), a kitchen/dining area with a fence-and-pressure-plate table, and in the east wing a "metal storage" nook: five blocks of different metal (lead, copper, tin, iron, gold looks) stacked in a corner. Some chests here.
- **First floor (layers 5 to 8):** a staircase from the hall (oak or dark-oak stairs, `@stairs`) leads to two bedrooms (each with a bed, a crafting table, a chest and a lamp) and a landing. Beds are two cells each; keep them as matched foot and head pairs with `@facing`-style rotation (check, see the notes below).
- **Attic (layers 9 to 10):** reached by a vanilla ladder (`@facing`) from the landing. It holds the reinforced and weapon chests in a cluster under the ridge.
- **Lighting:** lamps in every room, at wall height.

## Chests and spawners

Totals: 8 vanilla, 6 reinforced, 4 weapon. Positions are approximate; the sheet gives counts only for the floors named.

| Mark | Count | Floor / place | Category | Ceiling |
|---|---|---|---|---|
| `chest.vanilla` | 4 | ground floor: living room, kitchen, hall, east-wing storage | `gregguns:weaponchest_t1` | LV |
| `chest.vanilla` | 4 | first floor: bedrooms and landing | `gregguns:weaponchest_t1` | LV |
| `chest.reinforced` | 2 | first floor, one per bedroom | `gregguns:reinforcedchest_t1` | LV |
| `chest.reinforced` | 4 | attic | `gregguns:reinforcedchest_t1` | LV |
| `chest.weapon` | 2 | first floor, in the master bedroom | `gregguns:weaponchest_t1` | LV |
| `chest.weapon` | 2 | attic | `gregguns:weaponchest_t1` | LV |

Chests face into the room (`@facing`). Reinforced and weapon chests are GregGuns chest blocks; the generator handles the block type from the mark tag. No spawners, no flag.

## Palette roles

| Role | `PaletteKeys` key | Vanilla fallback | Where |
|---|---|---|---|
| Base course | `brick` | stone brick | bottom two layers |
| Main walls | `concrete` weight 2, `brick` weight 1 | stone brick | walls |
| Trim | `iron_plate` | iron block | corner posts, door frames (sparingly) |
| Floor boards | `minecraft:planks#5` | same | dark-oak plank floors |
| Roof | `minecraft:nether_brick_stairs@stairs` | same | gables |
| Roof fill, gable ends | `minecraft:nether_brick` | same | ridge, gable ends |
| Windows | `glass_pane`; `minecraft:iron_bars` | glass pane | walls |
| Metal storage corner | `lead_block`, `copper_block`, `tin_block`, `iron_plate`, `gold_plate` | per key | east-wing nook |
| Lamps | `lantern` | glowstone | rooms |
| Doors | `minecraft:wooden_door` with `@door`, both halves | same | front, back, interior |
| Stairs inside | `minecraft:oak_stairs@stairs`, `minecraft:dark_oak_stairs@stairs` | same | staircase |
| Furniture | `minecraft:bookshelf`, `minecraft:crafting_table`, `minecraft:fence`, `minecraft:wooden_pressure_plate`, `minecraft:carpet`, `minecraft:bed` | same | rooms |
| Ladder, sign | `minecraft:ladder@facing`, `minecraft:wall_sign@facing` | same | attic ladder, living-room sign |
| Air | `air` | air | hollow rooms (large share of the grid) |

Avoid raw stone and cobblestone for exterior walls (open question Q4).

## Rotation and known bugs

- Beds: `@facing` for the foot and head pair; verify the pair at all four rotations so heads do not split from feet.
- Stairs and doors as above; use `@stairs` and `@door`.
- WGEN-05: footprint 16 by 16 is square, so rotated checks are harmless. WGEN-24: all materials as palette keys. WGEN-25: chests are filled by the generator from the seeded draw; the layout only places marks.
- Open question for the builder: where the front lands after rotation is not a content concern; keep everything authored facing south.

## Confidence

Good: footprint, overall plan, window and roof style, chest totals, ladder to attic. Soft: exact room partition, which room holds which chest (sheet gives floor-level hints only), and the east wing's height. Designer-level choices are acceptable; report them.
