# Mob catalogue (registration, stats, eggs, names)

All living entities that Techguns 0.1.2 registers, with their registry data and headline stats. Each mob's full behaviour is in [per-mob.md](per-mob.md).

## Registration (`java/techguns/TGEntities.java`)

Every entity is registered with FML `registerModEntity`. The mod ID string is `Techguns` (capital T), so the global entity name is `Techguns.<Name>`, and that full name is what spawners, `EntityList` lookups and lang keys use.

The mod-local network IDs come from a shared counter (`Techguns.modEntityID`). The projectile entities take IDs 1 to 26 first, in `Techguns.preInit` (see 04-projectiles-fx). `TGEntities.init()` runs later in the same `preInit`, so the living entities get IDs 27 to 46 in this order:

| ID | Registry name | Class | Tracking range | Update freq. | Velocity updates | Spawn egg | Egg base colour | Egg spot colour |
|---|---|---|---|---|---|---|---|---|
| 27 | `ZombieSoldier` | `ZombieSoldier` | 80 | 3 | no | yes | `0x757468` | `0x38B038` |
| 28 | `ZombieFarmer` | `ZombieFarmer` | 80 | 3 | no | yes | `0x757468` | `0x38B038` |
| 29 | `ZombieMiner` | `ZombieMiner` | 80 | 3 | no | yes | `0x757468` | `0x38B038` |
| 30 | `ZombiePigmanSoldier` | `ZombiePigmanSoldier` | 80 | 3 | no | yes | `0xFF1111` | `0x770000` |
| 31 | `AttackHelicopter` | `AttackHelicopter` | 80 | 3 | no | yes | `0x373d23` | `0x8ec0d7` |
| 32 | `AlienBug` | `AlienBug` | 80 | 3 | no | yes | `0xc6a96b` | `0x71552e` |
| 33 | `CyberDemon` | `CyberDemon` | 80 | 3 | no | yes | `0xFF1111` | `0x777777` |
| 34 | `ArmySoldier` | `ArmySoldier` | 80 | 3 | no | yes | `0x74806e` | `0x191512` |
| 35 | `Bandit` | `Bandit` | 80 | 3 | no | yes | `0x8f9d59` | `0x2c3117` |
| 36 | `Outcast` | `Outcast` | 80 | 3 | no | yes | `0x101010` | `0xFF0000` |
| 37 | `SkeletonSoldier` | `SkeletonSoldier` | 80 | 3 | no | yes | `0x404040` | `0xF0F0F0` |
| 38 | `SimpleTurret` | `NPCTurret` | **120** | 3 | no | **no** | – | – |
| 39 | `TGDummySpawn` | `TGDummySpawn` | 80 | 3 | no | **no** | – | – |
| 40 | `PsychoSteve` | `PsychoSteve` | 80 | 3 | no | yes | `0x757468` | `0xF0F0F0` |
| 41 | `DictatorDave` | `DictatorDave` | 80 | 3 | no | yes | `0x8f9d59` | `0xA0A000` |
| 42 | `Commando` | `Commando` | 80 | 3 | no | yes | `0x191512` | `0x74806e` |
| 43 | `Stormtrooper` | `StormTrooper` | 80 | 3 | no | yes | `0xF0F0F0` | `0x404040` |
| 44 | `SuperMutantBasic` | `SuperMutantBasic` | 80 | 3 | no | yes | `0xc6a96b` | `0x71552e` |
| 45 | `SuperMutantHeavy` | `SuperMutantHeavy` | 80 | 3 | no | yes | `0xc6a96b` | `0x71552e` |
| 46 | `SuperMutantElite` | `SuperMutantElite` | 80 | 3 | no | yes | `0xc6a96b` | `0x71552e` |

Some classes exist in the code but are never registered:
- `AlienBugDomesticated`: its registration line is commented out, though a renderer is still bound to it.
- `TurretEntityUNUSED`: its registration in `Techguns.java` is commented out.
- `GenericNPC`: the abstract-in-practice base class. The `"Skeleshooter"` registration of it is commented out.

The NPC registration also carries a commented-out block of vanilla `EntityRegistry.addSpawn` calls with hard-coded biome lists (overworld land, desert, ice). That block is dead and has been replaced by the TG spawn manager ([spawning.md](spawning.md)).

`TGEntities.MAX_NPC_DIFFICULTY = 3` is the upper bound of the random "difficulty" rolled when an NPC is spawned from an egg or spawner.

## Spawn eggs (`java/techguns/items/TGMonsterPlacer.java`)

Each of the 18 egg-bearing mobs gets its own `TGMonsterPlacer` item:

| Property | Value |
|---|---|
| GameRegistry name | `spawnEgg<Name>`, e.g. `spawnEggZombieSoldier` |
| Unlocalized name | `spawn_egg_<name lower-cased>`, e.g. `spawn_egg_stormtrooper` |
| Icon | `techguns:spawn_egg`, i.e. `textures/items/spawn_egg.png` (pass 0) plus `textures/items/spawn_egg_overlay.png` (pass 1). See [textures.md](textures.md) |
| Tint | Render pass 0 uses the base colour and pass 1 the spot colour (table above) |
| Display name | Hard-coded `"Spawn " + <registry name>`, e.g. **"Spawn ZombieSoldier"** and **"Spawn Stormtrooper"**. It is not localised and has no lang key |
| Creative tab | Techguns tab |
| Stack size | 64. There are no subtypes and the creative list shows a single stack (meta 0) |

For spawn behaviour see [spawning.md § Spawn eggs](spawning.md#spawn-eggs).

## Lang keys (`lang/en_US.lang`)

| Key | English |
|---|---|
| `entity.Techguns.AttackHelicopter.name` | Attack Helicopter *(defined twice, lines 271 and 275)* |
| `entity.Techguns.ArmySoldier.name` | Army Soldier |
| `entity.Techguns.ZombieSoldier.name` | Zombie Soldier |
| `entity.Techguns.ZombiePigmanSoldier.name` | Zombie Pigman Soldier |
| `entity.Techguns.CyberDemon.name` | Cyberdemon |
| `entity.Techguns.SimpleTurret.name` | Turret |
| `entity.Techguns.ZombieFarmer.name` | Zombie Farmer |
| `entity.Techguns.ZombieMiner.name` | Zombie Miner |
| `entity.Techguns.Bandit.name` | Bandit |
| `entity.Techguns.Outcast.name` | Outcast |
| `entity.Techguns.AlienBug.name` | Alien Bug |
| `entity.Techguns.PsychoSteve.name` | Psycho Steve |
| `entity.Techguns.SkeletonSoldier.name` | Skeleton Soldier |
| `entity.Techguns.DictatorDave.name` | Dictator Dave |
| `entity.Techguns.Commando.name` | Commando |
| `entity.Techguns.Stormtrooper.name` | Stormtrooper |
| `entity.Techguns.SuperMutantBasic.name` | Super Mutant |
| `entity.Techguns.SuperMutantHeavy.name` | Super Mutant |
| `entity.Techguns.SuperMutantElite.name` | Super Mutant |

`TGDummySpawn` has no lang key, but it never exists in the world. The related block names `techguns.tile.TGMonsterSpawner_0.name` = "Bug Hole" and `techguns.tile.TGMonsterSpawner_1.name` = "Zombie Hole" belong to 07-blocks.

These entities generate no tooltips in code.

## Headline stats

Base attributes come from `applyEntityAttributes`. "Armour" is the vanilla armour value from worn items or the `getTotalArmorValue` override. The per-damage-type Techguns armour is in [per-mob.md](per-mob.md). Speed is the vanilla `movementSpeed` attribute (a player walks at 0.1; a zombie is 0.23). XP is the base `experienceValue`; for `EntityLiving` the game adds 1 to 3 per equipped item.

| Mob | Base class | Faction | HP | Melee dmg | Speed | Follow range | Size (W×H) | Base XP | Fire immune |
|---|---|---|---|---|---|---|---|---|---|
| Zombie Soldier | GenericNPC (EntityMob) | HOSTILE | 25 | 4 | 0.25 | 50 | 0.6×1.8 | 5 | no |
| Zombie Farmer | GenericNPC | HOSTILE | 18 | 3 | 0.20 | 50 | 0.6×1.8 | 5 | no |
| Zombie Miner | GenericNPC | HOSTILE | 20 | 4 | 0.20 | 50 | 0.6×1.8 | 5 | no |
| Zombie Pigman Soldier | GenericNPC | HOSTILE | 25 | 5 | 0.30 | 50 | 0.6×1.8 | 5 | **yes** |
| Skeleton Soldier | GenericNPC | HOSTILE | 25 | 4 | 0.25 | 50 | 0.6×1.8 | 5 | no |
| Army Soldier | GenericNPC | HOSTILE | 30 | 4 | 0.30 | 75 | 0.6×1.8 | 5 | no |
| Bandit | GenericNPC | HOSTILE | 20 | 5 | 0.30 | 40 | 0.6×1.8 | 5 | no |
| Outcast | GenericNPC | HOSTILE | 30 | 8 | 0.20 | 60 | 0.6×1.8 | 5 | no |
| Commando | GenericNPC | HOSTILE | 30 | 5 | 0.35 | 75 | 0.6×1.8 | 5 | no |
| Stormtrooper | GenericNPC | HOSTILE | 35 | 4 | 0.30 | 75 | 0.6×1.8 | 5 | no |
| Cyberdemon | GenericNPC | HOSTILE | 30 | 7 | 0.35 | 50 | 0.6×1.8 | 5 | **yes** |
| Psycho Steve (mini-boss) | GenericNPC | HOSTILE | 75 | 7 | **0.60** | 60 | 0.6×1.8 | **25** | no |
| Dictator Dave (mini-boss) | GenericNPC | HOSTILE | 100 | 7 | 0.40 | 60 | 0.6×1.8 | **35** | no |
| Super Mutant (Basic) | GenericNPC | HOSTILE | 30 | 7 | 0.30 | 50 | 0.6×2.43 | 5 | no |
| Super Mutant (Heavy) | SuperMutantBasic | HOSTILE | 40 | 9 | 0.25 | 50 | 0.6×2.70 | 5 | no |
| Super Mutant (Elite) | SuperMutantBasic | HOSTILE | 50 | 11 | 0.20 | 50 | 0.6×2.97 | 5 | no |
| Alien Bug | EntitySpider | HOSTILE | 20 | 4 | **1.0** | 16 (spider default) | 1.1×1.2 | 5 | no |
| Attack Helicopter (boss) | EntityFlying | *(none; not ITGNpcTeam)* but `IMob` | 100 | – | – (flies) | – | 10×4 | **50** | no |
| Turret (`SimpleTurret`) | EntityCreature | TURRET | 30 | – | 0 | 20, then the gun's AI range | 0.6×1.0 | 0 | no |
| TGDummySpawn | GenericNPC | HOSTILE | (20) | – | – | – | 0.6×1.8 | – | – |

Vanilla armour values for the super mutants come from the `getTotalArmorValue` override: Basic 7, Heavy 12, Elite 10. The Heavy has more armour than the Elite, though the Elite has more HP and damage. The turret's armour value equals the turret-armour item installed in its base: 0, 5, 10, 15 or 20.

## Equipment at a glance

The full probability tables are in [per-mob.md](per-mob.md).

| Mob | Armour | Weapon pool |
|---|---|---|
| Zombie Soldier | T1 Combat, each piece 50% | revolver, thompson, iron shovel, stone shovel (25% each) |
| Zombie Farmer | T1 Miner, random shared camo; chest 100%, legs and boots 50% | wooden, iron or stone hoe, handcannon (25% each) |
| Zombie Miner | T1 Miner, random shared camo; helmet 100%, rest 50% | wooden, iron or stone pickaxe, handcannon (25% each) |
| Zombie Pigman Soldier | T2 Combat camo 3 ("arctic"); helmet always (bug), rest 50% | depends on spawn difficulty: thompson, revolver, AK-47, pistol, flamethrower, combat shotgun, LMG, rocket launcher |
| Skeleton Soldier | helmet T1 combat or T1 scout; boots T1 combat or T1 scout | revolver, thompson, handcannon (⅓ each) |
| Army Soldier | 66% helmet (T2 combat or green beret), T2 Combat chest, legs and boots 50% each | M4, combat shotgun, bolt-action (⅓ each) |
| Bandit | T1 Scout chest, legs and boots always; helmet 50% | pistol, AK-47, bolt-action, sawed-off, thompson, revolver (⅙ each) |
| Outcast | full T3 Power Armour, each piece dark or dark2 | laser gun, Grim Reaper, minigun, bio gun, flamethrower, tesla gun, LMG (1/7 each) |
| Commando | full T2 Commando | silenced M4 (`m4_uq`) |
| Stormtrooper | full T3 Miner (HEV) camo 1 "silver" | blaster rifle or laser gun (50/50) |
| Cyberdemon | none | Cyberdemon Blaster |
| Psycho Steve | full T1 Miner camo 1 "red" | chainsaw |
| Dictator Dave | default beret, T1 combat chest, T1 scout legs camo 2 "snow", T1 combat boots | golden revolver |
| Super Mutants (all) | none (built-in armour) | rocket launcher, AK-47, combat shotgun (⅕ each), laser gun (⅖) |
| Alien Bug, Helicopter, Turret | – | built-in attacks; the turret uses whatever gun is in its base |
