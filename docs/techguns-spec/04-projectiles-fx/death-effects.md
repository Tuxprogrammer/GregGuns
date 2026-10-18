# Death effects (gore, bio-dissolve, laser-disintegrate)

Special death animations are chosen by the damage type that kills an entity.

Sources: [`java/techguns/util/EntityDeathUtils.java`](index.md#coverage), `java/techguns/client/renderer/death/RendererDeadLivingEntity.java`, `java/techguns/client/models/ModelGibs*.java`, [`java/techguns/entities/projectiles/FlyingGibs.java`](projectiles.md#319-flyinggibs), [`java/techguns/client/renderer/entity/RenderFlyingGibs.java`](projectiles.md#1-registration), `java/techguns/client/particle/EntityBloodEffect.java`.

Supporting code outside this area, described here only as far as the trigger chain needs: [`client/DeathEffect.java`](../01-core/client-effects-audio.md#deatheffectcreatedeatheffectentity-type-mx-my-mz), `client/GoreData.java`, [`packets/PacketEntityDeathType.java`](../01-core/networking.md#6--packetentitydeathtype-sc), [`events/TechgunsEventhandler.java`](../01-core/events.md#techgunseventhandler-forge-bus) (death and render events), and the gore roll in [`damagesystem/DamageSystem.java`](../02-guns/index.md#coverage) ([`01-core`](../01-core/index.md)).

## 1. Death types

`EntityDeathUtils.DeathType` (the id is sent over the network):

| Value | Id | Visual |
|---|---|---|
| `DEFAULT` | 0 | Vanilla death (falls over and turns red). |
| `GORE` | 1 | The body vanishes and explodes into flying limb gibs and a blood burst. Sound `techguns:effects.gore`. |
| `BIO` | 2 | The body swells, turns green, flashes, and fades out with a green acid splash. Sound `techguns:effects.biodeath`. |
| `LASER` | 3 | The body turns red-hot, glows with a lava overlay and fades, with fire and ash particles. Sound `techguns:effects.disintegrate`. |
| `DISMEMBER` | 4 | The body tips over, the head spins off. **No damage source ever uses it**, so it is effectively dead code. |

Which projectile carries which death type and gore chance is listed in [projectiles.md §3](projectiles.md#3-per-projectile-reference). The default [`TGDamageSource.goreChance`](../02-guns/damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource) is 0.5.

## 2. Trigger chain

1. **Lethal hit (server, `DamageSystem`, [`01-core`](../01-core/index.md)).** When a `TGDamageSource` kills an entity, the damage system rolls `rand > goreChance`. If true, it sets the source's `deathType` to `DEFAULT` before calling `onDeath`. So `goreChance` is the probability of the special death.
   - [`MiningDummyProjectile`](projectiles.md#314-miningdummyprojectile) does its own kill with the comparison **inverted** (bug [PFX-13](index.md#suspected-bugs)).
   - Sources that bypass the damage system keep their death type unconditionally.
2. **`LivingDeathEvent` (server, `TechgunsEventhandler.onLivingDeathEvent`).**
   - If the source is a `TGDamageSource` with a death type other than DEFAULT, and `EntityDeathUtils.hasSpecialDeathAnim(entity, deathType)` is true, send `PacketEntityDeathType` to all players within **100** blocks.
   - Server-side sound playback here is commented out; sounds are played client-side by the packet handler.
3. **`hasSpecialDeathAnim(entity, type)`:**
   - Bosses (`IBossDisplayData`) return true only if they are also a Techguns [`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava). Vanilla bosses (Wither, Dragon) never get special deaths.
   - `BIO` and `LASER` return true for **every** other living entity (the code comments call this "test code").
   - `GORE` and `DISMEMBER` return true only if the entity's *exact* class is in the gore list:
     - vanilla: `EntityPlayer`, `EntityZombie`, `EntitySkeleton`, `EntityEnderman`, `EntityCreeper`, `EntityCow`, `EntitySheep`, `EntityPig`, `EntityChicken`, `EntityPigZombie`, `EntitySpider`, `EntityCaveSpider`, `EntityWitch`, `EntitySlime`, `EntityHorse`, `EntityMooshroom`, `EntityWolf`, `EntitySquid`, `EntityGhast`, `EntityVillager`
     - Techguns: [`ZombieSoldier`](../10-entities-npcs/per-mob.md#1-zombie-soldier-zombiesoldier) (listed twice), [`ArmySoldier`](../10-entities-npcs/per-mob.md#6-army-soldier-armysoldier), [`CyberDemon`](../10-entities-npcs/per-mob.md#11-cyberdemon-cyberdemon), [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier), [`ZombieFarmer`](../10-entities-npcs/per-mob.md#2-zombie-farmer-zombiefarmer), [`ZombieMiner`](../10-entities-npcs/per-mob.md#3-zombie-miner-zombieminer), [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit), [`PsychoSteve`](../10-entities-npcs/per-mob.md#12-psycho-steve-psychosteve-early-game-mini-boss), [`DictatorDave`](../10-entities-npcs/per-mob.md#13-dictator-dave-dictatordave-mini-boss), [`SkeletonSoldier`](../10-entities-npcs/per-mob.md#5-skeleton-soldier-skeletonsoldier), [`AlienBug`](../10-entities-npcs/per-mob.md#15-alien-bug-alienbug)
   - The exact-class test means subclasses (and `EntityPlayerMP`, the actual runtime class of players) do not match. **Players therefore never gore** (bug [PFX-18](index.md#suspected-bugs)), and the same applies to modded mob subclasses.
4. **`PacketEntityDeathType`.** Fields in order: `entityId` (int), `deathtypeId` (int), `motionX`, `motionY`, `motionZ` (floats, the entity's motion at death).
5. **Client handler:**
   - Does nothing if config `cl_enableDeathFX` is off. Ignores `GORE` if `cl_enableDeathFX_Gore` is off.
   - Otherwise:
     1. Look up the entity.
     2. Overwrite its motion with the packet motion.
     3. Store the death type in [`ClientProxy.entityDeathTypes`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy), keyed by the player's name for players or the entity id otherwise.
     4. Call `DeathEffect.createDeathEffect(entity, type, motion)`.
     5. Play the death sound attached to the entity: `proxy.playSoundOnEntity(entity, sound, volume 1, pitch 1, non-repeating, TGSoundCategory.DEATHEFFECT)`. See [sounds.md](sounds.md).
6. **Rendering (`TechgunsEventhandler.handleEntityRenderEvent`, `RenderLivingEvent.Pre`).** For an entity with a stored death type:
   - `GORE`: cancel the normal render. The corpse is invisible for its whole 20-tick death.
   - `BIO`, `LASER`, `DISMEMBER`: cancel and call `RendererDeadLivingEntity.doRender(…)`.
   - `DEFAULT`: render normally.
   - When the entity is rendered with `deathTime == 0` (for example a respawned player with the same name key), the stored entry is cleared. Entries for entities that are never rendered again stay in the map, a small leak.

### Config (client side, category `Clientside`)

| Key | Field | Default | Comment |
|---|---|---|---|
| [`EnableDeathEffects`](../01-core/config.md#category-clientside) | [`TGConfig.cl_enableDeathFX`](../01-core/config.md) | true | "Enable Death Effects, pure clientside check." |
| [`EnableGoreDeathEffect`](../01-core/config.md#category-clientside) | `TGConfig.cl_enableDeathFX_Gore` | true | "Enable the gore Death Effect, requires DeathEffects to be enabled, pure clientside check." |

## 3. GORE: blood burst and flying gibs

`DeathEffect.createDeathEffect` for GORE, at (x, y + height/2, z):

1. Look up `GoreData` for the entity's exact class. The fallback is `genericGore`.
2. Spawn one `EntityBloodEffect` (see below).
3. Spawn `data.numGibs` `FlyingGibs` entities into the **client** world, with body-part index i = 0..n−1:
   - velocity = (motionX × 0.35 + vx, motionY × 0.15 + vy, motionZ × 0.35 + vz)
   - vx, vz are uniform ±0.175 (that is (0.5 − rand) × 0.35)
   - vy is rand × 0.35 if the entity is on the ground, otherwise ±0.175
   - gib `size` = (width + height) / 2

### `GoreData` table (`client/DeathEffect.java`)

| Entity class | Gib model | Gibs | Texture | Scale | Blood RGB | Blood? |
|---|---|---|---|---|---|---|
| `EntityPlayer` | `ModelGibsBiped(1.0, 0, 64, 32)` | 6 | `minecraft:textures/entity/steve.png` | 0.66 | 160, 21, 31 | yes |
| `EntityZombie` | `ModelGibsBiped(1.0)` (64 × 64) | 6 | `minecraft:textures/entity/zombie/zombie.png` | 0.66 | 110, 21, 41 | yes |
| `ZombieSoldier` | `ModelGibsBiped(1.0)` | 6 | `techguns:textures/entity/zombie_soldier.png` | 0.66 | 110, 21, 41 | yes |
| `ZombieFarmer`, `ZombieMiner` | `ModelGibsBiped(1.0)` | 6 | `techguns:textures/entity/zombie_soldier.png` | 0.66 | 110, 21, 41 | yes |
| `ArmySoldier` | `ModelGibsBiped(1.0)` | 6 | `techguns:textures/entity/army_soldier.png` | 0.66 | 160, 21, 31 | yes |
| `Bandit` | `ModelGibsBiped(1.0)` | 6 | `techguns:textures/entity/bandit.png` | 0.66 | 160, 21, 31 | yes |
| `EntitySkeleton`, `SkeletonSoldier` | `ModelGibsSkeleton(1.0)` | 6 | `minecraft:textures/entity/skeleton/skeleton.png` | 0.66 | 255, 255, 255 | **no** (dust) |
| `EntityVillager` | `ModelGibsVillagerGeneric(new ModelVillager(1.0))` | 5 | `minecraft:textures/entity/villager/villager.png` | 0.66 | 150, 21, 51 | yes |
| `EntityWitch` | `ModelGibsVillagerGeneric(new ModelWitch(1.0))` | 5 | `minecraft:textures/entity/witch.png` | 0.66 | 160, 21, 31 | yes |
| `EntityCow` | `ModelGibsGeneric(new ModelCow())` | 6 | `minecraft:textures/entity/cow/cow.png` | 1.0 | 170, 26, 37 | yes |
| `EntitySheep` | `ModelGibsGeneric(new ModelSheep1())` | 6 | `minecraft:textures/entity/sheep/sheep.png` | 1.0 | 170, 26, 37 | yes |
| `EntityChicken` | `ModelGibsGeneric(new ModelChicken())` | 6 | `minecraft:textures/entity/chicken.png` | 1.0 | 170, 26, 37 | yes |
| `EntityPig` | `ModelGibsGeneric(new ModelPig())` | 6 | `minecraft:textures/entity/pig/pig.png` | 0.8 | 170, 26, 37 | yes |
| `EntityCreeper` | `ModelGibsGeneric(new ModelCreeper())` | 7 | `minecraft:textures/entity/creeper/creeper.png` | 1.0 | 50, 175, 57 | yes |
| `EntityEnderman` | `ModelGibsGeneric(new ModelEnderman())` | 7 | `minecraft:textures/entity/enderman/enderman.png` | 1.0 | 160, 36, 167 | yes |
| `EntitySpider` | `ModelGibsGeneric(new ModelSpider())` | 11 | `minecraft:textures/entity/spider/spider.png` | 1.0 | 85, 156, 17 | yes |
| `EntityCaveSpider` | `ModelGibsGeneric(new ModelSpider())` | 11 | `minecraft:textures/entity/spider/cave_spider.png` | 0.66 | 85, 156, 17 | yes |
| `EntityPigZombie`, `ZombiePigmanSoldier` | `ModelGibsBiped(1.0)` | 6 | `minecraft:textures/entity/zombie_pigman.png` | 0.66 | 110, 51, 11 | yes |
| `CyberDemon` | `ModelGibsBipedGeneric(new ModelCyberDemon())` | 6 | `techguns:textures/entity/cyberdemon.png` | 1.0 | 85, 156, 17 | yes |
| `AlienBug` | `ModelGibsAlienBug()` | 8 | `techguns:textures/entity/alienbug.png` | 0.8 | 235, 255, 70 | yes |
| `EntitySlime` | none | 0 | none | 1.0 | 40, 255, 40 | yes (blood only) |
| *anything else* (`genericGore`) | `ModelGibsBiped(1.0)` | 6 | `techguns:textures/entity/gore.png` | 0.66, × random 0.5–0.8 per gib | 160, 21, 31 | yes |

Entities in the gore list without a `GoreData` entry (horse, mooshroom, wolf, squid, ghast, PsychoSteve, DictatorDave) use `genericGore`: six generic flesh-cube limbs textured with `gore.png`. The horse entry is commented out (`ModelGibsHorse`).

### Blood burst (`EntityBloodEffect`)

An invisible `EntityFX` (it renders nothing) with the entity's death motion, max age 5 ticks and vanilla gravity 0.08. On each of its ticks:

- **Blood (`showBlood`):** spawn `1 + floor((width + height) / 2 × 2)` blood sprites.
  - Position: uniform within the entity's box (x and z ± width/2, y ± height/2 around the centre).
  - Velocity: (rand − 0.5) × 0.15 + motion × (0.5, 0.1, 0.5).
  - Sprite parameters are in [fx-catalogue.md](fx-catalogue.md#death-effect-sprites-not-named-built-directly) (`blood4x4.png`, ALPHA, tinted with the blood RGB).
- **No blood (skeletons):** one `dustclouds02.png` animated puff per tick, velocity (rand − 0.5) × 0.10 + motion × (0.3, 0.1, 0.3), tinted with the "blood" colour (white).

### Gib models (`client/models/ModelGibs*.java`)

All extend the abstract `ModelGibs` (`ModelBase`), which adds `render(entity, f…, scale, partIndex)` to draw a single part and a deprecated `getNumGibs()`. All parts have their rotation point at the origin, so each gib spins about its own box origin.

| Model | Part index → box |
|---|---|
| `ModelGibsBiped(inflate[, yOff, texW, texH])` (default 64 × 64) | 0 head 8×8×8 at (−4, −8, −4), UV (0, 0) · 1 body 8×12×4 at (−4, 0, −2), UV (16, 16) · 2 right arm 4×12×4 at (−3, −2, −2), UV (40, 16) · 3 left arm (mirrored) at (−1, −2, −2), UV (40, 16) · 4 right leg 4×12×4 at (−2, 0, −2), UV (0, 16) · 5 left leg (mirrored), UV (0, 16). All boxes are inflated by `inflate` (1.0 in every use), so gibs are slightly fatter than the live model. |
| `ModelGibsSkeleton(inflate)` (64 × 32) | As biped, but arms and legs are 2×12×2 at (−1, −2, −1) and (−1, 0, −1). |
| `ModelGibsBipedGeneric(ModelBiped)` | Wraps an existing biped model (CyberDemon) with its 6 standard parts (head, body, right arm, left arm, right leg, left leg) and their rotation points reset to the origin. |
| `ModelGibsVillagerGeneric(ModelVillager)` | 0 head, 1 body, 2 right leg, 3 left leg, 4 arms (with rotation points reset). Used for the villager and the witch. |
| `ModelGibsGeneric(ModelBase)` | Part *i* = the *i*-th entry of the wrapped model's `boxList` (rotation points reset; child boxes come along with their parent). Parts beyond `numGibs` are never drawn, for example the chicken's bill and chin. |
| `ModelGibsAlienBug` (64 × 64) | 0 head `h1` 5×5×6 at (−2.5, −2.5, −3), UV (20, 9), tilted 0.137 rad X, with children upper jaw `JAW_U` 4×4×10 (UV (2, 15), pivot (−2, −1, −2), −0.182 rad X) and lower jaw `JAW_L` 3×4×9 (UV (21, 20), pivot (−1.5, −1.5, −2.5), 0.455 rad X). 1 head plate `h2` 6×3×8 at (0, 0, −7), UV (36, 18). 2–5 the same leg `L1_A` 9×3×3 (UV (0, 4), rotation (0, 2.79, 0.524) rad) with child `L1_B` 11×2×2 (UV (0, 0), pivot (7.5, 0, 0), Z 1.745 rad). 6 abdomen `b1` 5×5×9, UV (0, 33). 7 thorax `b2` 6×4×4 (UV (23, 33)) with children `b3` 4×3×4 (UV (43, 31), pivot (1, 0.5, 4.5), −0.182 X) and `b4` 2×2×2 (UV (56, 30), pivot (2, 2, 8.5), −0.364 X). |
| `ModelGibsHorse(ModelHorse)` | Unused (commented out). Part *i* = `boxList[i]`. Its `getNumGibs` wrongly returns its own empty list size. |

### Flying gibs: entity

See [projectiles.md §3.19](projectiles.md#319-flyinggibs): lifetime 150–250 ticks, gravity 0.03, bounce ×−0.9, lava pop, blood trail while airborne.

### Flying gibs rendering

`RenderFlyingGibs`:

1. Bind the `GoreData` texture of the dying entity's class.
2. Translate to the gib.
3. **Spin:**
   - Airborne: angle step `a = 5 + 15 × timeToLive / maxTimeToLive` degrees per tick. Total rotation = 90 + (ticksExisted + pt) × a. Gibs spin fastest when fresh.
   - On the ground: `a = 5 + 15 × hitGroundTTL / max`, and the rotation is frozen at 90 + (max − hitGroundTTL) × a.
   - The rotation axis is the gib's random `rotationAxis`.
4. **Sinking:** once more than 20 ticks have passed since landing, translate down by `(ticks since landing − 20 + pt) × 0.05` blocks. Gibs slowly sink into the ground before despawning.
5. Scale: `GoreData.scale` × a per-part random factor from `minPartScale`–`maxPartScale` (seeded with entity id + part index). Halved for child entities.
6. Culling off. Render part `bodypart` at 1/16 × scale.

## 4. BIO death (`RendererDeadLivingEntity.renderModelDeathBio`)

- Client FX: `createFX("biodeath", centre, motion)`. Green splats, see the catalogue.
- Sound: `techguns:effects.biodeath`.
- Render, with `prog = deathTime / 20` and a random seeded by the entity id:
  - Living animations are applied. Biped models get their rotation angles.
  - The model is rotated by the entity yaw (not interpolated). Slimes are scaled by their size (offset −0.8 for size 2, −1.2 for size 4).
  - For each top-level model box (child boxes render through their parent; hidden boxes are skipped):
    1. Scale the box uniformly by `1 + rand × prog` about its offset point, so each limb swells by a random amount.
    2. **Base pass** with the entity texture, colour (1 − prog², 1, 1 − prog²) and alpha (1 − prog)². It turns green and fades; the alpha test cuts it once the alpha drops below 0.1.
    3. **Overlay pass** with [`textures/fx/bio.png`](textures.md#single-images), ADDITIVE, full-bright, grey level `0.5 + sin((√prog + 0.75) × 2π) / 2`. This produces a bright green pulsing flash.

## 5. LASER death (`renderModelDeathLaser`)

- Client FX: `createFX("laserdeathFire")` and `createFX("laserdeathAsh")` at the centre, with horizontal motion only (Y = 0).
- Sound: `techguns:effects.disintegrate`.
- Render: the whole model is drawn in two passes, rotated by entity yaw with slime scaling:
  1. Entity texture tinted (1, 1 − prog², 1 − prog²) with alpha (1 − prog)². It turns red and fades.
  2. Overlay [`textures/fx/laserDeathfx.png`](textures.md#single-images), ADDITIVE, full-bright, grey level `0.5 + sin((√prog + 0.75) × 2π) / 2`.
- A per-box swelling variant is commented out.

## 6. DISMEMBER (unused)

`renderModelDeathDismember`, with `prog = (deathTime − pt) / 20` and `p = min(1, 2 × prog)`:

- **`ModelBiped`:** body, arms and legs rotate together by −90 × (1 − cos πp)/2 degrees about the X axis, pivoting at 0.75 × height. They are also lifted by 0.25 × sin(πp). The head is drawn separately, rotated 360 × prog degrees about a random axis (seeded by entity id) through the centre of its cubes.
- **`ModelQuadruped`:** the same, with the body tipping about the Z axis.
- **Other models:** draw all top-level boxes unchanged, and print `boxes.size = …` to stdout every frame.

`RendererDeadLivingEntity` also contains unused helpers (`renderModel`, `renderExtraPasses`, `biped_renderEquippedItems`). They are reflection-based re-implementations of vanilla `RendererLivingEntity` passes (armour layers, enchant glint, hurt tint, held items) and are never called, so dying entities rendered by the BIO, LASER and DISMEMBER paths show **no armour or held items**. All reflection targets are looked up with SRG and MCP names via `ReflectionHelper`. Exceptions during rendering are swallowed.

## 7. Death messages (lang)

These are used by the projectile damage sources in this area. The message is `death.attack.<source name>`, with a `.item` variant used when the killer's held item has a custom name.

| Key | English |
|---|---|
| `death.attack.tg_bullet` | `%s was shot by %s` |
| `death.attack.tg_bullet.item` | `%s was shot by %s with %s` |
| `death.attack.tg_explosion` | `%s was blown up by %s` |
| `death.attack.tg_explosion.item` | `%s was blown up by %s with %s` |
| `death.attack.tg_fire` | `%s was burned to death by %s` |
| `death.attack.tg_fire.item` | `%s was burned to death by %s with %s` |
| `death.attack.tg_poison` | `%s was poisoned by %s` |
| `death.attack.tg_poison.item` | `%s was poisoned by %s with %s` |
| `death.attack.powerhammer` | `%s was dismembered by %s with a powerhammer` |
| `death.attack.powerhammer.item` | `%s was dismembered by %s with %s` |
| `death.attack.tg_lightning` | `%s was electrocuted by %s` |
| `death.attack.tg_lightning.item` | `%s was electrocuted by %s with %s` |
| `death.attack.tg_energy` | `%s was melted by %s` |
| `death.attack.tg_energy.item` | `%s was melted by %s with %s` |
| `death.attack.chainsaw` | `%s was dismembered by %s with a chainsaw` |
| `death.attack.tg_sonic` | `%s was shreddered by %s's sonic weapon` |
| `death.attack.tg_rad` | `%s was radiated by %s` |

Missing keys: `death.attack.tg_knockback` (a 0.01 dummy hit can kill a nearly dead entity and would show the raw key), `death.attack.bullet` (the [`BulletDamageSource`](../01-core/textures-sounds-lang.md#missing-or-untranslated-keys-used-by-core-code) name, used only by unspawned projectiles), and `.item` variants for chainsaw, sonic and rad. A missing `.item` key falls back to the plain message.
