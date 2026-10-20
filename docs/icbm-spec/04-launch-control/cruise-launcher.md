# Cruise Launcher

A single-block launcher with a rotating barrel that fires a missile in a direct line at a target, with a fuel limit. Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Classes in `content/blocks/launcher/cruise/`. It implements the same launcher API as the pad ([launch-sequence.md](launch-sequence.md)) but with different rules. Flight (`DirectFlightLogic`) is `03-missiles`.

## 1. Identity

| Item | Value |
|---|---|
| Classes | `BlockCruiseLauncher`, `TileCruiseLauncher`, `CLauncherCapability`, `RadioCruise`, `TESRCruiseLauncher` (with `CruiseLauncherTopModel`, a model class outside this area), `gui/ContainerCruiseLauncher`, `gui/GuiCruiseLauncher`, `gui/LaunchButton` |
| Registry name | block `icbmclassic:cruiselauncher`; tile `icbmclassic:cruiseLauncher` (capital L; the block name is lowercased by the base class) |
| English name | "Cruise Launcher" (`tile.icbmclassic:cruiselauncher.name`); tooltip "Dummy-fires a missile without target data" (misleading, ICBM-04-28); GUI title "Cruise Launcher" (`gui.launcher.cruise.name`) |
| Block properties | ICBM block base: iron, hardness 10, resistance 10, `rotation` property (placer's horizontal facing, in meta), not opaque, not a full cube; connects to redstone on all sides; drops its inventory on break |
| Oredict | none |

## 2. Obtaining

Shaped, 2 rows (`recipes/machine/cruiselauncher.json`): `C L P` over `S B S`; C = `circuitAdvanced`, L = `icbmclassic:launcherbase`, P = `plateSteel`, B = `icbmclassic:launcherscreen`, S = `ingotSteel`. Result 1. Costs one pad and one screen. Recipes belong to `06-blocks-items`.

## 3. Numbers

| Name | Value | Notes |
|---|---|---|
| Energy buffer / per launch | 1,200 FE / 1,200 FE | the shared launcher config |
| Min range | 20 blocks | the 3D distance from the block center to the target; hard-coded, TODO |
| Max range | none | `max_range` is not read (ICBM-04-30) |
| Missile spawn point | block center, 2.0 above the base | constant `MISSILE__HOLDER_Y` |
| Barrel rotation speed | 10 (degrees per unit of "delta time") | see ICBM-04-21 |
| Aim tolerance | 0.01 degrees on yaw, pitch and roll | |
| Redstone poll | every 40 ticks | |
| Redstone threshold | signal strength above 1 (2 or more) | |
| Cruise fuel | 200 ticks | `ConfigMissile.CRUISE_FUEL`, config `icbmclassic/missile`, key `cruise_fuel`; "ticks before a missile fired from a cruise launcher starts to fall out of the air" |
| Missile speed | direct-fire speed 2 m/tick (SAM missile uses the SAM speed) | `speed_direct_fire`; so the nominal reach is about 400 blocks, which is `03-missiles` to confirm |
| Render box | block position -2..+2 in X/Z, 0..3 in Y | |

## 4. Behavior

### Server tick

1. Compute `deltaTime` from the real clock: the nanoseconds since the last update divided by 100,000,000. A normal 50 ms tick gives 0.5.
2. Move the barrel: the current aim moves toward the wanted aim by `10 * deltaTime` degrees per axis (snapping if closer than one step; wrap-around handled), then is clamped to 0..360. At 20 TPS this is 5 degrees per tick (100 degrees per second); a lagging server turns the barrel faster per tick.
3. Every 40 ticks, for each of the six sides: if the redstone power received from that side's neighbor is above 1, create a **firing package** for the current target with a redstone cause and a countdown of 0. This **replaces** any package already waiting, including one queued by radio (ICBM-04-19).
4. If a package exists and the barrel is aimed (within 0.01 degrees): count down one; at 0, launch through the capability and clear the package.

The block does not use the network list for itself: it is a network node (it accepts items) and the screen can fire it (see below).

### Aiming

The wanted aim comes from `Pos(center + (0.5, 2.0, 0.5)).toEulerAngle(target)` (yaw and pitch from the barrel pivot to the target, clamped to 0..360). Recomputed whenever the target changes. With no target, the wanted aim is (0, 0, 0). The "has target" test compares object identity against the shared zero vector, so a zero target read back from NBT or sent over the network counts as a real target at world origin (ICBM-04-06). On load, the current aim snaps to the wanted aim.

### Launch (`CLauncherCapability`)

**Pre-check:** target null: `ERROR_TARGET_NULL`; closer than 20 (3D): `ERROR_MIN_RANGE`; then status.

**Status order:** power (`ERROR_POWER`); a package exists and the barrel is not aimed (`FIRING_AIMING`, blocking, not an error); no missile (`ERROR_EMPTY_STACK`); a package with a positive countdown (`FiringWithDelay`); no space above (`ERROR_SPAWN_COLLIDER`); else `READY`.

**Space check:** a 3 x 3 x 2 volume (x and z -1..+1, y +1..+2 above the block). The loop walks that volume and, on the first non-air block it meets, returns the verdict for that block alone: free if the block has no collision box or its box is under half a block tall, else blocked. Later blocks are never examined (ICBM-04-11).

**Launch:**
1. Pre-check. If blocking, return it. If simulating, return `LAUNCHED`.
2. Set the launcher's target to the requested one (so the barrel turns).
3. If the barrel is not aimed: store a package (target, cause, countdown 0) and return `FIRING_AIMING`; the tick loop fires it once aimed.
4. Else, on the server: build the missile from the stack, move it to (block center x, y + 2.0, block center z), **consume** the missile, re-check and pay power (consumed before the check, ICBM-04-07), set the missile's source (a block cause, previous cause = the caller's), set the target **exactly** as given (no inaccuracy), flight logic = direct flight with `cruise_fuel` ticks, call `launch()`, spawn. A world that refuses the spawn gives `ERROR_SPAWN` with the missile gone. Success returns `LaunchedWithMissile`.

The cruise launcher has no firing delay, no groups, no warm-up and does not carry riders. The missile gets no initial rotation; direct flight sets its own.

### Redstone, radio and GUI

- Redstone: the section above. Level-triggered, repeating every 40 ticks while a missile and power are present.
- Radio: [launch-sequence.md](launch-sequence.md#5-radio-firing). Registered on the radio bus on load (server) and removed from it on invalidate; but invalidate does not detach the tile from the launcher network (ICBM-04-10).
- GUI launch button: sends `launch`; the server fires immediately with the player as the cause. Feedback is the button state (the tooltip shows the pre-check message, evaluated client side with client-side data).
- Screen: a cruise launcher touching a screen network is fired with the rest. The solution's count is ignored and the ballistic range rules do not apply to it.

### Interactions

Right click with a GPS item applies the item's data as the target; otherwise opens the GUI. No missile hand-loading; load through the GUI or an item handler.

### Capabilities (*1.12-only mechanism*)

Item handler (2 slots: missile at 0, battery at 1), missile holder, missile launcher, radio, and Forge Energy (own buffer) when `use_energy` is on.

## 5. Persistence and sync

NBT (save handler): `inventory`, `radio`, `target`, `current_aim` (a euler angle), `firing_package`, `energy`, `launcher` (the capability, UUID). Legacy `frequency` int is converted to a channel string.

| Packet | Declared direction | Fields |
|---|---|---|
| `description` | server to client | held missile stack, target, current yaw, current pitch |
| `gui` | declared client to server, **also sent to viewers every 3 ticks** | energy, channel, disabled, target |
| `radio.frequency`, `radio.disable`, `target`, `launch` | client to server | channel; toggle; target; none |

The description packet is sent on `markDirty` (inventory, energy, target changes), not each tick. The server rotates the barrel every tick but the client does not (the client's update only measures time), so other players see the barrel at the angle of the last description packet (ICBM-04-22, unverified in game). Because the direction flags are not enforced, a modified client can send the `gui` packet and write **energy** into the launcher (ICBM-04-12).

## 6. UI

Texture `textures/gui/gui_cruise_launcher.png` (256 x 256; also the source of the launch button sprites for the screen GUI, see [launch-screen.md](launch-screen.md)). Container: missile slot (88, 47), battery slot (141, 47), inventory (7, 84). Shift-click works correctly here (unlike the pad).

| Control | Position (x, y, w, h) |
|---|---|
| Target field "x, y, z" | (18, 17, 100, 12) |
| Channel field | (135, 17, 34, 12) |
| Radio disable button | (119, 16) |
| Launch button | (24, 38) 28 x 29, enabled when the pre-check is not blocking |
| Energy bar | (141, 66), with the firing cost |
| Tooltips | target (2, 16, 14, 14), radio (119, 16, 14, 14) |

Title text at (52, 6) in gray, "Inventory" label at (8, ySize - 92). The constructor sets `height`/`width` rather than `xSize`/`ySize` (harmless, ICBM-04-29).

## 7. Visuals

- Block model `models/block/cruise_launcher.json` (credit "QueenOfMissiles"; texture size 32 x 32, `blocks/cruise_launcher.png` 32 x 32): a 16 x 4 x 16 base plate, three splayed legs, a console with a monitor and a small antenna. `blockstates/cruiselauncher.json` rotates by `rotation` (north 0, south 180, east 90, west 270).
- The rotating barrel is a TESR: `CruiseLauncherTopModel` with `textures/models/cruise_launcher_top.png` (64 x 64), drawn at 2.5 above the block, turned by the tile's current yaw and pitch; the held missile is drawn through the shared missile renderer at 2.0 above, with a pitch offset of -90. A debug method that draws a sphere of missiles is left in the renderer, unused.

## 8. Audio

None here (the missile's launch sound is `03-missiles` / `07-visuals-audio`).

## 9. Relationships

- [launch-sequence.md](launch-sequence.md): API and radio; [launcher-connector-and-network.md](launcher-connector-and-network.md): shared energy and items.
- `03-missiles`: `DirectFlightLogic`, `CRUISE_FUEL`, `ConfigMissile`.
- `01-core`: euler angle math (`moveTowards`, `isWithin`, `Pos.toEulerAngle`), radio bus.

## 10. Bugs

ICBM-04-06, -07, -10, -11, -12, -13, -19, -21, -22, -28, -29, -30.

## Lang

`tile.icbmclassic:cruiselauncher.name` / `.info`; `gui.launcher.cruise.name`; shared radio, target and energy tooltips (see [launch-screen.md](launch-screen.md)); status strings (see [launch-sequence.md](launch-sequence.md)).
