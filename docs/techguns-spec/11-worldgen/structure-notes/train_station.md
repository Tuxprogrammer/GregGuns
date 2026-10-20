# Notes: Small Train Station (`train_station`)

- **Purpose:** word-only brief for the clean-room layout builder. Write `assets/gregguns/structures/train_station.txt` in the [layout format](../../../gregguns-spec/11-worldgen/layout-format.md).
- **Sources:** [structure-catalog A2](../structure-catalog.md#a2-small-train-station-smalltrainstation), [structure-loot](../../../gregguns-spec/11-worldgen/structure-loot.md), plan M8 section 2.

## Footprint and orientation

- `size: 11 7 11`. Techguns declared 11 by 12 but built 11 by 11 (WGEN-30); use 11 by 11.
- **Front is west (x = 0), the track side.** Rails run north to south (along z) for the full 11 blocks. The station house stands east of the tracks and its door faces west onto the platform.
- `ground: 1`: layer 0 is the gravel/cobble bed at surface-block level.

## Shape

- **Track bed (x 0 to 4):** gravel and cobble with two rails, one along x = 0 and one along x = 3, each full length in z. Between and beside them is gravel with some cobblestone and mossy cobblestone (about 2 to 1). A one-wide platform strip (x 4) of the same bed lies against the house.
- **Rails are broken on purpose:** every rail cell is a weighted choice, rail 2 and air 1, so the line looks wrecked. Rails run straight north-south, `@none`.
- **Station house (x 5 to 9, z 1 to 7):** 5 wide, 7 deep. Brick walls (random mix of two brick looks), 3 blocks of height inside. Cobble-look tile floor. Windows are 50/50 glass pane or air, one or two per long wall and one on the east wall. The door is in the west wall at about z 4: a plain 1-wide, 2-high doorway (an open gap is fine; a wooden door must use `@door`).
- **Roof:** a pitched gable with the ridge along z at the house's middle (x 7). Limestone-look stairs on each slope with `@stairs`; the ridge and end caps use stair or slab variants. Each roof cell has about a 1 in 5 chance of being air, so the roof has holes. Gable ends are wall-filled.
- **Loft:** over the house's east half, upper wooden slabs make a loft about two blocks wide at wall height plus one. A vanilla ladder (`@facing`) against the inner wall reaches it. One chest sits on the loft.
- **Furniture:** two small tables (a fence post with a pressure plate on top), one furnace and one crafting table on the ground floor along the east wall.
- The remaining east strip (x 10) and corners are `.` or bare gravel.

## Chests and spawners

| Mark | Position (approx.) | Category | Ceiling |
|---|---|---|---|
| `chest.vanilla` | loft, north end, about x 8, z 2 | `gregguns:weaponchest_t1` | LV |

- No spawners, no flag. The furnace stays empty.

## Palette roles

| Role | `PaletteKeys` key | Vanilla fallback | Where |
|---|---|---|---|
| Walls | `brick` | stone brick | house walls (weighted mix with `concrete`) |
| Floor | `concrete` | stone brick | house interior |
| Track bed | `minecraft:gravel`, `minecraft:cobblestone`, `minecraft:mossy_cobblestone` | same | bed (cobble is fine here: it is ground fill, not a wall) |
| Rails | `minecraft:rail` weight 2, `air` weight 1 | same | two lines |
| Roof | `minecraft:stone_stairs@stairs` (sandstone stairs as a second option) | same | gable; add `air` weight 1 against stairs weight 4 |
| Windows | `glass_pane` weight 1, `air` weight 1 | glass pane | house walls |
| Loft slabs | `minecraft:wooden_slab` upper half | same | loft |
| Furniture | `minecraft:ladder@facing`, `minecraft:fence`, `minecraft:wooden_pressure_plate`, `minecraft:furnace@facing`, `minecraft:crafting_table` | same | interior |

## Rotation and known bugs

- All roof stairs use `@stairs`; ladder and furnace use `@facing`; check at four rotations.
- Weighted picks must be exact (WGEN-14: Techguns rails came out 3 in 4, not 2 in 3). The layout format already fixes this.
- Do not copy a declared size (WGEN-30). No hard Chisel references (WGEN-24).

## Confidence

Good: footprint, track layout, house size, roof style, chest count. Soft: door and window positions and the exact loft extent (the sheet gives counts, not coordinates), and how the x = 3 rail relates to the platform strip. Keep the intent and report choices.
