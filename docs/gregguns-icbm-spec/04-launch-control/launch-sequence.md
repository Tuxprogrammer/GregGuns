# Launch sequence (pad)

- **ICBM baseline:** [launch-sequence](../../icbm-spec/04-launch-control/launch-sequence.md)
- **Decisions:** [D11](../DECISIONS.md), [D13](../DECISIONS.md), [D20](../DECISIONS.md#d20-in-detail), [D24](../DECISIONS.md)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

From "fire" to the missile leaving the pad. Flight, fuel and the entity are in [03-missiles](../03-missiles/index.md).

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Bug avoided |
|---|---|---|
| Missile consumed, spawned and launched before power is re-checked | All checks first; the EU is taken only after the pad accepted | ICBM-04-07 |
| No clearance check; pad launches into a blocked shaft | The pad API owns the clearance of the flight column | ICBM-04-26 |
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

## Tests

`LaunchControlTest.theGateReportsTheFirstProblemInOrder`, `padResultsMapOneToOne`, `everyStatusHasItsOwnLangKey`, `distanceIsACircleNotASquare`; scenario `icbm.control.pad_and_gate`.
