# Launch sequence (pad)

- **ICBM baseline:** [launch-sequence](../../icbm-spec/04-launch-control/launch-sequence.md)
- **Decisions:** [D11](../DECISIONS.md), [D13](../DECISIONS.md), [D20](../DECISIONS.md#d20-in-detail), [D24](../DECISIONS.md)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

From "fire" to the missile leaving the pad. Flight, fuel and the entity are in [03-missiles](../03-missiles/index.md).

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Bug avoided |
|---|---|---|
| Missile consumed, spawned and launched before power is re-checked | All checks first; the EU is taken only after the pad accepted | ICBM-04-07 |
| No clearance check; pad launches into a blocked shaft | The pad API owns the clearance of the flight column: a solid block in the 12-block climb column returns `BLOCKED` (lang `gregguns.icbm.launch.blocked`) before any EU is taken | ICBM-04-26 |
| Delayed shots lose their result and the cluster penalty | No firing delay in I2: a shot is immediate, so there is no discarded result | ICBM-04-02, 04-08, 04-31 |
| Success means "some launcher did not block" | The status names exactly what happened, one lang key per outcome | ICBM-04-09 |
| Result keys missing from the lang file | Every status and launch result has a key; a test pins it | ICBM-04-16, 04-17 |
| Debug code on right-click | None | ICBM-04-18 |

## Steps

1. A trigger arrives ([launch-control-block](launch-control-block.md#triggers)).
2. The block takes a snapshot: target, pad found, EU, cooldown, then the pad's `MissileStatus` and range.
3. `LaunchGate.check` returns the first problem in a fixed order: **no target, no pad, no power, cooling down, no missile, not armed, not fueled, too close, out of range**. Not READY ends here and the status is shown.
4. The block calls `PadMissiles.launch`. The API re-checks everything (including the in-flight cap, D11) and returns a `LaunchResult`.
5. `OK` becomes FIRED: the block takes 300 EU and starts its cooldown. Any other result becomes the matching status and nothing is charged.

The minimum range (20) and the tier's maximum come from the missile body tier; the distance is the horizontal circle from the pad's center to the center of the target block.

## The sequence (D88)

OK no longer means the missile leaves at once. The missile enters `WARMUP` and the rest runs from its clock (`LaunchTimer`, both sides):

1. **Lead (silo only):** the doors open for `icbm.doorOpenTicks` (40). Nothing burns yet, and the missile cannot ignite before the doors are open: the lead is part of the launch, so the countdown always starts with open doors.
2. **Countdown:** `icbm.countdownTicks` (200, 10 s). The Launch Control shows "Countdown: N s". Launch smoke starts at the nozzle: gray puffs that roll out along the floor and swell, 1 puff a tick at first and 6 in the last seconds, with flame in the last 20 ticks (`LaunchSmokeFX`, our own motion code over vanilla particle frames). Clients keep the clock between the server's state packets (sent on change and once a second), so a second player sees the same countdown.
3. **Abort:** the Abort button (`MTELaunchControl.abortFromButton`, re-checks the player like the Launch button) calls `LaunchSite.abort`. During the lead or the countdown it puts the missile back on `PAD`, armed and fueled, drops its chunk ticket, and a silo's doors swing shut from wherever they are. The EU already paid stays paid.
4. **Ignition:** when the clock reaches the total, the sound plays and the phase becomes `CLIMB`. From here `abort()` returns false, the button is locked, and the control shows "Ignition: no abort" until the missile has left the launch column.
5. **Liftoff and arc** are unchanged. A silo closes its doors once the missile's bottom is `icbm.doorCloseHeight` (4) blocks above the rim.

Pads run steps 2 to 5 with no lead and no doors. `ControlStatus` gains `COUNTDOWN` and `IGNITED` (lang keys `gregguns.icbm.control.status.countdown`, `countdown_s` and `ignited`).

Requirements: **GGS-ICBM-04-20** the doors open before the countdown, ignition follows after the lead plus the countdown, and the doors close once the missile clears (`icbm.launch.silo_sequence`); **GGS-ICBM-04-21** abort during the doors or the countdown stands the launch down, armed and fueled, and the missile can fire again (`icbm.launch.silo_abort`, `icbm.launch.pad_sequence`); **GGS-ICBM-04-22** abort after ignition is refused (`icbm.launch.no_abort_after_ignition`); **GGS-ICBM-04-23** the door state and the countdown survive a save (`icbm.launch.reload`) and a silo that comes apart stands down (`icbm.launch.unform`, D90); **GGS-ICBM-04-24** a silo-only body whose silo is intact counts down for as long as the config says, longer than the 300-tick stray-body grace, and ignites (`icbm.launch.silo_long_countdown`).

## Tests

`LaunchTimerTest`, `SiloDoorsTest`, `LaunchControlTest.theGateReportsTheFirstProblemInOrder`, `padResultsMapOneToOne`, `everyStatusHasItsOwnLangKey`, `distanceIsACircleNotASquare`; scenario `icbm.control.pad_and_gate`.
