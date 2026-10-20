# Radio Bus

Source roots (relative to `src/main/java/icbm/classic/` at `9c1d985a`): `api/radio/` (interfaces and message contracts), `lib/radio/` (map, registry, base classes, message classes, capability). Also read, to see who plugs in: `content/blocks/launcher/screen/RadioScreen`, `content/blocks/launcher/cruise/RadioCruise`, `content/blocks/emptower/RadioEmpTower`, `content/blocks/radarstation/RadioRadar`, `prefab/FakeRadioSender`, `prefab/item/ItemRadio`, and the senders in `ItemRemoteDetonator`, `ItemLaserDetonator` and `TileRadarStation`.

**GregGuns replaces this bus with data sticks (decision D13 in [DECISIONS.md](../../gregguns-icbm-spec/DECISIONS.md)).** This sheet is therefore written as a behavioral contract: section 1 says what the bus does for the player, section 6 lists the interactions a replacement must reproduce, and the rest records the 1.12 mechanics and their bugs so the replacement does not inherit them.

## 1. What it is, for the player

The bus is a **wireless, dimension-wide, name-matched broadcast**. A device that wants to talk picks a **channel**, a short text string (the lang text calls it "a shared alpha-numeric value to connect devices"). Every receiving device in the same dimension whose channel is the same text acts on the message. There is **no range, no line of sight, no ownership, no security**; the only protection is that a 4-character random channel is hard to guess and that the player can disable a device.

Players meet it in three places:
- A **channel text box** in the GUI of the launcher screen, cruise launcher, EMP tower and radar station. The GUI tooltip (`gui.icbmclassic:tooltip.radio.channel`) reads: "Radio Channel: Set to a shared alpha-numeric value to connect devices. Alt + Click to toggle active". The *Alt + click* toggles the disabled flag (an on/off button in the receiver GUIs).
- A **radio item** (the remote detonator and the laser detonator share a base class) that stores a channel in its stack NBT. Using it on a device that exposes the radio capability copies that device's channel into the item and prints `chat.launcher.toolFrequencySet` ("Frequency set to %s").
- **Action-bar feedback** when a message lands (section 5).

## 2. Participants

```
sender --message(channel, payload)--> RadioRegistry --> RadioMap(dimension) --> every receiver
                                                                                  |
                              receiver checks: not disabled, not the sender, server side, channel equal
                                                                                  |
                                    acts, then calls sender.onMessageCallback(receiver, TextMessage)
```

| Role | Interface | Notes |
|---|---|---|
| Any radio | `IRadio` | World, block position, `IBoundBox` range, `isDisabled()` (default false). The doc warns: do not reveal radio positions to players |
| Channel holder | `IRadioChannelAccess` | `getChannel()` (never null) and `setChannel(text)`. For tools that read or write the channel. Not every radio has one (a device with several channels would not) |
| Receiver | `IRadioReceiver` | `onMessage(sender, packet)` |
| Sender | `IRadioSender` | `onMessageCallback(receiver, response)`: the reply path. The docs frame the bus as two-way like TCP: every receiver reaction can send a response back for display; silent devices may ignore it |
| Message | `IRadioMessage` | Only `getChannel()` |

Base classes (`lib/radio/imp/`):
- `Radio` (abstract): stores the channel and a `disabled` flag. A radio with no channel generates one on first read: **4 random letters and digits** (case-sensitive alphanumeric, not cryptographic random). NBT keys: `channel` (omitted while null) and `disabled`.
- `RadioTile<T>`: a radio hosted by a tile entity. Position and world come from the tile. Its range is the shared **INFINITE** box. It supplies the standard acceptance test `canReceive(sender, packet)`: not disabled, sender is not itself, host has a world and is on the **server**, and the radio's channel equals the packet's channel (reading the channel here creates it if missing).
- `CapabilityRadio`: an empty registration of `IRadio` as a capability, with no storage and null answers *(1.12-only mechanism)*. The real radio objects are the per-tile fields, returned when a tile is asked for `RADIO_CAPABILITY`.

Registered tiles: launcher screen, cruise launcher and EMP tower expose the capability (so the radio item can copy their channel); **the radar station does not**, so the radio item cannot copy from it.

## 3. Message types

All are simple value objects built with a channel. The payload interfaces:

| Message | Interface (`api/radio/messages/`) | Class (`lib/radio/messages/`) | Payload | Sent by | Understood by |
|---|---|---|---|---|---|
| Trigger | `ITriggerActionMessage` (deprecated in favor of passing action fields) | `TriggerActionMessage` | `shouldTrigger()` (default true); `onTriggerCallback(status)` (default empty) | Remote detonator | Launcher screen, cruise launcher, EMP tower |
| Target | `ITargetMessage` | `TargetMessage` | `getTarget()` position (null if lost); `getIntercept(x, y, z, interceptorSpeed)` (default = target); `getTargetEntity()` (default null) | Nothing in ICBM (available to addons) | Launcher screen, cruise launcher |
| Trigger + target | both | `TriggerActionTargetMessage` | target position | Laser detonator | Launcher screen, cruise launcher, EMP tower (as a plain trigger) |
| Incoming missile | `IIncomingMissileMessage` (target + trigger) | `IncomingMissileMessage` | the missile, `trigger` flag (radar always sends true); target = the missile's current position; entity = the missile entity | Radar station | Launcher screen, cruise launcher (as target + trigger), EMP tower (special case) |
| Text | `ITextMessage` | `TextMessage` | text or translation key, a `shouldTranslate` flag (true for the two simple constructors) and optional format arguments | Receivers, as replies | Senders that show it (`FakeRadioSender`) |

Notes on the incoming-missile message:
- `onTriggerCallback(status)`: if the status is a launch result that carries the fired missile and that missile is a surface-to-air missile, the SAM's scan logic is told to lock onto the incoming missile. This only ever runs for the cruise launcher (section 6).
- `getIntercept` is meant to lead the target; the code is a stub that always returns the missile's present position (ICBM-01-60).

Reply texts and their lang keys (`radio.icbmclassic:message.*`, from `RadioTranslations`):

| Key | English | Sent when |
|---|---|---|
| `target.null` | Target data missing | A target message had no usable position |
| `target.set` | Target set to (%1$f %2$f %3$f) | Target accepted and no trigger accompanied it |
| `launch.success` | Firing missile(s) at (%1$f %2$f %3$f) with range %4$f | Screen fired at least one launcher |
| `launch.failed` | Failed to fire missile(s) | Screen fired none |
| `launch.triggered` | Firing missile(s), aiming launcher(s) | Cruise launcher queued a firing package |
| `tile.emptower.radio.success` | (missing from the lang file) | EMP tower fired (ICBM-01-53) |
| `info.icbmclassic:launcher.error.missile.queued` (a launcher lang string) | | Cruise launcher already has a missile queued |

## 4. Routing: the radio map

`RadioRegistry` keeps one `RadioMap` per dimension, created on demand and dropped when that dimension unloads. Everything is server side; calls with a client world, a null world or null provider are logged and dropped.

**Registration.** Receivers add themselves on server tile load (`onLoad`) and remove themselves on `invalidate()`. A receiver with a range equal to the shared INFINITE box goes in a "full-map" list. Any other range is indexed by the chunks it touches (plus one chunk of padding on every side). In ICBM every receiver is a `RadioTile`, so every receiver is full-map and the chunk index is empty and unused.

**Sending** (`popMessage`):
1. Reject a null packet, a null or blank channel, or a null sender (log an error with a stack trace; nothing is delivered).
2. If the sender has a cached receiver list, deliver to exactly that list and stop. (Nothing ever builds that cache; ICBM-01-58.)
3. Otherwise deliver to every full-map receiver, iterating from the most recently added to the first.
4. Then, if the sender has a range, deliver to ranged receivers whose range "overlaps" the sender's. Small maps (under 200 ranged receivers) or very wide senders (over 320 blocks in X or Z) scan every receiver; otherwise the chunk index is used. The overlap test is wrong (ICBM-01-56) but unreachable in ICBM.

Order of delivery is therefore reverse registration order for tiles. Channel filtering happens inside each receiver, not in the map: the bus hands the packet to every full-map receiver in the dimension, and each one ignores it unless its channel matches. Cost per message is O(receivers in the dimension).

*MIT-portable* mention (not recommended to port, because GregGuns drops the bus): the routing and chunk index live in [`lib/radio/RadioMap.java#L1-L407`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/lib/radio/RadioMap.java#L1-L407). The only observable behavior is the dimension-wide broadcast above.

## 5. Senders in ICBM

| Sender | Range | Triggers when | Message |
|---|---|---|---|
| Radar station (`RadioRadar`, a `RadioTile`) | whole dimension | Powered, channel not empty, and at least one incoming threat. Re-sent every `SAM_TICKS` (default 40 ticks; config file `icbmclassic/radar`, key `sam_delay`). Sends the **first** threat in its list each time (see [radar-station.md](../05-detection-defense/radar-station.md)) | `IncomingMissileMessage(channel, threat, true)`. Ignores all replies. Its disable flag is saved and has a packet but nothing reads it (ICBM-01-72) |
| Remote detonator ([handheld-tools.md](../05-detection-defense/handheld-tools.md)) | whole dimension | Right-click, after a cancellable `RemoteTriggerEvent` | `TriggerActionMessage(item channel)` |
| Laser detonator (same sheet) | whole dimension | Server receives a target from the client; a cancellable `LaserRemoteTriggerEvent` may edit or veto the target; then the player is told the target in chat | `TriggerActionTargetMessage(item channel, target)` |
| `FakeRadioSender` | | Wrapper that lets an item act as a sender. Holds the player and stack. Range is whole dimension unless a range is given (neither detonator gives one). Its reply handler shows each text reply on the player's **action bar** (translated, with format arguments, when the message asks for it) | |

An item's channel is stored in stack NBT as `radio_channel` (string). Older stacks used a float under `hz`; reading converts it to a whole number text; writing removes `hz`. Tiles also convert a legacy integer `frequency` NBT value to text on load (screen, radar; the screen comment says "TODO data fixer"). With no channel the item returns the empty string; the empty string is also the radar's "do not broadcast" value.

## 6. Receivers and their reactions (the contract to reproduce)

Every receiver first applies `canReceive`. A receiver that matches replies through `onMessageCallback`.

| Receiver | Message handling |
|---|---|
| **Launcher screen** ([04-launch-control](../04-launch-control/)) | 1. If the message has target data: compute a target using the **highest payload speed among the launchers in its group** (default direct-flight speed from the missile config if there are none) and the screen's center; if a target comes back, set it (this fires the cancellable `LauncherSetTargetEvent`), and reply "target set" unless a trigger is also present; if none, reply "target null". 2. If the message is a trigger whose `shouldTrigger()` is true: fire all launchers in the group (not simulated). Success replies with target and distance from the screen; failure replies "failed". The trigger callback is never invoked (ICBM-01-61) |
| **Cruise launcher** (04-launch-control) | Same target step with its own launcher's speed. On trigger: if a firing package is already queued, reply "queued" and stop; else queue a firing package aimed at its current target with delay 0 and a cause (the player's entity cause if the sender is a `FakeRadioSender`, no cause otherwise), wire the message's trigger callback into the package, and reply "triggered". The callback is how a SAM gets locked on the incoming missile |
| **EMP tower** ([emp-tower.md](../05-detection-defense/emp-tower.md)) | Incoming-missile message with `trigger` true: fire if the missile is closer than the tower's range (straight-line distance, **inside** the range; at or beyond is ignored with no reply). Any other trigger message whose `shouldTrigger()` is true: fire. Success replies with the tower's success text. Cause is the player's entity cause for a `FakeRadioSender` |
| **Radar station** | Sender only |

Behavioral notes any replacement must settle:
- **Target before trigger** in a combined message (laser detonator, radar), because the same packet both aims and fires.
- A radar that spams a trigger every 40 ticks while a threat exists makes the launcher screen re-set its target and refire repeatedly. Setting a target fires the cancellable `LauncherSetTargetEvent` and marks the tile dirty each time the screen sees a new vector object (ICBM-01-63).
- Replies are advisory; the radar discards them and the text only reaches a player when an item sent the message.
- All receivers are **server-only** and the radio flag `disabled` silences a receiver without changing its channel.
- There is **no authentication**: any device that knows the channel can fire a launcher from anywhere in the dimension (ICBM-01-62).

## 7. Mapping to data sticks (D13), checklist

| ICBM behavior | A data-stick replacement should |
|---|---|
| Shared channel text links devices | Store a link (device or group ID) on the stick; linking is a deliberate player act on both ends |
| Radio item copies a device's channel | Stick copies a device's link key from the device; stick-to-device writes it |
| Whole-dimension reach | Either keep (dimension-wide for linked devices) or choose a range per tier; ICBM has none |
| Radar auto-triggers launchers and the EMP | Keep radar-to-battery/EMP triggering (D14, D15 in DECISIONS), as an explicit link rather than a broadcast |
| Remote detonator triggers silos, laser detonator aims and triggers | Keep both verbs: "fire" and "aim and fire" |
| Reply texts to the player | Keep an action-bar reply for target set / fired / failed / queued / EMP fired |
| Disabled toggle on receivers | Keep; players use it to arm and disarm a base |
| Event hooks (`RemoteTriggerEvent`, `LaserRemoteTriggerEvent`, `LauncherSetTargetEvent`) | Keep as veto points for protection (see [public-api.md](public-api.md)) |

## 8. Related

- Radar, EMP tower, detonators, the radar gun: [05-detection-defense](../05-detection-defense/index.md).
- Launcher screen, cruise launcher, firing packages: `04-launch-control`.
- Missile and SAM scan logic that the callback talks to: `03-missiles`.
- The reply and logging side-channel used by packets: [event-tracker.md](event-tracker.md).
