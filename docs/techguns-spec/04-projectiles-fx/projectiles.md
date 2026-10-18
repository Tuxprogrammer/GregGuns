# Projectiles

Every projectile, beam, grenade, rocket and gib entity in Techguns 1.7.10 (`java/techguns/entities/projectiles/`, 30 classes). Visual renderers are in [renderers.md](renderers.md), explosion math in [explosions.md](explosions.md), particle FX names in [fx-catalog.md](fx-catalog.md), and death animations in [death-effects.md](death-effects.md). Gun stats (damage, speed, TTL, spread, drop-off, penetration) are passed in by the guns. See [`02-guns`](../02-guns/index.md).

Units: positions and speeds are in blocks and blocks/tick, time is in ticks (20 per second), angles are in degrees unless stated.

---

## 1. Registration

All projectile entities are registered in [`java/techguns/Techguns.java`](../01-core/lifecycle-and-registration.md#lifecycle-overview) (preInit, lines 487–514), **not** in [`TGEntities.java`](../10-entities-npcs/mob-catalog.md#registration-javatechgunstgentitiesjava). `TGEntities.java` holds only NPC registrations (see [`10-entities-npcs`](../10-entities-npcs/index.md)). The projectile block runs *before* `TGEntities.init()`, so projectiles take mod-local entity IDs 1–26 and NPCs start at 27. The shared counter is `Techguns.modEntityID`. The FML mod id is `Techguns`, so the global entity string is `Techguns.<name>`.

| ID | Class | Registration name | Tracking range | Update freq | Velocity updates | Renderer (ClientProxy) | Status |
|---|---|---|---|---|---|---|---|
| 1 | `GenericProjectile` | `GenericProjectile` | 80 | 3 | yes | `RenderGenericBullet` | active (most bullet guns, NPC guns, helicopter) |
| 2 | `SimpleProjectile` | `SimpleProjectile` | 80 | 3 | yes | `RenderSimpleProjectile(1.0, techguns:textures/entity/handgunBullet.png)` | active (handcannon) |
| 3 | `FlamethrowerProjectile` | `FlamethrowerProjectile` | 80 | 3 | yes | `RenderDummy` (invisible) | active (flamethrower) |
| 4 | `LaserProjectile` | `LaserProjectile` | 80 | 3 | yes | `RenderDummy` | active (lasergun, scatterBeamGun) |
| 5 | [`LaserBeam`](fx-catalog.md#legacy-named-particles-clientproxyspawnparticle) | `LaserBeam` | 80 | 3 | yes | [`RenderGenericLaser`](renderers.md#laser-beam-renderlaserbeam2-rendergenericlaser) | registered, never spawned (deprecated) |
| 6 | `RocketProjectile` | `RocketProjectile` | 80 | 3 | yes | `RenderRocketProjectile(ModelRocket, techguns:textures/guns/rocketlauncher.png)` | active |
| 7 | `NukeRocketProjectile` | `NukeRocketProjectile` | 80 | 3 | yes | `RenderRocketProjectile(ModelRocket, techguns:textures/guns/nukerocketlauncher.png)` | active |
| 8 | `BiogunProjectile` | `BiogunProjectile` | 80 | 3 | yes | `RenderBiogunProjectile(techguns:textures/entity/bioblob.png)` | active |
| 9 | `TeslaProjectile` | `TeslaProjectile` | 80 | 3 | yes | `RenderDummy` | active |
| 10 | `TeslaBeam` | `TeslaBeam` | 80 | 3 | yes | `RenderTeslaBeam(xOffset 0.25)` | active |
| 11 | `GrenadeProjectile` | `GrenadeProjectile` | 120 | 1 | yes | `RenderStielgranateProjectile(ModelStielgranate, techguns:textures/guns/stielgranate.png)` | active (stick grenade, dispenser) |
| 12 | `MiningDummyProjectile` | `DummyMiningProjectile` | 80 | 3 | yes | none registered, falls back to the superclass renderer `RenderGenericBullet` | registered; spawner (`driller`) is commented out |
| 13 | `CyberdemonBlasterProjectile` | `CyberdemonBlasterProjectile` | 80 | 3 | yes | `RenderDummy` | active |
| 14 | `BlasterRifleProjectile` | `BlasterRifleProjectile` | 80 | 3 | yes | `RenderBlasterRifleProjectile` | active |
| 15 | `PowerHammerProjectile` | `PowerHammerProjectile` | 80 | 3 | yes | `RenderPowerHammerProjectile` | active |
| 16 | `FlyingGibs` | `FlyingGibs` | 80 | 3 | yes | [`RenderFlyingGibs`](death-effects.md#flying-gibs-rendering) | client-side only in practice (spawned by death FX) |
| 17 | [`EntityParticleSystem`](particle-system.md#31-entityparticlesystem-the-emitter) (in `client/particle`) | `ParticleSystem` | 150 | 3 | yes | `RenderDummy` | client-side emitter (see particle-system.md) |
| 18 | `LaserBeam2` | `LaserBeam2` | 80 | 3 | yes | [`RenderLaserBeam2`](renderers.md#laser-beam-renderlaserbeam2-rendergenericlaser) | registered; the gun spawn lines are commented out, but `LaserProjectile` spawns it itself |
| 19 | `GrenadeLauncherProjectile` | `GrenadeLauncherProjectile` | 120 | 1 | yes | `RenderGrenadeLauncherProjectile(ModelModel models/entity/launchergrenade.obj, techguns:textures/entity/launchergrenade.png)` | active |
| 20 | `SonicShotgunProjectile` | `SonicShotgunProjectile` | 80 | 1 | yes | `RenderSonicShotgunProjectile` | active |
| 21 | `InvisibleProjectile` | `InvisibleProjectile` | 80 | 3 | yes | `RenderDummy` | active (chainsaw) |
| 22 | [`MovingBeam`](fx-catalog.md#triggers-which-game-event-spawns-which-fx) | `MovingBeam` | 80 | 3 | yes | [`RenderMovingBeam`](renderers.md#nuclear-death-ray-rendermovingbeam) | active (nuclearDeathRay, a [`Beamgun`](../02-guns/sheets-energy-special.md#nuclear-death-ray-nucleardeathray)) |
| 23 | `BeamProjectile` | `BeamProjectile` | 80 | 3 | yes | `RenderDummy` | registered, never spawned |
| 24 | `FragGrenadeProjectile` | `FragGrenadeProjectile` | 120 | 1 | yes | `RenderStielgranateProjectile(ModelFragGrenade(false), techguns:textures/guns/fragGrenade_texture.png)` | active |
| 25 | `NeutrinoBeam` | `NeutrinoBeam` | 80 | 3 | yes | `RenderMovingBeam` | registered; `neutrinoGun` is commented out |
| 26 | `EnergyBlastProjectile` | `EnergyBlastProjectile` | 80 | 3 | yes | `RenderDummy` | active (mibGun, mibGunAkimbo) |

Not registered, so they are never tracked to clients or saved:

| Class | Notes |
|---|---|
| `AlienBlasterProjectile` | **Actively spawned by the [`alienBlaster`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) gun but has no entity registration.** Its renderer (`RenderDummy`) is bound in ClientProxy, but the entity is never sent to clients. It works server-side, and its visuals come from FX packets. See bug [PFX-01](index.md#suspected-bugs). |
| `BlasterProjectile` | `@Deprecated`. Registration and renderer (`RenderBlasterProjectile`) are commented out. Gun spawn lines are commented. |
| `PiercingProjectile` | `@Deprecated`. No registration. [`RenderPiercingProjectile`](textures.md#single-images) exists but is unbound. |
| [`FlashBangProjectile`](index.md#key-facts) | Spawned by the [`Flashbang`](../02-guns/per-gun-behavior.md#grenades-genericgrenade-fraggrenade-flashbang) item. The item is constructed in [`TGuns`](../02-guns/gun-catalog.md), but its `GameRegistry.registerItem` is missing, so it is unreachable in practice. |
| `RailgunProjectile` | Registered renderer `RenderRailgunProjectile`, but no entity registration, and the `railgun` gun is commented out. |

IDs 1–26 above are the complete list in `Techguns.java`. `RailgunProjectile` and `AlienBlasterProjectile` are missing from it, although ClientProxy still binds renderers for both. An unregistered entity is not matched by FML's tracker or by any vanilla tracker class, so the server simulates it but no client ever sees it.

Lang: `entity.Techguns.RocketProjectile.name=Rocket` is the only projectile entity name in `en_US.lang`.

---

## 2. `GenericProjectile`: the base for almost everything

`GenericProjectile extends Entity implements IProjectile`. Nearly every class below inherits its movement, collision and damage pipeline.

### 2.1 Fields and constructor parameters

| Parameter | Meaning |
|---|---|
| `damage` | Base damage at short range. |
| `speed` | Initial speed in blocks/tick (the motion vector is normalized, then multiplied by `speed`). |
| `TTL` | Lifetime in ticks, stored as both `ticksToLive` (decrements) and `lifetime` (constant). |
| `spread` | Random aim error: yaw and pitch are each offset by a uniform value in [-spread, +spread] × **40** degrees. |
| `dmgDropStart`, `dmgDropEnd`, `dmgMin` | Linear damage fall-off with *distance traveled* (see 2.4). |
| `penetration` | Copied to [`TGDamageSource.armorPenetration`](../02-guns/damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource) (consumed by the damage system, see [`01-core`](../01-core/index.md)). |
| `blockdamage` | Whether this shot may alter blocks (fire, bio blobs, explosions). Guns pass `!player.enableSafemode`. NPCs pass false. |
| `leftGun` | Akimbo: mirrors the muzzle side offset. |
| `silenced` | Set by `setSilenced()`. Only `MiningDummyProjectile` reads it. |
| `shooter` | The `EntityLivingBase` that fired it, or null. Not synced. |

Defaults when built from the client/world-only constructor: `ticksToLive = lifetime = 100`, `speed = 1`, size 0.25 × 0.25 is **not** set (the world constructor keeps the vanilla 0.6 × 1.8 size). `isImmuneToFire = true` always.

### 2.2 Spawn position and initial velocity

1. Start at the shooter's eye position (`posY + eyeHeight`) with yaw = `rotationYawHead` and pitch = `rotationPitch`, each perturbed by the spread rule above.
2. Shift sideways 0.16 blocks: for the right hand (default), subtract (cos yaw × 0.16, 0.1, sin yaw × 0.16) from the position. For the left gun, add the horizontal parts instead, still lowering Y by 0.1.
3. Size 0.25 × 0.25 and `yOffset = 0`.
4. The direction vector is the standard look vector from yaw and pitch. It is normalized, the yaw/pitch fields are recomputed from it with `atan2(mx, mz)` and `atan2(my, horizontal)` (projectile convention, as for arrows), and the motion is set to **direction × speed**.
5. Guns may then call `shiftForward(offset)`, which adds motion × offset to the position (muzzle offset).

There is also a position-plus-yaw/pitch constructor (no shooter), used by turrets, dispensers and helicopters. It follows the same rules from a given point.

### 2.3 Per-tick update

Order of operations each tick (`onUpdate`):

1. **Server only, first tick:** remember the start position (`startX/Y/Z`) for the fall-off distance.
2. Save `lastTickPos`, then run vanilla `Entity.onUpdate`.
3. **Block ray-trace** from the current position to position + motion, ignoring liquids, `ignoreBlockWithoutBoundingBox = true`, `returnLastUncollidableBlock = false`. This runs on **both client and server**.
4. **Entity sweep (server only):** gather entities in the bounding box expanded by the motion and then by 1 block. For each one that `canBeCollidedWith()` and is not the shooter, expand its box by 0.3 and intersect the segment. The nearest hit wins and **replaces** any block hit. Note that only the segment start-to-block-hit is tested, so blocks still occlude entities.
5. If something was hit: a nether portal calls `setInPortal()`. Otherwise `onImpact(hit)` runs.
6. Move: position += motion.
7. Recompute yaw/pitch from the motion, unwrap them, and **smooth** toward the new value: `rot = prev + (new − prev) × 0.2`.
8. Drag: multiply motion by 0.99 in air. In water, multiply by 0.8 and spawn 4 vanilla `bubble` particles trailing at 0.25 × motion.
9. Gravity: `motionY -= getGravityVelocity()`. The base value is 0. Subclasses override it.
10. `ticksToLive--`. When it reaches ≤ 0, the projectile dies silently (no impact).

### 2.4 Damage fall-off

`getDamage()` uses the straight-line distance from the start position to the current position:

- distance ≤ `dmgDropStart`: full `damage`
- distance > `dmgDropEnd`: `dmgMin`
- in between: `dmgMin + (damage − dmgMin) × (1 − (d − start) / (end − start))`

Client-side copies never initialize the start position, so they would compute from (0,0,0). They never deal damage, so this has no effect.

### 2.5 `onImpact` (default)

**Entity hit:**

- Build the damage source via `getProjectileDamageSource()`. The default is `TGDamageSource.causeBulletDamage(this, shooter, GORE)` (`tg_bullet`, damage type PROJECTILE, `ignoreHurtresistTime = true`), with `armorPenetration` set and **knockback multiplier 0**.
- If the target is an `EntityLivingBase`:
  - `dmg = DamageSystem.getDamageFactor(shooter, target) × getDamage()`. The factor handles friendly fire and NPC teams (see [`01-core`](../01-core/index.md)).
  - If dmg > 0:
    - If the source's knockback multiplier is 0, first hit the target with a `tg_knockback` dummy source (PHYSICAL, DEFAULT death, knockback 1) for **0.01** damage. In effect this gives the target vanilla knockback while the real hit ignores hurt-resistance.
    - Apply the real damage.
    - If the target is an `EntityLiving`, set revenge/last attacker (rules below).
    - Call `onHitEffect(target)`. The base does nothing; subclasses add fire, poison and so on.
- Any other entity: `attackEntityFrom(src, getDamage())` with no factor.
- The projectile always dies after an entity hit, even when the factor was 0.

**Block hit:** if the block does *not* block movement (`getBlocksMovement() == false`, i.e. a solid block for this purpose), call `hitBlock(mop)`, then die. Passable blocks (tall grass, flowers and similar) let the projectile fly through.

Default `hitBlock` → `spawnImpactParticle(material)`. This runs on both sides. The sound plays from the server, the vanilla particle from the client.

| Block material | Sound (volume, pitch) | Vanilla particle |
|---|---|---|
| `rock` | `dig.stone` (2.0, 1.0) | `lava` |
| `wood` | `dig.wood` (2.0, 1.0) | `smoke` |
| `glass` | `dig.glass` (1.0, 1.0) | `smoke` |
| `iron`, `anvil` | `mob.irongolem.hit` (1.0, 1.0) | `lava` |
| anything else | `techguns:effects.bulletimpact` (1.0, 1.0) | `smoke` |

**Revenge targeting (`setAIRevengeTarget`):** if the shooter and the target are both [`ITGNpcTeam`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava), the target retaliates only if [`TGNpcFactions.isHostile(shooterFaction, targetFaction)`](../10-entities-npcs/ai.md#1-factions-tgnpcfaction-tgnpcfactions). Otherwise it retaliates whenever the shooter is not a player. Retaliation calls `setRevengeTarget(shooter)` and `setLastAttacker(shooter)`. Players hitting mobs rely on vanilla's own revenge logic.

**Unused:** `isHeadshot(ent, height)` has thresholds (creeper, skeleton and zombie 1.6; enderman 2.0) but its call is commented out.

### 2.6 NBT

| Key | Type |
|---|---|
| `damage` | float |
| `speed` | float |
| `ticksToLive` | int |
| `lifetime` | int |
| `damageDropStart` | int |
| `damageDropEnd` | int |
| `damageMin` | float |

Not saved: shooter, penetration, blockdamage, start position, subclass fields (except `SimpleProjectile`).

### 2.7 Network sync

There is no DataWatcher use. Clients receive the standard FML spawn packet (position, yaw/pitch, velocity), plus `IEntityAdditionalSpawnData` where noted below. The client-side copy is built with the `(World)` constructor, so it has default fields: TTL 100, no shooter, damage 0, and subclass defaults such as gravity. Server velocity updates (frequency 3, or 1 for grenades) keep it roughly on track. Client copies run the block-collision path themselves and die on contact.

---

## 3. Per-projectile reference

The summary table gives hard-coded values. "param" means the gun supplies it (see [`02-guns`](../02-guns/index.md)).

| Class | Gravity / tick | Water | Damage source (name, TG type, death type, gore chance, knockback) | Entity hit extras | Block hit | Pierces entities? |
|---|---|---|---|---|---|---|
| GenericProjectile | 0 | slows ×0.8 | `tg_bullet`, PROJECTILE, GORE, 0.5 (default), 0 + dummy | none | material sound/particle | no |
| SimpleProjectile | param (`gravity`, default 0.015 on client) | slows | as Generic | none | as Generic | no |
| FlamethrowerProjectile | param | **dies** | `tg_fire`, FIRE, DEFAULT, 0.5, 0 + dummy | sets fire 3 s | 50 % chance to place fire if `ignite`; `lava` particle | no |
| LaserProjectile | 0 | slows | `tg_energy`, ENERGY, LASER, 0.35, 0 (no dummy) | spawns `LaserBeam2` visual on creation | dies only on movement-blocking blocks | no |
| RocketProjectile | param (default 0.01) | continues, bubbles | `tg_explosion`, EXPLOSION, GORE, 1.0, ×3 | direct hit for full `damage`, then explosion | explosion on solid block | no |
| NukeRocketProjectile | param (default 0.01) | continues | `tg_explosion`, EXPLOSION, GORE, 1.0, ×10 | as Rocket, nuke FX and sound | nuke explosion | no |
| BiogunProjectile | 0.01 | **dies** | `tg_poison`, POISON, BIO, 1.0, 0 + dummy | Poison IV for 5 s | places or grows bio blobs; `slime` particle | no |
| TeslaProjectile | 0 | slows | `tg_lightning`, LIGHTNING, LASER, 0.5, 0 (no dummy) | chain lightning | dies on any block | no |
| GrenadeProjectile | param (default 0.01) | **dies, no explosion** | vanilla explosion source on direct hit | explodes on entity contact | bounces (reflect ×0.5) until bounces run out | no |
| FragGrenadeProjectile | as Grenade | dies | as Grenade | as Grenade | as Grenade, frag FX | no |
| GrenadeLauncherProjectile | as Grenade | dies | as Grenade | as Grenade | bounce keeps blockdamage, TTL −20, smoke trail | no |
| FlashBangProjectile | as Grenade | dies | none | blindness and nausea area | as Grenade | no |
| CyberdemonBlasterProjectile | param | **dies** | `tg_fire`, FIRE, DEFAULT, 0.5, **×1** (no dummy) | impact FX (server-side call, see bugs) | impact FX; 50 % fire if blockdamage | no |
| BlasterRifleProjectile | 0 | slows | `tg_energy`, ENERGY, LASER, 0.25, 0 + dummy | none | vanilla `smoke` only, no sound | no |
| AlienBlasterProjectile | 0 | slows | `tg_fire`, FIRE, LASER, 0.5, 0 + dummy | sets fire 3 s and a small explosion | small explosion; 100 % fire on face if blockdamage | no |
| EnergyBlastProjectile | 0 | slows | `tg_energy`, ENERGY, GORE, 1.0, **×1** (no dummy) | `BlueBlasterExplosion` FX | `BlueBlasterExplosion` FX | no |
| PowerHammerProjectile | 0 | **dies** | `powerhammer`, PHYSICAL, GORE, 1.0, ×1 | impact sound | ground-impact sound; dies on *any* block | no |
| InvisibleProjectile | 0 | slows | [`chainsaw`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition), PHYSICAL, GORE, 1.0, 0 + dummy, ignores hurt-resist | none | as Generic | no |
| SonicShotgunProjectile | 0 | slows | `tg_sonic`, PHYSICAL, GORE, 1.0, ×2 (+ dummy anyway), ignores hurt-resist | each entity hit once per volley | dies on solid block; main projectile sends impact FX | **yes** |
| MiningDummyProjectile | 0 | slows | `tg_bullet`, PROJECTILE, GORE | direct health subtraction | **breaks the block and drops it** | no |
| MovingBeam / NeutrinoBeam | n/a (hitscan each tick) | n/a | `tg_rad`, RADIATION, LASER, 1.0, 0 + dummy | damage every tick while aimed at a target | none | n/a |
| BeamProjectile | 0 | slows | `tg_energy`, ENERGY, LASER, 0.35, 0 (no dummy) | none | dies on any block | no |
| RailgunProjectile | 0 | slows | vanilla-style `bullet` ([`BulletDamageSource`](../01-core/textures-sounds-lang.md#missing-or-untranslated-keys-used-by-core-code)) | damage −2 per target hit, keeps flying | `reddust` particle, dies | **yes**, while damage > 2 |
| BlasterProjectile (deprecated) | 0 | slows | `bullet` | none | `reddust` | no |
| PiercingProjectile (deprecated) | 0 | slows | as Generic | line-of-sight checked, each entity once | ignored (passes through blocks) | **yes** |

"0 + dummy" means the real hit has no knockback and the 0.01 dummy hit provides vanilla knockback. "×N" is `TGDamageSource.knockbackMultiplier`, used by the damage system ([`01-core`](../01-core/index.md)).

The following sections cover what the table does not.

### 3.1 `SimpleProjectile`
- An extra constructor takes a texture path, scale and gravity. **Only gravity has any effect.** Texture and scale are stored but never synced, and the renderer uses a fixed texture and scale (see renderers.md).
- NBT adds `projectileTexture` (string, a resource path read back under the `techguns` domain), `scale` (float) and `gravity` (float). Read and write each print a debug line to stdout.

### 3.2 `FlamethrowerProjectile`
- Extra parameters: `gravity`, `piercing` (stored, unused), `ignite` (also passed as `blockdamage`). Hard-coded `chanceToIgnite = 0.5` and `entityIgniteTime = 3` s.
- Constructor calls `proxy.spawnParticle("Flamethrower", …, motion × 0.2)`. This runs on the server and is a no-op on a dedicated server.
- **Client each tick** (when not in water): two legacy `"Flamethrower"` particles at the projectile position. One gets velocity (motion × 0.1, with Y adjusted by −gravity), the other gets (0, +gravity, 0). See fx-catalog.md §Legacy particles.
- **Fire placement** (`hitBlock`, only when `ignite` is set, which is server-side only because the field is not synced). With 50 % probability:
  - Hit face down: place fire below if air.
  - Hit face up: if the block above is air, convert farmland at the hit block to dirt, then place fire above.
  - Hit face N/S/W/E: place fire in the adjacent air block on that face.
  - Then special cases: if the hit block is [`TGBlocks.camoNetRoof`](../07-blocks/block-catalog.md#registered-blocks) or [`camoNetPane`](../07-blocks/block-catalog.md#registered-blocks), replace it with fire (and emit `lava`). Else, if the block *below* the hit block is farmland, turn it to dirt and replace the hit block with fire.
- On a block hit it always emits the vanilla `lava` particle.

### 3.3 `LaserProjectile`
- An invisible damage carrier for the lasergun and scatter beam gun.
- After `super` it overwrites the `speed` *field* with [`Lasergun.SPEED`](../02-guns/gun-catalog.md#table-d-ai-visual-hooks-and-textures) (5.0) and the TTL with `Lasergun.LIFETIME` (**5 ticks**). The motion was already computed from the gun's speed parameter, so range = gun speed × 5.
- On construction (server) it spawns a `LaserBeam2` visual at its own position (see 3.15).
- Entity hit: damage × factor, no dummy knockback, no revenge, no `onHitEffect`. It dies.
- Block hit: dies only on movement-blocking blocks. No particles or sound.

### 3.4 `RocketProjectile` and `NukeRocketProjectile`
- Extra parameters: `radius`, `gravity`. The client defaults gravity to 0.01.
- **Entity hit (server):** `attackEntityFrom(source, damage)` with the raw base damage (no drop-off, no damage factor), revenge, then `explodeRocket()`.
- **Block hit:** if the block blocks movement, `explodeRocket()`. The client-side copy also reaches this and just dies.
- `explodeRocket()` (server only):
  - Rocket: send `PacketSpawnParticle("RocketExplosion")` to players within 50 blocks, then `ProjectileExplosion(size = radius, entityDamage = (int)damage, r1 = radius × 0.5, r2 = radius × 1.5).doExplosion(blockdamage, shooter)`. The explosion sound is the default `random.explode`.
  - Nuke: `PacketSpawnParticle("NukeExplosion")` within **150** blocks, `playSoundEffect("techguns:effects.nukeexplosion", volume 8.0, pitch 1.0)`, then the same explosion formula.
  - The projectile dies on both sides.
- **Client each tick** (not in water): legacy `"Fireblast"` particle at the rocket with velocity (motion × 0.1, Y − gravity). Two bubbles per tick in water. Rockets keep flying underwater.
- Direct-hit source: Rocket `goreChance 1.0`, `knockbackMultiplier 3.0`. Nuke `goreChance 1.0`, knockback 10.0.

### 3.5 `BiogunProjectile`
- Hard-coded `gravity = 0.01` and a shadowing field `speed = 0.75` (unused in practice). The full constructor multiplies the motion by the `speed` *parameter* a second time (bug [PFX-12](index.md#suspected-bugs); harmless because the gun passes 1.0).
- `level` (1–3) is the number of ammo units consumed by the charge (see [`02-guns`](../02-guns/index.md)). It is synced via spawn data as one byte, but not saved to NBT.
- Entity hit (`onHitEffect`): Poison amplifier 3 (Poison IV), 100 ticks.
- Block hit (`hitBlock`):
  - If the hit block is `TGBlocks.bioBlobSmall`: call [`BioblobTileEnt.hitBlob(level, shooter)`](../09-machines-utility/misc-tile-entities.md#tile-entity-bioblobtileent) on its tile. This runs on both sides; the tile handles growth and explosion, see [`07-blocks`](../07-blocks/index.md).
  - Otherwise, server only: compute the adjacent position on the hit face.
    - If it is air: if the shooter is a player, post a Forge `BlockEvent.PlaceEvent` (snapshot of that position, placed block = the hit block, player = shooter). If it is not canceled, place `bioBlobSmall` there. If level > 1, call `hitBlob(level − 1, shooter)` on the new tile, then set the block metadata to the hit side (0–5, the face the blob sticks to).
    - If it is not air and is a bio blob tile, call `hitBlob(level, shooter)` on it.
  - Always emit the vanilla `slime` particle.
- **Blob placement ignores `blockdamage`** (safe mode). See bugs.
- Dies on entering water (with bubbles).
- Dead code: `updateBlockLevel`/`setBlockForLevel` (an older level-merge scheme that exploded at level > 3 with `ProjectileExplosion(1.5, 30, 1.0, 2.0)` and set fire).

### 3.6 `TeslaProjectile` (chain lightning)
- Invisible. The speed field is [`Teslagun.SPEED`](../02-guns/index.md#coverage) (5.0), the lifetime `Teslagun.LIFETIME` (**10 ticks**), and spread is forced to 0. Motion uses `Teslagun.SPEED`, so the range is 50 blocks.
- Extra parameters: `chain` (number of extra targets), `chainDmgDrop` (damage removed per chain step).
- **Entity hit (server):**
  1. Damage the primary target with `getDamage() × factor` (factor 1 for non-living targets), then revenge.
  2. **Chaining:** collect every entity in a cube of ±`Teslagun.CHAIN_RANGE` (5.0 blocks) around the primary target, excluding that target. In list order, take each one that casts to `EntityLivingBase` and is not the shooter. Keep taking until `chain` targets are collected; at least one is always taken, even when `chain` is 0 (bug [PFX-08](index.md#suspected-bugs)). There is no line-of-sight, alive or team check.
  3. For each chain target *i* (0-based), in order:
     - Spawn a `TeslaBeam` visual from the previous point to the target. The first previous point is the primary target's center (`posY + height/2`); after that it is the previous chain target's center.
     - Apply damage `getDamage() − chainDmgDrop × (i+1) × factor` (operator-precedence bug [PFX-07](index.md#suspected-bugs): the factor scales only the drop).
     - Revenge.
- Any block hit kills it (no passability check). There is no visual on a miss.
- Dead code: a "chaining bolt" constructor (it feeds radians into degree fields) and `findNextTarget`.

### 3.7 `GrenadeProjectile` family
Fields: `radius`, `gravity` (client default 0.01), its own shadowing `blockdamage`, and `bounces` (remaining bounces).

**Hand throw** ([`GenericGrenade.spawnGrenade`](../02-guns/per-gun-behavior.md#grenades-genericgrenade-fraggrenade-flashbang), see [`02-guns`](../02-guns/index.md)): speed `0.5 + 0.5 × charge`, TTL 200, spread 0.05, gravity `0.015 / charge`, no blockdamage, penetration 0, the item's `maxBounces`.

**Dispenser** (`Techguns.java`): stick grenade uses speed 1.0, TTL 200, spread 0.05, gravity 0.015, no blockdamage, penetration 0. Frag grenade is the same with TTL 120.

**Update:** the base update runs, then:
- If `ticksToLive ≤ 1`: `explode(null)` and die. This is the fuse.
- If in water: die **without exploding**.

**Impact:**
- Block hit on a block that does *not* block movement: ignored (it flies through).
- Entity hit, or `bounces ≤ 0`: `explode(entityHit)`, then die.
- Otherwise **bounce**:
  1. Decrement `bounces`.
  2. Take the face normal from `sideHit`: 0 = (0,−1,0), 1 = (0,1,0), 2 = (0,0,−1), 3 = (0,0,1), 4 = (−1,0,0), 5 = (1,0,0).
  3. Reflect the motion: **r = v − 2(v·n)n**. Vanilla 1.7.10 `Vec3.subtract(a)` computes *a − this*.
  4. Play `mob.irongolem.hit` (volume 2.0, pitch 1.0).
  5. Server only: build a **new** grenade entity via `createBounceProjectile()` at the current position, set its motion to r × `getBounceSpeedFactor()` (0.5), spawn it, call `createProjectileTrail(newGrenade)`, then kill the old entity.

**`createBounceProjectile()` per class:**

| Class | Remaining TTL | blockdamage | Speed field | Trail |
|---|---|---|---|---|
| `GrenadeProjectile` | current `ticksToLive` | **false** (bug [PFX-10](index.md#suspected-bugs)) | same | none |
| `FragGrenadeProjectile` | current `ticksToLive` | **false** | same | none |
| `GrenadeLauncherProjectile` | `ticksToLive − 20` | kept | × 0.75 (unused, since the motion is overwritten) | `PacketSpawnParticleOnEntity("GrenadeLauncherTrail", newGrenade)` within 200 blocks |

The new grenade gets spread 0. If there is a shooter, it is built via the shooter constructor and then repositioned.

**`explode(entityHit)`:**
1. `ProjectileExplosion(pos, shooter, size = radius, entityDamage = (int)damage, r1 = radius × 0.5, r2 = radius × 1.5)`, sound `techguns:effects.explosion1`.
2. If an entity was hit directly: factor = damage factor (1 for non-living). If factor > 0, `attackEntityFrom(DamageSource.setExplosionSource(vanillaExplosion), factor × damage)` (the vanilla `explosion`/`explosion.player` source), then revenge.
3. `doExplosion(blockdamage, shooter)`.
4. Server: `PacketSpawnParticle(getExplosionEffect())` within 50 blocks. Grenade and launcher use `"RocketExplosion"`; frag uses `"FragGrenadeExplosion"`.

The client copy also calls `explode` (with radius 0 and damage 0), which is harmless. Grenade timing on the client uses the default TTL of 100, but the server's death removes it anyway.

**`FlashBangProjectile.explode`:** no damage, no sound, no particles. It builds Blindness (amplifier 1, 200 t) and Nausea (amplifier 1, 200 t) and applies them to every `EntityLivingBase` in the grenade's bounding box expanded by (4, 2, 4) whose squared distance is < 32. The strength factor is `f = 1 − √d² / √32`. Duration is `f × 200 + 0.5`, applied only if > 20 ticks. Instant potions would use `affectEntity` (none here). It affects the thrower too.

### 3.8 `CyberdemonBlasterProjectile`
- Extra parameters: `gravity`, `piercing` (unused), `ignite`. `ignite` is also passed as `blockdamage`, and fire placement is gated on `blockdamage`. Hard-coded chance 0.5.
- The world constructor sets `speed` to 0.85 and multiplies the (zero) motion, which is a no-op.
- The constructor calls `proxy.spawnParticle("BlasterFlare", motion × 0.2)`, server-side.
- **Block hit** (both sides): `proxy.createFX("NetherBlasterImpactFlare")` and `createFX("MediumExplosionSmoke")` at the projectile. The client call shows the FX. Then the same fire-placement routine as the flamethrower (3.2), gated on `blockdamage`.
- **Entity hit:** `onHitEffect` makes the same two `createFX` calls, but this only runs on the server, so on a dedicated server the entity-hit FX never shows (bug [PFX-05](index.md#suspected-bugs)). It does **not** set the target on fire, despite the field `entityIgniteTime = 3`.
- The trail FX is sent by the gun: `PacketSpawnParticleOnEntity("CyberDemonBlasterTrail")`.

### 3.9 `AlienBlasterProjectile` (not registered, see §1)
- Extra parameter `ignite` (stored, unused). Hard-coded `chanceToIgnite = 1.0`, `entityIgniteTime = 3`.
- `doExplosion()`: server sends `PacketSpawnParticle("AlienExplosion")` within 50 blocks, then on both sides runs `ProjectileExplosion(size 0.5, entityDamage (int)getDamage(), r1 0.25, r2 0.75).doExplosion(false, shooter)`. That is a tiny AoE that never breaks blocks.
- `hitBlock`: if `blockdamage`, burn the face (same routine, 100 % chance), then `doExplosion()`.
- `onHitEffect`: set fire 3 s, then `doExplosion()`.
- The gun sends trail FX `"AlienBlasterTrail"` on the entity. Because the entity is not tracked to clients, that packet finds no entity and prints a warning (bug PFX-01).

### 3.10 `EnergyBlastProjectile` (MIB gun)
- Akimbo-aware constructor (`leftgun`).
- On both block and entity impact, `doExplosion()` sends only `PacketSpawnParticle("BlueBlasterExplosion")` (50 blocks). The AoE explosion is commented out.
- The trail FX is sent by the gun: `"BlueBlasterTrail"`.

### 3.11 `PowerHammerProjectile`
- A short-range melee "shockwave". Spread is 0 and the size is set to the `collisionSize` parameter. The `charge` parameter is stored but unused.
- Rendered in pass 1 (translucent).
- **Any** impact (entity or block, including passable blocks): entity → damage `getDamage() × factor` with the `powerhammer` source (gore 1.0), revenge, and sound `techguns:guns.powerhammerImpact`. Block → sound `techguns:guns.powerhammerImpactGround`. Then it dies, and `proxy.spawnParticle("impact")` runs on both sides (legacy particle, see fx-catalog.md).
- Dies in water.

### 3.12 `InvisibleProjectile` (chainsaw)
- Pure base behavior with the source `chainsaw` (PHYSICAL, GORE, gore 1.0, knockback 0 + dummy, ignores hurt-resist). Bullet impact sounds and particles play on blocks.

### 3.13 `SonicShotgunProjectile`
- The gun fires a volley that shares **one** `entitiesHit` list: a center projectile (`mainProjectile = true`) plus rings of 5 × j projectiles for j = 1..2 (15 side projectiles). Ring j sits at angle `0.075 × j` rad off-axis, evenly spaced around the axis. See [`02-guns`](../02-guns/index.md).
- Entity hit: if the entity is not yet in the list, apply the dummy knockback (0.01), then damage × factor with the `tg_sonic` source (knockback ×2, gore 1.0), `onHitEffect`, revenge, and add the entity to the list. **The projectile keeps flying.** If `entitiesHit` is null (any other spawner), entity hits do nothing.
- Block hit (solid): `hitBlock`, which on the main projectile sends `PacketSpawnParticle("SonicShotgunImpact")` within 50 blocks, then dies. Side projectiles die silently.
- Spawn data: `mainProjectile` (boolean). Render pass: the main projectile renders in pass 1, the others are "rendered" in pass 0 by a renderer that draws nothing for them.
- The trail FX is sent by the gun: `PacketSpawnParticleOnEntity("SonicShotgunTrail", mainProjectile)` (200 blocks).

### 3.14 `MiningDummyProjectile`
- The constructor always passes `blockdamage = true`.
- Entity hit:
  - Source `tg_bullet` (GORE).
  - For `EntityLiving` targets with health > 0: compute the armor-reduced amount via [`DamageSystem.getDamage(src, getDamage(), target)`](../02-guns/index.md#coverage) and **subtract it directly from health**.
  - If not silenced, also `attackEntityFrom(src, 0.1)` for the hurt animation and aggro.
  - If health ≤ 0: with probability `goreChance`, set the death type to DEFAULT (**inverted** compared with the damage system, bug [PFX-13](index.md#suspected-bugs)), then call `onDeath(src)`.
  - Other entities (including players, which are not `EntityLiving`): `attackEntityFrom(src, getDamage())`.
- Block hit (any block, passable or not):
  - Unbreakable blocks (hardness < 0): `dig.stone` and `lava` only.
  - Otherwise: the material sound/particle table from 2.5, then **server-side `dropBlockAsItem` (fortune 0) and set to air**, with no break event, no tool check and no permission check (bug [PFX-14](index.md#suspected-bugs)).
  - It dies either way.

### 3.15 Beam visual entities

**`TeslaBeam`** (`extends Entity`, `IEntityAdditionalSpawnData`). This is visual only.
- `ticksToLive = lifetime = Teslagun.LIFETIME` (10). It dies when the counter reaches 0. `ignoreFrustumCheck = true` (client constructor).
- **Gun constructor** `(world, shooter[, beamlength])`:
  1. Place at the shooter's eye with the shooter's head yaw and pitch via `setInitialPosition` (overridable).
  2. Shift right 0.16 and down 0.1.
  3. Pick a random `seed`.
  4. If no length is given, trace with [`MathUtil.traceVector(start, look, 10, 10 steps)`](../01-core/utilities.md#mathutil). The hit position becomes the target; on a miss the target is set to the *direction vector itself* (bug [PFX-03](index.md#suspected-bugs)).
  5. Server: send `PacketSpawnParticle(getParticleName())` at the target (TeslaBeam uses `"TeslaFlare"`) and, if non-null, `getParticleNameStart()` at the start, both within 50 blocks.
  6. `beamlength` = distance from start to target.
- **Chain constructor** `(world, shooter, x, y, z, target)`:
  - Placed at (x, y, z), yaw = `atan2(dz, dx)` in degrees − 90, pitch = `atan2(horizontal, dy)` in degrees + 90, where d = start − target. The dy term has a sign slip (bug [PFX-09](index.md#suspected-bugs)).
  - `beamlength` = distance to the target's center. There are no particles.
- Spawn data: `beamlength` (double), `seed` (long).

**`LaserBeam2`** (`extends TeslaBeam`):
- Lifetime `Lasergun.BEAM_LIFETIME` = 5. Particle names: end `"LaserFlareEnd"`, start `"LaserFlareStart"`.
- `setInitialPosition` places it at eye + look × 0.3.
- The `(world, shooter, projectile)` constructor runs the TeslaBeam trace (so `beamlength` comes from the shooter's eye) and then moves the entity to the `LaserProjectile`'s position with yaw = −projectile yaw and pitch = −projectile pitch.
- Spawn data: `beamlength` only (no seed).

**`LaserBeam`** (deprecated, never spawned):
- Lifetime 5. Places 0.36 blocks in front of the shooter's eye and immediately calls `proxy.spawnParticle("LaserFlareStart")`.
- Traces 500 blocks with `traceVector(…, 10, 10)`. On a miss the target equals the direction offset (same bug as TeslaBeam).
- Tick 1 (client): legacy `"LaserFlareEnd"` at the target.
- Spawn data: target x, y, z (3 doubles).

**`MovingBeam` / `NeutrinoBeam`** (continuous hitscan beam, extends `GenericProjectile`):
- Not collidable. Rendered in pass 1. `ignoreFrustumCheck`.
- Client constructor: lifetime `Beamgun.BEAM_LIFETIME` (10), and attaches FX `"BeamGunMuzzleFX"` via `createFXOnEntity`.
- Server lifetime = the gun's TTL parameter.
- Spawn data: shooter entity id (int, 0 if none). The client resolves the shooter lazily.
- `onUpdate` **replaces** the base update. There is no motion. Each tick:
  1. Resolve the shooter.
  2. Position the beam origin:
     - Local first-person player: shooter feet position + `getFPOffset()` rotated by pitch and head yaw. MovingBeam offset is (0, −0.08, 0.12); Neutrino is (0, −0.08, 0.24).
     - [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret): feet + rotated (0, eyeHeight − 0.1, −0.1).
     - Otherwise: shifted 0.16 sideways by body yaw and raised to eye − 0.1 + `get3PYOffset()` (MovingBeam 0, Neutrino −0.2).
     - Yaw = shooter head yaw, pitch = shooter pitch.
     - With no shooter, it uses its own yaw and pitch.
  3. Trace along the look direction with `traceVector(origin, dir, 12, 4)`, i.e. up to 48 blocks.
  4. Block hit → target = hit point. Entity hit → target = hit point, and on the **server** record `distance` (used for fall-off) and call the base `onImpact` (damage, dummy knockback, revenge, gore), then un-kill itself. **This repeats every tick**, so the beam deals damage once per tick, limited only by the damage system's hurt-resistance handling (the source ignores hurt-resist). Miss → target = direction × 100 *as an absolute point* (bug PFX-03).
  5. `proxy.createFX("BeamGunImpactFX")` at the target. On a dedicated server only the client call shows it; see bug [PFX-04](index.md#suspected-bugs) for single-player.
  6. `beamlength` = origin-to-target distance.
  7. `ticksToLive--`; it dies at 0.
- `getDistanceTravelled()` returns the recorded beam distance, so fall-off is per beam length.
- `getBeam3PYOffset()` (MovingBeam 0, Neutrino −0.3) is never used.
- Damage source `tg_rad` (RADIATION, LASER, gore 1.0).

**`BeamProjectile`**: never spawned. Speed field `Beamgun.SPEED` (5), TTL `Beamgun.LIFETIME` (5), not collidable. Every impact (entity or any block) ends it after damage (`tg_energy`, LASER, gore 0.35).

### 3.16 `RailgunProjectile` (never spawned)
- Speed field `Lasergun.SPEED`; the world constructor TTL is 50.
- Entity hit: `BulletDamageSource.causeBulletDamage(this, shooter, false)` (name `bullet`, from `java/techguns/util/BulletDamageSource.java`, see [`01-core`](../01-core/index.md)) for `getDamage()`. If damage > 2, reduce the stored damage by 2 and keep flying; otherwise die.
- Block hit: `reddust` particle and die.
- Each tick (both sides, not in water), it spawns 7 legacy `"AlienBlasterFlare"` particles: one at the projectile and three pairs at ±0.2, ±0.4 and ±0.6 × motion along the path, each with velocity motion × 0.1.

### 3.17 `PiercingProjectile` (deprecated, unregistered)
- Grows from `minSize` to `maxSize` (collision width and height) linearly over `sizeEnd` ticks.
- It has its own shadowing `ticksToLive`, so the base TTL logic uses the default value (bug-ish, dead code).
- On an entity hit it checks line of sight by repeated `traceVector` from the start position, and damages each entity once (with the dummy knockback).
- Block hits are ignored.
- Spawn data: minSize, maxSize (floats), sizeEnd, ticksToLive (shorts).

### 3.18 `BlasterProjectile` (deprecated)
Lasergun constants. `bullet` damage on entities, `reddust` on blocks, and it dies on any impact.

### 3.19 `FlyingGibs`
A body-part entity spawned **client-side** by the gore death effect (see death-effects.md). Registered as entity 16, but it is only ever spawned into the client world.
- Fields:
  - `timeToLive = maxTimeToLive` = 150 + rand(0..100).
  - `gravity = 0.03`.
  - `size` = (entity width + height) / 2.
  - Random `rotationAxis` (each component 0–1).
  - `bodypart` index.
  - `entity` = the dying entity (used for texture, model and child scaling).
  - `GoreData data`.
  - `hitGroundTTL` = the `timeToLive` value at the moment it first lands.
- Tick:
  1. Decrement `timeToLive` and die at 0.
  2. `motionY −= 0.03`.
  3. If inside lava: motionY = 0.2, random horizontal ±0.2, and play `random.fizz` (volume 0.4, pitch 2.0–2.4).
  4. `moveEntity` (with block collision).
  5. Horizontal friction: 0.98 in air, block slipperiness × 0.98 on the ground. Vertical friction is 0.98.
  6. **Bounce:** on the ground, `motionY ×= −0.9`.
  7. **Blood trail:** while airborne and `data.showBlood`, spawn one blood particle per tick. Parameters:
     - [`EntityParticleAnimated`](particle-system.md#4-legacy-sprite-particle-entityparticleanimated), `blood4x4.png`, 2 × 2 grid with 4 sprites used as random variations, scale 5, ALPHA blend.
     - Velocity = gib motion, gravity 0.07, max age 15.
     - Scale grows from size × 2 to size × 4, soft.
     - Color fades from the blood RGB at alpha 255 to the blood RGB at alpha 0, linearly starting at 50 % of life.
     - Random rotation, randomness 0.8, motion damping 0.92.
- No NBT. Water uses vanilla material acceleration.

---

## 4. Entity sheets (consistency pass)

The sections above hold the behavior in depth. This section restates, in the CONVENTIONS field order, the four thinnest classes as full sheets, and then gives every class a one-row summary with the same fields (registration, size, speed, gravity, TTL, collision, damage, explosion, effects, sync, renderer, sounds, FX names, users). Nothing here contradicts the sections above.

### 4.1 `GenericProjectile` (the base)

| Field | Content |
|---|---|
| **Identity** | Class `techguns.entities.projectiles.GenericProjectile`, extends `Entity`, implements `IProjectile`. Registered as ID 1, name `GenericProjectile` (global string `Techguns.GenericProjectile`), tracking range 80, update frequency 3, velocity updates on. No lang entry (a projectile is never named in the UI). Not an item, so no metadata, creative tab or oredict. |
| **Obtaining** | Not an item. Spawned by every bullet-type gun that does not override `spawnProjectile` (pistol, shotguns, rifles, SMGs, minigun and so on; full list in [`02-guns`](../02-guns/gun-catalog.md)), by NPC guns through the same gun code, and by the [`AttackHelicopter`](../10-entities-npcs/per-mob.md#16-attack-helicopter-attackhelicopter-boss) machine gun. It is also the superclass of nearly every other projectile. |
| **Numbers** | All come from the firing gun: `damage`, `speed` (gun default 1.5 blocks/tick, scaled by `setSpeed`), `TTL`, `spread` (offset is uniform in plus or minus `spread` times 40 degrees), `dmgDropStart`, `dmgDropEnd`, `dmgMin`, `penetration`, `blockdamage` (true only for a player with safe mode off). Size 0.25 x 0.25 on the gun and position constructors; the world (client) constructor does not set it. Gravity 0. Air drag 0.99 per tick, water 0.8 with bubbles. `isImmuneToFire` true. Defaults for the client copy: TTL 100, speed 1, damage 0. Helicopter constants: damage 12.0, speed 1.0, TTL 100, spread 0.05, drop 30 to 40, min damage 8.0, penetration 0.25, no block damage. The unused [`TurretEntityUNUSED`](../10-entities-npcs/per-mob.md#19-turretentityunused-dead-code) passes damage 10.0, speed 1.0, TTL 50, spread 0.05, drop 25 to 40, min 5.0. |
| **Behavior** | Tick order in section 2.3, impact in 2.5. Collision: block ray-trace on both sides, entity sweep (box expanded by 0.3, nearest wins) on the server only; non-solid blocks such as grass are passed through; solid blocks end it with the material sound table. Damage source `tg_bullet`, damage type PROJECTILE, death type GORE, gore chance 0.5 (damage-system default), `ignoreHurtresistTime` true, knockback multiplier 0 plus a 0.01 dummy `tg_knockback` hit. Explosion: none. Effects: none beyond impact particles. |
| **Persistence and sync** | NBT keys in 2.6. No DataWatcher, no `IEntityAdditionalSpawnData` on the base. Clients see the standard FML spawn packet and velocity updates. |
| **UI** | none (no tooltip, GUI or chat). Kill message `death.attack.tg_bullet` (see [lang.md](lang.md)). |
| **Visuals** | Renderer `RenderGenericBullet`, texture [`textures/entity/bullet.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) (32x32; only the top-left 16x5 pixel rectangle is sampled; palette in [textures.md](textures.md)). Crossed quads 0.2 x 0.05 blocks, drawn from tick 2. FX names from the projectile itself: none. |
| **Audio** | `techguns:effects.bulletimpact` (volume 1.0, pitch 1.0) on a block of an unlisted material; `dig.stone` and `dig.wood` (2.0, 1.0); `dig.glass` and `mob.irongolem.hit` (1.0, 1.0). Durations and sample rates in [sounds.md](sounds.md). |
| **Relationships** | Fired by [`02-guns`](../02-guns/index.md) bullet guns and by NPCs ([`10-entities-npcs`](../10-entities-npcs/index.md)). Uses `DamageSystem` and `TGDamageSource` ([`01-core`](../01-core/index.md)). Subclassed by every projectile below except the plain `Entity` classes `TeslaBeam`, `LaserBeam`, `LaserBeam2` and `FlyingGibs`. |
| **Bugs** | [PFX-04](index.md#suspected-bugs), [PFX-15](index.md#suspected-bugs), [PFX-23](index.md#suspected-bugs), [PFX-27](index.md#suspected-bugs) (subclasses), [PFX-30](index.md#suspected-bugs). |

### 4.2 `SimpleProjectile`

| Field | Content |
|---|---|
| **Identity** | Class `SimpleProjectile extends GenericProjectile`. ID 2, registration name `SimpleProjectile`, tracking 80, frequency 3, velocity updates on. No lang entry. |
| **Obtaining** | Not an item. Spawned only by the [`Handgun`](../02-guns/sheets-handguns-smgs.md#hand-cannon-handcannon) gun class, registered once as [`handcannon`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) (English name "Hand Cannon", lang key `techguns.item.handcannon.name`; recipe in [`06-items`](../06-items/recipes.md)). The constructor with texture, scale and gravity is the one used. |
| **Numbers** | Handcannon call: damage 8.0 times the damage bonus; speed fixed at 1.0 (ignores the gun's 1.5 default); TTL 25; spread from the gun accuracy (0.035); fall-off 10 to 25 blocks down to min damage 5.0; penetration from the gun; blockdamage true when safe mode is off. Arguments: texture `techguns:textures/entity/handgunBullet.png`, scale 1.0, gravity 0.015. World constructor: gravity 0.015, scale 0, texture field default `techguns:textures/items/testItem.png` (an existing item texture, never drawn). Size 0.25 x 0.25. |
| **Behavior** | As the base, except `getGravityVelocity` returns the stored `gravity`, so the ball drops 0.015 blocks/tick per tick (after drag). Collision, damage source (`tg_bullet`, GORE) and impact sounds are the base ones. Only `gravity` has any effect; texture and scale are never used (PFX-34). |
| **Persistence and sync** | NBT adds `projectileTexture` (string; the resource path, re-read under the `techguns` domain), `scale` (float) and `gravity` (float), with a debug print on every read and write (PFX-30). Gravity is not synced, so the client copy always uses its 0.015 default, which equals the only value any gun passes. |
| **UI** | none. |
| **Visuals** | Renderer `RenderSimpleProjectile(1.0, techguns:textures/entity/handgunBullet.png)`, a camera-facing quad 0.1 blocks across using the full 16x16 texture. Measured palette (ASSET-FACTS): average #616161, dominant #B0B0B0, #707070, #303030, i.e. neutral grays of a shaded ball. No FX names. |
| **Audio** | Base impact sounds only (volumes and pitches as 4.1). Gun fire and reload sounds (`guns.handgunFire`, `guns.handgunReload`) belong to [`03-gun-visuals-audio`](../03-gun-visuals-audio/index.md). |
| **Relationships** | Fired by `handcannon` only. |
| **Bugs** | [PFX-34](index.md#suspected-bugs), [PFX-30](index.md#suspected-bugs). |

### 4.3 `InvisibleProjectile`

| Field | Content |
|---|---|
| **Identity** | Class `InvisibleProjectile extends GenericProjectile`. ID 21, registration name `InvisibleProjectile`, tracking 80, frequency 3, velocity updates on. No lang entry. |
| **Obtaining** | Not an item. Spawned by the [`Chainsaw`](../02-guns/sheets-launchers-tools-grenades.md#chainsaw-chainsaw) gun class (`chainsaw`, a melee-range pseudo-gun that uses flamethrower fuel tanks as ammo). |
| **Numbers** | Chainsaw call: damage 10.0 times the damage bonus; speed from the gun (default 1.5); TTL 2 ticks; spread 0.0 (accuracy argument); fall-off values are the gun defaults; penetration 0.25; blockdamage true when safe mode is off. With speed 1.5 and a 2-tick life the reach is about 3 blocks. Size 0.25 x 0.25 on the gun constructor. There are also a position-plus-yaw/pitch constructor and a position-only one, which no gun uses. Gravity 0. |
| **Behavior** | Base flight and collision. Its only override is the damage source: name `chainsaw`, damage type PHYSICAL, death type GORE, gore chance 1.0, knockback multiplier 0 (so the dummy 0.01 knockback hit runs as for bullets), armor penetration equal to the projectile's `penetration`, `ignoreHurtresistTime` true. Block hits use the default `hitBlock`, so the bullet-impact sound and particle fire on the struck block. No explosion, no on-hit effect. |
| **Persistence and sync** | Base NBT keys only. No extra spawn data. |
| **UI** | none. The kill message is `death.attack.chainsaw` ("%s was dismembered by %s with a chainsaw"); there is no `.item` variant (see [lang.md](lang.md)). |
| **Visuals** | `RenderDummy` (draws nothing). No texture, no FX. The chainsaw's visuals are all on the gun model ([`03-gun-visuals-audio`](../03-gun-visuals-audio/index.md)). |
| **Audio** | Base block-impact sounds (volume 1.0 to 2.0, pitch 1.0). Chainsaw loop and start sounds are gun sounds. |
| **Relationships** | Fired by `chainsaw` only. Its death type is GORE with chance 1.0, so killed mobs on the gore list produce gibs ([death-effects.md](death-effects.md)). |
| **Bugs** | none specific; shares the base bugs. |

### 4.4 `BlasterProjectile` (deprecated, unregistered)

| Field | Content |
|---|---|
| **Identity** | Class `BlasterProjectile extends GenericProjectile`, marked deprecated. **Not registered** (registration and renderer are commented out), so it has no ID and clients never see it. No lang entry. |
| **Obtaining** | Unobtainable: every `spawnEntityInWorld(new BlasterProjectile ...)` line in `Beamgun`, `Lasergun`, [`NeutrinoGun`](../02-guns/index.md#coverage), [`ScatterBeamGun`](../02-guns/index.md#coverage) and `Teslagun` is commented out. Dead code. |
| **Numbers** | Both constructors (world and gun) overwrite the `speed` field with `Lasergun.SPEED` (5.0) and both TTL fields with `Lasergun.LIFETIME` (5). As with `LaserProjectile`, the gun constructor has already built the motion from the gun's speed argument, so only the stored fields change (see PFX-35); range would be gun speed times 5. Gravity 0, size 0.25 x 0.25. |
| **Behavior** | `onImpact` replaces the base logic. An entity hit calls `attackEntityFrom` with a `BulletDamageSource` (name `bullet`, not the `TGDamageSource` pipeline) for the distance-scaled `getDamage()`: no damage factor, no armor model, no dummy knockback. A block hit spawns a vanilla `reddust` particle at the projectile (a once-planned `LaserImpact` particle is commented out). It then always dies. |
| **Persistence and sync** | Base NBT only. Not tracked. |
| **UI** | The kill message would be `death.attack.bullet`, which has no lang entry (PFX-31). |
| **Visuals** | The renderer class `RenderBlasterProjectile` exists but is unbound; it samples the top-left 16x5 px of [`textures/entity/Laser.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) (32x32, average #F85D26, dominant #F01010, #F07010, #D01010: a red-to-yellow gradient band). |
| **Audio** | none. |
| **Relationships** | None live. Superseded by `LaserProjectile` (plus `LaserBeam2`) and `BlasterRifleProjectile`. |
| **Bugs** | [PFX-35](index.md#suspected-bugs), [PFX-31](index.md#suspected-bugs). |

### 4.5 One-row sheet for every class

Columns: ID and registration name; speed; gravity per tick; TTL; collision; damage; explosion; effects and FX names; sync (extra spawn data); renderer and texture; sounds; fired by. "gun" means the value comes from the gun's stat block ([`02-guns`](../02-guns/index.md)). Size is 0.25 x 0.25 unless stated in the sections above.

| Class | ID / name | Speed | Gravity | TTL | Collision | Damage | Explosion | Effects and FX | Sync | Renderer / texture | Sounds | Fired by |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `GenericProjectile` | 1 / `GenericProjectile` | gun (1.5) | 0 | gun | ray + entity | gun, fall-off | none | none | none | `RenderGenericBullet` / `bullet.png` | bulletimpact, dig.* | bullet guns, NPCs, helicopter |
| `SimpleProjectile` | 2 / `SimpleProjectile` | 1.0 | 0.015 | 25 | as base | 8.0 | none | none | none | `RenderSimpleProjectile` / `handgunBullet.png` | base | `handcannon` |
| `FlamethrowerProjectile` | 3 / `FlamethrowerProjectile` | gun | gun | gun | dies in water | `tg_fire` | none | fire 3 s; legacy [`Flamethrower`](../02-guns/sheets-energy-special.md#flamethrower-flamethrower) particles; `lava` | none | `RenderDummy` | none | [`flamethrower`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) |
| `LaserProjectile` | 4 / `LaserProjectile` | gun (field 5.0) | 0 | 5 | movement-blocking blocks | `tg_energy` | none | spawns `LaserBeam2`; `LaserFlareStart`/`End` | none | `RenderDummy` | none | [`lasergun`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition), [`scatterBeamGun`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) |
| `LaserBeam` | 5 / `LaserBeam` | n/a | n/a | 5 | none (visual) | none | none | `LaserFlareStart`/`End` | target x, y, z | `RenderGenericLaser` / `laser3.png` | none | nothing (deprecated) |
| `RocketProjectile` | 6 / `RocketProjectile` | gun | 0.01 default | gun | solid block, entity | raw `damage`, knockback x3 | r 0.5 and 1.5 x radius | `RocketExplosion`, legacy `Fireblast` | none | `RenderRocketProjectile` / `guns/rocketlauncher.png` | `random.explode` | [`rocketlauncher`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition), [`grimreaper`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition), dispenser, helicopter (damage 20, speed 2.0) |
| `NukeRocketProjectile` | 7 / `NukeRocketProjectile` | gun | 0.01 default | gun | as rocket | raw, knockback x10 | as rocket | `NukeExplosion` (150 blocks) | none | `RenderRocketProjectile` / `guns/nukerocketlauncher.png` | nukeexplosion (8.0, 1.0) | [`nukelauncher`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition), dispenser |
| `BiogunProjectile` | 8 / `BiogunProjectile` | 1.0 | 0.01 | gun | dies in water | `tg_poison`, Poison IV 100 t | none | bio blobs; `slime` | `level` (byte) | `RenderBiogunProjectile` / `bioblob.png` | none | [`biogun`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) |
| `TeslaProjectile` | 9 / `TeslaProjectile` | 5.0 | 0 | 10 | any block | `tg_lightning`, chain | none | `TeslaBeam` bolts, `TeslaFlare` | none | `RenderDummy` | none | [`teslagun`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) |
| `TeslaBeam` | 10 / `TeslaBeam` | n/a | n/a | 10 | none (visual) | none | none | `TeslaFlare` | `beamlength`, `seed` | [`RenderTeslaBeam`](renderers.md#tesla-bolt-renderteslabeam) / `laser_blue.png` | none | `TeslaProjectile`, `teslagun` |
| `GrenadeProjectile` | 11 / `GrenadeProjectile` (range 120, freq 1) | hand 0.5 to 1.0, dispenser 1.0 | 0.015 / charge | 200 | bounces, dies in water | explosion | r 0.5 and 1.5 x radius | `RocketExplosion` | none | `RenderStielgranateProjectile` / `guns/stielgranate.png` | explosion1, irongolem.hit | stick grenade, dispenser |
| `MiningDummyProjectile` | 12 / `DummyMiningProjectile` | gun | 0 | gun | any block | direct health | none | breaks and drops blocks | none | `RenderGenericBullet` (inherited) | bulletimpact, dig.* | [`GenericGunTool`](../02-guns/index.md#coverage) (spawner unreachable) |
| `CyberdemonBlasterProjectile` | 13 / same | gun | gun | gun | dies in water | `tg_fire` | none | `NetherBlasterImpactFlare`, `MediumExplosionSmoke`, `CyberDemonBlasterTrail` | none | `RenderDummy` | none | [`cyberdemonBlaster`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) |
| `BlasterRifleProjectile` | 14 / same | gun | 0 | gun | as base | `tg_energy` | none | `smoke` only | none | `RenderBlasterRifleProjectile` / `laser3.png` | none | [`blasterRifle`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) |
| `PowerHammerProjectile` | 15 / same | gun | 0 | gun | any impact | `powerhammer` | none | legacy `impact` | none | `RenderPowerHammerProjectile` / `shockwave.png` | powerhammerImpact(Ground) | [`PowerHammer`](../02-guns/sheets-launchers-tools-grenades.md#power-hammer-powerhammer) item |
| `FlyingGibs` | 16 / same | from death motion | 0.03 | 150 to 250 | `moveEntity` | none | none | blood trail `blood4x4.png` | none | `RenderFlyingGibs` / `entity/gore.png` | `random.fizz` in lava | client death FX only |
| `EntityParticleSystem` | 17 / `ParticleSystem` (range 150) | n/a | n/a | per system | none | none | none | the FX lists | see particle-system.md | `RenderDummy` | none | client FX |
| `LaserBeam2` | 18 / same | n/a | n/a | 5 | none (visual) | none | none | `LaserFlareStart`/`End` | `beamlength` | `RenderLaserBeam2` / `laser3.png` | none | `LaserProjectile` |
| `GrenadeLauncherProjectile` | 19 / same (range 120, freq 1) | gun | param | gun | bounces (TTL -20 per bounce) | explosion | r 0.5 and 1.5 x radius | `GrenadeLauncherTrail`, `RocketExplosion` | none | `RenderGrenadeLauncherProjectile` / `launchergrenade.png` | explosion1 | [`grenadelauncher`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition), dispenser |
| `SonicShotgunProjectile` | 20 / same (freq 1) | gun | 0 | gun | pierces entities | `tg_sonic` | none | `SonicShotgunImpact`, `SonicShotgunTrail` | `mainProjectile` (boolean) | `RenderSonicShotgunProjectile` / `shockwave.png` | none | [`sonicShotgun`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) |
| `InvisibleProjectile` | 21 / same | gun | 0 | 2 | as base | 10.0, `chainsaw` | none | none | none | `RenderDummy` | base | `chainsaw` |
| `MovingBeam` | 22 / same | n/a (hitscan) | n/a | gun TTL | 48-block trace each tick | `tg_rad` every tick | none | `BeamGunMuzzleFX`, `BeamGunImpactFX` | shooter entity id (int) | `RenderMovingBeam` / `nukebeam.png` | none | [`nuclearDeathRay`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) |
| `BeamProjectile` | 23 / same | field 5 | 0 | 5 | any block | `tg_energy` | none | none | none | `RenderDummy` | none | nothing |
| `FragGrenadeProjectile` | 24 / same (range 120, freq 1) | as grenade | as grenade | 120 (dispenser) | bounces | explosion | r 0.5 and 1.5 x radius | `FragGrenadeExplosion` | none | `RenderStielgranateProjectile` / `guns/fragGrenade_texture.png` | explosion1 | [`FragGrenade`](../02-guns/per-gun-behavior.md#grenades-genericgrenade-fraggrenade-flashbang) item, dispenser |
| `NeutrinoBeam` | 25 / same | n/a | n/a | gun TTL | as MovingBeam | `tg_rad` | none | as MovingBeam | shooter id | `RenderMovingBeam` | none | `neutrinoGun` (commented out) |
| `EnergyBlastProjectile` | 26 / same | gun | 0 | gun | as base | `tg_energy`, GORE | none (commented out) | `BlueBlasterExplosion`, `BlueBlasterTrail` | none | `RenderDummy` | none | [`mibGun`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition), [`mibGunAkimbo`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) |
| `AlienBlasterProjectile` | not registered | gun | 0 | gun | as base | `tg_fire` | r 0.5, 0.25 and 0.75, no blocks | `AlienExplosion`, `AlienBlasterTrail` | not tracked | `RenderDummy` (bound, unused) | `random.explode` | `alienBlaster` |
| `RailgunProjectile` | not registered | field 5.0 | 0 | 50 | pierces while damage > 2 | `bullet` | none | 7 `AlienBlasterFlare` per tick | not tracked | `RenderRailgunProjectile` / `BlueBeam.png` (bound, unused) | none | nothing |
| `BlasterProjectile` | not registered | field 5.0 | 0 | 5 | dies on any hit | `bullet` | none | `reddust` | not tracked | none | none | nothing |
| `PiercingProjectile` | not registered | gun | 0 | base default | passes through blocks | as base | none | none | min/max size, sizeEnd, ttl | `RenderPiercingProjectile` (unbound) | none | nothing |
| `FlashBangProjectile` | not registered | 5.0 | grenade default | gun | as grenade | none | none | blindness and nausea area | none | none | none | `Flashbang` item (never registered) |

Sound durations for every event named in the Sounds column are in [sounds.md](sounds.md); texture palettes and pixel rectangles are in [textures.md](textures.md).
