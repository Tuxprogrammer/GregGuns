# Launcher Base (Launcher Pad)

Source root for all paths: `src/main/java/icbm/classic/` at `9c1d985a`. The base is the ballistic launcher: it holds one missile, rotates visually to its facing, and fires it through the shared [launch sequence](launch-sequence.md). It joins a [launcher network](launcher-connector-and-network.md) with its neighbors. Missile flight is `03-missiles`; the radio bus is `01-core`.

## 1. Identity

| Item | Value |
|---|---|
| Classes | `content/blocks/launcher/base/BlockLauncherBase`, `TileLauncherBase`, `LauncherCapability`, `TESRLauncherBase`, `gui/ContainerLaunchBase`, `gui/GuiLauncherBase`; shared `content/blocks/launcher/LauncherBaseCapability`, `FiringPackage`, `LauncherSolution`, `LauncherLangs`, `status/*` |
| Registry name | block `icbmclassic:launcherbase`, tile `icbmclassic:launcherbase` |
| English name | "Launcher Pad" (`tile.icbmclassic:launcherbase.name`); GUI title "Launcher Platform" (`gui.icbmclassic:launcherbase.name`); tooltip "Hold missiles in place before launch" |
| Block properties | iron material, hardness 10, resistance 10, full-block flag set but `isOpaqueCube` false (to avoid lighting artifacts on the rendered missile). Render type: model. Creative tab: the ICBM tab |
| State | one property, `facing` (6 values), the launch direction. *1.12-only mechanism* (blockstate). Meta = `facing.ordinal() + 1`; meta 0 is read as UP, because older tiles had no rotation |
| Placement | the facing is the **clicked face** (not the player's look direction), so a pad placed on a floor faces UP, on a ceiling DOWN, on a wall horizontally |
| Oredict | none |

## 2. Obtaining

Shaped, 2 rows: `i c i` over `p p p`; i = `ingotSteel`, c = `circuitBasic`, p = `plateSteel`. Result 1 (`recipes/launcher/base.json`). The cruise launcher recipe consumes one base (see [cruise-launcher.md](cruise-launcher.md)). Recipes belong to `06-blocks-items`.

## 3. Numbers

| Name | Value | Notes |
|---|---|---|
| Energy buffer | 1,200 FE | `ConfigLauncher.POWER_CAPACITY`, config `icbmclassic/launcher`, key `power_capacity` |
| Energy per launch | 1,200 FE | `power_per_launch`. Equal to the buffer, so the pad stores exactly one shot |
| Max range | 10,000 | `max_range`; applied **per axis** (a square), see [launch-sequence.md](launch-sequence.md) |
| Min range | 10 (hard-coded) | per axis; a 20 x 20 block no-fire square around the pad |
| Inaccuracy | base 2, +10 at full range, +1 per extra launcher | keys `min_inaccuracy_range`, `scaled_inaccuracy_range`, `scaled_inaccuracy_per_launcher` |
| Pad warm-up | 40 ticks | constant `PAD_WARM_UP_TIME` (TODO config in source) |
| Slow-climb distance | 2 blocks | constant `MISSILE_CLIMB_HEIGHT`; UP launches only |
| Lock height (default) | 3 | user field, "height to move before changing direction" |
| Firing delay (default) | 0 ticks | user field |
| Group id / group index | -1 / -1 | user fields; **stored but never used** (ICBM-04-01) |
| Missile collision box | 1 x 5 x 1, from the pad block's Y upward | blocks inserting a missile while a missile entity is inside |

`ConfigMain.REQUIRES_POWER` (`use_energy`, default true) false makes every power check pass; the Forge Energy capability is then hidden. Energy is Forge Energy, a *1.12-only mechanism*; GregGuns re-bases it on EU (`docs/gregguns-icbm-spec`).

### Missile spawn offsets

The missile entity is created at the block center plus an offset that depends on the facing:

| Facing | Offset (x, y, z) from block center |
|---|---|
| DOWN | (0, -3.1, 0) |
| UP | (0, +2.6, 0) |
| NORTH | (0, -0.2, -2.8) |
| SOUTH | (0, -0.2, +2.8) |
| WEST | (-2.8, -0.2, 0) |
| EAST | (+2.8, -0.2, 0) |

The held missile is **drawn** 2.0 blocks out along the facing from the block center (so 2.5 up for UP); the spawn point is further out than the drawn position. Launch rotation (what the entity gets, versus what the renderer uses):

| Facing | Yaw (launch / render) | Pitch (launch / render) |
|---|---|---|
| UP | 0 / 0 | 90 / 0 |
| DOWN | 0 / 0 | -90 / -180 |
| NORTH | -180 / 0 | 0 / -90 |
| SOUTH | 0 / -180 | 0 / -90 |
| WEST | -90 / 90 | 0 / -90 |
| EAST | 90 / -90 | 0 / -90 |

Angle conventions are the renderer's and the missile entity's (`03-missiles`, `07-visuals-audio`).

## 4. Behavior

### Server tick

The tile is a `TileMachine`: a tick counter plus a list of tick actions.

1. Description packet: sent to everyone nearby on the tick after any `markDirty` (a once-per-tick coalescing flag).
2. Every 3 ticks: send the GUI sync packet to players that have the GUI open. Every 20 ticks: drop viewers whose open container is no longer the launcher container.
3. If a firing package exists: count it down one per tick; at 0 or below, launch through the capability (the full launch sequence runs again) and clear the package. The result status is discarded (ICBM-04-08).
4. Every 3 ticks: re-arm the missile collision check, then keep the **seat** entity in step: if a missile is present and there is no seat, spawn an `EntityPlayerSeat` at the block center and bind it to this tile; if there is no missile and a seat exists, remove its passengers and kill it. (The seat entity is a fake entity that lets a player "ride" the missile before launch; its class is outside this area.)
5. The battery slot discharges any energy item into the buffer each tick (`EnergySystem::isEnergyItem` accepts it).

### Interactions (right click)

Server side, evaluated in order:

1. Holding a stone axe prints a debug network line to chat (leftover debug code, ICBM-04-18).
2. `tryInsertMissile`: if the pad is empty and the held item is a valid missile, insert it into slot 0 (creative mode keeps the item) and consume the click. If the player is sneaking with an empty hand and a missile is present, take the missile into the hand. If neither applies, the GUI opens (`openGui` id 0).
3. A missile cannot be inserted while the collision check finds a missile entity in the 1 x 5 x 1 box above the pad. The click still counts as handled, with no message (ICBM-04-27).

Right click returns true on both sides; breaking the block notifies the network node, then drops the inventory contents.

### Capabilities (*1.12-only mechanism*; behavior it produces)

- Item handler (the 2-slot inventory), missile holder (wraps slot 0), missile launcher (the `LauncherCapability`) on any side.
- Forge Energy: the pad's own buffer, only when `use_energy` is on.

### Launch

All launching goes through `LauncherCapability`. See [launch-sequence.md](launch-sequence.md) for the checks, the inaccuracy model, the delay package and the flight path that the pad assigns (warm-up, slow climb, lock height, arc).

## 5. Persistence and sync

NBT, tile root (via a save handler):

| Key | Type | Notes |
|---|---|---|
| `lock_height` | int | |
| `group_id`, `group_index` | int | unused downstream |
| `firing_delay` | int | |
| `energy` | int | |
| `inventory` | compound | 2 slots |
| `firing_package` | compound | `countdown`, plus a registry-built `target` and `cause`; the `hasFired` flag is not saved |
| `launcher` | compound | the capability, holds `uniqueId` (a UUID created lazily on first read) |
| `seat_side` | facing | defaults by launch direction: NORTH for UP/DOWN, UP for horizontals |

Packets (the "packet codex" is a *1.12-only mechanism*; the direction flags it records are never enforced, ICBM-04-13):

| Packet | Declared direction | Fields |
|---|---|---|
| `description` | server to client | held missile stack, seat side |
| `gui` | server to client | group index, group id, firing delay, lock height |
| `lock_height`, `group.id`, `group.index`, `firing.delay` | client to server | one int each |
| `rotation.seat` | client to server | facing |

The client keeps a cached copy of the missile stack for rendering (so the GUI slot and the renderer don't fight).

## 6. UI

Container: missile slot at (88, 47), battery slot at (141, 47), player inventory at (7, 84). Texture `textures/gui/gui_silo_base.png` (256 x 256, `ySize` 166).

| Control | Position (x, y, w, h) | Does |
|---|---|---|
| Lock height field | (17, 17, 30, 12) | integer; sends `lock_height` |
| Group id field | (82, 17, 30, 12) | integer; sends `group.id` |
| Group index field | (127, 17, 30, 12) | integer; sends `group.index` |
| Firing delay field | (17, 33, 30, 12) | integer ticks; sends `firing.delay` |
| Seat face button | (guiLeft + 157, guiTop + 3) | cycles the seat side relative to the launch direction (Alt + click clockwise, Shift + click counter-clockwise); sends `rotation.seat` |
| Energy bar | (141, 66) | shows stored/max and the action cost (the firing cost) |
| Tooltips | 14 x 14 boxes beside each field | lock height, group id, group index, firing delay |

Shift-click moves the missile or battery into the matching slot, but the slot test is off by one (ICBM-04-14). The title text is drawn at (30, 6) in gray.

## 7. Visuals

- Block model `models/block/silo_pad.json` (parent `cube_bottom_top`): textures `blocks/silo_pad/top`, `side`, `bottom` (all 16 x 16). Blockstate `blockstates/launcherbase.json` applies rotations per `facing` (`up`: none; `down`: x 180; horizontals: x -90 with y 180/0/270/90 for north/south/east/west).
- The held missile is drawn by a TESR (`TESRLauncherBase`) using the shared missile item renderer (`07-visuals-audio`), at the facing offset above, with the tile's render box set to infinite so a tall missile never culls.
- GUI texture above.

## 8. Audio

None in this area. The launch sound is played by the missile entity (`03-missiles`, `07-visuals-audio`).

## 9. Relationships

- [launch-sequence.md](launch-sequence.md): everything that happens on fire.
- [launcher-connector-and-network.md](launcher-connector-and-network.md): the pad's network node accepts items (it is an inventory node) and exposes energy to the network.
- [launch-screen.md](launch-screen.md): fires all pads on the network.
- [cruise-launcher.md](cruise-launcher.md): the other launcher type; uses a pad as an ingredient.
- `03-missiles`: `WarmupFlightLogic`, `MoveByFacingLogic`, `ArcFlightLogic`, `BallisticTargetingData`, the missile stack capability.
- `05-detection-defense`: the radar's recipe uses the screen; the radar message reaches the screen, not the pad.

## 10. Bugs

See the index table: ICBM-04-01 (groups unused), -02, -03 (inaccuracy count), -05 (range boxes), -07 (consume-before-spawn), -08 (delayed result lost), -14 (shift-click), -18 (debug code), -25 (seat), -26 (no shaft check), -27 (silent insert failure), -31 (event twice).

## Lang

| Key | English |
|---|---|
| `tile.icbmclassic:launcherbase.name` / `.info` | Launcher Pad / Hold missiles in place before launch |
| `gui.icbmclassic:launcherbase.name` | Launcher Platform |
| `gui.icbmclassic:launcherbase.lock_height` | Lock Height: distance the missile will travel up before changing course |
| `gui.icbmclassic:launcherbase.group_id` | Group ID: used to group missiles for fire missions |
| `gui.icbmclassic:launcherbase.group_index` | Group Index: index of the missile in the group, used for chain firing |
| `gui.icbmclassic:launcherbase.firing_delay` | Firing Delay: time in ticks before firing (20 per second) |
| `gui.icbmclassic:button.face.tooltip` | Missile Rotation: sets the visual rotation and missile seat position (Alt/Shift + click) |
| `gui.icbmclassic:energy*` | energy bar tooltips (with action cost variants) |

Status and error strings are in [launch-sequence.md](launch-sequence.md).
