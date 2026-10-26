# Missile bodies

- **ICBM baseline:** [missile-item](../../icbm-spec/03-missiles/missile-item.md)
- **Design records:** [0018 §8](../../design/0018-icbm-program.md)
- **Decisions:** [D8](../DECISIONS.md#d8-in-detail), [D24](../DECISIONS.md), [D31](../DECISIONS.md#d31-in-detail), [D34](../DECISIONS.md), [D47](../DECISIONS.md)
- **Pack mods:** Forge (`WavefrontObject`), GregTech (recycling data)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| One missile item per explosive, carrying its warhead | A **body** item per tier with no warhead in it; the warhead is a separate block stacked on the nose | D31 |
| Health set before the explosive is known, so tiers 2 to 4 got tier-1 health (ICBM-03-01) | Health is a constant of the tier, fixed by the entity class | Avoided |
| Tier set by the explosive | Tier set by the **body**: T1 (HV) and T2 (EV to IV) ship in I2; a body carries a warhead of its own tier or a lighter one | D5 |

## The two bodies

| | T1 | T2 |
|---|---|---|
| Registry name (block and item) | `missile_body_t1` | `missile_body_t2` |
| Display name | Missile Body T1 | Missile Body T2 |
| Model height | 3 blocks (3.0) | 4 blocks (4.0) |
| Model width over the fins | 0.88 | 0.80 |
| Collision column | fuselage 0.5 + 1/16 | fuselage 0.4 + 1/16 |
| Carries a warhead up to | T1 | T2 |
| Fuel (D19, D20) | 2,000 mB, RP-1 or better | 4,000 mB, Dense Hydrazine or better |
| Range | 20 to 1,000 blocks | 20 to 3,000 blocks |
| Flight speed | 0.8 blocks per tick | 1.2 blocks per tick |
| Base scatter (D24) | 12 blocks half-width | 8 blocks half-width |
| Health | 20 | 40 |

Fuel, range and speed are constants of the tier (`MissileTier`); the config multiplies the tank (`icbm.missileFuelFactor`) and the range (`icbm.missileRangeFactor`). The production track (the missile chip, schematic, NASA Workbench page and Assembly Line recipes) makes the body items and decides their ingredients, see [04-launch-control](../04-launch-control/index.md) and its build sheet.

## The item and its NBT

- One int key, `guidance` (0 to 3): the guidance computer's circuit tiers above the body's own (D24). Absent reads as 0; a hand-edited value is clamped.
- The item is also the block's item form. It stacks to 1. Its display name is `Missile Body T<n>`.
- **Tooltip (D41):** tier (colored), fuel, range, guidance when above 0, then `[SHIFT]`; the details (which warhead it carries, how to stack and arm it, the fuel bonus) appear with Shift, followed by the *Added by* line. The layout is `MissileTooltipFormat`, a pure class with a unit test.

## Model and rendering

- OBJ models from HBM's Nuclear Tech Mod (LGPL-3.0, restyled), `models/thirdparty/ntm/missile_body_t1.obj` and `_t2.obj`, one texture each, scaled so the fuselage is a whole number of blocks tall. Nose up, base at y = 0, footprint centered at 0.5, 0.5.
- The base block uses a custom ISBRH (`MissileBodyRenderer`) that tessellates the model into the chunk mesh, like the warheads, so there is no per-tick renderer for a placed body. Its textures load into the block atlas through the same `WarheadSprite`. The shared drawing code is `ObjDraw`.
- The flying missile uses `RenderMissile`, which binds the texture directly and draws the same body and warhead models, turned so the nose follows the heading (D31: no visual jump at launch).
- On a Galacticraft pad, which is only 0.2 of a block tall, the body model is drawn **0.8 lower** so it stands on the pad's surface (`MissileSeat`), and a warhead seated on it is lowered with it.

## Recycling (GT)

Both items carry GT item data so the Macerator and the Arc Furnace return the hull: T1 gives 8 stainless steel plates' worth, T2 gives 12 titanium plates' worth and 4 stainless. The guidance circuit and the fuel are lost (D44). The numbers match the plate counts the recipe track uses.

## Requirements

- **GGS-ICBM-03-01** A T1 and a T2 body item and block exist; their fuel, range, speed, height and health are the table above.
- **GGS-ICBM-03-02** `guidance` NBT clamps to 0..3; absent reads as 0.
- **GGS-ICBM-03-03** Health is the same at every point of the missile's life and never depends on the load order of the save (avoids ICBM-03-01, -04).
- **GGS-ICBM-03-04** The body models load through Forge's loader and their measured height matches `heightBlocks` (checked in `MissileModelsTest`).
- **GGS-ICBM-03-05** The tooltip layout follows D41.

## Tests

`MissileTierTest`, `MissileTooltipFormatTest`; scenario `icbm.missile.save_round_trip` (health clamp); the model check in `MissileModelsTest`.

## Open questions

None.
