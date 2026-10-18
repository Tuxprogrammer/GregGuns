# Special-slot equipment (face and back slots)

Source: `java/techguns/items/armors/ITGSpecialSlot.java`, [`ItemTGSpecialSlot.java`](lang-and-strings.md#3-armour-tooltips), [`ItemTGSpecialSlotAmmo.java`](lang-and-strings.md#3-armour-tooltips), `ItemGasMask.java`, `ItemNightVisionGoggles.java`, `ItemTacticalMask.java`, `ItemScubaTanks.java`, `ItemJetpack.java`, `ItemJumppack.java`, `ItemAntiGravPack.java`.

Two items (the glider and the oxygen mask) are metadata variants of the shared item [`GenericItemShared`](../06-items/special-items.md#shared-item-genericitemshared-behaviour) ([06-items](../06-items/index.md)). Their slot behaviour is described here. The registration lines are in [`TGItems.java`](../06-items/item-catalogue.md) ([06-items](../06-items/index.md)) and are restated here for completeness.

## 1. Interfaces and base classes

**`ITGSpecialSlot`**
- `getSlot(stack)` → a [`TGSlotType`](extra-inventory.md#2-tgslottype-enum)
- `onPlayerTick(stack, PlayerTickEvent, props)`, called every END-phase player tick on both sides while the stack is in the face or back slot
- `getBonus(type, stack, consume, player)`, which feeds into the bonus aggregation (set-bonuses.md §2)

**`ItemTGSpecialSlot`** (abstract; extends [`GenericItem`](../06-items/special-items.md#base-classes), implements `ITGSpecialSlot` and [`ICamoChangeable`](armor-catalogue.md#1-class-hierarchy))
- Constructor takes (unlocalizedName, textureName, camoCount, durability).
- Max stack 1; max damage = durability. The **durability bar is the item's charge or fuel**: damage == maxDamage means empty.
- The icon is `techguns:<unlocalizedName>`. The stored `textureName` field is **not used** for anything.
- **Camo:** NBT byte `camo` (default 0).
  - `switchCamo` cycles with wrap-around only when camoCount > 1, otherwise it returns 0.
  - `getCurrentCamoName`: index 0 → `techguns.item.defaultcamo` ("Default Camo"); index n > 0 → `techguns.item.<name>.camoname.<n>`.
- **Tooltip:** adds a grey `Slot: Face` or `Slot: Back` line, using `TGSlotType.toString` (hard-coded English).
- **Right-click** in the air: if the item belongs in the back (or face) slot and that TG-inventory slot is empty, copy the stack into the slot and shrink the held stack to 0 (quick-equip). Otherwise fall through to the default behaviour.

**`ItemTGSpecialSlotAmmo`** (abstract) adds:
- An "ammo" item and an "empty ammo" item.
- Tooltip line (aqua): `Reloads with: <ammo display name>`. Hard-coded English prefix.
- `tryReloadAndRepair(stack, player)`: if one ammo item can be consumed (TG ammo slots first, then main inventory), set damage to 0 (full) and give back one empty item. If the inventory is full, the server drops the empty as an item entity.

## 2. Registered items

| Item (registry / unlocalized) | English name | Class | Slot | Durability = charge | camoCount | Reload ammo → empty | Icon texture |
|---|---|---|---|---|---|---|---|
| [`gasMask`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems) | Gas Mask | `ItemGasMask` | Face | 300 | 1 | – (repaired by recipe) | [`textures/items/gasMask.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`nightVisionGoggles`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems) | Nightvision Goggles | `ItemNightVisionGoggles` | Face | 24000 | 1 | Energy Cell → Energy Cell (Empty) | [`textures/items/nightVisionGoggles.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`tacticalMask`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems) | Tactical Mask | `ItemTacticalMask` | Face | 24000 | 4 | Energy Cell → empty | [`textures/items/tacticalMask.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`scubaTanks`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems) | Oxygen Tanks | `ItemScubaTanks` | Back | 600 | 1 | Compressed Air Tank → empty | [`textures/items/scubaTanks.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`jetpack`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems) | Jetpack | `ItemJetpack` | Back | 5000 | 4 | Fuel Tank ([`flamethrowerTank`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo)) → Fuel Tank (Empty) | [`textures/items/jetpack.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) (64×64) |
| [`jumppack`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems) | Jump Pack | `ItemJumppack` | Back | 1000 | 4 | Compressed Air Tank → empty | [`textures/items/jumppack.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`antiGravPack`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems) | Anti Gravity Device | `ItemAntiGravPack` | Back | 12000 | 5 | Nuclear Power Cell → Nuclear Power Cell (Depleted) | [`textures/items/antiGravPack.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| shared item [`glider`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) | Glider | `GenericItemShared` | Back (stack size 1) | – | – | – | [`textures/items/glider.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| shared item [`oxygenMask`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) | Oxygen Mask | `GenericItemShared` | Face (stack size 1) | – | – | – | [`textures/items/oxygenMask.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |

Constructor texture-name arguments, which are unused for icons: gas mask `techguns:textures/armor/gasMask.png`; goggles `nightVisionGoggles`; jetpack `jetPackIcon`; scuba `oxygenFlasksIcon`; tactical `tacticalMaskItem`; jump pack `jumpPackIcon`; anti-grav `jetPackIcon`.

Recipes are in recipes.md. All icon textures are owned by [06-items](../06-items/index.md).

## 3. Face items

### 3.1 Gas Mask (`ItemGasMask`)
- **Tick:**
  - If the player has **Poison** and the mask is not empty, remove Poison and add 1 damage.
  - Then the same check for **Wither**.
  - This repeats every tick in which the effect is present, so standing in a continuously re-applied effect drains 1 per tick.
- **Bonuses:** none.
- **Does not reload.** Restore it with the shapeless recipe *gas mask (any damage) + Gas Mask Filter → new gas mask*.
- **Rendering:** while the mask is empty (damage == max), it is not drawn on the player.

### 3.2 Night Vision Goggles (`ItemNightVisionGoggles`)
- **Tick:**
  - If not empty and `enableNightVision` is on: add 1 damage per tick. A full charge lasts 24000 ticks (20 min) of active use.
  - If empty: try to reload from an Energy Cell.
- **Bonus:** NIGHTVISION = 1 while not empty. The effect is applied by the generic night-vision code (set-bonuses.md §3.1 step 6).
- **Rendering:** two models. The "lowered" goggles over the eyes are shown while the wearer has the Night Vision potion effect; the "raised" goggles flipped up onto the forehead are shown otherwise.

### 3.3 Tactical Mask (`ItemTacticalMask`)
A combined Night Vision Goggles, Gas Mask and Oxygen Mask.
- **Tick:**
  - If not empty and night vision is enabled, add 1 damage per tick. If empty, try to reload from an Energy Cell.
  - Then, like the gas mask, remove Poison and Wither, costing 1 damage each while not empty.
- **Bonuses:** NIGHTVISION = 1 and OXYGEN_GEAR = 1 while not empty.
- **Camos:** 0 = `tacticalMask` (black), 1 = `tacticalMask_green`, 2 = `tacticalMask_white`, 3 = `tacticalMask_lightgreen`. The camo-3 path does not match the file's case; see bug [ARM-14](index.md#suspected-bugs).

### 3.4 Oxygen Mask (shared item)
- `GenericItemShared.getBonus` returns OXYGEN_GEAR = 1 for the oxygen-mask metadata.
- It has no tick logic and no durability. Its only purpose is to enable the Oxygen Tanks.
- Unlike the other special items, it cannot be quick-equipped by right-clicking (bug [ARM-21](index.md#suspected-bugs)). Put it in the slot through the GUI or with shift-click.

## 4. Back items

### 4.1 Oxygen Tanks (`ItemScubaTanks`)
- **Tick:**
  - If empty, try to reload from a Compressed Air Tank.
  - Then, if not empty, the player's air ≤ 280 (max 300), **and** the player has OXYGEN_GEAR > 0 from some source (T2 Commando helmet, powered HEV helmet, Tactical Mask or Oxygen Mask): add 20 air and 1 damage.

  In practice this keeps the air bar topped up, costing 1 charge per 20 air. 600 charge is about 12 000 air, i.e. 40 full air bars.
- Without oxygen gear the tanks do nothing. "Oxygen Gear" has no other effect anywhere.
- **Bonuses:** none.

### 4.2 Glider (shared item) and its physics (`GenericItemShared.onPlayerTickGlider`)
The same routine runs for the glider and, as a side effect, for a jetpack that is not empty.

**Active** when the player is **not on the ground and is sneaking**.
- If motionY < −0.1:
  - Set `props.isGliding = true`.
  - Get the horizontal look direction (x, z), normalised.
  - `m = min(0.08, −0.1 − motionY)`.
  - motionY += m.
  - motionX += dirX × m and motionZ += dirZ × m.
  - jumpMovementFactor += 0.03.

  In other words, part of the falling speed is converted into forward speed, capping the descent at about 0.1 blocks/tick. The source comment credits the algorithm to Modular Powersuits.
- While active, fallDistance is reset to 0 every tick, whether or not the player is falling fast enough to glide.

**Sound:** each tick, `handlePlayerGliding` (client) checks `props.isGliding` for every player.
- When it becomes true, start a looping, moving [`TGSound`](../01-core/client-effects-audio.md#tgsound) `techguns:glider.loop` (volume 1, pitch 1, category PLAYER_EFFECT) attached to that player.
- When it becomes false, stop the loop.

**Visual:** the folded backpack is always drawn. The wings are drawn only while the player is airborne and sneaking (rendering-and-models.md).

### 4.3 Jetpack (`ItemJetpack`)
**Tick, both sides:** if empty (damage ≥ max), try to reload from a Fuel Tank.

**Client, own player only**, while `enableJetpack` is on, watch the vanilla **Jump** key:
- On press: set `jumpkeyPressed = true` and send `PacketTGKeybindPress(JETPACK_BOOST_START = 2)`.
- On release: set it false and send `JETPACK_BOOST_STOP = 3`.

Then, if the jetpack is not empty:
- While `jumpkeyPressed`: apply the boost.
- Also run the glider routine (§4.2).

If the jetpack is empty and `jumpkeyPressed` is true, clear it and send BOOST_STOP.

**Server:** if not empty, apply the boost while the synced `jumpkeyPressed` is true, and run the glider routine.

**Boost (per tick):**
- `motionY = min(motionY + 0.15, 0.4)`
- `fallDistance = 0`
- damage + 1

So a full tank (5000) gives 250 s of thrust. Thrust speed is capped at 0.4 blocks/tick upward. There is no horizontal thrust; the player steers with normal air control.

**Jump-key state sync (`PacketTGKeybindPress` handler, [01-core](../01-core/index.md)):**
- BOOST_START / BOOST_STOP set the server flag and broadcast `PacketTGPlayerFieldSync(FIELD_JUMPBUTTONPRESSED = 0, value)` to the whole dimension.
- On every client, the client-side properties' `setJumpkeyPressed` plays the sounds:
  - On false→true: play `techguns:jetpack.start` once at the player, and start a looping, moving `techguns:jetpack.loop` sound (category PLAYER_EFFECT).
  - On true→false: stop the loop and play `techguns:jetpack.stop`.

**Flames:** while a jetpack wearer's `jumpkeyPressed` is true, the renderer spawns two `Jetpack` particles per frame ([04-projectiles-fx](../04-projectiles-fx/index.md): an animated flamethrower sprite, 10-tick life).
- Positions: offset from the player by ±0.2 sideways and −0.3 backwards (−0.45 when sneaking), rotated by the body yaw, at y − 0.7.
- Vertical motion: min(−0.025, player motionY).

**Dead code:** hover and descend routines (`hoverPlayer`, `descendPlayer`) and forward-key packets (`JETPACK_FORWARD_START/STOP = 4/5`, `isForwardKeyPressed`) exist but are never triggered. A `hovermode` player flag is saved but unused.

**Bonuses:** none.

### 4.4 Jump Pack (`ItemJumppack`)
Constants: jump boost **0.8**, free fall height **8.0** blocks, fall-damage reduction **0.2**, cost **1** per use.

**Tick:** if damage + 1 > max, try to reload from a Compressed Air Tank.

**Bonuses:**

| Bonus | Behaviour |
|---|---|
| JUMP, consume (`LivingJumpEvent`) | If `enableJetpack` is on and there is enough charge: add 1 damage, have the server send the effects, and return 0.8. Otherwise return 0. |
| JUMP, no consume | 0.8 if there is enough charge (used only for queries) |
| FREEHEIGHT | 8.0 while not empty, regardless of the toggle |
| FALLDMG, consume (falls > 3 blocks) | If the toggle is on and there is charge: add 1 damage, have the server send the `JumpPackFall` particle at the player's feet to players within 50 blocks, and return 0.2. Otherwise return 0. |
| FALLDMG, no consume (falls ≤ 3 blocks) | 0.2 while not empty |

**Jump effects (server, within 50 blocks):**
- Particle-on-entity `JumpPackBoost` ([04-projectiles-fx](../04-projectiles-fx/index.md)).
- Sound `techguns:effects.steamarmorJump2`, volume 1, pitch 1, category PLAYER_EFFECT.

**Tooltip** adds `Jump height: +0.8`, `Fall damage: -20.0%` and `Fall height: -8.0`.

**Rendering:** the jetpack model without wings (variant 1). It shares the four jetpack camo textures.

### 4.5 Anti Gravity Device (`ItemAntiGravPack`)
Constants: consumption 1 per tick of flight; fly-speed bonus **0.25**.

**Tick:**
- If damage + 1 ≥ max, try to reload from a Nuclear Power Cell (one tick before it runs dry).
- While the player is flying and **not** in creative mode, add 1 damage per tick (12000 ticks = 10 min of flight).

**Bonuses** (while not empty):
- CREATIVE_FLIGHT = 1, which grants vanilla creative-style double-tap-jump flight (set-bonuses.md §3.1 step 7).
- FLYSPEED = 0.25, i.e. +25 % fly speed while `enableJetpack` is on.

**On flight start:** an `AntiGravRing` FX (client) and the `techguns:items.antiGravStart` sound (server broadcast). A `doEffect` method that would spawn `AntiGravRingGreen` each tick exists, but its call is commented out.

**When the pack empties or is removed:** flight is revoked (allowFlying and isFlying set to false) unless the player is in creative.

**Tooltip** adds `Creative Flight` and `Flying Speed: +25.0%`.

## 5. Interaction with armour

- [`GenericArmor.hideFaceslot`](armor-catalogue.md#1-class-hierarchy) (Steam, Power and HEV helmets) stops the face item from being **drawn**. It does not disable its function.
- `hideBackslot` (Steam chestplate only) stops the back item from being drawn, but only for renderers that honour it: Oxygen Tanks and Anti-Grav. The Jetpack, Jump Pack and Glider always draw (rendering-and-models.md §3).

## 6. Sounds used by armour and special equipment

The sound files are owned by [04-projectiles-fx](../04-projectiles-fx/index.md) and [06-items](../06-items/index.md); they are listed here with their trigger. All events are category `player` in `sounds.json`.

| sounds.json event | Files | When it plays |
|---|---|---|
| `effects.steamarmorJump` | [`sounds/effects/steamarmor_jump_a.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/steamarmor_jump_b.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/steamarmor_jump_c.ogg`](../04-projectiles-fx/sounds.md) | Server, on every jump while wearing powered leggings that have power (vol 0.66). The event plays one of the three variants at random. |
| `effects.steamarmorJump2` | [`sounds/effects/steamarmor_jump_d.ogg`](../04-projectiles-fx/sounds.md) | Jump Pack boost jump (broadcast within 50 blocks) |
| `nightvision.on` | [`sounds/items/nightvision_on.ogg`](../06-items/sounds.md) | Toggling night vision on while a NIGHTVISION source is worn (server, world sound at the player) |
| `nightvision.off` | [`sounds/items/nightvision_off.ogg`](../06-items/sounds.md) | Toggling night vision off while the Night Vision potion is active |
| `glider.loop` | [`sounds/items/glider_loop_stereo.ogg`](../06-items/sounds.md) | Looping while gliding (glider, or a jetpack used as a glider) |
| `jetpack.start` | [`sounds/items/jetpack_start_a.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_start_b.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_start_c.ogg`](../06-items/sounds.md) | Jetpack boost begins (every client) |
| `jetpack.loop` | [`sounds/items/jetpack_loop_a.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_loop_b.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_loop_c.ogg`](../06-items/sounds.md) | Looping while boosting |
| `jetpack.stop` | [`sounds/items/jetpack_stop_a.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_stop_b.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_stop_c.ogg`](../06-items/sounds.md) | Boost ends |
| `items.antiGravStart` | [`sounds/items/antigrav_start_a.ogg`](../06-items/sounds.md), [`sounds/items/antigrav_start_b.ogg`](../06-items/sounds.md) | Anti-grav flight begins (broadcast within 50 blocks) |

[`sounds/items/glider_loop.ogg`](../06-items/sounds.md) exists but no sounds.json event references it, so it is never played.

## 7. FX names referenced (implemented in 04-projectiles-fx)

`Jetpack` (flame particle), `JumpPackBoost` (on-entity burst), `JumpPackFall` (landing puff), `AntiGravRing` (ring on flight start), and `AntiGravRingGreen` (unused).

## 8. Toggle packets (server handling, `PacketTGKeybindPress`)

The packet carries one byte, the button ID.

| ID | Constant | Server action |
|---|---|---|
| 0 | `TOGGLE_NIGHTVISION` | Flip `enableNightVision`. When turning off with Night Vision active, remove the effect and play `nightvision.off`. When turning on with a NIGHTVISION source worn, play `nightvision.on`. Then send a full player sync ([01-core](../01-core/index.md)). |
| 7 | `TOGGLE_JETPACK` | Only while the jump key is not held: flip `enableJetpack` and send a field sync (`FIELD_ENABLEJETPACK`) to that player. (A nested "stop boost" branch inside it can never run; bug [ARM-16](index.md#suspected-bugs).) |
| 8 | `TOGGLE_STEP_ASSIST` | Flip `enableStepAssist` and send a full sync. |
| 2 / 3 | `JETPACK_BOOST_START` / `JETPACK_BOOST_STOP` | See §4.3. |
| 4 / 5 | `JETPACK_FORWARD_START` / `JETPACK_FORWARD_STOP` | Set `isForwardKeyPressed` (unused). |
| 1, 6, 9 | safe mode, force reload, toggle HUD | [01-core](../01-core/index.md) / [02-guns](../02-guns/index.md) |

Default key bindings (category `techguns.key.categories.techguns`): night vision **N**, safe mode B, step assist **V**, force reload R, jetpack **J**. The jetpack boost uses the vanilla Jump key.
