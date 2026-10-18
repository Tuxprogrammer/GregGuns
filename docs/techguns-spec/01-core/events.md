# Event handlers

There are three handler objects:
- `TechgunsEventhandler` on the **Forge** event bus, both sides.
- `TechgunsTickHandler` on the **FML** bus, both sides.
- `GuiOverlayAmmoCount` on the Forge bus, client only.

The debug `KeyInputHandler` and [`TGKeybinds`](input-and-hud.md#keybinds-tgkeybinds-client-only) are on the FML bus, client only. Handlers marked "client" are `@SideOnly(CLIENT)` and are stripped on a dedicated server.

## TechgunsEventhandler (Forge bus)

| Event | Priority | Side | Behaviour |
|---|---|---|---|
| `FOVUpdateEvent` | normal | client | Applies zoom and the speed-FOV lock ([input-and-hud.md](input-and-hud.md#fov-and-zoom)) |
| `LivingJumpEvent` | normal | both | Players: `motionY += JUMP bonus` from armor, consuming power ([05](../05-armor/index.md)) |
| `LivingFallEvent` | normal | both | Players. Power is consumed only if the fall distance is over 3. `distance = max(0, distance − FREEHEIGHT)`, then multiplied by (1 − FALLDMG) if FALLDMG < 1, otherwise set to 0 |
| `BreakSpeed` (`stopGunHarvesting`) | **highest** | both | Holding a left-click [`GenericGun`](../02-guns/gun-lifecycle.md) → `newSpeed = −1` and the event is cancelled (no mining with guns) |
| `BreakSpeed` | normal | both | `newSpeed ×= (1 + BREAKSPEED bonus)`. If the player is inside water or lava, it is also multiplied by (1 + BREAKSPEED_WATER). Both bonuses consume power |
| `ChunkEvent.Unload` | normal | client worlds | Every `EntityLivingBase` in the unloading chunk is removed from the ClientProxy recoil/reload map |
| `PlayerEvent.Clone` | normal | server | Copies all Techguns player data from the old to the new player entity ([player-state.md](player-state.md#drops-death-clone)) |
| `RenderLivingEvent.Pre` | normal | client | 1. If the entity has a pending death type ([client-effects-audio.md](client-effects-audio.md#death-effects)): GORE → cancel the render (the body is replaced by gibs); DISMEMBER, BIO or LASER → cancel and draw the entity with [`RendererDeadLivingEntity`](../04-projectiles-fx/death-effects.md#6-dismember-unused) instead ([04](../04-projectiles-fx/index.md)); DEFAULT → normal render. The pending type is cleared once `deathTime == 0`. 2. If the entity is an [`INPCTechgunsShooter`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) drawn by a `RenderBiped` and holds a `GenericGun`: set the model's aimed-bow pose (GenericNPCs use their own aim flag, other shooters always aim) |
| `PlayerDropsEvent` | normal | server | Drops the 12 Techguns slots unless `keepInventory` is set |
| `LivingDeathEvent` | normal | both | **Client:** remove [`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) and [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) entities from the recoil/reload maps. **Server:** for players, reset `foodleft` and `lastSaturation`. When the damage source is a [`TGDamageSource`](../02-guns/damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource) whose death type is not DEFAULT and [`EntityDeathUtils.hasSpecialDeathAnim`](../04-projectiles-fx/index.md#coverage) is true, send [`PacketEntityDeathType`](networking.md#6--packetentitydeathtype-sc) to all within 100 blocks |
| `EntityConstructing` | normal | both | Players: register [`TechgunsExtendedPlayerProperties`](player-state.md#techgunsextendedplayerproperties) if missing |
| `GuiScreenEvent.InitGuiEvent.Post` | normal | client | Adds the two inventory tab buttons to `GuiInventory` when TConstruct is absent ([input-and-hud.md](input-and-hud.md#inventory-tab-buttons)) |
| `RenderPlayerEvent.Specials.Pre` (`renderAkimboSecondGun`) | normal | client | If the held gun is akimbo, draws the off-hand copy through `RenderAdditionalSlotItem.getRenderForItem(stack)` ([03](../03-gun-visuals-audio/index.md)) |
| `RenderPlayerEvent.Pre` | normal | client | Holding a `GenericGun` sets the aimed-bow pose on the player's main, armor and chestplate models (two-handed aim) |
| `RenderPlayerEvent.SetArmorModel` | normal | client | Pass 0 (helmet): read the face item from the DataWatcher and draw it with its `RenderAdditionalSlotItem` renderer ([05](../05-armor/index.md)). It is drawn "visible" unless the worn helmet is a [`GenericArmor`](../05-armor/armor-catalogue.md#1-class-hierarchy) that hides the face slot. Pass 1 (chest): the same for the back item, hidden when the chestplate hides the back slot |
| `EntityJoinWorldEvent` | normal | server | Player: send a full extended sync to that player. [`TGDummySpawn`](../10-entities-npcs/per-mob.md#18-tgdummyspawn-tgdummyspawn): hand it to [`TGSpawnManager.handleSpawn`](../10-entities-npcs/spawning.md#11-vanilla-hook) ([10](../10-entities-npcs/index.md)) and **cancel** the join, so the placeholder never exists |
| `MouseEvent` | **high** | client | Left-click handling for guns ([input-and-hud.md](input-and-hud.md#firing-guns-with-the-left-mouse-button)) |
| `LivingAttackEvent` | **high** | both | See [below](#livingattackevent-routing) |
| `PlayerEvent.StartTracking` | normal | (server event) | Meant to send [`PacketRequestTGPlayerSync`](networking.md#10--packetrequesttgplayersync-cs) from the client. It checks `isRemote`, but the event only fires on the server, so this is dead code ([CORE-10](index.md#suspected-bugs)) |
| `PlayerEvent.StopTracking` | normal | (server event) | Meant to clear the client recoil maps and reset jetpack and glider state for an untracked player. Dead for the same reason |
| `TextureStitchEvent.Pre` | normal | client | Block atlas (texture type 0), only when [`TGFluids.addedMilk`](../06-items/fluids.md) (Techguns added its own milk fluid, 07): registers the icons `techguns:milk_still` and `techguns:milk_flow` and assigns them to `TGFluids.MILK` |

A large amount of commented-out experimentation (fog color, block place/break tests, explosion tests, item-use speed) has no runtime effect.

### LivingAttackEvent routing

1. Find the attacker (`source.getSourceOfDamage()`). If it is a living entity whose main-hand item implements [`IToolTGDamageSystem`](../02-guns/damage-system.md#3-interfaces), replace the damage source with the tool's own TG damage source. This is how melee guns such as the chainsaw and power hammer deal TG damage ([02](../02-guns/index.md)).
2. If the victim is a player and the (possibly replaced) source is vanilla lava, inFire or onFire, and the armor's COOLING_SYSTEM bonus is ≥ 1 (power is consumed): **cancel** the event. Full fire and lava immunity.
3. Otherwise, if the source is a `TGDamageSource` **or** the victim implements [`INpcTGDamageSystem`](../02-guns/damage-system.md#3-interfaces): **cancel** the vanilla attack and hand it to [`DamageSystem.onLivingAttackPlayer`](../02-guns/index.md#coverage) (player victims) or `DamageSystem.onLivingAttackEntityLivingBase` (others) with the original amount. The damage pipeline is described in [02-guns](../02-guns/index.md) and [05-armor](../05-armor/index.md).

## TechgunsTickHandler (FML bus)

| Event | Side | Behaviour |
|---|---|---|
| `PlayerTickEvent` (`onPlayerTick`) | both | All per-player logic ([player-state.md](player-state.md#per-tick-player-logic-techgunstickhandleronplayertick-fml-playertickevent)) |
| `PlayerTickEvent` (`updateCheckMessage`, receives cancelled events) | client | Posts the update-available chat message once ([client-effects-audio.md](client-effects-audio.md#version-checker)) |
| `RenderTickEvent` | client | Updates `player_zoom` |
| `ItemCraftedEvent` | both | Akimbo split: gives back the second gun ([player-state.md](player-state.md#other-handlers-in-the-tick-handler-class)) |
| `PlayerLoggedOutEvent` | marked client | Removes the player from the recoil maps if `isRemote`. FML fires this on the server, so it does nothing |

## GuiOverlayAmmoCount (Forge bus, client)

| Event | Behaviour |
|---|---|
| `RenderGameOverlayEvent` (post, EXPERIENCE) | Ammo counter and status icon column |
| `RenderGameOverlayEvent.Pre` (ARMOR) | Replaces the armor bar (up to 24 points) |

Full layout in [input-and-hud.md](input-and-hud.md#hud-overlay-guioverlayammocount).

## Key input (FML bus, client)

`TGKeybinds.onKeyInput` and the debug `KeyInputHandler.onKeyInput`, both on `InputEvent.KeyInputEvent`. See [input-and-hud.md](input-and-hud.md#keybinds-tgkeybinds-client-only).

## Event handler sheets (consistency pass)

For every subscription: the bus it sits on, the registration point, the method, its priority, whether `receiveCanceled` is set (that is, whether it still runs after another handler cancelled the event), whether the handler itself cancels the event, and the behaviour pointer. Where the source writes a bare `@SubscribeEvent`, the defaults are priority NORMAL and `receiveCanceled = false`.

### Registration points

| Handler object | Bus | Registered in | Sides |
|---|---|---|---|
| `TechgunsEventhandler` | Forge (`MinecraftForge.EVENT_BUS`) | [`Techguns.java`](lifecycle-and-registration.md#lifecycle-overview) line 685, init | both |
| `TechgunsTickHandler` | FML (`FMLCommonHandler.bus()`) | `Techguns.java` line 687, init | both |
| `GuiOverlayAmmoCount` | Forge | [`ClientProxy`](lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) line 1290 | client only |
| `debug.KeyInputHandler` | FML | `ClientProxy` line 1293, only if [`enableDebugKeybinds`](config.md#category-debug) | client only |
| `TGKeybinds` | FML | `ClientProxy` line 1297 | client only |

### `TechgunsEventhandler` (Forge bus)

| Event type | Method | Priority | `receiveCanceled` | Cancels? | Side annotation | Behaviour |
|---|---|---|---|---|---|---|
| `FOVUpdateEvent` | `handleFovEvent` | NORMAL (bare) | false | no, rewrites `newfov` | `@SideOnly(CLIENT)` | [FOV and zoom](input-and-hud.md#fov-and-zoom) |
| `LivingJumpEvent` | `onEvent` | NORMAL | false | no | none (both sides) | Jump bonus from armour |
| `LivingFallEvent` | `onEvent` | NORMAL | false | no, rewrites `distance` | none | Fall-damage bonus |
| `PlayerEvent.BreakSpeed` | `stopGunHarvesting` | HIGHEST | false | yes, when holding a left-click gun | none | No mining with guns |
| `PlayerEvent.BreakSpeed` | `onEvent` | NORMAL | false | no, rewrites `newSpeed` | none | Armour break-speed bonuses |
| `ChunkEvent.Unload` | `onEvent` | NORMAL | false | no | none; acts only when `world.isRemote` | Clears recoil/reload map entries |
| `PlayerEvent.Clone` | `onPlayerClone` | NORMAL (bare) | false | no | none | Copies Techguns player data |
| `RenderLivingEvent.Pre` | `handleEntityRenderEvent` | NORMAL (bare) | false | yes, for GORE / BIO / LASER / DISMEMBER corpses | `@SideOnly(CLIENT)` | Hides or replaces the corpse; aimed-bow pose for shooters |
| `PlayerDropsEvent` | `onPlayerDrops` | NORMAL | false | no | none | Drops the 12 slots |
| `LivingDeathEvent` | `onLivingDeathEvent` | NORMAL | false | no | none; early branch for `isRemote` | Death-type packet, map cleanup, food reset |
| `EntityEvent.EntityConstructing` | `onEntityConstructing` | NORMAL (bare) | false | no | none | Registers extended properties |
| `GuiScreenEvent.InitGuiEvent.Post` | `onGuiPostInit` | NORMAL | false | no (not cancelable) | `@SideOnly(CLIENT)` | Tab buttons |
| `RenderPlayerEvent.Specials.Pre` | `renderAkimboSecondGun` | NORMAL | false | no | `@SideOnly(CLIENT)` | Off-hand akimbo gun |
| `RenderPlayerEvent.Pre` | `onRenderPlayerEventPre` | NORMAL | false | no | `@SideOnly(CLIENT)` | Two-handed aim pose |
| `RenderPlayerEvent.SetArmorModel` | `onRenderArmorEvent` | NORMAL | false | no | `@SideOnly(CLIENT)` | Face and back item rendering |
| `EntityJoinWorldEvent` | `onEntityJoinWorld` | NORMAL | false | yes, for `TGDummySpawn` only | none; acts only when `!isRemote` | Join sync; spawn replacement |
| `MouseEvent` | `onMouseEvent` | HIGH | false (default) | yes, for gun clicks and reload-time melee clicks | `@SideOnly(CLIENT)` | [Left-click firing](input-and-hud.md#firing-guns-with-the-left-mouse-button) |
| `LivingAttackEvent` | `OnLivingAttack` | HIGH | false | yes, for fire immunity and every TG-damage hit | none | [Routing](#livingattackevent-routing) |
| `PlayerEvent.StartTracking` | `onStartTracking` | NORMAL | false (default) | no | none; body guarded by `isRemote` | Dead code ([CORE-10](index.md#suspected-bugs)) |
| `PlayerEvent.StopTracking` | `onStopTracking` | NORMAL | false (default) | no | none; body guarded by `isRemote` | Dead code ([CORE-10](index.md#suspected-bugs)) |
| `TextureStitchEvent.Pre` | `onTextureStitch` | NORMAL | false (default) | no | `@SideOnly(CLIENT)` | Milk icons |

All other `TechgunsEventhandler` subscriptions in the source (interact, block place and break, explosion start and detonate, fog density and colour, living update, GUI pre-draw, lava burn) are commented out and have no runtime effect.

### `TechgunsTickHandler` (FML bus)

| Event type | Method | Priority | `receiveCanceled` | Cancels? | Side annotation | Behaviour |
|---|---|---|---|---|---|---|
| `TickEvent.PlayerTickEvent` | `onPlayerTick` | NORMAL (bare) | false | not cancelable | none; internal `event.side` and `isRemote` branches | [Per-tick player logic](player-state.md#per-tick-player-logic-techgunstickhandleronplayertick-fml-playertickevent) |
| `TickEvent.PlayerTickEvent` | `updateCheckMessage` | NORMAL | **true** | not cancelable | `@SideOnly(CLIENT)` | Update chat message ([CORE-02](index.md#suspected-bugs)) |
| `TickEvent.RenderTickEvent` | `onRenderTick` | NORMAL (bare) | false | not cancelable | `@SideOnly(CLIENT)` | Writes `player_zoom` |
| `PlayerEvent.ItemCraftedEvent` | `onCraftingEvent` | NORMAL | false | not cancelable | none | Akimbo split ([CORE-36](index.md#suspected-bugs)) |
| `PlayerEvent.PlayerLoggedOutEvent` | `onEvent` | NORMAL | false | not cancelable | `@SideOnly(CLIENT)` plus an `isRemote` guard | Dead code ([CORE-37](index.md#suspected-bugs)) |

### `GuiOverlayAmmoCount` (Forge bus, client)

| Event type | Method | Priority | `receiveCanceled` | Cancels? | Behaviour |
|---|---|---|---|---|---|
| `RenderGameOverlayEvent` (base type; returns early unless the event is not cancelable and its element is `EXPERIENCE`) | `onEvent` | NORMAL | false | no | Ammo counter, safe-mode icon, status column |
| `RenderGameOverlayEvent.Pre` (only element `ARMOR`) | `onEventArmorBar` | NORMAL | false | yes, when `showTGHudElements` is on and the properties exist | 24-point armour bar |

### Key handlers (FML bus, client)

| Event type | Class and method | Priority | `receiveCanceled` | Cancels? | Behaviour |
|---|---|---|---|---|---|
| `InputEvent.KeyInputEvent` | `TGKeybinds.onKeyInput` | NORMAL (bare) | false | not cancelable | One packet per press, see [keybind sheets](input-and-hud.md#keybind-entity-sheets-consistency-pass) |
| `InputEvent.KeyInputEvent` | `debug.KeyInputHandler.onKeyInput` | NORMAL (bare) | false | not cancelable | Debug offsets and FX reload |

### Non-event hooks owned by core

These are not bus subscribers but run alongside them. `Techguns.java` registers three `IWorldGenerator`s through `GameRegistry` ([`OreGenerator`](../11-worldgen/ore-generation.md#1-ore-veins-oregenerator) at line 645, [`WorldGenOilHoles`](../11-worldgen/ore-generation.md#3-oil-holes-worldgenoilholes) at line 752 with weight 3, [`WorldGenTGStructureSpawn`](../11-worldgen/index.md#overview) at line 748 with weight 4), documented in [11-worldgen](../11-worldgen/index.md). There is no `IFuelHandler` registration anywhere in the source. Core registers no live `LivingHurtEvent`, `LivingUpdateEvent`, `PlayerInteractEvent` or `ServerTickEvent` handler.
