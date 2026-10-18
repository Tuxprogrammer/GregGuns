# Special-slot equipment (face and back slots)

Source: `java/techguns/items/armors/ITGSpecialSlot.java`, [`ItemTGSpecialSlot.java`](lang-and-strings.md#3-armor-tooltips), [`ItemTGSpecialSlotAmmo.java`](lang-and-strings.md#3-armor-tooltips), `ItemGasMask.java`, `ItemNightVisionGoggles.java`, `ItemTacticalMask.java`, `ItemScubaTanks.java`, `ItemJetpack.java`, `ItemJumppack.java`, `ItemAntiGravPack.java`.

Two items (the glider and the oxygen mask) are metadata variants of the shared item [`GenericItemShared`](../06-items/special-items.md#shared-item-genericitemshared-behavior) ([06-items](../06-items/index.md)). Their slot behavior is described here. The registration lines are in [`TGItems.java`](../06-items/item-catalog.md) ([06-items](../06-items/index.md)) and are restated here for completeness.

## 1. Interfaces and base classes

**`ITGSpecialSlot`**
- `getSlot(stack)` → a [`TGSlotType`](extra-inventory.md#2-tgslottype-enum)
- `onPlayerTick(stack, PlayerTickEvent, props)`, called every END-phase player tick on both sides while the stack is in the face or back slot
- `getBonus(type, stack, consume, player)`, which feeds into the bonus aggregation (set-bonuses.md §2)

**`ItemTGSpecialSlot`** (abstract; extends [`GenericItem`](../06-items/special-items.md#base-classes), implements `ITGSpecialSlot` and [`ICamoChangeable`](armor-catalog.md#1-class-hierarchy))
- Constructor takes (unlocalizedName, textureName, camoCount, durability).
- Max stack 1; max damage = durability. The **durability bar is the item's charge or fuel**: damage == maxDamage means empty.
- The icon is `techguns:<unlocalizedName>`. The stored `textureName` field is **not used** for anything.
- **Camo:** NBT byte `camo` (default 0).
  - `switchCamo` cycles with wrap-around only when camoCount > 1, otherwise it returns 0.
  - `getCurrentCamoName`: index 0 → `techguns.item.defaultcamo` ("Default Camo"); index n > 0 → `techguns.item.<name>.camoname.<n>`.
- **Tooltip:** adds a gray `Slot: Face` or `Slot: Back` line, using `TGSlotType.toString` (hard-coded English).
- **Right-click** in the air: if the item belongs in the back (or face) slot and that TG-inventory slot is empty, copy the stack into the slot and shrink the held stack to 0 (quick-equip). Otherwise fall through to the default behavior.

**`ItemTGSpecialSlotAmmo`** (abstract) adds:
- An "ammo" item and an "empty ammo" item.
- Tooltip line (aqua): `Reloads with: <ammo display name>`. Hard-coded English prefix.
- `tryReloadAndRepair(stack, player)`: if one ammo item can be consumed (TG ammo slots first, then main inventory), set damage to 0 (full) and give back one empty item. If the inventory is full, the server drops the empty as an item entity.

## 2. Registered items

| Item (registry / unlocalized) | English name | Class | Slot | Durability = charge | camoCount | Reload ammo → empty | Icon texture |
|---|---|---|---|---|---|---|---|
| [`gasMask`](../06-items/item-catalog.md#2-stand-alone-items-registered-by-tgitems) | Gas Mask | `ItemGasMask` | Face | 300 | 1 | – (repaired by recipe) | [`textures/items/gasMask.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`nightVisionGoggles`](../06-items/item-catalog.md#2-stand-alone-items-registered-by-tgitems) | Nightvision Goggles | `ItemNightVisionGoggles` | Face | 24000 | 1 | Energy Cell → Energy Cell (Empty) | [`textures/items/nightVisionGoggles.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`tacticalMask`](../06-items/item-catalog.md#2-stand-alone-items-registered-by-tgitems) | Tactical Mask | `ItemTacticalMask` | Face | 24000 | 4 | Energy Cell → empty | [`textures/items/tacticalMask.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`scubaTanks`](../06-items/item-catalog.md#2-stand-alone-items-registered-by-tgitems) | Oxygen Tanks | `ItemScubaTanks` | Back | 600 | 1 | Compressed Air Tank → empty | [`textures/items/scubaTanks.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`jetpack`](../06-items/item-catalog.md#2-stand-alone-items-registered-by-tgitems) | Jetpack | `ItemJetpack` | Back | 5000 | 4 | Fuel Tank ([`flamethrowerTank`](../06-items/item-catalog.md#1-the-shared-meta-item-techgunstechgunsammo)) → Fuel Tank (Empty) | [`textures/items/jetpack.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) (64×64) |
| [`jumppack`](../06-items/item-catalog.md#2-stand-alone-items-registered-by-tgitems) | Jump Pack | `ItemJumppack` | Back | 1000 | 4 | Compressed Air Tank → empty | [`textures/items/jumppack.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| [`antiGravPack`](../06-items/item-catalog.md#2-stand-alone-items-registered-by-tgitems) | Anti Gravity Device | `ItemAntiGravPack` | Back | 12000 | 5 | Nuclear Power Cell → Nuclear Power Cell (Depleted) | [`textures/items/antiGravPack.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| shared item [`glider`](../06-items/item-catalog.md#1-the-shared-meta-item-techgunstechgunsammo) | Glider | `GenericItemShared` | Back (stack size 1) | – | – | – | [`textures/items/glider.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |
| shared item [`oxygenMask`](../06-items/item-catalog.md#1-the-shared-meta-item-techgunstechgunsammo) | Oxygen Mask | `GenericItemShared` | Face (stack size 1) | – | – | – | [`textures/items/oxygenMask.png`](../06-items/textures.md#9-wearables-and-special-slot-icons) |

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
  - Get the horizontal look direction (x, z), normalized.
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
- Positions: offset from the player by ±0.2 sideways and −0.3 backward (−0.45 when sneaking), rotated by the body yaw, at y − 0.7.
- Vertical motion: min(−0.025, player motionY).

**Dead code:** hover and descend routines (`hoverPlayer`, `descendPlayer`) and forward-key packets (`JETPACK_FORWARD_START/STOP = 4/5`, `isForwardKeyPressed`) exist but are never triggered. A `hovermode` player flag is saved but unused.

**Bonuses:** none.

### 4.4 Jump Pack (`ItemJumppack`)
Constants: jump boost **0.8**, free fall height **8.0** blocks, fall-damage reduction **0.2**, cost **1** per use.

**Tick:** if damage + 1 > max, try to reload from a Compressed Air Tank.

**Bonuses:**

| Bonus | Behavior |
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

## 5. Interaction with armor

- [`GenericArmor.hideFaceslot`](armor-catalog.md#1-class-hierarchy) (Steam, Power and HEV helmets) stops the face item from being **drawn**. It does not disable its function.
- `hideBackslot` (Steam chestplate only) stops the back item from being drawn, but only for renderers that honor it: Oxygen Tanks and Anti-Grav. The Jetpack, Jump Pack and Glider always draw (rendering-and-models.md §3).

## 6. Sounds used by armor and special equipment

The sound files are owned by [04-projectiles-fx](../04-projectiles-fx/index.md) and [06-items](../06-items/index.md); they are listed here with their trigger. All events are category `player` in `sounds.json`.

| sounds.json event | Files | When it plays | Duration, rate, channels ([ASSET-FACTS](../ASSET-FACTS.md)) | Volume / pitch |
|---|---|---|---|---|
| `effects.steamarmorJump` | [`sounds/effects/steamarmor_jump_a.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/steamarmor_jump_b.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/steamarmor_jump_c.ogg`](../04-projectiles-fx/sounds.md) | Server, on every jump while wearing powered leggings that have power (vol 0.66). The event plays one of the three variants at random. | each 1.21 s, 44100 Hz, mono | 0.66 / 1.0 |
| `effects.steamarmorJump2` | [`sounds/effects/steamarmor_jump_d.ogg`](../04-projectiles-fx/sounds.md) | Jump Pack boost jump (broadcast within 50 blocks) | 1.21 s, 44100 Hz, mono | 1.0 / 1.0 |
| `nightvision.on` | [`sounds/items/nightvision_on.ogg`](../06-items/sounds.md) | Toggling night vision on while a NIGHTVISION source is worn (server, world sound at the player) | 1.60 s, 44100 Hz, mono | 1.0 / 1.0 |
| `nightvision.off` | [`sounds/items/nightvision_off.ogg`](../06-items/sounds.md) | Toggling night vision off while the Night Vision potion is active | 0.44 s, 44100 Hz, mono | 1.0 / 1.0 |
| `glider.loop` | [`sounds/items/glider_loop_stereo.ogg`](../06-items/sounds.md) | Looping while gliding (glider, or a jetpack used as a glider) | 9.41 s, 48000 Hz, **stereo** (stereo samples are not positioned in 3-D by the vanilla sound engine, so other players hear it at full level regardless of distance; inferred from the format) | 1.0 / 1.0, repeating |
| `jetpack.start` | [`sounds/items/jetpack_start_a.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_start_b.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_start_c.ogg`](../06-items/sounds.md) | Jetpack boost begins (every client) | each 1.29 s, 48000 Hz, mono | 1.0 / 1.0 |
| `jetpack.loop` | [`sounds/items/jetpack_loop_a.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_loop_b.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_loop_c.ogg`](../06-items/sounds.md) | Looping while boosting | each 1.10 s, 48000 Hz, mono | 1.0 / 1.0, repeating, follows the player |
| `jetpack.stop` | [`sounds/items/jetpack_stop_a.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_stop_b.ogg`](../06-items/sounds.md), [`sounds/items/jetpack_stop_c.ogg`](../06-items/sounds.md) | Boost ends | each 1.40 s, 48000 Hz, mono | 1.0 / 1.0 |
| `items.antiGravStart` | [`sounds/items/antigrav_start_a.ogg`](../06-items/sounds.md), [`sounds/items/antigrav_start_b.ogg`](../06-items/sounds.md) | Anti-grav flight begins (broadcast within 50 blocks) | each 1.81 s, 48000 Hz, mono | 1.0 / 1.0 |

[`sounds/items/glider_loop.ogg`](../06-items/sounds.md) exists but no sounds.json event references it, so it is never played (3.02 s, 48000 Hz, mono per ASSET-FACTS).

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

## 9. Entity sheets for every special-slot item

Added by the consistency pass. One sheet per item, in the CONVENTIONS field order (1 Identity, 2 Obtaining, 3 Numbers, 4 Behavior, 5 Persistence and sync, 6 UI, 7 Visuals, 8 Audio, 9 Relationships, 10 Bugs). Facts shared by all of them are stated once here.

**Shared by every item below**
- **Unlocalized names:** `GenericItem` prepends `techguns.` to the vanilla `item.<name>` form, so the unlocalized name is `techguns.item.<registry name>` and the English name key is that plus `.name`. Camo name keys are that plus `.camoname.<n>`.
- **Creative tab:** [`Techguns`](../01-core/lifecycle-and-registration.md#lifecycle-overview) (`Techguns.tabTechgun`), set by the `GenericItem` base (stand-alone items) or the `GenericItemShared` constructor (the two shared items).
- **Oredict names:** none registered for any of these items.
- **Config keys:** none. The only switches are the per-player flags `enableNightvision` and `enableJetpack` (saved in the player's extended NBT, toggled with the keybinds in §8), not config keys. [`TGConfig`](../01-core/config.md) has no key that mentions these items.
- **Item tooltip lines**, in order: the item name; gray `Slot: Face` or `Slot: Back`; for the six ammo-using items, aqua `Reloads with: <ammo name>`; then any item-specific lines. There is no Shift-expanded tooltip and no durability text (the vanilla durability bar is the only charge display). The slot word is hard-coded English from `TGSlotType.toString`.
- **Stack NBT:** only the byte `camo` (default 0, written by `switchCamo`). Charge is the vanilla damage value, not an NBT key. Crafted and loot stacks carry no tag at all.
- **Tick scheduling:** `onPlayerTick` runs from [`TechgunsTickHandler`](../01-core/events.md#techgunstickhandler-fml-bus) at the END phase of each player tick, on **both** logical sides. The face item ticks first; then `isGliding` is reset to false; then the back item ticks; then the client glider-sound handler runs. Because both sides tick the stack, charge changes and ammo consumption happen on the server copy and on the client's copy independently (bug [ARM-34](index.md#suspected-bugs)).
- **Camo switching:** the Camo Bench cycles `camo` with wrap-around (`switchCamo`); items with a camo count of 1 never change.
- **Sound facts** are quoted from [ASSET-FACTS](../ASSET-FACTS.md). Any description of what a sound is like is inferred from its file name only; nothing was listened to.

### 9.1 Gas Mask
1. **Identity:** class `ItemGasMask` (extends `ItemTGSpecialSlot`); registry name `gasMask`; unlocalized `techguns.item.gasMask`; lang `techguns.item.gasMask.name`; English **Gas Mask**; slot Face; charge is the damage value, not a metadata value.
2. **Obtaining:** crafting `grg` / `rrr` / `rfr` (`g` "paneGlass", `r` "itemRubber", `f` Gas Mask Filter), see [recipes.md](recipes.md) §2. Refill by shapeless crafting with a Gas Mask Filter. Loot: `techgunsMilBaseBarracks` (weight 2) and the vanilla dungeon chest (weight 1). A heavier variant (`gasMaskHeavy`, 600 charge) is commented out in `TGItems` and does not exist.
3. **Numbers:** max damage (charge) **300**; camo count **1**; stack size 1; bonuses none; reload ammo none.
4. **Behavior:** *Both sides, every tick, face slot:* if Poison is active and damage < max, remove Poison and add 1 damage; then the same for Wither (both effects in one tick cost 2). At damage == max it does nothing and is not rendered. It does not reload: restore it with the shapeless recipe. It has no effect when held anywhere other than the face slot.
5. **Persistence and sync:** damage value only (plus `camo`). The face stack is mirrored to other clients through the player DataWatcher so they can render it ([extra-inventory.md](extra-inventory.md)).
6. **UI:** tooltip is just `Slot: Face` (gray). No chat messages.
7. **Visuals:** inventory icon `textures/items/gasMask.png` (32x32, avg #2B3E2F, dominant #305030 20%, #103010 17%, #505050 15%). Worn model [`ModelGasMask`](rendering-and-models.md) ([rendering-and-models.md](rendering-and-models.md) §2.6) drawn by [`RenderAdditionalArmorModel`](rendering-and-models.md#3-special-slot-rendering-on-the-player-rendereradditionalslots); texture [`textures/armor/gasMask.png`](textures.md#9-face-gear-layout-d-6464) (64x64, avg #232B21, dominant #303030 69%, #103010 23%, #101010 5%, 45% opaque). Hidden when the helmet has `hideFaceslot` or when the mask is empty. No particles or FX.
8. **Audio:** none.
9. **Relationships:** consumes the Gas Mask Filter shared item (meta 74, [06-items](../06-items/item-catalog.md)); is an ingredient of the Tactical Mask.
10. **Bugs:** [ARM-22](index.md#suspected-bugs) (two worn masks merge by vanilla repair crafting), [ARM-24](index.md#suspected-bugs) (head shell drawn twice), [ARM-30](index.md#suspected-bugs), [ARM-34](index.md#suspected-bugs).

### 9.2 Nightvision Goggles
1. **Identity:** class `ItemNightVisionGoggles` (extends `ItemTGSpecialSlotAmmo`); registry `nightVisionGoggles`; unlocalized `techguns.item.nightVisionGoggles`; lang `techguns.item.nightVisionGoggles.name`; English **Nightvision Goggles**; slot Face.
2. **Obtaining:** crafting `rhr` / `cec` / `gsg` (see [recipes.md](recipes.md) §2); a second recipe with an Energy Cell (Empty) yields goggles at damage 24000. Loot: barracks chest (weight 1) and the vanilla dungeon chest (weight 1).
3. **Numbers:** max damage **24000**; camo count 1; ammo Energy Cell, empty Energy Cell (Empty); NIGHTVISION bonus 1.0 while damage < max.
4. **Behavior:** *Both sides, every tick:* if charge remains and the player's `enableNightVision` flag is on, add 1 damage (20 min of continuous use). If empty, try to reload (consume one Energy Cell from the TG ammo slots, else the main inventory; set damage 0; give back an Energy Cell (Empty), dropped as an item entity by the server if the inventory is full). Drain does not depend on whether it is dark or whether the potion effect is visible. The actual Night Vision potion is applied by the generic night-vision code, not by this class ([set-bonuses.md](set-bonuses.md) §3.1).
5. **Persistence and sync:** damage, `camo`; player flag NBT `enableNightvision` (boolean). Toggle packet `PacketTGKeybindPress` button 0 (keybind default **N**, lang `techguns.key.toggleNightvision`), answered by a full [`PacketTGExtendedPlayerSync`](../01-core/networking.md#5--packettgextendedplayersync) to that player; the GUI toggle button sends the same packet.
6. **UI:** tooltip `Slot: Face`, aqua `Reloads with: Energy Cell`. Inventory toggle-button tooltips are in [lang-and-strings.md](lang-and-strings.md) §4. The HUD shows a night-vision icon ([01-core](../01-core/index.md)).
7. **Visuals:** icon `textures/items/nightVisionGoggles.png` (32x32, avg #0D2011, dominant #101010 75%, #107030 8%, #303030 6%). Worn models [`ModelNightVisionGoggles`](rendering-and-models.md) (down, while the wearer has the Night Vision effect) and [`ModelNightVisionGogglesOff`](rendering-and-models.md) (flipped up otherwise), renderer [`RenderNightVisionGoggles`](rendering-and-models.md#3-special-slot-rendering-on-the-player-rendereradditionalslots); texture [`textures/armor/nightVisionGoggles.png`](textures.md#9-face-gear-layout-d-6464) (64x64, avg #161816, dominant #101010 95%, #105010 1%, #101030 1%, 15% opaque).
8. **Audio:** `nightvision.on` (1.60 s, 44100 Hz, mono, vol 1.0, pitch 1.0; world sound at the player, played by the toggle handler when a NIGHTVISION source is worn) and `nightvision.off` (0.44 s, 44100 Hz, mono, vol 1.0, pitch 1.0; only if the Night Vision effect was active at the time). Details in §6.
9. **Relationships:** an ingredient of the Tactical Mask; uses Energy Cell items ([06-items](../06-items/index.md)).
10. **Bugs:** [ARM-08](index.md#suspected-bugs), [ARM-29](index.md#suspected-bugs), [ARM-22](index.md#suspected-bugs), [ARM-24](index.md#suspected-bugs), [ARM-34](index.md#suspected-bugs).

### 9.3 Tactical Mask
1. **Identity:** class `ItemTacticalMask`; registry `tacticalMask`; unlocalized `techguns.item.tacticalMask`; lang `techguns.item.tacticalMask.name`; English **Tactical Mask**; slot Face.
2. **Obtaining:** crafting `pnp` / `cgc` / `pop` ([recipes.md](recipes.md) §2) consuming Nightvision Goggles, a Gas Mask (any damage) and an Oxygen Mask. Not in any loot table.
3. **Numbers:** max damage **24000**; camo count **4**; ammo Energy Cell; bonuses NIGHTVISION = 1 and OXYGEN_GEAR = 1 while damage < max. Each Poison or Wither removal costs 1 damage.
4. **Behavior:** *Both sides, every tick:* the goggles logic (1 damage per tick while `enableNightVision`; reload from an Energy Cell when empty), then the gas-mask logic (remove Poison and Wither, 1 damage each, only while charge remains). The OXYGEN_GEAR bonus lets the Oxygen Tanks work (9.5). The 24000 charge is shared, so poison removal and night vision compete for it.
5. **Persistence and sync:** damage; `camo` byte 0..3. Night-vision toggle as in 9.2.
6. **UI:** tooltip `Slot: Face`, aqua `Reloads with: Energy Cell`. Camo names from `getCurrentCamoName`: 0 `Default Camo`; 1 Dark Green (`techguns.item.tacticalMask.camoname.1`); 2 White (`.2`); 3 has no lang key, so the raw key is shown (bug ARM-13).
7. **Visuals:** icon `textures/items/tacticalMask.png` (16x16, avg #1D1111, dominant #101010 81%, #701010 8%, #501010 7%). Model `ModelFaceMask(true)` via `RenderAdditionalArmorModel`; textures by camo: 0 `tacticalMask.png` (64x64, avg #1B1A1A), 1 `tacticalMask_green.png` (avg #0E180E), 2 `tacticalMask_white.png` (avg #939394), 3 requested as `tacticalMask_lightgreen.png` but the file is `tacticalmask_lightgreen.png` (avg #232F1A; wrong-case path, bug ARM-14). All are 40% opaque; dominant colors are in [textures.md](textures.md) §9.
8. **Audio:** the night-vision toggle sounds of 9.2 (played whenever the mask is the NIGHTVISION source).
9. **Relationships:** crafted from Nightvision Goggles + Gas Mask + Oxygen Mask; enables Oxygen Tanks.
10. **Bugs:** [ARM-14](index.md#suspected-bugs), [ARM-13](index.md#suspected-bugs), [ARM-12](index.md#suspected-bugs), [ARM-22](index.md#suspected-bugs), [ARM-24](index.md#suspected-bugs), [ARM-29](index.md#suspected-bugs), [ARM-34](index.md#suspected-bugs).

### 9.4 Oxygen Mask (shared item, metadata 90)
1. **Identity:** `GenericItemShared` (the shared item `Techguns:TechgunsAmmo`, see [06-items](../06-items/item-catalog.md#1-the-shared-meta-item-techgunstechgunsammo)), metadata **90**, name `oxygenMask`, lang `techguns.item.oxygenMask.name`, English **Oxygen Mask**; slot Face; stack size 1; no damage bar.
2. **Obtaining:** crafting ` p ` / `rpr` (`p` "sheetPlastic", `r` "itemRubber"). Loot: barracks chest (weight 1).
3. **Numbers:** OXYGEN_GEAR bonus **1.0**, always (no charge, no durability).
4. **Behavior:** no tick logic. It only supplies the OXYGEN_GEAR bonus. Right-click does not quick-equip it (the shared-item handler has no face branch, bug ARM-21); place it with the GUI or shift-click.
5. **Persistence and sync:** none (no NBT).
6. **UI:** no item-specific lines; shared-item tooltip rules ([06-items](../06-items/special-items.md#shared-item-genericitemshared-behavior)) add nothing for this meta.
7. **Visuals:** icon `textures/items/oxygenMask.png` (16x16, avg #818181, dominant #707070 43%, #909090 32%, #B0B0B0 15%). Worn: `ModelFaceMask(true)`; texture [`textures/armor/oxygenMask.png`](textures.md#9-face-gear-layout-d-6464) (64x64, avg #777777, dominant #707070 63%, #909090 25%, #505050 6%, only 9% opaque).
8. **Audio:** none.
9. **Relationships:** an ingredient of the Tactical Mask; enables Oxygen Tanks.
10. **Bugs:** [ARM-21](index.md#suspected-bugs), [ARM-24](index.md#suspected-bugs).

### 9.5 Oxygen Tanks
1. **Identity:** `ItemScubaTanks`; registry `scubaTanks`; unlocalized `techguns.item.scubaTanks`; lang `techguns.item.scubaTanks.name`; English **Oxygen Tanks**; slot Back.
2. **Obtaining:** crafting `p_p` / `aia` / `p_p` ([recipes.md](recipes.md) §2); loot: barracks chest (weight 1).
3. **Numbers:** max damage **600**; camo count 1; ammo Compressed Air Tank (empty: Compressed Air Tank (Empty)); top-up when air <= 280 of 300; +20 air and 1 damage per top-up; bonuses none.
4. **Behavior:** *Both sides, every tick:* if empty, try to reload; then if charge remains, air <= 280 and the player has OXYGEN_GEAR from another source, add 20 air and 1 damage. The tanks never provide oxygen gear themselves.
5. **Persistence and sync:** damage, `camo`.
6. **UI:** tooltip `Slot: Back`, aqua `Reloads with: Compressed Air Tank`.
7. **Visuals:** icon `textures/items/scubaTanks.png` (32x32, avg #606060, dominant #909090 21%, #101010 21%, #B0B0B0 15%); model [`ModelOxygenFlasks`](rendering-and-models.md), renderer [`RenderOxygenFlasks`](rendering-and-models.md#3-special-slot-rendering-on-the-player-rendereradditionalslots); texture [`textures/armor/oxygenFlasks.png`](textures.md#10-back-gear-layout-d-6464) (64x64, avg #424242, dominant #101010 44%, #707070 23%, #909090 14%, 20% opaque). Hidden by `hideBackslot` (Steam chestplate).
8. **Audio:** none (no bubble or refill sound).
9. **Relationships:** needs an OXYGEN_GEAR source (Commando helmet, HEV helmet, Tactical Mask, Oxygen Mask).
10. **Bugs:** [ARM-22](index.md#suspected-bugs), [ARM-34](index.md#suspected-bugs).

### 9.6 Glider (shared item, metadata 51)
1. **Identity:** `GenericItemShared` meta **51**, name `glider`, lang `techguns.item.glider.name`, English **Glider**, slot Back, stack size 1. Its crafting parts, Glider Wing (meta 58, `techguns.item.gliderWing.name`) and Glider Backback (meta 59, `techguns.item.gliderBackback.name`, spelling as in the lang file), are plain items with no slot behavior.
2. **Obtaining:** crafting: Backback `hhh` / `shs` / `hhh`, Wing `sss` / `hhh` / `hhh`, Glider shapeless Backback + 2 Wings ([recipes.md](recipes.md) §2). Loot: barracks chest (weight 1). It is an ingredient of the Jetpack, and the Backback is an ingredient of the Jump Pack and the Anti Gravity Device.
3. **Numbers:** no durability. Glide: needs airborne, sneaking and motionY < -0.1; per-tick conversion capped at 0.08; air control +0.03 per tick. See §4.2.
4. **Behavior:** §4.2 (both sides run the same routine; a non-empty Jetpack reuses it). `isGliding` is cleared before every back-slot tick, so it is true only on ticks where the routine set it.
5. **Persistence and sync:** no NBT. `isGliding` is a transient property field (not saved); the client also clears it on `PlayerEvent.StopTracking`.
6. **UI:** no item-specific tooltip lines.
7. **Visuals:** icon `textures/items/glider.png` (32x32, avg #644C40, dominant #705030 19%, #303030 13%, #503030 10%); model [`ModelGlider`](rendering-and-models.md), renderer [`RenderGlider`](rendering-and-models.md#3-special-slot-rendering-on-the-player-rendereradditionalslots) (backpack always, wings only while airborne and sneaking); texture [`textures/armor/glider.png`](textures.md#10-back-gear-layout-d-6464) (64x64, avg #6E6957, dominant #303030 14%, #505030 14%, #707070 10%, 54% opaque).
8. **Audio:** `glider.loop` (file `glider_loop_stereo.ogg`, 9.41 s, 48000 Hz, stereo; vol 1.0, pitch 1.0, repeating, attached to the player, category PLAYER_EFFECT), started by the client when `isGliding` becomes true and stopped when it becomes false. The stereo format is unusual for a positional sound (inferred). The mono file `glider_loop.ogg` (3.02 s, 48000 Hz, mono) is unused.
9. **Relationships:** the crafting chain above; the Jetpack reuses the physics.
10. **Bugs:** [ARM-32](index.md#suspected-bugs) (unused sound file), [ARM-34](index.md#suspected-bugs).

### 9.7 Jetpack
1. **Identity:** `ItemJetpack`; registry `jetpack`; unlocalized `techguns.item.jetpack`; lang `techguns.item.jetpack.name`; English **Jetpack**; slot Back.
2. **Obtaining:** crafting `f_f` / `pgp` / `rmr` ([recipes.md](recipes.md) §2) with a Glider or any Jump Pack as `g`. No loot.
3. **Numbers:** max damage **5000**; camo count **4**; ammo Fuel Tank (`flamethrowerTank`), empty Fuel Tank (Empty); boost +0.15 motionY per tick up to 0.4; 1 damage per boosting tick (250 s per full charge); bonuses none.
4. **Behavior:** §4.3, summarized: reload when empty (both sides); the client's own-player jump-key watcher sends BOOST_START/STOP while `enableJetpack`; both sides apply the boost while `jumpkeyPressed` is true and charge remains, then run the glide routine. Hover, descend and forward-thrust code is unreachable.
5. **Persistence and sync:** damage, `camo`; player flag NBT `enableJetpack` (boolean); transient `jumpkeyPressed` is broadcast with [`PacketTGPlayerFieldSync`](../01-core/networking.md#11--packettgplayerfieldsync) field 0 (`FIELD_JUMPBUTTONPRESSED`) to the whole dimension; `FIELD_ENABLEJETPACK` goes only to the toggling player. `PacketTGKeybindPress` buttons 2/3 (boost start/stop) and 7 (toggle). Keybind: toggle **J** (`techguns.key.toggleJetpack`); boost uses the vanilla Jump key.
6. **UI:** tooltip `Slot: Back`, aqua `Reloads with: Fuel Tank`. GUI toggle tooltip `Toggle Jetpack` / "Enables your Jetpack" / " when you jump".
7. **Visuals:** icon `textures/items/jetpack.png` (64x64, avg #4F4D4B, dominant #505050 26%, #707070 21%, #101010 19%); model `ModelJetPack(0)` with stub wings, renderer [`RenderJetpack`](rendering-and-models.md#3-special-slot-rendering-on-the-player-rendereradditionalslots); textures by camo: 0 `jetPack.png` (avg #5D5A55), 1 `jetPack_bronze.png` (#60502B), 2 `jetPack_black.png` (#222020), 3 `jetPack_darkgreen.png` (#1F221C), all 64x64 and 41% opaque. FX: two `Jetpack` flame particles per rendered frame while boosting.
8. **Audio:** `jetpack.start` (3 files, each 1.29 s, 48000 Hz, mono), `jetpack.loop` (3 files, each 1.10 s, 48000 Hz, mono; repeating), `jetpack.stop` (3 files, each 1.40 s, 48000 Hz, mono); all vol 1.0, pitch 1.0, category PLAYER_EFFECT; triggers in §4.3 and §6.
9. **Relationships:** consumes Fuel Tanks; crafted from a Glider or Jump Pack; shares textures and camo names with the Jump Pack.
10. **Bugs:** [ARM-15](index.md#suspected-bugs), [ARM-16](index.md#suspected-bugs), [ARM-22](index.md#suspected-bugs), [ARM-32](index.md#suspected-bugs), [ARM-34](index.md#suspected-bugs).

### 9.8 Jump Pack
1. **Identity:** `ItemJumppack`; registry `jumppack`; unlocalized `techguns.item.jumppack`; lang `techguns.item.jumppack.name`; English **Jump Pack**; slot Back.
2. **Obtaining:** crafting `n_n` / `pbp` / `c_c` ([recipes.md](recipes.md) §2). No loot.
3. **Numbers:** max damage **1000**; camo count **4**; ammo Compressed Air Tank; jump boost **0.8**; free fall height **8.0** blocks; fall-damage reduction **0.2**; cost 1 per use (jump or damaging fall).
4. **Behavior:** §4.4. Charge is spent by the jump-event and fall-damage handlers through `getBonus`; the per-tick routine only reloads. Spending requires `enableJetpack` to be on; the FREEHEIGHT and non-consuming queries ignore it.
5. **Persistence and sync:** damage, `camo`; flag `enableJetpack`. Effects are broadcast with [`PacketSpawnParticleOnEntity`](../01-core/networking.md#14--packetspawnparticleonentity), [`PacketSpawnParticle`](../01-core/networking.md#13--packetspawnparticle) and [`PacketPlaySound`](../01-core/networking.md#9--packetplaysound) to players within 50 blocks.
6. **UI:** tooltip `Slot: Back`, aqua `Reloads with: Compressed Air Tank`, then `Jump height: +0.8`, `Fall damage: -20.0%`, `Fall height: -8.0`.
7. **Visuals:** icon `textures/items/jumppack.png` (32x32, avg #5F5D57, dominant #707070 28%, #909090 20%, #303030 16%); model `ModelJetPack(1)` (no wings), renderer `RenderJetpack`; the four jetpack textures of 9.7. FX `JumpPackBoost` (on entity) and `JumpPackFall` (at the feet), implemented in [04-projectiles-fx](../04-projectiles-fx/index.md). The renderer only spawns flames when `jumpkeyPressed` is set, which the jump pack never sets.
8. **Audio:** `effects.steamarmorJump2` (file `steamarmor_jump_d.ogg`, 1.21 s, 44100 Hz, mono), vol 1.0, pitch 1.0, category PLAYER_EFFECT, on each consuming jump. The fall effect is silent.
9. **Relationships:** needs a Glider Backback to craft; may replace the Glider in the Jetpack recipe.
10. **Bugs:** [ARM-22](index.md#suspected-bugs), [ARM-34](index.md#suspected-bugs).

### 9.9 Anti Gravity Device
1. **Identity:** `ItemAntiGravPack`; registry `antiGravPack`; unlocalized `techguns.item.antiGravPack`; lang `techguns.item.antiGravPack.name`; English **Anti Gravity Device**; slot Back.
2. **Obtaining:** crafting `tet` / `cac` / `mbm` ([recipes.md](recipes.md) §2). No loot.
3. **Numbers:** max damage **12000**; camo count **5**; ammo Nuclear Power Cell (empty: Nuclear Power Cell (Depleted)); consumption 1 per flying tick; fly-speed bonus **0.25**.
4. **Behavior:** §4.5. Reload is attempted one tick before it would run dry. Charge is not used in creative mode. The CREATIVE_FLIGHT bonus drives `allowFlying` in the generic flight code; on the first flying tick the ring FX is created (client) and the start sound is broadcast (server); when the pack is empty or removed, flight is revoked outside creative.
5. **Persistence and sync:** damage, `camo`; transient `wasFlying` and `gotCreativeFlightLastTick` properties; vanilla ability fields.
6. **UI:** tooltip `Slot: Back`, aqua `Reloads with: Nuclear Power Cell`, `Creative Flight`, `Flying Speed: +25.0%`.
7. **Visuals:** icon `textures/items/antiGravPack.png` (32x32, avg #5C717B, dominant #909090 15%, #707070 14%, #505050 13%); model [`ModelAntiGravPack`](rendering-and-models.md), renderer `RenderOxygenFlasks`; textures by camo: 0 `AntiGravPack.png` (avg #788A8E), 1 `AntiGravPack_bronze.png` (#8E8951), 2 `AntiGravPack_black.png` (#342626), 3 `AntiGravPack_darkGreen.png` (#273527), 4 `AntiGravPack_silver.png` (#9AB1B2), all 64x64 and 30% opaque. FX `AntiGravRing` (ring on flight start); `AntiGravRingGreen` is never spawned.
8. **Audio:** `items.antiGravStart` (2 files, each 1.81 s, 48000 Hz, mono), vol 1.0, pitch 1.0, category PLAYER_EFFECT, broadcast within 50 blocks on the tick flight begins.
9. **Relationships:** needs a Glider Backback, an Anti Gravity Core and a Nuclear Power Cell to craft.
10. **Bugs:** [ARM-23](index.md#suspected-bugs), [ARM-22](index.md#suspected-bugs), [ARM-32](index.md#suspected-bugs), [ARM-34](index.md#suspected-bugs).
