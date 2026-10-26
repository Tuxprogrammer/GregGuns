# Launch Control block

- **ICBM baseline:** [launch-screen](../../icbm-spec/04-launch-control/launch-screen.md), [launcher-base](../../icbm-spec/04-launch-control/launcher-base.md)
- **Design records:** [0018 §8-9](../../design/0018-icbm-program.md), [0011](../../design/0011-machine-policy.md)
- **Decisions:** [D13](../DECISIONS.md), [D41](../DECISIONS.md), [D46](../DECISIONS.md)
- **Pack mods:** GregTech (single block, covers, data stick), ModularUI2, Galacticraft (the pad)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| Launcher screen block linked to launchers by a cable network | One block within reach of the pad; no network at T1-T2 | A Galacticraft pad is one 3x3 area; the silo (I3) brings its own feed |
| Energy buffer fed by a battery slot (dead in ICBM, ICBM-04-15) | GT EU buffer; 300 EU per shot, paid only when the pad accepted the launch | ICBM-04-07 |
| Default target (0, 0, 0) is a legal target (ICBM-04-06) | A target is unset until both coordinates are given | ICBM-04-06 |
| Range is two per-axis squares with a hard-coded minimum (ICBM-04-05) | A circle on the horizontal distance from the pad's center; the minimum is the body tier's (20) | ICBM-04-05 |
| Strong-power, level-triggered redstone (ICBM-04-20) | Fires once on a rising edge | ICBM-04-19, 04-20 |
| Client packets unauthenticated (ICBM-04-13) | ModularUI2 sync handlers: the server re-checks the player (alive, within 8 blocks) and parses and clamps every typed value | ICBM-04-12, 04-13 |

## The block

- GT single block, **HV**, MTE ID **20140**, English name **Launch Control**, in `GregGunsMTEIDs.LAUNCH_CONTROL` (the roster's first ICBM entry). Energy buffer of `V[HV] * 16` EU, one amp in, from any side.
- Recipe: Assembler, HV, 30 s, circuit 24: HV hull, HV sensor, HV emitter, an empty GT data stick, 2 Advanced circuits. Recycling returns the hull's stainless steel.
- One inventory slot for a GT **data stick**. Automation can put a stick in; nothing can pull it out.
- Tooltip lines come from lang keys; GT machine tooltips do not support Shift, so the details (reach, range, EU, triggers) are always shown. This deviates from D41's Shift rule for machines because GT's block tooltip has no Shift hook (the Turret and benches do the same). It carries "Added by: GregGuns".

## Finding the pad

The block scans a box of plus or minus 4 horizontally and plus or minus 2 vertically for the **center block of a Galacticraft rocket pad** (`GCBlocks.landingPadFull` with metadata 0) and takes the nearest (`PadSearch`, unit tested). A buggy pad is not a rocket pad. `LaunchPads` is the only class that names a Galacticraft block, and it is reached only after the module gate. The pad's center is passed to the pad API.

## The pad API (coder 1's `PadMissiles`)

`status(world, x, y, z)` returns whether a missile stands there, armed, with its tank, tier and guidance; `maxRange`, `expectedRadius` (including the D20 fuel bonus) feed the GUI; `launch(world, x, y, z, tx, tz, player)` returns a `LaunchResult`. All range, cap, fuel and arming rules live behind that API. The block's own pre-check (`LaunchGate`) repeats them only to say why the button is locked; the API's answer is final and mapped one to one to a status line.

## Targeting

1. **Coordinates.** X and Z number fields in the GUI. Either field empty means no target. The server parses with `LaunchTarget.parseCoordinate`: integers up to plus or minus 30,000,000, nine characters at most; anything else is ignored.
2. **Use my position.** Sets the target to the pressing player's block.
3. **Data stick.** Save copies the target onto the stick as `gregguns_icbm_target {x, z, dim}`; Load copies it back. A stick from another dimension, an empty stick, and no stick each give their own message. (`TargetStick`, unit tested.)
4. **Redstone and covers**, below, use the stored target.

## The GUI (ModularUI2)

A dark status screen (state, target, distance and range, expected blast radius, EU), the X and Z fields, **Use my position**, the data stick slot with **Load target** and **Save target**, and **Launch**. Every control is labeled with a tooltip. **Launch** is gray until the pre-check is READY, and its tooltip then names the first reason (no target, no pad, power, cooling down, no missile, not armed, not fueled, too close, out of range). Load and Save are gray without a stick and say so. Status numbers reach the client as read-only sync values; no client code reads the world.

## Triggers

| Trigger | Behavior |
|---|---|
| Launch button | Server re-checks the player, then fires |
| Redstone | A rising edge of the strongest input fires once. A held signal does not refire; the first sample after a load only seeds the state |
| GT machine controller cover | The cover enables and disables the block's work; a rising edge of "allowed to work" fires once. A soft mallet toggle does the same |
| OpenComputers | Not in I2; the outputs sheet places it with the other components in I4 |

A shot starts a 40-tick cooldown on the block.

## Requirements

- **GGS-I2-10** An unset target is never fired at; (0, 0) is a legal explicit target.
- **GGS-I2-11** Range is the horizontal circle from the pad's center.
- **GGS-I2-12** A refused launch costs no EU; the EU is taken after the pad accepts.
- **GGS-I2-13** A held signal fires once. A far or dead player's button press does nothing.
- **GGS-I2-14** Every status has a lang key (ICBM-04-16, 04-17).
- **GGS-I2-15** The block never touches the pad, the fuel loader or anything attached; only the pad API acts on the pad.

## Tests

`LaunchControlTest` (target, parse, gate order, trigger edge, pad search, stick codec, number format) and scenarios `icbm.control.registered`, `icbm.control.pad_and_gate`, `icbm.control.data_stick`, `icbm.control.redstone`. The full launch from the block is in the missile milestone's scenarios.

## Open questions

- A cruise launcher: not shipped in I2 or I3 (D10 defers cargo and held-item; the cruise launcher is not in the decisions). Left for the owner's alpha review.
