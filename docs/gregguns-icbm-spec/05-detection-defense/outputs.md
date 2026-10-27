# Outputs

- **ICBM baseline:** [launcher-connector-and-network](../../icbm-spec/04-launch-control/launcher-connector-and-network.md)
- **Decisions:** [D13](../DECISIONS.md), [D14](../DECISIONS.md), [D18](../DECISIONS.md), [D124](../DECISIONS.md)
- **Roadmap:** [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense)

How each ICBM machine talks to the rest of the base: redstone, GT covers, data sticks and OpenComputers. This sheet lists what is on `master` today and marks what the I4 branches add. A `TODO` row is a promise for a branch that has not merged; the orchestrator fills it at integration.

## Redstone

| Machine | Input | Output |
|---|---|---|
| Launch Control (20140) | A rising edge fires once ([launch-control-block](../04-launch-control/launch-control-block.md#triggers)) | None on master |
| Missile Silo (20141) | None (the Launch Control fires it) | None on master |
| Radar HV / EV, Radar Tower (20142 to 20144) | None | TODO (branch `icbm/i4-detect`): a signal while a threat is present |
| Air-Defense Battery (20145) | TODO (branch `icbm/i4-detect`) | TODO (branch `icbm/i4-detect`) |
| EMP Tower (20146) | TODO (branch `icbm/i4-emp`) | TODO (branch `icbm/i4-emp`) |

## GT covers

| Machine | Covers |
|---|---|
| Launch Control | The machine controller cover drives "allowed to work"; a rising edge fires once. All other GT covers behave as on any GT machine. |
| Missile Silo | Normal GT multiblock behavior: the controller cover works on the controller, the hatches take their usual covers. |
| Radar, battery, EMP tower | TODO (branches `icbm/i4-detect`, `icbm/i4-emp`) |

## Data sticks (D13)

| Machine | Link |
|---|---|
| Launch Control | Load and Save of a target (coordinates and dimension) to a GT data stick, in its one slot. A stick from another dimension is refused. |
| Missile Silo | None. It is fired by a Launch Control. |
| Air-Defense Battery | TODO (branch `icbm/i4-detect`): a radar link by data stick |
| Radar | TODO (branch `icbm/i4-detect`) |

The handheld tools do **not** use sticks. The remote detonator links by sneak-clicking a warhead ([handheld-tools](handheld-tools.md)).

## OpenComputers (D18)

No ICBM machine is an OpenComputers component on `master`. The component table, the methods and the signals are in [golden-dome.md](../01-core/golden-dome.md#components). The I4 branch `icbm/i4-emp` adds them.

| Machine | Component | State |
|---|---|---|
| Launch Control | `gg_launch_control` | TODO (branch `icbm/i4-emp`) |
| Missile Silo | `gg_silo` | TODO (branch `icbm/i4-emp`) |
| Radar | `gg_radar` | TODO (branch `icbm/i4-emp`, reads the contacts API from `icbm/i4-detect`) |
| Air-Defense Battery | `gg_air_defense` | TODO (branch `icbm/i4-emp`) |
| EMP Tower | `gg_emp_tower` | TODO (branch `icbm/i4-emp`) |

OpenComputers types stay behind `@Optional`, so without the mod the machines are unchanged.
