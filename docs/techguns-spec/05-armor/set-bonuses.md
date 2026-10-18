# Armor bonuses: aggregation, application and removal

Source: `java/techguns/TGArmorBonus.java`, [`GenericArmor.getArmorBonusForPlayer`](armor-catalog.md#1-class-hierarchy), [`PoweredArmor`](armor-catalog.md#9-power-system-poweredarmor), the special-slot items' `getBonus`. The consumers are [`events/TechgunsTickHandler.java`](../01-core/events.md#techgunstickhandler-fml-bus) (player tick), [`events/TechgunsEventhandler.java`](../01-core/events.md#techgunseventhandler-forge-bus) (jump, fall, break speed, attack) and [`packets/PacketTGKeybindPress.java`](../01-core/networking.md#7--packettgkeybindpress-cs). Those files belong to [01-core](../01-core/index.md); this page covers only their armor behavior.

**Techguns has no full-set bonuses.** Every bonus is a per-piece number, and the numbers from all sources are simply added together.

## 1. `TGArmorBonus` enum

| Constant | Meaning | Provided by |
|---|---|---|
| `SPEED` | movement speed multiplier bonus | armor |
| `JUMP` | added to motionY on jump | armor, Jump Pack |
| `FALLDMG` | fall-damage reduction fraction | armor boots, Jump Pack |
| `FREEHEIGHT` | blocks of fall distance ignored | armor boots, Jump Pack |
| `BREAKSPEED` | block break-speed bonus | armor |
| `BREAKSPEED_WATER` | extra break-speed bonus while in water or lava | armor |
| `GUN_ACCURACY` | spread reduction fraction (used by [02-guns](../02-guns/index.md)) | T2 Commando |
| `EXTRA_HEART` | extra max health points | Steam, Power armor |
| `NIGHTVISION` | enables night vision | Night Vision Goggles, Tactical Mask (no armor piece sets it) |
| `KNOCKBACK_RESISTANCE` | knockback resistance attribute | armor |
| `STEPASSIST` | step height 1.0 | Steam, Power, HEV, Exo boots |
| `OXYGEN_GEAR` | prerequisite for Oxygen Tanks | T2 Commando helmet, HEV helmet, Tactical Mask, Oxygen Mask |
| `WATER_ELECTROLYZER` | **never set and never read** | – |
| `COOLING_SYSTEM` | fire immunity | HEV chestplate |
| `SPEED_WATER` | extra speed while in water or lava | HEV pieces |
| `CREATIVE_FLIGHT` | allows creative-style flight | Anti Gravity Device |
| `FLYSPEED` | fly-speed bonus | Anti Gravity Device |

`GenericArmor.getBonus` and `PoweredArmor.getBonusUnpowered` cover every constant except `CREATIVE_FLIGHT` and `FLYSPEED`, which only back-slot items return.

## 2. Aggregation: `getArmorBonusForPlayer(player, type, consumePower)`

1. If the player has no Techguns extended properties, return 0.
2. For each of the 4 armor slots that holds a `GenericArmor`:
   - If the piece is **broken** (damage ≥ max−1), it contributes nothing.
   - **Non-powered piece:** add `getBonus(type)`.
   - **Powered piece with power > 0:** add the powered bonus. If `consumePower` is true and the bonus is > 0, deduct `applyPowerConsumptionOnAction(type)` from that piece (see armor-catalog.md §9).
   - **Powered piece with no power:** add the unpowered bonus (always 0 in practice).
3. **Face slot** (TG inventory slot 0): if there is an item and it is either the shared item, which is metadata-based, or its damage < maxDamage, add its [`ITGSpecialSlot.getBonus(type, stack, consume, player)`](special-equipment.md#1-interfaces-and-base-classes).
4. **Back slot** (slot 1): same rule as the face slot.
5. Return the sum.

## 3. Where each bonus is applied

### 3.1 Player tick (END phase, both client and server, every player)

The steps below run in this order every tick.

1. **Sync:** the server copies the face- and back-slot stacks into the player's DataWatcher (see extra-inventory.md).
2. **Gate:** `wearingTechgunsArmor` is true if any armor slot holds a `GenericArmor`.
3. **Remove old modifiers:** the three Techguns attribute modifiers are removed unconditionally every tick. This is what "removes" the bonuses when armor comes off:

   | Attribute | Modifier UUID | Name | Operation |
   |---|---|---|---|
   | movementSpeed | `5D8E53EB-DCFA-4121-B4DB-99BCAFA6B70B` | `TechgunsSpeedboost` | 2 (final multiply by 1 + x) |
   | maxHealth | `4CFA49EB-D215-498B-9CC9-4BD0D1350B1F` | `TechgunsHealthbonus` | 0 (add) |
   | knockbackResistance | `3441FC5D-F0B6-47F4-AFBB-DC5005670254` | `TechgunsKnockbackresistbonus` | 0 (add) |

4. **If wearing Techguns armor:**
   1. Run the powered-armor consumption and recharge tick (armor-catalog.md §9).
   2. **Cooling:** if the player is burning and COOLING_SYSTEM ≥ 1, extinguish them. This check does not consume power.
   3. **Speed:**
      - Start from `speed = SPEED`.
      - If the player is inside water or lava, add `SPEED_WATER`.
      - If speed > 0:
        - When sprinting, double it.
        - When using an item and holding a [`GenericGun`](../02-guns/gun-lifecycle.md) (i.e. aiming), add **5.0**. This offsets vanilla's item-use slowdown, but only for players who have some armor speed (bug [ARM-06](index.md#suspected-bugs)).
        - Apply the speed modifier with value = speed.
   4. **Health:** if EXTRA_HEART > 0, apply the health modifier with that value. The value is in health points (1 = half a heart).
   5. **Knockback:** if KNOCKBACK_RESISTANCE > 0, apply the knockback modifier. Vanilla clamps the attribute at 1.0, so the full Power Armor set (1.0) is immune to knockback while powered.
5. **Step assist** (client only, own player only):
   - Compute STEPASSIST and pass "has step assist" to the client proxy for the HUD icon (owned by [03-gun-visuals-audio](../03-gun-visuals-audio/index.md) and [01-core](../01-core/index.md)).
   - If the player toggle `enableStepAssist` is on and the bonus > 0, raise `stepHeight` to at least 1.0.
   - Otherwise, if `stepHeight` > 0.5, force it back to 0.5 (bug [ARM-07](index.md#suspected-bugs)).
6. **Night vision:**
   - Compute NIGHTVISION with `consumePower = enableNightVision` and pass "has night vision" to the client proxy (the HUD icon; bug [ARM-08](index.md#suspected-bugs)).
   - If the bonus > 0 and the toggle is on, the server applies Night Vision for 220 ticks with amplifier −1. It is refreshed every tick, so it never flickers.
7. **Creative flight:** if CREATIVE_FLIGHT > 0:
   - Set `allowFlying = true` and `gotCreativeFlightLastTick = true`.
   - On the tick flight begins (`isFlying` and not `wasFlying`):
     - Client: spawn FX `AntiGravRing` on the player ([04-projectiles-fx](../04-projectiles-fx/index.md)).
     - Server: send `techguns:items.antiGravStart` (volume 1, pitch 1, category PLAYER_EFFECT) to everyone within 50 blocks.
   - Track `wasFlying`.

   Otherwise, if the player had flight last tick and is not in creative, set `allowFlying` and `isFlying` to false. Then clear both flags.
8. **Fly speed** (client only, own player only; default 0.05):
   - If FLYSPEED > 0, set the fly speed to (1 + bonus) × 0.05 when `enableJetpack` is on, and to 0.05 when it is off.
   - With no bonus, reset to 0.05, but only if creative flight was granted last tick.
9. **Auto-feeder:** owned by [01-core](../01-core/index.md).
10. **Special-slot ticks:** call `onPlayerTick` on the face item, then reset `props.isGliding = false` and call `onPlayerTick` on the back item. Finally `handlePlayerGliding` starts or stops the glider loop sound (special-equipment.md).

### 3.2 Event handlers

| Event | Behavior |
|---|---|
| `LivingJumpEvent` (players) | `motionY += JUMP` with consume = true. Powered leggings pay 5 power and play the steam jump sound. The Jump Pack pays 1 durability. |
| `LivingFallEvent` (players) | Compute `fall = FALLDMG`, with consume = true only if the distance > 3. Set `reduction = 1 − fall` when fall < 1, otherwise 0. Subtract `FREEHEIGHT` (no consumption) from the distance, flooring at 0, then multiply the distance by `reduction`. |
| `BreakSpeed` (normal priority) | `newSpeed *= (1 + BREAKSPEED) × (1 + BREAKSPEED_WATER if in water or lava, otherwise 1)`. Both lookups use consume = true, but powered armor has no cost for these. A separate highest-priority handler cancels block breaking with left-click guns ([02-guns](../02-guns/index.md)). |
| `LivingAttackEvent` (HIGH) | If the victim is a player and the source is lava, inFire or onFire (after any melee-weapon source substitution): when COOLING_SYSTEM ≥ 1 (consume = true, so the HEV chest pays 1 power), cancel the event. The remaining branch hands TG damage to the damage system ([01-core](../01-core/index.md)). |
| `GenericGun` shooting ([02-guns](../02-guns/index.md)) | Spread multiplier = 1 − GUN_ACCURACY (no consumption). |
| [`ItemScubaTanks`](special-equipment.md#41-oxygen-tanks-itemscubatanks) tick | Needs OXYGEN_GEAR > 0 (special-equipment.md). |
| Night-vision toggle packet | Plays the `on` sound only if NIGHTVISION > 0. |

## 4. Toggles that gate bonuses (player properties, synced by 01-core packets)

| Field | Default | Toggled by | Gates |
|---|---|---|---|
| `enableNightVision` | false | key N (`techguns.key.toggleNightvision`) or GUI toggle 1 | night-vision potion; drains goggle/mask charge |
| `enableStepAssist` | true | key V (`techguns.key.toggleStepassist`) or GUI toggle 3 | step assist |
| `enableJetpack` | true | key J (`techguns.key.toggleJetpack`) or GUI toggle 4 | Jetpack boost, Jump Pack jump and fall effects, Anti-Grav fly-speed bonus |
| `enableSafemode` | – | key B / GUI toggle 2 | owned by [02-guns](../02-guns/index.md) / [01-core](../01-core/index.md) |
| `showTGHudElements` | – | GUI toggle 0 | HUD icons ([01-core](../01-core/index.md) / [03](../03-gun-visuals-audio/index.md)) |

The server-side toggle handling and its sounds are covered in special-equipment.md §8.

## 5. Notes for implementers

- Bonuses are summed linearly with no caps except vanilla attribute clamps. Full Exo is +80 % speed (+160 % while sprinting), jump +0.6 motionY.
- All bonus attribute modifiers are re-created every tick, so removal is implicit. Unequipping armor, depleting power, or reaching the broken state all take effect on the next tick.
- Night vision lingers up to 11 s after the source goes away, because the potion is not removed. Toggling night vision **off** removes the Night Vision effect even if it came from a vanilla potion.
