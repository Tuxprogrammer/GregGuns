# 04: Projectiles and FX

Techguns 1.7.10 (v0.1.2 alpha). This area covers everything a gun puts into the world after the trigger is pulled:

- projectile and beam entities
- their renderers
- explosions
- the client particle system and its data files
- muzzle-flash and scope overlay sprites
- special death animations (gore, bio-dissolve, laser-disintegrate)
- the `textures/fx/` art and `sounds/effects/` audio

Gun stats and firing logic are in `02-guns`. Gun models and first-person rendering are in `03-gun-visuals-audio`. The damage pipeline (`DamageSystem`, `TGDamageSource`, armour penetration, friendly fire) is in `01-core`.

## Contents

| File | What it covers |
|---|---|
| [projectiles.md](projectiles.md) | Entity registration (IDs, names, tracking), the `GenericProjectile` base (spawn, flight, collision, fall-off, impact, NBT, sync), and a per-class reference for all 30 classes in `entities/projectiles/`, including grenade bouncing, rockets, tesla chaining, beams, bio blobs and flying gibs. |
| [renderers.md](renderers.md) | Every projectile/beam/gib renderer in `client/renderer/entity/` (geometry, UVs, animation formulas) and `ScreenEffect` muzzle flashes and scopes. |
| [explosions.md](explosions.md) | `ProjectileExplosion` (ray-cast, damage and knockback formulas, events, sound), all callers, `TGExplosionDamageSource`. |
| [particle-system.md](particle-system.md) | Data-driven particle engine: entry points, packets, full `FXList.txt`/`Nuke.txt` grammar and keys, emitter and particle maths, render types, interpolation quirks, the legacy `EntityParticleAnimated` sprite, and other `EntityFX` classes. |
| [fx-catalogue.md](fx-catalogue.md) | Every FX list and particle system decoded into tables (58 systems, 17 lists) with triggers, plus every legacy named particle. |
| [death-effects.md](death-effects.md) | Death types, the trigger chain (damage roll → death event → packet → client), `GoreData` per mob, gib models, blood, the BIO/LASER/DISMEMBER corpse renderers, config, death-message lang keys. |
| [textures.md](textures.md) | All 49 `textures/fx/*.png` (size, layout/frames, content, palette, blend, users) plus the projectile textures in `textures/entity/`. |
| [sounds.md](sounds.md) | All 26 `sounds/effects/*.ogg` with sounds.json events and exact triggers. |

## Scope notes

- Projectile entity registrations live in `java/techguns/Techguns.java` (lines 487–514), not in `TGEntities.java`. `TGEntities.java` was read in full and contains only NPC registrations and spawn setup (`10-entities-npcs`).
- The death-type trigger chain spans files outside the listed scope (`client/DeathEffect.java`, `client/GoreData.java`, `packets/PacketEntityDeathType.java`, `packets/PacketSpawnParticle*.java`, `events/TechgunsEventhandler.java`, the legacy particle switch in `client/ClientProxy.java`). They were read and their behaviour is documented here because it is the only way to specify the effects. Ownership of those files remains with `01-core`.
- NPC/mob renderers in `client/renderer/entity/` (`RenderAlienBug`, `RenderGenericNPC`, `RenderHelicopter`, `RenderSimpleTurret`, `RenderSuperMutant`) are left to `10-entities-npcs`.

## Quick facts

- 26 projectile/FX entities are registered (mod-local IDs 1–26). `AlienBlasterProjectile` is fired by a live gun but is **not** registered. `RailgunProjectile`, `BlasterProjectile`, `PiercingProjectile` and `FlashBangProjectile` are also unregistered, but nothing reachable spawns them.
- Projectiles fly in straight lines at `speed` blocks/tick with 0.99 drag (0.8 in water) and optional constant gravity. They ray-trace blocks on both sides and entities on the server only, and pass through non-solid blocks such as grass.
- Bullets ignore hurt-resistance. Knockback comes from a separate 0.01-damage "dummy" hit.
- Damage falls off linearly with *distance travelled* between `dmgDropStart` and `dmgDropEnd`.
- Explosions: flat damage inside r1 (0.5 × radius) falling to 0 at r2 (1.5 × radius). Blocks break per vanilla ray-casting when the shooter's safe mode is off. The shooter is immune to their own blast.
- All visuals are client-side and driven by FX packets. Named effects are defined in two text data files.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| PFX-01 | Alien blaster projectile not registered | `java/techguns/Techguns.java:487-514` (missing entry); spawned at `java/techguns/items/guns/AlienBlaster.java:47` | `AlienBlasterProjectile` (and `RailgunProjectile`) have bound renderers but no `registerModEntity`. The server simulates the shot, but clients never receive the entity, so `PacketSpawnParticleOnEntity("AlienBlasterTrail")` finds no entity and the trail never shows. It is also not saved. | gameplay |
| PFX-02 | Unknown FX name crashes the client; `AntiGravRingGreen` undefined | `java/techguns/client/particle/FXList.java:46-48`, `java/techguns/client/ClientProxy.java:887-903`, caller `java/techguns/items/armors/ItemAntiGravPack.java:62` | `createFX`/`createFXOnEntity` return null for unknown names and the proxy iterates the null list (NullPointerException). `AntiGravRingGreen` is not in any data file. | crash |
| PFX-03 | Beam trace miss uses the direction as an absolute point | `java/techguns/entities/projectiles/TeslaBeam.java:104-113`, `MovingBeam.java:232-243`, `LaserBeam.java:88-100` | When nothing is hit, the target is set to the direction/offset vector rather than origin + direction. The beam length becomes the distance to near world origin (huge or tiny), and the TeslaBeam/LaserBeam2 end-flare packet is sent to players near (0, 0, 0). | visual |
| PFX-04 | Proxy FX called from server code / off-thread | `java/techguns/packets/PacketSpawnParticle.java:83`, `java/techguns/packets/PacketSpawnParticleOnEntity.java:55`, `CyberdemonBlasterProjectile.java:58,126`, `FlamethrowerProjectile.java:54`, `MovingBeam.java:246`, `PowerHammerProjectile.java:103`, `RailgunProjectile.java:72` | In single-player the integrated server uses `ClientProxy`, so server-side FX calls spawn particle-system entities into the server world or push `EntityFX` from the server thread. Packet handlers spawn client entities from the Netty thread. Risk of ConcurrentModificationException and duplicated FX. | crash |
| PFX-05 | Cyberdemon blaster entity-hit FX never shown on a dedicated server | `java/techguns/entities/projectiles/CyberdemonBlasterProjectile.java:125-127` | `onHitEffect` runs only server-side and calls `proxy.createFX`, which is a no-op there. Block hits show FX because they also run client-side. | visual |
| PFX-06 | Explosion player knockback never synced | `java/techguns/util/ProjectileExplosion.java:243-246` | A per-player knockback map is built but no explosion packet is sent and the map is never read, so remote players are not pushed by Techguns explosions. | gameplay |
| PFX-07 | Tesla chain damage ignores the friendly-fire factor | `java/techguns/entities/projectiles/TeslaProjectile.java:172` | `getDamage() - chainDmgDrop*(i+1)*dmg`: operator precedence applies the factor only to the drop, so allies and teammates take full chain damage. The value can also go negative. | gameplay |
| PFX-08 | Tesla always chains at least once; no checks | `java/techguns/entities/projectiles/TeslaProjectile.java:215-232` | With `chain = 0` the counter goes to −1 after the first add, so one target is still chained. There are no alive, line-of-sight or team checks, and dead entities can be chained. | gameplay |
| PFX-09 | Tesla chain-beam pitch sign slip | `java/techguns/entities/projectiles/TeslaBeam.java:159` | `dY = y - target.posY + height/2` should subtract the half height. Chain bolts aim at the wrong height. | visual |
| PFX-10 | Bounced hand and frag grenades lose block damage | `java/techguns/entities/projectiles/GrenadeProjectile.java:215-217`, `FragGrenadeProjectile.java:30-32` | `createBounceProjectile` hard-codes `blockdamage = false` (the launcher version keeps it). Whether a grenade breaks blocks depends on whether it bounced first. | gameplay |
| PFX-11 | Tesla renderer mutates a shared field; possible divide by zero | `java/techguns/client/renderer/entity/RenderTeslaBeam.java:61-63` | The `offset` field is overwritten each render (shared by all bolts), so the spacing drifts. When `round(distance/offset) == 0` it becomes Infinity/NaN and all later tesla bolts break for the session. | visual |
| PFX-12 | Bio projectile speed applied twice | `java/techguns/entities/projectiles/BiogunProjectile.java:26,35-37` | Motion is multiplied by `speed` in super and again in the subclass, and a shadowing `speed = 0.75` field exists. Latent (the gun passes 1.0). | cosmetic |
| PFX-13 | Mining projectile gore roll inverted | `java/techguns/entities/projectiles/MiningDummyProjectile.java:57-59` | It sets DEFAULT death when `rand < goreChance`; the damage system does the opposite. | cosmetic |
| PFX-14 | Mining projectile breaks blocks with no checks | `java/techguns/entities/projectiles/MiningDummyProjectile.java:76-118` | Drops and removes any non-unbreakable block (even passable ones) without a `BreakEvent`, harvest-level check or safe-mode check. It bypasses claims and protection mods. Currently unreachable (`driller` is commented out). | gameplay |
| PFX-15 | Explosions run in full on the client | `java/techguns/util/ProjectileExplosion.java:91-94` | Callers that run on both sides (grenades, alien blaster) execute block ray-casting and entity damage/knockback client-side without Forge events. The client-side knockback causes motion desync. | visual |
| PFX-16 | Death-ray third-person origin mismatch | `java/techguns/client/renderer/entity/RenderMovingBeam.java:86` vs `MovingBeam.java:174` | The renderer raises the beam by +0.1 while the entity's hit trace lowers it by 0.1. The visual beam and the damage ray diverge. | visual |
| PFX-17 | Particle ground features dead | `java/techguns/client/particle/EntityParticle.java:142,149-155` | Particles move via `setPosition` with `noClip`, so `onGround` is never true. `RemoveOnGround` and `VelocityDampingOnGround` in the data files never take effect. | visual |
| PFX-18 | Players (and mob subclasses) never get gore or dismember deaths | `java/techguns/util/EntityDeathUtils.java:97` | The gore list is matched by exact class. The runtime player class is `EntityPlayerMP`, not `EntityPlayer`. | visual |
| PFX-19 | Particle UV ignores rows | `java/techguns/client/particle/EntityParticle.java:253-254` | The row height uses `1/columns`. Sheets with rows ≠ columns would sample wrongly (latent; `laserflare02` works only because its art is 4 × 4). | visual |
| PFX-20 | Particle alpha/colour curves reversed and NaN-prone | `java/techguns/client/particle/EntityParticle.java:186-214`, `ParticleSystemType.java:103` | Segment weights are swapped and alpha is stored as 1 − value. Curves only match the authored keys when α₁ + α₂ = 1. A single key or a progress outside the keys gives 0/0 = NaN colour or alpha. | visual |
| PFX-21 | `EXTENDS` copies wrongly and shares state | `java/techguns/client/particle/ParticleSystemType.java:379-417` | `initialDelayMax` is taken from the parent's min. Particle-count min, system lifetime, start-size rate and system-velocity factor are not copied. Alpha/colour lists and velocity/volume arrays are shared by reference, so child edits change the parent. | visual |
| PFX-22 | Flashbang unreachable and incomplete | `java/techguns/TGuns.java:163`, `java/techguns/entities/projectiles/FlashBangProjectile.java:37-101` | The item is constructed but never registered, and the projectile has no entity registration. The effect has no sound or FX and also blinds the thrower. | gameplay |
| PFX-23 | Hitscan prefers entities over nearer blocks | `java/techguns/util/MathUtil.java:272-276` (used by `MovingBeam`, `TeslaBeam`, `LaserBeam2`) | Within each trace segment (12 or 10 blocks) an entity intersection replaces the block hit even when the block is closer. The death ray damages entities behind walls. | gameplay |
| PFX-24 | Grenades silently vanish in water | `java/techguns/entities/projectiles/GrenadeProjectile.java:236-243` | Any grenade entering water is removed without exploding (rockets keep flying underwater). | gameplay |
| PFX-25 | Shooter immune to own explosions | `java/techguns/util/ProjectileExplosion.java:182` | The exploder (the shooter) is excluded from the entity list, so there is no rocket-jumping or self-damage. Possibly intentional, but asymmetric with vanilla. | gameplay |
| PFX-26 | Rocket direct hit ignores friendly fire and fall-off | `java/techguns/entities/projectiles/RocketProjectile.java:71` | Raw `damage` is applied without `DamageSystem.getDamageFactor`, unlike every other projectile. | gameplay |
| PFX-27 | Some projectiles stop on grass and flowers | `java/techguns/entities/projectiles/PowerHammerProjectile.java:96-100`, `TeslaProjectile.java:190`, `BeamProjectile.java:84` | No `getBlocksMovement` check, so passable blocks end the shot (the base class lets bullets pass through). | gameplay |
| PFX-28 | Bio gun places blobs in safe mode | `java/techguns/entities/projectiles/BiogunProjectile.java:76-141` | Blob placement and growth ignore the `blockdamage` (safe-mode) flag. Only the Forge place event (players only) can stop it. | gameplay |
| PFX-29 | Particle system without `VolumeType` crashes | `java/techguns/client/particle/ParticleSystemType.java:69`, `EntityParticleSystem.java:114` | The volume type defaults to null and is dereferenced on spawn (NullPointerException). The data-file grammar does not require the key. | crash |
| PFX-30 | Debug stdout spam | `SimpleProjectile.java:66,81`, `client/particle/EntityLaserBeamFX.java:30,57-64`, `client/renderer/death/RendererDeadLivingEntity.java:453`, `TeslaProjectile.java:257`, `FXList.java:315` | Prints on every NBT save/load, every frame (dismember and laser FX), and per parsed system. | cosmetic |
| PFX-31 | Missing death-message keys | `java/techguns/damagesystem/TGDamageSource.java:68`, `java/techguns/util/BulletDamageSource.java:60`, `resources/assets/techguns/lang/en_US.lang` | No `death.attack.tg_knockback` (the dummy hit can deliver the killing blow) or `death.attack.bullet`. | cosmetic |
| PFX-32 | ScreenEffect references missing textures | `java/techguns/client/renderer/FX/ScreenEffect.java:21,33,34` | `muzzleFlashYellow2x2.png`, `muzzleflashnew.png` and `muzzleflashnew_2_add.png` do not exist (the instances are unused). | cosmetic |
| PFX-33 | FX parse error message prints a literal | `java/techguns/client/particle/FXList.java:326` | Prints the word `"error"` instead of the error variable. A bad line also aborts the rest of the file. | cosmetic |
| PFX-34 | SimpleProjectile texture/scale never used | `java/techguns/entities/projectiles/SimpleProjectile.java:34-41`, `client/renderer/entity/RenderSimpleProjectile.java:36-69` | The per-shot texture and scale are stored and saved but never synced, and the renderer uses fixed values. | cosmetic |
| PFX-35 | Laser/Beam/Railgun override the gun TTL | `java/techguns/entities/projectiles/LaserProjectile.java:29-31`, `BeamProjectile.java:29-31`, `RailgunProjectile.java:25-27` | After the motion has been computed, `speed` (field only) and `ticksToLive` are overwritten with class constants. The lasergun range is fixed at gunSpeed × 5 regardless of the configured TTL. | gameplay |
| PFX-36 | Death corpse renderer drops armour and items, and snaps rotation | `java/techguns/client/renderer/death/RendererDeadLivingEntity.java:252,327,397` | Uses the raw `rotationYaw` (not interpolated `renderYawOffset`), and the equipment and armour passes are never called. Corpses lose armour and held items and may face the wrong way. | visual |
| PFX-37 | TRAIL volume without an entity returns an absolute position | `java/techguns/client/particle/ParticleSystemType.java:350-351` | Returns the system's world position as a local offset, which doubles the coordinates (latent: trails are only spawned attached). | visual |
| PFX-38 | Death-type map never cleaned for unseen entities | `java/techguns/client/ClientProxy.java:1106-1121`, `events/TechgunsEventhandler.java:226-228` | Entries are removed only when the entity is rendered with `deathTime == 0`. Entities that die out of view leak entries, and a player-name key could suppress the render of a later entity. | cosmetic |
