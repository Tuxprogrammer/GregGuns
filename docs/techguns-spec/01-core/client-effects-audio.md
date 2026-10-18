# Client effects, audio plumbing, death effects, version checker

Source: [`client/ClientProxy.java`](lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy), [`client/AttackTime.java`](networking.md#1--gunfiredmessage), `client/DeathEffect.java`, `client/GoreData.java`, `client/TGVersionChecker.java`, `client/audio/*`, `util/SoundUtil.java`.

## TGSound

`client.audio.TGSound` extends vanilla `MovingSound`. Every Techguns sound that goes through the proxy or a packet is played as a `TGSound`.

**Constructor variants:**
- `(name, entity, volume, pitch, repeat, moving, gunPosition, category)`: an entity-attached sound.
- `(name, x, y, z, volume, pitch, repeat, category)`: a fixed-position sound (the position is the block coordinates passed in).

**Fields:**
- The sound name is used as a `ResourceLocation`, so names carry their domain (`techguns:…`).
- Volume goes through [`TGConfig.getVolumeMultiplier`](config.md), which is a no-op.
- `repeat` loops the sound, with a repeat delay of 0 unless `setRepeatDelay` is called.
- The initial position is the entity's position.

**`gunPosition`:** the sound position is shifted 1 block horizontally, by (cos(yawHead), sin(yawHead)), and raised by half the entity's height. The comments say "in front of the entity", but that vector points to the entity's **left** ([CORE-21](index.md#suspected-bugs)).

**Update (every client tick):** if `moving` is set and an entity is attached, the position follows the entity, and the gunPosition shift is re-applied when that flag is set.

**API:**
- `setDonePlaying()` turns off repeat, marks the sound done and zeroes the delay. Loops are stopped this way.
- `setPitch` and `setRepeatDelay` are also available.

**Auto-removal:** there is none. A looping sound stops only when its owner calls `setDonePlaying`.

## TGSoundCategory

| Ordinal | Name | Id | Typical use |
|---|---|---|---|
| 0 | GUN_FIRE | `gunfire` | gun shots |
| 1 | PLAYER_EFFECT | `playereffect` | jetpack, glider, anti-grav |
| 2 | RELOAD | `reload` | reloads |
| 3 | EXPLOSION | `explosion` | explosions |
| 4 | MACHINE | `machine` | machines |
| 5 | DEATHEFFECT | `deathFX` | death sounds |

The category is sent over the network as its ordinal, but it doesn't affect playback. Sounds use the category declared in `sounds.json` (player or hostile), so per-category volume isn't implemented.

## Sound routing (`SoundUtil`)

All helpers branch on `world.isRemote`. On the **server** they send [`PacketPlaySound`](networking.md#9--packetplaysound-sc) to all players within **100 blocks** of the entity, in the entity's dimension. On the **client** they play the sound immediately through the proxy, as a local prediction.

| Helper | Server packet flags | Client local play |
|---|---|---|
| `playSoundOnEntityGunPosition(world, ent, name, vol, pitch, repeat, moving, cat)` | moving as given, gunPosition = true | same, with gunPosition |
| `playReloadSoundOnEntity(…)` | as above | plays only if more than **500 ms** have passed since the last local reload sound (`ClientProxy.lastReloadsoundPlayed`), so overlapping reload calls don't double up |
| `playSoundOnEntity(world, ent, name, vol, pitch, repeat, cat)` | moving = true, no gunPosition | moving |
| `playSoundAtEntityPos(world, ent, name, vol, pitch, repeat, cat)` | moving = false (fixed at the entity's current position) | not moving |

On receipt, the client skips sounds whose entity is the local player unless `playOnOwnPlayer` was set, because the local player already heard its own prediction.

## Sounds played by core code

Event names come from `sounds.json`. Paths are relative to `assets/techguns`.

| Event | Files | sounds.json category | When it plays |
|---|---|---|---|
| `techguns:nightvision.on` | [`sounds/items/nightvision_on.ogg`](../06-items/sounds.md) | player | NV toggle switched **on** while the armor provides NV. Server world sound at the player |
| `techguns:nightvision.off` | [`sounds/items/nightvision_off.ogg`](../06-items/sounds.md) | player | NV toggle switched **off** while a Night Vision effect is active (the effect is removed) |
| `techguns:effects.gore` | [`sounds/effects/gore_a.ogg`](../04-projectiles-fx/sounds.md), `gore_b.ogg`, `gore_c.ogg` (random) | hostile | Client, on a GORE death packet (if gore is enabled), at the dying entity |
| `techguns:effects.biodeath` | [`sounds/effects/biodeath.ogg`](../04-projectiles-fx/sounds.md) | hostile | Client, on a BIO death packet |
| `techguns:effects.disintegrate` | [`sounds/effects/disintegrate.ogg`](../04-projectiles-fx/sounds.md) | hostile | Client, on a LASER death packet |
| `techguns:jetpack.start` | [`sounds/items/jetpack_start_a.ogg`](../06-items/sounds.md), `_b`, `_c` | player | Client, when a player starts boosting (a non-moving sound at the player) |
| `techguns:jetpack.loop` | [`sounds/items/jetpack_loop_a.ogg`](../06-items/sounds.md), `_b`, `_c` | player | Client, moving and repeating while the player boosts |
| `techguns:jetpack.stop` | [`sounds/items/jetpack_stop_a.ogg`](../06-items/sounds.md), `_b`, `_c` | player | Client, when boosting stops |
| `techguns:glider.loop` | [`sounds/items/glider_loop_stereo.ogg`](../06-items/sounds.md) | player | Client, moving and repeating while `isGliding`. The file is **stereo**, so OpenAL won't position it in 3D ([CORE-31](index.md#suspected-bugs)). [`sounds/items/glider_loop.ogg`](../06-items/sounds.md) (mono) exists but isn't referenced by this event |
| `techguns:items.antiGravStart` | [`sounds/items/antigrav_start_a.ogg`](../06-items/sounds.md), `_b` | player | Server → all within 50 blocks (including the flyer), on the tick an anti-grav flyer starts flying |
| `random.eat` (vanilla) | — | — | Server, each time the auto-feeder eats an item |
| `gui.button.press` (vanilla) | — | — | Clicking an enabled inventory tab button |

## Attack timing (recoil / reload)

`ClientProxy.attackTimes` maps a key to an `AttackTime`. The key is the player name for players and the entity ID (as a string) for anything else.

| Field | Meaning |
|---|---|
| reloadTime (long ms) | wall-clock time when the current reload ends |
| reloadTimeTotal (int ms) | duration of that reload |
| recoilTime (long ms) | wall-clock time when the current recoil animation ends |
| recoilTimeTotal (int ms) | its duration |
| attackType (byte) | which hand or attack mode the last action used (0/1, used for akimbo) |

Gun and NPC renderers ([03](../03-gun-visuals-audio/index.md), [10](../10-entities-npcs/index.md)) read these to animate recoil and reload. Progress = (end − now) / total.

**Writers:**
- [`GunFiredMessage`](networking.md#1--gunfiredmessage-sc) (recoil).
- [`ReloadStartedMessage`](networking.md#3--reloadstartedmessage-sc) (reload; for other entities only).
- The gun item itself, for the local player ([02](../02-guns/index.md)).

**`isStillRecoiling(entity, type)`** is true only when all of these hold:
- the entity is the local player;
- the camera is in first person;
- a record exists with the same attack type;
- its recoil end time is still in the future.

`GunFiredMessage` uses it so a shot echoing back from the server doesn't restart the local player's own recoil animation.

**Cleanup:** entries are removed on client chunk unload (every living entity in the chunk) and on a client-side death of a [`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) or [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret). The logout and stop-tracking cleanups are dead ([CORE-10](index.md#suspected-bugs)).

## Muzzle flash placement (`ClientProxy.spawnMuzzleFXForEntity(shooter, leftGun)`)

1. If the shooter is the local player in first person, do nothing. The first-person gun renderer draws its own flash ([03](../03-gun-visuals-audio/index.md)).
2. The held item must be a [`GenericGun`](../02-guns/gun-lifecycle.md) with a non-null muzzle-flash particle name ([02](../02-guns/index.md)/[03](../03-gun-visuals-audio/index.md)).
3. Build an offset (X right, Y up, Z forward) relative to the shooter's feet:
   - player seen by others: Y += 1.6 (the local player in third person gets Y += 0);
   - players and `GenericNPC`s: X −0.2, Y +0.05, Z +1.15;
   - [`INPCTechgunsShooter`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) NPCs add their weapon position offsets, and turrets also add the gun's turret position offset plus its turret muzzle-flash offset.
4. If this is the left gun of an akimbo pair, mirror X.
5. Add the gun's own muzzle-flash offsets (X, Y, Z).
6. Rotate the offset:
   - players, turrets and GenericNPCs: pitch the (Z, Y) pair by −pitch, then yaw by −yawHead;
   - other entities: yaw by −renderYawOffset only.
7. Spawn the legacy named particle (below) at the result, with motion (shooter.motionX, 0, shooter.motionZ).

## Legacy named particles (`ClientProxy.spawnParticle`, deprecated but live)

This path spawns single animated sprites. It is still used for muzzle flashes (the gun's particle name) and by older projectile code. The newer FXList particle-system path (`createFX`) is described in 04.

**Columns:**
- *Size*: the starting scale.
- *Grid*: the sprite-sheet layout, columns × rows : frame count.
- *Blend*: ADDITIVE, ALPHA or ALPHA_SHADED.
- *Age*: max age in ticks.

**Shared extras:** every "scale to" entry also has the `true` flag (grow over life). "Random rotation" means a random roll per particle.

Textures are under `textures/fx/` and owned by [04-projectiles-fx](../04-projectiles-fx/index.md).

| Name | Texture | Size | Grid | Blend | Age | Other settings |
|---|---|---|---|---|---|---|
| BioTrail | vanilla slime-ball breaking particle | — | — | — | — | — |
| LaserImpact | vanilla redstone breaking particle | — | — | — | — | — |
| RailgunTrail | trail particle using the diamond icon | — | — | — | — | params 0.1, 0.5, 1.0, 0.5 |
| LaserBeam | [`EntityLaserBeamFX`](../04-projectiles-fx/fx-catalog.md#legacy-named-particles-clientproxyspawnparticle); the "motion" arguments are the beam's end point | — | — | — | — | — |
| Fireball | fireball_4x4_add.png | 1.5 | 4×4:16 | ADD | default | gravity 0.025, random rotation |
| FireballImpact | fireball_4x4_add.png | 3.5 | 4×4:16 | ADD | 20 | gravity 0.025, random rotation, scale 3.5→6 |
| Explosion | Explosion.png | 5 | 5×5:20 | ALPHA | 20 | gravity 0.05, scale 5→10, random rotation |
| ExplosionHuge | Explosion.png | 5 | 5×5:20 | ALPHA | 40 | gravity 0.05, scale 20→40, random rotation |
| Fireblast | fireblast.png | 2.5 | 5×5:25 | ALPHA | 20 | scale 2.5→5, random rotation, randomness 0.33, damping 0.90 |
| BlasterFlare | blasterflare01.png | 2.5 | 4×4:14 | ADD | 16 | — |
| AlienBlasterFlare | alienflare.png | 3.0 | 4×4:14 | ADD | 5 | — |
| LargeSmoke | largesmoke.png | 7.5 | 2×2:4 | ALPHA_SHADED | 40 | delay 7, scale 7.5→12.5, random rotation, randomness 0.5, damping 0.98 |
| LaserFlareEnd | laserflare02.png | 5.0 | 4×4:7 | ADD | 18 | scale 5→7.5 |
| LaserFlareStart | laserflare02.png | 2.5 | 4×4:7 | ADD | 12 | — |
| Flamethrower | flamethrower_a.png | 0.25 | 4×4:16 | ALPHA | 20 | scale 0.25→2.5 (rate 0.5), random rotation, randomness 0.75, damping 0.98 |
| Jetpack | flamethrower_a.png | 0.125 | 4×4:16 | ALPHA | 10 | scale 0.125→1.5 (rate 0.5), random rotation, randomness 0.75, damping 0.98 |
| MuzzleFlashYellow | muzzleflash2.png | 1.5 | 4×4:8 | ADD | 4 | — |
| MuzzleFlashExp | muzzleflash4x4.png | 1.5 | 4×4:10 | ADD | 4 | — |
| MuzzleFlashBlue | muzzleflash_blue.png | 1.5 | 4×4:8 | ADD | 4 | — |
| MuzzleFlashLaser | laserflare02.png | 1.5 | 4×4:7 | ADD | 4 | — |
| MuzzleFlashAlienBlue | alienflare.png | 1.5 | 4×4:14 | ADD | 4 | color fixed at RGBA (40, 255, 140, 255), FAST mode |
| Blood | blood01.png | 5 | 1×1:1 | ALPHA | 20 | gravity 0.07, scale 5→10, alpha fades 255→0 linearly from 50 % of life, random rotation, randomness 0.5, damping 0.98 |
| impact | impact.png | 2.5 | 2×2:4 | ADD | 5 | scale 2.5→7.5, color (255, 215, 155) with alpha fading 128→0 linearly |
| DustClouds | dustclouds.png | 7.5 | 4×4:16 | ALPHA_SHADED | 40 | scale 5→10, random rotation, randomness 0.5, damping 0.98, alpha 255→0 sinusoidal |
| TeslaFlare | teslaflare01.png | 5.0 | 4×4:14 | ADD | 15 | scale 5→10, alpha fades 255→0 linearly from 75 % of life |

Unknown names spawn nothing.

## Death effects

### Flow

1. Server: a living entity dies from a [`TGDamageSource`](../02-guns/damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource) whose death type is not DEFAULT and has a special animation. [`PacketEntityDeathType`](networking.md#6--packetentitydeathtype-sc) is sent to everyone within 100 blocks.
2. Client: the packet handler checks the two config flags, copies the death motion to the entity, records the death type in `ClientProxy.entityDeathTypes`, calls `DeathEffect.createDeathEffect`, and plays the death sound (table above).
3. While the type is recorded, `RenderLivingEvent.Pre` hides or replaces the corpse:
   - GORE: the body is not rendered.
   - BIO, LASER, DISMEMBER: the body is drawn with [`RendererDeadLivingEntity`](../04-projectiles-fx/death-effects.md#6-dismember-unused) ([04](../04-projectiles-fx/index.md)).
4. The record is cleared when the entity's `deathTime` reads 0.

### `DeathEffect.createDeathEffect(entity, type, mx, my, mz)`

The origin is the entity's center: (posX, posY + height/2, posZ).

- **GORE:**
  1. Look up the entity's `GoreData` by exact class. Unknown classes fall back to the generic gore entry.
  2. Spawn one [`EntityBloodEffect`](../04-projectiles-fx/death-effects.md#blood-burst-entitybloodeffect) particle ([04](../04-projectiles-fx/index.md)), sized by the entity's width and height and tinted with the gore data's blood color.
  3. Spawn `numGibs` [`FlyingGibs`](../04-projectiles-fx/projectiles.md#319-flyinggibs) entities ([04](../04-projectiles-fx/index.md)). Each gets a velocity of (mx·0.35 + rx, my·0.15 + ry, mz·0.35 + rz), where:
     - rx and rz are uniform in ±0.175;
     - ry is uniform in [0, 0.35) when the entity is on the ground, otherwise ±0.175.

     The gib size is (width + height)/2, and each gib gets its index i.
- **BIO:** `createFX("biodeath")` at the origin, with the death motion.
- **LASER:** `createFX("laserdeathFire")` and `createFX("laserdeathAsh")`, using the death motion with Y forced to 0.
- **DEFAULT, DISMEMBER:** nothing here.

The FX names refer to FXList entries ([04](../04-projectiles-fx/index.md)).

### Gore table (`DeathEffect.goreStats`)

`GoreData` holds:
- gib model;
- gib count;
- texture;
- scale;
- blood color (R, G, B);
- `showBlood` (default true);
- per-gib random scale range (default 1–1).

Models: "biped" = `ModelGibsBiped(1.0)`; the player uses a biped built for a 64×32 texture; "generic(X)" = `ModelGibsGeneric` wrapping vanilla model X.

| Entity | Model | Gibs | Texture | Scale | Blood RGB |
|---|---|---|---|---|---|
| EntityPlayer | biped (64×32) | 6 | vanilla `textures/entity/steve.png` | 0.66 | 160, 21, 31 |
| EntityZombie | biped | 6 | vanilla zombie | 0.66 | 110, 21, 41 |
| ZombieSoldier, ZombieFarmer, ZombieMiner | biped | 6 | [`textures/entity/zombie_soldier.png`](../10-entities-npcs/textures.md#texturesentityzombie_soldierpng-128128) | 0.66 | 110, 21, 41 |
| ArmySoldier | biped | 6 | [`textures/entity/army_soldier.png`](../10-entities-npcs/textures.md#texturesentityarmy_soldierpng-128128) | 0.66 | 160, 21, 31 |
| Bandit | biped | 6 | [`textures/entity/bandit.png`](../10-entities-npcs/textures.md#texturesentitybanditpng-6464) | 0.66 | 160, 21, 31 |
| EntitySkeleton, SkeletonSoldier | `ModelGibsSkeleton` | 6 | vanilla skeleton | 0.66 | 255, 255, 255, no blood |
| EntityVillager | villager gib model | 5 | vanilla villager | 0.66 | 150, 21, 51 |
| EntityWitch | villager gib model (witch) | 5 | vanilla witch | 0.66 | 160, 21, 31 |
| EntityCow / EntitySheep / EntityChicken | generic(cow / sheep / chicken) | 6 | vanilla textures | 1.0 | 170, 26, 37 |
| EntityPig | generic(pig) | 6 | vanilla pig | 0.8 | 170, 26, 37 |
| EntityCreeper | generic(creeper) | 7 | vanilla creeper | 1.0 | 50, 175, 57 |
| EntityEnderman | generic(enderman) | 7 | vanilla enderman | 1.0 | 160, 36, 167 |
| EntitySpider | generic(spider) | 11 | vanilla spider | 1.0 | 85, 156, 17 |
| EntityCaveSpider | generic(spider) | 11 | vanilla cave spider | 0.66 | 85, 156, 17 |
| EntityPigZombie, ZombiePigmanSoldier | biped | 6 | vanilla zombie pigman | 0.66 | 110, 51, 11 |
| CyberDemon | `ModelGibsBipedGeneric(ModelCyberDemon)` | 6 | [`textures/entity/cyberdemon.png`](../10-entities-npcs/textures.md#texturesentitycyberdemonpng-128128-fully-opaque-on-a-black-background) | 1.0 | 85, 156, 17 |
| EntitySlime | none | 0 | none | 1.0 | 40, 255, 40 (blood particle only) |
| AlienBug | [`ModelGibsAlienBug`](../04-projectiles-fx/death-effects.md#gib-models-clientmodelsmodelgibsjava) | 8 | [`textures/entity/alienbug.png`](../10-entities-npcs/textures.md#texturesentityalienbugpng-6464-about-34-opaque) | 0.8 | 235, 255, 70 |
| *anything else (generic)* | biped | 6 | [`textures/entity/gore.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | 0.66 | 160, 21, 31; gib scale random 0.5–0.8 |

Which entities get gore at all is decided server-side by [`EntityDeathUtils`](../04-projectiles-fx/index.md#coverage) ([utilities.md](utilities.md#entitydeathutils)). Some entities in that list, such as horses, wolves, squid, ghasts, PsychoSteve and DictatorDave, have no gore entry and therefore use the generic `gore.png` gibs.

## Version checker

`TGVersionChecker` runs once on a background thread at client postInit, if [`EnableVersionChecker`](config.md#category-clientside) is on.

1. Download `https://raw.githubusercontent.com/pWn3d1337/Techguns/master/version.txt`. Line 1 is `recommended` and line 2 is `latest`.
2. Decide what to show:

   | Condition | Result |
   |---|---|
   | VERSION == latest | up to date, `warned` = true (log "Techguns is up to date.") |
   | VERSION == recommended | `warn` = 2 (beta notice) if the beta check is on; otherwise up to date |
   | Neither | `warn` = 2 if the beta check is on, otherwise `warn` = 1 (stable notice) |

3. Network errors are logged ("Could not check Techguns version: …") and `warn` stays 0.

The **chat message** is posted on the first client player tick after `warn` becomes non-zero, and only once:
- Line 1: `§a[Techguns](<VERSION>): §f` followed by either `techguns.updateCheck.newVersion.stable` ("New stable version is available!") + `: <recommended>`, or `techguns.updateCheck.newVersion.beta` ("New beta version is available!") + `: <latest>`.
- Line 2: `§7>` + `techguns.updateCheck.click` ("Click to download"). Clicking it opens `https://minecraft.curseforge.com/projects/techguns/files`, and its hover text is `techguns.updateCheck.download` ("Click to open the download page").

When the checker is disabled the field is null, and the tick handler dereferences it unconditionally, which crashes the client ([CORE-02](index.md#suspected-bugs)).

## Measured audio parameters (consistency pass)

The sound list above ("Sounds played by core code") gives events and triggers. For each file's duration, sample rate and channels, and for the exact volume, pitch, repeat and moving flags every call passes, see [textures-sounds-lang.md](textures-sounds-lang.md#measured-sound-facts-and-play-parameters-consistency-pass). Summary of the parameters in code:

| Call path | Volume | Pitch | Repeat | Moving | Category label |
|---|---|---|---|---|---|
| Death sounds (`PacketEntityDeathType` handler) | 1.0 | 1.0 | no | no | DEATHEFFECT |
| Jetpack start and stop ([`TechgunsExtendedPlayerPropertiesClient`](player-state.md#client-subclass-techgunsextendedplayerpropertiesclient)) | 1.0 | 1.0 | no | no | PLAYER_EFFECT |
| Jetpack loop and glider loop (`TGSound`) | 1.0 | 1.0 | yes | yes | PLAYER_EFFECT |
| Anti-grav start (`PacketPlaySound` from the tick handler, radius 50) | 1.0 | 1.0 | no | yes | PLAYER_EFFECT, `playOnOwnPlayer` true |
| Night vision on and off (`world.playSoundAtEntity`) | 1.0 | 1.0 | no | no | none (vanilla world sound) |

All timing figures in this table are code constants; nothing here was timed at run time.

### Death-effect and gore parameters not stated above

- The origin of the blood and gib effect is the entity center. The gib velocity formula uses 0.35 for horizontal and 0.15 for vertical motion inheritance, with random spread of plus or minus 0.175; this is unchanged from the section above.
- `GoreData` default values: `showBlood` true, gib scale random range 1 to 1. The generic entry overrides the range to 0.5 to 0.8, as in the gore table.
- The death sound is played after the effect is created, in the same handler call, so sound and gibs start on the same client tick.
