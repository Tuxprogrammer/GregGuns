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
- **GregGuns does:** one `NetworkChannel("gregguns")`, held by `com.gregguns.network.GregGunsNetwork` and created in `CommonProxy.preInit`. Packets register in a fixed order (all client-bound, then all server-bound, each in list order), because the order is the wire ID. Append only.
- **Verified at 0.11.52:** the API matches this sheet. Two details to know: `IPacket` is itself the `IMessageHandler`, and exceptions thrown by `executeServer` or `executeClient` are not caught by GTNHLib, so the GregGuns base classes catch them. `fromBytes` also lets decode errors escape into Netty, so the base classes override it.
- **Deviation (package):** the sheet said `com.gregguns.net`; the package is `com.gregguns.network`, as CONTRIBUTING.md's architecture lists.
- **Deviation (validation shape):** validation is not static methods on each packet. It is declarative rule sets (`Rules`, `SenderRules`, `ClientRules` in `com.gregguns.network.validation`), plain Java with no Minecraft types, driven by one `PacketPipeline`. This keeps the rules reusable and unit-testable.
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

## Writing a packet

1. Extend `ServerboundPacket` (client to server) or `ClientboundPacket` (server to client) from `com.gregguns.network`. Give it a public no-argument constructor and implement `encode` and `decode` (strings via `PacketStrings`).
2. Declare rules in `validate(...)`. Server-bound rules: `sameDimension`, `withinReachOfBlock` and `withinReachOf` (default 8 blocks), `indexInRange`, `ordinalInRange`, `inRange`, `finite`, `targetOfType` and `targetExists` (use `WorldTargets`, which never loads chunks), and `authorized`. The base class always checks first that the sender is alive and not spectator-like, and calls the `isAuthorised(Sender)` hook last, after the target is known to exist. Later milestones override that hook for ownership checks.
3. Do the work in `handle(...)`. It runs on the main thread and only after every rule passed, so validated targets can be cast without further checks.
4. Append an instance to `GregGunsNetwork.serverboundPackets()` or `clientboundPackets()`.
5. Unit-test the rules, following `ServerboundPacketTest` and `ClientboundPacketTest`.

A failed packet is dropped and logged at debug level through a rate limiter (20 reports per 10 seconds, then a suppressed count), so a hostile client cannot flood the log. The Javadoc on `ServerboundPacket` and `ClientboundPacket` has full examples.

## Tests

- GGS-01-07 (written, `GregGunsNetworkTest`): a unit test lists the registered packet classes and asserts each is an `IPacket`.
- GGS-01-08: code review against the `IPacket` contract, plus a `runServer` smoke test that sends one packet from a test client.
- GGS-01-09, GGS-01-10 (written: `RulesTest`, `SenderRulesTest`, `ServerboundPacketTest`, `ClientboundPacketTest`): unit tests on the validation rules and both base classes (missing target, wrong type, out of range, wrong dimension, too far, unauthorized). Regression tests are named `doesNotReproduceCORE05_...`, `CORE06`, `CORE08` and `CORE40`. The tests need GTNHLib on the test classpath (`testImplementation` in `dependencies.gradle`).
- GGS-01-11 (written, `NoDataWatcherTest`): scans the compiled GregGuns classes for any constant-pool reference to `net/minecraft/entity/DataWatcher` and fails on one. It checks the class, not just `addObject`, which is stricter; a self-test proves the scan reports a planted class.
- GGS-01-12 (written, `PacketStringsTest`): `PacketStrings` round-trips empty, ASCII, non-ASCII (2, 3 and 4-byte UTF-8), and maximum-length (32767 chars) strings through `PacketBuffer`, rejects an over-long write and a read over the field limit. None of the current packets (`PacketGunTrigger`, `PacketGunZoom`, `PacketGunEvent`) carries a string; a guard test fails when a registered packet gains a `String` field without being listed for its own round trip.

## Open questions

- Face and back cosmetic items: are they worth a tracking packet, or should GregGuns use Baubles Expanded or TravellersGear slots instead? That is a [05-armor](../../techguns-spec/05-armor/index.md) decision and needs a record before the armor milestone.
