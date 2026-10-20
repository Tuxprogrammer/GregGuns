# Structure layout format

- **Techguns baseline:** none. Techguns' structure data files are All Rights Reserved and are not read, copied or converted ([BUGS WGEN-23](../../techguns-spec/BUGS.md), WGEN-24, WGEN-14, WGEN-31 are the lessons). This format is original to GregGuns.
- **Design records:** [0005](../../design/0005-world-presence.md)
- **Code:** `com.gregguns.worldgen.layout` (`LayoutParser`, `Layout`, `LayoutRegistry`, `PaletteKeys`, `Stamper`, `RotationKind`, `Rotations`)
- **Roadmap:** M8, infrastructure piece G5

## What a layout is

One plain-text file per structure at `assets/gregguns/structures/<id>.txt`, UTF-8. It is a block grid in *layout space* (x east, y up, z south), authored facing rotation 0, plus the palette that says what each character means. Rotation, position and variant come from the plan at generation time, never from the file.

## File shape

```
# comment lines start with '#'; blank lines are ignored
gregguns-layout 1
id: gas_station
size: 13 8 16
ground: 1

palette:
W = concrete*3 | minecraft:iron_bars*1
S = minecraft:stone_stairs#0@stairs
D = minecraft:wooden_door#1@door

marks:
chest.vanilla 3 1 4

layers:
layer 0
WWWW...
layer 1
...
```

| Part | Rule |
|---|---|
| First line | `gregguns-layout 1`. Any other version is rejected. |
| Header | `id: lower_snake_case` (must equal the file name), `size: X Y Z` (X, Z in 1..128, Y in 1..255), optional `ground: N` (default 0). Unknown keys are errors. |
| `ground` | The layer that sits at the plan anchor's Y. Layers below it are buried (the Big Bunker's 10 buried blocks). |
| `palette:` | One line per character: `<char> = choice [| choice]...`. The character is a single non-space character, not `.`, `#`, `=` or `|`, and unique. |
| `marks:` | Optional. `<tag> x y z` with a tag like `chest.weapon` or `flag`. The generator acts on marks after the blocks are down. Coordinates are in layout space and inside `size`. |
| `layers:` | Last section. `layer N` (0 is the bottom), then exactly `Z` rows of exactly `X` characters. A row is one z, a column is one x. `.` means "leave what is there". Layers that are not listed are all `.`. |

### Palette choices

`key[#meta][@kind][*weight]`, read right to left.

- **key:** a palette key (`concrete`, `factory_wall`, `lantern`, `air`, ... see [chisel-palette.md](chisel-palette.md)) or a direct block name `namespace:name`. Use palette keys for anything Chisel could supply, so it falls back on a stripped install.
- **`#meta`:** 0..15. Omitted means the key's own default meta.
- **`@kind`:** how the meta turns with the structure: `none` (default), `facing` (chests, furnaces, ladders, signs: 2 N, 5 E, 3 S, 4 W), `stairs`, `torch`, `door`, `trapdoor`, `axis` (log axis). Needed for any block whose metadata is a direction. Tables are in `RotationKind`; bits outside the facing (open, upside-down, top half) are kept. This is what keeps door and stair halves facing the right way at every rotation (WGEN-31).
- **`*weight`:** positive integer, default 1. With several choices, each cell picks one in proportion to the weights.

### Weighted picks (WGEN-14)

A choice of weight w owns exactly w of the `total` rolls: the roll is drawn from `[0, total)` and compared with `<`. Techguns drew from `[0, total]` with `<=`, which gave the first option an extra point (rails came out 3/4 instead of 2/3). The roll is a hash of the plan variant and the **world position** of the cell, not a running Random, so a structure written by several populate windows is identical whichever window writes a column and in whatever order.

## Rotation

Rotation `q` is `q` quarter turns clockwise seen from above (layout east becomes south). The rotated footprint is `X x Z` for even `q` and `Z x X` for odd `q`, and **the plan anchor is the footprint's minimum-x, minimum-z corner at every rotation**. So the footprint is `[anchorX, anchorX + w) x [anchorZ, anchorZ + d)` and the terrain and window checks never have to un-rotate. Cell mapping (`Rotations`):

| q | offset x | offset z |
|---|---|---|
| 0 | x | z |
| 1 | Z-1-z | x |
| 2 | X-1-x | Z-1-z |
| 3 | z | X-1-x |

## Loading and validation (GGS-11-28, WGEN-23)

`LayoutRegistry.load` runs once in `postInit`, after `PaletteKeys.resolveForPack`. For each structure id it reads, parses and checks the file. Any of these disables **that structure only**, logs one error line naming the file and line, and never throws: missing file, wrong version, missing header field, out-of-range size, bad palette line, undefined character in a layer, wrong row width, missing or extra rows, duplicate layer, unknown rotation kind, mark outside the grid, a palette key that does not resolve, or a header `id` that differs from the file name. Nothing runs in a static initializer.

## Stamping

`new Stamper(keys).write(sink, plan, window)` writes the part of a `StampPlan` (layout, anchor, rotation, variant) that lies in one populate window and returns the count of blocks written (GGS-11-08). It walks only the window-and-footprint intersection, so it produces no write the `ClippingSink` would reject, never reads the world and never requests a chunk (GGS-11-09). Windows tile the plane, so the writes of all windows a structure touches add up to the whole structure exactly once. Marks inside the window are reported through `MarkListener` for the generator's chest, spawner and flag steps. Layers that would fall outside Y 0..255 are skipped.

`StampPlan` is deliberately small (layout, anchor, rotation, variant). G9 adapts the G2 `StructurePlan` to it.

## Tests

`LayoutParserTest` (every malformed shape), `LayoutRegistryTest` (missing, truncated and unknown-key files disable only themselves), `PaletteEntryTest` (WGEN-14 exact weights), `RotationsTest` (bijection, inverse, facing tables), `PaletteKeysTest` (Chisel absent, null field, source scan for GGS-11-25), `StamperTest` (each rotation into `RecordingWorld`, only in-window writes, adjacent windows cover the structure exactly once). Fixtures: `src/test/resources/assets/gregguns/structures/test_*.txt`.

## Authoring note (PLAN-M8 §4)

Layout files are written by a clean-room builder working only from the structure notes, in this format. Use palette keys for decorative blocks so a missing Chisel or missing GregGuns decor block degrades to vanilla instead of failing.
