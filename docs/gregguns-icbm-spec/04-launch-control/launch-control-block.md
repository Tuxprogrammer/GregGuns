# Launch Control block

- **ICBM baseline:** [launch-screen](../../icbm-spec/04-launch-control/launch-screen.md), [launcher-base](../../icbm-spec/04-launch-control/launcher-base.md)
- **Design records:** [0018 §8-9](../../design/0018-icbm-program.md), [0011](../../design/0011-machine-policy.md)
- **Decisions:** [D13](../DECISIONS.md), [D41](../DECISIONS.md), [D46](../DECISIONS.md), [D288](../DECISIONS.md), [D299](../DECISIONS.md), [D300](../DECISIONS.md)
- **Pack mods:** GregTech (single block, covers, data stick), ModularUI2, Galacticraft (the pad)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| Launcher screen block linked to launchers by a cable network | One block set against the pad's edge, or built into the silo's structure; no network | A Galacticraft pad is one 3x3 area and Galacticraft's own controller attaches by touch; the silo knows its own Launch Control (D288) |
| Energy buffer fed by a battery slot (dead in ICBM, ICBM-04-15) | GT EU buffer; 300 EU per shot, paid only when the pad accepted the launch; a silo's own Launch Control pays from the silo's Energy Hatches | ICBM-04-07, D300 |
| Default target (0, 0, 0) is a legal target (ICBM-04-06) | A target is unset until both coordinates are given | ICBM-04-06 |
| Range is two per-axis squares with a hard-coded minimum (ICBM-04-05) | A circle on the horizontal distance from the pad's center; the minimum is the body tier's (20) | ICBM-04-05 |
| Strong-power, level-triggered redstone (ICBM-04-20) | Fires once on a rising edge | ICBM-04-19, 04-20 |
| Client packets unauthenticated (ICBM-04-13) | ModularUI2 sync handlers: the server re-checks the player (alive, within 8 blocks) and parses and clamps every typed value | ICBM-04-12, 04-13 |

## The block

- GT single block, **HV**, MTE ID **20140**, English name **Launch Control**, in `GregGunsMTEIDs.LAUNCH_CONTROL` (the roster's first ICBM entry). Energy buffer of `V[HV] * 16` EU, one amp in, from any side.
- Recipe: Assembler, HV, 30 s, circuit 24: HV hull, HV sensor, HV emitter, an empty GT data stick, 2 Advanced circuits. Recycling returns the hull's stainless steel.
- One inventory slot for a GT **data stick**. Automation can put a stick in; nothing can pull it out.
- Tooltip lines come from lang keys; GT machine tooltips do not support Shift, so the details (reach, range, EU, triggers) are always shown. This deviates from D41's Shift rule for machines because GT's block tooltip has no Shift hook (the Turret and benches do the same). It carries "Added by: GregGuns".

## Finding the pad or the silo

The block fires a `LaunchSite` (D288). There is no search for silos and no election between sites.

- **Part of a silo.** A Launch Control built into a Missile Silo's structure, in any hatch position (a wall, glass or floor-ring block; [missile-silo](../03-missiles/missile-silo.md)), is that silo's. The silo's structure check finds it (`SiloLaunchControl`, a hatch element) and binds it (`MTELaunchControl.bindSilo`, the controller's coordinates, saved): the silo knows its Launch Control and the Launch Control knows its silo. It fires that silo only. While the silo is not formed it shows `silo_unformed` and fires nothing; a silo that forms without it (or a controller that is gone) releases it, and it stands free. A silo takes at most one Launch Control: a second one fails its position, and the silo does not form. Once, 20 ticks after it loads or is placed, a free block asks every formed silo whose structure could hold it, and that has no Launch Control, to check its structure again, because GT only rechecks a structure when a machine block beside its parts changes.
- **Free-standing, beside a pad.** A Launch Control that is part of no silo fires a Galacticraft rocket pad it **touches**: it stands against one of the 3x3 pad's four edges, level with the pad, the way Galacticraft's own launch controller attaches (`TileEntityLandingPad.getConnectedTiles`). That is the twelve cells two blocks from the pad's center along one axis and at most one along the other (`PadAdjacency`, unit tested). A corner contact, a gap or a different level does not attach. The pad is found by its **center block** (`GCBlocks.landingPadFull`, metadata 0); a buggy pad is not a rocket pad. A formed silo's shaft floor is never a bare pad. With no touching pad the status is `no_pad` ("No pad adjacent").
- **Several touching pads.** The block takes the one with a launch under way (so Abort reaches it), then one with a ready missile (armed, full tank), then an armed one, then one with anything on it, then an empty one; among equals the first in the fixed side order (north, east, south, west, each from the low coordinate up) (`SiteChoice`, unit tested). The choice never flips between two equal pads.

The range line and the expected radius are those of the bound site (a silo adds its bonus), and a silo whose Energy Hatch is below the missile's tier shows `silo_tier` and locks the button. `LaunchPads` is the only class that names a Galacticraft block, and it is reached only after the module gate.

**Generated silos** carry their Launch Control in the front face, on top of the right hatch column (seen by a player facing the controller), level with the window's top row and outside the 3x3 glass window (`SiloShape.LAUNCH_CONTROL_COLUMN`, `LAUNCH_CONTROL_RISE`; worldgen cell `SILO_LAUNCH_CONTROL`; D299). The dev builder (`DevStructureBuilder.buildSilo`, `SILO_LC`) puts it in the same place. The Launch Control room of the complex keeps its own Launch Control as furniture: it touches no pad and is part of no silo, so it shows "No pad adjacent".

## Power

A shot costs 300 EU (`EU_PER_SHOT`), checked by the pre-check and paid only once the site accepted the launch.

- **Free (beside a pad):** the block's own GT buffer, fed by cable.
- **Part of a silo (D300):** the silo's Energy Hatches, normal and exotic (`MTEMissileSilo.launchControlEu`, `payLaunchControl`). A Launch Control in a silo wall has no cable, so it draws on the silo. Its own buffer is not used while it is bound, so the screen and the status say one thing. With too little EU in the silo the status is `silo_no_power` ("Silo has too little EU"), and the screen's power line reads `Silo EU: %s (shot: %d)`. The payment checks the full amount first, then drains it hatch by hatch (`ExoticEnergyInputHelper.drainEnergy`, which is not atomic). If the hatches were drained in the same tick between the check and the payment, the shot stands and nothing goes below zero.
- **OpenComputers:** `launchInfo()` carries `power` (`silo` or `own`) and `eu` (what a shot is paid from).

## The pad API (coder 1's `PadMissiles`)

`status(world, x, y, z)` returns whether a missile stands there, armed, with its tank, tier and guidance; `maxRange`, `expectedRadius` (including the D20 fuel bonus) feed the GUI; `launch(world, x, y, z, tx, tz, player)` returns a `LaunchResult`. All range, cap, fuel and arming rules live behind that API. The block's own pre-check (`LaunchGate`) repeats them only to say why the button is locked; the API's answer is final and mapped one to one to a status line.

## Targeting

1. **Coordinates.** X and Z number fields in the GUI. Either field empty means no target. The server parses with `LaunchTarget.parseCoordinate`: integers up to plus or minus 30,000,000, nine characters at most; anything else is ignored.
2. **Use my position.** Sets the target to the pressing player's block.
3. **Radar Gun in the slot (D285).** The target slot takes a Radar Gun: Load copies its target in, Save writes the block's target onto it, and the gun stays in the slot. A gun with no target, no item, or a target from another dimension each give their own message. Right-clicking the block with a targeted gun in hand copies the target without opening the screen (D93). The screen reads "No target: use a Radar Gun" when empty.
   *Also accepts (admins and automation, unadvertised in game): a GT data stick.* Save copies the target onto the stick as `gregguns_icbm_target {x, z, dim}`; Load copies it back. A stick from another dimension, an empty stick, and no stick each give their own message. (`TargetStick`, unit tested.)
4. **Redstone and covers**, below, use the stored target.

## The GUI (ModularUI2)

A dark status screen (state, the bound site, target, distance and range, expected blast radius, EU), the X and Z fields, **Use my position**, the target slot (a Radar Gun, with a faint gun icon when empty; data sticks are accepted without a mention) with **Load target** and **Save target**, **Launch** and **Abort** (D88). Every control is labeled with a tooltip. **Launch** is gray until the pre-check is READY, and its tooltip then names the first reason (no target, no pad, power, cooling down, no missile, not armed, not fueled, too close, out of range). Load and Save are gray with nothing in the slot and say so. **Abort** sits beside Launch: it is live while a countdown runs (the screen reads "Countdown: N s"), gray when none runs, and gray with "Ignition: no abort" once the engines have lit. The site line names what the block is bound to: `Silo 2 at 120, -30` (the silo's name is the first written line of a wall sign on its face one block above the controller, where the complex hangs `Silo N`), `Silo at 120, -30` without a sign, `Pad at 120, -30`, or `Bound to no silo or pad`; the coordinates are the launch point (the shaft's or the pad's center). The state line never repeats the target line: with no target set it reports the silo or pad instead, as its first problem (no pad, not formed, power, cooling down, no missile, not armed, not fueled, hatch tier) in red, or "Missile armed and fueled" in amber when only the target is missing (`LaunchGate.checkSite`, `statusLineKey`); the distance reads "-" with no target. Six 8 px rows fill the screen. Status numbers reach the client as read-only sync values; no client code reads the world.

## Triggers

| Trigger | Behavior |
|---|---|
| Launch button | Server re-checks the player, then fires |
| Abort button | Server re-checks the player, then stops a countdown in progress (refused after ignition) |
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
- **GGS-ICBM-04-25** A Launch Control in a silo's structure fires that silo only, and names it; a free one fires only a pad it touches, level with it, and prefers a site with a ready missile over an empty one (D288).

## Tests

`LaunchControlTest` (target, parse, gate order, the site check and the no-target status line, trigger edge, stick codec, number format), `ControlStatusTextTest` (every status line fits its row), `LaunchControlPairingTest` (pad adjacency, the site choice and its tie-break, the site line) and scenarios `icbm.control.registered`, `icbm.control.pad_and_gate` (only a touching pad, level with the block; with no target the status line names the pad's own problem or the ready missile), `icbm.control.pad_choice` (between two pads the loaded one wins; ties keep the side order), `icbm.control.data_stick`, `icbm.control.redstone`, `icbm.silo.own_control` (two silos, each fired by its own Launch Control; a free one between them belongs to neither) and `icbm.silo.unformed_pad`. The full launch from the block is in the missile milestone's scenarios.

## Open questions

- A cruise launcher: not shipped in I2 or I3 (D10 defers cargo and held-item; the cruise launcher is not in the decisions). Left for the owner's alpha review.
