# Infernal Mobs

- **Techguns baseline:** [per-mob.md § shared GenericNPC mechanics](../../techguns-spec/10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava), [ai.md § factions](../../techguns-spec/10-entities-npcs/ai.md#1-factions-tgnpcfaction-tgnpcfactions), [damage-system.md](../../techguns-spec/02-guns/damage-system.md)
- **Design records:** [0004 Damage model](../../design/0004-damage-model.md), [0005 World presence](../../design/0005-world-presence.md)
- **Pack mods:** Infernal Mobs (`InfernalMobs`, 1.10.7-GTNH)
- **Roadmap:** M7 (NPCs and spawning), M9 (balance and pack config)

Techguns has no Infernal Mobs support. This sheet makes GregGuns hostile NPCs **eligible** for Infernal Mobs, keeps GregGuns' damage model compatible with the modifiers, and adds GregGuns-tiered loot to *our* infernal NPCs.

## What changes from Techguns

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| NPCs are plain `EntityMob`s. Infernal Mobs would already roll on them (the pack config defaults unknown entities to allowed), but nothing was checked. | Eligibility is a stated contract: every hostile-faction NPC is eligible, and the turret and any non-hostile NPC are not. | The user wants hostile NPCs able to spawn as elite, ultra and infernal mobs. |
| `DamageSystem` cancels `LivingAttackEvent` and re-implements damage for TG damage sources and for `INpcTGDamageSystem` victims ([damage-system.md § 4](../../techguns-spec/02-guns/damage-system.md#4-entry-point-livingattackevent-high-priority)). | The pipeline must still deliver `LivingHurtEvent` with the final amount, and ranged sources must look like projectiles to Infernal Mobs. | The modifiers hook only `LivingHurtEvent`. Bulwark, Vengeance and the contact modifiers rely on it. |
| Loot is the mob's own table and the vanilla equipment drop. | Infernal NPCs also drop GregGuns items by tier. | Matches the pack's elite, ultra and infernal reward bands. |

## How Infernal Mobs 1.10.7-GTNH behaves

All of this is read from source at the pack tag. Line anchors are in the table.

| Topic | Behavior | Source |
|---|---|---|
| When it rolls | On every server-side `EntityJoinWorldEvent` for a living entity that has no saved modifier string. Natural spawns, eggs, spawner blocks, camp flags and commands all reach it. | [EntityEventHandler#L70-L80](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L70-L80), [InfernalMobsCore#L382-L408](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L382-L408) |
| Eligibility | `instanceof IMob`, not `IEntityOwnable`, and `permittedentities.<key>` true. | [InfernalMobsCore#L411-L418](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L411-L418), [#L438-L451](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L438-L451) |
| Config key | The **simple Java class name** when `useSimpleEntityClassnames=true`, which the pack sets. Otherwise it is the `EntityList` name. Unknown keys are written back to the file on first sight, with `permittedentities` true. | [InfernalMobsCore#L265](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L265), [#L438-L466](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L438-L466) |
| Tier | One roll for elite (`eliteRarity`), then one for ultra (`ultraRarity`), then one for infernal (`infernoRarity`). The tier sets the modifier count range. The tier name is **not stored**. | [InfernalMobsCore#L501-L520](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L501-L520) |
| Drop band | Chosen at death from the modifier count: up to 5 is the elite list, up to 10 the ultra list, otherwise the infernal list. One item per 5 modifiers. Only when the kill was a player kill. | [InfernalMobsCore#L651-L686](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L651-L686), [MobModifier#L167-L172](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/modifiers/MobModifier.java#L167-L172) |
| Drop delivery | Items are spawned directly in the world. They never enter `LivingDropsEvent.drops`. After its handler runs, Infernal Mobs **removes the entity from its modifier map**. | [EntityEventHandler#L223-L236](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L223-L236) |
| Public API | `InfernalMobsCore.getIsRareEntity(ent)` and `getMobModifiers(ent).getModSize()`. | [InfernalMobsCore#L621-L628](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L621-L628) |
| Client | Glow is `mobSpell` particles from a `RenderWorldLastEvent` hook, and the HUD bar draws in the vanilla boss-health slot. It needs no cooperation from entity renderers. | [RendererBossGlow](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/client/RendererBossGlow.java) |

## Integration points

### Infernal Mobs: which GregGuns entities are eligible

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/InfernalMobs.cfg`.
  - `general.useSimpleEntityClassnames=true`, so every key below is a **class simple name**, not the `gregguns.<Name>` registry name.
  - `eliteRarity=20`, `ultraRarity=10`, `infernoRarity=7`. That is 1 in 20 eligible spawns elite, 1 in 200 ultra and 1 in 1400 infernal.
  - Modifier counts are elite 2-5, ultra 5-10 and infernal 8-15. The drop bands (up to 5, up to 10, above 10) overlap these ranges, so an infernal mob with 8-10 modifiers drops from the *ultra* list.
  - `mobHealthFactor="1.8"`, `dimensionIDBlackList=50`, `AntiMobfarmingEnabled=false`.
  - The pack has no key for any GregGuns or Techguns class. `EntityAlienBug` belongs to another mod and does not match our `AlienBug`.
- **Source:** eligibility and key lookup are the `InfernalMobsCore` lines in the table above.
- **GregGuns does:**

| Class (simple name, as in Techguns) | Registry name | `IMob` | Infernal | Base HP | Note |
|---|---|---|---|---|---|
| `ZombieSoldier` | `gregguns.ZombieSoldier` | yes | eligible | 25 | |
| `ZombieFarmer` | `gregguns.ZombieFarmer` | yes | eligible | 18 | |
| `ZombieMiner` | `gregguns.ZombieMiner` | yes | eligible | 20 | |
| `ZombiePigmanSoldier` | `gregguns.ZombiePigmanSoldier` | yes | eligible | 25 | |
| `SkeletonSoldier` | `gregguns.SkeletonSoldier` | yes | eligible | 25 | |
| `ArmySoldier` | `gregguns.ArmySoldier` | yes | eligible | 30 | |
| `Bandit` | `gregguns.Bandit` | yes | eligible | 20 | |
| `Outcast` | `gregguns.Outcast` | yes | eligible | 30 | |
| `Commando` | `gregguns.Commando` | yes | eligible | 30 | |
| `StormTrooper` | `gregguns.Stormtrooper` | yes | eligible | 35 | Class and registry names differ in case. |
| `CyberDemon` | `gregguns.CyberDemon` | yes | eligible | 30 | Late-game, spawn-gated by config ([0005](../../design/0005-world-presence.md)). |
| `PsychoSteve` | `gregguns.PsychoSteve` | yes | eligible | 75 | Mini-boss. |
| `DictatorDave` | `gregguns.DictatorDave` | yes | eligible | 100 | Mini-boss. |
| `SuperMutantBasic`, `SuperMutantHeavy`, `SuperMutantElite` | `gregguns.SuperMutant*` | yes | eligible | 30, 40, 50 | |
| `AlienBug` | `gregguns.AlienBug` | yes (`EntitySpider`) | eligible | 20 | Late-game, spawn-gated by config. Cloaking is blocked for spiders by Infernal Mobs. |
| `AttackHelicopter` | `gregguns.AttackHelicopter` | yes | **excluded** | 100 | Pack-side `permittedentities.AttackHelicopter=false`. It is an `IBossDisplayData` boss with its own bar, so the infernal overlay would draw a second one on top. |
| `NPCTurret` | `gregguns.SimpleTurret` | **no** (`EntityCreature` + `IAnimals`) | not eligible | 30 | Faction TURRET. |
| `TGDummySpawn` (if kept) | – | yes | must never be seen | – | See GGS-10-04. |

Base HP is the Techguns value from [mob-catalog.md](../../techguns-spec/10-entities-npcs/mob-catalog.md#headline-stats). The Alien Bug and the Cyberdemon are not Techguns bosses, so they are eligible. Their rarity is already controlled by spawn gating.

- **Pack-side change:** add the `permittedentities`, `entitybasehealth` and `AttackHelicopter` entries to `config/InfernalMobs.cfg`. Without them Infernal Mobs writes its own on first sight, and `entitybasehealth` defaults to the **first spawned instance's** max health. The tuple is then frozen in the file.

### Infernal Mobs: damage events and the damage model

- **Pack config:** `InfernalMobs.cfg`, `general."MM_<name> enabled"`. The pack **disables** Ender, Lifesteal, Ninja, Rust and Sticky, and enables the rest.
- **Source:**
  - Both directions run in one `LivingHurtEvent` handler: victim modifiers via `onHurt`, attacker modifiers via `onAttack`, skipping `thorns`. [EntityEventHandler#L115-L133](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L115-L133).
  - Contact modifiers skip `EntityDamageSourceIndirect` and `isProjectile()` sources: [MM_Fiery#L20-L28](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/modifiers/MM_Fiery.java#L20-L28), [MM_Poisonous#L25-L36](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/modifiers/MM_Poisonous.java#L25-L36). Wither and Darkness use the same test; Rust and Sticky too, but are disabled.
  - Ability modifiers read the target from `LivingSetAttackTargetEvent`: [EntityEventHandler#L97-L104](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L97-L104). Alchemist, Blastoff, Choke, Cloaking, Ghastly, Gravity, Quicksand, Sprint, Storm and Webber use `getMobTarget()`.
  - Health: `setEntityHealthPastMax` calls vanilla `setHealth`. [InfernalMobsCore#L489-L492](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L489-L492). Vanilla clamps `setHealth` to `getMaxHealth()`. The jar contains no mixin or transformer that raises the maximum, so the "health x modifiers x 1.8" figure may be display-only. This is unverified; GGS-10-07 makes our code safe either way.
  - No modifier cancels death. Only Choke overrides `onDeath`, and it returns false. [MM_Choke#L71-L78](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/modifiers/MM_Choke.java#L71-L78).
- **GregGuns does:** the pipeline rules in GGS-10-05 to GGS-10-09.
- **Pack-side change:** one optional fix. Berserk caps outgoing damage with `Math.min(berserkMaxDamage, damage * damageMultiplier)`. The shipped `berserkMaxDamage=0.0` is documented as "unlimited" but the code does not special-case zero, so a Berserk mob deals **0** damage and takes the un-multiplied damage itself. [MM_Berserk#L22-L30](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/modifiers/MM_Berserk.java#L22-L30). This affects every mob in the pack, not only ours. Setting a positive cap fixes it; the proper fix is upstream.

How each enabled modifier meets a GregGuns NPC:

| Modifier group | Members (pack-enabled) | Interplay with gun-wielding NPCs | Requirement |
|---|---|---|---|
| Hurt reaction | Bulwark, Vengeance, Berserk | Needs `LivingHurtEvent` on the NPC victim with the final amount applied. Vengeance hits `source.getEntity()`, so for a bullet that is the shooter. | GGS-10-05, GGS-10-06 |
| Attacker effects | Poisonous, Weakness, Wither, Sapper, Exhaust, Darkness, Fiery | `onAttack` runs on the NPC's own bullets because `getEntity()` is the shooter. Contact effects (`onHurt`) skip projectile and indirect sources. | GGS-10-06 |
| Target abilities | Alchemist, Blastoff, Choke, Cloaking, Ghastly, Gravity, Quicksand, Sprint, Storm, Webber | Need `getAttackTarget()` set through `setAttackTarget`. Cloaking makes the NPC invisible, so the held gun and armor stay drawn as floating items, as for a vanilla zombie. | GGS-10-08 |
| Health | 1UP, Regen | Use `getHealth()`/`setHealth()` against the modifier's own maximum. | GGS-10-07 |
| Disabled in the pack | Ender, Ninja, Lifesteal, Rust, Sticky | Ender and Ninja teleport the NPC beside the attacker. If re-enabled, check that the ranged AI recovers. Rust damages the target's armor. | none |

### Infernal Mobs: tiered GregGuns loot

- **Pack config:** `InfernalMobs.cfg`, `general.droppedItemIDsElite`, `droppedItemIDsUltra` and `droppedItemIDsInfernal` are **global** lists of vanilla items. They stay untouched.
- **Source:** the drop band and delivery rows in the table above. The vanilla stack for each list item is parsed once at load. [InfernalMobsCore#L338-L360](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L338-L360).
- **GregGuns does:** a soft-dependency `LivingDropsEvent` handler. Only for GregGuns NPCs, it reads the tier from `getMobModifiers(ent).getModSize()` and adds items from the tiered tables in [pack-loot.md](../06-items/pack-loot.md#infernal-mobs-handler) to `event.drops`.
  - It runs at `EventPriority.HIGH`. Infernal Mobs removes the entity from its map at normal priority, so a later handler sees `getIsRareEntity` false.
  - The handler lives in its own class and is registered only after `Loader.isModLoaded("InfernalMobs")`. Common code never names an Infernal Mobs class.
  - Mirror the core: act only when `event.recentlyHit`, use the same bands, and roll `ceil(modSize / 5)` times.
  - Loot tier ceilings come from [0005](../../design/0005-world-presence.md). The tables, and which items sit in which band, belong to [06-items](../06-items/pack-loot.md#infernal-mobs-handler). This sheet owns only the trigger.
- **Pack-side change:** none by default. Alternative (route A in [pack-loot.md](../06-items/pack-loot.md#infernal-mobs)): append GregGuns items to `droppedItemIDs*`. That is the only way to make them show in NEI's Infernal Drops page (see [mob-drops-nei.md](mob-drops-nei.md)), but it makes **every** infernal mob in the pack drop them.

### Infernal Mobs: opt-out for farmable sources

- **Pack config:** none.
- **Source:** if the entity's persistent data already holds a non-empty modifier string, the join handler calls `addEntityModifiersByString` and **skips** the rolling path, and an unparseable string adds no modifiers. [EntityEventHandler#L70-L80](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L70-L80), [InfernalMobsCore#L582-L595](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L582-L595). The key is `InfernalMobsCore.getNBTTag()`.
- **GregGuns does:** NPCs spawned by the Bug Hole and Zombie Hole spawner blocks ([07-blocks](../../techguns-spec/07-blocks/index.md)) get a non-modifier marker string in that key before they join the world, unless `infernal.allowSpawnerNpcs` is true (default false). A spawner is a farm, and the pack has anti-farming off. This relies on internal behavior, so it is guarded by the soft-dependency check and covered by a test.
- **Pack-side change:** none.

## Requirements

| ID | Requirement |
|---|---|
| GGS-10-01 | Every hostile-faction GregGuns NPC (all `GenericNPC` subclasses and the Alien Bug) is an `IMob` and is not an `IEntityOwnable`. |
| GGS-10-02 | The turret entity and any future neutral or allied NPC are not `IMob`. An NPC that has an owner implements `IEntityOwnable`. |
| GGS-10-03 | The simple names of NPC classes are stable and unique in the pack. A rename needs a matching pack-side change to `permittedentities` and `entitybasehealth`. |
| GGS-10-04 | No throwaway entity (such as a spawn placeholder) can be processed by Infernal Mobs. A placeholder's join is canceled at `EventPriority.HIGHEST`, or the design has no placeholder (see [spawning.md](spawning.md)). Infernal Mobs holds modified entities in a strong map. |
| GGS-10-05 | Damage to and from a GregGuns NPC posts `LivingHurtEvent` exactly once, with the attacker's `DamageSource`, and the pipeline applies the event's final amount. This holds for player to NPC, NPC to player and NPC to NPC. |
| GGS-10-06 | `DamageSource.getEntity()` returns the shooter for every ranged GregGuns source. Ranged sources (bullets, beams, projectiles, explosions) subclass `EntityDamageSourceIndirect`, with the projectile as the direct entity. Melee sources subclass `EntityDamageSource`. A laser shot at a Fiery or Poisonous infernal NPC must not ignite or poison the shooter. |
| GGS-10-07 | NPC code reads and writes health only through `getHealth()` and `setHealth()`. It never caches the maximum, clamps health to it each tick, or compares against it for death. |
| GGS-10-08 | NPC target selection and retargeting go through `setAttackTarget`, so `LivingSetAttackTargetEvent` fires. Clearing a stale target also goes through it ([NPC-31](../../techguns-spec/10-entities-npcs/index.md#suspected-bugs)). |
| GGS-10-09 | The NPC death effects (gore, death sound, drops) run on the vanilla `onDeath` path after `LivingDeathEvent`, so a canceled death produces no gore. |
| GGS-10-10 | The infernal loot handler is a soft dependency in its own class. It runs at `EventPriority.HIGH` and acts only for GregGuns NPCs where `getIsRareEntity` is true and `recentlyHit` is true. The config toggle `infernal.tieredLoot` (default true) disables it. |
| GGS-10-11 | The handler's tier bands match the core: up to 5 modifiers is `ELITE`, up to 10 `ULTRA`, above that `INFERNAL`. It rolls `ceil(modSize / 5)` items. The band logic is a pure function with unit tests. |
| GGS-10-12 | Items added by the handler come from the tier tables in [pack-loot.md](../06-items/pack-loot.md#infernal-mobs-handler) and never exceed the tier ceiling in [0005](../../design/0005-world-presence.md). |
| GGS-10-13 | NPCs from spawner blocks skip infernal rolling unless `infernal.allowSpawnerNpcs` is true. |
| GGS-10-14 | NPC code never replaces, clears or rewrites `getEntityData()`. Infernal Mobs stores its modifier string there, and it persists through the entity's normal save. |

## Tests

| ID | Verification |
|---|---|
| GGS-10-01, 02 | Unit test over the NPC registry: for each entry assert the `IMob` / `IEntityOwnable` expectation from the table. |
| GGS-10-03 | Unit test that lists NPC simple names, asserts uniqueness and compares them with the table above. |
| GGS-10-04 | runServer check: spawn through the natural-spawn path 200 times with Infernal Mobs loaded. The count of `getRareMobs()` entries that are not in a world must be 0. |
| GGS-10-05, 06 | Unit tests on the pure damage pipeline: a recorded `LivingHurtEvent` count of 1, the modified amount is applied, and each ranged source is `instanceof EntityDamageSourceIndirect`. `/gregguns test` scenario: `/spawninfernal` a Bulwark Zombie Soldier and shoot it. Damage must be reduced. |
| GGS-10-06 | `/gregguns test` scenario: a Fiery infernal Bandit shot with a Laser Rifle from 20 blocks. The player must not burn. |
| GGS-10-07 | `/gregguns test` scenario: `/spawninfernal` with Regen and 1UP, then read `getHealth()` against `getMaxHealth()` and the modifier's maximum. Record the result in the sheet (open question 1). |
| GGS-10-08 | `/gregguns test` scenario: Ghastly Army Soldier fires fireballs at a player in view. |
| GGS-10-09 | Unit test: a canceled `LivingDeathEvent` leaves no gore request. |
| GGS-10-10 to 12 | Unit tests for the band function and roll count. `/gregguns test` scenario: kill 50 elite and 50 ultra spawns and check that every drop is in the table for its band. |
| GGS-10-13 | runServer check: a Zombie Hole spawns 100 NPCs and none has a modifier. |
| GGS-10-14 | Unit test: write and read an NPC with a modifier string in its persistent data. The string survives. |

Spawn note for tests: `/spawninfernal x y z <EntityList name> <modifiers>` builds the entity by constructor only. [InfernalCommandSpawnInfernal#L45-L62](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/commands/InfernalCommandSpawnInfernal.java#L45-L62). It does not run the equipment roll, so the NPC is unarmed. Give it a gun first, or spawn from an egg and then add modifiers.

## Open questions

1. Is the health hack effective in this build? Answer with the GGS-10-07 scenario. If it is not, the modifiers are mostly cosmetic for health. Either way, the balance in M9 should treat the shipped HP as the real HP.
2. Should the Alien Bug and Cyberdemon be infernal-eligible? This sheet says yes, because they are not Techguns bosses and 0005 already gates their spawning. A pack that wants them clean sets `permittedentities` false. Needs a one-line confirmation in a record, probably an amendment to [0005](../../design/0005-world-presence.md).
3. Tier ceilings for the infernal bands. A surface NPC following 0005 gives LV-MV items, and the infernal band may deserve one tier more. The table owner is 06. A new record would settle it.
4. The Berserk upstream bug. Report it to Infernal-Mobs, or accept the pack-side cap.
5. Ranged-source classes in GGS-10-06 refine [0004](../../design/0004-damage-model.md). They do not contradict it, but the implementer of M1 needs to know before the first projectile ships.
