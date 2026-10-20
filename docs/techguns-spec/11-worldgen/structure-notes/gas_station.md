# Notes: Gas Station (`gas_station`)

- **Purpose:** design brief for the clean-room layout builder. This structure is **new work**: Techguns never registered it (the dormant city version, sheet C2, is the only reference). Write `assets/gregguns/structures/gas_station.txt` in the [layout format](../../../gregguns-spec/11-worldgen/layout-format.md).
- **Sources:** [structure-catalog C2](../structure-catalog.md#c2-city-gas-station-citygasstation), [index open question](../index.md), [structure-loot](../../../gregguns-spec/11-worldgen/structure-loot.md), plan M8 section 2 (SMALL pool, 1/3 pick).

## Footprint and orientation

- `size: 13 7 16` (x 13, y 7, z 16). The sheet gives 13 by 6 by 16; one extra layer is allowed for a low sign pole.
- **Front is south (z = 15)**, where cars would pull up. The pump canopy is at the front, the shop at the back (north). The shop's main door faces south onto the pumps; a staff door at the north-west corner of the shop leads out the back.
- `ground: 1`: layer 0 is the paved apron at surface level.

## Shape

- **Apron:** the whole footprint is a paved forecourt in a dark concrete look, with stone-brick-look paving panels (a border and a few inset slabs). Keep one-block margins unpaved with `.` so it blends into terrain.
- **Shop:** 11 wide by 6 deep (x 1 to 11, z 3 to 8), walls 3 blocks high, flat roof at layer 4. Brick-look walls. A row of glass panes along the south wall either side of the door (the "storefront"), a few panes on the east and west walls, none at the back.
- **Inside the shop:**
  - A counter of fence posts topped with pressure plates, running parallel to the back wall, with a fence-gate gap in it.
  - Shelves along the back and one side wall (bookshelf blocks; the shop is the one place bookshelves belong).
  - Two chests in the back storeroom area (north-east), against the north wall.
  - Two to three lamps.
- **Canopy over the pumps (z 9 to 15):** a flat roof slab at layer 5, wider than the pumps, held by two stone-brick-look pillars at the south corners (layers 1 to 4). The shop roof at layer 4 steps up to the canopy at layer 5 with a one-block lip.
- **Pumps:** two pumps on the forecourt about two-thirds of the way down (z 13), one third and two thirds across (about x 4 and x 8). Each pump is two blocks tall: a hazard-striped lower block and a box-like upper block, with a lever on the front face as the nozzle. Levers use `@facing` where needed.
- **Sign:** an optional short sign post (fence post and a plate of iron bars or a wall sign without text) at the south-east corner of the canopy; skip if it complicates anything.
- **Lighting:** two canopy lamps hanging below the slab above the pumps, plus a lamp at each shop door.

## Doors (WGEN-31, the bug not to reproduce)

- Techguns' doors were broken because the lower halves had no rotation kind and their metadata did not pair with the upper halves.
- Here each door is two cells, lower and upper, both with `@door`, and metadata chosen as a matched pair (same facing and hinge; upper half has the top bit set). Check by reading the rotated results at all four rotations: the storefront door must still open outward onto the pumps and the back door onto the back lot.

## Chests and spawners

| Mark | Position (approx.) | Category | Ceiling |
|---|---|---|---|
| `chest.vanilla` | shop storeroom, x 10, z 4 | `gregguns:weaponchest_t1` | LV |
| `chest.vanilla` | shop storeroom, x 9, z 4 | `gregguns:weaponchest_t1` | LV |

- No spawners, no flag.

## Palette roles

| Role | `PaletteKeys` key | Vanilla fallback | Where |
|---|---|---|---|
| Apron | `concrete` | stone brick | forecourt |
| Paving panels, pillars | `brick` | stone brick | inset panels, canopy pillars |
| Shop walls | `brick` weight 3, `concrete` weight 1 | stone brick | shop |
| Canopy and shop roof | `plating` | iron block | canopy slab, shop roof |
| Storefront glass | `glass_pane` | glass pane | windows |
| Pump body | `hazard` (lower), `factory_wall` (upper) | stained clay / iron block | two pumps |
| Lamps | `lantern` | glowstone | canopy, doors |
| Shelves | `minecraft:bookshelf` | same | shop walls |
| Counter | `minecraft:fence`, `minecraft:wooden_pressure_plate`, `minecraft:fence_gate@facing` | same | counter |
| Doors | `minecraft:wooden_door` with `@door`, both halves | same | south and back doors |
| Pump nozzle | `minecraft:lever@facing` | same | pump face |

Avoid raw stone and cobblestone for walls (open question Q4).

## Rotation and known bugs

- Doors: see above (WGEN-31).
- Fence gate, levers, chests: `@facing`.
- Size from this file only; there is no declared-size mismatch to copy (WGEN-30).
- All materials through palette keys (WGEN-24).

## Confidence

This is a design brief, not a copy: the sheet fixes the overall size, shop footprint (11 by 6), canopy depth (z 9 to 15), two pumps at z 13, two chests and the palette feel. Pump x positions, chest spots and the sign are design choices; the builder may adjust and report.
