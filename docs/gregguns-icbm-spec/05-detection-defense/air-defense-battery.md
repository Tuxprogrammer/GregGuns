# Air-defense battery

- **ICBM baseline:** [targeting-and-tracking.md](../../icbm-spec/03-missiles/targeting-and-tracking.md) (SAM selection and interception)
- **Design records:** [0018](../../design/0018-icbm-program.md); D15
- **Pack mods:** GT5-Unofficial, ModularUI2
- **Roadmap:** [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense)

## What changes from ICBM

ICBM fires SAMs from launchers on a radio message and picks targets by scanning around the SAM. Ours is a separate HV single block (MTE 20145) that holds SAMs and fires at what a linked radar reports, independent of the silo (D15).

## Numbers (clean-room; lead pursuit, nothing ported)

| Item | Value |
|---|---|
| SAM speed | 9.0 blocks/tick (D126): above the steepest dive any tier makes, `speed * (80 / flat + 1.2)` at the end of an arc, 8.1 for a T3 on a level 20-block flight; unit-tested against `ArcPath.terminalSpeed` |
| Burst radius | 3.5 blocks, at the end of a tick or anywhere along the tick's path (a swept test, since a head-on pass can skip the sphere) |
| Stall | a SAM whose range to its target has not fallen for 20 ticks stops counting in `stillNeeded` |
| Damage | 40; T1 (20 hp) and T2 (40) fall to one SAM, T3 (60) needs two |
| Lifetime | 300 ticks |
| Battery slots | 4 SAM slots (stack 16) plus 1 data stick slot |
| Fire interval | 20 ticks; at most one launch per interval |
| Cost | 400 EU per SAM, paid when it leaves |
| Engage range | 256 blocks from the battery (3D) |

## Behavior

- Every 5 ticks the battery reads the linked radar's incoming list. For the nearest threat in range that the SAMs already in the air will not kill (`SamRules.stillNeeded`), it launches one SAM.
- The SAM flies at the predicted intercept point of the missile (its position plus its last step times the time the SAM needs to reach it, at most 40 ticks of lead), bursts inside 3.5 blocks and calls `attackEntityFrom`, the existing shot-down path (the warhead goes off where the missile is, ICBM-03-03). It passes through blocks and is not saved. A SAM whose chunk has unloaded (the world has taken it off its loaded-entity list) fizzles at once: a SAM nobody can see never hits.
- The world skips an entity whose 32-block surroundings are unloaded, so the battery drives its own SAMs each tick (`EntitySam.advance`, once per tick).
- Link: a data stick in the slot with a radar link. The radar must be in the same dimension, loaded and online; a link never loads a chunk.
- States: no power, no link, link lost, out of SAMs, standby, engaging. Redstone 15 and the active flag while a threat is in range.

## Requirements

- **GGI-05-10.** A threat the SAMs in the air will kill gets no further SAM. A SAM that is not closing does not count toward that.
- **GGI-05-11.** A missile aimed outside the radar's protected radius is never engaged.
- **GGI-05-12.** A SAM never outlives its target or 300 ticks.
- **GGI-05-13.** The battery takes only SAMs and data sticks.

## Recipes (D69, nothing below HV)

Assembler: battery = HV hull, 2 robot arms, sensor, 2 Advanced circuits (HV). SAM x2 = 4 stainless plates, HV motor, HV sensor, Advanced circuit (HV).

## Tests

`SamRulesTest`; scenarios `icbm.defense.intercept`, `.not_incoming`.
