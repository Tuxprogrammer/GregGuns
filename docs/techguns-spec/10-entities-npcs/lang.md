# Lang keys and strings

Every lang key in scope for this area (mob names, spawn-egg names, the two spawner blocks), the strings the code builds itself, the hard-coded English, and the keys that are missing or duplicated. The source file is `assets/techguns/lang/en_US.lang`; there is no other language file in the Techguns resources. Line numbers below are lines of that file.

## 1. Entity name keys (`entity.Techguns.<registry name>.name`)

Forge builds an entity's translation key from the name given at registration, `entity.<modid>.<name>.name`. The mod ID string is [`Techguns`](../01-core/lifecycle-and-registration.md#lifecycle-overview) with a capital T (see [mob-catalogue.md](mob-catalogue.md#registration-javatechgunstgentitiesjava)). The name is shown in the death message of a killed mob, in the boss bar of the Attack Helicopter, in name tags and in the kill feed of other mods.

| Line | Key | English text | Registered entity | Note |
|---|---|---|---|---|
| 271 | `entity.Techguns.AttackHelicopter.name` | Attack Helicopter | [`AttackHelicopter`](per-mob.md#16-attack-helicopter-attackhelicopter-boss) (ID 31) | Defined twice, see section 4 |
| 272 | `entity.Techguns.ArmySoldier.name` | Army Soldier | [`ArmySoldier`](per-mob.md#6-army-soldier-armysoldier) (ID 34) | |
| 273 | `entity.Techguns.ZombieSoldier.name` | Zombie Soldier | [`ZombieSoldier`](per-mob.md#1-zombie-soldier-zombiesoldier) (ID 27) | |
| 274 | `entity.Techguns.ZombiePigmanSoldier.name` | Zombie Pigman Soldier | [`ZombiePigmanSoldier`](per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (ID 30) | |
| 275 | `entity.Techguns.AttackHelicopter.name` | Attack Helicopter | `AttackHelicopter` | Duplicate of line 271 ([NPC-29](index.md#suspected-bugs)) |
| 276 | `entity.Techguns.CyberDemon.name` | Cyberdemon | [`CyberDemon`](per-mob.md#11-cyberdemon-cyberdemon) (ID 33) | The English text has no capital D, unlike the registry name |
| 277 | `entity.Techguns.SimpleTurret.name` | Turret | [`SimpleTurret`](mob-catalogue.md#registration-javatechgunstgentitiesjava) = [`NPCTurret`](ai.md#5-turret-npcturret) (ID 38) | |
| 278 | `entity.Techguns.ZombieFarmer.name` | Zombie Farmer | [`ZombieFarmer`](per-mob.md#2-zombie-farmer-zombiefarmer) (ID 28) | |
| 279 | `entity.Techguns.ZombieMiner.name` | Zombie Miner | [`ZombieMiner`](per-mob.md#3-zombie-miner-zombieminer) (ID 29) | |
| 280 | `entity.Techguns.Bandit.name` | Bandit | [`Bandit`](per-mob.md#7-bandit-bandit) (ID 35) | |
| 281 | `entity.Techguns.Outcast.name` | Outcast | [`Outcast`](per-mob.md#8-outcast-outcast) (ID 36) | |
| 282 | `entity.Techguns.AlienBug.name` | Alien Bug | [`AlienBug`](per-mob.md#15-alien-bug-alienbug) (ID 32) | |
| 549 | `entity.Techguns.PsychoSteve.name` | Psycho Steve | [`PsychoSteve`](per-mob.md#12-psycho-steve-psychosteve-early-game-mini-boss) (ID 40) | |
| 554 | `entity.Techguns.SkeletonSoldier.name` | Skeleton Soldier | [`SkeletonSoldier`](per-mob.md#5-skeleton-soldier-skeletonsoldier) (ID 37) | |
| 632 | `entity.Techguns.DictatorDave.name` | Dictator Dave | [`DictatorDave`](per-mob.md#13-dictator-dave-dictatordave-mini-boss) (ID 41) | |
| 644 | `entity.Techguns.Commando.name` | Commando | [`Commando`](per-mob.md#9-commando-commando) (ID 42) | |
| 645 | `entity.Techguns.Stormtrooper.name` | Stormtrooper | [`Stormtrooper`](mob-catalogue.md#registration-javatechgunstgentitiesjava) = [`StormTrooper`](per-mob.md#10-stormtrooper-stormtrooper-registry-name-stormtrooper) (ID 43) | The key follows the registry name `Stormtrooper`, not the class name `StormTrooper` |
| 650 | `entity.Techguns.SuperMutantBasic.name` | Super Mutant | [`SuperMutantBasic`](per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (ID 44) | The three mutants share one English name |
| 651 | `entity.Techguns.SuperMutantHeavy.name` | Super Mutant | [`SuperMutantHeavy`](per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (ID 45) | |
| 652 | `entity.Techguns.SuperMutantElite.name` | Super Mutant | [`SuperMutantElite`](per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (ID 46) | |

That is 20 `entity.Techguns.*` lines for 19 distinct mob keys (the helicopter is the duplicate). One more `entity.Techguns.*` key exists in the file but belongs to a projectile, not a mob:

| Line | Key | English text | Owner |
|---|---|---|---|
| 548 | `entity.Techguns.RocketProjectile.name` | Rocket | the rocket projectile entity, see [04-projectiles-fx](../04-projectiles-fx/index.md) |

## 2. Other in-scope keys

| Line | Key | English text | Where it is used |
|---|---|---|---|
| 582 | `techguns.tile.TGMonsterSpawner_0.name` | Bug Hole | The monster-spawner block, metadata 0 (the Alien Bug spawner); the block is owned by [07-blocks](../07-blocks/index.md), its tile entity behaviour by [spawning.md](spawning.md#3-monster-spawner-blocks-tgspawnertileent) |
| 583 | `techguns.tile.TGMonsterSpawner_1.name` | Zombie Hole | The same block, metadata 1 (the zombie spawner) |
| 72 | `techguns.itemGroup.Techguns` | Techguns | The creative tab that the spawn eggs are filed under |
| 188 | `techguns.tile.BasicMachine_2.name` | Turret | The turret base block (meta 2 of the basic machine); distinct from the entity key `entity.Techguns.SimpleTurret.name`, though both read "Turret" |

The turret-armour item names and the turret GUI strings (for example `techguns.container.turretbase` = "Turret Platform" and `techguns.turret.targetAnimals` = "Target Animals") belong to [09-machines-utility](../09-machines-utility/index.md).

## 3. Strings the code produces

| String | Produced by | Condition |
|---|---|---|
| `Spawn <registry name>`, for example "Spawn ZombieSoldier", "Spawn SuperMutantHeavy", "Spawn Stormtrooper" | [`TGMonsterPlacer.getItemStackDisplayName`](../06-items/special-items.md#spawn-eggs-tgmonsterplacer), concatenated in code | Always, for each of the 18 spawn eggs. The registry name is used as is, so there are no spaces ("Spawn PsychoSteve"). No lang lookup is done ([NPC-19](index.md#suspected-bugs)). |
| `Entity not found <name>` (console only) | `TGMonsterPlacer` line 187 and [`NPCUtil`](spawning.md#4-camp-flags-and-npcutil) line 36 | The name lookup in `EntityList` fails. For the egg, see also [NPC-18](index.md#suspected-bugs) (stale entity). |
| `TotalWeight 0, nothing to spawn???` (console only) | [`TGSpawnManager.handleSpawn`](spawning.md#11-vanilla-hook) | No spawn entry has weight in the current dimension and biome ([NPC-16](index.md#suspected-bugs)). |
| `Tried to register NPC spawn for dangerlevel outside of [0-5]: <n>` (console only) | `TGSpawnManager.registerSpawn` | A spawn is registered with a danger level outside 0 to 5. |
| none | tooltips | The mob classes and spawn eggs generate no tooltips and no chat messages. |

## 4. The 18 spawn-egg names in full (hard-coded, NPC-19)

The egg display names are produced from the registered name in [`TGEntities`](mob-catalogue.md#registration-javatechgunstgentitiesjava), so they are not in the lang file and cannot be translated:

| Egg item (GameRegistry name) | Displayed name |
|---|---|
| `spawnEggZombieSoldier` | Spawn ZombieSoldier |
| `spawnEggZombieFarmer` | Spawn ZombieFarmer |
| `spawnEggZombieMiner` | Spawn ZombieMiner |
| `spawnEggZombiePigmanSoldier` | Spawn ZombiePigmanSoldier |
| `spawnEggAttackHelicopter` | Spawn AttackHelicopter |
| `spawnEggAlienBug` | Spawn AlienBug |
| `spawnEggCyberDemon` | Spawn CyberDemon |
| `spawnEggArmySoldier` | Spawn ArmySoldier |
| `spawnEggBandit` | Spawn Bandit |
| `spawnEggOutcast` | Spawn Outcast |
| `spawnEggSkeletonSoldier` | Spawn SkeletonSoldier |
| `spawnEggPsychoSteve` | Spawn PsychoSteve |
| `spawnEggDictatorDave` | Spawn DictatorDave |
| `spawnEggCommando` | Spawn Commando |
| `spawnEggStormtrooper` | Spawn Stormtrooper |
| `spawnEggSuperMutantBasic` | Spawn SuperMutantBasic |
| `spawnEggSuperMutantHeavy` | Spawn SuperMutantHeavy |
| `spawnEggSuperMutantElite` | Spawn SuperMutantElite |

The unlocalized name of each egg is `spawn_egg_<registry name lower-cased>` (for example `spawn_egg_supermutantheavy`), which would map to `item.spawn_egg_supermutantheavy.name`; no such key exists, and the override in code ignores it anyway. `NPCTurret` (`SimpleTurret`) and [`TGDummySpawn`](per-mob.md#tgdummyspawn-lifecycle-in-detail) have no egg.

## 5. Duplicate, missing and odd keys

| Issue | Detail |
|---|---|
| Duplicate key (NPC-29) | `entity.Techguns.AttackHelicopter.name` appears at lines 271 and 275 with identical text. Java `.lang` loading keeps the last value, so the result is harmless, but one line is redundant. |
| Missing: `TGDummySpawn` | There is no `entity.Techguns.TGDummySpawn.name`. The entity never lives in a loaded world, so the gap is not visible in play. |
| Missing: egg names | There are no `item.spawn_egg_*.name` keys, because the names are hard-coded (section 4). |
| Missing: [`AlienBugDomesticated`](per-mob.md#alienbugdomesticated-dead-code), [`TurretEntityUNUSED`](per-mob.md#19-turretentityunused-dead-code), [`GenericNPC`](per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) | Never registered (or, for `GenericNPC`, registered only under a commented-out name), so no key is needed. |
| Shared name | The three Super Mutant variants all read "Super Mutant"; a player cannot tell the variants apart from the name tag or a death message. |
| Spelling | "Cyberdemon" (lang) against `CyberDemon` (registry and class). Not an error, but a search for the registry name in the lang file will not match by case. |
| No `death.attack.*` keys here | The Techguns damage-source death messages live in [04-projectiles-fx](../04-projectiles-fx/index.md) and [02-guns](../02-guns/index.md); a mob killed by a Techguns weapon uses them with the mob's entity name above. |

## 6. Counts

- 19 distinct mob-name keys on 20 lines (the helicopter is the duplicate). 20 entities are registered (IDs 27 to 46), so 19 have a name and the only registered entity without one is `TGDummySpawn`.
- 2 other keys (`TGMonsterSpawner_0` and `_1`) are in scope, plus the shared creative-tab name.
- 18 hard-coded English egg names.
