# Launcher Screen (Launch Control Panel)

The control block: holds a target, a radio channel and the launch button, and fires every launcher on its network. Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Classes in `content/blocks/launcher/screen/`. The firing rules are in [launch-sequence.md](launch-sequence.md); the network is in [launcher-connector-and-network.md](launcher-connector-and-network.md).

## 1. Identity

| Item | Value |
|---|---|
| Classes | `BlockLaunchScreen`, `TileLauncherScreen`, `RadioScreen`, `BlockScreenCause`, `LauncherPair`, `gui/ContainerLaunchScreen`, `gui/GuiLauncherScreen`, and the shared button `cruise/gui/LaunchButton` |
| Registry name | block `icbmclassic:launcherscreen`, tile `icbmclassic:launcherscreen` |
| English name | "Launcher Screen" (`tile.icbmclassic:launcherscreen.name`); tooltip "Used to fire missiles from launchers"; GUI title "Launcher Control Panel" |
| Block properties | the shared ICBM block base: iron, hardness 10, resistance 10; `rotation` property (placer's horizontal facing, stored in meta); drops its inventory on break; render type model |
| Oredict | none |

## 2. Obtaining

Shaped, 2 x 2 (`recipes/launcher/screen.json`): `c g` over `s w`; c = `circuitBasic`, g = glass, s = `ingotSteel`, w = `wireCopper`. Result 1. The radar station recipe uses the screen as an ingredient (`05-detection-defense`). Recipes belong to `06-blocks-items`.

## 3. Numbers

| Name | Value | Notes |
|---|---|---|
| Energy buffer | 1,200 FE (`power_capacity`) | **Used for nothing** (ICBM-04-15) |
| Inventory | 1 slot, a battery | at (141, 47) |
| Status refresh | every 5 ticks, and right after any fire attempt | server |
| Redstone poll | every 3 ticks | server |
| GUI sync | every 3 ticks to viewers | |
| Default target | (0, 0, 0) | a valid target (ICBM-04-06) |
| Radio channel | string, default per the radio base class | NBT `radio` |

## 4. Behavior

### Server tick

1. Every 5 ticks, or when a fire attempt just happened: rebuild the **status list**. For each launcher in the screen's group (always everyone), call `preCheckLaunch` with the screen's current target and a screen-cause, store (group, index, status), and take the **largest** `getInaccuracy(target, launcherCount)` over them as the displayed "Acc". `launcherCount` is the number of launchers in the group.
2. Every 3 ticks, if the world's **strong** redstone power at the screen's position is above 0: `fireAllLaunchers`. This is level-triggered (no edge detection, a TODO in the source) and uses strong power only, so, for example, a redstone block (weak power only) is not expected to work while a lever or repeater feeding the screen does (unverified in game, ICBM-04-20).

### `fireAllLaunchers(simulate)`

1. Set the refresh flag. If the screen has no network, return false (no feedback).
2. Take the launchers in the group (all). Build one solution: the target (a plain position), group -1, count = number of launchers.
3. Offer the solution to every launcher via the network, in network order.
4. Return true if **at least one** launcher returned a non-blocking status. Delayed launchers return a blocking status, so a mission where every pad has a firing delay reports false (ICBM-04-09).

### Setting the target

- By hand in the GUI (a "x, y, z" text field), by a GPS item (right click on the block applies the item's data; the item logic is `06-blocks-items`), or by a radio target message.
- Server side, a Forge `LauncherSetTargetEvent` can reject the change or replace the position; a null replacement becomes (0, 0, 0). The client copy is set without the event.
- No range check on the target at set time; the launchers decide at fire time.

### Block interactions

Right click (server): a GPS item with data applies it; a stone axe prints debug network info (an NPE if the screen has no network, ICBM-04-18); otherwise the GUI opens. Break: detaches the node, drops the battery.

### Radio

Registered on the radio bus when loaded on the server, removed on invalidate. Channel and "disabled" flag are edited in the GUI. Behavior of messages: [launch-sequence.md](launch-sequence.md#5-radio-firing). Legacy: an old NBT integer `frequency` is converted to a channel string on load.

### Capabilities (*1.12-only mechanism*)

The radio capability is always the screen's own. For energy and item handler, if the screen is on a network it returns the **network's** shared sink and inventory, not its own buffer or slot. So power pushed into the screen goes to the pads, while the screen's own buffer and its battery slot never reach them (ICBM-04-15). Everything else comes from the tile base.

## 5. Persistence and sync

| NBT key | Content |
|---|---|
| `radio` | channel, disabled flag |
| `inventory` | 1 slot |
| `target` | the target position (shared key constant) |

Not saved: the status list and the inaccuracy figure (rebuilt).

Packets (direction flags are not enforced, ICBM-04-13):

| Packet | Declared direction | Fields |
|---|---|---|
| `description` (GUI sync) | server to client | target, channel, disabled, inaccuracy, target again (duplicate field), status list (NBT: group, index, status per launcher) |
| `radio.frequency` | client to server | channel string |
| `radio.disable` | client to server | toggles disabled |
| `target` | client to server | target position |
| `launch` | client to server | none; the server fires all launchers, with no check of who sent it or where they are |

## 6. UI

Texture `textures/gui/gui_silo_screen.png` (256 x 256; size 175 x 166). Container: battery slot (141, 47), player inventory (7, 84).

| Control | Position (x, y, w, h) | Does |
|---|---|---|
| Target field | (18, 17, 100, 12) | text "x, y, z"; sends `target` |
| Channel field | (135, 17, 34, 12) | text; sends `radio.frequency` |
| Launch button | (24, 38) 28 x 29 | enabled only if the status list is non-empty and has no blocking status; sends `launch`. Its tooltip is the status text |
| Radio disable button | (119, 16) | toggles; Alt + click per the tooltip |
| Inaccuracy text | (60, 32) | "Acc: `%1$s`m", two decimals; tooltip: "Radius in meters from target the missile could land. Driven by a range of factors, such as: number of launchers, and distances." |
| Energy bar | (141, 66) | shows the **screen's own** buffer (not the network) |
| Tooltips | (2, 16, 14, 14) target; (119, 16, 14, 14) radio | |

The launch button draws from the **cruise launcher** GUI texture (`gui_cruise_launcher.png`), sprite column at u = 182: normal v 6, hover v 41, pressed v 76, disabled v 111 (each 28 x 29), plus a 36 x 36 "glass cover" at (220, 5) drawn over a disabled button.

### Status text (`getStatusTranslation`)

Tooltip logic: no network: "Self Check; No launcher network". No launchers in the group: "...No launcher(s) connected" (a missing-key bug, ICBM-04-16). Empty status list (client before first sync): "No Status... server desync". All launchers fine: "Ready to fire". Else `"<n> / <total> not ready"` followed by up to 5 individual blocking messages.

## 7. Visuals

- `models/block/silo_screen.json` (parent `cube`): `blocks/screen/silo.png` (16 x 16) on the north face; `blocks/machine_green.png` (16 x 16) on every other face and as particle. `blockstates/launcherscreen.json` rotates by `rotation` (south 0, west 90, north 180, east 270).
- GUI above.

## 8. Audio

None.

## 9. Relationships

- [launcher-connector-and-network.md](launcher-connector-and-network.md): the screen's launcher list and shared capabilities.
- [launch-sequence.md](launch-sequence.md): what a fire does.
- `01-core`: radio bus, `RadioTile`, `FakeRadioSender`.
- `05-detection-defense`: radar and laser detonator messages arrive here; the radar recipe uses the screen.
- `06-blocks-items`: GPS item logic (`GPSDataHelpers`).

## 10. Bugs

ICBM-04-03, -06, -09, -13, -15, -16, -18, -20.

## Lang

| Key | English |
|---|---|
| `tile.icbmclassic:launcherscreen.name` / `.info` | Launcher Screen / Used to fire missiles from launchers |
| `gui.launcherscreen.name` | Launcher Control Panel |
| `gui.launcherscreen.inaccuracy` | Acc: %1$sm |
| `gui.launcherscreen.inaccuracy.info` | see UI |
| `gui.icbmclassic:tooltip.radio.channel` | Radio Channel: set to a shared alpha-numeric value to connect devices; Alt + click toggles active |
| `gui.icbmclassic:tooltip.target` | Target: current aim position; "x, y, z" format, example 56.5, 40, 89 |
| `info.icbmclassic:gps.*` | GPS transfer messages (`06-blocks-items`) |
