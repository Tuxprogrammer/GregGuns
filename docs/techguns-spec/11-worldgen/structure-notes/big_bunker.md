# Notes: Big Bunker (`big_bunker`)

- **Purpose:** word-only brief for the clean-room layout builder. Write `assets/gregguns/structures/big_bunker.txt` in the [layout format](../../../gregguns-spec/11-worldgen/layout-format.md).
- **Sources:** [structure-catalog A4](../structure-catalog.md#a4-big-bunker-bigbunker), [structure-loot](../../../gregguns-spec/11-worldgen/structure-loot.md), plan M8 section 2 and G11 (Zombie Hole spawners).

## Footprint and orientation

- `size: 33 14 18` (x 33, y 14, z 18). Techguns declared 32 by 17 (WGEN-30); the data is 33 by 18. Use 33 by 18.
- **Height:** 14 layers total, of which the lowest 10 (layers 0 to 9) are buried and layer 10 is the original ground-surface layer. The plan table writes "14 plus 10 buried"; the sheet's own layer descriptions (vault at layers 0 to 2, hall at 5 to 8, roof at 12 to 13) only fit 14 in total. Author 14 layers and set `ground` so layer 10 is the surface block layer (the builder must apply the layout-format definition of `ground` and say which value was chosen).
- **Front/entrance:** the structure is symmetrical about its long axis. The main way down is a ramp at each end (west and east, x 8 to 9 and x 27, per the sheet); treat the west ramp as the front and the east one as the rear. There is no street-side door.
- Rotation 0: long axis east-west. After 90 degree rotations the footprint becomes 18 by 33; the format handles the anchor.

## Shape (from the surface down)

- **Surface berm (layers 8 to 11):** a podzol-and-sandbag earthwork ring around a concrete blockhouse in the middle (inner room about x 11 to 21, z 4 to 13). The berm slopes up from the terrain to about two blocks over ground. Four sandbag pill-boxes sit at the four corners of the berm, each about 3 by 3, with spruce-stair steps and a door.
- **Ramps:** two stone-brick-stair ramps (west and east ends) descend from the berm into the lower level. Use `@stairs`.
- **Roof:** a 25 by 10 concrete slab at layer 12 over the blockhouse with a stone-brick-stair rim, entirely covered by a camo-net roof layer on top (layer 13), so from above it looks like foliage.
- **Surface room (layers 10 to 11):** inside the blockhouse under the roof slab; one vanilla chest and one reinforced chest.
- **Middle level (layers 3 to 8):** two side wings connected by stone-brick stairs to a central hall at layers 6 to 8, floored on a concrete slab at layer 5. Interior walls in several concrete looks (light, dark, steel-frame, noise). Doors divide the rooms.
- **Central hall (layer 6):** two reinforced chests and two vanilla chests. A Zombie Hole spawner is set into the layer-5 slab at about (14, 5, 9).
- **Lowest vault (layers 1 to 2, floor at layer 0):** a room 13 wide (about x 11 to 23, z 3 to 13 or so, centered on the structure). A second Zombie Hole spawner is set in its floor at about (17, 0, 8). Eight weapon chests in two rows of four, against the north and south walls.
- **Interior carving:** about 1,700 cells of the 33 by 18 by 14 volume are explicit air to hollow rooms inside solid terrain; the builder must write air (`air` role) for all rooms and corridors, and `.` outside the walls only where terrain should stay.

## Chests and spawners

| Mark | Count | Position | Category | Ceiling |
|---|---|---|---|---|
| `chest.vanilla` | 2 | central hall, layer 6 | `gregguns:weaponchest_t2` | MV |
| `chest.vanilla` | 1 | surface room | `gregguns:weaponchest_t2` | MV |
| `chest.reinforced` | 2 | central hall, layer 6 | `gregguns:reinforcedchest_t2` | MV |
| `chest.reinforced` | 1 | surface room | `gregguns:reinforcedchest_t2` | MV |
| `chest.weapon` | 4 + 4 | vault, two rows of four against the north and south walls | `gregguns:weaponchest_t2` | MV |
| `spawner.zombie_hole` | 1 | central hall slab, about (14, 5, 9) | Zombie Hole (G11) | n/a |
| `spawner.zombie_hole` | 1 | vault floor, about (17, 0, 8) | Zombie Hole (G11) | n/a |

Totals: 3 vanilla, 3 reinforced, 8 weapon, 2 spawners. Spawner behavior (5 nearby, 3 per try, player range 24) is G11's concern, not the layout's. Chests face into the room.

## Palette roles

| Role | `PaletteKeys` key | Vanilla fallback | Where |
|---|---|---|---|
| Main concrete (blockhouse, vault walls) | `concrete` | stone brick | the bulk of the solid shell |
| Dark concrete / frame looks | `brick`, `factory_wall` (a few steel-frame accents) | stone brick / iron block | interior walls, columns |
| Light slab | `concrete` | stone brick | hall floor at layer 5, roof slab |
| Berm soil | `minecraft:dirt#2` (podzol) | same | berm outside |
| Sandbags | no key yet (future GregGuns block); use `minecraft:stained_hardened_clay#12` as a stand-in | same | berm and pill-boxes |
| Camo net | no key yet; `minecraft:leaves#4` (non-decaying) as a stand-in | same | roof cover |
| Stairs | `minecraft:stone_brick_stairs@stairs`, `minecraft:spruce_stairs@stairs` | same | ramps, rim, pill-box steps |
| Doors | `minecraft:iron_door` with `@door`, both halves | same | pill-boxes and interior (about 20 doors in all) |
| Lamps | `lantern` | glowstone | hall and vault |
| Spawner holes | direct block once G11 lands; layout uses the mark `spawner.zombie_hole`, not a block | the mark is the contract | two places |
| Air | `air` | air | all rooms |

## Rotation and known bugs

- WGEN-05 and WGEN-30: Techguns had hand-tuned shifts to hide a rotation bug on this very structure. Do not reproduce them. The anchor-at-min-corner rule and the file size are all the generator needs.
- WGEN-23: a short or truncated file disables this structure only; keep rows exact.
- WGEN-24: all palette keys; no Chisel names. WGEN-25: chests filled by the generator from the seeded draw.
- Stairs: `@stairs`; doors: both halves `@door`; chests: `@facing`.
- Multi-window: the footprint (33 by 18) spans populate windows; the stamper handles it, so the layout needs no special structure.

## Confidence

Good: size, level order, chest totals and places (hall, surface room, vault), spawner positions and kinds, roof and camo cover. Soft: berm outline, pill-box detail, the middle-level wing shapes and the exact vault extent, and the total-height ambiguity noted above. Keep the sequence (berm, roof, surface room, middle hall, vault) and report choices.
