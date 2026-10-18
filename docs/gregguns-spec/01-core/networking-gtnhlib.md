# Networking and player sync on GTNHLib

- **Techguns baseline:** [networking.md](../../techguns-spec/01-core/networking.md) (channel `Techguns`, 16 packets), [sync model](../../techguns-spec/01-core/networking.md#sync-model-summary), [DataWatcher mirrors](../../techguns-spec/01-core/player-state.md#datawatcher-mirrors), [CORE-05](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-06](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-08](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-11](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-40](../../techguns-spec/01-core/index.md#suspected-bugs)
- **Design records:** none decide networking directly
- **Pack mods:**
  - GTNHLib (`gtnhlib`, 0.11.52)
  - NotEnoughIDs (`neid`; config only, no source read)
  - Hodgepodge (`hodgepodge`, 2.7.215; config only)
- **Roadmap:** M0 (channel and helpers); each packet arrives with its feature

## What changes from Techguns

Techguns registers 16 classes on a raw `SimpleNetworkWrapper` and runs every handler on the Netty thread. GregGuns builds packets on GTNHLib's `IPacket` and `NetworkChannel`.

| Techguns | GregGuns | Why |
|---|---|---|
| `IMessage` plus a separate `IMessageHandler`; the handler runs on the Netty thread | `IPacket` with `encode`, `decode`, `executeClient`, `executeServer` | `IPacket.onMessage` already reschedules onto the Minecraft thread. Fixes CORE-05 for every packet at once |
| Strings read with `readBytes(len).array()` and the platform charset (CORE-11) | `PacketBuffer` string methods inside `encode` and `decode` | No heap-buffer assumption, fixed charset |
| Discriminators assigned by hand in `TGPackets` | `NetworkChannel.toClient` and `toServer` assign IDs in registration order | One place, no gaps |
| Handlers cast entities and index enums unchecked (CORE-40) | Each `execute*` method validates before use | Edge-case crashes |
| Server accepts any GUI ID, tile position or button press (CORE-06, CORE-08) | Server checks the target, distance and permission before acting | Exploit and crash fixes |
| Face and back items mirrored through player DataWatcher IDs 23 and 24, with the IDs set by config | A packet to the players tracking the wearer (`sendToAllTrackingAndSelf`) carries the cosmetic stacks | The pack leaves NEID's DataWatcher extension off, so the vanilla limit of 31 applies |

## Integration points

### GTNHLib: `IPacket` and `NetworkChannel`
- **Pack config:** none.
- **Source:**
  - [`IPacket.onMessage`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/network/base/IPacket.java#L26-L48): on the client it calls `executeClient` directly when already on the Minecraft thread, otherwise it schedules it (`func_152344_a`); on the server it does the same through `ServerThreadUtil.addScheduledTask`.
  - [`encode`/`decode`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/network/base/IPacket.java#L21-L23) and [`executeClient`/`executeServer`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/network/base/IPacket.java#L73-L80).
  - [`NetworkChannel`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/network/base/NetworkChannel.java#L15): `new NetworkChannel(String channel)`, `toClient` and `toServer` (L26, L31), plus `sendTo`, `sendToAll`, `sendToAllAround`, `sendToDimension`, `sendToAllTracking` and `sendToAllTrackingAndSelf`.
  - Precedent: GTNHLib's own channel in [`NetworkHandler`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/network/NetworkHandler.java) registers seven packets this way.
- **GregGuns does:** one `NetworkChannel("gregguns")` in `com.gregguns.net`. Packets register in a fixed order, because the order is the wire ID. Validation logic sits in static methods so unit tests can call it without Minecraft.
- **Pack-side change:** none.

### GTNHLib: config sync packet
- **Pack config:** none.
- **Source:** `PacketSyncConfig` is registered by [`NetworkHandler.init`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/network/NetworkHandler.java). It carries `@Config.Sync` fields ([config-gtnhlib.md](config-gtnhlib.md)).
- **GregGuns does:** relies on it, so no GregGuns packet is needed to push config.
- **Pack-side change:** none.

### NotEnoughIDs: DataWatcher limit
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/NEID.cfg`, key `ExtendDataWatcher` = `false`. The comment says the vanilla limit is 31 and the extended limit is 127.
- **Source:** the config comment only. GregGuns reads no NEID code.
- **GregGuns does:** takes no DataWatcher slot on any entity. Techguns' IDs 23 and 24 are inside the vanilla range, but if another mod picks the same free slot on `EntityPlayer`, the game crashes on player construction (see [`ID Conflicts`](../../techguns-spec/01-core/config.md#category-id-conflicts)).
- **Pack-side change:** none.

### Hodgepodge: potion array (checked, no change)
- **Pack config:** `config/hodgepodge.cfg`, keys `enlargePotionArray` = `true`, `fixPotionLimit` = `true`.
- **Source:** [`MixinPotion`](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.215/src/main/java/com/mitchej123/hodgepodge/mixins/early/minecraft/MixinPotion.java) copies `Potion.potionTypes` into a 256-entry array.
- **GregGuns does:** nothing. The Techguns config has no potion ID key, and none of the 01-core sheets record a custom `Potion`. GregGuns applies vanilla effects only. If a later milestone adds a custom potion, it takes its ID from the enlarged array, and that needs a record first.
- **Pack-side change:** none.

## Requirements

- **GGS-01-07** All GregGuns packets implement GTNHLib `IPacket` and are registered on a single `NetworkChannel`.
- **GGS-01-08** No packet handler touches world, entity, inventory or tile state off the Minecraft thread.
- **GGS-01-09** A server-bound packet that names an entity, tile or GUI is rejected (logged at debug, no exception) when the target is missing, the wrong type or unloaded, when a tile or GUI target is more than 8 blocks from the sender, or when the sender lacks permission.
- **GGS-01-10** A client-bound packet that names an entity or an enum index is ignored when the entity is missing or not the expected type, or the index is out of range.
- **GGS-01-11** GregGuns claims no DataWatcher slot on any entity.
- **GGS-01-12** Packet strings are written with `PacketBuffer` string methods only.

## Tests

- GGS-01-07: a unit test lists the registered packet classes and asserts each is an `IPacket`.
- GGS-01-08: code review against the `IPacket` contract, plus a `runServer` smoke test that sends one packet from a test client.
- GGS-01-09, GGS-01-10: unit tests on the static validation methods (missing target, wrong type, out of range, too far).
- GGS-01-11: a unit test scans GregGuns classes for calls to `DataWatcher.addObject` and fails on any.
- GGS-01-12: a round-trip test of every packet with non-ASCII text.

## Open questions

- Face and back cosmetic items: are they worth a tracking packet, or should GregGuns use Baubles Expanded or TravellersGear slots instead? That is a [05-armor](../../techguns-spec/05-armor/index.md) decision and needs a record before the armour milestone.
