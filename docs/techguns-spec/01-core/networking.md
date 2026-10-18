# Networking

Source: `java/techguns/TGPackets.java` and `java/techguns/packets/*`.

## Channel and registration

All packets share one FML `SimpleNetworkWrapper` channel named **[`Techguns`](lifecycle-and-registration.md#lifecycle-overview)**, created in preInit (`TGPackets.init`, called from the category-init step). Discriminators are assigned in registration order, starting at 0. "Handled on" is the receiving side.

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

**Threading:** every handler runs directly on the Netty I/O thread, as 1.7.10 `SimpleNetworkWrapper` does by default. None of them hands work to the main thread, yet they mutate world, entity and inventory state ([CORE-05](index.md#suspected-bugs)). A rewrite should run the handler logic on the main thread.

Field encodings: `int` = 4 bytes, `short` = 2, `byte` = 1, `bool` = 1, `float` = 4, `double` = 8, all big-endian (Netty defaults). Strings are encoded with the JVM's default charset ([CORE-11](index.md#suspected-bugs)).

## Per-packet reference

### 0 · GUIButtonClick (C→S)
- **Fields:** x int, y int, z int, buttonId int. The client builds it from a [`BasicInventoryTileEnt`](../08-machines-production/framework.md#2-basicinventorytileent)'s coordinates.
- **Handler:** looks up the tile entity at x/y/z in the sender's world. If it is a `BasicInventoryTileEnt`, it calls `buttonClicked(buttonId, player)`. What each button ID does is machine-specific ([08](../08-machines-production/index.md)/[09](../09-machines-utility/index.md)). There is **no distance or permission check** here, and the lookup can force-load the chunk ([CORE-08](index.md#suspected-bugs)).

### 1 · GunFiredMessage (S→C)
- **Fields:** entityID int (shooter), time int (recoil duration in ms), attackType byte, checkRecoil bool, akimboGun bool, doubleShot bool.
- **Sender:** guns and NPC shooters when they fire ([02](../02-guns/index.md), [10](../10-entities-npcs/index.md)).
- **Handler:** resolves the shooter as an `EntityLivingBase`. It proceeds if `checkRecoil` is false, or if the shooter is **not** "still recoiling" for this attack type (see [client-effects-audio.md](client-effects-audio.md#attack-timing-recoil--reload)). Proceeding means:
  1. Recoil end = now + time; recoil total = time; attack type recorded.
  2. Muzzle flash FX: with `doubleShot`, it spawns for both the left and the right gun. Otherwise it spawns once, mirrored to the left gun only when `akimboGun` is set and attackType == 1.

### 2 · RequestTileEntitySync (C→S)
- **Fields:** x int, y short, z int.
- **Handler:** if a tile entity exists at the position, its `getDescriptionPacket()` is sent straight back to the requester. Used by client tiles that need their state on load (turret base, bio blob, ...). There is no range check, and a null description packet would cause an NPE (CORE-08).
- *Correction (consistency pass):* the only sender in the source is [`TurretBaseEnt`](../09-machines-utility/turret.md#tile-entity-turretbaseent) (client side, `tileentities/TurretBaseEnt.java:249`). The bio blob does **not** send this packet; the "bio blob" mention above is wrong.

### 3 · ReloadStartedMessage (S→C)
- **Fields:** entityID int, time int (reload duration, ms), attackType byte.
- **Handler:** if the entity exists and is **not** the local player (whose reload is already predicted locally), it records reload end = now + time, total = time, attackType. Third-person reload animations read this.

### 4 · PacketOpenPlayerGUI (C→S)
- **Fields:** guiID short.
- **Senders:** the inventory tab buttons (vanilla-inventory tab, TConstruct tab, tabs inside the Techguns inventory).
- **Handler:** if guiID > 0, it calls `player.openGui(Techguns, guiID, world, (int)posX, (int)posY, (int)posZ)`. IDs ≤ 0 are ignored; the client opens the vanilla inventory locally instead. The server accepts **any** positive ID, so a client can request machine GUIs at its own position, where they would be built with a missing or wrong tile entity ([CORE-06](index.md#suspected-bugs)).

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
- **Handler:** finds the player entity by ID and registers extended properties for it if it has none. It then **unconditionally** copies radlevel, lastSaturation, foodleft and the six flags, and copies `size` inventory slots. In partial mode the flags were never transmitted, so they are overwritten with the receiver-side defaults: food 0, saturation 0, NV false, safe true, hover false, step false, HUD false, jetpack false ([CORE-09](index.md#suspected-bugs)).
- **When sent (full mode, to the player only):** on EntityJoinWorld (server), after every keybind toggle that changes a flag, and after every auto-feed step. Partial mode is only sent in answer to `PacketRequestTGPlayerSync`.

### 6 · PacketEntityDeathType (S→C)
- **Fields:** entityId int, deathtypeId int (`DeathType` ordinal: 0 DEFAULT, 1 GORE, 2 BIO, 3 LASER, 4 DISMEMBER), motionX/Y/Z float (the entity's motion at death).
- **Sender:** [`TechgunsEventhandler.onLivingDeathEvent`](events.md#techgunseventhandler-forge-bus) (server). Sent to all players within 100 blocks of the dying entity, when the damage source is a [`TGDamageSource`](../02-guns/damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource) with a non-DEFAULT death type and [`EntityDeathUtils.hasSpecialDeathAnim`](../04-projectiles-fx/index.md#coverage) is true ([utilities.md](utilities.md#entitydeathutils)).
- **Handler:**
  1. Does nothing if [`EnableDeathEffects`](config.md#category-clientside) is off.
  2. Does nothing for GORE when [`EnableGoreDeathEffect`](config.md#category-clientside) is off.
  3. Otherwise, if the entity is found: copy the motion onto the entity, record the death type in ClientProxy, and run [`DeathEffect.createDeathEffect`](client-effects-audio.md#deatheffectcreatedeatheffectentity-type-mx-my-mz).
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
  | 2 | JETPACK_BOOST_START | jetpack and jump-pack items when jump goes down ([05](../05-armor/index.md)) |
  | 3 | JETPACK_BOOST_STOP | when jump is released, or the pack stops boosting |
  | 4 | JETPACK_FORWARD_START | never sent (the sender is commented out) |
  | 5 | JETPACK_FORWARD_STOP | never sent |
  | 6 | FORCE_RELOAD | R key |
  | 7 | TOGGLE_JETPACK | J key; toggle button 4 |
  | 8 | TOGGLE_STEP_ASSIST | V key; toggle button 3 |
  | 9 | TOGGLE_HUD | toggle button 0 in the Techguns inventory (no key) |

- **Server handling:**
  - **0 TOGGLE_NIGHTVISION:** flips `enableNightVision`.
    - Turned off: if the player has *any* Night Vision potion effect, remove it and play `techguns:nightvision.off` at the player (vanilla world sound, volume 1, pitch 1). This also strips potion-granted night vision ([CORE-15](index.md#suspected-bugs)).
    - Turned on: if the worn armor gives a NIGHTVISION bonus > 0, play `techguns:nightvision.on`.
    - Then send a full extended sync.
  - **1 TOGGLE_SAFEMODE:** flips `enableSafemode`. If it ends up off and `allowUnsafemode(player)` is false, it is forced back on. Then full sync.
  - **7 TOGGLE_JETPACK:** only while the jump key is *not* held.
    1. Flip `enableJetpack`.
    2. Send `PacketTGPlayerFieldSync(FIELD_ENABLEJETPACK)` to the player.
    3. If the jetpack was just disabled while the jump flag was still set, clear it and broadcast `FIELD_JUMPBUTTONPRESSED=false` to the dimension. Because of the guard above, this can't actually happen.
  - **2 JETPACK_BOOST_START / 3 STOP:** set the server jump-key flag to true or false, and broadcast `FIELD_JUMPBUTTONPRESSED` with that value to everyone in the dimension so they hear the jetpack.
  - **4 / 5 forward start/stop:** set `isForwardKeyPressed`.
  - **6 FORCE_RELOAD:** if the held item is a [`GenericGun`](../02-guns/gun-lifecycle.md), call its `tryForcedReload` ([02](../02-guns/index.md)).
  - **9 TOGGLE_HUD:** flip `showTGHudElements`, then full sync.
  - **8 TOGGLE_STEP_ASSIST:** flip `enableStepAssist`, then full sync.

### 8 · PacketShootGun (C→S)
- **Fields:** isZooming bool.
- **Sender:** the client tick handler while the fire button is held with a left-click gun ([input-and-hud.md](input-and-hud.md#firing-guns-with-the-left-mouse-button)).
- **Handler:** if the held item is a `GenericGun`, call `shootGunLeftClick(stack, world, player, isZooming)` ([02](../02-guns/index.md)). The client calls the same method locally for prediction.

### 9 · PacketPlaySound (S→C)
- **Fields, in wire order:** entityId int (−1 means positional), volume float, pitch float, repeat bool, moving bool, gunPosition bool, playOnOwnPlayer bool, soundx int, soundy short, soundz int, category byte ([`TGSoundCategory`](client-effects-audio.md#tgsoundcategory) ordinal), then the sound name as raw bytes filling the rest of the payload (no length prefix).
- **Constructors:** entity-bound with or without gunPosition/playOnOwnPlayer; positional (x, y, z) with entity −1.
- **Handler:**
  - entityId ≠ −1: `proxy.handleSoundEvent`. The entity is looked up and, if found, the sound plays **unless the entity is the local player and `playOnOwnPlayer` is false**, because the local player already played it as a prediction.
  - entityId = −1: plays at (soundx, soundy, soundz).
- Details in [client-effects-audio.md](client-effects-audio.md#tgsound).

### 10 · PacketRequestTGPlayerSync (C→S)
- **Fields:** entityID int (the tracked player).
- **Handler:** if that entity is a player with extended properties, send a *partial* (face and back slot) `PacketTGExtendedPlayerSync` of it to the requester.
- **Intended sender:** the client side of `PlayerEvent.StartTracking`. That event only fires on the server, so the request is never sent ([CORE-10](index.md#suspected-bugs)). Other players' face and back gear still render because those two slots are also mirrored through DataWatcher.

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
- **Handler:** if the entity is an [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) with a mounted base tile entity, the base's description packet is sent to the requester. Used by the client turret to relink to its base ([09](../09-machines-utility/index.md)/[10](../10-entities-npcs/index.md)).

### 13 · PacketSpawnParticle (S→C)
- **Fields:** name length short, name bytes, posX/Y/Z double, motionX/Y/Z double (default 0).
- **Handler:** [`ClientProxy.createFX(name, …)`](lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) builds the FXList particle systems by name ([04](../04-projectiles-fx/index.md)) and spawns them in the client world.

### 14 · PacketSpawnParticleOnEntity (S→C)
- **Fields:** name length short, name bytes, entityID int.
- **Handler:** if the entity exists, `createFXOnEntity(name, entity)` attaches the FX systems to it. Otherwise it logs "Got Packet for FX … but ent was null".
- **Example:** the dispenser sends `GrenadeLauncherTrail` to everyone within 200 blocks after dispensing a 40 mm grenade.

### 15 · PacketTGMessage (S→C)
- **Fields:** messageType byte, x int, y int, z int.
- **Handler:** adds a chat line: the translated `techguns.tgmessage.<type>`, a space, then `x, y, z`. The only type in use is 0, `techguns.tgmessage.0` = "Invalid Block for Reaction Chamber at", sent by the reaction chamber multiblock check ([08](../08-machines-production/index.md)).

## Sync model summary

| State | Mechanism |
|---|---|
| Own player's extended data (12 slots, food buffer, flags, rad level) | full `PacketTGExtendedPlayerSync` on join and after each change |
| Other players' face and back items (rendering) | DataWatcher slots 23 and 24 (configurable), refreshed every server player tick |
| Other players' jetpack boost state | `PacketTGPlayerFieldSync` field 0, broadcast to the dimension |
| Recoil, reload and muzzle flash of any shooter | `GunFiredMessage`, `ReloadStartedMessage` |
| Sounds | `PacketPlaySound`, normally sent to all within 100 blocks by [`SoundUtil`](utilities.md#soundutil) |
| Particle FX | `PacketSpawnParticle` / `PacketSpawnParticleOnEntity` |
| Tile-entity description refresh | `RequestTileEntitySync`, `PacketRequestTurretSync` |

## Packet entity sheets (consistency pass)

One sheet per packet, using the CONVENTIONS fields that apply to a packet: sender, direction, wire fields and types, validation, handler logic (thread and side), side effects, bug IDs. Source for all of them: `java/techguns/packets/<Class>.java` and `java/techguns/TGPackets.java`.

**Common to every handler.**
- Handlers run on the Netty I/O thread and are never scheduled onto the main thread ([CORE-05](index.md#suspected-bugs)).
- A server-bound handler gets the sender through `TGPackets.getPlayerFromContext`, which is the `EntityPlayerMP` that owns the connection. Nothing in any handler checks a permission, a distance or a cooldown unless a row below says so.
- Every handler returns `null` (no reply packet). Replies are separate packets.
- Byte counts are the payload size after the discriminator byte that `SimpleNetworkWrapper` adds. No Techguns packet carries a version or length header of its own, except the two particle packets, which prefix the name with a short length.
- `TargetPoint` radii are in blocks and are measured in the sender's dimension.

### Wire-size and routing summary

| Disc. | Class | Payload bytes | Sent via | Registered side | Live senders in source |
|---|---|---|---|---|---|
| 0 | `GUIButtonClick` | 16 | `sendToServer` | SERVER | 5 GUI classes |
| 1 | `GunFiredMessage` | 12 | `sendToAllAround`, r = 100 | CLIENT | `GenericGun` |
| 2 | `RequestTileEntitySync` | 10 | `sendToServer` | SERVER | `TurretBaseEnt` |
| 3 | `ReloadStartedMessage` | 9 | `sendToAllAround`, r = 100 | CLIENT | `GenericGun` (2 sites) |
| 4 | `PacketOpenPlayerGUI` | 2 | `sendToServer` | SERVER | [`TGGuiTabButton`](../05-armor/extra-inventory.md), [`TGInventoryTab`](integrations.md#client-inventory-tab) |
| 5 | `PacketTGExtendedPlayerSync` | 9 + stacks (+12 in full mode) | `sendTo` one player | CLIENT | event handler, tick handler, keybind handler |
| 6 | `PacketEntityDeathType` | 20 | `sendToAllAround`, r = 100 | CLIENT | `TechgunsEventhandler` |
| 7 | `PacketTGKeybindPress` | 1 | `sendToServer` | SERVER | [`TGKeybinds`](input-and-hud.md#keybinds-tgkeybinds-client-only), [`TGGuiToggleButton`](../05-armor/extra-inventory.md), [`ItemJetpack`](../05-armor/special-equipment.md#43-jetpack-itemjetpack) |
| 8 | `PacketShootGun` | 1 | `sendToServer` | SERVER | [`TechgunsTickHandler`](events.md#techgunstickhandler-fml-bus) |
| 9 | `PacketPlaySound` | 27 + name length | `sendToAllAround` (r = 50 or 100) | CLIENT | `SoundUtil`, tick handler, jump pack, chainsaw, power hammer |
| 10 | `PacketRequestTGPlayerSync` | 4 | `sendToServer` | SERVER | dead code (`TechgunsEventhandler`) |
| 11 | `PacketTGPlayerFieldSync` | 7 | `sendTo` one player or `sendToDimension` | CLIENT | `PacketTGKeybindPress` handler |
| 12 | `PacketRequestTurretSync` | 4 | `sendToServer` | SERVER | `NPCTurret` |
| 13 | `PacketSpawnParticle` | 50 + name length | `sendToAllAround` (r = 50, 150 or 200) | CLIENT | projectiles, bio blob, reaction chamber, jump pack, tesla beam |
| 14 | `PacketSpawnParticleOnEntity` | 6 + name length | `sendToAllAround` (r = 200 for trails) | CLIENT | guns, dispenser behavior, grenade projectile, jump pack |
| 15 | `PacketTGMessage` | 13 | `sendTo` one player | CLIENT | [`MultiBlockMachineBlock`](../08-machines-production/blocks-and-items.md#2-multiblockmachineblock-registry-name-machinemultiblock) (8 sites) |

### 0 · `GUIButtonClick`
| Field | Detail |
|---|---|
| Sender | Client GUI button handlers: [`GuiCamoBench`](../09-machines-utility/camo-bench.md#gui-guicamobench) (line 191), [`GuiExplosiveCharge`](../08-machines-production/gui-textures.md#10-texturesguiexpchargeguipng-owned-by-09) (95), [`GuiRepairBench`](../09-machines-utility/repair-bench.md#gui-guirepairbench) (165), [`GuiTechgunsChest`](../09-machines-utility/misc-tile-entities.md#gui-guitechgunschest) (110), [`GuiTechgunsPoweredTileEnt`](../08-machines-production/gui-handler.md#3-guitechgunspoweredtileent-base-gui-for-every-powered-machine) (118). Each sends the GUI's tile entity plus the vanilla `GuiButton` id |
| Direction | client to server |
| Wire fields | `x` int, `y` int, `z` int, `buttonId` int (4 + 4 + 4 + 4) |
| Validation | None. A missing tile entity or one that is not a `BasicInventoryTileEnt` is silently ignored. No range, ownership or open-container check |
| Handler | Netty thread, server. `world.getTileEntity(x, y, z)` in the sender's world, then `BasicInventoryTileEnt.buttonClicked(buttonId, player)` |
| Side effects | Whatever the machine's `buttonClicked` does (mode switches, recipe selects, owner toggles; see [08](../08-machines-production/index.md) and [09](../09-machines-utility/index.md)) |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-08](index.md#suspected-bugs) |

### 1 · `GunFiredMessage`
| Field | Detail |
|---|---|
| Sender | `GenericGun.shootGun` (line 847), server only (`!world.isRemote`). Recoil time is converted from ticks to milliseconds as `recoiltime / 20 * 1000`. Used for players and NPC shooters alike |
| Direction | server to all players within 100 blocks of the shooter |
| Wire fields | `entityID` int, `time` int (ms), `attackType` byte, `checkRecoil` bool, `akimboGun` bool, `doubleShot` bool (4 + 4 + 1 + 1 + 1 + 1) |
| Validation | Unknown entity id is ignored. The entity is cast to `EntityLivingBase` without an `instanceof` check ([CORE-40](index.md#suspected-bugs)) |
| Handler | Netty thread, client. Skip when `checkRecoil` is set and the local player is still recoiling; otherwise store recoil end and total in the `AttackTime` map, then spawn the muzzle flash once or twice (reference entry above) |
| Side effects | `ClientProxy.attackTimes` entry; muzzle-flash particle(s) ([client-effects-audio.md](client-effects-audio.md#muzzle-flash-placement-clientproxyspawnmuzzlefxforentityshooter-leftgun)) |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-40](index.md#suspected-bugs) |

### 2 · `RequestTileEntitySync`
| Field | Detail |
|---|---|
| Sender | `TurretBaseEnt` (line 249), client side only |
| Direction | client to server |
| Wire fields | `x` int, `y` short, `z` int (4 + 2 + 4) |
| Validation | None. Null tile entity ignored; a null description packet is passed to `sendPacket` unchecked |
| Handler | Netty thread, server. Casts the sender to `EntityPlayerMP`, then sends `tile.getDescriptionPacket()` straight back as a vanilla packet |
| Side effects | Client receives the tile entity's normal update packet |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-08](index.md#suspected-bugs) |

### 3 · `ReloadStartedMessage`
| Field | Detail |
|---|---|
| Sender | `GenericGun` line 724 (reload started with the gun's attack type) and line 820 (second reload path, attack type 0). Server only |
| Direction | server to all players within 100 blocks of the reloader |
| Wire fields | `entityID` int, `time` int (ms), `attackType` byte (4 + 4 + 1) |
| Validation | Unknown entity ignored; unchecked cast to `EntityLivingBase` ([CORE-40](index.md#suspected-bugs)) |
| Handler | Netty thread, client. Ignored when the entity is the local player, otherwise `setplayerReloadtime(entity, now + time, time, attackType)` |
| Side effects | `AttackTime` reload fields, read by third-person reload animations |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-40](index.md#suspected-bugs) |

### 4 · `PacketOpenPlayerGUI`
| Field | Detail |
|---|---|
| Sender | `TGGuiTabButton.mousePressed` (line 42) for ids above 0; `TGInventoryTab.onTabClicked` (line 18) with the literal 17 |
| Direction | client to server |
| Wire fields | `guiID` short (2) |
| Validation | Only `guiID > 0`. Any positive id is accepted |
| Handler | Netty thread, server. `player.openGui(Techguns.instance, guiID, world, floor x, floor y, floor z)` at the player's feet |
| Side effects | Opens the container (id 17 builds [`TGPlayerContainer`](../05-armor/extra-inventory.md#4-container-tgplayercontainer)). A commented-out branch once replied with `PacketClientOpenGui` for ids at or below 0 |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-06](index.md#suspected-bugs), [CORE-07](index.md#suspected-bugs) |

### 5 · `PacketTGExtendedPlayerSync`
| Field | Detail |
|---|---|
| Sender | `TechgunsEventhandler.onEntityJoinWorld` (line 487, full), `TechgunsTickHandler` auto-feeder (lines 409 and 443, full), `PacketTGKeybindPress` handler (after toggles 0, 1, 8, 9, full), `PacketRequestTGPlayerSync` handler (partial). Two other call sites ([`CamoBenchTileEnt`](../09-machines-utility/index.md#key-facts), [`TGPlayerInventory.closeInventory`](../05-armor/extra-inventory.md#1-data-model-tgplayerinventory-iinventory-12-slots)) are commented out |
| Direction | server to one player |
| Wire fields | `entityId` int, `radlevel` int, `size` byte, then `size` ItemStacks in the [`PacketUtil`](utilities.md#packetutil-itemstack-wire-format) format (variable). If `size > 2`: `foodLeft` short, `lastSaturation` float, `nightVisionEnabled` bool, `safemodeEnabled` bool, `hovermodeEnabled` bool, `stepAssist` bool, `showHUD` bool, `enableJetpack` bool (2 + 4 + 6 = 12 bytes) |
| Validation | None. `size` is trusted: a size above 12 would overrun the 12-slot array, a negative one throws on array creation. Item read errors are caught and printed, leaving that slot null |
| Handler | Netty thread, client. Looks the entity up in the client world; if it is a player, fetches or creates its extended properties, copies the fields and deep-copies each received stack into the slot of the same index |
| Side effects | Overwrites client copies of the toggles, the food buffer and the inventory. In partial mode the untransmitted flags are overwritten with defaults ([CORE-09](index.md#suspected-bugs)) |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-09](index.md#suspected-bugs) |

### 6 · `PacketEntityDeathType`
| Field | Detail |
|---|---|
| Sender | `TechgunsEventhandler.onLivingDeathEvent` (line 301), server |
| Direction | server to all players within 100 blocks of the victim |
| Wire fields | `entityId` int, `deathtypeId` int, `motionX` float, `motionY` float, `motionZ` float (4 + 4 + 12) |
| Validation | Unknown entity ignored (unchecked cast to `EntityLivingBase`, [CORE-40](index.md#suspected-bugs)). `deathtypeId` is used as an enum index without a range check |
| Handler | Netty thread, client. Gated by `EnableDeathEffects` and, for GORE, `EnableGoreDeathEffect`. Copies the motion, records the type, runs `DeathEffect.createDeathEffect`, plays the death sound |
| Side effects | Entity motion overwritten; death type stored in `ClientProxy`; particles and gibs; a sound with volume 1.0, pitch 1.0, not repeating, not moving, not at gun position, category DEATHEFFECT |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-40](index.md#suspected-bugs) |

### 7 · `PacketTGKeybindPress`
| Field | Detail |
|---|---|
| Sender | `TGKeybinds.onKeyInput` (ids 0, 1, 7, 8, 6), `TGGuiToggleButton.mousePressed` (ids 9, 0, 1, 8, 7), `ItemJetpack` client tick (ids 2 and 3). Ids 4 and 5 are only referenced in commented-out code |
| Direction | client to server |
| Wire fields | `buttonID` byte (1) |
| Validation | Unknown ids fall through every branch and do nothing. No check that the sender wears a jetpack for ids 2 and 3 ([CORE-41](index.md#suspected-bugs)). The extended properties are used without a null check |
| Handler | Netty thread, server. Per-id logic is in the reference entry above |
| Side effects | Flips flags on the extended properties and syncs them; plays the night-vision sounds; broadcasts the jetpack jump state; triggers a forced reload |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-15](index.md#suspected-bugs), [CORE-41](index.md#suspected-bugs) |

### 8 · `PacketShootGun`
| Field | Detail |
|---|---|
| Sender | `TechgunsTickHandler.onPlayerTick` (line 131) while the left mouse button is held with a left-click gun |
| Direction | client to server |
| Wire fields | `isZooming` bool (1) |
| Validation | Held item must be a `GenericGun`. The zoom flag is taken from the client as is. Any rate limit lives inside `shootGunLeftClick` (see [02](../02-guns/index.md)), not in the packet |
| Handler | Netty thread, server. `((GenericGun) item).shootGunLeftClick(stack, world, player, isZooming)` |
| Side effects | Fires the gun on the server (projectile, ammo use, recoil and sound packets) |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-32](index.md#suspected-bugs) |

### 9 · `PacketPlaySound`
| Field | Detail |
|---|---|
| Sender | `SoundUtil` helpers (lines 20, 35, 55, 68; radius 100); `TechgunsTickHandler` anti-grav start (line 340; radius 50); [`ItemJumppack`](../05-armor/special-equipment.md#44-jump-pack-itemjumppack) (line 101, `techguns:effects.steamarmorJump2`, radius 50); [`Chainsaw`](../02-guns/sheets-launchers-tools-grenades.md#chainsaw-chainsaw) (line 138, `techguns:guns.chainsawSwing`, radius 50); [`PowerHammer`](../02-guns/sheets-launchers-tools-grenades.md#power-hammer-powerhammer) (line 119, `techguns:guns.powerhammerSwing`, radius 50) |
| Direction | server to players inside the radius |
| Wire fields | `entityId` int, `volume` float, `pitch` float, `repeat` bool, `moving` bool, `gunPosition` bool, `playOnOwnPlayer` bool, `soundx` int, `soundy` short, `soundz` int, `category` byte, then the name as raw bytes to the end of the payload (27 fixed bytes) |
| Validation | None. The category byte is used as an enum index without a range check; the name is decoded with the platform default charset ([CORE-11](index.md#suspected-bugs)) |
| Handler | Netty thread, client. Entity id other than -1: `proxy.handleSoundEvent` (skips the local player's own entity unless `playOnOwnPlayer`). Id -1: `proxy.playSoundOnPosition` at the transmitted coordinates |
| Side effects | A [`TGSound`](client-effects-audio.md#tgsound) (moving or fixed) is queued on the client sound handler |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-11](index.md#suspected-bugs), [CORE-40](index.md#suspected-bugs) |

### 10 · `PacketRequestTGPlayerSync`
| Field | Detail |
|---|---|
| Sender | `TechgunsEventhandler.onStartTracking` (line 600). Never runs ([CORE-10](index.md#suspected-bugs)) |
| Direction | client to server |
| Wire fields | `entityID` int (4) |
| Validation | Entity must exist, be a player and have extended properties. No range limit, so any known entity id yields that player's face and back items |
| Handler | Netty thread, server. Replies with a partial `PacketTGExtendedPlayerSync` |
| Side effects | None beyond the reply |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-10](index.md#suspected-bugs) |

### 11 · `PacketTGPlayerFieldSync`
| Field | Detail |
|---|---|
| Sender | `PacketTGKeybindPress` handler: field 1 to the toggling player after `TOGGLE_JETPACK`; field 0 to the whole dimension after ids 2 and 3 (and after id 7 when a jump flag is cleared) |
| Direction | server to one player or to every player in the sender's dimension |
| Wire fields | `entityID` int, `id` short, then a bool for ids 0 and 1 (4 + 2 + 1). Other ids carry no value |
| Validation | Unknown field ids are ignored. The handler uses `Minecraft.theWorld` directly |
| Handler | Netty thread, client. Field 0 on a player other than the local one calls `setJumpkeyPressed`, which starts or stops the jetpack loop. Field 1 sets `enableJetpack` on any player |
| Side effects | Jetpack loop and start/stop sounds for observers ([player-state.md](player-state.md#client-subclass-techgunsextendedplayerpropertiesclient)) |
| Bugs | [CORE-05](index.md#suspected-bugs) |

### 12 · `PacketRequestTurretSync`
| Field | Detail |
|---|---|
| Sender | `NPCTurret` (line 328), client side, when the turret needs its base |
| Direction | client to server |
| Wire fields | `turret_entityId` int (4) |
| Validation | Entity must be an `NPCTurret` with a mounted base tile entity. No range check |
| Handler | Netty thread, server. Sends the base's `getDescriptionPacket()` to the requester |
| Side effects | Client turret relinks to the base |
| Bugs | [CORE-05](index.md#suspected-bugs) |

### 13 · `PacketSpawnParticle`
| Field | Detail |
|---|---|
| Sender | Server-side projectile classes ([`AlienBlasterProjectile`](../04-projectiles-fx/projectiles.md#1-registration) x2, [`EnergyBlastProjectile`](../04-projectiles-fx/projectiles.md#310-energyblastprojectile-mib-gun), [`GrenadeProjectile`](../04-projectiles-fx/projectiles.md#37-grenadeprojectile-family), [`NukeRocketProjectile`](../04-projectiles-fx/projectiles.md#34-rocketprojectile-and-nukerocketprojectile) x2, [`RocketProjectile`](../04-projectiles-fx/projectiles.md#34-rocketprojectile-and-nukerocketprojectile), [`SonicShotgunProjectile`](../04-projectiles-fx/projectiles.md#313-sonicshotgunprojectile), [`TeslaBeam`](../04-projectiles-fx/projectiles.md#1-registration) x2), [`BioblobTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-bioblobtileent) (line 110), [`ReactionChamberTileEntMaster`](../08-machines-production/framework.md#1-class-hierarchy) (line 590), `ItemJumppack` (line 107). Radius 50, 150 or 200 depending on the sender |
| Direction | server to players inside the radius |
| Wire fields | name length short, name bytes, `posX/Y/Z` double, `motionX/Y/Z` double (2 + n + 48). Motion defaults to 0 |
| Validation | None. Unknown FX names spawn nothing. Heap-buffer assumption in the decoder ([CORE-11](index.md#suspected-bugs)) |
| Handler | Netty thread, client. `ClientProxy.createFX(name, world, pos, motion)` |
| Side effects | FXList particle systems in the client world |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-11](index.md#suspected-bugs) |

### 14 · `PacketSpawnParticleOnEntity`
| Field | Detail |
|---|---|
| Sender | Gun classes [`AlienBlaster`](../02-guns/sheets-energy-special.md#alien-blaster-alienblaster), [`Biogun`](../02-guns/sheets-energy-special.md#bio-gun-biogun), [`CyberdemonBlaster`](../02-guns/sheets-energy-special.md#nether-blaster-cyberdemonblaster), [`GrenadeLauncher`](../02-guns/index.md#coverage), [`MibGun`](../02-guns/sheets-energy-special.md#de-atomizer-pistol-mibgun), [`SonicShotgun`](../02-guns/index.md#coverage); [`GrenadeLauncherProjectile`](../04-projectiles-fx/projectiles.md#1-registration); [`BehaviorProjectileDispenseDamagevalue`](lifecycle-and-registration.md#preinit-in-exact-order) (line 99); `ItemJumppack` (line 100). Radius 200 for the trails |
| Direction | server to players inside the radius |
| Wire fields | name length short, name bytes, `entityID` int (2 + n + 4) |
| Validation | A missing entity logs "Got Packet for FX ... but ent was null" and stops. Same decoder fragility as packet 13 |
| Handler | Netty thread, client. `ClientProxy.createFXOnEntity(name, entity)` |
| Side effects | Trail or boost FX attached to the entity |
| Bugs | [CORE-05](index.md#suspected-bugs), [CORE-11](index.md#suspected-bugs) |

### 15 · `PacketTGMessage`
| Field | Detail |
|---|---|
| Sender | `MultiBlockMachineBlock` (lines 393, 402, 421, 430, 551, 560, 580, 589), always with type 0 |
| Direction | server to the one player who placed or checked the multiblock |
| Wire fields | `messageType` byte, `x` int, `y` int, `z` int (1 + 12) |
| Validation | None; the type is concatenated into the lang key, so an unknown type shows the raw key |
| Handler | Netty thread, client. Appends a chat line: translated `techguns.tgmessage.<type>`, a space, then `x, y, z` |
| Side effects | Chat line only |
| Bugs | [CORE-05](index.md#suspected-bugs) |

### Unregistered classes
| Class | Wire fields | Status |
|---|---|---|
| `PacketClientOpenGui` | `id` byte (1) | Registration commented out; the only sender is also commented out in `PacketOpenPlayerGUI`. Handler: id 0 opens the vanilla inventory |
| `PacketGrenadeBounce` | `entityId` int, `motionX/Y/Z` double (4 + 24) | Never registered or sent. Handler would overwrite the entity's motion on the client |
