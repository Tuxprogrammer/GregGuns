# Stack on the pad

- **ICBM baseline:** [missile-item](../../icbm-spec/03-missiles/missile-item.md) (a missile is one item there)
- **Design records:** [0018 §8, §9](../../design/0018-icbm-program.md)
- **Decisions:** [D31](../DECISIONS.md#d31-in-detail), [D32](../DECISIONS.md), [D8](../DECISIONS.md#d8-in-detail)
- **Pack mods:** Galacticraft (`GalacticraftCore`, `3.4.34-GTNH`): launch pad, fuel loader
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

ICBM puts a finished missile item into a launcher. GregGuns builds the missile in the world (D31): place the **body** on the pad, place a **warhead** on its nose, **arm** the stack. Arming replaces the blocks with one missile entity.

## Placing the body

- Right-click the **top** of a Galacticraft launch pad with the body item. A click on any of the pad's nine blocks places the body on the **centre** block (the pad's tile docks only what stands in the centre column). On other ground, the top face of any solid block takes it.
- The base cell and `heightBlocks - 1` filler cells above it plus one cell for the warhead must be free; otherwise nothing is placed.
- The filler block (`missile_filler`) is the warhead mechanism reused (D31): invisible, collides like its slice, forwards clicks to the base, is not in the creative tab, and breaking any cell breaks the body and drops one item with its guidance. If the base is removed some other way, the fillers remove themselves on their next update.
- The base tile (`TileMissileBody`) stores the guidance, the placer and whether it stands on a pad (which lowers the model by 0.8, see [missile-bodies](missile-bodies.md)).

## Seating a warhead

Any warhead block placed on the cell above the top filler seats on the nose; there is no special placement code. Its model is drawn lowered to follow the body when the body is on a pad. A warhead of a heavier tier than the body is refused at arming, not at placement, so a player can see why.

## Arming

Sneak and right-click any cell of the body **with an empty hand**. `MissileStacks.arm` checks, in order:

| Result | Lang key | Condition |
|---|---|---|
| `NOT_BODY` | `gregguns.icbm.arm.not_body` | The clicked cell is not part of a body |
| `INCOMPLETE` | `...incomplete` | A filler cell is missing or the base has no tile |
| `NO_WARHEAD` | `...no_warhead` | No warhead block on the nose |
| `WRONG_WARHEAD` | `...wrong_warhead` | The warhead's tier is heavier than the body's |
| `WARHEAD_LIT` | `...warhead_lit` | The warhead's fuse is lit |
| `OK` | `...ok` | Done |

On `OK` the warhead block, the fillers and the base block are removed **without drops**, and a missile entity of the body's tier is spawned at the pad surface (the base cell's Y minus 0.8 on a pad, else the base cell's Y), carrying the warhead's explosive and fill, the body's guidance and the owner. It stands on the pad, unlaunched and with an empty tank.

## Taking it apart

Sneak with an empty hand and right-click the entity, or hit it while sneaking, or break the pad under it: it drops the body item (with its guidance) and the warhead item (with its explosive and fill). The fuel is lost. Nothing else harms a missile that has not launched, so a creeper or a stray arrow cannot set it off on its pad. A missile whose support block is removed from under it does the same.

## The pad API (the seam to launch control)

`PadMissiles`, package `com.gregguns.icbm.missile`. Pad coordinates are any block of the 3x3 pad area, with `padY` the pad block's Y.

| Method | Returns |
|---|---|
| `status(world, padX, padY, padZ)` | `MissileStatus`: `present()`, `armed()`, `fueled()`, `tier()`, `fuelMb()`, `capacityMb()`, `guidance()`, `explosive()`, `fill()`; `MissileStatus.NONE` when nothing stands there |
| `maxRange(world, x, y, z)` | the range in blocks, 0 when empty |
| `expectedRadius(world, x, y, z, tx, tz)` | the blast radius at that target with the fuel bonus (D20), for the GUI |
| `launch(world, x, y, z, tx, tz, player)` | `LaunchResult`: `OK`, `NO_MISSILE`, `NOT_ARMED`, `NOT_FUELED`, `TOO_CLOSE`, `OUT_OF_RANGE`, `OVER_CAP`, `BAD_TARGET`, `BUSY` |

Every rule lives in the launch call; the launch-control block only passes the target and shows the result with the lang key `gregguns.icbm.launch.<name lowercased>`. A body that is stacked but not armed has no entity, so the caller sees `NO_MISSILE`.

## Requirements

- **GGS-ICBM-03-10** A body placed on a pad lands on the pad's centre block whichever pad block was clicked.
- **GGS-ICBM-03-11** Arming a complete stack with a fitting warhead replaces it with one missile entity carrying explosive, fill, guidance and owner, and drops nothing.
- **GGS-ICBM-03-12** A stack with no warhead, a too-heavy warhead, a missing filler or a lit warhead is refused with the matching result and left untouched.
- **GGS-ICBM-03-13** Disarming drops the body (with guidance) and the warhead (with explosive and fill).
- **GGS-ICBM-03-14** `PadMissiles.launch` returns the first failing rule and leaves the missile on the pad on every refusal.

## Tests

Scenarios `icbm.missile.stack_and_arm`, `icbm.missile.launch_rules`, `icbm.pad.survives` (pad placement and the pad API from a pad cell).

## Open questions

None.
