# 04-launch-control

## Overview

How an ICBM base fires: the **launcher base** (ballistic pad), the **frame** and **connector** blocks, the **launcher screen**, the shared **launcher network** (energy, inventory, selection), the **cruise launcher**, and the **launch sequence** from "fire" to the missile entity spawning (checks, inaccuracy, delay, radio and redstone, the launcher API). Missile flight, the radio bus, radar and SAM triggering are other areas and are only linked. Source: ICBM-Classic `prod/1.12` @ `9c1d985a`; cite classes relative to `src/main/java/icbm/classic/`.

## Contents

- [launch-sequence.md](launch-sequence.md): triggers, the pre-checks, the inaccuracy model (MIT-portable), firing and flight assignment, radio firing, the launcher/solution API, the firing package, status and lang.
- [launcher-base.md](launcher-base.md): the pad: facing, spawn offsets, inventory, GUI, seat, persistence.
- [launcher-frame.md](launcher-frame.md): the frame block, its computed state, and **the launcher layout** (the exact footprint and frame height rules: there are none enforced).
- [launcher-connector-and-network.md](launcher-connector-and-network.md): the connector, how networks form and merge (MIT-portable), shared energy and inventory, grouping and selection.
- [launch-screen.md](launch-screen.md): the control block: target, channel, status list, redstone, `fireAllLaunchers`, GUI.
- [cruise-launcher.md](cruise-launcher.md): the rotating-barrel direct-fire launcher.

## Key facts

- **No multiblock is enforced.** Pad, frames, connectors, screen and cruise launcher link by **face adjacency** into a network. Nothing checks a footprint, a frame height, or an empty shaft above the pad. The frame's lattice is a hollow shaft around the missile; its three looks (bottom plate, middle, open top) are purely visual.
- A fire mission offers one solution to **every** launcher on the network. Group id and index are editable on the pad but nothing reads them, so selection by group does not work.
- Ballistic range is a **square**: min 10 and max `max_range` (10,000) are tested per axis and ignore Y. The cruise launcher has a 20-block 3D minimum and no maximum.
- Inaccuracy radius = 2 + 10 * (distance / range)^2 + (launchers - 1). The "10 over the range" term is quadratic, not linear. Launchers are counted whether or not they can fire. A delayed shot is recomputed at fire time with a count of 1. The offset is uniform in radius (clusters at the center) and the angle helper is slightly wrong in Z.
- Power: 1,200 FE per shot and a 1,200 FE buffer per launcher. Energy is **per launcher**; the network only splits incoming energy among launchers. The screen's own battery slot and buffer power nothing.
- A screen with the default target fires at world origin (0, 0, 0). A redstone-powered cruise launcher with no target does the same.
- The server accepts every launcher packet from any client, with no distance or open-container check, and the "direction" flags on the packets are never enforced. A client can fire any loaded screen or cruise launcher, change its target, and write energy into a cruise launcher.
- The missile is consumed before the spawn and the power check, so a failed spawn loses the missile.
- Capabilities, blockstates, packet codex classes and Forge Energy are *1.12-only mechanisms*. The sheets describe the behavior they produce.

## Related areas

| Area | Why |
|---|---|
| `01-core` | Radio bus and message classes (`ITargetMessage`, `ITriggerActionMessage`, `FakeRadioSender`), the energy buffer and `use_energy`, the packet codex (no sender checks), `EulerAngle` (the `z()` bug), the GPS data helpers, config groups |
| `02-explosives` | Warheads that the launched missile carries |
| `03-missiles` | Missile entity and stack capability, `WarmupFlightLogic`, `MoveByFacingLogic`, `ArcFlightLogic`, `DirectFlightLogic`, `BallisticTargetingData`, `ConfigMissile` (`cruise_fuel`, `speed_direct_fire`), the seat entity |
| `05-detection-defense` | Radar station sends the "incoming missile" message that retargets and fires a screen; laser detonator sends target-and-trigger; the radar recipe uses the screen |
| `06-blocks-items` | Recipes, GPS items, the block base class (hardness) |
| `07-visuals-audio` | Full texture and model catalog, the missile renderer and the cruise barrel model |

## Coverage

Source files read in full: 44 of 44 in scope.
- `content/blocks/launcher/`: `base/` 6 (`BlockLauncherBase`, `TileLauncherBase`, `LauncherCapability`, `TESRLauncherBase`, `gui/` 2), `connector/` 2, `cruise/` 8 (incl. `gui/` 3), `frame/` 4, `network/` 6, `screen/` 7 (incl. `gui/` 2), `status/` 3, and the top-level `FiringPackage`, `LauncherBaseCapability`, `LauncherSolution`, `LauncherLangs`.
- `content/blocks/multiblock/TileMulti` 1 of 1 (a deprecated stub).
- `api/launcher/` 2 of 2 (`ILauncherSolution`, `IMissileLauncher`).
- `config/machines/ConfigLauncher` in full. `ConfigRadar` and `ConfigEmpTower` belong to [05](../05-detection-defense/index.md) and were not re-read.

Also read, to resolve references: `api/events/LauncherEvent`, `LauncherSetTargetEvent`; `lib/capability/launcher/CapabilityMissileLauncher`, `CapabilityMissileHolder`; `lib/radio/imp/RadioTile`; `api/radio/messages/` (the three interfaces used); `prefab/tile/BlockICBM`, `TileMachine`; `lib/tile/TickAction`; `lib/network/lambda/PacketCodex` (direction flags, send helpers), `PacketLambdaTile` (server handling), `PacketCodexReg`; `lib/energy/storage/EnergyBuffer` (`consumePower`, `setEnergyStored`); `lib/transform/rotation/EulerAngle` (`x/y/z`, `moveTowards`, `moveToAngle`); `api/actions/status/IActionStatus` and `ActionStatus` (blocking rule); `ConfigMissile` (three keys); `ConfigMain.REQUIRES_POWER`.

Assets at catalog depth:
- GUI textures `gui_silo_base.png`, `gui_silo_screen.png`, `gui_cruise_launcher.png` (256 x 256; control positions given by component coordinates in the sheets, not measured against the PNGs).
- Block textures: `silo_pad/top|side|bottom` (16 x 16), `silo_frame/frame|base_bottom|base_side|base_top` (16 x 16), `silo_connector/connected|disconnected` (16 x 16), `screen/silo` (16 x 16), `machine_green` (16 x 16, shared), `cruise_launcher` (32 x 32); entity-model texture `models/cruise_launcher_top.png` (64 x 64).
- Block models `silo_pad`, `frame_bottom|middle|top`, `launcher_connector`, `silo_screen`, `cruise_launcher`; blockstates `launcherbase`, `launcherframe`, `launcher_connector`, `launcherscreen`, `cruiselauncher`.
- Recipes (5): base, connector, screen, frame (`support.json`), cruise launcher.
- Lang keys in the sheets.

Not covered: the `EntityPlayerSeat` class, `RenderMissile`, `CruiseLauncherTopModel`, `FaceRotationButton`, `SlotEnergyBar`, `TextInput`, `ContainerBase`, `GPSDataHelpers`, the cause classes (`RedstoneCause`, `EntityCause`, `CausedByBlock`), `ActionSource`, `BallisticTargetingData` and the flight logic classes (all other areas or shared UI libraries). Other-language lang files, texture pixel content, and the missile launch sound.

## Known limits

- No code was run. Timings, offsets and counts come from reading the source; the worked inaccuracy figures are arithmetic on the default config.
- The effect of an obstructed shaft on a flying missile, and whether the seat entity persists across a world reload, were not checked (`03-missiles`).
- Whether a redstone block (weak power only) fails to trigger a screen, and what other players see of a rotating cruise barrel, are read from the code and not tested in game.
- The generic packet handler is `01-core`; ICBM-04-13 describes its effect on this area only.
- The recipes use GT-compatible oredict names that the pack changes (`docs/gregguns-icbm-spec`).

## Suspected bugs

"Logic" means we could inherit it in a rebuild. "1.12-only" means it does not matter to a 1.7.10 rebuild.

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-04-01 | Launcher groups are dead (logic) | `content/blocks/launcher/base/TileLauncherBase.java:101-106`, `LauncherCapability.java` (no override), `api/launcher/IMissileLauncher.java:87,99`, `network/LauncherNetwork.java:50`, `screen/TileLauncherScreen.java:145-147` | Group id and index are saved, synced and editable, but neither launcher overrides `getLauncherGroup` / `getLaunchIndex` (default -1) and the screen always selects group -1. Group selection and chain fire never happen | gameplay |
| ICBM-04-02 | Delayed shots lose the cluster penalty (logic) | `content/blocks/launcher/FiringPackage.java:58`, `base/LauncherCapability.java:136` | The package re-launches through a one-line solution whose firing count is the API default, 1. A pad with a firing delay therefore applies no per-launcher inaccuracy, while the screen's accuracy readout includes it | gameplay |
| ICBM-04-03 | Launcher count includes launchers that cannot fire (logic) | `screen/TileLauncherScreen.java:115,239` | The count passed for the "+1 per launcher" term is the size of the group, ready or not, so empty or unpowered pads make everyone less accurate. The API doc for the same field says "-1 to fire all" and the pad also reads it as a cluster size (two meanings) | gameplay |
| ICBM-04-04 | Direction helper is wrong in Z (logic, `01-core`) | `lib/transform/rotation/EulerAngle.java:438-441` (used at `LauncherCapability.java:293`) | `z()` takes the sine of `-cos(yaw) * cos(pitch)` instead of the product itself. At pitch 0, z spans about -0.84..0.84 while x spans -1..1, so the miss scatter is squashed by up to 16% along Z | gameplay |
| ICBM-04-05 | Range is a per-axis square with a hard-coded minimum (logic) | `base/LauncherCapability.java:302-315` | Min (10, a TODO) and max (`max_range`) are tested on X and Z separately and ignore Y, so the real limits are a square, while inaccuracy uses true 3D distance (corner targets reach scale 2, radius 22). The min-range square is only 20 x 20 | gameplay |
| ICBM-04-06 | The default target is a legal target (logic) | `screen/TileLauncherScreen.java:65`, `base/LauncherCapability.java:86`, `cruise/TileCruiseLauncher.java:76,191,261-264` | The stored target defaults to (0, 0, 0); the only "no target" check is for null. A screen fired by redstone or radio with no target launches at world origin if it is in range. The cruise launcher's `hasTarget` compares object identity with the zero constant, so a zero loaded from NBT or a packet counts as a real target | gameplay |
| ICBM-04-07 | Missile is consumed before spawn and power check (logic) | `base/LauncherCapability.java:172-200`, `cruise/CLauncherCapability.java:114-122` | Pad: the missile is consumed, spawned and launched, then power is re-checked, so a failure returns `ERROR_POWER` with the missile already flying and unpaid. Cruise: the missile is consumed before the power re-check and before the spawn, so `ERROR_POWER` or `ERROR_SPAWN` destroys the missile with no launch. Both re-checks repeat a test passed earlier in the same tick, so these are mostly latent | gameplay |
| ICBM-04-08 | Result of a delayed launch is discarded (logic) | `base/TileLauncherBase.java:234-240`, `FiringPackage.java:58-61` | After the countdown the status of the real launch goes only to an optional callback (none for pads). If power, target or missile changed during the delay, the shot silently fails and the package is cleared | gameplay |
| ICBM-04-09 | Screen reports failure for delayed and aiming shots (logic) | `screen/TileLauncherScreen.java:240-244`, `status/FiringWithDelay.java:39`, `screen/RadioScreen.java:46-55` | Success is "at least one non-blocking status"; `FiringWithDelay` and `FIRING_AIMING` are blocking. A mission where every pad has a firing delay (or the cruise launcher is still turning) replies "Failed to fire missile(s)" although shots are queued | cosmetic |
| ICBM-04-10 | Cruise launcher never leaves the network on unload (logic) | `cruise/TileCruiseLauncher.java:157-164` | `invalidate` removes the radio entry but not the network node (the pad, screen and frame all detach). After a chunk unload or non-break removal the network keeps a stale launcher entry; a fire mission can call into it, and a failed spawn there loses the missile (ICBM-04-07). The block's break handler does detach | gameplay |
| ICBM-04-11 | Cruise clearance check judges only the first block (logic) | `cruise/TileCruiseLauncher.java:266-284` | The loop returns on the first non-air block it meets, in x/z/y scan order. A carpet or torch (no box or box under half a block) hides a full block later in the volume, and a full block hides everything. The check is therefore unreliable in both directions | gameplay |
| ICBM-04-12 | Cruise `gui` packet lets a client write energy (1.12-only, but the design must be fixed) | `cruise/TileCruiseLauncher.java:410-415` | The packet is declared client to server and its setters include the energy buffer. The server applies it (clamped to capacity), so any client can fill the launcher's buffer | gameplay |
| ICBM-04-13 | Launcher packets are unauthenticated (1.12-only mechanism; logic of trust) | `lib/network/lambda/PacketCodex.java:52-86`, `PacketLambdaTile.java` (server handler), launcher tiles' packet fields | The direction flags are written but never read, and the server handler checks only dimension and "chunk loaded". There is no distance, open-container, ownership or cooldown check. Any client can send `launch`, `target`, `radio.*` to any loaded screen or cruise launcher in its dimension, and the pad's server-to-client packets are accepted too | gameplay |
| ICBM-04-14 | Pad shift-click uses the wrong slot test (logic) | `base/gui/ContainerLaunchBase.java:39` | `slotIndex > 0` treats the battery slot (index 1) like a player slot, so shift-clicking the battery out does nothing useful. The cruise container uses `> 1` correctly | cosmetic |
| ICBM-04-15 | Screen energy buffer and battery slot are dead (logic) | `screen/TileLauncherScreen.java:67-72,350-374`, `screen/gui/GuiLauncherScreen.java:64` | The screen charges its own buffer from the slot, but nothing reads it, and with a network its energy capability is the network's. The GUI bar shows the unused buffer. A battery put in the screen does not power any pad. The source has a TODO for a combined wrapper | gameplay |
| ICBM-04-16 | "No launchers connected" message key has a missing dot (logic) | `LauncherLangs.java:17` | The key is built as `error` + `network.launchers`, giving `...errornetwork.launchers`, which is not in the lang file, so the raw key shows | cosmetic |
| ICBM-04-17 | Status keys missing from `en_US.lang` (logic) | `LauncherLangs.java:21,23,25` | `.error.missile.invalid`, `.error.missile.queued` and `.error.group.empty` are not defined. The queued one is shown on a radio reply | cosmetic |
| ICBM-04-18 | Leftover debug right click dereferences a null network (logic) | `base/BlockLauncherBase.java:61-66`, `screen/BlockLaunchScreen.java:57-61`, `frame/BlockLaunchFrame.java:66-73`, `connector/BlockLaunchConnector.java:101-108` | Holding a stone axe prints network info; with no network the `getLaunchers()` call throws a null pointer on the server. Debug code shipped in four blocks | cosmetic |
| ICBM-04-19 | Cruise redstone overwrites a queued shot and needs strength 2 (logic) | `cruise/TileCruiseLauncher.java:187-194` | Every 40 ticks, any side with power above 1 builds a new package and replaces the existing one (and its cause); a radio-queued shot is overwritten. Signal strength 1 does nothing. A held signal fires a missile every 2 seconds | gameplay |
| ICBM-04-20 | Screen redstone is strong-power, level-triggered (logic) | `screen/TileLauncherScreen.java:130` | Uses strong power at the block (not "any redstone power", unlike the cruise launcher) and fires every 3 ticks while high; the source marks it TODO (pulse, delay). Weak-only sources may not trigger it (unverified) | gameplay |
| ICBM-04-21 | Cruise barrel speed depends on wall-clock time (logic) | `cruise/TileCruiseLauncher.java:178-184` | `deltaTime` is real nanoseconds over 1e8, which is 0.5 at 20 TPS while the field comment says 1.0 is normal. Rotation per tick scales with real elapsed time, so lag or a paused server changes it; the first tick after load uses the time since the tile was constructed | gameplay |
| ICBM-04-22 | Other players may not see the barrel turn (1.12-only; unverified) | `cruise/TileCruiseLauncher.java:172-204,388-393`, `cruise/TESRCruiseLauncher.java:20-22` | Only the server rotates; clients read the barrel angle from the description packet, which is sent on `markDirty`, not per tick. The barrel would appear to jump or lag | visual |
| ICBM-04-23 | Network inventory throws on a bad slot (logic) | `network/NetworkInventory.java:99-103` | An out-of-range slot raises a runtime exception instead of returning empty or refusing, and the slot list is only rebuilt when a launcher joins or the network clears. A hopper or pipe that cached the old slot count can crash the tick. Extraction of missiles and batteries is also open to any adjacent automation | crash |
| ICBM-04-24 | Network energy split is lossy and arbitrary (logic) | `network/NetworkEnergyStorage.java:19-36` | Each pad is offered `floor(left/remaining)+1` in hash order, and energy refused by a full pad is not re-offered. A network with mixed fill levels accepts less than it could in one call | cosmetic |
| ICBM-04-25 | Seat cleanup targets the wrong entity (logic) | `base/TileLauncherBase.java:254-258` | When the missile is removed it calls `removePassengers` on whatever the seat is riding (`getRidingEntity`), not on the seat's passengers. Killing the seat dismounts riders anyway, so the visible effect is nil | cosmetic |
| ICBM-04-26 | Pad launches into a blocked shaft (logic) | `base/LauncherCapability.java:180-183` | No raytrace or clearance check (TODO) on the spawn point or the path; the spawn offset is fixed. Only the cruise launcher has a clearance test (and it is flawed, ICBM-04-11) | gameplay |
| ICBM-04-27 | Missile insertion fails silently (logic) | `base/TileLauncherBase.java:325-337`, `:77-83` | While a missile entity is in the 1 x 5 x 1 box above the pad, the insert is refused by the slot, yet the click is reported handled, so the GUI does not open and no message is sent | cosmetic |
| ICBM-04-28 | Tooltips describe the wrong thing (logic) | `lang/en_US.lang` (lines for `launcherframe.info`, `launcher_connector.info`, `cruiselauncher.info`) | The frame and connector are called "decorative" although they carry the network, energy and items. The cruise launcher is described as dummy-firing "without target data", though it requires a target | cosmetic |
| ICBM-04-29 | Cruise GUI sets the wrong size fields (1.12-only) | `cruise/gui/GuiCruiseLauncher.java:33-34` | Sets `height`/`width` (screen size, overwritten on init) instead of `ySize`/`xSize`; harmless in practice | cosmetic |
| ICBM-04-30 | Cruise launcher ignores max range and inaccuracy (logic) | `cruise/CLauncherCapability.java:62-73,98-133` | `max_range` and the inaccuracy settings are not applied; only the 3D minimum of 20. Reach is limited only by missile fuel. This may be intended, but the config text says "Range of silo launcher" and the docs do not say it is pad-only | gameplay |
| ICBM-04-31 | PreLaunch event fires twice for delayed shots (logic) | `base/LauncherCapability.java:120-127`, `FiringPackage.java:58` | The event is posted when the shot is queued and again when it fires, and it also fires for simulated calls. A listener that counts or charges per event will double-count | cosmetic |

Minor, not given IDs: the `FiringPackage.update` method is an unfinished stub that is never registered; `LauncherNetwork.mergeNetwork` leaves the other network object stale (harmless); the network and packet code carry an unused duplicate `target` field in the screen sync packet; `ERROR_QUEUED`, `ERROR_EMPTY_GROUP`, `ERROR_NO_NETWORK`, `ERROR_NO_FUEL` and `ERROR_TARGET_ANGLE` are defined and never produced; the pad's GUI title ("Launcher Platform") differs from its block name ("Launcher Pad"); the `TileMulti` multiblock tile is registered but unused.
