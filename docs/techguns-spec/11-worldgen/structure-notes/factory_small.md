# Notes: Small Factory Building (`factory_small`)

- **Purpose:** word-only brief for the clean-room layout builder. Describes shape and intent, not cells. Write `assets/gregguns/structures/factory_small.txt` in the [layout format](../../../gregguns-spec/11-worldgen/layout-format.md).
- **Sources:** [structure-catalog A1](../structure-catalog.md#a1-small-factory-building-factoryhousesmall), [structure-loot](../../../gregguns-spec/11-worldgen/structure-loot.md), plan M8 section 2.

## Footprint and orientation

- `size: 9 7 11` (x 9, y 7, z 11). Authored at rotation 0.
- **Front is north (z = 0).** The main entrance is a door in the north wall of the big hall, a little west of center. A back door sits at the south end of the annex. Rotation turns all of this together.
- Layer 0 is the ground/foundation layer (`ground: 1`, so layer 1 is the first walkable level). Layer 0 is solid; the stamper does not fill under it.

## Shape

An L-shaped, two-story light-industrial shed.

- **Main hall:** the full 9 blocks wide along x, 5 deep (z 1 to 5), three blocks of clear height inside. Flat roof at layer 4, which is a walkable terrace.
- **West annex:** a 5 by 5 block attached to the south-west of the hall (x 0 to 4, z 5 to 9), sharing the hall's south-west corner area.
- **Upper room:** a 5 by 5 room over the hall's west end (x 0 to 4, z 1 to 5, layers 4 to 6) with a solid roof slab and lamps. Keep it sealed except for a ladder hatch.
- **Terrace:** the hall roof east of the upper room is open sky, edged with an iron-bar railing one block high on its open sides.
- **Yard:** a small fenced yard on the ground at the south-east, about 3 wide by 4 deep (x 6 to 8, z 7 to 10), edged with iron bars. No gate; players climb over or enter from the annex side.
- **Ladders:** two, each climbing from floor to terrace. One inside the hall near its east end (about x 7, z 2), one just outside the annex's east side (about x 5, z 6). Use `ladder` with `@facing` so they stay attached to a wall at every rotation.

## Interior

- Floor: a technical-looking panel floor with a one-block concrete border.
- Two chests side by side against the hall's south wall, near the middle.
- One crafting table and two furnaces along the hall's east wall (furnaces empty, `@facing` toward the room).
- Windows: glass panes in the north wall, a handful in the long walls, none in the annex's far walls.
- Lamps: four to six, in the hall ceiling, the upper room and beside each door.

## Chests and spawners

| Mark | Position (approx.) | Category | Ceiling |
|---|---|---|---|
| `chest.vanilla` | hall south wall, x 3 | `gregguns:weaponchest_t1` | LV |
| `chest.vanilla` | hall south wall, x 4 | `gregguns:weaponchest_t1` | LV |

- No spawners, no flag. The generator's loot step picks the category from the structure's ceiling; the layout only places marks.

## Palette roles

| Role | `PaletteKeys` key | Vanilla fallback | Where |
|---|---|---|---|
| Hall walls | `factory_wall` | iron block | most wall cells |
| Wall accent / lower band | `concrete` | stone brick | skirting, annex walls (mix about 3 to 1 with `factory_wall`) |
| Floor | `factory_floor` | double stone slab | hall and annex floor; `concrete` for the border |
| Roof and terrace deck | `concrete` | stone brick | upper-room roof slab, terrace |
| Railing / yard fence | `minecraft:iron_bars` | same | terrace edge, yard |
| Windows | `glass_pane` | glass pane | walls |
| Lamps | `lantern` | glowstone | ceiling and door sides |
| Doors | `minecraft:iron_door` with `@door`, both halves | same | north entrance, annex back door |
| Ladders | `minecraft:ladder@facing` | same | two climbs |
| Furniture | `minecraft:crafting_table`, `minecraft:furnace@facing` | same | hall east wall |
| Air | `air` | air | hollow the interior; use `.` outside the shell |

Avoid raw stone and cobblestone for walls (GT's ore pass may overwrite them, open question Q4).

## Rotation and known bugs

- Doors: both halves go through `@door`. Check at all four rotations that each door still opens the right way (WGEN-31 lesson).
- Furnaces, ladders and chests use `@facing`.
- Size comes from the file: Techguns declared 11 by 10 but the data is 9 by 11 (WGEN-30). Do not copy the declared size.
- Materials are palette keys only, never hard-wired mod blocks (WGEN-24).
- This building also appears inside Military Bases as a camp building; keep the footprint exactly as above so the camp placer can reuse the same file.

## Confidence

Good: footprint, floors, chest count, door count. Soft: the exact annex and upper-room overlap and the ladder positions; the sheet gives ranges, not a plan. Keep the intent (L shape, upper room over the west end, terrace, two ladders) and report any deviation.
