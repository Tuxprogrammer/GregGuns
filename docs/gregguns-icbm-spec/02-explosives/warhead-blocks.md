# Warhead blocks

- **ICBM baseline:** [explosive-block](../../icbm-spec/02-explosives/explosive-block.md): triggers, fuse, the primed entity
- **Design records:** [0018 §8](../../design/0018-icbm-program.md)
- **Decisions:** [D31](../DECISIONS.md#d31-in-detail), [D43](../DECISIONS.md#decided), [D44](../DECISIONS.md#decided), [D47](../DECISIONS.md#decided)
- **Pack mods:** Forge (OBJ loader `WavefrontObject`, 1.7.10), GregTech (hazard-free; recycling data)
- **Roadmap:** [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine); stacking on a missile body in I2

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| Explosive block is a textured cube | A real 3D warhead model, nose up, drawn as a static block model | D31 |
| Always 1x1x1 | T1 and T2 fit in 1x1x1; bigger warheads add invisible filler blocks | D31 |
| Primed TNT-like entity | A fuse on the tile, and the block becomes a falling-free primed state | One tile, no entity; the missile stack (I2) reads the tile |

## Model and rendering

- Models are Wavefront OBJ in Forge's strict grammar (D43), from HBM's Nuclear Tech Mod (LGPL-3.0, modified, see CREDITS), under `assets/gregguns/models/thirdparty/ntm/`. Nose up (+Y), block-local coordinates 0..1, one texture per model. They load through `WavefrontObject`.
- **Textures and the atlas.** A static block model is drawn into the chunk mesh, which has the block atlas bound, so a texture cannot be bound directly as it can in an entity renderer. The textures stay where the guides track put them (`textures/models/thirdparty/ntm/`) and a small custom atlas sprite loads each one into the block atlas at stitch time. The renderer maps the OBJ's UVs into that sprite.
- The block uses a **custom ISBRH** (`getRenderType` returns our id). On a chunk rebuild it tessellates the model's faces into the chunk mesh, so there is no per-tick renderer. The item in the hand and inventory uses the same model through an item renderer.
- A **unit test** loads every shipped warhead OBJ through `WavefrontObject(String, InputStream)`, so a grammar error fails the build (D43).

## Shape and collision

- T1 and T2 fit in 1x1x1. The **selection and collision box** is a centered column the model's width plus 1/16: 0.5625 wide for T1 (model 0.5) and 0.4625 for T2 (model 0.4), full height 1.0. `WarheadModelsTest` measures the shipped OBJs and checks the box contains the model and is at most 1/16 larger.
- The block is not a full cube (`isOpaqueCube` and `renderAsNormalBlock` false), so neighbors still draw their faces.

## Filler blocks (multi-block warheads)

- A warhead taller or wider than one block occupies extra cells. Each extra cell holds an invisible **filler block** (`warhead_filler`), with a tile that stores the offset to the **main** block.
- The filler has the collision box of its slice of the model, is not rendered, and is not in the creative tab. The player cannot hold or place one.
- **One piece:** breaking any cell breaks the whole warhead and drops one item with the NBT of the main tile. Explosions and pistons do the same. If the main block is removed some other way, the fillers remove themselves on their next update, and a filler whose main is gone is never left behind.
- Placing checks every cell is replaceable first; if not, nothing is placed.
- I1 ships the mechanism and a dev-only 1x1x2 test warhead. T3 and up use it in I3.

## Use as a demolition charge

A warhead placed alone is a charge, as ICBM's explosive blocks are:

- **Redstone:** a rising edge of power on the block (or its fillers) starts the fuse.
- **By hand:** right-click with flint and steel or a fire charge starts it; a bare-hand click does nothing (so the player does not trip it by accident).
- **Fuse:** 100 ticks for every warhead (ICBM uses 100 to 120). While fused the tile beeps once per second (no flash: the model is a static chunk mesh). Breaching's instant fuse is dropped.
- **Detonation:** the block is removed, then the blast starts at its center through the shared registry and pipeline (`ExplosionRules`, `BlockBreakGuard`, `ExplosionEvent.Detonate`) with the placer as the actor, so claims and `explosions.blockDamage` are respected.
- Breaking a fused block defuses and drops it. The warhead is not chain-reaction-prone: another explosion primes it with a short fuse (10 ticks).
- The tile saves its explosive, fill, fuse and owner. The fuse is an int, not ICBM's byte, so a fuse above 127 survives a reload (`ICBM-02-*`).

## Requirements

- GGS-ICBM-02-10: placing a T1 or T2 warhead fills one block and renders the OBJ model, not a cube.
- GGS-ICBM-02-11: the collision and selection box contain the model and are within 1/16 of it.
- GGS-ICBM-02-12: a multi-block warhead breaks as one piece and drops exactly one item with the fill and explosive intact.
- GGS-ICBM-02-13: redstone and a lighter both start the fuse; a bare click does not.
- GGS-ICBM-02-14: a fuse longer than 127 ticks saves and loads.
- GGS-ICBM-02-15: every shipped OBJ loads through `WavefrontObject`.

## Tests

`WarheadGeometryTest`, `WarheadFuseTest`, `WarheadModelsTest` (loads the OBJs); scenarios `icbm.warhead.block.*` (place, redstone, hand, detonate, break as one, multi-block drop).

## Open questions

None.
