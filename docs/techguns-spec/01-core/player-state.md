# Player state (extended properties) and per-tick player logic

Source: `extendedproperties/*`, `events/TechgunsTickHandler.java`, and parts of `events/TechgunsEventhandler.java`. The slot layout comes from `gui/playerinventory/TGPlayerInventory` (whose GUI belongs to the player inventory GUI).

## TechgunsExtendedPlayerProperties

Registered on every `EntityPlayer` during `EntityConstructing` under the name **`TechgunsExtendedPlayer`**, using `proxy.registerExtendedProperties`. Any proxy that is a ClientProxy registers the client subclass, including for server-side players in single-player. If the client receives a sync packet for a player that has no properties yet, the packet handler registers them on demand.

### Fields

| Field | Type / default | Saved to NBT | Synced | Meaning |
|---|---|---|---|---|
| `TG_inventory` | `TGPlayerInventory`, 12 slots | yes | full sync (all 12) / partial (2); DataWatcher (face, back) | extra slots (below) |
| `radlevel` | int, 0 | yes `radlevel` | yes | radiation contamination level (used by armor and radiation features, 05) |
| `foodleft` | int, 0 | yes `foodLeft` (written as short, read as int) | yes (short) | auto-feeder leftover hunger points |
| `lastSaturation` | float, 1.0 | yes `lastSaturation` | yes | saturation modifier of the leftover food |
| `enableNightVision` | bool, false | yes `enableNightvision` | yes | NV toggle |
| `enableSafemode` | bool, **true** | yes `enableSafemode` | yes | safe-mode toggle; forced true at load if `allowUnsafemode` fails |
| `enableHovermode` | bool, false | yes `enableHovermode` | yes | jetpack hover mode (05) |
| `enableJetpack` | bool, **true** | yes `enableJetpack` | yes + field sync 1 | jetpack master toggle |
| `showTGHudElements` | bool, **true** | yes `showHud` | yes | HUD icon toggle |
| `enableStepAssist` | bool, **true** | yes `stepAssist` | yes | step-assist toggle |
| `gotCreativeFlightLastTick` | bool | no | no | anti-grav flight bookkeeping |
| `wasFlying` | bool | no | no | detects the moment flight starts (for the FX and sound) |
| `isJumpkeyPressed` (protected) | bool | no | field sync 0 to others | jetpack boosting |
| `isForwardKeyPressed` | bool | no | no | jetpack forward; never set in practice |
| `fireDelay` | int ticks | no | no | gun cooldown; counted down on both sides |
| `loopSoundDelay` | int ticks | no | no | counted down each tick; used by guns with looping sounds (02) |
| `isGliding` | bool | no | no | set by the glider's slot tick each tick (05) |
| `swingSoundDelay` | byte | no | no | server-only cooldown for melee swing sounds (02) |

**NBT layout:** `saveNBTData` and `loadNBTData` receive the **player's root compound**, which is how 1.7.10 extended properties work. So all keys sit directly in the player NBT root with no sub-compound: `TechgunsPlayerInventory`, `radlevel`, `foodLeft`, `lastSaturation`, `enableNightvision`, `enableSafemode`, `enableHovermode`, `enableJetpack`, `showHud`, `stepAssist`. For a save that lacks these keys, every boolean loads as **false**, including jetpack, HUD and step assist, whose in-code defaults are true.

**Inventory NBT:** a tag list `TechgunsPlayerInventory` of compounds, each with a byte `Slot` (0–11) plus the standard ItemStack keys. Loading replaces the whole array.

### Slot layout (`TGPlayerInventory`, name `TechgunsPlayerInventory`, stack limit 64)

| Index | Purpose |
|---|---|
| 0 | Face slot (masks, goggles, …; `ITGSpecialSlot` items, 05) |
| 1 | Back slot (jetpack, glider, packs, …) |
| 2–4 | Auto-food slots |
| 5 | Auto-heal slot (used by 05/06 items) |
| 6–11 | Ammo slots. Guns draw ammo from these first, then from the main inventory |

Writing slot 0 or 1 through `setInventorySlotContents` immediately updates the matching DataWatcher entry.

### DataWatcher mirrors

The constructor registers two ItemStack (type 5) watchers on the player, with IDs from config (default 23 = face, 24 = back). At the **end** of every server player tick, both are set to the current slot contents. Clients render other players' face and back gear from these (`RenderPlayerEvent.SetArmorModel`, see [events.md](events.md)).

### Client subclass (`TechgunsExtendedPlayerPropertiesClient`)

Adds two sound handles, `gliderLoop` and `jetPackLoop`. Its `setJumpkeyPressed(v)` only does extra work in client worlds:
- Turning on with no loop running: create a moving, repeating `techguns:jetpack.loop` on the player (category PLAYER_EFFECT) and play `techguns:jetpack.start` at the player's position.
- Edge false → true: start playing the loop.
- Edge true → false: stop the loop, clear it, and play `techguns:jetpack.stop`.

The jetpack item calls this for the local player, and `PacketTGPlayerFieldSync` calls it for other players.

### Drops, death, clone

- **PlayerDropsEvent:** if the `keepInventory` gamerule is off, all 12 Techguns slots are dropped through the player's normal drop path (captured into the event's drop list) and cleared.
- **LivingDeathEvent (server, player):** `foodleft` and `lastSaturation` are reset to 0.
- **PlayerEvent.Clone** (respawn or end-portal return): the old player's properties are saved to a scratch compound and loaded into the new player, copying everything, including the inventory when it wasn't dropped.
- **EntityJoinWorldEvent (server, player):** a full sync is sent to that player.

## Per-tick player logic (`TechgunsTickHandler.onPlayerTick`, FML `PlayerTickEvent`)

The handler runs for **every** player on **both** sides (client-world players include other players' entities).

### Phase START

1. Decrement `fireDelay` and `loopSoundDelay` if they are above 0.
2. Client, local player only: the left-click fire loop ([input-and-hud.md](input-and-hud.md#firing-guns-with-the-left-mouse-button)).
3. Server: decrement `swingSoundDelay`.

### Phase END

1. **Server:** refresh the DataWatcher face and back mirrors.
2. Check whether any worn armor piece is Techguns armor (`GenericArmor.isTechgunArmor`).
3. Remove the three Techguns attribute modifiers if present:

   | UUID | Attribute |
   |---|---|
   | `5D8E53EB-DCFA-4121-B4DB-99BCAFA6B70B` | movement speed, "TechgunsSpeedboost" |
   | `4CFA49EB-D215-498B-9CC9-4BD0D1350B1F` | max health, "TechgunsHealthbonus" |
   | `3441FC5D-F0B6-47F4-AFBB-DC5005670254` | knockback resistance, "TechgunsKnockbackresistbonus" |

4. **If wearing Techguns armor:**
   - `PoweredArmor.calculateConsumptionTick(player)` (power drain, 05).
   - If burning and the armor's COOLING_SYSTEM bonus is ≥ 1: extinguish.
   - **Speed:** speed = SPEED bonus. Add SPEED_WATER if inside water or lava. If speed > 0:
     - double it while sprinting;
     - add **5.0** if the player is using an item (right-click held) while holding a `GenericGun`, which counteracts the use-item slowdown while aiming;
     - apply as an operation-2 modifier, so the final speed is multiplied by (1 + speed).
   - **Health:** EXTRA_HEART bonus > 0 → an operation-0 (additive) max-health modifier.
   - **Knockback resistance:** KNOCKBACK_RESISTANCE bonus > 0 → an operation-0 modifier.

   The non-armor branch would apply a speed of 0, so it is effectively dead.

   Bonus semantics, and when a call drains power (the "consume" flag), belong to 05-armor.
5. **Step assist** (client, local player): read the STEPASSIST bonus and store `hasStepassist` (bonus > 0) for the HUD.
   - If `enableStepAssist` is on and the bonus is > 0: raise `stepHeight` to 1.0 when it is below that.
   - Otherwise: if `stepHeight` > 0.5, force it to 0.5. This also overrides step height granted by other mods (CORE-14).
6. **Night vision:**
   - Query the NIGHTVISION bonus, consuming power only when `enableNightVision` is on.
   - `proxy.setHasNightvision(bonus > 0)`. This runs for every ticked player on both sides, so the HUD flag can come from another player (CORE-13).
   - If bonus > 0 and NV is enabled, on the server: apply Night Vision for **220 ticks**, amplifier −1, refreshed every tick. That is long enough to avoid the vanilla flicker under 10 s.
7. **Anti-gravity / creative flight** (CREATIVE_FLIGHT bonus):
   - Bonus > 0:
     - Set `allowFlying` and `gotCreativeFlightLastTick`.
     - On the tick flight starts (flying now, `wasFlying` false): the client spawns the FXList effect `AntiGravRing` on the player. The server sends `PacketPlaySound("techguns:items.antiGravStart", player, vol 1, pitch 1, no repeat, moving, not gun position, playOnOwnPlayer = true, PLAYER_EFFECT)` to everyone within 50 blocks.
     - `wasFlying` tracks `isFlying`.
   - Bonus = 0: if flight was granted last tick and the player isn't in creative, `allowFlying` and `isFlying` are cleared. Both flags reset.
8. **Fly speed** (client, local player). The default fly speed is 0.05.
   - FLYSPEED bonus > 0: fly speed = 0.05 × (1 + bonus) when `enableJetpack` is on, otherwise 0.05.
   - No FLYSPEED bonus but flight was granted: 0.05.
9. **Auto-feeder** ([below](#auto-feeder)).
10. **Special slots:** call `onPlayerTick(stack, event, props)` on the face-slot item's `ITGSpecialSlot`. Then reset `isGliding = false` and call it on the back-slot item (the glider sets `isGliding` again). Both casts are unchecked. Finally `proxy.handlePlayerGliding`: on the client, if `isGliding` and no loop is playing, start a repeating, moving `techguns:glider.loop` (PLAYER_EFFECT) on the player; if not gliding, stop it.

### Auto-feeder

Active unless `disableAutofeeder` is set, whenever food level ≤ 19. It runs on both sides; the client duplicates the work and is corrected by the next sync (CORE-16).

```
needed = 20 − foodLevel
if foodleft > 0:                         # leftover from a food eaten earlier
    if foodleft ≤ needed: addStats(foodleft, lastSaturation); foodleft = 0; lastSaturation = 0
    else:                 addStats(needed,  lastSaturation); foodleft −= needed
    server: full sync to the player
else:
    food = take 1 item from the first ItemFood stack in slots 2..4 (nothing → stop)
    call that ItemFood's protected onFoodEaten(stack, world, player)  # potion side effects
    server: play "random.eat" at the player (vol 1, pitch 1)
    h = heal amount, s = saturation modifier
    if h > needed: addStats(needed, s); foodleft = h − needed; lastSaturation = s
    else:          addStats(h, s);      foodleft = 0;          lastSaturation = 0
    server: full sync
```

`onFoodEaten` is found by reflection (names `onFoodEaten` / `func_77849_c`). Container returns such as stew bowls are not given back. Eating happens as soon as a single hunger point is missing.

### Other handlers in the tick-handler class

- **ItemCraftedEvent: akimbo split.** When the crafting result is a `GenericGun`, the first non-empty grid slot is an akimbo `GenericGun`, and a player is present:
  - The output stack size is forced to 1.
  - A copy of it gets NBT short `ammo` = akimbo ammo − ammo already in the output.
  - That copy is added to the player's main inventory. On the server, any part that doesn't fit is spawned as an item entity at the player's feet.
  - This runs on the client too, which inserts a client-only copy (CORE-36).
- **PlayerLoggedOutEvent** (registered as client-only, and also checks `isRemote`): removes the player from the client recoil/reload maps. FML fires this event on the server, so the code is effectively dead.
- **updateCheckMessage** and **onRenderTick**: covered in [client-effects-audio.md](client-effects-audio.md#version-checker) and [input-and-hud.md](input-and-hud.md#fov-and-zoom).

## TechgunsShooterValuesProperties (deprecated, unused)

An `IExtendedEntityProperties` named `ExtendedEntityShooter`, intended for any living shooter. It holds reload start (long), reload total (int), recoil start (long) and recoil total (int), saved as NBT keys `reloadtime`, `recoiltime`, `totalreloadtime`, `totalrecoiltime`, and only when they are above 0. It is never registered. The same data now lives in client-side `AttackTime` maps.
