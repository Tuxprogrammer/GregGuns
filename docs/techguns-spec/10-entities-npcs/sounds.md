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

<!-- audio-profiles:start -->
## How each sound sounds

Generated from [AUDIO-PROFILES.md](../AUDIO-PROFILES.md), which has full profiles with elements, envelope and recreation notes. Claude described these from spectrograms and measurements, so treat the prose as an informed reading.

| Sound | Sounds like | Character | Loudness (LUFS / peak dBFS) | Confidence |
|---|---|---|---|---|
| [`sounds/npcs/alienbug_aggro_a.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_aggro_aogg) | A gritty, chittering insectoid screech that starts as a dense burst of noisy rasping and breaks into sparse, crackly clicks that fade out over about a second. | noisy, gritty, organic, creature | -15.1 / 1.0 | medium |
| [`sounds/npcs/alienbug_aggro_b.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_aggro_bogg) | A short, harsh, noisy alien-bug aggro screech/chitter: a loud, dense, gritty burst lasting about 0.4 s, followed by a decaying trail of small clicky, rattling pulses. | noisy, gritty, creature, hostile | -14.0 / 1.6 | medium |
| [`sounds/npcs/alienbug_aggro_c.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_aggro_cogg) | A short, gritty, growling creature screech or snarl with a strong formant-like midrange body that fades into a trail of faint clicky chitters. | noisy, creature, growl, vocal | -15.3 / -2.3 | medium |
| [`sounds/npcs/alienbug_aggro_d.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_aggro_dogg) | A short, gritty, clicky alien-creature screech/chitter that opens with a low growly rasp and breaks into a decaying series of dry crackling clicks. | noisy, crunchy, organic, creature | -16.5 / 0.8 | medium |
| [`sounds/npcs/alienbug_bite_a.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_bite_aogg) | A short, wet, crunchy organic bite made of a few quick noisy chomps with a low thump, peaking around 0.13-0.2s and fading into a quiet tail. | organic, noisy, crunchy, transient | -20.8 / -2.2 | medium |
| [`sounds/npcs/alienbug_bite_b.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_bite_bogg) | A short, loud, wet and crunchy creature bite made of a few rapid chomp bursts with a noisy, bright bite texture and a thump underneath, followed by a quickly fading low rumble tail. | transient, noisy, crunchy, organic | -20.3 / 1.3 | medium |
| [`sounds/npcs/alienbug_bite_c.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_bite_cogg) | A short, wet, crunchy chomp of clustered noisy clicks and snaps with a low thump underneath, fading into a quiet tail. | transient, noisy, crunchy, organic | -18.9 / 5.8 | medium |
| [`sounds/npcs/alienbug_bite_d.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_bite_dogg) | A short, wet, crunchy bite with a few sharp snapping chomps and a low thud, decaying quickly into a faint noisy tail. | transient, noisy, crunchy, bite | -19.7 / 4.9 | medium |
| [`sounds/npcs/alienbug_death_a.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_death_aogg) | A loud, noisy, gurgling burst with a heavy low-end thump that decays into a long tail of wet crackles and clicks, like an alien creature dying and dissolving. | noisy, organic, crackle, transient | -16.2 / 4.0 | medium |
| [`sounds/npcs/alienbug_death_b.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_death_bogg) | A loud, noisy alien creature death sound: a heavy low thump and burst of noise at the start, followed by a long, crackly, crunchy, wet-sounding noise trail that fades out. | noisy, crackly, transient, organic | -14.5 / 3.8 | medium |
| [`sounds/npcs/alienbug_hit-01.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_hit-01ogg) | A loud, noisy, wet crunchy-squelch burst like a chitinous alien bug screech-hit that decays into a series of smaller crackly clicks and gurgles over about a second. | noisy, crunchy, organic, transient | -18.2 / 0.5 | medium |
| [`sounds/npcs/alienbug_hit-02.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_hit-02ogg) | A short, gritty, noisy alien creature hurt sound: a sharp crackly chitter burst at the start, a mid-body of rasping noise with two pulsed swells, then a fading tail of tiny clicks. | noisy, crackly, organic, creature | -18.5 / 1.1 | medium |
| [`sounds/npcs/alienbug_hit-03.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_hit-03ogg) | A short, harsh, clicky insectoid hurt sound: a loud noisy burst followed by a rapid train of about eight bright chittering ticks that fade into a quiet tail. | transient, noisy, gritty, chittering | -21.1 / 2.1 | medium |
| [`sounds/npcs/alienbug_hit-04.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_hit-04ogg) | A short, bright, crackly noise burst like a chittering insect screech or wet clicky rasp that starts loud and breaks into sparse clicks before fading, with one small late blip. | noisy, bright, crackly, transient | -17.1 / 0.7 | medium |
| [`sounds/npcs/alienbug_idle-01.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_idle-01ogg) | A very short, hissy, chittering insect-like rasp that swells quickly to a peak around 0.11 s, flutters through a few small clicky pulses, then fades out by about 0.5 s. | noisy, chittering, bright, hissy | -20.8 / -3.2 | medium |
| [`sounds/npcs/alienbug_idle-02.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_idle-02ogg) | A short, noisy, chittering insect-like burst of clicky, rattling texture that swells into a bright hissy peak around 0.2s and then fades into a quiet tail. | noisy, chittering, clicky, bright | -13.5 / -0.4 | medium |
| [`sounds/npcs/alienbug_idle-03.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_idle-03ogg) | A short, gritty, noisy rasp or chitter with a swelling body around 0.05-0.23s that fades into a quiet, sparse tail, like an insectoid idle vocalization. | noisy, gritty, raspy, insectoid | -17.1 / -1.0 | medium |
| [`sounds/npcs/alienbug_idle-04.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_idle-04ogg) | A short, noisy, bright chittering rasp, like a clicky insect or alien bug vocalisation that swells to a peak around 0.2s and then fades quickly into a quiet tail. | noisy, bright, chittering, organic | -14.8 / -0.2 | medium |
| [`sounds/npcs/alienbug_idle-05.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_idle-05ogg) | A short, bright, noisy insect-like chitter or rattle with a few clicky pulses that fades into a hissy tail, like an alien bug's idle call. | noisy, chittering, bright, hissy | -14.6 / -0.3 | medium |
| [`sounds/npcs/alienbug_idle-06.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_idle-06ogg) | A short, bright, gritty burst of chittering clicks and noisy rasp that swells over about 0.2 s, ends in a small tick near 0.27 s, then fades out, like an insectoid alien creature's idle chirp. | noisy, clicky, chittering, bright | -13.3 / 0.1 | medium |
| [`sounds/npcs/alienbug_step_a.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_step_aogg) | A short, bright, crunchy burst of clicky, chitinous skittering that opens with a sharp snap and then breaks into scattered noisy ticks and a brief mid-clip clatter before fading out. | transient, noisy, crunchy, clicky | -17.7 / 9.5 | medium |
| [`sounds/npcs/alienbug_step_b.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_step_bogg) | A short, crackly, scratchy burst of noisy clicks and rustles, like a skittering crunch of chitinous legs or crumpled plastic, with a sharp snap near 0.31s and a fading tail. | noisy, crackly, transient, crunchy | -18.0 / 5.4 | medium |
| [`sounds/npcs/alienbug_step_c.ogg`](../AUDIO-PROFILES.md#soundsnpcsalienbug_step_cogg) | A short, crunchy, clicky burst of scattered chitinous clicks and scrabbling crackles, like an insectoid creature's skittering footstep, with a loud initial snap and a fading tail. | transient, crackly, noisy, chitinous | -19.6 / 5.0 | medium |
| [`sounds/npcs/apache_burst.ogg`](../AUDIO-PROFILES.md#soundsnpcsapache_burstogg) | A heavily clipped, loud burst of rumbling noise and low-frequency chop, like a rotor or machine-gun burst, that swells for about 0.8 s and then decays into a low rumbling tail. | noisy, boomy, mechanical, distorted | 2.0 / 15.5 | medium |
| [`sounds/npcs/apache_die.ogg`](../AUDIO-PROFILES.md#soundsnpcsapache_dieogg) | A loud, heavy, noisy low-end crash and rumble, probably a helicopter going down, with three buzzy tonal pulses early on and a thin whistle that wavers as the sound decays over about 6 seconds. | rumble, noisy, boomy, mechanical | -12.8 / 4.6 | medium |
| [`sounds/npcs/apache_explode.ogg`](../AUDIO-PROFILES.md#soundsnpcsapache_explodeogg) | A massive, heavily clipped noise-based explosion with a boomy sub-heavy body that decays into a long rumbling debris tail over about 6 seconds. | explosion, boomy, noisy, rumble | -4.7 / 9.6 | high |
| [`sounds/npcs/apache_hit_a.ogg`](../AUDIO-PROFILES.md#soundsnpcsapache_hit_aogg) | A loud, low-heavy noisy crash or explosion-like impact with a rumbling sub body that decays over about 2.5 seconds into scattered debris-like rattles and thuds. | explosive, boomy, noisy, impact | -16.1 / 1.1 | medium |
| [`sounds/npcs/apache_hit_b.ogg`](../AUDIO-PROFILES.md#soundsnpcsapache_hit_bogg) | A loud, low-heavy explosive impact with a noisy crunchy burst at the start that collapses into a rumbling, crackling sub-bass tail over about 2.5 seconds. | explosive, boomy, rumble, noisy | -15.9 / 2.5 | medium |
| [`sounds/npcs/apache_rotor.ogg`](../AUDIO-PROFILES.md#soundsnpcsapache_rotorogg) | A continuous, low, gritty helicopter-rotor-like churn of dense bass thump and noise with no clear start or end, holding steady for about 3 seconds. | mechanical, rumbling, noisy, boomy | -10.3 / -3.4 | medium |
| [`sounds/npcs/cyberdemon_aggro_a.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_aggro_aogg) | A low, gritty, growl-like roar made of three rumbling vocal-like bursts that fade into a quiet tail, resembling a huge monstrous creature snarling. | growl, rumbling, gritty, low | -23.4 / -7.9 | medium |
| [`sounds/npcs/cyberdemon_aggro_b.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_aggro_bogg) | A roughly 3.9 second guttural, growly monster vocalization made of several low, rough bursts with formant-like harmonic bands, fading into a long quiet tail. | growl, vocal, creature, low-mid | -25.0 / -4.4 | medium |
| [`sounds/npcs/cyberdemon_aggro_c.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_aggro_cogg) | A low, growly monster vocalization with rough, guttural bursts that peak around 0.65s and then taper into a long, fading breathy rumble. | growl, vocal, guttural, creature | -23.1 / -2.8 | medium |
| [`sounds/npcs/cyberdemon_death_a.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_death_aogg) | A deep, growling, vocal-like monster death roar that swells in the low end, then sinks into a rumbling, fading tail. | growl, vocal, boomy, tonal | -15.9 / -2.2 | medium |
| [`sounds/npcs/cyberdemon_death_b.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_death_bogg) | A heavy, growling monster death roar that starts harsh and bright, swells into a low guttural bellow around 0.4-0.9s, then collapses into a fading, rumbling tail. | growl, roar, noisy, guttural | -16.5 / -1.5 | medium |
| [`sounds/npcs/cyberdemon_hurt_a.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_hurt_aogg) | A rough, guttural growl-roar of a large monster being hurt, with a dense low-mid body that swells quickly, holds for about half a second, then trails off into a quiet breathy tail. | growl, vocal, guttural, noisy | -15.0 / -0.6 | medium |
| [`sounds/npcs/cyberdemon_hurt_b.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_hurt_bogg) | A short, harsh, guttural growl-roar of a large monster being hurt, with dense low-mid body that swells to a peak around 0.3s and fades into a breathy tail by about 1.4s. | growl, vocal, creature, harsh | -15.1 / -0.1 | medium |
| [`sounds/npcs/cyberdemon_hurt_c.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_hurt_cogg) | A rough, growling monster pain vocalization with a dense low-mid body and a gritty harmonic texture, swelling quickly, then trailing off in a second smaller growl pulse. | growl, vocal, gritty, low-mid | -16.2 / -0.5 | medium |
| [`sounds/npcs/cyberdemon_idle_a.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_idle_aogg) | A low, growling, rumbling vocal-like rumble in two phases: a rough, gravelly bass growl for about a second, then a louder, more sustained and darker low-mid growl. | growl, rumbling, boomy, vocal | -22.4 / -7.5 | medium |
| [`sounds/npcs/cyberdemon_idle_b.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_idle_bogg) | A low, guttural, growling monster idle vocalisation made of two heavy rumbling phrases separated by a brief dip around 1.1s, with a formant-rich bass body. | growl, vocal, creature, boomy | -22.9 / -8.1 | medium |
| [`sounds/npcs/cyberdemon_idle_c.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_idle_cogg) | A low, gravelly growl-rumble of a large monster, made of a few weaker guttural bursts followed by a louder, sustained throaty roar in the second half. | growl, rumbling, low, guttural | -23.7 / -5.1 | medium |
| [`sounds/npcs/cyberdemon_step_a.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_step_aogg) | A heavy, low-end footstep thud for a huge monster: a rumbling, pulsing sub-bass impact that swells over about 0.1 s and decays into a short, crunchy, gritty tail. | boomy, heavy, thud, sub-bass | -23.6 / -4.1 | medium |
| [`sounds/npcs/cyberdemon_step_b.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_step_bogg) | A short, heavy, low-end footstep thud with a rumbling, pulsing body that decays quickly into a faint tail, like a huge creature stomping. | boomy, thud, transient, low-end | -24.0 / -0.2 | medium |
| [`sounds/npcs/cyberdemon_step_c.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_step_cogg) | A heavy, low-rumbling thud of a giant footstep with a few staggered impact pulses and a quick fading tail. | boomy, thud, heavy, sub-heavy | -21.4 / -2.5 | medium |
| [`sounds/npcs/cyberdemon_step_d.ogg`](../AUDIO-PROFILES.md#soundsnpcscyberdemon_step_dogg) | A heavy, low-frequency boomy footstep thud with a rumbling, crunchy body that swells to a peak around 0.15s and decays quickly, with faint high-frequency ticks at the very end. | boomy, heavy, thud, rumbling | -22.7 / -2.9 | medium |
| [`sounds/npcs/turret_beep-01.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_beep-01ogg) | A roughly 2-second robotic turret chatter made of a steady train of short electronic ticks over a constant 7.3 kHz whine, with a cluster of pitched, vocal-like beeps in the middle and a hard cutoff near 2.05 s. | electronic, beepy, tonal, metallic | -26.8 / -12.4 | medium |
| [`sounds/npcs/turret_beep-02.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_beep-02ogg) | A short, bright electronic double-beep: two quick tonal chirps with a faint tick between them, then the same pattern repeated about 0.38 s later, like a turret's targeting ping. | tonal, electronic, beep, bright | -31.3 / -14.0 | medium |
| [`sounds/npcs/turret_beep-03.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_beep-03ogg) | A very short electronic turret beep: a steady ~5.9 kHz pure tone chopped into a rapid stutter of about eight pulses that shrink in level, then a louder final blip with a brief tail. | electronic, tonal, beep, high-pitched | -25.3 / -18.5 | medium |
| [`sounds/npcs/turret_beep-04.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_beep-04ogg) | A rapid train of about thirteen short, bright electronic beeps with a metallic, buzzy tone, like a turret's warning chirp repeating at roughly 25 pulses per second. | beep, electronic, tonal, metallic | -24.5 / -15.1 | medium |
| [`sounds/npcs/turret_beep-05.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_beep-05ogg) | A very short, bright electronic turret beep made of a few tonal chirp segments with a mid-section dip and brief gaps, ending abruptly. | electronic, tonal, bright, beep | -70.0 / -16.9 | medium |
| [`sounds/npcs/turret_beep-06.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_beep-06ogg) | A very short electronic turret chirp: a rapid three-step ascending blip (about 1.5k, 3k, then 5k Hz) lasting roughly 0.07s, followed by a faint high-frequency ringing tail. | electronic, tonal, beep, chirp | -70.0 / -9.5 | medium |
| [`sounds/npcs/turret_beep-07.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_beep-07ogg) | A very short, extremely bright electronic chirp-buzz made of fast-pulsing high-frequency tones that cuts off abruptly at about 0.16 s and leaves a faint tail. | electronic, beep, bright, buzzy | -70.0 / -13.4 | medium |
| [`sounds/npcs/turret_gunequip-01.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_gunequip-01ogg) | A short burst of sharp metallic clicks and clacks, like a gun being cocked or a mechanism racking, building to a heavier clunk around 0.55 s and then fading quickly. | transient, metallic, mechanical, clicky | -14.8 / -3.0 | medium |
| [`sounds/npcs/turret_gunequip-02.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_gunequip-02ogg) | A short burst of clattery, metallic mechanical clicks and clunks, like a turret gun being cocked or loaded, with a few small ticks followed by two clusters of heavier rattles and a short tail. | mechanical, metallic, transient, clicky | -16.6 / -4.0 | medium |
| [`sounds/npcs/turret_gunequip-03.ogg`](../AUDIO-PROFILES.md#soundsnpcsturret_gunequip-03ogg) | A short burst of rapid, bright mechanical clicks and clacks, like a gun action being cocked or racked in several quick stages. | transient, mechanical, metallic, clicky | -15.0 / -3.8 | medium |
<!-- audio-profiles:end -->
