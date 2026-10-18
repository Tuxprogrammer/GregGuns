# Event handlers

There are three handler objects:
- `TechgunsEventhandler` on the **Forge** event bus, both sides.
- `TechgunsTickHandler` on the **FML** bus, both sides.
- `GuiOverlayAmmoCount` on the Forge bus, client only.

The debug `KeyInputHandler` and `TGKeybinds` are on the FML bus, client only. Handlers marked "client" are `@SideOnly(CLIENT)` and are stripped on a dedicated server.

## TechgunsEventhandler (Forge bus)

| Event | Priority | Side | Behaviour |
|---|---|---|---|
| `FOVUpdateEvent` | normal | client | Applies zoom and the speed-FOV lock ([input-and-hud.md](input-and-hud.md#fov-and-zoom)) |
| `LivingJumpEvent` | normal | both | Players: `motionY += JUMP bonus` from armor, consuming power (05) |
| `LivingFallEvent` | normal | both | Players. Power is consumed only if the fall distance is over 3. `distance = max(0, distance − FREEHEIGHT)`, then multiplied by (1 − FALLDMG) if FALLDMG < 1, otherwise set to 0 |
| `BreakSpeed` (`stopGunHarvesting`) | **highest** | both | Holding a left-click `GenericGun` → `newSpeed = −1` and the event is cancelled (no mining with guns) |
| `BreakSpeed` | normal | both | `newSpeed ×= (1 + BREAKSPEED bonus)`. If the player is inside water or lava, it is also multiplied by (1 + BREAKSPEED_WATER). Both bonuses consume power |
| `ChunkEvent.Unload` | normal | client worlds | Every `EntityLivingBase` in the unloading chunk is removed from the ClientProxy recoil/reload map |
| `PlayerEvent.Clone` | normal | server | Copies all Techguns player data from the old to the new player entity ([player-state.md](player-state.md#drops-death-clone)) |
| `RenderLivingEvent.Pre` | normal | client | 1. If the entity has a pending death type ([client-effects-audio.md](client-effects-audio.md#death-effects)): GORE → cancel the render (the body is replaced by gibs); DISMEMBER, BIO or LASER → cancel and draw the entity with `RendererDeadLivingEntity` instead (04); DEFAULT → normal render. The pending type is cleared once `deathTime == 0`. 2. If the entity is an `INPCTechgunsShooter` drawn by a `RenderBiped` and holds a `GenericGun`: set the model's aimed-bow pose (GenericNPCs use their own aim flag, other shooters always aim) |
| `PlayerDropsEvent` | normal | server | Drops the 12 Techguns slots unless `keepInventory` is set |
| `LivingDeathEvent` | normal | both | **Client:** remove `GenericNPC` and `NPCTurret` entities from the recoil/reload maps. **Server:** for players, reset `foodleft` and `lastSaturation`. When the damage source is a `TGDamageSource` whose death type is not DEFAULT and `EntityDeathUtils.hasSpecialDeathAnim` is true, send `PacketEntityDeathType` to all within 100 blocks |
| `EntityConstructing` | normal | both | Players: register `TechgunsExtendedPlayerProperties` if missing |
| `GuiScreenEvent.InitGuiEvent.Post` | normal | client | Adds the two inventory tab buttons to `GuiInventory` when TConstruct is absent ([input-and-hud.md](input-and-hud.md#inventory-tab-buttons)) |
| `RenderPlayerEvent.Specials.Pre` (`renderAkimboSecondGun`) | normal | client | If the held gun is akimbo, draws the off-hand copy through `RenderAdditionalSlotItem.getRenderForItem(stack)` (03) |
| `RenderPlayerEvent.Pre` | normal | client | Holding a `GenericGun` sets the aimed-bow pose on the player's main, armor and chestplate models (two-handed aim) |
| `RenderPlayerEvent.SetArmorModel` | normal | client | Pass 0 (helmet): read the face item from the DataWatcher and draw it with its `RenderAdditionalSlotItem` renderer (05). It is drawn "visible" unless the worn helmet is a `GenericArmor` that hides the face slot. Pass 1 (chest): the same for the back item, hidden when the chestplate hides the back slot |
| `EntityJoinWorldEvent` | normal | server | Player: send a full extended sync to that player. `TGDummySpawn`: hand it to `TGSpawnManager.handleSpawn` (10) and **cancel** the join, so the placeholder never exists |
| `MouseEvent` | **high** | client | Left-click handling for guns ([input-and-hud.md](input-and-hud.md#firing-guns-with-the-left-mouse-button)) |
| `LivingAttackEvent` | **high** | both | See [below](#livingattackevent-routing) |
| `PlayerEvent.StartTracking` | normal | (server event) | Meant to send `PacketRequestTGPlayerSync` from the client. It checks `isRemote`, but the event only fires on the server, so this is dead code (CORE-10) |
| `PlayerEvent.StopTracking` | normal | (server event) | Meant to clear the client recoil maps and reset jetpack and glider state for an untracked player. Dead for the same reason |
| `TextureStitchEvent.Pre` | normal | client | Block atlas (texture type 0), only when `TGFluids.addedMilk` (Techguns added its own milk fluid, 07): registers the icons `techguns:milk_still` and `techguns:milk_flow` and assigns them to `TGFluids.MILK` |

A large amount of commented-out experimentation (fog color, block place/break tests, explosion tests, item-use speed) has no runtime effect.

### LivingAttackEvent routing

1. Find the attacker (`source.getSourceOfDamage()`). If it is a living entity whose main-hand item implements `IToolTGDamageSystem`, replace the damage source with the tool's own TG damage source. This is how melee guns such as the chainsaw and power hammer deal TG damage (02).
2. If the victim is a player and the (possibly replaced) source is vanilla lava, inFire or onFire, and the armor's COOLING_SYSTEM bonus is ≥ 1 (power is consumed): **cancel** the event. Full fire and lava immunity.
3. Otherwise, if the source is a `TGDamageSource` **or** the victim implements `INpcTGDamageSystem`: **cancel** the vanilla attack and hand it to `DamageSystem.onLivingAttackPlayer` (player victims) or `DamageSystem.onLivingAttackEntityLivingBase` (others) with the original amount. The damage pipeline is described in 02-guns and 05-armor.

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
