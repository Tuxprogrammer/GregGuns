# Damage sources, hazmat and other mods' armor

- **Techguns baseline:** [damage-system.md § entry point](../../techguns-spec/02-guns/damage-system.md#4-entry-point-livingattackevent-high-priority), [damage-system.md § pipeline](../../techguns-spec/02-guns/damage-system.md#5-damage-pipeline-damagesystemjava), and the bugs GUN-33 to GUN-45 in [index.md](../../techguns-spec/02-guns/index.md#suspected-bugs)
- **Design records:** [0004 Damage model](../../design/0004-damage-model.md)
- **Pack mods:**
  - GregTech (`gregtech`, `5.09.54.205`), including GT++ (`gtPlusPlus` packages in the same repository)
  - Industrial Craft 2 (`IC2`, `2.2.2.828`), API only
  - GTNH Core Mod (`dreamcraft`, `2.9.84`)
- **Roadmap:** M1 (combat core), M9 (balance); D4 gate

## What changes from Techguns

[0004](../../design/0004-damage-model.md) keeps the Techguns damage model (types, penetration, resistances) and fixes its known flaws. This sheet covers only the parts where the model meets the pack.

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| A `LivingAttackEvent` handler at HIGH priority **cancels** the event and re-implements vanilla `attackEntityFrom` and `damageEntity` by reflection | The model must not stop other mods seeing the hit (see below) | Pack mods listen to `LivingAttackEvent` and a canceled event never reaches them. |
| Fire damage is not flagged as fire, so Fire Resistance never applies (damage-system § 5.2, step 1) | Fire-type damage sets the vanilla fire flag | Vanilla and mods key off `isFireDamage()`. |
| Vanilla armor on players blocks every Techguns type at full value, but on mobs gives half or nothing (GUN-45) | Vanilla and other mods' armor act as vanilla armor on **both** players and mobs ([0004](../../design/0004-damage-model.md)) | Consistency. |
| No link between Techguns damage types and GT's hazard system | The secondary effects of fire, ice, radiation and poison respect GT hazmat | GT players expect hazmat to matter. |

## Integration points

### GT5-Unofficial: its damage sources and what they do

- **Source:** [GTDamageSources.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/damagesources/GTDamageSources.java#L13-L111):
  - `getHeatDamage()` is source `"steam"`, flagged as fire and difficulty-scaled (L35, L73-L87).
  - `getFrostDamage()` is source `"frost"`, difficulty-scaled (L39, L58-L71).
  - `getExplodingDamage()` is source `"exploded"` and is allowed in creative, bypasses armor and is **absolute** (L27, L104-L115).
  - `getElectricDamage()`, `getRadioactiveDamage()` and `getNukeExplosionDamage()` return IC2's `Info.DMG_ELECTRIC`, `DMG_RADIATION` and `DMG_NUKE_EXPLOSION` (L15-L25). The IC2 field declarations are in `ic2/api/info/Info.java` of the API jar.
  - Each GT source supplies its own death message (`GT5U.die.*`).
- **GregGuns does:** does **not** reuse these. GT's `exploded` source ignores armor and potions, which is wrong for a rocket. GregGuns defines its own source class, one `damageType` string per Techguns damage type (for example `gregguns.energy`), with its own `death.attack.<damageType>` lang keys.
- **Pack-side change:** none.

### GT5-Unofficial: hazmat protection

- **Source:**
  - The hazards are `BIOLOGICAL, FROST, HEAT, RADIOLOGICAL, ELECTRICAL, GAS, SPACE`: [Hazard.java#L10-L18](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/Hazard.java#L10-L18).
  - Armor opts in by implementing `IHazardProtector.protectsAgainst(...)`: [IHazardProtector.java#L8-L21](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/IHazardProtector.java#L8-L21).
  - `HazardProtection.isWearingFullHazmatAgainst(entity, hazard)` is true when all four armor slots protect, or one slot protects "fully": [HazardProtection.java#L44-L60](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/HazardProtection.java#L44-L60). The GT Hazmat enchantment also counts ([L95-L101](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/HazardProtection.java#L95-L101)).
  - GT applies this itself: `applyHeatDamage`, `applyFrostDamage`, `applyElectricityDamage` and `applyRadioactivity` all skip the target when it wears full hazmat for the matching hazard: [GTUtility.java#L1982-L2060](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1982-L2060). Radioactivity also skips undead and arthropods.
- **GregGuns does:**
  - Direct damage follows [0004](../../design/0004-damage-model.md): armor value, penetration, resistance.
  - The **secondary effect** of a hit (burning, slowness from ice, the radiation potion set, the Bio Gun's poison) is skipped when the target wears full GT hazmat for the matching hazard. The mapping is fire to `HEAT`, ice to `FROST`, radiation to `RADIOLOGICAL`, poison to `BIOLOGICAL`, lightning and Tesla to `ELECTRICAL`.
  - The call is `HazardProtection.isWearingFullHazmatAgainst(entity, hazard)`. It is a hard dependency on GT, so no guard is needed.
  - GregGuns armor sets that should count as hazmat implement `IHazardProtector`. That is decided per set in [05-armor](../05-armor/index.md).
- **Pack-side change:** none. The pack's own list of hazardous items is `config/GTNewHorizons/HazardousItems.xml`. It does not affect gun damage.

### Other mods' armor: the Forge `ISpecialArmor` contract

- **Source:** GT's own armor implements Forge `ISpecialArmor`. [MechArmorBase.java#L383-L396](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L383-L396) returns a force-field absorb when active, then **`(0, 0, 0)` for `source.isUnblockable()`**, otherwise its damage-reduction ratio. The older electric armor does the same by ratio: [ItemEnergyArmor.java#L283-L292](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/ItemEnergyArmor.java#L283-L292). `MechArmorBase` also spends charge in `damageArmor`.
- **Forge `ArmorProperties.ApplyArmor` and the order of vanilla's damage steps:** Forge is not in the mod index, so there is no pinned source. The Techguns spec describes the same call ([damage-system.md § 5.3](../../techguns-spec/02-guns/damage-system.md#53-damageentityplayerplayer-src-d)). The statements below come from that and from general Forge 1.7.10 behavior; check them in the M1 unit tests.
- **GregGuns does:**
  - Armor that is not GregGuns armor is handled by the vanilla path: Forge `ApplyArmor` over the four slots, then `damageArmor` on each, for **every** non-unblockable damage type, on players and on mobs alike. That is the fix for GUN-45.
  - A source that must ignore armor (the `UNRESISTABLE` type) is marked unblockable (`setDamageBypassesArmor`). GT's armor returns zero reduction for it, as shown above.
- **Pack-side change:** none.

### GT++ and other `LivingAttackEvent` listeners

- **Source:** GT++ baubles cancel damage by matching the source's `damageType` string against a list: [PlayerAttackEventHandler.java#L18-L36](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtPlusPlus/core/handler/events/PlayerAttackEventHandler.java#L18-L36) with the list in `BaseBauble.getDamageNegations()`. The dreamcraft Oven Glove cancels hot-item damage the same way: [OvenGlove.java#L144-L151](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/item/baubles/OvenGlove.java#L144-L151).
- **Effect on the Techguns approach:** a canceled `LivingAttackEvent` is not delivered to later listeners unless they ask for canceled events. Techguns cancels at HIGH priority and applies damage itself, so listeners like these never see its hits. (Forge and FML event-bus behavior; no pinned source.)
- **GregGuns does:** keeps the hit inside vanilla's `attackEntityFrom`, so `LivingAttackEvent` and `LivingHurtEvent` listeners run. The formulas stay in the pure-logic class under `com.gregguns.combat` that [0004](../../design/0004-damage-model.md) requires. How the model plugs into the hit is an open question below.
- **Pack-side change:** none.

## Requirements

- **GGS-02-17.** GregGuns damage is delivered with a `DamageSource` subclass that has one `damageType` string per damage type, prefixed `gregguns.`.
- **GGS-02-18.** A hit from GregGuns is visible to other mods' `LivingAttackEvent` and `LivingHurtEvent` listeners. GregGuns does not cancel `LivingAttackEvent` to run its own copy of the vanilla damage routine.
- **GGS-02-19.** Fire-type damage sets the vanilla fire flag. Explosion-type sets the explosion flag. Projectile-type sets the projectile flag.
- **GGS-02-20.** Against armor that is not GregGuns armor, players and mobs get the same vanilla result. Forge `ApplyArmor` and `damageArmor` run once per hit (fixes GUN-45; penetration is also applied once per hit, as [0004](../../design/0004-damage-model.md) already requires for GUN-39).
- **GGS-02-21.** The `UNRESISTABLE` type is unblockable and skips armor. No other type is.
- **GGS-02-22.** The secondary effect of fire, ice, radiation, poison and electric hits is skipped when `HazardProtection.isWearingFullHazmatAgainst` is true for the mapped hazard. Direct damage is not.
- **GGS-02-23.** Each `damageType` has a `death.attack.<damageType>` lang key in every shipped language.

## Tests

- GGS-02-17, 19, 21: unit test on the source class (name, flags).
- GGS-02-20: unit test with a stub `ISpecialArmor` and a plain vanilla armor set. Same result for player and mob. Also the worked example from [damage-system.md § 5.7](../../techguns-spec/02-guns/damage-system.md#57-worked-example).
- GGS-02-18: `/gregguns test` scenario with a test `LivingAttackEvent` listener that records the source type, hit by each gun type.
- GGS-02-22: `/gregguns test` scenario with a target in a full hazmat set (a stub `IHazardProtector`) and one without.
- GGS-02-23: lang-file check in the build.

## Settled in M1 (implementation notes and deviations)

Code: `com.gregguns.combat`. Decision: [0009 #3](../../design/0009-gtnh-integration-defaults.md), option B.

- **How the model plugs into the hit (settled).** `CombatEvents.onLivingHurt` (LOW priority) runs inside vanilla's `damageEntity`. `LivingAttackEvent` is never canceled.
- **Evidence for the armor order (Forge 10.13.4.1614 patches `EntityLivingBase.java.patch`, `EntityPlayer.java.patch`, and `ISpecialArmor.java`).**
  - `damageEntity` calls `ForgeHooks.onLivingHurt` **first**, then armor. Players run `ArmorProperties.ApplyArmor(this, armorInventory, source, amount)`; every other entity runs `applyArmorCalculations`, which does nothing when `source.isUnblockable()`.
  - `ApplyArmor` skips vanilla `ItemArmor` for unblockable sources and leaves `ISpecialArmor` to decide (GT's `MechArmorBase` returns zero). It also wears the pieces.
  - So the event amount is **pre-armor**. The handler cannot leave armor to vanilla, because armor must see only the non-penetrating share. It applies armor to that share itself (`ApplyArmor` for players, vanilla's `(25 - armor) / 25` for mobs), sets the event amount to `d * pen + armored share`, then marks the source "armor settled". `RangedDamageSource` and `DirectDamageSource` report `isUnblockable()` as true from then on, so vanilla's own armor step is skipped and armor is applied exactly once (checked by `combat.player`: each piece is worn once).
  - Potion/enchantment reductions and absorption still run after, once, as in vanilla. Vanilla's sword-block halving for players runs after our event, so it acts on the already-armored amount. Accepted.
- **Penetration travels with the source** (`HitProperties.penetration()`), is clamped to 0..1, and is computed once per hit by `DamageModel.effectivePenetration(base, scale, totalResistance)`. Resistance is the sum over worn `TypedArmor` pieces plus the target's own `TypedProtection`.
- **Vanilla armor is plain vanilla (GUN-45).** Techguns' per-type halving of vanilla armor on mobs (`typeAdjust`) is **dropped**. A mob's armor is `getTotalArmorValue()` clamped to 0..24; players use `ApplyArmor`. Every blockable type sees the same result. Per-type values exist only for GregGuns armor (`TypedArmor`, M3) and NPCs (`TypedProtection`, M7). Consequence: poison and radiation are reduced by vanilla armor too (GGS-02-21 allows only `UNRESISTABLE` to skip armor).
- **Worked example 5.7 changes for vanilla mobs.** The bullet against a zombie in iron now takes **3.92**, not 3.2: penetration applies to every living target (GUN-34). A zombie also carries 2 base armor points in 1.7.10, so a real zombie in iron has 17.
- **Invulnerability frames (GUN-33).** Vanilla's `attackEntityFrom` already subtracts `lastDamage` inside the frames, so nothing is re-implemented. The `ignoresInvulnerabilityFrames` flag (on by default for projectile, fire, energy, poison, lightning and radiation, as in the Techguns factories) is honored by clearing the frames in a LOWEST `LivingAttackEvent` handler and restoring them in the hurt handler. **Rejected hits (resolved, hardening pass).** Vanilla's `attackEntityFrom` can still drop the hit after the attack event: invulnerable target, a creative player (source cannot harm in creative), health at or below zero, or fire damage against Fire Resistance (Forge 1.7.10 `EntityLivingBase` / `EntityPlayer` `.attackEntityFrom`; fire-immune mobs are *not* rejected there, vanilla guards fire at the fire source). The handler now asks the pure `FrameRules.vanillaRejects` first and does not touch the frames for those hits. For any other rejection (for example peaceful difficulty against a difficulty-scaled source on a player) `DamageSources.hit` puts the saved frames back when the hurt event never ran. Scenario `combat.rejected` proves it in the world.
- **Fire sets the fire flag**, so Fire Resistance blocks it (GUN-44). The fire resistance check is vanilla's.
- **Unresistable** sets bypass-armor and absolute (as in Techguns).
- **Death messages.** `death.attack.gregguns.<type>` and the `.item` variant (used by `EntityDamageSource` when the killer holds a named item) and `.player` (used by plain sources) all exist for the ten types, including unresistable (GUN-43).
- **Secondary effects** are a per-type default (`SecondaryEffects`), applied after the hit when final damage is above zero and skipped under full hazmat. Radiation also skips undead and arthropods, as GT's `applyRadioactivity` does. Techguns sets effects per projectile; a projectile can switch the default off with `withoutSecondaryEffects()`. The durations are placeholders for M9. Lightning's "shock" (Mining Fatigue) has no Techguns equivalent.
- **Not in M1.**
  - GUN-06 (NPC accuracy applied twice) is an accuracy/spread bug in the gun and NPC code, not in the damage path. It belongs to M2/M7.
  - (Built in the hardening pass, see "Damage factors" below.) The Techguns damage-factor table and the `ArmorPenetrationMultiplier*` configs now exist.
  - Mobs do not lose armor durability (vanilla 1.7.10 does not wear mob armor).
  - GUN-36, GUN-37, GUN-41, GUN-42, GUN-35 do not apply: vanilla sources are never converted and absorption is subtracted once by vanilla.
  - **GUN-38 (knockback strength) is fixed in M2.** Vanilla 1.7.10 `knockBack` ignores its strength, so Techguns' x1.5 to x10 multipliers all pushed like x1. `GunStats.knockback()` (default 1) is applied by `GenericProjectile.onEntityHit`: after `DamageSources.hit` lets vanilla push the target, the change in the target's velocity is scaled by the multiplier (`Knockback.scale`: 0 cancels the push, 2 doubles it) and the entity is flagged so a player's client receives it. Tests: `KnockbackTest.doesNotReproduceGUN38_*`, scenario `gun.knockback`. Explosions keep their own knockback (M2 grenades and launchers).
- **Drop-off** is `DamageFalloff.damageAt(base, min, start, end, distance)`, pure logic for the projectiles to call.

## Damage factors (hardening pass)

Config: `DamageFactorsConfig` (`damagefactors` category, all `@Config.Sync`): `damagePvP` 0.5, `damageTurretToPlayer` 0.5, `damageFactorNPC` 1.0 (each 0 to 100), `armorPenetrationMultiplierNPCtoPlayer` and `armorPenetrationMultiplierPvP` (each 0 to 1, default 1.0). Selection is the pure `DamageFactors` (Techguns damage-system 5.5): player to player uses the PvP factor (0 when server PvP is off); a `TurretAttacker` shooter to a player uses the turret factor; any other shooter (including `null`) to a player, and NPC or turret to a non-player, use the NPC factor; player to a non-player is 1. The penetration multiplier applies only against players (PvP for a player shooter, NPC-to-player otherwise) and travels as `HitProperties.penetrationScale()` into `DamageModel.effectivePenetration`'s `scale`. `DamageModel` reads no config.

The factors are applied **at the hit** inside `DamageSources.hit`, because they depend on the target. `ProjectileLauncher.launch`'s `damageScale` therefore stays 1 for every caller; do not scale twice.

- **For M6 (turrets):** make the turret entity implement `TurretAttacker`, fire through `Projectiles.launcher()` and let the projectile call `DamageSources.hit` with the turret as shooter.
- **For M7 (NPCs):** nothing extra for the factors; any non-player `EntityLivingBase` shooter is classed as an NPC. NPC-versus-NPC fire uses the NPC factor.
- Hits that do not go through `DamageSources.hit` (explosions built with `DamageSources.direct`) are not scaled yet; whoever adds area damage calls `DamageFactors` itself.

## Open questions

- **Hazmat mapping.** The mapping in GGS-02-22 is a proposed default. M9 balance may narrow it.
- **Reused sources.** A source is meant for one hit. The handler tolerates reuse (it resets the settled flag), but a reused source reports `isUnblockable()` true between hits.
- **Mod armor that ignores `isUnblockable()`.** If an `ISpecialArmor` from another mod reduces damage for unblockable sources, it would reduce it a second time after our handler. Only the player path is exposed (mobs use vanilla's formula). Not seen in the pack (GT's armor returns zero reduction).
