# Outputs

- **ICBM baseline:** [launcher-connector-and-network](../../icbm-spec/04-launch-control/launcher-connector-and-network.md)
- **Decisions:** [D13](../DECISIONS.md), [D14](../DECISIONS.md), [D18](../DECISIONS.md), [D124](../DECISIONS.md)
- **Roadmap:** [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense)

How each ICBM machine talks to the rest of the base: redstone, GT covers, data sticks and OpenComputers. This sheet lists what the code does, as integrated on I4.

## Redstone

| Machine | Input | Output |
|---|---|---|
| Launch Control (20140) | A rising edge fires once ([launch-control-block](../04-launch-control/launch-control-block.md#triggers)) | None |
| Missile Silo (20141) | None (the Launch Control fires it) | None |
| Radar HV / EV, Radar Tower (20142 to 20144) | None | Strength 1 to 15 on every side: the number of incoming contacts (predicted impact inside the protected radius), capped at 15; 0 when none. Updated each scan (every second) and only on change |
| Air-Defense Battery (20145) | None | 15 on every side while a linked-radar threat is within 256 blocks, else 0 (also while auto-engage is off); the active flag follows it |
| EMP Tower (20146) | A rising edge fires one pulse through the same gate as the Fire button; a rising edge of GT's work-allowed flag (a machine controller cover) does the same | None; the active flag is on while the tower holds the pulse cost and is not cooling down, so a machine-state cover can read "ready" |

GT hands a machine's per-side strength to the world only when the machine overrides `allowGeneralRedstoneOutput()` to true (default false). The radars, the Radar Tower and the battery do, so a lamp or comparator beside any side sees the strength above; the scenarios `icbm.defense.intercept` and `icbm.defense.tower_detects` read the world's power at a block beside the machine, not the machine's internal value. The Launch Control, the silo and the EMP tower output no strength, so they leave it off.

## GT covers

| Machine | Covers |
|---|---|
| Launch Control | The machine controller cover drives "allowed to work"; a rising edge fires once. All other GT covers behave as on any GT machine. |
| Missile Silo | Normal GT multiblock behavior: the controller cover works on the controller, the hatches take their usual covers. |
| Radar, battery, EMP tower | Radar and battery: a machine-state cover reads the active flag (threat present / engaging). EMP tower: a controller cover fires on a rising edge; a machine-state cover reads "ready". All other covers behave as on any GT machine. |

## Data sticks (D13)

| Machine | Link |
|---|---|
| Launch Control | Load and Save of a target (coordinates and dimension) to a GT data stick, in its one slot. A stick from another dimension is refused. |
| Missile Silo | None. It is fired by a Launch Control. |
| Air-Defense Battery | Listens to one radar: the stick in its slot carries the radar's position and dimension, written at the radar (see Radar below). A lost, unloaded or offline radar shows `link lost`. |
| Radar, Radar Tower | Right-click the radar or the tower controller with a data stick, or press Save link in a single block's GUI (it has one stick slot); the stick keeps dimension and position, and the battery reads it. The EMP tower has no stick. |

The handheld tools do **not** use sticks. The remote detonator links by sneak-clicking a warhead ([handheld-tools](handheld-tools.md)).

## OpenComputers (D18)

Every ICBM machine except the radar-gun and handheld tools is an OpenComputers component through an Adapter. The component table, the methods and the signals are in [golden-dome.md](../01-core/golden-dome.md#components).

| Machine | Component | State |
|---|---|---|
| Launch Control | `gg_launch_control` | Built (`LaunchEnvironment`): `launch`, `canLaunch`, `setTarget`, targets from a stick; signals on transitions |
| Missile Silo | `gg_silo` | Built (`LaunchEnvironment`); no stick slot |
| Radar | `gg_radar` | Built (`RadarEnvironment`) over `RadarView`, implemented by `MTERadar` and `MTERadarTower`: `getInfo`, `getContacts`, `getIncoming`, `setProtectedRadius`; signals `radar_contact`, `radar_incoming`, `radar_lost` |
| Air-Defense Battery | `gg_air_defense` | Built (`AirDefenseEnvironment`) over `AirDefenseView`, implemented by `MTEAirDefense`: `getInfo`, `getSamCount`, `fireAt`, `setAuto`; signals `sam_fired`, `sam_hit`. `setAuto(false)` holds fire until `fireAt`; the flag and the fired and hit counters are saved. `fireAt` runs the checks of the automatic engagement: linked online radar, the contact is in its incoming list, within 256 blocks, a SAM, EU, cooldown |
| EMP Tower | `gg_emp_tower` | Built (`EmpTowerEnvironment`): `fire` goes through the tower's own gate; signals `emp_ready`, `emp_fired` |

OpenComputers types stay behind `@Optional`, so without the mod the machines are unchanged.
