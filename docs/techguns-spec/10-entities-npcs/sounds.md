# Sounds

This covers all 54 files in `sounds/npcs/` and their `sounds.json` events, plus the vanilla sounds that this area's mobs play.

All `npcs.*` events are in category **`hostile`** and are not streamed. When an event lists several files, the game picks one at random each time it plays. The files are mono Ogg Vorbis. Durations are approximate and were measured from the files.

## Event table

| sounds.json event (`techguns:` + …) | Files (`sounds/npcs/…`) | When it plays | Volume / pitch |
|---|---|---|---|
| `npcs.alienbugIdle` | `sounds/npcs/alienbug_idle-01.ogg` … `alienbug_idle-06.ogg` (6 files, 0.48–0.80 s, 44.1 kHz) | Alien Bug ambient sound (`getLivingSound`), at vanilla ambient intervals | entity sound volume (1.0), vanilla pitch jitter |
| `npcs.alienbugHurt` | `sounds/npcs/alienbug_hit-01.ogg` … `alienbug_hit-04.ogg` (4 files, 0.82–1.21 s) | Alien Bug takes damage | default |
| `npcs.alienbugDeath` | `sounds/npcs/alienbug_death_a.ogg` (2.85 s), `alienbug_death_b.ogg` (1.78 s), 32 kHz | Alien Bug dies | default |
| `npcs.alienbugStep` | `sounds/npcs/alienbug_step_a.ogg`, `_b`, `_c` (0.83–0.93 s, 48 kHz) | Every footstep of the Alien Bug; replaces the block step sound | 0.15 / 1.0 |
| `npcs.alienbugBite` | `sounds/npcs/alienbug_bite_a.ogg` … `_d` (4 files, ~0.73 s) | Each bite (`attackEntityAsMob`), whether or not the hit lands | 0.75 / 1.0 |
| `npcs.alienbugAggro` | `sounds/npcs/alienbug_aggro_a.ogg` (1.12 s, 44.1 kHz), `_b` (1.41 s), `_c` (1.28 s), `_d` (1.12 s), the last three at 22.05 kHz | While the bug has no target and a vulnerable player is within 16 blocks, even an unseen one. It can repeat every tick (bug [NPC-07](index.md#suspected-bugs)). | 1.0 / 1.0 |
| `npcs.cyberdemonIdle` | `sounds/npcs/cyberdemon_idle_a.ogg`, `_b`, `_c` (1.9–2.1 s) | Cyberdemon and all Super Mutants, ambient | default |
| `npcs.cyberdemonHurt` | `sounds/npcs/cyberdemon_hurt_a.ogg`, `_b`, `_c` (1.5–1.7 s) | Cyberdemon and Super Mutants take damage | default |
| `npcs.cyberdemonDeath` | `sounds/npcs/cyberdemon_death_a.ogg` (2.18 s), `_b` (2.65 s), 48 kHz | Cyberdemon and Super Mutants die | default |
| `npcs.cyberdemonStep` | `sounds/npcs/cyberdemon_step_a.ogg` … `_d` (4 files, 0.53 s each) | Each footstep of the Cyberdemon and Super Mutants, played *in addition to* the block step sound | 0.15 / 1.0 |
| `npcs.cyberdemonAggro` | `sounds/npcs/cyberdemon_aggro_a.ogg` (3.31 s), `_b` (3.89 s), `_c` (3.93 s) | **Never in practice.** It is coded in `findPlayerToAttack` of the Cyberdemon and Super Mutants, a legacy hook that AI-enabled mobs never call (bug [NPC-06](index.md#suspected-bugs)). The intent was a roar when the mob acquires a player. | 1.0 / 1.0 |
| `npcs.apacheRotor` | `sounds/npcs/apache_rotor.ogg` (3.07 s, 22.05 kHz) | Attack Helicopter rotor loop. It is restarted every 61 ticks (≈3.05 s) while alive (`deathTime == 0`). | **6.0** / 1.0 |
| `npcs.apacheBurst` | `sounds/npcs/apache_burst.ogg` (2.22 s) | Start of each machine-gun burst (`attackCounter == 16`). It covers all 7 shots, which have no individual sounds. | 5.0 / 1.0 |
| `npcs.apacheHit` | `sounds/npcs/apache_hit_a.ogg` (3.25 s), `_b` (3.05 s) | Helicopter takes damage (hurt sound) | entity volume **10.0** |
| `npcs.apacheDie` | `sounds/npcs/apache_die.ogg` (5.99 s, 48 kHz) | The first tick of the helicopter's 100-tick death sequence (a crash or alarm wind-down) | 6.0 / 1.0 |
| `npcs.apacheExplode` | `sounds/npcs/apache_explode.ogg` (6.79 s) | Final explosion at death tick 100, when the entity is removed | 6.0 / 1.0 |
| `npcs.turretBeep` | `sounds/npcs/turret_beep-01.ogg` (2.24 s), `-02` (0.62 s), `-03` (0.84 s), `-04` (0.55 s), `-05` (0.32 s), `-06` (0.31 s), `-07` (0.26 s), 48 kHz | **Unused.** No code plays this event. It was presumably meant as the turret targeting or acquire beep (see [NPC-27](index.md#suspected-bugs)). | – |
| `npcs.turretGunequip` | `sounds/npcs/turret_gunequip-01.ogg` (0.81 s), `-02` (0.88 s), `-03` (0.50 s), 48 kHz | **Unused.** No code plays this event. It was presumably meant to play when a gun is fitted to a turret (see NPC-27). | – |

That accounts for 54 files: 23 alien bug, 6 apache, 15 cyberdemon and 10 turret.

The helicopter's rocket shot also plays `techguns:guns.rocketFire` (volume 5; owned by [02](../02-guns/index.md)/[03](../03-gun-visuals-audio/index.md)) and vanilla aux SFX **1008** (the ghast fireball whoosh). Its coded death sound `npcs.apacheHit` lacks the `techguns:` domain, so it resolves to a missing vanilla sound ([NPC-08](index.md#suspected-bugs)). Its ambient sound is the empty string ([NPC-09](index.md#suspected-bugs)).

## Vanilla sounds used by this area's mobs

| Mob(s) | Idle | Hurt | Death | Extra step sound (volume 0.15) |
|---|---|---|---|---|
| GenericNPC default: Army Soldier, Bandit, Outcast, Commando, Stormtrooper | `mob.villager.idle` | `mob.villager.hit` | `mob.villager.death` | – |
| Psycho Steve, Dictator Dave | villager set | villager set | villager set | `mob.zombie.step` |
| Zombie Soldier, Zombie Farmer, Zombie Miner | `mob.zombie.say` | `mob.zombie.hurt` | `mob.zombie.death` | `mob.zombie.step` |
| Zombie Pigman Soldier | `mob.zombiepig.zpig` | `mob.zombiepig.zpighurt` | `mob.zombiepig.zpigdeath` | `mob.zombie.step` |
| Skeleton Soldier | `mob.skeleton.say` | `mob.skeleton.hurt` | `mob.skeleton.death` | `mob.skeleton.step` (replaces the block step) |
| Turret | none | `mob.irongolem.hit` | `mob.irongolem.death` | none (step suppressed) |

The NPCs' gunfire, reload and similar sounds are each gun's own sounds ([02](../02-guns/index.md)/[03](../03-gun-visuals-audio/index.md)). `fireWeaponFromNPC` plays the fire sound at the shooter's gun position.

## Complete file list (54)

| File | Event |
|---|---|
| `sounds/npcs/alienbug_aggro_a.ogg` | `npcs.alienbugAggro` |
| `sounds/npcs/alienbug_aggro_b.ogg` | `npcs.alienbugAggro` |
| `sounds/npcs/alienbug_aggro_c.ogg` | `npcs.alienbugAggro` |
| `sounds/npcs/alienbug_aggro_d.ogg` | `npcs.alienbugAggro` |
| `sounds/npcs/alienbug_bite_a.ogg` | `npcs.alienbugBite` |
| `sounds/npcs/alienbug_bite_b.ogg` | `npcs.alienbugBite` |
| `sounds/npcs/alienbug_bite_c.ogg` | `npcs.alienbugBite` |
| `sounds/npcs/alienbug_bite_d.ogg` | `npcs.alienbugBite` |
| `sounds/npcs/alienbug_death_a.ogg` | `npcs.alienbugDeath` |
| `sounds/npcs/alienbug_death_b.ogg` | `npcs.alienbugDeath` |
| `sounds/npcs/alienbug_hit-01.ogg` | `npcs.alienbugHurt` |
| `sounds/npcs/alienbug_hit-02.ogg` | `npcs.alienbugHurt` |
| `sounds/npcs/alienbug_hit-03.ogg` | `npcs.alienbugHurt` |
| `sounds/npcs/alienbug_hit-04.ogg` | `npcs.alienbugHurt` |
| `sounds/npcs/alienbug_idle-01.ogg` | `npcs.alienbugIdle` |
| `sounds/npcs/alienbug_idle-02.ogg` | `npcs.alienbugIdle` |
| `sounds/npcs/alienbug_idle-03.ogg` | `npcs.alienbugIdle` |
| `sounds/npcs/alienbug_idle-04.ogg` | `npcs.alienbugIdle` |
| `sounds/npcs/alienbug_idle-05.ogg` | `npcs.alienbugIdle` |
| `sounds/npcs/alienbug_idle-06.ogg` | `npcs.alienbugIdle` |
| `sounds/npcs/alienbug_step_a.ogg` | `npcs.alienbugStep` |
| `sounds/npcs/alienbug_step_b.ogg` | `npcs.alienbugStep` |
| `sounds/npcs/alienbug_step_c.ogg` | `npcs.alienbugStep` |
| `sounds/npcs/apache_burst.ogg` | `npcs.apacheBurst` |
| `sounds/npcs/apache_die.ogg` | `npcs.apacheDie` |
| `sounds/npcs/apache_explode.ogg` | `npcs.apacheExplode` |
| `sounds/npcs/apache_hit_a.ogg` | `npcs.apacheHit` |
| `sounds/npcs/apache_hit_b.ogg` | `npcs.apacheHit` |
| `sounds/npcs/apache_rotor.ogg` | `npcs.apacheRotor` |
| `sounds/npcs/cyberdemon_aggro_a.ogg` | `npcs.cyberdemonAggro` |
| `sounds/npcs/cyberdemon_aggro_b.ogg` | `npcs.cyberdemonAggro` |
| `sounds/npcs/cyberdemon_aggro_c.ogg` | `npcs.cyberdemonAggro` |
| `sounds/npcs/cyberdemon_death_a.ogg` | `npcs.cyberdemonDeath` |
| `sounds/npcs/cyberdemon_death_b.ogg` | `npcs.cyberdemonDeath` |
| `sounds/npcs/cyberdemon_hurt_a.ogg` | `npcs.cyberdemonHurt` |
| `sounds/npcs/cyberdemon_hurt_b.ogg` | `npcs.cyberdemonHurt` |
| `sounds/npcs/cyberdemon_hurt_c.ogg` | `npcs.cyberdemonHurt` |
| `sounds/npcs/cyberdemon_idle_a.ogg` | `npcs.cyberdemonIdle` |
| `sounds/npcs/cyberdemon_idle_b.ogg` | `npcs.cyberdemonIdle` |
| `sounds/npcs/cyberdemon_idle_c.ogg` | `npcs.cyberdemonIdle` |
| `sounds/npcs/cyberdemon_step_a.ogg` | `npcs.cyberdemonStep` |
| `sounds/npcs/cyberdemon_step_b.ogg` | `npcs.cyberdemonStep` |
| `sounds/npcs/cyberdemon_step_c.ogg` | `npcs.cyberdemonStep` |
| `sounds/npcs/cyberdemon_step_d.ogg` | `npcs.cyberdemonStep` |
| `sounds/npcs/turret_beep-01.ogg` | `npcs.turretBeep (unused)` |
| `sounds/npcs/turret_beep-02.ogg` | `npcs.turretBeep (unused)` |
| `sounds/npcs/turret_beep-03.ogg` | `npcs.turretBeep (unused)` |
| `sounds/npcs/turret_beep-04.ogg` | `npcs.turretBeep (unused)` |
| `sounds/npcs/turret_beep-05.ogg` | `npcs.turretBeep (unused)` |
| `sounds/npcs/turret_beep-06.ogg` | `npcs.turretBeep (unused)` |
| `sounds/npcs/turret_beep-07.ogg` | `npcs.turretBeep (unused)` |
| `sounds/npcs/turret_gunequip-01.ogg` | `npcs.turretGunequip (unused)` |
| `sounds/npcs/turret_gunequip-02.ogg` | `npcs.turretGunequip (unused)` |
| `sounds/npcs/turret_gunequip-03.ogg` | `npcs.turretGunequip (unused)` |

## Measured file table (all 54 files)

Duration, sample rate and channels are read from the Ogg Vorbis headers and the category from `sounds.json` ([ASSET-FACTS.md](../ASSET-FACTS.md)). Every file is mono, and every event is in category `hostile`. Because they are mono, Minecraft 1.7.10 attenuates them with distance and pans them in 3D.

| File | Event | Category | Duration | Sample rate | Channels | Used by code |
|---|---|---|---|---|---|---|
| `sounds/npcs/alienbug_aggro_a.ogg` | `npcs.alienbugAggro` | hostile | 1.12 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_aggro_b.ogg` | `npcs.alienbugAggro` | hostile | 1.41 s | 22.05 kHz | mono | yes |
| `sounds/npcs/alienbug_aggro_c.ogg` | `npcs.alienbugAggro` | hostile | 1.28 s | 22.05 kHz | mono | yes |
| `sounds/npcs/alienbug_aggro_d.ogg` | `npcs.alienbugAggro` | hostile | 1.12 s | 22.05 kHz | mono | yes |
| `sounds/npcs/alienbug_bite_a.ogg` | `npcs.alienbugBite` | hostile | 0.78 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_bite_b.ogg` | `npcs.alienbugBite` | hostile | 0.73 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_bite_c.ogg` | `npcs.alienbugBite` | hostile | 0.72 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_bite_d.ogg` | `npcs.alienbugBite` | hostile | 0.73 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_death_a.ogg` | `npcs.alienbugDeath` | hostile | 2.85 s | 32 kHz | mono | yes |
| `sounds/npcs/alienbug_death_b.ogg` | `npcs.alienbugDeath` | hostile | 1.78 s | 32 kHz | mono | yes |
| `sounds/npcs/alienbug_hit-01.ogg` | `npcs.alienbugHurt` | hostile | 1.03 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_hit-02.ogg` | `npcs.alienbugHurt` | hostile | 1.21 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_hit-03.ogg` | `npcs.alienbugHurt` | hostile | 0.86 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_hit-04.ogg` | `npcs.alienbugHurt` | hostile | 0.82 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_idle-01.ogg` | `npcs.alienbugIdle` | hostile | 0.59 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_idle-02.ogg` | `npcs.alienbugIdle` | hostile | 0.80 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_idle-03.ogg` | `npcs.alienbugIdle` | hostile | 0.56 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_idle-04.ogg` | `npcs.alienbugIdle` | hostile | 0.54 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_idle-05.ogg` | `npcs.alienbugIdle` | hostile | 0.71 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_idle-06.ogg` | `npcs.alienbugIdle` | hostile | 0.48 s | 44.1 kHz | mono | yes |
| `sounds/npcs/alienbug_step_a.ogg` | `npcs.alienbugStep` | hostile | 0.93 s | 48 kHz | mono | yes |
| `sounds/npcs/alienbug_step_b.ogg` | `npcs.alienbugStep` | hostile | 0.83 s | 48 kHz | mono | yes |
| `sounds/npcs/alienbug_step_c.ogg` | `npcs.alienbugStep` | hostile | 0.87 s | 48 kHz | mono | yes |
| `sounds/npcs/apache_burst.ogg` | `npcs.apacheBurst` | hostile | 2.22 s | 44.1 kHz | mono | yes |
| `sounds/npcs/apache_die.ogg` | `npcs.apacheDie` | hostile | 5.99 s | 48 kHz | mono | yes |
| `sounds/npcs/apache_explode.ogg` | `npcs.apacheExplode` | hostile | 6.79 s | 44.1 kHz | mono | yes |
| `sounds/npcs/apache_hit_a.ogg` | `npcs.apacheHit` | hostile | 3.25 s | 44.1 kHz | mono | yes |
| `sounds/npcs/apache_hit_b.ogg` | `npcs.apacheHit` | hostile | 3.05 s | 44.1 kHz | mono | yes |
| `sounds/npcs/apache_rotor.ogg` | `npcs.apacheRotor` | hostile | 3.07 s | 22.05 kHz | mono | yes |
| `sounds/npcs/cyberdemon_aggro_a.ogg` | `npcs.cyberdemonAggro` | hostile | 3.31 s | 44.1 kHz | mono | no (NPC-06) |
| `sounds/npcs/cyberdemon_aggro_b.ogg` | `npcs.cyberdemonAggro` | hostile | 3.89 s | 44.1 kHz | mono | no (NPC-06) |
| `sounds/npcs/cyberdemon_aggro_c.ogg` | `npcs.cyberdemonAggro` | hostile | 3.93 s | 44.1 kHz | mono | no (NPC-06) |
| `sounds/npcs/cyberdemon_death_a.ogg` | `npcs.cyberdemonDeath` | hostile | 2.18 s | 48 kHz | mono | yes |
| `sounds/npcs/cyberdemon_death_b.ogg` | `npcs.cyberdemonDeath` | hostile | 2.65 s | 48 kHz | mono | yes |
| `sounds/npcs/cyberdemon_hurt_a.ogg` | `npcs.cyberdemonHurt` | hostile | 1.46 s | 44.1 kHz | mono | yes |
| `sounds/npcs/cyberdemon_hurt_b.ogg` | `npcs.cyberdemonHurt` | hostile | 1.70 s | 44.1 kHz | mono | yes |
| `sounds/npcs/cyberdemon_hurt_c.ogg` | `npcs.cyberdemonHurt` | hostile | 1.46 s | 44.1 kHz | mono | yes |
| `sounds/npcs/cyberdemon_idle_a.ogg` | `npcs.cyberdemonIdle` | hostile | 1.89 s | 44.1 kHz | mono | yes |
| `sounds/npcs/cyberdemon_idle_b.ogg` | `npcs.cyberdemonIdle` | hostile | 2.13 s | 44.1 kHz | mono | yes |
| `sounds/npcs/cyberdemon_idle_c.ogg` | `npcs.cyberdemonIdle` | hostile | 1.98 s | 44.1 kHz | mono | yes |
| `sounds/npcs/cyberdemon_step_a.ogg` | `npcs.cyberdemonStep` | hostile | 0.53 s | 44.1 kHz | mono | yes |
| `sounds/npcs/cyberdemon_step_b.ogg` | `npcs.cyberdemonStep` | hostile | 0.53 s | 44.1 kHz | mono | yes |
| `sounds/npcs/cyberdemon_step_c.ogg` | `npcs.cyberdemonStep` | hostile | 0.53 s | 44.1 kHz | mono | yes |
| `sounds/npcs/cyberdemon_step_d.ogg` | `npcs.cyberdemonStep` | hostile | 0.53 s | 44.1 kHz | mono | yes |
| `sounds/npcs/turret_beep-01.ogg` | `npcs.turretBeep` | hostile | 2.24 s | 48 kHz | mono | no (NPC-27) |
| `sounds/npcs/turret_beep-02.ogg` | `npcs.turretBeep` | hostile | 0.62 s | 48 kHz | mono | no (NPC-27) |
| `sounds/npcs/turret_beep-03.ogg` | `npcs.turretBeep` | hostile | 0.84 s | 48 kHz | mono | no (NPC-27) |
| `sounds/npcs/turret_beep-04.ogg` | `npcs.turretBeep` | hostile | 0.55 s | 48 kHz | mono | no (NPC-27) |
| `sounds/npcs/turret_beep-05.ogg` | `npcs.turretBeep` | hostile | 0.32 s | 48 kHz | mono | no (NPC-27) |
| `sounds/npcs/turret_beep-06.ogg` | `npcs.turretBeep` | hostile | 0.31 s | 48 kHz | mono | no (NPC-27) |
| `sounds/npcs/turret_beep-07.ogg` | `npcs.turretBeep` | hostile | 0.26 s | 48 kHz | mono | no (NPC-27) |
| `sounds/npcs/turret_gunequip-01.ogg` | `npcs.turretGunequip` | hostile | 0.81 s | 48 kHz | mono | no (NPC-27) |
| `sounds/npcs/turret_gunequip-02.ogg` | `npcs.turretGunequip` | hostile | 0.88 s | 48 kHz | mono | no (NPC-27) |
| `sounds/npcs/turret_gunequip-03.ogg` | `npcs.turretGunequip` | hostile | 0.50 s | 48 kHz | mono | no (NPC-27) |

Sample-rate mix: 44.1 kHz (32 files), 48 kHz (16 files), 22.05 kHz (4 files: `alienbug_aggro_b`, `_c`, `_d` and `apache_rotor`), 32 kHz (2 files: `alienbug_death_a`, `_b`). Total playing time per event:

| Event | Files | Shortest | Longest | Sum |
|---|---|---|---|---|
| `npcs.alienbugAggro` | 4 | 1.12 s | 1.41 s | 4.93 s |
| `npcs.alienbugBite` | 4 | 0.72 s | 0.78 s | 2.96 s |
| `npcs.alienbugDeath` | 2 | 1.78 s | 2.85 s | 4.63 s |
| `npcs.alienbugHurt` | 4 | 0.82 s | 1.21 s | 3.92 s |
| `npcs.alienbugIdle` | 6 | 0.48 s | 0.80 s | 3.68 s |
| `npcs.alienbugStep` | 3 | 0.83 s | 0.93 s | 2.63 s |
| `npcs.apacheBurst` | 1 | 2.22 s | 2.22 s | 2.22 s |
| `npcs.apacheDie` | 1 | 5.99 s | 5.99 s | 5.99 s |
| `npcs.apacheExplode` | 1 | 6.79 s | 6.79 s | 6.79 s |
| `npcs.apacheHit` | 2 | 3.05 s | 3.25 s | 6.30 s |
| `npcs.apacheRotor` | 1 | 3.07 s | 3.07 s | 3.07 s |
| `npcs.cyberdemonAggro` | 3 | 3.31 s | 3.93 s | 11.13 s |
| `npcs.cyberdemonDeath` | 2 | 2.18 s | 2.65 s | 4.83 s |
| `npcs.cyberdemonHurt` | 3 | 1.46 s | 1.70 s | 4.62 s |
| `npcs.cyberdemonIdle` | 3 | 1.89 s | 2.13 s | 6.00 s |
| `npcs.cyberdemonStep` | 4 | 0.53 s | 0.53 s | 2.12 s |
| `npcs.turretBeep` | 7 | 0.26 s | 2.24 s | 5.14 s |
| `npcs.turretGunequip` | 3 | 0.50 s | 0.88 s | 2.19 s |

## Event detail (all 18 events: trigger, call, volume, pitch)

How the volume and pitch reach the speaker, so the numbers below are unambiguous:
- **Vanilla hook sounds** (`getLivingSound`, `getHurtSound`, `getDeathSound`) are played by the 1.7.10 `EntityLiving`/`EntityLivingBase` machinery with volume `getSoundVolume()` (1.0 unless overridden; only the Attack Helicopter overrides it, to 10.0) and pitch `getSoundPitch()`. The pitch is vanilla's random value, `1.0 + (rand - rand) * 0.2`, so it falls between 0.8 and 1.2 and clusters near 1.0. Neither Techguns nor any mob overrides the pitch.
- **Idle timing.** The vanilla ambient timer applies: each living tick, if `rand(1000)` is below a counter that rises by 1 per tick, the idle sound plays and the counter drops to minus the talk interval (80 ticks). Techguns does not override the interval, so an idle sound plays on average every few seconds, never faster than every 80 ticks.
- **Explicit calls** (`playSound`, `playSoundAtEntity`) pass the volume and pitch given in the code, with no random jitter.
- In 1.7.10 a volume above 1.0 does not make a sound louder at the source; it widens the audible radius (about 16 blocks times the volume), so the helicopter volumes of 5, 6 and 10 mean the sounds carry roughly 80 to 160 blocks.
- All 18 events are category `hostile` and none is streamed or has a volume/pitch field in `sounds.json`.

| Event | Call site | Exact trigger | Volume | Pitch |
|---|---|---|---|---|
| `npcs.alienbugIdle` | [`AlienBug.getLivingSound`](per-mob.md#15-alien-bug-alienbug) (also [`AlienBugDomesticated`](per-mob.md#alienbugdomesticated-dead-code), unregistered) | The vanilla ambient timer, while the bug is alive. | 1.0 | vanilla jitter 0.8 to 1.2 |
| `npcs.alienbugHurt` | `AlienBug.getHurtSound` | The bug takes damage that does not kill it. | 1.0 | vanilla jitter |
| `npcs.alienbugDeath` | `AlienBug.getDeathSound` | The bug takes lethal damage (played once, on the killing hit). | 1.0 | vanilla jitter |
| `npcs.alienbugStep` | `AlienBug.func_145780_a` via `playSound` | Every footstep. The override replaces the block step sound (the vanilla step is not called). | 0.15 | 1.0 |
| `npcs.alienbugBite` | `AlienBug.attackEntityAsMob` via `playSoundAtEntity` | Every time the bug performs a bite attack, hit or miss; the same call also sets the 10-tick jaw-animation timer. | 0.75 | 1.0 |
| `npcs.alienbugAggro` | `AlienBug.findPlayerToAttack` via `playSoundAtEntity` | Whenever the legacy target search finds a vulnerable player within 16 blocks, before the sight check; this method runs each tick while the bug has no target, so it can repeat every tick (NPC-07). | 1.0 | 1.0 |
| `npcs.cyberdemonIdle` | [`CyberDemon.getLivingSound`](per-mob.md#11-cyberdemon-cyberdemon), [`SuperMutantBasic.getLivingSound`](per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (Heavy and Elite inherit) | The vanilla ambient timer. | 1.0 | vanilla jitter |
| `npcs.cyberdemonHurt` | `getHurtSound` on the same two classes | The mob takes non-lethal damage. | 1.0 | vanilla jitter |
| `npcs.cyberdemonDeath` | `getDeathSound` on the same two classes | The mob takes lethal damage. | 1.0 | vanilla jitter |
| `npcs.cyberdemonStep` | `func_145780_a` on both classes via `playSoundAtEntity` | Every footstep, played after the block step sound (the override calls its parent first). | 0.15 | 1.0 |
| `npcs.cyberdemonAggro` | `findPlayerToAttack` on both classes via `playSoundAtEntity` | Coded to play when the legacy target search returns a player. Never reached for these AI-task mobs (NPC-06). | 1.0 | 1.0 |
| `npcs.apacheRotor` | [`AttackHelicopter.onEntityUpdate`](per-mob.md#16-attack-helicopter-attackhelicopter-boss) via `playSound` | A countdown of 61 ticks runs while `deathTime` is 0; when it reaches 0 the sound plays and the countdown restarts at 61. The loop therefore starts on the first update after spawning and repeats about every 3.05 s, matching the 3.07 s file with a ~0.02 s overlap. | 6.0 | 1.0 |
| `npcs.apacheBurst` | `AttackHelicopter.updateEntityActionState` via `playSoundAtEntity` | Once per machine-gun burst, when the attack counter reaches exactly 16 (the first shot of the burst); the 2.22 s file covers the whole burst. | 5.0 | 1.0 |
| `npcs.apacheHit` | `AttackHelicopter.getHurtSound` | The helicopter takes non-lethal damage. The helicopter's `getDeathSound` returns the same string without the `techguns:` domain (NPC-08), so the lethal hit plays a nonexistent sound instead. | 10.0 (`getSoundVolume`) | vanilla jitter |
| `npcs.apacheDie` | `AttackHelicopter.onDeathUpdate` via `playSound` | On the first tick of the 100-tick death sequence (`deathTime == 0`). | 6.0 | 1.0 |
| `npcs.apacheExplode` | `AttackHelicopter.onDeathUpdate` via `playSound` | At death tick 100, right after the entity is set dead. | 6.0 | 1.0 |
| `npcs.turretBeep` | none | Unused: no code references the event (NPC-27). | none | none |
| `npcs.turretGunequip` | none | Unused: no code references the event (NPC-27). | none | none |

**Inferred descriptions (not heard).** The audio cannot be listened to here, so what follows is guessed from the file names and the use. `apache_rotor` (3.07 s, 22.05 kHz, the lowest-rate file in the set) is presumably a rotor-wash loop. `apache_burst` (2.22 s) is presumably a multi-shot gun burst. `apache_die` (5.99 s) and `apache_explode` (6.79 s) are presumably a crash or alarm wind-down and a large explosion. The `alienbug_*` files are presumably chittering, hissing and chomping noises, and the `cyberdemon_*` files growls and roars. `turret_beep-*` are presumably short electronic beeps (0.26 to 2.24 s) and `turret_gunequip-*` mechanical clicks (0.5 to 0.88 s). None of this has been verified by listening.

## Per-mob sound matrix (every sound each mob can make)

"Idle", "hurt" and "death" use the vanilla hook rules above (volume 1.0, jittered pitch 0.8 to 1.2). "Step" is the explicit extra call (volume 0.15, pitch 1.0). Sound names without a domain are vanilla.

| Mob | Idle | Hurt | Death | Extra step call |
|---|---|---|---|---|
| Army Soldier, Commando, Stormtrooper, Dictator Dave, Psycho Steve (each defines the villager set itself) and Bandit, Outcast (inherit it from [`GenericNPC`](per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava)) | `mob.villager.idle` | `mob.villager.hit` | `mob.villager.death` | Dave and Steve: `mob.zombie.step`; the others: none (the line is commented out) |
| Zombie Soldier, Farmer, Miner | `mob.zombie.say` | `mob.zombie.hurt` | `mob.zombie.death` | `mob.zombie.step` |
| Zombie Pigman Soldier | `mob.zombiepig.zpig` | `mob.zombiepig.zpighurt` | `mob.zombiepig.zpigdeath` | `mob.zombie.step` |
| Skeleton Soldier | `mob.skeleton.say` | `mob.skeleton.hurt` | `mob.skeleton.death` | `mob.skeleton.step`, replacing the block step |
| Cyberdemon, Super Mutant Basic, Heavy, Elite | `techguns:npcs.cyberdemonIdle` | `techguns:npcs.cyberdemonHurt` | `techguns:npcs.cyberdemonDeath` | `techguns:npcs.cyberdemonStep`, added to the block step |
| Alien Bug | `techguns:npcs.alienbugIdle` | `techguns:npcs.alienbugHurt` | `techguns:npcs.alienbugDeath` | `techguns:npcs.alienbugStep`, replacing the block step |
| Attack Helicopter | empty string `""` (NPC-09) | `techguns:npcs.apacheHit` at volume 10 | `npcs.apacheHit` without domain (NPC-08) | none |
| Turret | none (`null`) | `mob.irongolem.hit` | `mob.irongolem.death` | none (the override is empty) |
| TGDummySpawn | inherits the villager set but never exists in the world | | | |
