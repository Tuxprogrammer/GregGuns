# Command network (ICBM 2): short behavioral summary

Source: BuiltBrokenModding/ICBM @ `13e6a524`, under `src/main/java/com/builtbroken/icbm/content/launcher/controller/` and `content/items/`. Summary only; GregGuns plans OpenComputers, redstone and data-stick control instead (D13), so this is background.

## What the pieces are

| Piece | Class | Role |
|---|---|---|
| Command Central | `remote/central/TileCommandController` | The hub. Knows a set of silo connectors by position and receives radio "fire" messages |
| Silo connector | `remote/connector/TileCommandSiloConnector` | Holds up to **20** linked silos (`MAX_CONNECTIONS`), a display name and a **group name**. Links only silos within **20 blocks** (`MAX_LINK_DISTANCE`) |
| Antenna | `remote/antenna/TileAntenna`, `TileAntennaPart`, `AntennaNetwork`, `BlockAntennaParts` | A tower built from stacked parts. Gives the central a radio footprint |
| Silo interface | `remote/display/TileSiloInterface` | A display block and GUI that lists silos |
| Direct controller / local controller | `direct/`, `local/` | Redstone or GUI controllers wired straight to a silo (Classic area `04-launch-control` covers them) |
| Link tool | `items/ItemLinkTool` | Stores a position, then links the next clicked block to it |
| GPS flag | `items/ItemGPSFlag` | Stores a position to use as a missile target |

## Behavior

1. **Linking.** Link tool: sneak-click clears; a first click stores a location ("link.pos.set"); a second click on another linkable block calls `link(location, code)` on one of them and reports "link.completed" or an error (invalid position, same block loop, wrong pass code, too far, already added, limit reached). A connector re-linking a silo it already has toggles it off ("link.removed") or refreshes it ("link.updated"). Central to connector links work within 100 blocks (`TileLocalController.MAX_LINK_DISTANCE`) and use the target's pass code.
2. **Central housekeeping.** Every 60 ticks it re-resolves its stored connector positions. A connector whose chunk is unloaded is kept by position; one that is no longer a connector is dropped. NBT: `siloPosData`, `displayName`.
3. **Networks.** The central can join several wireless networks (it is a wireless data point with access control; a silo list is shared only to holders of the right pass key, `siloDataPassKey`). `canAcceptAntennaConnection` decides which sides accept an antenna.
4. **Firing by radio.** A radio message with header `fireMissile1` (use the silo's stored target), `fireMissileAtTarget` (data carries a position) or `fireMissile2` (alternate form) and data `[passcode, groupName, siloName, (target)]` is accepted if the message frequency is within 0.001 Hz of an attached network's frequency. The central finds connectors with the group name, then the silo with the silo name, then checks the silo's connection status: OFFLINE, NO_CONNECTION or ONLINE. ONLINE silos check the pass code, then call `fireMissile`. If the sender is a "fake radio sender" (a player), chat messages report each outcome (offline, bad code, fired with ETA, unknown group, unknown silo, failed). The pass-code cast is unchecked and exceptions are logged (ICBM-A1-21).
5. **Antenna network.** Parts merge and split as they are placed or broken. The network keeps a bounding box of the whole tower and computes two ranges:
   - **Receive range** = the tower's bounding box.
   - **Send range**, see the math below.
   - A connection is made only when the two endpoints' send ranges overlap each other's receive ranges, in both directions.
6. **GPS flag.** Sneak-use on a block stores that block's position (item damage becomes 1). Plain use on a launcher (or any block of a multiblock whose host is a launcher) sets that launcher's target to the stored position ("gps.data.transferred"). Sneak-use in air clears it ("gps.cleared"). The radar gun does the same job for a radar-found target.

### Antenna range math (MIT-portable)

Permalink: [`AntennaNetwork.java` L163-L179](https://github.com/BuiltBrokenModding/ICBM/blob/13e6a5245c76f5599baca521b462168b9ebe75d8/src/main/java/com/builtbroken/icbm/content/launcher/controller/remote/antenna/AntennaNetwork.java#L163-L179).

```
RANGE_PER_PIECE = 100,  MAX_HEIGHT_GAIN = 1000
heightBonus = MAX_HEIGHT_GAIN / max(50 - towerHeight, 1)          // 1 piece: 20; 49+ pieces: 1000
altitudeBonus = (maxY + baseY) < 64 ? -2000
              : MAX_HEIGHT_GAIN / max(192 - maxY + baseY, 1)       // precedence quirk, see bug 14
r = max(heightBonus + altitudeBonus, 0)
east  = max(box.max.x * 100 + r, 0)      west  = max(box.min.x * 100 + r, 0)
south = max(box.max.z * 100 + r, 0)      north = max(box.min.z * 100 + r, 0)
sendRange = box from (-west, 0, -north) to (east, 256, south), moved to the antenna base
```

Intent: each added piece sideways adds 100 blocks of range, tall towers gain range, towers below sea level (Y 64) are crippled. In practice the west and north arms use negative box coordinates and collapse to the bonus alone (ICBM-A1-13), so range is lopsided. The GregGuns relevant idea is simply "range grows with built size and height".

## Unfinished here

- `TileDirectAccessPort` is an empty 12-line stub.
- `MissileAssemblyLine` (the station chaining helper) is unused.
- Several `TODO` markers in the antenna code ("redo", "fix round error").
- The central's developer-only stick and blaze-rod debug prints only run in dev mode.
