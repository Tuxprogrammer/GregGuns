# Damage sources, hazmat and other mods' armour

- **Techguns baseline:** [damage-system.md § entry point](../../techguns-spec/02-guns/damage-system.md#4-entry-point-livingattackevent-high-priority), [damage-system.md § pipeline](../../techguns-spec/02-guns/damage-system.md#5-damage-pipeline-damagesystemjava), and the bugs GUN-33 to GUN-45 in [index.md](../../techguns-spec/02-guns/index.md#suspected-bugs)
- **Design records:** [0004 Damage model](../../design/0004-damage-model.md)
- **Pack mods:**
  - GregTech (`gregtech`, `5.09.54.205`), including GT++ (`gtPlusPlus` packages in the same repository)
  - Industrial Craft 2 (`IC2`, `2.2.2.828`), API only
  - GTNH Core Mod (`dreamcraft`, `2.9.84`)
- **Roadmap:** M1 (combat core), M9 (balance); D4 gate

## What changes from Techguns

[0004](../../design/0004-damage-model.md) keeps the Techguns damage model (types, penetration, resistances) and fixes its known flaws. This sheet covers only the parts where the model meets the pack.

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| A `LivingAttackEvent` handler at HIGH priority **cancels** the event and re-implements vanilla `attackEntityFrom` and `damageEntity` by reflection | The model must not stop other mods seeing the hit (see below) | Pack mods listen to `LivingAttackEvent` and a cancelled event never reaches them. |
| Fire damage is not flagged as fire, so Fire Resistance never applies (damage-system § 5.2, step 1) | Fire-type damage sets the vanilla fire flag | Vanilla and mods key off `isFireDamage()`. |
| Vanilla armour on players blocks every Techguns type at full value, but on mobs gives half or nothing (GUN-45) | Vanilla and other mods' armour act as vanilla armour on **both** players and mobs ([0004](../../design/0004-damage-model.md)) | Consistency. |
| No link between Techguns damage types and GT's hazard system | The secondary effects of fire, ice, radiation and poison respect GT hazmat | GT players expect hazmat to matter. |

## Integration points

### GT5-Unofficial: its damage sources and what they do

- **Source:** [GTDamageSources.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/damagesources/GTDamageSources.java#L13-L111):
  - `getHeatDamage()` is source `"steam"`, flagged as fire and difficulty-scaled (L35, L73-L87).
  - `getFrostDamage()` is source `"frost"`, difficulty-scaled (L39, L58-L71).
  - `getExplodingDamage()` is source `"exploded"` and is allowed in creative, bypasses armour and is **absolute** (L27, L104-L115).
  - `getElectricDamage()`, `getRadioactiveDamage()` and `getNukeExplosionDamage()` return IC2's `Info.DMG_ELECTRIC`, `DMG_RADIATION` and `DMG_NUKE_EXPLOSION` (L15-L25). The IC2 field declarations are in `ic2/api/info/Info.java` of the API jar.
  - Each GT source supplies its own death message (`GT5U.die.*`).
- **GregGuns does:** does **not** reuse these. GT's `exploded` source ignores armour and potions, which is wrong for a rocket. GregGuns defines its own source class, one `damageType` string per Techguns damage type (for example `gregguns.energy`), with its own `death.attack.<damageType>` lang keys.
- **Pack-side change:** none.

### GT5-Unofficial: hazmat protection

- **Source:**
  - The hazards are `BIOLOGICAL, FROST, HEAT, RADIOLOGICAL, ELECTRICAL, GAS, SPACE`: [Hazard.java#L10-L18](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/Hazard.java#L10-L18).
  - Armour opts in by implementing `IHazardProtector.protectsAgainst(...)`: [IHazardProtector.java#L8-L21](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/IHazardProtector.java#L8-L21).
  - `HazardProtection.isWearingFullHazmatAgainst(entity, hazard)` is true when all four armour slots protect, or one slot protects "fully": [HazardProtection.java#L44-L60](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/HazardProtection.java#L44-L60). The GT Hazmat enchantment also counts ([L95-L101](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/HazardProtection.java#L95-L101)).
  - GT applies this itself: `applyHeatDamage`, `applyFrostDamage`, `applyElectricityDamage` and `applyRadioactivity` all skip the target when it wears full hazmat for the matching hazard: [GTUtility.java#L1982-L2060](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1982-L2060). Radioactivity also skips undead and arthropods.
- **GregGuns does:**
  - Direct damage follows [0004](../../design/0004-damage-model.md): armour value, penetration, resistance.
  - The **secondary effect** of a hit (burning, slowness from ice, the radiation potion set, the Bio Gun's poison) is skipped when the target wears full GT hazmat for the matching hazard. The mapping is fire to `HEAT`, ice to `FROST`, radiation to `RADIOLOGICAL`, poison to `BIOLOGICAL`, lightning and Tesla to `ELECTRICAL`.
  - The call is `HazardProtection.isWearingFullHazmatAgainst(entity, hazard)`. It is a hard dependency on GT, so no guard is needed.
  - GregGuns armour sets that should count as hazmat implement `IHazardProtector`. That is decided per set in [05-armor](../05-armor/index.md).
- **Pack-side change:** none. The pack's own list of hazardous items is `config/GTNewHorizons/HazardousItems.xml`. It does not affect gun damage.

### Other mods' armour: the Forge `ISpecialArmor` contract

- **Source:** GT's own armour implements Forge `ISpecialArmor`. [MechArmorBase.java#L383-L396](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L383-L396) returns a force-field absorb when active, then **`(0, 0, 0)` for `source.isUnblockable()`**, otherwise its damage-reduction ratio. The older electric armour does the same by ratio: [ItemEnergyArmor.java#L283-L292](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/ItemEnergyArmor.java#L283-L292). `MechArmorBase` also spends charge in `damageArmor`.
- **Forge `ArmorProperties.ApplyArmor` and the order of vanilla's damage steps:** Forge is not in the mod index, so there is no pinned source. The Techguns spec describes the same call ([damage-system.md § 5.3](../../techguns-spec/02-guns/damage-system.md#53-damageentityplayerplayer-src-d)). The statements below come from that and from general Forge 1.7.10 behaviour; check them in the M1 unit tests.
- **GregGuns does:**
  - Armour that is not GregGuns armour is handled by the vanilla path: Forge `ApplyArmor` over the four slots, then `damageArmor` on each, for **every** non-unblockable damage type, on players and on mobs alike. That is the fix for GUN-45.
  - A source that must ignore armour (the `UNRESISTABLE` type) is marked unblockable (`setDamageBypassesArmor`). GT's armour returns zero reduction for it, as shown above.
- **Pack-side change:** none.

### GT++ and other `LivingAttackEvent` listeners

- **Source:** GT++ baubles cancel damage by matching the source's `damageType` string against a list: [PlayerAttackEventHandler.java#L18-L36](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtPlusPlus/core/handler/events/PlayerAttackEventHandler.java#L18-L36) with the list in `BaseBauble.getDamageNegations()`. The dreamcraft Oven Glove cancels hot-item damage the same way: [OvenGlove.java#L144-L151](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/item/baubles/OvenGlove.java#L144-L151).
- **Effect on the Techguns approach:** a cancelled `LivingAttackEvent` is not delivered to later listeners unless they ask for cancelled events. Techguns cancels at HIGH priority and applies damage itself, so listeners like these never see its hits. (Forge and FML event-bus behaviour; no pinned source.)
- **GregGuns does:** keeps the hit inside vanilla's `attackEntityFrom`, so `LivingAttackEvent` and `LivingHurtEvent` listeners run. The formulas stay in the pure-logic class under `com.gregguns.combat` that [0004](../../design/0004-damage-model.md) requires. How the model plugs into the hit is an open question below.
- **Pack-side change:** none.

## Requirements

- **GGS-02-17.** GregGuns damage is delivered with a `DamageSource` subclass that has one `damageType` string per damage type, prefixed `gregguns.`.
- **GGS-02-18.** A hit from GregGuns is visible to other mods' `LivingAttackEvent` and `LivingHurtEvent` listeners. GregGuns does not cancel `LivingAttackEvent` to run its own copy of the vanilla damage routine.
- **GGS-02-19.** Fire-type damage sets the vanilla fire flag. Explosion-type sets the explosion flag. Projectile-type sets the projectile flag.
- **GGS-02-20.** Against armour that is not GregGuns armour, players and mobs get the same vanilla result. Forge `ApplyArmor` and `damageArmor` run once per hit (fixes GUN-45; penetration is also applied once per hit, as [0004](../../design/0004-damage-model.md) already requires for GUN-39).
- **GGS-02-21.** The `UNRESISTABLE` type is unblockable and skips armour. No other type is.
- **GGS-02-22.** The secondary effect of fire, ice, radiation, poison and electric hits is skipped when `HazardProtection.isWearingFullHazmatAgainst` is true for the mapped hazard. Direct damage is not.
- **GGS-02-23.** Each `damageType` has a `death.attack.<damageType>` lang key in every shipped language.

## Tests

- GGS-02-17, 19, 21: unit test on the source class (name, flags).
- GGS-02-20: unit test with a stub `ISpecialArmor` and a plain vanilla armour set. Same result for player and mob. Also the worked example from [damage-system.md § 5.7](../../techguns-spec/02-guns/damage-system.md#57-worked-example).
- GGS-02-18: `/gregguns test` scenario with a test `LivingAttackEvent` listener that records the source type, hit by each gun type.
- GGS-02-22: `/gregguns test` scenario with a target in a full hazmat set (a stub `IHazardProtector`) and one without.
- GGS-02-23: lang-file check in the build.

## Open questions

- **How the model plugs into the hit.** Two options, for a new record under D4:
  - (A) keep the Techguns design, cancel and re-implement. Simple, but breaks GGS-02-18.
  - (B) let vanilla run the hit. The GregGuns handler runs in `LivingHurtEvent`, marks the source unblockable, and applies armour itself by calling Forge `ApplyArmor` on the penetrating share only.
  - Option B satisfies GGS-02-18 but needs the M1 test above. This sheet assumes B.
- **Hazmat mapping.** The mapping in GGS-02-22 is a proposed default. M9 balance may narrow it.
