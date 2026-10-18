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
| `npcs.alienbugAggro` | `sounds/npcs/alienbug_aggro_a.ogg` (1.12 s, 44.1 kHz), `_b` (1.41 s), `_c` (1.28 s), `_d` (1.12 s), the last three at 22.05 kHz | While the bug has no target and a vulnerable player is within 16 blocks, even an unseen one. It can repeat every tick (bug NPC-07). | 1.0 / 1.0 |
| `npcs.cyberdemonIdle` | `sounds/npcs/cyberdemon_idle_a.ogg`, `_b`, `_c` (1.9–2.1 s) | Cyberdemon and all Super Mutants, ambient | default |
| `npcs.cyberdemonHurt` | `sounds/npcs/cyberdemon_hurt_a.ogg`, `_b`, `_c` (1.5–1.7 s) | Cyberdemon and Super Mutants take damage | default |
| `npcs.cyberdemonDeath` | `sounds/npcs/cyberdemon_death_a.ogg` (2.18 s), `_b` (2.65 s), 48 kHz | Cyberdemon and Super Mutants die | default |
| `npcs.cyberdemonStep` | `sounds/npcs/cyberdemon_step_a.ogg` … `_d` (4 files, 0.53 s each) | Each footstep of the Cyberdemon and Super Mutants, played *in addition to* the block step sound | 0.15 / 1.0 |
| `npcs.cyberdemonAggro` | `sounds/npcs/cyberdemon_aggro_a.ogg` (3.31 s), `_b` (3.89 s), `_c` (3.93 s) | **Never in practice.** It is coded in `findPlayerToAttack` of the Cyberdemon and Super Mutants, a legacy hook that AI-enabled mobs never call (bug NPC-06). The intent was a roar when the mob acquires a player. | 1.0 / 1.0 |
| `npcs.apacheRotor` | `sounds/npcs/apache_rotor.ogg` (3.07 s, 22.05 kHz) | Attack Helicopter rotor loop. It is restarted every 61 ticks (≈3.05 s) while alive (`deathTime == 0`). | **6.0** / 1.0 |
| `npcs.apacheBurst` | `sounds/npcs/apache_burst.ogg` (2.22 s) | Start of each machine-gun burst (`attackCounter == 16`). It covers all 7 shots, which have no individual sounds. | 5.0 / 1.0 |
| `npcs.apacheHit` | `sounds/npcs/apache_hit_a.ogg` (3.25 s), `_b` (3.05 s) | Helicopter takes damage (hurt sound) | entity volume **10.0** |
| `npcs.apacheDie` | `sounds/npcs/apache_die.ogg` (5.99 s, 48 kHz) | The first tick of the helicopter's 100-tick death sequence (a crash or alarm wind-down) | 6.0 / 1.0 |
| `npcs.apacheExplode` | `sounds/npcs/apache_explode.ogg` (6.79 s) | Final explosion at death tick 100, when the entity is removed | 6.0 / 1.0 |
| `npcs.turretBeep` | `sounds/npcs/turret_beep-01.ogg` (2.24 s), `-02` (0.62 s), `-03` (0.84 s), `-04` (0.55 s), `-05` (0.32 s), `-06` (0.31 s), `-07` (0.26 s), 48 kHz | **Unused.** No code plays this event. It was presumably meant as the turret targeting or acquire beep (see NPC-27). | – |
| `npcs.turretGunequip` | `sounds/npcs/turret_gunequip-01.ogg` (0.81 s), `-02` (0.88 s), `-03` (0.50 s), 48 kHz | **Unused.** No code plays this event. It was presumably meant to play when a gun is fitted to a turret (see NPC-27). | – |

That accounts for 54 files: 23 alien bug, 6 apache, 15 cyberdemon and 10 turret.

The helicopter's rocket shot also plays `techguns:guns.rocketFire` (volume 5; owned by 02/03) and vanilla aux SFX **1008** (the ghast fireball whoosh). Its coded death sound `npcs.apacheHit` lacks the `techguns:` domain, so it resolves to a missing vanilla sound (NPC-08). Its ambient sound is the empty string (NPC-09).

## Vanilla sounds used by this area's mobs

| Mob(s) | Idle | Hurt | Death | Extra step sound (volume 0.15) |
|---|---|---|---|---|
| GenericNPC default: Army Soldier, Bandit, Outcast, Commando, Stormtrooper | `mob.villager.idle` | `mob.villager.hit` | `mob.villager.death` | – |
| Psycho Steve, Dictator Dave | villager set | villager set | villager set | `mob.zombie.step` |
| Zombie Soldier, Zombie Farmer, Zombie Miner | `mob.zombie.say` | `mob.zombie.hurt` | `mob.zombie.death` | `mob.zombie.step` |
| Zombie Pigman Soldier | `mob.zombiepig.zpig` | `mob.zombiepig.zpighurt` | `mob.zombiepig.zpigdeath` | `mob.zombie.step` |
| Skeleton Soldier | `mob.skeleton.say` | `mob.skeleton.hurt` | `mob.skeleton.death` | `mob.skeleton.step` (replaces the block step) |
| Turret | none | `mob.irongolem.hit` | `mob.irongolem.death` | none (step suppressed) |

The NPCs' gunfire, reload and similar sounds are each gun's own sounds (02/03). `fireWeaponFromNPC` plays the fire sound at the shooter's gun position.

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
