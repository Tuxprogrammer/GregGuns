# 10 – Entities & NPCs (Techguns 1.7.10, v0.1.2 alpha)

## Overview


This area covers every **living** entity Techguns adds: the hostile gun-wielding NPCs, the Alien Bug, the Attack Helicopter boss and the turret head entity. It also covers the faction and targeting system, the gun-driven NPC AI, the natural-spawn system (the "dummy replacement" spawn manager with distance and biome danger levels), spawn eggs, the monster-spawner tile entity, [`NPCUtil`](spawning.md#4-camp-flags-and-npcutil), and the renderers, models, textures and sounds of these mobs.

Projectiles and death/gore effects are in **[04-projectiles-fx](../04-projectiles-fx/index.md)**. Gun stats and NPC firing internals are in **[02-guns](../02-guns/index.md)**, and gun models in **[03-gun-visuals-audio](../03-gun-visuals-audio/index.md)**. Armor worn by NPCs is in **[05-armor](../05-armor/index.md)**, and loot items in **[06-items](../06-items/index.md)**. The spawner block and camp-flag block are in **[07-blocks](../07-blocks/index.md)** and **[09-machines-utility](../09-machines-utility/index.md)**. The turret base block, its power, ammo, repair and security, is in **[09-machines-utility](../09-machines-utility/index.md)**. Structures that place camp flags and spawners are in **[11-worldgen](../11-worldgen/index.md)**.

## Contents

| File | What it covers |
|---|---|
| [mob-catalog.md](mob-catalog.md) | Registry names, entity IDs, tracking parameters, spawn eggs and colors, lang keys, a headline stats table and an equipment summary |
| [per-mob.md](per-mob.md) | Shared GenericNPC mechanics (equipment roll, ranged firing, drops, XP, TG armor), then every mob: stats, equipment probabilities, loot tables, sounds, special abilities, death sequences, and the turret entity side |
| [ai.md](ai.md) | The faction table, GenericNPC task list, [`EntityAIRangedAttack`](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai) burst AI with per-gun AI stats, hurt-by-target faction rules, turret targeting, watching and linking, Attack Helicopter flight and attack AI, Alien Bug AI |
| [spawning.md](spawning.md) | The TGDummySpawn replacement system, danger levels, spawn tables and config keys, spawn eggs, the [`TGSpawnerTileEnt`](spawning.md#3-monster-spawner-blocks-tgspawnertileent) spawner settings, camp-flag spawning via `NPCUtil` |
| [rendering-and-models.md](rendering-and-models.md) | Renderer bindings, held-gun transforms, mutant scaling, helicopter OBJ rendering and crash animation, turret rendering, and the part breakdown and animation of every NPC model; gore hookup |
| [textures.md](textures.md) | All 19 `textures/entity/` files plus the spawn-egg item textures: size, content, palette, UV layout and users |
| [sounds.md](sounds.md) | All 54 `sounds/npcs/` files and their 18 events, with exactly when each plays and at what volume; the vanilla sounds used by each mob |
| [lang.md](lang.md) | Every `entity.Techguns.*` key with its line and English text, the spawner-block keys, strings built in code, the 18 hard-coded egg names ([NPC-19](#suspected-bugs)), the duplicate key ([NPC-29](#suspected-bugs)) and missing keys |
| [model-notes/cyberdemon.md](model-notes/cyberdemon.md) | Clean-room design notes for rebuilding the Cyberdemon (proportions, parts, palette, motion, checklist) |
| [model-notes/super_mutant.md](model-notes/super_mutant.md) | Clean-room design notes for rebuilding the Super Mutant, all three tiers (proportions, parts, palette per tier, motion, checklist) |
| [model-notes/alien_bug.md](model-notes/alien_bug.md) | Clean-room design notes for rebuilding the Alien Bug (proportions, parts, palette, motion, checklist) |
| [model-notes/zombie_soldier.md](model-notes/zombie_soldier.md) | Clean-room design notes for rebuilding the Zombie Soldier skin (also Farmer and Miner): 128x128 scaled-up biped skin, palette, armor worn over it |
| [model-notes/army_soldier.md](model-notes/army_soldier.md) | Clean-room design notes for rebuilding the Army Soldier skin (also under armor for Outcast, Psycho Steve, Commando, Stormtrooper): 128x128 scaled-up biped skin, palette, armor |
| [model-notes/bandit.md](model-notes/bandit.md) | Clean-room design notes for rebuilding the Bandit skin: 64x64 half-sheet biped skin, palette, armor worn over it |
| [model-notes/dictator.md](model-notes/dictator.md) | Clean-room design notes for rebuilding the Dictator Dave skin: 64x64 bandit variant with sunglasses and belt line, palette, armor |

## Key facts

- **Natural spawning.** One placeholder mob (`TGDummySpawn`) is added to vanilla monster spawning with weight 600 in the overworld and 300 in the Nether. When it joins the world it is swapped for a mob chosen by weight from a danger-tiered table. Danger is distance from world spawn (0 to 3) plus biome danger (0 to 2), and the danger value also feeds equipment rolls.
- **Gun-wielding NPCs.** These are biped `EntityMob`s whose combat task is built from the held gun's AI stats: range, attack interval and burst size. They fire the real Techguns gun at reduced, difficulty-scaled damage and accuracy.
- **Factions.** HOSTILE, TURRET and NEUTRAL. Hostile NPCs ignore each other's friendly fire and target only players. Turrets shoot every `IMob`. Depending on base settings they also shoot non-friend players (via FTB Utilities), other owners' turrets and animals.
- **Two mini-bosses.** Psycho Steve (chainsaw, very fast) and Dictator Dave (golden revolver) have guaranteed loot.
- **One boss.** The Attack Helicopter is a ghast-like flyer with a boss bar. It fires 7-round MG bursts and a rocket, and has a 5-second crash-and-explode death.
- **Cyberdemon and Super Mutants.** Heavy hitters with built-in per-damage-type armor; the Cyberdemon is Nether-only.
- **Alien Bug.** A spider-based insectoid from the "Bug Hole" spawner, with an animated jaw bite.

## Related areas

- [02-guns](../02-guns/index.md): the guns NPCs carry and their AI stats.
- [05-armor](../05-armor/index.md): the armor sets NPCs spawn with.
- [04-projectiles-fx](../04-projectiles-fx/index.md): the projectiles NPCs fire, and death and gore effects.
- [09-machines-utility](../09-machines-utility/index.md): turret bases, camp flags and spawner tile entities.
- [11-worldgen](../11-worldgen/index.md): structures that spawn mobs.
- [01-core](../01-core/index.md): spawn and faction config.
- [06-items](../06-items/index.md): drops and spawn eggs.

## Coverage

| Category | In scope | Covered |
|---|---|---|
| Java source | `TGEntities`, `TurretEntityUNUSED`, 27 files in `entities/npc`, 5 in `entities/ai`, 2 in `entities/spawn`, `RenderTurret`, `TGSpawnerTileEnt`, `TGMonsterPlacer`, `NPCUtil`, 5 living-entity renderers and 7 models | All of them, as listed under Scope coverage. The consistency pass re-checked registration, IDs and egg colors in `TGEntities`, every sound call site, the NBT and DataWatcher use of each mob class, and the unregistered classes against the source. |
| Textures | 19 files in `textures/entity/` plus `spawn_egg.png` and `spawn_egg_overlay.png` | 21 of 21, each with size, opaque share, measured average and dominant colors, a palette in words and pixel rectangles ([textures.md](textures.md)) |
| Sounds | 54 files in `sounds/npcs/` and 18 `sounds.json` events | 54 of 54 files with duration, sample rate, channels and category; 18 of 18 events with trigger, volume and pitch ([sounds.md](sounds.md)) |
| Lang | 20 `entity.Techguns.*` mob lines (19 distinct keys), 2 spawner-block keys | All, plus the hard-coded strings and gaps ([lang.md](lang.md)) |
| Mobs | 20 registered entities, 2 unregistered mob classes (`AlienBugDomesticated`, `TurretEntityUNUSED`) and the `GenericNPC` base | Each has an entity sheet at the top of its section in [per-mob.md](per-mob.md) |
| Bugs | [NPC-01](#suspected-bugs) to [NPC-35](#suspected-bugs) | No new bugs were added in this pass |

### Scope coverage

| Source | Notes |
|---|---|
| [`java/techguns/TGEntities.java`](mob-catalog.md#registration-javatechgunstgentitiesjava) | Registration, eggs, natural-spawn hook and spawn tables |
| [`java/techguns/entities/TurretEntityUNUSED.java`](per-mob.md#19-turretentityunused-dead-code) | Dead prototype |
| `java/techguns/entities/npc/*` (27 files) | Every NPC, including `NPCSpawnData` (fully commented out: an old `NPCType` enum ZOMBIESOLDIER/ARMYSOLDIER/CYBERDEMON and an empty `spawnNPC` switch), [`INPCTechgunsShooter`](per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava), [`ITGNpcTeam`](per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava), `TGNpcFaction(s)` and [`TGDummySpawn`](per-mob.md#18-tgdummyspawn-tgdummyspawn) |
| `java/techguns/entities/ai/*` (5 files) | See ai.md |
| `java/techguns/entities/spawn/*` (2 files) | See spawning.md |
| `java/techguns/entities/renderer/RenderTurret.java` | Unused renderer |
| `java/techguns/tileentities/TGSpawnerTileEnt.java`, [`java/techguns/items/TGMonsterPlacer.java`](../06-items/special-items.md#spawn-eggs-tgmonsterplacer), `java/techguns/util/NPCUtil.java` | See spawning.md |
| `client/renderer/entity/` living-entity renderers | RenderGenericNPC, RenderSuperMutant, RenderAlienBug, RenderHelicopter, RenderSimpleTurret. RenderDummy is noted but owned by 04. |
| `client/models/` | ModelGenericNPC, ModelSkeletonSoldier, ModelCyberDemon, ModelSuperMutant, ModelAlienBug, ModelNull and `blocks/ModelTurret`. ModelGrimReaper and ModelCyberdemonBlaster are gun models, cross-referenced to 03. |
| `models/entity/*.obj` | `apache.obj` (helicopter) in full; `launchergrenade.obj` and `reactionchamber.obj` summarized, with their owners |

## Known limits

- **Sound content.** The audio cannot be heard here, so every description of what a sound is like is inferred from its file name and use, and is marked as inferred in [sounds.md](sounds.md). Durations, sample rates and channels are measured.
- **Colors.** The average and dominant colors are the measured values from [ASSET-FACTS.md](../ASSET-FACTS.md). The prose palettes and "depicts" descriptions in [textures.md](textures.md) were written by looking at the images and are approximate.
- **Pixel rectangles.** The biped, Cyberdemon, Super Mutant and Alien Bug rectangles are computed from the model box sizes (width 2D+2W, height D+H) and checked against the measured opaque bounding boxes, not read from the images region by region. The helicopter rectangles are the bounding boxes of the OBJ texture coordinates, so they describe scattered islands, not exact cells.
- **Vanilla behavior.** Sound volume and pitch jitter, talk interval, the vanilla NBT keys and DataWatcher indices are stated from the Minecraft 1.7.10 behavior the classes inherit, not from Techguns source. They were not re-verified against a vanilla decompile here.
- **Runtime behavior.** Nothing was run in game; flight, targeting and spawn behavior are read from the source.
- **Other areas.** Gun AI stats, turret base mechanics, spawner and camp-flag blocks and structures are only summarized here and are owned by the areas named in Related areas.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| NPC-01 | Fallback gun pool can't pick handcannon | [`java/techguns/entities/npc/GenericNPC.java:73`](per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) | `nextInt(4)` gives 0 to 3, but the handcannon is on case 4/default, so it is unreachable. The base pool is only used by the never-materialized dummy. | cosmetic |
| NPC-02 | Skeleton Soldier stone-shovel branch unreachable | [`java/techguns/entities/npc/SkeletonSoldier.java:71`](per-mob.md#5-skeleton-soldier-skeletonsoldier) | `nextInt(3)` never reaches case 3/default. | cosmetic |
| NPC-03 | Pigman Soldier always gets a helmet | [`java/techguns/entities/npc/ZombiePigmanSoldier.java:40-43`](per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) | The `if` has no braces. It guards a discarded `getNewWithCamo` call, so `setCurrentItemOrArmor(4, …)` always runs and the intended 50% helmet is 100%. | gameplay |
| NPC-04 | Pigman can drop a zero-size stack | `java/techguns/entities/npc/ZombiePigmanSoldier.java:150` | Obsidian Steel count is `rand.nextInt(2)`, which is 0 half the time, creating an empty/ghost item entity. | gameplay |
| NPC-05 | Stormtrooper redstone count always 1 | [`java/techguns/entities/npc/StormTrooper.java:110`](per-mob.md#10-stormtrooper-stormtrooper-registry-name-stormtrooper) | `1+(int)y*3` casts `y` (in [0,1)) to 0 before multiplying. `1+(int)(y*3)` was clearly intended. | gameplay |
| NPC-06 | Cyberdemon/Super Mutant aggro roar never plays | [`java/techguns/entities/npc/CyberDemon.java:101-107`](per-mob.md#11-cyberdemon-cyberdemon), [`java/techguns/entities/npc/SuperMutantBasic.java:153-157`](per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) | The sound is in `findPlayerToAttack`, which only legacy (non-AI-task) mobs call. These mobs use the new AI. | cosmetic |
| NPC-07 | Alien Bug aggro sound spams | [`java/techguns/entities/npc/AlienBug.java:93-97`](per-mob.md#15-alien-bug-alienbug) | The sound plays whenever any player is within 16 blocks, *before* the visibility check, and the method runs every tick while the bug has no target. | cosmetic |
| NPC-08 | Helicopter death sound has no domain | [`java/techguns/entities/npc/AttackHelicopter.java:390`](per-mob.md#16-attack-helicopter-attackhelicopter-boss) | `"npcs.apacheHit"` lacks `techguns:`, so it resolves to a missing vanilla sound. | cosmetic |
| NPC-09 | Helicopter ambient sound is empty string | `java/techguns/entities/npc/AttackHelicopter.java:374` | Returns `""` instead of `null`, so vanilla tries to play a sound with an empty name. | cosmetic |
| NPC-10 | Helicopter cannot aim upward | `java/techguns/entities/npc/AttackHelicopter.java:258-261, 283-287` | Pitch is `acos` of the angle between the horizontal and target vectors, which is always ≥ 0, so targets above the gun get shot at downward. | gameplay |
| NPC-11 | Helicopter projectiles have no shooter | `java/techguns/entities/npc/AttackHelicopter.java:261, 289` | Bullets and rockets are built with the shooter-less constructor. Kills are not attributed, hit entities cannot retaliate, and owner-based checks see null. | gameplay |
| NPC-12 | Turret death NPE when unlinked | [`java/techguns/entities/npc/NPCTurret.java:225`](ai.md#5-turret-npcturret) | `onDeath` calls `mountedTileEnt.onTurretDeath()` without a null check. A turret killed before relinking (for example right after a chunk load) crashes the server tick. | crash |
| NPC-13 | Without FTB Utilities, turret security never targets players | [`java/techguns/entities/npc/TGNpcFactions.java:44`](ai.md#1-factions-tgnpcfaction-tgnpcfactions); [`java/techguns/plugins/ftbutilities/FTBUtilitiesIntegrationDummy.java:8`](../01-core/integrations.md#ftb-utilities-ftbu-friends) | The dummy `areFriends` always returns true, so `isHostile(UUID,UUID)` is always false. Security settings have no effect on players or other owners' turrets, and turrets never retaliate against players. | gameplay |
| NPC-14 | Weighted spawn pick is off by one | [`java/techguns/entities/spawn/TGSpawnManager.java:113`](spawning.md#11-vanilla-hook) | Uses `running >= roll` with `roll` in [0, total). A zero-weight first entry is picked when `roll == 0`, and every bucket boundary shifts by one. | gameplay |
| NPC-15 | "Weight 0 = not registered" is false | `java/techguns/TGEntities.java:141-152` | Spawns are registered regardless of weight. Combined with [NPC-14](#suspected-bugs), a mob configured to 0 can still spawn. | gameplay |
| NPC-16 | Log spam in other dimensions | `java/techguns/TGEntities.java:134`; `java/techguns/entities/spawn/TGSpawnManager.java:151` | The dummy is added to every non-Nether/End biome, including those of modded dimensions, but the tables only have dimensions 0 and −1. Each dummy spawn there prints "TotalWeight 0, nothing to spawn???" and wastes a spawn attempt. | cosmetic |
| NPC-17 | Camp NPC count never matches, so unlimited spawning | `java/techguns/util/NPCUtil.java:65` | `EntityByNameSelector` compares the FML-registered name ([`Techguns.ArmySoldier`](../01-core/lifecycle-and-registration.md#lifecycle-overview)) with the short names in camp lists ([`ArmySoldier`](per-mob.md#6-army-soldier-armysoldier)). The count is always 0, so camp flags spawn a soldier every 200 ticks forever. | gameplay |
| NPC-18 | Spawn egg returns stale entity | `java/techguns/items/TGMonsterPlacer.java:34` (used at 281-300) | `entityToSpawn` is an item field shared by all uses. If the name lookup fails, the previous entity is returned, the egg is consumed and that old entity gets renamed. | gameplay |
| NPC-19 | Spawn egg names not localisable | `java/techguns/items/TGMonsterPlacer.java:220` | Hard-coded `"Spawn " + registryName`, e.g. "Spawn SuperMutantHeavy". | cosmetic |
| NPC-20 | Egg spawns at block corner on liquids | `java/techguns/items/TGMonsterPlacer.java:141` | Uses integer `i, j, k` without +0.5. | cosmetic |
| NPC-21 | Alien Bug 180° death flip never applies | [`java/techguns/client/renderer/entity/RenderAlienBug.java:21`](rendering-and-models.md#4-renderalienbug-extends-renderliving) | `getDeathMaxRotation(EntityLiving)` does not override the `EntityLivingBase` method. | visual |
| NPC-22 | Psycho Steve has no boss bar | [`java/techguns/entities/npc/PsychoSteve.java:13`](per-mob.md#12-psycho-steve-psychosteve-early-game-mini-boss) | Implements `IBossDisplayData`, but nothing calls `BossStatus.setBossStatus`; the call in `onLivingUpdate` is commented out. | visual |
| NPC-23 | NPC gun difficulty scaling applied twice | `java/techguns/entities/npc/GenericNPC.java:332-348` | GenericNPC passes damage 0.5 and a difficulty accuracy factor. `fireWeaponFromNPC` then applies its own difficulty damage and accuracy factors again, so accuracy is squared (×1.69 spread on Easy). | gameplay |
| NPC-24 | Melee NPCs only hit players | `java/techguns/entities/npc/GenericNPC.java:44` | `EntityAIAttackOnCollide` is restricted to `EntityPlayer`. Hurt-by-target may select a turret or another attacker, but melee NPCs never engage it. | gameplay |
| NPC-25 | Super Mutant hitbox not widened | `java/techguns/entities/npc/SuperMutantBasic.java:22` | Only `height` is assigned (no `setSize`), so width stays 0.6 for a 1.35 to 1.65× model, and the box only updates on the next `setPosition`. | gameplay |
| NPC-26 | Turret watch-closest overwrites attack target | [`java/techguns/entities/ai/TurretEntityAIWatchClosest.java:60-69`](ai.md#turretentityaiwatchclosest) | Assigns the attack target, then immediately replaces it with the nearest-entity search (copied vanilla bug). The class filter `EntityLiving` also excludes players. | cosmetic |
| NPC-27 | Turret sounds never played | `resources/assets/techguns/sounds.json` (`npcs.turretBeep`, `npcs.turretGunequip`) | 10 sound files and 2 events are defined, but no code references them. | cosmetic |
| NPC-28 | Bosses despawn like normal mobs | `java/techguns/entities/npc/PsychoSteve.java`, [`java/techguns/entities/npc/DictatorDave.java`](per-mob.md#13-dictator-dave-dictatordave-mini-boss) (no `canDespawn`/persistence); `java/techguns/entities/npc/AttackHelicopter.java:180` (`despawnEntity`) | Mini-bosses and the helicopter boss can vanish when the player moves away. | gameplay |
| NPC-29 | Duplicate lang key | `resources/assets/techguns/lang/en_US.lang:271,275` | `entity.Techguns.AttackHelicopter.name` is defined twice. | cosmetic |
| NPC-30 | Bogus texture path passed to parent | [`java/techguns/client/renderer/entity/RenderSuperMutant.java:21`](rendering-and-models.md#3-rendersupermutant-extends-rendergenericnpc) | Builds `supermutant_texture_0.png` (nonexistent). This is masked by the `getEntityTexture` override. | cosmetic |
| NPC-31 | Ranged AI can keep shooting a stale target | `java/techguns/entities/ai/EntityAIRangedAttack.java:88` | `continueExecuting` stays true while the navigator has a path even after the attack target is cleared. `updateTask` keeps using the cached (possibly dead) target. | gameplay |
| NPC-32 | Ghast fireball insta-kill leftover | `java/techguns/entities/npc/AttackHelicopter.java:147` | A player-sourced "fireball" damage deals 1000 to the helicopter. This is copied from the ghast and is likely unintended. | gameplay |
| NPC-33 | Renderer for unregistered entity | [`java/techguns/client/ClientProxy.java:421`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy); `java/techguns/TGEntities.java:93` | [`AlienBugDomesticated`](per-mob.md#alienbugdomesticated-dead-code) has a renderer, but its registration is commented out (dead code; `createChild` returns null). | cosmetic |
| NPC-34 | "Attack animals" turret setting hits golems | [`java/techguns/entities/ai/TurretTargetSelector.java:64`](ai.md#turrettargetselector-the-target-filter) | Any `IAnimals` not tamed or owned is targeted, including iron and snow golems, squid and bats. Possibly unintended. | gameplay |
| NPC-35 | Out-of-range danger level check is dead | `java/techguns/entities/spawn/TGSpawnManager.java:58-66` | `spawntable.get(danger)` throws before the null check could print its message. | cosmetic |
