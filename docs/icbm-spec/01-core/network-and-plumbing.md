# Network and Plumbing

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Covers `lib/network/` (every class), and one-line catalogs of the 1.12-only plumbing in `config/util/`, `lib/saving/`, `lib/transform/`, `prefab/` and `client/ClientProxy`.

The netty channel, `IMessage` codecs, `Capability`, `ITickable` and the GUI base classes are *1.12-only mechanisms*. GregGuns rebuilds on the 1.7.10 `SimpleNetworkWrapper`. What matters for the rebuild is the **message list and the trust model**.

## 1. Transport

| Item | Behavior |
|---|---|
| Channel | One FML netty channel named `icbmclassic`, opened in `init` (`PacketManager.init`) |
| Codec | `PacketEncoderDecoderHandler` is an indexed codec with **6 packet classes**, discriminator IDs 0 to 5 in registration order (§2). Encode and decode failures are caught and logged; the buffer is left as it is (ICBM-01-28) |
| Receive | `PacketInboundHandler` picks the effective side: on the client it calls `handleClientSide(minecraft, player)`; on the server it calls `handleServerSide(player)` with the sending `EntityPlayerMP`. Exceptions are caught and logged. The default handlers throw `UnsupportedOperationException` ("Unsupported operation for Packet"), so a client sending a client-bound-only packet only produces a log line |
| Send helpers (`PacketManager`) | `sendToPlayer`, `sendToAllInDimension`, `sendToAll`, `sendToAllAround(point)` (several overloads, default range 64 for a tile), `sendToServer`. Every send null-checks the channel (for unit tests) and logs an error instead |
| Thread | Handlers run on the netty thread. The two lambda packets schedule their real work onto the main thread; `PacketPlayerItem` does not (the item receivers it calls schedule themselves, ICBM-01-24) |
| Error events | `PacketEvents` / `PacketTileEvents` post structured events to `MAIN_TRACKER` (wrong world, not-server-world, invalid tile, handling error). A logger listener prints them (wrong-world and invalid-tile only at debug level) |

Besides these, tile entities also sync through the vanilla update packet (`TileMachine.getUpdatePacket` sends the whole tile NBT) for the initial chunk send.

## 2. Packet catalog

Wire format notes: all integers big-endian ints; strings via Forge `ByteBufUtils` UTF-8; NBT via `ByteBufUtils.writeTag`.

| ID | Class | Direction (as used) | Payload | Server validation of the sender |
|---|---|---|---|---|
| 0 | `PacketLambdaTile` | both | int codex ID, int dimension, int x, int y, int z, then the codex's fields (§3) | Checks only: player is in the packet's dimension; the player's world is a server world; the target chunk is loaded; a tile exists at the position and is valid for that codex. **No distance check, no open-GUI check, no ownership or permission check, no check that the codex is meant for this direction** (ICBM-01-20, -21) |
| 1 | `PacketLambdaEntity` | both | int codex ID, int entity ID, int dimension, then the codex's fields | Dimension and server-world checks, then looks the entity up by ID in the player's world and checks it is alive. Same missing checks. Unknown codex ID is logged and dropped |
| 2 | `PacketPlayerItem` | client -> server (the client handler just calls the server one) | int slot ID, int sub-ID, then free-form payload | Slot ID is client-controlled: a value >= 0 indexes the player's inventory (out of range throws, caught); a negative value `-n` selects item ID `n` and calls the receiver of **whatever item has that ID** whether or not the player holds it. The receivers then check that the player's current stack is that item (ICBM-01-24) |
| 3 | `PacketSpawnAirParticle` | server -> client | int dimension; doubles x, y, z, vx, vy, vz; floats r, g, b, scale; int age | n/a (client only acts if the dimension matches). Sent to everyone within 256 blocks |
| 4 | `PacketSpawnBlockExplosion` | server -> client | int dimension; doubles source x, y, z, blast scale; ints block x, y, z | n/a. Sent to everyone within 256 blocks, **once per destroyed block** (ICBM-01-26) |
| 5 | `PacketEntityPos` | server -> client | int entity ID, int dimension; doubles x, y, z, motion x, y, z; floats yaw, pitch | Server-side handler is a no-op. Client sets the entity's position, motion and rotation if the dimension matches. Used by the Mekanism teleporter hook only |

`PacketBase` (deprecated) is the old helper that builds a payload by appending boxed values (bytes, ints, floats, strings, NBT, stacks, vectors, enum ordinals as ints). `PacketPlayerItem` and `PacketEntityPos` still extend it.

### `PacketPlayerItem` receivers (the three `IPacketIDReceiver` implementers)

| Receiver | Sub-ID | Payload read | Effect |
|---|---|---|---|
| `ItemLaserDetonator` (current stack must be this item; server only) | any | 3 doubles: target x, y, z | Fires a `LaserRemoteTriggerEvent` on the main thread; if not cancelled, tells the player the target and pops a radio target message (`TriggerActionTargetMessage`) on the item's channel. **The coordinates come from the client unchecked** (no line of sight, no range, no check that a laser actually hit anything) (ICBM-01-25) |
| `ItemRadarGun` | any | boolean (main hand?), 3 doubles | Fires `RadarGunTraceEvent` on the main thread; stores the position and world in the held stack's GPS data. Same unchecked coordinates |
| `EntityMissile` | 1 | the missile's spawn data | Re-reads the missile state from a spawn-data buffer. Not reachable through `PacketPlayerItem` in practice (the packet only resolves items) |

## 3. Codex (lambda) packets

The two lambda packets carry a **codex**: a registered list of typed fields with a getter and a setter, so one packet class serves every machine. `PacketCodexReg` assigns each codex an integer ID in the order `register` is called (counter starts at 0). The ID is what goes on the wire, so server and client must run the same registration order (ICBM-01-33). A codex has a parent (tile or entity registry name), a name, a converter from the raw tile to the object the fields read, and an optional "on finished" action.

Field encoders available: int, byte, float, double, boolean, string, item stack (as NBT), `Vec3d`, `EnumFacing` (byte), enum (byte or int ordinal), NBT compound, and arbitrary encoder/decoder pairs (the radar dot list).

The codex has `fromClient()` (serverbound) and `fromServer()` (clientbound) flags. **The flags are stored but nothing ever reads them**: the handlers do not check them, so any codex can be sent in either direction (ICBM-01-20). "Direction" in the tables below is therefore what the author intended.

A `toggleBoolean` field sends the sender's current value and the receiver stores the **negation** of the received value (ICBM-01-34).

Expected IDs given the call order in `BlockReg` (EMP tower, radar, launcher base, launcher screen, cruise launcher), then `EntityMissile` whenever that class first loads:

### EMP tower (parent `icbmclassic:emptower`), IDs 0 to 5

| ID | Name | Intended direction | Fields | Effect |
|---|---|---|---|---|
| 0 | `radius` | to server | int range | set range, capped at the maximum radius (no lower bound, ICBM-01-29); marks dirty |
| 1 | `radio.frequency` | to server | string channel | set the radio channel |
| 2 | `gui` | to client | int stored energy, string channel, bool disabled, int range | GUI sync, sent to players with the GUI open every 3 ticks; **setting the energy field from a received packet means a client can set the tower's stored energy** (ICBM-01-22) |
| 3 | `fire` | to server | none | fire the tower now if ready; no player is passed as the cause |
| 4 | `radio.disable` | to server | bool (toggle) | toggle the radio |
| 5 | `description` | to client | float rotation, int cooldown ticks | render sync |

### Radar station (parent `icbmclassic:radarstation`), IDs 6 to 12

| ID | Name | Direction | Fields |
|---|---|---|---|
| 6 | `description` | to client | enum radar state (byte) |
| 7 | `gui` | to client | int trigger range, int detection range, string channel, bool disabled, bool redstone output, radar dot list |
| 8 | `radio.frequency` | to server | string channel |
| 9 | `radio.disable` | to server | bool (toggle) |
| 10 | `range.detection` | to server | int |
| 11 | `range.trigger` | to server | int |
| 12 | `redstone.output` | to server | bool (toggle) |

### Launcher base (parent `icbmclassic:launcherbase`), IDs 13 to 19

| ID | Name | Direction | Fields |
|---|---|---|---|
| 13 | `description` | to client | item stack (missile in the tube), facing (seat side) |
| 14 | `gui` | to client | int group index, int group ID, int firing delay, int lock height |
| 15 | `lock_height` | to server | int |
| 16 | `group.id` | to server | int |
| 17 | `group.index` | to server | int |
| 18 | `firing.delay` | to server | int |
| 19 | `rotation.seat` | to server | facing |

### Launcher screen (parent `icbmclassic:launcherscreen`), IDs 20 to 24

| ID | Name | Direction | Fields |
|---|---|---|---|
| 20 | `radio.frequency` | to server | string |
| 21 | `radio.disable` | to server | bool (toggle) |
| 22 | `target` | to server | `Vec3d` target |
| 23 | `description` | to client | `Vec3d` target, string channel, bool disabled, float launcher inaccuracy, `Vec3d` target again (sent twice), NBT list of launcher status entries (group, index, action status) |
| 24 | `launch` | to server | none: fire all launchers in range |

### Cruise launcher (parent `icbmclassic:cruiselauncher`), IDs 25 to 30

| ID | Name | Direction | Fields |
|---|---|---|---|
| 25 | `description` | to client | item stack, `Vec3d` target, double yaw, double pitch |
| 26 | `radio.frequency` | to server | string |
| 27 | `radio.disable` | to server | bool (toggle) |
| 28 | `target` | to server | `Vec3d` |
| 29 | `gui` | declared **to server**, but the server also sends it to GUI users every 3 ticks | int stored energy, string channel, bool disabled, `Vec3d` target: one codex serves both directions, so a client packet sets the launcher's energy to any value (ICBM-01-22) |
| 30 | `launch` | to server | none: launches the stored missile at the stored target with the player as the cause |

### Missile entity (parent `icbmclassic:missile`), ID after the tiles

| Name | Direction | Fields | Effect |
|---|---|---|---|
| `description` | none declared (both) | one NBT compound: the missile's full save data (flight logic, target, flags, timers) | Client: sync. **Server: loads the received NBT into the missile**, letting any player in the same dimension rewrite any missile's flight and target by entity ID (ICBM-01-23) |

### What the server trusts from a client (summary)

| Action | What a modified client can do |
|---|---|
| Any `to server` codex | Do it to any machine in a loaded chunk of its current dimension, at any distance, whether or not it has the GUI open or owns the machine: change radio channels, ranges, targets, group IDs; **fire an EMP tower; launch from a screen or cruise launcher** |
| Any `to client` codex | Send it the other way too: set a tower's or cruise launcher's stored energy, set a screen's status list, and so on |
| Missile `description` | Edit any missile's state (retarget it, change its flight logic) |
| Laser detonator, radar gun | Choose arbitrary coordinates |

GregGuns must check, on the server, that the sender has the machine's GUI open (or is within reach), and treat direction as enforced.

## 4. Plumbing catalog (1.12-only; one line each)

No behavior sheets for these. They are infrastructure that a 1.7.10 rebuild either does not need or replaces.

### `config/util/` (config grammar)

| Class | Purpose |
|---|---|
| `ResourceConfigList` | Base for lists of `domain:name[...]` config strings: parse by regex, per-key matchers with sort order, defaults, error report. Reload clears loaded entries but not the defaults, so repeated reloads stack up duplicate defaults (ICBM-01-30) |
| `ResourceConfigEntry` | One parsed entry: a label, a sort order, and a matcher function |
| `BlockStateConfigList` | Block-state flavor: `block`, `block:meta`, `block[prop:value,...]` with `~` wildcards, `@chance(x)`, `@nbt(...)` |
| `BlockStateConfigListOld` | Earlier block-state flavor, still used by the flying block list |
| `ItemStackConfigList` | Item-stack flavor (`domain:item[:meta]=value`), with a float-valued subclass for armor ratings |
| `BlockReplacementData` | A "replace the block with this state, with this chance, and merge this NBT" record used by EMP swaps |

### `lib/saving/` (NBT save framework)

| Class | Purpose |
|---|---|
| `INbtSaveNode`, `NbtSaveNode` | One named field with a save and a load function |
| `NbtSaveHandler` | Builder and runner: a list of nodes (and nested sub-handlers) that saves or loads an object to NBT, so each content class declares its save format once |
| `NbtSaveRoot` | Cursor into the NBT tree used while saving/loading nested levels |
| `nodes/SaveBuildableObject`, `SaveBuildableObjectList` | Save an object created by a `BuildableObjectRegistry` (id plus data) or a list of them |
| `nodes/SaveNode...` (BlockPos, BlockState, Boolean, Byte, CompoundTag, Double, Enum, Facing, Float, Integer, ItemStack, Pos, ResourceLocation, String, UUID, Vec3d) | Typed node factories |

### `lib/transform/` (vector math and world helpers)

| Class | Purpose |
|---|---|
| `vector/Pos`, `AbstractPos`, `Location`, `AbstractLocation`, `Point` | Double-precision 3D vector (and world-bound location) with arithmetic, distance, rotation and ray helpers; the mod's own vector type, used everywhere |
| `rotation/EulerAngle`, `Quaternion`, `IRotation` | Mutable yaw/pitch/roll angle, quaternion and common interface |
| `region/Cube`, `Rectangle`, `Triangle`, `Shape2D`, `Shape3D` | Geometry shapes with containment and bounds tests |
| `RotationHelper` | Facing and rotation helpers for the Minecraft axis convention |
| `PosDistanceSorter` | Comparator that sorts positions by distance from a center |
| `BlockEditHandler` | Per-dimension queue of block edits applied over ticks under a tick-time budget (70% of 50 ms), discarded when the dimension unloads |

### `prefab/` (base classes)

| Class | Purpose |
|---|---|
| `entity/EntityICBM` | Base entity with optional hit points, world-position interface |
| `tile/TileMachine` | Base tile: tick counter, list of tick actions, `isServer`/`isClient`, update-packet sync |
| `tile/BlockICBM`, `tile/IGuiTile` | Base block (rotation property) and the interface the GUI handler uses |
| `item/ItemBase`, `ItemICBMBase`, `ItemICBMElectrical`, `ItemBlockAbstract`, `ItemBlockSubTypes` | Item bases that set name, tab, texture and subtype handling; the electrical one is a stub (see energy sheet) |
| `item/ItemRadio` | Base for the two detonators: right-click a tile with a radio capability to copy its channel into the stack (NBT `radio_channel`, legacy `hz` float) |
| `item/ItemStackCapProvider` | Holds capability instances on a stack and saves any that are NBT-serializable |
| `item/LootEntryItemStack` | Loot entry for a fixed ItemStack (see lifecycle sheet and ICBM-01-27) |
| `FakeRadioSender` | Lets an item act as a radio-bus sender (used by the detonators; see the radio bus, part b) |
| `inventory/InventorySlot`, `InventoryWithSlots` | Item handler with per-slot validators, tick callbacks and change callbacks (the energy slot uses the tick callback) |
| `gui/*` (`ContainerBase`, `GuiContainerBase`, `IGuiComponent`, `IPlayerUsing`, `GuiFormatHelpers`, `NumericField`, `TextInput`, `textbox/`, `button/` x4, `components/SlotEnergyBar`, `slot/SlotEnergyItem`, `tooltip/` x4) | Container and screen scaffolding, text/number inputs, buttons, energy bar, slot that accepts energy items, tooltips |

### `client/ClientProxy`

Client-only particle spawning and the chicken-helmet render layer; behavior is in [lifecycle-and-registration §7](lifecycle-and-registration.md#7-gui-handler-and-proxies).

### Exception

`lib/network/ex/PacketTileReadException`: a runtime exception that carries the location, tile and block for logging. Not thrown by any covered code.
