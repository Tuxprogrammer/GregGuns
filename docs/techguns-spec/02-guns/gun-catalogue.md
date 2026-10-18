# 02-guns: gun catalogue

All values come from `java/techguns/TGuns.java` (the constructor plus the chained setters) after every setter has been applied. These are also the **defaults** written to the weapon-stats config (see [Config](#weapon-stats-config)). For behaviour that differs per class, see [per-gun-behaviour.md](per-gun-behaviour.md).

## How to read the constructor

Every `GenericGun` subclass is built with the same 11 positional arguments:

| Pos | Field | Meaning |
|---|---|---|
| 1 | `name` | Unlocalized name. The lang key is `techguns.item.<name>.name`. |
| 2 | `ammo` | ItemStack consumed per reload step. Akimbo guns double its stack size. |
| 3 | `semiAuto` | If true, the client clears the "fire held" flag after each trigger evaluation, so you have to click once per shot. |
| 4 | `minFiretime` | Ticks between shots. This is the value the per-player `fireDelay` gets set to on each shot. |
| 5 | `clipsize` | Rounds in a full gun. Also used as `maxDamage`, which only matters for encoding crafting output (see lifecycle). |
| 6 | `reloadtime` | Reload length in ticks. After a reload starts, `fireDelay` is set to `reloadtime - minFiretime`. |
| 7 | `damage` | Maximum damage per projectile (half-hearts). |
| 8 | `firesound` | Sound event played on each shot. |
| 9 | `reloadsound` | Sound event played when a reload starts. |
| 10 | `TTL` | Projectile lifetime in ticks. Nominal reach is `speed × TTL` blocks. |
| 11 | `accuracy` | Spread value. The projectile's yaw and pitch each get an independent uniform random offset in ±`spread × 40`°. |

Defaults when no setter is called: `damageMin = damage`, `damageDropStart = damageDropEnd = TTL` (no drop-off), `ammoCount = 1`, `speed = 1.5` blocks/tick, `recoiltime = 5`, `muzzleFlashtime = 5`, `penetration = 0`, no zoom, `shootWithLeftClick = true`, AI range 15 / attackTime 60 / burst 0 / burstDelay 0.

Setters and what they set:

| Setter | Effect |
|---|---|
| `setEmptyMag(empty, bullet, bulletsPerMag)` | On reload the gun returns `empty` to the player (2 for akimbo guns). For forced-reload refunds it sets `shotsPerBullet = clip / bulletsPerMag`. |
| `setAmmoCount(n)` | The gun loads up to `n` ammo items per reload (shell-by-shell guns). Also sets `shotsPerBullet = clip / n`. |
| `setShotgunSpread(count, spread, burst)` | Each trigger pull spawns **1 + count** projectiles. The extra `count` projectiles use `spread` instead of `accuracy`. When `burst` is true, extra projectile *i* (1-based) is pushed forward by `i / count` of one tick's motion, so the shots form a line (burst fire). |
| `setZoom(mult, toggle, bonus)` | Scoped. FOV is multiplied by `mult` while zoomed, and spread is multiplied by `bonus` for shots fired while zoomed. `toggle=true` means right-click toggles the zoom. Every gun uses toggle mode. |
| `setSpeed(m)` | `speed *= m`, so the base 1.5 is multiplied. |
| `setDamageDrop(start, end, min)` | Damage drop-off by distance travelled, in blocks. |
| `setPenetration(p)` | Armour penetration, 0..1. |
| `setRecoiltime(t)` / `setMuzzleFlashTime(t)` | Client animation durations in ticks, converted to ms as t/20×1000. |
| `setCheckRecoil()` | The recoil animation only restarts if the previous one has finished. Used by fast-firing guns. |
| `setRechamberSound(s)` | An extra sound played after every shot. |
| `setFiresoundStart(s)` + `setMaxLoopDelay(n)` | Looping-sound guns. The start sound plays when no shot was fired in the last `n` ticks; otherwise the loop sound plays. |
| `setSilenced(true)` | The projectile is flagged as silenced (see 04-projectiles-fx). |
| `setAkimbo(doubleShot)` | Dual-wield. Doubles the ammo stack size. `doubleShot=false` alternates right/left guns. `doubleShot=true` fires both at once and uses 2 rounds. |
| `setAIStats(range, attackTime, burstCount, burstDelay)` | NPC/turret ranged-AI parameters. |
| `setTexture(path)` / `setTextures(path, n)` | Model texture plus `n` camo variants (`path.png`, `path_1.png` … `path_{n-1}.png`). `camoCount = n`. Documented in 03. |
| `setMuzzleParticle(i[,x,y,z])`, `setMuzzleParticleOffsetTurret`, `setTurretPosOffset` | Visual hooks (see 03 and 10). |
| `setTool(class, level)`, `setDigSpeed(f)`, `setShootWithLeftClick(false)`, `setMeleeDmg(full, empty)` | Tool-gun settings (see per-gun-behaviour). |

Range constants used for AI stats: `RANGE_MELEE=3`, `RANGE_CLOSE=12`, `RANGE_SHORT=18`, `RANGE_MEDIUM=24`, `RANGE_FAR=36` (blocks).

---

## Table A: identity and ammunition

"Ammo / reload" is the stack consumed when a reload starts. Unless noted, one item fully loads the gun. "Refund" is the bullet item and the rounds-per-item ratio used to refund bullets on a forced reload (only for guns with an empty magazine and a bullet item).

| # | Unlocalized name | Registry name | Display name (en_US) | Class | Ammo / reload | Clip | Empty returned | Refund (bullet, rounds per item) |
|---|---|---|---|---|---|---|---|---|
| 1 | `handcannon` | handcannon | Hand Cannon | Handgun | 1× Stone Bullet | 1 | – | – |
| 2 | `pistol` | pistol | 9mm Pistol | GenericGun | 1× 9mm Pistol Magazine | 18 | 9mm Pistol Magazine (Empty) | 9mm Bullets, 6 |
| 3 | `sawedoff` | sawedoff | Sawed Off | GenericGun | Shotgun Shell, up to 2 per reload | 2 | – | – |
| 4 | `boltaction` | boltaction | Bolt Action Rifle | GenericGun | 1× Rifle Bullets | 6 | – | – |
| 5 | `m4` | m4 | M4 Assault Rifle | GenericGun | 1× Assault Rifle Magazine | 30 | Assault Rifle Magazine (Empty) | Rifle Bullets, 10 |
| 6 | `flamethrower` | flamethrower | Flamethrower | Flamethrower | 1× Fuel Tank | 100 | Fuel Tank (Empty) | (bullet null, no refund) |
| 7 | `combatshotgun` | combatshotgun | Combat Shotgun | GenericGun | Shotgun Shell, up to 8 per reload | 8 | – | – |
| 8 | `lasergun` | lasergun | Laser Rifle | Lasergun | 1× Energy Cell | 45 | Energy Cell (Empty) | – |
| 9 | `rocketlauncher` | rocketlauncher | Rocket Launcher | RocketLauncher | 1× Rocket | 1 | – | – |
| 10 | `grimreaper` | grimreaper | Grim Reaper | RocketLauncher | Rocket, up to 4 per reload | 4 | – | – |
| 11 | `biogun` | biogun | Bio Gun | Biogun (charge) | 1× Bio Tank | 30 | Bio Tank (Empty) | – |
| 12 | `thompson` | thompson | Submachine Gun | GenericGun | 1× SMG Magazine | 20 | SMG Magazine (Empty) | 9mm Bullets, 10 |
| 13 | `revolver` | revolver | Revolver | GenericGun | 1× 9mm Bullets | 6 | – | – |
| 14 | `ak47` | ak47 | Kalaschnikow | GenericGun | 1× Assault Rifle Magazine | 30 | Assault Rifle Magazine (Empty) | Rifle Bullets, 10 |
| 15 | `as50` | as50 | AS50 Sniper Rifle | GenericGun | 1× AS50 Magazine | 10 | AS50 Magazine (Empty) | High Caliber Rounds, 5 |
| 16 | `teslagun` | teslagun | Tesla Rifle | Teslagun | 1× Energy Cell | 25 | Energy Cell (Empty) | – |
| 17 | `pdw` | pdw | PDW | GenericGun | 1× Advanced Magazine | 20 | Advanced Magazine (Empty) | Advanced Bullets, 6.67 |
| 18 | `lmg` | lmg | LMG | GenericGun | 1× LMG Magazine | 100 | LMG Magazine (Empty) | Rifle Bullets, 12.5 |
| 19 | `m4_uq` | m4_uq | The Infiltrator | GenericGun | 1× Assault Rifle Magazine | 30 | Assault Rifle Magazine (Empty) | Rifle Bullets, 10 |
| 20 | `goldenrevolver` | goldenrevolver | Golden Revolver | GenericGun | 1× 9mm Bullets | 6 | – | – |
| 21 | `pulseRifle` | pulserifle | Pulse Rifle | GenericGun | 1× Advanced Magazine | 12 | Advanced Magazine (Empty) | Advanced Bullets, 4 |
| 22 | `alienBlaster` | alienBlaster | Alien Blaster | AlienBlaster | 1× Energy Cell | 10 | Energy Cell (Empty) | – |
| 23 | `cyberdemonBlaster` | cyberdemonBlaster | Nether Blaster | CyberdemonBlaster | 1× Nether Charge | 10 | – (no empty item) | – |
| 24 | `minigun` | minigun | Minigun | GenericGun | 1× Minigun Ammo Drum | 200 | Minigun Ammo Drum (empty) | Rifle Bullets, 12.5 |
| 25 | `blasterRifle` | blasterRifle | Blaster Rifle | BlasterRifle | 1× Energy Cell | 50 | Energy Cell (Empty) | – |
| 26 | `powerHammer` | powerHammer | Power Hammer | PowerHammer (charge, tool) | 1× Compressed Air Tank | 300 | Compressed Air Tank (Empty) | – |
| 27 | `nukelauncher` | nukelauncher | Nuke Launcher | NukeLauncher | 1× Rocket (Tactical Nuke) | 1 | – | – |
| 28 | `powerHammerAdv` | powerHammerAdv | Power Hammer (Advanced) | PowerHammer | 1× Compressed Air Tank | 300 | Compressed Air Tank (Empty) | – |
| 29 | `grenadelauncher` | grenadelauncher | Grenade Launcher | GrenadeLauncher | 40mm Grenade, up to 6 per reload | 6 | – | – |
| 30 | `aug` | aug | AUG | GenericGun | 1× Assault Rifle Magazine | 30 | Assault Rifle Magazine (Empty) | Rifle Bullets, 10 |
| 31 | `sonicShotgun` | sonicShotgun | Sonic Rifle | SonicShotgun | 1× Energy Cell | 8 | Energy Cell (Empty) | – |
| 32 | `chainsaw` | chainsaw | Chainsaw | Chainsaw (tool) | 1× Fuel Tank | 300 | Fuel Tank (Empty) | – |
| 33 | `scatterBeamGun` | scatterBeamGun | Scatter Laser Rifle [WIP] | **Lasergun** (not ScatterBeamGun) | 1× Energy Cell | 40 | Energy Cell (Empty) | – |
| 34 | `nuclearDeathRay` | **beamtestgun** | Nuclear Death Ray | Beamgun | 1× Nuclear Power Cell | 100 | Nuclear Power Cell (Depleted) | – |
| 35 | `mac10` | mac10 | Mac10 | GenericGun | 1× SMG Magazine | 32 | SMG Magazine (Empty) | 9mm Bullets, 16 |
| 36 | `pistolAkimbo` | pistolAkimbo | Akimbo Pistols | GenericGun (akimbo, alternate) | **2×** 9mm Pistol Magazine | 36 | 2× 9mm Pistol Magazine (Empty) | 9mm Bullets, 6 |
| 37 | `revolverAkimbo` | revolverAkimbo | Akimbo Revolvers | GenericGun (akimbo, alternate) | **2×** 9mm Bullets | 12 | – | – |
| 38 | `goldenrevolverAkimbo` | goldenrevolverAkimbo | ` Akimbo Golden Revolvers` (the lang value has a leading space) | GenericGun (akimbo, alternate) | **2×** 9mm Bullets | 12 | – | – |
| 39 | `mac10Akimbo` | mac10Akimbo | Akimbo Mac10s | GenericGun (akimbo, double-shot) | **2×** SMG Magazine | 64 | 2× SMG Magazine (Empty) | 9mm Bullets, 32 (see GUN-14) |
| 40 | `mibGun` | mibGun | De-Atomizer Pistol | MibGun | 1× Energy Cell | 20 | Energy Cell (Empty) | – |
| 41 | `scar` | scar | SCAR | GenericGun | 1× Assault Rifle Magazine | 20 | Assault Rifle Magazine (Empty) | Rifle Bullets, 6.67 |
| 42 | `vector` | vector | Vector | GenericGun | 1× SMG Magazine | 25 | SMG Magazine (Empty) | 9mm Bullets, 12.5 |
| 43 | `mibGunAkimbo` | mibGunAkimbo | Akimbo De-Atomizer Pistols | MibGun (akimbo, alternate) | **2×** Energy Cell | 40 | 2× Energy Cell (Empty) | – |

Ammo items are meta-values of the shared item (documented in 06-items). "Empty returned" is only handed back when a reload actually consumes ammo. Guns with a `null` bullet but an empty container (energy and fuel guns) never refund on a forced reload: `shotsPerBullet` is `clip/0 = ∞` or `clip/1 = clip`, so the floor is always 0.

---

## Table B: damage and ballistics

`speed` is in blocks/tick. Reach is `speed × TTL` (nominal; gravity projectiles fall short). "Spread ±°" is `accuracy × 40`, applied independently to yaw and pitch. "Pellets" is the extra projectiles per shot (total = 1 + pellets). "Tooltip type" comes from `getDamageType()`. "Hit source" is the TGDamageSource the projectile actually deals (owned by 04, summarised here).

| Gun | Dmg | Min | Drop start→end (blocks) | TTL | Speed | Reach | Accuracy | ±° | Pellets (spread) | Pen | Tooltip type | Hit source (name / type) | Projectile entity |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| handcannon | 8 | 5 | 10→25 | 25 | 1.0 (fixed) | 25 | 0.035 | 1.4 | – | 0 | Projectile | tg_bullet / PROJECTILE | SimpleProjectile (gravity 0.015, texture `textures/entity/handgunBullet.png`) |
| pistol | 8 | 5 | 18→25 | 30 | 1.5 | 45 | 0.025 | 1.0 | – | 0 | Projectile | tg_bullet | GenericProjectile |
| sawedoff | 4 | 1.5 | 1→4 | 10 | 1.5 | 15 | 0.01 | 0.4 | 7 (0.2 → ±8°) | 0 | Projectile | tg_bullet | GenericProjectile ×8 |
| boltaction | 16 | 10 | 20→30 | 40 | 2.25 | 90 | 0.05 | 2.0 | – | 0.15 | Projectile | tg_bullet | GenericProjectile |
| m4 | 8 | 6 | 25→35 | 35 | 1.5 | 52.5 | 0.015 | 0.6 | – | 0.15 | Projectile | tg_bullet | GenericProjectile |
| flamethrower | 5 | 2 | 4→16 | 16 | 1.0 (fixed) | 16 | 0.05 | 2.0 | – | 0 | Fire | tg_fire / FIRE | FlamethrowerProjectile (gravity 0.01, ignite = not safe-mode) |
| combatshotgun | 4 | 1.5 | 2→5 | 15 | 1.5 | 22.5 | 0.01 | 0.4 | 7 (0.15 → ±6°) | 0.15 | Projectile | tg_bullet | GenericProjectile ×8 |
| lasergun | 12 | 12 | none (5→5) | 5 | 7.5 | 37.5 | 0.0 | 0 | – | 0 | Energy | tg_energy / ENERGY | LaserProjectile |
| rocketlauncher | 50 | – | n/a | 100 | 1.0 (fixed) | 100 | 0.05 | 2.0 | – | 0 | Explosion | tg_explosion | RocketProjectile (radius 1.75, gravity 0.01) |
| grimreaper | 50 | – | n/a | 100 | 1.0 (fixed) | 100 | 0.05 | 2.0 | – | 0 | Explosion | tg_explosion | RocketProjectile (radius 1.75, gravity 0.01) |
| biogun | 10 | 8 | 8→15 | 35 | 1.0 (fixed) | 35 | 0.015 | 0.6 | – | 0.15 | Poison | tg_poison / POISON | BiogunProjectile (level 1–3) |
| thompson | 5 | 3 | 15→24 | 25 | 1.5 | 37.5 | 0.05 | 2.0 | – | 0 | Projectile | tg_bullet | GenericProjectile |
| revolver | 8 | 6 | 12→20 | 25 | 1.5 | 37.5 | 0.025 | 1.0 | – | 0 | Projectile | tg_bullet | GenericProjectile |
| ak47 | 9 | 5 | 20→30 | 35 | 1.5 | 52.5 | 0.030 | 1.2 | – | 0.15 | Projectile | tg_bullet | GenericProjectile |
| as50 | 32 | 24 | 30→40 | 50 | 2.25 | 112.5 | 0.0625 | 2.5 | – | 0.25 | Projectile | tg_bullet | GenericProjectile |
| teslagun | 12 | 12 | none (10→10) | 10 (fixed) | 5.0 (fixed) | 50 | 0.0 (ignored) | 0 | – | 0 | Lightning | tg_lightning / LIGHTNING | TeslaProjectile (chains 3×, chain drop 0) + TeslaBeam visual |
| pdw | 6 | 3 | 16→25 | 25 | 1.5 | 37.5 | 0.03 | 1.2 | 1 burst (0.03) | 0.25 | Projectile | tg_bullet | GenericProjectile ×2 in a line |
| lmg | 8 | 6 | 30→40 | 50 | 1.5 | 75 | 0.020 | 0.8 | – | 0.15 | Projectile | tg_bullet | GenericProjectile |
| m4_uq | 8 | 6 | 25→35 | 35 | 1.5 | 52.5 | 0.010 | 0.4 | – | 0.15 | Projectile | tg_bullet | GenericProjectile (silenced) |
| goldenrevolver | 14 | 6 | 12→30 | 25 | 1.5 | 37.5 | 0.015 | 0.6 | – | 0 | Projectile | tg_bullet | GenericProjectile |
| pulseRifle | 10 | 8 | 25→35 | 50 | 2.25 | 112.5 | 0.024 | 0.96 | 2 burst (0.015) | 0.25 | Projectile | tg_bullet | GenericProjectile ×3 in a line |
| alienBlaster | 16 | 16 | none (50→50) | 50 | 1.5 | 75 | 0.0 | 0 | – | 0.2 | Fire | tg_fire / FIRE | AlienBlasterProjectile (ignite = not safe-mode; false for NPCs) + "AlienBlasterTrail" particle |
| cyberdemonBlaster | 14 | 8 | 15→30 | 50 | 0.85 (fixed) | 42.5 | 0.0 | 0 | – | 0.15 | Fire | tg_fire / FIRE (knockback 1) | CyberdemonBlasterProjectile (gravity 0, ignite = not safe-mode; **true** for NPCs) + "CyberDemonBlasterTrail" |
| minigun | 5 | 3 | 20→40 | 50 | 1.5 | 75 | 0.025 | 1.0 | – | 0.15 | Projectile | tg_bullet | GenericProjectile |
| blasterRifle | 10 | 8 | 25→35 | 50 | 1.5 | 75 | 0.025 | 1.0 | – | 0.2 | Energy | tg_energy | BlasterRifleProjectile |
| powerHammer | 2.5 × air used (1–10) | 2.5 | 3→3 | 2 | 1.5 | 3 | 0 | 0 | – | 0 | Physical | powerhammer / PHYSICAL | PowerHammerProjectile (4×4 hitbox) |
| nukelauncher | 250 | – | n/a | 100 | 1.0 (fixed) | 100 | 0.05 | 2.0 | – | 0 | Explosion | tg_explosion (knockback 10) | NukeRocketProjectile (radius 10, gravity 0.01) |
| powerHammerAdv | 2.5 × air used | 2.5 | none (2→2) | 2 | 1.5 | 3 | 0 | 0 | – | 0 | Physical | powerhammer | PowerHammerProjectile |
| grenadelauncher | 30 (fixed, unscaled) | – | n/a | 35 (ignored, uses 100) | 1.0 (own field) | ~100 (ballistic) | 0.015 (ignored, uses 0.05) | 2.0 | – | 0 (fixed) | Explosion | tg_explosion | GrenadeLauncherProjectile (radius 1.5, gravity 0.01, 2 bounces) + "GrenadeLauncherTrail" |
| aug | 8 | 7 | 25→40 | 35 | 1.5 | 52.5 | 0.010 | 0.4 | – | 0.15 | Projectile | tg_bullet | GenericProjectile |
| sonicShotgun | 25 | 5 | 5→15 | 20 | 1.5 | 30 | 0.0 (ignored) | fixed rings | 15 extra in 2 rings | 0.25 | **Projectile** (wrong, see GUN-20) | tg_sonic / PHYSICAL (knockback 2) | SonicShotgunProjectile ×16 + "SonicShotgunTrail" |
| chainsaw | 10 | 10 | none (2→2) | 2 | 1.5 | 3 | 0 | 0 | – | 0.25 | Physical | chainsaw / PHYSICAL | InvisibleProjectile |
| scatterBeamGun | 6 | 6 | none (5→5) | 5 | 7.5 | 37.5 | 0.1 | 4.0 | 4 (0.15 → ±6°) | 0 | Energy | tg_energy | LaserProjectile ×5 |
| nuclearDeathRay | 6 | 1 | 20→40 | 5 | 7.5 | 37.5 | 0.0 | 0 | – | 0.35 | Radiation | tg_rad / RADIATION | MovingBeam |
| mac10 | 5 | 3 | 15→24 | 25 | 1.5 | 37.5 | 0.05 | 2.0 | – | 0 | Projectile | tg_bullet | GenericProjectile |
| pistolAkimbo | 8 | 5 | 18→25 | 30 | 1.5 | 45 | 0.035 | 1.4 | – | 0 | Projectile | tg_bullet | GenericProjectile (alternating hand) |
| revolverAkimbo | 8 | 6 | 12→20 | 25 | 1.5 | 37.5 | 0.035 | 1.4 | – | 0 | Projectile | tg_bullet | GenericProjectile (alternating) |
| goldenrevolverAkimbo | **16** | 6 | 15→30 | 25 | 1.5 | 37.5 | 0.035 | 1.4 | – | 0 | Projectile | tg_bullet | GenericProjectile (alternating) |
| mac10Akimbo | 5 | 3 | 15→24 | 25 | 1.5 | 37.5 | 0.07 | 2.8 | – | 0 | Projectile | tg_bullet | GenericProjectile ×2 (both hands) |
| mibGun | 16 | 8 | 20→30 | 50 | 1.5 | 75 | 0.035 | 1.4 | – | 0.2 | Energy | tg_energy (gore 1.0) | EnergyBlastProjectile + "BlueBlasterTrail" |
| scar | 12 | 10 | 30→40 | 50 | 1.5 | 75 | 0.015 | 0.6 | – | 0.25 | Projectile | tg_bullet | GenericProjectile |
| vector | 6 | 4 | 17→25 | 30 | 1.5 | 45 | 0.05 | 2.0 | – | 0.15 | Projectile | tg_bullet | GenericProjectile |
| mibGunAkimbo | 16 | 8 | 20→30 | 50 | 1.5 | 75 | 0.040 | 1.6 | – | 0.2 | Energy | tg_energy | EnergyBlastProjectile (alternating) |

Drop-off formula (implemented in the projectile, 04). With `d` the straight-line distance from the projectile's first-tick position to its current position: if `d ≤ start` the damage is `damage`; if `d > end` it is `damageMin`; otherwise it interpolates linearly from `damage` down to `damageMin`. When NPCs fire, `damage` and `damageMin` are multiplied by the NPC damage factor (see lifecycle), except for the guns that ignore it.

---

## Table C: handling

Shots/s is `20 / minFiretime` (minFiretime 0 means one shot every tick). Reload ticks is the reload length; once a reload starts, the next shot is possible after `reloadtime − minFiretime` ticks. Recoil and flash are client animation lengths in ticks.

| Gun | Trigger | Semi | minFiretime | Shots/s | Reload (ticks / s) | Zoom FOV mult | Zoom spread mult | Recoil | Flash | checkRecoil | Fire sound | Start/loop | Reload sound | Rechamber |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| handcannon | LMB | no | 12 | 1.67 | 30 / 1.5 | – | – | 5 | 5 | | guns.handgunFire | | guns.handgunReload | |
| pistol | LMB | yes | 4 | 5 | 35 / 1.75 | – | – | 3 | 5 | | guns.pistolFire | | guns.pistolReload | |
| sawedoff | LMB | no | 4 | 5 | 28 / 1.4 | – | – | 5 | 5 | | guns.sawedoffFire | | guns.sawedoffReload | |
| boltaction | LMB | yes | 25 | 0.8 | 50 / 2.5 | 0.35 | 0.125 | 15 | 5 | | guns.boltactionFire | | guns.boltactionReload | guns.boltactionRechamber |
| m4 | LMB | no | 3 | 6.67 | 45 / 2.25 | 0.75 | 0.75 | 5 | 5 | | guns.assaultRifleFire | | guns.assaultRifleReload | |
| flamethrower | LMB | no | 2 | 10 | 45 / 2.25 | – | – | 5 | 5 | | guns.flamethrowerFire | start guns.flamethrowerStart, loop delay 10 | guns.flamethrowerReload | |
| combatshotgun | LMB | no | 14 | 1.43 | 50 / 2.5 | – | – | 12 | 5 | | guns.combatshotgunFire | | guns.combatshotgunReload | guns.combatshotgunPump |
| lasergun | LMB | no | 5 | 4 | 45 / 2.25 | 0.75 | 0.75 | 5 | 5 | | guns.lasergunFire | | guns.lasergunReload | |
| rocketlauncher | LMB | no | 10 | 2 | 40 / 2.0 | – | – | 10 | 5 | | guns.rocketFire | | guns.rocketReload | |
| grimreaper | LMB | no | 6 | 3.33 | 40 / 2.0 | – | – | 10 | 5 | | guns.rocketFire | | guns.rocketReload | |
| biogun | LMB auto + RMB charge | no | 6 | 3.33 | 45 / 2.25 | – | – | 5 | 5 | | guns.biogunFire | | guns.biogunReload | |
| thompson | LMB | no | 3 | 6.67 | 40 / 2.0 | – | – | 5 | 5 | | guns.thompsonFire | | guns.thompsonReload | |
| revolver | LMB | yes | 6 | 3.33 | 45 / 2.25 | – | – | 6 | 5 | | guns.revolverFire | | guns.revolverReload | |
| ak47 | LMB | no | 3 | 6.67 | 45 / 2.25 | – | – | 5 | 5 | | guns.ak47Fire | | guns.ak47Reload | |
| as50 | LMB | yes | 10 | 2 | 80 / 4.0 | 0.35 | 0.125 | 10 | 5 | | guns.as50Fire | | guns.as50Reload | |
| teslagun | LMB | no | 8 | 2.5 | 45 / 2.25 | 0.75 | 1.0 | 5 | 10 | | guns.teslaFire | | guns.teslaReload | |
| pdw | LMB | no | 2 | 10 (×2 rounds) | 40 / 2.0 | – | – | 5 | 5 | yes | guns.pdwFire | | guns.pdwReload | |
| lmg | LMB | no | 2 | 10 | 100 / 5.0 | 0.75 | 0.75 | 5 | 5 | | guns.lmgFire | | guns.lmgReload | |
| m4_uq | LMB | no | 3 | 6.67 | 45 / 2.25 | 0.50 | 0.5 | 5 | 5 | | guns.silencedM4Fire | | guns.assaultRifleReload | |
| goldenrevolver | LMB | yes | 8 | 2.5 | 45 / 2.25 | – | – | 8 | 5 | | guns.goldenrevolverFire | | guns.revolverReload | |
| pulseRifle | LMB | no | 7 | 2.86 (×3 rounds) | 45 / 2.25 | 0.35 | 0.5 | 8 | 5 | | guns.pulserifleFire | | guns.pulserifleReload | |
| alienBlaster | LMB | no | 8 | 2.5 | 35 / 1.75 | – | – | 5 | 10 | | guns.alienblasterFire | | guns.alienblasterReload | |
| cyberdemonBlaster | LMB | no | 8 | 2.5 | 35 / 1.75 | – | – | 5 | 10 | | guns.cyberdemonblasterFire | | guns.teslaReload | |
| minigun | LMB | no | 0 | 20 | 100 / 5.0 | – | – | 5 | 5 | yes | guns.minigunFire | | guns.minigunReload | |
| blasterRifle | LMB | no | 5 | 4 | 45 / 2.25 | 0.5 | 0.75 | 5 | 5 | | guns.blasterrifleFire | | guns.lasergunReload | |
| powerHammer | RMB charge, LMB melee/mine | no | 4 | – | 45 / 2.25 | – | – | 12 | 5 | | guns.powerhammerFire (on release) | charge sound guns.powerhammerCharge | guns.powerhammerReload | |
| nukelauncher | LMB | no | 10 | 2 | 40 / 2.0 | – | – | 10 | 5 | | guns.rocketFire | | guns.rocketReload | |
| powerHammerAdv | as powerHammer | no | 4 | – | 45 / 2.25 | – | – | 12 | 5 | | guns.powerhammerFire | guns.powerhammerCharge | guns.powerhammerReload | |
| grenadelauncher | LMB | yes | 5 | 4 | 100 / 5.0 | – | – | 5 | 5 | | guns.grenadeLauncherFire | | guns.grenadeLauncherReload | |
| aug | LMB | no | 3 | 6.67 | 45 / 2.25 | 0.50 | 0.5 | 5 | 5 | | guns.augFire | | guns.augReload | |
| sonicShotgun | LMB | yes | 12 | 1.67 | 40 / 2.0 | – | – | 5 | 5 | | guns.sonicshotgunFire | | guns.sonicshotgunReload | |
| chainsaw | RMB cut, LMB melee/mine | no | 3 | 6.67 | 45 / 2.25 | – | – | 5 | 5 | | guns.chainsawLoop | start guns.chainsawLoopStart, loop delay 10 | guns.powerhammerReload | |
| scatterBeamGun | LMB | no | 7 | 2.86 | 45 / 2.25 | 0.75 | 0.75 | 5 | 5 | | guns.lasergunFire | | guns.lasergunReload | |
| nuclearDeathRay | LMB | no | 5 | 4 | 30 / 1.5 | – | – | 5 | 5 | | guns.beamgunFire | start guns.beamgunStart, loop delay 10 | guns.lasergunReload | |
| mac10 | LMB | no | 2 | 10 | 40 / 2.0 | – | – | 2 | 5 | | guns.mac10Fire | | guns.mac10Reload | |
| pistolAkimbo | LMB | yes | 3 | 6.67 | 52 / 2.6 | – | – | 3 | 3 | | guns.pistolFire | | guns.pistolReloadAkimbo | |
| revolverAkimbo | LMB | yes | 4 | 5 | 70 / 3.5 | – | – | 6 | 5 | | guns.revolverFire | | guns.revolverReloadAkimbo | |
| goldenrevolverAkimbo | LMB | yes | 6 | 3.33 | 70 / 3.5 | – | – | 8 | 5 | | guns.goldenrevolverFire | | guns.revolverReloadAkimbo | |
| mac10Akimbo | LMB | no | 2 | 10 (×2 rounds) | 68 / 3.4 | – | – | 2 | 5 | | guns.mac10Fire | | guns.mac10ReloadAkimbo | |
| mibGun | LMB | yes | 8 | 2.5 | 45 / 2.25 | – | – | 5 | 5 | | guns.MIBblasterFire | | guns.MIBblasterReload | |
| scar | LMB | no | 4 | 5 | 45 / 2.25 | 0.65 | 0.5 | 5 | 5 | | guns.scarFire | | guns.scarReload | |
| vector | LMB | no | 2 | 10 | 40 / 2.0 | 0.75 | 0.35 | 2 | 5 | | guns.vectorFire | | guns.vectorReload | |
| mibGunAkimbo | LMB | yes | 5 | 4 | 60 / 3.0 | – | – | 5 | 5 | | guns.MIBblasterFire | | guns.MIBblasterReloadAkimbo | |

All sound events are prefixed with the `techguns:` namespace. Every one of them exists in `sounds.json`. The files behind them are documented in 03-gun-visuals-audio.

---

## Table D: AI, visual hooks and textures

AI columns follow `setAIStats(range, attackTime, burstCount, burstDelay)` (see lifecycle → NPC firing). Muzzle particle index: 0 = none, 1 = `MuzzleFlashYellow`, 2 = `MuzzleFlashExp`, 3 = `MuzzleFlashBlue`, 4 = `MuzzleFlashLaser`, 5 = `MuzzleFlashAlienBlue`. Offsets are (x, y, z). Textures are `textures/guns/<base>.png` plus `_1`…`_n-1` camo variants; their content is documented in 03.

| Gun | AI range | AI attackTime | Burst count | Burst delay | Muzzle idx | Muzzle offset | Turret pos offset | Turret muzzle offset | Texture base (variants) |
|---|---|---|---|---|---|---|---|---|---|
| handcannon | 12 | 60 | 0 | 0 | 2 | (0,0,0.2) | – | – | `handgun` (1) |
| pistol | 24 | 30 | 3 | 10 | 1 | (0,0.05,−0.45) | – | – | `pistol3` (1) |
| sawedoff | 12 | 60 | 2 | 20 | 2 | (0,0,0.1) | – | – | `sawedOff` (1) |
| boltaction | 36 | 60 | 0 | 0 | 1 | (0,0.05,0.3) | – | – | `boltactionrifle` (3) |
| m4 | 24 | 30 | 3 | 3 | 1 | (0,0.025,0) | – | – | `m4Texture` (5) |
| flamethrower | 12 | 20 | 5 | 5 | 0 | – | – | – | `Flamethrower` (1) |
| combatshotgun | 12 | 30 | 0 | 0 | 2 | (0,0,0.1) | (0,0.075,0) | – | `combatShotgun` (1) |
| lasergun | 24 | 30 | 0 | 0 | 0 | – | – | – | `Lasergun` (1) |
| rocketlauncher | 24 | 80 | 0 | 0 | 0 | – | (0,−0.7,−0.2) | – | `rocketlauncher` (1) |
| grimreaper | 24 | 120 | 4 | 30 | 0 | – | (0,−0.7,−0.4) | – | `grimreaper` (1) |
| biogun | 18 | 30 | 0 | 0 | 0 | – | – | – | `BioGun` (1) |
| thompson | 18 | 45 | 3 | 3 | 1 | (0,0.05,0) | – | – | `thompson` (1) |
| revolver | 18 | 90 | 6 | 20 | 2 | (0,0.05,−0.45) | – | – | `revolver` (1) |
| ak47 | 24 | 30 | 3 | 3 | 1 | (0,0.05,0.12) | – | – | `ak47Texture` (2) |
| as50 | 36 | 30 | 0 | 0 | 1 | (0,0.05,0.3) | – | – | `as50Texture` (1) |
| teslagun | 24 | 30 | 0 | 0 | 0 | – | – | – | `TeslaGun` (1) |
| pdw | 18 | 30 | 4 | 2 | 3 | (0,0.05,−0.35) | – | – | `PDW` (3) |
| lmg | 24 | 40 | 6 | 3 | 1 | (0,0.1,0.02) | – | – | `mg2_texture` (1) |
| m4_uq | 24 | 30 | 3 | 3 | 0 | – | – | – | `m4_uq_Texture` (1) |
| goldenrevolver | 24 | 60 | 6 | 15 | 2 | (0,0.05,−0.4) | – | – | `goldenrevolver` (1) |
| pulseRifle | 24 | 30 | 0 | 0 | 3 | (0,0.025,−0.05) | – | – | `pulseRifle` (3) |
| alienBlaster | 24 | 40 | 0 | 0 | 0 | – | – | – | `alien_blaster` (1) |
| cyberdemonBlaster | 24 | 40 | 0 | 0 | 0 | – | – | – | `cyberdemonblaster` (1) |
| minigun | 24 | 40 | 10 | 1 | 1 | (0,−0.8,0.4) | (0,0.2,0) | (0,0.6,0) | `minigun` (1) |
| blasterRifle | 24 | 30 | 5 | 3 | 4 | (0,−0.1,0.1) | – | (0,0,−0.4) | `blasterRifle` (1) |
| powerHammer | 3 | 30 | 0 | 0 | 0 | – | – | – | `powerHammer` (1) |
| nukelauncher | 24 | 80 | 0 | 0 | 0 | – | (0,−0.7,−0.2) | – | `nukerocketlauncher` (1) |
| powerHammerAdv | 3 | 30 | 0 | 0 | 0 | – | – | – | `powerHammerAdv` (1) |
| grenadelauncher | 24 | 40 | 3 | 20 | 0 | – | – | – | `grenadelauncher` (1) |
| aug | 24 | 30 | 3 | 3 | 1 | (0,0.025,0) | – | – | `AugTexture` (5) |
| sonicShotgun | 18 | 40 | 0 | 0 | 3 | (0,0.05,−0.35) | – | – | `sonicshotgun` (1) |
| chainsaw | 3 | 10 | 0 | 0 | 0 | – | (0,0.5,0) | – | `chainsaw` (1) |
| scatterBeamGun | 18 | 30 | 0 | 0 | 0 | – | – | – | `laserGunNew` (1) |
| nuclearDeathRay | 24 | 40 | 5 | 5 | 0 | – | (0.1,0,−0.4) | (−0.2,0,0) | `NDR` (1) |
| mac10 | 18 | 35 | 3 | 2 | 1 | (0,0.05,0) | – | (0,0,−0.45) | `mac10texture` (1) |
| pistolAkimbo | 24 | 30 | 3 | 10 | 1 | (0,0.05,−0.45) | – | – | `pistol3` (1) |
| revolverAkimbo | 18 | 90 | 6 | 20 | 2 | (0,0.05,−0.45) | – | – | `revolver` (1) |
| goldenrevolverAkimbo | 24 | 60 | 6 | 15 | 2 | (0,0.05,−0.4) | – | – | `goldenrevolver` (1) |
| mac10Akimbo | 18 | 45 | 3 | 3 | 1 | (0,0.05,0) | – | – | `mac10texture` (1) |
| mibGun | 24 | 60 | 0 | 0 | 5 | (0,−0.1,0.1) | – | (0,0,−0.2) | `mibgun` (1) |
| scar | 24 | 30 | 5 | 2 | 1 | (0,0.03,0) | – | – | `scar_texture` (2) |
| vector | 18 | 35 | 3 | 2 | 1 | (0,0.05,0) | – | (0,0,−0.45) | `vector_texture` (2) |
| mibGunAkimbo | 24 | 60 | 0 | 0 | 5 | (0,−0.1,0.1) | – | (0,0,−0.2) | `mibgun` (1) |

Texture files referenced (all present in the manifest; described in 03): `textures/guns/handgun.png`, `textures/guns/pistol3.png`, `textures/guns/sawedOff.png`, `textures/guns/boltactionrifle.png`, `textures/guns/boltactionrifle_1.png`, `textures/guns/boltactionrifle_2.png`, `textures/guns/m4Texture.png` … `textures/guns/m4Texture_4.png`, `textures/guns/Flamethrower.png`, `textures/guns/combatShotgun.png`, `textures/guns/Lasergun.png`, `textures/guns/rocketlauncher.png`, `textures/guns/grimreaper.png`, `textures/guns/BioGun.png`, `textures/guns/thompson.png`, `textures/guns/revolver.png`, `textures/guns/ak47Texture.png`, `textures/guns/ak47Texture_1.png`, `textures/guns/as50Texture.png`, `textures/guns/TeslaGun.png`, `textures/guns/PDW.png`, `textures/guns/PDW_1.png`, `textures/guns/PDW_2.png`, `textures/guns/mg2_texture.png`, `textures/guns/m4_uq_Texture.png`, `textures/guns/goldenrevolver.png`, `textures/guns/pulseRifle.png`, `textures/guns/pulseRifle_1.png`, `textures/guns/pulseRifle_2.png`, `textures/guns/alien_blaster.png`, `textures/guns/cyberdemonblaster.png`, `textures/guns/minigun.png`, `textures/guns/blasterRifle.png`, `textures/guns/powerHammer.png`, `textures/guns/nukerocketlauncher.png`, `textures/guns/powerHammerAdv.png`, `textures/guns/grenadelauncher.png`, `textures/guns/AugTexture.png` … `textures/guns/AugTexture_4.png`, `textures/guns/sonicshotgun.png`, `textures/guns/chainsaw.png`, `textures/guns/laserGunNew.png`, `textures/guns/NDR.png`, `textures/guns/mac10texture.png`, `textures/guns/mibgun.png`, `textures/guns/scar_texture.png`, `textures/guns/scar_texture_1.png`, `textures/guns/vector_texture.png`, `textures/guns/vector_texture_1.png`.

---

## Table E: tool guns and melee

| Gun | Melee dmg (has ammo / empty) | Melee source tweaks (has ammo) | Tool classes → level | Dig speed | Ammo per block mined / per melee hit | Block-break sound |
|---|---|---|---|---|---|---|
| powerHammer | 6 / 2 | PHYSICAL "player", gore 1.0, pen 0.25, knockback ×1.5 | pickaxe 2, shovel 2 (default 2) | 12 | 1 / 1 | `guns.powerhammerImpactGround` (vol 1.0) |
| powerHammerAdv | 7 / 2 | same | pickaxe 3, shovel 3 (default 3) | 15 | 1 / 1 | same |
| chainsaw | 12 / 2 | PHYSICAL "player", gore 1.0, pen 0.25, knockback 0 | axe 3 (default 3) | 14 | 1 / 1 | `guns.chainsawHit` (vol 0.65) |

Melee damage is an attribute modifier on the item. The player's base 1 is added on top, as with vanilla swords. When empty, the melee source falls back to the default death type.

---

## Table F: grenades (not GenericGun)

| Unlocalized name | Registered | Display name | Class | Stack | Damage | Radius | Bounces | Projectile | TTL | Throw speed | Gravity | Throw sound | Other |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `stielGranate` | yes, "stielGranate" | Stielgranate | GenericGrenade | 16 | 20 | 5 | 3 | GrenadeProjectile | 200 | 0.5 + 0.5·charge | 0.015 / charge | vanilla `random.bow`, pitch `1/(rand·0.4+1.2) + 0.5·charge` | – |
| `fragGrenade` | yes, "fragGrenade" | Frag Grenade | FragGrenade | 16 | 25 | 6 | 2 | FragGrenadeProjectile | 120 | 0.5 + 0.5·charge | 0.015 / charge | `techguns:guns.grenade_throw` (1.0, 1.0) | right-click plays `techguns:guns.grenade_pin` |
| `flashbang` | **no** (constructed, never registered) | – (no lang key) | Flashbang | 16 | 20 | 5 | 0 | FlashBangProjectile | 200 | 0.5 + 0.5·charge | 0.015 / charge | `random.bow` | dead (GUN-24) |

All grenades have spread 0.05, penetration 0 and block damage **off**.

---

## Weapon-stats config

`TGWeaponStats.init` runs in pre-init, right after `TGuns.init()`. It opens `<configdir>/Techguns_WeaponStats.cfg` (the suggested config file name with `.cfg` replaced by `_WeaponStats.cfg`).

* Category `Weapon Stats` (Forge may write it lower-cased). The category comment warns: don't remove entries (it crashes), not every stat affects every gun, values are doubles stored as floats, the "difficult" names (`m4_uq` = The Infiltrator, `mibGun` = De-Atomizer Pistol, `thompson` = Submachine Gun, `ak47` = Kalashnikov), and that deleting the file regenerates it.
* One entry per constructed `GenericGun`, in construction order. The key is `WeaponStats_` + the gun's `getUnlocalizedName()`, which is `techguns.item.<name>`, e.g. `WeaponStats_techguns.item.m4`. The value is a double list with bounds [0, Float.MAX].
* List layout (comment text is generated per class):

| Index | Stat | Comment text |
|---|---|---|
| 0 | DamageMax | float: damage close range |
| 1 | DamageMin | float: damage high range |
| 2 | DamageDropStart | int: distance in blocks for damage drop start, damage is max up to this range |
| 3 | DamageDropEnd | int: distance in blocks for damage drop end, damage is min after this distance |
| 4 | TimeToLive | int: "distance when the bullet gets deleted" (actually ticks) |
| 5 | Accuracy | float: max random angular offset when shooting |
| 6 | Penetration | float: 0.0–1.0 |
| 7 | Radius | only for `GenericGunRadius` subclasses: AoE radius |
| 8 | Gravity | only for `GenericGunRadiusGrav` subclasses: downward velocity |

Radius and gravity apply to the rocket launcher, grim reaper, nuke launcher and grenade launcher, so those four have 9 values. Every other gun has 7. Stats that a class ignores:

| Class / gun | Ignored indices |
|---|---|
| Teslagun | 4 (TTL fixed 10), 5 (spread fixed 0) |
| RocketLauncher, NukeLauncher | 1, 2, 3 (rockets don't use drop-off; 04 handles explosion damage) |
| GrenadeLauncher | 1–6 (only damage, radius and gravity are honoured) |
| SonicShotgun | 5 (ring geometry is fixed) |
| PowerHammer | 5 (spread fixed 0) |
| Handgun | none (speed is fixed, not a config stat) |
