# FX catalog

All named effects. The first part fully decodes the two data files `particles/FXList.txt` (16 FX lists, 50 particle systems) and `particles/Nuke.txt` (1 FX list, 8 systems). The format and engine are described in [particle-system.md](particle-system.md). The second part lists the hard-coded legacy particle names in [`ClientProxy.spawnParticle`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy).

How to read the numbers:
- Time is in ticks. Velocities are in blocks per second (the engine multiplies them by 0.05 per tick).
- `Size` is a particle's starting size. The on-screen quad half-width is **0.1 × size blocks**. `SizeRate` is added each tick and multiplied by `SizeRateDamping` each tick.
- `Gravity` is a constant downward drift in blocks/tick.
- `a..b` is a uniform random range.
- "SpawnDelay *n*" means a burst every *n* + 1 ticks. 99 means a single burst.
- "SystemLifetime 0" on an entity-attached system means "until the entity dies". On a free system it means a single burst.

## Triggers: which game event spawns which FX

Names are looked up case-insensitively. "Packet" means [`PacketSpawnParticle`](../01-core/networking.md#13--packetspawnparticle-sc) (at a point) or [`PacketSpawnParticleOnEntity`](../01-core/networking.md#14--packetspawnparticleonentity-sc) (attached), sent by the server to players within the given range.

| FX name (list or system) | Spawned by | How | Where |
|---|---|---|---|
| `RocketExplosion` (list) | [`RocketProjectile.explodeRocket`](projectiles.md#34-rocketprojectile-and-nukerocketprojectile), [`GrenadeProjectile`](projectiles.md#37-grenadeprojectile-family)/[`GrenadeLauncherProjectile.explode`](projectiles.md#1-registration), [`ReactionChamberTileEntMaster`](../08-machines-production/framework.md#1-class-hierarchy) (meltdown, [`08-machines-production`](../08-machines-production/index.md)) | Packet, 50 blocks | explosion center |
| `FragGrenadeExplosion` (list) | [`FragGrenadeProjectile.explode`](projectiles.md#1-registration), `ReactionChamberTileEntMaster` | Packet, 50 blocks | explosion center |
| `NukeExplosion` (list) | [`NukeRocketProjectile.explodeRocket`](projectiles.md#34-rocketprojectile-and-nukerocketprojectile) | Packet, **150** blocks | explosion center |
| `bioblobExplosion` (list) | [`BioblobTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-bioblobtileent) blob burst ([`07-blocks`](../07-blocks/index.md)) | Packet | blob |
| `AlienExplosion` (list) | [`AlienBlasterProjectile`](projectiles.md#1-registration) impact | Packet, 50 blocks | impact |
| `AlienBlasterTrail` (list) | [`AlienBlaster`](../02-guns/sheets-energy-special.md#alien-blaster-alienblaster) gun on fire | Packet on entity, but the entity is untracked, so it never shows ([PFX-01](index.md#suspected-bugs)) | follows projectile |
| `BlueBlasterTrail` (list) | [`MibGun`](../02-guns/sheets-energy-special.md#de-atomizer-pistol-mibgun) on fire | Packet on entity | follows projectile |
| `BlueBlasterExplosion` (list) | [`EnergyBlastProjectile`](projectiles.md#310-energyblastprojectile-mib-gun) impact (block or entity) | Packet, 50 blocks | impact |
| `CyberDemonBlasterTrail` (list) | [`CyberdemonBlaster`](../02-guns/sheets-energy-special.md#nether-blaster-cyberdemonblaster) gun on fire | Packet on entity | follows projectile |
| `CyberDemonBlasterTrailLarge` (list) | nothing (unused) | n/a | n/a |
| `NetherBlasterImpactFlare`, `MediumExplosionSmoke` | [`CyberdemonBlasterProjectile`](projectiles.md#38-cyberdemonblasterprojectile) block hit (client call); entity hit (server call, SP only) | `proxy.createFX` | impact |
| `NetherBlasterImpactFlare2` | nothing (unused) | n/a | n/a |
| `BioGunTrail` (list) | [`Biogun`](../02-guns/sheets-energy-special.md#bio-gun-biogun) on fire | Packet on entity, 200 blocks | follows projectile |
| `BioTrailGlow` | nothing (commented out of `BioGunTrail`) | n/a | n/a |
| `GrenadeLauncherTrail` | [`GrenadeLauncher`](../02-guns/index.md#coverage) gun on fire; each grenade-launcher bounce; dispenser launch ([`BehaviorProjectileDispenseDamagevalue`](../01-core/lifecycle-and-registration.md#preinit-in-exact-order)) | Packet on entity, 200 blocks | follows grenade |
| `SonicShotgunTrail` (list) | [`SonicShotgun`](../02-guns/index.md#coverage) on fire (main projectile) | Packet on entity, 200 blocks | follows projectile |
| `SonicShotgunImpact` (list) | [`SonicShotgunProjectile`](projectiles.md#313-sonicshotgunprojectile) (main) solid-block hit | Packet, 50 blocks | impact |
| `LaserFlareStart` / `LaserFlareEnd` | [`LaserBeam2`](projectiles.md#1-registration) creation (lasergun, scatter beam gun) | Packet, 50 blocks | muzzle / beam end |
| `TeslaFlare` | [`TeslaBeam`](projectiles.md#1-registration) creation by the gun (not chain beams) | Packet, 50 blocks | beam end |
| `BeamGunMuzzleFX` (list) | [`MovingBeam`](projectiles.md#1-registration)/[`NeutrinoBeam`](projectiles.md#1-registration) client constructor | `createFXOnEntity` | follows beam (the shooter's muzzle) |
| `BeamGunImpactFX` (list) | `MovingBeam` every tick | `proxy.createFX` | beam end |
| `biodeath` | BIO death effect | `createFX` (client, from the death packet) | entity center |
| `laserdeathFire`, `laserdeathAsh` | LASER death effect | `createFX` | entity center |
| `JumpPackBoost` (list) | [`ItemJumppack.doJumpEffect`](../05-armor/special-equipment.md#44-jump-pack-itemjumppack) ([`05-armor`](../05-armor/index.md)) | Packet on player, 50 blocks | player |
| `JumpPackFall` (list) | `ItemJumppack.doFallEffect` | Packet, 50 blocks | player feet |
| `AntiGravRing` | [`TechgunsTickHandler`](../01-core/events.md#techgunstickhandler-fml-bus): creative-style flight granted by armor starts flying (client) | `createFXOnEntity` | player |
| `AntiGravRingGreen` | [`ItemAntiGravPack.doEffect`](../05-armor/special-equipment.md#45-anti-gravity-device-itemantigravpack) (client) | `createFXOnEntity`. **Not defined: NullPointerException** ([PFX-02](index.md#suspected-bugs)) | n/a |
| `ChargeFlare` | [`ChargingStationTileEnt`](../08-machines-production/framework.md#1-class-hierarchy) (4 calls, [`09-machines-utility`](../09-machines-utility/index.md)) | `createFX` | station |
| `OreClusterPing` | [`OreClusterScanner`](../06-items/special-items.md#ore-cluster-scanner-oreclusterscanner-techgunsoreclusterscanner) ([`06-items`](../06-items/index.md)) | `createFX` | ore cluster (visible through walls) |

## Data-file systems

The tables below were extracted mechanically from the data files. They reflect the parser's actual behavior: duplicate keys use the last value, defaults apply where a key is absent, and `NukeExplosionBlastwave` uses the partial-copy `EXTENDS`.

### FX lists (`FXLIST` blocks)

| FX list | File | Member particle systems (spawned together) |
|---|---|---|
| `bioblobExplosion` | `particles/FXList.txt` | `bioblobExplosionExplosion`, `bioblobExplosionSplat`, `LargeExplosionBlastwave`, `bioblobExplosionBlobs` |
| `RocketExplosion` | `particles/FXList.txt` | `LargeExplosionBlastwave`, `LargeExplosionSmoke`, `LargeExplosion`, `LargeExplosionRing` |
| `AlienExplosion` | `particles/FXList.txt` | `AlienFlare`, `AlienShockwave` |
| `AlienBlasterTrail` | `particles/FXList.txt` | `AlienBlasterTrailFlare`, `AlienBlasterTrailWave`, `AlienBlasterTrailGlow` |
| `CyberDemonBlasterTrailLarge` | `particles/FXList.txt` | `CDBTrailFlares`, `CDBSparkyFlares`, `CDBTrailFlames` |
| `CyberDemonBlasterTrail` | `particles/FXList.txt` | `CDBTrailFlares_S`, `CDBSparkyFlares_S`, `CDBTrailFlames_S` |
| `BioGunTrail` | `particles/FXList.txt` | `BioTrailSplat` |
| `SonicShotgunTrail` | `particles/FXList.txt` | `SonicShotgunWaves`, `SonicShotgunWaves2` |
| `SonicShotgunImpact` | `particles/FXList.txt` | `SonicShotgunImpactWave`, `SonicShotgunImpactBlastwave` |
| `JumpPackBoost` | `particles/FXList.txt` | `JumpPackRing`, `JumpBoostSmoke` |
| `JumpPackFall` | `particles/FXList.txt` | `JumpPackRingFall`, `JumpPackFallRing` |
| `BeamGunMuzzleFX` | `particles/FXList.txt` | `BeamGunMuzzleFlare`, `BeamGunMuzzleSparks` |
| `BeamGunImpactFX` | `particles/FXList.txt` | `BeamGunImpactFlare`, `BeamGunImpactSparks` |
| `BlueBlasterTrail` | `particles/FXList.txt` | `BlueBlasterTrailFlare`, `BlueBlasterTrailGlow` |
| `BlueBlasterExplosion` | `particles/FXList.txt` | `BlueBlasterFlare`, `BlueBlasterShockwave` |
| `FragGrenadeExplosion` | `particles/FXList.txt` | `LargeExplosionBlastwave`, `LargeExplosionFrag`, `LargeExplosionSmoke` |
| `NukeExplosion` | `particles/Nuke.txt` | `NukeExplosionMain`, `NukeExplosionBlastwave`, `NukeExplosionBlast`, `NukeExplosionSmoke`, `NukeExplosionRing`, `NukeExplosionMushroomStem`, `NukeExplosionMushroomCloud`, `NukeExplosionBall` |

### Particle systems: emitter parameters

| System | File | Texture (`textures/fx/...`) | Render | Grid cols x rows / frames | Variations | RandRot | Particle life (ticks) | Particles per burst | SpawnDelay | InitialDelay | SystemLifetime |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `NetherBlasterImpactFlare` | FXList.txt | `blasterflare01.png` | ADDITIVE | 4x4 / 14 | - | - | 15..25 | 3..4 | 99 | 0 | 1 |
| `MediumExplosionSmoke` | FXList.txt | `dustclouds02.png` | ALPHA | 3x3 / 9 | - | Y | 35..45 | 1 | 1 | 5 | 4 |
| `NetherBlasterImpactFlare2` | FXList.txt | `blasterflare01.png` | ADDITIVE | 4x4 / 14 | - | Y | 35..45 | 13..14 | 1 | 0 | 3 |
| `LargeExplosionBlastwave` | FXList.txt | `shockwave.png` | ADDITIVE | 1x1 / 1 | - | Y | 10..11 | 1 | 99 | 0 | 1 |
| `LargeExplosionSmoke` | FXList.txt | `largesmoke.png` | ALPHA | 2x2 / 4 | Y | Y | 65..85 | 5..6 | 0 | 10 | 1 |
| `LargeExplosion` | FXList.txt | `fireblast.png` | ALPHA | 5x5 / 20 | - | Y | 25..35 | 6 | 1 | 0 | 1 |
| `LargeExplosionRing` | FXList.txt | `fireblast.png` | ALPHA | 5x5 / 20 | - | Y | 15..22 | 16..18 | 1 | 1 | 1 |
| `biodeath` | FXList.txt | `splat.png` | ADDITIVE | 3x3 / 6 | Y | Y | 25..35 | 1 | 1 | 0 | 5 |
| `laserdeathAsh` | FXList.txt | `largesmoke.png` | ALPHA | 2x2 / 4 | Y | Y | 35..45 | 2..3 | 0 | 2 | 10 |
| `laserdeathFire` | FXList.txt | `flamethrower.png` | ADDITIVE | 4x4 / 16 | - | Y | 15..25 | 1 | 0 | 0 | 3 |
| `bioblobExplosionExplosion` | FXList.txt | `Explosion.png` | ADDITIVE | 5x5 / 20 | - | Y | 12..17 | 8 | 1 | 0 | 1 |
| `bioblobExplosionSplat` | FXList.txt | `splat.png` | ADDITIVE | 3x3 / 6 | Y | Y | 30..45 | 5 | 1 | 0 | 5 |
| `bioblobExplosionBlobs` | FXList.txt | `blobs.png` | ADDITIVE | 4x4 / 16 | Y | Y | 25..35 | 5 | 1 | 0 | 5 |
| `LaserFlareEnd` | FXList.txt | `laserflare02.png` | ADDITIVE | 4x2 / 7 | - | Y | 7 | 1 | 1 | 0 | 1 |
| `LaserFlareStart` | FXList.txt | `laserflare02.png` | ADDITIVE | 4x2 / 7 | - | Y | 7 | 1 | 1 | 0 | 1 |
| `TeslaFlare` | FXList.txt | `teslaflare01.png` | ADDITIVE | 4x4 / 14 | - | Y | 14 | 1 | 1 | 0 | 1 |
| `AlienFlare` | FXList.txt | `alienflare.png` | ADDITIVE | 4x4 / 14 | - | Y | 10 | 1 | 1 | 0 | 1 |
| `AlienShockwave` | FXList.txt | `shockwave.png` | ADDITIVE | 1x1 / 1 | - | Y | 14 | 1 | 1 | 0 | 1 |
| `AlienBlasterTrailFlare` | FXList.txt | `alienflare.png` | ADDITIVE | 4x4 / 14 | - | Y | 6..7 | 10 | 1 | 2 | 0 |
| `AlienBlasterTrailWave` | FXList.txt | `shockwave.png` | ADDITIVE | 1x1 / 1 | - | Y | 4 | 3 | 1 | 3 | 0 |
| `AlienBlasterTrailGlow` | FXList.txt | `lensflare1.png` | ADDITIVE | 1x1 / 1 | - | Y | 3 | 3 | 0 | 2 | 0 |
| `CDBTrailFlares` | FXList.txt | `blasterflare01.png` | ADDITIVE | 4x4 / 14 | - | - | 6 | 6 | 0 | 2 | 0 |
| `CDBTrailFlames` | FXList.txt | `flamethrower.png` | ADDITIVE | 4x4 / 16 | - | Y | 4..5 | 2 | 0 | 3 | 0 |
| `CDBSparkyFlares` | FXList.txt | `blasterflare01.png` | ADDITIVE | 4x4 / 14 | - | Y | 6..7 | 2 | 0 | 3 | 0 |
| `CDBTrailFlares_S` | FXList.txt | `blasterflare01.png` | ADDITIVE | 4x4 / 14 | - | - | 5 | 6 | 0 | 2 | 0 |
| `CDBTrailFlames_S` | FXList.txt | `flamethrower.png` | ADDITIVE | 4x4 / 16 | - | Y | 4 | 1..2 | 0 | 3 | 0 |
| `CDBSparkyFlares_S` | FXList.txt | `blasterflare01.png` | ADDITIVE | 4x4 / 14 | - | Y | 6 | 2 | 0 | 3 | 0 |
| `BioTrailSplat` | FXList.txt | `splat.png` | ADDITIVE | 3x3 / 6 | Y | Y | 8..12 | 1 | 0 | 0 | 0 |
| `BioTrailGlow` | FXList.txt | `lensflare1.png` | ADDITIVE | 1x1 / 1 | - | Y | 3 | 1 | 0 | 2 | 0 |
| `GrenadeLauncherTrail` | FXList.txt | `dustclouds02.png` | ALPHA | 3x3 / 9 | - | Y | 10..15 | 3 | 1 | 0 | 0 |
| `SonicShotgunWaves` | FXList.txt | `sonicwave4x4.png` | ADDITIVE | 4x4 / 14 | - | Y | 6..8 | 1..2 | 0 | 1 | 0 |
| `SonicShotgunWaves2` | FXList.txt | `sonicwave4x4.png` | ADDITIVE | 4x4 / 14 | - | Y | 10..11 | 1 | 1..2 | 3 | 0 |
| `SonicShotgunImpactWave` | FXList.txt | `sonicwave4x4.png` | ADDITIVE | 4x4 / 14 | - | Y | 8..9 | 1 | 2 | 0 | 1 |
| `SonicShotgunImpactBlastwave` | FXList.txt | `shockwave.png` | ADDITIVE | 1x1 / 1 | - | Y | 7..8 | 1 | 99 | 0 | 1 |
| `ChargeFlare` | FXList.txt | `teslaflare01.png` | ADDITIVE | 4x4 / 14 | - | Y | 2 | 1 | 1 | 0 | 1 |
| `OreClusterPing` | FXList.txt | `shockwave.png` | NO_Z_TEST | 1x1 / 1 | - | Y | 15 | 1 | 10 | 0 | 100 |
| `JumpPackRing` | FXList.txt | `shockwave.png` | ADDITIVE | 1x1 / 1 | - | Y | 10 | 1 | 1 | 0 | 5 |
| `JumpBoostSmoke` | FXList.txt | `dustclouds02.png` | ALPHA | 3x3 / 9 | - | Y | 20..25 | 4..6 | 1 | 0 | 5 |
| `JumpPackRingFall` | FXList.txt | `shockwave.png` | ADDITIVE | 1x1 / 1 | - | Y | 10 | 1 | 1 | 0 | 5 |
| `JumpPackFallRing` | FXList.txt | `dustclouds02.png` | ALPHA | 3x3 / 9 | - | Y | 15..22 | 16..18 | 1 | 1 | 1 |
| `AntiGravRing` | FXList.txt | `shockwave.png` | ADDITIVE | 1x1 / 1 | - | Y | 6 | 1 | 1 | 0 | 1 |
| `BeamGunMuzzleFlare` | FXList.txt | `nukebeamflare.png` | ADDITIVE | 4x4 / 16 | - | Y | 5..6 | 1 | 0 | 0 | 5 |
| `BeamGunMuzzleSparks` | FXList.txt | `lensflare3.png` | ADDITIVE | 1x1 / 1 | - | Y | 12..16 | 1 | 0 | 0 | 5 |
| `BeamGunImpactFlare` | FXList.txt | `nukebeamflare.png` | ADDITIVE | 4x4 / 16 | - | Y | 6 | 1 | 0 | 0 | 1 |
| `BeamGunImpactSparks` | FXList.txt | `lensflare3.png` | ADDITIVE | 1x1 / 1 | - | Y | 12..18 | 1 | 0 | 0 | 5 |
| `BlueBlasterTrailFlare` | FXList.txt | `alienflare.png` | ADDITIVE | 4x4 / 14 | - | Y | 6..7 | 10 | 1 | 2 | 0 |
| `BlueBlasterTrailGlow` | FXList.txt | `lensflare1.png` | ADDITIVE | 1x1 / 1 | - | Y | 3 | 3 | 0 | 2 | 0 |
| `BlueBlasterFlare` | FXList.txt | `alienflare.png` | ADDITIVE | 4x4 / 14 | - | Y | 7 | 1 | 1 | 0 | 1 |
| `BlueBlasterShockwave` | FXList.txt | `shockwave.png` | ADDITIVE | 1x1 / 1 | - | Y | 8 | 1 | 1 | 0 | 1 |
| `LargeExplosionFrag` | FXList.txt | `fireblast.png` | ALPHA | 5x5 / 20 | - | Y | 25..35 | 18 | 1 | 0 | 1 |
| `NukeExplosionMain` | Nuke.txt | `fireblast.png` | ALPHA | 5x5 / 20 | - | Y | 105..115 | 20 | 1 | 6 | 1 |
| `NukeExplosionBlastwave` (EXTENDS `LargeExplosionBlastwave`) | Nuke.txt | `shockwave.png` | ADDITIVE | 1x1 / 1 | - | Y | 15..16 | 1 | 99 | 0 | 5 |
| `NukeExplosionBlast` | Nuke.txt | `blasterflare01.png` | ADDITIVE | 4x4 / 8 | - | Y | 7..10 | 35 | 1 | 13 | 3 |
| `NukeExplosionSmoke` | Nuke.txt | `largesmoke.png` | ALPHA | 2x2 / 4 | Y | Y | 100..180 | 30 | 1 | 0 | 5 |
| `NukeExplosionRing` | Nuke.txt | `flamethrower_a.png` | ALPHA | 4x4 / 16 | - | Y | 15..30 | 60 | 1 | 14 | 1 |
| `NukeExplosionMushroomStem` | Nuke.txt | `fireblast.png` | ALPHA | 5x5 / 20 | - | Y | 95..105 | 32 | 1 | 12 | 1 |
| `NukeExplosionMushroomCloud` | Nuke.txt | `fireblast.png` | ALPHA | 5x5 / 20 | - | Y | 85..95 | 20 | 1 | 14 | 1 |
| `NukeExplosionBall` | Nuke.txt | `blasterflare01.png` | ADDITIVE | 4x4 / 14 | - | Y | 20..22 | 25 | 1 | 0 | 1 |

### Particle systems: size, motion and spawn volume

| System | Size | SizeRate | SizeRateDamping | StartSizeRate (damping) | AnimSpeed | VelocityType | Vel1 | Vel2 | Vel3 | VelDamping | Gravity | SysVelFactor | VolumeType | Vol1 / Vol2 | Hollow | Offset | RemoveOnGround / GroundDamping |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `NetherBlasterImpactFlare` | 0.4..0.6 | 0.8..1.4 | 0.88..0.91 | 0 (1) | 1 | SPHERICAL | 0.5..1.5 | 0 | 0 | 0.85..0.9 | 0 | 0 | SPHERE | 0.15 / 0 | - | 0 0 0 | - / 1 |
| `MediumExplosionSmoke` | 1.4..1.6 | 0.4..0.6 | 0.95..0.96 | 0 (1) | 1 | OUTWARD | 0..0.5 | 0.25..1.5 | 0 | 0.95..0.97 | 0 | 0 | SPHERE | 0.45 / 0 | - | 0 0 0 | - / 1 |
| `NetherBlasterImpactFlare2` | 1..2 | 1.2..1.87 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0.1..2 | -0.1..0.1 | 0 | 0.98 | 0 | 1 | CYLINDER | 3.5 / 1 | Y | 0 0 0 | - / 1 |
| `LargeExplosionBlastwave` | 2 | 7..8 | 0.84..0.86 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.85..0.88 | 0 | 0 | CYLINDER | 0.1 / 0.1 | - | 0 0 0 | - / 1 |
| `LargeExplosionSmoke` | 5.8..9.6 | 0.4..0.6 | 0.92..0.96 | 0 (1) | 1 | OUTWARD | 0.2..0.3 | 0..0.05 | 0 | 0.95..0.98 | 0.02 | 0 | SPHERE | 2 / 0 | - | 0 0 0 | - / 1 |
| `LargeExplosion` | 1..2 | 1.2..1.9 | 0.86..0.91 | 0 (1) | 1 | OUTWARD | 1..2 | -2..2 | 0 | 0.85..0.88 | 0 | 0 | CYLINDER | 0.5 / 0.25 | - | 0 0 0 | - / 1 |
| `LargeExplosionRing` | 0.35..0.5 | 1..1.3 | 0.86..0.87 | 0 (1) | 1 | OUTWARD | 7..8 | -0.1..0.1 | 0 | 0.85..0.86 | 0 | 0 | CYLINDER | 0.5 / 0.1 | Y | 0 0 0 | - / 1 |
| `biodeath` | 1.4..1.8 | 1.4..1.8 | 0.9..0.93 | 0 (1) | 1 | OUTWARD | 0.4..0.5 | 0..0.08 | 0 | 0.92..0.93 | 0.05 | 0 | CYLINDER | 0.15 / 0.5 | - | 0 0 0 | Y / 1 |
| `laserdeathAsh` | 1.8..2.2 | 0.4..0.5 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0..0.2 | -0.2..-0.3 | 0 | 0.98..0.99 | 0.005 | 0 | CYLINDER | 0.35 / 0.85 | - | 0 0 0 | - / 0.5..0.65 |
| `laserdeathFire` | 0.4..0.6 | 0.4..0.6 | 0.9..0.93 | 0 (1) | 1 | OUTWARD | 0.5..1 | -0.2..0.8 | 0 | 0.94..0.97 | -0.02 | 0 | CYLINDER | 0.25 / 1 | - | 0 0 0 | - / 1 |
| `bioblobExplosionExplosion` | 1.8..2.9 | 1.2..1.9 | 0.86..0.91 | 0 (1) | 1 | OUTWARD | 1..3 | -3..3 | 0 | 0.85..0.89 | 0 | 0 | CYLINDER | 0.7 / 0.45 | - | 0 0 0 | - / 1 |
| `bioblobExplosionSplat` | 1.8..2.8 | 1.7..2.2 | 0.9..0.93 | 0 (1) | 1 | OUTWARD | 0.5..1.5 | 0..0.25 | 0 | 0.92..0.96 | 0.05 | 0 | CYLINDER | 0.5 / 0.5 | - | 0 0 0 | Y / 1 |
| `bioblobExplosionBlobs` | 0.5..1.5 | 0.2..0.4 | 0.85..0.86 | 0 (1) | 1 | OUTWARD | 1.5..4.5 | 6..12 | 0 | 0.96..0.98 | 0.4 | 0 | CYLINDER | 0.5 / 0.5 | - | 0 0 0 | Y / 1 |
| `LaserFlareEnd` | 1..2 | 1.2..1.87 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | CYLINDER | 0 / 0 | - | 0 0 0 | - / 1 |
| `LaserFlareStart` | 1..2 | 1.2..1.87 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | CYLINDER | 0 / 0 | - | 0 0 0 | - / 1 |
| `TeslaFlare` | 1..2 | 1.2..1.87 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | CYLINDER | 0 / 0 | - | 0 0 0 | - / 1 |
| `AlienFlare` | 1 | 2.2..2.5 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | CYLINDER | 0 / 0 | - | 0 0 0 | - / 1 |
| `AlienShockwave` | 1 | 2.5..2.6 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | CYLINDER | 0 / 0 | - | 0 0 0 | - / 1 |
| `AlienBlasterTrailFlare` | 3.5..5 | -0.5..-0.8 | 0.92..0.94 | 0 (1) | 0.5..1.5 | SPHERICAL | 0..0.1 | 0 | 0 | 0.95..0.97 | 0 | 0 | TRAIL | 5.5 / 0 | - | 0 0 0 | - / 1 |
| `AlienBlasterTrailWave` | 1 | 5 | 0.9 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | TRAIL | 4.5 / 0 | - | 0 0 0 | - / 1 |
| `AlienBlasterTrailGlow` | 4 | 5 | 0.9 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | TRAIL | 4.5 / 0 | - | 0 0 0 | - / 1 |
| `CDBTrailFlares` | 1.5..2 | 0.75..1 | 0.92..0.94 | 0 (1) | 1 | SPHERICAL | 0..0.5 | 0 | 0 | 0.95..0.97 | 0 | 0 | TRAIL | 3 / 1.5 | - | 0 0 0 | - / 1 |
| `CDBTrailFlames` | 1..2 | 1..2 | 0.94..0.97 | 0 (1) | 1 | SPHERICAL | 0..0.5 | 0 | 0 | 0.95..0.97 | 0 | 0 | TRAIL | 3 / 1.5 | - | 0 0 0 | - / 1 |
| `CDBSparkyFlares` | 0.5..1.2 | 0.75..1 | 0.92..0.94 | 0 (1) | 1 | SPHERICAL | 1.5..3.5 | 0 | 0 | 0.95..0.97 | 0 | 0 | POINT | 0 / 0 | - | 0 0 0 | - / 1 |
| `CDBTrailFlares_S` | 0.5..1 | 0.45..0.65 | 0.92..0.94 | 0 (1) | 1 | SPHERICAL | 0..0.5 | 0 | 0 | 0.95..0.97 | 0 | 0 | TRAIL | 3 / 1.5 | - | 0 0 0 | - / 1 |
| `CDBTrailFlames_S` | 0.5..1 | 0.5..1 | 0.94..0.97 | 0 (1) | 1 | SPHERICAL | 0..0.5 | 0 | 0 | 0.95..0.97 | 0 | 0 | TRAIL | 3 / 1.5 | - | 0 0 0 | - / 1 |
| `CDBSparkyFlares_S` | 0.3..0.6 | 0.35..0.6 | 0.92..0.94 | 0 (1) | 1 | SPHERICAL | 1..2 | 0 | 0 | 0.95..0.97 | 0 | 0 | POINT | 0 / 0 | - | 0 0 0 | - / 1 |
| `BioTrailSplat` | 0.5..1.5 | 0.5..1.5 | 0.9..0.93 | 0 (1) | 1 | SPHERICAL | 0.5..1.5 | 0 | 0 | 0.92..0.96 | 0.05 | 0 | POINT | 0 / 0 | - | 0 0 0 | Y / 1 |
| `BioTrailGlow` | 2 | 5 | 0.95 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | POINT | 0 / 0 | - | 0 0 0 | - / 1 |
| `GrenadeLauncherTrail` | 0.3..0.6 | 0.3..0.6 | 0.95..0.96 | 0 (1) | 1 | SPHERICAL | 0..0.15 | 0 | 0 | 0.95..0.97 | 0.01 | 0 | TRAIL | 4.5 / 0 | - | 0 0 0 | - / 1 |
| `SonicShotgunWaves` | 0.25..0.5 | 0.5..2 | 0.75..0.78 | 4 (1) | 1 | SPHERICAL | 0 | 0 | 0 | 0.95..0.97 | 0 | 0 | TRAIL | 3 / 1.5 | - | 0 0 0 | - / 1 |
| `SonicShotgunWaves2` | 1..2 | 4..8 | 0.75..0.78 | 5 (1) | 1 | SPHERICAL | 0..1.5 | 0 | 0 | 0.95..0.97 | 0 | 0 | POINT | 0 / 0 | - | 0 0 0 | - / 1 |
| `SonicShotgunImpactWave` | 2..2.5 | 1.5..2 | 0.75..0.78 | 0 (1) | 1 | SPHERICAL | 0..0.2 | 0 | 0 | 0.95..0.97 | 0 | 0 | TRAIL | 3 / 1.5 | - | 0 0 0 | - / 1 |
| `SonicShotgunImpactBlastwave` | 2 | 8..9 | 0.84..0.86 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.85..0.88 | 0 | 0 | CYLINDER | 0.1 / 0.1 | - | 0 0 0 | - / 1 |
| `ChargeFlare` | 1..1.5 | 1.2..1.87 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | CYLINDER | 0 / 0 | - | 0 0 0 | - / 1 |
| `OreClusterPing` | 1 | 8 | 0.85 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.85..0.88 | 0 | 0 | CYLINDER | 0.1 / 0.1 | - | 0 0 0 | - / 1 |
| `JumpPackRing` | 0.7..0.8 | 2 | 0.8..0.9 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.85..0.88 | 0 | 0 | CYLINDER | 0.1 / 0.1 | - | 0 0 0 | - / 1 |
| `JumpBoostSmoke` | 1.4..1.6 | 0.4..0.6 | 0.95..0.96 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.95..0.97 | 0.1 | 0 | CYLINDER | 0.5 / 0.1 | - | 0 0 0 | - / 1 |
| `JumpPackRingFall` | 1.2..1.5 | 2 | 0.8..0.9 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.85..0.88 | 0.5 | 0 | CYLINDER | 0.5 / 0.1 | - | 0 0 0 | - / 1 |
| `JumpPackFallRing` | 0.35..0.5 | 1..1.3 | 0.86..0.87 | 0 (1) | 1 | OUTWARD | 1..2 | -0.1..0.1 | 0 | 0.85..0.86 | 0 | 0 | CYLINDER | 0.4 / 0.1 | Y | 0 0 0 | - / 1 |
| `AntiGravRing` | 24 | -8 | 0.8..0.9 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.85..0.88 | 0 | 0 | CYLINDER | 0 / 0 | - | 0 0 0 | - / 1 |
| `BeamGunMuzzleFlare` | 0.25..0.5 | 0.15..0.25 | 0.75..0.78 | 0 (1) | 1 | ORTHO | 1..4 | 0 | 0 | 0.79..0.81 | 0 | 0 | CYLINDER2 | 0 / 10 | - | 1 0 0 | - / 1 |
| `BeamGunMuzzleSparks` | 0.1..0.2 | 0.1 | 0.75..0.78 | 0 (1) | 1 | ORTHO | 5..15 | -0.7..0.7 | -0.7..0.7 | 0.95..0.97 | 0.02 | 0 | POINT | 0 / 0 | - | 0.25 0 0 | - / 1 |
| `BeamGunImpactFlare` | 0.3..0.6 | 0.3..0.6 | 0.85..0.88 | 0 (1) | 1 | SPHERICAL | 0..0.5 | 0 | 0 | 0.79..0.81 | 0 | 0 | POINT | 0 / 0 | - | 0 0 0 | - / 1 |
| `BeamGunImpactSparks` | 0.1..0.2 | 0.1 | 0.75..0.78 | 0 (1) | 1 | SPHERICAL | -4..4 | 0 | 0 | 0.84..0.87 | 0.02 | 0 | POINT | 0 / 0 | - | 0 0 0 | Y / 1 |
| `BlueBlasterTrailFlare` | 2..2.5 | -0.5..-0.8 | 0.92..0.94 | 0 (1) | 0.5..1.5 | SPHERICAL | 0..0.1 | 0 | 0 | 0.95..0.97 | 0 | 0 | TRAIL | 5.5 / 0 | - | 0 0 0 | - / 1 |
| `BlueBlasterTrailGlow` | 0.7 | 5 | 0.9 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | TRAIL | 4.5 / 0 | - | 0 0 0 | - / 1 |
| `BlueBlasterFlare` | 1 | 1.2..1.5 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | CYLINDER | 0 / 0 | - | 0 0 0 | - / 1 |
| `BlueBlasterShockwave` | 1 | 1.5..1.6 | 0.9..0.91 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.98 | 0 | 0 | CYLINDER | 0 / 0 | - | 0 0 0 | - / 1 |
| `LargeExplosionFrag` | 1.5..2.2 | 1.2..1.9 | 0.86..0.91 | 0 (1) | 1 | OUTWARD | 3..4 | -3..3 | 0 | 0.88..0.91 | 0 | 0 | CYLINDER | 0.5 / 0.25 | - | 0 0 0 | - / 1 |
| `NukeExplosionMain` | 3..6 | 2..4 | 0.86..0.91 | 0 (1) | 1 | OUTWARD | 3..6 | 0..1 | 0 | 0.85..0.88 | 0.01 | 0 | CYLINDER | 8 / 1 | - | 0 0 0 | - / 1 |
| `NukeExplosionBlastwave` | 5 | 25 | 0.84..0.86 | 0 (1) | 1 | OUTWARD | 0 | 0 | 0 | 0.85..0.88 | 0 | 0 | CYLINDER | 0.1 / 0.1 | - | 0 0 0 | - / 1 |
| `NukeExplosionBlast` | 5..10 | 10..15 | 0.88..0.89 | 0 (1) | 1 | OUTWARD | 10..20 | 0..10 | 0 | 0.95..0.97 | 0 | 0 | CYLINDER | 5 / 2 | - | 0 0 0 | - / 1 |
| `NukeExplosionSmoke` | 8..12 | 0.4..0.6 | 0.92..0.96 | 0 (1) | 1 | OUTWARD | -17..-8 | 1..3 | 0 | 0.92..0.94 | 0.02 | 0 | CYLINDER | 18 / 2 | Y | 0 0 0 | - / 1 |
| `NukeExplosionRing` | 2..4 | 2..3 | 0.86..0.87 | 0 (1) | 1 | OUTWARD | 30..45 | -0.2..0.2 | 0 | 0.85..0.88 | 0 | 0 | CYLINDER | 2 / 0.5 | - | 0 1.5 0 | - / 1 |
| `NukeExplosionMushroomStem` | 3..6 | 2..4 | 0.86..0.91 | 0 (1) | 1 | OUTWARD | -1..-0.5 | 1..25 | 0 | 0.89..0.91 | 0.01 | 0 | CYLINDER | 4 / 8 | - | 0 0 0 | - / 1 |
| `NukeExplosionMushroomCloud` | 4..8 | 3..6 | 0.86..0.91 | 0 (1) | 1 | OUTWARD | 1.5..3 | 14..15 | 0 | 0.87..0.88 | 0.01 | 0 | CYLINDER | 6 / 3 | - | 0 11.5 0 | - / 1 |
| `NukeExplosionBall` | 12..14 | 5..8 | 0.96..0.98 | 0 (1) | 1 | HEMISPHERICAL | 1..4 | 0 | 0 | 0.95..0.97 | 0 | 0 | HEMISPHERE | 4 / 0 | - | 0 0 0 | - / 1 |

### Particle systems: alpha and color keys

Raw keys are written `value@time` exactly as in the file. The "effective" columns apply the two engine quirks described in [particle-system.md](particle-system.md#35-alpha-and-color-curves): each segment runs from the later key's value to the earlier key's value, and alpha is additionally stored as `1 - value`.

| System | Alpha keys (raw) | Effective alpha per segment | Color keys (raw RGB@time) | Effective color per segment |
|---|---|---|---|---|
| `NetherBlasterImpactFlare` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `MediumExplosionSmoke` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (192,168,128)@1 | 0-1: (192,168,128) to (255,255,255) |
| `NetherBlasterImpactFlare2` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `LargeExplosionBlastwave` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (0,0,0)@1 | 0-1: (0,0,0) to (255,255,255) |
| `LargeExplosionSmoke` | 0@0, 0.7@0.6, 0@1 | 0-0.6: 0.3 to 1; 0.6-1: 1 to 0.3 | (196,189,179)@0, (135,123,115)@1 | 0-1: (135,123,115) to (196,189,179) |
| `LargeExplosion` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `LargeExplosionRing` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `biodeath` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (0,255,0)@0, (45,255,30)@1 | 0-1: (45,255,30) to (0,255,0) |
| `laserdeathAsh` | 0@0, 0.6@0.1, 0@1 | 0-0.1: 0.4 to 1; 0.1-1: 1 to 0.4 | (196,189,179)@0, (135,123,115)@1 | 0-1: (135,123,115) to (196,189,179) |
| `laserdeathFire` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (128,128,128)@1 | 0-1: (128,128,128) to (255,255,255) |
| `bioblobExplosionExplosion` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (155,255,155)@0, (55,255,55)@1 | 0-1: (55,255,55) to (155,255,155) |
| `bioblobExplosionSplat` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (0,255,0)@0, (45,255,30)@1 | 0-1: (45,255,30) to (0,255,0) |
| `bioblobExplosionBlobs` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `LaserFlareEnd` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `LaserFlareStart` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `TeslaFlare` | 0@0, 0@1 | 0-1: 1 to 1 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `AlienFlare` | 0@0, 0@1 | 0-1: 1 to 1 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `AlienShockwave` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,50,255)@0, (255,50,255)@1 | 0-1: constant (255,50,255) |
| `AlienBlasterTrailFlare` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `AlienBlasterTrailWave` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,50,255)@0, (255,50,255)@1 | 0-1: constant (255,50,255) |
| `AlienBlasterTrailGlow` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,125,255)@0, (255,125,255)@1 | 0-1: constant (255,125,255) |
| `CDBTrailFlares` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `CDBTrailFlames` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `CDBSparkyFlares` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `CDBTrailFlares_S` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `CDBTrailFlames_S` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `CDBSparkyFlares_S` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `BioTrailSplat` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (0,255,0)@0, (45,255,30)@1 | 0-1: (45,255,30) to (0,255,0) |
| `BioTrailGlow` | 0@0, 0.25@0.5, 0@1 | 0-0.5: 0.75 to 1; 0.5-1: 1 to 0.75 | (0,0,0)@0, (0,100,0)@0.5, (0,0,0)@1 | 0-0.5: (0,100,0) to (0,0,0); 0.5-1: (0,0,0) to (0,100,0) |
| `GrenadeLauncherTrail` | 0@0, 0.33@0.25, 0@1 | 0-0.25: 0.67 to 1; 0.25-1: 1 to 0.67 | (255,255,255)@0, (255,225,200)@1 | 0-1: (255,225,200) to (255,255,255) |
| `SonicShotgunWaves` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (0,0,0)@0, (255,255,255)@0.25, (0,0,0)@1 | 0-0.25: (255,255,255) to (0,0,0); 0.25-1: (0,0,0) to (255,255,255) |
| `SonicShotgunWaves2` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (0,0,0)@0, (255,255,255)@0.25, (0,0,0)@1 | 0-0.25: (255,255,255) to (0,0,0); 0.25-1: (0,0,0) to (255,255,255) |
| `SonicShotgunImpactWave` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (0,0,0)@0, (255,255,255)@0.25, (0,0,0)@1 | 0-0.25: (255,255,255) to (0,0,0); 0.25-1: (0,0,0) to (255,255,255) |
| `SonicShotgunImpactBlastwave` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (225,235,255)@0, (0,0,0)@1 | 0-1: (0,0,0) to (225,235,255) |
| `ChargeFlare` | 0@0, 0@1 | 0-1: 1 to 1 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `OreClusterPing` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,0,0)@0, (0,0,0)@1 | 0-1: (0,0,0) to (255,0,0) |
| `JumpPackRing` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (0,0,0)@1 | 0-1: (0,0,0) to (255,255,255) |
| `JumpBoostSmoke` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (192,192,192)@1 | 0-1: (192,192,192) to (255,255,255) |
| `JumpPackRingFall` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (0,0,0)@1 | 0-1: (0,0,0) to (255,255,255) |
| `JumpPackFallRing` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `AntiGravRing` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (0,0,0)@0, (255,255,255)@1 | 0-1: (255,255,255) to (0,0,0) |
| `BeamGunMuzzleFlare` | 0@0, 0.25@0.5, 0@1 | 0-0.5: 0.75 to 1; 0.5-1: 1 to 0.75 | (155,155,155)@0, (0,0,0)@1 | 0-1: (0,0,0) to (155,155,155) |
| `BeamGunMuzzleSparks` | 0@0, 1@0.5, 0@1 | 0-0.5: 0 to 1; 0.5-1: 1 to 0 | (255,196,128)@0, (255,128,0)@1 | 0-1: (255,128,0) to (255,196,128) |
| `BeamGunImpactFlare` | 0@0, 0.1@0.5, 0@1 | 0-0.5: 0.9 to 1; 0.5-1: 1 to 0.9 | (0,0,0)@0, (255,255,255)@0.25, (0,0,0)@1 | 0-0.25: (255,255,255) to (0,0,0); 0.25-1: (0,0,0) to (255,255,255) |
| `BeamGunImpactSparks` | 0@0, 1@0.5, 0@1 | 0-0.5: 0 to 1; 0.5-1: 1 to 0 | (255,196,128)@0, (255,128,0)@1 | 0-1: (255,128,0) to (255,196,128) |
| `BlueBlasterTrailFlare` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (40,255,140)@0, (40,255,140)@1 | 0-1: constant (40,255,140) |
| `BlueBlasterTrailGlow` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (100,180,255)@0, (100,180,255)@1 | 0-1: constant (100,180,255) |
| `BlueBlasterFlare` | 0@0, 0@1 | 0-1: 1 to 1 | (40,255,140)@0, (40,255,140)@1 | 0-1: constant (40,255,140) |
| `BlueBlasterShockwave` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,50,255)@0, (255,50,255)@1 | 0-1: constant (255,50,255) |
| `LargeExplosionFrag` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `NukeExplosionMain` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `NukeExplosionBlastwave` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (0,0,0)@1 | 0-1: (0,0,0) to (255,255,255) |
| `NukeExplosionBlast` | 0@0, 1@0.5, 0@1 | 0-0.5: 0 to 1; 0.5-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `NukeExplosionSmoke` | 0@0, 0.7@0.6, 0@1 | 0-0.6: 0.3 to 1; 0.6-1: 1 to 0.3 | (196,189,179)@0, (135,123,115)@1 | 0-1: (135,123,115) to (196,189,179) |
| `NukeExplosionRing` | 0@0, 1@0.25, 0@1 | 0-0.25: 0 to 1; 0.25-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `NukeExplosionMushroomStem` | 0@0, 1@0.35, 0@1 | 0-0.35: 0 to 1; 0.35-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `NukeExplosionMushroomCloud` | 0@0, 1@0.5, 0@1 | 0-0.5: 0 to 1; 0.5-1: 1 to 0 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |
| `NukeExplosionBall` | 0@0, 1@1 | 0-1: 0 to 1 | (255,255,255)@0, (255,255,255)@1 | 0-1: constant (255,255,255) |

### Notes on specific systems

- **Single-burst flashes** (`LaserFlareEnd`, `LaserFlareStart`, `TeslaFlare`, `AlienFlare`, `BlueBlasterFlare`, `ChargeFlare`): one particle, a zero-size cylinder, no velocity. These are pure animated flares that grow by SizeRate with damping 0.9.
- **Shockwave rings** (`LargeExplosionBlastwave`, `SonicShotgunImpactBlastwave`, `NukeExplosionBlastwave`, `AlienShockwave`, `BlueBlasterShockwave`, `JumpPackRing`, `JumpPackRingFall`, `OreClusterPing`, `AntiGravRing`):
  - All use `shockwave.png` billboards that grow quickly; `AntiGravRing` shrinks from 24.
  - **They are camera-facing billboards, not flat ground rings.** The renderer always faces the camera.
  - `OreClusterPing` repeats every 11 ticks for 100 ticks and is NO_Z_TEST (visible through terrain).
  - Most color keys run white to black. With the swapped interpolation (particle-system.md §3.5) they *start* black and end white, while the alpha fades out.
- **Trails** (`TRAIL` volume): particles are laid evenly along the last `Volume1` blocks behind the projectile, in the direction opposite its motion, every burst. For example, `AlienBlasterTrailFlare` places 10 per tick over 5.5 blocks.
- **Sonic shotgun waves** use `StartSizeRate 4` / `5` with damping 1. Each later burst starts 4 to 5 size units larger, so the trail widens linearly with distance from the muzzle.
- **Nuke:**
  - Ball: an immediate hemisphere of 25 big flares.
  - Blastwave: the instant ring.
  - Main: a fire ring after 6 ticks.
  - Stem: rising fire column after 12 ticks, radius 4, 8 high, upward 1..25 b/s.
  - Blast: a big flare burst after 13 ticks.
  - Cloud and Ring: after 14 ticks. The mushroom cloud is offset 11.5 up. The shockwave ring is 60 fire puffs flying outward at 30..45 b/s, 1.5 up.
  - Smoke: 30 smoke puffs per burst for 5 bursts, spawned on a hollow cylinder of radius 18 and flying **inward** (negative outward speed).

## Legacy named particles (`ClientProxy.spawnParticle`)

These are hard-coded in `java/techguns/client/ClientProxy.java`. Each call creates a single sprite (see [particle-system.md §4](particle-system.md#4-legacy-sprite-particle-entityparticleanimated) for the [`EntityParticleAnimated`](particle-system.md#4-legacy-sprite-particle-entityparticleanimated) semantics). Sizes are `particleScale`, so the half-width is 0.1 × scale blocks. "Grow" is `setScaleIncrease(min, max, factor, soft = true)`. Values passed through `setMaxAge` and the scale setters are randomized ±20 % (randomness defaults to 0.20 unless the call sets it first; the scale and age setters are called before `setRandomness`). "Velocity" is the motion passed in.

| Name | Particle class | Texture (`textures/fx/...`) | Grid / sprites | Blend | Scale | Max age | Gravity | Grow | Color | Other | Active callers |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `Fireblast` | Animated | `fireblast.png` | 5×5 / 25 | ALPHA | 2.5 | 20 | 0 | 2.5 → 5 | white | random rotation, randomness 0.33, damping 0.90 | Rocket and Nuke rocket client trail (every tick); [`AttackHelicopter`](../10-entities-npcs/per-mob.md#16-attack-helicopter-attackhelicopter-boss) smoke and fire ([`10-entities-npcs`](../10-entities-npcs/index.md)) |
| [`Flamethrower`](../02-guns/per-gun-behavior.md#flamethrower-flamethrower-flamethrower) | Animated | `flamethrower_a.png` | 4×4 / 16 | ALPHA | 0.25 | 20 | 0 | 0.25 → 2.5, reaching max at 50 % of life | white | random rotation, randomness 0.75, damping 0.98 | [`FlamethrowerProjectile`](projectiles.md#32-flamethrowerprojectile) (2 per tick, client) |
| `Jetpack` | Animated | `flamethrower_a.png` | 4×4 / 16 | ALPHA | 0.125 | 10 | 0 | 0.125 → 1.5, at 50 % | white | as Flamethrower | [`RenderJetpack`](../05-armor/rendering-and-models.md#3-special-slot-rendering-on-the-player-rendereradditionalslots) ([`05-armor`](../05-armor/index.md)) |
| `Explosion` | Animated | `Explosion.png` | 5×5 / 20 | ALPHA | 5 | 20 | 0.05 | 5 → 10 | white | random rotation | [`ExplosiveChargeTileEnt`](../09-machines-utility/explosive-charges.md#nbt-explosivechargetileent) ([`07-blocks`](../07-blocks/index.md)); [`ProjectileExplosion.createExplosionEffect`](explosions.md#projectileexplosion) (dead) |
| `ExplosionHuge` | Animated | `Explosion.png` | 5×5 / 20 | ALPHA | 5 | 40 | 0.05 | 20 → 40 | white | random rotation | `AttackHelicopter` death |
| `LargeSmoke` | Animated | `largesmoke.png` | 2×2 / 4 | ALPHA_SHADED | 7.5 | 40 | 0 | 7.5 → 12.5 | white | delay 7 ticks, random rotation, randomness 0.5, damping 0.98 | `AttackHelicopter`, `ExplosiveChargeTileEnt` |
| `impact` | Animated | `impact.png` | 2×2 / 4 | ADDITIVE | 2.5 | 5 | 0 | 2.5 → 7.5 | (255, 215, 155) alpha 128 → 0, linear | n/a | [`PowerHammerProjectile`](projectiles.md#311-powerhammerprojectile) impact |
| `BlasterFlare` | Animated | `blasterflare01.png` | 4×4 / 14 | ADDITIVE | 2.5 | 16 | 0 | n/a | white | n/a | `CyberdemonBlasterProjectile` constructor (server-side, single-player only) |
| `AlienBlasterFlare` | Animated | `alienflare.png` | 4×4 / 14 | ADDITIVE | 3.0 | 5 | 0 | n/a | white | n/a | [`RailgunProjectile`](projectiles.md#316-railgunprojectile-never-spawned) trail (unspawned entity) |
| `MuzzleFlashYellow` | Animated | `muzzleflash2.png` | 4×4 / 8 | ADDITIVE | 1.5 | 4 | 0 | n/a | white | n/a | gun muzzle-particle index 1 (third-person and NPC flashes, [`03-gun-visuals-audio`](../03-gun-visuals-audio/index.md)) |
| `MuzzleFlashExp` | Animated | `muzzleflash4x4.png` | 4×4 / 10 | ADDITIVE | 1.5 | 4 | 0 | n/a | white | n/a | muzzle index 2 |
| `MuzzleFlashBlue` | Animated | `muzzleflash_blue.png` | 4×4 / 8 | ADDITIVE | 1.5 | 4 | 0 | n/a | white | n/a | muzzle index 3 |
| `MuzzleFlashLaser` | Animated | `laserflare02.png` | 4×4 / 7 | ADDITIVE | 1.5 | 4 | 0 | n/a | white | n/a | muzzle index 4 |
| `MuzzleFlashAlienBlue` | Animated | `alienflare.png` | 4×4 / 14 | ADDITIVE | 1.5 | 4 | 0 | n/a | constant (40, 255, 140, 255), FAST curve | n/a | muzzle index 5 |
| `LaserFlareEnd` | Animated | `laserflare02.png` | 4×4 / 7 | ADDITIVE | 5.0 | 18 | 0 | 5 → 7.5 | white | n/a | [`LaserBeam`](projectiles.md#1-registration) (deprecated). Packets with this name resolve to the **data-file** system instead. |
| `LaserFlareStart` | Animated | `laserflare02.png` | 4×4 / 7 | ADDITIVE | 2.5 | 12 | 0 | n/a | white | n/a | `LaserBeam` (deprecated) |
| `TeslaFlare` | Animated | `teslaflare01.png` | 4×4 / 14 | ADDITIVE | 5.0 | 15 | 0 | 5 → 10 | white, alpha 255 → 0 from 75 % of life | n/a | none (packets use the data-file version) |
| `Fireball` | Animated | `fireball_4x4_add.png` | 4×4 / 16 | ADDITIVE | 1.5 | 10..14 | 0.025 | n/a | white | random rotation | none |
| `FireballImpact` | Animated | `fireball_4x4_add.png` | 4×4 / 16 | ADDITIVE | 3.5 | 20 | 0.025 | 3.5 → 6 | white | random rotation | none (calls commented) |
| `Blood` | Animated | `blood01.png` | 1×1 / 1 | ALPHA | 5 | 20 | 0.07 | 5 → 10 | white, alpha 255 → 0 from 50 % | random rotation, randomness 0.5, damping 0.98 | none (commented) |
| `DustClouds` | Animated | `dustclouds.png` | 4×4 / 16 | ALPHA_SHADED | 7.5 | 40 | 0 | 5 → 10 | white, alpha 255 → 0 with SINUS curve | random rotation, randomness 0.5, damping 0.98 | none (commented) |
| `BioTrail` | vanilla `EntityBreakingFX` | slime ball item icon | n/a | n/a | n/a | n/a | n/a | n/a | n/a | velocity from caller | none (commented) |
| `LaserImpact` | vanilla `EntityBreakingFX` | redstone item icon | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | none (commented) |
| `RailgunTrail` | `EntityParticleTrail` | `glow.png` | n/a | ALPHA | 0.5 × vanilla | vanilla | 0 | n/a | (0.1, 0.5, 1.0) | n/a | none |
| `LaserBeam` | `EntityLaserBeamFX` | `laser2_a.png` | n/a | ALPHA | n/a | vanilla | n/a | n/a | n/a | the motion arguments are the beam target | none (commented) |

Unknown names produce no particle and no error message (the legacy switch simply falls through).

### Death-effect sprites (not named, built directly)

| Source | Texture | Grid | Blend | Parameters |
|---|---|---|---|---|
| [`EntityBloodEffect`](death-effects.md#blood-burst-entitybloodeffect) (blood) | `blood4x4.png` | 2×2, used as 4 random variations | ALPHA | Scale 5 → 10 grow, age 20, gravity 0.07. Blood color fades alpha 255 → 0 from 50 %. Random rotation, randomness 0.5, damping 0.98. |
| `EntityBloodEffect` (no-blood dust) | `dustclouds02.png` | 3×3 / 9, animated | ALPHA_SHADED | Scale 5 → 10, age 40, gravity 0.05. Blood color 255 → 0 from 50 %. Random rotation, randomness 0.5, damping 0.98. |
| [`FlyingGibs`](projectiles.md#319-flyinggibs) blood trail | `blood4x4.png` | 2×2 variations | ALPHA | Scale size × 2 → size × 4, age 15, gravity 0.07. Blood color 255 → 0 from 50 %. Random rotation, randomness 0.8, damping 0.92. |
