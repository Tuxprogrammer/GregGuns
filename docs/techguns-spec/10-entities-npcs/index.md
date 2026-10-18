# 10 – Entities & NPCs (Techguns 1.7.10, v0.1.2 alpha)

This area covers every **living** entity Techguns adds: the hostile gun-wielding NPCs, the Alien Bug, the Attack Helicopter boss and the turret head entity. It also covers the faction and targeting system, the gun-driven NPC AI, the natural-spawn system (the "dummy replacement" spawn manager with distance and biome danger levels), spawn eggs, the monster-spawner tile entity, `NPCUtil`, and the renderers, models, textures and sounds of these mobs.

Projectiles and death/gore effects are in **04-projectiles-fx**. Gun stats and NPC firing internals are in **02-guns**, and gun models in **03-gun-visuals-audio**. Armour worn by NPCs is in **05-armor**, and loot items in **06-items**. The spawner block and camp-flag block are in **07-blocks** and **09-machines-utility**. The turret base block, its power, ammo, repair and security, is in **09-machines-utility**. Structures that place camp flags and spawners are in **11-worldgen**.

## Contents

| File | What it covers |
|---|---|
| [mob-catalogue.md](mob-catalogue.md) | Registry names, entity IDs, tracking parameters, spawn eggs and colours, lang keys, a headline stats table and an equipment summary |
| [per-mob.md](per-mob.md) | Shared GenericNPC mechanics (equipment roll, ranged firing, drops, XP, TG armour), then every mob: stats, equipment probabilities, loot tables, sounds, special abilities, death sequences, and the turret entity side |
| [ai.md](ai.md) | The faction table, GenericNPC task list, `EntityAIRangedAttack` burst AI with per-gun AI stats, hurt-by-target faction rules, turret targeting, watching and linking, Attack Helicopter flight and attack AI, Alien Bug AI |
| [spawning.md](spawning.md) | The TGDummySpawn replacement system, danger levels, spawn tables and config keys, spawn eggs, the `TGSpawnerTileEnt` spawner settings, camp-flag spawning via `NPCUtil` |
| [rendering-and-models.md](rendering-and-models.md) | Renderer bindings, held-gun transforms, mutant scaling, helicopter OBJ rendering and crash animation, turret rendering, and the part breakdown and animation of every NPC model; gore hookup |
| [textures.md](textures.md) | All 19 `textures/entity/` files plus the spawn-egg item textures: size, content, palette, UV layout and users |
| [sounds.md](sounds.md) | All 54 `sounds/npcs/` files and their 18 events, with exactly when each plays and at what volume; the vanilla sounds used by each mob |

## Scope coverage

| Source | Notes |
|---|---|
| `java/techguns/TGEntities.java` | Registration, eggs, natural-spawn hook and spawn tables |
| `java/techguns/entities/TurretEntityUNUSED.java` | Dead prototype |
| `java/techguns/entities/npc/*` (27 files) | Every NPC, including `NPCSpawnData` (fully commented out: an old `NPCType` enum ZOMBIESOLDIER/ARMYSOLDIER/CYBERDEMON and an empty `spawnNPC` switch), `INPCTechgunsShooter`, `ITGNpcTeam`, `TGNpcFaction(s)` and `TGDummySpawn` |
| `java/techguns/entities/ai/*` (5 files) | See ai.md |
| `java/techguns/entities/spawn/*` (2 files) | See spawning.md |
| `java/techguns/entities/renderer/RenderTurret.java` | Unused renderer |
| `java/techguns/tileentities/TGSpawnerTileEnt.java`, `java/techguns/items/TGMonsterPlacer.java`, `java/techguns/util/NPCUtil.java` | See spawning.md |
| `client/renderer/entity/` living-entity renderers | RenderGenericNPC, RenderSuperMutant, RenderAlienBug, RenderHelicopter, RenderSimpleTurret. RenderDummy is noted but owned by 04. |
| `client/models/` | ModelGenericNPC, ModelSkeletonSoldier, ModelCyberDemon, ModelSuperMutant, ModelAlienBug, ModelNull and `blocks/ModelTurret`. ModelGrimReaper and ModelCyberdemonBlaster are gun models, cross-referenced to 03. |
| `models/entity/*.obj` | `apache.obj` (helicopter) in full; `launchergrenade.obj` and `reactionchamber.obj` summarised, with their owners |

## Key design facts at a glance

- **Natural spawning.** One placeholder mob (`TGDummySpawn`) is added to vanilla monster spawning with weight 600 in the overworld and 300 in the Nether. When it joins the world it is swapped for a mob chosen by weight from a danger-tiered table. Danger is distance from world spawn (0 to 3) plus biome danger (0 to 2), and the danger value also feeds equipment rolls.
- **Gun-wielding NPCs.** These are biped `EntityMob`s whose combat task is built from the held gun's AI stats: range, attack interval and burst size. They fire the real Techguns gun at reduced, difficulty-scaled damage and accuracy.
- **Factions.** HOSTILE, TURRET and NEUTRAL. Hostile NPCs ignore each other's friendly fire and target only players. Turrets shoot every `IMob`. Depending on base settings they also shoot non-friend players (via FTB Utilities), other owners' turrets and animals.
- **Two mini-bosses.** Psycho Steve (chainsaw, very fast) and Dictator Dave (golden revolver) have guaranteed loot.
- **One boss.** The Attack Helicopter is a ghast-like flyer with a boss bar. It fires 7-round MG bursts and a rocket, and has a 5-second crash-and-explode death.
- **Cyberdemon and Super Mutants.** Heavy hitters with built-in per-damage-type armour; the Cyberdemon is Nether-only.
- **Alien Bug.** A spider-based insectoid from the "Bug Hole" spawner, with an animated jaw bite.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| NPC-01 | Fallback gun pool can't pick handcannon | `java/techguns/entities/npc/GenericNPC.java:73` | `nextInt(4)` gives 0 to 3, but the handcannon is on case 4/default, so it is unreachable. The base pool is only used by the never-materialised dummy. | cosmetic |
| NPC-02 | Skeleton Soldier stone-shovel branch unreachable | `java/techguns/entities/npc/SkeletonSoldier.java:71` | `nextInt(3)` never reaches case 3/default. | cosmetic |
| NPC-03 | Pigman Soldier always gets a helmet | `java/techguns/entities/npc/ZombiePigmanSoldier.java:40-43` | The `if` has no braces. It guards a discarded `getNewWithCamo` call, so `setCurrentItemOrArmor(4, …)` always runs and the intended 50% helmet is 100%. | gameplay |
| NPC-04 | Pigman can drop a zero-size stack | `java/techguns/entities/npc/ZombiePigmanSoldier.java:150` | Obsidian Steel count is `rand.nextInt(2)`, which is 0 half the time, creating an empty/ghost item entity. | gameplay |
| NPC-05 | Stormtrooper redstone count always 1 | `java/techguns/entities/npc/StormTrooper.java:110` | `1+(int)y*3` casts `y` (in [0,1)) to 0 before multiplying. `1+(int)(y*3)` was clearly intended. | gameplay |
| NPC-06 | Cyberdemon/Super Mutant aggro roar never plays | `java/techguns/entities/npc/CyberDemon.java:101-107`, `java/techguns/entities/npc/SuperMutantBasic.java:153-157` | The sound is in `findPlayerToAttack`, which only legacy (non-AI-task) mobs call. These mobs use the new AI. | cosmetic |
| NPC-07 | Alien Bug aggro sound spams | `java/techguns/entities/npc/AlienBug.java:93-97` | The sound plays whenever any player is within 16 blocks, *before* the visibility check, and the method runs every tick while the bug has no target. | cosmetic |
| NPC-08 | Helicopter death sound has no domain | `java/techguns/entities/npc/AttackHelicopter.java:390` | `"npcs.apacheHit"` lacks `techguns:`, so it resolves to a missing vanilla sound. | cosmetic |
| NPC-09 | Helicopter ambient sound is empty string | `java/techguns/entities/npc/AttackHelicopter.java:374` | Returns `""` instead of `null`, so vanilla tries to play a sound with an empty name. | cosmetic |
| NPC-10 | Helicopter cannot aim upward | `java/techguns/entities/npc/AttackHelicopter.java:258-261, 283-287` | Pitch is `acos` of the angle between the horizontal and target vectors, which is always ≥ 0, so targets above the gun get shot at downward. | gameplay |
| NPC-11 | Helicopter projectiles have no shooter | `java/techguns/entities/npc/AttackHelicopter.java:261, 289` | Bullets and rockets are built with the shooter-less constructor. Kills are not attributed, hit entities cannot retaliate, and owner-based checks see null. | gameplay |
| NPC-12 | Turret death NPE when unlinked | `java/techguns/entities/npc/NPCTurret.java:225` | `onDeath` calls `mountedTileEnt.onTurretDeath()` without a null check. A turret killed before relinking (for example right after a chunk load) crashes the server tick. | crash |
| NPC-13 | Without FTB Utilities, turret security never targets players | `java/techguns/entities/npc/TGNpcFactions.java:44`; `java/techguns/plugins/ftbutilities/FTBUtilitiesIntegrationDummy.java:8` | The dummy `areFriends` always returns true, so `isHostile(UUID,UUID)` is always false. Security settings have no effect on players or other owners' turrets, and turrets never retaliate against players. | gameplay |
| NPC-14 | Weighted spawn pick is off by one | `java/techguns/entities/spawn/TGSpawnManager.java:113` | Uses `running >= roll` with `roll` in [0, total). A zero-weight first entry is picked when `roll == 0`, and every bucket boundary shifts by one. | gameplay |
| NPC-15 | "Weight 0 = not registered" is false | `java/techguns/TGEntities.java:141-152` | Spawns are registered regardless of weight. Combined with NPC-14, a mob configured to 0 can still spawn. | gameplay |
| NPC-16 | Log spam in other dimensions | `java/techguns/TGEntities.java:134`; `java/techguns/entities/spawn/TGSpawnManager.java:151` | The dummy is added to every non-Nether/End biome, including those of modded dimensions, but the tables only have dimensions 0 and −1. Each dummy spawn there prints "TotalWeight 0, nothing to spawn???" and wastes a spawn attempt. | cosmetic |
| NPC-17 | Camp NPC count never matches, so unlimited spawning | `java/techguns/util/NPCUtil.java:65` | `EntityByNameSelector` compares the FML-registered name (`Techguns.ArmySoldier`) with the short names in camp lists (`ArmySoldier`). The count is always 0, so camp flags spawn a soldier every 200 ticks forever. | gameplay |
| NPC-18 | Spawn egg returns stale entity | `java/techguns/items/TGMonsterPlacer.java:34` (used at 281-300) | `entityToSpawn` is an item field shared by all uses. If the name lookup fails, the previous entity is returned, the egg is consumed and that old entity gets renamed. | gameplay |
| NPC-19 | Spawn egg names not localisable | `java/techguns/items/TGMonsterPlacer.java:220` | Hard-coded `"Spawn " + registryName`, e.g. "Spawn SuperMutantHeavy". | cosmetic |
| NPC-20 | Egg spawns at block corner on liquids | `java/techguns/items/TGMonsterPlacer.java:141` | Uses integer `i, j, k` without +0.5. | cosmetic |
| NPC-21 | Alien Bug 180° death flip never applies | `java/techguns/client/renderer/entity/RenderAlienBug.java:21` | `getDeathMaxRotation(EntityLiving)` does not override the `EntityLivingBase` method. | visual |
| NPC-22 | Psycho Steve has no boss bar | `java/techguns/entities/npc/PsychoSteve.java:13` | Implements `IBossDisplayData`, but nothing calls `BossStatus.setBossStatus`; the call in `onLivingUpdate` is commented out. | visual |
| NPC-23 | NPC gun difficulty scaling applied twice | `java/techguns/entities/npc/GenericNPC.java:332-348` | GenericNPC passes damage 0.5 and a difficulty accuracy factor. `fireWeaponFromNPC` then applies its own difficulty damage and accuracy factors again, so accuracy is squared (×1.69 spread on Easy). | gameplay |
| NPC-24 | Melee NPCs only hit players | `java/techguns/entities/npc/GenericNPC.java:44` | `EntityAIAttackOnCollide` is restricted to `EntityPlayer`. Hurt-by-target may select a turret or another attacker, but melee NPCs never engage it. | gameplay |
| NPC-25 | Super Mutant hitbox not widened | `java/techguns/entities/npc/SuperMutantBasic.java:22` | Only `height` is assigned (no `setSize`), so width stays 0.6 for a 1.35 to 1.65× model, and the box only updates on the next `setPosition`. | gameplay |
| NPC-26 | Turret watch-closest overwrites attack target | `java/techguns/entities/ai/TurretEntityAIWatchClosest.java:60-69` | Assigns the attack target, then immediately replaces it with the nearest-entity search (copied vanilla bug). The class filter `EntityLiving` also excludes players. | cosmetic |
| NPC-27 | Turret sounds never played | `resources/assets/techguns/sounds.json` (`npcs.turretBeep`, `npcs.turretGunequip`) | 10 sound files and 2 events are defined, but no code references them. | cosmetic |
| NPC-28 | Bosses despawn like normal mobs | `java/techguns/entities/npc/PsychoSteve.java`, `java/techguns/entities/npc/DictatorDave.java` (no `canDespawn`/persistence); `java/techguns/entities/npc/AttackHelicopter.java:180` (`despawnEntity`) | Mini-bosses and the helicopter boss can vanish when the player moves away. | gameplay |
| NPC-29 | Duplicate lang key | `resources/assets/techguns/lang/en_US.lang:271,275` | `entity.Techguns.AttackHelicopter.name` is defined twice. | cosmetic |
| NPC-30 | Bogus texture path passed to parent | `java/techguns/client/renderer/entity/RenderSuperMutant.java:21` | Builds `supermutant_texture_0.png` (nonexistent). This is masked by the `getEntityTexture` override. | cosmetic |
| NPC-31 | Ranged AI can keep shooting a stale target | `java/techguns/entities/ai/EntityAIRangedAttack.java:88` | `continueExecuting` stays true while the navigator has a path even after the attack target is cleared. `updateTask` keeps using the cached (possibly dead) target. | gameplay |
| NPC-32 | Ghast fireball insta-kill leftover | `java/techguns/entities/npc/AttackHelicopter.java:147` | A player-sourced "fireball" damage deals 1000 to the helicopter. This is copied from the ghast and is likely unintended. | gameplay |
| NPC-33 | Renderer for unregistered entity | `java/techguns/client/ClientProxy.java:421`; `java/techguns/TGEntities.java:93` | `AlienBugDomesticated` has a renderer, but its registration is commented out (dead code; `createChild` returns null). | cosmetic |
| NPC-34 | "Attack animals" turret setting hits golems | `java/techguns/entities/ai/TurretTargetSelector.java:64` | Any `IAnimals` not tamed or owned is targeted, including iron and snow golems, squid and bats. Possibly unintended. | gameplay |
| NPC-35 | Out-of-range danger level check is dead | `java/techguns/entities/spawn/TGSpawnManager.java:58-66` | `spawntable.get(danger)` throws before the null check could print its message. | cosmetic |
