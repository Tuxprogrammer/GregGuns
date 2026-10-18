# 02-guns: damage system (`java/techguns/damagesystem/`)

Files: `DamageType.java`, `TGDamageSource.java`, `DamageSystem.java`, `TGArmorMaterial.java`, `INpcTGDamageSystem.java`, `IToolTGDamageSystem.java`.

Related code owned elsewhere:

* the `LivingAttackEvent` hook in `events/TechgunsEventhandler.java` (01-core), summarised in §4 because it is the entry point;
* `GenericArmor.getProperties` (05-armor), which applies player armour;
* projectile `onImpact` code (04), which calls `getDamageFactor` and `attackEntityFrom`;
* the material instances in `TGArmors.java` (05-armor).

## 1. Damage types (`DamageType` enum)

| Constant | Intended for | Tooltip colour | Lang key → English | Vanilla flags set on a new TGDamageSource |
|---|---|---|---|---|
| PHYSICAL | melee hits | dark gray | `techguns.TGDamageType.PHYSICAL` → Physical | none |
| PROJECTILE | bullets, arrows | gray | `…PROJECTILE` → Projectile | projectile |
| FIRE | flamethrower, fire | red | `…FIRE` → Fire | **magic** (deliberately not fire, so Fire Resistance doesn't grant immunity) |
| EXPLOSION | rockets, TNT | dark red | `…EXPLOSION` → Explosion | explosion |
| ENERGY | lasers, blasters, magic | dark aqua | `…ENERGY` → Energy | magic |
| POISON | gas, bio | dark green | `…POISON` → Poison | magic |
| UNRESISTABLE | starve, fall, suffocate… | black | `…UNRESISTABLE` → **missing** | bypasses armour + absolute |
| ICE | – | aqua | `…ICE` → Ice | magic |
| LIGHTNING | tesla | yellow | `…LIGHTNING` → Lightning | magic |
| RADIATION | death ray | green | `…RADIATION` → Radiation | magic |

`toString()` returns the colour code plus the translated name. It is used in tooltips (client-side translation).

## 2. `TGDamageSource` (extends vanilla `EntityDamageSource`)

Fields and defaults:

| Field | Default | Meaning |
|---|---|---|
| `attacker` | null | The real shooter. `getEntity()` returns it when set, otherwise the damaging entity. |
| `damageSourceEntity` (inherited) | – | The directly damaging entity (projectile or player). `getSourceOfDamage()` returns it. |
| `damageType` | – | `DamageType` |
| `deathType` | – | `DeathType` (DEFAULT, GORE, LASER, BIO, … owned by 01/10). Drives special death effects. |
| `goreChance` | 0.5 | On a killing blow, if a random float exceeds `goreChance`, the death type falls back to DEFAULT. |
| `armorPenetration` | 0.0 | 0..1 |
| `ignoreHurtresistTime` | false | If true, the hit ignores and doesn't set invulnerability frames. |
| `knockbackMultiplier` | 1.0 | 0 means no knockback. Any value > 0 means vanilla knockback (see §5). |
| `wasConverted` | false | True when built from a vanilla DamageSource. |

Factory helpers (the name becomes the death-message key `death.attack.<name>`):

| Factory | Name | Type | ignoreHurtresist |
|---|---|---|---|
| `causeBulletDamage` | `tg_bullet` | PROJECTILE | yes |
| `causeExplosionDamage` | `tg_explosion` | EXPLOSION | no |
| `causePoisonDamage` | `tg_poison` | POISON | yes |
| `causeFireDamage` | `tg_fire` | FIRE | yes |
| `causeEnergyDamage` | `tg_energy` | ENERGY | yes |
| `causeRadiationDamage` | `tg_rad` | RADIATION | yes |
| `causeLightningDamage` | `tg_lightning` | LIGHTNING | yes |
| `getKnockbackDummyDmgSrc` | `tg_knockback` | PHYSICAL, DeathType DEFAULT | no |

Other names constructed directly elsewhere: `player` (gun melee, PHYSICAL), `chainsaw` (PHYSICAL), `powerhammer` (PHYSICAL) and `tg_sonic` (PHYSICAL).

`setNoKnockback()` sets the multiplier to 0. `setKnockback(m)` sets it to m. `hasKnockback()` is true when the multiplier is > 0.

**Death message** (`func_151519_b`, the vanilla death-message hook): the killer name comes from `attacker`, falling back to the damaging entity, falling back to the victim's own name if both are null. If the attacker is a living entity holding an item with a custom display name and the key `death.attack.<name>.item` exists, it uses that key with (victim, killer, item). Otherwise it uses `death.attack.<name>` with (victim, killer).

Death-message lang keys present:

| Key | English |
|---|---|
| `death.attack.tg_bullet` | %s was shot by %s |
| `death.attack.tg_bullet.item` | %s was shot by %s with %s |
| `death.attack.tg_explosion` | %s was blown up by %s |
| `death.attack.tg_explosion.item` | %s was blown up by %s with %s |
| `death.attack.tg_fire` | %s was burned to death by %s |
| `death.attack.tg_fire.item` | %s was burned to death by %s with %s |
| `death.attack.tg_poison` | %s was poisoned by %s |
| `death.attack.tg_poison.item` | %s was poisoned by %s with %s |
| `death.attack.powerhammer` | %s was dismembered by %s with a powerhammer |
| `death.attack.powerhammer.item` | %s was dismembered by %s with %s |
| `death.attack.tg_lightning` | %s was electrocuted by %s |
| `death.attack.tg_lightning.item` | %s was electrocuted by %s with %s |
| `death.attack.tg_energy` | %s was melted by %s |
| `death.attack.tg_energy.item` | %s was melted by %s with %s |
| `death.attack.chainsaw` | %s was dismembered by %s with a chainsaw |
| `death.attack.tg_sonic` | %s was shreddered by %s's sonic weapon |
| `death.attack.tg_rad` | %s was radiated by %s |

`tg_knockback` has no key. That source only deals 0.01 damage, so a kill by it is very unlikely.

**Converting a vanilla DamageSource** (`getFromGenericDamageSource`): a TGDamageSource is returned unchanged. Anything else becomes a new TGDamageSource with the same name and the vanilla source's *direct* entity. `attacker` stays null and `wasConverted` is set to true (GUN-37). Type mapping, first match wins:

1. explosion → EXPLOSION
2. magic → ENERGY
3. fire → FIRE
4. projectile → PROJECTILE
5. name `wither` → POISON
6. can harm in creative, or name in {`inWall`, `drown`, `starve`, `fall`, `outOfWorld`} → UNRESISTABLE
7. otherwise → PHYSICAL

Vanilla flags are copied: creative-harm, absolute, explosion, fire, magic, projectile, unblockable. The difficulty-scaled flag is mapped to **absolute** instead (GUN-36). A second constructor overload for `EntityDamageSource` does the same but starts from `getEntity()`; it is only reached with a statically typed `EntityDamageSource`.

## 3. Interfaces

* `INpcTGDamageSystem` (implemented by `GenericNPC`, `AlienBug`, `AlienBugDomesticated` and `AttackHelicopter`, documented in 10):
  * `getTotalArmorAgainstType(src)`: the entity's armour value for the source's damage type;
  * `getPenetrationResistance(src)`: subtracted from the source's penetration.

  Any entity implementing it receives **all** damage through the Techguns pipeline, including vanilla damage.
* `IToolTGDamageSystem` (implemented by `TGSword` and `TGPickaxe`, 06): `getDamageSource(original)` turns the vanilla melee source into a TGDamageSource when the attacker holds that tool.

## 4. Entry point: `LivingAttackEvent` (HIGH priority)

1. If the source's direct entity is a living entity holding an `IToolTGDamageSystem` item, swap the source for the tool's TGDamageSource.
2. If the victim is a player, the source is lava, inFire or onFire, and the player's armour COOLING_SYSTEM bonus is ≥ 1, cancel the event. No damage is taken.
3. Otherwise, if the (possibly swapped) source is a TGDamageSource **or** the victim implements `INpcTGDamageSystem`, cancel the vanilla event and run:
   * `DamageSystem.onLivingAttackPlayer` for players;
   * `DamageSystem.onLivingAttackEntityLivingBase` for everything else.
4. All other damage stays vanilla.

## 5. Damage pipeline (`DamageSystem.java`)

### 5.1 `onLivingAttackPlayer(player, src, dmg)`

Re-implements the player-specific checks from vanilla `EntityPlayer.attackEntityFrom`:

* return if the player is invulnerable, or in creative/no-damage mode and the source can't harm creative players;
* reset `entityAge` to 0; return if already dead;
* wake a sleeping player (server);
* if the source is difficulty-scaled: Peaceful → 0, Easy → `d/2 + 1`, Hard → `d × 1.5`;
* return if the damage is 0;
* add `d × 10` (rounded) to the damage-taken statistic;
* continue with `onLivingAttackEntityLivingBase`.

### 5.2 `onLivingAttackEntityLivingBase(elb, src, dmg)` (server only)

Re-implements vanilla `EntityLivingBase.attackEntityFrom`, with these TG differences:

1. Convert the source to a TGDamageSource. Return if invulnerable, on the client, or dead. Reset `entityAge`. Return if the source is fire damage and the target has Fire Resistance. TG fire sources aren't flagged as fire, so this never stops them.
2. Anvil or falling-block damage with a helmet worn: damage the helmet by `d×4 + rand×d×2` and multiply damage by 0.75.
3. Set `limbSwingAmount = 1.5`.
4. **Invulnerability frames.** Let `flag` mean "this is a fresh hit".
   * If the source doesn't ignore i-frames and `hurtResistantTime > max/2`:
     * if `d ≤ lastDamage`, stop (no damage);
     * otherwise apply the **full** `d` (vanilla applies only `d − lastDamage`, GUN-33) and set `lastDamage = d`. Not a fresh hit.
   * Otherwise: `prevHealth = health`. If the source doesn't ignore i-frames, set `lastDamage = d`, `hurtResistantTime = max`, and `hurtTime = maxHurtTime = 10` after damage. Apply `d`. Fresh hit.

   Sources that ignore i-frames (most TG projectiles) never set or check i-frames. That is why every pellet of a shotgun lands, and why the minigun hits every tick.
5. **Apply** `d`: players go through `damageEntityPlayer` (§5.3), everything else through `damageEntity` (§5.4).
6. `attackedAtYaw = 0`. If the source's entity (the attacker, or the projectile when no attacker is set) is living, set it as revenge target. If it is a player, set `recentlyHit = 100` and `attackingPlayer` (for XP and player-kill loot). If it is a tamed tameable, set `recentlyHit = 100` and `attackingPlayer = null`.
7. On a fresh hit: send entity status 2 (hurt animation), mark the target as attacked (except for drowning), and apply knockback if the source has knockback and an entity. The knockback direction is from the attacker's position (the shooter, not the projectile), and it calls vanilla `knockBack(entity, d × multiplier, dx, dz)`. Vanilla 1.7.10 ignores the strength argument (fixed 0.4), so in practice the multiplier is a 0 / non-zero switch (GUN-38). Without knockback, `attackedAtYaw` is randomly 0 or 180.
8. If health ≤ 0: play the death sound (fresh hits only), roll gore (see `goreChance`), then `onDeath(src)`. Otherwise play the hurt sound (fresh hits only).

Reflection is used for the private vanilla fields and methods involved: `entityAge`, `lastDamage`, `recentlyHit`, `attackingPlayer`, `setBeenAttacked`, `getDeathSound`, `getHurtSound`, `getSoundVolume`, `getSoundPitch`, `applyPotionDamageCalculations`, `damageArmor`.

### 5.3 `damageEntityPlayer(player, src, d)`

1. Return if invulnerable. Fire Forge `LivingHurtEvent`; return if the result is ≤ 0.
2. If the type is PHYSICAL and the player is blocking: `d = (1 + d) × 0.5`.
3. `ArmorProperties.ApplyArmor` (Forge) runs over the four armour slots.
   * Techguns armour is `ISpecialArmor`, so `GenericArmor.getProperties` supplies the ratio (see the formula below).
   * Vanilla armour pieces use their own vanilla ratio for **every** non-unblockable TG type (GUN-45).
4. Return if ≤ 0. Apply potion calculations (Resistance and similar).
5. Absorption hearts absorb first. If anything remains: add exhaustion (`src.getHungerDamage()`), lower health, and record in the combat tracker.

**Player armour formula** (`GenericArmor.getProperties` in 05, using `DamageSystem.getReduction` and `calcPenetration`). For each worn TG piece that isn't nearly broken, with `A` = that piece's armour value against the damage type (`TGArmorMaterial.getArmorValueSlot`):

* if `A > 0`:
  * `pen = src.armorPenetration`;
  * if `pen > 0`: `pen = clamp(calcPenetration(player, pen − piecePenetrationResistance), 0, 1)`, written back into the source (GUN-39);
  * `ratio = (1 − pen) × A / 25`. This is `getReduction`: the share of damage blocked. The penetrating part `d×pen` passes through, and the blocked part `d×(1−pen)` is scaled by `(25 − A) × 0.04`.
* Forge sums the ratios of all pieces (capped at 1) and absorbs up to each piece's remaining durability.

`calcPenetration(target, pen, src)`: if the target is a player, multiply `pen` by `TGConfig.armorPenPvP` (config `ArmorPenetrationMultiplierPvP`, default 1.0) when the attacker is a player, otherwise by `TGConfig.armorPenNPCtoPlayer` (`ArmorPenetrationMultiplierNPCtoPlayer`, default 1.0). Clamp to 0..1.

### 5.4 `damageEntity(elb, src, d)` (non-players)

1. Return if invulnerable. Forge `LivingHurtEvent`; return if ≤ 0.
2. `d = getDamage(src, d, elb)` (formula below). Then the potion calculations.
3. Absorption first. Then lower health and record in the combat tracker. A second absorption subtraction follows (GUN-35).

**`getDamage(src, d, target)`:**

```
if type == UNRESISTABLE: return d
if target is INpcTGDamageSystem:
    armor = target.getTotalArmorAgainstType(src)            (not clamped, GUN-40)
    pen   = clamp(src.armorPenetration − target.getPenetrationResistance(src), 0, 1)
else:
    armor = clamp(target.getTotalArmorValue(), 0, 24)       (GenericArmor.MAX_ARMOR = 24)
    armor = typeAdjust(target, armor, type)
    pen   = 0                                               (penetration ignored, GUN-34)
target.damageArmor(d)            (vanilla armour wear, raw damage)
return d × pen + d × (1 − pen) × (25 − armor) / 25
```

`typeAdjust` (`getArmorAgainstDamageTypeDefault`):

| Type | Effective armour |
|---|---|
| PHYSICAL, PROJECTILE | armor |
| EXPLOSION, ENERGY, ICE, LIGHTNING | round(armor × 0.5) |
| FIRE | armor × 2 if the target is fire-immune, else armor × 0.5 |
| POISON, RADIATION, UNRESISTABLE | 0 |

### 5.5 Damage factors (`getDamageFactor(attacker, target)`)

Called by projectiles (04) and explosions (`ProjectileExplosion`) before `attackEntityFrom`. The result multiplies the projectile damage.

| Attacker → target | Factor | Config key (category "Damage Factors", file 01) | Default |
|---|---|---|---|
| player → player | `damagePvP` if server PvP is on, else **0** | `DamagePvP` (0–100) | 0.5 |
| NPCTurret → player | `damageTurretToPlayer` | `DamageTurretToPlayer` | 0.5 |
| other (incl. null) → player | `damageFactorNPC` | `DamageFactorNPC` | 1.0 |
| player → non-player | 1.0 | – | – |
| non-player → non-player | `damageFactorNPC` | `DamageFactorNPC` | 1.0 |

### 5.6 Other helpers

* `getTotalArmorAgainstType(player, type)`: sums `GenericArmor.getArmorValue` over worn TG pieces, plus the vanilla reduction amount for vanilla `ItemArmor` (PHYSICAL only). It is used by the Techguns inventory GUI to show per-type armour (01/05).
* `getAdditionalReduction(target, type)`: returns 0.5 for fire against fire-immune targets, otherwise 1. Never called.
* `damageEntity(EntityClientPlayerMP, …)`: subtracts health directly. Never called. Its signature uses a client-only class (GUN-41).

### 5.7 Worked example

M4 bullet (8 damage, 15% pen) against a zombie wearing full iron armour (15 armour points):

* `getDamage` uses pen 0 (vanilla mob) and armour 15 (PROJECTILE): 8 × (25 − 15)/25 = **3.2**.
* The same bullet against a `GenericNPC` with 15 projectile armour and penetration resistance 0: 8 × 0.15 + 8 × 0.85 × 10/25 = **3.92**.
* Against a player in TG armour with total projectile value 15 and per-piece penetration resistance 0: the ratio sums to (1 − 0.15) × 15/25 = 0.51 of the damage blocked, so **3.92** is taken (if the penetration had not compounded across pieces, see GUN-39).

## 6. `TGArmorMaterial`

The data class behind every Techguns armour set. Instances and their values are in `TGArmors.java` (05-armor).

Fields: `name`, `baseDur`, `enchantability`, per-type totals `armorPhys`, `armorProjectile`, `armorExplosion`, `armorEnergy`, `armorFire`, `armorLightning`, `armorIce`, `armorPoison`, `armorRadiation` (all floats, default 0), and `penetrationResistance` (default 0).

* Armour split per slot (`factorHead/Chest/Legs/Boots`): defaults 0.25 / 0.30 / 0.25 / 0.20 (sum 1.0). The slot value is `factor × typeTotal`. Change it with `setPartArmorSplit(helm, chest, legs, boots)`.
* Durability split per slot (`durHead/Chest/Legs/Boots`): defaults 0.25 each. Slot durability is `round(dur × 55 × baseDur)`, where 55 is the sum of the vanilla per-slot durability factors. Change it with `setPartDurabilitySplit`.
* Constructors:
  1. **Full**: name, baseDur, enchant, then explicit phys, proj, fire, exp, energy, poison, ice, light, rad.
  2. **Common**: name, baseDur, enchant, phys, proj, elemental. Fire, explosion, energy, ice and lightning are set to `elemental`. Poison and radiation stay 0.
  3. **Single**: name, baseDur, enchant, armor. Phys and proj are `armor`. Fire, explosion, energy, ice and lightning are `0.75 × armor`. Poison and radiation are 0.
* Fluent setters exist for every type (`setArmorPhys`, `setArmorProjectile`, `setArmorExplosion`, `setArmorEnergy`, `setArmorFire`, `setArmorLightning`, `setArmorIce`, `setArmorPoison`, `setArmorRadiation`) and for `setPenetrationResistance`.
* `getArmorValueTotal(type)` returns the type total (UNRESISTABLE → 0). `getArmorValueSlot(slot, type)` returns factor × total (slot 0 = helmet, 1 = chest, 2 = legs, 3 = boots). `getArmor(ItemArmor, type)` is shorthand for the item's slot.
* `createVanillaMaterial()` registers a vanilla `ArmorMaterial` (via Forge EnumHelper) with the same name, baseDur and enchantability. Its per-slot reduction amounts are the rounded PHYSICAL slot values, so vanilla armour bars and `getTotalArmorValue` reflect physical armour.
