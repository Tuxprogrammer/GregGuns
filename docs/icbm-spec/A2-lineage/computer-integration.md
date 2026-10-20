# Computer integration: 1.6.4 peripherals and the emptower component

## Overview

How the older ICBM lines let a computer drive launchers, radars and the EMP tower. Three surfaces exist, and 1.12 has none of them in this form:

1. **ComputerCraft peripherals** in Calclavia's 1.6.4 (`ICBMLauncher`, `ICBMRadar`). Poll-only.
2. **An OpenComputers component** `emptower` on the 1.6.4 EMP tower.
3. **VoltzEngine DataSystem methods** in 1.7.10 (named for context only; the ComputerCraft/OC glue lives in VoltzEngine, not in ICBM).

This sheet feeds `docs/gregguns-icbm-spec/01-core/golden-dome.md`. Sources: Calclavia 1.6.4 (`explosion/.../machines/launcher/TileLauncherPrefab.java`, `TileRadarStation.java`, `TileEMPTower.java`, `TileCruiseLauncher.java`; LGPL, described only) and ICBM-Classic `prod/1.7` @ `198999ed` (no license, described only; nothing here is portable code). All four 1.6.4 files were read for the parts below. 1.12 reference: the `IMachineInfo` interface (see the end).

Common to every 1.6.4 peripheral: `attach` and `detach` are empty, and no event is ever queued to a computer. A script must poll.

## Launcher peripheral: type `ICBMLauncher`

Lives in the shared launcher prefab, so every launcher that extends it exposes it (the cruise launcher is verified; the silo launcher screen is assumed to inherit the same prefab, unverified). Methods are looked up by index in the order below.

| Method | Arguments | Returns | Effect |
|---|---|---|---|
| `launch` | none | nothing | Calls the tile's `launch()`. For the cruise launcher this fires only when `canLaunch` would be true; otherwise a silent no-op. On success it takes one missile from slot 0, sets stored energy to 0, and launches at the stored target |
| `getTarget` | none | three numbers: x, y, z | Reads the stored target. If none was ever set it defaults to the launcher's own position (cruise: its own Y; others: Y 0) |
| `setTarget` | x, y, z (numbers) | nothing | Floors all three and stores them (NBT compound `target`). A wrong type raises "Target Parameter is Invalid." (and prints a stack trace on the server). Fewer than three arguments raises a Java index error, not a friendly message. If any given arg is nil the call is silently ignored |
| `canLaunch` | none | boolean | Cruise rule set: missile cached, missile in slot 0 matches, that explosive is cruise-capable and tier <= 3, the energy buffer is **full**, and the target is at least 8 blocks away horizontally |
| `setFrequency` | n (number) | nothing | Clamps to the short range (-32768..32767), stores it as the launcher's frequency (the radar and screen pair by frequency). Wrong type raises "Frequency Parameter is Invalid." |
| `getFrequency` | none | number | Current frequency |
| `getMissile` | none | string, or nothing | Missile name of the loaded missile, or nothing when empty |

Any other index raises "Invalid ICBM Launcher Function."

## Radar peripheral: type `ICBMRadar`

On the radar station tile. Both methods first check that the buffer can pay a pulse; if not, they raise "Radar has insufficient electricity!". They do not consume the pulse themselves.

| Method | Arguments | Returns | Effect |
|---|---|---|---|
| `getEntities` | none | one table keyed `x_0, y_0, z_0, x_1, ...` (doubles) | Every entity the radar registry holds within the current **alarm range** of the station (default 100). One table, three keys per entity, N counts from 0. There is no count field |
| `getBlocks` | none | same key shape | Every registered tile entity (radar-visible machines) in the square of side 2 x 500 centered on the station (`MAX_DETECTION_RANGE` 500). Integer coordinates returned as doubles |

Any other index raises "Invalid ICBM Radar Function." The flat string-keyed table is the awkward part: a script must loop until a key is missing. A rebuild should return an array of records with a count.

## EMP tower: OpenComputers component `emptower`

The tower implements OpenComputers' `SimpleComponent` (component name `emptower`). Every method is `@Callback` with plain Java types; no events. No ComputerCraft peripheral exists for it.

| Method | Arguments | Returns | Effect |
|---|---|---|---|
| `isReady` | none | boolean | Cooldown <= 0 (does **not** check power) |
| `getCooldown` | none | int ticks | Remaining cooldown |
| `getMaxCooldown` | none | int | Constant 120 ticks |
| `fire` (`limit = 1`) | none | boolean | If the buffer can pay **and** the cooldown is over: runs an EMP blast at the tower position with the current radius and mode, extracts the shot's energy, starts the cooldown, returns true. Otherwise false. A redstone power-on calls the same method (rising edge only) |
| `getEmpMode` | none | byte | 0 = blocks and entities, 1 = entities (missiles) only, 2 = blocks (electronics) only |
| `setEmpMode` | byte | nothing | Accepts 0..2; out-of-range values are silently ignored |
| `empAll`, `empMissiles`, `empElectronics` | none | nothing | Shortcuts setting mode 0, 1, 2 |
| `getEmpRadius` | none | int | Current radius (default 60) |
| `getMaxEmpRadius`, `getMaxRadius` | none | int | Constant 150 |
| `setEmpRadius` | int | nothing | Clamped to 0..150; recomputes buffer capacity only when the value changed |

Note: the capacity formula uses integer division and always yields the 1,000,000,000 floor (ICBM-A2-03). `getMaxRadius` is declared static yet tagged as a callback; whether OpenComputers exposes a static method is unverified (ICBM-A2-04).

## Newer lineages (context)

- **1.7.10 DataSystem (VoltzEngine).** The EMP tower registers `icbm.empTower` with `isEmpReady`, `getEmpCooldown`, `getEmpMaxCooldown` (registered twice), `fireEmp`. The 1.6.4 setters for mode and radius were **not** carried over. Launchers register shared methods `getMissileTypeName`, `getMissileType` (explosive ordinal, -1 if none), `getMissileTarget`, `setMissileTarget(x,y,z)`, `canFireMissile`, `fireMissile`, `getFrequency`, `setFrequency` under `icbm.launcher.screen` and `icbm.launcher.cruise`; the launcher base redirects to its screen. The radar registers `incomingMissileData` and `detectedEntityData`, each returning a table with `size`, `dim_N`, `X_N`, `Y_N`, `Z_N`, `VX_N`, `VY_N`, `VZ_N`; both return after the first entity only (ICBM-A2-05). Argument errors come back as error strings, not exceptions. Also a shared `getEnergyUsage`.
- **1.12.** No ComputerCraft or OC code in ICBM. `IMachineInfo.provideInformation` feeds a key/value map (Double, Integer, String or Map values only) so a script can read config-dependent facts: `NEEDS_POWER`, `ENERGY_COST_ACTION`, `ENERGY_COST_TICK`, `ENERGY_RECEIVE_LIMIT`, plus `COOLING_TICKS` and `MAX_RANGE` (EMP tower), `MAX_RANGE` and the three inaccuracy keys (launcher base), `MAX_RANGE` (radar). Implementers: EMP tower, launcher base, cruise launcher, launcher screen, radar. Reading it is a pull, like everything else.

## For GregGuns

Port the method list, not the API: GTNH ships OpenComputers, not the 1.6.4 ComputerCraft. Expose launcher, radar and EMP-tower reads and actions as OC callbacks that use real GT tile state, return proper arrays, and push signals on state change (ICBM never did). See `docs/gregguns-spec/INTEGRATION.md` for the OC API facts.
