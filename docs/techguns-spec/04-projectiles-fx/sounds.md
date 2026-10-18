# Sounds (`sounds/effects/`)

All 26 files in `sounds/effects/` are covered below. Event names are keys in `sounds.json` and are played as `techguns:<event>`. An event with several files picks one at random each time it plays. Files not under `effects/` that are triggered by projectiles (for example `techguns:guns.powerhammerImpact`) belong to [`03-gun-visuals-audio`](../03-gun-visuals-audio/index.md). They are listed at the end for completeness.

| File | `sounds.json` event | sounds.json category | When it plays |
|---|---|---|---|
| `sounds/effects/bullet_impact_a.ogg`, `sounds/effects/bullet_impact_b.ogg`, `sounds/effects/bullet_impact_c.ogg`, `sounds/effects/bullet_impact_d.ogg`, `sounds/effects/bullet_impact_e.ogg` (all 10083 B, possibly identical copies) | `effects.bulletimpact` | player | A projectile hits a block whose material is not rock, wood, glass, iron or anvil ([`GenericProjectile.spawnImpactParticle`](projectiles.md#2-genericprojectile-the-base-for-almost-everything), [`MiningDummyProjectile`](projectiles.md#314-miningdummyprojectile)). Volume 1.0, pitch 1.0, via `playSoundAtEntity` at the projectile. Applies to every projectile that keeps the default `hitBlock`/`spawnImpactParticle`: `GenericProjectile`, [`SimpleProjectile`](projectiles.md#31-simpleprojectile), [`InvisibleProjectile`](projectiles.md#312-invisibleprojectile-chainsaw) (chainsaw) and `MiningDummyProjectile`. Not sonic, blaster rifle or others that override it. A commented-out headshot sound also used this event. |
| `sounds/effects/explosion1_a.ogg`, `sounds/effects/explosion1_b.ogg`, `sounds/effects/explosion1_c.ogg`, `sounds/effects/explosion1_d.ogg`, `sounds/effects/explosion1_e.ogg` | `effects.explosion1` | master | Grenade explosions (stick, frag and 40 mm grenade) through [`ProjectileExplosion.setExplosionSound`](explosions.md#projectileexplosion). Volume 4.0, pitch `(1 + (rand − rand) × 0.2) × 0.7`. Rockets use vanilla `random.explode` instead. |
| `sounds/effects/nukeexplosion.ogg` | `effects.nukeexplosion` | master | Nuke rocket detonation ([`NukeRocketProjectile.explodeRocket`](projectiles.md#34-rocketprojectile-and-nukerocketprojectile)), `playSoundEffect` at the impact point, **volume 8.0** (audible about 128 blocks away), pitch 1.0. It plays together with the vanilla `random.explode` from the explosion itself. |
| `sounds/effects/gore_a.ogg`, `sounds/effects/gore_b.ogg`, `sounds/effects/gore_c.ogg` | `effects.gore` | hostile | Client, when a GORE death packet arrives ([`PacketEntityDeathType`](../01-core/networking.md#6--packetentitydeathtype-sc)). Attached to the dying entity via [`TGSound`](../01-core/client-effects-audio.md#tgsound), volume 1, pitch 1, not repeating, category [`TGSoundCategory.DEATHEFFECT`](../01-core/client-effects-audio.md#tgsoundcategory) ("deathFX"). |
| `sounds/effects/biodeath.ogg` | `effects.biodeath` | hostile | Client, BIO death packet (same mechanism as gore). Also the explosion sound of a bursting bio blob ([`BioblobTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-bioblobtileent), [`07-blocks`](../07-blocks/index.md)). |
| `sounds/effects/disintegrate.ogg` | `effects.disintegrate` | hostile | Client, LASER death packet (same mechanism as gore). |
| `sounds/effects/steamarmor_jump_a.ogg`, `sounds/effects/steamarmor_jump_b.ogg`, `sounds/effects/steamarmor_jump_c.ogg` | `effects.steamarmorJump` | player | Server, a powered/steam armour leg piece's jump boost is used ([`PoweredArmor.applyPowerConsumptionOnAction`](../05-armor/armor-catalogue.md#9-power-system-poweredarmor), JUMP bonus, `armorType == 2`). Volume 0.66, pitch 1.0 at the player. See [`05-armor`](../05-armor/index.md). |
| `sounds/effects/steamarmor_jump_d.ogg` | `effects.steamarmorJump2` | player | Jump-pack boost ([`ItemJumppack.doJumpEffect`](../05-armor/special-equipment.md#44-jump-pack-itemjumppack)): server sends [`PacketPlaySound`](../01-core/networking.md#9--packetplaysound-sc) to players within 50 blocks, attached to the player, category PLAYER_EFFECT, together with the `JumpPackBoost` FX. See [`05-armor`](../05-armor/index.md). |
| `sounds/effects/DemoCharge_Plant.ogg` | `tntcharge.plant` | block | TNT/demolition charge block placed ([`ItemBlockMetaDataMachine`](../07-blocks/special-blocks.md#16-itemblockmetadatamachine)), volume 4.0, pitch 1.0. See [`07-blocks`](../07-blocks/index.md). |
| `sounds/effects/DemoCharge_Init.ogg` | `tntcharge.init` | block | Demolition charge armed via its GUI button ([`ExplosiveChargeTileEnt.buttonClicked`](../09-machines-utility/explosive-charges.md#nbt-explosivechargetileent), id 0), volume 4.0. |
| `sounds/effects/DemoCharge_Tick.ogg` | `tntcharge.tick` | block | Every 20 ticks (once per second) while an armed demolition charge counts down (server), volume 4.0. |
| `sounds/effects/C4Charge_Plant.ogg` | `c4charge.plant` | block | As `tntcharge.plant`, for the advanced (C4) charge ([`ExplosiveChargeAdvTileEnt`](../09-machines-utility/explosive-charges.md)). |
| `sounds/effects/C4Charge_Init.ogg` | `c4charge.init` | block | As `tntcharge.init`, for C4. |
| `sounds/effects/C4Charge_Tick.ogg` | `c4charge.tick` | block | As `tntcharge.tick`, for C4. |

Character of the sounds, judged from file names and sizes (for replacement work):

| Sound | Character and length |
|---|---|
| bullet impacts | Five short ricochet/thud variants (measured 0.98 s each at 22050 Hz; the earlier ~0.5 s figure was a guess). |
| explosion1 | Five explosion variants (measured 1.21 s to 2.85 s). |
| nukeexplosion | One long, deep blast with rumble (the largest file, ~86 kB). |
| gore | Three wet splatter variants. |
| biodeath | One acidic squelch/sizzle. |
| disintegrate | One energy zap followed by a burn-out sizzle. |
| steamarmor jump | Four short pneumatic hiss/thump variants. |
| demo/C4 | Three each: plant (clunk/stick), init (arming beep or click), tick (short beep). |

## Other sounds triggered by code in this area (not `effects/` files)

| Event / vanilla name | Trigger |
|---|---|
| `dig.stone` (vol 2), `dig.wood` (vol 2), `dig.glass` (vol 1), `mob.irongolem.hit` (vol 1) | Bullet impact on rock, wood, glass, or iron/anvil. |
| `mob.irongolem.hit` (vol 2, pitch 1) | Grenade bounce. |
| `random.explode` | Default `ProjectileExplosion` sound (rockets, nuke, alien blaster mini-explosion). |
| `random.fizz` (vol 0.4, pitch 2.0–2.4) | A flying gib touches lava. |
| `techguns:guns.powerhammerImpact` / `techguns:guns.powerhammerImpactGround` | Power-hammer shockwave hits an entity / a block (see [`03-gun-visuals-audio`](../03-gun-visuals-audio/index.md)). |

## Measured audio facts and call parameters (consistency pass)

Duration, sample rate, channels and category are quoted unchanged from [ASSET-FACTS.md](../ASSET-FACTS.md) (read from the Ogg Vorbis headers; category is the `sounds.json` category). Every file is mono. Volume and pitch are the literal arguments in the calling code. Descriptions of what a sound *sounds like* remain inferred from file name and use, because the audio cannot be played here.

### All 26 files

| File | Duration | Sample rate | Channels | `sounds.json` event [category] |
|---|---|---|---|---|
| `sounds/effects/bullet_impact_a.ogg` | 0.98 s | 22050 Hz | mono | `effects.bulletimpact` [player] |
| `sounds/effects/bullet_impact_b.ogg` | 0.98 s | 22050 Hz | mono | `effects.bulletimpact` [player] |
| `sounds/effects/bullet_impact_c.ogg` | 0.98 s | 22050 Hz | mono | `effects.bulletimpact` [player] |
| `sounds/effects/bullet_impact_d.ogg` | 0.98 s | 22050 Hz | mono | `effects.bulletimpact` [player] |
| `sounds/effects/bullet_impact_e.ogg` | 0.98 s | 22050 Hz | mono | `effects.bulletimpact` [player] |
| `sounds/effects/explosion1_a.ogg` | 2.47 s | 44100 Hz | mono | `effects.explosion1` [master] |
| `sounds/effects/explosion1_b.ogg` | 1.75 s | 44100 Hz | mono | `effects.explosion1` [master] |
| `sounds/effects/explosion1_c.ogg` | 1.68 s | 44100 Hz | mono | `effects.explosion1` [master] |
| `sounds/effects/explosion1_d.ogg` | 1.21 s | 44100 Hz | mono | `effects.explosion1` [master] |
| `sounds/effects/explosion1_e.ogg` | 2.85 s | 44100 Hz | mono | `effects.explosion1` [master] |
| `sounds/effects/nukeexplosion.ogg` | 13.77 s | 44100 Hz | mono | `effects.nukeexplosion` [master] |
| `sounds/effects/gore_a.ogg` | 2.99 s | 44100 Hz | mono | `effects.gore` [hostile] |
| `sounds/effects/gore_b.ogg` | 2.99 s | 44100 Hz | mono | `effects.gore` [hostile] |
| `sounds/effects/gore_c.ogg` | 2.99 s | 44100 Hz | mono | `effects.gore` [hostile] |
| `sounds/effects/biodeath.ogg` | 3.99 s | 32000 Hz | mono | `effects.biodeath` [hostile] |
| `sounds/effects/disintegrate.ogg` | 5.29 s | 32000 Hz | mono | `effects.disintegrate` [hostile] |
| `sounds/effects/steamarmor_jump_a.ogg` | 1.21 s | 44100 Hz | mono | `effects.steamarmorJump` [player] |
| `sounds/effects/steamarmor_jump_b.ogg` | 1.21 s | 44100 Hz | mono | `effects.steamarmorJump` [player] |
| `sounds/effects/steamarmor_jump_c.ogg` | 1.21 s | 44100 Hz | mono | `effects.steamarmorJump` [player] |
| `sounds/effects/steamarmor_jump_d.ogg` | 1.21 s | 44100 Hz | mono | `effects.steamarmorJump2` [player] |
| `sounds/effects/DemoCharge_Plant.ogg` | 0.76 s | 44100 Hz | mono | `tntcharge.plant` [block] |
| `sounds/effects/DemoCharge_Init.ogg` | 1.91 s | 44100 Hz | mono | `tntcharge.init` [block] |
| `sounds/effects/DemoCharge_Tick.ogg` | 1.00 s | 44100 Hz | mono | `tntcharge.tick` [block] |
| `sounds/effects/C4Charge_Plant.ogg` | 1.50 s | 44100 Hz | mono | `c4charge.plant` [block] |
| `sounds/effects/C4Charge_Init.ogg` | 0.88 s | 44100 Hz | mono | `c4charge.init` [block] |
| `sounds/effects/C4Charge_Tick.ogg` | 0.70 s | 44100 Hz | mono | `c4charge.tick` [block] |

Observations: the five bullet-impact files share one length (0.98 s) and the lowest rate in the folder (22050 Hz), consistent with the "identical copies" note above. `biodeath` and `disintegrate` are 32000 Hz; everything else is 44100 Hz. All four steam-armour files are the same length.

### Every event: trigger, volume, pitch

| Event | Call site | Volume | Pitch | Notes |
|---|---|---|---|---|
| `effects.bulletimpact` | `playSoundAtEntity(this, ...)` in the default `hitBlock` of `GenericProjectile`, and in `MiningDummyProjectile` | 1.0 | 1.0 | Played when the struck material is not rock, wood, glass, iron or anvil. The same event string appears in a commented-out headshot branch and a commented-out [`BlasterRifleProjectile`](projectiles.md#1-registration) line (dead code). |
| `effects.explosion1` | End of `ProjectileExplosion.doExplosion`, after [`GrenadeProjectile`](projectiles.md#37-grenadeprojectile-family) (stick, frag, 40 mm launcher) calls `setExplosionSound` | 4.0 | `(1 + (rand - rand) * 0.2) * 0.7`, so 0.56 to 0.84, mean 0.7 | `playSoundEffect` at the explosion centre. Callers run on both sides ([PFX-15](index.md#suspected-bugs)). |
| `effects.nukeexplosion` | `NukeRocketProjectile.explodeRocket`, server only | 8.0 | 1.0 | `playSoundEffect` at the rocket. An identical call in a commented-out older method is dead code. Plays together with the vanilla `random.explode` from the `ProjectileExplosion` (volume 4.0, pitch as above). |
| `effects.gore` | `PacketEntityDeathType` client handler, GORE branch, through `proxy.playSoundOnEntity` | 1.0 | 1.0 | Client; not repeating; attached to the dying entity; category `DEATHEFFECT`. The older server-side `playSoundEffect` calls (volume 1.0, pitch 1.0) in [`TechgunsEventhandler`](../01-core/events.md#techgunseventhandler-forge-bus) and [`DeathEffect`](../01-core/client-effects-audio.md#deatheffectcreatedeatheffectentity-type-mx-my-mz) are commented out. |
| `effects.biodeath` (death) | `PacketEntityDeathType` client handler, BIO branch | 1.0 | 1.0 | As gore. |
| `effects.biodeath` (bio blob burst) | `BioblobTileEnt` passes it to `ProjectileExplosion.setExplosionSound` | 4.0 | `(1 + (rand - rand) * 0.2) * 0.7` | `playSoundEffect` at the block centre; here it is an explosion sound, not a death sound. |
| `effects.disintegrate` | `PacketEntityDeathType` client handler, LASER branch | 1.0 | 1.0 | As gore. |
| `effects.steamarmorJump` | `PoweredArmor.applyPowerConsumptionOnAction`, JUMP bonus | 0.66 | 1.0 | Server `playSoundAtEntity` on the player. |
| `effects.steamarmorJump2` | `ItemJumppack.doJumpEffect` through `PacketPlaySound` | 1.0 | 1.0 | Packet flags: repeat false, moving true, gunPosition false, playOnOwnPlayer true; category `PLAYER_EFFECT`; sent to players near the wearer (50 blocks). |
| `tntcharge.plant`, `c4charge.plant` | `ItemBlockMetaDataMachine` when the charge block is placed | 4.0 | 1.0 | `playSoundEffect` at the block centre. |
| `tntcharge.init`, `c4charge.init` | `ExplosiveChargeTileEnt` arming button handler | 4.0 | 1.0 | `playSoundEffect`. |
| `tntcharge.tick`, `c4charge.tick` | `ExplosiveChargeTileEnt` update, once per 20 ticks while armed | 4.0 | 1.0 | `playSoundEffect`. A randomised pitch is present only as a comment. |

The charge detonation itself plays vanilla `random.explode` at volume 4.0 with the same randomised 0.7-scaled pitch; that belongs to [`09-machines-utility`](../09-machines-utility/index.md).

### Vanilla sounds used by code in this area, with parameters

| Vanilla name | Volume | Pitch | Trigger |
|---|---|---|---|
| `dig.stone` | 2.0 | 1.0 | Projectile hits a rock-material block. |
| `dig.wood` | 2.0 | 1.0 | Wood-material block. |
| `dig.glass` | 1.0 | 1.0 | Glass-material block. |
| `mob.irongolem.hit` | 1.0 | 1.0 | Iron or anvil block hit. |
| `mob.irongolem.hit` | 2.0 | 1.0 | Grenade bounce. |
| `random.explode` | 4.0 | `(1 + (rand - rand) * 0.2) * 0.7` | Default `ProjectileExplosion` sound (rockets, alien blaster mini-explosion, nuke). |
| `random.fizz` | 0.4 | 2.0 to 2.4 | Flying gib in lava. |

Events with no parameters of their own in this area (`guns.powerhammerImpact`, `guns.powerhammerImpactGround`) are documented in [`03-gun-visuals-audio`](../03-gun-visuals-audio/index.md).
