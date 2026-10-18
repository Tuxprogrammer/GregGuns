# 07-blocks: ForgeMultipart integration

## Status: inactive

Techguns 0.1.2 has **no working ForgeMultipart (FMP) integration**:

- **No microblocks.** No Techguns block is registered as a microblock material. No `MicroMaterialRegistry` call exists anywhere in the source.
- **No parts.** `TGMultiPartRegister` is an empty class. Its whole body is commented out, the factory and converter interfaces are commented out of the class declaration, and nothing references the class.
- **The test blocks are not registered.** `TGBlocks.lamp01FMP` (`BlockTGLampFMP`) and `TGBlocks.smallCube` are constructed in `TGBlocks.init`, but their `GameRegistry.registerBlock` calls are commented out.
- The mod annotation lists `after:ForgeMultipart` as a load-order hint only.

So, to the question "which blocks get microblocks": **none**.

## What the dormant code was meant to do

The commented-out `TGMultiPartRegister` describes this design:

- `init()` would register the class as a part converter and as the factory for two part types, `techguns:smallCube` and `techguns:lamp01FMP`.
- `blockTypes()` would list `smallCube` and `lamp01FMP` as the convertible blocks.
- `createPart(name, client)` would return a new `SmallCubePart` or `Lamp01Part` for those names.
- `convert(world, pos)` would turn an in-world `smallCube` block into a `SmallCubePart`, and a `lamp01FMP` block into a `Lamp01Part` carrying the block's metadata. That would let them share a block space with microblocks.

### Part classes (compiled, but never used)

| Class | FMP base | Type string | Block | Bounds | Notes |
|---|---|---|---|---|---|
| `SmallCubePart` | `McMetaPart` | `techguns:smallCube` | `TGBlocks.smallCube` | cube 0.2–0.8 on every axis | test part |
| `Lamp01Part` | `McSidedMetaPart` | `techguns:lamp01FMP` | `TGBlocks.lamp01FMP` | 0.2–0.8 × 0–0.1 × 0.2–0.8 (a floor plate) | `sideForMeta(meta)` returns the opposite of the direction `meta`. The block stores the clicked side as its metadata, so the part attaches to the face it was placed against. |

`BlockTGLampFMP` (the matching block) follows a public "sided multiparts" tutorial (credited in its comment):

- **Metadata:** the clicked side.
- **Placement:** allowed if any of the six neighbours presents a solid face.
- **Support check:** it drops when its support is gone.
- **Bounds:** a 0.4 × 0.4 plate, 0.1 thick, on the face given by its metadata:

  | Meta | Plate at |
  |---|---|
  | 0 | the ceiling (y 0.9–1) |
  | 1 | the floor (y 0–0.1) |
  | 2 | the +Z face (z 0.9–1) |
  | 3 | the −Z face (z 0–0.1) |
  | 4 | the +X face (x 0.9–1) |
  | 5 | the −X face (x 0–0.1) |
- **Other properties:** texture `techguns:lamp01`, light 15, glass material.

The real lamp (`lamp01`, see [special-blocks.md](special-blocks.md#7-lamps-and-lanterns-lamp01-blocktglamp)) replaced it.
