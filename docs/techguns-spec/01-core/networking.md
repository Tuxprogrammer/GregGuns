# Networking

Source: `java/techguns/TGPackets.java` and `java/techguns/packets/*`.

## Channel and registration

All packets share one FML `SimpleNetworkWrapper` channel named **`Techguns`**, created in preInit (`TGPackets.init`, called from the category-init step). Discriminators are assigned in registration order, starting at 0. "Handled on" is the receiving side.

| Disc. | Message class | Direction | Handled on |
|---|---|---|---|
| 0 | GUIButtonClick | client → server | server |
| 1 | GunFiredMessage | server → client | client |
| 2 | RequestTileEntitySync | client → server | server |
| 3 | ReloadStartedMessage | server → client | client |
| 4 | PacketOpenPlayerGUI | client → server | server |
| 5 | PacketTGExtendedPlayerSync | server → client | client |
| 6 | PacketEntityDeathType | server → client | client |
| 7 | PacketTGKeybindPress | client → server | server |
| 8 | PacketShootGun | client → server | server |
| 9 | PacketPlaySound | server → client | client |
| 10 | PacketRequestTGPlayerSync | client → server | server |
| 11 | PacketTGPlayerFieldSync | server → client | client |
| 12 | PacketRequestTurretSync | client → server | server |
| 13 | PacketSpawnParticle | server → client | client |
| 14 | PacketSpawnParticleOnEntity | server → client | client |
| 15 | PacketTGMessage | server → client | client |

Two classes in the package are **not registered**:
- `PacketClientOpenGui` (deprecated): one byte, and id 0 opened the vanilla inventory on the client. Its old registration is commented out.
- `PacketGrenadeBounce`: entity id (int) plus motion X/Y/Z (doubles); the client would set the entity's motion. Nothing sends it.

`TGPackets.getPlayerFromContext(ctx)` returns the local client player when handling on the client, and the sending `EntityPlayerMP` on the server.

**Threading:** every handler runs directly on the Netty I/O thread, as 1.7.10 `SimpleNetworkWrapper` does by default. None of them hands work to the main thread, yet they mutate world, entity and inventory state (CORE-05). A rewrite should run the handler logic on the main thread.

Field encodings: `int` = 4 bytes, `short` = 2, `byte` = 1, `bool` = 1, `float` = 4, `double` = 8, all big-endian (Netty defaults). Strings are encoded with the JVM's default charset (CORE-11).

## Per-packet reference

### 0 · GUIButtonClick (C→S)
- **Fields:** x int, y int, z int, buttonId int. The client builds it from a `BasicInventoryTileEnt`'s coordinates.
- **Handler:** looks up the tile entity at x/y/z in the sender's world. If it is a `BasicInventoryTileEnt`, it calls `buttonClicked(buttonId, player)`. What each button ID does is machine-specific (08/09). There is **no distance or permission check** here, and the lookup can force-load the chunk (CORE-08).

### 1 · GunFiredMessage (S→C)
- **Fields:** entityID int (shooter), time int (recoil duration in ms), attackType byte, checkRecoil bool, akimboGun bool, doubleShot bool.
- **Sender:** guns and NPC shooters when they fire (02, 10).
- **Handler:** resolves the shooter as an `EntityLivingBase`. It proceeds if `checkRecoil` is false, or if the shooter is **not** "still recoiling" for this attack type (see [client-effects-audio.md](client-effects-audio.md#attack-timing-recoil--reload)). Proceeding means:
  1. Recoil end = now + time; recoil total = time; attack type recorded.
  2. Muzzle flash FX: with `doubleShot`, it spawns for both the left and the right gun. Otherwise it spawns once, mirrored to the left gun only when `akimboGun` is set and attackType == 1.

### 2 · RequestTileEntitySync (C→S)
- **Fields:** x int, y short, z int.
- **Handler:** if a tile entity exists at the position, its `getDescriptionPacket()` is sent straight back to the requester. Used by client tiles that need their state on load (turret base, bio blob, ...). There is no range check, and a null description packet would cause an NPE (CORE-08).

### 3 · ReloadStartedMessage (S→C)
- **Fields:** entityID int, time int (reload duration, ms), attackType byte.
- **Handler:** if the entity exists and is **not** the local player (whose reload is already predicted locally), it records reload end = now + time, total = time, attackType. Third-person reload animations read this.

### 4 · PacketOpenPlayerGUI (C→S)
- **Fields:** guiID short.
- **Senders:** the inventory tab buttons (vanilla-inventory tab, TConstruct tab, tabs inside the Techguns inventory).
- **Handler:** if guiID > 0, it calls `player.openGui(Techguns, guiID, world, (int)posX, (int)posY, (int)posZ)`. IDs ≤ 0 are ignored; the client opens the vanilla inventory locally instead. The server accepts **any** positive ID, so a client can request machine GUIs at its own position, where they would be built with a missing or wrong tile entity (CORE-06).

### 5 · PacketTGExtendedPlayerSync (S→C)
- **Fields, in wire order:**
  1. entityId int
  2. radlevel int
  3. size byte (number of inventory slots that follow)
  4. `size` ItemStacks in the [PacketUtil format](utilities.md#packetutil-itemstack-wire-format)
  5. **only if size > 2:** foodLeft short, lastSaturation float, nightVisionEnabled bool, safemodeEnabled bool, hovermodeEnabled bool, stepAssist bool, showHUD bool, enableJetpack bool
- **Two modes:**
  - *Full* (`allSlots = true`): size = 12, all fields. Sent to the owning player.
  - *Partial* (`allSlots = false`): size = 2 (face and back slots), no trailing fields. Sent to observers.
- **Handler:** finds the player entity by ID and registers extended properties for it if it has none. It then **unconditionally** copies radlevel, lastSaturation, foodleft and the six flags, and copies `size` inventory slots. In partial mode the flags were never transmitted, so they are overwritten with the receiver-side defaults: food 0, saturation 0, NV false, safe true, hover false, step false, HUD false, jetpack false (CORE-09).
- **When sent (full mode, to the player only):** on EntityJoinWorld (server), after every keybind toggle that changes a flag, and after every auto-feed step. Partial mode is only sent in answer to `PacketRequestTGPlayerSync`.

### 6 · PacketEntityDeathType (S→C)
- **Fields:** entityId int, deathtypeId int (`DeathType` ordinal: 0 DEFAULT, 1 GORE, 2 BIO, 3 LASER, 4 DISMEMBER), motionX/Y/Z float (the entity's motion at death).
- **Sender:** `TechgunsEventhandler.onLivingDeathEvent` (server). Sent to all players within 100 blocks of the dying entity, when the damage source is a `TGDamageSource` with a non-DEFAULT death type and `EntityDeathUtils.hasSpecialDeathAnim` is true ([utilities.md](utilities.md#entitydeathutils)).
- **Handler:**
  1. Does nothing if `EnableDeathEffects` is off.
  2. Does nothing for GORE when `EnableGoreDeathEffect` is off.
  3. Otherwise, if the entity is found: copy the motion onto the entity, record the death type in ClientProxy, and run `DeathEffect.createDeathEffect`.
  4. Play the death sound on the entity (category DEATHEFFECT, volume 1, pitch 1, not moving):

     | Death type | Sound |
     |---|---|
     | BIO | `techguns:effects.biodeath` |
     | GORE | `techguns:effects.gore` |
     | LASER | `techguns:effects.disintegrate` |
     | DEFAULT, DISMEMBER | none |

### 7 · PacketTGKeybindPress (C→S)
- **Fields:** buttonID byte. Values come from `TGKeybindsID`:

  | ID | Constant | Sent by |
  |---|---|---|
  | 0 | TOGGLE_NIGHTVISION | N key; Techguns-inventory toggle button 1 |
  | 1 | TOGGLE_SAFEMODE | B key; toggle button 2 |
  | 2 | JETPACK_BOOST_START | jetpack and jump-pack items when jump goes down (05) |
  | 3 | JETPACK_BOOST_STOP | when jump is released, or the pack stops boosting |
  | 4 | JETPACK_FORWARD_START | never sent (the sender is commented out) |
  | 5 | JETPACK_FORWARD_STOP | never sent |
  | 6 | FORCE_RELOAD | R key |
  | 7 | TOGGLE_JETPACK | J key; toggle button 4 |
  | 8 | TOGGLE_STEP_ASSIST | V key; toggle button 3 |
  | 9 | TOGGLE_HUD | toggle button 0 in the Techguns inventory (no key) |

- **Server handling:**
  - **0 TOGGLE_NIGHTVISION:** flips `enableNightVision`.
    - Turned off: if the player has *any* Night Vision potion effect, remove it and play `techguns:nightvision.off` at the player (vanilla world sound, volume 1, pitch 1). This also strips potion-granted night vision (CORE-15).
    - Turned on: if the worn armor gives a NIGHTVISION bonus > 0, play `techguns:nightvision.on`.
    - Then send a full extended sync.
  - **1 TOGGLE_SAFEMODE:** flips `enableSafemode`. If it ends up off and `allowUnsafemode(player)` is false, it is forced back on. Then full sync.
  - **7 TOGGLE_JETPACK:** only while the jump key is *not* held.
    1. Flip `enableJetpack`.
    2. Send `PacketTGPlayerFieldSync(FIELD_ENABLEJETPACK)` to the player.
    3. If the jetpack was just disabled while the jump flag was still set, clear it and broadcast `FIELD_JUMPBUTTONPRESSED=false` to the dimension. Because of the guard above, this can't actually happen.
  - **2 JETPACK_BOOST_START / 3 STOP:** set the server jump-key flag to true or false, and broadcast `FIELD_JUMPBUTTONPRESSED` with that value to everyone in the dimension so they hear the jetpack.
  - **4 / 5 forward start/stop:** set `isForwardKeyPressed`.
  - **6 FORCE_RELOAD:** if the held item is a `GenericGun`, call its `tryForcedReload` (02).
  - **9 TOGGLE_HUD:** flip `showTGHudElements`, then full sync.
  - **8 TOGGLE_STEP_ASSIST:** flip `enableStepAssist`, then full sync.

### 8 · PacketShootGun (C→S)
- **Fields:** isZooming bool.
- **Sender:** the client tick handler while the fire button is held with a left-click gun ([input-and-hud.md](input-and-hud.md#firing-guns-with-the-left-mouse-button)).
- **Handler:** if the held item is a `GenericGun`, call `shootGunLeftClick(stack, world, player, isZooming)` (02). The client calls the same method locally for prediction.

### 9 · PacketPlaySound (S→C)
- **Fields, in wire order:** entityId int (−1 means positional), volume float, pitch float, repeat bool, moving bool, gunPosition bool, playOnOwnPlayer bool, soundx int, soundy short, soundz int, category byte (`TGSoundCategory` ordinal), then the sound name as raw bytes filling the rest of the payload (no length prefix).
- **Constructors:** entity-bound with or without gunPosition/playOnOwnPlayer; positional (x, y, z) with entity −1.
- **Handler:**
  - entityId ≠ −1: `proxy.handleSoundEvent`. The entity is looked up and, if found, the sound plays **unless the entity is the local player and `playOnOwnPlayer` is false**, because the local player already played it as a prediction.
  - entityId = −1: plays at (soundx, soundy, soundz).
- Details in [client-effects-audio.md](client-effects-audio.md#tgsound).

### 10 · PacketRequestTGPlayerSync (C→S)
- **Fields:** entityID int (the tracked player).
- **Handler:** if that entity is a player with extended properties, send a *partial* (face and back slot) `PacketTGExtendedPlayerSync` of it to the requester.
- **Intended sender:** the client side of `PlayerEvent.StartTracking`. That event only fires on the server, so the request is never sent (CORE-10). Other players' face and back gear still render because those two slots are also mirrored through DataWatcher.

### 11 · PacketTGPlayerFieldSync (S→C)
- **Fields:** entityID int, id short, value (bool when id is 0 or 1).
- **Field IDs:**
  - 0 = FIELD_JUMPBUTTONPRESSED (jetpack boosting)
  - 1 = FIELD_ENABLEJETPACK
- **Handler (uses the client world directly):**
  - Field 0, on any player **except** the local one: `setJumpkeyPressed(value)`. On the client this starts or stops the jetpack loop and plays the start/stop sounds.
  - Field 1, on any player: set `enableJetpack`.

### 12 · PacketRequestTurretSync (C→S)
- **Fields:** turret_entityId int.
- **Handler:** if the entity is an `NPCTurret` with a mounted base tile entity, the base's description packet is sent to the requester. Used by the client turret to relink to its base (09/10).

### 13 · PacketSpawnParticle (S→C)
- **Fields:** name length short, name bytes, posX/Y/Z double, motionX/Y/Z double (default 0).
- **Handler:** `ClientProxy.createFX(name, …)` builds the FXList particle systems by name (04) and spawns them in the client world.

### 14 · PacketSpawnParticleOnEntity (S→C)
- **Fields:** name length short, name bytes, entityID int.
- **Handler:** if the entity exists, `createFXOnEntity(name, entity)` attaches the FX systems to it. Otherwise it logs "Got Packet for FX … but ent was null".
- **Example:** the dispenser sends `GrenadeLauncherTrail` to everyone within 200 blocks after dispensing a 40 mm grenade.

### 15 · PacketTGMessage (S→C)
- **Fields:** messageType byte, x int, y int, z int.
- **Handler:** adds a chat line: the translated `techguns.tgmessage.<type>`, a space, then `x, y, z`. The only type in use is 0, `techguns.tgmessage.0` = "Invalid Block for Reaction Chamber at", sent by the reaction chamber multiblock check (08).

## Sync model summary

| State | Mechanism |
|---|---|
| Own player's extended data (12 slots, food buffer, flags, rad level) | full `PacketTGExtendedPlayerSync` on join and after each change |
| Other players' face and back items (rendering) | DataWatcher slots 23 and 24 (configurable), refreshed every server player tick |
| Other players' jetpack boost state | `PacketTGPlayerFieldSync` field 0, broadcast to the dimension |
| Recoil, reload and muzzle flash of any shooter | `GunFiredMessage`, `ReloadStartedMessage` |
| Sounds | `PacketPlaySound`, normally sent to all within 100 blocks by `SoundUtil` |
| Particle FX | `PacketSpawnParticle` / `PacketSpawnParticleOnEntity` |
| Tile-entity description refresh | `RequestTileEntitySync`, `PacketRequestTurretSync` |
