# Sounds (`sounds/effects/`)

All 26 files in `sounds/effects/` are covered below. Event names are keys in `sounds.json` and are played as `techguns:<event>`. An event with several files picks one at random each time it plays. Files not under `effects/` that are triggered by projectiles (for example `techguns:guns.powerhammerImpact`) belong to `03-gun-visuals-audio`. They are listed at the end for completeness.

| File | `sounds.json` event | sounds.json category | When it plays |
|---|---|---|---|
| `sounds/effects/bullet_impact_a.ogg`, `sounds/effects/bullet_impact_b.ogg`, `sounds/effects/bullet_impact_c.ogg`, `sounds/effects/bullet_impact_d.ogg`, `sounds/effects/bullet_impact_e.ogg` (all 10083 B, possibly identical copies) | `effects.bulletimpact` | player | A projectile hits a block whose material is not rock, wood, glass, iron or anvil (`GenericProjectile.spawnImpactParticle`, `MiningDummyProjectile`). Volume 1.0, pitch 1.0, via `playSoundAtEntity` at the projectile. Applies to every projectile that keeps the default `hitBlock`/`spawnImpactParticle`: `GenericProjectile`, `SimpleProjectile`, `InvisibleProjectile` (chainsaw) and `MiningDummyProjectile`. Not sonic, blaster rifle or others that override it. A commented-out headshot sound also used this event. |
| `sounds/effects/explosion1_a.ogg`, `sounds/effects/explosion1_b.ogg`, `sounds/effects/explosion1_c.ogg`, `sounds/effects/explosion1_d.ogg`, `sounds/effects/explosion1_e.ogg` | `effects.explosion1` | master | Grenade explosions (stick, frag and 40 mm grenade) through `ProjectileExplosion.setExplosionSound`. Volume 4.0, pitch `(1 + (rand − rand) × 0.2) × 0.7`. Rockets use vanilla `random.explode` instead. |
| `sounds/effects/nukeexplosion.ogg` | `effects.nukeexplosion` | master | Nuke rocket detonation (`NukeRocketProjectile.explodeRocket`), `playSoundEffect` at the impact point, **volume 8.0** (audible about 128 blocks away), pitch 1.0. It plays together with the vanilla `random.explode` from the explosion itself. |
| `sounds/effects/gore_a.ogg`, `sounds/effects/gore_b.ogg`, `sounds/effects/gore_c.ogg` | `effects.gore` | hostile | Client, when a GORE death packet arrives (`PacketEntityDeathType`). Attached to the dying entity via `TGSound`, volume 1, pitch 1, not repeating, category `TGSoundCategory.DEATHEFFECT` ("deathFX"). |
| `sounds/effects/biodeath.ogg` | `effects.biodeath` | hostile | Client, BIO death packet (same mechanism as gore). Also the explosion sound of a bursting bio blob (`BioblobTileEnt`, `07-blocks`). |
| `sounds/effects/disintegrate.ogg` | `effects.disintegrate` | hostile | Client, LASER death packet (same mechanism as gore). |
| `sounds/effects/steamarmor_jump_a.ogg`, `sounds/effects/steamarmor_jump_b.ogg`, `sounds/effects/steamarmor_jump_c.ogg` | `effects.steamarmorJump` | player | Server, a powered/steam armour leg piece's jump boost is used (`PoweredArmor.applyPowerConsumptionOnAction`, JUMP bonus, `armorType == 2`). Volume 0.66, pitch 1.0 at the player. See `05-armor`. |
| `sounds/effects/steamarmor_jump_d.ogg` | `effects.steamarmorJump2` | player | Jump-pack boost (`ItemJumppack.doJumpEffect`): server sends `PacketPlaySound` to players within 50 blocks, attached to the player, category PLAYER_EFFECT, together with the `JumpPackBoost` FX. See `05-armor`. |
| `sounds/effects/DemoCharge_Plant.ogg` | `tntcharge.plant` | block | TNT/demolition charge block placed (`ItemBlockMetaDataMachine`), volume 4.0, pitch 1.0. See `07-blocks`. |
| `sounds/effects/DemoCharge_Init.ogg` | `tntcharge.init` | block | Demolition charge armed via its GUI button (`ExplosiveChargeTileEnt.buttonClicked`, id 0), volume 4.0. |
| `sounds/effects/DemoCharge_Tick.ogg` | `tntcharge.tick` | block | Every 20 ticks (once per second) while an armed demolition charge counts down (server), volume 4.0. |
| `sounds/effects/C4Charge_Plant.ogg` | `c4charge.plant` | block | As `tntcharge.plant`, for the advanced (C4) charge (`ExplosiveChargeAdvTileEnt`). |
| `sounds/effects/C4Charge_Init.ogg` | `c4charge.init` | block | As `tntcharge.init`, for C4. |
| `sounds/effects/C4Charge_Tick.ogg` | `c4charge.tick` | block | As `tntcharge.tick`, for C4. |

Character of the sounds, judged from file names and sizes (for replacement work):

| Sound | Character and length |
|---|---|
| bullet impacts | Five short (~0.5 s) ricochet/thud variants. |
| explosion1 | Five medium explosion variants (~1–2 s). |
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
| `techguns:guns.powerhammerImpact` / `techguns:guns.powerhammerImpactGround` | Power-hammer shockwave hits an entity / a block (see `03-gun-visuals-audio`). |
