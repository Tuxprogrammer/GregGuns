# 05-detection-defense (GregGuns ICBM)

## Overview

Radar, the air-defense battery, the EMP tower and the kept handheld tools. Radar reads the missile tracker's list of missiles in flight, not chunks. ICBM baseline: [icbm-spec 05-detection-defense](../../icbm-spec/05-detection-defense/index.md).

## Contents

All sheets are planned.

- radar.md (planned): single-block radars at HV and EV, a multiblock radar tower from IV, range by tier, impact-point threat test; the sweep is MIT-portable (D14).
- air-defense-battery.md (planned): the SAM-holding single block, radar link, intercept (MIT-portable) (D15).
- emp-tower.md (planned): EU drain, one maintenance issue per multiblock, kill-chance table for missiles, device cap, continuous EU cost (D16).
- handheld-tools.md (planned): the kept tools (radar gun, remote detonator, defuser) per the baseline sheet.
- outputs.md (planned): redstone, GT covers, OC components, data-stick link to batteries (D13, D14).

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Radar station | Single-block HV and EV radars; multiblock tower from IV | GT5-Unofficial | [D14](../DECISIONS.md) |
| "Incoming" test | Predicted impact point inside the protected radius | missile tracker | [D14, D25](../DECISIONS.md) |
| SAM launch | Separate air-defense battery, independent of the silo | GT5-Unofficial | [D15](../DECISIONS.md) |
| EMP vs machines | Drains stored EU; one random maintenance issue per multiblock; at most N devices per pulse (default 64), nearest first | GT machines, cables, batteries | [D16](../DECISIONS.md) |
| EMP vs missiles | ICBM's kill-chance table | missile tracker | [D16](../DECISIONS.md) |
| EMP cost by radius | Grows continuously (no integer division) | EU | [D16](../DECISIONS.md), [A2-lineage](../../icbm-spec/A2-lineage/index.md) |
| Radio bus | Data sticks and the GT machine-controller cover | GT5-Unofficial | [D13](../DECISIONS.md) |
| Computer control | OC components | OpenComputers | [golden-dome.md](../01-core/golden-dome.md) |
| Laser detonator, other tools | To settle in the I4 handheld-tools sheet | to settle | [ROADMAP-ICBM I4](../../ROADMAP-ICBM.md#i4--detection-and-defense) |

## Decisions carried

D13, D14, D15, D16, D25 (tracker data).

## Roadmap

[I4](../../ROADMAP-ICBM.md#i4--detection-and-defense).

## Pack-side changes

None yet.

## Related areas

[03-missiles](../03-missiles/index.md) (tracker, SAM missiles), [04-launch-control](../04-launch-control/index.md) (radar-triggered fire), [01-core](../01-core/index.md), [02-explosives](../02-explosives/index.md) (EMP blast).

## Open questions

- Which handheld tools beyond the radar gun, remote detonator and defuser are kept: to settle in the I4 sheet.
- SAM item stats and the battery's capacity: to settle in the I4 sheet.
